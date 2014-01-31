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

void Digest::release(void)
{
    if(context) {
        gnutls_hash_deinit((MD_CTX)context, buffer);
        context = NULL;
    }

    bufsize = 0;
    textbuf[0] = 0;
    hashid = 0;
}

int context::map_digest(const char *type)
{
    if(eq_case(type, "sha") || eq_case(type, "sha1")) 
        return GNUTLS_DIG_SHA1;
    else if(eq_case(type, "sha256"))
        return GNUTLS_DIG_SHA256;
    else if(eq_case(type, "sha512"))
        return GNUTLS_DIG_SHA512;
    else if(eq_case(type, "md5"))
        return GNUTLS_DIG_MD5;
    else if(eq_case(type, "md2"))
        return GNUTLS_DIG_MD2;
    else if(eq_case(type, "rmd160"))
        return GNUTLS_DIG_RMD160;
    else
        return 0;
}

void Digest::set(const char *type)
{
    secure::init();

    release();

    hashid = context::map_digest(type);
    
    if(!hashid || gnutls_hash_get_len((MD_ID)hashid) < 1) {
        hashid = 0;
        return;
    }

    gnutls_hash_init((MD_CTX *)&context, (MD_ID)hashid);
}

bool Digest::has(const char *type)
{
    MD_ID id = (MD_ID)context::map_digest(type);

    if(!id || (gnutls_hash_get_len(id) < 1))
        return false;

    return true;
}

bool Digest::put(const void *address, size_t size)
{
    if(!context || hashid == 0)
        return false;

    gnutls_hash((MD_CTX)context, address, size);
    return true;
}

void Digest::reset(void)
{
    unsigned char temp[MAX_DIGEST_HASHSIZE / 8];

    if(context) {
        gnutls_hash_deinit((MD_CTX)context, temp);
        context = NULL;
    }
    if(hashid == 0)
        return;

    gnutls_hash_init((MD_CTX *)&context, (MD_ID)hashid);
    bufsize = 0;
}

void Digest::recycle(bool bin)
{
    unsigned size = bufsize;

    if(!context || hashid == 0)
        return;

    if(!bufsize) {
        gnutls_hash_deinit((MD_CTX)context, buffer);
        context = NULL;
        gnutls_hash_init((MD_CTX *)&context, (MD_ID)hashid);
    }
    else
        Digest::reset();

    size = gnutls_hash_get_len((MD_ID)hashid);

    if(!size || !context || !hashid)
        return;

    if(bin)
        gnutls_hash((MD_CTX)context, buffer, size);
    else {
        unsigned count = 0;

        while(count < size) {
            snprintf(textbuf + (count * 2), 3, "%2.2x",
buffer[count]);
            ++count;

        }
        gnutls_hash((MD_CTX)context, textbuf, size * 2);
    }
    bufsize = 0;
}

const unsigned char *Digest::get(void)
{
    unsigned count = 0;
    unsigned size = 0;

    if(bufsize)
        return buffer;

    if(!context || hashid == 0)
        return NULL;

    gnutls_hash_deinit((MD_CTX)context, buffer);
    size = gnutls_hash_get_len((MD_ID)hashid);
    context = NULL;
    bufsize = size;

    while(count < bufsize) {
        snprintf(textbuf + (count * 2), 3, "%2.2x",
buffer[count]);
        ++count;
    }
    return buffer;
}

