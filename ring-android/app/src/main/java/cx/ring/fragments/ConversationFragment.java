/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.jami.conversation.ConversationPresenter;
import net.jami.conversation.ConversationView;
import net.jami.daemon.JamiService;
import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.model.DataTransfer;
import net.jami.model.Error;
import net.jami.model.Interaction;
import net.jami.model.Phone;
import net.jami.model.Uri;
import net.jami.services.NotificationService;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cx.ring.R;
import cx.ring.adapters.ConversationAdapter;
import cx.ring.application.JamiApplication;
import cx.ring.client.CallActivity;
import cx.ring.client.ContactDetailsActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragConversationBinding;
import cx.ring.interfaces.Colorable;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.service.DRingService;
import cx.ring.service.LocationSharingService;
import cx.ring.services.NotificationServiceImpl;
import cx.ring.services.SharedPreferencesServiceImpl;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.MediaButtonsHelper;
import cx.ring.views.AvatarDrawable;
import cx.ring.views.AvatarFactory;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;

@AndroidEntryPoint
public class ConversationFragment extends BaseSupportFragment<ConversationPresenter, ConversationView> implements
        MediaButtonsHelper.MediaButtonsHelperCallback,
        ConversationView, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = ConversationFragment.class.getSimpleName();

    public static final int REQ_ADD_CONTACT = 42;

    public static final String KEY_PREFERENCE_PENDING_MESSAGE = "pendingMessage";
    public static final String KEY_PREFERENCE_CONVERSATION_COLOR = "color";
    public static final String KEY_PREFERENCE_CONVERSATION_LAST_READ = "lastRead";
    public static final String KEY_PREFERENCE_CONVERSATION_SYMBOL = "symbol";
    public static final String EXTRA_SHOW_MAP = "showMap";

    private static final int REQUEST_CODE_FILE_PICKER = 1000;
    private static final int REQUEST_PERMISSION_CAMERA = 1001;
    private static final int REQUEST_CODE_TAKE_PICTURE = 1002;
    private static final int REQUEST_CODE_SAVE_FILE = 1003;
    private static final int REQUEST_CODE_CAPTURE_AUDIO = 1004;
    private static final int REQUEST_CODE_CAPTURE_VIDEO = 1005;

    private ServiceConnection locationServiceConnection = null;

    private FragConversationBinding binding;
    private MenuItem mAudioCallBtn = null;
    private MenuItem mVideoCallBtn = null;

    private View currentBottomView = null;
    private ConversationAdapter mAdapter = null;
    private int marginPx;
    private int marginPxTotal;
    private final ValueAnimator animation = new ValueAnimator();

    private SharedPreferences mPreferences;

    private File mCurrentPhoto = null;
    private String mCurrentFileAbsolutePath = null;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private int mSelectedPosition;

    private boolean mIsBubble;

    private AvatarDrawable mConversationAvatar;
    private final Map<String, AvatarDrawable> mParticipantAvatars = new HashMap<>();
    private final Map<String, AvatarDrawable> mSmallParticipantAvatars = new HashMap<>();
    private int mapWidth, mapHeight;
    private String mLastRead;

    private boolean loading = true;

    public AvatarDrawable getConversationAvatar(String uri) {
        return mParticipantAvatars.get(uri);
    }
    public AvatarDrawable getSmallConversationAvatar(String uri) {
        synchronized (mSmallParticipantAvatars) {
            return mSmallParticipantAvatars.get(uri);
        }
    }

    private static int getIndex(Spinner spinner, Uri myString) {
        for (int i = 0, n = spinner.getCount(); i < n; i++)
            if (((Phone) spinner.getItemAtPosition(i)).getNumber().equals(myString)) {
                return i;
            }
        return 0;
    }

    @Override
    public void refreshView(final List<Interaction> conversation) {
        if (conversation == null) {
            return;
        }
        if (binding != null)
            binding.pbLoading.setVisibility(View.GONE);
        if (mAdapter != null) {
            mAdapter.updateDataset(conversation);
            loading = false;
        }
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void scrollToEnd() {
        if (mAdapter.getItemCount() > 0) {
            binding.histList.scrollToPosition(mAdapter.getItemCount() - 1);
        }
    }

    private static void setBottomPadding(@NonNull View view, int padding) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                padding);
    }

    private void updateListPadding() {
        int bottomViewHeight = currentBottomView != null ? currentBottomView.getHeight() : 0;
        setBottomPadding(binding.histList, bottomViewHeight + marginPxTotal);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.mapCard.getLayoutParams();
        params.bottomMargin = bottomViewHeight + marginPxTotal;
        binding.mapCard.setLayoutParams(params);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Resources res = getResources();
        marginPx = res.getDimensionPixelSize(R.dimen.conversation_message_input_margin);
        mapWidth = res.getDimensionPixelSize(R.dimen.location_sharing_minmap_width);
        mapHeight = res.getDimensionPixelSize(R.dimen.location_sharing_minmap_height);
        marginPxTotal = marginPx;

        binding = FragConversationBinding.inflate(inflater, container, false);
        binding.setPresenter(this);

        animation.setDuration(150);
        animation.addUpdateListener(valueAnimator -> setBottomPadding(binding.histList, (Integer)valueAnimator.getAnimatedValue()));

        ViewCompat.setOnApplyWindowInsetsListener(binding.histList, (v, insets) -> {
            marginPxTotal = marginPx + insets.getSystemWindowInsetBottom();
            updateListPadding();
            insets.consumeSystemWindowInsets();
            return insets;
        });
        View layout = binding.conversationLayout;

        // remove action bar height for tablet layout
        if (DeviceUtils.isTablet(layout.getContext())) {
            layout.setPadding(layout.getPaddingLeft(), 0, layout.getPaddingRight(), layout.getPaddingBottom());
        }

        int paddingTop = layout.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, insets) -> {
            v.setPadding(
                    v.getPaddingLeft(),
                    paddingTop + insets.getSystemWindowInsetTop(),
                    v.getPaddingRight(),
                    v.getPaddingBottom());
            insets.consumeSystemWindowInsets();
            return insets;
        });

        binding.ongoingcallPane.setVisibility(View.GONE);
        binding.msgInputTxt.setMediaListener(contentInfo -> startFileSend(AndroidFileUtils
                .getCacheFile(requireContext(), contentInfo.getContentUri())
                .flatMapCompletable(this::sendFile)
                .doFinally(contentInfo::releasePermission)));
        binding.msgInputTxt.setOnEditorActionListener((v, actionId, event) -> actionSendMsgText(actionId));
        binding.msgInputTxt.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus)  {
                Fragment fragment = getChildFragmentManager().findFragmentById(R.id.mapLayout);
                if (fragment != null) {
                    ((LocationSharingFragment) fragment).hideControls();
                }
            }
        });
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
                boolean hasMessage = !TextUtils.isEmpty(message);
                presenter.onComposingChanged(hasMessage);
                if (hasMessage) {
                    binding.msgSend.setVisibility(View.VISIBLE);
                    binding.emojiSend.setVisibility(View.GONE);
                } else {
                    binding.msgSend.setVisibility(View.GONE);
                    binding.emojiSend.setVisibility(View.VISIBLE);
                }
                if (mPreferences != null) {
                    if (hasMessage)
                        mPreferences.edit().putString(KEY_PREFERENCE_PENDING_MESSAGE, message).apply();
                    else
                        mPreferences.edit().remove(KEY_PREFERENCE_PENDING_MESSAGE).apply();
                }
            }
        });

        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mPreferences != null) {
            String pendingMessage = mPreferences.getString(KEY_PREFERENCE_PENDING_MESSAGE, null);
            if (!TextUtils.isEmpty(pendingMessage)) {
                binding.msgInputTxt.setText(pendingMessage);
                binding.msgSend.setVisibility(View.VISIBLE);
                binding.emojiSend.setVisibility(View.GONE);
            }
        }

        binding.msgInputTxt.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (oldBottom == 0 && oldTop == 0) {
                updateListPadding();
            } else {
                if (animation.isStarted())
                    animation.cancel();
                animation.setIntValues(binding.histList.getPaddingBottom(), (currentBottomView == null ? 0 : currentBottomView.getHeight()) + marginPxTotal);
                animation.start();
            }
        });

        binding.histList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            // The minimum amount of items to have below current scroll position
            // before loading more.
            static private final int visibleThreshold = 3;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!loading && layoutManager.findFirstVisibleItemPosition() < visibleThreshold) {
                    loading = true;
                    presenter.loadMore();
                }
            }
        });

        DefaultItemAnimator animator = (DefaultItemAnimator) binding.histList.getItemAnimator();
        if (animator != null)
            animator.setSupportsChangeAnimations(false);
        binding.histList.setAdapter(mAdapter);
    }

    @Override
    public void setConversationColor(int color) {
        Colorable activity = (Colorable) getActivity();
        if (activity != null)
            activity.setColor(color);
        mAdapter.setPrimaryColor(color);
    }

    @Override
    public void setConversationSymbol(CharSequence symbol) {
        binding.emojiSend.setText(symbol);
    }

    @Override
    public void onDestroyView() {
        if (mPreferences != null)
            mPreferences.unregisterOnSharedPreferenceChangeListener(this);
        animation.removeAllUpdateListeners();
        binding.histList.setAdapter(null);
        mCompositeDisposable.clear();
        if (locationServiceConnection != null) {
            try {
                requireContext().unbindService(locationServiceConnection);
            } catch (Exception e) {
                Log.w(TAG, "Error unbinding service: " + e.getMessage());
            }
        }
        mAdapter = null;
        super.onDestroyView();
        binding = null;
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (mAdapter.onContextItemSelected(item))
            return true;
        return super.onContextItemSelected(item);
    }

    public void updateAdapterItem() {
        if (mSelectedPosition != -1) {
            mAdapter.notifyItemChanged(mSelectedPosition);
            mSelectedPosition = -1;
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

    @SuppressLint("RestrictedApi")
    public void expandMenu(View v) {
        Context context = requireContext();
        PopupMenu popup = new PopupMenu(context, v);
        popup.inflate(R.menu.conversation_share_actions);
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.conv_send_audio) {
                sendAudioMessage();
            } else if (itemId == R.id.conv_send_video) {
                sendVideoMessage();
            } else if (itemId == R.id.conv_send_file) {
                presenter.selectFile();
            } else if (itemId == R.id.conv_share_location) {
                shareLocation();
            } else if (itemId == R.id.chat_plugins) {
                presenter.showPluginListHandlers();
            }
            return false;
        });
        popup.getMenu().findItem(R.id.chat_plugins).setVisible(JamiService.getPluginsEnabled() && !JamiService.getChatHandlers().isEmpty());
        MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popup.getMenu(), v);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    public void showPluginListHandlers(String accountId, String contactId) {
        Log.w(TAG, "show Plugin Chat Handlers List");

        FragmentManager fragmentManager = getChildFragmentManager();
        PluginHandlersListFragment fragment = PluginHandlersListFragment.newInstance(accountId, contactId);
        fragmentManager.beginTransaction()
                .add(R.id.pluginListHandlers, fragment, PluginHandlersListFragment.TAG)
                .commit();

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.mapCard.getLayoutParams();
        if (params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            binding.mapCard.setLayoutParams(params);
        }
        binding.mapCard.setVisibility(View.VISIBLE);
    }

    public void hidePluginListHandlers() {
        if (binding.mapCard.getVisibility() != View.GONE) {
            binding.mapCard.setVisibility(View.GONE);

            FragmentManager fragmentManager = getChildFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.pluginListHandlers);

            if (fragment != null) {
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commit();
            }
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.mapCard.getLayoutParams();
        if (params.width != mapWidth) {
            params.width = mapWidth;
            params.height = mapHeight;
            binding.mapCard.setLayoutParams(params);
        }
    }

    public void shareLocation() {
        presenter.shareLocation();
    }

    public void closeLocationSharing(boolean isSharing) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.mapCard.getLayoutParams();
        if (params.width != mapWidth) {
            params.width = mapWidth;
            params.height = mapHeight;
            binding.mapCard.setLayoutParams(params);
        }
        if (!isSharing)
            hideMap();
    }

    public void openLocationSharing() {
        binding.conversationLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.mapCard.getLayoutParams();
        if (params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            binding.mapCard.setLayoutParams(params);
        }
    }

    @Override
    public void startShareLocation(String accountId, String conversationId) {
        showMap(accountId, conversationId, true);
    }

    /**
     * Used to update with the past adapter position when a long click was registered
     */
    public void updatePosition(int position) {
        mSelectedPosition = position;
    }

    @Override
    public void showMap(String accountId, String contactId, boolean open)  {
        if (binding.mapCard.getVisibility() == View.GONE) {
            Log.w(TAG, "showMap " + accountId + " " + contactId);

            FragmentManager fragmentManager = getChildFragmentManager();
            LocationSharingFragment fragment = LocationSharingFragment.newInstance(accountId, contactId, open);
            fragmentManager.beginTransaction()
                    .add(R.id.mapLayout, fragment, "map")
                    .commit();
            binding.mapCard.setVisibility(View.VISIBLE);
        }
        if (open) {
            Fragment fragment = getChildFragmentManager().findFragmentById(R.id.mapLayout);
            if (fragment != null) {
                ((LocationSharingFragment) fragment).showControls();
            }
        }
    }

    @Override
    public void hideMap() {
        if (binding.mapCard.getVisibility() != View.GONE) {
            binding.mapCard.setVisibility(View.GONE);

            FragmentManager fragmentManager = getChildFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.mapLayout);

            if (fragment != null) {
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commit();
            }
        }
    }

    public void sendAudioMessage() {
        if (!presenter.getDeviceRuntimeService().hasAudioPermission()) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_CAPTURE_AUDIO);
        } else {
            try {
                Context ctx = requireContext();
                Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                mCurrentPhoto = AndroidFileUtils.createAudioFile(ctx);
                startActivityForResult(intent, REQUEST_CODE_CAPTURE_AUDIO);
            } catch (Exception ex) {
                Log.e(TAG, "sendAudioMessage: error", ex);
                Toast.makeText(getActivity(), "Can't find audio recorder app", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void sendVideoMessage() {
        if (!presenter.getDeviceRuntimeService().hasVideoPermission()) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAPTURE_VIDEO);
        } else {
            try {
                Context context = requireContext();
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                intent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
                intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
                    mCurrentPhoto = AndroidFileUtils.createVideoFile(context);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, ContentUriHandler.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, mCurrentPhoto));
                startActivityForResult(intent, REQUEST_CODE_CAPTURE_VIDEO);
            } catch (Exception ex) {
                Log.e(TAG, "sendVideoMessage: error", ex);
                Toast.makeText(getActivity(), "Can't find video recorder app", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void takePicture() {
        if (!presenter.getDeviceRuntimeService().hasVideoPermission()) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_TAKE_PICTURE);
            return;
        }
        Context c = getContext();
        if (c == null)
            return;
        try {
            File photoFile = AndroidFileUtils.createImageFile(c);
            Log.i(TAG, "takePicture: trying to save to " + photoFile);
            android.net.Uri photoURI = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, photoFile);
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    .putExtra("android.intent.extras.CAMERA_FACING", 1)
                    .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    .putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            mCurrentPhoto = photoFile;
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE);
        } catch (Exception e) {
            Toast.makeText(c, "Error taking picture: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void askWriteExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, JamiApplication.PERMISSIONS_REQUEST);
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
                    displayErrorToast(Error.INVALID_FILE);
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        Log.w(TAG, "onActivityResult: " + requestCode + " " + resultCode + " " + resultData);
        android.net.Uri uri = resultData == null ? null : resultData.getData();
        if (requestCode == REQUEST_CODE_FILE_PICKER) {
            if (resultCode == RESULT_OK && uri != null) {
                startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMapCompletable(this::sendFile));
            }
        } else if (requestCode == REQUEST_CODE_TAKE_PICTURE
                || requestCode == REQUEST_CODE_CAPTURE_AUDIO
                || requestCode == REQUEST_CODE_CAPTURE_VIDEO)
        {
            if (resultCode != RESULT_OK) {
                mCurrentPhoto = null;
                return;
            }
            Log.w(TAG, "onActivityResult: mCurrentPhoto " + mCurrentPhoto.getAbsolutePath() + " " + mCurrentPhoto.exists() + " " + mCurrentPhoto.length());
            Single<File> file = null;
            if (mCurrentPhoto == null || !mCurrentPhoto.exists() || mCurrentPhoto.length() == 0) {
                if (uri != null) {
                    file = AndroidFileUtils.getCacheFile(requireContext(), uri);
                }
            } else {
                file = Single.just(mCurrentPhoto);
            }
            mCurrentPhoto = null;
            if (file == null) {
                Toast.makeText(getActivity(), "Can't find picture", Toast.LENGTH_SHORT).show();
                return;
            }
            startFileSend(file.flatMapCompletable(this::sendFile));
        }
        // File download trough SAF
        else if (requestCode == ConversationFragment.REQUEST_CODE_SAVE_FILE) {
            if (resultCode == RESULT_OK &&  uri != null) {
                writeToFile(uri);
            }
        }
    }

    private void writeToFile(android.net.Uri data) {
        File input = new File(mCurrentFileAbsolutePath);
        if (requireContext().getContentResolver() != null)
            mCompositeDisposable.add(AndroidFileUtils.copyFileToUri(requireContext().getContentResolver(), input, data).
                    observeOn(AndroidSchedulers.mainThread()).
                    subscribe(() -> Toast.makeText(getContext(), R.string.file_saved_successfully, Toast.LENGTH_SHORT).show(),
                            error -> Toast.makeText(getContext(), R.string.generic_error, Toast.LENGTH_SHORT).show()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0, n = permissions.length; i < n; i++) {
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    presenter.cameraPermissionChanged(granted);
                    if (granted) {
                        if (requestCode == REQUEST_CODE_CAPTURE_VIDEO) {
                            sendVideoMessage();
                        } else if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
                            takePicture();
                        }
                    }
                    return;
                case Manifest.permission.RECORD_AUDIO:
                    if (granted) {
                        if (requestCode == REQUEST_CODE_CAPTURE_AUDIO) {
                            sendAudioMessage();
                        }
                    }
                    return;
                default:
                    break;
            }
        }
    }

    @Override
    public void addElement(Interaction element) {
        if (mLastRead != null && mLastRead.equals(element.getMessageId()))
            element.read();
        if (mAdapter.add(element))
            scrollToEnd();
        loading = false;
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
    public void setComposingStatus(Account.ComposingStatus composingStatus) {
        mAdapter.setComposingStatus(composingStatus);
        if (composingStatus == Account.ComposingStatus.Active)
            scrollToEnd();
    }

    @Override
    public void setLastDisplayed(Interaction interaction) {
        mAdapter.setLastDisplayed(interaction);
    }

    @Override
    public void acceptFile(String accountId, Uri conversationUri, DataTransfer transfer) {
        File cacheDir = requireContext().getCacheDir();
        long spaceLeft = AndroidFileUtils.getSpaceLeft(cacheDir.toString());
        if (spaceLeft == -1L || transfer.getTotalSize() > spaceLeft) {
            presenter.noSpaceLeft();
            return;
        }
        requireActivity().startService(new Intent(DRingService.ACTION_FILE_ACCEPT, ConversationPath.toUri(accountId, conversationUri), requireContext(), DRingService.class)
                .putExtra(DRingService.KEY_MESSAGE_ID, transfer.getMessageId())
                .putExtra(DRingService.KEY_TRANSFER_ID, transfer.getFileId()));
    }

    @Override
    public void refuseFile(String accountId, Uri conversationUri, DataTransfer transfer) {
        requireActivity().startService(new Intent(DRingService.ACTION_FILE_CANCEL, ConversationPath.toUri(accountId, conversationUri), requireContext(), DRingService.class)
                .putExtra(DRingService.KEY_MESSAGE_ID, transfer.getMessageId())
                .putExtra(DRingService.KEY_TRANSFER_ID, transfer.getFileId()));
    }

    @Override
    public void shareFile(File path, String displayName) {
        Context c = getContext();
        if (c == null)
            return;
        android.net.Uri fileUri = null;
        try {
            fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path, displayName);
        } catch (IllegalArgumentException e) {
            Log.e("File Selector", "The selected file can't be shared: " + path.getName());
        }
        if (fileUri != null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String type = c.getContentResolver().getType(fileUri.buildUpon().appendPath(displayName).build());
            sendIntent.setDataAndType(fileUri, type);
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            startActivity(Intent.createChooser(sendIntent, null));
        }
    }

    @Override
    public void openFile(File path, String displayName) {
        Context c = getContext();
        if (c == null)
            return;
        android.net.Uri fileUri = null;
        try {
            fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path, displayName);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "The selected file can't be shared: " + path.getName());
        }
        if (fileUri != null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_VIEW);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String type = c.getContentResolver().getType(fileUri.buildUpon().appendPath(displayName).build());
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
    public void onStart() {
        super.onStart();
        presenter.resume(mIsBubble);
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.pause();
    }

    @Override
    public void onPause() {
        super.onPause();
        //presenter.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        //presenter.resume(mIsBubble);
    }

    @Override
    public void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (!isVisible()) {
            return;
        }
        inflater.inflate(R.menu.conversation_actions, menu);
        mAudioCallBtn = menu.findItem(R.id.conv_action_audiocall);
        mVideoCallBtn = menu.findItem(R.id.conv_action_videocall);
    }

    public void openContact() {
        presenter.openContact();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            startActivity(new Intent(getActivity(), HomeActivity.class));
            return true;
        } else if (itemId == R.id.conv_action_audiocall) {
            presenter.goToCall(true);
            return true;
        } else if (itemId == R.id.conv_action_videocall) {
            presenter.goToCall(false);
            return true;
        } else if (itemId == R.id.conv_contact_details) {
            presenter.openContact();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void initPresenter(@NonNull ConversationPresenter presenter) {
        ConversationPath path = ConversationPath.fromBundle(getArguments());
        mIsBubble = getArguments().getBoolean(NotificationServiceImpl.EXTRA_BUBBLE);
        Log.w(TAG, "initPresenter " + path);
        if (path == null)
            return;

        Uri uri = path.getConversationUri();
        mAdapter = new ConversationAdapter(this, presenter);
        presenter.init(uri, path.getAccountId());
        try {
            mPreferences = SharedPreferencesServiceImpl.getConversationPreferences(requireContext(), path.getAccountId(), uri);
            mPreferences.registerOnSharedPreferenceChangeListener(this);
            presenter.setConversationColor(mPreferences.getInt(KEY_PREFERENCE_CONVERSATION_COLOR, getResources().getColor(R.color.color_primary_light)));
            presenter.setConversationSymbol(mPreferences.getString(KEY_PREFERENCE_CONVERSATION_SYMBOL, getResources().getText(R.string.conversation_default_emoji).toString()));
            mLastRead = mPreferences.getString(KEY_PREFERENCE_CONVERSATION_LAST_READ, null);
        } catch (Exception e) {
            Log.e(TAG, "Can't load conversation preferences");
        }

        if (locationServiceConnection == null) {
            locationServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.w(TAG, "onServiceConnected");
                    LocationSharingService.LocalBinder binder = (LocationSharingService.LocalBinder) service;
                    LocationSharingService locationService = binder.getService();
                    ConversationPath path = new ConversationPath(presenter.getPath());
                    if (locationService.isSharing(path)) {
                        showMap(path.getAccountId(), uri.getUri(), false);
                    }
                    try {
                        requireContext().unbindService(locationServiceConnection);
                    } catch (Exception e) {
                        Log.w(TAG, "Error unbinding service", e);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.w(TAG, "onServiceDisconnected");
                    locationServiceConnection = null;
                }
            };

            Log.w(TAG, "bindService");
            requireContext().bindService(new Intent(requireContext(), LocationSharingService.class), locationServiceConnection, 0);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case KEY_PREFERENCE_CONVERSATION_COLOR:
                presenter.setConversationColor(prefs.getInt(KEY_PREFERENCE_CONVERSATION_COLOR, getResources().getColor(R.color.color_primary_light)));
                break;
            case KEY_PREFERENCE_CONVERSATION_SYMBOL:
                presenter.setConversationSymbol(prefs.getString(KEY_PREFERENCE_CONVERSATION_SYMBOL, getResources().getText(R.string.conversation_default_emoji).toString()));
                break;
        }
    }

    @Override
    public void updateContact(Contact contact) {
        String contactKey = contact.getPrimaryNumber();
        AvatarDrawable a = mSmallParticipantAvatars.get(contactKey);
        if (a != null) {
            a.update(contact);
            mParticipantAvatars.get(contactKey).update(contact);
            mAdapter.setPhoto();
        } else {
            mCompositeDisposable.add(AvatarFactory.getAvatar(requireContext(), contact, true)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(avatar -> {
                        mParticipantAvatars.put(contactKey, (AvatarDrawable) avatar);
                        mSmallParticipantAvatars.put(contactKey, new AvatarDrawable.Builder()
                                .withContact(contact)
                                .withCircleCrop(true)
                                .withPresence(false)
                                .build(requireContext()));
                        mAdapter.setPhoto();
                    }));
        }
    }

    @Override
    public void displayContact(Conversation conversation) {
        mCompositeDisposable.add(AvatarFactory.getAvatar(requireContext(), conversation, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d -> {
                    mConversationAvatar = (AvatarDrawable) d;
                    mParticipantAvatars.put(conversation.getUri().getRawRingId(), new AvatarDrawable((AvatarDrawable) d));
                    setupActionbar(conversation);
                }));
    }

    @Override
    public void displayOnGoingCallPane(final boolean display) {
        binding.ongoingcallPane.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void displayNumberSpinner(final Conversation conversation, final Uri number) {
        binding.numberSelector.setVisibility(View.VISIBLE);
        //binding.numberSelector.setAdapter(new NumberAdapter(getActivity(), conversation.getContact(), false));
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
    public void goToAddContact(Contact contact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(contact), REQ_ADD_CONTACT);
    }

    @Override
    public void goToCallActivity(String conferenceId) {
        startActivity(new Intent(Intent.ACTION_VIEW)
                .setClass(requireActivity().getApplicationContext(), CallActivity.class)
                .putExtra(NotificationService.KEY_CALL_ID, conferenceId));
    }

    @Override
    public void goToContactActivity(String accountId, Uri uri) {
        Toolbar toolbar = requireActivity().findViewById(R.id.main_toolbar);
        ImageView logo = toolbar.findViewById(R.id.contact_image);
        startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, uri))
                        .setClass(requireActivity().getApplicationContext(), ContactDetailsActivity.class),
                ActivityOptions.makeSceneTransitionAnimation(getActivity(), logo, "conversationIcon").toBundle());
    }

    @Override
    public void goToCallActivityWithResult(String accountId, Uri conversationUri, Uri contactUri, boolean audioOnly) {
        Intent intent = new Intent(Intent.ACTION_CALL)
                .setClass(requireContext(), CallActivity.class)
                .putExtras(ConversationPath.toBundle(accountId, conversationUri))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.getUri())
                .putExtra(CallFragment.KEY_AUDIO_ONLY, audioOnly);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
    }

    private void setupActionbar(Conversation conversation) {
        if (!isVisible()) {
            return;
        }
        Activity activity = requireActivity();
        String displayName = conversation.getTitle();
        String identity = conversation.getUriTitle();

        Toolbar toolbar = activity.findViewById(R.id.main_toolbar);
        TextView title = toolbar.findViewById(R.id.contact_title);
        TextView subtitle = toolbar.findViewById(R.id.contact_subtitle);
        ImageView logo = toolbar.findViewById(R.id.contact_image);

        logo.setImageDrawable(mConversationAvatar);
        logo.setVisibility(View.VISIBLE);
        title.setText(displayName);
        title.setTextSize(15);
        title.setTypeface(null, Typeface.NORMAL);

        if (identity != null && !identity.equals(displayName)) {
            subtitle.setText(identity);
            subtitle.setVisibility(View.VISIBLE);
            /*RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) title.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_TOP, R.id.contact_image);
            title.setLayoutParams(params);*/
        } else {
            subtitle.setText("");
            subtitle.setVisibility(View.GONE);

            /*RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) title.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_TOP);
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            title.setLayoutParams(params);*/
        }
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
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
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
        currentBottomView = binding.unknownContactPrompt;
        requireActivity().invalidateOptionsMenu();
        updateListPadding();
    }

    @Override
    public void switchToIncomingTrustRequestView(String contactDisplayName) {
        binding.cvMessageInput.setVisibility(View.GONE);
        binding.unknownContactPrompt.setVisibility(View.GONE);
        binding.trustRequestPrompt.setVisibility(View.VISIBLE);
        binding.tvTrustRequestMessage.setText(String.format(getString(R.string.message_contact_not_trusted_yet), contactDisplayName));
        binding.trustRequestMessageLayout.setVisibility(View.VISIBLE);
        currentBottomView = binding.trustRequestPrompt;
        requireActivity().invalidateOptionsMenu();
        updateListPadding();
    }

    @Override
    public void switchToConversationView() {
        binding.cvMessageInput.setVisibility(View.VISIBLE);
        binding.unknownContactPrompt.setVisibility(View.GONE);
        binding.trustRequestPrompt.setVisibility(View.GONE);
        binding.trustRequestMessageLayout.setVisibility(View.GONE);
        currentBottomView = binding.cvMessageInput;
        requireActivity().invalidateOptionsMenu();
        updateListPadding();
    }

    @Override
    public void switchToSyncingView() {
        binding.cvMessageInput.setVisibility(View.GONE);
        binding.unknownContactPrompt.setVisibility(View.GONE);
        binding.trustRequestPrompt.setVisibility(View.GONE);
        binding.trustRequestMessageLayout.setVisibility(View.VISIBLE);
        binding.tvTrustRequestMessage.setText("Syncing conversation...");
        currentBottomView = null;
        requireActivity().invalidateOptionsMenu();
        updateListPadding();
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
            binding.pbDataTransfer.setVisibility(View.VISIBLE);
        } else {
            binding.btnTakePicture.setVisibility(View.VISIBLE);
            binding.pbDataTransfer.setVisibility(View.GONE);
        }
    }

    public void handleShareIntent(Intent intent) {
        Log.w(TAG, "handleShareIntent " + intent);

        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
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
        } else if (Intent.ACTION_VIEW.equals(action)) {
            ConversationPath path = ConversationPath.fromIntent(intent);
            if (path != null && intent.getBooleanExtra(EXTRA_SHOW_MAP, false)) {
                shareLocation();
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

        try {
            //Use Android Storage File Access to download the file
            Intent downloadFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            downloadFileIntent.setType(AndroidFileUtils.getMimeTypeFromExtension(file.getExtension()));
            downloadFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            downloadFileIntent.putExtra(Intent.EXTRA_TITLE,file.getDisplayName());

            startActivityForResult(downloadFileIntent, ConversationFragment.REQUEST_CODE_SAVE_FILE);
        } catch (Exception e) {
            Log.i(TAG, "No app detected for saving files.");
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            writeToFile(android.net.Uri.fromFile(new File(directory, file.getDisplayName())));
        }
    }

    @Override
    public void displayNetworkErrorPanel() {
        if (binding != null) {
            binding.errorMsgPane.setVisibility(View.VISIBLE);
            binding.errorMsgPane.setOnClickListener(null);
            binding.errorMsgPane.setText(R.string.error_no_network);
        }
    }

    @Override
    public void displayAccountOfflineErrorPanel() {
        if (binding != null) {
            binding.errorMsgPane.setVisibility(View.VISIBLE);
            binding.errorMsgPane.setOnClickListener(null);
            binding.errorMsgPane.setText(R.string.error_account_offline);
            for ( int idx = 0 ; idx < binding.btnContainer.getChildCount() ; idx++) {
                binding.btnContainer.getChildAt(idx).setEnabled(false);
            }
        }
    }

    @Override
    public void setReadIndicatorStatus(boolean show) {
        if (mAdapter != null) {
            mAdapter.setReadIndicatorStatus(show);
        }
    }

    @Override
    public void updateLastRead(String last) {
        Log.w(TAG, "Updated last read " + mLastRead);
        mLastRead = last;
        if (mPreferences != null)
            mPreferences.edit().putString(KEY_PREFERENCE_CONVERSATION_LAST_READ, last).apply();
    }

    @Override
    public void hideErrorPanel() {
        if (binding != null) {
            binding.errorMsgPane.setVisibility(View.GONE);
        }
    }

}
