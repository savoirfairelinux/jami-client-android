// Copyright (C) 2001,2002,2003,2004 Federico Montesino Pouzols <fedemp@altern.org>.
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
 * @file sources.h 
 *
 * @short Sources of synchronization and participants related clases.
 **/

#ifndef	CCXX_RTP_SOURCES_H_
#define CCXX_RTP_SOURCES_H_

#include <string>
#include <ccrtp/rtcppkt.h>

#ifdef	CCXX_NAMESPACES
namespace ost {
#endif

/**
 * @defgroup sources Participants and synchronization sources.
 * @{
 **/

/**
 * @class SDESItemsHolder
 *
 * Holds the SDES items and related information from a participant in
 * an RTP application. This is a base class for participant classes.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT SDESItemsHolder
{
public:
	const std::string&
	getItem(SDESItemType type) const;

	inline const std::string& 
	getPRIVPrefix() const
	{ return sdesItems[SDESItemTypeEND]; }

	void
	setItem(SDESItemType item, const std::string& val);
	
	inline void
	setPRIVPrefix(const std::string& val)
	{ sdesItems[SDESItemTypeEND] = val; }

protected:
	SDESItemsHolder()
	{ }
	
	inline virtual ~SDESItemsHolder()
	{ }
	
private:
	// SDES items for a participant.
	// sdesItems[0] (== sdesItems[SDESItemTypeEND]) holds the prefix
	// value for the PRIV item. The rest of entries hold the
	// correponding SDES item value.
	std::string sdesItems[SDESItemTypeLast + 1];
};

/** 
 * @class Participant
 * @short A class of objects representing remote participants (RTP
 * applications) in a multimedia session.
 * 
 * Any RTP socket/queue class that directly or indirectly inherits
 * from QueueRTCPManager (and hence has RTCP support) will represent
 * participants from which any RTP or RTCP packet has been received
 * through a Participant object.  These Participant objects are
 * entities such as end systems (user applications, monitors, etc),
 * RTP mixers and RTP translators.
 *
 * Participant objects are identified by a CNAME and provide access to
 * all known data about the source of RTP/RTCP packets, such as the
 * CNAME and any other SDES item. Each participant object is related
 * to one or more synchronization objects (@see SyncSource).
 *
 * If an RTP application based on ccRTP receives packets from itself
 * (for instance, it is included in the destination list), there will
 * be a Participant object that corresponds to the "local participant"
 * (RTPApplication) object.
 * 
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 *
 * @todo implement reference counting from sources, so that when a
 * source is destroyed, we know if the Participant should be
 * destroyed.
 **/
class __EXPORT Participant : private SDESItemsHolder
{
public:
	/**
	 * Get the value of an SDES item. For instance,
	 * getSDESItem(SDESItemTypeCNAME), return the CNAME of this
	 * Participant.
	 *
	 * @param type type of SDES item to get value of.
	 *
	 * @return value of the SDES item as a string. 
	 * @retval empty string when the value is not known (no RTCP
	 * packet with the requested SDES item has been received from this
	 * source).
	 **/
	const std::string&
	getSDESItem(SDESItemType type) const
	{ return SDESItemsHolder::getItem(type); }

	/**
	 * Get the prefix value for the PRIV SDES item.
	 *
	 * @return PRIV SDES item prefix as a string.
	 * @retval empty string when no PRIV SDES item has been
	 * received from this source.
	 **/
	inline const std::string& 
	getPRIVPrefix() const
	{ return SDESItemsHolder::getPRIVPrefix(); }

	/** 
	 * Construct a new participant.
	 *
	 * @param cname Unique CNAME identifier.
	 **/
	Participant(const std::string& cname);

	~Participant();

private:
	friend class ParticipantHandler;

	/**
	 * Set the value of a SDES item.
	 **/
	inline void
	setSDESItem(SDESItemType item, const std::string& val)
	{ SDESItemsHolder::setItem(item,val); }

	/**
	 * Set prefix value for the PRIV SDES item.
	 **/
	inline void
	setPRIVPrefix(const std::string val)
	{ SDESItemsHolder::setPRIVPrefix(val); }
};

/**
 * @class SyncSource
 * @short Synchronization source in an RTP session
 *
 * Each synchronization source in an RTP session is identified by a
 * 32-bit numeric SSRC identifier. Each SyncSource object is related
 * to a Participant object, which can be retrieved through the
 * getParticipant() method.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT SyncSource
{
public:
	/**
	 * @enum State
	 *
	 * @short Synchronization source states during an RTP session.
	 *
	 * In general, new synchronization sources are not considered
	 * valid until multiple valid data packets or a valid RTCP
	 * compound packet has been received from the new source (@see
	 * IncomingDataQueue::setMinValidPacketSequence()). Thus, the
	 * source will probably be in statePrevalid before reaching
	 * one of the two states that indicate a valid source:
	 * stateActive and stateInactive.
	 *
	 * A valid participant is in stateActive state if RTP and/or
	 * RTCP packets are currently being received from it. If,
	 * after a small number of RTCP report intervals (see
	 * IncomingDataQueue::setSourceExpirationPeriod() ), no
	 * packets are received, it will reach the stateInactive
	 * state. If, after a small number of RTCP report intervals,
	 * no packet is received from an inactive source, it will be
	 * deleted.
	 *
	 * If RTCP is being used, after receiving a BYE RTCP packet
	 * from a synchronization source, it will reach the
	 * stateLeaving state and will be deleted after a delay (see
	 * QueueRTCPManager::setLeavingDelay()).
	 *
	 * Sources in statePrevalid and stateLeaving are not counted
	 * for the number of session members estimation.
	 **/
	typedef enum {
		stateUnknown,     ///< No valid packet has been received.
		statePrevalid,    ///< Some packets have been
				  ///received, but source validity not
				  ///yet guaranteed.
		stateActive,      ///< We currently receive packets
				  ///(data or control) from this source.
		stateInactive,    ///< Was active in the near past but
				  ///no packet from this source has
				  ///been received lately.
		stateLeaving      ///< An RTCP BYE has been received
				  ///from the source.
	}       State;

	/**
	 * @param ssrc SSRC identifier of the source, unique in each
	 * session.
	 */
	SyncSource(uint32 ssrc); 

	~SyncSource();

	State
	getState() const
	{ return state; }

	/**
	 * Whether this source sends RTP data packets.
	 **/
	bool isSender() const
	{ return activeSender; }

	uint32 getID() const
	{ return SSRC; }

	/**
	 * Get the participant this synchronization source is
	 * asociated to.
	 *
	 * @retval NULL if the stack has not been yet able to identify
	 * the participant this source is associated to.
	 **/
	inline Participant*
	getParticipant() const
	{ return participant; }

	tpport_t getDataTransportPort() const
	{ return dataTransportPort; }

	tpport_t getControlTransportPort() const
	{ return controlTransportPort; }
	
	const InetAddress& getNetworkAddress() const
	{ return networkAddress; }

protected:
	/**
	 * @param source The RTPSource object being copied
	 */
	SyncSource(const SyncSource& source);

	SyncSource&
	operator=(const SyncSource& source);

private:
	friend class SyncSourceHandler;

	inline void
	setState(State st)
	{ state = st; }

	/**
	 * Mark this source as an active sender.
	 **/
	inline void
	setSender(bool active)
	{ activeSender = active; }

	inline void
	setParticipant(Participant& p)
	{ participant = &p; }
	
	void setDataTransportPort(tpport_t p)
	{ dataTransportPort = p; }

	void setControlTransportPort(tpport_t p)
	{ controlTransportPort = p; }

	void setNetworkAddress(InetAddress addr)
	{ networkAddress = addr; }

	inline void
	setLink(void *l)
	{ link = l; }

	void *getLink() const
	{ return link; }

	// validity state of this source
	State state;
	// 32-bit SSRC identifier.
	uint32 SSRC;
	// A valid source not always is active
        bool activeSender;
	// The corresponding participant.
	Participant* participant;

	// Network protocol address for data and control connection
	// (both are assumed to be the same).
	InetAddress networkAddress;
	tpport_t dataTransportPort;
	tpport_t controlTransportPort;

	// Pointer to the SyncSourceLink or similar object in the
	// service queue. Saves a lot of searches in the membership
	// table.
	void* link;
};

/**
 * @class RTPApplication
 * @short An RTP application, holding identifying RTCP SDES item
 * values. Represents local participants.
 *
 * An application in the context of RTP: an entity that has a CNAME
 * (unique identifier in the form of user\@host.domain) as well as
 * other RTCP SDES items (such as NAME or TOOL), and may open a number
 * of RTP sessions. Each application is a different source of
 * synchronization (with a potentially diferent SSRC identifier) in
 * each RTP session it participates. All the sources of
 * synchronization from a participant are tied together by means of
 * the CNAME.
 *
 * The definition of this class allows applications based on ccRTP to
 * implement several "RTP applications" in the same process. Each
 * object of this class represents a local participant.
 *
 * @author Federico Montesino Pouzols <fedemp@altern.org>
 **/
class __EXPORT RTPApplication : private SDESItemsHolder
{
private:
	struct ParticipantLink;

public:
	/**
	 * Create a new RTP application. If the CNAME string provided
	 * has zero length, it is guessed from the user and machine
	 * name.
	 *
	 * @param cname Local participant canonical name.
	 **/
	RTPApplication(const std::string& cname);

	~RTPApplication();

	inline void
	setSDESItem(SDESItemType item, const std::string& val)
	{ SDESItemsHolder::setItem(item,val); }

	inline void
	setPRIVPrefix(const std::string& val)
	{ SDESItemsHolder::setPRIVPrefix(val); }

	const std::string&
	getSDESItem(SDESItemType item) const
	{ return SDESItemsHolder::getItem(item); }

	inline const std::string&
	getPRIVPrefix() const
	{ return SDESItemsHolder::getPRIVPrefix(); }

	/**
	 * Iterator through the list of participants in this
	 * session. Somehow resembles and standard const_iterator
	 **/
	class ParticipantsIterator
	{
	public:
		typedef std::forward_iterator_tag iterator_category;
		typedef Participant value_type;
		typedef ptrdiff_t difference_type;
		typedef const Participant* pointer;
		typedef const Participant& reference;

		ParticipantsIterator(ParticipantLink* p = NULL) : 
			link(p) 
		{ }

		ParticipantsIterator(const ParticipantsIterator& pi) : 
			link(pi.link)
		{ }

		reference operator*() const
		{ return *(link->getParticipant()); }

		pointer operator->() const
		{ return link->getParticipant(); }

		ParticipantsIterator& operator++() {
			link = link->getNext();
			return *this;
		}

		ParticipantsIterator operator++(int) {
			ParticipantsIterator result(*this);
			++(*this);
			return result;
		}
		friend bool operator==(const ParticipantsIterator& l,
				       const ParticipantsIterator& r)
		{ return l.link == r.link; }

		friend bool operator!=(const ParticipantsIterator& l,
				       const ParticipantsIterator& r)
		{ return l.link != r.link; }
	private:
		ParticipantLink *link;
	};

	ParticipantsIterator begin()
	{ return ParticipantsIterator(firstPart); }
	
	ParticipantsIterator end()
	{ return ParticipantsIterator(NULL); }

	const Participant*
	getParticipant(const std::string& cname) const;
	
private:
	friend class ApplicationHandler;

	struct ParticipantLink {
		ParticipantLink(Participant& p, 
				ParticipantLink* l) :
			participant(&p), next(l) 
		{ }
		inline ~ParticipantLink() { delete participant; }
		inline Participant* getParticipant() { return participant; }
		inline ParticipantLink* getPrev() { return prev; }
		inline ParticipantLink* getNext() { return next; }
		inline void setPrev(ParticipantLink* l) { prev = l; }
		inline void setNext(ParticipantLink* l) { next = l; }
		Participant* participant;
		ParticipantLink* next, *prev;
	};

	void
	addParticipant(Participant& part);

	void
	removeParticipant(ParticipantLink* part);

	/**
	 * Find out the local CNAME as user\@host and store it as part
	 * of the internal state of this class.  
	 */
	void
	findCNAME();

	/// Hash table with sources of RTP and RTCP packets.
	static const size_t defaultParticipantsNum;
	Participant** participants;
	/// List of participants, ordered from older to newer.
	ParticipantLink* firstPart, * lastPart;
};

/**
 * Get the RTPApplication object for the "default" application (the
 * only one used by common applications -those that only implement one
 * "RTP application"). Note that this application object differs from
 * all the others that may be defined in that it is automatically
 * constructed by the ccRTP stack and its CNAME is automatically
 * assigned (as user\@host), whereas the other application objects'
 * CNAME is provided to its constructor.
 **/
__EXPORT RTPApplication& defaultApplication();

/** @}*/ // sources

#ifdef CCXX_NAMESPACES
}
#endif

#endif //CCXX_RTP_SOURCES_H_

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */
