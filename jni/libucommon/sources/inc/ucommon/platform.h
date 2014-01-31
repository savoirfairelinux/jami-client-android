// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

/**
 * Various miscellaneous platform specific headers and defines.
 * This is used to support ucommon on different platforms.  The ucommon
 * library assumes at least a real posix threading library is present or
 * will build thread support native on Microsoft Windows legacy platform.
 * This header also deals with issues related to common base types.
 * @file ucommon/platform.h
 */

#ifndef _UCOMMON_PLATFORM_H_
#define _UCOMMON_PLATFORM_H_
#define UCOMMON_ABI 6

/**
 * Common namespace for all ucommon objects.
 * We are using a common namespace to easily separate ucommon from other
 * libraries.  This namespace usage is set to the package name and controlled
 * by macros so future changes will be hidden from user applications so long
 * as the namespace macros (UCOMMON_NAMESPACE, NAMESPACE_UCOMMON,
 * END_NAMESPACE) are used in place of direct namespace declarations.
 * @namespace ucommon
 */

#define UCOMMON_NAMESPACE   ucommon
#define NAMESPACE_UCOMMON   namespace ucommon {
#define NAMESPACE_EXTERN_C  extern "C" {
#define END_NAMESPACE       }
#define EXTERN_C            extern "C"

#ifndef _REENTRANT
#define _REENTRANT 1
#endif

#ifndef __PTH__
#ifndef _THREADSAFE
#define _THREADSAFE 1
#endif

#ifndef _POSIX_PTHREAD_SEMANTICS
#define _POSIX_PTHREAD_SEMANTICS
#endif
#endif

#if defined(__GNUC__) && (__GNUC < 3) && !defined(_GNU_SOURCE)
#define _GNU_SOURCE
#endif

#if __GNUC__ > 3 || (__GNUC__ == 3 && (__GNU_MINOR__ > 3))
#define __PRINTF(x,y)   __attribute__ ((format (printf, x, y)))
#define __SCANF(x, y) __attribute__ ((format (scanf, x, y)))
#define __MALLOC      __attribute__ ((malloc))
#endif

#ifndef __MALLOC
#define __PRINTF(x, y)
#define __SCANF(x, y)
#define __MALLOC
#endif

#ifndef DEBUG
#ifndef NDEBUG
#define NDEBUG
#endif
#endif

#ifdef  DEBUG
#ifdef  NDEBUG
#undef  NDEBUG
#endif
#endif

// see if we are building for or using extended stdc++ runtime library support

#if defined(NEW_STDCPP) || defined(OLD_STDCPP)
#define _UCOMMON_EXTENDED_
#endif

// see if targeting legacy Microsoft windows platform

#if defined(_MSC_VER) || defined(WIN32) || defined(_WIN32)
#define _MSWINDOWS_

#if defined(_M_X64) || defined(_M_ARM)
#define _MSCONDITIONALS_
#ifndef _WIN32_WINNT
#define _WIN32_WINNT    0x0600
#endif
#endif

//#if defined(_WIN32_WINNT) && _WIN32_WINNT < 0x0501
//#undef    _WIN32_WINNT
//#define   _WIN32_WINNT 0x0501
//#endif

//#ifndef _WIN32_WINNT
//#define   _WIN32_WINNT 0x0501
//#endif

#ifdef  _MSC_VER
#pragma warning(disable: 4251)
#pragma warning(disable: 4996)
#pragma warning(disable: 4355)
#pragma warning(disable: 4290)
#pragma warning(disable: 4291)
#endif

#if defined(__BORLANDC__) && !defined(__MT__)
#error Please enable multithreading
#endif

#if defined(_MSC_VER) && !defined(_MT)
#error Please enable multithreading (Project -> Settings -> C/C++ -> Code Generation -> Use Runtime Library)
#endif

// Require for compiling with critical sections.
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0501
#endif

// Make sure we're consistent with _WIN32_WINNT
#ifndef WINVER
#define WINVER _WIN32_WINNT
#endif

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif

#include <winsock2.h>
#include <ws2tcpip.h>

#if defined(_MSC_VER)
typedef signed long ssize_t;
typedef int pid_t;
#endif

#include <process.h>
#ifndef __EXPORT
#ifdef  UCOMMON_STATIC
#define __EXPORT
#else
#define __EXPORT    __declspec(dllimport)
#endif
#endif
#define __LOCAL

// if runtime mode then non-runtime libraries are static on windows...
#if defined(UCOMMON_RUNTIME) || defined(UCOMMON_STATIC)
#define __SHARED
#else
#define __SHARED __EXPORT
#endif

#elif UCOMMON_VISIBILITY > 0
#define __EXPORT    __attribute__ ((visibility("default")))
#define __LOCAL     __attribute__ ((visibility("hidden")))
#define __SHARED    __attribute__ ((visibility("default")))
#else
#define __EXPORT
#define __LOCAL
#define __SHARED
#endif

#ifdef  _MSWINDOWS_

#define _UWIN

#include <sys/stat.h>
#include <io.h>

typedef DWORD pthread_t;
typedef CRITICAL_SECTION pthread_mutex_t;
typedef char *caddr_t;
typedef HANDLE fd_t;
typedef SOCKET socket_t;

#ifdef  _MSC_VER
typedef struct timespec {
    time_t tv_sec;
    long  tv_nsec;
} timespec_t;
#endif

extern "C" {

    #define SERVICE_MAIN(id, argc, argv) void WINAPI service_##id(DWORD argc, LPSTR *argv)

    typedef LPSERVICE_MAIN_FUNCTION cpr_service_t;

    inline void sleep(int seconds)
        {::Sleep((seconds * 1000l));}

    inline void pthread_exit(void *p)
        {_endthreadex((DWORD)0);}

    inline pthread_t pthread_self(void)
        {return (pthread_t)GetCurrentThreadId();}

    inline int pthread_mutex_init(pthread_mutex_t *mutex, void *x)
        {InitializeCriticalSection(mutex); return 0;}

    inline void pthread_mutex_destroy(pthread_mutex_t *mutex)
        {DeleteCriticalSection(mutex);}

    inline void pthread_mutex_lock(pthread_mutex_t *mutex)
        {EnterCriticalSection(mutex);}

    inline void pthread_mutex_unlock(pthread_mutex_t *mutex)
        {LeaveCriticalSection(mutex);}

    inline char *strdup(const char *s)
        {return _strdup(s);}

    inline int stricmp(const char *s1, const char *s2)
        {return _stricmp(s1, s2);}

    inline int strnicmp(const char *s1, const char *s2, size_t l)
        {return _strnicmp(s1, s2, l);}
}

#elif defined(__PTH__)

#include <pth.h>
#include <sys/wait.h>

typedef int socket_t;
typedef int fd_t;
#define INVALID_SOCKET -1
#define INVALID_HANDLE_VALUE -1
#include <signal.h>

#define pthread_mutex_t pth_mutex_t
#define pthread_cond_t pth_cond_t
#define pthread_t pth_t

inline int pthread_sigmask(int how, const sigset_t *set, sigset_t *oset)
    {return pth_sigmask(how, set, oset);};

inline void pthread_exit(void *p)
    {pth_exit(p);};

inline void pthread_kill(pthread_t tid, int sig)
    {pth_raise(tid, sig);};

inline int pthread_mutex_init(pthread_mutex_t *mutex, void *x)
    {return pth_mutex_init(mutex) != 0;};

inline void pthread_mutex_destroy(pthread_mutex_t *mutex)
    {};

inline void pthread_mutex_lock(pthread_mutex_t *mutex)
    {pth_mutex_acquire(mutex, 0, NULL);};

inline void pthread_mutex_unlock(pthread_mutex_t *mutex)
    {pth_mutex_release(mutex);};

inline void pthread_cond_wait(pthread_cond_t *cond, pthread_mutex_t *mutex)
    {pth_cond_await(cond, mutex, NULL);};

inline void pthread_cond_signal(pthread_cond_t *cond)
    {pth_cond_notify(cond, FALSE);};

inline void pthread_cond_broadcast(pthread_cond_t *cond)
    {pth_cond_notify(cond, TRUE);};

#else

#include <pthread.h>

typedef int socket_t;
typedef int fd_t;
#define INVALID_SOCKET -1
#define INVALID_HANDLE_VALUE -1
#include <signal.h>

#endif

#ifdef _MSC_VER
typedef signed __int8 int8_t;
typedef unsigned __int8 uint8_t;
typedef signed __int16 int16_t;
typedef unsigned __int16 uint16_t;
typedef signed __int32 int32_t;
typedef unsigned __int32 uint32_t;
typedef signed __int64 int64_t;
typedef unsigned __int64 uint64_t;
typedef char *caddr_t;

#include <stdio.h>
#define snprintf _snprintf
#define vsnprintf _vsnprintf

#else

#include <sys/stat.h>
#include <sys/types.h>
#include <stdint.h>
#include <unistd.h>
#include <stdio.h>

#endif

#undef  getchar
#undef  putchar

#ifndef _GNU_SOURCE
typedef void (*sighandler_t)(int);  /**< Convenient typedef for signal handlers. */
#endif
typedef unsigned long timeout_t;    /**< Typedef for millisecond timer values. */

#include <stdlib.h>
#include <ctype.h>
#include <limits.h>
#include <errno.h>

#ifdef  _MSWINDOWS_
#ifndef ENETDOWN
#define ENETDOWN        ((int)(WSAENETDOWN))
#endif
#ifndef EINPROGRESS
#define EINPROGRESS     ((int)(WSAEINPROGRESS))
#endif
#ifndef ENOPROTOOPT
#define ENOPROTOOPT     ((int)(WSAENOPROTOOPT))
#endif
#ifndef EADDRINUSE
#define EADDRINUSE      ((int)(WSAEADDRINUSE))
#endif
#ifndef EADDRNOTAVAIL
#define EADDRNOTAVAIL   ((int)(WSAEADDRNOTAVAIL))
#endif
#ifndef ENETUNREACH
#define ENETUNREACH     ((int)(WSAENETUNREACH))
#endif
#ifndef EHOSTUNREACH
#define EHOSTUNREACH    ((int)(WSAEHOSTUNREACH))
#endif
#ifndef EHOSTDOWN
#define EHOSTDOWN       ((int)(WSAEHOSTDOWN))
#endif
#ifndef ENETRESET
#define ENETRESET       ((int)(WSAENETRESET))
#endif
#ifndef ECONNABORTED
#define ECONNABORTED    ((int)(WSAECONNABORTED))
#endif
#ifndef ECONNRESET
#define ECONNRESET      ((int)(WSAECONNRESET))
#endif
#ifndef EISCONN
#define EISCONN         ((int)(WSAEISCONN))
#endif
#ifndef ENOTCONN
#define ENOTCONN        ((int)(WSAENOTCONN))
#endif
#ifndef ESHUTDOWN
#define ESHUTDOWN       ((int)(WSAESHUTDOWN))
#endif
#ifndef ETIMEDOUT
#define ETIMEDOUT       ((int)(WSAETIMEDOUT))
#endif
#ifndef ECONNREFUSED
#define ECONNREFUSED    ((int)(WSAECONNREFUSED))
#endif
#endif

#ifndef DEBUG
#ifndef NDEBUG
#define NDEBUG
#endif
#endif

#ifdef  DEBUG
#ifdef  NDEBUG
#undef  NDEBUG
#endif
#endif

#ifndef PROGRAM_MAIN
#define PROGRAM_MAIN(argc, argv)    extern "C" int main(int argc, char **argv)
#define PROGRAM_EXIT(code)          return code
#endif

#ifndef SERVICE_MAIN
#define SERVICE_MAIN(id, argc, argv)    void service_##id(int argc, char **argv)
typedef void (*cpr_service_t)(int argc, char **argv);
#endif

#include <assert.h>
#ifdef  DEBUG
#define crit(x, text)   assert(x)
#else
#define crit(x, text) if(!(x)) cpr_runtime_error(text)
#endif

/**
 * Template function to initialize memory by invoking default constructor.
 * If NULL is passed, then NULL is returned without any constructor called.
 * @param memory to initialize.
 * @return memory initialized.
 */
template<class T>
inline T *init(T *memory)
    {return ((memory) ? new(((caddr_t)memory)) T : NULL);}

typedef long Integer;
typedef unsigned long Unsigned;
typedef double Real;

/**
 * Matching function for strdup().
 * @param string to release from allocated memory.
 */
inline void strfree(char *str)
    {::free(str);}

#endif
