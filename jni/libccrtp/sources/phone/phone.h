// Copyright (C) 2000-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2008 David Sugar, Tycho Softworks.
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
 
#ifndef CCXX_PHONE_H_
#define CCXX_PHONE_H_
 
#ifndef CCXX_RTP_H_
#include <ccrtp/rtp.h>
#endif
 
#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

/**
 * Load /etc/phone.conf [thread] key value pairs.  Has internal defaults
 * if section or file is missing.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Load keythreads priority and session count configuration.
 */
class KeyRTP : public Keydata
{
public:
        /**
         * Initialize keythread data.
         */
        KeyRTP();
	
	/**
	 * Get unicast address.
	 */
	inline InetHostAddress getInterface(void)
		{return InetHostAddress(getLast("interface"));};
	
	/**
	 * Get binding port number.
	 */
	inline tpport_t getPort(void)
		{return (tpport_t) atoi(getLast("port"));};

	/**
	 * Get stack timer.
	 */
	inline microtimeout_t getTimer(void)
		{return (microtimeout_t)atol(getLast("timer")) * 1000l;};

	/**
	 * Get packet expiration timer.
	 */
	inline microtimeout_t getExpire(void)
		{return (microtimeout_t)atol(getLast("expire")) * 1000l;};
};

/**
 * Load /etc/phone.conf [audio] key value pairs.  Has internal defaults
 * if section or file is missing.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Load keythreads priority and session count configuration.
 */
class KeyAudio : public Keydata
{
public:
        /**
         * Initialize keythread data.
         */
        KeyAudio();
};

/**
 * Load /etc/phone.conf [thread] key value pairs.  Has internal defaults
 * if section or file is missing.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short Load keythreads priority and session count configuration.
 */
class KeyThreads : public Keydata
{
public:
        /**
         * Initialize keythread data.
         */
        KeyThreads();
 
        /**
         * Get relative priority to run service threads at.
         *
         * @return audio thread priority (relative).
         */
        inline int priAudio(void)
                {return atoi(getLast("audio"));};

        /**
         * Get relative priority for the rtp stack.
         *
         * @return audio thread priority (relative).
         */
        inline int priRTP(void)
                {return atoi(getLast("rtp"));};

        /**
         * Get relative process priority.
         *
         * @return rtp stack thread priority (relative).
         */
        inline int getPriority(void)
                {return atoi(getLast("priority"));};

        /**
         * Get thread stack frame size.
         *
         * @return thread stack frame in k.
         */
        inline unsigned getStack(void)
                {return atoi(getLast("stack"));};
		

        /**
         * Get scheduler policy to use.
         *
         * @return scheduler policy.
         */
        inline const char *getPolicy(void)
                {return getLast("priority");};
};	

/**
 * Process RTP Events for plugins and special purpose classes.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short RTP event processing.
 */
class RTPEvent
{
private:
	friend class RTPAudio;

	static RTPEvent *first;
	RTPEvent *next;

protected:
	RTPEvent();

	virtual void gotHello(const SyncSource &src)
		{return;};

	virtual void gotGoodbye(const SyncSource &src, 
				const std::string& reason)
		{return;};
};

/**
 * This is the base session stack that will maintain all network audio
 * activity.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short RTP stack for network audio. 
 */
class RTPAudio : public RTPSocket
{
private:
	unsigned groups;	// multicast groups joined
	bool	unicast;	// indicate if in unicast call
	bool	shutdown;	// tracks shutdown state

	void onGotHello(const SyncSource &src);
	void onGotGoodbye(const SyncSource &src, const std::string& reason);

public:
	RTPAudio();

	void exit(const char *reason);
};

/**
 * This is the base interface for DSO loadable audio devices.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short base class for Audio devices. 
 */
class DevAudio 
{
protected:
	DevAudio();

public:
	virtual void open(void) = 0;	// open device channel
	virtual void close(void) = 0;	// close device channel
};

extern bool multicast;
extern bool daemon;
extern KeyThreads keythreads;
extern KeyRTP keyrtp;
extern KeyAudio keyaudio;
extern RTPAudio *rtp;
extern DevAudio *audio;

#ifdef	CCXX_NAMESPACES
}
#endif

#endif
