/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Yan Morin <yan.morin@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

#ifndef ACCOUNT_SCHEMA_H
#define ACCOUNT_SCHEMA_H_

/**
 * @file account_schema.h
 * @brief Account specfic keys/constants that must be shared in daemon and clients.
 */

// Account identifier
static const char *const CONFIG_ACCOUNT_ID                   = "Account.id";

// Common account parameters
static const char *const CONFIG_ACCOUNT_TYPE                 = "Account.type";
static const char *const CONFIG_ACCOUNT_ALIAS                = "Account.alias";
static const char *const CONFIG_ACCOUNT_MAILBOX              = "Account.mailbox";
static const char *const CONFIG_ACCOUNT_ENABLE               = "Account.enable";
static const char *const CONFIG_ACCOUNT_REGISTRATION_EXPIRE  = "Account.registrationExpire";
static const char *const CONFIG_ACCOUNT_REGISTRATION_STATUS = "Account.registrationStatus";
static const char *const CONFIG_ACCOUNT_REGISTRATION_STATE_CODE = "Account.registrationCode";
static const char *const CONFIG_ACCOUNT_REGISTRATION_STATE_DESC = "Account.registrationDescription";
static const char *const CONFIG_CREDENTIAL_NUMBER            = "Credential.count";
static const char *const CONFIG_ACCOUNT_DTMF_TYPE            = "Account.dtmfType";
static const char *const CONFIG_RINGTONE_PATH                = "Account.ringtonePath";
static const char *const CONFIG_RINGTONE_ENABLED             = "Account.ringtoneEnabled";
static const char *const CONFIG_KEEP_ALIVE_ENABLED           = "Account.keepAliveEnabled";

static const char *const CONFIG_ACCOUNT_HOSTNAME             = "Account.hostname";
static const char *const CONFIG_ACCOUNT_USERNAME             = "Account.username";
static const char *const CONFIG_ACCOUNT_ROUTESET             = "Account.routeset";
static const char *const CONFIG_ACCOUNT_PASSWORD             = "Account.password";
static const char *const CONFIG_ACCOUNT_REALM                = "Account.realm";
static const char *const CONFIG_ACCOUNT_DEFAULT_REALM        = "*";
static const char *const CONFIG_ACCOUNT_USERAGENT            = "Account.useragent";

static const char *const CONFIG_LOCAL_INTERFACE              = "Account.localInterface";
static const char *const CONFIG_PUBLISHED_SAMEAS_LOCAL       = "Account.publishedSameAsLocal";
static const char *const CONFIG_LOCAL_PORT                   = "Account.localPort";
static const char *const CONFIG_PUBLISHED_PORT               = "Account.publishedPort";
static const char *const CONFIG_PUBLISHED_ADDRESS            = "Account.publishedAddress";

static const char *const CONFIG_DISPLAY_NAME                 = "Account.displayName";
static const char *const CONFIG_DEFAULT_ADDRESS              = "0.0.0.0";

// SIP specific parameters
static const char *const CONFIG_SIP_PROXY                    = "SIP.proxy";
static const char *const CONFIG_STUN_SERVER                  = "STUN.server";
static const char *const CONFIG_STUN_ENABLE                  = "STUN.enable";

// SRTP specific parameters
static const char *const CONFIG_SRTP_ENABLE                  = "SRTP.enable";
static const char *const CONFIG_SRTP_KEY_EXCHANGE            = "SRTP.keyExchange";
static const char *const CONFIG_SRTP_ENCRYPTION_ALGO         = "SRTP.encryptionAlgorithm";  // Provided by ccRTP,0=NULL,1=AESCM,2=AESF8
static const char *const CONFIG_SRTP_RTP_FALLBACK            = "SRTP.rtpFallback";
static const char *const CONFIG_ZRTP_HELLO_HASH              = "ZRTP.helloHashEnable";
static const char *const CONFIG_ZRTP_DISPLAY_SAS             = "ZRTP.displaySAS";
static const char *const CONFIG_ZRTP_NOT_SUPP_WARNING        = "ZRTP.notSuppWarning";
static const char *const CONFIG_ZRTP_DISPLAY_SAS_ONCE        = "ZRTP.displaySasOnce";

static const char *const CONFIG_TLS_LISTENER_PORT            = "TLS.listenerPort";
static const char *const CONFIG_TLS_ENABLE                   = "TLS.enable";
static const char *const CONFIG_TLS_CA_LIST_FILE             = "TLS.certificateListFile";
static const char *const CONFIG_TLS_CERTIFICATE_FILE         = "TLS.certificateFile";
static const char *const CONFIG_TLS_PRIVATE_KEY_FILE         = "TLS.privateKeyFile";
static const char *const CONFIG_TLS_PASSWORD                 = "TLS.password";
static const char *const CONFIG_TLS_METHOD                   = "TLS.method";
static const char *const CONFIG_TLS_CIPHERS                  = "TLS.ciphers";
static const char *const CONFIG_TLS_SERVER_NAME              = "TLS.serverName";
static const char *const CONFIG_TLS_VERIFY_SERVER            = "TLS.verifyServer";
static const char *const CONFIG_TLS_VERIFY_CLIENT            = "TLS.verifyClient";
static const char *const CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE = "TLS.requireClientCertificate";
static const char *const CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC  = "TLS.negotiationTimeoutSec";
static const char *const CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC = "TLS.negotiationTimemoutMsec";

#endif
