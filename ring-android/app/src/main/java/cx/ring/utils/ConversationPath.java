package cx.ring.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Objects;

import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Interaction;

public class ConversationPath {
    private final String accountId;
    private final String contactId;
    public ConversationPath(String account, String contact) {
        accountId = account;
        contactId = contact;
    }

    public ConversationPath(@NonNull Tuple<String, String> path) {
        accountId = path.first;
        contactId = path.second;
    }

    public String getAccountId() {
        return accountId;
    }
    public String getContactId() {
        return contactId;
    }
    public Uri toUri() {
        return toUri(accountId, contactId);
    }
    public static Uri toUri(String accountId, String contactId) {
        Uri.Builder builder = ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon();
        builder = builder.appendEncodedPath(accountId);
        builder = builder.appendEncodedPath(contactId);
        return builder.build();
    }
    public static Uri toUri(String accountId, @NonNull cx.ring.model.Uri contactUri) {
        Uri.Builder builder = ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon();
        builder = builder.appendEncodedPath(accountId);
        builder = builder.appendEncodedPath(contactUri.getUri());
        return builder.build();
    }
    public static Uri toUri(@NonNull Interaction interaction) {
        return toUri(interaction.getAccount(), new cx.ring.model.Uri(interaction.getConversation().getParticipant()));
    }

    public Bundle toBundle() {
        return toBundle(accountId, contactId);
    }
    public void toBundle(Bundle bundle) {
        bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
        bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);
    }
    public static Bundle toBundle(String accountId, String contactId) {
        Bundle bundle = new Bundle();
        bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
        bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);
        return bundle;
    }

    public static ConversationPath fromUri(Uri uri) {
        if (uri != null && uri.toString().startsWith(ContentUriHandler.CONVERSATION_CONTENT_URI.toString())) {
            List<String> pathSegments = uri.getPathSegments();
            if (pathSegments.size() > 2) {
                return new ConversationPath(pathSegments.get(pathSegments.size() - 2), pathSegments.get(pathSegments.size() - 1));
            }
        }
        return null;
    }

    @Contract("null -> null")
    public static ConversationPath fromBundle(@Nullable Bundle bundle) {
        if (bundle != null) {
            String accountId = bundle.getString(ConversationFragment.KEY_ACCOUNT_ID);
            String contactId = bundle.getString(ConversationFragment.KEY_CONTACT_RING_ID);
            if (accountId != null && contactId != null) {
                return new ConversationPath(accountId, contactId);
            }
        }
        return null;
    }

    @Contract("null -> null")
    public static ConversationPath fromIntent(@Nullable Intent intent) {
        if (intent != null) {
            Uri uri = intent.getData();
            ConversationPath conversationPath = fromUri(uri);
            if (conversationPath != null)
                return conversationPath;
            return fromBundle(intent.getExtras());
        }
        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != getClass())
            return false;
        ConversationPath o = (ConversationPath) obj;
        return Objects.equals(o.accountId, accountId)
                && Objects.equals(o.contactId, contactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, contactId);
    }
}
