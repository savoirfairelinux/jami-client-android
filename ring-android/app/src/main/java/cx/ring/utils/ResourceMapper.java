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
import cx.ring.model.DataTransferEventCode;

public class ResourceMapper {

    public static String getReadableFileTransferStatus(Context context, DataTransferEventCode dataTransferEventCode) {
        if (dataTransferEventCode == DataTransferEventCode.CREATED) {
            return context.getString(R.string.file_transfer_status_created);
        }
        if (dataTransferEventCode == DataTransferEventCode.UNSUPPORTED) {
            return context.getString(R.string.file_transfer_status_unsupported);
        }
        if (dataTransferEventCode == DataTransferEventCode.WAIT_PEER_ACCEPTANCE) {
            return context.getString(R.string.file_transfer_status_wait_peer_acceptance);
        }
        if (dataTransferEventCode == DataTransferEventCode.WAIT_HOST_ACCEPTANCE) {
            return context.getString(R.string.file_transfer_status_wait_host_acceptance);
        }
        if (dataTransferEventCode == DataTransferEventCode.ONGOING) {
            return context.getString(R.string.file_transfer_status_ongoing);
        }
        if (dataTransferEventCode == DataTransferEventCode.FINISHED) {
            return context.getString(R.string.file_transfer_status_finished);
        }
        if (dataTransferEventCode == DataTransferEventCode.CLOSED_BY_HOST) {
            return context.getString(R.string.file_transfer_status_closed_by_host);
        }
        if (dataTransferEventCode == DataTransferEventCode.CLOSED_BY_PEER) {
            return context.getString(R.string.file_transfer_status_closed_by_peer);
        }
        if (dataTransferEventCode == DataTransferEventCode.INVALID_PATHNAME) {
            return context.getString(R.string.file_transfer_status_invalid_pathname);
        }
        if (dataTransferEventCode == DataTransferEventCode.UNJOINABLE_PEER) {
            return context.getString(R.string.file_transfer_status_unjoinable_peer);
        }
        return "";
    }
}
