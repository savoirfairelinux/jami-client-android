// Copyright (C) 2001,2002,2007 Federico Montesino Pouzols <fedemp@altern.org>.
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
 * @file rtcppkt.cpp
 *
 * @short RTCPCompoundHandler class implementation.
 **/

#include "private.h"
#include <ccrtp/rtcppkt.h>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#if __BYTE_ORDER == __BIG_ENDIAN
const uint16 RTCPCompoundHandler::RTCP_VALID_MASK = (0xc000 | 0x2000  | 0xfe);
const uint16 RTCPCompoundHandler::RTCP_VALID_VALUE = ((CCRTP_VERSION << 14) | RTCPPacket::tSR);
#else
const uint16 RTCPCompoundHandler::RTCP_VALID_MASK = (0x00c0 | 0x0020 | 0xfe00);
const uint16 RTCPCompoundHandler::RTCP_VALID_VALUE = ((CCRTP_VERSION << 6) | (RTCPPacket::tSR << 8));
#endif

timeval
NTP2Timeval(uint32 msw, uint32 lsw)
{
    struct timeval t;
    t.tv_sec = msw - NTP_EPOCH_OFFSET;
    t.tv_usec = (uint32)((((double)lsw) * 1000000.0) / ((uint32)(~0)));
    return t;
}

uint32
timevalIntervalTo65536(timeval& t)
{
    const uint32 f = 65536;
    uint32 result = t.tv_sec * f;
    result += (t.tv_usec << 12) / 125000 * 2; // * (4096 / 125000) * 2
    return result;
}

timeval
microtimeout2Timeval(microtimeout_t to)
{
    timeval result;
    result.tv_sec = to % 1000000;
    result.tv_usec = to / 1000000;
    return result;
}

RTCPCompoundHandler::RTCPCompoundHandler(uint16 mtu) :
rtcpSendBuffer(new unsigned char[mtu]), rtcpRecvBuffer(new unsigned char[mtu]),
pathMTU(mtu)
{
}

RTCPCompoundHandler::~RTCPCompoundHandler()
{
#ifdef  CCXX_EXCEPTIONS
    try {
#endif
        delete [] rtcpRecvBuffer;
#ifdef  CCXX_EXCEPTIONS
    } catch (...) {}
    try {
#endif
        delete [] rtcpSendBuffer;
#ifdef  CCXX_EXCEPTIONS
    } catch (...) {}
#endif
}

bool
RTCPCompoundHandler::checkCompoundRTCPHeader(size_t len)
{
    // Note that the first packet in the compount --in order to
    // detect possibly misaddressed RTP packets-- is more
    // thoroughly checked than the following. This mask checks the
    // first packet's version, padding (must be zero) and type
    // (must be SR or RR).
    if ( (*(reinterpret_cast<uint16*>(rtcpRecvBuffer))
          & RTCP_VALID_MASK)
         != RTCP_VALID_VALUE ) {
        return false;
    }

    // this checks that every packet in the compound is tagged
    // with version == CCRTP_VERSION, and the length of the compound
    // packet matches the addition of the packets lenghts
    uint32 pointer = 0;
    RTCPPacket* pkt;
    do {
        pkt = reinterpret_cast<RTCPPacket*>
            (rtcpRecvBuffer + pointer);
        pointer += (ntohs(pkt->fh.length)+1) << 2;
    } while ( (pointer < len) && (CCRTP_VERSION == pkt->fh.version) );

    if ( pointer != len )
        return false;

    return true;
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
