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

// for older/broken versions of gnutls_hmac_init headers

class multicode
{
public:
    int code;

    inline operator HMAC_ID()
        {return (HMAC_ID)code;}

    inline operator MD_ID()
        {return (MD_ID)code;}
};

void HMAC::release(void)
{
    if(context) {
        gnutls_hmac_deinit((HMAC_CTX)context, buffer);
        context = NULL;
    }

    bufsize = 0;
    textbuf[0] = 0;
    hmacid = 0;
}

int context::map_hmac(const char *type)
{
    if(eq_case(type, "sha") || eq_case(type, "sha1")) 
        return GNUTLS_MAC_SHA1;
    else if(eq_case(type, "sha256"))
        return GNUTLS_MAC_SHA256;
    else if(eq_case(type, "sha224"))
        return GNUTLS_MAC_SHA224;
    else if(eq_case(type, "sha384"))
        return GNUTLS_MAC_SHA384;
    else if(eq_case(type, "sha512"))
        return GNUTLS_MAC_SHA512;
    else if(eq_case(type, "md5"))
        return GNUTLS_MAC_MD5;
    else if(eq_case(type, "md2"))
        return GNUTLS_MAC_MD2;
    else if(eq_case(type, "rmd160"))
        return GNUTLS_MAC_RMD160;
    else
        return 0;
}

void HMAC::set(const char *digest, const char *key, size_t len)
{
    secure::init();

    release();

    if(!len)
        len = strlen(key);

    if(!len)
        return;

    hmacid = context::map_hmac(digest);

    multicode id;
    id.code = hmacid;

    if(hmacid)
        gnutls_hmac_init((HMAC_CTX *)&context, id, key, len);
}

bool HMAC::has(const char *type)
{
    HMAC_ID id = (HMAC_ID)context::map_hmac(type);

    if(!id || (gnutls_hmac_get_len(id) < 1))
        return false;

    return true;
}

bool HMAC::put(const void *address, size_t size)
{
    if(!context || hmacid == 0)
        return false;

    gnutls_hmac((HMAC_CTX)context, address, size);
    return true;
}

const unsigned char *HMAC::get(void)
{
    unsigned count = 0;
    unsigned size = 0;

    if(bufsize)
        return buffer;

    if(!context || hmacid == 0)
        return NULL;

    size = gnutls_hmac_get_len((HMAC_ID)hmacid);
    release();

    bufsize = size;

    while(count < bufsize) {
        snprintf(textbuf + (count * 2), 3, "%2.2x",
buffer[count]);
        ++count;
    }
    return buffer;
}
