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
import cx.ring.utils.Tuple;

/**
 * This facade handles the conversations
 * - Load from the history
 * - Keep a local cache of these conversations
 * <p>
 * Events are broadcasted:
 * - CONVERSATIONS_CHANGED
 */
public class ConversationFacade extends Observable {

    private final static String TAG = ConversationFacade.class.getName();

    @Inject
    AccountService mAccountService;

    @Inject
    ContactService mContactService;

    @Inject
    ConferenceService mConferenceService;

    @Inject
    HistoryService mHistoryService;

    @Inject
    CallService mCallService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    NotificationService mNotificationService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private Map<String, Conversation> mConversationMap;

    public ConversationFacade() {
        mConversationMap = new HashMap<>();
    }

    /**
     * Loads conversations from history calls and texts (also sends CONVERSATIONS_CHANGED event)
     */
    public void loadConversationsFromHistory() {

        List<HistoryCall> historyCalls;
        List<HistoryText> historyTexts;
        mConversationMap.clear();

        try {
            historyCalls = mHistoryService.getAll();
            historyTexts = mHistoryService.getAllTextMessages();
        } catch (SQLException e) {
            Log.e(TAG, "unable to retrieve history calls and texts", e);
            return;
        }

        for (HistoryCall call : historyCalls) {
            CallContact contact = mContactService.findContact(call.getContactID(), call.getContactKey(), call.getNumber());

            Map.Entry<String, Conversation> merge = null;
            for (Map.Entry<String, Conversation> ce : mConversationMap.entrySet()) {
                Conversation conversation = ce.getValue();
                if ((contact.getId() > 0 && contact.getId() == conversation.getContact().getId()) || conversation.getContact().hasNumber(call.getNumber())) {
                    merge = ce;
                    break;
                }
            }
            if (merge != null) {
                Conversation conversation = merge.getValue();
                if (conversation.getContact().getId() <= 0 && contact.getId() > 0) {
                    conversation.setContact(contact);
                    mConversationMap.remove(merge.getKey());
                    mConversationMap.put(contact.getIds().get(0), conversation);
                }
                conversation.addHistoryCall(call);
                continue;
            }
            String key = contact.getIds().get(0);
            if (mConversationMap.containsKey(key)) {
                mConversationMap.get(key).addHistoryCall(call);
            } else {
                Conversation conversation = new Conversation(contact);
                conversation.addHistoryCall(call);
                mConversationMap.put(key, conversation);
            }
        }

        for (HistoryText htext : historyTexts) {
            CallContact contact = mContactService.findContact(htext.getContactID(), htext.getContactKey(), htext.getNumber());
            Tuple<HistoryEntry, HistoryCall> p = findHistoryByCallId(mConversationMap, htext.getCallId());

            if (contact == null && p != null) {
                contact = p.first.getContact();
            }
            if (contact == null) {
                continue;
            }

            TextMessage msg = new TextMessage(htext);
            msg.setContact(contact);

            if (p != null) {
                if (msg.getNumberUri() == null) {
                    msg.setNumber(new Uri(p.second.getNumber()));
                }
                p.first.addTextMessage(msg);
            }

            String key = contact.getIds().get(0);
            if (mConversationMap.containsKey(key)) {
                mConversationMap.get(key).addTextMessage(msg);
            } else {
                Conversation c = new Conversation(contact);
                c.addTextMessage(msg);
                mConversationMap.put(key, c);
            }
        }

        Map<String, ArrayList<String>> conferences = mConferenceService.getConferenceList();

        for (Map.Entry<String, ArrayList<String>> conferenceEntry : conferences.entrySet()) {
            Conference conference = new Conference(conferenceEntry.getKey());
            for (String callId : conferenceEntry.getValue()) {
                SipCall call = getCall(callId).second;
                if (call == null) {
                    call = new SipCall(callId, mCallService.getCallDetails(callId));
                }
                Account acc = mAccountService.getAccount(call.getAccount());
                if (acc.isRing()
                        || acc.getDetailBoolean(ConfigKey.SRTP_ENABLE)
                        || acc.getDetailBoolean(ConfigKey.TLS_ENABLE)) {
                    call = new SecureSipCall(call, acc.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
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
        for (Conversation conversation : mConversationMap.values()) {
            Log.d(TAG, "Conversation : " + conversation.getContact().getId() + " " + conversation.getContact().getDisplayName() + " " + conversation.getLastNumberUsed(conversation.getLastAccountUsed()) + " " + conversation.getLastInteraction().toString());
        }

        for (CallContact contact : mContactService.getContacts()) {
            String key = contact.getIds().get(0);
            if (!mConversationMap.containsKey(key)) {
                mConversationMap.put(key, new Conversation(contact));
            }
        }

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONVERSATIONS_CHANGED);
        notifyObservers(event);
    }

    private Tuple<HistoryEntry, HistoryCall> findHistoryByCallId(final Map<String, Conversation> conversations, String id) {
        for (Conversation conversation : conversations.values()) {
            Tuple<HistoryEntry, HistoryCall> historyCall = conversation.findHistoryByCallId(id);
            if (historyCall != null) {
                return historyCall;
            }
        }
        return null;
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
        }

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONVERSATIONS_CHANGED);
        notifyObservers(event);

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

        setChanged();
        ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONVERSATIONS_CHANGED);
        notifyObservers(event);
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

    public Conversation getConversationFromMessage(TextMessage txt) {
        Conversation conv;
        String call = txt.getCallId();
        if (call != null && !call.isEmpty()) {
            conv = getConversationByCallId(call);
        } else {
            conv = startConversation(mContactService.findContactByNumber(txt.getNumberUri().getRawUriString()));
            txt.setContact(conv.getContact());
        }
        return conv;
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
        loadConversationsFromHistory();
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

}
