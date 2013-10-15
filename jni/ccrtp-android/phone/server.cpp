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
#include <cc++/process.h>
#include "server.h"
#include <iostream>
#include <fstream>

using namespace std;

#ifdef	CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

void server(void)
{	
	const char *reason = "exiting";
	char *cp, *ep;
	std::fstream fifo;
	new RTPAudio;

	::signal(SIGPIPE, SIG_IGN);

	int fd;
	char buf[256];

	::remove(".phonepid");

	if(daemon)
	{
		close(0);
		close(1);
		close(2);
		Process::detach();
		open("/dev/null", O_RDWR);
		open("/dev/null", O_RDWR);
		open("/dev/null", O_RDWR);
		slog.open("phone", Slog::classDaemon);
		slog.level(Slog::levelNotice);
		slog(Slog::levelNotice) << "daemon mode started" << std::endl;
	}
	else
	{
		slog.open("phone", Slog::classDaemon);
		slog.level(Slog::levelDebug);
		slog(Slog::levelNotice) << "server starting..." << std::endl;
	}
	snprintf(buf, 11, "%d", getpid());
	fd = ::creat(".phonepid", 0660);
	if(fd > -1)
	{
		::write(fd, buf, 10);
		::close(fd);
	}
	fifo.open(".phonectrl", std::ios::in | std::ios::out);
	if(!fifo.is_open())
	{
		slog(Slog::levelError) << "fifo failed; exiting" << std::endl;
		exit(-1);
	}

	rtp->startRunning();	// we assume it's always running
	while(!fifo.eof())
	{
		fifo.getline(buf, 256);
		cp = buf;
		while(isspace(*cp))
			++cp;
		ep = strrchr(cp, '\n');
		if(ep)
			*ep = 0;
		if(!*cp)
			continue;	
		slog(Slog::levelDebug) << "fifo: " << cp << std::endl;
		if(!strnicmp(cp, "exit", 4))
			break;

	}
	rtp->exit(reason);
	fifo.close();
	slog(Slog::levelWarning) << "server exiting..." << std::endl;
	exit(0);
}

#ifdef	CCXX_NAMESPACES
}
#endif
