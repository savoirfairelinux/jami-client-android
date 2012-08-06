// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2009 Leandro Melo de Sales <leandroal@gmail.com>
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks,
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
 * @file socket.h
 * @short Network addresses and sockets related classes.
 **/

#ifndef CCXX_SOCKET_H_
#define CCXX_SOCKET_H_

#ifndef CCXX_ADDRESS_H_
#include <cc++/address.h>
#endif

#if defined(WIN32) && !defined(__CYGWIN32__)
#include <io.h>
#define _IOLEN64    (unsigned)
#define _IORET64    (int)
#define TIMEOUT_INF ~((timeout_t) 0)
typedef int socklen_t;
#else
#define INVALID_SOCKET  -1
typedef int SOCKET;
#endif

#ifndef _IOLEN64
#define _IOLEN64
#endif

#ifndef _IORET64
#define _IORET64
#endif

#ifndef MSG_DONTWAIT
#define MSG_DONTWAIT    0
#endif

#ifndef MSG_NOSIGNAL
#define MSG_NOSIGNAL    0
#endif

#ifndef SOCK_DCCP
#define SOCK_DCCP       6
#endif
#ifndef IPPROTO_DCCP
#define IPPROTO_DCCP    33
#endif
#ifndef SOL_DCCP
#define SOL_DCCP       269
#endif
#define DCCP_SOCKOPT_AVAILABLE_CCIDS    12
#define DCCP_SOCKOPT_CCID               13
#define DCCP_SOCKOPT_TX_CCID            14
#define DCCP_SOCKOPT_RX_CCID            15

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

/**
 * Transport Protocol Ports.
 */
typedef unsigned short tpport_t;

/**
 * The Socket is used as the base for all Internet protocol services
 * under Common C++.  A socket is a system resource (or winsock descriptor)
 * that occupies a specific port address (and may be bound to a specific
 * network interface) on the local machine.  The socket may also be
 * directly connected to a specific socket on a remote internet host.
 *
 * This base class is not directly used, but is
 * provided to offer properties common to other Common C++ socket classes,
 * including the socket exception model and the ability to set socket
 * properties such as QoS, "sockopts" properties like Dont-Route
 * and Keep-Alive, etc.
 *
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short base class of all sockets.
 */
class __EXPORT Socket
{
public:
    enum Family {
#ifdef  CCXX_IPV6
        IPV6 = AF_INET6,
#endif
        IPV4 = AF_INET
    };

    typedef enum Family Family;

    enum Error {
        errSuccess = 0,
        errCreateFailed,
        errCopyFailed,
        errInput,
        errInputInterrupt,
        errResourceFailure,
        errOutput,
        errOutputInterrupt,
        errNotConnected,
        errConnectRefused,
        errConnectRejected,
        errConnectTimeout,
        errConnectFailed,
        errConnectInvalid,
        errConnectBusy,
        errConnectNoRoute,
        errBindingFailed,
        errBroadcastDenied,
        errRoutingDenied,
        errKeepaliveDenied,
        errServiceDenied,
        errServiceUnavailable,
        errMulticastDisabled,
        errTimeout,
        errNoDelay,
        errExtended,
        errLookupFail,
        errSearchErr,
        errInvalidValue
    };

    typedef enum Error Error;

    enum Tos {
        tosLowDelay = 0,
        tosThroughput,
        tosReliability,
        tosMinCost,
        tosInvalid
    };
    typedef enum Tos Tos;

    enum Pending {
        pendingInput,
        pendingOutput,
        pendingError
    };
    typedef enum Pending Pending;

protected:
    enum State {
        INITIAL,
        AVAILABLE,
        BOUND,
        CONNECTED,
        CONNECTING,
        STREAM
    };
    typedef enum State State;

private:
    // used by exception handlers....
    mutable Error errid;
    mutable const char *errstr;
    mutable long syserr;

    void setSocket(void);
    friend SOCKET dupSocket(SOCKET s,Socket::State state);

protected:
    static Mutex mutex;

    mutable struct {
        bool thrown: 1;
        bool broadcast: 1;
        bool route: 1;
        bool keepalive: 1;
        bool loopback: 1;
        bool multicast: 1;
        bool completion: 1;
        bool linger: 1;
        unsigned ttl: 8;
    } flags;

    /**
     * the actual socket descriptor, in Windows, unlike posix it
     * *cannot* be used as an file descriptor
     * that way madness lies -- jfc
     */
    SOCKET volatile so;
    State volatile state;

    /**
     * This service is used to throw all socket errors which usually
     * occur during the socket constructor.
     *
     * @param error defined socket error id.
     * @param err string or message to pass.
     * @param systemError the system error# that caused the error
     */
    Error error(Error error, const char *err = NULL, long systemError = 0) const;

    /**
     * This service is used to throw application defined socket errors
     * where the application specific error code is a string.
     *
     * @param err string or message to pass.
     */
    inline void error(const char *err) const
        {error(errExtended, err);};

    /**
     * This service is used to turn the error handler on or off for
     * "throwing" exceptions by manipulating the thrown flag.
     *
     * @param enable true to enable handler.
     */
    inline void setError(bool enable)
        {flags.thrown = !enable;};

    /**
     * Used as the default destructor for ending a socket.  This
     * will cleanly terminate the socket connection.  It is provided
     * for use in derived virtual destructors.
     */
    void endSocket(void);

    /**
     * Used as a common handler for connection failure processing.
     *
     * @return correct failure code to apply.
     */
    Error connectError(void);

    /**
     * Set the send limit.
     */
    Error sendLimit(int limit = 2048);

    /**
     * Set thr receive limit.
     */
    Error receiveLimit(int limit = 1);

    /**
     * Set the send timeout for sending raw network data.
     *
     * @return errSuccess if set.
     * @param timer value in millisec.
     */
    Error sendTimeout(timeout_t timer);

    /**
     * Receive timeout for receiving raw network data.
     *
     * @return errSuccess if set.
     * @param timer value in milliseconds.
     */
    Error receiveTimeout(timeout_t timer);

    /**
     * Set the protocol stack network kernel send buffer size
     * associated with the socket.
     *
     * @return errSuccess on success, or error.
     * @param size of buffer in bytes.
     */
    Error sendBuffer(unsigned size);

    /**
     * Set the protocol stack network kernel receive buffer size
     * associated with the socket.
     *
     * @return errSuccess on success, or error.
     * @param size of buffer in bytes.
     */
    Error receiveBuffer(unsigned size);

    /**
     * Set the total protocol stack network kernel buffer size
     * for both send and receive together.
     *
     * @return errSuccess on success
     * @param size of buffer.
     */
    Error bufferSize(unsigned size);

    /**
     * Set the subnet broadcast flag for the socket.  This enables
     * sending to a subnet and may require special image privileges
     * depending on the operating system.
     *
     * @return 0 (errSuccess) on success, else error code.
     * @param enable when set to true.
     */
    Error setBroadcast(bool enable);

    /**
     * Setting multicast binds the multicast interface used for
     * the socket to the interface the socket itself has been
     * implicitly bound to.  It is also used as a check flag
     * to make sure multicast is enabled before multicast
     * operations are used.
     *
     * @return 0 (errSuccess) on success, else error code.
     * @param enable when set to true.
     * @param family of protocol.
     */
    Error setMulticastByFamily(bool enable, Family family = IPV4);

    /**
     * Set the multicast loopback flag for the socket.  Loopback
     * enables a socket to hear what it is sending.
     *
     * @return 0 (errSuccess) on success, else error code.
     * @param enable when set to true.
     * @param family of protocol.
     */
    Error setLoopbackByFamily(bool enable, Family family = IPV4);

    /**
     * Set the multicast time to live for a multicast socket.
     *
     * @return 0 (errSuccess) on success, else error code.
     * @param ttl time to live.
     * @param fam family of protocol.
     */
    Error setTimeToLiveByFamily(unsigned char ttl, Family fam = IPV4);

    /**
     * Join a multicast group.
     *
     * @return 0 (errSuccess) on success, else error code.
     * @param ia address of multicast group to join.
     */
    Error join(const IPV4Multicast &ia);
#ifdef  CCXX_IPV6
    Error join(const IPV6Multicast &ia);
#endif

    /**
     * Drop membership from a multicast group.
     *
     * @return 0 (errSuccess) on success, else error code.
     * @param ia address of multicast group to drop.
     */
    Error drop(const IPV4Multicast &ia);
#ifdef  CCXX_IPV6
    Error drop(const IPV6Multicast &ia);
#endif

    /**
     * Set the socket routing to indicate if outgoing messages
     * should bypass normal routing (set false).
     *
     * @return 0 on success.
     * @param enable normal routing when set to true.
     */
    Error setRouting(bool enable);


    /**
     * Enable/disable delaying packets (Nagle algorithm)
     *
     * @return 0 on success.
     * @param enable disable Nagle algorithm when set to true.
     */
    Error setNoDelay(bool enable);

    /**
     * An unconnected socket may be created directly on the local
     * machine.  Sockets can occupy both the internet domain (AF_INET)
     * and UNIX socket domain (AF_UNIX) under unix.  The socket type
     * (SOCK_STREAM, SOCK_DGRAM) and protocol may also be specified.
     * If the socket cannot be created, an exception is thrown.
     *
     * @param domain socket domain to use.
     * @param type base type and protocol family of the socket.
     * @param protocol specific protocol to apply.
     */
    Socket(int domain, int type, int protocol = 0);

    /**
     * A socket object may be created from a file descriptor when that
     * descriptor was created either through a socket() or accept()
     * call.  This constructor is mostly for internal use.
     *
     * @param fd file descriptor of an already existing socket.
     */
    Socket(SOCKET fd);

    /**
     * Create an inactive socket object for base constructors.
     */
    Socket();

    /**
     * A socket can also be constructed from an already existing
     * Socket object. On POSIX systems, the socket file descriptor
     * is dup()'d. On Win32, DuplicateHandle() is used.
     *
     * @param source of existing socket to clone.
     */
    Socket(const Socket &source);

    /**
     * Process a logical input line from a socket descriptor
     * directly.
     *
     * @param buf pointer to string.
     * @param len maximum length to read.
     * @param timeout for pending data in milliseconds.
     * @return number of bytes actually read.
     */
    ssize_t readLine(char *buf, size_t len, timeout_t timeout = 0);

    /**
     * Read in a block of len bytes with specific separator.  Can
     * be zero, or any other char.  If \\n or \\r, it's treated just
     * like a readLine().  Otherwise it looks for the separator.
     *
     * @param buf pointer to byte allocation.
     * @param len maximum length to read.
     * @param separator separator for a particular ASCII character
     * @param t timeout for pending data in milliseconds.
     * @return number of bytes actually read.
     */
    virtual ssize_t readData(void * buf,size_t len,char separator=0,timeout_t t=0);

    /**
     * Write a block of len bytes to socket.
     *
     * @param buf pointer to byte allocation.
     * @param len maximum length to write.
     * @param t timeout for pending data in milliseconds.
     * @return number of bytes actually written.
     */
    virtual ssize_t writeData(const void* buf,size_t len,timeout_t t=0);

public:
    /**
     * The socket base class may be "thrown" as a result of an
     * error, and the "catcher" may then choose to destroy the
     * object.  By assuring the socket base class is a virtual
     * destructor, we can assure the full object is properly
     * terminated.
     */
    virtual ~Socket();

    /**
     * See if a specific protocol family is available in the
     * current runtime environment.
     *
     * @return true if family available.
     */
    static bool check(Family fam);

    /**
     * Sockets may also be duplicated by the assignment operator.
     */
    Socket &operator=(const Socket &from);

    /**
     * May be used to examine the origin of data waiting in the
     * socket receive queue.  This can tell a TCP server where pending
     * "connect" requests are coming from, or a UDP socket where it's
     * next packet arrived from.
     *
     * @param port ptr to port number of sender.
     * @return host address, test with "isInetAddress()".
     */
    virtual IPV4Host getIPV4Sender(tpport_t *port = NULL) const;

    inline IPV4Host getSender(tpport_t *port = NULL) const
        {return getIPV4Sender(port);}

#ifdef  CCXX_IPV6
    virtual IPV6Host getIPV6Sender(tpport_t *port = NULL) const;
#endif

    /**
     * Get the host address and port of the socket this socket
     * is connected to.  If the socket is currently not in a
     * connected state, then a host address of 0.0.0.0 is
     * returned.
     *
     * @param port ptr to port number of remote socket.
     * @return host address of remote socket.
     */
    IPV4Host getIPV4Peer(tpport_t *port = NULL) const;

    inline IPV4Host getPeer(tpport_t *port = NULL) const
        {return getIPV4Peer(port);}

#ifdef  CCXX_IPV6
    IPV6Host getIPV6Peer(tpport_t *port = NULL) const;
#endif

    /**
     * Get the local address and port number this socket is
     * currently bound to.
     *
     * @param port ptr to port number on local host.
     * @return host address of interface this socket is bound to.
     */
    IPV4Host getIPV4Local(tpport_t *port = NULL) const;

    inline IPV4Host getLocal(tpport_t *port = NULL) const
        {return getIPV4Local(port);}

#ifdef  CCXX_IPV6
    IPV6Host getIPV6Local(tpport_t *port = NULL) const;
#endif

    /**
     * Perform NAT table lookup for this socket.
     * Used to allow an application to know the original ip:port
     * pair the the client "thinks" it is connecting to. Used mostly
     * to transparently impersonate a remote server/service.
     *
     * On error, 0.0.0.0:0 is returned and one of the following error codes
     * is set: errServiceUnavailable - if nat is not supported on the
     * current platform or if it was not compiled; errLookupFail - if the
     * nat syscall failed for some reason (extended error code);
     * errSearchErr - if the socket does not have nat information (i.e.
     * is not nated).
     *
     * NAT lookup is supported on NetFilter for ipv4 and ipv6 (Linux),
     * IPFilter for ipv4 (Solaris, *BSD except OpenBSD, HP-UX, etc.) and
     * Packet Filter for ipv4 and ipv6 (OpenBSD).
     * When using IPFilter or Packet Filter, the first NAT lookup must be
     * performed as root (the NAT device is read only for root and is opened
     * once, unless an error occurs). Permissions on the nat device may be
     * changed to solve this.
     *
     * \warning When using IPFilter and Packet Filter, application data model
     * must be the same as the running kernel (32/64 bits).
     *
     * @param port ptr to NATed port number on local host.
     * @return NATed host address that this socket is related to.
     */
    IPV4Host getIPV4NAT(tpport_t *port = NULL) const;

    inline IPV4Host getNAT(tpport_t *port) const
        {return getIPV4NAT(port);}

#ifdef  CCXX_IPV6
    IPV6Host getIPV6NAT(tpport_t *port = NULL) const;
#endif

    /**
     * Used to specify blocking mode for the socket.  A socket
     * can be made non-blocking by setting setCompletion(false)
     * or set to block on all access with setCompletion(true).
     * I do not believe this form of non-blocking socket I/O is supported
     * in winsock, though it provides an alternate asynchronous set of
     * socket services.
     *
     * @param immediate mode specify socket I/O call blocking mode.
     */
    void setCompletion(bool immediate);

    /**
     * Enable lingering sockets on close.
     *
     * @param linger specify linger enable.
     */
    Error setLinger(bool linger);

    /**
     * Set the keep-alive status of this socket and if keep-alive
     * messages will be sent.
     *
     * @return 0 on success.
     * @param enable keep alive messages.
     */
    Error setKeepAlive(bool enable);

    /**
     * Set packet scheduling on platforms which support ip quality
     * of service conventions.  This effects how packets in the
     * queue are scheduled through the interface.
     *
     * @return 0 on success, error code on failure.
     * @param service type of service enumerated type.
     */
    Error setTypeOfService(Tos service);

    /**
     * Can test to see if this socket is "connected", and hence
     * whether a "catch" can safely call getPeer().  Of course,
     * an unconnected socket will return a 0.0.0.0 address from
     * getPeer() as well.
     *
     * @return true when socket is connected to a peer.
     */
    bool isConnected(void) const;

    /**
     * Test to see if the socket is at least operating or if it
     * is mearly initialized.  "initialized" sockets may be the
     * result of failed constructors.
     *
     * @return true if not in initial state.
     */
    bool isActive(void) const;

    /**
     * Operator based testing to see if a socket is currently
     * active.
     */
    bool operator!() const;

    /**
     * Return if broadcast has been enabled for the specified
     * socket.
     *
     * @return true if broadcast socket.
     */
    inline bool isBroadcast(void) const
        {return flags.broadcast;};

    /**
     * Return if socket routing is enabled.
     *
     * @return true if routing enabled.
     */
    inline bool isRouted(void) const
        {return flags.route;};

    /**
     * Often used by a "catch" to fetch the last error of a thrown
     * socket.
     *
     * @return error number of Error error.
     */
    inline Error getErrorNumber(void) const {return errid;}

    /**
     * Often used by a "catch" to fetch the user set error string
     * of a thrown socket, but only if EXTENDED error codes are used.
     *
     * @return string for error message.
     */
    inline const char *getErrorString(void) const {return errstr;}

    inline long getSystemError(void) const {return syserr;}

    const char *getSystemErrorString(void) const;

    /**
     * Get the status of pending operations.  This can be used to
     * examine if input or output is waiting, or if an error has
     * occured on the descriptor.
     *
     * @return true if ready, false on timeout.
     * @param pend ready check to perform.
     * @param timeout in milliseconds, inf. if not specified.
     */
    virtual bool isPending(Pending pend, timeout_t timeout = TIMEOUT_INF);
};

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
    bool setCCID(uint8 ccid);

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

/**
 * UDP sockets implement the TCP SOCK_DGRAM UDP protocol.  They can be
 * used to pass unverified messages between hosts, or to broadcast a
 * specific message to an entire subnet.  Please note that Streaming of
 * realtime data commonly use UDPDuplex related classes rather than
 * UDPSocket.
 *
 * In addition to connected TCP sessions, Common C++ supports UDP sockets and
 * these also cover a range of functionality.  Like a TCPSocket, A UDPSocket
 * can be created bound to a specific network interface and/or port address,
 * though this is not required.  UDP sockets also are usually either
 * connected or otherwise "associated" with a specific "peer" UDP socket.
 * Since UDP sockets operate through discreet packets, there are no streaming
 * operators used with UDP sockets.
 *
 * In addition to the UDP "socket" class, there is a "UDPBroadcast" class.
 * The UDPBroadcast is a socket that is set to send messages to a subnet as a
 * whole rather than to an individual peer socket that it may be associated
 * with.
 *
 * UDP sockets are often used for building "realtime" media  streaming
 * protocols and full duplex messaging services.  When used in this manner,
 * typically a pair of UDP sockets are used together; one socket is used to
 * send and the other to receive data with an associated pair of UDP sockets
 * on a "peer" host.  This concept is represented through the Common C++
 * UDPDuplex object, which is a pair of sockets that communicate with another
 * UDPDuplex pair.
 *
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram Protocol sockets.
 */
class __EXPORT UDPSocket : public Socket
{
private:
    inline Error setKeepAlive(bool enable)
        {return Socket::setKeepAlive(enable);};

protected:
#ifdef  CCXX_IPV6
    union {
        struct sockaddr_in6 ipv6;
        struct sockaddr_in ipv4;
    }   peer;
#else
    union {
        struct sockaddr_in ipv4;
    }   peer;
#endif

    Family family;

public:
    /**
     * Create an unbound UDP socket, mostly for internal use.
     */
    UDPSocket(Family family = IPV4);

    /**
     * Create a UDP socket bound by a service name.
     */
    UDPSocket(const char *name, Family family = IPV4);

    /**
     * Create a UDP socket and bind it to a specific interface
     * and port address so that other UDP sockets on remote
     * machines (or the same host) may find and send UDP messages
     * to it.  On failure to bind, an exception is thrown.
     *
     * @param bind address to bind this socket to.
     * @param port number to bind this socket to.
     */
    UDPSocket(const IPV4Address &bind, tpport_t port);
#ifdef  CCXX_IPV6
    UDPSocket(const IPV6Address &bind, tpport_t port);
#endif

    /**
     * Destroy a UDP socket as a socket.
     */
    virtual ~UDPSocket();

    /**
     * Set the loopback.
     */
    inline Error setLoopback(bool enable)
        {return Socket::setLoopbackByFamily(enable, family);}

    /**
     * Set the multicast.
     */
    inline Error setMulticast(bool enable)
        {return Socket::setMulticastByFamily(enable, family);}

    /**
     * Set time to live.
     */
    inline Error setTimeToLive(char ttl)
        {return Socket::setTimeToLiveByFamily(ttl, family);}

    /**
     * set the peer address to send message packets to.  This can be
     * set before every send() call if nessisary.
     *
     * @param host address to send packets to.
     * @param port number to deliver packets to.
     */
    void setPeer(const IPV4Host &host, tpport_t port);
    void connect(const IPV4Host &host, tpport_t port);
#ifdef  CCXX_IPV6
    void setPeer(const IPV6Host &host, tpport_t port);
    void connect(const IPV6Host &host, tpport_t port);
#endif

    /**
     * get the interface index for a named network device
     *
     * @param ethX is device name, like "eth0" or "eth1"
     * @param InterfaceIndex is the index value returned by os
     * @todo Win32 and ipv6 specific implementation.
     */
    Socket::Error getInterfaceIndex(const char *ethX,int& InterfaceIndex);

    /**
     * join a multicast group on a particular interface
     *
     * @param ia is the multicast address to use
     * @param InterfaceIndex is the index value returned by
     * getInterfaceIndex
     * @todo Win32 and ipv6 specific implementation.
     */
    Socket::Error join(const IPV4Multicast &ia,int InterfaceIndex);


    /**
     * Send a message packet to a peer host.
     *
     * @param buf pointer to packet buffer to send.
     * @param len of packet buffer to send.
     * @return number of bytes sent.
     */
    ssize_t send(const void *buf, size_t len);

    /**
     * Receive a message from any host.
     *
     * @param buf pointer to packet buffer to receive.
     * @param len of packet buffer to receive.
     * @param reply save sender address for reply if true.
     * @return number of bytes received.
     */
    ssize_t receive(void *buf, size_t len, bool reply = false);

    /**
     * Examine address of sender of next waiting packet.  This also
     * sets "peer" address to the sender so that the next "send"
     * message acts as a "reply".  This additional behavior overides
     * the standard socket getSender behavior.
     *
     * @param port pointer to hold port number.
     */
    IPV4Host getIPV4Peer(tpport_t *port = NULL) const;
    inline IPV4Host getPeer(tpport_t *port = NULL) const
        {return getIPV4Peer(port);}

#ifdef  CCXX_IPV6
    IPV6Host getIPV6Peer(tpport_t *port = NULL) const;
#endif

    /**
     * Examine contents of next waiting packet.
     *
     * @param buf pointer to packet buffer for contents.
     * @param len of packet buffer.
     * @return number of bytes examined.
     */
    inline ssize_t peek(void *buf, size_t len)
        {return _IORET64 ::recv(so, (char *)buf, _IOLEN64 len, MSG_PEEK);};

    /**
     * Associate socket with a named connection
     */
    void setPeer(const char *service);
    void connect(const char *service);

    /**
     * Disassociate this socket from any host connection.  No data
     * should be read or written until a connection is established.
     */
    Error disconnect(void);
};


/**
 * Representing a UDP socket used for subnet broadcasts, this class
 * provides an alternate binding and setPeer() capability for UDP
 * sockets.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram for subnet broadcasts.
 */
class __EXPORT UDPBroadcast : public UDPSocket
{
private:
    void setPeer(const IPV4Host &ia, tpport_t port);

    Error setBroadcast(bool enable)
        {return Socket::setBroadcast(enable);};

public:
    /**
     * Create and bind a subnet broadcast socket.
     *
     * @param ia address to bind socket under locally.
     * @param port to bind socket under locally.
     */
    UDPBroadcast(const IPV4Address &ia, tpport_t port);

    /**
     * Set peer by subnet rather than specific host.
     *
     * @param subnet of peer hosts to send to.
     * @param port number to use.
     */
    void setPeer(const IPV4Broadcast &subnet, tpport_t port);
};

/**
 * Representing half of a two-way UDP connection, the UDP transmitter
 * can broadcast data to another selected peer host or to an entire
 * subnet.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram Peer Associations.
 */
class __EXPORT UDPTransmit : protected UDPSocket
{
private:
    /**
     * Common code for diferent flavours of Connect (host, broadcast,
     * multicast).
     *
     * @param ia network address to associate with
     * @param port port number to associate with
     */
    Error cConnect(const IPV4Address &ia, tpport_t port);

protected:
    /**
     * Create a UDP transmitter.
     */
    UDPTransmit(Family family = IPV4);

    /**
     * Create a UDP transmitter, bind it to a specific interface
     * and port address so that other UDP sockets on remote
     * machines (or the same host) may find and send UDP messages
     * to it, and associate it with a given port on a peer host.
     * On failure to bind, an exception is thrown.  This class is
     * only used to build the UDP Duplex.
     *
     * @param bind address to bind this socket to.
     * @param port number to bind this socket to.
     */
    UDPTransmit(const IPV4Address &bind, tpport_t port = 5005);
#ifdef  CCXX_IPV6
    UDPTransmit(const IPV6Address &bind, tpport_t port = 5005);
#endif

    /**
     * Associate this socket with a specified peer host.  The port
     * number from the constructor will be used.  All UDP packets
     * will be sent to and received from the specified host.
     *
     * @return 0 on success, -1 on error.
     * @param host address to connect socket to.
     * @param port to connect socket to.
     */
    Error connect(const IPV4Host &host, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Address &host, tpport_t port);
#endif

    /**
     * Associate this socket with a subnet of peer hosts for
     * subnet broadcasting.  The server must be able to assert
     * broadcast permission for the socket.
     *
     * @return 0 on success, -1 on error.
     * @param subnet subnet address to broadcast into.
     * @param port transport port to broadcast into.
     */
    Error connect(const IPV4Broadcast &subnet, tpport_t port);

    /**
     * Associate this socket with a multicast group.
     *
     * @return 0 on success, -1 on error.
     * @param mgroup address of the multicast group to send to.
     * @param port port number
     */
    Error connect(const IPV4Multicast &mgroup, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Multicast &mgroup, tpport_t port);
#endif

    /**
     * Transmit "send" to use "connected" send rather than sendto.
     *
     * @return number of bytes sent.
     * @param buf address of buffer to send.
     * @param len of bytes to send.
     */
    inline ssize_t send(const void *buf, size_t len)
        {return _IORET64 ::send(so, (const char *)buf, _IOLEN64 len, MSG_NOSIGNAL);}

    /**
     * Stop transmitter.
     */
    inline void endTransmitter(void)
        {Socket::endSocket();}

    /*
     * Get transmitter socket.
     *
     * @return transmitter.
     */
    inline SOCKET getTransmitter(void)
        {return so;};

    inline Error setMulticast(bool enable)
        {return Socket::setMulticastByFamily(enable, family);}

    inline Error setTimeToLive(unsigned char ttl)
        {return Socket::setTimeToLiveByFamily(ttl, family);};

public:
    /**
     * Transmit "send" to use "connected" send rather than sendto.
     *
     * @note Windows does not support MSG_DONTWAIT, so it is defined
     *   as 0 on that platform.
     * @return number of bytes sent.
     * @param buffer address of buffer to send.
     * @param len of bytes to send.
     */
    inline ssize_t transmit(const char *buffer, size_t len)
        {return _IORET64 ::send(so, buffer, _IOLEN64 len, MSG_DONTWAIT|MSG_NOSIGNAL);}

    /**
     * See if output queue is empty for sending more packets.
     *
     * @return true if output available.
     * @param timeout in milliseconds to wait.
     */
    inline bool isOutputReady(unsigned long timeout = 0l)
        {return Socket::isPending(Socket::pendingOutput, timeout);};


    inline Error setRouting(bool enable)
        {return Socket::setRouting(enable);};

    inline Error setTypeOfService(Tos tos)
        {return Socket::setTypeOfService(tos);};

    inline Error setBroadcast(bool enable)
        {return Socket::setBroadcast(enable);};
};

/**
 * Representing half of a two-way UDP connection, the UDP receiver
 * can receive data from another peer host or subnet.  This class is
 * used exclusivily to derive the UDPDuplex.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram Peer Associations.
 */
class __EXPORT UDPReceive : protected UDPSocket
{
protected:
    /**
     * Create a UDP receiver, bind it to a specific interface
     * and port address so that other UDP sockets on remote
     * machines (or the same host) may find and send UDP messages
     * to it, and associate it with a given port on a peer host.
     * On failure to bind, an exception is thrown.
     *
     * @param bind address to bind this socket to.
     * @param port number to bind this socket to.
     */
    UDPReceive(const IPV4Address &bind, tpport_t port);
#ifdef  CCXX_IPV6
    UDPReceive(const IPV6Address &bind, tpport_t port);
#endif

    /**
     * Associate this socket with a specified peer host.  The port
     * number from the constructor will be used.  All UDP packets
     * will be sent received from the specified host.
     *
     * @return 0 on success, -1 on error.
     * @param host host network address to connect socket to.
     * @param port host transport port to connect socket to.
     */
    Error connect(const IPV4Host &host, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Host &host, tpport_t port);
#endif

    /**
     * Check for pending data.
     *
     * @return true if data is waiting.
     * @param timeout in milliseconds.
     */
    bool isPendingReceive(timeout_t timeout)
        {return Socket::isPending(Socket::pendingInput, timeout);};

    /**
     * End receiver.
     */
    inline void endReceiver(void)
        {Socket::endSocket();}

    inline SOCKET getReceiver(void) const
        {return so;};

    inline Error setRouting(bool enable)
        {return Socket::setRouting(enable);}

    inline Error setMulticast(bool enable)
        {return Socket::setMulticastByFamily(enable, family);}

    inline Error join(const IPV4Multicast &ia)
            {return Socket::join(ia);}

#ifdef  CCXX_IPV6
    inline Error join(const IPV6Multicast &ia)
        {return Socket::join(ia);}
#endif

    inline Error drop(const IPV4Multicast &ia)
            {return Socket::drop(ia);}

#ifdef  CCXX_IPV6
    inline Error drop(const IPV6Multicast &ia)
        {return Socket::drop(ia);}
#endif

public:
    /**
     * Receive a data packet from the connected peer host.
     *
     * @return num of bytes actually received.
     * @param buf address of data receive buffer.
     * @param len size of data receive buffer.
     */
    inline ssize_t receive(void *buf, size_t len)
        {return _IORET64 ::recv(so, (char *)buf, _IOLEN64 len, 0);};

    /**
     * See if input queue has data packets available.
     *
     * @return true if data packets available.
     * @param timeout in milliseconds.
     */
    inline bool isInputReady(timeout_t timeout = TIMEOUT_INF)
        {return Socket::isPending(Socket::pendingInput, timeout);};
};

/**
 * UDP duplex connections impliment a bi-directional point-to-point UDP
 * session between two peer hosts.  Two UDP sockets are typically used
 * on alternating port addresses to assure that sender and receiver
 * data does not collide or echo back.  A UDP Duplex is commonly used
 * for full duplex real-time streaming of UDP data between hosts.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram Peer Associations.
 */
class __EXPORT UDPDuplex : public UDPTransmit, public UDPReceive
{
public:
    /**
     * Create a UDP duplex as a pair of UDP simplex objects
     * bound to alternating and interconnected port addresses.
     *
     * @param bind address to bind this socket to.
     * @param port number to bind sender.
     */
    UDPDuplex(const IPV4Address &bind, tpport_t port);
#ifdef  CCXX_IPV6
    UDPDuplex(const IPV6Address &bind, tpport_t port);
#endif

    /**
     * Associate the duplex with a specified peer host. Both
     * the sender and receiver will be interconnected with
     * the remote host.
     *
     * @return 0 on success, error code on error.
     * @param host address to connect socket to.
     * @param port number to connect socket to.
     */
    Error connect(const IPV4Host &host, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Host &host, tpport_t port);
#endif

    /**
     * Disassociate this duplex from any host connection.  No data
     * should be read or written until a connection is established.
     *
     * @return 0 on success, error code on error.
     */
    Error disconnect(void);
};


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

/*
:\projects\libraries\cplusplus\commonc++\win32\socket.h(357) : warning C4275: non dll-interface class 'streambuf' used as base for dll-interface class 'TCPStream'
    c:\program files\microsoft visual studio\vc98\include\streamb.h(69) : see declaration of 'streambuf'
c:\projects\libraries\cplusplus\commonc++\win32\socket.h(358) : warning C4275: non dll-interface class 'iostream' used as base for dll-interface class 'TCPStream'
    c:\program files\microsoft visual studio\vc98\include\iostream.h(66) : see declaration of 'iostream'
*/

#ifdef _MSC_VER
#pragma warning(disable:4275) // disable C4275 warning
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
     * A copy constructor creates a new stream buffer.
     *
     * @param source reference of stream to copy from.
     *
     */
    TCPStream(const TCPStream &source);

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

#ifdef  HAVE_SNPRINTF
    /**
     * Print content into a socket.
     *
     * @return count of bytes sent.
     * @param format string
     */
    size_t printf(const char *format, ...);
#endif

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
         {return _IORET64 ::recv(so, (char *)buf, _IOLEN64 len, MSG_PEEK);};

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

#if defined(WIN32)

/**
 *  class init_WSA used to initalise windows sockets specfifc stuff : there is
 *  an MS - specific init sequence for Winsock 2 this class attempts to
 *  initalise Winsock 2.2 - needed for non - blocking I/O. It will fall back
 *  on 1.2 or lower if 2.0 or higher is not available,  but < 2.0 does not
 *  support non - blocking I/o
 *  TO DO : might be an idea to have a method that reports version of
 *  Winsock in use or a predicate to test if non - blocking is OK -- JFC
 */
class init_WSA
{
public:
    init_WSA();
    ~init_WSA();
};

#endif // WIN32

class __EXPORT SimpleTCPStream;

/**
 * @class SimpleTCPStream
 * @brief Simple TCP Stream, to be used with Common C++ Library
 *
 * This source is derived from a proposal made by Ville Vainio
 * (vvainio@tp.spt.fi).
 *
 * @author Mark S. Millard (msm@wizzer.com)
 * @date   2002-08-15
 * Copyright (C) 2002 Wizzer Works.
 **/
class __EXPORT SimpleTCPStream : public Socket
{
private:

    IPV4Host getSender(tpport_t *port) const;

protected:
    /**
     * The constructor required for "SimpleTCPStream", a more C++ style
     * version of the SimpleTCPStream class.
     */
    SimpleTCPStream();

    /**
     * Used to terminate the buffer space and cleanup the socket
     * connection.  This fucntion is called by the destructor.
     */
    void endStream(void);

    /**
     * Create a TCP stream by connecting to a TCP socket (on
     * a remote machine).
     *
     * @param host address of remote TCP server.
     * @param port number to connect.
     * @param size of streaming input and output buffers.
     */
    void Connect(const IPV4Host &host, tpport_t port, size_t size);


public:
    /**
     * Create a TCP stream by accepting a connection from a bound
     * TCP socket acting as a server.  This performs an "accept"
     * call.
     *
     * @param server bound server tcp socket.
     * @param size of streaming input and output buffers.
     */
    SimpleTCPStream(TCPSocket &server, size_t size = 512);

    /**
     * Create a TCP stream by connecting to a TCP socket (on
     * a remote machine).
     *
     * @param host address of remote TCP server.
     * @param port number to connect.
     * @param size of streaming input and output buffers.
     */
    SimpleTCPStream(const IPV4Host &host, tpport_t port, size_t size = 512);

    /**
     * A copy constructor creates a new stream buffer.
     *
     * @param source A reference to the SimpleTCPStream to copy.
     */
    SimpleTCPStream(const SimpleTCPStream &source);

    /**
     * Flush and empty all buffers, and then remove the allocated
     * buffers.
     */
    virtual ~SimpleTCPStream();

    /**
     * @brief Get the status of pending stream data.
     *
     * This method can be used to examine if input or output is waiting,
     * or if an error or disconnect has occured on the stream.
     * If a read buffer contains data then input is ready. If write buffer
     * contains data, it is first flushed and then checked.
     *
     * @param pend Flag indicating means to pend.
     * @param timeout The length of time to wait.
     */
    bool isPending(Pending pend, timeout_t timeout = TIMEOUT_INF);

    void flush() {}

    /**
     * @brief Read bytes into a buffer.
     *
     * <long-description>
     *
     * @param bytes A pointer to buffer that will contain the bytes read.
     * @param length The number of bytes to read (exactly).
     * @param timeout Period to time out, in milleseconds.
     *
     * @return The number of bytes actually read, 0 on EOF.
     */
    ssize_t read(char *bytes, size_t length, timeout_t timeout = 0);

    /**
     * @brief Write bytes to buffer
     *
     * <long-description>
     *
     * @param bytes A pointer to a buffer containing the bytes to write.
     * @param length The number of bytes to write (exactly).
     * @param timeout Period to time out, in milleseconds.
     *
     * @return The number of bytes actually written.
     */
    ssize_t write(const char *bytes, size_t length, timeout_t timeout = 0);

    /**
     * @brief Peek at the incoming data.
     *
     * The data is copied into the buffer
     * but is not removed from the input queue. The function then returns
     * the number of bytes currently pending to receive.
     *
     * @param bytes A pointer to buffer that will contain the bytes read.
     * @param length The number of bytes to read (exactly).
     * @param timeout Period to time out, in milleseconds.
     *
     * @return The number of bytes pending on the input queue, 0 on EOF.
     */
    ssize_t peek(char *bytes, size_t length, timeout_t timeout = 0);

};

#ifdef  COMMON_STD_EXCEPTION
class __EXPORT SockException : public IOException
{
private:
    Socket::Error _socketError;

public:
    SockException(const String &str, Socket::Error socketError, long systemError = 0) :
        IOException(str, systemError), _socketError(socketError) {};

    inline Socket::Error getSocketError() const
    { return _socketError; }
};
#endif

#ifdef  CCXX_NAMESPACES
}
#endif

#endif
/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
