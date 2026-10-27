/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.repositories

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import net.jami.model.Conversation
import net.jami.model.Target
import net.jami.model.TargetCapability
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TargetRepository @Inject constructor(
    private val accountService: AccountService,
    private val conversationFacade: ConversationFacade,
    private val circleRepository: CircleRepository,
    private val boardRepository: BoardRepository
) {

    /**
     * Observes all available targets (Contacts, Groups, Circles, Boards) for the current account,
     * optionally filtered by capability.
     */
    fun observeTargets(capability: TargetCapability? = null): Observable<List<Target>> {
        val convWithVmObservable: Observable<List<Pair<Conversation, ConversationItemViewModel>>> =
            accountService.currentAccountSubject.switchMap { account ->
                account.getConversationsSubject().switchMap { conversations ->
                    if (conversations.isEmpty()) {
                        Observable.just(emptyList())
                    } else {
                        Observable.combineLatest(
                            conversations.map { conv ->
                                // Contacts carry their presence, drawn as the avatar badge
                                conversationFacade.observeConversation(account, conv, !conv.isSwarmGroup())
                                    .map { vm -> conv to vm }
                            }
                        ) { array ->
                            @Suppress("UNCHECKED_CAST")
                            array.map { it as Pair<Conversation, ConversationItemViewModel> }
                        }
                    }
                }
            }

        return Observable.combineLatest(
            convWithVmObservable,
            circleRepository.observeForCurrentAccount(),
            boardRepository.observeBoards()
        ) { convPairs, circles, boards ->
            val targets = ArrayList<Target>()
            val seenUris = HashSet<String>()
            val vmByConvId = HashMap<String, ConversationItemViewModel>()

            // 1. Contacts & Groups from conversations
            for ((conv, vm) in convPairs) {
                val uri = conv.uri
                vmByConvId[uri.rawRingId] = vm
                val peer = conv.contacts.firstOrNull { !it.isUser } ?: conv.contacts.firstOrNull()
                val title = vm.title.takeIf { it.isNotBlank() }
                    ?: vm.uriTitle.takeIf { it.isNotBlank() }
                    ?: conv.profile.blockingFirst().displayName?.takeIf { it.isNotBlank() }
                    ?: peer?.profile?.blockingFirst()?.displayName?.takeIf { it.isNotBlank() }
                    ?: peer?.username?.blockingGet()?.takeIf { it.isNotBlank() }
                    ?: uri.rawUriString

                // Boards are listed below, from the daemon's Feed list
                if (conv.isFeed() || conv.mode.blockingFirst() == Conversation.Mode.Public) {
                    continue
                }

                if (conv.isSwarmGroup()) {
                    val group = Target.GroupTarget(
                        uri = uri,
                        title = title,
                        memberCount = conv.contacts.size,
                        avatar = null,
                        viewModel = vm
                    )
                    if (capability == null || group.hasCapability(capability)) {
                        targets.add(group)
                    }
                } else {
                    val contactUri = peer?.uri ?: uri
                    if (seenUris.add(contactUri.rawUriString)) {
                        val username = peer?.username?.blockingGet()
                        val isOnline = vm.presenceStatus == net.jami.model.Contact.PresenceStatus.CONNECTED
                        val contactTarget = Target.ContactTarget(
                            uri = contactUri,
                            name = title,
                            username = username,
                            photoUri = null,
                            isOnline = isOnline,
                            viewModel = vm
                        )
                        if (capability == null || contactTarget.hasCapability(capability)) {
                            targets.add(contactTarget)
                        }
                    }
                }
            }

            // 2. Real Circles (formerly Squads)
            for (circle in circles) {
                if (circle.isAllContacts) continue
                val circleTarget = Target.CircleTarget(
                    circleId = circle.id,
                    name = circleRepository.displayName(circle),
                    memberCount = circle.totalCount,
                    uri = null
                )
                if (capability == null || circleTarget.hasCapability(capability)) {
                    targets.add(circleTarget)
                }
            }

            // 3. Boards (daemon Feeds) with a local conversation; only owners can publish
            for (board in boards) {
                if (!board.isFollowed || (capability == TargetCapability.CAN_POST && !board.owned))
                    continue
                val boardTarget = Target.BoardTarget(
                    boardId = board.uri.rawUriString,
                    title = board.title,
                    description = boardRepository.statusSummary(board),
                    uri = board.uri,
                    viewModel = vmByConvId[board.id]
                )
                if (capability == null || boardTarget.hasCapability(capability)) {
                    targets.add(boardTarget)
                }
            }

            targets
        }
    }

    /**
     * Gets a single snapshot of available targets.
     */
    fun getTargets(capability: TargetCapability? = null): Single<List<Target>> {
        return observeTargets(capability).first(emptyList())
    }
}
