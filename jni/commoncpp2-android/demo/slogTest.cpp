
#include <cc++/slog.h>
#include <iostream>
#include <cstdio>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif


int main(int argc, char* argv[])
{
   slog("slogTest", Slog::classUser, Slog::levelInfo);
   slog << "Howdy daemon and clog." << endl;
   slog.clogEnable(false);
   slog << "This is only for the daemon." << endl;
   slog.clogEnable(true);
   slog << "Are you still there?" << endl;
   return 0;
}

