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
 * @file queuebase.h 
 *
 * @short Base classes for RTP queues.
 **/

#ifndef	CCXX_RTP_QUEUEBASE_H_
#define CCXX_RTP_QUEUEBASE_H_

#include <cc++/pointer.h>
#include <ccrtp/rtppkt.h>
#include <ccrtp/sources.h>

#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

/**
 * @defgroup queuebase Base classes for RTP queues.
 * @{
 **/

/** 
 * @class AppDataUnit
 * @short Interface (envelope) to data received over RTP packets.  
 *
 * A class of objects representing data transmitted over RTP packets.
 * Tipically, this object will apply to received data. Data blocks
 * received via RTP connections as well as its related objects
 * (source, etc), are accessed through the methods of this class.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/
class __EXPORT AppDataUnit
{		
public:
	AppDataUnit(const IncomingRTPPkt& packet, const SyncSource& src);
		
	inline ~AppDataUnit()
	{ }
		
	/**
	 * @param src the AppDataUnit object being copied
	 */
	AppDataUnit(const AppDataUnit& src);
		
	/**
	 * Assignment operator
	 * 
	 * @param source the AppDataUnit object being assigned @return
	 * the result of the assignment
	 */
	AppDataUnit&
	operator=(const AppDataUnit& source);
		
	/**
	 * @return type of this data
	 */
	inline PayloadType
	getType() const
	{ return datablock->getPayloadType(); }

	/**
	 *  Get data as it is received in RTP packets (i.e. for
	 *  multi-octet encodings, octets are in network
	 *  order.
	 *
	 * @return Raw pointer to data block.
	 **/
	inline const uint8* const
	getData() const
	{ return datablock->getPayload(); }
		
	/**
	 * @return length of data in octets
	 **/
	size_t
	getSize() const
	{ return datablock->getPayloadSize(); }

	/**
	 * @return Source that sent this data
	 */
	inline const SyncSource&
	getSource() const
	{ return *source; }

	/**
	 * Is this data unit marked?.
	 *
	 * @return true if marked.
	 **/
	inline bool 
	isMarked() const
	{ return datablock->isMarked(); }

	/**
	 * Get data unit sequence number.
	 **/
	inline uint16
	getSeqNum() const
	{ return datablock->getSeqNum(); }

	/**
	 * Get the number of contributing sources in the CSRC list.
	 **/
	inline uint8
	getContributorsCount() const
	{ return (uint8)datablock->getCSRCsCount(); }

	/**
	 * Get the array of 32-bit CSRC identifiers.
	 *
	 * @return NULL if (getContributorsCount() == 0)
	 **/
	inline const uint32*
	getContributorsID() const
	{ return datablock->getCSRCs(); }

private:
	Pointer<const IncomingRTPPkt> datablock;
	const SyncSource* source;
};

/**
 * @class RTPQueueBase
 *
 * A virtual base class for RTP queue hierarchies.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/
class __EXPORT RTPQueueBase
{
public:
	/**
	 * Set the payload format in use, for timing and payload type
	 * identification purposes.
	 *
	 * @param pf payload format to use from now on.
	 * @return whether the payload format has been successfully set.
	 **/
	inline bool
	setPayloadFormat(const PayloadFormat& pf)
	{ 
		currentPayloadType = pf.getPayloadType();
		currentRTPClockRate = pf.getRTPClockRate();
		return true;
	}

	inline uint32 getLocalSSRC() const
	{ return localSSRC; }

	/**
	 * Get the clock rate in RTP clock units (for instance, 8000
	 * units per second for PCMU, or 90000 units per second for
	 * MP2T). This value depends on what payload format has been
	 * selected using setPayloadFormat().
	 *
	 * @return clock rate in RTP clock units.
	 **/
	inline uint32 getCurrentRTPClockRate() const
	{ return currentRTPClockRate; }

	inline PayloadType getCurrentPayloadType() const
	{ return currentPayloadType; }

	inline timeval getInitialTime() const
	{ return initialTime; }

protected:
	/**
	 * @param ssrc If not null, the local SSRC identifier for this
	 * session.
	 **/
	RTPQueueBase(uint32 *ssrc = NULL);

	inline void setLocalSSRC(uint32 ssrc)
	{ localSSRC = ssrc; localSSRCNetwork = htonl(ssrc); }

	inline uint32 getLocalSSRCNetwork() const
	{ return localSSRCNetwork; }

	virtual 
	~RTPQueueBase()
	{ }

	/**
	 * A plugin point for posting of BYE messages.
	 *
	 * @param - reason to leave the RTP session.
	 * @return number of octets sent.
	 **/
	inline virtual size_t
	dispatchBYE(const std::string&)
	{ return 0; }

	inline virtual void
	renewLocalSSRC()
	{ }

private:
	// local SSRC 32-bit identifier
	uint32 localSSRC;
	// SSRC in network byte order
	uint32 localSSRCNetwork;
	// RTP clock rate for the current payload type.
	uint32 currentRTPClockRate;
	// Current payload type set for outgoing packets and expected
	// from incoming packets.
	PayloadType currentPayloadType;
	// when the queue is created
	timeval initialTime;
};

/**
 * @class OutgoingDataQueueBase
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/
class __EXPORT OutgoingDataQueueBase:
	public virtual RTPQueueBase
{
public:
	inline size_t
	getDefaultMaxSendSegmentSize()
	{ return defaultMaxSendSegmentSize;}

	/**
	 * Set maximum payload segment size before fragmenting sends.
	 *
	 * @param size Maximum payload size.
	 * @return Whether segment size was successfully set.
	 **/
	inline void
	setMaxSendSegmentSize(size_t size)
	{ maxSendSegmentSize = size; }

	inline size_t
	getMaxSendSegmentSize()
	{ return maxSendSegmentSize; }

protected:
	OutgoingDataQueueBase();

	inline virtual
	~OutgoingDataQueueBase()
	{ }

private:
	static const size_t defaultMaxSendSegmentSize;
	// maximum packet size before fragmenting sends.
	size_t maxSendSegmentSize;
};

/**
 * @class IncomingDataQueueBase
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/
class __EXPORT IncomingDataQueueBase:
	public virtual RTPQueueBase
{
public:
	inline size_t getDefaultMaxRecvPacketSize() const
	{ return defaultMaxRecvPacketSize; }

	inline size_t
	getMaxRecvPacketSize() const
	{ return maxRecvPacketSize; }

	/**
	 * @param maxsize maximum length of received RTP data packets,
	 * in octets. Defaults to the value returned by
	 * getDefaultMaxRecvPacketSize().
	 *
	 * @note This method sets a filter for incoming
	 * packets. Setting higher values does not necessarily imply
	 * higher memory usage (this method does not set any buffer
	 * size).
	 **/
	inline void
	setMaxRecvPacketSize(size_t maxsize)
	{ maxRecvPacketSize = maxsize; }

protected:
	IncomingDataQueueBase()
	{ setMaxRecvPacketSize(getDefaultMaxRecvPacketSize()); }

	inline virtual 
	~IncomingDataQueueBase()
	{ }

private:
	static const size_t defaultMaxRecvPacketSize;
	// filter value for received packets length.
	size_t maxRecvPacketSize;
};

/** @}*/ // queuebase

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  //CCXX_RTP_QUEUEBASE_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
