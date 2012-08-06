// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// Common C++.  If you copy code from other releases into a copy of GNU
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

#include <cc++/config.h>
#include <cc++/exception.h>
#include <cc++/thread.h>
#include <cc++/export.h>
#include <cc++/digest.h>
#include <cstdio>

#ifdef  WIN32
#include <io.h>
#endif

#define S11 7
#define S12 12
#define S13 17
#define S14 22
#define S21 5
#define S22 9
#define S23 14
#define S24 20
#define S31 4
#define S32 11
#define S33 16
#define S34 23
#define S41 6
#define S42 10
#define S43 15
#define S44 21

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

static inline unsigned long rotate_left(unsigned long x, unsigned long n)
{
    // is unsigned long > 32 bit mask
#if ~0lu != 0xfffffffflu
    return (x << n) | ((x & 0xffffffffu) >> (32-n));
#else
    return (x << n) | (x >> (32-n));
#endif
}

static inline unsigned long F(unsigned long x, unsigned long y, unsigned long z)
{
    return (x & y) | (~x & z);
}

static inline unsigned long G(unsigned long x, unsigned long y, unsigned long z)
{
    return (x & z) | (y & ~z);
}

static inline unsigned long H(unsigned long x, unsigned long y, unsigned long z)
{
    return x ^ y ^ z;
}

inline unsigned long I(unsigned long x, unsigned long y, unsigned long z)
{
    return y ^ (x | ~z);
}


static void FF(unsigned long &a, unsigned long b, unsigned long c, unsigned long d, unsigned long x, unsigned long s, unsigned long ac)
{
    a += F(b, c, d) + x + ac;
    a = rotate_left(a, s) + b;
}

static void GG(unsigned long &a, unsigned long b, unsigned long c, unsigned long d, unsigned long x, unsigned long s, unsigned long ac)
{
    a += G(b, c, d) + x + ac;
    a = rotate_left(a, s) + b;
}

static void HH(unsigned long &a, unsigned long b, unsigned long c, unsigned long d, unsigned long x, unsigned long s, unsigned long ac)
{
    a += H(b, c, d) + x + ac;
    a = rotate_left(a, s) + b;
}

static void II(unsigned long &a, unsigned long b, unsigned long c, unsigned long d, unsigned long x, unsigned long s, unsigned long ac)
{
    a += I(b, c, d) + x + ac;
    a = rotate_left(a, s) + b;
}

MD5Digest::MD5Digest() :
Digest()
{
    initDigest();
    updated = true;
}

void MD5Digest::initDigest(void)
{
    count[0] = count[1] = 0;
    state[0] = 0x67452301;
    state[1] = 0xefcdab89;
    state[2] = 0x98badcfe;
    state[3] = 0x10325476;
    bpos = 0;
    updated = true; // CCY Added
    setp((char*)buf,(char*)buf+64);
}

int MD5Digest::overflow(int c)
{
    updated = true;
    bpos = (unsigned)((unsigned char*)pptr() - buf);
    if(bpos >= 64)
        update();
    if (c != EOF)
        buf[bpos++] = (unsigned char)c;
    setp((char*)buf + bpos,(char*)buf + 64);

    return c;
}

void MD5Digest::update(void)
{
    unsigned long x[16], a, b, c, d;
    int i;

    if(!bpos)
        return;

    while(bpos < 64)
        buf[bpos++] = 0;
    bpos = 0;

    if((count[0] += 512) < 512)
        ++count[1];

    a = state[0];
    b = state[1];
    c = state[2];
    d = state[3];

    for(i = 0; i < 16; ++i)
        x[i] = (unsigned long)(buf[i * 4]) |
            (unsigned long)(buf[i * 4 + 1] << 8) |
            (unsigned long)(buf[i * 4 + 2] << 16) |
            (unsigned long)(buf[i * 4 + 3] << 24);

    FF(a, b, c, d, x[ 0], S11, 0xd76aa478);
    FF(d, a, b, c, x[ 1], S12, 0xe8c7b756);
    FF(c, d, a, b, x[ 2], S13, 0x242070db);
    FF(b, c, d, a, x[ 3], S14, 0xc1bdceee);
    FF(a, b, c, d, x[ 4], S11, 0xf57c0faf);
    FF(d, a, b, c, x[ 5], S12, 0x4787c62a);
    FF(c, d, a, b, x[ 6], S13, 0xa8304613);
    FF(b, c, d, a, x[ 7], S14, 0xfd469501);
    FF(a, b, c, d, x[ 8], S11, 0x698098d8);
    FF(d, a, b, c, x[ 9], S12, 0x8b44f7af);
    FF(c, d, a, b, x[10], S13, 0xffff5bb1);
    FF(b, c, d, a, x[11], S14, 0x895cd7be);
    FF(a, b, c, d, x[12], S11, 0x6b901122);
    FF(d, a, b, c, x[13], S12, 0xfd987193);
    FF(c, d, a, b, x[14], S13, 0xa679438e);
    FF(b, c, d, a, x[15], S14, 0x49b40821);

    GG(a, b, c, d, x[ 1], S21, 0xf61e2562);
    GG(d, a, b, c, x[ 6], S22, 0xc040b340);
    GG(c, d, a, b, x[11], S23, 0x265e5a51);
    GG(b, c, d, a, x[ 0], S24, 0xe9b6c7aa);
    GG(a, b, c, d, x[ 5], S21, 0xd62f105d);
    GG(d, a, b, c, x[10], S22,  0x2441453);
    GG(c, d, a, b, x[15], S23, 0xd8a1e681);
    GG(b, c, d, a, x[ 4], S24, 0xe7d3fbc8);
    GG(a, b, c, d, x[ 9], S21, 0x21e1cde6);
    GG(d, a, b, c, x[14], S22, 0xc33707d6);
    GG(c, d, a, b, x[ 3], S23, 0xf4d50d87);
    GG(b, c, d, a, x[ 8], S24, 0x455a14ed);
    GG(a, b, c, d, x[13], S21, 0xa9e3e905);
    GG(d, a, b, c, x[ 2], S22, 0xfcefa3f8);
    GG(c, d, a, b, x[ 7], S23, 0x676f02d9);
    GG(b, c, d, a, x[12], S24, 0x8d2a4c8a);

    HH(a, b, c, d, x[ 5], S31, 0xfffa3942);
    HH(d, a, b, c, x[ 8], S32, 0x8771f681);
    HH(c, d, a, b, x[11], S33, 0x6d9d6122);
    HH(b, c, d, a, x[14], S34, 0xfde5380c);
    HH(a, b, c, d, x[ 1], S31, 0xa4beea44);
    HH(d, a, b, c, x[ 4], S32, 0x4bdecfa9);
    HH(c, d, a, b, x[ 7], S33, 0xf6bb4b60);
    HH(b, c, d, a, x[10], S34, 0xbebfbc70);
    HH(a, b, c, d, x[13], S31, 0x289b7ec6);
    HH(d, a, b, c, x[ 0], S32, 0xeaa127fa);
    HH(c, d, a, b, x[ 3], S33, 0xd4ef3085);
    HH(b, c, d, a, x[ 6], S34,  0x4881d05);
    HH(a, b, c, d, x[ 9], S31, 0xd9d4d039);
    HH(d, a, b, c, x[12], S32, 0xe6db99e5);
    HH(c, d, a, b, x[15], S33, 0x1fa27cf8);
    HH(b, c, d, a, x[ 2], S34, 0xc4ac5665);

    II(a, b, c, d, x[ 0], S41, 0xf4292244);
    II(d, a, b, c, x[ 7], S42, 0x432aff97);
    II(c, d, a, b, x[14], S43, 0xab9423a7);
    II(b, c, d, a, x[ 5], S44, 0xfc93a039);
    II(a, b, c, d, x[12], S41, 0x655b59c3);
    II(d, a, b, c, x[ 3], S42, 0x8f0ccc92);
    II(c, d, a, b, x[10], S43, 0xffeff47d);
    II(b, c, d, a, x[ 1], S44, 0x85845dd1);
    II(a, b, c, d, x[ 8], S41, 0x6fa87e4f);
    II(d, a, b, c, x[15], S42, 0xfe2ce6e0);
    II(c, d, a, b, x[ 6], S43, 0xa3014314);
    II(b, c, d, a, x[13], S44, 0x4e0811a1);
    II(a, b, c, d, x[ 4], S41, 0xf7537e82);
    II(d, a, b, c, x[11], S42, 0xbd3af235);
    II(c, d, a, b, x[ 2], S43, 0x2ad7d2bb);
    II(b, c, d, a, x[ 9], S44, 0xeb86d391);

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
    updated = true;
}

void MD5Digest::commit(void)
{
    unsigned char cbuf[8];
    unsigned long i, len;

    static unsigned char pad[64]={
        0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    bpos = (unsigned)((unsigned char*)pptr() - buf);
    if(!updated && !bpos)
        return;

    count[0] += (unsigned long)(bpos << 3);
    if(count[0] < (unsigned long)(bpos << 3))
        ++count[1];

    for(i = 0; i < 2; ++i) {
        cbuf[i * 4] = (unsigned char)count[i] & 0xff;
        cbuf[i * 4 + 1] = (unsigned char)((count[i] >> 8) & 0xff);
        cbuf[i * 4 + 2] = (unsigned char)((count[i] >> 16) & 0xff);
        cbuf[i * 4 + 3] = (unsigned char)((count[i] >> 24) & 0xff);
    }

    i = (unsigned) ((count[0] >> 3) & 0x3f);
    len = (i < 56) ? (56 - i) : (120 - i);
    if(len)
        putDigest(pad, len);

    putDigest(cbuf, 8);

    for(i = 0; i < 4; ++i) {
        md5[i * 4] = (unsigned char)state[i] & 0xff;
        md5[i * 4 + 1] = (unsigned char)((state[i] >> 8) & 0xff);
        md5[i * 4 + 2] = (unsigned char)((state[i] >> 16) & 0xff);
        md5[i * 4 + 3] = (unsigned char)((state[i] >> 24) & 0xff);
    }
    initDigest();
}

unsigned MD5Digest::getDigest(unsigned char *buffer)
{
    commit();

    memcpy(buffer, md5, 16);
    return 16;
}

void MD5Digest::putDigest(const unsigned char *buffer, unsigned len)
{
    bpos = (unsigned)((unsigned char*)pptr()-buf);
    if(bpos >= 64)
        update();
    while(len--) {
        buf[bpos++] = *(buffer++);
        if(bpos >= 64)
            update();
    }
    setp((char*)buf+bpos,(char*)buf+64);
}

ostream &MD5Digest::strDigest(ostream &os)
{
    char dbuf[36];
    int i;

    commit();

    for(i = 0; i < 16; ++i)
        sprintf(dbuf + 2 * i, "%02x", md5[i]);
    os << dbuf;
    return os;
}

#ifdef  CCXX_NAMESPACES
}
#endif

