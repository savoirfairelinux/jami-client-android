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
static shell::stringopt hash('d', "--digest", _TEXT("digest method (md5)"), "method", "md5");
static shell::flagopt recursive('R', "--recursive", _TEXT("recursive directory scan"));
static shell::flagopt altrecursive('r', NULL, NULL);
static shell::flagopt hidden('s', "--hidden", _TEXT("show hidden files"));

static int exit_code = 0;
static const char *argv0 = "md";
static digest_t md;

static void result(const char *path, int code)
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
    case EISDIR:
        err = _TEXT("is a directory");
        break;
#ifdef  ELOOP
    case ELOOP:
        err = _TEXT("too many sym links");
        break;
#endif
    }

    if(!code) {
        if(!path)
            path="-";
        shell::printf("%s %s\n", *md, path);
        return;
    }

    if(path)
        shell::printf("%s: %s: %s\n", argv0, path, err);
    else
        shell::errexit(1, "*** %s: %s\n", argv0, err);

    exit_code = 1;
}

static void digest(const char *path = NULL)
{
    fsys_t fs;
    fsys::fileinfo_t ino;
    unsigned char buffer[1024];

    if(path) {
        int err = fsys::info(path, &ino);

        if(err) {
            result(path, err);
            return;
        }

        if(fsys::is_sys(&ino)) {
            result(path, EBADF);
            return;
        }

        fs.open(path, fsys::STREAM);
    }
    else
        fs.assign(shell::input());

    if(!is(fs)) {
        result(path, fs.err());
        return;
    }

    for(;;) {
        ssize_t size = fs.read(buffer, sizeof(buffer));
        if(size < 1)
            break;
        md.put(buffer, size);
    }

    fs.close();
    result(path, fs.err());
    md.reset();
}

static void scan(String path, bool top = true)
{
    char filename[128];
    string_t filepath;
    dir_t dir(path);

    while(is(dir) && dir.read(filename, sizeof(filename))) {
        if(*filename == '.' && (filename[1] == '.' || !filename[1]))
            continue;

        if(*filename == '.' && !is(hidden))
            continue;

        filepath = str(path) + str("/") + str(filename);
        if(fsys::is_dir(filepath)) {
            if(is(recursive) || is(altrecursive))
                scan(filepath, false);
            else
                result(filepath, EISDIR);
        }
        else
            digest(filepath);
    }
}

PROGRAM_MAIN(argc, argv)
{
    shell::bind("mdsum");
    shell args(argc, argv);
    argv0 = args.argv0();
    unsigned count = 0;

    argv0 = args.argv0();

    if(is(helpflag) || is(althelp)) {
        printf("%s\n", _TEXT("Usage: mdsum [options] path..."));
        printf("%s\n\n", _TEXT("Compute digests for files"));
        printf("%s\n", _TEXT("Options:"));
        shell::help();
        printf("\n%s\n", _TEXT("Report bugs to dyfet@gnu.org"));
        PROGRAM_EXIT(0);
    }

    secure::init();
    if(!Digest::has(*hash))
        shell::errexit(2, "*** %s: %s: %s\n",
            argv0, *hash, _TEXT("unkown or unsupported digest method"));

    md = *hash;

    // we can symlink md as md5, etc, to set alternate default digest names
    if(!is(hash) && Digest::has(argv0))
        md = argv0;

    if(!args())
        digest();
    else while(count < args()) {
        if(fsys::is_dir(args[count]))
            scan(str(args[count++]));
        else
            digest(args[count++]);
    }

    PROGRAM_EXIT(exit_code);
}

