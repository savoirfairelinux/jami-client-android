LOCAL_PATH:= $(call my-dir)

# We need to build this for both the device (as a shared library)
# and the host (as a static library for tools to use).

common_SRC_FILES := \
	xmlparse.c \
	xmlrole.c \
	xmltok.c

common_CFLAGS := -Wall -Wmissing-prototypes -Wstrict-prototypes -fexceptions -DHAVE_EXPAT_CONFIG_H

#common_C_INCLUDES += $(LOCAL_PATH)/../lib \
#                     $(LOCAL_PATH)/..

common_COPY_HEADERS_TO := libexpat
common_COPY_HEADERS := \
	lib/expat.h \
	lib/expat_external.h

# For the device
# =====================================================

# Device static library
include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH),arm)
LOCAL_NDK_VERSION := 4
LOCAL_SDK_VERSION := 8
endif

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += $(common_C_INCLUDES)

LOCAL_MODULE:= libexpat_static
LOCAL_MODULE_FILENAME := libexpat
LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)

# Device shared library
include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH),arm)
LOCAL_NDK_VERSION := 4
LOCAL_SDK_VERSION := 8
endif

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += $(common_C_INCLUDES)

LOCAL_MODULE:= libexpat_shared
LOCAL_MODULE_FILENAME := libexpat
LOCAL_MODULE_TAGS := optional
LOCAL_COPY_HEADERS_TO := $(common_COPY_HEADERS_TO)
LOCAL_COPY_HEADERS := $(common_COPY_HEADERS)

include $(BUILD_SHARED_LIBRARY)
