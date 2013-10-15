// Copyright (C) 2001,2002,2003,2004,2005 Federico Montesino <fedemp@altern.org>
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
 * @file control.cpp
 *
 * @short SDESItemsHolder, RTPSource and Participant classes implementation.
 **/

#include "private.h"
#include <ccrtp/sources.h>

#ifdef  CCXX_NAMESPACES
namespace ost {
#endif

#ifndef HAVE_GETTIMEOFDAY
#ifdef WIN32
int gettimeofday(struct timeval *tv_,  void *tz_)
{
    // We could use _ftime(), but it is not available on WinCE.
    // (WinCE also lacks time.h)
    // Note also that the average error of _ftime is around 20 ms :)
    DWORD ms = GetTickCount();
    tv_->tv_sec = ms / 1000;
    tv_->tv_usec = ms * 1000;
    return 0;
}
#endif //WIN32
#endif

static void
findusername(std::string &username);

#ifndef WIN32
static void
findusername(std::string &username)
{
    // LOGNAME environment var has two advantages:
    // 1) avoids problems of getlogin(3) and cuserid(3)
    // 2) unlike getpwuid, takes into account user
    //    customization of the environment.
    // Try both LOGNAME and USER env. var.
    const char *user = Process::getEnv("LOGNAME");
    if ( !user || !strcmp(user,"") )
        user = Process::getEnv("USER");
    if ( !user || !strcmp(user,"") )
        username = Process::getUser();

    if ( user )
        username = user;
    else
        username = "";
}

#else

static void
findusername(std::string &username)
{
    unsigned long len = 0;
    if ( GetUserName(NULL,&len) && (len > 0) ) {
        char *n = new char[len];
        GetUserName(n,&len);
        username = n;
        delete [] n;
    } else {
        username = "";
    }
}
#endif // #ifndef WIN32

void
SDESItemsHolder::setItem(SDESItemType item, const std::string& val)
{
    if ( item > SDESItemTypeEND && item <= SDESItemTypeH323CADDR ) {
        sdesItems[item] = val;
    }
}

const std::string&
SDESItemsHolder::getItem(SDESItemType type) const
{
    if ( type > SDESItemTypeEND && type <= SDESItemTypeH323CADDR ) {
        return sdesItems[type];
    } else
        return sdesItems[SDESItemTypeCNAME];
}

SyncSource::SyncSource(uint32 ssrc) :
state(stateUnknown), SSRC(ssrc), participant(NULL),
networkAddress("0"), dataTransportPort(0), controlTransportPort(0)
{}

SyncSource::~SyncSource()
{
    activeSender = false;
    state = statePrevalid;
}

Participant::Participant(const std::string& cname) : SDESItemsHolder()
{
    SDESItemsHolder::setItem(SDESItemTypeCNAME,cname);
}

Participant::~Participant()
{}

const size_t RTPApplication::defaultParticipantsNum = 11;

RTPApplication& defaultApplication()
{
    // default application CNAME is automatically assigned.
    static RTPApplication defApp("");

    return defApp;
}

RTPApplication::RTPApplication(const std::string& cname) :
SDESItemsHolder(), participants( new Participant* [defaultParticipantsNum] ),
firstPart(NULL), lastPart(NULL)
{
    // guess CNAME, in the form of user@host_fqn
    if ( cname.length() > 0 )
        SDESItemsHolder::setItem(SDESItemTypeCNAME,cname);
    else
        findCNAME();
}

RTPApplication::~RTPApplication()
{
    ParticipantLink *p;
    while ( NULL != firstPart ) {
        p = firstPart;
        firstPart = firstPart->getNext();
#ifdef  CCXX_EXCEPTIONS
        try {
#endif
            delete p;
#ifdef  CCXX_EXCEPTIONS
        } catch (...) {}
#endif
    }
    lastPart = NULL;
#ifdef  CCXX_EXCEPTIONS
    try {
#endif
        delete [] participants;
#ifdef  CCXX_EXCEPTIONS
    } catch (...) {}
#endif
}

// TODO: it should be implemented using the participant iterators
const Participant*
RTPApplication::getParticipant(const std::string& cname) const
{
    ParticipantLink* pl = firstPart;
    while ( (NULL != pl) &&
        ( pl->getParticipant()->getSDESItem(SDESItemTypeCNAME)
          != cname) ) {
        pl = pl->getNext();
    }
    if ( pl ) {
        return pl->getParticipant();
    } else {
        return NULL;
    }
}

void
RTPApplication::addParticipant(Participant& part)
{
    ParticipantLink* pl = new ParticipantLink(part,NULL);
    if ( NULL == firstPart )
        firstPart = lastPart = pl;
    else
        lastPart->setNext(pl);
    lastPart = pl;
}

void
RTPApplication::removeParticipant(ParticipantLink* pl)
{
    if ( NULL == pl )
        return;
    if ( pl->getPrev() )
        pl->getPrev()->setNext(pl->getNext());
    if ( pl->getNext() )
        pl->getNext()->setPrev(pl->getPrev());
    delete pl;
}

void
RTPApplication::findCNAME()
{
    // build string username@host_fqn
    std::string username;
    findusername(username);

    // First create an InetHostAddress object, otherwise the
    // object will be destructed and the hostname corrupted.
    InetHostAddress iha;
    const char *p = iha.getHostname();
    // Returned hostname can be NULL
    std::string hname;
    if (p) hname = p;

    setSDESItem(SDESItemTypeCNAME,
            username + "@" + hname);
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
