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

/**
 * @file persist.h
 * @short Persistence library classes.
 **/

#ifndef CCXX_PERSIST_H_
#define CCXX_PERSIST_H_

#ifndef CCXX_CONFIG_H_
#include <cc++/config.h>
#endif

#ifndef CCXX_EXCEPTIONS_H_
#include <cc++/exception.h>
#endif

#ifndef CCXX_MISSING_H_
#include <cc++/missing.h>
#endif

#ifndef CCXX_STRING_H_
#include <cc++/string.h>
#endif

#ifdef HAVE_ZLIB_H
#ifndef NO_COMPRESSION
#include <zlib.h>
#endif
#else
#define NO_COMPRESSION
#endif

#include <iostream>
#include <string>
#include <vector>
#include <deque>
#include <map>

#ifdef CCXX_NAMESPACES
namespace ost {
#define NS_PREFIX ost::
#else
#define NS_PREFIX
#endif

#ifdef  CCXX_EXCEPTIONS
#ifdef  COMMON_STD_EXCEPTION

class __EXPORT PersistException : public Exception
{
public:
    PersistException(const String &what) : Exception(what) {};
};

#else

class __EXPORT PersistException
{
public:
    PersistException(const String& reason);
    inline const String& getString() const
        {return Exception::getString();};

    virtual ~PersistException() {} throw();
protected:
    String _what;
};

#endif
#endif

// This typedef allows us to declare NewBaseObjectFunction now
typedef class BaseObject* (*NewBaseObjectFunction) (void);

/**
 * This class manages the types for generation of the persistent objects.
 * Its data structures are managed automatically by the system. They are
 * implicitly filled by the constructors who declare classes to the system.
 *
 * @author Daniel Silverstone
 * @short Type manager for persistence engine.
 */
class __EXPORT TypeManager
{
public:

    /**
     * This manages a registration to the typemanager - attempting to
     * remove problems with the optimisers
     */
    class Registration
    {
    public:
        Registration(const char* name, NewBaseObjectFunction func);
        virtual ~Registration();
    private:
        String myName;
    };

    /**
     * This adds a new construction function to the type manager
     */
    static void add(const char* name, NewBaseObjectFunction construction);

    /**
     * And this one removes a type from the managers lists
     */
    static void remove(const char* name);

    /**
     * This function creates a new object of the required type and
     * returns a pointer to it. NULL is returned if we couldn't find
     * the type
     */
    static BaseObject* createInstanceOf(const char* name);

    typedef std::map<String,NewBaseObjectFunction> StringFunctionMap;
};


/*
 * The following defines are used to declare and define the relevant code
 * to allow a class to use the Persistence::Engine code.
 */

#define DECLARE_PERSISTENCE(ClassType)                  \
  public:                               \
    friend NS_PREFIX Engine& operator>>( NS_PREFIX Engine& ar, ClassType *&ob);     \
    friend NS_PREFIX Engine& operator<<( NS_PREFIX Engine& ar, ClassType const &ob);    \
    friend NS_PREFIX BaseObject *createNew##ClassType();                \
    virtual const char* getPersistenceID() const;           \
    static NS_PREFIX TypeManager::Registration registrationFor##ClassType;

#define IMPLEMENT_PERSISTENCE(ClassType, FullyQualifiedName)              \
  NS_PREFIX BaseObject *createNew##ClassType() { return new ClassType; }              \
  const char* ClassType::getPersistenceID() const {return FullyQualifiedName;} \
  NS_PREFIX Engine& operator>>(NS_PREFIX Engine& ar, ClassType &ob)               \
    { ar >> (NS_PREFIX BaseObject &) ob; return ar; }                     \
  NS_PREFIX Engine& operator>>(NS_PREFIX Engine& ar, ClassType *&ob)                  \
    { ar >> (NS_PREFIX BaseObject *&) ob; return ar; }                    \
  NS_PREFIX Engine& operator<<(NS_PREFIX Engine& ar, ClassType const &ob)                 \
    { ar << (NS_PREFIX BaseObject const *)&ob; return ar; }               \
  NS_PREFIX TypeManager::Registration                             \
    ClassType::registrationFor##ClassType(FullyQualifiedName,         \
                          createNew##ClassType);

class Engine;

/**
 * BaseObject
 *
 * This object is the base for all Persistent data which is not
 * natively serialised by the Persistence::Engine
 *
 * It registers itself with the Persistence::TypeManager
 * using a global constructor function. A matching deregister call
 * is made in a global destructor, to allow DLL's to use the
 * Persistence::Engine in a main executable.
 *
 * Persistable objects must never maintain bad pointers. If a pointer
 * doesn't point to something valid, it must be NULL.  This is so
 * the persistence engine knows whether to allocate memory for an object
 * or whether the memory has been pre-allocated.
 *
 * @author Daniel Silverstone
 * @short Base class for classes that will be persistent.
 */
class __EXPORT BaseObject
{
public:
    /**
     * This constructor is used in serialisation processes.
     * It is called in CreateNewInstance in order to create
     * an instance of the class to have Read() called on it.
     */
     BaseObject();

    /**
     * Default destructor
     */
     virtual ~BaseObject();

    /**
     * This returns the ID of the persistent object (Its type)
     */
     virtual const char* getPersistenceID() const;

    /**
     * This method is used to write to the Persistence::Engine
     * It is not equivalent to the << operator as it writes only the data
     * and not the object type etc.
     */
     virtual bool write(Engine& archive) const;

    /**
     * This method is used to read from a Persistence::Engine
     * It is not equivalent to the >> operator as it does no
     * typesafety or anything.
     */
     virtual bool read(Engine& archive);
};


/**
 * Engine
 *
 * This class constructs on a standard C++ STL stream and then
 * operates in the mode specified. The stream passed into the
 * constructor must be a binary mode to function properly.
 *
 * @author Daniel Silverstone
 * @short stream serialization of persistent classes.
 */
class __EXPORT Engine
{
public:
    /**
     * These are the modes the Persistence::Engine can work in
     */
    enum EngineMode {
        modeRead,
        modeWrite
    };

    /**
     * Constructs a Persistence::Engine with the specified stream in
     * the given mode. The stream must be initialised properly prior
     * to this call or problems will ensue. If built using zlib compress
         * can be used to enable compression
     */
    Engine(std::iostream& stream, EngineMode mode, bool compress=true) THROWS (PersistException);

    /**
     * This Flushes the buffers and closes the Persistence::Engine
     * this must happen before the underlying stream is shut down
     */
    void sync();

    /**
     * This says there are more objects to deserialize
     */
    bool more();

    virtual ~Engine();


    // Write operations

    /**
     * writes a BaseObject from a reference.
     */
    void write(const BaseObject &object) THROWS (PersistException)
    { write(&object); };

    /**
     * writes a BaseObject from a pointer.
     */
    void write(const BaseObject *object) THROWS (PersistException);

    // writes supported primitive types
    // shortcut, to make the following more readable
#define CCXX_ENGINEWRITE_REF(valref) writeBinary((const uint8*)&valref,sizeof(valref))
    void write(int8 i)   THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
    void write(uint8 i)  THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
    void write(int16 i)  THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
    void write(uint16 i) THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
    void write(int32 i)  THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
    void write(uint32 i) THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
#ifdef  HAVE_64_BITS
    void write(int64 i)  THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
    void write(uint64 i) THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
#endif
    void write(float i)  THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
    void write(double i) THROWS (PersistException) { CCXX_ENGINEWRITE_REF(i); }
#undef CCXX_ENGINEWRITE_REF

    void write(const String& str) THROWS (PersistException);
    void write(const std::string& str) THROWS (PersistException);

    // Every write operation boils down to one or more of these
    void writeBinary(const uint8* data, const uint32 size) THROWS (PersistException);


    // Read Operations

    /**
     * reads a BaseObject into a reference overwriting the object.
     */
    void read(BaseObject &object) THROWS (PersistException);

    /**
     * reads a BaseObject into a pointer allocating memory for the object if necessary.
     */
    void read(BaseObject *&object) THROWS (PersistException);

    // reads supported primitive types
  // shortcut, to make the following more readable
#define CCXX_ENGINEREAD_REF(valref) readBinary((uint8*)&valref,sizeof(valref))
    void read(int8& i)   THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
    void read(uint8& i)  THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
    void read(int16& i)  THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
    void read(uint16& i) THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
    void read(int32& i)  THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
    void read(uint32& i) THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
#ifdef  HAVE_64_BITS
    void read(int64& i)  THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
    void read(uint64& i) THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
#endif
    void read(float& i)  THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
    void read(double& i) THROWS (PersistException) { CCXX_ENGINEREAD_REF(i); }
#undef CCXX_ENGINEREAD_REF

    void read(String& str) THROWS (PersistException);
    void read(std::string& str) THROWS (PersistException);

    // Every read operation boild down to one or more of these
    void readBinary(uint8* data, uint32 size) THROWS (PersistException);

private:
    /**
     * reads the actual object data into a pre-instantiated object pointer
     * by calling the read function of the derived class.
     */
    void readObject(BaseObject* object) THROWS (PersistException);

    /**
     * reads in a class name, and caches it into the ClassMap.
     */
    const String readClass() THROWS (PersistException);


    /**
     * The underlying stream
     */
    std::iostream& myUnderlyingStream;

    /**
     * The mode of the engine. read or write
     */
    EngineMode myOperationalMode;

    /**
     * Typedefs for the Persistence::BaseObject support
     */
    typedef std::vector<BaseObject*>           ArchiveVector;
    typedef std::map<BaseObject const*, int32> ArchiveMap;
    typedef std::vector<String>                ClassVector;
    typedef std::map<String, int32>            ClassMap;

    ArchiveVector myArchiveVector;
    ArchiveMap myArchiveMap;
    ClassVector myClassVector;
    ClassMap myClassMap;

    // Compression support
    bool use_compression; // valid onlry if NO_COMPRESSION is false
#ifndef NO_COMPRESSION
    z_stream myZStream;
    uint8* myCompressedDataBuffer;
    uint8* myUncompressedDataBuffer;
    uint8* myLastUncompressedDataRead;
#endif
};

// Standard >> and << stream operators for BaseObject
/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, BaseObject &ob)      THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, BaseObject *&ob)      THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, BaseObject const &ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, BaseObject const *ob) THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, int8& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, int8 ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, uint8& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, uint8 ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, int16& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, int16 ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, uint16& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, uint16 ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, int32& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, int32 ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, uint32& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, uint32 ob)  THROWS (PersistException);

#ifdef  HAVE_64_BITS
/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, int64& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, int64 ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, uint64& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, uint64 ob)  THROWS (PersistException);
#endif

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, float& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, float ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, double& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, double ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, String& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, String ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, std::string& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, std::string ob)  THROWS (PersistException);

/** @relates Engine */
__EXPORT Engine& operator >>( Engine& ar, bool& ob) THROWS (PersistException);
/** @relates Engine */
__EXPORT Engine& operator <<( Engine& ar, bool ob)  THROWS (PersistException);

/**
 * The following are templated classes
 */

/**
 * @relates Engine
 * serialize a vector of some serializable content to
 * the engine
 */
template<class T>
Engine& operator <<( Engine& ar, typename std::vector<T> const& ob) THROWS (PersistException)
{
    ar << (uint32)ob.size();
    for(unsigned int i=0; i < ob.size(); ++i)
        ar << ob[i];
    return ar;
}

/**
 * @relates Engine
 * deserialize a vector of deserializable content from
 * an engine.
 */
template<class T>
Engine& operator >>( Engine& ar, typename std::vector<T>& ob) THROWS (PersistException)
{
    ob.clear();
    uint32 siz;
    ar >> siz;
    ob.resize(siz);
    for(uint32 i=0; i < siz; ++i)
        ar >> ob[i];
    return ar;
}

/**
 * @relates Engine
 * serialize a deque of some serializable content to
 * the engine
 */
template<class T>
Engine& operator <<( Engine& ar, typename std::deque<T> const& ob) THROWS (PersistException)
{
    ar << (uint32)ob.size();
  for(typename std::deque<T>::const_iterator it=ob.begin(); it != ob.end(); ++it)
        ar << *it;
    return ar;
}

/**
 * @relates Engine
 * deserialize a deque of deserializable content from
 * an engine.
 */
template<class T>
Engine& operator >>( Engine& ar, typename std::deque<T>& ob) THROWS (PersistException)
{
    ob.clear();
    uint32 siz;
    ar >> siz;
    //ob.resize(siz);
    for(uint32 i=0; i < siz; ++i) {
    T node;
    ar >> node;
    ob.push_back(node);
        //ar >> ob[i];
  }
    return ar;
}

/**
 * @relates Engine
 * serialize a map with keys/values which both are serializeable
 * to an engine.
 */
template<class Key, class Value>
Engine& operator <<( Engine& ar, typename std::map<Key,Value> const & ob) THROWS (PersistException)
{
    ar << (uint32)ob.size();
    for(typename std::map<Key,Value>::const_iterator it = ob.begin();it != ob.end();++it)
        ar << it->first << it->second;
    return ar;
}

/**
 * @relates Engine
 * deserialize a map with keys/values which both are serializeable
 * from an engine.
 */
template<class Key, class Value>
Engine& operator >>( Engine& ar, typename std::map<Key,Value>& ob) THROWS (PersistException)
{
    ob.clear();
    uint32 siz;
    ar >> siz;
    for(uint32 i=0; i < siz; ++i) {
        Key a;
        ar >> a;
        ar >> ob[a];
    }
    return ar;
}

/**
 * @relates Engine
 * serialize a pair of some serializable content to the engine.
 */
template<class x, class y>
Engine& operator <<( Engine& ar, std::pair<x,y> &ob) THROWS (PersistException)
{
    ar << ob.first << ob.second;
    return ar;
}

/**
 * @relates Engine
 * deserialize a pair of some serializable content to the engine.
 */
template<class x, class y>
Engine& operator >>(Engine& ar, std::pair<x, y> &ob) THROWS (PersistException)
{
    ar >> ob.first >> ob.second;
    return ar;
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
