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
 * @file tokenizer.h
 * @short string tokenizer.
 **/

#ifndef COMMONCPP_TOKENIZER_H_
#define COMMONCPP_TOKENIZER_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_THREAD_H_
#include <commoncpp/thread.h>
#endif

#ifndef COMMMONCPP_EXCEPTION_H_
#include <commoncpp/exception.h>
#endif

NAMESPACE_COMMONCPP

/**
 * Splits delimited string into tokens.
 *
 * The StringTokenizer takes a pointer to a string and a pointer
 * to a string containing a number of possible delimiters.
 * The StringTokenizer provides an input forward iterator which allows
 * to iterate through all tokens. An iterator behaves like a logical
 * pointer to the tokens, i.e. to shift to the next token, you've
 * to increment the iterator, you get the token by dereferencing the
 * iterator.
 *
 * Memory consumption:
 * This class operates on the original string and only allocates memory
 * for the individual tokens actually requested, so this class
 * allocates at maximum the space required for the longest token in the
 * given string.
 * Since for each iteration, memory is reclaimed for the last token,
 * you MAY NOT store pointers to them; if you need them afterwards,
 * copy them. You may not modify the original string while you operate
 * on it with the StringTokenizer; the behaviour is undefined in that
 * case.
 *
 * The iterator has one special method 'nextDelimiter()' which returns
 * a character containing the next delimiter following this
 * tokenization process or '\\0', if there are no following delimiters. In
 * case of skipAllDelim, it returns the FIRST delimiter.
 *
 * With the method 'setDelimiters(const char*)' you may change the
 * set of delimiters. It affects all running iterators.
 *
 * Example:
 * <code><pre>
 *  StringTokenizer st("mary had a little lamb;its fleece was..", " ;");
 *  StringTokenizer::iterator i;
 *  for (i = st.begin() ; i != st.end() ; ++i) {
 *        cout << "Token: '" << *i << "'\t";
 *        cout << " next Delim: '" << i.nextDelimiter() << "'" << endl;
 *  }
 *  </pre></code>
 *
 * @author Henner Zeller <H.Zeller@acm.org>
 * @license LGPL
 */
class __EXPORT StringTokenizer {
public:
    /**
     * a delimiter string containing all usual whitespace delimiters.
     * These are space, tab, newline, carriage return,
     * formfeed and vertical tab. (see isspace() manpage).
     */
    static const char * const SPACE;

    /**
     * Exception thrown, if someone tried to read beyond the
     * end of the tokens.
     * Will not happen if you use it the 'clean' way with comparison
     * against end(), but if you skip some tokens, because you 'know'
     * they are there. Simplifies error handling a lot, since you can
     * just read your tokens the way you expect it, and if there is some
     * error in the input this Exception will be thrown.
     */
    // maybe move more global ?
    class NoSuchElementException { };

    /**
     * The input forward iterator for tokens.
     * @author Henner Zeller
     */
    class __EXPORT iterator {
        friend class StringTokenizer;  // access our private constructors
    private:
        const StringTokenizer *myTok; // my StringTokenizer
        const char *start;      // start of current token
        const char *tokEnd;     // end of current token (->nxDelimiter)
        const char *endp;       // one before next token
        char *token;            // allocated token, if requested

        // for initialization of the itEnd iterator
        iterator(const StringTokenizer &tok, const char *end)
            : myTok(&tok),tokEnd(0),endp(end),token(0) {}

        iterator(const StringTokenizer &tok)
            : myTok(&tok),tokEnd(0),endp(myTok->str-1),token(0) {
            ++(*this); // init first token.
        }

    public:
        iterator() : myTok(0),start(0),tokEnd(0),endp(0),token(0) {}

        // see also: comment in implementation of operator++
        virtual ~iterator()
            { if (token) *token='\0'; delete [] token; }

        /**
         * copy constructor.
         */
        // everything, but not responsible for the allocated token.
        iterator(const iterator& i) :
            myTok(i.myTok),start(i.start),tokEnd(i.tokEnd),
            endp(i.endp),token(0) {}

        /**
         * assignment operator.
         */
        // everything, but not responsible for the allocated token.
        iterator &operator = (const iterator &i)
        {
            myTok = i.myTok;
            start = i.start; endp = i.endp; tokEnd = i.tokEnd;
            if ( token )
                delete [] token;
            token = 0;
            return *this;
        }

        /**
         * shifts this iterator to the next token in the string.
         */
        iterator &operator ++ () THROWS (NoSuchElementException);

        /**
         * returns the immutable string this iterator
         * points to or '0' if no token is available (i.e.
         * i == end()).
         * Do not store pointers to this token, since it is
         * invalidated for each iteration. If you need the token,
         * copy it (e.g. with strdup());
         */
        const char*  operator *  () THROWS (NoSuchElementException);

        /**
         * returns the next delimiter after the current token or
         * '\\0', if there are no following delimiters.
         * It returns the very next delimiter (even if
         * skipAllDelim=true).
         */
        inline char nextDelimiter() const
            {return (tokEnd) ? *tokEnd : '\0';}

        /**
         * compares to other iterator. Usually used to
         * compare against the end() iterator.
         */
        // only compare the end-position. speed.
        inline bool operator == (const iterator &other) const
            {return (endp == other.endp);}

        /**
         * compares to other iterator. Usually used to
         * compare against the end() iterator.
         */
        // only compare the end position. speed.
        inline bool operator != (const iterator &other) const
            {return (endp != other.endp);}
    };
private:
    friend class StringTokenizer::iterator;
    const char *str;
    const char *delim;
    bool skipAll, trim;
    iterator itEnd;

public:
    /**
     * creates a new StringTokenizer for a string
     * and a given set of delimiters.
     *
     * @param  str          String to be split up. This string will
     *                      not be modified by this StringTokenizer,
     *                      but you may as well not modfiy this string
     *                      while tokenizing is in process, which may
     *                      lead to undefined behaviour.
     *
     * @param  delim        String containing the characters
     *                      which should be regarded as delimiters.
     *
     * @param  skipAllDelim OPTIONAL.
     *                      true, if subsequent
     *                      delimiters should be skipped at once
     *                      or false, if empty tokens should
     *                      be returned for two delimiters with
     *                      no other text inbetween. The first
     *                      behaviour may be desirable for whitespace
     *                      skipping, the second for input with
     *                      delimited entry e.g. /etc/passwd like files
     *                      or CSV input.
     *                      NOTE, that 'true' here resembles the
     *                      ANSI-C strtok(char *s,char *d) behaviour.
     *                      DEFAULT = false
     *
     * @param trim          OPTIONAL.
     *                      true, if the tokens returned
     *                      should be trimmed, so that they don't have
     *                      any whitespaces at the beginning or end.
     *                      Whitespaces are any of the characters
     *                      defined in StringTokenizer::SPACE.
     *                      If delim itself is StringTokenizer::SPACE,
     *                      this will result in a behaviour with
     *                      skipAllDelim = true.
     *                      DEFAULT = false
     */
    StringTokenizer (const char *str,
             const char *delim,
             bool skipAllDelim = false,
             bool trim = false);

    /**
     * create a new StringTokenizer which splits the input
     * string at whitespaces. The tokens are stripped from
     * whitespaces. This means, if you change the set of
     * delimiters in either the 'begin(const char *delim)' method
     * or in 'setDelimiters()', you then get whitespace
     * trimmed tokens, delimited by the new set.
     * Behaves like StringTokenizer(s, StringTokenizer::SPACE,false,true);
     */
    StringTokenizer (const char *s);

    /**
     * returns the begin iterator
     */
    iterator begin() const
        {return iterator(*this);}

    /**
     * changes the set of delimiters used in subsequent
     * iterations.
     */
    void setDelimiters (const char *d)
        {delim = d;}

    /**
     * returns a begin iterator with an alternate set of
     * delimiters.
     */
    iterator begin(const char *d)
    {
        delim = d;
        return iterator(*this);
    }

    /**
     * the iterator marking the end.
     */
    const iterator& end() const
        {return itEnd;}
};

END_NAMESPACE

#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
