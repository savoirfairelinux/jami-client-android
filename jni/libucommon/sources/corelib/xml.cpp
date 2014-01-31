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
#include <ucommon/protocols.h>
#include <ucommon/string.h>
#include <ucommon/xml.h>
#include <ctype.h>

using namespace UCOMMON_NAMESPACE;

static bool isElement(char c)
{
    return isalnum(c) || c == ':' || c == '-' || c == '.' || c == '_';
}

XMLParser::XMLParser(unsigned size)
{
    state = NONE;
    bufpos = 0;
    bufsize = size;
    buffer = new char[size];
    ecount = dcount = 0;
}

XMLParser::~XMLParser()
{
    if(buffer) {
        delete[] buffer;
        buffer = NULL;
    }
}

void XMLParser::putBuffer(char c)
{
    buffer[bufpos++] = c;
    if(bufpos >= bufsize) {
        if(ecount)
            characters((caddr_t)buffer, bufpos);
        bufpos = 0;
    }
}

void XMLParser::clearBuffer(void)
{
    if(bufpos && ecount)
        characters((caddr_t)buffer, bufpos);
    bufpos = 0;
}

bool XMLParser::parse(FILE *fp)
{
    state = NONE;
    bufpos = 0;
    ecount = dcount = 0;

    int ch;
    unsigned char cp;

    while((ch = fgetc(fp)) != EOF) {
        switch(state) {
        case AMP:
            if((!bufpos && ch == '#') || isElement(ch)) {
                buffer[bufpos++] = ch;
                break;
            }
            if(ch != ';')
                return false;
            buffer[bufpos] = 0;
            if(buffer[0] == '#')
                cp = atoi(buffer + 1);
            else if(eq(buffer, "amp"))
                cp = '&';
            else if(eq(buffer, "lt"))
                cp = '<';
            else if(eq(buffer, "gt"))
                cp = '>';
            else if(eq(buffer, "apos"))
                cp = '`';
            else if(eq(buffer, "quot"))
                cp = '\"';
            else
                return false;
            characters((caddr_t)&cp, 1);
            bufpos = 0;
            state = NONE;
            break;
        case TAG:
            if(ch == '>') {
                state = NONE;
                if(!parseTag())
                    return false;
            }
            else if(ch == '[' && bufpos == 7 && !strncmp(buffer, "![CDATA", 7)) {
                state = CDATA;
            }
            else if(ch == '-' && bufpos == 2 && !strncmp(buffer, "!-", 2)) {
                state = COMMENT;
                bufpos = 0;
            }
            else if(ch == '[' && !strncmp(buffer, "!DOCTYPE ", 9)) {
                state = DTD;
                bufpos = 0;
            }
            else
                putBuffer(ch);
            break;
        case COMMENT:
            if(ch == '>' && bufpos >= 2 && !strncmp(&buffer[bufpos - 2], "--", 2)) {
                bufpos -= 2;
                if(bufpos)
                    comment((caddr_t)buffer, bufpos);
                bufpos = 0;
                state = NONE;
            }
            else {
                buffer[bufpos++] = ch;
                if(bufpos == bufsize) {
                    comment((caddr_t)buffer, bufpos);
                    bufpos = 0;
                }
            }
            break;
        case CDATA:
            putBuffer(ch);
            if(bufpos > 2)
                if(eq(&buffer[bufpos - 3], "]]>")) {
                    bufpos -= 3;
                    state = NONE;
                    clearBuffer();
                }
            break;
        case DTD:
            if(ch == '<')
                ++dcount;
            else if(ch == '>' && dcount)
                --dcount;
            else if(ch == '>')
                state = NONE;
            break;
        case NONE:
            if(ch == '<') {
                clearBuffer();
                state = TAG;
            }
            else if(ecount && ch == '&') {
                clearBuffer();
                state = AMP;
            }
            else if(ecount)
                putBuffer(ch);
            break;
        case END:
            return true;
        }
        if(state == END)
            return true;
    }
    // eof before end of ducument...
    return false;
}


bool XMLParser::parse(CharacterProtocol& io)
{
    state = NONE;
    bufpos = 0;
    ecount = dcount = 0;

    int ch;
    unsigned char cp;

    while((ch = io.getchar()) != EOF) {
        switch(state) {
        case AMP:
            if((!bufpos && ch == '#') || isElement(ch)) {
                buffer[bufpos++] = ch;
                break;
            }
            if(ch != ';')
                return false;
            buffer[bufpos] = 0;
            if(buffer[0] == '#')
                cp = atoi(buffer + 1);
            else if(eq(buffer, "amp"))
                cp = '&';
            else if(eq(buffer, "lt"))
                cp = '<';
            else if(eq(buffer, "gt"))
                cp = '>';
            else if(eq(buffer, "apos"))
                cp = '`';
            else if(eq(buffer, "quot"))
                cp = '\"';
            else
                return false;
            characters((caddr_t)&cp, 1);
            bufpos = 0;
            state = NONE;
            break;
        case TAG:
            if(ch == '>') {
                state = NONE;
                if(!parseTag())
                    return false;
            }
            else if(ch == '[' && bufpos == 7 && !strncmp(buffer, "![CDATA", 7)) {
                state = CDATA;
            }
            else if(ch == '-' && bufpos == 2 && !strncmp(buffer, "!-", 2)) {
                state = COMMENT;
                bufpos = 0;
            }
            else if(ch == '[' && !strncmp(buffer, "!DOCTYPE ", 9)) {
                state = DTD;
                bufpos = 0;
            }
            else
                putBuffer(ch);
            break;
        case COMMENT:
            if(ch == '>' && bufpos >= 2 && !strncmp(&buffer[bufpos - 2], "--", 2)) {
                bufpos -= 2;
                if(bufpos)
                    comment((caddr_t)buffer, bufpos);
                bufpos = 0;
                state = NONE;
            }
            else {
                buffer[bufpos++] = ch;
                if(bufpos == bufsize) {
                    comment((caddr_t)buffer, bufpos);
                    bufpos = 0;
                }
            }
            break;
        case CDATA:
            putBuffer(ch);
            if(bufpos > 2)
                if(eq(&buffer[bufpos - 3], "]]>")) {
                    bufpos -= 3;
                    state = NONE;
                    clearBuffer();
                }
            break;
        case DTD:
            if(ch == '<')
                ++dcount;
            else if(ch == '>' && dcount)
                --dcount;
            else if(ch == '>')
                state = NONE;
            break;
        case NONE:
            if(ch == '<') {
                clearBuffer();
                state = TAG;
            }
            else if(ecount && ch == '&') {
                clearBuffer();
                state = AMP;
            }
            else if(ecount)
                putBuffer(ch);
            break;
        case END:
            return true;
        }
        if(state == END)
            return true;
    }
    // eof before end of ducument...
    return false;
}

bool XMLParser::partial(const char *data, size_t len)
{
    if(state == END)
        state = NONE;

    unsigned char cp;
    while(len--) {
        switch(state) {
        case AMP:
            if((!bufpos && *data == '#') || isElement(*data)) {
                buffer[bufpos++] = *data;
                break;
            }
            if(*data != ';')
                return false;
            buffer[bufpos] = 0;
            if(buffer[0] == '#')
                cp = atoi(buffer + 1);
            else if(eq(buffer, "amp"))
                cp = '&';
            else if(eq(buffer, "lt"))
                cp = '<';
            else if(eq(buffer, "gt"))
                cp = '>';
            else if(eq(buffer, "apos"))
                cp = '`';
            else if(eq(buffer, "quot"))
                cp = '\"';
            else
                return false;
            characters((caddr_t)&cp, 1);
            bufpos = 0;
            state = NONE;
            break;
        case TAG:
            if(*data == '>') {
                state = NONE;
                if(!parseTag())
                    return false;
            }
            else if(*data == '[' && bufpos == 7 && !strncmp(buffer, "![CDATA", 7)) {
                state = CDATA;
            }
            else if(*data == '-' && bufpos == 2 && !strncmp(buffer, "!-", 2)) {
                state = COMMENT;
                bufpos = 0;
            }
            else if(*data == '[' && !strncmp(buffer, "!DOCTYPE ", 9)) {
                state = DTD;
                bufpos = 0;
            }
            else
                putBuffer(*data);
            break;
        case COMMENT:
            if(*data == '>' && bufpos >= 2 && !strncmp(&buffer[bufpos - 2], "--", 2)) {
                bufpos -= 2;
                if(bufpos)
                    comment((caddr_t)buffer, bufpos);
                bufpos = 0;
                state = NONE;
            }
            else {
                buffer[bufpos++] = *data;
                if(bufpos == bufsize) {
                    comment((caddr_t)buffer, bufpos);
                    bufpos = 0;
                }
            }
            break;
        case CDATA:
            putBuffer(*data);
            if(bufpos > 2)
                if(eq(&buffer[bufpos - 3], "]]>")) {
                    bufpos -= 3;
                    state = NONE;
                    clearBuffer();
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
        case END:
            if(*data == '<') {
                clearBuffer();
                state = TAG;
            }
            else if(ecount && *data == '&') {
                clearBuffer();
                state = AMP;
            }
            else if(ecount)
                putBuffer(*data);
        }
        ++data;
    }
    return true;
}

bool XMLParser::parseTag(void)
{
    size_t len = bufpos;
    const char *data = buffer;
    bool end = false;
    caddr_t attrib[128];
    unsigned attr = 0;
    char *ep;

    if(*data == '/') {
        while(--len) {
            if(!isElement(*(++data)))
                break;
        }
        if(len)
            return false;

        buffer[bufpos] = 0;
        endElement((caddr_t)(buffer + 1));
        bufpos = 0;
        --ecount;
        if(ecount < 0)
            return false;
        if(!ecount) {
            state = END;
            endDocument();
        }
    }
    else if(*data == '!') {
        bufpos = 0;
        return true; // dtd
    }
    else if(*data == '?') {
        if(!strnicmp(data, "?xml version=\"", 14)) {
            // version info
        }
        bufpos = 0;
    }
    else if(!isElement(*data))
        return false;
    else {
        end = false;
        if(buffer[bufpos - 1] == '/') {
            --bufpos;
            end = true;
        }
        len = 0;
        data = buffer;
        while(len < bufpos) {
            if(!isElement(*data))
                break;
            ++len;
            ++data;
        }
        if(len == bufpos) {
            if(!ecount)
                startDocument();
            ++ecount;
            attrib[0] = attrib[1] = NULL;
            buffer[bufpos] = 0;
            startElement((caddr_t)buffer, attrib);
            if(end) {
ending:
                --ecount;
                endElement((caddr_t)buffer);
                if(!ecount) {
                    state = END;
                    endDocument();
                }
            }
            bufpos = 0;
            return true;
        }
        if(!ecount)
            startDocument();
        ++ecount;

        // attributes, name is between data and len

        for(;;) {
            while(!isElement(buffer[len]) && len < bufpos) {
                if(!isspace(buffer[len]))
                    return false;
                buffer[len++] = 0;
            }

            if(len == bufpos)
                break;

            attrib[attr++] = (caddr_t)(buffer + len);
            while(len < bufpos && isElement(buffer[len]))
                ++len;

            if(len == bufpos)
                return false;

            if(buffer[len] != '=')
                return false;

            buffer[len++] = 0;
            if(len == bufpos) {
                attrib[attr++] = (caddr_t)"";
                break;
            }

            if(isspace(buffer[len])) {
                attrib[attr++] = (caddr_t)"";
                continue;
            }
            if(buffer[len] == '\'' || buffer[len] == '\"') {
                ep = strchr(buffer + len + 1, buffer[len]);
                if(!ep)
                    return false;
                attrib[attr++] = (caddr_t)buffer + len + 1;
                *(ep++) = 0;
                len = ep - buffer;
                continue;
            }
            if(!isElement(buffer[len]))
                return false;
            attrib[attr++] = (caddr_t)buffer;
            while(isElement(buffer[len]) && len < bufpos)
                ++len;
            if(len == bufpos) {
                buffer[len] = 0;
                break;
            }
        }

        attrib[attr++] = NULL;
        attrib[attr++] = NULL;
        startElement((caddr_t)buffer, attrib);
        if(end)
            goto ending;
        bufpos = 0;
        return true;
    }
    return true;
}

// all our lovely base virtuals stubbed out so if we are lazy and forget to
// implement something we want to ignore anyway (say comments...) we don't
// bring whatever it is crashing down one day when we choose to add a
// comment into an xml stream...

void XMLParser::startDocument()
{
}

void XMLParser::endDocument()
{
}

void XMLParser::comment(caddr_t text, size_t len)
{
}

void XMLParser::characters(caddr_t text, size_t len)
{
}
