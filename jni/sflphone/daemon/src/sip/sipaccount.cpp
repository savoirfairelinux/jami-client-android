/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
*
*  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "account_schema.h"
#include "sipaccount.h"
#include "sipvoiplink.h"
#include "config/yamlnode.h"
#include "config/yamlemitter.h"
#include "logger.h"
#include "manager.h"
#include <pwd.h>
#include <sstream>
#include <stdlib.h>

#ifdef SFL_VIDEO
#include "video/libav_utils.h"
#endif

const char * const SIPAccount::IP2IP_PROFILE = "IP2IP";
const char * const SIPAccount::OVERRTP_STR = "overrtp";
const char * const SIPAccount::SIPINFO_STR = "sipinfo";

namespace {
    const int MIN_REGISTRATION_TIME = 60;
    const int DEFAULT_REGISTRATION_TIME = 3600;
}

SIPAccount::SIPAccount(const std::string& accountID)
    : Account(accountID, "SIP")
    , transport_(NULL)
    , credentials_()
    , regc_(NULL)
    , bRegister_(false)
    , registrationExpire_(MIN_REGISTRATION_TIME)
    , interface_("default")
    , publishedSameasLocal_(true)
    , publishedIpAddress_()
    , localPort_(DEFAULT_SIP_PORT)
    , publishedPort_(DEFAULT_SIP_PORT)
    , serviceRoute_()
    , tlsListenerPort_(DEFAULT_SIP_TLS_PORT)
    , transportType_(PJSIP_TRANSPORT_UNSPECIFIED)
    , cred_()
    , tlsSetting_()
    , ciphers(100)
    , contactHeader_()
    , contactUpdateEnabled_(false)
    , stunServerName_()
    , stunPort_(PJ_STUN_PORT)
    , dtmfType_(OVERRTP_STR)
    , tlsEnable_("false")
    , tlsCaListFile_()
    , tlsCertificateFile_()
    , tlsPrivateKeyFile_()
    , tlsPassword_()
    , tlsMethod_("TLSv1")
    , tlsCiphers_()
    , tlsServerName_(0, 0)
    , tlsVerifyServer_(true)
    , tlsVerifyClient_(true)
    , tlsRequireClientCertificate_(true)
    , tlsNegotiationTimeoutSec_("2")
    , tlsNegotiationTimeoutMsec_("0")
    , stunServer_("stun.sflphone.org")
    , stunEnabled_(false)
    , srtpEnabled_(false)
    , srtpKeyExchange_("sdes")
    , srtpFallback_(false)
    , zrtpDisplaySas_(true)
    , zrtpDisplaySasOnce_(false)
    , zrtpHelloHash_(true)
    , zrtpNotSuppWarning_(true)
    , registrationStateDetailed_()
    , keepAliveEnabled_(false)
    , keepAliveTimer_()
    , keepAliveTimerActive_(false)
    , link_(SIPVoIPLink::instance())
    , receivedParameter_("")
    , rPort_(-1)
{
    if (isIP2IP())
        alias_ = IP2IP_PROFILE;
}

void SIPAccount::serialize(Conf::YamlEmitter &emitter)
{
    using namespace Conf;
    using std::vector;
    using std::string;
    using std::map;
    MappingNode accountmap(NULL);
    MappingNode srtpmap(NULL);
    MappingNode zrtpmap(NULL);
    MappingNode tlsmap(NULL);

    ScalarNode id(Account::accountID_);
    ScalarNode username(Account::username_);
    ScalarNode alias(Account::alias_);
    ScalarNode hostname(Account::hostname_);
    ScalarNode enable(enabled_);
    ScalarNode type(Account::type_);
    std::stringstream registrationExpireStr;
    registrationExpireStr << registrationExpire_;
    ScalarNode expire(registrationExpireStr.str());
    ScalarNode interface(interface_);
    std::stringstream portstr;
    portstr << localPort_;
    ScalarNode port(portstr.str());
    ScalarNode serviceRoute(serviceRoute_);
    ScalarNode contactUpdateEnabled(contactUpdateEnabled_);
    ScalarNode keepAliveEnabled(keepAliveEnabled_);

    ScalarNode mailbox(mailBox_);
    ScalarNode publishAddr(publishedIpAddress_);
    std::stringstream publicportstr;
    publicportstr << publishedPort_;

    ScalarNode publishPort(publicportstr.str());

    ScalarNode sameasLocal(publishedSameasLocal_);
    ScalarNode audioCodecs(audioCodecStr_);
#ifdef SFL_VIDEO
    SequenceNode videoCodecs(NULL);
    accountmap.setKeyValue(VIDEO_CODECS_KEY, &videoCodecs);
    for (vector<map<string, string> >::iterator i = videoCodecList_.begin(); i != videoCodecList_.end(); ++i) {
        map<string, string> &codec = *i;
        MappingNode *mapNode = new MappingNode(NULL);
        mapNode->setKeyValue(VIDEO_CODEC_NAME, new ScalarNode(codec[VIDEO_CODEC_NAME]));
        mapNode->setKeyValue(VIDEO_CODEC_BITRATE, new ScalarNode(codec[VIDEO_CODEC_BITRATE]));
        mapNode->setKeyValue(VIDEO_CODEC_ENABLED, new ScalarNode(codec[VIDEO_CODEC_ENABLED]));
        videoCodecs.addNode(mapNode);
    }
#endif

    ScalarNode ringtonePath(ringtonePath_);
    ScalarNode ringtoneEnabled(ringtoneEnabled_);
    ScalarNode stunServer(stunServer_);
    ScalarNode stunEnabled(stunEnabled_);
    ScalarNode displayName(displayName_);
    ScalarNode dtmfType(dtmfType_);

    std::stringstream countstr;
    countstr << 0;
    ScalarNode count(countstr.str());

    ScalarNode srtpenabled(srtpEnabled_);
    ScalarNode keyExchange(srtpKeyExchange_);
    ScalarNode rtpFallback(srtpFallback_);

    ScalarNode displaySas(zrtpDisplaySas_);
    ScalarNode displaySasOnce(zrtpDisplaySasOnce_);
    ScalarNode helloHashEnabled(zrtpHelloHash_);
    ScalarNode notSuppWarning(zrtpNotSuppWarning_);

    portstr.str("");
    portstr << tlsListenerPort_;
    ScalarNode tlsport(portstr.str());
    ScalarNode certificate(tlsCertificateFile_);
    ScalarNode calist(tlsCaListFile_);
    ScalarNode ciphersNode(tlsCiphers_);
    ScalarNode tlsenabled(tlsEnable_);
    ScalarNode tlsmethod(tlsMethod_);
    ScalarNode timeout(tlsNegotiationTimeoutSec_);
    ScalarNode tlspassword(tlsPassword_);
    ScalarNode privatekey(tlsPrivateKeyFile_);
    ScalarNode requirecertif(tlsRequireClientCertificate_);
    ScalarNode server(tlsServerName_);
    ScalarNode verifyclient(tlsVerifyServer_);
    ScalarNode verifyserver(tlsVerifyClient_);

    accountmap.setKeyValue(ALIAS_KEY, &alias);
    accountmap.setKeyValue(TYPE_KEY, &type);
    accountmap.setKeyValue(ID_KEY, &id);
    accountmap.setKeyValue(USERNAME_KEY, &username);
    accountmap.setKeyValue(HOSTNAME_KEY, &hostname);
    accountmap.setKeyValue(ACCOUNT_ENABLE_KEY, &enable);
    accountmap.setKeyValue(MAILBOX_KEY, &mailbox);
    accountmap.setKeyValue(Preferences::REGISTRATION_EXPIRE_KEY, &expire);
    accountmap.setKeyValue(INTERFACE_KEY, &interface);
    accountmap.setKeyValue(PORT_KEY, &port);
    accountmap.setKeyValue(STUN_SERVER_KEY, &stunServer);
    accountmap.setKeyValue(STUN_ENABLED_KEY, &stunEnabled);
    accountmap.setKeyValue(PUBLISH_ADDR_KEY, &publishAddr);
    accountmap.setKeyValue(PUBLISH_PORT_KEY, &publishPort);
    accountmap.setKeyValue(SAME_AS_LOCAL_KEY, &sameasLocal);
    accountmap.setKeyValue(SERVICE_ROUTE_KEY, &serviceRoute);
    accountmap.setKeyValue(UPDATE_CONTACT_HEADER_KEY, &contactUpdateEnabled);
    accountmap.setKeyValue(DTMF_TYPE_KEY, &dtmfType);
    accountmap.setKeyValue(DISPLAY_NAME_KEY, &displayName);
    accountmap.setKeyValue(AUDIO_CODECS_KEY, &audioCodecs);
    accountmap.setKeyValue(RINGTONE_PATH_KEY, &ringtonePath);
    accountmap.setKeyValue(RINGTONE_ENABLED_KEY, &ringtoneEnabled);
    accountmap.setKeyValue(KEEP_ALIVE_ENABLED, &keepAliveEnabled);

    accountmap.setKeyValue(SRTP_KEY, &srtpmap);
    srtpmap.setKeyValue(SRTP_ENABLE_KEY, &srtpenabled);
    srtpmap.setKeyValue(KEY_EXCHANGE_KEY, &keyExchange);
    srtpmap.setKeyValue(RTP_FALLBACK_KEY, &rtpFallback);

    accountmap.setKeyValue(ZRTP_KEY, &zrtpmap);
    zrtpmap.setKeyValue(DISPLAY_SAS_KEY, &displaySas);
    zrtpmap.setKeyValue(DISPLAY_SAS_ONCE_KEY, &displaySasOnce);
    zrtpmap.setKeyValue(HELLO_HASH_ENABLED_KEY, &helloHashEnabled);
    zrtpmap.setKeyValue(NOT_SUPP_WARNING_KEY, &notSuppWarning);

    SequenceNode credentialseq(NULL);
    accountmap.setKeyValue(CRED_KEY, &credentialseq);

    std::vector<std::map<std::string, std::string> >::const_iterator it;

    for (it = credentials_.begin(); it != credentials_.end(); ++it) {
        std::map<std::string, std::string> cred = *it;
        MappingNode *map = new MappingNode(NULL);
        map->setKeyValue(CONFIG_ACCOUNT_USERNAME, new ScalarNode(cred[CONFIG_ACCOUNT_USERNAME]));
        map->setKeyValue(CONFIG_ACCOUNT_PASSWORD, new ScalarNode(cred[CONFIG_ACCOUNT_PASSWORD]));
        map->setKeyValue(CONFIG_ACCOUNT_REALM, new ScalarNode(cred[CONFIG_ACCOUNT_REALM]));
        credentialseq.addNode(map);
    }

    accountmap.setKeyValue(TLS_KEY, &tlsmap);
    tlsmap.setKeyValue(TLS_PORT_KEY, &tlsport);
    tlsmap.setKeyValue(CERTIFICATE_KEY, &certificate);
    tlsmap.setKeyValue(CALIST_KEY, &calist);
    tlsmap.setKeyValue(CIPHERS_KEY, &ciphersNode);
    tlsmap.setKeyValue(TLS_ENABLE_KEY, &tlsenabled);
    tlsmap.setKeyValue(METHOD_KEY, &tlsmethod);
    tlsmap.setKeyValue(TIMEOUT_KEY, &timeout);
    tlsmap.setKeyValue(TLS_PASSWORD_KEY, &tlspassword);
    tlsmap.setKeyValue(PRIVATE_KEY_KEY, &privatekey);
    tlsmap.setKeyValue(REQUIRE_CERTIF_KEY, &requirecertif);
    tlsmap.setKeyValue(SERVER_KEY, &server);
    tlsmap.setKeyValue(VERIFY_CLIENT_KEY, &verifyclient);
    tlsmap.setKeyValue(VERIFY_SERVER_KEY, &verifyserver);

    try {
        emitter.serializeAccount(&accountmap);
    } catch (const YamlEmitterException &e) {
        ERROR("%s", e.what());
    }

    // Cleanup
    Sequence *credSeq = credentialseq.getSequence();
    for (Sequence::iterator seqit = credSeq->begin(); seqit != credSeq->end(); ++seqit) {
        MappingNode *node = static_cast<MappingNode*>(*seqit);
        delete node->getValue(CONFIG_ACCOUNT_USERNAME);
        delete node->getValue(CONFIG_ACCOUNT_PASSWORD);
        delete node->getValue(CONFIG_ACCOUNT_REALM);
        delete node;
    }

#ifdef SFL_VIDEO
    Sequence *videoCodecSeq = videoCodecs.getSequence();
    for (Sequence::iterator i = videoCodecSeq->begin(); i != videoCodecSeq->end(); ++i) {
        MappingNode *node = static_cast<MappingNode*>(*i);
        delete node->getValue(VIDEO_CODEC_NAME);
        delete node->getValue(VIDEO_CODEC_BITRATE);
        delete node->getValue(VIDEO_CODEC_ENABLED);
        delete node;
    }
#endif
}

void SIPAccount::unserialize(const Conf::MappingNode &mapNode)
{
    using namespace Conf;
    using std::vector;
    using std::map;
    using std::string;

    mapNode.getValue(ALIAS_KEY, &alias_);
    mapNode.getValue(TYPE_KEY, &type_);
    mapNode.getValue(USERNAME_KEY, &username_);
    if (not isIP2IP()) mapNode.getValue(HOSTNAME_KEY, &hostname_);
    mapNode.getValue(ACCOUNT_ENABLE_KEY, &enabled_);
    if (not isIP2IP()) mapNode.getValue(MAILBOX_KEY, &mailBox_);
    mapNode.getValue(AUDIO_CODECS_KEY, &audioCodecStr_);
    // Update codec list which one is used for SDP offer
    setActiveAudioCodecs(ManagerImpl::split_string(audioCodecStr_));
#ifdef SFL_VIDEO
    YamlNode *videoCodecsNode(mapNode.getValue(VIDEO_CODECS_KEY));

    if (videoCodecsNode and videoCodecsNode->getType() == SEQUENCE) {
        SequenceNode *videoCodecs = static_cast<SequenceNode *>(videoCodecsNode);
        Sequence *seq = videoCodecs->getSequence();
        if (seq->empty()) {
            // Video codecs are an empty list
            WARN("Loading default video codecs");
            videoCodecList_ = libav_utils::getDefaultCodecs();
        } else {
            vector<map<string, string> > videoCodecDetails;
            for (Sequence::iterator it = seq->begin(); it != seq->end(); ++it) {
                MappingNode *codec = static_cast<MappingNode *>(*it);
                map<string, string> codecMap;
                codec->getValue(VIDEO_CODEC_NAME, &codecMap[VIDEO_CODEC_NAME]);
                codec->getValue(VIDEO_CODEC_BITRATE, &codecMap[VIDEO_CODEC_BITRATE]);
                codec->getValue(VIDEO_CODEC_ENABLED, &codecMap[VIDEO_CODEC_ENABLED]);
                videoCodecDetails.push_back(codecMap);
            }
            // these must be validated
            setVideoCodecs(videoCodecDetails);
        }
    } else {
        // either this is an older config file which had videoCodecs as a scalar node,
        // or it had no video codecs at all
        WARN("Loading default video codecs");
        videoCodecList_ = libav_utils::getDefaultCodecs();
    }
#endif

    mapNode.getValue(RINGTONE_PATH_KEY, &ringtonePath_);
    mapNode.getValue(RINGTONE_ENABLED_KEY, &ringtoneEnabled_);
    if (not isIP2IP()) mapNode.getValue(Preferences::REGISTRATION_EXPIRE_KEY, &registrationExpire_);
    mapNode.getValue(INTERFACE_KEY, &interface_);
    int port = DEFAULT_SIP_PORT;
    mapNode.getValue(PORT_KEY, &port);
    localPort_ = port;
    mapNode.getValue(PUBLISH_ADDR_KEY, &publishedIpAddress_);
    mapNode.getValue(PUBLISH_PORT_KEY, &port);
    publishedPort_ = port;
    mapNode.getValue(SAME_AS_LOCAL_KEY, &publishedSameasLocal_);
    if (not isIP2IP()) mapNode.getValue(KEEP_ALIVE_ENABLED, &keepAliveEnabled_);

    std::string dtmfType;
    mapNode.getValue(DTMF_TYPE_KEY, &dtmfType);
    dtmfType_ = dtmfType;

    if (not isIP2IP()) mapNode.getValue(SERVICE_ROUTE_KEY, &serviceRoute_);
    mapNode.getValue(UPDATE_CONTACT_HEADER_KEY, &contactUpdateEnabled_);

    // stun enabled
    if (not isIP2IP()) mapNode.getValue(STUN_ENABLED_KEY, &stunEnabled_);
    if (not isIP2IP()) mapNode.getValue(STUN_SERVER_KEY, &stunServer_);

    // Init stun server name with default server name
    stunServerName_ = pj_str((char*) stunServer_.data());

    mapNode.getValue(DISPLAY_NAME_KEY, &displayName_);

    std::vector<std::map<std::string, std::string> > creds;

    YamlNode *credNode = mapNode.getValue(CRED_KEY);

    /* We check if the credential key is a sequence
     * because it was a mapping in a previous version of
     * the configuration file.
     */
    if (credNode && credNode->getType() == SEQUENCE) {
        SequenceNode *credSeq = static_cast<SequenceNode *>(credNode);
        Sequence::iterator it;
        Sequence *seq = credSeq->getSequence();

        for (it = seq->begin(); it != seq->end(); ++it) {
            MappingNode *cred = static_cast<MappingNode *>(*it);
            std::string user;
            std::string pass;
            std::string realm;
            cred->getValue(CONFIG_ACCOUNT_USERNAME, &user);
            cred->getValue(CONFIG_ACCOUNT_PASSWORD, &pass);
            cred->getValue(CONFIG_ACCOUNT_REALM, &realm);
            std::map<std::string, std::string> credentialMap;
            credentialMap[CONFIG_ACCOUNT_USERNAME] = user;
            credentialMap[CONFIG_ACCOUNT_PASSWORD] = pass;
            credentialMap[CONFIG_ACCOUNT_REALM] = realm;
            creds.push_back(credentialMap);
        }
    }

    if (creds.empty()) {
        // migration from old file format
        std::map<std::string, std::string> credmap;
        std::string password;
        if (not isIP2IP()) mapNode.getValue(PASSWORD_KEY, &password);

        credmap[CONFIG_ACCOUNT_USERNAME] = username_;
        credmap[CONFIG_ACCOUNT_PASSWORD] = password;
        credmap[CONFIG_ACCOUNT_REALM] = "*";
        creds.push_back(credmap);
    }

    setCredentials(creds);

    // get srtp submap
    MappingNode *srtpMap = static_cast<MappingNode *>(mapNode.getValue(SRTP_KEY));

    if (srtpMap) {
        srtpMap->getValue(SRTP_ENABLE_KEY, &srtpEnabled_);
        srtpMap->getValue(KEY_EXCHANGE_KEY, &srtpKeyExchange_);
        srtpMap->getValue(RTP_FALLBACK_KEY, &srtpFallback_);
    }

    // get zrtp submap
    MappingNode *zrtpMap = static_cast<MappingNode *>(mapNode.getValue(ZRTP_KEY));

    if (zrtpMap) {
        zrtpMap->getValue(DISPLAY_SAS_KEY, &zrtpDisplaySas_);
        zrtpMap->getValue(DISPLAY_SAS_ONCE_KEY, &zrtpDisplaySasOnce_);
        zrtpMap->getValue(HELLO_HASH_ENABLED_KEY, &zrtpHelloHash_);
        zrtpMap->getValue(NOT_SUPP_WARNING_KEY, &zrtpNotSuppWarning_);
    }

    // get tls submap
    MappingNode *tlsMap = static_cast<MappingNode *>(mapNode.getValue(TLS_KEY));

    if (tlsMap) {
        tlsMap->getValue(TLS_ENABLE_KEY, &tlsEnable_);
        std::string tlsPort;
        tlsMap->getValue(TLS_PORT_KEY, &tlsPort);
        tlsListenerPort_ = atoi(tlsPort.c_str());
        tlsMap->getValue(CERTIFICATE_KEY, &tlsCertificateFile_);
        tlsMap->getValue(CALIST_KEY, &tlsCaListFile_);
        tlsMap->getValue(CIPHERS_KEY, &tlsCiphers_);
        tlsMap->getValue(METHOD_KEY, &tlsMethod_);
        tlsMap->getValue(TLS_PASSWORD_KEY, &tlsPassword_);
        tlsMap->getValue(PRIVATE_KEY_KEY, &tlsPrivateKeyFile_);
        tlsMap->getValue(REQUIRE_CERTIF_KEY, &tlsRequireClientCertificate_);
        tlsMap->getValue(SERVER_KEY, &tlsServerName_);
        tlsMap->getValue(VERIFY_CLIENT_KEY, &tlsVerifyServer_);
        tlsMap->getValue(VERIFY_SERVER_KEY, &tlsVerifyClient_);
        // FIXME
        tlsMap->getValue(TIMEOUT_KEY, &tlsNegotiationTimeoutSec_);
        tlsMap->getValue(TIMEOUT_KEY, &tlsNegotiationTimeoutMsec_);
    }
}


void SIPAccount::setAccountDetails(std::map<std::string, std::string> details)
{
    // Account setting common to SIP and IAX
    alias_ = details[CONFIG_ACCOUNT_ALIAS];
    type_ = details[CONFIG_ACCOUNT_TYPE];
    username_ = details[CONFIG_ACCOUNT_USERNAME];
    hostname_ = details[CONFIG_ACCOUNT_HOSTNAME];
    enabled_ = details[CONFIG_ACCOUNT_ENABLE] == "true";
    ringtonePath_ = details[CONFIG_RINGTONE_PATH];
    ringtoneEnabled_ = details[CONFIG_RINGTONE_ENABLED] == "true";
    mailBox_ = details[CONFIG_ACCOUNT_MAILBOX];

    // SIP specific account settings

    // general sip settings
    displayName_ = details[CONFIG_DISPLAY_NAME];
    serviceRoute_ = details[CONFIG_ACCOUNT_ROUTESET];
    interface_ = details[CONFIG_LOCAL_INTERFACE];
    publishedSameasLocal_ = details[CONFIG_PUBLISHED_SAMEAS_LOCAL] == "true";
    publishedIpAddress_ = details[CONFIG_PUBLISHED_ADDRESS];
    localPort_ = atoi(details[CONFIG_LOCAL_PORT].c_str());
    publishedPort_ = atoi(details[CONFIG_PUBLISHED_PORT].c_str());
    if (stunServer_ != details[CONFIG_STUN_SERVER]) {
        link_->sipTransport.destroyStunResolver(stunServer_);
        // pj_stun_sock_destroy(pj_stun_sock *stun_sock);
    }
    stunServer_ = details[CONFIG_STUN_SERVER];
    stunEnabled_ = details[CONFIG_STUN_ENABLE] == "true";
    dtmfType_ = details[CONFIG_ACCOUNT_DTMF_TYPE];
    registrationExpire_ = atoi(details[CONFIG_ACCOUNT_REGISTRATION_EXPIRE].c_str());
    if(registrationExpire_ < MIN_REGISTRATION_TIME)
        registrationExpire_ = MIN_REGISTRATION_TIME;

    userAgent_ = details[CONFIG_ACCOUNT_USERAGENT];
    keepAliveEnabled_ = details[CONFIG_KEEP_ALIVE_ENABLED] == "true";

    // srtp settings
    srtpEnabled_ = details[CONFIG_SRTP_ENABLE] == "true";
    srtpFallback_ = details[CONFIG_SRTP_RTP_FALLBACK] == "true";
    zrtpDisplaySas_ = details[CONFIG_ZRTP_DISPLAY_SAS] == "true";
    zrtpDisplaySasOnce_ = details[CONFIG_ZRTP_DISPLAY_SAS_ONCE] == "true";
    zrtpNotSuppWarning_ = details[CONFIG_ZRTP_NOT_SUPP_WARNING] == "true";
    zrtpHelloHash_ = details[CONFIG_ZRTP_HELLO_HASH] == "true";
    srtpKeyExchange_ = details[CONFIG_SRTP_KEY_EXCHANGE];

    // TLS settings
    tlsListenerPort_ = atoi(details[CONFIG_TLS_LISTENER_PORT].c_str());
    tlsEnable_ = details[CONFIG_TLS_ENABLE];
    tlsCaListFile_ = details[CONFIG_TLS_CA_LIST_FILE];
    tlsCertificateFile_ = details[CONFIG_TLS_CERTIFICATE_FILE];
    tlsPrivateKeyFile_ = details[CONFIG_TLS_PRIVATE_KEY_FILE];
    tlsPassword_ = details[CONFIG_TLS_PASSWORD];
    tlsMethod_ = details[CONFIG_TLS_METHOD];
    tlsCiphers_ = details[CONFIG_TLS_CIPHERS];
    tlsServerName_ = details[CONFIG_TLS_SERVER_NAME];
    tlsVerifyServer_ = details[CONFIG_TLS_VERIFY_SERVER] == "true";
    tlsVerifyClient_ = details[CONFIG_TLS_VERIFY_CLIENT] == "true";
    tlsRequireClientCertificate_ = details[CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE] == "true";
    tlsNegotiationTimeoutSec_ = details[CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC];
    tlsNegotiationTimeoutMsec_ = details[CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC];

    if (credentials_.empty()) { // credentials not set, construct 1 entry
        std::vector<std::map<std::string, std::string> > v;
        std::map<std::string, std::string> map;
        map[CONFIG_ACCOUNT_USERNAME] = username_;
        map[CONFIG_ACCOUNT_PASSWORD] = details[CONFIG_ACCOUNT_PASSWORD];
        map[CONFIG_ACCOUNT_REALM]    = "*";
        v.push_back(map);
        setCredentials(v);
    }
}

std::map<std::string, std::string> SIPAccount::getAccountDetails() const
{
    std::map<std::string, std::string> a;

    a[CONFIG_ACCOUNT_ID] = accountID_;
    // note: The IP2IP profile will always have IP2IP as an alias
    a[CONFIG_ACCOUNT_ALIAS] = alias_;

    a[CONFIG_ACCOUNT_ENABLE] = enabled_ ? "true" : "false";
    a[CONFIG_ACCOUNT_TYPE] = type_;
    a[CONFIG_ACCOUNT_HOSTNAME] = hostname_;
    a[CONFIG_ACCOUNT_USERNAME] = username_;

    a[CONFIG_RINGTONE_PATH] = ringtonePath_;
    a[CONFIG_RINGTONE_ENABLED] = ringtoneEnabled_ ? "true" : "false";
    a[CONFIG_ACCOUNT_MAILBOX] = mailBox_;

    RegistrationState state = UNREGISTERED;
    std::string registrationStateCode;
    std::string registrationStateDescription;

    if (isIP2IP())
        registrationStateDescription = "Direct IP call";
    else {
        state = registrationState_;
        int code = registrationStateDetailed_.first;
        std::stringstream out;
        out << code;
        registrationStateCode = out.str();
        registrationStateDescription = registrationStateDetailed_.second;
    }

    a[CONFIG_ACCOUNT_REGISTRATION_STATUS] = isIP2IP() ? "READY": mapStateNumberToString(state);
    a[CONFIG_ACCOUNT_REGISTRATION_STATE_CODE] = registrationStateCode;
    a[CONFIG_ACCOUNT_REGISTRATION_STATE_DESC] = registrationStateDescription;

    // Add sip specific details
    a[CONFIG_ACCOUNT_ROUTESET] = serviceRoute_;
    a[CONFIG_ACCOUNT_USERAGENT] = userAgent_;

    std::stringstream registrationExpireStr;
    registrationExpireStr << registrationExpire_;
    a[CONFIG_ACCOUNT_REGISTRATION_EXPIRE] = registrationExpireStr.str();
    a[CONFIG_LOCAL_INTERFACE] = interface_;
    a[CONFIG_PUBLISHED_SAMEAS_LOCAL] = publishedSameasLocal_ ? "true" : "false";
    a[CONFIG_PUBLISHED_ADDRESS] = publishedIpAddress_;

    std::stringstream localport;
    localport << localPort_;
    a[CONFIG_LOCAL_PORT] = localport.str();
    std::stringstream publishedport;
    publishedport << publishedPort_;
    a[CONFIG_PUBLISHED_PORT] = publishedport.str();
    a[CONFIG_STUN_ENABLE] = stunEnabled_ ? "true" : "false";
    a[CONFIG_STUN_SERVER] = stunServer_;
    a[CONFIG_ACCOUNT_DTMF_TYPE] = dtmfType_;
    a[CONFIG_KEEP_ALIVE_ENABLED] = keepAliveEnabled_ ? "true" : "false";

    a[CONFIG_SRTP_KEY_EXCHANGE] = srtpKeyExchange_;
    a[CONFIG_SRTP_ENABLE] = srtpEnabled_ ? "true" : "false";
    a[CONFIG_SRTP_RTP_FALLBACK] = srtpFallback_ ? "true" : "false";

    a[CONFIG_ZRTP_DISPLAY_SAS] = zrtpDisplaySas_ ? "true" : "false";
    a[CONFIG_ZRTP_DISPLAY_SAS_ONCE] = zrtpDisplaySasOnce_ ? "true" : "false";
    a[CONFIG_ZRTP_HELLO_HASH] = zrtpHelloHash_ ? "true" : "false";
    a[CONFIG_ZRTP_NOT_SUPP_WARNING] = zrtpNotSuppWarning_ ? "true" : "false";

    // TLS listener is unique and parameters are modified through IP2IP_PROFILE
    std::stringstream tlslistenerport;
    tlslistenerport << tlsListenerPort_;
    a[CONFIG_TLS_LISTENER_PORT] = tlslistenerport.str();
    a[CONFIG_TLS_ENABLE] = tlsEnable_;
    a[CONFIG_TLS_CA_LIST_FILE] = tlsCaListFile_;
    a[CONFIG_TLS_CERTIFICATE_FILE] = tlsCertificateFile_;
    a[CONFIG_TLS_PRIVATE_KEY_FILE] = tlsPrivateKeyFile_;
    a[CONFIG_TLS_PASSWORD] = tlsPassword_;
    a[CONFIG_TLS_METHOD] = tlsMethod_;
    a[CONFIG_TLS_CIPHERS] = tlsCiphers_;
    a[CONFIG_TLS_SERVER_NAME] = tlsServerName_;
    a[CONFIG_TLS_VERIFY_SERVER] = tlsVerifyServer_ ? "true" : "false";
    a[CONFIG_TLS_VERIFY_CLIENT] = tlsVerifyClient_ ? "true" : "false";
    a[CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE] = tlsRequireClientCertificate_ ? "true" : "false";
    a[CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC] = tlsNegotiationTimeoutSec_;
    a[CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC] = tlsNegotiationTimeoutMsec_;

    return a;
}

void SIPAccount::registerVoIPLink()
{
    if (hostname_.length() >= PJ_MAX_HOSTNAME)
        return;

    // Init TLS settings if the user wants to use TLS
    if (tlsEnable_ == "true") {
        DEBUG("TLS is enabled for account %s", accountID_.c_str());
        transportType_ = PJSIP_TRANSPORT_TLS;
        initTlsConfiguration();
    }

    // Init STUN settings for this account if the user selected it
    if (stunEnabled_) {
        transportType_ = PJSIP_TRANSPORT_START_OTHER;
        initStunConfiguration();
    } else {
        stunServerName_ = pj_str((char*) stunServer_.c_str());
    }

    // In our definition of the ip2ip profile (aka Direct IP Calls),
    // no registration should be performed
    if (isIP2IP())
        return;

    try {
        link_->sendRegister(this);
    } catch (const VoipLinkException &e) {
        ERROR("%s", e.what());
    }
}

void SIPAccount::unregisterVoIPLink()
{
    if (isIP2IP())
        return;

    try {
        link_->sendUnregister(this);
    } catch (const VoipLinkException &e) {
        ERROR("%s", e.what());
    }
}

void SIPAccount::startKeepAliveTimer() {

    if (isTlsEnabled())
        return;

    if (isIP2IP())
        return;

    if (keepAliveTimerActive_)
        return;

    DEBUG("Start keep alive timer for account %s", getAccountID().c_str());

    // make sure here we have an entirely new timer
    memset(&keepAliveTimer_, 0, sizeof(pj_timer_entry));

    pj_time_val keepAliveDelay_;
    keepAliveTimer_.cb = &SIPAccount::keepAliveRegistrationCb;
    keepAliveTimer_.user_data = this;
    keepAliveTimer_.id = rand();

    // expiration may be undetermined during the first registration request
    if (registrationExpire_ == 0) {
        DEBUG("Registration Expire: 0, taking 60 instead");
        keepAliveDelay_.sec = 3600;
    } else {
        DEBUG("Registration Expire: %d", registrationExpire_);
        keepAliveDelay_.sec = registrationExpire_ + MIN_REGISTRATION_TIME;
    }

    keepAliveDelay_.msec = 0;

    keepAliveTimerActive_ = true;

    link_->registerKeepAliveTimer(keepAliveTimer_, keepAliveDelay_);
}

void SIPAccount::stopKeepAliveTimer() {
    DEBUG("Stop keep alive timer %d for account %s", keepAliveTimer_.id, getAccountID().c_str());

    keepAliveTimerActive_ = false;

    link_->cancelKeepAliveTimer(keepAliveTimer_);
}

pjsip_ssl_method SIPAccount::sslMethodStringToPjEnum(const std::string& method)
{
    if (method == "Default")
        return PJSIP_SSL_UNSPECIFIED_METHOD;

    if (method == "TLSv1")
        return PJSIP_TLSV1_METHOD;

    if (method == "SSLv3")
        return PJSIP_SSLV3_METHOD;

    if (method == "SSLv23")
        return PJSIP_SSLV23_METHOD;

    return PJSIP_SSL_UNSPECIFIED_METHOD;
}

void SIPAccount::initTlsConfiguration()
{
    pj_status_t status;
    unsigned cipherNum;

    // Determine the cipher list supported on this machine
    cipherNum = PJ_ARRAY_SIZE(ciphers);
    status = pj_ssl_cipher_get_availables(&ciphers.front(), &cipherNum);
    if (status != PJ_SUCCESS) {
        ERROR("Could not determine cipher list on this system");
    }

    ciphers.resize(cipherNum);

    // TLS listener is unique and should be only modified through IP2IP_PROFILE
    pjsip_tls_setting_default(&tlsSetting_);

    pj_cstr(&tlsSetting_.ca_list_file, tlsCaListFile_.c_str());
    pj_cstr(&tlsSetting_.cert_file, tlsCertificateFile_.c_str());
    pj_cstr(&tlsSetting_.privkey_file, tlsPrivateKeyFile_.c_str());
    pj_cstr(&tlsSetting_.password, tlsPassword_.c_str());
    tlsSetting_.method = sslMethodStringToPjEnum(tlsMethod_);
    tlsSetting_.ciphers_num = ciphers.size();
    tlsSetting_.ciphers = &ciphers.front();

    tlsSetting_.verify_server = tlsVerifyServer_ ? PJ_TRUE: PJ_FALSE;
    tlsSetting_.verify_client = tlsVerifyClient_ ? PJ_TRUE: PJ_FALSE;
    tlsSetting_.require_client_cert = tlsRequireClientCertificate_ ? PJ_TRUE: PJ_FALSE;

    tlsSetting_.timeout.sec = atol(tlsNegotiationTimeoutSec_.c_str());
    tlsSetting_.timeout.msec = atol(tlsNegotiationTimeoutMsec_.c_str());

    tlsSetting_.qos_type = PJ_QOS_TYPE_BEST_EFFORT;
    tlsSetting_.qos_ignore_error = PJ_TRUE;
}

void SIPAccount::initStunConfiguration()
{
    size_t pos;
    std::string stunServer, serverName, serverPort;

    stunServer = stunServer_;
    // Init STUN socket
    pos = stunServer.find(':');

    if (pos == std::string::npos) {
        stunServerName_ = pj_str((char*) stunServer.data());
        stunPort_ = PJ_STUN_PORT;
        //stun_status = pj_sockaddr_in_init (&stun_srv.ipv4, &stun_adr, (pj_uint16_t) 3478);
    } else {
        serverName = stunServer.substr(0, pos);
        serverPort = stunServer.substr(pos + 1);
        stunPort_ = atoi(serverPort.data());
        stunServerName_ = pj_str((char*) serverName.data());
        //stun_status = pj_sockaddr_in_init (&stun_srv.ipv4, &stun_adr, (pj_uint16_t) nPort);
    }
}

void SIPAccount::loadConfig()
{
    if (registrationExpire_ == 0)
        registrationExpire_ = DEFAULT_REGISTRATION_TIME; /** Default expire value for registration */

    if (tlsEnable_ == "true") {
        initTlsConfiguration();
        transportType_ = PJSIP_TRANSPORT_TLS;
    } else
        transportType_ = PJSIP_TRANSPORT_UDP;
}

bool SIPAccount::fullMatch(const std::string& username, const std::string& hostname) const
{
    return userMatch(username) and hostnameMatch(hostname);
}

bool SIPAccount::userMatch(const std::string& username) const
{
    return !username.empty() and username == username_;
}

bool SIPAccount::hostnameMatch(const std::string& hostname) const
{
    return hostname == hostname_;
}

std::string SIPAccount::getLoginName()
{
    struct passwd * user_info = getpwuid(getuid());
    return user_info ? user_info->pw_name : "";
}

std::string SIPAccount::getFromUri() const
{
    std::string scheme;
    std::string transport;
    std::string username(username_);
    std::string hostname(hostname_);

    // UDP does not require the transport specification
    if (transportType_ == PJSIP_TRANSPORT_TLS) {
        scheme = "sips:";
        transport = ";transport=" + std::string(pjsip_transport_get_type_name(transportType_));
    } else
        scheme = "sip:";

    // Get login name if username is not specified
    if (username_.empty())
        username = getLoginName();

    // Get machine hostname if not provided
    if (hostname_.empty())
        hostname = std::string(pj_gethostname()->ptr, pj_gethostname()->slen);

    return "<" + scheme + username + "@" + hostname + transport + ">";
}

std::string SIPAccount::getToUri(const std::string& username) const
{
    std::string scheme;
    std::string transport;
    std::string hostname;

    // UDP does not require the transport specification
    if (transportType_ == PJSIP_TRANSPORT_TLS) {
        scheme = "sips:";
        transport = ";transport=" + std::string(pjsip_transport_get_type_name(transportType_));
    } else
        scheme = "sip:";

    // Check if scheme is already specified
    if (username.find("sip") == 0)
        scheme = "";

    // Check if hostname is already specified
    if (username.find("@") == std::string::npos)
        hostname = hostname_;

    return "<" + scheme + username + (hostname.empty() ? "" : "@") + hostname + transport + ">";
}

std::string SIPAccount::getServerUri() const
{
    std::string scheme;
    std::string transport;

    // UDP does not require the transport specification
    if (transportType_ == PJSIP_TRANSPORT_TLS) {
        scheme = "sips:";
        transport = ";transport=" + std::string(pjsip_transport_get_type_name(transportType_));
    } else {
        scheme = "sip:";
        transport = "";
    }

    return "<" + scheme + hostname_ + transport + ">";
}

void SIPAccount::setContactHeader(std::string address, std::string port)
{
    std::string scheme;
    std::string transport;

    // UDP does not require the transport specification
    if (transportType_ == PJSIP_TRANSPORT_TLS) {
        scheme = "sips:";
        transport = ";transport=" + std::string(pjsip_transport_get_type_name(transportType_));
    } else
        scheme = "sip:";

    contactHeader_ = displayName_ + (displayName_.empty() ? "" : " ") + "<" +
                     scheme + username_ + (username_.empty() ? "":"@") +
                     address + ":" + port + transport + ">";
}


std::string SIPAccount::getContactHeader() const
{
    if (transport_ == NULL)
        ERROR("Transport not created yet");

    // The transport type must be specified, in our case START_OTHER refers to stun transport
    pjsip_transport_type_e transportType = transportType_;
    if (transportType == PJSIP_TRANSPORT_START_OTHER)
        transportType = PJSIP_TRANSPORT_UDP;

    // Use the CONTACT header provided by the registrar if any
    if (!contactHeader_.empty())
        return contactHeader_;

    // Else we determine this infor based on transport information
    std::string address, port;
    std::ostringstream portstr;

    link_->sipTransport.findLocalAddressFromTransport(transport_, transportType, address, port);

    if (!receivedParameter_.empty())
       address = receivedParameter_;

    if (rPort_ != -1) {
        portstr << rPort_;
        port = portstr.str();
    }

    // UDP does not require the transport specification
    std::string scheme;
    std::string transport;
    if (transportType_ == PJSIP_TRANSPORT_TLS) {
        scheme = "sips:";
        transport = ";transport=" + std::string(pjsip_transport_get_type_name(transportType));
    } else
        scheme = "sip:";

    return displayName_ + (displayName_.empty() ? "" : " ") + "<" +
           scheme + username_ + (username_.empty() ? "" : "@") +
           address + ":" + port + transport + ">";
}

void SIPAccount::keepAliveRegistrationCb(UNUSED pj_timer_heap_t *th, pj_timer_entry *te)
{
    SIPAccount *sipAccount = static_cast<SIPAccount *>(te->user_data);

    ERROR("Keep alive registration callback for account %s", sipAccount->getAccountID().c_str());

    if (sipAccount == NULL) {
        ERROR("SIP account is NULL while registering a new keep alive timer");
        return;
    }

    // IP2IP default does not require keep-alive
    if (sipAccount->isIP2IP())
        return;

    // TLS is connection oriented and does not require keep-alive
    if (sipAccount->isTlsEnabled())
        return;

    sipAccount->stopKeepAliveTimer();

    if (sipAccount->isRegistered())
        sipAccount->registerVoIPLink();
}

namespace {
std::string computeMd5HashFromCredential(const std::string& username,
                                         const std::string& password,
                                         const std::string& realm)
{
#define MD5_APPEND(pms,buf,len) pj_md5_update(pms, (const pj_uint8_t*)buf, len)

    pj_md5_context pms;

    /* Compute md5 hash = MD5(username ":" realm ":" password) */
    pj_md5_init(&pms);
    MD5_APPEND(&pms, username.data(), username.length());
    MD5_APPEND(&pms, ":", 1);
    MD5_APPEND(&pms, realm.data(), realm.length());
    MD5_APPEND(&pms, ":", 1);
    MD5_APPEND(&pms, password.data(), password.length());
#undef MD5_APPEND

    unsigned char digest[16];
    pj_md5_final(&pms, digest);

    char hash[32];

    for (int i = 0; i < 16; ++i)
        pj_val_to_hex_digit(digest[i], &hash[2*i]);

    return std::string(hash, 32);
}
} // anon namespace

void SIPAccount::setCredentials(const std::vector<std::map<std::string, std::string> >& creds)
{
    // we can not authenticate without credentials
    if (creds.empty()) {
        ERROR("Cannot authenticate with empty credentials list");
        return;
    }

    using std::vector;
    using std::string;
    using std::map;

    bool md5HashingEnabled = Manager::instance().preferences.getMd5Hash();

    credentials_ = creds;

    /* md5 hashing */
    for (vector<map<string, string> >::iterator it = credentials_.begin(); it != credentials_.end(); ++it) {
        map<string, string>::const_iterator val = (*it).find(CONFIG_ACCOUNT_USERNAME);
        const std::string username = val != (*it).end() ? val->second : "";
        val = (*it).find(CONFIG_ACCOUNT_REALM);
        const std::string realm(val != (*it).end() ? val->second : "");
        val = (*it).find(CONFIG_ACCOUNT_PASSWORD);
        const std::string password(val != (*it).end() ? val->second : "");

        if (md5HashingEnabled) {
            // TODO: Fix this.
            // This is an extremly weak test in order to check
            // if the password is a hashed value. This is done
            // because deleteCredential() is called before this
            // method. Therefore, we cannot check if the value
            // is different from the one previously stored in
            // the configuration file. This is to avoid to
            // re-hash a hashed password.

            if (password.length() != 32)
                (*it)[CONFIG_ACCOUNT_PASSWORD] = computeMd5HashFromCredential(username, password, realm);
        }
    }

    // Create the credential array
    cred_.resize(credentials_.size());

    size_t i = 0;
    for (vector<map<string, string > >::const_iterator iter = credentials_.begin();
            iter != credentials_.end(); ++iter) {
        map<string, string>::const_iterator val = (*iter).find(CONFIG_ACCOUNT_PASSWORD);
        const std::string password = val != (*iter).end() ? val->second : "";
        int dataType = (md5HashingEnabled and password.length() == 32)
                       ? PJSIP_CRED_DATA_DIGEST
                       : PJSIP_CRED_DATA_PLAIN_PASSWD;

        val = (*iter).find(CONFIG_ACCOUNT_USERNAME);

        if (val != (*iter).end())
            cred_[i].username = pj_str((char*) val->second.c_str());

        cred_[i].data = pj_str((char*) password.c_str());

        val = (*iter).find(CONFIG_ACCOUNT_REALM);

        if (val != (*iter).end())
            cred_[i].realm = pj_str((char*) val->second.c_str());

        cred_[i].data_type = dataType;
        cred_[i].scheme = pj_str((char*) "digest");
        ++i;
    }
}

const std::vector<std::map<std::string, std::string> > &
SIPAccount::getCredentials() const
{
    return credentials_;
}

std::string SIPAccount::getUserAgentName() const
{
    std::string result(userAgent_);

    if (result == "sflphone" or result.empty())
        result += "/" PACKAGE_VERSION;

    return result;
}

std::map<std::string, std::string> SIPAccount::getIp2IpDetails() const
{
    assert(isIP2IP());
    std::map<std::string, std::string> ip2ipAccountDetails;
    ip2ipAccountDetails[CONFIG_ACCOUNT_ID] = IP2IP_PROFILE;
    ip2ipAccountDetails[CONFIG_SRTP_KEY_EXCHANGE] = srtpKeyExchange_;
    ip2ipAccountDetails[CONFIG_SRTP_ENABLE] = srtpEnabled_ ? "true" : "false";
    ip2ipAccountDetails[CONFIG_SRTP_RTP_FALLBACK] = srtpFallback_ ? "true" : "false";
    ip2ipAccountDetails[CONFIG_ZRTP_DISPLAY_SAS] = zrtpDisplaySas_ ? "true" : "false";
    ip2ipAccountDetails[CONFIG_ZRTP_HELLO_HASH] = zrtpHelloHash_ ? "true" : "false";
    ip2ipAccountDetails[CONFIG_ZRTP_NOT_SUPP_WARNING] = zrtpNotSuppWarning_ ? "true" : "false";
    ip2ipAccountDetails[CONFIG_ZRTP_DISPLAY_SAS_ONCE] = zrtpDisplaySasOnce_ ? "true" : "false";
    ip2ipAccountDetails[CONFIG_LOCAL_INTERFACE] = interface_;
    std::stringstream portstr;
    portstr << localPort_;
    ip2ipAccountDetails[CONFIG_LOCAL_PORT] = portstr.str();

    std::map<std::string, std::string> tlsSettings(getTlsSettings());
    std::copy(tlsSettings.begin(), tlsSettings.end(), std::inserter(
                  ip2ipAccountDetails, ip2ipAccountDetails.end()));

    return ip2ipAccountDetails;
}

std::map<std::string, std::string> SIPAccount::getTlsSettings() const
{
    assert(isIP2IP());
    std::map<std::string, std::string> tlsSettings;

    std::stringstream portstr;
    portstr << tlsListenerPort_;
    tlsSettings[CONFIG_TLS_LISTENER_PORT] = portstr.str();
    tlsSettings[CONFIG_TLS_ENABLE] = tlsEnable_;
    tlsSettings[CONFIG_TLS_CA_LIST_FILE] = tlsCaListFile_;
    tlsSettings[CONFIG_TLS_CERTIFICATE_FILE] = tlsCertificateFile_;
    tlsSettings[CONFIG_TLS_PRIVATE_KEY_FILE] = tlsPrivateKeyFile_;
    tlsSettings[CONFIG_TLS_PASSWORD] = tlsPassword_;
    tlsSettings[CONFIG_TLS_METHOD] = tlsMethod_;
    tlsSettings[CONFIG_TLS_CIPHERS] = tlsCiphers_;
    tlsSettings[CONFIG_TLS_SERVER_NAME] = tlsServerName_;
    tlsSettings[CONFIG_TLS_VERIFY_SERVER] = tlsVerifyServer_ ? "true" : "false";
    tlsSettings[CONFIG_TLS_VERIFY_CLIENT] = tlsVerifyClient_ ? "true" : "false";
    tlsSettings[CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE] = tlsRequireClientCertificate_ ? "true" : "false";
    tlsSettings[CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC] = tlsNegotiationTimeoutSec_;
    tlsSettings[CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC] = tlsNegotiationTimeoutMsec_;

    return tlsSettings;
}

namespace {
void set_opt(const std::map<std::string, std::string> &details, const char *key, std::string &val)
{
    std::map<std::string, std::string>::const_iterator it = details.find(key);

    if (it != details.end())
        val = it->second;
}

void set_opt(const std::map<std::string, std::string> &details, const char *key, bool &val)
{
    std::map<std::string, std::string>::const_iterator it = details.find(key);

    if (it != details.end())
        val = it->second == "true";
}

void set_opt(const std::map<std::string, std::string> &details, const char *key, pj_uint16_t &val)
{
    std::map<std::string, std::string>::const_iterator it = details.find(key);

    if (it != details.end())
        val = atoi(it->second.c_str());
}
} //anon namespace

void SIPAccount::setTlsSettings(const std::map<std::string, std::string>& details)
{
    assert(isIP2IP());
    set_opt(details, CONFIG_TLS_LISTENER_PORT, tlsListenerPort_);
    set_opt(details, CONFIG_TLS_ENABLE, tlsEnable_);
    set_opt(details, CONFIG_TLS_CA_LIST_FILE, tlsCaListFile_);
    set_opt(details, CONFIG_TLS_CERTIFICATE_FILE, tlsCertificateFile_);
    set_opt(details, CONFIG_TLS_PRIVATE_KEY_FILE, tlsPrivateKeyFile_);
    set_opt(details, CONFIG_TLS_PASSWORD, tlsPassword_);
    set_opt(details, CONFIG_TLS_METHOD, tlsMethod_);
    set_opt(details, CONFIG_TLS_CIPHERS, tlsCiphers_);
    set_opt(details, CONFIG_TLS_SERVER_NAME, tlsServerName_);
    set_opt(details, CONFIG_TLS_VERIFY_CLIENT, tlsVerifyClient_);
    set_opt(details, CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, tlsRequireClientCertificate_);
    set_opt(details, CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, tlsNegotiationTimeoutSec_);
    set_opt(details, CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, tlsNegotiationTimeoutMsec_);
}

VoIPLink* SIPAccount::getVoIPLink()
{
    return link_;
}

bool SIPAccount::isIP2IP() const
{
    return accountID_ == IP2IP_PROFILE;
}
