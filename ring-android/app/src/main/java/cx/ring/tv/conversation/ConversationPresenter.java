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
import cx.ring.model.Error;
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

public class ConversationPresenter extends RootPresenter<ConversationView> {

    private static final String TAG = ConversationPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final ConversationFacade mConversationFacade;
    private final Scheduler mUiScheduler;
    private final DeviceRuntimeService mDeviceRuntimeService;
    private final ContactService mContactService;

    private String mAccountId;
    private Uri mContactRingId;
    private Uri mUri;
    private String mMessage;
    private Conversation mConversation;

    private final Subject<Conversation> mConversationSubject = BehaviorSubject.create();

    @Inject
    public ConversationPresenter(ContactService contactService,
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
        mUri = new Uri(path.getContactId());
        mContactRingId = new Uri(path.getContactId());

//        mCompositeDisposable.add(mAccountService.getIncomingMessages()
//                .observeOn(mUiScheduler)
//                .subscribe(this::showMessage));

        mCompositeDisposable.add(mConversationFacade.loadConversationHistory(mAccountService.getCurrentAccount(), new Uri(mUri.getRawUriString()))
                .observeOn(mUiScheduler)
                .subscribe(this::setConversation));

        initContact(mAccountService.getCurrentAccount(), new Uri(mUri.getRawUriString()), getView());

    }

    public void sendText(ConversationPath path, String message){

        mMessage = message;
        mAccountId = path.getAccountId();
        mUri = new Uri(path.getContactId());

        Account account = mAccountService.getAccount(mAccountId);

        mCompositeDisposable.add(mConversationFacade.loadConversationHistory(account, new Uri(mUri.getRawUriString()))
                .observeOn(mUiScheduler)
                .subscribe(this::test));
    }

//    private void showMessage(TextMessage textMessage) {
//        getView().receivedMessage(textMessage);
//    }

    private void test(Conversation conversation) {
        if (StringUtils.isEmpty(mMessage) || conversation == null) {
            return;
        }

        Account account = mAccountService.getAccount(mAccountId);

        if (account != null) {
            Conference conference = account.getByUri(mUri).getCurrentCall();
            if (conference == null || !conference.isOnGoing()) {
                mConversationFacade.sendTextMessage(mAccountId, conversation, new Uri(mUri.getRawUriString()), mMessage).subscribe();
            } else {
                mConversationFacade.sendTextMessage(conversation, conference, mMessage);
            }
        }
    }

    public void sendFile(ConversationPath path, File file) {
        mAccountId = path.getAccountId();
        mUri = new Uri(path.getContactId());
        mConversationFacade.sendFile(mAccountId, new Uri(mUri.getRawUriString()), file).subscribe();
    }

    private void setConversation(final Conversation conversation) {
        if (conversation == null || mConversation == conversation)
            return;
        mConversation = conversation;
        mConversationSubject.onNext(conversation);
//        mCompositeDisposable.add(conversation.getSortedHistory()
//                .subscribe(getView()::receivedMessage));
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
                                    final ConversationView view) {
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

}
