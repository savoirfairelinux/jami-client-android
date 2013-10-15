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
#ifdef  CCXX_WITHOUT_EXTRAS
#include <cc++/export.h>
#endif
#include <cc++/thread.h>
#include <cc++/exception.h>
#ifndef CCXX_WITHOUT_EXTRAS
#include <cc++/export.h>
#endif

#ifdef  CCXX_SSL

#include <cc++/ssl.h>
#include <cerrno>
#include <cstdlib>
#include <cstdarg>
#include <cstdio>

#ifdef  CCXX_GNUTLS
#include <gcrypt.h>
#endif

#ifdef  WIN32
#include <io.h>
#define socket_errno    WSAGetLastError()
#else
#define socket_errno    errno
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

#ifdef  CCXX_GNUTLS

#ifndef WIN32
static int _gcry_mutex_init(Mutex **priv)
{
    Mutex *m = new Mutex();

    *priv = m;
    return 0;
}

static int _gcry_mutex_destroy(Mutex **priv)
{
    delete *priv;
    return 0;
}

static int _gcry_mutex_lock(Mutex **priv)
{
    (*priv)->enter();
    return 0;
}

static int _gcry_mutex_unlock(Mutex **priv)
{
    (*priv)->leave();
    return 0;
}

extern "C" {
    static int _wrap_mutex_init(void **priv)
    {
        return _gcry_mutex_init((Mutex **)(priv));
    }

    static int _wrap_mutex_destroy(void **priv)
    {
        return _gcry_mutex_destroy((Mutex **)(priv));
    }

    static int _wrap_mutex_lock(void **priv)
    {
        return _gcry_mutex_lock((Mutex **)(priv));
    }

    static int _wrap_mutex_unlock(void **priv)
    {
        return _gcry_mutex_unlock((Mutex **)(priv));
    }

    static struct gcry_thread_cbs _gcry_threads =
    {
        GCRY_THREAD_OPTION_PTHREAD, NULL,
        _wrap_mutex_init, _wrap_mutex_destroy,
        _wrap_mutex_lock, _wrap_mutex_unlock
    };

};

#endif

static class _ssl_global {
public:
    _ssl_global() {
#ifndef WIN32
        gcry_control(GCRYCTL_SET_THREAD_CBS, &_gcry_threads);
#endif
        gnutls_global_init();
    }

    ~_ssl_global() {
        gnutls_global_deinit();
    }

} _ssl_global;
#endif

#ifdef  CCXX_OPENSSL
static  Mutex *ssl_mutex = NULL;

extern "C" {
    static void ssl_lock(int mode, int n, const char *file, int line)
    {
        if(mode && CRYPTO_LOCK)
            ssl_mutex[n].enter();
        else
            ssl_mutex[n].leave();
    }

    static unsigned long ssl_thread(void)
    {
    #ifdef  WIN32
        return GetCurrentThreadId();
    #else
        return (unsigned long)pthread_self();
    #endif
    }
} // extern "C"

static class _ssl_global {
public:
    _ssl_global() {
        if(ssl_mutex)
            return;

        if(CRYPTO_get_id_callback() != NULL)
            return;

        ssl_mutex = new Mutex[CRYPTO_num_locks()];
        CRYPTO_set_id_callback(ssl_thread);
        CRYPTO_set_locking_callback(ssl_lock);
    }

    ~_ssl_global() {
        if(!ssl_mutex)
            return;
        CRYPTO_set_id_callback(NULL);
        CRYPTO_set_locking_callback(NULL);
        delete[] ssl_mutex;
        ssl_mutex = NULL;
    }
} _ssl_global;
#endif


SSLStream::SSLStream(Family f, bool tf, timeout_t to) :
TCPStream(f, tf, to)
{
    ssl = NULL;
}

SSLStream::SSLStream(const IPV4Host &h, tpport_t p, unsigned mss, bool tf, timeout_t to) :
TCPStream(h, p, mss, tf, to)
{
    ssl = NULL;
}

#ifdef  CCXX_IPV6
SSLStream::SSLStream(const IPV6Host &h, tpport_t p, unsigned mss, bool tf, timeout_t to) :
TCPStream(h, p, mss, tf, to)
{
    ssl = NULL;
}
#endif

SSLStream::SSLStream(const char *name, Family f, unsigned mss, bool tf, timeout_t to) :
TCPStream(name, f, mss, tf, to)
{
    ssl = NULL;
}

ssize_t SSLStream::readLine(char *str, size_t request, timeout_t timeout)
{
    ssize_t nstat;
    unsigned count = 0;

    if(!ssl)
        return Socket::readLine(str, request, timeout);

    while(count < request) {
        if(timeout && !isPending(pendingInput, timeout)) {
            error(errTimeout, "Read timeout", 0);
            return -1;
        }

#ifdef  CCXX_GNUTLS
        nstat = gnutls_record_recv(ssl->session, str + count, 1);
#else
        nstat = SSL_read(ssl, str + count, 1);
#endif
        if(nstat <= 0) {
            error(errInput, "Could not read from socket", socket_errno);
            return -1;
        }

        if(str[count] == '\n') {
            if(count > 0 && str[count - 1] == '\r')
                --count;
            break;
        }

        ++count;
    }
    str[count] = 0;
    return count;
}

ssize_t SSLStream::writeData(void *source, size_t size, timeout_t timeout)
{
    ssize_t nstat, count = 0;
    if(size < 1)
        return 0;

    const char *slide = (const char *)source;

    while(size) {
        if(timeout && !isPending(pendingOutput, timeout)) {
            error(errOutput);
            return -1;
        }

#ifdef  CCXX_GNUTLS
        nstat = gnutls_record_send(ssl->session, slide, size);
#else
        nstat = SSL_write(ssl, slide, size);
#endif
        if(nstat <= 0) {
            error(errOutput);
            return -1;
        }
        count += nstat;
        size -= nstat;
        slide += nstat;
    }
    return count;
}

ssize_t SSLStream::readData(void *target, size_t size, char separator, timeout_t timeout)
{
    char *str = (char *)target;
    ssize_t nstat;
    unsigned count = 0;
    if(!ssl)
        return Socket::readData(target, size, separator, timeout);

    if(separator == 0x0d || separator == 0x0a)
        return readLine((char *)target, size, timeout);

    if(separator) {
        while(count < size) {
            if(timeout && !isPending(pendingInput, timeout)) {
                error(errTimeout, "Read timeout", 0);
                return -1;
            }

#ifdef  CCXX_GNUTLS
            nstat = gnutls_record_recv(ssl->session, str + count, 1);
#else
            nstat = SSL_read(ssl, str + count, 1);
#endif
            if(nstat <= 0) {
                error(errInput, "Could not read from socket", socket_errno);
                return -1;
            }
            if(str[count] == separator)
                break;
            ++count;
        }
        if(str[count] == separator)
            str[count] = 0;
        return count;
    }

    if(timeout && !isPending(pendingInput, timeout)) {
        error(errTimeout);
        return -1;
    }

#ifdef  CCXX_GNUTLS
    nstat = gnutls_record_recv(ssl->session, target, size);
#else
    nstat = SSL_read(ssl, target, size);
#endif

    if(nstat < 0) {
        error(errInput);
        return -1;
    }
    return nstat;
}

#ifdef  CCXX_GNUTLS
bool SSLStream::getSession(void)
{
        const int cert_priority[3] =
        {GNUTLS_CRT_X509, GNUTLS_CRT_OPENPGP, 0};

    if(ssl)
        return true;

    if(so == INVALID_SOCKET)
        return false;

    ssl = new SSL;
    if(gnutls_init(&ssl->session, GNUTLS_CLIENT)) {
        delete ssl;
        ssl = NULL;
        return false;
    }

    gnutls_set_default_priority(ssl->session);
    gnutls_certificate_allocate_credentials(&ssl->xcred);
    gnutls_certificate_type_set_priority(ssl->session, cert_priority);
    gnutls_credentials_set(ssl->session, GNUTLS_CRD_CERTIFICATE, ssl->xcred);
    gnutls_transport_set_ptr(ssl->session, (gnutls_transport_ptr)so);
    if(gnutls_handshake(ssl->session)) {
        gnutls_deinit(ssl->session);
        gnutls_certificate_free_credentials(ssl->xcred);
        delete ssl;
        ssl = NULL;
        return false;
    }
    return true;
}
#else
bool SSLStream::getSession(void)
{
    SSL_CTX *ctx;
    int err;

    if(ssl)
        return true;

    if(so == INVALID_SOCKET)
        return false;

    ctx = SSL_CTX_new(SSLv3_client_method());
    if(!ctx) {
        SSL_CTX_free(ctx);
        return false;
    }

    ssl = SSL_new(ctx);
    if(!ssl) {
        SSL_CTX_free(ctx);
        return false;
    }

    SSL_set_fd(ssl, so);
    SSL_set_connect_state(ssl);
    err = SSL_connect(ssl);

    if(err < 0)
        SSL_shutdown(ssl);

    if(err <= 0) {
        SSL_free(ssl);
        SSL_CTX_free(ctx);
        ssl = NULL;
        return false;
    }
    return true;
}
#endif

#ifdef  CCXX_GNUTLS
void SSLStream::endStream(void)
{
    if(ssl && so != INVALID_SOCKET)
        gnutls_bye(ssl->session, GNUTLS_SHUT_WR);
    TCPStream::endStream();
    if(ssl) {
        gnutls_deinit(ssl->session);
        gnutls_certificate_free_credentials(ssl->xcred);
        delete ssl;
        ssl = NULL;
    }
}

void SSLStream::disconnect(void)
{
    if(ssl && so != INVALID_SOCKET)
        gnutls_bye(ssl->session, GNUTLS_SHUT_WR);

    if(so != INVALID_SOCKET)
        TCPStream::disconnect();
    if(ssl) {
        gnutls_deinit(ssl->session);
        gnutls_certificate_free_credentials(ssl->xcred);
        delete ssl;
        ssl = NULL;
    }
}
#else
void SSLStream::disconnect(void)
{
    if(ssl) {
        if(so != INVALID_SOCKET)
            SSL_shutdown(ssl);
        SSL_free(ssl);
        ssl = NULL;
    }
    TCPStream::disconnect();
}

void SSLStream::endStream(void)
{
    if(ssl) {
        if(so != INVALID_SOCKET)
            SSL_shutdown(ssl);
        SSL_free(ssl);
        ssl = NULL;
    }
    TCPStream::endStream();
}
#endif

SSLStream::~SSLStream()
{
#ifdef  CCXX_EXCEPTIONS
    try { endStream(); }
    catch( ...) { if ( ! std::uncaught_exception()) throw;};
#else
    endStream();
#endif
}

#ifdef  CCXX_NAMESPACES
}
#endif

#endif
