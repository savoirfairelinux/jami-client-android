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

#include <ucommon-config.h>
#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/string.h>
#include <commoncpp/exception.h>
#include <commoncpp/slog.h>
#include <commoncpp/file.h>
#include <commoncpp/process.h>

using namespace COMMONCPP_NAMESPACE;

DSO *DSO::first = NULL;
DSO *DSO::last = NULL;
Mutex   DSO::mutex;

#if defined(HAVE_DLFCN_H)

extern "C" {
#include <dlfcn.h>
}

#ifndef RTLD_GLOBAL
#define RTLD_GLOBAL 0
#endif

#endif // HAVE_DLFN_H

#ifdef HAVE_SHL_LOAD
#include <dl.h>
#endif

#ifdef HAVE_MACH_O_DYLD_H
#include <mach-o/dyld.h>
#define oModule ((oModule)(image))
#endif

#ifdef  _MSWINDOWS_
#define hImage ((HMODULE)(image))
#endif

void DSO::dynunload(void)
{
    while(DSO::last)
        delete DSO::last;
}

DSO::~DSO()
{
#if defined(HAVE_MACH_DYLD)
    NSSymbol sym;
    void (*fini)(void);
#endif
    MutexLock lock(mutex);
#if defined(_MSWINDOWS_)
    if(image)
        FreeLibrary(hImage);
#elif defined(HAVE_MACH_DYLD)
    if(image == NULL)
        return;

    sym = NSLookupSymbolInModule(oModule, "__fini");
    if(sym != NULL) {
        fini = (void (*)(void))NSAddressOfSymbol(sym);
        fini();
    }

    NSUnLinkModule(oModule, NSUNLINKMODULE_OPTION_NONE);
#elif defined(HAVE_SHL_LOAD)
    if(image)
        shl_unload(image);
#elif defined(HAVE_DLFCN_H)
    if(image)
        dlclose(image);
#endif

    if(first == this && last == this)
        first = last = NULL;

    if(!next && !prev)
        return;

    if(prev)
        prev->next = next;

    if(next)
        next->prev = prev;

    if(first == this)
        first = next;
    if(last == this)
        last = prev;
}

void DSO::loader(const char *filename, bool flag)
{
#if defined(HAVE_MACH_DYLD)
    NSObjectFileImage oImage;
    NSSymbol sym = NULL;
    void (*init)(void);
#endif

    id = strrchr(filename, '/');
    if(id)
        ++id;
    else
        id = filename;

    next = prev = NULL;

#if defined(_MSWINDOWS_)
    image = LoadLibrary(filename);
    err = "none";
#elif defined(HAVE_MACH_DYLD)
    err = "none";
    image = NULL;

    fprintf(stderr, "**** HERE %s\n", filename);

    switch(NSCreateObjectFileImageFromFile(filename, &oImage)) {
    case NSObjectFileImageSuccess:
        break;
    default:
        err = "unknown error";
        return;
    }
    if(flag)
        image = NSLinkModule(oImage, filename, NSLINKMODULE_OPTION_BINDNOW | NSLINKMODULE_OPTION_RETURN_ON_ERROR);
    else
        image = NSLinkModule(oImage, filename, NSLINKMODULE_OPTION_RETURN_ON_ERROR);
    NSDestroyObjectFileImage(oImage);
    if(oModule != NULL)
        sym = NSLookupSymbolInModule(oModule, "__init");
    if(sym) {
        init = (void (*)(void))NSAddressOfSymbol(sym);
        init();
    }

#elif defined(HAVE_SHL_LOAD)
    err = "none";
    if(flag)
        image = shl_load(filename, BIND_IMMEDIATE, 0L);
    else
        image = shl_load(filename, BIND_DEFERRED, 0L);
#elif defined(HAVE_DLFCN_H)
    if(flag)
        image = dlopen(filename, RTLD_NOW | RTLD_GLOBAL);
    else
        image = dlopen(filename, RTLD_LAZY | RTLD_GLOBAL);

#endif

#if defined(_MSWINDOWS_)
    if(!image) {
        err = "load failed";
#elif defined(HAVE_MACH_DYLD)
    if(image == NULL) {
        err = "load failed";
#elif defined(HAVE_SHL_LOAD)
    if(!image) {
        err = "load failed";
#elif defined(HAVE_DLFCN_H)
    if(!image) {
        err = dlerror();
#else
    if(1) {
        err = "load unsupported";
#endif

// since generally failure to map or load a plugin is fatel in most
// cases, we should generally log the error to syslog as well as notify
// the upper level system of the failure through exception.

        slog.error() << "dso: " << id << ": " << err << std::endl;

#ifdef  CCXX_EXCEPTIONS
        if(Thread::getException() == Thread::throwObject)
            throw(this);
#ifdef  COMMON_STD_EXCEPTION
        else if(Thread::getException() == Thread::throwException)
            throw(DSOException(String(id) + err));
#endif
#endif
        return;
    }

    if(!last) {
        last = first = this;
        return;
    }

    mutex.enterMutex();
    last->next = this;
    prev = last;
    last = this;
    mutex.leaveMutex();
}

DSO *DSO::getObject(const char *id)
{
    const char *chk = strrchr(id, '/');
    DSO *dso;

    if(chk)
        ++chk;
    else
        chk = id;

    mutex.enterMutex();
    dso = first;
    while(dso) {
        if(!stricmp(dso->id, chk))
            break;
        dso = dso->next;
    }
    mutex.leaveMutex();
    return dso;
}

bool DSO::isValid(void)
{
#if defined(_MSWINDOWS_)
    if(!image)
#elif   defined(HAVE_MACH_DYLD)
    if(image == NULL)
#else
    if(!image)
#endif
        return false;

    return true;
}

DSO::addr_t DSO::operator[](const char *sym)
{
#if   defined(HAVE_SHL_LOAD)
    int value;
    shl_t handle = (shl_t)image;

    if(shl_findsym(&handle, sym, 0, &value) == 0)
        return (DSO::addr_t)value;
    else
        return NULL;
#elif defined(HAVE_MACH_DYLD)
    NSSymbol oSymbol = NSLookupSymbolInModule(oModule, sym);
    if(oSymbol)
        return (DSO::addr_t)NSAddressOfSymbol(oSymbol);
    else
        return (DSO::addr_t)NULL;
#elif defined(_MSWINDOWS_)
    DSO::addr_t addr = (DSO::addr_t)GetProcAddress(hImage, sym);
    if(!addr)
        err = "symbol missing";

    return addr;
#elif defined(HAVE_DLFCN_H)
    return (DSO::addr_t)dlsym(image, (char *)sym);
#else
    return (DSO::addr_t)NULL;
#endif
}

#ifdef  HAVE_MACH_DYLD
static void MyLinkError(NSLinkEditErrors c, int errorNumber, const char *filename, const char *errstr)
{
    slog.error() << "dyld: " << filename << ": " << errstr << std::endl;
}

static void MyUndefined(const char *symname)
{
    slog.error() << "dyld: undefined: " << symname << std::endl;
}

static NSModule MyMultiple(NSSymbol s, NSModule oMod, NSModule nMod)
{
    slog.error() << "dyld: multiply defined symbols" << std::endl;
}

void DSO::setDebug(void)
{
    static NSLinkEditErrorHandlers handlers = {
        &MyUndefined,
        &MyMultiple,
        &MyLinkError};

    NSInstallLinkEditErrorHandlers(&handlers);
}

#else

void DSO::setDebug(void)
{
}

#endif

