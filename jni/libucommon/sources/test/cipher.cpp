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

#ifndef DEBUG
#define DEBUG
#endif

#include <ucommon-config.h>
#include <ucommon/secure.h>

#include <stdio.h>

using namespace UCOMMON_NAMESPACE;

#define STR "this is a test of some text we wish to post"

int main(int argc, char **argv)
{
    if(!secure::init())
        return 0;

    skey_t mykey("aes256", "sha", "testing");
    cipher_t enc, dec;
    unsigned char ebuf[256], dbuf[256];

    memset(dbuf, 0, sizeof(dbuf));

    enc.set(&mykey, Cipher::ENCRYPT, ebuf);
    dec.set(&mykey, Cipher::DECRYPT, dbuf);

    size_t total = enc.puts(STR);

    assert(!eq(STR, (char *)ebuf, strlen(STR)));

    assert(total == 48);

    dec.put(ebuf, total);
    dec.flush();
    assert(eq((char *)dbuf, STR));
    return 0;
}

