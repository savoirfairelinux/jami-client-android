/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Pierre-Luc Beaudoin <pierre-luc.beaudoin@savoirfairelinux.com>
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Guillaume Carmel-Archambault <guillaume.carmel-archambault@savoirfairelinux.com>
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

#include "client/configurationmanager.h"
#include "jni_callbacks.h"

ConfigurationManager::ConfigurationManager() {}

void ConfigurationManager::accountsChanged()
{
    on_accounts_changed_wrapper();
}

void ConfigurationManager::historyChanged()
{

}

void ConfigurationManager::stunStatusFailure(const std::string& accountID)
{

}

void ConfigurationManager::volumeChanged(const std::string&, const int&)
{
}

void ConfigurationManager::registrationStateChanged(const std::string& accountID, int const& state)
{
    on_account_state_changed_wrapper(accountID, state);
}

void ConfigurationManager::sipRegistrationStateChanged(const std::string& accountID, const std::string& state, const int32_t& code)
{
    on_account_state_changed_with_code_wrapper(accountID, state, code);
}

void ConfigurationManager::errorAlert(const int & /*alert*/)
{
}

std::vector< int32_t > ConfigurationManager::getHardwareAudioFormat()
{
    return get_hardware_audio_format_wrapper();
}

std::vector<std::string> ConfigurationManager::getSupportedAudioManagers()
{
    return {"opensl"};
}
