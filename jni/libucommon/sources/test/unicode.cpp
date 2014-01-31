// Copyright (C) 2009-2010 David Sugar, Tycho Softworks.
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
    char u1[] = {(char)0xc2, (char)0xa9, 0x00};
    char u2[] = {(char)0xe2, (char)0x89, (char)0xa0, 0x00};

    assert(utf8::size(u1) == 2);
    assert(utf8::size(u2) == 3);
    assert(utf8::count(u1) == 1);
    assert(utf8::count(u2) == 1);
    assert(utf8::codepoint(u1) == 0x00a9);
    assert(utf8::codepoint(u2) == 0x2260);

	return 0;
}
