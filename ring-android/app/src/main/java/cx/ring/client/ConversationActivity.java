package cx.ring.client;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.telecom.Call;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import cx.ring.R;
import cx.ring.loaders.AccountsLoader;
import cx.ring.loaders.LoaderConstants;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationList;
import cx.ring.model.SipCall;
import cx.ring.model.account.Account;
import cx.ring.service.ISipService;
import cx.ring.service.LocalService;
import cx.ring.service.SipService;

public class ConversationActivity extends Activity {
    private static final String TAG = ConversationActivity.class.getSimpleName();

    public static final Uri CONTENT_URI = Uri.withAppendedPath(LocalService.AUTHORITY_URI, "conversations");

    private boolean mBound = false;
    private LocalService service = null;
    //private ISipService service = null;

    private Conversation conversation = null;
    //private ConversationList mConvList = null;
    private ViewGroup bottomPane = null;

    //private List<Account> accounts = new ArrayList<>();

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((LocalService.LocalBinder)binder).getService();
            mBound = true;

            String conv_id = getIntent().getData().getLastPathSegment();
            conversation = service.getConversation(conv_id);
            Log.w(TAG, "ConversationActivity onServiceConnected " + conv_id);

            if (conversation == null) {
                finish();
                return;
            }
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
            /*if (mConvList == null) {
                mConvList = new ConversationList(ConversationActivity.this, service);
                mConvList.addObserver(new Observer() {
                    @Override
                    public void update(Observable observable, Object data) {
                        //observable.deleteObservers();
                        CallContact contact = getIntent().getParcelableExtra("contact");
                        if (contact != null) {
                            conversation = mConvList.getByContact(contact);
                            Log.w(TAG, "ConversationActivity update " + contact.getId() + " " + contact.getDisplayName());
                        } else {
                            Log.w(TAG, "ConversationActivity update NO CONTACT");
                        }
                        if (conversation == null)
                            finish();
                        Conference conf = conversation.getCurrentCall();
                        bottomPane.setVisibility(conf == null ? View.GONE : View.VISIBLE);
                        if (conf != null) {
                            Log.w(TAG, "ConversationActivity update " + conf.getId() + " " + conversation.getCurrentCall());
                            bottomPane.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent(ConversationActivity.this.getApplicationContext(), CallActivity.class).putExtra("conference", conversation.getCurrentCall()));
                                }
                            });
                        }
                    }
                });
                mConvList.startListener();
            }
            getLoaderManager().restartLoader(LoaderConstants.ACCOUNTS_LOADER, null, ConversationActivity.this);*/
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            //mConvList = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frag_conversation);
        bottomPane = (ViewGroup) findViewById(R.id.ongoingcall_pane);
        bottomPane.setVisibility(View.GONE);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        conversation = getIntent().getParcelableExtra("conversation");
        if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

/*
    @Override
    public void onStart() {
        super.onStart();
        if (service != null) {
            if (mConvList != null) {
                mConvList.addObserver(new Observer() {
                    @Override
                    public void update(Observable observable, Object data) {
                        observable.deleteObservers();
                        CallContact contact = getIntent().getParcelableExtra("contact");
                        if (contact != null)
                            conversation = mConvList.getByContact(contact);
                    }
                });
                mConvList.startListener();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mConvList.deleteObservers();
    }*/

    @Override
    protected void onDestroy() {
        /*if (mConvList != null) {
            mConvList.deleteObservers();
            mConvList.stopListener();;
            mConvList = null;
        }*/
        unbindService(mConnection);
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
            tmp = new Conference(Conference.DEFAULT_ID);

        tmp.getParticipants().add(infos);
        Intent intent = new Intent().setClass(this, CallActivity.class);
        intent.putExtra("conference", tmp);
        intent.putExtra("resuming", false);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);

        // overridePendingTransition(R.anim.slide_down, R.anim.slide_up);
    }

    private void onAudioCall() {
        if (service.getAccounts().isEmpty()) {
            //createNotRegisteredDialog().show();
            return;
        }

        Account usedAccount = service.getAccounts().get(0);
        CallContact contact = null;
        if (conversation != null) {
            Set<String> acc_ids = conversation.getAccountsUsed();
            if (!acc_ids.isEmpty())  {
                for (Account acc : service.getAccounts()) {
                    if (acc_ids.contains(acc.getAccountID())) {
                        usedAccount = acc;
                        break;
                    }
                }
            }
            contact = conversation.getContact();
        }
        /*if (contact == null) {

        }*/
        //conversation.getHistory().getAccountID()
        //if (usedAccount.isRegistered() || usedAccount.isIP2IP()) {
        //Account usedAccount = conversation.
        //Account usedAccount = new Account();
            Bundle args = new Bundle();
            //args.putString(SipCall.ID, Integer.toString(Math.abs(new Random().nextInt())));
            args.putParcelable(SipCall.ACCOUNT, usedAccount);
            args.putInt(SipCall.STATE, SipCall.state.CALL_STATE_NONE);
            args.putInt(SipCall.TYPE, SipCall.direction.CALL_TYPE_OUTGOING);
            args.putParcelable(SipCall.CONTACT, contact);

            try {
                launchCallActivity(new SipCall(args));
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        /*} else {
            createNotRegisteredDialog().show();
        }*/

    }
/*
    @Override
    public Loader<Bundle> onCreateLoader(int id, Bundle args) {
        AccountsLoader l = new AccountsLoader(this, service);
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<Bundle> loader, Bundle data) {
        accounts = data.getParcelableArrayList(AccountsLoader.ACCOUNTS);
        Account ip2ip = data.getParcelable(AccountsLoader.ACCOUNT_IP2IP);
        accounts.add(ip2ip);
    }

    @Override
    public void onLoaderReset(Loader<Bundle> loader) {

    }*/
}
