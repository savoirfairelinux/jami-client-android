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
 * @file counter.h
 * @short Generic automatic counter data type template class.
 **/

#ifndef CCXX_COUNTER_H_
#define CCXX_COUNTER_H_

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

/**
 * The counter template is used for generic objects which act as
 * automatic counters.  Each time the object is accessed, the underlying
 * counted data type is incremented.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Automatic counter template class.
 */
template <typename T>
class Counter
{
protected:
    T count;

public:
    /**
     * Construct and initialize a counter to zero.
     */
    inline Counter()
        {count = 0;};

    /**
     * Construct a counter with an initial value set for another counter.
     *
     * @param counter to copy from.
     */
    inline Counter(const Counter &counter)
        {count = counter.count;};

    /**
     * Construct a counter with an initial value of the specified
     * data type.
     *
     * @param initial value to set.
     */
    inline Counter(T initial)
        {count = initial;};

    inline T& operator=(T c)
        {counter = c;};

    inline operator T()
        {return count++;}
};

#ifdef  CCXX_NAMESPACES
} // namespace
#endif

#endif

