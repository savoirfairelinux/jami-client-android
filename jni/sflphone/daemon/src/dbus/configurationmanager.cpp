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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "configurationmanager.h"
#include "account_schema.h"
#include <cerrno>
#include <sstream>
#include "../manager.h"
#include "sip/sipvoiplink.h"
#include "sip/siptransport.h"
#include "account.h"
#include "logger.h"
#include "fileutils.h"
#include "sip/sipaccount.h"
#include "../history/historynamecache.h"
#include "../audio/audiolayer.h"

namespace {
    const char* SERVER_PATH = "/org/sflphone/SFLphone/ConfigurationManager";
}

ConfigurationManager::ConfigurationManager(DBus::Connection& connection) :
    DBus::ObjectAdaptor(connection, SERVER_PATH)
{}

std::map<std::string, std::string> ConfigurationManager::getIp2IpDetails()
{
    std::map<std::string, std::string> ip2ipAccountDetails;
    SIPAccount *sipaccount = Manager::instance().getIP2IPAccount();

    if (!sipaccount) {
        ERROR("Could not find IP2IP account");
        return ip2ipAccountDetails;
    } else
        return sipaccount->getIp2IpDetails();

    std::map<std::string, std::string> tlsSettings(getTlsSettings());
    std::copy(tlsSettings.begin(), tlsSettings.end(),
              std::inserter(ip2ipAccountDetails, ip2ipAccountDetails.end()));

    return ip2ipAccountDetails;
}


std::map<std::string, std::string> ConfigurationManager::getAccountDetails(
    const std::string& accountID)
{
    return Manager::instance().getAccountDetails(accountID);
}

std::map<std::string, std::string>
ConfigurationManager::getTlsSettingsDefault()
{
    std::stringstream portstr;
    portstr << DEFAULT_SIP_TLS_PORT;

    std::map<std::string, std::string> tlsSettingsDefault;
    tlsSettingsDefault[CONFIG_TLS_LISTENER_PORT] = portstr.str();
    tlsSettingsDefault[CONFIG_TLS_CA_LIST_FILE] = "";
    tlsSettingsDefault[CONFIG_TLS_CERTIFICATE_FILE] = "";
    tlsSettingsDefault[CONFIG_TLS_PRIVATE_KEY_FILE] = "";
    tlsSettingsDefault[CONFIG_TLS_PASSWORD] = "";
    tlsSettingsDefault[CONFIG_TLS_METHOD] = "TLSv1";
    tlsSettingsDefault[CONFIG_TLS_CIPHERS] = "";
    tlsSettingsDefault[CONFIG_TLS_SERVER_NAME] = "";
    tlsSettingsDefault[CONFIG_TLS_VERIFY_SERVER] = "true";
    tlsSettingsDefault[CONFIG_TLS_VERIFY_CLIENT] = "true";
    tlsSettingsDefault[CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE] = "true";
    tlsSettingsDefault[CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC] = "2";
    tlsSettingsDefault[CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC] = "0";

    return tlsSettingsDefault;
}

std::map<std::string, std::string> ConfigurationManager::getTlsSettings()
{
    std::map<std::string, std::string> tlsSettings;

    SIPAccount *sipaccount = Manager::instance().getIP2IPAccount();

    if (!sipaccount)
        return tlsSettings;

    return sipaccount->getTlsSettings();
}

void ConfigurationManager::setTlsSettings(const std::map<std::string, std::string>& details)
{
    SIPAccount * sipaccount = Manager::instance().getIP2IPAccount();

    if (!sipaccount) {
        DEBUG("No valid account in set TLS settings");
        return;
    }

    sipaccount->setTlsSettings(details);

    Manager::instance().saveConfig();

    // Update account details to the client side
    accountsChanged();
}


void ConfigurationManager::setAccountDetails(const std::string& accountID, const std::map<std::string, std::string>& details)
{
    Manager::instance().setAccountDetails(accountID, details);
}

void ConfigurationManager::sendRegister(const std::string& accountID, const bool& enable)
{
    Manager::instance().sendRegister(accountID, enable);
}

void ConfigurationManager::registerAllAccounts()
{
    Manager::instance().registerAllAccounts();
}

std::string ConfigurationManager::addAccount(const std::map<std::string, std::string>& details)
{
    return Manager::instance().addAccount(details);
}

void ConfigurationManager::removeAccount(const std::string& accoundID)
{
    return Manager::instance().removeAccount(accoundID);
}

std::vector<std::string> ConfigurationManager::getAccountList()
{
    return Manager::instance().getAccountList();
}

/**
 * Send the list of all codecs loaded to the client through DBus.
 * Can stay global, as only the active codecs will be set per accounts
 */
std::vector<int32_t> ConfigurationManager::getAudioCodecList()
{
    std::vector<int32_t> list(Manager::instance().audioCodecFactory.getAudioCodecList());

    if (list.empty())
        errorAlert(CODECS_NOT_LOADED);

    return list;
}

std::vector<std::string> ConfigurationManager::getSupportedTlsMethod()
{
    std::vector<std::string> method;
    method.push_back("Default");
    method.push_back("TLSv1");
    method.push_back("SSLv3");
    method.push_back("SSLv23");
    return method;
}

std::vector<std::string> ConfigurationManager::getAudioCodecDetails(const int32_t& payload)
{
    std::vector<std::string> result(Manager::instance().audioCodecFactory.getCodecSpecifications(payload));

    if (result.empty())
        errorAlert(CODECS_NOT_LOADED);

    return result;
}

std::vector<int32_t> ConfigurationManager::getActiveAudioCodecList(const std::string& accountID)
{
    Account *acc = Manager::instance().getAccount(accountID);

    if (acc)
        return acc->getActiveAudioCodecs();
    else {
        ERROR("Could not find account %s", accountID.c_str());
        return std::vector<int32_t>();
    }
}

void ConfigurationManager::setActiveAudioCodecList(const std::vector<std::string>& list, const std::string& accountID)
{
    Account *acc = Manager::instance().getAccount(accountID);

    if (acc) {
        acc->setActiveAudioCodecs(list);
        Manager::instance().saveConfig();
    }
}

std::vector<std::string> ConfigurationManager::getAudioPluginList()
{
    std::vector<std::string> v;

    v.push_back(PCM_DEFAULT);
    v.push_back(PCM_DMIX_DSNOOP);

    return v;
}

void ConfigurationManager::setAudioPlugin(const std::string& audioPlugin)
{
    return Manager::instance().setAudioPlugin(audioPlugin);
}

std::vector<std::string> ConfigurationManager::getAudioOutputDeviceList()
{
    return Manager::instance().getAudioOutputDeviceList();
}

std::vector<std::string> ConfigurationManager::getAudioInputDeviceList()
{
    return Manager::instance().getAudioInputDeviceList();
}

void ConfigurationManager::setAudioOutputDevice(const int32_t& index)
{
    return Manager::instance().setAudioDevice(index, AudioLayer::SFL_PCM_PLAYBACK);
}

void ConfigurationManager::setAudioInputDevice(const int32_t& index)
{
    return Manager::instance().setAudioDevice(index, AudioLayer::SFL_PCM_CAPTURE);
}

void ConfigurationManager::setAudioRingtoneDevice(const int32_t& index)
{
    return Manager::instance().setAudioDevice(index, AudioLayer::SFL_PCM_RINGTONE);
}

std::vector<std::string> ConfigurationManager::getCurrentAudioDevicesIndex()
{
    return Manager::instance().getCurrentAudioDevicesIndex();
}

int32_t ConfigurationManager::getAudioDeviceIndex(const std::string& name)
{
    return Manager::instance().getAudioDeviceIndex(name);
}

std::string ConfigurationManager::getCurrentAudioOutputPlugin()
{
    DEBUG("Get audio plugin %s", Manager::instance().getCurrentAudioOutputPlugin().c_str());

    return Manager::instance().getCurrentAudioOutputPlugin();
}

std::string ConfigurationManager::getNoiseSuppressState()
{
    return Manager::instance().getNoiseSuppressState();
}

void ConfigurationManager::setNoiseSuppressState(const std::string& state)
{
    Manager::instance().setNoiseSuppressState(state);
}

std::string ConfigurationManager::getEchoCancelState()
{
    return Manager::instance().getEchoCancelState() ? "enabled" : "disabled";
}

std::map<std::string, std::string> ConfigurationManager::getRingtoneList()
{
    std::map<std::string, std::string> ringToneList;
    std::string r_path(fileutils::get_data_dir());
    struct dirent **namelist;
    int n = scandir(r_path.c_str(), &namelist, 0, alphasort);
    if (n == -1) {
        ERROR("%s", strerror(errno));
        return ringToneList;
    }

    while (n--) {
        if (strcmp(namelist[n]->d_name, ".") and strcmp(namelist[n]->d_name, "..")) {
            std::string file(namelist[n]->d_name);

            if (file.find(".wav") != std::string::npos)
                file.replace(file.find(".wav"), 4, "");
            else
                file.replace(file.size() - 3, 3, "");
            if (file[0] <= 0x7A and file[0] >= 0x61) file[0] = file[0] - 32;
            ringToneList[r_path + namelist[n]->d_name] = file;
        }
        free(namelist[n]);
    }
    free(namelist);
    return ringToneList;
}

void ConfigurationManager::setEchoCancelState(const std::string& state)
{
    Manager::instance().setEchoCancelState(state);
}

int ConfigurationManager::getEchoCancelTailLength()
{
    return Manager::instance().getEchoCancelTailLength();
}

void ConfigurationManager::setEchoCancelTailLength(const int32_t& length)
{
    Manager::instance().setEchoCancelTailLength(length);
}

int ConfigurationManager::getEchoCancelDelay()
{
    return Manager::instance().getEchoCancelDelay();
}

void ConfigurationManager::setEchoCancelDelay(const int32_t& delay)
{
    Manager::instance().setEchoCancelDelay(delay);
}

int32_t ConfigurationManager::isIax2Enabled()
{
    return HAVE_IAX;
}

std::string ConfigurationManager::getRecordPath()
{
    return Manager::instance().getRecordPath();
}

void ConfigurationManager::setRecordPath(const std::string& recPath)
{
    Manager::instance().setRecordPath(recPath);
}

bool ConfigurationManager::getIsAlwaysRecording()
{
    return Manager::instance().getIsAlwaysRecording();
}

void ConfigurationManager::setIsAlwaysRecording(const bool& rec)
{
    Manager::instance().setIsAlwaysRecording(rec);
}

int32_t ConfigurationManager::getHistoryLimit()
{
    return Manager::instance().getHistoryLimit();
}

void ConfigurationManager::clearHistory()
{
    return Manager::instance().clearHistory();
}

void ConfigurationManager::setHistoryLimit(const int32_t& days)
{
    Manager::instance().setHistoryLimit(days);
}

void ConfigurationManager::setAudioManager(const std::string& api)
{
    Manager::instance().setAudioManager(api);
}

std::string ConfigurationManager::getAudioManager()
{
    return Manager::instance().getAudioManager();
}

void ConfigurationManager::setMailNotify()
{
    Manager::instance().setMailNotify();
}

int32_t ConfigurationManager::getMailNotify()
{
    return Manager::instance().getMailNotify();
}

std::map<std::string, int32_t> ConfigurationManager::getAddressbookSettings()
{
    return Manager::instance().getAddressbookSettings();
}

void ConfigurationManager::setAddressbookSettings(const std::map<std::string, int32_t>& settings)
{
    Manager::instance().setAddressbookSettings(settings);
}

std::vector<std::string> ConfigurationManager::getAddressbookList()
{
    return Manager::instance().getAddressbookList();
}

void ConfigurationManager::setAddressbookList(const std::vector<std::string>& list)
{
    Manager::instance().setAddressbookList(list);
}

std::map<std::string, std::string> ConfigurationManager::getHookSettings()
{
    return Manager::instance().hookPreference.toMap();
}

void ConfigurationManager::setHookSettings(const std::map<std::string,
        std::string>& settings)
{
    Manager::instance().hookPreference = HookPreference(settings);
}

void ConfigurationManager::setAccountsOrder(const std::string& order)
{
    Manager::instance().setAccountsOrder(order);
}

std::vector<std::map<std::string, std::string> > ConfigurationManager::getHistory()
{
    return Manager::instance().getHistory();
}

std::string
ConfigurationManager::getAddrFromInterfaceName(const std::string& interface)
{
    return SipTransport::getInterfaceAddrFromName(interface);
}

std::vector<std::string> ConfigurationManager::getAllIpInterface()
{
    return SipTransport::getAllIpInterface();
}

std::vector<std::string> ConfigurationManager::getAllIpInterfaceByName()
{
    return SipTransport::getAllIpInterfaceByName();
}

std::map<std::string, std::string> ConfigurationManager::getShortcuts()
{
    return Manager::instance().shortcutPreferences.getShortcuts();
}

void ConfigurationManager::setShortcuts(
    const std::map<std::string, std::string>& shortcutsMap)
{
    Manager::instance().shortcutPreferences.setShortcuts(shortcutsMap);
    Manager::instance().saveConfig();
}

std::vector<std::map<std::string, std::string> > ConfigurationManager::getCredentials(
    const std::string& accountID)
{
    SIPAccount *account = dynamic_cast<SIPAccount*>(Manager::instance().getAccount(accountID));
    std::vector<std::map<std::string, std::string> > credentialInformation;

    if (!account)
        return credentialInformation;
    else
        return account->getCredentials();
}

void ConfigurationManager::setCredentials(const std::string& accountID,
        const std::vector<std::map<std::string, std::string> >& details)
{
    SIPAccount *account = dynamic_cast<SIPAccount*>(Manager::instance().getAccount(accountID));
    if (account)
        account->setCredentials(details);
}
