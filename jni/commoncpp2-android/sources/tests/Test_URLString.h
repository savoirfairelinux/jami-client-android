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
#include <iostream>
#include <cc++/url.h>

using namespace ost;
using std::string;

/**
 * Test Fixture to excercise the Common C++ urlstring functions
 *
 * @author Chad C. Yates
 */
class URLStringTest : public CppUnit::TestFixture
{
  CPPUNIT_TEST_SUITE(URLStringTest);
  CPPUNIT_TEST(testBinaryBase64EncodeDecode);
  CPPUNIT_TEST(testStringVersion);
  CPPUNIT_TEST(testTypicalUrlEncodeDecode);
  CPPUNIT_TEST(testTypicalTextBase64EncodeDecode);
  CPPUNIT_TEST(testSimpleTextBase64EncodeDecode);
  CPPUNIT_TEST(testComplexTextBase64EncodeDecode);
  CPPUNIT_TEST(testPaddingPerfect);
  CPPUNIT_TEST(testPadding1Short);
  CPPUNIT_TEST(testPadding2Short);
  CPPUNIT_TEST_SUITE_END();

private:
  /*
  static const char inputChars1[] = "`1234567890-=\\QWERTYUIOP[]ASDFGHJKL;'ZXCVBNM,./~!@#$%^&*()_+|qwertyuiop{}asdfghjkl:\"zxcvbnm<>?";
  static const expectedOutputChars1[1000] = "YDEyMzQ1Njc4OTAtPVxRV0VSVFlVSU9QW11BU0RGR0hKS0w7J1pYQ1ZCTk0sLi9+IUAjJCVeJiooKV8rfHF3ZXJ0eXVpb3B7fWFzZGZnaGprbDoienhjdmJubTw+Pw==";
  static const char inputChars2[] = "This is a test.";
  static const char expectedOutputChars2[1000] = "VGhpcyBpcyBhIHRlc3Qu";
  */
  char actualEncodedChars[1000]; // should be enough for the tests
  char actualDecodedChars[1000]; // should be enough for the tests

public:
  void testTypicalUrlEncodeDecode() {
	char inputChars[] = "`1234567890-=\\QWERTYUIOP[]ASDFGHJKL;'ZXCVBNM,./~!@#$%^&*()_+|qwertyuiop{}asdfghjkl:\"zxcvbnm<>?";
	char expectedOutputChars[] = "%601234567890-%3d%5cQWERTYUIOP%5b%5dASDFGHJKL;%27ZXCVBNM,./%7e%21%40%23%24%25%5e%26%2a%28%29%5f%2b%7cqwertyuiop%7b%7dasdfghjkl:%22zxcvbnm%3c%3e%3f";

	urlEncode(inputChars, actualEncodedChars, sizeof(actualEncodedChars));
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to url encode was: '" + string(inputChars) + string("'"), string(expectedOutputChars), string(actualEncodedChars));

	urlDecode(actualEncodedChars, actualDecodedChars);
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to url decode was: '" + string(actualEncodedChars) + string("'"), string(inputChars), string(actualDecodedChars));
  }

  void testBinaryBase64EncodeDecode() {
	unsigned char binaryData[256];
	char encodedBinaryData[sizeof(binaryData)*4];
	unsigned char decodedBinaryData[sizeof(binaryData)];
	unsigned int i;

	for(i = 0; i < sizeof(binaryData); ++i)
	  binaryData[i] = i;

	b64Encode(binaryData, sizeof(binaryData), encodedBinaryData, sizeof(encodedBinaryData));
	b64Decode(encodedBinaryData, decodedBinaryData, sizeof(decodedBinaryData));

	for(i = 0; i < sizeof(binaryData); ++i)
	  if(binaryData[i] != decodedBinaryData[i]) {
		CPPUNIT_ASSERT_EQUAL(binaryData[i], decodedBinaryData[i]);
		break;
	  }

	CPPUNIT_ASSERT(true);
  }

  void testStringVersion() {
	CPPUNIT_ASSERT_EQUAL(String("VGhpcyBpcyBhIHRlc3Qu"), b64Encode(String("This is a test.")));
	CPPUNIT_ASSERT_EQUAL(String("This is a test."), b64Decode(String("VGhpcyBpcyBhIHRlc3Qu")));
  }

  void testTypicalTextBase64EncodeDecode() {
	char inputChars[] = "`1234567890-=\\QWERTYUIOP[]ASDFGHJKL;'ZXCVBNM,./~!@#$%^&*()_+|qwertyuiop{}asdfghjkl:\"zxcvbnm<>?";
	char expectedOutputChars[1000] = "YDEyMzQ1Njc4OTAtPVxRV0VSVFlVSU9QW11BU0RGR0hKS0w7J1pYQ1ZCTk0sLi9+IUAjJCVeJiooKV8rfHF3ZXJ0eXVpb3B7fWFzZGZnaGprbDoienhjdmJubTw+Pw==";

	b64Encode(inputChars, actualEncodedChars, sizeof(actualEncodedChars));
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to base64 encode was: '" + string(inputChars) + string("'"), string(expectedOutputChars), string(actualEncodedChars));

	b64Decode(actualEncodedChars, actualDecodedChars);
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to base64 decode was: '" + string(actualEncodedChars) + string("'"), string(inputChars), string(actualDecodedChars));
  }

  void testSimpleTextBase64EncodeDecode() {
	const char inputChars[] = "This is a test.";
	const char expectedOutputChars[1000] = "VGhpcyBpcyBhIHRlc3Qu";

	b64Encode((const unsigned char *)inputChars, strlen(inputChars), actualEncodedChars, sizeof(actualEncodedChars));
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to base64 encode was: '" + string(inputChars) + string("'"), string(expectedOutputChars), string(actualEncodedChars));
	int size = b64Decode(actualEncodedChars, (unsigned char *)actualDecodedChars, sizeof(actualDecodedChars));
	actualDecodedChars[size] = '\0';
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to base64 decode was: '" + string(actualEncodedChars) + string("'"), string(inputChars), string(actualDecodedChars));
  }

  void testPaddingPerfect() {
	const char inputChars[] = "aaabbb123";

	b64Encode((const unsigned char *)inputChars, strlen(inputChars), actualEncodedChars, sizeof(actualEncodedChars));
	int size = b64Decode(actualEncodedChars, (unsigned char *)actualDecodedChars, sizeof(actualDecodedChars));
	actualDecodedChars[size] = '\0';
	CPPUNIT_ASSERT_EQUAL_MESSAGE(
	  "String to base64 decode was: '" + string(actualEncodedChars) + string("' ") +
		"",
	  string(inputChars),   // expected
	  string(actualDecodedChars)); // actual
  }

  void testPadding1Short() {
	const char inputChars[] = "aaabbb12";

	b64Encode((const unsigned char *)inputChars, strlen(inputChars), actualEncodedChars, sizeof(actualEncodedChars));
	int size = b64Decode(actualEncodedChars, (unsigned char *)actualDecodedChars, sizeof(actualDecodedChars));
	actualDecodedChars[size] = '\0';
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to base64 decode was: '" + string(actualEncodedChars) + string("'"), string(inputChars), string(actualDecodedChars));
  }

  void testPadding2Short() {
	const char inputChars[] = "aaabbb1";

	b64Encode((const unsigned char *)inputChars, strlen(inputChars), actualEncodedChars, sizeof(actualEncodedChars));
	int size = b64Decode(actualEncodedChars, (unsigned char *)actualDecodedChars, sizeof(actualDecodedChars));
	actualDecodedChars[size] = '\0';
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to base64 decode was: '" + string(actualEncodedChars) + string("'"), string(inputChars), string(actualDecodedChars));
  }

  void testComplexTextBase64EncodeDecode() {
	const char inputChars[] =
	  "This module provides functions to encode and decode\n"
	  "strings into the Base64 encoding specified in RFC 2045 -\n"
	  "MIME (Multipurpose Internet Mail Extensions). The Base64\n"
	  "encoding is designed to represent arbitrary sequences of\n"
	  "octets in a form that need not be humanly readable. A\n"
	  "65-character subset ([A-Za-z0-9+/=]) of US-ASCII is used,\n"
	  "enabling 6 bits to be represented per printable character.";
	const char expectedOutputChars[1000] =
	  "VGhpcyBtb2R1bGUgcHJvdmlkZXMgZnVuY3Rpb25zIHRvIGVuY29kZSBhbmQgZGVjb2RlCnN0cmlu"
	  "Z3MgaW50byB0aGUgQmFzZTY0IGVuY29kaW5nIHNwZWNpZmllZCBpbiBSRkMgMjA0NSAtCk1JTUUg"
	  "KE11bHRpcHVycG9zZSBJbnRlcm5ldCBNYWlsIEV4dGVuc2lvbnMpLiBUaGUgQmFzZTY0CmVuY29k"
	  "aW5nIGlzIGRlc2lnbmVkIHRvIHJlcHJlc2VudCBhcmJpdHJhcnkgc2VxdWVuY2VzIG9mCm9jdGV0"
	  "cyBpbiBhIGZvcm0gdGhhdCBuZWVkIG5vdCBiZSBodW1hbmx5IHJlYWRhYmxlLiBBCjY1LWNoYXJh"
	  "Y3RlciBzdWJzZXQgKFtBLVphLXowLTkrLz1dKSBvZiBVUy1BU0NJSSBpcyB1c2VkLAplbmFibGlu"
	  "ZyA2IGJpdHMgdG8gYmUgcmVwcmVzZW50ZWQgcGVyIHByaW50YWJsZSBjaGFyYWN0ZXIu";

	b64Encode((const unsigned char *)inputChars, strlen(inputChars), actualEncodedChars, sizeof(actualEncodedChars));
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to base64 encode was: '" + string(inputChars) + string("'"), string(expectedOutputChars), string(actualEncodedChars));
	int size = b64Decode(actualEncodedChars, (unsigned char *)actualDecodedChars, sizeof(actualDecodedChars));
	actualDecodedChars[size] = '\0';
	CPPUNIT_ASSERT_EQUAL_MESSAGE("String to base64 decode was: '" + string(actualEncodedChars) + string("'"), string(inputChars), string(actualDecodedChars));
  }
};
