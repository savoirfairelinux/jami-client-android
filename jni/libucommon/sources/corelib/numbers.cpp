// Copyright (C) 1999-2005 Open Source Telecom Corporation.
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

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/numbers.h>
#include <stdlib.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

using namespace UCOMMON_NAMESPACE;

Number::Number(char *buf, unsigned width)
{
    if(width > 10)
        width = 10;
    if(width < 1)
        width = 1;
    size = width;

    buffer = buf;
}

long Number::get(void) const
{
    int count = size;
    bool sign = false;
    long ret = 0;
    char *bp = buffer;

    if(*bp == '-') {
        --count;
        ++bp;
        sign = true;
    }
    else if(*bp == '+') {
        --count;
        ++bp;
    }
    while(count && *bp >='0' && *bp <='9') {
        ret = ret * 10l + (*bp - '0');
        --count;
        ++bp;
    }

    if(sign)
        ret = -ret;
    return ret;
}

void Number::set(long value)
{
    int count = size;
    char *bp = buffer;
    long max = 1;
    int exp;
    bool z = false;

    if(value < 0) {
        value = -value;
        --count;
        *(bp++) = '-';
    }

    exp = count;
    while(--exp)
        max *= 10;

    while(max) {
        if(value >= max || z) {
            --count;
            *(bp++) = '0' + ((char)(value / max));
        }
        if(value >= max) {
            z = true;
            value -= (value / max) * max;
        }
        max /= 10;
    }
    while(count-- && *bp >= '0' && *bp <='9')
        *(bp++) = ' ';
}

long Number::operator=(long value)
{
    set(value);
    return value;
}

long Number::operator=(const Number& num)
{
    set(num.get());
    return get();
}

long Number::operator+=(long value)
{
    long value1 = get() + value;
    set(value1);
    return value1;
}

long Number::operator-=(long value)
{
    long value1 = get() - value;
    set(value1);
    return value1;
}

long Number::operator--()
{
    long val = get();
    set(--val);
    return val;
}

long Number::operator++()
{
    long val = get();
    set(++val);
    return val;
}

ZNumber::ZNumber(char *buf, unsigned chars) :
Number(buf, chars)
{}

void ZNumber::set(long value)
{
    int count = size;
    char *bp = buffer;
    long max = 1;
    int exp;

    if(value < 0) {
        value = -value;
        --count;
        *(bp++) = '-';
    }

    exp = count;
    while(--exp)
        max *= 10;

    while(max) {
        --count;
        *(bp++) = '0' + (char)(value / max);
        value -= (value / max) * max;
        max /= 10;
    }
}

long ZNumber::operator=(long value)
{
    set(value);
    return value;
}

