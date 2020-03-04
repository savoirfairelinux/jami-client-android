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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.client.QRCodeActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.services.AccountService;
import cx.ring.smartlist.SmartListPresenter;
import cx.ring.smartlist.SmartListView;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import cx.ring.viewholders.SmartListViewHolder;

public class SmartListFragment extends BaseSupportFragment<SmartListPresenter> implements SearchView.OnQueryTextListener,
        SmartListViewHolder.SmartListListeners,
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        SmartListView
{
    private static final String TAG = SmartListFragment.class.getSimpleName();
    private static final String STATE_LOADING = TAG + ".STATE_LOADING";
    public static final String KEY_ACCOUNT_ID = "accountId";

    private static final int SCROLL_DIRECTION_UP = -1;

    @BindView(R.id.list_coordinator)
    protected CoordinatorLayout mCoordinator;

    @BindView(R.id.newconv_fab)
    protected ExtendedFloatingActionButton mFloatingActionButton;

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

    @Inject
    AccountService mAccountService;

    private SmartListAdapter mSmartListAdapter;

    private SearchView mSearchView = null;
    private MenuItem mSearchMenuItem = null;
    private MenuItem mDialpadMenuItem = null;

    @Override
    public void onResume() {
        super.onResume();
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
                changeSeparatorHeight(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mDialpadMenuItem.setVisible(true);
                mFloatingActionButton.hide();
                setOverflowMenuVisible(menu, false);
                changeSeparatorHeight(true);
                return true;
            }
        });

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.searchbar_hint));
        mSearchView.setLayoutParams(new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.MATCH_PARENT));
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_GO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            EditText editText = mSearchView.findViewById(R.id.search_src_text);
            if (editText != null) {
                editText.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity  activity = getActivity();
        Intent intent = activity == null ? null : activity.getIntent();
        if (intent != null)
            handleIntent(intent);
    }

    public void handleIntent(@NonNull Intent intent) {
        if (mSearchView != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Intent.ACTION_VIEW:
                case Intent.ACTION_CALL:
                    mSearchView.setQuery(intent.getDataString(), true);
                    break;
                case Intent.ACTION_DIAL:
                    mSearchMenuItem.expandActionView();
                    mSearchView.setQuery(intent.getDataString(), false);
                    break;
                case Intent.ACTION_SEARCH:
                    mSearchMenuItem.expandActionView();
                    mSearchView.setQuery(intent.getStringExtra(SearchManager.QUERY), true);
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
                    mDialpadMenuItem.setIcon(R.drawable.baseline_dialpad_24);
                } else {
                    mSearchView.setInputType(EditorInfo.TYPE_CLASS_PHONE);
                    mDialpadMenuItem.setIcon(R.drawable.baseline_keyboard_24);
                }
                return true;
            case R.id.menu_scan_qr:
                presenter.clickQRSearch();
                return true;
            case R.id.menu_settings:
                ((HomeActivity) getActivity()).goToSettings();
                return true;
            case R.id.menu_about:
                ((HomeActivity) getActivity()).goToAbout();
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        mNewContact.setVisibility(View.GONE);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                boolean canScrollUp = recyclerView.canScrollVertically(SCROLL_DIRECTION_UP);
                boolean isExtended = mFloatingActionButton.isExtended();

                if (dy > 0 && isExtended) {
                    mFloatingActionButton.shrink();
                } else if ((dy < 0 || !canScrollUp) && !isExtended) {
                    mFloatingActionButton.extend();
                }

                ((HomeActivity) getActivity()).setToolbarElevation(mRecyclerView.canScrollVertically(SCROLL_DIRECTION_UP));
            }
        });

        DefaultItemAnimator animator = (DefaultItemAnimator) mRecyclerView.getItemAnimator();
        if (animator != null) {
            animator.setSupportsChangeAnimations(false);
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
            MenuItem overflowMenuItem = menu.findItem(R.id.menu_overflow);
            if (null != overflowMenuItem) {
                overflowMenuItem.setVisible(visible);
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
        if (mCoordinator != null) {
            String snackbarText = getString(R.string.conversation_action_copied_peer_number_clipboard,
                    ActionHelper.getShortenedNumber(copiedNumber));
            Snackbar.make(mCoordinator, snackbarText, Snackbar.LENGTH_LONG).show();
        }
    }

    public void onFabButtonClicked() {
        presenter.fabButtonClicked();
    }

    private void showErrorPanel(final int textResId,
                                final boolean showImage,
                                @Nullable View.OnClickListener clickListener) {
        if (mErrorMessagePane != null) {
            mErrorMessagePane.setVisibility(View.VISIBLE);
            mErrorMessagePane.setOnClickListener(clickListener);
        }
        if (mErrorMessageTextView != null) {
            mErrorMessageTextView.setText(textResId);
        }
        if (mErrorImageView != null) {
            mErrorImageView.setVisibility(showImage ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void displayNetworkErrorPanel() {
        showErrorPanel(R.string.error_no_network, false, null);
    }

    @Override
    public void displayMobileDataPanel() {
        showErrorPanel(R.string.error_mobile_network_available_but_disabled,
                true,
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
        final Context context = requireContext();
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.choose_number)
                .setItems(numbers, (dialog, which) -> {
                    CharSequence selected = numbers[which];
                    Intent intent = new Intent(CallActivity.ACTION_CALL)
                            .setClass(context, CallActivity.class)
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
        new MaterialAlertDialogBuilder(requireContext())
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

        View view = getView();
        if (view != null) {
            ViewGroup container = view.findViewById(R.id.conversation_container);
            if (container != null && container.getChildCount() == 0) {
                selectFirstItem();
            }
        }
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HomeActivity.REQUEST_CODE_QR_CONVERSATION && data != null && resultCode == Activity.RESULT_OK) {
            String contactID = data.getStringExtra(ConversationFragment.KEY_CONTACT_RING_ID);
            if (contactID != null) {
                presenter.startConversation(new cx.ring.model.Uri(contactID));
            }
        }
    }

    @Override
    public void goToConversation(String accountId, cx.ring.model.Uri contactId) {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }

        if (!DeviceUtils.isTablet(getContext())) {
            startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, contactId.toString()), requireContext(), ConversationActivity.class));
        } else {
            ((HomeActivity) requireActivity()).startConversationTablet(ConversationPath.toBundle(accountId, contactId.toString()));
        }
    }

    @Override
    public void goToCallActivity(String accountId, String contactId) {
        Intent intent = new Intent(CallActivity.ACTION_CALL)
                .setClass(requireActivity(), CallActivity.class)
                .putExtra(CallFragment.KEY_AUDIO_ONLY, false)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
    }

    @Override
    public void goToQRActivity() {
        Intent intent = new Intent(QRCodeActivity.ACTION_SCAN)
                .setClass(requireActivity(), QRCodeActivity.class);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_QR_CONVERSATION);
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

    private void changeSeparatorHeight(boolean open) {
        if (DeviceUtils.isTablet(getActivity())) {
            int margin;

            View separator = getView().findViewById(R.id.separator);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) separator.getLayoutParams();
            if (open) {
                Toolbar toolbar = getActivity().findViewById(R.id.main_toolbar);
                margin = toolbar.getHeight();
            } else {
                margin = 0;
            }

            params.topMargin = margin;
            separator.setLayoutParams(params);
        }
    }

    private void selectFirstItem() {
        if (mSmartListAdapter != null && mSmartListAdapter.getItemCount() > 0) {
            new Handler().postDelayed(() -> {
                if (mRecyclerView != null) {
                    RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(0);
                    if (holder != null)
                        holder.itemView.performClick();
                }

            }, 100);
        }
    }

}
