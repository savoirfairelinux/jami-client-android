/*
 * =====================================================================================
 *
 *       Filename:  test_alog.cpp
 *
 *    Description:  file to test alog and AppLog
 *
 *        Version:  1.0
 *        Created:  17/03/2009 14:49:21
 *       Compiler:  g++
 *
 *         Author:  Angelo Naselli (an), anaselli@linux.it (C) 2009
 *      Copyright:  See COPYING file that comes with this distribution
 *
 * =====================================================================================
 */

#include <cc++/thread.h>
#include <cc++/applog.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
#endif

class ThreadTest: public Thread
{
    static AppLog::Ident mod_name;
    void final ( void );
    void initial ( void );

    int _id;
  public:
    ThreadTest ( int id );
    void run();
};

AppLog::Ident  ThreadTest::mod_name ( "ThreadTest" );

ThreadTest::ThreadTest ( int id ) : Thread(), _id ( id )
{
}

void ThreadTest::final()
{
  alog << mod_name << info << __PRETTY_FUNCTION__ << " id " << _id << endl;
  alog.unsubscribe();
}

void ThreadTest::initial()
{
  //Init applog data
  alog.subscribe();
  alog.level ( Slog::levelInfo );
  alog << mod_name << info << __PRETTY_FUNCTION__ << " id " << _id << endl;
}

void ThreadTest::run()
{
  alog << mod_name << debug    <<  __PRETTY_FUNCTION__ << " id " << _id << endl;
  alog << mod_name << info     <<  __PRETTY_FUNCTION__ << " id " << _id << endl;
  alog << mod_name << warn     <<  __PRETTY_FUNCTION__ << " id " << _id << endl;
  alog << mod_name << alert    <<  __PRETTY_FUNCTION__ << " id " << _id << endl;
  Thread::sleep ( 10 );
  alog << mod_name << error    <<  __PRETTY_FUNCTION__ << " id " << _id << endl;
  alog << mod_name << notice   <<  __PRETTY_FUNCTION__ << " id " << _id << endl;
  alog << mod_name << critical <<  __PRETTY_FUNCTION__ << " id " << _id << endl;
  alog << mod_name << emerg    <<  __PRETTY_FUNCTION__ << " id " << _id << endl;

  Thread::sleep ( 100 );
}


int main()
{
  AppLog::Ident mod_name ( "main" );
  // uncomment third parameter to test log over pipe
  alog.logFileName ( "./test_alog.log", false /*, true */);
  alog.subscribe();
  alog.level ( Slog::levelDebug );
  alog.slogEnable ( false ); // default false
  alog.clogEnable ( true );
  alog << mod_name << info   <<  "testAlog V. 1.0.0" << std::endl;
  alog.clogEnable ( false );

  // change level for module ThreadTest to info
  alog.identLevel ( "ThreadTest", AppLog::levelTranslate ( "warn" ) );

  alog << mod_name << debug << __PRETTY_FUNCTION__ << " Starting t1 warn level" << endl;
  ThreadTest t1 ( 1 );
  t1.start();
  t1.join();
  alog << mod_name << debug << __PRETTY_FUNCTION__ << " t1 finished" << endl;

  alog << mod_name << debug << __PRETTY_FUNCTION__ << " Starting t2 and t3 enabling debug level" << endl;
  alog.identLevel ( "ThreadTest", AppLog::levelTranslate ( "debug" ) );
  ThreadTest t2 ( 2 ), t3 ( 3 );
  t2.start();
  t3.start();
  alog << mod_name << info << __PRETTY_FUNCTION__ << " Waiting 1000 ms" << endl;

  Thread::sleep ( 1000 );

  alog << mod_name << warn << __PRETTY_FUNCTION__ << " end test" << endl;

  alog << mod_name << info << "open a new log file, called testAlog.log" << endl;
  AppLog newLog ( "./testAlog.log" );
  newLog.subscribe();
  newLog << mod_name << info << "This is a new log" << endl;

  // HEXdump testing
  unsigned char buff[10] ;
  for (int i=0; i<10; i++) buff[i] = i+48;

  newLog << info << HEXdump(buff, 10) << std::endl;

  newLog << info <<  __PRETTY_FUNCTION__ << " end test" << endl;

  return 0;
}

