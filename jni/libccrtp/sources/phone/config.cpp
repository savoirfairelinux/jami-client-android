// Copyright (C) 2000-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2008 David Sugar, Tycho Softworks.
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

#include "server.h"

#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

KeyThreads::KeyThreads() :
Keydata("/phone/threads")
{
	static Keydata::Define defkeys[] = {
	{"audio", "0"},
	{"priority", "0"},
	{"rtp", "0"},
	{"gui", "0"},
	{"policy", "other"},
	{"stack", "8"},
	{NULL, NULL}};
	
	load("~phone/threads");
	load(defkeys);

	const char *cp = getLast("pri");
	
	if(cp)
		setValue("priority", cp);
}

KeyRTP::KeyRTP() :
Keydata("/phone/rtp")
{
	static Keydata::Define defkeys[] = {
	{"interface", "*"},
	{"multicast", "*"},
	{"port", "3128"},
	{NULL, NULL}};

	load("~phone/rtp");
	load(defkeys);
}

KeyAudio::KeyAudio() :
Keydata("/phone/audio")
{
	static Keydata::Define defkeys[] = {
	{"interface", "oss"},
	{"device", "/dev/audio"},
	{"mike", "80"},
	{"speaker", "80"},
	{NULL, NULL}};
	
	load("~phone/audio");
	load(defkeys);
}

KeyThreads keythreads;
KeyAudio keyaudio;
KeyRTP keyrtp;

#ifdef	CCXX_NAMESPACES
}
#endif
