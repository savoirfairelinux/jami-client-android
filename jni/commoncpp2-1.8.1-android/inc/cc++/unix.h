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
 * @file unix.h
 * @short UNIX domain sockets, streams and sessions.
 **/

#ifndef CCXX_UNIX_H_
#define CCXX_UNIX_H_

#ifndef CCXX_MISSING_H_
#include <cc++/missing.h>
#endif

#ifndef CCXX_SOCKET_H_
#include <cc++/socket.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifndef WIN32

 /**
  * Unix domain sockets are used for stream based connected sessions between
  * processes on the same machine.

  * An implicit and unique UnixSocket object exists in Common C++ to represent
  * a bound Unix domain socket acting as a "server" for receiving connection requests.
  * This class is not part of UnixStream because such objects normally perform
  * no physical I/O (read or write operations) other than to specify a listen
  * backlog queue and perform "accept" operations for pending connections.
  *
  * @author Alex Pavloff <alex@pavloff.net>
  * @short bound server for Unix domain streams and sessions.
  */
 class UnixSocket : protected Socket {
 protected:
    friend class UnixStream;
    friend class SocketPort;
    friend class unixstream;

    void close(void);
    char *path;

 public:
    /**
     * A Unix domain "server" is created as a Unix domain socket that is bound
     * to a pathname and that has a backlog queue to listen for connection
     * requests.  If the server cannot be created, an exception is thrown.
     *
     * @param pathname pathname to socket file
     * @param backlog size of connection request queue.
     */
    UnixSocket(const char* pathname, int backlog = 5);

    /**
     * Used to wait for pending connection requests.
     */
    inline bool isPendingConnection(timeout_t timeout = TIMEOUT_INF) /** not const -- jfc */
        {return Socket::isPending(pendingInput, timeout);}

    /**
     * Use base socket handler for ending this socket.
     */
    virtual ~UnixSocket();
 };

 /**
  * Unix streams are used to represent Unix domain client connections to a
  * local server for accepting client connections.  The Unix
  * stream is a C++ "stream" class, and can accept streaming of data to
  * and from other C++ objects using the << and >> operators.
  *
  * Unix Stream itself can be formed either by connecting to a bound network
  * address of a Unix domain server, or can be created when "accepting" a
  * network connection from a Unix domain server.
  *
  * @author Alex Pavloff <alex@pavloff.net>
  * @short streamable Unix domain socket connection.
  */
 class UnixStream : public Socket, public std::streambuf, public std::iostream {
 private:
    int doallocate();

 protected:
    timeout_t timeout;
    int bufsize;
    char *gbuf, *pbuf;

    /**
     * The constructor required for "unixstream", a more C++ style
     * version of the TCPStream class.
     */
    UnixStream(bool throwflag = true);

    /**
     * Used to allocate the buffer space needed for iostream
     * operations.  This function is called by the constructor.
     *
     * @param size of stream buffers from constructor.
     */
    void allocate(int size);

    /**
     * Used to terminate the buffer space and cleanup the socket
     * connection.  This fucntion is called by the destructor.
     */
    void endStream(void);

    /**
     * This streambuf method is used to load the input buffer
     * through the established unix domain socket connection.
     *
     * @return char from get buffer, EOF if not connected.
     */
    virtual int underflow(void);

    /**
     * This streambuf method is used for doing unbuffered reads
     * through the established unix domain socket connection when in interactive mode.
     * Also this method will handle proper use of buffers if not in
     * interative mode.
     *
     * @return char from unix domain socket connection, EOF if not connected.
     */
    int uflow(void);

    /**
     * This streambuf method is used to write the output
     * buffer through the established unix domain connection.
     *
     * @param ch char to push through.
     * @return char pushed through.
     */
    int overflow(int ch);

    /**
     * Create a Unix domain stream by connecting to a Unix domain socket
     *
     * @param pathname path to socket
     * @param size of streaming input and output buffers.
     */
    void connect(const char* pathname, int size);

    /**
     * Used in derived classes to refer to the current object via
     * it's iostream.  For example, to send a set of characters
     * in a derived method, one might use *tcp() << "test".
     *
     * @return stream pointer of this object.
     */
    std::iostream *unixstr(void)
        {return ((std::iostream *)this);};

 public:
    /**
     * Create a Unix domain stream by accepting a connection from a bound
     * Unix domain socket acting as a server.  This performs an "accept"
     * call.
     *
     * @param server socket listening.
     * @param size of streaming input and output buffers.
     * @param throwflag flag to throw errors.
     * @param timeout for all operations.
     */
    UnixStream(UnixSocket &server, int size = 512, bool throwflag = true, timeout_t timeout = 0);

    /**
     * Create a Unix domain stream by connecting to a Unix domain socket
     *
     * @param pathname path to socket
     * @param size of streaming input and output buffers.
     * @param throwflag flag to throw errors.
     * @param to timeout for all operations.
     */
    UnixStream(const char* pathname, int size = 512, bool throwflag = true, timeout_t to = 0);

    /**
     * Set the I/O operation timeout for socket I/O operations.
     *
     * @param to timeout to set.
     */
    inline void setTimeout(timeout_t to)
        {timeout = to;};

    /**
     * A copy constructor creates a new stream buffer.
     *
     * @param source of copy.
     *
     */
    UnixStream(const UnixStream &source);

    /**
     * Flush and empty all buffers, and then remove the allocated
     * buffers.
     */
    virtual ~UnixStream();

    /**
     * Flushes the stream input and output buffers, writes
     * pending output.
     *
     * @return 0 on success.
     */
    int sync(void);

    /**
     * Get the status of pending stream data.  This can be used to
     * examine if input or output is waiting, or if an error or
     * disconnect has occured on the stream.  If a read buffer
     * contains data then input is ready and if write buffer
     * contains data it is first flushed and then checked.
     */
    bool isPending(Pending pend, timeout_t timeout = TIMEOUT_INF);

    /**
     * Return the size of the current stream buffering used.
     *
     * @return size of stream buffers.
     */
    int getBufferSize(void) const
        {return bufsize;};
 };

 /**
  * A more natural C++ "unixstream" class for use by non-threaded
  * applications.  This class behaves a lot more like fstream and
  * similar classes.
  *
  * @author Alex Pavloff <alex@pavloff.net>
  * @short C++ "fstream" style unixstream class.
  */
 class unixstream : public UnixStream {
 public:
    /**
     * Construct an unopened "tcpstream" object.
     */
    unixstream();

    /**
     * Construct and "open" (connect) the tcp stream to a remote
     * socket.
     *
     * @param pathname pathname to socket file
     * @param buffer size for streaming (optional).
     */
    unixstream(const char *pathname, int buffer = 512);

    /**
     * Construct and "accept" (connect) the tcp stream through
     * a server.
     *
     * @param unixsock socket to accept from.
     * @param buffer size for streaming (optional).
     */
    unixstream(UnixSocket &unixsock, int buffer = 512);

    /**
     * Open a tcp stream connection.  This will close the currently
     * active connection first.
     *
     * @param pathname pathname to socket file
     * @param buffer size for streaming (optional)
     */
    void open(const char *pathname, int buffer = 512)
        {UnixStream::connect( pathname, buffer );}

    /**
     * Open a tcp stream connection by accepting a tcp socket.
     *
     * @param unixsock socket to accept from.
     * @param buffer size for streaming (optional)
     */
    void open(UnixSocket &unixsock, int buffer = 512);

    /**
     * Close the active tcp stream connection.
     */
    void close(void);

    /**
     * Test to see if stream is open.
     */
    bool operator!() const;
 };

 /**
  * The Unix domain session is used to primarily to represent a client connection
  * that can be managed on a seperate thread.  The Unix domain session also supports
  * a non-blocking connection scheme which prevents blocking during the
  * constructor and moving the process of completing a connection into the
  * thread that executes for the session.
  *
  * @author Alex Pavloff <alex@pavloff.net>
  * @short Threaded streamable unix domain socket with non-blocking constructor.
  */
 class __EXPORT UnixSession : public Thread, public UnixStream {
 protected:
    /**
     * Normally called during the thread Initial() method by default,
     * this will wait for the socket connection to complete when
     * connecting to a remote socket.  One might wish to use
     * setCompletion() to change the socket back to blocking I/O
     * calls after the connection completes.  To implement the
     * session one must create a derived class which implements
     * Run().
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
     * Create a Unix domain socket that will be connected to a local server
     * server and that will execute under it's own thread.
     *
     * @param pathname path to socket
     * @param size of streaming buffer.
     * @param pri execution priority relative to parent.
     * @param stack allocation needed on some platforms.
     */
    UnixSession(const char* pathname, int size = 512, int pri = 0, int stack = 0);

    /**
     * Create a Unix domain socket from a bound Unix domain server by accepting a pending
     * connection from that server and execute a thread for the accepted connection.
     *
     * @param server unix domain socket to accept a connection from.
     * @param size of streaming buffer.
     * @param pri execution priority relative to parent.
     * @param stack allocation needed on some platforms.
     */
    UnixSession(UnixSocket &server, int size = 512,
           int pri = 0, int stack = 0);

    /**
     * Virtual destructor.
     */
    virtual ~UnixSession();
 };

#endif // ndef WIN32

#ifdef  CCXX_NAMESPACES
}
#endif

#endif



