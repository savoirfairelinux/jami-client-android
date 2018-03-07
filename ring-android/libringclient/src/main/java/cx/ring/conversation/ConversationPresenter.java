/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.conversation;

import java.io.File;
import java.util.Iterator;

import javax.inject.Inject;

import cx.ring.daemon.Blob;
import cx.ring.daemon.DataTransferInfo;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.RingError;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
import cx.ring.services.VCardService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Scheduler;

public class ConversationPresenter extends RootPresenter<ConversationView> implements Observer<ServiceEvent> {

    private static final String TAG = ConversationPresenter.class.getSimpleName();
    private ContactService mContactService;

    private AccountService mAccountService;
    private HistoryService mHistoryService;
    private NotificationService mNotificationService;
    private HardwareService mHardwareService;
    private ConversationFacade mConversationFacade;
    private CallService mCallService;
    private Scheduler mMainScheduler;
    private VCardService mVCardService;
    private PreferencesService mPreferencesService;
    private DeviceRuntimeService mDeviceRuntimeService;

    private Conversation mConversation;
    private Uri mContactRingId;
    private String mAccountId;

    private CallContact mCurrentContact;

    @Inject
    public ConversationPresenter(ContactService mContactService,
                                 AccountService mAccountService,
                                 HistoryService mHistoryService,
                                 CallService callService,
                                 NotificationService notificationService,
                                 HardwareService hardwareService,
                                 ConversationFacade conversationFacade,
                                 VCardService vCardService,
                                 PreferencesService preferencesService,
                                 DeviceRuntimeService deviceRuntimeService,
                                 Scheduler mainScheduler) {
        this.mContactService = mContactService;
        this.mAccountService = mAccountService;
        this.mHistoryService = mHistoryService;
        this.mCallService = callService;
        this.mMainScheduler = mainScheduler;
        this.mNotificationService = notificationService;
        this.mHardwareService = hardwareService;
        this.mConversationFacade = conversationFacade;
        this.mVCardService = vCardService;
        this.mPreferencesService = preferencesService;
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mHistoryService.removeObserver(this);
        mCallService.removeObserver(this);
        mConversationFacade.removeObserver(this);
    }

    public void init(String contactRingId, String accountId) {
        init(new Uri(contactRingId), accountId);
    }

    public void init(Uri contactRingId, String accountId) {
        mContactRingId = contactRingId;
        mAccountId = accountId;

        mCurrentContact = mContactService.getContact(mContactRingId);
        this.mConversation = new Conversation(mCurrentContact);

        mAccountService.addObserver(this);
        mHistoryService.addObserver(this);
        mCallService.addObserver(this);
        mConversationFacade.addObserver(this);
    }

    public void pause() {
        if (mConversation != null) {
            Conversation localConversation = mConversationFacade.getConversationByContact(mCurrentContact);
            if (localConversation != null) {
                localConversation.setVisible(false);
            }
        }
    }

    public void resume() {
        loadHistory();

        TrustRequest incomingTrustRequests = getIncomingTrustRequests();
        if (incomingTrustRequests == null) {
            getView().switchToConversationView();
        } else {
            getView().switchToIncomingTrustRequestView(incomingTrustRequests.getDisplayname());
        }
    }

    public void prepareMenu() {
        getView().displayAddContact(mConversation != null && mConversation.getContact().getId() < 0);
    }

    public void addContact() {
        getView().goToAddContact(mConversation.getContact());
    }

    public void deleteAction() {
        getView().displayDeleteDialog(mConversation);
    }

    public void copyToClipboard() {
        getView().displayCopyToClipboard(mConversation.getContact());
    }

    public void sendTextMessage(String message) {
        if (message != null && !message.equals("")) {
            getView().clearMsgEdit();
            if (mConversation == null)
                mConversation = mConversationFacade.startConversation(mCurrentContact);
            Conference conference = mConversation.getCurrentCall();
            TextMessage txtMessage;
            if (conference == null || !conference.isOnGoing()) {
                long id = mCallService.sendAccountTextMessage(mAccountId, mContactRingId.getRawUriString(), message);
                txtMessage = new TextMessage(false, message, mContactRingId, null, mAccountId);
                txtMessage.setID(id);
            } else {
                mCallService.sendTextMessage(conference.getId(), message);
                SipCall call = conference.getParticipants().get(0);
                txtMessage = new TextMessage(false, message, call.getNumberUri(), conference.getId(), call.getAccount());
            }
            txtMessage.read();
            mHistoryService.insertNewTextMessage(txtMessage);
            mConversation.addTextMessage(txtMessage);

            checkTrustRequestStatus();

            Log.d(TAG, "sendTextMessage: AggregateHistorySize=" + mConversation.getAggregateHistory().size());
            getView().refreshView(mConversation);
        }
    }

    public void selectFile() {
        getView().openFilePicker();
    }

    public void sendFile(String filePath) {
        Log.d(TAG, "sendFile: sending file at " + filePath + " from accountId " + mAccountId + " to " + mContactRingId);

        if (filePath == null) {
            return;
        }

        // check file
        File file = new File(filePath);
        if (!file.exists()) {
            Log.d(TAG, "sendFile: file not found");
            return;
        }

        if (!file.canRead()) {
            Log.d(TAG, "sendFile: file not readable");
            return;
        }

        // send file
        DataTransferInfo dataTransferInfo = new DataTransferInfo();
        dataTransferInfo.setAccountId(mAccountId);
        dataTransferInfo.setPeer(mContactRingId.getHost());
        dataTransferInfo.setPath(filePath);
        dataTransferInfo.setDisplayName(file.getName());
        mAccountService.sendFile(0L, dataTransferInfo);
    }

    public void sendTrustRequest() {
        VCard vCard = mVCardService.loadSmallVCard(mAccountId);
        mAccountService.sendTrustRequest(mAccountId, mContactRingId.getRawRingId(), Blob.fromString(VCardUtils.vcardToString(vCard)));

        CallContact contact = mContactService.findContact(mContactRingId);
        if (contact == null) {
            Log.e(TAG, "sendTrustRequest: not able to find contact");
            return;
        }
        contact.setStatus(CallContact.Status.REQUEST_SENT);
    }

    public void blockContact() {
        mAccountService.removeContact(mAccountId, mContactRingId.getHost(), true);
        getView().goToHome();
    }

    public void clickOnGoingPane() {
        Conference conf = mConversation.getCurrentCall();
        if (conf != null) {
            getView().goToCallActivity(conf.getId());
        } else {
            getView().displayOnGoingCallPane(false);
        }
    }

    public void callWithAudioOnly(boolean audioOnly) {
        if (audioOnly && !mHardwareService.hasMicrophone()) {
            getView().displayErrorToast(RingError.NO_MICROPHONE);
            return;
        }

        Conference conf = mConversation.getCurrentCall();

        if (conf != null && (conf.getParticipants().get(0).getCallState() == SipCall.State.INACTIVE
                || conf.getParticipants().get(0).getCallState() == SipCall.State.FAILURE)) {
            mConversation.removeConference(conf);
            conf = null;
        }

        if (conf != null) {
            getView().goToCallActivity(conf.getId());
        } else {
            getView().goToCallActivityWithResult(mAccountId, mContactRingId.getRawUriString(), audioOnly);
        }
    }

    public void deleteConversation() {
        mHistoryService.clearHistoryForConversation(mConversation);
        getView().goToHome();
    }

    private void loadHistory() {
        mConversation = mConversationFacade.startConversation(mCurrentContact);
        mConversation.setVisible(true);
        mHistoryService.readMessages(mConversation);
        mNotificationService.cancelTextNotification(mContactRingId.getRawUriString());
        getView().hideNumberSpinner();
        getView().displayContactPhoto(mConversation.getContact().getPhoto());
        getView().displayContactName(mConversation.getContact());
        getView().refreshView(mConversation);
        Conference conf = mConversation.getCurrentCall();
        if (conf != null && conf.getState() != SipCall.State.INACTIVE) {
            getView().displayOnGoingCallPane(true);
        } else {
            getView().displayOnGoingCallPane(false);
        }
    }

    private void checkTrustRequestStatus() {
        if (getIncomingTrustRequests() != null) {
            Log.d(TAG, "checkTrustRequestStatus: null getIncomingTrustRequests()");
            return;
        }
        if (mCurrentContact != null && CallContact.Status.NO_REQUEST.equals(mCurrentContact.getStatus())) {
            sendTrustRequest();
        }
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (observable instanceof AccountService && event != null) {
            if (event.getEventType() == ServiceEvent.EventType.REGISTERED_NAME_FOUND) {
                final String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                final String address = event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                final int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);

                getView().updateView(address, name, state);
            }
        } else if (observable instanceof ConversationFacade && event != null) {
            switch (event.getEventType()) {
                case DATA_TRANSFER:
                    getView().refreshView(mConversation);
                    break;
                case INCOMING_MESSAGE:
                    getView().refreshView(mConversation);
                    break;
            }
        }
    }

    private TrustRequest getIncomingTrustRequests() {
        if (mAccountService == null) {
            Log.d(TAG, "getIncomingTrustRequests: not able to get account service");
            return null;
        }
        Account currentAccount = mAccountService.getCurrentAccount();
        if (currentAccount == null) {
            Log.d(TAG, "getIncomingTrustRequests: not able to get current account");
            return null;
        }
        return currentAccount.getRequest(mContactRingId.getHost());
    }

    public void onBlockIncomingContactRequest() {
        String accountId = mAccountId == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountId;
        mAccountService.discardTrustRequest(accountId, mContactRingId.getHost());
        mAccountService.removeContact(accountId, mContactRingId.getHost(), true);
        mPreferencesService.removeRequestPreferences(accountId, mContactRingId.getHost());

        getView().goToHome();
    }

    public void onRefuseIncomingContactRequest() {
        String accountId = mAccountId == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountId;
        mAccountService.discardTrustRequest(accountId, mContactRingId.getHost());
        mPreferencesService.removeRequestPreferences(accountId, mContactRingId.getHost());

        getView().goToHome();
    }

    public void onAcceptIncomingContactRequest() {
        String currentAccountId = mAccountId == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountId;
        mAccountService.acceptTrustRequest(currentAccountId, mContactRingId.getHost());
        mPreferencesService.removeRequestPreferences(mAccountId, mContactRingId.getHost());

        for (Iterator<TrustRequest> it = mAccountService.getCurrentAccount().getRequests().iterator(); it.hasNext(); ) {
            TrustRequest request = it.next();
            if (mAccountId.equals(request.getAccountId()) && mAccountId.equals(request.getContactId())) {
                VCard vCard = request.getVCard();
                if (vCard != null) {
                    VCardUtils.savePeerProfileToDisk(vCard, mContactRingId.getHost() + ".vcf", mDeviceRuntimeService.provideFilesDir());
                }
                it.remove();
            }
        }

        getView().switchToConversationView();
    }

    public DeviceRuntimeService getDeviceRuntimeService() {
        return mDeviceRuntimeService;
    }

    public void noSpaceLeft() {
        Log.e(TAG, "configureForFileInfoTextMessage: no space left on device");
        getView().displayErrorToast(RingError.NO_SPACE_LEFT);
    }
}