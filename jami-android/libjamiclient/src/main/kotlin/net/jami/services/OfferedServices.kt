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
package net.jami.services

/** A shared service, with the contact that offers it. */
data class OfferedService(
    val peerUri: String,
    val service: PeerServiceInfo,
)

/**
 * The shared services the contacts of an account offer, put together from the
 * answer each contact gives to its own query.
 *
 * Contacts keep the order they were asked in and each one its services in the
 * order it gave them, whichever contact answers first. A contact that could not
 * answer (offline, unreachable...) offers nothing, but is no longer waited for.
 */
class OfferedServices private constructor(
    private val contacts: List<String>,
    private val answers: Map<String, List<PeerServiceInfo>>,
) {
    val services: List<OfferedService> = contacts.flatMap { peer ->
        answers[peer].orEmpty().map { OfferedService(peer, it) }
    }

    /** How many contacts have not answered yet. */
    val pending: Int get() = contacts.size - answers.size

    val isLoading: Boolean get() = pending > 0

    /** With the answer of one of the contacts asked; its latest answer counts. */
    fun with(answer: PeerServicesResult): OfferedServices {
        val peer = answer.peerId
        if (peer !in contacts) return this
        val offered = if (answer.status == PeerServicesStatus.OK) answer.services.distinctBy { it.id } else emptyList()
        return OfferedServices(contacts, answers + (peer to offered))
    }

    companion object {
        /** Before any of [contacts] has answered. */
        fun asking(contacts: Collection<String>) = OfferedServices(contacts.distinct(), emptyMap())
    }
}
