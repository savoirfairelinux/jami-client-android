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
package cx.ring.mvp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import javax.inject.Inject;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cx.ring.R;
import cx.ring.account.RingAccountCreationFragment;
import cx.ring.application.JamiApplication;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.model.RingError;

public abstract class BaseSupportFragment<T extends RootPresenter> extends Fragment implements BaseView {

    protected static final String TAG = BaseSupportFragment.class.getSimpleName();

    @Inject
    protected T presenter;

    private Unbinder mUnbinder;

    @LayoutRes
    public abstract int getLayout();

    public abstract void injectFragment(JamiInjectionComponent component);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(getLayout(), container, false);
        // dependency injection
        injectFragment(((JamiApplication) getActivity().getApplication()).getRingInjectionComponent());
        //Butterknife
        mUnbinder = ButterKnife.bind(this, inflatedView);
        return inflatedView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Be sure to do the injection in onCreateView method
        if (presenter != null) {
            presenter.bindView(this);
            initPresenter(presenter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (presenter != null)
            presenter.unbindView();
        // Butterknife unbinding
        if (mUnbinder != null) {
            mUnbinder.unbind();
        }
    }

    public void displayErrorToast(int error) {
        String errorString;
        switch (error) {
            case RingError.NO_INPUT:
                errorString = getString(R.string.call_error_no_camera_no_microphone);
                break;
            case RingError.INVALID_FILE:
                errorString = getString(R.string.invalid_file);
                break;
            case RingError.NOT_ABLE_TO_WRITE_FILE:
                errorString = getString(R.string.not_able_to_write_file);
                break;
            case RingError.NO_SPACE_LEFT:
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

    protected void replaceFragmentWithSlide(Fragment fragment, @IdRes int content) {
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right,
                        R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(content, fragment, RingAccountCreationFragment.TAG)
                .addToBackStack(RingAccountCreationFragment.TAG)
                .commit();
    }

    protected void replaceFragment(Fragment fragment, @IdRes int content) {
        getFragmentManager()
                .beginTransaction()
                .replace(content, fragment, RingAccountCreationFragment.TAG)
                .addToBackStack(RingAccountCreationFragment.TAG)
                .commit();
    }
}
