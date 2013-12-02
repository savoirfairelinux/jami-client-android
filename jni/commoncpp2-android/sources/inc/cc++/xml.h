// Copyright (C) 2001-2005 Open Source Telecom Corporation.
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
 * @file xml.h
 * @short XML streams abstraction and RPC services.
 **/

#ifndef CCXX_XML_H_
#define CCXX_XML_H_

#ifndef CCXX_MISSING_H_
#include <cc++/missing.h>
#endif

#ifndef CCXX_THREAD_H_
#include <cc++/thread.h>
#endif

#ifndef CCXX_SLOG_H_
#include <cc++/slog.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

/**
 * This class impliments a basic XML stream parser that can be used to
 * examine an XML resource thru virtual I/O methods.  This class must
 * be derived into one that can impliment the physical I/O required to
 * parse actual data.  A mixer class using XMLStream and URLStream would
 * seem a likely combination for this purpose.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short XML Stream Parser (SAX)
 */
class __EXPORT XMLStream
{
private:
    int ecount, dcount;
    enum { TAG, CDATA, COMMENT, DTD, AMP, NONE} state;
    char dbuf[8192];
    unsigned dp;
    bool parseChunk(const char *chunk, size_t len);
    void parseInit(void);
    bool parseTag(void);
    void putData(char c);
    void clrData(void);

protected:
    virtual ~XMLStream();

public:
    /**
     * May perform an open operation on behalf of a parsed resource.
     * In some cases, the parser may be merged with a class that
     * already has performed some kind of open, and this method can
     * then be ignored.
     *
     * @return true if open is successful.
     * @param resource passed to Parse methods.
     */
    virtual bool open(const char *resource);

    /**
     * May perform a close operation of an i/o source when the parser
     * has completed operation.
     */
    virtual void close(void);

    /**
     * Get error logging level.
     *
     * @return error logging level.
     */
    virtual Slog::Level getLogging(void);

    /**
     * Virtual to receive embedded comments in an XML document being
     * parsed.
     *
     * @param text text comment extracted.
     * @param len length of comment.
     */
    virtual void comment(const unsigned char *text, size_t len);

    /**
     * Read method to aquire data for the parser.
     *
     * @return number of bytes actually read.
     * @param buffer to read data into.
     * @param len number of bytes to read.
     */
    virtual int read(unsigned char *buffer, size_t len) = 0;

    /**
     * Virtual to receive character text extracted from the document
     * in the current element.
     *
     * @param text received.
     * @param len length of text received.
     */
    virtual void characters(const unsigned char *text, size_t len) = 0;

    /**
     * Identify start of document event.
     */
    virtual void startDocument(void);

    /**
     * Identify end of document event.
     */
    virtual void endDocument(void);

    /**
     * Identify start of an element in the document.
     *
     * @param name of element found.
     * @param attr list of attributes extracted.
     */
    virtual void startElement(const unsigned char *name, const unsigned char **attr) = 0;

    /**
     * Identify end of an element in the document.
     *
     * @param name of element found.
     */
    virtual void endElement(const unsigned char *name) = 0;

    /**
     * Parse a resource as a stream thru the virtual read method.
     *
     * @return true if well formed document has been fully parsed.
     * @param resource optional name of resource.
     */
    bool parse(const char *resource = NULL);
};

/**
 * This class impliments a core XMLRPC service without the underlying
 * transports.  It is meant to create and parse XMLRPC messages.  To
 * use for a fit purpose, one might combine it with URLStream, although
 * this implimentation makes no requirement for http based transport.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short XML-RPC service building class
 */
class __EXPORT XMLRPC : public XMLStream
{
private:
#ifdef HAVE_SSTREAM
    std::stringstream strBuf;
#else
    char *buffer;
    std::strstream *oldStrBuf;
    size_t bufSize;
#endif
    bool structFlag;
    bool reply, fault;
    unsigned array;

protected:
    /**
     * Used in a derived transport class to deliver the XMLRPC encoded
     * request and return true if successful.  The Parse method can
     * then be used to decode the reply.
     *
     * @return true if successful.
     * @param resource to send to (such as url).
     * @param msg well formed XMLRPC request message.
     */
    virtual bool post(const char *resource, const char *msg) = 0;

    /**
     * Start member struct.
     */
    void begStruct(void);

public:
    /**
     * Construct XMLRPC workspace.
     *
     * @param bufferSize size of buffer when using old C++
     * strstreams. When the newer stringstream (\<sstream\>) is
     * available, this parameter is silently ignored.
     */
    XMLRPC(size_t bufferSize = 512);

    /**
     * Destroy XMLRPC object.
     */
    virtual ~XMLRPC();

    /**
     * Create an array.
     */
    void begArray(void);

    /**
     * end an array.
     */
    void endArray(void);

    /**
     * Create XMLRPC "method" call in buffer.
     *
     * @param method name of method being called.
     */
    void invoke(const char *method);

    /**
     * Create XMLRPC "reply" to a method call.
     *
     * @param fault set true for fault message.
     */
    void response(bool fault);

    /**
     * Add bool param to XMLRPC request.
     *
     * @param value to add.
     */
    void addParam(bool value);

    /**
     * Add bool member to a XMLRPC struct.
     *
     * @param name of member.
     * @param value of member.
     */
    void addMember(const char *name, bool value);

    /**
     * Add an integer paramater to XMLRPC request.
     *
     * @param value to add.
     */
    void addParam(long value);

    /**
     * Add an integer member to XMLRPC struct.
     *
     * @param name of member.
     * @param value of member.
     */
    void addMember(const char *name, long value);

    /**
     * Add a string paramater to XMLRPC request.
     *
     * @param string to add.
     */
    void addParam(const char *string);

    /**
     * Add a string member to XMLRPC struct.
     *
     * @param name of member.
     * @param value of member.
     */
    void addMember(const char *name, const char *value);

    /**
     * Clear a struct.
     */
    void endStruct(void);

    /**
     * Complete buffer and send well formed XMLRPC request thru post.
     *
     * @return true if successful.
     * @param resource to send to.
     */
    bool send(const char *resource);
};

//#else
//#error "XML support has been selected, but libxml could not be found"
//#endif // ifdef HAVE_XML

//#else
//#error "XML support is not available."
//#endif // ifdef COMMON_XML_PARSING

#ifdef  CCXX_NAMESPACES
}
#endif

#endif
/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
