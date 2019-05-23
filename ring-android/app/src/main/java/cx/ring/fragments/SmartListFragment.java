/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.client.QRCodeActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.smartlist.SmartListPresenter;
import cx.ring.smartlist.SmartListView;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.DeviceUtils;
import cx.ring.viewholders.SmartListViewHolder;

public class SmartListFragment extends BaseSupportFragment<SmartListPresenter> implements SearchView.OnQueryTextListener,
        SmartListViewHolder.SmartListListeners,
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        SmartListView {

    public static final String TAG = SmartListFragment.class.getSimpleName();
    private static final String STATE_LOADING = TAG + ".STATE_LOADING";
    public static final String KEY_ACCOUNT_ID = "accountId";

    @BindView(R.id.newconv_fab)
    protected FloatingActionButton mFloatingActionButton;

    @BindView(R.id.confs_list)
    protected RecyclerView mRecyclerView;

    @BindView(R.id.loading_indicator)
    protected ProgressBar mLoader;

    @BindView(R.id.empty_text_view)
    protected TextView mEmptyTextView;

    @BindView(R.id.newcontact_element)
    protected ViewGroup mNewContact;

    @BindView(R.id.error_msg_pane)
    protected ViewGroup mErrorMessagePane;

    @BindView(R.id.error_msg_txt)
    protected TextView mErrorMessageTextView;

    @BindView(R.id.error_image_view)
    protected ImageView mErrorImageView;

    private SmartListAdapter mSmartListAdapter;

    private SearchView mSearchView = null;
    private MenuItem mSearchMenuItem = null;
    private MenuItem mDialpadMenuItem = null;

    private Boolean isTabletMode = false;

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.app_name);

        presenter.refresh();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        menu.clear();

        inflater.inflate(R.menu.smartlist_menu, menu);
        mSearchMenuItem = menu.findItem(R.id.menu_contact_search);
        mDialpadMenuItem = menu.findItem(R.id.menu_contact_dial);
        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mDialpadMenuItem.setVisible(false);
                mFloatingActionButton.show();
                setOverflowMenuVisible(menu, true);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mDialpadMenuItem.setVisible(true);
                mFloatingActionButton.hide();
                setOverflowMenuVisible(menu, false);
                return true;
            }
        });

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.searchbar_hint));
        mSearchView.setLayoutParams(new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT));
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_GO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            EditText editText = mSearchView.findViewById(R.id.search_src_text);
            if (editText != null) {
                editText.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
            }
        }

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Intent.ACTION_VIEW:
                case Intent.ACTION_CALL:
                    mSearchView.setQuery(intent.getDataString(), true);
                    break;
                case Intent.ACTION_DIAL:
                    mSearchMenuItem.expandActionView();
                    mSearchView.setQuery(intent.getDataString(), false);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_contact_search:
                mSearchView.setInputType(EditorInfo.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                );
                return false;
            case R.id.menu_contact_dial:
                if (mSearchView.getInputType() == EditorInfo.TYPE_CLASS_PHONE) {
                    mSearchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                } else {
                    mSearchView.setInputType(EditorInfo.TYPE_CLASS_PHONE);
                }
                return true;
            case R.id.menu_scan_qr:
                presenter.clickQRSearch();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        presenter.newContactClicked();
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (null != mLoader) {
            // if there's another fragment on top of this one, when a rotation is done, this fragment is destroyed and
            // in the process of recreating it, as it is not shown on the top of the screen, the "onCreateView" method is never called, so the mLoader is null
            outState.putBoolean(STATE_LOADING, mLoader.isShown());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onQueryTextChange(final String query) {
        presenter.queryTextChanged(query);
        return true;
    }

    @Override
    public int getLayout() {
        return R.layout.frag_smartlist;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        mNewContact.setVisibility(View.GONE);

        if (DeviceUtils.isTablet(getActivity())) {
            isTabletMode = true;
        }
    }

    @OnClick(R.id.newcontact_element)
    void newContactClicked() {
        presenter.newContactClicked();
    }

    @OnClick(R.id.quick_call)
    void quickCallClicked() {
        presenter.quickCallClicked();
    }

    @OnClick(R.id.newconv_fab)
    void fabButtonClicked() {
        presenter.fabButtonClicked();
    }

    @Override
    public void setLoading(final boolean loading) {
        if (mLoader == null) {
            return;
        }
        mLoader.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * Handles the visibility of some menus to hide / show the overflow menu
     *
     * @param menu    the menu containing the menuitems we need to access
     * @param visible true to display the overflow menu, false otherwise
     */
    private void setOverflowMenuVisible(final Menu menu, boolean visible) {
        if (null != menu) {
            MenuItem scanQrMenuItem = menu.findItem(R.id.menu_scan_qr);

            if (null != scanQrMenuItem) {
                scanQrMenuItem.setVisible(visible);
            }
        }
    }

    @Override
    public void removeConversation(CallContact callContact) {
        presenter.removeConversation(callContact);
    }

    @Override
    public void clearConversation(CallContact callContact) {
        presenter.clearConversation(callContact);
    }

    @Override
    public void copyContactNumberToClipboard(String contactNumber) {
        ClipboardHelper.copyNumberToClipboard(getActivity(), contactNumber, this);
    }

    @Override
    public void clipBoardDidCopyNumber(String copiedNumber) {
        if (getView() != null) {
            String snackbarText = getString(R.string.conversation_action_copied_peer_number_clipboard,
                    ActionHelper.getShortenedNumber(copiedNumber));
            Snackbar.make(getView(), snackbarText, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showErrorPanel(final int textResId,
                                final boolean showImage,
                                final int imageResId,
                                @Nullable View.OnClickListener clickListener) {
        if (mErrorMessagePane != null) {
            mErrorMessagePane.setVisibility(View.VISIBLE);
            mErrorMessagePane.setOnClickListener(clickListener);
        }
        if (mErrorMessageTextView != null) {
            mErrorMessageTextView.setText(textResId);
        }
        if (mErrorImageView != null) {
            int visibility = showImage ? View.VISIBLE : View.GONE;
            mErrorImageView.setVisibility(visibility);
            mErrorImageView.setImageResource(imageResId);
        }
    }

    @Override
    public void displayNetworkErrorPanel() {
        showErrorPanel(R.string.error_no_network, false, 0, null);
    }

    @Override
    public void displayMobileDataPanel() {
        showErrorPanel(R.string.error_mobile_network_available_but_disabled,
                true,
                R.drawable.ic_settings_white,
                v -> {
                    Activity activity = getActivity();
                    if (activity instanceof HomeActivity) {
                        HomeActivity homeActivity = (HomeActivity) activity;
                        homeActivity.goToSettings();
                    }
                });
    }

    @Override
    public void displayContact(final CallContact contact) {
        if (mNewContact == null) {
            return;
        }

        TextView display_name = mNewContact.findViewById(R.id.display_name);
        display_name.setText(contact.getRingUsername());

        ImageView photo = mNewContact.findViewById(R.id.photo);

        AvatarFactory.loadGlideAvatar(photo, contact);
        mNewContact.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayChooseNumberDialog(final CharSequence[] numbers) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.choose_number)
                .setItems(numbers, (dialog, which) -> {
                    CharSequence selected = numbers[which];
                    Intent intent = new Intent(CallActivity.ACTION_CALL)
                            .setClass(getActivity(), CallActivity.class)
                            .setData(Uri.parse(selected.toString()));
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
                })
                .show();
    }

    @Override
    public void displayNoConversationMessage() {
        String emptyText = getResources().getQuantityString(R.plurals.home_conferences_title, 0, 0);
        mEmptyTextView.setText(emptyText);
        mEmptyTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayConversationDialog(final SmartListViewModel smartListViewModel) {
        new AlertDialog.Builder(getActivity())
                .setItems(R.array.conversation_actions, (dialog, which) -> {
                    switch (which) {
                        case ActionHelper.ACTION_COPY:
                            presenter.copyNumber(smartListViewModel);
                            break;
                        case ActionHelper.ACTION_CLEAR:
                            presenter.clearConversation(smartListViewModel);
                            break;
                        case ActionHelper.ACTION_DELETE:
                            presenter.removeConversation(smartListViewModel);
                            break;
                        case ActionHelper.ACTION_BLOCK:
                            presenter.banContact(smartListViewModel);
                            break;
                    }
                })
                .show();
    }

    @Override
    public void displayClearDialog(CallContact callContact) {
        ActionHelper.launchClearAction(getActivity(), callContact, SmartListFragment.this);
    }

    @Override
    public void displayDeleteDialog(CallContact callContact) {
        ActionHelper.launchDeleteAction(getActivity(), callContact, SmartListFragment.this);
    }

    @Override
    public void copyNumber(CallContact callContact) {
        ActionHelper.launchCopyNumberToClipboardFromContact(getActivity(), callContact, this);
    }

    @Override
    public void displayMenuItem() {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.expandActionView();
        }
    }

    @Override
    public void hideSearchRow() {
        if (mNewContact == null) {
            return;
        }
        mNewContact.setVisibility(View.GONE);
    }

    @Override
    public void hideErrorPanel() {
        if (mErrorMessagePane == null) {
            return;
        }
        mErrorMessagePane.setVisibility(View.GONE);
    }

    @Override
    public void hideList() {
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void hideNoConversationMessage() {
        mEmptyTextView.setVisibility(View.GONE);
    }

    @Override
    public void updateList(@Nullable final List<SmartListViewModel> smartListViewModels) {
        if (mRecyclerView == null)
            return;
        if (mRecyclerView.getAdapter() == null) {
            mSmartListAdapter = new SmartListAdapter(smartListViewModels, SmartListFragment.this);
            mRecyclerView.setAdapter(mSmartListAdapter);
            mRecyclerView.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            llm.setOrientation(RecyclerView.VERTICAL);
            mRecyclerView.setLayoutManager(llm);
        } else {
            mSmartListAdapter.update(smartListViewModels);
        }
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void update(int position) {
        Log.w(TAG, "update " + position + " " + mSmartListAdapter);
        if (mSmartListAdapter != null) {
            mSmartListAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void update(SmartListViewModel model) {
        if (mSmartListAdapter != null)
            mSmartListAdapter.update(model);
    }

    @Override
    public void goToConversation(String accountId, cx.ring.model.Uri contactId) {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }

        if (!isTabletMode) {
            Intent intent = new Intent()
                    .setClass(getActivity(), ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId.toString());
            startActivity(intent);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactId.toString());
            bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);
            ((HomeActivity) getActivity()).startConversationTablet(bundle);
        }
    }

    @Override
    public void goToCallActivity(String accountId, String contactId) {
        Intent intent = new Intent(CallActivity.ACTION_CALL)
                .setClass(getActivity(), CallActivity.class)
                .putExtra(CallFragment.KEY_AUDIO_ONLY, false)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
    }

    @Override
    public void goToQRActivity() {
        Intent i = new Intent(getActivity(), QRCodeActivity.class);
        if(presenter != null) {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_ACCOUNT_ID, presenter.getAccountID());
            i.putExtras(bundle);
        }
        startActivity(i);
    }

    @Override
    public void goToContact(CallContact callContact) {
        ActionHelper.displayContact(getActivity(), callContact);
    }

    @Override
    public void scrollToTop() {
        if (mRecyclerView != null)
            mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onItemClick(SmartListViewModel smartListViewModel) {
        presenter.conversationClicked(smartListViewModel);
    }

    @Override
    public void onItemLongClick(SmartListViewModel smartListViewModel) {
        presenter.conversationLongClicked(smartListViewModel);
    }
}
