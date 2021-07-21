/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.model;

import net.jami.utils.StringUtils;
import net.jami.utils.Tuple;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Uri implements Serializable {

    private final String mScheme;
    private final String mUsername;
    private final String mHost;
    private final String mPort;

    private static final Pattern ANGLE_BRACKETS_PATTERN = Pattern.compile("^\\s*([^<>]+)?\\s*<([^<>]+)>\\s*$");
    private static final Pattern HEX_ID_PATTERN = Pattern.compile("^\\p{XDigit}{40}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RING_URI_PATTERN = Pattern.compile("^\\s*(?:ring(?:[\\s\\:]+))?(\\p{XDigit}{40})(?:@ring\\.dht)?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern URI_PATTERN = Pattern.compile("^\\s*(\\w+:)?(?:([\\w.]+)@)?(?:([\\d\\w\\.\\-]+)(?::(\\d+))?)\\s*$", Pattern.CASE_INSENSITIVE);
    public static final String RING_URI_SCHEME = "ring:";
    public static final String JAMI_URI_SCHEME = "jami:";
    public static final String SWARM_SCHEME = "swarm:";

    private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";
    private static final Pattern VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);

    public Uri(String scheme, String user, String host, String port) {
        mScheme = scheme;
        mUsername = user;
        mHost = host;
        mPort = port;
    }

    public Uri(String scheme, String host) {
        mScheme = scheme;
        mUsername = null;
        mHost = host;
        mPort = null;
    }

    static public Uri fromString(String uri) {
        Matcher m = URI_PATTERN.matcher(uri);
        if (m.find()) {
            return new Uri(m.group(1), m.group(2), m.group(3), m.group(4));
        } else {
            return new Uri(null, null, uri, null);
        }
    }

    static public Tuple<Uri, String> fromStringWithName(String uriString) {
        Matcher m = ANGLE_BRACKETS_PATTERN.matcher(uriString);
        if (m.find()) {
            return new Tuple<>(fromString(m.group(2)), m.group(1));
        } else {
            return new Tuple<>(fromString(uriString), null);
        }
    }

    public static Uri fromId(String conversationId) {
        return new Uri(null, null, conversationId, null);
    }

    public String getRawRingId() {
        if (getUsername() != null) {
            return getUsername();
        } else {
            return getHost();
        }
    }

    public String getUri() {
        if (isSwarm())
            return getScheme() + getRawRingId();
        if (isHexId())
            return getRawRingId();
        return toString();
    }

    public String getRawUriString() {
        if (isSwarm())
            return getScheme() + getRawRingId();
        if (isHexId()) {
            return RING_URI_SCHEME + getRawRingId();
        }
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        if (!net.jami.utils.StringUtils.isEmpty(mScheme)) {
            builder.append(mScheme);
        }
        if (!net.jami.utils.StringUtils.isEmpty(mUsername)) {
            builder.append(mUsername).append('@');
        }
        if (!net.jami.utils.StringUtils.isEmpty(mHost)) {
            builder.append(mHost);
        }
        if (!net.jami.utils.StringUtils.isEmpty(mPort)) {
            builder.append(':').append(mPort);
        }
        return builder.toString();
    }

    public boolean isSingleIp() {
        return (getUsername() == null || getUsername().isEmpty()) && isIpAddress(getHost());
    }

    public boolean isHexId() {
        return (getHost() != null && HEX_ID_PATTERN.matcher(getHost()).find())
                || (getUsername() != null && HEX_ID_PATTERN.matcher(getUsername()).find());
    }
    public boolean isSwarm() {
        return SWARM_SCHEME.equals(getScheme());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Uri)) {
            return false;
        }
        Uri uo = (Uri) o;
        return Objects.equals(getUsername(), uo.getUsername())
                && Objects.equals(getHost(), uo.getHost());
    }

    public boolean isEmpty() {
        return net.jami.utils.StringUtils.isEmpty(getUsername()) && StringUtils.isEmpty(getHost());
    }

    /**
     * Determine if the given string is a valid IPv4 or IPv6 address.  This method
     * uses pattern matching to see if the given string could be a valid IP address.
     *
     * @param ipAddress A string that is to be examined to verify whether or not
     *                  it could be a valid IP address.
     * @return <code>true</code> if the string is a value that is a valid IP address,
     * <code>false</code> otherwise.
     */
    public static boolean isIpAddress(String ipAddress) {
        Matcher m1 = VALID_IPV4_PATTERN.matcher(ipAddress);
        if (m1.matches()) {
            return true;
        }
        Matcher m2 = VALID_IPV6_PATTERN.matcher(ipAddress);
        return m2.matches();
    }

    public String getScheme() {
        return mScheme;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getHost() {
        return mHost;
    }

    public String getPort() {
        return mPort;
    }

}
