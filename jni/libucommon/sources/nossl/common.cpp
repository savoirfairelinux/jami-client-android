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

Digest::Digest()
{
    hashtype = NULL;
    hashid = 0;
    context = NULL;
    bufsize = 0;
    textbuf[0] = 0;
}

Digest::Digest(const char *type)
{
    hashtype = NULL;
    hashid = 0;
    context = NULL;
    bufsize = 0;
    textbuf[0] = 0;

    set(type);
}

Digest::~Digest()
{
    release();
}

const char *Digest::c_str(void)
{
    if(!bufsize)
        get();

    return textbuf;
}

void Digest::uuid(char *str, const char *name, const unsigned char *ns)
{
    unsigned mask = 0x50;
    const char *type = "sha1";
    if(!has("sha1")) {
        mask = 0x30;
        type = "md5";
    }

    Digest md(type);
    if(ns)
        md.put(ns, 16);
    md.puts(name);
    unsigned char *buf = (unsigned char *)md.get();

    buf[6] &= 0x0f;
    buf[6] |= mask;
    buf[8] &= 0x3f;
    buf[8] |= 0x80;

    String::hexdump(buf, str, "4-2-2-2-6");
}

String Digest::uuid(const char *name, const unsigned char *ns)
{
    char buf[38];
    uuid(buf, name, ns);
    return String(buf);
}

#if defined(_MSWINDOWS_)

static void cexport(HCERTSTORE ca, FILE *fp)
{
    PCCERT_CONTEXT cert = NULL;
    const uint8_t *cp;
    char buf[80];

    while ((cert = CertEnumCertificatesInStore(ca, cert)) != NULL) {
        fprintf(fp, "-----BEGIN CERTIFICATE-----\n");
        size_t total = cert->cbCertEncoded;
        size_t count;
        cp = (const uint8_t *)cert->pbCertEncoded;
        while(total) {
            count = String::b64encode(buf, cp, total, 64);
            if(count)
                fprintf(fp, "%s\n", buf);
            total -= count;
            cp += count;
        }
        fprintf(fp, "-----END CERTIFICATE-----\n");
    }
}

const char *secure::oscerts(void)
{
    const char *path = "c:/temp/ca-bundle.crt";
    if(!is_file(path)) {
        if(oscerts(path))
            return NULL;
    }
    return path;
}

int secure::oscerts(const char *pathname)
{
    bool caset;
    string_t target;

    if(pathname[1] == ':' || pathname[0] == '/' || pathname[0] == '\\')
        target = pathname;
    else
        target = shell::path(shell::USER_CONFIG) + "/" + pathname;

    FILE *fp = fopen(*target, "wt");

    if(!fp)
        return ENOSYS;

    HCERTSTORE ca = CertOpenSystemStoreA((HCRYPTPROV)NULL, "ROOT");
    if(ca) {
        caset = true;
        cexport(ca, fp);
        CertCloseStore(ca, 0);
    }

    ca = CertOpenSystemStoreA((HCRYPTPROV)NULL, "CA");
    if(ca) {
        caset = true;
        cexport(ca, fp);
        CertCloseStore(ca, 0);
    }

    fclose(fp);

    if(!caset) {
        fsys::erase(*target);
        return ENOSYS;
    }
    return 0;
}

#else
const char *secure::oscerts(void)
{
    if(is_file("/etc/ssl/certs/ca-certificates.crt"))
        return "/etc/ssl/certs/ca-certificates.crt";

    if(is_file("/etc/pki/tls/ca-bundle.crt"))
        return "/etc/pki/tls/ca-bundle.crt";

    if(is_file("/etc/ssl/ca-bundle.pem"))
        return "/etc/ssl/ca-bundle.pem";

    return NULL;
}

int secure::oscerts(const char *pathname)
{
    string_t source = oscerts();
    string_t target;

    if(pathname[0] == '/')
        target = pathname;
    else
        target = shell::path(shell::USER_CONFIG) + "/" + pathname;

    if(!source)
        return ENOSYS;

    return fsys::copy(*source, *target);
}
#endif

void secure::uuid(char *str)
{
    static unsigned char buf[16];
    static Timer::tick_t prior = 0l;
    static unsigned short seq;
    Timer::tick_t current = Timer::ticks();

    Mutex::protect(&buf);

    // get our (random) node identifier...
    if(!prior)
        Random::fill(buf + 10, 6);

    if(current == prior)
        ++seq;
    else
        Random::fill((unsigned char *)&seq, sizeof(seq));

    buf[8] = (unsigned char)((seq >> 8) & 0xff);
    buf[9] = (unsigned char)(seq & 0xff);
    buf[3] = (unsigned char)(current & 0xff);
    buf[2] = (unsigned char)((current >> 8) & 0xff);
    buf[1] = (unsigned char)((current >> 16) & 0xff);
    buf[0] = (unsigned char)((current >> 24) & 0xff);
    buf[5] = (unsigned char)((current >> 32) & 0xff);
    buf[4] = (unsigned char)((current >> 40) & 0xff);
    buf[7] = (unsigned char)((current >> 48) & 0xff);
    buf[6] = (unsigned char)((current >> 56) & 0xff);

    buf[6] &= 0x0f;
    buf[6] |= 0x10;
    buf[8] |= 0x80;
    String::hexdump(buf, str, "4-2-2-2-6");
    Mutex::release(&buf);
}

String secure::uuid(void)
{
    char buf[38];
    uuid(buf);
    return String(buf);
}

HMAC::HMAC()
{
    hmactype = NULL;
    hmacid = 0;
    context = NULL;
    bufsize = 0;
    textbuf[0] = 0;
}

HMAC::HMAC(const char *digest, const char *key, size_t len)
{
    context = NULL;
    bufsize = 0;
    hmactype = NULL;
    hmacid = 0;
    textbuf[0] = 0;

    set(digest, key, len);
}

HMAC::~HMAC()
{
    release();
}

const char *HMAC::c_str(void)
{
    if(!bufsize)
        get();

    return textbuf;
}

Cipher::Key::Key(const char *cipher)
{
    hashtype = algotype = NULL;
    hashid = algoid = 0;

    secure::init();

    set(cipher);
}

Cipher::Key::Key(const char *cipher, const char *digest)
{
    hashtype = algotype = NULL;
    hashid = algoid = 0;

    secure::init();
    set(cipher, digest);
}

Cipher::Key::Key(const char *cipher, const char *digest, const char *text, size_t size, const unsigned char *salt, unsigned rounds)
{
    hashtype = algotype = NULL;
    hashid = algoid = 0;

    secure::init();

    set(cipher, digest);
    assign(text, size, salt, rounds);
}

Cipher::Key::Key()
{
    secure::init();
    clear();
}

Cipher::Key::~Key()
{
    clear();
}

void Cipher::Key::clear(void)
{
    algotype = NULL;
    hashtype = NULL;
    algoid = hashid = 0;
    keysize = blksize = 0;

    zerofill(keybuf, sizeof(keybuf));
    zerofill(ivbuf, sizeof(ivbuf));
}

Cipher::Cipher(key_t key, mode_t mode, unsigned char *address, size_t size)
{
    bufaddr = NULL;
    bufsize = bufpos = 0;
    context = NULL;
    set(key, mode, address, size);
}

Cipher::Cipher()
{
    bufaddr = NULL;
    bufsize = bufpos = 0;
    context = NULL;
}

Cipher::~Cipher()
{
    flush();
    release();
}

size_t Cipher::flush(void)
{
    size_t total = bufpos;

    if(bufpos && bufsize) {
        push(bufaddr, bufpos);
        bufpos = 0;
    }
    bufaddr = NULL;
    return total;
}

size_t Cipher::puts(const char *text)
{
    char padbuf[64];
    if(!text || !bufaddr)
        return 0;

    size_t len = strlen(text) + 1;
    unsigned pad = len % keys.iosize();

    size_t count = put((const unsigned char *)text, len - pad);
    if(pad) {
        memcpy(padbuf, text + len - pad, pad);
        memset(padbuf + pad, 0, keys.iosize() - pad);
        count += put((const unsigned char *)padbuf, keys.iosize());
        zerofill(padbuf, sizeof(padbuf));
    }
    return flush();
}

void Cipher::set(unsigned char *address, size_t size)
{
    flush();
    bufsize = size;
    bufaddr = address;
    bufpos = 0;
}

size_t Cipher::process(unsigned char *buf, size_t len, bool flag)
{
    set(buf);
    if(flag)
        return pad(buf, len);
    else
        return put(buf, len);
}

int Random::get(void)
{
    uint16_t v;;
    fill((unsigned char *)&v, sizeof(v));
    v /= 2;
    return (int)v;
}

int Random::get(int min, int max)
{
    unsigned rand;
    int range = max - min + 1;
    unsigned umax;

    if(max < min)
        return 0;

    memset(&umax, 0xff, sizeof(umax));

    do {
        fill((unsigned char *)&rand, sizeof(rand));
    } while(rand > umax - (umax % range));

    return min + (rand % range);
}

double Random::real(void)
{
    unsigned umax;
    unsigned rand;

    memset(&umax, 0xff, sizeof(umax));
    fill((unsigned char *)&rand, sizeof(rand));

    return ((double)rand) / ((double)umax);
}

double Random::real(double min, double max)
{
    return real() * (max - min) + min;
}

void Random::uuid(char *str)
{
    unsigned char buf[16];

    fill(buf, sizeof(buf));
    buf[6] &= 0x0f;
    buf[6] |= 0x40;
    buf[8] &= 0x3f;
    buf[8] |= 0x80;
    String::hexdump(buf, str, "4-2-2-2-6");
}

String Random::uuid(void)
{
    char buf[38];
    uuid(buf);
    return String(buf);
}

