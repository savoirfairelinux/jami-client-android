/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Blob;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransfer;
import cx.ring.model.RingError;
import cx.ring.model.SipCall;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
import cx.ring.services.VCardService;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ConversationPresenter extends RootPresenter<ConversationView> {

    private static final String TAG = ConversationPresenter.class.getSimpleName();
    private ContactService mContactService;

    private AccountService mAccountService;
    private HistoryService mHistoryService;
    private NotificationService mNotificationService;
    private HardwareService mHardwareService;
    private ConversationFacade mConversationFacade;
    private VCardService mVCardService;
    private PreferencesService mPreferencesService;
    private DeviceRuntimeService mDeviceRuntimeService;

    private Conversation mConversation;
    private Uri mContactRingId;
    private String mAccountId;

    private CompositeDisposable mConversationDisposable;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public ConversationPresenter(ContactService contactService,
                                 AccountService accountService,
                                 HistoryService historyService,
                                 NotificationService notificationService,
                                 HardwareService hardwareService,
                                 ConversationFacade conversationFacade,
                                 VCardService vCardService,
                                 PreferencesService preferencesService,
                                 DeviceRuntimeService deviceRuntimeService) {
        this.mContactService = contactService;
        this.mAccountService = accountService;
        this.mHistoryService = historyService;
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
    }

    @Override
    public void bindView(ConversationView view) {
        super.bindView(view);
    }

    public void init(String contactRingId, String accountId) {
        init(new Uri(contactRingId), accountId);
    }

    public void init(Uri contactRingId, String accountId) {
        Log.w(TAG, "init " + contactRingId + " " + accountId);
        mContactRingId = contactRingId;
        mAccountId = accountId;
        setConversation(mConversationFacade.startConversation(accountId, contactRingId));
    }

    private void setConversation(final Conversation conversation) {
        if (conversation == null || mConversation == conversation)
            return;
        mConversation = conversation;
        if (getView() != null)
            initView(conversation, getView());
    }

    public void pause() {
        if (mConversation != null) {
            mConversation.setVisible(false);
        }
    }

    public void resume() {
        if (mConversation != null) {
            mConversation.setVisible(true);
            mConversationFacade.readMessages(mConversation);
            mNotificationService.cancelTextNotification(mContactRingId.getRawUriString());
        }
    }

    private CallContact initContact(final Account account, final Uri uri, final ConversationView view) {
        String rawId = uri.getRawRingId();
        CallContact contact = account.getContact(rawId);
        if (contact == null) {
            contact = account.getContactFromCache(uri);
            TrustRequest req = account.getRequest(rawId);
            if (req == null) {
                getView().switchToUnknownView(contact.getRingUsername());
            } else {
                getView().switchToIncomingTrustRequestView(req.getDisplayname());
            }
        } else {
            getView().switchToConversationView();
        }
        Log.w(TAG, "initContact " + contact.getUsername());
        if (contact.getUsername() == null) {
            mAccountService.lookupAddress(mAccountId, "", rawId);
        }
        view.displayContactPhoto(contact.getPhoto());
        view.displayContactName(contact);
        return contact;
    }

    private void initView(final Conversation c, final ConversationView view) {
        Log.w(TAG, "initView");
        if (mConversationDisposable == null) {
            mConversationDisposable = new CompositeDisposable();
            mCompositeDisposable.add(mConversationDisposable);
        }
        mConversationDisposable.clear();
        view.hideNumberSpinner();

        Account account = mAccountService.getAccount(mAccountId);
        CallContact contact = initContact(account, mContactRingId, view);

        mConversationDisposable.add(c.getSortedHistory()
                .subscribeOn(Schedulers.computation())
                .observeOn(mUiScheduler)
                .subscribe(view::refreshView));
        mConversationDisposable.add(contact.getUpdates()
                .observeOn(mUiScheduler)
                .subscribe(r -> initContact(account, mContactRingId, view)));
        mConversationDisposable.add(c.getNewElements()
                .observeOn(mUiScheduler)
                .subscribe(view::addElement));
        mConversationDisposable.add(c.getUpdatedElements()
                .observeOn(mUiScheduler)
                .subscribe(view::updateElement));
        mConversationDisposable.add(c.getRemovedElements()
                .observeOn(mUiScheduler)
                .subscribe(view::removeElement));
        mConversationDisposable.add(c.getCalls()
                .observeOn(mUiScheduler)
                .subscribe(calls -> updateOngoingCallView()));
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
        if (StringUtils.isEmpty(message)) {
            return;
        }
        Conference conference = mConversation.getCurrentCall();
        if (conference == null || !conference.isOnGoing()) {
            mConversationFacade.sendTextMessage(mAccountId, mConversation, mContactRingId, message);
        } else {
            mConversationFacade.sendTextMessage(mConversation, conference, message);
        }
    }

    public void selectFile() {
        getView().openFilePicker();
    }

    public void sendFile(File file) {
        mConversationFacade.sendFile(mAccountId, mContactRingId, file);
    }

    public boolean downloadFile(DataTransfer transfer, File dest) {
        if (!transfer.isComplete())
            return false;
        File file = getDeviceRuntimeService().getConversationPath(transfer.getPeerId(), transfer.getStoragePath());
        if (FileUtils.copyFile(file, dest)) {
            Log.w(TAG, "Copied file to " + dest.getAbsolutePath() + " (" + FileUtils.readableFileSize(file.length()) + ")");
            return true;
        }
        return false;
    }

    public void shareFile(DataTransfer file) {
        File path = getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
        getView().shareFile(path);
    }

    public void deleteFile(DataTransfer transfer) {
        File file = getDeviceRuntimeService().getConversationPath(transfer.getPeerId(), transfer.getStoragePath());
        file.delete();
        mConversationFacade.deleteFile(transfer);
        mHistoryService.deleteFileHistory(transfer.getId());
    }

    public void sendTrustRequest() {
        VCard vCard = mVCardService.loadSmallVCard(mAccountId);
        mAccountService.sendTrustRequest(mAccountId, mContactRingId.getRawRingId(), Blob.fromString(VCardUtils.vcardToString(vCard)));

        CallContact contact = mContactService.findContact(mAccountId, mContactRingId);
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

        if (conf != null && (conf.getParticipants().isEmpty()
                || conf.getParticipants().get(0).getCallState() == SipCall.State.INACTIVE
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
        mConversationFacade.clearHistory(mAccountId, mContactRingId).subscribe();
        getView().goToHome();
    }

    private void updateOngoingCallView() {
        Conference conf = mConversation.getCurrentCall();
        if (conf != null && conf.getState() != SipCall.State.INACTIVE) {
            getView().displayOnGoingCallPane(true);
        } else {
            getView().displayOnGoingCallPane(false);
        }
        getView().scrollToEnd();
    }

    private TrustRequest getIncomingTrustRequests() {
        if (mAccountService != null) {
            Account acc = mAccountService.getAccount(mAccountId);
            if (acc != null) {
                return acc.getRequest(mContactRingId.getHost());
            }
        }
        return null;
    }

    public void onBlockIncomingContactRequest() {
        String accountId = mAccountId == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountId;
        mConversationFacade.discardRequest(accountId, mContactRingId);
        mAccountService.removeContact(accountId, mContactRingId.getHost(), true);

        getView().goToHome();
    }

    public void onRefuseIncomingContactRequest() {
        String accountId = mAccountId == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountId;

        mConversationFacade.discardRequest(accountId, mContactRingId);
        getView().goToHome();
    }

    public void onAcceptIncomingContactRequest() {
        mConversationFacade.acceptRequest(mAccountId, mContactRingId);
        getView().switchToConversationView();
    }

    public void onAddContact() {
        sendTrustRequest();
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