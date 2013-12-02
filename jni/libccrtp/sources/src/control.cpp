// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// ccRTP.  If you copy code from other releases into a copy of GNU
// ccRTP, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU ccRTP, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

/**
 * @file control.cpp
 *
 * @short QueueRTCPManager classes implementation.
 **/

#include "private.h"
#include <cstdlib>
#include <ccrtp/cqueue.h>
#include <cstdlib>
#include <climits>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

const uint16 QueueRTCPManager::TIMEOUT_MULTIPLIER = 5;
const double QueueRTCPManager::RECONSIDERATION_COMPENSATION = 2.718281828 - 1.5;
const SDESItemType QueueRTCPManager::firstSchedulable = SDESItemTypeNAME;
const SDESItemType QueueRTCPManager::lastSchedulable = SDESItemTypePRIV;
/// maximum end to end delay: unlimited
const microtimeout_t QueueRTCPManager::defaultEnd2EndDelay = 0;

QueueRTCPManager::QueueRTCPManager(uint32 size, RTPApplication& app):
RTPDataQueue(size), RTCPCompoundHandler(RTCPCompoundHandler::defaultPathMTU),
queueApplication(app)
{
    controlServiceActive = false;
    controlBwFract = 0.05f;
    sendControlBwFract = 0.25;
    recvControlBwFract = 1-sendControlBwFract;
    ctrlSendCount = 0;

    lowerHeadersSize = networkHeaderSize() + transportHeaderSize();

    nextScheduledSDESItem = SDESItemTypeNAME;

    // initialize RTCP timing
    reconsInfo.rtcpTp.tv_sec = reconsInfo.rtcpTc.tv_sec =
        reconsInfo.rtcpTn.tv_sec = 0;
    reconsInfo.rtcpTp.tv_usec = reconsInfo.rtcpTc.tv_usec =
        reconsInfo.rtcpTn.tv_usec = 0;
    reconsInfo.rtcpPMembers = 1;

    rtcpWeSent = false;
    rtcpAvgSize = sizeof(RTCPFixedHeader) + sizeof(uint32) +
        sizeof(SenderInfo);
    rtcpInitial = true;
    // force an initial check for incoming RTCP packets
    gettimeofday(&rtcpNextCheck,NULL);
    // check for incoming RTCP packets every 1/4 seconds.
    rtcpCheckInterval.tv_sec = 0;
    rtcpCheckInterval.tv_usec = 250000;
    timersub(&rtcpNextCheck,&rtcpCheckInterval,&rtcpLastCheck);

    lastSendPacketCount = 0;

    rtcpMinInterval = 5000000;  // 5 seconds.

    leavingDelay = 1000000; // 1 second
    end2EndDelay = getDefaultEnd2EndDelay();

    // Fill in fixed fields that will never change
    RTCPPacket* pkt = reinterpret_cast<RTCPPacket*>(rtcpSendBuffer);
    pkt->fh.version = CCRTP_VERSION;
    // (SSRCCollision will have to take this into account)
    pkt->info.SR.ssrc = getLocalSSRCNetwork();

    // allow to start RTCP service once everything is set up
    controlServiceActive = true;
}

// TODO Streamline this code (same as above, put into a separate method)
QueueRTCPManager::QueueRTCPManager(uint32 ssrc, uint32 size, RTPApplication& app):
RTPDataQueue(&ssrc, size),
RTCPCompoundHandler(RTCPCompoundHandler::defaultPathMTU),
queueApplication(app)
{
    controlServiceActive = false;
    controlBwFract = 0.05f;
    sendControlBwFract = 0.25;
    recvControlBwFract = 1-sendControlBwFract;
    ctrlSendCount = 0;

    lowerHeadersSize = networkHeaderSize() + transportHeaderSize();

    nextScheduledSDESItem = SDESItemTypeNAME;

    // initialize RTCP timing
    reconsInfo.rtcpTp.tv_sec = reconsInfo.rtcpTc.tv_sec =
        reconsInfo.rtcpTn.tv_sec = 0;

    reconsInfo.rtcpTp.tv_usec = reconsInfo.rtcpTc.tv_usec =
        reconsInfo.rtcpTn.tv_usec = 0;

    reconsInfo.rtcpPMembers = 1;

    rtcpWeSent = false;
    rtcpAvgSize = sizeof(RTCPFixedHeader) + sizeof(uint32) + sizeof(SenderInfo);
    rtcpInitial = true;
    // force an initial check for incoming RTCP packets
    gettimeofday(&rtcpNextCheck,NULL);
    // check for incoming RTCP packets every 1/4 seconds.
    rtcpCheckInterval.tv_sec = 0;
    rtcpCheckInterval.tv_usec = 250000;
    timersub(&rtcpNextCheck,&rtcpCheckInterval,&rtcpLastCheck);

    lastSendPacketCount = 0;

    rtcpMinInterval = 5000000;  // 5 seconds.

    leavingDelay = 1000000; // 1 second
    end2EndDelay = getDefaultEnd2EndDelay();

    // Fill in fixed fields that will never change
    RTCPPacket* pkt = reinterpret_cast<RTCPPacket*>(rtcpSendBuffer);
    pkt->fh.version = CCRTP_VERSION;
    // (SSRCCollision will have to take this into account)
    pkt->info.SR.ssrc = getLocalSSRCNetwork();

    // allow to start RTCP service once everything is set up
    controlServiceActive = true;
}

QueueRTCPManager::~QueueRTCPManager()
{
    endQueueRTCPManager();
}

void QueueRTCPManager::endQueueRTCPManager()
{
    controlServiceActive = false;
    controlBwFract = sendControlBwFract = 0;
}

bool QueueRTCPManager::checkSSRCInRTCPPkt(SyncSourceLink& sourceLink,
bool is_new, InetAddress& network_address, tpport_t transport_port)
{
    bool result = true;

    // Test if the source is new and it is not the local one.
    if ( is_new && sourceLink.getSource()->getID() != getLocalSSRC() )
        return result;

    SyncSource *s = sourceLink.getSource();
    if ( s->getControlTransportPort() != transport_port ||
         s->getNetworkAddress() != network_address ) {
        // SSRC collision or a loop has happened
        if ( s->getID() != getLocalSSRC() ) {
            // TODO: Optional error counter.

            // Note this differs from the default in the RFC.
            // Discard packet only when the collision is
            // repeating (to avoid flip-flopping)
            if ( sourceLink.getPrevConflict() &&
                 (
                  (network_address ==
                   sourceLink.getPrevConflict()->networkAddress)
                  &&
                  (transport_port ==
                   sourceLink.getPrevConflict()->controlTransportPort)
                  ) ) {
                // discard packet and do not flip-flop
                result = false;
            } else {
                // Record who has collided so that in
                // the future we can how if the
                // collision repeats.
                sourceLink.setPrevConflict(network_address,
                               0,transport_port);
                // Change sync source transport address
                setControlTransportPort(*s,transport_port);
                setNetworkAddress(*s,network_address);
            }

        } else {
            // Collision or loop of own packets.
            ConflictingTransportAddress* conflicting =
                searchControlConflict(network_address,
                              transport_port);
            if ( conflicting ) {
                // Optional error counter.
                updateConflict(*conflicting);
                result = false;
            } else {
                // New collision
                addConflict(s->getNetworkAddress(),
                        s->getDataTransportPort(),
                        s->getControlTransportPort());
                dispatchBYE("SSRC collision detected when receiving RTCP packet");
                renewLocalSSRC();
                setNetworkAddress(*s,network_address);
                setControlTransportPort(*s,transport_port);
                setControlTransportPort(*s,0);
                sourceLink.initStats();
            }
        }
    }
    return result;
}

void QueueRTCPManager::controlReceptionService()
{
    if ( !controlServiceActive )
        return;

    // A) see if there are incoming RTCP packets
    gettimeofday(&(reconsInfo.rtcpTc),NULL);
    if ( timercmp(&(reconsInfo.rtcpTc),&rtcpNextCheck,>=) ) {
        while ( isPendingControl(0) )
            takeInControlPacket();
        // If this do loops more than once, then we have not
        // been in time. So it skips until the next future
        // instant.
        do {
            timeval tmp = rtcpNextCheck;
            timeradd(&rtcpLastCheck,&rtcpCheckInterval,
                 &rtcpNextCheck);
            rtcpLastCheck = tmp;
        } while ( timercmp(&(reconsInfo.rtcpTc), &(rtcpNextCheck), >=) );
    }
}

void QueueRTCPManager::controlTransmissionService()
{
    if ( !controlServiceActive )
        return;

    // B) send RTCP packets
    gettimeofday(&(reconsInfo.rtcpTc),NULL);
    if ( timercmp(&(reconsInfo.rtcpTc),&(reconsInfo.rtcpTn),>=) ) {
        if ( timerReconsideration() ) {
            // this would update to last received RTCP packets
            //while ( isPendingControl(0) )
            //  takeInControlPacket();
            rtcpLastCheck = reconsInfo.rtcpTc;
            dispatchControlPacket();
            if (rtcpInitial)
                rtcpInitial = false;
            expireSSRCs();
            reconsInfo.rtcpTp = reconsInfo.rtcpTc;
            // we have updated tp and sent a report, so we
            // have to recalculate the sending interval
            timeval T = computeRTCPInterval();
            timeradd(&(reconsInfo.rtcpTc),&T,&(reconsInfo.rtcpTn));

            // record current number of members for the
            // next check.
            reconsInfo.rtcpPMembers = getMembersCount();
        }
    }
}

bool QueueRTCPManager::timerReconsideration()
{
    bool result = false;
    // compute again the interval to confirm it under current
    // circumstances
    timeval T = computeRTCPInterval();
    timeradd(&(reconsInfo.rtcpTp),&T,&(reconsInfo.rtcpTn));
    gettimeofday(&(reconsInfo.rtcpTc),NULL);
    if ( timercmp(&(reconsInfo.rtcpTc),&(reconsInfo.rtcpTn),>=) ) {
        reconsInfo.rtcpTp = reconsInfo.rtcpTc;
        result = true;
    }
    return result;
}

void
QueueRTCPManager::expireSSRCs()
{}

void
QueueRTCPManager::takeInControlPacket()
{
    size_t len = 0;
    InetHostAddress network_address;
    tpport_t transport_port;
    len = recvControl(rtcpRecvBuffer,getPathMTU(),network_address, transport_port);

    // get time of arrival
    struct timeval recvtime;
    gettimeofday(&recvtime,NULL);

    // process a 'len' octets long RTCP compound packet

    // Check validity of the header fields of the compound packet
    if ( !RTCPCompoundHandler::checkCompoundRTCPHeader(len) )
        return;

    RTCPPacket *pkt = reinterpret_cast<RTCPPacket *>(rtcpRecvBuffer);

    // TODO: for now, we do nothing with the padding bit
    // in the header.

    bool source_created;
    SyncSourceLink* sourceLink = getSourceBySSRC(pkt->getSSRC(),source_created);
    SyncSource* s = sourceLink->getSource();

    if ( source_created ) {
        // Set control transport address.
        setControlTransportPort(*s,transport_port);
        // Network address is assumed to be the same as the control one
        setNetworkAddress(*s,network_address);
        sourceLink->initStats();
        sourceLink->setProbation(getMinValidPacketSequence());
        if ( sourceLink->getHello() )
            onNewSyncSource(*s);
    } else if ( s->getControlTransportPort() == 0 ) {
        // Test if RTP data packets had been received but this
        // is the first control packet from this source.
        setControlTransportPort(*s,transport_port);
    }
    // record reception time
    sourceLink->lastRTCPPacketTime = recvtime;
    sourceLink->lastRTCPSRTime = recvtime;

    size_t pointer = 0;
    // Check the first packet is a report and do special
    // processing for SR reports.
    if ( RTCPPacket::tRR == pkt->fh.type ) {
        // no special initialization is required for
        // RR reports, all reports will be processed
        // in the do-while down here.
    } else if ( RTCPPacket::tSR == pkt->fh.type ){
        if ( checkSSRCInRTCPPkt(*sourceLink,source_created,
                    network_address,
                    transport_port) )
            sourceLink->lastRTCPSRTime = recvtime;
            onGotSR(*s,pkt->info.SR,pkt->fh.block_count);
        // Advance to the next packet in the compound.
        pointer += pkt->getLength();
        pkt = reinterpret_cast<RTCPPacket *>(rtcpRecvBuffer +pointer);
    } else if ( RTCPPacket::tXR == pkt->fh.type ) {
        // TODO: handle XR reports.
    } else {
        // Ignore RTCP types unknown.
    }

    // Process all RR reports.
    while ( (pointer < len) && (RTCPPacket::tRR == pkt->fh.type) ) {
        sourceLink = getSourceBySSRC(pkt->getSSRC(),
                         source_created);
        if ( checkSSRCInRTCPPkt(*sourceLink,source_created,
                    network_address,transport_port) )
            onGotRR(*s,pkt->info.RR,pkt->fh.block_count);
        // Advance to the next packet in the compound
        pointer += pkt->getLength();
        pkt = reinterpret_cast<RTCPPacket *>(rtcpRecvBuffer +pointer);
    }

    // SDES, APP and BYE. process first everything but the
    // BYE packets.
    bool cname_found = false;
    while ( (pointer < len ) &&
            (pkt->fh.type == RTCPPacket::tSDES ||
         pkt->fh.type == RTCPPacket::tAPP) ) {
        I ( cname_found || !pkt->fh.padding );
        sourceLink = getSourceBySSRC(pkt->getSSRC(),
                         source_created);
        if ( checkSSRCInRTCPPkt(*sourceLink,source_created,
                    network_address,
                    transport_port) ) {
            if ( pkt->fh.type == RTCPPacket::tSDES ) {
                bool cname = onGotSDES(*s,*pkt);
                cname_found = cname_found? cname_found : cname;
            } else if ( pkt->fh.type == RTCPPacket::tAPP ) {
                onGotAPP(*s,pkt->info.APP,pkt->getLength());
        //      pointer += pkt->getLength();
            } else {
                // error?
            }
        }
        // Get the next packet in the compound.
        pointer += pkt->getLength();
        pkt = reinterpret_cast<RTCPPacket *>(rtcpRecvBuffer +pointer);
    }

    // TODO: error? if !cname_found

    // process BYE packets
    while ( pointer < len ) {
        if ( pkt->fh.type == RTCPPacket::tBYE ) {
            sourceLink = getSourceBySSRC(pkt->getSSRC(),
                             source_created);
            if ( checkSSRCInRTCPPkt(*sourceLink,source_created,
                        network_address,
                        transport_port) )
                getBYE(*pkt,pointer,len);
        } else if ( pkt->fh.type != RTCPPacket::tBYE ) {
            break; // TODO: check non-BYE out of place.
        } else {
            break;
        }
    }

    // Call plug-in in case there are profile extensions
    // at the end of the SR/RR.
    if ( pointer != len ) {
        onGotRRSRExtension(rtcpRecvBuffer + pointer,
                   len - pointer);
    }

    // Everything went right, update the RTCP average size
    updateAvgRTCPSize(len);
}

bool QueueRTCPManager::end2EndDelayed(IncomingRTPPktLink& pl)
{
    bool result = false;

    if ( 0 != getEnd2EndDelay() ) {
        SyncSourceLink* sl = pl.getSourceLink();
        void* si = sl->getSenderInfo();
        if ( NULL != si ) {
            RTCPSenderInfo rsi(si);
            uint32 tsInc = pl.getPacket()->getTimestamp() -
                rsi.getRTPTimestamp();
            // approx.
            microtimeout_t Inc = tsInc * 1000 /
                (getCurrentRTPClockRate() / 1000);
            timeval timevalInc = microtimeout2Timeval(Inc);

            timeval tNTP = NTP2Timeval(rsi.getNTPTimestampInt(),
                        rsi.getNTPTimestampFrac());
            timeval packetTime;
            timeradd(&tNTP,&timevalInc,&packetTime);
            timeval now, diff;
            gettimeofday(&now,NULL);
            timersub(&now,&packetTime,&diff);

            if ( timeval2microtimeout(diff) > getEnd2EndDelay() )
                result = true;
        }
    }
    return result;
}

void QueueRTCPManager::onGotSR(SyncSource& source, SendReport& SR, uint8)
{
    // We ignore the receiver blocks and just get the sender info
    // at the beginning of the SR.
    getLink(source)->setSenderInfo(reinterpret_cast<unsigned char*>(&(SR.sinfo)));
}

void QueueRTCPManager::onGotRR(SyncSource& source, RecvReport& RR, uint8 blocks)
{
    for ( uint8 i = 0; i < blocks; i++) {
        // this generic RTCP manager ignores reports about
        // other sources than the local one
        if ( getLocalSSRCNetwork() == RR.ssrc ) {
            getLink(source)->
                setReceiverInfo
                (reinterpret_cast<unsigned char*>(&(RR.blocks[i].rinfo)));
        }
    }
}

void QueueRTCPManager::updateAvgRTCPSize(size_t len)
{
    size_t newlen = len;
    newlen += lowerHeadersSize;
    rtcpAvgSize = (uint16)(( (15 * rtcpAvgSize) >> 4 ) + ( newlen >> 4));
}

bool QueueRTCPManager::getBYE(RTCPPacket& pkt, size_t& pointer, size_t)
{
    if ( 0 == pkt.fh.block_count )
        return false;

    char *reason = NULL;

    if ( (sizeof(RTCPFixedHeader) + pkt.fh.block_count * sizeof(uint32))
         < pkt.getLength() ) {
        uint16 endpointer = (uint16)(pointer + sizeof(RTCPFixedHeader) +
            pkt.fh.block_count * sizeof(uint32));
        uint16 len = rtcpRecvBuffer[endpointer];
        reason = new char[len + 1];
        memcpy(reason,rtcpRecvBuffer + endpointer + 1,len);
        reason[len] = '\0';
    } else { // avoid dangerous conversion of NULL to a C++ string.
        reason = new char[1];
        reason[0] = '\0';
    }

    int i = 0;
    while ( i < pkt.fh.block_count ) {
        bool created;
        SyncSourceLink* srcLink =
            getSourceBySSRC(pkt.getSSRC(),created);
        i++;
        if( srcLink->getGoodbye() )
            onGotGoodbye(*(srcLink->getSource()),reason);
        BYESource(pkt.getSSRC());
        setState(*(srcLink->getSource()),SyncSource::stateLeaving);

        reverseReconsideration();
    }

    delete [] reason;
    pointer += pkt.getLength();
    return true;
}

void QueueRTCPManager::reverseReconsideration()
{
    if ( getMembersCount() < reconsInfo.rtcpPMembers ) {
        timeval inc;

        // reconsider reconsInfo.rtcpTn (time for next RTCP packet)
        microtimeout_t t =
            (reconsInfo.rtcpTn.tv_sec - reconsInfo.rtcpTc.tv_sec) *
            1000000 +
            (reconsInfo.rtcpTn.tv_usec - reconsInfo.rtcpTc.tv_usec);
        t *= getMembersCount();
        t /= reconsInfo.rtcpPMembers;
        inc.tv_usec = t % 1000000;
        inc.tv_sec = t / 1000000;
        timeradd(&(reconsInfo.rtcpTc),&inc,&(reconsInfo.rtcpTn));

        // reconsider tp (time for previous RTCP packet)
        t = (reconsInfo.rtcpTc.tv_sec - reconsInfo.rtcpTp.tv_sec) *
            1000000 +
            (reconsInfo.rtcpTc.tv_usec - reconsInfo.rtcpTp.tv_usec);
        t *= getMembersCount();
        t /= reconsInfo.rtcpPMembers;
        inc.tv_usec = t % 1000000;
        inc.tv_sec = t / 1000000;
        timeradd(&(reconsInfo.rtcpTc),&inc,&(reconsInfo.rtcpTp));
    }
    reconsInfo.rtcpPMembers = getMembersCount();
}

bool QueueRTCPManager::onGotSDES(SyncSource& source, RTCPPacket& pkt)
{
    // Take into account that length fields in SDES items are
    // 8-bit long, so no ntoh[s|l] is required
    bool cname_found = false;

    ptrdiff_t pointer = reinterpret_cast<unsigned char*>(&pkt) - rtcpRecvBuffer;
    uint16 i = 0;
    do {
        size_t len = pkt.getLength();
        pointer += sizeof(RTCPFixedHeader);
        SDESChunk* chunk = (SDESChunk*)(rtcpRecvBuffer + pointer);

        bool source_created = false;
        // TODO: avoid searching again the source of the first chunk.
        SyncSourceLink* sourceLink =
            getSourceBySSRC(chunk->getSSRC(),
                    source_created);
        // TODO: check that there are no two chunks with the
        // same SSRC but different CNAME
        SyncSource& src = *( sourceLink->getSource() );

        if ( onGotSDESChunk(source,*chunk,len) )
            cname_found = true;
        pointer +=len;
        if( sourceLink->getHello() )
            onNewSyncSource(src);
        i++;
    } while ( i < pkt.fh.block_count );
    return cname_found;
}

bool QueueRTCPManager::onGotSDESChunk(SyncSource& source, SDESChunk& chunk, size_t len)
{
    bool cname_found = false;
    bool end = false;

    SyncSourceLink* srcLink = getLink(source);
    Participant* part = source.getParticipant();

    size_t pointer = sizeof(chunk.ssrc);

    // process chunk items
    while ( (pointer < len) && !end ) {
        SDESItem* item =
            reinterpret_cast<SDESItem*>(size_t(&(chunk)) + pointer);
        if ( item->type > SDESItemTypeEND && item->type <= SDESItemTypeLast) {
            pointer += sizeof(item->type) + sizeof(item->len) +
                item->len;
            if ( NULL == part && SDESItemTypeCNAME == item->type ) {
                const RTPApplication& app = getApplication();
                std::string cname = std::string(item->data,item->len);
                const Participant* p = app.getParticipant(cname);
                if ( p ) {
                    part = const_cast<Participant*>(p);
                    setParticipant(*(srcLink->getSource()),*part);
                } else {
                    part = new Participant("-");
                    addParticipant(const_cast<RTPApplication&>(getApplication()),*part);
                }
                setParticipant(*(srcLink->getSource()),*part);
            }

            // support for CNAME updates
            if ( part )
                setSDESItem(part,(SDESItemType)item->type, item->data,item->len);

            if ( item->type == SDESItemTypeCNAME) {
                cname_found = true;
                // note that CNAME must be send in
                // every RTCP compound, so we only
                // trust sources that include it.
                setState(*(srcLink->getSource()),
                     SyncSource::stateActive);
            }
        } else if ( item->type == SDESItemTypeEND) {
            end = true;
            pointer++;
            pointer += (pointer & 0x03); // padding
        } else if ( item->type == SDESItemTypePRIV ) {
            ptrdiff_t prevpointer = pointer;
            uint8 plength = *( &(item->len) + 1 );
            pointer += sizeof(item->type) + sizeof(item->len)  + 1;

            if ( part )
                setSDESItem(part,SDESItemTypePRIV,
                        reinterpret_cast<char*>(item + pointer),plength);
            pointer += plength;
            setPRIVPrefix(part,
                      reinterpret_cast<char*>(item + pointer),
                      (item->len - 1 - plength));
            pointer = prevpointer + item->len;
        } else {
            pointer++;
            // TODO: error: SDES unknown
            I( false );
        }
    }
    return cname_found;
}

timeval QueueRTCPManager::computeRTCPInterval()
{
    float bwfract = controlBwFract * getSessionBandwidth();
    uint32 participants = getMembersCount();
    if ( getSendersCount() > 0 &&
         ( getSendersCount() < (getMembersCount() * sendControlBwFract) )) {
        // reserve "sendControlBwFract" fraction of the total
        // RTCP bandwith for senders.
        if (rtcpWeSent) {
            // we take the side of active senders
            bwfract *= sendControlBwFract;
            participants = getSendersCount();
        } else {
            // we take the side of passive receivers
            bwfract *= recvControlBwFract;
            participants = getMembersCount() - getSendersCount();
        }
    }

    microtimeout_t min_interval = rtcpMinInterval;
    // be a bit quicker at first
    if ( rtcpInitial )
        min_interval /= 2;
    // this is the real computation:
    microtimeout_t interval = 0;
    if ( bwfract != 0 ) {
        interval = static_cast<microtimeout_t>
            ((participants * rtcpAvgSize / bwfract) * 1000000);

        if ( interval < rtcpMinInterval )
            interval = rtcpMinInterval;
    } else {
        // 100 seconds instead of infinite
        interval = 100000000;
    }

    interval = static_cast<microtimeout_t>(interval * ( 0.5 +
        (rand() / (RAND_MAX + 1.0))));

    timeval result;
    result.tv_sec = interval / 1000000;
    result.tv_usec = interval % 1000000;
    return result;
}

size_t QueueRTCPManager::dispatchBYE(const std::string& reason)
{
    // for this method, see section 6.3.7 in RFC 3550
    // never send a BYE packet if never sent an RTP or RTCP packet
    // before
    if ( !(getSendPacketCount() || getSendRTCPPacketCount()) )
        return 0;

    if ( getMembersCount() > 50) {
        // Usurp the scheduler role and apply a back-off
        // algorithm to avoid BYE floods.
        gettimeofday(&(reconsInfo.rtcpTc),NULL);
        reconsInfo.rtcpTp = reconsInfo.rtcpTc;
        setMembersCount(1);
        setPrevMembersNum(1);
        rtcpInitial = true;
        rtcpWeSent = false;
        rtcpAvgSize = (uint16)(sizeof(RTCPFixedHeader) + sizeof(uint32) +
            strlen(reason.c_str()) +
            (4 - (strlen(reason.c_str()) & 0x03)));
        gettimeofday(&(reconsInfo.rtcpTc),NULL);
        timeval T = computeRTCPInterval();
        timeradd(&(reconsInfo.rtcpTp),&T,&(reconsInfo.rtcpTn));
        while ( timercmp(&(reconsInfo.rtcpTc),&(reconsInfo.rtcpTn),<) ) {
            getOnlyBye();
            if ( timerReconsideration() )
                break;
            gettimeofday(&(reconsInfo.rtcpTc),NULL);
        }
    }


    unsigned char buffer[500];
    // Build an empty RR as first packet in the compound.
        // TODO: provide more information if available. Not really
    // important, since this is the last packet being sent.
    RTCPPacket* pkt = reinterpret_cast<RTCPPacket*>(buffer);
    pkt->fh.version = CCRTP_VERSION;
    pkt->fh.padding = 0;
    pkt->fh.block_count = 0;
    pkt->fh.type = RTCPPacket::tRR;
    pkt->info.RR.ssrc= getLocalSSRCNetwork();
    uint16 len1 = sizeof(RTCPFixedHeader) + sizeof(uint32); // 1st pkt len.
    pkt->fh.length = htons((len1 >> 2) - 1);
    uint16 len = len1; // whole compound len.
    // build a BYE packet
    uint16 padlen = 0;
    pkt = reinterpret_cast<RTCPPacket*>(buffer + len1);
    pkt->fh.version = CCRTP_VERSION;
    pkt->fh.block_count = 1;
    pkt->fh.type = RTCPPacket::tBYE;
    // add the SSRC identifier
    pkt->info.BYE.ssrc = getLocalSSRCNetwork();
    len += sizeof(RTCPFixedHeader) + sizeof(BYEPacket);
    // add the optional reason
    if ( reason.c_str() != NULL ){
        pkt->info.BYE.length = (uint8)strlen(reason.c_str());
        memcpy(buffer + len,reason.c_str(),pkt->info.BYE.length);
        len += pkt->info.BYE.length;
        padlen = 4 - ((len - len1) & 0x03);
        if ( padlen ) {
            memset(buffer + len,0,padlen);
            len += padlen;
            pkt->info.BYE.length += padlen;
        }
    }
    pkt->fh.length = htons(((len - len1) >> 2) - 1);

    return sendControlToDestinations(buffer,len);
}

void QueueRTCPManager::getOnlyBye()
{
    // This method is kind of simplified recvControl
    timeval wait;
    timersub(&(reconsInfo.rtcpTn),&(reconsInfo.rtcpTc),&wait);
    microtimeout_t timer = wait.tv_usec/1000 + wait.tv_sec * 1000;
    // wait up to reconsInfo.rtcpTn
    if ( !isPendingControl(timer) )
        return;

    size_t len = 0;
    InetHostAddress network_address;
    tpport_t transport_port;
    while ( (len = recvControl(rtcpRecvBuffer,getPathMTU(),
                  network_address,transport_port)) ) {
        // Process a <code>len<code> octets long RTCP compound packet
        // Check validity of the header fields of the compound packet
        if ( !RTCPCompoundHandler::checkCompoundRTCPHeader(len) )
            return;

        // TODO: For now, we do nothing with the padding bit
        // in the header.
        uint32 pointer = 0;
        RTCPPacket* pkt;
        while ( pointer < len) {
            pkt = reinterpret_cast<RTCPPacket*>
                (rtcpRecvBuffer + pointer);

            if (pkt->fh.type == RTCPPacket::tBYE ) {
                bool created;
                SyncSourceLink* srcLink =
                    getSourceBySSRC(pkt->getSSRC(),
                            created);
                if( srcLink->getGoodbye() )
                    onGotGoodbye(*(srcLink->getSource()), "");
                BYESource(pkt->getSSRC());
            }
            pointer += pkt->getLength();
        }
    }
}

size_t QueueRTCPManager::dispatchControlPacket(void)
{
    rtcpInitial = false;
    // Keep in mind: always include a report (in SR or RR) and at
    // least a SDES with the local CNAME. It is mandatory.

    // (A) SR or RR, depending on whether we sent.
    // pkt will point to the packets of the compound

    RTCPPacket* pkt = reinterpret_cast<RTCPPacket*>(rtcpSendBuffer);
    // Fixed header of the first report
    pkt->fh.padding = 0;
    pkt->fh.version = CCRTP_VERSION;
    // length of the RTCP compound packet. It will increase till
    // the end of this routine. Both sender and receiver report
    // carry the general 32-bit long fixed header and a 32-bit
    // long SSRC identifier.
    uint16 len = sizeof(RTCPFixedHeader) + sizeof(uint32);

    // the fields block_count and length will be filled in later
    // now decide whether to send a SR or a SR
    if ( lastSendPacketCount != getSendPacketCount() ) {
        // we have sent rtp packets since last RTCP -> send SR
        lastSendPacketCount = getSendPacketCount();
        pkt->fh.type = RTCPPacket::tSR;
        pkt->info.SR.ssrc = getLocalSSRCNetwork();

        // Fill in sender info block. It would be more
        // accurate if this were done as late as possible.
        timeval now;
        gettimeofday(&now,NULL);
        // NTP MSB and MSB: dependent on current payload type.
        pkt->info.SR.sinfo.NTPMSW = htonl(now.tv_sec + NTP_EPOCH_OFFSET);
        pkt->info.SR.sinfo.NTPLSW = htonl((uint32)(((double)(now.tv_usec)*(uint32)(~0))/1000000.0));
        // RTP timestamp
        int32 tstamp = now.tv_usec - getInitialTime().tv_usec;
        tstamp *= (getCurrentRTPClockRate()/1000);
        tstamp /= 1000;
        tstamp += (now.tv_sec - getInitialTime().tv_sec) *
            getCurrentRTPClockRate();
        tstamp += getInitialTimestamp();
        pkt->info.SR.sinfo.RTPTimestamp = htonl(tstamp);
        // sender's packet and octet count
        pkt->info.SR.sinfo.packetCount = htonl(getSendPacketCount());
        pkt->info.SR.sinfo.octetCount = htonl(getSendOctetCount());
        len += sizeof(SenderInfo);
    } else {
        // RR
        pkt->fh.type = RTCPPacket::tRR;
        pkt->info.RR.ssrc = getLocalSSRCNetwork();
    }

    // (B) put report blocks
    // After adding report blocks, we have to leave room for at
    // least a CNAME SDES item
    uint16 available = (uint16)(getPathMTU()
        - lowerHeadersSize
        - len
        - (sizeof(RTCPFixedHeader) +
           2*sizeof(uint8) +
           getApplication().getSDESItem(SDESItemTypeCNAME).length())
        - 100);

    // if we have to go to a new RR packet
    bool another = false;
    uint16 prevlen = 0;
    RRBlock* reports;
    if ( RTCPPacket::tRR == pkt->fh.type )
        reports = pkt->info.RR.blocks;
    else // ( RTCPPacket::tSR == pkt->fh.type )
        reports = pkt->info.SR.blocks;
    do {
        uint8 blocks = 0;
        pkt->fh.block_count = blocks = packReportBlocks(reports,len,available);
        // the length field specifies 32-bit words
        pkt->fh.length = htons( ((len - prevlen) >> 2) - 1);
        prevlen = len;
        if ( 31 == blocks ) {
            // we would need room for a new RR packet and
            // a CNAME SDES
            if ( len < (available -
                 ( sizeof(RTCPFixedHeader) + sizeof(uint32) +
                   sizeof(RRBlock))) ) {
                another = true;
                // Header for this new packet in the compound
                pkt = reinterpret_cast<RTCPPacket*>
                    (rtcpSendBuffer + len);
                pkt->fh.version = CCRTP_VERSION;
                pkt->fh.padding = 0;
                pkt->fh.type = RTCPPacket::tRR;
                pkt->info.RR.ssrc = getLocalSSRCNetwork();
                // appended a new Header and a report block

                len += sizeof(RTCPFixedHeader)+ sizeof(uint32);
                reports = pkt->info.RR.blocks;
            } else {
                another = false;
            }
        } else {
            another = false;
        }
    } while ( (len < available) && another );

    // (C) SDES (CNAME)
    // each SDES chunk must be 32-bit multiple long
    // fill the padding with 0s
    packSDES(len);

    // TODO: virtual for sending APP RTCP packets?

    // actually send the packet.
    size_t count = sendControlToDestinations(rtcpSendBuffer,len);
    ctrlSendCount++;
    // Everything went right, update the RTCP average size
    updateAvgRTCPSize(len);

    return count;
}

void QueueRTCPManager::packSDES(uint16 &len)
{
    uint16 prevlen = len;
    RTCPPacket* pkt = reinterpret_cast<RTCPPacket*>(rtcpSendBuffer + len);
    // Fill RTCP fixed header. Note fh.length is not set till the
    // end of this routine.
    pkt->fh.version = CCRTP_VERSION;
    pkt->fh.padding = 0;
    pkt->fh.block_count = 1;
    pkt->fh.type = RTCPPacket::tSDES;
    pkt->info.SDES.ssrc = getLocalSSRCNetwork();
    pkt->info.SDES.item.type = SDESItemTypeCNAME;
    // put CNAME
    size_t cnameLen =
        getApplication().getSDESItem(SDESItemTypeCNAME).length();
    const char* cname =
        getApplication().getSDESItem(SDESItemTypeCNAME).c_str();
    pkt->info.SDES.item.len = (uint8)cnameLen;
    len += sizeof(RTCPFixedHeader) + sizeof(pkt->info.SDES.ssrc) +
        sizeof(pkt->info.SDES.item.type) +
        sizeof(pkt->info.SDES.item.len);

    memcpy((rtcpSendBuffer + len),cname,cnameLen);
    len += (uint16)cnameLen;
    // pack items other than CNAME (following priorities
    // stablished inside scheduleSDESItem()).
    SDESItemType nexttype = scheduleSDESItem();
    if ( (nexttype > SDESItemTypeCNAME) &&
         (nexttype <= SDESItemTypeLast ) ) {
        SDESItem *item = reinterpret_cast<SDESItem *>(rtcpSendBuffer + len);
        item->type = nexttype;
        const char *content =
            getApplication().getSDESItem(nexttype).c_str();
        item->len = (uint8)strlen(content);
        len += 2;
        memcpy(reinterpret_cast<char *>(rtcpSendBuffer + len),
              content,item->len);
        len += item->len;
    }

    // pack END item (terminate list of items in this chunk)
    rtcpSendBuffer[len] = SDESItemTypeEND;
    len++;

    uint8 padding = len & 0x03;
    if ( padding ) {
        padding = 4 - padding;
        memset((rtcpSendBuffer + len),SDESItemTypeEND,padding);
        len += padding;
    }
    pkt->fh.length = htons((len - prevlen - 1) >>2);
}

uint8 QueueRTCPManager::packReportBlocks(RRBlock* blocks, uint16 &len, uint16& available)
{
    uint8 j = 0;
    // pack as many report blocks as we can
    SyncSourceLink* i = getFirst();
    for ( ;
          ( ( i != NULL ) &&
        ( len < (available - sizeof(RTCPCompoundHandler::RRBlock)) ) &&
        ( j < 31 ) );
          i = i->getNext() ) {
        SyncSourceLink& srcLink = *i;
        // update stats.
        srcLink.computeStats();
        blocks[j].ssrc = htonl(srcLink.getSource()->getID());
        blocks[j].rinfo.fractionLost = srcLink.getFractionLost();
        blocks[j].rinfo.lostMSB =
            (srcLink.getCumulativePacketLost() & 0xFF0000) >> 16;
        blocks[j].rinfo.lostLSW =
            htons(srcLink.getCumulativePacketLost() & 0xFFFF);
        blocks[j].rinfo.highestSeqNum =
            htonl(srcLink.getExtendedMaxSeqNum());
        blocks[j].rinfo.jitter =
            htonl(static_cast<uint32>(srcLink.getJitter()));
        RTCPCompoundHandler::SenderInfo* si =
            reinterpret_cast<RTCPCompoundHandler::SenderInfo*>(srcLink.getSenderInfo());
        if ( NULL == si ) {
            blocks[j].rinfo.lsr = 0;
            blocks[j].rinfo.dlsr = 0;
        } else {
            blocks[j].rinfo.lsr =
                htonl( ((ntohl(si->NTPMSW) & 0x0FFFF) << 16 )+
                       ((ntohl(si->NTPLSW) & 0xFFFF0000) >> 16)
                       );
            timeval now, diff;
            gettimeofday(&now,NULL);
            timeval last = srcLink.getLastRTCPSRTime();
            timersub(&now,&last,&diff);
            blocks[j].rinfo.dlsr =
                htonl(timevalIntervalTo65536(diff));
        }
        len += sizeof(RTCPCompoundHandler::RRBlock);
        j++;
    }
    return j;
}

void QueueRTCPManager::setSDESItem(Participant* part, SDESItemType type,
const char* const value, size_t len)
{
    char* buf = new char[len + 1];
    memcpy(buf,value,len);
    buf[len] = '\0';
    ParticipantHandler::setSDESItem(part,type,buf);
    delete [] buf;
}

void QueueRTCPManager::setPRIVPrefix(Participant* part, const char* const value, size_t len)
{
    char *buf = new char[len + 1];
    memcpy(buf,value,len);
    buf[len] = '\0';
    ParticipantHandler::setPRIVPrefix(part,buf);
    delete buf;
}

SDESItemType QueueRTCPManager::scheduleSDESItem()
{
    uint8 i = 0;
    // TODO: follow, at least, standard priorities
    SDESItemType type = nextScheduledSDESItem;

    while ( (queueApplication.getSDESItem(type).length() <= 0) &&
        i < (lastSchedulable - firstSchedulable) ) {
        i++;
        type = nextSDESType(type);
    }
    bool empty = true;
    if ( queueApplication.getSDESItem(type).length() > 0 )
        empty = false;
    nextScheduledSDESItem = nextSDESType(type);
    if ( empty )
        return SDESItemTypeEND;
    else
        return type;
}

SDESItemType QueueRTCPManager::nextSDESType(SDESItemType t)
{
    t = static_cast<SDESItemType>( static_cast<int>(t) + 1 );
    if ( t > lastSchedulable )
        t = firstSchedulable;
    return t;
}

size_t QueueRTCPManager::sendControlToDestinations(unsigned char* buffer, size_t len)
{
    size_t count = 0;
    lockDestinationList();
    if ( isSingleDestination() ) {
        count = sendControl(buffer,len);
    } else {
        // when no destination has been added, NULL == dest.
        for (std::list<TransportAddress*>::iterator i =
                 destList.begin(); destList.end() != i; i++) {
            TransportAddress* dest = *i;
            setControlPeer(dest->getNetworkAddress(),
                       dest->getControlTransportPort());
            count += sendControl(buffer,len);
        }
    }
    unlockDestinationList();

    return count;
}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
