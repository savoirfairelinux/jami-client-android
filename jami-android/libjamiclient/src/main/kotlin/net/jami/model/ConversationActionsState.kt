package net.jami.model

import io.reactivex.rxjava3.core.Observable

/** Immutable rendering state for the conversation details actions. */
data class ConversationActionsState(
    val type: Type,
    val deleteAction: DeleteAction,
    val blockAction: BlockAction,
    val showDelete: Boolean,
    val showRemove: Boolean,
    val showDetails: Boolean,
    val showActions: Boolean,
    val contactUri: Uri?,
    val registeredName: String = ""
) {
    enum class Type { CONTACT, PRIVATE, GROUP }
    enum class DeleteAction { ADD_CONTACT, ACCEPT_INVITATION, DELETE_CONTACT, LEAVE_CONVERSATION }
    enum class BlockAction { NONE, BLOCK, UNBLOCK }

    val showPrivate: Boolean get() = type != Type.GROUP && contactUri != null
    val showUsername: Boolean get() = showPrivate && registeredName.isNotEmpty()
    val showDescription: Boolean get() = type == Type.GROUP

    companion object {
        fun from(conversation: Conversation, account: Account?): ConversationActionsState {
            val group = conversation.isSwarmGroup()
            val legacy = conversation.isLegacy()
            val request = conversation.mode.blockingFirst() == Conversation.Mode.Request
            val target = account?.let {
                ContactBlockingPolicy.target(conversation, it.username, it.isJami)
            }
            val blocked = !group && target?.let {
                (account?.getContact(it.uri) ?: it).isBlocked
            } == true
            return ConversationActionsState(
                type = when {
                    !conversation.isSwarm -> Type.CONTACT
                    group -> Type.GROUP
                    else -> Type.PRIVATE
                },
                deleteAction = when {
                    legacy -> DeleteAction.ADD_CONTACT
                    request -> DeleteAction.ACCEPT_INVITATION
                    group -> DeleteAction.LEAVE_CONVERSATION
                    else -> DeleteAction.DELETE_CONTACT
                },
                blockAction = when {
                    legacy || group || target == null -> BlockAction.NONE
                    request -> BlockAction.BLOCK
                    blocked -> BlockAction.UNBLOCK
                    else -> BlockAction.BLOCK
                },
                showDelete = if (legacy) account != null && !account.isSip else !blocked,
                showRemove = !legacy && !group && !request && !blocked,
                showDetails = !legacy && !blocked,
                showActions = !legacy && !request && !blocked,
                contactUri = if (group) null else conversation.contacts.firstOrNull { !it.isUser }?.uri
            )
        }

        fun observe(
            conversation: Conversation,
            accounts: Observable<List<Account>>
        ): Observable<ConversationActionsState> =
            accounts.switchMap { list ->
                val account = list.firstOrNull { it.accountId == conversation.accountId }
                val contactStatusUpdates = account?.blockedContactsUpdates
                    ?.map { Unit }?.startWithItem(Unit) ?: Observable.just(Unit)
                Observable.combineLatest(
                    conversation.contactUpdates.startWithItem(conversation.contacts),
                    conversation.mode,
                    contactStatusUpdates
                ) { _, _, _ ->
                    // Account updates reuse mutable contacts/collections: snapshot before equality
                    // checks or scheduling, not after comparing Contact identities.
                    val state = from(conversation, account)
                    state to conversation.contacts.firstOrNull { it.uri == state.contactUri }?.username
                }
            }
                .distinctUntilChanged()
                .switchMap { (state, username) ->
                    username?.toObservable()
                        ?.onErrorReturnItem("")
                        ?.map { state.copy(registeredName = it) }
                        ?.startWithItem(state)
                        ?: Observable.just(state)
                }
                .distinctUntilChanged()
    }
}
