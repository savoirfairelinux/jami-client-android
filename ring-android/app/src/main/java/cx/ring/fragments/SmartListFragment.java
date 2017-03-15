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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Collection;

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
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.mvp.BaseFragment;
import cx.ring.service.LocalService;
import cx.ring.smartlist.SmartListPresenter;
import cx.ring.smartlist.SmartListView;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.NetworkUtils;

public class SmartListFragment extends BaseFragment<SmartListPresenter> implements SearchView.OnQueryTextListener,
        HomeActivity.Refreshable,
        SmartListAdapter.SmartListAdapterCallback,
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        SmartListView {


    private static final String TAG = SmartListFragment.class.getSimpleName();
    private static final String STATE_LOADING = TAG + ".STATE_LOADING";

    @Inject
    protected SmartListPresenter smartListPresenter;

    @BindView(R.id.newconv_fab)
    protected FloatingActionButton mFloatingActionButton;

    @BindView(R.id.confs_list)
    protected ListView mList;

    @BindView(R.id.loading_indicator)
    protected ProgressBar mLoader;

    @BindView(R.id.emptyTextView)
    protected TextView mEmptyTextView = null;

    @BindView(R.id.newcontact_element)
    protected ViewGroup mNewContact;

    @BindView(R.id.error_msg_pane)
    protected ViewGroup mErrorMessagePane;

    @BindView(R.id.error_msg_txt)
    protected TextView mErrorMessageTextView;

    @BindView(R.id.error_image_view)
    protected ImageView mErrorImageView;

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;
    private SmartListAdapter mSmartListAdapter;

    private SearchView mSearchView = null;
    private MenuItem mSearchMenuItem = null;
    private MenuItem mDialpadMenuItem = null;

    private Boolean isTabletMode = false;
    private ConversationFragment mConversationFragment;

    public static final int REQUEST_TRANSFER = 10;
    public static final int REQUEST_CONF = 20;

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
    }

    public void refresh() {

        LocalService service = mCallbacks.getService();
        if (service == null) {
            Log.e(TAG, "refresh: null service");
            return;
        }

        if (mSmartListAdapter == null) {
            bindService(getActivity(), service);
        }

        smartListPresenter.refresh(NetworkUtils.isConnectedWifi(getActivity()),
                NetworkUtils.isConnectedMobile(getActivity()));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
        mCallbacks = LocalService.DUMMY_CALLBACKS;
    }

    @Override
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
        refresh();
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
                smartListPresenter.clickQRSearch();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mNewContact.callOnClick();
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
        smartListPresenter.queryTextChanged(query);
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

        mList.setOnItemClickListener(conversationClickListener);
        mList.setOnItemLongClickListener(conversationLongClickListener);

        if (savedInstanceState != null) {
            this.setLoading(savedInstanceState.getBoolean(STATE_LOADING, false));
        }

        mNewContact.setVisibility(View.GONE);

        LocalService service = mCallbacks.getService();
        if (service != null) {
            bindService(inflater.getContext(), service);
        }

        if (ConversationFragment.isTabletMode(getActivity())) {
            isTabletMode = true;
        }

        return inflatedView;
    }

    @OnClick(R.id.newcontact_element)
    void newContactClicked(View v) {
        smartListPresenter.newContactClicked((CallContact) v.getTag());
    }

    @OnClick(R.id.quick_call)
    void quickCallClicked(View v) {
        CallContact callContact = (CallContact) mNewContact.getTag();
        smartListPresenter.quickCallClicked(callContact);
    }

    @OnClick(R.id.newconv_fab)
    void fabButtonClicked(View v) {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.expandActionView();
        }
    }

    public void bindService(final Context ctx, final LocalService service) {
        mSmartListAdapter = new SmartListAdapter(ctx,
                service.get40dpContactCache(),
                service.getThreadPool());

        mSmartListAdapter.setCallback(this);
        if (mList != null) {
            mList.setAdapter(mSmartListAdapter);
        }
    }

    public void startConversationTablet(Bundle bundle) {
        mConversationFragment = new ConversationFragment();
        mConversationFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .replace(R.id.conversation_container, mConversationFragment, mConversationFragment.getClass().getName())
                .commit();
    }

    private final OnItemClickListener conversationClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
            presenter.conversationClicked(((SmartListAdapter.ViewHolder) v.getTag()).conv.getContact());
        }
    };

    private final AdapterView.OnItemLongClickListener conversationLongClickListener =
            new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                    presentActions(getActivity(),
                            ((SmartListAdapter.ViewHolder) v.getTag()).conv,
                            SmartListFragment.this);
                    return true;
                }
            };

    public static void presentActions(final Activity activity,
                                      final Conversation conversation,
                                      final Conversation.ConversationActionCallback callback) {
        if (activity == null) {
            cx.ring.utils.Log.d(TAG, "presentActions: activity is null");
            return;
        }

        if (conversation == null) {
            cx.ring.utils.Log.d(TAG, "presentActions: conversation is null");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setItems(R.array.conversation_actions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        ActionHelper.launchCopyNumberToClipboardFromContact(activity,
                                conversation.getContact(),
                                callback);
                        break;
                    case 1:
                        ActionHelper.launchDeleteAction(activity, conversation, callback);
                        break;
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Conference transfer;
        if (requestCode == REQUEST_TRANSFER) {
            switch (resultCode) {
                case 0:
                    Conference c = data.getParcelableExtra("target");
                    transfer = data.getParcelableExtra("transfer");
                    try {
                        mCallbacks.getService().getRemoteService().attendedTransfer(transfer.getParticipants().get(0).getCallId(), c.getParticipants().get(0).getCallId());
                        mSmartListAdapter.notifyDataSetChanged();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error on Transfer", e);
                    }
                    Toast.makeText(getActivity(), getString(cx.ring.R.string.home_transfer_complet), Toast.LENGTH_LONG).show();
                    break;

                case 1:
                    String to = data.getStringExtra("to_number");
                    transfer = data.getParcelableExtra("transfer");
                    try {
                        Toast.makeText(getActivity(), getString(cx.ring.R.string.home_transfering, transfer.getParticipants().get(0).getContact().getDisplayName(), to),
                                Toast.LENGTH_SHORT).show();
                        mCallbacks.getService().getRemoteService().transfer(transfer.getParticipants().get(0).getCallId(), to);
                        mCallbacks.getService().getRemoteService().hangUp(transfer.getParticipants().get(0).getCallId());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error on Transfer", e);
                    }
                    break;

                default:
                    break;
            }
        } else if (requestCode == REQUEST_CONF) {
            switch (resultCode) {
                case 0:
                    Conference callToAdd = data.getParcelableExtra("transfer");
                    Conference callTarget = data.getParcelableExtra("target");

                    bindCalls(callToAdd, callTarget);
                    break;

                default:
                    break;
            }
        } else if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null && resultCode == Activity.RESULT_OK) {
                String contact_uri = scanResult.getContents();
                smartListPresenter.startConversation(CallContact.buildUnknown(contact_uri));
            }
        }

    }

    private void bindCalls(Conference callToAdd, Conference callTarget) {
        try {
            Log.i(TAG, "joining calls:" + callToAdd.getId() + " and " + callTarget.getId());

            if (callTarget.hasMultipleParticipants() && !callToAdd.hasMultipleParticipants()) {
                mCallbacks.getService()
                        .getRemoteService()
                        .addParticipant(
                                callToAdd.getParticipants().get(0).getCallId(),
                                callTarget.getId()
                        );
            } else if (callTarget.hasMultipleParticipants() && callToAdd.hasMultipleParticipants()) {
                // We join two conferences
                mCallbacks.getService()
                        .getRemoteService()
                        .joinConference(
                                callToAdd.getId(),
                                callTarget.getId()
                        );
            } else if (!callTarget.hasMultipleParticipants() && callToAdd.hasMultipleParticipants()) {
                mCallbacks.getService()
                        .getRemoteService()
                        .addParticipant(
                                callTarget.getParticipants().get(0).getCallId(),
                                callToAdd.getId()
                        );
            } else {
                // We join two single calls to create a conf
                mCallbacks.getService()
                        .getRemoteService()
                        .joinParticipant(
                                callToAdd.getParticipants().get(0).getCallId(),
                                callTarget.getParticipants().get(0).getCallId()
                        );
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error on bindCalls", e);
        }
    }

    private void setLoading(boolean loading) {
        if (null != this.mLoader) {
            int loaderVisibility = (loading) ? View.VISIBLE : View.GONE;
            this.mLoader.setVisibility(loaderVisibility);
            if (loading) {
                this.mEmptyTextView.setText("");
            } else {
                String emptyText = getResources().getQuantityString(R.plurals.home_conferences_title, 0, 0);
                this.mEmptyTextView.setText(emptyText);
            }

            if (null != mList) {
                mList.setEmptyView(this.mEmptyTextView);
            }
        }
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
    public void pictureTapped(Conversation conversation) {
        if (conversation == null) {
            Log.d(TAG, "pictureTapped, conversation is null");
            return;
        }
        if (conversation.getContact() != null) {
            Activity activity = getActivity();
            if (activity != null) {
                ActionHelper.displayContact(getActivity(), conversation.getContact());
            }
        }
    }

    @Override
    public void deleteConversation(Conversation conversation) {
        smartListPresenter.deleteConversation(conversation);
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

    public void displayNewContactRowWithName(String name, String address) {
        ((TextView) mNewContact.findViewById(R.id.display_name)).setText(name);
        CallContact contact = CallContact.buildUnknown(name, address);
        mNewContact.setTag(contact);
        mNewContact.setVisibility(View.VISIBLE);
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
    public void hideSearchRow() {
        mNewContact.setVisibility(View.GONE);
    }

    @Override
    public void hideErrorPanel() {
        if (mErrorMessagePane != null) {
            mErrorMessagePane.setVisibility(View.GONE);
        }
    }

    @Override
    public void updateView(final Collection<Conversation> list, String query) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSmartListAdapter.updateDataset(list, null);
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
    protected SmartListPresenter createPresenter() {
        return smartListPresenter;
    }

    @Override
    protected void initPresenter(SmartListPresenter presenter) {
        smartListPresenter.init();
    }

    @Override
    public void showViewModel(Object viewModel) {

    }
}
