// Common header for audiorx/audiotx.
// A simple and amusing application for testing basic features of ccRTP.
// Copyright (C) 2001,2002  Federico Montesino <fedemp@altern.org>
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
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

/** file audio.h
 * Common header for audiorx and audiotx.
 */

// audiorx and audiotx are, respectively, the transmitter and receiver of
// a simple application for testing ccRTP with audio. It aims to
// transmit/receive \mu-law encoded audio on RTP packets.


// UDP/RTP ports
const int RECEIVER_BASE = 22222;
const int TRANSMITTER_BASE = 33334;

// We will sample at 8 Khz using \mu-law encoding
const int SAMPLING_RATE = 8000;

// Transmission interval between consecutive packets.
// Ideally, the interval should be 20 milliseconds
const int PERIOD = 20;

// Packets are 8000*20/1000 = 160 octects long.
const int PACKET_SIZE=SAMPLING_RATE*PERIOD/1000;
