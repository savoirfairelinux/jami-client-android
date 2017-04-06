/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.facades;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.inject.Inject;

import cx.ring.daemon.StringMap;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.ConfigKey;
import cx.ring.model.Conversation;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.HistoryText;
import cx.ring.model.SecureSipCall;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.StringUtils;
import cx.ring.utils.Tuple;

/**
 * This facade handles the conversations
 * - Load from the history
 * - Keep a local cache of these conversations
 * <p>
 * Events are broadcasted:
 * - CONVERSATIONS_CHANGED
 */
public class ConversationFacade extends Observable implements Observer<ServiceEvent> {

    private final static String TAG = ConversationFacade.class.getName();

    @Inject
    AccountService mAccountService;

    private ContactService mContactService;

    @Inject
    ConferenceService mConferenceService;

    private HistoryService mHistoryService;

    private CallService mCallService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    NotificationService mNotificationService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private Map<String, Conversation> mConversationMap;

    public ConversationFacade(HistoryService historyService, CallService callService, ContactService contactService) {
        mConversationMap = new HashMap<>();
        mHistoryService = historyService;
        mHistoryService.addObserver(this);
        mCallService = callService;
        mCallService.addObserver(this);
        mContactService = contactService;
        mContactService.addObserver(this);
    }

    private Tuple<Conference, SipCall> getCall(String id) {
        for (Conversation conv : mConversationMap.values()) {
            ArrayList<Conference> confs = conv.getCurrentCalls();
            for (Conference c : confs) {
                SipCall call = c.getCallById(id);
                if (call != null) {
                    return new Tuple<>(c, call);
                }
            }
        }
        return new Tuple<>(null, null);
    }

    /**
     * @return the local cache of conversations
     */
    public Map<String, Conversation> getConversations() {
        return mConversationMap;
    }

    /**
     * @param contact
     * @return
     */
    public Conversation getConversationByContact(CallContact contact) {
        ArrayList<String> keys = contact.getIds();
        for (String key : keys) {
            Conversation conversation = mConversationMap.get(key);
            if (conversation != null) {
                return conversation;
            }
        }
        return null;
    }

    /**
     * @param callId
     * @return
     */
    public Conversation getConversationByCallId(String callId) {
        for (Conversation conversation : mConversationMap.values()) {
            Conference conf = conversation.getConference(callId);
            if (conf != null) {
                return conversation;
            }
        }
        return null;
    }

    /**
     * @param contact
     * @return the started new conversation
     */
    public Conversation startConversation(CallContact contact) {
        if (contact.isUnknown()) {
            contact = mContactService.findContactByNumber(contact.getPhones().get(0).getNumber().getRawUriString());
        }
        Conversation conversation = getConversationByContact(contact);
        if (conversation == null) {
            conversation = new Conversation(contact);
            mConversationMap.put(contact.getIds().get(0), conversation);

            setConversationVisible();

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONVERSATIONS_CHANGED);
            notifyObservers(event);

            updateTextNotifications();
        }
        return conversation;
    }

    /**
     * @return the conversation local cache in a List
     */
    public ArrayList<Conversation> getConversationsList() {
        ArrayList<Conversation> convs = new ArrayList<>(mConversationMap.values());
        Collections.sort(convs, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation lhs, Conversation rhs) {
                return (int) ((rhs.getLastInteraction().getTime() - lhs.getLastInteraction().getTime()) / 1000l);
            }
        });
        return convs;
    }

    /**
     * @param id
     * @return the conversation from the local cache
     */
    public Conversation getConversationById(String id) {
        return mConversationMap.get(id);
    }

    /**
     * (also sends CONVERSATIONS_CHANGED event)
     *
     * @param newDisplayName
     * @param ringId
     */
    public void updateConversationContactWithRingId(String newDisplayName, String ringId) {

        if (newDisplayName == null || newDisplayName.isEmpty()) {
            return;
        }

        Uri uri = new Uri(newDisplayName);
        if (uri.isRingId()) {
            return;
        }

        Conversation conversation = mConversationMap.get(CallContact.PREFIX_RING + ringId);
        if (conversation == null) {
            return;
        }

        CallContact contact = conversation.getContact();
        if (contact == null || contact.getDisplayName().equals(newDisplayName)
                || !contact.getDisplayName().contains(ringId)) {
            return;
        }

        Uri ringIdUri = new Uri(ringId);
        contact.getPhones().clear();
        contact.getPhones().add(new cx.ring.model.Phone(ringIdUri, 0));
        contact.setDisplayName(newDisplayName);

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.USERNAME_CHANGED);
        notifyObservers(event);
    }

    public void sendTextMessage(String account, Uri to, String txt) {
        long id = mCallService.sendAccountTextMessage(account, to.getRawUriString(), txt);
        Log.i(TAG, "sendAccountTextMessage " + txt + " got id " + id);
        long conversationID = mHistoryService.getConversationID(account, to.getRawUriString());
        TextMessage message = new TextMessage(false, txt, to, null, account, conversationID);
        message.setID(id);
        message.read();
        mHistoryService.insertNewTextMessage(message);
    }

    public void sendTextMessage(Conference conf, String txt) {
        mCallService.sendTextMessage(conf.getId(), txt);
        SipCall call = conf.getParticipants().get(0);
        long conversationID = mHistoryService.getConversationID(call.getAccount(), call.getNumber());
        TextMessage message = new TextMessage(false, txt, call.getNumberUri(), conf.getId(), call.getAccount(), conversationID);
        message.read();
        mHistoryService.insertNewTextMessage(message);
    }

    private void readTextMessage(TextMessage message) {
        message.read();
        HistoryText ht = new HistoryText(message);
        mHistoryService.updateTextMessage(ht);
    }

    public void readConversation(Conversation conv) {
        for (HistoryEntry h : conv.getRawHistory().values()) {
            NavigableMap<Long, TextMessage> messages = h.getTextMessages();
            for (TextMessage msg : messages.descendingMap().values()) {
                if (msg.isRead()) {
                    break;
                }
                readTextMessage(msg);
            }
        }
        mNotificationService.cancelTextNotification(conv.getContact());
        updateTextNotifications();
    }

    public void refreshConversations() {
        Log.d(TAG, "refreshConversations()");
        try {
            mHistoryService.getCallAndTextAsync();
        } catch (SQLException e) {
            Log.e(TAG, "unable to retrieve history calls and texts", e);
        }
    }

    public void updateTextNotifications() {
        Log.d(TAG, "updateTextNotifications()");

        for (Conversation conversation : mConversationMap.values()) {

            if (conversation.isVisible()) {
                mNotificationService.cancelTextNotification(conversation.getContact());
                continue;
            }
            TreeMap<Long, TextMessage> texts = conversation.getUnreadTextMessages();
            if (texts.isEmpty() || texts.lastEntry().getValue().isNotified()) {
                continue;
            } else {
                mNotificationService.cancelTextNotification(conversation.getContact());
            }

            CallContact contact = conversation.getContact();
            mNotificationService.showTextNotification(contact, texts);
        }
    }

    public synchronized Conference getConference(String id) {
        for (Conversation conv : mConversationMap.values()) {
            Conference conf = conv.getConference(id);
            if (conf != null) {
                return conf;
            }
        }
        return null;
    }

    public Conference getCurrentCallingConf() {
        for (Conversation c : getConversations().values()) {
            Conference conf = c.getCurrentCall();
            if (conf != null) {
                return conf;
            }
        }
        return null;
    }

    public void setConversationVisible() {
        for (Conversation conv : mConversationMap.values()) {
            boolean isConversationVisible = conv.isVisible();
            String conversationKey = conv.getContact().getIds().get(0);
            Conversation newConversation = mConversationMap.get(conversationKey);
            if (newConversation != null) {
                newConversation.setVisible(isConversationVisible);
            }
        }
    }

    public void removeConversation(String id) {
        mConversationMap.remove(id);
    }

    private void parseNewMessage(TextMessage txt, String call) {
        if (!txt.getAccount().equals(mAccountService.getCurrentAccount().getAccountID())) {
            return;
        }

        Conversation conversation;
        if (call != null && !call.isEmpty()) {
            conversation = getConversationByCallId(call);
        } else {
            conversation = startConversation(mContactService.findContactByNumber(txt.getNumberUri().getRawUriString()));
            txt.setContact(conversation.getContact());
        }
        if (conversation.isVisible()) {
            txt.read();
        }

        conversation.addTextMessage(txt);
    }

    private void parseHistoryCalls(List<HistoryCall> historyCalls, boolean acceptAllMessages) {
        for (HistoryCall call : historyCalls) {
            if (call.getAccountID() != null && call.getAccountID().equals(mAccountService.getCurrentAccount().getAccountID())) {
                String key = StringUtils.getRingIdFromNumber(call.getNumber());
                String phone = "";
                CallContact contact = mContactService.findContact(call.getContactID(), call.getContactKey(), call.getNumber());
                if (contact != null) {
                    key = contact.getIds().get(0);
                    phone = contact.getPhones().get(0).getNumber().getRawUriString();
                }
                if (mConversationMap.containsKey(key) || mConversationMap.containsKey(phone)) {
                    mConversationMap.get(key).addHistoryCall(call);
                } else if (acceptAllMessages) {
                    Conversation conversation = new Conversation(contact);
                    conversation.addHistoryCall(call);
                    mConversationMap.put(key, conversation);
                }
            }
        }
    }

    private void parseHistoryTexts(List<HistoryText> historyTexts, boolean acceptAllMessages) {
        for (HistoryText htext : historyTexts) {
            if (htext.getAccountID() != null && htext.getAccountID().equals(mAccountService.getCurrentAccount().getAccountID())) {
                TextMessage msg = new TextMessage(htext);
                String key = StringUtils.getRingIdFromNumber(htext.getNumber());
                String phone = "";
                CallContact contact = mContactService.findContact(htext.getContactID(), htext.getContactKey(), htext.getNumber());
                if (contact != null) {
                    key = contact.getIds().get(0);
                    phone = contact.getPhones().get(0).getNumber().getRawUriString();
                }
                if (mConversationMap.containsKey(key) || mConversationMap.containsKey(phone)) {
                    mConversationMap.get(key).addTextMessage(msg);
                } else if (acceptAllMessages) {
                    Conversation conversation = new Conversation(contact);
                    conversation.addTextMessage(msg);
                    mConversationMap.put(key, conversation);
                }
            }
        }
    }


    private void addContactDaemon(boolean acceptAllMessages) {
        ArrayList<CallContact> contacts;
        if (acceptAllMessages) {
            contacts = new ArrayList<>(mContactService.getContactsNoBanned());
        } else {
            contacts = new ArrayList<>(mContactService.getContactsConfirmed());
        }
        for (CallContact contact : contacts) {
            String key = contact.getIds().get(0);
            String phone = contact.getPhones().get(0).getNumber().getRawUriString();
            if (!mConversationMap.containsKey(key) && !mConversationMap.containsKey(phone)) {
                mConversationMap.put(key, new Conversation(contact));
            }
        }
    }

    /**
     * Need to be called when switching account/allowing all calls
     */
    public void clearConversations() {
        mConversationMap.clear();
        refreshConversations();
    }

    private void aggregateHistory() {
        Map<String, ArrayList<String>> conferences = mConferenceService.getConferenceList();

        for (Map.Entry<String, ArrayList<String>> conferenceEntry : conferences.entrySet()) {
            Conference conference = new Conference(conferenceEntry.getKey());
            for (String callId : conferenceEntry.getValue()) {
                SipCall call = getCall(callId).second;
                if (call == null) {
                    call = new SipCall(callId, mCallService.getCallDetails(callId));
                }
                Account account = mAccountService.getAccount(call.getAccount());
                if (account.isRing()
                        || account.getDetailBoolean(ConfigKey.SRTP_ENABLE)
                        || account.getDetailBoolean(ConfigKey.TLS_ENABLE)) {
                    call = new SecureSipCall(call, account.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
                }
                conference.addParticipant(call);
            }
            List<SipCall> calls = conference.getParticipants();
            if (calls.size() == 1) {
                SipCall call = calls.get(0);
                CallContact contact = mContactService.findContact(-1, null, call.getNumber());
                call.setContact(contact);

                Conversation conv = null;
                ArrayList<String> ids = contact.getIds();
                for (String id : ids) {
                    conv = mConversationMap.get(id);
                    if (conv != null) {
                        break;
                    }
                }
                if (conv != null) {
                    conv.addConference(conference);
                } else {
                    conv = new Conversation(contact);
                    conv.addConference(conference);
                    mConversationMap.put(ids.get(0), conv);
                }
            }
        }
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {

        ServiceEvent mEvent;
        SipCall call;
        String callId;
        Uri number;

        if (observable instanceof HistoryService && event != null) {

            switch (event.getEventType()) {
                case INCOMING_MESSAGE:
                    TextMessage txt = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);
                    callId = event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class);

                    parseNewMessage(txt, callId);
                    updateTextNotifications();

                    setChanged();
                    mEvent = new ServiceEvent(ServiceEvent.EventType.INCOMING_MESSAGE);
                    notifyObservers(mEvent);
                    break;
                case HISTORY_LOADED:
                    Account account = mAccountService.getCurrentAccount();
                    boolean acceptAllMessages = account.getDetailBoolean(ConfigKey.DHT_PUBLIC_IN);

                    addContactDaemon(acceptAllMessages);

                    List<HistoryCall> historyCalls = (List<HistoryCall>) event.getEventInput(ServiceEvent.EventInput.HISTORY_CALLS, ArrayList.class);
                    parseHistoryCalls(historyCalls, acceptAllMessages);

                    List<HistoryText> historyTexts = (List<HistoryText>) event.getEventInput(ServiceEvent.EventInput.HISTORY_TEXTS, ArrayList.class);
                    parseHistoryTexts(historyTexts, acceptAllMessages);

                    aggregateHistory();

                    setChanged();
                    mEvent = new ServiceEvent(ServiceEvent.EventType.HISTORY_LOADED);
                    notifyObservers(mEvent);
                    break;
                case HISTORY_MODIFIED:
                    refreshConversations();
                    break;
            }
        } else if (observable instanceof CallService && event != null) {
            switch (event.getEventType()) {
                case CALL_STATE_CHANGED:
                    callId = event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class);
                    int newState = SipCall.stateFromString(event.getEventInput(ServiceEvent.EventInput.STATE, String.class));

                    Conversation conversation = null;
                    Conference conference = null;

                    call = mCallService.getCurrentCallForId(callId);

                    if (call == null) {
                        Log.w(TAG, "CALL_STATE_CHANGED : call is null " + callId);
                        return;
                    }

                    for (Conversation conv : mConversationMap.values()) {
                        conference = conv.getConference(callId);
                        if (conference != null) {
                            conversation = conv;
                            Log.w(TAG, "CALL_STATE_CHANGED : found conversation " + callId);
                            break;
                        }
                    }

                    if (conversation == null) {
                        conversation = startConversation(call.getContact());
                        conference = new Conference(call);
                        conversation.addConference(conference);
                    }

                    conference.getParticipants().clear();
                    conference.addParticipant(call);

                    Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + newState);
                    if ((call.isRinging() || newState == SipCall.State.CURRENT) && call.getTimestampStart() == 0) {
                        call.setTimestampStart(System.currentTimeMillis());
                    }

                    if ((newState == SipCall.State.CURRENT && call.isIncoming())
                            || newState == SipCall.State.RINGING && call.isOutGoing()) {
                        mAccountService.sendProfile(callId, call.getAccount());
                    } else if (newState == SipCall.State.HUNGUP
                            || newState == SipCall.State.BUSY
                            || newState == SipCall.State.FAILURE
                            || newState == SipCall.State.OVER) {
                        if (newState == SipCall.State.HUNGUP) {
                            call.setTimestampEnd(System.currentTimeMillis());
                        }
                        if (call.getTimestampStart() == 0) {
                            call.setTimestampStart(System.currentTimeMillis());
                        }
                        if (call.getTimestampEnd() == 0) {
                            call.setTimestampEnd(System.currentTimeMillis());
                        }

                        long conversationID = mHistoryService.getConversationID(call.getAccount(), call.getNumber());
                        call.setConversationID(conversationID);

                        mHistoryService.insertNewEntry(new Conference(call));
                        conference.removeParticipant(call);
                        conversation.addHistoryCall(new HistoryCall(call));
                        mNotificationService.cancelCallNotification(call.getCallId().hashCode());
                        mCallService.removeCallForId(callId);
                    }
                    if (conference.getParticipants().isEmpty()) {
                        conversation.removeConference(conference);
                    }

                    setChanged();
                    mEvent = new ServiceEvent(ServiceEvent.EventType.CALL_STATE_CHANGED);
                    notifyObservers(mEvent);

                    refreshConversations();
                    break;
                case INCOMING_CALL:
                    callId = event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class);
                    String accountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                    number = new Uri(event.getEventInput(ServiceEvent.EventInput.FROM, String.class));

                    Log.w(TAG, "INCOMING_CALL : " + callId + " " + accountId + " " + number);

                    CallContact contact = mContactService.findContactByNumber(number.getRawUriString());

                    Conversation conv = startConversation(contact);

                    call = mCallService.getCurrentCallForId(callId);
                    call.setContact(contact);
                    Conference toAdd = new Conference(call);

                    conv.addConference(toAdd);
                    mNotificationService.showCallNotification(toAdd);

                    mHardwareService.setPreviewSettings();

                    Conference currenConf = getCurrentCallingConf();
                    mDeviceRuntimeService.updateAudioState(currenConf.isRinging()
                            && currenConf.isIncoming());

                    setChanged();
                    ServiceEvent event1 = new ServiceEvent(ServiceEvent.EventType.INCOMING_CALL);
                    notifyObservers(event1);

                    refreshConversations();
                    break;
            }
        } else if (observable instanceof ContactService && event != null) {
            switch (event.getEventType()) {
                case CONTACTS_CHANGED:
                    refreshConversations();
                    break;
            }
        }
    }
}