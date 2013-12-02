include $(CLEAR_VARS)

MY_LIBGSM = libgsm/sources

LOCAL_SRC_FILES := 	$(MY_LIBGSM)/src/add.c \
					$(MY_LIBGSM)/src/debug.c \
					$(MY_LIBGSM)/src/gsm_create.c \
					$(MY_LIBGSM)/src/gsm_destroy.c \
					$(MY_LIBGSM)/src/gsm_explode.c \
					$(MY_LIBGSM)/src/gsm_option.c \
					$(MY_LIBGSM)/src/long_term.c \
					$(MY_LIBGSM)/src/preprocess.c \
					$(MY_LIBGSM)/src/short_term.c \
					$(MY_LIBGSM)/src/code.c \
					$(MY_LIBGSM)/src/gsm_decode.c \
					$(MY_LIBGSM)/src/gsm_encode.c \
					$(MY_LIBGSM)/src/decode.c \
					$(MY_LIBGSM)/src/gsm_print.c \
					$(MY_LIBGSM)/src/lpc.c \
					$(MY_LIBGSM)/src/rpe.c \
					$(MY_LIBGSM)/src/table.c \

LOCAL_C_INCLUDES := $(MY_LIBGSM)/inc

LOCAL_MODULE := libgsm

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
					-DCCPP_PREFIX \
					-DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
					-DPREFIX=\"$(MY_PREFIX)\" \
					-DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
					-DHAVE_CONFIG_H \
					-std=c++11 -frtti -fpermissive -fexceptions \
					-DAPP_NAME=\"codecfactory\"

include $(BUILD_STATIC_LIBRARY)
