// Copyright (C) 2001,2002,2004 Federico Montesino Pouzols <fedemp@altern.org>.
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
// ccRTP.  If you copy code from other releases into a copy of GNU
// ccRTP, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU ccRTP, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

/**
 * @file ioqueue.h
 *
 * @short Generic RTP input/output queues.
 **/

#ifndef	CCXX_RTP_IOQUEUE_H_
#define CCXX_RTP_IOQUEUE_H_

#include <ccrtp/iqueue.h>
#include <ccrtp/oqueue.h>

#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

/**
 * @defgroup ioqueue Generic RTP input/output queues.
 * @{
 **/

/**
 * @class RTPDataQueue
 *
 * A packet queue handler for building different kinds of RTP protocol
 * systems.  The queue manages both incoming and outgoing RTP packets,
 * as well as synchronization and transmission/reception timers.  By
 * making the queue handler a seperate base class it becomes possible
 * to define RTP classes for RTP profiles and sessions of different
 * types.
 *
 * Outgoing packets are sent via the OutgoingDataQueue::putData method.
 *
 * Incoming packets can be retrieved via IncomingDataQueue::getData
 * method.
 *
 * @author David Sugar <dyfet@ostel.com>
 * @short RTP data queue handler.
 */
class __EXPORT RTPDataQueue :
	public IncomingDataQueue,
	public OutgoingDataQueue
{
public:
	/**
	 * @enum Tos rtp.h cc++/rtp.h
	 * @short Type of network service the application uses.
	 *
	 * If the application uses enhanced network service, for
	 * instance Integrated Services or Differentiated Services, it
	 * <em>has not</em> to ensure fair competition with TCP,
	 * provided that the requested service is actually being
	 * delivered.  Whenever the application uses best-effort
	 * service or the requested enhanced service is not actually
	 * being delivered, it <em>has</em> to ensure fair competition
	 * with TCP. By default, best-effot is assumed.
	 *
	 * @note Although not required, RTP packets are always sent on
	 * top of UDP segments. No other underlying transport protocol
	 * is supported at present.
	 *
	 * @todo implement fair competition with tcp
	 **/
	typedef enum {
		tosBestEffort,      ///< Best-effort network service
		tosEnhanced         ///< Enhanced network service
	}       Tos;

	/**
	 * Specify the kind of service the application expects to use.
	 *
	 * @param tos type of service the application expects to use
	 *
	 * @note If enhanced service is specified but packet loss is
	 * high (the requested service does not appear to actually be
	 * delivered) ccRTP defaults to best-effort suitable
	 * behaviour: guarantee fair competition with TCP.
	 *
	 * @todo Implement fair competition with tcp
	 **/
	inline void
	setTypeOfService(Tos tos)
	{ typeOfService = tos; }

	/**
	 * Enable packet queue processing in the stack. This method
	 * will not any thread of execution.
	 **/
	inline void enableStack()
	{ dataServiceActive = true; }

        /**
         * Disable packet queue processing in the stack.
         **/
        inline void disableStack()
        { dataServiceActive = false; }

	/**
	 * Get active connection state flag.
	 *
	 * @return true if connection "active".
	 */
	inline bool
	isActive() const
	{ return dataServiceActive; }

	/**
	 * Get the timestamp that should be given for a packet whose
	 * payload sampling instant corresponds to the current system
	 * time.
	 *
	 * The timestamp applications should provide for each packet
	 * represents the sampling instant of its payload and should
	 * not be a reading of the system clock. Nevertheless, the
	 * internal operation of the RTP stack relies on the accuracy
	 * of the provided timestamp, since several computations
	 * assume that there is a certain degree of correspondence
	 * between the timestamp and the system clock.
	 *
	 * It is recommended that applications use this method in
	 * order to <em>periodically adjust the RTP timestamp</em>.
	 *
	 * In particular, it is advisable getting the timestamp
	 * corresponding to the first sampling instant or any instant
	 * after a period of inactivity through a call to this method.
	 *
	 * Applications should use the nominal sampling or
	 * any other value provided by the coder in order to compute
	 * the next timestamps with minimum computational requirement.
	 *
	 * For instance, an application using an RTP profile that
	 * specifies a fixed sampling rate of 8 Khz with eight bits
	 * per sample, continuously transmitting audio blocks 80
	 * octets long, would transmit 100 packets every
	 * second. Every packet would carry a timestamp 80 units
	 * greater than the previous one. So, the first timestamp
	 * would be obtained from this method, whereas the following
	 * ones would be computed adding 80 every time. Also the
	 * timestamp should be increased for every block whether
	 * it is put in the queue or dropped.
	 *
	 * The aforementioned increment can be obtained from the
	 * RTPDataQueue::getTimestampIncrement() method rather than
	 * computing it by hand in the application.
	 *
	 * @note Frame based applications must follow a specific
	 * timestamping method, probably specified in a profile.
	 *
	 * @note You should take into account that by default ccRTP
	 * assumes that the application begins sampling at the queue
	 * creation time.  Moreover, the first sampling instant is
	 * assigned a "user visible" timestamp of 0, although the RTP
	 * stack will then add internally a ramdom offset unknown to
	 * the application.  That is to say, the application may count
	 * samples from 0 in order to get the timestamp for the next
	 * packet, provided that the first sampling instant is the
	 * same as the queue creation time.  Nevertheless, this
	 * simpler way of starting will not be as accurate as it would
	 * be if the application got at least the first timestamp
	 * through getCurrentTimestamp.  <em>We provide this option
	 * since ccRTP interface is evolving, but we admit that it is
	 * ugly, we could remove this option or even replace uint32
	 * timestamps with a restrictively regulated object;
	 * suggestions are gladly welcomed</em>
	 **/
	uint32
	getCurrentTimestamp() const;

	/**
	 * Specify the bandwidth of the current session.
	 *
	 * @param bw bandwidth of the current session, in bits/s.
	 *
	 * @see AVPQueue::setControlBandwidth()
	 */
	void
	setSessionBandwidth(uint32 bw)
	{ sessionBw = bw; }

	uint32
	getDefaultSessionBandwidth() const
	{ return defaultSessionBw; }

	uint32
	getSessionBandwidth() const
	{ return sessionBw; }

 	/**
 	 * Set the packet timeclock for synchronizing timestamps.
 	 **/
 	inline void
	setTimeclock()
	{ timeclock.setTimer(); }

 	/**
 	 * Get the packet timeclock for synchronizing timestamps.
 	 *
 	 * @return runtime in milliseconds since last set.
 	 */
 	inline timeout_t
	getTimeclock() const
	{ return timeclock.getElapsed(); }

protected:

	/**
	 * Constructor. This will generate a random application SSRC
	 * identifier.
	 *
	 * @param size an estimation of the number of participants in
	 * the session
	 **/
	RTPDataQueue(uint32 size = defaultMembersHashSize);

	/**
	 * Using this constructor you can start a session with the
	 * given ssrc, instead of the usual randomly generated
	 * one. This is necessary when you need to initiate several
	 * sessions having the same SSRC identifier, for instance, to
	 * implement layered encoding, in which case each layer is
	 * managed through a different session but all sessions share
	 * the same SSRC identifier.
	 *
	 * @warning This doesn't seem to be a good solution
	 *
	 * @param ssrc Synchronization SouRCe identifier for this session
	 * @param size an estimation of the number of participants in the
	 *        session
	 */
	RTPDataQueue(uint32* ssrc, uint32 size = defaultMembersHashSize);

	/**
	 * The queue destructor flushes the queue and stops all
	 * services.
	 */
	inline virtual
	~RTPDataQueue()
	{ endQueue(); }

        /**
         * A plugin point for timer tick driven events.
         */
        inline virtual void
	timerTick()
	{ return; }

	void renewLocalSSRC()
		{IncomingDataQueue::renewLocalSSRC();}

private:
	RTPDataQueue(const RTPDataQueue &o);

	RTPDataQueue&
	operator=(const RTPDataQueue &o);

	/**
	 * Global queue initialization.
	 *
	 * @param localSSRC local 32-bit SSRC identifier
	 **/
	void
	initQueue();

protected:
	/**
	 * This method ends the queue.
	 */
	void
	endQueue();

	/**
	 * This function is used to check for and schedule against
	 * arriving packets based on the derived connection type.
	 *
	 * @return true if packet waiting for processing.
	 * @param number of microseconds to wait.
	 */
	virtual bool
	isPendingData(microtimeout_t timeout) = 0;

private:
	// true if connection "active"
	volatile bool dataServiceActive;
	Tos typeOfService;
	TimerPort timeclock;
	/* RTP session bandwidth control */
	static const uint32 defaultSessionBw;
	uint32 sessionBw;


};

/** @}*/ // ioqueue

#ifdef  CCXX_NAMESPACES
}
#endif

#endif  //CCXX_RTP_IOQUEUE_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
