// Copyright (C) 2001,2002,2004,2005 Federico Montesino Pouzols <fedemp@altern.org>.
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
 * @file oqueue.h
 *
 * @short Generic RTP output queues.
 **/

#ifndef	CCXX_RTP_OQUEUE_H_
#define CCXX_RTP_OQUEUE_H_

#include <ccrtp/queuebase.h>
#include <ccrtp/CryptoContext.h>
#include <list>

#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

/**
 * @defgroup oqueue Generic RTP output queues.
 * @{
 **/

/**
 * @class DestinationListHandler
 *
 * This class handles a list of destination addresses. Stores network
 * addresses as InetAddress objects.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT DestinationListHandler
{
protected:
	struct TransportAddress;
	std::list<TransportAddress*> destList;

public:
	DestinationListHandler();

	~DestinationListHandler();

	/**
	 * Get whether there is only a destination in the list.
	 **/
	inline bool isSingleDestination() const
	{ return (1 == destList.size()); }

	inline TransportAddress* getFirstDestination() const
	{ return destList.front(); }

	inline void lockDestinationList() const
	{ destinationLock.readLock(); }

	inline void unlockDestinationList() const
	{ destinationLock.unlock(); }

protected:
	inline void writeLockDestinationList() const
	{ destinationLock.writeLock(); }

	/**
	 * Locks the object before modifying it.
	 **/
	bool
	addDestinationToList(const InetAddress& ia, tpport_t data,
			     tpport_t control);

	/**
	 * Locks the object before modifying it.
	 **/
	bool removeDestinationFromList(const InetAddress& ia,
				       tpport_t dataPort,
				       tpport_t controlPort);

	struct TransportAddress
	{
		TransportAddress(InetAddress na, tpport_t dtp, tpport_t ctp) :
			networkAddress(na), dataTransportPort(dtp),
			controlTransportPort(ctp)
		{  }

		inline const InetAddress& getNetworkAddress() const
		{ return networkAddress; }

		inline tpport_t getDataTransportPort() const
		{ return dataTransportPort; }

		inline tpport_t getControlTransportPort() const
		{ return controlTransportPort; }

		InetAddress networkAddress;
		tpport_t dataTransportPort, controlTransportPort;
	};

private:
	mutable ThreadLock destinationLock;
};

#ifdef	CCXX_IPV6
/**
 * @class DestinationListHandler
 *
 * This class handles a list of destination addresses. Stores network
 * addresses as InetAddress objects.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT DestinationListHandlerIPV6
{
protected:
	struct TransportAddressIPV6;
	std::list<TransportAddressIPV6*> destListIPV6;

public:
	DestinationListHandlerIPV6();

	~DestinationListHandlerIPV6();

	/**
	 * Get whether there is only a destination in the list.
	 **/
	inline bool isSingleDestinationIPV6() const
	{ return (1 == destListIPV6.size()); }

	inline TransportAddressIPV6* getFirstDestinationIPV6() const
	{ return destListIPV6.front(); }

	inline void lockDestinationListIPV6() const
	{ destinationLock.readLock(); }

	inline void unlockDestinationListIPV6() const
	{ destinationLock.unlock(); }

protected:
	inline void writeLockDestinationListIPV6() const
	{ destinationLock.writeLock(); }

	/**
	 * Locks the object before modifying it.
	 **/
	bool
	addDestinationToListIPV6(const IPV6Address& ia, tpport_t data,
			     tpport_t control);

	/**
	 * Locks the object before modifying it.
	 **/
	bool removeDestinationFromListIPV6(const IPV6Address& ia,
				       tpport_t dataPort,
				       tpport_t controlPort);

	struct TransportAddressIPV6
	{
		TransportAddressIPV6(IPV6Address na, tpport_t dtp, tpport_t ctp) :
			networkAddress(na), dataTransportPort(dtp),
			controlTransportPort(ctp)
		{  }

		inline const IPV6Address& getNetworkAddress() const
		{ return networkAddress; }

		inline tpport_t getDataTransportPort() const
		{ return dataTransportPort; }

		inline tpport_t getControlTransportPort() const
		{ return controlTransportPort; }

		IPV6Address networkAddress;
		tpport_t dataTransportPort, controlTransportPort;
	};

private:
	mutable ThreadLock destinationLock;
};

#endif

/**
 * @class OutgoingDataQueue
 *
 * A generic outgoing RTP data queue supporting multiple destinations.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT OutgoingDataQueue:
	public OutgoingDataQueueBase,
#ifdef	CCXX_IPV6
	protected DestinationListHandlerIPV6,
#endif
	protected DestinationListHandler
{
public:
#ifdef	CCXX_IPV6
	bool
	addDestination(const IPV6Address& ia,
		       tpport_t dataPort = DefaultRTPDataPort,
		       tpport_t controlPort = 0);

	bool
	forgetDestination(const IPV6Address& ia,
			  tpport_t dataPort = DefaultRTPDataPort,
			  tpport_t controlPort = 0);

#endif

	bool
	addDestination(const InetHostAddress& ia,
		       tpport_t dataPort = DefaultRTPDataPort,
		       tpport_t controlPort = 0);

	bool
	addDestination(const InetMcastAddress& ia,
		       tpport_t dataPort = DefaultRTPDataPort,
		       tpport_t controlPort = 0);

	bool
	forgetDestination(const InetHostAddress& ia,
			  tpport_t dataPort = DefaultRTPDataPort,
			  tpport_t controlPort = 0);

	bool
	forgetDestination(const InetMcastAddress& ia,
			  tpport_t dataPort = DefaultRTPDataPort,
			  tpport_t controlPort = 0);

	/**
	 * Add csrc as the CSRC identifier of a new contributor. This
	 * method adds the CSRC identifier to a list of contributors
	 * that will be inserted in every packet enqueued from now on.
	 **/
	void
	addContributor(uint32 csrc);

	/**
	 * Remove CSRC from the list of contributors.
	 **/
	bool
	removeContributor(uint32 csrc);

 	/**
 	 * Determine if outgoing packets are waiting to send.
 	 *
 	 * @return true if there are packets waiting to be send.
 	 */
 	bool
	isSending() const;


	/**
	 * This is used to create a data packet in the send queue.
	 * Sometimes a "NULL" or empty packet will be used instead, and
	 * these are known as "silent" packets.  "Silent" packets are
	 * used simply to "push" the scheduler along more accurately
	 * by giving the appearence that a next packet is waiting to
	 * be sent and to provide a valid timestamp for that packet.
	 *
	 * @param stamp Timestamp for expected send time of packet.
	 * @param data Value or NULL if special "silent" packet.
	 * @param len May be 0 to indicate a default by payload type.
	 **/
	void
	putData(uint32 stamp, const unsigned char* data = NULL, size_t len = 0);

        /**
         * This is used to create a data packet and send it immediately.
         * Sometimes a "NULL" or empty packet will be used instead, and
         * these are known as "silent" packets.  "Silent" packets are
         * used simply to "push" the scheduler along more accurately
         * by giving the appearence that a next packet is waiting to
         * be sent and to provide a valid timestamp for that packet.
         *
         * @param stamp Timestamp immediate send time of packet.
         * @param data Value or NULL if special "silent" packet.
         * @param len May be 0 to indicate a default by payload type.
         **/
        void
        sendImmediate(uint32 stamp, const unsigned char* data = NULL, size_t len = 0);


	/**
	 * Set padding. All outgoing packets will be transparently
	 * padded to a multiple of paddinglen.
	 *
	 * @param paddinglen pad packets to a length multiple of paddinglen.
	 **/
	void setPadding(uint8 paddinglen)
	{ sendInfo.paddinglen = paddinglen; }

	/**
	 * Set marker bit for the packet in which the next data
	 * provided will be send. When transmitting audio, should be
	 * set for the first packet of a talk spurt. When transmitting
	 * video, should be set for the last packet for a video frame.
	 *
	 * @param mark Marker bit value for next packet.
	 **/
	void setMark(bool mark)
	{ sendInfo.marked = mark; }

	/**
	 * Get wheter the mark bit will be set in the next packet.
	 **/
	inline bool getMark() const
	{ return sendInfo.marked; }

	/**
	 * Set partial data for an already queued packet.  This is often
	 * used for multichannel data.
	 *
	 * @param timestamp Timestamp of packet.
	 * @param data Buffer to copy from.
	 * @param offset Offset to copy from.
	 * @param max Maximum data size.
	 * @return Number of packet data bytes set.
	 **/
	size_t
	setPartial(uint32 timestamp, unsigned char* data, size_t offset, size_t max);

	inline microtimeout_t
	getDefaultSchedulingTimeout() const
	{ return defaultSchedulingTimeout; }

	/**
	 * Set the default scheduling timeout to use when no data
	 * packets are waiting to be sent.
	 *
	 * @param to timeout in milliseconds.
	 **/
	inline void
	setSchedulingTimeout(microtimeout_t to)
	{ schedulingTimeout = to; }

	inline microtimeout_t
	getDefaultExpireTimeout() const
	{ return defaultExpireTimeout; }

	/**
	 * Set the "expired" timer for expiring packets pending in
	 * the send queue which have gone unsent and are already
	 * "too late" to be sent now.
	 *
	 * @param to timeout to expire unsent packets in milliseconds.
	 **/
	inline void
	setExpireTimeout(microtimeout_t to)
	{ expireTimeout = to; }

	inline microtimeout_t getExpireTimeout() const
	{ return expireTimeout; }

	/**
	 * Get the total number of packets sent so far
	 *
	 * @return total number of packets sent
	 */
	inline uint32
	getSendPacketCount() const
	{ return sendInfo.packetCount; }

	/**
	 * Get the total number of octets (payload only) sent so far.
	 *
	 * @return total number of payload octets sent in RTP packets.
	 **/
	inline uint32
	getSendOctetCount() const
	{ return sendInfo.octetCount; }

        /**
         * Get the sequence number of the next outgoing packet.
         *
         * @return the 16 bit sequence number.
         **/
        inline uint16
        getSequenceNumber() const
        { return sendInfo.sendSeq; }

        /**
         * Set ouput queue CryptoContext.
         *
         * The endQueue method (provided by RTPQueue) deletes all
         * registered CryptoContexts.
         *
         * @param cc Pointer to initialized CryptoContext.
         */
        void
        setOutQueueCryptoContext(CryptoContext* cc);

        /**
         * Remove output queue CryptoContext.
         *
         * The endQueue method (provided by RTPQueue) also deletes all
         * registered CryptoContexts.
         *
         * @param cc Pointer to initialized CryptoContext to remove.
         */
        void
        removeOutQueueCryptoContext(CryptoContext* cc);

        /**
         * Get an output queue CryptoContext identified by SSRC
         *
         * @param ssrc Request CryptoContext for this incoming SSRC
         * @return Pointer to CryptoContext of the SSRC of NULL if no context
         * available for this SSRC.
         */
        CryptoContext*
        getOutQueueCryptoContext(uint32 ssrc);


protected:
	OutgoingDataQueue();

	virtual ~OutgoingDataQueue()
	{ }

	struct OutgoingRTPPktLink
	{
		OutgoingRTPPktLink(OutgoingRTPPkt* pkt,
				   OutgoingRTPPktLink* p,
				   OutgoingRTPPktLink* n) :
			packet(pkt), prev(p), next(n) { }

		~OutgoingRTPPktLink() { delete packet; }

		inline OutgoingRTPPkt* getPacket() { return packet; }

		inline void setPacket(OutgoingRTPPkt* pkt) { packet = pkt; }

		inline OutgoingRTPPktLink* getPrev() { return prev; }

		inline void setPrev(OutgoingRTPPktLink* p) { prev = p; }

		inline OutgoingRTPPktLink* getNext() { return next; }

		inline void setNext(OutgoingRTPPktLink* n) { next = n; }

		// the packet this link refers to.
		OutgoingRTPPkt* packet;
		// global outgoing packets queue.
		OutgoingRTPPktLink * prev, * next;
	};

	/**
	 * This is used to write the RTP data packet to one or more
	 * destinations.  It is used by both sendImmediate and by
	 * dispatchDataPacket.
	 *
	 * @param RTP packet to send.
	 */
	void
	dispatchImmediate(OutgoingRTPPkt *packet);

	/**
	 * This computes the timeout period for scheduling transmission
	 * of the next packet at the "head" of the send buffer.  If no
	 * packets are waiting, a default timeout is used.  This actually
	 * forms the "isPending()" timeout of the rtp receiver in the
	 * service thread.
	 *
	 * @return timeout until next packet is scheduled to send.
	 **/
	microtimeout_t
	getSchedulingTimeout();

	/**
	 * This function is used by the service thread to process
	 * the next outgoing packet pending in the sending queue.
	 *
	 * @return number of bytes sent.  0 if silent, <0 if error.
	 **/
	size_t
	dispatchDataPacket();

	/**
	 * For thoses cases in which the application requires a method
	 * to set the sequence number for the outgoing stream (such as
	 * for implementing the RTSP PLAY command).
	 *
	 * @param seqNum next sequence number to be used for outgoing packets.
	 *
	 **/
	inline void
	setNextSeqNum(uint32 seqNum)
	{ sendInfo.sendSeq = seqNum; }

	inline uint32
	getCurrentSeqNum(void)
	{ return sendInfo.sendSeq; }

	/**
	 */
	inline void
	setInitialTimestamp(uint32 ts)
	{ initialTimestamp = ts; }

	/**
	 */
	inline uint32
	getInitialTimestamp()
	{ return initialTimestamp; }

	void purgeOutgoingQueue();

        virtual void
        setControlPeer(const InetAddress &host, tpport_t port) {}

#ifdef	CCXX_IPV6
	virtual void
	setControlPeerIPV6(const IPV6Address &host, tpport_t port) {}
#endif

        // The crypto contexts for outgoing SRTP sessions.
	mutable Mutex cryptoMutex;
        std::list<CryptoContext *> cryptoContexts;

private:
        /**
	 * A hook to filter packets being sent that have been expired.
	 *
	 * @param - expired packet from the send queue.
	 **/
	inline virtual void onExpireSend(OutgoingRTPPkt&)
	{ }

	virtual void
        setDataPeer(const InetAddress &host, tpport_t port) {}

#ifdef	CCXX_IPV6
	virtual void
	setDataPeerIPV6(const IPV6Address &host, tpport_t port) {}
#endif

	/**
	 * This function performs the physical I/O for writing a
	 * packet to the destination.  It is a virtual that is
	 * overriden in the derived class.
	 *
	 * @param buffer Pointer to data to write.
	 * @param len Length of data to write.
	 * @return number of bytes sent.
	 **/
	virtual size_t
	sendData(const unsigned char* const buffer, size_t len) {return 0;}

#ifdef	CCXX_IPV6
	virtual size_t
	sendDataIPV6(const unsigned char* const buffer, size_t len) {return 0;}
#endif

	static const microtimeout_t defaultSchedulingTimeout;
	static const microtimeout_t defaultExpireTimeout;
	mutable ThreadLock sendLock;
	// outgoing data packets queue
	OutgoingRTPPktLink* sendFirst, * sendLast;
	uint32 initialTimestamp;
	// transmission scheduling timeout for the service thread
	microtimeout_t schedulingTimeout;
	// how old a packet can reach in the sending queue before deletetion
	microtimeout_t expireTimeout;


	struct {
		// number of packets sent from the beginning
		uint32 packetCount;
		// number of payload octets sent from the beginning
		uint32 octetCount;
		// the sequence number of the next packet to sent
		uint16 sendSeq;
		// contributing sources
		uint32 sendSources[16];
		// how many CSRCs to send.
		uint16 sendCC;
		// pad packets to a paddinglen multiple
		uint8 paddinglen;
		// This flags tells whether to set the bit M in the
		// RTP fixed header of the packet in which the next
		// provided data will be sent.
		bool marked;
		// whether there was not loss.
		bool complete;
		// ramdonly generated offset for the timestamp of sent packets
		uint32 initialTimestamp;
		// elapsed time accumulated through successive overflows of
		// the local timestamp field
		timeval overflowTime;
	} sendInfo;
};

/** @}*/ // oqueue

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  //CCXX_RTP_OQUEUE_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
