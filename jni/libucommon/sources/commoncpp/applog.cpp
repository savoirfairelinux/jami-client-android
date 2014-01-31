// Copyright (C) 2005-2009 Angelo Naselli, Penta Engineering s.r.l.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// Common C++.  If you copy code from other releases into a copy of GNU
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

#ifdef  NEW_STDCPP

#include <ucommon-config.h>
#include <commoncpp/config.h>
#include <commoncpp/thread.h>
#include <commoncpp/slog.h>
#ifndef _MSWINDOWS_
#include <sys/types.h>
#include <sys/stat.h>
#endif
#include <string>
#include <iomanip>
#include <iostream>
#include <fstream>
#include <cstdio>
#include <cstdlib>
#include <stdarg.h>
#include <errno.h>

// TODO sc: test if has to move up now that it is into commoncpp
// NOTE: the order of inclusion is important do not move following include line
// redefinition of __EXPORT needed if we're compiling our dll
#include <commoncpp/export.h>
// local includes
#include <commoncpp/applog.h>

using namespace std;
using namespace COMMONCPP_NAMESPACE;

class logStruct
{
  public:
    string       _ident;
    int          _priority;
    Slog::Level  _level;
    bool         _enable;
    bool         _clogEnable;
    bool         _slogEnable;
    size_t       _msgpos;

    enum logEnum
    {
      BUFF_SIZE = 512,
      LAST_CHAR = BUFF_SIZE - 1
    };
    char         _msgbuf[BUFF_SIZE];

    logStruct() :  _ident("") ,  _priority(Slog::levelDebug),
        _level(Slog::levelDebug), _enable(false),
        _clogEnable(false), _slogEnable(false), _msgpos(0)
    {
      memset(_msgbuf, 0, BUFF_SIZE);
    };

    ~logStruct() {};
};

struct levelNamePair
{
  const char *name;
  Slog::Level level;
};

#ifdef _MSWINDOWS_
template class std::map<string, Slog::Level >;
#endif

class LevelName : public std::map<string, Slog::Level>
{
  public:

    LevelName(const levelNamePair initval[], int num)
    {
      for (int i = 0; i < num; i++)
        insert(make_pair(initval[i].name, initval[i].level));
    };
};

class logger : public ost::ThreadQueue
{
  private:
    string       _nomeFile;
    std::fstream _logfs;
    bool         _usePipe;
    bool         _closedByApplog;

  protected:
    // to dequeue log messages and write them to file if not log_directly
    virtual void  runQueue(void *data);
    virtual void  startQueue(void);
    virtual void  stopQueue(void);
    virtual void  onTimer(void);
    virtual void  final(void);
            void _openFile();
  
  public:
    logger(const char* logFileName = NULL, bool usePipe = false);
    virtual ~logger();

    // To change log file name
    void logFileName(const char* FileName, bool usePipe = false);

    void openFile();
    void closeFile();

};


// mapping thread ID <-> logStruct (buffer)
typedef std::map <cctid_t, logStruct> LogPrivateData;
// map ident <-> levels
typedef std::map <string, Slog::Level> IdentLevel;


NAMESPACE_COMMONCPP
class AppLogPrivate
{
  public:
    // subscription and unsubsciption must be protected as well
    ost::Mutex _subMutex;
    // mapping thread ID <-> logStruct (buffer)
    LogPrivateData _logs;
    // map ident <-> levels
    IdentLevel     _identLevel;
    // log directly into file
    bool           _logDirectly;
    bool           _logPipe;
    // log spooler
    logger         *_pLogger;

    string        _nomeFile;
    Mutex         _lock;
    std::fstream  _logfs;

    static const levelNamePair _values[];
    static LevelName           _assoc;

    AppLogPrivate() : _pLogger(NULL) {}

    ~AppLogPrivate()
    {
      if (_pLogger)
        delete _pLogger;
    }
};
END_NAMESPACE

const levelNamePair AppLogPrivate::_values[] =
{
  { "emerg", Slog::levelEmergency },
  { "alert", Slog::levelAlert },
  { "critical", Slog::levelCritical },
  { "error", Slog::levelError },
  { "warn", Slog::levelWarning },
  { "notice", Slog::levelNotice },
  { "info", Slog::levelInfo },
  { "debug", Slog::levelDebug }
};

AppLog alog;

LevelName AppLogPrivate::_assoc(_values, sizeof AppLogPrivate::_values / sizeof *AppLogPrivate::_values);
std::map<string, Slog::Level> *AppLog::assoc = &AppLogPrivate::_assoc;

#if defined(CCXX_EXCEPTIONS)

HEXdump::HEXdump(const unsigned char *buffer, int len, int max_len) : _str()
{
  std::stringstream sstr;


  if (buffer == NULL || len <= 0)
    return ;

  long buf_len = (max_len > 0 && len > max_len) ? max_len : len;
  long int addr = 0;
  int cnt2 = 0;
  int n;
  int i;

  sstr.str("");
  // get exception from ifstream failures
  sstr.exceptions(ifstream::failbit | ifstream::badbit);
  try
  {
    sstr << std::endl;
    sstr << "dump " << len << " byte." << std::endl;

    for (n = 0; n < buf_len; n++)
    {
      if (cnt2 == 0)
      {
        //  Print address.
        sstr << std::setw(7) << std::setfill('0') <<  int (addr) << " - ";
        addr = addr + 16;
      }
      cnt2 = (cnt2 + 1) % 18;
      if (cnt2 <= 16)
      {
        // print hex value
        sstr << std::hex << std::setw(2) << std::setfill('0') <<  int (buffer[n]) << " ";
      }
      else
      {
        sstr << "  ";
        sstr << std::setfill(' ');
        for (i = n - cnt2 + 1; i < n; i++)
        {
          // print ascii value
          if (buffer[i] < 32 || 126 < buffer[i])
          {
            sstr << '.';
          }
          else
          {
            sstr << buffer[i];
          }
        }
        sstr << std::endl;
        sstr << std::dec;
        cnt2 = 0;
        n--;
      }
    }

    sstr << std::setfill(' ');

    for (i = cnt2 + 1; i <= 16 ; i++)
    {
      sstr << std::setw(2) << "--" << " ";
    }
    sstr << "  ";

    for (i = n - cnt2; cnt2 <= 16 && i < n; i++)
    {
      if (buffer[i] < 32 || 126 < buffer[i])
      {
        sstr << '.';
      }
      else
      {
        sstr << buffer[i];
      }
    }
    sstr << std::dec;
    if (max_len > 0 && len > max_len)
      sstr << std::endl << "dump troncato a " << max_len << " byte." << std::endl;
  }
  catch (...)
  {
    sstr.str("HEXdump failed!");
  }

  _str = sstr.str();
}

#endif

// class logger
logger::logger(const char* logFileName, bool usePipe)  : ThreadQueue(NULL, 0, 0), _usePipe(usePipe), _closedByApplog(false)
{
  _nomeFile = "";

  if (logFileName)
    _nomeFile = logFileName;
 
  openFile();
}

logger::~logger()
{
  Semaphore::post();
  Thread::terminate();

  _logfs.flush();
  _logfs.close();
}

// New log file name
void logger::logFileName(const char* FileName, bool usePipe)
{
  if (!FileName)
    return;

  _usePipe = usePipe;
  _nomeFile = FileName;
  if (_logfs.is_open())
    _logfs.close();

  openFile();
}

/// open also logger if applog->open() is invoked
void logger::openFile()
{
  _closedByApplog=false;
}

///internal logger openFile needed to use pipe and avoid stream buffering in the case
/// the consumer is not connected to pipe
void logger::_openFile()
{
  if (!_closedByApplog && !_logfs.is_open())
  {
    if (!_nomeFile.empty())
    {
      _logfs.clear();
      if (!_usePipe)
      {
        _logfs.open(_nomeFile.c_str(), std::ofstream::out | std::ofstream::app | std::ofstream::ate);
      }
#ifndef _MSWINDOWS_
      else
      {
        // create pipe
        int err = mkfifo(_nomeFile.c_str(), S_IREAD | S_IWRITE);
        if (err == 0 || errno == EEXIST)
        {
          // and open it
          _logfs.open(_nomeFile.c_str(), std::fstream::in | std::fstream::out);
        }
        else
          THROW(AppLogException("Can't create pipe"));
      }
#endif
      if (_logfs.fail())
        THROW(AppLogException("Can't open log file name"));
    }
  }
}

/// close also logger if applog->close() is invoked
void logger::closeFile()
{
   _closedByApplog = true;
}
    

// writes into filename enqueued messages
void logger::runQueue(void * data)
{
  char *str = (char *) data;

  // if for some internal reasons file has been closed
  // reopen it
  try
  {
    _openFile();
  }
  catch (AppLogException e)
  {
    std::cerr << e.what() << std::endl;
    slog.emerg("%s\n", e.what());
    std::cerr.flush();
  }
  
  if (_logfs.is_open())
  {
    _logfs << str;
    _logfs.flush();
  }
  
  //if we use a pipe to avoid increasing of stream buffer
  // without a consumer, we open, use and close it
  if ((_usePipe || _closedByApplog) && _logfs.is_open())
  {
    _logfs.flush();
    _logfs.close();
  }
}

void logger::startQueue()
{
}

void logger::stopQueue()
{
}

void logger::onTimer()
{
}

void logger::final()
{
  if (started)
  {
    data_t *pFirst = first;
    while (pFirst)
    {
      runQueue(pFirst->data);
      pFirst = pFirst->next;
    }
  }
}

#ifndef _MSWINDOWS_
AppLog::AppLog(const char* logFileName, bool logDirectly, bool usePipe) :
    streambuf(), ostream((streambuf*) this)
#else
AppLog::AppLog(const char* logFileName, bool logDirectly) :
    streambuf(), ostream((streambuf*) this)
#endif
{
  d= NULL; // pedantic fussy about initing members before base classes...
  d = new AppLogPrivate();
  if (!d)
    THROW(AppLogException("Memory allocation problem"));

  d->_nomeFile = "";
  d->_logDirectly = logDirectly;
#ifndef _MSWINDOWS_
  d->_logPipe = usePipe;
#else
  d->_logPipe = false;
#endif
  // level string to level value
  //   assoc["emerg"]    = levelEmergency;
  //   assoc["alert"]    = levelAlert;
  //   assoc["critical"] = levelCritical;
  //   assoc["error"]    = levelError;
  //   assoc["warn"]     = levelWarning;
  //   assoc["notice"]   = levelNotice;
  //   assoc["info"]     = levelInfo;
  //   assoc["debug"]    = levelDebug;

  if (logFileName)
    d->_nomeFile = logFileName;

  if (!d->_logDirectly && logFileName)
    d->_pLogger = new logger(logFileName, d->_logPipe);
  else
    d->_pLogger = NULL;

  // writes to file directly
  if (!d->_nomeFile.empty() && d->_logDirectly)
  {
    if (!d->_logPipe)
    {
      d->_logfs.open(d->_nomeFile.c_str(), std::fstream::in | std::fstream::out);

      if (!d->_logfs.is_open())
      {
        d->_logfs.open(d->_nomeFile.c_str(), std::fstream::out | std::fstream::app);
      }
      else
        d->_logfs.seekg(0, std::fstream::end);
    }
// on Windows pipe are not used as they are not supported on WinNT
#ifndef _MSWINDOWS_
    else
    {
      // create pipe
      int err = mkfifo(d->_nomeFile.c_str(), S_IREAD | S_IWRITE);
      if (err == 0 || errno == EEXIST)
      {
        // and open it
        d->_logfs.open(d->_nomeFile.c_str(), std::fstream::in | std::fstream::out);
      }
      else
        THROW(AppLogException("Can't create pipe"));
    }
#endif
    if (d->_logfs.fail())
      THROW(AppLogException("Can't open log file name"));
  }

  //from Error level on write to syslog also
  slog.level(Slog::levelError);
  slog.clogEnable(false);
}

AppLog::~AppLog()
{
  // if _logDirectly
  close();
  if (d) delete d;
}

void AppLog::subscribe()
{
  ost::MutexLock mtx(d->_subMutex);

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
    {
      // subscribes new thread
      d->_logs[tid];
    }
  }
}

void AppLog::unsubscribe()
{
  ost::MutexLock mtx(d->_subMutex);

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt != d->_logs.end())
    {
      // unsubscribes thread
      d->_logs.erase(logIt);
    }
  }
}

#ifndef _MSWINDOWS_
void AppLog::logFileName(const char* FileName, bool logDirectly, bool usePipe)
#else
void AppLog::logFileName(const char* FileName, bool logDirectly)
#endif
{
  if (!FileName)
  {
    slog.error("Null file name!");
    return;
  }

  d->_lock.enterMutex();
  d->_nomeFile = FileName;
  close();
  d->_logDirectly = logDirectly;
#ifndef _MSWINDOWS_
  d->_logPipe = usePipe;
#else
  d->_logPipe = false;
#endif
  if (!d->_logDirectly)
  {
    if (d->_pLogger)
      d->_pLogger->logFileName(FileName, d->_logPipe);
    else
      d->_pLogger = new logger(FileName, d->_logPipe);

    d->_lock.leaveMutex();
    return;
  }

  // log directly
  if (!d->_nomeFile.empty())
  {
    if (!d->_logPipe)
    {
      d->_logfs.open(d->_nomeFile.c_str(), std::fstream::out | std::fstream::app);
    }
#ifndef _MSWINDOWS_
    else
    {
      // create pipe
      int err = mkfifo(d->_nomeFile.c_str(), S_IREAD | S_IWRITE);
      if (err == 0 || errno == EEXIST)
      {
        // and open it
        d->_logfs.open(d->_nomeFile.c_str(), std::fstream::in | std::fstream::out);
      }
      else
        THROW(AppLogException("Can't create pipe"));
    }
#endif
    if (d->_logfs.fail())
      THROW(AppLogException("Can't open log file name"));
  }
  d->_lock.leaveMutex();
}

// writes to log
void AppLog::writeLog(bool endOfLine)
{
  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    if ((d->_logDirectly && !d->_logfs.is_open() && !logIt->second._clogEnable) ||
        (!d->_logDirectly && !d->_pLogger && !logIt->second._clogEnable))

    {
      logIt->second._msgpos = 0;
      logIt->second._msgbuf[0] = '\0';
      return;
    }

    if (logIt->second._enable)
    {
      time_t now;
      struct tm *dt;
      time(&now);
      struct timeval detail_time;
      gettimeofday(&detail_time, NULL);
      dt = localtime(&now);
      char buf[50];

      const char *p = "unknown";
      switch (logIt->second._priority)
      {
        case Slog::levelEmergency:
          p = "emerg";
          break;
        case Slog::levelInfo:
          p = "info";
          break;
        case Slog::levelError:
          p = "error";
          break;
        case Slog::levelAlert:
          p = "alert";
          break;
        case Slog::levelDebug:
          p = "debug";
          break;
        case Slog::levelNotice:
          p = "notice";
          break;
        case Slog::levelWarning:
          p = "warn";
          break;
        case Slog::levelCritical:
          p = "crit";
          break;
      }

      snprintf(buf, sizeof(buf) - 1, "%04d-%02d-%02d %02d:%02d:%02d.%03d ",
               dt->tm_year + 1900, dt->tm_mon + 1, dt->tm_mday,
               dt->tm_hour, dt->tm_min, dt->tm_sec, (int)(detail_time.tv_usec / 1000));

      buf[sizeof(buf)-1] = 0;    // per sicurezza

      if (d->_logDirectly)
      {
        d->_lock.enterMutex();
        if (d->_logfs.is_open())
        {
          d->_logfs << buf;
          if (!logIt->second._ident.empty())
            d->_logfs << logIt->second._ident.c_str() << ": ";
          d->_logfs << "[" << p << "] ";

          d->_logfs << logIt->second._msgbuf;

          if (endOfLine)
            d->_logfs << endl;

          d->_logfs.flush();
        }
      }
      else if (d->_pLogger)
      {
        // ThreadQueue
        std::stringstream sstr;
        sstr.str("");    // reset contents
        sstr << buf;

        if (!logIt->second._ident.empty())
          sstr << logIt->second._ident.c_str() << ": ";
        sstr << "[" << p << "] ";

        sstr << logIt->second._msgbuf;
        if (endOfLine)
          sstr << endl;
        sstr.flush();

        if (sstr.fail())
          cerr << "stringstream failed!!!! " << endl;

        // enqueues log message
        d->_pLogger->post((void *) sstr.str().c_str(), sstr.str().length() + 1);

        d->_lock.enterMutex();
      }

      // slog it if error level is right
      if (logIt->second._slogEnable && logIt->second._priority <= Slog::levelError)
      {
        slog((Slog::Level) logIt->second._priority) << logIt->second._msgbuf;
        if (endOfLine) slog << endl;
      }
      if (logIt->second._clogEnable
#ifndef _MSWINDOWS_
          && (getppid() > 1)
#endif
         )
      {
        clog << logIt->second._msgbuf;
        if (endOfLine)
          clog << endl;
      }

      d->_lock.leaveMutex();
    }

    logIt->second._msgpos = 0;
    logIt->second._msgbuf[0] = '\0';
  }
}

void AppLog::close(void)
{
  if (d->_logDirectly)
  {
    d->_lock.enterMutex();
    if (d->_logfs.is_open())
    {
      d->_logfs.flush();
      d->_logfs.close();
    }
    d->_lock.leaveMutex();
  }
  else
  {
    if (d->_pLogger)
      d->_pLogger->closeFile();
  }
}

void AppLog::open(const char *ident)
{
  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    if (d->_nomeFile.empty())
    {
      std::cerr << "Empty file name" << std::endl;
      slog.emerg("Empty file nane!\n");
    }
    if (d->_logDirectly)
    {
      d->_lock.enterMutex();
      if (!d->_logfs.is_open())
      {
        d->_logfs.open(d->_nomeFile.c_str(), std::fstream::out | std::fstream::app);
      }
      if (!d->_logfs.is_open())
      {
        std::cerr << "Can't open file name!" << std::endl;
        slog.emerg("Can't open file name!\n");
      }
      d->_lock.leaveMutex();
    }
    else
    {
      if (d->_pLogger)
      	d->_pLogger->openFile();
    }
    if (ident != NULL)
      logIt->second._ident = ident;

  }
}

void AppLog::identLevel(const char *ident, Slog::Level level)
{
  if (!ident)
    return;

  string id = ident;

  IdentLevel::iterator idLevIt = d->_identLevel.find(id);
  if (idLevIt == d->_identLevel.end())
  {
    d->_identLevel[id] = level;
  }
  else
    idLevIt->second = level;

}

void AppLog::level(Slog::Level enable)
{
  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;
    logIt->second._level = enable;
  }
}

void AppLog::clogEnable(bool f)
{
  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;
    logIt->second._clogEnable = f;
  }
}

void AppLog::slogEnable(bool en)
{
  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    logIt->second._slogEnable = en;
  }
}

int AppLog::sync()
{
  int retVal = (pbase() != pptr());

  if (fail())
  {
    slog(Slog::levelNotice) << "fail() is true, calling clear()" << endl;
    clear();
  }

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();
    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt != d->_logs.end())
    {
      retVal = (logIt->second._msgpos > 0);
      if (retVal)
      {
        slog(Slog::levelNotice) << "sync called and msgpos > 0" << endl;
      }
    }
  }

  overflow(EOF);

  return retVal;

}

int AppLog::overflow(int c)
{
  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return c;
    if (!logIt->second._enable)
      return c;

    if (c == '\n' || !c || c == EOF)
    {
      if (!logIt->second._msgpos)
      {
        if (c == '\n') writeLog(true);
        return c;
      }
      if (logIt->second._msgpos < (int)(sizeof(logIt->second._msgbuf) - 1))
        logIt->second._msgbuf[logIt->second._msgpos] = 0;
      else
        logIt->second._msgbuf[logIt->second._msgpos-1] = 0;

      writeLog(c == '\n');
      //reset buffer
      logIt->second._msgpos = 0;

      return c;
    }

    if (logIt->second._msgpos < (int)(sizeof(logIt->second._msgbuf) - 1))
      logIt->second._msgbuf[logIt->second._msgpos++] = c;

  }

  return c;
}

void AppLog::error(const char *format, ...)
{
  va_list args;

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    error();
    if (!logIt->second._enable)
      return;
    overflow(EOF);

    va_start(args, format);
    logIt->second._msgbuf[logStruct::BUFF_SIZE-1] = '\0';
    logIt->second._msgpos = vsnprintf(logIt->second._msgbuf, logStruct::BUFF_SIZE, format, args);
    if (logIt->second._msgpos > logStruct::BUFF_SIZE - 1) logIt->second._msgpos = logStruct::BUFF_SIZE - 1;
    overflow(EOF);
    if (logIt->second._slogEnable)
      slog.error(logIt->second._msgbuf);
    va_end(args);
  }
}

void AppLog::warn(const char *format, ...)
{
  va_list args;

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    warn();
    if (!logIt->second._enable)
      return;
    overflow(EOF);

    va_start(args, format);
    logIt->second._msgbuf[logStruct::BUFF_SIZE-1] = '\0';
    logIt->second._msgpos = vsnprintf(logIt->second._msgbuf, logStruct::BUFF_SIZE, format, args);
    if (logIt->second._msgpos > logStruct::BUFF_SIZE - 1) logIt->second._msgpos = logStruct::BUFF_SIZE - 1;
    overflow(EOF);
    if (logIt->second._slogEnable)
      slog.warn(logIt->second._msgbuf);
    va_end(args);
  }
}

void AppLog::debug(const char *format, ...)
{
  va_list args;

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    debug();
    if (!logIt->second._enable)
      return;
    overflow(EOF);

    va_start(args, format);
    logIt->second._msgbuf[logStruct::BUFF_SIZE-1] = '\0';
    logIt->second._msgpos = vsnprintf(logIt->second._msgbuf, logStruct::BUFF_SIZE, format, args);
    if (logIt->second._msgpos > logStruct::BUFF_SIZE - 1) logIt->second._msgpos = logStruct::BUFF_SIZE - 1;
    overflow(EOF);
    va_end(args);
  }
}

void AppLog::emerg(const char *format, ...)
{
  va_list args;

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    emerg();
    if (!logIt->second._enable)
      return;
    overflow(EOF);

    va_start(args, format);
    logIt->second._msgbuf[logStruct::BUFF_SIZE-1] = '\0';
    logIt->second._msgpos = vsnprintf(logIt->second._msgbuf, logStruct::BUFF_SIZE, format, args);
    if (logIt->second._msgpos > logStruct::BUFF_SIZE - 1) logIt->second._msgpos = logStruct::BUFF_SIZE - 1;
    overflow(EOF);
    if (logIt->second._slogEnable)
      slog.emerg(logIt->second._msgbuf);
    va_end(args);
  }
}

void AppLog::alert(const char *format, ...)
{
  va_list args;

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    alert();
    if (!logIt->second._enable)
      return;
    overflow(EOF);

    va_start(args, format);
    logIt->second._msgbuf[logStruct::BUFF_SIZE-1] = '\0';
    logIt->second._msgpos = vsnprintf(logIt->second._msgbuf, logStruct::BUFF_SIZE, format, args);
    if (logIt->second._msgpos > logStruct::BUFF_SIZE - 1) logIt->second._msgpos = logStruct::BUFF_SIZE - 1;
    overflow(EOF);
    if (logIt->second._slogEnable)
      slog.alert(logIt->second._msgbuf);
    va_end(args);
  }
}

void AppLog::critical(const char *format, ...)
{
  va_list args;

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    critical();
    if (!logIt->second._enable)
      return;
    overflow(EOF);

    va_start(args, format);
    logIt->second._msgbuf[logStruct::BUFF_SIZE-1] = '\0';
    logIt->second._msgpos = vsnprintf(logIt->second._msgbuf, logStruct::BUFF_SIZE, format, args);
    if (logIt->second._msgpos > logStruct::BUFF_SIZE - 1) logIt->second._msgpos = logStruct::BUFF_SIZE - 1;
    overflow(EOF);
    if (logIt->second._slogEnable)
      slog.critical(logIt->second._msgbuf);
    va_end(args);
  }
}

void AppLog::notice(const char *format, ...)
{
  va_list args;

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    notice();
    if (!logIt->second._enable)
      return;
    overflow(EOF);

    va_start(args, format);
    logIt->second._msgbuf[logStruct::BUFF_SIZE-1] = '\0';
    logIt->second._msgpos = vsnprintf(logIt->second._msgbuf, logStruct::BUFF_SIZE, format, args);
    if (logIt->second._msgpos > logStruct::BUFF_SIZE - 1) logIt->second._msgpos = logStruct::BUFF_SIZE - 1;
    overflow(EOF);
    if (logIt->second._slogEnable)
      slog.notice(logIt->second._msgbuf);
    va_end(args);
  }
}

void AppLog::info(const char *format, ...)
{
  va_list args;

  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return;

    info();
    if (!logIt->second._enable)
      return;
    overflow(EOF);

    va_start(args, format);
    logIt->second._msgbuf[logStruct::BUFF_SIZE-1] = '\0';
    logIt->second._msgpos = vsnprintf(logIt->second._msgbuf, logStruct::BUFF_SIZE, format, args);
    if (logIt->second._msgpos > logStruct::BUFF_SIZE - 1) logIt->second._msgpos = logStruct::BUFF_SIZE - 1;
    overflow(EOF);
    va_end(args);
  }
}

AppLog &AppLog::operator()(Slog::Level lev)
{
  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return *this;

    // needed? overflow(EOF);

    // enables log
    Slog::Level th_lev = logIt->second._level;
    logIt->second._enable = (th_lev >= lev);
    // is there a log level per module?
    if (!logIt->second._ident.empty())
    {
      std::string st = logIt->second._ident;
      IdentLevel::iterator idLevIt = d->_identLevel.find(st);
      if (idLevIt != d->_identLevel.end())
      {
        th_lev = idLevIt->second;
        logIt->second._enable = (th_lev >= lev);
      }
    }

    logIt->second._priority = lev;
  }

  return *this;
}

AppLog &AppLog::operator()(const char *ident, Slog::Level lev)
{
  Thread *pThr = getThread();
  if (pThr)
  {
    cctid_t tid =  pThr->getId();

    LogPrivateData::iterator logIt = d->_logs.find(tid);
    if (logIt == d->_logs.end())
      return this->operator()(lev);

    logIt->second._enable =  true;
    open(ident);
  }

  return this->operator()(lev);
}


AppLog& AppLog::operator<< (AppLog& (*pfManipulator)(AppLog&))
{
  return (*pfManipulator)(*this);
}

AppLog& AppLog::operator<< (ostream& (*pfManipulator)(ostream&))
{
  (*pfManipulator)(*(dynamic_cast<ostream*>(this)));

  return  *this ;
}

#endif

