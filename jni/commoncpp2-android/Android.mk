include $(CLEAR_VARS)

LT_VERSION = "0:1"
LT_RELEASE = "1.8"
SHARED_FLAGS = "-no-undefined"

LOCAL_COMMONCPP_PATH = commoncpp2-android/sources/src

LOCAL_CPPFLAGS   += -std=c++11 -Wno-psabi -frtti -pthread -fexceptions

LOCAL_MODULE     := libccgnu2

LOCAL_LDLIBS     := -L$(SYSROOT)/usr/lib

LOCAL_C_INCLUDES += $(LOCAL_COMMONCPP_PATH) \
			$(LOCAL_COMMONCPP_PATH)/../inc
		

LOCAL_SRC_FILES  := $(LOCAL_COMMONCPP_PATH)/thread.cpp \
		$(LOCAL_COMMONCPP_PATH)/mutex.cpp \
		$(LOCAL_COMMONCPP_PATH)/semaphore.cpp \
		$(LOCAL_COMMONCPP_PATH)/threadkey.cpp \
		$(LOCAL_COMMONCPP_PATH)/friends.cpp \
		$(LOCAL_COMMONCPP_PATH)/event.cpp \
		$(LOCAL_COMMONCPP_PATH)/slog.cpp \
		$(LOCAL_COMMONCPP_PATH)/dir.cpp \
		$(LOCAL_COMMONCPP_PATH)/file.cpp \
		$(LOCAL_COMMONCPP_PATH)/inaddr.cpp \
		$(LOCAL_COMMONCPP_PATH)/peer.cpp \
		$(LOCAL_COMMONCPP_PATH)/timer.cpp \
		$(LOCAL_COMMONCPP_PATH)/socket.cpp \
		$(LOCAL_COMMONCPP_PATH)/strchar.cpp \
		$(LOCAL_COMMONCPP_PATH)/simplesocket.cpp \
		$(LOCAL_COMMONCPP_PATH)/mempager.cpp \
		$(LOCAL_COMMONCPP_PATH)/keydata.cpp \
		$(LOCAL_COMMONCPP_PATH)/dso.cpp \
		$(LOCAL_COMMONCPP_PATH)/exception.cpp \
		$(LOCAL_COMMONCPP_PATH)/missing.cpp \
		$(LOCAL_COMMONCPP_PATH)/process.cpp \
		$(LOCAL_COMMONCPP_PATH)/string.cpp \
		$(LOCAL_COMMONCPP_PATH)/in6addr.cpp \
		$(LOCAL_COMMONCPP_PATH)/buffer.cpp \
		$(LOCAL_COMMONCPP_PATH)/lockfile.cpp \
		$(LOCAL_COMMONCPP_PATH)/nat.cpp \
		$(LOCAL_COMMONCPP_PATH)/runlist.cpp \
		$(LOCAL_COMMONCPP_PATH)/assoc.cpp \
		$(LOCAL_COMMONCPP_PATH)/pointer.cpp \
		$(LOCAL_COMMONCPP_PATH)/linked.cpp \
		$(LOCAL_COMMONCPP_PATH)/map.cpp \
		$(LOCAL_COMMONCPP_PATH)/cidr.cpp

#LOCAL_LDFLAGS    := -version-info $(LT_VERSION) -release $(LT_RELEASE) $(SHARED_FLAGS)

include $(BUILD_SHARED_LIBRARY)

