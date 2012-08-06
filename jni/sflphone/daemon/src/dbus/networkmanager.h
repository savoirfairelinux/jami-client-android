/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Julien Bonjean <julien.bonjean@savoirfairelinux.com>
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

#ifndef NETWORKMANAGER_H_
#define NETWORKMANAGER_H_

#pragma GCC diagnostic ignored "-Wignored-qualifiers"
#pragma GCC diagnostic ignored "-Wunused-parameter"
#include "networkmanager_proxy.h"
#pragma GCC diagnostic warning "-Wignored-qualifiers"
#pragma GCC diagnostic warning "-Wunused-parameter"

class NetworkManager : public org::freedesktop::NetworkManager_proxy,
                       public DBus::IntrospectableProxy,
                       // cppcheck-suppress unusedFunction
                       public DBus::ObjectProxy {
    public:
        NetworkManager(DBus::Connection &, const DBus::Path &, const char*);
        void StateChanged(const uint32_t &state);
        void PropertiesChanged(const std::map<std::string, ::DBus::Variant> &argin0);

    private:
        enum NMState {
            NM_STATE_UNKNOWN = 0,
            NM_STATE_ASLEEP,
            NM_STATE_CONNECTING,
            NM_STATE_CONNECTED,
            NM_STATE_DISCONNECTED
        };
};
#endif

