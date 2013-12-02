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

#include <cc++/config.h>
#include <cc++/export.h>
#include <cc++/thread.h>
#include <cc++/address.h>
#include <cc++/socket.h>
#include "private.h"

#ifdef WIN32
#include <fcntl.h>
#endif
#include <cerrno>

#ifndef INADDR_LOOPBACK
#define INADDR_LOOPBACK (unsigned long)0x7f000001
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

UDPTransmit::UDPTransmit(const IPV4Address &ia, tpport_t port) :
UDPSocket(ia, port)
{
    disconnect();   // assure not started live
    shutdown(so, 0);
    receiveBuffer(0);
}

#ifdef  CCXX_IPV6
UDPTransmit::UDPTransmit(const IPV6Address &ia, tpport_t port) :
UDPSocket(ia, port)
{
    disconnect();   // assure not started live
    shutdown(so, 0);
    receiveBuffer(0);
}
#endif

UDPTransmit::UDPTransmit(Family family) : UDPSocket(family)
{
    disconnect();
    shutdown(so, 0);
    receiveBuffer(0);
}

Socket::Error UDPTransmit::cConnect(const IPV4Address &ia, tpport_t port)
{
    int len = sizeof(peer.ipv4);

    peer.ipv4.sin_family = AF_INET;
    peer.ipv4.sin_addr = getaddress(ia);
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
    peer.ipv6.sin6_addr = getaddress(ia);
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
    shutdown(so, 1);
    sendBuffer(0);
}

#ifdef  CCXX_IPV6
UDPReceive::UDPReceive(const IPV6Address &ia, tpport_t port) :
UDPSocket(ia, port)
{
    shutdown(so, 1);
    sendBuffer(0);
}
#endif

Socket::Error UDPReceive::connect(const IPV4Host &ia, tpport_t port)
{
    int len = sizeof(peer.ipv4);

    peer.ipv4.sin_family = AF_INET;
    peer.ipv4.sin_addr = getaddress(ia);
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
    peer.ipv6.sin6_addr = getaddress(ia);
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

#ifdef  CCXX_NAMESPACES
}
#endif
