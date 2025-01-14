/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.text.format.DateUtils
import android.widget.Toast
import cx.ring.R
import net.jami.model.interaction.Interaction.TransferStatus
import java.util.*

object TextUtils {
    val TAG = TextUtils::class.simpleName!!

    /**
     * Copy the text to the clipboard and show a toast to notify the user.
     */
    fun copyAndShow(context: Context, label: String, text: String?) {
        if (text.isNullOrEmpty()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))

        // Android 13 and higher automatically provide visual feedback when an app copies content
        // to the clipboard. Provide manual notification in Android 12L (API level 32) and lower
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Toast.makeText(context, context.getString(R.string.pin_copied), Toast.LENGTH_SHORT)
                .show()
    }

    /**
     * Computes the string to set in text details between messages, indicating time separation.
     *
     * @param timestamp The timestamp used to launch the computation with Date().getTime().
     * Can be the last received message timestamp for example.
     * @return The string to display in the text details between messages.
     */
    fun timestampToDetailString(context: Context, formatter: Formatter, timestamp: Long): String {
        (formatter.out() as? StringBuilder)?.setLength(0)
        val diff = Date().time - timestamp
        val timeStr: Formatter = if (diff < DateUtils.WEEK_IN_MILLIS) {
            if (diff < DateUtils.DAY_IN_MILLIS && DateUtils.isToday(timestamp)) { // 11:32 A.M.
                DateUtils.formatDateRange(context, formatter, timestamp, timestamp, DateUtils.FORMAT_SHOW_TIME)
            } else {
                DateUtils.formatDateRange(context, formatter, timestamp, timestamp,
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_NO_YEAR or
                            DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME)
            }
        } else if (diff < DateUtils.YEAR_IN_MILLIS) {
            DateUtils.formatDateRange(context, formatter, timestamp, timestamp, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or
                    DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME)
        } else {
            DateUtils.formatDateRange(context, formatter, timestamp, timestamp, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY or
                    DateUtils.FORMAT_ABBREV_ALL)
        }
        return timeStr.out().toString().uppercase(Locale.getDefault())
    }

    /**
     * Converts a timestamp into a date.
     * May returns terms like 'Today' or 'Yesterday'. In all other cases, the complete date.
     */
    fun timestampToDate(context: Context, formatter: Formatter, timestamp: Long): String {
        (formatter.out() as? StringBuilder)?.setLength(0)
        fun isYesterday(timestamp: Long): Boolean {
            return DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS)
        }

        if (DateUtils.isToday(timestamp)) return context.getString(R.string.today).uppercase()
        else if (isYesterday(timestamp)) return context.getString(R.string.yesterday).uppercase()

        val timeStr: Formatter =
            DateUtils.formatDateRange(
                context, formatter, timestamp, timestamp,
                DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            )
        return timeStr.out().toString().uppercase(Locale.getDefault())
    }

    /**
     * Converts a timestamp into a time. Don't display the date.
     * Example of the output format = 11:32 A.M.
     */
    fun timestampToTime(context: Context, formatter: Formatter, timestamp: Long): String {
        (formatter.out() as? StringBuilder)?.setLength(0)
        return DateUtils // Example of the format in the message = 11:32 A.M.
            .formatDateRange(context, formatter, timestamp, timestamp, DateUtils.FORMAT_SHOW_TIME)
            .out().toString().uppercase(Locale.getDefault())
    }

    fun getReadableFileTransferStatus(context: Context, transferStatus: TransferStatus): String {
        return when (transferStatus) {
            TransferStatus.TRANSFER_CREATED -> context.getString(R.string.file_transfer_status_created)
            TransferStatus.TRANSFER_AWAITING_PEER -> context.getString(R.string.file_transfer_status_wait_peer_acceptance)
            TransferStatus.TRANSFER_AWAITING_HOST -> context.getString(R.string.file_transfer_status_wait_host_acceptance)
            TransferStatus.TRANSFER_ONGOING -> context.getString(R.string.file_transfer_status_ongoing)
            TransferStatus.TRANSFER_FINISHED -> context.getString(R.string.file_transfer_status_finished)
            TransferStatus.TRANSFER_CANCELED -> context.getString(R.string.file_transfer_status_cancelled)
            TransferStatus.TRANSFER_UNJOINABLE_PEER -> context.getString(R.string.file_transfer_status_unjoinable_peer)
            TransferStatus.TRANSFER_ERROR -> context.getString(R.string.file_transfer_status_error)
            TransferStatus.TRANSFER_TIMEOUT_EXPIRED -> context.getString(R.string.file_transfer_status_timed_out)
            else -> ""
        }
    }
}

