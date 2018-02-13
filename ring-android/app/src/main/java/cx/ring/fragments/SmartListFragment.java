/*
 *  Copyright (C) 2004-2017 Savoir-faire Linux Inc.
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
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
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

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.client.QRCodeScannerActivity;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.mvp.BaseFragment;
import cx.ring.smartlist.SmartListPresenter;
import cx.ring.smartlist.SmartListView;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ClipboardHelper;
import cx.ring.viewholders.SmartListViewHolder;

public class SmartListFragment extends BaseFragment<SmartListPresenter> implements SearchView.OnQueryTextListener,
        SmartListViewHolder.SmartListListeners,
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        SmartListView {

    public static final String TAG = SmartListFragment.class.getSimpleName();
    private static final String STATE_LOADING = TAG + ".STATE_LOADING";

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
                displayFloatingActionButtonWithDelay(true, 50);
                setOverflowMenuVisible(menu, true);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mDialpadMenuItem.setVisible(true);
                displayFloatingActionButtonWithDelay(false, 0);
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
                if (mSearchView.getInputType() == EditorInfo.TYPE_CLASS_PHONE)
                    mSearchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                else
                    mSearchView.setInputType(EditorInfo.TYPE_CLASS_PHONE);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        mNewContact.setVisibility(View.GONE);

        if (ConversationFragment.isTabletMode(getActivity())) {
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null && resultCode == Activity.RESULT_OK) {
                String contact_uri = scanResult.getContents();
                presenter.startConversation(CallContact.buildUnknown(contact_uri));
            }
        }
    }

    @Override
    public void setLoading(final boolean loading) {
        getActivity().runOnUiThread(() -> {
            if (mLoader == null) {
                return;
            }
            mLoader.setVisibility(loading ? View.VISIBLE : View.GONE);
        });
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

    /**
     * Hides or displays the floating action button after a delay
     *
     * @param visible true to display, false to hide
     * @param delay   time in ms
     */
    private void displayFloatingActionButtonWithDelay(boolean visible, int delay) {
        if (this.mFloatingActionButton != null) {
            final int visibility = (visible) ? View.VISIBLE : View.GONE;
            new Handler().postDelayed(() -> mFloatingActionButton.setVisibility(visibility), delay);
        }
    }

    @Override
    public void deleteConversation(CallContact callContact) {
        presenter.deleteConversation(callContact);
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
        getActivity().runOnUiThread(() -> showErrorPanel(R.string.error_no_network, false, 0, null));
    }

    @Override
    public void displayMobileDataPanel() {
        getActivity().runOnUiThread(() -> showErrorPanel(R.string.error_mobile_network_available_but_disabled,
                true,
                R.drawable.ic_settings_white,
                v -> {
                    Activity activity = getActivity();
                    if (activity != null && activity instanceof HomeActivity) {
                        HomeActivity homeActivity = (HomeActivity) activity;
                        homeActivity.goToSettings();
                    }
                }));
    }

    @Override
    public void displayNewContactRowWithName(final String name) {
        getActivity().runOnUiThread(() -> {
            if (mNewContact == null) {
                return;
            }
            ((TextView) mNewContact.findViewById(R.id.display_name)).setText(name);
            mNewContact.setVisibility(View.VISIBLE);
        });
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
                        case ActionHelper.ACTION_DELETE:
                            presenter.deleteConversation(smartListViewModel);
                            break;
                        case ActionHelper.ACTION_BLOCK:
                            presenter.removeContact(smartListViewModel);
                            break;
                    }
                })
                .show();
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
        getActivity().runOnUiThread(() -> {
            if (mNewContact == null) {
                return;
            }
            mNewContact.setVisibility(View.GONE);
        });
    }

    @Override
    public void hideErrorPanel() {
        getActivity().runOnUiThread(() -> {
            if (mErrorMessagePane == null) {
                return;
            }
            mErrorMessagePane.setVisibility(View.GONE);
        });
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
    public void updateList(final ArrayList<SmartListViewModel> smartListViewModels) {
        if (mRecyclerView.getAdapter() == null) {
            mSmartListAdapter = new SmartListAdapter(smartListViewModels, SmartListFragment.this);
            mRecyclerView.setAdapter(mSmartListAdapter);
            mRecyclerView.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(llm);
        }
        mRecyclerView.setVisibility(View.VISIBLE);
        mSmartListAdapter.update(smartListViewModels);
    }

    @Override
    public void goToConversation(String accountId, String contactId) {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }

        if (!isTabletMode) {
            Intent intent = new Intent()
                    .setClass(getActivity(), ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
            startActivity(intent);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
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
        QRCodeScannerActivity.startQRCodeScanWithFragmentReceiver(this);
    }

    @Override
    public void goToContact(CallContact callContact) {
        Activity activity = getActivity();
        if (activity != null) {
            ActionHelper.displayContact(getActivity(), callContact);
        }
    }

    @Override
    public void scrollToTop() {
        getActivity().runOnUiThread(() -> {
            if (mRecyclerView == null) {
                return;
            }
            mRecyclerView.scrollToPosition(0);
        });
    }

    @Override
    public void onItemClick(SmartListViewModel smartListViewModel) {
        presenter.conversationClicked(smartListViewModel);
    }

    @Override
    public void onItemLongClick(SmartListViewModel smartListViewModel) {
        presenter.conversationLongClicked(smartListViewModel);
    }

    @Override
    public void onPhotoClick(SmartListViewModel smartListViewModel) {
        presenter.photoClicked(smartListViewModel);
    }
}
