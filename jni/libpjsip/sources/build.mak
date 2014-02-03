export PJDIR := /home/lisional/git/sflphone-android/jni/pjproject-android
include $(PJDIR)/version.mak
export PJ_DIR := $(PJDIR)

# build.mak.  Generated from build.mak.in by configure.
export MACHINE_NAME := auto
export OS_NAME := auto
export HOST_NAME := unix
export CC_NAME := gcc
export TARGET_NAME := arm-unknown-linux-androideabi
export CROSS_COMPILE := arm-linux-androideabi-
export LINUX_POLL := select 
export SHLIB_SUFFIX := so

export prefix := /usr/local
export exec_prefix := ${prefix}
export includedir := ${prefix}/include
export libdir := ${exec_prefix}/lib

LIB_SUFFIX = $(TARGET_NAME).a

ifeq (,1)
export PJ_SHARED_LIBRARIES := 1
endif

# Determine which party libraries to use
export APP_THIRD_PARTY_EXT :=
export APP_THIRD_PARTY_LIBS :=
export APP_THIRD_PARTY_LIB_FILES := $(PJ_DIR)/third_party/lib/libmilenage-$(LIB_SUFFIX)
ifeq ($(PJ_SHARED_LIBRARIES),)
APP_THIRD_PARTY_LIBS += -lmilenage-$(TARGET_NAME)
else
APP_THIRD_PARTY_LIBS += -lmilenage
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libmilenage.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libmilenage.$(SHLIB_SUFFIX)
endif

ifeq (0,1)
# External SRTP library
APP_THIRD_PARTY_EXT += -lsrtp
else
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libsrtp-$(LIB_SUFFIX)
ifeq ($(PJ_SHARED_LIBRARIES),)
APP_THIRD_PARTY_LIBS += -lsrtp-$(TARGET_NAME)
else
APP_THIRD_PARTY_LIBS += -lsrtp
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libsrtp.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libsrtp.$(SHLIB_SUFFIX)
endif
endif

ifeq (libresample,libresample)
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libresample-$(LIB_SUFFIX)
ifeq ($(PJ_SHARED_LIBRARIES),)
ifeq (,1)
export PJ_RESAMPLE_DLL := 1
APP_THIRD_PARTY_LIBS += -lresample
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libresample.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libresample.$(SHLIB_SUFFIX)
else
APP_THIRD_PARTY_LIBS += -lresample-$(TARGET_NAME)
endif
else
APP_THIRD_PARTY_LIBS += -lresample
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libresample.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libresample.$(SHLIB_SUFFIX)
endif
endif

ifneq (1,1)
ifeq (0,1)
# External GSM library
APP_THIRD_PARTY_EXT += -lgsm
else
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libgsmcodec-$(LIB_SUFFIX)
ifeq ($(PJ_SHARED_LIBRARIES),)
APP_THIRD_PARTY_LIBS += -lgsmcodec-$(TARGET_NAME)
else
APP_THIRD_PARTY_LIBS += -lgsmcodec
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libgsmcodec.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libgsmcodec.$(SHLIB_SUFFIX)
endif
endif
endif

ifneq (1,1)
ifeq (0,1)
APP_THIRD_PARTY_EXT += -lspeex -lspeexdsp
else
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libspeex-$(LIB_SUFFIX)
ifeq ($(PJ_SHARED_LIBRARIES),)
APP_THIRD_PARTY_LIBS += -lspeex-$(TARGET_NAME)
else
APP_THIRD_PARTY_LIBS += -lspeex
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libspeex.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libspeex.$(SHLIB_SUFFIX)
endif
endif
endif

ifneq (1,1)
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libilbccodec-$(LIB_SUFFIX)
ifeq ($(PJ_SHARED_LIBRARIES),)
APP_THIRD_PARTY_LIBS += -lilbccodec-$(TARGET_NAME)
else
APP_THIRD_PARTY_LIBS += -lilbccodec
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libilbccodec.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libilbccodec.$(SHLIB_SUFFIX)
endif
endif

ifneq (1,1)
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libg7221codec-$(LIB_SUFFIX)
ifeq ($(PJ_SHARED_LIBRARIES),)
APP_THIRD_PARTY_LIBS += -lg7221codec-$(TARGET_NAME)
else
APP_THIRD_PARTY_LIBS += -lg7221codec
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libg7221codec.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libg7221codec.$(SHLIB_SUFFIX)
endif
endif

ifneq ($(findstring pa,external),)
ifeq (0,1)
# External PA
APP_THIRD_PARTY_EXT += -lportaudio
else
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libportaudio-$(LIB_SUFFIX)
ifeq ($(PJ_SHARED_LIBRARIES),)
APP_THIRD_PARTY_LIBS += -lportaudio-$(TARGET_NAME)
else
APP_THIRD_PARTY_LIBS += -lportaudio
APP_THIRD_PARTY_LIB_FILES += $(PJ_DIR)/third_party/lib/libportaudio.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/third_party/lib/libportaudio.$(SHLIB_SUFFIX)
endif
endif
endif

# Additional flags


#
# Video
# Note: there are duplicated macros in pjmedia/os-auto.mak.in (and that's not
#       good!

# SDL flags
SDL_CFLAGS = 
SDL_LDFLAGS = 

# FFMPEG dlags
FFMPEG_CFLAGS =  
FFMPEG_LDFLAGS =  

# Video4Linux2
V4L2_CFLAGS = 
V4L2_LDFLAGS = 

# QT
AC_PJMEDIA_VIDEO_HAS_QT = 
QT_CFLAGS = 

# iOS
IOS_CFLAGS = 

# PJMEDIA features exclusion
PJ_VIDEO_CFLAGS += $(SDL_CFLAGS) $(FFMPEG_CFLAGS) $(V4L2_CFLAGS) $(QT_CFLAGS) \
		   $(IOS_CFLAGS)
PJ_VIDEO_LDFLAGS += $(SDL_LDFLAGS) $(FFMPEG_LDFLAGS) $(V4L2_LDFLAGS)


# CFLAGS, LDFLAGS, and LIBS to be used by applications
export APP_CC := /home/lisional/Dev/ADT/ndk/toolchains/arm-linux-androideabi-4.8/prebuilt/linux-x86_64/bin/arm-linux-androideabi-gcc
export APP_CXX := /home/lisional/Dev/ADT/ndk/toolchains/arm-linux-androideabi-4.8/prebuilt/linux-x86_64/bin/arm-linux-androideabi-g++
export APP_CFLAGS := -DPJ_AUTOCONF=1\
	 -fpic -ffunction-sections -funwind-tables -no-canonical-prefixes -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16 -mthumb -Os -g -DNDEBUG -fomit-frame-pointer -finline-limit=64 -O0 -UNDEBUG -marm -fno-omit-frame-pointer -Ijni -DANDROID -Wa,--noexecstack -Wformat -Werror=format-security -I/home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm/usr/include  -I/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/include -I/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a/include -DPJ_IS_BIG_ENDIAN=0 -DPJ_IS_LITTLE_ENDIAN=1\
	$(PJ_VIDEO_CFLAGS) \
	-I$(PJDIR)/pjlib/include\
	-I$(PJDIR)/pjlib-util/include\
	-I$(PJDIR)/pjnath/include\
	-I$(PJDIR)/pjmedia/include\
	-I$(PJDIR)/pjsip/include
export APP_CXXFLAGS := $(APP_CFLAGS)
#  x   x  x  x  x  x  x   x  x  x  x  x  x   x  x  x  x  x  x   x  x  x  x  x
#
# FIX THIS
#
# pjsua2 is c++ library hence maybe needs to be put in separate
# variables. it will also require -lstdc++ or -static-libstdc++
#  x   x  x  x  x  x  x   x  x  x  x  x  x   x  x  x  x  x  x   x  x  x  x  x
export APP_LDFLAGS := -L$(PJDIR)/pjlib/lib\
	-L$(PJDIR)/pjlib-util/lib\
	-L$(PJDIR)/pjnath/lib\
	-L$(PJDIR)/pjmedia/lib\
	-L$(PJDIR)/pjsip/lib\
	-L$(PJDIR)/third_party/lib\
	$(PJ_VIDEO_LDFLAGS) \
	-static-libstdc++ \
	 -nostdlib -L/home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm/usr/lib/ -L/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a

#  x   x  x  x  x  x  x   x  x  x  x  x  x   x  x  x  x  x  x   x  x  x  x  x
#
# FIX THIS
#
# pjsua2 is c++ library hence maybe needs to be put in separate
# variables. it will also require -lstdc++
#  x   x  x  x  x  x  x   x  x  x  x  x  x   x  x  x  x  x  x   x  x  x  x  x
export APP_LIB_FILES = \
	$(PJ_DIR)/pjsip/lib/libpjsua2-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjsip/lib/libpjsua-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjsip/lib/libpjsip-ua-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjsip/lib/libpjsip-simple-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjsip/lib/libpjsip-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjmedia/lib/libpjmedia-codec-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjmedia/lib/libpjmedia-videodev-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjmedia/lib/libpjmedia-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjmedia/lib/libpjmedia-audiodev-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjnath/lib/libpjnath-$(LIB_SUFFIX) \
	$(PJ_DIR)/pjlib-util/lib/libpjlib-util-$(LIB_SUFFIX) \
	$(APP_THIRD_PARTY_LIB_FILES) \
	$(PJ_DIR)/pjlib/lib/libpj-$(LIB_SUFFIX)

ifeq ($(PJ_SHARED_LIBRARIES),)
export PJLIB_LDLIB := -lpj-$(TARGET_NAME)
export PJLIB_UTIL_LDLIB := -lpjlib-util-$(TARGET_NAME)
export PJNATH_LDLIB := -lpjnath-$(TARGET_NAME)
export PJMEDIA_AUDIODEV_LDLIB := -lpjmedia-audiodev-$(TARGET_NAME)
export PJMEDIA_VIDEODEV_LDLIB := -lpjmedia-videodev-$(TARGET_NAME)
export PJMEDIA_LDLIB := -lpjmedia-$(TARGET_NAME)
export PJMEDIA_CODEC_LDLIB := -lpjmedia-codec-$(TARGET_NAME)
export PJSIP_LDLIB := -lpjsip-$(TARGET_NAME)
export PJSIP_SIMPLE_LDLIB := -lpjsip-simple-$(TARGET_NAME)
export PJSIP_UA_LDLIB := -lpjsip-ua-$(TARGET_NAME)
export PJSUA_LIB_LDLIB := -lpjsua-$(TARGET_NAME)
export PJSUA2_LIB_LDLIB := -lpjsua2-$(TARGET_NAME)
else
export PJLIB_LDLIB := -lpj
export PJLIB_UTIL_LDLIB := -lpjlib-util
export PJNATH_LDLIB := -lpjnath
export PJMEDIA_AUDIODEV_LDLIB := -lpjmedia-audiodev
export PJMEDIA_VIDEODEV_LDLIB := -lpjmedia-videodev
export PJMEDIA_LDLIB := -lpjmedia
export PJMEDIA_CODEC_LDLIB := -lpjmedia-codec
export PJSIP_LDLIB := -lpjsip
export PJSIP_SIMPLE_LDLIB := -lpjsip-simple
export PJSIP_UA_LDLIB := -lpjsip-ua
export PJSUA_LIB_LDLIB := -lpjsua
export PJSUA2_LIB_LDLIB := -lpjsua2

APP_LIB_FILES += $(PJ_DIR)/pjsip/lib/libpjsua.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjsip/lib/libpjsua.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjsip/lib/libpjsip-ua.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjsip/lib/libpjsip-ua.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjsip/lib/libpjsip-simple.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjsip/lib/libpjsip-simple.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjsip/lib/libpjsip.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjsip/lib/libpjsip.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjmedia/lib/libpjmedia-codec.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjmedia/lib/libpjmedia-codec.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjmedia/lib/libpjmedia-videodev.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjmedia/lib/libpjmedia-videodev.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjmedia/lib/libpjmedia.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjmedia/lib/libpjmedia.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjmedia/lib/libpjmedia-audiodev.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjmedia/lib/libpjmedia-audiodev.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjnath/lib/libpjnath.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjnath/lib/libpjnath.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjlib-util/lib/libpjlib-util.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjlib-util/lib/libpjlib-util.$(SHLIB_SUFFIX) \
	$(PJ_DIR)/pjlib/lib/libpj.$(SHLIB_SUFFIX).$(PJ_VERSION_MAJOR) $(PJ_DIR)/pjlib/lib/libpj.$(SHLIB_SUFFIX)
endif

export APP_LDLIBS := $(PJSUA2_LIB_LDLIB) \
	$(PJSUA_LIB_LDLIB) \
	$(PJSIP_UA_LDLIB) \
	$(PJSIP_SIMPLE_LDLIB) \
	$(PJSIP_LDLIB) \
	$(PJMEDIA_CODEC_LDLIB) \
	$(PJMEDIA_LDLIB) \
	$(PJMEDIA_VIDEODEV_LDLIB) \
	$(PJMEDIA_AUDIODEV_LDLIB) \
	$(PJNATH_LDLIB) \
	$(PJLIB_UTIL_LDLIB) \
	$(APP_THIRD_PARTY_LIBS)\
	$(APP_THIRD_PARTY_EXT)\
	$(PJLIB_LDLIB) \
	-lm /home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm/usr/lib/crtbegin_so.o -lgnustl_shared  -lc -lgcc -llog -lgcc

# Here are the variabels to use if application is using the library
# from within the source distribution
export PJ_CC := $(APP_CC)
export PJ_CXX := $(APP_CXX)
export PJ_CFLAGS := $(APP_CFLAGS)
export PJ_CXXFLAGS := $(APP_CXXFLAGS)
export PJ_LDFLAGS := $(APP_LDFLAGS)
export PJ_LDLIBS := $(APP_LDLIBS)
export PJ_LIB_FILES := $(APP_LIB_FILES)

# And here are the variables to use if application is using the
# library from the install location (i.e. --prefix)
export PJ_INSTALL_DIR := /usr/local
export PJ_INSTALL_INC_DIR := ${prefix}/include
export PJ_INSTALL_LIB_DIR := ${exec_prefix}/lib
export PJ_INSTALL_CFLAGS := -I$(PJ_INSTALL_INC_DIR) -DPJ_AUTOCONF=1	 -fpic -ffunction-sections -funwind-tables -no-canonical-prefixes -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16 -mthumb -Os -g -DNDEBUG -fomit-frame-pointer -finline-limit=64 -O0 -UNDEBUG -marm -fno-omit-frame-pointer -Ijni -DANDROID -Wa,--noexecstack -Wformat -Werror=format-security -I/home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm/usr/include  -I/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/include -I/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a/include -DPJ_IS_BIG_ENDIAN=0 -DPJ_IS_LITTLE_ENDIAN=1
export PJ_INSTALL_CXXFLAGS := $(PJ_INSTALL_CFLAGS)
export PJ_INSTALL_LDFLAGS := -L$(PJ_INSTALL_LIB_DIR) $(APP_LDLIBS)
