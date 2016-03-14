/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.adapters.ContactsAdapter;
import cx.ring.adapters.StarredContactsAdapter;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.loaders.ContactsLoader;
import cx.ring.loaders.LoaderConstants;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.service.LocalService;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

public class CallListFragment extends Fragment implements SearchView.OnQueryTextListener, LoaderManager.LoaderCallbacks<ContactsLoader.Result> {

    private static final String TAG = CallListFragment.class.getSimpleName();

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;
    private CallListAdapter mConferenceAdapter;
    private ContactsAdapter mListAdapter;
    private StarredContactsAdapter mGridAdapter;

    private FloatingActionButton newconv_btn = null;

    private SearchView searchView = null;
    private MenuItem searchMenuItem = null;
    private MenuItem dialpadMenuItem = null;

    private ListView list = null;
    private StickyListHeadersListView contactList = null;

    private LinearLayout llMain;
    private GridView mStarredGrid;
    private TextView favHeadLabel;
    private LinearLayout mHeader;
    private ViewGroup newcontact;
    private ViewGroup error_msg_pane;
    private TextView error_msg_txt;

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);
        intentFilter.addAction(LocalService.ACTION_ACCOUNT_UPDATE);
        getActivity().registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
        getActivity().unregisterReceiver(receiver);
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            updateLists();
        }
    };

    public static final int REQUEST_TRANSFER = 10;
    public static final int REQUEST_CONF = 20;

    @Override
    public void onAttach(Activity activity) {
        Log.i(TAG, "onAttach");
        super.onAttach(activity);

        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
    }

    public void updateLists() {
        LocalService service = mCallbacks.getService();
        if (service != null && mConferenceAdapter != null) {
            mConferenceAdapter.updateDataset(service.getConversations());
            if (service.isConnected()) {
                error_msg_pane.setVisibility(View.GONE);
            } else {
                error_msg_pane.setVisibility(View.VISIBLE);
                error_msg_txt.setText(R.string.error_no_network);
            }
        }
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "onDetach");
        super.onDetach();
        mCallbacks = LocalService.DUMMY_CALLBACKS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        //mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity)getActivity()).setToolbarState(false, R.string.app_name);
        updateLists();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mStarredGrid.setAdapter(mGridAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.call_list_menu, menu);
        searchMenuItem = menu.findItem(R.id.menu_contact_search);
        dialpadMenuItem = menu.findItem(R.id.menu_contact_dial);
        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                dialpadMenuItem.setVisible(false);
                list.setAdapter(mConferenceAdapter);
                //listSwitcher.setDisplayedChild(0);
                list.setVisibility(View.VISIBLE);
                contactList.setAdapter(null);
                contactList.setVisibility(View.GONE);
                newconv_btn.setVisibility(View.VISIBLE);
                return true;
            }
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                dialpadMenuItem.setVisible(true);
                contactList.setAdapter(mListAdapter);
                //listSwitcher.setDisplayedChild(1);
                contactList.setVisibility(View.VISIBLE);
                list.setAdapter(null);
                list.setVisibility(View.GONE);
                newconv_btn.setVisibility(View.GONE);
                onLoadFinished(null, mCallbacks.getService().getSortedContacts());
                return true;
            }
        });

        searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.searchbar_hint));
        searchView.setLayoutParams(new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT));
        searchView.setImeOptions(EditorInfo.IME_ACTION_GO);

        Intent i = getActivity().getIntent();
        switch (i.getAction()) {
            case Intent.ACTION_VIEW:
            case Intent.ACTION_CALL:
                searchView.setQuery(i.getDataString(), true);
                break;
            case Intent.ACTION_DIAL:
                searchMenuItem.expandActionView();
                searchView.setQuery(i.getDataString(), false);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_contact_search:
                searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                return false;
            case R.id.menu_contact_dial:
                if (searchView.getInputType() == EditorInfo.TYPE_CLASS_PHONE)
                    searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                else
                    searchView.setInputType(EditorInfo.TYPE_CLASS_PHONE);
                return true;
            case R.id.menu_clear_history:
                mCallbacks.getService().clearHistory();
                return true;
            case R.id.menu_scan_qr:
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.initiateScan();
            default:
                return false;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        newcontact.callOnClick();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query.isEmpty()) {
            getLoaderManager().destroyLoader(LoaderConstants.CONTACT_LOADER);
            onLoadFinished(null, mCallbacks.getService().getSortedContacts());
            newcontact.setVisibility(View.GONE);
            return true;
        }
        Bundle b = new Bundle();
        b.putString("filter", query);
        getLoaderManager().restartLoader(LoaderConstants.CONTACT_LOADER, b, this);
        newcontact.setVisibility(View.VISIBLE);
        ((TextView)newcontact.findViewById(R.id.display_name)).setText(/*getString(R.string.contact_call, query)*/query);
        CallContact contact = CallContact.buildUnknown(query);
        newcontact.setTag(contact);
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        setHasOptionsMenu(true);
        View inflatedView = inflater.inflate(cx.ring.R.layout.frag_call_list, container, false);

        newconv_btn = (FloatingActionButton) inflatedView.findViewById(R.id.newconv_fab);
        newconv_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchMenuItem.expandActionView();
            }
        });


        list = (ListView) inflatedView.findViewById(cx.ring.R.id.confs_list);
        list.setOnItemClickListener(callClickListener);
        //list.setOnItemLongClickListener(mItemLongClickListener);

        mHeader = (LinearLayout) inflater.inflate(R.layout.frag_contact_list_header, null);
        contactList = (StickyListHeadersListView) inflatedView.findViewById(R.id.contacts_stickylv);
        contactList.setDivider(null);
        contactList.addHeaderView(mHeader, null, false);
        contactList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final CallContact item = (CallContact) parent.getItemAtPosition(position);
                ((HomeActivity)getActivity()).onTextContact(item);
            }
        });

        mStarredGrid = (GridView) mHeader.findViewById(R.id.favorites_grid);
        llMain = (LinearLayout) mHeader.findViewById(R.id.llMain);
        favHeadLabel = (TextView) mHeader.findViewById(R.id.fav_head_label);
        newcontact = (ViewGroup) mHeader.findViewById(R.id.newcontact_element);
        newcontact.setVisibility(View.GONE);
        newcontact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallContact c = (CallContact) v.getTag();
                if (c == null)
                    return;
                startConversation(c);
            }
        });
        newcontact.findViewById(R.id.quick_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallContact c = (CallContact) newcontact.getTag();
                if (c != null)
                    ((HomeActivity)getActivity()).onCallContact(c);
            }
        });

        error_msg_pane = (ViewGroup) inflatedView.findViewById(R.id.error_msg_pane);
        error_msg_txt = (TextView) error_msg_pane.findViewById(R.id.error_msg_txt);

        list.setVisibility(View.VISIBLE);
        contactList.setVisibility(View.GONE);

        LocalService service = mCallbacks.getService();
        if (service == null)
            return inflatedView;

        mConferenceAdapter = new CallListAdapter(getActivity(), service.get40dpContactCache(), service.getThreadPool());
        mListAdapter = new ContactsAdapter(getActivity(), (HomeActivity)getActivity(), service.get40dpContactCache(), service.getThreadPool());
        mGridAdapter = new StarredContactsAdapter(getActivity());

        mConferenceAdapter.updateDataset(service.getConversations());
        list.setAdapter(mConferenceAdapter);

        return inflatedView;
    }

    private void startConversation(CallContact c) {
        Intent intent = new Intent()
                .setClass(getActivity(), ConversationActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)));
        intent.putExtra("resuming", true);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
    }

    private final OnItemClickListener callClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
            startConversation(((CallListAdapter.ViewHolder) v.getTag()).conv.getContact());
        }
    };

    private void setGridViewListeners() {
        mStarredGrid.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int pos, long arg3) {
                startConversation(mGridAdapter.getItem(pos));
            }
        });
    }

    @Override
    public Loader<ContactsLoader.Result> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "createLoader " + (args == null ? "" : args.getString("filter")));

        Uri baseUri = null;
        if (args != null)
            baseUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(args.getString("filter")));
        LocalService service = mCallbacks.getService();
        if (service == null)
            return null;
        ContactsLoader l = new ContactsLoader(getActivity(), baseUri, service.getContactCache());
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<ContactsLoader.Result> loader, ContactsLoader.Result data) {
        Log.i(TAG, "onLoadFinished with " + data.contacts.size() + " contacts, " + data.starred.size() + " starred.");

        mListAdapter.setData(data.contacts);
        //setListViewListeners();

        mGridAdapter.setData(data.starred);
        if (data.starred.isEmpty()) {
            llMain.setVisibility(View.GONE);
            favHeadLabel.setVisibility(View.GONE);
        } else {
            llMain.setVisibility(View.VISIBLE);
            favHeadLabel.setVisibility(View.VISIBLE);
            setGridViewListeners();
            mStarredGrid.post(new Runnable() {
                @Override
                public void run() {
                    setGridViewHeight(mStarredGrid, llMain);
                }
            });
        }
    }

    public void setGridViewHeight(GridView gridView, LinearLayout llMain) {
        ListAdapter listAdapter = gridView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        int firstHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(gridView.getWidth(), View.MeasureSpec.AT_MOST);

        int rows = (listAdapter.getCount() + gridView.getNumColumns() - 1) / gridView.getNumColumns();

        for (int i = 0; i < rows; i++) {
            if (i == 0) {
                View listItem = listAdapter.getView(i, null, gridView);
                listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                firstHeight = listItem.getMeasuredHeight();
            }
            totalHeight += firstHeight;
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llMain.getLayoutParams();

        params.height = (int) (totalHeight + (getResources().getDimension(R.dimen.contact_vertical_spacing) * (rows - 1) + llMain.getPaddingBottom() + llMain.getPaddingTop()));
        llMain.setLayoutParams(params);
        mHeader.requestLayout();
    }

    @Override
    public void onLoaderReset(Loader<ContactsLoader.Result> loader) {

    }

    public class CallListAdapter extends BaseAdapter {
        final private ArrayList<Conversation> calls = new ArrayList<>();
        final private ExecutorService infos_fetcher;
        final private LruCache<Long, Bitmap> mMemoryCache;
        final private HashMap<Long, WeakReference<ContactPictureTask>> running_tasks = new HashMap<>();

        private Context mContext;

        public CallListAdapter(Context act, LruCache<Long, Bitmap> cache, ExecutorService pool) {
            super();
            mContext = act;
            mMemoryCache = cache;
            infos_fetcher = pool;
        }

        public void updateDataset(final Collection<Conversation> list) {
            Log.i(TAG, "updateDataset " + list.size());
            if (list.size() == 0 && calls.size() == 0)
                return;
            calls.clear();
            for (Conversation c : list) {
                if (!c.getContact().isUnknown() || !c.getAccountsUsed().isEmpty() || c.getCurrentCall() != null)
                    calls.add(c);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return calls.size();
        }

        @Override
        public Conversation getItem(int position) {
            return calls.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        private class ViewHolder {
            TextView conv_participants;
            TextView conv_status;
            TextView conv_time;
            ImageView photo;
            int position;
            Conversation conv;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(mContext).inflate(cx.ring.R.layout.item_calllist, null);

            ViewHolder holder = (ViewHolder) convertView.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                holder.photo = (ImageView) convertView.findViewById(R.id.photo);
                holder.conv_participants = (TextView) convertView.findViewById(R.id.conv_participant);
                holder.conv_status = (TextView) convertView.findViewById(R.id.conv_last_item);
                holder.conv_time = (TextView) convertView.findViewById(R.id.conv_last_time);
                holder.position = -1;
                convertView.setTag(holder);
            }
            final ViewHolder h = holder;
            h.conv = calls.get(position);
            h.position = position;
            h.conv_participants.setText(h.conv.getContact().getDisplayName());
            long last_interaction = h.conv.getLastInteraction().getTime();
            h.conv_time.setText(last_interaction == 0 ? "" : DateUtils.getRelativeTimeSpanString(last_interaction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
            h.conv_status.setText(h.conv.getLastInteractionSumary(getResources()));
            if (h.conv.hasUnreadTextMessages()) {
                h.conv_participants.setTypeface(null, Typeface.BOLD);
                h.conv_time.setTypeface(null, Typeface.BOLD);
                h.conv_status.setTypeface(null, Typeface.BOLD);
            } else {
                h.conv_participants.setTypeface(null, Typeface.NORMAL);
                h.conv_time.setTypeface(null, Typeface.NORMAL);
                h.conv_status.setTypeface(null, Typeface.NORMAL);
            }

            final Long cid = h.conv.getContact().getId();
            Bitmap bmp = mMemoryCache.get(cid);
            if (bmp != null) {
                h.photo.setImageBitmap(bmp);
            } else {
                holder.photo.setImageBitmap(mMemoryCache.get(-1l));
                final WeakReference<ViewHolder> wh = new WeakReference<>(holder);
                final ContactPictureTask.PictureLoadedCallback cb = new ContactPictureTask.PictureLoadedCallback() {
                    @Override
                    public void onPictureLoaded(final Bitmap bmp) {
                        final ViewHolder fh = wh.get();
                        if (fh == null || fh.photo.getParent() == null)
                            return;
                        if (fh.conv.getContact().getId() == cid) {
                            fh.photo.post(new Runnable() {
                                @Override
                                public void run() {
                                    fh.photo.setImageBitmap(bmp);
                                    fh.photo.startAnimation(AnimationUtils.loadAnimation(fh.photo.getContext(), R.anim.contact_fadein));
                                }
                            });
                        }
                    }
                };
                WeakReference<ContactPictureTask> wtask = running_tasks.get(cid);
                ContactPictureTask task = wtask == null ? null : wtask.get();
                if (task != null) {
                    task.addCallback(cb);
                } else {
                    task = new ContactPictureTask(mContext, h.photo, h.conv.getContact(), new ContactPictureTask.PictureLoadedCallback() {
                        @Override
                        public void onPictureLoaded(Bitmap bmp) {
                            mMemoryCache.put(cid, bmp);
                            running_tasks.remove(cid);
                        }
                    });
                    task.addCallback(cb);
                    running_tasks.put(cid, new WeakReference<>(task));
                    infos_fetcher.execute(task);
                }
            }
            return convertView;
        }
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
                        mConferenceAdapter.notifyDataSetChanged();
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
                onQueryTextChange(contact_uri);
                onQueryTextSubmit(contact_uri);
            }
        }

    }

    private void bindCalls(Conference call_to_add, Conference call_target) {
        try {

            Log.i(TAG, "joining calls:" + call_to_add.getId() + " and " + call_target.getId());

            if (call_target.hasMultipleParticipants() && !call_to_add.hasMultipleParticipants()) {

                mCallbacks.getService().getRemoteService().addParticipant(call_to_add.getParticipants().get(0).getCallId(), call_target.getId());

            } else if (call_target.hasMultipleParticipants() && call_to_add.hasMultipleParticipants()) {

                // We join two conferences
                mCallbacks.getService().getRemoteService().joinConference(call_to_add.getId(), call_target.getId());

            } else if (!call_target.hasMultipleParticipants() && call_to_add.hasMultipleParticipants()) {

                mCallbacks.getService().getRemoteService().addParticipant(call_target.getParticipants().get(0).getCallId(), call_to_add.getId());

            } else {
                // We join two single calls to create a conf
                mCallbacks.getService().getRemoteService().joinParticipant(call_to_add.getParticipants().get(0).getCallId(),
                        call_target.getParticipants().get(0).getCallId());
            }

        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
