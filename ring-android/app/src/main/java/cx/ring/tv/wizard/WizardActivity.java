/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

package cx.ring.tv.wizard;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.application.RingAppApplication;

public class WizardActivity extends AppCompatActivity implements WizardView {

    static final String TAG = WizardActivity.class.getSimpleName();

    public static final int ACCOUNT_CREATE_REQUEST = 1;

    @Inject
    protected WizardPresenter mWizardPresenter;

    @BindView(R.id.text_loading)
    protected TextView mTextLoading;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity_wizard);
        ButterKnife.bind(this);

        // dependency injection
        ((RingAppApplication) getApplication()).getRingInjectionComponent().inject(this);

        mWizardPresenter.createAccount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWizardPresenter.bindView(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWizardPresenter.unbindView();
    }

    @Override
    public void endCreation() {
        finish();
    }

    @Override
    public void errorCreation(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextLoading.setText(error);
            }
        });
    }
}
