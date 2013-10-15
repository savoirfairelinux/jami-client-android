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

#include <cc++/config.h>
#include <cc++/exception.h>
#include <cc++/thread.h>
#include <cc++/export.h>
#include <cc++/numbers.h>
#include <cstdlib>
#include <cstdio>
#include <iostream>
#include <ctime>
#include <cmath>

#ifdef  WIN32
#include "time.h"
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#ifdef __BORLANDC__
using std::time_t;
using std::tm;
using std::localtime;
#endif
#endif

Date::Date()
{
    time_t now = SysTime::getTime();
    struct tm dt;
       SysTime::getLocalTime(&now, &dt);

    toJulian(dt.tm_year + 1900, dt.tm_mon + 1, dt.tm_mday);
}

Date::Date(struct tm *dt)
{
    toJulian(dt->tm_year + 1900, dt->tm_mon + 1, dt->tm_mday);
}

Date::Date(time_t tm)
{
    struct tm dt;
       SysTime::getLocalTime(&tm, &dt);
    toJulian(dt.tm_year + 1900, dt.tm_mon + 1, dt.tm_mday);
}

Date::Date(char *str, size_t size)
{
    setDate(str, size);
}

Date::~Date()
{
}

void Date::setDate(const char *str, size_t size)
{
    time_t now = SysTime::getTime();
    struct tm dt;
       SysTime::getLocalTime(&now, &dt);
    int year = 0;
    const char *mstr = str;
    const char *dstr = str;

    if(!size)
        size = strlen(str);
//0000
    if(size == 4) {
#ifdef DEBUG
        cout << "Date::SetDate(): 0000" << endl;
#endif
        year = dt.tm_year + 1900;
        mstr = str;
        dstr = str + 2;
    }
//00/00
    else if(size == 5) {
#ifdef DEBUG
        cout << "Date::SetDate(): 00/00" << endl;
#endif
        year = dt.tm_year + 1900;
        mstr = str;
        dstr = str + 3;
    }
//000000
    else if(size == 6) {
#ifdef DEBUG
        cout << "Date::SetDate(): 000000" << endl;
#endif
        ZNumber nyear((char*)str, 2);
        year = ((dt.tm_year + 1900) / 100) * 100 + nyear();
        mstr = str + 2;
        dstr = str + 4;
    }
//00000000
    else if(size == 8 && str[2] >= '0' && str[2] <= '9' && str[5] >= '0' && str[5] <= '9') {
#ifdef DEBUG
        cout << "Date::SetDate(): 00000000" << endl;
#endif
        ZNumber nyear((char*)str, 4);
        year = nyear();
        mstr = str + 4;
        dstr = str + 6;
    }
//00/00/00
    else if(size == 8) {
#ifdef DEBUG
        cout << "Date::SetDate(): 00/00/00" << endl;
#endif
        ZNumber nyear((char*)str, 2);
        year = ((dt.tm_year + 1900) / 100) * 100 + nyear();
        mstr = str + 3;
        dstr = str + 6;
    }

//0000/00/00
    else if(size == 10) {
#ifdef DEBUG
        cout << "Date::SetDate(): 0000-00-00" << endl;
#endif
        ZNumber nyear((char*)str, 4);
        year = nyear();
        mstr = str + 5;
        dstr = str + 8;
    }
#ifdef  CCXX_EXCEPTIONS
    else if(Thread::getException() == Thread::throwObject) {
        throw this;
    }
#ifdef  COMMON_STD_EXCEPTION
    else if(Thread::getException() == Thread::throwException) {
        throw Exception("Date::setDate(): Invalid date.");
    }
#endif
#endif
    else {
        julian = 0x7fffffffl;
        return;
    }

    ZNumber nmonth((char*)mstr, 2);
    ZNumber nday((char*)dstr, 2);
    toJulian(year, nmonth(), nday());
}

Date::Date(int year, unsigned month, unsigned day)
{
    toJulian(year, month, day);
}

void Date::update(void)
{
}

bool Date::isValid(void) const
{
    if(julian == 0x7fffffffl)
        return false;
    return true;
}

char *Date::getDate(char *buf) const
{
    fromJulian(buf);
    return buf;
}

time_t Date::getDate(tm* dt) const
{
    char buf[11];
    memset(dt, 0, sizeof(tm));
    fromJulian(buf);
    Number nyear(buf, 4);
    Number nmonth(buf + 5, 2);
    Number nday(buf + 8, 2);

    dt->tm_year = nyear() - 1900;
    dt->tm_mon = nmonth() - 1;
    dt->tm_mday = nday();

    return mktime(dt); // to fill in day of week etc.
}

time_t Date::getDate(void) const
{
    struct tm dt;
    return getDate(&dt);
}

int Date::getYear(void) const
{
    char buf[11];
    fromJulian(buf);
    Number num(buf, 4);
    return num();
}

unsigned Date::getMonth(void) const
{
    char buf[11];
    fromJulian(buf);
    Number num(buf + 5, 2);
    return num();
}

unsigned Date::getDay(void) const
{
    char buf[11];
    fromJulian(buf);
    Number num(buf + 8, 2);
    return num();
}

unsigned Date::getDayOfWeek(void) const
{
    return (unsigned)((julian + 1l) % 7l);
}

String Date::operator()() const
{
    char buf[11];

    fromJulian(buf);
    String date(buf);

    return date;
}

long Date::getValue(void) const
{
    char buf[11];
    fromJulian(buf);
    return atol(buf) * 10000 + atol(buf + 5) * 100 + atol(buf + 8);
}

Date& Date::operator++()
{
    ++julian;
    update();
    return *this;
}

Date& Date::operator--()
{
    --julian;
    update();
    return *this;
}

Date& Date::operator+=(const long val)
{
    julian += val;
    update();
    return *this;
}

Date& Date::operator-=(long val)
{
    julian -= val;
    update();
    return *this;
}

int Date::operator==(const Date &d)
{
    return julian == d.julian;
}

int Date::operator!=(const Date &d)
{
    return julian != d.julian;
}

int Date::operator<(const Date &d)
{
    return julian < d.julian;
}

int Date::operator<=(const Date &d)
{
    return julian <= d.julian;
}

int Date::operator>(const Date &d)
{
    return julian > d.julian;
}

int Date::operator>=(const Date &d)
{
    return julian >= d.julian;
}

void Date::toJulian(long year, long month, long day)
{
    julian = 0x7fffffffl;

    if(month < 1 || month > 12 || day < 1 || day > 31 || year == 0) {
#ifdef  CCXX_EXCEPTIONS
        if(Thread::getException() == Thread::throwObject) {
            throw this;
        }
#ifdef  COMMON_STD_EXCEPTION
        else if(Thread::getException() == Thread::throwException) {
            throw Exception("Date::toJulian(): Invalid date.");
        }
#endif
#endif
        return;
    }

    if(year < 0)
        year--;

    julian = day - 32075l +
        1461l * (year + 4800l + ( month - 14l) / 12l) / 4l +
        367l * (month - 2l - (month - 14l) / 12l * 12l) / 12l -
        3l * ((year + 4900l + (month - 14l) / 12l) / 100l) / 4l;
}

void Date::fromJulian(char *buffer) const
{
// The following conversion algorithm is due to
// Henry F. Fliegel and Thomas C. Van Flandern:

    ZNumber nyear(buffer, 4);
    buffer[4] = '-';
    ZNumber nmonth(buffer + 5, 2);
    buffer[7] = '-';
    ZNumber nday(buffer + 8, 2);

    double i, j, k, l, n;

    l = julian + 68569.0;
    n = int( 4 * l / 146097.0);
    l = l - int( (146097.0 * n + 3)/ 4 );
    i = int( 4000.0 * (l+1)/1461001.0);
    l = l - int(1461.0*i/4.0) + 31.0;
    j = int( 80 * l/2447.0);
    k = l - int( 2447.0 * j / 80.0);
    l = int(j/11);
    j = j+2-12*l;
    i = 100*(n - 49) + i + l;
    nyear = int(i);
    nmonth = int(j);
    nday = int(k);

    buffer[10] = '\0';
}

Date operator+(const Date &date, const long val)
{
    Date d = date;
    d.julian += val;
    d.update();
    return d;
}

Date operator+(const long val, const Date &date)
{
    Date d = date;
    d.julian += val;
    d.update();
    return d;
}

Date operator-(const Date &date, const long val)
{
    Date d = date;
    d.julian -= val;
    d.update();
    return d;
}

Date operator-(const long val, const Date &date)
{
    Date d = date;
    d.julian -= val;
    d.update();
    return d;
}

Time::Time()
{
    time_t now = SysTime::getTime();
    struct tm dt;
       SysTime::getLocalTime(&now, &dt);

    toSeconds(dt.tm_hour, dt.tm_min, dt.tm_sec);
}

Time::Time(struct tm *dt)
{
    toSeconds(dt->tm_hour, dt->tm_min, dt->tm_sec);
}

Time::Time(time_t tm)
{
    struct tm dt;
       SysTime::getLocalTime(&tm, &dt);
    toSeconds(dt.tm_hour, dt.tm_min, dt.tm_sec);
}

Time::Time(char *str, size_t size)
{
    setTime(str, size);
}

Time::Time(int hour, int minute, int second)
{
    toSeconds(hour, minute, second);
}

Time::~Time()
{
}

bool Time::isValid(void) const
{
    if(seconds == -1)
        return false;
    return true;
}

char *Time::getTime(char *buf) const
{
    fromSeconds(buf);
    return buf;
}

time_t Time::getTime(void) const
{
    return static_cast<time_t>(seconds);
}

int Time::getHour(void) const
{
    char buf[7];
    fromSeconds(buf);
    ZNumber num(buf, 2);
    return num();
}

int Time::getMinute(void) const
{
    char buf[7];
    fromSeconds(buf);
    ZNumber num(buf + 2, 2);
    return num();
}

int Time::getSecond(void) const
{
    char buf[7];
    fromSeconds(buf);
    ZNumber num(buf + 4, 2);
    return num();
}

void Time::update(void)
{
}

void Time::setTime(char *str, size_t size)
{
    int sec = 00;

    if(!size)
        size = strlen(str);

//00:00
    if (size == 5) {
        sec = 00;
    }
//00:00:00
    else if (size == 8) {
        ZNumber nsecond(str + 6, 2);
        sec = nsecond();
    }
#ifdef  CCXX_EXCEPTIONS
    else if(Thread::getException() == Thread::throwObject) {
        throw this;
    }
#ifdef  COMMON_STD_EXCEPTION
    else if(Thread::getException() == Thread::throwException) {
        throw Exception("Time::setTime(): Invalid time.");
    }
#endif
#endif
    else {
        return;
    }

    ZNumber nhour(str, 2);
    ZNumber nminute(str+3, 2);
    toSeconds(nhour(), nminute(), sec);
}

String Time::operator()() const
{
    char buf[7];

    fromSeconds(buf);
    String strTime(buf);

    return strTime;
}

long Time::getValue(void) const
{
    return seconds;
}

Time& Time::operator++()
{
    ++seconds;
    update();
    return *this;
}

Time& Time::operator--()
{
    --seconds;
    update();
    return *this;
}

Time& Time::operator+=(const int val)
{
    seconds += val;
    update();
    return *this;
}

Time& Time::operator-=(const int val)
{
    seconds -= val;
    update();
    return *this;
}

int Time::operator==(const Time &t)
{
    return seconds == t.seconds;
}

int Time::operator!=(const Time &t)
{
    return seconds != t.seconds;
}

int Time::operator<(const Time &t)
{
    return seconds < t.seconds;
}

int Time::operator<=(const Time &t)
{
    return seconds <= t.seconds;
}

int Time::operator>(const Time &t)
{
    return seconds > t.seconds;
}

int Time::operator>=(const Time &t)
{
    return seconds >= t.seconds;
}

void Time::toSeconds(int hour, int minute, int second)
{
    seconds = -1;

    if (hour > 23 ||minute > 59 ||second > 59) {
#ifdef  CCXX_EXCEPTIONS
        if(Thread::getException() == Thread::throwObject) {
            throw this;
        }
#ifdef  COMMON_STD_EXCEPTION
        else if(Thread::getException() == Thread::throwException) {
            throw Exception("Time::toSeconds(): Invalid time.");
        }
#endif
#endif
        return;
    }

    seconds = 3600 * hour + 60 * minute + second;
}

void Time::fromSeconds(char *buffer) const
{
    ZNumber hour(buffer, 2);
    ZNumber minute(buffer + 2, 2);
    ZNumber second(buffer + 4, 2);

    hour = seconds / 3600;
    minute = (seconds - (3600 * hour())) / 60;
    second = seconds - (3600 * hour()) - (60 * minute());
    buffer[6] = '\0';
}

Time operator+(const Time &time1, const Time &time2)
{
    Time t;
    t.seconds = time1.seconds + time2.seconds;
    t.update();
    return t;
}

Time operator-(const Time &time1, const Time &time2)
{
    Time t;
    t.seconds = time1.seconds - time2.seconds;
    t.update();
    return t;
}

Time operator+(const Time &time, const int val)
{
    Time t = time;
    t.seconds += val;
    t.update();
    return t;
}

Time operator+(const int val, const Time &time)
{
    Time t = time;
    t.seconds += val;
    t.update();
    return t;
}

Time operator-(const Time &time, const int val)
{
    Time t = time;
    t.seconds -= val;
    t.update();
    return t;
}

Time operator-(const int val, const Time &time)
{
    Time t = time;
    t.seconds -= val;
    t.update();
    return t;
}

Datetime::Datetime(time_t tm)
{
    struct tm dt;
       SysTime::getLocalTime(&tm, &dt);
    toJulian(dt.tm_year + 1900, dt.tm_mon + 1, dt.tm_mday);
    toSeconds(dt.tm_hour, dt.tm_min, dt.tm_sec);
}

Datetime::Datetime(tm *dt) :
Date(dt), Time(dt)
{}

Datetime::Datetime(const char *a_str, size_t size)
{
    char *timestr;

    if (!size)
        size = strlen(a_str);

    char *str = new char[size+1];
    strncpy(str, a_str, size);
    str[size]=0;

// 00/00 00:00
    if (size ==  11) {
        timestr = str + 6;
        setDate(str, 5);
        setTime(timestr, 5);
    }
// 00/00/00 00:00
    else if (size == 14) {
        timestr = str + 9;
        setDate(str, 8);
        setTime(timestr,5);
    }
// 00/00/00 00:00:00
    else if (size == 17) {
        timestr = str + 9;
        setDate(str, 8);
        setTime(timestr,8);
    }
// 0000/00/00 00:00:00
    else if (size == 19) {
        timestr = str + 11;
        setDate(str, 10);
        setTime(timestr,8);
    }
#ifdef  CCXX_EXCEPTIONS
    else if(Thread::getException() == Thread::throwObject) {
        delete str;
        throw this;
    }
#ifdef  COMMON_STD_EXCEPTION
    else if(Thread::getException() == Thread::throwException) {
        delete str;
        throw Exception("Datetime::Datetime(): Invalid time.");
    }
#endif
#endif
    delete str;
}

Datetime::Datetime(int year, unsigned month, unsigned day,
           int hour, int minute, int second) :
  Date(year, month, day), Time(hour, minute, second)
{}

Datetime::Datetime() : Date(), Time()
{
    time_t now = SysTime::getTime();
    struct tm dt;
       SysTime::getLocalTime(&now, &dt);

    toSeconds(dt.tm_hour, dt.tm_min, dt.tm_sec);
    toJulian(dt.tm_year + 1900, dt.tm_mon + 1, dt.tm_mday);
}

Datetime::~Datetime()
{
}

bool Datetime::isValid(void) const
{
    return Date::isValid() && Time::isValid();
}

char *Datetime::getDatetime(char *buf) const
{
    fromJulian(buf);
    buf[10] = ' ';
    fromSeconds(buf+11);
    return buf;
}

time_t Datetime::getDatetime(void) const
{
    char buf[11];
    struct tm dt;
    memset(&dt, 0, sizeof(dt));

    fromJulian(buf);
    ZNumber nyear(buf, 4);
    ZNumber nmonth(buf + 5, 2);
    ZNumber nday(buf + 8, 2);
    dt.tm_year = nyear() - 1900;
    dt.tm_mon = nmonth() - 1;
    dt.tm_mday = nday();

    fromSeconds(buf);
    ZNumber nhour(buf, 2);
    ZNumber nminute(buf + 2, 2);
    ZNumber nsecond(buf + 4, 2);
    dt.tm_hour = nhour();
    dt.tm_min = nminute();
    dt.tm_sec = nsecond();
    dt.tm_isdst = -1;

    return mktime(&dt);
}

Datetime& Datetime::operator=(const Datetime datetime)
{
    julian = datetime.julian;
    seconds = datetime.seconds;

    return *this;
}

Datetime& Datetime::operator+=(const Datetime &datetime)
{
    seconds += datetime.seconds;
    julian += datetime.julian;
    Date::update();
    Time::update();
    return *this;
}

Datetime& Datetime::operator-=(const Datetime &datetime)
{
    seconds -= datetime.seconds;
    julian -= datetime.julian;
    Date::update();
    Time::update();
    return *this;
}

Datetime& Datetime::operator+=(const Time &time)
{
    seconds += time.getValue();
    Date::update();
    Time::update();
    return *this;
}

Datetime& Datetime::operator-=(const Time &time)
{
    seconds -= time.getValue();
    Date::update();
    Time::update();
    return *this;
}


int Datetime::operator==(const Datetime &d)
{
    return (julian == d.julian) && (seconds == d.seconds);
}

int Datetime::operator!=(const Datetime &d)
{
    return (julian != d.julian) || (seconds != d.seconds);
}

int Datetime::operator<(const Datetime &d)
{
    if (julian != d.julian) {
        return (julian < d.julian);
    }
    else {
        return (seconds < d.seconds);
    }
}

int Datetime::operator<=(const Datetime &d)
{
    if (julian != d.julian) {
        return (julian < d.julian);
    }
    else {
        return (seconds <= d.seconds);
    }
}

int Datetime::operator>(const Datetime &d)
{
    if (julian != d.julian) {
        return (julian > d.julian);
    }
    else {
        return (seconds > d.seconds);
    }
}

int Datetime::operator>=(const Datetime &d)
{
    if (julian != d.julian) {
        return (julian > d.julian);
    }
    else {
        return (seconds >= d.seconds);
    }
}

bool Datetime::operator!() const
{
    return !(Date::isValid() && Time::isValid());
}


String Datetime::strftime(const char *format) const
{
    char buffer[64];
    size_t last;
    time_t t;
    tm tbp;
    String retval;

    t = getDatetime();
       SysTime::getLocalTime(&t, &tbp);
#ifdef  WIN32
    last = ::strftime(buffer, 64, format, &tbp);
#else
    last = std::strftime(buffer, 64, format, &tbp);
#endif

    buffer[last] = '\0';
    retval = buffer;
    return retval;
}

DateNumber::DateNumber(char *str) :
Number(str, 10), Date(str, 10)
{}

DateNumber::~DateNumber()
{}

#ifdef  CCXX_NAMESPACES
}
#endif
