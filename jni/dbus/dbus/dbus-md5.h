/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-md5.h md5 implementation (based on L Peter Deutsch implementation)
 *
 * Copyright (C) 2003 Red Hat Inc.
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
#ifndef DBUS_MD5_H
#define DBUS_MD5_H

#include <dbus/dbus-macros.h>
#include <dbus/dbus-errors.h>
#include <dbus/dbus-string.h>

DBUS_BEGIN_DECLS

typedef struct DBusMD5Context DBusMD5Context;

/**
 * A context used to store the state of the MD5 algorithm
 */
struct DBusMD5Context
{
  dbus_uint32_t count[2];       /**< message length in bits, lsw first */
  dbus_uint32_t abcd[4];        /**< digest buffer */
  unsigned char buf[64];        /**< accumulate block */
};

void        _dbus_md5_init    (DBusMD5Context   *context);
void        _dbus_md5_update  (DBusMD5Context   *context,
                               const DBusString *data);
dbus_bool_t _dbus_md5_final   (DBusMD5Context   *context,
                               DBusString       *results);
dbus_bool_t _dbus_md5_compute (const DBusString *data,
                               DBusString       *ascii_output);

DBUS_END_DECLS

#endif /* DBUS_MD5_H */
