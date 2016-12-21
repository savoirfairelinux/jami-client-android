package cx.ring.fragments;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.adapters.ConversationAdapter;
import cx.ring.adapters.NumberAdapter;
import cx.ring.application.RingApplication;
import cx.ring.client.CallActivity;
import cx.ring.client.HomeActivity;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.Uri;
import cx.ring.service.LocalService;
import cx.ring.services.CallService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.ContentUriHandler;

public class ConversationFragment extends Fragment implements
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        ContactDetailsTask.DetailsLoadedCallback {

    @Inject
    CallService mCallService;

    @BindView(R.id.msg_input_txt)
    EditText mMsgEditTxt;

    @BindView(R.id.msg_send)
    View mMsgSendBtn;

    @BindView(R.id.ongoingcall_pane)
    ViewGroup mBottomPane;

    @BindView(R.id.hist_list)
    RecyclerView mHistList;

    @BindView(R.id.number_selector)
    Spinner mNumberSpinner;

    private static final String TAG = ConversationFragment.class.getSimpleName();
    private static final String CONVERSATION_DELETE = "CONVERSATION_DELETE";

    public static final int REQ_ADD_CONTACT = 42;

    private boolean mVisible = false;
    private AlertDialog mDeleteDialog;
    private boolean mDeleteConversation = false;

    private LocalService mService = null;
    private Conversation mConversation = null;
    private Uri mPreferredNumber = null;

    private MenuItem mAddContactBtn = null;

    private ConversationAdapter mAdapter = null;
    private NumberAdapter mNumberAdapter = null;

    static private Pair<Conversation, Uri> getConversation(LocalService service, Intent intent) {
        if (service == null || intent == null || intent.getData() == null) {
            return new Pair<>(null, null);
        }

        String conversationId = intent.getData().getLastPathSegment();
        Uri number = new Uri(intent.getStringExtra("number"));

        Log.d(TAG, "getConversation " + conversationId + " " + number);
        Conversation conversation = service.getConversation(conversationId);
        if (conversation == null) {
            long contactId = CallContact.contactIdFromId(conversationId);
            Log.d(TAG, "no conversation found, contact_id " + contactId);
            CallContact contact = null;
            if (contactId >= 0) {
                contact = service.findContactById(contactId);
            }
            if (contact == null) {
                Uri conv_uri = new Uri(conversationId);
                if (!number.isEmpty()) {
                    contact = service.findContactByNumber(number);
                    if (contact == null) {
                        contact = CallContact.buildUnknown(conv_uri);
                    }
                } else {
                    contact = service.findContactByNumber(conv_uri);
                    if (contact == null) {
                        contact = CallContact.buildUnknown(conv_uri);
                        number = contact.getPhones().get(0).getNumber();
                    } else {
                        number = conv_uri;
                    }
                }
            }
            conversation = service.startConversation(contact);
        }

        Log.d(TAG, "returning " + conversation.getContact().getDisplayName() + " " + number);
        return new Pair<>(conversation, number);
    }

    static private int getIndex(Spinner spinner, Uri myString) {
        for (int i = 0, n = spinner.getCount(); i < n; i++)
            if (((Phone) spinner.getItemAtPosition(i)).getNumber().equals(myString)) {
                return i;
            }
        return 0;
    }

    public void refreshView(long refreshed) {
        if (mService == null) {
            return;
        }
        Pair<Conversation, Uri> conversation = getConversation(mService, getActivity().getIntent());
        mConversation = conversation.first;
        mPreferredNumber = conversation.second;

        if (mConversation == null) {
            return;
        }

        if (!mConversation.getContact().getPhones().isEmpty()) {
            CallContact contact = mCallService.getContact(mConversation.getContact().getPhones().get(0).getNumber());
            if (contact != null) {
                mConversation.setContact(contact);
            }
            if (getActivity().getActionBar() != null) {
                getActivity().getActionBar().setTitle(mConversation.getContact().getDisplayName());
            }
        }

        final CallContact contact = mConversation.getContact();
        if (contact != null) {
            new ContactDetailsTask(getActivity(), contact, this).run();
        }

        Conference conference = mConversation.getCurrentCall();
        mBottomPane.setVisibility(conference == null ? View.GONE : View.VISIBLE);
        if (conference != null) {
            Log.d(TAG, "ConversationFragment refreshView " + conference.getId() + " "
                    + mConversation.getCurrentCall());
        }

        mAdapter.updateDataset(mConversation.getAggregateHistory(), refreshed);

        if (mConversation.getContact().getPhones().size() > 1) {
            mNumberSpinner.setVisibility(View.VISIBLE);
            mNumberAdapter = new NumberAdapter(getActivity(),
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

        if (mAdapter.getItemCount() > 0) {
            mHistList.smoothScrollToPosition(mAdapter.getItemCount() - 1);
        }

        getActivity().invalidateOptionsMenu();
    }

    public void setCallback(LocalService callback) {
        mService = callback;

        mAdapter = new ConversationAdapter(getActivity(),
                mService.get40dpContactCache(),
                mService.getThreadPool());

        if (mHistList != null) {
            mHistList.setAdapter(mAdapter);
        }

        if (mVisible && mConversation != null && !mConversation.isVisible()) {
            mConversation.setVisible(true);
            mService.readConversation(mConversation);
        }

        if (mDeleteConversation) {
            mDeleteDialog = ActionHelper.launchDeleteAction(getActivity(), mConversation, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View inflatedView = inflater.inflate(R.layout.frag_conversation, container, false);

        ButterKnife.bind(this, inflatedView);

        // Dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);


        if (mBottomPane != null) {
            mBottomPane.setVisibility(View.GONE);
        }

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setStackFromEnd(true);

        if (mHistList != null) {
            mHistList.setLayoutManager(mLayoutManager);
            mHistList.setAdapter(mAdapter);
            mHistList.setItemAnimator(new DefaultItemAnimator());
        }

        // reload delete conversation state (before rotation)
        mDeleteConversation = savedInstanceState != null && savedInstanceState.getBoolean(CONVERSATION_DELETE);

        setHasOptionsMenu(true);
        return inflatedView;
    }

    @OnClick(R.id.msg_send)
    public void sendMessageText(View sender) {
        CharSequence txt = mMsgEditTxt.getText();
        if (txt.length() > 0) {
            onSendTextMessage(txt.toString());
            mMsgEditTxt.setText("");
        }
    }

    @OnEditorAction(R.id.msg_input_txt)
    public boolean actionSendMsgText(TextView view, int actionId, KeyEvent event) {
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

    @OnClick(R.id.ongoingcall_pane)
    public void onClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW)
                .setClass(getActivity().getApplicationContext(), CallActivity.class)
                .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONFERENCE_CONTENT_URI,
                        mConversation.getCurrentCall().getId())));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mVisible = false;
        if (mConversation != null) {
            mService.readConversation(mConversation);
            mConversation.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume " + mConversation);
        mVisible = true;
        if (mConversation != null) {
            mConversation.setVisible(true);
            if (mService != null) {
                mService.readConversation(mConversation);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mDeleteConversation) {
            mDeleteDialog.dismiss();
        }

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // persist the delete popup state in case of Activity rotation
        mDeleteConversation = mDeleteDialog != null && mDeleteDialog.isShowing();
        outState.putBoolean(CONVERSATION_DELETE, mDeleteConversation);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mAddContactBtn != null) {
            mAddContactBtn.setVisible(mConversation != null && mConversation.getContact().getId() < 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conversation_actions, menu);
        mAddContactBtn = menu.findItem(R.id.menuitem_addcontact);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(getActivity(), HomeActivity.class));
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
                mDeleteDialog = ActionHelper.launchDeleteAction(getActivity(),
                        this.mConversation,
                        this);
                return true;
            case R.id.menuitem_copy_content:
                ActionHelper.launchCopyNumberToClipboardFromContact(getActivity(),
                        this.mConversation.getContact(),
                        this);
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
        Account account = mService.getAccount(mConversation.getLastAccountUsed());

        // Guess account from number
        if (account == null && number != null) {
            account = mService.guessAccount(number);
        }

        // Guess number from account/call history
        if (account != null && number == null) {
            number = new Uri(mConversation.getLastNumberUsed(account.getAccountID()));
        }

        // If no account found, use first active
        if (account == null) {
            List<Account> accounts = mService.getAccounts();
            if (accounts.isEmpty()) {
                return null;
            } else
                account = accounts.get(0);
        }

        // If no number found, use first from contact
        if (number == null || number.isEmpty()) {
            number = mConversation.getContact().getPhones().get(0).getNumber();
        }

        return new Pair<>(account, number);
    }

    private void onCallWithVideo(boolean has_video) {
        Conference conf = mConversation.getCurrentCall();
        if (conf != null) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setClass(getActivity().getApplicationContext(), CallActivity.class)
                    .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONFERENCE_CONTENT_URI, conf.getId())));
            return;
        }
        Pair<Account, Uri> guess = guess();
        if (guess == null || guess.first == null) {
            return;
        }

        try {
            Intent intent = new Intent(CallActivity.ACTION_CALL)
                    .setClass(getActivity().getApplicationContext(), CallActivity.class)
                    .putExtra("account", guess.first.getAccountID())
                    .putExtra("video", has_video)
                    .setData(android.net.Uri.parse(guess.second.getRawUriString()));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
        } catch (Exception e) {
            Log.e(TAG, "Error during call", e);
        }
    }

    private void onSendTextMessage(String txt) {
        Conference conference = mConversation == null ? null : mConversation.getCurrentCall();
        if (conference == null || !conference.isOnGoing()) {
            Pair<Account, Uri> guess = guess();
            if (guess == null || guess.first == null) {
                return;
            }
            mService.sendTextMessage(guess.first.getAccountID(), guess.second, txt);
        } else {
            mService.sendTextMessage(conference, txt);
        }
    }

    @Override
    public void deleteConversation(Conversation conversation) {
        if (mService != null) {
            mService.deleteConversation(conversation);
            getActivity().finish();
        }
    }

    @Override
    public void copyContactNumberToClipboard(String contactNumber) {
        ClipboardHelper.copyNumberToClipboard(getActivity(), contactNumber, this);
    }

    @Override
    public void clipBoardDidCopyNumber(String copiedNumber) {
        View view = getActivity().findViewById(android.R.id.content);
        if (view != null) {
            String snackbarText = getString(R.string.conversation_action_copied_peer_number_clipboard,
                    Phone.getShortenedNumber(copiedNumber));
            Snackbar.make(view, snackbarText, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDetailsLoaded(Bitmap bmp, String formattedName) {
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null && formattedName != null) {
            actionBar.setTitle(formattedName);
        }
    }
}