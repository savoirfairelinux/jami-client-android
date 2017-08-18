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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.tv.about;

import android.os.Bundle;

import cx.ring.R;
import cx.ring.about.AboutPresenter;
import cx.ring.about.AboutView;
import cx.ring.mvp.BaseActivity;

public class AboutActivity extends BaseActivity<AboutPresenter> implements AboutView {

    public static final String TAG = AboutActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity_about);
    }

    @Override
    public void showRingLogo(byte[] image) {

    }

    @Override
    public void showSavoirFaireLinuxLogo(byte[] image) {

    }

    @Override
    public void showRelease(String release) {

    }

    @Override
    public void showContribute(String contribute) {

    }

    @Override
    public void showCopyright(String copyright) {

    }

    @Override
    public void showLicense(String license) {

    }

    @Override
    public void showFeedback(String feedback) {

    }

    @Override
    public void showSupport(String support) {

    }
}
