/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-sysdeps-pthread.c Implements threads using pthreads (internal to libdbus)
 * 
 * Copyright (C) 2002, 2003, 2006  Red Hat, Inc.
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
#include "dbus-sysdeps.h"
#include "dbus-threads.h"

#include <sys/time.h>
#include <pthread.h>
#include <string.h>

#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif

#include <config.h>

/* Whether we have a "monotonic" clock; i.e. a clock not affected by
 * changes in system time.
 * This is initialized once in check_monotonic_clock below.
 * https://bugs.freedesktop.org/show_bug.cgi?id=18121
 */
static dbus_bool_t have_monotonic_clock = 0;

typedef struct {
  pthread_mutex_t lock; /**< lock protecting count field */
  volatile int count;   /**< count of how many times lock holder has recursively locked */
  volatile pthread_t holder; /**< holder of the lock if count >0,
                                valid but arbitrary thread if count
                                has ever been >0, uninitialized memory
                                if count has never been >0 */
} DBusMutexPThread;

typedef struct {
  pthread_cond_t cond; /**< the condition */
} DBusCondVarPThread;

#define DBUS_MUTEX(m)         ((DBusMutex*) m)
#define DBUS_MUTEX_PTHREAD(m) ((DBusMutexPThread*) m)

#define DBUS_COND_VAR(c)         ((DBusCondVar*) c)
#define DBUS_COND_VAR_PTHREAD(c) ((DBusCondVarPThread*) c)


#ifdef DBUS_DISABLE_ASSERT
/* (tmp != 0) is a no-op usage to silence compiler */
#define PTHREAD_CHECK(func_name, result_or_call)    \
    do { int tmp = (result_or_call); if (tmp != 0) {;} } while (0)
#else
#define PTHREAD_CHECK(func_name, result_or_call) do {                                  \
    int tmp = (result_or_call);                                                        \
    if (tmp != 0) {                                                                    \
      _dbus_warn_check_failed ("pthread function %s failed with %d %s in %s\n",        \
                               func_name, tmp, strerror(tmp), _DBUS_FUNCTION_NAME);    \
    }                                                                                  \
} while (0)
#endif /* !DBUS_DISABLE_ASSERT */

static DBusMutex*
_dbus_pthread_mutex_new (void)
{
  DBusMutexPThread *pmutex;
  int result;
  
  pmutex = dbus_new (DBusMutexPThread, 1);
  if (pmutex == NULL)
    return NULL;

  result = pthread_mutex_init (&pmutex->lock, NULL);

  if (result == ENOMEM || result == EAGAIN)
    {
      dbus_free (pmutex);
      return NULL;
    }
  else
    {
      PTHREAD_CHECK ("pthread_mutex_init", result);
    }

  /* Only written */
  pmutex->count = 0;

  /* There's no portable way to have a "null" pthread afaik so we
   * can't set pmutex->holder to anything sensible.  We only access it
   * once the lock is held (which means we've set it).
   */
  
  return DBUS_MUTEX (pmutex);
}

static void
_dbus_pthread_mutex_free (DBusMutex *mutex)
{
  DBusMutexPThread *pmutex = DBUS_MUTEX_PTHREAD (mutex);

  _dbus_assert (pmutex->count == 0);
  
  PTHREAD_CHECK ("pthread_mutex_destroy", pthread_mutex_destroy (&pmutex->lock));

  dbus_free (pmutex);
}

static void
_dbus_pthread_mutex_lock (DBusMutex *mutex)
{
  DBusMutexPThread *pmutex = DBUS_MUTEX_PTHREAD (mutex);
  pthread_t self = pthread_self ();

  /* If the count is > 0 then someone had the lock, maybe us. If it is
   * 0, then it might immediately change right after we read it,
   * but it will be changed by another thread; i.e. if we read 0,
   * we assume that this thread doesn't have the lock.
   *
   * Not 100% sure this is safe, but ... seems like it should be.
   */
  if (pmutex->count == 0)
    {
      /* We know we don't have the lock; someone may have the lock. */
      
      PTHREAD_CHECK ("pthread_mutex_lock", pthread_mutex_lock (&pmutex->lock));

      /* We now have the lock. Count must be 0 since it must be 0 when
       * the lock is released by another thread, and we just now got
       * the lock.
       */
      _dbus_assert (pmutex->count == 0);
      
      pmutex->holder = self;
      pmutex->count = 1;
    }
  else
    {
      /* We know someone had the lock, possibly us. Thus
       * pmutex->holder is not pointing to junk, though it may not be
       * the lock holder anymore if the lock holder is not us.  If the
       * lock holder is us, then we definitely have the lock.
       */

      if (pthread_equal (pmutex->holder, self))
        {
          /* We already have the lock. */
          _dbus_assert (pmutex->count > 0);
        }
      else
        {
          /* Wait for the lock */
          PTHREAD_CHECK ("pthread_mutex_lock", pthread_mutex_lock (&pmutex->lock));
	  pmutex->holder = self;
          _dbus_assert (pmutex->count == 0);
        }

      pmutex->count += 1;
    }
}

static void
_dbus_pthread_mutex_unlock (DBusMutex *mutex)
{
  DBusMutexPThread *pmutex = DBUS_MUTEX_PTHREAD (mutex);

  _dbus_assert (pmutex->count > 0);
  
  pmutex->count -= 1;

  if (pmutex->count == 0)
    PTHREAD_CHECK ("pthread_mutex_unlock", pthread_mutex_unlock (&pmutex->lock));
  
  /* We leave pmutex->holder set to ourselves, its content is undefined if count is 0 */
}

static DBusCondVar *
_dbus_pthread_condvar_new (void)
{
  DBusCondVarPThread *pcond;
  pthread_condattr_t attr;
  int result;
  
  pcond = dbus_new (DBusCondVarPThread, 1);
  if (pcond == NULL)
    return NULL;

  pthread_condattr_init (&attr);
#ifdef HAVE_MONOTONIC_CLOCK
  if (have_monotonic_clock)
    pthread_condattr_setclock (&attr, CLOCK_MONOTONIC);
#endif

  result = pthread_cond_init (&pcond->cond, &attr);
  pthread_condattr_destroy (&attr);

  if (result == EAGAIN || result == ENOMEM)
    {
      dbus_free (pcond);
      return NULL;
    }
  else
    {
      PTHREAD_CHECK ("pthread_cond_init", result);
    }
  
  return DBUS_COND_VAR (pcond);
}

static void
_dbus_pthread_condvar_free (DBusCondVar *cond)
{  
  DBusCondVarPThread *pcond = DBUS_COND_VAR_PTHREAD (cond);
  
  PTHREAD_CHECK ("pthread_cond_destroy", pthread_cond_destroy (&pcond->cond));

  dbus_free (pcond);
}

static void
_dbus_pthread_condvar_wait (DBusCondVar *cond,
                            DBusMutex   *mutex)
{
  DBusMutexPThread *pmutex = DBUS_MUTEX_PTHREAD (mutex);
  DBusCondVarPThread *pcond = DBUS_COND_VAR_PTHREAD (cond);
  int old_count;
  
  _dbus_assert (pmutex->count > 0);
  _dbus_assert (pthread_equal (pmutex->holder, pthread_self ()));

  old_count = pmutex->count;
  pmutex->count = 0;		/* allow other threads to lock */
  PTHREAD_CHECK ("pthread_cond_wait", pthread_cond_wait (&pcond->cond, &pmutex->lock));
  _dbus_assert (pmutex->count == 0);
  pmutex->holder = pthread_self(); /* other threads may have locked the mutex in the meantime */

  /* The order of this line and the above line is important.
   * See the comments below at the end of _dbus_pthread_condvar_wait_timeout
   */
  pmutex->count = old_count;
}

static dbus_bool_t
_dbus_pthread_condvar_wait_timeout (DBusCondVar               *cond,
                                    DBusMutex                 *mutex,
                                    int                        timeout_milliseconds)
{
  DBusMutexPThread *pmutex = DBUS_MUTEX_PTHREAD (mutex);
  DBusCondVarPThread *pcond = DBUS_COND_VAR_PTHREAD (cond);
  struct timeval time_now;
  struct timespec end_time;
  int result;
  int old_count;
  
  _dbus_assert (pmutex->count > 0);
  _dbus_assert (pthread_equal (pmutex->holder, pthread_self ()));  

#ifdef HAVE_MONOTONIC_CLOCK
  if (have_monotonic_clock)
    {
      struct timespec monotonic_timer;
      clock_gettime (CLOCK_MONOTONIC,&monotonic_timer);
      time_now.tv_sec = monotonic_timer.tv_sec;
      time_now.tv_usec = monotonic_timer.tv_nsec / 1000;
    }
  else
    /* This else falls through to gettimeofday */
#endif
  gettimeofday (&time_now, NULL);
  
  end_time.tv_sec = time_now.tv_sec + timeout_milliseconds / 1000;
  end_time.tv_nsec = (time_now.tv_usec + (timeout_milliseconds % 1000) * 1000) * 1000;
  if (end_time.tv_nsec > 1000*1000*1000)
    {
      end_time.tv_sec += 1;
      end_time.tv_nsec -= 1000*1000*1000;
    }

  old_count = pmutex->count;
  pmutex->count = 0;
  result = pthread_cond_timedwait (&pcond->cond, &pmutex->lock, &end_time);
  
  if (result != ETIMEDOUT)
    {
      PTHREAD_CHECK ("pthread_cond_timedwait", result);
    }

  _dbus_assert (pmutex->count == 0);
  pmutex->holder = pthread_self(); /* other threads may have locked the mutex in the meantime */

  /* restore to old count after setting the owner back to self,
   * If reversing this line with above line, the previous owner thread could
   * get into the mutex without proper locking by passing the lock owner check.
   */
  pmutex->count = old_count;
  
  /* return true if we did not time out */
  return result != ETIMEDOUT;
}

static void
_dbus_pthread_condvar_wake_one (DBusCondVar *cond)
{
  DBusCondVarPThread *pcond = DBUS_COND_VAR_PTHREAD (cond);

  PTHREAD_CHECK ("pthread_cond_signal", pthread_cond_signal (&pcond->cond));
}

static void
_dbus_pthread_condvar_wake_all (DBusCondVar *cond)
{
  DBusCondVarPThread *pcond = DBUS_COND_VAR_PTHREAD (cond);
  
  PTHREAD_CHECK ("pthread_cond_broadcast", pthread_cond_broadcast (&pcond->cond));
}

static const DBusThreadFunctions pthread_functions =
{
  DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_NEW_MASK |
  DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_FREE_MASK |
  DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_LOCK_MASK |
  DBUS_THREAD_FUNCTIONS_RECURSIVE_MUTEX_UNLOCK_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_NEW_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_FREE_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_WAIT_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_WAIT_TIMEOUT_MASK |
  DBUS_THREAD_FUNCTIONS_CONDVAR_WAKE_ONE_MASK|
  DBUS_THREAD_FUNCTIONS_CONDVAR_WAKE_ALL_MASK,
  NULL, NULL, NULL, NULL,
  _dbus_pthread_condvar_new,
  _dbus_pthread_condvar_free,
  _dbus_pthread_condvar_wait,
  _dbus_pthread_condvar_wait_timeout,
  _dbus_pthread_condvar_wake_one,
  _dbus_pthread_condvar_wake_all,
  _dbus_pthread_mutex_new,
  _dbus_pthread_mutex_free,
  _dbus_pthread_mutex_lock,
  _dbus_pthread_mutex_unlock
};

static void
check_monotonic_clock (void)
{
#ifdef HAVE_MONOTONIC_CLOCK
  struct timespec dummy;
  if (clock_getres (CLOCK_MONOTONIC, &dummy) == 0)
    have_monotonic_clock = TRUE;
#endif
}

dbus_bool_t
_dbus_threads_init_platform_specific (void)
{
  check_monotonic_clock ();
  return dbus_threads_init (&pthread_functions);
}
