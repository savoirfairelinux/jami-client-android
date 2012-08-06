/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Pierre-Luc Beaudoin <pierre-luc.beaudoin@savoirfairelinux.com>
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <cstdlib>
#include "dbusmanager.h"
#include "global.h"
#include "manager.h"
#include "logger.h"
#include "instance.h"

#include "callmanager.h"
#include "configurationmanager.h"
#include "networkmanager.h"

#ifdef SFL_VIDEO
#include "dbus/video_controls.h"
#endif

DBusManager::DBusManager() : callManager_(0)
    , configurationManager_(0)
    , instanceManager_(0)
    , dispatcher_()
#ifdef SFL_VIDEO
    , videoControls_(0)
#endif
#ifdef USE_NETWORKMANAGER
    , networkManager_(0)
#endif
{
    try {
        DEBUG("DBUS init threading");
        DBus::_init_threading();
        DEBUG("DBUS instantiate default dispatcher");
        DBus::default_dispatcher = &dispatcher_;

        DEBUG("DBUS session connection to session bus");
        DBus::Connection sessionConnection(DBus::Connection::SessionBus());
        DEBUG("DBUS request org.sflphone.SFLphone from session connection");
        sessionConnection.request_name("org.sflphone.SFLphone");

        DEBUG("DBUS create call manager from session connection");
        callManager_ = new CallManager(sessionConnection);
        DEBUG("DBUS create configuration manager from session connection");
        configurationManager_ = new ConfigurationManager(sessionConnection);
        DEBUG("DBUS create instance manager from session connection");
        instanceManager_ = new Instance(sessionConnection);

#ifdef SFL_VIDEO
        videoControls_ = new VideoControls(sessionConnection);
#endif

#ifdef USE_NETWORKMANAGER
        DEBUG("DBUS system connection to system bus");
        DBus::Connection systemConnection(DBus::Connection::SystemBus());
        DEBUG("DBUS create the network manager from the system bus");
        networkManager_ = new NetworkManager(systemConnection, "/org/freedesktop/NetworkManager", "");
#endif

    } catch (const DBus::Error &err) {
        ERROR("%s: %s, exiting\n", err.name(), err.what());
        ::exit(EXIT_FAILURE);
    }

    DEBUG("DBUS registration done");
}

DBusManager::~DBusManager()
{
#ifdef USE_NETWORKMANAGER
    delete networkManager_;
#endif
#ifdef SFL_VIDEO
    delete videoControls_;
#endif
    delete instanceManager_;
    delete configurationManager_;
    delete callManager_;
}

void DBusManager::exec()
{
    try {
        dispatcher_.enter();
    } catch (const DBus::Error &err) {
        ERROR("%s: %s, exiting\n", err.name(), err.what());
        ::exit(EXIT_FAILURE);
    } catch (const std::exception &err) {
        ERROR("%s: exiting\n", err.what());
        ::exit(EXIT_FAILURE);
    }
}

void DBusManager::exit()
{
    try {
        dispatcher_.leave();
    } catch (const DBus::Error &err) {
        ERROR("%s: %s, exiting\n", err.name(), err.what());
        ::exit(EXIT_FAILURE);
    } catch (const std::exception &err) {
        ERROR("%s: exiting\n", err.what());
        ::exit(EXIT_FAILURE);
    }
}
