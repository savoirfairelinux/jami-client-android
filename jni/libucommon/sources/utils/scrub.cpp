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
#include <sys/stat.h>

using namespace UCOMMON_NAMESPACE;

static shell::flagopt helpflag('h',"--help",    _TEXT("display this list"));
static shell::flagopt althelp('?', NULL, NULL);
static shell::numericopt blocks('b', "--blocksize", _TEXT("size of i/o blocks in k (1-x)"), "size k", 1);
static shell::flagopt follow('F', "--follow", _TEXT("follow symlinks"));
static shell::numericopt passes('p', "--passes", _TEXT("passes with randomized data (0-x)"), "count", 1);
static shell::flagopt renamefile('n', "--rename", _TEXT("rename file randomly"));
static shell::flagopt recursive('R', "--recursive", _TEXT("recursive directory scan"));
static shell::flagopt altrecursive('r', NULL, NULL);
static shell::flagopt truncflag('t', "--truncate", _TEXT("decompose file by truncation"));
static shell::flagopt verbose('v', "--verbose", _TEXT("show active status"));

static int exit_code = 0;
static const char *argv0 = "scrub";

static void report(const char *path, int code)
{
    const char *err = _TEXT("i/o error");

    switch(code) {
    case EACCES:
    case EPERM:
        err = _TEXT("permission denied");
        break;
    case EROFS:
        err = _TEXT("read-only file system");
        break;
    case ENODEV:
    case ENOENT:
        err = _TEXT("no such file or directory");
        break;
    case ENOTDIR:
        err = _TEXT("not a directory");
        break;
    case ENOTEMPTY:
        err = _TEXT("directory not empty");
        break;
    case ENOSPC:
        err = _TEXT("no space left on device");
        break;
    case EBADF:
    case ENAMETOOLONG:
        err = _TEXT("bad file path");
        break;
    case EBUSY:
    case EINPROGRESS:
        err = _TEXT("file or directory busy");
        break;
    case EINTR:
        err = _TEXT("operation interupted");
        break;
#ifdef  ELOOP
    case ELOOP:
        err = _TEXT("too many sym links");
        break;
#endif
    }

    if(!code) {
        if(is(verbose))
            shell::printf("%s\n", _TEXT(" removed"));
        return;
    }

    if(is(verbose))
        shell::printf(" - %s\n", err);
    else
        shell::errexit(1, "*** %s: %s: %s\n", argv0, path, err);

    exit_code = 1;
}

static void scrub(const char *path)
{
    fsys_t fs;
    fsys::fileinfo_t ino;
    unsigned char block[1024];
    unsigned long count;
    fsys::offset_t pos = 0l;
    unsigned dots = 0;
    unsigned pass = 0;

    if(is(verbose))
        shell::printf("%s", path);

    int err = fsys::info(path, &ino);

    if(err == ENOENT || fsys::is_link(path)) {
        report(path, fsys::unlink(path));
        return;
    }

    if(fsys::is_dir(&ino)) {
        report(path, dir::remove(path));
        return;
    }

    if(err == ENOENT || !ino.st_size || fsys::is_sys(&ino) || fsys::is_dev(&ino)) {
        report(path, fsys::erase(path));
        return;
    }

    count = (ino.st_size + 1023l) / 1024;
    count /= (fsys::offset_t)(*blocks);
    count *= (fsys::offset_t)(*blocks);

    fs.open(path, fsys::REWRITE);
    if(!is(fs)) {
        report(path, fs.err());
        return;
    }

    while(count--) {
        while(++dots > 16384) {
            dots = 0;
            if(is(verbose))
                shell::printf(".");
        }
        pass = 0;

repeat:
        fs.seek(pos);
        if(fs.err()) {
            report(path, fs.err());
            fs.close();
            return;
        }
        if(pass < (unsigned)(*passes)) {
            Random::fill(block, 1024);
            fs.write(block, 1024);
            if(fs.err()) {
                report(path, fs.err());
                fs.close();
                return;
            }
            if(++pass < (unsigned)(*passes))
                goto repeat;
        }

        // we followup with a zero fill always as it is friendly for many
        // virtual machine image formats which can later re-pack unused disk
        // space, and if no random passes are specified, we at least do this.

        if(pass)
            fs.seek(pos);

        memset(block, 0, sizeof(block));
        fs.write(block, 1024);
        if(fs.err()) {
            report(path, fs.err());
            fs.close();
            return;
        }

        pos += 1024l;
    }

    while(is(truncflag) && pos > 0l) {
        pos -= 1024l * (fsys::offset_t)*blocks;
        fs.trunc(pos);
        if(fs.err()) {
            report(path, fs.err());
            fs.close();
            return;
        }
    }

    report(path, dir::remove(path));

    fs.close();

}

static void scan(String path, bool top = true)
{
    char filename[128];
    String filepath;
    dir_t dir(path);

    while(is(dir) && dir.read(filename, sizeof(filename))) {
        if(*filename == '.' && (filename[1] == '.' || !filename[1]))
            continue;

        filepath = str(path) + str("/") + str(filename);
        if(fsys::is_dir(filepath)) {
            if(is(follow) || is(recursive) || is(altrecursive)) {
                if(fsys::is_link(filepath) && !is(follow))
                    scrub(filepath);
                else
                    scan(filepath, false);
            }
            else
                scrub(filepath);
        }
        else
            scrub(filepath);
    }
    scrub(path);
}

PROGRAM_MAIN(argc, argv)
{
    shell::bind("scrub");
    shell args(argc, argv);
    argv0 = args.argv0();
    unsigned count = 0;

    if(*blocks < 1)
        shell::errexit(2, "*** %s: blocksize: %ld: %s\n",
            argv0, *blocks, _TEXT("must be greater than zero"));

    if(*passes < 0)
        shell::errexit(2, "*** %s: passes: %ld: %s\n",
            argv0, *passes, _TEXT("negative passes invalid"));

    argv0 = args.argv0();

    if(is(helpflag) || is(althelp)) {
        printf("%s\n", _TEXT("Usage: scrub [options] path..."));
        printf("%s\n\n", _TEXT("Securely erase files"));
        printf("%s\n", _TEXT("Options:"));
        shell::help();
        printf("\n%s\n", _TEXT("Report bugs to dyfet@gnu.org"));
        PROGRAM_EXIT(0);
    }

    if(!args())
        PROGRAM_EXIT(0);

    secure::init();

    while(count < args()) {
        if(fsys::is_dir(args[count]))
            scan(str(args[count++]));
        else
            scrub(args[count++]);
    }

    PROGRAM_EXIT(exit_code);
}

