// Copyright (C) 2004-2010 TintaDigital - STI, LDA.
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
 * @file nat.c
 * @short Network Address Translation interface implementation.
 * @author Ricardo Gameiro <rgameiro at tintadigital dot com>
 **/

#include <cc++/config.h>
#include "nat.h"

#ifdef CCXX_NAT
#  ifdef HAVE_SYS_TYPES_H
#   include <sys/types.h>
#  endif
#  ifdef HAVE_SYS_SOCKET_H
#   include <sys/socket.h>
#  endif
# ifdef HAVE_NAT_NETFILTER // Linux
#  ifdef HAVE_LIMITS_H
#   include <limits.h>
#  endif
#  ifdef HAVE_LINUX_NETFILTER_IPV4_H
#   include <linux/netfilter_ipv4.h>
#  endif
#  ifdef HAVE_LINUX_NETFILTER_IPV6_H
#   include <linux/netfilter_ipv6.h>
#  endif
# else
#  ifdef HAVE_NET_IP6_H
#   include <netinet/ip6.h>
#  endif
#  ifdef HAVE_NETINET_IN_H
#   include <netinet/in.h>
#  endif
#  ifdef HAVE_NET_IF_H
#   include <net/if.h>
#  endif
#  ifdef HAVE_SYS_IOCTL_H
#   include <sys/ioctl.h>
#  endif
#  ifdef HAVE_IOCTL_H
#   include <ioctl.h>
#  endif
#  ifdef HAVE_UNISTD_H
#   include <unistd.h>
#  endif
#  ifdef HAVE_ERRNO_H
#   include <errno.h>
#  endif
#  ifdef HAVE_NAT_IPF // Solaris, *BSD (except OpenBSD), HP-UX
#   ifdef HAVE_NETINET_IP_COMPAT_H
#    include <netinet/ip_compat.h>
#   endif
#   ifdef HAVE_IP_COMPAT_H
#    include <ip_compat.h>
#   endif
#   ifdef HAVE_NETINET_IP_FIL_COMPAT_H
#    include <netinet/ip_fil_compat.h>
#   endif
#   ifdef HAVE_IP_FIL_COMPAT_H
#    include <ip_fil_compat.h>
#   endif
#   ifdef HAVE_NETINET_IP_FIL_H
#    include <netinet/ip_fil.h>
#   endif
#   ifdef HAVE_IP_FIL_H
#    include <ip_fil.h>
#   endif
#   ifdef HAVE_NETINET_IP_NAT_H
#    include <netinet/ip_nat.h>
#   endif
#   ifdef HAVE_IP_NAT_H
#    include <ip_nat.h>
#   endif
#  endif
#  ifdef HAVE_NAT_PF // OpenBSD
#   ifdef HAVE_NET_PFVAR_H
#    include <net/pfvar.h>
#   endif
#  endif
# endif
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifdef HAVE_NAT_NETFILTER
# define NAT_SYSCALL "getsockopt"
# define NAT_DEVICE ""
#else
# define NAT_SYSCALL "ioctl"
# if defined(HAVE_NAT_IPF) && defined(IPL_NAT)
#  define NAT_DEVICE IPL_NAT
# else
#  ifdef HAVE_NAT_PF
#   define NAT_DEVICE "/dev/pf"
#  endif
# endif
#endif

#ifndef NAT_DEVICE
#define NAT_DEVICE ""
#endif

#ifdef CCXX_NAT

const char * natmsg[] = {
    "nat lookup successful",
    "nat address not in table",
    "nat not supported/implemented",
    "unable to open device "NAT_DEVICE,
    "unable to get socket name",
    "unable to get peer name",
    "unable to get socket type",
    "unable to lookup, nat "NAT_SYSCALL" failed",
    "unkown nat error code"
};

#ifdef HAVE_NAT_NETFILTER // Linux
natResult natv4Lookup( SOCKET sfd, struct sockaddr_in * nat )
{
    struct sockaddr_in local;
    socklen_t nlen = sizeof( *nat ), llen = sizeof( local );

    if( getsockname( sfd, ( struct sockaddr * ) &local, &llen ) )
        return natSocknameErr;

    memset( &nat->sin_addr.s_addr, 0, sizeof( nat->sin_addr.s_addr ) );
    if( getsockopt( sfd, SOL_IP, SO_ORIGINAL_DST, ( struct sockaddr * ) nat, &nlen) )
        return natIFaceErr;

    if( local.sin_addr.s_addr == nat->sin_addr.s_addr )
        return natSearchErr;

    nat->sin_family = local.sin_family;

    return natOK;
}

#ifdef CCXX_IPV6
natResult natv6Lookup( SOCKET sfd, struct sockaddr_in6 * nat )
{
    struct sockaddr_in6 local;
    socklen_t llen = sizeof( local ), nlen = sizeof( *nat );

    if( getsockname( sfd, ( struct sockaddr * ) &local, &llen ) )
        return natSocknameErr;

    memset( &nat->sin6_addr.s6_addr, 0, sizeof( nat->sin6_addr.s6_addr ) );
    if( getsockopt( sfd, SOL_IP, SO_ORIGINAL_DST, ( struct sockaddr * ) nat, &nlen) )
        return natIFaceErr;

    if( local.sin6_addr.s6_addr == nat->sin6_addr.s6_addr )
        return natSearchErr;

    nat->sin6_family = local.sin6_family;

    return natOK;
}
#endif
#endif

#ifdef HAVE_NAT_IPF // Solaris, *BSD (except OpenBSD), HP-UX, etc.
natResult natv4Lookup( int sfd, struct sockaddr_in * nat )
{
    static int natfd = -1;
    struct sockaddr_in local, peer;
    socklen_t  nlen = sizeof( *nat ), llen = sizeof( local ), plen = sizeof( peer );
    int socktype;
    socklen_t stlen = sizeof( socktype );
    struct natlookup nlu;
    int nres;

    if( natfd < 0 )
        if( ( natfd = open( NAT_DEVICE, O_RDONLY, 0 ) ) < 0 )
            return natDevUnavail;

    if( getsockname( sfd, ( struct sockaddr * ) &local, &llen ) )
        return natSocknameErr;
    if( getpeername( sfd, ( struct sockaddr * ) &peer, &plen ) )
        return natPeernameErr;
    if( getsockopt( sfd, SOL_SOCKET, SO_TYPE, ( int * ) &socktype, &stlen ) )
        return natSockTypeErr;

    memset( &nlu.nl_realip.s_addr, 0, sizeof( nlu.nl_realip.s_addr ) );
    nlu.nl_inip = local.sin_addr;
    nlu.nl_inport = local.sin_port;
    nlu.nl_outip = peer.sin_addr;
    nlu.nl_outport = peer.sin_port;
    nlu.nl_flags = ( socktype == SOCK_STREAM ) ? IPN_TCP : IPN_UDP;

    if( 63 == ( SIOCGNATL & 0xff ) ) {
        struct natlookup * nlup = &nlu;
        nres = ioctl( natfd, SIOCGNATL, &nlup );
    } else
        nres = ioctl( natfd, SIOCGNATL, &nlu );

    if( nres ) {
        if( errno != ESRCH ) {
            close( natfd );
            natfd = -1;
            return natIFaceErr;
        } else
            return natSearchErr;
    }

    if( local.sin_addr.s_addr == nlu.nl_realip.s_addr )
        return natSearchErr;

    nat->sin_family = local.sin_family;
    nat->sin_port = nlu.nl_realport;
    nat->sin_addr = nlu.nl_realip;

    return natOK;
}

#ifdef CCXX_IPV6 // IPV6 is not yet supported by IPFilter
natResult natv6Lookup( SOCKET sfd, struct sockaddr_in6 * nat ) {
    return natNotSupported;
}
#endif
#endif

#ifdef HAVE_NAT_PF // OpenBSD
natResult natv4Lookup( SOCKET sfd, struct sockaddr_in * nat ) {

    static int natfd = -1;
    struct sockaddr_in local, peer;
    socklen_t  nlen = sizeof( *nat ), llen = sizeof( local ), plen = sizeof( peer );
    int socktype;
    socklen_t stlen = sizeof( socktype );
    struct pfioc_natlook nlu;
    int nres;

    if( natfd < 0 )
        if( ( natfd = open( NAT_DEVICE, O_RDWR ) ) < 0 )
            return natDevUnavail;

    if( getsockname( sfd, ( struct sockaddr * ) &local, &llen ) )
        return natSocknameErr;
    if( getpeername( sfd, ( struct sockaddr * ) &peer, &plen ) )
        return natPeernameErr;
    if( getsockopt( sfd, SOL_SOCKET, SO_TYPE, ( int * ) &socktype, &stlen ) )
        return natSockTypeErr;

    memset( &nlu, 0, sizeof( nlu ) );
    nlu.daddr.v4.s_addr = local.sin_addr.s_addr;
    nlu.dport = local.sin_port;
    nlu.saddr.v4.s_addr = peer.sin_addr.s_addr;
    nlu.sport = peer.sin_port;
    nlu.af = AF_INET;
    nlu.proto = ( socktype == SOCK_STREAM ) ? IPPROTO_TCP : IPROTO_UDP;
    nlu.direction = PF_OUT;

    if( ioctl( natfd, DIOCNATLOOK, &nlu ) ) {
        if( errno != ESRCH ) {
            close( natfd );
            natfd = -1;
            return natIFaceErr;
        } else
            return natSearchErr;
    }

    if( local.sin_addr.s_addr == nlu.raddr.v4.s_addr )
        return natSearchErr;

    nat->sin_family = local.sin_family;
    nat->sin_port = nlu.rdport;
    nat->sin_addr = nlu.rdaddr.v4;

    return natOK;
}

#ifdef CCXX_IPV6
natResult natv6Lookup( SOCKET sfd, struct sockaddr_in6 * nat )
{
    static int natfd = -1;
    struct sockaddr_in6 local, peer;
    socklen_t  nlen = sizeof( *nat ), llen = sizeof( local ), plen = sizeof( peer );
    int socktype;
    socklen_t stlen = sizeof( socktype );
    struct pfioc_natlook nlu;
    int nres;

    if( natfd < 0 )
        if( ( natfd = open( NAT_DEVICE, O_RDWR ) ) < 0 )
            return natDevUnavail;

    if( getsockname( sfd, ( struct sockaddr * ) &local, &llen ) )
        return natSocknameErr;
    if( getpeername( sfd, ( struct sockaddr * ) &peer, &plen ) )
        return natPeernameErr;
    if( getsockopt( sfd, SOL_SOCKET, SO_TYPE, ( int * ) &socktype, &stlen ) )
        return natSockTypeErr;

    memset( &nlu, 0, sizeof( nlu ) );
    nlu.daddr.v6.s6_addr = local.sin6_addr.s6_addr;
    nlu.dport = local.sin6_port;
    nlu.saddr.v6.s6_addr = peer.sin6_addr.s6_addr;
    nlu.sport = peer.sin6_port;
    nlu.af = AF_INET6;
    nlu.proto = ( socktype == SOCK_STREAM ) ? IPPROTO_TCP : IPROTO_UDP;
    nlu.direction = PF_OUT;

    if( ioctl( natfd, DIOCNATLOOK, &nlu ) ) {
        if( errno != ESRCH ) {
            close( natfd );
            natfd = -1;
            return natIFaceErr;
        } else
            return natSearchErr;
    }

    if( local.sin6_addr.s6_addr == nlu.raddr.v6.s6_addr )
        return natSearchErr;

    nat->sin6_family = local.sin6_family;
    nat->sin6_flowinfo = local.sin6_flowinfo;
    nat->sin6_port = nlu.rdport;
    nat->sin6_addr = nlu.rdaddr.v6;

    return natOK;
}
#endif
#endif

const char * natErrorString( natResult res )
{
    return (char *)natmsg[ ( res >= natOK && res <= natIFaceErr ) ? res : natUnkownErr ];
}

#else
natResult natv4Lookup( SOCKET sfd, struct sockaddr_in * nat )
{
    return natNotSupported;
}

#ifdef CCXX_IPV6
natResult natv6Lookup( SOCKET sfd, struct sockaddr_in6 * nat )
{
    return natNotSupported;
}
#endif

const char * natErrorString( natResult res )
{
    return "nat support not included";
}

#endif

#ifdef  CCXX_NAMESPACES
}
#endif

