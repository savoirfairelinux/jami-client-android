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
 * Export interfaces for library interfaces.
 * This is a special header used when we have to build DLL's for dumb
 * platforms which require explicit declaration of imports and exports.
 * The real purpose is to include our local headers in a new library
 * module with external headers referenced as imports, and then to define
 * our own interfaces in our new library as exports.  This allows native
 * construction of new DLL's based on/that use ucommon on Microsoft Windows
 * and perhaps for other similarly defective legacy platforms.  Otherwise
 * this header is not used at all, and not when building applications.
 * @file commoncpp/export.h
 */

#if defined(_MSC_VER) || defined(WIN32) || defined(_WIN32) || defined(_MSWINDOWS_)
#ifdef  __EXPORT
#undef  __EXPORT
#endif

#if defined(UCOMMON_STATIC) || defined(UCOMMON_RUNTIME)
#define __EXPORT
#else
#define __EXPORT __declspec(dllexport)
#endif
#endif

