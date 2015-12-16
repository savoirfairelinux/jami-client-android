/*
 *  Copyright (C) 2015 Savoir-faire Linux Inc.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cx.ring.utils.Utilities;

public class SipUri
{
    public String display_name = null;
    public String scheme = null;
    public String username = null;
    public String host = null;
    public String port = null;

    public static final Pattern ANGLE_BRACKETS_PATTERN = Pattern.compile("^\\s*([^<>]+)?\\s*<([^<>]+)>\\s*$");
    public static final Pattern RING_ID_PATTERN = Pattern.compile("^\\p{XDigit}{40}$", Pattern.CASE_INSENSITIVE);
    public static final Pattern RING_URI_PATTERN = Pattern.compile("^\\s*(?:ring(?:[\\s\\:]+))?(\\p{XDigit}{40})(?:@ring\\.dht)?\\s*$", Pattern.CASE_INSENSITIVE);
    public static final Pattern URI_PATTERN = Pattern.compile("^\\s*(\\w+:)?(?:([\\w.]+)@)?(?:([\\d\\w\\.\\-]+)(?::(\\d+))?)\\s*$", Pattern.CASE_INSENSITIVE);

    public SipUri(String uri) {
        parseUri(uri);
    }

    public String getRawUriString() {
        if (host != null && RING_ID_PATTERN.matcher(host).find())
            return "ring:" + host;
        else if (username != null &&  RING_ID_PATTERN.matcher(username).find())
            return "ring:" + username;

        StringBuilder builder = new StringBuilder(64);
        if (username != null && !username.isEmpty())
            builder.append(username).append("@");
        if (host != null)
            builder.append(host);
        if (port != null && !port.isEmpty())
            builder.append(":").append(port);
        return builder.toString();
    }

    public String getUriString() {
        if (display_name == null || display_name.isEmpty())
            return getRawUriString();
        return display_name+" <"+getRawUriString()+">";
    }

    public String toString() {
        return getUriString();
    }

    public boolean isSingleIp() {
        return (username == null || username.isEmpty()) && Utilities.isIpAddress(host);
    }

    public boolean isRingId() {
        if (host != null && RING_ID_PATTERN.matcher(host).find())
            return true;
        else if (username != null && RING_ID_PATTERN.matcher(username).find())
            return true;
        return false;
    }

    private void parseUri(String uri) {
        Matcher m = ANGLE_BRACKETS_PATTERN.matcher(uri);
        if (m.find()) {
            display_name = m.group(1);
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
}
