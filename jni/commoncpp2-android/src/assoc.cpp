// Copyright (C) 2004-2005 Open Source Telecom Corporation.
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
#include <cc++/misc.h>
#include "private.h"

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

static unsigned getIndex(const char *id)
{
    unsigned idx = 0;
    while(*id)
        idx = (idx << 1) ^ (*(id++) & 0x1f);

    return idx % KEYDATA_INDEX_SIZE;
}

Assoc::Assoc()
{
    clear();
}

Assoc::~Assoc()
{
}

void Assoc::clear(void)
{
    memset(&entries, 0, sizeof(entries));
}

void Assoc::setPointer(const char *id, void *data)
{
    unsigned idx = getIndex(id);
    entry *e = (entry *)getMemory(sizeof(entry));
    e->id = (const char *)getMemory(strlen(id) + 1);
    strcpy((char *)e->id, id);
    e->data = data;
    e->next = entries[idx];
    entries[idx] = e;
}

void *Assoc::getPointer(const char *id) const
{
    entry *e = entries[getIndex(id)];

    while(e) {
        if(!stricmp(e->id, id))
            break;
        e = e->next;
    }
    if(e)
        return e->data;
    return NULL;
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
