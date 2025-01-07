/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package cx.ring.tv.about

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import cx.ring.R
import cx.ring.tv.cards.Card
import cx.ring.tv.cards.iconcards.IconCardHelper

class AboutDetailsFragment : DetailsSupportFragment() {
    private val mDetailsBackground = DetailsSupportFragmentBackgroundController(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        val extras = requireActivity().intent.extras
        var cardType = Card.Type.DEFAULT
        if (extras != null && extras.containsKey("abouttype")) {
            val ordinal = extras.getInt("abouttype", 0)
            cardType = Card.Type.values()[ordinal]
        }
        val context = requireContext()
        val card = IconCardHelper.getAboutCardByType(context, cardType)!!
        val selector = ClassPresenterSelector()
        val rowPresenter = object : FullWidthDetailsOverviewRowPresenter(AboutDetailsPresenter(context)) {
            override fun createRowViewHolder(parent: ViewGroup): RowPresenter.ViewHolder {
                // Customize Actionbar and Content by using custom colors.
                val viewHolder = super.createRowViewHolder(parent)
                val actionsView = viewHolder.view.findViewById<View>(androidx.leanback.R.id.details_overview_actions_background)
                actionsView.setBackgroundColor(resources.getColor(R.color.color_primary_dark))
                val detailsView = viewHolder.view.findViewById<View>(androidx.leanback.R.id.details_frame)
                detailsView.setBackgroundColor(resources.getColor(R.color.color_primary_dark))
                return viewHolder
            }
        }
        selector.addClassPresenter(DetailsOverviewRow::class.java, rowPresenter)
        selector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        val mRowsAdapter = ArrayObjectAdapter(selector)
        // Add images and action buttons to the details view
        mRowsAdapter.add(DetailsOverviewRow(card).apply {
            imageDrawable = resources.getDrawable(R.drawable.ic_jami)
        })
        adapter = mRowsAdapter
        initializeBackground()
    }

    private fun initializeBackground() {
        mDetailsBackground.enableParallax()
        mDetailsBackground.coverBitmap = BitmapFactory.decodeResource(resources, R.drawable.contrib_background)
    }

    companion object {
        private const val TAG = "AboutDetailsFragment"
    }
}