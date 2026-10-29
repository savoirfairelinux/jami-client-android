package cx.ring.views

import androidx.annotation.StringRes
import androidx.core.view.isVisible
import cx.ring.databinding.ItemLifecycleDateBinding
import java.text.DateFormat
import java.util.Date

internal fun ItemLifecycleDateBinding.setDate(@StringRes label: Int, timestamp: Long) {
    root.isVisible = timestamp > 0
    dateLabel.setText(label)
    dateValue.text = if (timestamp > 0)
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
    else ""
}
