// Copyright (C) 1999-2005 Open Source Telecom Corporation.
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
 * @file commoncpp/address.h
 * @short Network addresses and sockets related classes.
 **/

#ifndef COMMONCPP_ADDRESS_H_
#define COMMONCPP_ADDRESS_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_THREAD_H_
#include <commoncpp/thread.h>
#endif

#ifndef COMMMONCPP_EXCEPTION_H_
#include <commoncpp/exception.h>
#endif

NAMESPACE_COMMONCPP

// future definition of ipv4 specific classes, now defines

#define INET_IPV4_ADDRESS_SIZE  16
#define CIDR_IPV4_ADDRESS_SIZE  32
#define INET_IPV6_ADDRESS_SIZE  40
#define CIDR_IPV6_ADDRESS_SIZE  45

#define CIDR        IPV4Cidr
#define InetAddress IPV4Address
#define InetHostAddress IPV4Host
#define InetMaskAddress IPV4Mask
#define InetMcastAddress IPV4Multicast
#define InetMcastAddressValidator IPV4MulticastValidator
#define InetAddrValidator IPV4Validator
#define BroadcastAddress IPV4Broadcast

/**
 * Transport Protocol Ports.
 */
typedef unsigned short tpport_t;

class IPV4Host;

/**
 * Classes derived from IPV4Address would require an specific
 * validator to pass to the IPV4Address constructor. This is a base
 * class for classes of function objects used by such derived classes.
 *
 * @author Federico Montesino <p5087@quintero.fie.us.es>
 * @short Abstract base class for derived inet addresses validators.
 */
class __EXPORT IPV4Validator
{
public:
    /**
     * Constructor. Does not deal with any state.
     */
    IPV4Validator() { };

    /**
     * keeps compilers happy.
     */
    virtual ~IPV4Validator() {};

    /**
     * Pure virtual application operator. Apply the validation
     * algorithm specific to derived classes.
     */
    virtual void
    operator()(const in_addr address) const = 0;
};

/**
 * Class for the function object that validates multicast addresses.
 * Implements a specific application operator to validate multicast
 * addresses.
 *
 * @author Federico Montesino <p5087@quintero.fie.us.es>
 * @short Validating class specialized for multicast addresses.
 */
class __EXPORT IPV4MulticastValidator: public IPV4Validator
{
public:
    /**
     * Constructor. Does not deal with any state.
     */
    IPV4MulticastValidator(){};

    /**
     * Keeps compilers happy.
     */
    virtual ~IPV4MulticastValidator(){};

    /**
     * Application operator. Apply the validation algorithm
     * specific to multicast addresses
     */
    void operator()(const in_addr address) const;
};

/**
 * The CIDR class is used to support routing tables and validate address
 * policies.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Classless Internet Domain Routing
 */
class __EXPORT IPV4Cidr
{
protected:
    struct in_addr netmask, network;

    unsigned getMask(const char *cp) const;
public:
    /**
     * Get network address associated with this cidr.
     *
     * @return system binary coded address.
     */
    inline struct in_addr getNetwork(void) const
        {return network;};

    /**
     * Get network mask associated with this cidr.
     *
     * @return system binary coded network mask.
     */
    inline struct in_addr getNetmask(void) const
        {return netmask;};

    /**
     * Compute the broadcast address associated with this cidr.
     *
     * @return system binary coded network address.
     */
    struct in_addr getBroadcast(void) const;

    /**
     * Set the cidr from a full or partial hostname, or from an
     * address/mask, or a host/bits specification.
     *
     * @param cidr string to use.
     */
    void set(const char *cidr);

    /**
     * Construct a new cidr from a string.
     *
     * @param cidr string to use.
     */
    IPV4Cidr(const char *cidr);

    /**
     * Construct an empty cidr.
     */
    IPV4Cidr();

    /**
     * Construct a copy of a cidr.
     *
     * @param cidr to copy from.
     */
    IPV4Cidr(IPV4Cidr &);

    /**
     * See if a socket address is a member of this cidr's network.
     *
     * @param saddr pointer to test.
     * @return true if member of cidr.
     */
    bool isMember(const struct sockaddr *saddr) const;

    /**
     * See if a low level address object is a member of this cidr's net.
     *
     * @param inaddr object to test.
     * @return true if member of cidr.
     */
    bool isMember(const struct in_addr &inaddr) const;

    inline bool operator==(const struct sockaddr *a) const
        {return isMember(a);};

    inline bool operator==(const struct in_addr &a) const
        {return isMember(a);};
};

#ifdef  CCXX_IPV6
/**
 * The CIDR class is used to support routing tables and validate address
 * policies.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Classless Internet Domain Routing
 */
class __EXPORT IPV6Cidr
{
protected:
    struct in6_addr netmask, network;

    unsigned getMask(const char *cp) const;
public:
    /**
     * Get network address associated with this cidr.
     *
     * @return system binary coded address.
     */
    inline struct in6_addr getNetwork(void) const
        {return network;};

    /**
     * Get network mask associated with this cidr.
     *
     * @return system binary coded network mask.
     */
    inline struct in6_addr getNetmask(void) const
        {return netmask;};

    /**
     * Compute the broadcast address associated with this cidr.
     *
     * @return system binary coded network address.
     */
    struct in6_addr getBroadcast(void) const;

    /**
     * Set the cidr from a full or partial hostname, or from a
     * host/bits specification.
     *
     * @param cidr string to use.
     */
    void set(const char *cidr);

    /**
     * Construct a new cidr from a string.
     *
     * @param cidr string to use.
     */
    IPV6Cidr(const char *cidr);

    /**
     * Construct an empty cidr.
     */
    IPV6Cidr();

    /**
     * Construct a copy of a cidr.
     *
     * @param cidr to copy from.
     */
    IPV6Cidr(IPV6Cidr &);

    /**
     * See if a socket address is a member of this cidr's network.
     *
     * @param saddr pointer to test.
     * @return true if member of cidr.
     */
    bool isMember(const struct sockaddr *saddr) const;

    /**
     * See if a low level address object is a member of this cidr's net.
     *
     * @param inaddr object to test.
     * @return true if member of cidr.
     */
    bool isMember(const struct in6_addr &inaddr) const;

    inline bool operator==(const struct sockaddr *sa) const
        {return isMember(sa);};

    inline bool operator==(const struct in6_addr &a) const
        {return isMember(a);};
};

#endif

/**
 *  The network name and address objects are all derived from a common
 * IPV4Address base class. Specific classes, such as IPV4Host,
 * IPV4Mask, etc, are defined from IPV4Address entirely so that the
 * manner a network address is being used can easily be documented and
 * understood from the code and to avoid common errors and accidental misuse
 * of the wrong address object.  For example, a "connection" to something
 * that is declared as a "IPV4Host" can be kept type-safe from a
 * "connection" accidently being made to something that was declared a
 * "IPV4Broadcast".
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Internet Address binary data type.
 */
class __EXPORT IPV4Address
{
private:
    // The validator given to an IPV4Address object must not be a
    // transient object, but that must exist at least until the
    // last address object of its kind is deleted. This is an
    // artifact to be able to do specific checks for derived
    // classes inside constructors.
    const InetAddrValidator *validator;

protected:
    struct in_addr * ipaddr;
    size_t addr_count;
    mutable char* hostname;  // hostname for ipaddr[0]. Used by getHostname
#if defined(_MSWINDOWS_)
    static MutexCounter counter;
#else
    static Mutex mutex;
#endif
    /**
     * Sets the IP address from a string representation of the
     * numeric address, ie "127.0.0.1"
     *
     * @param host The string representation of the IP address
     * @return true if successful
     */
    bool setIPAddress(const char *host);

    /**
     * Used to specify a host name or numeric internet address.
     *
     * @param host The string representation of the IP address or
     *  a hostname, , if NULL, it will default to INADDR_ANY
     */
    void setAddress(const char *host);

public:
    /**
     * Create an Internet Address object with an empty (0.0.0.0)
     * address.
     *
     * @param validator optional validator function object, intended for
     *        derived classes.
     */
    IPV4Address(const InetAddrValidator *validator = NULL);

    /**
     * Convert the system internet address data type (struct in_addr)
     * into a Common C++ IPV4Address object.
     *
     * @param addr struct of system used binary internet address.
     * @param validator optional validator function object, intended for
     *        derived classes.
     */
    IPV4Address(struct in_addr addr, const InetAddrValidator *validator = NULL);

    /**
     * Convert a null terminated ASCII host address string
     * (example: "127.0.0.1") or host address name (example:
     * "www.voxilla.org") directly into a Common C++ IPV4Address
     * object.
     *
     * @param address null terminated C string.
     * @param validator optional validator function object, intended for
     *        derived classes.
     */
    IPV4Address(const char *address, const InetAddrValidator *validator = NULL);

    /**
     * Copy constructor
     */
    IPV4Address(const IPV4Address &rhs);

    /**
     * Destructor
     */
    virtual ~IPV4Address();

    /**
     * Provide a string representation of the value (Internet Address)
     * held in the IPV4Address object.
     *
     * @return string representation of IPV4Address.
     */
    const char *getHostname(void) const;

    /**
     * May be used to verify if a given IPV4Address returned
     * by another function contains a "valid" address, or "0.0.0.0"
     * which is often used to mark "invalid" IPV4Address values.
     *
     * @return true if address != 0.0.0.0.
     */
    bool isInetAddress(void) const;

    /**
     * Provide a low level system usable struct in_addr object from
     * the contents of IPV4Address.  This is needed for services such
     * as bind() and connect().
     *
     * @return system binary coded internet address.
     */
    struct in_addr getAddress(void) const;

    /**
     * Provide a low level system usable struct in_addr object from
     * the contents of IPV4Address.  This is needed for services such
     * as bind() and connect().
     *
     * @param i for IPV4Addresses with multiple addresses, returns the
     *  address at this index.  User should call getAddressCount()
     *  to determine the number of address the object contains.
     * @return system binary coded internet address.  If parameter i is
     *  out of range, the first address is returned.
     */
    struct in_addr getAddress(size_t i) const;

    /**
     * Returns the number of internet addresses that an IPV4Address object
     * contains.  This usually only happens with IPV4Host objects
     * where multiple IP addresses are returned for a DNS lookup
     */
    size_t getAddressCount() const { return addr_count; }

    IPV4Address &operator=(const char *str);
    IPV4Address &operator=(struct in_addr addr);
    IPV4Address &operator=(const IPV4Address &rhs);

    /**
     * Allows assignment from the return of functions like
     * inet_addr() or htonl()
     */
    IPV4Address &operator=(unsigned long addr);

    inline IPV4Address &operator=(unsigned int addr)
        {return *this = (unsigned long) addr; }

    inline bool operator!() const
        {return !isInetAddress();};

    /**
     * Compare two internet addresses to see if they are equal
     * (if they specify the physical address of the same internet host).
     *
     * If there is more than one IP address in either IPV4Address object,
     * this will return true if all of the IP addresses in the smaller
     * are in the larger in any order.
     */
    bool operator==(const IPV4Address &a) const;

    /**
     * Compare two internet addresses to see if they are not
     * equal (if they each refer to unique and different physical
     * ip addresses).
     *
     * This is implimented in terms of operator==
     */
    bool operator!=(const IPV4Address &a) const;
};

/**
 * Internet addresses used specifically as masking addresses (such as "
 * 255.255.255.0") are held in the IPV4Mask derived object.  The
 * seperate class is used so that C++ type casting can automatically
 * determine when an IPV4Address object is really a mask address object
 * rather than simply using the base class.  This also allows manipulative
 * operators for address masking to operate only when presented with a
 * Masked address as well as providing cleaner and safer source.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Internet Address Mask such as subnet masks.
 */
class __EXPORT IPV4Mask : public IPV4Address
{
public:
    /**
     * Create the mask from a null terminated ASCII string such as
     * "255.255.255.128".
     *
     * @param mask null terminated ASCII mask string.
     */
    IPV4Mask(const char *mask);

    /**
     * Masks are usually used to coerce host addresses into a specific
     * router or class domain.  This can be done by taking the Inet
     * Host Address object and "and"ing it with an address mask.  This
     * operation can be directly expressed in C++ through the & operator.
     *
     * @return a internet host address that has been masked.
     * @param addr host address to be masked by subnet.
     * @param mask inetnet mask address object to mask by.
     */
    friend __EXPORT IPV4Host operator&(const IPV4Host &addr,
                     const IPV4Mask &mask);

    /**
     * Allows assignment from the return of functions like
     * inet_addr() or htonl()
     */
    IPV4Address &operator=(unsigned long addr)
        { return IPV4Address::operator =(addr); }
};

/**
 * This object is used to hold the actual and valid internet address of a
 * specific host machine that will be accessed through a socket.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Address of a specific Internet host machine.
 */
class __EXPORT IPV4Host : public IPV4Address
{
private:
    static IPV4Host _host_;

public:
    /**
     * Create a new host address for a specific internet host.  The
     * internet host can be specified in a null terminated ASCII
     * string and include either the physical host address or the
     * DNS name of a host machine.  Hence, an IPV4Host
     * ("www.voxilla.org") can be directly declaired in this manner.
     *
     * Defaults to the IP address that represents the interface matching
     * "gethostname()".
     *
     * @param host dns or physical address of an Internet host.
     */
    IPV4Host(const char *host = NULL);

    /**
     * Convert a system socket binary address such as may be
     * returned through the accept() call or getsockpeer() into
     * an internet host address object.
     *
     * @param addr binary address of internet host.
     */
    IPV4Host(struct in_addr addr);

    /**
     * Allows assignment from the return of functions like
     * inet_addr() or htonl()
     */
    IPV4Address &operator=(unsigned long addr)
    { return IPV4Address::operator =(addr); }

    /**
     * Mask the internet host address object with a network mask address.
     * This is commonly used to coerce an address by subnet.
     */
    IPV4Host &operator&=(const IPV4Mask &mask);

    friend class IPV4Mask;
    friend __EXPORT IPV4Host operator&(const IPV4Host &addr,
                     const IPV4Mask &mask);
};

/**
 * The broadcast address object is used to store the broadcast address for
 * a specific subnet.  This is commonly used for UDP broadcast operations.
 */
class __EXPORT IPV4Broadcast : public IPV4Address
{
public:
    /**
     * Specify the physical broadcast address to use and create a new
     * broadcast address object based on a null terminated ASCII
     * string.
     *
     * @param net null terminated ASCII network address.
     */
    IPV4Broadcast(const char *net = "255.255.255.255");
};

/**
 * A specialization of IPV4Address that provides address validation
 * for multicast addresses. Whenever its value changes the new value
 * is checked to be in the range from 224.0.0.1 through
 * 239.255.255.255. If it is not, an exception is thrown.
 *
 * @short A multicast network address.
 * @author Federico Montesino <p5087@quintero.fie.us.es>
 */
class __EXPORT IPV4Multicast: public IPV4Address
{
public:
    /**
     * Create an Internet Multicast Address object with an empty
     * (0.0.0.0) address.
     */
    IPV4Multicast();

    /**
     * Convert the system internet address data type (struct in_addr)
     * into a Common C++ IPV4Multicast object.
     *
     * @param address struct of system used binary internet address.
     */
    IPV4Multicast(const struct in_addr address);

    /**
     * Convert a null terminated ASCII multicast address string
     * (example: "224.0.0.1") or multicast name string (example:
     * "sap.mcast.net") directly into a Common C++
     * IPV4Multicast object. Works like IPV4Address(const
     * char*).
     *
     * @param address null terminated C string.
     */
    IPV4Multicast(const char *address);

private:
    /**
     * Check the address in <code>addr<code> is a valid multicast
     * address. In case not, throws an exception.
     *
     * @param address a system network address
     * @return true if validation succeeded
     */
    static const IPV4MulticastValidator validator;
};

extern __EXPORT std::ostream& operator<<(std::ostream &os, const IPV4Address &ia);

inline struct in_addr getaddress(const IPV4Address &ia)
    {return ia.getAddress();}


#ifdef  CCXX_IPV6

class IPV6Host;

/**
 * Classes derived from IPV6Address would require an specific
 * validator to pass to the IPV6Address constructor. This is a base
 * class for classes of function objects used by such derived classes.
 *
 * @author Federico Montesino <p5087@quintero.fie.us.es>
 * @short Abstract base class for derived inet addresses validators.
 */
class __EXPORT IPV6Validator
{
public:
    /**
     * Constructor. Does not deal with any state.
     */
    IPV6Validator() { };

    /**
     * Keeps compilers happy.
     */
    virtual ~IPV6Validator() {};

    /**
     * Pure virtual application operator. Apply the validation
     * algorithm specific to derived classes.
     */
    virtual void operator()(const in6_addr address) const = 0;
};

/**
 * Class for the function object that validates multicast addresses.
 * Implements a specific application operator to validate multicast
 * addresses.
 *
 * @author Federico Montesino <p5087@quintero.fie.us.es>
 * @short Validating class specialized for multicast addresses.
 */
class __EXPORT IPV6MulticastValidator: public IPV6Validator
{
public:
    /**
     * Constructor. Does not deal with any state.
     */
    IPV6MulticastValidator(){};

    /**
     * Keeps compilers happy...
     */
    virtual ~IPV6MulticastValidator(){};

    /**
     * Application operator. Apply the validation algorithm
     * specific to multicast addresses
     */
    void operator()(const in6_addr address) const;
};

/**
 *  The network name and address objects are all derived from a common
 * IPV6Address base class. Specific classes, such as IPV4Host,
 * IPV6Mask, etc, are defined from IPV6Address entirely so that the
 * manner a network address is being used can easily be documented and
 * understood from the code and to avoid common errors and accidental misuse
 * of the wrong address object.  For example, a "connection" to something
 * that is declared as a "IPV6Host" can be kept type-safe from a
 * "connection" accidently being made to something that was declared a
 * "IPV6Broadcast".
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Internet Address binary data type.
 */
class __EXPORT IPV6Address
{
private:
    // The validator given to an IPV4Address object must not be a
    // transient object, but that must exist at least until the
    // last address object of its kind is deleted. This is an
    // artifact to be able to do specific checks for derived
    // classes inside constructors.
    const IPV6Validator *validator;

protected:
    struct in6_addr * ipaddr;
    size_t addr_count;
    mutable char* hostname;  // hostname for ipaddr[0]. Used by getHostname
#if defined(_MSWINDOWS_)
    static MutexCounter counter;
#else
    static Mutex mutex;
#endif
    /**
     * Sets the IP address from a string representation of the
     * numeric address, ie "127.0.0.1"
     *
     * @param host The string representation of the IP address
     * @return true if successful
     */
    bool setIPAddress(const char *host);

    /**
     * Used to specify a host name or numeric internet address.
     *
     * @param host The string representation of the IP address or
     *  a hostname, , if NULL, it will default to INADDR_ANY
     */
    void setAddress(const char *host);

public:
    /**
     * Create an Internet Address object with an empty (0.0.0.0)
     * address.
     *
     * @param validator optional validator function object, intended for
     *        derived classes.
     */
    IPV6Address(const IPV6Validator *validator = NULL);

    /**
     * Convert the system internet address data type (struct in_addr)
     * into a Common C++ IPV6Address object.
     *
     * @param addr struct of system used binary internet address.
     * @param validator optional validator function object, intended for
     *        derived classes.
     */
    IPV6Address(struct in6_addr addr, const IPV6Validator *validator = NULL);

    /**
     * Convert a null terminated ASCII host address string
     * (example: "127.0.0.1") or host address name (example:
     * "www.voxilla.org") directly into a Common C++ IPV6Address
     * object.
     *
     * @param address null terminated C string.
     * @param validator optional validator function object, intended for
     *        derived classes.
     */
    IPV6Address(const char *address, const IPV6Validator *validator = NULL);

    /**
     * Copy constructor
     */
    IPV6Address(const IPV6Address &rhs);

    /**
     * Destructor
     */
    virtual ~IPV6Address();

    /**
     * Provide a string representation of the value (Internet Address)
     * held in the IPV6Address object.
     *
     * @return string representation of IPV6Address.
     */
    const char *getHostname(void) const;

    /**
     * May be used to verify if a given IPV6Address returned
     * by another function contains a "valid" address, or "0.0.0.0"
     * which is often used to mark "invalid" IPV6Address values.
     *
     * @return true if address != 0.0.0.0.
     */
    bool isInetAddress(void) const;

    /**
     * Provide a low level system usable struct in_addr object from
     * the contents of IPV6Address.  This is needed for services such
     * as bind() and connect().
     *
     * @return system binary coded internet address.
     */
    struct in6_addr getAddress(void) const;

    /**
     * Provide a low level system usable struct in_addr object from
     * the contents of IPV6Address.  This is needed for services such
     * as bind() and connect().
     *
     * @param i for IPV6Addresses with multiple addresses, returns the
     *  address at this index.  User should call getAddressCount()
     *  to determine the number of address the object contains.
     * @return system binary coded internet address.  If parameter i is
     *  out of range, the first address is returned.
     */
    struct in6_addr getAddress(size_t i) const;

    /**
     * Returns the number of internet addresses that an IPV6Address object
     * contains.  This usually only happens with IPV6Host objects
     * where multiple IP addresses are returned for a DNS lookup
     */
    size_t getAddressCount() const { return addr_count; }

    IPV6Address &operator=(const char *str);
    IPV6Address &operator=(struct in6_addr addr);
    IPV6Address &operator=(const IPV6Address &rhs);

    inline bool operator!() const
        {return !isInetAddress();};

    /**
     * Compare two internet addresses to see if they are equal
     * (if they specify the physical address of the same internet host).
     *
     * If there is more than one IP address in either IPV6Address object,
     * this will return true if all of the IP addresses in the smaller
     * are in the larger in any order.
     */
    bool operator==(const IPV6Address &a) const;

    /**
     * Compare two internet addresses to see if they are not
     * equal (if they each refer to unique and different physical
     * ip addresses).
     *
     * This is implimented in terms of operator==
     */
    bool operator!=(const IPV6Address &a) const;
};

/**
 * Internet addresses used specifically as masking addresses (such as "
 * 255.255.255.0") are held in the IPV6Mask derived object.  The
 * seperate class is used so that C++ type casting can automatically
 * determine when an IPV6Address object is really a mask address object
 * rather than simply using the base class.  This also allows manipulative
 * operators for address masking to operate only when presented with a
 * Masked address as well as providing cleaner and safer source.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Internet Address Mask such as subnet masks.
 */
class __EXPORT IPV6Mask : public IPV6Address
{
public:
    /**
     * Create the mask from a null terminated ASCII string such as
     * "255.255.255.128".
     *
     * @param mask null terminated ASCII mask string.
     */
    IPV6Mask(const char *mask);

    /**
     * Masks are usually used to coerce host addresses into a specific
     * router or class domain.  This can be done by taking the Inet
     * Host Address object and "and"ing it with an address mask.  This
     * operation can be directly expressed in C++ through the & operator.
     *
     * @return a internet host address that has been masked.
     * @param addr host address to be masked by subnet.
     * @param mask inetnet mask address object to mask by.
     */
    friend __EXPORT IPV6Host operator&(const IPV6Host &addr,
                     const IPV6Mask &mask);
};

/**
 * This object is used to hold the actual and valid internet address of a
 * specific host machine that will be accessed through a socket.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Address of a specific Internet host machine.
 */
class __EXPORT IPV6Host : public IPV6Address
{
public:
    /**
     * Create a new host address for a specific internet host.  The
     * internet host can be specified in a null terminated ASCII
     * string and include either the physical host address or the
     * DNS name of a host machine.  Hence, an IPV6Host
     * ("www.voxilla.org") can be directly declaired in this manner.
     *
     * Defaults to the IP address that represents the interface matching
     * "gethostname()".
     *
     * @param host dns or physical address of an Internet host.
     */
    IPV6Host(const char *host = NULL);

    /**
     * Convert a system socket binary address such as may be
     * returned through the accept() call or getsockpeer() into
     * an internet host address object.
     *
     * @param addr binary address of internet host.
     */
    IPV6Host(struct in6_addr addr);

    /**
     * Mask the internet host address object with a network mask address.
     * This is commonly used to coerce an address by subnet.
     */
    IPV6Host &operator&=(const IPV6Mask &mask);

    friend class IPV6Mask;
    friend __EXPORT IPV6Host operator&(const IPV6Host &addr, const IPV6Mask &mask);
};

/**
 * The broadcast address object is used to store the broadcast address for
 * a specific subnet.  This is commonly used for UDP broadcast operations.
 */
class __EXPORT IPV6Broadcast : public IPV6Address
{
public:
    /**
     * Specify the physical broadcast address to use and create a new
     * broadcast address object based on a null terminated ASCII
     * string.
     *
     * @param net null terminated ASCII network address.
     */
    IPV6Broadcast(const char *net = "255.255.255.255");
};

/**
 * A specialization of IPV6Address that provides address validation
 * for multicast addresses. Whenever its value changes the new value
 * is checked to be in the range from 224.0.0.1 through
 * 239.255.255.255. If it is not, an exception is thrown.
 *
 * @short A multicast network address.
 * @author Federico Montesino <p5087@quintero.fie.us.es>
 */
class __EXPORT IPV6Multicast: public IPV6Address
{
public:
    /**
     * Create an Internet Multicast Address object with an empty
     * (0.0.0.0) address.
     */
    IPV6Multicast();

    /**
     * Convert the system internet address data type (struct in_addr)
     * into a Common C++ IPV4Multicast object.
     *
     * @param address struct of system used binary internet address.
     */
    IPV6Multicast(const struct in6_addr address);

    /**
     * Convert a null terminated ASCII multicast address string
     * (example: "224.0.0.1") or multicast name string (example:
     * "sap.mcast.net") directly into a Common C++
     * IPV6Multicast object. Works like IPV6Address(const
     * char*).
     *
     * @param address null terminated C string.
     */
    IPV6Multicast(const char *address);

private:
    /**
     * Check the address in <code>addr<code> is a valid multicast
     * address. In case not, throws an exception.
     *
     * @param address a system network address
     * @return true if validation succeeded
     */
    static const IPV6MulticastValidator validator;
};

extern __EXPORT std::ostream& operator<<(std::ostream &os, const IPV6Address &ia);

inline struct in6_addr getaddress(const IPV6Address &ia)
    {return ia.getAddress();}


#endif

END_NAMESPACE

#endif
