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

/**
 * @file process.h
 * @short Process services.
 **/

#ifndef CCXX_SSL_H_
#define CCXX_SSL_H_

#ifndef CCXX_CONFIG_H_
#include <cc++/config.h>
#endif

#ifndef CCXX_THREAD_H_
#include <cc++/thread.h>
#endif

#ifndef CCXX_SOCKET_H_
#include <cc++/socket.h>
#endif

#ifdef  CCXX_GNUTLS
#include <gnutls/gnutls.h>
typedef struct {
    gnutls_session  session;
    gnutls_certificate_credentials xcred;
    int result;
}       SSL;
#else
#include <openssl/ssl.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

class SSLStream : public TCPStream
{
protected:
    SSL *ssl;

public:
    SSLStream(Family family = IPV4, bool throwflag = true, timeout_t to = 0);
    void disconnect(void);

    SSLStream(const IPV4Host &host, tpport_t port, unsigned mss = 536, bool throwflag = true, timeout_t to = 0);
#ifdef  CCXX_IPV6
    SSLStream(const IPV6Host &host, tpport_t port, unsigned mss = 536, bool throwflag = true, timeout_t to = 0);
#endif
    SSLStream(const char *name, Family family = IPV4, unsigned mss = 536, bool throwflag = false, timeout_t to = 0);

    SSLStream(const SSLStream &ssl);

    inline bool isSSL(void)
        {return (bool)(ssl != NULL);};

    bool getSession(void);
    void endStream(void);
    virtual ~SSLStream();

    ssize_t readLine(char *str, size_t max, timeout_t to = 0);
    ssize_t readData(void *buf, size_t len, char separator = 0, timeout_t to = 0);
    ssize_t writeData(void *buf, size_t len, timeout_t to = 0);
};

#ifdef  CCXX_NAMESPACES
}
#endif

#endif
/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
