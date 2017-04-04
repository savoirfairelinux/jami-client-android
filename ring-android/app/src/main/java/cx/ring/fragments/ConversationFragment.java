package cx.ring.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.ByteArrayOutputStream;

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
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.conversation.ConversationView;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.Uri;
import cx.ring.mvp.BaseFragment;
import cx.ring.service.LocalService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.Tuple;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;

public class ConversationFragment extends BaseFragment<ConversationPresenter> implements
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        ContactDetailsTask.DetailsLoadedCallback,
        ConversationView {

    private static final String TAG = ConversationFragment.class.getSimpleName();
    private static final String CONVERSATION_DELETE = "CONVERSATION_DELETE";
    private static final int MIN_SIZE_TABLET = 960;

    public static final int REQ_ADD_CONTACT = 42;

    @Inject
    protected ConversationPresenter conversationPresenter;

    @BindView(R.id.msg_input_txt)
    protected EditText mMsgEditTxt;

    @BindView(R.id.ongoingcall_pane)
    protected ViewGroup mBottomPane;

    @BindView(R.id.hist_list)
    protected RecyclerView mHistList;

    @BindView(R.id.number_selector)
    protected Spinner mNumberSpinner;

    private AlertDialog mDeleteDialog;
    private boolean mDeleteConversation = false;

    private MenuItem mAddContactBtn = null;

    private ConversationAdapter mAdapter = null;
    private NumberAdapter mNumberAdapter = null;

    public static Boolean isTabletMode(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && context.getResources().getConfiguration().screenWidthDp >= MIN_SIZE_TABLET;
    }

    static private int getIndex(Spinner spinner, Uri myString) {
        for (int i = 0, n = spinner.getCount(); i < n; i++)
            if (((Phone) spinner.getItemAtPosition(i)).getNumber().equals(myString)) {
                return i;
            }
        return 0;
    }

    @Override
    public void refreshView(final Conversation conversation, Uri number) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CallContact contact = conversation.getContact();
                if (contact != null) {
                    new ContactDetailsTask(getActivity(), contact, ConversationFragment.this).run();
                }

                if (mAdapter != null) {
                    mAdapter.updateDataset(conversation.getAggregateHistory(), 0);

                    if (mAdapter.getItemCount() > 0) {
                        mHistList.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                    }
                }

                getActivity().invalidateOptionsMenu();
            }
        });
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

        mAdapter = new ConversationAdapter();

        if (mHistList != null) {
            mHistList.setAdapter(mAdapter);
        }

        if (mDeleteConversation) {
            presenter.deleteAction();
        }

        return inflatedView;
    }

    @Override
    public void displaySendTrustRequest(final String accountId, final String contactId) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.send_request_title);
        builder.setMessage(R.string.send_request_msg);

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton(R.string.send_request_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                VCard vcard = VCardUtils.loadLocalProfileFromDisk(getActivity().getFilesDir(), accountId);
                if (vcard != null && !vcard.getPhotos().isEmpty()) {
                    // Reduce photo size to fit in one DHT packet
                    Bitmap photo = BitmapUtils.bytesToBitmap(vcard.getPhotos().get(0).getData());
                    photo = BitmapUtils.reduceBitmap(photo, 30000);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    vcard.removeProperties(Photo.class);
                    vcard.addPhoto(new Photo(stream.toByteArray(), ImageType.PNG));
                    vcard.removeProperties(RawProperty.class);
                }
                presenter.sendTrustRequest(accountId, contactId, vcard);
            }
        });

        builder.show();
    }

    @OnClick(R.id.msg_send)
    public void sendMessageText() {
        Uri number = mNumberAdapter == null ?
                null : ((Phone) mNumberSpinner.getSelectedItem()).getNumber();
        presenter.sendTextMessage(mMsgEditTxt.getText().toString(), number);
    }

    @OnEditorAction(R.id.msg_input_txt)
    public boolean actionSendMsgText(int actionId) {
        switch (actionId) {
            case EditorInfo.IME_ACTION_SEND:
                Uri number = mNumberAdapter == null ?
                        null : ((Phone) mNumberSpinner.getSelectedItem()).getNumber();
                presenter.sendTextMessage(mMsgEditTxt.getText().toString(), number);
                return true;
        }
        return false;
    }

    @OnClick(R.id.ongoingcall_pane)
    public void onClick() {
        presenter.clickOnGoingPane();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.resume();
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
        conversationPresenter.prepareMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conversation_actions, menu);
        mAddContactBtn = menu.findItem(R.id.menuitem_addcontact);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Uri number;
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(getActivity(), HomeActivity.class));
                return true;
            case R.id.conv_action_audiocall:
                number = mNumberAdapter == null ?
                        null : ((Phone) mNumberSpinner.getSelectedItem()).getNumber();
                conversationPresenter.callWithVideo(false, number);
                return true;
            case R.id.conv_action_videocall:
                number = mNumberAdapter == null ?
                        null : ((Phone) mNumberSpinner.getSelectedItem()).getNumber();
                conversationPresenter.callWithVideo(true, number);
                return true;
            case R.id.menuitem_addcontact:
                presenter.addContact();
                return true;
            case R.id.menuitem_delete:
                presenter.deleteAction();
                return true;
            case R.id.menuitem_copy_content:
                presenter.copyToClipboard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void deleteConversation(Conversation conversation) {
        presenter.deleteConversation();
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
                    ActionHelper.getShortenedNumber(copiedNumber));
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
    protected ConversationPresenter createPresenter() {
        return conversationPresenter;
    }

    @Override
    public void showViewModel(Object viewModel) {

    }

    @Override
    protected void initPresenter(ConversationPresenter presenter) {
        super.initPresenter(presenter);
        String conversationId = getArguments().getString("conversationID");
        Uri number = new Uri(getArguments().getString("number"));
        conversationPresenter.init(conversationId, number);
    }

    @Override
    public void updateView(final String address, final String name, final int state) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    @Override
    public void displayContactName(final String contactName) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(contactName);
                }
            }
        });
    }

    @Override
    public void displayOnGoingCallPane(final boolean display) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBottomPane.setVisibility(display ? View.GONE : View.VISIBLE);
            }
        });
    }

    @Override
    public void displayContactPhoto(byte[] photo) {
        mAdapter.setPhoto(photo);
    }

    @Override
    public void displayNumberSpinner(final Conversation conversation, final Uri number) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNumberSpinner.setVisibility(View.VISIBLE);
                mNumberAdapter = new NumberAdapter(getActivity(),
                        conversation.getContact(),
                        false);
                mNumberSpinner.setAdapter(mNumberAdapter);
                mNumberSpinner.setSelection(getIndex(mNumberSpinner, number));
            }
        });
    }

    @Override
    public void displayAddContact(final boolean display) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAddContactBtn != null) {
                    mAddContactBtn.setVisible(display);
                }
            }
        });
    }

    @Override
    public void displayDeleteDialog(final Conversation conversation) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeleteDialog = ActionHelper.launchDeleteAction(getActivity(),
                        conversation,
                        ConversationFragment.this);
            }
        });
    }

    @Override
    public void displayCopyToClipboard(CallContact callContact) {
        ActionHelper.launchCopyNumberToClipboardFromContact(getActivity(),
                callContact,
                this);
    }

    @Override
    public void hideNumberSpinner() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNumberSpinner.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void clearMsgEdit() {
        mMsgEditTxt.setText("");
    }

    @Override
    public void goToHome() {
        if (getActivity() instanceof ConversationActivity) {
            getActivity().finish();
        }
    }

    @Override
    public void goToAddContact(CallContact callContact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(callContact), REQ_ADD_CONTACT);
    }

    @Override
    public void goToCallActivity(String conferenceId) {
        startActivity(new Intent(Intent.ACTION_VIEW)
                .setClass(getActivity().getApplicationContext(), CallActivity.class)
                .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONFERENCE_CONTENT_URI, conferenceId)));
    }

    @Override
    public void goToCallActivityWithResult(Tuple<Account, Uri> guess, boolean hasVideo) {
        Intent intent = new Intent(CallActivity.ACTION_CALL)
                .setClass(getActivity().getApplicationContext(), CallActivity.class)
                .putExtra("account", guess.first.getAccountID())
                .putExtra("video", hasVideo)
                .setData(android.net.Uri.parse(guess.second.getRawUriString()));
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
    }
}