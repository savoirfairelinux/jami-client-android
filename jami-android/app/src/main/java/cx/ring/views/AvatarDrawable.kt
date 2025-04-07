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
package cx.ring.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import cx.ring.R
import cx.ring.services.VCardServiceImpl
import cx.ring.utils.DeviceUtils.isTv
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import net.jami.model.*
import net.jami.smartlist.ConversationItemViewModel
import net.jami.utils.HashUtils
import net.jami.utils.toHex
import java.util.*
import kotlin.math.min
import androidx.core.graphics.createBitmap

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
    private val workspace: Array<Bitmap?>
    private val bitmaps: MutableList<Bitmap>?
    private var placeholder: VectorDrawable? = null
    private var checkedIcon: VectorDrawable? = null
    private val backgroundBounds: Array<RectF>?
    private val inBounds: Array<Rect?>?
    private var avatarText: String? = null
    private var textStartXPoint = 0f
    private var textStartYPoint = 0f
    private var color = 0
    private val clipPaint: Array<Paint>?
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        typeface = Typeface.SANS_SERIF
    }
    private val presenceFillPaint: Paint
    private val presenceStrokePaint: Paint
    @ColorInt
    private val presenceConnectedColor: Int
    @ColorInt
    private val presenceAvailableColor: Int
    private val checkedPaint: Paint
    private val cropCircle: Boolean
    private val groupCircle: Boolean = false
    private var presenceStatus = Contact.PresenceStatus.OFFLINE
    private var isChecked = false
    private var showPresence = false

    companion object {
        private val TAG = AvatarDrawable::class.simpleName!!
        private const val SIZE_AB = 36
        private const val SIZE_BORDER = 2
        private const val DEFAULT_TEXT_SIZE_PERCENTAGE = 0.5f
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
        private val drawPaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        fun load(context: Context, account: Account, crop: Boolean = true): Observable<AvatarDrawable> =
            VCardServiceImpl.loadProfile(context, account)
                .map { profile -> build(context, account, profile, crop) }

        fun build(context: Context, account: Account, profile: Profile, crop: Boolean = true, presenceStatus: Contact.PresenceStatus = Contact.PresenceStatus.OFFLINE) =
            Builder()
                .withPhoto(profile.avatar as Bitmap?)
                .withNameData(profile.displayName, account.registeredName)
                .withUri(
                    if (account.isSip) Uri(Uri.SIP_URI_SCHEME, account.uri!!)
                    else Uri(Uri.JAMI_URI_SCHEME, account.username!!)
                )
                .withCircleCrop(crop)
                .withOnlineState(presenceStatus)
                .build(context)

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
                h = b / bw
            } else {
                w = a / bh
                h = ih
            }
            val x = (iw - w) / 2
            val y = (ih - h) / 2
            if (ret is Rect) ret.set(x, y, x + w, y + h)
            else if (ret is RectF) ret.set(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
        }

        @ColorRes
        private fun getAvatarColor(id: String?): Int {
            if (id.isNullOrBlank()) {
                return R.color.grey_500
            }
            val md5 = HashUtils.md5(id).toHex()
            val index = Integer.parseInt(md5[0].toString(), 16)
            return contactColors[index % contactColors.size]
        }
    }

    class Builder {
        private var photos: MutableList<Bitmap>? = null
        private var name: String? = null
        private var id: String? = null
        private var circleCrop = false
        private var presenceStatus = Contact.PresenceStatus.OFFLINE
        private var showPresence = true
        private var isChecked = false
        private var isGroup = false
        fun withUri(uri: Uri): Builder {
            this.id = uri.rawUriString
            return this
        }
        fun withId(id: String): Builder {
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
            this.name = name?.trim()
            return this
        }

        fun withCircleCrop(crop: Boolean): Builder {
            circleCrop = crop
            return this
        }

        fun withOnlineState(status: Contact.PresenceStatus): Builder {
            this.presenceStatus = status
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

        fun withNameData(profileName: String?, username: String?) =
            withName(if (profileName.isNullOrEmpty()) username else profileName)

        fun withContact(contact: ContactViewModel?) = if (contact == null) this else
            withPhoto(contact.profile.avatar as? Bitmap?)
                .withUri(contact.contact.uri)
                .withPresence(contact.presence != Contact.PresenceStatus.OFFLINE)
                .withOnlineState(contact.presence)
                .withNameData(contact.profile.displayName, contact.registeredName)

        private fun withContacts(profile: Profile, contacts: List<ContactViewModel>): Builder {
            if (profile !== Profile.EMPTY_PROFILE) {
                if (!profile.displayName.isNullOrBlank())
                    withName(profile.displayName)
                if (profile.avatar != null) {
                    return withPhoto(profile.avatar as? Bitmap?)
                }
            }
            val bitmaps: MutableList<Bitmap> = ArrayList(contacts.size)
            var notTheUser = 0
            for (contact in contacts) {
                if (contact.contact.isUser) continue
                notTheUser++
                val bitmap = contact.profile.avatar as? Bitmap?
                if (bitmap != null) {
                    bitmaps.add(bitmap)
                }
                if (bitmaps.size == 4) break
            }
            if (notTheUser == 1) {
                for (contact in contacts) {
                    if (!contact.contact.isUser) return withContact(contact)
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

        fun withConversation(conversation: Conversation, profile: Profile, contacts: List<ContactViewModel>): Builder =
            if (conversation.isSwarm && conversation.mode.blockingFirst() != Conversation.Mode.OneToOne)
                withUri(conversation.uri)
                    .withContacts(profile, contacts)
                    .setGroup()
            else
                withContact(ConversationItemViewModel.getContact(contacts))

        private fun setGroup(): Builder {
            isGroup = true
            return this
        }

        fun withViewModel(vm: ConversationItemViewModel): Builder =
            if (vm.isGroup())
                withUri(vm.uri)
                    .withContacts(vm.conversationProfile, vm.contacts)
                    .setGroup()
            else withContact(ConversationItemViewModel.getContact(vm.contacts))
                .withPresence(vm.showPresence)
                .withOnlineState(vm.presenceStatus)
                .withCheck(vm.isChecked)

        fun build(context: Context): AvatarDrawable =
            AvatarDrawable(context, photos, name, id, circleCrop, isGroup).also {
                it.setPresenceStatus(presenceStatus)
                it.setChecked(isChecked)
                it.showPresence = showPresence
            }

        fun buildAsync(context: Context): Single<AvatarDrawable> = Single.fromCallable { build(context) }
    }

    fun update(contact: ContactViewModel) {
        avatarText = convertNameToAvatarText(contact.profile.displayName)
        contact.profile.avatar?.let { photo ->
            bitmaps?.set(0, photo as Bitmap)
        }
        presenceStatus = contact.presence
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

    fun setPresenceStatus(status: Contact.PresenceStatus) {
        presenceStatus = status
        presenceFillPaint.color = if (status == Contact.PresenceStatus.CONNECTED) presenceConnectedColor else presenceAvailableColor
    }

    fun setChecked(checked: Boolean) {
        isChecked = checked
    }

    /** Should only be used in tests */
    fun getBitmap(): MutableList<Bitmap>? = bitmaps

    @SuppressLint("UseCompatLoadingForDrawables")
    private constructor(
        context: Context,
        photos: MutableList<Bitmap>?,
        name: String?,
        id: String?,
        crop: Boolean,
        group: Boolean
    ) {
        //Log.w("AvatarDrawable", "AvatarDrawable name:$name id:$id");
        cropCircle = crop
        isGroup = group
        minSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SIZE_AB.toFloat(), context.resources.displayMetrics).toInt()
        val borderSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SIZE_BORDER.toFloat(), context.resources.displayMetrics)
        /*if (cropCircle) {
            inSize = minSize
        }*/
        if (photos != null && photos.isNotEmpty()) {
            avatarText = null
            bitmaps = photos
            if (photos.size == 1) {
                backgroundBounds = arrayOf(RectF())
                inBounds = arrayOf(null)
                clipPaint = if (cropCircle) arrayOf(Paint()) else null
                workspace = arrayOf(null)
            } else {
                backgroundBounds = Array(bitmaps.size) { RectF() }
                inBounds = if (cropCircle && groupCircle) arrayOfNulls(bitmaps.size) else Array(bitmaps.size) { Rect() }
                clipPaint = if (cropCircle) Array(if (groupCircle) bitmaps.size else 1) { Paint().apply {
                    strokeWidth = borderSize
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }} else null
                workspace =
                    if (cropCircle) arrayOfNulls(if (groupCircle) bitmaps.size else 1) else arrayOf(null)
            }
        } else {
            workspace = arrayOf(null)
            bitmaps = null
            backgroundBounds = null
            inBounds = null
            avatarText = convertNameToAvatarText(name)
            color = ContextCompat.getColor(context, getAvatarColor(id))
            clipPaint = if (cropCircle) arrayOf(Paint()) else null
            if (avatarText == null) {
                placeholder = context.getDrawable(
                    if (isGroup) R.drawable.baseline_group_24
                    else R.drawable.baseline_account_crop_24
                )?.mutate() as VectorDrawable?
            } else {
                textPaint.color = Color.WHITE
                textPaint.typeface = Typeface.SANS_SERIF
            }
        }
        presenceAvailableColor = ContextCompat.getColor(context, R.color.available_indicator)
        presenceConnectedColor = ContextCompat.getColor(context, R.color.online_indicator)
        presenceFillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = presenceConnectedColor
        }

        val typedValue = TypedValue()
        val presenceStrokeColor =
            if (isTv(context)) ContextCompat.getColor(context, R.color.grey_900)
            else if ( // Checks if the theme has a colorBackground attribute (false if not).
                context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
            ) typedValue.data
            else ContextCompat.getColor(context, R.color.background)
        presenceStrokePaint = Paint().apply {
            color = presenceStrokeColor
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        checkedIcon = context.getDrawable(R.drawable.baseline_check_circle_24) as VectorDrawable?
        checkedIcon?.setTint(ContextCompat.getColor(context, R.color.colorPrimary))
        checkedPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.background)
            style = Paint.Style.FILL_AND_STROKE
            isAntiAlias = true
        }
        if (clipPaint != null) for (p in clipPaint) p.isAntiAlias = true
    }

    constructor(other: AvatarDrawable) {
        //Log.w("AvatarDrawable", "AvatarDrawable copy");
        cropCircle = other.cropCircle
        isGroup = other.isGroup
        minSize = other.minSize
        bitmaps = other.bitmaps
        backgroundBounds = if (other.backgroundBounds != null)
            Array(other.backgroundBounds.size) { RectF() }
        else null
        inBounds = other.inBounds
        color = other.color
        placeholder = other.placeholder
        avatarText = other.avatarText
        workspace = arrayOfNulls(other.workspace.size)
        clipPaint = if (other.clipPaint == null) null else Array(other.clipPaint.size) { i -> Paint(other.clipPaint[i]).apply {
            shader = null
        } }
        presenceStatus = other.presenceStatus
        isChecked = other.isChecked
        showPresence = other.showPresence
        presenceConnectedColor = other.presenceConnectedColor
        presenceAvailableColor = other.presenceAvailableColor
        presenceFillPaint = other.presenceFillPaint
        presenceStrokePaint = other.presenceStrokePaint
        checkedPaint = other.checkedPaint
    }

    class AvatarConstantState(private val avatarDrawable: AvatarDrawable) : ConstantState() {
        override fun newDrawable(): AvatarDrawable = AvatarDrawable(avatarDrawable)

        override fun getChangingConfigurations(): Int = 0
    }

    override fun getConstantState(): ConstantState = AvatarConstantState(this)

    override fun draw(finalCanvas: Canvas) {
        val firstWorkspace = workspace[0] ?: return
        if (update) {
            var i = 0
            while (i < workspace.size) {
                drawActual(i, Canvas(workspace[i]!!))
                i++
            }
            update = false
        }
        if (cropCircle) {
            finalCanvas.save()
            finalCanvas.translate(
                bounds.left + (bounds.width() - firstWorkspace.width) / 2f,
                bounds.top + (bounds.height() - firstWorkspace.height) / 2f
            )
            var r = (min(firstWorkspace.width, firstWorkspace.height) / 2).toFloat()
            val cx = firstWorkspace.width / 2 //getBounds().centerX();
            var cy = (firstWorkspace.height / 2).toFloat() //getBounds().height() / 2;
            if (groupCircle) {
                val ratio = 1.333333f
                for ((i, paint) in clipPaint!!.withIndex()) {
                    finalCanvas.drawCircle(cx.toFloat(), firstWorkspace.height - cy, r, paint)
                    if (i != 0) {
                        val s = paint.shader
                        paint.shader = null
                        paint.style = Paint.Style.STROKE
                        finalCanvas.drawCircle(cx.toFloat(), firstWorkspace.height - cy, r, paint)
                        paint.shader = s
                        paint.style = Paint.Style.FILL
                    }
                    r /= ratio
                    cy /= ratio
                }
            } else {
                finalCanvas.drawCircle(cx.toFloat(), firstWorkspace.height - cy, r, clipPaint!![0])
            }
            finalCanvas.restore()
        } else {
            finalCanvas.drawBitmap(firstWorkspace, null, bounds, drawPaint)
        }
        if (showPresence && presenceStatus != Contact.PresenceStatus.OFFLINE) {
            drawPresence(finalCanvas)
        }
        if (isChecked) {
            drawChecked(finalCanvas)
        }
    }

    private fun drawActual(i: Int, canvas: Canvas) {
        if (bitmaps != null) {
            if (cropCircle && groupCircle) {
                canvas.drawBitmap(bitmaps[i], inBounds!![i], backgroundBounds!![i], drawPaint)
            } else {
                if (backgroundBounds!!.size == bitmaps.size) {
                    if (bitmaps.size == 1) {
                        canvas.drawBitmap(bitmaps[0], inBounds!![0], backgroundBounds[0], drawPaint)
                    } else {
                        var n = 0
                        val s = bitmaps.size
                        while (n < s) {
                            canvas.drawBitmap(bitmaps[n], inBounds!![n], backgroundBounds[n], drawPaint)
                            n++
                        }
                    }
                }
            }
        } else {
            canvas.drawColor(color)
            if (avatarText != null) {
                canvas.drawText(avatarText!!, textStartXPoint, textStartYPoint, textPaint)
            } else {
                placeholder?.let {
                    it.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    it.draw(canvas)
                }
            }
        }
    }

    private fun setupPresenceIndicator(bounds: Rect) {
        presence.radius = (0.29289321881 * bounds.width().toDouble() * 0.5).toInt()
        presence.cx = bounds.right - presence.radius
        presence.cy = bounds.bottom - presence.radius
        val presenceStrokeWidth = (presence.radius / 4).toFloat()
        presenceStrokePaint.strokeWidth = presenceStrokeWidth
        checkedPaint.strokeWidth = presenceStrokeWidth
        presence.radius -= (presenceStrokeWidth * 0.5f).toInt()
        checkedIcon?.setBounds(
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
        checkedIcon?.let { icon ->
            canvas.drawCircle(
                presence.cx.toFloat(),
                presence.cy.toFloat(),
                presence.radius.toFloat(),
                checkedPaint
            )
            icon.draw(canvas)
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        //if (showPresence)
        setupPresenceIndicator(bounds)
        val d = min(bounds.width(), bounds.height())
        val iw = if (cropCircle) d else bounds.width()
        val ih = if (cropCircle) d else bounds.height()
        placeholder?.let {
            val cx = (iw - d) / 2
            val cy = (ih - d) / 2
            it.setBounds(cx, cy, cx + d, cy + d)
        }
        for (i in workspace.indices) {
            if (workspace[i] != null) {
                workspace[i]!!.recycle()
                workspace[i] = null
                clipPaint?.get(i)?.shader = null//[i].shader = null
            }
        }
        if (iw <= 0 || ih <= 0) {
            return
        }
        if (cropCircle) {
            for (i in workspace.indices) {
                val workspacei = createBitmap(iw, ih)
                workspace[i] = workspacei
                clipPaint!![i].shader = BitmapShader(workspacei, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
        } else {
            workspace[0] = createBitmap(bounds.width(), bounds.height())
        }
        if (bitmaps != null) {
            if (bitmaps.size == 1 || (cropCircle && groupCircle)) {
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
                        fit(bitmap.width, bitmap.height, subBounds.width(), subBounds.height(), false, inBounds!![i])
                        backgroundBounds!![i].set(subBounds)
                    }
                }
            }
        } else {
            setAvatarTextValues(iw, ih)
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

    override fun getMinimumWidth(): Int = minSize

    override fun getMinimumHeight(): Int = minSize

    fun setInSize(s: Int): AvatarDrawable {
        inSize = s
        return this
    }

    override fun getIntrinsicWidth(): Int = inSize

    override fun getIntrinsicHeight(): Int = inSize

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun setAvatarTextValues(width: Int, height: Int) {
        if (avatarText != null) {
            textPaint.textSize = height * DEFAULT_TEXT_SIZE_PERCENTAGE
            val stringWidth = textPaint.measureText(avatarText ?: return)
            textStartXPoint = width / 2f - stringWidth / 2f
            textStartYPoint = height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
        }
    }

    private fun convertNameToAvatarText(name: String?): String? = if (name.isNullOrEmpty()) null
        else String(Character.toChars(name.codePointAt(0))).uppercase(Locale.getDefault())
}