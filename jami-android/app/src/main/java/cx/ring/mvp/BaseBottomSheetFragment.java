/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.mvp;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import javax.inject.Inject;

import cx.ring.R;
import net.jami.mvp.RootPresenter;

public abstract class BaseBottomSheetFragment<T extends RootPresenter> extends BottomSheetDialogFragment {

    protected static final String TAG = BaseBottomSheetFragment.class.getSimpleName();

    @Inject
    protected T presenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
    }

    protected void initPresenter(T presenter) {
    }

    protected void replaceFragmentWithSlide(Fragment fragment, @IdRes int content) {
        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right,
                        R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(content, fragment, TAG)
                .addToBackStack(TAG)
                .commit();
    }

    protected void replaceFragment(Fragment fragment, @IdRes int content) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(content, fragment, TAG)
                .addToBackStack(TAG)
                .commit();
    }
}
