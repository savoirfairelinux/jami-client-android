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
 * Locking protocol classes for member function automatic operations.
 * This header covers ucommon access related classes.  These are used to
 * provide automatic management of locks and synchronization objects through
 * common virtual base classes which can be used with automatic objects.
 * These classes are related to "protocols" and are used in conjunction
 * with smart pointer/referencing classes.  The access interface supports
 * member functions to acquire a lock when entered and automatically
 * release the lock when the member function returns that are used in
 * conjunction with special referencing smart pointers.
 * @file ucommon/access.h
 */

// we do this twice because of some bizarre issue in just this file that
// otherwise breaks doxygen and lists all items outside the namespace...
#include <ucommon/platform.h>

#ifndef _UCOMMON_ACCESS_H_
#define _UCOMMON_ACCESS_H_

#ifndef _UCOMMON_CPR_H_
#include <ucommon/cpr.h>
#endif

#ifndef _UCOMMON_PROTOCOLS_H_
#include <ucommon/protocols.h>
#endif

NAMESPACE_UCOMMON

/**
 * Common unlock protocol for locking protocol interface classes.
 * This is to assure _unlock is a common base virtual method.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT UnlockAccess
{
public:
    virtual ~UnlockAccess();

protected:
    virtual void _unlock(void) = 0;
};

/**
 * An exclusive locking protocol interface base.
 * This is an abstract class to form objects that will operate under an
 * exclusive lock while being actively referenced by a smart pointer.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT ExclusiveAccess : public UnlockAccess
{
protected:
    virtual ~ExclusiveAccess();

    virtual void _lock(void) = 0;

public:
    /**
     * Access interface to exclusive lock the object.
     */
    inline void exclusive_lock(void)
        {return _lock();}

    /**
     * Access interface to release a lock.
     */
    inline void release_exclusive(void)
        {return _unlock();}
};

/**
 * An exclusive locking access interface base.
 * This is an abstract class to form objects that will operate under an
 * exclusive lock while being actively referenced by a smart pointer.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT SharedAccess : protected UnlockAccess
{
protected:
    virtual ~SharedAccess();

protected:
    /**
     * Access interface to share lock the object.
     */
    virtual void _share(void) = 0;

public:
    /**
     * Share the lock with other referencers.  Many of our shared locking
     * objects support the ability to switch between shared and exclusive
     * mode.  This derived protocol member allows one to restore the lock
     * to shared mode after it has been made exclusive.
     */
    virtual void share(void);

    /**
     * Convert object to an exclusive lock.  Many of our shared locking
     * objects such as the "conditional lock" support the ability to switch
     * between shared and exclusive locking modes.  This derived protocol
     * member allows one to temporarily assert exclusive locking when tied
     * to such methods.
     */
    virtual void exclusive(void);

    inline void shared_lock(void)
        {return _share();}

    inline void release_share(void)
        {return _unlock();}
};

/**
 * A kind of smart pointer object to support exclusive locking protocol.
 * This object initiates an exclusive lock for the object being referenced when
 * it is instantiated, and releases the exclusive lock when it is destroyed.
 * You would pass the pointer an object that has the Exclusive as a base class.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT exclusive_access
{
private:
    ExclusiveAccess *lock;

public:
    /**
     * Create an instance of an exclusive object reference.
     * @param object containing Exclusive base class protocol to lock.
     */
    exclusive_access(ExclusiveAccess *object);

    /**
     * Destroy reference to exclusively locked object, release lock.
     */
    ~exclusive_access();

    /**
     * Test if the reference holds an active lock.
     * @return true if is not locking an object.
     */
    inline bool operator!() const
        {return lock == NULL;};

    /**
     * Test if the reference holds an active lock.
     * @return true if locking an object.
     */
    inline operator bool() const
        {return lock != NULL;};

    /**
     * Release a held lock programmatically.  This can be used to de-reference
     * the object being exclusively locked without having to wait for the
     * destructor to be called when the exclusive_lock falls out of scope.
     */
    void release(void);
};

/**
 * A kind of smart pointer object to support shared locking protocol.
 * This object initiates a shared lock for the object being referenced when
 * it is instantiated, and releases the shared lock when it is destroyed.
 * You would pass the pointer an object that has the Shared as a base class.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT shared_access
{
private:
    SharedAccess *lock;
    int state;
    bool modify;

public:
    /**
     * Create an instance of an exclusive object reference.
     * @param object containing Exclusive base class protocol to lock.
     */
    shared_access(SharedAccess *object);

    /**
     * Destroy reference to shared locked object, release lock.
     */
    ~shared_access();

    /**
     * Test if the reference holds an active lock.
     * @return true if is not locking an object.
     */
    inline bool operator!() const
        {return lock == NULL;};

    /**
     * Test if the reference holds an active lock.
     * @return true if locking an object.
     */
    inline operator bool() const
        {return lock != NULL;};

    /**
     * Release a held lock programmatically.  This can be used to de-reference
     * the object being share locked without having to wait for the
     * destructor to be called when the shared_lock falls out of scope.
     */
    void release(void);

    /**
     * Call exclusive access on referenced objects protocol.
     */
    void exclusive(void);

    /**
     * Restore shared access on referenced objects protocol.
     */
    void share(void);
};

/**
 * Convenience function to exclusively lock an object through it's protocol.
 * @param object to lock.
 */
inline void lock(ExclusiveAccess& object)
    {object.exclusive_lock();}

/**
 * Convenience function to unlock an exclusive object through it's protocol.
 * @param object to unlock.
 */
inline void unlock(ExclusiveAccess& object)
    {object.release_exclusive();}

/**
 * Convenience function to access (lock) shared object through it's protocol.
 * @param object to share lock.
 */
inline void access(SharedAccess& object)
    {object.shared_lock();}

/**
 * Convenience function to unlock shared object through it's protocol.
 * @param object to unlock.
 */
inline void release(SharedAccess& object)
    {object.release_share();}

/**
 * Convenience function to exclusive lock shared object through it's protocol.
 * @param object to exclusive lock.
 */
inline void exclusive(SharedAccess& object)
    {object.exclusive();}

/**
 * Convenience function to restore shared locking for object through it's protocol.
 * @param object to restore shared locking.
 */
inline void share(SharedAccess& object)
    {object.share();}

/**
 * Convenience type to use for object referencing an exclusive object.
 */
typedef exclusive_access exlock_t;

/**
 * Convenience type to use for object referencing a shared object.
 */
typedef shared_access shlock_t;

/**
 * Convenience function to release a reference to an exclusive lock.
 * @param reference to object referencing exclusive locked object.
 */
inline void release(exlock_t &reference)
    {reference.release();}

/**
 * Convenience function to release a reference to a shared lock.
 * @param reference to object referencing shared locked object.
 */
inline void release(shlock_t &reference)
    {reference.release();}

// Special macros to allow member functions of an object with a protocol
// to create self locking states while the member functions are called by
// placing an exclusive_lock or shared_lock smart object on their stack
// frame to reference their self.

#define exclusive_object()  exlock_t __autolock__ = this
#define protected_object()  shlock_t __autolock__ = this
#define exclusive_locking(x) exlock_t __autolock__ = &x
#define protected_locking(x) shlock_t __autolock__ = &x

END_NAMESPACE

#endif
