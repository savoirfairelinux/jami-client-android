/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-server-win.c Server implementation for WIN network protocols.
 *
 * Copyright (C) 2002, 2003, 2004  Red Hat Inc.
 * Copyright (C) 2007 Ralf Habacker <ralf.habacker@freenet.de>
 *
 * Licensed under the Academic Free License version 2.1
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

#include <config.h>
#include "dbus-internals.h"
#include "dbus-server-win.h"
#include "dbus-server-socket.h"

/**
 * @defgroup DBusServerWin DBusServer implementations for Windows
 * @ingroup  DBusInternals
 * @brief Implementation details of DBusServer on Windows 
 *
 * @{
 */

/**
 * Tries to interpret the address entry in a platform-specific
 * way, creating a platform-specific server type if appropriate.
 * Sets error if the result is not OK.
 * 
 * @param entry an address entry
 * @param server_p location to store a new DBusServer, or #NULL on failure.
 * @param error location to store rationale for failure on bad address
 * @returns the outcome
 * 
 */
DBusServerListenResult
_dbus_server_listen_platform_specific (DBusAddressEntry *entry,
                                       DBusServer      **server_p,
                                       DBusError        *error)
{
  const char *method;

  *server_p  = NULL;

  method = dbus_address_entry_get_method (entry);

  if (strcmp (method, "nonce-tcp") == 0)
    {
      const char *host;
      const char *port;
      const char *bind;
      const char *family;

      host = dbus_address_entry_get_value (entry, "host");
      bind = dbus_address_entry_get_value (entry, "bind");
      port = dbus_address_entry_get_value (entry, "port");
      family = dbus_address_entry_get_value (entry, "family");

      *server_p = _dbus_server_new_for_tcp_socket (host, bind, port,
                                                   family, error, TRUE);

      if (*server_p)
        {
          _DBUS_ASSERT_ERROR_IS_CLEAR(error);
          return DBUS_SERVER_LISTEN_OK;
        }
      else
        {
          _DBUS_ASSERT_ERROR_IS_SET(error);
          return DBUS_SERVER_LISTEN_DID_NOT_CONNECT;
        }
    }
  else
    {
  _DBUS_ASSERT_ERROR_IS_CLEAR(error);
  return DBUS_SERVER_LISTEN_NOT_HANDLED;
}
}

/** @} */

