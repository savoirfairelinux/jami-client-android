// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// Common C++.  If you copy code from other releases into a copy of GNU
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

#include <cc++/config.h>
#include <cc++/export.h>
#include <cc++/thread.h>
#include <cc++/process.h>
#include <cc++/strchar.h>

#include <sys/stat.h>
#include <cstdlib>
#include <cstdio>
#include <cerrno>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifdef  WIN32

Lockfile::Lockfile()
{
    _mutex = INVALID_HANDLE_VALUE;
    _flagged = false;
}

Lockfile::Lockfile(const char *name)
{
    _mutex = INVALID_HANDLE_VALUE;
    _flagged = false;
    lock(name);
}

bool Lockfile::lock(const char *name)
{
    char mname[65];
    char *ext = strrchr((char *)name, '/');

    if(ext)
        name = ++ext;

    unlock();
    snprintf(mname, sizeof(mname) - 4, "_lock_%s", name);
    ext = strrchr(mname, '.');
    if(ext && !stricmp(ext, ".lock")) {
        *ext = 0;
        ext = NULL;
    }
    if(!ext)
        addString(mname, sizeof(mname), ".lck");
    _mutex = CreateMutex(NULL, FALSE, mname);
    if(WaitForSingleObject(_mutex, 200) == WAIT_OBJECT_0)
        _flagged = true;
    return _flagged;
}

void Lockfile::unlock(void)
{
    if(_mutex == INVALID_HANDLE_VALUE)
        return;

    if(_flagged)
        ReleaseMutex(_mutex);
    CloseHandle(_mutex);
    _flagged = false;
    _mutex = INVALID_HANDLE_VALUE;
}

bool Lockfile::isLocked(void)
{
    return _flagged;
}

#else

Lockfile::Lockfile()
{
    _path = NULL;
}

Lockfile::Lockfile(const char *name)
{
    _path = NULL;
    lock(name);
}

bool Lockfile::lock(const char *name)
{
    struct stat ino;
    int fd, pid, status;
    const char *ext;
    char buffer[128];
    bool rtn = true;

    unlock();

    ext = strrchr(name, '/');
    if(ext)
        ext = strrchr(ext, '.');
    else
        ext = strrchr(name, '.');

    if(strchr(name, '/')) {
        _path = new char[strlen(name) + 1];
        strcpy(_path, name);
    }
    else if(ext && !stricmp(ext, ".pid")) {
        if(stat("/var/run", &ino))
            snprintf(buffer, sizeof(buffer), "/tmp/.%s", name);
        else
            snprintf(buffer, sizeof(buffer), "/var/run/%s", name);
        _path = new char[strlen(buffer) + 1];
        strcpy(_path, buffer);
    }
    else {
        if(!ext)
            ext = ".lock";
        if(stat("/var/lock", &ino))
            snprintf(buffer, sizeof(buffer), "/tmp/.%s%s", name, ext);
        else
            snprintf(buffer, sizeof(buffer), "/var/lock/%s%s", name, ext);

        _path = new char[strlen(buffer) + 1];
        strcpy(_path, buffer);
    }

    for(;;) {
        fd = ::open(_path, O_WRONLY | O_CREAT | O_EXCL, 0660);
        if(fd > 0) {
            pid = getpid();
            snprintf(buffer, sizeof(buffer), "%d\n", pid);
            if(::write(fd, buffer, strlen(buffer)))
                rtn = false;
            ::close(fd);
            return rtn;
        }
        if(fd < 0 && errno != EEXIST) {
            delete[] _path;
            return false;
        }

        fd = ::open(_path, O_RDONLY);
        if(fd < 0) {
            if(errno == ENOENT)
                continue;
            delete[] _path;
            return false;
        }

        Thread::sleep(2000);
        status = ::read(fd, buffer, sizeof(buffer) - 1);
        if(status < 1) {
            ::close(fd);
            continue;
        }

        buffer[status] = 0;
        pid = atoi(buffer);
        if(pid) {
            if(pid == getpid()) {
                status = -1;
                errno = 0;
            }
            else
                status = kill(pid, 0);

            if(!status || (errno == EPERM)) {
                ::close(fd);
                delete[] _path;
                return false;
            }
        }
        ::close(fd);
        ::unlink(_path);
    }
}

void Lockfile::unlock(void)
{
    if(_path) {
        remove(_path);
        delete[] _path;
        _path = NULL;
    }
}

bool Lockfile::isLocked(void)
{
    if(_path)
        return true;

    return false;
}

#endif

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
