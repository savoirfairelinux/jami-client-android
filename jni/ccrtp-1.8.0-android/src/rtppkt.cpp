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
 * @file rtppkt.cpp
 * @short StaticPayloadFormat, DynamicPayloadFormat, RTPPacket,
 * OutgoingRTPPkt and IncomingRTPPkt classes implementation.
 **/

#include "private.h"
#include <ccrtp/rtppkt.h>
#include <ccrtp/CryptoContext.h>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

// Default to 8Khz when no value is specified.
const uint32 PayloadFormat::defaultRTPClockRate = 8000;

//uint32 PayloadFormat::staticRates[lastStaticPayloadType]
uint32 StaticPayloadFormat::staticAudioTypesRates[] = {
    // audio types:
    8000,    //  0 - sptPCMU
    0,       //  1 - reserved
    8000,    //  2 - sptG726_32
    8000,    //  3 - sptGSM
    8000,    //  4 - sptG723
    8000,    //  5 - sptDVI4_8000
    16000,   //  6 - sptDVI4_16000
    8000,    //  7 - sptLPC
    8000,    //  8 - sptPCMA
    8000,    //  9 - sptG722
    44100,   // 10 - sptL16_DUAL
    44100,   // 11 - sptL16_MONO
    8000,    // 12 - sptQCELP
    0,       // 13 - reserved
    90000,   // 14 - sptMPA
    8000,    // 15 - sptG728
    11015,   // 16 - sptDVI4_11025
    22050,   // 17 - sptDVI4_22050
    8000     // 18 - sptG729
/*  0,       // reserved
    0,       // unassigned
    0,       // unassigned
    0,       // unassigned
    0        // unassigned
*/
    // All video types have 90000 hz RTP clock rate.
    // If sometime in the future a static video payload type is
    // defined with a different RTP clock rate (quite
    // unprobable). This table and/or the StaticPayloadType
    // constructor must be changed.
};

StaticPayloadFormat::StaticPayloadFormat(StaticPayloadType type)
{
    setPayloadType( (type <= lastStaticPayloadType)? type : 0);
    if ( type <= sptG729 ) {
        // audio static type
        setRTPClockRate(staticAudioTypesRates[type]);
    } else {
        // video static type
        setRTPClockRate(90000);
    }
}

DynamicPayloadFormat::DynamicPayloadFormat(PayloadType type, uint32 rate)
{
    PayloadFormat::setPayloadType(type);
    setRTPClockRate(rate);
}

// constructor commonly used for incoming packets
RTPPacket::RTPPacket(const unsigned char* const block, size_t len, bool duplicate) :
total((uint32)len), duplicated(duplicate)
{
    const RTPFixedHeader* const header =
        reinterpret_cast<const RTPFixedHeader*>(block);
    hdrSize = sizeof(RTPFixedHeader) + (header->cc << 2);
    if ( header->extension ){
        RTPHeaderExt *ext = (RTPHeaderExt *)(block + hdrSize);
                hdrSize += sizeof(uint32) + (ntohs(ext->length) * 4);
    }
    if ( header->padding )
        len -= block[len - 1];
    payloadSize = (uint32)(len - hdrSize);

    if ( duplicate ) {
        buffer = new unsigned char[len];
        setbuffer(block,len,0);
    } else {
        buffer = const_cast<unsigned char*>(block);
    }
}

// constructor commonly used for outgoing packets
RTPPacket::RTPPacket(size_t hdrlen, size_t plen, uint8 paddinglen, CryptoContext* pcc ) :
payloadSize((uint32)plen), buffer(NULL), hdrSize((uint32)hdrlen),
duplicated(false)
{
    total = (uint32)(hdrlen + payloadSize);
    // compute if there must be padding
    uint8 padding = 0;
    if ( 0 != paddinglen ) {
        padding = paddinglen - (total % paddinglen);
        total += padding;
    }
    srtpLength = 0;
    srtpDataOffset = 0;
    if (pcc != NULL) {
        // compute additional memory for SRTP data
        srtpLength = pcc->getTagLength() + pcc->getMkiLength();
        srtpDataOffset = total; // SRTP data go behind header plus payload plus padding
    }

    // now we know the actual total length of the packet, get  some memory
    // but take SRTP data into account. Don't change total because some RTP
    // functions rely on the fact that total is the overall size (without
    // the SRTP data)
    buffer = new unsigned char[total + srtpLength];
    *(reinterpret_cast<uint32*>(getHeader())) = 0;
    getHeader()->version = CCRTP_VERSION;
    if ( 0 != padding ) {
        memset(buffer + total - padding,0,padding - 1);
        buffer[total - 1] = padding;
        getHeader()->padding = 1;
    } else {
        getHeader()->padding = 0;
    }
}

void RTPPacket::endPacket()
{
#ifdef  CCXX_EXCEPTIONS
    try {
#endif
        delete [] buffer;
#ifdef  CCXX_EXCEPTIONS
    } catch (...) { };
#endif
}

void RTPPacket::reComputePayLength(bool padding)
{
    // If payloadsize was computed without padding set then re-compute
    // payloadSize after the padding bit was set and set padding flag
    // in RTP header - option for SRTP
    if (padding) {
        size_t len = 0;
        getHeader()->padding = 1;
        len -= buffer[payloadSize - 1];
        payloadSize = (uint32)(payloadSize - len);
    }
}

OutgoingRTPPkt::OutgoingRTPPkt(const uint32* const csrcs, uint16 numcsrc,
const unsigned char* const hdrext, uint32 hdrextlen,
const unsigned char* const data, size_t datalen,
uint8 paddinglen, CryptoContext* pcc) :
RTPPacket((getSizeOfFixedHeader() + sizeof(uint32) * numcsrc + hdrextlen),datalen,paddinglen, pcc)
{
    uint32 pointer = (uint32)getSizeOfFixedHeader();
    // add CSCR identifiers (putting them in network order).
    setCSRCArray(csrcs,numcsrc);
    pointer += numcsrc * sizeof(uint32);

    // add header extension.
    setbuffer(hdrext,hdrextlen,pointer);
    setExtension(hdrextlen > 0);
    pointer += hdrextlen;

    // add data.
    setbuffer(data,datalen,pointer);
}

OutgoingRTPPkt::OutgoingRTPPkt(const uint32* const csrcs, uint16 numcsrc,
const unsigned char* data, size_t datalen, uint8 paddinglen, CryptoContext* pcc) :
RTPPacket((getSizeOfFixedHeader() + sizeof(uint32) *numcsrc),datalen, paddinglen, pcc)
{
    uint32 pointer = (uint32)getSizeOfFixedHeader();
    // add CSCR identifiers (putting them in network order).
    setCSRCArray(csrcs,numcsrc);
    pointer += numcsrc * sizeof(uint32);

    // not needed, as the RTPPacket constructor sets by default
    // the whole fixed header to 0.
    // getHeader()->extension = 0;

    // add data.
    setbuffer(data,datalen,pointer);
}

OutgoingRTPPkt::OutgoingRTPPkt(const unsigned char* data, size_t datalen,
uint8 paddinglen, CryptoContext* pcc) :
RTPPacket(getSizeOfFixedHeader(),datalen,paddinglen, pcc)
{
    // not needed, as the RTPPacket constructor sets by default
    // the whole fixed header to 0.
    //getHeader()->cc = 0;
    //getHeader()->extension = 0;

    setbuffer(data,datalen,getSizeOfFixedHeader());
}

void OutgoingRTPPkt::setCSRCArray(const uint32* const csrcs, uint16 numcsrc)
{
    setbuffer(csrcs, numcsrc * sizeof(uint32),getSizeOfFixedHeader());
    uint32* csrc = const_cast<uint32*>(getCSRCs());
    for ( int i = 0; i < numcsrc; i++ )
        csrc[i] = htonl(csrc[i]);
    getHeader()->cc = numcsrc;
}

void OutgoingRTPPkt::protect(uint32 ssrc, CryptoContext* pcc)
{
    /* Encrypt the packet */
    uint64 index = ((uint64)pcc->getRoc() << 16) | (uint64)getSeqNum();

    pcc->srtpEncrypt(this, index, ssrc);

    // NO MKI support yet - here we assume MKI is zero. To build in MKI
    // take MKI length into account when storing the authentication tag.

    /* Compute MAC */
    pcc->srtpAuthenticate(this, pcc->getRoc(),
                                const_cast<uint8*>(getRawPacket()+srtpDataOffset) );
    /* Update the ROC if necessary */
    if (getSeqNum() == 0xFFFF ) {
        pcc->setRoc(pcc->getRoc() + 1);
    }
}

// These masks are valid regardless of endianness.
const uint16 IncomingRTPPkt::RTP_INVALID_PT_MASK = (0x7e);
const uint16 IncomingRTPPkt::RTP_INVALID_PT_VALUE = (0x48);

IncomingRTPPkt::IncomingRTPPkt(const unsigned char* const block, size_t len) :
RTPPacket(block,len)
{
    // first, perform validity check:
    // 1) check protocol version
    // 2) it is not an SR nor an RR
    // 3) consistent length field value (taking CC value and P and
    //    X bits into account)
    if ( getProtocolVersion() != CCRTP_VERSION || (getPayloadType() & RTP_INVALID_PT_MASK) == RTP_INVALID_PT_VALUE) {
            /*
         ||
         getPayloadSize() <= 0 ) {
            */
        headerValid = false;
        return;
    }
    headerValid = true;
    cachedTimestamp = getRawTimestamp();
    cachedSeqNum = ntohs(getHeader()->sequence);
    cachedSSRC = ntohl(getHeader()->sources[0]);
}

int32 IncomingRTPPkt::unprotect(CryptoContext* pcc)
{
    if (pcc == NULL) {
        return true;
    }

    /*
     * This is the setting of the packet data when we come to this
     * point:
     *
     * total:       complete length of received data
     * buffer:      points to data as received from network
     * hdrSize:     length of header including header extension
     * payloadSize: length of data excluding hdrSize and padding
     *
     * Because this is an SRTP packet we need to adjust some values here.
     * The SRTP MKI and authentication data is always at the end of a
     * packet. Thus compute the position of this data.
     */

    uint32 srtpDataIndex = total - (pcc->getTagLength() + pcc->getMkiLength());

    // now adjust total because some RTP functions rely on the fact that
    // total is the full length of data without SRTP data.
    total -= pcc->getTagLength() + pcc->getMkiLength();

    // recompute payloadSize by subtracting SRTP data
    payloadSize -= pcc->getTagLength() + pcc->getMkiLength();

    // unused??
    // const uint8* mki = getRawPacket() + srtpDataIndex;
    const uint8* tag = getRawPacket() + srtpDataIndex + pcc->getMkiLength();

    /* Replay control */
    if (!pcc->checkReplay(cachedSeqNum)) {
        return -2;
    }
    /* Guess the index */
    uint64 guessedIndex = pcc->guessIndex(cachedSeqNum);

    uint32 guessedRoc = guessedIndex >> 16;
    uint8* mac = new uint8[pcc->getTagLength()];

    pcc->srtpAuthenticate(this, guessedRoc, mac);
    if (memcmp(tag, mac, pcc->getTagLength()) != 0) {
        delete[] mac;
        return -1;
    }
    delete[] mac;

    /* Decrypt the content */
    pcc->srtpEncrypt( this, guessedIndex, cachedSSRC );

    /* Update the Crypto-context */
    pcc->update(cachedSeqNum);

    return 1;
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
