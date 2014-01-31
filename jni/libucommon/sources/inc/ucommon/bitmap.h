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
 * A simple class to perform bitmap manipulation.
 * Bitmaps are used to manage bit-aligned objects, such as network cidr
 * addresses.  This header introduces a common bitmap management class
 * for the ucommon library.
 * @file ucommon/bitmap.h
 */

#ifndef _UCOMMON_BITMAP_H_
#define _UCOMMON_BITMAP_H_

#ifndef _UCOMMON_CONFIG_H_
#include <ucommon/platform.h>
#endif

NAMESPACE_UCOMMON

/**
 * A class to access bit fields in external bitmaps.  The actual bitmap this
 * object manipulates may not be stored in the object.  Bitmaps may be
 * referenced on special memory mapped or i/o bus based devices or other
 * structures that have varying data word sizes which may differ from the
 * default cpu bus size.  The bitmap class can be set to the preferred memory
 * bus size of the specific external bitmap being used.  Bitmap size may also
 * be relevant when accessing individual bits in memory mapped device registers
 * where performing reference and manipulations may change the state of the
 * device and hence must be aligned with the device register being effected.
 *
 * This class offers only the most basic bit manipulations, getting and
 * setting individual bits in the bitmap.  More advanced bit manipulations
 * and other operations can be created in derived classes.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT bitmap
{
protected:
    size_t size;

    typedef union
    {
        void *a;
        uint8_t *b;
        uint16_t *w;
        uint32_t *l;
        uint64_t *d;
    }   addr_t;

    addr_t addr;

public:
    /**
     * Specify data word size to use in accessing a bitmap.
     */
    typedef enum {
        BMALLOC,    /**< Use default cpu size */
        B8,         /**< Accessing a bitmap on 8 bit bus device */
        B16,        /**< Accessing a bitmap on a 16 bit device */
        B32,        /**< Accessing a bitmap on a 32 bit device */
        B64,        /**< Accessing a bitmap on a 64 bit device */
        BMIN = BMALLOC,
        BMAX = B64
    } bus_t;

protected:
    bus_t bus;

    unsigned memsize(void) const;

public:
    /**
     * Create an object to reference the specified bitmap.
     * @param addr of the bitmap in mapped memory.
     * @param length of the bitmap being accessed in bits.
     * @param size of the memory bus or manipulation to use.
     */
    bitmap(void *addr, size_t length, bus_t size = B8);

    /**
     * Create a bitmap to manipulate locally.  This bitmap is created
     * as part of the object itself, and uses the BMALLOC bus mode.
     * @param length of bitmap to create in bits.
     */
    bitmap(size_t length);

    /**
     * Destroy bitmap manipulation object.  If a bitmap was locally
     * created with the alternate constructor, that bitmap will also be
     * removed from memory.
     */
    ~bitmap();

    /**
     * Clear (zero) all the bits in the bitmap.
     */
    void clear(void);

    /**
     * Get the value of a "bit" in the bitmap.
     * @param offset to bit in map to get.
     * @return true if bit is set.
     */
    bool get(size_t offset) const;

    /**
     * Set an individual bit in the bitmask.
     * @param offset to bit in map to change.
     * @param value to change specified bit to.
     */
    void set(size_t offset, bool value);
};

END_NAMESPACE

#endif
