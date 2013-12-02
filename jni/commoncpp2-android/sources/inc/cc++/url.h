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
 * @file url.h
 * @short URL streams abstraction.
 **/

#ifndef CCXX_URL_H_
#define CCXX_URL_H_

#ifndef CCXX_CONFIG_H_
#include <cc++/config.h>
#endif

#ifndef CCXX_SOCKET_H_
#include <cc++/socket.h>
#endif

#ifndef CCXX_MIME_H_
#include <cc++/mime.h>
#endif

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

/**
 * A URL processing version of TCPStream.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short C++ url processing stream class.
 */
class __EXPORT URLStream : public TCPStream
{
public:
    /**
     * Return error for url fetch
     */
    typedef enum {
        errSuccess = 0,
        errUnreachable,
        errMissing,
        errDenied,
        errInvalid,
        errForbidden,
        errUnauthorized,
        errRelocated,
        errFailure,
        errTimeout,
        errInterface
    } Error;

    /**
     * Type of authentication
     */
    typedef enum {
        authAnonymous = 0,
        authBasic
    } Authentication;

    /**
     * Encoding used in transfer
     */
    typedef enum {
        encodingBinary = 0,
        encodingChunked
    } Encoding;

    /**
     * Type of fetch
     */
    typedef enum {
        methodHttpGet,
        methodHttpPut,
        methodHttpPost,
        methodHttpPostMultipart,
        methodFtpGet,
        methodFtpPut,
        methodFileGet,
        methodFilePut
    } Method;

    /**
     * http protocol version
     */
    typedef enum {
        protocolHttp1_0,
        protocolHttp1_1
    } Protocol;

private:
    const char *agent, *referer, *cookie, *pragma, *user, *password;
    const char *proxyUser, *proxyPasswd;
    const char *localif;
    IPV4Host proxyHost;
#ifdef  CCXX_IPV6
    IPV6Host v6proxyHost;
#endif
    tpport_t proxyPort;
    Method urlmethod;
    Encoding encoding;
    Protocol protocol;
    Authentication auth;
    Authentication proxyAuth;
    timeout_t timeout;
    bool persistent;
    bool follow;
    unsigned chunk;

    Error getHTTPHeaders();
    URLStream(const URLStream& rhs);

protected:
    ost::String m_host, m_address;

    /**
     * Send http header to server.
     *
     * @param url base to send header to
     * @param vars to post or use in get method
     * @param bufsize of stream buffering to use
     * @return success or class error
     */
    Error sendHTTPHeader(const char *url, const char **vars, size_t bufsize);

    /**
     * Called if stream buffer needs refilling.
     *
     * @return number of bytes refilled or error if < 0
     */
    int underflow(void);

    /**
     * Derived method for async or timed I/O function on url stream.
     *
     * @return number of bytes read or < 0 for error.
     * @param buffer to read stream data into.
     * @param len of bytes to read from stream.
     * @param timer to wait for data in milliseconds.
     */
    virtual int aRead(char *buffer, size_t len, timeout_t timer);

    /**
     * Derived method for async or timed I/O function on url stream.
     *
     * @return number of bytes written or < 0 for error.
     * @param buffer to write stream data from.
     * @param len of bytes to write to stream.
     * @param timer to wait for data in milliseconds.
     */
    virtual int aWrite(char *buffer, size_t len, timeout_t timer);

    /**
     * Derived method to receive and parse http "headers".
     *
     * @param header keyword.
     * @param value header keyword value.
     */
    virtual void httpHeader(const char *header, const char *value);

    /**
     * A virtual to insert additional header info into the request.
     *
     * @return array of header attributes to add.
     */
    virtual char **extraHeader(void);

public:
    /**
     * Construct an instance of URL stream.
     *
     * @param family protocol to use.
     * @param timer for default timeout on I/O operations.
     */
    URLStream(Family family = IPV4, timeout_t timer = 0);

    /**
     * Line parsing with conversion.
     *
     * @return URLStream object reference.
     * @param buffer to store.
     * @param len maximum buffer size.
     */
    URLStream &getline(char *buffer, size_t len);

    /**
     * Get URL data from a named stream of a known buffer size.
     *
     * @return url error code.
     * @param url name of resource.
     * @param buffer size of buffer.
     */
    Error get(const char *url, size_t buffer = 512);

    /**
    * Get URL data from a named stream of a known buffer size.
    * Requesting URL defined in previous calls of setAddress() and
    * setHost() functions.
    *
    * @return url error code.
    * @param buffer size of buffer.
    */
    Error get(size_t buffer = 512);

    /**
     * Submit URL with vars passed as argument array.  This submit
     * assumes "GET" method.  Use "post" member to perform post.
     *
     * @return url error code.
     * @param url name of resource.
     * @param vars to set.
     * @param buffer size of buffer.
     */
    Error submit(const char *url, const char **vars, size_t buffer = 512);

    /**
     * Post URL vars with post method.
     *
     * @return success or error code.
     * @param url name of resource being posted.
     * @param vars to set in post.
     * @param buffer size of buffer.
     */
    Error post(const char *url, const char **vars, size_t buffer = 512);

    /**
     * Post URL with MIME multipart form.
     *
     * @return success or error code.
     * @param url name of resource being posted.
     * @param form multi-part resource.
     * @param buffer size to use.
     */
    Error post(const char *url, MIMEMultipartForm &form, size_t buffer = 512);

    /**
     * Used to fetch header information for a resource.
     *
     * @return url error code.
     * @param url name of resource.
     * @param buffer size of buffer.
     */
    Error head(const char *url, size_t buffer = 512);

    /**
     * Close the URL stream for a new connection.
     */
    void close();

    /**
     * Set the referer url.
     *
     * @param str referer string.
     */
    void setReferer(const char *str);

    /**
    * Set the host for the url
    *
    * @param str host address.
    */
    inline void setHost(const char *str)
        {m_host = str;};

    /**
    * Set the address for the url
    *
    * @param str address in the URL.
    */
    inline void setAddress(const char *str)
        {m_address = str;};

    /**
     * Set the cookie to pass.
     *
     * @param str cookie string.
     */
    inline void setCookie(const char *str)
        {cookie = str;};

    /**
     * Set user id for the url.
     *
     * @param str user id.
     */
    inline void setUser(const char *str)
        {user = str;};

    /**
     * Set password for the url.
     *
     * @param str password.
     */
    inline void setPassword(const char *str)
        {password = str;};

    /**
     * Set authentication type for the url.
     *
     * @param a authentication.
     * @param str string.
     */
    void setAuthentication(Authentication a, const char *str = NULL);

    /**
     * Set proxy user id for the url.
     *
     * @param str user id.
     */
    inline void setProxyUser(const char *str)
        {proxyUser = str;};

    /**
     * Set proxy password for the url.
     *
     * @param str password.
     */
    inline void setProxyPassword(const char *str)
        {proxyPasswd = str;};

    /**
     * Set proxy authentication type for the url.
     *
     * @param a authentication.
     * @param str string.
     */
    void setProxyAuthentication(Authentication a, const char *str = NULL);

    /**
     * Set the pragmas.
     *
     * @param str pragma setting.
     */
    inline void setPragma(const char *str)
        {pragma = str;};

    /**
     * Set the proxy server used.
     *
     * @param host proxy host.
     * @param port proxy port.
     */
    void setProxy(const char *host, tpport_t port);

    /**
     * Set the agent.
     *
     * @param str agent value.
     */
    inline void setAgent(const char *str)
        {agent = str;};

    /**
     * Get url method (and protocol) employed.
     *
     * @return url method in effect.
     */
    inline Method getMethod(void)
        {return urlmethod;};

    /**
     * Set socket timeout characteristics for processing URL
     * requests.  Set to 0 for no default timeouts.
     *
     * @param to timeout to set.
     */
    inline void setTimeout(timeout_t to)
        {timeout = to;};

    /**
     * Specify url following.  Set to false to disable following
     * of relocation requests.
     *
     * @param enable true to enable following.
     */
    inline void setFollow(bool enable)
        {follow = enable;};

    /**
     * Specify http protocol level being used.
     *
     * @param pro protocol level.
     */
    inline void setProtocol(Protocol pro)
        {protocol = pro;};
    /**
     * Specify local interface to use
     *
     * @param intf Local interface name
     */
    inline void setLocalInterface(const char *intf)
    {localif=intf;}
};

/** @relates URLStream
 * Decode an url parameter (ie "\%20" -> " ")
 * @param source string
 * @param dest destination buffer. If NULL source is used
 */
__EXPORT char* urlDecode(char *source, char *dest = NULL);

/** @relates URLStream
 * Encode an url parameter (ie " " -> "+")
 * @param source string
 * @param dest destination buffer. Do not overlap with source
 * @param size destination buffer size.
 */
__EXPORT char* urlEncode(const char *source, char *dest, size_t size);

/** @relates URLStream
 * Decode a string using base64 coding.
 * Destination size should be at least strlen(src)+1.
 * Destination will be a string, so is always terminated .
 * This function is deprecated, base64 can use binary source, not only string
 * use overloaded b64Decode.
 * @return string coded
 * @param src  source buffer
 * @param dest destination buffer. If NULL src is used
 */
__EXPORT char* b64Decode(char *src, char *dest = NULL);

/** @relates URLStream
 * Encode a string using base64 coding.
 * Destination size should be at least strlen(src)/4*3+1.
 * Destination is string terminated.
 * This function is deprecated, coded stream can contain terminator character
 * use overloaded b64Encode instead.
 * @return destination buffer
 * @param source source string
 * @param dest   destination octet buffer
 * @param size   destination buffer size
 */
__EXPORT char* b64Encode(const char *source, char *dest, size_t size);

/** @relates URLStream
 * Encode a octet stream using base64 coding.
 * Destination size should be at least (srcsize+2)/3*4+1.
 * Destination will be a string, so is always terminated
 * (unless you pass dstsize == 0).
 * @return size of string written not counting terminator
 * @param src     source buffer
 * @param srcsize source buffer size
 * @param dst     destination buffer
 * @param dstsize destination buffer size
 */
__EXPORT size_t b64Encode(const unsigned char *src, size_t srcsize,
           char *dst, size_t dstsize);

/** @relates URLStream
 * Decode a string using base64 coding.
 * Destination size should be at least strlen(src)/4*3.
 * Destination are not string terminated (It's just a octet stream).
 * @return number of octets written into destination buffer
 * @param src     source string
 * @param dst     destination octet buffer
 * @param dstsize destination buffer size
 */
__EXPORT size_t b64Decode(const char *src,
        unsigned char *dst, size_t dstsize);

/** @relates URLStream
 * Encode a STL string using base64 coding into a STL string
 * @return base 64 encoded string
 * @param src source string
 */
__EXPORT String b64Encode(const String& src);

/** @relates URLStream
 * Decode a STL string using base64 coding into an STL String.
 * Destination size should be at least strlen(src)/4*3.
 * Destination are not string terminated (It's just a octet stream).
 * @return decoded string
 * @param src     source string
 */
__EXPORT String b64Decode(const String& src);

/** @relates URLStream
 * Encode a octet stream using base64 coding into a STL string
 * @return base 64 encoded string
 * @param src     source buffer
 * @param srcsize source buffer size
 */
__EXPORT String b64Encode(const unsigned char *src, size_t srcsize);

/** @relates URLStream
 * Decode a string using base64 coding.
 * Destination size should be at least strlen(src)/4*3.
 * Destination are not string terminated (It's just a octet stream).
 * @return number of octets written into destination buffer
 * @param src     source string
 * @param dst     destination octet buffer
 * @param dstsize destination buffer size
 */
__EXPORT size_t b64Decode(const String& src,
        unsigned char *dst, size_t dstsize);


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
