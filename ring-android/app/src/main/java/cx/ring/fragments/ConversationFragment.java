/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.fragments;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.OnEditorAction;
import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.adapters.ConversationAdapter;
import cx.ring.adapters.NumberAdapter;
import cx.ring.application.RingApplication;
import cx.ring.client.CallActivity;
import cx.ring.client.ContactDetailsActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.conversation.ConversationView;
import cx.ring.databinding.FragConversationBinding;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.interfaces.Colorable;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.ConversationElement;
import cx.ring.model.DataTransfer;
import cx.ring.model.Phone;
import cx.ring.model.RingError;
import cx.ring.model.Uri;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.services.NotificationService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.MediaButtonsHelper;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static android.app.Activity.RESULT_OK;

public class ConversationFragment extends BaseSupportFragment<ConversationPresenter> implements
        MediaButtonsHelper.MediaButtonsHelperCallback,
        ConversationView, SharedPreferences.OnSharedPreferenceChangeListener {
    protected static final String TAG = ConversationFragment.class.getSimpleName();

    public static final int REQ_ADD_CONTACT = 42;

    public static final String KEY_CONTACT_RING_ID = BuildConfig.APPLICATION_ID + ".CONTACT_RING_ID";
    public static final String KEY_ACCOUNT_ID = BuildConfig.APPLICATION_ID + ".ACCOUNT_ID";
    public static final String KEY_PREFERENCE_PENDING_MESSAGE = "pendingMessage";
    public static final String KEY_PREFERENCE_CONVERSATION_COLOR = "color";

    private static final int REQUEST_CODE_FILE_PICKER = 1000;
    private static final int REQUEST_PERMISSION_CAMERA = 1001;
    private static final int REQUEST_CODE_TAKE_PICTURE = 1002;

    private FragConversationBinding binding;
    private MenuItem mAudioCallBtn = null;
    private MenuItem mVideoCallBtn = null;

    private ConversationAdapter mAdapter = null;
    private NumberAdapter mNumberAdapter = null;
    private int marginPx;
    private final ValueAnimator animation = new ValueAnimator();

    private SharedPreferences mPreferences;

    private File mCurrentPhoto = null;
    private Disposable actionbarTarget = null;
    private static int position;


    private String mCurrentFileAbsolutePath = null;

    public static final int SAVE_FILE_REQUEST_CODE = 1003;

    private static int getIndex(Spinner spinner, Uri myString) {
        for (int i = 0, n = spinner.getCount(); i < n; i++)
            if (((Phone) spinner.getItemAtPosition(i)).getNumber().equals(myString)) {
                return i;
            }
        return 0;
    }

    @Override
    public void refreshView(final List<ConversationElement> conversation) {
        if (conversation == null) {
            return;
        }
        if (binding != null)
            binding.pbLoading.setVisibility(View.GONE);
        if (mAdapter != null) {
            mAdapter.updateDataset(conversation);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void scrollToEnd() {
        if (mAdapter.getItemCount() > 0) {
            binding.histList.scrollToPosition(mAdapter.getItemCount() - 1);
        }
    }

    @Override
    public int getLayout() {
        return R.layout.frag_conversation;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        injectFragment(((RingApplication) getActivity().getApplication()).getRingInjectionComponent());
        marginPx = getResources().getDimensionPixelSize(R.dimen.margin_message_input);
        animation.setDuration(150);
        binding = FragConversationBinding.inflate(inflater, container, false);
        binding.setPresenter(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.msgInputTxt.setMediaListener(contentInfo -> startFileSend(AndroidFileUtils
                        .getCacheFile(requireContext(), contentInfo.getContentUri())
                        .flatMapCompletable(this::sendFile)
                        .doFinally(contentInfo::releasePermission)));

        if (mPreferences != null) {
            String pendingMessage = mPreferences.getString(KEY_PREFERENCE_PENDING_MESSAGE, null);
            if (!TextUtils.isEmpty(pendingMessage)) {
                binding.msgInputTxt.setText(pendingMessage);
                binding.msgSend.setVisibility(View.VISIBLE);
                binding.emojiSend.setVisibility(View.GONE);
            }
        }
        binding.msgInputTxt.setOnEditorActionListener((v, actionId, event) -> actionSendMsgText(actionId));
        binding.msgInputTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String message = s.toString();
                if (TextUtils.isEmpty(message)) {
                    binding.msgSend.setVisibility(View.GONE);
                    binding.emojiSend.setVisibility(View.VISIBLE);
                } else {
                    binding.msgSend.setVisibility(View.VISIBLE);
                    binding.emojiSend.setVisibility(View.GONE);
                }
                if (mPreferences != null) {
                    mPreferences.edit().putString(KEY_PREFERENCE_PENDING_MESSAGE, message).apply();
                }
            }
        });

        animation.addUpdateListener(valueAnimator -> binding.histList.setPadding(
                binding.histList.getPaddingLeft(),
                binding.histList.getPaddingTop(),
                binding.histList.getPaddingRight(),
                Integer.parseInt(valueAnimator.getAnimatedValue().toString())));

        binding.msgInputTxt.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (animation.isStarted())
                animation.cancel();
            animation.setIntValues(binding.histList.getPaddingBottom(), binding.msgInputTxt.getHeight() + marginPx);
            animation.start();
        });

        if (binding != null) {
            binding.ongoingcallPane.setVisibility(View.GONE);
        }

        DefaultItemAnimator animator = (DefaultItemAnimator) binding.histList.getItemAnimator();
        if (animator != null)
            animator.setSupportsChangeAnimations(false);
        binding.histList.setAdapter(mAdapter);
        setHasOptionsMenu(true);
    }

    @Override
    public void setConversationColor(int color) {
        Colorable activity = (Colorable) getActivity();
        if (activity != null)
            activity.setColor(color);
        mAdapter.setPrimaryColor(color);
    }

    @Override
    public void onDestroyView() {
        if (mPreferences != null)
            mPreferences.unregisterOnSharedPreferenceChangeListener(this);
        animation.removeAllUpdateListeners();
        binding.histList.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mAdapter.onContextItemSelected(item))
            return true;
        return super.onContextItemSelected(item);
    }

    public void updateAdapterItem() {
        if(position != -1) {
            mAdapter.notifyItemChanged(position);
            position = -1;
        }
    }

    public void sendMessageText() {
        String message = binding.msgInputTxt.getText().toString();
        clearMsgEdit();
        presenter.sendTextMessage(message);
    }

    public void sendEmoji() {
        presenter.sendTextMessage(binding.emojiSend.getText().toString());
    }

    public void selectFile() {
        presenter.selectFile();
    }

    /**
     * Used to update with the past adapter position when a long click was registered
     * @param position
     */
    public void updatePosition(int position) {
        this.position = position;
    }

    public void takePicture() {
        if (!presenter.getDeviceRuntimeService().hasVideoPermission()) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, RingApplication.PERMISSIONS_REQUEST);
        } else {
            Context c = getContext();
            if (c == null)
                return;
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(c.getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile;
                try {
                    photoFile = AndroidFileUtils.createImageFile(c);
                } catch (IOException ex) {
                    Log.e(TAG, "takePicture: error creating temporary file", ex);
                    return;
                }
                Log.i(TAG, "takePicture: trying to save to " + photoFile);
                mCurrentPhoto = photoFile;
                android.net.Uri photoURI = FileProvider.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE);
            }
        }
    }

    @Override
    public void askWriteExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RingApplication.PERMISSIONS_REQUEST);
    }

    @Override
    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, REQUEST_CODE_FILE_PICKER);
    }

    private Completable sendFile(File file) {
        return Completable.fromAction(() -> presenter.sendFile(file));
    }

    private void startFileSend(Completable op) {
        setLoading(true);
        op.observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> setLoading(false))
                .subscribe(() -> {}, e -> {
                    Log.e(TAG, "startFileSend: not able to create cache file", e);
                    displayErrorToast(RingError.INVALID_FILE);
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.w(TAG, "onActivityResult: " + requestCode + " " + resultCode + " " + resultData);
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == REQUEST_CODE_FILE_PICKER && resultCode == RESULT_OK) {
            if (resultData == null) {
                return;
            }
            android.net.Uri uri = resultData.getData();
            if (uri == null) {
                return;
            }
            startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMapCompletable(this::sendFile));
        } else if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
            if (resultCode != RESULT_OK) {
                mCurrentPhoto = null;
                return;
            }
            Log.w(TAG, "onActivityResult: mCurrentPhoto " + mCurrentPhoto.getAbsolutePath() + " " + mCurrentPhoto.exists() + " " + mCurrentPhoto.length());
            if (mCurrentPhoto == null || !mCurrentPhoto.exists() || mCurrentPhoto.length() == 0) {
                Toast.makeText(getActivity(), "Can't find picture", Toast.LENGTH_SHORT).show();
            }
            File file = mCurrentPhoto;
            mCurrentPhoto = null;
            startFileSend(sendFile(file));
        }
        // File download trough SAF
        else if(requestCode == ConversationFragment.SAVE_FILE_REQUEST_CODE
                && resultCode == RESULT_OK){
            if(resultData != null && resultData.getData() != null ) {
                boolean success = false;
                //Get the Uri of the file that was created by the app that received our intent
                android.net.Uri createdUri = resultData.getData();

                //Try to copy the data of the current file into the newly created one
                File input = new File(mCurrentFileAbsolutePath);
                if(getContext().getContentResolver() != null)
                    success = AndroidFileUtils.copyFileToUri(
                            getContext().getContentResolver(),input,createdUri);
                if(success)
                    Toast.makeText(getContext(), "File saved successfully",
                        Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getContext(), "Failed to save the file",
                            Toast.LENGTH_SHORT).show();
            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0, n = permissions.length; i < n; i++) {
            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    presenter.cameraPermissionChanged(granted);
                    if (granted) {
                        takePicture();
                    }
                    return;
                default:
                    break;
            }
        }
    }

    @Override
    public void addElement(ConversationElement element) {
        mAdapter.add(element);
        scrollToEnd();
    }

    @Override
    public void updateElement(ConversationElement element) {
        mAdapter.update(element);
    }

    @Override
    public void removeElement(ConversationElement element) {
        mAdapter.remove(element);
    }

    @Override
    public void shareFile(File path) {
        Context c = getContext();
        if (c == null)
            return;
        android.net.Uri fileUri = null;
        try {
            fileUri = FileProvider.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path);
        } catch (IllegalArgumentException e) {
            Log.e("File Selector", "The selected file can't be shared: " + path.getName());
        }
        if (fileUri != null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String type = c.getContentResolver().getType(fileUri);
            sendIntent.setDataAndType(fileUri, type);
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            startActivity(Intent.createChooser(sendIntent, null));
        }
    }

    @Override
    public void openFile(File path) {
        Context c = getContext();
        if (c == null)
            return;
        android.net.Uri fileUri = null;
        try {
            fileUri = FileProvider.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path);
        } catch (IllegalArgumentException e) {
            Log.e("File Selector", "The selected file can't be shared: " + path.getName());
        }
        if (fileUri != null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_VIEW);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String type = c.getContentResolver().getType(fileUri);
            sendIntent.setDataAndType(fileUri, type);
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            //startActivity(Intent.createChooser(sendIntent, null));
            try {
                startActivity(sendIntent);
            } catch (ActivityNotFoundException e) {
                Snackbar.make(getView(), R.string.conversation_open_file_error, Snackbar.LENGTH_LONG).show();
                Log.e("File Loader", "File of unknown type, could not open: " + path.getName());
            }
        }
    }

    @OnEditorAction(R.id.msg_input_txt)
    boolean actionSendMsgText(int actionId) {
        switch (actionId) {
            case EditorInfo.IME_ACTION_SEND:
                sendMessageText();
                return true;
        }
        return false;
    }

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
    public void onDetach() {
        if (actionbarTarget != null) {
            actionbarTarget.dispose();
            actionbarTarget = null;
        }
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conversation_actions, menu);
        mAudioCallBtn = menu.findItem(R.id.conv_action_audiocall);
        mVideoCallBtn = menu.findItem(R.id.conv_action_videocall);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(getActivity(), HomeActivity.class));
                return true;
            case R.id.conv_action_audiocall:
                presenter.callWithAudioOnly(true);
                return true;
            case R.id.conv_action_videocall:
                presenter.callWithAudioOnly(false);
                return true;
            case R.id.conv_contact_details:
                presenter.openContact();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void initPresenter(ConversationPresenter presenter) {
        Bundle args = getArguments();
        if (args == null)
            return;
        Uri contactUri = new Uri(args.getString(KEY_CONTACT_RING_ID));
        String accountId = args.getString(KEY_ACCOUNT_ID);
        mAdapter = new ConversationAdapter(this, presenter);
        presenter.init(contactUri, accountId);
        try {
            mPreferences = requireActivity().getSharedPreferences(accountId + "_" + contactUri.getRawRingId(), Context.MODE_PRIVATE);
            mPreferences.registerOnSharedPreferenceChangeListener(this);
            presenter.setConversationColor(mPreferences.getInt(KEY_PREFERENCE_CONVERSATION_COLOR, getResources().getColor(R.color.color_primary_light)));
        } catch (Exception e) {
            Log.e(TAG, "Can't load conversation preferences");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case KEY_PREFERENCE_CONVERSATION_COLOR:
                presenter.setConversationColor(prefs.getInt(KEY_PREFERENCE_CONVERSATION_COLOR, getResources().getColor(R.color.color_primary_light)));
                break;
        }
    }

    @Override
    public void displayContact(final CallContact contact) {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        Context context = actionBar.getThemedContext();
        String displayName = contact.getDisplayName();
        actionBar.setTitle(displayName);

        if (actionbarTarget != null) {
            actionbarTarget.dispose();
            actionbarTarget = null;
        }
        int targetSize = (int) (AvatarFactory.SIZE_AB * context.getResources().getDisplayMetrics().density);
        actionbarTarget = AvatarFactory.getBitmapAvatar(context, contact, targetSize)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(b -> actionBar.setLogo(new BitmapDrawable(context.getResources(), b)));

        String identity = contact.getRingUsername();
        if (identity != null && !identity.equals(displayName)) {
            actionBar.setSubtitle(identity);
        }

        mAdapter.setPhoto();
    }

    @Override
    public void displayOnGoingCallPane(final boolean display) {
        binding.ongoingcallPane.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayNumberSpinner(final Conversation conversation, final Uri number) {
        binding.numberSelector.setVisibility(View.VISIBLE);
        mNumberAdapter = new NumberAdapter(getActivity(),
                conversation.getContact(),
                false);
        binding.numberSelector.setAdapter(mNumberAdapter);
        binding.numberSelector.setSelection(getIndex(binding.numberSelector, number));
    }

    @Override
    public void hideNumberSpinner() {
        binding.numberSelector.setVisibility(View.GONE);
    }

    @Override
    public void clearMsgEdit() {
        binding.msgInputTxt.setText("");
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
                .setClass(requireActivity().getApplicationContext(), CallActivity.class)
                .putExtra(NotificationService.KEY_CALL_ID, conferenceId));
    }

    @Override
    public void goToContactActivity(String accountId, String contactRingId) {
        startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.withAppendedPath(android.net.Uri.withAppendedPath(ContentUriHandler.CONTACT_CONTENT_URI, accountId), contactRingId))
                .setClass(requireActivity().getApplicationContext(), ContactDetailsActivity.class));
    }

    @Override
    public void goToCallActivityWithResult(String accountId, String contactRingId, boolean audioOnly) {
        Intent intent = new Intent(CallActivity.ACTION_CALL)
                .setClass(requireActivity().getApplicationContext(), CallActivity.class)
                .putExtra(KEY_ACCOUNT_ID, accountId)
                .putExtra(CallFragment.KEY_AUDIO_ONLY, audioOnly)
                .putExtra(KEY_CONTACT_RING_ID, contactRingId);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
    }

    public void blockContactRequest() {
        presenter.onBlockIncomingContactRequest();
    }

    public void refuseContactRequest() {
        presenter.onRefuseIncomingContactRequest();
    }

    public void acceptContactRequest() {
        presenter.onAcceptIncomingContactRequest();
    }

    public void addContact() {
        presenter.onAddContact();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean visible = binding.cvMessageInput.getVisibility() == View.VISIBLE;
        if (mAudioCallBtn != null)
            mAudioCallBtn.setVisible(visible);
        if (mVideoCallBtn != null)
            mVideoCallBtn.setVisible(visible);
    }

    @Override
    public void switchToUnknownView(String contactDisplayName) {
        binding.cvMessageInput.setVisibility(View.GONE);
        binding.unknownContactPrompt.setVisibility(View.VISIBLE);
        binding.trustRequestPrompt.setVisibility(View.GONE);
        binding.tvTrustRequestMessage.setText(String.format(getString(R.string.message_contact_not_trusted), contactDisplayName));
        binding.trustRequestMessageLayout.setVisibility(View.VISIBLE);
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void switchToIncomingTrustRequestView(String contactDisplayName) {
        binding.cvMessageInput.setVisibility(View.GONE);
        binding.unknownContactPrompt.setVisibility(View.GONE);
        binding.trustRequestPrompt.setVisibility(View.VISIBLE);
        binding.tvTrustRequestMessage.setText(String.format(getString(R.string.message_contact_not_trusted_yet), contactDisplayName));
        binding.trustRequestMessageLayout.setVisibility(View.VISIBLE);
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void switchToConversationView() {
        binding.cvMessageInput.setVisibility(View.VISIBLE);
        binding.unknownContactPrompt.setVisibility(View.GONE);
        binding.trustRequestPrompt.setVisibility(View.GONE);
        binding.trustRequestMessageLayout.setVisibility(View.GONE);
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void positiveMediaButtonClicked() {
        presenter.clickOnGoingPane();
    }

    @Override
    public void negativeMediaButtonClicked() {
        presenter.clickOnGoingPane();
    }

    @Override
    public void toggleMediaButtonClicked() {
        presenter.clickOnGoingPane();
    }

    private void setLoading(boolean isLoading) {
        if (binding == null)
            return;
        if (isLoading) {
            binding.btnTakePicture.setVisibility(View.GONE);
            binding.sendData.setVisibility(View.GONE);
            binding.pbDataTransfer.setVisibility(View.VISIBLE);
        } else {
            binding.btnTakePicture.setVisibility(View.VISIBLE);
            binding.sendData.setVisibility(View.VISIBLE);
            binding.pbDataTransfer.setVisibility(View.GONE);
        }
    }

    @Override
    public void displayCompletedDownload(DataTransfer transfer, File destination) {
        DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.addCompletedDownload(transfer.getDisplayName(),
                    transfer.getDisplayName(),
                    true,
                    AndroidFileUtils.getMimeType(destination.getAbsolutePath()),
                    destination.getAbsolutePath(),
                    destination.length(),
                    true);
        }
    }

    public void handleShareIntent(Intent intent) {
        String type = intent.getType();
        if (type == null) {
            Log.w(TAG, "Can't share with no type");
            return;
        }
        if (type.startsWith("text/plain")) {
            binding.msgInputTxt.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
        } else {
            android.net.Uri uri = intent.getData();
            ClipData clip = intent.getClipData();
            if (uri == null && clip != null && clip.getItemCount() > 0)
                uri = clip.getItemAt(0).getUri();
            if (uri == null)
                return;
            startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri).flatMapCompletable(this::sendFile));
        }
    }

    /**
     * Creates an intent using Android Storage Access Framework
     * This intent is then received by application that can handle it like
     * Downloads or Google drive
     * @param file DataTranser of the file that is going to be stored
     */
    public void createSaveFileIntent(DataTransfer file){
        //Get the current file absolute path and store it
        mCurrentFileAbsolutePath = presenter.getCurrentFile(file).getAbsolutePath();

        //Use Android Storage File Access to download the file
        Intent downloadFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        downloadFileIntent.setType(AndroidFileUtils.getMimeTypeFromExtension(file.getExtension()));

        downloadFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        downloadFileIntent.putExtra(Intent.EXTRA_TITLE,file.getDisplayName());

        startActivityForResult(downloadFileIntent, ConversationFragment.SAVE_FILE_REQUEST_CODE);
    }

}
