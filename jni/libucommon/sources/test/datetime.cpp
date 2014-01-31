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

#ifndef DEBUG
#define DEBUG
#endif

#include <ucommon-config.h>
#include <ucommon/ucommon.h>

#include <stdio.h>

using namespace UCOMMON_NAMESPACE;

int main(int argc, char **argv)
{
    Date date = Date(2003, 1, 6);
    int exp_year = 2003;
    unsigned exp_month = 1;
    unsigned exp_day = 6;
    unsigned exp_dayofweek = 1;
    String exp_stringdate;
    tm_t exp_dt;
    time_t exp_ctime;
    char buf[20];

    snprintf(buf, sizeof(buf),
        "%04d-%02d-%02d", exp_year, exp_month, exp_day);

    memset(&exp_dt, 0, sizeof(exp_dt));
    exp_dt.tm_year = exp_year - 1900;
    exp_dt.tm_mon = exp_month - 1;
    exp_dt.tm_mday = exp_day;
    exp_ctime = mktime(&exp_dt);

    assert(exp_year == date.year());
    assert(exp_month == date.month());
    assert(exp_day == date.day());
    assert(exp_dayofweek == date.dow());

    // test some conversions...
    exp_stringdate = date();
    assert(eq(*exp_stringdate, "2003-01-06"));
    date.put(buf);
    assert(eq(buf, "2003-01-06"));
    assert(exp_ctime == date.timeref());

    // some operator tests...
    Date aday = date;
    Date nextday(2003, 1, 7);
    assert(aday == date);
    assert((++aday) == nextday);
    assert(aday != date);
    assert(date <= aday);
    assert(date < aday);

    // play with math and casting operators...
    Date newday = nextday + 5l;
    assert((long)newday == 20030112l);
    assert((long)nextday == 20030107l);
    assert(newday - nextday == 5);

    // test some math...
    assert(20030106l == date.get());
    date -= 6;
    assert(20021231l == date.get());

    // test invalid date...
    date = "20031306";
    assert(!is(date));

    // conversion check...
    date = "2003-08-04";
    assert((long)date == 20030804l);

    DateTimeString dts("2003-02-28 23:59:55");
    eq((const char *)dts, "2003-02-28 23:59:55");

    DateTime tmp("2003-02-28 23:59:55");
    snprintf(buf, sizeof(buf), "%.5f", (double)tmp);
    assert(eq(buf, "2452699.99994"));
    assert((long)tmp == 20030228l);
    tmp += 5;   // add 5 seconds to force rollover...
    assert((long)tmp == 20030301l);

    return 0;
}

