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
package cx.ring.client;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import net.jami.facades.ConversationFacade;
import net.jami.model.Call;
import net.jami.model.Conference;
import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.model.Uri;
import net.jami.services.AccountService;
import net.jami.services.NotificationService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.views.AvatarFactory;
import cx.ring.databinding.ActivityContactDetailsBinding;
import cx.ring.databinding.ItemContactActionBinding;
import cx.ring.databinding.ItemContactHorizontalBinding;
import cx.ring.fragments.CallFragment;
import cx.ring.fragments.ConversationFragment;
import cx.ring.services.SharedPreferencesServiceImpl;
import cx.ring.utils.ConversationPath;
import cx.ring.views.AvatarDrawable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

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

    interface IContactAction {
        void onAction();
    }

    static class ContactAction {
        @DrawableRes
        final int icon;
        final Single<Drawable> drawable;

        final CharSequence title;
        final IContactAction callback;

        int iconTint;
        CharSequence iconSymbol;

        ContactAction(@DrawableRes int i, int tint, CharSequence t, IContactAction cb) {
            icon = i;
            iconTint = tint;
            title = t;
            callback = cb;
            drawable = null;
        }

        ContactAction(@DrawableRes int i, CharSequence t, IContactAction cb) {
            icon = i;
            iconTint = Color.BLACK;
            title = t;
            callback = cb;
            drawable = null;
        }
        ContactAction(Single<Drawable> d, CharSequence t, IContactAction cb) {
            drawable = d;
            icon = 0;
            iconTint = Color.BLACK;
            title = t;
            callback = cb;
        }

        void setIconTint(int tint) {
            iconTint = tint;
        }

        void setSymbol(CharSequence t) {
            iconSymbol = t;
        }
    }

    static class ContactActionView extends RecyclerView.ViewHolder {
        final ItemContactActionBinding binding;
        IContactAction callback;
        final CompositeDisposable disposable = new CompositeDisposable();

        ContactActionView(@NonNull ItemContactActionBinding b, CompositeDisposable parentDisposable) {
            super(b.getRoot());
            binding = b;
            parentDisposable.add(disposable);
            itemView.setOnClickListener(view -> {
                try {
                    if (callback != null)
                        callback.onAction();
                } catch (Exception e) {
                    Log.w(TAG, "Error performing action", e);
                }
            });
        }
    }

    private static class ContactActionAdapter extends RecyclerView.Adapter<ContactActionView> {
        private final ArrayList<ContactAction> actions = new ArrayList<>();
        private final CompositeDisposable disposable;

        private ContactActionAdapter(CompositeDisposable disposable) {
            this.disposable = disposable;
        }

        @NonNull
        @Override
        public ContactActionView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            ItemContactActionBinding itemBinding = ItemContactActionBinding.inflate(layoutInflater, parent, false);
            return new ContactActionView(itemBinding, disposable);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactActionView holder, int position) {
            ContactAction action = actions.get(position);
            holder.disposable.clear();
            if (action.drawable != null) {
                holder.disposable.add(action.drawable.subscribe(holder.binding.actionIcon::setBackground));
            } else {
                holder.binding.actionIcon.setBackgroundResource(action.icon);
                holder.binding.actionIcon.setText(action.iconSymbol);
                if (action.iconTint != Color.BLACK)
                    ViewCompat.setBackgroundTintList(holder.binding.actionIcon, ColorStateList.valueOf(action.iconTint));
            }
            holder.binding.actionTitle.setText(action.title);
            holder.callback = action.callback;
        }

        @Override
        public void onViewRecycled(@NonNull ContactActionView holder) {
            holder.disposable.clear();
            holder.binding.actionIcon.setBackground(null);
        }

        @Override
        public int getItemCount() {
            return actions.size();
        }
    }

    static class ContactView extends RecyclerView.ViewHolder {
        final ItemContactHorizontalBinding binding;
        IContactAction callback;
        final CompositeDisposable disposable = new CompositeDisposable();

        ContactView(@NonNull ItemContactHorizontalBinding b, CompositeDisposable parentDisposable) {
            super(b.getRoot());
            binding = b;
            parentDisposable.add(disposable);
            itemView.setOnClickListener(view -> {
                try {
                    if (callback != null)
                        callback.onAction();
                } catch (Exception e) {
                    Log.w(TAG, "Error performing action", e);
                }
            });
        }
    }
    private static class ContactViewAdapter extends RecyclerView.Adapter<ContactView> {
        private final List<Contact> contacts;
        private final CompositeDisposable disposable;
        interface ContactCallback {
            void onContactClicked(Contact contact);
        }
        private final ContactCallback callback;

        private ContactViewAdapter(CompositeDisposable disposable, List<Contact> contacts, ContactCallback cb) {
            this.disposable = disposable;
            this.contacts = contacts;
            this.callback = cb;
        }

        @NonNull
        @Override
        public ContactView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            ItemContactHorizontalBinding itemBinding = ItemContactHorizontalBinding.inflate(layoutInflater, parent, false);
            return new ContactView(itemBinding, disposable);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactView holder, int position) {
            Contact contact = contacts.get(position);
            holder.disposable.clear();
            holder.disposable.add(AvatarFactory.getAvatar(holder.itemView.getContext(), contact, false).subscribe(holder.binding.photo::setImageDrawable));
            holder.binding.displayName.setText(contact.isUser() ? holder.itemView.getContext().getText(R.string.conversation_info_contact_you) : contact.getDisplayName());
            holder.itemView.setOnClickListener(v -> callback.onContactClicked(contact));
        }

        @Override
        public void onViewRecycled(@NonNull ContactView holder) {
            holder.disposable.clear();
            holder.binding.photo.setImageDrawable(null);
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }
    }

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();
    private final ContactActionAdapter adapter = new ContactActionAdapter(mDisposableBag);

    private ContactAction colorAction;
    private ContactAction symbolAction;
    private int colorActionPosition;
    private int symbolActionPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConversationPath path = ConversationPath.fromIntent(getIntent());
        if (path == null) {
            finish();
            return;
        }
        binding = ActivityContactDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        JamiApplication.getInstance().getInjectionComponent().inject(this);

        //CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.toolbar_layout);
        //collapsingToolbarLayout.setTitle("");

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //FloatingActionButton fab = binding.sendMessage;
        //fab.setOnClickListener(view -> goToConversationActivity(mConversation.getAccountId(), mConversation.getUri()));

        colorActionPosition = 0;
        symbolActionPosition = 1;

        Conversation conversation = mConversationFacade
                .startConversation(path.getAccountId(), path.getConversationUri())
                .blockingGet();

        mConversation = conversation;
        mPreferences = SharedPreferencesServiceImpl.getConversationPreferences(this, conversation.getAccountId(), conversation.getUri());
        binding.contactImage.setImageDrawable(
                new AvatarDrawable.Builder()
                        .withConversation(conversation)
                        .withPresence(false)
                        .withCircleCrop(true)
                        .build(this)
        );

        /*Map<String, String> details = Ringservice.getCertificateDetails(conversation.getContact().getUri().getRawRingId());
        for (Map.Entry<String, String> e : details.entrySet()) {
            Log.w(TAG, e.getKey() + " -> " + e.getValue());
        }*/

        @StringRes int infoString = conversation.isSwarm()
                ? (conversation.getMode() == Conversation.Mode.OneToOne
                ? R.string.conversation_type_private
                : R.string.conversation_type_group)
                : R.string.conversation_type_contact;
        /*@DrawableRes int infoIcon = conversation.isSwarm()
                ? (conversation.getMode() == Conversation.Mode.OneToOne
                ? R.drawable.baseline_person_24
                : R.drawable.baseline_group_24)
                : R.drawable.baseline_person_24;*/
        //adapter.actions.add(new ContactAction(R.drawable.baseline_info_24, getText(infoString), () -> {}));
        binding.conversationType.setText(infoString);
        //binding.conversationType.setCompoundDrawables(getDrawable(infoIcon), null, null, null);

        colorAction = new ContactAction(R.drawable.item_color_background, 0, getText(R.string.conversation_preference_color), () -> {
            ColorChooserBottomSheet frag = new ColorChooserBottomSheet();
            frag.setCallback(color -> {
                /*collapsingToolbarLayout.setBackgroundColor(color);
                collapsingToolbarLayout.setContentScrimColor(color);
                collapsingToolbarLayout.setStatusBarScrimColor(color);*/
                colorAction.setIconTint(color);
                adapter.notifyItemChanged(colorActionPosition);
                mPreferences.edit().putInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, color).apply();
            });
            frag.show(getSupportFragmentManager(), "colorChooser");
        });
        int color = mPreferences.getInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, getResources().getColor(R.color.color_primary_light));
        colorAction.setIconTint(color);
        /*collapsingToolbarLayout.setBackgroundColor(color);
        collapsingToolbarLayout.setTitle(conversation.getTitle());
        collapsingToolbarLayout.setContentScrimColor(color);
        collapsingToolbarLayout.setStatusBarScrimColor(color);*/
        adapter.actions.add(colorAction);

        symbolAction = new ContactAction(0, getText(R.string.conversation_preference_emoji), () -> {
            EmojiChooserBottomSheet frag = new EmojiChooserBottomSheet();
            frag.setCallback(s -> {
                symbolAction.setSymbol(s);
                adapter.notifyItemChanged(symbolActionPosition);
                mPreferences.edit().putString(ConversationFragment.KEY_PREFERENCE_CONVERSATION_SYMBOL, s).apply();
            });
            frag.show(getSupportFragmentManager(), "colorChooser");
        });
        symbolAction.setSymbol(mPreferences.getString(ConversationFragment.KEY_PREFERENCE_CONVERSATION_SYMBOL, getResources().getString(R.string.conversation_default_emoji)));
        adapter.actions.add(symbolAction);

        String conversationUri = conversation.isSwarm() ? conversation.getUri().toString() : conversation.getUriTitle();
        if (conversation.getContacts().size() <= 2) {
            Contact contact = conversation.getContact();
            adapter.actions.add(new ContactAction(R.drawable.baseline_call_24, getText(R.string.ab_action_audio_call), () ->
                    goToCallActivity(conversation, contact.getUri(), true)));
            adapter.actions.add(new ContactAction(R.drawable.baseline_videocam_24, getText(R.string.ab_action_video_call), () ->
                    goToCallActivity(conversation, contact.getUri(), false)));
            if (!conversation.isSwarm()) {
                adapter.actions.add(new ContactAction(R.drawable.baseline_clear_all_24, getText(R.string.conversation_action_history_clear), () ->
                        new MaterialAlertDialogBuilder(ContactDetailsActivity.this)
                                .setTitle(R.string.clear_history_dialog_title)
                                .setMessage(R.string.clear_history_dialog_message)
                                .setPositiveButton(R.string.conversation_action_history_clear, (b, i) -> {
                                    mConversationFacade.clearHistory(conversation.getAccountId(), contact.getUri()).subscribe();
                                    Snackbar.make(binding.getRoot(), R.string.clear_history_completed, Snackbar.LENGTH_LONG).show();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create()
                                .show()));
            }
            adapter.actions.add(new ContactAction(R.drawable.baseline_block_24, getText(R.string.conversation_action_block_this), () ->
                    new MaterialAlertDialogBuilder(ContactDetailsActivity.this)
                            .setTitle(getString(R.string.block_contact_dialog_title, conversationUri))
                            .setMessage(getString(R.string.block_contact_dialog_message, conversationUri))
                            .setPositiveButton(R.string.conversation_action_block_this, (b, i) -> {
                                mAccountService.removeContact(conversation.getAccountId(), contact.getUri().getRawRingId(), true);
                                Toast.makeText(getApplicationContext(), getString(R.string.block_contact_completed, conversationUri), Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show()));
        }
        getSupportActionBar().setTitle(conversation.getTitle());
        //new ContactAction(conversation.isSwarm() ? R.drawable.baseline_group_24 : R.drawable.baseline_person_24, conversationUri, () -> {});
        binding.conversationId.setText(conversationUri);
        binding.infoCard.setOnClickListener(v -> copyAndShow(path.getConversationId()));
        //adapter.actions.add(contactAction);
        binding.contactActionList.setAdapter(adapter);

        binding.contactListLayout.setVisibility(conversation.isSwarm() ? View.VISIBLE : View.GONE);
        if (conversation.isSwarm()) {
            binding.contactList.setAdapter(new ContactViewAdapter(mDisposableBag, conversation.getContacts(), contact -> copyAndShow(contact.getUri().getRawUriString())));
        }
    }

    void copyAndShow(String toCopy) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(getText(R.string.clip_contact_uri), toCopy));
            Snackbar.make(binding.getRoot(), getString(R.string.conversation_action_copied_peer_number_clipboard, toCopy), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finishAfterTransition();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        adapter.actions.clear();
        mDisposableBag.dispose();
        super.onDestroy();
        colorAction = null;
        mPreferences = null;
        binding = null;
    }

    private void goToCallActivity(Conversation conversation, Uri contactUri, boolean hasVideo) {
        Conference conf = mConversation.getCurrentCall();
        if (conf != null
                && !conf.getParticipants().isEmpty()
                && conf.getParticipants().get(0).getCallStatus() != Call.CallStatus.INACTIVE
                && conf.getParticipants().get(0).getCallStatus() != Call.CallStatus.FAILURE) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setClass(getApplicationContext(), CallActivity.class)
                    .putExtra(NotificationService.KEY_CALL_ID, conf.getId()));
        } else {
            Intent intent = new Intent(Intent.ACTION_CALL)
                    .setClass(getApplicationContext(), CallActivity.class)
                    .putExtras(ConversationPath.toBundle(conversation))
                    .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.getUri())
                    .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo);
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
        }
    }

    private void goToConversationActivity(String accountId, Uri conversationUri) {
        startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, conversationUri), getApplicationContext(), ConversationActivity.class));
    }
}
