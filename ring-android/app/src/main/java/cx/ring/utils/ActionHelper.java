package cx.ring.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import cx.ring.R;
import cx.ring.adapters.NumberAdapter;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;

/**
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

public class ActionHelper {

    private static final String TAG = ActionHelper.class.getSimpleName();

    public static AlertDialog launchDeleteAction(final Activity activity,
                                                 final Conversation conversation,
                                                 final Conversation.ConversationActionCallback callback) {
        if (activity == null) {
            Log.d(TAG, "launchDeleteAction: activity is null");
            return null;
        }

        if (conversation == null) {
            Log.d(TAG, "launchDeleteAction: conversation is null");
            return null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.conversation_action_delete_this_title)
                .setMessage(R.string.conversation_action_delete_this_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (callback != null) {
                            callback.deleteConversation(conversation);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Terminate with no action */
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        return alertDialog;
    }

    public static void launchCopyNumberToClipboardFromContact(final Activity activity,
                                                              final CallContact callContact,
                                                              final Conversation.ConversationActionCallback callback) {
        if (callContact == null) {
            Log.d(TAG, "launchCopyNumberToClipboardFromContact: callContact is null");
            return;
        }

        if (activity == null) {
            Log.d(TAG, "launchCopyNumberToClipboardFromContact: activity is null");
            return;
        }

        if (callContact.getPhones().isEmpty()) {
            Log.d(TAG, "launchCopyNumberToClipboardFromContact: no number to copy");
            return;
        } else if (callContact.getPhones().size() == 1 && callback != null) {
            String number = callContact.getPhones().get(0).getNumber().toString();
            callback.copyContactNumberToClipboard(number);
            return;
        }

        final NumberAdapter adapter = new NumberAdapter(activity, callContact, true);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.conversation_action_select_peer_number);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (callback != null) {
                    CallContact.Phone selectedPhone = (CallContact.Phone) adapter.getItem(which);
                    callback.copyContactNumberToClipboard(selectedPhone.getNumber().toString());
                }
            }
        });
        AlertDialog dialog = builder.create();
        final int listViewSidePadding = (int) activity
                .getResources()
                .getDimension(R.dimen.alert_dialog_side_padding_list_view);
        dialog.getListView().setPadding(listViewSidePadding, 0, listViewSidePadding, 0);
        dialog.show();
    }
}
