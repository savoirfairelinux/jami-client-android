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
#include <ctype.h>
#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/string.h>
#include <commoncpp/numbers.h>

NAMESPACE_COMMONCPP

char *newString(const char *src, size_t size)
{
    char *dest;

    if(!size)
        size = strlen(src) + 1;

    dest = new char[size];
    return setString(dest, size, src);
}

void delString(char *str)
{
    delete[] str;
}

char *setUpper(char *string, size_t size)
{
    char *ret = string;

    if(!size)
        size = strlen(string);

    while(size && *string) {
        *string = toupper(*string);
        ++string;
        --size;
    }

    return ret;
}

char *setLower(char *string, size_t size)
{
    char *ret = string;

    if(!size)
        size = strlen(string);

    while(size && *string) {
        *string = tolower(*string);
        ++string;
        --size;
    }

    return ret;
}

char *rsetField(char *dest, size_t size, const char *src, const char fill)
{
    size_t len = 0;

    if(src)
        len = strlen(src);

    if(len > size)
        len = size;

    if(len)
        memmove(dest + size - len, (void *)src, len);

    if(len < size && fill)
        memset(dest, fill, size - len);

    return dest;
}

char *lsetField(char *dest, size_t size, const char *src, const char fill)
{
    size_t len = 0;

    if(src)
        len = strlen(src);

    if(len > size)
        len = size;

    if(len)
        memmove(dest, src, len);

    if(len < size && fill)
        memset(dest + len, fill, size - len);

    return dest;
}

END_NAMESPACE
