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
#ifdef  HAVE_ENDIAN_H
#include <endian.h>
#endif
#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/thread.h>
#include <commoncpp/address.h>
#include <cstdlib>

#ifndef _MSWINDOWS_
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#endif

#if defined(_MSWINDOWS_) && !defined(__BIG_ENDIAN)
#define __LITTLE_ENDIAN 1234
#define __BIG_ENDIAN    4321
#define __PDP_ENDIAN    3412
#define __BYTE_ORDER    __LITTLE_ENDIAN
#endif

using namespace COMMONCPP_NAMESPACE;

#if __BYTE_ORDER == __BIG_ENDIAN
enum {
    MCAST_VALID_MASK = 0xF0000000,
    MCAST_VALID_VALUE = 0xE0000000
};
#else
enum {
    MCAST_VALID_MASK = 0x000000F0,
    MCAST_VALID_VALUE = 0x000000E0
};
#endif

#ifndef _MSWINDOWS_
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

IPV4Address::IPV4Address(const IPV4Validator *_validator) : 
validator(_validator), ipaddr(NULL), addr_count(0), hostname(NULL) 
{
    *this = (long unsigned int)INADDR_ANY;
}

IPV4Address::IPV4Address(const char *address, const IPV4Validator *_validator) :
validator(_validator), ipaddr(NULL), addr_count(0), hostname(NULL) 
{
    if(address == 0 || !strcmp(address, "*"))
        setAddress(NULL);
    else
        setAddress(address);
}

IPV4Address::IPV4Address(struct in_addr addr, const IPV4Validator *_validator) :
validator(_validator), ipaddr(NULL), hostname(NULL) 
{
    if ( this->validator ) {
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
        uint32_t addr;
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
            memcmp((char *)&smaller->ipaddr[s], (char *)&larger->ipaddr[l], sizeof(struct in_addr)); l++); 
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

#if defined(_MSWINDOWS_)
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

#ifdef _MSWINDOWS_
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
#elif (defined(__osf__) || defined(_MSWINDOWS_))
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
            memset(ipaddr, 0, sizeof(struct in_addr));
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

#ifdef _MSWINDOWS_
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
#elif defined(__osf__) || defined(_MSWINDOWS_)
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
    IPV4Address(address, &validator)
{}

IPV4Multicast::IPV4Multicast(const char *address) :
    IPV4Address(address, &validator)
{}

#ifdef  CCXX_IPV6

#ifndef _MSWINDOWS_
Mutex IPV6Address::mutex;
#endif

const IPV6MulticastValidator IPV6Multicast::validator;

void IPV6MulticastValidator::operator()(const in6_addr address) const
{
#ifdef  CCXX_EXCEPTIONS
    // "0000:" is always accepted, as it is an "empty" address.
    if ( (address.s6_addr[0] != 0 || address.s6_addr[1] != 0) &&
         (address.s6_addr[0] != 0xff || address.s6_addr[1] < 0x1f)) {
        throw "Multicast address not in the valid prefix ff00-ff1f:";
    }
#endif
}

IPV6Address::IPV6Address(const IPV6Validator *_validator) : 
validator(_validator), hostname(NULL) 
{
    addr_count = 1;
    ipaddr = new struct in6_addr[1];
    memcpy(ipaddr, &in6addr_any, sizeof(struct in6_addr));
}

IPV6Address::IPV6Address(const char *address, const IPV6Validator *_validator) :
validator(_validator), ipaddr(NULL), addr_count(0), hostname(NULL) 
{
    if(address == 0 || !strcmp(address, "*"))
        setAddress(NULL);
    else
        setAddress(address);
}

IPV6Address::IPV6Address(struct in6_addr addr, const IPV6Validator *_validator) :
validator(_validator), ipaddr(NULL), hostname(NULL) 
{
    if ( this->validator ) {
        (*validator)(addr);
    }
    addr_count = 1;
    ipaddr = new struct in6_addr[1];
    memcpy(&ipaddr, &addr, sizeof(struct in6_addr));
}

IPV6Address::IPV6Address(const IPV6Address &rhs) :
    validator(rhs.validator), addr_count(rhs.addr_count), hostname(NULL) {
    ipaddr = new struct in6_addr[addr_count];
    memcpy(ipaddr, rhs.ipaddr, sizeof(struct in6_addr) * addr_count);
}

IPV6Address::~IPV6Address()
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

struct in6_addr IPV6Address::getAddress(void) const
{
    return ipaddr[0];
}

struct in6_addr IPV6Address::getAddress(size_t i) const
{
    return (i < addr_count ? ipaddr[i] : ipaddr[0]);
}

bool IPV6Address::isInetAddress(void) const
{
    struct in6_addr addr;
    memset(&addr, 0, sizeof(addr));
    if(!ipaddr)
        return false;
    if(memcmp(&addr, &ipaddr[0], sizeof(addr)))
        return true;
    return false;
}

IPV6Address &IPV6Address::operator=(const char *str)
{
    if(str == 0 || !strcmp(str, "*"))
        str = "::";

    setAddress(str);

    return *this;
}

IPV6Address &IPV6Address::operator=(struct in6_addr addr)
{
    if(ipaddr)
        delete[] ipaddr;
    if ( validator )
        (*validator)(addr);
    addr_count = 1;
    ipaddr = new struct in6_addr[1];
    ipaddr[0] = addr;
    if(hostname)
        delString(hostname);
    hostname = NULL;
    return *this;
}

IPV6Address &IPV6Address::operator=(const IPV6Address &rhs)
{
    if(this == &rhs) return *this;

    addr_count = rhs.addr_count;
    if(ipaddr)
        delete[] ipaddr;
    ipaddr = new struct in6_addr[addr_count];
    memcpy(ipaddr, rhs.ipaddr, sizeof(struct in6_addr) * addr_count);
    validator = rhs.validator;
    if(hostname)
        delString(hostname);
    hostname = NULL;

    return *this;
}

bool IPV6Address::operator==(const IPV6Address &a) const
{
    const IPV6Address *smaller, *larger;
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
            memcmp((char *)&smaller->ipaddr[s], (char *)&larger->ipaddr[l], sizeof(struct in6_addr)); l++);
        if(l == larger->addr_count) return false;
    }
    return true;
}

bool IPV6Address::operator!=(const IPV6Address &a) const
{
    // Impliment in terms of operator==
    return (*this == a ? false : true);
}

IPV6Host &IPV6Host::operator&=(const IPV6Mask &ma)
{
    for(size_t i = 0; i < addr_count; i++) {
        struct in6_addr mask = ma.getAddress();
        unsigned char *a = (unsigned char *)&ipaddr[i];
        unsigned char *m = (unsigned char *)&mask;

        for(size_t j = 0; j < sizeof(struct in6_addr); ++j)
            *(a++) &= *(m++);
    }
    if(hostname)
        delString(hostname);
    hostname = NULL;

    return *this;
}

IPV6Host::IPV6Host(struct in6_addr addr) :
IPV6Address(addr) {}

IPV6Host::IPV6Host(const char *host) :
IPV6Address(host)
{
    char namebuf[256];

    if(!host) {
        gethostname(namebuf, 256);
        setAddress(namebuf);
    }
}

bool IPV6Address::setIPAddress(const char *host)
{
    if(!host)
        return false;

    struct in6_addr l_addr;

#ifdef  _MSWINDOWS_
    struct sockaddr saddr;
    int slen = sizeof(saddr);
    struct sockaddr_in6 *paddr = (struct sockaddr_in6 *)&saddr;
    int ok = WSAStringToAddress((LPSTR)host, AF_INET6, NULL, &saddr, &slen);
    l_addr = paddr->sin6_addr;
#else
    int ok = inet_pton(AF_INET6, host, &l_addr);
#endif
    if ( validator )
        (*validator)(l_addr);
    if ( !ok )
        return false;
    *this = l_addr;
    return true;
}

#if defined(HAVE_GETADDRINFO) && !defined(HAVE_GETHOSTBYNAME2)

void IPV6Address::setAddress(const char *host)
{
    if(hostname)
        delString(hostname);
    hostname = NULL;

    if(!host)  // The way this is currently used, this can never happen
        host = "::";

#ifdef  _MSWINDOWS_
    if(!stricmp(host, "localhost"))
        host = "::1";
#endif

    if(!setIPAddress(host)) {
        struct addrinfo hint, *list = NULL, *first;
        memset(&hint, 0, sizeof(hint));
        hint.ai_family = AF_INET6;
        struct in6_addr *addr;
        struct sockaddr_in6 *ip6addr;

        if(getaddrinfo(host, NULL, &hint, &list) || !list) {
            if(ipaddr)
                delete[] ipaddr;
            ipaddr = new struct in6_addr[1];
            memset((void *)&ipaddr[0], 0, sizeof(struct in6_addr));
            return;
        }

        // Count the number of IP addresses returned
        addr_count = 0;
        first = list;
        while(list) {
            ++addr_count;
            list = list->ai_next;
        }

        // Allocate enough memory
        if(ipaddr)
            delete[] ipaddr;    // Cause this was allocated in base
        ipaddr = new struct in6_addr[addr_count];

        // Now go through the list again assigning to
        // the member ipaddr;
        list = first;
        int i = 0;
        while(list) {
            ip6addr = (struct sockaddr_in6 *)list->ai_addr;
            addr = &ip6addr->sin6_addr;
            if(validator)
                (*validator)(*addr);
            ipaddr[i++] = *addr;
            list = list->ai_next;
        }
        freeaddrinfo(first);
    }
}

#else

void IPV6Address::setAddress(const char *host)
{
    if(hostname)
        delString(hostname);
    hostname = NULL;

    if(!host)  // The way this is currently used, this can never happen
        host = "::";

#ifdef  _MSWINDOWS_
    if(!stricmp(host, "localhost"))
        host = "::1";
#endif

    if(!setIPAddress(host)) {
        struct hostent *hp;
        struct in6_addr **bptr;
#if defined(__GLIBC__)
        char   hbuf[8192];
        struct hostent hb;
        int    rtn;

        if(gethostbyname2_r(host, AF_INET6, &hb, hbuf, sizeof(hbuf), &hp, &rtn))
            hp = NULL;
#elif defined(sun)
        char   hbuf[8192];
        struct hostent hb;
        int    rtn;

        hp = gethostbyname2_r(host, AF_INET6, &hb, hbuf, sizeof(hbuf), &rtn);
#elif (defined(__osf__) || defined(_OSF_SOURCE) || defined(__hpux))
        hp = gethostbyname(host);
#elif defined(_MSWINDOWS_) && (!defined(_MSC_VER) || _MSC_VER < 1300)
        hp = gethostbyname(host);
#elif defined(_MSWINDOWS_)
        hp = gethostbyname2(host, AF_INET6);
#else
        mutex.enterMutex();
        hp = gethostbyname2(host, AF_INET6);
        mutex.leaveMutex();
#endif
        if(!hp) {
            if(ipaddr)
                delete[] ipaddr;
            ipaddr = new struct in6_addr[1];
            memset((void *)&ipaddr[0], 0, sizeof(struct in6_addr));
            return;
        }

        // Count the number of IP addresses returned
        addr_count = 0;
        for(bptr = (struct in6_addr **)hp->h_addr_list; *bptr != NULL; bptr++) {
            addr_count++;
        }

        // Allocate enough memory
        if(ipaddr)
            delete[] ipaddr;    // Cause this was allocated in base
        ipaddr = new struct in6_addr[addr_count];

        // Now go through the list again assigning to
        // the member ipaddr;
        bptr = (struct in6_addr **)hp->h_addr_list;
        for(unsigned int i = 0; i < addr_count; i++) {
            if ( validator )
                (*validator)(*bptr[i]);
            ipaddr[i] = *bptr[i];
        }
    }
}

#endif

IPV6Broadcast::IPV6Broadcast(const char *net) :
IPV6Address(net)
{
}

IPV6Mask::IPV6Mask(const char *mask) :
IPV6Address(mask)
{
}

const char *IPV6Address::getHostname(void) const
{
    struct hostent *hp = NULL;
    struct in6_addr addr0;
    static char strbuf[64];

    memset(&addr0, 0, sizeof(addr0));
    if(!memcmp(&addr0, &ipaddr[0], sizeof(addr0)))
        return NULL;

    if(!memcmp(&in6addr_loopback, &ipaddr[0], sizeof(addr0)))
        return "localhost";

#if defined(__GLIBC__)
    char   hbuf[8192];
    struct hostent hb;
    int    rtn;
    if(gethostbyaddr_r((char *)&ipaddr[0], sizeof(addr0), AF_INET6, &hb, hbuf, sizeof(hbuf), &hp, &rtn))
        hp = NULL;
#elif defined(sun)
    char   hbuf[8192];
    struct hostent hb;
    int    rtn;
    hp = gethostbyaddr_r((char *)&ipaddr[0], sizeof(addr0), AF_INET6, &hb, hbuf, (int)sizeof(hbuf), &rtn);
#elif defined(__osf__) || defined(_MSWINDOWS_)
    hp = gethostbyaddr((char *)&ipaddr[0], sizeof(addr0), AF_INET6);
#else
    mutex.enterMutex();
    hp = gethostbyaddr((char *)&ipaddr[0], sizeof(addr0), AF_INET6);
    mutex.leaveMutex();
#endif
    if(hp) {
        if(hostname)
            delString(hostname);
        hostname = newString(hp->h_name);
        return hostname;
    } else {
#ifdef  _MSWINDOWS_
        struct sockaddr saddr;
        struct sockaddr_in6 *paddr = (struct sockaddr_in6 *)&saddr;
        DWORD slen = sizeof(strbuf);
        memset(&saddr, 0, sizeof(saddr));
        paddr->sin6_family = AF_INET6;
        paddr->sin6_addr = ipaddr[0];
        WSAAddressToString(&saddr, sizeof(saddr), NULL, strbuf, &slen);
        return strbuf;
#else
        return inet_ntop(AF_INET6, &ipaddr[0], strbuf, sizeof(strbuf));
#endif
    }
}

IPV6Host operator&(const IPV6Host &addr, const IPV6Mask &mask)
{
    IPV6Host temp = addr;
    temp &= mask;
    return temp;
}

IPV6Multicast::IPV6Multicast() :
IPV6Address(&validator)
{}

IPV6Multicast::IPV6Multicast(const char *address) :
IPV6Address(address,&validator)
{}

#endif

NAMESPACE_COMMONCPP
using namespace std;

ostream& operator<<(ostream &os, const IPV4Address &ia)
{
    os << inet_ntoa(getaddress(ia));
    return os;
}

END_NAMESPACE

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
#ifdef  _MSWINDOWS_
    DWORD mask;
#else
    uint32_t mask;
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
#ifdef  _MSWINDOWS_
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

#ifdef  _MSWINDOWS_
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

#ifdef  _MSWINDOWS_
    int slen = sizeof(network);
    WSAStringToAddressA(cbuf, AF_INET6, NULL, (struct sockaddr*)&network, &slen);
#else
    inet_pton(AF_INET6, cbuf, &network);
#endif
    bitmask((bit_t *)&network, (bit_t *)&netmask, sizeof(network));
}

#endif

