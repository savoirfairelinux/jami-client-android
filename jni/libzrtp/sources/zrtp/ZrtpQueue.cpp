/*
  Copyright (C) 2006-2009 Werner Dittmann

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
 * Authors: Werner Dittmann <Werner.Dittmann@t-online.de>
 */

#include <string>
#include <stdio.h>

#include <ZrtpQueue.h>
#include <libzrtpcpp/ZIDCache.h>
#include <libzrtpcpp/ZRtp.h>
#include <libzrtpcpp/ZrtpStateClass.h>
#include <libzrtpcpp/ZrtpUserCallback.h>

static TimeoutProvider<std::string, ost::ZrtpQueue*>* staticTimeoutProvider = NULL;

NAMESPACE_COMMONCPP
using namespace GnuZrtpCodes;

ZrtpQueue::ZrtpQueue(uint32 size, RTPApplication& app) :
        AVPQueue(size,app)
{
    init();
}

ZrtpQueue::ZrtpQueue(uint32 ssrc, uint32 size, RTPApplication& app) :
        AVPQueue(ssrc,size,app)
{
    init();
}

void ZrtpQueue::init()
{
    zrtpUserCallback = NULL;
    enableZrtp = false;
    started = false;
    mitmMode = false;
    enableParanoidMode = false;
    zrtpEngine = NULL;
    senderZrtpSeqNo = 1;

    clientIdString = clientId;
    peerSSRC = 0;
}

ZrtpQueue::~ZrtpQueue() {

    endQueue();
    stopZrtp();

    if (zrtpUserCallback != NULL) {
        delete zrtpUserCallback;
        zrtpUserCallback = NULL;
    }
}

int32_t
ZrtpQueue::initialize(const char *zidFilename, bool autoEnable, ZrtpConfigure* config)
{
    int32_t ret = 1;

    synchEnter();

    ZrtpConfigure* configOwn = NULL;
    if (config == NULL) {
        config = configOwn = new ZrtpConfigure();
        config->setStandardConfig();
    }
    enableZrtp = autoEnable;

    config->setParanoidMode(enableParanoidMode);

    if (staticTimeoutProvider == NULL) {
        staticTimeoutProvider = new TimeoutProvider<std::string, ZrtpQueue*>();
        staticTimeoutProvider->start();
    }
    ZIDCache* zf = getZidCacheInstance();
    if (!zf->isOpen()) {
        std::string fname;
        if (zidFilename == NULL) {
            char *home = getenv("HOME");
            std::string baseDir = (home != NULL) ? (std::string(home) + std::string("/."))
                                                    : std::string(".");
            fname = baseDir + std::string("GNUZRTP.zid");
            zidFilename = fname.c_str();
        }
        if (zf->open((char *)zidFilename) < 0) {
            enableZrtp = false;
            ret = -1;
        }
    }
    if (ret > 0) {
        const uint8_t* ownZid = zf->getZid();
        zrtpEngine = new ZRtp((uint8_t*)ownZid, (ZrtpCallback*)this, clientIdString, config, mitmMode, signSas);
    }
    if (configOwn != NULL) {
        delete configOwn;
    }
    synchLeave();
    return ret;
}

void ZrtpQueue::startZrtp() {
    if (zrtpEngine != NULL) {
        zrtpEngine->startZrtpEngine();
        zrtpUnprotect = 0;
        started = true;
    }
}

void ZrtpQueue::stopZrtp() {
    if (zrtpEngine != NULL) {
        if (zrtpUnprotect < 50 && !zrtpEngine->isMultiStream())
            zrtpEngine->setRs2Valid();
        delete zrtpEngine;
        zrtpEngine = NULL;
        started = false;
    }
}

/*
 * The takeInDataPacket implementation for ZRTPQueue.
 */
size_t
ZrtpQueue::takeInDataPacket(void)
{
    InetHostAddress network_address;
    tpport_t transport_port;

    uint32 nextSize = (uint32)getNextDataPacketSize();
    unsigned char* buffer = new unsigned char[nextSize];
    int32 rtn = (int32)recvData(buffer, nextSize, network_address, transport_port);
    if ( (rtn < 0) || ((uint32)rtn > getMaxRecvPacketSize()) ){
        delete buffer;
        return 0;
    }

    IncomingZRTPPkt* packet = NULL;
    // check if this could be a real RTP/SRTP packet.
    if ((*buffer & 0xf0) != 0x10) {
        return (rtpDataPacket(buffer, rtn, network_address, transport_port));
    }

    // We assume all other packets are ZRTP packets here. Process
    // if ZRTP processing is enabled. Because valid RTP packets are
    // already handled we delete any packets here after processing.
    if (enableZrtp && zrtpEngine != NULL) {
        // Fixed header length + smallest ZRTP packet (includes CRC)
        if (rtn < (int32)(12 + sizeof(HelloAckPacket_t))) // data too small, dismiss
            return 0;

        // Get CRC value into crc (see above how to compute the offset)
        uint16_t temp = rtn - CRC_SIZE;
        uint32_t crc = *(uint32_t*)(buffer + temp);
        crc = ntohl(crc);

        if (!zrtpCheckCksum(buffer, temp, crc)) {
            delete buffer;
            if (zrtpUserCallback != NULL)
                zrtpUserCallback->showMessage(Warning, WarningCRCmismatch);
            return 0;
        }

        packet = new IncomingZRTPPkt(buffer,rtn);

        uint32 magic = packet->getZrtpMagic();

        // Check if it is really a ZRTP packet, if not delete it and return 0
        if (magic != ZRTP_MAGIC || zrtpEngine == NULL) {
            delete packet;
            return 0;
        }
        // cover the case if the other party sends _only_ ZRTP packets at the
        // beginning of a session. Start ZRTP in this case as well.
        if (!started) {
            startZrtp();
         }
        // this now points beyond the undefined and length field.
        // We need them, thus adjust
        unsigned char* extHeader =
                const_cast<unsigned char*>(packet->getHdrExtContent());
        extHeader -= 4;

        // store peer's SSRC, used when creating the CryptoContext
        peerSSRC = packet->getSSRC();
        zrtpEngine->processZrtpMessage(extHeader, peerSSRC, rtn);
    }
    delete packet;
    return 0;
}

size_t
ZrtpQueue::rtpDataPacket(unsigned char* buffer, int32 rtn, InetHostAddress network_address, tpport_t transport_port)
{
     // Special handling of padding to take care of encrypted content.
    // In case of SRTP the padding length field is also encrypted, thus
    // it gives a wrong length. Check and clear padding bit before
    // creating the RTPPacket. Will be set and re-computed after a possible
    // SRTP decryption.
    uint8 padSet = (*buffer & 0x20);
    if (padSet) {
        *buffer = *buffer & ~0x20;          // clear padding bit
    }
    //  build a packet. It will link itself to its source
    IncomingRTPPkt* packet =
        new IncomingRTPPkt(buffer,rtn);

    // Generic header validity check.
    if ( !packet->isHeaderValid() ) {
        delete packet;
        return 0;
    }

    // Look for a CryptoContext for this packet's SSRC
    CryptoContext* pcc = getInQueueCryptoContext(packet->getSSRC());

    // If no crypto context is available for this SSRC but we are already in
    // Secure state then create a CryptoContext for this SSRC.
    // Assumption: every SSRC stream sent via this connection is secured
    // _and_ uses the same crypto parameters.
    if (pcc == NULL) {
        pcc = getInQueueCryptoContext(0);
        if (pcc != NULL) {
            pcc = pcc->newCryptoContextForSSRC(packet->getSSRC(), 0, 0L);
            if (pcc != NULL) {
                pcc->deriveSrtpKeys(0);
                setInQueueCryptoContext(pcc);
            }
        }
    }
    // If no crypto context: then either ZRTP is off or in early state
    // If crypto context is available then unprotect data here. If an error
    // occurs report the error and discard the packet.
    if (pcc != NULL) {
        int32 ret;
        if ((ret = packet->unprotect(pcc)) < 0) {
            if (!onSRTPPacketError(*packet, ret)) {
                delete packet;
                return 0;
            }
        }
        if (started && zrtpEngine->inState(WaitConfAck)) {
            zrtpEngine->conf2AckSecure();
        }
    }

    // virtual for profile-specific validation and processing.
    if (!onRTPPacketRecv(*packet) ) {
        delete packet;
        return 0;
    }
    if (padSet) {
        packet->reComputePayLength(true);
    }
    // get time of arrival
    struct timeval recvtime;
    gettimeofday(&recvtime,NULL);

    bool source_created;
    SyncSourceLink* sourceLink =
            getSourceBySSRC(packet->getSSRC(),source_created);
    SyncSource* s = sourceLink->getSource();
    if ( source_created ) {
        // Set data transport address.
        setDataTransportPort(*s,transport_port);
        // Network address is assumed to be the same as the control one
        setNetworkAddress(*s,network_address);
        sourceLink->initStats();
        // First packet arrival time.
        sourceLink->setInitialDataTime(recvtime);
        sourceLink->setProbation(getMinValidPacketSequence());
        if ( sourceLink->getHello() )
            onNewSyncSource(*s);
    }
    else if ( 0 == s->getDataTransportPort() ) {
        // Test if RTCP packets had been received but this is the
        // first data packet from this source.
        setDataTransportPort(*s,transport_port);
    }

    // Before inserting in the queue,
    // 1) check for collisions and loops. If the packet cannot be
    //    assigned to a source, it will be rejected.
    // 2) check the source is a sufficiently well known source
    // TODO: also check CSRC identifiers.
    if (checkSSRCInIncomingRTPPkt(*sourceLink, source_created,
        network_address, transport_port) &&
        recordReception(*sourceLink,*packet,recvtime) ) {
        // now the packet link is linked in the queues
        IncomingRTPPktLink* packetLink = new IncomingRTPPktLink(packet, sourceLink, recvtime,
                                       packet->getTimestamp() - sourceLink->getInitialDataTimestamp(),
                                       NULL,NULL,NULL,NULL);
        insertRecvPacket(packetLink);
    } else {
        // must be discarded due to collision or loop or
        // invalid source
        delete packet;
        return 0;
    }
    // Start the ZRTP engine after we got a at least one RTP packet and
    // sent some as well or we are in multi-stream mode.
    if (!started && enableZrtp) {
        startZrtp();
    }
    return rtn;
}

bool
ZrtpQueue::onSRTPPacketError(IncomingRTPPkt& pkt, int32 errorCode)
{
    if (errorCode == -1) {
        sendInfo(Warning, WarningSRTPauthError);
    }
    else {
        sendInfo(Warning, WarningSRTPreplayError);
    }
    return false;
}


void
ZrtpQueue::putData(uint32 stamp, const unsigned char* data, size_t len)
{
    OutgoingDataQueue::putData(stamp, data, len);
}


void
ZrtpQueue::sendImmediate(uint32 stamp, const unsigned char* data, size_t len)
{
    OutgoingDataQueue::sendImmediate(stamp, data, len);
}


/*
 * Here the callback methods required by the ZRTP implementation
 */
int32_t ZrtpQueue::sendDataZRTP(const unsigned char *data, int32_t length) {

    OutgoingZRTPPkt* packet = new OutgoingZRTPPkt(data, length);

    packet->setSSRC(getLocalSSRC());

    packet->setSeqNum(senderZrtpSeqNo++);

    /*
     * Compute the ZRTP CRC over the full ZRTP packet. Thus include
     * the fixed packet header into the calculation.
     */
    uint16_t temp = packet->getRawPacketSize() - CRC_SIZE;
    uint8_t* pt = (uint8_t*)packet->getRawPacket();
    uint32_t crc = zrtpGenerateCksum(pt, temp);
    // convert and store CRC in crc field of ZRTP packet.
    crc = zrtpEndCksum(crc);

    // advance pointer to CRC storage
    pt += temp;
    *(uint32_t*)pt = htonl(crc);

    dispatchImmediate(packet);
    delete packet;

    return 1;
}

bool ZrtpQueue::srtpSecretsReady(SrtpSecret_t* secrets, EnableSecurity part)
{
    CryptoContext* recvCryptoContext;
    CryptoContext* senderCryptoContext;
    CryptoContextCtrl* recvCryptoContextCtrl;
    CryptoContextCtrl* senderCryptoContextCtrl;

    int cipher;
    int authn;
    int authKeyLen;

    if (secrets->authAlgorithm == Sha1) {
        authn = SrtpAuthenticationSha1Hmac;
        authKeyLen = 20;
    }

    if (secrets->authAlgorithm == Skein) {
        authn = SrtpAuthenticationSkeinHmac;
        authKeyLen = 32;
    }

    if (secrets->symEncAlgorithm == Aes)
        cipher = SrtpEncryptionAESCM;

    if (secrets->symEncAlgorithm == TwoFish)
        cipher = SrtpEncryptionTWOCM;

    if (part == ForSender) {
        // To encrypt packets: intiator uses initiator keys,
        // responder uses responder keys
        // Create a "half baked" crypto context first and store it. This is
        // the main crypto context for the sending part of the connection.
        if (secrets->role == Initiator) {
            senderCryptoContext = new CryptoContext(
                    0,
                    0,
                    0L,                                      // keyderivation << 48,
                    cipher,                                  // encryption algo
                    authn,                                   // authtentication algo
                    (unsigned char*)secrets->keyInitiator,   // Master Key
                    secrets->initKeyLen / 8,                 // Master Key length
                    (unsigned char*)secrets->saltInitiator,  // Master Salt
                    secrets->initSaltLen / 8,                // Master Salt length
                    secrets->initKeyLen / 8,                 // encryption keyl
                    authKeyLen,                              // authentication key len
                    secrets->initSaltLen / 8,                // session salt len
                    secrets->srtpAuthTagLen / 8);            // authentication tag lenA
            senderCryptoContextCtrl = new CryptoContextCtrl(0,
                  cipher,                                    // encryption algo
                  authn,                                     // authtication algo
                  (unsigned char*)secrets->keyInitiator,     // Master Key
                  secrets->initKeyLen / 8,                   // Master Key length
                  (unsigned char*)secrets->saltInitiator,    // Master Salt
                  secrets->initSaltLen / 8,                  // Master Salt length
                  secrets->initKeyLen / 8,                   // encryption keyl
                  authKeyLen,                                // authentication key len
                  secrets->initSaltLen / 8,                  // session salt len
                  secrets->srtpAuthTagLen / 8);              // authentication tag len
        }
        else {
            senderCryptoContext = new CryptoContext(
                    0,
                    0,
                    0L,                                      // keyderivation << 48,
                    cipher,                                  // encryption algo
                    authn,                                   // authtentication algo
                    (unsigned char*)secrets->keyResponder,   // Master Key
                    secrets->respKeyLen / 8,                 // Master Key length
                    (unsigned char*)secrets->saltResponder,  // Master Salt
                    secrets->respSaltLen / 8,                // Master Salt length
                    secrets->respKeyLen / 8,                 // encryption keyl
                    authKeyLen,                              // authentication key len
                    secrets->respSaltLen / 8,                // session salt len
                    secrets->srtpAuthTagLen / 8);            // authentication tag len
            senderCryptoContextCtrl = new CryptoContextCtrl(0,
                  cipher,                                    // encryption algo
                  authn,                                     // authtication algo
                  (unsigned char*)secrets->keyResponder,     // Master Key
                  secrets->respKeyLen / 8,                   // Master Key length
                  (unsigned char*)secrets->saltResponder,    // Master Salt
                  secrets->respSaltLen / 8,                  // Master Salt length
                  secrets->respKeyLen / 8,                   // encryption keyl
                  authKeyLen,                                // authentication key len
                  secrets->respSaltLen / 8,                  // session salt len
                  secrets->srtpAuthTagLen / 8);              // authentication tag len
        }
        if (senderCryptoContext == NULL) {
            return false;
        }
        // Insert the Crypto templates (SSRC == 0) into the queue. When we send
        // the first RTP or RTCP packet the real crypto context will be created.
        // Refer to putData(), sendImmediate() in ccrtp's outqueue.cpp and
        // takeinControlPacket() in ccrtp's control.cpp.
        //
         setOutQueueCryptoContext(senderCryptoContext);
         setOutQueueCryptoContextCtrl(senderCryptoContextCtrl);
    }
    if (part == ForReceiver) {
        // To decrypt packets: intiator uses responder keys,
        // responder initiator keys
        // See comment above.
        if (secrets->role == Initiator) {
            recvCryptoContext = new CryptoContext(
                    0,
                    0,
                    0L,                                      // keyderivation << 48,
                    cipher,                                  // encryption algo
                    authn,                                   // authtentication algo
                    (unsigned char*)secrets->keyResponder,   // Master Key
                    secrets->respKeyLen / 8,                 // Master Key length
                    (unsigned char*)secrets->saltResponder,  // Master Salt
                    secrets->respSaltLen / 8,                // Master Salt length
                    secrets->respKeyLen / 8,                 // encryption keyl
                    authKeyLen,                              // authentication key len
                    secrets->respSaltLen / 8,                // session salt len
                    secrets->srtpAuthTagLen / 8);            // authentication tag len
            recvCryptoContextCtrl = new CryptoContextCtrl(0,
                  cipher,                                    // encryption algo
                  authn,                                     // authtication algo
                  (unsigned char*)secrets->keyResponder,     // Master Key
                  secrets->respKeyLen / 8,                   // Master Key length
                  (unsigned char*)secrets->saltResponder,    // Master Salt
                  secrets->respSaltLen / 8,                  // Master Salt length
                  secrets->respKeyLen / 8,                   // encryption keyl
                  authKeyLen,                                // authentication key len
                  secrets->respSaltLen / 8,                  // session salt len
                  secrets->srtpAuthTagLen / 8);              // authentication tag len

        }
        else {
            recvCryptoContext = new CryptoContext(
                    0,
                    0,
                    0L,                                      // keyderivation << 48,
                    cipher,                                  // encryption algo
                    authn,                                   // authtentication algo
                    (unsigned char*)secrets->keyInitiator,   // Master Key
                    secrets->initKeyLen / 8,                 // Master Key length
                    (unsigned char*)secrets->saltInitiator,  // Master Salt
                    secrets->initSaltLen / 8,                // Master Salt length
                    secrets->initKeyLen / 8,                 // encryption keyl
                    authKeyLen,                              // authentication key len
                    secrets->initSaltLen / 8,                // session salt len
                    secrets->srtpAuthTagLen / 8);            // authentication tag len
            recvCryptoContextCtrl = new CryptoContextCtrl(0,
                  cipher,                                    // encryption algo
                  authn,                                     // authtication algo
                  (unsigned char*)secrets->keyInitiator,     // Master Key
                  secrets->initKeyLen / 8,                   // Master Key length
                  (unsigned char*)secrets->saltInitiator,    // Master Salt
                  secrets->initSaltLen / 8,                  // Master Salt length
                  secrets->initKeyLen / 8,                   // encryption keyl
                  authKeyLen,                                // authentication key len
                  secrets->initSaltLen / 8,                  // session salt len
                  secrets->srtpAuthTagLen / 8);              // authentication tag len
        }
        if (recvCryptoContext == NULL) {
            return false;
        }
        // Insert the Crypto templates (SSRC == 0) into the queue. When we receive
        // the first RTP or RTCP packet the real crypto context will be created.
        // Refer to rtpDataPacket() above and takeinControlPacket in ccrtp's control.cpp.
        //
        setInQueueCryptoContext(recvCryptoContext);
        setInQueueCryptoContextCtrl(recvCryptoContextCtrl);
    }
    return true;
}

void ZrtpQueue::srtpSecretsOn(std::string c, std::string s, bool verified)
{

  if (zrtpUserCallback != NULL) {
    zrtpUserCallback->secureOn(c);
    if (!s.empty()) {
        zrtpUserCallback->showSAS(s, verified);
    }
  }
}

void ZrtpQueue::srtpSecretsOff(EnableSecurity part) {
    if (part == ForSender) {
        removeOutQueueCryptoContext(NULL);
        removeOutQueueCryptoContextCtrl(NULL);
    }
    if (part == ForReceiver) {
        removeInQueueCryptoContext(NULL);
        removeInQueueCryptoContextCtrl(NULL);
    }
    if (zrtpUserCallback != NULL) {
        zrtpUserCallback->secureOff();
    }
}

int32_t ZrtpQueue::activateTimer(int32_t time) {
    std::string s("ZRTP");
    if (staticTimeoutProvider != NULL) {
        staticTimeoutProvider->requestTimeout(time, this, s);
    }
    return 1;
}

int32_t ZrtpQueue::cancelTimer() {
    std::string s("ZRTP");
    if (staticTimeoutProvider != NULL) {
        staticTimeoutProvider->cancelRequest(this, s);
    }
    return 1;
}

void ZrtpQueue::handleTimeout(const std::string &c) {
    if (zrtpEngine != NULL) {
        zrtpEngine->processTimeout();
    }
}

void ZrtpQueue::handleGoClear()
{
    fprintf(stderr, "Need to process a GoClear message!");
}

void ZrtpQueue::sendInfo(MessageSeverity severity, int32_t subCode) {
    if (zrtpUserCallback != NULL) {
        zrtpUserCallback->showMessage(severity, subCode);
    }
}

void ZrtpQueue::zrtpNegotiationFailed(MessageSeverity severity, int32_t subCode) {
    if (zrtpUserCallback != NULL) {
        zrtpUserCallback->zrtpNegotiationFailed(severity, subCode);
    }
}

void ZrtpQueue::zrtpNotSuppOther() {
    if (zrtpUserCallback != NULL) {
        zrtpUserCallback->zrtpNotSuppOther();
    }
}

void ZrtpQueue::synchEnter() {
    synchLock.enter();
}

void ZrtpQueue::synchLeave() {
    synchLock.leave();
}

void ZrtpQueue::zrtpAskEnrollment(GnuZrtpCodes::InfoEnrollment  info) {
    if (zrtpUserCallback != NULL) {
        zrtpUserCallback->zrtpAskEnrollment(info);
    }
}

void ZrtpQueue::zrtpInformEnrollment(GnuZrtpCodes::InfoEnrollment  info) {
    if (zrtpUserCallback != NULL) {
        zrtpUserCallback->zrtpInformEnrollment(info);
    }
}

void ZrtpQueue::signSAS(uint8_t* sasHash) {
    if (zrtpUserCallback != NULL) {
        zrtpUserCallback->signSAS(sasHash);
    }
}

bool ZrtpQueue::checkSASSignature(uint8_t* sasHash) {
    if (zrtpUserCallback != NULL) {
        return zrtpUserCallback->checkSASSignature(sasHash);
    }
    return false;
}

void ZrtpQueue::setEnableZrtp(bool onOff)   {
    enableZrtp = onOff;
}

bool ZrtpQueue::isEnableZrtp() {
    return enableZrtp;
}

void ZrtpQueue::SASVerified() {
    if (zrtpEngine != NULL)
        zrtpEngine->SASVerified();
}

void ZrtpQueue::resetSASVerified() {
    if (zrtpEngine != NULL)
        zrtpEngine->resetSASVerified();
}

void ZrtpQueue::goClearOk()    {  }

void ZrtpQueue::requestGoClear()  { }

void ZrtpQueue::setAuxSecret(uint8* data, int32_t length)  {
    if (zrtpEngine != NULL)
        zrtpEngine->setAuxSecret(data, length);
}

void ZrtpQueue::setUserCallback(ZrtpUserCallback* ucb) {
    zrtpUserCallback = ucb;
}

void ZrtpQueue::setClientId(std::string id) {
    clientIdString = id;
}

std::string ZrtpQueue::getHelloHash(int32_t index)  {
    if (zrtpEngine != NULL)
        return zrtpEngine->getHelloHash(index);
    else
        return std::string();
}

std::string ZrtpQueue::getPeerHelloHash()  {
    if (zrtpEngine != NULL)
        return zrtpEngine->getPeerHelloHash();
    else
        return std::string();
}

std::string ZrtpQueue::getMultiStrParams()  {
    if (zrtpEngine != NULL)
        return zrtpEngine->getMultiStrParams();
    else
        return std::string();
}

void ZrtpQueue::setMultiStrParams(std::string parameters)  {
    if (zrtpEngine != NULL)
        zrtpEngine->setMultiStrParams(parameters);
}

bool ZrtpQueue::isMultiStream()  {
    if (zrtpEngine != NULL)
        return zrtpEngine->isMultiStream();
    return false;
}

bool ZrtpQueue::isMultiStreamAvailable()  {
    if (zrtpEngine != NULL)
        return zrtpEngine->isMultiStreamAvailable();
    return false;
}

void ZrtpQueue::acceptEnrollment(bool accepted) {
    if (zrtpEngine != NULL)
        zrtpEngine->acceptEnrollment(accepted);
}

std::string ZrtpQueue::getSasType() {
    if (zrtpEngine != NULL)
        return zrtpEngine->getSasType();
    else
        return NULL;
}

uint8_t* ZrtpQueue::getSasHash() {
    if (zrtpEngine != NULL)
        return zrtpEngine->getSasHash();
    else
        return NULL;
}

bool ZrtpQueue::sendSASRelayPacket(uint8_t* sh, std::string render) {

    if (zrtpEngine != NULL)
        return zrtpEngine->sendSASRelayPacket(sh, render);
    else
        return false;
}

bool ZrtpQueue::isMitmMode() {
    return mitmMode;
}

void ZrtpQueue::setMitmMode(bool mitmMode) {
    this->mitmMode = mitmMode;
}

bool ZrtpQueue::isEnrollmentMode() {
    if (zrtpEngine != NULL)
        return zrtpEngine->isEnrollmentMode();
    else
        return false;
}

void ZrtpQueue::setEnrollmentMode(bool enrollmentMode) {
    if (zrtpEngine != NULL)
        zrtpEngine->setEnrollmentMode(enrollmentMode);
}

void ZrtpQueue::setParanoidMode(bool yesNo) {
        enableParanoidMode = yesNo;
}

bool ZrtpQueue::isParanoidMode() {
        return enableParanoidMode;
}

bool ZrtpQueue::isPeerEnrolled() {
    if (zrtpEngine != NULL)
        return zrtpEngine->isPeerEnrolled();
    else
        return false;
}

void ZrtpQueue::setSignSas(bool sasSignMode) {
    signSas = sasSignMode;
}

bool ZrtpQueue::setSignatureData(uint8* data, int32 length) {
    if (zrtpEngine != NULL)
        return zrtpEngine->setSignatureData(data, length);
    return 0;
}

const uint8* ZrtpQueue::getSignatureData() {
    if (zrtpEngine != NULL)
        return zrtpEngine->getSignatureData();
    return 0;
}

int32 ZrtpQueue::getSignatureLength() {
    if (zrtpEngine != NULL)
        return zrtpEngine->getSignatureLength();
    return 0;
}

int32 ZrtpQueue::getPeerZid(uint8* data) {
    if (data == NULL)
        return 0;

    if (zrtpEngine != NULL)
        return zrtpEngine->getPeerZid(data);

    return 0;
}

int32_t ZrtpQueue::getNumberSupportedVersions() {
    if (zrtpEngine != NULL)
        return zrtpEngine->getNumberSupportedVersions();

    return 0;
}

int32_t ZrtpQueue::getCurrentProtocolVersion() {
    if (zrtpEngine != NULL)
        return zrtpEngine->getCurrentProtocolVersion();

    return 0;
}


IncomingZRTPPkt::IncomingZRTPPkt(const unsigned char* const block, size_t len) :
        IncomingRTPPkt(block,len) {
}

uint32 IncomingZRTPPkt::getZrtpMagic() const {
     return ntohl(getHeader()->timestamp);
}

uint32 IncomingZRTPPkt::getSSRC() const {
     return ntohl(getHeader()->sources[0]);
}

OutgoingZRTPPkt::OutgoingZRTPPkt(
    const unsigned char* const hdrext, uint32 hdrextlen) :
        OutgoingRTPPkt(NULL, 0, hdrext, hdrextlen, NULL ,0, 0, NULL)
{
    getHeader()->version = 0;
    getHeader()->timestamp = htonl(ZRTP_MAGIC);
}

END_NAMESPACE

/** EMACS **
 * Local variables:
 * mode: c++
 * c-default-style: ellemtel
 * c-basic-offset: 4
 * End:
 */

