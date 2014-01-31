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
 * Generic templates for C++.  These are templates that do not depend
 * on any ucommon classes.  They can be used for generic C++ programming.
 * @file ucommon/generics.h
 */

#ifndef _UCOMMON_GENERICS_H_
#define _UCOMMON_GENERICS_H_

#ifndef _UCOMMON_CPR_H_
#include <ucommon/cpr.h>
#endif

#include <cstdlib>
#include <string.h>

#ifdef  NEW_STDLIB
#include <stdexcept>
#endif

#if defined(NEW_STDLIB) || defined(OLD_STDLIB)
#define THROW(x)    throw x
#define THROWS(x)   throw(x)
#define THROWS_ANY  throw()
#else
#define THROW(x)    ::abort()
#define THROWS(x)
#define THROWS_ANY
#endif

NAMESPACE_UCOMMON

/**
 * Generic smart pointer class.  This is the original Common C++ "Pointer"
 * class with a few additions.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <typename T>
class pointer
{
protected:
    unsigned *counter;
    T *object;

public:
    inline void release(void) {
        if(counter && --(*counter)==0) {
            delete counter;
            delete object;
        }
        object = NULL;
        counter = NULL;
    }

    inline void retain(void) {
        if(counter)
            ++*counter;
    }

    inline void set(T* ptr) {
        if(object != ptr) {
            release();
            counter = new unsigned;
            *counter = 1;
            object = ptr;
        }
    }

    inline void set(const pointer<T> &ref) {
        if(object == ref.object)
            return;

        if(counter && --(*counter)==0) {
            delete counter;
            delete object;
        }
        object = ref.object;
        counter = ref.counter;
        if(counter)
            ++(*counter);
    }

    inline pointer() {
        counter = NULL;
        object = NULL;
    }

    inline explicit pointer(T* ptr = NULL) : object(ptr) {
        if(object) {
            counter = new unsigned;
            *counter = 1;
        }
        else
            counter = NULL;
    }

    inline pointer(const pointer<T> &ref) {
        object = ref.object;
        counter = ref.counter;
        if(counter)
            ++(*counter);
    }

    inline pointer& operator=(const pointer<T> &ref) {
        this->set(ref);
        return *this;
    }

    inline pointer& operator=(T *ptr) {
        this->set(ptr);
        return *this;
    }

    inline ~pointer()
        {release();}

    inline T& operator*() const
        {return *object;};

    inline T* operator->() const
        {return object;};

    inline bool operator!() const
        {return (counter == NULL);};

    inline operator bool() const
        {return counter != NULL;};
};

/**
 * Generic smart array class.  This is the original Common C++ "Pointer" class
 * with a few additions for arrays.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <typename T>
class array_pointer
{
protected:
    unsigned *counter;
    T *array;

public:
    inline void release(void) {
        if(counter && --(*counter)==0) {
            delete counter;
            delete[] array;
        }
        array = NULL;
        counter = NULL;
    }

    inline void retain(void) {
        if(counter)
            ++*counter;
    }

    inline void set(T* ptr) {
        if(array != ptr) {
            release();
            counter = new unsigned;
            *counter = 1;
            array = ptr;
        }
    }

    inline void set(const array_pointer<T> &ref) {
        if(array == ref.array)
            return;

        if(counter && --(*counter)==0) {
            delete counter;
            delete[] array;
        }
        array = ref.array;
        counter = ref.counter;
        if(counter)
            ++(*counter);
    }

    inline array_pointer() {
        counter = NULL;
        array = NULL;
    }

    inline explicit array_pointer(T* ptr = NULL) : array(ptr) {
        if(array) {
            counter = new unsigned;
            *counter = 1;
        }
        else
            counter = NULL;
    }

    inline array_pointer(const array_pointer<T> &ref) {
        array = ref.array;
        counter = ref.counter;
        if(counter)
            ++(*counter);
    }

    inline array_pointer& operator=(const array_pointer<T> &ref) {
        this->set(ref);
        return *this;
    }

    inline array_pointer& operator=(T *ptr) {
        this->set(ptr);
        return *this;
    }

    inline ~array_pointer()
        {release();}

    inline T* operator*() const
        {return array;};

    inline T& operator[](size_t offset) const
        {return array[offset];};

    inline T* operator()(size_t offset) const
        {return &array[offset];};

    inline bool operator!() const
        {return (counter == NULL);};

    inline operator bool() const
        {return counter != NULL;};
};

/**
 * Manage temporary object stored on the heap.  This is used to create a
 * object on the heap who's scope is controlled by the scope of a member
 * function call.  Sometimes we have data types and structures which cannot
 * themselves appear as auto variables.  We may also have a limited stack
 * frame size in a thread context, and yet have a dynamic object that we
 * only want to exist during the life of the method call.  Using temporary
 * allows any type to be created from the heap but have a lifespan of a
 * method's stack frame.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <typename T>
class temporary
{
protected:
    T *object;
public:
    /**
     * Construct a temporary object, create our stack frame reference.
     */
    inline temporary()
        {object = NULL;};

    /**
     * Disable copy constructor.
     */
    temporary(const temporary<T>&)
        {::abort();};

    /**
     * Construct an assigned pointer.
     */
    inline temporary(T *ptr)
        {object = ptr;};

    /**
     * Assign a temporary object.  This adds a pointer to an existing
     * type to the current temporary pointer.  If the temporary was
     * already assigned, then it is deleted.
     * @param temp object to assign.
     */
    inline T& operator=(T *temp) {
        if(object)
            delete object;
        object = temp;
        return *this;
    }

    /**
     * Assign a temporary object.  This adds a pointer to an existing
     * type to the current temporary pointer.  If the temporary was
     * already assigned, then it is deleted.
     * @param temp object to assign.
     */
    inline void set(T *temp) {
        if(object)
            delete object;
        object = temp;
    }

    /**
     * Access heap object through our temporary directly.
     * @return reference to heap resident object.
     */
    inline T& operator*() const
        {return *object;};

    /**
     * Access members of our heap object through our temporary.
     * @return member reference of heap object.
     */
    inline T* operator->() const
        {return object;};

    inline operator bool() const
        {return object != NULL;};

    inline bool operator!() const
        {return object == NULL;};

    inline ~temporary() {
        if(object)
            delete object;
        object = NULL;
    }
};

/**
 * Manage temporary array stored on the heap.   This is used to create an
 * array on the heap who's scope is controlled by the scope of a member
 * function call.  Sometimes we have data types and structures which cannot
 * themselves appear as auto variables.  We may also have a limited stack
 * frame size in a thread context, and yet have a dynamic object that we
 * only want to exist during the life of the method call.  Using temporary
 * allows any type to be created from the heap but have a lifespan of a
 * method's stack frame.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <typename T>
class temp_array
{
protected:
    T *array;
    size_t size;

public:
    /**
     * Construct a temporary object, create our stack frame reference.
     */
    inline temp_array(size_t s)
        {array =  new T[s]; size = s;};

    /**
     * Construct a temporary object with a copy of some initial value.
     * @param initial object value to use.
     */
    inline temp_array(const T& initial, size_t s) {
        array = new T[s];
        size = s;
        for(size_t p = 0; p < s; ++p)
            array[p] = initial;
    }

    inline void reset(size_t s)
        {delete[] array; array = new T[s]; size = s;};

    inline void reset(const T& initial, size_t s) {
        if(array)
            delete[] array;
        array = new T[s];
        size = s;
        for(size_t p = 0; p < s; ++p)
            array[p] = initial;
    }

    inline void set(const T& initial) {
        for(size_t p = 0; p < size; ++p)
            array[p] = initial;
    }

    /**
     * Disable copy constructor.
     */
    temp_array(const temp_array<T>&)
        {::abort();};

    inline operator bool() const
        {return array != NULL;};

    inline bool operator!() const
        {return array == NULL;};

    inline ~temp_array() {
        if(array)
            delete[] array;
        array = NULL;
        size = 0;
    }

    inline T& operator[](size_t offset) const {
        crit(offset < size, "array out of bound");
        return array[offset];
    }

    inline T* operator()(size_t offset) const {
        crit(offset < size, "array out of bound");
        return &array[offset];
    }
};

/**
 * Save and restore global objects in function call stack frames.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<typename T>
class save_restore
{
private:
    T *original;
    T temp;

public:
    /**
     * Save object into local copy and keep reference to the original object.
     * @param object to save.
     */
    inline save_restore(T& object)
        {original = &object; temp = object;};

    /**
     * Restore original when stack frame is released.
     */
    inline ~save_restore()
        {*original = temp;};
};

/**
 * Convenience function to validate object assuming it is castable to bool.
 * @param object we are testing.
 * @return true if object valid.
 */
template<class T>
inline bool is(T& object)
    {return object.operator bool();}

/**
 * Convenience function to test pointer object.  This solves issues where
 * some compilers get confused between bool and pointer operators.
 * @param object we are testing.
 * @return true if object points to NULL.
 */
template<typename T>
inline bool isnull(T& object)
    {return (bool)(object.operator*() == NULL);}

/**
 * Convenience function to test pointer-pointer object.  This solves issues
 * where some compilers get confused between bool and pointer operators.
 * @param object we are testing.
 * @return true if object points to NULL.
 */
template<typename T>
inline bool isnullp(T *object)
    {return (bool)(object->operator*() == NULL);}

/**
 * Convenience function to duplicate object pointer to heap.
 * @param object we are duping.
 * @return heap pointer instance.
 */
template<typename T>
inline T* dup(const T& object)
    {return new T(object);}

template<typename T>
inline void dupfree(T object)
    {delete object;}

template<>
inline char *dup<char>(const char& object)
    {return strdup(&object);}

template<>
inline void dupfree<char*>(char* object)
    {::free(object);}

/**
 * Convenience function to reset an existing object.
 * @param object type to reset.
 */
template<typename T>
inline void reset_unsafe(T& object)
    {new((caddr_t)&object) T;}

/**
 * Convenience function to zero an object and restore type info.
 * @param object to zero in memory.
 */
template<typename T>
inline void zero_unsafe(T& object)
    {memset((void *)&object, 0, sizeof(T)); new((caddr_t)&object) T;}

/**
 * Convenience function to copy class.
 * @param target to copy into.
 * @param source to copy from.
 */
template<typename T>
inline void copy_unsafe(T* target, const T* source)
    {memcpy((void *)target, (void *)source, sizeof(T));}

/**
 * Convenience function to store object pointer into object.
 * @param target to copy into.
 * @param source to copy from.
 */
template<typename T>
inline void store_unsafe(T& target, const T* source)
    {memcpy((void *)&target, (void *)source, sizeof(T));}

/**
 * Convenience function to swap objects.
 * @param o1 to swap.
 * @param o2 to swap.
 */
template<typename T>
inline void swap(T& o1, T& o2)
    {cpr_memswap(&o1, &o2, sizeof(T));}

/**
 * Convenience function to return max of two objects.
 * @param o1 to check.
 * @param o2 to check.
 * @return max object.
 */
template<typename T>
inline T& (max)(T& o1, T& o2)
{
    return o1 > o2 ? o1 : o2;
}

/**
 * Convenience function to return min of two objects.
 * @param o1 to check.
 * @param o2 to check.
 * @return min object.
 */
template<typename T>
inline T& (min)(T& o1, T& o2)
{
    return o1 < o2 ? o1 : o2;
}

/**
 * Convenience macro to range restrict values.
 * @param value to check.
 * @param low value.
 * @param high value.
 * @return adjusted value.
 */
template<typename T>
inline T& (limit)(T& value, T& low, T& high)
{
    return (value < low) ? low : ((value > high) ? high : value);
}

END_NAMESPACE

#endif
