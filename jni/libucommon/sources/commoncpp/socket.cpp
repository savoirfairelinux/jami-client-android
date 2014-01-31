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

#include <ucommon-config.h>
#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/address.h>
#include <commoncpp/socket.h>

#include <fcntl.h>

#ifdef _MSWINDOWS_
#include <io.h>
#define _IOLEN64    (unsigned)
#define _IORET64    (int)
typedef int socklen_t;

#define socket_errno    WSAGetLastError()
#else
#include <sys/ioctl.h>
#include <netinet/tcp.h>
#ifdef HAVE_NET_IP6_H
#include <netinet/ip6.h>
#endif
#define socket_errno errno
# ifndef  O_NONBLOCK
#  define O_NONBLOCK    O_NDELAY
# endif
# ifdef IPPROTO_IP
#  ifndef  SOL_IP
#   define SOL_IP   IPPROTO_IP
#  endif // !SOL_IP
# endif  // IPPROTO_IP
#endif   // !WIN32

#ifndef INADDR_LOOPBACK
#define INADDR_LOOPBACK (unsigned long)0x7f000001
#endif

#ifdef HAVE_NETINET_IN_H
#include <netinet/in.h>
#endif

#if defined(__hpux)
#define _XOPEN_SOURCE_EXTENDED
#endif

#ifdef  HAVE_NET_IF_H
#include <net/if.h>
#endif

#ifndef _IOLEN64
#define _IOLEN64
#endif

#ifndef _IORET64
#define _IORET64
#endif

using namespace COMMONCPP_NAMESPACE;

#if defined(_MSWINDOWS_) && !defined(__MINGW32__)
socket_t Socket::dupSocket(socket_t so, enum Socket::State state)
{
    if (state == Socket::STREAM)
        return dup((int)so);

    HANDLE pidHandle = GetCurrentProcess();
    HANDLE dupHandle;
    if(DuplicateHandle(pidHandle, reinterpret_cast<HANDLE>(so), pidHandle, &dupHandle, 0, FALSE, DUPLICATE_SAME_ACCESS))
        return reinterpret_cast<socket_t>(dupHandle);
    return (socket_t)(-1);
}
# define DUP_SOCK(s,state) dupSocket(s,state)
#else
socket_t Socket::dupSocket(socket_t so, State state)
{
    return dup(so);
}
#endif

Mutex Socket::mutex;

void Socket::setSocket(void)
{
    flags.thrown    = false;
    flags.broadcast = false;
    flags.route     = true;
    flags.keepalive = false;
    flags.loopback  = true;
    flags.multicast = false;
    flags.linger    = false;
    flags.ttl   = 1;
    errid           = errSuccess;
    errstr          = NULL;
    syserr          = 0;
    state           = INITIAL;
    so              = INVALID_SOCKET;
}

void Socket::endSocket(void)
{
    if(Socket::state == STREAM) {
        state = INITIAL;
        if(so != INVALID_SOCKET) {
            SOCKET sosave = so;
            so = INVALID_SOCKET;
            release(sosave);
        }
        return;
    }

    state = INITIAL;
    if(so == INVALID_SOCKET)
        return;

#ifdef  SO_LINGER
    struct linger linger;

    if(flags.linger) {
        linger.l_onoff = 1;
        linger.l_linger = 60;
    }
    else
        linger.l_onoff = linger.l_linger = 0;
    setsockopt(so, SOL_SOCKET, SO_LINGER, (char *)&linger,
        (socklen_t)sizeof(linger));
#endif
//  shutdown(so, 2);
    ucommon::Socket::release();
}

Socket::Socket() : ucommon::Socket()
{
    setSocket();
}

Socket::Socket(int domain, int type, int protocol) : ucommon::Socket()
{
    setSocket();
    so = socket(domain, type, protocol);
    if(so == INVALID_SOCKET) {
        error(errCreateFailed,(char *)"Could not create socket",socket_errno);
        return;
    }
#ifdef  SO_NOSIGPIPE
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_NOSIGPIPE, (char *)&opt, sizeof(opt));
#endif
    state = AVAILABLE;
}

Socket::Socket(socket_t fd) : ucommon::Socket()
{
    setSocket();
    if (fd == INVALID_SOCKET) {
        error(errCreateFailed,(char *)"Invalid socket handle passed",0);
        return;
    }
    so = fd;
    state = AVAILABLE;
}

Socket::Socket(const Socket &orig) : ucommon::Socket()
{
    setSocket();
    so = dupSocket(orig.so,orig.state);
    if(so == INVALID_SOCKET)
        error(errCopyFailed,(char *)"Could not duplicate socket handle",socket_errno);
    state = orig.state;
}

Socket::~Socket()
{
    endSocket();
}

Socket::Error Socket::error(Error err, const char *errs, long systemError) const
{
    errid  = err;
    errstr = errs;
    syserr = systemError;
    if(!err)
        return err;

    if(flags.thrown)
        return err;

    // prevents recursive throws

    flags.thrown = true;
#ifdef  CCXX_EXCEPTIONS
    switch(Thread::getException()) {
    case Thread::throwObject:
        THROW((Socket *)this);
    case Thread::throwException:
        if(!errs)
            errs = (char *)"";
        THROW(SockException(String(errs), err, systemError));
    case Thread::throwNothing:
        break;
    }
#endif
    return err;
}

#ifdef _MSWINDOWS_
Socket::Error Socket::connectError(void)
{
    const char* str = "Could not connect to remote host";
    switch(WSAGetLastError()) {
    case WSAENETDOWN:
        return error(errResourceFailure,str,socket_errno);
    case WSAEINPROGRESS:
        return error(errConnectBusy,str,socket_errno);
    case WSAEADDRNOTAVAIL:
        return error(errConnectInvalid,str,socket_errno);
    case WSAECONNREFUSED:
        return error(errConnectRefused,str,socket_errno);
    case WSAENETUNREACH:
        return error(errConnectNoRoute,str,socket_errno);
    default:
        return error(errConnectFailed,str,socket_errno);
    }
}
#else
Socket::Error Socket::connectError(void)
{
    char* str = (char *)"Could not connect to remote host";
    switch(errno) {
#ifdef  EHOSTUNREACH
    case EHOSTUNREACH:
        return error(errConnectNoRoute,str,socket_errno);
#endif
#ifdef  ENETUNREACH
    case ENETUNREACH:
        return error(errConnectNoRoute,str,socket_errno);
#endif
    case EINPROGRESS:
        return error(errConnectBusy,str,socket_errno);
#ifdef  EADDRNOTAVAIL
    case EADDRNOTAVAIL:
        return error(errConnectInvalid,str,socket_errno);
#endif
    case ECONNREFUSED:
        return error(errConnectRefused,str,socket_errno);
    case ETIMEDOUT:
        return error(errConnectTimeout,str,socket_errno);
    default:
        return error(errConnectFailed,str,socket_errno);
    }
}
#endif

Socket::Error Socket::sendLimit(int limit)
{
#ifdef  SO_SNDLOWAT
    if(setsockopt(so, SOL_SOCKET, SO_SNDLOWAT, (char *)&limit, sizeof(limit)))
        return errInvalidValue;

    return errSuccess;

#else
    return errServiceUnavailable;
#endif
}

Socket::Error Socket::receiveLimit(int limit)
{
#ifdef  SO_RCVLOWAT
    if(setsockopt(so, SOL_SOCKET, SO_RCVLOWAT, (char *)&limit, sizeof(limit)))
        return errInvalidValue;

    return errSuccess;

#else
    return errServiceUnavailable;
#endif
}

Socket::Error Socket::sendTimeout(timeout_t to)
{
#ifdef  SO_SNDTIMEO
    struct timeval tv;

    tv.tv_sec = to / 1000;
    tv.tv_usec = (to % 1000) * 1000;

    if(setsockopt(so, SOL_SOCKET, SO_SNDTIMEO, (char *)&tv, sizeof(tv)))
        return errInvalidValue;

    return errSuccess;

#else
    return errServiceUnavailable;
#endif
}

Socket::Error Socket::receiveTimeout(timeout_t to)
{
#ifdef  SO_RCVTIMEO
    struct timeval tv;

    tv.tv_sec = to / 1000;
    tv.tv_usec = (to % 1000) * 1000;

    if(setsockopt(so, SOL_SOCKET, SO_RCVTIMEO, (char *)&tv, sizeof(tv)))
        return errInvalidValue;

    return errSuccess;
#else
    return errServiceUnavailable;
#endif
}

Socket::Error Socket::sendBuffer(unsigned bufsize)
{
#ifdef  SO_SNDBUF
    if(setsockopt(so, SOL_SOCKET, SO_SNDBUF, (char *)&bufsize, sizeof(bufsize)))
        return errInvalidValue;
    return errSuccess;
#else
    return errServiceUnavailable;
#endif
}

Socket::Error Socket::bufferSize(unsigned bufsize)
{
    Error err = receiveBuffer(bufsize);
    if(err == errSuccess)
        err = sendBuffer(bufsize);

    return err;
}

Socket::Error Socket::receiveBuffer(unsigned bufsize)
{
#ifdef  SO_RCVBUF
    if(setsockopt(so, SOL_SOCKET, SO_RCVBUF, (char *)&bufsize, sizeof(bufsize)))
        return errInvalidValue;
    return errSuccess;
#else
    return errServiceUnavailable;
#endif
}

Socket::Error Socket::setBroadcast(bool enable)
{
    int opt = (enable ? 1 : 0);
    if(setsockopt(so, SOL_SOCKET, SO_BROADCAST,
              (char *)&opt, (socklen_t)sizeof(opt)))
        return error(errBroadcastDenied,(char *)"Could not set socket broadcast option",socket_errno);
    flags.broadcast = enable;
    return errSuccess;
}

Socket::Error Socket::setMulticastByFamily(bool enable, Family family)
{
    socklen_t len;

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        struct sockaddr_in6 addr;
        len = sizeof(addr);

        if(enable == flags.multicast)
            return errSuccess;

        flags.multicast = enable;
        if(enable)
            getsockname(so, (struct sockaddr *)&addr, &len);
        else
            memset(&addr.sin6_addr, 0, sizeof(addr.sin6_addr));

        setsockopt(so, IPPROTO_IPV6, IPV6_MULTICAST_IF, (char *)&addr.sin6_addr, sizeof(addr.sin6_addr));
        return errSuccess;
#endif
    case IPV4:
#ifdef  IP_MULTICAST_IF
        struct sockaddr_in addr4;
        len = sizeof(addr4);

        if(enable == flags.multicast)
            return errSuccess;

        flags.multicast = enable;
        if(enable)
            getsockname(so, (struct sockaddr *)&addr4, &len);
        else
            memset(&addr4.sin_addr, 0, sizeof(addr4.sin_addr));

        setsockopt(so, IPPROTO_IP, IP_MULTICAST_IF, (char *)&addr4.sin_addr, sizeof(addr4.sin_addr));
        return errSuccess;
#endif
    default:
        return error(errServiceUnavailable,(char *)"Multicast not supported");
    }
}

Socket::Error Socket::setTimeToLiveByFamily(unsigned char ttl, Family fam)
{
    if(!flags.multicast)
        return error(errMulticastDisabled,(char *)"Multicast not enabled on socket");

    switch(fam) {
#ifdef  CCXX_IPV6
    case IPV6:
        flags.ttl = ttl;
        setsockopt(so, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, (char *)&ttl, sizeof(ttl));
        return errSuccess;
#endif
    case IPV4:
#ifdef  IP_MULTICAST_TTL
        flags.ttl = ttl;
        setsockopt(so, IPPROTO_IP, IP_MULTICAST_TTL, (char *)&ttl, sizeof(ttl));
        return errSuccess;
#endif
    default:
        return error(errServiceUnavailable, (char *)"Multicast not supported");
    }
}

Socket::Error Socket::setLoopbackByFamily(bool enable, Family family)
{
    unsigned char loop;

    if(!flags.multicast)
        return error(errMulticastDisabled,(char *)"Multicast not enabled on socket");

    if(enable)
        loop = 1;
    else
        loop = 0;
    flags.loopback = enable;

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        setsockopt(so, IPPROTO_IPV6, IPV6_MULTICAST_LOOP, (char *)&loop, sizeof(loop));
        return errSuccess;
#endif
    case IPV4:
#ifdef  IP_MULTICAST_LOOP
        setsockopt(so, IPPROTO_IP, IP_MULTICAST_LOOP, (char *)&loop, sizeof(loop));
        return errSuccess;
#endif
    default:
        return error(errServiceUnavailable,(char *)"Multicast not supported");
    }
}

Socket::Error Socket::join(const IPV4Multicast &ia)
{
#ifdef  IP_ADD_MEMBERSHIP
    struct ip_mreq group;
    struct sockaddr_in myaddr;
    socklen_t len = sizeof(myaddr);

    if(!flags.multicast)
        return error(errMulticastDisabled,(char *)"Multicast not enabled on socket");

    getsockname(so, (struct sockaddr *)&myaddr, &len);
    group.imr_interface.s_addr = INADDR_ANY;
    group.imr_multiaddr = ia.getAddress();
    setsockopt(so, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char *)&group, sizeof(group));
    return errSuccess;
#else
    return error(errServiceUnavailable,(char *)"Multicast not supported");
#endif
}

#ifdef  CCXX_IPV6
Socket::Error Socket::join(const IPV6Multicast &ia)
{
#ifdef  IPV6_ADD_MEMBERSHIP
    struct ipv6_mreq group;
    struct sockaddr_in6 myaddr;
    socklen_t len = sizeof(myaddr);

    if(!flags.multicast)
        return error(errMulticastDisabled,(char *)"Multicast not enabled on socket");

    getsockname(so, (struct sockaddr *)&myaddr, &len);
    group.ipv6mr_interface = 0;
    group.ipv6mr_multiaddr = ia.getAddress();
    setsockopt(so, IPPROTO_IPV6, IPV6_ADD_MEMBERSHIP, (char *)&group, sizeof(group));
    return errSuccess;
#else
    return error(errServiceUnavailable,(char *)"Multicast not supported");
#endif
}

#endif

Socket::Error Socket::drop(const IPV4Multicast &ia)
{
#ifdef  IP_DROP_MEMBERSHIP
    struct ip_mreq group;
    struct sockaddr_in myaddr;
    socklen_t len = sizeof(myaddr);

    if(!flags.multicast)
        return error(errMulticastDisabled,(char *)"Multicast not enabled on socket");

    getsockname(so, (struct sockaddr *)&myaddr, &len);
    group.imr_interface.s_addr = INADDR_ANY;
    group.imr_multiaddr = ia.getAddress();
    setsockopt(so, IPPROTO_IP, IP_DROP_MEMBERSHIP, (char *)&group, sizeof(group));
    return errSuccess;
#else
    return error(errServiceUnavailable,(char *)"Multicast not supported");
#endif
}

#ifdef  CCXX_IPV6
Socket::Error Socket::drop(const IPV6Multicast &ia)
{
#ifdef  IPV6_DROP_MEMBERSHIP
    struct ipv6_mreq group;
    struct sockaddr_in6 myaddr;
    socklen_t len = sizeof(myaddr);

    if(!flags.multicast)
        return error(errMulticastDisabled,(char *)"Multicast not enabled on socket");

    getsockname(so, (struct sockaddr *)&myaddr, &len);
    group.ipv6mr_interface = 0;
    group.ipv6mr_multiaddr = ia.getAddress();
    setsockopt(so, IPPROTO_IPV6, IPV6_DROP_MEMBERSHIP, (char *)&group, sizeof(group));
    return errSuccess;
#else
    return error(errServiceUnavailable,(char *)"Multicast not supported");
#endif
}
#endif

Socket::Error Socket::setRouting(bool enable)
{
    int opt = (enable ? 1 : 0);

#ifdef  SO_DONTROUTE
    if(setsockopt(so, SOL_SOCKET, SO_DONTROUTE,
              (char *)&opt, (socklen_t)sizeof(opt)))
        return error(errRoutingDenied,(char *)"Could not set dont-route socket option",socket_errno);
#endif
    flags.route = enable;
    return errSuccess;
}

Socket::Error Socket::setNoDelay(bool enable)
{
    int opt = (enable ? 1 : 0);

    if(setsockopt(so, IPPROTO_TCP, TCP_NODELAY,
              (char *)&opt, (socklen_t)sizeof(opt)))
        return error(errNoDelay,(char *)"Could not set tcp-nodelay socket option",socket_errno);


    return errSuccess;
}

ssize_t Socket::readLine(char *str, size_t request, timeout_t timeout)
{
    bool crlf = false;
    bool nl = false;
    size_t nleft = request - 1; // leave also space for terminator
    int nstat,c;

    if(request < 1)
        return 0;

    str[0] = 0;

    while(nleft && !nl) {
        if(timeout) {
            if(!isPending(pendingInput, timeout)) {
                error(errTimeout,(char *)"Read timeout", 0);
                return -1;
            }
        }
        nstat = ::recv(so, str, _IOLEN64 nleft, MSG_PEEK);
        if(nstat <= 0) {
            error(errInput,(char *)"Could not read from socket", socket_errno);
            return -1;
        }

        // FIXME: if unique char in buffer is '\r' return "\r"
        //        if buffer end in \r try to read another char?
        //        and if timeout ??
        //        remember last \r

        for(c=0; c < nstat; ++c) {
            if(str[c] == '\n') {
                if (c > 0 && str[c-1] == '\r')
                    crlf = true;
                ++c;
                nl = true;
                break;
            }
        }
        nstat = ::recv(so, str, _IOLEN64 c, 0);
        // TODO: correct ???
        if(nstat < 0)
            break;

        // adjust ending \r\n in \n
        if(crlf) {
            --nstat;
            str[nstat - 1] = '\n';
        }

        str += nstat;
        nleft -= nstat;
    }
    *str = 0;
    return (ssize_t)(request - nleft - 1);
}

ssize_t Socket::readData(void *Target, size_t Size, char Separator, timeout_t timeout)
{
    if ((Separator == 0x0D) || (Separator == 0x0A))
        return (readLine ((char *) Target, Size, timeout));

    if (Size < 1)
        return (0);

    ssize_t nstat;

    if (Separator == 0) {       // Flat-out read for a number of bytes.
        if (timeout) {
            if (!isPending (pendingInput, timeout)) {
                error(errTimeout);
                return (-1);
            }
        }
        nstat =::recv (so, (char *)Target, _IOLEN64 Size, 0);

        if (nstat < 0) {
            error (errInput);
            return (-1);
        }
        return (nstat);
    }
    /////////////////////////////////////////////////////////////
    // Otherwise, we have a special char separator to use
    /////////////////////////////////////////////////////////////
    bool found = false;
    size_t nleft = Size;
    int c;
    char *str = (char *) Target;

    memset (str, 0, Size);

    while (nleft && !found) {
        if (timeout) {
            if (!isPending (pendingInput, timeout)) {
                error(errTimeout);
                return (-1);
            }
        }
        nstat =::recv (so, str, _IOLEN64 nleft, MSG_PEEK);
        if (nstat <= 0) {
            error (errInput);
            return (-1);
        }

        for (c = 0; (c < nstat) && !found; ++c) {
            if (str[c] == Separator)
                found = true;
        }

        memset (str, 0, nleft);
        nstat =::recv (so, str, c, 0);
        if (nstat < 0)
            break;

        str += nstat;
        nleft -= nstat;
    }
    return (ssize_t)(Size - (ssize_t) nleft);
}

ssize_t Socket::writeData(const void *Source, size_t Size, timeout_t timeout)
{
    if (Size < 1)
        return (0);

    ssize_t nstat;
    const char *Slide = (const char *) Source;

    while (true) {
        if (timeout) {
            if (!isPending (pendingOutput, timeout)) {
                error(errOutput);
                return (-1);
            }
        }
        nstat =::send (so, Slide, _IOLEN64 Size, MSG_NOSIGNAL);

        if (nstat <= 0) {
            error(errOutput);
            return (-1);
        }
        Size -= nstat;
        Slide += nstat;


        if (Size <= 0)
            break;
    }
    return (nstat);
}

const char *Socket::getSystemErrorString(void) const
{
#ifdef  CCXX_EXCEPTIONS
    SockException e(errstr, errid, syserr);
    return e.getSystemErrorString();
#else
    return NULL;
#endif
}

bool Socket::isPending(Pending pending, timeout_t timeout)
{
    int status = 0;
#ifdef USE_POLL
    struct pollfd pfd;

    pfd.fd = so;
    pfd.revents = 0;

    if(so == INVALID_SOCKET)
        return true;

    switch(pending) {
    case pendingInput:
        pfd.events = POLLIN;
        break;
    case pendingOutput:
        pfd.events = POLLOUT;
        break;
    case pendingError:
        pfd.events = POLLERR | POLLHUP;
        break;
    }

    status = 0;
    while(status < 1) {

        if(timeout == TIMEOUT_INF)
            status = poll(&pfd, 1, -1);
        else
            status = poll(&pfd, 1, timeout);

        if(status < 1) {
            // don't stop polling because of a simple
            // signal :)
            if(status == -1 && errno == EINTR)
                continue;

            return false;
        }
    }

    if(pfd.revents & pfd.events)
        return true;
    return false;
#else
    struct timeval tv;
    struct timeval *tvp = &tv;
    fd_set grp;

    if(timeout == TIMEOUT_INF)
        tvp = NULL;
    else {
        tv.tv_usec = (timeout % 1000) * 1000;
        tv.tv_sec = timeout / 1000;
    }

    FD_ZERO(&grp);
    SOCKET sosave = so;
    if(so == INVALID_SOCKET)
        return true;
    FD_SET(sosave, &grp);
    switch(pending) {
    case pendingInput:
        status = ::select((int)so + 1, &grp, NULL, NULL, tvp);
        break;
    case pendingOutput:
        status = ::select((int)so + 1, NULL, &grp, NULL, tvp);
        break;
    case pendingError:
        status = ::select((int)so + 1, NULL, NULL, &grp, tvp);
        break;
    }
    if(status < 1)
        return false;
    if(FD_ISSET(so, &grp))
        return true;
    return false;
#endif
}

bool Socket::operator!() const
{
    return (Socket::state == INITIAL) ? true : false;
}

Socket::operator bool() const
{
    return (Socket::state == INITIAL) ? false : true;
}

Socket &Socket::operator=(const Socket &from)
{
    if(so == from.so)
        return *this;

    if(state != INITIAL)
        endSocket();

    so = dupSocket(from.so,from.state);
    if(so == INVALID_SOCKET) {
        error(errCopyFailed,(char *)"Could not duplicate socket handle",socket_errno);
        state = INITIAL;
    }
    else
        state = from.state;

    return *this;
}

bool Socket::check(Family fam)
{
    SOCKET so = INVALID_SOCKET;

    switch(fam) {
    case IPV4:
        so = socket(fam, SOCK_DGRAM, IPPROTO_UDP);
        break;
#ifdef  CCXX_IPV6
    case IPV6:
        so = socket(fam, SOCK_DGRAM, IPPROTO_UDP);
        break;
#endif
    default:
        return false;
    }

    if(so == INVALID_SOCKET)
        return false;

    release(so);
    return true;
}

IPV4Host Socket::getIPV4Sender(tpport_t *port) const
{
    struct sockaddr_in from;
    char buf;
    socklen_t len = sizeof(from);
    int rc = ::recvfrom(so, &buf, 1, MSG_PEEK,
                (struct sockaddr *)&from, &len);

#ifdef _MSWINDOWS_
    if(rc < 1 && WSAGetLastError() != WSAEMSGSIZE) {
        if(port)
            *port = 0;

        memset((void*) &from, 0, sizeof(from));
        error(errInput,(char *)"Could not read from socket",socket_errno);
    }
#else
    if(rc < 0) {
        if(port)
            *port = 0;

        memset((void*) &from, 0, sizeof(from));
        error(errInput,(char *)"Could not read from socket",socket_errno);
    }
#endif
    else {
        if(rc < 1)
            memset((void*) &from, 0, sizeof(from));

        if(port)
            *port = ntohs(from.sin_port);
    }

    return IPV4Host(from.sin_addr);
}

#ifdef  CCXX_IPV6
IPV6Host Socket::getIPV6Sender(tpport_t *port) const
{
    struct sockaddr_in6 from;
    char buf;
    socklen_t len = sizeof(from);
    int rc = ::recvfrom(so, &buf, 1, MSG_PEEK,
                (struct sockaddr *)&from, &len);

#ifdef _MSWINDOWS_
    if(rc < 1 && WSAGetLastError() != WSAEMSGSIZE) {
        if(port)
            *port = 0;

        memset((void*) &from, 0, sizeof(from));
        error(errInput,(char *)"Could not read from socket",socket_errno);
    }
#else
    if(rc < 0) {
        if(port)
            *port = 0;

        memset((void*) &from, 0, sizeof(from));
        error(errInput,(char *)"Could not read from socket",socket_errno);
    }
#endif
    else {
        if(rc < 1)
            memset((void*) &from, 0, sizeof(from));

        if(port)
            *port = ntohs(from.sin6_port);
    }

    return IPV6Host(from.sin6_addr);
}
#endif

IPV4Host Socket::getIPV4Local(tpport_t *port) const
{
    struct sockaddr_in addr;
    socklen_t len = sizeof(addr);

    if(getsockname(so, (struct sockaddr *)&addr, &len)) {
        error(errResourceFailure,(char *)"Could not get socket address",socket_errno);
            if(port)
            *port = 0;
        memset(&addr.sin_addr, 0, sizeof(addr.sin_addr));
    }
    else {
        if(port)
                *port = ntohs(addr.sin_port);
    }
    return IPV4Host(addr.sin_addr);
}

#ifdef  CCXX_IPV6
IPV6Host Socket::getIPV6Local(tpport_t *port) const
{
    struct sockaddr_in6 addr;
    socklen_t len = sizeof(addr);

    if(getsockname(so, (struct sockaddr *)&addr, &len)) {
        error(errResourceFailure,(char *)"Could not get socket address",socket_errno);
            if(port)
            *port = 0;
        memset(&addr.sin6_addr, 0, sizeof(addr.sin6_addr));
    }
    else {
        if(port)
                *port = ntohs(addr.sin6_port);
    }
    return IPV6Host(addr.sin6_addr);
}
#endif

IPV4Host Socket::getIPV4Peer(tpport_t *port) const
{
    struct sockaddr_in addr;
    socklen_t len = sizeof(addr);

    if(getpeername(so, (struct sockaddr *)&addr, &len)) {
#ifndef _MSWINDOWS_
        if(errno == ENOTCONN)
            error(errNotConnected,(char *)"Could not get peer address",socket_errno);
        else
#endif
            error(errResourceFailure,(char *)"Could not get peer address",socket_errno);
        if(port)
            *port = 0;
        memset(&addr.sin_addr, 0, sizeof(addr.sin_addr));
    }
    else {
        if(port)
                *port = ntohs(addr.sin_port);
    }
    return IPV4Host(addr.sin_addr);
}

#ifdef  CCXX_IPV6
IPV6Host Socket::getIPV6Peer(tpport_t *port) const
{
    struct sockaddr_in6 addr;
    socklen_t len = sizeof(addr);

    if(getpeername(so, (struct sockaddr *)&addr, &len)) {
#ifndef _MSWINDOWS_
        if(errno == ENOTCONN)
            error(errNotConnected,(char *)"Could not get peer address",socket_errno);
        else
#endif
            error(errResourceFailure,(char *)"Could not get peer address",socket_errno);
        if(port)
            *port = 0;
        memset(&addr.sin6_addr, 0, sizeof(addr.sin6_addr));
    }
    else {
        if(port)
                *port = ntohs(addr.sin6_port);
    }
    return IPV6Host(addr.sin6_addr);
}
#endif

void Socket::setCompletion(bool immediate)
{
    flags.completion = immediate;
#ifdef _MSWINDOWS_
    unsigned long flag;
    // note that this will not work on some versions of Windows for Workgroups. Tough. -- jfc
    switch( immediate ) {
    case false:
        // this will not work if you are using WSAAsyncSelect or WSAEventSelect.
        // -- perhaps I should throw an exception
        flag = 1;
//      ioctlsocket( so, FIONBIO, (unsigned long *) 1);
        break;
    case true:
        flag = 0;
//      ioctlsocket( so, FIONBIO, (unsigned long *) 0);
        break;
    }
    ioctlsocket(so, FIONBIO, &flag);
#else
    int fflags = fcntl(so, F_GETFL);

    if(immediate) {
        fflags &=~ O_NONBLOCK;
        fcntl(so, F_SETFL, fflags);
    }
    else {
        fflags |= O_NONBLOCK;
        fcntl(so, F_SETFL, fflags);
    }
#endif
}

Socket::Error Socket::setKeepAlive(bool enable)
{
    int opt = (enable ? ~0: 0);
#if (defined(SO_KEEPALIVE) || defined(_MSWINDOWS_))
    if(setsockopt(so, SOL_SOCKET, SO_KEEPALIVE,
              (char *)&opt, (socklen_t)sizeof(opt)))
        return error(errKeepaliveDenied,(char *)"Could not set socket keep-alive option",socket_errno);
#endif
    flags.keepalive = enable;
    return errSuccess;
}

Socket::Error Socket::setLinger(bool linger)
{
#ifdef  SO_LINGER
    flags.linger = linger;
    return errSuccess;
#else
    return error(errServiceUnavailable,(char *)"Socket lingering not supported");
#endif
}

Socket::Error Socket::setTypeOfService(Tos service)
{
#ifdef  SOL_IP
    unsigned char tos;
    switch(service) {
#ifdef  IPTOS_LOWDELAY
    case tosLowDelay:
        tos = IPTOS_LOWDELAY;
        break;
#endif
#ifdef  IPTOS_THROUGHPUT
    case tosThroughput:
        tos = IPTOS_THROUGHPUT;
        break;
#endif
#ifdef  IPTOS_RELIABILITY
    case tosReliability:
        tos = IPTOS_RELIABILITY;
        break;
#endif
#ifdef  IPTOS_MINCOST
    case tosMinCost:
        tos = IPTOS_MINCOST;
        break;
#endif
    default:
        return error(errServiceUnavailable,(char *)"Unknown type-of-service");
    }
    if(setsockopt(so, SOL_IP, IP_TOS,(char *)&tos, (socklen_t)sizeof(tos)))
        return error(errServiceDenied,(char *)"Could not set type-of-service",socket_errno);
    return errSuccess;
#else
    return error(errServiceUnavailable,(char *)"Socket type-of-service not supported",socket_errno);
#endif
}

bool Socket::isConnected(void) const
{
    return (Socket::state == CONNECTED) ? true : false;
}

bool Socket::isActive(void) const
{
    return (state != INITIAL) ? true : false;
}

