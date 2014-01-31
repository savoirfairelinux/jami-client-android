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
 * Runtime functions.  This includes common runtime library functions we
 * may need portably.
 * @file ucommon/cpr.h
 * @author David Sugar <dyfet@gnutelephony.org>
 */

#ifndef _UCOMMON_CONFIG_H_
#include <ucommon/platform.h>
#endif

#ifndef _UCOMMON_CPR_H_
#define _UCOMMON_CPR_H_

#ifdef  _MSWINDOWS_

extern "C" {
    __EXPORT int cpr_setenv(const char *s, const char *v, int p);

    inline int setenv(const char *s, const char *v, int overwrite)
        {return cpr_setenv(s, v, overwrite);}
}

#endif


/**
 * Function to handle runtime errors.  When using the standard C library,
 * runtime errors are handled by a simple abort.  When using the stdc++
 * library with stdexcept, then std::runtime_error will be thrown.
 * @param text of runtime error.
 */
__EXPORT void cpr_runtime_error(const char *text);

/**
 * Portable memory allocation helper function.  Handles out of heap error
 * as a runtime error.
 * @param size of memory block to allocate from heap.
 * @return memory address of allocated heap space.
 */
extern "C" __EXPORT void *cpr_memalloc(size_t size) __MALLOC;

/**
 * Portable memory placement helper function.  This is used to process
 * "placement" new operators where a new object is constructed over a
 * pre-allocated area of memory.  This handles invalid values through
 * runtime error.
 * @param size of object being constructed.
 * @param address where the object is being placed.
 * @param known size of the location we are constructing the object in.
 */
extern "C" __EXPORT void *cpr_memassign(size_t size, caddr_t address, size_t known) __MALLOC;

/**
 * Portable swap code.
 * @param mem1 to swap.
 * @param mem2 to swap.
 * @param size of swap area.
 */
extern "C" __EXPORT void cpr_memswap(void *mem1, void *mem2, size_t size);

#ifndef _UCOMMON_EXTENDED_
/**
 * Our generic new operator.  Uses our heap memory allocator.
 * @param size of object being constructed.
 * @return memory allocated from heap.
 */
inline void *operator new(size_t size)
    {return cpr_memalloc(size);}

/**
 * Our generic new array operator.  Uses our heap memory allocator.
 * @param size of memory needed for object array.
 * @return memory allocated from heap.
 */
inline void *operator new[](size_t size)
    {return cpr_memalloc(size);}
#endif

#ifndef _UCOMMON_EXTENDED_
/**
 * A placement new array operator where we assume the size of memory is good.
 * We construct the array at a specified place in memory which we assume is
 * valid for our needs.
 * @param size of memory needed for object array.
 * @param address where to place object array.
 * @return memory we placed object array.
 */
inline void *operator new[](size_t size, caddr_t address)
    {return cpr_memassign(size, address, size);}

/**
 * A placement new array operator where we know the allocated size.  We
 * find out how much memory is needed by the new and can prevent arrayed
 * objects from exceeding the available space we are placing the object.
 * @param size of memory needed for object array.
 * @param address where to place object array.
 * @param known size of location we are placing array.
 * @return memory we placed object array.
 */
inline void *operator new[](size_t size, caddr_t address, size_t known)
    {return cpr_memassign(size, address, known);}
#endif

/**
 * Overdraft new to allocate extra memory for object from heap.  This is
 * used for objects that must have a known class size but store extra data
 * behind the class.  The last member might be an unsized or 0 element
 * array, and the actual size needed from the heap is hence not the size of
 * the class itself but is known by the routine allocating the object.
 * @param size of object.
 * @param extra heap space needed for data.
 */
inline void *operator new(size_t size, size_t extra)
    {return cpr_memalloc(size + extra);}

/**
 * A placement new operator where we assume the size of memory is good.
 * We construct the object at a specified place in memory which we assume is
 * valid for our needs.
 * @param size of memory needed for object.
 * @param address where to place object.
 * @return memory we placed object.
 */
inline void *operator new(size_t size, caddr_t address)
    {return cpr_memassign(size, address, size);}

/**
 * A placement new operator where we know the allocated size.  We
 * find out how much memory is needed by the new and can prevent the object
 * from exceeding the available space we are placing the object.
 * @param size of memory needed for object.
 * @param address where to place object.
 * @param known size of location we are placing object.
 * @return memory we placed object.
 */

inline void *operator new(size_t size, caddr_t address, size_t known)
    {return cpr_memassign(size, address, known);}

#ifndef _UCOMMON_EXTENDED_


#ifdef  __GNUC__
extern "C" __EXPORT void __cxa_pure_virtual(void);
#endif
#endif

extern "C" {
    __EXPORT uint16_t lsb_getshort(uint8_t *b);
    __EXPORT uint32_t lsb_getlong(uint8_t *b);
    __EXPORT uint16_t msb_getshort(uint8_t *b);
    __EXPORT uint32_t msb_getlong(uint8_t *b);

    __EXPORT void lsb_setshort(uint8_t *b, uint16_t v);
    __EXPORT void lsb_setlong(uint8_t *b, uint32_t v);
    __EXPORT void msb_setshort(uint8_t *b, uint16_t v);
    __EXPORT void msb_setlong(uint8_t *b, uint32_t v);
}

#endif
