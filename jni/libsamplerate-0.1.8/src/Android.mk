LOCAL_PATH:= $(call my-dir)

# We need to build this for both the device (as a shared library)
# and the host (as a static library for tools to use).

common_SRC_FILES := samplerate.c \
                    src_sinc.c \
					src_zoh.c \
					src_linear.c

# For the device
# =====================================================

# Device shared library
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += -Werror -g
LOCAL_LDFLAGS := 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/..

LOCAL_MODULE:= libsamplerate

include $(BUILD_SHARED_LIBRARY)
