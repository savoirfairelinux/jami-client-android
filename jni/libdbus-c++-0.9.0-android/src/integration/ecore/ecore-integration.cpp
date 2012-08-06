/*
 *
 *  D-Bus++ - C++ bindings for D-Bus
 *
 *  Copyright (C) 2005-2007  Paolo Durante <shackan@gmail.com>
 *
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <dbus-c++/ecore-integration.h>

#include <dbus/dbus.h> // for DBUS_WATCH_*

using namespace DBus;

Dispatcher *gdispatcher = NULL;

Ecore::BusTimeout::BusTimeout(Timeout::Internal *ti)
  : Timeout(ti)
{
  if (Timeout::enabled())
  {
    _enable();
  }
}

Ecore::BusTimeout::~BusTimeout()
{
  _disable();
}

void Ecore::BusTimeout::toggle()
{
  debug_log("ecore: timeout %p toggled (%s)", this, Timeout::enabled() ? "on" : "off");

  if (Timeout::enabled())
  {
    _enable();
  }
  else
  {
    _disable();
  }
}

Eina_Bool Ecore::BusTimeout::timeout_handler(void *data)
{
  Ecore::BusTimeout *t = reinterpret_cast<Ecore::BusTimeout *>(data);

  debug_log("Ecore::BusTimeout::timeout_handler( void *data )");

  t->handle();

  return ECORE_CALLBACK_RENEW;
}

void Ecore::BusTimeout::_enable()
{
  debug_log("Ecore::BusTimeout::_enable()");

  _etimer = ecore_timer_add(((double)Timeout::interval()) / 1000, timeout_handler, this);
}

void Ecore::BusTimeout::_disable()
{
  debug_log("Ecore::BusTimeout::_disable()");

  ecore_timer_del(_etimer);
}

Ecore::BusWatch::BusWatch(Watch::Internal *wi)
  : Watch(wi), fd_handler(NULL), _bd(NULL)
{
  if (Watch::enabled())
  {
    _enable();
  }
}

Ecore::BusWatch::~BusWatch()
{
  _disable();
}

void Ecore::BusWatch::toggle()
{
  debug_log("ecore: watch %p toggled (%s)", this, Watch::enabled() ? "on" : "off");

  if (Watch::enabled())	_enable();
  else			_disable();
}

Eina_Bool Ecore::BusWatch::watch_dispatch(void *data, Ecore_Fd_Handler *fdh)
{
  Ecore::BusWatch *w = reinterpret_cast<Ecore::BusWatch *>(data);

  debug_log("Ecore::BusWatch watch_handler");

  int flags = w->flags();

  if (w->flags() & DBUS_WATCH_READABLE)
    ecore_main_fd_handler_active_set(w->fd_handler, ECORE_FD_READ);
  if (w->flags() & DBUS_WATCH_WRITABLE)
    ecore_main_fd_handler_active_set(w->fd_handler, ECORE_FD_WRITE);

  w->handle(flags);
  w->_bd->dispatch_pending();

  return 1;
}

void Ecore::BusWatch::_enable()
{
  debug_log("Ecore::BusWatch::_enable()");

  fd_handler = ecore_main_fd_handler_add(descriptor(),
                                         (Ecore_Fd_Handler_Flags)(ECORE_FD_READ | ECORE_FD_WRITE),
                                         watch_dispatch, this,
                                         NULL, NULL);
}

void Ecore::BusWatch::_disable()
{
  if (fd_handler)
  {
    ecore_main_fd_handler_del(fd_handler);
    fd_handler = NULL;
  }
}

void Ecore::BusWatch::data(Ecore::BusDispatcher *bd)
{
  _bd = bd;
}

Ecore::BusDispatcher::BusDispatcher()
{
}

Eina_Bool Ecore::BusDispatcher::check(void *data, Ecore_Fd_Handler *fdh)
{
  return 0;
}

Timeout *Ecore::BusDispatcher::add_timeout(Timeout::Internal *wi)
{
  Timeout *t = new Ecore::BusTimeout(wi);

  debug_log("ecore: added timeout %p (%s)", t, t->enabled() ? "on" : "off");

  return t;
}

void Ecore::BusDispatcher::rem_timeout(Timeout *t)
{
  debug_log("ecore: removed timeout %p", t);

  delete t;
}

Watch *Ecore::BusDispatcher::add_watch(Watch::Internal *wi)
{
  Ecore::BusWatch *w = new Ecore::BusWatch(wi);
  w->data(this);

  debug_log("ecore: added watch %p (%s) fd=%d flags=%d",
            w, w->enabled() ? "on" : "off", w->descriptor(), w->flags()
           );
  return w;
}

void Ecore::BusDispatcher::rem_watch(Watch *w)
{
  debug_log("ecore: removed watch %p", w);

  delete w;
}
