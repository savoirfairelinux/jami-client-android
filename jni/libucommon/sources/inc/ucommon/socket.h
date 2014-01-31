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
 * Common socket class and address manipulation.
 * This offers a common socket base class that exposes socket functionality
 * based on what the target platform supports.  Support for multicast, IPV6
 * addressing, and manipulation of cidr policies are all supported here.
 * @file ucommon/socket.h
 */

#ifndef _UCOMMON_SOCKET_H_
#define _UCOMMON_SOCKET_H_

#ifndef _UCOMMON_TIMERS_H_
#include <ucommon/timers.h>
#endif

#ifndef _UCOMMON_LINKED_H_
#include <ucommon/linked.h>
#endif

#ifndef _UCOMMON_STRING_H_
#include <ucommon/string.h>
#endif

extern "C" {
    struct addrinfo;
}

#ifdef  _MSWINDOWS_
#define SHUT_RDWR   SD_BOTH
#define SHUT_WR     SD_SEND
#define SHUT_RD     SD_RECV
#else
#include <unistd.h>
#include <sys/socket.h>
#include <net/if.h>
#include <netinet/in.h>
#include <netdb.h>
#endif

#include <errno.h>
#include <stdio.h>

#ifndef IPTOS_LOWDELAY
#define IPTOS_LOWDELAY      0x10
#define IPTOS_THROUGHPUT    0x08
#define IPTOS_RELIABILITY   0x04
#define IPTOS_MINCOST       0x02
#endif

#ifdef  AF_UNSPEC
#define DEFAULT_FAMILY  AF_UNSPEC
#else
#define DEFAULT_FAMILY  AF_INET
#endif

struct sockaddr_internet;

typedef struct sockaddr *sockaddr_t;

typedef struct sockaddr sockaddr_struct;    // older gcc needs...?

/**
 * An object that holds ipv4 or ipv6 binary encoded host addresses.
 */
typedef struct hostaddr_internet
{
    union
    {
        struct in_addr ipv4;
#ifdef  AF_INET6
        struct in6_addr ipv6;
#endif
    };
}   inethostaddr_t;

#if defined(AF_INET6) || defined(__CYGWIN__)
/**
 * An object that can hold a ipv4 or ipv6 socket address.  This would be
 * used for tcpip socket connections.  We do not use sockaddr_storage
 * because it is not present in pre ipv6 stacks, and because the storage
 * size also includes the size of the path of a unix domain socket on
 * posix systems.
 */
typedef struct sockaddr_internet
{
    union {
#ifdef  AF_INET6
        struct sockaddr_in6 ipv6;
#endif
        struct sockaddr_in ipv4;
        struct sockaddr address;
    };
} inetsockaddr_t;
#else
typedef struct sockaddr_internet
{
    union {
        struct sockaddr_in ipv4;
        struct sockaddr address;
    };
} inetsockaddr_t;

struct sockaddr_storage
{
#ifdef  AF_UNIX
    char sa_data[128];
#else
    char sa_data[sizeof(struct sockaddr_in)];
#endif
};
#endif

#ifndef SOCK_DCCP
#define SOCK_DCCP       6
#endif

#ifndef IPPROTO_DCCP
#define IPPROTO_DCCP    23
#endif

#ifndef SOL_DCCP
#define SOL_DCCP        269
#endif

#define DCCP_SOCKOPT_AVAILABLE_CCIDS    12
#define DCCP_SOCKOPT_CCID               13
#define DCCP_SOCKOPT_TX_CCID            14
#define DCCP_SOCKOPT_RX_CCID            15

NAMESPACE_UCOMMON

/**
 * A class to hold internet segment routing rules.  This class can be used
 * to provide a stand-alone representation of a cidr block of internet
 * addresses or chained together into some form of access control list.  The
 * cidr class can hold segments for both IPV4 and IPV6 addresses.  The class
 * accepts cidr's defined as C strings, typically in the form of address/bits
 * or address/submask.  These routines auto-detect ipv4 and ipv6 addresses.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT cidr : public LinkedObject
{
protected:
    int Family;
    inethostaddr_t Netmask, Network;
    char Name[16];

    unsigned mask(const char *cp) const;

    inethostaddr_t broadcast(void) const;

    unsigned mask(void) const;

public:
    /**
     * A convenience type for using a pointer to a linked list as a policy chain.
     */
    typedef LinkedObject policy;

    /**
     * Create an uninitialized cidr.
     */
    cidr();

    /**
     * Create an unlinked cidr from a string.  The string is typically in
     * the form base-host-address/range, where range might be a bit count
     * or a network mask.
     * @param string for cidr block.
     */
    cidr(const char *string);

    /**
     * Create an unnamed cidr entry on a specified policy chain.
     * @param policy chain to link cidr to.
     * @param string for cidr block.
     */
    cidr(policy **policy, const char *string);

    /**
     * Create a named cidr entry on a specified policy chain.
     * @param policy chain to link cidr to.
     * @param string for cidr block.
     * @param name of this policy object.
     */
    cidr(policy **policy, const char *string, const char *name);

    /**
     * Construct a copy of an existing cidr.
     * @param existing cidr we copy from.
     */
    cidr(const cidr& existing);

    /**
     * Find the smallest cidr entry in a list that matches the socket address.
     * @param policy chain to search.
     * @param address to search for.
     * @return smallest cidr or NULL if none match.
     */
    static const cidr *find(const policy *policy, const struct sockaddr *address);

    /**
     * Get the largest container cidr entry in a list that matches the
     * socket address.
     * @param policy chain to search.
     * @param address to search for.
     * @return largest cidr or NULL if none match.
     */
    static const cidr *container(const policy *policy, const struct sockaddr *address);

    /**
     * Get the saved name of our cidr.  This is typically used with find
     * when the same policy name might be associated with multiple non-
     * overlapping cidr blocks.  A typical use might to have a cidr
     * block like 127/8 named "localdomain", as well as the ipv6 "::1".
     * @return name of cidr.
     */
    inline const char *getName(void) const
        {return Name;};

    /**
     * Get the address family of our cidr block object.
     * @return family of our cidr.
     */
    inline int getFamily(void) const
        {return Family;};

    /**
     * Get the network host base address of our cidr block.
     * @return binary network host address.
     */
    inline inethostaddr_t getNetwork(void) const
        {return Network;};

    /**
     * Get the effective network mask for our cidr block.
     * @return binary network mask for our cidr.
     */
    inline inethostaddr_t getNetmask(void) const
        {return Netmask;};

    /**
     * Get the broadcast host address represented by our cidr.
     * @return binary broadcast host address.
     */
    inline inethostaddr_t getBroadcast(void) const
        {return broadcast();}

    /**
     * Get the number of bits in the cidr bitmask.
     * @return bit mask of cidr.
     */
    inline unsigned getMask(void) const
        {return mask();}

    /**
     * Set our cidr to a string address.  Replaces prior value.
     * @param string to set for cidr.
     */
    void set(const char *string);

    /**
     * Test if a given socket address falls within this cidr.
     * @param address of socket to test.
     * @return true if address is within cidr.
     */
    bool is_member(const struct sockaddr *address) const;

    /**
     * Test if a given socket address falls within this cidr.
     * @param address of socket to test.
     * @return true if address is within cidr.
     */
    inline bool operator==(const struct sockaddr *address) const
        {return is_member(address);};

    /**
     * Test if a given socket address falls outside this cidr.
     * @param address of socket to test.
     * @return true if address is outside cidr.
     */
    inline bool operator!=(const struct sockaddr *address) const
        {return !is_member(address);};
};

/**
 * A generic socket base class.  This class can be used directly or as a
 * base class for building network protocol stacks.  This common base tries
 * to handle UDP and TCP sockets, as well as support multicast, IPV4/IPV6
 * addressing, and additional addressing domains (such as Unix domain sockets).
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT Socket
{
protected:
    socket_t so;
    int ioerr;
    timeout_t iowait;

public:
    /**
     * Get an address list directly.  This is used internally by some derived
     * socket types when generic address lists would be invalid.
     * @param host name in the form address or "address:port"
     * @param service id or port to use if not specified in host string.
     * @param type of service to get.
     * @param protocol of service to get.
     */
    static struct addrinfo *query(const char *host, const char *service, int type = SOCK_STREAM, int protocol = 0);

    /**
     * Release an address list directly.  This is used internally by some
     * derived socket types which do not use generic address lists.
     * @param list of addresses.
     */
    static void release(struct addrinfo *list);

    /**
     * A generic socket address class.  This class uses the addrinfo list
     * to store socket multiple addresses in a protocol and family
     * independent manner.  Hence, this address class can be used for ipv4
     * and ipv6 sockets, for assigning connections to multiple hosts, etc.
     * The address class will call the resolver when passed host names.
     * @author David Sugar <dyfet@gnutelephony.org>
     */
    class __EXPORT address
    {
    protected:
        struct addrinfo *list;

    public:
        /**
         * Construct a socket address.  This is used to get an address to
         * bind a socket interface to.  The address can be specified as the
         * ip address of the interface or as a "hostname".  If a hostname
         * is used, then family should be specified for clarity.
         * @param family of socket address.  Needed when host names are used.
         * @param address or hostname.
         * @param type of socket (stream, dgram, etc).
         * @param protocol number of socket.
         */
        address(int family, const char *address, int type = SOCK_STREAM, int protocol = 0);

        /**
         * Construct a socket address for an existing socket.  This can be the
         * name of a host or to perform a lookup in a domain for a service.
         * Family can be used to restrict the set of results returned, however
         * since things like connecto() already filter by family and create
         * will use family from the addrinfo, in most cases AF_UNSPEC can be
         * used.  This may be depreciated in favor of the constructor that
         * matches a set() method.
         * @param family of hosts to filter by or AF_UNSPEC.
         * @param hostname or ip address.  The socket family is used for hostnames.
         * @param service port or name we are referencing or NULL.
         */
        address(int family, const char *hostname, const char *service = NULL);

        /**
         * Construct a socket address list for a service.
         * @param host address for service.
         * @param service name or port number.
         * @param type of service, stream, dgram, etc.
         */
        address(const char *host, const char *service, int type = SOCK_STREAM);

        /**
         * Construct a socket address from host and service.  This is primarily
         * used to construct a list of potential service connections by pure
         * port numbers or for host lookup only.
         * @param hostname or address to use.
         * @param service port or 0.
         */
        address(const char *hostname, unsigned service = 0);

        /**
         * Construct an empty address.
         */
        address();

        /**
         * Copy constructor.
         * @param reference to object to copy from.
         */
        address(const address& reference);

        /**
         * Destroy address.  Deallocate addrinfo structure.
         */
        ~address();

        /**
         * Get the first socket address in our address list.
         * @return first socket address or NULL if none.
         */
        struct sockaddr *get(void) const;

        inline struct sockaddr *getAddr(void) const
            {return get();}

        inline struct sockaddr *operator()(void) const
            {return get();}

        /**
         * Get the first socket address by casted reference.
         * @return first socket address we resolved or NULL if none.
         */
        inline operator struct sockaddr *() const
            {return get();};

        /**
         * Get the first socket address of specified family from our list.
         * @param family to seek.
         * @return first socket address of family or NULL if none.
         */
        struct sockaddr *get(int family) const;

        inline struct sockaddr *operator()(int family) const
            {return get(family);}

        inline operator struct sockaddr_in *() const
            {return (struct sockaddr_in *)get(AF_INET);}

#ifdef  AF_INET6
        inline operator struct sockaddr_in6 *() const
            {return (struct sockaddr_in6 *)get(AF_INET6);}
#endif

        /**
         * Get the family of the first member in a list of services.
         * @return family of first socket address or 0 if none.
         */
        int family(void) const;

        /**
         * Find a specific socket address in our address list.
         * @return matching address from list or NULL if not found.
         */
        struct sockaddr *find(const struct sockaddr *addr) const;

        /**
         * Get the full socket address list from the object.
         * @return addrinfo list we resolved or NULL if none.
         */
        inline struct addrinfo *getList(void) const
            {return list;};

        /**
         * Get the full socket address list by casted reference.
         * @return addrinfo list we resolved or NULL if none.
         */
        inline operator struct addrinfo *() const
            {return list;};

        /**
         * Return the full socket address list by pointer reference.
         * @return addrinfo list we resolved or NULL if none.
         */
        inline struct addrinfo *operator*() const
            {return list;};

        /**
         * Test if the address list is valid.
         * @return true if we have an address list.
         */
        inline operator bool() const
            {return list != NULL;};

        /**
         * Test if we have no address list.
         * @return true if we have no address list.
         */
        inline bool operator!() const
            {return list == NULL;};


        /**
         * Clear current object.
         */
        void clear(void);

        /**
         * Set the host addresses to form a new list.
         * @param hostname or address to resolve.
         * @param service name or port number, or NULL if not used.
         * @param type of socket (stream or dgram) to filter list by.
         */
        void set(const char *hostname, const char *service = NULL, int type = SOCK_STREAM);

        /**
         * Append additional host addresses to our list.
         * @param hostname or address to resolve.
         * @param service name or port number, or NULL if not used.
         * @param type of socket (stream or dgram).
         */
        void add(const char *hostname, const char *service = NULL, int type = SOCK_STREAM);

        /**
         * Set an entry for host binding.
         * @param family of socket address.  Needed when hostnames are used.
         * @param address or hostname.
         * @param type of socket (stream, dgram, etc).
         * @param protocol number of socket.
         */
        void set(int family, const char *address, int type = SOCK_STREAM, int protocol = 0);

        /**
         * Add an individual socket address to our address list.
         * @param address to add.
         */
        void add(sockaddr *address);

        /**
         * Insert unique members from another socket address list to ours.
         * @param address list to insert into list.
         * @return count of addresses added.
         */
        unsigned insert(struct addrinfo *address);

        /**
         * Remove members from another socket address list from ours.
         * @param address list to remove from list.
         * @return count of addresses removed.
         */
        unsigned remove(struct addrinfo *address);

        /**
         * Remove an individual socket address from our address list.
         * @param address to remove.
         * @return true if found and removed, false if not found.
         */
        bool remove(struct sockaddr *address);

        /**
         * Insert an individual socket address to our address list only if
         * unique.
         * @param address to insert into list.
         * @return true if inserted, false if duplicate.
         */
        bool insert(struct sockaddr *address);

        /**
         * Copy an existing addrinfo into our object.  This is also used
         * to support the copy constructor.
         * @param address list to copy from.
         */
        void copy(const struct addrinfo *address);

        /**
         * Set an individual socket address for our address list.
         * @param address to add.
         */
        void set(struct sockaddr *address);

        /**
         * Set a socket address from host and service.
         * @param hostname or address to use.
         * @param service port or 0.
         */
        void set(const char *hostname, unsigned service = 0);

        /**
         * Duplicate a socket address.
         * @param address to duplicate.
         * @return duplicate address object.
         */
        static struct sockaddr *dup(struct sockaddr *address);

        /**
         * Convert address object into ipv4 address.
         * @param address to convert.
         * @return new ipv4 address or NULL if not ipv4.
         */
        static struct sockaddr_in *ipv4(struct sockaddr *address);

#ifdef  AF_INET6
        /**
         * Convert address object into ipv6 address.
         * @param address to convert.
         * @return new ipv6 address or NULL if not ipv6.
         */
        static struct sockaddr_in6 *ipv6(struct sockaddr *address);
#endif
    };

    friend class address;

    /**
     * Create a socket object for use.
     */
    Socket();

    /**
     * Create socket as duped handle of existing socket.
     * @param existing socket to dup.
     */
    Socket(const Socket& existing);

    /**
     * Create socket from existing socket descriptor.
     * @param socket descriptor to use.
     */
    Socket(socket_t socket);

    /**
     * Create and connect a socket to an address from an address list.  The
     * type of socket created is based on the type we are connecting to.
     * @param address list to connect with.
     */
    Socket(struct addrinfo *address);

    /**
     * Create an unbound socket of a specific type.
     * @param family of our new socket.
     * @param type (stream, udp, etc) of our new socket.
     * @param protocol number of our new socket.'
     */
    Socket(int family, int type, int protocol = 0);

    /**
     * Create a bound socket.  If one wishes to listen for connections on
     * a protocol, then ListenSocket should be used instead.
     * @param address to bind or "*" for all.
     * @param port number of service to bind.
     * @param family to bind as.
     * @param type of socket to bind (stream, udp, etc).
     * @param protocol of socket to bind.
     */
    Socket(const char *address, const char *port, int family = AF_UNSPEC, int type = 0, int protocol = 0);

    /**
     * Shutdown, close, and destroy socket.
     */
    virtual ~Socket();

    /**
     * Cancel pending i/o by shutting down the socket.
     */
    void cancel(void);

    /**
     * Cancel pending i/o by shutting down the socket.
     * @param socket to shutdown.
     */
    static void cancel(socket_t socket);

    /**
     * Shutdown and close the socket.
     */
    void release(void);

    /**
     * Get error code.
     */
    inline int err(void) const
        {return ioerr;}

    /**
     * See the number of bytes in the receive queue.
     * @param value to test for.
     * @return true if at least that many bytes waiting in receive queue.
     */
    bool is_pending(unsigned value) const;

    /**
     * Test if socket is connected.
     * @return true if connected.
     */
    bool connected(void) const;

    /**
     * Test for pending input data.  This function can wait up to a specified
     * timeout for data to appear.
     * @param timeout or 0 if none.
     * @return true if input data waiting.
     */
    bool wait(timeout_t timeout = 0) const;

    /**
     * Set nodelay option for tcp socket.
     * @return 0 if successful, -1 on error.
     */
    inline int nodelay(void) const
        {return nodelay(so);};

    /**
     * Test for pending input data.  This function can wait up to a specified
     * timeout for data to appear.
     * @param socket to test.
     * @param timeout or 0 if none.
     * @return true if input data waiting.
     */
    static bool wait(socket_t socket, timeout_t timeout = 0);

    /**
     * Test for output data sent.  This function can wait up to a specified
     * timeout for data to appear sent.
     * @param timeout or 0 if none.
     * @return false if cannot send more output/out of buffer space.
     */
    bool waitSending(timeout_t timeout = 0) const;

    /**
     * Get the number of bytes of data in the socket receive buffer.
     * @return bytes pending.
     */
    inline unsigned pending(void) const
        {return pending(so);};

    /**
     * Set socket for unicast mode broadcasts.
     * @param enable broadcasting if true.
     * @return 0 on success, -1 if error.
     */
    inline int broadcast(bool enable)
        {return broadcast(so, enable);};

    /**
     * Set socket for keepalive packets.
     * @param enable keep-alive if true.
     * @return 0 on success, -1 if error.
     */
    inline int keepalive(bool enable)
        {return keepalive(so, enable);};

    /**
     * Set socket blocking I/O mode.
     * @param enable true for blocking I/O.
     * @return 0 on success, -1 if error.
     */
    inline int blocking(bool enable)
        {return blocking(so, enable);};

    /**
     * Set multicast mode and multicast broadcast range.
     * @param ttl to set for multicast socket or 0 to disable multicast.
     * @return 0 on success, -1 if error.
     */
    inline int multicast(unsigned ttl = 1)
        {return multicast(so, ttl);};

    /**
     * Set loopback to read multicast packets we broadcast.
     * @param enable true to loopback, false to ignore.
     * @return 0 on success, -1 if error.
     */
    inline int loopback(bool enable)
        {return loopback(so, enable);};

    /**
     * Get socket error code.
     * @return socket error code.
     */
    inline int getError(void)
        {return error(so);};

    /**
     * Set the time to live before packets expire.
     * @param time to live to set.
     * @return 0 on success, -1 on error.
     */
    inline int ttl(unsigned char time)
        {return ttl(so, time);};

    /**
     * Set the size of the socket send buffer.
     * @param size of send buffer to set.
     * @return 0 on success, -1 on error.
     */
    inline int sendsize(unsigned size)
        {return sendsize(so, size);};

    /**
     * Set the size to wait before sending.
     * @param size of send wait buffer to set.
     * @return 0 on success, -1 on error.
     */
    inline int sendwait(unsigned size)
        {return sendwait(so, size);};


    /**
     * Set the size of the socket receive buffer.
     * @param size of recv buffer to set.
     * @return 0 on success, -1 on error.
     */
    inline int recvsize(unsigned size)
        {return recvsize(so, size);};

    /**
     * Get the type of a socket.
     * @param socket descriptor.
     * @return socket type.
     */
    static int type(socket_t socket);

    /**
     * Set segment size and get MTU.
     * @param socket to modify.
     * @param size of segment or zero to not set.
     * @return mtu size of socket.
     */
    static unsigned segsize(socket_t socket, unsigned size = 0);

    /**
     * Set congestion control id.
     * @param socket to modify.
     * @param ccid value to set.
     * @return true if success, false if not dccp or not supported ccid used.
     */
    static bool ccid(socket_t socket, uint8_t id);

    /**
     * Get the type of a socket.
     * @return socket type.
     */
    inline int type(void)
        {return type(so);};

    /**
     * Set segment size and get mtu of a socket.
     * @param size of segment or 0 to leave unchanged.
     * @return mtu size.
     */
    inline unsigned segsize(unsigned size)
        {return segsize(so, size);};

    /**
     * Set ccid of dccp socket.
     * @param ccid to set.
     * @return true if success, false if not dccp or not supported ccid used.
     */
    inline bool ccid(uint8_t id)
        {return ccid(so, id);};

    /**
     * Set the type of service field of outgoing packets.  Some useful
     * values include IPTOS_LOWDELAY to minimize delay for interactive
     * traffic, IPTOS_THROUGHPUT to optimize throughput, OPTOS_RELIABILITY
     * to optimize for reliability, and IPTOS_MINCOST for low speed use.
     * @param type of service value.
     * @return 0 on success or -1 on error.
     */
    inline int tos(int type)
        {return tos(so, type);};

    /**
     * Set packet priority, 0 to 6 unless privileged.  Should be set before
     * type-of-service.
     * @param scheduling priority for packet scheduling.
     * @return 0 on success, -1 on error.
     */
    inline int priority(int scheduling)
        {return priority(so, scheduling);};

    /**
     * Shutdown the socket communication channel.
     */
    inline void shutdown(void)
        {::shutdown(so, SHUT_RDWR);};

    /**
     * Connect our socket to a remote host from an address list.
     * For TCP (and DCCP) sockets, the entire list may be tried.  For UDP,
     * connect is only a state and the first valid entry in the list is used.
     * @param list of addresses to connect to.
     * @return 0 on success or error.
     */
    int connectto(struct addrinfo *list);

    /**
     * Disconnect a connected socket.  Depending on the implementation, this
     * might be done by connecting to AF_UNSPEC, connecting to a 0 address,
     * or connecting to self.
     * @return 0 on success or error.
     */
    int disconnect(void);

    /**
     * Join socket to multicast group.
     * @param list of groups to join.
     * @return 0 on success, -1 on error.
     */
    int join(const struct addrinfo *list);

    /**
     * Drop socket from multicast group.
     * @param list of groups to drop.
     * @return 0 on success, -1 on error.
     */
    int drop(const struct addrinfo *list);

    /**
     * Socket i/o timer setting.
     * @param timeout to wait, inf for blocking, 0 pure non-blocking.
     * @return 0 on success or error code.
     */
    int wait(timeout_t timeout = Timer::inf);

    /**
     * Peek at data waiting in the socket receive buffer.
     * @param data pointer to save data in.
     * @param number of bytes to peek.
     * @return number of bytes actually read, or 0 if no data waiting.
     */
    size_t peek(void *data, size_t number) const;

    /**
     * Read data from the socket receive buffer.  This will be used in abi 4.
     * @param data pointer to save data in.
     * @param number of bytes to read.
     * @param address of peer data was received from.
     * @return number of bytes actually read, 0 if none, -1 if error.
     */
    size_t readfrom(void *data, size_t number, struct sockaddr_storage *address = NULL);

    /**
     * Write data to the socket send buffer.  This will be used in abi 4.
     * @param data pointer to write data from.
     * @param number of bytes to write.
     * @param address of peer to send data to if not connected.
     * @return number of bytes actually sent, 0 if none, -1 if error.
     */
    size_t writeto(const void *data, size_t number, const struct sockaddr *address = NULL);

    /**
     * Read a newline of text data from the socket and save in NULL terminated
     * string.  This uses an optimized I/O method that takes advantage of
     * socket peeking.  This presumes a connected socket on a streamble
     * protocol.  Because the trailing newline is dropped, the return size
     * may be greater than the string length.  If there was no data read
     * because of eof of data, an error has occured, or timeout without
     * input, then 0 will be returned.
     * @param data to save input line.
     * @param size of input line buffer.
     * @return number of bytes read, 0 if none, err() has error.
     */
    size_t readline(char *data, size_t size);

    /**
     * Print formatted string to socket.
     * @param format string.
     * @return number of bytes sent.
     */
    size_t printf(const char *format, ...) __PRINTF(2,3);

    /**
     * Read a string of input from the socket and strip trailing newline.
     * This uses an optimized I/O method that takes advantage of
     * socket peeking.  This presumes a connected socket on a streamble
     * protocol.  Because the trailing newline is dropped, the return size
     * may be greater than the string length.  If there was no data read
     * because of eof of data, an error has occured, or timeout without
     * input, then 0 will be returned.
     * @param buffer to save input line.
     * @return number of bytes read, 0 if none, err() has error.
     */
    size_t readline(String& buffer);

    /**
     * Read a newline of text data from the socket and save in NULL terminated
     * string.  This uses an optimized I/O method that takes advantage of
     * socket peeking.  As such, it has to be rewritten to be used in a ssl
     * layer socket.
     * @param socket to read from.
     * @param data to save input line.
     * @param size of input line buffer.
     * @param timeout to wait for a complete input line.
     * @return number of bytes read, 0 if none, -1 if error.
     */
    static ssize_t readline(socket_t socket, char *data, size_t size, timeout_t timeout = Timer::inf);

    /**
     * Print formatted string to socket.
     * @param socket to write to.
     * @param format string.
     * @return number of bytes sent, -1 if error.
     */
    static ssize_t printf(socket_t socket, const char *format, ...) __PRINTF(2,3);

    /**
     * Write a null terminated string to the socket.  This exists because
     * we messed up consistency with the original puts() method.  In the
     * future there will be a single puts() that has a NULL default.
     * @param string to write.
     * @return number of bytes sent, 0 if none, -1 if error.
     */
    size_t writes(const char *string);

    /**
     * Test if socket is valid.
     * @return true if valid socket.
     */
    operator bool();

    /**
     * Test if socket is invalid.
     * @return true if socket is invalid.
     */
    bool operator!() const;

    /**
     * Assign socket from a socket descriptor.  Release existing socket if
     * one present.
     * @param socket descriptor to assign to object.
     */
    Socket& operator=(socket_t socket);

    /**
     * Get the socket descriptor by casting.
     * @return socket descriptor of object.
     */
    inline operator socket_t() const
        {return so;};

    /**
     * Get the socket descriptor by pointer reference.
     * @return socket descriptor of object.
     */
    inline socket_t operator*() const
        {return so;};

    /**
     * Get the number of bytes pending in the receive buffer of a socket
     * descriptor.
     * @param socket descriptor.
     * @return number of pending bytes.
     */
    static unsigned pending(socket_t socket);

    /**
     * Set the send size of a socket descriptor.
     * @param socket descriptor.
     * @param size of send buffer to set.
     * @return 0 on success, -1 on error.
     */
    static int sendsize(socket_t socket, unsigned size);

    /**
     * Set the size to wait before sending.
     * @param socket descriptor.
     * @param size of send wait buffer to set.
     * @return 0 on success, -1 on error.
     */
    static int sendwait(socket_t socket, unsigned size);

    /**
     * Set the receive size of a socket descriptor.
     * @param socket descriptor.
     * @param size of receive buffer to set.
     * @return 0 on success, -1 on error.
     */
    static int recvsize(socket_t socket, unsigned size);

    /**
     * Connect socket descriptor to a remote host from an address list.
     * For TCP (and DCCP) sockets, the entire list may be tried.  For UDP,
     * connect is only a state and the first valid entry in the list is used.
     * @param socket descriptor.
     * @param list of addresses to connect to.
     * @return 0 on success, -1 on error.
     */
    static int connectto(socket_t socket, struct addrinfo *list);

    /**
     * Disconnect a connected socket descriptor.
     * @param socket descriptor.
     * @return 0 on success, -1 on error.
     */
    static int disconnect(socket_t socket);

    /**
     * Drop socket descriptor from multicast group.
     * @param socket descriptor.
     * @param list of groups to drop.
     * @return 0 on success, -1 on error.
     */
    static int drop(socket_t socket, const struct addrinfo *list);

    /**
     * Join socket descriptor to multicast group.
     * @param socket descriptor.
     * @param list of groups to join.
     * @return 0 on success, -1 on error.
     */
    static int join(socket_t socket, const struct addrinfo *list);

    /**
     * Get socket error code of socket descriptor.
     * @param socket descriptor.
     * @return socket error code.
     */
    static int error(socket_t socket);

    /**
     * Set multicast mode and multicast broadcast range for socket descriptor.
     * @param socket descriptor.
     * @param ttl to set for multicast socket or 0 to disable multicast.
     * @return 0 if success, -1 if error.
     */
    static int multicast(socket_t socket, unsigned ttl = 1);

    /**
     * Set loopback to read multicast packets socket descriptor broadcasts.
     * @param socket descriptor.
     * @param enable true to loopback, false to ignore.
     * @return 0 if success, -1 if error.
     */
    static int loopback(socket_t socket, bool enable);

    /**
     * Set socket blocking I/O mode of socket descriptor.
     * @param socket descriptor.
     * @param enable true for blocking I/O.
     * @return 0 if success, -1 if error.
     */
    static int blocking(socket_t socket, bool enable);

    /**
     * Set socket for keepalive packets for socket descriptor.
     * @param socket descriptor.
     * @param enable keep-alive if true.
     * @return 0 if success, -1 if error.
     */
    static int keepalive(socket_t socket, bool enable);

    /**
     * Set socket for unicast mode broadcasts on socket descriptor.
     * @param socket descriptor.
     * @param enable broadcasting if true.
     * @return 0 if success, -1 if error.
     */
    static int broadcast(socket_t socket, bool enable);

    /**
     * Set tcp nodelay option on socket descriptor.
     * @param socket descriptor.
     * @return 0 if success, -1 if error.
     */
    static int nodelay(socket_t socket);

    /**
     * Set packet priority of socket descriptor.
     * @param socket descriptor.
     * @param scheduling priority for packet scheduling.
     * @return 0 on success, -1 on error.
     */
    static int priority(socket_t socket, int scheduling);

    /**
     * Set type of service of socket descriptor.
     * @param socket descriptor.
     * @param type of service.
     * @return 0 on success, -1 on error.
     */
    static int tos(socket_t socket, int type);

    /**
     * Set the time to live for the socket descriptor.
     * @param socket descriptor.
     * @param time to live to set.
     * @return 0 on success, -1 on error.
     */
    static int ttl(socket_t socket, unsigned char time);

    /**
     * Get the address family of the socket descriptor.
     * @return address family.
     */
    static int family(socket_t socket);

    /**
     * Get the address family of a socket address object.
     * @param address to examine.
     * @return address family.
     */
    inline static int family(const struct sockaddr_storage& address)
        {return ((const struct sockaddr *)&address)->sa_family;};

    /**
     * Get the address family of an internet socket address object.
     * @param address to examine.
     * @return address family.
     */
    inline static int family(const struct sockaddr_internet& address)
        {return address.address.sa_family;};

    /**
     * Get data waiting in receive queue.
     * @param socket to get from.
     * @param buffer to save.
     * @param size of data buffer to request.
     * @param flags for i/o operation (MSG_OOB, MSG_PEEK, etc).
     * @param address of source.
     * @return number of bytes received, -1 if error.
     */
    static ssize_t recvfrom(socket_t socket, void *buffer, size_t size, int flags = 0, struct sockaddr_storage *address = NULL);

    /**
     * Send data on socket.
     * @param socket to send to.
     * @param buffer to send.
     * @param size of data buffer to send.
     * @param flags for i/o operation (MSG_OOB, MSG_PEEK, etc).
     * @param address of destination, NULL if connected.
     * @return number of bytes sent, -1 if error.
     */
    static ssize_t sendto(socket_t socket, const void *buffer, size_t size, int flags = 0, const struct sockaddr *address = NULL);

    /**
     * Send reply on socket.  Used to reply to a recvfrom message.
     * @param socket to send to.
     * @param buffer to send.
     * @param size of data buffer to send.
     * @param flags for i/o operation (MSG_OOB, MSG_PEEK, etc).
     * @param address to reply to.
     * @return number of bytes sent, -1 if error.
     */
    inline static ssize_t replyto(socket_t socket, const void *buffer, size_t size, int flags, const struct sockaddr_storage *address)
        {return sendto(socket, buffer, size, flags, (const struct sockaddr *)address);};

    /**
     * Send to internet socket.
     * @param socket to send to.
     * @param buffer to send.
     * @param size of data buffer to send.
     * @param flags for i/o operation (MSG_OOB, MSG_PEEK, etc).
     * @param address to send to.
     * @return number of bytes sent, -1 if error.
     */
    inline static ssize_t sendinet(socket_t socket, const void *buffer, size_t size, int flags, const struct sockaddr_internet *address)
        {return sendto(socket, buffer, size, flags, (const struct sockaddr *)address);};

    /**
     * Get internet data waiting in receive queue.
     * @param socket to get from.
     * @param buffer to save.
     * @param size of data buffer to request.
     * @param flags for i/o operation (MSG_OOB, MSG_PEEK, etc).
     * @param address of source.
     * @return number of bytes received, -1 if error.
     */
    static ssize_t recvinet(socket_t socket, void *buffer, size_t size, int flags = 0, struct sockaddr_internet *address = NULL);

    /**
     * Bind the socket descriptor to a known interface and service port.
     * @param socket descriptor to bind.
     * @param address to bind to or "*" for all.
     * @param service port to bind.
     * @param protocol to use or 0 if default.
     * @return 0 on success, -1 if error.
     */
    static int bindto(socket_t socket, const char *address, const char *service, int protocol = 0);

    /**
     * Bind the socket descriptor to a known interface listen on service port.
     * @param socket descriptor to bind.
     * @param address of interface to bind to.
     * @param backlog for service.
     * @return 0 on success, -1 if error.
     */
    static int listento(socket_t socket, const struct sockaddr *address, int backlog = 5);

    /**
     * Bind the socket descriptor to a known interface.
     * @param socket descriptor to bind.
     * @param address of interface to bind to.
     * @return 0 on success, -1 if error.
     */
    static int bindto(socket_t socket, const struct sockaddr *address);

    /**
     * Accept a socket connection from a remote host.
     * @param socket descriptor to accept from.
     * @param address of socket accepting.
     * @return new socket accepted.
     */
    static socket_t acceptfrom(socket_t socket, struct sockaddr_storage *address = NULL);

    /**
     * Create a socket object unbound.
     * @param family of socket.
     * @param type of socket.
     * @param protocol of socket.
     * @return socket descriptor created or INVALID_SOCKET.
     */
    static socket_t create(int family, int type, int protocol);

    /**
     * Create a connected socket.
     * @param address list to connect to.
     * @param type of socket to create.
     * @param protocol of socket.
     * @return socket descriptor created or INVALID_SOCKET.
     */
    static socket_t create(const struct addrinfo *address, int type, int protocol);

    /**
     * Create a bound socket for a service.
     * @param iface to bind.
     * @param service port to bind.
     * @param family to select or AF_UNSPEC
     * @param type of socket to create.
     * @param protocol of socket to create.
     * @return socket descriptor created or INVALID_SOCKET.
     */
    static socket_t create(const char *iface, const char *service, int family = AF_UNSPEC, int type = 0, int protocol = 0);

    /**
     * Create a connected socket for a service.
     * @param address of service for connect.
     * @return socket descriptor.
     */
    static socket_t create(const Socket::address &address);

    /**
     * Release (close) a socket.
     * @param socket to close.
     */
    static void release(socket_t socket);

    /**
     * Lookup and return the host name associated with a socket address.
     * @param address to lookup.
     * @param buffer to save hostname into.
     * @param size of buffer to save hostname into.
     * @return buffer or NULL if lookup fails.
     */
    static char *hostname(const struct sockaddr *address, char *buffer, size_t size);

    /**
     * Create an address info lookup hint based on the family and type
     * properties of a socket descriptor.
     * @param socket descriptor.
     * @param hint buffer.
     * @return hint buffer.
     */
    static struct addrinfo *hinting(socket_t socket, struct addrinfo *hint);

    /**
     * Lookup a host name and service address based on the addressing family
     * and socket type of a socket descriptor.  Store the result in a socket
     * address structure.
     * @param socket descriptor.
     * @param address that is resolved.
     * @param hostname to resolve.
     * @param service port.
     * @return socket address size.
     */
    static socklen_t query(socket_t socket, struct sockaddr_storage *address, const char *hostname, const char *service);

    /**
     * Get the size of a socket address.
     * @param address of socket.
     * @return size to use for this socket address object.
     */
    static socklen_t len(const struct sockaddr *address);

    /**
     * Compare socket addresses.  Test if the address and service matches
     * or if there is no service, then just the host address values.
     * @param address1 to compare.
     * @param address2 to compare.
     * @return true if same family and equal.
     */
    static bool equal(const struct sockaddr *address1, const struct sockaddr *address2);

    /**
     * Copy a socket address.
     * @param target address pointer to copy into.
     * @param origin address pointer to copy from.
     * @return number of bytes copied, 0 if invalid.
     */
    static unsigned copy(struct sockaddr *target, const struct sockaddr *origin);

    /**
     * Store an address into an address object.
     * @param storage for address.
     * @param address to store.
     * @return number of bytes stored.
     */
    inline static unsigned store(struct sockaddr_storage *storage, const struct sockaddr *address)
        {return copy((struct sockaddr*)storage, address);};

    /**
     * Store an address into an internet address object.
     * @param storage for address.
     * @param address to store.
     * @return number of bytes stored.
     */
    static unsigned store(struct sockaddr_internet *storage, const struct sockaddr *address);

    /**
     * Compare socket host addresses.  Test if the host address matches
     * or if there is no service, then just the host address values.
     * @param address1 to compare.
     * @param address2 to compare.
     * @return true if same family and equal.
     */
    static bool eq_host(const struct sockaddr *address1, const struct sockaddr *address2);

    /**
     * Compare socket addresses.  Test if the stored addresses received match.
     * or if there is no service, then just the host address values.
     * @param address1 to compare.
     * @param address2 to compare.
     * @return true if same family and equal.
     */
    inline static bool eq_from(const struct sockaddr_storage *address1, const struct sockaddr_storage *address2)
        {return equal((const struct sockaddr *)address1, (const struct sockaddr *)address2);};

    /**
     * Compare socket addresses.  Test if the internet addresses received match.
     * or if there is no service, then just the host address values.
     * @param address1 to compare.
     * @param address2 to compare.
     * @return true if same family and equal.
     */
    inline static bool eq_inet(const struct sockaddr_internet *address1, const struct sockaddr_internet *address2)
        {return equal((const struct sockaddr *)address1, (const struct sockaddr *)address2);};

    /**
     * See if both addresses are in the same subnet.  This is only relevant
     * to IPV4 and class domain routing.
     * @param address1 to test.
     * @param address2 to test.
     * @return true if in same subnet.
     */
    static bool eq_subnet(const struct sockaddr *address1, const struct sockaddr *address2);

    /**
     * Get the socket address of the interface needed to reach a destination
     * address.
     * @param address of interface found.
     * @param destination address.
     * @return 0 on success, -1 on error.
     */
    static int via(struct sockaddr *address, const struct sockaddr *destination);

    /**
     * Get the hostname of a socket address.
     * @param address to lookup.
     * @param buffer to save hostname in.
     * @param size of hostname buffer.
     * @return buffer if found or NULL if not.
     */
    static char *query(const struct sockaddr *address, char *buffer, socklen_t size);

    /**
     * Get the service port of a socket.
     * @param address of socket to examine.
     * @return service port number.
     */
    static short service(const struct sockaddr *address);

    /**
     * Get the service port of an inet socket.
     * @param address of internet socket to examine.
     * @return service port number.
     */
    inline static short service(const struct sockaddr_internet *address)
        {return service((const struct sockaddr *)address);};

    /**
     * Convert a socket address and service into a hash map index.
     * @param address to convert.
     * @param size of map index.
     * @return key index path.
     */
    static unsigned keyindex(const struct sockaddr *address, unsigned size);

    /**
     * Convert a socket host address into a hash map index.
     * @param address to convert.
     * @param size of map index.
     * @return key index path.
     */
    static unsigned keyhost(const struct sockaddr *address, unsigned size);

    /**
     * Initialize socket subsystem.
     */
    static void init(void);

    /**
     * Initialize with program name.  Used by socks, for example.
     * @param program name.
     */
    static void init(const char *program);

    /**
     * Set default socket family preference for query options when the
     * socket type is otherwise not specified.
     * @param family to select.
     */
    static void query(int family);

    /**
     * Set the default socket behavior for v6-v4 mapping.  This also
     * effects v6 address lookup as to whether v4 remapped addresses
     * can be used if no v6 address is found.
     * @param enable true to set mapping.  This is default.
     */
    static void v4mapping(bool enable);

    /**
     * Return error code of last socket operation,
     * @return errno style error code.
     */
    static int error(void);

    /**
     * Simple function to validate that a given IP address string is a "zero"
     * address.  Such address strings are used for example in SIP to indicate
     * "hold" by re-inviting peers to a null address.  Supports IPV4 and
     * IPV6 addresses.
     * @param string address to check.
     * @return true if zero/null address.
     */
    static bool is_null(const char *string);

    /**
     * Simple function to validate that a given IP address string is a numeric
     * address.  This can be used to verify an address is not a "host" name.
     * Supports IPV4 and IPV6 address strings.
     * @param string address to check.
     * @return true if zero/null address.
     */
    static bool is_numeric(const char *string);

    /**
     * Get local address to which the socket is bound.  This is defined here
     * because we may re-define the backend linkage for the socks proxy in
     * the future.
     * @param socket descriptor to examine.
     * @param address storage for local address.
     * @return 0 on success, -1 on failure.
     */
    static int local(socket_t socket, struct sockaddr_storage *address);

    /**
     * Get remote address to which the socket is connected.  This is defined
     * here because we may re-define the backend linkage for the socks proxy in
     * the future.
     * @param socket descriptor to examine.
     * @param address storage for remote address.
     * @return 0 on success, -1 on failure.
     */
    static int remote(socket_t socket, struct sockaddr_storage *address);
};

/**
 * A bound socket used to listen for inbound socket connections.  This class
 * is commonly used for TCP and DCCP listener sockets.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT ListenSocket : protected Socket
{
public:
    /**
     * Create and bind a listener socket.
     * @param address to bind on or "*" for all.
     * @param service port to bind listener.
     * @param backlog size for buffering pending connections.
     * @param family of socket.
     * @param type of socket.
     * @param protocol for socket if not TCPIP.
     */
    ListenSocket(const char *address, const char *service, unsigned backlog = 5, int family = AF_UNSPEC, int type = 0, int protocol = 0);

    /**
     * Create a listen socket directly.
     * @param address to bind on or "*" for all.
     * @param service port to bind listener.
     * @param backlog size for buffering pending connections.
     * @param family of socket.
     * @param type of socket.
     * @param protocol for socket if not TCPIP.
     * @return bound and listened to socket.
     */
    static socket_t create(const char *address, const char *service, unsigned backlog = 5, int family = AF_UNSPEC, int type = 0, int protocol = 0);

    /**
     * Accept a socket connection.
     * @param address to save peer connecting.
     * @return socket descriptor of connected socket.
     */
    socket_t accept(struct sockaddr_storage *address = NULL) const;

    /**
     * Wait for a pending connection.
     * @param timeout to wait.
     * @return true when acceptable connection is pending.
     */
    inline bool wait(timeout_t timeout = Timer::inf) const
        {return Socket::wait(timeout);};

    /**
     * Get the socket descriptor of the listener.
     * @return socket descriptor.
     */
    inline operator socket_t() const
        {return so;}

    /**
     * Get the socket descriptor of the listener by pointer reference.
     * @return socket descriptor.
     */
    inline socket_t operator*() const
        {return so;}

    /**
     * Get the socket descriptor of the listener.
     * @return socket descriptor.
     */
    inline socket_t getsocket(void) const
        {return so;}

    inline socket_t handle(void) const
        {return so;}

};

/**
 * A generic tcp server class.  This saves the service id tag so that it
 * can be propagated.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT TCPServer : public ListenSocket
{
public:
    /**
     * Create and bind a tcp server.  This mostly is used to preserve the
     * service tag for TCP Socket when derived from a server instance.
     * @param service tag to use.
     * @param address of interface to bind or "*" for all.
     * @param backlog size for pending connections.
     */
    TCPServer(const char *address, const char *service, unsigned backlog = 5);
};

/**
 * Helper function for linked_pointer<struct sockaddr>.
 */
__EXPORT struct addrinfo *_nextaddrinfo(struct addrinfo *addrinfo);

/**
 * Helper function for linked_pointer<struct sockaddr>.
 */
__EXPORT struct sockaddr *_getaddrinfo(struct addrinfo *addrinfo);

/**
 * Helper function for linked_pointer<struct sockaddr>.
 */
__EXPORT socket_t _getaddrsock(struct addrinfo *addrinfo);

/**
 * Linked pointer for address lists.  This can be used to iterate through
 * the list of a Socket::address object using the linked_pointer method.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
template <>
class linked_pointer<sockaddr_struct>
{
private:
    struct addrinfo *ptr;

public:
    inline linked_pointer(struct addrinfo *list)
        {ptr = list;}

    inline linked_pointer()
        {ptr = NULL;}

    inline linked_pointer(Socket::address& list)
        {ptr = list.getList();};

    /**
     * Get the full socket address list by casted reference.
     * @return addrinfo list we resolved or NULL if none.
     */
    inline operator struct sockaddr *() const
        {return _getaddrinfo(ptr);};

    /**
     * Return the full socket address list by pointer reference.
     * @return addrinfo list we resolved or NULL if none.
     */
    inline struct sockaddr *operator*() const
        {return _getaddrinfo(ptr);};

    inline operator struct sockaddr_in *() const
        {return (struct sockaddr_in *)_getaddrinfo(ptr);};

    inline struct sockaddr_in *in(void) const
        {return (struct sockaddr_in *)_getaddrinfo(ptr);};

#ifdef  AF_INET6
    inline operator struct sockaddr_in6 *() const
        {return (struct sockaddr_in6 *)_getaddrinfo(ptr);};

    inline struct sockaddr_in6 *in6(void) const
        {return (struct sockaddr_in6 *)_getaddrinfo(ptr);};
#endif

    /**
     * Get socket as expression operator.
     */
    inline socket_t operator()(void) const
        {return _getaddrsock(ptr);};

    /**
     * Test if the address list is valid.
     * @return true if we have an address list.
     */
    inline operator bool() const
        {return ptr != NULL;};

    /**
     * Assign our pointer from an address list.
     * @param pointer of linked list.
     */
    inline void operator=(struct addrinfo *list)
        {ptr = list;};

    /**
     * Assign our pointer from an address list.
     * @param pointer of linked list.
     */
    inline void operator=(Socket::address& list)
        {ptr = list.getList();};

    /**
     * Assign our pointer from an address list.
     * @param pointer of linked list.
     */
    inline void set(struct addrinfo *list)
        {ptr = list;};

    /**
     * Assign our pointer from an address list.
     * @param pointer of linked list.
     */
    inline void set(Socket::address& list)
        {ptr = list.getList();};


    /**
     * Return member from typed object our pointer references.
     * @return evaluated member of object we point to.
     */
    inline struct sockaddr* operator->() const
        {return _getaddrinfo(ptr);};

    /**
     * Test if we have no address list.
     * @return true if we have no address list.
     */
    inline bool operator!() const
        {return ptr == NULL;};

    inline void next(void)
        {ptr = _nextaddrinfo(ptr);};
};

/**
 * A convenience function to convert a socket address list into an addrinfo.
 * @param address list object.
 * @return addrinfo list or NULL if empty.
 */
inline struct addrinfo *addrinfo(Socket::address& address)
    {return address.getList();}

/**
 * A convenience function to convert a socket address list into a socket
 * address.
 * @param address list object.
 * @return first socket address in list or NULL if empty.
 */
inline struct sockaddr *addr(Socket::address& address)
    {return address.get();}

/**
 * Compare two socket addresses to see if equal.  If the port is zero
 * then this is the same as comparing host address alone.
 * @param s1 socket address to compare.
 * @param s2 socket address to compare.
 * @return true if addresses same.
 */
inline bool eq(const struct sockaddr *s1, const struct sockaddr *s2)
    {return Socket::equal(s1, s2);}

/**
 * Compare two stored socket addresses to see if equal.  If the port is zero
 * then this is the same as comparing host address alone.
 * @param s1 stored socket address to compare.
 * @param s2 stored socket address to compare.
 * @return true if addresses same.
 */
inline bool eq(const struct sockaddr_storage *s1, const struct sockaddr_storage *s2)
    {return Socket::equal((const struct sockaddr *)s1, (const struct sockaddr *)s2);}

/**
 * Compare two host addresses to see if equal.  The port numbers are
 * ignored.
 * @param s1 socket address to compare.
 * @param s2 socket address to compare.
 * @return true if addresses same.
 */
inline bool eq_host(const struct sockaddr *s1, const struct sockaddr *s2)
    {return Socket::eq_host(s1, s2);}

inline bool eq_subnet(const struct sockaddr *s1, const struct sockaddr *s2)
    {return Socket::eq_subnet(s1, s2);}

String str(Socket& so, strsize_t size);

typedef TCPServer   tcpserv_t;

END_NAMESPACE

#endif
