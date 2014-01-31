// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon.
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

#include <iostream>
#include <cstdio>

using namespace UCOMMON_NAMESPACE;

static string testing("second test");

int main()
{
    char buff[33];
    string::fill(buff, 32, ' ');
    stringbuf<128> mystr;
    mystr = (string)"hello" + (string)" this is a test";
    std::cout << "STARTING " << *mystr << std::endl;
    std::cout << "SECOND " << *testing << std::endl;
    std::cout << "AN OFFET " << mystr(-10) << std::endl;
};
