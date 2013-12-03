# libyaml/jni/Android.mk

include $(CLEAR_VARS)

MY_LIBYAML := libyaml/sources

LOCAL_CFLAGS := -DYAML_VERSION_STRING=\"0.1.4\" \
				-DYAML_VERSION_MAJOR=0 \
				-DYAML_VERSION_MINOR=1 \
				-DYAML_VERSION_PATCH=4

LOCAL_MODULE     := libyaml

LOCAL_SRC_FILES  := $(MY_LIBYAML)/api.c \
					$(MY_LIBYAML)/reader.c \
					$(MY_LIBYAML)/scanner.c \
					$(MY_LIBYAML)/parser.c \
					$(MY_LIBYAML)/loader.c \
					$(MY_LIBYAML)/writer.c \
					$(MY_LIBYAML)/emitter.c \
					$(MY_LIBYAML)/dumper.c

LOCAL_C_INCLUDES += $(MY_LIBYAML)/inc

include $(BUILD_STATIC_LIBRARY)
