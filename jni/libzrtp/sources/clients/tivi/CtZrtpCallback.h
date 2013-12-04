/*
 * Tivi client glue code for ZRTP.
 * Copyright (c) 2012 Slient Circle LLC.  All rights reserved.
 *
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */

/**
 * Interfaces for Tivi callback classes.
 *
 * @author: Werner Dittmann <Werner.Dittmann@t-online.de>
 */

#ifndef _CTZRTPCALLBACK_H_
#define _CTZRTPCALLBACK_H_

#include <CtZrtpSession.h>

/**
 * @brief Tivi callback functions for state changes, warnings, and enrollment.
 *
 * The @c CtZrpSession and @c CtZrtpStream classes use these callbacks to inform
 * the Tivi client about a new ZRTP state, if a @c Warning occured or if the
 * client should display the @c Enrollment GUI.
 */
class __EXPORT CtZrtpCb {
public:
    /**
     * @brief Destructor.
     * Define a virtual destructor to enable cleanup in derived classes.
     */
    virtual ~CtZrtpCb() {};

    virtual void onNewZrtpStatus(CtZrtpSession *session, char *p, CtZrtpSession::streamName streamNm) =0;
    virtual void onNeedEnroll(CtZrtpSession *session, CtZrtpSession::streamName streamNm, int32_t info) =0;
    virtual void onPeer(CtZrtpSession *session, char *name, int iIsVerified, CtZrtpSession::streamName streamNm) =0;
    virtual void onZrtpWarning(CtZrtpSession *session, char *p, CtZrtpSession::streamName streamNm) =0;
};


/**
 * @brief Tivi callback function to send a ZRTP packet via the RTP session.
 *
 * The @c CtZrtpStream class uses this callback to send a ZRTP packet via Tivi's
 * RTP session.
 */
class __EXPORT CtZrtpSendCb {
public:
    /**
     * @brief Destructor.
     * Define a virtual destructor to enable cleanup in derived classes.
     */
    virtual ~CtZrtpSendCb() {};

    virtual void sendRtp(CtZrtpSession const *session, uint8_t* packet, size_t length, CtZrtpSession::streamName streamNm) =0;
};

#endif