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

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/protocols.h>
#include <ucommon/socket.h>
#include <ucommon/timers.h>
#include <ucommon/buffer.h>
#include <ucommon/string.h>
#include <ucommon/shell.h>

using namespace UCOMMON_NAMESPACE;

TCPBuffer::TCPBuffer() :
BufferProtocol()
{
    so = INVALID_SOCKET;
}

TCPBuffer::TCPBuffer(const char *host, const char *service, size_t size) :
BufferProtocol()
{
    so = INVALID_SOCKET;
    open(host, service, size);
}

TCPBuffer::TCPBuffer(const TCPServer *server, size_t size) :
BufferProtocol()
{
    so = INVALID_SOCKET;
    open(server, size);
}

TCPBuffer::~TCPBuffer()
{
    TCPBuffer::close();
}

void TCPBuffer::open(const char *host, const char *service, size_t size)
{
    struct addrinfo *list = Socket::query(host, service, SOCK_STREAM, 0);
    if(!list)
        return;

    so = Socket::create(list, SOCK_STREAM, 0);
    Socket::release(list);
    if(so == INVALID_SOCKET)
        return;

    _buffer(size);
}

void TCPBuffer::open(const TCPServer *server, size_t size)
{
    close();
    so = server->accept();
    if(so == INVALID_SOCKET)
        return;

    _buffer(size);
}

void TCPBuffer::close(void)
{
    if(so == INVALID_SOCKET)
        return;

    BufferProtocol::release();
    Socket::release(so);
    so = INVALID_SOCKET;
}

void TCPBuffer::_buffer(size_t size)
{
    unsigned iobuf = 0;
    unsigned mss = size;
    unsigned max = 0;

#ifdef  TCP_MAXSEG
    socklen_t alen = sizeof(max);
#endif

    if(size < 80) {
        allocate(size);
        return;
    }

#ifdef  TCP_MAXSEG
    if(mss)
        setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&max, sizeof(max));
    getsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&max, &alen);
#endif

    if(max && max < mss)
        mss = max;

    if(!mss) {
        if(max)
            mss = max;
        else
            mss = 536;
        goto alloc;
    }

    if(mss < 80)
        mss = 80;

    if(mss * 7 < 64000u)
        iobuf = mss * 7;
    else if(size * 6 < 64000u)
        iobuf = mss * 6;
    else
        iobuf = mss * 5;

    Socket::sendsize(so, iobuf);
    Socket::recvsize(so, iobuf);

    if(mss < 512)
        Socket::sendwait(so, mss * 4);

alloc:
    allocate(size);
}

int TCPBuffer::_err(void) const
{
    return ioerr;
}

void TCPBuffer::_clear(void)
{
    ioerr = 0;
}

bool TCPBuffer::_blocking(void)
{
    if(iowait)
        return true;

    return false;
}

size_t TCPBuffer::_push(const char *address, size_t len)
{
    if(ioerr)
        return 0;

    ssize_t result = writeto(address, len);
    if(result < 0)
        result = 0;

    return (size_t)result;
}

size_t TCPBuffer::_pull(char *address, size_t len)
{
    ssize_t result;

    result = readfrom(address, len);

    if(result < 0)
        result = 0;
    return (size_t)result;
}

bool TCPBuffer::_pending(void)
{
    if(input_pending())
        return true;

    if(is_input() && iowait && iowait != Timer::inf)
        return Socket::wait(so, iowait);

    return Socket::wait(so, 0);
}
