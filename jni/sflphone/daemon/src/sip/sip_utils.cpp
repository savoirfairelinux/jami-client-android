/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Tristan Matthews <tristan.matthews@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

#include <pjsip.h>
#include <pjlib.h>
#include <pjsip_ua.h>
#include <pjlib-util.h>
#include <pjnath.h>
#include <pjnath/stun_config.h>
#include <pj/string.h>
#include <pjsip/sip_msg.h>
#include <pjsip/sip_types.h>
#include <pjsip/sip_uri.h>
#include <pj/list.h>
#include "sip_utils.h"

std::string
sip_utils::fetchHeaderValue(pjsip_msg *msg, const std::string &field)
{
    pj_str_t name = pj_str((char*) field.c_str());
    pjsip_generic_string_hdr *hdr = static_cast<pjsip_generic_string_hdr*>(pjsip_msg_find_hdr_by_name(msg, &name, NULL));

    if (!hdr)
        return "";

    std::string value(hdr->hvalue.ptr, hdr->hvalue.slen);

    size_t pos = value.find("\n");

    if (pos != std::string::npos)
        return value.substr(0, pos);
    else
        return "";
}

pjsip_route_hdr *
sip_utils::createRouteSet(const std::string &route, pj_pool_t *hdr_pool)
{
    int port = 0;
    std::string host;

    size_t found = route.find(":");

    if (found != std::string::npos) {
        host = route.substr(0, found);
        port = atoi(route.substr(found + 1, route.length()).c_str());
    } else
        host = route;

    pjsip_route_hdr *route_set = pjsip_route_hdr_create(hdr_pool);
    pjsip_route_hdr *routing = pjsip_route_hdr_create(hdr_pool);
    pjsip_sip_uri *url = pjsip_sip_uri_create(hdr_pool, 0);
    url->lr_param = 1;
    routing->name_addr.uri = (pjsip_uri*) url;
    pj_strdup2(hdr_pool, &url->host, host.c_str());
    url->port = port;

    pj_list_push_back(route_set, pjsip_hdr_clone(hdr_pool, routing));

    return route_set;
}


std::string
sip_utils::parseDisplayName(const char * buffer)
{
    const char* from_header = strstr(buffer, "From: ");

    if (!from_header)
        return "";

    std::string temp(from_header);
    size_t begin_displayName = temp.find("\"") + 1;
    size_t end_displayName = temp.rfind("\"");
    std::string displayName(temp.substr(begin_displayName, end_displayName - begin_displayName));

    static const size_t MAX_DISPLAY_NAME_SIZE = 25;
    if (displayName.size() > MAX_DISPLAY_NAME_SIZE)
        return "";

    return displayName;
}

void
sip_utils::stripSipUriPrefix(std::string& sipUri)
{
    // Remove sip: prefix
    static const char SIP_PREFIX[] = "sip:";
    size_t found = sipUri.find(SIP_PREFIX);

    if (found != std::string::npos)
        sipUri.erase(found, found + (sizeof SIP_PREFIX) - 1);

    found = sipUri.find("@");

    if (found != std::string::npos)
        sipUri.erase(found);
}
