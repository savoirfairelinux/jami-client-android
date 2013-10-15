// Copyright (C) 2006-2010 David Sugar, Tycho Softworks
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
#include <cc++/address.h>
#include "private.h"
#include <cstdlib>
#include <fcntl.h>
#include <cstdio>


#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

typedef unsigned char   bit_t;

static void bitmask(bit_t *bits, bit_t *mask, unsigned len)
{
    while(len--)
        *(bits++) &= *(mask++);
}

static void bitimask(bit_t *bits, bit_t *mask, unsigned len)
{
    while(len--)
        *(bits++) |= ~(*(mask++));
}

static void bitset(bit_t *bits, unsigned blen)
{
    bit_t mask;

    while(blen) {
        mask = (bit_t)(1 << 7);
        while(mask && blen) {
            *bits |= mask;
            mask >>= 1;
            --blen;
        }
        ++bits;
    }
}

static unsigned bitcount(bit_t *bits, unsigned len)
{
    unsigned count = 0;
    bit_t mask, test;

    while(len--) {
        mask = (bit_t)(1<<7);
        test = *bits++;
        while(mask) {
            if(!(mask & test))
                return count;
            ++count;
            mask >>= 1;
        }
    }
    return count;
}

IPV4Cidr::IPV4Cidr()
{
    memset(&network, 0, sizeof(network));
    memset(&netmask, 0, sizeof(netmask));
}

IPV4Cidr::IPV4Cidr(const char *cp)
{
    set(cp);
}

IPV4Cidr::IPV4Cidr(IPV4Cidr &cidr)
{
    memcpy(&network, &cidr.network, sizeof(network));
    memcpy(&netmask, &cidr.netmask, sizeof(netmask));
}

bool IPV4Cidr::isMember(const struct in_addr &addr) const
{
    struct in_addr host = addr;

    bitmask((bit_t *)&host, (bit_t *)&netmask, sizeof(host));
    if(!memcmp(&host, &network, sizeof(host)))
        return true;

    return false;
}

bool IPV4Cidr::isMember(const struct sockaddr *saddr) const
{
    struct sockaddr_in *addr = (struct sockaddr_in *)saddr;
    struct in_addr host;

    if(saddr->sa_family != AF_INET)
        return false;

    memcpy(&host, &addr->sin_addr.s_addr, sizeof(host));
    bitmask((bit_t *)&host, (bit_t *)&netmask, sizeof(host));
    if(!memcmp(&host, &network, sizeof(host)))
        return true;

    return false;
}

struct in_addr IPV4Cidr::getBroadcast(void) const
{
    struct in_addr bcast;
    memcpy(&bcast, &network, sizeof(network));
    bitimask((bit_t *)&bcast, (bit_t *)&netmask, sizeof(bcast));
    return bcast;
}

unsigned IPV4Cidr::getMask(const char *cp) const
{
    unsigned dcount = 0;
    const char *gp = cp;
    const char *mp = strchr(cp, '/');
    unsigned char dots[4];
#ifdef  WIN32
    DWORD mask;
#else
    uint32 mask;
#endif

    if(mp) {
        if(!strchr(++mp, '.'))
            return atoi(mp);

        mask = inet_addr(mp);
        return bitcount((bit_t *)&mask, sizeof(mask));
    }

    memset(dots, 0, sizeof(dots));
    dots[0] = atoi(cp);
    while(*gp && dcount < 3) {
        if(*(gp++) == '.')
            dots[++dcount] = atoi(gp);
    }

    if(dots[3])
        return 32;

    if(dots[2])
        return 24;

    if(dots[1])
        return 16;

    return 8;
}

void IPV4Cidr::set(const char *cp)
{
    char cbuf[INET_IPV4_ADDRESS_SIZE];
    char *ep;
    unsigned dots = 0;
#ifdef  WIN32
    DWORD addr;
#endif

    memset(&netmask, 0, sizeof(netmask));
    bitset((bit_t *)&netmask, getMask(cp));
    setString(cbuf, sizeof(cbuf), cp);

    ep = (char *)strchr(cp, '/');

    if(ep)
        *ep = 0;

    cp = cbuf;
    while(NULL != (cp = strchr(cp, '.'))) {
        ++dots;
        ++cp;
    }

    while(dots++ < 3)
        addString(cbuf, sizeof(cbuf), ".0");

#ifdef  WIN32
    addr = inet_addr(cbuf);
    memcpy(&network, &addr, sizeof(network));
#else
    inet_aton(cbuf, &network);
#endif
    bitmask((bit_t *)&network, (bit_t *)&netmask, sizeof(network));
}




#ifdef  CCXX_IPV6

IPV6Cidr::IPV6Cidr()
{
    memset(&network, 0, sizeof(network));
    memset(&netmask, 0, sizeof(netmask));
}

IPV6Cidr::IPV6Cidr(const char *cp)
{
    set(cp);
}

IPV6Cidr::IPV6Cidr(IPV6Cidr &cidr)
{
    memcpy(&network, &cidr.network, sizeof(network));
    memcpy(&netmask, &cidr.netmask, sizeof(netmask));
}

bool IPV6Cidr::isMember(const struct in6_addr &addr) const
{
    struct in6_addr host = addr;

    bitmask((bit_t *)&host, (bit_t *)&netmask, sizeof(host));
    if(!memcmp(&host, &network, sizeof(host)))
        return true;

    return false;
}

bool IPV6Cidr::isMember(const struct sockaddr *saddr) const
{
    struct sockaddr_in6 *addr = (struct sockaddr_in6 *)saddr;
    struct in6_addr host;

    if(saddr->sa_family != AF_INET6)
        return false;

    memcpy(&host, &addr->sin6_addr, sizeof(host));
    bitmask((bit_t *)&host, (bit_t *)&netmask, sizeof(host));
    if(!memcmp(&host, &network, sizeof(host)))
        return true;

    return false;
}

struct in6_addr IPV6Cidr::getBroadcast(void) const
{
    struct in6_addr bcast;
    memcpy(&bcast, &network, sizeof(network));
    bitimask((bit_t *)&bcast, (bit_t *)&netmask, sizeof(bcast));
    return bcast;
}

unsigned IPV6Cidr::getMask(const char *cp) const
{
    unsigned count = 0, rcount = 0;
    const char *sp = strchr(cp, '/');
    int flag = 0;

    if(sp)
        return atoi(++sp);

    if(!strncmp(cp, "ff00:", 5))
        return 8;

    if(!strncmp(cp, "fe80:", 5))
        return 10;

    if(!strncmp(cp, "2002:", 5))
        return 16;

    sp = strrchr(cp, ':');
    while(*(++sp) == '0')
        ++sp;
    if(*sp)
        return 128;

    while(*cp && count < 128) {
        if(*(cp++) == ':') {
            count+= 16;
            while(*cp == '0')
                ++cp;
            if(*cp == ':') {
                if(!flag)
                    rcount = count;
                flag = 1;
            }
            else
                flag = 0;
        }
    }
    return rcount;
}

void IPV6Cidr::set(const char *cp)
{
    char cbuf[INET_IPV6_ADDRESS_SIZE];
    char *ep;

    memset(&netmask, 0, sizeof(netmask));
    bitset((bit_t *)&netmask, getMask(cp));
    setString(cbuf, sizeof(cbuf), cp);
    ep = (char *)strchr(cp, '/');
    if(ep)
        *ep = 0;

    inet_pton(AF_INET6, cbuf, &network);
    bitmask((bit_t *)&network, (bit_t *)&netmask, sizeof(network));
}

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
