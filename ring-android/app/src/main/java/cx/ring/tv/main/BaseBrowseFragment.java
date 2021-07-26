/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.app.BrowseSupportFragment;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

import cx.ring.R;
import net.jami.model.Error;
import net.jami.mvp.BaseView;
import net.jami.mvp.RootPresenter;

public class BaseBrowseFragment<T extends RootPresenter> extends BrowseSupportFragment implements BaseView {

    protected static final String TAG = BaseBrowseFragment.class.getSimpleName();

    @Inject
    protected T presenter;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Be sure to do the injection in onCreateView method
        presenter.bindView(this);
        initPresenter(presenter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.unbindView();
    }

    public void displayErrorToast(Error error) {
        String errorString;
        switch (error) {
            case NO_INPUT:
                errorString = getString(R.string.call_error_no_camera_no_microphone);
                break;
            default:
                errorString = getString(R.string.generic_error);
                break;
        }

        Toast.makeText(getActivity(), errorString, Toast.LENGTH_LONG).show();
    }

    protected void initPresenter(T presenter) {

    }
}