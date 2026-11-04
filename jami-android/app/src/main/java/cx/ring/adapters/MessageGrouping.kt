package cx.ring.adapters

import net.jami.model.interaction.Interaction
import net.jami.model.interaction.Interaction.InteractionType
import net.jami.utils.StringUtils
import java.util.concurrent.TimeUnit

internal object MessageGrouping {
    val MAX_GAP_MILLIS = TimeUnit.HOURS.toMillis(1)

    /** Neighbors are chronological; replies start a group, but may be followed by other messages. */
    fun isSeqBreak(first: Interaction, second: Interaction): Boolean =
        isAlwaysSingleMsg(first)
                || isAlwaysSingleMsg(second)
                || first.isIncoming != second.isIncoming
                || second.replyToId != null
                || second.replyTo != null
                || first.contact != second.contact
                || second.timestamp - first.timestamp > MAX_GAP_MILLIS

    fun isAlwaysSingleMsg(message: Interaction): Boolean = when (message.type) {
        InteractionType.TEXT -> StringUtils.isOnlyEmoji(message.body)
        InteractionType.CALL, InteractionType.DATA_TRANSFER -> false
        else -> true
    }
}
