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
package cx.ring.client

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bidirectional mapping between the daemon's Quill-delta rich-text format and an Android
 * [Editable]/[Spannable], mirroring the Qt client's CollabRichBinding semantics.
 *
 * Attributes are per-character (matching the Qt daemon, which stores even headings and lists as a
 * per-character attribute to avoid Quill's trailing-newline invariant):
 *   b/i/u/s = bold/italic/underline/strikethrough (true | absent)
 *   header  = 1..3 (paragraph heading, stored per character) | absent
 *   list    = "bullet" | "ordered" (paragraph list, stored per character) | absent
 *   link    = href (display only) | absent
 * Offsets are UTF-16 code units, which match both Android char indices and the daemon's
 * Y_OFFSET_UTF16 mode, so no index conversion is needed.
 */
object CollabRichText {

    data class Attrs(
        val b: Boolean = false,
        val i: Boolean = false,
        val u: Boolean = false,
        val s: Boolean = false,
        val header: Int = 0,
        val list: String? = null,
        val link: String? = null,
    ) {
        val isEmpty: Boolean
            get() = !b && !i && !u && !s && header == 0 && list == null && link == null
    }

    /** A heading rendered as a relative size bump + faux-bold, carrying its level for read-back. */
    class HeaderSpan(val level: Int) : MetricAffectingSpan() {
        private fun scale() = when (level) { 1 -> 1.6f; 2 -> 1.3f; 3 -> 1.15f; else -> 1f }
        override fun updateMeasureState(p: TextPaint) { apply(p) }
        override fun updateDrawState(tp: TextPaint) { apply(tp) }
        private fun apply(p: TextPaint) {
            p.textSize = p.textSize * scale()
            p.isFakeBoldText = true
        }
    }

    /**
     * A paragraph list marker (bullet or ordered). Carries [ordered] for read-back and a mutable
     * [number] set by [reconcileLists] for ordered numbering. The marker is drawn in the leading
     * margin of the paragraph's first line.
     */
    class ListSpan(val ordered: Boolean, var number: Int = 0) : LeadingMarginSpan {
        override fun getLeadingMargin(first: Boolean): Int = MARGIN
        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            text: CharSequence?, start: Int, end: Int,
            first: Boolean, layout: Layout?
        ) {
            if (!first) return // draw the marker only on the paragraph's first line
            val marker = if (ordered) "$number." else "\u2022"
            c.drawText(marker, x.toFloat() + dir * PAD, baseline.toFloat(), p)
        }
        companion object {
            const val MARGIN = 56
            const val PAD = 8f
        }
    }


    // --- Reading attributes back from the editable --------------------------------------------

    /** Inline attributes of the character at [pos] (the character starting at [pos]). */
    fun attrsAt(text: Spanned, pos: Int): Attrs {
        val len = text.length
        if (len == 0) return Attrs()
        // Read the span state covering [p, p+1); clamp so the end of the text reports the
        // attributes of the last character (so typing at the end inherits formatting).
        val p = pos.coerceIn(0, len - 1)
        var b = false; var i = false; var u = false; var s = false; var header = 0; var list: String? = null; var link: String? = null
        for (span in text.getSpans(p, p + 1, Any::class.java)) {
            // Ignore spans that merely touch the boundary without covering the character.
            if (text.getSpanStart(span) > p || text.getSpanEnd(span) <= p) continue
            when (span) {
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> b = true
                    Typeface.ITALIC -> i = true
                    Typeface.BOLD_ITALIC -> { b = true; i = true }
                }
                is UnderlineSpan -> u = true
                is StrikethroughSpan -> s = true
                is HeaderSpan -> header = span.level
                is ListSpan -> list = if (span.ordered) "ordered" else "bullet"
                is android.text.style.URLSpan -> link = span.url
            }
        }
        return Attrs(b, i, u, s, header, list, link)
    }

    // --- Applying attributes as spans ---------------------------------------------------------

    /** Replace this range's inline spans for [attr] keys present in [attrs] (true sets, false clears). */
    private fun applyAttrs(text: Spannable, start: Int, end: Int, attrs: Map<String, Any?>) {
        if (start >= end) return
        for ((key, value) in attrs) {
            val on = when (value) {
                is Boolean -> value
                is String -> value.isNotEmpty()
                is Int -> value != 0
                null -> false
                else -> false
            }
            when (key) {
                "b" -> { clearStyle(text, start, end, Typeface.BOLD); if (on) text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
                "i" -> { clearStyle(text, start, end, Typeface.ITALIC); if (on) text.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
                "u" -> { clearSpans(text, start, end, UnderlineSpan::class.java); if (on) text.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
                "s" -> { clearSpans(text, start, end, StrikethroughSpan::class.java); if (on) text.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
                "header" -> {
                    clearSpans(text, start, end, HeaderSpan::class.java)
                    val level = (value as? Int) ?: 0
                    if (level in 1..3) text.setSpan(HeaderSpan(level), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "list" -> {
                    clearSpans(text, start, end, ListSpan::class.java)
                    when (value as? String) {
                        "bullet" -> text.setSpan(ListSpan(false), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                        "ordered" -> text.setSpan(ListSpan(true), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                        else -> {} // null clears
                    }
                }
                "link" -> {
                    clearSpans(text, start, end, android.text.style.URLSpan::class.java)
                    if (on) text.setSpan(android.text.style.URLSpan(value as String), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun clearStyle(text: Spannable, start: Int, end: Int, style: Int) {
        for (span in text.getSpans(start, end, StyleSpan::class.java)) {
            if (span.style == style || (style != Typeface.BOLD_ITALIC && span.style == Typeface.BOLD_ITALIC))
                text.removeSpan(span)
        }
    }

    private fun <T> clearSpans(text: Spannable, start: Int, end: Int, cls: Class<T>) {
        for (span in text.getSpans(start, end, cls)) text.removeSpan(span as Any)
    }

    // --- Delta -> editable --------------------------------------------------------------------

    /** Build a fresh spannable from a full content delta (all-insert ops). */
    fun deltaToSpannable(deltaJson: String): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        val ops = parseOps(deltaJson) ?: return sb
        for (k in 0 until ops.length()) {
            val op = ops.optJSONObject(k) ?: continue
            if (!op.has("insert") || op.get("insert") !is String) continue
            val insert = op.getString("insert")
            val start = sb.length
            sb.append(insert)
            applyAttrs(sb, start, sb.length, attrMap(op.optJSONObject("attributes")))
        }
        reconcileLists(sb)
        return sb
    }

    /**
     * Apply an incremental remote delta onto [text] in place, walking a cursor through retain/
     * insert/delete ops. Returns nothing; the caller refreshes its shadow afterwards.
     */
    fun applyRemoteDelta(text: Editable, deltaJson: String) {
        val ops = parseOps(deltaJson) ?: return
        var index = 0
        for (k in 0 until ops.length()) {
            val op = ops.optJSONObject(k) ?: continue
            when {
                op.has("insert") && op.get("insert") is String -> {
                    val s = op.getString("insert")
                    val at = index.coerceIn(0, text.length)
                    text.insert(at, s)
                    applyAttrs(text, at, at + s.length, attrMap(op.optJSONObject("attributes")))
                    index = at + s.length
                }
                op.has("retain") -> {
                    val n = op.optInt("retain")
                    val attrs = op.optJSONObject("attributes")
                    if (attrs != null && n > 0) {
                        val a = index.coerceIn(0, text.length)
                        val b = (index + n).coerceIn(a, text.length)
                        applyAttrs(text, a, b, attrMap(attrs))
                    }
                    index += n
                }
                op.has("delete") -> {
                    val n = op.optInt("delete")
                    if (n > 0) {
                        val a = index.coerceIn(0, text.length)
                        val b = (index + n).coerceIn(a, text.length)
                        text.delete(a, b)
                    }
                }
            }
        }
        reconcileLists(text)
    }

    /**
     * Normalize list paragraph markers, mirroring the Qt client's reconcileLists: walk lines and,
     * for each whose first character carries a list marker, apply one [ListSpan] over the whole
     * line; consecutive "ordered" lines are numbered 1,2,3… and any non-ordered line resets the
     * counter. The first character's marker is the per-line source of truth.
     */
    fun reconcileLists(text: Spannable) {
        val str = text.toString()
        var ordinal = 0
        var i = 0
        while (i <= str.length) {
            val nl = str.indexOf('\n', i)
            val lineStart = i
            val lineEnd = if (nl < 0) str.length else nl
            val kind = lineKind(text, lineStart, lineEnd)
            // Remove existing markers on the line; re-apply a single normalized one.
            clearSpans(text, lineStart, maxOf(lineStart + 1, lineEnd), ListSpan::class.java)
            when (kind) {
                "ordered" -> { ordinal++; if (lineEnd > lineStart) text.setSpan(ListSpan(true, ordinal), lineStart, lineEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE) }
                "bullet" -> { ordinal = 0; if (lineEnd > lineStart) text.setSpan(ListSpan(false), lineStart, lineEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE) }
                else -> ordinal = 0
            }
            if (nl < 0) break
            i = nl + 1
        }
    }

    /** The list kind of a line, read from the first character's marker (null if none). */
    private fun lineKind(text: Spanned, lineStart: Int, lineEnd: Int): String? {
        if (lineEnd <= lineStart) return null
        for (span in text.getSpans(lineStart, lineStart + 1, ListSpan::class.java)) {
            if (text.getSpanStart(span) > lineStart || text.getSpanEnd(span) <= lineStart) continue
            return if (span.ordered) "ordered" else "bullet"
        }
        return null
    }

    // --- Local edits -> delta -----------------------------------------------------------------

    /**
     * Diff [now] against [shadow] (common prefix/suffix) and produce a Quill delta for the local
     * text change, grouping inserted characters into runs that share inline attributes (read from
     * [text] at the insertion point). Returns null if there is no text change.
     */
    fun localTextDiffDelta(shadow: String, now: String, text: Spanned): String? {
        if (now == shadow) return null
        val oldLen = shadow.length
        val newLen = now.length
        var prefix = 0
        val maxPrefix = minOf(oldLen, newLen)
        while (prefix < maxPrefix && shadow[prefix] == now[prefix]) prefix++
        var suffix = 0
        val maxSuffix = minOf(oldLen, newLen) - prefix
        while (suffix < maxSuffix && shadow[oldLen - 1 - suffix] == now[newLen - 1 - suffix]) suffix++

        val removed = oldLen - prefix - suffix
        val added = newLen - prefix - suffix

        val ops = JSONArray()
        if (prefix > 0) ops.put(JSONObject().put("retain", prefix))
        if (removed > 0) ops.put(JSONObject().put("delete", removed))
        if (added > 0) {
            val inserted = now.substring(prefix, newLen - suffix)
            var i = 0
            while (i < inserted.length) {
                val a = attrsAt(text, prefix + i)
                var j = i + 1
                while (j < inserted.length && attrsAt(text, prefix + j) == a) j++
                val op = JSONObject().put("insert", inserted.substring(i, j))
                attrsToJson(a)?.let { op.put("attributes", it) }
                ops.put(op)
                i = j
            }
        }
        return if (ops.length() > 0) ops.toString() else null
    }

    /**
     * Toggle an inline attribute over [start,end) based on the first character's state, mutate the
     * spans in place, and return the delta describing the change.
     */
    fun toggleInline(text: Spannable, attr: String, start: Int, end: Int): String? {
        if (start >= end) return null
        val current = attrsAt(text, start)
        val isSet = when (attr) { "b" -> current.b; "i" -> current.i; "u" -> current.u; "s" -> current.s; else -> false }
        val value: Any? = if (isSet) null else true
        applyAttrs(text, start, end, mapOf(attr to value))
        val ops = JSONArray()
        if (start > 0) ops.put(JSONObject().put("retain", start))
        ops.put(JSONObject().put("retain", end - start).put("attributes", JSONObject().put(attr, value ?: JSONObject.NULL)))
        return ops.toString()
    }

    /**
     * Apply a heading [level] (0 clears) to the whole paragraph(s) spanned by [start,end), mutate
     * the spans, and return the delta. Heading is a per-character attribute over the line text.
     */
    fun setHeading(text: Spannable, level: Int, start: Int, end: Int): String? {
        val str = text.toString()
        val lineStart = str.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        var lineEnd = str.indexOf('\n', maxOf(start, end))
        if (lineEnd < 0) lineEnd = str.length
        if (lineStart >= lineEnd) return null
        val value: Any? = if (level in 1..3) level else null
        applyAttrs(text, lineStart, lineEnd, mapOf("header" to value))
        val ops = JSONArray()
        if (lineStart > 0) ops.put(JSONObject().put("retain", lineStart))
        ops.put(JSONObject().put("retain", lineEnd - lineStart).put("attributes", JSONObject().put("header", value ?: JSONObject.NULL)))
        return ops.toString()
    }

    // --- JSON helpers -------------------------------------------------------------------------

    private fun parseOps(deltaJson: String): JSONArray? = try {
        JSONArray(deltaJson)
    } catch (e: Exception) {
        null
    }

    private fun attrMap(o: JSONObject?): Map<String, Any?> {
        if (o == null) return emptyMap()
        val m = HashMap<String, Any?>()
        for (key in o.keys()) {
            val v = o.get(key)
            m[key] = when {
                v == JSONObject.NULL -> null
                key == "header" -> (v as? Number)?.toInt() ?: 0
                else -> v
            }
        }
        return m
    }

    private fun attrsToJson(a: Attrs): JSONObject? {
        if (a.isEmpty) return null
        val o = JSONObject()
        if (a.b) o.put("b", true)
        if (a.i) o.put("i", true)
        if (a.u) o.put("u", true)
        if (a.s) o.put("s", true)
        if (a.header in 1..3) o.put("header", a.header)
        if (a.list != null) o.put("list", a.list)
        if (a.link != null) o.put("link", a.link)
        return o
    }
}
