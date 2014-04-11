
include $(CLEAR_VARS)

# Path to library folder
MY_IAX := sflphone/daemon/libs/iax2

# .c files
LOCAL_SRC_FILES := $(MY_IAX)/iax.c \
					$(MY_IAX)/iax2-parser.c \
					$(MY_IAX)/jitterbuf.c \
					$(MY_IAX)/options.c \
					$(MY_IAX)/md5.c \

LOCAL_CFLAGS := -DLIBIAX $(LOCAL_CFLAGS)

# headers locations
LOCAL_C_INCLUDES += $(MY_IAX) \

LOCAL_MODULE:= libiax2

include $(BUILD_STATIC_LIBRARY)
