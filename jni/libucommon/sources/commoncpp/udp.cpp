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
#include <commoncpp/udp.h>

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

#ifdef  HAVE_GETADDRINFO

UDPSocket::UDPSocket(const char *name, Family fam) :
Socket(fam, SOCK_DGRAM, IPPROTO_UDP)
{
    char namebuf[128], *cp;
    struct addrinfo hint, *list = NULL, *first;

    family = fam;

    switch(fam) {
#ifdef  CCXX_IPV6
    case IPV6:
        peer.ipv6.sin6_family = family;
        break;
#endif
    case IPV4:
        peer.ipv4.sin_family = family;
    }

    snprintf(namebuf, sizeof(namebuf), "%s", name);
    cp = strrchr(namebuf, '/');
    if(!cp && family == IPV4)
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

    hint.ai_family = family;
    hint.ai_socktype = SOCK_DGRAM;
    hint.ai_protocol = IPPROTO_UDP;
    hint.ai_flags = AI_PASSIVE;

    if(getaddrinfo(name, cp, &hint, &list) || !list) {
        error(errBindingFailed, (char *)"Could not find service", errno);
        endSocket();
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
        error(errBindingFailed, (char *)"Count not bind socket", errno);
        return;
    }
}

#else

UDPSocket::UDPSocket(const char *name, Family fam) :
Socket(fam, SOCK_DGRAM, IPPROTO_UDP)
{
    char namebuf[128], *cp;
    tpport_t port = 0;
    struct servent *svc = NULL;
    socklen_t alen = 0;
    struct sockaddr *addr = NULL;

    family = fam;

    snprintf(namebuf, sizeof(namebuf), "%s", name);
    cp = strrchr(namebuf, '/');
    if(!cp && family == IPV4)
        cp = strrchr(namebuf, ':');

    if(!cp) {
        cp = namebuf;
        name = "*";
    }
    else {
        name = namebuf;
        *(cp++) = 0;
    }

    if(isdigit(*cp))
        port = atoi(cp);
    else {
        mutex.enter();
        svc = getservbyname(cp, "udp");
        if(svc)
            port = ntohs(svc->s_port);
        mutex.leave();
        if(!svc) {
            error(errBindingFailed, (char *)"Could not find service", errno);
            endSocket();
            return;
        }
    }

    struct sockaddr_in addr4;
    IPV4Address ia4(name);
#ifdef  CCXX_IPV6
    struct sockaddr_in6 addr6;
    IPV6Address ia6(name);
#endif

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        peer.ipv6.sin6_family = family;
        memset(&addr6, 0, sizeof(addr6));
        addr6.sin6_family = family;
        addr6.sin6_addr = ia6.getAddress();
        addr6.sin6_port = htons(port);
        alen = sizeof(addr6);
        addr = (struct sockaddr *)&addr6;
        break;
#endif
    case IPV4:
        peer.ipv4.sin_family = family;
        memset(&addr4, 0, sizeof(addr4));
        addr4.sin_family = family;
        addr4.sin_addr = ia4.getAddress();
        addr4.sin_port = htons(port);
        alen = sizeof(&addr4);
        addr = (struct sockaddr *)&addr4;
    }

#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt,
        (socklen_t)sizeof(opt));
#endif

    if(addr && !bind(so, addr, alen))
        state = BOUND;

    if(state != BOUND) {
        endSocket();
        error(errBindingFailed, (char *)"Count not bind socket", errno);
        return;
    }
}

#endif

UDPSocket::UDPSocket(Family fam) :
Socket(fam, SOCK_DGRAM, IPPROTO_UDP)
{
    family = fam;
    memset(&peer, 0, sizeof(peer));
    switch(fam) {
#ifdef  CCXX_IPV6
    case IPV6:
        peer.ipv6.sin6_family = family;
        break;
#endif
    case IPV4:
        peer.ipv4.sin_family = family;
    }
}

UDPSocket::UDPSocket(const IPV4Address &ia, tpport_t port) :
Socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
{
    family = IPV4;
    memset(&peer.ipv4, 0, sizeof(peer));
    peer.ipv4.sin_family = AF_INET;
    peer.ipv4.sin_addr = ia.getAddress();
    peer.ipv4.sin_port = htons(port);
#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt, (socklen_t)sizeof(opt));
#endif
    if(bind(so, (struct sockaddr *)&peer.ipv4, sizeof(peer.ipv4))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }
    state = BOUND;
}

#ifdef  CCXX_IPV6
UDPSocket::UDPSocket(const IPV6Address &ia, tpport_t port) :
Socket(AF_INET6, SOCK_DGRAM, IPPROTO_UDP)
{
    family = IPV6;
    memset(&peer.ipv6, 0, sizeof(peer));
    peer.ipv6.sin6_family = AF_INET6;
    peer.ipv6.sin6_addr = ia.getAddress();
    peer.ipv6.sin6_port = htons(port);
#if defined(SO_REUSEADDR)
    int opt = 1;
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (char *)&opt, (socklen_t)sizeof(opt));
#endif
    if(bind(so, (struct sockaddr *)&peer.ipv6, sizeof(peer.ipv6))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }
    state = BOUND;
}
#endif

UDPSocket::~UDPSocket()
{
    endSocket();
}

ssize_t UDPSocket::send(const void *buf, size_t len)
{
    struct sockaddr *addr = NULL;
    socklen_t alen = 0;

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        addr = (struct sockaddr *)&peer.ipv6;
        alen = sizeof(struct sockaddr_in6);
        break;
#endif
    case IPV4:
        addr = (struct sockaddr *)&peer.ipv4;
        alen = sizeof(struct sockaddr_in);
        break;
    default:
        return -1;
    }

    if(isConnected()) {
        addr = NULL;
        alen = 0;
    }

    return _IORET64 ::sendto(so, (const char *)buf, _IOLEN64 len, MSG_NOSIGNAL, addr, alen);
}

ssize_t UDPSocket::receive(void *buf, size_t len, bool reply)
{
    struct sockaddr *addr = NULL;
    struct sockaddr_in senderAddress;  // DMC 2/7/05 ADD for use below.
    socklen_t alen = 0;

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        addr = (struct sockaddr *)&peer.ipv6;
        alen = sizeof(struct sockaddr_in6);
        break;
#endif
    case IPV4:
        addr = (struct sockaddr *)&peer.ipv4;
        alen = sizeof(struct sockaddr_in);
        break;
    default:
        return -1;
    }

    if(isConnected() || !reply) {
        // DMC 2/7/05 MOD to use senderAddress instead of NULL, to prevent 10014 error
        // from recvfrom.
        //addr = NULL;
        //alen = 0;
        addr = (struct sockaddr*)(&senderAddress);
        alen = sizeof(struct sockaddr_in);
    }

    int bytes = ::recvfrom(so, (char *)buf, _IOLEN64 len, 0, addr, &alen);

#ifdef  _MSWINDOWS_

    if (bytes == SOCKET_ERROR) {
        WSAGetLastError();
    }
#endif

    return _IORET64 bytes;
}

Socket::Error UDPSocket::join(const IPV4Multicast &ia,int InterfaceIndex)
{

#if defined(_MSWINDOWS_) && defined(IP_ADD_MEMBERSHIP)

        // DMC 2/7/05: Added WIN32 block.  Win32 does not define the ip_mreqn structure,
        // so we must use ip_mreq with INADDR_ANY.
        struct ip_mreq      group;
    struct sockaddr_in   myaddr;
    socklen_t            len = sizeof(myaddr);

    if(!flags.multicast)
      return error(errMulticastDisabled);

    memset(&group, 0, sizeof(group));
    getsockname(so, (struct sockaddr *)&myaddr, &len);
    group.imr_multiaddr.s_addr = ia.getAddress().s_addr;
    group.imr_interface.s_addr = INADDR_ANY;
    setsockopt(so, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char *)&group, sizeof(group));
    return errSuccess;

#elif defined(IP_ADD_MEMBERSHIP) && defined(SIOCGIFINDEX) && !defined(__FreeBSD__) && !defined(__FreeBSD_kernel__) && !defined(_OSF_SOURCE) && !defined(__hpux) && !defined(__GNU__)

        struct ip_mreqn      group;
    struct sockaddr_in   myaddr;
    socklen_t            len = sizeof(myaddr);

    if(!flags.multicast)
      return error(errMulticastDisabled);

    getsockname(so, (struct sockaddr *)&myaddr, &len);
    memset(&group, 0, sizeof(group));
    memcpy(&group.imr_address, &myaddr.sin_addr, sizeof(myaddr.sin_addr));
    group.imr_multiaddr = ia.getAddress();
    group.imr_ifindex   = InterfaceIndex;
    setsockopt(so, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char *)&group, sizeof(group));
    return errSuccess;
#elif defined(IP_ADD_MEMBERSHIP)
    // if no by index, use INADDR_ANY
    struct ip_mreq group;
    struct sockaddr_in myaddr;
    socklen_t len = sizeof(myaddr);

    if(!flags.multicast)
        return error(errMulticastDisabled);

    getsockname(so, (struct sockaddr *)&myaddr, &len);
    memset(&group, sizeof(group), 0);
    group.imr_multiaddr.s_addr = ia.getAddress().s_addr;
    group.imr_interface.s_addr = INADDR_ANY;
    setsockopt(so, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char *)&group, sizeof(group));
    return errSuccess;
#else
    return error(errServiceUnavailable);
#endif
}

Socket::Error UDPSocket::getInterfaceIndex(const char *DeviceName,int& InterfaceIndex)
{
#ifndef _MSWINDOWS_
#if defined(IP_ADD_MEMBERSHIP) && defined(SIOCGIFINDEX) && !defined(__FreeBSD__) && !defined(__FreeBSD_kernel__) && !defined(_OSF_SOURCE) && !defined(__hpux) && !defined(__GNU__)

    struct ip_mreqn  mreqn;
    struct ifreq       m_ifreq;
    int            i;
    sockaddr_in*          in4 = (sockaddr_in*) &peer.ipv4;
    InterfaceIndex = -1;

    memset(&mreqn, 0, sizeof(mreqn));
    memcpy(&mreqn.imr_multiaddr.s_addr, &in4->sin_addr, sizeof(mreqn.imr_multiaddr.s_addr));

    for (i = 0; i < IFNAMSIZ && DeviceName[i]; ++i)
        m_ifreq.ifr_name[i] = DeviceName[i];
    for (; i < IFNAMSIZ; ++i)
        m_ifreq.ifr_name[i] = 0;

    if (ioctl (so, SIOCGIFINDEX, &m_ifreq))
        return error(errServiceUnavailable);

#if defined(__FreeBSD__) || defined(__GNU__)
    InterfaceIndex = m_ifreq.ifr_ifru.ifru_index;
#else
    InterfaceIndex = m_ifreq.ifr_ifindex;
#endif
    return errSuccess;
#else
    return error(errServiceUnavailable);
#endif
#else
    return error(errServiceUnavailable);
#endif // WIN32
}

#ifdef  AF_UNSPEC
Socket::Error UDPSocket::disconnect(void)
{
    struct sockaddr_in addr;
    int len = sizeof(addr);

    if(so == INVALID_SOCKET)
        return errSuccess;

    Socket::state = BOUND;

    memset(&addr, 0, len);
#ifndef _MSWINDOWS_
    addr.sin_family = AF_UNSPEC;
#else
    addr.sin_family = AF_INET;
    memset(&addr.sin_addr, 0, sizeof(addr.sin_addr));
#endif
    if(::connect(so, (sockaddr *)&addr, len))
        return connectError();
    return errSuccess;
}
#else
Socket::Error UDPSocket::disconnect(void)
{
    if(so == INVALID_SOCKET)
        return errSuccess;

    Socket::state = BOUND;
    return connect(getLocal());
}
#endif

void UDPSocket::setPeer(const IPV4Host &ia, tpport_t port)
{
    memset(&peer.ipv4, 0, sizeof(peer.ipv4));
    peer.ipv4.sin_family = AF_INET;
    peer.ipv4.sin_addr = ia.getAddress();
    peer.ipv4.sin_port = htons(port);
}

void UDPSocket::connect(const IPV4Host &ia, tpport_t port)
{
    setPeer(ia, port);
    if(so == INVALID_SOCKET)
        return;

    if(!::connect(so, (struct sockaddr *)&peer.ipv4, sizeof(struct sockaddr_in)))
        Socket::state = CONNECTED;
}

#ifdef  CCXX_IPV6
void UDPSocket::setPeer(const IPV6Host &ia, tpport_t port)
{
    memset(&peer.ipv6, 0, sizeof(peer.ipv6));
    peer.ipv6.sin6_family = AF_INET6;
    peer.ipv6.sin6_addr = ia.getAddress();
    peer.ipv6.sin6_port = htons(port);
}

void UDPSocket::connect(const IPV6Host &ia, tpport_t port)
{
    setPeer(ia, port);

    if(so == INVALID_SOCKET)
        return;

    if(!::connect(so, (struct sockaddr *)&peer.ipv6, sizeof(struct sockaddr_in6)))
        Socket::state = CONNECTED;

}

#endif

#ifdef  HAVE_GETADDRINFO

void UDPSocket::setPeer(const char *name)
{
    char namebuf[128], *cp;
    struct addrinfo hint, *list;

    snprintf(namebuf, sizeof(namebuf), "%s", name);
    cp = strrchr(namebuf, '/');
    if(!cp)
        cp = strrchr(namebuf, ':');

    if(!cp)
        return;

    memset(&hint, 0, sizeof(hint));
    hint.ai_family = family;
    hint.ai_socktype = SOCK_DGRAM;
    hint.ai_protocol = IPPROTO_UDP;

    if(getaddrinfo(namebuf, cp, &hint, &list) || !list)
        return;

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        memcpy(&peer.ipv6, list->ai_addr, sizeof(peer.ipv6));
        break;
#endif
    case IPV4:
        memcpy(&peer.ipv4, list->ai_addr, sizeof(peer.ipv4));
        break;
    }

    freeaddrinfo(list);
}

#else

void UDPSocket::setPeer(const char *name)
{
    char namebuf[128], *cp;
    struct servent *svc;
    tpport_t port;

    snprintf(namebuf, sizeof(namebuf), "%s", name);
    cp = strrchr(namebuf, '/');
    if(!cp)
        cp = strrchr(namebuf, ':');

    if(!cp)
        return;

    if(isdigit(*cp))
        port = atoi(cp);
    else {
        mutex.enter();
        svc = getservbyname(cp, "udp");
        if(svc)
            port = ntohs(svc->s_port);
        mutex.leave();
        if(!svc)
            return;
    }

    memset(&peer, 0, sizeof(peer));

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        setPeer(IPV6Host(namebuf), port);
        break;
#endif
    case IPV4:
        setPeer(IPV4Host(namebuf), port);
        break;
    }
}

#endif

void UDPSocket::connect(const char *service)
{
    int rtn = -1;

    setPeer(service);

    if(so == INVALID_SOCKET)
        return;

    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        rtn = ::connect(so, (struct sockaddr *)&peer.ipv6, sizeof(struct sockaddr_in6));
        break;
#endif
    case IPV4:
        rtn = ::connect(so, (struct sockaddr *)&peer.ipv4, sizeof(struct sockaddr_in));
        break;
    }
    if(!rtn)
        Socket::state = CONNECTED;
}

IPV4Host UDPSocket::getIPV4Peer(tpport_t *port) const
{
    // FIXME: insufficient buffer
    //        how to retrieve peer ??
    char buf;
    socklen_t len = sizeof(peer.ipv4);
    int rtn = ::recvfrom(so, &buf, 1, MSG_PEEK, (struct sockaddr *)&peer.ipv4, &len);

#ifdef _MSWINDOWS_
    if(rtn < 1 && WSAGetLastError() != WSAEMSGSIZE) {
        if(port)
            *port = 0;

        memset((void*) &peer.ipv4, 0, sizeof(peer.ipv4));
    }
#else
    if(rtn < 1) {
        if(port)
            *port = 0;

        memset((void*) &peer.ipv4, 0, sizeof(peer.ipv4));
    }
#endif
    else {
        if(port)
            *port = ntohs(peer.ipv4.sin_port);
    }
    return IPV4Host(peer.ipv4.sin_addr);
}

#ifdef  CCXX_IPV6
IPV6Host UDPSocket::getIPV6Peer(tpport_t *port) const
{
    // FIXME: insufficient buffer
    //        how to retrieve peer ??
    char buf;
    socklen_t len = sizeof(peer.ipv6);
    int rtn = ::recvfrom(so, &buf, 1, MSG_PEEK, (struct sockaddr *)&peer.ipv6, &len);

#ifdef _MSWINDOWS_
    if(rtn < 1 && WSAGetLastError() != WSAEMSGSIZE) {
        if(port)
            *port = 0;

        memset((void*) &peer.ipv6, 0, sizeof(peer.ipv6));
    }
#else
    if(rtn < 1) {
        if(port)
            *port = 0;

        memset((void*) &peer.ipv6, 0, sizeof(peer.ipv6));
    }
#endif
    else {
        if(port)
            *port = ntohs(peer.ipv6.sin6_port);
    }
    return IPV6Host(peer.ipv6.sin6_addr);
}
#endif

UDPBroadcast::UDPBroadcast(const IPV4Address &ia, tpport_t port) :
UDPSocket(ia, port)
{
    if(so != INVALID_SOCKET)
        setBroadcast(true);
}

void UDPBroadcast::setPeer(const IPV4Broadcast &ia, tpport_t port)
{
    memset(&peer.ipv4, 0, sizeof(peer.ipv4));
    peer.ipv4.sin_family = AF_INET;
    peer.ipv4.sin_addr = ia.getAddress();
    peer.ipv4.sin_port = htons(port);
}

UDPTransmit::UDPTransmit(const IPV4Address &ia, tpport_t port) :
UDPSocket(ia, port)
{
    disconnect();   // assure not started live
    ::shutdown(so, 0);
    receiveBuffer(0);
}

#ifdef  CCXX_IPV6
UDPTransmit::UDPTransmit(const IPV6Address &ia, tpport_t port) :
UDPSocket(ia, port)
{
    disconnect();   // assure not started live
    ::shutdown(so, 0);
    receiveBuffer(0);
}
#endif

UDPTransmit::UDPTransmit(Family family) : UDPSocket(family)
{
    disconnect();
    ::shutdown(so, 0);
    receiveBuffer(0);
}

Socket::Error UDPTransmit::cConnect(const IPV4Address &ia, tpport_t port)
{
    int len = sizeof(peer.ipv4);

    peer.ipv4.sin_family = AF_INET;
    peer.ipv4.sin_addr = ia.getAddress();
    peer.ipv4.sin_port = htons(port);
    // Win32 will crash if you try to connect to INADDR_ANY.
    if ( INADDR_ANY == peer.ipv4.sin_addr.s_addr )
        peer.ipv4.sin_addr.s_addr = INADDR_LOOPBACK;
    if(::connect(so, (sockaddr *)&peer.ipv4, len))
        return connectError();
    return errSuccess;
}


#ifdef  CCXX_IPV6

Socket::Error UDPTransmit::connect(const IPV6Address &ia, tpport_t port)
{
    int len = sizeof(peer.ipv6);

    peer.ipv6.sin6_family = AF_INET6;
    peer.ipv6.sin6_addr = ia.getAddress();
    peer.ipv6.sin6_port = htons(port);
    // Win32 will crash if you try to connect to INADDR_ANY.
    if(!memcmp(&peer.ipv6.sin6_addr, &in6addr_any, sizeof(in6addr_any)))
        memcpy(&peer.ipv6.sin6_addr, &in6addr_loopback,
            sizeof(in6addr_loopback));
    if(::connect(so, (struct sockaddr *)&peer.ipv6, len))
        return connectError();
    return errSuccess;
}
#endif

Socket::Error UDPTransmit::connect(const IPV4Host &ia, tpport_t port)
{
    if(isBroadcast())
        setBroadcast(false);

    return cConnect((IPV4Address)ia,port);
}

Socket::Error UDPTransmit::connect(const IPV4Broadcast &subnet, tpport_t  port)
{
    if(!isBroadcast())
        setBroadcast(true);

    return cConnect((IPV4Address)subnet,port);
}

Socket::Error UDPTransmit::connect(const IPV4Multicast &group, tpport_t port)
{
    Error err;
    if(!( err = UDPSocket::setMulticast(true) ))
        return err;

    return cConnect((IPV4Address)group,port);
}

#ifdef  CCXX_IPV6
Socket::Error UDPTransmit::connect(const IPV6Multicast &group, tpport_t port)
{
    Error error;
    if(!( error = UDPSocket::setMulticast(true) ))
        return error;

    return connect((IPV6Address)group,port);
}
#endif

UDPReceive::UDPReceive(const IPV4Address &ia, tpport_t port) :
UDPSocket(ia, port)
{
    ::shutdown(so, 1);
    sendBuffer(0);
}

#ifdef  CCXX_IPV6
UDPReceive::UDPReceive(const IPV6Address &ia, tpport_t port) :
UDPSocket(ia, port)
{
    ::shutdown(so, 1);
    sendBuffer(0);
}
#endif

Socket::Error UDPReceive::connect(const IPV4Host &ia, tpport_t port)
{
    int len = sizeof(peer.ipv4);

    peer.ipv4.sin_family = AF_INET;
    peer.ipv4.sin_addr = ia.getAddress();
    peer.ipv4.sin_port = htons(port);
    // Win32 will crash if you try to connect to INADDR_ANY.
    if ( INADDR_ANY == peer.ipv4.sin_addr.s_addr )
        peer.ipv4.sin_addr.s_addr = INADDR_LOOPBACK;
    if(::connect(so, (struct sockaddr *)&peer.ipv4, len))
        return connectError();
    return errSuccess;
}

#ifdef  CCXX_IPV6
Socket::Error UDPReceive::connect(const IPV6Host &ia, tpport_t port)
{
    int len = sizeof(peer.ipv6);

    peer.ipv6.sin6_family = AF_INET6;
    peer.ipv6.sin6_addr = ia.getAddress();
    peer.ipv6.sin6_port = htons(port);
    // Win32 will crash if you try to connect to INADDR_ANY.
    if(!memcmp(&peer.ipv6.sin6_addr, &in6addr_any, sizeof(in6addr_any)))
        memcpy(&peer.ipv6.sin6_addr, &in6addr_loopback, sizeof(in6addr_loopback));
    if(::connect(so, (sockaddr *)&peer.ipv6, len))
        return connectError();
    return errSuccess;
}
#endif

UDPDuplex::UDPDuplex(const IPV4Address &bind, tpport_t port) :
UDPTransmit(bind, port + 1), UDPReceive(bind, port)
{}

#ifdef  CCXX_IPV6
UDPDuplex::UDPDuplex(const IPV6Address &bind, tpport_t port) :
UDPTransmit(bind, port + 1), UDPReceive(bind, port)
{}
#endif

Socket::Error UDPDuplex::connect(const IPV4Host &host, tpport_t port)
{
    Error rtn = UDPTransmit::connect(host, port);
    if(rtn) {
        UDPTransmit::disconnect();
        UDPReceive::disconnect();
        return rtn;
    }
    return UDPReceive::connect(host, port + 1);
}

#ifdef  CCXX_IPV6
Socket::Error UDPDuplex::connect(const IPV6Host &host, tpport_t port)
{
    Error rtn = UDPTransmit::connect(host, port);
    if(rtn) {
        UDPTransmit::disconnect();
        UDPReceive::disconnect();
        return rtn;
    }
    return UDPReceive::connect(host, port + 1);
}
#endif

Socket::Error UDPDuplex::disconnect(void)
{
    Error rtn = UDPTransmit::disconnect();
    Error rtn2 = UDPReceive::disconnect();
    if (rtn) return rtn;
        return rtn2;
}

