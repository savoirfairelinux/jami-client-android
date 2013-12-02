// Copyright (C) 2002-2010 Christian Prochnow <cproch@seculogix.de>
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

#ifdef  __QNX__
#include <net/if.h>
#endif

#include <cc++/config.h>
#include <cc++/thread.h>
#include <cc++/address.h>
#include <cc++/socket.h>
#include <cc++/export.h>
#include <cc++/network.h>

#ifndef WIN32
#ifdef  HAVE_IOCTL_H
#include <ioctl.h>
#else
#include <sys/ioctl.h>
#endif
# ifdef HAVE_SYS_SOCKIO_H
# include <sys/sockio.h>
# endif
#ifdef  HAVE_NET_IF_H
#include <net/if.h>
#endif
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#if defined(HAVE_NET_IF_H) || defined(WIN32)
#if !defined(_MSC_VER) || _MSC_VER >= 1300

using namespace std;

NetworkDeviceInfo::NetworkDeviceInfo(const String& name,
                     const IPV4Host& addr,
                     const IPV4Broadcast& broadcast,
                     const IPV4Mask& netmask, int mtu)
    : _name(name), _addr(addr), _broadcast(broadcast), _netmask(netmask),
      _mtu(mtu)
{}

NetworkDeviceInfo::NetworkDeviceInfo(const NetworkDeviceInfo& ndi)
    : _name(ndi._name), _addr(ndi._addr), _broadcast(ndi._broadcast),
      _netmask(ndi._netmask), _mtu(ndi._mtu)
{}

NetworkDeviceInfo::~NetworkDeviceInfo()
{}

bool enumNetworkDevices(vector<NetworkDeviceInfo>& devs)
{
    devs.clear();

#ifndef WIN32
    char buffer[8192];
    struct ifconf ifc;

    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if(fd == -1)
        return false;

    ifc.ifc_len = sizeof(buffer);
    ifc.ifc_buf = buffer;

    if(ioctl(fd, SIOCGIFCONF, &ifc) == -1)
        return false;

    IPV4Host addr;
    IPV4Broadcast brdaddr;
    IPV4Mask maskaddr("255.255.255.255");
    int mtu;

    int count = ifc.ifc_len / sizeof(ifreq);
    for(int i = 0; i < count; ++i) {
        if(ifc.ifc_req[i].ifr_addr.sa_family != AF_INET)
            continue;

        addr = ((sockaddr_in&)ifc.ifc_req[i].ifr_addr).sin_addr;

        struct ifreq devifreq;
        setString(devifreq.ifr_name, sizeof(devifreq.ifr_name), ifc.ifc_req[i].ifr_name);

        if(ioctl(fd, SIOCGIFBRDADDR, &devifreq) == -1)
            (IPV4Address&)brdaddr = htonl(INADDR_ANY);
        else
            (IPV4Address&)brdaddr = ((sockaddr_in&)devifreq.ifr_broadaddr).sin_addr;

        if(ioctl(fd, SIOCGIFNETMASK, &devifreq) == -1)
            maskaddr = htonl(INADDR_BROADCAST);
        else
            (IPV4Address&)maskaddr = ((sockaddr_in&)devifreq.ifr_addr).sin_addr;

#if defined(SIOCGLIFMTU) && !defined(__hpux)
        struct lifreq devlifreq;
        if(ioctl(fd, SIOCGLIFMTU, &devlifreq) == -1)
            mtu = 0;
        else
            mtu = devlifreq.lifr_mtu;
#else
        if(ioctl(fd, SIOCGIFMTU, &devifreq) == -1)
            mtu = 0;
        else
#if defined(__hpux)
            mtu = devifreq.ifr_metric;
#else
            mtu = devifreq.ifr_mtu;
#endif
#endif
        devs.push_back(NetworkDeviceInfo(ifc.ifc_req[i].ifr_name, addr, brdaddr, maskaddr, mtu));
    }

    close(fd);
#elif defined(SIO_GET_INTERFACE_LIST) // WIN32

    SOCKET s = WSASocket(AF_INET, SOCK_DGRAM, 0, 0, 0, 0);
    if(s == INVALID_SOCKET) {
        if(WSAGetLastError() == WSANOTINITIALISED) {
            WSADATA wsadata;
            WSAStartup(MAKEWORD(2,0),&wsadata);
        }
        else
            return false;
    }

    char outbuff[8192];
    DWORD outlen;
    DWORD ret = WSAIoctl(s, SIO_GET_INTERFACE_LIST, 0, 0, outbuff, sizeof(outbuff), &outlen, 0, 0);
    if(ret == SOCKET_ERROR)
        return false;

    INTERFACE_INFO* iflist = (INTERFACE_INFO*)outbuff;
    int ifcount = outlen / sizeof(INTERFACE_INFO);

    IPV4Host addr;
    IPV4Broadcast brdaddr;
    IPV4Mask maskaddr("0.0.0.0");

    for(int i = 0; i < ifcount; ++i) {
        addr = ((sockaddr_in&)iflist[i].iiAddress).sin_addr;
        (IPV4Address&)brdaddr = ((sockaddr_in&)iflist[i].iiBroadcastAddress).sin_addr;
        (IPV4Address&)maskaddr = ((sockaddr_in&)iflist[i].iiNetmask).sin_addr;
        devs.push_back(NetworkDeviceInfo("", addr, brdaddr, maskaddr, -1));
    }

    closesocket(s);
#endif

    return true;
}

#endif
#endif

#ifdef  CCXX_NAMESPACES
}
#endif
/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
