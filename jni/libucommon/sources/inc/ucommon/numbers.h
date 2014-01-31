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
 * Support classes for manipulation of numbers as strings.  This is
 * used for things which parse numbers out of strings, such as in the
 * date and time classes.  Other useful math related functions, templates,
 * and macros may also be found here.
 * @file ucommon/numbers.h
 */

#ifndef _UCOMMON_NUMBERS_H_
#define _UCOMMON_NUMBERS_H_

#ifndef _UCOMMON_CONFIG_H_
#include <ucommon/platform.h>
#endif

NAMESPACE_UCOMMON

/**
 * A number manipulation class.  This is used to extract, convert,
 * and manage simple numbers that are represented in C ascii strings
 * in a very quick and optimal way.  This class modifies the string
 * representation each time the value is changed.  No math expressions
 * or explicit comparison operators are supported for the Numbers class
 * because these are best done by casting to long first.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short number manipulation.
 */
class __EXPORT Number
{
protected:
    char *buffer;
    unsigned size;

public:
    /**
     * Create an instance of a number.
     * @param buffer or NULL if created internally.
     * @param size of field if not null terminated.
     */
    Number(char *buffer, unsigned size);

    /**
     * Set string based on a new value.
     * @param value to set.
     */
    void set(long value);

    /**
     * Get string buffer representing the number.
     * @return string buffer.
     */
    inline const char *c_str() const
        {return buffer;};

    /**
     * Get value of string buffer as a long integer.
     * @return long integer value of string buffer.
     */
    long get() const;

    /**
     * Get value of string buffer as expression of object.
     * @return long integer value of string buffer.
     */
    inline long operator()()
        {return get();};

    /**
     * Cast string as long integer and get value of buffer.
     * @return long integer value of string buffer.
     */
    inline operator long()
        {return get();};

    /**
     * Cast object as a string to retrieve buffer.
     * @return string buffer of value.
     */
    inline operator char*()
        {return buffer;};

    /**
     * Assign a value to the number.  This rewrites the string buffer.
     * @param value to assign.
     * @return new value of number object assigned.
     */
    long operator=(long value);

    /**
     * Assign another number to this number.
     * @param number to assign to assign.
     * @return new value of number object assigned.
     */
    long operator=(const Number& number);

    /**
     * Add a value to the number.  This rewrites the string buffer.
     * @param value to add.
     * @return new value of number object.
     */
    long operator+=(const long value);

    /**
     * Subtract a value from the number.  This rewrites the string buffer.
     * @param value to subtract.
     * @return new value of number object.
     */
    long operator-=(const long value);

    /**
     * Decrement the number object.  This rewrites the string buffer.
     * @return new value of number object.
     */
    long operator--();

    /**
     * Increment the number object.  This rewrites the string buffer.
     * @return new value of number object.
     */
    long operator++();

    inline bool operator==(const long value) const
        {return get() == value;}

    inline bool operator!=(const long value) const
        {return get() != value;}

    inline bool operator<(const long value) const
        {return get() < value;}

    inline bool operator>(const long value) const
        {return get() > value;}

    inline bool operator<=(const long value) const
        {return get() <= value;}

    inline bool operator>=(const long value) const
        {return get() >= value;}
};

/**
 * A number manipulation class that maintains a zero lead filled string.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short zero filled number manipulation.
 */
class __EXPORT ZNumber : public Number
{
public:
    /**
     * Create a number class for zero fill.
     * @param pointer to field.
     * @param size of field to fill.
     */
    ZNumber(char *pointer, unsigned size);

    /**
     * Set value of zero filled number.
     * @param value to set.
     */

    void set(long value);

    /**
     * Assign number from value.
     * @param value to assign.
     * @return value assigned.
     */
    long operator=(long value);
};

/**
 * A convenience type for number.
 */
typedef Number  number_t;

/**
 * A convenience type for znumber.
 */
typedef ZNumber znumber_t;

/**
 * Template for absolute value of a type.
 * @param value to check
 * @return absolute value
 */
template<typename T>
inline const T abs(const T& value)
{
    if(value < (T)0)
        return -value;
    return value;
}


/**
 * Template for min value of a type.
 * @param v1 value to check
 * @param v2 value to check
 * @return v1 if < v2, else v2
 */
template<typename T>
inline const T (min)(const T& v1, const T& v2)
{
    return ((v1 < v2) ? v1 : v2);
}

/**
 * Template for max value of a type.
 * @param v1 value to check
 * @param v2 value to check
 * @return v1 if > v2, else v2
 */
template<typename T>
inline const T (max)(const T& v1, const T& v2)
{
    return ((v1 > v2) ? v1 : v2);
}

END_NAMESPACE

#endif
