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
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// Common C++.  If you copy code from other releases into a copy of GNU
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

/**
 * @file missing.h
 * @short substitute functions which may be missing in target platform libc.
 **/

#ifndef CCXX_MISSING_H_
#define CCXX_MISSING_H_

#ifndef CCXX_CONFIG_H_
#include <cc++/config.h>
#endif

#ifndef CCXX_STRCHAR_H_
#include <cc++/strchar.h>
#endif

#include <ctime>

#ifdef  MACOSX
#undef  HAVE_LOCKF
#endif

#ifdef  WIN32
#ifndef HAVE_LOCKF
#define HAVE_LOCKF
#endif
#endif

#include <fstream>
#include <iostream>
#include <ctime>

#ifdef  HAVE_SSTREAM
#include <sstream>
#else
#include <strstream>
#endif

#if defined(__KCC)
#define ostream ostream_withassign
#endif

#ifdef __BORLANDC__
#include <time.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifndef HAVE_GETTIMEOFDAY
#ifdef  WIN32
#define HAVE_GETTIMEOFDAY
__EXPORT int gettimeofday(struct timeval *tv_,  void *tz_);
#endif
#endif

#ifdef  HAVE_GETTIMEOFDAY
#ifdef  WIN32
__EXPORT DWORD getTicks(void);
#else
__EXPORT unsigned long getTicks(void);
#endif
#endif

#ifndef HAVE_MEMMOVE
__EXPORT void *memmove(char *dest, const char *source, size_t length);
#endif

#ifndef HAVE_STRDUP
__EXPORT char *strdup(const char *str);
#endif

#ifndef HAVE_LOCKF
__EXPORT int lockf(int fd, int mode, long offset);
#endif

#ifndef HAVE_STRTOK_R

inline char *strtok_r(char *s, const char *d, char **x) \
    {return strtok(s, d);};

#endif

#ifdef  CCXX_NAMESPACES
}
#endif

#endif
