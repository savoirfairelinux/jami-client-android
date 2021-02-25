package cx.ring.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Interaction;

public class ConversationPath {
    private final String accountId;
    private final String conversationId;
    public ConversationPath(String account, String contact) {
        accountId = account;
        conversationId = contact;
    }

    public ConversationPath(@NonNull Tuple<String, String> path) {
        accountId = path.first;
        conversationId = path.second;
    }

    public String getAccountId() {
        return accountId;
    }
    public String getConversationId() {
        return conversationId;
    }

    @Deprecated
    public String getContactId() {
        return conversationId;
    }

    public Uri toUri() {
        return toUri(accountId, conversationId);
    }
    public static Uri toUri(String accountId, String contactId) {
        return ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
                .appendEncodedPath(accountId)
                .appendEncodedPath(contactId)
                .build();
    }
    public static Uri toUri(String accountId, @NonNull cx.ring.model.Uri conversationUri) {
        return ContentUriHandler.CONVERSATION_CONTENT_URI.buildUpon()
                .appendEncodedPath(accountId)
                .appendEncodedPath(conversationUri.getUri())
                .build();
    }
    public static Uri toUri(@NonNull Interaction interaction) {
        return toUri(interaction.getAccount(), new cx.ring.model.Uri(interaction.getConversation().getParticipant()));
    }

    public Bundle toBundle() {
        return toBundle(accountId, conversationId);
    }
    public void toBundle(Bundle bundle) {
        bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, conversationId);
        bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);
    }
    public static Bundle toBundle(String accountId, String uri) {
        Bundle bundle = new Bundle();
        bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, uri);
        bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);
        return bundle;
    }
    public static Bundle toBundle(String accountId, cx.ring.model.Uri uri) {
        return toBundle(accountId, uri.getUri());
    }

    public static String toKey(String accountId, String uri) {
        return TextUtils.join(",", Arrays.asList(accountId, uri));
    }

    public static ConversationPath fromKey(String key) {
        if (key != null) {
            String[] keys = TextUtils.split(key, ",");
            String accountId = keys[0];
            String contactId = keys[1];
            if (accountId != null && contactId != null) {
                return new ConversationPath(accountId, contactId);
            }
        }
        return null;
    }

    public static ConversationPath fromUri(Uri uri) {
        if (uri == null)
            return null;
        if (ContentUriHandler.SCHEME_TV.equals(uri.getScheme()) || uri.toString().startsWith(ContentUriHandler.CONVERSATION_CONTENT_URI.toString())) {
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
                && Objects.equals(o.conversationId, conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, conversationId);
    }

    public cx.ring.model.Uri getConversationUri() {
        return new cx.ring.model.Uri(conversationId);
    }
}
