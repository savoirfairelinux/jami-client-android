#include <cc++/thread.h>
#include <cstdio>
#include <cstring>
#include <iostream>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

// Test if cancellation unwinds stack frame
//

class myObject
{
public:
	myObject()
		{cout << "created auto object on stack" << endl;};

	~myObject()
		{cout << "destroyed auto object on cancel" << endl;};
};

class myThread: public Thread
{
public:
	myThread() : Thread() {};

	void run(void) {
		myObject obj;
		setCancel(cancelImmediate);
		Thread::sleep(TIMEOUT_INF);
	}

	~myThread()
		{cout << "ending thread" << endl;};
};

int main(int argc, char* argv[])
{
	cout << "starting thread" << endl;
	myThread *th = new myThread();
	th->start();
	Thread::sleep(1000);	// 1 second
	delete th;		// delete to join

	return 0;
}

