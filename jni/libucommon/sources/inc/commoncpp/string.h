// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 3 of the License, or
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

/**
 * @file commoncpp/string.h
 * @short Common C++ generic string class
 **/

#ifndef COMMONCPP_STRING_H_
#define COMMONCPP_STRING_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

NAMESPACE_COMMONCPP

typedef ucommon::String String;

__EXPORT char *lsetField(char *target, size_t size, const char *src, const char fill = 0);
__EXPORT char *rsetField(char *target, size_t size, const char *src, const char fill = 0);
__EXPORT char *newString(const char *src, size_t size = 0);
__EXPORT void delString(char *str);
__EXPORT char *setUpper(char *string, size_t size);
__EXPORT char *setLower(char *string, size_t size);

inline char *setString(char *target, size_t size, const char *str)
    {return String::set(target, size, str);}

inline char *addString(char *target, size_t size, const char *str)
    {return String::add(target, size, str);}

inline char *dupString(const char *src, size_t size = 0)
    {return newString(src, size);}

/*
__EXPORT char *find(const char *cs, char *str, size_t len = 0);
__EXPORT char *rfind(const char *cs, char *str, size_t len = 0);
__EXPORT char *ifind(const char *cs, char *str, size_t len = 0);
__EXPORT char *strip(const char *cs, char *str, size_t len = 0);
__EXPORT size_t strchop(const char *cs, char *str, size_t len = 0);
__EXPORT size_t strtrim(const char *cs, char *str, size_t len = 0);

*/

END_NAMESPACE

#endif
