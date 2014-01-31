// Copyright (C) 1999-2005 Open Source Telecom Corporation.
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
#include <ucommon/memory.h>
#include <stdlib.h>
#include <ctype.h>
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#undef  getc
#undef  putc
#undef  puts
#undef  gets

using namespace UCOMMON_NAMESPACE;

void MemoryProtocol::fault(void) const
{
}

char *MemoryProtocol::dup(const char *str)
{
    if(!str)
        return NULL;
    size_t len = strlen(str) + 1;
    char *mem = static_cast<char *>(alloc(len));
    if(mem)
        String::set(mem, len, str);
    else
        fault();
    return mem;
}

void *MemoryProtocol::dup(void *obj, size_t size)
{
    assert(obj != NULL);
    assert(size > 0);

    char *mem = static_cast<char *>(alloc(size));
    if(mem)
        memcpy(mem, obj, size);
    else
        fault();
    return mem;
}

void *MemoryProtocol::zalloc(size_t size)
{
    void *mem = alloc(size);

    if(mem)
        memset(mem, 0, size);
    else
        fault();

    return mem;
}

MemoryProtocol::~MemoryProtocol()
{
}

MemoryRedirect::MemoryRedirect(MemoryProtocol *protocol)
{
    target = protocol;
}

void *MemoryRedirect::_alloc(size_t size)
{
    // a null redirect uses the heap...
    if(!target)
        return malloc(size);

    return target->_alloc(size);
}

BufferProtocol::BufferProtocol() : CharacterProtocol()
{
    end = true;
    eol = "\r\n";
    input = output = buffer = NULL;
}

BufferProtocol::BufferProtocol(size_t size, mode_t mode)
{
    end = true;
    eol = "\r\n";
    input = output = buffer = NULL;
    allocate(size, mode);
}

BufferProtocol::~BufferProtocol()
{
    release();
}

void BufferProtocol::fault(void) const
{
}

void BufferProtocol::release(void)
{
    if(buffer) {
        flush();
        free(buffer);
        input = output = buffer = NULL;
        end = true;
    }
}

void BufferProtocol::allocate(size_t size, mode_t mode)
{
    release();
    _clear();

    if(!size)
        return;

    back = 0;
    switch(mode) {
    case RDWR:
        input = buffer = (char *)malloc(size * 2);
        if(buffer)
            output = buffer + size;
        else
            fault();
        break;
    case RDONLY:
        input = buffer = (char *)malloc(size);
        if(!buffer)
            fault();
        break;
    case WRONLY:
        output = buffer = (char *)malloc(size);
        if(!buffer)
            fault();
        break;
    }

    bufpos = insize = outsize = 0;
    bufsize = size;

    if(buffer)
        end = false;
}

bool BufferProtocol::_blocking(void)
{
    return false;
}

size_t BufferProtocol::put(const void *address, size_t size)
{
    size_t count = 0;

    if(!output || !address || !size)
        return 0;

    const char *cp = (const char *)address;

    while(count < size) {
        if(outsize == bufsize) {
            outsize = 0;
            if(_push(output, bufsize) < bufsize) {
                output = NULL;
                end = true;     // marks a disconnection...
                return count;
            }
        }
        output[outsize++] = cp[count++];
    }
    return count;
}

size_t BufferProtocol::get(void *address, size_t size)
{
    size_t count = 0;

    if(!input || !address || !size)
        return 0;

    char *cp = (char *)address;

    while(count < size) {
        if(bufpos == insize) {
            if(end)
                return count;

            insize = _pull(input, bufsize);
            bufpos = 0;
            if(insize == 0)
                end = true;
            else if(insize < bufsize && !_blocking())
                end = true;

            if(!insize)
                return count;
        }
        cp[count++] = input[bufpos++];
    }
    return count;
}

int BufferProtocol::_getch(void)
{
    if(!input)
        return EOF;

    if(back) {
        back = 0;
        return back;
    }

    if(bufpos == insize) {
        if(end)
            return EOF;

        insize = _pull(input, bufsize);
        bufpos = 0;
        if(insize == 0)
            end = true;
        else if(insize < bufsize && !_blocking())
            end = true;

        if(!insize)
            return EOF;
    }

    return input[bufpos++];
}

size_t CharacterProtocol::putchars(const char *address, size_t size)
{
    size_t count = 0;

    if(!address)
        return 0;

    if(!size)
        size = strlen(address);

    while(count < size) {
        if(_putch(*address) == EOF)
            break;
        ++count;
        ++address;
    }

    return count;
}

int BufferProtocol::_putch(int ch)
{
    if(!output)
        return EOF;

    if(ch == 0) {
        puts(eol);
        flush();
        return 0;
    }

    if(outsize == bufsize) {
        outsize = 0;
        if(_push(output, bufsize) < bufsize) {
            output = NULL;
            end = true;     // marks a disconnection...
            return EOF;
        }
    }

    output[outsize++] = ch;
    return ch;
}

size_t BufferProtocol::printf(const char *pformat, ...)
{
    va_list args;
    int result;
    size_t count;

    if(!flush() || !output || !pformat)
        return 0;

    va_start(args, pformat);
    result = vsnprintf(output, bufsize, pformat, args);
    va_end(args);
    if(result < 1)
        return 0;

    if((size_t)result > bufsize)
        result = bufsize;

    count = _push(output, result);
    if(count < (size_t)result) {
        output = NULL;
        end = true;
    }

    return count;
}

void BufferProtocol::purge(void)
{
    outsize = insize = bufpos = 0;
}

bool BufferProtocol::_flush(void)
{
    if(!output)
        return false;

    if(!outsize)
        return true;

    if(_push(output, outsize) == outsize) {
        outsize = 0;
        return true;
    }
    output = NULL;
    end = true;
    return false;
}

char *BufferProtocol::gather(size_t size)
{
    if(!input || size > bufsize)
        return NULL;

    if(size + bufpos > insize) {
        if(end)
            return NULL;

        size_t adjust = outsize - bufpos;
        memmove(input, input + bufpos, adjust);
        insize = adjust +  _pull(input, bufsize - adjust);
        bufpos = 0;

        if(insize < bufsize)
            end = true;
    }

    if(size + bufpos <= insize) {
        char *bp = input + bufpos;
        bufpos += size;
        return bp;
    }

    return NULL;
}

char *BufferProtocol::request(size_t size)
{
    if(!output || size > bufsize)
        return NULL;

    if(size + outsize > bufsize)
        flush();

    size_t offset = outsize;
    outsize += size;
    return output + offset;
}

size_t CharacterProtocol::putline(const char *string)
{
    size_t count = 0;

    while(string && *string && (EOF != _putch(*string)))
        ++count;

    string = eol;
    while(string && *string && (EOF != _putch(*string)))
        ++count;

    return count;
}

size_t CharacterProtocol::getline(String& s)
{
    size_t result = getline(s.c_mem(), s.size() + 1);
    String::fix(s);
    return result;
}

size_t CharacterProtocol::getline(char *string, size_t size)
{
    size_t count = 0;
    unsigned eolp = 0;
    const char *eols = eol;
    bool eof = false;

    if(!eols)
        eols = "\0";

    if(string)
        string[0] = 0;

    while(count < size - 1) {
        int ch = _getch();
        if(ch == EOF) {
            eolp = 0;
            eof = true;
            break;
        }

        string[count++] = ch;

        if(ch == eols[eolp]) {
            ++eolp;
            if(eols[eolp] == 0)
                break;
        }
        else
            eolp = 0;

        // special case for \r\n can also be just eol as \n
        if(eq(eol, "\r\n") && ch == '\n') {
            ++eolp;
            break;
        }
    }
    count -= eolp;
    string[count] = 0;
    if(!eof)
        ++count;
    return count;
}

size_t CharacterProtocol::print(const PrintProtocol& f)
{
    const char *cp = f._print();

    if(cp == NULL)
        return putchar(0);

    return putchars(cp);
}

size_t CharacterProtocol::input(InputProtocol& f)
{
    int c;
    size_t count = 0;

    for(;;) {
        if(back) {
            c = back;
            back = 0;
        }
        else
            c = _getch();

        c = f._input(c);
        if(c) {
            if(c != EOF)
                back = c;
            else
                ++count;
            break;
        }
        ++count;
    }
    return count;
}

size_t CharacterProtocol::load(StringPager *list)
{
    if(!list)
        return 0;

    size_t used = 0;
    size_t size = list->size() - 64;

    char *tmp = (char *)malloc(size);
    while(getline(tmp, size)) {
        if(!list->filter(tmp, size))
            break;

        ++used;
    }
    free(tmp);
    return used;
}

size_t CharacterProtocol::save(const StringPager *list)
{
    size_t used = 0;
    if(!list)
        return 0;

    StringPager::iterator sp = list->begin();
    while(is(sp) && putline(sp->get())) {
        ++used;
        sp.next();
    }
    return used;
}

void BufferProtocol::reset(void)
{
    insize = bufpos = 0;
}

bool BufferProtocol::eof(void)
{
    if(!input)
        return true;

    if(end && bufpos == insize)
        return true;

    return false;
}

bool BufferProtocol::_pending(void)
{
    if(!input)
        return false;

    if(!bufpos)
        return false;

    return true;
}

void LockingProtocol::_lock(void)
{
}

void LockingProtocol::_unlock(void)
{
}

LockingProtocol::~LockingProtocol()
{
}

CharacterProtocol::CharacterProtocol()
{
    back = 0;
    eol = "\n";
}

CharacterProtocol::~CharacterProtocol()
{
}

ObjectProtocol::~ObjectProtocol()
{
}

ObjectProtocol *ObjectProtocol::copy(void)
{
    retain();
    return this;
}

CharacterProtocol& _character_operators::print(CharacterProtocol& p, const char& c)
{
    p.putchar((int)c);
    return p;
}

CharacterProtocol& _character_operators::input(CharacterProtocol& p, char& c)
{
    int code = p.getchar();
    if(code == EOF)
        code = 0;
    c = code;
    return p;
}

CharacterProtocol& _character_operators::print(CharacterProtocol& p, const char *str)
{
    if(!str)
        p.putline("");
    else
        p.putchars(str);
    return p;
}

CharacterProtocol& _character_operators::input(CharacterProtocol& p, String& str)
{
    if(str.c_mem()) {
        p.getline(str.c_mem(), str.size());
        String::fix(str);
    }
    return p;
}

CharacterProtocol& _character_operators::print(CharacterProtocol& p, const long& v)
{
    char buf[40];
    snprintf(buf, sizeof(buf), "%ld", v);
    p.putchars(buf);
    return p;
}

CharacterProtocol& _character_operators::print(CharacterProtocol& p, const double& v)
{
    char buf[40];
    snprintf(buf, sizeof(buf), "%f", v);
    p.putchars(buf);
    return p;
}


class __LOCAL _input_long : public InputProtocol
{
public:
    long* ref;
    size_t pos;
    char buf[32];

    _input_long(long& v);

    int _input(int code);
};

class __LOCAL _input_double : public InputProtocol
{
public:
    double* ref;
    bool dot;
    bool e;
    size_t pos;
    char buf[60];

    _input_double(double& v);

    int _input(int code);
};

_input_long::_input_long(long& v)
{
    ref = &v;
    v = 0l;
    pos = 0;
}

_input_double::_input_double(double& v)
{
    dot = e = false;
    v = 0.0;
    pos = 0;
    ref = &v;
}

int _input_long::_input(int code)
{
    if(code == '-' && !pos)
        goto valid;

    if(isdigit(code) && pos < sizeof(buf) - 1)
        goto valid;

    buf[pos] = 0;
    if(pos)
        sscanf(buf, "%ld", ref);

    return code;

valid:
    buf[pos++] = code;
    return 0;
}

int _input_double::_input(int code)
{
    if(code == '-' && !pos)
        goto valid;

    if(code == '-' && buf[pos] == 'e')
        goto valid;

    if(tolower(code) == 'e' && !e) {
        e = true;
        code = 'e';
        goto valid;
    }

    if(code == '.' && !dot) {
        dot = true;
        goto valid;
    }

    if(isdigit(code) && pos < sizeof(buf) - 1)
        goto valid;

    buf[pos] = 0;
    if(pos)
        sscanf(buf, "%lf", ref);

    return code;

valid:
    buf[pos++] = code;
    return 0;
}

CharacterProtocol& _character_operators::input(CharacterProtocol& p, long& v)
{
    _input_long lv(v);
    p.input(lv);
    return p;
}

CharacterProtocol& _character_operators::input(CharacterProtocol& p, double& v)
{
    _input_double lv(v);
    p.input(lv);
    return p;
}

PrintProtocol::~PrintProtocol()
{
}

InputProtocol::~InputProtocol()
{
}
