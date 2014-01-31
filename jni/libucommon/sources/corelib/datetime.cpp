// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/numbers.h>
#include <ucommon/string.h>
#include <ucommon/datetime.h>
#include <ucommon/thread.h>
#include <ucommon/timers.h>
#include <stdlib.h>
#include <ctype.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

using namespace UCOMMON_NAMESPACE;

#ifdef __BORLANDC__
using std::time_t;
using std::tm;
using std::localtime;
#endif

const long Time::c_day = 86400l;
const long Time::c_hour = 3600l;
const long Time::c_week = 604800l;

const size_t Date::sz_string = 11;
const size_t Time::sz_string = 9;
const size_t DateTime::sz_string = 20;

#ifdef  HAVE_LOCALTIME_R

tm_t *DateTime::local(time_t *now)
{
    tm_t *result, *dt = new tm_t;
    time_t tmp;

    if(!now) {
        now = &tmp;
        time(&tmp);
    }

    result = localtime_r(now, dt);
    if(result)
        return result;
    delete dt;
    return NULL;
}

tm_t *DateTime::gmt(time_t *now)
{
    tm_t *result, *dt = new tm_t;
    time_t tmp;

    if(!now) {
        now = &tmp;
        time(&tmp);
    }

    result = gmtime_r(now, dt);
    if(result)
        return result;
    delete dt;
    return NULL;
}

void DateTime::release(tm_t *dt)
{
    if(dt)
        delete dt;
}

#else
static mutex_t lockflag;

tm_t *DateTime::local(time_t *now)
{
    tm_t *dt;
    time_t tmp;

    if(!now) {
        now = &tmp;
        time(&tmp);
    }

    lockflag.acquire();
    dt = localtime(now);
    if(dt)
        return dt;
    lockflag.release();
    return NULL;
}

tm_t *DateTime::gmt(time_t *now)
{
    tm_t *dt;
    time_t tmp;

    if(!now) {
        now = &tmp;
        time(&tmp);
    }

    lockflag.acquire();
    dt = gmtime(now);
    if(dt)
        return dt;
    lockflag.release();
    return NULL;
}

void DateTime::release(tm_t *dt)
{
    if(dt)
        lockflag.release();
}

#endif

Date::Date()
{
    set();
}

Date::Date(const Date& copy)
{
    julian = copy.julian;
}

Date::Date(tm_t *dt)
{
    set(dt->tm_year + 1900, dt->tm_mon + 1, dt->tm_mday);
}

Date::Date(time_t tm)
{
    tm_t *dt = DateTime::local(&tm);
    set(dt->tm_year + 1900, dt->tm_mon + 1, dt->tm_mday);
    DateTime::release(dt);
}

Date::Date(const char *str, size_t size)
{
    set(str, size);
}

Date::~Date()
{
}

void Date::set()
{
    tm_t *dt = DateTime::local();

    set(dt->tm_year + 1900, dt->tm_mon + 1, dt->tm_mday);
    DateTime::release(dt);
}

void Date::set(const char *str, size_t size)
{
    tm_t *dt = DateTime::local();
    int nyear = 0;
    const char *mstr = str;
    const char *dstr = str;

    if(!size)
        size = strlen(str);
//0000
    if(size == 4) {
        nyear = dt->tm_year + 1900;
        mstr = str;
        dstr = str + 2;
    }
//00/00
    else if(size == 5) {
        nyear = dt->tm_year + 1900;
        mstr = str;
        dstr = str + 3;
    }
//000000
    else if(size == 6) {
        ZNumber zyear((char*)str, 2);
        nyear = ((dt->tm_year + 1900) / 100) * 100 + zyear();
        mstr = str + 2;
        dstr = str + 4;
    }
//00000000
    else if(size == 8 && str[2] >= '0' && str[2] <= '9' && str[5] >= '0' && str[5] <= '9') {
        ZNumber zyear((char*)str, 4);
        nyear = zyear();
        mstr = str + 4;
        dstr = str + 6;
    }
//00/00/00
    else if(size == 8) {
        ZNumber zyear((char*)str, 2);
        nyear = ((dt->tm_year + 1900) / 100) * 100 + zyear();
        mstr = str + 3;
        dstr = str + 6;
    }
//0000/00/00
    else if(size == 10) {
        ZNumber zyear((char*)str, 4);
        nyear = zyear();
        mstr = str + 5;
        dstr = str + 8;
    }
    else {
        julian = 0x7fffffffl;
        DateTime::release(dt);
        return;
    }

    DateTime::release(dt);
    ZNumber nmonth((char*)mstr, 2);
    ZNumber nday((char*)dstr, 2);
    set(nyear, nmonth(), nday());
}

Date::Date(int nyear, unsigned nmonth, unsigned nday)
{
    set(nyear, nmonth, nday);
}

void Date::update(void)
{
}

bool Date::is_valid(void) const
{
    if(julian == 0x7fffffffl)
        return false;
    return true;
}

time_t Date::timeref(void) const
{
    char buf[11];
    tm_t dt;
    memset(&dt, 0, sizeof(tm_t));
    put(buf);
    Number nyear(buf, 4);
    Number nmonth(buf + 5, 2);
    Number nday(buf + 8, 2);

    dt.tm_year = nyear() - 1900;
    dt.tm_mon = nmonth() - 1;
    dt.tm_mday = nday();

    return mktime(&dt); // to fill in day of week etc.
}

int Date::year(void) const
{
    char buf[11];
    put(buf);
    Number num(buf, 4);
    return num();
}

unsigned Date::month(void) const
{
    char buf[11];
    put(buf);
    Number num(buf + 5, 2);
    return num();
}

unsigned Date::day(void) const
{
    char buf[11];
    put(buf);
    Number num(buf + 8, 2);
    return num();
}

unsigned Date::dow(void) const
{
    return (unsigned)((julian + 1l) % 7l);
}

String Date::operator()() const
{
    char buf[11];

    put(buf);
    String date(buf);

    return date;
}

long Date::get(void) const
{
    char buf[11];
    put(buf);
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

Date Date::operator+(long val)
{
    Date result = *this;
    result += val;
    return result;
}

Date Date::operator-(long val)
{
    Date result = *this;
    result -= val;
    return result;
}

Date& Date::operator+=(long val)
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

bool Date::operator==(const Date &d) const
{
    return julian == d.julian;
}

bool Date::operator!=(const Date &d) const
{
    return julian != d.julian;
}

bool Date::operator<(const Date &d) const
{
    return julian < d.julian;
}

bool Date::operator<=(const Date &d) const
{
    return julian <= d.julian;
}

bool Date::operator>(const Date &d) const
{
    return julian > d.julian;
}

bool Date::operator>=(const Date &d) const
{
    return julian >= d.julian;
}

void Date::set(long nyear, long nmonth, long nday)
{
    julian = 0x7fffffffl;

    if(nmonth < 1 || nmonth > 12 || nday < 1 || nday > 31 || nyear == 0)
        return;

    if(nyear < 0)
        nyear--;

    julian = nday - 32075l +
        1461l * (nyear + 4800l + ( nmonth - 14l) / 12l) / 4l +
        367l * (nmonth - 2l - (nmonth - 14l) / 12l * 12l) / 12l -
        3l * ((nyear + 4900l + (nmonth - 14l) / 12l) / 100l) / 4l;
}

Date& Date::operator=(const Date& date)
{
    julian = date.julian;
    return *this;
}

const char *Date::put(char *buffer) const
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
    return buffer;
}

Time::Time()
{
    set();
}

Time::Time(const Time& copy)
{
    seconds = copy.seconds;
}

Time::Time(tm_t *dt)
{
    set(dt->tm_hour, dt->tm_min, dt->tm_sec);
}

Time::Time(time_t tm)
{
    tm_t *dt = DateTime::local(&tm);
    set(dt->tm_hour, dt->tm_min, dt->tm_sec);
    DateTime::release(dt);
}

Time::Time(const char *str, size_t size)
{
    set(str, size);
}

Time::Time(int nhour, int nminute, int nsecond)
{
    set(nhour, nminute, nsecond);
}

Time::~Time()
{
}

void Time::set(void)
{
    tm_t *dt = DateTime::local();
    set(dt->tm_hour, dt->tm_min, dt->tm_sec);
    DateTime::release(dt);
}

bool Time::is_valid(void) const
{
    if(seconds == -1)
        return false;
    return true;
}

int Time::hour(void) const
{
    if(seconds == -1)
        return -1;

    return (int)(seconds / 3600l);
}

int Time::minute(void) const
{
    if(seconds == -1)
        return -1;

    return (int)((seconds / 60l) % 60l);
}

int Time::second(void) const
{
    if(seconds == -1)
        return -1;

    return (int)(seconds % 60l);
}

void Time::update(void)
{
    seconds = abs(seconds % DateTime::c_day);
}

void Time::set(const char *str, size_t size)
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
        ZNumber nsecond((char *)(str + 6), 2);
        sec = nsecond();
    }
    else {
        seconds = -1;
        return;
    }

    ZNumber nhour((char *)str, 2);
    ZNumber nminute((char *)(str + 3), 2);
    set(nhour(), nminute(), sec);
}

String Time::operator()() const
{
    char buf[7];

    put(buf);
    String strTime(buf);

    return strTime;
}

long Time::get(void) const
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

Time& Time::operator+=(long val)
{
    seconds += val;
    update();
    return *this;
}

Time& Time::operator-=(long val)
{
    seconds -= val;
    update();
    return *this;
}

Time Time::operator+(long val)
{
    Time result = *this;
    result += val;
    return result;
}

Time Time::operator-(long val)
{
    Time result = *this;
    result -= val;
    return result;
}

bool Time::operator==(const Time &t) const
{
    return seconds == t.seconds;
}

bool Time::operator!=(const Time &t) const
{
    return seconds != t.seconds;
}

bool Time::operator<(const Time &t) const
{
    return seconds < t.seconds;
}

bool Time::operator<=(const Time &t) const
{
    return seconds <= t.seconds;
}

bool Time::operator>(const Time &t) const
{
    return seconds > t.seconds;
}

bool Time::operator>=(const Time &t) const
{
    return seconds >= t.seconds;
}

long Time::operator-(const Time &t)
{
    if(seconds < t.seconds)
        return (seconds + DateTime::c_day) - t.seconds;
    else
        return seconds - t.seconds;
}

void Time::set(int nhour, int nminute, int nsecond)
{
    seconds = -1;

    if (nminute > 59 || nsecond > 59 || nhour > 23)
        return;

    seconds = 3600 * nhour + 60 * nminute + nsecond;
}

const char *Time::put(char *buffer) const
{
    ZNumber zhour(buffer, 2);
    buffer[2] = ':';
    ZNumber zminute(buffer + 3, 2);
    buffer[5] = ':';
    ZNumber zsecond(buffer + 6, 2);

    zhour = (seconds / 3600l) % 24l;
    zminute = (seconds - (3600l * zhour())) / 60l;
    zsecond = seconds - (3600l * zhour()) - (60l * zminute());
    buffer[8] = '\0';
    return buffer;
}

Time& Time::operator=(const Time& time)
{
    seconds = time.seconds;
    return *this;
}

DateTime::DateTime(time_t tm)
{
    tm_t *dt = DateTime::local();
    Date::set(dt->tm_year + 1900, dt->tm_mon + 1, dt->tm_mday);
    Time::set(dt->tm_hour, dt->tm_min, dt->tm_sec);
    DateTime::release(dt);
}

DateTime::DateTime(tm_t *dt) :
Date(dt), Time(dt)
{}

DateTime::DateTime(const DateTime& copy)
{
    julian = copy.julian;
    seconds = copy.seconds;
}

DateTime::DateTime(const char *a_str, size_t size)
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
        Date::set(str, 5);
        Time::set(timestr, 5);
    }
// 00/00/00 00:00
    else if (size == 14) {
        timestr = str + 9;
        Date::set(str, 8);
        Time::set(timestr,5);
    }
// 00/00/00 00:00:00
    else if (size == 17) {
        timestr = str + 9;
        Date::set(str, 8);
        Time::set(timestr,8);
    }
// 0000/00/00 00:00:00
    else if (size == 19) {
        timestr = str + 11;
        Date::set(str, 10);
        Time::set(timestr,8);
    }
    delete str;
}


DateTime::DateTime(int year, unsigned month, unsigned day,
           int hour, int minute, int second) :
    Date(year, month, day), Time(hour, minute, second)
{}

DateTime::DateTime() : Date(), Time()
{
    tm_t *dt = DateTime::local();
    Time::set(dt->tm_hour, dt->tm_min, dt->tm_sec);
    Date::set(dt->tm_year + 1900, dt->tm_mon + 1, dt->tm_mday);
    DateTime::release(dt);
}

DateTime::~DateTime()
{
}

void DateTime::set()
{
    Date::set();
    Time::set();
}

bool DateTime::is_valid(void) const
{
    return Date::is_valid() && Time::is_valid();
}

const char *DateTime::put(char *buf) const
{
    Date::put(buf);
    buf[10] = ' ';
    Time::put(buf+11);
    return buf;
}

time_t DateTime::get(void) const
{
    char buf[11];
    tm_t dt;
    memset(&dt, 0, sizeof(dt));

    Date::put(buf);
    ZNumber nyear(buf, 4);
    ZNumber nmonth(buf + 5, 2);
    ZNumber nday(buf + 8, 2);
    dt.tm_year = nyear() - 1900;
    dt.tm_mon = nmonth() - 1;
    dt.tm_mday = nday();

    Time::put(buf);
    ZNumber nhour(buf, 2);
    ZNumber nminute(buf + 2, 2);
    ZNumber nsecond(buf + 4, 2);
    dt.tm_hour = nhour();
    dt.tm_min = nminute();
    dt.tm_sec = nsecond();
    dt.tm_isdst = -1;

    return mktime(&dt);
}

DateTime& DateTime::operator=(const DateTime& datetime)
{
    julian = datetime.julian;
    seconds = datetime.seconds;

    return *this;
}

DateTime& DateTime::operator+=(long value)
{
    seconds += value;
    update();
    return *this;
}

DateTime& DateTime::operator-=(long value)
{
    seconds -= value;
    update();
    return *this;
}

void DateTime::update(void)
{
    julian += (seconds / c_day);
    Time::update();
}

bool DateTime::operator==(const DateTime &d) const
{
    return (julian == d.julian) && (seconds == d.seconds);
}

bool DateTime::operator!=(const DateTime &d) const
{
    return (julian != d.julian) || (seconds != d.seconds);
}

bool DateTime::operator<(const DateTime &d) const
{
    if (julian != d.julian) {
        return (julian < d.julian);
    }
    else {
        return (seconds < d.seconds);
    }
}

bool DateTime::operator<=(const DateTime &d) const
{
    if (julian != d.julian) {
        return (julian < d.julian);
    }
    else {
        return (seconds <= d.seconds);
    }
}

bool DateTime::operator>(const DateTime &d) const
{
    if (julian != d.julian) {
        return (julian > d.julian);
    }
    else {
        return (seconds > d.seconds);
    }
}

bool DateTime::operator>=(const DateTime &d) const
{
    if (julian != d.julian) {
        return (julian > d.julian);
    }
    else {
        return (seconds >= d.seconds);
    }
}

bool DateTime::operator!() const
{
    return !(Date::is_valid() && Time::is_valid());
}


String DateTime::format(const char *text) const
{
    char buffer[64];
    size_t last;
    time_t t;
    tm_t *tbp;
    String retval;

    t = get();
    tbp = local(&t);
    last = ::strftime(buffer, 64, text, tbp);
    release(tbp);

    buffer[last] = '\0';
    retval = buffer;
    return retval;
}

long DateTime::operator-(const DateTime &dt)
{
    long secs = (julian - dt.julian) * c_day;
    secs += (seconds - dt.seconds);
    return secs;
}

DateTime DateTime::operator+(long value)
{
    DateTime result = *this;
    result += value;
    return result;
}

DateTime DateTime::operator-(long value)
{
    DateTime result = *this;
    result -= value;
    return result;
}

DateTime& DateTime::operator++()
{
    ++julian;
    update();
    return *this;
}


DateTime& DateTime::operator--()
{
    --julian;
    update();
    return *this;
}

DateTime::operator double() const
{
    return (double)julian + ((double)seconds/86400.0);
}

DateNumber::DateNumber(char *str) :
Number(str, 10), Date(str, 10)
{}

DateNumber::~DateNumber()
{}

void DateNumber::update(void)
{
    Date::put(buffer);
}

void DateNumber::set(void)
{
    Date::set();
    update();
}

DateTimeString::DateTimeString(time_t t) :
DateTime(t)
{
    mode = BOTH;
    DateTimeString::update();
}

DateTimeString::~DateTimeString()
{
}

DateTimeString::DateTimeString(tm_t *dt) :
DateTime(dt)
{
    mode = BOTH;
    DateTimeString::update();
}


DateTimeString::DateTimeString(const DateTimeString& copy) :
DateTime(copy)
{
    mode = copy.mode;
    DateTimeString::update();
}

DateTimeString::DateTimeString(const char *a_str, size_t size) :
DateTime(a_str, size)
{
    mode = BOTH;
    DateTimeString::update();
}


DateTimeString::DateTimeString(int year, unsigned month, unsigned day,
           int hour, int minute, int second) :
DateTime(year, month, day, hour, minute, second)
{
    mode = BOTH;
    DateTimeString::update();
}

DateTimeString::DateTimeString(mode_t m) :
DateTime()
{
    mode = m;
    DateTimeString::update();
}

void DateTimeString::update(void)
{
    DateTime::update();
    switch(mode) {
    case BOTH:
        DateTime::put(buffer);
        break;
    case DATE:
        Date::put(buffer);
        break;
    case TIME:
        Time::put(buffer);
    }
}

void DateTimeString::set(mode_t newmode)
{
    mode = newmode;
    update();
}

void DateTimeString::set(void)
{
    DateTime::set();
    update();
}

isotime::isotime(Time& time)
{
    t = &time;
    pos = 0;
    mode = TIME;
    time.put(buf);
}

isotime::isotime(Date& date)
{
    d = &date;
    pos = 0;
    mode = DATE;
    date.put(buf);
}

isotime::isotime(Date& date, Time& time)
{
    d = &date;
    t = &time;
    pos = 0;
    mode = DATETIME;
    date.put(buf);
    buf[10] = ' ';
    time.put(buf + 11);
}

const char *isotime::_print(void) const
{
    return buf;
}

int isotime::_input(int code)
{
    if(isdigit(buf[pos]) && isdigit(code)) {
        buf[pos++] = code;
        if(buf[pos] == 0) {
            code = EOF;
            goto final;
        }
        return 0;
    }

    if(code == buf[pos]) {
        ++pos;
        return 0;
    }

final:
    buf[pos] = 0;

    switch(mode) {
    case DATE:
        d->set(buf);
        break;
    case TIME:
        t->set(buf);
        break;
    case DATETIME:
        buf[10] = 0;
        d->set(buf);
        t->set(buf + 11);
        break;
    };

    return code;
}

extern "C" {
    long tzoffset(struct timezone *tz)
    {
        struct timeval now;
        time_t t1, t2 = 0;
        struct tm t;
        
        gettimeofday(&now, tz);
        t1 = now.tv_sec;

#ifdef  HAVE_GMTIME_R
        gmtime_r(&t1, &t);
#else
        t = *gmtime(&t1);
#endif

        t.tm_isdst = 0;
        t2 = mktime(&t);
        return (time_t)difftime(t1, t2);
    } 
}

