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

/**
 * XML "SAX" (stream) parsing support from GNU Common C++.
 * @file ucommon/xml.h
 */

#ifndef _UCOMMON_STRING_H_
#include <ucommon/string.h>
#endif

#ifndef _UCOMMON_PROTOCOLS_H_
#include <ucommon/protocols.h>
#endif

#ifndef _UCOMMON_XML_H_
#define _UCOMMON_XML_H_

NAMESPACE_UCOMMON

/**
 * XML streaming parser.  This class implements a basic XML stream parser
 * that can be used to examine an XML resource thru virtual I/O methods.
 * This class must be derived into one that can implement the physical I/O
 * required to parse actual data.  A mixer class using XMLParser and
 * tcpstream would be one example of this.  This can also be used to
 * parse xml content in memory buffers easily.  This parser is only concerned
 * with well-formedness, and does not perform validation.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT XMLParser
{
private:
    int ecount, dcount;
    enum {TAG, CDATA, COMMENT, DTD, AMP, NONE, END} state;
    char *buffer;
    unsigned bufpos, bufsize;
    __LOCAL bool parseTag(void);
    __LOCAL void putBuffer(char c);
    __LOCAL void clearBuffer(void);

protected:
    /**
     * Create xml parser.
     * @param size of XML data buffer.
     */
    XMLParser(unsigned size = 8192);

    /**
     * Destroy xml parser.
     */
    virtual ~XMLParser();

    /**
     * Virtual to receive embedded comments in XML document being parsed.
     * @param text received.
     * @param size of text received.
     */
    virtual void comment(caddr_t text, size_t size);

    /**
     * Virtual to receive character text extracted from the document.
     * @param text received.
     * @param size of text received.
     */
    virtual void characters(caddr_t text, size_t size);

    /**
     * Notify start of document event.
     */
    virtual void startDocument(void);

    /**
     * Notify end of document event.
     */
    virtual void endDocument(void);

    /**
     * Notify start of an element in the document.
     * @param name of element found.
     * @param attr list of attributes extracted.
     */
    virtual void startElement(caddr_t name, caddr_t *attr) = 0;

    /**
     * Notify end of an element in the document.
     * @param name of element ending.
     */
    virtual void endElement(caddr_t name) = 0;

    /**
     * Parse a chunk of data and return parser completion flag.  This is
     * used to externally drive data into the XML parser.  The return
     * status can be used to determine when a document has been fully
     * parsed.  This can be called multiple times to push stream data
     * into the parser.
     * @param address of data to parse.
     * @param size of data to parse.
     */
    bool partial(const char *address, size_t size);

    /**
     * Parse a stream buffer and return parser document completion flag.
     * This is used to scan a stream buffer for a complete XML document.
     * The stream is scanned until the document is complete or EOF.
     * Multiple XML document instances can be scanned from a continues
     * XML streaming source.
     * @param stream buffer to parse.
     * @return true if parse complete, false if invalid or EOF.
     */
    bool parse(CharacterProtocol& stream);

    /**
     * Parse a file buffer and return parser document completion flag.
     * This is used to scan a file buffer for a complete XML document.
     * The file is scanned until the document is complete or EOF.
     * Multiple XML document instances can be scanned from a continues
     * XML streaming source.
     * @param file buffer to parse.
     * @return true if parse complete, false if invalid or EOF.
     */
    bool parse(FILE *file);

    /**
     * End of document check.
     * @return true if end of document.
     */
    bool end(void)
        {return state == END;};
};

END_NAMESPACE

#endif
