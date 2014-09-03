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

$(info SFLPHONE_CONTRIB=$(SFLPHONE_CONTRIB))
$(info SFLPHONE_SRC=$(SFLPHONE_SRC))


include $(CLEAR_VARS)
LOCAL_MODULE := sflphone
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/.libs/libsflphone.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
VERSION="1.1.0"
MY_PREFIX=/sdcard
MY_DATADIR=/data/data

ARCH=$(ANDROID_ABI)

CPP_STATIC= $(ANDROID_NDK)/sources/cxx-stl/gnu-libstdc++$(CXXSTL)/libs/$(ARCH)/libgnustl_static.a \
			$(SFLPHONE_CONTRIB)/lib/libucommon.a \
			$(SFLPHONE_CONTRIB)/lib/libccrtp.a \
			$(SFLPHONE_CONTRIB)/lib/libpjlib-util-arm-unknown-linux-androideabi.a \
			$(SFLPHONE_CONTRIB)/lib/libpj-arm-unknown-linux-androideabi.a \
			$(SFLPHONE_CONTRIB)/lib/libogg.a \
			$(SFLPHONE_CONTRIB)/lib/libFLAC.a \
			$(SFLPHONE_CONTRIB)/lib/libgcrypt.a \
			$(SFLPHONE_CONTRIB)/lib/libgpg-error.a \



LOCAL_CPPFLAGS += -frtti
LOCAL_CPPFLAGS += -fexceptions

LOCAL_SRC_FILES :=  sflphone_wrapper.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH) \
					$(SFLPHONE_SRC)/daemon \
					$(SFLPHONE_SRC)/daemon/src \
					$(SFLPHONE_SRC)/contrib/$(TARGET_TUPLE)/include

LOCAL_MODULE := libsflphonejni

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

LOCAL_LDLIBS  += 	-lz \
					-llog \
					-lOpenSLES \
					-L$(SFLPHONE_CONTRIB)/lib \
					-L$(SFLPHONE_SRC)/daemon/src/.libs \
					$(SFLPHONE_SRC)/daemon/src/.libs/libsflphone.a \
					-lavcodec \
					-lexpat -lhogweed -lpj-arm-unknown-linux-androideabi \
					-lpjsip-simple-arm-unknown-linux-androideabi \
     				-lpjlib-util-arm-unknown-linux-androideabi \
    				-lpjsip-ua-arm-unknown-linux-androideabi \
					-lspeexdsp -lvorbisfile \
					-lavdevice -lFLAC \
     				-liax -lsrtp-arm-unknown-linux-androideabi \
 					-lvpx -lavfilter -lgcrypt -lnettle \
   					-lpjmedia-arm-unknown-linux-androideabi \
         			-lpjsua2-arm-unknown-linux-androideabi \
      				-lswscale -lx264 -lavformat -lgmp \
    				-logg -lpjmedia-audiodev-arm-unknown-linux-androideabi \
					-lpjsua-arm-unknown-linux-androideabi -lucommon -lyaml \
					-lavresample -lgnutls -lopus \
      				-lpjmedia-codec-arm-unknown-linux-androideabi \
					-lresample-arm-unknown-linux-androideabi -lusecure \
					-lavutil -lgnutls-xssl  -lpcre -lpjmedia-videodev-arm-unknown-linux-androideabi \
					-lsamplerate -luuid -lccrtp -lgpg-error -lpcrecpp \
					-lpjnath-arm-unknown-linux-androideabi -lsndfile -lvorbis \
					-lcommoncpp -lgsm -lpcreposix  -lpjsip-arm-unknown-linux-androideabi \
					-lspeex -lvorbisenc \
					$(CPP_STATIC)

include $(BUILD_SHARED_LIBRARY)

########### Codecs ###############

include $(CLEAR_VARS)
LOCAL_MODULE := ulaw
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_ulaw.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := alaw
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_alaw.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g722
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_g722.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := speex_nb
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_speex_nb.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := speex_ub
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_speex_ub.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := speex_wb
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_speex_wb.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opus
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_opus.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := gsm
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_gsm.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g729
LOCAL_SRC_FILES := $(SFLPHONE_SRC)/daemon/src/audio/codecs/libcodec_g729.so
include $(PREBUILT_SHARED_LIBRARY)
