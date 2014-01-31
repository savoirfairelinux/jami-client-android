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

extern "C" int main()
{
    keydata *keys;
    keyfile myfile("keydata.conf");

    keys = myfile.get();
    assert(eq_case(keys->get("key2"), "value2"));

    keys = myfile["section1"];
    assert(keys != NULL);
    assert(eq_case(keys->get("key1"), "this is value 1 quoted"));

    keys = myfile["section2"];
    assert(keys != NULL);
    assert(eq_case(keys->get("key1"), "replaced value"));
    return 0;
}
