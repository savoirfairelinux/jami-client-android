// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
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
 * @file numbers.h
 * @short Numbers and dates manipulation.
 **/

#ifndef CCXX_NUMBERS_H_
#define CCXX_NUMBERS_H_

#ifndef CCXX_THREAD_H_
#include <cc++/thread.h>
#endif

#ifndef CCXX_MISSING_H_
#include <cc++/missing.h>
#endif

#ifndef CCXX_STRCHAR_H_
#include <cc++/strchar.h>
#endif

#ifndef CCXX_STRING_H_
#include <cc++/string.h>
#endif

#ifndef CCXX_THREAD_H_
#include <cc++/thread.h>
#endif

#include <ctime>

#ifdef  CCXX_NAMESPACES
namespace ost {
#ifdef __BORLANDC__
    using std::tm;
    using std::time_t;
#endif
#endif

/**
 * A number manipulation class.  This is used to extract, convert,
 * and manage simple numbers that are represented in C ascii strings
 * in a very quick and optimal way.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short number manipulation.
 */
class __EXPORT Number
{
protected:
    char *buffer;
    unsigned size;

public:
    /**
     * Create an instance of a number.
     * @param buffer or NULL if created internally.
     * @param size use - values for zero filled.
     */
    Number(char *buffer, unsigned size);

    void setValue(long value);
    const char *getBuffer() const
        {return buffer;};

    long getValue() const;

    long operator()()
        {return getValue();};

    operator long()
        {return getValue();};

    operator char*()
        {return buffer;};

    long operator=(const long value);
    long operator+=(const long value);
    long operator-=(const long value);
    long operator--();
    long operator++();
    int operator==(const Number &num);
    int operator!=(const Number &num);
    int operator<(const Number &num);
    int operator<=(const Number &num);
    int operator>(const Number &num);
    int operator>=(const Number &num);

    friend long operator+(const Number &num, const long val);
    friend long operator+(const long val, const Number &num);
    friend long operator-(const Number &num, long val);
    friend long operator-(const long val, const Number &num);
};

class __EXPORT ZNumber : public Number
{
public:
    ZNumber(char *buf, unsigned size);
    void setValue(long value);
    long operator=(long value);
};

/**
 * The Date class uses a julian date representation of the current
 * year, month, and day.  This is then manipulated in several forms
 * and may be exported as needed.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short julian number based date class.
 */
class __EXPORT Date
{
protected:
    long julian;

protected:
    void toJulian(long year, long month, long day);
    void fromJulian(char *buf) const;

    /**
     * A method to use to "post" any changed values when shadowing
     * a mixed object class.  This is used by DateNumber.
     */
    virtual void update(void);

public:

    Date(time_t tm);
    Date(tm *dt);
    Date(char *str, size_t size = 0);
    Date(int year, unsigned month, unsigned day);
    Date();
    virtual ~Date();

    int getYear(void) const;
    unsigned getMonth(void) const;
    unsigned getDay(void) const;
    unsigned getDayOfWeek(void) const;
    char *getDate(char *buffer) const;
    time_t getDate(void) const;
    time_t getDate(tm *buf) const;
    long getValue(void) const;
    void setDate(const char *str, size_t size = 0);
    bool isValid(void) const;

    friend Date operator+(const Date &date, const long val);
    friend Date operator-(const Date &date, const long val);
    friend Date operator+(const long val, const Date &date);
    friend Date operator-(const long val, const Date &date);

    operator long() const
        {return getValue();};

    String operator()() const;
    Date& operator++();
    Date& operator--();
    Date& operator+=(const long val);
    Date& operator-=(const long val);
    int operator==(const Date &date);
    int operator!=(const Date &date);
    int operator<(const Date &date);
    int operator<=(const Date &date);
    int operator>(const Date &date);
    int operator>=(const Date &date);
    bool operator!() const
        {return !isValid();};
};

/**
 * The Time class uses a integer representation of the current
 * time.  This is then manipulated in several forms
 * and may be exported as needed.
 *
 * @author Marcelo Dalmas <mad@brasmap.com.br>
 * @short Integer based time class.
 */

class __EXPORT Time
{
protected:
    long seconds;

protected:
    void toSeconds(int hour, int minute, int second);
    void fromSeconds(char *buf) const;
    virtual void update(void);

public:
    Time(time_t tm);
    Time(tm *dt);
    Time(char *str, size_t size = 0);
    Time(int hour, int minute, int second);
    Time();
    virtual ~Time();

    long getValue(void) const;
    int getHour(void) const;
    int getMinute(void) const;
    int getSecond(void) const;
    char *getTime(char *buffer) const;
    time_t getTime(void) const;
    tm *getTime(tm *buf) const;
    void setTime(char *str, size_t size = 0);
    bool isValid(void) const;

    friend Time operator+(const Time &time1, const Time &time2);
    friend Time operator-(const Time &time1, const Time &time2);
    friend Time operator+(const Time &time, const int val);
    friend Time operator-(const Time &time, const int val);
    friend Time operator+(const int val, const Time &time);
    friend Time operator-(const int val, const Time &time);

    operator long()
        {return getValue();};

    String operator()() const;
    Time& operator++();
    Time& operator--();
    Time& operator+=(const int val);
    Time& operator-=(const int val);
    int operator==(const Time &time);
    int operator!=(const Time &time);
    int operator<(const Time &time);
    int operator<=(const Time &time);
    int operator>(const Time &time);
    int operator>=(const Time &time);
    bool operator!() const
        {return !isValid();};
};

/**
 * The Datetime class uses a julian date representation of the current
 * year, month, and day and a integer representation of the current
 * time.  This is then manipulated in several forms
 * and may be exported as needed.
 *
 * @author Marcelo Dalmas <mad@brasmap.com.br>
 * @short Integer based time class.
 */

class __EXPORT Datetime : public Date, public Time
{
  public:
    Datetime(time_t tm);
    Datetime(tm *dt);
    Datetime(const char *str, size_t size = 0);
    Datetime(int year, unsigned month, unsigned day, int hour, int minute, int second);
    Datetime();
    virtual ~Datetime();

    char *getDatetime(char *buffer) const;
    time_t getDatetime(void) const;
    bool isValid(void) const;

    Datetime& operator=(const Datetime datetime);
    Datetime& operator+=(const Datetime &datetime);
    Datetime& operator-=(const Datetime &datetime);
    Datetime& operator+=(const Time &time);
    Datetime& operator-=(const Time &time);

    int operator==(const Datetime&);
    int operator!=(const Datetime&);
    int operator<(const Datetime&);
    int operator<=(const Datetime&);
    int operator>(const Datetime&);
    int operator>=(const Datetime&);
    bool operator!() const;

    String strftime(const char *format) const;
};

/**
 * A number class that manipulates a string buffer that is also a date.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short a number that is also a date string.
 */
class __EXPORT DateNumber : public Number, public Date
{
protected:
    void update(void)
        {fromJulian(buffer);};

public:
    DateNumber(char *buffer);
    virtual ~DateNumber();
};

#ifdef  CCXX_NAMESPACES
}
#endif

#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */

