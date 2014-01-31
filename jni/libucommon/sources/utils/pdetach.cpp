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
static shell::stringopt error('e', "--error", _TEXT("stderr path to use"), "filename");
static shell::stringopt input('i', "--input", _TEXT("stdin path to use"), "filename");
static shell::stringopt output('o', "--output", _TEXT("stdout path to use"), "filename");

PROGRAM_MAIN(argc, argv)
{
    fd_t stdio[3] = {INVALID_HANDLE_VALUE, INVALID_HANDLE_VALUE, INVALID_HANDLE_VALUE};
    const char *argv0;

    shell::bind("pdetach");
    shell args(argc, argv);

    if(is(helpflag) || is(althelp)) {
        printf("%s\n", _TEXT("Usage: pdetach [stdio options] command [arguments...]"));
        printf("%s\n\n", _TEXT("Create detached process"));
        printf("%s\n", _TEXT("Options:"));
        shell::help();
        printf("\n%s\n", _TEXT("Report bugs to dyfet@gnu.org"));
        PROGRAM_EXIT(0);
    }

    if(!args())
        shell::errexit(10, "*** pdetach %s", _TEXT("no command specified"));

    argv = args.argv();
    argv0 = *argv;

    if(is(input)) {
        stdio[0] = fsys::input(*input);
        if(stdio[0] == INVALID_HANDLE_VALUE)
            shell::errexit(1, "*** pdetach: %s: %s",
                _TEXT("cannot access"), *input);
    }

    if(is(output)) {
        stdio[1] = fsys::output(*output);
        if(stdio[1] == INVALID_HANDLE_VALUE)
            shell::errexit(2, "*** pdetach: %s: %s",
                _TEXT("cannot access"), *output);
    }

    if(is(error)) {
        stdio[2] = fsys::output(*error);
        if(stdio[2] == INVALID_HANDLE_VALUE)
            shell::errexit(3, "*** pdetach: %s: %s",
                _TEXT("cannot access"), *error);
    }

    if(shell::detach(argv0, argv, NULL, stdio))
        shell::errexit(-1, "*** pdetach: %s: %s",
            argv0, _TEXT("failed to execute"));

    PROGRAM_EXIT(0);
}

