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
package net.jami.model

import java.io.Serializable
import java.lang.StringBuilder
import java.util.*
import java.util.regex.Pattern

class Uri : Serializable {
    val scheme: String?
    val username: String?
    val host: String
    val port: String?

    constructor(scheme: String?, user: String?, host: String, port: String?) {
        this.scheme = scheme
        username = user
        this.host = host
        this.port = port
    }

    constructor(scheme: String?, host: String) {
        this.scheme = scheme
        username = null
        this.host = host
        port = null
    }

    val rawRingId: String
        get() = username ?: host

    val uri: String
        get() =
            if (isSwarm) scheme + rawRingId
            else if (isHexId) rawRingId
            else toString()

    val rawUriString: String
        get() =
            if (isSwarm) scheme + rawRingId
            else if (isHexId) DEFAULT_CONTACT_SCHEME + rawRingId
            else toString()

    override fun toString(): String {
        val builder = StringBuilder(64)
        if (!scheme.isNullOrEmpty()) {
            builder.append(scheme)
        }
        if (!username.isNullOrEmpty()) {
            builder.append(username).append('@')
        }
        if (host.isNotEmpty()) {
            builder.append(host)
        }
        if (!port.isNullOrEmpty()) {
            builder.append(':').append(port)
        }
        return builder.toString()
    }

    val isSingleIp: Boolean
        get() = username.isNullOrEmpty() && isIpAddress(host)
    val isHexId: Boolean
        get() = HEX_ID_PATTERN.matcher(host).find() || username != null && HEX_ID_PATTERN.matcher(username).find()
    val isSwarm: Boolean
        get() = SWARM_SCHEME == scheme
    val isJami: Boolean
        get() = JAMI_URI_SCHEME == scheme || RING_URI_SCHEME == scheme || (scheme.isNullOrEmpty() && isHexId)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Uri) {
            return false
        }
        return (username == other.username
                && host == other.host)
    }

    override fun hashCode(): Int = Objects.hash(scheme, username, host, port)

    val isEmpty: Boolean
        get() = username.isNullOrEmpty() && host.isEmpty()

    companion object {
        private val ANGLE_BRACKETS_PATTERN = Pattern.compile("^\\s*([^<>]+)?\\s*<([^<>]+)>\\s*$")
        private val HEX_ID_PATTERN = Pattern.compile("^\\p{XDigit}{40}$", Pattern.CASE_INSENSITIVE)
        private val RING_URI_PATTERN = Pattern.compile("^\\s*(?:ring(?:[\\s:]+))?(\\p{XDigit}{40})(?:@ring\\.dht)?\\s*$", Pattern.CASE_INSENSITIVE)
        private val URI_PATTERN = Pattern.compile("^\\s*(\\w+:)?(?:([\\w.]+)@)?(?:([\\d\\w.\\-]+)(?::(\\d+))?)\\s*$", Pattern.CASE_INSENSITIVE)
        const val RING_URI_SCHEME = "ring:"
        const val JAMI_URI_SCHEME = "jami:"
        const val SIP_URI_SCHEME = "sip:"
        const val DEFAULT_CONTACT_SCHEME = JAMI_URI_SCHEME
        const val SWARM_SCHEME = "swarm:"
        const val RDV_SCHEME = "rdv:"
        private const val ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
        private const val ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}"
        private val VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE)
        private val VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE)

        fun fromString(uri: String): Uri {
            val m = URI_PATTERN.matcher(uri)
            return if (m.find()) {
                Uri(m.group(1), m.group(2), m.group(3), m.group(4))
            } else {
                Uri(null, null, uri, null)
            }
        }

        fun fromStringWithName(uriString: String): Pair<Uri, String?> {
            val m = ANGLE_BRACKETS_PATTERN.matcher(uriString)
            return if (m.find()) {
                Pair(fromString(m.group(2)), m.group(1))
            } else {
                Pair(fromString(uriString), null)
            }
        }

        fun fromId(conversationId: String): Uri = Uri(null, conversationId)

        /**
         * Determine if the given string is a valid IPv4 or IPv6 address.  This method
         * uses pattern matching to see if the given string could be a valid IP address.
         *
         * @param ipAddress A string that is to be examined to verify whether or not
         * it could be a valid IP address.
         * @return `true` if the string is a value that is a valid IP address,
         * `false` otherwise.
         */
        fun isIpAddress(ipAddress: String): Boolean {
            val m1 = VALID_IPV4_PATTERN.matcher(ipAddress)
            if (m1.matches()) {
                return true
            }
            val m2 = VALID_IPV6_PATTERN.matcher(ipAddress)
            return m2.matches()
        }
    }
}