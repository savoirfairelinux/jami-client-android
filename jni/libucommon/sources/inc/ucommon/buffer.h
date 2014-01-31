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

/**
 * Classes which use the buffer protocol to stream data.
 * @file ucommon/buffer.h
 */

#ifndef _UCOMMON_BUFFER_H_
#define _UCOMMON_BUFFER_H_

#ifndef _UCOMMON_CONFIG_H_
#include <ucommon/platform.h>
#endif

#ifndef _UCOMMON_PROTOCOLS_H_
#include <ucommon/protocols.h>
#endif

#ifndef _UCOMMON_SOCKET_H_
#include <ucommon/socket.h>
#endif

#ifndef _UCOMMON_STRING_H_
#include <ucommon/string.h>
#endif

#ifndef _UCOMMON_FSYS_H_
#include <ucommon/fsys.h>
#endif

#ifndef _UCOMMON_SHELL_H_
#include <ucommon/shell.h>
#endif

NAMESPACE_UCOMMON

/**
 * A generic tcp socket class that offers i/o buffering.  All user i/o
 * operations are directly inherited from the IOBuffer base class public
 * members.  Some additional members are added for layering ssl services.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT TCPBuffer : public BufferProtocol, protected Socket
{
protected:
    void _buffer(size_t size);

    virtual size_t _push(const char *address, size_t size);
    virtual size_t _pull(char *address, size_t size);
    int _err(void) const;
    void _clear(void);
    bool _blocking(void);

    /**
     * Get the low level socket object.
     * @return socket we are using.
     */
    inline socket_t getsocket(void) const
        {return so;};

public:
    /**
     * Construct an unconnected tcp client and specify our service profile.
     */
    TCPBuffer();

    /**
     * Construct a tcp server session from a listening socket.
     * @param server socket we are created from.
     * @param size of buffer and tcp fragments.
     */
    TCPBuffer(const TCPServer *server, size_t size = 536);

    /**
     * Construct a tcp client session connected to a specific host uri.
     * @param host and optional :port we are connecting to.
     * @param service identifier of our client.
     * @param size of buffer and tcp fragments.
     */
    TCPBuffer(const char *host, const char *service, size_t size = 536);

    /**
     * Destroy the tcp socket and release all resources.
     */
    virtual ~TCPBuffer();

    /**
     * Connect a tcp socket to a client from a listener.  If the socket was
     * already connected, it is automatically closed first.
     * @param server we are connected from.
     * @param size of buffer and tcp fragments.
     */
    void open(const TCPServer *server, size_t size = 536);

    /**
     * Connect a tcp client session to a specific host uri.  If the socket
     * was already connected, it is automatically closed first.
     * @param host we are connecting.
     * @param service to connect to.
     * @param size of buffer and tcp fragments.
     */
    void open(const char *host, const char *service, size_t size = 536);

    /**
     * Close active connection.
     */
    void close(void);

protected:
    /**
     * Check for pending tcp or ssl data.
     * @return true if data pending.
     */
    virtual bool _pending(void);
};

/**
 * Convenience type for pure tcp sockets.
 */
typedef TCPBuffer tcp_t;

END_NAMESPACE

#endif
