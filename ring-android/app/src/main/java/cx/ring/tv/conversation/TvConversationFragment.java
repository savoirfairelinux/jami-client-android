/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import androidx.transition.TransitionManager;

import android.os.Environment;
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
import cx.ring.views.AvatarFactory;
import net.jami.conversation.ConversationPresenter;
import net.jami.conversation.ConversationView;
import net.jami.model.Account;
import cx.ring.databinding.FragConversationTvBinding;
import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.model.DataTransfer;
import net.jami.model.Error;
import net.jami.model.Interaction;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.service.DRingService;
import cx.ring.tv.camera.CustomCameraActivity;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import net.jami.utils.StringUtils;
import cx.ring.views.AvatarDrawable;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class TvConversationFragment extends BaseSupportFragment<ConversationPresenter, ConversationView> implements ConversationView {
    private static final String TAG = TvConversationFragment.class.getSimpleName();

    private static final String ARG_MODEL = "model";
    private static final String KEY_AUDIOFILE = "audiofile";

    private static final int REQUEST_CODE_PHOTO = 101;
    private static final int REQUEST_SPEECH_CODE = 102;
    private static final int REQUEST_CODE_SAVE_FILE = 103;

    private static final int DIALOG_WIDTH = 900;
    private static final int DIALOG_HEIGHT = 400;

    private ConversationPath mConversationPath;

    private int mSelectedPosition;

    private static final String[] permissions = { Manifest.permission.RECORD_AUDIO };
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private File fileName = null;

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    boolean mStartRecording = true;
    boolean mStartPlaying = true;

    private TvConversationAdapter mAdapter = null;
    private AvatarDrawable mConversationAvatar;
    private final Map<String, AvatarDrawable> mParticipantAvatars = new HashMap<>();

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private FragConversationTvBinding binding;

    private String mCurrentFileAbsolutePath = null;

    public static TvConversationFragment newInstance(Bundle args) {
        TvConversationFragment fragment = new TvConversationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mConversationPath = ConversationPath.fromBundle(getArguments());
        }
        String audiofile = savedInstanceState == null ? null : savedInstanceState.getString(KEY_AUDIOFILE);
        if (audiofile != null) {
            fileName = new File(audiofile);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (fileName != null) {
            outState.putString(KEY_AUDIOFILE, fileName.getAbsolutePath());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragConversationTvBinding.inflate(inflater, container, false);
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
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_PROMPT, getText(R.string.conversation_input_speech_hint));
            startActivityForResult(intent, REQUEST_SPEECH_CODE);
        } catch (Exception e) {
            Snackbar.make(requireView(), "Can't get voice input", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //ConversationPath path = ConversationPath.fromIntent(requireActivity().getIntent());
        presenter.init(mConversationPath.getConversationUri(), mConversationPath.getAccountId());
        mAdapter = new TvConversationAdapter(this, presenter);

        binding.buttonText.setOnClickListener(v -> displaySpeechRecognizer());
        binding.buttonVideo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomCameraActivity.class)
                    .setAction(MediaStore.ACTION_VIDEO_CAPTURE);
            startActivityForResult(intent, REQUEST_CODE_PHOTO);
        });

        binding.buttonAudio.setOnClickListener(v -> {
            onRecord(mStartRecording);
            mStartRecording = !mStartRecording;
        });

        binding.buttonText.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(binding.textContainer);
            binding.textText.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        binding.buttonAudio.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(binding.audioContainer);
            binding.textAudio.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        binding.buttonVideo.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(binding.videoContainer);
            binding.textVideo.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(mAdapter);
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
                break;
            case REQUEST_CODE_SAVE_FILE:
                if(resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        writeToFile(data.getData());
                    }
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void writeToFile(Uri data) {
        File input = new File(mCurrentFileAbsolutePath);
        if (requireContext().getContentResolver() != null)
            mCompositeDisposable.add(AndroidFileUtils.copyFileToUri(requireContext().getContentResolver(), input, data).
                    observeOn(AndroidSchedulers.mainThread()).
                    subscribe(() -> Toast.makeText(getContext(), R.string.file_saved_successfully, Toast.LENGTH_SHORT).show(),
                            error -> Toast.makeText(getContext(), R.string.generic_error, Toast.LENGTH_SHORT).show()));
    }

    private void createTextDialog(String spokenText) {
        if (StringUtils.isEmpty(spokenText)) {
            return;
        }

        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialComponents_Dialog)
                .setTitle(spokenText)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send, (dialog, whichButton) -> presenter.sendTextMessage(spokenText))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.getWindow().setLayout(DIALOG_WIDTH, DIALOG_HEIGHT);
        alertDialog.setOwnerActivity(requireActivity());
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
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialComponents_Dialog)
                .setTitle(R.string.tv_send_audio_dialog_message)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send, (dialog, whichButton) -> sendAudio())
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.tv_audio_play, null)
                .create();
        alertDialog.getWindow().setLayout(DIALOG_WIDTH, DIALOG_HEIGHT);
        alertDialog.setOwnerActivity(requireActivity());
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
    public void shareFile(File path, String displayName) {
        Context c = getContext();
        if (c == null)
            return;
        try {
            android.net.Uri fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path);
            if (fileUri != null) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String type = c.getContentResolver().getType(fileUri);
                sendIntent.setDataAndType(fileUri, type);
                sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                startActivity(Intent.createChooser(sendIntent, null));
            }
        } catch (Exception e) {
            Snackbar.make(requireView(), "Error sharing file: " + e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void openFile(File path, String displayName) {
        Context c = getContext();
        if (c == null)
            return;
        try {
            android.net.Uri fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path);
            if (fileUri != null) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_VIEW);
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String type = c.getContentResolver().getType(fileUri);
                sendIntent.setDataAndType(fileUri, type);
                sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                startActivity(Intent.createChooser(sendIntent, null));
            }
        } catch (IllegalArgumentException e) {
            Snackbar.make(requireView(), "Error opening file: " + e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
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
        mCurrentFileAbsolutePath = currentFileAbsolutePath;
        try {
            // Use Android Storage File Access to download the file
            Intent downloadFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            downloadFileIntent.setType(AndroidFileUtils.getMimeTypeFromExtension(file.getExtension()));

            downloadFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            downloadFileIntent.putExtra(Intent.EXTRA_TITLE, file.getDisplayName());

            startActivityForResult(downloadFileIntent, REQUEST_CODE_SAVE_FILE);
        } catch (Exception e) {
            Log.i(TAG, "No app detected for saving files.");
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            writeToFile(Uri.fromFile(new File(directory, file.getDisplayName())));
        }
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
        if (fileName == null)
            return;
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

        binding.buttonAudio.setImageResource(R.drawable.lb_ic_stop);
        binding.textAudio.setText(R.string.tv_audio_recording);
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(500);
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        binding.textAudio.startAnimation(anim);
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
        binding.buttonAudio.setImageResource(R.drawable.baseline_androidtv_message_audio);
        binding.textAudio.setText(R.string.tv_send_audio);
        binding.textAudio.clearAnimation();
        createAudioDialog();
    }

    private void sendAudio() {
        if (fileName != null) {
            Single<File> file = Single.just(fileName);
            fileName = null;
            startFileSend(file.flatMapCompletable(this::sendFile));
        }
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
            binding.recyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
        }
    }

    @Override
    public void displayContact(Conversation conversation) {
        List<Contact> contacts = conversation.getContacts();
        mCompositeDisposable.clear();
        mCompositeDisposable.add(AvatarFactory.getAvatar(requireContext(), conversation, true)
                .doOnSuccess(d -> {
                    mConversationAvatar = (AvatarDrawable) d;
                    mParticipantAvatars.put(contacts.get(0).getPrimaryNumber(),
                            new AvatarDrawable((AvatarDrawable) d));
                })
                .flatMapObservable(d -> contacts.get(0).getUpdatesSubject())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(c -> {
                    String id = c.getRingUsername();
                    String displayName = c.getDisplayName();
                    binding.title.setText(displayName);
                    if (TextUtils.isEmpty(displayName) || !displayName.equals(id))
                        binding.subtitle.setText(id);
                    else
                        binding.subtitle.setVisibility(View.GONE);
                    mConversationAvatar.update(c);
                    String uri = contacts.get(0).getPrimaryNumber();
                    AvatarDrawable a = mParticipantAvatars.get(uri);
                    if (a != null)
                        a.update(c);
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
    public void updateContact(Contact contact) {
        mCompositeDisposable.add(AvatarFactory.getAvatar(requireContext(), contact, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatar -> {
                    mParticipantAvatars.put(contact.getPrimaryNumber(), (AvatarDrawable) avatar);
                    mAdapter.setPhoto();
                }));
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
    public void setConversationSymbol(CharSequence symbol) {

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

    public void showPluginListHandlers(String accountId, String contactId) {

    }

    @Override
    public void hideErrorPanel() {

    }

    @Override
    public void displayNetworkErrorPanel() {

    }

    @Override
    public void displayAccountOfflineErrorPanel() {

    }

    @Override
    public void setReadIndicatorStatus(boolean show) {

    }

    @Override
    public void updateLastRead(String last) {

    }

    @Override
    public void displayOnGoingCallPane(boolean display) {

    }

    @Override
    public void displayNumberSpinner(Conversation conversation, net.jami.model.Uri number) {

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
    public void goToAddContact(Contact contact) {

    }

    @Override
    public void goToCallActivity(String conferenceId) {

    }

    @Override
    public void goToCallActivityWithResult(String accountId, net.jami.model.Uri conversationUri, net.jami.model.Uri contactRingId, boolean audioOnly) {

    }

    @Override
    public void goToContactActivity(String accountId, net.jami.model.Uri contactRingId) {

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
    public void switchToSyncingView() {
        // todo
    }


    @Override
    public void openFilePicker() {

    }

    @Override
    public void acceptFile(String accountId, net.jami.model.Uri conversationUri, DataTransfer transfer) {
        File cacheDir = requireContext().getCacheDir();
        long spaceLeft = AndroidFileUtils.getSpaceLeft(cacheDir.toString());
        if (spaceLeft == -1L || transfer.getTotalSize() > spaceLeft) {
            presenter.noSpaceLeft();
            return;
        }
        requireActivity().startService(new Intent(DRingService.ACTION_FILE_ACCEPT)
                .setClass(requireContext(), DRingService.class)
                .setData(ConversationPath.toUri(accountId, conversationUri))
                .putExtra(DRingService.KEY_MESSAGE_ID, transfer.getMessageId())
                .putExtra(DRingService.KEY_TRANSFER_ID, transfer.getFileId()));
    }

    @Override
    public void refuseFile(String accountId, net.jami.model.Uri conversationUri, DataTransfer transfer) {
        requireActivity().startService(new Intent(DRingService.ACTION_FILE_CANCEL)
                .setClass(requireContext(), DRingService.class)
                .setData(ConversationPath.toUri(accountId, conversationUri))
                .putExtra(DRingService.KEY_MESSAGE_ID, transfer.getMessageId())
                .putExtra(DRingService.KEY_TRANSFER_ID, transfer.getFileId()));
    }

}
