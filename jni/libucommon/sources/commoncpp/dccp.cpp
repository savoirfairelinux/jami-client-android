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
#include <commoncpp/dccp.h>
#include <errno.h>

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

#ifdef HAVE_SYS_FILIO_H
#include <sys/filio.h>
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

DCCPSocket::DCCPSocket(Family fam) :
Socket(fam, SOCK_DCCP, IPPROTO_DCCP)
{
    family = fam;
}

DCCPSocket::DCCPSocket(DCCPSocket& server, timeout_t timeout) :
Socket(accept(server.so, NULL, NULL))
{
    family = server.family;
    Socket::state = CONNECTED;
    socklen_t alen = sizeof(peer);

    getpeername(so, (struct sockaddr *)&peer, &alen);

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        if(!server.onAccept(IPV6Host(peer.ipv6.sin6_addr), peer.ipv6.sin6_port))
            endSocket();
        break;
#endif
    case IPV4:
        if(!server.onAccept(IPV4Host(peer.ipv4.sin_addr), peer.ipv4.sin_port))
            endSocket();
        break;
    }
}

#ifdef  HAVE_GETADDRINFO
DCCPSocket::DCCPSocket(const char *name, Family fam, unsigned backlog) :
Socket(fam, SOCK_DCCP, IPPROTO_DCCP)
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

    family = fam;
    memset(&hint, 0, sizeof(hint));
    hint.ai_family = family;
    hint.ai_socktype = SOCK_DCCP;
    hint.ai_protocol = IPPROTO_DCCP;
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

    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",socket_errno);
        return;
    }
}
#else
DCCPSocket::DCCPSocket(const char *name, Family fam, unsigned backlog) :
Socket(fam, SOCK_DCCP, IPPROTO_DCCP)
{
    char namebuf[128], *cp;
    struct sockaddr_in addr;
#ifdef  CCXX_IPV6
    struct sockaddr_in6 addr6;
#endif
    struct sockaddr *ap = NULL;
    socklen_t alen = 0;

    struct servent *svc;

    family = fam;
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

    addr.sin_family = family;
    if(isdigit(*cp))
        addr.sin_port = htons(atoi(cp));
    else {
        mutex.enter();
        svc = getservbyname(cp, "dccp");
        if(svc)
            addr.sin_port = svc->s_port;
        mutex.leave();
        if(!svc) {
            endSocket();
            error(errBindingFailed, "Could not find service", errno);
            return;

        }
    }

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt,
            (socklen_t)sizeof(opt));
#endif

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6: {
        IPV6Address ia6(name);
        addr6.sin6_port = addr.sin_port;
        addr6.sin6_family = family;
        addr6.sin6_addr = getaddress(ia6);
        ap = (struct sockaddr *)&addr6;
        alen = sizeof(addr6);
        break;
    }
#endif
    case IPV4:
        IPV4Address ia(name);
        addr.sin_addr = getaddress(ia);
        ap = (struct sockaddr *)&addr;
        alen = sizeof(addr);
        break;
    }

    if(!ap || bind(so, (struct sockaddr *)ap, alen)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }

    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",
                socket_errno);
        return;
    }
    state = BOUND;
}
#endif

DCCPSocket::DCCPSocket(const IPV4Address &ia, tpport_t port, unsigned backlog) :
Socket(AF_INET, SOCK_DCCP, IPPROTO_DCCP)
{
    struct sockaddr_in addr;

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr = getaddress(ia);
    addr.sin_port = htons(port);
    family = IPV4;

    memset(&peer, 0, sizeof(peer));
    peer.ipv4 = addr;

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt, (socklen_t)sizeof(opt));
#endif
    if(bind(so, (struct sockaddr *)&addr, sizeof(addr))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }

    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",socket_errno);
        return;
    }
    state = BOUND;
}

#ifdef CCXX_IPV6
DCCPSocket::DCCPSocket(const IPV6Address &ia, tpport_t port, unsigned backlog) :
Socket(AF_INET6, SOCK_DCCP, IPPROTO_DCCP)
{
    struct sockaddr_in6 addr;

    memset(&addr, 0, sizeof(addr));
    addr.sin6_family = AF_INET6;
    addr.sin6_addr = getaddress(ia);
    addr.sin6_port = htons(port);

    family = IPV6;
    memset(&peer, 0, sizeof(peer));
    peer.ipv6 = addr;

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt, (socklen_t)sizeof(opt));
#endif
    if(bind(so, (struct sockaddr *)&addr, sizeof(addr))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }

    if(listen(so, backlog)) {
        endSocket();
        error(errBindingFailed,(char *)"Could not listen on socket",socket_errno);
        return;
    }
    state = BOUND;
}

bool DCCPSocket::onAccept(const IPV6Host &ia, tpport_t port)
{
    return true;
}

#endif

bool DCCPSocket::onAccept(const IPV4Host &ia, tpport_t port)
{
    return true;
}

IPV4Host DCCPSocket::getIPV4Sender(tpport_t *port) const
{
    if(port)
        *port = ntohs(peer.ipv4.sin_port);
    return IPV4Host(peer.ipv4.sin_addr);
}

#ifdef  CCXX_IPV6
IPV6Host DCCPSocket::getIPV6Sender(tpport_t *port) const
{
    return IPV6Host(peer.ipv6.sin6_addr);
}
#endif

DCCPSocket::~DCCPSocket()
{
    endSocket();
}

void DCCPSocket::disconnect(void)
{
    if(Socket::state != CONNECTED)
        return;

    endSocket();
    so = socket(family, SOCK_DCCP, IPPROTO_DCCP);
    if(so != INVALID_SOCKET)
        Socket::state = AVAILABLE;
}

#ifdef  HAVE_GETADDRINFO
void DCCPSocket::connect(const char *target)
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
        connectError();
        return;
    }

    *(cp++) = 0;

    memset(&hint, 0, sizeof(hint));
    hint.ai_family = family;
    hint.ai_socktype = SOCK_DCCP;
    hint.ai_protocol = IPPROTO_DCCP;

    if(getaddrinfo(namebuf, cp, &hint, &list) || !list) {
        connectError();
        return;
    }

    first = list;

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
        connectError();
        return;
    }

    Socket::state = CONNECTED;
}
#else
void DCCPSocket::connect(const char *target)
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
        connectError();
        return;
    }

    *(cp++) = 0;

    if(isdigit(*cp))
        port = atoi(cp);
    else {
        mutex.enter();
        svc = getservbyname(cp, "dccp");
        if(svc)
            port = ntohs(svc->s_port);
        mutex.leave();
        if(!svc) {
            connectError();
            return;
        }
    }

    switch(family) {
    case IPV4:
        connect(IPV4Host(namebuf), port);
        break;
#ifdef  CCXX_IPV6
    case IPV6:
        connect(IPV6Host(namebuf), port);
        break;
#endif
    default:
        connectError();
    }
}
#endif

void DCCPSocket::connect(const IPV4Host &host, tpport_t port, timeout_t timeout)
{
    size_t i;
    fd_set fds;
    struct timeval to;
    bool connected = false;
    int rtn;
    int sockopt;
    socklen_t len = sizeof(sockopt);

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
                so = socket(AF_INET, SOCK_DCCP, IPPROTO_DCCP);
                if(so == INVALID_SOCKET)
                    break;
            }
    }

    setCompletion(true);
    if(!connected) {
        rtn = errno;
        errno = rtn;
        connectError();
        return;
    }

    Socket::state = CONNECTED;
}

#ifdef  CCXX_IPV6
void DCCPSocket::connect(const IPV6Host &host, tpport_t port, timeout_t timeout)
{
    size_t i;
    fd_set fds;
    struct timeval to;
    bool connected = false;
    int rtn;
    int sockopt;
    socklen_t len = sizeof(sockopt);

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
                so = socket(AF_INET6, SOCK_DCCP, IPPROTO_DCCP);
                if(so == INVALID_SOCKET)
                    break;
            }
    }

    setCompletion(true);
    if(!connected) {
        rtn = errno;
        errno = rtn;
        connectError();
        return;
    }

    Socket::state = CONNECTED;
}
#endif

bool DCCPSocket::setCCID(uint8_t ccid)
{
    uint8_t ccids[16];           /* for getting the available CCIDs, should be large enough */
    socklen_t len = sizeof(ccids);
    int ret;
    bool ccid_supported = false;

    /*
     * Determine which CCIDs are available on the host
     */
    ret = getsockopt(so, SOL_DCCP, DCCP_SOCKOPT_AVAILABLE_CCIDS, (char *)&ccids, &len);
    if (ret < 0) {
        error(errInput,(char *)"Can not determine available CCIDs",socket_errno);
        return false;
    }

    for (unsigned i = 0; i < sizeof(ccids); i++) {
        if (ccid == ccids[i]) {
            ccid_supported = true;
            break;
        }
    }

    if (!ccid_supported) {
        error(errInput,(char *)"CCID specified is not supported",socket_errno);
        return false;
    }

    if (setsockopt(so, SOL_DCCP, DCCP_SOCKOPT_CCID, (char *)&ccid, sizeof (ccid)) < 0) {
        error(errInput,(char *)"Can not set CCID",socket_errno);
        return false;
    }

    return true;
}

int DCCPSocket::getTxCCID()
{
    int ccid, ret;
    socklen_t ccidlen;

    ccidlen = sizeof(ccid);
    ret = getsockopt(so, SOL_DCCP, DCCP_SOCKOPT_TX_CCID, (char *)&ccid, &ccidlen);
    if (ret < 0) {
    error(errInput,(char *)"Can not determine get current TX CCID value",socket_errno);
        return -1;
    }
    return ccid;
}

int DCCPSocket::getRxCCID()
{
    int ccid, ret;
    socklen_t ccidlen;

    ccidlen = sizeof(ccid);
    ret = getsockopt(so, SOL_DCCP, DCCP_SOCKOPT_RX_CCID, (char *)&ccid, &ccidlen);
    if (ret < 0) {
    error(errInput,(char *)"Can not determine get current DX CCID value",socket_errno);
        return -1;
    }
    return ccid;
}

size_t  DCCPSocket::available()
{
    size_t readsize;
#ifndef _MSWINDOWS_
     if (ioctl (so, FIONREAD, &readsize) < 0) {
         error(errInput,(char *)"Error on retrieve the FIONREAD option.",socket_errno);
     }
#else
    if (ioctlsocket(so, FIONREAD, (u_long *)&readsize)){
        error(errInput,(char *)"Error on retrieve the FIONREAD option.",socket_errno);
    }
#endif
    return readsize;
}



