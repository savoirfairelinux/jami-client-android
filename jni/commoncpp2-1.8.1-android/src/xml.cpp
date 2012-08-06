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

#include <cc++/config.h>
#ifdef  CCXX_WITHOUT_EXTRAS
#include <cc++/export.h>
#endif
#include <cc++/file.h>
#include <cc++/thread.h>
#include <cc++/exception.h>
#ifndef CCXX_WITHOUT_EXTRAS
#include <cc++/export.h>
#endif
#include <cc++/xml.h>
#ifndef WIN32
#include <syslog.h>
#endif

#include <cstdlib>

// very ugly, but saves a lot of #ifdefs. To understand this, look at
// the private members of XMLRPC.
#ifndef HAVE_SSTREAM
#define strBuf (*oldStrBuf)
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#ifdef HAVE_SSTREAM
using std::stringstream;
#else
using std::strstream;
#endif
using std::streambuf;
using std::ofstream;
using std::ostream;
using std::clog;
using std::endl;
using std::ends;
using std::ios;
#endif

static bool isElement(char c)
{
    return isalnum(c) || c == ':' || c == '-' || c == '.' || c == '_';
}

void XMLStream::putData(char c)
{
    dbuf[dp++] = c;
    if(dp >= sizeof(dbuf)) {
        if(ecount)
            characters((unsigned char *)dbuf, dp);
        dp = 0;
    }
}

void XMLStream::clrData(void)
{
    if(dp && ecount)
        characters((unsigned char *)dbuf, dp);
    dp = 0;
}

void XMLStream::parseInit(void)
{
    state = NONE;
    dp = 0;
    ecount = dcount = 0;
}

bool XMLStream::parseTag(void)
{
    size_t len = dp;
    const char *data = dbuf;
    bool end = false;
    const unsigned char *attrib[128];
    unsigned attr = 0;
    char *ep;

    if(*data == '/') {
        while(--len) {
            if(!isElement(*(++data)))
                break;
        }
        if(len)
            return false;

        dbuf[dp] = 0;
        endElement((const unsigned char *)(dbuf + 1));
        dp = 0;
        --ecount;
        if(ecount < 0)
            return false;
        if(!ecount)
            endDocument();
    }
    else if(*data == '!') {
        dp = 0;
        return true; // dtd
    }
    else if(*data == '?') {
        if(!strnicmp(data, "?xml version=\"", 14)) {
            // version info
        }
        dp = 0;
    }
    else if(!isElement(*data))
        return false;
    else {
        end = false;
        if(dbuf[dp - 1] == '/') {
            --dp;
            end = true;
        }
        len = 0;
        data = dbuf;
        while(len < dp) {
            if(!isElement(*data))
                break;
            ++len;
            ++data;
        }
        if(len == dp) {
            if(!ecount)
                startDocument();
            ++ecount;
            attrib[0] = attrib[1] = NULL;
            dbuf[dp] = 0;
            startElement((const unsigned char *)dbuf, attrib);
            if(end) {
ending:
                --ecount;
                endElement((const unsigned char *)dbuf);
                if(!ecount)
                    endDocument();
            }
            dp = 0;
            return true;
        }
        if(!ecount)
            startDocument();
        ++ecount;

        // attributes, name is between data and len

        for(;;) {
            while(!isElement(dbuf[len]) && len < dp) {
                if(!isspace(dbuf[len]))
                    return false;
                dbuf[len++] = 0;
            }

            if(len == dp)
                break;

            attrib[attr++] = (const unsigned char *)(dbuf + len);
            while(len < dp && isElement(dbuf[len]))
                ++len;

            if(len == dp)
                return false;

            if(dbuf[len] != '=')
                return false;

            dbuf[len++] = 0;
            if(len == dp) {
                attrib[attr++] = (const unsigned char *)"";
                break;
            }

            if(isspace(dbuf[len])) {
                attrib[attr++] = (const unsigned char *)"";
                continue;
            }
            if(dbuf[len] == '\'' || dbuf[len] == '\"') {
                ep = strchr(dbuf + len + 1, dbuf[len]);
                if(!ep)
                    return false;
                attrib[attr++] = (const unsigned char *)dbuf + len + 1;
                *(ep++) = 0;
                len = ep - dbuf;
                continue;
            }
            if(!isElement(dbuf[len]))
                return false;
            attrib[attr++] = (const unsigned char *)dbuf;
            while(isElement(dbuf[len]) && len < dp)
                ++len;
            if(len == dp) {
                dbuf[len] = 0;
                break;
            }
        }

        attrib[attr++] = NULL;
        attrib[attr++] = NULL;
        startElement((const unsigned char *)dbuf, attrib);
        if(end)
            goto ending;
        dp = 0;
        return true;
    }
    return true;
}

bool XMLStream::parseChunk(const char *data, size_t len)
{
    unsigned char cp;
    while(len--) {
        switch(state) {
        case AMP:
            if((!dp && *data == '#') || isElement(*data)) {
                dbuf[dp++] = *data;
                break;
            }
            if(*data != ';')
                return false;
            dbuf[dp] = 0;
            if(dbuf[0] == '#')
                cp = atoi(dbuf + 1);
            else if(!stricmp(dbuf, "amp"))
                cp = '&';
            else if(!stricmp(dbuf, "lt"))
                cp = '<';
            else if(!stricmp(dbuf, "gt"))
                cp = '>';
            else if(!stricmp(dbuf, "apos"))
                cp = '`';
            else if(!stricmp(dbuf, "quot"))
                cp = '\"';
            else
                return false;
            characters(&cp, 1);
            dp = 0;
            state = NONE;
            break;
        case TAG:
            if(*data == '>') {
                state = NONE;
                if(!parseTag())
                    return false;
            }
            else if(*data == '[' && dp == 7 && !strncmp(dbuf, "![CDATA", 7)) {
                state = CDATA;
            }
            else if(*data == '-' && dp == 2 && !strncmp(dbuf, "!-", 2)) {
                state = COMMENT;
                dp = 0;
            }
            else if(*data == '[' && !strncmp(dbuf, "!DOCTYPE ", 9)) {
                state = DTD;
                dp = 0;
            }
            else
                putData(*data);
            break;
        case COMMENT:
            if(*data == '>' && dp >= 2 && !strncmp(&dbuf[dp - 2], "--", 2)) {
                dp -= 2;
                if(dp)
                    comment((unsigned char *)dbuf, dp);
                dp = 0;
                state = NONE;
            }
            else {
                dbuf[dp++] = *data;
                if(dp == sizeof(dbuf)) {
                    comment((unsigned char *)dbuf, dp);
                    dp = 0;
                }
            }
            break;
        case CDATA:
            putData(*data);
            if(dp > 2)
                if(!strcmp(&dbuf[dp - 3], "]]>")) {
                    dp -= 3;
                    state = NONE;
                    clrData();
                }
            break;
        case DTD:
            if(*data == '<')
                ++dcount;
            else if(*data == '>' && dcount)
                --dcount;
            else if(*data == '>')
                state = NONE;
            break;
        case NONE:
            if(*data == '<') {
                clrData();
                state = TAG;
            }
            else if(ecount && *data == '&') {
                clrData();
                state = AMP;
            }
            else if(ecount)
                putData(*data);
        }
        ++data;
    }
    return true;
}

bool XMLStream::parse(const char *resource)
{
    bool ret = false;
    char buffer[1024];
    int res;

    if(resource)
        if(!open(resource))
            return false;

    parseInit();

    while((res = read((unsigned char *)buffer, 1024)))
        ret = parseChunk(buffer, res);
    return ret;
}

XMLRPC::XMLRPC(size_t bufferSize) :
XMLStream()
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    buffer = new char[bufferSize];
    oldStrBuf = NULL;
    bufSize = bufferSize;
#endif
}

XMLRPC::~XMLRPC()
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(buffer)
        delete[] buffer;
    if(oldStrBuf)
        delete oldStrBuf;
#endif
    close();
}

void XMLRPC::invoke(const char *member)
{
#ifdef HAVE_SSTREAM
    strBuf.str() = "";
#else
    buffer[0] = 0;
    oldStrBuf = new strstream(buffer,bufSize);
#endif

    structFlag = reply = fault = false;
    array = 0;

    strBuf << "<?xml version=\"1.0\"?>" << endl;
    strBuf << "<methodCall>" << endl;
    strBuf << "<methodName>" << member << "</methodName>" << endl;
    strBuf << "<params>" << endl;
}

void XMLRPC::response(bool f)
{
    reply = true;
    structFlag = false;
    fault = f;
    array = 0;

#ifdef HAVE_SSTREAM
    // nothing
#else
    buffer[0] = 0;
    oldStrBuf = new strstream(buffer,bufSize);
#endif

    strBuf << "<?xml version=\"1.0\"?>" << endl;
    strBuf << "<methodResponse>" << endl;
    if(fault)
        strBuf << "<fault>" << endl;
    else
        strBuf << "<params>" << endl;
}

void XMLRPC::addMember(const char *name, long value)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return;
#endif

    begStruct();

    strBuf << "<member><name>" << name << "</name>" << endl;
    strBuf << "<value><i4>" << value << "</i4></value></member>" << endl;
}

void XMLRPC::addMember(const char *name, const char *value)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return;
#endif

    begStruct();

    strBuf << "<member><name>" << name << "</name>" << endl;
    strBuf << "<value><string>" << value << "</string></value></member>" << endl;
}


void XMLRPC::addMember(const char *name, bool value)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return;
#endif

    begStruct();

    strBuf << "<member><name>" << name << "</name>" << endl;
    strBuf << "<value><boolean>";
    if(value)
        strBuf << "1";
    else
        strBuf << "0";

    strBuf << "</boolean></value></member>" << endl;
}

void XMLRPC::addParam(bool value)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return;
#endif

    endStruct();

    if(!fault && !array)
        strBuf << "<param>";

    strBuf << "<value><boolean>";
    if(value)
        strBuf << "1";
    else
        strBuf << "0";
    strBuf << "</boolean></value>";

    if(!fault && !array)
        strBuf << "</param>";

    strBuf << endl;
}

void XMLRPC::addParam(long value)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return;
#endif

    endStruct();

    if(!fault && !array)
        strBuf << "<param>";

    strBuf << "<value><i4>" << value << "</i4></value>";

    if(!fault && !array)
        strBuf << "</param>";

    strBuf << endl;
}

void XMLRPC::addParam(const char *value)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return;
#endif

    endStruct();

    if(!fault && !array)
        strBuf << "<param>" << endl;

    strBuf << "<value><string>" << value << "</string></value>";

    if(!fault && !array)
        strBuf << "</param>";

    strBuf << endl;
}

void XMLRPC::begArray(void)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return;
#endif

    if(fault) //do not include arrays in fault responses.
        return;

    if(!array)
        strBuf << "<param>";
    array++;
    strBuf << "<array><data>" << endl;
}

void XMLRPC::endArray(void)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return;
#endif

    if(!array)
        return;

    strBuf << "</data></array>";

    if(!--array)
        strBuf << "</param>";

    strBuf << endl;
}

void XMLRPC::begStruct(void)
{
    if(structFlag)
        return;

    structFlag = true;

    if(!fault && !array)
        strBuf << "<param>";

    strBuf << "<value><struct>" << endl;
}

void XMLRPC::endStruct(void)
{
    if(!structFlag)
        return;

    strBuf << "</struct></value>";

    if(!fault && !array)
        strBuf << "</param>";
    strBuf << endl;
    structFlag = false;
}

bool XMLRPC::send(const char *resource)
{
#ifdef HAVE_SSTREAM
    // nothing
#else
    if(!oldStrBuf)
        return false;
#endif

    endStruct();
    while(array) {
        strBuf << "</data></array>" << endl;
        --array;
    }
    if(!fault)
        strBuf << "</params>" << endl;
    else
        strBuf << "</fault>" << endl;

    if(reply)
        strBuf << "</methodResponse>" << endl << ends;
    else
        strBuf << "</methodCall>" << endl << ends;

    bool result;

#ifdef HAVE_SSTREAM
    result = post(resource, strBuf.str().c_str());
    strBuf.str("");
#else
    delete oldStrBuf;
    oldStrBuf = NULL;
    result = post(resource, (const char *)buffer);
#endif

    return result;
}

bool XMLStream::open(const char *resource)
{
    return true;
}

void XMLStream::close(void)
{}

Slog::Level XMLStream::getLogging(void)
{
    return Slog::levelCritical;
}

void XMLStream::comment(const unsigned char *text, size_t len)
{}

void XMLStream::startDocument(void)
{}

void XMLStream::endDocument(void)
{}

XMLStream::~XMLStream()
{}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
