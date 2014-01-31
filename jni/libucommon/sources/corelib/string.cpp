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
#include <ucommon/string.h>
#include <stdarg.h>
#include <ctype.h>
#include <stdio.h>
#ifdef  HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <limits.h>

using namespace UCOMMON_NAMESPACE;

#if _MSC_VER > 1400        // windows broken dll linkage issue...
#else
const strsize_t String::npos = (strsize_t)(-1);
const size_t memstring::header = sizeof(cstring);
#endif

String::cstring::cstring(strsize_t size) :
CountedObject()
{
    max = size;
    len = 0;
    fill = 0;
    text[0] = 0;
}

String::cstring::cstring(strsize_t size, char f) :
CountedObject()
{
    max = size;
    len = 0;
    fill = f;

    if(fill) {
        memset(text, fill, max);
        len = max;
    }
}

void String::cstring::fix(void)
{
    while(fill && len < max)
        text[len++] = fill;
    text[len] = 0;
}

void String::cstring::unfix(void)
{
    while(len && fill) {
        if(text[len - 1] == fill)
            --len;
    }
    text[len] = 0;
}

void String::cstring::clear(strsize_t offset, strsize_t size)
{
    if(!fill || offset >= max)
        return;

    while(size && offset < max) {
        text[offset++] = fill;
        --size;
    }
}

void String::cstring::dec(strsize_t offset)
{
    if(!len)
        return;

    if(offset >= len) {
        text[0] = 0;
        len = 0;
        fix();
        return;
    }

    if(!fill) {
        text[--len] = 0;
        return;
    }

    while(len) {
        if(text[--len] == fill)
            break;
    }
    text[len] = 0;
    fix();
}

void String::cstring::inc(strsize_t offset)
{
    if(!offset)
        ++offset;

    if(offset >= len) {
        text[0] = 0;
        len = 0;
        fix();
        return;
    }

    memmove(text, text + offset, len - offset);
    len -= offset;
    fix();
}

void String::cstring::add(const char *str)
{
    strsize_t size = strlen(str);

    if(!size)
        return;

    while(fill && len && text[len - 1] == fill)
        --len;

    if(len + size > max)
        size = max - len;

    if(size < 1)
        return;

    memcpy(text + len, str, size);
    len += size;
    fix();
}

void String::cstring::add(char ch)
{
    if(!ch)
        return;

    while(fill && len && text[len - 1] == fill)
        --len;

    if(len == max)
        return;

    text[len++] = ch;
    fix();
}

void String::cstring::set(strsize_t offset, const char *str, strsize_t size)
{
    assert(str != NULL);

    if(offset >= max || offset > len)
        return;

    if(offset + size > max)
        size = max - offset;

    while(*str && size) {
        text[offset++] = *(str++);
        --size;
    }

    while(size && fill) {
        text[offset++] = fill;
        --size;
    }

    if(offset > len) {
        len = offset;
        text[len] = 0;
    }
}

char String::fill(void)
{
    if(!str)
        return 0;

    return str->fill;
}

strsize_t String::size(void) const
{
    if(!str)
        return 0;

    return str->max;
}

strsize_t String::len(void) const
{
    if(!str)
        return 0;

    return str->len;
}

void String::cstring::set(const char *str)
{
    assert(str != NULL);

    strsize_t size = strlen(str);
    if(size > max)
        size = max;

    if(str < text || str > text + len)
        memcpy(text, str, size);
    else if(str != text)
        memmove(text, str, size);
    len = size;
    fix();
}

String::String()
{
    str = NULL;
}

String::String(const char *s, const char *end)
{
    strsize_t size = 0;

    if(!s)
        s = "";
    else if(!end)
        size = strlen(s);
    else if(end > s)
        size = (strsize_t)(end - s);
    str = create(size);
    str->retain();
    str->set(s);
}

String::String(const char *s)
{
    strsize_t size = count(s);
    if(!s)
        s = "";
    str = create(size);
    str->retain();
    str->set(s);
}

String::String(const char *s, strsize_t size)
{
    if(!s)
        s = "";
    if(!size)
        size = strlen(s);
    str = create(size);
    str->retain();
    str->set(s);
}

String::String(strsize_t size)
{
    str = create(size);
    str->retain();
}

String::String(long value)
{
    str = create(20);
    str->retain();
    snprintf(&str->text[0], 20, "%ld", value);
    str->len = strlen(str->text);
    str->fix();
}

String::String(double value)
{
    str = create(32);
    str->retain();
    snprintf(&str->text[0], 32, "%f", value);
    str->len = strlen(str->text);
    str->fix();
}

String::String(strsize_t size, char fill)
{
    str = create(size, fill);
    str->retain();
}

String::String(strsize_t size, const char *format, ...)
{
    assert(format != NULL);

    va_list args;
    va_start(args, format);

    str = create(size);
    str->retain();
    vsnprintf(str->text, size + 1, format, args);
    va_end(args);
}

String::String(const String &dup)
{
    str = dup.c_copy();
    if(str)
        str->retain();
}

String::~String()
{
    String::release();
}

String::cstring *String::c_copy(void) const
{
    return str;
}

String String::get(strsize_t offset, strsize_t len) const
{
    if(!str || offset >= str->len)
        return String("");

    if(!len)
        len = str->len - offset;
    return String(str->text + offset, len);
}

String::cstring *String::create(strsize_t size, char fill) const
{
    if(fill)
        return new((size_t)size) cstring(size, fill);
    else
        return new((size_t)size) cstring(size);
}

void String::retain(void)
{
    if(str)
        str->retain();
}

void String::release(void)
{
    if(str)
        str->release();
    str = NULL;
}

char *String::c_mem(void) const
{
    if(!str)
        return NULL;

    return str->text;
}

const char *String::c_str(void) const
{
    if(!str)
        return "";

    return str->text;
}

bool String::equal(const char *s) const
{
#ifdef  HAVE_STRCOLL
    const char *mystr = "";

    if(str)
        mystr = str->text;

    if(!s)
        s = "";

    return strcmp(mystr, s) == 0;
#else
    return compare(s) == 0;
#endif
}

int String::compare(const char *s) const
{
    const char *mystr = "";

    if(str)
        mystr = str->text;

    if(!s)
        s = "";

#ifdef  HAVE_STRCOLL
    return strcoll(mystr, s);
#else
    return strcmp(mystr, s);
#endif
}

const char *String::begin(void) const
{
    if(!str)
        return NULL;

    return str->text;
}

const char *String::end(void) const
{
    if(!str)
        return NULL;

    return str->text + str->len;
}

const char *String::chr(char ch) const
{
    assert(ch != 0);

    if(!str)
        return NULL;

    return ::strchr(str->text, ch);
}

const char *String::rchr(char ch) const
{
    assert(ch != 0);

    if(!str)
        return NULL;

    return ::strrchr(str->text, ch);
}

const char *String::skip(const char *clist, strsize_t offset) const
{
    if(!str || !clist || !*clist || !str->len || offset > str->len)
        return NULL;

    while(offset < str->len) {
        if(!strchr(clist, str->text[offset]))
            return str->text + offset;
        ++offset;
    }
    return NULL;
}

const char *String::rskip(const char *clist, strsize_t offset) const
{
    if(!str || !clist || !*clist)
        return NULL;

    if(!str->len)
        return NULL;

    if(offset > str->len)
        offset = str->len;

    while(offset--) {
        if(!strchr(clist, str->text[offset]))
            return str->text + offset;
    }
    return NULL;
}

const char *String::rfind(const char *clist, strsize_t offset) const
{
    if(!str || !clist || !*clist)
        return NULL;

    if(!str->len)
        return str->text;

    if(offset > str->len)
        offset = str->len;

    while(offset--) {
        if(strchr(clist, str->text[offset]))
            return str->text + offset;
    }
    return NULL;
}

void String::chop(const char *clist)
{
    strsize_t offset;

    if(!str)
        return;

    if(!str->len)
        return;

    offset = str->len;
    while(offset) {
        if(!strchr(clist, str->text[offset - 1]))
            break;
        --offset;
    }

    if(!offset) {
        clear();
        return;
    }

    if(offset == str->len)
        return;

    str->len = offset;
    str->fix();
}

void String::strip(const char *clist)
{
    trim(clist);
    chop(clist);
}

void String::trim(const char *clist)
{
    unsigned offset = 0;

    if(!str)
        return;

    while(offset < str->len) {
        if(!strchr(clist, str->text[offset]))
            break;
        ++offset;
    }

    if(!offset)
        return;

    if(offset == str->len) {
        clear();
        return;
    }

    memmove(str->text, str->text + offset, str->len - offset);
    str->len -= offset;
    str->fix();
}

unsigned String::replace(const char *substring, const char *cp, unsigned flags)
{
    const char *result = "";
    unsigned count = 0;
    size_t cpl = 0;

    if(cp)
        cpl = strlen(cp);

    if(!str || !substring || str->len == 0)
        return 0;

    strsize_t offset = 0;

    size_t tcl = strlen(substring);

    while(result) {
        const char *text = str->text + offset;
#if defined(_MSWINDOWS_)
        if((flags & 0x01) == INSENSITIVE)
            result = strstr(text, substring);
#elif  defined(HAVE_STRICMP)
        if((flags & 0x01) == INSENSITIVE)
            result = stristr(text, substring);
#else
        if((flags & 0x01) == INSENSITIVE)
            result = strcasestr(text, substring);
#endif
        else
            result = strstr(text, substring);

        if(result) {
            ++count;
            offset = text - str->text;
            cut(offset, tcl);
            if(cpl) {
                paste(offset, cp);
                offset += cpl;
            }
        }
    }
    return count;
}

const char *String::search(const char *substring, unsigned instance, unsigned flags) const
{
    const char *result = "";

    if(!str || !substring || str->len == 0)
        return NULL;

    const char *text = str->text;

    if(!instance)
        ++instance;
    while(instance-- && result) {
#if defined(_MSWINDOWS_)
        if((flags & 0x01) == INSENSITIVE)
            result = strstr(text, substring);
#elif  defined(HAVE_STRICMP)
        if((flags & 0x01) == INSENSITIVE)
            result = stristr(text, substring);
#else
        if((flags & 0x01) == INSENSITIVE)
            result = strcasestr(text, substring);
#endif
        else
            result = strstr(text, substring);

        if(result)
            text = result + strlen(result);
    }
    return result;
}

const char *String::find(const char *clist, strsize_t offset) const
{
    if(!str || !clist || !*clist || !str->len || offset > str->len)
        return NULL;

    while(offset < str->len) {
        if(strchr(clist, str->text[offset]))
            return str->text + offset;
        ++offset;
    }
    return NULL;
}

bool String::unquote(const char *clist)
{
    assert(clist != NULL);

    char *s;

    if(!str)
        return false;

    str->unfix();
    s = unquote(str->text, clist);
    if(!s) {
        str->fix();
        return false;
    }

    set(s);
    return true;
}

void String::upper(void)
{
    if(str)
        upper(str->text);
}

void String::lower(void)
{
    if(str)
        lower(str->text);
}

void String::erase(void)
{
    if(str) {
        memset(str->text, 0, str->max);
        str->fix();
    }
}

strsize_t String::offset(const char *s) const
{
    if(!str || !s)
        return npos;

    if(s < str->text || s > str->text + str->max)
        return npos;

    if(s - str->text > str->len)
        return str->len;

    return (strsize_t)(s - str->text);
}

strsize_t String::count(void) const
{
    if(!str)
        return 0;
    return str->len;
}

strsize_t String::ccount(const char *clist) const
{
    if(!str)
        return 0;

    return ccount(str->text, clist);
}

strsize_t String::printf(const char *format, ...)
{
    assert(format != NULL);

    va_list args;
    va_start(args, format);
    if(str) {
        vsnprintf(str->text, str->max + 1, format, args);
        str->len = strlen(str->text);
        str->fix();
    }
    va_end(args);
    return len();
}

strsize_t String::vprintf(const char *format, va_list args)
{
    assert(format != NULL);

    if(str) {
        vsnprintf(str->text, str->max + 1, format, args);
        str->len = strlen(str->text);
        str->fix();
    }
    return len();
}

#if !defined(_MSC_VER)
int String::vscanf(const char *format, va_list args)
{
    assert(format != NULL);

    if(str)
        return vsscanf(str->text, format, args);
    return -1;
}

int String::scanf(const char *format, ...)
{
    assert(format != NULL);

    va_list args;
    int rtn = -1;

    va_start(args, format);
    if(str)
        rtn = vsscanf(str->text, format, args);
    va_end(args);
    return rtn;
}
#else
int String::vscanf(const char *format, va_list args)
{
    return 0;
}

int String::scanf(const char *format, ...)
{
    return 0;
}
#endif

void String::rsplit(const char *s)
{
    if(!str || !s || s <= str->text || s > str->text + str->len)
        return;

    str->set(s);
}

void String::rsplit(strsize_t pos)
{
    if(!str || pos > str->len || !pos)
        return;

    str->set(str->text + pos);
}

void String::split(const char *s)
{
    unsigned pos;

    if(!s || !*s || !str)
        return;

    if(s < str->text || s >= str->text + str->len)
        return;

    pos = s - str->text;
    str->text[pos] = 0;
    str->fix();
}

void String::split(strsize_t pos)
{
    if(!str || pos >= str->len)
        return;

    str->text[pos] = 0;
    str->fix();
}

void String::set(strsize_t offset, const char *s, strsize_t size)
{
    if(!s || !*s || !str)
        return;

    if(!size)
        size = strlen(s);

    str->set(offset, s, size);
}

void String::set(const char *s, char overflow, strsize_t offset, strsize_t size)
{
    size_t len = count(s);

    if(!s || !*s || !str)
        return;

    if(offset >= str->max)
        return;

    if(!size || size > str->max - offset)
        size = str->max - offset;

    if(len <= size) {
        set(offset, s, size);
        return;
    }

    set(offset, s, size);

    if(len > size && overflow)
        str->text[offset + size - 1] = overflow;
}

void String::rset(const char *s, char overflow, strsize_t offset, strsize_t size)
{
    size_t len = count(s);
    strsize_t dif;

    if(!s || !*s || !str)
        return;

    if(offset >= str->max)
        return;

    if(!size || size > str->max - offset)
        size = str->max - offset;

    dif = len;
    while(dif < size && str->fill) {
        str->text[offset++] = str->fill;
        ++dif;
    }

    if(len > size)
        s += len - size;
    set(offset, s, size);
    if(overflow && len > size)
        str->text[offset] = overflow;
}

void String::set(const char *s)
{
    strsize_t len;

    if(!s)
        s = "";

    if(!str) {
        len = strlen(s);
        str = create(len);
        str->retain();
    }

    str->set(s);
}

void String::paste(strsize_t offset, const char *cp, strsize_t size)
{
    if(!cp)
        return;

    if(!size)
        size = strlen(cp);

    if(!size)
        return;

    cow(size);

    if(!str) {
        String::set(str->text, ++size, cp);
        str->len = --size;
        str->fix();
        return;
    }

    if(offset >= str->len)
        String::set(str->text + str->len, size + 1, cp);
    else {
        memmove(str->text + offset + size, str->text + offset, str->len - offset);
        memmove(str->text + offset, cp, size);
    }
    str->len += size;
    str->fix();
}

void String::cut(strsize_t offset, strsize_t size)
{
    if(!str || offset >= str->len)
        return;

    if(!size)
        size = str->len;

    if(offset + size >= str->len) {
        str->len = offset;
        str->fix();
        return;
    }

    memmove(str->text + offset, str->text + offset + size, str->len - offset - size);
    str->len -= size;
    str->fix();
}

void String::paste(char *text, size_t max, size_t offset, const char *cp, size_t size)
{
    if(!cp || !text)
        return;

    if(!size)
        size = strlen(cp);

    if(!size)
        return;

    size_t len = strlen(text);
    if(len <= max)
        return;

    if(len + size >= max)
        size = max - len;

    if(offset >= len)
        String::set(text + len, size + 1, cp);
    else {
        memmove(text + offset + size, text + offset, len - offset);
        memmove(text + offset, cp, size);
    }
}

void String::cut(char *text, size_t offset, size_t size)
{
    if(!text)
        return;

    size_t len = strlen(text);
    if(!len || offset >= len)
        return;

    if(offset + size >= len) {
        text[offset] = 0;
        return;
    }

    memmove(text + offset, text + offset + size, len - offset - size);
    len -= size;
    text[len] = 0;
}

bool String::resize(strsize_t size)
{
    char fill = 0;

    if(!size) {
        release();
        str = NULL;
        return true;
    }

    if(!str) {
        str = create(size, fill);
        str->retain();
    }
    else if(str->is_copied() || str->max < size) {
        fill = str->fill;
        str->release();
        str = create(size, fill);
        str->retain();
    }
    return true;
}

void String::clear(strsize_t offset, strsize_t size)
{
    if(!str)
        return;

    if(!size)
        size = str->max;

    str->clear(offset, size);
}

void String::clear(void)
{
    if(str)
        str->set("");
}

void String::cow(strsize_t size)
{
    if(str) {
        if(str->fill)
            size = str->max;
        else
            size += str->len;
    }

    if(!size)
        return;

    if(!str || !str->max || str->is_copied() || size > str->max) {
        cstring *s = create(size);
        s->len = str->len;
        String::set(s->text, s->max + 1, str->text);
        s->retain();
        str->release();
        str = s;
    }
}

void String::add(char ch)
{
    char buf[2];

    if(ch == 0)
        return;

    buf[0] = ch;
    buf[1] = 0;

    if(!str) {
        set(buf);
        return;
    }

    cow(1);
    str->add(buf);
}

void String::add(const char *s)
{
    if(!s || !*s)
        return;

    if(!str) {
        set(s);
        return;
    }

    cow(strlen(s));
    str->add(s);
}

char String::at(int offset) const
{
    if(!str)
        return 0;

    if(offset >= (int)str->len)
        return 0;

    if(offset > -1)
        return str->text[offset];

    if((strsize_t)(-offset) >= str->len)
        return str->text[0];

    return str->text[(int)(str->len) + offset];
}

String String::operator()(int offset, strsize_t len) const
{
    const char *cp = operator()(offset);
    if(!cp)
        cp = "";

    if(!len)
        len = strlen(cp);

    return String(cp, len);
}

const char *String::operator()(int offset) const
{
    if(!str)
        return NULL;

    if(offset >= (int)str->len)
        return NULL;

    if(offset > -1)
        return str->text + offset;

    if((strsize_t)(-offset) >= str->len)
        return str->text;

    return str->text + str->len + offset;
}

const char String::operator[](int offset) const
{
    if(!str)
        return 0;

    if(offset >= (int)str->len)
        return 0;

    if(offset > -1)
        return str->text[offset];

    if((strsize_t)(-offset) >= str->len)
        return *str->text;

    return str->text[str->len + offset];
}

String &String::operator++()
{
    if(str)
        str->inc(1);
    return *this;
}

String &String::operator--()
{
    if(str)
        str->dec(1);
    return *this;
}

String &String::operator+=(strsize_t offset)
{
    if(str)
        str->inc(offset);
    return *this;
}

String &String::operator-=(strsize_t offset)
{
    if(str)
        str->dec(offset);
    return *this;
}

bool String::operator*=(const char *substr)
{
    if(search(substr))
        return true;

    return false;
}

String &String::operator^=(const char *s)
{
    release();
    set(s);
    return *this;
}

String &String::operator=(const char *s)
{
    release();
    set(s);
    return *this;
}

String &String::operator|=(const char *s)
{
    set(s);
    return *this;
}

String &String::operator&=(const char *s)
{
    set(s);
    return *this;
}

String &String::operator^=(const String &s)
{
    release();
    set(s.c_str());
    return *this;
}

String &String::operator=(const String &s)
{
    if(str == s.str)
        return *this;

    if(s.str)
        s.str->retain();

    if(str)
        str->release();

    str = s.str;
    return *this;
}

bool String::full(void) const
{
    if(!str)
        return false;

    if(str->len == str->max &&
       str->text[str->len - 1] != str->fill)
        return true;

    return false;
}

bool String::operator!() const
{
    bool rtn = false;
    if(!str)
        return true;

    str->unfix();
    if(!str->len)
        rtn = true;

    str->fix();
    return rtn;
}

String::operator bool() const
{
    bool rtn = false;

    if(!str)
        return false;

    str->unfix();
    if(str->len)
        rtn = true;
    str->fix();
    return rtn;
}

bool String::operator==(const char *s) const
{
    return (compare(s) == 0);
}

bool String::operator!=(const char *s) const
{
    return (compare(s) != 0);
}

bool String::operator<(const char *s) const
{
    return (compare(s) < 0);
}

bool String::operator<=(const char *s) const
{
    return (compare(s) <= 0);
}

bool String::operator>(const char *s) const
{
    return (compare(s) > 0);
}

bool String::operator>=(const char *s) const
{
    return (compare(s) >= 0);
}

String &String::operator&(const char *s)
{
    add(s);
    return *this;
}

String &String::operator|(const char *s)
{
    if(!s || !*s)
        return *this;

    if(!str) {
        set(s);
        return *this;
    }

    str->add(s);
    return *this;
}

String String::operator+(const char *s)
{
    String tmp;
    if(str && str->text)
        tmp.set(str->text);

    if(s && *s)
        tmp.add(s);

    return tmp;
}

String &String::operator+=(const char *s)
{
    if(!s || !*s)
        return *this;

    add(s);
    return *this;
}

memstring::memstring(void *mem, strsize_t size, char fill)
{
    assert(mem != NULL);
    assert(size > 0);

    str = new((caddr_t)mem) cstring(size, fill);
    str->set("");
}

memstring::~memstring()
{
    str = NULL;
}

memstring *memstring::create(strsize_t size, char fill)
{
    assert(size > 0);

    caddr_t mem = (caddr_t)cpr_memalloc(size + sizeof(memstring) + sizeof(cstring));
    return new(mem) memstring(mem + sizeof(memstring), size, fill);
}

memstring *memstring::create(MemoryProtocol *mpager, strsize_t size, char fill)
{
    assert(size > 0);

    caddr_t mem = (caddr_t)mpager->alloc(size + sizeof(memstring) + sizeof(cstring));
    return new(mem) memstring(mem + sizeof(memstring), size, fill);
}

void memstring::release(void)
{
    str = NULL;
}

bool memstring::resize(strsize_t size)
{
    return false;
}

void memstring::cow(strsize_t adj)
{
}

char *String::token(char *text, char **token, const char *clist, const char *quote, const char *eol)
{
    char *result;

    if(!eol)
        eol = "";

    if(!token || !clist)
        return NULL;

    if(!*token)
        *token = text;

    if(!**token) {
        *token = text;
        return NULL;
    }

    while(**token && strchr(clist, **token))
        ++*token;

    result = *token;

    if(*result && *eol && NULL != (eol = strchr(eol, *result))) {
        if(eol[0] != eol[1] || *result == eol[1]) {
            *token = text;
            return NULL;
        }
    }

    if(!*result) {
        *token = text;
        return NULL;
    }

    while(quote && *quote && *result != *quote) {
        quote += 2;
    }

    if(quote && *quote) {
        ++result;
        ++quote;
        *token = strchr(result, *quote);
        if(!*token)
            *token = result + strlen(result);
        else {
            **token = 0;
            ++(*token);
        }
        return result;
    }

    while(**token && !strchr(clist, **token))
        ++(*token);

    if(**token) {
        **token = 0;
        ++(*token);
    }

    return result;
}

String::cstring *memstring::c_copy(void) const
{
    cstring *tmp = String::create(str->max, str->fill);
    tmp->set(str->text);
    return tmp;
}

void String::fix(String &s)
{
    if(s.str) {
        s.str->len = strlen(s.str->text);
        s.str->fix();
    }
}

void String::swap(String &s1, String &s2)
{
    String::cstring *s = s1.str;
    s1.str = s2.str;
    s2.str = s;
}

char *String::dup(const char *cp)
{
    char *mem;

    if(!cp)
        return NULL;

    size_t len = strlen(cp) + 1;
    mem = (char *)malloc(len);
    crit(mem != NULL, "string dup allocation error");
    String::set(mem, len, cp);
    return mem;
}

char *String::left(const char *cp, size_t size)
{
    char *mem;

    if(!cp)
        return NULL;

    if(!size)
        size = strlen(cp);

    mem = (char *)malloc(++size);
    crit(mem != NULL, "string dup allocation error");
    String::set(mem, size, cp);
    return mem;
}

const char *String::pos(const char *cp, ssize_t offset)
{
    if(!cp)
        return NULL;

    size_t len = strlen(cp);
    if(!len)
        return cp;

    if(offset >= 0) {
        if((size_t)offset > len)
            offset = len;
        return cp + offset;
    }

    offset = -offset;
    if((size_t)offset >= len)
        return cp;

    return cp + len - offset;
}

size_t String::count(const char *cp)
{
    if(!cp)
        return 0;

    return strlen(cp);
}

const char *String::find(const char *str, const char *key, const char *delim)
{
    unsigned l1 = strlen(str);
    unsigned l2 = strlen(key);

    if(!delim[0])
        delim = NULL;

    while(l1 >= l2) {
        if(!strncmp(key, str, l2)) {
            if(l1 == l2 || !delim || strchr(delim, str[l2]))
                return str;
        }
        if(!delim) {
            ++str;
            --l1;
            continue;
        }
        while(l1 >= l2 && !strchr(delim, *str)) {
            ++str;
            --l1;
        }
        while(l1 >= l2 && strchr(delim, *str)) {
            ++str;
            --l1;
        }
    }
    return NULL;
}

const char *String::ifind(const char *str, const char *key, const char *delim)
{
    unsigned l1 = strlen(str);
    unsigned l2 = strlen(key);

    if(!delim[0])
        delim = NULL;

    while(l1 >= l2) {
        if(!strnicmp(key, str, l2)) {
            if(l1 == l2 || !delim || strchr(delim, str[l2]))
                return str;
        }
        if(!delim) {
            ++str;
            --l1;
            continue;
        }
        while(l1 >= l2 && !strchr(delim, *str)) {
            ++str;
            --l1;
        }
        while(l1 >= l2 && strchr(delim, *str)) {
            ++str;
            --l1;
        }
    }
    return NULL;
}

char *String::set(char *str, size_t size, const char *s, size_t len)
{
    if(!str)
        return NULL;

    if(size < 2)
        return str;

    if(!s)
        s = "";

    size_t l = strlen(s);
    if(l >= size)
        l = size - 1;

    if(l > len)
        l = len;

    if(!l) {
        *str = 0;
        return str;
    }

    memmove(str, s, l);
    str[l] = 0;
    return str;
}

char *String::rset(char *str, size_t size, const char *s)
{
    size_t len = count(s);
    if(len > size)
        s += len - size;
    return set(str, size, s);
}

char *String::set(char *str, size_t size, const char *s)
{
    if(!str)
        return NULL;

    if(size < 2)
        return str;

    if(!s)
        s = "";

    size_t l = strlen(s);

    if(l >= size)
        l = size - 1;

    if(!l) {
        *str = 0;
        return str;
    }

    memmove(str, s, l);
    str[l] = 0;
    return str;
}

char *String::add(char *str, size_t size, const char *s, size_t len)
{
    if(!str)
        return NULL;

    if(!s)
        return str;

    size_t l = strlen(s);
    size_t o = strlen(str);

    if(o >= (size - 1))
        return str;
    set(str + o, size - o, s, l);
    return str;
}

char *String::add(char *str, size_t size, const char *s)
{
    if(!str)
        return NULL;

    if(!s)
        return str;

    size_t o = strlen(str);

    if(o >= (size - 1))
        return str;

    set(str + o, size - o, s);
    return str;
}

char *String::trim(char *str, const char *clist)
{
    if(!str)
        return NULL;

    if(!clist)
        return str;

    while(*str && strchr(clist, *str))
        ++str;

    return str;
}

char *String::chop(char *str, const char *clist)
{
    if(!str)
        return NULL;

    if(!clist)
        return str;

    size_t offset = strlen(str);
    while(offset && strchr(clist, str[offset - 1]))
        str[--offset] = 0;
    return str;
}

char *String::strip(char *str, const char *clist)
{
    str = trim(str, clist);
    chop(str, clist);
    return str;
}

void String::upper(char *str)
{
    while(str && *str) {
        *str = toupper(*str);
        ++str;
    }
}

void String::lower(char *str)
{
    while(str && *str) {
        *str = tolower(*str);
        ++str;
    }
}

void String::erase(char *str)
{
    if(!str || *str == 0)
        return;

    memset(str, 0, strlen(str));
}

unsigned String::ccount(const char *str, const char *clist)
{
    unsigned count = 0;
    while(str && *str) {
        if(strchr(clist, *(str++)))
            ++count;
    }
    return count;
}

char *String::skip(char *str, const char *clist)
{
    if(!str || !clist)
        return NULL;

    while(*str && strchr(clist, *str))
        ++str;

    if(*str)
        return str;

    return NULL;
}

char *String::rskip(char *str, const char *clist)
{
    size_t len = count(str);

    if(!len || !clist)
        return NULL;

    while(len > 0) {
        if(!strchr(clist, str[--len]))
            return str;
    }
    return NULL;
}

size_t String::seek(char *str, const char *clist)
{
    size_t pos = 0;

    if(!str)
        return 0;

    if(!clist)
        return strlen(str);

    while(str[pos]) {
        if(strchr(clist, str[pos]))
            return pos;
        ++pos;
    }
    return pos;
}

char *String::find(char *str, const char *clist)
{
    if(!str)
        return NULL;

    if(!clist)
        return str;

    while(str && *str) {
        if(strchr(clist, *str))
            return str;
    }
    return NULL;
}

char *String::rfind(char *str, const char *clist)
{
    if(!str)
        return NULL;

    if(!clist)
        return str + strlen(str);

    char *s = str + strlen(str);

    while(s > str) {
        if(strchr(clist, *(--s)))
            return s;
    }
    return NULL;
}

bool String::eq_case(const char *s1, const char *s2)
{
    if(!s1)
        s1 = "";

    if(!s2)
        s2 = "";

#ifdef  HAVE_STRICMP
    return stricmp(s1, s2) == 0;
#else
    return strcasecmp(s1, s2) == 0;
#endif
}

int String::case_compare(const char *s1, const char *s2)
{
    if(!s1)
        s1 = "";

    if(!s2)
        s2 = "";

#ifdef  HAVE_STRICMP
    return stricmp(s1, s2);
#else
    return strcasecmp(s1, s2);
#endif
}

int String::case_compare(const char *s1, const char *s2, size_t size)
{
    if(!s1)
        s1 = "";

    if(!s2)
        s2 = "";

#ifdef  HAVE_STRICMP
    return strnicmp(s1, s2, size);
#else
    return strncasecmp(s1, s2, size);
#endif
}

bool String::eq_case(const char *s1, const char *s2, size_t size)
{
    if(!s1)
        s1 = "";

    if(!s2)
        s2 = "";

#ifdef  HAVE_STRICMP
    return strnicmp(s1, s2, size) == 0;
#else
    return strncasecmp(s1, s2, size) == 0;
#endif
}

bool String::equal(const char *s1, const char *s2)
{
    if(!s1)
        s1 = "";
    if(!s2)
        s2 = "";

    return strcmp(s1, s2) == 0;
}

bool String::equal(const char *s1, const char *s2, size_t size)
{
    if(!s1)
        s1 = "";
    if(!s2)
        s2 = "";

    return strncmp(s1, s2, size) == 0;
}

int String::compare(const char *s1, const char *s2)
{
    if(!s1)
        s1 = "";
    if(!s2)
        s2 = "";

#ifdef  HAVE_STRCOLL
    return strcoll(s1, s2);
#else
    return strcmp(s1, s2);
#endif
}


char *String::unquote(char *str, const char *clist)
{
    assert(clist != NULL);

    size_t len = count(str);
    if(!len || !str)
        return NULL;

    while(clist[0]) {
        if(*str == clist[0] && str[len - 1] == clist[1]) {
            str[len - 1] = 0;
            return ++str;
        }
        clist += 2;
    }
    return str;
}

char *String::fill(char *str, size_t size, char fill)
{
    if(!str)
        return NULL;

    memset(str, fill, size - 1);
    str[size - 1] = 0;
    return str;
}

unsigned String::hexsize(const char *format)
{
    unsigned count = 0;
    char *ep;
    unsigned skip;

    while(format && *format) {
        while(*format && !isdigit(*format)) {
            ++format;
            ++count;
        }
        if(isdigit(*format)) {
            skip = (unsigned)strtol(format, &ep, 10);
            format = ep;
            count += skip * 2;
        }
    }
    return count;
}

unsigned String::hexdump(const unsigned char *binary, char *string, const char *format)
{
    unsigned count = 0;
    char *ep;
    unsigned skip;

    while(format && *format) {
        while(*format && !isdigit(*format)) {
            *(string++) = *(format++);
            ++count;
        }
        if(isdigit(*format)) {
            skip = (unsigned)strtol(format, &ep, 10);
            format = ep;
            count += skip * 2;
            while(skip--) {
                snprintf(string, 3, "%02x", *(binary++));
                string += 2;
            }
        }
    }
    *string = 0;
    return count;
}

static unsigned hex(char ch)
{
    if(ch >= '0' && ch <= '9')
        return ch - '0';
    else
        return toupper(ch) - 'A' + 10;
}

unsigned String::hexpack(unsigned char *binary, const char *string, const char *format)
{
    unsigned count = 0;
    char *ep;
    unsigned skip;

    while(format && *format) {
        while(*format && !isdigit(*format)) {
            if(*(string++) != *(format++))
                return count;
            ++count;
        }
        if(isdigit(*format)) {
            skip = (unsigned)strtol(format, &ep, 10);
            format = ep;
            count += skip * 2;
            while(skip--) {
                *(binary++) = hex(string[0]) * 16 + hex(string[1]);
                string += 2;
            }
        }
    }
    return count;
}

String &String::operator%(unsigned short& value)
{
    unsigned long temp = USHRT_MAX + 1;
    char *ep;
    if(!str || !str->text)
        return *this;

    value = 0;
    temp = strtoul(str->text, &ep, 0);
    if(temp > USHRT_MAX)
        goto failed;

    value = (unsigned short)temp;

    if(ep)
        set(ep);
    else
        set("");

failed:
    return *this;
}

String &String::operator%(short& value)
{
    long temp = SHRT_MAX + 1;
    char *ep;
    if(!str || !str->text)
        return *this;

    value = 0;
    temp = strtol(str->text, &ep, 0);
    if(temp < 0 && temp < SHRT_MIN)
        goto failed;

    if(temp > SHRT_MAX)
        goto failed;

    value = (short)temp;

    if(ep)
        set(ep);
    else
        set("");

failed:
    return *this;
}

String &String::operator%(long& value)
{
    value = 0;
    char *ep;
    if(!str || !str->text)
        return *this;

    value = strtol(str->text, &ep, 0);
    if(ep)
        set(ep);
    else
        set("");

    return *this;
}

String &String::operator%(unsigned long& value)
{
    value = 0;
    char *ep;
    if(!str || !str->text)
        return *this;

    value = strtoul(str->text, &ep, 0);
    if(ep)
        set(ep);
    else
        set("");

    return *this;
}

String &String::operator%(double& value)
{
    value = 0;
    char *ep;
    if(!str || !str->text)
        return *this;

    value = strtod(str->text, &ep);
    if(ep)
        set(ep);
    else
        set("");

    return *this;
}

String &String::operator%(const char *get)
{
    if(!str || !str->text || !get)
        return *this;

    unsigned len = strlen(get);
    const char *cp = str->text;

    while(isspace(*cp))
        ++cp;

    if(eq(cp, get, len))
        set(cp + len);
    else if(cp != str->text)
        set(cp);

    return *this;
}

String str(CharacterProtocol *p, strsize_t size)
{
    String temp(size);
    char *cp = temp.c_mem();
    int ch;
    bool cr = false;

    while(--size) {
        ch = p->getchar();
        if(ch == 0 || ch == EOF || ch == '\n')
            break;

        if(cr) {
            cr = false;
            *(cp++) = '\r';
        }

        if(ch == '\r')
            cr = true;
        else
            *(cp++) = ch;
    }

    *cp = 0;

    String::fix(temp);
    return temp;
}

static const unsigned char alphabet[65] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

size_t String::b64encode(char *dest, const uint8_t *bin, size_t size, size_t dsize)
{
    assert(dest != NULL && bin != NULL);

    size_t count = 0;

    if(!dsize)
        dsize = (size * 4 / 3) + 1;

    if (!dsize || !size)
        goto end;

    unsigned bits;

    while(size >= 3 && dsize > 4) {
        bits = (((unsigned)bin[0])<<16) | (((unsigned)bin[1])<<8)
            | ((unsigned)bin[2]);
        bin += 3;
        size -= 3;
        count += 3;
        *(dest++) = alphabet[bits >> 18];
        *(dest++) = alphabet[(bits >> 12) & 0x3f];
        *(dest++) = alphabet[(bits >> 6) & 0x3f];
        *(dest++) = alphabet[bits & 0x3f];
        dsize -= 4;
    }

    if (size && dsize > 4) {
        bits = ((unsigned)bin[0])<<16;
        *(dest++) = alphabet[bits >> 18];
        ++count;
        if (size == 1) {
            *(dest++) = alphabet[(bits >> 12) & 0x3f];
            *(dest++) = '=';
        }
        else {
            ++count;
            bits |= ((unsigned)bin[1])<<8;
            *(dest++) = alphabet[(bits >> 12) & 0x3f];
            *(dest++) = alphabet[(bits >> 6) & 0x3f];
        }
        *(dest++) = '=';
    }

end:
    *dest = 0;
    return count;
}

size_t String::b64decode(uint8_t *dest, const char *src, size_t size)
{
    char decoder[256];
    unsigned long bits;
    uint8_t c;
    unsigned i;
    size_t count = 0;

    for (i = 0; i < 256; ++i)
        decoder[i] = 64;

    for (i = 0; i < 64 ; ++i)
        decoder[alphabet[i]] = i;

    bits = 1;

    while(*src) {
        c = (uint8_t)(*(src++));
        if (c == '=') {
            if (bits & 0x40000) {
                if (size < 2)
                    break;
                *(dest++) = (uint8_t)((bits >> 10) & 0xff);
                *(dest++) = (uint8_t)((bits >> 2) & 0xff);
                count += 2;
                break;
            }
            if ((bits & 0x1000) && size) {
                *(dest++) = (uint8_t)((bits >> 4) & 0xff);
                ++count;
            }
            break;
        }
        // end on invalid chars
        if (decoder[c] == 64)
            break;
        bits = (bits << 6) + decoder[c];
        if (bits & 0x1000000) {
            if (size < 3)
                break;
            *(dest++) = (uint8_t)((bits >> 16) & 0xff);
            *(dest++) = (uint8_t)((bits >> 8) & 0xff);
            *(dest++) = (uint8_t)((bits & 0xff));
            bits = 1;
            size -= 3;
            count += 3;
        }
    }
    return count;
}

#define CRC24_INIT 0xb704ceL
#define CRC24_POLY 0x1864cfbL

uint32_t String::crc24(uint8_t *binary, size_t size)
{
    uint32_t crc = CRC24_INIT;
    unsigned i;

    while (size--) {
        crc ^= (*binary++) << 16;
        for (i = 0; i < 8; i++) {
            crc <<= 1;
            if (crc & 0x1000000)
                crc ^= CRC24_POLY;
        }
    }
    return crc & 0xffffffL;
}

uint16_t String::crc16(uint8_t *binary, size_t size)
{
    uint16_t crc = 0xffff;
    unsigned i;

    while (size--) {
        crc ^= (*binary++);
        for (i = 0; i < 8; i++) {
            if(crc & 1)
                crc = (crc >> 1) ^ 0xa001;
            else
                crc = (crc >> 1);
        }
    }
    return crc;
}

