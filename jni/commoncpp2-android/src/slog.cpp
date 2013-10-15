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

#include <cc++/config.h>
#include <cc++/export.h>
#include <cc++/thread.h>
#include <cc++/slog.h>
#ifdef  __BORLANDC__
#include <stdio.h>
#include <stdarg.h>
#else
#include <cstdio>
#include <cstdarg>
#endif
#include "../src/private.h"

#ifdef  HAVE_SYSLOG_H
#include <syslog.h>
#endif

using std::streambuf;
using std::ofstream;
using std::ostream;
using std::clog;
using std::endl;
using std::ios;

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

Slog slog;

Slog::Slog(void) :
streambuf()
#ifdef  HAVE_OLD_IOSTREAM
,ostream()
#else
,ostream((streambuf *)this)
#endif
{
#ifdef  HAVE_OLD_IOSTREAM
    init((streambuf *)this);
#endif
    _enable = true;
    _level = levelDebug;
        _clogEnable = true;
#ifndef HAVE_SYSLOG_H
    syslog = NULL;
#endif
}

Slog::~Slog(void)
{
#ifdef  HAVE_SYSLOG_H
    closelog();
#else
    if(syslog)
        fclose(syslog);
#endif
}

ThreadImpl *Slog::getPriv(void)
{
    Thread *thread = Thread::get();

    if(!thread)
        return NULL;

    return thread->priv;
}

void Slog::close(void)
{
#ifdef  HAVE_SYSLOG_H
    closelog();
#else
    lock.enterMutex();
    if(syslog)
        fclose(syslog);
    syslog = NULL;
    lock.leaveMutex();
#endif
}

void Slog::open(const char *ident, Class grp)
{
    const char *cp;

#ifdef  HAVE_SYSLOG_H
    cp = strrchr(ident, '/');
    if(cp)
        ident = ++cp;

    int fac;

    switch(grp) {
    case classUser:
        fac = LOG_USER;
        break;

    case classDaemon:
        fac = LOG_DAEMON;
        break;

    case classAudit:
#ifdef  LOG_AUTHPRIV
        fac = LOG_AUTHPRIV;
        break;
#endif
    case classSecurity:
        fac = LOG_AUTH;
        break;

    case classLocal0:
        fac = LOG_LOCAL0;
        break;

    case classLocal1:
        fac = LOG_LOCAL1;
        break;

    case classLocal2:
        fac = LOG_LOCAL2;
        break;

    case classLocal3:
        fac = LOG_LOCAL3;
        break;

    case classLocal4:
        fac = LOG_LOCAL4;
        break;

    case classLocal5:
        fac = LOG_LOCAL5;
        break;

    case classLocal6:
        fac = LOG_LOCAL6;
        break;

    case classLocal7:
        fac = LOG_LOCAL7;
        break;

    default:
        fac = LOG_USER;
        break;
    }
    openlog(ident, 0, fac);
#else
    char *buf;

    lock.enterMutex();
    if(syslog)
            fclose(syslog);
    buf = new char[strlen(ident) + 1];
    strcpy(buf, ident);
    cp = (const char *)buf;
    buf = strrchr(buf, '.');
    if(buf) {
        if(!stricmp(buf, ".exe"))
            strcpy(buf, ".log");
    }
    syslog = fopen(cp, "a");
    delete[] (char *)cp;
    lock.leaveMutex();
#endif
}

#ifdef  HAVE_SNPRINTF

void Slog::error(const char *format, ...)
{
    ThreadImpl *thread = getPriv();
    va_list args;
    va_start(args, format);
    overflow(EOF);

    if(!thread)
        return;

    error();

    vsnprintf(thread->_msgbuf, sizeof(thread->_msgbuf), format, args);
    thread->_msgpos = strlen(thread->_msgbuf);
    overflow(EOF);
    va_end(args);
}

void Slog::warn(const char *format, ...)
{
    ThreadImpl *thread = getPriv();
    va_list args;

    if(!thread)
        return;

    va_start(args, format);
    overflow(EOF);
    warn();
    vsnprintf(thread->_msgbuf, sizeof(thread->_msgbuf), format, args);
    thread->_msgpos = strlen(thread->_msgbuf);
    overflow(EOF);
    va_end(args);
}

void Slog::debug(const char *format, ...)
{
    ThreadImpl *thread = getPriv();
    va_list args;

    if(!thread)
        return;

    va_start(args, format);
    overflow(EOF);
    debug();
    vsnprintf(thread->_msgbuf, sizeof(thread->_msgbuf), format, args);
    thread->_msgpos = strlen(thread->_msgbuf);
    overflow(EOF);
    va_end(args);
}

void Slog::emerg(const char *format, ...)
{
    ThreadImpl *thread = getPriv();
    va_list args;

    if(!thread)
        return;

    va_start(args, format);
    overflow(EOF);
    emerg();
    vsnprintf(thread->_msgbuf, sizeof(thread->_msgbuf), format, args);
    thread->_msgpos = strlen(thread->_msgbuf);
    overflow(EOF);
    va_end(args);
}

void Slog::alert(const char *format, ...)
{
    ThreadImpl *thread = getPriv();
    va_list args;

    if(!thread)
        return;

    va_start(args, format);
    overflow(EOF);
    alert();
    vsnprintf(thread->_msgbuf, sizeof(thread->_msgbuf), format, args);
    thread->_msgpos = strlen(thread->_msgbuf);
    overflow(EOF);
    va_end(args);
}

void Slog::critical(const char *format, ...)
{
    ThreadImpl *thread = getPriv();
    va_list args;

    if(!thread)
        return;

    va_start(args, format);
    overflow(EOF);
    critical();
    vsnprintf(thread->_msgbuf, sizeof(thread->_msgbuf), format, args);
    thread->_msgpos = strlen(thread->_msgbuf);
    overflow(EOF);
    va_end(args);
}

void Slog::notice(const char *format, ...)
{
    ThreadImpl *thread = getPriv();
    va_list args;

    if(!thread)
        return;

    va_start(args, format);
    overflow(EOF);
    notice();
    vsnprintf(thread->_msgbuf, sizeof(thread->_msgbuf), format, args);
    thread->_msgpos = strlen(thread->_msgbuf);
    overflow(EOF);
    va_end(args);
}

void Slog::info(const char *format, ...)
{
    ThreadImpl *thread = getPriv();
    va_list args;

    if(!thread)
        return;

    va_start(args, format);
    overflow(EOF);
    info();
    vsnprintf(thread->_msgbuf, sizeof(thread->_msgbuf), format, args);
    thread->_msgpos = strlen(thread->_msgbuf);
    overflow(EOF);
    va_end(args);
    }
#endif

int Slog::overflow(int c)
{
    ThreadImpl *thread = getPriv();
    if(!thread)
        return c;

    if(c == '\n' || !c || c == EOF) {
        if(!thread->_msgpos)
            return c;

        thread->_msgbuf[thread->_msgpos] = 0;
        if (_enable)
#ifdef  HAVE_SYSLOG_H
            syslog(priority, "%s", thread->_msgbuf);
#else
        {
            time_t now;
            struct tm *dt;
            time(&now);
            dt = localtime(&now);
            char buf[256];
            const char *p = "unknown";
            switch(priority) {
            case levelEmergency:
                p = "emerg";
                break;
            case levelInfo:
                p = "info";
                break;
            case levelError:
                p = "error";
                break;
            case levelAlert:
                p = "alert";
                break;
            case levelDebug:
                p = "debug";
                break;
            case levelNotice:
                p = "notice";
                break;
            case levelWarning:
                p = "warn";
                break;
            case levelCritical:
                p = "crit";
                break;
            }

            lock.enterMutex();
            snprintf(buf, sizeof(buf), "%04d-%02d-%02d %02d:%02d:%02d [%s] %s\n",
                dt->tm_year + 1900, dt->tm_mon + 1, dt->tm_mday,
                dt->tm_hour, dt->tm_min, dt->tm_sec,
                p, thread->_msgbuf);
            if(syslog)
                fputs(buf, syslog);
//              syslog << "[" << priority << "] " << thread->_msgbuf << endl;
            lock.leaveMutex();
        }
#endif
    thread->_msgpos = 0;

        if ( _enable && _clogEnable
#ifndef WIN32
            && (getppid() > 1)
#endif
            )
            clog << thread->_msgbuf << endl;
        _enable = true;
        return c;
    }

    if (thread->_msgpos < (int)(sizeof(thread->_msgbuf) - 1))
        thread->_msgbuf[thread->_msgpos++] = c;

    return c;
}

Slog &Slog::operator()(const char *ident, Class grp, Level lev)
{
    ThreadImpl *thread = getPriv();

    if(!thread)
        return *this;

    thread->_msgpos = 0;
    _enable = true;
    open(ident, grp);
    return this->operator()(lev, grp);
}

Slog &Slog::operator()(Level lev, Class grp)
{
    ThreadImpl *thread = getPriv();

    if(!thread)
        return *this;

    thread->_msgpos = 0;
    if(_level >= lev)
        _enable = true;
    else
        _enable = false;

#ifdef  HAVE_SYSLOG_H
    switch(lev) {
    case levelEmergency:
        priority = LOG_EMERG;
        break;
    case levelAlert:
        priority = LOG_ALERT;
        break;
    case levelCritical:
        priority = LOG_CRIT;
        break;
    case levelError:
        priority = LOG_ERR;
        break;
    case levelWarning:
        priority = LOG_WARNING;
        break;
    case levelNotice:
        priority = LOG_NOTICE;
        break;
    case levelInfo:
        priority = LOG_INFO;
        break;
    case levelDebug:
        priority = LOG_DEBUG;
        break;
    }
    switch(grp) {
    case classAudit:
#ifdef  LOG_AUTHPRIV
        priority |= LOG_AUTHPRIV;
        break;
#endif
    case classSecurity:
        priority |= LOG_AUTH;
        break;
    case classUser:
        priority |= LOG_USER;
        break;
    case classDaemon:
        priority |= LOG_DAEMON;
        break;
    case classDefault:
        priority |= LOG_USER;
        break;
    case classLocal0:
        priority |= LOG_LOCAL0;
        break;
    case classLocal1:
        priority |= LOG_LOCAL1;
        break;
    case classLocal2:
        priority |= LOG_LOCAL2;
        break;
    case classLocal3:
        priority |= LOG_LOCAL3;
        break;
    case classLocal4:
        priority |= LOG_LOCAL4;
        break;
    case classLocal5:
        priority |= LOG_LOCAL5;
        break;
    case classLocal6:
        priority |= LOG_LOCAL6;
        break;
    case classLocal7:
        priority |= LOG_LOCAL7;
        break;
    }
#else
    priority = lev;
#endif
    return *this;
}

Slog &Slog::operator()(void)
{
    return *this;
}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 6
 * End:
 */
