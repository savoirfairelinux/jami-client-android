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

static const unsigned char *_salt = NULL;
static unsigned _rounds = 1;

void Cipher::Key::assign(const char *text, size_t size)
{
    assign(text, size, _salt, _rounds);
}

void Cipher::Key::options(const unsigned char *salt, unsigned rounds)
{
    _salt = salt;
    _rounds = rounds;
}

void Cipher::Key::assign(const char *text, size_t size, const unsigned char *salt, unsigned rounds)
{
    if(!algotype || !hashtype)
        return;

    if(!size)
        size = strlen((const char *)text);

    if(!rounds)
        rounds = _rounds;

    if(!salt)
        salt = _salt;

    if(EVP_BytesToKey((const EVP_CIPHER*)algotype, (const EVP_MD*)hashtype, salt, (const unsigned char *)text, size, rounds, keybuf, ivbuf) < (int)keysize)
        keysize = 0;
}

void Cipher::Key::set(const char *cipher, const char *digest)
{
    set(cipher);

    // never use sha0...
    if(eq_case(digest, "sha"))
        digest = "sha1";

    hashtype = EVP_get_digestbyname(digest);
}

void Cipher::Key::set(const char *cipher)
{
    char algoname[64];

    clear();
    String::set(algoname, sizeof(algoname), cipher);
    char *fpart = strchr(algoname, '-');
    char *lpart = strrchr(algoname, '-');

    if(fpart && fpart == lpart)
        strcpy(fpart, fpart + 1);

    algotype = EVP_get_cipherbyname(algoname);

    if(!algotype)
        return;

    keysize = EVP_CIPHER_key_length((const EVP_CIPHER*)algotype);
    blksize = EVP_CIPHER_block_size((const EVP_CIPHER*)algotype);
}


bool Cipher::has(const char *id)
{
    // make sure cipher-bitsize forms without -mode do not fail...
    char algoname[64];
    String::set(algoname, sizeof(algoname), id);
    char *fpart = strchr(algoname, '-');
    char *lpart = strrchr(algoname, '-');

    if(fpart && fpart == lpart)
        strcpy(fpart, fpart + 1);

    return (EVP_get_cipherbyname(algoname) != NULL);
}

void Cipher::push(unsigned char *address, size_t size)
{
}

void Cipher::release(void)
{
    keys.clear();
    if(context) {
        EVP_CIPHER_CTX_cleanup((EVP_CIPHER_CTX*)context);
        delete (EVP_CIPHER_CTX*)context;
        context = NULL;
    }
}

void Cipher::set(key_t key, mode_t mode, unsigned char *address, size_t size)
{
    release();

    bufsize = size;
    bufmode = mode;
    bufaddr = address;

    memcpy(&keys, key, sizeof(keys));
    if(!keys.keysize)
        return;

    context = new EVP_CIPHER_CTX;
    EVP_CIPHER_CTX_init((EVP_CIPHER_CTX *)context);
    EVP_CipherInit_ex((EVP_CIPHER_CTX *)context, (EVP_CIPHER *)keys.algotype, NULL, keys.keybuf, keys.ivbuf, (int)mode);
    EVP_CIPHER_CTX_set_padding((EVP_CIPHER_CTX *)context, 0);
}

size_t Cipher::put(const unsigned char *data, size_t size)
{
    int outlen;
    size_t count = 0;

    if(!bufaddr)
        return 0;

    if(size % keys.iosize())
        return 0;

    while(bufsize && size + bufpos > bufsize) {
        size_t diff = bufsize - bufpos;
        count += put(data, diff);
        data += diff;
        size -= diff;
    }

    if(!EVP_CipherUpdate((EVP_CIPHER_CTX *)context, bufaddr + bufpos, &outlen, data, size)) {
        release();
        return count;
    }
    bufpos += outlen;
    count += outlen;
    if(bufsize && bufpos >= bufsize) {
        push(bufaddr, bufsize);
        bufpos = 0;
    }
    return count;
}

size_t Cipher::pad(const unsigned char *data, size_t size)
{
    size_t padsize = 0;
    unsigned char padbuf[64];
    const unsigned char *ep;

    if(!bufaddr)
        return 0;

    switch(bufmode) {
    case DECRYPT:
        if(size % keys.iosize())
            return 0;
        put(data, size);
        ep = data + size - 1;
        bufpos -= *ep;
        size -= *ep;
        break;
    case ENCRYPT:
        padsize = size % keys.iosize();
        put(data, size - padsize);
        if(padsize) {
            memcpy(padbuf, data + size - padsize, padsize);
            memset(padbuf + padsize, keys.iosize() - padsize, keys.iosize() - padsize);
            size = (size - padsize) + keys.iosize();
        }
        else {
            size += keys.iosize();
            memset(padbuf, keys.iosize(), keys.iosize());
        }

        put((const unsigned char *)padbuf, keys.iosize());
        zerofill(padbuf, sizeof(padbuf));
    }

    flush();
    return size;
}

