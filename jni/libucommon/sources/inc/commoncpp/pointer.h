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
 * @file commoncpp/pointer.h
 * @short Template for creating reference count managed smart pointers.
 **/

#ifndef COMMONCPP_POINTER_H_
#define COMMONCPP_POINTER_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

NAMESPACE_COMMONCPP

/**
 * Used to create and manage referece counted pointers.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short reference counted pointer template.
 */
template <class T>
class Pointer
{
protected:
    unsigned *ptrCount;
    T *ptrObject;

    void ptrDetach(void)
    {
        if(ptrCount && --(*ptrCount)==0) {
            delete ptrObject;
            delete ptrCount;
        }
        ptrObject = NULL;
        ptrCount = NULL;
    }

public:
    explicit Pointer(T* ptr = NULL) : ptrObject(ptr)
    {
        ptrCount = new unsigned;
        *ptrCount = 1;
    }

    Pointer(const Pointer<T> &ref)
    {
        ptrObject = ref.ptrObject;
        ptrCount = ref.ptrCount;
        ++(*ptrCount);
    }

    inline virtual ~Pointer()
        {ptrDetach();}


    Pointer& operator=(const Pointer<T> &ref)
    {
        if(this != &ref) {
            ptrDetach();
            ptrObject = ref.ptrObject;
            ptrCount = ref.ptrCount;
            ++(*ptrCount);
        }
        return *this;
    }

    inline T& operator*() const
        {return *ptrObject;};

    inline T* getObject() const
        {return ptrObject;};

    inline T* operator->() const
        {return ptrObject;};

    inline bool operator!() const
        {return (*ptrCount == 1);};

    inline int operator++() const
        {return ++(*ptrCount);};

    int operator--() const
    {
        if(*ptrCount == 1) {
            delete this;
            return 0;
        }
        return --(*ptrCount);
    }
};

END_NAMESPACE

#endif
