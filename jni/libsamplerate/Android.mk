include $(CLEAR_VARS)

MY_LIBSAMPLE := libsamplerate/sources

# We need to build this for both the device (as a shared library)
# and the host (as a static library for tools to use).

common_SRC_FILES := $(MY_LIBSAMPLE)/src/samplerate.c \
                    $(MY_LIBSAMPLE)/src/src_sinc.c \
					$(MY_LIBSAMPLE)/src/src_zoh.c \
					$(MY_LIBSAMPLE)/src/src_linear.c

# For the device
# =====================================================

# Device shared library
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)

LOCAL_CFLAGS += -Werror -g

LOCAL_C_INCLUDES += $(MY_LIBSAMPLE)

LOCAL_MODULE:= libsamplerate

include $(BUILD_SHARED_LIBRARY)
