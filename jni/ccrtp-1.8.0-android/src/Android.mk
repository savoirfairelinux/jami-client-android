LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LT_VERSION = 
LT_RELEASE = 
SHARED_FLAGS = "-no-undefined"
SRTP_OPENSSL =
SRTP_GCRYPT =

#LOCAL_CPPFLAGS   += -Wno-psabi -frtti -pthread -fexceptions
LOCAL_CPPFLAGS   += -std=gnu++0x -fexceptions
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../commoncpp2-1.8.1-android/inc
LOCAL_MODULE     := libccrtp1
LOCAL_STATIC_LIBRARIES := libccgnu2
LOCAL_LDLIBS     := -L$(SYSROOT)/usr/lib -lccgnu2
LOCAL_CPP_EXTENSION := .cxx .cpp

ifneq ($(SRTP_OPENSSL),)
SRTP_SRC_O = 	ccrtp/crypto/openssl/hmac.cpp \
				ccrtp/crypto/openssl/AesSrtp.cxx \
				ccrtp/crypto/openssl/InitializeOpenSSL.cxx
endif

ifneq ($(SRTP_GCRYPT),)
SRTP_SRC_G =    ccrtp/crypto/gcrypt/gcrypthmac.cxx \
				ccrtp/crypto/gcrypt/gcryptAesSrtp.cxx \
				ccrtp/crypto/gcrypt/InitializeGcrypt.cxx
endif

SKEIN_SRCS = ccrtp/crypto/macSkein.cpp \
        ccrtp/crypto/skein.c \
        ccrtp/crypto/skein_block.c \
        ccrtp/crypto/skeinApi.c

LOCAL_SRC_FILES  := rtppkt.cpp rtcppkt.cpp source.cpp data.cpp incqueue.cpp \
	outqueue.cpp queue.cpp control.cpp members.cpp socket.cpp duplex.cpp pool.cpp \
	CryptoContext.cxx $(SRTP_SRC_G) $(SRTP_SRC_O) $(SKEIN_SRCS)


#LOCAL_LDFLAGS    := -version-info $(LT_VERSION) -release $(LT_RELEASE) $(SHARED_FLAGS)

include $(BUILD_SHARED_LIBRARY)
