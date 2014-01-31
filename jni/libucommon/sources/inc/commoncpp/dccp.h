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

/**
 * @file commoncpp/udp.h
 * @short udp derived socket classes.
 **/

#ifndef COMMONCPP_DCCP_H_
#define COMMONCPP_DCCP_H_

#include <cstdio>

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_STRING_H_
#include <commoncpp/string.h>
#endif

#ifndef COMMONCPP_ADDRESS_H_
#include <commoncpp/address.h>
#endif

#ifndef COMMONCPP_SOCKET_H_
#include <commoncpp/socket.h>
#endif

NAMESPACE_COMMONCPP

/**
 * DCCP sockets are used for stream based connected sessions between two
 * sockets.  Both error recovery and flow control operate transparently
 * for a DCCP socket connection.  The DCCP socket base class is used both
 * for client connections and to bind a DCCP "server" for accepting DCCP
 * streams.
 *
 * An implicit and unique DCCPSocket object exists in Common C++ to represent
 * a bound DCCP socket acting as a "server" for receiving connection requests.
 * This class is not part of DCCPStream because such objects normally perform
 * no physical I/O (read or write operations) other than to specify a listen
 * backlog queue and perform "accept" operations for pending connections.
 * The Common C++ DCCPSocket offers a Peek method to examine where the next
 * pending connection is coming from, and a Reject method to flush the next
 * request from the queue without having to create a session.
 *
 * The DCCPSocket also supports a "OnAccept" method which can be called when a
 * DCCPStream related object is created from a DCCPSocket.  By creating a
 * DCCPStream from a DCCPSocket, an accept operation automatically occurs, and
 * the DCCPSocket can then still reject the client connection through the
 * return status of it's OnAccept method.
 *
 * @author Leandro Sales <leandroal@gmail.com>
 * @author Heverton Stuart <hevertonsns@gmail.com>
 * @short bound server for DCCP streams and sessions.
 */
class __EXPORT DCCPSocket : public Socket
{
    union {
        struct sockaddr_in ipv4;
#ifdef  CCXX_IPV6
        struct sockaddr_in6 ipv6;
#endif
    }   peer;

    Family family;

public:
    /**
     * A method to call in a derived DCCPSocket class that is acting
     * as a server when a connection request is being accepted.  The
     * server can implement protocol specific rules to exclude the
     * remote socket from being accepted by returning false.  The
     * Peek method can also be used for this purpose.
     *
     * @return true if client should be accepted.
     * @param ia internet host address of the client.
     * @param port number of the client.
     */
    virtual bool onAccept(const IPV4Host &ia, tpport_t port);
#ifdef  CCXX_IPV6
    virtual bool onAccept(const IPV6Host &ia, tpport_t port);
#endif

    virtual IPV4Host getIPV4Sender(tpport_t *port = NULL) const;

#ifdef  CCXX_IPV6
    virtual IPV6Host getIPV6Sender(tpport_t *port = NULL) const;
#endif

    /**
     * A DCCP "server" is created as a DCCP socket that is bound
     * to a hardware address and port number on the local machine
     * and that has a backlog queue to listen for remote connection
     * requests.  If the server cannot be created, an exception is
     * thrown.
     *
     * @param bind local ip address or interface to use.
     * @param port number to bind socket under.
     * @param backlog size of connection request queue.
     */
    DCCPSocket(const IPV4Address &bind, tpport_t port, unsigned backlog = 5);
#ifdef  CCXX_IPV6
    DCCPSocket(const IPV6Address &bind, tpport_t port, unsigned backlog = 5);
#endif

    /**
     * Create a named dccp socket by service and/or interface id.
     * For IPV4 we use [host:]svc or [host/]svc for the string.
     * If we have getaddrinfo, we use that to obtain the addr to
     * bind for.
     *
     * @param name of host interface and service port to bind.
     * @param backlog size of connection request queue.
     */
    DCCPSocket(const char *name, Family family = IPV4, unsigned backlog = 5);

    /**
     * Create an unconnected ephemeral DCCP client socket.
     */
    DCCPSocket(Family family = IPV4);

    /**
     * Create a server session by accepting a DCCP Socket.
     */
    DCCPSocket(DCCPSocket& server, timeout_t timeout = 0);

    /**
     * Used to reject the next incoming connection request.
     */
    void reject(void);

    /**
     * Disconnect active dccp connection (client use).
     */
    void disconnect(void);

    /**
     * Set CCID DCCP.
     */
    bool setCCID(uint8_t ccid);

    /**
     * Get TX CCID DCCP.
     */
    int getTxCCID();

    /**
     * Get RX CCID DCCP.
     */
    int getRxCCID();

    /**
     * Return number of bytes to be read
     */
    size_t available();

    /**
     * Create a DCCP client connection to a DCCP socket (on
     * a remote machine).
     *
     * @param host address of remote DCCP server.
     * @param port number to connect.
     */
    void connect(const IPV4Host &host, tpport_t port, timeout_t timeout = 0);
#ifdef  CCXX_IPV6
    void connect(const IPV6Host &host, tpport_t port, timeout_t timeout = 0);
#endif

    /**
     * Connect to a named client.
     */
    void connect(const char *name);

    /**
     * Used to wait for pending connection requests.
     * @return true if data packets available.
     * @param timeout in milliseconds. TIMEOUT_INF if not specified.
     */
    inline bool isPendingConnection(timeout_t timeout = TIMEOUT_INF) /* not const -- jfc */
        {return Socket::isPending(Socket::pendingInput, timeout);}

    /**
     * Use base socket handler for ending this socket.
     */
    virtual ~DCCPSocket();
};

END_NAMESPACE

#endif
