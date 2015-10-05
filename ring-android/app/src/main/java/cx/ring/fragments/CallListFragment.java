/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */
package cx.ring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.util.LruCache;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.client.NewConversationActivity;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.service.LocalService;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallListFragment extends Fragment {

    private static final String TAG = CallListFragment.class.getSimpleName();

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;
    //private TextView mConversationsTitleTextView;
    private CallListAdapter mConferenceAdapter;
    private FloatingActionButton newconv_btn = null;

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        // Bind to LocalService
        /*Intent intent = new Intent(getActivity(), LocalService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);*/

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);
        getActivity().registerReceiver(receiver, intentFilter);
        updateLists();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
        // Unbind from the service
        /*if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }*/
        getActivity().unregisterReceiver(receiver);
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            updateLists();
        }
    };
/*
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.w(TAG, "onServiceConnected " + className.getClassName());
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;
            mService = binder.getService();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);

            getActivity().registerReceiver(receiver, intentFilter);
            mBound = true;

            updateLists();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w(TAG, "onServiceDisconnected " + arg0.getClassName());
            getActivity().unregisterReceiver(receiver);
            mBound = false;
        }
    };
*/

    public static final int REQUEST_TRANSFER = 10;
    public static final int REQUEST_CONF = 20;

    /*
    @Override
    public void callStateChanged(Conference c, String callID, String State) {
        Log.i(TAG, "callStateChanged " + callID + "    " + State);
        updateLists();
    }

    @Override
    public void confCreated(Conference c, String id) {
        Log.i(TAG, "confCreated");
        updateLists();
    }

    @Override
    public void confRemoved(Conference c, String id) {
        Log.i(TAG, "confRemoved");
        updateLists();
    }

    @Override
    public void confChanged(Conference c, String id, String State) {
        Log.i(TAG, "confChanged");
        updateLists();
    }

    @Override
    public void recordingChanged(Conference c, String callID, String filename) {
        Log.i(TAG, "confChanged");
        updateLists();
    }
*/

    @Override
    public void onAttach(Activity activity) {
        Log.i(TAG, "onAttach");
        super.onAttach(activity);

        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
        if (mCallbacks.getService() != null) {
            /*mConvList = new ConversationList(getActivity(), mCallbacks.getService());
            if (mConferenceAdapter != null) {
                Log.i(TAG, "mConvList.addObserver");
                mConferenceAdapter.updateDataset(mConvList.getConversations());
                mConvList.addObserver(mConferenceAdapter);
            }*/
        }
    }

    /*
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            final long start = SystemClock.uptimeMillis();
            long millis = SystemClock.uptimeMillis() - start;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            mConferenceAdapter.notifyDataSetChanged();
            mHandler.postAtTime(this, start + (((minutes * 60) + seconds + 1) * 1000));
        }
    };*/

    //private Handler mHandler = new Handler();

    /*9
    @Override
    public void onResume() {
        super.onResume();
        if (mCallbacks.getService() != null) {
            if (mConvList != null)
                mConvList.startListener();

            updateLists();
        }

    }
*/
    public void updateLists() {
        if (mCallbacks.getService() != null)
            mConferenceAdapter.updateDataset(mCallbacks.getService().getConversations());
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View inflatedView = inflater.inflate(cx.ring.R.layout.frag_call_list, container, false);

        newconv_btn = (FloatingActionButton) inflatedView.findViewById(R.id.newconv_fab);
        newconv_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent().setClass(getActivity(), NewConversationActivity.class));
            }
        });

        //mConversationsTitleTextView = (TextView) inflatedView.findViewById(cx.ring.R.id.confs_counter);
/*
        if (mConferenceAdapter != null && mConvList != null)
            mConvList.deleteObserver(mConferenceAdapter);*/
        mConferenceAdapter = new CallListAdapter(getActivity());
        /*if (mConvList != null) {
            Log.i(TAG, "mConvList.addObserver");
            mConferenceAdapter.updateDataset(mConvList.getConversations());
            mConvList.addObserver(mConferenceAdapter);
        }*/
        /*if (mBound) {
            mConferenceAdapter.updateDataset(mService.getConversations());
        }*/
        LocalService service = mCallbacks.getService();
        if (service != null)
            mConferenceAdapter.updateDataset(mCallbacks.getService().getConversations());

        ListView list = (ListView) inflatedView.findViewById(cx.ring.R.id.confs_list);
        list.setAdapter(mConferenceAdapter);
        list.setOnItemClickListener(callClickListener);
        list.setOnItemLongClickListener(mItemLongClickListener);

        return inflatedView;
    }

    private final OnItemClickListener callClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
            Intent intent = new Intent()
                    .setClass(getActivity(), ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, ((CallListAdapter.ViewHolder) v.getTag()).conv.getContact().getIds().get(0)));
            intent.putExtra("resuming", true);
            //intent.putExtra("contact", ((Conversation) v.getTag()).getContact());
            //intent.putExtra("conversation", (Conversation) v.getTag());
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    };

    private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> adptv, View view, int pos, long arg3) {
            final Vibrator vibe = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(80);
            Intent i = new Intent();
            Bundle b = new Bundle();
            //b.putParcelable("conference", (Conference) adptv.getAdapter().getItem(pos));
            b.putParcelable("contact", ((Conversation) adptv.getAdapter().getItem(pos)).getContact());
            i.putExtra("bconference", b);

            DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            ClipData data = ClipData.newIntent("conference", i);
            view.startDrag(data, shadowBuilder, view, 0);
            return false;
        }

    };

    public class CallListAdapter extends BaseAdapter /*implements Observer*/ {
        final private ArrayList<Conversation> calls = new ArrayList<>();
        final private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
        final private LruCache<Long, Bitmap> mMemoryCache;
        final private HashMap<Long, WeakReference<ContactPictureTask>> running_tasks = new HashMap<>();

        private Context mContext;

        public CallListAdapter(Context act) {
            super();
            mContext = act;
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            final int cacheSize = maxMemory / 8;
            Log.i(TAG, "CallListAdapter created " + cacheSize);
            mMemoryCache = new LruCache<Long, Bitmap>(cacheSize){
                @Override
                protected int sizeOf(Long key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };
        }

        public void updateDataset(Collection<Conversation> list) {
            Log.i(TAG, "updateDataset " + list.size());
            if (list.size() == 0 && calls.size() == 0)
                return;
            calls.clear();
            calls.addAll(list);
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
            TextView conv_title;
            TextView conv_status;
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
                holder.conv_title = (TextView) convertView.findViewById(cx.ring.R.id.msg_txt);
                holder.conv_status = (TextView) convertView.findViewById(cx.ring.R.id.call_status);
                holder.position = -1;
                convertView.setTag(holder);
            }
            final ViewHolder h = holder;
            if (h.position == position && h.conv != null && h.conv == calls.get(position)) {
                return convertView;
            }
            h.conv = calls.get(position);
            h.position = position;
            h.conv_title.setText(h.conv.getContact().getDisplayName());
            h.conv_status.setText(DateFormat.getDateTimeInstance().format(h.conv.getLastInteraction()));

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
                        //if (fh.position == position) {
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
            convertView.setOnDragListener(dragListener);
            return convertView;
        }
    }

    OnDragListener dragListener = new OnDragListener() {

        @SuppressWarnings("deprecation")
        // deprecated in API 16....
        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Do nothing
                    // Log.w(TAG, "ACTION_DRAG_STARTED");
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    // Log.w(TAG, "ACTION_DRAG_ENTERED");
                    v.setBackgroundColor(Color.GREEN);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // Log.w(TAG, "ACTION_DRAG_EXITED");
                    v.setBackgroundDrawable(getResources().getDrawable(cx.ring.R.drawable.item_generic_selector));
                    break;
                case DragEvent.ACTION_DROP:
                    // Log.w(TAG, "ACTION_DROP");
                    View view = (View) event.getLocalState();

                    Item i = event.getClipData().getItemAt(0);
                    Intent intent = i.getIntent();
                    intent.setExtrasClassLoader(Conference.class.getClassLoader());

                    Conversation initial = ((CallListAdapter.ViewHolder) view.getTag()).conv;
                    Conversation target = ((CallListAdapter.ViewHolder) v.getTag()).conv;

                    if (initial == target) {
                        return true;
                    }

                    DropActionsChoice dialog = DropActionsChoice.newInstance();
                    Bundle b = new Bundle();
                    b.putParcelable("call_initial", initial.getCurrentCall());
                    b.putParcelable("call_targeted", target.getCurrentCall());
                    dialog.setArguments(b);
                    dialog.setTargetFragment(CallListFragment.this, 0);
                    dialog.show(getFragmentManager(), "dialog");

                    // view.setBackgroundColor(Color.WHITE);
                    // v.setBackgroundColor(Color.BLACK);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // Log.w(TAG, "ACTION_DRAG_ENDED");
                    View view1 = (View) event.getLocalState();
                    view1.setVisibility(View.VISIBLE);
                    v.setBackgroundDrawable(getResources().getDrawable(cx.ring.R.drawable.item_generic_selector));
                default:
                    break;
            }
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
        }
    }

    private void bindCalls(Conference call_to_add, Conference call_target) {
        try {

            Log.i(TAG, "joining calls:" + call_to_add.getId() + " and " + call_target.getId());

            if (call_target.hasMultipleParticipants() && !call_to_add.hasMultipleParticipants()) {

                mCallbacks.getService().getRemoteService().addParticipant(call_to_add.getParticipants().get(0), call_target.getId());

            } else if (call_target.hasMultipleParticipants() && call_to_add.hasMultipleParticipants()) {

                // We join two conferences
                mCallbacks.getService().getRemoteService().joinConference(call_to_add.getId(), call_target.getId());

            } else if (!call_target.hasMultipleParticipants() && call_to_add.hasMultipleParticipants()) {

                mCallbacks.getService().getRemoteService().addParticipant(call_target.getParticipants().get(0), call_to_add.getId());

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
