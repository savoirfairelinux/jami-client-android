/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.client;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.model.SipUri;
import cx.ring.model.TextMessage;
import cx.ring.model.account.Account;
import cx.ring.service.LocalService;

public class ConversationActivity extends AppCompatActivity {
    private static final String TAG = ConversationActivity.class.getSimpleName();

    public static final Uri CONTENT_URI = Uri.withAppendedPath(LocalService.AUTHORITY_URI, "conversations");
    public static final int REQ_ADD_CONTACT = 42;

    private boolean mBound = false;
    private boolean mVisible = false;

    private LocalService service = null;
    private Conversation conversation = null;
    private SipUri preferredNumber = null;

    private RecyclerView histList = null;
    private View msgSendBtn = null;
    private EditText msgEditTxt = null;
    private ViewGroup bottomPane = null;
    private Spinner numberSpinner = null;
    private MenuItem addContactBtn = null;

    private ConversationAdapter adapter = null;
    private NumberAdapter numberAdapter = null;

    static private Pair<Conversation, SipUri> getConversation(LocalService s, Intent i) {
        if (s == null || i == null || i.getData() == null)
            return new Pair<>(null, null);
        String conv_id = i.getData().getLastPathSegment();
        SipUri number = new SipUri(i.getStringExtra("number"));
        Log.w(TAG, "getConversation " + conv_id + " " + number);
        Conversation conv = s.getConversation(conv_id);
        if (conv == null) {
            long contact_id = CallContact.contactIdFromId(conv_id);
            Log.w(TAG, "no conversation found, contact_id " + contact_id);
            CallContact contact = null;
            if (contact_id >= 0)
                contact = s.findContactById(contact_id);
            if (contact == null) {
                SipUri conv_uri = new SipUri(conv_id);
                if (!number.isEmpty()) {
                    contact = s.findContactByNumber(number);
                    if (contact == null)
                        contact = CallContact.buildUnknown(conv_uri);
                } else {
                    contact = s.findContactByNumber(conv_uri);
                    if (contact == null) {
                        contact = CallContact.buildUnknown(conv_uri);
                        number = contact.getPhones().get(0).getNumber();
                    } else {
                        number = conv_uri;
                    }
                }
            }
            conv = s.startConversation(contact);
        }
        Log.w(TAG, "returning " + conv.getContact().getDisplayName() + " " + number);
        return new Pair<>(conv, number);
    }

    static private int getIndex(Spinner spinner, SipUri myString) {
        for (int i=0, n=spinner.getCount();i<n;i++)
            if (((CallContact.Phone)spinner.getItemAtPosition(i)).getNumber().equals(myString))
                return i;
        return 0;
    }

    private void refreshView() {
        Pair<Conversation, SipUri> conv = getConversation(service, getIntent());
        conversation = conv.first;
        preferredNumber = conv.second;
        if (conversation == null) {
            finish();
            return;
        }
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setTitle(conversation.getContact().getDisplayName());
        Conference conf = conversation.getCurrentCall();
        bottomPane.setVisibility(conf == null ? View.GONE : View.VISIBLE);
        if (conf != null) {
            Log.w(TAG, "ConversationActivity onServiceConnected " + conf.getId() + " " + conversation.getCurrentCall());
            bottomPane.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setClass(getApplicationContext(), CallActivity.class)
                            .setData(Uri.withAppendedPath(Conference.CONTENT_URI, conversation.getCurrentCall().getId())));
                }
            });
        }

        adapter.updateDataset(conversation.getHistory());

        if (conversation.getContact().getPhones().size() > 1) {
            numberSpinner.setVisibility(View.VISIBLE);
            numberAdapter = new NumberAdapter(ConversationActivity.this, conversation.getContact());
            numberSpinner.setAdapter(numberAdapter);
            if (preferredNumber == null || preferredNumber.isEmpty()) {
                preferredNumber = new SipUri(conversation.getLastNumberUsed(conversation.getLastAccountUsed()));
            }
            numberSpinner.setSelection(getIndex(numberSpinner, preferredNumber));
            numberSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    msgEditTxt.setHint(getString(R.string.action_send_msg, ((CallContact.Phone)numberAdapter.getItem(position)).getNumber().getRawUriString()));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            numberSpinner.setVisibility(View.GONE);
            preferredNumber = conversation.getContact().getPhones().get(0).getNumber();
        }

        msgEditTxt.setHint(getString(R.string.action_send_msg, preferredNumber.getRawUriString()));

        invalidateOptionsMenu();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((LocalService.LocalBinder) binder).getService();
            registerReceiver(receiver, new IntentFilter(LocalService.ACTION_CONF_UPDATE));

            adapter = new ConversationAdapter(ConversationActivity.this, service.get40dpContactCache(), service.getThreadPool());
            if (histList != null)
                histList.setAdapter(adapter);

            refreshView();

            mBound = true;
            if (mVisible && conversation != null && !conversation.mVisible) {
                conversation.mVisible = true;
                service.readConversation(conversation);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w(TAG, "ConversationActivity onServiceDisconnected " + arg0.getClassName());
            mBound = false;
            if (conversation != null) {
                conversation.mVisible = false;
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            refreshView();
            if (adapter.getItemCount() > 0)
                histList.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frag_conversation);
        msgEditTxt = (EditText) findViewById(R.id.msg_input_txt);
        msgEditTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEND:
                        CharSequence txt = msgEditTxt.getText();
                        if (txt.length() > 0) {
                            onSendTextMessage(msgEditTxt.getText().toString());
                            msgEditTxt.setText("");
                        }
                        return true;
                }
                return false;
            }
        });
        msgSendBtn = findViewById(R.id.msg_send);
        msgSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence txt = msgEditTxt.getText();
                if (txt.length() > 0) {
                    onSendTextMessage(txt.toString());
                    msgEditTxt.setText("");
                }
            }
        });
        bottomPane = (ViewGroup) findViewById(R.id.ongoingcall_pane);
        bottomPane.setVisibility(View.GONE);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);

        histList = (RecyclerView) findViewById(R.id.hist_list);
        histList.setLayoutManager(mLayoutManager);
        histList.setAdapter(adapter);
        histList.setItemAnimator(new DefaultItemAnimator());

        numberSpinner = (Spinner) findViewById(R.id.number_selector);

        if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            service = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        mVisible = false;
        if (conversation != null) {
            service.readConversation(conversation);
            conversation.mVisible = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume " + conversation);
        mVisible = true;
        if (conversation != null) {
            conversation.mVisible = true;
            if (mBound && service != null) {
                service.readConversation(conversation);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_ADD_CONTACT:
                service.refreshConversations();
                break;
        }
    }

    private class NumberAdapter extends BaseAdapter {
        final private Context context;
        private ArrayList<CallContact.Phone> numbers;

        NumberAdapter(Context context, CallContact c) {
            this.context = context;
            numbers = c.getPhones();
        }

        /*public void updateDataset(CallContact c) {
            numbers = c.getPhones();
            notifyDataSetChanged();
        }*/

        @Override
        public int getCount() {
            return numbers.size();
        }

        @Override
        public Object getItem(int position) {
            return numbers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.item_number_selected, parent, false);

            CallContact.Phone number = numbers.get(position);

            ImageView numberIcon = (ImageView) convertView.findViewById(R.id.number_icon);
            numberIcon.setImageResource(number.getNumber().isRingId() ? R.drawable.ring_logo_24dp : R.drawable.ic_dialer_sip_black_24dp);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.item_number, parent, false);

            CallContact.Phone number = numbers.get(position);

            TextView numberTxt = (TextView) convertView.findViewById(R.id.number_txt);
            TextView numberLabelTxt = (TextView) convertView.findViewById(R.id.number_label_txt);
            ImageView numberIcon = (ImageView) convertView.findViewById(R.id.number_icon);

            numberTxt.setText(number.getNumber().getRawUriString());
            numberLabelTxt.setText(number.getTypeString(context.getResources()));
            numberIcon.setImageResource(number.getNumber().isRingId() ? R.drawable.ring_logo_24dp : R.drawable.ic_dialer_sip_black_24dp);

            return convertView;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewGroup txtEntry;
        public TextView msgTxt;
        public TextView msgDetailTxt;
        public ImageView photo;
        public ViewGroup callEntry;
        public TextView histTxt;
        public TextView histDetailTxt;
        public long cid = -1;

        public ViewHolder(ViewGroup v, int type) {
            super(v);
            if (type == 2) {
                callEntry = (ViewGroup) v.findViewById(R.id.call_entry);
                histTxt = (TextView) v.findViewById(R.id.call_hist_txt);
                histDetailTxt = (TextView) v.findViewById(R.id.call_details_txt);
                photo = (ImageView) v.findViewById(R.id.call_icon);
            } else {
                txtEntry = (ViewGroup) v.findViewById(R.id.txt_entry);
                msgTxt = (TextView) v.findViewById(R.id.msg_txt);
                msgDetailTxt = (TextView) v.findViewById(R.id.msg_details_txt);
                if (type == 0)
                    photo = (ImageView) v.findViewById(R.id.photo);
            }
        }
    }

    private class ConversationAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final Context context;
        private final ArrayList<Conversation.ConversationElement> texts = new ArrayList<>();
        private final LruCache<Long, Bitmap> memory_cache;
        private final ExecutorService infos_fetcher;
        private final HashMap<Long, WeakReference<ContactPictureTask>> running_tasks = new HashMap<>();

        ConversationAdapter(Context ctx, LruCache<Long, Bitmap> cache, ExecutorService pool) {
            context = ctx;
            memory_cache = cache;
            infos_fetcher = pool;
        }

        public void updateDataset(final ArrayList<Conversation.ConversationElement> list) {
            Log.i(TAG, "updateDataset " + list.size());
            if (list.size() == texts.size())
                return;
            int lastPos = texts.size();
            int newItmes = list.size() - lastPos;
            if (lastPos == 0 || newItmes < 0) {
                texts.clear();
                texts.addAll(list);
                notifyDataSetChanged();
            } else {
                for (int i = lastPos; i < list.size(); i++)
                    texts.add(list.get(i));
                notifyItemRangeInserted(lastPos, newItmes);
            }
        }

        @Override
        public int getItemCount() {
            return texts.size();
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            Conversation.ConversationElement txt = texts.get(position);
            if (txt.text != null) {
                if (txt.text.isIncoming())
                    return 0;
                else
                    return 1;
            }
            return 2;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int res = viewType == 0 ? R.layout.item_conv_msg_peer : (viewType == 1 ? R.layout.item_conv_msg_me : R.layout.item_conv_call);
            ViewGroup v = (ViewGroup)LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
            // set the view's size, margins, paddings and layout parameters
            ViewHolder vh = new ViewHolder(v, viewType);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder h, int position) {
            Conversation.ConversationElement txt = texts.get(position);

            if (txt.text != null) {
                boolean sep = false;
                boolean sep_same = false;
                if (position > 0 && texts.get(position - 1).text != null) {
                    TextMessage msg = texts.get(position - 1).text;
                    if (msg.isIncoming() && txt.text.isIncoming() && msg.getNumber().equals(txt.text.getNumber()))
                        sep_same = true;
                }
                if (position > 0 && texts.get(position - 1).text != null && position < texts.size() - 1) {
                    TextMessage msg = texts.get(position + 1).text;
                    if (msg != null) {
                        long diff = msg.getTimestamp() - txt.text.getTimestamp();
                        if (diff > 30 * 1000)
                            sep = true;
                    } else {
                        sep = true;
                    }
                }

                h.cid = txt.text.getContact().getId();
                if (h.photo != null)
                    h.photo.setImageBitmap(null);
                if (txt.text.isIncoming() && !sep_same) {
                    final Long cid = txt.text.getContact().getId();
                    Bitmap bmp = memory_cache.get(cid);
                    if (bmp != null)
                        h.photo.setImageBitmap(bmp);
                    else {
                        h.photo.setImageBitmap(memory_cache.get(-1l));
                        final WeakReference<ViewHolder> wh = new WeakReference<>(h);
                        final ContactPictureTask.PictureLoadedCallback cb = new ContactPictureTask.PictureLoadedCallback() {
                            @Override
                            public void onPictureLoaded(final Bitmap bmp) {
                                final ViewHolder fh = wh.get();
                                if (fh == null || fh.photo.getParent() == null)
                                    return;
                                if (fh.cid == cid) {
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
                            task = new ContactPictureTask(context, h.photo, txt.text.getContact(), new ContactPictureTask.PictureLoadedCallback() {
                                @Override
                                public void onPictureLoaded(Bitmap bmp) {
                                    memory_cache.put(cid, bmp);
                                    running_tasks.remove(cid);
                                }
                            });
                            task.addCallback(cb);
                            running_tasks.put(cid, new WeakReference<>(task));
                            infos_fetcher.execute(task);
                        }
                    }
                }
                h.msgTxt.setText(txt.text.getMessage());
                if (sep) {
                    h.msgDetailTxt.setVisibility(View.VISIBLE);
                    h.msgDetailTxt.setText(DateUtils.getRelativeTimeSpanString(txt.text.getTimestamp(), new Date().getTime(), 0, 0));
                } else {
                    h.msgDetailTxt.setVisibility(View.GONE);
                }
            } else {
                h.cid = txt.call.getContactID();
                if (txt.call.isMissed()) {
                    h.photo.setImageResource(txt.call.isIncoming() ? R.drawable.ic_call_missed_black_24dp : R.drawable.ic_call_missed_outgoing_black_24dp);
                    h.histTxt.setText(txt.call.isIncoming() ? getString(R.string.notif_missed_incoming_call, txt.call.getNumber())
                            : getString(R.string.notif_missed_outgoing_call, txt.call.getNumber()));
                } else {
                    h.photo.setImageResource(txt.call.isIncoming() ? R.drawable.ic_call_received_black_24dp : R.drawable.ic_call_made_black_24dp);
                    h.histTxt.setText(txt.call.isIncoming() ? getString(R.string.notif_incoming_call_title, txt.call.getNumber())
                            : getString(R.string.notif_outgoing_call_title, txt.call.getNumber()));
                }
                h.histDetailTxt.setText(DateFormat.getDateTimeInstance().format(txt.call.getStartDate()));
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mBound) {
            unregisterReceiver(receiver);
            unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (addContactBtn != null)
            addContactBtn.setVisible(conversation != null && conversation.getContact().getId() < 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.conversation_actions, menu);
        addContactBtn = menu.findItem(R.id.menuitem_addcontact);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.conv_action_audiocall:
                onAudioCall();
                return true;
            /*case R.id.conv_action_videocall:
                return true;*/
            case R.id.menuitem_addcontact:
                startActivityForResult(conversation.contact.getAddNumberIntent(), REQ_ADD_CONTACT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Guess account and number to use to initiate a call
     */
    private Pair<Account, SipUri> guess() {
        SipUri number = numberAdapter == null ? preferredNumber : ((CallContact.Phone) numberSpinner.getSelectedItem()).getNumber();
        Account a = service.getAccount(conversation.getLastAccountUsed());

        // Guess account from number
        if (a == null && number != null)
            a = service.guessAccount(conversation.getContact(), number);

        // Guess number from account/call history
        if (a != null && (number == null/* || number.isEmpty()*/))
            number = new SipUri(conversation.getLastNumberUsed(a.getAccountID()));

        // If no account found, use first active
        if (a == null)
            a = service.getAccounts().get(0);

        // If no number found, use first from contact
        if (number == null || number.isEmpty())
            number = conversation.contact.getPhones().get(0).getNumber();

        return new Pair<>(a, number);
    }

    private void onSendTextMessage(String txt) {
        Conference conf = conversation == null ? null : conversation.getCurrentCall();
        if (conf == null || !conf.isOnGoing()) {
            Pair<Account, SipUri> g = guess();
            service.sendTextMessage(g.first.getAccountID(), g.second, txt);
        } else {
            service.sendTextMessage(conf, txt);
        }
    }

    private void onAudioCall() {
        Conference conf = conversation.getCurrentCall();
        if (conf != null) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setClass(getApplicationContext(), CallActivity.class)
                    .setData(Uri.withAppendedPath(Conference.CONTENT_URI, conf.getId())));
            return;
        }
        CallContact contact = conversation.getContact();
        Pair<Account, SipUri> g = guess();

        SipCall call = new SipCall(null, g.first.getAccountID(), g.second, SipCall.Direction.OUTGOING);
        call.setContact(contact);

        try {
            Intent intent = new Intent(CallActivity.ACTION_CALL)
                    .setClass(getApplicationContext(), CallActivity.class)
                    .putExtra("account", g.first.getAccountID())
                    .setData(Uri.parse(g.second.getRawUriString()));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }

    }
}
