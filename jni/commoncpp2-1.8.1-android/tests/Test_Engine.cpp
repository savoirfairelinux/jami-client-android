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

#include "Test_Engine.h"
#include <cstdio>

CPPUNIT_TEST_SUITE_REGISTRATION(EngineTest);

void EngineTest::setUp()
{
  // Create an example object with some sub objects and various data elements
  complexObject.iInteger = 5;
  complexObject.strString = "String 1";
  complexObject.strString2 = "String 2";
  complexObject.uninitializedNullPtr = NULL;  // initialized here instead of constructor to see if it will be unpersisted as a null (vs defaulting to null from the constructor)
  for(int i = 0; i < 10; i++)
	complexObject.numbers.push_back(i);
  complexObject.strings.push_back("a");
  complexObject.strings.push_back("b");
  complexObject.strings.push_back("c");
  complexObject.strings.push_back("d");
  complexObject.strings.push_back("e");

  complexObject.moreStrings.push_back("z");
  complexObject.moreStrings.push_back("y");
  complexObject.moreStrings.push_back("x");
  complexObject.moreStrings.push_back("w");
  complexObject.moreStrings.push_back("v");

  complexObject.subObjectPtr = new TestSub;
  complexObject.subObjectPtr2 = complexObject.subObjectPtr; // set to point to the same thing as subObjectPtr to test unpersisting of shared instances
  complexObject.subObjectPtr->iInteger = 5;
  complexObject.subObjectPtr->strString = "SubStringPtr 1";
  complexObject.subObjectPtr->strString2 = "SubStringPtr 2";
  for(int j = 10; j < 20; j++)
	complexObject.subObjectPtr->numbers.push_back(j);

  complexObject.subObjectRef.iInteger = 5;
  complexObject.subObjectRef.strString = "SubString2 1";
  complexObject.subObjectRef.strString2 = "SubString2 2";
  for(int k = 30; k < 35; k++)
	complexObject.subObjectRef.numbers.push_back(k);

  // make a std::deque of TestSub objects
  for(int l = 0; l < 2; l++) {
	TestSub newSubObject;
	newSubObject.iInteger = l;
	char tmp[50];
	sprintf(tmp, "test %d", l+1);
	newSubObject.strString = tmp;
	for(int k = 30; k < 35; k++)
	  newSubObject.numbers.push_back(k);
	complexObject.subObjects.push_back(newSubObject);
  }
}

void EngineTest::tearDown()
{}

void EngineTest::testPrimitives()
{
  // write primitive types
  std::fstream outputArchive("EnginePrimitiveTest.dat", std::ios::out|std::ios::binary);
  Engine outputEngine(outputArchive, ost::Engine::modeWrite);

  TEST_PRIMITIVE_OUTPUT(int8, 0x01);
  TEST_PRIMITIVE_OUTPUT(uint8, 0x01);
  TEST_PRIMITIVE_OUTPUT(int16, 0x0123);
  TEST_PRIMITIVE_OUTPUT(uint16, 0x0123);
  TEST_PRIMITIVE_OUTPUT(int32, 0x01234567);
  TEST_PRIMITIVE_OUTPUT(uint32, 0x01234567);
  //TEST_PRIMITIVE_OUTPUT(int64, 0x0123456789ABCDEF); // warning: integer constant larger than the maximum value of an unsigned long int
  //TEST_PRIMITIVE_OUTPUT(uint64, 0x0123456789ABCDEF); // warning: integer constant larger than the maximum value of an unsigned long int
  TEST_PRIMITIVE_OUTPUT(float, 3.141592653589793238462643f);
  TEST_PRIMITIVE_OUTPUT(double, 3.141592653589793238462643);
  TEST_PRIMITIVE_OUTPUT(string, "abcdefghijklmnopqrstuvwxyz0123456789");

  outputEngine.sync(); // flush Engine buffers before closing file
  outputArchive.close();

  // read primitive types back in and check
  std::fstream inputArchive("EnginePrimitiveTest.dat", std::ios::in|std::ios::binary);
  Engine inputEngine(inputArchive, ost::Engine::modeRead);

  TEST_PRIMITIVE_INPUT(int8);
  TEST_PRIMITIVE_INPUT(uint8);
  TEST_PRIMITIVE_INPUT(int16);
  TEST_PRIMITIVE_INPUT(uint16);
  TEST_PRIMITIVE_INPUT(int32);
  TEST_PRIMITIVE_INPUT(uint32);
  //TEST_PRIMITIVE_INPUT(int64);
  //TEST_PRIMITIVE_INPUT(uint64);
  TEST_PRIMITIVE_INPUT(float);
  TEST_PRIMITIVE_INPUT(double);
  TEST_PRIMITIVE_INPUT(string);

  inputArchive.close();
}


void EngineTest::testRawBinary()
{
  int i;

  // write raw binary data
  std::fstream outputArchive("EngineRawBinaryTest.dat", std::ios::out|std::ios::binary);
  Engine outputEngine(outputArchive, ost::Engine::modeWrite);

  unsigned int binaryBuffer[BINARY_BUFFER_SIZE];
  for(i = 0; i < BINARY_BUFFER_SIZE; i++)
	binaryBuffer[i] = i;

  outputEngine.writeBinary((const uint8*) binaryBuffer, sizeof(binaryBuffer));
  outputEngine.sync(); // flush Engine buffers before closing file
  outputArchive.close();

  // read binary data back in and check
  std::fstream inputArchive("EngineRawBinaryTest.dat", std::ios::in|std::ios::binary);
  Engine inputEngine(inputArchive, ost::Engine::modeRead);

  unsigned int binaryBuffer2[BINARY_BUFFER_SIZE];
  inputEngine.readBinary((uint8*) binaryBuffer2, sizeof(binaryBuffer2));
  inputArchive.close();

  for(i = 0; i < BINARY_BUFFER_SIZE; i++)
	CPPUNIT_ASSERT(binaryBuffer[i] == binaryBuffer2[i]);
}

void EngineTest::testSTLVector()
{
  int i;

  // write STL data
  std::fstream outputArchive("EngineSTLVectorTest.dat", std::ios::out|std::ios::binary);
  Engine outputEngine(outputArchive, ost::Engine::modeWrite);

  std::vector<int32> intVector;
  std::vector<int32>* pIntVector = new std::vector<int32>;
  for(i = 0; i < STL_CONTAINER_SIZE; i++) {
	intVector.push_back(i);
	pIntVector->push_back(i);
  }

  outputEngine << intVector;
  outputEngine << *pIntVector;
  outputEngine.sync(); // flush Engine buffers before closing file
  outputArchive.close();

  // read STL std::vector back in and check
  std::fstream inputArchive("EngineSTLVectorTest.dat", std::ios::in|std::ios::binary);
  Engine inputEngine(inputArchive, ost::Engine::modeRead);

  std::vector<int32> intVector2;
  inputEngine >> intVector2;
  CPPUNIT_ASSERT(intVector == intVector2);

  std::vector<int32>* pIntVector2 = new std::vector<int32>;
  inputEngine >> *pIntVector2;
  CPPUNIT_ASSERT(*pIntVector == *pIntVector2);
  delete pIntVector2;

  inputArchive.close();

  delete pIntVector;
}

void EngineTest::testSTLDeque()
{
  int i;

  // write STL std::deque data
  std::fstream outputArchive("EngineSTLDequeTest.dat", std::ios::out|std::ios::binary);
  Engine outputEngine(outputArchive, ost::Engine::modeWrite);

  std::deque<int32> intDeque;
  std::deque<int32>* pIntDeque = new std::deque<int32>;
  for(i = 0; i < STL_CONTAINER_SIZE; i++) {
	intDeque.push_back(i);
	pIntDeque->push_back(i);
  }

  outputEngine << intDeque;
  outputEngine << *pIntDeque;
  outputEngine.sync(); // flush Engine buffers before closing file
  outputArchive.close();

  // read STL std::deque back in and check
  std::fstream inputArchive("EngineSTLDequeTest.dat", std::ios::in|std::ios::binary);
  Engine inputEngine(inputArchive, ost::Engine::modeRead);

  std::deque<int32> intDeque2;
  inputEngine >> intDeque2;
  CPPUNIT_ASSERT(intDeque == intDeque2);

  std::deque<int32>* pIntDeque2 = new std::deque<int32>;
  inputEngine >> *pIntDeque2;
  CPPUNIT_ASSERT(*pIntDeque == *pIntDeque2);
  delete pIntDeque2;

  inputArchive.close();

  delete pIntDeque;
}

void EngineTest::testSTLMap()
{
  int i;

  // write STL std::map data
  std::fstream outputArchive("EngineSTLMapTest.dat", std::ios::out|std::ios::binary);
  Engine outputEngine(outputArchive, ost::Engine::modeWrite);

  std::map<int32, int32> intMap;
  std::map<int32, int32>* pIntMap = new std::map<int32, int32>;
  for(i = 0; i < STL_CONTAINER_SIZE; i++) {
	intMap.insert(std::pair<int32, int32>(i, i+10));
	pIntMap->insert(std::pair<int32, int32>(i, i+11));
  }

  outputEngine << intMap;
  outputEngine << *pIntMap;
  outputEngine.sync(); // flush Engine buffers before closing file
  outputArchive.close();

  // read STL std::map back in and check
  std::fstream inputArchive("EngineSTLMapTest.dat", std::ios::in|std::ios::binary);
  Engine inputEngine(inputArchive, ost::Engine::modeRead);

  std::map<int32, int32> intMap2;
  inputEngine >> intMap2;
  CPPUNIT_ASSERT(intMap == intMap2);

  std::map<int32, int32>* pIntMap2 = new std::map<int32, int32>;
  inputEngine >> *pIntMap2;
  CPPUNIT_ASSERT(*pIntMap == *pIntMap2);
  delete pIntMap2;

  inputArchive.close();

  delete pIntMap;
}

void EngineTest::testComplexObject()
{
  // write BaseObject hierarchy
  std::fstream outputArchive("EngineComplexObjectTest.dat", std::ios::out|std::ios::binary);
  Engine outputEngine(outputArchive, ost::Engine::modeWrite);
  outputEngine << complexObject;
  outputEngine.sync(); // flush Engine buffers before closing file
  outputArchive.close();

  // Unpersist a new object structure into an uninitialized object
  {
	Test* myObjPtr = NULL; // must initialize pointer or new persistence engine will think it is already allocated
	std::fstream inputArchive("EngineComplexObjectTest.dat", std::ios::in);
	Engine inputEngine(inputArchive, ost::Engine::modeRead);
	inputEngine >> myObjPtr;
	inputArchive.close();

	CPPUNIT_ASSERT_MESSAGE("Unpersisted into unallocated pointer == Original", *myObjPtr == complexObject);
	CPPUNIT_ASSERT_MESSAGE("nullPtr is NULL", myObjPtr->nullPtr == NULL);
	CPPUNIT_ASSERT_MESSAGE("uninitializedNullPtr is NULL", myObjPtr->uninitializedNullPtr == NULL);

	myObjPtr->subObjectPtr->strString2 = "Changed SubStringPtr 1";
	CPPUNIT_ASSERT_MESSAGE("subObjectPtr.strString2 should always equal subObjectPtr2.strString2", myObjPtr->subObjectPtr->strString2 == myObjPtr->subObjectPtr2->strString2);
	CPPUNIT_ASSERT_MESSAGE("subObjectPtr.strString2 should now equal 'Changed SubStringPtr 1'", myObjPtr->subObjectPtr->strString2 == "Changed SubStringPtr 1");
	CPPUNIT_ASSERT_MESSAGE("subObjectPtr.strString2 should still equal subObjectPtr2.strString2", myObjPtr->subObjectPtr->strString2 == myObjPtr->subObjectPtr2->strString2);
  }

  // Unpersist a new object structure into an instantiated class variable
  {
	Test myObjInstance;
	std::fstream inputArchive("EngineComplexObjectTest.dat", std::ios::in);
	Engine inputEngine(inputArchive, ost::Engine::modeRead);
	inputEngine >> myObjInstance;
	inputArchive.close();

	CPPUNIT_ASSERT_MESSAGE("Unpersisted into default constructed instance == Original", myObjInstance == complexObject);
	CPPUNIT_ASSERT_MESSAGE("nullPtr is NULL", myObjInstance.nullPtr == NULL);
	CPPUNIT_ASSERT_MESSAGE("UninitializedNullPtr is NULL", myObjInstance.uninitializedNullPtr == NULL);
  }

  // Unpersist a new object structure into a pre-initialized pointer to an object
  {
	Test* myObjAllocatedPtr = new Test;
	std::fstream inputArchive("EngineComplexObjectTest.dat", std::ios::in);
	Engine inputEngine(inputArchive, ost::Engine::modeRead);
	inputEngine >> myObjAllocatedPtr;
	outputEngine.sync(); // flush Engine buffers before closing file
	inputArchive.close();

	CPPUNIT_ASSERT_MESSAGE("Unpersisted into pre-allocated pointer", *myObjAllocatedPtr == complexObject);
	CPPUNIT_ASSERT_MESSAGE("nullPtr is NULL", myObjAllocatedPtr->nullPtr == NULL);
	CPPUNIT_ASSERT_MESSAGE("uninitializedNullPtr is NULL", myObjAllocatedPtr->uninitializedNullPtr == NULL);
  }
}

void EngineTest::testModeExceptions()
{
  // write primitive types
  std::fstream outputArchive("EnginePrimitiveTest.dat", std::ios::in|std::ios::binary);
  Engine outputEngine(outputArchive, ost::Engine::modeWrite);

  int32 test_Int32 = 0x01234567;
  try {
	outputEngine << test_Int32;
	//CPPUNIT_FAIL("Call to persist to an input stream should throw an exception");
  }
  catch(PersistException &ex) {
	CPPUNIT_ASSERT_MESSAGE(ex.getString(), true);
  }

  outputArchive.close();
}
