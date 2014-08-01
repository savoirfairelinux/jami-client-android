/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

%header %{
#include "client/configurationmanager.h"

typedef struct configurationmanager_callback
{
    void (*on_accounts_changed)(void);
    void (*on_account_state_changed)(const std::string& accountID, const int32_t& state);
    void (*on_account_state_changed_with_code)(const std::string& accountID, const std::string& state, const int32_t& code);
    std::vector<int32_t> (*get_hardware_audio_format)(void);
} configurationmanager_callback_t;


class ConfigurationCallback {
public:
    virtual ~ConfigurationCallback() {}
    virtual void on_accounts_changed(void) {}
    virtual void on_account_state_changed(const std::string& accountID, const int32_t& state) {}
    virtual void on_account_state_changed_with_code(const std::string& accountID, const std::string& state, const int32_t& code) {}
    virtual std::vector<int32_t> get_hardware_audio_format(void) {}
};

static ConfigurationCallback *registeredConfigurationCallbackObject = NULL;

void on_accounts_changed_wrapper (void) {
    registeredConfigurationCallbackObject->on_accounts_changed();
}

void on_account_state_changed_wrapper (const std::string& accountID, const int32_t& state) {
    registeredConfigurationCallbackObject->on_account_state_changed(accountID, state);
}

void on_account_state_changed_with_code_wrapper (const std::string& accountID, const std::string& state, const int32_t& code) {
    registeredConfigurationCallbackObject->on_account_state_changed_with_code(accountID, state, code);
}

std::vector<int32_t> get_hardware_audio_format_wrapper(void) {
    return registeredConfigurationCallbackObject->get_hardware_audio_format();
}

static struct configurationmanager_callback wrapper_configurationcallback_struct = {
    &on_accounts_changed_wrapper,
    &on_account_state_changed_wrapper,
    &on_account_state_changed_with_code_wrapper,
    &get_hardware_audio_format_wrapper
};

void setConfigurationCallbackObject(ConfigurationCallback *callback) {
    registeredConfigurationCallbackObject = callback;
}

%}

%feature("director") ConfigurationCallback;

class ConfigurationManager {
public:
    std::map< std::string, std::string > getAccountDetails(const std::string& accountID);
    void setAccountDetails(const std::string& accountID, const std::map< std::string, std::string >& details);
    std::map<std::string, std::string> getAccountTemplate();
    std::string addAccount(const std::map< std::string, std::string >& details);
    void removeAccount(const std::string& accountID);
    std::vector< std::string > getAccountList();
    void sendRegister(const std::string& accountID, const bool& enable);
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
    int32_t getAudioInputDeviceIndex(const std::string& name);
    int32_t getAudioOutputDeviceIndex(const std::string& name);
    std::string getCurrentAudioOutputPlugin();
    bool getNoiseSuppressState();
    void setNoiseSuppressState(const bool& state);
    bool isAgcEnabled();
    void setAgcState(const bool& state);
    bool isDtmfMuted();
    void muteDtmf(const bool& mute);
    bool isCaptureMuted();
    void muteCapture(const bool& mute);
    bool isPlaybackMuted();
    void mutePlayback(const bool& mute);
    void setVolume(const std::string& device, const double& value);
    double getVolume(const std::string& device);

    std::map<std::string, std::string> getRingtoneList();

    std::string getAudioManager();
    bool setAudioManager(const std::string& api);

    int32_t isIax2Enabled();
    std::string getRecordPath();
    void setRecordPath(const std::string& recPath);
    bool getIsAlwaysRecording();
    void setIsAlwaysRecording(const bool& rec);

    void setHistoryLimit(const int32_t& days);
    int32_t getHistoryLimit();
    void clearHistory();

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

    bool checkForPrivateKey(const std::string& pemPath);
    bool checkCertificateValidity(const std::string& caPath, const std::string& pemPath);
    bool checkHostnameCertificate(const  std::string& host, const std::string& port);

};

class ConfigurationCallback {
public:
    virtual ~ConfigurationCallback();
    virtual void on_accounts_changed(void);
    virtual void on_account_state_changed(const std::string& accountID, const int32_t& state);
    virtual void on_account_state_changed_with_code(const std::string& accountID, const std::string& state, const int32_t& code);
    virtual std::vector<int32_t> get_hardware_audio_format(void);
};

static ConfigurationCallback *registeredConfigurationCallbackObject = NULL;

void setConfigurationCallbackObject(ConfigurationCallback *callback) {
    registeredConfigurationCallbackObject = callback;
}
