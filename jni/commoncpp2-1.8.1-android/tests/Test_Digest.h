// Copyright (C) 2002-2003 Chad C. Yates cyates@uidaho.edu
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

#include <cppunit/extensions/HelperMacros.h>
#include <cc++/digest.h>
#include <fstream>

using namespace ost;

/**
 * Template class that provides common tests and data to excercise
 * the digest classes.  Provides common messages to run through the digests.
 * Sub-classes of this Fixture provide what tests to run and what the expected
 * digest strings are, as well as any other tests that are specific to that digest
 * type.  see the pre-defined subclasses for examples
 *
 * @author Chad C. Yates
 */
template<class DigestTypeClass>
class GenericDigestTests : public CppUnit::TestFixture
{
private:
  enum {BINARY_DATA1_SIZE = 256};  // constant trick
  unsigned char binaryData1[BINARY_DATA1_SIZE];

protected:
  DigestTypeClass digest;
  std::stringstream digestOutput;

  GenericDigestTests() {
	  // generate some binary data
	  for(unsigned int i = 0; i< BINARY_DATA1_SIZE; ++i)
		binaryData1[i] = i % 256; // repeating pattern of ascending bytes

	  // output it to a file in case it needs to be checked against an external program
	  std::ofstream file("BinaryData1.dat", std::ios::out|std::ios::binary);
	  file.write((char *) binaryData1, sizeof(binaryData1));
	  file.close();
	}

  virtual ~GenericDigestTests()
	{}

  std::string getEmptyMsg()
	{ return ""; }

  std::string getMsg1()
	{ return "pippo"; }

  std::string getLongMsg1()
	{ return "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
	  "blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah";
	}
  unsigned char* getBinaryData1()
  { return binaryData1; }

  // These define the textual digests that should be the result of processing
  // the corresponding message/data and exporting to a stream
  virtual std::string getEmptyMsg_Digest() { return ""; }
  virtual std::string getMsg1_Digest() { return ""; }
  virtual std::string getLongMsg1_Digest() { return ""; }
  virtual std::string getBinaryData1_Digest() { return ""; }

public:
  /**
   * Test case for Digest
   *
   * @author Chad C. Yates
   */
  void testTypicalUsage();

  /**
   * Test case for Digest
   *
   * @author Chad C. Yates
   */
  void testConstruction();

  /**
   * Test case for Digest
   *
   * @author Chad C. Yates
   */
  void testEmptyData();

  /**
   * Test case for Digest
   *
   * @author Chad C. Yates
   */
  void testSmallMessage();

  /**
   * Test case for Digest
   *
   * @author Chad C. Yates
   */
  void testLargeMessage();

  /**
   * Test case for Digest
   *
   * @author Chad C. Yates
   */
  void testBinaryData();

  /**
   * Test case for Digest
   *
   * @author Chad C. Yates
   */
  void testReInitialization();

  /**
   * Test case for Digest
   * attempts to excercise the putDigest method by giving it a long string a piece at a time.
   *
   * @author Chad C. Yates
   */
  void testPiecewise();
};

/**
 * Subclass of the generic digest tests that specifies the expected digest
 * strings and tests to run for the Checksum digest class.
 */
class ChecksumDigestTest : public GenericDigestTests<ChecksumDigest>
{
  CPPUNIT_TEST_SUITE(ChecksumDigestTest);
  CPPUNIT_TEST(testTypicalUsage);
  CPPUNIT_TEST(testConstruction);
  CPPUNIT_TEST(testEmptyData);
  CPPUNIT_TEST(testSmallMessage);
  CPPUNIT_TEST(testLargeMessage);
  CPPUNIT_TEST(testBinaryData);
  CPPUNIT_TEST(testReInitialization);
  CPPUNIT_TEST(testPiecewise);
  CPPUNIT_TEST_SUITE_END();

public:
  std::string getEmptyMsg_Digest()
	{ return "00"; }
  std::string getMsg1_Digest()
	{ return "28"; }
  std::string getLongMsg1_Digest()
	{ return "29"; }
  std::string getBinaryData1_Digest()
	{ return "80"; }
};

/**
 * Subclass of the generic digest tests that specifies the expected digest
 * strings and tests to run for the CRC16 digest class.
 */
class CRC16DigestTest : public GenericDigestTests<CRC16Digest>
{
  CPPUNIT_TEST_SUITE(CRC16DigestTest);
  CPPUNIT_TEST(testTypicalUsage);
  CPPUNIT_TEST(testConstruction);
  CPPUNIT_TEST(testEmptyData);
  CPPUNIT_TEST(testSmallMessage);
  CPPUNIT_TEST(testLargeMessage);
  CPPUNIT_TEST(testBinaryData);
  CPPUNIT_TEST(testReInitialization);
  CPPUNIT_TEST(testPiecewise);
  CPPUNIT_TEST_SUITE_END();

public:
  std::string getEmptyMsg_Digest()
	{ return "0000"; }
  std::string getMsg1_Digest()
	{ return "fa3b"; }
  std::string getLongMsg1_Digest()
	{ return "54cf"; }
  std::string getBinaryData1_Digest()
	{ return "7e55"; }
};

/**
 * Subclass of the generic digest tests that specifies the expected digest
 * strings and tests to run for the CRC32 digest class.
 */
class CRC32DigestTest : public GenericDigestTests<CRC32Digest>
{
  CPPUNIT_TEST_SUITE(CRC32DigestTest);
  CPPUNIT_TEST(testTypicalUsage);
  CPPUNIT_TEST(testConstruction);
  CPPUNIT_TEST(testEmptyData);
  CPPUNIT_TEST(testSmallMessage);
  CPPUNIT_TEST(testLargeMessage);
  CPPUNIT_TEST(testBinaryData);
  CPPUNIT_TEST(testReInitialization);
  CPPUNIT_TEST(testPiecewise);
  CPPUNIT_TEST_SUITE_END();

public:
  virtual std::string getEmptyMsg_Digest()
	{ return "00000000"; }
  virtual std::string getMsg1_Digest()
	{ return "df1dc234"; }
  virtual std::string getLongMsg1_Digest()
	{ return "2d69085d"; }
  virtual std::string getBinaryData1_Digest()
	{ return "29058c73"; }
};

/**
 * Subclass of the generic digest tests that specifies the expected digest
 * strings and tests to run for the MD5 digest class.
 */
class MD5DigestTest : public GenericDigestTests<MD5Digest>
{
  CPPUNIT_TEST_SUITE(MD5DigestTest);
  CPPUNIT_TEST(testTypicalUsage);
  CPPUNIT_TEST(testConstruction);
  CPPUNIT_TEST(testEmptyData);
  CPPUNIT_TEST(testSmallMessage);
  CPPUNIT_TEST(testLargeMessage);
  CPPUNIT_TEST(testBinaryData);
  CPPUNIT_TEST(testReInitialization);
  CPPUNIT_TEST(testPiecewise);
  CPPUNIT_TEST_SUITE_END();

public:
  virtual std::string getEmptyMsg_Digest()
	{ return "d41d8cd9" "8f00b204" "e9800998" "ecf8427e"; }
  virtual std::string getMsg1_Digest()
	{ return "0c88028b" "f3aa6a6a" "143ed846" "f2be1ea4"; }
  virtual std::string getLongMsg1_Digest()
	{ return "7588d0eb" "c99997f3" "a4284b11" "6ac117b5"; }
  virtual std::string getBinaryData1_Digest()
	{ return "e2c865db" "4162bed9" "63bfaa9e" "f6ac18f0"; }
};

/**
 * Subclass of the generic digest tests that specifies the expected digest
 * strings and tests to run for the SHA1 digest class.
 */
class SHA1DigestTest : public GenericDigestTests<SHA1Digest>
{
  CPPUNIT_TEST_SUITE(SHA1DigestTest);
  //CPPUNIT_TEST(testTypicalUsage);
  //CPPUNIT_TEST(testConstruction);
  //CPPUNIT_TEST(testEmptyData);
  //CPPUNIT_TEST(testSmallMessage);
  //CPPUNIT_TEST(testLargeMessage);
  //CPPUNIT_TEST(testBinaryData);
  //CPPUNIT_TEST(testReInitialization);
  //CPPUNIT_TEST(testPiecewise);
  CPPUNIT_TEST_SUITE_END();

public:
  virtual std::string getEmptyMsg_Digest()
	{ return ""; }
  virtual std::string getMsg1_Digest()
	{ return "d012f681" "44ed0f12" "1d3cc330" "a17eec52" "8c2e7d59"; }
  virtual std::string getLongMsg1_Digest()
	{ return "c2cce34c" "96d1ab91" "ce5cb15d" "7542a37a" "a7c57fae"; }
  virtual std::string getBinaryData1_Digest()
	{ return "4916d6bd" "b7f78e68" "03698cab" "32d1586e" "a457dfc8"; }
};

/**
 * Subclass of the generic digest tests that specifies the expected digest
 * strings and tests to run for the SHA256 digest class.
 */
class SHA256DigestTest : public GenericDigestTests<SHA256Digest>
{
  CPPUNIT_TEST_SUITE(SHA256DigestTest);
  //CPPUNIT_TEST(testTypicalUsage);
  //CPPUNIT_TEST(testConstruction);
  //CPPUNIT_TEST(testEmptyData);
  //CPPUNIT_TEST(testSmallMessage);
  //CPPUNIT_TEST(testLargeMessage);
  //CPPUNIT_TEST(testBinaryData);
  //CPPUNIT_TEST(testReInitialization);
  //CPPUNIT_TEST(testPiecewise);
  CPPUNIT_TEST_SUITE_END();

public:
  virtual std::string getEmptyMsg_Digest()
	{ return ""; }
  virtual std::string getMsg1_Digest()
	{ return "a2242ead" "55c94c3d" "eb7cf234" "0bfef9d5" "bcaca22d" "fe66e646" "745ee437" "1c633fc8"; }
  virtual std::string getLongMsg1_Digest()
	{ return "24703531" "a4557f53" "d6da5faa" "c96566e4" "d8b1b8ec" "9655757f" "53778d62" "63257943"; }
  virtual std::string getBinaryData1_Digest()
	{ return "40aff2e9" "d2d8922e" "47afd464" "8e696749" "7158785f" "bd1da870" "e7110266" "bf944880"; }
};
