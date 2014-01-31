// Copyright (C) 2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

#include <ucommon/ucommon.h>

using namespace UCOMMON_NAMESPACE;

static shell::flagopt helpflag('h',"--help",    _TEXT("display this list"));
static shell::flagopt althelp('?', NULL, NULL);
static shell::flagopt udp('u', "--udp", _TEXT("lookup udp service"));

PROGRAM_MAIN(argc, argv)
{
    unsigned type = SOCK_STREAM;
    unsigned port;
    char buffer[256];

    shell::bind("sockaddr");
    shell args(argc, argv);

    if(is(helpflag) || is(althelp)) {
        printf("%s\n", _TEXT("Usage: sockaddr [options] arguments..."));
        printf("%s\n\n", _TEXT("Echo command line arguments"));
        printf("%s\n", _TEXT("Options:"));
        shell::help();
        printf("\n%s\n", _TEXT("Report bugs to dyfet@gnu.org"));
        PROGRAM_EXIT(0);
    }

    const char *host = args[0];
    const char *service = args[1];

    if(!host || !service)
        shell::errexit(1, "use: sockaddr [options] host service\n");

    if(udp)
        type = SOCK_DGRAM;

    Socket::address addr(host, service, type);
    linked_pointer<struct sockaddr> ap = addr;

    if(!is(ap))
        shell::errexit(2, "*** sockaddr: %s/%s: %s\n",
            host, service, _TEXT("could not find"));

    while(is(ap)) {
        port = 0;
        switch(ap->sa_family) {
        case AF_INET:
            port = ntohs(ap.in()->sin_port);
            break;
#ifdef  AF_INET6
        case AF_INET6:
            port = ntohs(ap.in6()->sin6_port);
            break;
#endif
        }

        if(port) {
            Socket::query(*ap, buffer, sizeof(buffer));
            printf("%s/%u\n", buffer, port);
        }

        ap.next();
    }

    PROGRAM_EXIT(0);
}

