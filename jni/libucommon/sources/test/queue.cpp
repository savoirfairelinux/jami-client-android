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

#ifndef DEBUG
#define DEBUG
#endif

#include <ucommon/ucommon.h>

#include <stdio.h>

using namespace UCOMMON_NAMESPACE;

static unsigned reused = 0;

class myobject : public ReusableObject
{
public:
    unsigned count;

    myobject()
        {count = ++reused;};
};

static mempager pool;
static paged_reuse<myobject> myobjects(&pool, 100);
static queueof<myobject> mycache(&pool, 10);

extern "C" int main()
{
	unsigned i;

    myobject *x;
    for(i = 0; i < 10; ++i) {
        x = myobjects.create();
        mycache.post(x);
    }
    assert(x->count == 10);

    for(i = 0; i < 3; ++i) {
        x = mycache.lifo();
        assert(x != NULL);
    }
    assert(x->count == 8);

    init<myobject>(x);
    assert(x->count == 11);

    for(i = 0; i < 3; ++i) {
        x = mycache.lifo();
        assert(x != NULL);
        myobjects.release(x);
    }

    x = init<myobject>(NULL);
    assert(x == NULL);
    assert(reused == 11);
    return 0;
}

