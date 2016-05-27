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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
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

import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.client.QRCodeScannerActivity;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.service.LocalService;
import cx.ring.utils.ClipboardHelper;

public class SmartListFragment extends Fragment implements SearchView.OnQueryTextListener,
        HomeActivity.Refreshable,
        SmartListAdapter.SmartListAdapterCallback,
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback {
    private static final String TAG = SmartListFragment.class.getSimpleName();

    private static final int USER_INPUT_DELAY = 300;
    private static final String STATE_LOADING = TAG + ".STATE_LOADING";

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;
    private SmartListAdapter mSmartListAdapter;

    private FloatingActionButton mFloatingActionButton = null;

    private SearchView mSearchView = null;
    private MenuItem mSearchMenuItem = null;
    private MenuItem mDialpadMenuItem = null;

    private ListView mList = null;
    private ProgressBar mLoader = null;
    private TextView mEmptyTextView = null;

    private ViewGroup mNewContact;
    private ViewGroup mErrorMessagePane;
    private TextView mErrorMessageTextView;
    private ImageView mErrorImageView;

    private Handler mUserInputHandler;

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            if (LocalService.ACTION_CONF_LOADED.equals(intent.getAction())) {
                setLoading(false);
            }
            refresh();
        }
    };

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
        } else {
            mSmartListAdapter.updateDataset(service.getConversations(), null);
        }

        if (service.isConnected()) {
            if (mErrorMessagePane != null) {
                mErrorMessagePane.setVisibility(View.GONE);
            }
        } else {
            this.presentNetworkErrorPanel(service);
        }
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
        mCallbacks = LocalService.DUMMY_CALLBACKS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);
        intentFilter.addAction(LocalService.ACTION_CONF_LOADED);
        intentFilter.addAction(LocalService.ACTION_ACCOUNT_UPDATE);
        getActivity().registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.app_name);
        refresh();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
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

        Intent i = getActivity().getIntent();
        switch (i.getAction()) {
            case Intent.ACTION_VIEW:
            case Intent.ACTION_CALL:
                mSearchView.setQuery(i.getDataString(), true);
                break;
            case Intent.ACTION_DIAL:
                mSearchMenuItem.expandActionView();
                mSearchView.setQuery(i.getDataString(), false);
                break;
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
                QRCodeScannerActivity.startQRCodeScanWithFragmentReceiver(this);
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
        if (TextUtils.isEmpty(query)) {
            mNewContact.setVisibility(View.GONE);
        } else {
            ((TextView) mNewContact.findViewById(R.id.display_name)).setText(query);
            CallContact contact = CallContact.buildUnknown(query);
            mNewContact.setTag(contact);
            mNewContact.setVisibility(View.VISIBLE);
        }

        mUserInputHandler.removeCallbacksAndMessages(null);
        mUserInputHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mCallbacks.getService() == null) {
                    Log.d(TAG, "onQueryTextChange: null service");
                } else {
                    mSmartListAdapter.updateDataset(
                            mCallbacks.getService().getConversations(),
                            query
                    );
                }
            }
        }, USER_INPUT_DELAY);

        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        setHasOptionsMenu(true);
        View inflatedView = inflater.inflate(cx.ring.R.layout.frag_smartlist, container, false);

        this.mUserInputHandler = new Handler();

        mFloatingActionButton = (FloatingActionButton) inflatedView.findViewById(R.id.newconv_fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSearchMenuItem != null) {
                    mSearchMenuItem.expandActionView();
                }
            }
        });

        mList = (ListView) inflatedView.findViewById(cx.ring.R.id.confs_list);
        mList.setOnItemClickListener(conversationClickListener);
        mList.setOnItemLongClickListener(conversationLongClickListener);

        this.mEmptyTextView = (TextView) inflatedView.findViewById(R.id.emptyTextView);
        this.mLoader = (ProgressBar) inflatedView.findViewById(R.id.loading_indicator);

        if (savedInstanceState != null) {
            this.setLoading(savedInstanceState.getBoolean(STATE_LOADING, false));
        }

        mNewContact = (ViewGroup) inflatedView.findViewById(R.id.newcontact_element);
        mNewContact.setVisibility(View.GONE);
        mNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallContact c = (CallContact) v.getTag();
                if (c == null)
                    return;
                startConversation(c);
            }
        });
        mNewContact.findViewById(R.id.quick_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallContact c = (CallContact) mNewContact.getTag();
                if (c != null)
                    ((HomeActivity) getActivity()).onCallContact(c);
            }
        });

        mErrorMessagePane = (ViewGroup) inflatedView.findViewById(R.id.error_msg_pane);
        mErrorMessageTextView = (TextView) mErrorMessagePane.findViewById(R.id.error_msg_txt);
        mErrorImageView = (ImageView) mErrorMessagePane.findViewById(R.id.error_image_view);

        LocalService service = mCallbacks.getService();
        if (service != null) {
            bindService(inflater.getContext(), service);
            if (service.areConversationsLoaded()) {
                setLoading(false);
            }
        }

        return inflatedView;
    }

    public void bindService(final Context ctx, final LocalService service) {
        mSmartListAdapter = new SmartListAdapter(ctx,
                service.get40dpContactCache(),
                service.getThreadPool());

        mSmartListAdapter.updateDataset(service.getConversations(), null);
        mSmartListAdapter.setCallback(this);
        if (mList != null) {
            mList.setAdapter(mSmartListAdapter);
        }
    }

    private void startConversation(CallContact c) {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }

        Intent intent = new Intent()
                .setClass(getActivity(), ConversationActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)));
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
    }

    private final OnItemClickListener conversationClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
            startConversation(((SmartListAdapter.ViewHolder) v.getTag()).conv.getContact());
        }
    };

    private final AdapterView.OnItemLongClickListener conversationLongClickListener =
            new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                    Conversation.presentActions(getActivity(),
                            ((SmartListAdapter.ViewHolder) v.getTag()).conv,
                            SmartListFragment.this);
                    return true;
                }
            };

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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;

                default:
                    break;
            }
        } else if (requestCode == REQUEST_CONF) {
            switch (resultCode) {
                case 0:
                    Conference call_to_add = data.getParcelableExtra("transfer");
                    Conference call_target = data.getParcelableExtra("target");

                    bindCalls(call_to_add, call_target);
                    break;

                default:
                    break;
            }
        } else {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null && resultCode == Activity.RESULT_OK) {
                String contact_uri = scanResult.getContents();
                startConversation(CallContact.buildUnknown(contact_uri));
            }
        }

    }

    private void bindCalls(Conference call_to_add, Conference call_target) {
        try {
            Log.i(TAG, "joining calls:" + call_to_add.getId() + " and " + call_target.getId());

            if (call_target.hasMultipleParticipants() && !call_to_add.hasMultipleParticipants()) {
                mCallbacks.getService()
                        .getRemoteService()
                        .addParticipant(
                                call_to_add.getParticipants().get(0).getCallId(),
                                call_target.getId()
                        );
            } else if (call_target.hasMultipleParticipants() && call_to_add.hasMultipleParticipants()) {
                // We join two conferences
                mCallbacks.getService()
                        .getRemoteService()
                        .joinConference(
                                call_to_add.getId(),
                                call_target.getId()
                        );
            } else if (!call_target.hasMultipleParticipants() && call_to_add.hasMultipleParticipants()) {
                mCallbacks.getService()
                        .getRemoteService()
                        .addParticipant(
                                call_target.getParticipants().get(0).getCallId(),
                                call_to_add.getId()
                        );
            } else {
                // We join two single calls to create a conf
                mCallbacks.getService()
                        .getRemoteService()
                        .joinParticipant(
                                call_to_add.getParticipants().get(0).getCallId(),
                                call_target.getParticipants().get(0).getCallId()
                        );
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setLoading(boolean loading) {
        if (null != this.mLoader) {
            int loaderVisibility = (loading) ? View.VISIBLE : View.GONE;
            this.mLoader.setVisibility(loaderVisibility);
            this.initEmptyTextViewWhileLoading(loading);
        }
    }

    private void initEmptyTextViewWhileLoading(boolean loading) {
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

    /**
     * Handles the visibility of some menus to hide / show the overflow menu
     *
     * @param menu    the menu containing the menuitems we need to access
     * @param visible true to display the overflow menu, false otherwise
     */
    private void setOverflowMenuVisible(final Menu menu, boolean visible) {
        if (null != menu) {
            MenuItem scanQrMenuItem = menu.findItem(R.id.menu_scan_qr);
            MenuItem clearHistoryMenuItem = menu.findItem(R.id.menu_clear_history);

            if (null != scanQrMenuItem) {
                scanQrMenuItem.setVisible(visible);
            }
            if (null != clearHistoryMenuItem) {
                clearHistoryMenuItem.setVisible(visible);
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

    //region SmartlistAdapterCallback

    @Override
    public void pictureTapped(Conversation conversation) {
        if (conversation == null) {
            Log.d(TAG, "pictureTapped, conversation is null");
            return;
        }
        if (conversation.getContact() != null) {
            Activity activity = getActivity();
            if (activity != null) {
                conversation.getContact().displayContact(getActivity());
            }
        }
    }

    //endregion


    @Override
    public void deleteConversation(Conversation conversation) {
        if (mCallbacks.getService() != null) {
            mCallbacks.getService().deleteConversation(conversation);
        }
    }

    @Override
    public void copyContactNumberToClipboard(String contactNumber) {
        ClipboardHelper.copyNumberToClipboard(getActivity(), contactNumber, this);
    }

    @Override
    public void clipBoardDidCopyNumber(String copiedNumber) {
        if (getView() != null) {
            String snackbarText = getString(R.string.conversation_action_copied_peer_number_clipboard,
                    CallContact.Phone.getShortenedNumber(copiedNumber));
            Snackbar.make(getView(), snackbarText, Snackbar.LENGTH_LONG).show();
        }
    }

    private void presentNetworkErrorPanel(@NonNull LocalService service) {
        if (service.isMobileNetworkConnectedButNotGranted()) {
            this.showErrorPanel(R.string.error_mobile_network_available_but_disabled,
                    true,
                    R.drawable.ic_settings_white_48dp,
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
        } else {
            this.showErrorPanel(R.string.error_no_network, false, 0, null);
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
            int visibility = (showImage) ? View.VISIBLE : View.GONE;
            mErrorImageView.setVisibility(visibility);
            mErrorImageView.setImageResource(imageResId);
        }
    }
}
