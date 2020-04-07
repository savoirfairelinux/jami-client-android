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
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
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

import java.io.File;
import java.util.List;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.conversation.ConversationView;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransfer;
import cx.ring.model.Interaction;
import cx.ring.model.Uri;
import cx.ring.services.NotificationService;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.contactrequest.TVContactRequestDetailPresenter;
import cx.ring.tv.main.BaseDetailFragment;
import cx.ring.model.TVListViewModel;
import cx.ring.views.AvatarDrawable;

public class TVContactFragment extends BaseDetailFragment<ConversationPresenter> implements ConversationView {

    private static final int ACTION_CALL = 0;
    private static final int ACTION_DELETE = 1;
    private static final int ACTION_ACCEPT = 2;
    private static final int ACTION_REFUSE = 3;
    private static final int ACTION_BLOCK = 4;
    private static final int ACTION_ADD_CONTACT = 5;
    private static final int ACTION_CLEAR_HISTORY = 6;

    private ArrayObjectAdapter mAdapter;
    private int iconSize = -1;

    private boolean isIncomingRequest = false;
    private boolean isOutgoingRequest = false;

    private Uri mContactUri;
    private String mAccountId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((JamiApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String type = getActivity().getIntent().getType();
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

        mAccountId = getActivity().getIntent().getStringExtra(ConversationFragment.KEY_ACCOUNT_ID);
        mContactUri = (Uri) getActivity().getIntent().getSerializableExtra(ConversationFragment.KEY_CONTACT_RING_ID);
        presenter.init(mContactUri, mAccountId);

        // Override down navigation as we do not use it in this screen
        // Only the detailPresenter will be displayed

        prepareBackgroundManager();
        setupAdapter();
        Resources res = getResources();
        iconSize = res.getDimensionPixelSize(R.dimen.tv_avatar_size);
        presenter.setContact();
    }

    private void prepareBackgroundManager() {
        Activity activity = requireActivity();
        BackgroundManager mBackgroundManager = BackgroundManager.getInstance(activity);
        mBackgroundManager.attach(activity.getWindow());
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

        detailsPresenter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_900));
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
            } else if (action.getId() == ACTION_DELETE) {
                presenter.removeContact();
            } else if (action.getId() == ACTION_CLEAR_HISTORY) {
                presenter.clearHistory();
            } else if (action.getId() == ACTION_ADD_CONTACT) {
                presenter.onAddContact();
            } else if (action.getId() == ACTION_ACCEPT) {
                presenter.onAcceptIncomingContactRequest();
            } else if (action.getId() == ACTION_REFUSE) {
                presenter.onRefuseIncomingContactRequest();
            } else if (action.getId() == ACTION_BLOCK) {
                presenter.onBlockIncomingContactRequest();
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
        AvatarDrawable avatar =
                new AvatarDrawable.Builder()
                        .withContact(model.getContact())
                        .withPresence(false)
                        .withCircleCrop(false)
                        .build(getActivity());
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
                    null, requireContext().getDrawable(R.drawable.baseline_videocam_24)));
            adapter.set(ACTION_DELETE, new Action(ACTION_DELETE, getResources().getString(R.string.conversation_action_remove_this)));
            adapter.set(ACTION_CLEAR_HISTORY, new Action(ACTION_CLEAR_HISTORY, getResources().getString(R.string.conversation_action_history_clear)));
        }
        row.setActionsAdapter(adapter);

        mAdapter.add(row);
    }

    @Override
    public void goToCallActivity(String id) {
        Context context = requireContext();
        Intent intent = new Intent(context, TVCallActivity.class);
        intent.putExtra(NotificationService.KEY_CALL_ID, id);
        context.startActivity(intent, null);
    }

    @Override
    public void goToCallActivityWithResult(String accountId, String contactRingId, boolean audioOnly) {
        Context context = requireContext();
        Intent intent = new Intent(context, TVCallActivity.class);
        intent.putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId);
        intent.putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactRingId);
        context.startActivity(intent, null);
    }

    @Override
    public void switchToConversationView() {
        isIncomingRequest = false;
        isOutgoingRequest = false;
        setupAdapter();
        presenter.setContact();
    }

    @Override
    public void finishView() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    @Override
    public void refreshView(List<Interaction> conversation) {

    }

    @Override
    public void scrollToEnd() {

    }

    @Override
    public void displayContact(CallContact contact) {

    }

    @Override
    public void displayOnGoingCallPane(boolean display) {

    }

    @Override
    public void displayNumberSpinner(Conversation conversation, Uri number) {

    }

    @Override
    public void hideNumberSpinner() {

    }

    @Override
    public void clearMsgEdit() {

    }

    @Override
    public void goToHome() {

    }

    @Override
    public void goToAddContact(CallContact callContact) {

    }

    @Override
    public void goToContactActivity(String accountId, String contactRingId) {

    }

    @Override
    public void switchToUnknownView(String name) {

    }

    @Override
    public void switchToIncomingTrustRequestView(String message) {

    }

    @Override
    public void askWriteExternalStoragePermission() {

    }

    @Override
    public void openFilePicker() {

    }

    @Override
    public void shareFile(File path) {

    }

    @Override
    public void openFile(File path) {

    }

    @Override
    public void addElement(Interaction e) {

    }

    @Override
    public void updateElement(Interaction e) {

    }

    @Override
    public void removeElement(Interaction e) {

    }

    @Override
    public void setComposingStatus(Account.ComposingStatus composingStatus) {

    }

    @Override
    public void setLastDisplayed(Interaction interaction) {

    }

    @Override
    public void setConversationColor(int integer) {

    }

    @Override
    public void startSaveFile(DataTransfer currentFile, String fileAbsolutePath) {

    }

    @Override
    public void startShareLocation(String accountId, String contactId) {

    }

    @Override
    public void showMap(String accountId, String contactId, boolean open) {

    }

    @Override
    public void hideMap() {

    }

    @Override
    public void hideErrorPanel() {

    }

    @Override
    public void displayNetworkErrorPanel() {

    }
}
