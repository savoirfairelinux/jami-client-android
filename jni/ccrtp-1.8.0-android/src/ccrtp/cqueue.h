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
 * @file cqueue.h
 *
 * @short Generic RTCP control queues.
 **/

#ifndef	CCXX_RTP_CQUEUE_H_
#define CCXX_RTP_CQUEUE_H_

#include <ccrtp/ioqueue.h>

#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

/**
 * @defgroup cqueue Generic RTCP control queues.
 * @{
 **/

/**
 * @class QueueRTCPManager
 * @short Adds generic management of RTCP functions to an RTP data
 * queue.
 *
 * Extends an RTP data i/o queue adding management of RTCP functions:
 *
 * Provide feedback on the quality of the data distribution.
 *
 * Convey the CNAME (persistent transport-level identifier) for every
 * RTP source.
 *
 * Control the sending rate of RTCP packets
 *
 * Convey minimal control information about the participants
 *
 * This class implements generic RTCP behaviour (as specified in RFC
 * 1889/draft-ietf-avt-rtp-new) and may be specialized for specific
 * profiles (see AVPQueue) or particular RTCP extensions.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT QueueRTCPManager : public RTPDataQueue,
	protected RTCPCompoundHandler
{
public:
	/**
	 * Get the most recent sender report received from a
	 * synchronization source.
	 *
	 * @param src Synchronization source of the sender info.
	 * @return Most recent sender info received from src.
	 * @retval NULL when no sender report has been received from
	 * the specified source.
	 **/
	RTCPSenderInfo* getMRSenderInfo(SyncSource& src);

	/**
	 * Ask for the info in the most recent receiver report about
	 * the local source received from the source given as
	 * parameter.
	 *
	 * @param srcFrom Source of the receiver info.
	 * @return most recent receiver info received from src.
	 * @retval NULL when no receiver report has been received from
	 * the specified source.
	 */
	RTCPReceiverInfo* getMRReceiverInfo(SyncSource& srcFrom);

	/**
	 * Set how much time the stack will wait before deleting a
	 * synchronization source that has sent an RTCP BYE packet.
	 *
	 * @param delay delay in microseconds.
	 *
	 * @note The default delay is 1000000 microseconds
	 **/
	void setLeavingDelay(microtimeout_t delay)
	{ leavingDelay = delay; }

	/**
	 * This method sets the maximum end to end delay allowed. If
	 * the processing delay plus the trip time for a packet is
	 * greater than the end to end delay, the packet is discarded,
	 * and the application cannot get it.
	 *
	 * This is a way of setting an upper bound to the end to end
	 * delay, computed as the elapsed time between the packet
	 * timestamping at the sender side, and the picking of the
	 * packet at the receiver side.
	 *
	 * @param t maximum end to end delay allowed. A value of 0
	 * implies there is no limit and is the default
	 */
	inline void
	setEnd2EndDelay(microtimeout_t t)
		{ end2EndDelay = t; }

	inline microtimeout_t
	getDefaultEnd2EndDelay() const
	{ return defaultEnd2EndDelay; }

	inline microtimeout_t
	getEnd2EndDelay() const
	{ return end2EndDelay; }

	/**
	 * Specify the fraction of the total control bandwith to be
	 * dedicated to senders reports.
	 *
	 * @param fraction fraction of bandwidth, must be between 0 an 1.
	 *
	 * This method sets the fraction of the global control
	 * bandwidth that will be dedicated to senders reports. Of
	 * course, <code>1 - fraction</code> will be dedicated to
	 * receivers reports.
	 *
	 * @see setControlBandwidth
	 */
	inline void
	setSendersControlFraction(float fraction)
	{ sendControlBwFract = fraction; recvControlBwFract = 1 - fraction;}

	/**
	 * Manually set the minimum interval for sending RTP compound
	 * packets
	 *
	 * @param interval minimum interval between RTCP packets, in
	 * microseconds.
	 *
	 * @see computeRTCPInterval()
	 **/
	void
	setMinRTCPInterval(microtimeout_t interval)
	{ rtcpMinInterval = interval; }

	/**
	 * Get the total number of RTCP packets sent until now
	 **/
	inline uint32
	getSendRTCPPacketCount() const
	{ return ctrlSendCount; }

protected:
	QueueRTCPManager(uint32 size = RTPDataQueue::defaultMembersHashSize,
			 RTPApplication& app = defaultApplication());

	QueueRTCPManager(uint32 ssrc,
			 uint32 size = RTPDataQueue::defaultMembersHashSize,
			 RTPApplication& app = defaultApplication());

	virtual
	~QueueRTCPManager();

	const RTPApplication&
	getApplication()
	{ return queueApplication; }

	inline void
	setControlBandwidth(float fraction)
	{ controlBwFract = fraction; }

	float
	getControlBandwidth() const
	{ return controlBwFract; }

	/**
	 * Build and send RTCP packets following timing rules
	 * (including the "timer reconsideration" algorithm).
	 **/
	void
	controlTransmissionService();

	/**
	 * Process incoming RTCP packets pending in the control
	 * reception socket.
	 **/
	void
	controlReceptionService();

	/**
	 * Appy collision and loop detection and correction algorithm
	 * when receiving RTCP packets. Follows section 8.2 in
	 * draft-ietf-avp-rtp-new.
	 *
	 * @param sourceLink link to the source object.
	 * @param is_new whether the source has been just recorded.
	 * @param na RTCP packet network address.
	 * @param tp RTCP packet source transport port.
	 *
	 * @return whether the packet must not be discarded.
	 **/
	bool checkSSRCInRTCPPkt(SyncSourceLink& sourceLink, bool is_new,
				InetAddress& na, tpport_t tp);

	void
	endQueueRTCPManager();

	/**
	 * Plug-in for processing (acquire information carried in) an
	 * incoming RTCP Sender Report. The default implementation in
	 * this class only processes the sender information and the
	 * receiver report blocks about the local source.
	 *
	 * @param source Synchronization source this report comes from.
	 * @param SR Sender report structure.
	 * @param blocks Number of report blocks in the packet.
	 **/
	virtual void
	onGotSR(SyncSource& source, SendReport& SR, uint8 blocks);

	/**
	 * Plug-in for processing (acquire information carried in) an
	 * incoming RTCP Receiver Report. The default implementation
	 * in this class only processes the receiver report blocks
	 * about the local source.
	 *
	 * @param source Synchronization source this report comes from.
	 * @param RR Receiver report structure
	 * @param blocks Number of report blocks in the packet
	 **/
	virtual void
	onGotRR(SyncSource& source, RecvReport& RR, uint8 blocks);

	/**
	 * @param source Synchronization source of SDES RTCP packet.
	 * @param pkt SDES RTCP packet received.
	 **/
	bool
	onGotSDES(SyncSource& source, RTCPPacket& pkt);

	/**
	 * Plug-in for handling of SDES chunks.
	 *
	 * @param source Synchronization source of SDES chunk.
	 * @param chunk SDES chunk structure.
	 * @param len Length of chunk, in octets.
	 *
	 * @return whether there was a CNAME.
	 **/
	virtual bool
	onGotSDESChunk(SyncSource& source, SDESChunk& chunk, size_t len);

	/**
	 * Plug-in for handling of APP (application specific) RTCP
	 * packets.
	 *
	 * @param - Synchronization source of this packet.
	 * @param - RTCP APP packet struct.
	 * @param - Length of the app data packet, including ssrc.
	 * name and app. specific data.
	 **/
	inline virtual void
	onGotAPP(SyncSource&, RTCPCompoundHandler::APPPacket&,
		 size_t)
	{ return; }

	inline timeval
	getRTCPCheckInterval()
	{ return rtcpCheckInterval; }

	/**
	 * Get the number of data packets sent at the time the last SR
	 * was generated.
	 **/
	uint32
	getLastSendPacketCount() const
	{ return lastSendPacketCount; }

	/**
	 * @param n Number of members.
	 **/
	inline void
	setPrevMembersNum(uint32 n)
	{ reconsInfo.rtcpPMembers = n; }

	inline uint32
	getPrevMembersCount() const
	{ return reconsInfo.rtcpPMembers; }

	/**
	 * This method is used to send an RTCP BYE packet.  An RTCP
	 * BYE packet is sent when one of the the following
	 * circumstances occur:
	 * - when leaving the session
	 * - when we have detected that another synchronization source
	 * in the same session is using the same SSRC identifier as
	 * us.
	 *
	 * Try to post a BYE message. It will send a BYE packet as
	 * long as at least one RTP or RTCP packet has been sent
	 * before. If the number of members in the session is more
	 * than 50, the algorithm described in section 6.3.7 of
	 * RFC 3550 is applied in order to avoid a flood
	 * of BYE messages.
	 *
	 * @param reason reason to specify in the BYE packet.
	 **/
	size_t
	dispatchBYE(const std::string& reason);

	size_t
	sendControlToDestinations(unsigned char* buffer, size_t len);

private:
	QueueRTCPManager(const QueueRTCPManager &o);

	QueueRTCPManager&
	operator=(const QueueRTCPManager &o);

	/**
	 * Posting of RTCP messages.
	 *
	 * @return std::size_t number of octets sent
	 */
	size_t
	dispatchControlPacket();

	/**
	 * For picking up incoming RTCP packets if they are waiting. A
	 * timeout for the maximum interval since the last RTCP packet
	 * had been received is also returned. This is checked every
	 * rtcpCheckInterval seconds.
	 *
	 * This method decomposes all incoming RTCP compound packets
	 * pending in the control socket and processes each RTCP
	 * packet.
	 *
	 **/
	void
	takeInControlPacket();

	/**
	 * Computes the interval for sending RTCP compound packets,
	 * based on the average size of RTCP packets sent and
	 * received, and the current estimated number of participants
	 * in the session.
	 *
	 * @note This currently follows the rules in section 6 of
	 *       RFC 3550
	 * @todo make it more flexible as recommended in the draft. For now,
	 * we have setMinRTCPInterval.
	 *
	 * @return interval for sending RTCP compound packets
	 **/
	virtual timeval
	computeRTCPInterval();

	/**
	 * Choose which should be the type of the next SDES item
	 * sent. This method is called when packing SDES chunks in a
	 * new RTCP packet.
	 *
	 * @return type of the next SDES item to be sent
	 **/
	virtual SDESItemType
	scheduleSDESItem();

	/**
	 * Plug-in for SSRC collision handling.
	 *
	 * @param - previously identified source.
	 **/
	inline virtual void
	onSSRCCollision(const SyncSource&)
	{ }

	/**
	 * Virtual reimplemented from RTPDataQueue
	 **/
	virtual bool
	end2EndDelayed(IncomingRTPPktLink& p);

	/**
	 * Plug-in for processing of SR/RR RTCP packet
	 * profile-specific extensions (third part of SR reports or
	 * second part of RR reports).
	 *
	 * @param - Content of the profile extension.
	 * @param - Length of the extension, in octets.
	 **/
	inline virtual void
	onGotRRSRExtension(unsigned char*, size_t)
	{ return; }

 	/**
 	 * A plugin point for goodbye message.  Called when a BYE RTCP
 	 * packet has been received from a valid synchronization
 	 * source.
	 *
	 * @param - synchronization source from what a BYE RTCP
	 * packet has been just received.
	 * @param - reason string the source has provided.
 	 **/
 	inline virtual void
	onGotGoodbye(const SyncSource&, const std::string&)
	{ return; }

	/**
	 * Process a BYE packet just received and identified.
	 *
	 * @param pkt previously identified RTCP BYE packet
	 * @param pointer octet number in the RTCP reception buffer
	 *        where the packet is stored
	 * @param len total length of the compount RTCP packet the BYE
	 *        packet to process is contained
	 *
	 * @bug if the bye packet contains several SSRCs,
	 *      eventSourceLeaving is only called for the last one
	 **/
	bool
	getBYE(RTCPPacket &pkt, size_t &pointer, size_t len);

	/**
	 * @return number of Report Blocks packed
	 **/
	uint8
	packReportBlocks(RRBlock* blocks, uint16& len, uint16& available);

	/**
	 * Builds an SDES RTCP packet. Each chunk is built following
	 * appendix A.4 in draft-ietf-avt-rtp-new.
	 *
	 * @param len provisionary length of the RTCP compound packet
	 *
	 * @return
	 **/
	void
	packSDES(uint16& len);

	/**
	 * This must be called in order to update the average RTCP compound
	 * packet size estimation when:
	 *
	 * a compoung RTCP packet is received (6.3.3).
	 *
	 * a compound RTCP packet is transmitted (6.3.6).
	 *
	 * @param len length in octets of the compound RTCP packet
	 * just received/transmitted.
	 **/
	void
	updateAvgRTCPSize(size_t len);

	/**
	 * Apply reverse reconsideration adjustment to timing
	 * parameters when receiving BYE packets and not waiting to
	 * send a BYE.
	 **/
	void
	reverseReconsideration();

	bool
	timerReconsideration();

	/**
	 * Purge sources that do not seem active any more.
	 *
	 * @note MUST be perform at least every RTCP transmission
	 *       interval
	 * @todo implement it. It may be dangerous and anyway should
	 * be optional.
	 **/
	void
	expireSSRCs();

	/**
	 * To be executed when whe are leaving the session.
	 **/
	void
	getOnlyBye();

	/**
	 * Set item value from a string without null termination (as
	 * it is transported in RTCP packets).
	 **/
	void
	setSDESItem(Participant* part, SDESItemType type,
		    const char* const value, size_t len);

	/**
	 * Set PRIV item previx value from a string without null
	 * termination (as it is transported in RTCP packets).
	 **/
	void
	setPRIVPrefix(Participant* part, const char* const value, size_t len);

	/**
	 * For certain control calculations in RTCP, the size of the
	 * underlying network and transport protocols is needed. This
	 * method provides the size of the network level header for
	 * the default case of IP (20 octets). In case other protocol
	 * with different header size is used, this method should be
	 * redefined in a new specialized class.
	 *
	 * @return size of the headers of the network level. IP (20) by
	 *        default.
	 **/
	inline virtual uint16
	networkHeaderSize()
	{ return 20; }

	/**
	 * For certain control calculations in RTCP, the size of the
	 * underlying network and transport protocols is needed. This
	 * method provides the size of the transport level header for
	 * the default case of UDP (8 octets). In case other protocol
	 * with different header size is used, this method should be
	 * redefined in a new specialized class.
	 *
	 * return size of the headers of the transport level. UDP (8)
	 *        by default
	 **/
	inline virtual uint16
	transportHeaderSize()
	{ return 8; }

	SDESItemType
	nextSDESType(SDESItemType t);

	virtual size_t
	sendControl(const unsigned char* const buffer, size_t len) = 0;

	virtual size_t
	recvControl(unsigned char* buffer, size_t len,
		    InetHostAddress& na, tpport_t& tp) = 0;

	virtual bool
	isPendingControl(microtimeout_t timeout) = 0;

	// whether the RTCP service is active
	volatile bool controlServiceActive;
	float controlBwFract, sendControlBwFract, recvControlBwFract;
	// number of RTCP packets sent since the beginning
	uint32 ctrlSendCount;

	// Network + transport headers size, typically size of IP +
	// UDP headers
	uint16 lowerHeadersSize;

	SDESItemType nextScheduledSDESItem;
	static const SDESItemType firstSchedulable;
	static const SDESItemType lastSchedulable;

	// state for rtcp timing. Its meaning is defined in
	// draft-ietf-avt-rtp-new, 6.3.

	// Parameters for timer reconsideration algorithm
	struct {
		timeval rtcpTp, rtcpTc, rtcpTn;
		uint32 rtcpPMembers;
	} reconsInfo;
	bool rtcpWeSent;
	uint16 rtcpAvgSize;
	bool rtcpInitial;
	// last time we checked if there were incoming RTCP packets
	timeval rtcpLastCheck;
	// interval to check if there are incoming RTCP packets
	timeval rtcpCheckInterval;
	// next time to check if there are incoming RTCP packets
	timeval rtcpNextCheck;

	// number of RTP data packets sent at the time of the last
	// RTCP packet transmission.
	uint32 lastSendPacketCount;

	// minimum interval for transmission of RTCP packets. The
	// result of computeRTCPInterval will always be >= (times a
	// random number between 0.5 and 1.5).
	microtimeout_t rtcpMinInterval;

	microtimeout_t leavingDelay;
	static const microtimeout_t defaultEnd2EndDelay;
	// Maximum delay allowed between packet timestamping and
	// packet availability for the application.
	microtimeout_t end2EndDelay;
	// Application this queue is bound to.
	RTPApplication& queueApplication;

	// an empty RTPData
	static const uint16 TIMEOUT_MULTIPLIER;
	static const double RECONSIDERATION_COMPENSATION;
};

/**
 * This class, an RTP/RTCP queue, adds audio/video profile (AVP)
 * specific methods to the generic RTCP service queue
 * (QueueRTCPManager).
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class AVPQueue : public QueueRTCPManager
{
public:
	/**
	 * Specify the bandwith available for control (RTCP) packets.
	 * This method sets the global control bandwidth for both
	 * sender and receiver reports. As recommended in RFC 1890,
	 * 1/4 of the total control bandwidth is dedicated to senders,
	 * whereas 3/4 are dedicated to receivers.
	 *
	 * @param fraction fraction of the session bandwidth, between
	 * 0 and 1
	 *
	 * @note If this method is not called, it is assumed that the
	 * control bandwidth is equal to 5% of the session
	 * bandwidth. Note also that the RFC RECOMMENDS the 5%.
	 *
	 **/
	inline void
	setControlBandwidth(float fraction)
	{ QueueRTCPManager::setControlBandwidth(fraction); }

	float
	getControlBandwidth() const
	{ return QueueRTCPManager::getControlBandwidth(); }

protected:
	AVPQueue(uint32 size = RTPDataQueue::defaultMembersHashSize,
		 RTPApplication& app = defaultApplication()) :
		QueueRTCPManager(size,app)
	{ }

	/**
	 * Local SSRC is given instead of computed by the queue.
	 **/
	AVPQueue(uint32 ssrc, uint32 size =
		 RTPDataQueue::defaultMembersHashSize,
		 RTPApplication& app = defaultApplication()) :
 		QueueRTCPManager(ssrc,size,app)
	{ }
	inline virtual ~AVPQueue()
	{ }
};

/** @}*/ // cqueue

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  //CCXX_RTP_CQUEUE_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
