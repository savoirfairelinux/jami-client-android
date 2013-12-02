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
 * @file socketport.h
 * @short Network service framework and design pattern.
 **/

#ifndef CCXX_SOCKETPORT_H_
#define CCXX_SOCKETPORT_H_

#ifndef CCXX_ADDRESS_H_
#include <cc++/address.h>
#endif

#ifndef CCXX_SOCKET_H_
#include <cc++/socket.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

class __EXPORT SocketPort;
class __EXPORT SocketService;

/**
 * The socket port is an internal class which is attached to and then
 * serviced by a specific SocketService "object".  Derived versions of
 * this class offer specific functionality for specific protocols.  Both
 * Common C++ supporting frameworks and application objects may be derived
 * from related protocol specific base classes.
 *
 * A special set of classes, "SocketPort" and "SocketService", exist
 * for building realtime streaming media servers on top of UDP and TCP
 * protocols.  The "SocketPort" is used to hold a connected or associated TCP
 * or UDP socket which is being "streamed" and which offers callback methods
 * that are invoked from a "SocketService" thread.  SocketService's can be
 * pooled into logical thread pools that can service a group of SocketPorts.
 * A millisecond accurate "timer" is associated with each SocketPort and can
 * be used to time synchronize SocketPort I/O operations.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short base class for realtime and thread pool serviced protocols.
 */
class __EXPORT SocketPort : public Socket, public TimerPort
{
private:
    SocketPort *next, *prev;
    SocketService *service;
#ifndef WIN32
    struct timeval porttimer;
#ifdef USE_POLL
    struct pollfd   * ufd;
#endif
#else
    HANDLE event;
#endif
    bool detect_pending;
    bool detect_output;
    bool detect_disconnect;

    friend class SocketService;

protected:
    /**
     * Construct an accepted TCP socket connection from a specific
     * bound TCP server.  This is meant to derive advanced application
     * specific TCP servers that can be thread pooled.
     *
     * @param svc pool thread object.
     * @param tcp socket object to accept.
     */
    SocketPort(SocketService *svc, TCPSocket &tcp);
#ifdef  CCXX_IPV6
    SocketPort(SocketService *svc, TCPV6Socket &tcp);
#endif

    /**
     * Construct a bound UDP socket for use in deriving realtime
     * UDP streaming protocols handled by thread pool objects.
     *
     * @param svc pool thread object.
     * @param ia address of interface to bind.
     * @param port number to bind to.
     */
    SocketPort(SocketService *svc, const IPV4Address &ia, tpport_t port);
#ifdef  CCXX_IPV6
    SocketPort(SocketService *svc, const IPV6Address &ia, tpport_t port);
#endif

    /**
     * A non-blocking constructor for outbound tcp connections.
     * To detect when the connection is established, overload
     * SocketPort::output().  SocketPort::output() get's called by
     * the SocketService when the connection is ready,
     * SocketPort::disconnect() when the connect failed.  at the
     * moment you should set the socket state to "CONNECTED" when
     * SocketPort::output() get's called for the first time.
     *
     * @param svc pool thread object.
     * @param ih addess to connect to.
     * @param port number to connect to.
     **/
    SocketPort(SocketService *svc, const IPV4Host &ih, tpport_t port);
#ifdef  CCXX_IPV6
    SocketPort(SocketService *svc, const IPV6Host &ih, tpport_t port);
#endif

    /**
     * Attach yourself to the service pool thread object. The later version.
     *
     * @param svc pool thread object
     */
     void attach( SocketService* svc );


    /**
     * Disconnect the socket from the service thread pool and
     * the remote connection.
     */
    virtual ~SocketPort();

    /**
     * Used to indicate if the service thread should monitor pending
     * data for us.
     */
    void setDetectPending( bool );

    /**
     * Get the current state of the DetectPending flag.
     */
    bool getDetectPending( void ) const
        { return detect_pending; }

    /**
     * Used to indicate if output ready monitoring should be performed
     * by the service thread.
     */
    void setDetectOutput( bool );

    /**
     * Get the current state of the DetectOutput flag.
     */
    bool getDetectOutput( void ) const
        { return detect_output; }

    /**
     * Called by the service thread pool when the objects timer
     * has expired.  Used for timed events.
     */
    virtual void expired(void);

    /**
     * Called by the service thread pool when input data is pending
     * for this socket.
     */
    virtual void pending(void);

    /**
     * Called by the service thread pool when output data is pending
     * for this socket.
     */
    virtual void output(void);

    /**
     * Called by the service thread pool when a disconnect has
     * occured.
     */
    virtual void disconnect(void);

    /**
     * Connect a Socket Port to a known peer host.  This is normally
     * used with the UDP constructor.  This is also performed as a
     * non-blocking operation under Posix systems to prevent delays
     * in a callback handler.
     *
     * @return 0 if successful.
     * @param ia address of remote host or subnet.
     * @param port number of remote peer(s).
     */
    Error connect(const IPV4Address &ia, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Address &ia, tpport_t port);
#endif

    /**
     * Transmit "send" data to a connected peer host.  This is not
     * public by default since an overriding protocol is likely to
     * be used in a derived class.
     *
     * @return number of bytes sent.
     * @param buf address of buffer to send.
     * @param len of bytes to send.
     */
    inline ssize_t send(const void *buf, size_t len)
        {return _IORET64 ::send(so, (const char *)buf, _IOLEN64 len, 0);};

    /**
     * Receive a message from any host.  This is used in derived
     * classes to build protocols.
     *
     * @param buf pointer to packet buffer to receive.
     * @param len of packet buffer to receive.
     * @return number of bytes received.
     */
    inline ssize_t receive(void *buf, size_t len)
        {return _IORET64 ::recv(so, (char *)buf, _IOLEN64 len, 0);};

    /**
     * Examine the content of the next packet.  This can be used
     * to build "smart" line buffering for derived TCP classes.
     *
     * @param buf pointer to packet buffer to examine.
     * @param len of packet buffer to examine.
     * @return number of bytes actually available.
     */
    inline ssize_t peek(void *buf, size_t len)
        {return _IORET64 ::recv(so, (char *)buf, _IOLEN64 len, MSG_PEEK);};

public:
    /**
     * Derived setTimer to notify the service thread pool of change
     * in expected timeout.  This allows SocketService to
     * reschedule all timers.  Otherwise same as TimerPort.
     *
     * @param timeout in milliseconds.
     */
    void setTimer(timeout_t timeout = 0);

    /**
     * Derived incTimer to notify the service thread pool of a
     * change in expected timeout.  This allows SocketService to
     * reschedule all timers.  Otherwise same as TimerPort.
     *
     * @param timeout in milliseconds.
     */
    void incTimer(timeout_t timeout);
};

/**
 * The SocketService is a thread pool object that is meant to service
 * attached socket ports.  Multiple pool objects may be created and
 * multiple socket ports may be attached to the same thread of execution.
 * This allows one to balance threads and sockets they service rather than
 * either using a single thread for all connections or a seperate thread
 * for each connection.  Features can be added through supported virtual
 * methods.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Thread pool service object for socket ports.
 */
class __EXPORT SocketService : public Thread, private Mutex
{
private:
#ifndef WIN32
    fd_set connect;
    int iosync[2];
    int hiwater;
#else
    // private syncronization class
    class Sync;
    Sync* sync;
#endif
    int volatile count;
    SocketPort* volatile first, *last;

    /**
     * Attach a new socket port to this service thread.
     *
     * @param port of SocketPort derived object to attach.
     */
    void attach(SocketPort *port);
    /**
     * Detach a socket port from this service thread.
     *
     * @param port of SocketPort derived object to remove.
     */
    void detach(SocketPort *port);

    /**
     * The service thread itself.
     */
    void run(void);

    friend class SocketPort;

protected:
    /**
     * Handles all requests other than "termination".
     *
     * @param buf request id as posted from update().
     */
    virtual void onUpdate(unsigned char buf);

    /**
     * Called once each time the service thread is rescheduled.
     * This is called after the mutex is locked and can be used to
     * slip in additional processing.
     */
    virtual void onEvent(void);

    /**
     * Called for each port that is being processed in response to
     * an event.  This can be used to add additional notification
     * options during callback in combination with update().
     *
     * @param port SocketPort who's callback events are being evaluated.
     */
    virtual void onCallback(SocketPort *port);

public:
    /**
     * Notify service thread that a port has been added or
     * removed, or a timer changed, so that a new schedule
     * can be computed for expiring attached ports.  A "0"
     * is used to terminate the service thread, and additional
     * values can be specified which will be "caught" in the
     * onUpdate() handler.
     *
     * @param flag update flag value.
     */
    void update(unsigned char flag = 0xff);

    /**
     * Create a service thread for attaching socket ports.  The
     * thread begins execution with the first attached socket.
     *
     * @param pri of this thread to run under.
     * @param stack stack size.
     * @param id thread ID.
     */
    SocketService(int pri = 0, size_t stack = 0, const char *id = NULL);

    /**
     * Terminate the thread pool and eliminate any attached
     * socket ports.
     */
    virtual ~SocketService();

    /**
     * Get current reference count.  This can be used when selecting
     * the least used service handler from a pool.
     *
     * @return count of active ports.
     */
    inline int getCount(void) const
        {return count;};
};

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
