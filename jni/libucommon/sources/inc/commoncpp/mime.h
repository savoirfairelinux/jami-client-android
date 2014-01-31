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
 * @file mime.h
 * @short MIME document abstractions.
 **/

#ifndef COMMONCPP_MIME_H_
#define COMMONCPP_MIME_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_SOCKET_H_
#include <commoncpp/socket.h>
#endif

NAMESPACE_COMMONCPP

class MIMEMultipart;
class MIMEItemPart;

/**
 * A container class for multi-part MIME document objects which can
 * be streamed to a std::ostream destination.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short container for streamable multi-part MIME documents.
 */
class __EXPORT MIMEMultipart
{
protected:
    friend class MIMEItemPart;
    char boundry[8];
    char mtype[80];
    char *header[16];
    MIMEItemPart *first, *last;

    virtual ~MIMEMultipart();

public:
    /**
     * Contruct a multi-part document, and describe it's type.
     *
     * @param document (content) type.
     */
    MIMEMultipart(const char *document);

    /**
     * Stream the headers of the multi-part document.  The headers
     * of individual entities are streamed as part of the body.
     *
     * @param output to stream document header into.
     */
    virtual void head(std::ostream *output);

    /**
     * Stream the "body" of the multi-part document.  This involves
     * streaming the headers and body of each document part.
     *
     * @param output to stream document body into.
     */
    virtual void body(std::ostream *output);

    /**
     * Get a string array of the headers to use.  This is used to
     * assist URLStream::post.
     *
     * @return array of headers.
     */
    char **getHeaders(void)
        {return header;};
};

/**
 * The Multipart form is a MIME multipart document specific for the
 * construction and delivery of form data to a web server through a
 * post method.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short deliver form results as multipart document.
 */
class __EXPORT MIMEMultipartForm : public MIMEMultipart
{
protected:
    virtual ~MIMEMultipartForm();

public:
    /**
     * Construct a form result.  This is a MIMEMultipart of type
     * multipart/form-data.
     */
    MIMEMultipartForm();
};

/**
 * This is used to attach an item part to a MIME multipart document
 * that is being streamed.  The base item part class is used by all
 * derived items.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short item or part of a multi-part object.
 */
class __EXPORT MIMEItemPart
{
protected:
    friend class MIMEMultipart;

    MIMEMultipart *base;
    MIMEItemPart *next;
    const char *ctype;

    /**
     * Stream the header(s) for the current document part.
     *
     * @param output to stream header into.
     */
    virtual void head(std::ostream *output);

    /**
     * Stream the content of this document part.
     *
     * @param output to stream document body into.
     */
    virtual void body(std::ostream *output) = 0;

    /**
     * Construct and attach a document part to a multipart document.
     *
     * @param top multipart document to attach to.
     * @param ct Content-Type to use.
     */
    MIMEItemPart(MIMEMultipart *top, const char *ct);

    virtual ~MIMEItemPart();
};

/**
 * This is a document part type for use in submitting multipart form
 * data to a web server.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short multipart document part for web form data field.
 */
class __EXPORT MIMEFormData : public MIMEItemPart
{
protected:
    const char *content;
    const char *name;

    virtual ~MIMEFormData();

public:
    /**
     * Stream header, Content-Disposition form-data.
     *
     * @param output stream to send header to.
     */
    void head(std::ostream *output);

    /**
     * Stream content (value) of this form data field.
     *
     * @param output stream to send body to.
     */
    void body(std::ostream *output);

    /**
     * Construct form data field part of multipart form.
     *
     * @param top multipart form this is part of
     * @param name of form data field
     * @param content of form data field
     */
    MIMEFormData(MIMEMultipartForm *top, const char *name, const char *content);
};

END_NAMESPACE

#endif
/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
