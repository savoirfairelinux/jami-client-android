/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Authors:    Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.adapters.ConversationAdapter;
import cx.ring.adapters.NumberAdapter;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.Uri;
import cx.ring.service.LocalService;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.ContentUriHandler;

public class ConversationActivity extends AppCompatActivity implements
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        ContactDetailsTask.DetailsLoadedCallback {

    @Inject
    AccountService mAccountService;

    @Inject
    ContactService mContactService;

    private static final String TAG = ConversationActivity.class.getSimpleName();
    private static final String CONVERSATION_DELETE = "CONVERSATION_DELETE";

    public static final int REQ_ADD_CONTACT = 42;
    static final long REFRESH_INTERVAL_MS = 30 * 1000;

    private boolean mBound = false;
    private boolean mVisible = false;
    private AlertDialog mDeleteDialog;
    private boolean mDeleteConversation = false;

    private LocalService mService = null;
    private Conversation mConversation = null;
    private Uri mPreferredNumber = null;

    private RecyclerView mHistList = null;
    private EditText mMsgEditTxt = null;
    private ViewGroup mBottomPane = null;
    private Spinner mNumberSpinner = null;
    private MenuItem mAddContactBtn = null;

    private ConversationAdapter mAdapter = null;
    private NumberAdapter mNumberAdapter = null;

    private final Handler mRefreshTaskHandler = new Handler();

    private Pair<Conversation, Uri> getConversation(LocalService s, Intent i) {
        if (s == null || i == null || i.getData() == null)
            return new Pair<>(null, null);

        String conv_id = i.getData().getLastPathSegment();
        Uri number = new Uri(i.getStringExtra("number"));

        Log.d(TAG, "getConversation " + conv_id + " " + number);
        Conversation conv = s.getConversation(conv_id);
        if (conv == null) {
            long contact_id = CallContact.contactIdFromId(conv_id);
            Log.d(TAG, "no conversation found, contact_id " + contact_id);
            CallContact contact = null;
            if (contact_id >= 0)
                contact = mContactService.findContactById(contact_id);
            if (contact == null) {
                Uri conv_uri = new Uri(conv_id);
                if (!number.isEmpty()) {
                    contact = mContactService.findContactByNumber(number.getRawUriString());
                    if (contact == null)
                        contact = CallContact.buildUnknown(conv_uri);
                } else {
                    contact = mContactService.findContactByNumber(conv_uri.getRawUriString());
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

        Log.d(TAG, "returning " + conv.getContact().getDisplayName() + " " + number);
        return new Pair<>(conv, number);
    }

    static private int getIndex(Spinner spinner, Uri myString) {
        for (int i = 0, n = spinner.getCount(); i < n; i++)
            if (((Phone) spinner.getItemAtPosition(i)).getNumber().equals(myString))
                return i;
        return 0;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        refreshView(0);
    }

    private void refreshView(long refreshed) {
        Pair<Conversation, Uri> conv = getConversation(mService, getIntent());
        mConversation = conv.first;
        mPreferredNumber = conv.second;

        if (mConversation == null) {
            finish();
            return;
        }

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            if (!mConversation.getContact().getPhones().isEmpty()) {
                CallContact contact = mContactService.getContact(mConversation.getContact().getPhones().get(0).getNumber());
                if (contact != null) {
                    mConversation.setContact(contact);
                }
            }

            ab.setTitle(mConversation.getContact().getDisplayName());
        }

        final CallContact contact = mConversation.getContact();
        if (contact != null) {
            new ContactDetailsTask(this, contact, this).run();
        }

        Conference conf = mConversation.getCurrentCall();
        mBottomPane.setVisibility(conf == null ? View.GONE : View.VISIBLE);
        if (conf != null) {
            Log.d(TAG, "ConversationActivity refreshView " + conf.getId() + " "
                    + mConversation.getCurrentCall());

            mBottomPane.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setClass(getApplicationContext(), CallActivity.class)
                            .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONFERENCE_CONTENT_URI,
                                    mConversation.getCurrentCall().getId())));
                }
            });
        }

        mAdapter.updateDataset(mConversation.getAggregateHistory(), refreshed);

        if (mConversation.getContact().getPhones().size() > 1) {
            mNumberSpinner.setVisibility(View.VISIBLE);
            mNumberAdapter = new NumberAdapter(ConversationActivity.this,
                    mConversation.getContact(),
                    false);
            mNumberSpinner.setAdapter(mNumberAdapter);
            if (mPreferredNumber == null || mPreferredNumber.isEmpty()) {
                mPreferredNumber = new Uri(
                        mConversation.getLastNumberUsed(mConversation.getLastAccountUsed())
                );
            }
            mNumberSpinner.setSelection(getIndex(mNumberSpinner, mPreferredNumber));
        } else {
            mNumberSpinner.setVisibility(View.GONE);
            mPreferredNumber = mConversation.getContact().getPhones().get(0).getNumber();
        }

        invalidateOptionsMenu();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "ConversationActivity onServiceConnected " + className.getClassName());
            mService = ((LocalService.LocalBinder) binder).getService();

            mAdapter = new ConversationAdapter(ConversationActivity.this,
                    mService.get40dpContactCache(),
                    mService.getThreadPool());

            if (mHistList != null) {
                mHistList.setAdapter(mAdapter);
            }

            refreshView(0);

            IntentFilter filter = new IntentFilter(LocalService.ACTION_CONF_UPDATE);
            registerReceiver(receiver, filter);

            mBound = true;
            if (mVisible && mConversation != null && !mConversation.isVisible()) {
                mConversation.setVisible(true);
                mService.readConversation(mConversation);
            }

            if (mDeleteConversation) {
                mDeleteDialog = ActionHelper.launchDeleteAction(ConversationActivity.this, mConversation, ConversationActivity.this);
            }

            mRefreshTaskHandler.postDelayed(refreshTask, REFRESH_INTERVAL_MS);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "ConversationActivity onServiceDisconnected " + arg0.getClassName());
            mBound = false;
            mRefreshTaskHandler.removeCallbacks(refreshTask);
            if (mConversation != null) {
                mConversation.setVisible(false);
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            refreshView(intent.getLongExtra(LocalService.ACTION_CONF_UPDATE_EXTRA_MSG, 0));
            if (mAdapter.getItemCount() > 0)
                mHistList.smoothScrollToPosition(mAdapter.getItemCount() - 1);
        }
    };

    private final Runnable refreshTask = new Runnable() {
        private long lastRefresh = 0;

        public void run() {
            if (lastRefresh == 0)
                lastRefresh = SystemClock.uptimeMillis();
            else
                lastRefresh += REFRESH_INTERVAL_MS;

            mAdapter.notifyDataSetChanged();

            mRefreshTaskHandler.postAtTime(this, lastRefresh + REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);

        mMsgEditTxt = (EditText) findViewById(R.id.msg_input_txt);
        mMsgEditTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEND:
                        CharSequence txt = mMsgEditTxt.getText();
                        if (txt.length() > 0) {
                            onSendTextMessage(mMsgEditTxt.getText().toString());
                            mMsgEditTxt.setText("");
                        }
                        return true;
                }
                return false;
            }
        });
        View msgSendBtn = findViewById(R.id.msg_send);
        if (msgSendBtn != null) {
            msgSendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CharSequence txt = mMsgEditTxt.getText();
                    if (txt.length() > 0) {
                        onSendTextMessage(txt.toString());
                        mMsgEditTxt.setText("");
                    }
                }
            });
        }

        mBottomPane = (ViewGroup) findViewById(R.id.ongoingcall_pane);
        if (mBottomPane != null) {
            mBottomPane.setVisibility(View.GONE);
        }

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);

        mHistList = (RecyclerView) findViewById(R.id.hist_list);
        if (mHistList != null) {
            mHistList.setLayoutManager(mLayoutManager);
            mHistList.setAdapter(mAdapter);
            mHistList.setItemAnimator(new DefaultItemAnimator());
        }

        mNumberSpinner = (Spinner) findViewById(R.id.number_selector);

        // reload delete conversation state (before rotation)
        mDeleteConversation = savedInstanceState != null && savedInstanceState.getBoolean(CONVERSATION_DELETE);

        if (!mBound) {
            Log.d(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mService = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mVisible = false;
        if (mConversation != null) {
            mService.readConversation(mConversation);
            mConversation.setVisible(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume " + mConversation);
        mVisible = true;
        if (mConversation != null) {
            mConversation.setVisible(true);
            if (mBound && mService != null) {
                mService.readConversation(mConversation);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_ADD_CONTACT:
                if (mService != null) mService.refreshConversations();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (mBound) {
            unregisterReceiver(receiver);
            unbindService(mConnection);
            mBound = false;
        }

        if (mDeleteConversation) {
            mDeleteDialog.dismiss();
        }

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // persist the delete popup state in case of Activity rotation
        mDeleteConversation = mDeleteDialog != null && mDeleteDialog.isShowing();
        outState.putBoolean(CONVERSATION_DELETE, mDeleteConversation);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mAddContactBtn != null) {
            mAddContactBtn.setVisible(mConversation != null && mConversation.getContact().getId() < 0);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.conversation_actions, menu);
        mAddContactBtn = menu.findItem(R.id.menuitem_addcontact);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            case R.id.conv_action_audiocall:
                onCallWithVideo(false);
                return true;
            case R.id.conv_action_videocall:
                onCallWithVideo(true);
                return true;
            case R.id.menuitem_addcontact:
                startActivityForResult(ActionHelper.getAddNumberIntentForContact(mConversation.getContact()), REQ_ADD_CONTACT);
                return true;
            case R.id.menuitem_delete:
                mDeleteDialog = ActionHelper.launchDeleteAction(this, this.mConversation, this);
                return true;
            case R.id.menuitem_copy_content:
                ActionHelper.launchCopyNumberToClipboardFromContact(this,
                        this.mConversation.getContact(), this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Guess account and number to use to initiate a call
     */
    private Pair<Account, Uri> guess() {
        Uri number = mNumberAdapter == null ?
                mPreferredNumber : ((Phone) mNumberSpinner.getSelectedItem()).getNumber();
        Account a = mAccountService.getAccount(mConversation.getLastAccountUsed());

        // Guess account from number
        if (a == null && number != null)
            a = mAccountService.guessAccount(number);

        // Guess number from account/call history
        if (a != null && (number == null/* || number.isEmpty()*/))
            number = new Uri(mConversation.getLastNumberUsed(a.getAccountID()));

        // If no account found, use first active
        if (a == null) {
            List<Account> accs = mAccountService.getAccounts();
            if (accs.isEmpty()) {
                finish();
                return null;
            } else
                a = accs.get(0);
        }

        // If no number found, use first from contact
        if (number == null || number.isEmpty())
            number = mConversation.getContact().getPhones().get(0).getNumber();

        return new Pair<>(a, number);
    }

    private void onSendTextMessage(String txt) {
        Conference conf = mConversation == null ? null : mConversation.getCurrentCall();
        if (conf == null || !conf.isOnGoing()) {
            Pair<Account, Uri> g = guess();
            if (g == null || g.first == null)
                return;
            mService.sendTextMessage(g.first.getAccountID(), g.second, txt);
        } else {
            mService.sendTextMessage(conf, txt);
        }
    }

    private void onCallWithVideo(boolean has_video) {
        Conference conf = mConversation.getCurrentCall();
        if (conf != null) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setClass(getApplicationContext(), CallActivity.class)
                    .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONFERENCE_CONTENT_URI, conf.getId())));
            return;
        }
        Pair<Account, Uri> g = guess();
        if (g == null || g.first == null)
            return;

        try {
            Intent intent = new Intent(CallActivity.ACTION_CALL)
                    .setClass(getApplicationContext(), CallActivity.class)
                    .putExtra("account", g.first.getAccountID())
                    .putExtra("video", has_video)
                    .setData(android.net.Uri.parse(g.second.getRawUriString()));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void deleteConversation(Conversation conversation) {
        if (mService != null) {
            mService.deleteConversation(conversation);
        }
        finish();
    }

    @Override
    public void copyContactNumberToClipboard(String contactNumber) {
        ClipboardHelper.copyNumberToClipboard(this, contactNumber, this);
    }

    @Override
    public void clipBoardDidCopyNumber(String copiedNumber) {
        View view = this.findViewById(android.R.id.content);
        if (view != null) {
            String snackbarText = getString(R.string.conversation_action_copied_peer_number_clipboard,
                    Phone.getShortenedNumber(copiedNumber));
            Snackbar.make(view, snackbarText, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDetailsLoaded(Bitmap bmp, String formattedName) {
        ActionBar ab = getSupportActionBar();
        if (ab != null && formattedName != null) {
            ab.setTitle(formattedName);
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
