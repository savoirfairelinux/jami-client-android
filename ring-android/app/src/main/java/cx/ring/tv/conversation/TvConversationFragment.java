/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
package cx.ring.tv.conversation;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.MediaViewerActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.conversation.ConversationView;
import cx.ring.model.Account;
import cx.ring.databinding.FragConversationTvBinding;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransfer;
import cx.ring.model.Error;
import cx.ring.model.Interaction;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.tv.camera.CustomCameraActivity;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.StringUtils;
import cx.ring.views.AvatarDrawable;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class TvConversationFragment extends BaseSupportFragment<ConversationPresenter> implements ConversationView {
    private static final String TAG = TvConversationFragment.class.getSimpleName();

    private static final String ARG_MODEL = "model";

    private static final int REQUEST_CODE_PHOTO = 101;
    private static final int REQUEST_SPEECH_CODE = 102;
    private static final int REQUEST_CODE_SAVE_FILE = 103;

    private static final int DIALOG_WIDTH = 900;
    private static final int DIALOG_HEIGHT = 400;

    private TVListViewModel mTvListViewModel;

    private TextView mTitle;
    private TextView mSubTitle;
    private TextView mTextAudio;
    private TextView mTextVideo;
    private TextView mTextMessage;
    private RecyclerView mRecyclerView;
    private ImageButton mAudioButton;

    private int mSelectedPosition;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static File fileName = null;

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    private String[] permissions = { Manifest.permission.RECORD_AUDIO };

    boolean mStartRecording = true;
    boolean mStartPlaying = true;

    private TvConversationAdapter mAdapter = null;
    private AvatarDrawable mConversationAvatar;
    private Map<String, AvatarDrawable> mParticipantAvatars = new HashMap<>();

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private FragConversationTvBinding binding;

    public static TvConversationFragment newInstance(TVListViewModel param) {
        TvConversationFragment fragment = new TvConversationFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MODEL, param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTvListViewModel = getArguments().getParcelable(ARG_MODEL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragConversationTvBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        if (!checkAudioPermission())
            return;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getText(R.string.conversation_input_speech_hint));
        startActivityForResult(intent, REQUEST_SPEECH_CODE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ConversationPath path = ConversationPath.fromIntent(getActivity().getIntent());
        presenter.init(new cx.ring.model.Uri(path.getContactId()), path.getAccountId());

        mAdapter = new TvConversationAdapter(this, presenter);

        ViewGroup textContainer = view.findViewById(R.id.text_container);
        ViewGroup audioContainer = view.findViewById(R.id.audio_container);
        ViewGroup videoContainer = view.findViewById(R.id.video_container);
        mTextAudio = view.findViewById(R.id.text_audio);
        mTextMessage = view.findViewById(R.id.text_text);
        mTextVideo = view.findViewById(R.id.text_video);

        ImageButton text = view.findViewById(R.id.button_text);
        text.setOnClickListener(v -> displaySpeechRecognizer());

        ImageButton video = view.findViewById(R.id.button_video);
        video.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomCameraActivity.class)
                    .setAction(MediaStore.ACTION_VIDEO_CAPTURE);
            startActivityForResult(intent, REQUEST_CODE_PHOTO);
        });

        mAudioButton = view.findViewById(R.id.button_audio);
        mAudioButton.setOnClickListener(v -> {
            onRecord(mStartRecording);
            mStartRecording = !mStartRecording;
        });

        text.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(textContainer);
            mTextMessage.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        mAudioButton.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(audioContainer);
            mTextAudio.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        video.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(videoContainer);
            mTextVideo.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        mTitle = view.findViewById(R.id.title);
        mSubTitle = view.findViewById(R.id.subtitle);
        mRecyclerView = view.findViewById(R.id.recycler_view);

        CallContact contact = mTvListViewModel.getContact();
        if (contact != null) {
            String id = contact.getRingUsername();
            String displayName = contact.getDisplayName();
            mTitle.setText(displayName);
            if (TextUtils.isEmpty(displayName) || !displayName.equals(id))
                mSubTitle.setText(id);
            else
                mSubTitle.setVisibility(View.GONE);
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (mAdapter.onContextItemSelected(item))
            return true;
        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PHOTO:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri media = (Uri) data.getExtras().get(MediaStore.EXTRA_OUTPUT);
                    String type = data.getType();
                    createMediaDialog(media, type);
                }
                break;
            case REQUEST_SPEECH_CODE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String spokenText = results.get(0);
                    createTextDialog(spokenText);
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void createTextDialog(String spokenText) {
        if (StringUtils.isEmpty(spokenText)) {
            return;
        }

        AlertDialog alertDialog = new MaterialAlertDialogBuilder(getContext(), R.style.Theme_MaterialComponents_Dialog)
                .setTitle(spokenText)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send, (dialog, whichButton) -> presenter.sendTextMessage(spokenText))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.getWindow().setLayout(DIALOG_WIDTH, DIALOG_HEIGHT);
        alertDialog.setOwnerActivity(getActivity());
        alertDialog.setOnShowListener(dialog -> {
            Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setFocusable(true);
            positive.setFocusableInTouchMode(true);
            positive.requestFocus();
        });

        alertDialog.show();
    }

    private void createMediaDialog(Uri media, String type) {
        if (media == null) {
            return;
        }
        Activity activity = getActivity();
        if (activity == null)
            return;

        Single<File> file = AndroidFileUtils.getCacheFile(activity, media);
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(activity, R.style.Theme_MaterialComponents_Dialog)
                .setTitle(type.equals(CustomCameraActivity.TYPE_IMAGE) ? R.string.tv_send_image_dialog_message : R.string.tv_send_video_dialog_message)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send, (dialog, whichButton) ->
                        startFileSend(file.flatMapCompletable(TvConversationFragment.this::sendFile)))
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.tv_media_preview, null)
                .create();
        alertDialog.getWindow().setLayout(DIALOG_WIDTH, DIALOG_HEIGHT);
        alertDialog.setOwnerActivity(activity);
        alertDialog.setOnShowListener(dialog -> {
            Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setFocusable(true);
            positive.setFocusableInTouchMode(true);
            positive.requestFocus();

            Button button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(v -> {
                if (type.equals(CustomCameraActivity.TYPE_IMAGE)) {
                    Intent i = new Intent(getContext(), MediaViewerActivity.class);
                    i.setAction(Intent.ACTION_VIEW).setDataAndType(media, "image/*").setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(i);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, media);
                    intent.setDataAndType(media, "video/*").setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                }
            });
        });

        alertDialog.show();
    }

    private void createAudioDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(getContext(), R.style.Theme_MaterialComponents_Dialog)
                .setTitle(R.string.tv_send_audio_dialog_message)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send, (dialog, whichButton) -> sendAudio())
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.tv_audio_play, null)
                .create();
        alertDialog.getWindow().setLayout(DIALOG_WIDTH, DIALOG_HEIGHT);
        alertDialog.setOwnerActivity(getActivity());
        alertDialog.setOnShowListener(dialog -> {
            Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setFocusable(true);
            positive.setFocusableInTouchMode(true);
            positive.requestFocus();

            Button button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(v -> {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    button.setText(R.string.tv_audio_pause);
                    if (player != null) {
                        player.setOnCompletionListener(mp -> {
                            button.setText(R.string.tv_audio_play);
                            mStartPlaying = true;
                        });
                    }
                } else {
                    button.setText(R.string.tv_audio_play);
                }
                mStartPlaying = !mStartPlaying;
            });
        });

        alertDialog.show();
    }

    @Override
    public void addElement(Interaction element) {
        mAdapter.add(element);
        scrollToTop();
    }

    @Override
    public void shareFile(File path) {
        Context c = getContext();
        if (c == null)
            return;
        android.net.Uri fileUri = null;
        try {
            fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path);
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
            fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path);
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
            startActivity(Intent.createChooser(sendIntent, null));
            try {
                startActivity(sendIntent);
            } catch (ActivityNotFoundException e) {
                Snackbar.make(getView(), R.string.conversation_open_file_error, Snackbar.LENGTH_LONG).show();
                Log.e("File Loader", "File of unknown type, could not open: " + path.getName());
            }
        }
    }

    /**
     * Creates an intent using Android Storage Access Framework
     * This intent is then received by applications that can handle it like
     * Downloads or Google drive
     * @param file DataTransfer of the file that is going to be stored
     * @param currentFileAbsolutePath absolute path of the file we want to save
     */
    public void startSaveFile(DataTransfer file, String currentFileAbsolutePath) {
        //Use Android Storage File Access to download the file
        Intent downloadFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        downloadFileIntent.setType(AndroidFileUtils.getMimeTypeFromExtension(file.getExtension()));

        downloadFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        downloadFileIntent.putExtra(Intent.EXTRA_TITLE, file.getDisplayName());

        startActivityForResult(downloadFileIntent, REQUEST_CODE_SAVE_FILE);
    }

    @Override
    public void refreshView(List<Interaction> interactions) {
        if (interactions == null) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.updateDataset(interactions);
        }
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onStop() {
        releaseRecorder();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            // NOOP
        }
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName.getAbsolutePath());
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    private void startRecording() {
        if (!checkAudioPermission())
            return;
        if (recorder != null) {
            return;
        }
        try {
            fileName = AndroidFileUtils.createAudioFile(requireContext());
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFile(fileName.getAbsolutePath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.OGG);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS);
            } else {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            }

            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error starting recording: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
            return;
        }

        mAudioButton.setImageResource(R.drawable.lb_ic_stop);
        mTextAudio.setText(R.string.tv_audio_recording);
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(500);
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        mTextAudio.startAnimation(anim);
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (Exception e) {
                Log.w(TAG, "Exception stopping recorder");
            }
            recorder.release();
            recorder = null;
        }
    }

    private void stopRecording() {
        releaseRecorder();
        mAudioButton.setImageResource(R.drawable.baseline_mic_24);
        mTextAudio.setText(R.string.tv_send_audio);
        mTextAudio.clearAnimation();
        createAudioDialog();
    }

    private void sendAudio() {
        Log.w(TAG, "onActivityResult: fileName " + fileName.getAbsolutePath() + " " + fileName.exists() + " " + fileName.length());
        Single<File> file = Single.just(fileName);
        fileName = null;
        if (file == null) {
            Toast.makeText(getActivity(), "Can't find picture", Toast.LENGTH_SHORT).show();
            return;
        }
        startFileSend(file.flatMapCompletable(this::sendFile));
    }

    private void startFileSend(Completable op) {
        op.observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                }, e -> {
                    Log.e(TAG, "startFileSend: not able to create cache file", e);
                    displayErrorToast(Error.INVALID_FILE);
                });
    }

    private Completable sendFile(File file) {
        return Completable.fromAction(() -> presenter.sendFile(file));
    }

    public void updatePosition(int position) {
        mSelectedPosition = position;
    }

    public void updateAdapterItem() {
        if (mSelectedPosition != -1) {
            mAdapter.notifyItemChanged(mSelectedPosition);
            mSelectedPosition = -1;
        }
    }

    private void scrollToTop() {
        if (mAdapter.getItemCount() > 0) {
            mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
        }
    }

    @Override
    public void displayContact(CallContact contact) {
        mCompositeDisposable.clear();
        mCompositeDisposable.add(AvatarFactory.getAvatar(requireContext(), contact)
                .doOnSuccess(d -> {
                    mConversationAvatar = (AvatarDrawable) d;
                    mParticipantAvatars.put(contact.getPrimaryNumber(),
                            new AvatarDrawable((AvatarDrawable) d));
                })
                .flatMapObservable(d -> contact.getUpdatesSubject())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(c -> {
                    mConversationAvatar.update(c);
                    String uri = contact.getPrimaryNumber();
                    if (mParticipantAvatars.containsKey(uri)) {
                        mParticipantAvatars.get(uri).update(c);
                    }
                    mAdapter.setPhoto();
                }));
    }

    @Override
    public void updateElement(Interaction element) {
        mAdapter.update(element);
    }

    @Override
    public void removeElement(Interaction element) {
        mAdapter.remove(element);
    }

    public AvatarDrawable getConversationAvatar(String uri) {
        return mParticipantAvatars.get(uri);
    }

    public void askWriteExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, JamiApplication.PERMISSIONS_REQUEST);
    }

    @Override
    public void scrollToEnd() {

    }

    @Override
    public void setComposingStatus(Account.ComposingStatus composingStatus) {

    }

    @Override
    public void setLastDisplayed(Interaction interaction) {

    }

    @Override
    public void setConversationColor(int integer) {

    }

    @Override
    public void startShareLocation(String accountId, String contactId) {

    }

    @Override
    public void showMap(String accountId, String contactId, boolean open) {

    }

    @Override
    public void hideMap() {

    }

    @Override
    public void hideErrorPanel() {

    }

    @Override
    public void displayNetworkErrorPanel() {

    }

    @Override
    public void displayOnGoingCallPane(boolean display) {

    }

    @Override
    public void displayNumberSpinner(Conversation conversation, cx.ring.model.Uri number) {

    }

    @Override
    public void hideNumberSpinner() {

    }

    @Override
    public void clearMsgEdit() {

    }

    @Override
    public void goToHome() {

    }

    @Override
    public void goToAddContact(CallContact callContact) {

    }

    @Override
    public void goToCallActivity(String conferenceId) {

    }

    @Override
    public void goToCallActivityWithResult(String accountId, String contactRingId, boolean audioOnly) {

    }

    @Override
    public void goToContactActivity(String accountId, String contactRingId) {

    }

    @Override
    public void switchToUnknownView(String name) {
        // todo
    }

    @Override
    public void switchToIncomingTrustRequestView(String message) {
        // todo
    }

    @Override
    public void switchToConversationView() {
        // todo
    }

    @Override
    public void openFilePicker() {

    }

}
