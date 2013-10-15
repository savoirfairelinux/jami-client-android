// Copyright (C) 2001,2002,2004 Federico Montesino Pouzols <fedemp@altern.org>
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

#include "private.h"
#include <ccrtp/iqueue.h>

#ifdef CCXX_NAMESPACES
namespace ost {
#endif

const size_t IncomingDataQueueBase::defaultMaxRecvPacketSize = 65534;

ConflictHandler::ConflictingTransportAddress::
ConflictingTransportAddress(InetAddress na,tpport_t dtp, tpport_t ctp):
networkAddress(na), dataTransportPort(dtp),
controlTransportPort(ctp), next(NULL)
{
    gettimeofday(&lastPacketTime,NULL);
}

ConflictHandler::ConflictingTransportAddress*
ConflictHandler::searchDataConflict(InetAddress na, tpport_t dtp)
{
    ConflictingTransportAddress* result = firstConflict;
    while ( result->networkAddress != na ||
        result->dataTransportPort != dtp)
        result = result->next;
    return result;
}

ConflictHandler::ConflictingTransportAddress*
ConflictHandler::searchControlConflict(InetAddress na, tpport_t ctp)
{
    ConflictingTransportAddress* result = firstConflict;
    while ( result &&
        (result->networkAddress != na ||
         result->controlTransportPort != ctp) )
        result = result->next;
    return result;
}

void
ConflictHandler::addConflict(const InetAddress& na, tpport_t dtp, tpport_t ctp)
{
    ConflictingTransportAddress* nc =
        new ConflictingTransportAddress(na,dtp,ctp);

    if ( lastConflict ) {
        lastConflict->setNext(nc);
        lastConflict = nc;
    } else {
        firstConflict = lastConflict = nc;
    }
}

const uint8 IncomingDataQueue::defaultMinValidPacketSequence = 0;
const uint16 IncomingDataQueue::defaultMaxPacketMisorder = 0;
const uint16 IncomingDataQueue::defaultMaxPacketDropout = 3000;
const size_t IncomingDataQueue::defaultMembersSize =
MembershipBookkeeping::defaultMembersHashSize;

IncomingDataQueue::IncomingDataQueue(uint32 size) :
IncomingDataQueueBase(), MembershipBookkeeping(size)
{
    recvFirst = recvLast = NULL;
    sourceExpirationPeriod = 5; // 5 RTCP report intervals
    minValidPacketSequence = getDefaultMinValidPacketSequence();
    maxPacketDropout = getDefaultMaxPacketDropout();
    maxPacketMisorder = getDefaultMaxPacketMisorder();
}

void
IncomingDataQueue::purgeIncomingQueue()
{
    IncomingRTPPktLink* recvnext;
    // flush the reception queue (incoming packets not yet
    // retrieved)
    recvLock.writeLock();
    while( recvFirst )
    {
        recvnext = recvFirst->getNext();

        // nullify source specific packet list
        SyncSourceLink *s = recvFirst->getSourceLink();
        s->setFirst(NULL);
        s->setLast(NULL);

        delete recvFirst->getPacket();
        delete recvFirst;
        recvFirst = recvnext;
    }
    recvLock.unlock();
}

void
IncomingDataQueue::renewLocalSSRC()
{
    const uint32 MAXTRIES = 20;
    uint32 newssrc;
    uint16 tries = 0;
    do {
        newssrc = random32();
        tries++;
    } while ( (tries < MAXTRIES) && isRegistered(newssrc) );

    if ( MAXTRIES == tries  ) {
        // TODO we are in real trouble.
    }
}

bool
IncomingDataQueue::isWaiting(const SyncSource* src) const
{
    bool w;
    recvLock.readLock();
    if ( NULL == src )
        w = ( NULL != recvFirst);
    else
        w = isMine(*src) && ( NULL != getLink(*src)->getFirst() );

    recvLock.unlock();
    return w;
}

uint32
IncomingDataQueue::getFirstTimestamp(const SyncSource* src) const
{
    recvLock.readLock();

    // get the first packet
    IncomingRTPPktLink* packetLink;
    if ( NULL == src )
        packetLink = recvFirst;
    else
        packetLink = isMine(*src) ? getLink(*src)->getFirst() : NULL;

    // get the timestamp of the first packet
    uint32 ts;
    if ( packetLink )
        ts = packetLink->getTimestamp();
    else
        ts = 0l;

    recvLock.unlock();
    return ts;
}

size_t
IncomingDataQueue::takeInDataPacket(void)
{
    InetHostAddress network_address;
    tpport_t transport_port;

    uint32 nextSize = (uint32)getNextDataPacketSize();
    unsigned char* buffer = new unsigned char[nextSize];
    int32 rtn = (int32)recvData(buffer,nextSize,network_address,transport_port);
    if ( (rtn < 0) || ((uint32)rtn > getMaxRecvPacketSize()) ){
        delete buffer;
        return 0;
    }

    // get time of arrival
    struct timeval recvtime;
    gettimeofday(&recvtime,NULL);

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

    CryptoContext* pcc = getInQueueCryptoContext( packet->getSSRC());
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
    if (pcc != NULL) {
        int32 ret = packet->unprotect(pcc);
        if (ret < 0) {
           if (!onSRTPPacketError(*packet, ret)) {
               delete packet;
               return 0;
            }
        }
    }
    if (padSet) {
        packet->reComputePayLength(true);
    }
    // virtual for profile-specific validation and processing.
    if ( !onRTPPacketRecv(*packet) ) {
        delete packet;
        return 0;
    }

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
    } else if ( 0 == s->getDataTransportPort() ) {
        // Test if RTCP packets had been received but this is the
        // first data packet from this source.
        setDataTransportPort(*s,transport_port);
    }

    // Before inserting in the queue,
    // 1) check for collisions and loops. If the packet cannot be
    //    assigned to a source, it will be rejected.
    // 2) check the source is a sufficiently well known source
    // TODO: also check CSRC identifiers.
    if ( checkSSRCInIncomingRTPPkt(*sourceLink,source_created,
                       network_address,transport_port) &&
         recordReception(*sourceLink,*packet,recvtime) ) {
        // now the packet link is linked in the queues
        IncomingRTPPktLink* packetLink =
            new IncomingRTPPktLink(packet,
                           sourceLink,
                           recvtime,
                           packet->getTimestamp() -
                           sourceLink->getInitialDataTimestamp(),
                           NULL,NULL,NULL,NULL);
        insertRecvPacket(packetLink);
    } else {
        // must be discarded due to collision or loop or
        // invalid source
        delete packet;
    }

    // ccRTP keeps packets from the new source, but avoids
    // flip-flopping. This allows losing less packets and for
    // mobile telephony applications or other apps that may change
    // the source transport address during the session.
    return rtn;
}

bool IncomingDataQueue::checkSSRCInIncomingRTPPkt(SyncSourceLink& sourceLink,
bool is_new, InetAddress& network_address, tpport_t transport_port)
{
    bool result = true;

    // Test if the source is new and it is not the local one.
    if ( is_new &&
         sourceLink.getSource()->getID() != getLocalSSRC() )
        return result;

    SyncSource *s = sourceLink.getSource();

    if ( s->getDataTransportPort() != transport_port ||
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
                   sourceLink.getPrevConflict()->dataTransportPort)
                  ) ) {
                // discard packet and do not flip-flop
                result = false;
            } else {
                // Record who has collided so that in
                // the future we can how if the
                // collision repeats.
                sourceLink.setPrevConflict(network_address,
                               transport_port,0);
                // Change sync source transport address
                setDataTransportPort(*s,transport_port);
                setNetworkAddress(*s,network_address);
            }

        } else {
            // Collision or loop of own packets.
            ConflictingTransportAddress* conflicting =
                searchDataConflict(network_address,
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
                dispatchBYE("SSRC collision detected when receiving data packet.");
                renewLocalSSRC();
                setNetworkAddress(*s,network_address);
                setDataTransportPort(*s,transport_port);
                setControlTransportPort(*s,0);
                sourceLink.initStats();
                sourceLink.setProbation(getMinValidPacketSequence());
            }
        }
    }
    return result;
}

bool
IncomingDataQueue::insertRecvPacket(IncomingRTPPktLink* packetLink)
{
    SyncSourceLink *srcLink = packetLink->getSourceLink();
    unsigned short seq = packetLink->getPacket()->getSeqNum();
    recvLock.writeLock();
    IncomingRTPPktLink* plink = srcLink->getLast();
    if ( plink && (seq < plink->getPacket()->getSeqNum()) ) {
        // a disordered packet, so look for its place
        while ( plink && (seq < plink->getPacket()->getSeqNum()) ){
            // the packet is a duplicate
            if ( seq == plink->getPacket()->getSeqNum() ) {
                recvLock.unlock();
                VDL(("Duplicated disordered packet: seqnum %d, SSRC:",
                     seq,srcLink->getSource()->getID()));
                delete packetLink->getPacket();
                delete packetLink;
                return false;
            }
            plink = plink->getSrcPrev();
        }
        if ( !plink ) {
            // we have scanned the whole (and non empty)
            // list, so this must be the older (first)
            // packet from this source.

            // insert into the source specific queue
            IncomingRTPPktLink* srcFirst = srcLink->getFirst();
            srcFirst->setSrcPrev(packetLink);
            packetLink->setSrcNext(srcFirst);
            // insert into the global queue
            IncomingRTPPktLink* prevFirst = srcFirst->getPrev();
            if ( prevFirst ){
                prevFirst->setNext(packetLink);
                packetLink->setPrev(prevFirst);
            }
            srcFirst->setPrev(packetLink);
            packetLink->setNext(srcFirst);
            srcLink->setFirst(packetLink);
        } else {
            // (we are in the middle of the source list)
            // insert into the source specific queue
            plink->getSrcNext()->setSrcPrev(packetLink);
            packetLink->setSrcNext(plink->getSrcNext());
            // -- insert into the global queue, with the
            // minimum priority compared to packets from
            // other sources
            plink->getSrcNext()->getPrev()->setNext(packetLink);
            packetLink->setPrev(plink->getSrcNext()->getPrev());
            plink->getSrcNext()->setPrev(packetLink);
            packetLink->setNext(plink->getSrcNext());
            // ------
            plink->setSrcNext(packetLink);
            packetLink->setSrcPrev(plink);
              // insert into the global queue (giving
              // priority compared to packets from other sources)
              //list->getNext->setPrev(packetLink);
              //packetLink->setNext(list->getNext);
              //list->setNext(packet);
              //packet->setPrev(list);
        }
    } else {
        // An ordered packet
        if ( !plink ) {
            // the only packet in the source specific queue
            srcLink->setLast(packetLink);
            srcLink->setFirst(packetLink);
            // the last packet in the global queue
            if ( recvLast ) {
                recvLast->setNext(packetLink);
                packetLink->setPrev(recvLast);
            }
            recvLast = packetLink;
            if ( !recvFirst )
                recvFirst = packetLink;
        } else {
            // there are already more packets from this source.
            // this ignores duplicate packets
            if ( plink && (seq == plink->getPacket()->getSeqNum()) ) {
                VDL(("Duplicated packet: seqnum %d, SSRC:",
                   seq,srcLink->getSource->getID()));
                recvLock.unlock();
                delete packetLink->getPacket();
                delete packetLink;
                return false;
            }
            // the last packet in the source specific queue
            srcLink->getLast()->setSrcNext(packetLink);
            packetLink->setSrcPrev(srcLink->getLast());
            srcLink->setLast(packetLink);
            // the last packet in the global queue
            recvLast->setNext(packetLink);
            packetLink->setPrev(recvLast);
            recvLast = packetLink;
        }
    }
    // account the insertion of this packet into the queue
    srcLink->recordInsertion(*packetLink);
    recvLock.unlock();
    // packet successfully inserted
    return true;
}

const AppDataUnit*
IncomingDataQueue::getData(uint32 stamp, const SyncSource* src)
{
    IncomingRTPPktLink* pl;
//  unsigned count = 0;
    AppDataUnit* result;

    if ( NULL != (pl = getWaiting(stamp,src)) ) {
        IncomingRTPPkt* packet = pl->getPacket();
//      size_t len = packet->getPayloadSize();

        SyncSource &src = *(pl->getSourceLink()->getSource());
        result = new AppDataUnit(*packet,src);

        // delete the packet link, but not the packet
        delete pl;
//      count += len;
    } else {
        result = NULL;
    }
    return result;
}

// FIX: try to merge and organize
IncomingDataQueue::IncomingRTPPktLink*
IncomingDataQueue::getWaiting(uint32 timestamp, const SyncSource* src)
{
    if ( src && !isMine(*src) )
        return NULL;

    IncomingRTPPktLink *result;
    recvLock.writeLock();
    if ( src != NULL ) {
        // process source specific queries:
        // we will modify the queue of this source
        SyncSourceLink* srcm = getLink(*src);

        // first, delete all older packets. The while loop
        // down here counts how many older packets are there;
        // then the for loop deletes them and advances l till
        // the first non older packet.
        int nold = 0;
        IncomingRTPPktLink* l = srcm->getFirst();
        if ( !l ) {
            result = NULL;
            recvLock.unlock();
            return result;
        }
        while ( l && ((l->getTimestamp() < timestamp) ||
            end2EndDelayed(*l))) {
            nold++;
            l = l->getSrcNext();
        }
        // to know whether the global queue gets empty
        bool nonempty = false;
        for ( int i = 0; i < nold; i++) {
            l = srcm->getFirst();
            srcm->setFirst(srcm->getFirst()->getSrcNext());;
            // unlink from the global queue
            nonempty = false;
            if ( l->getPrev() ){
                nonempty = true;
                l->getPrev()->setNext(l->getNext());
            } if ( l->getNext() ) {
                nonempty = true;
                l->getNext()->setPrev(l->getPrev());
            }
            // now, delete it
            onExpireRecv(*(l->getPacket()));// notify packet discard
            delete l->getPacket();
            delete l;
        }
        // return the packet, if found
        if ( !srcm->getFirst() ) {
            // threre are no more packets from this source
            srcm->setLast(NULL);
            if ( !nonempty )
                recvFirst = recvLast = NULL;
            result = NULL;
        } else if ( srcm->getFirst()->getTimestamp() > timestamp ) {
            // threre are only newer packets from this source
            srcm->getFirst()->setSrcPrev(NULL);
            result = NULL;
        } else {
            // (src->getFirst()->getTimestamp() == stamp) is true
            result = srcm->getFirst();
            // unlink the selected packet from the global queue
            if ( result->getPrev() )
                result->getPrev()->setNext(result->getNext());
            else
                recvFirst = result->getNext();
            if ( result->getNext() )
                result->getNext()->setPrev(result->getPrev());
            else
                recvLast = result->getPrev();
            // unlink the selected packet from the source queue
            srcm->setFirst(result->getSrcNext());
            if ( srcm->getFirst() )
                srcm->getFirst()->setPrev(NULL);
            else
                srcm->setLast(NULL);
        }
    } else {
        // process source unspecific queries
        int nold = 0;
        IncomingRTPPktLink* l = recvFirst;
        while ( l && (l->getTimestamp() < timestamp ||
                  end2EndDelayed(*l) ) ){
            nold++;
            l = l->getNext();
        }
        for (int i = 0; i < nold; i++) {
            IncomingRTPPktLink* l = recvFirst;
            recvFirst = recvFirst->getNext();
            // unlink the packet from the queue of its source
            SyncSourceLink* src = l->getSourceLink();
            src->setFirst(l->getSrcNext());
            if ( l->getSrcNext() )
                l->getSrcNext()->setSrcPrev(NULL);
            else
                src->setLast(NULL);
            // now, delete it
            onExpireRecv(*(l->getPacket()));// notify packet discard
            delete l->getPacket();
            delete l;
        }

        // return the packet, if found
        if ( !recvFirst ) {
            // there are no more packets in the queue
            recvLast = NULL;
            result = NULL;
        } else if ( recvFirst->getTimestamp() > timestamp ) {
            // there are only newer packets in the queue
            l->setPrev(NULL);
            result = NULL;
        } else {
            // (recvFirst->getTimestamp() == stamp) is true
            result = recvFirst;
            // unlink the selected packet from the global queue
            recvFirst = recvFirst->getNext();
            if ( recvFirst )
                recvFirst->setPrev(NULL);
            else
                recvLast = NULL;
            // unlink the selected packet from the queue
            // of its source
            SyncSourceLink* src = result->getSourceLink();
            src->setFirst(result->getSrcNext());
            if ( src->getFirst() )
                src->getFirst()->setSrcPrev(NULL);
            else
                src->setLast(NULL);
        }
    }
    recvLock.unlock();
    return result;
}

bool
IncomingDataQueue::recordReception(SyncSourceLink& srcLink,
const IncomingRTPPkt& pkt, const timeval recvtime)
{
    bool result = true;

    // Source validation.
    SyncSource* src = srcLink.getSource();
    if ( !(srcLink.isValid()) ) {
        // source is not yet valid.
        if ( pkt.getSeqNum() == srcLink.getMaxSeqNum() + 1 ) {
            // packet in sequence.
            srcLink.decProbation();
            if ( srcLink.isValid() ) {
                // source has become valid.
                // TODO: avoid this the first time.
                srcLink.initSequence(pkt.getSeqNum());
            } else {
                result = false;
            }
        } else {
            // packet not in sequence.
            srcLink.probation = getMinValidPacketSequence() - 1;
            result = false;
        }
        srcLink.setMaxSeqNum(pkt.getSeqNum());
    } else {
        // source was already valid.
        uint16 step = pkt.getSeqNum() - srcLink.getMaxSeqNum();
        if ( step < getMaxPacketDropout() ) {
            // Ordered, with not too high step.
            if ( pkt.getSeqNum() < srcLink.getMaxSeqNum() ) {
                // sequene number wrapped.
                srcLink.incSeqNumAccum();
            }
            srcLink.setMaxSeqNum(pkt.getSeqNum());
        } else if ( step <= (SEQNUMMOD - getMaxPacketMisorder()) ) {
            // too high step of the sequence number.
            if ( pkt.getSeqNum() == srcLink.getBadSeqNum() ) {
                srcLink.initSequence(pkt.getSeqNum());
            } else {
                srcLink.setBadSeqNum((pkt.getSeqNum() + 1) &
                              (SEQNUMMOD - 1) );
                //This additional check avoids that
                //the very first packet from a source
                //be discarded.
                if ( 0 < srcLink.getObservedPacketCount() ) {
                    result = false;
                } else {
                                        srcLink.setMaxSeqNum(pkt.getSeqNum());
                                }
            }
        } else {
            // duplicate or reordered packet
        }
    }

    if ( result ) {
        // the packet is considered valid.
        srcLink.incObservedPacketCount();
        srcLink.incObservedOctetCount(pkt.getPayloadSize());
        srcLink.lastPacketTime = recvtime;
        if ( srcLink.getObservedPacketCount() == 1 ) {
            // ooops, it's the first packet from this source
            setSender(*src,true);
            srcLink.setInitialDataTimestamp(pkt.getTimestamp());
        }
        // we record the last time a packet from this source
        // was received, this has statistical interest and is
        // needed to time out old senders that are no sending
        // any longer.

        // compute the interarrival jitter estimation.
        timeval tarrival;
        timeval lastT = srcLink.getLastPacketTime();
        timeval initial = srcLink.getInitialDataTime();
        timersub(&lastT,&initial,&tarrival);
        uint32 arrival = timeval2microtimeout(tarrival)
            * getCurrentRTPClockRate();
        uint32 transitTime = arrival - pkt.getTimestamp();
        int32 delta = transitTime -
            srcLink.getLastPacketTransitTime();
        srcLink.setLastPacketTransitTime(transitTime);
        if ( delta < 0 )
            delta = -delta;
        srcLink.setJitter( srcLink.getJitter() +
                   (1.0f / 16.0f) *
                  (static_cast<float>(delta) -
                   srcLink.getJitter()));
    }
    return result;
}

void
IncomingDataQueue::recordExtraction(const IncomingRTPPkt&)
{}

void
IncomingDataQueue::setInQueueCryptoContext(CryptoContext* cc)
{
    std::list<CryptoContext *>::iterator i;

    MutexLock lock(cryptoMutex);
    // check if a CryptoContext for a SSRC already exists. If yes
    // remove it from list before inserting the new one.
    for( i = cryptoContexts.begin(); i!= cryptoContexts.end(); i++ ) {
        if( (*i)->getSsrc() == cc->getSsrc() ) {
            CryptoContext* tmp = *i;
            cryptoContexts.erase(i);
            delete tmp;
            break;
        }
    }
    cryptoContexts.push_back(cc);
}

void
IncomingDataQueue::removeInQueueCryptoContext(CryptoContext* cc)
{
    std::list<CryptoContext *>::iterator i;

    MutexLock lock(cryptoMutex);
    if (cc == NULL) {     // Remove any incoming crypto contexts
        for (i = cryptoContexts.begin(); i != cryptoContexts.end(); ) {
            CryptoContext* tmp = *i;
            i = cryptoContexts.erase(i);
            delete tmp;
        }
    }
    else {
        for( i = cryptoContexts.begin(); i!= cryptoContexts.end(); i++ ){
            if( (*i)->getSsrc() == cc->getSsrc() ) {
                CryptoContext* tmp = *i;
                cryptoContexts.erase(i);
                delete tmp;
                return;
            }
        }
    }
}

CryptoContext*
IncomingDataQueue::getInQueueCryptoContext(uint32 ssrc)
{
    std::list<CryptoContext *>::iterator i;

    MutexLock lock(cryptoMutex);
    for( i = cryptoContexts.begin(); i!= cryptoContexts.end(); i++ ){
        if( (*i)->getSsrc() == ssrc) {
            return (*i);
        }
    }
    return NULL;
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
