// can't get this to work because the SHATumbler is not accessable from the dll
#include <cppunit/extensions/HelperMacros.h>
#include <cc++/config.h>
#include <cc++/digest.h>
#include <fstream>

using namespace ost;

class SHATumblerTest : public CppUnit::TestFixture
{
  CPPUNIT_TEST_SUITE(SHATumblerTest);
  CPPUNIT_TEST(test);
  CPPUNIT_TEST_SUITE_END();
protected:
public:
  void test() {
	SHATumbler<uint32> tumbler(1);
  }
};
