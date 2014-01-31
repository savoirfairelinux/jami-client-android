// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/socket.h>
#include <ucommon/string.h>
#include <ucommon/thread.h>
#include <ucommon/fsys.h>
#ifndef _MSWINDOWS_
#include <net/if.h>
#include <sys/un.h>
#include <sys/ioctl.h>
#include <arpa/inet.h>
#else
#define HAVE_GETADDRINFO 1
#endif
#ifdef  HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <errno.h>

#if defined(HAVE_SOCKS)
#include <socks.h>
#undef  HAVE_POLL_H
#undef  HAVE_SYS_POLL_H
#endif

#if defined(HAVE_POLL_H)
#include <poll.h>
#elif defined(HAVE_SYS_POLL_H)
#include <sys/poll.h>
#endif

#if defined(HAVE_SYS_FILIO_H)
#include <sys/filio.h>
#endif

#if defined(HAVE_POLL) && defined(POLLRDNORM)
#define USE_POLL
#endif

#if defined(__linux__) && !defined(IP_MTU)
#define IP_MTU 14
#endif

#ifndef MSG_DONTWAIT
#define MSG_DONTWAIT 0
#endif

#ifndef MSG_NOSIGNAL
#define MSG_NOSIGNAL 0
#endif

#ifdef  __FreeBSD__
#ifdef  AI_V4MAPPED
#undef  AI_V4MAPPED
#endif
#endif

typedef struct multicast_internet
{
    union {
        struct ip_mreq  ipv4;
#ifdef  AF_INET6
        struct ipv6_mreq ipv6;
#endif
    };
} inetmulticast_t;

#ifndef HAVE_GETADDRINFO

struct addrinfo {
    int ai_flags;
    int ai_family;
    int ai_socktype;
    int ai_protocol;
    size_t ai_addrlen;
    char *ai_canonname;
    struct sockaddr *ai_addr;
    struct addrinfo *ai_next;
};

#define NI_NUMERICHOST  0x0001
#define NI_NUMERICSERV  0x0002
#define NI_NAMEREQD     0x0004
#define NI_NOFQDN       0x0008
#define NI_DGRAM        0x0010

#define AI_PASSIVE      0x0100
#define AI_CANONNAME    0x0200
#define AI_NUMERICHOST  0x0400
#define AI_NUMERICSERV  0x0800

#endif

#if defined(HAVE_SOCKS)
#define _getsockname_(so, addr, alen) ::Rgetsockname(so, addr, alen)
#define _getpeername_(so, addr, alen) ::Rgetpeername(so, addr, alen)
#define _accept_(so, addr, alen) ::Raccept(so, addr, alen)
#define _connect_(so, addr, alen) ::Rconnect(so, addr, alen)
#define _bind_(so, addr, alen) ::Rbind(so, addr, alen)
#define _listen_(so, backlog) ::Rlisten(so, backlog)
#define _select_(cnt, rfd, wfd, efd, timeout) ::Rselect(cnt, rfd, wfd, efd, timeout)
#define _send_(so, buf, bytes, flag) ::Rsend(so, buf, bytes, flag)
#define _recv_(so, buf, bytes, flag) ::Rrecv(so, buf, bytes, flag)
#define _sendto_(so, buf, bytes, flag, to, tolen) ::Rsendto(so, buf, bytes, flag, to, tolen)
#define _recvfrom_(so, buf, bytes, flag, from, fromlen) ::Rrecvfrom(so, buf, bytes, flag, from, fromlen)
#undef  USE_POLL
#undef  accept
#undef  sendto
#undef  recvfrom
#undef  select
#elif defined(__PTH__)
#define _send_(so, buf, bytes, flag) pth_send(so, buf, bytes, flag)
#define _recv_(so, buf, bytes, flag) pth_recv(so, buf, bytes, flag)
#define _sendto_(so, buf, bytes, flag, to, tolen) pth_sendto(so, buf, bytes, flag, to, tolen)
#define _recvfrom_(so, buf, bytes, flag, from, fromlen) pth_recvfrom(so, buf, bytes, flag, from, fromlen)
#define _connect_(so, addr, addrlen) pth_connect(so, addr, addrlen)
#define _accept_(so, addr, addrlen) pth_accept(so, addr, addrlen)
#define _select_(cnt, rfd, wfd, efd, timeout) pth_select(cnt, rfd, wfd, efd, timeout)
#define _poll_(fds, cnt, timeout) pth_poll(fds, cnt, timeout)
#define _getsockname_(so, addr, alen) ::getsockname(so, addr, alen)
#define _getpeername_(so, addr, alen) ::getpeername(so, addr, alen)
#define _bind_(so, addr, alen) ::bind(so, addr, alen)
#define _listen_(so, count) ::listen(so, count)
#else
#define _send_(so, buf, bytes, flag) ::send(so, buf, bytes, flag)
#define _recv_(so, buf, bytes, flag) ::recv(so, buf, bytes, flag)
#define _sendto_(so, buf, bytes, flag, to, tolen) ::sendto(so, buf, bytes, flag, to, tolen)
#define _recvfrom_(so, buf, bytes, flag, from, fromlen) ::recvfrom(so, buf, bytes, flag, from, fromlen)
#define _connect_(so, addr, addrlen) ::connect(so, addr, addrlen)
#define _accept_(so, addr, addrlen) ::accept(so, addr, addrlen)
#define _select_(cnt, rfd, wfd, efd, timeout) ::select(cnt, rfd, wfd, efd, timeout)
#define _poll_(fds, cnt, timeout) ::poll(fds, cnt, timeout)
#define _getsockname_(so, addr, alen) ::getsockname(so, addr, alen)
#define _getpeername_(so, addr, alen) ::getpeername(so, addr, alen)
#define _bind_(so, addr, alen) ::bind(so, addr, alen)
#define _listen_(so, count) ::listen(so, count)
#endif

using namespace UCOMMON_NAMESPACE;

typedef unsigned char   bit_t;

static int query_family = 0;
static int v6only = 0;

static void socket_mapping(int family, socket_t so)
{
    if(so == INVALID_SOCKET)
        return;

#if defined(IPV6_V6ONLY) && defined(IPPROTO_IPV6)
    if(family == AF_INET6)
        setsockopt (so, IPPROTO_IPV6, IPV6_V6ONLY, (char *) &v6only, sizeof (v6only));
#endif
}

#ifndef HAVE_GETADDRINFO

static mutex servmutex, hostmutex;

static void freeaddrinfo(struct addrinfo *aip)
{
    while(aip != NULL) {
        struct addrinfo *next = aip->ai_next;
        if(aip->ai_canonname != NULL)
            free(aip->ai_canonname);
        if(aip->ai_addr != NULL)
            free(aip->ai_addr);
        free(aip);
        aip = next;
    }
}

static int getnameinfo(const struct sockaddr *addr, socklen_t len, char *host, size_t hostlen, char *service, size_t servlen, int flags)
{
    char *cp;
    struct hostent *hp;
    struct servent *sp;
    assert(addr != NULL);
    assert(host != NULL || hostlen == 0);
    assert(service != NULL || servlen == 0);

    short port = 0;

    switch(addr->sa_family) {
#ifdef  AF_UNIX
    case AF_UNIX:
        if(hostlen > 0)
            snprintf(host, hostlen, "%s", ((struct sockaddr_un *)addr)->sun_path);
        if(servlen > 0)
            snprintf(service, servlen, "%s", ((struct sockaddr_un *)addr)->sun_path);
        return 0;
#endif
#ifdef  AF_INET6
    case AF_INET6:
        port = ((struct sockaddr_in6 *)addr)->sin6_port;
        break;
#endif
    case AF_INET:
        port = ((struct sockaddr_in *)addr)->sin_port;
        break;
    default:
        return -1;
    }
    if(hostlen > 0) {
        if(flags & NI_NUMERICHOST) {
            if(inet_ntop(addr->sa_family, addr, host, hostlen) == NULL)
                return -1;
        }
        else {
            hostmutex.lock();
            hp = gethostbyaddr((caddr_t)addr, len, addr->sa_family);
            if(hp != NULL && hp->h_name != NULL) {
                if(flags & NI_NOFQDN) {
                    cp = strchr(hp->h_name, '.');
                    if(cp)
                        *cp = 0;
                }
                snprintf(host, hostlen, "%s", hp->h_name);
                hostmutex.unlock();
            }
            else {
                hostmutex.unlock();
                if(flags & NI_NAMEREQD)
                    return -1;
                if(inet_ntop(addr->sa_family, addr, host, hostlen) != NULL)
                    return -1;
            }
        }
    }
    if(servlen > 0) {
        if(flags & NI_NUMERICSERV)
            snprintf(service, servlen, "%d", ntohs(port));
        else {
            servmutex.lock();
            sp = getservbyport(port, (flags & NI_DGRAM) ? "udp" : NULL);
            if(sp && sp->s_name)
                snprintf(service, servlen, "%s", sp->s_name);
            else
                snprintf(service, servlen, "%d", ntohs(port));
            servmutex.unlock();
        }
    }
    return 0;
}

static int getaddrinfo(const char *hostname, const char *servname, const struct addrinfo *hintsp, struct addrinfo **res)
{
    int family;
    const char *servtype = "tcp";
    struct hostent *hp;
    struct servent *sp;
    char **np;
    struct addrinfo hints;
    struct addrinfo *aip = NULL, *prior = NULL;
    socklen_t len;
    short port = 0;
    struct sockaddr_in *ipv4;
#ifdef  AF_INET6
    struct sockaddr_in6 *ipv6;
#endif
    if(hintsp == NULL) {
        memset(&hints, 0, sizeof(hints));
        hints.ai_family = AF_UNSPEC;
    }
    else
        memcpy(&hints, hintsp, sizeof(hints));

    *res = NULL;

#ifdef  AF_UNIX
    if(hints.ai_family == AF_UNIX || (hints.ai_family == AF_UNSPEC && hostname && *hostname == '/')) {
        if(hints.ai_socktype == 0)
            hints.ai_socktype = SOCK_STREAM;

        aip = (struct addrinfo *)malloc(sizeof(struct addrinfo));
        memset(aip, 0, sizeof(struct addrinfo));
        aip->ai_next = NULL;
        aip->ai_canonname = NULL;
        aip->ai_protocol = hints.ai_protocol;
        struct sockaddr_un *unp;
        if(strlen(hostname) >= sizeof(unp->sun_path))
            return -1;
        unp = (struct sockaddr_un *)malloc(sizeof(struct sockaddr_un));
        memset(unp, 0, sizeof(struct sockaddr_un));
        unp->sun_family = AF_UNIX;
        String::set(unp->sun_path, sizeof(unp->sun_path), hostname);
#ifdef  __SUN_LEN
        len = sizeof(unp->sun_len) + strlen(unp->sun_path) +
            sizeof(unp->sun_family) + 1;
        unp->sun_len = len;
#else
        len = strlen(unp->sun_path) + sizeof(unp->sun_family) + 1;
#endif
        if(hints.ai_flags & AI_PASSIVE)
            unlink(unp->sun_path);
        aip->ai_addr = (struct sockaddr *)unp;
        aip->ai_addrlen = len;
        *res = aip;
        return 0;
    }
#endif

    if(servname && *servname) {
        if(servname[0] >= '0' && servname[0] <= '9') {
            port = htons(atoi(servname));
        }
        else {
            if(hints.ai_socktype == SOCK_DGRAM)
                servtype = "udp";
            servmutex.lock();
            sp = getservbyname(servname, servtype);
            if(!sp) {
                servmutex.unlock();
                return -1;
            }
            port = sp->s_port;
            servmutex.unlock();
        }
    }

    if((!hostname || !*hostname)) {
        aip = (struct addrinfo *)malloc(sizeof(struct addrinfo));
        memset(aip, 0, sizeof(struct addrinfo));
        aip->ai_canonname = NULL;
        aip->ai_socktype = hints.ai_socktype;
        aip->ai_protocol = hints.ai_protocol;
        aip->ai_next = NULL;

#ifdef  AF_INET6
        if(hints.ai_family == AF_INET6) {
            aip->ai_family = AF_INET6;
            ipv6 = (struct sockaddr_in6 *)malloc(sizeof(struct sockaddr_in6));
            memset(ipv6, 0, sizeof(struct sockaddr_in6));
            if(!(hints.ai_flags & AI_PASSIVE))
                inet_pton(AF_INET6, "::1", &ipv6->sin6_addr);
            ipv6->sin6_family = AF_INET6;
            ipv6->sin6_port = port;
            aip->ai_addr = (struct sockaddr *)ipv6;
            *res = aip;
            return 0;
        }
#endif
        aip->ai_family = AF_INET;
        ipv4 = (struct sockaddr_in*)malloc(sizeof(struct sockaddr_in));
        memset(ipv4, 0, sizeof(struct sockaddr_in));
        ipv4->sin_family = AF_INET;
        ipv4->sin_port = port;
        if(!(hints.ai_flags & AI_PASSIVE))
            inet_pton(AF_INET, "127.0.0.1", &ipv4->sin_addr);
        aip->ai_addr = (struct sockaddr *)ipv4;
        *res = aip;
        return 0;
    }
    family = hints.ai_family;
#ifdef  AF_UNSPEC
    if(family == AF_UNSPEC)
        family = AF_INET;
#endif
    hostmutex.lock();
#ifdef  HAVE_GETHOSTBYNAME2
    hp = gethostbyname2(hostname, family);
#else
    hp = gethostbyname(hostname);
#endif
    if(!hp) {
        hostmutex.unlock();
        return -1;
    }

    for(np = hp->h_addr_list; *np != NULL; np++) {
        aip = (struct addrinfo *)malloc(sizeof(struct addrinfo));
        memset(aip, 0, sizeof(struct addrinfo));
        if(hints.ai_flags & AI_CANONNAME)
            aip->ai_canonname = strdup(hp->h_name);
        else
            aip->ai_canonname = NULL;
        aip->ai_socktype = hints.ai_socktype;
        aip->ai_protocol = hints.ai_protocol;
        aip->ai_next = NULL;
        if(prior)
            prior->ai_next = aip;
        else
            *res = aip;
        prior = aip;

#ifdef  AF_INET6
        if(hints.ai_family == AF_INET6) {
            aip->ai_family = AF_INET6;
            ipv6 = (struct sockaddr_in6 *)malloc(sizeof(struct sockaddr_in6));
            memset(ipv6, 0, sizeof(struct sockaddr_in6));
            memcpy(&ipv6->sin6_addr, *np, sizeof(&ipv6->sin6_addr));
            ipv6->sin6_family = AF_INET6;
            ipv6->sin6_port = port;
            aip->ai_addr = (struct sockaddr *)ipv6;
            continue;
        }
#endif
        aip->ai_family = AF_INET;
        ipv4 = (struct sockaddr_in*)malloc(sizeof(struct sockaddr_in));
        memset(ipv4, 0, sizeof(struct sockaddr_in));
        ipv4->sin_family = AF_INET;
        ipv4->sin_port = port;
        memcpy(&ipv4->sin_addr, *np, sizeof(&ipv4->sin_addr));
        aip->ai_addr = (struct sockaddr *)ipv4;
    }

    hostmutex.unlock();
    if(*res)
        return 0;
    else
        return -1;
}
#endif

#if defined(AF_UNIX) && !defined(_MSWINDOWS_)

static socklen_t unixaddr(struct sockaddr_un *addr, const char *path)
{
    assert(addr != NULL);
    assert(path != NULL && *path != 0);

    socklen_t len;
    unsigned slen = strlen(path);

    if(slen > sizeof(struct sockaddr_storage) - 8)
        slen = sizeof(struct sockaddr_storage) - 8;

    memset(addr, 0, sizeof(struct sockaddr_storage));
    addr->sun_family = AF_UNIX;
    memcpy(addr->sun_path, path, slen);

#ifdef  __SUN_LEN
    len = sizeof(addr->sun_len) + strlen(addr->sun_path) +
        sizeof(addr->sun_family) + 1;
    addr->sun_len = len;
#else
    len = strlen(addr->sun_path) + sizeof(addr->sun_family) + 1;
#endif
    return len;
}

#endif

#ifndef AF_UNSPEC
#define AF_UNSPEC   0
#endif

static int setfamily(int family, const char *host)
{
    const char *hc = host;
    if(!host)
        return  family;

    if(!family || family == AF_UNSPEC) {
#ifdef  AF_INET6
        if(strchr(host, ':'))
            family = AF_INET6;
#endif
#ifdef  AF_UNIX
        if(*host == '/')
            family = AF_UNIX;
#endif
        while((*hc >= '0' && *hc <= '9') || *hc == '.')
            ++hc;
        if(!*hc)
            family = AF_INET;
    }

    if(!family || family == AF_UNSPEC)
        family = query_family;

    return family;
}

static void bitmask(bit_t *bits, bit_t *mask, unsigned len)
{
    assert(bits != NULL);
    assert(mask != NULL);

    while(len--)
        *(bits++) &= *(mask++);
}

static void bitimask(bit_t *bits, bit_t *mask, unsigned len)
{
    assert(bits != NULL);
    assert(mask != NULL);

    while(len--)
        *(bits++) |= ~(*(mask++));
}

static void bitset(bit_t *bits, unsigned blen)
{
    assert(bits != NULL);

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
    assert(bits != NULL);

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

#ifdef  _MSWINDOWS_

static bool _started = false;

static void _socketcleanup(void)
{
    if(_started)
        WSACleanup();
}

void Socket::init(void)
{
    static bool initialized = false;
    unsigned short version;
    WSADATA status;

    if(initialized)
        return;

    initialized = true;
    version = MAKEWORD(2,2);
    status.wVersion = 0;
    WSAStartup(version, &status);
    crit(status.wVersion == version, "socket init failure");
    atexit(_socketcleanup);
    _started = true;
}
#else
void Socket::init(void)
{
}
#endif

void Socket::init(const char *progname)
{
    assert(progname != NULL);

    Socket::init();
#ifdef  HAVE_SOCKS
    const char *cp;

#ifdef  _MSWINDOWS_
    char path[65];

    cp = strrchr(progname, '/');
    if(!cp)
        cp = strrchr(progname, '\\');
    if(!cp)
        cp = strrchr(progname, ':');
    if(cp)
        progname = ++cp;

    String::set(path, sizeof(path), progname);
    const char *ext = strrchr(path, '.');
    if(ext && (case_eq(ext, ".exe") || case_eq(ext, ".com")))
        *ext = 0;
    progname = path;
#else
    cp = strrchr(progname, '/');
    if(cp)
        progname = ++cp;
#endif

    SOCKSinit((char *)progname);
#endif
}

void Socket::v4mapping(bool enable)
{
    if(enable)
        v6only = 0;
    else
        v6only = 1;
}

void Socket::query(int querymode)
{
    query_family = querymode;
}

cidr::cidr() :
LinkedObject()
{
    Family = AF_UNSPEC;
    memset(&Network, 0, sizeof(Network));
    memset(&Netmask, 0, sizeof(Netmask));
    Name[0] = 0;
}

cidr::cidr(const char *cp) :
LinkedObject()
{
    assert(cp != NULL && *cp != 0);
    set(cp);
    Name[0] = 0;
}

cidr::cidr(policy **policy, const char *cp) :
LinkedObject(policy)
{
    assert(policy != NULL);
    assert(cp != NULL && *cp != 0);

    set(cp);
    Name[0] = 0;
}

cidr::cidr(policy **policy, const char *cp, const char *id) :
LinkedObject(policy)
{
    assert(policy != NULL);
    assert(cp != NULL && *cp != 0);
    assert(id != NULL && *id != 0);

    set(cp);
    String::set(Name, sizeof(Name), id);
}


cidr::cidr(const cidr &copy) :
LinkedObject()
{
    Family = copy.Family;
    memcpy(&Network, &copy.Network, sizeof(Network));
    memcpy(&Netmask, &copy.Netmask, sizeof(Netmask));
    memcpy(&Name, &copy.Name, sizeof(Name));
}

unsigned cidr::mask(void) const
{
    switch(Family)
    {
    case AF_INET:
        return bitcount((bit_t *)&Netmask.ipv4, sizeof(struct in_addr));
#ifdef  AF_INET6
    case AF_INET6:
        return bitcount((bit_t *)&Netmask.ipv6, sizeof(struct in6_addr));
#endif
    default:
        return 0;
    }
}

const cidr *cidr::find(const policy *policy, const struct sockaddr *s)
{
    assert(policy != NULL);
    assert(s != NULL);

    const cidr *member = NULL;
    unsigned top = 0;

    linked_pointer<const cidr> p = policy;
    while(p) {
        if(p->is_member(s)) {
            if(p->getMask() > top) {
                top = p->getMask();
                member = *p;
            }
        }
        p.next();
    }
    return member;
}

const cidr *cidr::container(const policy *policy, const struct sockaddr *s)
{
    assert(policy != NULL);
    assert(s != NULL);

    const cidr *member = NULL;
    unsigned top = 128;

    linked_pointer<const cidr> p = policy;
    while(p) {
        if(p->is_member(s)) {
            if(p->getMask() < top) {
                top = p->getMask();
                member = *p;
            }
        }
        p.next();
    }
    return member;
}


bool cidr::is_member(const struct sockaddr *s) const
{
    assert(s != NULL);

    inethostaddr_t host;
    struct sockaddr_internet *addr = (struct sockaddr_internet *)s;

    if(addr->address.sa_family != Family)
        return false;

    switch(Family) {
    case AF_INET:
        memcpy(&host.ipv4, &addr->ipv4.sin_addr, sizeof(host.ipv4));
        bitmask((bit_t *)&host.ipv4, (bit_t *)&Netmask, sizeof(host.ipv4));
        if(!memcmp(&host.ipv4, &Network.ipv4, sizeof(host.ipv4)))
            return true;
        return false;
#ifdef  AF_INET6
    case AF_INET6:
        memcpy(&host.ipv6, &addr->ipv6.sin6_addr, sizeof(host.ipv6));
        bitmask((bit_t *)&host.ipv6, (bit_t *)&Netmask, sizeof(host.ipv6));
        if(!memcmp(&host.ipv6, &Network.ipv6, sizeof(host.ipv6)))
            return true;
        return false;
#endif
    default:
        return false;
    }
}

inethostaddr_t cidr::broadcast(void) const
{
    inethostaddr_t bcast;

    switch(Family) {
    case AF_INET:
        memcpy(&bcast.ipv4, &Network.ipv4, sizeof(Network.ipv4));
        bitimask((bit_t *)&bcast.ipv4, (bit_t *)&Netmask.ipv4, sizeof(bcast.ipv4));
        return bcast;
#ifdef  AF_INET6
    case AF_INET6:
        memcpy(&bcast.ipv6, &Network.ipv6, sizeof(Network.ipv6));
        bitimask((bit_t *)&bcast.ipv6, (bit_t *)&Netmask.ipv6, sizeof(bcast.ipv6));
        return bcast;
#endif
    default:
        memset(&bcast, 0, sizeof(bcast));
        return  bcast;
    }
}

unsigned cidr::mask(const char *cp) const
{
    assert(cp != NULL && *cp != 0);

    unsigned count = 0, rcount = 0, dcount = 0;
    const char *sp = strchr(cp, '/');
    bool flag = false;
    const char *gp = cp;
    unsigned char dots[4];
    uint32_t mask;

    switch(Family) {
#ifdef  AF_INET6
    case AF_INET6:
        if(sp)
            return atoi(++sp);
        if(!strncmp(cp, "ff00:", 5))
            return 8;
        if(!strncmp(cp, "ff80:", 5))
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
                count += 16;
                while(*cp == '0')
                    ++cp;
                if(*cp == ':') {
                    if(!flag)
                        rcount = count;
                    flag = true;
                }
                else
                    flag = false;
            }
        }
        return rcount;
#endif
    case AF_INET:
        if(sp) {
            if(!strchr(++sp, '.'))
                return atoi(sp);
            mask = inet_addr(sp);
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
    default:
        return 0;
    }
}

void cidr::set(const char *cp)
{
    assert(cp != NULL && *cp != 0);

    char cbuf[128];
    char *ep;
    unsigned dots = 0;
#ifdef  _MSWINDOWS_
//  struct sockaddr saddr;
    int slen;
    struct sockaddr_in6 *paddr;
    DWORD addr = (DWORD)inet_addr(cbuf);
#endif

#ifdef  AF_INET6
    if(strchr(cp, ':'))
        Family = AF_INET6;
    else
#endif
        Family = AF_INET;

    switch(Family) {
    case AF_INET:
        memset(&Netmask.ipv4, 0, sizeof(Netmask.ipv4));
        bitset((bit_t *)&Netmask.ipv4, mask(cp));
        String::set(cbuf, sizeof(cbuf), cp);
        ep = (char *)strchr(cbuf, '/');
        if(ep)
            *ep = 0;

        cp = cbuf;
        while(NULL != (cp = strchr(cp, '.'))) {
            ++dots;
            ++cp;
        }

        while(dots++ < 3)
            String::add(cbuf, sizeof(cbuf), ".0");

#ifdef  _MSWINDOWS_
        memcpy(&Network.ipv4, &addr, sizeof(Network.ipv4));
#else
        inet_aton(cbuf, &Network.ipv4);
#endif
        bitmask((bit_t *)&Network.ipv4, (bit_t *)&Netmask.ipv4, sizeof(Network.ipv4));
        break;
#ifdef  AF_INET6
    case AF_INET6:
        memset(&Netmask.ipv6, 0, sizeof(Netmask));
        bitset((bit_t *)&Netmask.ipv6, mask(cp));
        String::set(cbuf, sizeof(cbuf), cp);
        ep = (char *)strchr(cp, '/');
        if(ep)
            *ep = 0;
#ifdef  _MSWINDOWS_
        struct sockaddr saddr;
        slen = sizeof(saddr);
        paddr = (struct sockaddr_in6 *)&saddr;
        WSAStringToAddress((LPSTR)cbuf, AF_INET6, NULL, &saddr, &slen);
        Network.ipv6 = paddr->sin6_addr;
#else
        inet_pton(AF_INET6, cbuf, &Network.ipv6);
#endif
        bitmask((bit_t *)&Network.ipv6, (bit_t *)&Netmask.ipv6, sizeof(Network.ipv6));
#endif
    default:
        break;
    }
}

Socket::address::address(int family, const char *a, int type, int protocol)
{
    assert(a != NULL && *a != 0);

    list = NULL;
#ifdef  _MSWINDOWS_
    Socket::init();
#endif
    set(family, a, type, protocol);
}

Socket::address::address(const char *host, unsigned port)
{
    assert(host != NULL && *host != 0);

    list = NULL;
#ifdef  _MSWINDOWS_
    Socket::init();
#endif
    set(host, port);
}

Socket::address::address(int family, const char *host, const char *svc)
{
    assert(host != NULL && *host != 0);
    assert(svc != NULL && *svc != 0);

    struct addrinfo hint;

#ifdef  _MSWINDOWS_
    Socket::init();
#endif

    list = NULL;
    memset(&hint, 0, sizeof(hint));
    hint.ai_family = family;
    getaddrinfo(host, svc, &hint, &list);
}

Socket::address::address()
{
    list = NULL;
}

Socket::address::address(const address& from)
{
    list = NULL;
    copy(from.list);
}

Socket::address::address(const char *host, const char *service, int type)
{
    list = NULL;
    set(host, service, type);
}

Socket::address::~address()
{
    clear();
}

void Socket::address::clear(void)
{
    if(list) {
        freeaddrinfo(list);
        list = NULL;
    }
}

void Socket::release(struct addrinfo *list)
{
    if(list)
        freeaddrinfo(list);
}

struct ::addrinfo *Socket::query(const char *hp, const char *svc, int type, int protocol)
{
    assert(hp != NULL && *hp != 0);

    int family = DEFAULT_FAMILY;
    char hostbuf[256];
    struct addrinfo hint;
    String::set(hostbuf, sizeof(hostbuf), hp);
    char *cp = strchr(hostbuf, '/');
    char *host = hostbuf;

    memset(&hint, 0, sizeof(hint));

    hint.ai_socktype = type;
    hint.ai_protocol = protocol;

#ifdef  PF_UNSPEC
    hint.ai_family = PF_UNSPEC;
    hint.ai_flags = AI_PASSIVE;
#endif

    if(cp)
        *cp = 0;

    if(*host == '[') {
        cp = strchr(++host, ']');
        if(cp) {
            *(cp++) = 0;
            if(*cp == ':')
                svc = ++cp;
        }
#ifdef  AF_INET6
        family = AF_INET6;
#else
        return NULL;
#endif
    }
    else if(((cp = strrchr(host, ':')) != NULL) && (strchr(host, ':') == cp)) {
        *(cp++) = 0;
        svc = cp;
    }

    if(is_numeric(host)) {
        hint.ai_flags |= AI_NUMERICHOST;

        if(strchr(host, ':')) {
#ifdef  AF_INET6
            family = AF_INET6;
#else
            return NULL;
#endif
        }
        else
            family = AF_INET;
    }

    if(family && family != AF_UNSPEC)
        hint.ai_family = family;

#if defined(AF_INET6) && defined(AI_V4MAPPED)
    if(hint.ai_family == AF_INET6 && !v6only)
        hint.ai_flags |= AI_V4MAPPED;
#endif
#ifdef  AI_NUMERICSERV
    if(svc && atoi(svc) > 0)
        hint.ai_flags |= AI_NUMERICSERV;
#endif

    struct addrinfo *result = NULL;
    getaddrinfo(host, svc, &hint, &result);
    return result;
}

void Socket::address::set(const char *host, unsigned port)
{
    assert(host != NULL && *host != 0);

    char buf[16];

    clear();

    if(port) {
        snprintf(buf, sizeof(buf), "%u", port);
        list = Socket::query(host, buf);
    }
    else
        list = Socket::query(host, NULL);
}

void Socket::address::set(int family, const char *a, int type, int protocol)
{
    assert(a != NULL && *a != 0);

    char *addr = strdup(a);
    char *host = strchr(addr, '@');
    char *ep;
    char *svc = NULL;
    struct addrinfo hint;

    clear();

    memset(&hint, 0, sizeof(hint));
#ifdef  PF_UNSPEC
    hint.ai_family = PF_UNSPEC;
    hint.ai_socktype = SOCK_STREAM;
    hint.ai_flags = AI_PASSIVE;
#endif

    if(!host)
        host = addr;
    else
        ++host;

    if(*host != '[') {
        ep = strchr(host, ':');
        if(ep) {
            *(ep++) = 0;
            svc = ep;
        }
        goto proc;
    }
#ifdef  AF_INET6
    if(*host == '[') {
        family = AF_INET6;
        ++host;
        ep = strchr(host, ']');
        if(ep) {
            *(ep++) = 0;
            if(*ep == ':')
                svc = ++ep;
        }
    }
#endif
proc:
    hint.ai_family = family;
    hint.ai_socktype = type;
    hint.ai_protocol = protocol;

#if defined(AF_INET6) && defined(AI_V4MAPPED)
    if(hint.ai_family == AF_INET6 && !v6only)
        hint.ai_flags |= AI_V4MAPPED;
#endif

    getaddrinfo(host, svc, &hint, &list);
}

struct sockaddr *Socket::address::get(void) const
{
    if(!list)
        return NULL;

    return list->ai_addr;
}

int Socket::address::family(void) const
{
    struct sockaddr *ap;
    if(!list)
        return 0;

    ap = list->ai_addr;
    if(!ap)
        return 0;

    return ap->sa_family;
}

struct sockaddr *Socket::address::get(int family) const
{
    struct sockaddr *ap;
    struct addrinfo *lp;

    lp = list;

    while(lp) {
        ap = lp->ai_addr;
        if(ap && ap->sa_family == family)
            return ap;
        lp = lp->ai_next;
    }
    return NULL;
}

void Socket::address::set(struct sockaddr *addr)
{
    clear();
    add(addr);
}

bool Socket::address::remove(struct sockaddr *addr)
{
    assert(addr != NULL);
    struct addrinfo *node = list, *prior = NULL;

    while(node) {
        if(node->ai_addr && equal(addr, node->ai_addr))
            break;
        prior = node;
        node = node->ai_next;
    }

    if(!node)
        return false;

    if(!prior)
        list = node->ai_next;
    else
        prior->ai_next = node->ai_next;

    node->ai_next = NULL;
    freeaddrinfo(node);
    return true;
}

unsigned Socket::address::insert(struct addrinfo *alist)
{
    unsigned count = 0;
    while(alist) {
        if(insert(alist->ai_addr))
            ++count;
        alist = alist->ai_next;
    }
    return count;
}

unsigned Socket::address::remove(struct addrinfo *alist)
{
    unsigned count = 0;
    while(alist) {
        if(remove(alist->ai_addr))
            ++count;
        alist = alist->ai_next;
    }
    return count;
}

bool Socket::address::insert(struct sockaddr *addr)
{
    assert(addr != NULL);

    struct addrinfo *node = list;

    while(node) {
        if(node->ai_addr && equal(addr, node->ai_addr))
            return false;
        node = node->ai_next;
    }

    node = (struct addrinfo *)malloc(sizeof(struct addrinfo));
    memset(node, 0, sizeof(struct addrinfo));
    node->ai_family = addr->sa_family;
    node->ai_addrlen = len(addr);
    node->ai_next = list;
    node->ai_addr = (struct sockaddr *)malloc(node->ai_addrlen);
    memcpy(node->ai_addr, addr, node->ai_addrlen);
    list = node;
    return true;
}

void Socket::address::copy(const struct addrinfo *addr)
{
    struct addrinfo *last = NULL;
    struct addrinfo *node;

    clear();
    while(addr) {
        node = (struct addrinfo *)malloc(sizeof(struct addrinfo));
        memcpy(node, addr, sizeof(struct addrinfo));
        node->ai_next = NULL;
        node->ai_addr = dup(addr->ai_addr);
        if(last)
            last->ai_next = node;
        else
            list = node;
        last = node;
    }
}

struct sockaddr_in *Socket::address::ipv4(struct sockaddr *addr)
{
    if(addr == NULL || addr->sa_family != AF_INET)
        return NULL;

    return (struct sockaddr_in*)addr;
}

#ifdef  AF_INET6
struct sockaddr_in6 *Socket::address::ipv6(struct sockaddr *addr)
{
    if(addr == NULL || addr->sa_family != AF_INET6)
        return NULL;

    return (struct sockaddr_in6*)addr;
}
#endif

struct sockaddr *Socket::address::dup(struct sockaddr *addr)
{
    struct sockaddr *node;

    if(!addr)
        return NULL;

    size_t slen = len(addr);
    if(!slen)
        return NULL;

    node = (struct sockaddr *)malloc(slen);
    memcpy(node, addr, slen);
    return node;
}

void Socket::address::add(struct sockaddr *addr)
{
    assert(addr != NULL);

    char buffer[80];
    char svc[8];

    Socket::query(addr, buffer, sizeof(buffer));
    snprintf(svc, sizeof(svc), "%d", Socket::service(addr));
    add(buffer, svc, addr->sa_family);
}

void Socket::address::set(const char *host, const char *svc, int socktype)
{
    clear();
    add(host, svc, socktype);
}

void Socket::address::add(const char *host, const char *svc, int socktype)
{
    assert(host != NULL && *host != 0);
    assert(svc != NULL && *svc != 0);

    struct addrinfo *join = NULL, *last = NULL;

    join = Socket::query(host, svc, socktype);
    if(!join)
        return;

    if(!list) {
        list = join;
        return;
    }
    last = list;
    while(last->ai_next)
        last = last->ai_next;
    last->ai_next = join;
}

struct sockaddr *Socket::address::find(const struct sockaddr *addr) const
{
    assert(addr != NULL);

    struct addrinfo *node = list;

    while(node) {
        if(equal(addr, node->ai_addr))
            return node->ai_addr;
        node = node->ai_next;
    }
    return NULL;
}

Socket::Socket(const Socket &s)
{
#ifdef  _MSWINDOWS_
    HANDLE pidH = GetCurrentProcess();
    HANDLE dupH;

    init();
    if(DuplicateHandle(pidH, reinterpret_cast<HANDLE>(s.so), pidH, &dupH, 0, FALSE, DUPLICATE_SAME_ACCESS))
        so = reinterpret_cast<SOCKET>(dupH);
    else
        so = INVALID_SOCKET;
#else
    so = ::dup(s.so);
#endif
    iowait = s.iowait;
    ioerr = 0;
}

Socket::Socket()
{
    so = INVALID_SOCKET;
    iowait = Timer::inf;
    ioerr = 0;
}

Socket::Socket(socket_t s)
{
    so = s;
    iowait = Timer::inf;
    ioerr = 0;
}

Socket::Socket(struct addrinfo *addr)
{
#ifdef  _MSWINDOWS_
    init();
#endif
    assert(addr != NULL);

    while(addr) {
        so = ::socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        socket_mapping(addr->ai_family, so);
        if(so != INVALID_SOCKET) {
            if(!_connect_(so, addr->ai_addr, addr->ai_addrlen))
                return;
        }
        addr = addr->ai_next;
    }
    so = INVALID_SOCKET;
    iowait = Timer::inf;
    ioerr = 0;
}

Socket::Socket(int family, int type, int protocol)
{
    so = create(family, type, protocol);
    iowait = Timer::inf;
    ioerr = 0;
}

Socket::Socket(const char *iface, const char *port, int family, int type, int protocol)
{
    assert(iface != NULL && *iface != 0);
    assert(port != NULL && *port != 0);

#ifdef  _MSWINDOWS_
    init();
#endif
    family = setfamily(family, iface);
    so = create(iface, port, family, type, protocol);
    iowait = Timer::inf;
    ioerr = 0;
}

socket_t Socket::create(const Socket::address &address)
{
    socket_t so;
    struct addrinfo *res = *address;
    if(!res)
        return INVALID_SOCKET;

    so = create(res->ai_family, res->ai_socktype, res->ai_protocol);
    if(so == INVALID_SOCKET)
        return INVALID_SOCKET;

    if(connectto(so, res)) {
        release(so);
        return INVALID_SOCKET;
    }
    return so;
}

socket_t Socket::create(const char *iface, const char *port, int family, int type, int protocol)
{
    assert(iface != NULL && *iface != 0);
    assert(port != NULL && *port != 0);

    struct addrinfo hint, *res;
    socket_t so;
    int reuse = 1;

#ifdef  _MSWINDOWS_
    Socket::init();
#endif

    memset(&hint, 0, sizeof(hint));
    hint.ai_flags = AI_PASSIVE | AI_NUMERICHOST;
    hint.ai_family = setfamily(family, iface);
    hint.ai_socktype = type;
    hint.ai_protocol = protocol;

#if defined(AF_INET6) && defined(AI_V4MAPPED)
    if(hint.ai_family == AF_INET6 && !v6only)
        hint.ai_flags |= AI_V4MAPPED;
#endif

#if defined(AF_UNIX) && !defined(_MSWINDOWS_)
    if(strchr(iface, '/')) {
        struct sockaddr_un uaddr;
        socklen_t len = unixaddr(&uaddr, iface);
        if(!type)
            type = SOCK_STREAM;
        so = create(AF_UNIX, type, 0);
        if(so == INVALID_SOCKET)
            return INVALID_SOCKET;
        if(_bind_(so, (struct sockaddr *)&uaddr, len)) {
            release(so);
            return INVALID_SOCKET;
        }
        return so;
    };
#endif

    if(iface && !strcmp(iface, "*"))
        iface = NULL;

    getaddrinfo(iface, port, &hint, &res);
    if(res == NULL)
        return INVALID_SOCKET;

    so = create(res->ai_family, res->ai_socktype, res->ai_protocol);
    if(so == INVALID_SOCKET) {
        freeaddrinfo(res);
        return INVALID_SOCKET;
    }
    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (caddr_t)&reuse, sizeof(reuse));
    if(res->ai_addr) {
        if(_bind_(so, res->ai_addr, res->ai_addrlen)) {
            release(so);
            so = INVALID_SOCKET;
        }
    }
    freeaddrinfo(res);
    return so;
}

Socket::~Socket()
{
    release();
}

socket_t Socket::create(int family, int type, int protocol)
{
    socket_t so;
#ifdef  _MSWINDOWS_
    init();
#endif
    so = ::socket(family, type, protocol);
    socket_mapping(family, so);
    return so;
}

void Socket::cancel(void)
{
    if(so != INVALID_SOCKET)
        ::shutdown(so, SHUT_RDWR);
}

void Socket::cancel(socket_t so)
{
    if(so != INVALID_SOCKET)
        ::shutdown(so, SHUT_RDWR);
}

void Socket::release(socket_t so)
{
#ifdef  _MSWINDOWS_
    ::closesocket(so);
#else
    if(!::shutdown(so, SHUT_RDWR))
        ::close(so);
#endif
}

void Socket::release(void)
{
    if(so != INVALID_SOCKET) {
#ifdef  _MSWINDOWS_
        ::closesocket(so);
#else
        ::shutdown(so, SHUT_RDWR);
        ::close(so);
#endif
        so = INVALID_SOCKET;
    }
    iowait = Timer::inf;
    ioerr = 0;
}

#ifdef  _MSWINDOWS_
int Socket::error(void)
{
    switch(WSAGetLastError())
    {
    case WSANOTINITIALISED:
    case WSAENETDOWN:
    case WSASYSNOTREADY:
        return ENETDOWN;
    case WSAEFAULT:
        return EFAULT;
    case WSAEINTR:
    case WSAECANCELLED:
    case WSA_OPERATION_ABORTED:
    case WSA_IO_INCOMPLETE:
    case WSASYSCALLFAILURE:
    case WSA_E_CANCELLED:
        return EINTR;
    case WSA_IO_PENDING:
    case WSAEINPROGRESS:
        return EINPROGRESS;
    case WSAEINVAL:
        return EINVAL;
    case WSAEMFILE:
        return EMFILE;
    case WSAENETUNREACH:
        return ENETUNREACH;
    case WSAENOBUFS:
    case WSAETOOMANYREFS:
    case WSA_NOT_ENOUGH_MEMORY:
        return ENOMEM;
    case WSAEACCES:
        return EACCES;
    case WSAEBADF:
    case WSAENOTSOCK:
    case WSA_INVALID_HANDLE:
        return EBADF;
    case WSAEOPNOTSUPP:
        return ENOSYS;
    case WSAEWOULDBLOCK:
    case WSAEALREADY:
        return EAGAIN;
    case WSAENOPROTOOPT:
        return ENOPROTOOPT;
    case WSAEADDRINUSE:
        return EADDRINUSE;
    case WSAENETRESET:
        return ENETRESET;
    case WSAECONNABORTED:
        return ECONNABORTED;
    case WSAECONNRESET:
        return ECONNRESET;
    case WSAEISCONN:
        return EISCONN;
    case WSAENOTCONN:
        return ENOTCONN;
    case WSAESHUTDOWN:
        return ESHUTDOWN;
    case WSAETIMEDOUT:
        return ETIMEDOUT;
    case WSAECONNREFUSED:
        return ECONNREFUSED;
    case WSAEHOSTDOWN:
        return EHOSTDOWN;
    case WSAEHOSTUNREACH:
        return EHOSTUNREACH;
    }
    return EINVAL;
}
#else
int Socket::error(void)
{
    return errno;
}
#endif

Socket::operator bool()
{
    if(so == INVALID_SOCKET)
        return false;
    return true;
}

bool Socket::operator!() const
{
    if(so == INVALID_SOCKET)
        return true;
    return false;
}

Socket &Socket::operator=(socket_t s)
{
    release();
    so = s;
    return *this;
}

size_t Socket::peek(void *data, size_t len) const
{
    assert(data != NULL);
    assert(len > 0);

    if(iowait && iowait != Timer::inf && !Socket::wait(so, iowait))
        return 0;

    ssize_t rtn = _recv_(so, (caddr_t)data, 1, MSG_DONTWAIT | MSG_PEEK);
    if(rtn < 1)
        return 0;
    return (size_t)rtn;
}

ssize_t Socket::recvinet(socket_t so, void *data, size_t len, int flags, struct sockaddr_internet *addr)
{
    assert(data != NULL);
    assert(len > 0);

    socklen_t slen = sizeof(struct sockaddr_internet);
    return _recvfrom_(so, (caddr_t)data, len, flags, (struct sockaddr *)addr, &slen);
}

ssize_t Socket::recvfrom(socket_t so, void *data, size_t len, int flags, struct sockaddr_storage *addr)
{
    assert(data != NULL);
    assert(len > 0);

    socklen_t slen = sizeof(struct sockaddr_storage);
    return _recvfrom_(so, (caddr_t)data, len, flags, (struct sockaddr *)addr, &slen);
}

size_t Socket::readfrom(void *data, size_t len, struct sockaddr_storage *from)
{
    assert(data != NULL);
    assert(len > 0);

    // wait for input by timer if possible...
    if(iowait && iowait != Timer::inf && !Socket::wait(so, iowait))
        return 0;

    socklen_t slen = sizeof(struct sockaddr_storage);
    ssize_t result = _recvfrom_(so, (caddr_t)data, len, 0, (struct sockaddr *)from, &slen);

    if(result < 0) {
        ioerr = Socket::error();
        return 0;
    }
    return (size_t)result;
}

size_t Socket::writeto(const void *data, size_t dlen, const struct sockaddr *dest)
{
    assert(data != NULL);
    assert(len > 0);

    socklen_t slen = 0;
    if(dest)
        slen = len(dest);

    ssize_t result = _sendto_(so, (caddr_t)data, dlen, MSG_NOSIGNAL, dest, slen);
    if(result < 0) {
        ioerr = Socket::error();
        return 0;
    }
    return (size_t)result;
}

ssize_t Socket::sendto(socket_t so, const void *data, size_t dlen, int flags, const struct sockaddr *dest)
{
    assert(data != NULL);
    assert(len > 0);

    socklen_t slen = 0;
    if(dest)
        slen = len(dest);

    return _sendto_(so, (caddr_t)data, dlen, MSG_NOSIGNAL | flags, dest, slen);
}

size_t Socket::writes(const char *str)
{
    if(!str)
        return 0;

    if(!*str)
        return 0;

    return writeto(str, strlen(str), NULL);
}

size_t Socket::readline(char *data, size_t max)
{
    assert(data != NULL);
    assert(max > 0);

    *data = 0;

    ssize_t result = Socket::readline(so, data, max, iowait);
    if(result < 0) {
        ioerr = Socket::error();
        return 0;
    }
    return (size_t)result;
}

size_t Socket::readline(String& s)
{
    if(!s.c_mem())
        return 0;

    ssize_t result = Socket::readline(so, s.c_mem(), s.size() + 1, iowait);
    if(result < 0) {
        ioerr = Socket::error();
        s.clear();
        return 0;
    }
    String::fix(s);
    return (size_t)result;
}

ssize_t Socket::readline(socket_t so, char *data, size_t max, timeout_t timeout)
{
    assert(data != NULL);
    assert(max > 0);

    bool crlf = false;
    bool nl = false;
    int nleft = max - 1;        // leave space for null byte
    int nstat, c;

    if(max < 1)
        return -1;

    data[0] = 0;
    while(nleft && !nl) {
        if(timeout) {
            if(!wait(so, timeout))
                return 0;
        }
        nstat = _recv_(so, data, nleft, MSG_PEEK);
        if(nstat < 0)
            return -1;

        if(nstat == 0)
            return max - nleft - 1;

        for(c = 0; c < nstat; ++c) {
            if(data[c] == '\n') {
                if(c > 0 && data[c - 1] == '\r')
                    crlf = true;
                ++c;
                nl = true;
                break;
            }
        }

        nstat = _recv_(so, (caddr_t)data, c, 0);
        if(nstat < 0)
            break;

        if(crlf) {
            --nstat;
            data[nstat - 1] = '\n';
        }

        data += nstat;
        nleft -= nstat;
    }

    if(nl)
        --data;

    *data = 0;
    return ssize_t(max - nleft - 1);
}

int Socket::loopback(socket_t so, bool enable)
{
    union {
        struct sockaddr_storage saddr;
        struct sockaddr_in inaddr;
    } us;

    struct sockaddr *addr = (struct sockaddr *)&us.saddr;
    int family;
    socklen_t len = sizeof(us.saddr);
    int opt = 0;

    if(enable)
        opt = 1;

    if(so == INVALID_SOCKET)
        return EBADF;

    _getsockname_(so, addr, &len);
    family = us.inaddr.sin_family;
    switch(family) {
    case AF_INET:
        if(!setsockopt(so, IPPROTO_IP, IP_MULTICAST_LOOP, (char *)&opt, sizeof(opt)))
            return 0;
        break;
#if defined(AF_INET6) && defined(IPROTO_IPV6)
    case AF_INET6:
        if(!setsockopt(so, IPPROTO_IPV6, IPV6_MULTICAST_LOOP, (char *)&opt, sizeof(opt)))
            return 0;
#endif
    }
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
}

int Socket::ttl(socket_t so, unsigned char t)
{
    union {
        struct sockaddr_storage saddr;
        struct sockaddr_in inaddr;
    } us;

    struct sockaddr *addr = (struct sockaddr *)&us.saddr;
    int family;
    socklen_t len = sizeof(us.saddr);

    if(so == INVALID_SOCKET)
        return EBADF;

    _getsockname_(so, addr, &len);
    family = us.inaddr.sin_family;
    switch(family) {
    case AF_INET:
        if(!setsockopt(so, IPPROTO_IP, IP_TTL, (char *)&t, sizeof(t)))
            return 0;
        break;
#if defined(AF_INET6) && defined(IPPROTO_IPV6)
    case AF_INET6:
        if(!setsockopt(so, IPPROTO_IPV6, IPV6_UNICAST_HOPS, (char *)&t, sizeof(t)))
            return 0;
#endif
    }
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
}

int Socket::priority(socket_t so, int pri)
{
    if(so == INVALID_SOCKET)
        return EBADF;
#ifdef  SO_PRIORITY
    if(!setsockopt(so, SOL_SOCKET, SO_PRIORITY, (char *)&pri, (socklen_t)sizeof(pri)))
        return 0;
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
#else
    return ENOSYS;
#endif
}

int Socket::tos(socket_t so, int ts)
{
    if(so == INVALID_SOCKET)
        return EBADF;

#ifdef  SOL_IP
    if(!setsockopt(so, SOL_IP, IP_TOS,(char *)&ts, (socklen_t)sizeof(ts)))
        return 0;

    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
#else
    return ENOSYS;
#endif
}

int Socket::broadcast(socket_t so, bool enable)
{
    if(so == INVALID_SOCKET)
        return EBADF;
    int opt = (enable ? 1 : 0);
    if(!::setsockopt(so, SOL_SOCKET, SO_BROADCAST,
              (char *)&opt, (socklen_t)sizeof(opt)))
        return 0;

    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
}

int Socket::nodelay(socket_t so)
{
    if(so == INVALID_SOCKET)
        return EBADF;
#if defined(TCP_NODELAY)
    int opt = 1;
    if(!::setsockopt(so, IPPROTO_TCP, TCP_NODELAY,
            (char *)&opt, (socklen_t)sizeof(opt)))
        return 0;
#else
    return ENOSYS;
#endif
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
}

int Socket::keepalive(socket_t so, bool enable)
{
    if(so == INVALID_SOCKET)
        return EBADF;
#if defined(SO_KEEPALIVE) || defined(_MSWINDOWS_)
    int opt = (enable ? ~0 : 0);
    if(!::setsockopt(so, SOL_SOCKET, SO_KEEPALIVE, (char *)&opt, (socklen_t)sizeof(opt)))
        return 0;
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
#else
    return ENOSYS;
#endif
}

int Socket::multicast(socket_t so, unsigned ttl)
{
    struct sockaddr_internet addr;
    socklen_t len = sizeof(addr);
    bool enable;
    int rtn;

    if(so == INVALID_SOCKET)
        return EBADF;

    if(ttl)
        enable = true;
    else
        enable = false;

    _getsockname_(so, (struct sockaddr *)&addr, &len);
    if(!enable)
        switch(addr.address.sa_family)
        {
        case AF_INET:
            memset(&addr.ipv4.sin_addr, 0, sizeof(addr.ipv4.sin_addr));
            break;
#ifdef  AF_INET6
        case AF_INET6:
            memset(&addr.ipv6.sin6_addr, 0, sizeof(addr.ipv6.sin6_addr));
            break;
#endif
        default:
            break;
        }
    switch(addr.address.sa_family) {
#if defined(AF_INET6) && defined(IPPROTO_IPV6)
    case AF_INET6:
        rtn = ::setsockopt(so, IPPROTO_IPV6, IPV6_MULTICAST_IF, (char *)&addr.ipv6.sin6_addr, sizeof(addr.ipv6.sin6_addr));
        if(!rtn)
            rtn = ::setsockopt(so, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, (char *)&ttl, sizeof(ttl));
        if(rtn) {
            rtn = Socket::error();
            if(!rtn)
                rtn = EIO;
        }
        return rtn;
#endif
    case AF_INET:
#ifdef  IP_MULTICAST_IF
        rtn = ::setsockopt(so, IPPROTO_IP, IP_MULTICAST_IF, (char *)&addr.ipv4.sin_addr, sizeof(addr.ipv4.sin_addr));
        if(!rtn)
            rtn = ::setsockopt(so, IPPROTO_IP, IP_MULTICAST_TTL, (char *)&ttl, sizeof(ttl));
        if(rtn) {
            rtn = Socket::error();
            if(!rtn)
                rtn = EIO;
        }
        return rtn;
#else
        return ENOSYS;
#endif
    default:
        return ENOSYS;
    }
}

int Socket::join(const struct addrinfo *addr)
{
    int rtn = Socket::join(so, addr);
    if(rtn)
        ioerr = rtn;
    return rtn;
}

int Socket::drop(const struct addrinfo *addr)
{
    int rtn = Socket::drop(so, addr);
    if(rtn)
        ioerr = rtn;
    return rtn;
}

int Socket::wait(timeout_t timeout)
{
    bool mode = true;

    if(timeout < Timer::inf)
        mode = false;

    int rtn = Socket::blocking(so, mode);
    if(!rtn)
        iowait = timeout;
    else
        ioerr = rtn;

    return rtn;
}

int Socket::blocking(socket_t so, bool enable)
{
    if(so == INVALID_SOCKET)
        return EBADF;

#if defined(_MSWINDOWS_)
    unsigned long flag = (enable ? 0 : 1);
    if(!ioctlsocket(so, FIONBIO, &flag))
        return 0;

#else
    long flags = fcntl(so, F_GETFL);
    if(enable)
        flags &=~ O_NONBLOCK;
    else
        flags |= O_NONBLOCK;
    if(!fcntl(so, F_SETFL, flags))
        return 0;
#endif
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
}

int Socket::disconnect(socket_t so)
{
    union {
        struct sockaddr_storage saddr;
        struct sockaddr_in inaddr;
    } us;

    struct sockaddr *addr = (struct sockaddr *)&us.saddr;
    socklen_t len = sizeof(us.saddr);

#if defined(_MSWINDOWS_)
    _getsockname_(so, addr, &len);
    int family = us.inaddr.sin_family;
    memset(addr, 0, sizeof(us.saddr));
    us.inaddr.sin_family = family;
#else
    memset(addr, 0, sizeof(us.saddr));
    us.inaddr.sin_family = AF_UNSPEC;
#endif
    if((size_t)len > sizeof(us.saddr))
        len = sizeof(us.saddr);
    if(so == INVALID_SOCKET)
        return EBADF;
    if(!_connect_(so, addr, len))
        return 0;
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
}

int Socket::join(socket_t so, const struct addrinfo *node)
{
    assert(node != NULL);

    struct multicast_internet mcast;
    struct sockaddr_internet addr;
    socklen_t len = sizeof(addr);
    const struct sockaddr_internet *target;
    int family;
    int rtn = 0;

    if(so == INVALID_SOCKET)
        return EBADF;

    _getsockname_(so, (struct sockaddr *)&addr, &len);
    while(!rtn && node && node->ai_addr) {
        target = (const struct sockaddr_internet *)node->ai_addr;
        family = node->ai_family;
        node = node->ai_next;
        if(family != addr.address.sa_family)
            continue;
        switch(addr.address.sa_family) {
#if defined(AF_INET6) && defined(IPV6_ADD_MEMBERSHIP) && defined(IPPROTO_IPV6)
        case AF_INET6:
            mcast.ipv6.ipv6mr_interface = 0;
            memcpy(&mcast.ipv6.ipv6mr_multiaddr, &target->ipv6.sin6_addr, sizeof(target->ipv6.sin6_addr));
            rtn = ::setsockopt(so, IPPROTO_IPV6, IPV6_ADD_MEMBERSHIP, (char *)&mcast, sizeof(mcast.ipv6));
            break;
#endif
#if defined(IP_ADD_MEMBERSHIP)
        case AF_INET:
            mcast.ipv4.imr_interface.s_addr = INADDR_ANY;
            memcpy(&mcast.ipv4.imr_multiaddr, &target->ipv4.sin_addr, sizeof(target->ipv4.sin_addr));
            rtn = ::setsockopt(so, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char *)&mcast, sizeof(mcast.ipv4));
            break;
#endif
        default:
            return ENOSYS;
        }
    }
    if(rtn) {
        rtn = Socket::error();
        if(!rtn)
            rtn = EIO;
    }
    return rtn;
}

int Socket::drop(socket_t so, const struct addrinfo *node)
{
    assert(node != NULL);

    struct multicast_internet mcast;
    struct sockaddr_internet addr;
    socklen_t len = sizeof(addr);
    const struct sockaddr_internet *target;
    int family;
    int rtn = 0;

    if(so == INVALID_SOCKET)
        return EBADF;

    _getsockname_(so, (struct sockaddr *)&addr, &len);
    while(!rtn && node && node->ai_addr) {
        target = (const struct sockaddr_internet *)node->ai_addr;
        family = node->ai_family;
        node = node->ai_next;

        if(family != addr.address.sa_family)
            continue;

        switch(addr.address.sa_family) {
#if defined(AF_INET6) && defined(IPV6_DROP_MEMBERSHIP) && defined(IPPROTO_IPV6)
        case AF_INET6:
            mcast.ipv6.ipv6mr_interface = 0;
            memcpy(&mcast.ipv6.ipv6mr_multiaddr, &target->ipv6.sin6_addr, sizeof(target->ipv6.sin6_addr));
            rtn = ::setsockopt(so, IPPROTO_IPV6, IPV6_DROP_MEMBERSHIP, (char *)&mcast, sizeof(mcast.ipv6));
            break;
#endif
#if defined(IP_DROP_MEMBERSHIP)
        case AF_INET:
            mcast.ipv4.imr_interface.s_addr = INADDR_ANY;
            memcpy(&mcast.ipv4.imr_multiaddr, &target->ipv4.sin_addr, sizeof(target->ipv4.sin_addr));
            rtn = ::setsockopt(so, IPPROTO_IP, IP_DROP_MEMBERSHIP, (char *)&mcast, sizeof(mcast.ipv4));
            break;
#endif
        default:
            return ENOSYS;
        }
    }
    if(rtn) {
        rtn = Socket::error();
        if(!rtn)
            rtn = EIO;
    }
    return rtn;
}

socket_t Socket::create(const struct addrinfo *node, int stype, int sprotocol)
{
    assert(node != NULL);

    socket_t so = INVALID_SOCKET;
    int sfamily = AF_UNSPEC;
    int cprotocol, ctype;

    while(node) {
        if(stype && node->ai_socktype && node->ai_socktype != stype)
            goto next;

        if(sprotocol && node->ai_protocol && node->ai_protocol != sprotocol)
            goto next;

        if(node->ai_family != sfamily) {
            if(so != INVALID_SOCKET)
                Socket::release(so);
            sfamily = node->ai_family;
            if(stype)
                ctype = stype;
            else
                ctype = node->ai_socktype;
            if(sprotocol)
                cprotocol = sprotocol;
            else
                cprotocol = node->ai_protocol;
            so = Socket::create(sfamily, ctype, cprotocol);
        }
        if(so != INVALID_SOCKET) {
            if(!_connect_(so, node->ai_addr, node->ai_addrlen))
                return so;
        }
next:
        node = node->ai_next;
    }
    if(so != INVALID_SOCKET)
        Socket::release(so);
    return INVALID_SOCKET;
}

int Socket::connectto(struct addrinfo *node)
{
    return (ioerr = connectto(so, node));
}

int Socket::disconnect(void)
{
    return (ioerr = disconnect(so));
}

int Socket::connectto(socket_t so, struct addrinfo *node)
{
    assert(node != NULL);

    int rtn = -1;
    int socket_family;

    if(so == INVALID_SOCKET)
        return EBADF;

    socket_family = family(so);

    while(node) {
        if(node->ai_family == socket_family) {
            if(!_connect_(so, node->ai_addr, node->ai_addrlen)) {
                rtn = 0;
                goto exit;
            }
        }
        node = node->ai_next;
    }

exit:
#ifndef _MSWINDOWS_
    if(!rtn || errno == EINPROGRESS)
        return 0;
#endif
    if(rtn) {
        rtn = Socket::error();
        if(!rtn)
            rtn = EIO;
    }
    return rtn;
}

int Socket::error(socket_t so)
{
    assert(so != INVALID_SOCKET);

    int opt;
    socklen_t slen = sizeof(opt);

    if(getsockopt(so, SOL_SOCKET, SO_ERROR, (caddr_t)&opt, &slen))
        return ENOSYS;

    return opt;
}

int Socket::sendwait(socket_t so, unsigned size)
{
    assert(so != INVALID_SOCKET);

#ifdef  SO_SNDLOWAT
    if(!setsockopt(so, SOL_SOCKET, SO_SNDLOWAT, (caddr_t)&size, sizeof(size)))
        return 0;
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
#else
    return ENOSYS;
#endif
}

int Socket::sendsize(socket_t so, unsigned size)
{
    assert(so != INVALID_SOCKET);

#ifdef  SO_SNDBUF
    if(!setsockopt(so, SOL_SOCKET, SO_SNDBUF, (caddr_t)&size, sizeof(size)))
        return 0;
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
#else
    return ENOSYS;
#endif
}

int Socket::recvsize(socket_t so, unsigned size)
{
#ifdef  SO_RCVBUF
    if(!setsockopt(so, SOL_SOCKET, SO_RCVBUF, (caddr_t)&size, sizeof(size)))
        return 0;
    int err = Socket::error();
    if(!err)
        err = EIO;
    return err;
#else
    return ENOSYS;
#endif
}

bool Socket::connected(void) const
{
    char buf;

    if(so == INVALID_SOCKET)
        return false;

    if(!wait())
        return true;

    if(_recv_(so, &buf, 1, MSG_DONTWAIT | MSG_PEEK) < 1)
        return false;

    return true;
}

bool Socket::is_pending(unsigned qio) const
{
    if(pending() >= qio)
        return true;

    return false;
}

#ifdef _MSWINDOWS_
unsigned Socket::pending(socket_t so)
{
    u_long opt;
    if(so == INVALID_SOCKET)
        return 0;

    ioctlsocket(so, FIONREAD, &opt);
    return (unsigned)opt;
}

#else
unsigned Socket::pending(socket_t so)
{
    int opt;
    if(so == INVALID_SOCKET)
        return 0;

    if(::ioctl(so, FIONREAD, &opt))
        return 0;
    return (unsigned)opt;
}

#endif

socket_t Socket::acceptfrom(socket_t so, struct sockaddr_storage *addr)
{
    socklen_t len = sizeof(struct sockaddr_storage);
    if(addr)
        return _accept_(so, (struct sockaddr *)addr, &len);
    else
        return _accept_(so, NULL, NULL);
}

bool Socket::wait(timeout_t timeout) const
{
    return wait(so, timeout);
}

bool Socket::wait(socket_t so, timeout_t timeout)
{
    int status;

#ifdef  USE_POLL
    struct pollfd pfd;

    pfd.fd = so;
    pfd.revents = 0;
    pfd.events = POLLIN;

    if(so == INVALID_SOCKET)
        return false;

    status = 0;
    while(status < 1) {
        if(timeout == Timer::inf)
            status = _poll_(&pfd, 1, -1);
        else
            status = _poll_(&pfd, 1, timeout);
        if(status == -1 && errno == EINTR)
            continue;
        if(status < 0)
            return false;
    }
    if(pfd.revents & POLLIN)
        return true;
    return false;
#else
    struct timeval tv;
    struct timeval *tvp = &tv;
    fd_set grp;

    if(so == INVALID_SOCKET)
        return false;

    if(timeout == Timer::inf)
        tvp = NULL;
    else {
        tv.tv_usec = (timeout % 1000) * 1000;
        tv.tv_sec = timeout / 1000;
    }

    FD_ZERO(&grp);
    FD_SET(so, &grp);
    status = _select_((int)(so + 1), &grp, NULL, NULL, tvp);
    if(status < 1)
        return false;
    if(FD_ISSET(so, &grp))
        return true;
    return false;
#endif
}

bool Socket::waitSending(timeout_t timeout) const
{
    int status;
#ifdef  USE_POLL
    struct pollfd pfd;

    pfd.fd = so;
    pfd.revents = 0;
    pfd.events = POLLOUT;

    if(so == INVALID_SOCKET)
        return false;

    status = 0;
    while(status < 1) {
        if(timeout == Timer::inf)
            status = _poll_(&pfd, 1, -1);
        else
            status = _poll_(&pfd, 1, timeout);
        if(status == -1 && errno == EINTR)
            continue;
        if(status < 0)
            return false;
    }
    if(pfd.revents & POLLOUT)
        return true;
    return false;
#else
    struct timeval tv;
    struct timeval *tvp = &tv;
    fd_set grp;

    if(so == INVALID_SOCKET)
        return false;

    if(timeout == Timer::inf)
        tvp = NULL;
    else {
        tv.tv_usec = (timeout % 1000) * 1000;
        tv.tv_sec = timeout / 1000;
    }

    FD_ZERO(&grp);
    FD_SET(so, &grp);
    status = _select_((int)(so + 1), NULL, &grp, NULL, tvp);
    if(status < 1)
        return false;
    if(FD_ISSET(so, &grp))
        return true;
    return false;
#endif
}

ListenSocket::ListenSocket(const char *iface, const char *svc, unsigned backlog, int family, int type, int protocol) :
Socket()
{
    if(!iface)
        iface = "*";

    assert(iface != NULL && *iface != 0);
    assert(svc != NULL && *svc != 0);
    assert(backlog > 0);

    so = create(iface, svc, backlog, family, type, protocol);
}

socket_t ListenSocket::create(const char *iface, const char *svc, unsigned backlog, int family, int type, int protocol)
{
    if(!type)
        type = SOCK_STREAM;

    socket_t so = Socket::create(iface, svc, family, type, protocol);

    if(so == INVALID_SOCKET)
        return so;

    if(_listen_(so, backlog)) {
        release(so);
        return INVALID_SOCKET;
    }
    return so;
}

socket_t ListenSocket::accept(struct sockaddr_storage *addr) const
{
    socklen_t len = sizeof(struct sockaddr_storage);
    if(addr)
        return _accept_(so, (struct sockaddr *)addr, &len);
    else
        return _accept_(so, NULL, NULL);
}

TCPServer::TCPServer(const char *address, const char *service, unsigned backlog) :
ListenSocket(address, service, backlog)
{
}

#ifdef  _MSWINDOWS_
#undef  AF_UNIX
#endif

struct ::addrinfo *Socket::hinting(socket_t so, struct addrinfo *hint)
{
    assert(hint != NULL);

    union {
        struct sockaddr_storage st;
        struct sockaddr_in in;
    } us;
    struct sockaddr *sa = (struct sockaddr *)&us.st;
    socklen_t slen = sizeof(us.st);

    memset(hint, 0, sizeof(struct addrinfo));
    memset(sa, 0, sizeof(us.st));
    if(_getsockname_(so, sa, &slen))
        return NULL;
    hint->ai_family = us.in.sin_family;
    slen = sizeof(hint->ai_socktype);
    getsockopt(so, SOL_SOCKET, SO_TYPE, (caddr_t)&hint->ai_socktype, &slen);
    return hint;
}

int Socket::type(socket_t so)
{
    int sotype;
    socklen_t slen = sizeof(sotype);
    if(getsockopt(so, SOL_SOCKET, SO_TYPE, (caddr_t)&sotype, &slen))
        return 0;
    return sotype;
}

bool Socket::ccid(socket_t so, uint8_t ccid)
{
    uint8_t ccids[4];
    socklen_t len = sizeof(ccids);
    bool supported = false;

    // maybe also not dccp socket...
    if(getsockopt(so, SOL_DCCP, DCCP_SOCKOPT_AVAILABLE_CCIDS, (char *)&ccids, &len) < 0)
        return false;

    for(unsigned pos = 0; pos < sizeof(ccids); ++pos) {
        if(ccid == ccids[pos]) {
            supported = true;
            break;
        }
    }

    if(!supported)
        return false;

    if(setsockopt(so, SOL_DCCP, DCCP_SOCKOPT_CCID, (char *)&ccid, sizeof(ccid)) < 0)
        return false;

    return true;
}

unsigned Socket::segsize(socket_t so, unsigned size)
{
#ifdef  IP_MTU
    socklen_t alen = sizeof(size);
#endif

    switch(type(so)) {
    case SOCK_STREAM:
#ifdef  TCP_MAXSEG
        if(size)
            setsockopt(so, IPPROTO_TCP, TCP_MAXSEG, (char *)&size, sizeof(size));
#endif
        break;
    case SOCK_DCCP:
#ifdef  DCCP_MAXSEG
        if(size)
            setsockopt(so, IPPROTO_DCCP, DCCP_MAXSEG, (char *)&size, sizeof(size));
#endif
        break;
    }
#ifdef  IP_MTU
    getsockopt(so, IPPROTO_IP, IP_MTU, &size, &alen);
#else
    size = 0;
#endif
    return size;
}

char *Socket::hostname(const struct sockaddr *sa, char *buf, size_t max)
{
    assert(sa != NULL);
    assert(buf != NULL);
    assert(max > 0);

    socklen_t sl;

#ifdef  AF_UNIX
    const struct sockaddr_un *un = (const struct sockaddr_un *)sa;
#endif

    switch(sa->sa_family) {
#ifdef  AF_UNIX
    case AF_UNIX:
        if(max > sizeof(un->sun_path))
            max = sizeof(un->sun_path);
        else
            --max;
        strncpy(buf, un->sun_path, max);
        buf[max] = 0;
        return buf;
#endif
    case AF_INET:
        sl = sizeof(struct sockaddr_in);
        break;
#ifdef  AF_INET6
    case AF_INET6:
        sl = sizeof(struct sockaddr_in6);
        break;
#endif
    default:
        return NULL;
    }

    if(getnameinfo(sa, sl, buf, max, NULL, 0, NI_NOFQDN))
        return NULL;

    return buf;
}

socklen_t Socket::query(socket_t so, struct sockaddr_storage *sa, const char *host, const char *svc)
{
    assert(sa != NULL);
    assert(host != NULL && *host != 0);
    assert(svc != NULL && *svc != 0);

    socklen_t len = 0;
    struct addrinfo hint, *res = NULL;

#ifdef  AF_UNIX
    if(strchr(host, '/'))
        return unixaddr((struct sockaddr_un *)sa, host);
#endif

    if(!hinting(so, &hint) || !svc)
        return 0;

    if(getaddrinfo(host, svc, &hint, &res) || !res)
        goto exit;

    memcpy(sa, res->ai_addr, res->ai_addrlen);
    len = res->ai_addrlen;

exit:
    if(res)
        freeaddrinfo(res);
    return len;
}

int Socket::bindto(socket_t so, const struct sockaddr *iface)
{
    if(!_bind_(so, iface, len(iface)))
        return 0;
    return Socket::error();
}

int Socket::listento(socket_t so, const struct sockaddr *iface, int backlog)
{
    if(_bind_(so, iface, len(iface)))
        return Socket::error();
    if(_listen_(so, backlog))
        return Socket::error();
    return 0;
}

int Socket::bindto(socket_t so, const char *host, const char *svc, int protocol)
{
    assert(so != INVALID_SOCKET);
    assert(host != NULL && *host != 0);
    assert(svc != NULL && *svc != 0);

    int rtn = -1;
    int reuse = 1;

    struct addrinfo hint, *res = NULL;

    setsockopt(so, SOL_SOCKET, SO_REUSEADDR, (caddr_t)&reuse, sizeof(reuse));

#ifdef AF_UNIX
    if(strchr(host, '/')) {
        struct sockaddr_un uaddr;
        socklen_t len = unixaddr(&uaddr, host);
        rtn = _bind_(so, (struct sockaddr *)&uaddr, len);
        goto exit;
    };
#endif

    if(!hinting(so, &hint) || !svc)
        return ENOSYS;

    hint.ai_protocol = protocol;
    if(host && !strcmp(host, "*"))
        host = NULL;

#if defined(SO_BINDTODEVICE) && !defined(__QNX__)
    if(host && !strchr(host, '.') && !strchr(host, ':')) {
        struct ifreq ifr;
        memset(&ifr, 0, sizeof(ifr));
        strncpy(ifr.ifr_ifrn.ifrn_name, host, IFNAMSIZ);
        setsockopt(so, SOL_SOCKET, SO_BINDTODEVICE, &ifr, sizeof(ifr));
        host = NULL;
    }
#endif

    hint.ai_flags = AI_PASSIVE | AI_NUMERICHOST;

#if defined(AF_INET6) && defined(AI_V4MAPPED)
    if(hint.ai_family == AF_INET6 && !v6only)
        hint.ai_flags |= AI_V4MAPPED;
#endif

    rtn = getaddrinfo(host, svc, &hint, &res);
    if(rtn)
        goto exit;

    rtn = _bind_(so, res->ai_addr, res->ai_addrlen);
exit:
    if(res)
        freeaddrinfo(res);
    if(rtn)
        rtn = Socket::error();
    return rtn;
}

unsigned Socket::keyhost(const struct sockaddr *addr, unsigned keysize)
{
    assert(addr != NULL);
    assert(keysize > 0);

    unsigned key = 0;
    caddr_t cp = NULL;
    unsigned len;
switch(addr->sa_family) {
#ifdef  AF_INET6
    case AF_INET6:
        cp = (caddr_t)(&((const struct sockaddr_in6 *)(addr))->sin6_addr);
        len = 16;
        break;
#endif
    case AF_INET:
        cp = (caddr_t)(&((const struct sockaddr_in *)(addr))->sin_addr);
        len = 4;
        break;
    default:
        return 0;
    }
    while(len--) {
        key = key << 1;
        key ^= cp[len];
    }
    return key % keysize;
}

unsigned Socket::keyindex(const struct sockaddr *addr, unsigned keysize)
{
    assert(addr != NULL);
    assert(keysize > 0);

    unsigned key = 0;
    caddr_t cp = NULL;
    unsigned len;
switch(addr->sa_family) {
#ifdef  AF_INET6
    case AF_INET6:
        cp = (caddr_t)(&((const struct sockaddr_in6 *)(addr))->sin6_addr);
        len = 16;
        key = service(addr);
        break;
#endif
    case AF_INET:
        cp = (caddr_t)(&((const struct sockaddr_in *)(addr))->sin_addr);
        len = 4;
        key = service(addr);
        break;
    default:
        return 0;
    }
    while(len--) {
        key = key << 1;
        key ^= cp[len];
    }
    return key % keysize;
}

short Socket::service(const struct sockaddr *addr)
{
    assert(addr != NULL);

    switch(addr->sa_family) {
#ifdef  AF_INET6
    case AF_INET6:
        return ntohs(((const struct sockaddr_in6 *)(addr))->sin6_port);
#endif
    case AF_INET:
        return ntohs(((const struct sockaddr_in *)(addr))->sin_port);
    }
    return 0;
}

char *Socket::query(const struct sockaddr *addr, char *name, socklen_t size)
{
    assert(addr != NULL);
    assert(name != NULL);

#ifdef  _MSWINDOWS_
    DWORD slen = size;
#endif

    *name = 0;
    if(!addr)
        return NULL;

    switch(addr->sa_family) {
#ifdef  AF_UNIX
    case AF_UNIX:
        String::set(name, size, ((const struct sockaddr_un *)(addr))->sun_path);
        return name;
#endif
#ifdef  _MSWINDOWS_
#ifdef  AF_INET6
    case AF_INET6:
        struct sockaddr_in6 saddr6;
        memcpy(&saddr6, addr, sizeof(saddr6));
        saddr6.sin6_port = 0;
        WSAAddressToString((struct sockaddr *)&saddr6, sizeof(saddr6), NULL, name, &slen);
        return name;
#endif
    case AF_INET:
        struct sockaddr_in saddr;
        memcpy(&saddr, addr, sizeof(saddr));
        saddr.sin_port = 0;
        WSAAddressToString((struct sockaddr *)&saddr, sizeof(saddr), NULL, name, &slen);
        return name;
#else
#ifdef  HAVE_INET_NTOP
#ifdef  AF_INET6
    case AF_INET6:
        inet_ntop(addr->sa_family, &((const struct sockaddr_in6 *)(addr))->sin6_addr, name, size);
        return name;
#endif
    case AF_INET:
        inet_ntop(addr->sa_family, &((const struct sockaddr_in *)(addr))->sin_addr, name, size);
        return name;
#else
    case AF_INET:
        ENTER_EXCLUSIVE
        String::set(name, size, inet_ntoa(((const struct sockaddr_in *)(addr))->sin_addr));
        LEAVE_EXCLUSIVE
        return name;
#endif
#endif
    }
    return NULL;
}

int Socket::via(struct sockaddr *iface, const struct sockaddr *dest)
{
    assert(iface != NULL);
    assert(dest != NULL);

    int rtn = -1;
    socket_t so = INVALID_SOCKET;
    socklen_t slen = len(dest);

    if(slen)
        memset(iface, 0, slen);

    iface->sa_family = AF_UNSPEC;
    switch(dest->sa_family) {
#ifdef  AF_INET6
    case AF_INET6:
#endif
    case AF_INET:
        so = ::socket(dest->sa_family, SOCK_DGRAM, 0);
        if((socket_t)so == INVALID_SOCKET)
            return -1;
        socket_mapping(dest->sa_family, so);
        if(!_connect_(so, dest, slen))
            rtn = _getsockname_(so, iface, &slen);
        break;
    default:
        return ENOSYS;
    }
    switch(iface->sa_family) {
    case AF_INET:
        ((struct sockaddr_in*)(iface))->sin_port = 0;
        break;
#ifdef  AF_INET6
    case AF_INET6:
        ((struct sockaddr_in6*)(iface))->sin6_port = 0;
        break;
#endif
    }

    if((socket_t)so != INVALID_SOCKET) {
#ifdef  _MSWINDOWS_
        ::closesocket(so);
#else
        ::shutdown(so, SHUT_RDWR);
        ::close(so);
#endif
        so = INVALID_SOCKET;
    }
    if(rtn)
        rtn = Socket::error();
    return rtn;
}

bool Socket::eq_subnet(const struct sockaddr *s1, const struct sockaddr *s2)
{
    assert(s1 != NULL && s2 != NULL);

    unsigned char *a1, *a2;
    if(s1->sa_family != s2->sa_family)
        return false;

    if(s1->sa_family != AF_INET)
        return true;

    a1 = (unsigned char *)&(((const struct sockaddr_in *)(s1))->sin_addr);
    a2 = (unsigned char *)&(((const struct sockaddr_in *)(s1))->sin_addr);

    if(*a1 == *a2 && *a1 < 128)
        return true;

    if(*a1 != *a2)
        return false;

    if(*a1 > 127 && *a1 < 192 && a1[1] == a2[1])
        return true;

    if(a1[1] != a2[1])
        return false;

    if(a1[2] != a2[2])
        return false;

    return true;
}

unsigned Socket::store(struct sockaddr_internet *storage, const struct sockaddr *address)
{
    if(storage == NULL || address == NULL)
        return 0;

    if(address->sa_family == AF_INET) {
        memcpy(&storage->ipv4, address, sizeof(storage->ipv4));
        return sizeof(storage->ipv4);
    }

#ifdef  AF_INET6
    if(address->sa_family == AF_INET6) {
        memcpy(&storage->ipv6, address, sizeof(storage->ipv6));
        return sizeof(storage->ipv6);
    }
#endif

    return 0;
}

unsigned Socket::copy(struct sockaddr *s1, const struct sockaddr *s2)
{
    if(s1 == NULL || s2 == NULL)
        return 0;

    socklen_t slen = len(s1);
    if(len > 0) {
        memcpy(s1, s2, slen);
        return slen;
    }
    return 0;
}

bool Socket::eq_host(const struct sockaddr *s1, const struct sockaddr *s2)
{
    assert(s1 != NULL && s2 != NULL);

    if(s1->sa_family != s2->sa_family)
        return false;

    switch(s1->sa_family) {
    case AF_INET:
        if(memcmp(&(((const struct sockaddr_in *)s1)->sin_addr),
            &(((const struct sockaddr_in *)s2)->sin_addr), 4))
                return false;

        return true;
#ifdef  AF_INET6
    case AF_INET6:
        if(memcmp(&(((const struct sockaddr_in6 *)s1)->sin6_addr),
            &(((const struct sockaddr_in6 *)s2)->sin6_addr), 4))
                return false;

        return true;
#endif
    default:
        if(memcmp(s1, s2, len(s1)))
            return false;
        return true;
    }
    return false;
}


bool Socket::equal(const struct sockaddr *s1, const struct sockaddr *s2)
{
    assert(s1 != NULL && s2 != NULL);

    if(s1->sa_family != s2->sa_family)
        return false;

    switch(s1->sa_family) {
    case AF_INET:
        if(memcmp(&(((const struct sockaddr_in *)s1)->sin_addr),
            &(((const struct sockaddr_in *)s2)->sin_addr), 4))
                return false;

        if(!((const struct sockaddr_in *)s1)->sin_port || !((const struct sockaddr_in *)s2)->sin_port)
            return true;

        if(((const struct sockaddr_in *)s1)->sin_port != ((const struct sockaddr_in *)s2)->sin_port)
            return false;

        return true;
#ifdef  AF_INET6
    case AF_INET6:
        if(memcmp(&(((const struct sockaddr_in6 *)s1)->sin6_addr),
            &(((const struct sockaddr_in6 *)s2)->sin6_addr), 4))
                return false;

        if(!((const struct sockaddr_in6 *)s1)->sin6_port || !((const struct sockaddr_in6 *)s2)->sin6_port)
            return true;

        if(((const struct sockaddr_in6 *)s1)->sin6_port != ((const struct sockaddr_in6 *)s2)->sin6_port)
            return false;

        return true;
#endif
    default:
        if(memcmp(s1, s2, len(s1)))
            return false;
        return true;
    }
    return false;
}

size_t Socket::printf(const char *format, ...)
{
    assert(format != NULL);

    char buf[1024];
    va_list args;

    va_start(args, format);
    vsnprintf(buf, sizeof(buf), format, args);
    va_end(args);

    return writes(buf);
}

ssize_t Socket::printf(socket_t so, const char *format, ...)
{
    assert(format != NULL);

    char buf[536];
    va_list args;

    va_start(args, format);
    vsnprintf(buf, sizeof(buf), format, args);
    va_end(args);

    return sendto(so, buf, strlen(buf), 0, NULL);
}

socklen_t Socket::len(const struct sockaddr *sa)
{
    if(!sa)
        return 0;

    switch(sa->sa_family)
    {
    case AF_INET:
        return sizeof(sockaddr_in);
#ifdef  AF_INET6
    case AF_INET6:
        return sizeof(sockaddr_in6);
#endif
    default:
        return sizeof(sockaddr_storage);
    }
}

int Socket::family(socket_t so)
{
    assert(so != INVALID_SOCKET);

    union {
        struct sockaddr_storage saddr;
        struct sockaddr_in inaddr;
    } us;

    socklen_t len = sizeof(us.saddr);
    struct sockaddr *addr = (struct sockaddr *)(&us.saddr);

    if(_getsockname_(so, addr, &len))
        return AF_UNSPEC;

    return us.inaddr.sin_family;
}

bool Socket::is_null(const char *str)
{
    assert(str != NULL);

    while(*str && strchr("0:.*", *str) != NULL)
        ++str;

    // allow field separation...
    if(*str <= ' ')
        return true;

    if(*str)
        return false;

    return true;
}

bool Socket::is_numeric(const char *str)
{
    assert(str != NULL);

    // if raw ipv6, then we can just exit, no chance to confuse with names
    if(strchr(str, ':'))
        return true;

    while(*str && strchr("0123456789.", *str) != NULL)
        ++str;

    // allow field separators
    if(*str <= ' ')
        return true;

    if(*str)
        return false;

    return true;
}

int Socket::local(socket_t sock, struct sockaddr_storage *addr)
{
    socklen_t slen = sizeof(sockaddr_storage);
    return _getsockname_(sock, (struct sockaddr *)addr, &slen);
}

int Socket::remote(socket_t sock, struct sockaddr_storage *addr)
{
    socklen_t slen = sizeof(sockaddr_storage);
    return _getpeername_(sock, (struct sockaddr *)addr, &slen);
}

String str(Socket& so, strsize_t size)
{
    String s(size);
    so.readline(s.c_mem(), s.size());
    String::fix(s);
    return s;
}

NAMESPACE_UCOMMON

struct sockaddr *_getaddrinfo(struct addrinfo *list)
{
    return list->ai_addr;
}

struct addrinfo *_nextaddrinfo(struct addrinfo *list)
{
    if(!list)
        return NULL;

    return list->ai_next;
}

socket_t _getaddrsock(struct addrinfo *list)
{
    if(!list)
        return INVALID_SOCKET;

    return ::socket(list->ai_family, list->ai_socktype, list->ai_protocol);
}

END_NAMESPACE

