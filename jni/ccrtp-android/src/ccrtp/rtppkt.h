// Copyright (C) 2002 Federico Montesino Pouzols <fedemp@altern.org>.
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

#ifndef	CCXX_RTP_RTPPKT_H_
#define CCXX_RTP_RTPPKT_H_

#include <ccrtp/base.h>
#include <ccrtp/formats.h>
#include <ccrtp/CryptoContext.h>

#ifdef CCXX_NAMESPACES
namespace ost {
#endif

/**
 * @file rtppkt.h
 *
 * @short RTP packets handling.
 **/

/**
 * @defgroup rtppacket RTP data packets manipulation.
 * @{
 **/

/**
 * @class RTPPacket
 * @short A base class for both IncomingRTPPkt and OutgoingRTPPkt.
 *
 * Provides common low level header structures and related
 * methods. This class provides an interface that allows for partial
 * and generic manipulation of RTP data packets. Values are returned
 * in host order, except raw structures, which are returned as they
 * are sent through the network.
 *
 * @author David Sugar <dyfet@ostel.com>
 **/

class CryptoContext;

class  __EXPORT RTPPacket
{
private:
	struct RTPFixedHeader;
	struct RTPHeaderExt;

public:
	/**
	 * Constructor, construct a packet object given the memory
	 * zone its content (header and payload) is stored. Commonly
	 * used to build RTPPacket objects from incoming data.
	 *
	 * @param block whole packet
	 * @param len total length (header + payload + padding) of the
	 *        packet
	 * @param duplicate whether to memcopy the packet. At present,
	 *        this feature is not used.
	 * @note used in IncomingRTPPkt.
	 **/
	RTPPacket(const unsigned char* const block, size_t len,
		  bool duplicate = false);

	/**
	 * Construct a packet object without specifying its real
	 * content yet. Commonly used for outgoing packets. Header
	 * fields and payload must be filled in by another methods or
	 * by a derived constructor.
	 *
	 * @param hdrlen length of the header (including CSRC and extension).
	 * @param plen payload length.
	 * @param paddinglen pad packet to a multiple of paddinglen
	 * @note used in OutgoingRTPPkt.
	 */
        RTPPacket(size_t hdrlen, size_t plen, uint8 paddinglen, CryptoContext* pcc= NULL);

	/**
	 * Get the length of the header, including contributing
	 * sources identifiers and header extension, if present.
	 *
	 * @return number of octets.
	 **/
	inline uint32
	getHeaderSize() const
	{ return hdrSize; }

	/**
	 * @return pointer to the payload section of the packet.
	 **/
	inline const uint8* const
	getPayload() const
	{ return (uint8*)(buffer + getHeaderSize()); }

	/**
	 * @return length of the payload section, in octets.
	 **/
	inline uint32
	getPayloadSize() const
	{ return payloadSize; }

	/**
	 * @return value of the PT header field.
	 **/
	inline PayloadType
	getPayloadType() const
	{ return static_cast<PayloadType>(getHeader()->payload); }

	/**
	 * @return value of the sequence number header field, in host order.
	 **/
	inline uint16
	getSeqNum() const
	{ return cachedSeqNum; }

	/**
	 * @return packet timestamp in host order.
	 **/
	inline uint32
	getTimestamp() const
	{ return cachedTimestamp; }

	/**
	 * @return RTP protocol version of packet.
	 **/
	inline uint8
	getProtocolVersion() const
	{ return getHeader()->version; }

	/**
	 * Ask whether the packet contains padding bytes at the end
	 * @return true if the header padding bit is 1.
	 **/
	inline bool
	isPadded() const
	{ return getHeader()->padding; }

	/**
	 * Get the number of octets padding the end of the payload
	 * section.
	 *
	 * @return Padding length in octets.
	 **/
	inline uint8
	getPaddingSize() const
	{ return buffer[total - 1]; }

	/**
	 * Ask whether the packet is marked (for isntance, is a new
	 * talk spurt in some audio profiles).
	 *
	 * @return true is the header marker bit is 1.
	 **/
	inline bool
	isMarked() const
	{ return getHeader()->marker; }

	/**
	 * Ask whether the packet contains header extensions.
	 *
	 * @return true if the header extension bit is 1.
	 **/
	inline bool
	isExtended() const
	{ return getHeader()->extension; }

	/**
	 * Get the number of contributing sources specified in the
	 * packet header.
	 **/
	inline uint16
	getCSRCsCount() const
	{ return getHeader()->cc; }

	/**
	 * Get the 32-bit identifiers of the contributing sources for
	 * the packet as an array, of length getCSRCsCount().
	 *
	 * @return An array of CSRC identifiers as they are in the
	 * packet (in network order).
	 **/
	inline const uint32*
	getCSRCs() const
	{ return static_cast<const uint32*>(&(getHeader()->sources[1])); }

	/**
	 * Get the first 16 bits (in network order) of the header of
	 * the RTP header extension. Its meaning is undefined at this
	 * level.
	 *
	 * @return 0 if the packet has no header extension, otherwise
	 *         the first 16 bits of the header extension, in
	 *         network order.
	 *
	 * @note 0 could be a valid value for the first 16 bits, in
	 *         that case RTPPacket::isExtended() should be use.
	 **/
	inline uint16
	getHdrExtUndefined() const
	{ return (isExtended()? getHeaderExt()->undefined : 0); }

	/**
	 * Get the length (in octets) of the data contained in the
	 * header extension. Note that this length does not include
	 * the four octets at the beginning of the header extension.
	 *
	 * @return 0 if the packet has no header extension, otherwise
	 *         the length.
	 *
	 * @note 0 is a valid value for this field, so
	 *       RTPPacket::isExtended() should be used.
	 **/
	inline uint32
	getHdrExtSize() const
	{ return (isExtended()?
		  (static_cast<uint32>(ntohs(getHeaderExt()->length)) << 2) :
		  0); }

	/**
	 * Get the content of the header extension.
	 *
	 * @return NULL if the packet has no header extension, otherwise
	 *         a pointer to the packet header extension content.
	 **/
	inline const unsigned char*
	getHdrExtContent() const
	{ return (isExtended() ?
		  (reinterpret_cast<const unsigned char*>(getHeaderExt()) +
		   sizeof(RTPHeaderExt)) :
		  NULL); }

	/**
	 * Get the raw packet as it will be sent through the network.
	 *
	 * @return memory zone where the raw packet structure is
	 *         stored in.
	 **/
	inline const unsigned char* const
	getRawPacket() const
	{ return buffer; }

	/**
	 * Get the raw packet length, including header, extension,
	 * payload and padding.
	 *
	 * @return size of the raw packet structure.
	 **/
	inline uint32
	getRawPacketSize() const
	{ return total; }

        inline uint32
        getRawPacketSizeSrtp() const
        { return total + srtpLength; }

        inline size_t
	getSizeOfFixedHeader() const
	{ return sizeof(RTPFixedHeader); }
	
	/**
     * Re-compute payload length.
     *
     * This recomputation may be necessary in case of SRTP. We need to decrypt
     * the packet before we can handle padding. See @c takeInDataPacket in
     * @c incqueue.cpp
     *
     * @param padding
     *     If true then set padding flag in RTP header and re-compute 
     *     payloadSize.
     */
    void reComputePayLength(bool padding);
    
protected:
	/**
	 * Destructor, free the buffer provided in the constructor.
	 **/
	inline virtual ~RTPPacket()
	{ endPacket(); }

	/**
	 * Free memory allocated for the packet.
	 **/
	void
	endPacket();

	/**
	 * Return low level structure for the header of the packet.
	 *
	 * @return RTPFixedHeader pointer to the header of the packet.
	 **/
	inline RTPFixedHeader*
	getHeader() const
	{ return reinterpret_cast<RTPFixedHeader*>(buffer); }

	inline void
	setExtension(bool e)
	{ getHeader()->extension = e; }

	/**
	 * Get a pointer to RTPHeaderExt pointing after the RTP header
	 * (fixed part plus contributing sources). No check for
	 * for the X bit is done.
	 *
	 * @return header extension if present, garbage if not.
	 **/
	inline const RTPHeaderExt*
	getHeaderExt() const
	{
         uint32 fixsize = sizeof(RTPFixedHeader) + (getHeader()->cc << 2);
	 return (reinterpret_cast<RTPHeaderExt*>(buffer + fixsize));
	}

	/**
	 * Obtain the absolute timestamp carried in the packet header.
	 *
	 * @return 32-bit timestamp in host order.
	 **/
	inline uint32
	getRawTimestamp() const
	{ return ntohl(getHeader()->timestamp); }

	inline void
	setbuffer(const void* src, size_t len, size_t pos)
	{ memcpy(buffer + pos,src,len); }

	/// Packet sequence number in host order.
	uint16 cachedSeqNum;
	/// Packet timestamp in host order (includes initial shift).
	uint32 cachedTimestamp;

        /**
         * Offset into packet memory pointing to area for SRTP data.
         *
         * This offset points to the memory where the SRTP protect will
         * store the authentication and MKI data.
         */
        uint32 srtpDataOffset;

        /**
         * Lebgth of additional SRTP data.
         *
         * Covers the SRTP authentication and MKI data.
         */
        int32 srtpLength;

        /// total length, including header, payload and padding
        uint32 total;

        /// note: payload (not full packet) size.
        uint32 payloadSize;

private:
	/// packet in memory
	unsigned char* buffer;
	/// size of the header, including contributing sources and extensions
	uint32 hdrSize;
	/// whether the object was contructed with duplicated = true
	bool duplicated;

#ifdef	CCXX_PACKED
#pragma pack(1)
#endif
	/**
	 * @struct RTPFixedHeader
	 * @short RTP fixed header as it is send through the network.
	 *
	 * A low-level representation for generic RTP packet header as
	 * defined in RFC 1889. A packet consists of the fixed RTP
	 * header, a possibly empty list of contributing sources and
	 * the payload. Header contents are kept in network (big
	 * endian) order.
	 **/
	struct RTPFixedHeader
	{
#if	__BYTE_ORDER == __BIG_ENDIAN
		/// For big endian boxes
		unsigned char version:2;       ///< Version, currently 2
		unsigned char padding:1;       ///< Padding bit
		unsigned char extension:1;     ///< Extension bit
		unsigned char cc:4;            ///< CSRC count
		unsigned char marker:1;        ///< Marker bit
		unsigned char payload:7;       ///< Payload type
#else
		/// For little endian boxes
		unsigned char cc:4;            ///< CSRC count
		unsigned char extension:1;     ///< Extension bit
		unsigned char padding:1;       ///< Padding bit
		unsigned char version:2;       ///< Version, currently 2
		unsigned char payload:7;       ///< Payload type
		unsigned char marker:1;        ///< Marker bit
#endif
		uint16 sequence;        ///< sequence number
		uint32 timestamp;       ///< timestamp
		uint32 sources[1];      ///< contributing sources
	};

	/**
 	 * @struct RFC2833Payload
	 * @short a structure defining RFC2833 Telephony events.
	 *
	 * structure to define RFC2833 telephony events in RTP.  You can
	 * use this by recasing the pointer returned by getPayload().
	 */

public:
	struct RFC2833Payload
	{
#if __BYTE_ORDER == __BIG_ENDIAN
        	uint8 event : 8;
        	bool ebit : 1;
        	bool rbit : 1;
        	uint8 vol : 6;
        	uint16 duration : 16;
#else
        	uint8 event : 8;
        	uint8 vol : 6;
        	bool rbit : 1;
        	bool ebit : 1;
        	uint16 duration : 16;
#endif
	};

private:
	/**
	 * @struct RTPHeaderExt
	 *
	 * Fixed component of the variable-length header extension,
	 * appended to the fixed header, after the CSRC list, when X
	 * == 1.
	 **/
	struct RTPHeaderExt
	{
		uint16 undefined; ///< to be defined
		uint16 length;    ///< number of 32-bit words in the extension
	};
#ifdef	CCXX_PACKED
#pragma pack()
#endif

	/* definitions for access to most common 2833 fields... */

public:
	/**
	 * Fetch a raw 2833 packet.
	 *
	 * @return low level 2833 data structure.
	 */
	inline struct RFC2833Payload *getRaw2833Payload(void)
		{return (struct RFC2833Payload *)getPayload();}

	/**
	 * Fetch 2833 duration field.
	 *
	 * @return 2833 duration in native host machine byte order.
	 */
	inline uint16 get2833Duration(void)
		{return ntohs(getRaw2833Payload()->duration);}

	/**
	 * Set 2833 duration field.
	 *
	 * @param timestamp to use, native host machine byte order.
	 */
	inline void set2833Duration(uint16 timestamp)
		{getRaw2833Payload()->duration = htons(timestamp);}
};

/**
 * @class OutgoingRTPPkt
 * @short RTP packets being sent.
 *
 * This class is intented to construct packet objects just before they
 * are inserted into the sending queue, so that they are processed in
 * a understandable and format independent manner inside the stack.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT OutgoingRTPPkt : public RTPPacket
{
public:
	/**
	 * Construct a new packet to be sent, containing several
	 * contributing source identifiers, header extensions and
	 * payload.
         *
         * A new copy in memory (holding all this components
	 * along with the fixed header) is created. If the pointer
         * to the SRTP CryptoContext is not NULL and holds a CryptoContext
         * for the SSRC take the SSRC data into account when computing
         * the required memory buffer.
	 *
	 * @param csrcs array of countributing source 32-bit
	 *        identifiers, in host order.
	 * @param numcsrc number of CSRC identifiers in the array.
	 * @param hdrext whole header extension.
	 * @param hdrextlen size of whole header extension, in octets.
	 * @param data payload.
	 * @param datalen payload length, in octets.
	 * @param paddinglen pad packet to a multiple of paddinglen.
         * @param pcc Pointer to the SRTP CryptoContext, defaults to NULL
         * if not specified.
	 *
	 * @note For efficiency purposes, since this constructor is
	 * valid for all packets but is too complex for the common
	 * case, two simpler others are provided.
	 **/
	OutgoingRTPPkt(const uint32* const csrcs, uint16 numcsrc,
		       const unsigned char* const hdrext, uint32 hdrextlen,
		       const unsigned char* const data, size_t datalen,
                       uint8 paddinglen= 0, CryptoContext* pcc= NULL);

	/**
	 * Construct a new packet to be sent, containing several
	 * contributing source identifiers and payload.
         *
         * A new copy in
	 * memory (holding all this components along with the fixed
         * header) is created. If the pointer
         * to the SRTP CryptoContext is not NULL and holds a CryptoContext
         * for the SSRC take the SSRC data into account when computing
         * the required memory buffer.
	 *
	 * @param csrcs array of countributing source 32-bit
	 * identifiers, in host order.
	 * @param numcsrc number of CSRC identifiers in the array.
	 * @param data payload.
	 * @param datalen payload length, in octets.
	 * @param paddinglen pad packet to a multiple of paddinglen.
         * @param pcc Pointer to the SRTP CryptoContext, defaults to NULL
         * if not specified.
         **/
	OutgoingRTPPkt(const uint32* const csrcs, uint16 numcsrc,
		       const unsigned char* const data, size_t datalen,
                       uint8 paddinglen= 0, CryptoContext* pcc= NULL);

	/**
	 * Construct a new packet (fast variant, with no contributing
	 * sources and no header extension) to be sent.
         *
         * A new copy in
         * memory (holding the whole packet) is created. If the pointer
         * to the SRTP CryptoContext is not NULL and holds a CryptoContext
         * for the SSRC take the SSRC data into account when computing
         * the required memory buffer.
	 *
	 * @param data payload.
	 * @param datalen payload length, in octets.
	 * @param paddinglen pad packet to a multiple of paddinglen.
         * @param pcc Pointer to the SRTP CryptoContext, defaults to NULL
         * if not specified.
         **/
	OutgoingRTPPkt(const unsigned char* const data, size_t datalen,
                       uint8 paddinglen= 0, CryptoContext* pcc= NULL);

	~OutgoingRTPPkt()
	{ }

	/**
	 * @param pt Packet payload type.
	 **/
	inline void
	setPayloadType(PayloadType pt)
	{ getHeader()->payload = pt; }

	/**
         * Sets the sequence number in the header.
         *
	 * @param seq Packet sequence number, in host order.
	 **/
	inline void
	setSeqNum(uint16 seq)
	{
		cachedSeqNum = seq;
		getHeader()->sequence = htons(seq);
	}

	/**
	 * @param pts Packet timestamp, in host order.
	 **/
	inline void
	setTimestamp(uint32 pts)
	{
		cachedTimestamp = pts;
		getHeader()->timestamp = htonl(pts);
	}

	/**
	 * Set synchronization source numeric identifier.
	 *
	 * @param ssrc 32-bit Synchronization SouRCe numeric
	 * identifier, in host order.
	 **/
	inline void
	setSSRC(uint32 ssrc) const
	{ getHeader()->sources[0] = htonl(ssrc); }

	/**
	 * Set synchronization source numeric identifier. Special
	 * version to save endianness conversion.
	 *
	 * @param ssrc 32-bit Synchronization SouRCe numeric
	 * identifier, in network order.
	 **/
	inline void
	setSSRCNetwork(uint32 ssrc) const
	{ getHeader()->sources[0] = ssrc; }

	/**
	 * Specify the value of the marker bit. By default, the marker
	 * bit of outgoing packets is false/0. This method allows to
	 * explicity specify and change that value.
	 *
	 * @param mark value for the market bit.
	 */
	inline void
	setMarker(bool mark)
	{ getHeader()->marker = mark; }

        /**
         * Called packet is setup.
         *
         * This private method computes the SRTP data and stores it in the
         * packet. Then encrypt the payload data (ex padding).
         */
        void protect(uint32 ssrc, CryptoContext* pcc);

	/**
	 * Outgoing packets are equal if their sequence numbers match.
	 **/
	inline bool
	operator==(const OutgoingRTPPkt &p) const
	{ return ( this->getSeqNum() == p.getSeqNum() ); }

	/**
	 * Outgoing packets are not equal if their sequence numbers differ.
	 **/
	inline bool
	operator!=(const OutgoingRTPPkt &p) const
	{ return ( this->getSeqNum() != p.getSeqNum() ); }

private:
	/**
	 * Copy constructor from objects of its same kind, declared
	 * private to avoid its use.
	 **/
	OutgoingRTPPkt(const OutgoingRTPPkt &o);

	/**
	 * Assignment operator from objects of its same kind, declared
	 * private to avoid its use.
	 **/
	OutgoingRTPPkt&
	operator=(const OutgoingRTPPkt &o);

	/**
	 * Set the list of CSRC identifiers in an RTP packet,
	 * switching host to network order.
	 */
	void setCSRCArray(const uint32* const csrcs, uint16 numcsrc);

};

/**
 * @class IncomingRTPPkt
 *
 * @short RTP packets received from other participants.
 *
 * This class is intented to construct a packet object just after
 * every packet is received by the scheduled queue, so that they are
 * processed in an understandable and format independent manner inside
 * the stack.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 */
class __EXPORT IncomingRTPPkt : public RTPPacket
{
public:
	/**
	 * Build an RTP packet object from a data buffer. This
	 * constructor first performs a generic RTP data packet header
	 * check, whose result can be checked via isHeaderValid().
	 *
	 * @param block pointer to the buffer the whole packet is stored in.
	 * @param len length of the whole packet, expressed in octets.
	 *
	 * @note If check fails, the packet object is
	 * incomplete. checking isHeaderValid() is recommended before
	 * using a new RTPPacket object.
	 **/
	IncomingRTPPkt(const unsigned char* block, size_t len);

	~IncomingRTPPkt()
	{ }

	/**
	 * Get validity of this packet
	 * @return whether the header check performed at construction
	 *         time ended successfully.
	 **/
	inline bool
	isHeaderValid()
	{ return headerValid; }

	/**
	 * Get synchronization source numeric identifier.
	 *
	 * @return 32-bits Synchronization SouRCe numeric identifier,
	 * in host order.
	 **/
	inline uint32
	getSSRC() const
	{ return cachedSSRC; }

        /**
         * Unprotect a received packet.
         *
         * Perform SRTP processing on this packet.
         *
         * @param pcc Pointer to SRTP CryptoContext.
         * @return
         *     one if no errors, -1 if authentication failed, -2 if
         *     replay check failed
         */
        int32
        unprotect(CryptoContext* pcc);

	/**
	 * Two incoming packets are equal if they come from sources
	 * with the same SSRC and have the same sequence number.
	 **/
	inline bool
	operator==(const IncomingRTPPkt &p) const
	{ return ( (this->getSeqNum() == p.getSeqNum()) &&
		   (this->getSSRC() == p.getSSRC()) ); }

	/**
	 * Two incoming packets are not equal if they come from
	 * different sources or have different sequence numbers.
	 **/
	inline bool
	operator!=(const IncomingRTPPkt &p) const
	{ return !( *this == p ); }

private:
	/**
	 * Copy constructor from objects of its same kind, declared
	 * private to avoid its use.
	 **/
	IncomingRTPPkt(const IncomingRTPPkt &ip);

	/**
	 * Assignment operator from objects of its same kind, declared
	 * private to avoid its use.
	 */
	IncomingRTPPkt&
	operator=(const IncomingRTPPkt &ip);

	/// Header validity, checked at construction time.
	bool headerValid;
	/// SSRC 32-bit identifier in host order.
	uint32 cachedSSRC;
	// Masks for RTP header validation: types matching RTCP SR or
	// RR must be rejected to avoid accepting misaddressed RTCP
	// packets.
	static const uint16 RTP_INVALID_PT_MASK;
	static const uint16 RTP_INVALID_PT_VALUE;
};

/** @}*/ // rtppacket

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  // ndef CCXX_RTP_RTPPKT_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
