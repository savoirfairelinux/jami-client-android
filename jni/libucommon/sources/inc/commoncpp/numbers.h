#ifndef COMMONCPP_NUMBERS_H_
#define COMMONCPP_NUMBERS_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_STRING_H_
#include <commoncpp/string.h>
#endif

typedef ucommon::DateTimeString DateTimeString;
typedef ucommon::DateNumber DateNumber;

class __EXPORT Date : public ucommon::Date
{
protected:
    inline void toJulian(long year, long month, long day)
        {ucommon::Date::set(year, month, day);}

    inline void fromJulian(char *buf) const
        {put(buf);}

public:
    inline Date(time_t value) : ucommon::Date(value) {};

    inline Date(struct tm *object) : ucommon::Date(object) {};

    inline Date(const char *ptr, size_t size = 0) : ucommon::Date(ptr, size) {};

    inline Date(int y, unsigned m, unsigned d) : ucommon::Date(y, m, d) {};

    inline Date(const Date& object) : ucommon::Date(object) {};

    inline Date() : ucommon::Date() {};

    inline int getYear(void) const
        {return year();}

    inline unsigned getMonth(void) const
        {return month();}

    inline unsigned getDay(void) const
        {return day();}

    inline unsigned getDayOfWeek(void) const
        {return dow();}

    inline long getJulian(void) const
        {return julian;}

    inline const char *get(char *buffer) const
        {return put(buffer);}

    inline time_t getTime(void) const
        {return timeref();}

    inline bool isValid(void) const
        {return is_valid();}
};

class __EXPORT Time : public ucommon::Time
{
protected:
    inline void toSeconds(int h, int m = 0, int s = 0)
        {set(h, m, s);}

    inline void fromSeconds(char *buf) const
        {put(buf);}

public:
    inline Time(time_t value) : ucommon::Time(value) {};

    inline Time(tm_t *object) : ucommon::Time(object) {};

    inline Time(const char *ptr, size_t size) : ucommon::Time(ptr, size) {};

    inline Time(int h, int m, int s) : ucommon::Time(h, m, s) {};

    inline Time() : ucommon::Time() {};

    inline int getHour(void) const
        {return hour();}

    inline int getMinute(void) const
        {return minute();}

    inline int getSecond(void) const
        {return second();}

    inline const char *get(char *buffer) const
        {return put(buffer);}

    inline bool isValid(void) const
        {return is_valid();}

};

class __EXPORT DateTime : public ucommon::DateTime
{
public:
    inline DateTime(time_t time) : ucommon::DateTime(time) {};

    inline DateTime(struct tm *dt) : ucommon::DateTime(dt) {};


    inline DateTime(int year, unsigned month, unsigned day,
        int hour = 0, int minute = 0, int second = 0) :
            ucommon::DateTime(year, month, day, hour, minute, second) {};

    inline DateTime(const char *ptr, size_t size) :
        ucommon::DateTime(ptr, size) {};

    inline DateTime(const DateTime& obj) : ucommon::DateTime(obj) {};

    inline DateTime() : ucommon::DateTime() {};

    inline int getYear(void) const
        {return year();}

    inline unsigned getMonth(void) const
        {return month();}

    inline unsigned getDay(void) const
        {return day();}

    inline unsigned getDayOfWeek(void) const
        {return dow();}

    inline long getJulian(void) const
        {return julian;}

    inline const char *get(char *buffer) const
        {return ucommon::DateTime::put(buffer);}

    inline time_t getTime(void) const
        {return ucommon::DateTime::timeref();}

    inline bool isValid(void) const
        {return ucommon::DateTime::is_valid();}

    inline int getHour(void) const
        {return hour();}

    inline int getMinute(void) const
        {return minute();}

    inline int getSecond(void) const
        {return second();}

    inline static tm_t *glt(time_t *time = NULL)
        {return ucommon::DateTime::local(time);}
};

#endif

