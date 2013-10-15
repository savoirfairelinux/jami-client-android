#include <cc++/thread.h>
using namespace ost;

class Foo : public Thread
 {
 void run() { exit(); };
 };

int main()
 {
 Foo *f = new Foo();
 f->start();

 // Endless!? why?
 do {
 Thread::sleep( 1000 );
 }
 while( f->isRunning() );
 }

