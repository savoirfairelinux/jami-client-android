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

#include "networkmanager.h"
#include "../manager.h"
#include "array_size.h"
#include "logger.h"

namespace {
    const char *stateAsString(uint32_t state)
    {
        static const char * STATES[] = {"unknown", "asleep", "connecting",
            "connected", "disconnected"};

        const size_t idx = state < ARRAYSIZE(STATES) ? state : 0;
        return STATES[idx];
    }
}

void NetworkManager::StateChanged(const uint32_t &state)
{
    WARN("Network state changed: %s", stateAsString(state));
}

void NetworkManager::PropertiesChanged(const std::map<std::string, ::DBus::Variant> &argin0)
{
    WARN("Properties changed: ");
    for (std::map<std::string, ::DBus::Variant>::const_iterator iter = argin0.begin();
            iter != argin0.end(); ++iter)
        WARN("%s", iter->first.c_str());
    Manager::instance().registerAccounts();
}

NetworkManager::NetworkManager(DBus::Connection &connection,
                               const DBus::Path &dbus_path,
                               const char *destination) :
    DBus::ObjectProxy(connection, dbus_path, destination)
{}
