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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.inject.Inject;

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

    private final static String TAG = ConversationFacade.class.getSimpleName();

    private final AccountService mAccountService;

    private final ContactService mContactService;

    @Inject
    ConferenceService mConferenceService;

    private final HistoryService mHistoryService;

    private final CallService mCallService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    NotificationService mNotificationService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private final Map<String, Conversation> mConversationMap = new HashMap<>();

    public ConversationFacade(HistoryService historyService, CallService callService, ContactService contactService, AccountService accountService) {
        mHistoryService = historyService;
        mHistoryService.addObserver(this);
        mCallService = callService;
        mCallService.addObserver(this);
        mContactService = contactService;
        mContactService.addObserver(this);
        mAccountService = accountService;
        mAccountService.addObserver(this);
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
        if (contact != null) {
            ArrayList<String> keys = contact.getIds();
            for (String key : keys) {
                Conversation conversation = mConversationMap.get(key);
                if (conversation != null) {
                    return conversation;
                }
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
        Conversation conversation = getConversationByContact(contact);
        if (conversation == null) {
            conversation = new Conversation(contact);
            mConversationMap.put(contact.getIds().get(0), conversation);

            Account account = mAccountService.getCurrentAccount();
            if (account != null && account.isRing()) {
                Uri number = contact.getPhones().get(0).getNumber();
                if (number.isRingId()) {
                    mAccountService.lookupAddress(account.getAccountID(), "", number.getRawRingId());
                }
            }

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
        mHistoryService.getCallAndTextAsync();
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

    public void removeConversation(String id) {
        mConversationMap.remove(id);
    }

    private void parseNewMessage(TextMessage txt) {
        Conversation conversation;
        if (!StringUtils.isEmpty(txt.getCallId())) {
            conversation = getConversationByCallId(txt.getCallId());
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
            CallContact contact = mContactService.findContact(call.getContactID(), call.getContactKey(), new Uri(call.getNumber()));
            String key = contact.getIds().get(0);
            String phone = contact.getPhones().get(0).getNumber().getRawUriString();
            if (mConversationMap.containsKey(key) || mConversationMap.containsKey(phone)) {
                mConversationMap.get(key).addHistoryCall(call);
            } else if (acceptAllMessages) {
                Conversation conversation = new Conversation(contact);
                conversation.addHistoryCall(call);
                mConversationMap.put(key, conversation);
            }
        }
    }

    private void parseHistoryTexts(List<HistoryText> historyTexts, boolean acceptAllMessages) {
        for (HistoryText htext : historyTexts) {
            TextMessage msg = new TextMessage(htext);
            CallContact contact = mContactService.findContact(htext.getContactID(), htext.getContactKey(), new Uri(htext.getNumber()));
            String key = contact.getIds().get(0);
            String phone = contact.getPhones().get(0).getNumber().getRawUriString();
            if (mConversationMap.containsKey(key) || mConversationMap.containsKey(phone)) {
                mConversationMap.get(key).addTextMessage(msg);
            } else if (acceptAllMessages) {
                Conversation conversation = new Conversation(contact);
                conversation.addTextMessage(msg);
                mConversationMap.put(key, conversation);
            }
        }
    }


    private void addContacts(boolean acceptAllMessages) {
        ArrayList<CallContact> contacts;
        if (acceptAllMessages) {
            contacts = new ArrayList<>(mContactService.getContactsNoBanned());
        } else {
            contacts = new ArrayList<>(mContactService.getContactsDaemon());
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
                CallContact contact = call.getContact();
                if (call.getContact() == null) {
                    contact = mContactService.findContact(call.getNumberUri());
                    call.setContact(contact);
                }
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

    private void searchForRingIdInBlockchain() {
        final String currentAccountId = mAccountService.getCurrentAccount().getAccountID();
        for (Conversation conversation : mConversationMap.values()) {
            CallContact contact = conversation.getContact();
            if (contact == null) {
                continue;
            }
            Uri contactUri = contact.getPhones().get(0).getNumber();
            if (!contactUri.isRingId() || !StringUtils.isEmpty(contact.getUsername())) {
                continue;
            }
            boolean currentAccountChecked = false;
            for (String accountId : conversation.getAccountsUsed()) {
                Account account = mAccountService.getAccount(accountId);
                if (account == null || !account.isRing()) {
                    continue;
                }
                if (accountId.equals(currentAccountId)) {
                    currentAccountChecked = true;
                }
                mAccountService.lookupAddress(accountId, "", contactUri.getRawRingId());
            }
            if (!currentAccountChecked) {
                mAccountService.lookupAddress(currentAccountId, "", contactUri.getRawRingId());
            }
        }
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {

        if (event == null) {
            return;
        }

        ServiceEvent mEvent;

        if (observable instanceof HistoryService) {

            switch (event.getEventType()) {
                case INCOMING_MESSAGE: {
                    TextMessage txt = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);

                    parseNewMessage(txt);
                    updateTextNotifications();

                    setChanged();
                    mEvent = new ServiceEvent(ServiceEvent.EventType.INCOMING_MESSAGE);
                    notifyObservers(mEvent);
                    break;
                }
                case ACCOUNT_MESSAGE_STATUS_CHANGED: {
                    TextMessage newMsg = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);
                    Conversation conv = getConversationByContact(mContactService.findContactByNumber(newMsg.getNumber()));
                    if (conv != null) {
                        conv.updateTextMessage(newMsg);
                    }
                    setChanged();

                    mEvent = new ServiceEvent(ServiceEvent.EventType.INCOMING_MESSAGE);
                    notifyObservers(mEvent);
                    break;
                }
                case HISTORY_LOADED:
                    Account account = mAccountService.getCurrentAccount();
                    if (account != null) {
                        boolean acceptAllMessages = account.getDetailBoolean(ConfigKey.DHT_PUBLIC_IN);

                        mConversationMap.clear();

                        addContacts(acceptAllMessages);

                        List<HistoryCall> historyCalls = (List<HistoryCall>) event.getEventInput(ServiceEvent.EventInput.HISTORY_CALLS, ArrayList.class);
                        parseHistoryCalls(historyCalls, acceptAllMessages);

                        List<HistoryText> historyTexts = (List<HistoryText>) event.getEventInput(ServiceEvent.EventInput.HISTORY_TEXTS, ArrayList.class);
                        parseHistoryTexts(historyTexts, acceptAllMessages);

                        aggregateHistory();

                        searchForRingIdInBlockchain();
                    }

                    setChanged();
                    mEvent = new ServiceEvent(ServiceEvent.EventType.CONVERSATIONS_CHANGED);
                    notifyObservers(mEvent);
                    break;
                case HISTORY_MODIFIED:
                    refreshConversations();
                    break;
            }
        } else if (observable instanceof CallService) {
            Conversation conversation = null;
            Conference conference = null;
            SipCall call;
            switch (event.getEventType()) {
                case CALL_STATE_CHANGED:
                    call = event.getEventInput(ServiceEvent.EventInput.CALL, SipCall.class);
                    if (call == null) {
                        Log.w(TAG, "CALL_STATE_CHANGED : call is null");
                        return;
                    }
                    int newState = call.getCallState();
                    mDeviceRuntimeService.updateAudioState(call.isRinging() && call.isIncoming());

                    for (Conversation conv : mConversationMap.values()) {
                        conference = conv.getConference(call.getCallId());
                        if (conference != null) {
                            conversation = conv;
                            Log.w(TAG, "CALL_STATE_CHANGED : found conversation " + call.getCallId());
                            break;
                        }
                    }

                    if (conversation == null) {
                        conversation = startConversation(call.getContact());
                        conference = new Conference(call);
                        conversation.addConference(conference);
                    }

                    Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + newState);
                    if ((call.isRinging() || newState == SipCall.State.CURRENT) && call.getTimestampStart() == 0) {
                        call.setTimestampStart(System.currentTimeMillis());
                    }

                    if ((newState == SipCall.State.CURRENT && call.isIncoming())
                            || newState == SipCall.State.RINGING && call.isOutGoing()) {
                        mAccountService.sendProfile(call.getCallId(), call.getAccount());
                    } else if (newState == SipCall.State.HUNGUP
                            || newState == SipCall.State.BUSY
                            || newState == SipCall.State.FAILURE
                            || newState == SipCall.State.OVER) {
                        mNotificationService.cancelCallNotification(call.getCallId().hashCode());
                        mDeviceRuntimeService.closeAudioState();

                        if (newState == SipCall.State.HUNGUP) {
                            call.setTimestampEnd(System.currentTimeMillis());
                        }
                        if (call.getTimestampStart() == 0) {
                            call.setTimestampStart(System.currentTimeMillis());
                        }
                        if (call.getTimestampEnd() == 0) {
                            call.setTimestampEnd(System.currentTimeMillis());
                        }

                        mHistoryService.insertNewEntry(conference);
                        conference.removeParticipant(call);
                        conversation.addHistoryCall(new HistoryCall(call));
                        mCallService.removeCallForId(call.getCallId());
                    }
                    if (conference.getParticipants().isEmpty()) {
                        conversation.removeConference(conference);
                    }

                    setChanged();
                    mEvent = new ServiceEvent(ServiceEvent.EventType.CALL_STATE_CHANGED);
                    notifyObservers(mEvent);
                    break;
                case INCOMING_CALL:
                    call = event.getEventInput(ServiceEvent.EventInput.CALL, SipCall.class);
                    conversation = startConversation(call.getContact());
                    conference = new Conference(call);

                    conversation.addConference(conference);
                    mNotificationService.showCallNotification(conference);

                    mHardwareService.setPreviewSettings();

                    Conference currenConf = getCurrentCallingConf();
                    mDeviceRuntimeService.updateAudioState(currenConf.isRinging()
                            && currenConf.isIncoming());

                    setChanged();
                    ServiceEvent event1 = new ServiceEvent(ServiceEvent.EventType.INCOMING_CALL);
                    notifyObservers(event1);
                    break;
            }
        } else if (observable instanceof ContactService) {
            switch (event.getEventType()) {
                case CONTACTS_CHANGED:
                    refreshConversations();
                    break;
            }
        } else if (observable instanceof AccountService) {
            switch (event.getEventType()) {
                case REGISTERED_NAME_FOUND: {
                    int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);
                    if (state != 0) {
                        break;
                    }
                    String accountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                    String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                    Uri address = new Uri(event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class));
                    if (mContactService.setRingContactName(accountId, address, name)) {
                        setChanged();
                        notifyObservers(new ServiceEvent(ServiceEvent.EventType.USERNAME_CHANGED));
                    }
                    break;
                }
            }
        }
    }
}