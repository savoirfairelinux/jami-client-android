#ifndef TEST_APP_H
#define TEST_APP_H

// STD
#include <cstring>
#include <list>
#include <string>

/* DBus-cxx */
#include <dbus-c++/dbus.h>
#include "TestAppIntroProvider.h"

class TestApp
{
public:
  TestApp();

private:
  void initDBus();

  static void testHandler(const void *data, void *buffer, unsigned int nbyte);
  static void *testThreadRunner(void *arg);
  static void *testThreadRunnerProvider(void *arg);

  // variables
  pthread_t testThread;
};

#endif // TEST_APP_H
