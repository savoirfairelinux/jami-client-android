package cx.ring.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import cx.ring.R
import cx.ring.databinding.ItemParticipantLabelBinding
import net.jami.model.Conference.ParticipantInfo
import kotlin.math.max
import kotlin.math.min

class ParticipantsContainerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    var participants: List<ParticipantInfo> = ArrayList()

    fun init() {
        if (participants.isEmpty()) {
            removeAllViews()
            return
        }

        val inflater = LayoutInflater.from(context)

        val mainWidth = width.toFloat()
        val mainHeight = height.toFloat()

        for (childView in children) {
            val tag = childView.tag  as String?
            if (tag.isNullOrEmpty() || participants.firstOrNull { (it.sinkId ?: it.contact.contact.uri.uri) == tag } == null) removeView(childView)
        }
        val activeParticipants = participants.count { it.active }
        val inactiveParticipants = participants.size - activeParticipants
        val grid = activeParticipants == 0

        val portrait = mainWidth < mainHeight
        val maxCol = if (grid) (if (portrait) 1 else 3) else 3
        val maxRow = if (grid) (if (portrait) 3 else 1) else 3
        val activeSeparation = if (inactiveParticipants == 0) 0f else .2f
        val activeWidth = 1f/activeParticipants
        val inactiveMaxCols = min(maxCol, inactiveParticipants)
        val inactiveMaxRows = min(maxRow, inactiveParticipants)
        val inactiveCount = if (portrait) inactiveMaxCols else inactiveMaxRows
        val centerMaxInactive = 2
        val inactiveSize = 1f / max(inactiveCount, centerMaxInactive)
        val inactiveOffset = if (!grid && inactiveCount < centerMaxInactive) ((1f - inactiveSize * inactiveCount)/2f) else 0f
        val inactiveOffsetX = if (portrait) inactiveOffset else 0f
        val inactiveOffsetY = if (portrait) 0f else inactiveOffset
        val inactiveWidth = if (grid) 1f/inactiveMaxCols else if (portrait) inactiveSize else activeSeparation
        val gridRows = if (grid) inactiveParticipants / inactiveMaxCols else 0
        val inactiveHeight = if (grid) (1f / gridRows) else if (portrait) activeSeparation else inactiveSize
        val margin = if (participants.size < 2) 0 else context.resources.getDimensionPixelSize(
            R.dimen.call_participant_margin)
        val cornerRadius = if (participants.size < 2) 0f else context.resources.getDimension(
            R.dimen.call_participant_corner_radius)

        var iActive = 0
        var iInactive = 0
        val toAdd: MutableList<View> = ArrayList()
        for (i in participants) {
            if (!i.active && !grid && (iInactive >= maxCol || iInactive >= maxRow))
                break

            val viewTag = i.sinkId ?: i.contact.contact.uri.uri
            val view: View? = findViewWithTag(viewTag)
            // adding name, mic etc..
            val participantInfoOverlay = if (view != null) ItemParticipantLabelBinding.bind(view) else ItemParticipantLabelBinding.inflate(inflater).apply {
                root.tag = viewTag
            }

            participantInfoOverlay.root.radius = cornerRadius
            participantInfoOverlay.sink.setFitToContent(i.active)
            participantInfoOverlay.sink.videoListener = { hasVideo ->
                participantInfoOverlay.avatar.isVisible = !hasVideo
                if (!hasVideo) {
                    participantInfoOverlay.avatar.setImageDrawable(AvatarDrawable.Builder()
                        .withContact(i.contact)
                        .withCircleCrop(true)
                        .build(context))
                }
            }
            participantInfoOverlay.avatar.updateLayoutParams {
                if (i.active) {
                    val size = context.resources.getDimensionPixelSize(R.dimen.call_participant_avatar_size)
                    width = size
                    height = size
                } else {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
            participantInfoOverlay.sink.setSinkId(i.sinkId)

            participantInfoOverlay.participantName.text = i.contact.displayName
            participantInfoOverlay.mute.isVisible = i.audioModeratorMuted || i.audioLocalMuted

            val col = if (i.active) iActive.toFloat() else (if (grid) (iInactive % inactiveMaxCols).toFloat() else if (portrait) iInactive.toFloat() else 0f)
            val row = if (portrait) 0f else iInactive.toFloat()
            val x = if (i.active) col * activeWidth else inactiveOffsetX + col * inactiveWidth
            val y = if (i.active) activeSeparation else (if (grid) ((iInactive / inactiveMaxCols).toFloat()/gridRows) else inactiveOffsetY + row * inactiveHeight)
            val w = if (i.active) activeWidth else inactiveWidth
            val h = if (i.active) 1f-activeSeparation else inactiveHeight

            val params = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (i.active) {
                    leftMargin = if (portrait) (x * mainWidth).toInt() + margin else (inactiveWidth * mainWidth).toInt()
                    rightMargin = if (portrait) leftMargin else leftMargin / 2
                    topMargin = if (portrait) (inactiveHeight * mainHeight).toInt() else margin
                    bottomMargin = if (portrait) topMargin / 2 else topMargin
                    gravity = Gravity.CENTER
                } else {
                    leftMargin = (x * mainWidth).toInt() + margin
                    topMargin = (y * mainHeight).toInt() + margin
                    width = (w * mainWidth).toInt() - 2 * margin
                    height = (h * mainHeight).toInt() - 2 * margin
                }
            }

            if (view == null) {
                participantInfoOverlay.root.layoutParams = params
                toAdd.add(participantInfoOverlay.root)
            } else {
                post { participantInfoOverlay.root.layoutParams = params }
            }

            if (i.active)
                iActive++
            else
                iInactive++
        }

        for (v in toAdd) addView(v)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        init()
        super.onSizeChanged(w, h, oldw, oldh)
    }

}