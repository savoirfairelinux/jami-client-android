package cx.ring.service

/**
 * A bounded opportunity for asynchronous proxy/DHT recovery, not a sync-completion signal.
 * Only fresh call/message wakeups extend it; renewals and presence pushes do not start an FGS.
 */
internal class PushRecoveryWindow {
    private var startedAt: Long? = null

    fun extend(now: Long): Long {
        val start = startedAt ?: now.also { startedAt = it }
        return minOf(RECOVERY_MS, (start + MAX_EPISODE_MS - now).coerceAtLeast(0L))
    }

    companion object {
        // Keep recovery alive beyond the 30s DHT request timeout.
        const val RECOVERY_MS = 60_000L
        const val MAX_EPISODE_MS = 10 * 60_000L
    }
}
