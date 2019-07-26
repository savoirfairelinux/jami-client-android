/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.utils;

import android.content.Context;

import cx.ring.R;
import cx.ring.model.Interaction.InteractionStatus;

public class ResourceMapper {

    public static String getReadableFileTransferStatus(Context context, InteractionStatus dataTransferEventCode) {
        if (dataTransferEventCode == InteractionStatus.TRANSFER_CREATED) {
            return context.getString(R.string.file_transfer_status_created);
        }
        if (dataTransferEventCode == InteractionStatus.TRANSFER_AWAITING_PEER) {
            return context.getString(R.string.file_transfer_status_wait_peer_acceptance);
        }
        if (dataTransferEventCode == InteractionStatus.TRANSFER_AWAITING_HOST) {
            return context.getString(R.string.file_transfer_status_wait_host_acceptance);
        }
        if (dataTransferEventCode == InteractionStatus.TRANSFER_ONGOING) {
            return context.getString(R.string.file_transfer_status_ongoing);
        }
        if (dataTransferEventCode == InteractionStatus.TRANSFER_FINISHED) {
            return context.getString(R.string.file_transfer_status_finished);
        }
        if (dataTransferEventCode == InteractionStatus.TRANSFER_CANCELED) {
            return context.getString(R.string.file_transfer_status_closed_by_peer);
        }
        if (dataTransferEventCode == InteractionStatus.TRANSFER_UNJOINABLE_PEER) {
            return context.getString(R.string.file_transfer_status_unjoinable_peer);
        }
        if (dataTransferEventCode == InteractionStatus.TRANSFER_ERROR) {
            return "transfer error";
        }
        if (dataTransferEventCode == InteractionStatus.TRANSFER_TIMEOUT_EXPIRED) {
            return "transfer timed out";
        }
        return "";
    }
}
