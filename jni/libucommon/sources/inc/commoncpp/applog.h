// Copyright (C) 2005-2010 Angelo Naselli, Penta Engineering s.r.l.
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

/**
 * @file commoncpp/applog.h
 * @short Application logging facilities abstraction.
 **/


#ifndef COMMONCPP_APPLOG_H_
#define COMMONCPP_APPLOG_H_

#ifndef COMMONCPP_SLOG_H_
#include <commoncpp/slog.h>
#endif

#ifndef COMMONCPP_EXCEPTION_H_
#include <commoncpp/exception.h>
#endif

#include <string>
#include <sstream>
#include <iostream>
#include <map>

NAMESPACE_COMMONCPP
using namespace std;
/**
 * Produces a dump of a buffer in a hexdump way with its
 * code Ascii translation and relative buffer address.
 *
 * For instance:
 * 0000000 - 77 98 21 49 0e 00 05 00 40 1c 01 1c 2f 00 00 00 w.!I....@.../...
 *
 */
class __EXPORT HEXdump
{
  protected:
    /**
     * output string
     */
    std::string _str;

  public:
    // max_len: max number of bytes to be printed. 0 prints all.
    /**
     * HEXdump constructor.
     *
     * @param buffer    buffer to be "hexdumped"
     * @param buff_len  buffer length
     * @param max_len   max number of bytes to be "hexdumped". Usefull to
     *                  truncate output. mas_len=0 does prints all.
     */
    HEXdump(const unsigned char *buffer, int buff_len, int max_len = 200);

    /**
     * HEXdump destructor.
     */
    virtual ~HEXdump() { _str = string();}

    /**
     * const char* cast provided for conveneince.
     */

    const char * c_str() const
    {
      return _str.c_str();
    }

    /**
     * string cast provided for conveneince.
     */
    std::string str()
    {
      return _str;
    }

    /**
    * operator <<
    * @param hd hexdump.
    * @return application logger stream
    */
    friend std::ostream& operator<< (std::ostream& out, const HEXdump &hd)
    {
      out << hd.c_str();
      return out;
    }

};

#ifdef  CCXX_EXCEPTIONS
/**
 * Applog exception, used for memory problems at the moment
 *
 */
class __EXPORT AppLogException : public ost::Exception
{
  public:
    /**
     * Constructor.
     * @param what_arg exception string
     */
    AppLogException(String &what_arg) : ost::Exception(what_arg) {};

};
#endif

class AppLogPrivate;

/**
 * Application logger is a class that implements a logger that can be used
 * by applications to save log file somewhere on the system.
 *
 * It uses ost::slog to write to syslog and std::clog to write to standard
 * output.
 *
 * It provides either a stream oriented logger or a old printf style one.
 *
 * It can be used to log directly on a file or in a spooler like way. Latter
 * uses a ost::ThreadQueue to implement a thread safe access to logger.
 *
 * It provides a global stream variable called ost::alog.
 *
 * It provides an AppLog::Ident class that represents a module name for
 * instance that can be used to tag logs. Logging levels are the same
 * defined into ost::Slog:
 * Slog::levelEmergency
 * Slog::levelAlert
 * Slog::levelCritical
 * Slog::levelError
 * Slog::levelWarning
 * Slog::levelNotice
 * Slog::levelInfo
 * Slog::levelDebugfrom.
 *
 * Example of usage: alog << mod_name << debug << "Hello world!" << std::endl;
 */
class __EXPORT AppLog : protected streambuf, public ostream
{
  protected:
    // d pointer
    AppLogPrivate *d;
    void writeLog(bool endOfLine = true);
    static std::map<string, Slog::Level> *assoc;

  public:
    /**
     * Ident class that represents module name.
     */
    class __EXPORT Ident
    {
      private:
        std::string _ident;
      public:

        /**
         * Constructor.
         */
        Ident() {};

        /**
         * Desctructor.
         */
        ~Ident() {};

        /**
         * Copy constructor.
         */
        Ident(Ident& id) {_ident = id._ident;}

        /**
         * const char* constructor, provided for convenience.
         */
        Ident(const char *str) : _ident(str) {};

        /**
         * std::string cast.
         */
        std::string& str() {return _ident;}

        /**
         * Assignment operator (string).
         */
        Ident& operator= (std::string &st) {_ident = st; return *this;}

        /**
         * Assignment operator (const char[]), provided for convenience.
         */
        Ident& operator= (const char str[]) {_ident = str; return *this;}

        /**
         * const char* cast provided for conveneince.
         */
        const char* c_str() {return _ident.c_str();}
    };

#ifndef _MSWINDOWS_
    /**
     * Constructor for a customized logger.
     * @param logFileName log file name.
     * @param logDirectly true to write directly to file, false to use
     *                    a spooler like logger.
     * @param usePipe     true to use pipe instead of file, false otherwise
     */
    AppLog(const char* logFileName = NULL, bool logDirectly = false , bool usePipe = false);
#else
    /**
    * Constructor for a customized logger.
    * @param logFileName log file name.
    * @param logDirectly true to write directly to file, false to use
    *                    a spooler like logger.
    */
    AppLog(const char* logFileName = NULL, bool logDirectly = false);
#endif
    /**
     * Destructor
     */
    virtual ~AppLog();

    /**
     * Subscribes the current thread to logger, it reserves thread safe
     * buffer for it.
     */
    void subscribe();

    /**
     * Unsubscribes the current thread from logger.
     */
    void unsubscribe();

#ifndef _MSWINDOWS_
    /**
     * Allows to set up ost::alog parameters.
     * @param FileName log file name.
     * @param logDirectly true to write directly to file, false to use
     *                    a spooler like logger.
     * @param usePipe     true to use pipe instead of file, false otherwise
     */
    void logFileName(const char* FileName, bool logDirectly = false, bool usePipe = false);
#else
    /**
     * Allows to set up ost::alog parameters.
     * @param FileName log file name.
     * @param logDirectly true to write directly to file, false to use
     *                    a spooler like logger.
     */
    void logFileName(const char* FileName, bool logDirectly = false);
#endif
    /**
     * if logDirectly is set it closes the file.
     */
    void close(void);

    /**
     * Sets the log level.
     * @param enable log level.
     */
    void level(Slog::Level enable);

    /**
     * Enables clog output.
     * @param en true to enable clog output.
     */
    void clogEnable(bool en = true);

    /**
     * Enables slog output for error level messages.
     * @param en true to enable slog output.
     */
    void slogEnable(bool en = true);

    /**
     * Sets the level for that ident.
     * @param ident ident (module name for instance).
     * @param level level
     */
    void identLevel(const char *ident, Slog::Level level);

    /**
     * Opens the file if not already and sets ident
     * @param ident module name for instance.
     */
    void open(const char *ident);

    /**
     * stream overflow() overload.
     * @param c character to be managed
     * @return c
     */
    virtual int overflow(int c);

    /**
     * stream sync() overload
     */
    virtual int sync();

    /**
     * emerg level printf style method, provided for convenience.
     * @param format printf format
     */
    void emerg(const char *format, ...);

    /**
     * alert level printf style method, provided for convenience.
     * @param format printf format
     */
    void alert(const char *format, ...);

    /**
     * critical level printf style method, provided for convenience.
     * @param format printf format
     */
    void critical(const char *format, ...);

    /**
     * error level printf style method, provided for convenience.
     * @param format printf format
     */
    void error(const char *format, ...);

    /**
     * warn level printf style method, provided for convenience.
     * @param format printf format
     */
    void warn(const char *format, ...);

    /**
     * notice level printf style method, provided for convenience.
     * @param format printf format
     */
    void notice(const char *format, ...);

    /**
     * info level printf style method, provided for convenience.
     * @param format printf format
     */
    void info(const char *format, ...);

    /**
     * debug level printf style method, provided for convenience.
     * @param format printf format
     */
    void debug(const char *format, ...);

    /**
     * operator to change ident and log level
     * @param ident ident (module name for instance)
     * @param level new log level
     * @return application logger stream
     */
    AppLog &operator()(const char *ident, Slog::Level level = Slog::levelError);

    /**
     * operator to change ident
     * @param ident ident (module name for instance)
     * @return application logger stream
     */
    inline AppLog& operator()(Ident &ident)
    {
      open(ident.c_str());
      return *this;
    }

    /**
     * operator to change logging level
     * @param level new log level
     * @return application logger stream
     */
    AppLog &operator()(Slog::Level level);

    /**
     * manipulator operator, to change print levels.
     * @param (* pfManipulator)(AppLog &)
     * @return application logger stream
     */
    AppLog& operator<< (AppLog& (*pfManipulator)(AppLog&));

    /**
     * manipulator operator, to use ostream manipulators (i.e. std::endl,...)
     * @param (* pfManipulator)(AppLog &)
     * @return application logger stream
     */
    AppLog& operator<< (ostream& (*pfManipulator)(ostream&));

    friend  ostream& operator << (ostream &os, AppLog & al)
    {
      return al;
    }

    /**
     * operator <<
     * @param ident module name for instance.
     * @return application logger stream
     */
    inline AppLog& operator<< (Ident &ident)
    {
      open(ident.c_str());
      return *this;
    }


    /**
     * warn level
     * @return application logger stream
     */
    inline AppLog &warn(void)
    {return operator()(Slog::levelWarning);}

    /**
     * error level
     * @return application logger stream
     */
    AppLog &error(void)
    { return operator()(Slog::levelError);}

    /**
     * debug level
     * @return application logger stream
     */
    inline AppLog &debug(void)
    {return operator()(Slog::levelDebug);}

    /**
     * emerg level
     * @return application logger stream
     */
    inline AppLog &emerg(void)
    {return operator()(Slog::levelEmergency);}

    /**
     * alert level
     * @return application logger stream
     */
    inline AppLog &alert(void)
    {return operator()(Slog::levelAlert);}

    /**
     * critical level
     * @return application logger stream
     */
    inline AppLog &critical(void)
    {return operator()(Slog::levelCritical);}

    /**
     * notice level
     * @return application logger stream
     */
    inline AppLog &notice(void)
    {return operator()(Slog::levelNotice);}

    /**
     * info level
     * @return application logger stream
     */
    inline AppLog &info(void)
    {return operator()(Slog::levelInfo);}

    /**
     * Translates level from string to Slog::Level, useful for
     * configuration files for instance.
     * Valid level names are:
     * "emerg" for Slog::levelEmergency
     * "alert" for Slog::levelAlert
     * "critical" for Slog::levelCritical
     * "error" for Slog::levelError
     * "warn" for Slog::levelWarning
     * "notice" for Slog::levelNotice
     * "info" for Slog::levelInfo
     * "debug" for Slog::levelDebug
     * @param name Slog Level name
     * @return Slog level value
     */
    static Slog::Level levelTranslate(string name)
    {
	std::map<string, Slog::Level>::iterator  it = assoc->find(name);
	return (it != assoc->end()) ? it->second : Slog::levelEmergency;
    }

};

/**
 * Manipulator for debug level
 * @param sl application logger stream
 * @return application logger stream
 */
__EXPORT inline AppLog &debug(AppLog& sl)
{return sl.operator()(Slog::levelDebug);}

/**
 * Manipulator for warn level
 * @param sl application logger stream
 * @return application logger stream
 */
__EXPORT inline AppLog &warn(AppLog& sl)
{return sl.operator()(Slog::levelWarning);}

/**
 * Manipulator for error level
 * @param sl application logger stream
 * @return application logger stream
 */
__EXPORT inline AppLog &error(AppLog& sl)
{ return sl.operator()(Slog::levelError);}

/**
 * Manipulator for emerg level
 * @param sl application logger stream
 * @return application logger stream
 */
__EXPORT inline AppLog &emerg(AppLog& sl)
{return sl.operator()(Slog::levelEmergency);}

/**
 * Manipulator for alert level
 * @param sl application logger stream
 * @return application logger stream
 */
__EXPORT inline AppLog &alert(AppLog& sl)
{return sl.operator()(Slog::levelAlert);}

/**
 * Manipulator for critical level
 * @param sl application logger stream
 * @return application logger stream
 */
__EXPORT inline AppLog &critical(AppLog& sl)
{return sl.operator()(Slog::levelCritical);}

/**
  * Manipulator for notice level
  * @param sl application logger stream
  * @return application logger stream
  */
__EXPORT inline AppLog &notice(AppLog& sl)
{return sl.operator()(Slog::levelNotice);}

/**
 * Manipulator for info level
 * @param sl application logger stream
 * @return application logger stream
 */
__EXPORT inline AppLog &info(AppLog& sl)
{return sl.operator()(Slog::levelInfo);}

/**
 * alog global log stream definition
 */
__EXPORT extern AppLog alog;

END_NAMESPACE

#endif //___APPLOG_H___
