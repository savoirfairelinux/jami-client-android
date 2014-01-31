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
#include <ucommon/memory.h>
#include <stdarg.h>
#include <ctype.h>
#include <stdio.h>
#ifdef  HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <limits.h>
#ifdef  HAVE_REGEX_H
#include <regex.h>
#endif

using namespace UCOMMON_NAMESPACE;

String::regex::regex(const char *pattern, size_t slots)
{
#ifdef  HAVE_REGEX_H
    regex_t *r = (regex_t *)malloc(sizeof(regex_t));
    if(regcomp(r, pattern, 0)) {
        regfree(r);
        free(r);
        r = NULL;
    }
    object = r;
    count = slots;
    results = (regmatch_t *)malloc(sizeof(regmatch_t) * slots);
#else
    object = results = NULL;
    count = 0;
#endif
}

String::regex::regex(size_t slots)
{
#ifdef  HAVE_REGEX_H
    count = slots;
    results = (regmatch_t *)malloc(sizeof(regmatch_t) * slots);
    object = NULL;
#else
    object = results = NULL;
    count = 0;
#endif
}

String::regex& String::regex::operator=(const char *pattern)
{
#ifdef  HAVE_REGEX_H
    if(object) {
        regfree((regex_t *)object);
        free(object);
    }
    regex_t *r = (regex_t *)malloc(sizeof(regex_t));
    if(regcomp(r, pattern, 0)) {
        regfree(r);
        free(r);
        r = NULL;
    }
    object = r;
#endif
    return *this;
}

bool String::regex::operator*=(const char *text)
{
    return match(text);
}

String::regex::~regex()
{
#ifdef  HAVE_REGEX_H
    if(object) {
        regfree((regex_t *)object);
        free(object);
    }
    if(results)
        free(results);
    object = results = NULL;
#endif
}

size_t String::regex::offset(unsigned member)
{
#ifdef  HAVE_REGEX_H
    if(!results)
        return (size_t)-1;

    regmatch_t *r = (regmatch_t *)results;

    if(member >= count)
        return (size_t)-1;
    return (size_t)r[member].rm_so;
#else
    return (size_t)-1;
#endif
}

size_t String::regex::size(unsigned member)
{
#ifdef  HAVE_REGEX_H
    if(!results)
        return 0;

    regmatch_t *r = (regmatch_t *)results;

    if(member >= count)
        return (size_t)-1;

    if(r[member].rm_so == -1)
        return 0;

    return (size_t)(r[member].rm_eo - r[member].rm_so);
#else
    return (size_t)0;
#endif
}

bool String::regex::match(const char *text, unsigned mode)
{
#ifdef  HAVE_REGEX_H
    int flags = 0;

    if((mode & 0x01) == INSENSITIVE)
        flags |= REG_ICASE;

    if(!text || !object || !results)
        return false;

    if(regexec((regex_t *)object, text, count, (regmatch_t *)results, flags))
        return false;

    return true;
#else
    return false;
#endif
}

const char *String::search(regex& expr, unsigned member, unsigned flags) const
{
    if(!str)
        return NULL;

#ifdef  HAVE_REGEX_H
    if(expr.match(str->text, flags))
        return NULL;

    if(member >= expr.members())
        return NULL;

    if(expr.size(member) == 0)
        return NULL;

    return str->text + expr.offset(member);
#else
    return NULL;
#endif
}

unsigned String::replace(regex& expr, const char *cp, unsigned flags)
{
#ifdef  HAVE_REGEX_H
    size_t cpl = 0;

    if(cp)
        cpl = strlen(cp);

    if(!str || str->len == 0)
        return 0;

    if(expr.match(str->text, flags))
        return 0;

    ssize_t adjust = 0;
    unsigned member = 0;

    while(member < expr.members()) {
        ssize_t tcl = expr.size(member);
        ssize_t offset = (expr.offset(member) + adjust);
        if(!tcl)
            break;

        ++member;
        cut(offset, tcl);
        if(cpl) {
            paste(offset, cp);
            adjust += (cpl - tcl);
        }
    }
    return member;
#else
    return 0;
#endif
}

bool String::operator*=(regex& expr)
{
    if(search(expr))
        return true;

    return false;
}

unsigned StringPager::split(stringex_t& expr, const char *string, unsigned flags)
{
    strdup_t tmp = String::dup(string);
    int prior = 0, match = 0;
    size_t tcl = strlen(string);
    unsigned count = 0, member = 0;

    if(!expr.match(string, flags))
        return 0;

    while(member < expr.members()) {
        if(!expr.size(member))
            break;
        match = expr.offset(member++);
        if(match > prior) {
            tmp[match] = 0;
            add(tmp + (size_t)prior);
            ++count;
        }
        prior = match + tcl;
    }
    if(tmp[prior]) {
        add(tmp + (size_t)prior);
        ++count;
    }
    return count;
}

