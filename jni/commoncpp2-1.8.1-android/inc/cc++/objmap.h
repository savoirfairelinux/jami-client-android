// Copyright (C) 2001-2005 Open Source Telecom Corporation.
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
// As a special exception to the GNU General Public License, permission is
// granted for additional uses of the text contained in its release
// of Common C++.
//
// The exception is that, if you link the Common C++ library with other
// files to produce an executable, this does not by itself cause the
// resulting executable to be covered by the GNU General Public License.
// Your use of that executable is in no way restricted on account of
// linking the Common C++ library code into it.
//
// This exception does not however invalidate any other reasons why
// the executable file might be covered by the GNU General Public License.
//
// This exception applies only to the code released under the
// name Common C++.  If you copy code from other releases into a copy of
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.

/**
 * @file objmap.h
 * @short Template for creating hash indexed objects.
 **/

#ifndef CCXX_OBJMAP_H
#define CCXX_OBJMAP_H

#include <cc++/strchar.h>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

/**
 * Used to create and manage a hash index of objects through a common
 * type.  The objects can be examined and a specific instance located
 * by a hash key.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short hash indexed searchable template chain.
 */
template <class T, class K, unsigned S>
class objMap {
protected:
    static T *objIndex[S];
    T* objNext;
    const K objKey;

    virtual unsigned keyIndex(K k)
    {
        unsigned idx = 0;
        unsigned char *p = (unsigned char *)&k;
        unsigned len = sizeof(K);

        while(len--) {
            idx ^= (idx << 1) ^ *p;
            ++p;
        }
        return idx % S;
    }

    inline unsigned getSize(void)
        {return S;}

    objMap(const K key)
    {
        unsigned idx = keyIndex(key);
        objKey = key;
        objNext = objIndex[idx];
        objIndex[idx] = (T *)this;
    }
public:
    static T *getObject(keystring key);
};

template <class T, unsigned S>
class keyMap : public objMap<T, keystring, S>
{
    keyMap(keystring key) : objMap<T, keystring, S>(key) {};

    unsigned keyIndex(keystring k)
    {
        unsigned idx = 0;
        while(*k) {
            idx = (idx << 1) ^ (unsigned)*k;
            ++k;
        }
        return idx % S;
    }
};

template <class T, class K, unsigned S>
T *objMap<T, K, S>::objIndex[S](0);

template <class T, class K, unsigned S>
T *objMap<T, K, S>::getObject(const keystring key)
{
    T *obj = objIndex[keyIndex(key)];
    while(obj) {
        if(key == obj->objKey)
            break;
        obj = obj->objNext;
    }
    return obj;
}

#ifdef  CCXX_NAMESPACES
} // namespace
#endif

#endif
