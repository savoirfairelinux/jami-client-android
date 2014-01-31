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

#include <ucommon-config.h>
#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/thread.h>
#include <commoncpp/object.h>

using namespace COMMONCPP_NAMESPACE;

MapIndex& MapIndex::operator=(MapObject *theObject)
{
    thisObject = theObject;
    return *this;
}

MapIndex& MapIndex::operator++()
{
    if ( thisObject == NULL )
        return *this;

    if (thisObject->nextObject != NULL) {
        thisObject = thisObject->nextObject;
    }
    else if (thisObject->table != NULL) {
        MapObject* obj = NULL;
        unsigned i = thisObject->table->getIndex(thisObject->idObject) + 1;


        thisObject->table->enterMutex();
        for ( ; obj == NULL && i < thisObject->table->range; i++)
                obj = thisObject->table->map[i];
        thisObject->table->leaveMutex();

        thisObject = obj;
    }

    return *this;
}

MapTable::MapTable(unsigned size) :
Mutex()
{
    map = new MapObject *[size + 1];
    memset(map, 0, sizeof(MapObject *) * (size + 1));
    range = size;
    count = 0;
}

MapTable::~MapTable()
{
    cleanup();
}

void MapTable::cleanup(void)
{
    enterMutex();
    if(map)
        delete[] map;
    map = NULL;
    leaveMutex();
}

unsigned MapTable::getIndex(const char *id)
{
    unsigned key = 0;

    while(*id)
        key = (key << 1) ^ (*(id++) & 0x1f);

    return key % range;
}

void *MapTable::getObject(const char *id)
{
    if(!map)
        return NULL;

    enterMutex();
    MapObject *obj = map[getIndex(id)];

    while(obj) {
        if(!stricmp(obj->idObject, id))
            break;
        obj = obj->nextObject;
    }
    leaveMutex();
    return (void *)obj;
}

void *MapTable::getFirst()
{
    MapObject *obj;

    if(!map)
        return NULL;

    enterMutex();
    obj = *map;
    for (register unsigned i = 0; obj == NULL && i < range; i++)
        obj = map[i];
    leaveMutex();
    return obj;
}

void *MapTable::getLast()
{
    MapObject *obj = NULL;

    if(!map)
        return NULL;

    enterMutex();
    for (register int i = range - 1; obj == NULL && i >= 0; i--)
        obj = map[i];

    if ( obj != NULL )
        while ( obj->nextObject != NULL )
            obj = obj->nextObject;
    leaveMutex();
    return obj;
}

void *MapTable::getFree(void)
{
    enterMutex();
    MapObject *obj = map[range];
    if(obj)
        map[range] = obj->nextObject;
    leaveMutex();
    return obj;
}

void MapTable::addFree(MapObject *obj)
{
    obj->detach();
    enterMutex();
    obj->nextObject = map[range];
    map[range] = obj;
    leaveMutex();
}

void MapTable::addObject(MapObject &obj)
{
    unsigned idx = getIndex(obj.idObject);

    if(obj.table == this || !map)
        return;

    obj.detach();
    enterMutex();
    obj.nextObject = map[idx];
    map[idx] = &obj;
    obj.table = this;
    count++;
    leaveMutex();
}

MapTable &MapTable::operator+=(MapObject &obj)
{
    addObject(obj);
    return *this;
}

MapTable &MapTable::operator-=(MapObject &obj)
{
    if(obj.table == this)
        obj.detach();
    return *this;
}

MapObject::MapObject(const char *id)
{
    table = NULL;
    idObject = id;
}

void MapObject::detach(void)
{
    MapObject *node, *prev = NULL;
    unsigned idx;

    if(!table)
        return;

    idx = table->getIndex(idObject);
    table->enterMutex();
    node = table->map[idx];

    while(node) {
        if(node == this)
            break;
        prev = node;
        node = prev->nextObject;
    }

    if(node && !prev)
        table->map[idx] = nextObject;
    else if(node)
        prev->nextObject = nextObject;
    table->count--;
    table->leaveMutex();
    table = NULL;
}
