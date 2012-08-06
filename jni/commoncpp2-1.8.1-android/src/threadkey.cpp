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
#include "private.h"

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifdef WIN32
# define KEY_INVALID 0xffffffff
# define pthread_key_delete(key) TlsFree(key)
# define pthread_getspecific(key) TlsGetValue(key)
# define pthread_setspecific(key, ptr) TlsSetValue(key, ptr)
#else
# ifndef    KEY_INVALID
#  define   KEY_INVALID ((pthread_key_t)(~0))
# endif
#endif

#ifndef WIN32
ThreadKey::ThreadKey(ThreadKey::TDestruct destruct)
{
    if (pthread_key_create(&key, destruct))
        key = KEY_INVALID;
}
#endif

ThreadKey::ThreadKey()
{
#ifdef WIN32
    key = TlsAlloc();
#else
    if(pthread_key_create(&key, NULL))
        key = KEY_INVALID;
#endif
}

ThreadKey::~ThreadKey()
{
    if(key != KEY_INVALID)
        pthread_key_delete(key);
}

void *ThreadKey::getKey(void)
{
    if(key != KEY_INVALID)
        return pthread_getspecific(key);
    else
        return NULL;
}

void ThreadKey::setKey(void *ptr)
{
    if(key != KEY_INVALID)
        pthread_setspecific(key, ptr);
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
