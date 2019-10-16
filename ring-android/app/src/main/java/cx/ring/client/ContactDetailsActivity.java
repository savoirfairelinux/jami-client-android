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
package cx.ring.client;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.core.widget.ImageViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.ActivityContactDetailsBinding;
import cx.ring.databinding.ItemContactActionBinding;
import cx.ring.facades.ConversationFacade;
import cx.ring.fragments.CallFragment;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.views.AvatarDrawable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class ContactDetailsActivity extends AppCompatActivity {
    private static final String TAG = ContactDetailsActivity.class.getName();

    @Inject
    @Singleton
    ConversationFacade mConversationFacade;

    @Inject
    @Singleton
    AccountService mAccountService;

    private SharedPreferences mPreferences;
    private ActivityContactDetailsBinding binding;
    private Conversation mConversation;
    private CallContact mContact = null;

    interface IContactAction {
        void onAction();
    }

    class ContactAction {
        final int icon;
        int iconTint;
        CharSequence title;
        final IContactAction callback;

        ContactAction(int i, int tint, CharSequence t, IContactAction cb) {
            icon = i;
            iconTint = tint;
            title = t;
            callback = cb;
        }
        ContactAction(int i, CharSequence t, IContactAction cb) {
            icon = i;
            iconTint = Color.BLACK;
            title = t;
            callback = cb;
        }

        void setIconTint(int tint) {
            iconTint = tint;
        }
        void setTitle(CharSequence t) { title = t; }
    }

    class ContactActionView extends RecyclerView.ViewHolder {
        final ItemContactActionBinding binding;
        IContactAction callback;
        ContactActionView(@NonNull ItemContactActionBinding b) {
            super(b.getRoot());
            binding = b;
            itemView.setOnClickListener(view -> {
                try {
                    if (callback != null)
                        callback.onAction();
                } catch (Exception e) {
                    Log.w(TAG, "Error performing action" ,e);
                }
            });
        }
    }

    class ContactActionAdapter extends RecyclerView.Adapter<ContactActionView> {
        private final ArrayList<ContactAction> actions = new ArrayList<>();

        @NonNull
        @Override
        public ContactActionView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            ItemContactActionBinding itemBinding = ItemContactActionBinding.inflate(layoutInflater, parent, false);
            return new ContactActionView(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactActionView holder, int position) {
            ContactAction action = actions.get(position);
            holder.binding.actionIcon.setImageResource(action.icon);
            if (action.iconTint != Color.BLACK)
                ImageViewCompat.setImageTintList(holder.binding.actionIcon, ColorStateList.valueOf(action.iconTint));
            holder.binding.actionTitle.setText(action.title);
            holder.callback = action.callback;
        }

        @Override
        public int getItemCount() {
            return actions.size();
        }
    }

    private final ContactActionAdapter adapter = new ContactActionAdapter();
    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    private ContactAction colorAction;
    private ContactAction contactAction;
    private int colorActionPosition;
    private int contactIdPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_details);
        JamiApplication.getInstance().getRingInjectionComponent().inject(this);

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle("");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> goToConversationActivity(mConversation.getAccountId(), mContact.getPrimaryNumber()));

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            List<String> segments = uri.getPathSegments();
            if (segments.size() >= 3) {
                String account = segments.get(1);
                String contactUri = segments.get(2);
                mDisposableBag.add(mConversationFacade
                        .startConversation(account, new cx.ring.model.Uri(contactUri))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(conversation -> {
                            CallContact contact = conversation.getContact();
                            mPreferences = getSharedPreferences(conversation.getAccountId() + "_" + conversation.getContact().getPrimaryNumber(), Context.MODE_PRIVATE);
                            int color = mPreferences.getInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, getResources().getColor(R.color.color_primary_light));
                            colorAction.setIconTint(color);
                            adapter.notifyItemChanged(colorActionPosition);
                            contactAction.setTitle(contact.getRingUsername());
                            adapter.notifyItemChanged(contactIdPosition);
                            collapsingToolbarLayout.setBackgroundColor(color);
                            collapsingToolbarLayout.setTitle(contact.getDisplayName());
                            collapsingToolbarLayout.setContentScrimColor(color);
                            collapsingToolbarLayout.setStatusBarScrimColor(color);
                            //collapsingToolbarLayout.setCollapsedTitleTextColor();
                            binding.contactImage.setImageDrawable(new AvatarDrawable(this, contact, false));
                            mConversation = conversation;
                            mContact = contact;
                        }));

                colorAction = new ContactAction(R.drawable.item_color_background, 0, "Choose color", () -> {
                    ColorChooserBottomSheet frag = new ColorChooserBottomSheet();
                    frag.setCallback(color -> {
                        collapsingToolbarLayout.setBackgroundColor(color);
                        collapsingToolbarLayout.setContentScrimColor(color);
                        collapsingToolbarLayout.setStatusBarScrimColor(color);
                        colorAction.setIconTint(color);
                        adapter.notifyItemChanged(colorActionPosition);
                        mPreferences.edit().putInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, color).apply();
                    });
                    frag.show(getSupportFragmentManager(), "colorChooser");
                });
                adapter.actions.add(colorAction);
                adapter.actions.add(new ContactAction(R.drawable.baseline_call_24, getText(R.string.ab_action_audio_call), () ->
                        goToCallActivity(mConversation.getAccountId(), mContact.getPrimaryNumber(), true)));
                adapter.actions.add(new ContactAction(R.drawable.baseline_videocam_24, getText(R.string.ab_action_video_call), () ->
                        goToCallActivity(mConversation.getAccountId(), mContact.getPrimaryNumber(), false)));
                adapter.actions.add(new ContactAction(R.drawable.baseline_clear_all_24, getText(R.string.conversation_action_history_clear), () ->
                        new MaterialAlertDialogBuilder(ContactDetailsActivity.this)
                                .setTitle(R.string.clear_history_dialog_title)
                                .setMessage(R.string.clear_history_dialog_message)
                                .setPositiveButton(R.string.conversation_action_history_clear, (b, i) -> {
                                    mConversationFacade.clearHistory(mConversation.getAccountId(), mContact.getPrimaryUri()).subscribe();
                                    Snackbar.make(binding.getRoot(), R.string.clear_history_completed, Snackbar.LENGTH_LONG).show();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create()
                                .show()));
                adapter.actions.add(new ContactAction(R.drawable.baseline_block_24, getText(R.string.conversation_action_block_this), () ->
                        new MaterialAlertDialogBuilder(ContactDetailsActivity.this)
                                .setTitle(getString(R.string.block_contact_dialog_title, contactAction.title))
                                .setMessage(getString(R.string.block_contact_dialog_message, contactAction.title))
                                .setPositiveButton(R.string.conversation_action_block_this, (b, i) -> {
                                    mAccountService.removeContact(mConversation.getAccountId(), mContact.getPrimaryUri().getRawRingId(), true);
                                    Toast.makeText(getApplicationContext(), getString(R.string.block_contact_completed, contactAction.title), Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create()
                                .show()));
                contactAction = new ContactAction(R.drawable.baseline_person_24, "", () -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText(getText(R.string.clip_contact_uri), contactAction.title));
                        Snackbar.make(binding.getRoot(), getString(R.string.conversation_action_copied_peer_number_clipboard, contactUri), Snackbar.LENGTH_LONG).show();
                    }
                });
                adapter.actions.add(contactAction);
                colorActionPosition = 0;
                contactIdPosition = adapter.actions.size() - 1;
                binding.contactActionList.setAdapter(adapter);
            }
        }
    }

    @Override
    protected void onDestroy() {
        adapter.actions.clear();
        mDisposableBag.clear();
        super.onDestroy();
        contactAction = null;
        colorAction = null;
        mPreferences = null;
        binding = null;
    }

    private void goToCallActivity(String accountId, String contactRingId, boolean audioOnly) {
        Conference conf = mConversation.getCurrentCall();

        if (conf != null
                && !conf.getParticipants().isEmpty()
                && conf.getParticipants().get(0).getCallStatus() != SipCall.CallStatus.INACTIVE
                && conf.getParticipants().get(0).getCallStatus() != SipCall.CallStatus.FAILURE) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setClass(getApplicationContext(), CallActivity.class)
                    .putExtra(NotificationService.KEY_CALL_ID, conf.getId()));
        } else {
            Intent intent = new Intent(Intent.ACTION_CALL)
                    .setClass(getApplicationContext(), CallActivity.class)
                    .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactRingId)
                    .putExtra(CallFragment.KEY_AUDIO_ONLY, audioOnly);
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
        }
    }

    private void goToConversationActivity(String accountId, String contactRingId) {
        startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, contactRingId), getApplicationContext(), ConversationActivity.class));
    }
}
