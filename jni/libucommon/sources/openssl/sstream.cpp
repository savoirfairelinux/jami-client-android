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

#if defined(OLD_STDCPP) || defined(NEW_STDCPP)

sstream::sstream(secure::client_t scontext) :
tcpstream()
{
    context *ctx = (context *)scontext;
    ssl = NULL;
    bio = NULL;
    server = false;

    if(ctx && ctx->ctx && ctx->err() == secure::OK)
        ssl = SSL_new(ctx->ctx);
}

sstream::sstream(const TCPServer *tcp, secure::server_t scontext, size_t size) :
tcpstream(tcp, size)
{
    context *ctx = (context *)scontext;
    ssl = NULL;
    bio = NULL;
    server = true;

    if(ctx && ctx->ctx && ctx->err() == secure::OK)
        ssl = SSL_new(ctx->ctx);

    if(!is_open() || !ssl)
        return;

    SSL_set_fd((SSL *)ssl, getsocket());

    if(SSL_accept((SSL *)ssl) > 0)
        bio = SSL_get_wbio((SSL *)ssl);
}

sstream::~sstream()
{
    release();
}

void sstream::open(const char *host, const char *service, size_t size)
{
    if(server)
        return;

    close();
    tcpstream::open(host, service, size);

    if(!is_open() || !ssl)
        return;

    SSL_set_fd((SSL *)ssl, getsocket());

    if(SSL_connect((SSL *)ssl) > 0)
        bio = SSL_get_wbio((SSL *)ssl);
}

void sstream::close(void)
{
    if(server)
        return;

    if(bio) {
        SSL_shutdown((SSL *)ssl);
        bio = NULL;
    }

    tcpstream::close();
}

void sstream::release(void)
{
    server = false;
    close();

    if(ssl) {
        SSL_free((SSL *)ssl);
        ssl = NULL;
    }
}

ssize_t sstream::_write(const char *address, size_t size)
{
    if(!bio)
        return tcpstream::_write(address, size);

    return SSL_write((SSL *)ssl, address, size);
}

ssize_t sstream::_read(char *address, size_t size)
{
    if(!bio)
        return tcpstream::_read(address, size);

    return SSL_read((SSL *)ssl, address, size);
}

bool sstream::_wait(void)
{
    if(so == INVALID_SOCKET)
        return false;

    if(ssl && SSL_pending((SSL *)ssl))
        return true;

    return tcpstream::_wait();
}

int sstream::sync()
{
    int rtn = tcpstream::sync();
    if(bio)
        rtn = BIO_flush((BIO *)bio);

    return rtn;
}

#endif
