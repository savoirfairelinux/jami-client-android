/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-mainloop.c  Main loop utility
 *
 * Copyright (C) 2003, 2004  Red Hat, Inc.
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
#include "dbus-mainloop.h"

#ifndef DOXYGEN_SHOULD_SKIP_THIS

#include <dbus/dbus-list.h>
#include <dbus/dbus-sysdeps.h>

#define MAINLOOP_SPEW 0

#if MAINLOOP_SPEW
#ifdef DBUS_ENABLE_VERBOSE_MODE
static const char*
watch_flags_to_string (int flags)
{
  const char *watch_type;

  if ((flags & DBUS_WATCH_READABLE) &&
      (flags & DBUS_WATCH_WRITABLE))
    watch_type = "readwrite";
  else if (flags & DBUS_WATCH_READABLE)
    watch_type = "read";
  else if (flags & DBUS_WATCH_WRITABLE)
    watch_type = "write";
  else
    watch_type = "not read or write";
  return watch_type;
}
#endif /* DBUS_ENABLE_VERBOSE_MODE */
#endif /* MAINLOOP_SPEW */

struct DBusLoop
{
  int refcount;
  DBusList *callbacks;
  int callback_list_serial;
  int watch_count;
  int timeout_count;
  int depth; /**< number of recursive runs */
  DBusList *need_dispatch;
};

typedef enum
{
  CALLBACK_WATCH,
  CALLBACK_TIMEOUT
} CallbackType;

typedef struct
{
  int refcount;
  CallbackType type;
  void *data;
  DBusFreeFunction free_data_func;
} Callback;

typedef struct
{
  Callback callback;
  DBusWatchFunction function;
  DBusWatch *watch;
  /* last watch handle failed due to OOM */
  unsigned int last_iteration_oom : 1;
} WatchCallback;

typedef struct
{
  Callback callback;
  DBusTimeout *timeout;
  DBusTimeoutFunction function;
  unsigned long last_tv_sec;
  unsigned long last_tv_usec;
} TimeoutCallback;

#define WATCH_CALLBACK(callback)   ((WatchCallback*)callback)
#define TIMEOUT_CALLBACK(callback) ((TimeoutCallback*)callback)

static WatchCallback*
watch_callback_new (DBusWatch         *watch,
                    DBusWatchFunction  function,
                    void              *data,
                    DBusFreeFunction   free_data_func)
{
  WatchCallback *cb;

  cb = dbus_new (WatchCallback, 1);
  if (cb == NULL)
    return NULL;

  cb->watch = watch;
  cb->function = function;
  cb->last_iteration_oom = FALSE;
  cb->callback.refcount = 1;
  cb->callback.type = CALLBACK_WATCH;
  cb->callback.data = data;
  cb->callback.free_data_func = free_data_func;
  
  return cb;
}

static TimeoutCallback*
timeout_callback_new (DBusTimeout         *timeout,
                      DBusTimeoutFunction  function,
                      void                *data,
                      DBusFreeFunction     free_data_func)
{
  TimeoutCallback *cb;

  cb = dbus_new (TimeoutCallback, 1);
  if (cb == NULL)
    return NULL;

  cb->timeout = timeout;
  cb->function = function;
  _dbus_get_current_time (&cb->last_tv_sec,
                          &cb->last_tv_usec);
  cb->callback.refcount = 1;    
  cb->callback.type = CALLBACK_TIMEOUT;
  cb->callback.data = data;
  cb->callback.free_data_func = free_data_func;
  
  return cb;
}

static Callback * 
callback_ref (Callback *cb)
{
  _dbus_assert (cb->refcount > 0);
  
  cb->refcount += 1;

  return cb;
}

static void
callback_unref (Callback *cb)
{
  _dbus_assert (cb->refcount > 0);

  cb->refcount -= 1;

  if (cb->refcount == 0)
    {
      if (cb->free_data_func)
        (* cb->free_data_func) (cb->data);
      
      dbus_free (cb);
    }
}

static dbus_bool_t
add_callback (DBusLoop  *loop,
              Callback *cb)
{
  if (!_dbus_list_append (&loop->callbacks, cb))
    return FALSE;

  loop->callback_list_serial += 1;

  switch (cb->type)
    {
    case CALLBACK_WATCH:
      loop->watch_count += 1;
      break;
    case CALLBACK_TIMEOUT:
      loop->timeout_count += 1;
      break;
    }
  
  return TRUE;
}

static void
remove_callback (DBusLoop  *loop,
                 DBusList *link)
{
  Callback *cb = link->data;
  
  switch (cb->type)
    {
    case CALLBACK_WATCH:
      loop->watch_count -= 1;
      break;
    case CALLBACK_TIMEOUT:
      loop->timeout_count -= 1;
      break;
    }
  
  callback_unref (cb);
  _dbus_list_remove_link (&loop->callbacks, link);
  loop->callback_list_serial += 1;
}

DBusLoop*
_dbus_loop_new (void)
{
  DBusLoop *loop;

  loop = dbus_new0 (DBusLoop, 1);
  if (loop == NULL)
    return NULL;

  loop->refcount = 1;
  
  return loop;
}

DBusLoop *
_dbus_loop_ref (DBusLoop *loop)
{
  _dbus_assert (loop != NULL);
  _dbus_assert (loop->refcount > 0);

  loop->refcount += 1;

  return loop;
}

void
_dbus_loop_unref (DBusLoop *loop)
{
  _dbus_assert (loop != NULL);
  _dbus_assert (loop->refcount > 0);

  loop->refcount -= 1;
  if (loop->refcount == 0)
    {
      while (loop->need_dispatch)
        {
          DBusConnection *connection = _dbus_list_pop_first (&loop->need_dispatch);

          dbus_connection_unref (connection);
        }
      
      dbus_free (loop);
    }
}

dbus_bool_t
_dbus_loop_add_watch (DBusLoop          *loop,
                      DBusWatch        *watch,
                      DBusWatchFunction  function,
                      void             *data,
                      DBusFreeFunction  free_data_func)
{
  WatchCallback *wcb;

  wcb = watch_callback_new (watch, function, data, free_data_func);
  if (wcb == NULL)
    return FALSE;

  if (!add_callback (loop, (Callback*) wcb))
    {
      wcb->callback.free_data_func = NULL; /* don't want to have this side effect */
      callback_unref ((Callback*) wcb);
      return FALSE;
    }
  
  return TRUE;
}

void
_dbus_loop_remove_watch (DBusLoop          *loop,
                         DBusWatch        *watch,
                         DBusWatchFunction  function,
                         void             *data)
{
  DBusList *link;
  
  link = _dbus_list_get_first_link (&loop->callbacks);
  while (link != NULL)
    {
      DBusList *next = _dbus_list_get_next_link (&loop->callbacks, link);
      Callback *this = link->data;

      if (this->type == CALLBACK_WATCH &&
          WATCH_CALLBACK (this)->watch == watch &&
          this->data == data &&
          WATCH_CALLBACK (this)->function == function)
        {
          remove_callback (loop, link);
          
          return;
        }
      
      link = next;
    }

  _dbus_warn ("could not find watch %p function %p data %p to remove\n",
              watch, (void *)function, data);
}

dbus_bool_t
_dbus_loop_add_timeout (DBusLoop            *loop,
                        DBusTimeout        *timeout,
                        DBusTimeoutFunction  function,
                        void               *data,
                        DBusFreeFunction    free_data_func)
{
  TimeoutCallback *tcb;

  tcb = timeout_callback_new (timeout, function, data, free_data_func);
  if (tcb == NULL)
    return FALSE;

  if (!add_callback (loop, (Callback*) tcb))
    {
      tcb->callback.free_data_func = NULL; /* don't want to have this side effect */
      callback_unref ((Callback*) tcb);
      return FALSE;
    }
  
  return TRUE;
}

void
_dbus_loop_remove_timeout (DBusLoop            *loop,
                           DBusTimeout        *timeout,
                           DBusTimeoutFunction  function,
                           void               *data)
{
  DBusList *link;
  
  link = _dbus_list_get_first_link (&loop->callbacks);
  while (link != NULL)
    {
      DBusList *next = _dbus_list_get_next_link (&loop->callbacks, link);
      Callback *this = link->data;

      if (this->type == CALLBACK_TIMEOUT &&
          TIMEOUT_CALLBACK (this)->timeout == timeout &&
          this->data == data &&
          TIMEOUT_CALLBACK (this)->function == function)
        {
          remove_callback (loop, link);
          
          return;
        }
      
      link = next;
    }

  _dbus_warn ("could not find timeout %p function %p data %p to remove\n",
              timeout, (void *)function, data);
}

/* Convolutions from GLib, there really must be a better way
 * to do this.
 */
static dbus_bool_t
check_timeout (unsigned long    tv_sec,
               unsigned long    tv_usec,
               TimeoutCallback *tcb,
               int             *timeout)
{
  long sec_remaining;
  long msec_remaining;
  unsigned long expiration_tv_sec;
  unsigned long expiration_tv_usec;
  long interval_seconds;
  long interval_milliseconds;
  int interval;

  /* I'm pretty sure this function could suck (a lot) less */
  
  interval = dbus_timeout_get_interval (tcb->timeout);
  
  interval_seconds = interval / 1000L;
  interval_milliseconds = interval % 1000L;
  
  expiration_tv_sec = tcb->last_tv_sec + interval_seconds;
  expiration_tv_usec = tcb->last_tv_usec + interval_milliseconds * 1000;
  if (expiration_tv_usec >= 1000000)
    {
      expiration_tv_usec -= 1000000;
      expiration_tv_sec += 1;
    }
  
  sec_remaining = expiration_tv_sec - tv_sec;
  /* need to force this to be signed, as it is intended to sometimes
   * produce a negative result
   */
  msec_remaining = ((long) expiration_tv_usec - (long) tv_usec) / 1000L;

#if MAINLOOP_SPEW
  _dbus_verbose ("Interval is %ld seconds %ld msecs\n",
                 interval_seconds,
                 interval_milliseconds);
  _dbus_verbose ("Now is  %lu seconds %lu usecs\n",
                 tv_sec, tv_usec);
  _dbus_verbose ("Last is %lu seconds %lu usecs\n",
                 tcb->last_tv_sec, tcb->last_tv_usec);
  _dbus_verbose ("Exp is  %lu seconds %lu usecs\n",
                 expiration_tv_sec, expiration_tv_usec);
  _dbus_verbose ("Pre-correction, sec_remaining %ld msec_remaining %ld\n",
                 sec_remaining, msec_remaining);
#endif
  
  /* We do the following in a rather convoluted fashion to deal with
   * the fact that we don't have an integral type big enough to hold
   * the difference of two timevals in milliseconds.
   */
  if (sec_remaining < 0 || (sec_remaining == 0 && msec_remaining < 0))
    {
      *timeout = 0;
    }
  else
    {
      if (msec_remaining < 0)
	{
	  msec_remaining += 1000;
	  sec_remaining -= 1;
	}

      if (sec_remaining > (_DBUS_INT_MAX / 1000) ||
          msec_remaining > _DBUS_INT_MAX)
        *timeout = _DBUS_INT_MAX;
      else
        *timeout = sec_remaining * 1000 + msec_remaining;        
    }

  if (*timeout > interval)
    {
      /* This indicates that the system clock probably moved backward */
      _dbus_verbose ("System clock set backward! Resetting timeout.\n");
      
      tcb->last_tv_sec = tv_sec;
      tcb->last_tv_usec = tv_usec;

      *timeout = interval;
    }
  
#if MAINLOOP_SPEW
  _dbus_verbose ("  timeout expires in %d milliseconds\n", *timeout);
#endif
  
  return *timeout == 0;
}

dbus_bool_t
_dbus_loop_dispatch (DBusLoop *loop)
{

#if MAINLOOP_SPEW
  _dbus_verbose ("  %d connections to dispatch\n", _dbus_list_get_length (&loop->need_dispatch));
#endif
  
  if (loop->need_dispatch == NULL)
    return FALSE;
  
 next:
  while (loop->need_dispatch != NULL)
    {
      DBusConnection *connection = _dbus_list_pop_first (&loop->need_dispatch);
      
      while (TRUE)
        {
          DBusDispatchStatus status;
          
          status = dbus_connection_dispatch (connection);

          if (status == DBUS_DISPATCH_COMPLETE)
            {
              dbus_connection_unref (connection);
              goto next;
            }
          else
            {
              if (status == DBUS_DISPATCH_NEED_MEMORY)
                _dbus_wait_for_memory ();
            }
        }
    }

  return TRUE;
}

dbus_bool_t
_dbus_loop_queue_dispatch (DBusLoop       *loop,
                           DBusConnection *connection)
{
  if (_dbus_list_append (&loop->need_dispatch, connection))
    {
      dbus_connection_ref (connection);
      return TRUE;
    }
  else
    return FALSE;
}

/* Returns TRUE if we invoked any timeouts or have ready file
 * descriptors, which is just used in test code as a debug hack
 */

dbus_bool_t
_dbus_loop_iterate (DBusLoop     *loop,
                    dbus_bool_t   block)
{  
#define N_STACK_DESCRIPTORS 64
  dbus_bool_t retval;
  DBusPollFD *fds;
  DBusPollFD stack_fds[N_STACK_DESCRIPTORS];
  int n_fds;
  WatchCallback **watches_for_fds;
  WatchCallback *stack_watches_for_fds[N_STACK_DESCRIPTORS];
  int i;
  DBusList *link;
  int n_ready;
  int initial_serial;
  long timeout;
  dbus_bool_t oom_watch_pending;
  int orig_depth;
  
  retval = FALSE;      

  fds = NULL;
  watches_for_fds = NULL;
  n_fds = 0;
  oom_watch_pending = FALSE;
  orig_depth = loop->depth;
  
#if MAINLOOP_SPEW
  _dbus_verbose ("Iteration block=%d depth=%d timeout_count=%d watch_count=%d\n",
                 block, loop->depth, loop->timeout_count, loop->watch_count);
#endif
  
  if (loop->callbacks == NULL)
    goto next_iteration;

  if (loop->watch_count > N_STACK_DESCRIPTORS)
    {
      fds = dbus_new0 (DBusPollFD, loop->watch_count);
      
      while (fds == NULL)
        {
          _dbus_wait_for_memory ();
          fds = dbus_new0 (DBusPollFD, loop->watch_count);
        }
      
      watches_for_fds = dbus_new (WatchCallback*, loop->watch_count);
      while (watches_for_fds == NULL)
        {
          _dbus_wait_for_memory ();
          watches_for_fds = dbus_new (WatchCallback*, loop->watch_count);
        }
    }
  else
    {      
      fds = stack_fds;
      watches_for_fds = stack_watches_for_fds;
    }

  /* fill our array of fds and watches */
  n_fds = 0;
  link = _dbus_list_get_first_link (&loop->callbacks);
  while (link != NULL)
    {
      DBusList *next = _dbus_list_get_next_link (&loop->callbacks, link);
      Callback *cb = link->data;
      if (cb->type == CALLBACK_WATCH)
        {
          unsigned int flags;
          WatchCallback *wcb = WATCH_CALLBACK (cb);

          if (wcb->last_iteration_oom)
            {
              /* we skip this one this time, but reenable it next time,
               * and have a timeout on this iteration
               */
              wcb->last_iteration_oom = FALSE;
              oom_watch_pending = TRUE;
              
              retval = TRUE; /* return TRUE here to keep the loop going,
                              * since we don't know the watch is inactive
                              */

#if MAINLOOP_SPEW
              _dbus_verbose ("  skipping watch on fd %d as it was out of memory last time\n",
                             dbus_watch_get_socket (wcb->watch));
#endif
            }
          else if (dbus_watch_get_enabled (wcb->watch))
            {
              watches_for_fds[n_fds] = wcb;

              callback_ref (cb);
                  
              flags = dbus_watch_get_flags (wcb->watch);
                  
              fds[n_fds].fd = dbus_watch_get_socket (wcb->watch);
              fds[n_fds].revents = 0;
              fds[n_fds].events = 0;
              if (flags & DBUS_WATCH_READABLE)
                fds[n_fds].events |= _DBUS_POLLIN;
              if (flags & DBUS_WATCH_WRITABLE)
                fds[n_fds].events |= _DBUS_POLLOUT;

#if MAINLOOP_SPEW
              _dbus_verbose ("  polling watch on fd %d  %s\n",
                             fds[n_fds].fd, watch_flags_to_string (flags));
#endif

              n_fds += 1;
            }
          else
            {
#if MAINLOOP_SPEW
              _dbus_verbose ("  skipping disabled watch on fd %d  %s\n",
                             dbus_watch_get_socket (wcb->watch),
                             watch_flags_to_string (dbus_watch_get_flags (wcb->watch)));
#endif
            }
        }
              
      link = next;
    }
  
  timeout = -1;
  if (loop->timeout_count > 0)
    {
      unsigned long tv_sec;
      unsigned long tv_usec;
      
      _dbus_get_current_time (&tv_sec, &tv_usec);
          
      link = _dbus_list_get_first_link (&loop->callbacks);
      while (link != NULL)
        {
          DBusList *next = _dbus_list_get_next_link (&loop->callbacks, link);
          Callback *cb = link->data;

          if (cb->type == CALLBACK_TIMEOUT &&
              dbus_timeout_get_enabled (TIMEOUT_CALLBACK (cb)->timeout))
            {
              TimeoutCallback *tcb = TIMEOUT_CALLBACK (cb);
              int msecs_remaining;

              check_timeout (tv_sec, tv_usec, tcb, &msecs_remaining);

              if (timeout < 0)
                timeout = msecs_remaining;
              else
                timeout = MIN (msecs_remaining, timeout);

#if MAINLOOP_SPEW
              _dbus_verbose ("  timeout added, %d remaining, aggregate timeout %ld\n",
                             msecs_remaining, timeout);
#endif
              
              _dbus_assert (timeout >= 0);
                  
              if (timeout == 0)
                break; /* it's not going to get shorter... */
            }
#if MAINLOOP_SPEW
          else if (cb->type == CALLBACK_TIMEOUT)
            {
              _dbus_verbose ("  skipping disabled timeout\n");
            }
#endif
          
          link = next;
        }
    }

  /* Never block if we have stuff to dispatch */
  if (!block || loop->need_dispatch != NULL)
    {
      timeout = 0;
#if MAINLOOP_SPEW
      _dbus_verbose ("  timeout is 0 as we aren't blocking\n");
#endif
    }

  /* if a watch is OOM, don't wait longer than the OOM
   * wait to re-enable it
   */
  if (oom_watch_pending)
    timeout = MIN (timeout, _dbus_get_oom_wait ());

#if MAINLOOP_SPEW
  _dbus_verbose ("  polling on %d descriptors timeout %ld\n", n_fds, timeout);
#endif
  
  n_ready = _dbus_poll (fds, n_fds, timeout);

  initial_serial = loop->callback_list_serial;

  if (loop->timeout_count > 0)
    {
      unsigned long tv_sec;
      unsigned long tv_usec;

      _dbus_get_current_time (&tv_sec, &tv_usec);

      /* It'd be nice to avoid this O(n) thingy here */
      link = _dbus_list_get_first_link (&loop->callbacks);
      while (link != NULL)
        {
          DBusList *next = _dbus_list_get_next_link (&loop->callbacks, link);
          Callback *cb = link->data;

          if (initial_serial != loop->callback_list_serial)
            goto next_iteration;

          if (loop->depth != orig_depth)
            goto next_iteration;
              
          if (cb->type == CALLBACK_TIMEOUT &&
              dbus_timeout_get_enabled (TIMEOUT_CALLBACK (cb)->timeout))
            {
              TimeoutCallback *tcb = TIMEOUT_CALLBACK (cb);
              int msecs_remaining;
              
              if (check_timeout (tv_sec, tv_usec,
                                 tcb, &msecs_remaining))
                {
                  /* Save last callback time and fire this timeout */
                  tcb->last_tv_sec = tv_sec;
                  tcb->last_tv_usec = tv_usec;

#if MAINLOOP_SPEW
                  _dbus_verbose ("  invoking timeout\n");
#endif
                  
                  (* tcb->function) (tcb->timeout,
                                     cb->data);

                  retval = TRUE;
                }
              else
                {
#if MAINLOOP_SPEW
                  _dbus_verbose ("  timeout has not expired\n");
#endif
                }
            }
#if MAINLOOP_SPEW
          else if (cb->type == CALLBACK_TIMEOUT)
            {
              _dbus_verbose ("  skipping invocation of disabled timeout\n");
            }
#endif

          link = next;
        }
    }
      
  if (n_ready > 0)
    {
      i = 0;
      while (i < n_fds)
        {
          /* FIXME I think this "restart if we change the watches"
           * approach could result in starving watches
           * toward the end of the list.
           */
          if (initial_serial != loop->callback_list_serial)
            goto next_iteration;

          if (loop->depth != orig_depth)
            goto next_iteration;

          if (fds[i].revents != 0)
            {
              WatchCallback *wcb;
              unsigned int condition;
                  
              wcb = watches_for_fds[i];
              
              condition = 0;
              if (fds[i].revents & _DBUS_POLLIN)
                condition |= DBUS_WATCH_READABLE;
              if (fds[i].revents & _DBUS_POLLOUT)
                condition |= DBUS_WATCH_WRITABLE;
              if (fds[i].revents & _DBUS_POLLHUP)
                condition |= DBUS_WATCH_HANGUP;
              if (fds[i].revents & _DBUS_POLLERR)
                condition |= DBUS_WATCH_ERROR;

              /* condition may still be 0 if we got some
               * weird POLLFOO thing like POLLWRBAND
               */
                  
              if (condition != 0 &&
                  dbus_watch_get_enabled (wcb->watch))
                {
                  if (!(* wcb->function) (wcb->watch,
                                          condition,
                                          ((Callback*)wcb)->data))
                    wcb->last_iteration_oom = TRUE;

#if MAINLOOP_SPEW
                  _dbus_verbose ("  Invoked watch, oom = %d\n",
                                 wcb->last_iteration_oom);
#endif
                  
                  retval = TRUE;
                }
            }
              
          ++i;
        }
    }
      
 next_iteration:
#if MAINLOOP_SPEW
  _dbus_verbose ("  moving to next iteration\n");
#endif
  
  if (fds && fds != stack_fds)
    dbus_free (fds);
  if (watches_for_fds)
    {
      i = 0;
      while (i < n_fds)
        {
          callback_unref (&watches_for_fds[i]->callback);
          ++i;
        }
      
      if (watches_for_fds != stack_watches_for_fds)
        dbus_free (watches_for_fds);
    }
  
  if (_dbus_loop_dispatch (loop))
    retval = TRUE;
  
#if MAINLOOP_SPEW
  _dbus_verbose ("Returning %d\n", retval);
#endif
  
  return retval;
}

void
_dbus_loop_run (DBusLoop *loop)
{
  int our_exit_depth;

  _dbus_assert (loop->depth >= 0);
  
  _dbus_loop_ref (loop);
  
  our_exit_depth = loop->depth;
  loop->depth += 1;

  _dbus_verbose ("Running main loop, depth %d -> %d\n",
                 loop->depth - 1, loop->depth);
  
  while (loop->depth != our_exit_depth)
    _dbus_loop_iterate (loop, TRUE);

  _dbus_loop_unref (loop);
}

void
_dbus_loop_quit (DBusLoop *loop)
{
  _dbus_assert (loop->depth > 0);  
  
  loop->depth -= 1;

  _dbus_verbose ("Quit main loop, depth %d -> %d\n",
                 loop->depth + 1, loop->depth);
}

int
_dbus_get_oom_wait (void)
{
#ifdef DBUS_BUILD_TESTS
  /* make tests go fast */
  return 0;
#else
  return 500;
#endif
}

void
_dbus_wait_for_memory (void)
{
  _dbus_verbose ("Waiting for more memory\n");
  _dbus_sleep_milliseconds (_dbus_get_oom_wait ());
}

#endif /* !DOXYGEN_SHOULD_SKIP_THIS */
