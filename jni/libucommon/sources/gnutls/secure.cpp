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

extern "C" {
    static void secure_shutdown(void)
    {
        gnutls_global_deinit();
    }
}

gnutls_priority_t context::priority_cache;

bool secure::fips(void)
{
    return false;
}

bool secure::init(void)
{
    static bool initialized = false;

    if(!initialized) {
        Thread::init();
        Socket::init();

        gnutls_global_init();
        gnutls_priority_init (&context::priority_cache, "NORMAL", NULL);
        atexit(secure_shutdown);
        initialized = true;
    }
    return true;
}

secure::server_t secure::server(const char *certfile, const char *ca)
{
    context *ctx = new context;

    if(!ctx)
        return NULL;

    ctx->error = secure::OK;
    ctx->connect = GNUTLS_SERVER;
    ctx->xtype = GNUTLS_CRD_CERTIFICATE;
    ctx->xcred = NULL;
    ctx->dh = NULL;
    gnutls_certificate_allocate_credentials(&ctx->xcred);

    gnutls_certificate_set_x509_key_file(ctx->xcred, certfile, certfile, GNUTLS_X509_FMT_PEM);

    if(!ca)
        return ctx;

    if(eq(ca, "*"))
        ca = oscerts();

    gnutls_certificate_set_x509_trust_file (ctx->xcred, ca, GNUTLS_X509_FMT_PEM);

    return ctx;
}

secure::client_t secure::client(const char *ca)
{
    context *ctx = new context;

    if(!ctx)
        return NULL;

    ctx->error = secure::OK;
    ctx->connect = GNUTLS_CLIENT;
    ctx->xtype = GNUTLS_CRD_CERTIFICATE;
    ctx->xcred = NULL;
    ctx->dh = NULL;
    gnutls_certificate_allocate_credentials(&ctx->xcred);

    if(!ca)
        return ctx;

    if(eq(ca, "*"))
        ca = oscerts();

    gnutls_certificate_set_x509_trust_file (ctx->xcred, ca, GNUTLS_X509_FMT_PEM);

    return ctx;
}

context::~context()
{
    if(dh)
        gnutls_dh_params_deinit(dh);

    if(!xcred)
        return;

    switch(xtype) {
    case GNUTLS_CRD_ANON:
        gnutls_anon_free_client_credentials((gnutls_anon_client_credentials_t)xcred);
        break;
    case GNUTLS_CRD_CERTIFICATE:
        gnutls_certificate_free_credentials(xcred);
        break;
    default:
        break;
    }
}

secure::~secure()
{
}

gnutls_session_t context::session(context *ctx)
{
    SSL ssl = NULL;
    if(ctx && ctx->xcred && ctx->err() == secure::OK) {
        gnutls_init(&ssl, ctx->connect);
        switch(ctx->connect) {
        case GNUTLS_CLIENT:
            gnutls_priority_set_direct(ssl, "PERFORMANCE", NULL);
            break;
        case GNUTLS_SERVER:
            gnutls_priority_set(ssl, context::priority_cache);
            gnutls_certificate_server_set_request(ssl, GNUTLS_CERT_REQUEST);
            gnutls_session_enable_compatibility_mode(ssl);
        default:
            break;
        }
        gnutls_credentials_set(ssl, ctx->xtype, ctx->xcred);
    }
    return ssl;
}


