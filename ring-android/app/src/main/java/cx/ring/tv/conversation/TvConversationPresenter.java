/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
package cx.ring.tv.conversation;

import java.io.File;

import javax.inject.Inject;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransfer;
import cx.ring.model.Error;
import cx.ring.model.Interaction;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Scheduler;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class TvConversationPresenter extends RootPresenter<TvConversationView> {

    private static final String TAG = TvConversationPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ConversationFacade mConversationFacade;
    private final Scheduler mUiScheduler;
    private final DeviceRuntimeService mDeviceRuntimeService;
    private final ContactService mContactService;

    private String mAccountId;
    private Uri mContactRingId;
    private Uri mContactId;
    private String mMessage;
    private Conversation mConversation;

    private final Subject<Conversation> mConversationSubject = BehaviorSubject.create();

    @Inject
    public TvConversationPresenter(ContactService contactService,
                                   AccountService accountService,
                                   ConversationFacade conversationFacade,
                                   Scheduler uiScheduler,
                                   DeviceRuntimeService deviceRuntimeService) {
        this.mContactService = contactService;
        mAccountService = accountService;
        mConversationFacade = conversationFacade;
        mUiScheduler = uiScheduler;
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    public void init(ConversationPath path) {
        mAccountId = path.getAccountId();
        mContactId = new Uri(path.getContactId());
        mContactRingId = new Uri(path.getContactId());

        mCompositeDisposable.add(mConversationFacade.loadConversationHistory(mAccountService.getCurrentAccount(), new Uri(mContactId.getRawUriString()))
                .observeOn(mUiScheduler)
                .subscribe(this::setConversation));

        initContact(mAccountService.getCurrentAccount(), new Uri(mContactId.getRawUriString()), getView());

    }

    public void sendText(String message){

        mMessage = message;

        Account account = mAccountService.getAccount(mAccountId);

        mCompositeDisposable.add(mConversationFacade.loadConversationHistory(account, new Uri(mContactId.getRawUriString()))
                .observeOn(mUiScheduler)
                .subscribe(this::test));
    }

    private void test(Conversation conversation) {
        if (StringUtils.isEmpty(mMessage) || conversation == null) {
            return;
        }

        Account account = mAccountService.getAccount(mAccountId);

        if (account != null) {
            Conference conference = account.getByUri(mContactId).getCurrentCall();
            if (conference == null || !conference.isOnGoing()) {
                mConversationFacade.sendTextMessage(mAccountId, conversation, new Uri(mContactId.getRawUriString()), mMessage).subscribe();
            } else {
                mConversationFacade.sendTextMessage(conversation, conference, mMessage);
            }
        }
    }

    public void sendFile(File file) {
        mConversationFacade.sendFile(mAccountId, new Uri(mContactId.getRawUriString()), file).subscribe();
    }

    private void setConversation(final Conversation conversation) {
        if (conversation == null || mConversation == conversation)
            return;
        mConversation = conversation;
        mConversationSubject.onNext(conversation);
        mCompositeDisposable.add(conversation.getSortedHistory()
                .subscribe(getView()::refreshView, e -> Log.e(TAG, "Can't update element", e)));
        mCompositeDisposable.add(conversation.getCleared()
                .observeOn(mUiScheduler)
                .subscribe(getView()::refreshView, e -> Log.e(TAG, "Can't update elements", e)));
        mCompositeDisposable.add(mContactService.getLoadedContact(conversation.getAccountId(), conversation.getContact())
                .observeOn(mUiScheduler)
                .subscribe(contact -> initContact(mAccountService.getCurrentAccount(), mContactRingId, getView()), e -> Log.e(TAG, "Can't get contact", e)));
        mCompositeDisposable.add(conversation.getUpdatedElements()
                .observeOn(mUiScheduler)
                .subscribe(elementTuple -> {
                    switch(elementTuple.second) {
                        case ADD:
                            getView().addElement(elementTuple.first);
                            break;
                        case UPDATE:
                            getView().updateElement(elementTuple.first);
                            break;
                        case REMOVE:
                            getView().removeElement(elementTuple.first);
                            break;
                    }
                }, e -> Log.e(TAG, "Can't update element", e)));

        mCompositeDisposable.add(conversation.getSortedHistory()
                .subscribe(getView()::refreshView, e -> Log.e(TAG, "Can't update element", e)));
        mCompositeDisposable.add(conversation.getCleared()
                .observeOn(mUiScheduler)
                .subscribe(getView()::refreshView, e -> Log.e(TAG, "Can't update elements", e)));
    }

    public DeviceRuntimeService getDeviceRuntimeService() {
        return mDeviceRuntimeService;
    }

    private CallContact initContact(final Account account, final Uri uri,
                                    final TvConversationView view) {
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

    public void noSpaceLeft() {
        Log.e(TAG, "configureForFileInfoTextMessage: no space left on device");
        getView().displayErrorToast(Error.NO_SPACE_LEFT);
    }

    /**
     * Gets the absolute path of the file dataTransfer and sends both the DataTransfer and the
     * found path to the TvConversationView in order to start saving the file
     *
     * @param interaction an interaction representing a datat transfer
     */
    public void saveFile(Interaction interaction) {
        DataTransfer transfer = (DataTransfer) interaction;
        String fileAbsolutePath = getDeviceRuntimeService().
                getConversationPath(transfer.getPeerId(), transfer.getStoragePath())
                .getAbsolutePath();
        getView().startSaveFile(transfer, fileAbsolutePath);
    }

    public void openFile(Interaction interaction) {
        DataTransfer file = (DataTransfer) interaction;
        File path = getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
        getView().openFile(path);
    }

    public void deleteConversationItem(Interaction element) {
        mConversationFacade.deleteConversationItem(element);
    }

    public void cancelMessage(Interaction message) {
        mConversationFacade.cancelMessage(message);
    }

}
