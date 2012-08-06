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
#include <getopt.h>
#include <sys/wait.h>
#include <iostream>
#include <fstream>


using namespace std;

#ifdef	CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

bool multicast = false;
bool daemon = false;
bool drop = false;
bool answer = false;

static int initial(int argc, char **argv)
{
	static bool usage = false;

	static struct option long_options[] = {
		{"hangup", 0, 0, 'd'},
		{"drop", 0, 0, 'd'},
		{"background", 0, 0, 'D'},
                {"foreground", 0, 0, 'F'},
                {"daemon", 0, 0, 'D'},
		{"multicast", 0, 0, 'm'},
		{"unicast", 0, 0, 'u'},
                {"help", 0, 0, 'h'},
                {"priority", 1, 0, 'p'},
		{0, 0, 0, 0}};
		
	int idx, opt;	
	char *cp = strchr(argv[0], '/');
	if(cp)
		++cp;
	else
		cp = argv[0];

	if(*cp == 'm')
		multicast = true;
		
	while(EOF != (opt = getopt_long(argc, argv, "mudp:FDh", long_options, &idx)))
	{
		switch(opt)
		{
		case 'm':
			multicast = true;
			break;
		case 'u':
			multicast = false;
			break;
		case 'p':
			keythreads.setValue("priority", optarg);
			break;	
		case 'F':
			daemon = false;
			break;
		case 'D':
			daemon = true;
			break;
		case 'd':
			drop = true;
			break;
		default:
			usage = true;
		}	
	}
	if(usage)
	{
		std::cerr << "use: phone [options] [parties...]" << std::endl;
		exit(-1);
	}
	return optind;
}
		
static int getPid() 
{
	int pid, fd;
	char buf[20];
	
	fd = ::open(".phonepid", O_RDONLY);
	if(fd < 0)
		return 0;
		
	::read(fd, buf, 16);
	buf[10] = 0;
	::close(fd);
	pid = atol(buf);
	if(kill(pid, 0))
		return 0;
	return pid;
}	

#ifdef	CCXX_NAMESPACES
extern "C" {
#endif

int main(int argc, char **argv)
{
	int pid = 0, wpid = 0;
	int idx;
	std::ofstream fifo;
	
	chdir(getenv("HOME"));
	if(canAccess(".phonepid"))
		if(canModify(".phonectrl"))
			pid = getPid();		

	idx = initial(argc, argv);

	if(!pid)
	{
		::remove(".phonectrl");
		::mkfifo(".phonectrl", 0660);
		pid = ::fork();
		if(!pid)
		{
			server();
			exit(0);
		}
		if(daemon)
			::waitpid(pid, NULL, 0);
		else
			wpid = pid;
	}
	fifo.open(".phonectrl", std::ios::out);
	if(!fifo.is_open())
	{
		std::cerr << "phone: cannot get control interface" << std::endl;
		exit(-1);
	}
	if(idx == argc && drop)
		fifo << "DROP *" << std::endl;

	while(idx < argc)
	{
		if(drop)
			fifo << "DROP " << argv[idx++] << std::endl;
		else
			fifo << "JOIN " << argv[idx++] << std::endl;
	}
		
	fifo.close();

	if(wpid > 0)
		::waitpid(wpid, NULL, 0);
		
	exit(0);
}

#ifdef	CCXX_NAMESPACES
} }
#endif
