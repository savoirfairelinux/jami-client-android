# libyaml/jni/Android.mk

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS := -DYAML_VERSION_STRING=\"0.1.4\" \
				-DYAML_VERSION_MAJOR=0 \
				-DYAML_VERSION_MINOR=1 \
				-DYAML_VERSION_PATCH=4
LOCAL_MODULE     := libyaml
LOCAL_LDLIBS     := -L$(SYSROOT)/usr/lib
LOCAL_SRC_FILES  := api.c reader.c scanner.c \
                    parser.c loader.c writer.c emitter.c dumper.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)/inc

#include $(BUILD_STATIC_LIBRARY) 
include $(BUILD_SHARED_LIBRARY)
