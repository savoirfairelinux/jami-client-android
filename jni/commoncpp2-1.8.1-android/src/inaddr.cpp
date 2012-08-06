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
#include <cc++/address.h>
#include "private.h"
#include <cstdlib>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifndef WIN32
Mutex IPV4Address::mutex;
#endif

IPV4Host IPV4Host::_host_;

const IPV4MulticastValidator IPV4Multicast::validator;

void IPV4MulticastValidator::operator()(const in_addr address) const
{
#ifdef  CCXX_EXCEPTIONS
    // "0.0.0.0" is always accepted, as it is an "empty" address.
    if ( (address.s_addr != INADDR_ANY) &&
         (address.s_addr & MCAST_VALID_MASK) != MCAST_VALID_VALUE ) {
        throw "Multicast address not in the valid range: from 224.0.0.1 through 239.255.255.255";
    }
#endif
}

IPV4Address::IPV4Address(const IPV4Validator *_validator)
    : validator(_validator), ipaddr(NULL), addr_count(0), hostname(NULL) {
    *this = (long unsigned int)INADDR_ANY;
}

IPV4Address::IPV4Address(const char *address, const IPV4Validator *_validator) :
    validator(_validator), ipaddr(NULL), addr_count(0), hostname(NULL) {
    if ( this->validator )
        this->validator = validator;
    if(address == 0 || !strcmp(address, "*"))
        setAddress(NULL);
    else
        setAddress(address);
}

IPV4Address::IPV4Address(struct in_addr addr, const IPV4Validator *_validator) :
    validator(_validator), ipaddr(NULL), hostname(NULL) {
    if ( this->validator ){
        this->validator = validator;
        (*validator)(addr);
    }
    addr_count = 1;
    ipaddr = new struct in_addr[1];
    ipaddr[0] = addr;
}

IPV4Address::IPV4Address(const IPV4Address &rhs) :
validator(rhs.validator), addr_count(rhs.addr_count), hostname(NULL)
{
    ipaddr = new struct in_addr[addr_count];
    memcpy(ipaddr, rhs.ipaddr, sizeof(struct in_addr) * addr_count);
}

IPV4Address::~IPV4Address()
{
    if(ipaddr) {
        delete[] ipaddr;
        ipaddr = NULL;
    }
    if(hostname) {
        delString(hostname);
        hostname = NULL;
    }
}

struct in_addr IPV4Address::getAddress(void) const
{
    return ipaddr[0];
}

struct in_addr IPV4Address::getAddress(size_t i) const
{
    return (i < addr_count ? ipaddr[i] : ipaddr[0]);
}

bool IPV4Address::isInetAddress(void) const
{
    struct in_addr addr;
    memset(&addr, 0, sizeof(addr));
    if(memcmp(&addr, &ipaddr[0], sizeof(addr)))
        return true;
    return false;
}

IPV4Address &IPV4Address::operator=(const char *str)
{
    if(str == 0 || !strcmp(str, "*"))
        str = "0.0.0.0";

    setAddress(str);

    return *this;
}

IPV4Address &IPV4Address::operator=(struct in_addr addr)
{
    if(ipaddr)
        delete[] ipaddr;
    if ( validator )
        (*validator)(addr);
    addr_count = 1;
    ipaddr = new struct in_addr[1];
    ipaddr[0] = addr;
    if(hostname)
        delString(hostname);
    hostname = NULL;
    return *this;
}

IPV4Address &IPV4Address::operator=(unsigned long addr)
{
    union {
        uint32 addr;
        struct in_addr in4;
    } aptr;

    aptr.addr = addr;

    if ( validator )
        (*validator)(aptr.in4);

    if(ipaddr)
        delete[] ipaddr;

    addr_count = 1;
    ipaddr = new struct in_addr[1];
    memcpy(ipaddr, &aptr.in4, sizeof(struct in_addr));
    if(hostname)
        delString(hostname);
    hostname = NULL;
    return *this;
}

IPV4Address &IPV4Address::operator=(const IPV4Address &rhs)
{
    if(this == &rhs) return *this;

    addr_count = rhs.addr_count;
    if(ipaddr)
        delete[] ipaddr;
    ipaddr = new struct in_addr[addr_count];
    memcpy(ipaddr, rhs.ipaddr, sizeof(struct in_addr) * addr_count);
    validator = rhs.validator;
    if(hostname)
        delString(hostname);
    hostname = NULL;

    return *this;
}

bool IPV4Address::operator==(const IPV4Address &a) const
{
    const IPV4Address *smaller, *larger;
    size_t s, l;

    if(addr_count > a.addr_count) {
        smaller = &a;
        larger  = this;
    }
    else {
        smaller = this;
        larger  = &a;
    }

    // Loop through all addr's in the smaller and make sure
    // that they are all in the larger
    for(s = 0; s < smaller->addr_count; s++) {
        // bool found = false;
        for(l = 0; l < larger->addr_count &&
            memcmp((char *)&ipaddr[s], (char *)&a.ipaddr[l], sizeof(struct in_addr)); l++);
        if(l == larger->addr_count) return false;
    }
    return true;
}

bool IPV4Address::operator!=(const IPV4Address &a) const
{
    // Impliment in terms of operator==
    return (*this == a ? false : true);
}

IPV4Host &IPV4Host::operator&=(const IPV4Mask &ma)
{
    for(size_t i = 0; i < addr_count; i++) {
        struct in_addr mask = ma.getAddress();
        unsigned char *a = (unsigned char *)&ipaddr[i];
        unsigned char *m = (unsigned char *)&mask;

        for(size_t j = 0; j < sizeof(struct in_addr); ++j)
            *(a++) &= *(m++);
    }
    if(hostname)
        delString(hostname);
    hostname = NULL;

    return *this;
}

IPV4Host::IPV4Host(struct in_addr addr) :
IPV4Address(addr) {}

IPV4Host::IPV4Host(const char *host) :
IPV4Address(host)
{
    char namebuf[256];

    if(!host) {
        if(this == &_host_) {
            gethostname(namebuf, 256);
            setAddress(namebuf);
        }
        else
            *this = _host_;
    }
}

bool IPV4Address::setIPAddress(const char *host)
{
    if(!host)
        return false;

#if defined(WIN32)
    struct sockaddr_in addr;
    addr.sin_addr.s_addr = inet_addr(host);
    if ( validator )
        (*validator)(addr.sin_addr);
    if(addr.sin_addr.s_addr == INADDR_NONE)
        return false;
    *this = addr.sin_addr.s_addr;
#else
    struct in_addr l_addr;

    int ok = inet_aton(host, &l_addr);
    if ( validator )
        (*validator)(l_addr);
    if ( !ok )
        return false;
    *this = l_addr;
#endif
    return true;
}

void IPV4Address::setAddress(const char *host)
{
    if(hostname)
        delString(hostname);
    hostname = NULL;

    if(!host)  // The way this is currently used, this can never happen
    {
        *this = (long unsigned int)htonl(INADDR_ANY);
        return;
    }

#ifdef  WIN32
    if(!stricmp(host, "localhost")) {
        *this = (long unsigned int)inet_addr("127.0.0.1");
        return;
    }
#endif

    if(!setIPAddress(host)) {
        struct hostent *hp;
        struct in_addr **bptr;
#if defined(__GLIBC__)
        char   hbuf[8192];
        struct hostent hb;
        int    rtn;

        if(gethostbyname_r(host, &hb, hbuf, sizeof(hbuf), &hp, &rtn))
            hp = NULL;
#elif defined(sun)
        char   hbuf[8192];
        struct hostent hb;
        int    rtn;

        hp = gethostbyname_r(host, &hb, hbuf, sizeof(hbuf), &rtn);
#elif (defined(__osf__) || defined(WIN32))
        hp = gethostbyname(host);
#else
        mutex.enterMutex();
        hp = gethostbyname(host);
        mutex.leaveMutex();
#endif
        if(!hp) {
            if(ipaddr)
                delete[] ipaddr;
            ipaddr = new struct in_addr[1];
            memset((void *)&ipaddr[0], 0, sizeof(ipaddr));
            return;
        }

        // Count the number of IP addresses returned
        addr_count = 0;
        for(bptr = (struct in_addr **)hp->h_addr_list; *bptr != NULL; bptr++) {
            addr_count++;
        }

        // Allocate enough memory
        if(ipaddr)
            delete[] ipaddr;    // Cause this was allocated in base
        ipaddr = new struct in_addr[addr_count];

        // Now go through the list again assigning to
        // the member ipaddr;
        bptr = (struct in_addr **)hp->h_addr_list;
        for(unsigned int i = 0; i < addr_count; i++) {
            if ( validator )
                (*validator)(*bptr[i]);
            ipaddr[i] = *bptr[i];
        }
    }
}

IPV4Broadcast::IPV4Broadcast(const char *net) :
IPV4Address(net)
{
}

IPV4Mask::IPV4Mask(const char *mask)
{
    unsigned long x = 0xffffffff;
    int l = 32 - atoi(mask);

    if(setIPAddress(mask))
        return;

    if(l < 1 || l > 32) {
#ifdef  CCXX_EXCEPTIONS
        if(Thread::getException() == Thread::throwObject)
            throw((IPV4Address *)this);
#endif
        return;
    }

    *this = htonl(x << l);
}

const char *IPV4Address::getHostname(void) const
{
    struct hostent *hp = NULL;
    struct in_addr addr0;

    memset(&addr0, 0, sizeof(addr0));
    if(!memcmp(&addr0, &ipaddr[0], sizeof(addr0)))
        return NULL;

#ifdef  WIN32
    memset(&addr0, 0xff, sizeof(addr0));
    if(!memcmp(&addr0, &ipaddr[0], sizeof(addr0)))
        return "255.255.255.255";
    long a = inet_addr("127.0.0.1");
    if(!memcmp(&a, &ipaddr[0], sizeof(a)))
        return "localhost";
#endif

#if defined(__GLIBC__)
    char   hbuf[8192];
    struct hostent hb;
    int    rtn;
    if(gethostbyaddr_r((char *)&ipaddr[0], sizeof(addr0), AF_INET, &hb, hbuf, sizeof(hbuf), &hp, &rtn))
        hp = NULL;
#elif defined(sun)
    char   hbuf[8192];
    struct hostent hb;
    int    rtn;
    hp = gethostbyaddr_r((char *)&ipaddr[0], (int)sizeof(addr0), (int)AF_INET, &hb, hbuf, (int)sizeof(hbuf), &rtn);
#elif defined(__osf__) || defined(WIN32)
    hp = gethostbyaddr((char *)&ipaddr[0], sizeof(addr0), AF_INET);
#else
    mutex.enterMutex();
    hp = gethostbyaddr((char *)&ipaddr[0], sizeof(addr0), AF_INET);
    mutex.leaveMutex();
#endif
    if(hp) {
        if(hostname)
            delString(hostname);
        hostname = newString(hp->h_name);
        return hostname;
    } else {
        return inet_ntoa(ipaddr[0]);
    }
}

IPV4Host operator&(const IPV4Host &addr, const IPV4Mask &mask)
{
    IPV4Host temp = addr;
    temp &= mask;
    return temp;
}

IPV4Multicast::IPV4Multicast() :
    IPV4Address(&validator)
{}

IPV4Multicast::IPV4Multicast(const struct in_addr address) :
    IPV4Address(address,&validator)
{}

IPV4Multicast::IPV4Multicast(const char *address) :
    IPV4Address(address,&validator)
{}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
