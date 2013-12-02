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
#include <cc++/thread.h>
#include <cc++/numbers.h> // includes Date
#include <iostream>
#include <iomanip>

using namespace ost;
using std::string;

/**
 * Test Fixture to excercise the Common C++ urlstring functions
 *
 * @author Chad C. Yates
 */
class DateTest : public CppUnit::TestFixture
{
  CPPUNIT_TEST_SUITE(DateTest);
  CPPUNIT_TEST(testSimpleGets);
  CPPUNIT_TEST(testSimpleSets);
  CPPUNIT_TEST(testIsValid);
  CPPUNIT_TEST(testGetDate_String);
  CPPUNIT_TEST(testGetDate_time_t);
  CPPUNIT_TEST(testGeteDate_struct_tm);
  CPPUNIT_TEST(testOperations);
  //CPPUNIT_TEST_SUB_SUITE(CppUnit::TestCaller<Orthodox<Date> >, DateTest);
  CPPUNIT_TEST_SUITE_END();

protected:
  Date date;
  int exp_year;
  long exp_value;
  unsigned exp_month;
  unsigned exp_day;
  unsigned exp_dayofweek;
  string exp_stringdate;
  tm exp_dt;
  time_t exp_ctime;

public:
  void setUp() {
	Thread::setException(Thread::throwNothing);

	date = Date(2003, 1, 6);
	exp_year = 2003;
	exp_month = 1;
	exp_day = 6;
	exp_dayofweek = 1;

	std::stringstream tmp;
	tmp << exp_year << "-" << std::setfill('0') << std::setw(2) << exp_month << "-" << std::setw(2) << exp_day;
	exp_stringdate = tmp.str();

	std::stringstream tmp2;
	tmp2 << exp_year << std::setfill('0') << std::setw(2) << exp_month << std::setw(2) << exp_day;
	exp_value = atoi(tmp2.str().c_str());

	// make a ctime style datetime stamp
	  memset(&exp_dt, 0, sizeof(exp_dt));
	exp_dt.tm_year = exp_year - 1900;  // years since 1900
	exp_dt.tm_mon = exp_month - 1;  // months since january (0-11)
	exp_dt.tm_mday = exp_day;
	exp_ctime = mktime(&exp_dt);
  }

  void testSimpleGets() {
	CPPUNIT_ASSERT_EQUAL(exp_year, date.getYear());
	CPPUNIT_ASSERT_EQUAL(exp_month, date.getMonth());
	CPPUNIT_ASSERT_EQUAL(exp_day, date.getDay());
	CPPUNIT_ASSERT_EQUAL(exp_dayofweek, date.getDayOfWeek()); // 0 = sunday (what about locales?)
	CPPUNIT_ASSERT_EQUAL(exp_value, date.getValue());
  }

  void testSimpleSets() {
	//Date date;
	//const char aDate[] = "20030106";
	//date.setDate(aDate, sizeof(aDate));
	//CPPUNIT_ASSERT_EQUAL(long(atoi(aDate)), date.getValue()); // also checks that aDate was not changed (constness)
  }

  void testIsValid() {
	char aDate[9];
	strcpy(aDate, "20031306"); // 13th month

	date.setDate(aDate, sizeof(aDate));
	CPPUNIT_ASSERT_EQUAL(bool(false), date.isValid());
  }

  void testGetDate_String() {
	char dateBuffer[1000];
	CPPUNIT_ASSERT_EQUAL(exp_stringdate, string(date.getDate(dateBuffer)));
  }

  void testGetDate_time_t() {
	CPPUNIT_ASSERT_EQUAL_MESSAGE("time_t Date::getDate()", exp_ctime, date.getDate());
  }

  void testGeteDate_struct_tm() {
	//tm dtBuffer;
	//CPPUNIT_ASSERT_EQUAL_MESSAGE("time_t does not match", exp_ctime, date.getDate(&dtBuffer));
	//CPPUNIT_ASSERT_MESSAGE("tm structs do not match", memcmp(&exp_dt, &dtBuffer, sizeof(exp_dt)) == 0);
  }

  void testOperations() {
	Date aDate = date;
	Date theNextDay(2003, 1, 7);
	//Date badDate(2003, 2, 30);

	CPPUNIT_ASSERT_MESSAGE("Operator ==", aDate == date);

	++aDate;
	CPPUNIT_ASSERT_MESSAGE("Operator ++ (prefix)", aDate == theNextDay);

	CPPUNIT_ASSERT_MESSAGE("Operator !=", aDate != date);
	CPPUNIT_ASSERT_MESSAGE("Operator <", date < aDate);
	CPPUNIT_ASSERT_MESSAGE("Operator <=", date <= aDate);
	CPPUNIT_ASSERT_MESSAGE("Operator <=", date <= date);
	CPPUNIT_ASSERT_MESSAGE("Operator >", aDate > date);
	CPPUNIT_ASSERT_MESSAGE("Operator >=", aDate >= date);
	CPPUNIT_ASSERT_MESSAGE("Operator >=", date >= date);

	--aDate;
	CPPUNIT_ASSERT_MESSAGE("Operator -- (prefix)", aDate == date);

	aDate += 1;
	CPPUNIT_ASSERT_MESSAGE("Operator +=", aDate == theNextDay);

	aDate -= 1;
	CPPUNIT_ASSERT_MESSAGE("Operator -= ", aDate == date);

	CPPUNIT_ASSERT_MESSAGE("Operator !", !aDate == false);

	// FIXME can't get these to work???
	//std::cout << theNextDay - long(1) << std::endl;
	//CPPUNIT_ASSERT_MESSAGE("", theNextDay - date == 1);
	//CPPUNIT_ASSERT_MESSAGE("", date + 1 == theNextDay);
	//CPPUNIT_ASSERT_MESSAGE("", theNextDay - 1 == date);

	//FIXME add leap year checks
  }

};
