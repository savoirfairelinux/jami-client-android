/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.tv.about;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.app.DetailsSupportFragmentBackgroundController;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import cx.ring.R;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.iconcards.IconCard;
import cx.ring.tv.cards.iconcards.IconCardHelper;

public class AboutDetailsFragment extends DetailsSupportFragment {
    private static final String TAG = "AboutDetailsFragment";
    private final DetailsSupportFragmentBackgroundController mDetailsBackground =
            new DetailsSupportFragmentBackgroundController(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setupUi();
    }

    private void setupUi() {
        Bundle extras = getActivity().getIntent().getExtras();
        Card.Type cardType = Card.Type.DEFAULT;
        if (extras != null && extras.containsKey("abouttype")) {
            int ordinal = extras.getInt("abouttype", 0);
            cardType = Card.Type.values()[ordinal];
        }

        Context context = requireContext();
        IconCard card = IconCardHelper.getAboutCardByType(context, cardType);

        ClassPresenterSelector selector = new ClassPresenterSelector();

        FullWidthDetailsOverviewRowPresenter rowPresenter = new FullWidthDetailsOverviewRowPresenter(
                new AboutDetailsPresenter(context)) {
            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                // Customize Actionbar and Content by using custom colors.
                RowPresenter.ViewHolder viewHolder = super.createRowViewHolder(parent);

                View actionsView = viewHolder.view.findViewById(R.id.details_overview_actions_background);
                actionsView.setBackgroundColor(getResources().getColor(R.color.color_primary_dark));

                View detailsView = viewHolder.view.findViewById(R.id.details_frame);
                detailsView.setBackgroundColor(getResources().getColor(R.color.color_primary_dark));
                return viewHolder;
            }
        };
        selector.addClassPresenter(DetailsOverviewRow.class, rowPresenter);
        selector.addClassPresenter(ListRow.class,
                new ListRowPresenter());
        ArrayObjectAdapter mRowsAdapter = new ArrayObjectAdapter(selector);

        Resources res = getResources();
        DetailsOverviewRow detailsOverview = new DetailsOverviewRow(
                card);

        // Add images and action buttons to the details view
        detailsOverview.setImageDrawable(res.getDrawable(R.drawable.ic_jami));
        mRowsAdapter.add(detailsOverview);

        setAdapter(mRowsAdapter);
        initializeBackground();
    }


    private void initializeBackground() {
        mDetailsBackground.enableParallax();
        mDetailsBackground.setCoverBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.contrib_background));
    }
}
