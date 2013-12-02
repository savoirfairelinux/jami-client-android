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
#include <cc++/thread.h>
#include <cc++/address.h>
#include <cc++/socket.h>
#include <cc++/export.h>
#include <cc++/socketport.h>
#include "private.h"
#ifndef WIN32
#include <cerrno>
#define socket_errno errno
#else
#define socket_errno WSAGetLastError()
#endif

#ifndef INADDR_LOOPBACK
#define INADDR_LOOPBACK (unsigned long)0x7f000001
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

SocketPort::SocketPort(SocketService *svc, TCPSocket &tcp) :
Socket(accept(tcp.getSocket(), NULL, NULL))
{
    detect_pending = true;
    detect_output = false;
    detect_disconnect = true;

#ifdef WIN32
    // FIXME: error handling
    event = CreateEvent(NULL,TRUE,FALSE,NULL);
#endif
    next = prev = NULL;
    service = NULL;

    // FIXME: use macro here and in other files...
#ifndef WIN32
    if(so > -1)
#else
    if(so != INVALID_SOCKET)
#endif
    {
        setError(false);
    if( svc )
        svc->attach(this);
    }
}

#ifdef  CCXX_IPV6
SocketPort::SocketPort(SocketService *svc, TCPV6Socket &tcp) :
Socket(accept(tcp.getSocket(), NULL, NULL))
{
    detect_pending = true;
    detect_output = false;
    detect_disconnect = true;

#ifdef WIN32
    // FIXME: error handling
    event = CreateEvent(NULL,TRUE,FALSE,NULL);
#endif
    next = prev = NULL;
    service = NULL;

    // FIXME: use macro here and in other files...
#ifndef WIN32
    if(so > -1)
#else
    if(so != INVALID_SOCKET)
#endif
    {
        setError(false);
    if( svc )
        svc->attach(this);
    }
}
#endif

SocketPort::SocketPort(SocketService *svc, const IPV4Address &ia, tpport_t port) :
Socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
{
#ifdef WIN32
    // FIXME: error handling
    event = CreateEvent(NULL,TRUE,FALSE,NULL);
#endif
    struct sockaddr_in addr;

    memset(&addr, 0, sizeof(addr));
    next = prev = NULL;
    service = NULL;
    addr.sin_family = AF_INET;
    addr.sin_addr = getaddress(ia);
    addr.sin_port = htons(port);
    detect_pending = true;
    detect_output = false;
    detect_disconnect = true;

    if(bind(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }
    state = BOUND;
    setError(false);

  if(svc)
    svc->attach(this);
}

#ifdef  CCXX_IPV6
SocketPort::SocketPort(SocketService *svc, const IPV6Address &ia, tpport_t port) :
Socket(AF_INET6, SOCK_DGRAM, IPPROTO_UDP)
{
#ifdef WIN32
    // FIXME: error handling
    event = CreateEvent(NULL,TRUE,FALSE,NULL);
#endif
    struct sockaddr_in6 addr;

    memset(&addr, 0, sizeof(addr));
    next = prev = NULL;
    service = NULL;
    addr.sin6_family = AF_INET6;
    addr.sin6_addr = getaddress(ia);
    addr.sin6_port = htons(port);
    detect_pending = true;
    detect_output = false;
    detect_disconnect = true;

    if(bind(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr))) {
        endSocket();
        error(errBindingFailed,(char *)"Could not bind socket",socket_errno);
        return;
    }
    state = BOUND;
    setError(false);

  if(svc)
    svc->attach(this);
}
#endif

SocketPort::SocketPort(SocketService *svc, const IPV4Host &ih, tpport_t port) :
Socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
{
#ifdef WIN32
    // FIXME: error handling
    event = CreateEvent(NULL,TRUE,FALSE,NULL);
#endif
    struct sockaddr_in addr;

    memset(&addr, 0, sizeof(addr));
    next = prev = NULL;
    service = NULL;
    addr.sin_family = AF_INET;
    addr.sin_addr = getaddress(ih);
    addr.sin_port = htons(port);
    detect_pending = true;
    detect_disconnect = true;

#ifndef WIN32
    long opts = fcntl(so, F_GETFL);
    fcntl(so, F_SETFL, opts | O_NDELAY);
#else
    u_long opts = 1;
    ioctlsocket(so,FIONBIO,&opts);
#endif

    int rtn = ::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr));

    if(!rtn) {
        state = CONNECTED;
    }
    else {
#ifndef WIN32
        if(errno == EINPROGRESS)
#else
        if(WSAGetLastError() == WSAEINPROGRESS || WSAGetLastError() == WSAEWOULDBLOCK)
#endif
        {
            state = CONNECTING;
        }
        else {
            endSocket();
            connectError();
            return;
        }
    }

#ifndef WIN32
    fcntl(so, F_SETFL, opts);
#else
    opts = 0;
    ioctlsocket(so,FIONBIO,&opts);
#endif

    setError(false);
    detect_output = (state == CONNECTING);

    if(svc)
        svc->attach(this);

//  if(state == CONNECTING)
//      setDetectOutput(true);
}

#ifdef  CCXX_IPV6
SocketPort::SocketPort(SocketService *svc, const IPV6Host &ih, tpport_t port) :
Socket(AF_INET6, SOCK_STREAM, IPPROTO_TCP)
{
#ifdef WIN32
    // FIXME: error handling
    event = CreateEvent(NULL,TRUE,FALSE,NULL);
#endif
    struct sockaddr_in6 addr;

    memset(&addr, 0, sizeof(addr));
    next = prev = NULL;
    service = NULL;
    addr.sin6_family = AF_INET6;
    addr.sin6_addr = getaddress(ih);
    addr.sin6_port = htons(port);
    detect_pending = true;
    detect_disconnect = true;

#ifndef WIN32
    long opts = fcntl(so, F_GETFL);
    fcntl(so, F_SETFL, opts | O_NDELAY);
#else
    u_long opts = 1;
    ioctlsocket(so,FIONBIO,&opts);
#endif

    int rtn = ::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr));

    if(!rtn) {
        state = CONNECTED;
    }
    else {
#ifndef WIN32
        if(errno == EINPROGRESS)
#else
        if(WSAGetLastError() == WSAEINPROGRESS || WSAGetLastError() == WSAEWOULDBLOCK)
#endif
        {
            state = CONNECTING;
        }
        else {
            endSocket();
            connectError();
            return;
        }
    }

#ifndef WIN32
    fcntl(so, F_SETFL, opts);
#else
    opts = 0;
    ioctlsocket(so,FIONBIO,&opts);
#endif

    setError(false);
    detect_output = (state == CONNECTING);

    if(svc)
        svc->attach(this);

//  if(state == CONNECTING)
//      setDetectOutput(true);
}
#endif

SocketPort::~SocketPort()
{
#ifdef WIN32
    CloseHandle(event);
#endif
    if(service) {
        service->detach(this);
    }
    endSocket();
}

void SocketPort::expired(void)
{}

void SocketPort::pending(void)
{}

void SocketPort::output(void)
{}

void SocketPort::disconnect(void)
{}

void SocketPort::attach( SocketService* svc )
{
    if(service)
        service->detach(this);
    service = svc;
    if(svc)
        svc->attach(this);
}

Socket::Error SocketPort::connect(const IPV4Address &ia, tpport_t port)
{
    struct sockaddr_in addr;
    Error rtn = errSuccess;

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr = getaddress(ia);
    addr.sin_port = htons(port);

#ifndef WIN32
    long opts = fcntl(so, F_GETFL);
    fcntl(so, F_SETFL, opts | O_NDELAY);
#else
    u_long opts = 1;
    ioctlsocket(so,FIONBIO,&opts);
#endif
    // Win32 will crash if you try to connect to INADDR_ANY.
    if ( INADDR_ANY == addr.sin_addr.s_addr )
            addr.sin_addr.s_addr = INADDR_LOOPBACK;
    if(::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr)))
        rtn = connectError();
#ifndef WIN32
    fcntl(so, F_SETFL, opts);
#else
    opts = 0;
    ioctlsocket(so,FIONBIO,&opts);
#endif
    return rtn;
}

#ifdef  CCXX_IPV6
Socket::Error SocketPort::connect(const IPV6Address &ia, tpport_t port)
{
    struct sockaddr_in6 addr;
    Error rtn = errSuccess;

    memset(&addr, 0, sizeof(addr));
    addr.sin6_family = AF_INET6;
    addr.sin6_addr = getaddress(ia);
    addr.sin6_port = htons(port);

#ifndef WIN32
    long opts = fcntl(so, F_GETFL);
    fcntl(so, F_SETFL, opts | O_NDELAY);
#else
    u_long opts = 1;
    ioctlsocket(so,FIONBIO,&opts);
#endif
    // Win32 will crash if you try to connect to INADDR_ANY.
    if(!memcmp(&addr.sin6_addr, &in6addr_any, sizeof(in6addr_any)))
        memcpy(&addr.sin6_addr, &in6addr_loopback, sizeof(in6addr_loopback));
    if(::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr)))
        rtn = connectError();
#ifndef WIN32
    fcntl(so, F_SETFL, opts);
#else
    opts = 0;
    ioctlsocket(so,FIONBIO,&opts);
#endif
    return rtn;
}
#endif

void SocketPort::setTimer(timeout_t ptimer)
{
    TimerPort::setTimer(ptimer);
    if( service )
        service->update();
}

void SocketPort::incTimer(timeout_t ptimer)
{
    TimerPort::incTimer(ptimer);
    if( service )
        service->update();
}

void SocketPort::setDetectPending( bool val )
{
    if ( detect_pending != val ) {
        detect_pending = val;
#ifdef USE_POLL
        if ( ufd ) {
            if ( val ) {
                ufd->events |= POLLIN;
            } else {
                ufd->events &= ~POLLIN;
            }
        }
#endif
    if( service )
        service->update();
    }
}

void SocketPort::setDetectOutput( bool val )
{
    if ( detect_output != val ) {
        detect_output = val;
#ifdef USE_POLL
        if ( ufd ) {
            if ( val ) {
                ufd->events |= POLLOUT;
            } else {
                ufd->events &= ~POLLOUT;
            }
        }
#endif
    if( service )
        service->update();
    }
}

#ifdef WIN32
class SocketService::Sync
{
public:
    /* FIXME: error handling */
    Sync() :
    sync(CreateEvent(NULL,TRUE,FALSE,NULL)),
    semWrite(CreateSemaphore(NULL,1,1,NULL)),
    flag(-1)
    {}

    ~Sync() {
        CloseHandle(sync);
        CloseHandle(semWrite);
    }

    HANDLE GetSync() const {
        return sync;
    }

    void update(unsigned char flag) {
        // FIXME: cancellation
        WaitForSingleObject(semWrite,INFINITE);
        this->flag = flag;
        SetEvent(sync);
    }

    int getFlag() {
        int res = flag;
        flag = -1;
        if (res > 0) {
            ReleaseSemaphore(semWrite,1,NULL);
            ResetEvent(sync);
        }
        return res;
    }
private:
    HANDLE sync;
    HANDLE semWrite;
    int flag;
};
#endif

SocketService::SocketService(int pri, size_t stack, const char *id) :
Thread(pri, stack), Mutex(id)
{
    first = last = NULL;
    count = 0;
#ifndef WIN32
    FD_ZERO(&connect);
    long opt;
    if(::pipe(iosync)) {
#ifdef  CCXX_EXCEPTIONS
        switch(Thread::getException()) {
        case throwObject:
            throw(this);
            return;
#ifdef  COMMON_STD_EXCEPTION
        case throwException:
            throw(ThrException("no service pipe"));
            return;
#endif
        default:
            return;
        }
#else
        return;
#endif
    }
    hiwater = iosync[0] + 1;

#ifndef USE_POLL
    FD_SET(iosync[0], &connect);
#endif
    opt = fcntl(iosync[0], F_GETFL);
    fcntl(iosync[0], F_SETFL, opt | O_NDELAY);
#else
    sync = new Sync();
#endif
}

SocketService::~SocketService()
{
    update(0);

#ifdef WIN32
    // FIXME: thread is finished ???
    delete sync;
#endif

    terminate();

    while(first)
        delete first;
}

void SocketService::onUpdate(unsigned char buf)
{}

void SocketService::onEvent(void)
{}

void SocketService::onCallback(SocketPort *port)
{}

void SocketService::attach(SocketPort *port)
{
    enterMutex();
#ifdef  USE_POLL
    port->ufd = 0;
#endif
    if(last)
        last->next = port;

    port->prev = last;
    last = port;
#ifndef WIN32
#ifndef USE_POLL
    FD_SET(port->so, &connect);
#endif
    if(port->so >= hiwater)
        hiwater = port->so + 1;
#endif
    port->service = this;

    ++count;
    if(!first) first = port;

    // start thread if necessary
    if (count == 1) {
        if (!isRunning()) {
            leaveMutex();
            start();
            return;
        }
    }
    leaveMutex();
    update();
}

void SocketService::detach(SocketPort *port)
{
    enterMutex();
#if !defined(USE_POLL) && !defined(WIN32)
    FD_CLR(port->so, &connect);
#endif
    if(port->prev) {
        port->prev->next = port->next;
    } else {
        first = port->next;
    }

    if(port->next) {
        port->next->prev = port->prev;
    } else {
        last = port->prev;
    }
    port->service = NULL;

    --count;
    leaveMutex();
    update();
}

void SocketService::update(unsigned char flag)
{
#ifndef WIN32
    if(::write(iosync[1], (char *)&flag, 1) < 1) {

#ifdef  CCXX_EXCEPTIONS
        switch(Thread::getException()) {
        case throwObject:
            throw(this);
            return;
#ifdef  COMMON_STD_EXCEPTION
        case throwException:
            throw(ThrException("update failed"));
            return;
#endif
        default:
            return;
        }
#else
        return;
#endif
    }

#else
    sync->update(flag);
#endif
}

#define MUTEX_START { MutexLock _lock_(*this);
#define MUTEX_END }

void SocketService::run(void)
{
    timeout_t timer, expires;
    SocketPort *port;
    unsigned char buf;

#ifndef WIN32
#ifdef  USE_POLL

    Poller            mfd;
    pollfd          * p_ufd;
    int               lastcount = 0;

    // initialize ufd in all attached ports :
    // probably don't need this but it can't hurt.
    enterMutex();
    port = first;
    while(port) {
        port->ufd = 0;
        port = port->next;
    }
    leaveMutex();

#else
    struct timeval timeout, *tvp;
    fd_set inp, out, err;
    FD_ZERO(&inp);
    FD_ZERO(&out);
    FD_ZERO(&err);
    int so;
#endif
#else // WIN32
    int numHandle = 0;
    HANDLE hv[MAXIMUM_WAIT_OBJECTS];
#endif


#ifdef WIN32
    // FIXME: needed ?
    ResetEvent(sync->GetSync());
#endif

    setCancel(cancelDeferred);
    for(;;) {
        timer = TIMEOUT_INF;
#ifndef WIN32
        while(1 == ::read(iosync[0], (char *)&buf, 1)) {
#else
        for(;;) {
            int f = sync->getFlag();
            if (f < 0)
                break;

            buf = f;
#endif
            if(buf) {
                onUpdate(buf);
                continue;
            }

            setCancel(cancelImmediate);
            sleep(TIMEOUT_INF);
            exit();
        }

#ifndef WIN32
#ifdef  USE_POLL

        bool    reallocate = false;

        MUTEX_START
        onEvent();
        port = first;
        while(port) {
            onCallback(port);
            if ( ( p_ufd = port->ufd ) ) {

                if ( ( POLLHUP | POLLNVAL ) & p_ufd->revents ) {
                    // Avoid infinite loop from disconnected sockets
                    port->detect_disconnect = false;
                    p_ufd->events &= ~POLLHUP;

                    SocketPort* p = port;
                    port = port->next;
                    detach(p);
                    reallocate = true;
                    p->disconnect();
                    continue;
                }

                if ( ( POLLIN | POLLPRI ) & p_ufd->revents )
                    port->pending();

                if ( POLLOUT & p_ufd->revents )
                    port->output();

            } else {
                reallocate = true;
            }

retry:
            expires = port->getTimer();

            if(expires > 0)
                if(expires < timer)
                    timer = expires;

            if(!expires) {
                port->endTimer();
                port->expired();
                goto retry;
            }

            port = port->next;
        }

        //
        // reallocate things if we saw a ServerPort without
        // ufd set !
        if ( reallocate || ( ( count + 1 ) != lastcount ) ) {
            lastcount = count + 1;
            p_ufd = mfd.getList( count + 1 );

            // Set up iosync polling
            p_ufd->fd = iosync[0];
            p_ufd->events = POLLIN | POLLHUP;
            p_ufd ++;

            port = first;
            while(port) {
                p_ufd->fd = port->so;
                p_ufd->events =
                    ( port->detect_disconnect ? POLLHUP : 0 )
                    | ( port->detect_output ? POLLOUT : 0 )
                    | ( port->detect_pending ? POLLIN : 0 )
                ;
                port->ufd = p_ufd;
                p_ufd ++;
                port = port->next;
            }
        }
        MUTEX_END
        poll( mfd.getList(), lastcount, timer );

#else
        MUTEX_START
        onEvent();
        port = first;
        while(port) {
            onCallback(port);
            so = port->so;
            if(FD_ISSET(so, &err)) {
                port->detect_disconnect = false;

                SocketPort* p = port;
                port = port->next;
                p->disconnect();
                continue;
            }

            if(FD_ISSET(so, &inp))
                port->pending();

            if(FD_ISSET(so, &out))
                port->output();

retry:
            expires = port->getTimer();
            if(expires > 0)
                if(expires < timer)
                    timer = expires;

            // if we expire, get new scheduling now

            if(!expires) {
                port->endTimer();
                port->expired();
                goto retry;
            }

            port = port->next;
        }

        FD_ZERO(&inp);
        FD_ZERO(&out);
        FD_ZERO(&err);
        FD_SET(iosync[0],&inp);
        port = first;
        while(port) {
            so = port->so;
            if(port->detect_pending)
                FD_SET(so, &inp);

            if(port->detect_output)
                FD_SET(so, &out);

            if(port->detect_disconnect)
                FD_SET(so, &err);

            port = port->next;
        }

        MUTEX_END
        if(timer == TIMEOUT_INF)
            tvp = NULL;
        else {
            tvp = &timeout;
            timeout.tv_sec = timer / 1000;
            timeout.tv_usec = (timer % 1000) * 1000;
        }
        select(hiwater, &inp, &out, &err, tvp);
#endif
#else // WIN32
        MUTEX_START
        onEvent();

        hv[0] = sync->GetSync();
        numHandle = 1;
        port = first;
        while(port) {
            onCallback(port);

            long events = 0;

            if(port->detect_pending)
                events |= FD_READ;

            if(port->detect_output)
                events |= FD_WRITE;

            if(port->detect_disconnect)
                events |= FD_CLOSE;

            // !!! ignore some socket on overflow !!!
            if (events && numHandle < MAXIMUM_WAIT_OBJECTS) {
                WSAEventSelect(port->so,port->event,events);
                hv[numHandle++] = port->event;
            }

retry:
            expires = port->getTimer();
            if(expires > 0)
                if(expires < timer)
                    timer = expires;

            // if we expire, get new scheduling now

            if(!expires) {
                port->endTimer();
                port->expired();
                goto retry;
            }

            port = port->next;
        }

        MUTEX_END

        // FIXME: handle thread cancellation correctly
        DWORD res = WaitForMultipleObjects(numHandle,hv,FALSE,timer);
        switch (res) {
        case WAIT_OBJECT_0:
            break;
        case WAIT_TIMEOUT:
            break;
        default:
            // FIXME: handle failures (detach SocketPort)
            if (res >= WAIT_OBJECT_0+1 && res <= WAIT_OBJECT_0+MAXIMUM_WAIT_OBJECTS) {
                int curr = res - (WAIT_OBJECT_0);
                WSANETWORKEVENTS events;

                // search port
                MUTEX_START
                port = first;
                while(port) {
                    if (port->event == hv[curr])
                        break;
                    port = port->next;
                }
                MUTEX_END

                // if port not found ignore
                if (!port || port->event != hv[curr])
                    break;

                WSAEnumNetworkEvents(port->so,port->event,&events);

                if(events.lNetworkEvents & FD_CLOSE) {
                    port->detect_disconnect = false;
                    port->disconnect();
                    continue;
                }

                if(events.lNetworkEvents & FD_READ)
                    port->pending();

                if(events.lNetworkEvents & FD_WRITE)
                    port->output();
            }
        }
#endif
    }
}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
