#include <cc++/thread.h>
#include <cstdio>

#ifdef	CCXX_NAMESPACES
using namespace ost;
#endif

// This is a little regression test
//

class ThreadTest: public Thread
{
public:
	ThreadTest();
	void run();
};

volatile int n = 0;

bool WaitNValue(int value)
{
	for(int i=0;; ++i) {
		if (n == value)
			break;
		if (i >= 100)
			return false;
		Thread::sleep(10);
	}
	return true;
}

bool WaitChangeNValue(int value)
{
	for(int i=0;; ++i) {
		if (n != value)
			break;
		if (i >= 100)
			return false;
		Thread::sleep(10);
	}
	return true;
}

ThreadTest::ThreadTest()
{
}

void ThreadTest::run()
{
	setCancel(Thread::cancelDeferred);
	n = 1;

	// wait for main thread
	if (!WaitNValue(2)) return;

	// increment infinitely
	for(;;) {
		yield();
		n = n+1;
	}
}

bool TestChange(bool shouldChange)
{
	if (shouldChange)
		printf("- thread should change n...");
	else
		printf("- thread should not change n...");
	if (WaitChangeNValue(n) == shouldChange) {
		printf("ok\n");
		return true;
	}
	printf("ko\n");
	return false;
}

#undef ERROR
#undef OK
#define ERROR {printf("ko\n"); return 1; }
#define OK {printf("ok\n"); }

#define TEST_CHANGE(b) if (!TestChange(b)) return 1;

int main(int argc, char* argv[])
{
	ThreadTest test;

	// test only thread, without sincronization
	printf("***********************************************\n");
	printf("* Testing class Thread without syncronization *\n");
	printf("***********************************************\n");

	printf("Testing thread creation\n\n");
	n = 0;
	test.start();

	// wait for n == 1
	printf("- thread should set n to 1...");
	if (WaitNValue(1)) OK
	else ERROR;

	// increment number in thread
	printf("\nTesting thread is working\n\n");
	n = 2;
	TEST_CHANGE(true);
	TEST_CHANGE(true);

	// suspend thread, variable should not change
	printf("\nTesting suspend & resume\n\n");
	test.suspend();
	TEST_CHANGE(false);
	TEST_CHANGE(false);

	// resume, variable should change
	test.resume();
	TEST_CHANGE(true);
	TEST_CHANGE(true);

	printf("\nTesting recursive suspend & resume\n\n");
	test.suspend();
	test.suspend();
	TEST_CHANGE(false);
	TEST_CHANGE(false);

	test.resume();
	TEST_CHANGE(false);
	TEST_CHANGE(false);
	test.resume();
	TEST_CHANGE(true);
	TEST_CHANGE(true);

	printf("\nTesting no suspend on resume\n\n");
	test.resume();
	TEST_CHANGE(true);
	TEST_CHANGE(true);

	// suspend thread, variable should not change
	printf("\nTesting resuspend\n\n");
	test.suspend();
	TEST_CHANGE(false);
	TEST_CHANGE(false);

	printf("\nNow program should finish... :)\n");
	test.resume();

	return 0;
}
