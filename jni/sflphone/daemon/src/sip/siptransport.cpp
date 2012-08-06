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

#include <map>

#include <pjsip.h>
#include <pjlib.h>
#include <pjsip_ua.h>
#include <pjlib-util.h>
#include <pjnath.h>
#include <pjnath/stun_config.h>
#include <netinet/in.h>
#include <arpa/nameser.h>
#include <resolv.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <stdexcept>
#include <sstream>

#include "logger.h"
#include "siptransport.h"
#include "manager.h"

#include "sipaccount.h"

#include "pjsip/sip_types.h"
#if HAVE_TLS
#include "pjsip/sip_transport_tls.h"
#endif

#include "dbus/dbusmanager.h"
#include "dbus/configurationmanager.h"

static const char * const DEFAULT_INTERFACE = "default";
static const char * const ANY_HOSTS = "0.0.0.0";

#define RETURN_IF_FAIL(A, VAL, M, ...) if (!(A)) { ERROR(M, ##__VA_ARGS__); return (VAL); }

std::string SipTransport::getSIPLocalIP()
{
    pj_sockaddr ip_addr;

    const pj_status_t status = pj_gethostip(pj_AF_INET(), &ip_addr);
    RETURN_IF_FAIL(status == PJ_SUCCESS, "", "Could not get local IP");
    return pj_inet_ntoa(ip_addr.ipv4.sin_addr);
}

std::vector<std::string> SipTransport::getAllIpInterfaceByName()
{
    static ifreq ifreqs[20];
    ifconf ifconf;

    std::vector<std::string> ifaceList;
    ifaceList.push_back("default");

    ifconf.ifc_buf = (char*) (ifreqs);
    ifconf.ifc_len = sizeof(ifreqs);

    int sock = socket(AF_INET,SOCK_STREAM,0);

    if (sock >= 0) {
        if (ioctl(sock, SIOCGIFCONF, &ifconf) >= 0)
            for (unsigned i = 0; i < ifconf.ifc_len / sizeof(ifreq); ++i)
                ifaceList.push_back(std::string(ifreqs[i].ifr_name));

        close(sock);
    }

    return ifaceList;
}

std::string SipTransport::getInterfaceAddrFromName(const std::string &ifaceName)
{
    if (ifaceName == DEFAULT_INTERFACE)
        return getSIPLocalIP();

    int fd = socket(AF_INET, SOCK_DGRAM,0);
    RETURN_IF_FAIL(fd >= 0, "", "Could not open socket: %m");

    ifreq ifr;
    strcpy(ifr.ifr_name, ifaceName.c_str());
    memset(&ifr.ifr_addr, 0, sizeof(ifr.ifr_addr));
    ifr.ifr_addr.sa_family = AF_INET;

    ioctl(fd, SIOCGIFADDR, &ifr);
    close(fd);

    sockaddr_in *saddr_in = (sockaddr_in *) &ifr.ifr_addr;
    std::string result(inet_ntoa(saddr_in->sin_addr));
    if (result == ANY_HOSTS)
        result = getSIPLocalIP();
    return result;
}

std::vector<std::string> SipTransport::getAllIpInterface()
{
    pj_sockaddr addrList[16];
    unsigned addrCnt = PJ_ARRAY_SIZE(addrList);

    std::vector<std::string> ifaceList;

    if (pj_enum_ip_interface(pj_AF_INET(), &addrCnt, addrList) == PJ_SUCCESS) {
        for (unsigned i = 0; i < addrCnt; i++) {
            char addr[PJ_INET_ADDRSTRLEN];
            pj_sockaddr_print(&addrList[i], addr, sizeof(addr), 0);
            ifaceList.push_back(std::string(addr));
        }
    }

    return ifaceList;
}

SipTransport::SipTransport(pjsip_endpoint *endpt, pj_caching_pool *cp, pj_pool_t *pool) : transportMap_(), stunSocketMap_(), cp_(cp), pool_(pool), endpt_(endpt)
{}

pj_bool_t
stun_sock_on_status_cb(pj_stun_sock * /*stun_sock*/, pj_stun_sock_op op,
                       pj_status_t status)
{
    switch (op) {
        case PJ_STUN_SOCK_DNS_OP:
            DEBUG("STUN operation dns resolution");
            break;
        case PJ_STUN_SOCK_BINDING_OP:
            DEBUG("STUN operation binding");
            break;
        case PJ_STUN_SOCK_KEEP_ALIVE_OP:
            DEBUG("STUN operation keep alive");
            break;
        case PJ_STUN_SOCK_MAPPED_ADDR_CHANGE:
            DEBUG("STUN operation address mapping change");
            break;
        default:
            DEBUG("STUN unknown operation");
            break;
    }

    if (status == PJ_SUCCESS)
        DEBUG("STUN operation success");
    else
        ERROR("STUN operation failure");

    // Always return true so the stun transport registration retry even on failure
    return true;
}

static pj_bool_t
stun_sock_on_rx_data_cb(pj_stun_sock * /*stun_sock*/, void * /*pkt*/,
                        unsigned /*pkt_len*/,
                        const pj_sockaddr_t * /*src_addr*/,
                        unsigned /*addr_len*/)
{
    return PJ_TRUE;
}


pj_status_t SipTransport::createStunResolver(pj_str_t serverName, pj_uint16_t port)
{
    std::string stunResolverName(serverName.ptr, serverName.slen);
    if (stunSocketMap_.find(stunResolverName) != stunSocketMap_.end()) {
        DEBUG("%s already added", stunResolverName.c_str());
        return PJ_SUCCESS;
    }

    pj_stun_config stunCfg;
    pj_stun_config_init(&stunCfg, &cp_->factory, 0,
            pjsip_endpt_get_ioqueue(endpt_), pjsip_endpt_get_timer_heap(endpt_));

    static const pj_stun_sock_cb stun_sock_cb = {
        stun_sock_on_rx_data_cb,
        NULL,
        stun_sock_on_status_cb
    };

    pj_stun_sock *stun_sock = NULL;
    pj_status_t status = pj_stun_sock_create(&stunCfg,
            stunResolverName.c_str(), pj_AF_INET(), &stun_sock_cb, NULL, NULL,
            &stun_sock);

    if (status != PJ_SUCCESS) {
        char errmsg[PJ_ERR_MSG_SIZE];
        pj_strerror(status, errmsg, sizeof(errmsg));
        ERROR("Failed to create STUN socket for %.*s: %s",
              (int) serverName.slen, serverName.ptr, errmsg);
        return status;
    }

    status = pj_stun_sock_start(stun_sock, &serverName, port, NULL);

    // store socket inside list
    if (status == PJ_SUCCESS) {
        DEBUG("Adding %s resolver", stunResolverName.c_str());
        stunSocketMap_[stunResolverName] = stun_sock;
    } else {
        char errmsg[PJ_ERR_MSG_SIZE];
        pj_strerror(status, errmsg, sizeof(errmsg));
        DEBUG("Error starting STUN socket for %.*s: %s",
              (int) serverName.slen, serverName.ptr, errmsg);
        pj_stun_sock_destroy(stun_sock);
    }

    return status;
}

pj_status_t SipTransport::destroyStunResolver(const std::string &serverName)
{
    std::map<std::string, pj_stun_sock *>::iterator it;
    it = stunSocketMap_.find(serverName);

    DEBUG("***************** Destroy Stun Resolver *********************");

    if (it != stunSocketMap_.end()) {
        DEBUG("Deleting STUN resolver %s", it->first.c_str());
        if (it->second)
            pj_stun_sock_destroy(it->second);
        stunSocketMap_.erase(it);
    }

    return PJ_SUCCESS;
}

#if HAVE_TLS
pjsip_tpfactory* SipTransport::createTlsListener(SIPAccount &account)
{
    pj_sockaddr_in local_addr;
    pj_sockaddr_in_init(&local_addr, 0, 0);
    local_addr.sin_port = pj_htons(account.getTlsListenerPort());

    RETURN_IF_FAIL(account.getTlsSetting() != NULL, NULL, "TLS settings not specified");

    std::string interface(account.getLocalInterface());
    std::string listeningAddress;
    if (interface == DEFAULT_INTERFACE)
        listeningAddress = getSIPLocalIP();
    else
        listeningAddress = getInterfaceAddrFromName(interface);

    if (listeningAddress.empty())
        ERROR("Could not determine IP address for this transport");

    pj_str_t pjAddress;
    pj_cstr(&pjAddress, listeningAddress.c_str());
    pj_sockaddr_in_set_str_addr(&local_addr, &pjAddress);
    pj_sockaddr_in_set_port(&local_addr, account.getTlsListenerPort());

    pjsip_tpfactory *listener = NULL;
    const pj_status_t status = pjsip_tls_transport_start(endpt_, account.getTlsSetting(), &local_addr, NULL, 1, &listener);
    RETURN_IF_FAIL(status == PJ_SUCCESS, NULL, "Failed to start TLS listener");
    return listener;
}

pjsip_transport *
SipTransport::createTlsTransport(SIPAccount &account)
{
    std::string remoteSipUri(account.getServerUri());
    static const char SIPS_PREFIX[] = "<sips:";
    size_t sips = remoteSipUri.find(SIPS_PREFIX) + (sizeof SIPS_PREFIX) - 1;
    size_t trns = remoteSipUri.find(";transport");
    std::string remoteAddr(remoteSipUri.substr(sips, trns-sips));
    std::string ipAddr = "";
    int port = DEFAULT_SIP_TLS_PORT;

    // parse c string
    size_t pos = remoteAddr.find(":");
    if (pos != std::string::npos) {
        ipAddr = remoteAddr.substr(0, pos);
        port = atoi(remoteAddr.substr(pos + 1, remoteAddr.length() - pos).c_str());
    } else
        ipAddr = remoteAddr;

    pj_str_t remote;
    pj_cstr(&remote, ipAddr.c_str());

    pj_sockaddr_in rem_addr;
    pj_sockaddr_in_init(&rem_addr, &remote, (pj_uint16_t) port);

    // The local tls listener
    static pjsip_tpfactory *localTlsListener = NULL;

    if (localTlsListener == NULL)
        localTlsListener = createTlsListener(account);

    DEBUG("Get new tls transport from transport manager");
    pjsip_transport *transport = NULL;
    pjsip_endpt_acquire_transport(endpt_, PJSIP_TRANSPORT_TLS, &rem_addr,
                                  sizeof rem_addr, NULL, &transport);
    RETURN_IF_FAIL(transport != NULL, NULL, "Could not create new TLS transport");
    return transport;
}
#endif

namespace {
std::string transportMapKey(const std::string &interface, int port)
{
    std::ostringstream os;
    os << interface << ":" << port;
    return os.str();
}
}

void SipTransport::createSipTransport(SIPAccount &account)
{
    shutdownSipTransport(account);

#if HAVE_TLS
    if (account.isTlsEnabled()) {
        account.transport_ = createTlsTransport(account);
    } else if (account.isStunEnabled()) {
#else
    if (account.isStunEnabled()) {
#endif
        account.transport_ = createStunTransport(account);
        if (account.transport_ == NULL) {
            WARN("falling back to UDP transport");
            account.transport_ = createUdpTransport(account.getLocalInterface(), account.getLocalPort());
        }
    } else {
        // if this transport already exists, reuse it
        std::string key(transportMapKey(account.getLocalInterface(), account.getLocalPort()));
        std::map<std::string, pjsip_transport *>::iterator iter = transportMap_.find(key);

        if (iter != transportMap_.end()) {
            account.transport_ = iter->second;
            pjsip_transport_add_ref(account.transport_);
        } else
            account.transport_ = createUdpTransport(account.getLocalInterface(), account.getLocalPort());
    }

    if (!account.transport_) {
#if HAVE_TLS
        if (account.isTlsEnabled())
            throw std::runtime_error("Could not create TLS connection");
        else
#endif
            throw std::runtime_error("Could not create new UDP transport");
    }
}

pjsip_transport *
SipTransport::createUdpTransport(const std::string &interface, unsigned int port)
{
    // init socket to bind this transport to
    pj_uint16_t listeningPort = (pj_uint16_t) port;

    // determine the IP address for this transport
    std::string listeningAddress;
    if (interface == DEFAULT_INTERFACE)
        listeningAddress = getSIPLocalIP();
    else
        listeningAddress = getInterfaceAddrFromName(interface);

    RETURN_IF_FAIL(not listeningAddress.empty(), NULL, "Could not determine ip address for this transport");
    RETURN_IF_FAIL(listeningPort != 0, NULL, "Could not determine port for this transport");

    std::ostringstream fullAddress;
    fullAddress << listeningAddress << ":" << listeningPort;
    pj_str_t udpString;
    std::string fullAddressStr(fullAddress.str());
    pj_cstr(&udpString, fullAddressStr.c_str());
    pj_sockaddr boundAddr;
    pj_sockaddr_parse(pj_AF_UNSPEC(), 0, &udpString, &boundAddr);
    pj_status_t status;
    pjsip_transport *transport = NULL;

    if (boundAddr.addr.sa_family == pj_AF_INET()) {
        status = pjsip_udp_transport_start(endpt_, &boundAddr.ipv4, NULL, 1, &transport);
        RETURN_IF_FAIL(status == PJ_SUCCESS, NULL, "UDP IPV4 Transport did not start");
    } else if (boundAddr.addr.sa_family == pj_AF_INET6()) {
        status = pjsip_udp_transport_start6(endpt_, &boundAddr.ipv6, NULL, 1, &transport);
        RETURN_IF_FAIL(status == PJ_SUCCESS, NULL, "UDP IPV6 Transport did not start");
    }

    DEBUG("Created UDP transport on %s:%d", interface.c_str(), port);
    DEBUG("Listening address %s", fullAddressStr.c_str());
    // dump debug information to stdout
    pjsip_tpmgr_dump_transports(pjsip_endpt_get_tpmgr(endpt_));
    transportMap_[transportMapKey(interface, port)] = transport;

    return transport;
}

pjsip_transport *
SipTransport::createUdpTransport(const std::string &interface, unsigned int port, const std::string &publicAddr, unsigned int publicPort)
{
    // init socket to bind this transport to
    pj_uint16_t listeningPort = (pj_uint16_t) port;
    pjsip_transport *transport = NULL;

    DEBUG("Update UDP transport on %s:%d with public addr %s:%d",
          interface.c_str(), port, publicAddr.c_str(), publicPort);

    // determine the ip address for this transport
    std::string listeningAddress(getInterfaceAddrFromName(interface));

    RETURN_IF_FAIL(not listeningAddress.empty(), NULL, "Could not determine ip address for this transport");

    std::ostringstream fullAddress;
    fullAddress << listeningAddress << ":" << listeningPort;
    pj_str_t udpString;
    std::string fullAddressStr(fullAddress.str());
    pj_cstr(&udpString, fullAddressStr.c_str());
    pj_sockaddr boundAddr;
    pj_sockaddr_parse(pj_AF_UNSPEC(), 0, &udpString, &boundAddr);

    pj_str_t public_addr = pj_str((char *) publicAddr.c_str());
    pjsip_host_port hostPort;
    hostPort.host = public_addr;
    hostPort.port = publicPort;

    pj_status_t status = pjsip_udp_transport_start(endpt_, &boundAddr.ipv4, &hostPort, 1, &transport);
    RETURN_IF_FAIL(status == PJ_SUCCESS, NULL,
            "Could not start new transport with address %s:%d, error code %d", publicAddr.c_str(), publicPort, status);

    // dump debug information to stdout
    pjsip_tpmgr_dump_transports(pjsip_endpt_get_tpmgr(endpt_));

    return transport;
}

pjsip_tpselector *SipTransport::initTransportSelector(pjsip_transport *transport, pj_pool_t *tp_pool) const
{
    RETURN_IF_FAIL(transport != NULL, NULL, "Transport is not initialized");
    pjsip_tpselector *tp = (pjsip_tpselector *) pj_pool_zalloc(tp_pool, sizeof(pjsip_tpselector));
    tp->type = PJSIP_TPSELECTOR_TRANSPORT;
    tp->u.transport = transport;
    return tp;
}

pjsip_transport *SipTransport::createStunTransport(SIPAccount &account)
{
#define RETURN_IF_STUN_FAIL(A, M, ...) \
    if (!(A)) { \
        ERROR(M, ##__VA_ARGS__); \
        Manager::instance().getDbusManager()->getConfigurationManager()->stunStatusFailure(account.getAccountID()); \
        return NULL; }

    pj_str_t serverName = account.getStunServerName();
    pj_uint16_t port = account.getStunPort();

    DEBUG("Create STUN transport  server name: %s, port: %d", serverName, port);
    RETURN_IF_STUN_FAIL(createStunResolver(serverName, port) == PJ_SUCCESS, "Can't resolve STUN server");

    pj_sock_t sock = PJ_INVALID_SOCKET;

    pj_sockaddr_in boundAddr;

    RETURN_IF_STUN_FAIL(pj_sockaddr_in_init(&boundAddr, &serverName, 0) == PJ_SUCCESS,
                        "Can't initialize IPv4 socket on %*s:%i", serverName.slen, serverName.ptr, port);

    RETURN_IF_STUN_FAIL(pj_sock_socket(pj_AF_INET(), pj_SOCK_DGRAM(), 0, &sock) == PJ_SUCCESS,
                        "Can't create or bind socket");

    // Query the mapped IP address and port on the 'outside' of the NAT
    pj_sockaddr_in pub_addr;

    if (pjstun_get_mapped_addr(&cp_->factory, 1, &sock, &serverName, port, &serverName, port, &pub_addr) != PJ_SUCCESS) {
        ERROR("Can't contact STUN server");
        pj_sock_close(sock);
        Manager::instance().getDbusManager()->getConfigurationManager()->stunStatusFailure(account.getAccountID());
        return NULL;
    }

    pjsip_host_port a_name = {
        pj_str(pj_inet_ntoa(pub_addr.sin_addr)),
        pj_ntohs(pub_addr.sin_port)
    };

    pjsip_transport *transport;
    pjsip_udp_transport_attach2(endpt_, PJSIP_TRANSPORT_UDP, sock, &a_name, 1,
                                &transport);

    pjsip_tpmgr_dump_transports(pjsip_endpt_get_tpmgr(endpt_));

    return transport;
#undef RETURN_IF_STUN_FAIL
}

void SipTransport::shutdownSipTransport(SIPAccount &account)
{
    if (account.isStunEnabled()) {
        pj_str_t stunServerName = account.getStunServerName();
        std::string server(stunServerName.ptr, stunServerName.slen);
        destroyStunResolver(server);
    }

    if (account.transport_) {
        pjsip_transport_dec_ref(account.transport_);
        account.transport_ = NULL;
    }
}

void SipTransport::findLocalAddressFromTransport(pjsip_transport *transport, pjsip_transport_type_e transportType, std::string &addr, std::string &port) const
{
#define RETURN_IF_NULL(A, M, ...) if ((A) == NULL) { ERROR(M, ##__VA_ARGS__); return; }

    // Initialize the sip port with the default SIP port
    std::stringstream ss;
    ss << DEFAULT_SIP_PORT;
    port = ss.str();

    // Initialize the sip address with the hostname
    const pj_str_t *pjMachineName = pj_gethostname();
    addr = std::string(pjMachineName->ptr, pjMachineName->slen);

    // Update address and port with active transport
    RETURN_IF_NULL(transport, "Transport is NULL in findLocalAddress, using local address %s:%s", addr.c_str(), port.c_str());

    // get the transport manager associated with the SIP enpoint
    pjsip_tpmgr *tpmgr = pjsip_endpt_get_tpmgr(endpt_);
    RETURN_IF_NULL(tpmgr, "Transport manager is NULL in findLocalAddress, using local address %s:%s", addr.c_str(), port.c_str());

    // initialize a transport selector
    // TODO Need to determine why we exclude TLS here...
    // if (transportType == PJSIP_TRANSPORT_UDP and transport_)
    pjsip_tpselector *tp_sel = initTransportSelector(transport, pool_);
    RETURN_IF_NULL(tp_sel, "Could not initialize transport selector, using local address %s:%s", addr.c_str(), port.c_str());

    pj_str_t localAddress = {0,0};
    int i_port = 0;

    // Find the local address and port for this transport
    if (pjsip_tpmgr_find_local_addr(tpmgr, pool_, transportType, tp_sel, &localAddress, &i_port) != PJ_SUCCESS) {
        WARN("SipTransport: Could not retrieve local address and port from transport, using %s:%s", addr.c_str(), port.c_str());
        return;
    }

    // Update local address based on the transport type
    addr = std::string(localAddress.ptr, localAddress.slen);

    // Fallback on local ip provided by pj_gethostip()
    if (addr == ANY_HOSTS)
        addr = getSIPLocalIP();

    // Determine the local port based on transport information
    ss.str("");
    ss << i_port;
    port = ss.str();

#undef RETURN_IF_FAIL
}
