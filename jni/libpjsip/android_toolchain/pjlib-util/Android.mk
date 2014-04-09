##############
# PJLIB-UTIL #
##############

include $(CLEAR_VARS)

LOCAL_MODULE    := pjlib-util

PJLIB_DIR := sflphone/daemon/libs/pjproject-2.2.1/pjlib-util

LOCAL_C_INCLUDES += $(PJLIB_DIR)/include \
					$(PJLIB_DIR)/../pjlib/include

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := $(PJLIB_DIR)/src/pjlib-util

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/base64.c \
					$(PJLIB_SRC_DIR)/crc32.c \
					$(PJLIB_SRC_DIR)/errno.c \
					$(PJLIB_SRC_DIR)/dns.c \
					$(PJLIB_SRC_DIR)/dns_dump.c \
					$(PJLIB_SRC_DIR)/dns_server.c \
					$(PJLIB_SRC_DIR)/getopt.c \
					$(PJLIB_SRC_DIR)/hmac_md5.c \
					$(PJLIB_SRC_DIR)/hmac_sha1.c \
					$(PJLIB_SRC_DIR)/md5.c \
					$(PJLIB_SRC_DIR)/pcap.c \
					$(PJLIB_SRC_DIR)/resolver.c \
					$(PJLIB_SRC_DIR)/scanner.c \
					$(PJLIB_SRC_DIR)/sha1.c \
					$(PJLIB_SRC_DIR)/srv_resolver.c \
					$(PJLIB_SRC_DIR)/string.c \
					$(PJLIB_SRC_DIR)/stun_simple.c \
					$(PJLIB_SRC_DIR)/stun_simple_client.c \
					$(PJLIB_SRC_DIR)/xml.c

include $(BUILD_STATIC_LIBRARY)

