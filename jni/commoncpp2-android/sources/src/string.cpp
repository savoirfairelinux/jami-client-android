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
#include <cc++/export.h>
#include <cc++/thread.h>
#include <cc++/misc.h>
#include <cc++/strchar.h>
#include <cc++/string.h>
#include "private.h"

#ifdef  __BORLANDC__
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#else
#include <cstdarg>
#include <cstdio>
#include <cstdlib>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
using std::streambuf;
using std::ofstream;
using std::ostream;
using std::endl;
using std::ios;
#endif

static Mutex mutex;
MemPager *String::pager = NULL;
char **String::idx = NULL;

const unsigned String::minsize = ((sizeof(char *) + (sizeof(unsigned) * 2) + 1));
const unsigned String::slotsize = 32;
const unsigned String::pagesize = 1024;
const unsigned String::slotlimit = 512;
const unsigned String::slotcount = ((slotlimit / slotsize) + 1);
const size_t String::npos = (size_t)(-1);

String::String()
{
    init();
}

String::String(std::string str)
{
       init();
       set(str.c_str());
}

String::String(const String &original)
{
    init();
    copy(original);
}

String::String(size_t chars, const char chr)
{
    init();
    resize(chars + 1);
    memset(getText(), chr, chars);
    setLength(chars);
}

#ifdef  HAVE_SNPRINTF
String::String(size_t chars, const char *format, ...)
{
    va_list args;
    va_start(args, format);

    init();
    resize(chars);

    char *ptr = getText();
    vsnprintf(ptr, chars, format, args);
    setLength(strlen(ptr));
    va_end(args);
}
#else
String::String(size_t chars, const char *str)
{
    init();
    resize(chars);

    if(!str || !*str)
        return;

    set(str);
}
#endif

String::String(const char *str)
{
    init();
    set(str);
}

String::String(const String &str, size_t start, size_t chars)
{
    init();
    char *ptr = str.getText();
    size_t len = str.getLength();

    if(start >= len)
        return;

    ptr += start;
    len -= start;

    if(chars >= len)
        chars = len;

    set(ptr, chars);
}

char String::at(ssize_t ind) const
{
    if(ind < 0)
        ind = (ssize_t)(getLength() - ind + 1);
    if((size_t)ind > getLength() || ind < 0)
        return 0;

    return (getText())[ind];
}

void String::insert(size_t start, const char *str, size_t chars)
{
    char *ptr = getText();
    size_t len = getLength();
    size_t sz = getSize();

    if(!str)
        str = "";

    if(!chars)
        chars = strlen(str);

    if(!chars || start > len)
        return;

    if(len + chars >= sz) {
        resize(len + chars + 1);
        ptr = getText();
    }

    if(start == len) {
        memmove(ptr + start, str, chars);
        len += chars;
        setLength(len);
        ptr[len] = 0;
        return;
    }

    memmove(ptr + start + chars, ptr + start, len - start);
    memmove(ptr + start, str, chars);
    len += chars;
    setLength(len);
    ptr[len] = 0;
    return;
}


void String::insert(size_t start, const String &s)
{
    insert(start, s.getText(), s.getLength());
}

void String::replace(size_t start, size_t len, const char *cp, size_t chars)
{
    erase(start, len);
    insert(start, cp, chars);
}

void String::replace(size_t start, size_t len, const String &s)
{
    erase(start, len);
    insert(start, s);
}

void String::erase(size_t start, size_t chars)
{
    size_t len = getLength();
    char *ptr = getText();

    if(start >= len)
        return;

    if(start + chars >= len || chars == npos || !chars) {
        setLength(start);
        ptr[start] = 0;
        return;
    }

    memmove(ptr + start, ptr + start + chars, len - start - chars);
    len -= chars;
    setLength(len);
    ptr[len] = 0;
}

void String::append(const char *str, size_t offset, size_t len)
{
    size_t slen = getLength();
    char *ptr = getText();

    if(slen < offset) {
        append(str, len);
        return;
    }

    setLength(offset);
    ptr[offset] = 0;
    append(str, len);
}

void String::append(const char *str, size_t len)
{
    if(!str)
        return;

    if(!len)
        len = strlen(str);

    if(!len)
        return;

    if(getLength() + len >= getSize())
        resize(getLength() + len + 1);
    memmove(getText() + getLength(), str, len);
    len += getLength();
    setLength(len);
    getText()[len] = 0;
    return;
}

void String::add(char c)
{
    size_t len = getLength();
    char *ptr;

    if(len + 1 >= getSize())
        resize(len + 2);

    ptr = getText();
    ptr[len++] = c;
    setLength(len);
    ptr[len] = 0;
}

void String::trim(size_t chars)
{
    size_t len = getLength();
    char *ptr = getText();

    if(chars >= len)
        chars = len;

    len -= chars;
    ptr[len] = 0;
    setLength(len);
}

String String::token(const char *delim, size_t offset)
{
    char *ptr = getText();
    size_t len = getLength();
    size_t chars = 0;
    String result;
    bool found = false;

    if(offset >= len)
        return result;

    len -= offset;
    ptr += offset;

    while(chars < len) {
        if(strchr(delim, ptr[chars])) {
            found = true;
            break;
        }
        ++chars;
    }

    if(!chars && found)
        erase(offset, 1);

    if(!chars)
        return result;

    result.set(ptr, chars);
    if(found)
        ++chars;
    erase(offset, chars);
    return result;
}

void String::strip(const char *chars)
{
    size_t len = strtrim(chars, getText(), getLength());
    if(!len) {
        setLength(len);
        return;
    }
    setLength(strchop(chars, getText(), len));
}

#ifdef  HAVE_SNPRINTF
const char *String::set(size_t chars, const char *format, ...)
{
    va_list args;
    va_start(args, format);

    if(chars <= minsize)
        clear();

    if(chars > getSize())
        resize(chars);

    char *ptr = getText();
    vsnprintf(ptr, chars, format, args);
    setLength(strlen(ptr));
    va_end(args);
    return ptr;
}

void String::append(size_t chars, const char *format, ...)
{
    va_list args;
    va_start(args, format);
    size_t len = getLength();

    if(len + chars <= minsize)
        clear();

    if(len + chars > getSize())
        resize(len + chars);

    char *ptr = getText() + len;
    vsnprintf(ptr, chars, format, args);
    setLength(strlen(getText()));
    va_end(args);
}

#endif
const char *String::set(const char *str, size_t len)
{
    if(!str) {
        clear();
        return str;
    }

    if(!len)
        len = strlen(str);

    // if we are making a short string, lets clear prior alloc

    if(len < minsize)
        clear();

    if(len >= getSize())
        resize(len + 1);
    memmove(getText(), str, len);
    getText()[len] = 0;
    setLength(len);
    return str;
}

void String::set(const String &str)
{
    set(str.getText(), str.getLength());
}

void String::append(const String &str)
{
    append(str.getText(), str.getLength());
}


void String::copy(const String &original)
{
    clear();

    if(original.getLength() < minsize) {
        content.ministring.length = (unsigned)original.getLength();
        memmove(content.ministring.text, original.getText(), original.getLength() + 1);
        content.ministring.big = false;
        return;
    }

//  ptr = original.getText();
    content.bigstring.length = original.getLength();
    content.bigstring.size = setSize(original.getLength() + 1);
    content.bigstring.text = getSpace(original.getLength() + 1);
    content.ministring.big = true;
    memmove(content.bigstring.text, original.getText(), original.getLength() + 1);
}

String::~String()
{
    clear();
}

void String::init(void)
{
    content.ministring.big = false;
    content.ministring.length = 0;
    content.ministring.text[0] = 0;
}

size_t String::search(const char *cp, size_t clen, size_t ind) const
{
    size_t len = getLength();

    if(!cp)
        cp = "";

    if(!clen)
        clen = strlen(cp);

    while(clen + ind <= len) {
        if(compare(cp, clen, ind) == 0)
            return ind;
        ++ind;
    }
    return npos;
}

size_t String::find(const char *cp, size_t ind, size_t len, unsigned instance) const
{
    size_t pos = npos;

    if(!cp)
        cp = "";

    if(!len)
        len = strlen(cp);

    while(instance--) {
        pos = search(cp, len, ind);
        if(pos == npos)
            break;
        ind = pos + 1;
    }
    return pos;
}

size_t String::rfind(const char *cp, size_t ind, size_t len) const
{
    size_t result = npos;

    if(!cp)
        cp = "";

    if(!len)
        len = strlen(cp);

    for(;;) {
        ind = search(cp, len, ind);
        if(ind == npos)
            break;
        result = ind++;
    }
    return result;
}

unsigned String::count(const char *cp, size_t ind, size_t len) const
{
    unsigned chars = 0;
    if(!cp)
        cp = "";

    if(!len)
        len = strlen(cp);

    for(;;) {
        ind = search(cp, len, ind);
        if(ind == npos)
            break;
        ++chars;
        ++ind;
    }
    return chars;
}

unsigned String::count(const String &str, size_t ind) const
{
    return count(str.getText(), ind, str.getLength());
}

size_t String::find(const String &str, size_t ind, unsigned instance) const
{
    return find(str.getText(), ind, str.getLength(), instance);
}

size_t String::rfind(const String &str, size_t ind) const
{
    return rfind(str.getText(), ind, str.getLength());
}

int String::compare(const char *cp, size_t len, size_t ind) const
{
    if(!cp)
        cp = "";

    if(ind > getLength())
        return -1;

    if(!len)
        return strcmp(getText() + ind, cp);

    return strncmp(getText() + ind, cp, len);
}

bool String::isEmpty(void) const
{
    char *ptr = getText();

    if(!ptr || !*ptr)
        return true;

    return false;
}

void String::resize(size_t chars)
{
    size_t len = getLength();
    char *ptr;

    if(len >= chars)
        len = chars - 1;

    ++len;

    if(!isBig() && chars <= minsize)
        return;

    if(!isBig()) {
        ptr = getSpace(chars);
        memmove(ptr, content.ministring.text, len);
        ptr[--len] = 0;
        content.ministring.big = true;
        content.bigstring.text = ptr;
        content.bigstring.length = len;
        setSize(chars);
        return;
    }

    if(chars <= minsize && getSize() > slotlimit) {
        ptr = getText();
        memmove(content.ministring.text, ptr, len);
        content.ministring.text[--len] = 0;
        content.ministring.big = false;
        content.ministring.length = (unsigned)len;
        delete[] ptr;
        return;
    }

    ptr = getSpace(chars);
    memmove(ptr, getText(), len);
    ptr[--len] = 0;
    clear();
    setSize(chars);
    content.bigstring.length = len;
    content.bigstring.text = ptr;
    content.ministring.big = true;
}

void String::clear(void)
{
    char **next;
    unsigned slot;

    if(!isBig())
        goto end;

    if(!content.bigstring.text)
        goto end;

    if(getSize() > slotlimit) {
        delete[] content.bigstring.text;
        goto end;
    }

    slot = ((unsigned)getSize() - 1) / slotsize;
    next = (char **)content.bigstring.text;
    mutex.enterMutex();
    *next = idx[slot];
    idx[slot] = content.bigstring.text;
    setLength(0);
    content.bigstring.text = NULL;
    mutex.leaveMutex();

end:
    init();
    return;
}

char *String::getSpace(size_t chars)
{
    unsigned slot;
    char *cp, **next;

    if(chars > slotlimit)
        return new char[chars];

    slot = (unsigned)chars / slotsize;
    mutex.enterMutex();
    if(!pager) {
        pager = new MemPager(pagesize);
        idx = (char **)pager->alloc(sizeof(char *) * slotcount);
        memset(idx, 0, sizeof(char *) * slotcount);
    }
    cp = idx[slot];
    if(cp) {
        next = (char **)cp;
        idx[slot] = *next;
    }
    else
        cp = (char *)pager->alloc(++slot * slotsize);
    mutex.leaveMutex();
    return cp;
}

const char *String::getIndex(size_t ind) const
{
    const char *dp = getText();

    if(ind > getLength())
        return NULL;

    return (const char *)dp + ind;
}

char *String::getText(void) const
{
    if(isBig())
        return (char *)content.bigstring.text;

    return (char *)content.ministring.text;
}

long String::getValue(long def) const
{
    unsigned base = 10;
    char *cp = getText();
    char *ep = 0;
    long val;

    if(!cp)
        return def;

    if(!strnicmp(cp, "0x", 2)) {
        cp += 2;
        base = 16;
    }

    val = ::strtol(cp, &ep, base);
    if(!ep || *ep)
        return def;
    return val;
}

bool String::getBool(bool def) const
{
    char *cp = getText();

    if(!cp)
        return def;

    if(isdigit(*cp)) {
        if(!getValue(0))
            return false;
        return true;
    }

    if(!stricmp(cp, "true") || !stricmp(cp, "yes"))
        return true;

    if(!stricmp(cp, "false") || !stricmp(cp, "no"))
        return false;

    return def;
}

const size_t String::getSize(void) const
{
    if(isBig())
        return content.bigstring.size;

    return minsize;
}

void String::setLength(size_t len)
{
    if(isBig())
        content.bigstring.length = len;
    else
        content.ministring.length = (unsigned)len;
}

size_t String::setSize(size_t chars)
{
    if(chars <= minsize && !isBig())
        return minsize;

  if(chars <= slotlimit) {
    size_t slotcount = chars / slotsize;
    if((chars % slotsize)!=0) ++slotcount;
    chars = slotcount*slotsize;
  }
    content.bigstring.size = chars;
    return chars;
}

const size_t String::getLength(void) const
{
    if(isBig())
        return content.bigstring.length;

    return content.ministring.length;
}

String operator+(const String &s1, const char c2)
{
    String result(s1);
    result.add(c2);
    return result;
}

String operator+(const String &s1, const String &s2)
{
    String result(s1);
    result.append(s2);
    return result;
}

String operator+(const String &s1, const char *s2)
{
    String result(s1);
    result += s2;
    return result;
}

String operator+(const char *s1, const String &s2)
{
    String result(s1);
    result += s2;
    return result;
}

String operator+(const char c1, const String &s2)
{
    String result(1, c1);
    result += s2;
    return result;
}

bool String::operator<(const String &s1) const
{
    if(compare(s1.getText()) < 0)
        return true;

    return false;
}

bool String::operator<(const char *s1) const
{
    if(compare(s1) < 0)
        return true;

    return false;
}

bool String::operator>(const String &s1) const
{
    if(compare(s1.getText()) > 0)
        return true;

    return false;
}

bool String::operator>(const char *s1) const
{
    if(compare(s1) > 0)
        return true;

    return false;
}

bool String::operator<=(const String &s1) const
{
    if(compare(s1.getText()) <= 0)
        return true;

    return false;
}

bool String::operator<=(const char *s1) const
{
    if(compare(s1) <= 0)
        return true;

    return false;
}

bool String::operator>=(const String &s1) const
{
    if(compare(s1.getText()) >= 0)
        return true;

    return false;
}

bool String::operator>=(const char *s1) const
{
    if(compare(s1) >= 0)
        return true;

    return false;
}

bool String::operator==(const String &s1) const
{
    if(compare(s1.getText()) == 0)
        return true;

    return false;
}

bool String::operator==(const char *s1) const
{
    if(compare(s1) == 0)
        return true;

    return false;
}

bool String::operator!=(const String &s1) const
{
    if(compare(s1.getText()) != 0)
        return true;

    return false;
}

bool String::operator!=(const char *s1) const
{
    if(compare(s1) != 0)
        return true;

    return false;
}

bool String::operator*=(const String &s1) const
{
    return search(s1.getText(), s1.getLength()) != npos;
}

bool String::operator*=(const char *s) const
{
    return search(s) != npos;
}

std::ostream &operator<<(std::ostream &os, const String &str)
{
    os << str.getText();
    return os;
}

void *StringObject::operator new(size_t size) NEW_THROWS
{
    char *base;
    size_t *sp;
    size += sizeof(size_t);

    if(size > String::slotlimit)
        return NULL;

    base = String::getSpace(size);

    if(!base)
        return NULL;

    sp = (size_t *)base;
    *sp = size;
    base += sizeof(size_t);
    return (void *)base;
}

void StringObject::operator delete(void *ptr)
{
    char **next;
    unsigned slot;
    size_t *size = (size_t *)ptr;

    --size;
    ptr = size;

    slot = (unsigned)(((*size) - 1) / String::slotsize);
    next = ((char **)(ptr));
    mutex.enter();
    *next = String::idx[slot];
    String::idx[slot] = (char *)ptr;
    mutex.leave();
}

std::istream &getline(std::istream &is, String &str, char delim, size_t len)
{
    if(!len)
        len = str.getSize() - 1;

    if(len >= str.getSize())
        str.resize(len + 1);

    char *ptr = str.getText();
    is.getline(ptr, (std::streamsize)len, delim);
    str.setLength(strlen(ptr));
    return is;
}

SString::SString() :
String(), streambuf()
#ifdef  HAVE_OLD_IOSTREAM
,ostream()
#else
,ostream((streambuf *)this)
#endif
{
#ifdef  HAVE_OLD_IOSTREAM
        ostream::init((streambuf *)this);
#endif
}

SString::SString(const SString &from) :
String(from), streambuf()
#ifdef  HAVE_OLD_IOSTREAM
,ostream()
#else
,ostream((streambuf *)this)
#endif
{
#ifdef  HAVE_OLD_IOSTREAM
    ostream::init((streambuf *)this);
#endif
}

SString::~SString()
{
    if(isBig())
        String::clear();
}

int SString::overflow(int c)
{
    String::add((char)(c));
    return c;
}

#ifdef  HAVE_SNPRINTF
int strprintf(String &str, size_t size, const char *format, ...)
{
    va_list args;
    va_start(args, format);
    int rtn;

    if(!size)
        size = str.getSize();

    if(size > str.getSize())
        str.resize(size);

    char *ptr = str.getText();
    str.setLength(0);
    ptr[0] = 0;
    rtn = vsnprintf(ptr, size, format, args);
    str.setLength(strlen(ptr));
    va_end(args);
    return rtn;
}
#endif

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
