// Copyright (C) 2004-2008 TintaDigital - STI, LDA.
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
// As a special exception to the GNU General Public License, permission is
// granted for additional uses of the text contained in its release
// of Common C++.
//
// The exception is that, if you link the Common C++ library with other files
// to produce an executable, this does not by itself cause the
// resulting executable to be covered by the GNU General Public License.
// Your use of that executable is in no way restricted on account of
// linking the Common C++ library code into it.
//
// This exception does not however invalidate any other reasons why
// the executable file might be covered by the GNU General Public License.
//
// This exception applies only to the code released under the
// name Common C++.  If you copy code from other releases into a copy of
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.

/**
 * @file nat.h
 * @short Network Address Translation interface functions.
 * @author Ricardo Gameiro <rgameiro at tintadigital dot com>
 **/

#ifndef CCXX_NAT_H
#define CCXX_NAT_H

#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

#ifndef	WIN32
typedef	int SOCKET;
#endif

enum natResult {
  natOK = 0,
  natSearchErr = 1,
  natNotSupported = 2,
  natDevUnavail = 3,
  natSocknameErr = 4,
  natPeernameErr = 5,
  natSockTypeErr = 6,
  natIFaceErr = 7,
  natUnkownErr = 8
};

typedef natResult natResult;

/**
 * Perform NAT address table lookup for the current socket.
 *
 * @param sfd socket to get nat info from.
 * @param nat sockaddr structure to hold results.
 */
natResult natv4Lookup( SOCKET sfd, struct sockaddr_in * nat );
#ifdef CCXX_IPV6
natResult natv6Lookup( SOCKET sfd, struct sockaddr_in6 * nat );
#endif

/**
 * Return descriptive text for the NAT Lookup return error.
 *
 * @param res result from NATLookup function.
 * @return error description.
 */
const char * natErrorString( natResult res );

#ifdef	CCXX_NAMESPACES
}
#endif

#endif

