#ifndef TEST_APP_INTRO_PROVIDER_H
#define TEST_APP_INTRO_PROVIDER_H

#include "TestAppIntroProviderPrivate.h"
#include "TestAppIntro.h"
#include "../../../tools/generator_utils.h"

#include <iostream>

using namespace std;

class TestAppIntroProvider :
  public DBusCpp::Test::Com::Intro_adaptor,
  public DBus::IntrospectableAdaptor,
  public DBus::ObjectAdaptor
{
public:
  TestAppIntroProvider(DBus::Connection &connection, TestAppIntro *testComIntro) :
    DBus::ObjectAdaptor(connection, "/DBusCpp/Test/Com/Intro"),
    mTestAppIntro(testComIntro)
  {}

  void test1()
  {
    cout << "Test1" << endl;
    mTestAppIntro->test1Result();
  }

  void testByte(const uint8_t &Byte)
  {
    printf("TestByte: %d\n", Byte);
    mTestAppIntro->testByteResult(Byte);
  }

private:
  TestAppIntro *mTestAppIntro;
};

#endif // TEST_COM_INTRO_PROVIDER_H

