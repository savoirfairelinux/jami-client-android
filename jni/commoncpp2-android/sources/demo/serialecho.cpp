/**********************************************************************
 * C/C++ Source: serialecho.cc
 *
 * Defines the methods for the SerialEcho class
 *
 * @author:  Gary Lawrence Murphy <garym@canada.com>
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

#include "serialecho.h"
#ifndef	WIN32
#include <cstdlib>
#endif

using namespace std;

SerialEcho::SerialEcho(const char *device,
		   int priority, int stacksize) :
  TTYSession( device, priority, stacksize ) {

  cout << "Creating SerialEcho" << endl;

  if (!(*this)) {
	throw xError();
	::exit(1);
  } else {
	cout << "modem ready" << endl;
  }

  interactive(false);

  if (setSpeed(38400)) cout << getErrorString() << endl;
  if (setCharBits(8)) cout << getErrorString() << endl;
  if (setParity(Serial::parityNone)) cout << getErrorString() << endl;
  if (setStopBits(1)) cout << getErrorString() << endl;
  if (setFlowControl(Serial::flowHard)) cout << getErrorString() << endl;

  cout << "config done" << endl;
}

void SerialEcho::run() {
  char* s = new char[getBufferSize()];

  cout << "start monitor" << endl;

  while (s[0] != 'X') {
	while (isPending(Serial::pendingInput)) {
	  cout.put( TTYStream::get() );
		}
	sleep(500);
  }

  cout << "end of monitor" << endl;

  delete [] s;

  exit();
}

