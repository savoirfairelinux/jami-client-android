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

#ifndef	CCXX_RTP_FORMATS_H_
#define CCXX_RTP_FORMATS_H_

#ifdef CCXX_NAMESPACES
namespace ost {
#endif

/** 
 * @file formats.h 
 *
 * @short Payload types and formats.
 **/

/**
 * @defgroup payload Payload types and formats.
 * @{
 **/

/**
 * @typedef PayloadType
 *
 * @short RTP payload type numeric identifier.
 *
 * At the RTP level a payload format is identified with a 7 bit
 * number. This number is binded to a payload format through non-RTP
 * means (SIP, SAP, application specific rules, etc.)
 **/
typedef uint8 PayloadType;

/// Predefined value for invalid or uninitialized payload type variables.
const PayloadType ptINVALID = 128;

/**
 * @enum StaticPayloadType
 *
 * @short RTP static payload types (default bindings) defined in the
 * AVP profile.
 **/
typedef enum {
	// Types for audio formats:
	sptPCMU = 0,        ///< ITU-T G.711. mu-law audio 8 Khz (RFC 1890)
	firstStaticPayloadType = sptPCMU,
	// 1016 static payload type is now deprecated. Type 1 is reserved.
	// spt1016,         ///< CELP audio (FED-STD 1016) (RFC 1890)
	sptG726_32 = 2,     ///< ITU-T G.726. ADPCM audio (RFC 1890)
	sptGSM,             ///< GSM audio (RFC 1890)
	sptG723,            ///< ITU-T G.723. MP-MLQ ACELP audio (RFC 1890)
	sptDVI4_8000,       ///< Modified IMA ADPCM audio 8Khz (RFC 1890)
	sptDVI4_16000,      ///< Modified IMA ADPCM audio 16Khz (RFC 1890)
	sptLPC,             ///< LPC audio (RFC 1890)
	sptPCMA,            ///< ITU-T G.711 A-law audio 8 Khz (RFC 1890) 
	sptG722,            ///< Audio (RFCs 1890, 3047)
	sptL16_DUAL,        ///< Linear uncompressed dual audio (RFC 1890)
	sptL16_MONO,        ///< Linear uncompressed mono audio (RFC 1890)
	sptQCELP,           ///< Audio at 8000 hz.
	// Type 13 is reserved.
	sptMPA = 14,	    ///< MPEG Audio elem. stream (RFCs 1890, 2250)
	sptG728,            ///< ITU-T G.728. LD-CELP audio
	sptDVI4_11025,      ///< DVI audio at 11025 hz (by Joseph Di Pol)
	sptDVI4_22050,      ///< DVI audio at 22050 hz (by Joseph Di Pol)
	sptG729,            ///< ITU-T G.729. CS-ACELP audio
	// Type 19 is reserved. Types 20 - 23 are unassigned.
	lastStaticAudioPayloadType = sptG729,

	// Types for video formats:
	// Type 24 is unassigned.
	sptCELB = 25,       ///< Sun's propietary video (RFCs 1890, 2029)
	sptJPEG,            ///< JPEG (ISO 10918) video (RFCs 1890, 2435)
	// Type 27 is unassigned.
	sptNV = 28,         ///< Ron Frederick's nv audio (RFC 1890)
	// Types 29 and 30 are unassigned.
	sptH261 = 31,       ///< ITU-T H.261 video (RFCs 1890, 2032) 
	sptMPV,             ///< MPEG Video elem. stream (RFCs 1890, 2250)
	sptMP2T,            ///< MPEG 2 Transport stream (RFCs 1890, 2250)
	sptH263,            ///< ITU-T H.263 video (RFCs 2190, 2429)
	// Types 35 - 71 are unassigned.
	// Types 72 - 76 are reserved.
	// Types 96 - 127 are dynamic.
	lastStaticPayloadType = sptH263
}	StaticPayloadType;

/**
 * @class PayloadFormat
 * @short Base payload format class.
 *
 * The properties of a payload format that, as an RTP stack, ccRTP
 * takes into account are the payload type (numeric identifier) and
 * the RTP clock rate.
 *
 * This is a base class for both StaticPayloadFormat and
 * DynamicPayloadFormat.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/
class __EXPORT PayloadFormat
{
public:
	/**
	 * Get payload type numeric identifier carried in RTP packets.
	 *
	 * @return payload type numeric identifier.
	 **/
	inline PayloadType getPayloadType() const
	{ return payloadType; }

	/**
	 * Get RTP clock rate for this payload format. Note this
	 * method provides the RTP clock rate (for the timestamp in
	 * RTP data packets headers), which is not necessarily the
	 * same as the codec clock rate.
	 *
	 * @return RTP clock rate in Hz.
	 **/
	inline uint32 getRTPClockRate() const
	{ return RTPClockRate; }
	
protected:
	/**
	 * PayloadFormat must not be used but as base class.
	 **/
	PayloadFormat()
	{ }
	
	/**
	 * PayloadFormat must not be used but as base class.
	 **/
	inline virtual ~PayloadFormat()	
	{ }

	/**
	 * Set payload type numeric identifier carried in RTP packets.
	 *
	 * @param pt payload type number.
	 **/
	inline void setPayloadType(PayloadType pt)
	{ payloadType = pt; }

	/**
	 * Set RTP clock rate.
	 *
	 * @param rate RTP clock rate in Hz.
	 **/
	inline void setRTPClockRate(uint32 rate)
	{ RTPClockRate = rate; }

	// default clock rate
	static const uint32 defaultRTPClockRate;

private:
	PayloadType payloadType;    ///< Numeric identifier.
	uint32 RTPClockRate;        ///< Rate in Hz.
};

/**
 * @class StaticPayloadFormat
 * @short Static payload format objects.
 *
 * Class of payload formats objects for payload types statically
 * assigned. Because these payloads have an RTP clock rate assigned,
 * it is not specified to the constructor. A call to
 * StaticPayloadFormat(sptPCMU) will set the proper clock rate and any
 * other parameters for that static payload type.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/	
class __EXPORT StaticPayloadFormat : public PayloadFormat
{
public:
	/**
	 * Constructor. Builds a payload format from a static payload
	 * binding identifier, assigning the numeric identifier and
	 * RTP clock rate statically bounded.
	 *
	 * @param type Numeric identifier in the range 0-96.
	 * @note some identifiers are reserved.
	 **/
	StaticPayloadFormat(StaticPayloadType type);
	
private:
	/** 
	 * RTP clock rate for static payload types. There is no need
	 * for a table like this for video types, since they all have
	 * 90000 Khz rate.
	 **/
	static uint32 staticAudioTypesRates[lastStaticAudioPayloadType - 
					    firstStaticPayloadType + 1];
};

/**
 * @class DynamicPayloadFormat
 * @short Dynamic payload format objects.
 *
 * Class of payload formats objects for payload types dynamically
 * negotiated. Because these payloads do not have a fix RTP clock rate
 * assigned, it must be specified to the constructor. This class will
 * be used by applications that support dynamic payload negotiation.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org> 
 **/	
class __EXPORT DynamicPayloadFormat : public PayloadFormat
{
public:
	/**
	 * Constructor. Builds a dynamic payload format from payload
	 * numeric identifier and the corresponding RTP clock rate.
	 *
	 * @param type payload type numeric identifier.
	 * @param rate RTP clock rate.
	 **/
	DynamicPayloadFormat(PayloadType type, uint32 rate);
};

/** @}*/ // payload 

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  // ndef CCXX_RTP_FORMATS_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
