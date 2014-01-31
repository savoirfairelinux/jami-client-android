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

#include <ucommon-config.h>

#ifndef _XOPEN_SOURCE
#define _XOPEN_SOURCE   600
#endif

#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/string.h>
#include <commoncpp/thread.h>
#include <commoncpp/exception.h>

#if defined(CCXX_EXCEPTIONS)

using namespace COMMONCPP_NAMESPACE;

Exception::Exception(const String& what_arg) throw():
_what(what_arg)
{}

Exception::~Exception() throw()
{}

const char *Exception::what() const throw()
{
    return _what.c_str();
}

const char *Exception::getString() const
{
    return _what.c_str();
}

IOException::IOException(const String &what_arg, long systemError) throw() :
    Exception(what_arg), _systemError(systemError),
    _systemErrorString(NULL) {
}

IOException::~IOException() throw()
{
    delete [] _systemErrorString;
}

long IOException::getSystemError() const throw()
{
    return _systemError;
}

const char* IOException::getSystemErrorString() const throw()
{
    const uint32_t errStrSize = 2048;
    if ( !_systemErrorString )
        _systemErrorString = new char[errStrSize];
#ifndef _MSWINDOWS_
#ifdef  HAVE_STRERROR_R
#if ((_POSIX_C_SOURCE >= 200112L || _XOPEN_SOURCE >= 600) && !defined(_GNU_SOURCE) || !defined(APPLE))
    assert(strerror_r(_systemError, _systemErrorString, errStrSize) == 0);
#else
    _systemErrorString = strerror_r(_systemError, _systemErrorString, errStrSize);
#endif
#else
    static Mutex mlock;

    mlock.enter();
    String::set(_systemErrorString, errStrSize, strerror(_systemError));
    mlock.leave();
#endif
    return _systemErrorString;
#else
    FormatMessage( FORMAT_MESSAGE_FROM_SYSTEM, NULL, _systemError,
        MAKELANGID(LANG_NEUTRAL,SUBLANG_DEFAULT),
        _systemErrorString, errStrSize, NULL);
    return _systemErrorString;
#endif
}

#endif
