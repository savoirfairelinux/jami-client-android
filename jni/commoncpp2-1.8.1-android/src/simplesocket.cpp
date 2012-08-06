// Copyright (C) 2002-2010 Wizzer Works.
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
 * @file   simplesocket.cpp
 * @brief  Implementation of SimpleTCPStream methods.
 *
 * Implementation of SimpleTCPStream methods.
 *
 * @author Mark S. Millard (msm@wizzer.com)
 * @date   2002-08-15
 */

// Include Common C++ Library header files.
#include <cc++/config.h>
#include <cc++/export.h>
#include <cc++/socket.h>

#ifndef INADDR_LOOPBACK
#define INADDR_LOOPBACK (unsigned long)0x7f000001
#endif

#ifdef CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

SimpleTCPStream::SimpleTCPStream(TCPSocket &server, size_t size) :
Socket(accept(server.getSocket(), NULL, NULL))
{
    tpport_t port;
    IPV4Host host = getPeer(&port);

    if (! server.onAccept(host, port)) {
        endSocket();
        error(errConnectRejected);
        return;
    }

    Socket::state = CONNECTED;
}

SimpleTCPStream::SimpleTCPStream(const IPV4Host &host, tpport_t port, size_t size) :
Socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
{
    Connect(host, port, size);
}

SimpleTCPStream::~SimpleTCPStream()
{
    endStream();
}

IPV4Host SimpleTCPStream::getSender(tpport_t *port) const
{
    return IPV4Host();
}

void SimpleTCPStream::Connect(const IPV4Host &host, tpport_t port, size_t size)
{
    size_t i;

    for (i = 0 ; i < host.getAddressCount(); i++) {
        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr = host.getAddress(i);
        addr.sin_port = htons(port);

        // Win32 will crash if you try to connect to INADDR_ANY.
        if ( INADDR_ANY == addr.sin_addr.s_addr )
            addr.sin_addr.s_addr = INADDR_LOOPBACK;
        if (::connect(so, (struct sockaddr *)&addr, (socklen_t)sizeof(addr)) == 0)
            break;
    }

    if (i == host.getAddressCount()) {
        connectError();
        endSocket();
        return;
    }

    Socket::state = CONNECTED;
}

SimpleTCPStream::SimpleTCPStream() :
Socket(PF_INET, SOCK_STREAM, IPPROTO_TCP) {}

SimpleTCPStream::SimpleTCPStream(const SimpleTCPStream &source) :
#ifdef WIN32
Socket(source.so)
#else
Socket(dup(source.so))
#endif
{}

void SimpleTCPStream::endStream(void)
{
    endSocket();
}

ssize_t SimpleTCPStream::read(char *bytes, size_t length, timeout_t timeout)
{
    // Declare local variables.
    ssize_t rlen = 0;
    size_t totalrecv = 0;
    char *currentpos = bytes;

    // Check for reasonable requested length.
    if (length < 1) {
        return (ssize_t)totalrecv;
    }

    while (totalrecv < length) {
        // Check for timeout condition.
        if (timeout) {
            if (! isPending(pendingInput, timeout)) {
                error(errTimeout);
                return -1;
            }
        }

        // Attempt to read data.
        rlen = _IORET64 ::recv(so, (char *) currentpos, _IOLEN64 (length-totalrecv), 0);
        if (rlen == 0 || rlen == -1) {
            break;
        }
        // cout << "received " << rlen << " bytes, remaining " << length - totalrecv << flush;

        totalrecv += rlen;
        currentpos += rlen;
    }

    // Set error condition if necessary.
    if (rlen == -1) {
        error(errInput);
    }

    // Return total number of bytes recieved.
    return (ssize_t)totalrecv;
}

ssize_t SimpleTCPStream::write(const char *bytes, size_t length, timeout_t timeout)
{
    // Declare local variables.
    ssize_t rlen = 0;

    // Check for reasonable requested length.
    if (length < 1) {
        return rlen;
    }

    // Check for timeout condition.
    if (timeout) {
        if (! isPending(pendingOutput, timeout)) {
            error(errTimeout);
            return -1;
        }
    }

    // Attempt to write data.
    rlen = _IORET64 ::send(so, (const char *)bytes, _IOLEN64 length, MSG_NOSIGNAL);
    if (rlen == -1) {
        error(errOutput);
    }

    return rlen;
}

ssize_t SimpleTCPStream::peek(char *bytes, size_t length, timeout_t timeout)
{
    // Declare local variables.
    ssize_t rlen = 0;
    size_t totalrecv = 0;
    char *currentpos = bytes;

    // Check for reasonable requested length.
    if (length < 1) {
        return (ssize_t)totalrecv;
    }

    while (totalrecv < length) {
        // Check for timeout condition.
        if (timeout) {
            if (! isPending(pendingInput, timeout)) {
                error(errTimeout);
                return -1;
            }
        }

        // Attempt to read data.
        rlen = _IORET64 ::recv(so, (char *) currentpos, _IOLEN64 (length-totalrecv), MSG_PEEK);
        if (rlen == 0 || rlen == -1) {
            break;
        }
        // cout << "received " << rlen << " bytes, remaining " << length - totalrecv << flush;

        totalrecv += rlen;
        currentpos += rlen;
    }

    // Set error condition if necessary.
    if (rlen == -1) {
        error(errInput);
    }

    // Return total number of bytes recieved.
    return (ssize_t)totalrecv;
}

bool SimpleTCPStream::isPending(Pending pending, timeout_t timeout)
{
    return Socket::isPending(pending, timeout);
}

#ifdef CCXX_NAMESPACES
} /* for ost */
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
