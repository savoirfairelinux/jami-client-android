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
#include <cc++/slog.h>
#include <cc++/thread.h>
#include <cc++/misc.h>
#include <cstdlib>
#include "private.h"

#ifdef  CCXX_NAMESPACES
namespace ost {
using std::endl;
#endif

const static size_t palign = __BYTE_ALIGNMENT;

#if defined(HAVE_POSIX_MEMALIGN) && __BYTE_ALIGNMENT < 2
#undef  HAVE_POSIX_MEMALIGN
#endif

#ifdef  HAVE_POSIX_MEMALIGN
static volatile size_t malign = 0;
#endif

MemPager::MemPager(size_t pg)
{
    pagesize = ((pg + sizeof(void *) - 1) / sizeof(void *)) * sizeof(void *);
    pages = 1;

#ifdef  COMMON_MEMORY_AUDIT
    slog(Slog::levelDebug) << "MemPager: creating pool, id=" << this << endl;
    slog(Slog::levelDebug) << "MemPager: alloc, id="
        << this << " page=" << page << endl;
#endif
#if defined(HAVE_POSIX_MEMALIGN)
    unsigned p2 = 2;
    if(!malign) {
        while(p2 < sizeof(void *))
            p2 *= 2;
        while(((p2 / sizeof(void *)) * sizeof(void *)) != p2)
            p2 *= 2;
        malign = p2;
    }
    posix_memalign((void **)&page, malign, pagesize);
#else
    page = (struct _page *)::new void *[pagesize / sizeof(void *)];
#endif
    page->next = NULL;
    page->used = sizeof(struct _page);
}

MemPager::~MemPager()
{
    clean();
}

void MemPager::clean(void)
{
    struct _page *root = page;

    while(root) {
        page = page->next;
#ifdef  COMMON_MEMORY_AUDIT
        slog(Slog::levelDebug) << "MemPager: delete, id="
            << this << " page=" << root << endl;
#endif
#ifdef  HAVE_POSIX_MEMALIGN
        ::free(root);
#else
        delete[] root;
#endif
        root = page;
    }
#ifdef  COMMON_MEMORY_AUDIT
    slog(Slog::levelDebug) << "Mempager: destroy pool, id=" << this << endl;
#endif
}

void MemPager::purge(void)
{
    struct _page *root = page;

    while(root->next) {
        page = root->next;
#ifdef  COMMON_MEMORY_AUDIT
        slog(Slog::levelDebug) << "Mempager: delete, id="
            << this << " page=" << root << endl;
#endif
#ifdef  HAVE_POSIX_MEMALIGN
        ::free(root);
#else
        delete[] root;
#endif
        --pages;
        root = page;
    }
    page->used = sizeof(struct _page);
}


void *MemPager::alloc(size_t size)
{
    char *ptr;
    struct _page *npage;

#if __BYTE_ALIGNMENT > 1
    size_t align = size % palign;
    if (align)
        size += palign - align;
#endif

    if(size > pagesize - sizeof(struct _page)) {
        slog.critical("mempager overflow");
#ifdef  CCXX_EXCEPTIONS
        if(Thread::getException() == Thread::throwObject)
            throw this;
#ifdef  COMMON_STD_EXCEPTION
        else if(Thread;:getException() == Thread::throwException)
            Throw Exception("Mempager::alloc(): Memory Overflow");
#endif
#else
        abort();
#endif
    }

    if(page->used + size > pagesize) {
#if defined(HAVE_POSIX_MEMALIGN)
        posix_memalign((void **)&npage, malign, pagesize);
#else
        npage = (struct _page *) ::new void *[pagesize / sizeof(void *)];
#endif

#ifdef  COMMON_MEMORY_AUDIT
        slog(Slog::levelDebug) << "MemPager: alloc, id="
            << this << " page=" << npage << endl;
#endif
        npage->next = page;
        npage->used = sizeof(struct _page);
        page = npage;
        ++pages;
    }
    ptr = (char *)page;
    ptr += page->used;
    page->used += size;
    return (void *)ptr;
}

void *MemPager::first(size_t size)
{
    struct _page *npage = page;
    char *ptr;

#if __BYTE_ALIGNMENT > 1
    size_t align = size % palign;

    if (align)
        size += palign - align;
#endif

    while(npage) {
        if(npage->used + size <= pagesize)
            break;

        npage = npage->next;
    }
    if(!npage)
        return alloc(size);

    ptr = (char *)npage;
    ptr += npage->used;
    npage->used += size;
    return (void *)ptr;
}

char *MemPager::alloc(const char *str)
{
    size_t len = strlen(str) + 1;
    char *cp = (char *)alloc(len);
    return setString(cp, len, str);
}

char *MemPager::first(char *str)
{
    size_t len = strlen(str) + 1;
    char *cp = (char *)first(len);
    return setString(cp, len, str);
}

StackPager::StackPager(size_t pg) :
MemPager(pg)
{
    stack = NULL;
}

void StackPager::purge(void)
{
    MemPager::purge();
    stack = NULL;
}

void *StackPager::pull(void)
{
    frame_t *object = stack;

    if(!stack) {
        purge();
        return NULL;
    }

    stack = object->next;
    return object->data;
}

void *StackPager::push(const void* object, size_t len)
{
    frame_t *frame = (frame_t *)alloc(len + sizeof(frame_t) - 1);

    if(!frame)
        return NULL;

    frame->next = stack;
    stack = frame;
    memcpy(frame->data, object, len);
    return (void *)frame->data;
}

void *StackPager::push(const char *string)
{
    return push(string, strlen(string) + 1);
}

SharedMemPager::SharedMemPager(size_t pg, const char *name) :
MemPager(pg), Mutex(name)
{}

void SharedMemPager::purge(void)
{
    enterMutex();
    MemPager::purge();
    leaveMutex();
}

void *SharedMemPager::first(size_t size)
{
    void *mem;

    enterMutex();
    mem = MemPager::first(size);
    leaveMutex();
    return mem;
}

void *SharedMemPager::alloc(size_t size)
{
    void *mem;

    enterMutex();
    mem = MemPager::alloc(size);
    leaveMutex();
    return mem;
}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
