LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LT_VERSION = "0:1"
LT_RELEASE = "1.8"
SHARED_FLAGS = "-no-undefined"

LOCAL_CPPFLAGS   += -std=gnu++0x -Wno-psabi -frtti -pthread -fexceptions
LOCAL_MODULE     := libccgnu2
LOCAL_LDLIBS     := -L$(SYSROOT)/usr/lib

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../inc

LOCAL_SRC_FILES  := thread.cpp mutex.cpp semaphore.cpp threadkey.cpp \
	friends.cpp event.cpp slog.cpp dir.cpp file.cpp inaddr.cpp \
	peer.cpp timer.cpp socket.cpp strchar.cpp simplesocket.cpp \
	mempager.cpp keydata.cpp dso.cpp exception.cpp missing.cpp \
	process.cpp string.cpp in6addr.cpp buffer.cpp lockfile.cpp \
	nat.cpp runlist.cpp assoc.cpp pointer.cpp linked.cpp map.cpp \
	cidr.cpp

#LOCAL_LDFLAGS    := -version-info $(LT_VERSION) -release $(LT_RELEASE) $(SHARED_FLAGS)

include $(BUILD_SHARED_LIBRARY)

