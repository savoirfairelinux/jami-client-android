/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Alexandre Bourget <alexandre.bourget@savoirfairelinux.com>
 *  Author: Yan Morin <yan.morin@savoirfairelinux.com>
 *  Author : Laurielle Lea <laurielle.lea@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

#include "sipcall.h"
#include "logger.h" // for _debug
#include "sdp.h"
#include "manager.h"
#ifdef SFL_VIDEO
#include "dbus/video_controls.h"
#endif

namespace {
    static const int INITIAL_SIZE = 16384;
    static const int INCREMENT_SIZE = INITIAL_SIZE;
}

SIPCall::SIPCall(const std::string& id, Call::CallType type,
                 pj_caching_pool *caching_pool) : Call(id, type)
    , inv(NULL)
    , audiortp_(this)
#ifdef SFL_VIDEO
    // The ID is used to associate video streams to calls
    , videortp_(id, Manager::instance().getDbusManager()->getVideoControls()->getSettings())
#endif
    , pool_(pj_pool_create(&caching_pool->factory, id.c_str(), INITIAL_SIZE, INCREMENT_SIZE, NULL))
    , local_sdp_(new Sdp(pool_))
{}

SIPCall::~SIPCall()
{
    delete local_sdp_;
    pj_pool_release(pool_);
}

void SIPCall::answer()
{
    pjsip_tx_data *tdata;
    if (pjsip_inv_answer(inv, PJSIP_SC_OK, NULL, NULL, &tdata) != PJ_SUCCESS)
        throw std::runtime_error("Could not init invite request answer (200 OK)");

    if (pjsip_inv_send_msg(inv, tdata) != PJ_SUCCESS)
        throw std::runtime_error("Could not send invite request answer (200 OK)");

    setConnectionState(CONNECTED);
    setState(ACTIVE);
}
