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
#include <commoncpp/string.h>
#include <commoncpp/socket.h>
#include <commoncpp/tcp.h>

#include <iostream>

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
using namespace std;

void TCPSocket::setSegmentSize(unsigned mss)
{
#ifdef  TCP_MAXSEG
    if(mss > 1)
        setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&mss, sizeof(mss));
#endif
    segsize = mss;
}

#ifdef  HAVE_GETADDRINFO
TCPSocket::TCPSocket(const char *name, unsigned backlog, unsigned mss) :
Socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
{
    char namebuf[128], *cp;
    struct addrinfo hint, *list = NULL, *first;
    snprintf(namebuf, sizeof(namebuf), "%s", name);
    cp = strrchr(namebuf, '/');
    if(!cp)
        cp = strrchr(namebuf, ':');

    if(!cp) {
        cp = namebuf;
        name = NULL;
    }
    else {
        name = namebuf;
        *(cp++) = 0;
        if(!strcmp(name, "*"))
            name = NULL;
    }

    memset(&hint, 0, sizeof(hint));
    hint.ai_family = AF_INET;
    hint.ai_socktype = SOCK_STREAM;
    hint.ai_protocol = IPPROTO_TCP;
    hint.ai_flags = AI_PASSIVE;

    if(getaddrinfo(name, cp, &hint, &list) || !list) {
        endSocket();
        error(errBindingFailed, (char *)"Could not find service", errno);
        return;
    }

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt,
        (socklen_t)sizeof(opt));
#endif

    first = list;
    while(list) {
        if(!bind(so, list->ai_addr, (socklen_t)list->ai_addrlen)) {
            state = BOUND;
            break;
        }
        list = list->ai_next;
    }
    freeaddrinfo(first);
    if(state != BOUND) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }

    setSegmentSize(mss);
    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",socket_errno);
        return;
    }
}

#else
TCPSocket::TCPSocket(const char *name, unsigned backlog, unsigned mss) :
Socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
{
    char namebuf[128], *cp;
    struct sockaddr_in addr;
    struct servent *svc;

    memset(&addr, 0, sizeof(addr));
    snprintf(namebuf, sizeof(namebuf), "%s", name);
    cp = strrchr(namebuf, '/');
    if(!cp)
        cp = strrchr(namebuf, ':');

    if(!cp) {
        cp = namebuf;
        name = "*";
    }
    else {
        name = namebuf;
        *(cp++) = 0;
    }

    addr.sin_family = AF_INET;
    if(isdigit(*cp))
        addr.sin_port = htons(atoi(cp));
    else {
        mutex.enter();
        svc = getservbyname(cp, "tcp");
        if(svc)
            addr.sin_port = svc->s_port;
        mutex.leave();
        if(!svc) {
                    endSocket();
                    error(errBindingFailed, "Could not find service", errno);
                    return;

        }
    }

    IPV4Address ia(name);
    addr.sin_addr = getaddress(ia);

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt,
        (socklen_t)sizeof(opt));
#endif

    if(bind(so, (struct sockaddr *)&addr, sizeof(addr))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }

    setSegmentSize(mss);
    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",
            socket_errno);
        return;
    }
    state = BOUND;
}

#endif

TCPSocket::TCPSocket(const IPV4Address &ia, tpport_t port, unsigned backlog, unsigned mss) :
Socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
{
    struct sockaddr_in addr;

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr = getaddress(ia);
    addr.sin_port = htons(port);

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt, (socklen_t)sizeof(opt));
#endif
    if(bind(so, (struct sockaddr *)&addr, sizeof(addr))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }

    setSegmentSize(mss);

    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",socket_errno);
        return;
    }
    state = BOUND;
}

bool TCPSocket::onAccept(const IPV4Host &ia, tpport_t port)
{
    return true;
}

TCPSocket::~TCPSocket()
{
    endSocket();
}

#ifdef  CCXX_IPV6

void TCPV6Socket::setSegmentSize(unsigned mss)
{
#ifdef  TCP_MAXSEG
    if(mss > 1)
        setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&mss, sizeof(mss));
#endif
    segsize = mss;
}

TCPV6Socket::TCPV6Socket(const char *name, unsigned backlog, unsigned mss) :
Socket(AF_INET6, SOCK_STREAM, IPPROTO_TCP)
{
    char namebuf[128], *cp;
    struct addrinfo hint, *list = NULL, *first;

    snprintf(namebuf, sizeof(namebuf), "%s", name);
    cp = strrchr(namebuf, '/');

    if(!cp) {
        cp = namebuf;
        name = NULL;
    }
    else {
        name = namebuf;
        *(cp++) = 0;
        if(!strcmp(name, "*"))
            name = NULL;
    }

    memset(&hint, 0, sizeof(hint));
    hint.ai_family = AF_INET6;
    hint.ai_socktype = SOCK_STREAM;
    hint.ai_protocol = IPPROTO_TCP;
    hint.ai_flags = AI_PASSIVE;

    if(getaddrinfo(name, cp, &hint, &list) || !list) {
        endSocket();
        error(errBindingFailed, (char *)"Could not find service", errno);
        return;
    }

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt,
        (socklen_t)sizeof(opt));
#endif

    first = list;
    while(list) {
        if(!bind(so, list->ai_addr, (socklen_t)list->ai_addrlen)) {
            state = BOUND;
            break;
        }
        list = list->ai_next;
    }
    freeaddrinfo(first);
    if(state != BOUND) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }

    setSegmentSize(mss);

    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",socket_errno);
        return;
    }
}

TCPV6Socket::TCPV6Socket(const IPV6Address &ia, tpport_t port, unsigned backlog, unsigned mss) :
Socket(AF_INET6, SOCK_STREAM, IPPROTO_TCP)
{
    struct sockaddr_in6 addr;

    memset(&addr, 0, sizeof(addr));
    addr.sin6_family = AF_INET6;
    addr.sin6_addr = getaddress(ia);
    addr.sin6_port = htons(port);

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt, (socklen_t)sizeof(opt));
#endif
    if(bind(so, (struct sockaddr *)&addr, sizeof(addr))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }

    setSegmentSize(mss);

    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",socket_errno);
        return;
    }
    state = BOUND;
}

bool TCPV6Socket::onAccept(const IPV6Host &ia, tpport_t port)
{
    return true;
}

TCPV6Socket::~TCPV6Socket()
{
    endSocket();
}

#endif

void TCPSocket::reject(void)
{
    SOCKET rej = accept(so, NULL, NULL);
    ::shutdown(rej, 2);
    release(rej);
}

#ifdef  CCXX_IPV6
void TCPV6Socket::reject(void)
{
    SOCKET rej = accept(so, NULL, NULL);
    ::shutdown(rej, 2);
    release(rej);
}
#endif

TCPStream::TCPStream(TCPSocket &server, bool throwflag, timeout_t to) :
    streambuf(), Socket(accept(server.getSocket(), NULL, NULL)),
#ifdef  OLD_IOSTREAM
    iostream()
#else
    iostream((streambuf *)this)
#endif
    ,bufsize(0)
    ,gbuf(NULL)
    ,pbuf(NULL) {
    tpport_t port;
    family = IPV4;

#ifdef  OLD_IOSTREAM
    init((streambuf *)this);
#endif

    timeout = to;
    setError(throwflag);
    IPV4Host host = getPeer(&port);
    if(!server.onAccept(host, port)) {
        endSocket();
        error(errConnectRejected);
        iostream::clear(ios::failbit | rdstate());
        return;
    }

    segmentBuffering(server.getSegmentSize());
    Socket::state = CONNECTED;
}

#ifdef  CCXX_IPV6
TCPStream::TCPStream(TCPV6Socket &server, bool throwflag, timeout_t to) :
    streambuf(), Socket(accept(server.getSocket(), NULL, NULL)),
#ifdef  OLD_IOSTREAM
    iostream()
#else
    iostream((streambuf *)this)
#endif
    ,bufsize(0)
    ,gbuf(NULL)
    ,pbuf(NULL) {
    tpport_t port;

    family = IPV6;

#ifdef  OLD_IOSTREAM
    init((streambuf *)this);
#endif

    timeout = to;
    setError(throwflag);
    IPV6Host host = getIPV6Peer(&port);
    if(!server.onAccept(host, port)) {
        endSocket();
        error(errConnectRejected);
        iostream::clear(ios::failbit | rdstate());
        return;
    }

    segmentBuffering(server.getSegmentSize());
    Socket::state = CONNECTED;
}
#endif

TCPStream::TCPStream(const IPV4Host &host, tpport_t port, unsigned size, bool throwflag, timeout_t to) :
    streambuf(), Socket(AF_INET, SOCK_STREAM, IPPROTO_TCP),
#ifdef  OLD_IOSTREAM
    iostream(),
#else
    iostream((streambuf *)this),
#endif
    bufsize(0),gbuf(NULL),pbuf(NULL) {
#ifdef  OLD_IOSTREAM
    init((streambuf *)this);
#endif
    family = IPV4;
    timeout = to;
    setError(throwflag);
    connect(host, port, size);
}

#ifdef  CCXX_IPV6
TCPStream::TCPStream(const IPV6Host &host, tpport_t port, unsigned size, bool throwflag, timeout_t to) :
    streambuf(), Socket(AF_INET6, SOCK_STREAM, IPPROTO_TCP),
#ifdef OLD_IOSTREAM
    iostream(),
#else
    iostream((streambuf *)this),
#endif
    bufsize(0),gbuf(NULL),pbuf(NULL) {
    family = IPV6;

#ifdef  OLD_IOSTREAM
    init((streambuf *)this);
#endif
    timeout = to;
    setError(throwflag);
    connect(host, port, size);
}
#endif

TCPStream::~TCPStream()
{
#ifdef  CCXX_EXCEPTIONS
        try { endStream(); }
        catch( ... ) { if ( ! std::uncaught_exception()) throw;};
#else
        endStream();
#endif
}

#ifdef  HAVE_GETADDRINFO

void TCPStream::connect(const char *target, unsigned mss)
{
    char namebuf[128];
    char *cp;
    struct addrinfo hint, *list = NULL, *next, *first;
    bool connected = false;

    snprintf(namebuf, sizeof(namebuf), "%s", target);
    cp = strrchr(namebuf, '/');
    if(!cp)
        cp = strrchr(namebuf, ':');

    if(!cp) {
        endStream();
        connectError();
        return;
    }

    *(cp++) = 0;

    memset(&hint, 0, sizeof(hint));
    hint.ai_family = family;
    hint.ai_socktype = SOCK_STREAM;
    hint.ai_protocol = IPPROTO_TCP;

    if(getaddrinfo(namebuf, cp, &hint, &list) || !list) {
        endStream();
        connectError();
        return;
    }

    first = list;

#ifdef  TCP_MAXSEG
    if(mss)
        setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&mss, sizeof(mss));
#endif

    while(list) {
        if(!::connect(so, list->ai_addr, (socklen_t)list->ai_addrlen)) {
            connected = true;
            break;
        }
        next = list->ai_next;
        list = next;
    }

    freeaddrinfo(first);

    if(!connected) {
        endStream();
        connectError();
        return;
    }

    segmentBuffering(mss);
    Socket::state = CONNECTED;
}

#else
void TCPStream::connect(const char *target, unsigned mss)
{
    char namebuf[128];
    char *cp;
    struct servent *svc;
    tpport_t port;

    snprintf(namebuf, sizeof(namebuf), "%s", target);
    cp = strrchr(namebuf, '/');
    if(!cp)
        cp = strrchr(namebuf, ':');

    if(!cp) {
        endStream();
        connectError();
        return;
    }

    *(cp++) = 0;

    if(isdigit(*cp))
        port = atoi(cp);
    else {
        mutex.enter();
        svc = getservbyname(cp, "tcp");
        if(svc)
            port = ntohs(svc->s_port);
        mutex.leave();
        if(!svc) {
            endStream();
            connectError();
            return;
        }
    }

    switch(family) {
    case IPV4:
        connect(IPV4Host(namebuf), port, mss);
        break;
#ifdef  CCXX_IPV6
    case IPV6:
        connect(IPV6Host(namebuf), port, mss);
        break;
#endif
    default:
        endStream();
        connectError();
    }
}
#endif

void TCPStream::connect(const IPV4Host &host, tpport_t port, unsigned mss)
{
    size_t i;
    fd_set fds;
    struct timeval to;
    bool connected = false;
    int rtn;
    int sockopt;
    socklen_t len = sizeof(sockopt);

#ifdef  TCP_MAXSEG
    if(mss)
        setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&mss, sizeof(mss));
#endif

    for(i = 0 ; i < host.getAddressCount(); i++) {
        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr = host.getAddress(i);
        addr.sin_port = htons(port);

        if(timeout)
            setCompletion(false);

        // Win32 will crash if you try to connect to INADDR_ANY.
        if ( INADDR_ANY == addr.sin_addr.s_addr )
            addr.sin_addr.s_addr = INADDR_LOOPBACK;
        rtn = ::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr));
        if(!rtn) {
            connected = true;
            break;
        }

#ifndef _MSWINDOWS_
        if(errno == EINPROGRESS)
#else
        if(WSAGetLastError() == WSAEINPROGRESS)
#endif
        {
            FD_ZERO(&fds);
            FD_SET(so, &fds);
            to.tv_sec = timeout / 1000;
            to.tv_usec = timeout % 1000 * 1000;

            // timeout check for connect completion

            if(::select((int)so + 1, NULL, &fds, NULL, &to) < 1)
                continue;

            getsockopt(so, SOL_SOCKET, SO_ERROR, (char *)&sockopt, &len);
            if(!sockopt) {
                connected = true;
                break;
            }
            endSocket();
            so = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
            if(so == INVALID_SOCKET)
                break;
        }
    }

    setCompletion(true);
    if(!connected) {
        rtn = errno;
        endStream();
        errno = rtn;
        connectError();
        return;
    }

    segmentBuffering(mss);
    Socket::state = CONNECTED;
}

#ifdef  CCXX_IPV6
void TCPStream::connect(const IPV6Host &host, tpport_t port, unsigned mss)
{
    size_t i;
    fd_set fds;
    struct timeval to;
    bool connected = false;
    int rtn;
    int sockopt;
    socklen_t len = sizeof(sockopt);

#ifdef  TCP_MAXSEG
    if(mss)
        setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&mss, sizeof(mss));
#endif

    for(i = 0 ; i < host.getAddressCount(); i++) {
        struct sockaddr_in6 addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin6_family = AF_INET6;
        addr.sin6_addr = host.getAddress(i);
        addr.sin6_port = htons(port);

        if(timeout)
            setCompletion(false);

        // Win32 will crash if you try to connect to INADDR_ANY.
        if ( !memcmp(&addr.sin6_addr, &in6addr_any, sizeof(in6addr_any)))
            memcpy(&addr.sin6_addr, &in6addr_loopback, sizeof(in6addr_loopback));
        rtn = ::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr));
        if(!rtn) {
            connected = true;
            break;
        }

#ifndef _MSWINDOWS_
        if(errno == EINPROGRESS)
#else
        if(WSAGetLastError() == WSAEINPROGRESS)
#endif
        {
            FD_ZERO(&fds);
            FD_SET(so, &fds);
            to.tv_sec = timeout / 1000;
            to.tv_usec = timeout % 1000 * 1000;

            // timeout check for connect completion

            if(::select((int)so + 1, NULL, &fds, NULL, &to) < 1)
                continue;

            getsockopt(so, SOL_SOCKET, SO_ERROR, (char *)&sockopt, &len);
            if(!sockopt) {
                connected = true;
                break;
            }
            endSocket();
            so = socket(AF_INET6, SOCK_STREAM, IPPROTO_TCP);
            if(so == INVALID_SOCKET)
                break;
        }
    }

    setCompletion(true);
    if(!connected) {
        rtn = errno;
        endStream();
        errno = rtn;
        connectError();
        return;
    }

    segmentBuffering(mss);
    Socket::state = CONNECTED;
}
#endif

TCPStream::TCPStream(const char *target, Family fam, unsigned mss, bool throwflag, timeout_t to) :
streambuf(), Socket(PF_INET, SOCK_STREAM, IPPROTO_TCP),
#ifdef  OLD_IOSTREAM
iostream(),
#else
iostream((streambuf *)this),
#endif
timeout(to), bufsize(0),gbuf(NULL),pbuf(NULL)
{
    family = fam;
#ifdef  OLD_IOSTREAM
    init((streambuf *)this);
#endif
    setError(throwflag);
    connect(target, mss);
}

TCPStream::TCPStream(Family fam, bool throwflag, timeout_t to) :
streambuf(), Socket(PF_INET, SOCK_STREAM, IPPROTO_TCP),
#ifdef  OLD_IOSTREAM
iostream(),
#else
iostream((streambuf *)this),
#endif
timeout(to), bufsize(0),gbuf(NULL),pbuf(NULL)
{
    family = fam;
#ifdef  OLD_IOSTREAM
    init((streambuf *)this);
#endif
    setError(throwflag);
}

void TCPStream::connect(TCPSocket &tcpip)
{
    tpport_t port;

    endStream();
    family = IPV4;
    so = accept(tcpip.getSocket(), NULL, NULL);
    if(so == INVALID_SOCKET)
        return;

    IPV4Host host = getPeer(&port);
    if(!tcpip.onAccept(host, port)) {
        endSocket();
        iostream::clear(ios::failbit | rdstate());
        return;
    }

    segmentBuffering(tcpip.getSegmentSize());
    Socket::state = CONNECTED;
}

#ifdef  CCXX_IPV6

void TCPStream::connect(TCPV6Socket &tcpip)
{
    tpport_t port;

    endStream();
    family = IPV6;
    so = accept(tcpip.getSocket(), NULL, NULL);
    if(so == INVALID_SOCKET)
        return;

    IPV6Host host = getIPV6Peer(&port);
    if(!tcpip.onAccept(host, port)) {
        endSocket();
        iostream::clear(ios::failbit | rdstate());
        return;
    }

    segmentBuffering(tcpip.getSegmentSize());
        Socket::state = CONNECTED;
}
#endif

void TCPStream::segmentBuffering(unsigned mss)
{
    unsigned max = 0;

    if(mss == 1) {  // special interactive
        allocate(1);
        return;
    }

#ifdef  TCP_MAXSEG
    socklen_t alen = sizeof(max);

    if(mss)
        setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&max, sizeof(max));
    getsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&max, &alen);
#endif

    if(max && max < mss)
        mss = max;

    if(!mss) {
        if(max)
            mss = max;
        else
            mss = 536;
        allocate(mss);
        return;
    }

#ifdef  TCP_MAXSEG
    setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&mss, sizeof(mss));
#endif

    if(mss < 80)
        mss = 80;

    if(mss * 7 < 64000)
        bufferSize(mss * 7);
    else if(mss * 6 < 64000)
        bufferSize(mss * 6);
    else
        bufferSize(mss * 5);

    if(mss < 512)
        sendLimit(mss * 4);

    allocate(mss);
}

int TCPStream::getSegmentSize(void)
{
    unsigned mss = 0;
#ifdef  TCP_MAXSEG
    socklen_t alen = sizeof(mss);

    getsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&mss, &alen);
#endif
    if(!mss)
        return (int)bufsize;

    return mss;
}

void TCPStream::disconnect(void)
{
    if(Socket::state == AVAILABLE)
        return;

    endStream();
    so = socket(family, SOCK_STREAM, IPPROTO_TCP);
    if(so != INVALID_SOCKET)
        Socket::state = AVAILABLE;
}

void TCPStream::endStream(void)
{
    if(bufsize)
        sync();
    if(gbuf)
        delete[] gbuf;
    if(pbuf)
        delete[] pbuf;
    gbuf = pbuf = NULL;
    bufsize = 0;
    iostream::clear();
    endSocket();
}

void TCPStream::allocate(size_t size)
{
    if(size < 2) {
        bufsize = 1;
        gbuf = pbuf = 0;
        return;
    }

    gbuf = new char[size];
    pbuf = new char[size];
    if(!pbuf || !gbuf) {
        error(errResourceFailure, (char *)"Could not allocate socket stream buffers");
        return;
    }
    bufsize = size;
    iostream::clear();

#if (defined(__GNUC__) && (__GNUC__ < 3)) && !defined(_MSWINDOWS_) && !defined(STLPORT)
    setb(gbuf, gbuf + size, 0);
#endif
    setg(gbuf, gbuf + size, gbuf + size);
    setp(pbuf, pbuf + size);
}

int TCPStream::doallocate()
{
    if(bufsize)
        return 0;

    allocate(1);
    return 1;
}

int TCPStream::uflow()
{
    int ret = underflow();

    if (ret == EOF)
        return EOF;

    if (bufsize != 1)
        gbump(1);

    return ret;
}

int TCPStream::underflow()
{
    ssize_t rlen = 1;
    unsigned char ch;

    if(bufsize == 1) {
        if(Socket::state == STREAM)
            rlen = ::read((int)so, (char *)&ch, 1);
        else if(timeout && !Socket::isPending(pendingInput, timeout)) {
            iostream::clear(ios::failbit | rdstate());
            error(errTimeout,(char *)"Socket read timed out",socket_errno);
            return EOF;
        }
        else
            rlen = readData(&ch, 1);
        if(rlen < 1) {
            if(rlen < 0) {
                iostream::clear(ios::failbit | rdstate());
                error(errInput,(char *)"Could not read from socket",socket_errno);
            }
            return EOF;
        }
        return ch;
    }

    if(!gptr())
        return EOF;

    if(gptr() < egptr())
        return (unsigned char)*gptr();

    rlen = (ssize_t)((gbuf + bufsize) - eback());
    if(Socket::state == STREAM)
        rlen = ::read((int)so, (char *)eback(), _IOLEN64 rlen);
    else if(timeout && !Socket::isPending(pendingInput, timeout)) {
        iostream::clear(ios::failbit | rdstate());
        error(errTimeout,(char *)"Socket read timed out",socket_errno);
        return EOF;
    }
    else
        rlen = readData(eback(), rlen);
    if(rlen < 1) {
//      clear(ios::failbit | rdstate());
        if(rlen < 0)
                        error(errNotConnected,(char *)"Connection error",socket_errno);
        else {
            error(errInput,(char *)"Could not read from socket",socket_errno);
            iostream::clear(ios::failbit | rdstate());
        }
        return EOF;
    }
    error(errSuccess);

    setg(eback(), eback(), eback() + rlen);
    return (unsigned char) *gptr();
}

bool TCPStream::isPending(Pending pending, timeout_t timer)
{
    if(pending == pendingInput && in_avail())
        return true;
    else if(pending == pendingOutput)
        flush();

    return Socket::isPending(pending, timer);
}

int TCPStream::sync(void)
{
    overflow(EOF);
    setg(gbuf, gbuf + bufsize, gbuf + bufsize);
    return 0;
}

size_t TCPStream::printf(const char *format, ...)
{
    va_list args;
    size_t len;
    char *buf;

    va_start(args, format);
    overflow(EOF);
    len = pptr() - pbase();
    buf = pptr();
    vsnprintf(buf, len, format, args);
    va_end(args);
    len = strlen(buf);
    if(Socket::state == STREAM)
        return ::write((int)so, buf, _IOLEN64 len);
    else
        return writeData(buf, len);
}

int TCPStream::overflow(int c)
{
    unsigned char ch;
    ssize_t rlen, req;

    if(bufsize == 1) {
        if(c == EOF)
            return 0;

        ch = (unsigned char)(c);
        if(Socket::state == STREAM)
            rlen = ::write((int)so, (const char *)&ch, 1);
        else
            rlen = writeData(&ch, 1);
        if(rlen < 1) {
            if(rlen < 0) {
                iostream::clear(ios::failbit | rdstate());
                error(errOutput,(char *)"Could not write to socket",socket_errno);
            }
            return EOF;
        }
        else
            return c;
    }

    if(!pbase())
        return EOF;

    req = (ssize_t)(pptr() - pbase());
    if(req) {
        if(Socket::state == STREAM)
            rlen = ::write((int)so, (const char *)pbase(), req);
        else
            rlen = writeData(pbase(), req);
        if(rlen < 1) {
            if(rlen < 0) {
                iostream::clear(ios::failbit | rdstate());
                error(errOutput,(char *)"Could not write to socket",socket_errno);
            }
            return EOF;
        }
        req -= rlen;
    }

    // if write "partial", rebuffer remainder

    if(req)
//      memmove(pbuf, pptr() + rlen, req);
        memmove(pbuf, pbuf + rlen, req);
    setp(pbuf, pbuf + bufsize);
    pbump(req);

    if(c != EOF) {
        *pptr() = (unsigned char)c;
        pbump(1);
    }
    return c;
}

TCPSession::TCPSession(const IPV4Host &ia, tpport_t port, size_t size, int pri, size_t stack) :
Thread(pri, stack), TCPStream(IPV4)
{
    setCompletion(false);
    setError(false);
    allocate(size);

    size_t i;
    for(i = 0 ; i < ia.getAddressCount(); i++) {
        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr = ia.getAddress(i);
        addr.sin_port = htons(port);

        // Win32 will crash if you try to connect to INADDR_ANY.
        if ( INADDR_ANY == addr.sin_addr.s_addr )
            addr.sin_addr.s_addr = INADDR_LOOPBACK;
        if(::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr)) == 0)
            break;

#ifdef _MSWINDOWS_
        if(WSAGetLastError() == WSAEISCONN || WSAGetLastError() == WSAEWOULDBLOCK)
#else
        if(errno == EINPROGRESS)
#endif
        {
            Socket::state = CONNECTING;
            return;
        }
    }

    if(i == ia.getAddressCount()) {
        endSocket();
        Socket::state = INITIAL;
        return;
    }

    setCompletion(true);
    Socket::state = CONNECTED;
}

#ifdef  CCXX_IPV6
TCPSession::TCPSession(const IPV6Host &ia, tpport_t port, size_t size, int pri, size_t stack) :
Thread(pri, stack), TCPStream(IPV6)
{
    setCompletion(false);
    setError(false);
    allocate(size);

    size_t i;
    for(i = 0 ; i < ia.getAddressCount(); i++) {
        struct sockaddr_in6 addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin6_family = AF_INET6;
        addr.sin6_addr = ia.getAddress(i);
        addr.sin6_port = htons(port);

        // Win32 will crash if you try to connect to INADDR_ANY.
        if(!memcmp(&addr.sin6_addr, &in6addr_any, sizeof(in6addr_any)))
            memcpy(&addr.sin6_addr, &in6addr_loopback, sizeof(in6addr_loopback));
        if(::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr)) == 0)
            break;

#ifdef _MSWINDOWS_
//      if(WSAGetLastError() == WSAEWOULDBLOCK)
        if(WSAGetLastError() == WSAEISCONN)
#else
        if(errno == EINPROGRESS)
#endif
        {
            Socket::state = CONNECTING;
            return;
        }
    }

    if(i == ia.getAddressCount()) {
        endSocket();
        Socket::state = INITIAL;
        return;
    }

    setCompletion(true);
    Socket::state = CONNECTED;
}
#endif

TCPSession::TCPSession(TCPSocket &s, int pri, size_t stack) :
Thread(pri, stack), TCPStream(s)
{
    setCompletion(true);
    setError(false);
}

#ifdef  CCXX_IPV6
TCPSession::TCPSession(TCPV6Socket &s, int pri, size_t stack) :
Thread(pri, stack), TCPStream(s)
{
    setCompletion(true);
    setError(false);
}
#endif

TCPSession::~TCPSession()
{
    endStream();
}

int TCPSession::waitConnection(timeout_t timer)
{
    int sockopt = 0;
    socklen_t len = sizeof(sockopt);

    switch(Socket::state) {
    case INITIAL:
        return -1;
    case CONNECTED:
        break;
    case CONNECTING:
        if(!Socket::isPending(pendingOutput, timer)) {
            endSocket();
            Socket::state = INITIAL;
            return -1;
        }

        getsockopt(so, SOL_SOCKET, SO_ERROR, (char *)&sockopt, &len);
        if(sockopt) {
            endSocket();
            Socket::state = INITIAL;
            return -1;
        }
    default:
        break;
    }
    Socket::state = CONNECTED;
    return 0;
}

void TCPSession::initial(void)
{
    if(waitConnection(60000))
        exit();
}


