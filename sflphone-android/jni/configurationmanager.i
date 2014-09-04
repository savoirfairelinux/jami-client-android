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
#include "sflphone.h"

class ConfigurationCallback {
public:
    virtual ~ConfigurationCallback(){}
    virtual void configOnVolumeChange(const std::string& device, int value){}
    virtual void configOnAccountsChange(void){}
    virtual void configOnHistoryChange(void){}
    virtual void configOnStunStatusFail(const std::string& account_id){}
    virtual void configOnRegistrationStateChange(const std::string& account_id, int state){}
    virtual void configOnSipRegistrationStateChange(const std::string& account_id, const std::string& state, int code){}
    virtual void configOnError(int alert){}
    virtual std::vector<int32_t> configGetHardwareAudioFormat(void){}
};
%}

%feature("director") ConfigurationCallback;

std::map<std::string, std::string> sflph_config_get_account_details(const std::string& account_id);
void sflph_config_set_account_details(const std::string& account_id, const std::map<std::string, std::string>& details);
std::map<std::string, std::string> sflph_config_get_account_template(void);
std::string sflph_config_add_account(const std::map<std::string, std::string>& details);
void sflph_config_remove_account(const std::string& account_id);
std::vector<std::string> sflph_config_get_account_list(void);
void sflph_config_send_register(const std::string& account_id, bool enable);
void sflph_config_register_all_accounts(void);
std::map<std::string, std::string> sflph_config_get_tls_default_settings(void);
std::vector<int> sflph_config_get_audio_codec_list(void);
std::vector<std::string> sflph_config_get_supported_tls_method(void);
std::vector<std::string> sflph_config_get_audio_codec_details(int payload);
std::vector<int> sflph_config_get_active_audio_codec_list(const std::string& account_id);
void sflph_config_set_active_audio_codec_list(const std::vector<std::string>& list, const std::string& account_id);
std::vector<std::string> sflph_config_get_audio_plugin_list(void);
void sflph_config_set_audio_plugin(const std::string& audio_plugin);
std::vector<std::string> sflph_config_get_audio_output_device_list();
void sflph_config_set_audio_output_device(int index);
void sflph_config_set_audio_input_device(int index);
void sflph_config_set_audio_ringtone_device(int index);
std::vector<std::string> sflph_config_get_audio_input_device_list(void);
std::vector<std::string> sflph_config_get_current_audio_devices_index(void);
int sflph_config_get_audio_input_device_index(const std::string& name);
int sflph_config_get_audio_output_device_index(const std::string& name);
std::string sflph_config_get_current_audio_output_plugin(void);
bool sflph_config_get_noise_suppress_state(void);
void sflph_config_set_noise_suppress_state(bool state);
bool sflph_config_is_agc_enabled(void);
void sflph_config_enable_agc(bool enabled);
void sflph_config_mute_dtmf(bool mute);
bool sflph_config_is_dtmf_muted(void);
bool sflph_config_is_capture_muted(void);
void sflph_config_mute_capture(bool mute);
bool sflph_config_is_playback_muted(void);
void sflph_config_mute_playback(int mute);
std::map<std::string, std::string> sflph_config_get_ringtone_list(void);
std::string sflph_config_get_audio_manager(void);
bool sflph_config_set_audio_manager(const std::string& api);
std::vector<std::string> sflph_config_get_supported_audio_managers(void);
int sflph_config_is_iax2_enabled(void);
std::string sflph_config_get_record_path(void);
void sflph_config_set_record_path(const std::string& path);
bool sflph_config_is_always_recording(void);
void sflph_config_set_always_recording(bool rec);
void sflph_config_set_history_limit(int days);
int sflph_config_get_history_limit(void);
void sflph_config_clear_history(void);
void sflph_config_set_accounts_order(const std::string& order);
std::map<std::string, std::string> sflph_config_get_hook_settings(void);
void sflph_config_set_hook_settings(const std::map<std::string, std::string>& settings);
std::vector<std::map<std::string, std::string> > sflph_config_get_history(void);
std::map<std::string, std::string> sflph_config_get_tls_settings();
void sflph_config_set_tls_settings(const std::map< std::string, std::string >& settings);
std::map<std::string, std::string> sflph_config_get_ip2ip_details(void);
std::vector<std::map<std::string, std::string> > sflph_config_get_credentials(const std::string& account_id);
void sflph_config_set_credentials(const std::string& account_id, const std::vector<std::map<std::string, std::string> >& details);
std::string sflph_config_get_addr_from_interface_name(const std::string& interface);
std::vector<std::string> sflph_config_get_all_ip_interface(void);
std::vector<std::string> sflph_config_get_all_ip_interface_by_name(void);
std::map<std::string, std::string> sflph_config_get_shortcuts();
void sflph_config_set_shortcuts(const std::map<std::string, std::string>& shortcuts);
void sflph_config_set_volume(const std::string& device, double value);
double sflph_config_get_volume(const std::string& device);
bool sflph_config_check_for_private_key(const std::string& pem_path);
bool sflph_config_check_certificate_validity(const std::string& ca_path, const std::string& pem_path);
bool sflph_config_check_hostname_certificate(const std::string& host, const std::string& port);


class ConfigurationCallback {
public:
    virtual ~ConfigurationCallback();
    virtual void configOnVolumeChange(const std::string& device, int value);
    virtual void configOnAccountsChange(void);
    virtual void configOnHistoryChange(void);
    virtual void configOnStunStatusFail(const std::string& account_id);
    virtual void configOnRegistrationStateChange(const std::string& account_id, int state);
    virtual void configOnSipRegistrationStateChange(const std::string& account_id, const std::string& state, int code);
    virtual void configOnError(int alert);
    virtual std::vector<int32_t> configGetHardwareAudioFormat(void);
};

