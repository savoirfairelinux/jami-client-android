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
 * @file commoncpp/udp.h
 * @short udp derived socket classes.
 **/

#ifndef COMMONCPP_UDP_H_
#define COMMONCPP_UDP_H_

#include <cstdio>

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

#ifndef COMMONCPP_STRING_H_
#include <commoncpp/string.h>
#endif

#ifndef COMMONCPP_ADDRESS_H_
#include <commoncpp/address.h>
#endif

#ifndef COMMONCPP_SOCKET_H_
#include <commoncpp/socket.h>
#endif

NAMESPACE_COMMONCPP

/**
 * UDP sockets implement the TCP SOCK_DGRAM UDP protocol.  They can be
 * used to pass unverified messages between hosts, or to broadcast a
 * specific message to an entire subnet.  Please note that Streaming of
 * realtime data commonly use UDPDuplex related classes rather than
 * UDPSocket.
 *
 * In addition to connected TCP sessions, Common C++ supports UDP sockets and
 * these also cover a range of functionality.  Like a TCPSocket, A UDPSocket
 * can be created bound to a specific network interface and/or port address,
 * though this is not required.  UDP sockets also are usually either
 * connected or otherwise "associated" with a specific "peer" UDP socket.
 * Since UDP sockets operate through discreet packets, there are no streaming
 * operators used with UDP sockets.
 *
 * In addition to the UDP "socket" class, there is a "UDPBroadcast" class.
 * The UDPBroadcast is a socket that is set to send messages to a subnet as a
 * whole rather than to an individual peer socket that it may be associated
 * with.
 *
 * UDP sockets are often used for building "realtime" media  streaming
 * protocols and full duplex messaging services.  When used in this manner,
 * typically a pair of UDP sockets are used together; one socket is used to
 * send and the other to receive data with an associated pair of UDP sockets
 * on a "peer" host.  This concept is represented through the Common C++
 * UDPDuplex object, which is a pair of sockets that communicate with another
 * UDPDuplex pair.
 *
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram Protocol sockets.
 */
class __EXPORT UDPSocket : public Socket
{
private:
    inline Error setKeepAlive(bool enable)
        {return Socket::setKeepAlive(enable);};

protected:
#ifdef  CCXX_IPV6
    union {
        struct sockaddr_in6 ipv6;
        struct sockaddr_in ipv4;
    }   peer;
#else
    union {
        struct sockaddr_in ipv4;
    }   peer;
#endif

    Family family;

public:
    /**
     * Create an unbound UDP socket, mostly for internal use.
     */
    UDPSocket(Family family = IPV4);

    /**
     * Create a UDP socket bound by a service name.
     */
    UDPSocket(const char *name, Family family = IPV4);

    /**
     * Create a UDP socket and bind it to a specific interface
     * and port address so that other UDP sockets on remote
     * machines (or the same host) may find and send UDP messages
     * to it.  On failure to bind, an exception is thrown.
     *
     * @param bind address to bind this socket to.
     * @param port number to bind this socket to.
     */
    UDPSocket(const IPV4Address &bind, tpport_t port);
#ifdef  CCXX_IPV6
    UDPSocket(const IPV6Address &bind, tpport_t port);
#endif

    /**
     * Destroy a UDP socket as a socket.
     */
    virtual ~UDPSocket();

    /**
     * Set the loopback.
     */
    inline Error setLoopback(bool enable)
        {return Socket::setLoopbackByFamily(enable, family);}

    /**
     * Set the multicast.
     */
    inline Error setMulticast(bool enable)
        {return Socket::setMulticastByFamily(enable, family);}

    /**
     * Set time to live.
     */
    inline Error setTimeToLive(char ttl)
        {return Socket::setTimeToLiveByFamily(ttl, family);}

    /**
     * set the peer address to send message packets to.  This can be
     * set before every send() call if nessisary.
     *
     * @param host address to send packets to.
     * @param port number to deliver packets to.
     */
    void setPeer(const IPV4Host &host, tpport_t port);
    void connect(const IPV4Host &host, tpport_t port);
#ifdef  CCXX_IPV6
    void setPeer(const IPV6Host &host, tpport_t port);
    void connect(const IPV6Host &host, tpport_t port);
#endif

    /**
     * get the interface index for a named network device
     *
     * @param ethX is device name, like "eth0" or "eth1"
     * @param InterfaceIndex is the index value returned by os
     * @todo Win32 and ipv6 specific implementation.
     */
    Socket::Error getInterfaceIndex(const char *ethX,int& InterfaceIndex);

    /**
     * join a multicast group on a particular interface
     *
     * @param ia is the multicast address to use
     * @param InterfaceIndex is the index value returned by
     * getInterfaceIndex
     * @todo Win32 and ipv6 specific implementation.
     */
    Socket::Error join(const IPV4Multicast &ia,int InterfaceIndex);

    /**
     * Send a message packet to a peer host.
     *
     * @param buf pointer to packet buffer to send.
     * @param len of packet buffer to send.
     * @return number of bytes sent.
     */
    ssize_t send(const void *buf, size_t len);

    /**
     * Receive a message from any host.
     *
     * @param buf pointer to packet buffer to receive.
     * @param len of packet buffer to receive.
     * @param reply save sender address for reply if true.
     * @return number of bytes received.
     */
    ssize_t receive(void *buf, size_t len, bool reply = false);

    /**
     * Examine address of sender of next waiting packet.  This also
     * sets "peer" address to the sender so that the next "send"
     * message acts as a "reply".  This additional behavior overides
     * the standard socket getSender behavior.
     *
     * @param port pointer to hold port number.
     */
    IPV4Host getIPV4Peer(tpport_t *port = NULL) const;
    inline IPV4Host getPeer(tpport_t *port = NULL) const
        {return getIPV4Peer(port);}

#ifdef  CCXX_IPV6
    IPV6Host getIPV6Peer(tpport_t *port = NULL) const;
#endif

    /**
     * Examine contents of next waiting packet.
     *
     * @param buf pointer to packet buffer for contents.
     * @param len of packet buffer.
     * @return number of bytes examined.
     */
    inline ssize_t peek(void *buf, size_t len)
        {return ::recv(so, (char *)buf, len, MSG_PEEK);};

    /**
     * Associate socket with a named connection
     */
    void setPeer(const char *service);
    void connect(const char *service);

    /**
     * Disassociate this socket from any host connection.  No data
     * should be read or written until a connection is established.
     */
    Error disconnect(void);
};

/**
 * Representing a UDP socket used for subnet broadcasts, this class
 * provides an alternate binding and setPeer() capability for UDP
 * sockets.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram for subnet broadcasts.
 */
class __EXPORT UDPBroadcast : public UDPSocket
{
private:
    void setPeer(const IPV4Host &ia, tpport_t port);

    Error setBroadcast(bool enable)
        {return Socket::setBroadcast(enable);};

public:
    /**
     * Create and bind a subnet broadcast socket.
     *
     * @param ia address to bind socket under locally.
     * @param port to bind socket under locally.
     */
    UDPBroadcast(const IPV4Address &ia, tpport_t port);

    /**
     * Set peer by subnet rather than specific host.
     *
     * @param subnet of peer hosts to send to.
     * @param port number to use.
     */
    void setPeer(const IPV4Broadcast &subnet, tpport_t port);
};

/**
 * Representing half of a two-way UDP connection, the UDP transmitter
 * can broadcast data to another selected peer host or to an entire
 * subnet.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram Peer Associations.
 */
class __EXPORT UDPTransmit : protected UDPSocket
{
private:
    /**
     * Common code for diferent flavours of Connect (host, broadcast,
     * multicast).
     *
     * @param ia network address to associate with
     * @param port port number to associate with
     */
    Error cConnect(const IPV4Address &ia, tpport_t port);

protected:
    /**
     * Create a UDP transmitter.
     */
    UDPTransmit(Family family = IPV4);

    /**
     * Create a UDP transmitter, bind it to a specific interface
     * and port address so that other UDP sockets on remote
     * machines (or the same host) may find and send UDP messages
     * to it, and associate it with a given port on a peer host.
     * On failure to bind, an exception is thrown.  This class is
     * only used to build the UDP Duplex.
     *
     * @param bind address to bind this socket to.
     * @param port number to bind this socket to.
     */
    UDPTransmit(const IPV4Address &bind, tpport_t port = 5005);
#ifdef  CCXX_IPV6
    UDPTransmit(const IPV6Address &bind, tpport_t port = 5005);
#endif

    /**
     * Associate this socket with a specified peer host.  The port
     * number from the constructor will be used.  All UDP packets
     * will be sent to and received from the specified host.
     *
     * @return 0 on success, -1 on error.
     * @param host address to connect socket to.
     * @param port to connect socket to.
     */
    Error connect(const IPV4Host &host, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Address &host, tpport_t port);
#endif

    /**
     * Associate this socket with a subnet of peer hosts for
     * subnet broadcasting.  The server must be able to assert
     * broadcast permission for the socket.
     *
     * @return 0 on success, -1 on error.
     * @param subnet subnet address to broadcast into.
     * @param port transport port to broadcast into.
     */
    Error connect(const IPV4Broadcast &subnet, tpport_t port);

    /**
     * Associate this socket with a multicast group.
     *
     * @return 0 on success, -1 on error.
     * @param mgroup address of the multicast group to send to.
     * @param port port number
     */
    Error connect(const IPV4Multicast &mgroup, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Multicast &mgroup, tpport_t port);
#endif

    /**
     * Transmit "send" to use "connected" send rather than sendto.
     *
     * @return number of bytes sent.
     * @param buf address of buffer to send.
     * @param len of bytes to send.
     */
    inline ssize_t send(const void *buf, size_t len)
        {return ::send(so, (const char *)buf, len, MSG_NOSIGNAL);}

    /**
     * Stop transmitter.
     */
    inline void endTransmitter(void)
        {Socket::endSocket();}

    /*
     * Get transmitter socket.
     *
     * @return transmitter.
     */
    inline SOCKET getTransmitter(void)
        {return so;};

    inline Error setMulticast(bool enable)
        {return Socket::setMulticastByFamily(enable, family);}

    inline Error setTimeToLive(unsigned char ttl)
        {return Socket::setTimeToLiveByFamily(ttl, family);};

public:
    /**
     * Transmit "send" to use "connected" send rather than sendto.
     *
     * @note Windows does not support MSG_DONTWAIT, so it is defined
     *   as 0 on that platform.
     * @return number of bytes sent.
     * @param buffer address of buffer to send.
     * @param len of bytes to send.
     */
    inline ssize_t transmit(const char *buffer, size_t len)
        {return ::send(so, buffer, len, MSG_DONTWAIT|MSG_NOSIGNAL);}

    /**
     * See if output queue is empty for sending more packets.
     *
     * @return true if output available.
     * @param timeout in milliseconds to wait.
     */
    inline bool isOutputReady(unsigned long timeout = 0l)
        {return Socket::isPending(Socket::pendingOutput, timeout);};


    inline Error setRouting(bool enable)
        {return Socket::setRouting(enable);};

    inline Error setTypeOfService(Tos tos)
        {return Socket::setTypeOfService(tos);};

    inline Error setBroadcast(bool enable)
        {return Socket::setBroadcast(enable);};
};

/**
 * Representing half of a two-way UDP connection, the UDP receiver
 * can receive data from another peer host or subnet.  This class is
 * used exclusivily to derive the UDPDuplex.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram Peer Associations.
 */
class __EXPORT UDPReceive : protected UDPSocket
{
protected:
    /**
     * Create a UDP receiver, bind it to a specific interface
     * and port address so that other UDP sockets on remote
     * machines (or the same host) may find and send UDP messages
     * to it, and associate it with a given port on a peer host.
     * On failure to bind, an exception is thrown.
     *
     * @param bind address to bind this socket to.
     * @param port number to bind this socket to.
     */
    UDPReceive(const IPV4Address &bind, tpport_t port);
#ifdef  CCXX_IPV6
    UDPReceive(const IPV6Address &bind, tpport_t port);
#endif

    /**
     * Associate this socket with a specified peer host.  The port
     * number from the constructor will be used.  All UDP packets
     * will be sent received from the specified host.
     *
     * @return 0 on success, -1 on error.
     * @param host host network address to connect socket to.
     * @param port host transport port to connect socket to.
     */
    Error connect(const IPV4Host &host, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Host &host, tpport_t port);
#endif

    /**
     * Check for pending data.
     *
     * @return true if data is waiting.
     * @param timeout in milliseconds.
     */
    bool isPendingReceive(timeout_t timeout)
        {return Socket::isPending(Socket::pendingInput, timeout);};

    /**
     * End receiver.
     */
    inline void endReceiver(void)
        {Socket::endSocket();}

    inline SOCKET getReceiver(void) const
        {return so;};

    inline Error setRouting(bool enable)
        {return Socket::setRouting(enable);}

    inline Error setMulticast(bool enable)
        {return Socket::setMulticastByFamily(enable, family);}

    inline Error join(const IPV4Multicast &ia)
            {return Socket::join(ia);}

#ifdef  CCXX_IPV6
    inline Error join(const IPV6Multicast &ia)
        {return Socket::join(ia);}
#endif

    inline Error drop(const IPV4Multicast &ia)
            {return Socket::drop(ia);}

#ifdef  CCXX_IPV6
    inline Error drop(const IPV6Multicast &ia)
        {return Socket::drop(ia);}
#endif

public:
    /**
     * Receive a data packet from the connected peer host.
     *
     * @return num of bytes actually received.
     * @param buf address of data receive buffer.
     * @param len size of data receive buffer.
     */
    inline ssize_t receive(void *buf, size_t len)
        {return ::recv(so, (char *)buf, len, 0);};

    /**
     * See if input queue has data packets available.
     *
     * @return true if data packets available.
     * @param timeout in milliseconds.
     */
    inline bool isInputReady(timeout_t timeout = TIMEOUT_INF)
        {return Socket::isPending(Socket::pendingInput, timeout);};
};

/**
 * UDP duplex connections impliment a bi-directional point-to-point UDP
 * session between two peer hosts.  Two UDP sockets are typically used
 * on alternating port addresses to assure that sender and receiver
 * data does not collide or echo back.  A UDP Duplex is commonly used
 * for full duplex real-time streaming of UDP data between hosts.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Unreliable Datagram Peer Associations.
 */
class __EXPORT UDPDuplex : public UDPTransmit, public UDPReceive
{
public:
    /**
     * Create a UDP duplex as a pair of UDP simplex objects
     * bound to alternating and interconnected port addresses.
     *
     * @param bind address to bind this socket to.
     * @param port number to bind sender.
     */
    UDPDuplex(const IPV4Address &bind, tpport_t port);
#ifdef  CCXX_IPV6
    UDPDuplex(const IPV6Address &bind, tpport_t port);
#endif

    /**
     * Associate the duplex with a specified peer host. Both
     * the sender and receiver will be interconnected with
     * the remote host.
     *
     * @return 0 on success, error code on error.
     * @param host address to connect socket to.
     * @param port number to connect socket to.
     */
    Error connect(const IPV4Host &host, tpport_t port);
#ifdef  CCXX_IPV6
    Error connect(const IPV6Host &host, tpport_t port);
#endif

    /**
     * Disassociate this duplex from any host connection.  No data
     * should be read or written until a connection is established.
     *
     * @return 0 on success, error code on error.
     */
    Error disconnect(void);
};

END_NAMESPACE

#endif
