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

#include <ucommon/secure.h>

using namespace UCOMMON_NAMESPACE;

static shell::flagopt helpflag('h',"--help",    _TEXT("display this list"));
static shell::flagopt althelp('?', NULL, NULL);
static shell::numericopt passes('r', "--random", _TEXT("optional passes with randomized data (0-x)"), "count", 0);

static unsigned char buffer[65536];
static bool live = false;
static bool temp = false;

static void cleanup(void)
{
    if(temp)
        fsys::erase("zerofill.tmp");
    if(live)
        shell::printf("\n");
    live = false;
}

static void zerofill(void)
{
    fsys::offset_t pos = 0l;
    ssize_t size = 65536;
    unsigned long count = 0;
    fsys_t fs;

    temp = true;

    fs.open("zerofill.tmp", 0666, fsys::STREAM);
    if(!is(fs))
        shell::errexit(1, "*** zerofill: %s\n",
            _TEXT("cannot create temporary file"));

    live = true;
    while(size == 65536 && live) {
        unsigned pass = 0;

        while(pass < (unsigned)*passes) {
            fs.seek(pos);
            Random::fill(buffer, sizeof(buffer));
            fs.write(buffer, sizeof(buffer));
            ++pass;
        }

        fs.seek(pos);
        memset(buffer, 0, sizeof(buffer));
        size = fs.write(buffer, sizeof(buffer));
        pos += sizeof(buffer);
        if(++count >= 256) {
            shell::printf(".");
            count = 0;
        }
    }

    cleanup();

    if(fs.err() != ENOSPC)
        shell::errexit(3, "*** zerofill: %s\n",
            _TEXT("failed before end of space"));
}

static void zerofill(const char *devname)
{
    fsys::fileinfo_t ino;
    fsys::offset_t pos = 0l;
    ssize_t size = 65536;
    unsigned long count = 0;
    fsys_t fs;

    if(fsys::info(devname, &ino))
        shell::errexit(5, "*** zerofill: %s: %s\n",
            devname, _TEXT("cannot access"));

    if(!fsys::is_disk(&ino))
        shell::errexit(6, "*** zerofill: %s: %s\n",
            devname, _TEXT("not block device"));

    fs.open(devname, fsys::WRONLY);
    if(fs.err())
        shell::errexit(5, "*** zerofill: %s: %s\n",
            devname, _TEXT("cannot modify"));

    shell::printf("%s", devname);

    live = true;
    while(size == 65536 && live) {
        unsigned pass = 0;

        while(pass < (unsigned)*passes) {
            fs.seek(pos);
            Random::fill(buffer, sizeof(buffer));
            fs.write(buffer, sizeof(buffer));
            ++pass;
        }

        fs.seek(pos);
        memset(buffer, 0, sizeof(buffer));
        size = fs.write(buffer, sizeof(buffer));
        pos += sizeof(buffer);
        if(++count >= 256) {
            shell::printf(".");
            count = 0;
        }
    }

    cleanup();

    if(fs.err() != ENOSPC)
        shell::errexit(3, "*** zerofill: %s\n",
            _TEXT("failed before end of space"));
}

PROGRAM_MAIN(argc, argv)
{
    shell::bind("zerofill");
    shell args(argc, argv);
    unsigned count = 0;

    if(*passes < 0)
        shell::errexit(2, "*** zerofill: random: %ld: %s\n",
            *passes, _TEXT("negative random passes invalid"));

    if(is(helpflag) || is(althelp)) {
        printf("%s\n", _TEXT("Usage: zerofill [options] path..."));
        printf("%s\n\n", _TEXT("Erase and fill unused space with zeros"));
        printf("%s\n", _TEXT("Options:"));
        shell::help();
        printf("\n%s\n", _TEXT("Report bugs to dyfet@gnu.org"));
        PROGRAM_EXIT(0);
    }

    secure::init();

    shell::exiting(&cleanup);

    if(!args())
        zerofill();

    while(count < args() && live)
        zerofill(args[count++]);

    PROGRAM_EXIT(0);
}

