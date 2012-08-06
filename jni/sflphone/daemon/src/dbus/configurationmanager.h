/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Pierre-Luc Beaudoin <pierre-luc.beaudoin@savoirfairelinux.com>
 *  Author: Alexandre Bourget <alexandre.bourget@savoirfairelinux.com>
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

#ifndef CONFIGURATIONMANAGER_H
#define CONFIGURATIONMANAGER_H

#if __GNUC__ >= 4 && __GNUC_MINOR__ >= 6
#pragma GCC diagnostic ignored "-Wunused-but-set-variable"
#endif

#pragma GCC diagnostic ignored "-Wignored-qualifiers"
#pragma GCC diagnostic ignored "-Wunused-parameter"
#pragma GCC diagnostic ignored "-Weffc++"
#include "configurationmanager-glue.h"
#pragma GCC diagnostic warning "-Wignored-qualifiers"
#pragma GCC diagnostic warning "-Wunused-parameter"
#pragma GCC diagnostic warning "-Weffc++"

#if __GNUC__ >= 4 && __GNUC_MINOR__ >= 6
#pragma GCC diagnostic warning "-Wunused-but-set-variable"
#endif

#include "dbus_cpp.h"

class ConfigurationManager
    : public org::sflphone::SFLphone::ConfigurationManager_adaptor,
    public DBus::IntrospectableAdaptor,
    public DBus::ObjectAdaptor {

    public:
        ConfigurationManager(DBus::Connection& connection);
        std::map< std::string, std::string > getAccountDetails(const std::string& accountID);
        void setAccountDetails(const std::string& accountID, const std::map< std::string, std::string >& details);
        std::string addAccount(const std::map< std::string, std::string >& details);
        void removeAccount(const std::string& accoundID);
        void deleteAllCredential(const std::string& accountID);
        std::vector< std::string > getAccountList();
        void sendRegister(const std::string& accoundID, const bool& enable);
        void registerAllAccounts(void);

        std::map< std::string, std::string > getTlsSettingsDefault();

        std::vector< int32_t > getAudioCodecList();
        std::vector< std::string > getSupportedTlsMethod();
        std::vector< std::string > getAudioCodecDetails(const int32_t& payload);
        std::vector< int32_t > getActiveAudioCodecList(const std::string& accountID);

        void setActiveAudioCodecList(const std::vector< std::string >& list, const std::string& accountID);

        std::vector< std::string > getAudioPluginList();
        void setAudioPlugin(const std::string& audioPlugin);
        std::vector< std::string > getAudioOutputDeviceList();
        void setAudioOutputDevice(const int32_t& index);
        void setAudioInputDevice(const int32_t& index);
        void setAudioRingtoneDevice(const int32_t& index);
        std::vector< std::string > getAudioInputDeviceList();
        std::vector< std::string > getCurrentAudioDevicesIndex();
        int32_t getAudioDeviceIndex(const std::string& name);
        std::string getCurrentAudioOutputPlugin();
        std::string getNoiseSuppressState();
        void setNoiseSuppressState(const std::string& state);
        std::string getEchoCancelState();
        void setEchoCancelState(const std::string& state);
        void setEchoCancelTailLength(const int32_t& length);
        int getEchoCancelTailLength();
        void setEchoCancelDelay(const int32_t& length);
        int getEchoCancelDelay();

        std::map<std::string, std::string> getRingtoneList();

        std::string getAudioManager();
        void setAudioManager(const std::string& api);

        int32_t isIax2Enabled();
        std::string getRecordPath();
        void setRecordPath(const std::string& recPath);
        bool getIsAlwaysRecording();
        void setIsAlwaysRecording(const bool& rec);

        void setHistoryLimit(const int32_t& days);
        int32_t getHistoryLimit();
        void clearHistory();

        int32_t getMailNotify();
        void setMailNotify();


        std::map<std::string, int32_t> getAddressbookSettings();
        void setAddressbookSettings(const std::map<std::string, int32_t>& settings);
        std::vector< std::string > getAddressbookList();
        void setAddressbookList(const std::vector< std::string >& list);

        void setAccountsOrder(const std::string& order);

        std::map<std::string, std::string> getHookSettings();
        void setHookSettings(const std::map<std::string, std::string>& settings);

        std::vector<std::map<std::string, std::string> > getHistory();

        std::map<std::string, std::string> getTlsSettings();
        void setTlsSettings(const std::map< std::string, std::string >& details);
        std::map< std::string, std::string > getIp2IpDetails();

        std::vector< std::map< std::string, std::string > > getCredentials(const std::string& accountID);
        void setCredentials(const std::string& accountID, const std::vector< std::map< std::string, std::string > >& details);

        std::string getAddrFromInterfaceName(const std::string& interface);

        std::vector<std::string> getAllIpInterface();
        std::vector<std::string> getAllIpInterfaceByName();

        std::map<std::string, std::string> getShortcuts();
        void setShortcuts(const std::map<std::string, std::string> &shortcutsMap);
};

#endif //CONFIGURATIONMANAGER_H

