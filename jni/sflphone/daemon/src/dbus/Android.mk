LOCAL_PATH:= $(call my-dir)

# FIXME
prefix=/sdcard
datadir=

# FIXME
ifneq ($(SFL_VIDEO),)
video_SOURCES += video_controls.cpp
video_controls-glue.h: video_controls-introspec.xml Makefile.am
	dbusxx-xml2cpp $< --adaptor=$@
endif

ifneq ($(USE_NETWORKMANAGER),)
network_SOURCES += networkmanager.cpp
NETWORKMANAGER = -DUSE_NETWORKMANAGER
endif

# should work...
ifneq ($(FIXME),)
# Rule to generate the binding headers
%-glue.h: %-introspec.xml Makefile.am
	dbusxx-xml2cpp $< --adaptor=$@
endif


include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(video_SOURCES) $(network_SOURCES) \
	callmanager.cpp \
    configurationmanager.cpp  \
    instance.cpp  \
    dbusmanager.cpp

# FIXME
LOCAL_C_INCLUDES += $(LOCAL_PATH)/.. \
					$(LOCAL_PATH)/../sip \
					$(LOCAL_PATH)/../../libs/pjproject/third_party/speex/include \
					$(LOCAL_PATH)/../../libs/pjproject/pjlib/include \
					$(LOCAL_PATH)/../../libs/pjproject/pjsip/include \
					$(LOCAL_PATH)/../../libs/pjproject/pjlib-util/include \
					$(LOCAL_PATH)/../../libs/pjproject/third_party/build/speex \
					$(APP_PROJECT_PATH)/jni/commoncpp2-1.8.1-android/inc \
					$(APP_PROJECT_PATH)/jni/ccrtp-1.8.0-android/src
LOCAL_MODULE := libdbus-glue
LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DPREFIX=\"$(prefix)\" \
				  -DPROGSHAREDIR=\"${datadir}/sflphone\"
LOCAL_SHARED_LIBRARIES += libdbus-c++-1

include $(BUILD_STATIC_LIBRARY)

