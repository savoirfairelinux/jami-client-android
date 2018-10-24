/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.tv.contactrequest;

import android.content.res.Resources;
import android.os.Bundle;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewLogoPresenter;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.core.content.ContextCompat;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.Uri;
import cx.ring.tv.main.BaseDetailFragment;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.views.AvatarDrawable;

public class TVContactRequestFragment extends BaseDetailFragment<TVContactRequestPresenter> implements TVContactRequestView {

    private static final int ACTION_ACCEPT = 1;
    private static final int ACTION_REFUSE = 2;
    private static final int ACTION_BLOCK = 3;

    private Uri mSelectedContactRequest;
    private ArrayObjectAdapter mAdapter;
    private int iconSize = -1;
    private int imageCardWidth = -1;
    private int imageCardHeight = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
        mSelectedContactRequest = (Uri) getActivity().getIntent()
                .getSerializableExtra(TVContactRequestActivity.CONTACT_REQUEST);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BrowseFrameLayout layout = (BrowseFrameLayout) view;

        // Override down navigation as we do not use it in this screen
        // Only the detailPresenter will be displayed
        layout.setOnDispatchKeyListener((v, keyCode, event) -> event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN);
        prepareBackgroundManager();
        if (mSelectedContactRequest != null) {
            setupAdapter();
        }
        Resources res = getResources();
        iconSize = res.getDimensionPixelSize(R.dimen.tv_avatar_size);
        imageCardWidth = res.getDimensionPixelSize(R.dimen.default_image_card_width);
        imageCardHeight = res.getDimensionPixelSize(R.dimen.default_image_card_height);
        presenter.setContact(mSelectedContactRequest);
    }

    private void prepareBackgroundManager() {
        BackgroundManager mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
    }

    private void setupAdapter() {
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new TVContactRequestDetailPresenter(),
                        new LogoPresenter(imageCardWidth, imageCardHeight));

        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getActivity(), R.color.color_primary_dark));
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_HALF);

        // Hook up transition element.
        FullWidthDetailsOverviewSharedElementHelper mHelper = new FullWidthDetailsOverviewSharedElementHelper();
        mHelper.setSharedElementEnterTransition(getActivity(),
                TVContactRequestActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(mHelper);
        detailsPresenter.setParticipatingEntranceTransition(false);
        prepareEntranceTransition();

        detailsPresenter.setOnActionClickedListener(action -> {
            if (action.getId() == ACTION_ACCEPT) {
                presenter.acceptTrustRequest(mSelectedContactRequest);
            } else if (action.getId() == ACTION_REFUSE) {
                presenter.refuseTrustRequest(mSelectedContactRequest);
            } else if (action.getId() == ACTION_BLOCK) {
                presenter.blockTrustRequest(mSelectedContactRequest);
            }
        });

        ClassPresenterSelector mPresenterSelector = new ClassPresenterSelector();
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    public void showRequest(TVListViewModel model) {
        final DetailsOverviewRow row = new DetailsOverviewRow(model);
        AvatarDrawable avatar = new AvatarDrawable(getActivity(), model.getContact(), false);
        avatar.setInSize(iconSize);
        row.setImageDrawable(avatar);

        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();
        adapter.set(ACTION_ACCEPT, new Action(ACTION_ACCEPT, getResources()
                .getString(R.string.accept)));
        adapter.set(ACTION_REFUSE, new Action(ACTION_REFUSE, getResources().getString(R.string.refuse)));
        adapter.set(ACTION_BLOCK, new Action(ACTION_BLOCK, getResources().getString(R.string.block)));
        row.setActionsAdapter(adapter);

        mAdapter.add(row);
    }

    @Override
    public void finishView() {
        getActivity().finish();
    }

    static class LogoPresenter extends DetailsOverviewLogoPresenter {
        private final int lw, lh;
        LogoPresenter(int w, int h) {
            lw = w;
            lh = h;
        }

        @Override
        public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
            ImageView imageView = (ImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false);
            imageView.setLayoutParams(new ViewGroup.MarginLayoutParams(lw, lh));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            DetailsOverviewRow row = (DetailsOverviewRow) item;
            ImageView imageView = ((ImageView) viewHolder.view);
            imageView.setImageDrawable(row.getImageDrawable());
            if (isBoundToImage((ViewHolder) viewHolder, row)) {
                LogoPresenter.ViewHolder vh = (LogoPresenter.ViewHolder) viewHolder;
                vh.getParentPresenter().notifyOnBindLogo(vh.getParentViewHolder());
            }
        }
    }
}
