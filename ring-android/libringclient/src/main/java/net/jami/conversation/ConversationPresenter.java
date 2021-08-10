/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.conversation;

import net.jami.daemon.Blob;
import net.jami.facades.ConversationFacade;
import net.jami.model.Account;
import net.jami.model.Call;
import net.jami.model.Conference;
import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.model.DataTransfer;
import net.jami.model.Error;
import net.jami.model.Interaction;
import net.jami.model.TrustRequest;
import net.jami.model.Uri;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.services.ContactService;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.HardwareService;
import net.jami.services.PreferencesService;
import net.jami.services.VCardService;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;
import net.jami.utils.Tuple;
import net.jami.utils.VCardUtils;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class ConversationPresenter extends RootPresenter<ConversationView> {

    private static final String TAG = ConversationPresenter.class.getSimpleName();
    private final ContactService mContactService;
    private final AccountService mAccountService;
    private final HardwareService mHardwareService;
    private final ConversationFacade mConversationFacade;
    private final VCardService mVCardService;
    private final DeviceRuntimeService mDeviceRuntimeService;
    private final PreferencesService mPreferencesService;

    private Conversation mConversation;
    private Uri mConversationUri;

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
                                 DeviceRuntimeService deviceRuntimeService,
                                 PreferencesService preferencesService) {
        mContactService = contactService;
        mAccountService = accountService;
        mHardwareService = hardwareService;
        mConversationFacade = conversationFacade;
        mVCardService = vCardService;
        mDeviceRuntimeService = deviceRuntimeService;
        mPreferencesService = preferencesService;
        mCompositeDisposable.add(mVisibilityDisposable);
    }

    public void init(Uri conversationUri, String accountId) {
        Log.w(TAG, "init " + conversationUri + " " + accountId);
        if (conversationUri.equals(mConversationUri))
            return;
        mConversationUri = conversationUri;
        mCompositeDisposable.add(mConversationFacade.getAccountSubject(accountId)
                .flatMap(a -> mConversationFacade.loadConversationHistory(a, conversationUri)
                        .observeOn(mUiScheduler)
                        .doOnSuccess(c -> setConversation(a, c)))
                .observeOn(mUiScheduler)
                .subscribe(c -> {}, e -> {
                    Log.e(TAG, "Error loading conversation", e);
                    getView().goToHome();
                }));
        getView().setReadIndicatorStatus(showReadIndicator());
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mConversation = null;
        mConversationUri = null;
        if (mConversationDisposable != null) {
            mConversationDisposable.dispose();
            mConversationDisposable = null;
        }
    }

    private void setConversation(Account account, final Conversation conversation) {
        Log.w(TAG, "setConversation " + conversation.getAggregateHistory().size());
        if (mConversation == conversation)
            return;
        mConversation = conversation;
        mConversationSubject.onNext(conversation);
        ConversationView view = getView();
        if (view != null)
            initView(account, conversation, view);
    }

    public void pause() {
        mVisibilityDisposable.clear();
        if (mConversation != null) {
            mConversation.setVisible(false);
        }
    }

    public void resume(boolean isBubble) {
        Log.w(TAG, "resume " + mConversationUri);
        mVisibilityDisposable.clear();
        mVisibilityDisposable.add(mConversationSubject
                .subscribe(conversation -> {
                    conversation.setVisible(true);
                    updateOngoingCallView(conversation);
                    mConversationFacade.readMessages(mAccountService.getAccount(conversation.getAccountId()), conversation, !isBubble);
                }, e -> Log.e(TAG, "Error loading conversation", e)));
    }

    private void initContact(final Account account, final Conversation conversation, Conversation.Mode mode, final ConversationView view) {
        if (account.isJami()) {
            Log.w(TAG, "initContact " + conversation.getUri() + " mode:" + mode + " " + conversation.getContacts());
            if (mode == Conversation.Mode.Syncing) {
                view.switchToSyncingView();
            } else if (conversation.isSwarm() || account.isContact(conversation)) {
                //if (conversation.isEnded())
                //    conversation.s
                view.switchToConversationView();
            } else {
                Uri uri = conversation.getUri();
                TrustRequest req = account.getRequest(uri);
                if (req == null) {
                    view.switchToUnknownView(uri.getRawUriString());
                } else {
                    view.switchToIncomingTrustRequestView(req.getDisplayname());
                }
            }
        } else {
            view.switchToConversationView();
        }
        view.displayContact(conversation);
    }

    private void initView(Account account, final Conversation c, final ConversationView view) {
        Log.w(TAG, "initView " + c.getUri() + " " + c.getMode());
        if (mConversationDisposable == null) {
            mConversationDisposable = new CompositeDisposable();
            mCompositeDisposable.add(mConversationDisposable);
        }
        mConversationDisposable.clear();
        view.hideNumberSpinner();

        mConversationDisposable.add(c.getMode()
                .switchMapSingle(mode -> mContactService.getLoadedContact(c.getAccountId(), c.getContacts(), true)
                        .observeOn(mUiScheduler)
                        .doOnSuccess(contact -> initContact(account, c, mode, view)))
                .subscribe());
        mConversationDisposable.add(c.getMode()
                .switchMap(mode -> (mode == Conversation.Mode.Legacy || mode == Conversation.Mode.OneToOne) ?
                        c.getContact().getConversationUri() : Observable.empty())
                .observeOn(mUiScheduler)
                .subscribe(uri -> init(uri, account.getAccountID())));

        mConversationDisposable.add(Observable.combineLatest(
                mHardwareService.getConnectivityState(),
                mAccountService.getObservableAccount(account),
                (isConnected, a) -> isConnected || a.isRegistered())
                .observeOn(mUiScheduler)
                .subscribe(isOk -> {
                    ConversationView v = getView();
                    if (v != null) {
                        if (!isOk)
                            v.displayNetworkErrorPanel();
                        else if(!account.isEnabled()) {
                            v.displayAccountOfflineErrorPanel();
                        }
                        else {
                            v.hideErrorPanel();
                        }
                    }
                }));

        mConversationDisposable.add(c.getSortedHistory()
                .observeOn(mUiScheduler)
                .subscribe(view::refreshView, e -> Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(c.getCleared()
                .observeOn(mUiScheduler)
                .subscribe(view::refreshView, e -> Log.e(TAG, "Can't update elements", e)));

        mConversationDisposable.add(c.getContactUpdates()
                .switchMap(contacts -> Observable.merge(mContactService.observeLoadedContact(c.getAccountId(), contacts, true)))
                .observeOn(mUiScheduler)
                .subscribe(contact -> {
                    ConversationView v = getView();
                    if (v != null)
                        v.updateContact(contact);
                }));

        mConversationDisposable.add(c.getUpdatedElements()
                .observeOn(mUiScheduler)
                .subscribe(elementTuple -> {
                    switch(elementTuple.second) {
                        case ADD:
                            view.addElement(elementTuple.first);
                            break;
                        case UPDATE:
                            view.updateElement(elementTuple.first);
                            break;
                        case REMOVE:
                            view.removeElement(elementTuple.first);
                            break;
                    }
                }, e -> Log.e(TAG, "Can't update element", e)));

        if (showTypingIndicator()) {
            mConversationDisposable.add(c.getComposingStatus()
                    .observeOn(mUiScheduler)
                    .subscribe(view::setComposingStatus));
        }
        mConversationDisposable.add(c.getLastDisplayed()
                .observeOn(mUiScheduler)
                .subscribe(view::setLastDisplayed));
        mConversationDisposable.add(c.getCalls()
                .observeOn(mUiScheduler)
                .subscribe(calls -> updateOngoingCallView(mConversation), e -> Log.e(TAG, "Can't update call view", e)));
        mConversationDisposable.add(c.getColor()
                .observeOn(mUiScheduler)
                .subscribe(view::setConversationColor, e -> Log.e(TAG, "Can't update conversation color", e)));
        mConversationDisposable.add(c.getSymbol()
                .observeOn(mUiScheduler)
                .subscribe(view::setConversationSymbol, e -> Log.e(TAG, "Can't update conversation color", e)));

        mConversationDisposable.add(account
                .getLocationUpdates(c.getUri())
                .observeOn(mUiScheduler)
                .subscribe(u -> {
                    Log.e(TAG, "getLocationUpdates: update");
                    getView().showMap(c.getAccountId(), c.getUri().getUri(), false);
                }));
    }

    public void loadMore() {
        mConversationDisposable.add(mAccountService.loadMore(mConversation)
                .subscribe(c -> {}, e-> {}));
    }

    public void openContact() {
        if (mConversation != null)
            getView().goToContactActivity(mConversation.getAccountId(), mConversation.getUri());
    }

    public void sendTextMessage(String message) {
        if (StringUtils.isEmpty(message) || mConversation == null) {
            return;
        }
        Conference conference = mConversation.getCurrentCall();
        if (mConversation.isSwarm() || conference == null || !conference.isOnGoing()) {
            mConversationFacade.sendTextMessage(mConversation, mConversationUri, message).subscribe();
        } else {
            mConversationFacade.sendTextMessage(mConversation, conference, message);
        }
    }

    public void selectFile() {
        getView().openFilePicker();
    }

    public void sendFile(File file) {
        if (mConversation ==  null)
            return;
        mConversationFacade.sendFile(mConversation, mConversationUri, file).subscribe();
    }

    /**
     * Gets the absolute path of the file dataTransfer and sends both the DataTransfer and the
     * found path to the ConversationView in order to start saving the file
     *
     * @param interaction an interaction representing a datat transfer
     */
    public void saveFile(Interaction interaction) {
        DataTransfer transfer = (DataTransfer) interaction;
        String fileAbsolutePath = getDeviceRuntimeService().
                getConversationPath(transfer)
                .getAbsolutePath();
        getView().startSaveFile(transfer, fileAbsolutePath);
    }

    public void shareFile(Interaction interaction) {
        DataTransfer file = (DataTransfer) interaction;
        File path = getDeviceRuntimeService().getConversationPath(file);
        getView().shareFile(path, file.getDisplayName());
    }

    public void openFile(Interaction interaction) {
        DataTransfer file = (DataTransfer) interaction;
        File path = getDeviceRuntimeService().getConversationPath(file);
        getView().openFile(path, file.getDisplayName());
    }

    public void acceptFile(DataTransfer transfer) {
        getView().acceptFile(mConversation.getAccountId(), mConversationUri, transfer);
    }

    public void refuseFile(DataTransfer transfer) {
        getView().refuseFile(mConversation.getAccountId(), mConversationUri, transfer);
    }

    public void deleteConversationItem(Interaction element) {
        mConversationFacade.deleteConversationItem(mConversation, element);
    }

    public void cancelMessage(Interaction message) {
        mConversationFacade.cancelMessage(message);
    }

    private void sendTrustRequest() {
        Contact contact = mConversation.getContact();
        if (contact != null) {
            contact.setStatus(Contact.Status.REQUEST_SENT);
        }
        mVCardService.loadSmallVCardWithDefault(mConversation.getAccountId(), VCardService.MAX_SIZE_REQUEST)
                .subscribeOn(Schedulers.computation())
                .subscribe(vCard -> mAccountService.sendTrustRequest(mConversation, contact.getUri(), Blob.fromString(VCardUtils.vcardToString(vCard))),
                        e -> mAccountService.sendTrustRequest(mConversation, contact.getUri(), null));
    }

    public void clickOnGoingPane() {
        Conference conf = mConversation == null ? null : mConversation.getCurrentCall();
        if (conf != null) {
            getView().goToCallActivity(conf.getId());
        } else {
            getView().displayOnGoingCallPane(false);
        }
    }

    public void goToCall(boolean audioOnly) {
        if (audioOnly && !mHardwareService.hasMicrophone()) {
            getView().displayErrorToast(Error.NO_MICROPHONE);
            return;
        }

        mCompositeDisposable.add(mConversationSubject
                .firstElement()
                .subscribe(conversation -> {
                    ConversationView view = getView();
                    if (view != null) {
                        Conference conf = mConversation.getCurrentCall();
                        if (conf != null
                                && !conf.getParticipants().isEmpty()
                                && conf.getParticipants().get(0).getCallStatus() != Call.CallStatus.INACTIVE
                                && conf.getParticipants().get(0).getCallStatus() != Call.CallStatus.FAILURE) {
                            view.goToCallActivity(conf.getId());
                        } else {
                            view.goToCallActivityWithResult(mConversation.getAccountId(), mConversation.getUri(), mConversation.getContact().getUri(), audioOnly);
                        }
                    }
                }));
    }

    private void updateOngoingCallView(Conversation conversation) {
        Conference conf = conversation == null ? null : conversation.getCurrentCall();
        getView().displayOnGoingCallPane(conf != null && (conf.getState() == Call.CallStatus.CURRENT || conf.getState() == Call.CallStatus.HOLD || conf.getState() == Call.CallStatus.RINGING));
    }

    public void onBlockIncomingContactRequest() {
        mConversationFacade.discardRequest(mConversation.getAccountId(), mConversationUri);
        mAccountService.removeContact(mConversation.getAccountId(), mConversationUri.getHost(), true);

        getView().goToHome();
    }

    public void onRefuseIncomingContactRequest() {
        mConversationFacade.discardRequest(mConversation.getAccountId(), mConversationUri);
        getView().goToHome();
    }

    public void onAcceptIncomingContactRequest() {
        mConversationFacade.acceptRequest(mConversation.getAccountId(), mConversationUri);
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
        getView().displayErrorToast(Error.NO_SPACE_LEFT);
    }

    public void setConversationColor(int color) {
        mCompositeDisposable.add(mConversationSubject
                .firstElement()
                .subscribe(conversation -> conversation.setColor(color)));
    }
    public void setConversationSymbol(CharSequence symbol) {
        mCompositeDisposable.add(mConversationSubject
                .firstElement()
                .subscribe(conversation -> conversation.setSymbol(symbol)));
    }

    public void cameraPermissionChanged(boolean isGranted) {
        if (isGranted && mHardwareService.isVideoAvailable()) {
            mHardwareService.initVideo()
                    .onErrorComplete()
                    .subscribe();
        }
    }

    public void shareLocation() {
        getView().startShareLocation(mConversation.getAccountId(), mConversationUri.getUri());
    }

    public void showPluginListHandlers() {
        getView().showPluginListHandlers(mConversation.getAccountId(), mConversationUri.getUri());
    }

    public Tuple<String, String> getPath() {
        return new Tuple<>(mConversation.getAccountId(), mConversationUri.getUri());
    }

    public void onComposingChanged(boolean hasMessage) {
        if (mConversation == null || !showTypingIndicator()) {
            return;
        }
        mConversationFacade.setIsComposing(mConversation.getAccountId(), mConversationUri, hasMessage);
    }

    public boolean showTypingIndicator() {
        return mPreferencesService.getSettings().isAllowTypingIndicator();
    }

    private boolean showReadIndicator() {
        return mPreferencesService.getSettings().isAllowReadIndicator();
    }

}
