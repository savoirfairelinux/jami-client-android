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
#include <dbus-c++/glib-integration.h>

#include <dbus/dbus.h> // for DBUS_WATCH_*

using namespace DBus;

Glib::BusTimeout::BusTimeout(Timeout::Internal *ti, GMainContext *ctx, int priority)
  : Timeout(ti), _ctx(ctx), _priority(priority), _source(NULL)
{
  if (Timeout::enabled())
    _enable();
}

Glib::BusTimeout::~BusTimeout()
{
  _disable();
}

void Glib::BusTimeout::toggle()
{
  debug_log("glib: timeout %p toggled (%s)", this, Timeout::enabled() ? "on" : "off");

  if (Timeout::enabled())	_enable();
  else			_disable();
}

gboolean Glib::BusTimeout::timeout_handler(gpointer data)
{
  Glib::BusTimeout *t = reinterpret_cast<Glib::BusTimeout *>(data);

  t->handle();

  return TRUE;
}

void Glib::BusTimeout::_enable()
{
  if (_source)
    _disable(); // be sane

  _source = g_timeout_source_new(Timeout::interval());
  g_source_set_priority(_source, _priority);
  g_source_set_callback(_source, timeout_handler, this, NULL);
  g_source_attach(_source, _ctx);
}

void Glib::BusTimeout::_disable()
{
  if (_source)
  {
    g_source_destroy(_source);
    _source = NULL;
  }
}

struct BusSource
{
  GSource source;
  GPollFD poll;
};

static gboolean watch_prepare(GSource *source, gint *timeout)
{
  debug_log("glib: watch_prepare");

  *timeout = -1;
  return FALSE;
}

static gboolean watch_check(GSource *source)
{
  debug_log("glib: watch_check");

  BusSource *io = (BusSource *)source;
  return io->poll.revents ? TRUE : FALSE;
}

static gboolean watch_dispatch(GSource *source, GSourceFunc callback, gpointer data)
{
  debug_log("glib: watch_dispatch");

  gboolean cb = callback(data);
  return cb;
}

static GSourceFuncs watch_funcs =
{
  watch_prepare,
  watch_check,
  watch_dispatch,
  NULL
};

Glib::BusWatch::BusWatch(Watch::Internal *wi, GMainContext *ctx, int priority)
  : Watch(wi), _ctx(ctx), _priority(priority), _source(NULL)
{
  if (Watch::enabled())
    _enable();
}

Glib::BusWatch::~BusWatch()
{
  _disable();
}

void Glib::BusWatch::toggle()
{
  debug_log("glib: watch %p toggled (%s)", this, Watch::enabled() ? "on" : "off");

  if (Watch::enabled())	_enable();
  else			_disable();
}

gboolean Glib::BusWatch::watch_handler(gpointer data)
{
  Glib::BusWatch *w = reinterpret_cast<Glib::BusWatch *>(data);

  BusSource *io = (BusSource *)(w->_source);

  int flags = 0;
  if (io->poll.revents & G_IO_IN)
    flags |= DBUS_WATCH_READABLE;
  if (io->poll.revents & G_IO_OUT)
    flags |= DBUS_WATCH_WRITABLE;
  if (io->poll.revents & G_IO_ERR)
    flags |= DBUS_WATCH_ERROR;
  if (io->poll.revents & G_IO_HUP)
    flags |= DBUS_WATCH_HANGUP;

  w->handle(flags);

  return TRUE;
}

void Glib::BusWatch::_enable()
{
  if (_source)
    _disable(); // be sane
  _source = g_source_new(&watch_funcs, sizeof(BusSource));
  g_source_set_priority(_source, _priority);
  g_source_set_callback(_source, watch_handler, this, NULL);

  int flags = Watch::flags();
  int condition = 0;

  if (flags & DBUS_WATCH_READABLE)
    condition |= G_IO_IN;
  if (flags & DBUS_WATCH_WRITABLE)
    condition |= G_IO_OUT;
  if (flags & DBUS_WATCH_ERROR)
    condition |= G_IO_ERR;
  if (flags & DBUS_WATCH_HANGUP)
    condition |= G_IO_HUP;

  GPollFD *poll = &(((BusSource *)_source)->poll);
  poll->fd = Watch::descriptor();
  poll->events = condition;
  poll->revents = 0;

  g_source_add_poll(_source, poll);
  g_source_attach(_source, _ctx);
}

void Glib::BusWatch::_disable()
{
  if (!_source)
    return;
  GPollFD *poll = &(((BusSource *)_source)->poll);
  g_source_remove_poll(_source, poll);
  g_source_destroy(_source);
  _source = NULL;
}

/*
 * We need this on top of the IO handlers, because sometimes
 * there are messages to dispatch queued up but no IO pending.
 * (fixes also a previous problem of code not working in case of multiple dispatchers)
*/
struct DispatcherSource
{
  GSource source;
  Dispatcher *dispatcher;
};


static gboolean dispatcher_prepare(GSource *source, gint *timeout)
{
  Dispatcher *dispatcher = ((DispatcherSource *)source)->dispatcher;

  *timeout = -1;

  return dispatcher->has_something_to_dispatch() ? TRUE : FALSE;
}

static gboolean dispatcher_check(GSource *source)
{
  return FALSE;
}

static gboolean
dispatcher_dispatch(GSource *source,
                    GSourceFunc callback,
                    gpointer user_data)
{
  Dispatcher *dispatcher = ((DispatcherSource *)source)->dispatcher;

  dispatcher->dispatch_pending();
  return TRUE;
}

static const GSourceFuncs dispatcher_funcs =
{
  dispatcher_prepare,
  dispatcher_check,
  dispatcher_dispatch,
  NULL
};

Glib::BusDispatcher::BusDispatcher()
  : _ctx(NULL), _priority(G_PRIORITY_DEFAULT), _source(NULL)
{
}

Glib::BusDispatcher::~BusDispatcher()
{
  if (_source)
  {
    GSource *temp = _source;
    _source = NULL;

    g_source_destroy(temp);
    g_source_unref(temp);
  }

  if (_ctx)
    g_main_context_unref(_ctx);
}

void Glib::BusDispatcher::attach(GMainContext *ctx)
{
  g_assert(_ctx == NULL); // just to be sane

  _ctx = ctx ? ctx : g_main_context_default();
  g_main_context_ref(_ctx);

  // create the source for dispatching messages
  _source = g_source_new((GSourceFuncs *) &dispatcher_funcs,
                         sizeof(DispatcherSource));

  ((DispatcherSource *)_source)->dispatcher = this;
  g_source_attach(_source, _ctx);
}

Timeout *Glib::BusDispatcher::add_timeout(Timeout::Internal *wi)
{
  Timeout *t = new Glib::BusTimeout(wi, _ctx, _priority);

  debug_log("glib: added timeout %p (%s)", t, t->enabled() ? "on" : "off");

  return t;
}

void Glib::BusDispatcher::rem_timeout(Timeout *t)
{
  debug_log("glib: removed timeout %p", t);

  delete t;
}

Watch *Glib::BusDispatcher::add_watch(Watch::Internal *wi)
{
  Watch *w = new Glib::BusWatch(wi, _ctx, _priority);

  debug_log("glib: added watch %p (%s) fd=%d flags=%d",
            w, w->enabled() ? "on" : "off", w->descriptor(), w->flags()
           );
  return w;
}

void Glib::BusDispatcher::rem_watch(Watch *w)
{
  debug_log("glib: removed watch %p", w);

  delete w;
}

void Glib::BusDispatcher::set_priority(int priority)
{
  _priority = priority;
}
