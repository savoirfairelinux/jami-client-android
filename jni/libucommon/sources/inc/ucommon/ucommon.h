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
 * Top level include file for the GNU uCommon C++ core library.
 * This is the only include file you need to have in your sources; it
 * includes the remaining header files.
 * @file ucommon/ucommon.h
 */

/**
 * @short A portable C++ threading library for embedded applications.
 * GNU uCommon C++ is meant as a very light-weight library to facilitate using
 * C++ design patterns even for very deeply embedded applications, such as for
 * systems using uclibc along with posix threading support.  For this reason,
 * uCommon disables language features that consume memory or introduce runtime
 * overhead, such as rtti and exception handling, and assumes one will mostly
 * be linking applications with other pure C based libraries rather than using
 * the overhead of the standard C++ library and other class frameworks.
 *
 * uCommon by default does build with support for the bloated ansi standard c++
 * library unless this is changed at configure time with the --disable-stdcpp
 * option.  This is to assure maximum portability and will be used to merge
 * uCommon with GNU Common C++ to form GNU Common C++ 2.0.  Some specific
 * features are tested for when stdc++ is enabled, and these will be used
 * to add back in GNU Common C++ classes such as TCP Stream and serialization.
 *
 * uCommon introduces some Objective-C based design patterns, such as reference
 * counted objects, memory pools, smart pointers, and offers dynamic typing
 * through very light use of inline templates for pure type translation that are
 * then tied to concrete base classes to avoid template instantiation issues.  C++
 * auto-variable automation is also used to enable referenced objects to be
 * deleted and threading locks to be released that are acquired automatically when
 * methods return rather than requiring one to explicitly code for these things.
 *
 * uCommon depends on and when necessary will introduce some portable C
 * replacement functions, especially for sockets, such as adding getaddrinfo for
 * platforms which do not have it, or when threadsafe versions of existing C
 * library functions are needed.  Basic socket support for connecting to named
 * destinations and multicast addresses, and binding to interfaces with IPV4 and
 * IPV6 addresses is directly supported.  Support for high resolution timing and
 * Posix realtime clocks are also used when available.
 *
 * uCommon builds all higher level thread synchronization objects directly from
 * conditionals.  Hence, on platforms which for example do not have rwlocks,
 * barriers, or semaphores, these are still found in uCommon.  A common and
 * consistent call methodology is used for all locks, whether mutex, rw, or
 * semaphore, based on whether used for exclusive or "shared" locking.
 *
 * uCommon requires some knowledge of compiler switches and options to disable
 * language features, the C++ runtime and stdlibs, and associated C++ headers. The
 * current version supports compiling with GCC, which is commonly found on
 * GNU/Linux, OS/X, BSD based systems, and many other platforms; and the Sun
 * Workshop compiler, which is offered as an example how to adapt uCommon for
 * additional compilers. uCommon may also be built with GCC cross compiling for
 * mingw32 to build threaded applications for Microsoft Windows targets nativiely.
 *
 * The minimum platform support for uCommon is a modern and working posix
 * pthread threading library.  I further use a subset of posix threads to assure
 * wider portability by avoiding more specialized features like process shared
 * synchronization objects, pthread rwlocks and pthread semaphores, as these are
 * not implemented on all platforms that I have found.  Finally, I have
 * eliminated the use of posix thread cancellation.
 * @author David Sugar <dyfet@gnutelephony.org>
 * @license GNU Lesser General Public License Version 3 or later
 * @mainpage GNU uCommon C++
 */

#ifndef _UCOMMON_UCOMMON_H_
#define _UCOMMON_UCOMMON_H_
#include <ucommon/platform.h>
#include <ucommon/cpr.h>
#include <ucommon/atomic.h>
#include <ucommon/generics.h>
#include <ucommon/protocols.h>
#include <ucommon/object.h>
#include <ucommon/string.h>
#include <ucommon/counter.h>
#include <ucommon/numbers.h>
#include <ucommon/vector.h>
#include <ucommon/linked.h>
#include <ucommon/timers.h>
#include <ucommon/access.h>
#include <ucommon/memory.h>
#include <ucommon/mapped.h>
#include <ucommon/unicode.h>
#include <ucommon/datetime.h>
#include <ucommon/keydata.h>
#include <ucommon/bitmap.h>
#include <ucommon/socket.h>
#include <ucommon/thread.h>
#include <ucommon/containers.h>
#include <ucommon/fsys.h>
#include <ucommon/file.h>
#include <ucommon/buffer.h>
#include <ucommon/shell.h>
#include <ucommon/xml.h>

#ifdef  _UCOMMON_EXTENDED_
#include <ucommon/stream.h>
#include <ucommon/persist.h>
#include <ucommon/stl.h>
#endif

#endif
