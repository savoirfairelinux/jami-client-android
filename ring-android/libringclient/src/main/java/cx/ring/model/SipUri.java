/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
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
package cx.ring.model;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SipUri implements Serializable {

    public String displayName = null;
    public String scheme = null;
    public String username = null;
    public String host = null;
    public String port = null;

    public static final Pattern ANGLE_BRACKETS_PATTERN = Pattern.compile("^\\s*([^<>]+)?\\s*<([^<>]+)>\\s*$");
    public static final Pattern RING_ID_PATTERN = Pattern.compile("^\\p{XDigit}{40}$", Pattern.CASE_INSENSITIVE);
    public static final Pattern RING_URI_PATTERN = Pattern.compile("^\\s*(?:ring(?:[\\s\\:]+))?(\\p{XDigit}{40})(?:@ring\\.dht)?\\s*$", Pattern.CASE_INSENSITIVE);
    public static final Pattern URI_PATTERN = Pattern.compile("^\\s*(\\w+:)?(?:([\\w.]+)@)?(?:([\\d\\w\\.\\-]+)(?::(\\d+))?)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";
    private static final Pattern VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);


    public SipUri(String uri) {
        if (uri != null)
            parseUri(uri);
    }

    public String getRawUriString() {
        if (host != null && RING_ID_PATTERN.matcher(host).find()) {
            return "ring:" + host;
        } else if (username != null && RING_ID_PATTERN.matcher(username).find()) {
            return "ring:" + username;
        }

        StringBuilder builder = new StringBuilder(64);
        if (username != null && !username.isEmpty()) {
            builder.append(username).append("@");
        }
        if (host != null) {
            builder.append(host);
        }
        if (port != null && !port.isEmpty()) {
            builder.append(":").append(port);
        }
        return builder.toString();
    }

    public String getUriString() {
        if (displayName == null || displayName.isEmpty())
            return getRawUriString();
        return displayName + " <" + getRawUriString() + ">";
    }

    @Override
    public String toString() {
        return getUriString();
    }

    public boolean isSingleIp() {
        return (username == null || username.isEmpty()) && isIpAddress(host);
    }

    public boolean isRingId() {
        return host != null && RING_ID_PATTERN.matcher(host).find()
                || username != null && RING_ID_PATTERN.matcher(username).find();
    }

    private void parseUri(String uri) {
        Matcher m = ANGLE_BRACKETS_PATTERN.matcher(uri);
        if (m.find()) {
            displayName = m.group(1);
            parseUriRaw(m.group(2));
        } else {
            parseUriRaw(uri);
        }
    }

    private void parseUriRaw(String uri) {
        Matcher m = URI_PATTERN.matcher(uri);
        if (m.find()) {
            scheme = m.group(1);
            username = m.group(2);
            host = m.group(3);
            port = m.group(4);
        } else {
            host = uri;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof SipUri)) {
            return false;
        }
        SipUri uo = (SipUri) o;
        return (username == null ? uo.username == null : username.equals(uo.username))
                && (host == null ? uo.host == null : host.equals(uo.host));
    }

    public boolean isEmpty() {
        return (username == null || username.isEmpty()) && (host == null || host.isEmpty());
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
}
