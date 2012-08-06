/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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

#include "sdp.h"
#include "logger.h"
#include "manager.h"

#include <algorithm>

using std::string;
using std::map;
using std::vector;
using std::stringstream;

Sdp::Sdp(pj_pool_t *pool)
    : memPool_(pool)
    , negotiator_(NULL)
    , localSession_(NULL)
    , remoteSession_(NULL)
    , activeLocalSession_(NULL)
    , activeRemoteSession_(NULL)
    , audio_codec_list_()
    , video_codec_list_()
    , sessionAudioMedia_()
    , sessionVideoMedia_()
    , localIpAddr_()
    , remoteIpAddr_()
    , localAudioPort_(0)
    , localVideoPort_(0)
    , remoteAudioPort_(0)
    , remoteVideoPort_(0)
    , zrtpHelloHash_()
    , srtpCrypto_()
    , telephoneEventPayload_(101) // same as asterisk
{}

void Sdp::setActiveLocalSdpSession(const pjmedia_sdp_session *sdp)
{
    activeLocalSession_ = (pjmedia_sdp_session*) sdp;

    if (activeLocalSession_->media_count < 1)
        return;

    for (unsigned media = 0; media < activeLocalSession_->media_count; ++media) {
        pjmedia_sdp_media *current = activeLocalSession_->media[media];

        for (unsigned fmt = 0; fmt < current->desc.fmt_count; ++fmt) {
            static const pj_str_t STR_RTPMAP = { (char*) "rtpmap", 6 };
            pjmedia_sdp_attr *attribute = pjmedia_sdp_media_find_attr(current, &STR_RTPMAP, NULL);

            if (!attribute) {
                ERROR("Could not find rtpmap attribute");
                break;
            }

            pjmedia_sdp_rtpmap *rtpmap;
            pjmedia_sdp_attr_to_rtpmap(memPool_, attribute, &rtpmap);

            string type(current->desc.media.ptr, current->desc.media.slen);
            if (type == "audio") {
                const int pt = pj_strtoul(&rtpmap->pt);
                sfl::Codec *codec = Manager::instance().audioCodecFactory.getCodec(pt);
                if (codec)
                    sessionAudioMedia_.push_back(codec);
                else {
                    DEBUG("Could not get codec for payload type %lu", pt);
                    break;
                }
            } else if (type == "video")
                sessionVideoMedia_.push_back(string(rtpmap->enc_name.ptr, rtpmap->enc_name.slen));
        }
    }
}



void Sdp::setActiveRemoteSdpSession(const pjmedia_sdp_session *sdp)
{
    activeRemoteSession_ = (pjmedia_sdp_session*) sdp;

    if (!sdp) {
        ERROR("Remote sdp is NULL while parsing telephone event attribute");
        return;
    }

    for (unsigned i = 0; i < sdp->media_count; i++) {
        if (pj_stricmp2(&sdp->media[i]->desc.media, "audio") == 0) {
            pjmedia_sdp_media *r_media = sdp->media[i];
            static const pj_str_t STR_TELEPHONE_EVENT = { (char*) "telephone-event", 15};
            pjmedia_sdp_attr *attribute = pjmedia_sdp_attr_find(r_media->attr_count, r_media->attr, &STR_TELEPHONE_EVENT, NULL);

            if (attribute != NULL) {
                pjmedia_sdp_rtpmap *rtpmap;
                pjmedia_sdp_attr_to_rtpmap(memPool_, attribute, &rtpmap);
                telephoneEventPayload_ = pj_strtoul(&rtpmap->pt);
            }

            return;
        }
    }

    ERROR("Could not found dtmf event from remote sdp");
}

string Sdp::getSessionVideoCodec() const
{
    if (sessionVideoMedia_.empty()) {
        DEBUG("Session video media is empty");
        return "";
    }
    return sessionVideoMedia_[0];
}

string Sdp::getAudioCodecName() const
{
    try {
        sfl::AudioCodec *codec = getSessionAudioMedia();
        return codec ? codec->getMimeSubtype() : "";
    } catch (...) {
        return "";
    }
}

sfl::AudioCodec* Sdp::getSessionAudioMedia() const
{
    if (sessionAudioMedia_.empty())
        throw SdpException("No codec description for this media");

    return dynamic_cast<sfl::AudioCodec *>(sessionAudioMedia_[0]);
}


pjmedia_sdp_media *Sdp::setMediaDescriptorLine(bool audio)
{
    pjmedia_sdp_media *med = PJ_POOL_ZALLOC_T(memPool_, pjmedia_sdp_media);

    med->desc.media = audio ? pj_str((char*) "audio") : pj_str((char*) "video");
    med->desc.port_count = 1;
    med->desc.port = audio ? localAudioPort_ : localVideoPort_;
    // in case of sdes, media are tagged as "RTP/SAVP", RTP/AVP elsewhere
    med->desc.transport = pj_str(srtpCrypto_.empty() ? (char*) "RTP/AVP" : (char*) "RTP/SAVP");

    int dynamic_payload = 96;

    med->desc.fmt_count = audio ? audio_codec_list_.size() : video_codec_list_.size();
    for (unsigned i = 0; i < med->desc.fmt_count; ++i) {
        unsigned clock_rate;
        string enc_name;
        int payload;

        if (audio) {
            sfl::Codec *codec = audio_codec_list_[i];
            payload = codec->getPayloadType();
            enc_name = codec->getMimeSubtype();
            clock_rate = codec->getClockRate();
            // G722 require G722/8000 media description even if it is 16000 codec
            if (codec->getPayloadType () == 9)
                clock_rate = 8000;
        } else {
            // FIXME: get this key from header
            enc_name = video_codec_list_[i]["name"];
            clock_rate = 90000;
            payload = dynamic_payload;
        }

        std::ostringstream s;
        s << payload;
        pj_strdup2(memPool_, &med->desc.fmt[i], s.str().c_str());

        // Add a rtpmap field for each codec
        // We could add one only for dynamic payloads because the codecs with static RTP payloads
        // are entirely defined in the RFC 3351, but if we want to add other attributes like an asymmetric
        // connection, the rtpmap attribute will be useful to specify for which codec it is applicable
        pjmedia_sdp_rtpmap rtpmap;

        rtpmap.pt = med->desc.fmt[i];
        rtpmap.enc_name = pj_str((char*) enc_name.c_str());
        rtpmap.clock_rate = clock_rate;
        rtpmap.param.ptr = ((char* const)"");
        rtpmap.param.slen = 0;

        pjmedia_sdp_attr *attr;
        pjmedia_sdp_rtpmap_to_attr(memPool_, &rtpmap, &attr);

        med->attr[med->attr_count++] = attr;
        if (enc_name == "H264") {
            std::ostringstream os;
            // FIXME: this should not be hardcoded, it will determine what profile and level
            // our peer will send us
            os << "fmtp:" << dynamic_payload << " profile-level-id=428014";
            med->attr[med->attr_count++] = pjmedia_sdp_attr_create(memPool_, os.str().c_str(), NULL);
        }
        if (not audio)
            dynamic_payload++;
    }

    med->attr[med->attr_count++] = pjmedia_sdp_attr_create(memPool_, "sendrecv", NULL);
    if (!zrtpHelloHash_.empty())
        addZrtpAttribute(med, zrtpHelloHash_);

    if (audio)
        setTelephoneEventRtpmap(med);

    return med;
}

void Sdp::setTelephoneEventRtpmap(pjmedia_sdp_media *med)
{
    pjmedia_sdp_attr *attr_rtpmap = static_cast<pjmedia_sdp_attr *>(pj_pool_zalloc(memPool_, sizeof(pjmedia_sdp_attr)));
    attr_rtpmap->name = pj_str((char *) "rtpmap");
    attr_rtpmap->value = pj_str((char *) "101 telephone-event/8000");

    med->attr[med->attr_count++] = attr_rtpmap;

    pjmedia_sdp_attr *attr_fmtp = static_cast<pjmedia_sdp_attr *>(pj_pool_zalloc(memPool_, sizeof(pjmedia_sdp_attr)));
    attr_fmtp->name = pj_str((char *) "fmtp");
    attr_fmtp->value = pj_str((char *) "101 0-15");

    med->attr[med->attr_count++] = attr_fmtp;
}

void Sdp::setLocalMediaVideoCapabilities(const vector<map<string, string> > &codecs)
{
    video_codec_list_.clear();
#ifdef SFL_VIDEO
    if (codecs.empty())
        WARN("No selected video codec while building local SDP offer");
    else
        video_codec_list_ = codecs;
#else
    (void) codecs;
#endif
}

void Sdp::setLocalMediaAudioCapabilities(const vector<int> &selectedCodecs)
{
    if (selectedCodecs.empty())
        WARN("No selected codec while building local SDP offer");

    audio_codec_list_.clear();
    for (vector<int>::const_iterator i = selectedCodecs.begin(); i != selectedCodecs.end(); ++i) {
        sfl::Codec *codec = Manager::instance().audioCodecFactory.getCodec(*i);

        if (codec)
            audio_codec_list_.push_back(codec);
        else
            WARN("Couldn't find audio codec");
    }
}

namespace {
    void printSession(const pjmedia_sdp_session *session)
    {
        char buffer[2048];
        size_t size = pjmedia_sdp_print(session, buffer, sizeof(buffer));
        string sessionStr(buffer, std::min(size, sizeof(buffer)));
        DEBUG("%s", sessionStr.c_str());
    }
}

int Sdp::createLocalSession(const vector<int> &selectedAudioCodecs, const vector<map<string, string> > &selectedVideoCodecs)
{
    setLocalMediaAudioCapabilities(selectedAudioCodecs);
    setLocalMediaVideoCapabilities(selectedVideoCodecs);

    localSession_ = PJ_POOL_ZALLOC_T(memPool_, pjmedia_sdp_session);
    localSession_->conn = PJ_POOL_ZALLOC_T(memPool_, pjmedia_sdp_conn);

    /* Initialize the fields of the struct */
    localSession_->origin.version = 0;
    pj_time_val tv;
    pj_gettimeofday(&tv);

    localSession_->origin.user = pj_str(pj_gethostname()->ptr);
    // Use Network Time Protocol format timestamp to ensure uniqueness.
    localSession_->origin.id = tv.sec + 2208988800UL;
    localSession_->origin.net_type = pj_str((char*) "IN");
    localSession_->origin.addr_type = pj_str((char*) "IP4");
    localSession_->origin.addr = pj_str((char*) localIpAddr_.c_str());

    localSession_->name = pj_str((char*) PACKAGE);

    localSession_->conn->net_type = localSession_->origin.net_type;
    localSession_->conn->addr_type = localSession_->origin.addr_type;
    localSession_->conn->addr = localSession_->origin.addr;

    // RFC 3264: An offer/answer model session description protocol
    // As the session is created and destroyed through an external signaling mean (SIP), the line
    // should have a value of "0 0".
    localSession_->time.start = 0;
    localSession_->time.stop = 0;

    // For DTMF RTP events
    const bool audio = true;
    localSession_->media_count = 1;
    localSession_->media[0] = setMediaDescriptorLine(audio);
    if (not selectedVideoCodecs.empty()) {
        localSession_->media[1] = setMediaDescriptorLine(!audio);
        ++localSession_->media_count;
    }

    if (!srtpCrypto_.empty())
        addSdesAttribute(srtpCrypto_);

    DEBUG("SDP: Local SDP Session:");
    printSession(localSession_);

    return pjmedia_sdp_validate(localSession_);
}

void Sdp::createOffer(const vector<int> &selectedCodecs, const vector<map<string, string> > &videoCodecs)
{
    if (createLocalSession(selectedCodecs, videoCodecs) != PJ_SUCCESS)
        ERROR("Failed to create initial offer");
    else if (pjmedia_sdp_neg_create_w_local_offer(memPool_, localSession_, &negotiator_) != PJ_SUCCESS)
        ERROR("Failed to create an initial SDP negotiator");
}

void Sdp::receiveOffer(const pjmedia_sdp_session* remote,
                       const vector<int> &selectedCodecs,
                       const vector<map<string, string> > &videoCodecs)
{
    if (!remote) {
        ERROR("Remote session is NULL");
        return;
    }

    DEBUG("Remote SDP Session:");
    printSession(remote);

    if (!localSession_ and createLocalSession(selectedCodecs, videoCodecs) != PJ_SUCCESS) {
        ERROR("Failed to create initial offer");
        return;
    }

    remoteSession_ = pjmedia_sdp_session_clone(memPool_, remote);

    pjmedia_sdp_neg_create_w_remote_offer(memPool_, localSession_,
                                          remoteSession_, &negotiator_);
}

void Sdp::startNegotiation()
{
    if (negotiator_ == NULL) {
        ERROR("Can't start negotiation with invalid negotiator");
        return;
    }

    const pjmedia_sdp_session *active_local;
    const pjmedia_sdp_session *active_remote;

    if (pjmedia_sdp_neg_get_state(negotiator_) != PJMEDIA_SDP_NEG_STATE_WAIT_NEGO)
        WARN("Negotiator not in right state for negotiation");

    if (pjmedia_sdp_neg_negotiate(memPool_, negotiator_, 0) != PJ_SUCCESS)
        return;

    if (pjmedia_sdp_neg_get_active_local(negotiator_, &active_local) != PJ_SUCCESS)
        ERROR("Could not retrieve local active session");
    else
        setActiveLocalSdpSession(active_local);

    if (pjmedia_sdp_neg_get_active_remote(negotiator_, &active_remote) != PJ_SUCCESS)
        ERROR("Could not retrieve remote active session");
    else
        setActiveRemoteSdpSession(active_remote);
}

namespace
{
    vector<string> split(const string &s, char delim)
    {
        vector<string> elems;
        stringstream ss(s);
        string item;
        while(getline(ss, item, delim))
            elems.push_back(item);
        return elems;
    }
} // end anonymous namespace

string Sdp::getLineFromSession(const pjmedia_sdp_session *sess, const string &keyword) const
{
    char buffer[2048];
    int size = pjmedia_sdp_print(sess, buffer, sizeof buffer);
    string sdp(buffer, size);
    const vector<string> tokens(split(sdp, '\n'));
    for (vector<string>::const_iterator iter = tokens.begin(); iter != tokens.end(); ++iter)
        if ((*iter).find(keyword) != string::npos)
            return *iter;
    return "";
}

string Sdp::getActiveIncomingVideoDescription() const
{
    stringstream ss;
    ss << "v=0" << std::endl;
    ss << "o=- 0 0 IN IP4 " << localIpAddr_ << std::endl;
    ss << "s=sflphone" << std::endl;
    ss << "c=IN IP4 " << remoteIpAddr_ << std::endl;
    ss << "t=0 0" << std::endl;

    std::string videoLine(getLineFromSession(activeLocalSession_, "m=video"));
    ss << videoLine << std::endl;

    int payload_num;
    if (sscanf(videoLine.c_str(), "m=video %*d %*s %d", &payload_num) != 1)
        payload_num = 0;

    std::ostringstream s;
    s << "a=rtpmap:";
    s << payload_num;

    std::string vCodecLine(getLineFromSession(activeLocalSession_, s.str()));
    ss << vCodecLine << std::endl;

    unsigned videoIdx;
    for (videoIdx = 0; videoIdx < activeLocalSession_->media_count and pj_stricmp2(&activeLocalSession_->media[videoIdx]->desc.media, "video") != 0; ++videoIdx)
        ;

    // get direction string
    static const pj_str_t DIRECTIONS[] = {
        {(char*) "sendrecv", 8},
        {(char*) "sendonly", 8},
        {(char*) "recvonly", 8},
        {(char*) "inactive", 8},
        {NULL, 0}
    };
    pjmedia_sdp_attr *direction = NULL;

    const pj_str_t *guess = DIRECTIONS;
    while (!direction and guess->ptr)
        direction = pjmedia_sdp_media_find_attr(activeLocalSession_->media[videoIdx], guess++, NULL);

    if (direction)
        ss << "a=" + std::string(direction->name.ptr, direction->name.slen) << std::endl;

    return ss.str();
}

std::string Sdp::getActiveOutgoingVideoCodec() const
{
    string str("a=rtpmap:");
    str += getActiveOutgoingVideoPayload();
    string vCodecLine(getLineFromSession(activeRemoteSession_, str));
    char codec_buf[32];
    codec_buf[0] = '\0';
    sscanf(vCodecLine.c_str(), "a=rtpmap:%*d %31[^/]", codec_buf);
    return string(codec_buf);
}

std::string Sdp::getActiveOutgoingVideoBitrate(const std::string &codec) const
{
    for (vector<map<string, string> >::const_iterator i = video_codec_list_.begin(); i != video_codec_list_.end(); ++i) {
        map<string, string>::const_iterator name = i->find("name");
        if (name != i->end() and (codec == name->second)) {
            map<string, string>::const_iterator bitrate = i->find("bitrate");
            if (bitrate != i->end())
                return bitrate->second;
        }
    }
    return "0";
}

std::string Sdp::getActiveOutgoingVideoPayload() const
{
    string videoLine(getLineFromSession(activeRemoteSession_, "m=video"));
    int payload_num;
    if (sscanf(videoLine.c_str(), "m=video %*d %*s %d", &payload_num) != 1)
        payload_num = 0;
    std::ostringstream os;
    os << payload_num;
    return os.str();
}

void Sdp::addSdesAttribute(const vector<std::string>& crypto)
{
    for (vector<std::string>::const_iterator iter = crypto.begin();
            iter != crypto.end(); ++iter) {
        pj_str_t val = { (char*)(*iter).c_str(), static_cast<pj_ssize_t>((*iter).size()) };
        pjmedia_sdp_attr *attr = pjmedia_sdp_attr_create(memPool_, "crypto", &val);

        for (unsigned i = 0; i < localSession_->media_count; i++)
            if (pjmedia_sdp_media_add_attr(localSession_->media[i], attr) != PJ_SUCCESS)
                throw SdpException("Could not add sdes attribute to media");
    }
}


void Sdp::addZrtpAttribute(pjmedia_sdp_media* media, std::string hash)
{
    /* Format: ":version value" */
    std::string val = "1.10 " + hash;
    pj_str_t value = { (char*)val.c_str(), static_cast<pj_ssize_t>(val.size()) };
    pjmedia_sdp_attr *attr = pjmedia_sdp_attr_create(memPool_, "zrtp-hash", &value);

    if (pjmedia_sdp_media_add_attr(media, attr) != PJ_SUCCESS)
        throw SdpException("Could not add zrtp attribute to media");
}

namespace {
    // Returns index of desired media attribute, or -1 if not found */
    int getIndexOfAttribute(const pjmedia_sdp_session * const session, const char * const type)
    {
        if (!session) {
            ERROR("Session is NULL when looking for \"%s\" attribute", type);
            return -1;
        }
        int i = 0;
        while (i < session->media_count and pj_stricmp2(&session->media[i]->desc.media, type) != 0)
            ++i;

        if (i == session->media_count)
            return -1;
        else
            return i;
    }
}

void Sdp::addAttributeToLocalAudioMedia(const char *attr)
{
    const int i = getIndexOfAttribute(localSession_, "audio");
    if (i == -1)
        return;
    pjmedia_sdp_attr *attribute = pjmedia_sdp_attr_create(memPool_, attr, NULL);
    pjmedia_sdp_media_add_attr(localSession_->media[i], attribute);
}

void Sdp::removeAttributeFromLocalAudioMedia(const char *attr)
{
    const int i = getIndexOfAttribute(localSession_, "audio");
    if (i == -1)
        return;
    pjmedia_sdp_media_remove_all_attr(localSession_->media[i], attr);
}

void Sdp::removeAttributeFromLocalVideoMedia(const char *attr)
{
    const int i = getIndexOfAttribute(localSession_, "video");
    if (i == -1)
        return;
    pjmedia_sdp_media_remove_all_attr(localSession_->media[i], attr);
}

void Sdp::addAttributeToLocalVideoMedia(const char *attr)
{
    const int i = getIndexOfAttribute(localSession_, "video");
    if (i == -1)
        return;
    pjmedia_sdp_attr *attribute = pjmedia_sdp_attr_create(memPool_, attr, NULL);
    pjmedia_sdp_media_add_attr(localSession_->media[i], attribute);
}

void Sdp::setMediaTransportInfoFromRemoteSdp()
{
    if (!activeRemoteSession_) {
        ERROR("Remote sdp is NULL while parsing media");
        return;
    }

    for (unsigned i = 0; i < activeRemoteSession_->media_count; ++i) {
        if (pj_stricmp2(&activeRemoteSession_->media[i]->desc.media, "audio") == 0) {
            remoteAudioPort_ = activeRemoteSession_->media[i]->desc.port;
            remoteIpAddr_ = std::string(activeRemoteSession_->conn->addr.ptr, activeRemoteSession_->conn->addr.slen);
        } else if (pj_stricmp2(&activeRemoteSession_->media[i]->desc.media, "video") == 0) {
            remoteVideoPort_ = activeRemoteSession_->media[i]->desc.port;
            remoteIpAddr_ = std::string(activeRemoteSession_->conn->addr.ptr, activeRemoteSession_->conn->addr.slen);
        }
    }
}

void Sdp::getRemoteSdpCryptoFromOffer(const pjmedia_sdp_session* remote_sdp, CryptoOffer& crypto_offer)
{
    for (unsigned i = 0; i < remote_sdp->media_count; ++i) {
        pjmedia_sdp_media *media = remote_sdp->media[i];

        for (unsigned j = 0; j < media->attr_count; j++) {
            pjmedia_sdp_attr *attribute = media->attr[j];

            // @TODO our parser require the "a=crypto:" to be present
            if (pj_stricmp2(&attribute->name, "crypto") == 0)
                crypto_offer.push_back("a=crypto:" + std::string(attribute->value.ptr, attribute->value.slen));
        }
    }
}
