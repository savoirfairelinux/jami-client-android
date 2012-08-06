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

#if !defined(_MSC_VER) || _MSC_VER >= 1300

#include <cc++/exception.h>
#include <cc++/export.h>
#include <cc++/persist.h>
#include "assert.h"

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

#ifdef  CCXX_EXCEPTIONS
#   ifndef  COMMON_STD_EXCEPTION

PersistException::PersistException(String const& reason)
: _what(reason)
{} // Nothing :)

const String& PersistException::getString() const
{
    return _what;
}
#   endif
#endif

const char* BaseObject::getPersistenceID() const
{
    return "BaseObject";
}

BaseObject::BaseObject()
{} // Do nothing

BaseObject::~BaseObject()
{} // Do nothing

bool BaseObject::write(Engine& archive) const
{
  // Do nothing
  return true; // Successfully
}

bool BaseObject::read(Engine& archive)
{
    // Do nothing
    return true; // Successfully
}

static TypeManager::StringFunctionMap* theInstantiationFunctions = 0;
static int refCount = 0;

TypeManager::StringFunctionMap& _internal_GetMap()
{
    return *theInstantiationFunctions;
}

void TypeManager::add(const char* name, NewBaseObjectFunction construction)
{
    if (refCount++ == 0) {
        theInstantiationFunctions = new StringFunctionMap;
    }
    assert(_internal_GetMap().find(String(name)) == _internal_GetMap().end());
    _internal_GetMap()[String(name)] = construction;
}

void TypeManager::remove(const char* name)
{
    assert(_internal_GetMap().find(String(name)) != _internal_GetMap().end());
    _internal_GetMap().erase(_internal_GetMap().find(String(name)));
    if (--refCount == 0) {
        delete theInstantiationFunctions;
        theInstantiationFunctions = 0;
    }
}

BaseObject* TypeManager::createInstanceOf(const char* name)
{
    if (!refCount || _internal_GetMap().find(String(name)) == _internal_GetMap().end())
        return NULL;
    return (_internal_GetMap()[String(name)])();
}

TypeManager::Registration::Registration(const char *name, NewBaseObjectFunction func) :
myName(name)
{
    TypeManager::add(name, func);
}

TypeManager::Registration::~Registration()
{
    TypeManager::remove(myName.c_str());
}

#ifdef  CCXX_NAMESPACES
}
#endif

#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
