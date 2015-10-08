package cx.ring.client;

import android.app.Activity;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cx.ring.R;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.account.Account;
import cx.ring.service.LocalService;

public class ConversationActivity extends Activity {
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

    private ConversationAdapter adapter = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((LocalService.LocalBinder)binder).getService();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);
            registerReceiver(receiver, intentFilter);

            mBound = true;

            String conv_id = getIntent().getData().getLastPathSegment();
            preferredNumber = getIntent().getStringExtra("number");
            conversation = service.getConversation(conv_id);
            if (conversation == null) {
                long contact_id = CallContact.contactIdFromId(conv_id);
                CallContact contact;
                if (contact_id >= 0)
                    contact = service.findContactById(contact_id);
                else if (preferredNumber != null && !preferredNumber.isEmpty()) {
                    contact = service.findContactByNumber(preferredNumber);
                    if (contact == null)
                        contact = CallContact.ContactBuilder.buildUnknownContact(conv_id);
                } else {
                    contact = service.findContactByNumber(conv_id);
                    if (contact == null)
                        contact = CallContact.ContactBuilder.buildUnknownContact(conv_id);
                    preferredNumber = conv_id;
                }
                conversation = service.startConversation(contact);
            }

            Log.w(TAG, "ConversationActivity onServiceConnected " + conv_id);

            if (conversation == null) {
                finish();
                return;
            }

            getActionBar().setTitle(conversation.getContact().getDisplayName());

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
            scrolltoBottom();
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
            conversation = service.getByContact(conversation.getContact());
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
                callEntry.setVisibility(View.GONE);
                if (txt.text.isIncoming()) {
                    txtEntry.setVisibility(View.VISIBLE);
                    txtEntryRight.setVisibility(View.GONE);
                    msgTxt.setText(txt.text.getMessage());
                    msgDetailTxt.setText(DateFormat.getDateTimeInstance().format(new Date(txt.text.getTimestamp())));
                    infos_fetcher.execute(new ContactPictureTask(context, photo, txt.text.getContact()));
                } else {
                    txtEntry.setVisibility(View.GONE);
                    txtEntryRight.setVisibility(View.VISIBLE);
                    msgTxtRight.setText(txt.text.getMessage());
                    msgDetailTxtRight.setText(DateFormat.getDateTimeInstance().format(new Date(txt.text.getTimestamp())));
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

    private void onSendTextMessage(String txt) {

        Conference conf = conversation.getCurrentCall();
        if (conf == null || !conf.isOnGoing()) {
            String account = conversation.getLastAccountUsed();
            if (account == null || account.isEmpty())
                account = service.guessAccount(conversation.getContact(), conversation.contact.getPhones().get(0).getNumber()).getAccountID();
            String number = preferredNumber;
            if (number == null || number.isEmpty())
                number = conversation.getLastNumberUsed(account);
            if (number == null || number.isEmpty())
                number = conversation.contact.getPhones().get(0).getNumber();
            try {
                service.getRemoteService().sendAccountTextMessage(account, number, txt);
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
        Conference conf = conversation.getCurrentCall();
        if (conf != null) {
            startActivity(new Intent(ConversationActivity.this.getApplicationContext(), CallActivity.class).putExtra("conference", conversation.getCurrentCall()));
            return;
        }

        if (service.getAccounts().isEmpty()) {
            //createNotRegisteredDialog().show();
            return;
        }

        Account usedAccount = service.getAccounts().get(0);
        CallContact contact = null;
        if (conversation != null) {
            String last_used = conversation.getLastAccountUsed();
            Account a = service.getAccount(last_used);
            if (a != null/* && a.isEnabled()*/)
                usedAccount = a;
            else {
                Set<String> acc_ids = conversation.getAccountsUsed();
                for (Account acc : service.getAccounts()) {
                    if (acc_ids.contains(acc.getAccountID())) {
                        usedAccount = acc;
                        break;
                    }
                }
            }
            contact = conversation.getContact();
        }   

        String number = preferredNumber;
        if (number == null)
            number = conversation.getLastNumberUsed(usedAccount.getAccountID());
        if (number == null && contact != null)
            number = contact.getPhones().get(0).getNumber();

        //conversation.getHistory().getAccountID()
        //if (usedAccount.isRegistered() || usedAccount.isIP2IP()) {
         /*   Bundle args = new Bundle();
            args.putParcelable(SipCall.ACCOUNT, usedAccount);
            args.putInt(SipCall.STATE, SipCall.State.NONE);
            args.putInt(SipCall.TYPE, SipCall.Direction.OUTGOING);
            args.putParcelable(SipCall.CONTACT, contact);*/
        SipCall call = new SipCall(null, usedAccount.getAccountID(), number, SipCall.Direction.OUTGOING);
        call.setContact(contact);

            try {
                launchCallActivity(call);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        /*} else {
            createNotRegisteredDialog().show();
        }*/

    }
}
