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

#include <ucommon-config.h>
#include <ucommon/ucommon.h>
#include <ucommon/export.h>
#include <ucommon/secure.h>
#include <gnutls/gnutls.h>
#include <gnutls/crypto.h>

#ifdef  _MSWINDOWS_
#include <wincrypt.h>
#endif

#define NAMESPACE_LOCAL namespace __secure__ {
#define LOCAL_NAMESPACE __secure__

NAMESPACE_LOCAL
using namespace UCOMMON_NAMESPACE;

class __LOCAL context : public secure
{
public:
    ~context();

    unsigned int connect;
    gnutls_credentials_type_t xtype;
    gnutls_certificate_credentials_t xcred;
    gnutls_dh_params_t dh;

    static gnutls_priority_t priority_cache;

    static gnutls_session_t session(context *ctx);

    static int map_digest(const char *type);
    static int map_cipher(const char *type);
    static int map_hmac(const char *type);
};

typedef gnutls_session_t SSL;
typedef gnutls_mac_algorithm_t HMAC_ID;
typedef gnutls_hmac_hd_t HMAC_CTX;
typedef gnutls_digest_algorithm_t MD_ID;
typedef gnutls_hash_hd_t MD_CTX;
typedef gnutls_cipher_hd_t CIPHER_CTX;
typedef gnutls_cipher_algorithm_t CIPHER_ID;
typedef gnutls_datum_t CIPHER_KEYDATA;
typedef context *SSL_CTX;

END_NAMESPACE

using namespace UCOMMON_NAMESPACE;
using namespace LOCAL_NAMESPACE;
