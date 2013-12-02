/** ********************************************************************
 * C/C++ Source: serialecho.h
 *
 * Class definitions for the SerialEcho and related classes.
 * This package requires the Serial, TTYSession and Thread classes
 * from the FSF Common C++ library (v 1.2.4 cplusplus.sourceforge.net)
 *
 * SerialEcho is a monitor on the serial port which runs in its own
 * thread and is responsible for detecting and echoing any serial
 * input.  The class is based on the ttysession class so it can be
 * used as any fstream-like class
 *
 * @author:  Gary Lawrence Murphy <garym@canada.com>
 * Copyright:  2000 TeleDynamics Communications Inc (www.teledyn.com)
 ********************************************************************
 */

#ifndef SERIALECHO_H
#define SERIALECHO_H

#include <cc++/common.h>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

class SerialEcho : public TTYSession {
 public:

  SerialEcho(const char *device,
	 int priority = 0, int stacksize = 0);

  // Exception classes
  class xError{}; // nebulous inexplicable error
  class xLocked{}; // port is there but we are locked out
  class xOverrun{}; // too much data, too little time

 protected:

  void run();
};

#endif
