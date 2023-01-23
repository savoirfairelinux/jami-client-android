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
package cx.ring.tv.cards

import androidx.core.content.res.ResourcesCompat
import androidx.leanback.widget.*
import cx.ring.R

/**
 * This [PresenterSelector] will return a [ListRowPresenter] which has shadow support
 * enabled or not depending on [CardRow.useShadow] for a given row.
 */
class ShadowRowPresenterSelector : PresenterSelector() {
    private val mShadowEnabledRowPresenter = CustomListRowPresenter().apply { setNumRows(1) }
    private val mShadowDisabledRowPresenter = CustomDimListRowPresenter().apply { shadowEnabled = false }
    override fun getPresenter(item: Any): Presenter {
        if (item !is CardListRow) return mShadowDisabledRowPresenter
        val row = item.cardRow
        return if (row.shadow) mShadowEnabledRowPresenter else mShadowDisabledRowPresenter
    }

    override fun getPresenters(): Array<Presenter> = arrayOf(mShadowDisabledRowPresenter, mShadowEnabledRowPresenter)

    private class CustomListRowPresenter : ListRowPresenter() {
        init {
            headerPresenter = CustomRowHeaderPresenter()
        }
    }

    private class CustomDimListRowPresenter : NoDimListRowPresenter() {
        init {
            headerPresenter = CustomRowHeaderPresenter()
        }
    }

    private class CustomRowHeaderPresenter : RowHeaderPresenter() {
        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            super.onBindViewHolder(viewHolder, item)
            val titleView: RowHeaderView = viewHolder.view.findViewById(R.id.row_header)
            titleView.typeface = ResourcesCompat.getFont(titleView.context, R.font.ubuntu_medium)
            titleView.textSize = 16f
            viewHolder.view.alpha = 1f
        }
    }
}