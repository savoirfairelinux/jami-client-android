/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.utils

import android.text.TextUtils
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateUtils
import cx.ring.R
import net.jami.model.Interaction.InteractionStatus
import java.util.*

object TextUtils {
    val TAG = TextUtils::class.simpleName!!

    fun copyToClipboard(context: Context, text: String?) {
        if (TextUtils.isEmpty(text)) {
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getText(R.string.clip_contact_uri), text))
    }

    fun getShortenedNumber(number: String): String {
        val size = number.length
        if (size > 18)
            return number.substring(0, 9) + "\u2026" + number.substring(size - 9, size)
        return number
    }

    /**
     * Computes the string to set in text details between messages, indicating time separation.
     *
     * @param timestamp The timestamp used to launch the computation with Date().getTime().
     * Can be the last received message timestamp for example.
     * @return The string to display in the text details between messages.
     */
    fun timestampToDetailString(context: Context, timestamp: Long): String {
        val diff = Date().time - timestamp
        val timeStr: String = if (diff < DateUtils.WEEK_IN_MILLIS) {
            if (diff < DateUtils.DAY_IN_MILLIS && DateUtils.isToday(timestamp)) { // 11:32 A.M.
                DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME)
            } else {
                DateUtils.formatDateTime(context, timestamp,
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_NO_YEAR or
                            DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME)
            }
        } else if (diff < DateUtils.YEAR_IN_MILLIS) {
            DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or
                    DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME)
        } else {
            DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY or
                    DateUtils.FORMAT_ABBREV_ALL)
        }
        return timeStr.uppercase(Locale.getDefault())
    }

    fun getReadableFileTransferStatus(context: Context, transferStatus: InteractionStatus): String {
        return when (transferStatus) {
            InteractionStatus.TRANSFER_CREATED -> context.getString(R.string.file_transfer_status_created)
            InteractionStatus.TRANSFER_AWAITING_PEER -> context.getString(R.string.file_transfer_status_wait_peer_acceptance)
            InteractionStatus.TRANSFER_AWAITING_HOST -> context.getString(R.string.file_transfer_status_wait_host_acceptance)
            InteractionStatus.TRANSFER_ONGOING -> context.getString(R.string.file_transfer_status_ongoing)
            InteractionStatus.TRANSFER_FINISHED -> context.getString(R.string.file_transfer_status_finished)
            InteractionStatus.TRANSFER_CANCELED -> context.getString(R.string.file_transfer_status_cancelled)
            InteractionStatus.TRANSFER_UNJOINABLE_PEER -> context.getString(R.string.file_transfer_status_unjoinable_peer)
            InteractionStatus.TRANSFER_ERROR -> context.getString(R.string.file_transfer_status_error)
            InteractionStatus.TRANSFER_TIMEOUT_EXPIRED -> context.getString(R.string.file_transfer_status_timed_out)
            else -> ""
        }
    }
}

