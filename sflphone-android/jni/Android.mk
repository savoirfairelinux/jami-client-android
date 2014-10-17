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

$(info PWD=$(PWD))
$(info SFLPHONE_CONTRIB=$(SFLPHONE_CONTRIB))
$(info SFLPHONE_SRC_DIR=$(SFLPHONE_SRC_DIR))
$(info SFLPHONE_BUILD_DIR=$(SFLPHONE_BUILD_DIR))

include $(CLEAR_VARS)

VERSION="1.1.0"
MY_PREFIX=/sdcard
MY_DATADIR=/data/data

ARCH=$(ANDROID_ABI)

CPP_STATIC= $(ANDROID_NDK)/sources/cxx-stl/gnu-libstdc++$(CXXSTL)/libs/$(ARCH)/libgnustl_static.a \
			$(SFLPHONE_CONTRIB)/lib/libucommon.a \
			$(SFLPHONE_CONTRIB)/lib/libccrtp.a \
			$(SFLPHONE_CONTRIB)/lib/libogg.a \
			$(SFLPHONE_CONTRIB)/lib/libFLAC.a \
			$(SFLPHONE_CONTRIB)/lib/libgcrypt.a \
			$(SFLPHONE_CONTRIB)/lib/libgpg-error.a \

ifeq ($(ARCH),$(filter $(ARCH),x86))
CPP_STATIC += $(SFLPHONE_CONTRIB)/lib/libpjlib-util-i686-pc-linux-android.a \
			$(SFLPHONE_CONTRIB)/lib/libpj-i686-pc-linux-android.a
else
CPP_STATIC += $(SFLPHONE_CONTRIB)/lib/libpjlib-util-arm-unknown-linux-androideabi.a \
			$(SFLPHONE_CONTRIB)/lib/libpj-arm-unknown-linux-androideabi.a
endif

LOCAL_SRC_FILES :=  sflphone_wrapper.cpp

# SFLPHONE_BUILD_DIR contains config.h, which we need
LOCAL_C_INCLUDES += $(LOCAL_PATH) \
					$(SFLPHONE_BUILD_DIR) \
					$(SFLPHONE_SRC_DIR)/daemon \
					$(SFLPHONE_SRC_DIR)/daemon/src \
					$(SFLPHONE_SRC_DIR)/daemon/contrib/$(TARGET_TUPLE)/include

LOCAL_MODULE := libsflphonejni

LOCAL_CPPFLAGS += 	-DCCPP_PREFIX \
					-DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
					-DHAVE_CONFIG_H \
					-DHAVE_SPEEX_CODEC \
					-DHAVE_GSM_CODEC \
					-w -frtti \
					-std=c++11 -fexceptions -fpermissive \
					-DAPP_NAME=\"Ring\" \
					-DSWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON \
					-DDEBUG_DIRECTOR_OWNED \
					-DPJ_AUTOCONF=1

LOCAL_LDFLAGS := -L$(SFLPHONE_CONTRIB)/lib \

LOCAL_LDLIBS  += 	-lz \
					-llog \
					-lOpenSLES \
					$(SFLPHONE_BUILD_DIR)/src/.libs/libsflphone.a \


ifeq ($(ARCH),$(filter $(ARCH),x86))
LOCAL_LDLIBS += -lpj-i686-pc-linux-android \
				-lpjsip-simple-i686-pc-linux-android \
				-lpjlib-util-i686-pc-linux-android \
				-lpjsip-ua-i686-pc-linux-android \
				-lpjmedia-i686-pc-linux-android \
				-lpjnath-i686-pc-linux-android \
				-lpjmedia-audiodev-i686-pc-linux-android \
				-lsrtp-i686-pc-linux-android \
				-lpjsip-i686-pc-linux-android \
				-lresample-i686-pc-linux-android

else
LOCAL_LDLIBS += -lpj-arm-unknown-linux-androideabi \
				-lpjsip-simple-arm-unknown-linux-androideabi \
				-lpjlib-util-arm-unknown-linux-androideabi \
				-lpjsip-ua-arm-unknown-linux-androideabi \
				-lpjmedia-arm-unknown-linux-androideabi \
				-lpjnath-arm-unknown-linux-androideabi \
				-lpjmedia-audiodev-arm-unknown-linux-androideabi \
				-lsrtp-arm-unknown-linux-androideabi \
				-lpjsip-arm-unknown-linux-androideabi \
				-lresample-arm-unknown-linux-androideabi
endif

LOCAL_LDLIBS	+=	-lexpat -lhogweed \
					-lspeexdsp -lvorbisfile -lyaml-cpp \
					-lFLAC -liax -lgcrypt -lnettle \
					-logg -lucommon \
					-lpcre -lsamplerate -luuid -lccrtp -lgpg-error -lpcrecpp \
					-lsndfile -lvorbis \
					-lcommoncpp	-lspeex -lvorbisenc \
					$(CPP_STATIC)


include $(BUILD_SHARED_LIBRARY)

########### Codecs ###############

include $(CLEAR_VARS)
LOCAL_MODULE := ulaw
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_ulaw.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := alaw
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_alaw.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g722
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_g722.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := speex_nb
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_speex_nb.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := speex_ub
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_speex_ub.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := speex_wb
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_speex_wb.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opus
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_opus.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := gsm
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_gsm.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g729
LOCAL_SRC_FILES := ../$(SFLPHONE_BUILD_DIR)/src/audio/codecs/libcodec_g729.so
include $(PREBUILT_SHARED_LIBRARY)
