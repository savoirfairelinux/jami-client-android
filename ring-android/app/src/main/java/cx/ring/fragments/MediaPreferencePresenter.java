/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.fragments;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.model.Codec;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import io.reactivex.Scheduler;

public class MediaPreferencePresenter extends RootPresenter<MediaPreferenceView>
{
    public static final String TAG = MediaPreferencePresenter.class.getSimpleName();

    protected AccountService mAccountService;
    protected DeviceRuntimeService mDeviceRuntimeService;

    private Account mAccount;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public MediaPreferencePresenter(AccountService accountService, DeviceRuntimeService deviceRuntimeService) {
        this.mAccountService = accountService;
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    @Override
    public void unbindView() {
        super.unbindView();
    }

    @Override
    public void bindView(MediaPreferenceView view) {
        super.bindView(view);
    }

    void init(String accountId) {
        mAccount = mAccountService.getAccount(accountId);
        mCompositeDisposable.clear();
        mCompositeDisposable.add(mAccountService
                .getObservableAccount(accountId)
                .switchMapSingle(account -> mAccountService.getCodecList(accountId)
                        .observeOn(mUiScheduler)
                        .doOnSuccess(codecList -> {
                            final ArrayList<Codec> audioCodec = new ArrayList<>();
                            final ArrayList<Codec> videoCodec = new ArrayList<>();
                            for (Codec codec : codecList) {
                                if (codec.getType() == Codec.Type.AUDIO) {
                                    audioCodec.add(codec);
                                } else if (codec.getType() == Codec.Type.VIDEO) {
                                    videoCodec.add(codec);
                                }
                            }
                            getView().accountChanged(account, audioCodec, videoCodec);
                        }))
                .subscribe(l -> {}, e -> Log.e(TAG, "Error loading codec list")));
    }


    void codecChanged(ArrayList<Long> codecs) {
        mAccountService.setActiveCodecList(mAccount.getAccountID(), codecs);
    }

    void videoPreferenceChanged(ConfigKey key, Object newValue) {
            mAccount.setDetail(key, newValue.toString());
            updateAccount();
    }

    private void updateAccount() {
        mAccountService.setCredentials(mAccount.getAccountID(), mAccount.getCredentialsHashMapList());
        mAccountService.setAccountDetails(mAccount.getAccountID(), mAccount.getDetails());
    }

}
