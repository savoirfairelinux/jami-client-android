/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010 Savoir-Faire Linux Inc.
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Yun Liu <yun.liu@savoirfairelinux.com>
 *  Author: Pierre-Luc Bacon <pierre-luc.bacon@savoirfairelinux.com>
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "sip_utils.h"

#include "sipvoiplink.h"
#include "array_size.h"
#include "manager.h"
#include "logger.h"

#include "sip/sdp.h"
#include "sipcall.h"
#include "sipaccount.h"
#include "eventthread.h"
#if HAVE_SDES
#include "sdes_negotiator.h"
#endif
#include "array_size.h"

#include "dbus/dbusmanager.h"
#include "dbus/callmanager.h"
#include "dbus/configurationmanager.h"

#include "im/instant_messaging.h"

#include "audio/audiolayer.h"

#ifdef SFL_VIDEO
#include "video/video_rtp_session.h"
#include "dbus/video_controls.h"
#endif

#include "pjsip/sip_endpoint.h"
#include "pjsip/sip_uri.h"
#include "pjnath.h"

#include <netinet/in.h>
#include <arpa/nameser.h>
#include <arpa/inet.h>
#include <resolv.h>
#include <istream>
// #include <fstream>
#include <utility> // for std::pair

#include <map>

using namespace sfl;

SIPVoIPLink *SIPVoIPLink::instance_ = 0;
bool SIPVoIPLink::destroyed_ = false;

namespace {

/** Environment variable used to set pjsip's logging level */
#define SIPLOGLEVEL "SIPLOGLEVEL"

/** A map to retreive SFLphone internal call id
 *  Given a SIP call ID (usefull for transaction sucha as transfer)*/
static std::map<std::string, std::string> transferCallID;

/**************** EXTERN VARIABLES AND FUNCTIONS (callbacks) **************************/

/**
 * Set audio and video (SDP) configuration for a call
 * localport, localip, localexternalport
 * @param call a SIPCall valid pointer
 */
void setCallMediaLocal(SIPCall* call, const std::string &localIP);

static pj_caching_pool pool_cache, *cp_ = &pool_cache;
static pj_pool_t *pool_;
static pjsip_endpoint *endpt_;
static pjsip_module mod_ua_;
static pj_thread_t *thread_;

void sdp_media_update_cb(pjsip_inv_session *inv, pj_status_t status);
void sdp_request_offer_cb(pjsip_inv_session *inv, const pjmedia_sdp_session *offer);
void sdp_create_offer_cb(pjsip_inv_session *inv, pjmedia_sdp_session **p_offer);
void invite_session_state_changed_cb(pjsip_inv_session *inv, pjsip_event *e);
void outgoing_request_forked_cb(pjsip_inv_session *inv, pjsip_event *e);
void transaction_state_changed_cb(pjsip_inv_session *inv, pjsip_transaction *tsx, pjsip_event *e);
void registration_cb(pjsip_regc_cbparam *param);
pj_bool_t transaction_request_cb(pjsip_rx_data *rdata);
pj_bool_t transaction_response_cb(pjsip_rx_data *rdata) ;

void transfer_client_cb(pjsip_evsub *sub, pjsip_event *event);

/**
 * Send a reINVITE inside an active dialog to modify its state
 * Local SDP session should be modified before calling this method
 * @param sip call
 */
int SIPSessionReinvite(SIPCall *);

/**
 * Helper function to process refer function on call transfer
 */
void onCallTransfered(pjsip_inv_session *inv, pjsip_rx_data *rdata);

void handleIncomingOptions(pjsip_rx_data *rdata)
{
    pjsip_tx_data *tdata;

    if (pjsip_endpt_create_response(endpt_, rdata, PJSIP_SC_OK, NULL, &tdata) != PJ_SUCCESS)
        return;

#define ADD_HDR(hdr) do { \
    const pjsip_hdr *cap_hdr = hdr; \
    if (cap_hdr) \
    pjsip_msg_add_hdr (tdata->msg, (pjsip_hdr*) pjsip_hdr_clone (tdata->pool, cap_hdr)); \
} while (0)
#define ADD_CAP(cap) ADD_HDR(pjsip_endpt_get_capability(endpt_, cap, NULL));

    ADD_CAP(PJSIP_H_ALLOW);
    ADD_CAP(PJSIP_H_ACCEPT);
    ADD_CAP(PJSIP_H_SUPPORTED);
    ADD_HDR(pjsip_evsub_get_allow_events_hdr(NULL));

    pjsip_response_addr res_addr;
    pjsip_get_response_addr(tdata->pool, rdata, &res_addr);

    if (pjsip_endpt_send_response(endpt_, &res_addr, tdata, NULL, NULL) != PJ_SUCCESS)
        pjsip_tx_data_dec_ref(tdata);
}

// return PJ_FALSE so that eventuall other modules will handle these requests
// TODO: move Voicemail to separate module
// TODO: add Buddy presence in separate module
pj_bool_t transaction_response_cb(pjsip_rx_data *rdata)
{
    pjsip_dialog *dlg = pjsip_rdata_get_dlg(rdata);

    if (!dlg)
        return PJ_FALSE;

    pjsip_transaction *tsx = pjsip_rdata_get_tsx(rdata);

    if (!tsx or tsx->method.id != PJSIP_INVITE_METHOD)
        return PJ_FALSE;

    if (tsx->status_code / 100 == 2) {
        /**
         * Send an ACK message inside a transaction. PJSIP send automatically, non-2xx ACK response.
         * ACK for a 2xx response must be send using this method.
         */
        pjsip_tx_data *tdata;
        if (rdata->msg_info.cseq) {
            pjsip_dlg_create_request(dlg, &pjsip_ack_method, rdata->msg_info.cseq->cseq, &tdata);
            pjsip_dlg_send_request(dlg, tdata, -1, NULL);
        }
    }

    return PJ_FALSE;
}

pj_bool_t transaction_request_cb(pjsip_rx_data *rdata)
{
    if (!rdata or !rdata->msg_info.msg) {
        ERROR("rx_data is NULL");
        return PJ_FALSE;
    }
    pjsip_method *method = &rdata->msg_info.msg->line.req.method;
    if (!method) {
        ERROR("method is NULL");
        return PJ_FALSE;
    }

    if (method->id == PJSIP_ACK_METHOD && pjsip_rdata_get_dlg(rdata))
        return PJ_FALSE;

    if (!rdata->msg_info.to or !rdata->msg_info.from) {
        ERROR("NULL from/to fields");
        return PJ_FALSE;
    }
    pjsip_sip_uri *sip_to_uri = (pjsip_sip_uri *) pjsip_uri_get_uri(rdata->msg_info.to->uri);
    pjsip_sip_uri *sip_from_uri = (pjsip_sip_uri *) pjsip_uri_get_uri(rdata->msg_info.from->uri);
    if (!sip_to_uri or !sip_from_uri) {
        ERROR("NULL uri");
        return PJ_FALSE;
    }
    std::string userName(sip_to_uri->user.ptr, sip_to_uri->user.slen);
    std::string server(sip_from_uri->host.ptr, sip_from_uri->host.slen);
    std::string account_id(Manager::instance().getAccountIdFromNameAndServer(userName, server));

    std::string displayName(sip_utils::parseDisplayName(rdata->msg_info.msg_buf));

    pjsip_msg_body *body = rdata->msg_info.msg->body;
    if (method->id == PJSIP_OTHER_METHOD) {
        pj_str_t *str = &method->name;
        std::string request(str->ptr, str->slen);

        if (request.find("NOTIFY") != std::string::npos) {
            if (body and body->data) {
                int voicemail = 0;
                int ret = sscanf((const char*) body->data, "Voice-Message: %d/", &voicemail);
                if (ret == 1 and voicemail != 0)
                    Manager::instance().startVoiceMessageNotification(account_id, voicemail);
            }
        }

        pjsip_endpt_respond_stateless(endpt_, rdata, PJSIP_SC_OK, NULL, NULL, NULL);
        return PJ_FALSE;
    } else if (method->id == PJSIP_OPTIONS_METHOD) {
        handleIncomingOptions(rdata);
        return PJ_FALSE;
    } else if (method->id != PJSIP_INVITE_METHOD && method->id != PJSIP_ACK_METHOD) {
        pjsip_endpt_respond_stateless(endpt_, rdata, PJSIP_SC_METHOD_NOT_ALLOWED, NULL, NULL, NULL);
        return PJ_FALSE;
    }

    SIPAccount *account = dynamic_cast<SIPAccount *>(Manager::instance().getAccount(account_id));
    if (!account) {
        ERROR("Could not find account %s", account_id.c_str());
        return PJ_FALSE;
    }

    pjmedia_sdp_session *r_sdp;

    if (!body || pjmedia_sdp_parse(rdata->tp_info.pool, (char*) body->data, body->len, &r_sdp) != PJ_SUCCESS)
        r_sdp = NULL;

    if (account->getActiveAudioCodecs().empty()) {
        pjsip_endpt_respond_stateless(endpt_, rdata,
                                      PJSIP_SC_NOT_ACCEPTABLE_HERE, NULL, NULL,
                                      NULL);
        return PJ_FALSE;
    }

    // Verify that we can handle the request
    unsigned options = 0;

    if (pjsip_inv_verify_request(rdata, &options, NULL, NULL, endpt_, NULL) != PJ_SUCCESS) {
        pjsip_endpt_respond_stateless(endpt_, rdata, PJSIP_SC_METHOD_NOT_ALLOWED, NULL, NULL, NULL);
        return PJ_FALSE;
    }

    Manager::instance().hookPreference.runHook(rdata->msg_info.msg);

    SIPCall* call = new SIPCall(Manager::instance().getNewCallID(), Call::INCOMING, cp_);
    Manager::instance().associateCallToAccount(call->getCallId(), account_id);

    // May use the published address as well
    std::string addrToUse = SipTransport::getInterfaceAddrFromName(account->getLocalInterface());
    std::string addrSdp = account->isStunEnabled()
                          ? account->getPublishedAddress()
                          : addrToUse;

    pjsip_tpselector *tp = SIPVoIPLink::instance()->sipTransport.initTransportSelector(account->transport_, call->getMemoryPool());

    char tmp[PJSIP_MAX_URL_SIZE];
    size_t length = pjsip_uri_print(PJSIP_URI_IN_FROMTO_HDR, sip_from_uri, tmp, PJSIP_MAX_URL_SIZE);
    std::string peerNumber(tmp, std::min(length, sizeof tmp));
    sip_utils::stripSipUriPrefix(peerNumber);

    std::string remote_user(sip_from_uri->user.ptr, sip_from_uri->user.slen);
    std::string remove_hostname(sip_from_uri->host.ptr, sip_from_uri->host.slen);
    if (remote_user.size() > 0 && remove_hostname.size() > 0) {
      peerNumber = remote_user+"@"+remove_hostname;
    }

    call->setConnectionState(Call::PROGRESSING);
    call->setPeerNumber(peerNumber);
    call->setDisplayName(displayName);
    call->initRecFilename(peerNumber);

    setCallMediaLocal(call, addrToUse);

    call->getLocalSDP()->setLocalIP(addrSdp);

    call->getAudioRtp().initConfig();
    call->getAudioRtp().initSession();

    if (body and body->len > 0) {
        std::string sdpOffer(static_cast<const char*>(body->data), body->len);
        size_t start = sdpOffer.find("a=crypto:");

        // Found crypto header in SDP
        if (start != std::string::npos) {
            CryptoOffer crypto_offer;
            crypto_offer.push_back(std::string(sdpOffer.substr(start, (sdpOffer.size() - start) - 1)));

            const size_t size = ARRAYSIZE(sfl::CryptoSuites);
            std::vector<sfl::CryptoSuiteDefinition> localCapabilities(size);

            std::copy(sfl::CryptoSuites, sfl::CryptoSuites + size,
                      localCapabilities.begin());

#if HAVE_SDES
            sfl::SdesNegotiator sdesnego(localCapabilities, crypto_offer);

            if (sdesnego.negotiate()) {
                call->getAudioRtp().setRemoteCryptoInfo(sdesnego);
                call->getAudioRtp().initLocalCryptoInfo();
            }
#endif
        }
    }

    call->getLocalSDP()->receiveOffer(r_sdp, account->getActiveAudioCodecs(), account->getActiveVideoCodecs());

    sfl::AudioCodec* ac = dynamic_cast<sfl::AudioCodec*>(Manager::instance().audioCodecFactory.instantiateCodec(PAYLOAD_CODEC_ULAW));
    if (!ac) {
        ERROR("Could not instantiate codec");
        delete call;
        return PJ_FALSE;
    }
    call->getAudioRtp().start(ac);

    pjsip_dialog *dialog = 0;

    if (pjsip_dlg_create_uas(pjsip_ua_instance(), rdata, NULL, &dialog) != PJ_SUCCESS) {
        delete call;
        pjsip_endpt_respond_stateless(endpt_, rdata, PJSIP_SC_INTERNAL_SERVER_ERROR, NULL, NULL, NULL);
        return PJ_FALSE;
    }

    pjsip_inv_create_uas(dialog, rdata, call->getLocalSDP()->getLocalSdpSession(), 0, &call->inv);

    if (pjsip_dlg_set_transport(dialog, tp) != PJ_SUCCESS) {
        ERROR("Could not set transport for dialog");
        delete call;
        return PJ_FALSE;
    }

    if (!call->inv) {
        ERROR("Call invite is not initialized");
        delete call;
        return PJ_FALSE;
    }

    call->inv->mod_data[mod_ua_.id] = call;

    // Check whether Replaces header is present in the request and process accordingly.
    pjsip_dialog *replaced_dlg;
    pjsip_tx_data *response;

    if (pjsip_replaces_verify_request(rdata, &replaced_dlg, PJ_FALSE, &response) != PJ_SUCCESS) {
        ERROR("Something wrong with Replaces request.");
        delete call;
        pjsip_endpt_respond_stateless(endpt_, rdata, 500 /* internal server error */, NULL, NULL, NULL);
    }

    // Check if call has been transfered
    pjsip_tx_data *tdata = 0;

    // If Replace header present
    if (replaced_dlg) {
        // Always answer the new INVITE with 200 if the replaced call is in early or confirmed state.
        if (pjsip_inv_answer(call->inv, PJSIP_SC_OK, NULL, NULL, &response) == PJ_SUCCESS)
            pjsip_inv_send_msg(call->inv, response);

        // Get the INVITE session associated with the replaced dialog.
        pjsip_inv_session *replaced_inv = pjsip_dlg_get_inv_session(replaced_dlg);

        // Disconnect the "replaced" INVITE session.
        if (pjsip_inv_end_session(replaced_inv, PJSIP_SC_GONE, NULL, &tdata) == PJ_SUCCESS && tdata)
            pjsip_inv_send_msg(replaced_inv, tdata);
    } else { // Prooceed with normal call flow
        if (pjsip_inv_initial_answer(call->inv, rdata, PJSIP_SC_RINGING, NULL, NULL, &tdata) != PJ_SUCCESS) {
            ERROR("Could not answer invite");
            delete call;
            return PJ_FALSE;
        }
        if (pjsip_inv_send_msg(call->inv, tdata) != PJ_SUCCESS) {
            ERROR("Could not send msg for invite");
            delete call;
            return PJ_FALSE;
        }

        call->setConnectionState(Call::RINGING);

        Manager::instance().incomingCall(*call, account_id);
        Manager::instance().getAccountLink(account_id)->addCall(call);
    }

    return PJ_FALSE;
}
} // end anonymous namespace

/*************************************************************************************************/

SIPVoIPLink::SIPVoIPLink() : sipTransport(endpt_, cp_, pool_), evThread_(this)
{
#define TRY(ret) do { \
    if (ret != PJ_SUCCESS) \
    throw VoipLinkException(#ret " failed"); \
} while (0)

    srand(time(NULL)); // to get random number for RANDOM_PORT

    TRY(pj_init());
    TRY(pjlib_util_init());

    setSipLogLevel();
    TRY(pjnath_init());

    pj_caching_pool_init(cp_, &pj_pool_factory_default_policy, 0);
    pool_ = pj_pool_create(&cp_->factory, PACKAGE, 4000, 4000, NULL);

    if (!pool_)
        throw VoipLinkException("UserAgent: Could not initialize memory pool");

    TRY(pjsip_endpt_create(&cp_->factory, pj_gethostname()->ptr, &endpt_));

    sipTransport.setEndpoint(endpt_);
    sipTransport.setCachingPool(cp_);
    sipTransport.setPool(pool_);

    if (SipTransport::getSIPLocalIP().empty())
        throw VoipLinkException("UserAgent: Unable to determine network capabilities");

    TRY(pjsip_tsx_layer_init_module(endpt_));
    TRY(pjsip_ua_init_module(endpt_, NULL));
    TRY(pjsip_replaces_init_module(endpt_)); // See the Replaces specification in RFC 3891
    TRY(pjsip_100rel_init_module(endpt_));

    // Initialize and register sflphone module
    mod_ua_.name = pj_str((char*) PACKAGE);
    mod_ua_.id = -1;
    mod_ua_.priority = PJSIP_MOD_PRIORITY_APPLICATION;
    mod_ua_.on_rx_request = &transaction_request_cb;
    mod_ua_.on_rx_response = &transaction_response_cb;
    TRY(pjsip_endpt_register_module(endpt_, &mod_ua_));

    TRY(pjsip_evsub_init_module(endpt_));
    TRY(pjsip_xfer_init_module(endpt_));

    static const pjsip_inv_callback inv_cb = {
        invite_session_state_changed_cb,
        outgoing_request_forked_cb,
        transaction_state_changed_cb,
        sdp_request_offer_cb,
        sdp_create_offer_cb,
        sdp_media_update_cb,
        NULL,
        NULL,
    };
    TRY(pjsip_inv_usage_init(endpt_, &inv_cb));

    static const pj_str_t allowed[] = { { (char*) "INFO", 4}, { (char*) "REGISTER", 8}, { (char*) "OPTIONS", 7}, { (char*) "MESSAGE", 7 } };       //  //{"INVITE", 6}, {"ACK",3}, {"BYE",3}, {"CANCEL",6}
    pjsip_endpt_add_capability(endpt_, &mod_ua_, PJSIP_H_ALLOW, NULL, PJ_ARRAY_SIZE(allowed), allowed);

    static const pj_str_t text_plain = { (char*) "text/plain", 10 };
    pjsip_endpt_add_capability(endpt_, &mod_ua_, PJSIP_H_ACCEPT, NULL, 1, &text_plain);

    static const pj_str_t accepted = { (char*) "application/sdp", 15 };
    pjsip_endpt_add_capability(endpt_, &mod_ua_, PJSIP_H_ACCEPT, NULL, 1, &accepted);

    DEBUG("pjsip version %s for %s initialized", pj_get_version(), PJ_OS_NAME);

    TRY(pjsip_replaces_init_module(endpt_));
#undef TRY

    handlingEvents_ = true;
    evThread_.start();
}

SIPVoIPLink::~SIPVoIPLink()
{
    const int MAX_TIMEOUT_ON_LEAVING = 5;
    for (int timeout = 0; pjsip_tsx_layer_get_tsx_count() and timeout < MAX_TIMEOUT_ON_LEAVING; timeout++)
        sleep(1);

    handlingEvents_ = false;
    if (thread_) {
        pj_thread_join(thread_);
        pj_thread_destroy(thread_);
        DEBUG("PJ thread destroy finished");
        thread_ = 0;
    }

    const pj_time_val tv = {0, 10};
    pjsip_endpt_handle_events(endpt_, &tv);
    pjsip_endpt_destroy(endpt_);

    pj_pool_release(pool_);
    pj_caching_pool_destroy(cp_);

    pj_shutdown();
}

SIPVoIPLink* SIPVoIPLink::instance()
{
    assert(!destroyed_);
    if (!instance_)
        instance_ = new SIPVoIPLink;
    return instance_;
}

void SIPVoIPLink::destroy()
{
    delete instance_;
    destroyed_ = true;
    instance_ = 0;
}

void SIPVoIPLink::setSipLogLevel()
{
    char *envvar = getenv(SIPLOGLEVEL);
    int level = 0;

    if(envvar != NULL) {
        std::string loglevel = envvar;

        if ( ! (std::istringstream(loglevel) >> level) ) level = 0;

        level = level > 6 ? 6 : level;
        level = level < 0 ? 0 : level;
    }

    // From 0 (min) to 6 (max)
    pj_log_set_level(level);
}

// Called from EventThread::run (not main thread)
bool SIPVoIPLink::getEvent()
{
    static pj_thread_desc desc;

    // We have to register the external thread so it could access the pjsip frameworks
    if (!pj_thread_is_registered()) {
        DEBUG("Registering thread");
        pj_thread_register(NULL, desc, &thread_);
    }

    static const pj_time_val timeout = {0, 10};
    pjsip_endpt_handle_events(endpt_, &timeout);
    return handlingEvents_;
}

void SIPVoIPLink::sendRegister(Account *a)
{
    SIPAccount *account = dynamic_cast<SIPAccount*>(a);

    if (!account)
        throw VoipLinkException("SipVoipLink: Account is not SIPAccount");
    try {
        sipTransport.createSipTransport(*account);
    } catch (const std::runtime_error &e) {
        ERROR("%s", e.what());
    }

    account->setRegister(true);
    account->setRegistrationState(TRYING);

    pjsip_regc *regc = account->getRegistrationInfo();

    if (pjsip_regc_create(endpt_, (void *) account, &registration_cb, &regc) != PJ_SUCCESS)
        throw VoipLinkException("UserAgent: Unable to create regc structure.");

    std::string srvUri(account->getServerUri());

    // std::string address, port;
    // findLocalAddressFromUri(srvUri, account->transport_, address, port);
    pj_str_t pjSrv = pj_str((char*) srvUri.c_str());

    // Generate the FROM header
    std::string from(account->getFromUri());
    pj_str_t pjFrom = pj_str((char*) from.c_str());

    // Get the received header
    std::string received(account->getReceivedParameter());

    // Get the contact header
    std::string contact = account->getContactHeader();
    pj_str_t pjContact = pj_str((char*) contact.c_str());

    if (not received.empty() and received != account->getPublishedAddress()) {
        // Set received parameter string to empty in order to avoid creating new transport for each register
        account->setReceivedParameter("");
        DEBUG("Creating transport on random port because we have rx param %s", received.c_str());
        // Explicitly set the bound address port to 0 so that pjsip determines a random port by itself
        account->transport_= sipTransport.createUdpTransport(account->getLocalInterface(), 0, received, account->getRPort());
        account->setRPort(-1);
        if (account->transport_ == NULL)
            ERROR("Could not create new udp transport with public address: %s:%d", received.c_str(), account->getLocalPort());
    }

    if (pjsip_regc_init(regc, &pjSrv, &pjFrom, &pjFrom, 1, &pjContact, account->getRegistrationExpire()) != PJ_SUCCESS)
        throw VoipLinkException("Unable to initialize account registration structure");

    if (not account->getServiceRoute().empty())
        pjsip_regc_set_route_set(regc, sip_utils::createRouteSet(account->getServiceRoute(), pool_));

    pjsip_regc_set_credentials(regc, account->getCredentialCount(), account->getCredInfo());

    pjsip_hdr hdr_list;
    pj_list_init(&hdr_list);
    std::string useragent(account->getUserAgentName());
    pj_str_t pJuseragent = pj_str((char*) useragent.c_str());
    const pj_str_t STR_USER_AGENT = { (char*) "User-Agent", 10 };

    pjsip_generic_string_hdr *h = pjsip_generic_string_hdr_create(pool_, &STR_USER_AGENT, &pJuseragent);
    pj_list_push_back(&hdr_list, (pjsip_hdr*) h);
    pjsip_regc_add_headers(regc, &hdr_list);

    pjsip_tx_data *tdata;

    if (pjsip_regc_register(regc, PJ_TRUE, &tdata) != PJ_SUCCESS)
        throw VoipLinkException("Unable to initialize transaction data for account registration");

    if (pjsip_regc_set_transport(regc, sipTransport.initTransportSelector(account->transport_, pool_)) != PJ_SUCCESS)
        throw VoipLinkException("Unable to set transport");

    // decrease transport's ref count, counter incrementation is managed when acquiring transport
    pjsip_transport_dec_ref(account->transport_);

    // pjsip_regc_send increment the transport ref count by one,
    if (pjsip_regc_send(regc, tdata) != PJ_SUCCESS)
        throw VoipLinkException("Unable to send account registration request");

    // Decrease transport's ref count, since coresponding reference counter decrementation
    // is performed in pjsip_regc_destroy. This function is never called in SFLphone as the
    // regc data structure is permanently associated to the account at first registration.
    pjsip_transport_dec_ref(account->transport_);

    account->setRegistrationInfo(regc);

    // start the periodic registration request based on Expire header
    // account determines itself if a keep alive is required
    if (account->isKeepAliveEnabled())
        account->startKeepAliveTimer();
}

void SIPVoIPLink::sendUnregister(Account *a)
{
    SIPAccount *account = dynamic_cast<SIPAccount *>(a);

    // This may occurs if account failed to register and is in state INVALID
    if (!account->isRegistered()) {
        account->setRegistrationState(UNREGISTERED);
        return;
    }

    // Make sure to cancel any ongoing timers before unregister
    account->stopKeepAliveTimer();

    pjsip_regc *regc = account->getRegistrationInfo();

    if (!regc)
        throw VoipLinkException("Registration structure is NULL");

    pjsip_tx_data *tdata = NULL;

    if (pjsip_regc_unregister(regc, &tdata) != PJ_SUCCESS)
        throw VoipLinkException("Unable to unregister sip account");

    if (pjsip_regc_send(regc, tdata) != PJ_SUCCESS)
        throw VoipLinkException("Unable to send request to unregister sip account");

    account->setRegister(false);
}

void SIPVoIPLink::registerKeepAliveTimer(pj_timer_entry &timer, pj_time_val &delay)
{
    DEBUG("Register new keep alive timer %d with delay %d", timer.id, delay.sec);

    if (timer.id == -1)
        WARN("Timer already scheduled");

    switch (pjsip_endpt_schedule_timer(endpt_, &timer, &delay)) {
        case PJ_SUCCESS:
            break;
        default:
            ERROR("Could not schedule new timer in pjsip endpoint");
            /* fallthrough */
        case PJ_EINVAL:
            ERROR("Invalid timer or delay entry");
            break;
        case PJ_EINVALIDOP:
            ERROR("Invalid timer entry, maybe already scheduled");
            break;
    }
}

void SIPVoIPLink::cancelKeepAliveTimer(pj_timer_entry& timer)
{
    pjsip_endpt_cancel_timer(endpt_, &timer);
}

bool isValidIpAddress(const std::string &address)
{
    size_t pos = address.find(":");
    std::string address_without_port(address);
    if (pos != std::string::npos)
        address_without_port = address.substr(0, pos);

    DEBUG("Testing address %s", address_without_port.c_str());
    struct sockaddr_in sa;
    int result = inet_pton(AF_INET, address_without_port.data(), &(sa.sin_addr));
    return result != 0;
}

/**
 * This function look for '@' and replace the second part with the corresponding ip address (when possible)
 */
std::string resolvDns(const std::string& url)
{
   size_t pos;
   if ((pos = url.find("@")) == std::string::npos) {
      return url;
   }
   std::string hostname = url.substr(pos+1);

   int i;
   struct hostent *he;
   struct in_addr **addr_list;

   if ((he = gethostbyname(hostname.c_str())) == NULL) {
      return url;
   }

   addr_list = (struct in_addr **)he->h_addr_list;
   std::list<std::string> ipList;

   for(i = 0; addr_list[i] != NULL; i++) {
      ipList.push_back(inet_ntoa(*addr_list[i]));
   }

   if (ipList.size() > 0 && ipList.front().size() > 7 )
      return url.substr(0,pos+1)+ipList.front();
   else
      return hostname;
}

Call *SIPVoIPLink::newOutgoingCall(const std::string& id, const std::string& toUrl)
{
    DEBUG("New outgoing call to %s", toUrl.c_str());
    std::string toCpy = toUrl;
    std::string resolvedUrl = resolvDns(toUrl);
    DEBUG("URL resolved to %s", resolvedUrl.c_str());

    sip_utils::stripSipUriPrefix(toCpy);

    const bool IPToIP = isValidIpAddress(toCpy);
    Manager::instance().setIPToIPForCall(id, IPToIP);

    if (IPToIP) {
        Manager::instance().associateCallToAccount(id, SIPAccount::IP2IP_PROFILE);
        return SIPNewIpToIpCall(id, resolvedUrl);
    } else {
        return newRegisteredAccountCall(id, resolvedUrl);
    }
}

Call *SIPVoIPLink::SIPNewIpToIpCall(const std::string& id, const std::string& to)
{
    DEBUG("New IP to IP call to %s", to.c_str());

    SIPAccount *account = Manager::instance().getIP2IPAccount();

    if (!account)
        throw VoipLinkException("Could not retrieve default account for IP2IP call");

    SIPCall *call = new SIPCall(id, Call::OUTGOING, cp_);

    call->setIPToIP(true);
    call->initRecFilename(to);

    std::string localAddress(SipTransport::getInterfaceAddrFromName(account->getLocalInterface()));

    setCallMediaLocal(call, localAddress);

    std::string toUri = account->getToUri(to);
    call->setPeerNumber(toUri);

    sfl::AudioCodec* ac = dynamic_cast<sfl::AudioCodec*>(Manager::instance().audioCodecFactory.instantiateCodec(PAYLOAD_CODEC_ULAW));

    if (!ac) {
        delete call;
        throw VoipLinkException("Could not instantiate codec");
    }
    // Audio Rtp Session must be initialized before creating initial offer in SDP session
    // since SDES require crypto attribute.
    call->getAudioRtp().initConfig();
    call->getAudioRtp().initSession();
    call->getAudioRtp().initLocalCryptoInfo();
    call->getAudioRtp().start(ac);

    // Building the local SDP offer
    call->getLocalSDP()->setLocalIP(localAddress);
    call->getLocalSDP()->createOffer(account->getActiveAudioCodecs(), account->getActiveVideoCodecs());

    if (!SIPStartCall(call)) {
        delete call;
        throw VoipLinkException("Could not create new call");
    }

    return call;
}

Call *SIPVoIPLink::newRegisteredAccountCall(const std::string& id, const std::string& toUrl)
{
    DEBUG("UserAgent: New registered account call to %s", toUrl.c_str());

    SIPAccount *account = dynamic_cast<SIPAccount *>(Manager::instance().getAccount(Manager::instance().getAccountFromCall(id)));

    if (account == NULL) // TODO: We should investigate how we could get rid of this error and create a IP2IP call instead
        throw VoipLinkException("Could not get account for this call");

    SIPCall* call = new SIPCall(id, Call::OUTGOING, cp_);

    // If toUri is not a well formatted sip URI, use account information to process it
    std::string toUri;

    if (toUrl.find("sip:") != std::string::npos or
        toUrl.find("sips:") != std::string::npos)
        toUri = toUrl;
    else
        toUri = account->getToUri(toUrl);

    call->setPeerNumber(toUri);
    std::string localAddr(SipTransport::getInterfaceAddrFromName(account->getLocalInterface()));
    setCallMediaLocal(call, localAddr);

    // May use the published address as well
    std::string addrSdp = account->isStunEnabled() ?
    account->getPublishedAddress() :
    SipTransport::getInterfaceAddrFromName(account->getLocalInterface());

    // Initialize the session using ULAW as default codec in case of early media
    // The session should be ready to receive media once the first INVITE is sent, before
    // the session initialization is completed
    sfl::AudioCodec* ac = dynamic_cast<sfl::AudioCodec*>(Manager::instance().audioCodecFactory.instantiateCodec(PAYLOAD_CODEC_ULAW));

    if (ac == NULL) {
        delete call;
        throw VoipLinkException("Could not instantiate codec for early media");
    }

    try {
        call->getAudioRtp().initConfig();
        call->getAudioRtp().initSession();
        call->getAudioRtp().initLocalCryptoInfo();
        call->getAudioRtp().start(ac);
    } catch (...) {
        delete call;
        throw VoipLinkException("Could not start rtp session for early media");
    }

    call->initRecFilename(toUrl);

    call->getLocalSDP()->setLocalIP(addrSdp);
    call->getLocalSDP()->createOffer(account->getActiveAudioCodecs(), account->getActiveVideoCodecs());

    if (!SIPStartCall(call)) {
        delete call;
        throw VoipLinkException("Could not send outgoing INVITE request for new call");
    }

    return call;
}

void
SIPVoIPLink::answer(Call *call)
{
    if (!call)
        return;
    call->answer();
}

void
SIPVoIPLink::hangup(const std::string& id)
{
    SIPCall* call = getSIPCall(id);

    std::string account_id(Manager::instance().getAccountFromCall(id));
    SIPAccount *account = dynamic_cast<SIPAccount *>(Manager::instance().getAccount(account_id));

    if (account == NULL)
        throw VoipLinkException("Could not find account for this call");

    pjsip_inv_session *inv = call->inv;

    if (inv == NULL)
        throw VoipLinkException("No invite session for this call");

    // Looks for sip routes
    if (not account->getServiceRoute().empty()) {
        pjsip_route_hdr *route_set = sip_utils::createRouteSet(account->getServiceRoute(), inv->pool);
        pjsip_dlg_set_route_set(inv->dlg, route_set);
    }

    pjsip_tx_data *tdata = NULL;

    // User hangup current call. Notify peer
    if (pjsip_inv_end_session(inv, 404, NULL, &tdata) != PJ_SUCCESS || !tdata)
        return;

    if (pjsip_inv_send_msg(inv, tdata) != PJ_SUCCESS)
        return;

    // Make sure user data is NULL in callbacks
    inv->mod_data[mod_ua_.id] = NULL;

    if (Manager::instance().isCurrentCall(id))
        call->getAudioRtp().stop();

    removeCall(id);
}

void
SIPVoIPLink::peerHungup(const std::string& id)
{
    SIPCall* call = getSIPCall(id);

    // User hangup current call. Notify peer
    pjsip_tx_data *tdata = NULL;

    if (pjsip_inv_end_session(call->inv, 404, NULL, &tdata) != PJ_SUCCESS || !tdata)
        return;

    if (pjsip_inv_send_msg(call->inv, tdata) != PJ_SUCCESS)
        return;

    // Make sure user data is NULL in callbacks
    call->inv->mod_data[mod_ua_.id ] = NULL;

    if (Manager::instance().isCurrentCall(id))
        call->getAudioRtp().stop();

    removeCall(id);
}

void
SIPVoIPLink::onhold(const std::string& id)
{
    SIPCall *call = getSIPCall(id);
    call->setState(Call::HOLD);
    call->getAudioRtp().saveLocalContext();
    call->getAudioRtp().stop();
#ifdef SFL_VIDEO
    call->getVideoRtp().stop();
#endif

    Sdp *sdpSession = call->getLocalSDP();

    if (!sdpSession)
        throw VoipLinkException("Could not find sdp session");

    sdpSession->removeAttributeFromLocalAudioMedia("sendrecv");
    sdpSession->removeAttributeFromLocalAudioMedia("sendonly");
    sdpSession->addAttributeToLocalAudioMedia("sendonly");

#ifdef SFL_VIDEO
    sdpSession->removeAttributeFromLocalVideoMedia("sendrecv");
    sdpSession->removeAttributeFromLocalVideoMedia("inactive");
    sdpSession->addAttributeToLocalVideoMedia("inactive");
#endif

    SIPSessionReinvite(call);
}

void
SIPVoIPLink::offhold(const std::string& id)
{
    SIPCall *call = getSIPCall(id);

    Sdp *sdpSession = call->getLocalSDP();

    if (sdpSession == NULL)
        throw VoipLinkException("Could not find sdp session");

    try {
        int pl = PAYLOAD_CODEC_ULAW;
        sfl::Codec *sessionMedia = sdpSession->getSessionAudioMedia();

        if (sessionMedia)
            pl = sessionMedia->getPayloadType();

        // Create a new instance for this codec
        sfl::AudioCodec* ac = dynamic_cast<sfl::AudioCodec*>(Manager::instance().audioCodecFactory.instantiateCodec(pl));

        if (ac == NULL)
            throw VoipLinkException("Could not instantiate codec");

        call->getAudioRtp().initConfig();
        call->getAudioRtp().initSession();
        call->getAudioRtp().restoreLocalContext();
        call->getAudioRtp().initLocalCryptoInfoOnOffHold();
        call->getAudioRtp().start(ac);
    } catch (const SdpException &e) {
        ERROR("%s", e.what());
    } catch (...) {
        throw VoipLinkException("Could not create audio rtp session");
    }

    sdpSession->removeAttributeFromLocalAudioMedia("sendrecv");
    sdpSession->removeAttributeFromLocalAudioMedia("sendonly");
    sdpSession->addAttributeToLocalAudioMedia("sendrecv");

#ifdef SFL_VIDEO
    sdpSession->removeAttributeFromLocalVideoMedia("sendrecv");
    sdpSession->removeAttributeFromLocalVideoMedia("sendonly");
    sdpSession->addAttributeToLocalVideoMedia("sendrecv");
#endif

    if (SIPSessionReinvite(call) == PJ_SUCCESS)
        call->setState(Call::ACTIVE);
}

#if HAVE_INSTANT_MESSAGING
void SIPVoIPLink::sendTextMessage(const std::string &callID,
                                  const std::string &message,
                                  const std::string &from)
{
    using namespace sfl::InstantMessaging;
    SIPCall *call;

    try {
        call = getSIPCall(callID);
    } catch (const VoipLinkException &e) {
        return;
    }

    /* Send IM message */
    UriList list;
    UriEntry entry;
    entry[sfl::IM_XML_URI] = std::string("\"" + from + "\"");  // add double quotes for xml formating
    list.push_front(entry);
    send_sip_message(call->inv, callID, appendUriList(message, list));
}
#endif // HAVE_INSTANT_MESSAGING

bool
SIPVoIPLink::transferCommon(SIPCall *call, pj_str_t *dst)
{
    if (!call or !call->inv)
        return false;

    pjsip_evsub_user xfer_cb;
    pj_bzero(&xfer_cb, sizeof(xfer_cb));
    xfer_cb.on_evsub_state = &transfer_client_cb;

    pjsip_evsub *sub;

    if (pjsip_xfer_create_uac(call->inv->dlg, &xfer_cb, &sub) != PJ_SUCCESS)
        return false;

    /* Associate this voiplink of call with the client subscription
     * We can not just associate call with the client subscription
     * because after this function, we can no find the cooresponding
     * voiplink from the call any more. But the voiplink is useful!
     */
    pjsip_evsub_set_mod_data(sub, mod_ua_.id, this);

    /*
     * Create REFER request.
     */
    pjsip_tx_data *tdata;

    if (pjsip_xfer_initiate(sub, dst, &tdata) != PJ_SUCCESS)
        return false;

    // Put SIP call id in map in order to retrieve call during transfer callback
    std::string callidtransfer(call->inv->dlg->call_id->id.ptr, call->inv->dlg->call_id->id.slen);
    transferCallID[callidtransfer] = call->getCallId();

    /* Send. */
    if (pjsip_xfer_send_request(sub, tdata) != PJ_SUCCESS)
        return false;

    return true;
}

void
SIPVoIPLink::transfer(const std::string& id, const std::string& to)
{
    SIPCall *call = getSIPCall(id);
    call->stopRecording();

    std::string account_id(Manager::instance().getAccountFromCall(id));
    SIPAccount *account = dynamic_cast<SIPAccount *>(Manager::instance().getAccount(account_id));

    if (account == NULL)
        throw VoipLinkException("Could not find account");

    std::string toUri;
    pj_str_t dst = { 0, 0 };

    if (to.find("@") == std::string::npos) {
        toUri = account->getToUri(to);
        pj_cstr(&dst, toUri.c_str());
    }

    if (!transferCommon(call, &dst))
        throw VoipLinkException("Couldn't transfer");
}

bool SIPVoIPLink::attendedTransfer(const std::string& id, const std::string& to)
{
    SIPCall *call = getSIPCall(to);
    if (!call->inv or !call->inv->dlg)
        throw VoipLinkException("Couldn't get invite dialog");
    pjsip_dialog *target_dlg = call->inv->dlg;
    pjsip_uri *uri = (pjsip_uri*) pjsip_uri_get_uri(target_dlg->remote.info->uri);

    char str_dest_buf[PJSIP_MAX_URL_SIZE * 2] = { '<' };
    pj_str_t dst = { str_dest_buf, 1 };

    dst.slen += pjsip_uri_print(PJSIP_URI_IN_REQ_URI, uri, str_dest_buf+1, sizeof(str_dest_buf)-1);
    dst.slen += pj_ansi_snprintf(str_dest_buf + dst.slen,
    sizeof(str_dest_buf) - dst.slen,
    "?"
    "Replaces=%.*s"
    "%%3Bto-tag%%3D%.*s"
    "%%3Bfrom-tag%%3D%.*s>",
    (int)target_dlg->call_id->id.slen,
    target_dlg->call_id->id.ptr,
    (int)target_dlg->remote.info->tag.slen,
    target_dlg->remote.info->tag.ptr,
    (int)target_dlg->local.info->tag.slen,
    target_dlg->local.info->tag.ptr);

    return transferCommon(getSIPCall(id), &dst);
}

void
SIPVoIPLink::refuse(const std::string& id)
{
    SIPCall *call = getSIPCall(id);

    if (!call->isIncoming() or call->getConnectionState() == Call::CONNECTED or !call->inv)
        return;

    call->getAudioRtp().stop();

    pjsip_tx_data *tdata;
    if (pjsip_inv_end_session(call->inv, PJSIP_SC_DECLINE, NULL, &tdata) != PJ_SUCCESS)
        return;

    if (pjsip_inv_send_msg(call->inv, tdata) != PJ_SUCCESS)
        return;

    // Make sure the pointer is NULL in callbacks
    call->inv->mod_data[mod_ua_.id] = NULL;

    removeCall(id);
}

std::string
SIPVoIPLink::getCurrentVideoCodecName(Call *call) const
{
    return dynamic_cast<SIPCall*>(call)->getLocalSDP()->getSessionVideoCodec();
}

std::string
SIPVoIPLink::getCurrentAudioCodecName(Call *call) const
{
    return dynamic_cast<SIPCall*>(call)->getLocalSDP()->getAudioCodecName();
}

/* Only use this macro with string literals or character arrays, will not work
 * as expected with char pointers */
#define CONST_PJ_STR(X) {(char *) (X), ARRAYSIZE(X) - 1}

namespace {
void sendSIPInfo(const SIPCall &call, const char *const body, const char *const subtype)
{
    pj_str_t methodName = CONST_PJ_STR("INFO");
    pjsip_method method;
    pjsip_method_init_np(&method, &methodName);

    /* Create request message. */
    pjsip_tx_data *tdata;

    if (pjsip_dlg_create_request(call.inv->dlg, &method, -1, &tdata) != PJ_SUCCESS) {
        ERROR("Could not create dialog");
        return;
    }

    /* Create "application/<subtype>" message body. */
    pj_str_t content;
    pj_cstr(&content, body);
    const pj_str_t type = CONST_PJ_STR("application");
    pj_str_t pj_subtype;
    pj_cstr(&pj_subtype, subtype);
    tdata->msg->body = pjsip_msg_body_create(tdata->pool, &type, &pj_subtype, &content);

    if (tdata->msg->body == NULL)
        pjsip_tx_data_dec_ref(tdata);
    else
        pjsip_dlg_send_request(call.inv->dlg, tdata, mod_ua_.id, NULL);
}

void
dtmfSend(SIPCall &call, char code, const std::string &dtmf)
{
    if (dtmf == SIPAccount::OVERRTP_STR) {
        call.getAudioRtp().sendDtmfDigit(code - '0');
        return;
    } else if (dtmf != SIPAccount::SIPINFO_STR) {
        WARN("SIPVoIPLink: Unknown DTMF type %s, defaulting to %s instead",
             dtmf.c_str(), SIPAccount::SIPINFO_STR);
    } // else : dtmf == SIPINFO

    int duration = Manager::instance().voipPreferences.getPulseLength();
    char dtmf_body[1000];
    snprintf(dtmf_body, sizeof dtmf_body - 1, "Signal=%c\r\nDuration=%d\r\n", code, duration);
    sendSIPInfo(call, dtmf_body, "dtmf-relay");
}
}

#ifdef SFL_VIDEO
void
SIPVoIPLink::requestFastPictureUpdate(const std::string &callID)
{
    SIPCall *call;
    try {
         call = SIPVoIPLink::instance()->getSIPCall(callID);
    } catch (const VoipLinkException &e) {
        ERROR("%s", e.what());
        return;
    }

    const char * const BODY =
        "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
        "<media_control><vc_primitive><to_encoder>"
        "<picture_fast_update/>"
        "</to_encoder></vc_primitive></media_control>";

    DEBUG("Sending video keyframe request via SIP INFO");
    sendSIPInfo(*call, BODY, "media_control+xml");
}
#endif

void
SIPVoIPLink::carryingDTMFdigits(const std::string& id, char code)
{
    std::string accountID(Manager::instance().getAccountFromCall(id));
    SIPAccount *account = dynamic_cast<SIPAccount*>(Manager::instance().getAccount(accountID));
    if (!account)
        return;

    try {
        SIPCall *call(getSIPCall(id));
        dtmfSend(*call, code, account->getDtmfType());
    } catch (const VoipLinkException &e) {
        // don't do anything if call doesn't exist
    }
}


bool
SIPVoIPLink::SIPStartCall(SIPCall *call)
{
    std::string id(Manager::instance().getAccountFromCall(call->getCallId()));
    SIPAccount *account = dynamic_cast<SIPAccount *>(Manager::instance().getAccount(id));

    if (account == NULL) {
        ERROR("Account is NULL in SIPStartCall");
        return false;
    }

    std::string toUri(call->getPeerNumber()); // expecting a fully well formed sip uri

    pj_str_t pjTo = pj_str((char*) toUri.c_str());

    // Create the from header
    std::string from(account->getFromUri());
    pj_str_t pjFrom = pj_str((char*) from.c_str());

    // Get the contact header
    std::string contact(account->getContactHeader());
    pj_str_t pjContact = pj_str((char*) contact.c_str());

    pjsip_dialog *dialog = NULL;

    if (pjsip_dlg_create_uac(pjsip_ua_instance(), &pjFrom, &pjContact, &pjTo, NULL, &dialog) != PJ_SUCCESS) {
        ERROR("Unable to sip create dialogs for user agent client");
        return false;
    }

    if (pjsip_inv_create_uac(dialog, call->getLocalSDP()->getLocalSdpSession(), 0, &call->inv) != PJ_SUCCESS) {
        ERROR("Unable to create invite session for user agent client");
        return false;
    }

    if (not account->getServiceRoute().empty())
        pjsip_dlg_set_route_set(dialog, sip_utils::createRouteSet(account->getServiceRoute(), call->inv->pool));

    if (account->hasCredentials() and pjsip_auth_clt_set_credentials(&dialog->auth_sess, account->getCredentialCount(), account->getCredInfo()) != PJ_SUCCESS) {
        ERROR("Could not initialize credentials for invite session authentication");
        return false;
    }

    call->inv->mod_data[mod_ua_.id] = call;

    pjsip_tx_data *tdata;

    if (pjsip_inv_invite(call->inv, &tdata) != PJ_SUCCESS) {
        ERROR("Could not initialize invite messager for this call");
        return false;
    }

    pjsip_tpselector *tp = sipTransport.initTransportSelector(account->transport_, call->inv->pool);

    if (pjsip_dlg_set_transport(dialog, tp) != PJ_SUCCESS) {
        ERROR("Unable to associate transport fir invite session dialog");
        return false;
    }

    if (pjsip_inv_send_msg(call->inv, tdata) != PJ_SUCCESS) {
        ERROR("Unable to send invite message for this call");
        return false;
    }

    call->setConnectionState(Call::PROGRESSING);
    call->setState(Call::ACTIVE);
    addCall(call);

    return true;
}

void
SIPVoIPLink::SIPCallServerFailure(SIPCall *call)
{
    std::string id(call->getCallId());
    Manager::instance().callFailure(id);
    removeCall(id);
}

void
SIPVoIPLink::SIPCallClosed(SIPCall *call)
{
    std::string id(call->getCallId());

    if (Manager::instance().isCurrentCall(id))
        call->getAudioRtp().stop();

    Manager::instance().peerHungupCall(id);
    removeCall(id);
}

void
SIPVoIPLink::SIPCallAnswered(SIPCall *call, pjsip_rx_data * /*rdata*/)
{
    if (call->getConnectionState() != Call::CONNECTED) {
        call->setConnectionState(Call::CONNECTED);
        call->setState(Call::ACTIVE);
        Manager::instance().peerAnsweredCall(call->getCallId());
    }
}


SIPCall*
SIPVoIPLink::getSIPCall(const std::string& id)
{
    SIPCall *result = dynamic_cast<SIPCall*>(getCall(id));

    if (result == NULL)
        throw VoipLinkException("Could not find SIPCall " + id);

    return result;
}

///////////////////////////////////////////////////////////////////////////////
// Private functions
///////////////////////////////////////////////////////////////////////////////

namespace {
int SIPSessionReinvite(SIPCall *call)
{
    pjmedia_sdp_session *local_sdp = call->getLocalSDP()->getLocalSdpSession();
    pjsip_tx_data *tdata;
    if (local_sdp && pjsip_inv_reinvite(call->inv, NULL, local_sdp, &tdata) == PJ_SUCCESS)
        return pjsip_inv_send_msg(call->inv, tdata);

    return !PJ_SUCCESS;
}

void invite_session_state_changed_cb(pjsip_inv_session *inv, pjsip_event *ev)
{
    if (!inv)
        return;
    SIPCall *call = static_cast<SIPCall*>(inv->mod_data[mod_ua_.id]);

    if (call == NULL)
        return;

    if (ev and inv->state != PJSIP_INV_STATE_CONFIRMED) {
        // Update UI with the current status code and description
        pjsip_transaction * tsx = ev->body.tsx_state.tsx;
        int statusCode = tsx ? tsx->status_code : 404;

        if (statusCode) {
            const pj_str_t * description = pjsip_get_status_text(statusCode);
            std::string desc(description->ptr, description->slen);
            CallManager *cm = Manager::instance().getDbusManager()->getCallManager();
            cm->sipCallStateChanged(call->getCallId(), desc, statusCode);
        }
    }

    SIPVoIPLink *link = SIPVoIPLink::instance();
    if (inv->state == PJSIP_INV_STATE_EARLY and ev and ev->body.tsx_state.tsx and
            ev->body.tsx_state.tsx->role == PJSIP_ROLE_UAC) {
        call->setConnectionState(Call::RINGING);
        Manager::instance().peerRingingCall(call->getCallId());
    } else if (inv->state == PJSIP_INV_STATE_CONFIRMED and ev) {
        // After we sent or received a ACK - The connection is established
        link->SIPCallAnswered(call, ev->body.tsx_state.src.rdata);
    } else if (inv->state == PJSIP_INV_STATE_DISCONNECTED) {
        std::string accId(Manager::instance().getAccountFromCall(call->getCallId()));

        switch (inv->cause) {
                // The call terminates normally - BYE / CANCEL
            case PJSIP_SC_OK:
            case PJSIP_SC_REQUEST_TERMINATED:
                link->SIPCallClosed(call);
                break;
            case PJSIP_SC_DECLINE:
                if (inv->role != PJSIP_ROLE_UAC)
                    break;

            case PJSIP_SC_NOT_FOUND:
            case PJSIP_SC_REQUEST_TIMEOUT:
            case PJSIP_SC_NOT_ACCEPTABLE_HERE:  /* no compatible codecs */
            case PJSIP_SC_NOT_ACCEPTABLE_ANYWHERE:
            case PJSIP_SC_UNSUPPORTED_MEDIA_TYPE:
            case PJSIP_SC_UNAUTHORIZED:
            case PJSIP_SC_FORBIDDEN:
            case PJSIP_SC_REQUEST_PENDING:
            case PJSIP_SC_ADDRESS_INCOMPLETE:
            default:
                link->SIPCallServerFailure(call);
                break;
        }
    }
}

void sdp_request_offer_cb(pjsip_inv_session *inv, const pjmedia_sdp_session *offer)
{
    if (!inv)
        return;
    SIPCall *call = static_cast<SIPCall*>(inv->mod_data[mod_ua_.id]);

    if (!call)
        return;

    std::string accId(Manager::instance().getAccountFromCall(call->getCallId()));
    SIPAccount *account = dynamic_cast<SIPAccount *>(Manager::instance().getAccount(accId));
    if (!account)
        return;

    call->getLocalSDP()->receiveOffer(offer, account->getActiveAudioCodecs(), account->getActiveVideoCodecs());
    call->getLocalSDP()->startNegotiation();

    pjsip_inv_set_sdp_answer(call->inv, call->getLocalSDP()->getLocalSdpSession());
}

void sdp_create_offer_cb(pjsip_inv_session *inv, pjmedia_sdp_session **p_offer)
{
    if (!inv or !p_offer)
        return;
    SIPCall *call = static_cast<SIPCall*>(inv->mod_data[mod_ua_.id]);
    if (!call)
        return;
    std::string accountid(Manager::instance().getAccountFromCall(call->getCallId()));

    SIPAccount *account = dynamic_cast<SIPAccount *>(Manager::instance().getAccount(accountid));

    std::string localAddress(SipTransport::getInterfaceAddrFromName(account->getLocalInterface()));
    std::string addrSdp(localAddress);

    setCallMediaLocal(call, localAddress);

    call->getLocalSDP()->setLocalIP(addrSdp);
    call->getLocalSDP()->createOffer(account->getActiveAudioCodecs(), account->getActiveVideoCodecs());

    *p_offer = call->getLocalSDP()->getLocalSdpSession();
}

// This callback is called after SDP offer/answer session has completed.
void sdp_media_update_cb(pjsip_inv_session *inv, pj_status_t status)
{
    if (!inv)
        return;
    SIPCall *call = static_cast<SIPCall *>(inv->mod_data[mod_ua_.id]);

    if (call == NULL) {
        DEBUG("Call declined by peer, SDP negotiation stopped");
        return;
    }

    if (status != PJ_SUCCESS) {
        WARN("Could not negotiate offer");
        SIPVoIPLink::instance()->hangup(call->getCallId());
        Manager::instance().callFailure(call->getCallId());
        return;
    }

    if (!inv->neg) {
        WARN("No negotiator for this session");
        return;
    }

    // Retreive SDP session for this call
    Sdp *sdpSession = call->getLocalSDP();
    if (!sdpSession) {
        ERROR("No SDP session");
        return;
    }

    // Get active session sessions
    const pjmedia_sdp_session *remote_sdp = 0;
    pjmedia_sdp_neg_get_active_remote(inv->neg, &remote_sdp);

    if (pjmedia_sdp_validate(remote_sdp) != PJ_SUCCESS) {
        ERROR("Invalid remote SDP session");
        return;
    }
    const pjmedia_sdp_session *local_sdp;
    pjmedia_sdp_neg_get_active_local(inv->neg, &local_sdp);
    if (pjmedia_sdp_validate(local_sdp) != PJ_SUCCESS) {
        ERROR("Invalid local SDP session");
        return;
    }

    // Print SDP session
    char buffer[4096];
    memset(buffer, 0, sizeof buffer);
    if (pjmedia_sdp_print(remote_sdp, buffer, sizeof buffer) == -1) {
        ERROR("SDP was too big for buffer");
        return;
    }
    DEBUG("Remote active SDP Session:\n%s", buffer);

    memset(buffer, 0, sizeof buffer);
    if (pjmedia_sdp_print(local_sdp, buffer, sizeof buffer) == -1) {
        ERROR("SDP was too big for buffer");
        return;
    }
    DEBUG("Local active SDP Session:\n%s", buffer);

    // Set active SDP sessions
    sdpSession->setActiveRemoteSdpSession(remote_sdp);
    sdpSession->setActiveLocalSdpSession(local_sdp);

    // Update internal field for
    sdpSession->setMediaTransportInfoFromRemoteSdp();

    call->getAudioRtp().updateDestinationIpAddress();
    call->getAudioRtp().setDtmfPayloadType(sdpSession->getTelephoneEventType());
#ifdef SFL_VIDEO
    Manager::instance().getVideoControls()->stopPreview();
    call->getVideoRtp().updateSDP(*call->getLocalSDP());
    call->getVideoRtp().updateDestination(call->getLocalSDP()->getRemoteIP(), call->getLocalSDP()->getRemoteVideoPort());
    call->getVideoRtp().start();
#endif

    // Get the crypto attribute containing srtp's cryptographic context (keys, cipher)
    CryptoOffer crypto_offer;
    call->getLocalSDP()->getRemoteSdpCryptoFromOffer(remote_sdp, crypto_offer);

#if HAVE_SDES
    bool nego_success = false;

    if (!crypto_offer.empty()) {
        std::vector<sfl::CryptoSuiteDefinition> localCapabilities;

        for (size_t i = 0; i < ARRAYSIZE(sfl::CryptoSuites); ++i)
            localCapabilities.push_back(sfl::CryptoSuites[i]);

        sfl::SdesNegotiator sdesnego(localCapabilities, crypto_offer);

        if (sdesnego.negotiate()) {
            nego_success = true;

            try {
                call->getAudioRtp().setRemoteCryptoInfo(sdesnego);
            } catch (...) {}

            Manager::instance().getDbusManager()->getCallManager()->secureSdesOn(call->getCallId());
        } else {
            ERROR("SDES negotiation failure");
            Manager::instance().getDbusManager()->getCallManager()->secureSdesOff(call->getCallId());
        }
    }
    else {
        DEBUG("No crypto offer available");
    }

    // We did not find any crypto context for this media, RTP fallback
    if (!nego_success && call->getAudioRtp().isSdesEnabled()) {
        ERROR("Negotiation failed but SRTP is enabled, fallback on RTP");
        call->getAudioRtp().stop();
        call->getAudioRtp().setSrtpEnabled(false);

        std::string accountID = Manager::instance().getAccountFromCall(call->getCallId());

        if (dynamic_cast<SIPAccount*>(Manager::instance().getAccount(accountID))->getSrtpFallback())
            call->getAudioRtp().initSession();
    }
#endif // HAVE_SDES

    sfl::AudioCodec *sessionMedia = sdpSession->getSessionAudioMedia();

    if (!sessionMedia)
        return;

    try {
        Manager::instance().audioLayerMutexLock();
        Manager::instance().getAudioDriver()->startStream();
        Manager::instance().audioLayerMutexUnlock();

        int pl = sessionMedia->getPayloadType();

        if (pl != call->getAudioRtp().getSessionMedia()) {
            sfl::AudioCodec *ac = dynamic_cast<sfl::AudioCodec*>(Manager::instance().audioCodecFactory.instantiateCodec(pl));
            if (!ac)
                throw std::runtime_error("Could not instantiate codec");
            call->getAudioRtp().updateSessionMedia(ac);
        }
    } catch (const SdpException &e) {
        ERROR("%s", e.what());
    } catch (const std::exception &rtpException) {
        ERROR("%s", rtpException.what());
    }

}

void outgoing_request_forked_cb(pjsip_inv_session * /*inv*/, pjsip_event * /*e*/)
{}

bool handle_media_control(pjsip_inv_session * inv, pjsip_transaction *tsx, pjsip_event *event)
{
    /*
     * Incoming INFO request for media control.
     */
    const pj_str_t STR_APPLICATION = CONST_PJ_STR("application");
    const pj_str_t STR_MEDIA_CONTROL_XML = CONST_PJ_STR("media_control+xml");
    pjsip_rx_data *rdata = event->body.tsx_state.src.rdata;
    pjsip_msg_body *body = rdata->msg_info.msg->body;

    if (body and body->len and pj_stricmp(&body->content_type.type, &STR_APPLICATION) == 0 and
        pj_stricmp(&body->content_type.subtype, &STR_MEDIA_CONTROL_XML) == 0) {
        pj_str_t control_st;

        /* Apply and answer the INFO request */
        pj_strset(&control_st, (char *) body->data, body->len);
        const pj_str_t PICT_FAST_UPDATE = CONST_PJ_STR("picture_fast_update");

        if (pj_strstr(&control_st, &PICT_FAST_UPDATE)) {
#ifdef SFL_VIDEO
            DEBUG("handling picture fast update request");
            SIPCall *call = static_cast<SIPCall *>(inv->mod_data[mod_ua_.id]);
            if (call)
                call->getVideoRtp().forceKeyFrame();
            pjsip_tx_data *tdata;
            pj_status_t status = pjsip_endpt_create_response(tsx->endpt, rdata,
                                                             PJSIP_SC_OK, NULL, &tdata);
            if (status == PJ_SUCCESS) {
                status = pjsip_tsx_send_msg(tsx, tdata);
                return true;
            }
#else
        (void) inv;
        (void) tsx;
#endif
        }
    }
    return false;
}

void transaction_state_changed_cb(pjsip_inv_session * inv,
                                  pjsip_transaction *tsx, pjsip_event *event)
{
    if (!tsx or !event or !inv or tsx->role != PJSIP_ROLE_UAS or
            tsx->state != PJSIP_TSX_STATE_TRYING)
        return;

    // Handle the refer method
    if (pjsip_method_cmp(&tsx->method, &pjsip_refer_method) == 0) {
        onCallTransfered(inv, event->body.tsx_state.src.rdata);
        return;
    }

    pjsip_tx_data* t_data;
    if (tsx->role == PJSIP_ROLE_UAS and tsx->state == PJSIP_TSX_STATE_TRYING) {
        if (handle_media_control(inv, tsx, event))
            return;
    }

    if (event->body.rx_msg.rdata) {
        pjsip_rx_data *r_data = event->body.rx_msg.rdata;

        if (r_data && r_data->msg_info.msg->line.req.method.id == PJSIP_OTHER_METHOD) {
            std::string request(pjsip_rx_data_get_info(r_data));
            DEBUG("%s", request.c_str());

            if (request.find("NOTIFY") == std::string::npos and
                request.find("INFO") != std::string::npos) {
                pjsip_dlg_create_response(inv->dlg, r_data, PJSIP_SC_OK, NULL, &t_data);
                pjsip_dlg_send_response(inv->dlg, tsx, t_data);
                return;
            }
        }
    }

    if (!event->body.tsx_state.src.rdata)
        return;

    pjsip_rx_data *r_data = event->body.tsx_state.src.rdata;

    // Respond with a 200/OK
    pjsip_dlg_create_response(inv->dlg, r_data, PJSIP_SC_OK, NULL, &t_data);
    pjsip_dlg_send_response(inv->dlg, tsx, t_data);

#if HAVE_INSTANT_MESSAGING
    // Try to determine who is the recipient of the message
    SIPCall *call = static_cast<SIPCall *>(inv->mod_data[mod_ua_.id]);

    if (!call)
        return;

    // Incoming TEXT message

    // Get the message inside the transaction
    if (!r_data->msg_info.msg->body)
        return;
    const char *formattedMsgPtr = static_cast<const char*>(r_data->msg_info.msg->body->data);
    if (!formattedMsgPtr)
        return;
    std::string formattedMessage(formattedMsgPtr, strlen(formattedMsgPtr));

    using namespace sfl::InstantMessaging;

    try {
        // retreive the recipient-list of this message
        std::string urilist = findTextUriList(formattedMessage);
        UriList list = parseXmlUriList(urilist);

        // If no item present in the list, peer is considered as the sender
        std::string from;

        if (list.empty()) {
            from = call->getPeerNumber();
        } else {
            from = list.front()[IM_XML_URI];

            if (from == "Me")
                from = call->getPeerNumber();
        }

        // strip < and > characters in case of an IP address
        if (from[0] == '<' && from[from.size()-1] == '>')
            from = from.substr(1, from.size()-2);

        Manager::instance().incomingMessage(call->getCallId(), from, findTextMessage(formattedMessage));

    } catch (const sfl::InstantMessageException &except) {
        ERROR("%s", except.what());
    }
#endif
}

void update_contact_header(pjsip_regc_cbparam *param, SIPAccount *account)
{
    SIPVoIPLink *siplink = dynamic_cast<SIPVoIPLink *>(account->getVoIPLink());
    if (siplink == NULL) {
        ERROR("Could not find voip link from account");
        return;
    }

    pj_pool_t *pool = pj_pool_create(&cp_->factory, "tmp", 512, 512, NULL);
    if (pool == NULL) {
        ERROR("Could not create temporary memory pool in transport header");
        return;
    }

    if (!param or param->contact_cnt == 0) {
        WARN("SIPVoIPLink: No contact header in registration callback");
        pj_pool_release(pool);
        return;
    }

    pjsip_contact_hdr *contact_hdr = param->contact[0];
    if (!contact_hdr)
        return;

    pjsip_sip_uri *uri = (pjsip_sip_uri*) contact_hdr->uri;
    if (uri == NULL) {
        ERROR("Could not find uri in contact header");
        pj_pool_release(pool);
        return;
    }

    // TODO: make this based on transport type
    // with pjsip_transport_get_default_port_for_type(tp_type);
    if (uri->port == 0) {
        ERROR("Port is 0 in uri");
        uri->port = DEFAULT_SIP_PORT;
    }

    std::string recvContactHost(uri->host.ptr, uri->host.slen);
    std::stringstream ss;
    ss << uri->port;
    std::string recvContactPort = ss.str();

    std::string currentAddress, currentPort;
    siplink->sipTransport.findLocalAddressFromTransport(account->transport_, PJSIP_TRANSPORT_UDP, currentAddress, currentPort);

    bool updateContact = false;
    std::string currentContactHeader = account->getContactHeader();

    size_t foundHost = currentContactHeader.find(recvContactHost);
    if (foundHost == std::string::npos)
        updateContact = true;

    size_t foundPort = currentContactHeader.find(recvContactPort);
    if (foundPort == std::string::npos)
        updateContact = true;

    if (updateContact) {
        DEBUG("Update contact header: %s:%s\n", recvContactHost.c_str(), recvContactPort.c_str());
        account->setContactHeader(recvContactHost, recvContactPort);
        siplink->sendRegister(account);
    }
    pj_pool_release(pool);
}

void lookForReceivedParameter(pjsip_regc_cbparam &param, SIPAccount &account)
{
    if (!param.rdata or !param.rdata->msg_info.via)
        return;
    pj_str_t receivedValue = param.rdata->msg_info.via->recvd_param;

    if (receivedValue.slen) {
        std::string publicIpFromReceived(receivedValue.ptr, receivedValue.slen);
        account.setReceivedParameter(publicIpFromReceived);
    }

    account.setRPort(param.rdata->msg_info.via->rport_param);
}

void processRegistrationError(SIPAccount &account, RegistrationState state)
{
    account.stopKeepAliveTimer();
    account.setRegistrationState(state);
    account.setRegister(false);
    SIPVoIPLink::instance()->sipTransport.shutdownSipTransport(account);
}

void registration_cb(pjsip_regc_cbparam *param)
{
    if (param == NULL) {
        ERROR("registration callback parameter is NULL");
        return;
    }

    SIPAccount *account = static_cast<SIPAccount *>(param->token);
    if (account == NULL) {
        ERROR("account doesn't exist in registration callback");
        return;
    }

    if (account->isContactUpdateEnabled())
        update_contact_header(param, account);

    const pj_str_t *description = pjsip_get_status_text(param->code);

    const std::string accountID = account->getAccountID();

    if (param->code && description) {
        std::string state(description->ptr, description->slen);
        Manager::instance().getDbusManager()->getCallManager()->registrationStateChanged(accountID, state, param->code);
        std::pair<int, std::string> details(param->code, state);
        // TODO: there id a race condition for this ressource when closing the application
        account->setRegistrationStateDetailed(details);
        account->setRegistrationExpire(param->expiration);
    }

#define FAILURE_MESSAGE() ERROR("Could not register account %s with error %d", accountID.c_str(), param->code)

    if (param->status != PJ_SUCCESS) {
        FAILURE_MESSAGE();
        processRegistrationError(*account, ERROR_AUTH);
        return;
    }

    if (param->code < 0 || param->code >= 300) {
        switch (param->code) {
            case PJSIP_SC_MULTIPLE_CHOICES: // 300
            case PJSIP_SC_MOVED_PERMANENTLY: // 301
            case PJSIP_SC_MOVED_TEMPORARILY: // 302
            case PJSIP_SC_USE_PROXY: // 305
            case PJSIP_SC_ALTERNATIVE_SERVICE: // 380
                FAILURE_MESSAGE();
                processRegistrationError(*account, ERROR_GENERIC);
                break;
            case PJSIP_SC_SERVICE_UNAVAILABLE: // 503
                FAILURE_MESSAGE();
                processRegistrationError(*account, ERROR_HOST);
                break;
            case PJSIP_SC_UNAUTHORIZED: // 401
                // Automatically answered by PJSIP
                account->registerVoIPLink();
                break;
            case PJSIP_SC_FORBIDDEN: // 403
            case PJSIP_SC_NOT_FOUND: // 404
                FAILURE_MESSAGE();
                processRegistrationError(*account, ERROR_AUTH);
                break;
            case PJSIP_SC_REQUEST_TIMEOUT: // 408
                FAILURE_MESSAGE();
                processRegistrationError(*account, ERROR_HOST);
                break;
            case PJSIP_SC_INTERVAL_TOO_BRIEF: // 423
                // Expiration Interval Too Brief
                account->doubleRegistrationExpire();
                account->registerVoIPLink();
                account->setRegister(false);
                break;
            case PJSIP_SC_NOT_ACCEPTABLE_ANYWHERE: // 606
                lookForReceivedParameter(*param, *account);
                account->setRegistrationState(ERROR_NOT_ACCEPTABLE);
                account->registerVoIPLink();
                break;
            default:
                FAILURE_MESSAGE();
                processRegistrationError(*account, ERROR_GENERIC);
                break;
        }

    } else {
        lookForReceivedParameter(*param, *account);
        if (account->isRegistered())
            account->setRegistrationState(REGISTERED);
        else {
            account->setRegistrationState(UNREGISTERED);
            SIPVoIPLink::instance()->sipTransport.shutdownSipTransport(*account);
        }
    }

#undef FAILURE_MESSAGE
}

void onCallTransfered(pjsip_inv_session *inv, pjsip_rx_data *rdata)
{
    SIPCall *currentCall = static_cast<SIPCall *>(inv->mod_data[mod_ua_.id]);

    if (currentCall == NULL)
        return;

    static const pj_str_t str_refer_to = { (char*) "Refer-To", 8};
    pjsip_generic_string_hdr *refer_to = static_cast<pjsip_generic_string_hdr*>
                                         (pjsip_msg_find_hdr_by_name(rdata->msg_info.msg, &str_refer_to, NULL));

    if (!refer_to) {
        pjsip_dlg_respond(inv->dlg, rdata, 400, NULL, NULL, NULL);
        return;
    }

    SIPVoIPLink::instance()->newOutgoingCall(Manager::instance().getNewCallID(), std::string(refer_to->hvalue.ptr, refer_to->hvalue.slen));
    Manager::instance().hangupCall(currentCall->getCallId());
}

void transfer_client_cb(pjsip_evsub *sub, pjsip_event *event)
{
    switch (pjsip_evsub_get_state(sub)) {
        case PJSIP_EVSUB_STATE_ACCEPTED:
            if (!event)
                return;
            pj_assert(event->type == PJSIP_EVENT_TSX_STATE && event->body.tsx_state.type == PJSIP_EVENT_RX_MSG);
            break;

        case PJSIP_EVSUB_STATE_TERMINATED:
            pjsip_evsub_set_mod_data(sub, mod_ua_.id, NULL);
            break;

        case PJSIP_EVSUB_STATE_ACTIVE: {
            SIPVoIPLink *link = static_cast<SIPVoIPLink *>(pjsip_evsub_get_mod_data(sub, mod_ua_.id));

            if (!link or !event)
                return;

            pjsip_rx_data* r_data = event->body.rx_msg.rdata;

            if (!r_data)
                return;

            std::string request(pjsip_rx_data_get_info(r_data));

            pjsip_status_line status_line = { 500, *pjsip_get_status_text(500) };

            if (!r_data->msg_info.msg)
                return;
            if (r_data->msg_info.msg->line.req.method.id == PJSIP_OTHER_METHOD and
                request.find("NOTIFY") != std::string::npos) {
                pjsip_msg_body *body = r_data->msg_info.msg->body;

                if (!body)
                    return;

                if (pj_stricmp2(&body->content_type.type, "message") or
                        pj_stricmp2(&body->content_type.subtype, "sipfrag"))
                    return;

                if (pjsip_parse_status_line((char*) body->data, body->len, &status_line) != PJ_SUCCESS)
                    return;
            }

            if (r_data->msg_info.cid)
                return;
            std::string transferID(r_data->msg_info.cid->id.ptr, r_data->msg_info.cid->id.slen);
            SIPCall *call = dynamic_cast<SIPCall *>(link->getCall(transferCallID[transferID]));

            if (!call)
                return;

            if (status_line.code / 100 == 2) {
                pjsip_tx_data *tdata;

                if (!call->inv)
                    return;
                if (pjsip_inv_end_session(call->inv, PJSIP_SC_GONE, NULL, &tdata) == PJ_SUCCESS)
                    pjsip_inv_send_msg(call->inv, tdata);

                Manager::instance().hangupCall(call->getCallId());
                pjsip_evsub_set_mod_data(sub, mod_ua_.id, NULL);
            }

            break;
        }
        default:
            break;
    }
}

namespace {
    unsigned int getRandomPort()
    {
        return ((rand() % 27250) + 5250) * 2;
    }
}

void setCallMediaLocal(SIPCall* call, const std::string &localIP)
{
    std::string account_id(Manager::instance().getAccountFromCall(call->getCallId()));
    SIPAccount *account = dynamic_cast<SIPAccount *>(Manager::instance().getAccount(account_id));
    if (!account)
        return;

    const unsigned int callLocalAudioPort = getRandomPort();

    const unsigned int callLocalExternAudioPort = account->isStunEnabled()
                                            ? account->getStunPort()
                                            : callLocalAudioPort;

    call->setLocalIp(localIP);
    call->setLocalAudioPort(callLocalAudioPort);
    call->getLocalSDP()->setLocalPublishedAudioPort(callLocalExternAudioPort);
#ifdef SFL_VIDEO
    unsigned int callLocalVideoPort = 0;
    do
        callLocalVideoPort = getRandomPort();
    while (callLocalAudioPort == callLocalVideoPort);

    call->setLocalVideoPort(callLocalVideoPort);
    call->getLocalSDP()->setLocalPublishedVideoPort(callLocalVideoPort);
#endif
}
} // end anonymous namespace
