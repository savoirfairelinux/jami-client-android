package cx.ring.views

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import cx.ring.R
import cx.ring.databinding.ItemParticipantLabelBinding
import cx.ring.fragments.CallFragment
import net.jami.model.Conference.ParticipantInfo
import kotlin.math.max
import kotlin.math.min

class ParticipantsContainerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    var participants: List<ParticipantInfo> = ArrayList()

    fun init() {
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

        val maxCol = if (grid) (if (mainWidth < mainHeight) 1 else 3) else 3
        val maxRow = if (grid) (if (mainWidth < mainHeight) 3 else 1) else 3
        val activeSeparation = if (inactiveParticipants == 0) 0f else .2f
        val activeWidth = 1f/activeParticipants
        val inactiveMaxCols = min(maxCol, inactiveParticipants)
        val inactiveMaxRows = min(maxRow, inactiveParticipants)
        val inactiveWidth = if (grid) 1f/inactiveMaxCols else if(mainWidth < mainHeight) min(1f/inactiveMaxCols, .5f) else activeSeparation
        val gridRows = if (grid) inactiveParticipants / inactiveMaxCols else 0
        val inactiveHeight = if (grid) (1f / gridRows) else if (mainWidth < mainHeight) activeSeparation else min(1f/inactiveMaxRows, .5f)
        val margin = if (participants.size < 2) 0 else context.resources.getDimensionPixelSize(
            R.dimen.call_participant_margin)
        val cornerRadius = if (participants.size < 2) 0f else context.resources.getDimension(
            R.dimen.call_participant_corner_radius)

        Log.w(CallFragment.TAG, "generateParticipantOverlay count:${participants.size} grid:$grid ($maxCol by $gridRows) active:$activeWidth inactive: $inactiveWidth $inactiveHeight")

        var iActive = 0
        var iInactive = 0
        val toAdd: MutableList<View> = java.util.ArrayList()
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
            participantInfoOverlay.sink.setSinkId(i.sinkId)

            participantInfoOverlay.participantName.text = i.contact.displayName
            participantInfoOverlay.mute.isVisible = i.audioModeratorMuted || i.audioLocalMuted

            val col = if (i.active) iActive.toFloat() else (if (grid) (iInactive % inactiveMaxCols).toFloat() else if (mainWidth < mainHeight) iInactive.toFloat() else 0f)
            val row = if (mainWidth < mainHeight) 0f else iInactive.toFloat()
            val x = if (i.active) col * activeWidth else col * inactiveWidth
            val y = if (i.active) activeSeparation else (if (grid) ((iInactive / inactiveMaxCols).toFloat()/gridRows) else row * inactiveHeight)
            val w = if (i.active) activeWidth else inactiveWidth
            val h = if (i.active) 1f-activeSeparation else inactiveHeight
            Log.w(CallFragment.TAG, "generateParticipantOverlay index $iActive $iInactive x:$x y:$y w:$w h:$h")

            val inactiveParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = (x * mainWidth).toInt() + margin
                topMargin = (y * mainHeight).toInt() + margin
                width = (w * mainWidth).toInt() - 2 * margin
                height = (h * mainHeight).toInt() - 2 * margin
            }

            val activeParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = if (mainWidth < mainHeight) (x * mainWidth).toInt() + margin else (inactiveWidth * mainHeight).toInt()
                rightMargin = if (mainWidth < mainHeight) leftMargin else leftMargin / 2
                topMargin = if (mainWidth < mainHeight) (inactiveHeight * mainHeight).toInt() else margin
                bottomMargin = if (mainWidth < mainHeight) topMargin / 2 else topMargin
                gravity = Gravity.CENTER
            }

            post { participantInfoOverlay.root.layoutParams = if (!i.active) inactiveParams else activeParams }
            if (view == null)
                toAdd.add(participantInfoOverlay.root)

            if (i.active)
                iActive++
            else
                iInactive++
        }

        for (v in toAdd) addView(v)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        init()
    }

}