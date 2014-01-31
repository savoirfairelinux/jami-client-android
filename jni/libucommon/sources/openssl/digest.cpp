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

bool Digest::has(const char *id)
{
    return (EVP_get_digestbyname(id) != NULL);
}

void Digest::set(const char *type)
{
    secure::init();

    release();

    // never use sha0
    if(eq_case(type, "sha"))
        type = "sha1";

    hashtype = (void *)EVP_get_digestbyname(type);
    if(hashtype) {
        context = new EVP_MD_CTX;
        EVP_MD_CTX_init((EVP_MD_CTX *)context);
        EVP_DigestInit_ex((EVP_MD_CTX *)context, (const EVP_MD *)hashtype, NULL);
    }
}

void Digest::release(void)
{
    if(context)
        EVP_MD_CTX_cleanup((EVP_MD_CTX *)context);

    if(context) {
        delete (EVP_MD_CTX *)context;
        context = NULL;
    }

    bufsize = 0;
    textbuf[0] = 0;
}

bool Digest::put(const void *address, size_t size)
{
    if(!context)
        return false;

    EVP_DigestUpdate((EVP_MD_CTX *)context, address, size);
    return true;
}

void Digest::reset(void)
{
    if(!context) {
        if(hashtype) {
            context = new EVP_MD_CTX;
            EVP_MD_CTX_init((EVP_MD_CTX *)context);
        }
        else
            return;
    }

    EVP_DigestInit_ex((EVP_MD_CTX *)context, (const EVP_MD *)hashtype, NULL);
    bufsize = 0;
}

void Digest::recycle(bool bin)
{
    unsigned size = bufsize;

    if(!context)
        return;

    if(!bufsize)
        EVP_DigestFinal_ex((EVP_MD_CTX *)context, buffer, &size);

    EVP_DigestInit_ex((EVP_MD_CTX *)context, (const EVP_MD *)hashtype, NULL);

    if(bin)
        EVP_DigestUpdate((EVP_MD_CTX *)context, buffer, size);
    else {
        unsigned count = 0;
        while(count < size) {
            snprintf(textbuf + (count * 2), 3, "%2.2x",
buffer[count]);
            ++count;
        }
        EVP_DigestUpdate((EVP_MD_CTX *)context, textbuf, size *
2);
    }
    bufsize = 0;

}

const unsigned char *Digest::get(void)
{
    unsigned count = 0;
    unsigned size = 0;

    if(bufsize)
        return buffer;

    if(!context)
        return NULL;

    EVP_DigestFinal_ex((EVP_MD_CTX *)context, buffer, &size);

    release();

    bufsize = size;

    while(count < bufsize) {
        snprintf(textbuf + (count * 2), 3, "%2.2x",
buffer[count]);
        ++count;
    }
    return buffer;
}

