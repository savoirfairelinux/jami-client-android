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

/**
 * The GNU Common C++ persistance engine by Daniel Silverstone.
 * @file ucommon/persist.h
 */

#if defined(OLD_STDCPP) || defined(NEW_STDCPP)
#if !defined(_MSC_VER) || _MSC_VER >= 1400
#ifndef _UCOMMON_PERSIST_H_
#define _UCOMMON_PERSIST_H_

#ifndef _UCOMMON_PLATFORM_H_
#include <ucommon/platform.h>
#endif

#include <iostream>
#include <string>
#include <vector>
#include <deque>
#include <map>

NAMESPACE_UCOMMON
#define NS_PREFIX   ucc::

// This typedef allows us to declare NewPersistObjectFunction now
typedef class PersistObject* (*NewPersistObjectFunction) (void);

class __EXPORT PersistException
{
public:
    PersistException(const std::string& reason);
    const std::string& getString() const;

    virtual ~PersistException() throw();

protected:
    std::string _what;
};

/**
 * Type manager for persistence engine.
 * This class manages the types for generation of the persistent objects.
 * Its data structures are managed automatically by the system. They are
 * implicitly filled by the constructors who declare classes to the system.
 *
 * @author Daniel Silverstone
 */
class __EXPORT TypeManager
{
public:
    /**
     * This manages a registration to the typemanager - attempting to
     * remove problems with the optimizers
     */
    class registration
    {
    public:
        registration(const char* name, NewPersistObjectFunction func);
        virtual ~registration();
    private:
        std::string myName;
    };

    /**
     * This adds a new construction function to the type manager
     */
    static void add(const char* name, NewPersistObjectFunction construction);

    /**
     * And this one removes a type from the managers lists
     */
    static void remove(const char* name);

    /**
     * This function creates a new object of the required type and
     * returns a pointer to it. NULL is returned if we couldn't find
     * the type
     */
    static PersistObject* createInstanceOf(const char* name);

    typedef std::map<std::string,NewPersistObjectFunction> StringFunctionMap;
};

/*
 * The following defines are used to declare and define the relevant code
 * to allow a class to use the Persistence::Engine code.
 */

#define DECLARE_PERSISTENCE(ClassType)                  \
  public:                               \
    friend NS_PREFIX PersistEngine& operator>>( NS_PREFIX PersistEngine& ar, ClassType *&ob);     \
    friend NS_PREFIX PersistEngine& operator<<( NS_PREFIX PersistEngine& ar, ClassType const &ob);    \
    friend NS_PREFIX PersistObject *createNew##ClassType();                \
    virtual const char* getPersistenceID() const;           \
    static NS_PREFIX TypeManager::Registration registrationFor##ClassType;

#define IMPLEMENT_PERSISTENCE(ClassType, FullyQualifiedName)              \
  NS_PREFIX PersistObject *createNew##ClassType() { return new ClassType; }              \
  const char* ClassType::getPersistenceID() const {return FullyQualifiedName;} \
  NS_PREFIX PersistEngine& operator>>(NS_PREFIX PersistEngine& ar, ClassType &ob)               \
    { ar >> (NS_PREFIX PersistObject &) ob; return ar; }                     \
  NS_PREFIX PersistEngine& operator>>(NS_PREFIX PersistEngine& ar, ClassType *&ob)                  \
    { ar >> (NS_PREFIX PersistObject *&) ob; return ar; }                    \
  NS_PREFIX PersistEngine& operator<<(NS_PREFIX PersistEngine& ar, ClassType const &ob)                 \
    { ar << (NS_PREFIX PersistObject const *)&ob; return ar; }               \
  NS_PREFIX TypeManager::Registration                             \
    ClassType::registrationFor##ClassType(FullyQualifiedName,         \
                          createNew##ClassType);

class PersistEngine;

/**
 * PersistObject
 *
 * Base class for classes that will be persistent.  This object is the base
 * for all Persistent data which is not natively serialized by the
 * persistence::engine
 *
 * It registers itself with the persistence::TypeManager
 * using a global constructor function. A matching deregister call
 * is made in a global destructor, to allow DLL's to use the
 * persistence::engine in a main executable.
 *
 * Persistable objects must never maintain bad pointers. If a pointer
 * doesn't point to something valid, it must be NULL.  This is so
 * the persistence engine knows whether to allocate memory for an object
 * or whether the memory has been pre-allocated.
 *
 * @author Daniel Silverstone
 */
class __EXPORT PersistObject
{
public:
    /**
     * This constructor is used in serialization processes.
     * It is called in CreateNewInstance in order to create
     * an instance of the class to have Read() called on it.
     */
    PersistObject();

    /**
     * Default destructor
     */
     virtual ~PersistObject();

    /**
     * This returns the ID of the persistent object (Its type)
     */
     virtual const char* getPersistenceID() const;

    /**
     * This method is used to write to the Persistence::Engine
     * It is not equivalent to the << operator as it writes only the data
     * and not the object type etc.
     */
     virtual bool write(PersistEngine& archive) const;

    /**
     * This method is used to read from a Persistence::Engine
     * It is not equivalent to the >> operator as it does no
     * typesafety or anything.
     */
     virtual bool read(PersistEngine& archive);
};

/**
 * Stream serialization of persistent classes.
 * This class constructs on a standard C++ STL stream and then
 * operates in the mode specified. The stream passed into the
 * constructor must be a binary mode to function properly.
 *
 * @author Daniel Silverstone
 */
class __EXPORT PersistEngine
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
     * the given mode. The stream must be initialized properly prior
     * to this call or problems will ensue.
     */
    PersistEngine(std::iostream& stream, EngineMode mode) throw(PersistException);

    virtual ~PersistEngine();

    // Write operations

    /**
     * writes a PersistObject from a reference.
     */
    inline void write(const PersistObject &object) throw(PersistException)
        {write(&object); };

    /**
     * writes a PersistObject from a pointer.
     */
    void write(const PersistObject *object) throw(PersistException);

    // writes supported primitive types
  // shortcut, to make the following more readable
#define CCXX_ENGINEWRITE_REF(valref) writeBinary((const uint8_t*)&valref,sizeof(valref))
    inline void write(int8_t i) throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
    inline void write(uint8_t i) throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
    inline void write(int16_t i)  throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
    inline void write(uint16_t i) throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
    inline void write(int32_t i)  throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
    inline void write(uint32_t i) throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
    inline void write(float i)  throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
    inline void write(double i) throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
    inline void write(bool i) throw(PersistException) { CCXX_ENGINEWRITE_REF(i); }
#undef CCXX_ENGINEWRITE_REF

    void write(const std::string& str) throw(PersistException);

    // Every write operation boils down to one or more of these
    void writeBinary(const uint8_t* data, const uint32_t size) throw(PersistException);

    // Read Operations

    /**
     * reads a PersistObject into a reference overwriting the object.
     */
    void read(PersistObject &object) throw(PersistException);

    /**
     * reads a PersistObject into a pointer allocating memory for the object if necessary.
     */
    void read(PersistObject *&object) throw(PersistException);

    // reads supported primitive types
  // shortcut, to make the following more readable
#define CCXX_ENGINEREAD_REF(valref) readBinary((uint8_t*)&valref,sizeof(valref))
    inline void read(int8_t& i) throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
    inline void read(uint8_t& i) throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
    inline void read(int16_t& i) throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
    inline void read(uint16_t& i) throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
    inline void read(int32_t& i) throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
    inline void read(uint32_t& i) throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
    inline void read(float& i)  throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
    inline void read(double& i) throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
    inline void read(bool &i) throw(PersistException) { CCXX_ENGINEREAD_REF(i); }
#undef CCXX_ENGINEREAD_REF

    void read(std::string& str) throw(PersistException);

    // Every read operation boiled down to one or more of these
    void readBinary(uint8_t* data, uint32_t size) throw(PersistException);

private:
    /**
     * reads the actual object data into a pre-instantiated object pointer
     * by calling the read function of the derived class.
     */
    void readObject(PersistObject* object) throw(PersistException);

    /**
     * reads in a class name, and caches it into the ClassMap.
     */
    const std::string readClass() throw(PersistException);


    /**
     * The underlying stream
     */
    std::iostream& myUnderlyingStream;

    /**
     * The mode of the engine. read or write
     */
    EngineMode myOperationalMode;

    /**
     * Typedefs for the Persistence::PersistObject support
     */
    typedef std::vector<PersistObject*>           ArchiveVector;
    typedef std::map<PersistObject const*, int32_t> ArchiveMap;
    typedef std::vector<std::string>                ClassVector;
    typedef std::map<std::string, int32_t>            ClassMap;

    ArchiveVector myArchiveVector;
    ArchiveMap myArchiveMap;
    ClassVector myClassVector;
    ClassMap myClassMap;
};

#define CCXX_RE(ar,ob)   ar.read(ob); return ar
#define CCXX_WE(ar,ob)   ar.write(ob); return ar

// Standard >> and << stream operators for PersistObject
/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, PersistObject &ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, PersistObject *&ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, PersistObject const &ob) throw(PersistException) {CCXX_WE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, PersistObject const *ob) throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, int8_t& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, int8_t ob)  throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, uint8_t& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, uint8_t ob)  throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, int16_t& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, int16_t ob)  throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, uint16_t& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, uint16_t ob)  throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, int32_t& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, int32_t ob)  throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, uint32_t& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, uint32_t ob)  throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, float& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, float ob)  throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, double& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, double ob) throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, std::string& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, std::string ob) throw(PersistException) {CCXX_WE(ar,ob);}

/** @relates PersistEngine */
inline PersistEngine& operator >>( PersistEngine& ar, bool& ob) throw(PersistException) {CCXX_RE(ar,ob);}
/** @relates PersistEngine */
inline PersistEngine& operator <<( PersistEngine& ar, bool ob)  throw(PersistException) {CCXX_WE(ar,ob);}

#undef CCXX_RE
#undef CCXX_WE

/**
 * The following are template classes
 */

/**
 * @relates PersistEngine
 * serialize a vector of some serializable content to
 * the engine
 */
template<class T>
PersistEngine& operator <<( PersistEngine& ar, typename std::vector<T> const& ob) throw(PersistException)
{
    ar << (uint32_t)ob.size();
    for(unsigned int i=0; i < ob.size(); ++i)
        ar << ob[i];
    return ar;
}

/**
 * @relates PersistEngine
 * deserialize a vector of deserializable content from
 * an engine.
 */
template<class T>
PersistEngine& operator >>( PersistEngine& ar, typename std::vector<T>& ob) throw(PersistException)
{
    ob.clear();
    uint32_t siz;
    ar >> siz;
    ob.resize(siz);
    for(uint32_t i=0; i < siz; ++i)
        ar >> ob[i];
    return ar;
}

/**
 * @relates PersistEngine
 * serialize a deque of some serializable content to
 * the engine
 */
template<class T>
PersistEngine& operator <<( PersistEngine& ar, typename std::deque<T> const& ob) throw(PersistException)
{
    ar << (uint32_t)ob.size();
  for(typename std::deque<T>::const_iterator it=ob.begin(); it != ob.end(); ++it)
        ar << *it;
    return ar;
}

/**
 * @relates PersistEngine
 * deserialize a deque of deserializable content from
 * an engine.
 */
template<class T>
PersistEngine& operator >>( PersistEngine& ar, typename std::deque<T>& ob) throw(PersistException)
{
    ob.clear();
    uint32_t siz;
    ar >> siz;
    //ob.resize(siz);
    for(uint32_t i=0; i < siz; ++i) {
    T node;
    ar >> node;
    ob.push_back(node);
        //ar >> ob[i];
  }
    return ar;
}

/**
 * @relates PersistEngine
 * serialize a map with keys/values which both are serializeable
 * to an engine.
 */
template<class Key, class Value>
PersistEngine& operator <<( PersistEngine& ar, typename std::map<Key,Value> const & ob) throw(PersistException)
{
    ar << (uint32_t)ob.size();
    for(typename std::map<Key,Value>::const_iterator it = ob.begin();it != ob.end();++it)
        ar << it->first << it->second;
    return ar;
}

/**
 * @relates PersistEngine
 * deserialize a map with keys/values which both are serializeable
 * from an engine.
 */
template<class Key, class Value>
PersistEngine& operator >>( PersistEngine& ar, typename std::map<Key,Value>& ob) throw(PersistException)
{
    ob.clear();
    uint32_t siz;
    ar >> siz;
    for(uint32_t i=0; i < siz; ++i) {
        Key a;
        ar >> a;
        ar >> ob[a];
    }
    return ar;
}

/**
 * @relates PersistEngine
 * serialize a pair of some serializable content to the engine.
 */
template<class x, class y>
PersistEngine& operator <<( PersistEngine& ar, std::pair<x,y> &ob) throw(PersistException)
{
    ar << ob.first << ob.second;
    return ar;
}

/**
 * @relates PersistEngine
 * deserialize a pair of some serializable content to the engine.
 */
template<class x, class y>
PersistEngine& operator >>(PersistEngine& ar, std::pair<x, y> &ob) throw(PersistException)
{
    ar >> ob.first >> ob.second;
    return ar;
}

END_NAMESPACE

#endif
#endif
#endif
