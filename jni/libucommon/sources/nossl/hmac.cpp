// Copyright (C) 2010 David Sugar, Tycho Softworks.
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

#include "local.h"

bool HMAC::has(const char *id)
{
    return false;
}

void HMAC::set(const char *digest, const char *key, size_t len)
{
    release();
}

void HMAC::release(void)
{
    bufsize = 0;
    textbuf[0] = 0;

    hmactype = " ";
}

bool HMAC::put(const void *address, size_t size)
{
    return false;
}

const unsigned char *HMAC::get(void)
{
    if(bufsize)
        return buffer;

    return NULL;
}

