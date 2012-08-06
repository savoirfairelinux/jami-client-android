// Copyright (C) 2001-2005 Federico Montesino Pouzols <fedemp@altern.org>.
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

#ifndef	CCXX_RTP_BASE_H_
#define CCXX_RTP_BASE_H_

#ifndef	CCXX_SOCKET_H_
#include <cc++/config.h>
#include <cc++/socket.h>
#endif

#ifndef	CCXX_PACKING
#if defined(__GNUC__)
#define CCXX_PACKED
#elif !defined(__hpux) && !defined(_AIX)
#define CCXX_PACKED        
#endif 
#endif

#ifdef CCXX_NAMESPACES
namespace ost {
#endif

/** 
 * @file base.h 
 *
 * @short Base elements for RTP stacks: constants, types and global
 * functions.
 **/

/// RTP protocol version supported.
const uint8 CCRTP_VERSION = 2;

/// Time interval expressed in microseconds.
typedef uint32 microtimeout_t;

/// Time interval expressed in nanoseconds.
typedef uint32 nanotimeout_t;

/**
 * Convert a time interval, expressed as a microtimeout_t (number of
 * microseconds), into a timeval value.
 *
 * @param to time interval, in microseconds.
 * @return the same time interval, as a timeval value.
 **/
__EXPORT timeval 
microtimeout2Timeval(microtimeout_t to);

/**
 * Convert a time interval, expressed as a timeval value into a
 * microseconds counter.
 *
 * @param t time, as a timeval.
 * @return the same time, as a microseconds counter.
 **/
inline microtimeout_t
timeval2microtimeout(const timeval& t)
{ return ((t.tv_sec * 1000000ul) + t.tv_usec); }

/**
 * Convert a time interval, expressed as the difference between two
 * timeval values (t1-t2), into a microseconds counter.
 *
 * @param t1 First timeval.
 * @param t2 Second timeval.
 * @return difference between t1 and t2, in microseconds.
 **/
inline microtimeout_t
timevalDiff2microtimeout(const timeval& t1, const timeval& t2)
{
	return ((t1.tv_sec - t2.tv_sec) * 1000000ul) + 
		(t1.tv_usec - t2.tv_usec);
}

/// registered default RTP data transport port
const tpport_t DefaultRTPDataPort = 5004;

/// registered default RTCP transport port
const tpport_t DefaultRTCPPort = 5005;

#ifndef	HAVE_GETTIMEOFDAY
#ifdef	WIN32
__EXPORT int
gettimeofday(struct timeval *tv_, void *tz_);
#endif
#endif

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  // ndef CCXX_RTP_BASE_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
