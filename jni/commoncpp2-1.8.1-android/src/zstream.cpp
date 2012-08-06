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

#ifdef  HAVE_ZLIB_H

#include <cc++/config.h>
#ifdef  CCXX_WITHOUT_EXTRAS
#include <cc++/export.h>
#endif
#include <cc++/thread.h>
#include <cc++/exception.h>
#ifndef CCXX_WITHOUT_EXTRAS
#include <cc++/export.h>
#endif
#include <cc++/zstream.h>
#include <cerrno>
#include <cstdlib>
#include <cstdarg>
#include <cstdio>

#ifdef  WIN32
#include <io.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

IZStream::IZStream(const char *name, size_t size, bool tf) :
streambuf(),
#ifdef  HAVE_OLD_IOSTREAM
istream(),
#else
istream((streambuf *)this),
#endif
bufsize(0), gbuf(NULL), fp(0)
{
#ifdef  HAVE_OLD_IOSTREAM
    init((streambuf *)this);
#endif

    throwflag = tf;
    fp = ::gzopen(name, "rb");
    if(!fp) {
#ifdef  COMMON_STD_EXCEPTION
        if(Thread::getException() == Thread::throwException && throwflag)
            throw IOZException(String(::gzerror(fp, NULL)));
#endif
        clear(ios::failbit | rdstate());
        return;
    }

    allocate(size);
}

IZStream::IZStream(bool tf) :
streambuf(),
#ifdef  HAVE_OLD_IOSTREAM
istream(),
#else
istream((streambuf *)this),
#endif
bufsize(0), gbuf(NULL), fp(0)
{
#ifdef  HAVE_OLD_IOSTREAM
    init((streambuf *)this);
#endif
    throwflag = tf;
}

IZStream::~IZStream()
{
    close();
}

void IZStream::open(const char *name, size_t size)
{
    if(fp)
        close();

    fp = ::gzopen(name, "rb");
    if(!fp) {
#ifdef  COMMON_STD_EXCEPTION
        if(Thread::getException() == Thread::throwException && throwflag)
            throw IOZException(String(::gzerror(fp, NULL)));
#endif
        clear(ios::failbit | rdstate());
        return;
    }
    allocate(size);
}

void IZStream::close(void)
{
    if(gbuf)
        delete[] gbuf;

    ::gzclose(fp);
    fp = 0;

    gbuf = NULL;
    bufsize = 0;
    clear();
}

void IZStream::allocate(size_t size)
{
    if(size < 2) {
        bufsize = 1;
        gbuf = 0;
        return;
    }

    gbuf = new char[size];
    if(!gbuf)
        return;
    bufsize = size;
    clear();

#if (defined(__GNUC__) && (__GNUC__ < 3)) && !defined(WIN32) && !defined(STLPORT)
    setb(gbuf, gbuf + size, 0);
#endif
    setg(gbuf, gbuf + size, gbuf + size);
}

int IZStream::doallocate()
{
    if(bufsize)
        return 0;

    allocate(1);
    return 1;
}

bool IZStream::isOpen(void)
{
    if(fp)
        return true;

    return false;
}

int IZStream::uflow()
{
    int ret = underflow();

    if (ret == EOF)
        return EOF;

    if (bufsize != 1)
        gbump(1);

    return ret;
}

int IZStream::underflow()
{
    ssize_t rlen = 1;
    unsigned char ch;

    if(bufsize == 1) {
        rlen = ::gzread(fp, &ch, 1);
        if(rlen < 1) {
            if(rlen < 0)
                clear(ios::failbit | rdstate());
            return EOF;
        }
        return ch;
    }

    if(!gptr())
        return EOF;

    if(gptr() < egptr())
        return (unsigned char)*gptr();

    rlen = (ssize_t)((gbuf + bufsize) - eback());
    rlen = ::gzread(fp, eback(), rlen);
    if(rlen < 1) {
        clear(ios::failbit | rdstate());
        return EOF;
    }

    setg(eback(), eback(), eback() + rlen);
    return (unsigned char) *gptr();
}



OZStream::OZStream(const char *name, int level, size_t size, bool tf) :
streambuf(),
#ifdef  HAVE_OLD_IOSTREAM
    ostream(),
#else
    ostream((streambuf *)this),
#endif
    bufsize(0),pbuf(NULL),fp(0) {
    char mode[4];

#ifdef  HAVE_OLD_IOSTREAM
    init((streambuf *)this);
#endif

    strcpy(mode, "wb\0");
    if(level != Z_DEFAULT_COMPRESSION)
        mode[2] = '0' + level;

    throwflag = tf;
    fp = ::gzopen(name, mode);
    if(!fp) {
#ifdef  COMMON_STD_EXCEPTION
        if(Thread::getException() == Thread::throwException && throwflag)
            throw IOZException(String(::gzerror(fp, NULL)));
#endif
        clear(ios::failbit | rdstate());
        return;
    }

    allocate(size);
}

OZStream::OZStream(bool tf) :
streambuf(),
#ifdef  HAVE_OLD_IOSTREAM
ostream(),
#else
ostream((streambuf *)this),
#endif
bufsize(0), pbuf(NULL), fp(0)
{
#ifdef  HAVE_OLD_IOSTREAM
    init((streambuf *)this);
#endif
    throwflag = tf;
}

OZStream::~OZStream()
{
    close();
}

void OZStream::open(const char *name, int level, size_t size)
{
    char mode[4];

    if(fp)
        close();

    strcpy(mode, "wb\0");
    if(level != Z_DEFAULT_COMPRESSION)
        mode[2] = '0' + level;

    fp = ::gzopen(name, mode);
    if(!fp) {
#ifdef  COMMON_STD_EXCEPTION
        if(Thread::getException() == Thread::throwException && throwflag)
            throw IOZException(String(::gzerror(fp, NULL)));
#endif
        clear(ios::failbit | rdstate());
        return;
    }
    allocate(size);
}

void OZStream::close(void)
{
    if(pbuf)
        delete[] pbuf;

    ::gzclose(fp);
    fp = 0;

    pbuf = NULL;
    bufsize = 0;
    clear();
}

void OZStream::allocate(size_t size)
{
    if(size < 2) {
        bufsize = 1;
        pbuf = 0;
        return;
    }

    pbuf = new char[size];
    if(!pbuf)
        return;
    bufsize = size;
    clear();

    setp(pbuf, pbuf + size);
}

int OZStream::doallocate()
{
    if(bufsize)
        return 0;

    allocate(1);
    return 1;
}

bool OZStream::isOpen(void)
{
    if(fp)
        return true;

    return false;
}

int OZStream::overflow(int c)
{
    unsigned char ch;
    ssize_t rlen, req;

    if(bufsize == 1) {
        if(c == EOF)
            return 0;

        ch = (unsigned char)(c);
        rlen = ::gzwrite(fp, &ch, 1);
        if(rlen < 1) {
            if(rlen < 0)
                clear(ios::failbit | rdstate());
            return EOF;
        }
        else
            return c;
    }

    if(!pbase())
        return EOF;

    req = (ssize_t)(pptr() - pbase());
    if(req) {
        rlen = ::gzwrite(fp, pbase(), req);
        if(rlen < 1) {
            if(rlen < 0)
                clear(ios::failbit | rdstate());
            return EOF;
        }
        req -= rlen;
    }

    // if write "partial", rebuffer remainder

    if(req)
        memmove(pbuf, pbuf + rlen, req);
    setp(pbuf, pbuf + bufsize);
    pbump(req);

    if(c != EOF) {
        *pptr() = (unsigned char)c;
        pbump(1);
    }
    return c;
}


#ifdef  CCXX_NAMESPACES
}
#endif

#endif
