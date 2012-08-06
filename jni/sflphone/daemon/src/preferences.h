/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
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

#ifndef __PREFERENCE_H__
#define __PREFERENCE_H__

#include "config/serializable.h"
#include <string>
#include <map>

class AudioLayer;

class Preferences : public Serializable {
    public:
        static const char * const DFT_ZONE;
        static const char * const REGISTRATION_EXPIRE_KEY;

        Preferences();

        virtual void serialize(Conf::YamlEmitter &emitter);
        virtual void unserialize(const Conf::MappingNode &map);

        std::string getAccountOrder() const {
            return accountOrder_;
        }

        void setAccountOrder(const std::string &ord) {
            accountOrder_ = ord;
        }

        int getHistoryLimit() const {
            return historyLimit_;
        }

        void setHistoryLimit(int lim) {
            historyLimit_ = lim;
        }

        int getHistoryMaxCalls() const {
            return historyMaxCalls_;
        }

        void setHistoryMaxCalls(int max) {
            historyMaxCalls_ = max;
        }

        bool getNotifyMails() const {
            return notifyMails_;
        }

        void setNotifyMails(bool mails) {
            notifyMails_ = mails;
        }

        std::string getZoneToneChoice() const {
            return zoneToneChoice_;
        }

        void setZoneToneChoice(const std::string &str) {
            zoneToneChoice_ = str;
        }

        int getRegistrationExpire() const {
            return registrationExpire_;
        }

        void setRegistrationExpire(int exp) {
            registrationExpire_ = exp;
        }

        int getPortNum() const {
            return portNum_;
        }

        void setPortNum(int port) {
            portNum_ = port;
        }

        bool getSearchBarDisplay() const {
            return searchBarDisplay_;
        }

        void setSearchBarDisplay(bool search) {
            searchBarDisplay_ = search;
        }

        bool getZeroConfenable() const {
            return zeroConfenable_;
        }
        void setZeroConfenable(bool enable) {
            zeroConfenable_ = enable;
        }

        bool getMd5Hash() const {
            return md5Hash_;
        }
        void setMd5Hash(bool md5) {
            md5Hash_ = md5;
        }

    private:
        std::string accountOrder_;
        int historyLimit_;
        int historyMaxCalls_;
        bool notifyMails_;
        std::string zoneToneChoice_;
        int registrationExpire_;
        int portNum_;
        bool searchBarDisplay_;
        bool zeroConfenable_;
        bool md5Hash_;
};

class VoipPreference : public Serializable {
    public:
        VoipPreference();

        virtual void serialize(Conf::YamlEmitter &emitter);
        virtual void unserialize(const Conf::MappingNode &map);

        bool getPlayDtmf() const {
            return playDtmf_;
        }

        void setPlayDtmf(bool dtmf) {
            playDtmf_ = dtmf;
        }

        bool getPlayTones() const {
            return playTones_;
        }

        void setPlayTones(bool tone) {
            playTones_ = tone;
        }

        int getPulseLength() const {
            return pulseLength_;
        }

        void setPulseLength(int length) {
            pulseLength_ = length;
        }

        bool getSymmetricRtp() const {
            return symmetricRtp_;
        }
        void setSymmetricRtp(bool sym) {
            symmetricRtp_ = sym;
        }

        std::string getZidFile() const {
            return zidFile_;
        }
        void setZidFile(const std::string &file) {
            zidFile_ = file;
        }

    private:

        bool playDtmf_;
        bool playTones_;
        int pulseLength_;
        bool symmetricRtp_;
        std::string zidFile_;
};

class AddressbookPreference : public Serializable {
    public:
        AddressbookPreference();

        virtual void serialize(Conf::YamlEmitter &emitter);
        virtual void unserialize(const Conf::MappingNode &map);

        bool getPhoto() const {
            return photo_;
        }

        void setPhoto(bool p) {
            photo_ = p;
        }

        bool getEnabled() const {
            return enabled_;
        }

        void setEnabled(bool e) {
            enabled_ = e;
        }

        std::string getList() const {
            return list_;
        }

        void setList(const std::string &l) {
            list_ = l;
        }

        int getMaxResults() const {
            return maxResults_;
        }

        void setMaxResults(int r) {
            maxResults_ = r;
        }

        bool getBusiness() const {
            return business_;
        }

        void setBusiness(bool b) {
            business_ = b;
        }

        bool getHome() const {
            return home_;
        }
        void setHone(bool h) {
            home_ = h;
        }

        bool getMobile() const {
            return mobile_;
        }
        void setMobile(bool m) {
            mobile_ = m;
        }

    private:

        bool photo_;
        bool enabled_;
        std::string list_;
        int maxResults_;
        bool business_;
        bool home_;
        bool mobile_;
};


class pjsip_msg;

class HookPreference : public Serializable {
    public:
        HookPreference();
        HookPreference(const std::map<std::string, std::string> &settings);

        virtual void serialize(Conf::YamlEmitter &emitter);
        virtual void unserialize(const Conf::MappingNode &map);

        std::string getNumberAddPrefix() const {
            if (numberEnabled_)
                return numberAddPrefix_;
            else
                return "";
        }

        std::map<std::string, std::string> toMap() const;
        void runHook(pjsip_msg *msg);

    private:
        bool iax2Enabled_;
        std::string numberAddPrefix_;
        bool numberEnabled_;
        bool sipEnabled_;
        std::string urlCommand_;
        std::string urlSipField_;
};

class AudioPreference : public Serializable {
    public:
        AudioPreference();
        AudioLayer *createAudioLayer();
        AudioLayer *switchAndCreateAudioLayer();

        std::string getAudioApi() const {
            return audioApi_;
        }

        void setAudioApi(const std::string &api) {
            audioApi_ = api;
        }

        virtual void serialize(Conf::YamlEmitter &emitter);
        virtual void unserialize(const Conf::MappingNode &map);

        // alsa preference
        int getAlsaCardin() const {
            return alsaCardin_;
        }
        void setAlsaCardin(int c) {
            alsaCardin_ = c;
        }

        int getAlsaCardout() const {
            return alsaCardout_;
        }

        void setAlsaCardout(int c) {
            alsaCardout_ = c;
        }

        int getAlsaCardring() const {
            return alsaCardring_;
        }

        void setAlsaCardring(int c) {
            alsaCardring_ = c;
        }

        std::string getAlsaPlugin() const {
            return alsaPlugin_;
        }

        void setAlsaPlugin(const std::string &p) {
            alsaPlugin_ = p;
        }

        int getAlsaSmplrate() const {
            return alsaSmplrate_;
        }
        void setAlsaSmplrate(int r) {
            alsaSmplrate_ = r;
        }

        //pulseaudio preference
        std::string getPulseDevicePlayback() const {
            return pulseDevicePlayback_;
        }

        void setPulseDevicePlayback(const std::string &p) {
            pulseDevicePlayback_ = p;
        }

        std::string getPulseDeviceRecord() const {
            return pulseDeviceRecord_;
        }
        void setPulseDeviceRecord(const std::string &r) {
            pulseDeviceRecord_ = r;
        }

        std::string getPulseDeviceRingtone() const {
            return pulseDeviceRingtone_;
        }

        void setPulseDeviceRingtone(const std::string &r) {
            pulseDeviceRingtone_ = r;
        }

        // general preference
        std::string getRecordpath() const {
            return recordpath_;
        }
        void setRecordpath(const std::string &r) {
            recordpath_ = r;
        }

        bool getIsAlwaysRecording() const {
            return alwaysRecording_;
        }

        void setIsAlwaysRecording(bool rec) {
            alwaysRecording_ = rec;
        }

        int getVolumemic() const {
            return volumemic_;
        }
        void setVolumemic(int m) {
            volumemic_ = m;
        }

        int getVolumespkr() const {
            return volumespkr_;
        }
        void setVolumespkr(int s) {
            volumespkr_ = s;
        }

        bool getNoiseReduce() const {
            return noisereduce_;
        }

        void setNoiseReduce(bool noise) {
            noisereduce_ = noise;
        }

        bool getEchoCancel() const {
            return echocancel_;
        }

        void setEchoCancel(bool echo) {
            echocancel_ = echo;
        }

        int getEchoCancelTailLength() const {
            return echoCancelTailLength_;
        }

        void setEchoCancelTailLength(int length) {
            echoCancelTailLength_ = length;
        }

        int getEchoCancelDelay() const {
            return echoCancelDelay_;
        }

        void setEchoCancelDelay(int delay) {
            echoCancelDelay_ = delay;
        }

    private:
        std::string audioApi_;

        // alsa preference
        int alsaCardin_;
        int alsaCardout_;
        int alsaCardring_;
        std::string alsaPlugin_;
        int alsaSmplrate_;

        //pulseaudio preference
        std::string pulseDevicePlayback_;
        std::string pulseDeviceRecord_;
        std::string pulseDeviceRingtone_;

        // general preference
        std::string recordpath_; //: /home/msavard/Bureau
        bool alwaysRecording_;
        int volumemic_;
        int volumespkr_;

        bool noisereduce_;
        bool echocancel_;
        int echoCancelTailLength_;
        int echoCancelDelay_;
};

class ShortcutPreferences : public Serializable {
    public:
        ShortcutPreferences();
        virtual void serialize(Conf::YamlEmitter &emitter);
        virtual void unserialize(const Conf::MappingNode &map);

        void setShortcuts(std::map<std::string, std::string> shortcuts);
        std::map<std::string, std::string> getShortcuts() const;

        std::string getHangup() const {
            return hangup_;
        }

        void setHangup(const std::string &hangup) {
            hangup_ = hangup;
        }

        std::string getPickup() const {
            return pickup_;
        }

        void setPickup(const std::string &pickup) {
            pickup_ = pickup;
        }

        std::string getPopup() const {
            return popup_;
        }

        void setPopup(const std::string &popup) {
            popup_ = popup;
        }

        std::string getToggleHold() const {
            return toggleHold_;
        }

        void setToggleHold(const std::string &hold) {
            toggleHold_ = hold;
        }

        std::string getTogglePickupHangup() const {
            return togglePickupHangup_;
        }

        void setTogglePickupHangup(const std::string &toggle) {
            togglePickupHangup_ = toggle;
        }

    private:
        std::string hangup_;
        std::string pickup_;
        std::string popup_;
        std::string toggleHold_;
        std::string togglePickupHangup_;
};

#endif
