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
include $(CLEAR_VARS)

LOCAL_CODECS_PATH = $(SFLPHONE_SRC)/daemon/src/audio/codecs
LOCAL_SRC_PATH = $(SFLPHONE_SRC)/daemon/src

$(info SFLPHONE_CONTRIB=$(SFLPHONE_CONTRIB))
$(info SFLPHONE_SRC=$(SFLPHONE_SRC))

include $(CLEAR_VARS)
VERSION="1.1.0"
MY_PREFIX=/sdcard
MY_DATADIR=/data/data

ARCH=$(ANDROID_ABI)

CPP_STATIC=$(ANDROID_NDK)/sources/cxx-stl/gnu-libstdc++$(CXXSTL)/libs/$(ARCH)/libgnustl_static.a

LOCAL_CPPFLAGS += -frtti
LOCAL_CPPFLAGS += -fexceptions

LOCAL_SRC_FILES :=  sflphone_wrapper.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH) \
					$(LOCAL_SRC_PATH) \
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
					-lsflphone \
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

LOCAL_SHARE_LIBRARIES := libsflphone
include $(BUILD_SHARED_LIBRARY)
