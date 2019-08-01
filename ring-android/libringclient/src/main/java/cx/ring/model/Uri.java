/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

public class Uri implements Serializable {

    private String mDisplayName = null;
    private String mScheme = null;
    private String mUsername = null;
    private String mHost = null;
    private String mPort = null;

    private static final Pattern ANGLE_BRACKETS_PATTERN = Pattern.compile("^\\s*([^<>]+)?\\s*<([^<>]+)>\\s*$");
    private static final Pattern RING_ID_PATTERN = Pattern.compile("^\\p{XDigit}{40}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RING_URI_PATTERN = Pattern.compile("^\\s*(?:ring(?:[\\s\\:]+))?(\\p{XDigit}{40})(?:@ring\\.dht)?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern URI_PATTERN = Pattern.compile("^\\s*(\\w+:)?(?:([\\w.]+)@)?(?:([\\d\\w\\.\\-]+)(?::(\\d+))?)\\s*$", Pattern.CASE_INSENSITIVE);
    public static final String RING_URI_SCHEME = "ring:";

    private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";
    private static final Pattern VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);


    public Uri(String uri) {
        if (uri != null) {
            parseUri(uri);
        }
    }

    public String getRawUriString() {
        if (isRingId()) {
            return RING_URI_SCHEME + getRawRingId();
        }

        StringBuilder builder = new StringBuilder(64);
        if (getUsername() != null && !getUsername().isEmpty()) {
            builder.append(getUsername()).append("@");
        }
        if (getHost() != null) {
            builder.append(getHost());
        }
        if (getPort() != null && !getPort().isEmpty()) {
            builder.append(":").append(getPort());
        }
        return builder.toString();
    }

    public String getUriString() {
        if (getDisplayName() == null || getDisplayName().isEmpty()) {
            return getRawUriString();
        }
        return getDisplayName() + " <" + getRawUriString() + ">";
    }

    @Override
    public String toString() {
        return getUriString();
    }

    public boolean isSingleIp() {
        return (getUsername() == null || getUsername().isEmpty()) && isIpAddress(getHost());
    }

    public boolean isRingId() {
        return (getHost() != null && RING_ID_PATTERN.matcher(getHost()).find())
                || (getUsername() != null && RING_ID_PATTERN.matcher(getUsername()).find());
    }

    public String getRawRingId() {
        if (getUsername() != null) {
            return getUsername();
        } else {
            return getHost();
        }
    }

    private void parseUri(String uri) {
        Matcher m = ANGLE_BRACKETS_PATTERN.matcher(uri);
        if (m.find()) {
            setDisplayName(m.group(1));
            parseUriRaw(m.group(2));
        } else {
            parseUriRaw(uri);
        }
    }

    private void parseUriRaw(String uri) {
        Matcher m = URI_PATTERN.matcher(uri);
        if (m.find()) {
            setScheme(m.group(1));
            setUsername(m.group(2));
            setHost(m.group(3));
            setPort(m.group(4));
        } else {
            setHost(uri);
        }
    }

    public String getUri() {
        if (isRingId())
            return getRawRingId();
        else {
            StringBuilder builder = new StringBuilder(64);
            if (getUsername() != null && !getUsername().isEmpty()) {
                builder.append(getUsername()).append("@");
            }
            if (getHost() != null) {
                builder.append(getHost());
            }
            return builder.toString();
        }
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
        return (getUsername() == null ? uo.getUsername() == null : getUsername().equals(uo.getUsername()))
                && (getHost() == null ? uo.getHost() == null : getHost().equals(uo.getHost()));
    }

    public boolean isEmpty() {
        return (getUsername() == null || getUsername().isEmpty()) && (getHost() == null || getHost().isEmpty());
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

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public String getScheme() {
        return mScheme;
    }

    public void setScheme(String scheme) {
        this.mScheme = scheme;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getHost() {
        return mHost;
    }

    public void setHost(String host) {
        this.mHost = host;
    }

    public String getPort() {
        return mPort;
    }

    public void setPort(String port) {
        this.mPort = port;
    }
}
