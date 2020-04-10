/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tv.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.fragment.app.FragmentActivity;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.services.AccountService;
import cx.ring.tv.account.TVAccountWizard;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class HomeActivity extends FragmentActivity {
    private BackgroundManager mBackgroundManager;
    private final CompositeDisposable mDisposable = new CompositeDisposable();
    @Inject
    AccountService mAccountService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JamiApplication.getInstance().startDaemon();
        JamiApplication.getInstance().getInjectionComponent().inject(this);
        setContentView(R.layout.tv_activity_home);
        mBackgroundManager = BackgroundManager.getInstance(this);
        mBackgroundManager.attach(getWindow());
    }

    @Override
    public void onBackPressed() {
        if (GuidedStepSupportFragment.getCurrentGuidedStepSupportFragment(getSupportFragmentManager()) != null) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBackgroundManager.setDrawable(getResources().getDrawable(R.drawable.tv_background));
        mDisposable.clear();
        mDisposable.add(mAccountService.getObservableAccountList()
                .observeOn(AndroidSchedulers.mainThread())
                .firstElement()
                .subscribe(accounts -> {
                    if (accounts.isEmpty()) {
                        startActivity(new Intent(this, TVAccountWizard.class));
                    }
                }));
    }
}