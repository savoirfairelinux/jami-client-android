/*
 *  Copyright (C) 2015 Savoir-faire Linux Inc.
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
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private boolean mBound = false;
    private LocalService service = null;
    private Conversation conversation = null;
    private String preferredNumber = null;

    private ListView histList = null;
    private View msgSendBtn = null;
    private EditText msgEditTxt = null;
    private ViewGroup bottomPane = null;
    private Spinner numberSpinner = null;

    private ConversationAdapter adapter = null;
    private NumberAdapter numberAdapter = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((LocalService.LocalBinder) binder).getService();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);
            registerReceiver(receiver, intentFilter);

            mBound = true;

            String conv_id = getIntent().getData().getLastPathSegment();
            preferredNumber = getIntent().getStringExtra("number");
            conversation = service.getConversation(conv_id);
            if (conversation == null) {
                long contact_id = CallContact.contactIdFromId(conv_id);
                CallContact contact = null;
                if (contact_id >= 0)
                    contact = service.findContactById(contact_id);
                if (contact == null) {
                    if (preferredNumber != null && !preferredNumber.isEmpty()) {
                        contact = service.findContactByNumber(preferredNumber);
                        if (contact == null)
                            contact = CallContact.buildUnknown(conv_id);
                    } else {
                        contact = service.findContactByNumber(conv_id);
                        if (contact == null)
                            contact = CallContact.buildUnknown(conv_id);
                        preferredNumber = conv_id;
                    }
                }
                conversation = service.startConversation(contact);
            }

            Log.w(TAG, "ConversationActivity onServiceConnected " + conv_id);

            if (conversation == null) {
                finish();
                return;
            }

            getSupportActionBar().setTitle(conversation.getContact().getDisplayName());

            Conference conf = conversation.getCurrentCall();
            bottomPane.setVisibility(conf == null ? View.GONE : View.VISIBLE);
            if (conf != null) {
                Log.w(TAG, "ConversationActivity onServiceConnected " + conf.getId() + " " + conversation.getCurrentCall());
                bottomPane.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(ConversationActivity.this.getApplicationContext(), CallActivity.class).putExtra("conference", conversation.getCurrentCall()));
                    }
                });
            }

            adapter.updateDataset(conversation.getHistory());

            if (conversation.getContact().getPhones().size() > 1) {
                numberAdapter = new NumberAdapter(ConversationActivity.this, conversation.getContact());
                numberSpinner.setAdapter(numberAdapter);
                if (preferredNumber == null || preferredNumber.isEmpty()) {
                    preferredNumber = CallContact.canonicalNumber(conversation.getLastNumberUsed(conversation.getLastAccountUsed()));
                }
                numberSpinner.setSelection(getIndex(numberSpinner, preferredNumber));
            } else {
                numberSpinner.setVisibility(View.GONE);
            }

            scrolltoBottom();
        }
        private int getIndex(Spinner spinner, String myString)
        {
            for (int i=0, n=spinner.getCount();i<n;i++)
                if (((CallContact.Phone)spinner.getItemAtPosition(i)).getNumber().equalsIgnoreCase(myString))
                    return i;
            return 0;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w(TAG, "ConversationActivity onServiceDisconnected " + arg0.getClassName());
            mBound = false;
        }
    };
    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            //conversation = service.getConversation(conversation.getId());
            Conversation newc = service.getByContact(conversation.getContact());
            if (newc != null)
                conversation = newc;
            adapter.updateDataset(conversation.getHistory());
            scrolltoBottom();
            Conference conf = conversation.getCurrentCall();
            bottomPane.setVisibility(conf == null ? View.GONE : View.VISIBLE);
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
                        onSendTextMessage(msgEditTxt.getText().toString());
                        msgEditTxt.setText("");
                        return true;
                }
                return false;
            }
        });
        msgSendBtn = findViewById(R.id.msg_send);
        msgSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSendTextMessage(msgEditTxt.getText().toString());
                msgEditTxt.setText("");
            }
        });
        bottomPane = (ViewGroup) findViewById(R.id.ongoingcall_pane);
        bottomPane.setVisibility(View.GONE);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        conversation = getIntent().getParcelableExtra("conversation");

        adapter = new ConversationAdapter(this);
        histList = (ListView) findViewById(R.id.hist_list);
        histList.setAdapter(adapter);

        numberSpinner = (Spinner) findViewById(R.id.number_selector);

        if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            service = null;
        }
    }

    private void scrolltoBottom() {
        histList.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                histList.setSelection(adapter.getCount() - 1);
            }
        });
    }

    private class NumberAdapter extends BaseAdapter {
        final private Context context;
        private ArrayList<CallContact.Phone> numbers;

        NumberAdapter(Context context, CallContact c) {
            this.context = context;
            numbers = c.getPhones();
        }

        public void updateDataset(CallContact c) {
            numbers = c.getPhones();
            notifyDataSetChanged();
        }

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
                convertView = LayoutInflater.from(context).inflate(R.layout.item_number_selected, null);

            CallContact.Phone number = numbers.get(position);

            ImageView numberIcon = (ImageView) convertView.findViewById(R.id.number_icon);
            numberIcon.setImageResource(new SipUri(number.getNumber()).isRingId() ? R.drawable.ring_logo_24dp : R.drawable.ic_dialer_sip_black_24dp);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.item_number, null);

            CallContact.Phone number = numbers.get(position);

            TextView numberTxt = (TextView) convertView.findViewById(R.id.number_txt);
            TextView numberLabelTxt = (TextView) convertView.findViewById(R.id.number_label_txt);
            ImageView numberIcon = (ImageView) convertView.findViewById(R.id.number_icon);

            numberTxt.setText(number.getNumber());
            numberLabelTxt.setText(number.getTypeString(context.getResources()));
            numberIcon.setImageResource(new SipUri(number.getNumber()).isRingId() ? R.drawable.ring_logo_24dp : R.drawable.ic_dialer_sip_black_24dp);

            return convertView;
        }
    }

    private class ConversationAdapter extends BaseAdapter {
        final private Context context;
        final private ArrayList<Conversation.ConversationElement> texts = new ArrayList<>();
        private ExecutorService infos_fetcher = Executors.newCachedThreadPool();

        public void updateDataset(ArrayList<Conversation.ConversationElement> list) {
            Log.i(TAG, "updateDataset " + list.size());
            if (list.size() == 0 && texts.size() == 0)
                return;
            texts.clear();
            texts.addAll(list);
            notifyDataSetChanged();
        }

        ConversationAdapter(Context ctx) {
            context = ctx;
        }

        @Override
        public int getCount() {
            return texts.size();
        }

        @Override
        public Conversation.ConversationElement getItem(int position) {
            return texts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.item_textmsg, null);

            ViewGroup txtEntry = (ViewGroup) convertView.findViewById(R.id.txt_entry);
            TextView msgTxt = (TextView) convertView.findViewById(R.id.msg_txt);
            TextView msgDetailTxt = (TextView) convertView.findViewById(R.id.msg_details_txt);
            ImageView photo = (ImageView) convertView.findViewById(R.id.photo);

            ViewGroup txtEntryRight = (ViewGroup) convertView.findViewById(R.id.txt_entry_right);
            TextView msgTxtRight = (TextView) convertView.findViewById(R.id.msg_txt_right);
            TextView msgDetailTxtRight = (TextView) convertView.findViewById(R.id.msg_details_txt_right);

            ViewGroup callEntry = (ViewGroup) convertView.findViewById(R.id.call_entry);
            TextView histTxt = (TextView) convertView.findViewById(R.id.call_hist_txt);
            TextView histDetailTxt = (TextView) convertView.findViewById(R.id.call_details_txt);

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

                callEntry.setVisibility(View.GONE);
                TextView message;
                TextView details;

                if (txt.text.isIncoming()) {
                    txtEntry.setVisibility(View.VISIBLE);
                    txtEntryRight.setVisibility(View.GONE);
                    message = msgTxt;
                    details = msgDetailTxt;
                    photo.setImageBitmap(null);
                    if (/*sep && */!sep_same)
                        infos_fetcher.execute(new ContactPictureTask(context, photo, txt.text.getContact()));
                } else {
                    txtEntry.setVisibility(View.GONE);
                    txtEntryRight.setVisibility(View.VISIBLE);
                    message = msgTxtRight;
                    details = msgDetailTxtRight;
                }

                message.setText(txt.text.getMessage());
                if (sep) {
                    details.setVisibility(View.VISIBLE);
                    details.setText(DateUtils.getRelativeTimeSpanString(txt.text.getTimestamp(), new Date().getTime(), 0, 0));
                } else {
                    details.setVisibility(View.GONE);
                }
            } else {
                callEntry.setVisibility(View.VISIBLE);
                txtEntry.setVisibility(View.GONE);
                txtEntryRight.setVisibility(View.GONE);
                msgTxt.setText("");
                histTxt.setText((txt.call.isIncoming() ? "Incoming" : "Outgoing") + " call with " + txt.call.getNumber());
                histDetailTxt.setText(DateFormat.getDateTimeInstance().format(txt.call.getStartDate()));
            }

            return convertView;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.conversation_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.conv_action_audiocall:
                onAudioCall();
                return true;
            case R.id.conv_action_videocall:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void launchCallActivity(SipCall infos) {
        Conference tmp = conversation.getCurrentCall();
        if (tmp == null)
            //tmp = service.startConversation(infos.getContact());
            tmp = new Conference(Conference.DEFAULT_ID);

        tmp.getParticipants().add(infos);
        Intent intent = new Intent().setClass(this, CallActivity.class);
        intent.putExtra("conference", tmp);
        intent.putExtra("resuming", false);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
        // overridePendingTransition(R.anim.slide_down, R.anim.slide_up);
    }

    private Pair<Account, String> guess() {
        Account a = service.getAccount(conversation.getLastAccountUsed());
        String number = numberAdapter == null ? conversation.contact.getPhones().get(0).getNumber() : CallContact.canonicalNumber(((CallContact.Phone) numberSpinner.getSelectedItem()).getNumber());
        if (a == null)
            a = service.guessAccount(conversation.getContact(), number);
        if (number == null || number.isEmpty())
            number = conversation.getLastNumberUsed(a.getAccountID());
        if (number == null || number.isEmpty())
            number = conversation.contact.getPhones().get(0).getNumber();
        return new Pair<>(a, number);
    }

    private void onSendTextMessage(String txt) {
        Conference conf = conversation == null ? null : conversation.getCurrentCall();
        if (conf == null || !conf.isOnGoing()) {
            Pair<Account, String> g = guess();
            try {
                service.getRemoteService().sendAccountTextMessage(g.first.getAccountID(), g.second, txt);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            try {
                service.getRemoteService().sendTextMessage(conf.getId(), new TextMessage(false, txt));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void onAudioCall() {
        Conference conf = conversation == null ? null : conversation.getCurrentCall();
        if (conf != null) {
            startActivity(new Intent(ConversationActivity.this.getApplicationContext(), CallActivity.class).putExtra("conference", conversation.getCurrentCall()));
            return;
        }
        CallContact contact = conversation.getContact();
        Pair<Account, String> g = guess();

        SipCall call = new SipCall(null, g.first.getAccountID(), g.second, SipCall.Direction.OUTGOING);
        call.setContact(contact);

        try {
            launchCallActivity(call);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }

    }
}
