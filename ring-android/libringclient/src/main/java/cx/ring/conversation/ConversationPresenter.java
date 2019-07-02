/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import cx.ring.model.ConversationElement;
import cx.ring.model.DataTransfer;
import cx.ring.model.RingError;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.VCardService;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import cx.ring.utils.VCardUtils;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class ConversationPresenter extends RootPresenter<ConversationView> {

    private static final String TAG = ConversationPresenter.class.getSimpleName();
    private final ContactService mContactService;
    private final AccountService mAccountService;
    private final HardwareService mHardwareService;
    private final ConversationFacade mConversationFacade;
    private final VCardService mVCardService;
    private final DeviceRuntimeService mDeviceRuntimeService;

    private Conversation mConversation;
    private Uri mContactRingId;
    private String mAccountId;

    private CompositeDisposable mConversationDisposable;
    private final CompositeDisposable mVisibilityDisposable = new CompositeDisposable();

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private final Subject<Conversation> mConversationSubject = BehaviorSubject.create();

    @Inject
    public ConversationPresenter(ContactService contactService,
                                 AccountService accountService,
                                 HardwareService hardwareService,
                                 ConversationFacade conversationFacade,
                                 VCardService vCardService,
                                 DeviceRuntimeService deviceRuntimeService) {
        this.mContactService = contactService;
        this.mAccountService = accountService;
        this.mHardwareService = hardwareService;
        this.mConversationFacade = conversationFacade;
        this.mVCardService = vCardService;
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    @Override
    public void bindView(ConversationView view) {
        super.bindView(view);
        mCompositeDisposable.add(mVisibilityDisposable);
        if (mConversationDisposable == null && mConversation != null)
            initView(mConversation, view);
    }

    public void init(Uri contactRingId, String accountId) {
        Log.w(TAG, "init " + contactRingId + " " + accountId);
        mContactRingId = contactRingId;
        mAccountId = accountId;
        Account account = mAccountService.getAccount(accountId);
        if (account != null)
            initContact(account, contactRingId, getView());
        mCompositeDisposable.add(mConversationFacade
                .startConversation(accountId, contactRingId)
                .observeOn(mUiScheduler)
                .subscribe(this::setConversation, e -> getView().goToHome()));
    }

    private void setConversation(final Conversation conversation) {
        if (conversation == null || mConversation == conversation)
            return;
        mConversation = conversation;
        mConversationSubject.onNext(conversation);
        ConversationView view = getView();
        if (view != null)
            initView(conversation, view);
    }

    public void pause() {
        mVisibilityDisposable.clear();
        if (mConversation != null) {
            mConversation.setVisible(false);
        }
    }

    public void resume() {
        Log.w(TAG, "resume " + mConversation + " " + mAccountId + " " + mContactRingId);
        mVisibilityDisposable.clear();
        mVisibilityDisposable.add(mConversationSubject
                .firstOrError()
                .subscribe(conversation -> {
                    conversation.setVisible(true);
                    updateOngoingCallView();
                    mConversationFacade.readMessages(mAccountService.getAccount(mAccountId), conversation);
                }));
    }

    private CallContact initContact(final Account account, final Uri uri, final ConversationView view) {
        CallContact contact;
        if (account.isRing()) {
            String rawId = uri.getRawRingId();
            contact = account.getContact(rawId);
            if (contact == null) {
                contact = account.getContactFromCache(uri);
                TrustRequest req = account.getRequest(uri);
                if (req == null) {
                    view.switchToUnknownView(contact.getRingUsername());
                } else {
                    view.switchToIncomingTrustRequestView(req.getDisplayname());
                }
            } else {
                view.switchToConversationView();
            }
            Log.w(TAG, "initContact " + contact.getUsername());
            if (contact.getUsername() == null) {
                mAccountService.lookupAddress(mAccountId, "", rawId);
            }
        } else {
            contact = mContactService.findContact(account, uri);
            view.switchToConversationView();
        }
        view.displayContact(contact);
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

        mConversationDisposable.add(c.getSortedHistory()
                .subscribe(view::refreshView, e -> Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(c.getCleared()
                .observeOn(mUiScheduler)
                .subscribe(view::refreshView, e -> Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(mContactService.getLoadedContact(c.getAccountId(), c.getContact())
                .observeOn(mUiScheduler)
                .subscribe(contact -> initContact(account, mContactRingId, view), e -> Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(c.getNewElements()
                .observeOn(mUiScheduler)
                .subscribe(view::addElement, e -> Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(c.getUpdatedElements()
                .observeOn(mUiScheduler)
                .subscribe(view::updateElement, e -> Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(c.getRemovedElements()
                .observeOn(mUiScheduler)
                .subscribe(view::removeElement, e -> Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(c.getCalls()
                .observeOn(mUiScheduler)
                .subscribe(calls -> updateOngoingCallView(), e -> Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(c.getColor()
                .observeOn(mUiScheduler)
                .subscribe(view::setConversationColor, e -> Log.e(TAG, "Can't update element", e)));
    }

    public void openContact() {
        getView().goToContactActivity(mAccountId, mConversation.getContact().getPrimaryNumber());
    }

    public void sendTextMessage(String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        Conference conference = mConversation.getCurrentCall();
        if (conference == null || !conference.isOnGoing()) {
            mConversationFacade.sendTextMessage(mAccountId, mConversation, mContactRingId, message).subscribe();
        } else {
            mConversationFacade.sendTextMessage(mConversation, conference, message);
        }
    }

    public void selectFile() {
        getView().openFilePicker();
    }

    public void sendFile(File file) {
        mConversationFacade.sendFile(mAccountId, mContactRingId, file).subscribe();
    }

    /**
     * Gets the absolute path of the file dataTransfer and sends both the DataTransfer and the
     * found path to the ConversationView in order to start saving the file
     * @param transfer DataTransfer of the file
     */
    public void saveFile(DataTransfer transfer) {
       String fileAbsolutePath =  getDeviceRuntimeService().
                getConversationPath(transfer.getPeerId(), transfer.getStoragePath())
               .getAbsolutePath();
        getView().startSaveFile(transfer,fileAbsolutePath);
    }

    public void shareFile(DataTransfer file) {
        File path = getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
        getView().shareFile(path);
    }

    public void openFile(DataTransfer file) {
        File path = getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
        getView().openFile(path);
    }

    public void deleteConversationItem(ConversationElement element) {
        mConversationFacade.deleteConversationItem(element);
    }

    public void cancelMessage(TextMessage message) {
        mConversationFacade.cancelMessage(message);
    }

    public void sendTrustRequest() {
        final String accountId = mAccountId;
        final Uri contactId = mContactRingId;
        mVCardService.loadSmallVCard(accountId, VCardService.MAX_SIZE_REQUEST)
                .subscribeOn(Schedulers.computation())
                .subscribe(vCard -> {
                    mAccountService.sendTrustRequest(accountId, contactId.getRawRingId(), Blob.fromString(VCardUtils.vcardToString(vCard)));
                    CallContact contact = mContactService.findContact(mAccountService.getAccount(accountId), contactId);
                    if (contact == null) {
                        Log.e(TAG, "sendTrustRequest: not able to find contact");
                        return;
                    }
                    contact.setStatus(CallContact.Status.REQUEST_SENT);
                });
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

    private void updateOngoingCallView() {
        Conference conf = mConversation.getCurrentCall();
        if (conf != null && (conf.getState() == SipCall.State.CURRENT || conf.getState() == SipCall.State.HOLD || conf.getState() == SipCall.State.RINGING)) {
            getView().displayOnGoingCallPane(true);
        } else {
            getView().displayOnGoingCallPane(false);
        }
        getView().scrollToEnd();
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

    public void setConversationColor(int color) {
        mCompositeDisposable.add(mConversationSubject
                .firstElement()
                .subscribe(conversation -> conversation.setColor(color)));
    }

    public void cameraPermissionChanged(boolean isGranted) {
        if (isGranted && mHardwareService.isVideoAvailable()) {
            mHardwareService.initVideo();
        }
    }

}
