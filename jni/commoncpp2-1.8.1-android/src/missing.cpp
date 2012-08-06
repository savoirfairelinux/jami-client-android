// Copyright (C) 2001-2005 Open Source Telecom Corporation.
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

#include <cc++/config.h>
#include <cc++/export.h>
#include <cc++/strchar.h>

#include <cstdio>
#include <cstdlib>
#include <cstring>

#ifdef  WIN32
#ifndef _WIN32_WCE
#include <sys/timeb.h>
#endif
#else

#ifdef  HAVE_SYS_PARAM_H
#include <sys/param.h>
#endif

#ifdef  HAVE_SYS_FILE_H
#include <sys/file.h>
#endif

#ifdef  HAVE_SYS_LOCKF_H
#include <sys/lockf.h>
#endif

#ifdef  COMMON_AIX_FIXES
#undef  LOCK_EX
#undef  LOCK_SH
#endif

#ifndef F_LOCK
#define MISSING_LOCKF

enum {
    F_ULOCK = 1,
    F_LOCK,
    F_TLOCK,
    F_TEST
};
#endif

#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

#ifdef  WIN32
#ifdef  _WIN32_WCE
int gettimeofday(struct timeval *tv_,  void *tz_)
{
    // We could use _ftime(), but it is not available on WinCE.
    // (WinCE also lacks time.h)
    // Note also that the average error of _ftime is around 20 ms :)
    DWORD ms = GetTickCount();
    tv_->tv_sec = ms / 1000;
    tv_->tv_usec = ms * 1000;
    return 0;
}
#else

int gettimeofday(struct timeval *tv_, void *tz_)
{
#if defined(_MSC_VER) && _MSC_VER >= 1300
    struct __timeb64 tb;
    _ftime64(&tb);
#else
# ifndef __BORLANDC__
    struct _timeb tb;
    _ftime(&tb);
# else
    struct timeb tb;
    ftime(&tb);
# endif
#endif
    tv_->tv_sec = (long)tb.time;
    tv_->tv_usec = tb.millitm * 1000;
    return 0;
}
#endif
#endif

#ifndef WIN32
#ifdef  HAVE_GETTIMEOFDAY

unsigned long getTicks(void)
{
    unsigned long ticks;

    struct timeval now;
    gettimeofday(&now, NULL);
    ticks = now.tv_sec * 1000l;
    ticks += now.tv_usec / 1000l;
    return ticks;
}

#endif
#else

DWORD getTicks(void)
{
    return GetTickCount();
}

#endif

#ifndef HAVE_STRDUP
char *strdup(const char *str)
{
    if(!str)
        return NULL;

    size_t len = strlen(str) + 1;
    char *dest = (char *)malloc(len);

    if(!dest)
        return NULL;

    return setString(dest, len, str);
}
#endif

#ifndef HAVE_MEMMOVE
void *memmove (char *dest, const char *source, size_t length)
{
    char *save = dest;
    if (source < dest) {
        for (source += length, dest += length; length; --length)
            *--dest = *--source;
    }
    else if (source != dest) {
        for (; length; --length)
            *dest++ = *source++;
    }
    return (void *) save;
}
#endif

#ifndef HAVE_LOCKF
int lockf(int fd, int cmd, long len)
{
    struct  flock lck;

    lck.l_start = 0l;
    lck.l_whence = SEEK_CUR;
    lck.l_len = len;

    switch(cmd) {
    case F_ULOCK:
        lck.l_type = F_UNLCK;
        return fcntl(fd, F_SETLK, &lck);
    case F_LOCK:
        lck.l_type = F_WRLCK;
        return fcntl(fd, F_SETLKW, &lck);
    case F_TLOCK:
        lck.l_type = F_WRLCK;
        return fcntl(fd, F_SETLK, &lck);
    case F_TEST:
        lck.l_type = F_WRLCK;
        fcntl(fd, F_GETLK, &lck);
        if(lck.l_type == F_UNLCK)
            return 0;
        return -1;
    }
}
#endif

#ifdef  CCXX_NAMESPACES
}
#endif
