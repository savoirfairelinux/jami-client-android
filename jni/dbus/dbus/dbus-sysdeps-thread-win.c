/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-sysdeps-pthread.c Implements threads using Windows threads (internal to libdbus)
 * 
 * Copyright (C) 2006  Red Hat, Inc.
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
#include "dbus-sysdeps-win.h"
#include "dbus-threads.h"
#include "dbus-list.h"

#include <windows.h>

struct DBusCondVar {
  DBusList *list;        /**< list thread-local-stored events waiting on the cond variable */
  CRITICAL_SECTION lock; /**< lock protecting the list */
};

static DWORD dbus_cond_event_tls = TLS_OUT_OF_INDEXES;


static HMODULE dbus_dll_hmodule;

void *
_dbus_win_get_dll_hmodule (void)
{
  return dbus_dll_hmodule;
}

#ifdef DBUS_WINCE
#define hinst_t HANDLE
#else
#define hinst_t HINSTANCE
#endif

BOOL WINAPI DllMain (hinst_t, DWORD, LPVOID);

/* We need this to free the TLS events on thread exit */
BOOL WINAPI
DllMain (hinst_t hinstDLL,
	 DWORD     fdwReason,
	 LPVOID    lpvReserved)
{
  HANDLE event;
  switch (fdwReason) 
    { 
    case DLL_PROCESS_ATTACH:
      dbus_dll_hmodule = hinstDLL;
      break;
    case DLL_THREAD_DETACH:
      if (dbus_cond_event_tls != TLS_OUT_OF_INDEXES)
	{
	  event = TlsGetValue(dbus_cond_event_tls);
	  CloseHandle (event);
	  TlsSetValue(dbus_cond_event_tls, NULL);
	}
      break;
    case DLL_PROCESS_DETACH: 
      if (dbus_cond_event_tls != TLS_OUT_OF_INDEXES)
	{
	  event = TlsGetValue(dbus_cond_event_tls);
	  CloseHandle (event);
	  TlsSetValue(dbus_cond_event_tls, NULL);

	  TlsFree(dbus_cond_event_tls); 
	}
      break;
    default: 
      break; 
    }
  return TRUE;
}

static DBusMutex*
_dbus_windows_mutex_new (void)
{
  HANDLE handle;
  handle = CreateMutex (NULL, FALSE, NULL);
  return (DBusMutex *) handle;
}

static void
_dbus_windows_mutex_free (DBusMutex *mutex)
{
  CloseHandle ((HANDLE *) mutex);
}

static dbus_bool_t
_dbus_windows_mutex_lock (DBusMutex *mutex)
{
  return WaitForSingleObject ((HANDLE *) mutex, INFINITE) != WAIT_FAILED;
}

static dbus_bool_t
_dbus_windows_mutex_unlock (DBusMutex *mutex)
{
  return ReleaseMutex ((HANDLE *) mutex) != 0;
}

static DBusCondVar *
_dbus_windows_condvar_new (void)
{
  DBusCondVar *cond;
    
  cond = dbus_new (DBusCondVar, 1);
  if (cond == NULL)
    return NULL;
  
  cond->list = NULL;
  
  InitializeCriticalSection (&cond->lock);
  return (DBusCondVar *) cond;
}

static void
_dbus_windows_condvar_free (DBusCondVar *cond)
{
  DeleteCriticalSection (&cond->lock);
  _dbus_list_clear (&cond->list);
  dbus_free (cond);
}

static dbus_bool_t
_dbus_condvar_wait_win32 (DBusCondVar *cond,
			  DBusMutex *mutex,
			  int milliseconds)
{
  DWORD retval;
  dbus_bool_t ret;
  HANDLE event = TlsGetValue (dbus_cond_event_tls);

  if (!event)
    {
      event = CreateEvent (0, FALSE, FALSE, NULL);
      if (event == 0)
	return FALSE;
      TlsSetValue (dbus_cond_event_tls, event);
    }

  EnterCriticalSection (&cond->lock);

  /* The event must not be signaled. Check this */
  _dbus_assert (WaitForSingleObject (event, 0) == WAIT_TIMEOUT);

  ret = _dbus_list_append (&cond->list, event);
  
  LeaveCriticalSection (&cond->lock);
  
  if (!ret)
    return FALSE; /* Prepend failed */

  _dbus_mutex_unlock (mutex);
  retval = WaitForSingleObject (event, milliseconds);
  _dbus_mutex_lock (mutex);
  
  if (retval == WAIT_TIMEOUT)
    {
      EnterCriticalSection (&cond->lock);
      _dbus_list_remove (&cond->list, event);

      /* In the meantime we could have been signaled, so we must again
       * wait for the signal, this time with no timeout, to reset
       * it. retval is set again to honour the late arrival of the
       * signal */
      retval = WaitForSingleObject (event, 0);

      LeaveCriticalSection (&cond->lock);
    }

#ifndef DBUS_DISABLE_ASSERT
  EnterCriticalSection (&cond->lock);

  /* Now event must not be inside the array, check this */
  _dbus_assert (_dbus_list_remove (&cond->list, event) == FALSE);

  LeaveCriticalSection (&cond->lock);
#endif /* !G_DISABLE_ASSERT */

  return retval != WAIT_TIMEOUT;
}

static void
_dbus_windows_condvar_wait (DBusCondVar *cond,
                            DBusMutex   *mutex)
{
  _dbus_condvar_wait_win32 (cond, mutex, INFINITE);
}

static dbus_bool_t
_dbus_windows_condvar_wait_timeout (DBusCondVar               *cond,
				     DBusMutex                 *mutex,
				     int                        timeout_milliseconds)
{
  return _dbus_condvar_wait_win32 (cond, mutex, timeout_milliseconds);
}

static void
_dbus_windows_condvar_wake_one (DBusCondVar *cond)
{
  EnterCriticalSection (&cond->lock);
  
  if (cond->list != NULL)
    SetEvent (_dbus_list_pop_first (&cond->list));
    
  LeaveCriticalSection (&cond->lock);
}

static void
_dbus_windows_condvar_wake_all (DBusCondVar *cond)
{
  EnterCriticalSection (&cond->lock);

  while (cond->list != NULL)
    SetEvent (_dbus_list_pop_first (&cond->list));
  
  LeaveCriticalSection (&cond->lock);
}

static const DBusThreadFunctions windows_functions =
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
  _dbus_windows_mutex_new,
  _dbus_windows_mutex_free,
  _dbus_windows_mutex_lock,
  _dbus_windows_mutex_unlock,
  _dbus_windows_condvar_new,
  _dbus_windows_condvar_free,
  _dbus_windows_condvar_wait,
  _dbus_windows_condvar_wait_timeout,
  _dbus_windows_condvar_wake_one,
  _dbus_windows_condvar_wake_all
};

dbus_bool_t
_dbus_threads_init_platform_specific (void)
{
  /* We reuse this over several generations, because we can't
   * free the events once they are in use
   */
  if (dbus_cond_event_tls == TLS_OUT_OF_INDEXES)
    {
      dbus_cond_event_tls = TlsAlloc ();
      if (dbus_cond_event_tls == TLS_OUT_OF_INDEXES)
	return FALSE;
    }

  return dbus_threads_init (&windows_functions);
}

