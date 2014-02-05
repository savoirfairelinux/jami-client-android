
include $(CLEAR_VARS)

MY_LIBEXPAT := libexpat/sources

common_SRC_FILES := $(MY_LIBEXPAT)/xmlparse.c \
					$(MY_LIBEXPAT)/xmlrole.c \
					$(MY_LIBEXPAT)/xmltok.c

common_CFLAGS := -Wall -Wmissing-prototypes -Wstrict-prototypes -fexceptions -DHAVE_EXPAT_CONFIG_H

common_C_INCLUDES += $(MY_LIBEXPAT)

common_COPY_HEADERS_TO := libexpat

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += $(common_C_INCLUDES)

LOCAL_MODULE:= libexpat

include $(BUILD_STATIC_LIBRARY)