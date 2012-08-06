/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Pierre-Luc Beaudoin <pierre-luc.beaudoin@savoirfairelinux.com>
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Guillaume Carmel-Archambault <guillaume.carmel-archambault@savoirfairelinux.com>
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

#include "video_controls.h"
#include "video/libav_utils.h"
#include "video/video_preview.h"
#include "account.h"
#include "logger.h"
#include "manager.h"

namespace {
const char * const SERVER_PATH = "/org/sflphone/SFLphone/VideoControls";
}

VideoControls::VideoControls(DBus::Connection& connection) :
    DBus::ObjectAdaptor(connection, SERVER_PATH), preview_(), videoPreference_()
{
    // initialize libav libraries
    libav_utils::sfl_avcodec_init();
}

VideoPreference &
VideoControls::getVideoPreferences()
{
    return videoPreference_;
}

std::vector<std::map<std::string, std::string> >
VideoControls::getCodecs(const std::string &accountID)
{
    Account *acc = Manager::instance().getAccount(accountID);

    if (acc != NULL)
        return acc->getAllVideoCodecs();
    else
        return std::vector<std::map<std::string, std::string> >();
}

void
VideoControls::setCodecs(const std::string& accountID,
                         const std::vector<std::map<std::string, std::string> > &details)
{
    Account *acc = Manager::instance().getAccount(accountID);
    if (acc != NULL) {
        acc->setVideoCodecs(details);
        Manager::instance().saveConfig();
    }
}

std::vector<std::string>
VideoControls::getDeviceList()
{
    return videoPreference_.getDeviceList();
}

std::vector<std::string>
VideoControls::getDeviceChannelList(const std::string &dev)
{
    return videoPreference_.getChannelList(dev);
}

std::vector<std::string>
VideoControls::getDeviceSizeList(const std::string &dev, const std::string &channel)
{
    return videoPreference_.getSizeList(dev, channel);
}

std::vector<std::string>
VideoControls::getDeviceRateList(const std::string &dev, const std::string &channel, const std::string &size)
{
    return videoPreference_.getRateList(dev, channel, size);
}

std::string
VideoControls::getActiveDevice()
{
    return videoPreference_.getDevice();
}

std::string
VideoControls::getActiveDeviceChannel()
{
    return videoPreference_.getChannel();
}

std::string
VideoControls::getActiveDeviceSize()
{
    return videoPreference_.getSize();
}

std::string
VideoControls::getActiveDeviceRate()
{
    return videoPreference_.getRate();
}

void
VideoControls::setActiveDevice(const std::string &device)
{
    DEBUG("Setting device to %s", device.c_str());
    videoPreference_.setDevice(device);
}

void
VideoControls::setActiveDeviceChannel(const std::string &channel)
{
    videoPreference_.setChannel(channel);
}

void
VideoControls::setActiveDeviceSize(const std::string &size)
{
    videoPreference_.setSize(size);
}

void
VideoControls::setActiveDeviceRate(const std::string &rate)
{
    videoPreference_.setRate(rate);
}

std::map<std::string, std::string>
VideoControls::getSettings() {
    return videoPreference_.getSettings();
}

void
VideoControls::startPreview()
{
    if (preview_.get()) {
        ERROR("Video preview was already started!");
        return;
    }

    using std::map;
    using std::string;

    map<string, string> args(videoPreference_.getSettings());
    preview_.reset(new sfl_video::VideoPreview(args));
}

void
VideoControls::stopPreview()
{
    if (preview_.get()) {
        DEBUG("Stopping video preview");
        preview_.reset();
    } else {
        WARN("Video preview was already stopped");
    }
}

bool
VideoControls::hasPreviewStarted()
{
    return preview_.get() != 0;
}

std::string
VideoControls::getCurrentCodecName(const std::string &callID)
{
    return Manager::instance().getCurrentVideoCodecName(callID);
}

