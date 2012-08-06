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
 * @file digest.h
 * @short Digest algorithms: checksum, CRC and MD5.
 **/

#ifndef CCXX_DIGEST_H_
#define CCXX_DIGEST_H_

#ifndef CCXX_MISSING_H_
#include <cc++/missing.h>
#endif

#ifndef CCXX_THREAD_H_
#include <cc++/thread.h>
#endif

#ifndef CCXX_EXCEPTION_H_
#include <cc++/exception.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

/**
 * The digest base class is used for implementing and deriving one way
 * hashing functions.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short base class for hashing services.
 */
class __EXPORT Digest : protected std::streambuf, public std::ostream
{
protected:
    Digest();

    /**
     * Get the size of a digest in octets.
     *
     * @return number of bytes in digest.
     */
    virtual unsigned getSize(void) = 0;

    /**
     * Copy the binary digest buffer to user memory.
     *
     * @return number of bytes in digest.
     * @param buffer to write into.
     */
    virtual unsigned getDigest(unsigned char *buffer) = 0;

    /**
     * Put data into the digest bypassing the stream subsystem.
     *
     * @param buffer to read from.
     * @param length of data.
     */
    virtual void putDigest(const unsigned char *buffer, unsigned length) = 0;

    /**
     * print a digest string for export.
     *
     * @return string representation of digest.
     */
    virtual std::ostream &strDigest(std::ostream &os) = 0;

    friend std::ostream &operator<<(std::ostream &os, Digest &ia)
        {return ia.strDigest(os);};

public:
    /**
     * Reset the digest table to an initial default value.
     */
    virtual void initDigest(void) = 0;

    virtual ~Digest();
};

/**
 * A simple checksum digest function.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short checksum hash function.
 */
class __EXPORT ChecksumDigest : public Digest
{
private:
    unsigned char csum;

protected:
    int overflow(int c);
    std::ostream &strDigest(std::ostream &os);

public:
    ChecksumDigest();

    void initDigest(void)
        {csum = 0;};

    unsigned getSize(void)
        {return 1;};

    unsigned getDigest(unsigned char *buffer);

    void putDigest(const unsigned char *buffer, unsigned length);
};

/**
 * A crc16 collection/compution hash accumulator class.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short crc16 computation hash.
 */
class __EXPORT CRC16Digest : public Digest
{
  private:
    uint16 crc16;

  protected:
    int overflow(int c);

    std::ostream &strDigest(std::ostream &os);

  public:
    CRC16Digest();

    CRC16Digest ( const CRC16Digest &crc );

    virtual ~CRC16Digest() {};

    inline void initDigest(uint16 crc) {crc16 = crc;};

    void initDigest(void) {initDigest(0);};

    inline unsigned getSize ( void )
    {return sizeof(crc16);};

    CRC16Digest& operator= ( const CRC16Digest &right );

    operator const uint16() const
    {return crc16;};

    inline uint16 getDigest(void)
    {return crc16;};

    unsigned getDigest ( unsigned char *buffer );

    void putDigest ( const unsigned char *buffer, unsigned length );

};

/**
 * A crc32 collection/computation hash accumulator class.
 *
 * @author Kevin Kraatz <kraatz@arlut.utexas.edu>
 * @short crc32 computation hash.
 */

class __EXPORT CRC32Digest : public Digest
{
  private:
    uint32 crc_table[256];
    uint32 crc_reg;
    uint32 crc32;

  protected:
    unsigned char overflow(unsigned char octet);

    std::ostream &strDigest(std::ostream &os);

  public:
    CRC32Digest();
    CRC32Digest(const CRC32Digest &crc);

    void initDigest(void);

    inline unsigned getSize(void) {return sizeof(crc32);}

    operator const uint32() const
    {return crc32;};

    inline uint32 getDigest(void)
    {return crc32;};

    unsigned getDigest(unsigned char *buffer);

    void putDigest(const unsigned char *buffer, unsigned length);

    CRC32Digest& operator= (const CRC32Digest &right);
};

/**
 * A md5 collection/computation accululator class.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short md5 hash accumulation.
 */
class __EXPORT MD5Digest : public Digest
{
private:
    unsigned long state[4];
    unsigned long count[2];
    unsigned char buf[64];
    unsigned bpos;
    unsigned char md5[16];
    bool updated;

protected:
    int overflow(int c);

    void update(void);

    void commit(void);

    std::ostream &strDigest(std::ostream &os);

public:
    MD5Digest();

    void initDigest(void);

    inline unsigned getSize(void)
        {return 16;};

    unsigned getDigest(unsigned char *buffer);

    void putDigest(const unsigned char *buffer, unsigned len);
};

#ifdef  COMMON_STD_EXCEPTION
/**
 * DigestException
 *
 * Exception classes that pertain to errors when making or otherwise
 * working with digests.
 *
 * @author Elizabeth Barham <lizzy@soggytrousers.net>
 * @short Exceptions involving digests.
 */
class __EXPORT DigestException : public Exception {
public:
    DigestException(const String &str) : Exception(str) {};
};
#endif

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

