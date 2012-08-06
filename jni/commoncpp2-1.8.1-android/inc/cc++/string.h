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
 * @file string.h
 * @short Common C++ generic string class
 **/

#ifndef CCXX_STRING_H_
#define CCXX_STRING_H_

#ifndef CCXX_MISSING_H_
#include <cc++/missing.h>
#endif

#ifndef CCXX_STRCHAR_H_
#include <cc++/strchar.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

class MemPager;

/**
 * This is a generic and portable string class.  It uses optimized
 * memory allocation strategies to efficiently handle smaller string
 * content by grouping strings into 32 byte aligned slots that can
 * be re-allocated from a free list directly.
 *
 * While meant to cover the basic functionality of the ANSI C++
 * string class in form and function, this class offers some important
 * enhancements, including the ability to derive class type specific
 * versions of itself.  The latter might be used to derive a unicode
 * string, a string for data and time data types, or to add case
 * insensitive comparisons, for example.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Generic string class.
 */
class __EXPORT String
{
protected:
    static const unsigned minsize;
    static const unsigned slotsize;
    static const unsigned pagesize;
    static const unsigned slotlimit;
    static const unsigned slotcount;

    friend class StringObject;

private:
    friend class MemPager;

    static MemPager *pager;
    static char **idx;

#ifdef  CCXX_PACKED
#pragma pack(1)
#endif

    union {
        struct {
            char *text;
            size_t size;
            size_t length;
        }   bigstring;
        struct {
            char text[(sizeof(char *) + (sizeof(size_t) * 2) + 1)];
            char length : 6;
            bool big : 1;
        }   ministring;
    }   content;

#ifdef  CCXX_PACKED
#pragma pack()
#endif

protected:
    /**
     * Determine if string is allocated in local variable or an
     * external reference.
     *
     * @return true if external heap is used.
     */
    inline bool isBig(void) const
        {return content.ministring.big;};

    /**
     * Set the content of the string variable to the specified
     * string value, and use smart re-allocation strategies if
     * appropriate to shrink the size of the variable.
     *
     * @param str string to set.
     * @param len length of string if passed.
     */
    const char *set(const char *str, size_t len = 0);

    /**
     * Set the content of the string variable to that of another
     * variable.  Uses the string set method.
     *
     * @param str string to copy from.
     */
    void set(const String &str);

#ifdef  HAVE_SNPRINTF
    /**
     * Set the content of the string variable to that of a
     * formatted printf style string.
     *
     * @param size of string data to set.
     * @param format of string to write into object.
     */
    const char *set(size_t size, const char *format, ...);
#endif

    /**
     * Impliment the copy constructor, used internally.  Will always
     * create a minimum sized string allocation.
     *
     * @param str string to copy from.
     */
    void copy(const String &str);

    /**
     * Used to initialize a string object.
     */
    void init(void);

    /**
     * Used to fetch memory, if needed, based on the size, from the
     * pager, or the system heap.
     *
     * @return string pointer to space.
     * @param size of space needed.
     */
    static char *getSpace(size_t size);

    /**
     * Set the size of allocated space in the string variable
     * (capacity) to a known value.  The value is recomputed and
     * adjusted based on allocation method.
     *
     * @param size in bytes.
     */
    size_t setSize(size_t size);

    /**
     * Set the length value of the string content.
     *
     * @param len size in bytes.
     */
    void setLength(size_t len);

    /**
     * A derivable low level comparison operator.  This can be used
     * to create custom comparison data types in derived string
     * classes.
     *
     * @return 0 if match, or value for ordering.
     * @param text text to compare.
     * @param len length of text to compare.
     * @param index offset from start of string, used in searchs.
     */
    virtual int compare(const char *text, size_t len = 0, size_t index = 0) const;

    /**
     * An internal method used to search for a substring starting at
     * a known offset.  Used by find and count methods.
     *
     * @return npos if fails, or offset to text found.
     * @param text text to search for.
     * @param clen length of search text.
     * @param offset offset to start from.
     */
    size_t search(const char *text, size_t clen = 0, size_t offset = 0) const;

public:
    static const size_t npos;

    typedef size_t size_type;

    /**
     * Construct an empty string.
     */
    String();

    /**
     * Copy constructor.
     *
     * @param original string to copy from.
     */
    String(const String &original);

    /**
     * Create a string from a cstring.
     *
     * @param str text to set with.
     */
    String(const char *str);

    /**
     * Create a String from std::string.
     *
     * @param string from std::string to copy from.
     */
    String(std::string string);

    /**
     * Create a new string from a subset of another string.
     *
     * @param str reference of source string.
     * @param offset offset to start of data in prior string.
     * @param len length of our substring.
     */
    String(const String &str, size_t offset, size_t len = npos);

#ifdef  HAVE_SNPRINTF
    /**
     * Create a string from formatted text input.
     *
     * @param size to allocate for our new string.
     * @param format of data to input.
     */
    String(size_t size, const char *format, ...);
#else
    /**
     * Create a string of a known size, and optionally fill with
     * content.
     *
     * @param count size to allocate for our new string.
     * @param str content to put into it.
     */
    String(size_t count, const char *str);
#endif

    /**
     * Fill a new string with character data.
     *
     * @param count size of new string.
     * @param fill char to fill string with.
     */
    String(size_t count, const char fill = ' ');

    /**
     * Destroy the string...
     */
    virtual ~String();

    /**
     * Get a string pointer to string content based on an indexed
     * offset.  A NULL is returned if the index is outsize of range.
     *
     * @return string content or NULL if invalid index.
     * @param index
     */
    const char *getIndex(size_t index) const;

    /**
     * Get the text of a string.
     *
     * @return string content.
     */
    char *getText(void) const;

    /**
     * Get the value of a string.
     *
     * @return string value as number.
     */
    long getValue(long defvalue = 0l) const;

    /**
     * Get the bool flag of a string.
     *
     * @return boolean value.
     */
    bool getBool(bool defbool = false) const;

    /**
     * Get the assigned length of string.
     *
     * @return string length.
     */
    const size_t getLength(void) const;

    /**
     * Get the allocation size of the string variable.
     *
     * @return allocation size.
     */
    const size_t getSize(void) const;

    /**
     * Return true if string is empty.
     *
     * @return true if string is empty string.
     */
    bool isEmpty(void) const;

    /**
     * Re-allocate buffer space for string.
     *
     * @param size new size to use.
     */
    void resize(size_t size);

    /**
     * Clear the contents of the entire string.
     */
    void clear(void);

    /**
     * Return a character at a known offset.
     *
     * @return character at offset.
     */
    char at(ssize_t offset) const;

    /**
     * Count the number of occurences of a specific string within
     * our string.
     *
     * @return count of instances.
     * @param s string to test.
     * @param offset offset to start from.
     */
    unsigned count(const String &s, size_t offset = 0) const;

    /**
     * Count the number of occurrences of a specific text pattern
     * within our string.
     *
     * @return count of instances.
     * @param s text pattern to find
     * @param offset offset to start from.
     * @param len length of text pattern if specified.
     */
    unsigned count(const char *s, size_t offset = 0, size_t len = 0) const;

    /**
     * Extract a new string as a token from the current string.
     *
     * @return string containing token.
     * @param delim deliminator characters.
     * @param offset offset to start from.
     */
    String token(const char *delim = " \t\n\r", size_t offset = 0);

    /**
     * Find the index to the nth instance of a substring in our string.
     *
     * @return index of found substring.
     * @param s string to search for.
     * @param offset offset to start at.
     * @param instance instance to look for.
     */
    size_t find(const String &s, size_t offset = 0, unsigned instance = 1) const;

    /**
     * Find last occurence of a substring in our string.
     *
     * @return index of last instance found,
     * @param s string to search for.
     * @param offset offset to start from.
     */
    size_t rfind(const String &s, size_t offset = 0) const;

    /**
     * Find the index to the nth instance of text in our string.
     *
     * @return index of found substring.
     * @param s string to search for.
     * @param offset offset to start at.
     * @param len size of string text.
     * @param count instance to look for.
     */
    size_t find(const char *s, size_t offset = 0, size_t len = 0, unsigned count = 1) const;

       /**
     * Find last occurence of a text in our string.
     *
     * @return index of last instance found,
     * @param s string to search for.
     * @param offset offset to start from.
     * @param len size of string to look for.
     */
    size_t rfind(const char *s, size_t offset = 0, size_t len = 0) const;

    /**
     * Trim trailing characters from a string.
     *
     * @param cs list of chars to trim.
     */
    inline void trim(const char *cs)
        {setLength(strtrim(cs, getText(), getLength()));};

    /**
     * Chop leading characters from a string.
     *
     * @param cs list of chars to chop.
     */
    inline void chop(const char *cs)
        {setLength(strchop(cs, getText(), getLength()));};

    /**
     * Strip lead and trailing characters from a string.
     *
     * @param cs list of chars to strip.
     */
    void strip(const char *cs);

    /**
     * Chop n leading characters from a string.
     *
     * @param chars count to chop.
     */
    inline void chop(size_t chars)
        {erase(0, chars);};

    /**
     * Trim n trailing characters from a string.
     *
     * @param count number of bytes to trim.
     */
    void trim(size_t count);

    /**
     * Erase a portion of string.
     *
     * @param start starting index to erase from.
     * @param len number of characters to erase.
     */
    void erase(size_t start, size_t len = npos);

    /**
     * Insert text into a string.
     *
     * @param start starting offset to insert at.
     * @param text text to insert.
     * @param len size of text to insert.
     */
    void insert(size_t start, const char *text, size_t len = 0);

    /**
     * Insert other string into our string.
     *
     * @param start string offset to insert at.
     * @param str string to insert.
     */
    void insert(size_t start, const String &str);

    /**
     * Replace text at a specific position in the string with new
     * text.
     *
     * @param start starting offset to replace at.
     * @param len length of text to remove.
     * @param text text to replace with.
     * @param count size of replacement text.
     */
    void replace(size_t start, size_t len, const char *text, size_t count = 0);

    /**
     * Replace text at a specific position in the string with new
     * string,
     *
     * @param start starting offset to replace at.
     * @param len length of text to remove.
     * @param string reference to replace with.
     */
    void replace(size_t start, size_t len, const String &string);

    /**
     * A more convenient version of find for nth occurences, by
     * putting the instance first.
     *
     * @param instance nth instance to look for.
     * @param text text to look for.
     * @param offset offset to start at.
     * @param len length of text.
     */
    inline size_t find(unsigned instance, const char *text, size_t offset = 0, size_t len = 0) const
        {return find(text, offset, len, instance);};

    /**
     * A more convenient version of find for nth occurences, by
     * putting the instance first.
     *
     * @param instance nth instance to look for.
     * @param string reference to look for.
     * @param offset offset to start at.
     */
    inline size_t find(unsigned instance, const String &string, size_t offset = 0) const
        {return find(string, offset, instance);};

    /**
     * Return a new string that contains a specific substring of the
     * current string.
     *
     * @return new string.
     * @param start starting offset for extracted substring.
     * @param len length of substring.
     */
    inline String substr(size_t start, size_t len) const
        {return String(*this, start, len);};

    /**
     * Return an indexed string based on the index, such as from a
     * find.  If out of range, a NULL string is returned.
     *
     * @return pointer to string data from our string,
     * @param ind index or offset to use.
     */
    inline const char *(index)(size_t ind) const
        {return getIndex(ind);};

    /**
     * Reduce the size of the string allocation to the minimum
     * needed based on the current effective length.
     */
    inline void compact(void)
        {resize(getLength() + 1);};

    /**
     * Old ANSI C++ compatible string pointer extraction.
     *
     * @return string data.
     */
    inline char *c_str(void) const
        {return getText();};

    /**
     * Get our string data through dereference operator.
     *
     * @return string data.
     */
    inline operator char *() const
        {return getText();};

    /**
     * Logical test for string empty.
     *
     * @return true if is empty.
     */
    inline bool operator!(void) const
        {return isEmpty();};

    /**
     * Alternate get text method.
     *
     * @return string data.
     */
    inline char *text(void) const
        {return getText();};

    /**
     * Alternate get text method.
     *
     * @return string data.
     */
    inline char *data(void) const
        {return getText();};

    /**
     * Get length as if null terminated string.
     *
     * @return cstring length.
     */
    inline size_t length(void) const
        {return strlen(getText());};

    /**
     * Get actual length of string data.
     *
     * @return actual size of string.
     */
    inline size_t size(void) const
        {return getLength();};

    /**
     * Get space allocated to hold current string.
     *
     * @return space of memory buffer from heap or local.
     */
    inline size_t capacity(void) const
        {return getSize();};

    /**
     * Return true if string is empty.
     */
    bool empty(void) const
        {return isEmpty();};

    /**
     * Append text to the end of the current string.
     *
     * @param str text to append.
     * @param count size of text to append.
     */
    void append(const char *str, size_t count = 0);

#ifdef  HAVE_SNPRINTF
    /**
     * Append formatted text to the end of the current string.
     *
     * @param size size of text to append.
     * @param format of data to append.
     */
    void append(size_t size, const char *format, ...);
#endif

    /**
     * Append text into the current string.
     *
     * @param str text to append.
     * @param offset offset to overlay.
     * @param count size of text to append.
     */
    void append(const char *str, size_t offset, size_t count);

    /**
     * Add a character to the end of a string.
     *
     * @param c char to add.
     */
    void add(char c);

    /**
     * Append string to the end of the current string.
     *
     * @param str string to append.
     */
    void append(const String &str);

    /**
     * Extract a character by array indexing.
     *
     * @return character code.
     */
    inline const char operator[](unsigned ind) const
        {return at(ind);};

    /**
     * Assign our string for c string.
     */
    inline const char *operator =(const char *str)
        {return set(str);};

    /**
     * Add two strings and return a temporary object.
     */
    friend __EXPORT String operator+(const String &s1, const String &s2);

    friend __EXPORT String operator+(const String &s1, const char *s2);

    friend __EXPORT String operator+(const char *s1, const String &s2);

    friend __EXPORT String operator+(const String &s1, const char c2);

    friend __EXPORT String operator+(const char c1, const String &s2);

    /**
     * Append operator.
     */
    inline String &operator+=(const String &str)
        {append(str); return *this;};

    /**
     * Append operator.
     */
    inline String &operator+=(char c)
        {add(c); return *this;};

    /**
     * Append operator.
     */
    inline String &operator+=(const char *str)
        {append(str); return *this;};

    /**
     * Append operator.
     */
    inline String &operator+=(const std::string &str)
        {append(str.c_str()); return *this;};

    /**
     * Fetch input from a std::istream into the current string
     * variable until either the string variable is filled (based on
     * current length) or the deliminator is read.
     *
     * @param is stream to read.
     * @param str string to save into.
     * @param delim deliminator to use.
     * @param size optional size limitor.
     */
    friend __EXPORT std::istream &getline(std::istream &is, String &str, char delim = '\n', size_t size = 0);

    /**
     * Stream the content of our string variable directly to a C++
     * streaming source.
     */
    friend __EXPORT std::ostream &operator<<(std::ostream &os, const String &str);

    /**
     * Stream input into our variable.
     */
    inline friend std::istream &operator>>(std::istream &is, String &str)
        {return getline(is, str);};

#ifdef  HAVE_SNPRINTF
    /**
     * Print values directly into a string variable.
     *
     * @return character count.
     * @param str object reference to use.
     * @param size of string required.
     * @param format of data.
     */
    friend __EXPORT int strprintf(String &str, size_t size, const char *format, ...);
#endif

    bool operator<(const String &str) const;
    bool operator<(const char *str) const;
    bool operator>(const String &str) const;
    bool operator>(const char *str) const;
    bool operator<=(const String &str) const;
    bool operator<=(const char *str) const;
    bool operator>=(const String &str) const;
    bool operator>=(const char *str) const;
    bool operator==(const String &str) const;
    bool operator==(const char *str) const;
    bool operator!=(const String &str) const;
    bool operator!=(const char *str) const;

#ifdef  HAVE_SNPRINTF

    /**
     * Append operator
     */
    inline String &operator+=(int i)
        {append(16, "%d", i); return *this;};

    inline String &operator+=(unsigned int i)
        {append(16, "%u", i); return *this;};

    inline String &operator+=(long l)
        {append(16, "%l", l); return *this;};

    inline String &operator+=(unsigned long l)
        {append(16, "%ul", l); return *this;};

    inline String &operator+=(float f)
        {append(32, "%f", f); return *this;};

    inline String &operator+=(double d)
        {append(32, "%f", d); return *this;};

    inline String &operator+=(short s)
        {append(8, "%hd", s); return *this;};

    inline String &operator+=(unsigned short s)
        {append(8, "%hu", s); return *this;};


    /**
     * Assignment operator.
     */
    inline String &operator=(int i)
        {set(16, "%d", i); return *this;};

    inline String &operator=(unsigned int i)
        {set(16, "%u", i); return *this;};

    inline String &operator=(long l)
        {set(16, "%l", l); return *this;};

    inline String &operator=(unsigned long l)
        {set(16, "%ul", l); return *this;};

    inline String &operator=(float f)
        {set(32, "%f", f); return *this;};

    inline String &operator=(double d)
        {set(32, "%f", d); return *this;};

    inline String &operator=(short s)
        {set(8, "%hd", s); return *this;};

    inline String &operator=(unsigned short s)
        {set(8, "%hu", s); return *this;};
#endif

    inline String &operator=(const String &original)
        {copy(original); return *this;};

    /**
     * Test if string is contained in our string.
     */
    bool operator*=(const String &str) const;

    /**
     * Test if text is contained in our string.
     */
    bool operator*=(const char *str) const;
};

class __EXPORT SString : public String, protected std::streambuf, public std::ostream
{
protected:
    /**
     * This is the streambuf function that actually outputs the data
     * to the string.  Since all output should be done with the standard
     * ostream operators, this function should never be called directly.
     */
    int overflow(int c);

public:
    /**
     * Create an empty streamable string ready for input.
     */
    SString();

    /**
     * Copy constructor
     */
    SString(const SString &from);

    /**
     * Cancel out the object.
     */
    ~SString();
};

/**
 * The StringObject class is used to derive subclasses that use the
 * String managed memory pool for all space allocations by overriding
 * new and delete operators.  Due to size limits, StringObject should
 * not hold very large objects.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Objects managed in reusable String memory pools
 */
class __EXPORT StringObject
{
public:
    /**
     * Create a new object in string managed space.
     */
    void *operator new(size_t size) NEW_THROWS;

    /**
     * Delete object from string managed space.
     */
    void operator delete(void *obj);
};

#ifdef  CCXX_NAMESPACES
}
#endif

#endif
