package cx.ring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import cx.ring.model.DaemonEvent;
import cx.ring.model.Phone;
import cx.ring.model.Uri;
import cx.ring.service.LocalService;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class ConversationFragment extends Fragment implements
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        ContactDetailsTask.DetailsLoadedCallback,
        Observer<DaemonEvent> {

    @Inject
    CallService mCallService;

    @Inject
    AccountService mAccountService;

    @BindView(R.id.msg_input_txt)
    EditText mMsgEditTxt;

    @BindView(R.id.ongoingcall_pane)
    ViewGroup mBottomPane;

    @BindView(R.id.hist_list)
    RecyclerView mHistList;

    @BindView(R.id.number_selector)
    Spinner mNumberSpinner;

    private static final String TAG = ConversationFragment.class.getSimpleName();
    private static final String CONVERSATION_DELETE = "CONVERSATION_DELETE";

    public static final int REQ_ADD_CONTACT = 42;

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;

    private boolean mVisible = false;
    private AlertDialog mDeleteDialog;
    private boolean mDeleteConversation = false;

    private Conversation mConversation = null;
    private Uri mPreferredNumber = null;

    private MenuItem mAddContactBtn = null;

    private ConversationAdapter mAdapter = null;
    private NumberAdapter mNumberAdapter = null;

    private static Pair<Conversation, Uri> getConversation(LocalService service, Bundle bundle) {
        if (service == null || bundle == null) {
            return new Pair<>(null, null);
        }

        String conversationId = bundle.getString("conversationID");
        Uri number = new Uri(bundle.getString("number"));

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
                Uri convUri = new Uri(conversationId);
                if (!number.isEmpty()) {
                    contact = service.findContactByNumber(number);
                    if (contact == null) {
                        contact = CallContact.buildUnknown(convUri);
                    }
                } else {
                    contact = service.findContactByNumber(convUri);
                    if (contact == null) {
                        contact = CallContact.buildUnknown(convUri);
                        number = contact.getPhones().get(0).getNumber();
                    } else {
                        number = convUri;
                    }
                }
            }
            conversation = service.startConversation(contact);
        }

        Log.d(TAG, "returning " + conversation.getContact().getDisplayName() + " " + number);
        return new Pair<>(conversation, number);
    }

    private static int getIndex(Spinner spinner, Uri myString) {
        for (int i = 0, n = spinner.getCount(); i < n; i++)
            if (((Phone) spinner.getItemAtPosition(i)).getNumber().equals(myString)) {
                return i;
            }
        return 0;
    }

    public void refreshView(long refreshed) {
        if (mCallbacks == null || mCallbacks.getService() == null) {
            return;
        }
        Pair<Conversation, Uri> conversation = getConversation(mCallbacks.getService(), getArguments());
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
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mConversation.getContact().getDisplayName());
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
            for (Phone phone : mConversation.getContact().getPhones()) {
                if (phone.getNumber() != null && phone.getNumber().isRingId()) {
                    mAccountService.lookupAddress("", "", phone.getNumber().getRawUriString());
                }
            }

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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            refreshView(intent.getLongExtra(LocalService.ACTION_CONF_UPDATE_EXTRA_MSG, 0));
        }
    };

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach");
        super.onAttach(activity);

        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
        mCallbacks = LocalService.DUMMY_CALLBACKS;
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

        LocalService service = mCallbacks.getService();
        if (service != null) {
            bindService(inflater.getContext(), service);
            refreshView(0);
        }

        return inflatedView;
    }

    public void bindService(final Context ctx, final LocalService service) {
        mAdapter = new ConversationAdapter(getActivity(),
                service.get40dpContactCache(),
                service.getThreadPool());

        if (mHistList != null) {
            mHistList.setAdapter(mAdapter);
        }

        if (mVisible && mConversation != null && !mConversation.isVisible()) {
            mConversation.setVisible(true);
            service.readConversation(mConversation);
        }

        if (mDeleteConversation) {
            mDeleteDialog = ActionHelper.launchDeleteAction(getActivity(), mConversation, this);
        }
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
        if (mConversation != null && mCallbacks.getService() != null) {
            mCallbacks.getService().readConversation(mConversation);
            mConversation.setVisible(false);
        }

        getActivity().unregisterReceiver(receiver);
        mAccountService.removeObserver(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume " + mConversation);
        mVisible = true;
        if (mConversation != null) {
            mConversation.setVisible(true);
            if (mCallbacks.getService() != null) {
                mCallbacks.getService().readConversation(mConversation);
            }
        }

        IntentFilter filter = new IntentFilter(LocalService.ACTION_CONF_UPDATE);
        getActivity().registerReceiver(receiver, filter);
        mAccountService.addObserver(this);
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
        Account account = mAccountService.getAccount(mConversation.getLastAccountUsed());

        // Guess account from number
        if (account == null && number != null) {
            account = mAccountService.guessAccount(number);
        }

        // Guess number from account/call history
        if (account != null && number == null) {
            number = new Uri(mConversation.getLastNumberUsed(account.getAccountID()));
        }

        // If no account found, use first active
        if (account == null) {
            List<Account> accounts = mAccountService.getAccounts();
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
            mCallbacks.getService().sendTextMessage(guess.first.getAccountID(), guess.second, txt);
        } else {
            mCallbacks.getService().sendTextMessage(conference, txt);
        }
    }

    @Override
    public void deleteConversation(Conversation conversation) {
        if (mCallbacks.getService() != null) {
            mCallbacks.getService().deleteConversation(conversation);
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
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null && formattedName != null) {
            actionBar.setTitle(formattedName);
        }
    }

    @Override
    public void update(Observable observable, DaemonEvent arg) {
        if (observable instanceof AccountService && arg != null) {
            if (arg.getEventType() == DaemonEvent.EventType.REGISTERED_NAME_FOUND) {
                final String name = arg.getEventInput(DaemonEvent.EventInput.NAME, String.class);
                final String address = arg.getEventInput(DaemonEvent.EventInput.ADDRESS, String.class);
                final int state = arg.getEventInput(DaemonEvent.EventInput.STATE, Integer.class);

                if (state != 0 || mNumberAdapter == null || mNumberAdapter.isEmpty()) {
                    return;
                }

                for (int i = 0; i < mNumberAdapter.getCount(); i++) {
                    Phone phone = (Phone) mNumberAdapter.getItem(i);
                    if (phone.getNumber() != null) {
                        String ringID = phone.getNumber().getRawUriString();
                        if (address.equals(ringID)) {
                            phone.getNumber().setUsername(name);
                            mNumberAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        }
    }
}