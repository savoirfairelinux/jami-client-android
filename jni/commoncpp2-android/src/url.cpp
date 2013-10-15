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
#include <cc++/address.h>
#include <cc++/socket.h>
#ifndef CCXX_WITHOUT_EXTRAS
#include <cc++/export.h>
#endif
#include <cc++/url.h>

#include <string>
#include <cstdio>
#include <cstdlib>
#include <fcntl.h>
#include <cerrno>
#include <iostream>

#ifdef  WIN32
#include <io.h>
#endif

#ifdef HAVE_SSTREAM
#include <sstream>
#else
#include <strstream>
#endif

#include <cctype>

#ifndef WIN32
// cause problem on Solaris
#if !defined(__sun) && !defined(__SUN__)
#ifdef  HAVE_NET_IF_H
#include <net/if.h>
#endif
#endif
#include <sys/ioctl.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
using namespace std;
#endif

URLStream::URLStream(Family fam, timeout_t to) :
TCPStream(fam)
{
    persistent = false;
    proxyPort = 0;
    timeout = to;
    protocol = protocolHttp1_0;
    follow = true;
    proxyAuth = authAnonymous;
    encoding = encodingBinary;
    proxyUser = proxyPasswd = NULL;
    auth = authAnonymous;
    cookie = agent = pragma = referer = user = password = NULL;
    localif = NULL;
    setError(false);
}

int URLStream::aRead(char *buffer, size_t len, timeout_t timer)
{
    return readData(buffer, len, 0, timer);
}

int URLStream::aWrite(char *buffer, size_t len, timeout_t timer)
{
    return writeData(buffer, len, timer);
}

void URLStream::httpHeader(const char *header, const char *value)
{
}

char **URLStream::extraHeader(void)
{
    return NULL;
}

int URLStream::underflow(void)
{
    ssize_t len = 0, rlen;
    char *buf;

    if(bufsize == 1)
        return TCPStream::underflow();

    if(!gptr())
        return EOF;

    if(gptr() < egptr())
        return (unsigned char)*gptr();

    rlen = (ssize_t)((gbuf + bufsize) - eback());
    if(encoding == encodingChunked) {
        buf = (char *)eback();
        *buf = '\n';
        while(!chunk && (*buf == '\n' || *buf == '\r')) {
            *buf = 0;
            len = readLine(buf, rlen, timeout);
        }
        if(len) {
            if(!chunk)
                chunk = strtol(buf, NULL, 16);

            if(rlen > (int)chunk)
                rlen = chunk;
        }
        else
            rlen = -1;
    }

    if(rlen > 0) {
        if(Socket::state == STREAM)
            rlen = aRead((char *)eback(), rlen, timeout);
        else if(timeout) {
            if(Socket::isPending(pendingInput, timeout))
                rlen = readData(eback(), rlen);
            else
                rlen = -1;
        }
        else
            rlen = readData(eback(), rlen);
    }
    if(encoding == encodingChunked && rlen > 0)
        chunk -= rlen;

    if(rlen < 1) {
        if(rlen < 0)
            clear(ios::failbit | rdstate());
        return EOF;
    }
    setg(eback(), eback(), eback() + rlen);
    return (unsigned char)*gptr();
}

void URLStream::setProxy(const char *host, tpport_t port)
{
    switch(family) {
#ifdef  CCXX_IPV6
    case IPV6:
        v6proxyHost = host;
        break;
#endif
    case IPV4:
        proxyHost = host;
        break;
    default:
        proxyPort = 0;
        return;
    }
    proxyPort = port;
}

URLStream::Error URLStream::submit(const char *path, const char **vars, size_t buf)
{
    Error status = errInvalid, saved;

    if(!strnicmp(path, "http:", 5)) {
        urlmethod = methodHttpGet;
        path = strchr(path + 5, '/');
        status = sendHTTPHeader(path, vars, buf);
    }
    if((status == errInvalid || status == errTimeout)) {
        if(Socket::state != AVAILABLE)
            close();
        return status;
    }
    else {
        saved = status;
        status = getHTTPHeaders();
        if(status == errSuccess)
            return saved;
        else if(status == errTimeout) {
            if(Socket::state != AVAILABLE)
                close();
        }
        return status;
    }
}

URLStream::Error URLStream::post(const char *path, MIMEMultipartForm &form, size_t buf)
{
    Error status = errInvalid, saved;
    if(!strnicmp(path, "http:", 5)) {
        urlmethod = methodHttpPostMultipart;
        path = strchr(path + 5, '/');
        status = sendHTTPHeader(path, (const char **)form.getHeaders(), buf);
    }

    if(status == errInvalid || status == errTimeout) {
        if(Socket::state != AVAILABLE)
            close();
        return status;
    }
    saved = status;
    status = getHTTPHeaders();
    if(status == errSuccess) {
        form.body(dynamic_cast<std::ostream *>(this));
        return saved;
    }
    if(status == errTimeout) {
        if(Socket::state != AVAILABLE)
            close();
    }
    return status;
}

URLStream::Error URLStream::post(const char *path, const char **vars, size_t buf)
{
    Error status = errInvalid, saved;

    if(!strnicmp(path, "http:", 5)) {
        urlmethod = methodHttpPost;
        path = strchr(path + 5, '/');
        status = sendHTTPHeader(path, vars, buf);
    }

    if((status == errInvalid || status == errTimeout)) {
        if(Socket::state != AVAILABLE)
            close();
        return status;
    }
    saved = status;
    status = getHTTPHeaders();
    if(status == errSuccess)
        return saved;
    if(status == errTimeout) {
        if(Socket::state != AVAILABLE)
            close();
    }
    return status;
}


URLStream::Error URLStream::head(const char *path, size_t buf)
{
    Error status = errInvalid, saved;

    if(!strnicmp(path, "http:", 5)) {
        urlmethod = methodHttpGet;
        path = strchr(path + 5, '/');
        status = sendHTTPHeader(path, NULL, buf);
    }

    if((status == errInvalid || status == errTimeout)) {
        if(Socket::state != AVAILABLE)
            close();
        return status;
    }
    else {
        saved = status;
        status = getHTTPHeaders();
        if(status == errSuccess)
            return saved;
        else if(status == errTimeout) {
            if(Socket::state != AVAILABLE)
                close();
        }
        return status;
    }
}

URLStream &URLStream::getline(char *buffer, size_t size)
{
    size_t len;

    *buffer = 0;
    // TODO: check, we mix use of streambuf with Socket::readLine...
    iostream::getline(buffer, (unsigned long)size);
    len = strlen(buffer);

    while(len) {
        if(buffer[len - 1] == '\r' || buffer[len - 1] == '\n')
            buffer[len - 1] = 0;
        else
            break;
        --len;
    }
    return *this;
}

URLStream::Error URLStream::get(size_t buffer)
{
    String path = String("http://") + m_host;

    if ( m_address.operator[](0) != '/' )
        path += "/";

    path += m_address;

    return get(path.c_str(), buffer);
}

URLStream::Error URLStream::get(const char *urlpath, size_t buf)
{
    const char *path = urlpath;
    Error status = errInvalid, saved;

    urlmethod = methodFileGet;

    if(Socket::state != AVAILABLE)
        close();


    if(!strnicmp(path, "file:", 5)) {
        urlmethod = methodFileGet;
        path += 5;
    }
    else if(!strnicmp(path, "http:", 5)) {
        urlmethod = methodHttpGet;
        path = strchr(path + 5, '/');
    }
    switch(urlmethod) {
    case methodHttpGet:
        status = sendHTTPHeader(path, NULL, buf);
        break;
    case methodFileGet:
        if(so != INVALID_SOCKET)
            ::close((int)so);
        so = ::open(path, O_RDWR);
        if(so == INVALID_SOCKET)
            so = ::open(path, O_RDONLY);
        // FIXME: open return the same handle type as socket call ??
        if(so == INVALID_SOCKET)
            return errInvalid;
        Socket::state = STREAM;
        allocate(buf);
        return errSuccess;
    default:
                break;
    }


    if((status == errInvalid || status == errTimeout)) {
        if(Socket::state != AVAILABLE)
            close();
        return status;
    }
    else {
        saved = status;
        status = getHTTPHeaders();
        if(status == errSuccess)
            return saved;
        else if(status == errTimeout) {
            if(Socket::state != AVAILABLE)
                close();
        }
        return status;
    }
}

URLStream::Error URLStream::getHTTPHeaders()
{
    char buffer[512];
    size_t buf = sizeof(buffer);
    Error status = errSuccess;
    char *cp, *ep;
    ssize_t len = 1;
    char nc = 0;

    chunk = ((unsigned)-1) / 2;
    encoding = encodingBinary;
    while(len > 0) {
        len = readLine(buffer, buf, timeout);
        if(len < 1)
            return errTimeout;

        // FIXME: for multiline syntax ??
        if(buffer[0] == ' ' || buffer[0] == '\r' || buffer[0] == '\n')
            break;
        cp = strchr(buffer, ':');
        if(!cp)
            continue;
        *(cp++) = 0;
        while(*cp == ' ' || *cp == '\t')
            ++cp;
        ep = strchr(cp, '\n');
        if(!ep)
            ep = &nc;
        while(*ep == '\n' || *ep == '\r' || *ep == ' ') {
            *ep = 0;
            if((--ep) < cp)
                break;
        }
        if(!stricmp(buffer, "Transfer-Encoding")) {
            if(!stricmp(cp, "chunked")) {
                chunk = 0;
                encoding = encodingChunked;
            }
        }
        httpHeader(buffer, cp);
    }
    return status;
}


void URLStream::close(void)
{
    if(Socket::state == AVAILABLE)
        return;

    endStream();
    so = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if(so != INVALID_SOCKET)
        Socket::state = AVAILABLE;
}

URLStream::Error URLStream::sendHTTPHeader(const char *url, const char **vars, size_t buf)
{
    // TODO: implement authentication
    char reloc[4096];
    // "//" host ":" port == max 2 + 128 + 1 + 5 + 1(\0) = 137, rounded 140
    char host[140];
    // TODO: add support for //user:pass@host:port/ syntax
#ifdef HAVE_SSTREAM
    ostringstream str;
#else
    char buffer[4096];
    strstream str(buffer, sizeof(buffer), ios::out);
#endif
    char *ref, *cp, *ep;
    char *hp;
    const char *uri = "/";
    int count = 0;
    size_t len = 0;
    tpport_t port = 80;
    const char **args = vars;
    const char *var;
    bool lasteq = true;

    struct servent *svc;

retry:
#ifdef HAVE_SSTREAM
    str.str("");
#else
    buffer[0] = 0;
    str.seekp(0);
#endif

    setString(host, sizeof(host), url);
reformat:
    hp = strchr(host, '/');
    if(!hp) {
        host[0] = '/';
        setString(host + 1, sizeof(host) - 1, url);
        goto reformat;
    }
    while(*hp == '/')
        ++hp;
    cp = strchr(hp, '/');
    if (cp) *cp = 0;
    ep = strrchr(hp, ':');
    if(ep) {
        *ep = 0;
        ++ep;
        if(isdigit(*ep))
            port = atoi(ep);
        else {
            Socket::mutex.enter();
            svc = getservbyname(ep, "tcp");
            if(svc)
                port = ntohs(svc->s_port);
            Socket::mutex.leave();
        }
    }

    if(!proxyPort) {
        const char* ep1 = url;
        while(*ep1 == '/')
            ++ep1;
        ep1 = strchr(ep1, '/');
        if(ep1)
                uri = ep1;
    }

    switch(urlmethod) {
    case methodHttpGet:
        str << "GET ";
        if(proxyPort) {
            str << "http:" << url;
            if(!cp) str << '/';
        }
        else
            str << uri;
        break;
    case methodHttpPost:
    case methodHttpPostMultipart:
        str << "POST ";
        if(proxyPort) {
            str << "http:" << url;
            if(!cp) str << '/';
        }
        else
            str << uri;
        break;
    default:
        return errInvalid;
    }

    if(vars && urlmethod == methodHttpGet) {
        str << "?";
        while(*vars) {
            if(count++ && lasteq)
                str << "&";
            str << *vars;
            if(!lasteq)
                lasteq = true;
            else if(strchr(*vars, '='))
                lasteq = true;
            else {
                lasteq = false;
                str << "=";
            }
            ++vars;
        }
    }

    switch(protocol) {
    case protocolHttp1_1:
        str << " HTTP/1.1" << "\r\n";
        break;
    case protocolHttp1_0:
        str << " HTTP/1.0" << "\r\n";
        break;
    }

    if ( m_host.empty() )
        m_host = hp;

    str << "Host: " << hp << "\r\n";
    if(agent)
        str << "User-Agent: " << agent << "\r\n";

    if(cookie)
        str << "Cookie: " << cookie << "\r\n";

    if(pragma)
        str << "Pragma: " << pragma << "\r\n";

    if(referer)
        str << "Referer: " << referer << "\r\n";

    switch(auth) {
    case authBasic:
        str << "Authorization: Basic ";
        snprintf(reloc, 64, "%s:%s", user, password);
        b64Encode(reloc, reloc + 64, 128);
        str << reloc + 64 << "\r\n";
    case authAnonymous:
        break;
    }

    switch(proxyAuth) {
    case authBasic:
        str << "Proxy-Authorization: Basic ";
        snprintf(reloc, 64, "%s:%s", proxyUser, proxyPasswd);
        b64Encode(reloc, reloc + 64, 128);
        str << reloc + 64 << "\r\n";
        str << "Proxy-Connection: close" << "\r\n";
    case authAnonymous:
        break;
    }


    str << "Connection: close\r\n";
    char **add = extraHeader();
    if(add) {
        while(*add) {
            str << *(add++) << ": ";
            str << *(add++) << "\r\n";
        }
    }
    if(vars)
        switch(urlmethod) {
        case methodHttpPost:
            while(*args) {
                var = *args;
                if(count++ || !strchr(var, '='))
                    len += strlen(var) + 1;
                else
                    len = strlen(var);
                ++args;
            }
            count = 0;
            len += 2;
            str << "Content-Type: application/x-www-form-urlencoded" << "\r\n";
            str << "Content-Length: " << (unsigned)len << "\r\n";
            break;
        case methodHttpPostMultipart:
            while(*args)
                str << *(args++) << "\r\n";
        default:
            break;
        }

    str << "\r\n";
#ifdef HAVE_SSTREAM
    // sstream does not want ends
#else
    str << ends;
#endif

    if(Socket::state != AVAILABLE)
            close();
#ifndef WIN32
#ifdef  SOICGIFINDEX
    if (localif != NULL) {
        struct ifreq ifr;

        switch(family) {
#ifdef  CCXX_IPV6
        case IPV6:
            sockaddr_in6 source;
            int alen = sizeof(source);

            memset(&ifr, 0, sizeof(ifr));
            setString(ifr.ifr_name, sizeof(ifr.ifr_name), localif);
            if (ioctl(so, SIOCGIFINDEX, &ifr) < 0)
                return errInterface;
            else {
                if (setsockopt(so, SOL_SOCKET, SO_BINDTODEVICE, &ifr, sizeof(ifr)) == -1)
                    return errInterface;
                else if(getsockname(so, (struct sockaddr*)&source,(socklen_t *) &alen) == -1)
                    return errInterface;
                else if (bind(so, (struct sockaddr*)&source, sizeof(source)) == -1)
                    return errInterface;
                else
                    source.sin6_port = 0;
            }

            break;
#endif
        case IPV4:
            sockaddr_in source;
            int alen = sizeof(source);

            memset(&ifr, 0, sizeof(ifr));
            setString(ifr.ifr_name, sizeof(ifr.ifr_name), localif);
            if (ioctl(so, SIOCGIFINDEX, &ifr) < 0)
                return errInterface;
            else {
                if (setsockopt(so, SOL_SOCKET, SO_BINDTODEVICE, &ifr, sizeof(ifr)) == -1)
                    return errInterface;
                else if(getsockname(so, (struct sockaddr*)&source,(socklen_t *) &alen) == -1)
                    return errInterface;
                else if (bind(so, (struct sockaddr*)&source, sizeof(source)) == -1)
                    return errInterface;
                else
                    source.sin_port = 0;
            }
        }

    }
#endif
#endif

    if(proxyPort) {
        switch(family) {
#ifdef  CCXX_IPV6
        case IPV6:
            connect(v6proxyHost, proxyPort, (unsigned)buf);
            break;
#endif
        case IPV4:
                connect(proxyHost, proxyPort, (unsigned)buf);
            break;
        }
    }
    else {
        switch(family) {
#ifdef  CCXX_IPV6
        case IPV6:
            connect(IPV6Host(hp), port, (unsigned)buf);
            break;
#endif
        case IPV4:
            connect(IPV4Host(hp), port, (unsigned)buf);
        }
    }

    if(!isConnected())
        return errUnreachable;

    // FIXME: send (or write) can send less than len bytes
    // use stream funcion ??
#ifdef HAVE_SSTREAM
    writeData(str.str().c_str(), _IOLEN64 str.str().length());
#else
    writeData(str.str().c_str(), _IOLEN64 str.str().length());
#endif

    if(urlmethod == methodHttpPost && vars) {
#ifdef HAVE_SSTREAM
            str.str() = "";
#else
        str.seekp(0);
#endif
        bool sep = false;
        while(*vars) {
            if(sep)
                writeData("&", 1);
            else
                sep = true;
            var = *vars;
            if(!strchr(var, '=')) {
                snprintf(reloc, sizeof(reloc), "%s=%s", var, *(++vars));
                writeData(reloc, strlen(reloc));
            }
            else
                writeData(var, strlen(var));
            ++vars;
        }
        writeData("\r\n", 2);
    }

cont:
#ifdef HAVE_SSTREAM
    char buffer[4096];
#else
    // nothing here
#endif

    len = readLine(buffer, sizeof(buffer) - 1, timeout);
    if(len < 1)
        return errTimeout;

    if(strnicmp(buffer, "HTTP/", 5))
        return errInvalid;

    ref = strchr(buffer, ' ');
    while(*ref == ' ')
        ++ref;

    switch(atoi(ref)) {
        default:
        return errInvalid;
    case 100:
        goto cont;
        case 200:
        return errSuccess;
        case 401:
        return errUnauthorized;
        case 403:
        return errForbidden;
        case 404:
        return errMissing;
        case 405:
        return errDenied;
        case 500:
        case 501:
        case 502:
        case 503:
        case 504:
        case 505:
        return errFailure;
        case 300:
        case 301:
        case 302:
        break;
    }
    if(!follow)
        return errRelocated;
    for(;;) {
        len = readLine(reloc, sizeof(reloc), timeout);
        if(len < 1)
            return errTimeout;
        if(!strnicmp(reloc, "Location: ", 10))
            break;
    }
    if(!strnicmp(reloc + 10, "http:", 5)) {
        url = strchr(reloc + 15, '/');
        ep = (char *)(url + strlen(url) - 1);
        while(*ep == '\r' || *ep == '\n')
            *(ep--) = 0;
    }
    else
        url = reloc + 10;
    close();
    goto retry;
}

void URLStream::setAuthentication(Authentication a, const char *value)
{
    auth = a;
    if (auth != authAnonymous) {
        if(!user)
            user = "anonymous";
        if(!password)
            password = "";
    }
}

void URLStream::setProxyAuthentication(Authentication a, const char *value)
{
    proxyAuth = a;
    if (proxyAuth != authAnonymous) {
        if(!proxyUser)
            proxyUser = "anonymous";

        if(!proxyPasswd)
            proxyPasswd = "";
    }
}

void URLStream::setReferer(const char *str)
{
    if(!str)
        return;
    referer = str;
}

#ifdef  CCXX_NAMESPACES
}
#endif

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */
