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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import cx.ring.contacts.AvatarFactory;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.model.CallContact;
import cx.ring.model.DataTransfer;
import cx.ring.model.Error;
import cx.ring.model.Interaction;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.views.AvatarDrawable;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class TvConversationFragment extends BaseSupportFragment<TvConversationPresenter> implements TvConversationView {

    private static final String ARG_MODEL = "model";

    private static final int SPEECH_REQUEST_CODE = 43600;
    private static final int REQUEST_CODE_SAVE_FILE = 1003;

    private TVListViewModel mTvListViewModel;

    private TextView mTitle;
    private TextView mSubTitle;
    private TextView mTextAudio;
    private TextView mTextMessage;
    private RecyclerView mRecyclerView;
    private ImageButton mAudioButton;

    private String mCurrentFileAbsolutePath = null;

    private int mSelectedPosition;

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static File fileName = null;

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    boolean mStartRecording = true;
    boolean mStartPlaying = true;

    private TvConversationAdapter mAdapter = null;
    private AvatarDrawable mConversationAvatar;
    private Map<String, AvatarDrawable> mParticipantAvatars = new HashMap<>();

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public static TvConversationFragment newInstance(TVListViewModel param) {
        TvConversationFragment fragment = new TvConversationFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MODEL, param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((JamiApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTvListViewModel = getArguments().getParcelable(ARG_MODEL);
        }

        ActivityCompat.requestPermissions(getActivity(), permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say something...");
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }


    @Override
    public int getLayout() {
        return R.layout.frag_conversation_tv;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ConversationPath path = ConversationPath.fromIntent(getActivity().getIntent());

        presenter.init(path);

        mAdapter = new TvConversationAdapter(this, presenter);

        ViewGroup textContainer = view.findViewById(R.id.text_container);
        ViewGroup audioContainer = view.findViewById(R.id.audio_container);
        mTextAudio = view.findViewById(R.id.text_audio);
        mTextMessage = view.findViewById(R.id.text_text);

        ImageButton text = view.findViewById(R.id.button_text);
        text.setOnClickListener(v -> displaySpeechRecognizer());

        mAudioButton = view.findViewById(R.id.button_audio);
        mAudioButton.setOnClickListener(v -> {
            onRecord(mStartRecording);
            mStartRecording = !mStartRecording;
        });

        mAudioButton.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(textContainer);
            mTextAudio.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        text.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(audioContainer);
            mTextMessage.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        mTitle = view.findViewById(R.id.title);
        mSubTitle = view.findViewById(R.id.subtitle);
        mRecyclerView = view.findViewById(R.id.recycler_view);

        String id = mTvListViewModel.getContact().getRingUsername();
        String displayName = mTvListViewModel.getContact().getDisplayName();
        mTitle.setText(displayName);
        if (TextUtils.isEmpty(displayName) || !displayName.equals(id))
            mSubTitle.setText(id);
        else
            mSubTitle.setVisibility(View.GONE);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (mAdapter.onContextItemSelected(item))
            return true;
        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            createTextDialog(spokenText);
        }
    }

    private void createTextDialog(String spokenText) {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(getContext(), R.style.Theme_MaterialComponents_Dialog)
                .setTitle(spokenText)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send, (dialog, whichButton) -> presenter.sendText(spokenText))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.getWindow().setLayout(900, 400);
        alertDialog.setOwnerActivity(getActivity());
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive= alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setFocusable(true);
                positive.setFocusableInTouchMode(true);
                positive.requestFocus();
            }
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
        alertDialog.getWindow().setLayout(900, 400);
        alertDialog.setOwnerActivity(getActivity());
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive= alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setFocusable(true);
                positive.setFocusableInTouchMode(true);
                positive.requestFocus();

                Button button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPlay(mStartPlaying);
                        if (mStartPlaying) {
                            button.setText(R.string.tv_audio_pause);
                            if (player != null) {
                                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        button.setText(R.string.tv_audio_play);
                                        mStartPlaying = true;
                                    }
                                });
                            }
                        } else {
                            button.setText(R.string.tv_audio_play);
                        }
                        mStartPlaying = !mStartPlaying;
                    }
                });
            }
        });

        alertDialog.show();
    }

    @Override
    public void addElement(Interaction element) {
        mAdapter.add(element);
        scrollToTop();
    }

    @Override
    public void updateElement(Interaction element) {
        mAdapter.update(element);
    }

    @Override
    public void removeElement(Interaction element) {
        mAdapter.remove(element);
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
    public void startSaveFile(DataTransfer file, String currentFileAbsolutePath){
        //Get the current file absolute path and store it
        mCurrentFileAbsolutePath = currentFileAbsolutePath;

        //Use Android Storage File Access to download the file
        Intent downloadFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        downloadFileIntent.setType(AndroidFileUtils.getMimeTypeFromExtension(file.getExtension()));

        downloadFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        downloadFileIntent.putExtra(Intent.EXTRA_TITLE,file.getDisplayName());

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) getActivity().finish();

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
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    private void startRecording() {
        try {
            fileName = AndroidFileUtils.createAudioFile(getContext());
        } catch (IOException e) {
            return;
        }
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

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();

        mAudioButton.setImageResource(R.drawable.lb_ic_stop);
        mTextAudio.setText(R.string.tv_audio_recording);
    }

    private void stopRecording() {
        if (recorder == null) {
            return;
        }
        recorder.stop();
        recorder.release();
        recorder = null;

        mAudioButton.setImageResource(R.drawable.baseline_mic_24);
        mTextAudio.setText(R.string.tv_send_audio);

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
                .subscribe(() -> {}, e -> {
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

    @Override
    public void scrollToTop() {
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

    public AvatarDrawable getConversationAvatar(String uri) {
        return mParticipantAvatars.get(uri);
    }

    public void askWriteExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, JamiApplication.PERMISSIONS_REQUEST);
    }

}
