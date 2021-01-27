/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
package cx.ring.mvp;

import android.app.Fragment;
import android.os.Bundle;

import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

import cx.ring.R;
import net.jami.model.Error;
import net.jami.mvp.BaseView;
import net.jami.mvp.RootPresenter;

public abstract class BaseFragment<T extends RootPresenter> extends Fragment implements BaseView {

    protected static final String TAG = BaseFragment.class.getSimpleName();

    @Inject
    protected T presenter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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
            case INVALID_FILE:
                errorString = getString(R.string.invalid_file);
                break;
            case NOT_ABLE_TO_WRITE_FILE:
                errorString = getString(R.string.not_able_to_write_file);
                break;
            case NO_SPACE_LEFT:
                errorString = getString(R.string.no_space_left_on_device);
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
