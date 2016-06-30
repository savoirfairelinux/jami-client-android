 #  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 #
 #  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 #			Adrien Beraud <adrien.beraud@savoirfairelinux.com>
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

LOCAL_PATH:= $(call my-dir)

$(info PWD=$(PWD))
$(info RING_CONTRIB=$(RING_CONTRIB))
$(info RING_SRC_DIR=$(RING_SRC_DIR))
$(info RING_BUILD_DIR=$(RING_BUILD_DIR))

include $(CLEAR_VARS)

VERSION="1.1.0"
MY_PREFIX=/sdcard
MY_DATADIR=/data/data

ARCH=$(ANDROID_ABI)

CPP_STATIC= $(ANDROID_NDK)/sources/cxx-stl/gnu-libstdc++$(CXXSTL)/libs/$(ARCH)/libgnustl_static.a \
			$(RING_CONTRIB)/lib/libgnutls.a \
			$(RING_CONTRIB)/lib/libnettle.a \
			$(RING_CONTRIB)/lib/libhogweed.a \
			$(RING_CONTRIB)/lib/libogg.a \
			$(RING_CONTRIB)/lib/libFLAC.a \
			$(RING_CONTRIB)/lib/libavcodec.a \
			$(RING_CONTRIB)/lib/libavfilter.a \
			$(RING_CONTRIB)/lib/libavformat.a \
			$(RING_CONTRIB)/lib/libavdevice.a \
			$(RING_CONTRIB)/lib/libavutil.a \
			$(RING_CONTRIB)/lib/libswscale.a \
			$(RING_CONTRIB)/lib/libz.a \
			$(RING_CONTRIB)/lib/libupnp.a \
			$(RING_CONTRIB)/lib/libthreadutil.a \
			$(RING_CONTRIB)/lib/libiconv.a \
			$(RING_CONTRIB)/lib/libixml.a \
			$(RING_CONTRIB)/lib/libgmp.a \
			$(RING_CONTRIB)/lib/libopendht.a \
			$(RING_CONTRIB)/lib/libjsoncpp.a

ifeq ($(ARCH),$(filter $(ARCH),x86))
CPP_STATIC += $(RING_CONTRIB)/lib/libpjlib-util-i686-pc-linux-android.a \
			$(RING_CONTRIB)/lib/libpj-i686-pc-linux-android.a
else
CPP_STATIC += $(RING_CONTRIB)/lib/libpjlib-util-arm-unknown-linux-androideabi.a \
			$(RING_CONTRIB)/lib/libpj-arm-unknown-linux-androideabi.a
endif

LOCAL_SRC_FILES :=  ring_wrapper.cpp

# RING_BUILD_DIR contains config.h, which we need
LOCAL_C_INCLUDES += $(LOCAL_PATH) \
					$(RING_BUILD_DIR) \
					$(RING_SRC_DIR) \
					$(RING_SRC_DIR)/src \
					$(RING_SRC_DIR)/contrib/$(TARGET_TUPLE)/include

LOCAL_MODULE := libringjni

LOCAL_CFLAGS   +=   -fpic

LOCAL_CPPFLAGS += 	-DCCPP_PREFIX \
					-DPROGSHAREDIR=\"${MY_DATADIR}/ring\" \
					-DHAVE_CONFIG_H \
					-DHAVE_SPEEX_CODEC \
					-DHAVE_GSM_CODEC \
					-w -frtti -fpic \
					-std=c++11 -fexceptions -fpermissive \
					-DAPP_NAME=\"Ring\" \
					-DSWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON \
					-DDEBUG_DIRECTOR_OWNED \
					-DPJ_AUTOCONF=1

LOCAL_DISABLE_FATAL_LINKER_WARNINGS = true

LOCAL_LDFLAGS := -L$(RING_CONTRIB)/lib \

LOCAL_LDLIBS  += 	-lz \
					-llog \
					-lOpenSLES \
					$(RING_BUILD_DIR)/src/.libs/libring.a \


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

LOCAL_LDLIBS	+=	-lhogweed \
					-lspeexdsp -lvorbisfile -lyaml-cpp -ljsoncpp \
					-lFLAC -lnettle \
					-logg \
					-lpcre -lsamplerate -luuid \
					-lsndfile -lvorbis \
					-lspeex -lvorbisenc \
					-lgmp -lgnutls -lopendht \
					-lavformat -lavcodec -lavutil \
					-lopus -lspeex \
					-landroid \
					$(CPP_STATIC)


include $(BUILD_SHARED_LIBRARY)

########### Codecs ###############

#include $(CLEAR_VARS)
#LOCAL_MODULE := ulaw
#LOCAL_SRC_FILES := ../$(RING_BUILD_DIR)/src/audio/codecs/libcodec_ulaw.so
#include $(PREBUILT_SHARED_LIBRARY)
