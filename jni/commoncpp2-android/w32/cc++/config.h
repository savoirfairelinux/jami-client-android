// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception to the GNU General Public License, permission is
// granted for additional uses of the text contained in its release
// of Common C++.
//
// The exception is that, if you link the Common C++ library with other
// files to produce an executable, this does not by itself cause the
// resulting executable to be covered by the GNU General Public License.
// Your use of that executable is in no way restricted on account of
// linking the Common C++ library code into it.
//
// This exception does not however invalidate any other reasons why
// the executable file might be covered by the GNU General Public License.
//
// This exception applies only to the code released under the
// name Common C++.  If you copy code from other releases into a copy of
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.

#ifndef CCXX_CONFIG_H_
#define CCXX_CONFIG_H_
#define CCXX_PACKED
#define CCXX_PACKING

// Config option: uncomment this line if you want to use static linkage!
//#define CCXX_STATIC

// define automatically WIN32 for windows application compiled with Borland
#ifndef WIN32
# if defined(__BORLANDC__) && defined(_Windows)
#  define WIN32
# elif defined(_MSC_VER) && defined(_WIN32)
#  define WIN32
# endif
#endif

#pragma warning(disable: 4996)
#pragma warning(disable: 4355)

// check multithreading
#if defined(__BORLANDC__) && !defined(__MT__)
#  error Please enable multithreading
#endif
#if defined(_MSC_VER) && !defined(_MT)
#  error Please enable multithreading (Project -> Settings -> C/C++ -> Code Generation -> Use Runtime Library)
#endif

// check DLL compiling
#ifdef _MSC_VER
#ifndef CCXX_STATIC
# ifndef _DLL
#  error Please enable DLL linking (Project -> Settings -> C/C++ -> Code Generation -> Use Runtime Library)
# endif
#endif
#endif

#ifndef CCXX_WIN32
#define CCXX_WIN32
/**
* @todo Why may be need using kernel object Mutex as a background for ost::Mutex?
*/

// Select the way, that the ost::Mutex based on.
//#define MUTEX_UNDERGROUND_WIN32_MUTEX
#define MUTEX_UNDERGROUND_WIN32_CRITICALSECTION

/*
http://msdn.microsoft.com/library/en-us/winprog/winprog/using_the_windows_headers.asp

Minimum system required    Macros to define

Windows "Longhorn"         _WIN32_WINNT >= 0x0600
                           WINVER >= 0x0600

Windows Server 2003        _WIN32_WINNT> = 0x0502
                           WINVER >= 0x0502

Windows XP                 _WIN32_WINNT >= 0x0501
                           WINVER >= 0x0501

Windows 2000               _WIN32_WINNT >= 0x0500
                           WINVER >= 0x0500

Windows NT 4.0             _WIN32_WINNT >= 0x0400
                           WINVER >= 0x0400

Windows Me                 _WIN32_WINDOWS >= 0x0500
                           WINVER >= 0x0500

Windows 98                 _WIN32_WINDOWS >= 0x0410
                           WINVER >= 0x0410

Windows 95                 _WIN32_WINDOWS >= 0x0400
                           WINVER >= 0x0400

Faster Builds with Smaller Header Files

WIN32_LEAN_AND_MEAN
*/

// Require for compiling with critical sections.
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0400
#endif

// Make sure we're consistent with _WIN32_WINNT
#ifndef WINVER
#define WINVER _WIN32_WINNT
#endif

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#define _CCXX_WIN32_LEAN_AND_MEAN_
#endif

#include <winsock2.h>
#include <ws2tcpip.h>

#ifdef _CCXX_WIN32_LEAN_AND_MEAN_
#undef WIN32_LEAN_AND_MEAN
#undef _CCXX_WIN32_LEAN_AND_MEAN_
#endif

#if _WIN32_WINNT >= 0x0501
#define CCXX_IPV6
#endif

#endif /* #ifndef CCXX_WIN32 */

#ifdef WIN32
#ifndef ssize_t
#define ssize_t int
#endif
#endif

#undef  __DLLRTL
#undef CCXX_EMPTY
#define CCXX_EMPTY


#if defined(__MINGW32__) && !defined(__MSVCRT__)
#define CCXX_NOMSVCRT
#endif

#if defined(__MINGW32__) || defined(__CYGWIN32__)

#define HAVE_OLD_IOSTREAM
#define HAVE_LIBXML

#undef __LOCAL
#undef __EXPORT
#undef __stdcall
#define __stdcall
#define __EXPORT
#define __LOCAL
typedef char int8;
typedef short int16;
typedef long int32;
typedef long long int64;
typedef unsigned char uint8;
typedef unsigned short uint16;
typedef unsigned long uint32;
typedef unsigned long long uint64;
#ifdef __MINGW32__
# define HAVE_MODULES   1
# define alloca(x)      __builtin_alloca(x)
# define THROW(x)       throw x
# define THROWS(x)      throw(x)
# define NEW_THROWS throw()
# define THROWS_EMPTY   throw()
  typedef unsigned int  uint;
# define        snprintf            _snprintf
# ifndef ETC_PREFIX
#   define ETC_PREFIX   "c:/"
# endif
#else /* #ifndef __MINGW32__ */
typedef DWORD size_t;
#endif /* #ifndef __MINGW32__ */

#else /* !defined(__MINGW32__) && !defined(__CYGWIN32__) */

#ifdef CCXX_STATIC
#define __DLLRTL
#define __EXPORT
#define __LOCAL
#define __EXPORT_TEMPLATE(x)
#else
#define __DLLRTL  __declspec(dllexport)
#define __EXPORT  __declspec(dllimport)
#define __EXPORT_TEMPLATE(x)    template class __EXPORT x;
#define __LOCAL
#endif

#if !defined(_MSC_VER) || _MSC_VER >= 1300
#define HAVE_GETADDRINFO
#endif

#define HAVE_MEMMOVE
#define HAVE_SNPRINTF
#define snprintf    _snprintf

#if defined(_MSC_VER) && _MSC_VER < 1500
#define vsnprintf   _vsnprintf
#endif

typedef __int8  int8;
typedef __int16 int16;
typedef __int32 int32;
typedef __int64 int64;

typedef unsigned int uint;
typedef unsigned __int8 uint8;
typedef unsigned __int16 uint16;
typedef unsigned __int32 uint32;
typedef unsigned __int64 uint64;

#define SECS_BETWEEN_EPOCHS ((__int64)(11644473600))
#define SECS_TO_100NS ((__int64)(10000000))

#define THROW(x) throw x
#define THROWS(x) throw(x)
#define USING(x)
#define NEW_THROWS  throw()
#define THROWS_EMPTY    throw()

#define HAVE_MODULES 1
#undef  HAVE_PTHREAD_RWLOCK
#undef  PTHREAD_MUTEXTYPE_RECURSIVE

// define endian macros
#define __BYTE_ORDER __LITTLE_ENDIAN
#define __LITTLE_ENDIAN 1234
#define __BIG_ENDIAN 4321
#define __BYTE_ALIGNMENT 1

#pragma warning (disable:4786)
#if _MSC_VER >= 1300
#pragma warning (disable:4290)
#endif

#ifndef ETC_PREFIX
#define ETC_PREFIX "c:/"
#endif

#endif /* !defined(__MINGW32__) && !defined(__CYGWIN32__) */

// have exceptions
#ifdef  CCXX_NO_EXCEPTIONS
#undef  CCXX_EXCEPTIONS
#else
#define CCXX_EXCEPTIONS 1
#endif
// use namespace
#define CCXX_NAMESPACES 1

#define COMMON_DEADLOCK_DEBUG
#define COMMON_TPPORT_TYPE_DEFINED

#define CCXX_HAVE_NEW_INIT

#define HAVE_SSTREAM
#define HAVE_EXCEPTION

#ifdef  __BORLANDC__
#define HAVE_LOCALTIME_R
#endif

#endif /* #ifndef CCXX_CONFIG_H_ */

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
