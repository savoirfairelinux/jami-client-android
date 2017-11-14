/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.adapters.ConversationAdapter;
import cx.ring.adapters.NumberAdapter;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.conversation.ConversationView;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.Uri;
import cx.ring.mvp.BaseFragment;
import cx.ring.services.NotificationService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ClipboardHelper;
import cx.ring.utils.Tuple;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;

public class ConversationFragment extends BaseFragment<ConversationPresenter> implements
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        ConversationView {

    public static final int REQ_ADD_CONTACT = 42;

    public static final String KEY_CONTACT_RING_ID = "CONTACT_RING_ID";
    public static final String KEY_ACCOUNT_ID = "ACCOUNT_ID";

    private static final String CONVERSATION_DELETE = "CONVERSATION_DELETE";
    private static final int MIN_SIZE_TABLET = 960;

    @BindView(R.id.msg_input_txt)
    protected EditText mMsgEditTxt;

    @BindView(R.id.ongoingcall_pane)
    protected ViewGroup mTopPane;

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
    public void refreshView(final Conversation conversation) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
    public int getLayout() {
        return R.layout.frag_conversation;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mTopPane != null) {
            mTopPane.setVisibility(View.GONE);
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
    }

    @Override
    public void displaySendTrustRequest(final String accountId, final String contactId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
        });
    }

    @OnClick(R.id.msg_send)
    public void sendMessageText() {
        presenter.sendTextMessage(mMsgEditTxt.getText().toString());
    }

    @OnEditorAction(R.id.msg_input_txt)
    public boolean actionSendMsgText(int actionId) {
        switch (actionId) {
            case EditorInfo.IME_ACTION_SEND:
                presenter.sendTextMessage(mMsgEditTxt.getText().toString());
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
        presenter.prepareMenu();
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
                presenter.callWithVideo(false, number);
                return true;
            case R.id.conv_action_videocall:
                number = mNumberAdapter == null ?
                        null : ((Phone) mNumberSpinner.getSelectedItem()).getNumber();
                presenter.callWithVideo(true, number);
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
            case R.id.menuitem_block:
                presenter.blockContact();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void deleteConversation(CallContact callContact) {
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
    protected void initPresenter(ConversationPresenter presenter) {
        super.initPresenter(presenter);
        String contactRingID = getArguments().getString(KEY_CONTACT_RING_ID);
        String accountId = getArguments().getString(KEY_ACCOUNT_ID);
        presenter.init(contactRingID, accountId);
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
    public void displayContactName(final CallContact contact) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                if (actionBar == null) {
                    return;
                }
                String displayName = contact.getDisplayName();
                actionBar.setTitle(displayName);
                String identity = contact.getRingUsername();
                if (identity != null && !identity.equals(displayName)) {
                    actionBar.setSubtitle(identity);
                }
            }
        });
    }

    @Override
    public void displayOnGoingCallPane(final boolean display) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTopPane.setVisibility(display ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void displayContactPhoto(final byte[] photo) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.setPhoto(photo);
            }
        });
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
                        conversation.getContact(),
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
                .putExtra(NotificationService.KEY_CALL_ID, conferenceId));
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