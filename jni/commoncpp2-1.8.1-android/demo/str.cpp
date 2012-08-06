// Copyright (C) 2001 Open Source Telecom Corporation.
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

#include <cc++/common.h>
#include <iostream>
#include <cstdlib>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

String x;

int main(int argc, char **argv)
{
	String a("abc");
	String b(40, "abcdefghixxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

	printf("B %p\n", b.getText());

	String mystring(65);
	String *pstr;

	for(int i = 1; i < 20; ++i) {
		pstr = new String(i, "abcdefg");
		String zstr(i, "abc");
		zstr = *pstr + zstr;
		delete pstr;
		zstr.clear();
	}

	const char *cp = a.index(1);

	printf("part of A %ld is %s\n", a.length(), cp);

	x = b;
	printf("X %p %ld %s\n", x.getText(), x.getLength(), x.getText());

	x = a;
	printf("X %p %ld %ld %s\n", x.getText(), x.size(), x.length(), x.getText());
	x += b;

	printf("X %p %ld %ld %s\n", x.getText(), x.capacity(), x.length(), x.getText());
	x += a;

	printf("X %p %ld %ld %s\n", x.getText(), x.capacity(), x.length(), x.getText());
	x += "0123456789abcdefg";
	printf("X %p %ld %ld %s\n", x.getText(), x.capacity(), x.length(), x.getText());


	cout << "A = " << a << endl;
	cout << "B = " << b << endl;


	if(a > b)
		printf("a > b\n");
	else
		printf("a <= b\n");

	if(a > "123")
		printf("a > 123\n");
	else
		printf("a < 123\n");

	if(x *= "def")
		printf("def contained in X\n");

	if(!(x *= "zebra"))
		printf("zebra not in X\n");

	printf("X IS %p\n", x.getText());

	b.erase(10,20);
	cout << b.length() << " " << b << endl;

	printf("B IS %p\n", b.getText());

	b.insert(3, "123456");
	cout << b << endl;

	cout << b.substr(5, 3) << endl;

	printf("find x %ld\n", b.find("x"));
	printf("find 3rd x %ld\n", b.find(3, "x"));

	String l(",1,2222222222222222222222222222222,3");

	cout << "list is " << l << endl;
	cout << "first is empty" << l.token(",") << endl;
	cout << l.token(",") << endl;
	cout << l.token(",") << endl;
	cout << "remaining " << l << endl;
	cout << l.token(",") << endl;
	cout << "final " << l << " size=" << l.capacity() << endl;

	String f;

	snprintf(f, 80, "testing %f done", 1.03);
	cout << f << endl;

	String p1(20, "hello");
	String p2(20, "Count %d words", 10);
	cout << p1 << endl;
	cout << p2 << endl;

	SString ss;

	ss << "Counting " << 10 << " words";
	ss << " and more";
	cout << ss << endl;
	ss << " and more";
	cout << ss << endl;

	String zip;
	zip = ss;
	cout << zip << endl;
	String num;
#ifdef	HAVE_SNPRINTF
	num = 17;
	num += 23;
	cout << "number " << num << " should be 1723" << endl;
#endif
}

