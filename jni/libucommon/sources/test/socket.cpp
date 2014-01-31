// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
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
// along with GNU ucommon.  If not, see <http://www.gnu.org/licenses/>.

#ifndef DEBUG
#define DEBUG
#endif

#include <ucommon/ucommon.h>

#include <stdio.h>

using namespace UCOMMON_NAMESPACE;

static Socket::address localhost("127.0.0.1", 4444);
#ifdef  AF_INET6
static Socket::address testing6("44:022:66::1", 4444);
static Socket::address localhost6("::1", 4444);
#endif

extern "C" int main()
{
    struct sockaddr_internet addr;

    char addrbuf[128];
    addrbuf[0] = 0;
    assert(localhost.get(AF_INET) != NULL);
    Socket::query(localhost.get(AF_INET), addrbuf, sizeof(addrbuf));
    assert(0 == strcmp(addrbuf, "127.0.0.1"));
    Socket::via((struct sockaddr *)&addr, localhost.get(AF_INET));
    Socket::query((struct sockaddr *)&addr, addrbuf, sizeof(addrbuf));
    assert(0 == strcmp(addrbuf, "127.0.0.1"));
    assert(eq((struct sockaddr *)&addr, localhost.get(AF_INET)));
    assert(eq_subnet((struct sockaddr *)&addr, localhost.get(AF_INET)));
#ifdef  AF_INET6
    // we can only test if interface/ipv6 support is actually running
    // so we use getinterface to find out first.  it will return -1 for
    // localhost interface if ipv6 is down...
    assert(localhost6.get(AF_INET6) != NULL);
    if(!Socket::via((struct sockaddr *)&addr, localhost6.get(AF_INET6))) {
        Socket::query((struct sockaddr *)&addr, addrbuf, sizeof(addrbuf));
        assert(0 == strcmp(addrbuf, "::1"));
        assert(Socket::equal((struct sockaddr *)&addr, localhost6.get(AF_INET6)));
        Socket::query(testing6.get(AF_INET6), addrbuf, sizeof(addrbuf));
        assert(0 == strcmp(addrbuf, "44:22:66::1"));
    }
#endif
    return 0;
}
