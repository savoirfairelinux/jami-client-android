/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Alexandre Bourget <alexandre.bourget@savoirfairelinux.com>
 *  Author: Yan Morin <yan.morin@savoirfairelinux.com>
 *  Author: Pierre-Luc Bacon <pierre-luc.bacon@savoirfairelinux.com>
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

#ifndef SIPACCOUNT_H
#define SIPACCOUNT_H

#include <vector>
#include <map>
#include "account.h"
#include "pjsip/sip_transport_tls.h"
#include "pjsip/sip_types.h"
#include "pjsip-ua/sip_regc.h"
#include "noncopyable.h"

typedef std::vector<pj_ssl_cipher> CipherArray;

namespace Conf {
    class YamlEmitter;
    class MappingNode;
    // SIP specific configuration keys
    const char *const INTERFACE_KEY = "interface";
    const char *const PORT_KEY = "port";
    const char *const PUBLISH_ADDR_KEY = "publishAddr";
    const char *const PUBLISH_PORT_KEY = "publishPort";
    const char *const SAME_AS_LOCAL_KEY = "sameasLocal";
    const char *const DTMF_TYPE_KEY = "dtmfType";
    const char *const SERVICE_ROUTE_KEY = "serviceRoute";
    const char *const UPDATE_CONTACT_HEADER_KEY = "updateContact";
    const char *const KEEP_ALIVE_ENABLED = "keepAlive";

    // TODO: write an object to store credential which implement serializable
    const char *const SRTP_KEY = "srtp";
    const char *const SRTP_ENABLE_KEY = "enable";
    const char *const KEY_EXCHANGE_KEY = "keyExchange";
    const char *const RTP_FALLBACK_KEY = "rtpFallback";

    // TODO: wirte an object to store zrtp params wich implement serializable
    const char *const ZRTP_KEY = "zrtp";
    const char *const DISPLAY_SAS_KEY = "displaySas";
    const char *const DISPLAY_SAS_ONCE_KEY = "displaySasOnce";
    const char *const HELLO_HASH_ENABLED_KEY = "helloHashEnabled";
    const char *const NOT_SUPP_WARNING_KEY = "notSuppWarning";

    // TODO: write an object to store tls params which implement serializable
    const char *const TLS_KEY = "tls";
    const char *const TLS_PORT_KEY = "tlsPort";
    const char *const CERTIFICATE_KEY = "certificate";
    const char *const CALIST_KEY = "calist";
    const char *const CIPHERS_KEY = "ciphers";
    const char *const TLS_ENABLE_KEY = "enable";
    const char *const METHOD_KEY = "method";
    const char *const TIMEOUT_KEY = "timeout";
    const char *const TLS_PASSWORD_KEY = "password";
    const char *const PRIVATE_KEY_KEY = "privateKey";
    const char *const REQUIRE_CERTIF_KEY = "requireCertif";
    const char *const SERVER_KEY = "server";
    const char *const VERIFY_CLIENT_KEY = "verifyClient";
    const char *const VERIFY_SERVER_KEY = "verifyServer";

    const char *const STUN_ENABLED_KEY = "stunEnabled";
    const char *const STUN_SERVER_KEY = "stunServer";
    const char *const CRED_KEY = "credential";
}

class SIPVoIPLink;

/**
 * @file sipaccount.h
 * @brief A SIP Account specify SIP specific functions and object = SIPCall/SIPVoIPLink)
 */

class SIPAccount : public Account {
    public:
        static const char * const IP2IP_PROFILE;
        static const char * const OVERRTP_STR;
        static const char * const SIPINFO_STR;

        /**
         * Constructor
         * @param accountID The account identifier
         */
        SIPAccount(const std::string& accountID);

        virtual VoIPLink* getVoIPLink();

        std::string getUserAgentName() const;
        void setRegistrationStateDetailed(const std::pair<int, std::string> &details) {
            registrationStateDetailed_ = details;
        }

        /**
         * Returns true if this is the IP2IP account
         */
        bool isIP2IP() const;

        /**
         * Serialize internal state of this account for configuration
         * @param YamlEmitter the configuration engine which generate the configuration file
         */
        virtual void serialize(Conf::YamlEmitter &emitter);

        /**
         * Populate the internal state for this account based on info stored in the configuration file
         * @param The configuration node for this account
         */
        virtual void unserialize(const Conf::MappingNode &map);

        /**
         * Set the internal state for this account, mainly used to manage account details from the client application.
         * @param The map containing the account information.
         */
        virtual void setAccountDetails(std::map<std::string, std::string> details);

        /**
         * Return an map containing the internal state of this account. Client application can use this method to manage
         * account info.
         * @return A map containing the account information.
         */
        virtual std::map<std::string, std::string> getAccountDetails() const;

        /**
         * Return the information for the default IP to IP account
         */
        std::map<std::string, std::string> getIp2IpDetails() const;

        /**
         * Return the TLS settings, mainly used to return security information to
         * a client application
         */
        std::map<std::string, std::string> getTlsSettings() const;

        /**
         * Manage the TLS settings from a client application
         */
        void setTlsSettings(const std::map<std::string, std::string>& details);

        /**
         * Actually useless, since config loading is done in init()
         */
        void loadConfig();

        /**
         * Initialize the SIP voip link with the account parameters and send registration
         */
        void registerVoIPLink();

        /**
         * Send unregistration and clean all related stuff ( calls , thread )
         */
        void unregisterVoIPLink();

        /**
         * Start the keep alive function, once started, the account will be registered periodically
         * a new REGISTER request is sent bey the client application. The account must be initially
         * registered for this call to be effective.
         */
        void startKeepAliveTimer();

        /**
         * Stop the keep alive timer. Once canceled, no further registration will be scheduled
         */
        void stopKeepAliveTimer();


        const pjsip_cred_info* getCredInfo() const {
            return &(*cred_.begin());
        }

        /**
         * Get the number of credentials defined for
         * this account.
         * @param none
         * @return int The number of credentials set for this account.
         */
        unsigned getCredentialCount() const {
            return credentials_.size();
        }

        bool hasCredentials() const {
            return not credentials_.empty();
        }

        void setCredentials(const std::vector<std::map<std::string, std::string> >& details);

        const std::vector<std::map<std::string, std::string> > &
        getCredentials() const;

        /**
         * A client sendings a REGISTER request MAY suggest an expiration
         * interval that indicates how long the client would like the
         * registration to be valid.
         *
         * @return the expiration value.
         */
        unsigned getRegistrationExpire() const {
            if (registrationExpire_ == 0)
                return PJSIP_REGC_EXPIRATION_NOT_SPECIFIED;

            return registrationExpire_;
        }

        /**
         * Set the expiration for this account as found in
         * the "Expire" sip header or the CONTACT's "expire" param.
         */
        void setRegistrationExpire(int expire) {
            if (expire > 0)
                registrationExpire_ = expire;
        }

        /**
         * Doubles the Expiration Interval sepecified for registration.
         */
        void doubleRegistrationExpire() {
            registrationExpire_ *= 2;

            if (registrationExpire_ < 0)
                registrationExpire_ = 0;
        }

        bool fullMatch(const std::string& username, const std::string& hostname) const;
        bool userMatch(const std::string& username) const;
        bool hostnameMatch(const std::string& hostname) const;

        /**
         * Registration flag
	 */
        bool isRegistered() const {
            return bRegister_;
        }

	/**
         * Set registration flag
         */
        void setRegister(bool result) {
            bRegister_ = result;
        }

        /**
         * Get the registration stucture that is used
         * for PJSIP in the registration process.
         * Settings are loaded from configuration file.
         * @return pjsip_regc* A pointer to the registration structure
         */
        pjsip_regc* getRegistrationInfo() {
            return regc_;
        }

        /**
         * Set the registration structure that is used
         * for PJSIP in the registration process;
         * @pram A pointer to the new registration structure
         * @return void
         */
        void setRegistrationInfo(pjsip_regc *regc) {
            regc_ = regc;
        }

        /**
         * @return pjsip_tls_setting structure, filled from the configuration
         * file, that can be used directly by PJSIP to initialize
         * TLS transport.
         */
        pjsip_tls_setting * getTlsSetting() {
            return &tlsSetting_;
        }

        /**
         * @return pj_str_t , filled from the configuration
         * file, that can be used directly by PJSIP to initialize
         * an alternate UDP transport.
         */
        std::string getStunServer() const {
            return stunServer_;
        }
        void setStunServer(const std::string &srv) {
            stunServer_ = srv;
        }

        pj_str_t getStunServerName() const {
            return stunServerName_;
        }

        /**
         * @return pj_uint8_t structure, filled from the configuration
         * file, that can be used directly by PJSIP to initialize
         * an alternate UDP transport.
         */
        pj_uint16_t getStunPort() const {
            return stunPort_;
        }
        void setStunPort(pj_uint16_t port) {
            stunPort_ = port;
        }

        /**
         * @return bool Tells if current transport for that
         * account is set to TLS.
         */
        bool isTlsEnabled() const {
            return transportType_ == PJSIP_TRANSPORT_TLS;
        }

        /**
         * @return bool Tells if current transport for that
         * account is set to OTHER.
         */
        bool isStunEnabled() const {
            return stunEnabled_;
        }

        /*
         * @return pj_str_t "From" uri based on account information.
         * From RFC3261: "The To header field first and foremost specifies the desired
         * logical" recipient of the request, or the address-of-record of the
         * user or resource that is the target of this request. [...]  As such, it is
         * very important that the From URI not contain IP addresses or the FQDN
         * of the host on which the UA is running, since these are not logical
         * names."
         */
        std::string getFromUri() const;

        /*
         * This method adds the correct scheme, hostname and append
         * the ;transport= parameter at the end of the uri, in accordance with RFC3261.
         * It is expected that "port" is present in the internal hostname_.
         *
         * @return pj_str_t "To" uri based on @param username
         * @param username A string formatted as : "username"
         */
        std::string getToUri(const std::string& username) const;

        /*
         * In the current version of SFLPhone, "srv" uri is obtained in the preformated
         * way: hostname:port. This method adds the correct scheme and append
         * the ;transport= parameter at the end of the uri, in accordance with RFC3261.
         *
         * @return pj_str_t "server" uri based on @param hostPort
         * @param hostPort A string formatted as : "hostname:port"
         */
        std::string getServerUri() const;

        /**
         * Set the contact header
         * @param port Optional port. Otherwise set to the port defined for that account.
         * @param hostname Optional local address. Otherwise set to the hostname defined for that account.
         */
        void setContactHeader(std::string address, std::string port);

        /**
         * Get the contact header for
         * @return pj_str_t The contact header based on account information
         */
        std::string getContactHeader(void) const;

        /**
         * The contact header can be rewritten based on the contact provided by the registrar in 200 OK
         */
        void enableContactUpdate(void) {
            contactUpdateEnabled_ = true;
        }

        /**
         * The contact header is not updated even if the registrar
         */
        void disableContactUpdate(void) {
            contactUpdateEnabled_ = false;
        }

        bool isContactUpdateEnabled(void) {
            return contactUpdateEnabled_;
        }

        /**
         * Get the local interface name on which this account is bound.
         */
        std::string getLocalInterface() const {
            return interface_;
        }

        /**
         * Get a flag which determine the usage in sip headers of either the local
         * IP address and port (_localAddress and localPort_) or to an address set
         * manually (_publishedAddress and publishedPort_).
         */
        bool getPublishedSameasLocal() const {
            return publishedSameasLocal_;
        }

        /**
         * Get the port on which the transport/listener should use, or is
         * actually using.
         * @return pj_uint16 The port used for that account
         */
        pj_uint16_t getLocalPort() const {
            return localPort_;
        }

        /**
         * Set the new port on which this account is running over.
         * @pram port The port used by this account.
         */
        void setLocalPort(pj_uint16_t port) {
            localPort_ = port;
        }

        /**
         * Get the published port, which is the port to be advertised as the port
         * for the chosen SIP transport.
         * @return pj_uint16 The port used for that account
         */
        pj_uint16_t getPublishedPort() const {
            return (pj_uint16_t) publishedPort_;
        }

        /**
         * Set the published port, which is the port to be advertised as the port
         * for the chosen SIP transport.
         * @pram port The port used by this account.
         */
        void setPublishedPort(pj_uint16_t port) {
            publishedPort_ = port;
        }

        /**
             * Get the local port for TLS listener.
             * @return pj_uint16 The port used for that account
             */
        pj_uint16_t getTlsListenerPort() const {
            return tlsListenerPort_;
        }

        /**
         * Get the public IP address set by the user for this account.
         * If this setting is not provided, the local bound adddress
         * will be used.
         * @return std::string The public IPV4 address formatted in the standard dot notation.
         */
        std::string getPublishedAddress() const {
            return publishedIpAddress_;
        }

        /**
         * Set the public IP address to be used in Contact header.
         * @param The public IPV4 address in the standard dot notation.
         * @return void
         */
        void setPublishedAddress(const std::string &publishedIpAddress) {
            publishedIpAddress_ = publishedIpAddress;
        }

        std::string getServiceRoute() const {
            return serviceRoute_;
        }

        std::string getDtmfType() const {
            return dtmfType_;
        }

        bool getSrtpEnabled() const {
            return srtpEnabled_;
        }

        std::string getSrtpKeyExchange() const {
            return srtpKeyExchange_;
        }

        bool getSrtpFallback() const {
            return srtpFallback_;
        }

        bool getZrtpHelloHash() const {
            return zrtpHelloHash_;
        }

        void setReceivedParameter(const std::string &received) {
            receivedParameter_ = received;
        }

        std::string getReceivedParameter() const {
            return receivedParameter_;
        }

        int getRPort() const {
            if (rPort_ == -1)
                return localPort_;
            else
                return rPort_;
        }

        void setRPort(int rPort) { rPort_ = rPort; }

        /**
         * Timer used to periodically send re-register request based
         * on the "Expire" sip header (or the "expire" Contact parameter)
         */
        static void keepAliveRegistrationCb(pj_timer_heap_t *th, pj_timer_entry *te);

        bool isKeepAliveEnabled() const {
            return keepAliveEnabled_;
        }

        /**
         * Pointer to the transport used by this acccount
         */
        pjsip_transport* transport_;

    private:
        NON_COPYABLE(SIPAccount);

        /**
         * Map of credential for this account
         */
        std::vector< std::map<std::string, std::string > > credentials_;

        /**
         * Maps a string description of the SSL method
         * to the corresponding enum value in pjsip_ssl_method.
         * @param method The string representation
         * @return pjsip_ssl_method The corresponding value in the enum
         */
        static pjsip_ssl_method sslMethodStringToPjEnum(const std::string& method);

        /**
         * Initializes tls settings from configuration file.
         */
        void initTlsConfiguration();

        /**
         * Initializes STUN config from the config file
         */
        void initStunConfiguration();

        /**
         * If username is not provided, as it happens for Direct ip calls,
         * fetch the Real Name field of the user that is currently
         * running this program.
         * @return std::string The login name under which SFLPhone is running.
         */
        static std::string getLoginName();

        /**
         * The pjsip client registration information
	 */
        pjsip_regc *regc_;

        /**
	 * To check if the account is registered
         */
        bool bRegister_;

        /**
         * Network settings
         */
        int registrationExpire_;

        /**
         * interface name on which this account is bound
         */
        std::string interface_;

        /**
         * Flag which determine if localIpAddress_ or publishedIpAddress_ is used in
         * sip headers
         */
        bool publishedSameasLocal_;

        /**
         * Published IP address, ued only if defined by the user in account
         * configuration
         */
        std::string publishedIpAddress_;

        /**
         * Local port to whih this account is bound
         */
        pj_uint16_t localPort_;

        /**
         * Published port, used only if defined by the user
         */
        pj_uint16_t publishedPort_;

        /**
         * Optional list of SIP service this
         */
        std::string serviceRoute_;

        /**
         * The global TLS listener port which can be configured through the IP2IP_PROFILE
         */
        pj_uint16_t tlsListenerPort_;

        /**
         * Transport type used for this sip account. Currently supported types:
         *    PJSIP_TRANSPORT_UNSPECIFIED
         *    PJSIP_TRANSPORT_UDP
         *    PJSIP_TRANSPORT_TLS
         */
        pjsip_transport_type_e transportType_;

        /**
         * Credential information stored for further registration.
         */
        std::vector<pjsip_cred_info> cred_;

        /**
         * The TLS settings, used only if tls is chosen as a sip transport.
         */
        pjsip_tls_setting tlsSetting_;

        /**
         * Allocate a static array to be used by pjsip to store the supported ciphers on this system.
         */
        CipherArray ciphers;

        /**
         * The CONTACT header used for registration as provided by the registrar, this value could differ
         * from the host name in case the registrar is inside a subnetwork (such as a VPN).
         * The header will be stored
         */
        std::string contactHeader_;

        /**
         * Enble the contact header based on the header received from the registrar in 200 OK
         */
        bool contactUpdateEnabled_;

        /**
         * The STUN server name (hostname)
         */
        pj_str_t stunServerName_;

        /**
         * The STUN server port
         */
        pj_uint16_t stunPort_;

        /**
         * DTMF type used for this account SIPINFO or RTP
         */
        std::string dtmfType_;

        /**
         * Determine if TLS is enabled for this account. TLS provides a secured channel for
         * SIP signalization. It is independant than the media encription provided by SRTP or ZRTP.
         */
        std::string tlsEnable_;

        /**
         * Certificate autority file
         */
        std::string tlsCaListFile_;
        std::string tlsCertificateFile_;
        std::string tlsPrivateKeyFile_;
        std::string tlsPassword_;
        std::string tlsMethod_;
        std::string tlsCiphers_;
        std::string tlsServerName_;
        bool tlsVerifyServer_;
        bool tlsVerifyClient_;
        bool tlsRequireClientCertificate_;
        std::string tlsNegotiationTimeoutSec_;
        std::string tlsNegotiationTimeoutMsec_;

        /**
         * The stun server hostname (optional), used to provide the public IP address in case the softphone
         * stay behind a NAT.
         */
        std::string stunServer_;

        /**
         * Determine if STUN public address resolution is required to register this account. In this case a
         * STUN server hostname must be specified.
         */
        bool stunEnabled_;

        /**
         * Determine if SRTP is enabled for this account, SRTP and ZRTP are mutually exclusive
         * This only determine if the media channel is secured. One could only enable TLS
         * with no secured media channel.
         */
        bool srtpEnabled_;

        /**
         * Specifies the type of key exchange usd for SRTP (sdes/zrtp)
         */
        std::string srtpKeyExchange_;

        /**
         * Determine if the softphone should fallback on non secured media channel if SRTP negotiation fails.
         * Make sure other SIP endpoints share the same behavior since it could result in encrypted data to be
         * played through the audio device.
         */
        bool srtpFallback_;

        /**
         * Determine if the SAS sould be displayed on client side. SAS is a 4-charcter string
         * that end users should verbaly validate to ensure the channel is secured. Used especially
         * to prevent man-in-the-middle attack.
         */
        bool zrtpDisplaySas_;

        /**
         * Only display SAS 4-character string once at the begining of the call.
         */
        bool zrtpDisplaySasOnce_;

        bool zrtpHelloHash_;
        bool zrtpNotSuppWarning_;

        /**
         * Details about the registration state.
         * This is a protocol Code:Description pair.
         */
        std::pair<int, std::string> registrationStateDetailed_;

        /**
         * Determine if the keep alive timer will be activated or not
         */
        bool keepAliveEnabled_;

        /**
         * Timer used to regularrly send re-register request based
         * on the "Expire" sip header (or the "expire" Contact parameter)
         */
        pj_timer_entry keepAliveTimer_;

        /**
         * Once enabled, this variable tells if the keepalive timer is activated
         * for this accout
         */
        bool keepAliveTimerActive_;

        /**
         * Voice over IP Link contains a listener thread and calls
         */
        SIPVoIPLink* link_;

        /**
         * Optional: "received" parameter from VIA header
         */
        std::string receivedParameter_;

        /**
         * Optional: "rport" parameter from VIA header
         */
        int rPort_;
};

#endif
