/**********************************************************************
 * C/C++ Source: main.cc
 *
 * Test harness for the serialecho class
 *
 * @author:  Gary Lawrence Murphy <garym@teledyn.com>
 * Copyright:  2000 TeleDynamics Communications Inc (www.teledyn.com)
 ********************************************************************
*/
// Copyright (C) 1999-2000 Teledynamics Communications Inc.
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
// Created 2000/10/14 10:56:35 EDT by garym@teledyn.com

#include "serialecho.h"
#ifndef	WIN32
#include <cstdlib>
#endif

int main(int argc, char **argv)
{
  cout << "Serial Echo to TCP Sessions" << endl;
  SerialEcho *modem = NULL;
  try {
	modem = new SerialEcho("/dev/modem2");
  } catch (SerialEcho::xError *e) {
	cout << "Modem Error; aborting" << endl;
	::exit(1);
  } catch (Serial *e) {
	cout << "Serial Error: "
		 << modem->getErrorString()
		 << "; aborting"
		 << endl;
	::exit(1);
  }

  char* b = new char[modem->getBufferSize()];

  cout << "Modem code:" << modem->start() << endl;

  while (cin >> b, b[0]) {

	*modem << b << "\r" << endl;

	cout << "sent: " << b << endl;
	memset( b, 0, sizeof(b));

  }
  cout << "fin" << endl;

  delete [] b;

  return 0;
}

/**  2000 by TeleDynamics Communications Inc - teledynamics@canada.com*/

