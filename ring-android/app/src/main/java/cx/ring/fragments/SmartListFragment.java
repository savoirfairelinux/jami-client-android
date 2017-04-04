/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.application.RingApplication;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.client.QRCodeScannerActivity;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.mvp.BaseFragment;
import cx.ring.smartlist.SmartListPresenter;
import cx.ring.smartlist.SmartListView;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.NetworkUtils;
import cx.ring.viewholders.SmartListViewHolder;

public class SmartListFragment extends BaseFragment<SmartListPresenter> implements SearchView.OnQueryTextListener,
        SmartListViewHolder.SmartListListeners,
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        SmartListView {

    public static final String TAG = SmartListFragment.class.getSimpleName();
    private static final String STATE_LOADING = TAG + ".STATE_LOADING";

    @Inject
    protected SmartListPresenter mSmartListPresenter;

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
    private ConversationFragment mConversationFragment;

    public void onResume() {
        super.onResume();

        if (mConversationFragment == null) {
            // is there an old persistent fragment to clean ?
            Fragment fragment = getFragmentManager().findFragmentByTag(ConversationFragment.class.getName());
            if (fragment != null) {
                getFragmentManager().beginTransaction().remove(fragment).commit();
            }
        }

        if (mConversationFragment != null && ConversationFragment.isTabletMode(getActivity())) {
            startConversationTablet(mConversationFragment.getArguments());
        }

        Log.d(TAG, "onResume");
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.app_name);

        mSmartListPresenter.refresh(NetworkUtils.isConnectedWifi(getActivity()),
                NetworkUtils.isConnectedMobile(getActivity()));
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        menu.clear();

        inflater.inflate(R.menu.smartlist_menu, menu);
        mSearchMenuItem = menu.findItem(R.id.menu_contact_search);
        mDialpadMenuItem = menu.findItem(R.id.menu_contact_dial);
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mDialpadMenuItem.setVisible(false);
                displayFloatingActionButtonWithDelay(true, 50);
                setOverflowMenuVisible(menu, true);
                setLoading(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mDialpadMenuItem.setVisible(true);
                displayFloatingActionButtonWithDelay(false, 0);
                setOverflowMenuVisible(menu, false);
                setLoading(false);
                return true;
            }
        });

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.searchbar_hint));
        mSearchView.setLayoutParams(new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT));
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_GO);

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
                mSmartListPresenter.clickQRSearch();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSmartListPresenter.newContactClicked();
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
        mSmartListPresenter.queryTextChanged(query);
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        setHasOptionsMenu(true);
        View inflatedView = inflater.inflate(cx.ring.R.layout.frag_smartlist, container, false);

        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        if (savedInstanceState != null) {
            this.setLoading(savedInstanceState.getBoolean(STATE_LOADING, false));
        }

        mNewContact.setVisibility(View.GONE);

        if (ConversationFragment.isTabletMode(getActivity())) {
            isTabletMode = true;
        }

        return inflatedView;
    }

    public void refresh() {
        mSmartListPresenter.refresh(NetworkUtils.isConnectedWifi(getActivity()),
                NetworkUtils.isConnectedMobile(getActivity()));
    }

    @OnClick(R.id.newcontact_element)
    void newContactClicked(View v) {
        mSmartListPresenter.newContactClicked();
    }

    @OnClick(R.id.quick_call)
    void quickCallClicked(View v) {
        mSmartListPresenter.quickCallClicked();
    }

    @OnClick(R.id.newconv_fab)
    void fabButtonClicked(View v) {
        mSmartListPresenter.fabButtonClicked();
    }

    public void startConversationTablet(Bundle bundle) {
        mConversationFragment = new ConversationFragment();
        mConversationFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .replace(R.id.conversation_container, mConversationFragment, mConversationFragment.getClass().getName())
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null && resultCode == Activity.RESULT_OK) {
                String contact_uri = scanResult.getContents();
                mSmartListPresenter.startConversation(CallContact.buildUnknown(contact_uri));
            }
        }
    }

    @Override
    public void setLoading(final boolean loading) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLoader == null) {
                    return;
                }
                mLoader.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
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
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFloatingActionButton.setVisibility(visibility);
                }
            }, delay);
        }
    }

    @Override
    public void deleteConversation(Conversation conversation) {
        mSmartListPresenter.deleteConversation(conversation);
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
        this.showErrorPanel(R.string.error_no_network, false, 0, null);
    }

    @Override
    public void displayMobileDataPanel() {
        this.showErrorPanel(R.string.error_mobile_network_available_but_disabled,
                true,
                R.drawable.ic_settings_white,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Activity activity = getActivity();
                        if (activity != null && activity instanceof HomeActivity) {
                            HomeActivity homeActivity = (HomeActivity) activity;
                            homeActivity.goToSettings();
                        }
                    }
                });
    }

    @Override
    public void displayNewContactRowWithName(final String name) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) mNewContact.findViewById(R.id.display_name)).setText(name);
                mNewContact.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void displayChooseNumberDialog(final CharSequence[] numbers) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_number);
        builder.setItems(numbers, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CharSequence selected = numbers[which];
                Intent intent = new Intent(CallActivity.ACTION_CALL)
                        .setClass(getActivity(), CallActivity.class)
                        .setData(Uri.parse(selected.toString()));
                startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
            }
        });
        builder.show();
    }

    @Override
    public void displayNoConversationMessage() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String emptyText = getResources().getQuantityString(R.plurals.home_conferences_title, 0, 0);
                mEmptyTextView.setText(emptyText);
                mEmptyTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void displayConversationDialog(final Conversation conversation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.conversation_actions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        ActionHelper.launchCopyNumberToClipboardFromContact(getActivity(),
                                conversation.getContact(),
                                SmartListFragment.this);
                        break;
                    case 1:
                        ActionHelper.launchDeleteAction(getActivity(), conversation, SmartListFragment.this);
                        break;
                    case 2:
                        presenter.removeContact(conversation.getLastAccountUsed(), conversation.getContact().getDisplayName());
                        break;
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void displayMenuItem() {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.expandActionView();
        }
    }

    @Override
    public void hideSearchRow() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNewContact.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void hideErrorPanel() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mErrorMessagePane.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void hideNoConversationMessage() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEmptyTextView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void updateList(final ArrayList<SmartListViewModel> smartListViewModels) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRecyclerView.getAdapter() == null) {
                    mSmartListAdapter = new SmartListAdapter(smartListViewModels, SmartListFragment.this);
                    mRecyclerView.setAdapter(mSmartListAdapter);
                    mRecyclerView.setHasFixedSize(true);
                    LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                    llm.setOrientation(LinearLayoutManager.VERTICAL);
                    mRecyclerView.setLayoutManager(llm);
                }
                mSmartListAdapter.update(smartListViewModels);
                mRecyclerView.scrollToPosition(0);
                setLoading(false);
            }
        });
    }

    @Override
    public void goToConversation(CallContact callContact) {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }

        if (!isTabletMode) {
            Intent intent = new Intent()
                    .setClass(getActivity(), ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, callContact.getIds().get(0)));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("conversationID", callContact.getIds().get(0));
            startConversationTablet(bundle);
        }
    }

    @Override
    public void goToCallActivity(String rawUriNumber) {
        Intent intent = new Intent(CallActivity.ACTION_CALL)
                .setClass(getActivity(), CallActivity.class)
                .setData(Uri.parse(rawUriNumber));
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
    protected SmartListPresenter createPresenter() {
        return mSmartListPresenter;
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
