// Copyright (C) 2001,2002,2004,2007 Federico Montesino Pouzols <fedemp@altern.org>.
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

#ifndef	CCXX_RTP_RTCPPKT_H_
#define CCXX_RTP_RTCPPKT_H_

#include <ccrtp/base.h>

#ifdef CCXX_NAMESPACES
namespace ost {
#endif

/** 
 * @file rtcppkt.h 
 *
 * @short RTCP packets handling.
 **/

/**
 * @defgroup rtcppacket RTCP compound packets manipulation.
 * @{
 **/

/** 
 * @enum SDESItemType
 * @short SDES items that may be carried in a Source DEScription RTCP packet.
 *
 * CNAME is mandatory in each RTCP compound packet (except when
 * splitted for partial encryption), the others are optional and have
 * different sending frequencies, though with recommended default
 * values.
 **/
typedef enum
{
	SDESItemTypeEND = 0,         ///< END of SDES item list.
	SDESItemTypeCNAME,           ///< Canonical end-point identifier.
	SDESItemTypeNAME,            ///< Personal NAME of the user.
	SDESItemTypeEMAIL,           ///< EMAIL address of the user.
	SDESItemTypePHONE,           ///< Phone number of the user.
	SDESItemTypeLOC,             ///< Location where the user is.
	SDESItemTypeTOOL,            ///< Application or tool.
	SDESItemTypeNOTE,            ///< Comment usually reporting state.
	SDESItemTypePRIV,            ///< Private extension.
	SDESItemTypeH323CADDR,       ///< H323 callable address.
	SDESItemTypeLast = SDESItemTypeH323CADDR ///< Last defined code.
}       SDESItemType;

/**
 * @class RTCPCompoundHandler
 * @short low level structs and RTCP packet parsing and building
 * methods.
 *
 * Intended to be used, through inheritance, in RTCP management
 * classes, such as QueueRTCPManager.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/
class __EXPORT RTCPCompoundHandler 
{
public:
	inline void setPathMTU(uint16 mtu)
	{ pathMTU = mtu; }

	inline uint16 getPathMTU()
	{ return pathMTU; }

#ifdef	CCXX_PACKED
#pragma pack(1)	
#endif
	/**
	 * @struct ReceiverInfo
	 *
	 * Struct for the data contained in a receiver info
	 * block. Receiver info blocks can be found in SR (sender
	 * report) or RR (receiver report) RTCP packets.
	 **/
	struct ReceiverInfo
	{
		uint8 fractionLost;      ///< packet fraction lost.
		uint8 lostMSB;           ///< cumulative lost MSB of 3 octets.
		uint16 lostLSW;          ///< cumulative lost two LSB.
		uint32 highestSeqNum;    ///< highest sequence number.
		uint32 jitter;           ///< arrival jitter.
		uint32 lsr;              ///< last sender report timestamp.
		uint32 dlsr;             ///< delay since last sender report.
	};
	
	/**
	 * @struct RRBlock
	 *
	 * Struct for a receiver info block in a SR (sender report) or an RR
	 * (receiver report) RTCP packet.
	 **/
	struct RRBlock
	{
		uint32 ssrc;                   ///< source identifier.
		ReceiverInfo rinfo;            ///< info about the source.
	};
	
	/**
	 * @struct RecvReport
	 *
	 * @short raw structure of the source and every receiver report in an
	 * SR or RR RTCP packet.  
	 **/
	struct RecvReport
	{
		uint32 ssrc;                 ///< source identifier.
		RRBlock blocks[1];           ///< receiver report blocks.
	};
	
	/**
	 * @struct SenderInfo
	 *
	 * Struct for the sender info block in a SR (sender report)
	 * RTCP packet.
	 **/
	struct SenderInfo
	{
		uint32 NTPMSW;              ///< NTP timestamp higher octets.
		uint32 NTPLSW;              ///< NTP timestamp lower octets.
		uint32 RTPTimestamp;        ///< RTP timestamp.
		uint32 packetCount;         ///< cumulative packet counter.
		uint32 octetCount;          ///< cumulative octet counter.
	};
	
	/**
	 * @struct SendReport
	 *
	 * Struct for SR (sender report) RTCP packets. 
	 **/
	struct SendReport
	{
		uint32 ssrc;       ///< source identifier.
		SenderInfo sinfo;  ///< actual sender info.
		RRBlock blocks[1]; ///< possibly several receiver info blocks.
	};
	
	/**
	 * @struct SDESItem
	 *
	 * Struct for an item description of a SDES packet.
	 **/
	struct SDESItem 
	{
		uint8 type;       ///< item identifier.
		uint8 len;        ///< item len in octets.
		char data[1];     ///< item content.
	};

	/**
	 * @struct SDESChunk
	 *
	 * Struct for a chunk of items in a SDES RTCP packet.
	 **/
	struct SDESChunk 
	{
		uint32 getSSRC() const
		{ return (ntohl(ssrc)); }

		uint32 ssrc;      ///< SSRC identifer from sender.
		SDESItem item;    ///< SDES item from sender.		
	};

	/**
	 * @struct BYEPacket
	 *
	 * @short Struct for BYE (leaving session) RTCP packets.
	 **/
	struct BYEPacket 
	{
		uint32 ssrc;          ///< ssrc identifier of source leaving.
		uint8 length;         ///< [optional] length of reason.
	};
	
	/**
	 * @struct APPPacket
	 *
	 * @short Struct for APP (application specific) RTCP packets.
	 **/
	struct APPPacket
	{
		uint32 ssrc;           ///< ssrc identifier of source.
		char name [4];         ///< Name of the APP packet,
				       ///interpreted as a sequence of
				       ///four characters.
		unsigned char data[1]; ///< application dependent data.
	};

	/**
	 * @struct FIRPacket
	 *
	 * @short Struct for Full Intra-frame Request (FIR) RTCP
	 * packet. Specific for H.261 sessions (see RFC 2032).
	 **/
	struct FIRPacket
	{
		uint32 ssrc;           ///< ssrc identifier of source.
	};

	/**
	 * @struct NACKPacket
	 *
	 * @short Struct for Negative ACKnowledgements (NACK) RTCP
	 * packet. Specific for H.261 sessions (see RFC 2032).
	 **/
	struct NACKPacket
	{
		uint32 ssrc;           ///< ssrc identifier of source.
		uint16 fsn;            ///< First Sequence Number lost.
		uint16 blp;            ///< Bitmask of following Lost Packets.
	};

	/**
	 * @struct RTCPFixedHeader
	 * Fixed RTCP packet header. First 32-bit word in any RTCP
	 * packet.
	 */
	struct RTCPFixedHeader
	{
#if	__BYTE_ORDER == __BIG_ENDIAN
		///< For big endian boxes
		unsigned char version:2;      ///< Version, currently 2.
		unsigned char padding:1;      ///< Padding bit.
		unsigned char block_count:5;  ///< Number of RR, SR, or SDES chunks.
#else
		///< For little endian boxes
		unsigned char block_count:5;  ///< Number of RR, SR, or SDES chunks. 
		unsigned char padding:1;      ///< Padding bit.
		unsigned char version:2;      ///< Version, currently 2.
#endif
		uint8 type;    ///< type of RTCP packet.
		uint16 length; ///< number of 32-bit words in the packet (*minus one*).
	};

	/**
	 * @struct RTCPPacket 
	 *
	 * @short Struct representing general RTCP packet headers as they are
	 * sent through the network.
	 * 
	 * This struct consists of a fixed header, always at the
	 * beginning of any RTCP packet, and a union for all the RTCP
	 * packet types supported.
	 **/
	struct RTCPPacket
	{
		/**
		 * @enum Type rtp.h cc++/rtp.h
		 * 
		 * RTCP packet types. They are registered with IANA.
		 */
		typedef enum {
			tSR = 200,      ///< Sender Report.
			tRR,            ///< Receiver Report.
			tSDES,          ///< Source DEScription.
			tBYE,           ///< End of participation.
			tAPP,           ///< APPlication specific.
			tFIR   = 192,   ///< Full Intra-frame request.
			tNACK  = 193,   ///< Negative ACK.
			tXR             ///< Extended Report.
		}       Type;
		
		/**
		 * Get the packet length specified in its header, in
		 * octets and in host order.
		 **/
		uint32 getLength() const
		{ return ((ntohs(fh.length) + 1) << 2); }

		/**
		 * Get the SSRC identifier specified in the packet
		 * header, in host order.
		 **/
		uint32 getSSRC()  const			
		{ return (ntohl(info.RR.ssrc)); } // SSRC is always the first
						  // word after fh.

		RTCPFixedHeader fh;           ///< Fixed RTCP header.

		// An RTCP packet may be of any of the types defined
		// above, including APP specific ones.
		union
		{
			SendReport SR;
			RecvReport RR;
			SDESChunk SDES; 
			BYEPacket BYE;
			APPPacket APP;
			NACKPacket NACK;
			FIRPacket FIR;
		}       info;        ///< Union for SR, RR, SDES, BYE and APP
	};
#ifdef	CCXX_PACKED
#pragma pack()
#endif

protected:
	enum { defaultPathMTU = 1500 };

	RTCPCompoundHandler(uint16 mtu = defaultPathMTU);

	~RTCPCompoundHandler();

	/**
	 * Perform RTCP compound packet header validity check as
	 * specified in draft-ietv-avt-rtp-new. This method follows
	 * appendix A.2. Correct version, payload type, padding bit
	 * and length of every RTCP packet in the compound are
	 * verified.
	 *
	 * @param len length of the RTCP compound packet in 
	 *        the reception buffer
	 * @return whether the header is valid.
	 */
	bool
	checkCompoundRTCPHeader(size_t len);

	// buffer to hold RTCP compound packets being sent. Allocated
	// in construction time
	unsigned char* rtcpSendBuffer;
	// buffer to hold RTCP compound packets being
	// received. Allocated at construction time
	unsigned char* rtcpRecvBuffer;

	friend class RTCPSenderInfo;
	friend class RTCPReceiverInfo;
private:
	// path MTU. RTCP packets should not be greater than this
	uint16 pathMTU;
	// masks for RTCP header validation;
	static const uint16 RTCP_VALID_MASK;
	static const uint16 RTCP_VALID_VALUE;
};

/**
 * @class RTCPReceiverInfo
 * @short Report block information of SR/RR RTCP reports.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/
class __EXPORT RTCPReceiverInfo
{
public:
	RTCPReceiverInfo(void* ri)
	{ memcpy(&receiverInfo,&ri,
		 sizeof(RTCPCompoundHandler::ReceiverInfo));}

        RTCPReceiverInfo(RTCPCompoundHandler::ReceiverInfo& si)
		: receiverInfo( si )
        {
        }

	~RTCPReceiverInfo()
	{ }

	/**
	 * Get fraction of lost packets, as a number between 0 and
	 * 255.
	 **/
	inline uint8
	getFractionLost() const
	{ return receiverInfo.fractionLost; }

	inline uint32
	getCumulativePacketLost() const
	{ return ( ((uint32)ntohs(receiverInfo.lostLSW)) + 
		   (((uint32)receiverInfo.lostMSB) << 16) ); }
	
	inline uint32
	getExtendedSeqNum() const
	{ return ntohl(receiverInfo.highestSeqNum); }

	/**
	* Get the statistical variance of the RTP data packets
	* interarrival time.
	*
	* @return Interarrival jitter, in timestamp units.
	**/
	uint32
	getJitter() const
	{ return ntohl(receiverInfo.jitter); }

	/**
	 * Get the integer part of the NTP timestamp of the last SR
	 * RTCP packet received from the source this receiver report
	 * refers to.
	 **/
	uint16
	getLastSRNTPTimestampInt() const
	{ return (uint16)((ntohl(receiverInfo.lsr) & 0xFFFF0000) >> 16); }

	/**
	 * Get the fractional part of the NTP timestamp of the last SR
	 * RTCP packet received from the source this receiver report
	 * refers to.
	 **/
	uint16
	getLastSRNTPTimestampFrac() const
	{ return (uint16)(ntohl(receiverInfo.lsr) & 0xFFFF); }

	/**
	 * Get the delay between the last SR packet received and the
	 * transmission of this report.
	 *
	 * @return Delay, in units of 1/65536 seconds
	 **/
	uint32
	getDelayLastSR() const
	{ return ntohl(receiverInfo.dlsr); }	

private:
	RTCPCompoundHandler::ReceiverInfo receiverInfo;
};

/**
 * @class RTCPSenderInfo
 * @short Sender block information of SR RTCP reports.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/
class __EXPORT RTCPSenderInfo
{
public:
	RTCPSenderInfo(void* si)
	{ memcpy(&senderInfo,&si,
		 sizeof(RTCPCompoundHandler::SenderInfo));}

        RTCPSenderInfo(RTCPCompoundHandler::SenderInfo& si)
		: senderInfo( si )
        {
        }

	~RTCPSenderInfo()
	{ }

	/**
	 * Get integer part of the NTP timestamp of this packet.
	 * @see NTP2Timeval
	 **/
	uint32
	getNTPTimestampInt() const
	{ return ntohl(senderInfo.NTPMSW); }

	/**
	 * Get fractional part of the NTP timestamp of this packet.
	 * @see NTP2Timeval
	 **/
	uint32
	getNTPTimestampFrac() const
	{ return ntohl(senderInfo.NTPLSW); }

	inline uint32
	getRTPTimestamp() const
	{ return ntohl(senderInfo.RTPTimestamp); }

	/**
	 * Get count of sent data packets.
	 **/
	inline uint32
	getPacketCount() const
	{ return ntohl(senderInfo.packetCount); }
	
	inline uint32
	getOctetCount() const
	{ return ntohl(senderInfo.octetCount); }

private:
	RTCPCompoundHandler::SenderInfo senderInfo;
};

/**
 * Convert a NTP timestamp, expressed as two 32-bit long words, into a
 * timeval value.
 *
 * @param msw Integer part of NTP timestamp.
 * @param lsw Fractional part of NTP timestamp.
 * @return timeval value corresponding to the given NTP timestamp.
 **/
timeval
NTP2Timeval(uint32 msw, uint32 lsw);

/**
 * Convert a time interval, expressed as a timeval, into a 32-bit time
 * interval expressed in units of 1/65536 seconds.
 *
 * @param t Timeval interval.
 * @return 32-bit value corresponding to the given timeval interval.
 **/
uint32
timevalIntervalTo65536(timeval& t);

/** @}*/ // rtcppacket

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  // ndef CCXX_RTP_RTCPPKT_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */

