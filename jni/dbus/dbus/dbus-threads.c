/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-threads.h  D-Bus threads handling
 *
 * Copyright (C) 2002, 2003, 2006 Red Hat Inc.
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
#include "dbus-threads.h"
#include "dbus-internals.h"
#include "dbus-threads-internal.h"
#include "dbus-list.h"

static DBusThreadFunctions thread_functions =
{
  0,
  NULL, NULL, NULL, NULL, NULL,
  NULL, NULL, NULL, NULL, NULL,
  NULL, NULL, NULL, NULL,
  
  NULL, NULL, NULL, NULL
};

static int thread_init_generation = 0;
 
static DBusList *uninitialized_mutex_list = NULL;
static DBusList *uninitialized_condvar_list = NULL;

/** This is used for the no-op default mutex pointer, just to be distinct from #NULL */
#define _DBUS_DUMMY_MUTEX ((DBusMutex*)0xABCDEF)

/** This is used for the no-op default mutex pointer, just to be distinct from #NULL */
#define _DBUS_DUMMY_CONDVAR ((DBusCondVar*)0xABCDEF2)

/**
 * @defgroup DBusThreadsInternals Thread functions
 * @ingroup  DBusInternals
 * @brief _dbus_mutex_lock(), etc.
 *
 * Functions and macros related to threads and thread locks.
 *
 * @{
 */

/**
 * Creates a new mutex using the function supplied to dbus_threads_init(),
 * or creates a no-op mutex if threads are not initialized.
 * May return #NULL even if threads are initialized, indicating
 * out-of-memory.
 *
 * @returns new mutex or #NULL
 */
DBusMutex*
_dbus_mutex_new (void)
{
  if (thread_functions.recursive_mutex_new)
    return (* thread_functions.recursive_mutex_new) ();
  else if (thread_functions.mutex_new)
    return (* thread_functions.mutex_new) ();
  else
    return _DBUS_DUMMY_MUTEX;
}

/**
 * This does the same thing as _dbus_mutex_new.  It however
 * gives another level of indirection by allocating a pointer
 * to point to the mutex location.  This allows the threading
 * module to swap out dummy mutexes for real a real mutex so libraries
 * can initialize threads even after the D-Bus API has been used.
 *
 * @param location_p the location of the new mutex, can return #NULL on OOM
 */
void
_dbus_mutex_new_at_location (DBusMutex **location_p)
{
  _dbus_assert (location_p != NULL);

  *location_p = _dbus_mutex_new();

  if (thread_init_generation != _dbus_current_generation && *location_p)
    {
      if (!_dbus_list_append (&uninitialized_mutex_list, location_p))
        {
	  _dbus_mutex_free (*location_p);
	  *location_p = NULL;
	}
    }
}

/**
 * Frees a mutex created with dbus_mutex_new(); does
 * nothing if passed a #NULL pointer.
 */
void
_dbus_mutex_free (DBusMutex *mutex)
{
  if (mutex)
    {
      if (mutex && thread_functions.recursive_mutex_free)
        (* thread_functions.recursive_mutex_free) (mutex);
      else if (mutex && thread_functions.mutex_free)
        (* thread_functions.mutex_free) (mutex);
    }
}

/**
 * Frees a mutex and removes it from the 
 * uninitialized_mutex_list;
 * does nothing if passed a #NULL pointer.
 */
void
_dbus_mutex_free_at_location (DBusMutex **location_p)
{
  if (location_p)
    {
      if (thread_init_generation != _dbus_current_generation)
        _dbus_list_remove (&uninitialized_mutex_list, location_p);

      _dbus_mutex_free (*location_p);
    }
}

/**
 * Locks a mutex. Does nothing if passed a #NULL pointer.
 * Locks may be recursive if threading implementation initialized
 * recursive locks.
 */
void
_dbus_mutex_lock (DBusMutex *mutex)
{
  if (mutex) 
    {
      if (thread_functions.recursive_mutex_lock)
        (* thread_functions.recursive_mutex_lock) (mutex);
      else if (thread_functions.mutex_lock)
        (* thread_functions.mutex_lock) (mutex);
    }
}

/**
 * Unlocks a mutex. Does nothing if passed a #NULL pointer.
 *
 * @returns #TRUE on success
 */
void
_dbus_mutex_unlock (DBusMutex *mutex)
{
  if (mutex)
    {
      if (thread_functions.recursive_mutex_unlock)
        (* thread_functions.recursive_mutex_unlock) (mutex);
      else if (thread_functions.mutex_unlock)
        (* thread_functions.mutex_unlock) (mutex);
    }
}

/**
 * Creates a new condition variable using the function supplied
 * to dbus_threads_init(), or creates a no-op condition variable
 * if threads are not initialized. May return #NULL even if
 * threads are initialized, indicating out-of-memory.
 *
 * @returns new mutex or #NULL
 */
DBusCondVar *
_dbus_condvar_new (void)
{
  if (thread_functions.condvar_new)
    return (* thread_functions.condvar_new) ();
  else
    return _DBUS_DUMMY_CONDVAR;
}


/**
 * This does the same thing as _dbus_condvar_new.  It however
 * gives another level of indirection by allocating a pointer
 * to point to the condvar location.  This allows the threading
 * module to swap out dummy condvars for real a real condvar so libraries
 * can initialize threads even after the D-Bus API has been used.
 *
 * @returns the location of a new condvar or #NULL on OOM
 */

void 
_dbus_condvar_new_at_location (DBusCondVar **location_p)
{
  *location_p = _dbus_condvar_new();

  if (thread_init_generation != _dbus_current_generation && *location_p)
    {
      if (!_dbus_list_append (&uninitialized_condvar_list, location_p))
        {
          _dbus_condvar_free (*location_p);
          *location_p = NULL;
        }
    }
}


/**
 * Frees a conditional variable created with dbus_condvar_new(); does
 * nothing if passed a #NULL pointer.
 */
void
_dbus_condvar_free (DBusCondVar *cond)
{
  if (cond && thread_functions.condvar_free)
    (* thread_functions.condvar_free) (cond);
}

/**
 * Frees a conditional variable and removes it from the 
 * uninitialized_condvar_list; 
 * does nothing if passed a #NULL pointer.
 */
void
_dbus_condvar_free_at_location (DBusCondVar **location_p)
{
  if (location_p)
    {
      if (thread_init_generation != _dbus_current_generation)
        _dbus_list_remove (&uninitialized_condvar_list, location_p);

      _dbus_condvar_free (*location_p);
    }
}

/**
 * Atomically unlocks the mutex and waits for the conditions
 * variable to be signalled. Locks the mutex again before
 * returning.
 * Does nothing if passed a #NULL pointer.
 */
void
_dbus_condvar_wait (DBusCondVar *cond,
                    DBusMutex   *mutex)
{
  if (cond && mutex && thread_functions.condvar_wait)
    (* thread_functions.condvar_wait) (cond, mutex);
}

/**
 * Atomically unlocks the mutex and waits for the conditions variable
 * to be signalled, or for a timeout. Locks the mutex again before
 * returning.  Does nothing if passed a #NULL pointer.  Return value
 * is #FALSE if we timed out, #TRUE otherwise.
 *
 * @param cond the condition variable
 * @param mutex the mutex
 * @param timeout_milliseconds the maximum time to wait
 * @returns #FALSE if the timeout occurred, #TRUE if not
 */
dbus_bool_t
_dbus_condvar_wait_timeout (DBusCondVar               *cond,
                            DBusMutex                 *mutex,
                            int                        timeout_milliseconds)
{
  if (cond && mutex && thread_functions.condvar_wait)
    return (* thread_functions.condvar_wait_timeout) (cond, mutex, timeout_milliseconds);
  else
    return TRUE;
}

/**
 * If there are threads waiting on the condition variable, wake
 * up exactly one. 
 * Does nothing if passed a #NULL pointer.
 */
void
_dbus_condvar_wake_one (DBusCondVar *cond)
{
  if (cond && thread_functions.condvar_wake_one)
    (* thread_functions.condvar_wake_one) (cond);
}

/**
 * If there are threads waiting on the condition variable, wake
 * up all of them. 
 * Does nothing if passed a #NULL pointer.
 */
void
_dbus_condvar_wake_all (DBusCondVar *cond)
{
  if (cond && thread_functions.condvar_wake_all)
    (* thread_functions.condvar_wake_all) (cond);
}

static void
shutdown_global_locks (void *data)
{
  DBusMutex ***locks = data;
  int i;

  i = 0;
  while (i < _DBUS_N_GLOBAL_LOCKS)
    {
      _dbus_mutex_free (*(locks[i]));
      *(locks[i]) = NULL;
      ++i;
    }
  
  dbus_free (locks);
}

static void
shutdown_uninitialized_locks (void *data)
{
  _dbus_list_clear (&uninitialized_mutex_list);
  _dbus_list_clear (&uninitialized_condvar_list);
}

static dbus_bool_t
init_uninitialized_locks (void)
{
  DBusList *link;

  _dbus_assert (thread_init_generation != _dbus_current_generation);

  link = uninitialized_mutex_list;
  while (link != NULL)
    {
      DBusMutex **mp;

      mp = (DBusMutex **)link->data;
      _dbus_assert (*mp == _DBUS_DUMMY_MUTEX);

      *mp = _dbus_mutex_new ();
      if (*mp == NULL)
        goto fail_mutex;

      link = _dbus_list_get_next_link (&uninitialized_mutex_list, link);
    }

  link = uninitialized_condvar_list;
  while (link != NULL)
    {
      DBusCondVar **cp;

      cp = (DBusCondVar **)link->data;
      _dbus_assert (*cp == _DBUS_DUMMY_CONDVAR);

      *cp = _dbus_condvar_new ();
      if (*cp == NULL)
        goto fail_condvar;

      link = _dbus_list_get_next_link (&uninitialized_condvar_list, link);
    }

  _dbus_list_clear (&uninitialized_mutex_list);
  _dbus_list_clear (&uninitialized_condvar_list);

  if (!_dbus_register_shutdown_func (shutdown_uninitialized_locks,
                                     NULL))
    goto fail_condvar;

  return TRUE;

 fail_condvar:
  link = uninitialized_condvar_list;
  while (link != NULL)
    {
      DBusCondVar **cp;

      cp = (DBusCondVar **)link->data;

      if (*cp != _DBUS_DUMMY_CONDVAR)
        _dbus_condvar_free (*cp);
      else
        break;

      *cp = _DBUS_DUMMY_CONDVAR;

      link = _dbus_list_get_next_link (&uninitialized_condvar_list, link);
    }

 fail_mutex:
  link = uninitialized_mutex_list;
  while (link != NULL)
    {
      DBusMutex **mp;

      mp = (DBusMutex **)link->data;

      if (*mp != _DBUS_DUMMY_MUTEX)
        _dbus_mutex_free (*mp);
      else
        break;

      *mp = _DBUS_DUMMY_MUTEX;

      link = _dbus_list_get_next_link (&uninitialized_mutex_list, link);
    }

  return FALSE;
}

static dbus_bool_t
init_locks (void)
{
  int i;
  DBusMutex ***dynamic_global_locks;
  
  DBusMutex **global_locks[] = {
#define LOCK_ADDR(name) (& _dbus_lock_##name)
    LOCK_ADDR (win_fds),
    LOCK_ADDR (sid_atom_cache),
    LOCK_ADDR (list),
    LOCK_ADDR (connection_slots),
    LOCK_ADDR (pending_call_slots),
    LOCK_ADDR (server_slots),
    LOCK_ADDR (message_slots),
#if !DBUS_USE_SYNC
    LOCK_ADDR (atomic),
#endif
    LOCK_ADDR (bus),
    LOCK_ADDR (bus_datas),
    LOCK_ADDR (shutdown_funcs),
    LOCK_ADDR (system_users),
    LOCK_ADDR (message_cache),
    LOCK_ADDR (shared_connections),
    LOCK_ADDR (machine_uuid)
#undef LOCK_ADDR
  };

  _dbus_assert (_DBUS_N_ELEMENTS (global_locks) ==
                _DBUS_N_GLOBAL_LOCKS);

  i = 0;
  
  dynamic_global_locks = dbus_new (DBusMutex**, _DBUS_N_GLOBAL_LOCKS);
  if (dynamic_global_locks == NULL)
    goto failed;
  
  while (i < _DBUS_N_ELEMENTS (global_locks))
    {
      *global_locks[i] = _dbus_mutex_new ();
      
      if (*global_locks[i] == NULL)
        goto failed;

      dynamic_global_locks[i] = global_locks[i];

      ++i;
    }
  
  if (!_dbus_register_shutdown_func (shutdown_global_locks,
                                     dynamic_global_locks))
    goto failed;

  if (!init_uninitialized_locks ())
    goto failed;
  
  return TRUE;

 failed:
  dbus_free (dynamic_global_locks);
                                     
  for (i = i - 1; i >= 0; i--)
    {
      _dbus_mutex_free (*global_locks[i]);
      *global_locks[i] = NULL;
    }
  return FALSE;
}

/** @} */ /* end of internals */

/**
 * @defgroup DBusThreads Thread functions
 * @ingroup  DBus
 * @brief dbus_threads_init() and dbus_threads_init_default()
 *
 * Functions and macros related to threads and thread locks.
 *
 * If threads are initialized, the D-Bus library has locks on all
 * global data structures.  In addition, each #DBusConnection has a
 * lock, so only one thread at a time can touch the connection.  (See
 * @ref DBusConnection for more on connection locking.)
 *
 * Most other objects, however, do not have locks - they can only be
 * used from a single thread at a time, unless you lock them yourself.
 * For example, a #DBusMessage can't be modified from two threads
 * at once.
 * 
 * @{
 */

/**
 * 
 * Initializes threads. If this function is not called, the D-Bus
 * library will not lock any data structures.  If it is called, D-Bus
 * will do locking, at some cost in efficiency. Note that this
 * function must be called BEFORE the second thread is started.
 *
 * Almost always, you should use dbus_threads_init_default() instead.
 * The raw dbus_threads_init() is only useful if you require a
 * particular thread implementation for some reason.
 *
 * A possible reason to use dbus_threads_init() rather than
 * dbus_threads_init_default() is to insert debugging checks or print
 * statements.
 *
 * dbus_threads_init() may be called more than once.  The first one
 * wins and subsequent calls are ignored. (Unless you use
 * dbus_shutdown() to reset libdbus, which will let you re-init
 * threads.)
 *
 * Either recursive or nonrecursive mutex functions must be specified,
 * but not both. New code should provide only the recursive functions
 * - specifying the nonrecursive ones is deprecated.
 *
 * Because this function effectively sets global state, all code
 * running in a given application must agree on the thread
 * implementation. Most code won't care which thread implementation is
 * used, so there's no problem. However, usually libraries should not
 * call dbus_threads_init() or dbus_threads_init_default(), instead
 * leaving this policy choice to applications.
 *
 * The exception is for application frameworks (GLib, Qt, etc.)  and
 * D-Bus bindings based on application frameworks. These frameworks
 * define a cross-platform thread abstraction and can assume
 * applications using the framework are OK with using that thread
 * abstraction.
 *
 * However, even these app frameworks may find it easier to simply call
 * dbus_threads_init_default(), and there's no reason they shouldn't.
 * 
 * @param functions functions for using threads
 * @returns #TRUE on success, #FALSE if no memory
 */
dbus_bool_t
dbus_threads_init (const DBusThreadFunctions *functions)
{
  dbus_bool_t mutex_set;
  dbus_bool_t recursive_mutex_set;

  _dbus_assert (functions != NULL);

  /* these base functions are required. Future additions to
   * DBusThreadFunctions may be optional.
   */
  _dbus_assert (functions->mask & DBUS_THREAD_FUNCTIONS_CONDVAR_NEW_MASK);
  _dbus_assert (functions->mask & DBUS_THREAD_FUNCTIONS_CONDVAR_FREE_MASK);
  _dbus_assert (functions->mask & DBUS_THREAD_FUNCTIONS_CONDVAR_WAIT_MASK);
  _dbus_assert (functions->mask & DBUS_THREAD_FUNCTIONS_CONDVAR_WAIT_TIMEOUT_MASK);
  _dbus_assert (functions->mask & DBUS_THREAD_FUNCTIONS_CONDVAR_WAKE_ONE_MASK);
  _dbus_assert (functions->mask & DBUS_THREAD_FUNCTIONS_CONDVAR_WAKE_ALL_MASK);
  _dbus_assert (functions->condvar_new != NULL);
  _dbus_assert (functions->condvar_free != NULL);
  _dbus_assert (functions->condvar_wait != NULL);
  _dbus_assert (functions->condvar_wait_timeout != NULL);
  _dbus_assert (functions->condvar_wake_one != NULL);
  _dbus_assert (functions->condvar_wake_all != NULL);

  /* Either the mutex function set or recursive mutex set needs 
   * to be available but not both
   */
  mutex_set = (functions->mask & DBUS_THREAD_FUNCTIONS_MUTEX_NEW_MASK) &&  
              (functions->mask & DBUS_THREAD_FUNCTIONS_MUTEX_FREE_MASK) && 
              (functions->mask & DBUS_THREAD_FUNCTIONS_MUTEX_LOCK_MASK) &&
              (functions->mask & DBUS_THREAD_FUNCTIONS_MUTEX_UNLOCK_MASK) &&
               functions->mutex_new &&
               functions->mutex_free &&
               functions->mutex_lock &&
               functions->mutex_unlock;

  recursive_mutex_set = 
              (functions->mask & DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_NEW_MASK) && 
              (functions->mask & DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_FREE_MASK) && 
              (functions->mask & DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_LOCK_MASK) && 
              (functions->mask & DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_UNLOCK_MASK) &&
                functions->recursive_mutex_new &&
                functions->recursive_mutex_free &&
                functions->recursive_mutex_lock &&
                functions->recursive_mutex_unlock;

  if (!(mutex_set || recursive_mutex_set))
    _dbus_assert_not_reached ("Either the nonrecusrive or recursive mutex " 
                              "functions sets should be passed into "
                              "dbus_threads_init. Neither sets were passed.");

  if (mutex_set && recursive_mutex_set)
    _dbus_assert_not_reached ("Either the nonrecusrive or recursive mutex " 
                              "functions sets should be passed into "
                              "dbus_threads_init. Both sets were passed. "
                              "You most likely just want to set the recursive "
                              "mutex functions to avoid deadlocks in D-Bus.");
                          
  /* Check that all bits in the mask actually are valid mask bits.
   * ensures people won't write code that breaks when we add
   * new bits.
   */
  _dbus_assert ((functions->mask & ~DBUS_THREAD_FUNCTIONS_ALL_MASK) == 0);

  if (thread_init_generation != _dbus_current_generation)
    thread_functions.mask = 0; /* allow re-init in new generation */
 
  /* Silently allow multiple init
   * First init wins and D-Bus will always use its threading system 
   */ 
  if (thread_functions.mask != 0)
    return TRUE;
  
  thread_functions.mutex_new = functions->mutex_new;
  thread_functions.mutex_free = functions->mutex_free;
  thread_functions.mutex_lock = functions->mutex_lock;
  thread_functions.mutex_unlock = functions->mutex_unlock;
  
  thread_functions.condvar_new = functions->condvar_new;
  thread_functions.condvar_free = functions->condvar_free;
  thread_functions.condvar_wait = functions->condvar_wait;
  thread_functions.condvar_wait_timeout = functions->condvar_wait_timeout;
  thread_functions.condvar_wake_one = functions->condvar_wake_one;
  thread_functions.condvar_wake_all = functions->condvar_wake_all;
 
  if (functions->mask & DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_NEW_MASK)
    thread_functions.recursive_mutex_new = functions->recursive_mutex_new;
  
  if (functions->mask & DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_FREE_MASK)
    thread_functions.recursive_mutex_free = functions->recursive_mutex_free;
  
  if (functions->mask & DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_LOCK_MASK)
    thread_functions.recursive_mutex_lock = functions->recursive_mutex_lock;

  if (functions->mask & DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_UNLOCK_MASK)
    thread_functions.recursive_mutex_unlock = functions->recursive_mutex_unlock;

  thread_functions.mask = functions->mask;

  if (!init_locks ())
    return FALSE;

  thread_init_generation = _dbus_current_generation;
  
  return TRUE;
}



/* Default thread implemenation */

/**
 *
 * Calls dbus_threads_init() with a default set of
 * #DBusThreadFunctions appropriate for the platform.
 *
 * Most applications should use this rather than dbus_threads_init().
 *
 * It's safe to call dbus_threads_init_default() as many times as you
 * want, but only the first time will have an effect.
 *
 * dbus_shutdown() reverses the effects of this function when it
 * resets all global state in libdbus.
 * 
 * @returns #TRUE on success, #FALSE if not enough memory
 */
dbus_bool_t
dbus_threads_init_default (void)
{
  return _dbus_threads_init_platform_specific ();
}


/** @} */

#ifdef DBUS_BUILD_TESTS
/** Fake mutex used for debugging */
typedef struct DBusFakeMutex DBusFakeMutex;
/** Fake mutex used for debugging */
struct DBusFakeMutex
{
  dbus_bool_t locked; /**< Mutex is "locked" */
};	

static DBusMutex *  dbus_fake_mutex_new            (void);
static void         dbus_fake_mutex_free           (DBusMutex   *mutex);
static dbus_bool_t  dbus_fake_mutex_lock           (DBusMutex   *mutex);
static dbus_bool_t  dbus_fake_mutex_unlock         (DBusMutex   *mutex);
static DBusCondVar* dbus_fake_condvar_new          (void);
static void         dbus_fake_condvar_free         (DBusCondVar *cond);
static void         dbus_fake_condvar_wait         (DBusCondVar *cond,
                                                    DBusMutex   *mutex);
static dbus_bool_t  dbus_fake_condvar_wait_timeout (DBusCondVar *cond,
                                                    DBusMutex   *mutex,
                                                    int          timeout_msec);
static void         dbus_fake_condvar_wake_one     (DBusCondVar *cond);
static void         dbus_fake_condvar_wake_all     (DBusCondVar *cond);


static const DBusThreadFunctions fake_functions =
{
  DBUS_THREAD_FUNCTIONS_MUTEX_NEW_MASK |
  DBUS_THREAD_FUNCTIONS_MUTEX_FREE_MASK |
  DBUS_THREAD_FUNCTIONS_MUTEX_LOCK_MASK |
  DBUS_THREAD_FUNCTIONS_MUTEX_UNLOCK_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_NEW_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_FREE_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_WAIT_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_WAIT_TIMEOUT_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_WAKE_ONE_MASK|
  DBUS_THREAD_FUNCTIONS_CONDVAR_WAKE_ALL_MASK,
  dbus_fake_mutex_new,
  dbus_fake_mutex_free,
  dbus_fake_mutex_lock,
  dbus_fake_mutex_unlock,
  dbus_fake_condvar_new,
  dbus_fake_condvar_free,
  dbus_fake_condvar_wait,
  dbus_fake_condvar_wait_timeout,
  dbus_fake_condvar_wake_one,
  dbus_fake_condvar_wake_all
};

static DBusMutex *
dbus_fake_mutex_new (void)
{
  DBusFakeMutex *mutex;

  mutex = dbus_new0 (DBusFakeMutex, 1);

  return (DBusMutex *)mutex;
}

static void
dbus_fake_mutex_free (DBusMutex *mutex)
{
  DBusFakeMutex *fake = (DBusFakeMutex*) mutex;

  _dbus_assert (!fake->locked);
  
  dbus_free (fake);
}

static dbus_bool_t
dbus_fake_mutex_lock (DBusMutex *mutex)
{
  DBusFakeMutex *fake = (DBusFakeMutex*) mutex;

  _dbus_assert (!fake->locked);

  fake->locked = TRUE;
  
  return TRUE;
}

static dbus_bool_t
dbus_fake_mutex_unlock (DBusMutex *mutex)
{
  DBusFakeMutex *fake = (DBusFakeMutex*) mutex;

  _dbus_assert (fake->locked);

  fake->locked = FALSE;
  
  return TRUE;
}

static DBusCondVar*
dbus_fake_condvar_new (void)
{
  return (DBusCondVar*) _dbus_strdup ("FakeCondvar");
}

static void
dbus_fake_condvar_free (DBusCondVar *cond)
{
  dbus_free (cond);
}

static void
dbus_fake_condvar_wait (DBusCondVar *cond,
                        DBusMutex   *mutex)
{
  
}

static dbus_bool_t
dbus_fake_condvar_wait_timeout (DBusCondVar *cond,
                                DBusMutex   *mutex,
                                int         timeout_msec)
{
  return TRUE;
}

static void
dbus_fake_condvar_wake_one (DBusCondVar *cond)
{

}

static void
dbus_fake_condvar_wake_all (DBusCondVar *cond)
{

}

dbus_bool_t
_dbus_threads_init_debug (void)
{
#ifdef DBUS_WIN
  return _dbus_threads_init_platform_specific();
#else
  return dbus_threads_init (&fake_functions);
#endif
}

#endif /* DBUS_BUILD_TESTS */
