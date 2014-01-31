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
 * @file commoncpp/socket.h
 * @short socket operations.
 **/

#ifndef COMMONCPP_SOCKET_H_
#define COMMONCPP_SOCKET_H_

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

#ifndef COMMONCPP_EXCEPTION_H_
#include <commoncpp/exception.h>
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

NAMESPACE_COMMONCPP

typedef socket_t    SOCKET;

class __EXPORT Socket : protected ucommon::Socket
{
public:
    enum State {
        INITIAL,
        AVAILABLE,
        BOUND,
        CONNECTED,
        CONNECTING,
        STREAM
    };
    typedef enum State State;

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

private:
    // used by exception handlers....
    mutable Error errid;
    mutable const char *errstr;
    mutable long syserr;

    void setSocket(void);

protected:
    static socket_t dupSocket(socket_t s,Socket::State state);

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
    Socket(socket_t fd);

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
    ~Socket();

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

    /**
     * See if a specific protocol family is available in the
     * current runtime environment.
     *
     * @return true if family available.
     */
    static bool check(Family fam);

    /**
     * Operator based testing to see if a socket is currently
     * active.
     */
    bool operator!() const;

    operator bool() const;

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


    inline struct in_addr getaddress(const IPV4Address &ia)
        {return ia.getAddress();}

#ifdef  CCXX_IPV6
    inline struct in6_addr getaddress(const IPV6Address &ia)
        {return ia.getAddress();}
#endif

};

#if defined(CCXX_EXCEPTIONS)

class __EXPORT SockException : public IOException
{
private:
    Socket::Error _socketError;

public:
    inline SockException(const String &str, Socket::Error socketError, long systemError = 0) :
        IOException(str, systemError), _socketError(socketError) {};

    inline Socket::Error getSocketError() const
    { return _socketError; }
};

#endif

END_NAMESPACE

#endif
