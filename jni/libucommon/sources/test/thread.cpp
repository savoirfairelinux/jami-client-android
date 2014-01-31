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
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

#ifndef DEBUG
#define DEBUG
#endif

#include <ucommon/ucommon.h>

#include <stdio.h>

using namespace UCOMMON_NAMESPACE;

static unsigned count = 0;

class testThread : public JoinableThread
{
public:
    testThread() : JoinableThread() {};

    void run(void) {
        ++count;
        ::sleep(2);
    };
};

extern "C" int main()
{
    time_t now, later;
    testThread *thr;

    time(&now);
    thr = new testThread();
    start(thr);
    Thread::sleep(10);
    delete thr;
    assert(count == 1);
    time(&later);
    assert(later >= now + 1);

    time(&now);
    TimedEvent evt;
    evt.wait(2000);
    time(&later);
    assert(later >= now + 1);
    return 0;
}

