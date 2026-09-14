package net.jami.services

import io.reactivex.rxjava3.core.Completable
import net.jami.model.Uri

/** Native removeContact returns void, so success must be confirmed before discarding an invitation. */
internal class ContactBlockOperation(
    private val canBlock: (String, Uri) -> Boolean,
    private val remove: (String, Uri) -> Unit,
    private val isBlocked: (String, Uri) -> Boolean
) {
    fun block(accountId: String, target: Uri): Completable = Completable.fromAction {
        check(canBlock(accountId, target)) { "Cannot block own identity or an unavailable contact" }
        remove(accountId, target)
        check(isBlocked(accountId, target)) { "The contact could not be blocked" }
    }

    companion object {
        fun afterSuccess(block: Completable, action: () -> Unit): Completable =
            block.andThen(Completable.fromAction(action))
    }
}
