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

#include <cc++/slog.h>
#include "server.h"

using namespace std;

#ifdef	CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

RTPEvent *RTPEvent::first = NULL;

RTPEvent::RTPEvent()
{
	next = first;
	first = this;
}

RTPAudio::RTPAudio() :
RTPSocket(keyrtp.getInterface(), keyrtp.getPort(), keythreads.priRTP())
{
	rtp = this;
	setSchedulingTimeout(keyrtp.getTimer());
	setExpireTimeout(keyrtp.getExpire());
	groups = 0;
	unicast = false;
	shutdown = false;
}

void RTPAudio::exit(const char *reason)
{
	shutdown = true;
	dispatchBYE(reason);
	sleep(500);
	delete rtp;
	rtp = NULL;
}

void RTPAudio::onGotHello(const SyncSource &src)
{
	RTPEvent *event = RTPEvent::first;

	slog(Slog::levelDebug) << "hello(" << src.getID() << ") ";
        Participant* p = src.getParticipant();
	slog() << p->getSDESItem(SDESItemTypeCNAME) << std::endl;

	while(event)
	{
		event->gotHello(src);
		event = event->next;
	}
}

void RTPAudio::onGotGoodbye(const SyncSource &src, const string& reason)
{
	RTPEvent *event = RTPEvent::first;

	slog(Slog::levelDebug) << "bye(" << src.getID() << ") ";
	Participant* p = src.getParticipant();
	slog() << p->getSDESItem(SDESItemTypeCNAME) << "; " << reason;
	slog() << std::endl;

	while(event)
	{
		event->gotGoodbye(src, reason);
		event = event->next;
	}
}
	
RTPAudio *rtp;

#ifdef	CCXX_NAMESPACES
}
#endif
