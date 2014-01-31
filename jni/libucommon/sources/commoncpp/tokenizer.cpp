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

#include <ucommon-config.h>
#include <commoncpp/config.h>
#include <commoncpp/export.h>
#include <commoncpp/string.h>
#include <commoncpp/exception.h>
#include <commoncpp/thread.h>
#include <commoncpp/tokenizer.h>
#include <cstdlib>
#include <cstdio>

NAMESPACE_COMMONCPP
using namespace std;

// sorted by the usual probability of occurence
// see also: manpage of isspace()
const char * const StringTokenizer::SPACE=" \t\n\r\f\v";

StringTokenizer::StringTokenizer (const char *_str, const char *_delim, bool _skipAll, bool _trim) :
str(_str),delim(_delim),skipAll(_skipAll),trim(_trim)
{
    if (str == 0)
        itEnd = iterator(*this, 0);
    else
        itEnd = iterator(*this,strchr(str, '\0')+1);
}

StringTokenizer::StringTokenizer (const char *s) :
str(s), delim(SPACE), skipAll(false),trim(true)
{
    if (str == 0)
        itEnd = iterator(*this, 0);
    else
        itEnd = iterator(*this,strchr(str, '\0')+1);
}


StringTokenizer::iterator& StringTokenizer::iterator::operator ++ () THROWS (StringTokenizer::NoSuchElementException)
{

    // someone requested to read beyond the end .. tsts
    if (endp == myTok->itEnd.endp)
        THROW (NoSuchElementException());

    if (token) {
        // this is to help people find their bugs, if they
        // still maintain a pointer to this invalidated
        // area :-)
        *token = '\0';
        delete[] token;
        token = 0;
    }

    start = ++endp;
    if (endp == myTok->itEnd.endp) return *this; // done

    // search for next delimiter
    while (*endp && strchr(myTok->delim, *endp)==NULL)
        ++endp;

    tokEnd = endp;

    if (*endp && myTok->skipAll) { // skip all delimiters
        while (*(endp+1) && strchr(myTok->delim, *(endp+1)))
            ++endp;
    }
    return *this;
}

/*
 * if no one requests the token, no time is spent skipping the whitespaces
 * or allocating memory.
 */
const char * StringTokenizer::iterator::operator * () THROWS (StringTokenizer::NoSuchElementException)
{
    // someone requested to read beyond the end .. tsts
    if (endp == myTok->itEnd.endp)
        THROW (NoSuchElementException());

    if (!token) {
        /*
         * someone requests this token; return a copy to provide
         * a NULL terminated string.
         */

        /* don't clobber tokEnd, it is used in nextDelimiter() */
        const char *wsTokEnd = tokEnd;
        if (myTok->trim) {
            while (wsTokEnd > start && strchr(SPACE, *start))
                ++start;
            while (wsTokEnd > start && strchr(SPACE,*(wsTokEnd-1)))
                --wsTokEnd;
        }
        size_t tokLen = wsTokEnd - start;
        if (start > wsTokEnd) {
            tokLen = 0;
        }
        token = newString(start, tokLen + 1);
    }
    return token;
}

END_NAMESPACE

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
