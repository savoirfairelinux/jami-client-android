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
 * @file commoncpp/exception.h
 * @short GNU Common C++ exception model base classes.
 **/

#ifndef COMMONCPP_EXCEPTION_H_
#define COMMONCPP_EXCEPTION_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_STRING_H_
#include <commoncpp/string.h>
#endif

// see if we support useful and std exception handling, else we ignore
// it for the rest of the system.

#if defined(CCXX_EXCEPTIONS)
#define COMMONCPP_EXCEPTIONS

#include <exception>
#include <stdexcept>

NAMESPACE_COMMONCPP

/**
 * Mainline exception handler, this is the root for all Common C++
 * exceptions and assures the ansi C++ exception class hierarchy is both
 * followed and imported into the gnu Common C++ class hierarchy.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Base exception class for all Common C++ exceptions.
 */
class __EXPORT Exception : public std::exception
{
private:
    String _what;

public:
    Exception(const String& what_arg) throw();
    virtual ~Exception() throw();
    virtual const char *getString() const;
    virtual const char *what() const throw();
};

/**
 * A sub-hierarchy for all Common C++ I/O related classes.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short I/O operation exception hierarchy.
 */
class __EXPORT IOException : public Exception
{
private:
    long _systemError;
    mutable char* _systemErrorString;

public:
    IOException(const String &what_arg, long systemError = 0) throw();
    virtual ~IOException() throw();

    virtual long getSystemError() const throw();
    virtual const char* getSystemErrorString() const throw();
};

/**
 * A sub-hierarchy for thread exceptions.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short thread exception hierarchy.
 */
class __EXPORT ThrException : public Exception
{
public:
    inline ThrException(const String &what_arg) : Exception(what_arg) {};
};

/**
 * A sub-hierarchy for all task synchronizion related exceptions.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Synchronization object exception hierarchy.
 */
class __EXPORT SyncException : public ThrException
{
public:
    inline SyncException(const String &what_arg) : ThrException(what_arg) {};
};

class __EXPORT InterruptException : public ThrException
{
public:
    inline InterruptException() : ThrException("interrupted") {};
};

END_NAMESPACE

#endif

#endif
