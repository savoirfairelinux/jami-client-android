/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package net.jami.mvp;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public abstract class RootPresenter<T> {

    protected CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public RootPresenter() { }

    private WeakReference<T> mView;

    public void bindView(T view) {
        mView = new WeakReference<>(view);
    }

    public void unbindView() {
        if (mView != null) {
            mView.clear();
            mView = null;
        }
        mCompositeDisposable.clear();
    }

    public void onDestroy() {
        mCompositeDisposable.dispose();
    }

    public T getView() {
        if (mView != null) {
            return mView.get();
        }
        return null;
    }

}


