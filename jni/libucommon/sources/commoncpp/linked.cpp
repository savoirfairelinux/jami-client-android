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

LinkedSingle::~LinkedSingle() {}

LinkedSingle *LinkedSingle::getFirst(void)
{
    return this;
}

LinkedSingle *LinkedSingle::getLast(void)
{
    LinkedSingle *obj = this;

    while(obj->nextObject)
        obj = obj->nextObject;

    return obj;
}

void LinkedSingle::insert(LinkedSingle& obj)
{
    obj.nextObject = nextObject;
    nextObject = &obj;
}

LinkedSingle &LinkedSingle::operator+=(LinkedSingle &obj)
{
    insert(obj);
    return *this;
}

LinkedDouble::~LinkedDouble()
{
    detach();
}

void LinkedDouble::enterLock() {}

void LinkedDouble::leaveLock() {}

LinkedDouble *LinkedDouble::firstObject()
{
    LinkedDouble *node = this;

    while(node->prevObject)
        node = node->prevObject;

    return node;
}

LinkedDouble *LinkedDouble::lastObject()
{
    LinkedDouble *node = this;

    while(node->nextObject)
        node = node->nextObject;

    return node;
}

LinkedDouble *LinkedDouble::getFirst(void)
{
    LinkedDouble *node;

    enterLock();
    node = firstObject();
    leaveLock();

    return node;
}

LinkedDouble *LinkedDouble::getLast(void)
{
    LinkedDouble *node;

    enterLock();
    node = lastObject();
    leaveLock();

    return node;
}

LinkedDouble *LinkedDouble::getInsert(void)
{
    return getLast();
}

void LinkedDouble::insert(LinkedDouble& obj, InsertMode position)
{
    LinkedDouble *node;

    enterLock();
    obj.detach();

    switch ( position ) {
    case modeAtFirst:
        node = firstObject();
        obj.nextObject = node;
        node->prevObject = &obj;
        break;

    case modeBefore:
        obj.nextObject = this;
        obj.prevObject = this->prevObject;
        this->prevObject = &obj;
        if (obj.prevObject)
            obj.prevObject->nextObject = &obj;
        break;

    case modeAfter:
        obj.nextObject = this->nextObject;
        obj.prevObject = this;
        this->nextObject = &obj;
        if (obj.nextObject)
            obj.nextObject->prevObject = &obj;
        break;

    case modeAtLast:
    default:
        node = lastObject();
        obj.nextObject = node->nextObject;
        obj.prevObject = node;
        node->nextObject = &obj;
        if(obj.nextObject)
            obj.nextObject->prevObject = &obj;
        break;
    }
    leaveLock();
}

void LinkedDouble::detach(void)
{
    enterLock();

    if(prevObject)
        prevObject->nextObject = nextObject;

    if(nextObject)
        nextObject->prevObject = prevObject;

    prevObject = NULL;
    nextObject = NULL;

    leaveLock();
}

LinkedDouble &LinkedDouble::operator+=(LinkedDouble &obj)
{
    insert(obj);
    return *this;
}

LinkedDouble &LinkedDouble::operator--()
{
    detach();
    return *this;
}

