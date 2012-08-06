/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-transport-win.c Windows socket subclasses of DBusTransport
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
#include "dbus-connection-internal.h"
#include "dbus-transport-socket.h"
#include "dbus-transport-protected.h"
#include "dbus-watch.h"
#include "dbus-sysdeps-win.h"

/**
 * @defgroup DBusTransportUnix DBusTransport implementations for UNIX
 * @ingroup  DBusInternals
 * @brief Implementation details of DBusTransport on UNIX
 *
 * @{
 */

/**
 * Opens platform specific transport types.
 * 
 * @param entry the address entry to try opening
 * @param transport_p return location for the opened transport
 * @param error error to be set
 * @returns result of the attempt
 */
DBusTransportOpenResult
_dbus_transport_open_platform_specific (DBusAddressEntry  *entry,
                                        DBusTransport    **transport_p,
                                        DBusError         *error)
{
  const char *method;

  const char *host = dbus_address_entry_get_value (entry, "host");
  const char *port = dbus_address_entry_get_value (entry, "port");
  const char *family = dbus_address_entry_get_value (entry, "family");
  const char *noncefile = dbus_address_entry_get_value (entry, "noncefile");

  method = dbus_address_entry_get_method (entry);
  _dbus_assert (method != NULL);

  if (strcmp (method, "nonce-tcp") != 0)
    {
      _DBUS_ASSERT_ERROR_IS_CLEAR (error);
      return DBUS_TRANSPORT_OPEN_NOT_HANDLED;
    }

  if (port == NULL)
    {
      _dbus_set_bad_address (error, "nonce-tcp", "port", NULL);
      return DBUS_TRANSPORT_OPEN_BAD_ADDRESS;
    }

  *transport_p = _dbus_transport_new_for_tcp_socket (host, port, family, noncefile, error);
  if (*transport_p == NULL)
    {
      _DBUS_ASSERT_ERROR_IS_SET (error);
      return DBUS_TRANSPORT_OPEN_DID_NOT_CONNECT;
    }
  else
    {
      _DBUS_ASSERT_ERROR_IS_CLEAR (error);
      return DBUS_TRANSPORT_OPEN_OK;
    }
}

/** @} */
