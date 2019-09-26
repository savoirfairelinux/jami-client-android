/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.tv.contact;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
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
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Uri;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.main.BaseDetailFragment;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.views.AvatarDrawable;

public class TVContactFragment extends BaseDetailFragment<TVContactPresenter> implements TVContactView {

    private static final int ACTION_CALL = 0;
    private static final int ACTION_DELETE = 1;

    private Uri mContactUri;
    private ArrayObjectAdapter mAdapter;
    private int iconSize = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
        mContactUri = (Uri) getActivity().getIntent().getSerializableExtra(TVContactActivity.CONTACT_REQUEST);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BrowseFrameLayout layout = (BrowseFrameLayout) view;

        // Override down navigation as we do not use it in this screen
        // Only the detailPresenter will be displayed
        layout.setOnDispatchKeyListener((v, keyCode, event) -> event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN);
        prepareBackgroundManager();
        if (mContactUri != null) {
            setupAdapter();
        }
        Resources res = getResources();
        iconSize = res.getDimensionPixelSize(R.dimen.tv_avatar_size);
        presenter.setContact(mContactUri);
    }

    private void prepareBackgroundManager() {
        Activity activity = requireActivity();
        BackgroundManager mBackgroundManager = BackgroundManager.getInstance(activity);
        mBackgroundManager.attach(activity.getWindow());
    }

    private void setupAdapter() {
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(
                        new TVContactDetailPresenter(),
                        new DetailsOverviewLogoPresenter());

        detailsPresenter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_primary_dark));
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_HALF);

        // Hook up transition element.
        Activity activity = getActivity();
        if (activity != null) {
            FullWidthDetailsOverviewSharedElementHelper mHelper = new FullWidthDetailsOverviewSharedElementHelper();
            mHelper.setSharedElementEnterTransition(activity, TVContactActivity.SHARED_ELEMENT_NAME);
            detailsPresenter.setListener(mHelper);
            detailsPresenter.setParticipatingEntranceTransition(false);
            prepareEntranceTransition();
        }

        detailsPresenter.setOnActionClickedListener(action -> {
            if (action.getId() == ACTION_CALL) {
                presenter.contactClicked(mContactUri);
            } else if (action.getId() == ACTION_DELETE) {
                presenter.removeContact(mContactUri);
            }
        });

        ClassPresenterSelector mPresenterSelector = new ClassPresenterSelector();
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    public void showContact(TVListViewModel model) {
        final DetailsOverviewRow row = new DetailsOverviewRow(model);
        AvatarDrawable avatar = new AvatarDrawable(getActivity(), model.getContact(), false);
        avatar.setInSize(iconSize);
        row.setImageDrawable(avatar);

        ArrayObjectAdapter adapter = new ArrayObjectAdapter();
        adapter.add(ACTION_CALL, new Action(ACTION_CALL, getResources().getString(R.string.ab_action_video_call)));
        adapter.add(ACTION_DELETE, new Action(ACTION_DELETE, getResources().getString(R.string.conversation_action_remove_this)));
        row.setActionsAdapter(adapter);

        mAdapter.add(row);
    }

    @Override
    public void callContact(String accountID, Uri uri) {
        Context context = requireContext();
        Intent intent = new Intent(context, TVCallActivity.class);
        intent.putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountID);
        intent.putExtra(ConversationFragment.KEY_CONTACT_RING_ID, uri.getRawUriString());
        context.startActivity(intent, null);
    }

    @Override
    public void finishView() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }
}
