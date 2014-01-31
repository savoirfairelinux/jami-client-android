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
 * Placeholder for future classes that require stl support.
 * @file ucommon/stl.h
 */

#if defined(OLD_STDCPP) || defined(NEW_STDCPP)
#if !defined(_MSC_VER) || _MSC_VER >= 1400
#ifndef _UCOMMON_STL_H_
#define _UCOMMON_STL_H_
#define _UCOMMON_STL_EXTENDED_

#ifndef _UCOMMON_PLATFORM_H_
#include <ucommon/platform.h>
#endif

#include <list> // example...

NAMESPACE_UCOMMON

/*
    In the future we may introduce optional classes which require and/or
    build upon the standard template library.  This header indicates how and
    where they may be added.
*/

END_NAMESPACE

#endif
#endif
#endif
