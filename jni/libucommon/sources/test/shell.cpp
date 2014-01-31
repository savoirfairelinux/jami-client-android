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
    int test_argc;
    char *test_argv[6];

    shell::bind("test");

    test_argc = 5;
    test_argv[0] = (char *)"test";

    test_argv[1] = (char *)"--lines=5";
    test_argv[2] = (char *)"-r";
    test_argv[3] = (char *)"a";
    test_argv[4] = (char *)"b";
    test_argv[5] = NULL;

    shell::flagopt rflag('r', "--reverse", "reverse order of arguments");
    shell::flagopt tflag('t', "--testing", "never hit this flag");
    shell::numericopt lines('l', "--lines", "number of lines in output");
    shell args(test_argc, test_argv);

    assert(!tflag);
    assert(is(rflag));
    assert(*lines == 5);
    assert(args() == 2);            // two file arguments left
    assert(eq(args[0], "a"));       // first file argument is "a"

    string_t prefix, subdir, basedir;

    prefix = shell::path(shell::SYSTEM_PREFIX);
    subdir = shell::path(shell::SYSTEM_PREFIX, "test");
    basedir = shell::path(shell::SYSTEM_PREFIX, "/test");

    prefix = prefix + "/test";

    assert(eq(basedir, "/test"));
    assert(eq(subdir, prefix));
}
