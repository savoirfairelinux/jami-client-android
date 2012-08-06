
#include <cc++/thread.h>
#include <cstdio>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif


int main(int argc, char* argv[])
{
	TimerPort timer;
	unsigned i = 12;
	time_t now, start;

	time(&start);
	timer.setTimer();
	fflush(stdout);
	while(i--) {
		timer.incTimer(250);
		Thread::sleep(250);
		printf("!");
		fflush(stdout);
	}
	time(&now);
	printf("%ld", now - start);
	for(;;) {
		timer.incTimer(100);
		timer.sleepTimer();
		printf(".");
		fflush(stdout);
	}
	return 0;
}

