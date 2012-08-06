// Copyright (C) 2001,2002,2004 Federico Montesino Pouzols <fedemp@altern.org>.
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
 * @file iqueue.h
 *
 * @short Generic RTP input queues.
 **/

#ifndef	CCXX_RTP_IQUEUE_H_
#define CCXX_RTP_IQUEUE_H_

#include <ccrtp/queuebase.h>
#include <ccrtp/CryptoContext.h>

#include <list>

#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

/**
 * @defgroup iqueue Generic RTP input queues.
 * @{
 **/

/**
 * @class Members rtp.h
 * @short members and senders accounting
 *
 * Records the number of members as well as active senders. For now,
 * it is too simple.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT Members
{
public:
	inline void
	setMembersCount(uint32 n)
	{ members = n; }

	inline void
	increaseMembersCount()
	{ members++; }

	inline void
	decreaseMembersCount()
	{ members--; }

	inline uint32
	getMembersCount() const
	{ return members; }

	inline void
	setSendersCount(uint32 n)
	{ activeSenders = n; }

	inline void
	increaseSendersCount()
	{ activeSenders++; }

	inline void
	decreaseSendersCount()
	{ activeSenders--; }

	inline uint32
	getSendersCount() const
	{ return activeSenders; }

protected:
	Members() :
		members(0),
		activeSenders(0)
	{ }

	inline virtual ~Members()
	{ }

private:
	/// number of identified members
	uint32 members;
	/// number of identified members that currently are active senders
	uint32 activeSenders;
};

/**
 * @class SyncSourceHandler
 * @short SyncSource objects modification methods.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT SyncSourceHandler
{
public:
	/**
	 * This requires SyncSource - SyncSourceHandler friendship.
	 *
	 * Get the SyncSourceLink corresponding to a SyncSource
	 * object.
	 **/
	inline void*
	getLink(const SyncSource& source) const
	{ return source.getLink(); }

	inline void
	setLink(SyncSource& source, void* link)
	{ source.setLink(link); }

	inline void
	setParticipant(SyncSource& source, Participant& p)
	{ source.setParticipant(p); }

	inline void
	setState(SyncSource& source, SyncSource::State ns)
	{ source.setState(ns); }

	inline void
	setSender(SyncSource& source, bool active)
	{ source.setSender(active); }

	inline void
	setDataTransportPort(SyncSource& source, tpport_t p)
	{ source.setDataTransportPort(p); }

	inline void
	setControlTransportPort(SyncSource& source, tpport_t p)
	{ source.setControlTransportPort(p); }

	inline void
	setNetworkAddress(SyncSource& source, InetAddress addr)
	{ source.setNetworkAddress(addr); }

protected:
	SyncSourceHandler()
	{ }

	inline virtual ~SyncSourceHandler()
	{ }
};

/**
 * @class ParticipantHandler
 * @short Participant objects modification methods.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT ParticipantHandler
{
public:
	inline void
	setSDESItem(Participant* part, SDESItemType item,
		    const std::string& val)
	{ part->setSDESItem(item,val); }

	inline void
	setPRIVPrefix(Participant* part, const std::string val)
	{ part->setPRIVPrefix(val); }

protected:
	ParticipantHandler()
	{ }

	inline virtual ~ParticipantHandler()
	{ }
};

/**
 * @class ApplicationHandler
 * @short Application objects modification methods.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT ApplicationHandler
{
public:
	inline void
	addParticipant(RTPApplication& app, Participant& part)
	{ app.addParticipant(part); }

	inline void
	removeParticipant(RTPApplication& app,
			  RTPApplication::ParticipantLink* pl)
	{ app.removeParticipant(pl); }

protected:
	ApplicationHandler()
	{ }

	inline virtual ~ApplicationHandler()
	{ }
};

/**
 * @class ConflictHandler
 * @short To track addresses of sources conflicting with the local
 * one.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT ConflictHandler
{
public:
	struct ConflictingTransportAddress
	{
		ConflictingTransportAddress(InetAddress na,
					    tpport_t dtp, tpport_t ctp);

		void setNext(ConflictingTransportAddress* nc)
		{ next = nc; }

		inline const InetAddress& getNetworkAddress( ) const
		{ return networkAddress; }

		inline tpport_t getDataTransportPort() const
		{ return dataTransportPort; }

		inline tpport_t getControlTransportPort() const
		{ return controlTransportPort; }

		InetAddress networkAddress;
		tpport_t dataTransportPort;
		tpport_t controlTransportPort;
		ConflictingTransportAddress* next;
		// arrival time of last data or control packet.
		timeval lastPacketTime;
	};

	/**
	 * @param na Inet network address.
	 * @param dtp Data transport port.
	 **/
	ConflictingTransportAddress* searchDataConflict(InetAddress na,
							tpport_t dtp);
	/**
	 * @param na Inet network address.
	 * @param ctp Data transport port.
	 **/
	ConflictingTransportAddress* searchControlConflict(InetAddress na,
							   tpport_t ctp);

	void updateConflict(ConflictingTransportAddress& ca)
	{ gettimeofday(&(ca.lastPacketTime),NULL); }

	void addConflict(const InetAddress& na, tpport_t dtp, tpport_t ctp);

protected:
	ConflictHandler()
	{ firstConflict = lastConflict = NULL; }

	inline virtual ~ConflictHandler()
	{ }

	ConflictingTransportAddress* firstConflict, * lastConflict;
};

/**
 * @class MembershipBookkeeping
 * @short Controls the group membership in the current session.
 *
 * For now, this class implements only a hash table of members, but
 * its design and relation with other classes is intented to support
 * group membership sampling in case scalability problems arise.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 */
class __EXPORT MembershipBookkeeping :
	public SyncSourceHandler,
	public ParticipantHandler,
	public ApplicationHandler,
	public ConflictHandler,
	private Members
{
public:
	inline size_t getDefaultMembersHashSize()
	{ return defaultMembersHashSize; }

protected:

	/**
	 * @short The initial size is a hint to allocate the resources
	 * needed in order to keep the members' identifiers and
	 * associated information.
	 *
	 * Although ccRTP will reallocate resources when it becomes
	 * necessary, a good hint may save a lot of unpredictable time
	 * penalties.
	 *
	 * @param initialSize an estimation of how many participants
	 * the session will consist of.
	 *
	 */
	MembershipBookkeeping(uint32 initialSize = defaultMembersHashSize);

	/**
	 * Purges all RTPSource structures created during the session,
	 * as well as the hash table and the list of sources.
	 **/
	inline virtual
	~MembershipBookkeeping()
	{ endMembers(); }

	struct SyncSourceLink;

	inline SyncSourceLink* getLink(const SyncSource& source) const
	{ return static_cast<SyncSourceLink*>(SyncSourceHandler::getLink(source)); }
	/**
	 * Get whether a synchronization source is recorded in this
	 * membership controller.
	 **/
	inline bool isMine(const SyncSource& source) const
	{ return getLink(source)->getMembership() == this; }

	/**
	 * @struct IncomingRTPPktLink
	 *
	 * @short Incoming RTP data packets control structure within
	 * the incoming packet queue class.
	 **/
	struct IncomingRTPPktLink
	{
		IncomingRTPPktLink(IncomingRTPPkt* pkt, SyncSourceLink* sLink,
				   struct timeval& recv_ts,
				   uint32 shifted_ts,
				   IncomingRTPPktLink* sp,
				   IncomingRTPPktLink* sn,
				   IncomingRTPPktLink* p,
				   IncomingRTPPktLink* n) :
			packet(pkt),
			sourceLink(sLink),
			prev(p), next(n),
			srcPrev(sp), srcNext(sn),
			receptionTime(recv_ts),
			shiftedTimestamp(shifted_ts)
		{ }

		~IncomingRTPPktLink()
		{ }

		inline SyncSourceLink* getSourceLink() const
		{ return sourceLink; }

		inline void setSourceLink(SyncSourceLink* src)
		{ sourceLink = src; }

		inline IncomingRTPPktLink* getNext() const
		{ return next; }

		inline void setNext(IncomingRTPPktLink* nl)
		{ next = nl; }

		inline IncomingRTPPktLink* getPrev() const
		{ return prev; }

		inline void setPrev(IncomingRTPPktLink* pl)
		{ prev = pl; }

		inline IncomingRTPPktLink* getSrcNext() const
		{ return srcNext; }

		inline void setSrcNext(IncomingRTPPktLink* sn)
		{ srcNext = sn; }

		inline IncomingRTPPktLink* getSrcPrev() const
		{ return srcPrev; }

		inline void setSrcPrev(IncomingRTPPktLink* sp)
		{ srcPrev = sp; }

		inline IncomingRTPPkt* getPacket() const
		{ return packet; }

		inline void setPacket(IncomingRTPPkt* pkt)
		{ packet = pkt; }

		/**
		 * Set the time this packet was received at.
		 *
		 * @param t time of reception.
		 * @note this has almost nothing to do with the 32-bit
		 * timestamp contained in the packet header.
		 **/
		inline void setRecvTime(const timeval &t)
		{ receptionTime = t; }

		/**
		 * Get the time this packet was received at.
		 **/
		inline timeval getRecvTime() const
		{ return receptionTime; }

		/**
		 * Get timestamp of this packet. The timestamp of
		 * incoming packets is filtered so that the timestamp
		 * this method provides for the first packet received
		 * from every source starts from 0.
		 *
		 * @return 32 bit timestamp starting from 0 for each source.
		 */
		inline uint32 getTimestamp() const
		{ return shiftedTimestamp; }

		inline void setTimestamp(uint32 ts)
		{ shiftedTimestamp = ts;}

		// the packet this link refers to.
		IncomingRTPPkt* packet;
		// the synchronization source this packet comes from.
		SyncSourceLink* sourceLink;
		// global incoming packet queue links.
		IncomingRTPPktLink* prev, * next;
		// source specific incoming packet queue links.
		IncomingRTPPktLink* srcPrev, * srcNext;
		// time this packet was received at
		struct timeval receptionTime;
		// timestamp of the packet in host order and after
		// substracting the initial timestamp for its source
		// (it is an increment from the initial timestamp).
		uint32 shiftedTimestamp;
	};

	/**
	 * @struct SyncSourceLink
	 *
	 * @short Synchronization Source internal handler within the
	 * incoming packets queue.
	 *
	 * Incoming packets queue objects hold a hash table and a
	 * linked list of synchronization sources. For each of these
	 * sources, there is also a linked list of incoming rtp
	 * packets, which are linked in an "all incoming packets" list
	 * as well. SyncSourceLink objects hold the necessary data to
	 * maintain these data estructures, as well as source specific
	 * information and statistics for RTCP,
	 *
	 * @author Federico Montesino Pouzols <fedemp@altern.org>
	 **/
	struct SyncSourceLink
	{
		// 2^16
		static const uint32 SEQNUMMOD;

		SyncSourceLink(MembershipBookkeeping* m,
			       SyncSource* s,
			       IncomingRTPPktLink* fp = NULL,
			       IncomingRTPPktLink* lp = NULL,
			       SyncSourceLink* ps = NULL,
			       SyncSourceLink* ns = NULL,
			       SyncSourceLink* ncollis = NULL) :
			membership(m), source(s), first(fp), last(lp),
			prev(ps), next(ns), nextCollis(ncollis),
			prevConflict(NULL)
		{ m->setLink(*s,this); // record that the source is associated
		  initStats();         // to this link.
		}

		/**
		 * Note it deletes the source.
		 **/
		~SyncSourceLink();

		inline MembershipBookkeeping* getMembership()
		{ return membership; }

		/**
		 * Get the synchronization source object this link
		 * objet holds information for.
		 **/
		inline SyncSource* getSource() { return source; }

		/**
		 * Get first RTP (data) packet in the queue of packets
		 * received from this socket.
		 **/
		inline IncomingRTPPktLink* getFirst()
		{ return first; }

		inline void setFirst(IncomingRTPPktLink* fp)
		{ first = fp; }

		/**
		 * Get last RTP (data) packet in the queue of packets
		 * received from this socket.
		 **/
		inline IncomingRTPPktLink* getLast()
		{ return last; }

		inline void setLast(IncomingRTPPktLink* lp)
		{ last = lp; }

		/**
		 * Get the link object for the previous RTP source.
		 **/
		inline SyncSourceLink* getPrev()
		{ return prev; }

		inline void setPrev(SyncSourceLink* ps)
		{ prev = ps; }

		/**
		 * Get the link object for the next RTP source.
		 **/
		inline SyncSourceLink* getNext()
		{ return next; }

		inline void setNext(SyncSourceLink *ns)
		{ next = ns; }

		/**
		 * Get the link object for the next RTP source in the
		 * hash table entry collision list.  Note that
		 * collision does not refer to SSRC collision, but
		 * hash table collision.
		 **/
		inline SyncSourceLink* getNextCollis()
		{ return nextCollis; }

		inline void setNextCollis(SyncSourceLink* ns)
		{ nextCollis = ns; }

		inline ConflictingTransportAddress* getPrevConflict() const
		{ return prevConflict; }

		/**
		 * Get conflicting address.
		 **/
		void setPrevConflict(InetAddress& addr, tpport_t dataPort,
				     tpport_t controlPort);

		unsigned char* getSenderInfo()
		{ return senderInfo; }

		void setSenderInfo(unsigned char* si);

		unsigned char* getReceiverInfo()
		{ return receiverInfo; }

		void setReceiverInfo(unsigned char* ri);

		inline timeval getLastPacketTime() const
		{ return lastPacketTime; }

		inline timeval getLastRTCPPacketTime() const
		{ return lastRTCPPacketTime; }

		inline timeval getLastRTCPSRTime() const
		{ return lastRTCPSRTime; }

		/**
		 * Get the total number of RTP packets received from this
		 * source.
		 */
		inline uint32 getObservedPacketCount() const
		{ return obsPacketCount; }

		inline void incObservedPacketCount()
		{ obsPacketCount++; }

		/**
		 * Get the total number of payload octets received from this
		 * source.
		 **/
		inline uint32 getObservedOctetCount() const
		{ return obsOctetCount; }

		inline void incObservedOctetCount(uint32 n)
		{ obsOctetCount += n; }

		/**
		 * Get the highest valid sequence number received.
		 **/
		uint16
		getMaxSeqNum() const
		{ return maxSeqNum; }

		/**
		 * Set the highest valid sequence number recived.
		 * @param max Sequence number.
		 **/
		void
		setMaxSeqNum(uint16 max)
		{ maxSeqNum = max; }

		inline uint32
		getExtendedMaxSeqNum() const
		{ return extendedMaxSeqNum; }

		inline void
		setExtendedMaxSeqNum(uint32 seq)
		{ extendedMaxSeqNum = seq; }

		inline uint32 getCumulativePacketLost() const
		{ return cumulativePacketLost; }

		inline void setCumulativePacketLost(uint32 pl)
		{ cumulativePacketLost = pl; }

		inline uint8 getFractionLost() const
		{ return fractionLost; }

		inline void setFractionLost(uint8 fl)
		{ fractionLost = fl; }

		inline uint32 getLastPacketTransitTime()
		{ return lastPacketTransitTime; }

		inline void setLastPacketTransitTime(uint32 time)
		{ lastPacketTransitTime = time; }

		inline float getJitter() const
		{ return jitter; }

		inline void setJitter(float j)
		{ jitter = j; }

		inline uint32 getInitialDataTimestamp() const
		{ return initialDataTimestamp; }

		inline void setInitialDataTimestamp(uint32 ts)
		{ initialDataTimestamp = ts; }

		inline timeval getInitialDataTime() const
		{ return initialDataTime; }

		inline void setInitialDataTime(timeval it)
		{ initialDataTime = it; }

		/**
		 * Mark this source as having sent a BYE control packet.
		 *
		 * @return whether some packet from this source had
		 * been received before (getHello() has been called at
		 * least once)
		 **/
		bool getGoodbye()
		{
			if(!flag)
				return false;
			flag = false;
			return true;
		}

		/**
		 * Mark this source as having sent some packet.
		 *
		 * @return whether no packet from this source had been
		 * received before
		 **/
		bool getHello() {
			if(flag)
				return false;
			flag = true;
			return true;
		}

		inline uint32 getBadSeqNum() const
		{ return badSeqNum; }

		inline void setBadSeqNum(uint32 seq)
		{ badSeqNum = seq; }

		uint8 getProbation() const
		{ return probation; }

		inline void setProbation(uint8 p)
		{ probation = p; }

		inline void decProbation()
		{ --probation; }

		bool isValid() const
		{ return 0 == probation; }

		inline uint16 getBaseSeqNum() const
		{ return baseSeqNum; }

		inline uint32 getSeqNumAccum() const
		{ return seqNumAccum; }

		inline void incSeqNumAccum()
		{ seqNumAccum += SEQNUMMOD; }

		/**
		 * Start a new sequence of received packets.
		 **/
		inline void initSequence(uint16 seqnum)
		{ maxSeqNum = seqNumAccum = seqnum; }

		/**
		 * Record the insertion of an RTP packet from this
		 * source into the scheduled reception queue. All
		 * received packets should be registered with
		 * recordReception(), but only those actually inserted
		 * into the queue should be registered via this
		 * method.
		 *
		 * @param pl Link structure for packet inserted into the queue.
		 **/
		void recordInsertion(const IncomingRTPPktLink& pl);

		void initStats();

		/**
		 * Compute cumulative packet lost and fraction of
		 * packets lost during the last reporting interval.
		 **/
		void computeStats();

		MembershipBookkeeping* membership;
		// The source this link object refers to.
		SyncSource* source;
		// first/last packets from this source in the queue.
		IncomingRTPPktLink* first, * last;
		// Links for synchronization sources located before
		// and after this one in the list of sources.
		SyncSourceLink* prev, * next;
		// Prev and next inside the hash table collision list.
		SyncSourceLink* nextCollis;
		ConflictingTransportAddress* prevConflict;
		unsigned char* senderInfo;
		unsigned char* receiverInfo;
		// time the last RTP packet from this source was
		// received at.
		timeval lastPacketTime;
		// time the last RTCP packet was received.
		timeval lastRTCPPacketTime;
		// time the lasrt RTCP SR was received. Required for
		// DLSR computation.
		timeval lastRTCPSRTime;

		// for outgoing RR reports.
		// number of packets received from this source.
		uint32 obsPacketCount;
		// number of octets received from this source.
		uint32 obsOctetCount;
		// the higher sequence number seen from this source
		uint16 maxSeqNum;
		uint32 extendedMaxSeqNum;
		uint32 cumulativePacketLost;
		uint8 fractionLost;
		// for interarrivel jitter computation
		uint32 lastPacketTransitTime;
		// interarrival jitter of packets from this source.
		float jitter;
		uint32 initialDataTimestamp;
		timeval initialDataTime;

		// this flag assures we only call one gotHello and one
		// gotGoodbye for this src.
		bool flag;

		// for source validation:
		uint32 badSeqNum;
		uint8 probation;  // packets in sequence before valid.
		uint16 baseSeqNum;
		uint32 expectedPrior;
		uint32 receivedPrior;
		uint32 seqNumAccum;
	};

	/**
	 * Returns whether there is already a synchronizacion source
	 * with "ssrc" SSRC identifier.
	 **/
	bool
	isRegistered(uint32 ssrc);

	/**
	 * Get the description of a source by its <code>ssrc</code> identifier.
	 *
	 * @param ssrc SSRC identifier, in host order.
	 * @param created whether a new source has been created.
	 * @return Pointer to the SyncSource object identified by
	 * <code>ssrc</code>.
	 */
	SyncSourceLink*
	getSourceBySSRC(uint32 ssrc, bool& created);

	/**
	 * Mark the source identified by <code>ssrc</code> as having
	 * sent a BYE packet. It is not deleted until a timeout
	 * expires, so that in case some packets from this source
	 * arrive a bit later the source is not inserted again in the
	 * table of known sources.
	 *
	 * @return true if the source had been previously identified.
	 * false if it was not in the table of known sources.
	 **/
	bool
	BYESource(uint32 ssrc);

	/**
	 * Remove the description of the source identified by
	 * <code>ssrc</code>
	 *
	 * @return whether the source has been actually removed or it
	 * did not exist.
	 */
	bool
	removeSource(uint32 ssrc);

	inline SyncSourceLink* getFirst()
	{ return first; }

	inline SyncSourceLink* getLast()
	{ return last; }

	inline uint32
	getMembersCount()
	{ return Members::getMembersCount(); }

	inline void
	setMembersCount(uint32 n)
	{ Members::setMembersCount(n); }

	inline uint32
	getSendersCount()
	{ return Members::getSendersCount(); }

	static const size_t defaultMembersHashSize;
	static const uint32 SEQNUMMOD;

private:
	MembershipBookkeeping(const MembershipBookkeeping &o);

	MembershipBookkeeping&
	operator=(const MembershipBookkeeping &o);

	/**
	 * Purge all RTPSource structures, the hash table and the list
	 * of sources.
	 **/
	void
	endMembers();

	// Hash table with sources of RTP and RTCP packets
	uint32 sourceBucketsNum;
	SyncSourceLink** sourceLinks;
	// List of sources, ordered from older to newer
	SyncSourceLink* first, * last;
};

/**
 * @class IncomingDataQueue
 * @short Queue for incoming RTP data packets in an RTP session.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT IncomingDataQueue: public IncomingDataQueueBase,
	protected MembershipBookkeeping
{
public:
	/**
	 * @class SyncSourcesIterator
	 * @short iterator through the list of synchronizations
	 * sources in this session
	 **/
	class SyncSourcesIterator
	{
	public:
		typedef std::forward_iterator_tag iterator_category;
		typedef SyncSource value_type;
		typedef ptrdiff_t difference_type;
		typedef const SyncSource* pointer;
		typedef const SyncSource& reference;

		SyncSourcesIterator(SyncSourceLink* l = NULL) :
			link(l)
		{ }

		SyncSourcesIterator(const SyncSourcesIterator& si) :
			link(si.link)
		{ }

		reference operator*() const
		{ return *(link->getSource()); }

		pointer operator->() const
		{ return link->getSource(); }

		SyncSourcesIterator& operator++() {
			link = link->getNext();
			return *this;
		}

		SyncSourcesIterator operator++(int) {
			SyncSourcesIterator result(*this);
			++(*this);
			return result;
		}

		friend bool operator==(const SyncSourcesIterator& l,
				       const SyncSourcesIterator& r)
		{ return l.link == r.link; }

		friend bool operator!=(const SyncSourcesIterator& l,
				       const SyncSourcesIterator& r)
		{ return l.link != r.link; }

	private:
		SyncSourceLink *link;
	};

	SyncSourcesIterator begin()
	{ return SyncSourcesIterator(MembershipBookkeeping::getFirst()); }

	SyncSourcesIterator end()
	{ return SyncSourcesIterator(NULL); }

 	/**
	 * Retreive data from a specific timestamped packet if such a
	 * packet is currently available in the receive buffer. 
	 *
	 * @param stamp Data unit timestamp.
	 * @param src Optional synchronization source selector.
	 * @return data retrieved from the reception buffer.
	 * @retval null pointer if no packet with such timestamp is available.
	 **/
	const AppDataUnit*
	getData(uint32 stamp, const SyncSource* src = NULL);


 	/**
 	 * Determine if packets are waiting in the reception queue.
 	 *
	 * @param src Optional synchronization source selector.
 	 * @return True if packets are waiting.
 	 */
 	bool
	isWaiting(const SyncSource* src = NULL) const;

 	/**
 	 * Get timestamp of first packet waiting in the queue.
 	 *
	 * @param src optional source selector.
 	 * @return timestamp of first arrival packet.
 	 **/
 	uint32
	getFirstTimestamp(const SyncSource* src = NULL) const;

	/**
	 * When receiving packets from a new source, it may be
	 * convenient to reject a first few packets before we are
	 * really sure the source is valid. This method sets how many
	 * data packets must be received in sequence before the source
	 * is considered valid and the stack starts to accept its
	 * packets.
	 *
	 * @note the default (see defaultMinValidPacketSequence())
	 * value for this parameter is 0, so that no packets are
	 * rejected (data packets are accepted from the first one).
	 *
	 * @note this validation is performed after the generic header
	 * validation and the additional validation done in
	 * onRTPPacketRecv().
	 *
	 * @note if any valid RTCP packet is received from this
	 * source, it will be immediatly considered valid regardless
	 * of the number of sequential data packets received.
	 *
	 * @param packets number of sequential packet required
	 **/
	void
	setMinValidPacketSequence(uint8 packets)
	{ minValidPacketSequence = packets; }

	uint8
	getDefaultMinValidPacketSequence() const
	{ return defaultMinValidPacketSequence; }

	/**
	 * Get the minimun number of consecutive packets that must be
	 * received from a source before accepting its data packets.
	 **/
	uint8
	getMinValidPacketSequence() const
	{ return minValidPacketSequence; }

	void
	setMaxPacketMisorder(uint16 packets)
	{ maxPacketMisorder = packets; }

	uint16
	getDefaultMaxPacketMisorder() const
	{ return defaultMaxPacketMisorder; }

	uint16
	getMaxPacketMisorder() const
	{ return maxPacketMisorder; }

	/**
	 *
	 * It also prevents packets sent after a restart of the source
	 * being immediately accepted.
	 **/
	void
	setMaxPacketDropout(uint16 packets) // default: 3000.
	{ maxPacketDropout = packets; }

	uint16
	getDefaultMaxPacketDropout() const
	{ return defaultMaxPacketDropout; }

	uint16
	getMaxPacketDropout() const
	{ return maxPacketDropout; }

	// default value for constructors that allow to specify
	// members table s\ize
        inline static size_t
        getDefaultMembersSize()
        { return defaultMembersSize; }

        /**
         * Set input queue CryptoContext.
         *
         * The endQueue method (provided by RTPQueue) deletes all
         * registered CryptoContexts.
         *
         * @param cc Pointer to initialized CryptoContext.
         */
        void
        setInQueueCryptoContext(CryptoContext* cc);

        /**
         * Remove input queue CryptoContext.
         *
         * The endQueue method (provided by RTPQueue) also deletes all
         * registered CryptoContexts.
         *
         * @param cc
         *     Pointer to initialized CryptoContext to remove. If pointer
         *     if <code>NULL</code> then delete the whole queue
         */
        void
        removeInQueueCryptoContext(CryptoContext* cc);

        /**
         * Get an input queue CryptoContext identified by SSRC
         *
         * @param ssrc Request CryptoContext for this incoming SSRC
         * @return Pointer to CryptoContext of the SSRC of NULL if no context
         * available for this SSRC.
         */
        CryptoContext*
        getInQueueCryptoContext(uint32 ssrc);

protected:
	/**
	 * @param size initial size of the membership table.
	 **/
	IncomingDataQueue(uint32 size);

	virtual ~IncomingDataQueue()
	{ }

	/**
	 * Apply collision and loop detection and correction algorithm
	 * when receiving RTP data packets. Follows section 8.2 in
	 * draft-ietf-avt-rtp-new.
	 *
	 * @param sourceLink link to the source object.
	 * @param is_new whether the source has been just recorded.
	 * @param na data packet network address.
	 * @param tp data packet source transport port.
	 *
	 * @return whether the packet must not be discarded.
	 **/
	bool checkSSRCInIncomingRTPPkt(SyncSourceLink& sourceLink,
				       bool is_new, InetAddress& na,
				       tpport_t tp);

	/**
	 * Set the number of RTCP intervals that the stack will wait
	 * to change the state of a source from stateActive to
	 * stateInactive, or to delete the source after being in
	 * stateInactive.
	 *
	 * Note that this value should be uniform accross all
	 * participants and SHOULD be fixed for a particular profile.
	 *
	 * @param intervals number of RTCP report intervals
	 *
	 * @note If RTCP is not being used, the RTCP interval is
	 * assumed to be the default: 5 seconds.
	 * @note The default for this value is, as RECOMMENDED, 5.
	 **/
	void setSourceExpirationPeriod(uint8 intervals)
	{ sourceExpirationPeriod = intervals; }

	/**
	 * This function is used by the service thread to process
	 * the next incoming packet and place it in the receive list.
	 *
	 * @return number of payload bytes received.  <0 if error.
	 */
	virtual size_t
	takeInDataPacket();

	void renewLocalSSRC();

	/**
	 * This is used to fetch a packet in the receive queue and to
	 * expire packets older than the current timestamp.
	 *
	 * @return packet buffer object for current timestamp if found.
	 * @param timestamp timestamp requested.
	 * @param src optional source selector
	 * @note if found, the packet is removed from the reception queue
	 **/
	IncomingDataQueue::IncomingRTPPktLink*
	getWaiting(uint32 timestamp, const SyncSource *src = NULL);

	/**
	 * Log reception of a new RTP packet from this source. Usually
	 * updates data such as the packet counter, the expected
	 * sequence number for the next packet and the time the last
	 * packet was received at.
	 *
	 * @param srcLink Link structure for the synchronization
	 * source of this packet.
	 * @param pkt Packet just created and to be logged.
	 * @param recvtime Reception time.
	 *
	 * @return whether, according to the source state and
	 * statistics, the packet is considered valid and must be
	 * inserted in the incoming packets queue.
	 **/
	bool
	recordReception(SyncSourceLink& srcLink, const IncomingRTPPkt& pkt,
			const timeval recvtime);

	/**
	 * Log extraction of a packet from this source from the
	 * scheduled reception queue.
	 *
	 * @param pkt Packet extracted from the queue.
	 **/
	void
	recordExtraction(const IncomingRTPPkt& pkt);

	void purgeIncomingQueue();

	/**
	 * Virtual called when a new synchronization source has joined
	 * the session.
	 *
	 * @param - new synchronization source
	 **/
	inline virtual void
	onNewSyncSource(const SyncSource&)
	{ }

protected:
	/**
	 * A virtual function to support parsing of arriving packets
	 * to determine if they should be kept in the queue and to
	 * dispatch events.
	 *
	 * A generic header validity check (as specified in RFC 1889)
	 * is performed on every incoming packet. If the generic check
	 * completes succesfully, this method is called before the
	 * packet is actually inserted into the reception queue.
	 *
	 * May be used to perform additional validity checks or to do
	 * some application specific processing.
	 *
	 * @param - packet just received.
	 * @return true if packet is kept in the incoming packets queue.
	 **/
	inline virtual bool
	onRTPPacketRecv(IncomingRTPPkt&)
	{ return true; }

	/**
	 * A hook to filter packets in the receive queue that are being
	 * expired. This hook may be used to do some application
	 * specific processing on expired packets before they are
	 * deleted.
	 *
	 * @param - packet expired from the recv queue.
	 **/
	inline virtual void onExpireRecv(IncomingRTPPkt&)
	{ return; }

        /**
         * A hook that gets called if the decoding of an incoming SRTP was erroneous
         *
         * @param pkt
         *     The SRTP packet with error.
         * @param errorCode
         *     The error code: -1 - SRTP authentication failure, -2 - replay
         *     check failed
         * @return
         *     True: put the packet in incoming queue for further processing
         *     by the applications; false: dismiss packet. The default
         *     implementation returns false.
         **/
        inline virtual bool
        onSRTPPacketError(IncomingRTPPkt& pkt, int32 errorCode)
        { return false; }

        inline virtual bool
	end2EndDelayed(IncomingRTPPktLink&)
	{ return false; }

       	/**
	 * Insert a just received packet in the queue (both general
	 * and source specific queues). If the packet was already in
	 * the queue (same SSRC and sequence number), it is not
	 * inserted but deleted.
	 *
	 * @param packetLink link to a packet just received and
	 * generally validated and processed by onRTPPacketRecv.
	 *
	 * @return whether the packet was successfully inserted.
	 * @retval false when the packet is duplicated (there is
	 * already a packet from the same source with the same
	 * timestamp).
	 * @retval true when the packet is not duplicated.
	 **/
	bool
	insertRecvPacket(IncomingRTPPktLink* packetLink);

	/**
	 * This function performs the physical I/O for reading a
	 * packet from the source.  It is a virtual that is
	 * overriden in the derived class.
	 *
	 * @return number of bytes read.
	 * @param buffer of read packet.
	 * @param length of data to read.
	 * @param host address of source.
	 * @param port number of source.
	 **/
	virtual size_t
	recvData(unsigned char* buffer, size_t length,
		 InetHostAddress& host, tpport_t& port) = 0;

	virtual size_t
	getNextDataPacketSize() const = 0;

	mutable ThreadLock recvLock;
	// reception queue
	IncomingRTPPktLink* recvFirst, * recvLast;
	// values for packet validation.
	static const uint8 defaultMinValidPacketSequence;
	static const uint16 defaultMaxPacketMisorder;
	static const uint16 defaultMaxPacketDropout;
	uint8 minValidPacketSequence;
	uint16 maxPacketMisorder;
	uint16 maxPacketDropout;
	static const size_t defaultMembersSize;
	uint8 sourceExpirationPeriod;
	mutable Mutex cryptoMutex;
        std::list<CryptoContext *> cryptoContexts;
};

/** @}*/ // iqueue

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  //CCXX_RTP_IQUEUE_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
