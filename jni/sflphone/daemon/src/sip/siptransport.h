/*
 *  Copyright (C) [2004, 2012] Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

#ifndef SIPTRANSPORT_H_
#define SIPTRANSPORT_H_

#include <string>
#include <vector>

#include <pjsip.h>
#include <pjlib.h>
#include <pjsip_ua.h>
#include <pjlib-util.h>
#include <pjnath.h>
#include <pjnath/stun_config.h>
#include "noncopyable.h"

#include "config.h"

class SIPAccount;

class SipTransport {
    public:
        SipTransport(pjsip_endpoint *endpt, pj_caching_pool *cp, pj_pool_t *pool);
        static std::string getSIPLocalIP();

        /**
         * Get the IP for the network interface named ifaceName
         */
        static std::string getInterfaceAddrFromName(const std::string &ifaceName);

        /**
        * List all the interfaces on the system and return
        * a vector list containing their name (eth0, eth0:1 ...).
        * @param void
        * @return std::vector<std::string> A std::string vector
        * of interface name available on all of the interfaces on
        * the system.
        */
        static std::vector<std::string> getAllIpInterfaceByName();

        /**
         * List all the interfaces on the system and return
         * a vector list containing their IPV4 address.
         * @param void
         * @return std::vector<std::string> A std::string vector
         * of IPV4 address available on all of the interfaces on
         * the system.
         */
        static std::vector<std::string> getAllIpInterface();

        void setEndpoint(pjsip_endpoint *endpt) {
            endpt_ = endpt;
        }

        void setCachingPool(pj_caching_pool *cp) {
            cp_ = cp;
        }

        void setPool(pj_pool_t *pool) {
            pool_ = pool;
        }

        pj_status_t destroyStunResolver(const std::string &serverName);

        /**
         * General Sip transport creation method according to the
         * transport type specified in account settings
         * @param account The account for which a transport must be created.
         */
        void createSipTransport(SIPAccount &account);

        /**
         * Initialize the transport selector
         * @param transport		A transport associated with an account
         *
         * @return          	A pointer to the transport selector structure
         */
        pjsip_tpselector *initTransportSelector(pjsip_transport *transport, pj_pool_t *tp_pool) const;

        /**
         * This function unset the transport for a given account.
         */
        void shutdownSipTransport(SIPAccount &account);

        /**
         * Get the correct address to use (ie advertised) from
         * a uri. The corresponding transport that should be used
         * with that uri will be discovered.
         *
         * @param uri The uri from which we want to discover the address to use
         * @param transport The transport to use to discover the address
         */
        void findLocalAddressFromTransport(pjsip_transport *transport, pjsip_transport_type_e transportType, std::string &address, std::string &port) const;

        /**
         * Create a new udp transport specifying the bound address AND the published address.
         * The published address is the address to appears in the sip VIA header. This is used
         * essentially when the client is behind a trafic routing device.
         *
         * @param The interface to bind this transport with
         * @param The requested udp port number
         * @param The public address for this transport
         * @param The public port for this transport
         */
        pjsip_transport *createUdpTransport(const std::string &interface, unsigned int port, const std::string &publicAddr, unsigned int publicPort);

    private:
        NON_COPYABLE(SipTransport);

        pjsip_transport *
        createStunTransport(SIPAccount &account);

#if HAVE_TLS
        /**
         * Create a connection oriented TLS transport and register to the specified remote address.
         * First, initialize the TLS listener sole instance. This means that, for the momment, only one TLS transport
         * is allowed to be created in the application. Any subsequent account attempting to
         * register a new using this transport even if new settings are specified.
         * @param the account that is creating the TLS transport
         */
        pjsip_transport *
        createTlsTransport(SIPAccount &account);

        /**
         * Create The default TLS listener which is global to the application. This means that
         * only one TLS connection can be established for the momment.
         * @param the SIPAccount for which we are creating the TLS listener
         * @return a pointer to the new listener
         */
        pjsip_tpfactory *
        createTlsListener(SIPAccount &account);
#endif

        /**
         * Create a new stun resolver. Store it inside the array. Resolve public address for this
         * server name.
         * @param serverName The name of the stun server
         * @param port number
         */
        pj_status_t createStunResolver(pj_str_t serverName, pj_uint16_t port);

        /**
        * Create SIP UDP transport from account's setting
        * @param account The account for which a transport must be created.
        */
        pjsip_transport *createUdpTransport(const std::string &interface,
                                            unsigned int port);

        /**
         * UDP Transports are stored in this map in order to retreive them in case
         * several accounts would share the same port number.
         */
        std::map<std::string, pjsip_transport*> transportMap_;

        /**
         * Stun resolver array
         */
        std::map<std::string, pj_stun_sock *> stunSocketMap_;

        pj_caching_pool *cp_;

        pj_pool_t *pool_;

        pjsip_endpoint *endpt_;
};

#endif // SIPTRANSPORT_H_
