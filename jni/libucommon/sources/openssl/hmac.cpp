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
    return (EVP_get_digestbyname(id) != NULL);
}

void HMAC::set(const char *digest, const char *key, size_t len)
{
    secure::init();

    release();

    if(!len)
        len = strlen(key);

    hmactype = EVP_get_digestbyname(digest);
    if(hmactype && len) {
        context = new HMAC_CTX;
        HMAC_CTX_init((HMAC_CTX *)context);
        HMAC_Init((HMAC_CTX *)context, key, len, (const EVP_MD *)hmactype);
    }
}

void HMAC::release(void)
{
    if(context)
        HMAC_cleanup((HMAC_CTX *)context);

    if(context) {
        delete (HMAC_CTX *)context;
        context = NULL;
    }

    bufsize = 0;
    textbuf[0] = 0;
}

bool HMAC::put(const void *address, size_t size)
{
    if(!context)
        return false;

    HMAC_Update((HMAC_CTX *)context, (const unsigned char *)address, size);
    return true;
}

const unsigned char *HMAC::get(void)
{
    unsigned count = 0;
    unsigned size = 0;

    if(bufsize)
        return buffer;

    if(!context)
        return NULL;

	HMAC_Final((HMAC_CTX *)context, buffer, &size);
	release();

	if(!size)
		return NULL;

    bufsize = size;

    while(count < bufsize) {
        snprintf(textbuf + (count * 2), 3, "%2.2x",
buffer[count]);
        ++count;
    }
    return buffer;
}

