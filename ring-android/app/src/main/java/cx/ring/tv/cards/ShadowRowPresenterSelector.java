/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package cx.ring.tv.cards;

import android.content.Context;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.RowHeaderPresenter;
import androidx.leanback.widget.RowHeaderView;

import cx.ring.R;

/**
 * This {@link PresenterSelector} will return a {@link ListRowPresenter} which has shadow support
 * enabled or not depending on {@link CardRow#useShadow()} for a given row.
 */
public class ShadowRowPresenterSelector extends PresenterSelector {

    private CustomListRowPresenter mShadowEnabledRowPresenter = new CustomListRowPresenter();
    private CustomDimListRowPresenter mShadowDisabledRowPresenter = new CustomDimListRowPresenter();

    public ShadowRowPresenterSelector() {
        mShadowEnabledRowPresenter.setNumRows(1);
        mShadowDisabledRowPresenter.setShadowEnabled(false);
    }

    @Override
    public Presenter getPresenter(Object item) {
        if (!(item instanceof CardListRow)) return mShadowDisabledRowPresenter;
        CardListRow listRow = (CardListRow) item;
        CardRow row = listRow.getCardRow();
        if (row.useShadow()) return mShadowEnabledRowPresenter;
        return mShadowDisabledRowPresenter;
    }

    @Override
    public Presenter[] getPresenters() {
        return new Presenter[]{
                mShadowDisabledRowPresenter,
                mShadowEnabledRowPresenter
        };
    }

    private static class CustomListRowPresenter extends ListRowPresenter {
        public CustomListRowPresenter() {
            super();
            setHeaderPresenter(new CustomRowHeaderPresenter());
        }
    }

    private static class CustomDimListRowPresenter extends NoDimListRowPresenter {
        public CustomDimListRowPresenter() {
            super();
            setHeaderPresenter(new CustomRowHeaderPresenter());
        }
    }

    private static class CustomRowHeaderPresenter extends RowHeaderPresenter {
        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            super.onBindViewHolder(viewHolder, item);
            RowHeaderView titleView = viewHolder.view.findViewById(R.id.row_header);
            titleView.setTypeface(ResourcesCompat.getFont(titleView.getContext(), R.font.ubuntu_medium));
            titleView.setTextSize(16);
            viewHolder.view.setAlpha(1);
        }
    }

}
