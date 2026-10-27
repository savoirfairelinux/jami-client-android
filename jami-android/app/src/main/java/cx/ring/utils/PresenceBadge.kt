/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.utils

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import cx.ring.R
import net.jami.model.Contact

/** Presence dot for contact lists without avatars, with the colours of the avatar badge. */
object PresenceBadge {
    @ColorInt
    fun color(context: Context, status: Contact.PresenceStatus): Int? = when (status) {
        Contact.PresenceStatus.CONNECTED -> ContextCompat.getColor(context, R.color.online_indicator)
        Contact.PresenceStatus.AVAILABLE -> ContextCompat.getColor(context, R.color.available_indicator)
        Contact.PresenceStatus.OFFLINE -> null
    }

    /** The name followed by a green (connected) or orange (available) dot; nothing when offline. */
    fun label(context: Context, name: CharSequence, status: Contact.PresenceStatus): CharSequence {
        val color = color(context, status) ?: return name
        return SpannableStringBuilder(name)
            .append("  ")
            .append("●", ForegroundColorSpan(color), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
