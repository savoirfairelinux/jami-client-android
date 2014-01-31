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
 * Support for various automatic counting objects.
 * This header defines templates for various kinds of automatic counting
 * and sequencing objects.  Templates are used to allow manipulation of
 * various numerical-like types.
 * @file ucommon/counter.h
 */

#ifndef _UCOMMON_COUNTER_H_
#define _UCOMMON_COUNTER_H_

#ifndef _UCOMMON_CONFIG_H_
#include <ucommon/platform.h>
#endif

NAMESPACE_UCOMMON

/**
 * Automatic integer counting class.  This is an automatic counting object
 * that is used to retrieve a new integer value between 0 and n each time
 * the object is referenced.  When reaching the last n value, the object
 * restarts at 0, and so is used to retrieve a sequence of values in order.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT counter
{
private:
    unsigned value, cycle;

public:
    /**
     * Initialize integer counter of unknown size.
     */
    counter();

    /**
     * Initialize integer counter for a range of values.
     * @param limit before recycling to zero.
     */
    counter(unsigned limit);

    /**
     * Get the next counter value.
     * @return next counter value.
     */
    unsigned get(void);

    /**
     * Get the range of values before recycling.
     * @return counter limit.
     */
    inline unsigned range(void)
        {return cycle;};

    /**
     * Reference next counter value through pointer operation.
     * @return next counter value.
     */
    inline unsigned operator*()
        {return get();};

    /**
     * Reference next counter value by casting to integer.
     * @return next counter value.
     */
    inline operator unsigned()
        {return get();};

    /**
     * Assign the value of the counter.
     * @param value to assign.
     */
    void operator=(unsigned value);
};

/**
 * Automatically return a sequence of untyped objects.  This is an automatic
 * counter based class which returns the next pointer in an array of pointers
 * and restarts the list when reaching the end.  This is used to support the
 * sequence template.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT SeqCounter : protected counter
{
private:
    void *item;
    size_t offset;

protected:
    SeqCounter(void *start, size_t size, unsigned count);

    void *get(void);

    void *get(unsigned idx);

public:
    /**
     * Used to directly assign sequence position in template.
     * @param inc_offset in sequence to reset sequencing to.
     */
    inline void operator=(unsigned inc_offset)
        {counter::operator=(inc_offset);};
};

/**
 * Automatically toggle a bool on each reference.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT toggle
{
private:
    bool value;

public:
    inline toggle()
        {value = false;};

    bool get(void);

    inline bool operator*()
        {return get();};

    inline void operator=(bool v)
        {value = v;};

    inline operator bool()
        {return get();};

};

/**
 * A template to return a sequence of objects of a specified type.
 * This is used to return a different member in a sequence of objects of
 * a specified type during each reference to the sequencer.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <class T>
class sequence : public SeqCounter
{
protected:
    inline T *get(unsigned idx)
        {return static_cast<T *>(SeqCounter::get(idx));};

public:
    /**
     * Create a template auto-sequence from a list of typed pointers.
     * @param array of typed values to sequence on reference.
     * @param size of list of typed values.
     */
    inline sequence(T *array, unsigned size) :
        SeqCounter(array, sizeof(T), size) {};

    /**
     * Return next typed member of the sequence.
     * @return next typed member of sequence.
     */
    inline T* get(void)
        {return static_cast<T *>(SeqCounter::get());};

    /**
     * Return next typed member of the sequence by pointer reference.
     * @return next typed member of sequence.
     */
    inline T& operator*()
        {return *get();};

    /**
     * Return next typed member of the sequence by casted reference.
     * @return next typed member of sequence.
     */
    inline operator T&()
        {return *get();};

    /**
     * Return a specific typed member from the sequence list.
     * @param offset of member to return.
     * @return typed value at the specified offset.
     */
    inline T& operator[](unsigned offset)
        {return *get(offset);};
};

/**
 * A convenience typecast for integer counters.
 */
typedef counter counter_t;

/**
 * A convenience typecast for auto-toggled bools.
 */
typedef toggle toggle_t;

END_NAMESPACE

#endif
