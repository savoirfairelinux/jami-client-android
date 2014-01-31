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

int context::map_cipher(const char *cipher)
{
    char algoname[64];

    enum {
        NONE, CBC, ECB, CFB, OFB
    } modeid;

    String::set(algoname, sizeof(algoname), cipher);
    char *fpart = strchr(algoname, '-');
    char *lpart = strrchr(algoname, '-');

    modeid = NONE;

    if(lpart) {
        if(fpart != lpart)
            *(fpart++) = 0;
        else
            ++fpart;

        *(lpart++) = 0;
        if(eq_case(lpart, "cbc"))
            modeid = CBC;
        else if(eq_case(lpart, "ecb"))
            modeid = ECB;
        else if(eq_case(lpart, "cfb") || eq_case(lpart, "pgp"))
            modeid = CFB;
        else if(eq_case(lpart, "ofb"))
            modeid = OFB;
        else
            modeid = NONE;    
    }
    else if(eq_case(cipher, "aes128") || eq_case(cipher, "aes"))
        return GNUTLS_CIPHER_AES_128_CBC;
    else if(eq_case(cipher, "aes256"))
        return GNUTLS_CIPHER_AES_256_CBC;
    else if(eq_case(cipher, "aes192"))
        return GNUTLS_CIPHER_AES_192_CBC;
    else if(eq_case(cipher, "arcfour") || eq_case(cipher, "arc4"))
        return GNUTLS_CIPHER_ARCFOUR_128;
    else if(eq_case(cipher, "des"))
        return GNUTLS_CIPHER_DES_CBC;
    else if(eq_case(cipher, "3des"))
        return GNUTLS_CIPHER_3DES_CBC;
    else if(eq_case(cipher, "rc2"))
        return GNUTLS_CIPHER_RC2_40_CBC;
    else if(eq_case(cipher, "idea"))
        return GNUTLS_CIPHER_IDEA_PGP_CFB;
    else if(eq_case(cipher, "twofish") || eq_case(cipher, "2fish"))
        return GNUTLS_CIPHER_TWOFISH_PGP_CFB;
    else if(eq_case(cipher, "blowfish"))
        return GNUTLS_CIPHER_BLOWFISH_PGP_CFB;

    else if(eq_case(algoname, "cast") || eq_case(algoname, "cast5"))
        return GNUTLS_CIPHER_CAST5_PGP_CFB;

    switch(modeid) {
    case CFB:
        if(eq_case(algoname, "aes")) {
            if(atoi(fpart) == 128)
                return GNUTLS_CIPHER_AES128_PGP_CFB;
            if(atoi(fpart) == 192)
                return GNUTLS_CIPHER_AES192_PGP_CFB;
            if(atoi(fpart) == 256)
                return GNUTLS_CIPHER_AES256_PGP_CFB;
            return 0;
        }

        if(eq_case(algoname, "idea"))
            return GNUTLS_CIPHER_IDEA_PGP_CFB;
        if(eq_case(algoname, "3des"))
            return GNUTLS_CIPHER_3DES_PGP_CFB;
        if(eq_case(algoname, "cast") || eq_case(algoname, "cast5"))
            return GNUTLS_CIPHER_CAST5_PGP_CFB;
        if(eq_case(algoname, "twofish") || eq_case(algoname, "2fish"))
            return GNUTLS_CIPHER_TWOFISH_PGP_CFB;
        if(eq_case(algoname, "blowfish"))
            return GNUTLS_CIPHER_BLOWFISH_PGP_CFB;
        if(eq_case(algoname, "sk"))
            return GNUTLS_CIPHER_SAFER_SK128_PGP_CFB;
        return 0;
    case CBC:
        if(eq_case(algoname, "aes")) {
            if(atoi(fpart) == 128)
                return GNUTLS_CIPHER_AES_128_CBC;
            if(atoi(fpart) == 192)
                return GNUTLS_CIPHER_AES_192_CBC;
            if(atoi(fpart) == 256)
                return GNUTLS_CIPHER_AES_256_CBC;
            return 0;
        }
        if(eq_case(algoname, "camellia")) {
            if(atoi(fpart) == 128)
                return GNUTLS_CIPHER_CAMELLIA_128_CBC;
            if(atoi(fpart) == 256)
                return GNUTLS_CIPHER_CAMELLIA_256_CBC;
            return 0;
        }
        if(eq_case(algoname, "3des"))
            return GNUTLS_CIPHER_3DES_CBC;
        if(eq_case(algoname, "des"))
            return GNUTLS_CIPHER_DES_CBC;
        if(eq_case(algoname, "rc2"))
            return GNUTLS_CIPHER_RC2_40_CBC;
        return 0;
    default:
        if(eq_case(algoname, "arc4") || eq_case(algoname, "arcfour")) {
            if(atoi(fpart) == 40)
                return GNUTLS_CIPHER_ARCFOUR_40;
            if(atoi(fpart) == 128)
                return GNUTLS_CIPHER_ARCFOUR_128;
        }
        return 0;
    }
}

void Cipher::Key::assign(const char *text, size_t size, const unsigned char *salt, unsigned count)
{
    if(!hashid || !algoid) {
        keysize = 0;
        return;
    }

    size_t kpos = 0, ivpos = 0;
    size_t mdlen = gnutls_hash_get_len((MD_ID)hashid);
    size_t tlen = strlen(text);

    if(!hashid || !mdlen) {
        clear();
        return;
    }

    char previous[MAX_DIGEST_HASHSIZE / 8];
    unsigned char temp[MAX_DIGEST_HASHSIZE / 8];
    MD_CTX mdc;

    unsigned prior = 0;
    unsigned loop;

    if(!salt)
        salt = _salt;

    if(!count)
        count = _rounds;

    do {
        gnutls_hash_init(&mdc, (MD_ID)hashid);

        if(prior++)
            gnutls_hash(mdc, previous, mdlen);

        gnutls_hash(mdc, text, tlen);

        if(salt)
            gnutls_hash(mdc, salt, 8);

        gnutls_hash_deinit(mdc, previous);

        for(loop = 1; loop < count; ++loop) {
            memcpy(temp, previous, mdlen);
            gnutls_hash_fast((MD_ID)hashid, temp, mdlen, previous); 
        }

        size_t pos = 0;
        while(kpos < keysize && pos < mdlen)
            keybuf[kpos++] = previous[pos++];
        while(ivpos < blksize && pos < mdlen)
            ivbuf[ivpos++] = previous[pos++];
    } while(kpos < keysize || ivpos < blksize);
}

void Cipher::Key::assign(const char *text, size_t size)
{
    assign(text, size, _salt, _rounds);
}

void Cipher::Key::options(const unsigned char *salt, unsigned rounds)
{
    _salt = salt;
    _rounds = rounds;
}

void Cipher::Key::set(const char *cipher, const char *digest)
{
    set(cipher);

    hashid = context::map_digest(digest);
}

void Cipher::Key::set(const char *cipher)
{
    clear();
    
    algoid = context::map_cipher(cipher);

    if(algoid) {
        blksize = gnutls_cipher_get_block_size((CIPHER_ID)algoid);
        keysize = gnutls_cipher_get_key_size((CIPHER_ID)algoid);
    }
}

void Cipher::push(unsigned char *address, size_t size)
{
}

void Cipher::release(void)
{
    keys.clear();
    if(context) {
        gnutls_cipher_deinit((CIPHER_CTX)context);
        context = NULL;
    }
}

bool Cipher::has(const char *cipher)
{
    return context::map_cipher(cipher) != 0;
}

void Cipher::set(const key_t key, mode_t mode, unsigned char *address, size_t size)
{
    release();

    bufsize = size;
    bufmode = mode;
    bufaddr = address;

    memcpy(&keys, key, sizeof(keys));
    if(!keys.keysize)
        return;

    gnutls_datum_t keyinfo, ivinfo;
    keyinfo.data = keys.keybuf;
    keyinfo.size = keys.keysize;
    ivinfo.data = keys.ivbuf;
    ivinfo.size = keys.blksize;

    gnutls_cipher_init((CIPHER_CTX *)&context, (CIPHER_ID)keys.algoid, &keyinfo, &ivinfo);
}

size_t Cipher::put(const unsigned char *data, size_t size)
{
    if(size % keys.iosize() || !bufaddr)
        return 0;

    size_t count = 0;

    while(bufsize && size + bufpos > bufsize) {
        size_t diff = bufsize - bufpos;
        count += put(data, diff);
        data += diff;
        size -= diff;
    }

    switch(bufmode) {
    case Cipher::ENCRYPT:
        gnutls_cipher_encrypt2((CIPHER_CTX)context, (void *)data, size, bufaddr + bufpos, size);
        break;
    case Cipher::DECRYPT:
        gnutls_cipher_decrypt2((CIPHER_CTX)context, data, size, bufaddr + bufpos, size);
    }

    count += size;
    if(!count) {
        release();
        return 0;
    }
    bufpos += size;
    if(bufsize && bufpos >= bufsize) {
        push(bufaddr, bufsize);
        bufpos = 0;
    }
    return count;
}

size_t Cipher::pad(const unsigned char *data, size_t size)
{
    size_t padsz = 0;
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
        padsz = size % keys.iosize();
        put(data, size - padsz);
        if(padsz) {
            memcpy(padbuf, data + size - padsz, padsz);
            memset(padbuf + padsz, keys.iosize() - padsz, keys.iosize() - padsz);
            size = (size - padsz) + keys.iosize();
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


