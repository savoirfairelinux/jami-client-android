/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
import java.util.List;

import javax.inject.Inject;

import cx.ring.daemon.Blob;
import cx.ring.daemon.DataTransferInfo;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransferError;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryFileTransfer;
import cx.ring.model.HistoryText;
import cx.ring.model.RingError;
import cx.ring.model.ServiceEvent;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.services.DataTransferWrapper;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.VCardService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.observers.ResourceSingleObserver;
import io.reactivex.schedulers.Schedulers;

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
    private DeviceRuntimeService mDeviceRuntimeService;

    private Conversation mConversation;
    private String mContactRingId;
    private String mAccountId;

    private boolean hasContactRequestPopupShown = false;
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
                                 DeviceRuntimeService mDeviceRuntimeService,
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
        this.mDeviceRuntimeService = mDeviceRuntimeService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mHistoryService.removeObserver(this);
        mCallService.removeObserver(this);
    }

    public void init(String contactRingId, String accountId) {
        mContactRingId = contactRingId;
        mAccountId = accountId;

        mCurrentContact = mContactService.getContact(new Uri(mContactRingId));
        this.mConversation = new Conversation(mCurrentContact);

        mAccountService.addObserver(this);
        mHistoryService.addObserver(this);
        mCallService.addObserver(this);
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
            Conference conference = mConversation == null ? null : mConversation.getCurrentCall();
            TextMessage txtMessage;
            if (conference == null || !conference.isOnGoing()) {
                long id = mCallService.sendAccountTextMessage(mAccountId, mContactRingId, message);
                txtMessage = new TextMessage(false, message, new Uri(mContactRingId), null, mAccountId);
                txtMessage.setID(id);
            } else {
                mCallService.sendTextMessage(conference.getId(), message);
                SipCall call = conference.getParticipants().get(0);
                txtMessage = new TextMessage(false, message, call.getNumberUri(), conference.getId(), call.getAccount());
            }
            txtMessage.read();
            mHistoryService.insertNewTextMessage(txtMessage);
            mConversation.addTextMessage(txtMessage);

            if (mCurrentContact.getStatus() == CallContact.Status.NO_REQUEST) {
                sendTrustRequest();
            }

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
        Uri uri = new Uri(mContactRingId);
        DataTransferInfo dataTransferInfo = new DataTransferInfo();
        dataTransferInfo.setAccountId(mAccountId);
        dataTransferInfo.setPeer(uri.getHost());
        dataTransferInfo.setPath(filePath);
        dataTransferInfo.setDisplayName(file.getName());
        mCallService.sendFile(0L, dataTransferInfo);
    }

    public void sendTrustRequest() {
        VCard vCard = mVCardService.loadSmallVCard(mAccountId);
        mAccountService.sendTrustRequest(mAccountId, new Uri(mContactRingId).getRawRingId(), Blob.fromString(VCardUtils.vcardToString(vCard)));
    }

    public void blockContact() {
        mAccountService.removeContact(mAccountId, new Uri(mContactRingId).getRawRingId(), true);
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
            getView().goToCallActivityWithResult(mAccountId, mContactRingId, audioOnly);
        }
    }

    public void deleteConversation() {
        mHistoryService.clearHistoryForConversation(mConversation);
        getView().goToHome();
    }

    private void loadHistory() {

        //Get all messages and calls to create a conversation
        mCompositeDisposable.add(mHistoryService.getAllTextMessagesForAccountAndContactRingId(mAccountId, mContactRingId)
                .zipWith(mHistoryService.getAllCallsForAccountAndContactRingId(mAccountId, mContactRingId),
                        new BiFunction<List<HistoryText>, List<HistoryCall>, Conversation>() {
                            @Override
                            public Conversation apply(@NonNull List<HistoryText> historyTexts,
                                                      @NonNull List<HistoryCall> historyCalls) throws Exception {

                                mConversation.removeAll();

                                for (HistoryCall call : historyCalls) {
                                    mConversation.addHistoryCall(call);
                                }

                                for (HistoryText htext : historyTexts) {
                                    TextMessage msg = new TextMessage(htext);
                                    mConversation.addTextMessage(msg);
                                }

                                Uri uri = new Uri(mContactRingId);
                                List<HistoryFileTransfer> historyFileTransfers = mHistoryService.getFileTransfers(mAccountId, uri.getRawRingId());
                                mConversation.addFileTransfers(historyFileTransfers);

                                return mConversation;
                            }
                        })
                .subscribeOn(Schedulers.computation())
                .observeOn(mMainScheduler)
                .subscribeWith(new ResourceSingleObserver<Conversation>() {
                    @Override
                    public void onSuccess(@NonNull Conversation conversation) {
                        mConversation = conversation;

                        SipCall sipCall = mCallService.getCurrentCallForContactId(mContactRingId);
                        if (sipCall != null && sipCall.getCallState() != SipCall.State.INACTIVE) {
                            mConversation.addConference(new Conference(sipCall));
                            getView().displayOnGoingCallPane(true);
                        } else {
                            mConversation.removeConference(mConversation.getCurrentCall());
                            getView().displayOnGoingCallPane(false);
                        }

                        getView().hideNumberSpinner();
                        Log.d(TAG, "loadHistory: AggregateHistorySize=" + mConversation.getAggregateHistory().size());
                        getView().refreshView(mConversation);
                        if (!hasContactRequestPopupShown) {
                            checkContact();
                            hasContactRequestPopupShown = true;
                        }

                        mHistoryService.readMessages(mConversation);
                        Conversation localConversation = mConversationFacade.getConversationByContact(mCurrentContact);
                        if (localConversation != null) {
                            localConversation.setVisible(true);
                        }
                        mNotificationService.cancelTextNotification(mContactRingId);

                        getView().displayContactPhoto(mConversation.getContact().getPhoto());
                        getView().displayContactName(mConversation.getContact());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, e.toString());
                    }
                }));
    }

    private void checkContact() {
        CallContact contact = mContactService.findContact(new Uri(mContactRingId));
        if (contact != null && CallContact.Status.CONFIRMED.equals(contact.getStatus())) {
            return;
        }

        getView().displaySendTrustRequest(mAccountId);
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
        } else if (observable instanceof HistoryService && event != null) {
            switch (event.getEventType()) {
                case INCOMING_MESSAGE:
                    TextMessage txt = event.getEventInput(ServiceEvent.EventInput.MESSAGE, TextMessage.class);
                    mConversation.addTextMessage(txt);
                    mHistoryService.readMessages(mConversation);
                    Log.d(TAG, "update: AggregateHistorySize=" + mConversation.getAggregateHistory().size());
                    getView().refreshView(mConversation);
                    break;
            }
        } else if (observable instanceof CallService && event != null) {
            switch (event.getEventType()) {
                case DATA_TRANSFER:
                    Log.d(TAG, "update: data transfer callback received");

                    Long transferId = event.getEventInput(ServiceEvent.EventInput.TRANSFER_ID, Long.class);
                    DataTransferEventCode transferEventCode = event.getEventInput(ServiceEvent.EventInput.TRANSFER_EVENT_CODE, DataTransferEventCode.class);

                    handleDataTransferEvent(transferId, transferEventCode);
                    break;
                case INCOMING_CALL:
                case CALL_STATE_CHANGED:
                    loadHistory();
                    break;
            }
        }
    }

    private void handleDataTransferEvent(Long transferId, DataTransferEventCode transferEventCode) {

        // find corresponding transfer
        mConversation.updateFileTransfer(transferId, transferEventCode);

        DataTransferInfo dataTransferInfo = new DataTransferInfo();
        DataTransferWrapper dataTransferWrapper = null;
        if (transferEventCode == DataTransferEventCode.CREATED || transferEventCode == DataTransferEventCode.FINISHED) {
            dataTransferWrapper = mCallService.dataTransferInfo(transferId, dataTransferInfo);

            if (dataTransferWrapper.getDataTransferError() != DataTransferError.SUCCESS) {
                Log.e(TAG, "handleDataTransferEvent: an error occurred during data transfer " + dataTransferWrapper.getDataTransferError().name());
                return;
            }
        }

        Log.d(TAG, "handleDataTransferEvent: " + transferEventCode.name());
        switch (transferEventCode) {
            case CREATED:
                mConversation.addFileTransfer(transferId, dataTransferInfo.getDisplayName(),
                        dataTransferWrapper.isOutgoing(), dataTransferInfo.getTotalSize(),
                        dataTransferInfo.getBytesProgress(), dataTransferInfo.getPeer(),
                        dataTransferInfo.getAccountId());
                break;
            case FINISHED:
                if (!dataTransferWrapper.isOutgoing()) {
                    getView().writeCacheFile(dataTransferInfo.getDisplayName());
                }
                break;
        }

        Log.d(TAG, "handleDataTransferEvent: AggregateHistorySize=" + mConversation.getAggregateHistory().size() + ", transferEventCode=" + transferEventCode);
        getView().refreshView(mConversation);
    }

    public void acceptTransfer(Long dataTransferId, String filePath) {
        Log.d(TAG, "acceptTransfer: dataTransferId=" + dataTransferId);
        mCallService.acceptFileTransfer(dataTransferId, filePath, 0);
    }

    public void cancelTransfer(Long dataTransferId) {
        Log.d(TAG, "cancelTransfer: dataTransferId=" + dataTransferId);
        mCallService.cancelDataTransfer(dataTransferId);
    }

    public DeviceRuntimeService getDeviceRuntimeService() {
        return mDeviceRuntimeService;
    }

    public void noSpaceLeft() {
        Log.e(TAG, "configureForFileInfoTextMessage: no space left on device");
        getView().displayErrorToast(RingError.NO_SPACE_LEFT);
    }
}