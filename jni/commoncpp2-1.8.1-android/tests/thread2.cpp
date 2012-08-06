#include <cc++/thread.h>
#include <cstdio>
#include <cstring>
#include <iostream>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif

// Test child thread destroying before father
//
class Child: public Thread
{
public:
	Child()
	{ }
	void run() {
		cout << "child start" << endl;
		Thread::sleep(3000);
		cout << "child end" << endl;
	}
	void final() {
//		delete this;
	}
};

class Father: public Thread
{
public:
	Father()
	{ }
	void run() {
		cout << "starting child thread" << endl;
		Thread *th = new Child();
		th->detach();
		Thread::sleep(1000);
		cout << "father end" << endl;
	}
	void final() {
		// delete this; - not used since detached threads self delete
		// reset memory to test access violation
		memset(this,0,sizeof(*this));
	}
};

int main(int argc, char* argv[])
{
	cout << "starting father thread" << endl;
	Father *th = new Father();
	th->start();
	Thread::sleep(10000);

	return 0;
}

