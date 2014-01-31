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

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/linked.h>
#include <ucommon/string.h>
#include <ucommon/timers.h>
#include <ucommon/mapped.h>
#include <ucommon/fsys.h>

#ifdef  HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <ctype.h>
#include <stdarg.h>
#include <errno.h>
#ifdef  HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifdef HAVE_SYS_MMAN_H
#undef  __EXTENSIONS__
#define __EXTENSIONS__
#ifndef _POSIX_C_SOURCE
#define _POSIX_C_SOURCE 200112L
#include <sys/mman.h>
#undef  _POSIX_C_SOURCE
#else
#include <sys/mman.h>
#endif
#include <sys/types.h>
#endif

#ifdef HAVE_FTOK
#include <sys/ipc.h>
#ifdef defined(HAVE_SYS_SHM_H) && !defined(ANDROID)
#include <sys/shm.h>
#endif
#endif

#include <ctype.h>
#include <stdlib.h>
#include <limits.h>

#if _POSIX_PRIORITY_SCHEDULING > 0
#include <sched.h>
#endif

#if defined(__APPLE__) && defined(__MACH__)
#define INSERT_OFFSET   16
#endif

#ifndef INSERT_OFFSET
#define INSERT_OFFSET   0
#endif

#if defined(__FreeBSD__)
#undef  HAVE_SHM_OPEN
#endif

#if defined(HAVE_FTOK) && !defined(HAVE_SHM_OPEN)

static void ftok_name(const char *name, char *buf, size_t max)
{
    assert(name != NULL && *name != 0);
    assert(buf != NULL);
    assert(max > 0);

    struct stat ino;
    if(*name == '/')
        ++name;

    if(!stat("/var/run/ipc", &ino) && S_ISDIR(ino.st_mode))
        snprintf(buf, max, "/var/run/ipc/%s", name);
    else
        snprintf(buf, max, "/tmp/.%s.ipc", name);
}

static key_t createipc(const char *name, char mode)
{
    assert(name != NULL && *name != 0);

    char buf[65];
    int fd;

    ftok_name(name, buf, sizeof(buf));
    fd = ::open(buf, O_CREAT | O_EXCL | O_WRONLY, 0664);
    if(fd > -1)
        ::close(fd);
    return ftok(buf, mode);
}

static key_t accessipc(const char *name, char mode)
{
    assert(name != NULL && *name != 0);

    char buf[65];

    ftok_name(name, buf, sizeof(buf));
    return ftok(buf, mode);
}

#endif

static  bool use_mapping = true;

using namespace UCOMMON_NAMESPACE;

void MappedMemory::disable(void)
{
    use_mapping = false;
}

MappedMemory::MappedMemory(const char *fn, size_t len)
{
    assert(fn != NULL && *fn != 0);
    assert(len > 0);

    erase = true;
    String::set(idname, sizeof(idname), fn);
    create(fn, size);
}

MappedMemory::MappedMemory(const char *fn)
{
    erase = false;
    assert(fn != NULL && *fn != 0);
    create(fn, 0);
}

MappedMemory::MappedMemory()
{
    erase = false;
    size = 0;
    used = 0;
    map = NULL;
}

#if defined(_MSWINDOWS_)

void MappedMemory::create(const char *fn, size_t len)
{
    assert(fn != NULL && *fn != 0);

    int share = FILE_SHARE_READ;
//  int prot = FILE_MAP_READ;
    int mode = GENERIC_READ;

    size = 0;
    used = 0;
    map = NULL;

    if(!use_mapping) {
        assert(len > 0);    // cannot use dummy for r/o...
        map = (caddr_t)malloc(len);
        if(!map)
            fault();
        size = len;
        return;
    }

    if(*fn == '/')
        ++fn;

    if(len) {
//      prot = FILE_MAP_WRITE;
        mode |= GENERIC_WRITE;
        share |= FILE_SHARE_WRITE;
        fd = CreateFileMapping(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE, 0, len, fn);
    }
    else
        fd = OpenFileMapping(FILE_MAP_ALL_ACCESS, FALSE, fn);

    if(fd == INVALID_HANDLE_VALUE || fd == NULL)
        return;

    map = (caddr_t)MapViewOfFile(fd, FILE_MAP_ALL_ACCESS, 0, 0, len);
    if(map) {
        size = len;
        VirtualLock(map, size);
    }
    else
        fault();
}

MappedMemory::~MappedMemory()
{
    release();
}

void MappedMemory::remove(const char *id)
{
    assert(id != NULL && *id != 0);
}

void MappedMemory::release(void)
{
    if(map) {
        if(use_mapping) {
            VirtualUnlock(map, size);
            UnmapViewOfFile(fd);
            CloseHandle(fd);
            map = NULL;
            fd = INVALID_HANDLE_VALUE;
        }
        else {
            free(map);
            map = NULL;
        }
    }
    if(erase) {
        remove(idname);
        erase = false;
    }
}

#elif defined(HAVE_SHM_OPEN)

void MappedMemory::create(const char *fn, size_t len)
{
    assert(fn != NULL && *fn != 0);

    int prot = PROT_READ;
    struct stat ino;
    char fbuf[80];

    size = 0;
    used = 0;

    if(!use_mapping) {
        assert(len > 0);
        map = (caddr_t)malloc(size);
        if(!map)
            fault();
        size = mapsize = len;
        return;
    }

    if(*fn != '/') {
        snprintf(fbuf, sizeof(fbuf), "/%s", fn);
        fn = fbuf;
    }

    if(len) {
        len += INSERT_OFFSET;
        prot |= PROT_WRITE;
        fd = shm_open(fn, O_RDWR | O_CREAT, 0664);
        if(fd > -1) {
            if(ftruncate(fd, len)) {
                ::close(fd);
                fd = -1;
            }
        }
    }
    else {
        fd = shm_open(fn, O_RDONLY, 0664);
        if(fd > -1) {
            fstat(fd, &ino);
            len = ino.st_size;
        }
    }

    if(fd < 0)
        return;


    map = (caddr_t)mmap(NULL, len, prot, MAP_SHARED, fd, 0);
    if(!map)
        fault();
    ::close(fd);
    if(map != (caddr_t)MAP_FAILED) {
        size = mapsize = len;
        mlock(map, mapsize);
#if INSERT_OFFSET > 0
        if(prot & PROT_WRITE) {
            size -= INSERT_OFFSET;
            snprintf(map, INSERT_OFFSET, "%ld", size);
        }
        else
            size = atol(map);
        map += INSERT_OFFSET;
#endif
    }
}

MappedMemory::~MappedMemory()
{
    release();
}

void MappedMemory::release()
{
    if(size) {
        if(use_mapping) {
            map -= INSERT_OFFSET;
            munlock(map, mapsize);
            munmap(map, mapsize);
        }
        else
            free(map);
        size = 0;
    }
    if(erase) {
        remove(idname);
        erase = false;
    }
}

void MappedMemory::remove(const char *fn)
{
    assert(fn != NULL && *fn != 0);

    char fbuf[80];

    if(!use_mapping)
        return;

    if(*fn != '/') {
        snprintf(fbuf, sizeof(fbuf), "/%s", fn);
        fn = fbuf;
    }

    shm_unlink(fn);
}

#else

void MappedMemory::remove(const char *name)
{
    assert(name != NULL && *name != 0);

    key_t key;
    fd_t fd;

    if(!use_mapping)
        return;

    key = accessipc(name, 'S');
    if(key) {
        fd = shmget(key, 0, 0);
        if(fd > -1)
            shmctl(fd, IPC_RMID, NULL);
    }
}

void MappedMemory::create(const char *name, size_t len)
{
    assert(name != NULL && *name != 0);

    struct shmid_ds stat;
    size = 0;
    used = 0;
    key_t key;

    if(!use_mapping) {
        assert(len > 0);
        map = (caddr_t)malloc(len);
        if(!map)
            fault();
        size = len;
        return;
    }

    if(len) {
        key = createipc(name, 'S');
remake:
        fd = shmget(key, len, IPC_CREAT | IPC_EXCL | 0664);
        if(fd == -1 && errno == EEXIST) {
            fd = shmget(key, 0, 0);
            if(fd > -1) {
                shmctl(fd, IPC_RMID, NULL);
                goto remake;
            }
        }
    }
    else {
        key = accessipc(name, 'S');
        fd = shmget(key, 0, 0);
    }

    if(fd > -1) {
        if(len)
            size = len;
        else if(shmctl(fd, IPC_STAT, &stat) == 0)
            size = stat.shm_segsz;
        else
            fd = -1;
    }
    map = (caddr_t)shmat(fd, NULL, 0);
    if(!map)
        fault();
#ifdef  SHM_LOCK
    if(fd > -1)
        shmctl(fd, SHM_LOCK, NULL);
#endif
}

MappedMemory::~MappedMemory()
{
    release();
}

void MappedMemory::release(void)
{
    if(size > 0) {
        if(use_mapping) {
#ifdef  SHM_UNLOCK
            shmctl(fd, SHM_UNLOCK, NULL);
#endif
            shmdt(map);
            fd = -1;
        }
        else
            free(map);
        size = 0;
    }
    if(erase) {
        remove(idname);
        erase = false;
    }
}

#endif

void *MappedMemory::invalid(void) const
{
    abort();
}

void MappedMemory::fault(void) const
{
    abort();
}

void *MappedMemory::sbrk(size_t len)
{
    assert(len > 0);
    void *mp = (void *)(map + used);
    if(used + len > size)
        fault();
    used += len;
    return mp;
}

bool MappedMemory::copy(size_t offset, void *buffer, size_t bufsize) const
{
    if(!map || (offset + bufsize > size)) {
        fault();
        return false;
    }

    const void *member = (const void *)(map + offset);

    do {
        memcpy(buffer, member, bufsize);
    } while(memcmp(buffer, member, bufsize));

    return true;
}

void *MappedMemory::offset(size_t offset) const
{
    if(offset >= size)
        return invalid();
    return (void *)(map + offset);
}

MappedReuse::MappedReuse(const char *name, size_t osize, unsigned count) :
ReusableAllocator(), MappedMemory(name,  osize * count)
{
    assert(name != NULL && *name != 0);
    assert(osize > 0 && count > 0);

    objsize = osize;
    reading = 0;
}

MappedReuse::MappedReuse(size_t osize) :
ReusableAllocator(), MappedMemory()
{
    assert(osize > 0);

    objsize = osize;
    reading = 0;
}

bool MappedReuse::avail(void)
{
    bool rtn = false;
    lock();
    if(freelist || used < size)
        rtn = true;
    unlock();
    return rtn;
}

ReusableObject *MappedReuse::request(void)
{
    ReusableObject *obj = NULL;

    lock();
    if(freelist) {
        obj = freelist;
        freelist = next(obj);
    }
    else if(used + objsize <= size)
        obj = (ReusableObject *)sbrk(objsize);
    unlock();
    return obj;
}

ReusableObject *MappedReuse::get(void)
{
    return getTimed(Timer::inf);
}

void MappedReuse::removeLocked(ReusableObject *obj)
{
    assert(obj != NULL);

    obj->retain();
    obj->enlist((LinkedObject **)&freelist);
}

ReusableObject *MappedReuse::getLocked(void)
{
    ReusableObject *obj = NULL;

    if(freelist) {
        obj = freelist;
        freelist = next(obj);
    }
    else if(used + objsize <= size)
        obj = (ReusableObject *)sbrk(objsize);

    return obj;
}

ReusableObject *MappedReuse::getTimed(timeout_t timeout)
{
    bool rtn = true;
    struct timespec ts;
    ReusableObject *obj = NULL;

    if(timeout && timeout != Timer::inf)
        set(&ts, timeout);

    lock();
    while(rtn && (!freelist || (freelist && reading)) && used >= size) {
        ++waiting;
        if(timeout == Timer::inf)
            wait();
        else if(timeout)
            rtn = wait(&ts);
        else
            rtn = false;
        --waiting;
    }
    if(!rtn) {
        unlock();
        return NULL;
    }
    if(freelist) {
        obj = freelist;
        freelist = next(obj);
    }
    else if(used + objsize <= size)
        obj = (ReusableObject *)sbrk(objsize);
    unlock();
    return obj;
}

