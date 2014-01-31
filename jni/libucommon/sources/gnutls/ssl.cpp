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

SSLBuffer::SSLBuffer(const TCPServer *tcp, secure::server_t scontext, size_t size) :
TCPBuffer(tcp, size)
{
    ssl = context::session((context *)scontext);
    bio = NULL;
    server = true;

    if(!is_open() || !ssl)
        return;

    gnutls_transport_set_ptr((SSL)ssl, reinterpret_cast<gnutls_transport_ptr_t>( so));
    int result = gnutls_handshake((SSL)ssl);

    if(result >= 0)
        bio = ssl;

}

SSLBuffer::SSLBuffer(secure::client_t scontext) :
TCPBuffer()
{
    ssl = context::session((context *)scontext);
    bio = NULL;
    server = false;
}

SSLBuffer::~SSLBuffer()
{
    release();
}

bool SSLBuffer::_pending(void)
{
    return TCPBuffer::_pending();
}

void SSLBuffer::open(const char *host, const char *service, size_t size)
{
    if(server) {
        ioerr = EBADF;
        return;
    }

    close();

    TCPBuffer::open(host, service, size);

    if(!is_open() || !ssl)
        return;

    gnutls_transport_set_ptr((SSL)ssl, reinterpret_cast<gnutls_transport_ptr_t>(so));
    int result = gnutls_handshake((SSL)ssl);

    if(result >= 0)
        bio = ssl;
}

void SSLBuffer::close(void)
{
    if(server) {
        ioerr = EBADF;
        return;
    }

    if(bio)
        gnutls_bye((SSL)ssl, GNUTLS_SHUT_RDWR);
    bio = NULL;
    TCPBuffer::close();
}

void SSLBuffer::release(void)
{
    server = false;
    close();
    if(ssl) {
        gnutls_deinit((SSL)ssl);
        ssl = NULL;
    }
}

bool SSLBuffer::_flush(void)
{
    return TCPBuffer::_flush();
}

size_t SSLBuffer::_push(const char *address, size_t size)
{
    if(!bio)
        return TCPBuffer::_push(address, size);

    int result = gnutls_record_send((SSL)ssl, address, size);
    if(result < 0) {
        result = 0;
        ioerr = EIO;
    }
    return (size_t)result;
}

size_t SSLBuffer::_pull(char *address, size_t size)
{
    if(!bio)
        return TCPBuffer::_pull(address, size);

    int result = gnutls_record_recv((SSL)ssl, address, size);
    if(result < 0) {
        result = 0;
        ioerr = EIO;
    }
    return (size_t)result;
}

