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
#include <cc++/persist.h>
#include "SampleObject.h"
#include <fstream>

using namespace ost;

#define BINARY_BUFFER_SIZE 1000
#define STL_CONTAINER_SIZE 1000

// support macros to clean things for testing of primitive types
// tests both primitive and pointer to primitive types
#define TEST_PRIMITIVE_OUTPUT(type, value) \
  type test_##type = value; \
  type* test_##type##p = new type(value); \
  outputEngine << test_##type; \
  outputEngine << *test_##type##p;

#define TEST_PRIMITIVE_INPUT(type) \
  type test2_##type; \
  type* test2_##type##p = new type; \
  inputEngine >> test2_##type; \
  CPPUNIT_ASSERT(test_##type == test2_##type); \
  inputEngine >> *test2_##type##p; \
  CPPUNIT_ASSERT(test_##type == *test2_##type##p); \
  delete test2_##type##p; \
  delete test_##type##p;

/**
 * Test Fixture to excercise the Common C++ Persistence engine
 *
 * @author Chad C. Yates
 */
class EngineTest : public CppUnit::TestFixture
{
  CPPUNIT_TEST_SUITE(EngineTest);
  CPPUNIT_TEST(testPrimitives);
  CPPUNIT_TEST(testRawBinary);
  CPPUNIT_TEST(testSTLVector);
  CPPUNIT_TEST(testSTLDeque);
  CPPUNIT_TEST(testSTLMap);
  CPPUNIT_TEST(testComplexObject);
  //CPPUNIT_TEST(testModeExceptions);
  CPPUNIT_TEST_SUITE_END();

private:
  Test complexObject;

public:
  void setUp();
  void tearDown();

  /**
   * Test case for all supported primative types
   *
   * @author Chad C. Yates
   */
  void testPrimitives();

  /**
   * Test case for a raw chunk of binary data
   *
   * @author Chad C. Yates
   */
  void testRawBinary();

  /**
   * Test case for all an STL Vector
   *
   * @author Chad C. Yates
   */
  void testSTLVector();

  /**
   * Test case for all an STL Deque
   *
   * @author Chad C. Yates
   */
  void testSTLDeque();

  /**
   * Test case for an STL Map
   *
   * @author Chad C. Yates
   */
  void testSTLMap();

  /**
   * Test case for all a complex Object hierarchy derived from BaseObject
   *
   * @author Chad C. Yates
   */
  void testComplexObject();

  /**
   * Test case for engine mode exceptions
   * since the persistence engine does not actually throw exceptions
   * yet, we can't test for it.
   *
   * @author Chad C. Yates
   */
  void testModeExceptions();
};
