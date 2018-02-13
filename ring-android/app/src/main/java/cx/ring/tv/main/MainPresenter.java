/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package cx.ring.tv.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.RingError;
import cx.ring.model.ServiceEvent;
import cx.ring.model.TrustRequest;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.navigation.RingNavigationViewModel;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.HardwareService;
import cx.ring.services.PresenceService;
import cx.ring.tv.model.TVContactRequestViewModel;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.observers.ResourceSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainPresenter extends RootPresenter<MainView> implements Observer<ServiceEvent> {

    private static final String TAG = MainPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private ContactService mContactService;
    private PresenceService mPresenceService;
    private HardwareService mHardwareService;
    private ArrayList<TVContactRequestViewModel> mContactRequestViewModels;
    private List<TVListViewModel> mTvListViewModels;
    private Scheduler mMainScheduler;

    @Inject
    public MainPresenter(AccountService accountService,
                         ContactService contactService,
                         PresenceService presenceService,
                         HardwareService hardwareService,
                         Scheduler mainScheduler) {


        mAccountService = accountService;
        mContactService = contactService;
        mPresenceService = presenceService;
        mHardwareService = hardwareService;
        mMainScheduler = mainScheduler;
    }

    @Override
    public void bindView(MainView view) {
        super.bindView(view);
        mAccountService.addObserver(this);
        mContactService.addObserver(this);
        mPresenceService.addObserver(this);
    }

    @Override
    public void unbindView() {
        super.unbindView();
        mAccountService.removeObserver(this);
        mContactService.removeObserver(this);
        mPresenceService.removeObserver(this);
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case CONVERSATIONS_CHANGED:
            case ACCOUNTS_CHANGED:
            case NAME_REGISTRATION_ENDED:
                reloadConversations();
                reloadAccountInfos();
                break;
            case INCOMING_TRUST_REQUEST:
                loadContactRequest();
                break;
        }
        if (observable instanceof PresenceService) {
            switch (event.getEventType()) {
                case NEW_BUDDY_NOTIFICATION:
                    refreshContact(
                            event.getString(ServiceEvent.EventInput.BUDDY_URI));
                    break;
            }
        }
    }

    private void refreshContact(String buddy) {
        for (int i = 0; i < mTvListViewModels.size(); i++) {
            TVListViewModel tvListViewModel = mTvListViewModels.get(i);
            CallContact callContact = tvListViewModel.getCallContact();

            if (callContact.getIds().get(0).equals("ring:" + buddy)) {
                TVListViewModel updatedTvListViewModel = new TVListViewModel(
                        callContact,
                        mPresenceService.isBuddyOnline(callContact.getIds().get(0)));

                if (!updatedTvListViewModel.equals(tvListViewModel)) {
                    getView().refreshContact(i, updatedTvListViewModel);
                }
            }
        }
    }

    public void reloadConversations() {
        getView().showLoading(true);
        Account currentAccount = mAccountService.getCurrentAccount();
        if (currentAccount == null) {
            Log.e(TAG, "reloadConversations: Not able to get currentAccount");
            return;
        }

        final Collection<CallContact> contacts = currentAccount.getContacts().values();

        //Get all non-ban contact and then get last message and last call to create a smartList entry
        mCompositeDisposable.add(io.reactivex.Observable.fromIterable(contacts)
                .filter(callContact -> !callContact.isBanned())
                .map(this::modelToViewModel)
                .toSortedList()
                .subscribeOn(Schedulers.computation())
                .observeOn(mMainScheduler)
                .subscribeWith(new DisposableSingleObserver<List<TVListViewModel>>() {
                    @Override
                    public void onSuccess(List<TVListViewModel> tvListViewModels) {
                        MainPresenter.this.mTvListViewModels = tvListViewModels;
                        getView().showContacts(tvListViewModels);
                        getView().showLoading(false);

                        subscribePresence();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, throwable.toString());
                        getView().showLoading(false);
                    }
                }));
    }

    public void loadContactRequest() {
        mCompositeDisposable.add(Single.fromCallable(() -> {

            List<TrustRequest> requests = mAccountService.getCurrentAccount().getRequests();
            ArrayList<TVContactRequestViewModel> contactRequestViewModels = new ArrayList<>();

            for (TrustRequest request : requests) {

                byte[] photo;
                if (request.getVCard().getPhotos().isEmpty()) {
                    photo = null;
                } else {
                    photo = request.getVCard().getPhotos().get(0).getData();
                }

                TVContactRequestViewModel tvContactRequestVM = new TVContactRequestViewModel(request.getContactId(),
                        request.getDisplayname(),
                        request.getFullname(),
                        photo,
                        request.getMessage());
                contactRequestViewModels.add(tvContactRequestVM);
            }
            return contactRequestViewModels;
        }).subscribeOn(Schedulers.computation())
                .observeOn(mMainScheduler)
                .subscribeWith(new ResourceSingleObserver<ArrayList<TVContactRequestViewModel>>() {
                    @Override
                    public void onSuccess(@NonNull ArrayList<TVContactRequestViewModel> contactRequestViewModels) {
                        mContactRequestViewModels = contactRequestViewModels;

                        if (mContactRequestViewModels.isEmpty()) {
                            getView().showContactRequestsRow(false);
                        } else {
                            getView().showContactRequestsRow(true);
                            getView().showContactRequests(mContactRequestViewModels);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, e.toString());
                    }
                }));
    }

    private TVListViewModel modelToViewModel(CallContact callContact) {
        mContactService.loadContactData(callContact);

        return new TVListViewModel(
                callContact,
                mPresenceService.isBuddyOnline(callContact.getIds().get(0)));
    }

    public void contactClicked(TVListViewModel item) {
        if (!mHardwareService.isVideoAvailable() && !mHardwareService.hasMicrophone()) {
            getView().displayErrorToast(RingError.NO_INPUT);
            return;
        }

        String accountID = mAccountService.getCurrentAccount().getAccountID();

        String ringID = item.getCallContact().getPhones().get(0).getNumber().toString();
        getView().callContact(accountID, ringID);
    }

    public void reloadAccountInfos() {
        if (mAccountService == null) {
            Log.e(TAG, "reloadAccountInfos: No account service available");
            return;
        }
        String displayableAddress = null;
        List<Account> accounts = mAccountService.getAccounts();
        for (Account account : accounts) {
            displayableAddress = account.getDisplayUri();
        }

        RingNavigationViewModel viewModel = new RingNavigationViewModel(mAccountService.getCurrentAccount(), accounts);
        getView().displayAccountInfos(displayableAddress, viewModel);
    }

    public void onExportClicked() {
        getView().showExportDialog(mAccountService.getCurrentAccount().getAccountID());
    }

    public void onLicenceClicked(int aboutType) {
        getView().showLicence(aboutType);
    }

    public void onEditProfileClicked() {
        getView().showProfileEditing();
    }

    public void onSettingsClicked() {
        getView().showSettings();
    }

    private void subscribePresence() {
        if (mAccountService.getCurrentAccount() == null || mTvListViewModels == null || mTvListViewModels.isEmpty()) {
            return;
        }
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        for (TVListViewModel tvListViewModel : mTvListViewModels) {
            String ringId = tvListViewModel.getCallContact().getPhones().get(0).getNumber().getRawRingId();
            Uri uri = new Uri(ringId);
            if (uri.isRingId()) {
                mPresenceService.subscribeBuddy(accountId, ringId, true);
            } else {
                Log.i(TAG, "Trying to subscribe to an invalid uri " + ringId);
            }
        }
    }
}