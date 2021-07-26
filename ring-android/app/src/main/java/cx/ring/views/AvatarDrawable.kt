/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 * Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.text.TextUtils
import android.util.TypedValue
import androidx.core.content.ContextCompat
import cx.ring.R
import cx.ring.services.VCardServiceImpl.Companion.loadProfile
import cx.ring.utils.DeviceUtils.isTv
import io.reactivex.rxjava3.core.Single
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.smartlist.SmartListViewModel
import net.jami.utils.HashUtils
import java.util.*

class AvatarDrawable : Drawable {
    private class PresenceIndicatorInfo {
        var cx = 0
        var cy = 0
        var radius = 0
    }

    private val presence = PresenceIndicatorInfo()
    private var update = true
    private val isGroup: Boolean
    private var inSize = -1
    private val minSize: Int
    private val workspace: MutableList<Bitmap?>
    private val bitmaps: MutableList<Bitmap>?
    private var placeholder: VectorDrawable? = null
    private var checkedIcon: VectorDrawable? = null
    private val backgroundBounds: List<RectF>?
    private val inBounds: List<Rect?>?
    private var avatarText: String? = null
    private var textStartXPoint = 0f
    private var textStartYPoint = 0f
    private var color = 0
    private val clipPaint: MutableList<Paint>?
    private val textPaint = Paint()
    private val presenceFillPaint: Paint
    private val presenceStrokePaint: Paint
    private val checkedPaint: Paint

    companion object {
        private val TAG = AvatarDrawable::class.simpleName!!
        private const val SIZE_AB = 36
        private const val SIZE_BORDER = 2
        private const val DEFAULT_TEXT_SIZE_PERCENTAGE = 0.5f
        private const val PLACEHOLDER_ICON = R.drawable.baseline_account_crop_24
        private const val PLACEHOLDER_ICON_GROUP = R.drawable.baseline_group_24
        private const val CHECKED_ICON = R.drawable.baseline_check_circle_24
        private const val PRESENCE_COLOR = R.color.green_A700
        private val contactColors = intArrayOf(
            R.color.red_500, R.color.pink_500,
            R.color.purple_500, R.color.deep_purple_500,
            R.color.indigo_500, R.color.blue_500,
            R.color.cyan_500, R.color.teal_500,
            R.color.green_500, R.color.light_green_500,
            R.color.grey_500, R.color.lime_500,
            R.color.amber_500, R.color.deep_orange_500,
            R.color.brown_500, R.color.blue_grey_500
        )
        private val drawPaint = Paint()

        @JvmStatic @JvmOverloads
        fun load(context: Context, account: Account, crop: Boolean = true): Single<AvatarDrawable> {
            return loadProfile(context, account)
                .map { data ->
                    Builder()
                        .withPhoto(data.second as Bitmap?)
                        .withNameData(data.first, account.registeredName)
                        .withId(account.uri)
                        .withCircleCrop(crop)
                        .build(context)
                }
        }

        private fun getSubBounds(bounds: Rect, total: Int, i: Int): Rect? {
            if (total == 1) return bounds
            if (total == 2 || total == 3 && i == 0) {
                //Rect zone = getSubZone(bounds, 2, 1);
                val w = bounds.width() / 2
                return if (i == 0)
                    Rect(bounds.left, bounds.top, bounds.left + w, bounds.bottom)
                else
                    Rect(bounds.left + w, bounds.top, bounds.right, bounds.bottom)
            }
            if (total == 3 || total == 4 && (i == 1 || i == 2)) {
                val w = bounds.width() / 2
                val h = bounds.height() / 2
                return if (i == 1)
                    Rect(bounds.left + w, bounds.top, bounds.right, bounds.top + h)
                else
                    Rect(bounds.left + w, bounds.top + h, bounds.right, bounds.bottom)
            }
            if (total == 4) {
                val w = bounds.width() / 2
                val h = bounds.height() / 2
                return if (i == 0)
                    Rect(bounds.left, bounds.top, bounds.left + w, bounds.top + h)
                else
                    Rect(bounds.left, bounds.top + h, bounds.left + w, bounds.bottom)
            }
            return null
        }

        private fun <T> fit(iw: Int, ih: Int, bw: Int, bh: Int, outfit: Boolean, ret: T) {
            val a = bw * ih
            val b = bh * iw
            val w: Int
            val h: Int
            if (outfit == a < b) {
                w = iw
                h = iw * bh / bw
            } else {
                w = ih * bw / bh
                h = ih
            }
            val x = (iw - w) / 2
            val y = (ih - h) / 2
            if (ret is Rect) ret[x, y, x + w] =
                y + h else if (ret is RectF) ret[x.toFloat(), y.toFloat(), (x + w).toFloat()] =
                (y + h).toFloat()
        }

        private fun getAvatarColor(id: String?): Int {
            if (id == null) {
                return R.color.grey_500
            }
            val md5 = HashUtils.md5(id) ?: return R.color.grey_500
            val colorIndex = (md5[0].toString() + "").toInt(16)
            return contactColors[colorIndex % contactColors.size]
        }

        init {
            drawPaint.isAntiAlias = true
            drawPaint.isFilterBitmap = true
        }
    }

    private val cropCircle: Boolean
    private var isOnline = false
    private var isChecked = false
    private var showPresence = false

    class Builder {
        private var photos: MutableList<Bitmap>? = null
        private var name: String? = null
        private var id: String? = null
        private var circleCrop = false
        private var isOnline = false
        private var showPresence = true
        private var isChecked = false
        private var isGroup = false
        fun withId(id: String?): Builder {
            this.id = id
            return this
        }

        fun withPhoto(photo: Bitmap?): Builder {
            photos = if (photo == null) null else mutableListOf(photo) // list elements must be mutable
            return this
        }

        fun withPhotos(photos: MutableList<Bitmap>): Builder {
            this.photos = if (photos.isEmpty()) null else photos
            return this
        }

        fun withName(name: String?): Builder {
            this.name = name
            return this
        }

        fun withCircleCrop(crop: Boolean): Builder {
            circleCrop = crop
            return this
        }

        fun withOnlineState(isOnline: Boolean): Builder {
            this.isOnline = isOnline
            return this
        }

        fun withPresence(showPresence: Boolean): Builder {
            this.showPresence = showPresence
            return this
        }

        fun withCheck(checked: Boolean): Builder {
            isChecked = checked
            return this
        }

        fun withNameData(profileName: String?, username: String?): Builder {
            return withName(if (TextUtils.isEmpty(profileName)) username else profileName)
        }

        fun withContact(contact: Contact?): Builder {
            return if (contact == null) this else withPhoto(contact.photo as Bitmap?)
                .withId(contact.primaryNumber)
                .withOnlineState(contact.isOnline)
                .withNameData(contact.profileName, contact.username)
        }

        fun withContacts(contacts: List<Contact>): Builder {
            val bitmaps: MutableList<Bitmap> = ArrayList(contacts.size)
            var notTheUser = 0
            for (contact in contacts) {
                if (contact.isUser) continue
                notTheUser++
                val bitmap = contact.photo as Bitmap?
                if (bitmap != null) {
                    bitmaps.add(bitmap)
                }
                if (bitmaps.size == 4) break
            }
            if (notTheUser == 1) {
                for (contact in contacts) {
                    if (!contact.isUser) return withContact(contact)
                }
            }
            if (bitmaps.isEmpty()) {
                // Fallback to the user avatar
                for (contact in contacts) return withContact(contact)
            } else {
                return withPhotos(bitmaps)
            }
            return this
        }

        fun withConversation(conversation: Conversation): Builder {
            return if (conversation.isSwarm) withContacts(conversation.contacts).setGroup()
            else withContact(conversation.contact)
        }

        private fun setGroup(): Builder {
            isGroup = true
            return this
        }

        fun withViewModel(vm: SmartListViewModel): Builder {
            val isSwarm = vm.uri.isSwarm
            return (if (isSwarm) withContacts(vm.contacts).setGroup() else withContact(if (vm.contacts.isEmpty()) null else vm.contacts[vm.contacts.size - 1]))
                .withPresence(vm.showPresence())
                .withOnlineState(vm.isOnline)
                .withCheck(vm.isChecked)
        }

        fun build(context: Context): AvatarDrawable {
            val avatarDrawable = AvatarDrawable(
                context, photos, name, id, circleCrop, isGroup
            )
            avatarDrawable.setOnline(isOnline)
            avatarDrawable.setChecked(isChecked)
            avatarDrawable.showPresence = showPresence
            return avatarDrawable
        }

        fun buildAsync(context: Context): Single<AvatarDrawable> {
            return Single.fromCallable { build(context) }
        }
    }

    fun update(contact: Contact) {
        val profileName = contact.profileName
        val username = contact.username
        avatarText = convertNameToAvatarText(
            if (TextUtils.isEmpty(profileName)) username else profileName
        )
        bitmaps?.set(0, contact.photo as Bitmap)
        isOnline = contact.isOnline
        update = true
    }

    fun setName(name: String?) {
        avatarText = convertNameToAvatarText(name)
        update = true
    }

    fun setPhoto(photo: Bitmap) {
        bitmaps!![0] = photo
        update = true
    }

    fun setOnline(online: Boolean) {
        isOnline = online
    }

    fun setChecked(checked: Boolean) {
        isChecked = checked
    }

    private constructor(
        context: Context,
        photos: MutableList<Bitmap>?,
        name: String?,
        id: String?,
        crop: Boolean,
        group: Boolean
    ) {
        //Log.w("AvatarDrawable", "AvatarDrawable " + (photos == null ? null : photos.size()) + " " + name);
        cropCircle = crop
        isGroup = group
        minSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SIZE_AB.toFloat(), context.resources.displayMetrics).toInt()
        val borderSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SIZE_BORDER.toFloat(), context.resources.displayMetrics)
        if (cropCircle) {
            inSize = minSize
        }
        if (photos != null && photos.size > 0) {
            avatarText = null
            bitmaps = photos
            if (photos.size == 1) {
                backgroundBounds = listOf(RectF())
                inBounds = listOf<Rect?>(null)
                clipPaint = if (cropCircle) mutableListOf(Paint()) else null
                workspace = mutableListOf(null as Bitmap?)
            } else {
                backgroundBounds = ArrayList(bitmaps.size)
                inBounds = ArrayList(bitmaps.size)
                clipPaint = if (cropCircle) ArrayList(bitmaps.size) else null
                workspace =
                    if (cropCircle) ArrayList(bitmaps.size) else Arrays.asList(null as Bitmap?)
                for (ignored in bitmaps) {
                    backgroundBounds.add(RectF())
                    inBounds.add(if (cropCircle) null else Rect())
                    if (cropCircle) {
                        val p = Paint()
                        p.strokeWidth = borderSize
                        p.color = Color.WHITE
                        p.style = Paint.Style.FILL
                        clipPaint!!.add(p)
                        workspace.add(null)
                    }
                }
            }
        } else {
            workspace = mutableListOf(null as Bitmap?)
            bitmaps = null
            backgroundBounds = null
            inBounds = null
            avatarText = convertNameToAvatarText(name)
            color = ContextCompat.getColor(context, getAvatarColor(id))
            clipPaint = if (cropCircle) mutableListOf(Paint()) else null
            if (avatarText == null) {
                placeholder =
                    context.getDrawable(if (isGroup) PLACEHOLDER_ICON_GROUP else PLACEHOLDER_ICON) as VectorDrawable?
            } else {
                textPaint.color = Color.WHITE
                textPaint.typeface = Typeface.SANS_SERIF
            }
        }
        presenceFillPaint = Paint()
        presenceFillPaint.color = ContextCompat.getColor(context, PRESENCE_COLOR)
        presenceFillPaint.style = Paint.Style.FILL
        presenceFillPaint.isAntiAlias = true
        presenceFillPaint.color = ContextCompat.getColor(context, PRESENCE_COLOR)
        presenceFillPaint.style = Paint.Style.FILL
        presenceFillPaint.isAntiAlias = true
        presenceStrokePaint = Paint()
        presenceStrokePaint.color = ContextCompat.getColor(
            context,
            if (isTv(context)) R.color.grey_900 else R.color.background
        )
        presenceStrokePaint.style = Paint.Style.STROKE
        presenceStrokePaint.isAntiAlias = true
        checkedIcon = context.getDrawable(CHECKED_ICON) as VectorDrawable?
        checkedIcon!!.setTint(ContextCompat.getColor(context, R.color.colorPrimary))
        checkedPaint = Paint()
        checkedPaint.color = ContextCompat.getColor(context, R.color.background)
        checkedPaint.style = Paint.Style.FILL_AND_STROKE
        checkedPaint.isAntiAlias = true
        if (clipPaint != null) for (p in clipPaint) p.isAntiAlias = true
        textPaint.isAntiAlias = true
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.SANS_SERIF
    }

    constructor(other: AvatarDrawable) {
        //Log.w("AvatarDrawable", "AvatarDrawable copy");
        cropCircle = other.cropCircle
        isGroup = other.isGroup
        minSize = other.minSize
        bitmaps = other.bitmaps
        if (other.backgroundBounds != null) {
            backgroundBounds = ArrayList(other.backgroundBounds.size)
            var i = 0
            val n = other.backgroundBounds.size
            while (i < n) {
                backgroundBounds.add(RectF())
                i++
            }
        } else {
            backgroundBounds = null
        }
        inBounds = other.inBounds
        color = other.color
        placeholder = other.placeholder
        avatarText = other.avatarText
        workspace = ArrayList(other.workspace.size)
        var i = 0
        val n = other.workspace.size
        while (i < n) {
            workspace.add(null)
            i++
        }
        clipPaint = if (other.clipPaint == null) null else ArrayList(other.clipPaint.size)
        if (clipPaint != null) {
            i = 0
            val n = other.clipPaint!!.size
            while (i < n) {
                clipPaint.add(Paint(other.clipPaint[i]))
                clipPaint[i].shader = null
                i++
            }
        }
        isOnline = other.isOnline
        isChecked = other.isChecked
        showPresence = other.showPresence
        presenceFillPaint = other.presenceFillPaint
        presenceStrokePaint = other.presenceStrokePaint
        checkedPaint = other.checkedPaint
        textPaint.isAntiAlias = true
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.SANS_SERIF
    }

    override fun draw(finalCanvas: Canvas) {
        if (workspace[0] == null) return
        if (update) {
            var i = 0
            val s = workspace.size
            while (i < s) {
                drawActual(i, Canvas(workspace[i]!!))
                i++
            }
            update = false
        }
        if (cropCircle) {
            finalCanvas.save()
            finalCanvas.translate(
                (bounds.width() - workspace[0]!!
                    .width) / 2f, (bounds.height() - workspace[0]!!.height) / 2f
            )
            var r = (Math.min(
                workspace[0]!!.width, workspace[0]!!.height
            ) / 2).toFloat()
            val cx = workspace[0]!!.width / 2 //getBounds().centerX();
            var cy = (workspace[0]!!.height / 2).toFloat() //getBounds().height() / 2;
            var i = 0
            val ratio = 1.333333f
            for (paint in clipPaint!!) {
                finalCanvas.drawCircle(cx.toFloat(), workspace[0]!!.height - cy, r, paint)
                if (i != 0) {
                    val s = paint.shader
                    paint.shader = null
                    paint.style = Paint.Style.STROKE
                    finalCanvas.drawCircle(cx.toFloat(), workspace[0]!!.height - cy, r, paint)
                    paint.shader = s
                    paint.style = Paint.Style.FILL
                }
                i++
                r /= ratio
                cy /= ratio
            }
            finalCanvas.restore()
        } else {
            finalCanvas.drawBitmap(workspace[0]!!, null, bounds, drawPaint)
        }
        if (showPresence && isOnline) {
            drawPresence(finalCanvas)
        }
        if (isChecked) {
            drawChecked(finalCanvas)
        }
    }

    private fun drawActual(i: Int, canvas: Canvas) {
        if (bitmaps != null) {
            if (cropCircle) {
                canvas.drawBitmap(bitmaps[i], inBounds!![i], backgroundBounds!![i], drawPaint)
            } else {
                if (backgroundBounds!!.size == bitmaps.size) {
                    var n = 0
                    val s = bitmaps.size
                    while (n < s) {
                        canvas.drawBitmap(bitmaps[n], inBounds!![n], backgroundBounds[n], drawPaint)
                        n++
                    }
                }
            }
        } else {
            canvas.drawColor(color)
            if (avatarText != null) {
                canvas.drawText(avatarText!!, textStartXPoint, textStartYPoint, textPaint)
            } else {
                placeholder!!.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                placeholder!!.draw(canvas)
            }
        }
    }

    private fun setupPresenceIndicator(bounds: Rect) {
        presence.radius = (0.29289321881 * bounds.width().toDouble() * 0.5).toInt()
        presence.cx = bounds.right - presence.radius
        presence.cy = bounds.bottom - presence.radius
        val presenceStrokeWidth = presence.radius / 3
        presenceStrokePaint.strokeWidth = presenceStrokeWidth.toFloat()
        checkedPaint.strokeWidth = presenceStrokeWidth.toFloat()
        presence.radius -= (presenceStrokeWidth * 0.5).toInt()
        if (checkedIcon != null) checkedIcon!!.setBounds(
            presence.cx - presence.radius,
            presence.cy - presence.radius,
            presence.cx + presence.radius,
            presence.cy + presence.radius
        )
    }

    private fun drawPresence(canvas: Canvas) {
        canvas.drawCircle(
            presence.cx.toFloat(),
            presence.cy.toFloat(),
            (presence.radius - 1).toFloat(),
            presenceFillPaint
        )
        canvas.drawCircle(
            presence.cx.toFloat(),
            presence.cy.toFloat(),
            presence.radius.toFloat(),
            presenceStrokePaint
        )
    }

    private fun drawChecked(canvas: Canvas) {
        if (checkedIcon != null) {
            canvas.drawCircle(
                presence.cx.toFloat(),
                presence.cy.toFloat(),
                presence.radius.toFloat(),
                checkedPaint
            )
            checkedIcon!!.draw(canvas)
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        //if (showPresence)
        setupPresenceIndicator(bounds)
        val d = Math.min(bounds.width(), bounds.height())
        if (placeholder != null) {
            val cx = (bounds.width() - d) / 2
            val cy = (bounds.height() - d) / 2
            placeholder!!.setBounds(cx, cy, cx + d, cy + d)
        }
        val iw = if (cropCircle) d else bounds.width()
        val ih = if (cropCircle) d else bounds.height()
        var i = 0
        val n = workspace.size
        while (i < n) {
            if (workspace[i] != null) {
                workspace[i]!!.recycle()
                workspace[i] = null
                clipPaint!![i].shader = null
            }
            i++
        }
        if (iw <= 0 || ih <= 0) {
            return
        }
        if (cropCircle) {
            i = 0
            val s = workspace.size
            while (i < s) {
                val workspacei = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888)
                workspace[i] = workspacei
                clipPaint!![i].shader = BitmapShader(workspacei, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                i++
            }
        } else {
            workspace[0] =
                Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        }
        if (bitmaps != null) {
            if (bitmaps.size == 1 || cropCircle) {
                for (i in bitmaps.indices) {
                    val bitmap = bitmaps[i]
                    fit(iw, ih, bitmap.width, bitmap.height, true, backgroundBounds!![i])
                }
            } else {
                val realBounds = if (cropCircle) Rect(0, 0, iw, ih) else bounds
                for (i in bitmaps.indices) {
                    val bitmap = bitmaps[i]
                    val subBounds = getSubBounds(realBounds, bitmaps.size, i)
                    if (subBounds != null) {
                        fit(
                            bitmap.width,
                            bitmap.height,
                            subBounds.width(),
                            subBounds.height(),
                            false,
                            inBounds!![i]
                        )
                        backgroundBounds!![i].set(subBounds)
                    }
                }
            }
        } else {
            setAvatarTextValues(bounds)
        }
        update = true
    }

    override fun setAlpha(alpha: Int) {
        if (placeholder != null) {
            placeholder!!.alpha = alpha
        } else {
            textPaint.alpha = alpha
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        if (placeholder != null) {
            placeholder!!.colorFilter = colorFilter
        } else {
            textPaint.colorFilter = colorFilter
        }
    }

    override fun getMinimumWidth(): Int {
        return minSize
    }

    override fun getMinimumHeight(): Int {
        return minSize
    }

    fun setInSize(s: Int) {
        inSize = s
    }

    override fun getIntrinsicWidth(): Int {
        return inSize
    }

    override fun getIntrinsicHeight(): Int {
        return inSize
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    private fun setAvatarTextValues(bounds: Rect) {
        if (avatarText != null) {
            textPaint.textSize = bounds.height() * DEFAULT_TEXT_SIZE_PERCENTAGE
            val stringWidth = textPaint.measureText(avatarText)
            textStartXPoint = bounds.width() / 2f - stringWidth / 2f
            textStartYPoint = bounds.height() / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
        }
    }

    private fun convertNameToAvatarText(name: String?): String? {
        return if (TextUtils.isEmpty(name)) {
            null
        } else {
            String(Character.toChars(name!!.codePointAt(0))).uppercase(Locale.getDefault())
        }
    }
}