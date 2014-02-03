##########
# PJNATH #
##########

include $(CLEAR_VARS)

LOCAL_MODULE    := pjnath

PJLIB_DIR := libpjsip/sources/pjnath

LOCAL_C_INCLUDES += $(PJLIB_DIR)/../pjlib/include \
					$(PJLIB_DIR)/../pjlib-util/include \
					$(PJLIB_DIR)/include

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := $(PJLIB_DIR)/src/pjnath

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/errno.c \
	$(PJLIB_SRC_DIR)/ice_session.c \
	$(PJLIB_SRC_DIR)/ice_strans.c \
	$(PJLIB_SRC_DIR)/nat_detect.c \
	$(PJLIB_SRC_DIR)/stun_auth.c \
	$(PJLIB_SRC_DIR)/stun_msg.c \
	$(PJLIB_SRC_DIR)/stun_msg_dump.c \
	$(PJLIB_SRC_DIR)/stun_session.c \
	$(PJLIB_SRC_DIR)/stun_sock.c \
	$(PJLIB_SRC_DIR)/stun_transaction.c \
	$(PJLIB_SRC_DIR)/turn_session.c \
	$(PJLIB_SRC_DIR)/turn_sock.c 


include $(BUILD_STATIC_LIBRARY)

