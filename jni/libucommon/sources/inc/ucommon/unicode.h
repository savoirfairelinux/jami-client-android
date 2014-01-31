// Copyright (C) 2009-2010 David Sugar, Tycho Softworks.
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

/**
 * Basic UCommon Unicode support.
 * This includes computing unicode transcoding and supporting a
 * UTF8-aware string class (UString).  We may add support for a wchar_t
 * aware string class as well, as some external api libraries may require
 * ucs-2 or 4 encoded strings.
 * @file ucommon/unicode.h
 */

/**
 * An example of some unicode-utf8 transcoding.
 * @example unicode.cpp
 */

#ifndef _UCOMMON_UNICODE_H_
#define _UCOMMON_UNICODE_H_

#ifndef _UCOMMON_STRING_H_
#include <ucommon/string.h>
#endif

NAMESPACE_UCOMMON

/**
 * 32 bit unicode character code.  We may extract this from a ucs2 or utf8
 * string.
 */
typedef int32_t ucs4_t;

/**
 * 16 bit unicode character code.  Java and some api's like these.
 */
typedef int16_t ucs2_t;

/**
 * Resolves issues where wchar_t is not defined.
 */
typedef void *unicode_t;

/**
 * A core class of ut8 encoded string functions.  This is a foundation for
 * all utf8 string processing.
 * @author David Sugar
 */
class __EXPORT utf8
{
public:
    /**
     * Size of "unicode_t" character codes, may not be ucs4_t size.
     */
    static const unsigned ucsize;

    /**
     * A convenient NULL pointer value.
     */
    static const char *nil;

    /**
     * Compute character size of utf8 string codepoint.
     * @param codepoint in string.
     * @return size of codepoint as utf8 encoded data, 0 if invalid.
     */
    static unsigned size(const char *codepoint);

    /**
     * Count ut8 encoded ucs4 codepoints in string.
     * @param string of utf8 data.
     * @return codepount count, 0 if empty or invalid.
     */
    static size_t count(const char *string);

    /**
     * Get codepoint offset in a string.
     * @param string of utf8 data.
     * @param position of codepoint in string, negative offsets are from tail.
     * @return offset of codepoint or NULL if invalid.
     */
    static char *offset(char *string, ssize_t position);

    /**
     * Convert a utf8 encoded codepoint to a ucs4 character value.
     * @param encoded utf8 codepoint.
     * @return ucs4 string or 0 if invalid.
     */
    static ucs4_t codepoint(const char *encoded);

    /**
     * How many chars requires to encode a given wchar string.
     * @param string of ucs4 data.
     * @return number of chars required to encode given string.
     */
    static size_t chars(const unicode_t string);

    /**
     * How many chars requires to encode a given unicode character.
     * @param character to encode.
     * @return number of chars required to encode given character.
     */
    static size_t chars(ucs4_t character);

    /**
     * Convert a unicode string into utf8.
     * @param string of unicode data to pack
     * @param buffer of character protocol to put data into.
     * @return number of code points converted.
     */
    static size_t unpack(const unicode_t string, CharacterProtocol& buffer);

    /**
     * Convert a utf8 string into a unicode data buffer.
     * @param unicode data buffer.
     * @param buffer of character protocol to pack from.
     * @param size of unicode data buffer in codepoints.
     * @return number of code points converted.
     */
    static size_t pack(unicode_t unicode, CharacterProtocol& buffer, size_t size);

    /**
     * Dup a utf8 string into a ucs4_t string.
     */
    static ucs4_t *udup(const char *string);

    /**
     * Dup a utf8 string into a ucs2_t representation.
     */
    static ucs2_t *wdup(const char *string);

    /**
     * Find first occurance of character in string.
     * @param string to search in.
     * @param character code to search for.
     * @param start offset in string in codepoints.
     * @return pointer to first instance or NULL if not found.
     */
    static const char *find(const char *string, ucs4_t character, size_t start = 0);

    /**
     * Find last occurrence of character in string.
     * @param string to search in.
     * @param character code to search for.
     * @param end offset to start from in codepoints.
     * @return pointer to last instance or NULL if not found.
     */
    static const char *rfind(const char *string, ucs4_t character, size_t end = (size_t)-1l);

    /**
     * Count occurrences of a unicode character in string.
     * @param string to search in.
     * @param character code to search for.
     * @return count of occurrences.
     */
    static unsigned ccount(const char *string, ucs4_t character);

    /**
     * Get a unicode character from a character protocol.
     * @param buffer of character protocol to read from.
     * @return unicode character or EOF error.
     */
    static ucs4_t get(CharacterProtocol& buffer);

    /**
     * Push a unicode character to a character protocol.
     * @param character to push to file.
     * @param buffer of character protocol to push character to.
     * @return unicode character or EOF on error.
     */
    static ucs4_t put(ucs4_t character, CharacterProtocol& buffer);
};

/**
 * A copy-on-write utf8 string class that operates by reference count.  This
 * is derived from the classic uCommon String class by adding operations that
 * are utf8 encoding aware.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT UString : public String, public utf8
{
protected:
    /**
     * Create a new empty utf8 aware string object.
     */
    UString();

    /**
     * Create an empty string with a buffer pre-allocated to a specified size.
     * @param size of buffer to allocate.
     */
    UString(strsize_t size);

    /**
     * Create a utf8 aware string for a null terminated unicode string.
     * @param text of ucs4 encoded data.
     */
    UString(const unicode_t text);

    /**
     * Create a string from null terminated text up to a maximum specified
     * size.
     * @param text to use for string.
     * @param size limit of new string.
     */
    UString(const char *text, strsize_t size);

    /**
     * Create a string for a substring.  The end of the substring is a
     * pointer within the substring itself.
     * @param text to use for string.
     * @param end of text in substring.
     */
    UString(const unicode_t *text, const unicode_t *end);

    /**
     * Construct a copy of a string object.  Our copy inherits the same
     * reference counted instance of cstring as in the original.
     * @param existing string to copy from.
     */
    UString(const UString& existing);

    /**
     * Destroy string.  De-reference cstring.  If last reference to cstring,
     * then also remove cstring from heap.
     */
    virtual ~UString();

    /**
     * Get a new string object as a substring of the current object.
     * @param codepoint offset of substring.
     * @param size of substring in codepoints or 0 if to end.
     * @return string object holding substring.
     */
    UString get(strsize_t codepoint, strsize_t size = 0) const;

    /**
     * Extract a unicode byte sequence from utf8 object.
     * @param unicode data buffer.
     * @param size of data buffer.
     * @return codepoints copied.
     */
    size_t get(unicode_t unicode, size_t size) const;

    /**
     * Set a utf8 encoded string based on unicode data.
     * @param unicode text to set.
     */
    void set(const unicode_t unicode);

    /**
     * Add (append) unicode to a utf8 encoded string.
     * @param unicode text to add.
     */
    void add(const unicode_t unicode);

    /**
     * Return unicode character found at a specific codepoint in the string.
     * @param position of codepoint in string, negative values computed from end.
     * @return character code at specified position in string.
     */
    ucs4_t at(int position) const;

    /**
     * Extract a unicode byte sequence from utf8 object.
     * @param unicode data buffer.
     * @param size of data buffer.
     * @return codepoints copied.
     */
    inline size_t operator()(unicode_t unicode, size_t size) const
        {return get(unicode, size);};

    /**
     * Get a new substring through object expression.
     * @param codepoint offset of substring.
     * @param size of substring or 0 if to end.
     * @return string object holding substring.
     */
    UString operator()(int codepoint, strsize_t size) const;

    /**
     * Convenience method for left of string.
     * @param size of substring to gather in codepoints.
     * @return string object holding substring.
     */
    inline UString left(strsize_t size) const
        {return operator()(0, size);}

    /**
     * Convenience method for right of string.
     * @param offset of substring from right in codepoints.
     * @return string object holding substring.
     */
    inline UString right(strsize_t offset) const
        {return operator()(-((int)offset), 0);}

    /**
     * Convenience method for substring extraction.
     * @param offset into string.
     * @param size of string to return.
     * @return string object holding substring.
     */
    inline UString copy(strsize_t offset, strsize_t size) const
        {return operator()((int)offset, size);}

    /**
     * Cut (remove) text from string using codepoint offsets.
     * @param offset to start of text field to remove.
     * @param size of text field to remove or 0 to remove to end of string.
     */
    void cut(strsize_t offset, strsize_t size = 0);

    /**
     * Insert (paste) text into string using codepoint offsets.
     * @param offset to start paste.
     * @param text to paste.
     * @param size of text to paste.
     */
    void paste(strsize_t offset, const char *text, strsize_t size = 0);

    /**
     * Reference a string in the object by codepoint offset.  Positive
     * offsets are from the start of the string, negative from the
     * end.
     * @param offset to string position.
     * @return pointer to string data or NULL if invalid offset.
     */
    const char *operator()(int offset) const;

    /**
     * Reference a unicode character in string object by array offset.
     * @param position of codepoint offset to character.
     * @return character value at offset.
     */
    inline ucs4_t operator[](int position) const
        {return UString::at(position);};

    /**
     * Count codepoints in current string.
     * @return count of codepoints.
     */
    inline strsize_t count(void) const
        {return utf8::count(str->text);}

    /**
     * Count occurrences of a unicode character in string.
     * @param character code to search for.
     * @return count of occurrences.
     */
    unsigned ccount(ucs4_t character) const;

    /**
     * Find first occurrence of character in string.
     * @param character code to search for.
     * @param start offset in string in codepoints.
     * @return pointer to first instance or NULL if not found.
     */
    const char *find(ucs4_t character, strsize_t start = 0) const;

    /**
     * Find last occurrence of character in string.
     * @param character code to search for.
     * @param end offset to start from in codepoints.
     * @return pointer to last instance or NULL if not found.
     */
    const char *rfind(ucs4_t character, strsize_t end = npos) const;
};

/**
 * Pointer to utf8 encoded character data.  This is a kind of "char *" for
 * utf8 text.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT utf8_pointer
{
protected:
    uint8_t *text;

public:
    /**
     * Create a utf8 pointer set to NULL.
     */
    utf8_pointer();

    /**
     * Create a utf8 pointer for an existing char pointer.
     * @param string pointer to use.
     */
    utf8_pointer(const char *string);

    /**
     * Create a utf8 pointer as a copy of existing utf8 pointer.
     * @param copy of object to use.
     */
    utf8_pointer(const utf8_pointer& copy);

    /**
     * Iterative increment of a utf8 pointer to prior codepoint.
     * @return object incremented.
     */
    utf8_pointer& operator ++();

    /**
     * Iterative decrement of a utf8 pointer to next codepoint.
     * @return object decremented.
     */
    utf8_pointer& operator --();

    /**
     * Adjust utf8 pointer by specified codepoints forward.
     * @param offset to increment by.
     * @return object incremented.
     */
    utf8_pointer& operator +=(long offset);

    /**
     * Adjust utf8 pointer by specified codepoints backward.
     * @param offset to decrement by.
     * @return object decremented.
     */
    utf8_pointer& operator -=(long offset);

    /**
     * Get new utf8 string after adding a codepoint offset.
     * @param offset to add.
     * @return new utf8 pointer pointing to specified offset.
     */
    utf8_pointer operator+(long offset) const;

    /**
     * Get new utf8 string after subtracting a codepoint offset.
     * @param offset to subtract.
     * @return new utf8 pointer pointing to specified offset.
     */
    utf8_pointer operator-(long offset) const;

    /**
     * Check if text is valid pointer.
     * @return true if not NULL.
     */
    inline operator bool() const
        {return text != NULL;};

    /**
     * Check if text is an invalid pointer.
     * @return false if not NULL.
     */
    inline bool operator!() const
        {return text == NULL;};

    /**
     * Extract a unicode character from a specified codepoint.
     * @param codepoint offset to extract character from.
     * @return unicode character or 0.
     */
    ucs4_t operator[](long codepoint) const;

    /**
     * Assign a utf8 string to point to.
     * @param string to point to.
     * @return current object after set to string.
     */
    utf8_pointer& operator=(const char *string);

    /**
     * Iterative increment of a utf8 pointer to next codepoint.
     */
    void inc(void);

    /**
     * Iterative decrement of a utf8 pointer to prior codepoint.
     */
    void dec(void);

    /**
     * check if pointer equals another string.
     * @param string to check.
     * @return true if same memory address.
     */
    inline bool operator==(const char *string) const
        {return (const char *)text == string;};

    /**
     * check if pointer does not equal another string.
     * @param string to check.
     * @return false if same memory address.
     */
    inline bool operator!=(const char *string) const
        {return (const char *)text != string;};

    /**
     * Get unicode character pointed to by pointer.
     * @return unicode character we are pointing to.
     */
    inline  ucs4_t operator*() const
        {return utf8::codepoint((const char *)text);};

    /**
     * Get c string we point to.
     * @return string we point to.
     */
    inline char *c_str(void) const
        {return (char *)text;};

    /**
     * Convert utf8 pointer to a generic string pointer.
     * @return generic string pointer.
     */
    inline operator char*() const
        {return (char *)text;};

    /**
     * Get length of null terminated utf8 string in codepoints.
     * @return codepoint length of string.
     */
    inline size_t len(void) const
        {return utf8::count((const char *)text);};
};

inline ucs4_t *strudup(const char *string)
    {return utf8::udup(string);}

inline ucs2_t *strwdup(const char *string)
    {return utf8::wdup(string);}

__EXPORT unicode_t unidup(const char *string);

template<>
inline void dupfree<ucs2_t*>(ucs2_t *string)
    {::free(string);}

template<>
inline void dupfree<ucs4_t*>(ucs4_t *string)
    {::free(string);}

template<>
inline void dupfree<unicode_t>(unicode_t string)
    {::free(string);}

/**
 * Convenience type for utf8 encoded strings.
 */
typedef UString ustring_t;

/**
 * Convenience type for utf8_pointer strings.
 */
typedef utf8_pointer utf8_t;

END_NAMESPACE

#endif
