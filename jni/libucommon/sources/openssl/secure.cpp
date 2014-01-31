// Copyright (C) 2010 David Sugar, Tycho Softworks.
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

#include "local.h"

#ifdef  HAVE_OPENSSL_FIPS_H
#include <openssl/fips.h>
#endif

static mutex_t *private_locks = NULL;

extern "C" {
    static void ssl_lock(int mode, int n, const char *file, int line)
    {
        if((mode & 0x03) == CRYPTO_LOCK)
            private_locks[n].acquire();
        else if((mode & 0x03) == CRYPTO_UNLOCK)
            private_locks[n].release();
    }

    static unsigned long ssl_self(void)
    {
#ifdef  _MSWINDOWS_
        return GetCurrentThreadId();
#else
        return (long)Thread::self();
#endif
    }
}

bool secure::fips(void)
{
#ifdef  HAVE_OPENSSL_FIPS_H

    // must always be first init function called...
    if(private_locks)
        return false;

    if(!FIPS_mode_set(1))
        return false;

    return init();
#else
    return false;
#endif
}

bool secure::init(void)
{
    if(private_locks)
        return true;

    Thread::init();
    Socket::init();

    SSL_library_init();
    SSL_load_error_strings();
    ERR_load_BIO_strings();
    OpenSSL_add_all_algorithms();
    OpenSSL_add_all_digests();

    if(CRYPTO_get_id_callback() != NULL)
        return false;

    private_locks = new Mutex[CRYPTO_num_locks()];
    CRYPTO_set_id_callback(ssl_self);
    CRYPTO_set_locking_callback(ssl_lock);
    return true;
}

void secure::cipher(secure *scontext, const char *ciphers)
{
    context *ctx = (context *)scontext;
    if(!ctx)
        return;

    SSL_CTX_set_cipher_list(ctx->ctx, ciphers);
}

secure::client_t secure::client(const char *ca)
{
    context *ctx = new(context);
    secure::init();

    if(!ctx)
        return NULL;

    ctx->error = secure::OK;

    ctx->ctx = SSL_CTX_new(SSLv23_client_method());

    if(!ctx->ctx) {
        ctx->error = secure::INVALID;
        return ctx;
    }

    if(!ca)
        return ctx;

    if(eq(ca, "*"))
        ca = oscerts();

    if(!SSL_CTX_load_verify_locations(ctx->ctx, ca, 0)) {
        ctx->error = secure::INVALID_AUTHORITY;
        return ctx;
    }

    return ctx;
}

secure::server_t secure::server(const char *certfile, const char *ca)
{
    context *ctx = new(context);

    if(!ctx)
        return NULL;

    secure::init();
    ctx->error = secure::OK;
    ctx->ctx = SSL_CTX_new(SSLv23_server_method());

    if(!ctx->ctx) {
        ctx->error = secure::INVALID;
        return ctx;
    }

    if(!SSL_CTX_use_certificate_chain_file(ctx->ctx, certfile)) {
        ctx->error = secure::MISSING_CERTIFICATE;
        return ctx;
    }

    if(!SSL_CTX_use_PrivateKey_file(ctx->ctx, certfile, SSL_FILETYPE_PEM)) {
        ctx->error = secure::MISSING_PRIVATEKEY;
        return ctx;
    }

    if(!SSL_CTX_check_private_key(ctx->ctx)) {
        ctx->error = secure::INVALID_CERTIFICATE;
        return ctx;
    }

    if(!ca)
        return ctx;

    if(eq(ca, "*"))
        ca = oscerts();

    if(!SSL_CTX_load_verify_locations(ctx->ctx, ca, 0)) {
        ctx->error = secure::INVALID_AUTHORITY;
        return ctx;
    }

    return ctx;
}

secure::error_t secure::verify(session_t session, const char *peername)
{
    SSL *ssl = (SSL *)session;

    char peer_cn[256];

    if(SSL_get_verify_result(ssl) != X509_V_OK)
        return secure::INVALID_CERTIFICATE;

    if(!peername)
        return secure::OK;

    X509 *peer = SSL_get_peer_certificate(ssl);

    if(!peer)
        return secure::INVALID_PEERNAME;

    X509_NAME_get_text_by_NID(
        X509_get_subject_name(peer),
        NID_commonName, peer_cn, sizeof(peer_cn));
    if(!eq_case(peer_cn, peername))
        return secure::INVALID_PEERNAME;

    return secure::OK;
}

secure::~secure()
{
}

context::~context()
{
    if(ctx)
        SSL_CTX_free(ctx);
}


