// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

#if defined(OLD_STDCPP) || defined(NEW_STDCPP)
#if !defined(_MSC_VER) || _MSC_VER >= 1400

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/persist.h>

using namespace UCOMMON_NAMESPACE;
using namespace std;

const uint32_t NullObject = 0xffffffff;

PersistException::PersistException(const std::string& reason) :
_what(reason)
{
}

const std::string& PersistException::getString() const
{
    return _what;
}

PersistException::~PersistException() throw()
{
}

const char* PersistObject::getPersistenceID() const
{
  return "PersistObject";
}

PersistObject::PersistObject()
{
  // Do nothing
}

PersistObject::~PersistObject()
{
  // Do nothing
}

bool PersistObject::write(PersistEngine& archive) const
{
  // Do nothing
  return true; // Successfully
}

bool PersistObject::read(PersistEngine& archive)
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
void TypeManager::add(const char* name, NewPersistObjectFunction construction)
{
  if (refCount++ == 0) {
    theInstantiationFunctions = new StringFunctionMap;
  }
  assert(_internal_GetMap().find(std::string(name)) == _internal_GetMap().end());
  _internal_GetMap()[std::string(name)] = construction;
}

void TypeManager::remove(const char* name)
{
  assert(_internal_GetMap().find(std::string(name)) != _internal_GetMap().end());
  _internal_GetMap().erase(_internal_GetMap().find(std::string(name)));
  if (--refCount == 0) {
    delete theInstantiationFunctions;
    theInstantiationFunctions = 0;
  }
}

PersistObject* TypeManager::createInstanceOf(const char* name)
{
  assert(refCount);
  assert(_internal_GetMap().find(std::string(name)) != _internal_GetMap().end());
  return (_internal_GetMap()[std::string(name)])();
}

TypeManager::registration::registration(const char *name, NewPersistObjectFunction func) :
myName(name)
{
    TypeManager::add(name, func);
}

TypeManager::registration::~registration()
{
    TypeManager::remove(myName.c_str());
}

PersistEngine::PersistEngine(std::iostream& stream, EngineMode mode) throw(PersistException) :
myUnderlyingStream(stream), myOperationalMode(mode)
{
}

PersistEngine::~PersistEngine()
{
    if (myUnderlyingStream.good())
        myUnderlyingStream.sync();
}

void PersistEngine::writeBinary(const uint8_t* data, const uint32_t size) throw(PersistException)
{
  if(myOperationalMode != modeWrite)
    throw("Cannot write to an input Engine");
  myUnderlyingStream.write((const char *)data,size);
}


void PersistEngine::readBinary(uint8_t* data, uint32_t size) throw(PersistException)
{
  if(myOperationalMode != modeRead)
    throw("Cannot read from an output Engine");
  myUnderlyingStream.read((char *)data,size);
}

void PersistEngine::write(const PersistObject *object) throw(PersistException)
{
  // Pre-step, if object is NULL, then don't serialize it - serialize a
  // marker to say that it is null.
  // as ID's are uint32's, NullObject will do nicely for the task
  if (object == NULL) {
    uint32_t id = NullObject;
    write(id);
    return;
  }

  // First off - has this Object been serialized already?
  ArchiveMap::const_iterator itor = myArchiveMap.find(object);
  if (itor == myArchiveMap.end()) {
    // Unfortunately we need to serialize it - here we go ....
    uint32_t id = (uint32_t)myArchiveMap.size();
    myArchiveMap[object] = id; // bumps id automatically for next one
    write(id);
    ClassMap::const_iterator classItor = myClassMap.find(object->getPersistenceID());
    if (classItor == myClassMap.end()) {
      uint32_t classId = (uint32_t)myClassMap.size();
      myClassMap[object->getPersistenceID()] = classId;
      write(classId);
      write(static_cast<std::string>(object->getPersistenceID()));
        }
    else {
      write(classItor->second);
        }
    std::string majik;
    majik = "OBST";
    write(majik);
    object->write(*this);
    majik = "OBEN";
    write(majik);
  }
  else {
    // This object has been serialized, so just pop its ID out
    write(itor->second);
  }
}

void PersistEngine::read(PersistObject &object) throw(PersistException)
{
  uint32_t id = 0;
  read(id);
  if (id == NullObject)
    throw("Object Id should not be NULL when un-persisting to a reference");

  // Do we already have this object in memory?
  if (id < myArchiveVector.size()) {
    object = *(myArchiveVector[id]);
    return;
  }

  // Okay - read the identifier for the class in...
  // we won't need it later since this object is already allocated
  readClass();

  // Okay then - we can read data straight into this object
  readObject(&object);
}

void PersistEngine::read(PersistObject *&object) throw(PersistException)
{
  uint32_t id = 0;
  read(id);
  // Is the ID a NULL object?
  if (id == NullObject) {
    object = NULL;
    return;
  }

  // Do we already have this object in memory?
  if (id < myArchiveVector.size()) {
    object = myArchiveVector[id];
    return;
  }

  // Okay - read the identifier for the class in...
  std::string className = readClass();

  // is the pointer already initialized? if so then no need to reallocate
  if (object != NULL) {
    readObject(object);
    return;
  }

  // Create the object (of the relevant type)
  object = TypeManager::createInstanceOf(className.c_str());
  if (object) {
    // Okay then - we can make this object
    readObject(object);
  }
  else
    throw(PersistException(std::string("Unable to instantiate object of class ")+className));
}

void PersistEngine::readObject(PersistObject* object) throw(PersistException)
{
  // Okay then - we can make this object
  myArchiveVector.push_back(object);
  std::string majik;
  read(majik);
  if(majik != std::string("OBST"))
    throw( PersistException("Missing Start-of-Object marker"));
  object->read(*this);
  read(majik);
  if(majik != std::string("OBEN"))
    throw( PersistException("Missing End-of-Object marker"));
}

const std::string PersistEngine::readClass() throw(PersistException)
{
  // Okay - read the identifier for the class in...
  uint32_t classId = 0;
  read(classId);
  std::string className;
  if (classId < myClassVector.size()) {
    className = myClassVector[classId];
  }
  else {
    // Okay the class wasn't known yet - save its name
    read(className);
    myClassVector.push_back(className);
  }

  return className;
}

void PersistEngine::write(const std::string& str) throw(PersistException)
{
  uint32_t len = (uint32_t)str.length();
  write(len);
  writeBinary((uint8_t*)str.c_str(),len);
}

void PersistEngine::read(std::string& str) throw(PersistException)
{
  uint32_t len = 0;
  read(len);
  uint8_t *buffer = new uint8_t[len+1];
  readBinary(buffer,len);
  buffer[len] = 0;
  str = (char*)buffer;
  delete[] buffer;
}

#endif
#endif
