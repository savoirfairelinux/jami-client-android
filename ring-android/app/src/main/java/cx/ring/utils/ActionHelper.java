/*
 * Copyright (C) 2016 by Savoir-faire Linux
 * Author : Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.utils;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.adapters.NumberAdapter;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.Uri;

public class ActionHelper {

    public static final String TAG = ActionHelper.class.getSimpleName();
    public static final int ACTION_COPY = 0;
    public static final int ACTION_DELETE = 1;
    public static final int ACTION_BLOCK = 2;

    private ActionHelper() {
    }

    public static AlertDialog launchDeleteAction(final Context context,
                                                 final CallContact callContact,
                                                 final Conversation.ConversationActionCallback callback) {
        if (context == null) {
            Log.d(TAG, "launchDeleteAction: activity is null");
            return null;
        }

        if (callContact == null) {
            Log.d(TAG, "launchDeleteAction: conversation is null");
            return null;
        }

        return new AlertDialog.Builder(context)
                .setTitle(R.string.conversation_action_delete_this_title)
                .setMessage(R.string.conversation_action_delete_this_message)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    if (callback != null) {
                        callback.deleteConversation(callContact);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                    /* Terminate with no action */
                })
                .show();
    }

    public static void launchCopyNumberToClipboardFromContact(final Context context,
                                                              final CallContact callContact,
                                                              final Conversation.ConversationActionCallback callback) {
        if (callContact == null) {
            Log.d(TAG, "launchCopyNumberToClipboardFromContact: callContact is null");
            return;
        }

        if (context == null) {
            Log.d(TAG, "launchCopyNumberToClipboardFromContact: activity is null");
            return;
        }

        if (callContact.getPhones().isEmpty()) {
            Log.d(TAG, "launchCopyNumberToClipboardFromContact: no number to copy");
            return;
        }

        if (callContact.getPhones().size() == 1 && callback != null) {
            String number = callContact.getPhones().get(0).getNumber().toString();
            callback.copyContactNumberToClipboard(number);
        } else {
            final NumberAdapter adapter = new NumberAdapter(context, callContact, true);
            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.conversation_action_select_peer_number)
                    .setAdapter(adapter, (dialog, which) -> {
                        if (callback != null) {
                            Phone selectedPhone = (Phone) adapter.getItem(which);
                            callback.copyContactNumberToClipboard(selectedPhone.getNumber().toString());
                        }
                    })
                    .create();
            final int listViewSidePadding = (int) context
                    .getResources()
                    .getDimension(R.dimen.alert_dialog_side_padding_list_view);
            alertDialog.getListView().setPadding(listViewSidePadding, 0, listViewSidePadding, 0);
            alertDialog.show();
        }
    }

    public static void displayAddContactConfirmationDialog(final CallContact contact, final Context context) {
        if (context == null) {
            Log.d(TAG, "displayAddContactConfirmationDialog: context is null");
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.ab_action_contact_add_question)
                .setMessage(context.getString(R.string.add_call_contact_number_to_contacts,
                        contact.getDisplayName()))
                .setPositiveButton(R.string.ab_action_contact_add, (dialog, whichButton) -> {
                    Intent intent = getAddNumberIntentForContact(contact);
                    context.startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                    /* Terminate with no action */
                })
                .show();
    }

    public static Intent getAddNumberIntentForContact(CallContact contact) {
        final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);

        ArrayList<ContentValues> data = new ArrayList<>();
        ContentValues values = new ContentValues();

        Uri number = contact.getPhones().get(0).getNumber();
        if (number.isRingId()) {
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.Im.DATA, number.getRawUriString());
            values.put(ContactsContract.CommonDataKinds.Im.PROTOCOL, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
            values.put(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, "Ring");
        } else {
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS, number.getRawUriString());
        }
        data.add(values);
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        return intent;
    }

    public static void displayContact(Context context, CallContact contact) {
        if (context == null) {
            Log.d(TAG, "displayContact: context is null");
            return;
        }

        if (contact.getId() == CallContact.UNKNOWN_ID) {
            Log.d(TAG, "displayContact: contact is unknown");
            ActionHelper.displayAddContactConfirmationDialog(contact, context);
        } else {
            Log.d(TAG, "displayContact: contact is known, displaying...");
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                android.net.Uri uri = android.net.Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                        String.valueOf(contact.getId()));
                intent.setData(uri);
                context.startActivity(intent);
            } catch (ActivityNotFoundException exc) {
                Log.e(TAG, "Error displaying contact", exc);
            }
        }
    }

    public static String getShortenedNumber(String number) {
        if (number != null && !number.isEmpty() && number.length() > 18) {
            int size = number.length();
            return number.substring(0, 9).concat("\u2026").concat(number.substring(size - 9, size));
        }
        return number;
    }
}
