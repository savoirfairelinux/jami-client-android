// Copyright (C) 2001,2002 Federico Montesino <p5087@quintero.fie.us.es>
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
 * @file members.cpp
 * @shot MembershipBookkeeping class implementation
 *
 * @todo implement the reallocation mechanism (e.g., when the number
 * of ssrcs per collision list goes up to 2, make the size
 * approx. four times bigger (0.5 ssrcs per list now. when the number
 * of ssrcs per list goes down to 0.5, decrease four times. Do not use
 * 2 or 0.5, but `2 + something' and `0.5 - somehting'). Always
 * jumping between prime numbers -> provide a table from 7 to many.
 **/

#include "private.h"
#include <ccrtp/cqueue.h>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

const uint32 MembershipBookkeeping::SyncSourceLink::SEQNUMMOD = (1<<16);

MembershipBookkeeping::SyncSourceLink::~SyncSourceLink()
{
#ifdef  CCXX_EXCEPTIONS
    try {
#endif
        delete source;
        delete prevConflict;
        delete receiverInfo;
        delete senderInfo;
#ifdef  CCXX_EXCEPTIONS
    } catch (...) { }
#endif
}

void
MembershipBookkeeping::SyncSourceLink::initStats()
{
    lastPacketTime.tv_sec = lastPacketTime.tv_usec = 0;
    lastRTCPPacketTime.tv_sec = lastRTCPPacketTime.tv_usec = 0;
    lastRTCPSRTime.tv_sec = lastRTCPSRTime.tv_usec = 0;

    senderInfo = NULL;
    receiverInfo = NULL;

    obsPacketCount = obsOctetCount = 0;
    maxSeqNum = extendedMaxSeqNum = 0;
    cumulativePacketLost = 0;
    fractionLost = 0;
    jitter = 0;
    initialDataTimestamp = 0;
    initialDataTime.tv_sec = initialDataTime.tv_usec = 0;
    flag = false;

    badSeqNum = SEQNUMMOD + 1;
    probation = 0;
    baseSeqNum = 0;
    expectedPrior = 0;
    receivedPrior = 0;
    seqNumAccum = 0;
}

void
MembershipBookkeeping::SyncSourceLink::computeStats()
{
    // See Appendix A.3

    // compute cumulative packet lost.
    setExtendedMaxSeqNum(getMaxSeqNum() + getSeqNumAccum());
    uint32 expected =
        (getExtendedMaxSeqNum() - getBaseSeqNum() + 1);
    uint32 pc = getObservedPacketCount();
    uint32 lost;
    if ( 0 == pc )
        lost = 0;
    else
        lost = expected - pc;
    setCumulativePacketLost(lost);

    // compute the fraction of packets lost during the last
    // reporting interval.
    uint32 expectedDelta = expected - expectedPrior;
    expectedPrior = expected;
    uint32 receivedDelta = getObservedPacketCount() -
        receivedPrior;
    receivedPrior = getObservedPacketCount();
    uint32 lostDelta = expectedDelta - receivedDelta;
    if ( expectedDelta == 0 || lostDelta <= 0 )
        setFractionLost(0);
    else
        setFractionLost((lostDelta<<8) / expectedDelta );
}

void
MembershipBookkeeping::SyncSourceLink::setPrevConflict(InetAddress& addr,
tpport_t dataPort, tpport_t controlPort)
{
    delete prevConflict;
    prevConflict =
        new ConflictingTransportAddress(addr,dataPort,controlPort);
}

void
MembershipBookkeeping::SyncSourceLink::
recordInsertion(const IncomingRTPPktLink&)
{}

void
MembershipBookkeeping::SyncSourceLink::
setSenderInfo(unsigned char* si)
{
    if ( NULL == senderInfo )
        senderInfo = reinterpret_cast<unsigned char*>
            (new RTCPCompoundHandler::SenderInfo);
    memcpy(senderInfo,si,sizeof(RTCPCompoundHandler::SenderInfo));
}

void
MembershipBookkeeping::SyncSourceLink::
setReceiverInfo(unsigned char* ri)
{
    if ( NULL == receiverInfo )
        receiverInfo = reinterpret_cast<unsigned char*>
            (new RTCPCompoundHandler::ReceiverInfo);
    memcpy(receiverInfo,ri,sizeof(RTCPCompoundHandler::ReceiverInfo));
}

const size_t MembershipBookkeeping::defaultMembersHashSize = 11;
const uint32 MembershipBookkeeping::SEQNUMMOD = (1<<16);

#define HASH(a) ((a + (a >> 8)) % MembershipBookkeeping::sourceBucketsNum)

// Initializes the array (hash table) and the global list of
// SyncSourceLink objects
MembershipBookkeeping::MembershipBookkeeping(uint32 initialSize):
SyncSourceHandler(), ParticipantHandler(), ConflictHandler(), Members(),
sourceBucketsNum(initialSize),
sourceLinks( new SyncSourceLink* [sourceBucketsNum] ), first(NULL), last(NULL)
{
    for ( uint32 i = 0; i < sourceBucketsNum; i++ )
        sourceLinks[i] = NULL;
}

void
MembershipBookkeeping::endMembers()
{
    SyncSourceLink* s;
    while( first ) {
        s = first;
        first = first->next;
#ifdef  CCXX_EXCEPTIONS
        try {
#endif
            delete s;
#ifdef  CCXX_EXCEPTIONS
        } catch (...) {}
#endif
    }
    last = NULL;
#ifdef  CCXX_EXCEPTIONS
    try {
#endif
        delete [] sourceLinks;
#ifdef  CCXX_EXCEPTIONS
    } catch (...) {}
#endif
}

bool
MembershipBookkeeping::isRegistered(uint32 ssrc)
{
    bool result = false;
    SyncSourceLink* sl = sourceLinks[ HASH(ssrc) ];

    while ( sl != NULL ) {
        if ( ssrc == sl->getSource()->getID() ) {
            result = true;
            break;
        } else if ( ssrc < sl->getSource()->getID() ) {
            break;
        } else {
            // keep on searching
            sl = sl->getNextCollis();
        }
    }
    return result;
}

// Gets or creates the source and its link structure.
MembershipBookkeeping::SyncSourceLink*
MembershipBookkeeping::getSourceBySSRC(uint32 ssrc, bool& created)
{
    uint32 hashing = HASH(ssrc);
    SyncSourceLink* result = sourceLinks[hashing];
    SyncSourceLink* prev = NULL;
    created = false;

    if ( NULL == result ) {
        result = sourceLinks[hashing] =
            new SyncSourceLink(this,new SyncSource(ssrc));
        created = true;
    } else {
        while ( NULL != result ) {
            if ( ssrc == result->getSource()->getID() ) {
                // we found it!
                break;
            } else if ( ssrc > result->getSource()->getID() ) {
                // keep on searching
                prev = result;
                result = result->getNextCollis();
            } else {
                // ( ssrc < result->getSource()->getID() )
                // it isn't recorded here -> create it.
                SyncSourceLink* newlink =
                    new SyncSourceLink(this,new SyncSource(ssrc));
                if ( NULL != prev )
                    prev->setNextCollis(newlink);
                else
                    sourceLinks[hashing] = newlink;
                newlink->setNextCollis(result);
                result = newlink;
                created = true;
                break;
            }
        }
        if ( NULL == result ) {
            // insert at the end of the collision list
            result =
                new SyncSourceLink(this,new SyncSource(ssrc));
            created = true;
            prev->setNextCollis(result);
        }
    }
    if ( created ) {
        if ( first )
            last->setNext(result);
        else
            first =  result;
        last = result;
        increaseMembersCount();
    }

    return result;
}

bool
MembershipBookkeeping::BYESource(uint32 ssrc)
{
    bool found = false;
    // If the source identified by ssrc is in the table, mark it
    // as leaving the session. If it was not, do nothing.
    if ( isRegistered(ssrc) ) {
        found = true;
        decreaseMembersCount(); // TODO really decrease right now?
    }
    return found;
}

bool
MembershipBookkeeping::removeSource(uint32 ssrc)
{
    bool found = false;
    SyncSourceLink* old = NULL,
        * s = sourceLinks[ HASH(ssrc) ];
    while ( s != NULL ){
        if ( s->getSource()->getID() == ssrc ) {
            // we found it
            if ( old )
                old->setNextCollis(s->getNextCollis());
            if ( s->getPrev() )
                s->getPrev()->setNext(s->getNext());
            if ( s->getNext() )
                s->getNext()->setPrev(s->getPrev());
            decreaseMembersCount();
            if ( s->getSource()->isSender() )
                decreaseSendersCount();
            delete s;
            found = true;
            break;
        } else if ( s->getSource()->getID() > ssrc ) {
            // it wasn't here
            break;
        } else {
            // keep on searching
            old = s;
            s = s->getNextCollis();
        }
    }
    return found;
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
