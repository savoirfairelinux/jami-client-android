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
import java.util.Set;
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
import cx.ring.services.PresenceService;
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

    private PresenceService mPresenceService;

    private Map<String, Conversation> mConversationMap;

    public ConversationFacade(HistoryService historyService, CallService callService, ContactService contactService, PresenceService presenceService) {
        mConversationMap = new HashMap<>();
        mHistoryService = historyService;
        mHistoryService.addObserver(this);
        mCallService = callService;
        mCallService.addObserver(this);
        mContactService = contactService;
        mContactService.addObserver(this);
        mPresenceService = presenceService;
        mPresenceService.addObserver(this);
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
     * @param oldId
     * @param ringId
     */
    public void updateConversationContactWithRingId(String oldId, String ringId) {

        if (oldId == null || oldId.isEmpty()) {
            return;
        }

        Uri uri = new Uri(oldId);
        if (uri.isRingId()) {
            return;
        }

        Conversation conversation = mConversationMap.get(oldId);
        if (conversation == null) {
            return;
        }

        CallContact contact = conversation.getContact();

        if (contact == null) {
            return;
        }

        Uri ringIdUri = new Uri(ringId);
        contact.getPhones().clear();
        contact.getPhones().add(new cx.ring.model.Phone(ringIdUri, 0));
        contact.resetDisplayName();

        mConversationMap.remove(oldId);
        mConversationMap.put(contact.getIds().get(0), conversation);

        for (Map.Entry<String, HistoryEntry> entry : conversation.getHistory().entrySet()) {
            HistoryEntry historyEntry = entry.getValue();
            historyEntry.setContact(contact);
            NavigableMap<Long, TextMessage> messages = historyEntry.getTextMessages();
            for (TextMessage textMessage : messages.values()) {
                textMessage.setNumber(ringIdUri);
                textMessage.setContact(contact);
                mHistoryService.updateTextMessage(new HistoryText(textMessage));
            }
        }

        setConversationVisible();

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONVERSATIONS_CHANGED);
        notifyObservers(event);

        updateTextNotifications();
    }

    public Conversation findOrStartConversationByNumber(Uri number) {
        if (number == null || number.isEmpty()) {
            return null;
        }

        for (Conversation conversation : mConversationMap.values()) {
            if (conversation.getContact().hasNumber(number)) {
                return conversation;
            }
        }

        return startConversation(mContactService.findContactByNumber(number.getRawUriString()));
    }

    public Conference placeCall(SipCall call) {
        Conference conf;
        CallContact contact = call.getContact();
        if (contact == null) {
            contact = mContactService.findContactByNumber(call.getNumberUri().getRawUriString());
        }
        Conversation conv = startConversation(contact);
        mHardwareService.setPreviewSettings(mDeviceRuntimeService.retrieveAvailablePreviewSettings());
        Uri number = call.getNumberUri();
        if (number == null || number.isEmpty()) {
            number = contact.getPhones().get(0).getNumber();
        }
        String callId = mCallService.placeCall(call.getAccount(), number.getUriString(), !call.isVideoMuted());
        if (callId == null || callId.isEmpty()) {
            return null;
        }
        call.setCallID(callId);
        Account account = mAccountService.getAccount(call.getAccount());
        if (account.isRing()
                || account.getDetailBoolean(ConfigKey.SRTP_ENABLE)
                || account.getDetailBoolean(ConfigKey.TLS_ENABLE)) {
            Log.i(TAG, "placeCall() call is secure");
            SecureSipCall secureCall = new SecureSipCall(call, account.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
            conf = new Conference(secureCall);
        } else {
            conf = new Conference(call);
        }
        conf.getParticipants().get(0).setContact(contact);
        conv.addConference(conf);

        return conf;
    }

    public void sendTextMessage(String account, Uri to, String txt) {
        long id = mCallService.sendAccountTextMessage(account, to.getRawUriString(), txt);
        Log.i(TAG, "sendAccountTextMessage " + txt + " got id " + id);
        TextMessage message = new TextMessage(false, txt, to, null, account);
        message.setID(id);
        message.read();
        mHistoryService.insertNewTextMessage(message);
    }

    public void sendTextMessage(Conference conf, String txt) {
        mCallService.sendTextMessage(conf.getId(), txt);
        SipCall call = conf.getParticipants().get(0);
        TextMessage message = new TextMessage(false, txt, call.getNumberUri(), conf.getId(), call.getAccount());
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
            mNotificationService.showTextNotification(contact, conversation, texts);
        }
    }

    public Conference getConference(String id) {
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

    private void parseNewMessage(TextMessage txt, String call) {
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
            String key = StringUtils.getRingIdFromNumber(call.getNumber());
            if (mConversationMap.containsKey(key)) {
                mConversationMap.get(key).addHistoryCall(call);
            } else if (acceptAllMessages) {
                CallContact contact = mContactService.findContact(call.getContactID(), call.getContactKey(), call.getNumber());
                Conversation conversation = new Conversation(contact);
                conversation.addHistoryCall(call);
                mConversationMap.put(key, conversation);
            }
        }
    }

    private void parseHistoryTexts(List<HistoryText> historyTexts, boolean acceptAllMessages) {
        for (HistoryText htext : historyTexts) {
            TextMessage msg = new TextMessage(htext);
            String key = StringUtils.getRingIdFromNumber(msg.getNumber());
            if (mConversationMap.containsKey(key)) {
                mConversationMap.get(key).addTextMessage(msg);
            } else if (acceptAllMessages) {
                CallContact contact = mContactService.findContact(htext.getContactID(), htext.getContactKey(), htext.getNumber());
                Conversation conversation = new Conversation(contact);
                conversation.addTextMessage(msg);
                mConversationMap.put(key, conversation);
            }
        }
    }

    private void addContactsDaemon() {
        mConversationMap.clear();

        for (CallContact contact : mContactService.getContactsNoBanned()) {
            String key = contact.getIds().get(0);
            String phone = contact.getPhones().get(0).getNumber().getRawUriString();
            if (!mConversationMap.containsKey(key) && !mConversationMap.containsKey(phone)) {
                mConversationMap.put(key, new Conversation(contact));
            }
        }
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

    private void subscribePresence() {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        Set<String> keys = mConversationMap.keySet();
        for (String key : keys) {
            Uri uri = new Uri(key);
            if (uri.isRingId()) {
                mPresenceService.subscribeBuddy(accountId, key, true);
            } else {
                Log.i(TAG, "Trying to subscribe to an invalid uri " + key);
            }
        }
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {

        ServiceEvent mEvent;

        if (observable instanceof HistoryService && event != null) {

            switch (event.getEventType()) {
                case INCOMING_MESSAGE:
                    TextMessage txt = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);
                    String call = event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class);

                    parseNewMessage(txt, call);
                    updateTextNotifications();

                    setChanged();
                    mEvent = new ServiceEvent(ServiceEvent.EventType.INCOMING_MESSAGE);
                    notifyObservers(mEvent);
                    break;
                case HISTORY_LOADED:
                    addContactsDaemon();
                    Account account = mAccountService.getCurrentAccount();
                    boolean acceptAllMessages = account.getDetailBoolean(ConfigKey.DHT_PUBLIC_IN);

                    List<HistoryCall> historyCalls = (List<HistoryCall>) event.getEventInput(ServiceEvent.EventInput.HISTORY_CALLS, ArrayList.class);
                    parseHistoryCalls(historyCalls, acceptAllMessages);

                    List<HistoryText> historyTexts = (List<HistoryText>) event.getEventInput(ServiceEvent.EventInput.HISTORY_TEXTS, ArrayList.class);
                    parseHistoryTexts(historyTexts, acceptAllMessages);

                    aggregateHistory();

                    subscribePresence();

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
                    String callId = event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class);
                    int newState = SipCall.stateFromString(event.getEventInput(ServiceEvent.EventInput.STATE, String.class));

                    if (newState == SipCall.State.INCOMING ||
                            newState == SipCall.State.OVER) {
                        mHistoryService.updateVCard();
                    }

                    Conversation conversation = null;
                    Conference found = null;

                    for (Conversation conv : mConversationMap.values()) {
                        Conference tconf = conv.getConference(callId);
                        if (tconf != null) {
                            conversation = conv;
                            found = tconf;
                            break;
                        }
                    }

                    if (found == null) {
                        Log.w(TAG, "CALL_STATE_CHANGED : Can't find conference " + callId);
                    } else {
                        SipCall call = found.getCallById(callId);
                        int oldState = call.getCallState();

                        Log.w(TAG, "CALL_STATE_CHANGED for " + callId + " : " + SipCall.stateToString(oldState) + " -> " + SipCall.stateToString(newState));

                        if (newState != oldState) {
                            Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + newState);
                            if ((call.isRinging() || newState == SipCall.State.CURRENT) && call.getTimestampStart() == 0) {
                                call.setTimestampStart(System.currentTimeMillis());
                            }
                            if (newState == SipCall.State.RINGING) {
                                try {
                                    mAccountService.sendProfile(callId, call.getAccount());
                                    Log.d(TAG, "send vcard " + call.getAccount());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error while sending profile", e);
                                }
                            }
                            call.setCallState(newState);
                        }

                        try {
                            call.setDetails((HashMap<String, String>) event.getEventInput(ServiceEvent.EventInput.DETAILS, HashMap.class));
                        } catch (Exception e) {
                            Log.w(TAG, "CALL_STATE_CHANGED Can't set call details.", e);
                        }

                        if (newState == SipCall.State.HUNGUP
                                || newState == SipCall.State.BUSY
                                || newState == SipCall.State.FAILURE
                                || newState == SipCall.State.OVER) {
                            if (newState == SipCall.State.HUNGUP) {
                                call.setTimestampEnd(System.currentTimeMillis());
                            }

                            mHistoryService.insertNewEntry(found);
                            conversation.addHistoryCall(new HistoryCall(call));
                            mNotificationService.cancelCallNotification(call);
                            found.removeParticipant(call);
                        } else {
                            mNotificationService.showCallNotification(found);
                        }
                        if (newState == SipCall.State.FAILURE || newState == SipCall.State.BUSY || newState == SipCall.State.HUNGUP) {
                            mCallService.hangUp(callId);
                        }
                        if (found.getParticipants().isEmpty()) {
                            conversation.removeConference(found);
                        }
                    }

                    mDeviceRuntimeService.updateAudioState(getCurrentCallingConf());

                    setChanged();
                    mEvent = new ServiceEvent(ServiceEvent.EventType.CALL_STATE_CHANGED);
                    notifyObservers(mEvent);

                    refreshConversations();
                    break;
                case INCOMING_CALL:
                    String callid = event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class);
                    String accountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                    Uri number = new Uri(event.getEventInput(ServiceEvent.EventInput.FROM, String.class));
                    CallContact contact = mContactService.findContactByNumber(number.getRawUriString());

                    Conversation conv = startConversation(contact);

                    SipCall call = new SipCall(callid, accountId, number, SipCall.Direction.INCOMING);
                    call.setContact(contact);

                    Account accountCall = mAccountService.getAccount(accountId);

                    Conference toAdd;
                    if (accountCall.useSecureLayer()) {
                        SecureSipCall secureCall = new SecureSipCall(call, accountCall.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
                        toAdd = new Conference(secureCall);
                    } else {
                        toAdd = new Conference(call);
                    }

                    conv.addConference(toAdd);
                    mNotificationService.showCallNotification(toAdd);

                    Map<String, StringMap> camSettings = mDeviceRuntimeService.retrieveAvailablePreviewSettings();
                    mHardwareService.setPreviewSettings(camSettings);

                    // Sending VCard when receiving a call
                    mAccountService.sendProfile(callid, accountId);

                    mDeviceRuntimeService.updateAudioState(getCurrentCallingConf());

                    setChanged();
                    ServiceEvent event1 = new ServiceEvent(ServiceEvent.EventType.INCOMING_CALL);
                    notifyObservers(event1);
                    break;
            }
        } else if (observable instanceof ContactService && event != null) {
            switch (event.getEventType()) {
                case CONTACTS_CHANGED:
                    refreshConversations();
                    break;
            }
        } else if (observable instanceof PresenceService && event != null) {
            switch (event.getEventType()) {
                case NEW_BUDDY_NOTIFICATION:
                    String contactID = event.getEventInput(ServiceEvent.EventInput.BUDDY_URI, String.class);
                    int status = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);
                    Conversation conversation = mConversationMap.get(CallContact.PREFIX_RING + contactID);
                    if (conversation != null && conversation.getContact() != null) {
                        conversation.getContact().setOnline(status == 1);
                        setChanged();
                        notifyObservers(new ServiceEvent(ServiceEvent.EventType.CONVERSATIONS_CHANGED));
                    }
                    break;
            }
        }
    }
}