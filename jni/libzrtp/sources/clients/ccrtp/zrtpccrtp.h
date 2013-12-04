/*
  Copyright (C) 2006-2007 Werner Dittmann

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#ifndef _ZRTPCCRTP_H_
#define _ZRTPCCRTP_H_

#include <ccrtp/rtp.h>
#include <ZrtpQueue.h>

NAMESPACE_COMMONCPP

// Define a dummy variable only to overcome a doxygen problem.
static int dummy __attribute__ ((unused)) = 0;


/**
 * @typedef SymmetricZRTPSession
 *
 * Uses one pair of sockets, (1) for RTP data and (2) for RTCP
 * transmission/reception.
 *
 * This session uses the ZrtpQueue instead of the AVPQueue. The ZrtpQueue
 * inherits from AVPQueue and adds support for ZRTP thus enabling
 * ad-hoc key negotiation to setup SRTP sessions.
 *
 * @short Symmetric UDP/IPv4 RTP session scheduled by one thread of execution.
 **/
typedef SingleThreadRTPSession<SymmetricRTPChannel,
                               SymmetricRTPChannel,
                               ZrtpQueue> SymmetricZRTPSession;


#ifdef CCXX_IPV6
/**
 * @typedef SymmetricZRTPSession
 *
 * Uses one pair of sockets, (1) for RTP data and (2) for RTCP
 * transmission/reception.
 *
 * This session uses the ZrtpQueue instead of the AVPQueue. The ZrtpQueue
 * inherits from AVPQueue and adds support for ZRTP thus enabling
 * ad-hoc key negotiation to setup SRTP sessions.
 *
 * @short Symmetric UDP/IPv6 RTP session scheduled by one thread of execution.
 **/
typedef SingleThreadRTPSessionIPV6<SymmetricRTPChannelIPV6,
                   SymmetricRTPChannelIPV6,
                   ZrtpQueue> SymmetricZRTPSessionIPV6;
#endif // CCXX_IPV6

END_NAMESPACE

#endif // _ZRTPCCRTP_H_


/** EMACS **
 * Local variables:
 * mode: c++
 * c-default-style: ellemtel
 * c-basic-offset: 4
 * End:
 */
