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
#include <ucommon/cpr.h>

#if !defined(NEW_STDCPP) && !defined(OLD_STDCPP)
#undef  HAVE_STDEXCEPT
#endif

#ifdef  HAVE_STDEXCEPT
#include <stdexcept>
#endif

#include <ucommon/export.h>
#include <ucommon/string.h>
#include <ucommon/socket.h>
#include <errno.h>
#include <stdarg.h>

#include <ctype.h>
#include <stdlib.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#include <limits.h>

using namespace UCOMMON_NAMESPACE;

#ifdef  _MSWINDOWS_
int cpr_setenv(const char *sym, const char *val, int flag)
{
    char buf[128];

    if(!flag) {
        if(GetEnvironmentVariable(sym, buf, sizeof(buf)) > 0)
            return 0;
        if(GetLastError() != ERROR_ENVVAR_NOT_FOUND)
            return -1;
    }
    if(SetEnvironmentVariable(sym, val) == 0)
        return -1;
    return 0;
}

#endif

void cpr_runtime_error(const char *str)
{
    assert(str != NULL);

#ifdef  HAVE_STDEXCEPT
    throw std::runtime_error(str);
#endif
    abort();
}

// just become we need to get binary types in a specific binary endian order.

extern "C" uint16_t lsb_getshort(uint8_t *b)
{
    assert(b != NULL);
    return (b[1] * 256) + b[0];
}

extern "C" uint32_t lsb_getlong(uint8_t *b)
{
    assert(b != NULL);
    return (b[3] * 16777216l) + (b[2] * 65536l) + (b[1] * 256) + b[0];
}

extern "C" uint16_t msb_getshort(uint8_t *b)
{
    assert(b != NULL);
    return (b[0] * 256) + b[1];
}

extern "C" uint32_t msb_getlong(uint8_t *b)
{
    assert(b != NULL);
    return (b[0] * 16777216l) + (b[1] * 65536l) + (b[2] * 256) + b[3];
}

extern "C" void lsb_setshort(uint8_t *b, uint16_t v)
{
    assert(b != NULL);
    b[0] = v & 0x0ff;
    b[1] = (v / 256) & 0xff;
}

extern "C" void msb_setshort(uint8_t *b, uint16_t v)
{
    assert(b != NULL);
    b[1] = v & 0x0ff;
    b[0] = (v / 256) & 0xff;
}

// oh, and we have to be able to set them in order too...

extern "C" void lsb_setlong(uint8_t *b, uint32_t v)
{
    assert(b != NULL);
    b[0] = (uint8_t)(v & 0x0ff);
    b[1] = (uint8_t)((v / 256) & 0xff);
    b[2] = (uint8_t)((v / 65536l) & 0xff);
    b[3] = (uint8_t)((v / 16777216l) & 0xff);
}

extern "C" void msb_setlong(uint8_t *b, uint32_t v)
{
    assert(b != NULL);
    b[3] = (uint8_t)(v & 0x0ff);
    b[2] = (uint8_t)((v / 256) & 0xff);
    b[1] = (uint8_t)((v / 65536l) & 0xff);
    b[0] = (uint8_t)((v / 16777216l) & 0xff);
}

extern "C" void cpr_memswap(void *s1, void *s2, size_t size)
{
    assert(s1 != NULL);
    assert(s2 != NULL);
    assert(size > 0);

    char *buf = new char[size];
    memcpy(buf, s1, size);
    memcpy(s1, s2, size);
    memcpy(s2, buf, size);
    delete[] buf;
}

// if malloc ever fails, we probably should consider that a critical error and
// kill the leaky dingy, which this does for us here..

extern "C" void *cpr_memalloc(size_t size)
{
    void *mem;

    if(!size)
        ++size;

    mem = malloc(size);
    crit(mem != NULL, "memory alloc failed");
    return mem;
}

extern "C" void *cpr_memassign(size_t size, caddr_t addr, size_t max)
{
    assert(addr);
    crit((size <= max), "memory assign failed");
    return addr;
}

#ifdef  __GNUC__

// here we have one of those magic things in gcc, and what to do when
// we have an unimplemented virtual function if we build ucommon without
// a stdc++ runtime library.

extern "C" void __cxa_pure_virtual(void)
{
    abort();
}

#endif

// vim: set ts=4 sw=4:
// Local Variables:
// c-basic-offset: 4
// tab-width: 4
// End:
