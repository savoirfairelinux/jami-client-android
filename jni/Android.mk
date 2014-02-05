 #  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 #
 #  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>           
 #			Adrien Beraud <adrien.beraud@gmail.com> 
 #
 #  This program is free software; you can redistribute it and/or modify
 #  it under the terms of the GNU General Public License as published by
 #  the Free Software Foundation; either version 3 of the License, or
 #  (at your option) any later version.
 #  This program is distributed in the hope that it will be useful,
 #  but WITHOUT ANY WARRANTY; without even the implied warranty of
 #  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 #  GNU General Public License for more details.
 #
 #  You should have received a copy of the GNU General Public License
 #  along with this program; if not, write to the Free Software
 #  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA.
 #
 #  Additional permission under GNU GPL version 3 section 7:
 #
 #  If you modify this program, or any covered work, by linking or
 #  combining it with the OpenSSL project's OpenSSL library (or a
 #  modified version of that library), containing parts covered by the
 #  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 #  grants you additional permission to convey the resulting work.
 #  Corresponding Source for a non-source form of such a combination
 #  shall include the source code for the parts of OpenSSL used as well
 #  as that of the covered work.


LOCAL_PATH:= $(call my-dir)

LOCAL_CODECS_PATH = $(LOCAL_PATH)/sflphone/daemon/src/audio/codecs
LOCAL_SRC_PATH = $(LOCAL_PATH)/sflphone/daemon/src


include $(CLEAR_VARS)

include $(LOCAL_PATH)/libpjsip/Android.mk
include $(LOCAL_PATH)/libopus/Android.mk
include $(LOCAL_PATH)/libsndfile/Android.mk
include $(LOCAL_PATH)/libpcre/Android.mk
include $(LOCAL_PATH)/libgsm/Android.mk
include $(LOCAL_PATH)/libccrtp/Android.mk
include $(LOCAL_PATH)/libspeex/Android.mk
include $(LOCAL_PATH)/libyaml/Android.mk
include $(LOCAL_PATH)/libsamplerate/Android.mk
include $(LOCAL_PATH)/libexpat/Android.mk
include $(LOCAL_PATH)/libucommon/Android.mk
include $(LOCAL_PATH)/libopenssl/Android.mk
include $(LOCAL_PATH)/libzrtp/Android.mk

include $(CLEAR_VARS)
# FIXME
VERSION="1.1.0"
MY_PREFIX=/sdcard
MY_DATADIR=/data/data
MY_PJPROJECT=libpjsip/sources
MY_COMMONCPP=libucommon/sources
MY_CCRTP=libccrtp/sources
MY_OPENSSL=libopenssl
MY_SPEEX=libspeex/sources
MY_LIBZRTPCPP=libzrtp/sources

MY_JNI_WRAP := $(LOCAL_SRC_PATH)/client/android/callmanager_wrap.cpp


# FIXME: It would be cool to call the swig script automatically
#$(shell $(LOCAL_PATH)/../make-swig.sh)

	

#LOCAL_CPPFLAGS += -std=c++11
LOCAL_CPPFLAGS += -frtti
LOCAL_CPPFLAGS += -fexceptions
#LOCAL_CPPFLAGS += -fpermissive

LOCAL_SRC_FILES := \
		$(LOCAL_SRC_PATH)/conference.cpp \
		$(LOCAL_SRC_PATH)/voiplink.cpp \
		$(LOCAL_SRC_PATH)/preferences.cpp \
		$(LOCAL_SRC_PATH)/managerimpl.cpp \
		$(LOCAL_SRC_PATH)/manager.cpp \
		$(LOCAL_SRC_PATH)/eventthread.cpp \
		$(LOCAL_SRC_PATH)/call.cpp \
		$(LOCAL_SRC_PATH)/account.cpp \
		$(LOCAL_SRC_PATH)/numbercleaner.cpp \
		$(LOCAL_SRC_PATH)/fileutils.cpp \
		$(LOCAL_SRC_PATH)/audio/audioloop.cpp \
		$(LOCAL_SRC_PATH)/audio/ringbuffer.cpp \
		$(LOCAL_SRC_PATH)/audio/mainbuffer.cpp \
		$(LOCAL_SRC_PATH)/audio/audiorecord.cpp \
		$(LOCAL_SRC_PATH)/audio/audiobuffer.cpp \
		$(LOCAL_SRC_PATH)/audio/audiorecorder.cpp \
		$(LOCAL_SRC_PATH)/audio/recordable.cpp \
		$(LOCAL_SRC_PATH)/audio/audiolayer.cpp \
		$(LOCAL_SRC_PATH)/audio/samplerateconverter.cpp \
		$(LOCAL_SRC_PATH)/audio/dcblocker.cpp \
		$(LOCAL_SRC_PATH)/audio/opensl/opensllayer.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/audiofile.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/tone.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/tonelist.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/dtmf.cpp \
		$(LOCAL_SRC_PATH)/audio/dsp.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/dtmfgenerator.cpp \
		$(LOCAL_SRC_PATH)/audio/codecs/audiocodecfactory.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_rtp_session.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_symmetric_rtp_session.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_rtp_record_handler.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_rtp_factory.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_srtp_session.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/dtmf_event.cpp \
		$(LOCAL_SRC_PATH)/config/sfl_config.cpp \
		$(LOCAL_SRC_PATH)/config/yamlemitter.cpp \
		$(LOCAL_SRC_PATH)/config/yamlparser.cpp \
		$(LOCAL_SRC_PATH)/config/yamlnode.cpp \
		$(LOCAL_SRC_PATH)/client/android/client.cpp \
		$(LOCAL_SRC_PATH)/client/callmanager.cpp \
		$(LOCAL_SRC_PATH)/client/android/callmanager_jni.cpp \
		$(LOCAL_SRC_PATH)/client/configurationmanager.cpp  \
		$(LOCAL_SRC_PATH)/client/android/configurationmanager_jni.cpp  \
		$(LOCAL_SRC_PATH)/client/presencemanager.cpp  \
		$(LOCAL_SRC_PATH)/client/android/presencemanager_jni.cpp  \
		$(LOCAL_SRC_PATH)/client/android/callmanager_wrap.cpp \
		$(LOCAL_SRC_PATH)/history/historyitem.cpp \
		$(LOCAL_SRC_PATH)/history/history.cpp \
		$(LOCAL_SRC_PATH)/history/historynamecache.cpp \
		$(LOCAL_SRC_PATH)/hooks/urlhook.cpp \
		$(LOCAL_SRC_PATH)/im/instant_messaging.cpp \
		$(LOCAL_SRC_PATH)/sip/sdp.cpp \
		$(LOCAL_SRC_PATH)/sip/sipaccount.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp \
		$(LOCAL_SRC_PATH)/sip/sipcall.cpp \
		$(LOCAL_SRC_PATH)/sip/sipvoiplink.cpp \
		$(LOCAL_SRC_PATH)/sip/siptransport.cpp \
		$(LOCAL_SRC_PATH)/sip/sip_utils.cpp \
		$(LOCAL_SRC_PATH)/sip/sippresence.cpp \
		$(LOCAL_SRC_PATH)/sip/pattern.cpp \
		$(LOCAL_SRC_PATH)/sip/sdes_negotiator.cpp \
		$(LOCAL_SRC_PATH)/sip/pres_sub_client.cpp \
		$(LOCAL_SRC_PATH)/sip/pres_sub_server.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_zrtp_session.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/zrtp_session_callback.cpp \

# FIXME
LOCAL_C_INCLUDES += $(LOCAL_SRC_PATH)/.. \
					$(LOCAL_SRC_PATH) \
					$(LOCAL_SRC_PATH)/audio \
					$(LOCAL_SRC_PATH)/audio/opensl \
					$(LOCAL_SRC_PATH)/audio/sound \
					$(LOCAL_SRC_PATH)/audio/codecs \
					$(LOCAL_SRC_PATH)/audio/audiortp \
					$(LOCAL_SRC_PATH)/config \
					$(LOCAL_SRC_PATH)/client/android \
					$(LOCAL_SRC_PATH)/history \
					$(LOCAL_SRC_PATH)/hooks \
					$(LOCAL_SRC_PATH)/im \
					$(LOCAL_SRC_PATH)/sip \
					$(MY_SPEEX) \
					$(MY_SPEEX)/include \
					$(MY_LIBYAML)/inc \
					$(MY_LIBZRTPCPP) \
					$(MY_LIBZRTPCPP)/src \
					$(MY_LIBZRTPCPP)/src/libzrtpcpp \
					$(MY_CCRTP)/src \
					$(MY_LIBSAMPLE)/src \
					$(MY_OPENSSL)/include \
					$(MY_PJPROJECT)/pjsip/include \
					$(MY_PJPROJECT)/pjlib/include \
					$(MY_PJPROJECT)/pjlib-util/include \
					$(MY_PJPROJECT)/pjmedia/include \
					$(MY_PJPROJECT)/pjnath/include \
					$(MY_LIBEXPAT) \
					libsndfile/sources/src \
					libpcre/sources \
					${MY_COMMONCPP}/inc \
					
					

LOCAL_MODULE := libsflphone

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
					-DCCPP_PREFIX \
					-DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
					-DPREFIX=\"$(MY_PREFIX)\" \
					-DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
					-DHAVE_CONFIG_H \
					-DHAVE_SPEEX_CODEC \
					-DHAVE_GSM_CODEC \
					-w \
					-std=c++11 -fexceptions -fpermissive \
					-DAPP_NAME=\"sflphone\" \
					-DSWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON \
					-DDEBUG_DIRECTOR_OWNED \
					-DPJ_AUTOCONF=1

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)


LOCAL_LDLIBS  += 	-lz \
					-llog \
					-lOpenSLES \

# LOCAL_STATIC_LIBRARIES (NDK documentation)
#   The list of static libraries modules (built with BUILD_STATIC_LIBRARY)
#   that should be linked to this module. 
LOCAL_STATIC_LIBRARIES += 	pjsip \
							pjnath \
							pjmedia \
							pjlib \
							pjlib-util \
							libssl \
							libpcre \
							libccgnu2 \
							libsamplerate \
							libspeex \
							libcrypto_static \
							libzrtpcpp \
							libsndfile \
							libccrtp1 \
							libexpat \
							libspeexresampler \
							libyaml
							
						
				

include $(BUILD_SHARED_LIBRARY)


############# ulaw ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := 	$(LOCAL_CODECS_PATH)/ulaw.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp

# FIXME
LOCAL_C_INCLUDES += $(LOCAL_CODECS_PATH)/.. \
					$(LOCAL_CODECS_PATH)/../.. \
					$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
					$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc

LOCAL_MODULE := libcodec_ulaw

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codec_ulaw\"

include $(BUILD_SHARED_LIBRARY)



############# alaw ###############

include $(CLEAR_VARS)



LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/alaw.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_CODECS_PATH)/.. \
			$(LOCAL_CODECS_PATH)/../.. \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc \

LOCAL_MODULE := libcodec_alaw

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codec_alaw\"

LOCAL_LDFLAGS += -Wl,--export-dynamic

include $(BUILD_SHARED_LIBRARY)


############# g722 ###############

include $(CLEAR_VARS)



LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/g722.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_CODECS_PATH)/.. \
			$(LOCAL_CODECS_PATH)/../.. \
			$(LOCAL_CODECS_PATH)/../../.. \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc

LOCAL_MODULE := libcodec_g722

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)

############# libgsm ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := 	$(LOCAL_CODECS_PATH)/gsmcodec.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp \


LOCAL_C_INCLUDES += $(LOCAL_CODECS_PATH)/.. \
			$(LOCAL_CODECS_PATH)/../.. \
			$(LOCAL_CODECS_PATH)/../../.. \
			$(APP_PROJECT_PATH)/jni/$(MY_LIBGSM)/inc \

LOCAL_MODULE := libcodec_gsm

LOCAL_STATIC_LIBRARIES = libgsm

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
					-DCCPP_PREFIX \
					-DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
					-DPREFIX=\"$(MY_PREFIX)\" \
					-DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
					-DHAVE_CONFIG_H \
					-std=c++11 -frtti -fpermissive -fexceptions \
					-DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)

############# libcodec_opus ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/opus.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp 

LOCAL_C_INCLUDES += $(LOCAL_PATH)/.. \
			$(LOCAL_PATH)/../.. \
			$(LOCAL_PATH)/../../.. \
			$(APP_PROJECT_PATH)/jni/sflphone/daemon/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc

LOCAL_MODULE := libcodec_opus

LOCAL_LDLIBS := -llog 
				
LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

LOCAL_STATIC_LIBRARIES := libopus

include $(BUILD_SHARED_LIBRARY)


############# speex_nb ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  $(LOCAL_CODECS_PATH)/speexcodec_nb.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_SRC_PATH) \
			$(MY_SPEEX)/include/speex \
			$(MY_SPEEX)/include \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc \
			$(APP_PROJECT_PATH)/jni/sflphone/daemon

LOCAL_MODULE := libcodec_speex_nb

LOCAL_LDLIBS := -llog

LOCAL_STATIC_LIBRARIES := libspeex

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)



############# speex_ub ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/speexcodec_ub.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_SRC_PATH) \
			$(MY_SPEEX)/include/speex \
			$(MY_SPEEX)/include \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc \
			$(APP_PROJECT_PATH)/jni/sflphone/daemon

LOCAL_MODULE := libcodec_speex_ub

LOCAL_LDLIBS := -llog

LOCAL_STATIC_LIBRARIES := libspeex

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)

############# speex_wb ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/speexcodec_wb.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_SRC_PATH) \
					$(MY_SPEEX)/include/speex \
					$(MY_SPEEX)/include \
					$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
					$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc \
					$(APP_PROJECT_PATH)/jni/sflphone/daemon

LOCAL_MODULE := libcodec_speex_wb

LOCAL_LDLIBS := -llog

LOCAL_STATIC_LIBRARIES := libspeex

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)