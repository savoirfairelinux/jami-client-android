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

// needed for GNU/LINUX glibc otherwise pread/pwrite wont work

#ifdef  __linux__
#ifndef _XOPEN_SOURCE
#define _XOPEN_SOURCE 500
#endif
/*
 * on old glibc's, this has to be
 * defined explicitly
 */
#ifndef _XOPEN_SOURCE_EXTENDED
#define _XOPEN_SOURCE_EXTENDED
#endif
#endif

#include <cc++/config.h>
#include <cc++/export.h>
#include <cc++/exception.h>
#include <cc++/thread.h>
#include <cc++/file.h>
#include <cc++/process.h>
#include "private.h"

#ifdef  __BORLANDC__
#include <stdio.h>
#include <stdlib.h>
#else
#include <cstdio>
#include <cstdlib>
#endif
#include <sys/stat.h>
#include <cerrno>

#ifndef WIN32

#ifdef  HAVE_SYS_PARAM_H
#include <sys/param.h>
#endif

#ifdef  HAVE_SYS_FILE_H
#include <sys/file.h>
#endif

#ifdef  HAVE_SYS_LOCKF_H
#include <sys/lockf.h>
#endif

#ifdef  COMMON_AIX_FIXES
#undef  LOCK_EX
#undef  LOCK_SH
#endif

#ifdef  MACOSX
#define MISSING_LOCKF
#endif

#ifndef F_LOCK
#define MISSING_LOCKF

enum {
    F_ULOCK = 1,
    F_LOCK,
    F_TLOCK,
    F_TEST
};

#endif

#endif // ndef WIN32

#if defined(_OSF_SOURCE) && defined(_POSIX_C_SOURCE) && _POSIX_C_SOURCE > 1
#undef  LOCK_EX
#undef  LOCK_SH
#endif

#if 0
/*
 * not used anymore ? (hen)
 */
static const char *clearfile(const char *pathname)
{
    remove(pathname);
    return pathname;
}

static const char *clearfifo(const char *pathname, int mode)
{
    remove(pathname);
    mkfifo(pathname, mode);
    return pathname;
}
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

RandomFile::RandomFile(const char *name) : Mutex(name)
{
#ifdef WIN32
    fd = INVALID_HANDLE_VALUE;
    // immediate is not defined on Win32
#else
    fd = -1;
    flags.immediate = false;
#endif
    flags.thrown = flags.initial = flags.temp = false;
    flags.count = 0;
    pathname = NULL;
}

RandomFile::RandomFile(const RandomFile &rf) : Mutex()
{
    // first, `dup'-licate the file descriptor/handle
#ifdef WIN32
    HANDLE pidHandle = GetCurrentProcess();
    HANDLE dupHandle;

    if(rf.fd != INVALID_HANDLE_VALUE) {
        if(!DuplicateHandle(pidHandle, rf.fd, pidHandle, &dupHandle, 0, FALSE, DUPLICATE_SAME_ACCESS))
            fd = INVALID_HANDLE_VALUE;
        else
            fd = dupHandle;
    }
    else
        fd = INVALID_HANDLE_VALUE;

#else
    if(rf.fd > -1)
        fd = dup(rf.fd);
    else
        fd = -1;

#endif

    flags = rf.flags;
    flags.count = 0;

    if(rf.pathname)
        pathname = newString(rf.pathname);
    else
        pathname = NULL;
}

RandomFile::~RandomFile()
{
    final();
}

File::Error RandomFile::restart(void)
{
    return errOpenFailed;
}

File::Attr RandomFile::initialize(void)
{
    return attrPublic;
}

void RandomFile::final(void)
{
#ifdef WIN32
    if(fd != INVALID_HANDLE_VALUE) {
        CloseHandle(fd);
        if(flags.temp && pathname)
            DeleteFile(pathname);
    }

#else
    if(fd > -1) {
        close(fd);
        if(flags.temp && pathname)
            remove(pathname);
    }
#endif

    if(pathname) {
        delString(pathname);
        pathname = NULL;
    }

#ifdef WIN32
    fd = INVALID_HANDLE_VALUE;
#else
    fd = -1;
#endif
    flags.count = 0;
    flags.initial = false;
}

RandomFile::Error RandomFile::error(Error id, char *str)
{
    errstr = str;
    errid = id;
    if(!flags.thrown) {
        flags.thrown = true;
#ifdef  CCXX_EXCEPTIONS
        if(Thread::getException() == Thread::throwObject)
            throw(this);
#ifdef  COMMON_STD_EXCEPTION
        else if(Thread::getException() == Thread::throwException) {
            if(!str)
                str = (char *)"";
            throw FileException(str);
        }
#endif
#endif
    }
    return id;
}

bool RandomFile::initial(void)
{
    bool init;

#ifdef WIN32
    if(fd == INVALID_HANDLE_VALUE)
#else
    if(fd < 0)
#endif
        return false;

    enterMutex();
    init = flags.initial;
    flags.initial = false;

    if(!init) {
        leaveMutex();
        return false;
    }

#ifdef WIN32
    Attr access = initialize();
    if(access == attrInvalid) {
        CloseHandle(fd);
        if(pathname)
            DeleteFile(pathname);
        fd = INVALID_HANDLE_VALUE;
        leaveMutex();
        error(errInitFailed);
        return false;
    }

#else
    int mode = (int)initialize();
    if(!mode) {
        close(fd);
        fd = -1;
        if(pathname)
            remove(pathname);
        leaveMutex();
        error(errInitFailed);
        return false;
    }
    fchmod(fd, mode);
#endif

    leaveMutex();
    return init;
}

#ifndef WIN32
RandomFile::Error RandomFile::setCompletion(Complete mode)
{
    long flag = fcntl(fd, F_GETFL);

    if(fd < 0)
        return errNotOpened;

    flags.immediate = false;
#ifdef O_SYNC
    flag &= ~(O_SYNC | O_NDELAY);
#else
    flag &= ~O_NDELAY;
#endif
    switch(mode) {
    case completionImmediate:
#ifdef O_SYNC
        flag |= O_SYNC;
#endif
        flags.immediate = true;
        break;

    case completionDelayed:
        flag |= O_NDELAY;

    //completionDeferred: ? (hen)
    case completionDeferred:
        break;
    }
    fcntl(fd, F_SETFL, flag);
    return errSuccess;
}
#endif

off_t RandomFile::getCapacity(void)
{
    off_t eof, pos = 0;
#ifdef WIN32
    if(!fd)
#else
    if(fd < 0)
#endif
        return 0;

    enterMutex();
#ifdef WIN32
    pos = SetFilePointer(fd, 0l, NULL, FILE_CURRENT);
    eof = SetFilePointer(fd, 0l, NULL, FILE_END);
    SetFilePointer(fd, pos, NULL, FILE_BEGIN);
#else
    lseek(fd, pos, SEEK_SET);
    pos = lseek(fd, 0l, SEEK_CUR);
    eof = lseek(fd, 0l, SEEK_END);
#endif
    leaveMutex();
    return eof;
}

bool RandomFile::operator!(void)
{
#ifdef WIN32
    return fd == INVALID_HANDLE_VALUE;
#else
    if(fd < 0)
        return true;

    return false;
#endif
}

ThreadFile::ThreadFile(const char *path) : RandomFile(path)
{
    first = NULL;
    open(path);
}

ThreadFile::~ThreadFile()
{
    final();
    fcb_t *next;
    while(first) {
        next = first->next;
        delete first;
        first = next;
    }
}

ThreadFile::Error ThreadFile::restart(void)
{
    return open(pathname);
}

ThreadFile::Error ThreadFile::open(const char *path)
{
#ifdef WIN32
    if(fd != INVALID_HANDLE_VALUE)
#else
    if(fd > -1)
#endif
        final();


    if(path != pathname) {
        if(pathname)
            delString(pathname);
        pathname = newString(path);
    }

    flags.initial = false;

#ifdef WIN32
    fd = CreateFile(pathname, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_RANDOM_ACCESS, NULL);
    if(fd == INVALID_HANDLE_VALUE) {
        flags.initial = true;
        fd = CreateFile(pathname, GENERIC_READ | GENERIC_WRITE, 0, NULL, CREATE_NEW, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_RANDOM_ACCESS, NULL);
    }
    if(fd == INVALID_HANDLE_VALUE)
        return errOpenFailed;

#else
    fd = ::open(pathname, O_RDWR);
    if(fd < 0) {
        flags.initial = true;
        fd = ::open(pathname, O_CREAT | O_RDWR | O_TRUNC,
                (int)attrPrivate);
    }
    if(fd < 0)
        return error(errOpenFailed);

#ifdef  LOCK_EX
    if(flock(fd, LOCK_EX | LOCK_NB)) {
        close(fd);
        fd = -1;
        return error(errOpenInUse);
    }
#endif
#endif // WIN32

    return errSuccess;
}

ThreadFile::fcb_t *ThreadFile::getFCB(void)
{
    fcb_t *fcb = (fcb_t *)state.getKey();

    if(!fcb) {
        fcb = new fcb_t;
        fcb->next = first;
        first = fcb;
        fcb->address = NULL;
        fcb->len = 0;
        fcb->pos = 0;
        state.setKey(fcb);
    }
    return fcb;
}

ThreadFile::Error ThreadFile::fetch(caddr_t address, ccxx_size_t len, off_t pos)
{
    fcb_t *fcb = getFCB();
#ifdef WIN32
    Thread::Cancel save;
    if(fd == INVALID_HANDLE_VALUE)
#else
    if(fd < 0)
#endif
        return errNotOpened;

    if(address)
        fcb->address = address;

    if(len)
        fcb->len = len;

    if(pos != -1)
        fcb->pos = pos;


#ifdef WIN32
    enterMutex();
    save = Thread::enterCancel();
    SetFilePointer(fd, fcb->pos, NULL, FILE_BEGIN);
    DWORD count;
    if(!ReadFile(fd, fcb->address, fcb->len, &count, NULL)) {
        Thread::exitCancel(save);
        leaveMutex();
        return errReadFailure;
    }
    Thread::exitCancel(save);
    leaveMutex();
    if(count < fcb->len)
        return errReadIncomplete;

    return errSuccess;

#else
#ifdef  HAVE_PREAD_PWRITE
    int io = ::pread(fd, fcb->address, fcb->len, fcb->pos);
#else
    enterMutex();
    lseek(fd, fcb->pos, SEEK_SET);
    int io = ::read(fd, fcb->address, fcb->len);
    leaveMutex();
#endif

    if((size_t) io == fcb->len)
        return errSuccess;

    if(io > -1)
        return errReadIncomplete;

    switch(errno) {
    case EINTR:
        return errReadInterrupted;
    default:
        return errReadFailure;
    }
#endif // WIN32
}

ThreadFile::Error ThreadFile::update(caddr_t address, ccxx_size_t len, off_t pos)
{
    fcb_t *fcb = getFCB();
#ifdef WIN32
    if(fd == INVALID_HANDLE_VALUE)
#else
    if(fd < 0)
#endif
        return errNotOpened;


    if(address)
        fcb->address = address;

    if(len)
        fcb->len = len;

    if(pos != -1)
        fcb->pos = pos;

#ifdef WIN32
    enterMutex();
    Thread::Cancel save = Thread::enterCancel();
    SetFilePointer(fd, fcb->pos, NULL, FILE_BEGIN);
    DWORD count;
    if(!WriteFile(fd, fcb->address, fcb->len, &count, NULL)) {
        Thread::exitCancel(save);
        leaveMutex();
        return errWriteFailure;
    }
    Thread::exitCancel(save);
    leaveMutex();
    if(count < fcb->len)
        return errWriteIncomplete;

    return errSuccess;

#else
#ifdef  HAVE_PREAD_PWRITE
    int io = ::pwrite(fd, fcb->address, fcb->len, fcb->pos);
#else
    enterMutex();
    lseek(fd, fcb->pos, SEEK_SET);
    int io = ::write(fd, fcb->address, fcb->len);
    leaveMutex();
#endif

    if((size_t) io == fcb->len)
        return errSuccess;

    if(io > -1)
        return errWriteIncomplete;

    switch(errno) {
    case EINTR:
        return errWriteInterrupted;
    default:
        return errWriteFailure;
    }
#endif //WIN32
}

ThreadFile::Error ThreadFile::append(caddr_t address, ccxx_size_t len)
{
    fcb_t *fcb = getFCB();
#ifdef WIN32
    if(fd == INVALID_HANDLE_VALUE)
#else
    if(fd < 0)
#endif
        return errNotOpened;

    if(address)
        fcb->address = address;

    if(len)
        fcb->len = len;

    enterMutex();

#ifdef WIN32
    Thread::Cancel save = Thread::enterCancel();
    fcb->pos = SetFilePointer(fd, 0l, NULL, FILE_END);
    DWORD count;
    if(!WriteFile(fd, fcb->address, fcb->len, &count, NULL)) {
        Thread::exitCancel(save);
        leaveMutex();
        return errWriteFailure;
    }
    Thread::exitCancel(save);
    leaveMutex();
    if(count < fcb->len)
        return errWriteIncomplete;

    return errSuccess;

#else
    fcb->pos = lseek(fd, 0l, SEEK_END);
    int io = ::write(fd, fcb->address, fcb->len);
    leaveMutex();

    if((size_t) io == fcb->len)
        return errSuccess;

    if(io > -1)
        return errWriteIncomplete;

    switch(errno) {
    case EINTR:
        return errWriteInterrupted;
    default:
        return errWriteFailure;
    }
#endif // WIN32
}

off_t ThreadFile::getPosition(void)
{
    fcb_t *fcb = getFCB();

    return fcb->pos;
}

bool ThreadFile::operator++(void)
{
    off_t eof;
    fcb_t *fcb = getFCB();

    fcb->pos += fcb->len;
    enterMutex();
#ifdef WIN32
    eof = SetFilePointer(fd, 0l, NULL, FILE_END);
#else
    eof = lseek(fd, 0l, SEEK_END);
#endif
    leaveMutex();

    if(fcb->pos >= eof) {
        fcb->pos = eof;
        return true;
    }
    return false;
}

bool ThreadFile::operator--(void)
{
    fcb_t *fcb = getFCB();

    fcb->pos -= fcb->len;
    if(fcb->pos <= 0) {
        fcb->pos = 0;
        return true;
    }
    return false;
}

SharedFile::SharedFile(const char *path) :
RandomFile(path)
{
    fcb.address = NULL;
    fcb.len = 0;
    fcb.pos = 0;
    open(path);
}

SharedFile::SharedFile(const SharedFile &sh) :
RandomFile(sh)
{
}

SharedFile::~SharedFile()
{
    final();
}

SharedFile::Error SharedFile::open(const char *path)
{
#ifdef WIN32
    if(fd != INVALID_HANDLE_VALUE)
#else
    if(fd > -1)
#endif
        final();

    if(path != pathname) {
        if(pathname)
            delString(pathname);
        pathname = newString(path);
    }

    flags.initial = false;

#ifdef WIN32
    fd = CreateFile(pathname, GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_RANDOM_ACCESS, NULL);
    if(fd == INVALID_HANDLE_VALUE) {
        flags.initial = true;
        fd = CreateFile(pathname, GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE, NULL, CREATE_NEW, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_RANDOM_ACCESS, NULL);
    }
    if(fd == INVALID_HANDLE_VALUE)
        return errOpenFailed;

    return errSuccess;

#else
    fd = ::open(pathname, O_RDWR);
    if(fd < 0) {
        flags.initial = true;
        fd = ::open(pathname, O_CREAT | O_RDWR | O_TRUNC,
                (int)attrPrivate);
    }
    if(fd < 0)
        return error(errOpenFailed);

#ifdef  LOCK_SH
    if(flock(fd, LOCK_SH | LOCK_NB)) {
        close(fd);
        fd = -1;
        return error(errOpenInUse);
    }
#endif
    return errSuccess;
#endif // WIN32
}

SharedFile::Error SharedFile::fetch(caddr_t address, ccxx_size_t len, off_t pos)
{
#ifdef WIN32
    if(fd == INVALID_HANDLE_VALUE)
#else
    if(fd < 0)
#endif
        return errNotOpened;

    enterMutex();
    if(address)
        fcb.address = address;

    if(len)
        fcb.len = len;

    if(pos != -1)
        fcb.pos = pos;

#ifdef WIN32
    Thread::Cancel save = Thread::enterCancel();
    OVERLAPPED over;
    SetFilePointer(fd, fcb.pos, NULL, FILE_BEGIN);
    over.hEvent = 0;
    over.Offset = fcb.pos;
    over.OffsetHigh = 0;
    LockFileEx(fd, LOCKFILE_EXCLUSIVE_LOCK, 0, fcb.len, 0, &over);
    DWORD count;
    if(!ReadFile(fd, fcb.address, fcb.len, &count, NULL)) {
        Thread::exitCancel(save);
        leaveMutex();
        return errReadFailure;
    }
    Thread::exitCancel(save);
    leaveMutex();
    if(count < fcb.len)
        return errReadIncomplete;

    return errSuccess;

#else
    lseek(fd, fcb.pos, SEEK_SET);
    if(lockf(fd, F_LOCK, fcb.len)) {
        leaveMutex();
        return errLockFailure;
    }

    int io = ::read(fd, fcb.address, fcb.len);
    leaveMutex();

    if((size_t) io == fcb.len)
        return errSuccess;

    if(io > -1)
        return errReadIncomplete;

    switch(errno) {
    case EINTR:
        return errReadInterrupted;
    default:
        return errReadFailure;
    }
#endif
}

#ifndef WIN32
SharedFile::Error SharedFile::clear(ccxx_size_t len, off_t pos)
{
    if(fd < 0)
        return errNotOpened;

    enterMutex();
    if(len)
        fcb.len = len;

    if(pos != -1)
        fcb.pos = pos;

    lseek(fd, fcb.pos, SEEK_SET);
    if(lockf(fd, F_ULOCK, fcb.len)) {
        leaveMutex();
        return errLockFailure;
    }
    leaveMutex();
    return errSuccess;
}
#endif // ndef WIN32

SharedFile::Error SharedFile::update(caddr_t address, ccxx_size_t len, off_t pos)
{
#ifdef WIN32
    if(fd == INVALID_HANDLE_VALUE)
#else
    if(fd < 0)
#endif
        return errNotOpened;

    enterMutex();
    if(address)
        fcb.address = address;

    if(len)
        fcb.len = len;

    if(pos != -1)
        fcb.pos = pos;

#ifdef WIN32
    Thread::Cancel save = Thread::enterCancel();
    OVERLAPPED over;
    SetFilePointer(fd, fcb.pos, NULL, FILE_BEGIN);
    over.hEvent = 0;
    over.Offset = pos;
    over.OffsetHigh = 0;
    DWORD count;
    if(!WriteFile(fd, fcb.address, fcb.len, &count, NULL)) {
        SetFilePointer(fd, fcb.pos, NULL, FILE_CURRENT);
        UnlockFileEx(fd, 0, len, 0, &over);
        Thread::exitCancel(save);
        leaveMutex();
        return errWriteFailure;
    }
    SetFilePointer(fd, fcb.pos, NULL, FILE_CURRENT);
    UnlockFileEx(fd, 0, len, 0, &over);
    Thread::exitCancel(save);
    leaveMutex();
    if(count < fcb.len)
        return errWriteIncomplete;

    return errSuccess;

#else
    lseek(fd, fcb.pos, SEEK_SET);
    int io = ::write(fd, fcb.address, fcb.len);
    if(lockf(fd, F_ULOCK, fcb.len)) {
        leaveMutex();
        return errLockFailure;
    }
    leaveMutex();

    if((size_t) io == fcb.len)
        return errSuccess;

    if(io > -1)
        return errWriteIncomplete;

    switch(errno) {
    case EINTR:
        return errWriteInterrupted;
    default:
        return errWriteFailure;
    }
#endif // WIN32
}

SharedFile::Error SharedFile::append(caddr_t address, ccxx_size_t len)
{
#ifdef WIN32
    if(fd == INVALID_HANDLE_VALUE)
#else
    if(fd < 0)
#endif
        return errNotOpened;

    enterMutex();
    if(address)
        fcb.address = address;

    if(len)
        fcb.len = len;

#ifdef WIN32
    Thread::Cancel save = Thread::enterCancel();
    fcb.pos = SetFilePointer(fd, 0l, NULL, FILE_END);
    OVERLAPPED over;
    over.hEvent = 0;
    over.Offset = fcb.pos;
    over.OffsetHigh = 0;
    LONG eof = fcb.pos;
    LockFileEx(fd, LOCKFILE_EXCLUSIVE_LOCK, 0, 0x7fffffff, 0, &over);
    fcb.pos = SetFilePointer(fd, 0l, NULL, FILE_END);
    DWORD count;
    if(!WriteFile(fd, fcb.address, fcb.len, &count, NULL)) {
        SetFilePointer(fd, eof, NULL, FILE_CURRENT);
        Thread::exitCancel(save);
        UnlockFileEx(fd, 0, 0x7fffffff, 0, &over);
        Thread::exitCancel(save);
        leaveMutex();
        return errWriteFailure;
    }
    SetFilePointer(fd, eof, NULL, FILE_CURRENT);
    UnlockFileEx(fd, 0, 0x7fffffff, 0, &over);
    Thread::exitCancel(save);
    leaveMutex();
    if(count < fcb.len)
        return errWriteIncomplete;

    return errSuccess;

#else
    fcb.pos = lseek(fd, 0l, SEEK_END);
    if(lockf(fd, F_LOCK, -1)) {
        leaveMutex();
        return errLockFailure;
    }
    fcb.pos = lseek(fd, 0l, SEEK_END);
    int io = ::write(fd, fcb.address, fcb.len);
    lseek(fd, fcb.pos, SEEK_SET);
    if(lockf(fd, F_ULOCK, -1)) {
        leaveMutex();
        return errLockFailure;
    }
    leaveMutex();

    if((size_t) io == fcb.len)
        return errSuccess;

    if(io > -1)
        return errWriteIncomplete;

    switch(errno) {
    case EINTR:
        return errWriteInterrupted;
    default:
        return errWriteFailure;
    }
#endif // WIN32
}

off_t SharedFile::getPosition(void)
{
    return fcb.pos;
}

bool SharedFile::operator++(void)
{
    off_t eof;

    enterMutex();
    fcb.pos += fcb.len;
#ifdef WIN32
    eof = SetFilePointer(fd, 0l, NULL, FILE_END);
#else
    eof = lseek(fd, 0l, SEEK_END);
#endif

    if(fcb.pos >= eof) {
        fcb.pos = eof;
        leaveMutex();
        return true;
    }
    leaveMutex();
    return false;
}

bool SharedFile::operator--(void)
{
    enterMutex();
    fcb.pos -= fcb.len;
    if(fcb.pos <= 0) {
        fcb.pos = 0;
        leaveMutex();
        return true;
    }
    leaveMutex();
    return false;
}

size_t MappedFile::pageAligned(size_t size)
{
    size_t pages = size / Process::getPageSize();

    if(size % Process::getPageSize())
        ++pages;

    return pages * Process::getPageSize();
}

#ifdef WIN32

static void makemapname(const char *source, char *target)
{
    unsigned count = 60;
    while(*source && count--) {
        if(*source == '/' || *source == '\\')
            *(target++) = '_';
        else
            *(target++) = toupper(*source);
        ++source;
    }
    *target = 0;
}

MappedFile::MappedFile(const char *fname, Access mode, size_t size) :
RandomFile(fname)
{
    DWORD share, page;
    map = INVALID_HANDLE_VALUE;
    fcb.address = NULL;

    switch(mode) {
        case accessReadOnly:
        share = FILE_SHARE_READ;
        page = PAGE_READONLY;
        prot = FILE_MAP_READ;
        break;
        case accessWriteOnly:
        share = FILE_SHARE_WRITE;
        page = PAGE_WRITECOPY;
        prot = FILE_MAP_COPY;
        break;
        case accessReadWrite:
        share = FILE_SHARE_READ|FILE_SHARE_WRITE;
        page = PAGE_READWRITE;
        prot = FILE_MAP_WRITE;
    }
    fd = CreateFile(pathname, mode, share, NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_RANDOM_ACCESS, NULL);
    if(fd == INVALID_HANDLE_VALUE) {
        error(errOpenFailed);
        return;
    }
    SetFilePointer(fd, (LONG)size, 0, FILE_BEGIN);
    SetEndOfFile(fd);
    makemapname(fname, mapname);
    map = CreateFileMapping(fd, NULL, page, 0, 0, mapname);
    if(!map)
        error(errMapFailed);
    fcb.address = MapViewOfFile(map, prot, 0, 0, size);
    fcb.len = (ccxx_size_t)size;
    fcb.pos = 0;
    if(!fcb.address)
        error(errMapFailed);
}

MappedFile::MappedFile(const char *fname, Access mode) :
RandomFile(fname)
{
    DWORD share, page;
    map = INVALID_HANDLE_VALUE;
    fcb.address = NULL;

    switch(mode) {
    case accessReadOnly:
        share = FILE_SHARE_READ;
        page = PAGE_READONLY;
        prot = FILE_MAP_READ;
        break;
    case accessWriteOnly:
        share = FILE_SHARE_WRITE;
        page = PAGE_WRITECOPY;
        prot = FILE_MAP_COPY;
        break;
    case accessReadWrite:
        share = FILE_SHARE_READ|FILE_SHARE_WRITE;
        page = PAGE_READWRITE;
        prot = FILE_MAP_WRITE;
    }
    fd = CreateFile(pathname, mode, share, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_RANDOM_ACCESS, NULL);
    if(fd == INVALID_HANDLE_VALUE) {
        error(errOpenFailed);
        return;
    }
    makemapname(fname, mapname);
    map = CreateFileMapping(fd, NULL, page, 0, 0, mapname);
    if(!map)
        error(errMapFailed);
}

MappedFile::MappedFile(const char *fname, pos_t pos, size_t len, Access mode) :
RandomFile(fname)
{
    DWORD share, page;
    map = INVALID_HANDLE_VALUE;
    fcb.address = NULL;

    switch(mode) {
    case accessReadOnly:
        share = FILE_SHARE_READ;
        page = PAGE_READONLY;
        prot = FILE_MAP_READ;
        break;
    case accessWriteOnly:
        share = FILE_SHARE_WRITE;
        page = PAGE_WRITECOPY;
        prot = FILE_MAP_COPY;
        break;
    case accessReadWrite:
        share = FILE_SHARE_READ|FILE_SHARE_WRITE;
        page = PAGE_READWRITE;
        prot = FILE_MAP_WRITE;
    }
    fd = CreateFile(pathname, mode, share, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_RANDOM_ACCESS, NULL);
    if(fd == INVALID_HANDLE_VALUE) {
        error(errOpenFailed);
        return;
    }
    makemapname(fname, mapname);
    map = CreateFileMapping(fd, NULL, page, 0, 0, mapname);
    if(!map) {
        error(errMapFailed);
        return;
    }
    fcb.address = MapViewOfFile(map, prot, 0, pos, len);
    fcb.len = (ccxx_size_t)len;
    fcb.pos = pos;
    if(!fcb.address)
        error(errMapFailed);
}

MappedFile::~MappedFile()
{
    if(fcb.address) {
        unlock();
        UnmapViewOfFile(fcb.address);
    }

    if(map != INVALID_HANDLE_VALUE)
        CloseHandle(map);

    final();
}

void MappedFile::sync(void)
{
}

void MappedFile::sync(caddr_t address, size_t len)
{
}

void MappedFile::release(caddr_t address, size_t len)
{
    if(fcb.address) {
        unlock();
        UnmapViewOfFile(fcb.address);
    }
    fcb.address = NULL;
}

caddr_t MappedFile::fetch(off_t pos, size_t len)
{
    if(fcb.address) {
        unlock();
        UnmapViewOfFile(fcb.address);
    }

    fcb.address = MapViewOfFile(map, prot, 0, pos, len);
    fcb.len = (ccxx_size_t)len;
    fcb.pos = pos;
    if(!fcb.address)
        error(errMapFailed);

    return fcb.address;
}

void MappedFile::update(size_t offset, size_t len)
{
}

void MappedFile::update(caddr_t address, size_t len)
{
}

bool MappedFile::lock(void)
{
    unlock();
    if(VirtualLock(fcb.address, fcb.len))
        fcb.locked = true;
    return fcb.locked;
}

void MappedFile::unlock(void)
{
    if(!fcb.address)
        fcb.locked = false;

    if(!fcb.locked)
        return;

    VirtualUnlock(fcb.address, fcb.len);
    fcb.locked = false;
}

#else
#ifdef  HAVE_MLOCK
MappedFile::MappedFile(const char *fname, Access mode) :
RandomFile(fname)
{
    fd = open(fname, (int)mode);
    if(fd < 0 && mode != accessReadOnly)
        fd = ::open(pathname, O_CREAT | O_RDWR | O_TRUNC,
                (int)attrPrivate);

    if(fd < 0) {
        error(errOpenFailed);
        return;
    }

    switch(mode) {
    case O_RDONLY:
        prot = PROT_READ;
        break;
    case O_WRONLY:
        prot = PROT_WRITE;
        break;
    default:
        prot = PROT_READ | PROT_WRITE;
    }
}

MappedFile::MappedFile(const char *fname, Access mode, size_t size) :
RandomFile(fname)
{
    fd = open(fname, (int)mode | O_CREAT, 0660);

    if(fd < 0) {
        error(errOpenFailed);
        return;
    }

    switch(mode) {
    case O_RDONLY:
        prot = PROT_READ;
        break;
    case O_WRONLY:
        prot = PROT_WRITE;
        break;
    default:
        prot = PROT_READ | PROT_WRITE;
    }

    enterMutex();
    lseek(fd, size, SEEK_SET);
    fcb.address = (caddr_t)mmap(NULL, size, prot, MAP_SHARED, fd, 0);
    fcb.len = size;
    fcb.pos = 0;
    leaveMutex();
    if((caddr_t)(fcb.address) == (caddr_t)(MAP_FAILED)) {
        close(fd);
        fd = -1;
        error(errMapFailed);
    }
}

MappedFile::MappedFile(const char *fname, pos_t pos, size_t len, Access mode) :
RandomFile(fname)
{
    fd = open(fname, (int)mode);
    if(fd < 0) {
        error(errOpenFailed);
        return;
    }

    switch(mode) {
    case O_RDONLY:
        prot = PROT_READ;
        break;
    case O_WRONLY:
        prot = PROT_WRITE;
        break;
    default:
        prot = PROT_READ | PROT_WRITE;
    }

    enterMutex();
    lseek(fd, pos + len, SEEK_SET);
    fcb.address = (caddr_t)mmap(NULL, len, prot, MAP_SHARED, fd, pos);
    fcb.len = len;
    fcb.pos = pos;
    leaveMutex();
    if((caddr_t)(fcb.address) == (caddr_t)(MAP_FAILED)) {
        close(fd);
        fd = -1;
        error(errMapFailed);
    }
}

MappedFile::~MappedFile()
{
    unlock();
    final();
}

void MappedFile::sync(void)
{
    msync(fcb.address, fcb.len, MS_SYNC);
}

void MappedFile::release(caddr_t address, size_t len)
{
    enterMutex();
    if(address)
        fcb.address = address;

    if(len)
        fcb.len = len;

    if(fcb.locked)
        unlock();

    munmap(fcb.address, fcb.len);
    leaveMutex();
}

caddr_t MappedFile::fetch(off_t pos, size_t len)
{
    enterMutex();
    unlock();
    fcb.len = len;
    fcb.pos = pos;
    lseek(fd, fcb.pos + len, SEEK_SET);
    fcb.address = (caddr_t)mmap(NULL, len, prot, MAP_SHARED, fd, pos);
    leaveMutex();
    return fcb.address;
}

bool MappedFile::lock(void)
{
    unlock();
    if(!mlock(fcb.address, fcb.len))
        fcb.locked = true;
    return fcb.locked;
}

void MappedFile::unlock(void)
{
    if(!fcb.address)
        fcb.locked = false;

    if(!fcb.locked)
        return;

    munlock(fcb.address, fcb.len);
    fcb.locked = false;
}

void MappedFile::update(size_t offset, size_t len)
{
    int mode = MS_ASYNC;
    caddr_t address;

    if(flags.immediate)
        mode = MS_SYNC;

    enterMutex();
    address = fcb.address;
    address += offset;
    if(!len)
        len = fcb.len;
    leaveMutex();
    msync(address, len, mode);
}

void MappedFile::update(caddr_t address, size_t len)
{
    int mode = MS_ASYNC;
    if(flags.immediate)
        mode = MS_SYNC;

    msync(address, len, mode);
}
#endif
#endif // ndef WIN32

#ifdef  WIN32
#ifndef SECS_BETWEEN_EPOCHS
#define SECS_BETWEEN_EPOCHS 11644473600LL
#endif

#ifndef SECS_TO_100NS
#define SECS_TO_100NS   10000000LL
#endif
#endif

time_t lastAccessed(const char *path)
{
#ifdef  WIN32
    __int64 ts;
    WIN32_FILE_ATTRIBUTE_DATA ino;

    if(!GetFileAttributesEx(path, GetFileExInfoStandard, &ino))
        return 0;

    ts = ((__int64)ino.ftLastAccessTime.dwHighDateTime << 32) +
        ino.ftLastAccessTime.dwLowDateTime;

    ts -= (SECS_BETWEEN_EPOCHS * SECS_TO_100NS);
    ts /= SECS_TO_100NS;

    return(time_t)ts;
#else
    struct stat ino;

    if(stat(path, &ino))
        return 0;

    return ino.st_atime;
#endif
}

time_t lastModified(const char *path)
{
#ifdef  WIN32
    __int64 ts;
    WIN32_FILE_ATTRIBUTE_DATA ino;

    if(!GetFileAttributesEx(path, GetFileExInfoStandard, &ino))
        return 0;

    ts = ((__int64)ino.ftLastWriteTime.dwHighDateTime << 32) +
        ino.ftLastWriteTime.dwLowDateTime;

    ts -= (SECS_BETWEEN_EPOCHS * SECS_TO_100NS);
    ts /= SECS_TO_100NS;

    return(time_t)ts;
#else
    struct stat ino;

    if(stat(path, &ino))
        return 0;

    return ino.st_mtime;
#endif
}

bool isDir(const char *path)
{
#ifdef WIN32
    DWORD attr = GetFileAttributes(path);
    if(attr == (DWORD)~0l)
        return false;

    if(attr & FILE_ATTRIBUTE_DIRECTORY)
        return true;

    return false;

#else
    struct stat ino;

    if(stat(path, &ino))
        return false;

    if(S_ISDIR(ino.st_mode))
        return true;

    return false;
#endif // WIN32
}

bool isFile(const char *path)
{
#ifdef WIN32
    DWORD attr = GetFileAttributes(path);
    if(attr == (DWORD)~0l)
        return false;

    if(attr & FILE_ATTRIBUTE_DIRECTORY)
        return false;

    return true;

#else
    struct stat ino;

    if(stat(path, &ino))
        return false;

    if(S_ISREG(ino.st_mode))
        return true;

    return false;
#endif // WIN32
}

#ifndef WIN32
// the Win32 version is given in line in the header
bool isDevice(const char *path)
{
    struct stat ino;

    if(stat(path, &ino))
        return false;

    if(S_ISCHR(ino.st_mode))
        return true;

    return false;
}
#endif

bool canAccess(const char *path)
{
#ifdef WIN32
    DWORD attr = GetFileAttributes(path);
    if(attr == (DWORD)~0l)
        return false;

    if(attr & FILE_ATTRIBUTE_SYSTEM)
        return false;

    if(attr & FILE_ATTRIBUTE_HIDDEN)
        return false;

    return true;

#else
    if(!access(path, R_OK))
        return true;

    return false;

#endif
}

bool canModify(const char *path)
{
#ifdef WIN32
    DWORD attr = GetFileAttributes(path);

    if(!canAccess(path))
        return false;

    if(attr & FILE_ATTRIBUTE_READONLY)
        return false;

    if(attr & (FILE_ATTRIBUTE_DIRECTORY | FILE_ATTRIBUTE_NORMAL))
        return true;

    return false;

#else
    if(!access(path, W_OK | R_OK))
        return true;

    return false;
#endif
}

#ifdef  WIN32

const char *File::getExtension(const char *path)
{
    const char *cp = strrchr(path, '\\');
    if(!cp)
        cp = strrchr(path, '/');
    if(!cp)
        cp = strrchr(path, ':');


    if(cp)
        ++cp;
    else
        cp = path;

    if(*cp == '.')
        return "";

    cp = strrchr(cp, '.');

    if(!cp)
        cp = "";

    return cp;
}

const char *File::getFilename(const char *path)
{
    const char *cp = strrchr(path, '\\');
    if(cp)
        return ++cp;

    cp = strrchr(path, '/');
    if(cp)
        return ++cp;

    cp = strrchr(path, ':');
    if(cp)
        ++cp;

    return path;
}

char *File::getFilename(const char *path, char *buffer, size_t size)
{
    const char *cp = strrchr(path, '\\');
    if(!cp)
        cp = strrchr(path, '/');
    if(!cp)
        cp = strrchr(path, ':');

    if(cp)
        snprintf(buffer, size, "%s", ++cp);
    else
        snprintf(buffer, size, "%s", path);

    return buffer;
}

char *File::getDirname(const char *path, char *buffer, size_t size)
{
    size_t len;
    const char *cp = strrchr(path, '\\');

    snprintf(buffer, size, "%s", path);

    if(!cp)
        cp = strrchr(path, '/');
    if(!cp)
        cp = strrchr(path, ':');

    if(!cp)
        return buffer;

    if(cp)
        len = cp - path;

    if(len >= size)
        len = size - 1;
    buffer[len] = 0;
    return buffer;
}

#else

const char *File::getExtension(const char *path)
{
    const char *cp = strrchr(path, '/');
    if(cp)
        ++cp;
    else
        cp = path;

    if(*cp == '.')
        return "";

    cp = strrchr(cp, '.');
    if(cp)
        return cp;
    return "";
}

char *File::getDirname(const char *path, char *buffer, size_t size)
{
    unsigned len;
    const char *cp = strrchr(path, '/');

    snprintf(buffer, size, "%s", path);

    if(!cp)
        return buffer;

    if(cp)
        len = cp - path;

    if(len >= size)
        len = size - 1;
    buffer[len] = 0;
    return buffer;
}

const char *File::getFilename(const char *path)
{
    const char *cp = strrchr(path, '/');

    if(cp)
        return ++cp;

    return path;
}

char *File::getFilename(const char *path, char *buffer, size_t size)
{
    const char *cp = strrchr(path, '/');

    if(cp)
        snprintf(buffer, size, "%s", ++cp);
    else
        snprintf(buffer, size, "%s", path);

    return buffer;
}

#endif

#ifdef  HAVE_REALPATH

char *File::getRealpath(const char *path, char *buffer, size_t size)
{
    char temp[PATH_MAX];
    setString(buffer, size, ".");
    if(!realpath(path, temp))
        return NULL;
    if(strlen(temp) >= size)
        return NULL;
    setString(buffer, size, temp);
    return buffer;
}

#else

#ifdef  WIN323
static char *getFile(char *path)
{
    char *cp = strchr(path, '\\');
    if(!cp)
        cp = strchr(path, '/');
    if(!cp)
        cp = strchr(path, ':');
    return cp;
}
#else
static char *getFile(char *path)
{
    return strchr(path, '/');
}
#endif

char *File::getRealpath(const char *path, char *buffer, size_t size)
{
    if(size > PATH_MAX)
        size = PATH_MAX;

    unsigned symlinks = 0;
#if !defined(DYNAMCIC_LOCAL_ARRAYS)
    char left[PATH_MAX];
#else
    char left[size];
#endif
    size_t left_len, buffer_len;

#ifdef  WIN32
    if(path[1] == ':')
#else
    if(path[0] == '/')
#endif
    {
        buffer[0] = '/';
        buffer[1] = 0;
        if(!path[1])
            return buffer;

        buffer_len = 1;
        snprintf(left, size, "%s", path + 1);
        left_len = strlen(left);
    }
    else {
        if(!Dir::getPrefix(buffer, size)) {
            snprintf(buffer, size, "%s", ".");
            return NULL;
        }
        buffer_len = strlen(buffer);
        snprintf(left, size, "%s", path);
        left_len = strlen(left);
    }

    if(left_len >= size || buffer_len >= size)
        return NULL;

    while(left_len > 0) {
#ifdef  HAVE_LSTAT
        struct stat ino;
#endif
#if !defined(DYNAMIC_LOCAL_ARRAYS)
        char next_token[PATH_MAX];
#else
        char next_token[size];
#endif
        char *p;
        const char *s = (p = getFile(left)) ? p : left + left_len;

        memmove(next_token, left, s - left);
        left_len -= s - left;
        if(p != NULL)
            memmove(left, s + 1, left_len + 1);

        next_token[s - left] = 0;
        if(buffer[buffer_len - 1] != '/') {
            if(buffer_len +1 >= size)
                return NULL;

            buffer[buffer_len++] = '/';
            buffer[buffer_len] = 0;
        }
        if(!next_token[0])
            continue;
        else if(!strcmp(next_token, "."))
            continue;
        else if(!strcmp(next_token, "..")) {
            if(buffer_len > 1) {
                char *q;
                buffer[buffer_len - 1] = 0;
#ifdef  WIN32
                q = strrchr(buffer, '\\');
                if(!q)
                    q = strrchr(buffer, '/');
                if(!q)
                    q = strchr(buffer, ':');
#else
                q = strrchr(buffer, '/');
#endif
                *q = 0;
                buffer_len = q - buffer;
            }
            continue;
        }
        snprintf(next_token, size, "%s", buffer);
        buffer_len = strlen(buffer);
        if(buffer_len >= size)
            return NULL;

#ifndef HAVE_LSTAT
        if(!isFile(buffer) && !isDir(buffer))
            return buffer;

        continue;
#else
        if(lstat(buffer, &ino) < 0) {
            if(errno == ENOENT && !p)
                return buffer;

            return NULL;
        }

        if((ino.st_mode & S_IFLNK) == S_IFLNK) {
                    char symlink[size];
            int slen;

            if (symlinks++ > MAXSYMLINKS)
                return NULL;

            slen = readlink(buffer, symlink, size);

            if (slen < 0)
                return NULL;
            symlink[slen] = 0;

            if (symlink[0] == '/') {
                buffer[1] = 0;
                buffer_len = 1;
                } else if (buffer_len > 1) {
                char *q;

                buffer[buffer_len - 1] = 0;
                q = strrchr(buffer, '/');
                *q = 0;
                buffer_len = q - buffer;
                }
            if (symlink[slen - 1] != '/' && p) {
                if (slen >= size)
                    return NULL;

                symlink[slen] = '/';
                symlink[slen + 1] = 0;
                }
            if(p) {
                snprintf(symlink, size, "%s", left);
                left_len = strlen(symlink);
            }
            if(left_len >= size)
                return NULL;
            snprintf(left, size, "%s", symlink);
            left_len = strlen(left);
        }
#endif

    }

#ifdef  WIN32
    if(buffer_len > 1 && buffer[buffer_len - 1] == '\\')
        buffer[buffer_len - 1] = 0;
    else if(buffer_len > 1 && buffer[buffer_len - 1] == '/')
        buffer[buffer_len - 1] = 0;
#else
    if(buffer_len > 1 && buffer[buffer_len - 1] == '/')
        buffer[buffer_len - 1] = 0;
#endif
    return buffer;
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
