package cx.ring.mvp;
/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import android.os.Bundle;

import android.support.v14.preference.PreferenceFragment;
import android.view.View;

import javax.inject.Inject;

import cx.ring.utils.Log;

public abstract class BasePreferenceFragment<T extends RootPresenter> extends PreferenceFragment {

    protected static final String TAG = BaseFragment.class.getSimpleName();

    @Inject
    protected T presenter;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        //Be sure to do the injection in onCreateView method
        presenter.bindView(this);
        initPresenter(presenter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        presenter.unbindView();
    }

    protected void initPresenter(T presenter) {

    }

}
