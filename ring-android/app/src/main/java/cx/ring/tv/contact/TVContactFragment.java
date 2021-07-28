/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tv.contact;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewLogoPresenter;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;

import cx.ring.R;
import cx.ring.application.JamiApplication;

import net.jami.model.Uri;
import net.jami.services.NotificationService;
import net.jami.smartlist.SmartListViewModel;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.contact.more.TVContactMoreActivity;
import cx.ring.tv.contact.more.TVContactMoreFragment;
import cx.ring.tv.contactrequest.TVContactRequestDetailPresenter;
import cx.ring.tv.main.BaseDetailFragment;
import cx.ring.utils.ConversationPath;
import cx.ring.views.AvatarDrawable;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class TVContactFragment extends BaseDetailFragment<TVContactPresenter> implements TVContactView {

    public static final String TAG = TVContactFragment.class.getSimpleName();

    private static final int ACTION_CALL = 0;
    private static final int ACTION_ACCEPT = 1;
    private static final int ACTION_REFUSE = 2;
    private static final int ACTION_BLOCK = 3;
    private static final int ACTION_ADD_CONTACT = 4;
    private static final int ACTION_MORE = 5;
    private static final int REQUEST_CODE = 100;

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    private ArrayObjectAdapter mAdapter;
    private int iconSize = -1;

    private boolean isIncomingRequest = false;
    private boolean isOutgoingRequest = false;

    private ConversationPath mConversationPath;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String type;
        if (getArguments() != null) {
            mConversationPath = ConversationPath.fromBundle(getArguments());
            type = getArguments().getString("type");
        } else {
            mConversationPath = ConversationPath.fromIntent(requireActivity().getIntent());
            type = getActivity().getIntent().getType();
        }

        if (type != null) {
            switch (type) {
                case TVContactActivity.TYPE_CONTACT_REQUEST_INCOMING:
                    isIncomingRequest = true;
                    break;
                case TVContactActivity.TYPE_CONTACT_REQUEST_OUTGOING:
                    isOutgoingRequest = true;
                    break;
            }
        }

        setupAdapter();
        Resources res = getResources();
        iconSize = res.getDimensionPixelSize(R.dimen.tv_avatar_size);
        presenter.setContact(mConversationPath);
    }

    private void setupAdapter() {
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter;
        if (isIncomingRequest || isOutgoingRequest) {
            detailsPresenter = new FullWidthDetailsOverviewRowPresenter(
                    new TVContactRequestDetailPresenter(),
                    new DetailsOverviewLogoPresenter());
        } else {
            detailsPresenter = new FullWidthDetailsOverviewRowPresenter(
                    new TVContactDetailPresenter(),
                    new DetailsOverviewLogoPresenter());
        }

        detailsPresenter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.tv_contact_background));
        detailsPresenter.setActionsBackgroundColor(ContextCompat.getColor(requireContext(), R.color.tv_contact_row_background));
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
                presenter.contactClicked();
            } else if (action.getId() == ACTION_ADD_CONTACT) {
                presenter.onAddContact();
            } else if (action.getId() == ACTION_ACCEPT) {
                presenter.acceptTrustRequest();
            } else if (action.getId() == ACTION_REFUSE) {
                presenter.refuseTrustRequest();
            } else if (action.getId() == ACTION_BLOCK) {
                presenter.blockTrustRequest();
            } else if (action.getId() == ACTION_MORE) {
                startActivityForResult(new Intent(getActivity(), TVContactMoreActivity.class)
                        .setDataAndType(mConversationPath.toUri(), TVContactMoreActivity.CONTACT_REQUEST_URI),
                        REQUEST_CODE);
            }
        });

        ClassPresenterSelector mPresenterSelector = new ClassPresenterSelector();
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == TVContactMoreFragment.DELETE)
                finishView();
        }
    }

    public void showContact(SmartListViewModel model) {
        Context context = requireContext();
        final DetailsOverviewRow row = new DetailsOverviewRow(model);
        AvatarDrawable avatar =
                new AvatarDrawable.Builder()
                        .withViewModel(model)
                        //.withPresence(false)
                        .withCircleCrop(false)
                        .build(context);
        avatar.setInSize(iconSize);
        row.setImageDrawable(avatar);

        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();
        if (isIncomingRequest) {
            adapter.set(ACTION_ACCEPT, new Action(ACTION_ACCEPT, getResources()
                    .getString(R.string.accept)));
            adapter.set(ACTION_REFUSE, new Action(ACTION_REFUSE, getResources().getString(R.string.refuse)));
            adapter.set(ACTION_BLOCK, new Action(ACTION_BLOCK, getResources().getString(R.string.block)));
        } else if (isOutgoingRequest) {
            adapter.set(ACTION_ADD_CONTACT, new Action(ACTION_ADD_CONTACT, getResources().getString(R.string.ab_action_contact_add)));
        } else {
            adapter.set(ACTION_CALL, new Action(ACTION_CALL, getResources().getString(R.string.ab_action_video_call),
                    null, context.getDrawable(R.drawable.baseline_videocam_24)));
            adapter.set(ACTION_MORE, new Action(ACTION_MORE, getResources().getString(R.string.tv_action_more),
                    null, context.getDrawable(R.drawable.baseline_more_vert_24)));
        }
        row.setActionsAdapter(adapter);

        mAdapter.add(row);
    }

    @Override
    public void callContact(String accountId, Uri conversationUri, Uri uri) {
        startActivity(new Intent(Intent.ACTION_CALL)
                .setClass(requireContext(), TVCallActivity.class)
                .putExtras(ConversationPath.toBundle(accountId, conversationUri))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, uri.getUri()));
    }

    @Override
    public void goToCallActivity(String id) {
        startActivity(new Intent(requireContext(), TVCallActivity.class)
                .putExtra(NotificationService.KEY_CALL_ID, id));
    }

    @Override
    public void switchToConversationView() {
        isIncomingRequest = false;
        isOutgoingRequest = false;
        setupAdapter();
        presenter.setContact(mConversationPath);
    }

    @Override
    public void finishView() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposableBag.dispose();
    }

}
