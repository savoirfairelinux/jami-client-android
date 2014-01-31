// Copyright (C) 2010 David Sugar, Tycho Softworks.
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
 * This library holds basic cryptographic functions and secure socket support
 * for use with GNU uCommon C++.  This library might be used in conjunction
 * with openssl, gnutls, etc.  If no secure socket library is available, then
 * a stub library may be used with very basic cryptographic support.
 * @file ucommon/secure.h
 */

/**
 * Example of SSL socket code.
 * @example ssl.cpp
 */

/**
 * Example of cryptographic digest code.
 * @example digest.cpp
 */

/**
 * Example of cipher code.
 * @example cipher.cpp
 */

#ifndef _UCOMMON_SECURE_H_
#define _UCOMMON_SECURE_H_

#ifndef _UCOMMON_CONFIG_H_
#include <ucommon/platform.h>
#endif

#ifndef _UCOMMON_UCOMMON_H_
#include <ucommon/ucommon.h>
#endif

#define MAX_CIPHER_KEYSIZE  512
#define MAX_DIGEST_HASHSIZE 512

NAMESPACE_UCOMMON

/**
 * Common secure socket support.  This offers common routines needed for
 * secure/ssl socket support code.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __SHARED secure
{
public:
    /**
     * Different error states of the security context.
     */
    typedef enum {OK=0, INVALID, MISSING_CERTIFICATE, MISSING_PRIVATEKEY, INVALID_CERTIFICATE, INVALID_AUTHORITY, INVALID_PEERNAME, INVALID_CIPHER} error_t;

protected:
    /**
     * Last error flagged for this context.
     */
    error_t error;

    inline secure() {error = OK;};

public:
    /**
     * This is derived in different back-end libraries, and will be used to
     * clear certificate credentials.
     */
    virtual ~secure();

    /**
     * Convenience type to represent a security context.
     */
    typedef secure *client_t;

    typedef secure *server_t;

    /**
     * Convenience type to represent a secure socket session.
     */
    typedef void *session_t;

    /**
     * Convenience type to represent a secure socket buf i/o stream.
     */
    typedef void *bufio_t;

    /**
     * Initialize secure stack for first use, and report if SSL support is
     * compiled in.
     * @return true if ssl support is available, false if not.
     */
    static bool init(void);

    /**
     * Initialize secure stack with fips support.  If fips support is not
     * successfully enabled, the secure stack is also not initialized.  Hence
     * init() can be used for non-fips certified operation if fips fails.
     * @return true if fips support enabled and stack initialized.
     */
    static bool fips(void);

    /**
     * Copy system certificates to a local path.
     * @param path to copy to.
     * @return 0 or error number on failure.
     */
    static int oscerts(const char *path);

    /**
     * Get path to system certificates.
     * @return path to system certificates.
     */
    static const char *oscerts(void);

    /**
     * Verify a certificate chain through your certificate authority.
     * This uses the ca loaded as an optional argument for client and
     * server.  Optionally the hostname of the connection can also be
     * verified by pulling the peer certificate.
     * @param session that is connected.
     * @param peername that we expect.
     * @return secure error level or secure::OK if none.
     */
    static error_t verify(session_t session, const char *peername = NULL);

    /**
     * Create a sever context.  The certificate file used will be based on
     * the init() method name.  This may often be /etc/ssl/certs/initname.pem.
     * Similarly, a matching private key certificate will also be loaded.  An
     * optional certificate authority document can be used when we are
     * establishing a service which ssl clients have their own certificates.
     * @param authority path to use or NULL if none.
     * @return a security context that is cast from derived library.
     */
    static server_t server(const char *keyfile = NULL, const char *authority = NULL);

    /**
     * Create an anonymous client context with an optional authority to
     * validate.
     * @param authority path to use or NULL if none.
     * @return a basic client security context.
     */
    static client_t client(const char *authority = NULL);

    /**
     * Create a peer user client context.  This assumes a user certificate
     * in ~/.ssl/certs and the user private key in ~/.ssl/private.  The
     * path to an authority is also sent.
     * @param authority path to use.
     */
    static client_t user(const char *authority);

    /**
     * Assign a non-default cipher to the context.
     * @param context to set cipher for.
     * @param ciphers to set.
     */
    static void cipher(secure *context, const char *ciphers);

    /**
     * Determine if the current security context is valid.
     * @return true if valid, -1 if not.
     */
    inline bool is_valid(void) const
        {return error == OK;};

    /**
     * Get last error code associated with the security context.
     * @return last error code or 0/OK if none.
     */
    inline error_t err(void)
        {return error;};

    /**
     * Create 36 character traditional version 1 uuid.
     * @param string to write uuid into, must be 37 bytes or more.
     */
    static void uuid(char *string);

    static String uuid(void);

    template <typename T>
    inline static void erase(T *object)
        {memset(object, 0, sizeof(object)); delete object;}

    inline operator bool()
        {return is_valid();}

    inline bool operator!()
        {return !is_valid();}

};

/**
 * Secure socket buffer.  This is used to create ssl socket connections
 * for both clients and servers.  The use depends in part on the type of
 * context created and passed at construction time.  If no context is
 * passed (NULL), then this reverts to TCPBuffer behavior.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __SHARED SSLBuffer : public TCPBuffer
{
protected:
    secure::session_t ssl;
    secure::bufio_t bio;
    bool server;
    bool verify;

public:
    SSLBuffer(secure::client_t context);
    SSLBuffer(const TCPServer *server, secure::server_t context, size_t size = 536);
    ~SSLBuffer();

    /**
     * Connect a ssl client session to a specific host uri.  If the socket
     * was already connected, it is automatically closed first.
     * @param host we are connecting to.
     * @param service to connect to.
     * @param size of buffer and tcp fragments.
     */
    void open(const char *host, const char *service, size_t size = 536);

    void close(void);

    void release(void);

    size_t _push(const char *address, size_t size);

    size_t _pull(char *address, size_t size);

    bool _flush(void);

    bool _pending(void);

    inline bool is_secure(void)
        {return bio != NULL;};
};

/**
 * A generic data ciphering class.  This is used to construct cryptographic
 * ciphers to encode and decode data as needed.  The cipher type is specified
 * by the key object.  This class can be used to send output streaming to
 * memory or in a fixed size buffer.  If the latter is used, a push() method
 * is called through a virtual when the buffer is full.  Since block ciphers
 * are used, buffers should be aligned to the block size.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __SHARED Cipher
{
public:
    typedef enum {ENCRYPT = 1, DECRYPT = 0} mode_t;

    /**
     * Cipher key formed by hash algorithm.  This can generate both a
     * key and iv table based on the algorithms used and required.  Normally
     * it is used from a pass-phrase, though any block of data may be
     * supplied.
     * @author David Sugar <dyfet@gnutelephony.org>
     */
    class __SHARED Key
    {
    protected:
        friend class Cipher;

        union {
            const void *algotype;
            int algoid;
        };

        union {
            const void *hashtype;
            int hashid;
        };

        int modeid;

        // assume 512 bit cipher keys possible...
        unsigned char keybuf[MAX_CIPHER_KEYSIZE / 8], ivbuf[MAX_CIPHER_KEYSIZE / 8];

        // generated keysize
        size_t keysize, blksize;

        Key(const char *cipher);
        Key();

        void set(const char *cipher);

        void set(const char *cipher, const char *digest);

        void assign(const char *key, size_t size, const unsigned char *salt, unsigned rounds);

    public:
        Key(const char *cipher, const char *digest, const char *text, size_t size = 0, const unsigned char *salt = NULL, unsigned rounds = 1);

        Key(const char *cipher, const char *digest);

        ~Key();

        void assign(const char *key, size_t size = 0);

        void clear(void);

        inline size_t size(void)
            {return keysize;};

        inline size_t iosize(void)
            {return blksize;};

        inline operator bool()
            {return keysize > 0;};

        inline bool operator!()
            {return keysize == 0;};

        inline Key& operator=(const char *pass)
            {assign(pass); return *this;};

        static void options(const unsigned char *salt = NULL, unsigned rounds = 1);
    };

    typedef Key *key_t;

private:
    Key keys;
    size_t bufsize, bufpos;
    mode_t bufmode;
    unsigned char *bufaddr;
    void *context;

protected:
    virtual void push(unsigned char *address, size_t size);

    void release(void);

public:
    Cipher();

    Cipher(key_t key, mode_t mode, unsigned char *address = NULL, size_t size = 0);

    virtual ~Cipher();

    void set(unsigned char *address, size_t size = 0);

    void set(key_t key, mode_t mode, unsigned char *address, size_t size = 0);

    /**
     * Push a final cipher block.  This is used to push the final buffer into
     * the push method for any remaining data.
     */
    size_t flush(void);

    /**
     * Process cipher data.  This requires the size to be a multiple of the
     * cipher block size.  If an unaligned sized block of data is used, it
     * will be ignored and the size returned will be 0.
     * @param data to process.
     * @param size of data to process.
     * @return size of processed output, should be same as size or 0 if error.
     */
    size_t put(const unsigned char *data, size_t size);

    /**
     * This essentially encrypts a single string and pads with NULL bytes
     * as needed.
     * @param string to encrypt.
     * @return total encrypted size.
     */
    size_t puts(const char *string);

    /**
     * This is used to process any data unaligned to the blocksize at the end
     * of a cipher session.  On an encryption, it will add padding or an
     * entire padding block with the number of bytes to strip.  On decryption
     * it will remove padding at the end.  The pkcs5 method of padding with
     * removal count is used.  This also sets the address buffer to NULL
     * to prevent further puts until reset.
     * @param address of data to add before final pad.
     * @param size of data to add before final pad.
     * @return actual bytes encrypted or decrypted.
     */
    size_t pad(const unsigned char *address, size_t size);

    /**
     * Process encrypted data in-place.  This assumes no need to set the
     * address buffer.
     * @param address of data to process.
     * @param size of data to process.
     * @param flag if to pad data.
     * @return bytes processed and written back to buffer.
     */
    size_t process(unsigned char *address, size_t size, bool flag = false);

    inline size_t size(void)
        {return bufsize;};

    inline size_t pos(void)
        {return bufpos;};

    inline size_t align(void)
        {return keys.iosize();};

    /**
     * Check if a specific cipher is supported.
     * @param name of cipher to check.
     * @return true if supported, false if not.
     */
    static bool has(const char *name);
};

/**
 * A cryptographic digest class.  This class can support md5 digests, sha1,
 * sha256, etc, depending on what the underlying library supports.  The
 * hash class accumulates the hash in the object.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __SHARED Digest
{
private:
    void *context;

    union {
        const void *hashtype;
        int hashid;
    };

    unsigned bufsize;
    unsigned char buffer[MAX_DIGEST_HASHSIZE / 8];
    char textbuf[MAX_DIGEST_HASHSIZE / 8 + 1];

protected:
    void release(void);

public:
    Digest(const char *type);

    Digest();

    ~Digest();

    inline bool puts(const char *str)
        {return put(str, strlen(str));};

    inline Digest &operator<<(const char *str)
        {puts(str); return *this;}

    inline Digest &operator<<(int16_t value)
        {int16_t v = htons(value); put(&v, 2); return *this;}

    inline Digest &operator<<(int32_t value)
        {int32_t v = htonl(value); put(&v, 4); return *this;}

    inline Digest &operator<<(const PrintProtocol& p)
        {const char *cp = p._print(); if(cp) puts(cp); return *this;}

    bool put(const void *memory, size_t size);

    inline unsigned size() const
        {return bufsize;};

    const unsigned char *get(void);

    const char *c_str(void);

    inline String str(void)
        {return String(c_str());};

    inline operator String()
        {return String(c_str());};

    void set(const char *id);

    inline void operator=(const char *id)
        {set(id);};

    inline bool operator *=(const char *text)
        {return puts(text);};

    inline bool operator +=(const char *text)
        {return puts(text);};

    inline const char *operator*()
        {return c_str();};

    inline bool operator!() const
        {return !bufsize && context == NULL;};

    inline operator bool() const
        {return bufsize > 0 || context != NULL;};

    /**
     * Finalize and recycle current digest to start a new
     * digest.
     * @param binary digest used rather than text if true.
     */
    void recycle(bool binary = false);

    /**
     * Reset and restart digest object.
     */
    void reset(void);

    /**
     * Test to see if a specific digest type is supported.
     * @param name of digest we want to check.
     * @return true if supported, false if not.
     */
    static bool has(const char *name);

    static void uuid(char *string, const char *name, const unsigned char *ns = NULL);

    static String uuid(const char *name, const unsigned char *ns = NULL);
};

/**
 * A cryptographic message authentication code class.  This class can support 
 * md5 digests, sha1, sha256, etc, depending on what the underlying library 
 * supports.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __SHARED HMAC
{
private:
    void *context;

    union {
        const void *hmactype;
        int hmacid;
    };

    unsigned bufsize;
    unsigned char buffer[MAX_DIGEST_HASHSIZE / 8];
    char textbuf[MAX_DIGEST_HASHSIZE / 8 + 1];

protected:
    void release(void);

public:
    HMAC(const char *digest, const char *key, size_t keylen = 0);

    HMAC();

    ~HMAC();

    inline bool puts(const char *str)
        {return put(str, strlen(str));};

    inline HMAC &operator<<(const char *str)
        {puts(str); return *this;}

    inline HMAC &operator<<(int16_t value)
        {int16_t v = htons(value); put(&v, 2); return *this;}

    inline HMAC &operator<<(int32_t value)
        {int32_t v = htonl(value); put(&v, 4); return *this;}

    inline HMAC &operator<<(const PrintProtocol& p)
        {const char *cp = p._print(); if(cp) puts(cp); return *this;}

    bool put(const void *memory, size_t size);

    inline unsigned size() const
        {return bufsize;};

    const unsigned char *get(void);

    const char *c_str(void);

    inline String str(void)
        {return String(c_str());};

    inline operator String()
        {return String(c_str());};

    void set(const char *digest, const char *key, size_t len);

    inline bool operator *=(const char *text)
        {return puts(text);};

    inline bool operator +=(const char *text)
        {return puts(text);};

    inline const char *operator*()
        {return c_str();};

    inline bool operator!() const
        {return !bufsize && context == NULL;};

    inline operator bool() const
        {return bufsize > 0 || context != NULL;};

    /**
     * Test to see if a specific digest type is supported.
     * @param name of digest we want to check.
     * @return true if supported, false if not.
     */
    static bool has(const char *name);
};

/**
 * Cryptographically relevant random numbers.  This is used both to gather
 * entropy pools and pseudo-random values.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __SHARED Random
{
public:
    /**
     * Push entropic seed.
     * @param buffer of random data to push.
     * @param size of buffer.
     * @return true if successful.
     */
    static bool seed(const unsigned char *buffer, size_t size);

    /**
     * Re-seed pseudo-random generation and entropy pools.
     */
    static void seed(void);

    /**
     * Get high-entropy random data.  This is often used to
     * initialize keys.  This operation may block if there is
     * insufficient entropy immediately available.
     * @param memory buffer to fill.
     * @param size of buffer.
     * @return number of bytes filled.
     */
    static size_t key(unsigned char *memory, size_t size);

    /**
     * Fill memory with pseudo-random values.  This is used
     * as the basis for all get and real operations and does
     * not depend on seed entropy.
     * @param memory buffer to fill.
     * @param size of buffer to fill.
     * @return number of bytes set.
     */
    static size_t fill(unsigned char *memory, size_t size);

    /**
     * Get a pseudo-random integer, range 0 - 32767.
     * @return random integer.
     */
    static int get(void);

    /**
     * Get a pseudo-random integer in a preset range.
     * @param min value of random integer.
     * @param max value of random integer.
     * @return random value from min to max.
     */
    static int get(int min, int max);

    /**
     * Get a pseudo-random floating point value.
     * @return psudo-random value 0 to 1.
     */
    static double real(void);

    /**
     * Get a pseudo-random floating point value in a preset range.
     * @param min value of random floating point number.
     * @param max value of random floating point number.
     * @return random value from min to max.
     */
    static double real(double min, double max);

    /**
     * Determine if we have sufficient entropy to return random
     * values.
     * @return true if sufficient entropy.
     */
    static bool status(void);

    /**
     * Create 36 character random uuid string.
     * @param string to write uuid into, must be 37 bytes or more.
     */
    static void uuid(char *string);

    static String uuid(void);
};

/**
 * Convenience type for secure socket.
 */
typedef SSLBuffer ssl_t;

/**
 * Convenience type for generic digests.
 */
typedef Digest digest_t;

/**
 * Convenience type for generic digests.
 */
typedef HMAC hmac_t;

/**
 * Convenience type for generic ciphers.
 */
typedef Cipher cipher_t;

/**
 * Convenience type for generic cipher key.
 */
typedef Cipher::Key skey_t;

inline void zerofill(void *addr, size_t size)
{
    ::memset(addr, 0, size);
}

#if defined(OLD_STDCPP) || defined(NEW_STDCPP)

/**
 * Secure socket using std::iostream.  This class is similar to SSLBuffer
 * but uses the libstdc++ library to stream i/o.  Being based on tcpstream,
 * it also inherits the character protocol.  Like SSLBuffer, if no context
 * is given or the handshake fails, then the stream defaults to insecure TCP
 * connection behavior.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __SHARED sstream : public tcpstream
{
protected:
    secure::session_t ssl;
    secure::bufio_t bio;
    bool server;
    bool verify;

private:
    // kill copy constructor
    sstream(const sstream&);

public:
    sstream(secure::client_t context);
    sstream(const TCPServer *server, secure::server_t context, size_t size = 536);
    ~sstream();

    void open(const char *host, const char *service, size_t size = 536);

    void close(void);

    int sync();

    void release(void);

    ssize_t _write(const char *address, size_t size);

    ssize_t _read(char *address, size_t size);

    bool _wait(void);

    inline void flush(void)
        {sync();}

    inline bool is_secure(void)
        {return bio != NULL;}
};

/**
 * A template to create a string array that automatically erases.
 * This is a mini string/stringbuf class that supports a subset of
 * functionality but does not require a complex supporting object.  Like
 * stringbuf, this can be used to create local string variables.  When
 * the object falls out of scope it's memory is reset.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<size_t S>
class keystring
{
private:
    char buffer[S];

    /**
     * Disable copy constructor.
     */
    inline keystring(const keystring& copy) {};

public:
    /**
     * Create a new character buffer with an empty string.
     */
    inline keystring()
        {buffer[0] = 0;}

    /**
     * Create a character buffer with assigned text.  If the text is
     * larger than the size of the object, it is truncated.
     * @param text to assign.
     */
    inline keystring(const char *text)
        {String::set(buffer, S, text);}

    /**
     * Clear memory when destroyed.
     */
    inline ~keystring()
        {memset(buffer, 0, S);}

    /**
     * Clear current key memory.
     */
    inline void clear(void)
        {memset(buffer, 0, S);}

    /**
     * Assign null terminated text to the object.
     * @param text to assign.
     */
    inline void operator=(const char *text)
        {String::set(buffer, S, text);}

    /**
     * Concatenate text into the object.  If the text is larger than the
     * size of the object, then it is truncated.
     * @param text to append.
     */
    inline void operator+=(const char *text)
        {String::add(buffer, S, text);}

    /**
     * Test if data is contained in the object.
     * @return true if there is text.
     */
    inline operator bool() const
        {return buffer[0];}

    /**
     * Test if the object is empty.
     * @return true if the object is empty.
     */
    inline bool operator!() const
        {return buffer[0] == 0;}

    /**
     * Get text by casting reference.
     * @return pointer to text in object.
     */
    inline operator char *()
        {return buffer;}

    /**
     * Get text by object pointer reference.
     * @return pointer to text in object.
     */
    inline char *operator*()
        {return buffer;}

    /**
     * Array operator to get a character from the object.
     * @param offset of character in string buffer.
     * @return character at offset.
     */
    inline char& operator[](size_t offset) const
        {return buffer[offset];}

    /**
     * Get a pointer to an offset in the object by expression operator.
     * @param offset of character in string buffer.
     * @return pointer to offset in object.
     */
    inline char *operator()(size_t offset)
        {return buffer + offset;}

    /**
     * Get allocated size of the object.
     * @return allocated size.
     */
    inline size_t size(void) const
        {return S;}

    /**
     * Get current length of string.
     * @return length of string.
     */
    inline size_t len(void) const
        {return strlen(buffer);}
};

/**
 * A template to create a random generated key of specified size.  The
 * key memory is cleared when the object is destroyed.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template<size_t S>
class keyrandom
{
private:
    unsigned char buffer[S];

    /**
     * Disable copy constructor.
     */
    inline keyrandom(const keyrandom& copy) {};

public:
    /**
     * Create a new character buffer with an empty string.
     */
    inline keyrandom()
        {Random::key(buffer, S);}

    /**
     * Clear memory when destroyed.
     */
    inline ~keyrandom()
        {memset(buffer, 0, S);}

    /**
     * Update with new random key.
     */
    inline void update(void)
        {Random::key(buffer, S);}

    /**
     * Clear current key memory.
     */
    inline void clear(void)
        {memset(buffer, 0, S);}

    /**
     * Get text by casting reference.
     * @return pointer to text in object.
     */
    inline operator unsigned char *()
        {return buffer;}

    /**
     * Get text by object pointer reference.
     * @return pointer to text in object.
     */
    inline unsigned char *operator*()
        {return buffer;}

    /**
     * Get allocated size of the object.
     * @return allocated size.
     */
    inline size_t size(void) const
        {return S;}
};


#endif

END_NAMESPACE

#endif
