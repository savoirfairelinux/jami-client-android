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
 * @file commoncpp/tcp.h
 * @short tcp derived socket classes.
 **/

#ifndef COMMONCPP_TCP_H_
#define COMMONCPP_TCP_H_

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
 * TCP sockets are used for stream based connected sessions between two
 * sockets.  Both error recovery and flow control operate transparently
 * for a TCP socket connection.  The TCP socket base class is primary used
 * to bind a TCP "server" for accepting TCP streams.
 *
 * An implicit and unique TCPSocket object exists in Common C++ to represent
 * a bound TCP socket acting as a "server" for receiving connection requests.
 * This class is not part of TCPStream because such objects normally perform
 * no physical I/O (read or write operations) other than to specify a listen
 * backlog queue and perform "accept" operations for pending connections.
 * The Common C++ TCPSocket offers a Peek method to examine where the next
 * pending connection is coming from, and a Reject method to flush the next
 * request from the queue without having to create a session.
 *
 * The TCPSocket also supports a "OnAccept" method which can be called when a
 * TCPStream related object is created from a TCPSocket.  By creating a
 * TCPStream from a TCPSocket, an accept operation automatically occurs, and
 * the TCPSocket can then still reject the client connection through the
 * return status of it's OnAccept method.
 *
 * @author David Sugar <dyfet@tycho.com>
 * @short bound server for TCP streams and sessions.
 */
class __EXPORT TCPSocket : protected Socket
{
protected:
    int segsize;
    void setSegmentSize(unsigned mss);

public:
    /**
     * A method to call in a derived TCPSocket class that is acting
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

    /**
     * Fetch out the socket.
     */
    inline SOCKET getSocket(void)
        {return so;};

    /**
     * Get the buffer size for servers.
     */
    inline int getSegmentSize(void)
        {return segsize;};

    /**
     * A TCP "server" is created as a TCP socket that is bound
     * to a hardware address and port number on the local machine
     * and that has a backlog queue to listen for remote connection
     * requests.  If the server cannot be created, an exception is
     * thrown.
     *
     * @param bind local ip address or interface to use.
     * @param port number to bind socket under.
     * @param backlog size of connection request queue.
     * @param mss maximum segment size for accepted streams.
     */
    TCPSocket(const IPV4Address &bind, tpport_t port, unsigned backlog = 5, unsigned mss = 536);

    /**
     * Create a named tcp socket by service and/or interface id.
     * For IPV4 we use [host:]svc or [host/]svc for the string.
     * If we have getaddrinfo, we use that to obtain the addr to
     * bind for.
     *
     * @param name of host interface and service port to bind.
     * @param backlog size of connection request queue.
     * @param mss maximum segment size for streaming buffers.
     */
    TCPSocket(const char *name, unsigned backlog = 5, unsigned mss = 536);

    /**
     * Return address and port of next connection request.  This
     * can be used instead of OnAccept() to pre-evaluate connection
     * requests.
     *
     * @return host requesting a connection.
     * @param port number of requestor.
     */
    inline IPV4Host getRequest(tpport_t *port = NULL) const
        {return Socket::getIPV4Sender(port);}

    /**
     * Used to reject the next incoming connection request.
     */
    void reject(void);

    /**
     * Used to get local bound address.
     */
    inline IPV4Host getLocal(tpport_t *port = NULL) const
        {return Socket::getIPV4Local(port);}

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
    virtual ~TCPSocket();
};

#ifdef  CCXX_IPV6
/**
 * TCPV6 sockets are used for stream based connected sessions between two
 * ipv6 sockets.  Both error recovery and flow control operate transparently
 * for a TCP socket connection.  The TCP socket base class is primary used
 * to bind a TCP "server" for accepting TCP streams.
 *
 * An implicit and unique TCPV6Socket object exists in Common C++ to represent
 * a bound ipv6 TCP socket acting as a "server" for receiving connection requests.
 * This class is not part of TCPStream because such objects normally perform
 * no physical I/O (read or write operations) other than to specify a listen
 * backlog queue and perform "accept" operations for pending connections.
 * The Common C++ TCPV6Socket offers a Peek method to examine where the next
 * pending connection is coming from, and a Reject method to flush the next
 * request from the queue without having to create a session.
 *
 * The TCPV6Socket also supports a "OnAccept" method which can be called when a
 * TCPStream related object is created from a TCPSocket.  By creating a
 * TCPStream from a TCPV6Socket, an accept operation automatically occurs, and
 * the TCPV6Socket can then still reject the client connection through the
 * return status of it's OnAccept method.
 *
 * @author David Sugar <dyfet@tycho.com>
 * @short bound server for TCP streams and sessions.
 */
class __EXPORT TCPV6Socket : protected Socket
{
private:
    int segsize;
    void setSegmentSize(unsigned mss);

public:
    /**
     * A method to call in a derived TCPSocket class that is acting
     * as a server when a connection request is being accepted.  The
     * server can implement protocol specific rules to exclude the
     * remote socket from being accepted by returning false.  The
     * Peek method can also be used for this purpose.
     *
     * @return true if client should be accepted.
     * @param ia internet host address of the client.
     * @param port number of the client.
     */
    virtual bool onAccept(const IPV6Host &ia, tpport_t port);

    /**
     * Fetch out the socket.
     */
    inline SOCKET getSocket(void)
        {return so;};

    inline int getSegmentSize(void)
        {return segsize;};

    /**
     * A TCP "server" is created as a TCP socket that is bound
     * to a hardware address and port number on the local machine
     * and that has a backlog queue to listen for remote connection
     * requests.  If the server cannot be created, an exception is
     * thrown.
     *
     * @param bind local ip address or interface to use.
     * @param port number to bind socket under.
     * @param backlog size of connection request queue.
     * @param mss maximum segment size of streaming buffer.
     */
    TCPV6Socket(const IPV6Address &bind, tpport_t port, unsigned backlog = 5, unsigned mss = 536);

    /**
     * Create a TCP server for a named host interface and service
     * port.  We use [host/]port for specifying the optional host
     * name and service port since ':' is a valid char for ipv6
     * addresses.
     *
     * @param name of host interface and service to use.
     * @param backlog size of connection request queue.
     * @param mss maximum segment size of streaming buffers.
     */
    TCPV6Socket(const char *name, unsigned backlog = 5, unsigned mss = 536);

    /**
     * Return address and port of next connection request.  This
     * can be used instead of OnAccept() to pre-evaluate connection
     * requests.
     *
     * @return host requesting a connection.
     * @param port number of requestor.
     */
    inline IPV6Host getRequest(tpport_t *port = NULL) const
        {return Socket::getIPV6Sender(port);}

    /**
     * Used to reject the next incoming connection request.
     */
    void reject(void);

    /**
     * Used to get local bound address.
     */
    inline IPV6Host getLocal(tpport_t *port = NULL) const
        {return Socket::getIPV6Local(port);}

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
    virtual ~TCPV6Socket();
};
#endif

/**
 * TCP streams are used to represent TCP client connections to a server
 * by TCP protocol servers for accepting client connections.  The TCP
 * stream is a C++ "stream" class, and can accept streaming of data to
 * and from other C++ objects using the << and >> operators.
 *
 *  TCPStream itself can be formed either by connecting to a bound network
 *  address of a TCP server, or can be created when "accepting" a
 *  network connection from a TCP server.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short streamable TCP socket connection.
 */
class __EXPORT TCPStream : protected std::streambuf, public Socket, public std::iostream
{
private:
    int doallocate();

    void segmentBuffering(unsigned mss);

    friend TCPStream& crlf(TCPStream&);
    friend TCPStream& lfcr(TCPStream&);

    // no copy constructor...
    TCPStream(const TCPStream &source);


protected:
    timeout_t timeout;
    size_t bufsize;
    Family family;
    char *gbuf, *pbuf;

public:
    /**
     * The constructor required for building other classes or to
     * start an unconnected TCPStream for connect.
     */
    TCPStream(Family family = IPV4, bool throwflag = true, timeout_t to = 0);

    /**
     * Disconnect the current session and prepare for a new one.
     */
    void disconnect(void);

    /**
     * Get protocol segment size.
     */
    int getSegmentSize(void);

protected:
    /**
     * Used to allocate the buffer space needed for iostream
     * operations.  This function is called by the constructor.
     *
     * @param size of stream buffers from constructor.
     */
    void allocate(size_t size);

    /**
     * Used to terminate the buffer space and cleanup the socket
     * connection.  This fucntion is called by the destructor.
     */
    void endStream(void);

    /**
     * This streambuf method is used to load the input buffer
     * through the established tcp socket connection.
     *
     * @return char from get buffer, EOF if not connected.
     */
    int underflow();

    /**
     * This streambuf method is used for doing unbuffered reads
     * through the establish tcp socket connection when in interactive mode.
     * Also this method will handle proper use of buffers if not in
     * interative mode.
     *
     * @return char from tcp socket connection, EOF if not connected.
     */
    int uflow();

    /**
     * This streambuf method is used to write the output
     * buffer through the established tcp connection.
     *
     * @param ch char to push through.
     * @return char pushed through.
     */
    int overflow(int ch);

    /**
     * Create a TCP stream by connecting to a TCP socket (on
     * a remote machine).
     *
     * @param host address of remote TCP server.
     * @param port number to connect.
     * @param mss maximum segment size of streaming buffers.
     */
    void connect(const IPV4Host &host, tpport_t port, unsigned mss = 536);
#ifdef  CCXX_IPV6
    void connect(const IPV6Host &host, tpport_t port, unsigned mss = 536);
#endif

    /**
     * Connect a TCP stream to a named destination host and port
     * number, using getaddrinfo interface if available.
     *
     * @param name of host and service to connect
     * @param mss maximum segment size of stream buffer
     */
    void connect(const char *name, unsigned mss = 536);

    /**
     * Used in derived classes to refer to the current object via
     * it's iostream.  For example, to send a set of characters
     * in a derived method, one might use *tcp() << "test".
     *
     * @return stream pointer of this object.
     */
    std::iostream *tcp(void)
        {return ((std::iostream *)this);};

public:
    /**
     * Create a TCP stream by accepting a connection from a bound
     * TCP socket acting as a server.  This performs an "accept"
     * call.
     *
     * @param server socket listening
     * @param throwflag flag to throw errors.
     * @param timeout for all operations.
     */
    TCPStream(TCPSocket &server, bool throwflag = true, timeout_t timeout = 0);
#ifdef  CCXX_IPV6
    TCPStream(TCPV6Socket &server, bool throwflag = true, timeout_t timeout = 0);
#endif

    /**
     * Accept a connection from a TCP Server.
     *
     * @param server socket listening
     */
    void connect(TCPSocket &server);
#ifdef  CCXX_IPV6
    void connect(TCPV6Socket &server);
#endif

    /**
     * Create a TCP stream by connecting to a TCP socket (on
     * a remote machine).
     *
     * @param host address of remote TCP server.
     * @param port number to connect.
     * @param mss maximum segment size of streaming buffers.
     * @param throwflag flag to throw errors.
     * @param timeout for all operations.
     */
    TCPStream(const IPV4Host &host, tpport_t port, unsigned mss = 536, bool throwflag = true, timeout_t timeout = 0);
#ifdef  CCXX_IPV6
    TCPStream(const IPV6Host &host, tpport_t port, unsigned mss = 536, bool throwflag = true, timeout_t timeout = 0);
#endif

    /**
     * Construct a named TCP Socket connected to a remote machine.
     *
     * @param name of remote service.
     * @param family of protocol.
     * @param mss maximum segment size of streaming buffers.
     * @param throwflag flag to throw errors.
     * @param timer for timeout for all operations.
     */
    TCPStream(const char *name, Family family = IPV4, unsigned mss = 536, bool throwflag = false, timeout_t timer = 0);

    /**
     * Set the I/O operation timeout for socket I/O operations.
     *
     * @param timer to change timeout.
     */
    inline void setTimeout(timeout_t timer)
        {timeout = timer;};


    /**
     * Flush and empty all buffers, and then remove the allocated
     * buffers.
     */
    virtual ~TCPStream();

    /**
     * Flushes the stream input and output buffers, writes
     * pending output.
     *
     * @return 0 on success.
     */
    int sync(void);

    /**
     * Print content into a socket.
     *
     * @return count of bytes sent.
     * @param format string
     */
    size_t printf(const char *format, ...);

    /**
     * Get the status of pending stream data.  This can be used to
     * examine if input or output is waiting, or if an error or
     * disconnect has occured on the stream.  If a read buffer
     * contains data then input is ready and if write buffer
     * contains data it is first flushed and then checked.
     */
    bool isPending(Pending pend, timeout_t timeout = TIMEOUT_INF);

    /**
      * Examine contents of next waiting packet.
      *
      * @param buf pointer to packet buffer for contents.
      * @param len of packet buffer.
      * @return number of bytes examined.
      */
     inline ssize_t peek(void *buf, size_t len)
         {return ::recv(so, (char *)buf, len, MSG_PEEK);};

    /**
     * Return the size of the current stream buffering used.
     *
     * @return size of stream buffers.
     */
    inline size_t getBufferSize(void) const
        {return bufsize;};
};

/**
 * The TCP session is used to primarily to represent a client connection
 * that can be managed on a seperate thread.  The TCP session also supports
 * a non-blocking connection scheme which prevents blocking during the
 * constructor and moving the process of completing a connection into the
 * thread that executes for the session.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Threaded streamable socket with non-blocking constructor.
 */
class __EXPORT TCPSession : public Thread, public TCPStream
{
private:
    TCPSession(const TCPSession &rhs); // not defined
protected:
    /**
     * Normally called during the thread Initial() method by default,
     * this will wait for the socket connection to complete when
     * connecting to a remote socket.  One might wish to use
     * setCompletion() to change the socket back to blocking I/O
     * calls after the connection completes.  To implement the
     * session one must create a derived class which implements
     * run().
     *
     * @return 0 if successful, -1 if timed out.
     * @param timeout to wait for completion in milliseconds.
     */
    int waitConnection(timeout_t timeout = TIMEOUT_INF);

    /**
     * The initial method is used to esablish a connection when
     * delayed completion is used.  This assures the constructor
     * terminates without having to wait for a connection request
     * to complete.
     */
    void initial(void);

public:
    /**
     * Create a TCP socket that will be connected to a remote TCP
     * server and that will execute under it's own thread.
     *
     * @param host internet address of remote TCP server.
     * @param port number of remote server.
     * @param size of streaming buffer.
     * @param pri execution priority relative to parent.
     * @param stack allocation needed on some platforms.
     */
    TCPSession(const IPV4Host &host,
        tpport_t port, size_t size = 536, int pri = 0, size_t stack = 0);
#ifdef  CCXX_IPV6
    TCPSession(const IPV6Host &host,
        tpport_t port, size_t size = 536, int pri = 0, size_t stack = 0);
#endif

    /**
     * Create a TCP socket from a bound TCP server by accepting a pending
     * connection from that server and execute a thread for the accepted
     * connection.
     *
     * @param server tcp socket to accept a connection from.
     * @param pri execution priority relative to parent.
     * @param stack allocation needed on some platforms.
     */
    TCPSession(TCPSocket &server, int pri = 0, size_t stack = 0);
#ifdef  CCXX_IPV6
    TCPSession(TCPV6Socket &server, int pri = 0, size_t stack = 0);
#endif

    /**
     * Make sure destruction happens through a virtual...
     */
    virtual ~TCPSession();
};

END_NAMESPACE

#endif
