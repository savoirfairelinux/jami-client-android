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
package cx.ring.tv.account;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import cx.ring.R;

import static cx.ring.tv.account.TVProfileEditingFragment.REQUEST_CODE_GALLERY;
import static cx.ring.tv.account.TVProfileEditingFragment.REQUEST_CODE_PHOTO;

public class TVProfileEditingActivity extends Activity {

    public static final String TAG = TVProfileEditingActivity.class.getSimpleName();
    private static final String TV_PROFILE_EDITING_TAG = "tv_profile_editing";
    private TVProfileEditingFragment fTvProfileEditing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity_profile_editing);

        if (savedInstanceState != null) {
            fTvProfileEditing = (TVProfileEditingFragment) getFragmentManager().findFragmentByTag(TV_PROFILE_EDITING_TAG);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_PHOTO:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    fTvProfileEditing.updatePhoto((Bitmap) data.getExtras().get("data"));
                }
                break;
            case REQUEST_CODE_GALLERY:
                if (resultCode == RESULT_OK && data != null) {
                    fTvProfileEditing.updatePhoto(data.getData());
                }
                break;
        }
    }
}
