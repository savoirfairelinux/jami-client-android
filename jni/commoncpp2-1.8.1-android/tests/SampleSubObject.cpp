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

#include "SampleSubObject.h"

IMPLEMENT_PERSISTENCE(TestSub, "TestSub")

TestSub::TestSub()
: iInteger(0), strString("Null"), strString2("Null")
{}

TestSub::TestSub(const TestSub &ob)
{
  iInteger = ob.iInteger;
  strString = ob.strString;
  strString2 = ob.strString2;
}

bool TestSub::operator==(const TestSub &ob) const
{
  return
	iInteger == ob.iInteger &&
	strString == ob.strString &&
	strString2 == ob.strString2 &&
	numbers == ob.numbers;
}

bool TestSub::write(Engine& archive) const
{
  archive << iInteger << strString << strString2;
  archive << numbers;

  return true;
}

bool TestSub::read(Engine& archive)
{
  archive >> iInteger >> strString >> strString2;
  archive >> numbers;

  return true;
}

void TestSub::print(const string& name) const
{
  cout << "------------------------------------\n";

  cout <<
	name << ":\n" <<
	"     iInteger: " << iInteger << endl <<
	"    strString: '" << strString << "'" << endl <<
	"   strString2: '"  << strString2 << "'" << endl;

  cout <<
	"      numbers: ";
  std::copy(numbers.begin(), numbers.end(), std::ostream_iterator<double>(cout, " "));
  cout << endl;
}

