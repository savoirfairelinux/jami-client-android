package cx.ring.views

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import cx.ring.R
import cx.ring.databinding.ItemParticipantLabelBinding
import net.jami.model.Conference.ParticipantInfo
import kotlin.math.max
import kotlin.math.min

class ParticipantsContainerView// adding name, mic etc..
    (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0, 0)

    var participants: List<ParticipantInfo> = ArrayList()
    private val hscroll = HorizontalScrollView(context)
    private val vscroll = ScrollView(context)
    private val ll = FrameLayout(context)
    private var pipMode = false

    init {
        hscroll.tag = "scroll"
        ll.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        vscroll.addView(ll)
        hscroll.addView(vscroll)
        addView(hscroll)
    }

    fun initialize() {
        // If there is no participants, we can remove all the views.
        if (participants.isEmpty()) {
            removeAllViews()
            return
        }

        // Used to know which participant to display in PiP mode.
        val pipFocusParticipant = getPipFocusParticipant()

        val toRemove: MutableList<View> = ArrayList()
        for (childView in children) {
            val tag = childView.tag as String?
            if (tag.isNullOrEmpty() // Remove all the views that are not participants.
                || (participants.firstOrNull { (it.tag) == tag } == null && tag != "scroll"))
                toRemove.add(childView)
            else // Hide the view if it is not the focused participant in PiP mode.
                childView.isVisible = !(pipMode && pipFocusParticipant?.tag != tag)
        }
        toRemove.forEach { removeView(it) }

        // Active participants are participants on which the interest is focused.
        // It generally means that they are the ones talking. So they are usually full screen.
        val activeParticipants = participants.count { it.active }
        val inactiveParticipants = participants.size - activeParticipants

        // There is some display modes:
        // - If there is no active participants, we display all the participants in a grid.
        // - If there is active participants, we display them either :
        //     - Full screen
        //     - Semi full screen, with top panel where we display the other participants

        // Manage display modes
        val grid = activeParticipants == 0
        val mainWidth = width.toFloat()
        val mainHeight = height.toFloat()
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
        val inactiveOffset =
            if (!grid && inactiveCount < centerMaxInactive)
                (1f - inactiveSize * inactiveCount) / 2f
            else 0f
        val inactiveOffsetX = if (portrait) inactiveOffset else 0f
        val inactiveOffsetY = if (portrait) 0f else inactiveOffset
        val inactiveWidth =
            if (grid) 1f/inactiveMaxCols else if (portrait) inactiveSize else activeSeparation
        val gridRows =
            if (grid) inactiveParticipants / inactiveMaxCols else 0
        val inactiveHeight =
            if (grid) (1f / gridRows) else if (portrait) activeSeparation else inactiveSize
        val margin =
            if (participants.size < 2) 0
            else context.resources.getDimensionPixelSize(R.dimen.call_participant_margin)
        val cornerRadius =
            if (participants.size < 2) 0f
            else context.resources.getDimension(R.dimen.call_participant_corner_radius)

        var iActive = 0
        var iInactive = 0
        val addToMain: MutableList<View> = ArrayList()
        val addToScroll: MutableList<View> = ArrayList()
        for (participantInfo in participants) {
            val viewTag = participantInfo.sinkId ?: participantInfo.contact.contact.uri.uri
            val view: View? = findViewWithTag(viewTag)

            // Create or recycle the view for each participants
            val participantInfoOverlay =
                if (view != null) // Recycle the view
                    ItemParticipantLabelBinding.bind(view)
                else // Create a new one
                    ItemParticipantLabelBinding.inflate(LayoutInflater.from(context))
                        .apply {
                            root.tag = viewTag
                        }

            val isPipFocus = pipFocusParticipant?.tag == participantInfo.tag
            participantInfoOverlay.root.isVisible = !pipMode || isPipFocus
            participantInfoOverlay.sink.setFitToContent(participantInfo.active && !pipMode)
            participantInfoOverlay.root.radius = if(pipMode) 0f else cornerRadius

            // If there is no video available, we display the avatar
            participantInfoOverlay.sink.videoListener = { hasVideo ->
                participantInfoOverlay.avatar.isVisible = !hasVideo
                if (!hasVideo) {
                    participantInfoOverlay.avatar.setImageDrawable(AvatarDrawable.Builder()
                        .withContact(participantInfo.contact)
                        .withCircleCrop(true)
                        .build(context))
                }
            }

            // Adjust avatar layout params
            participantInfoOverlay.avatar.updateLayoutParams {
                if (participantInfo.active) {
                    val size = context.resources
                        .getDimensionPixelSize(R.dimen.call_participant_avatar_size)
                    width = size
                    height = size
                } else {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }

            participantInfoOverlay.sink.setSinkId(participantInfo.sinkId)

            // Set up name and mute icon
            participantInfoOverlay.participantName.text = participantInfo.contact.displayName
            participantInfoOverlay.mute.isVisible =
                participantInfo.audioModeratorMuted || participantInfo.audioLocalMuted

            val layoutWidth =
                if (portrait) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
            val layoutHeight =
                if (portrait) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
            val layoutParams = LayoutParams(layoutWidth, layoutHeight)
            hscroll.layoutParams = layoutParams
            vscroll.layoutParams = layoutParams

            val scrollable = !grid && !participantInfo.active
            val col =
                if (participantInfo.active) iActive.toFloat()
                else
                    if (grid) (iInactive % inactiveMaxCols).toFloat()
                    else if (portrait) iInactive.toFloat()
                    else 0f

            val row = if (portrait) 0f else iInactive.toFloat()
            val x =
                if (participantInfo.active) col * activeWidth
                else inactiveOffsetX + col * inactiveWidth
            val y =
                if (participantInfo.active) activeSeparation
                else
                    if (grid) ((iInactive / inactiveMaxCols).toFloat() / gridRows)
                    else inactiveOffsetY + row * inactiveHeight
            val w = if (participantInfo.active) activeWidth else inactiveWidth
            val h = if (participantInfo.active) 1f-activeSeparation else inactiveHeight

            val params = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if(pipMode && isPipFocus) {
                    leftMargin = 0
                    rightMargin =  0
                    topMargin = 0
                    bottomMargin = 0
                    width = mainWidth.toInt()
                    height = mainHeight.toInt()
                }
                else if (participantInfo.active) {
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
                    bottomMargin = if (scrollable) margin else 0
                    rightMargin = if (scrollable) margin else 0
                }
            }

            if (view == null) {
                participantInfoOverlay.root.layoutParams = params
                if (scrollable) {
                    addToScroll.add(participantInfoOverlay.root)
                } else {
                    addToMain.add(participantInfoOverlay.root)
                }
            } else {
                post { participantInfoOverlay.root.layoutParams = params }
                if (scrollable && participantInfoOverlay.root.parent != ll) {
                    removeView(participantInfoOverlay.root)
                    addToScroll.add(participantInfoOverlay.root)
                } else if (!scrollable && participantInfoOverlay.root.parent == ll) {
                    ll.removeView(participantInfoOverlay.root)
                    addToMain.add(participantInfoOverlay.root)
                }
            }

            if (participantInfo.active)
                iActive++
            else
                iInactive++
        }

        for (v in addToMain) addView(v)
        for (v in addToScroll) ll.addView(v)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        initialize()
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun togglePipMode(isInPip: Boolean) {
        pipMode = isInPip
    }

    /**
     * Get the participant to focus in PiP mode.
     * Usually the first active participant is chosen.
     */
    private fun getPipFocusParticipant() = participants
        .sortedWith(compareBy({!it.active},{it.contact.contact.isUser}))
        .firstOrNull()
}