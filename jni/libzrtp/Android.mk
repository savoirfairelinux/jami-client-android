ROOT_SRC_PATH := $(LOCAL_PATH)/libzrtp/sources

OLD_PATH = $(LOCAL_PATH)
#
# Define and build the zrtpcpp static lib
#
include $(CLEAR_VARS)


LOCAL_MODULE := libzrtpcpp
#LOCAL_CPP_FEATURES := exceptions

#LOCAL_CPPFLAGS += -std=c++11
LOCAL_CPPFLAGS += -frtti
LOCAL_CPPFLAGS += -fexceptions
#LOCAL_CPPFLAGS += -fpermissive

MY_COMMONCPP = libucommon/sources
MY_CCRTP = libccrtp/sources
MY_OPENSSL = libopenssl

# include paths for zrtpcpp modules
LOCAL_C_INCLUDES += $(ROOT_SRC_PATH) \
                    $(ROOT_SRC_PATH)/srtp \
                    $(ROOT_SRC_PATH)/src \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/openssl \
                    ${MY_COMMONCPP}/inc \
                    $(MY_OPENSSL)/include \
                    $(MY_CCRTP)/src \


LOCAL_SRC_FILES +=  $(ROOT_SRC_PATH)/src/ZrtpCallbackWrapper.cpp \
                    $(ROOT_SRC_PATH)/src/ZRtp.cpp \
                    $(ROOT_SRC_PATH)/src/ZIDFile.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpCrc32.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketCommit.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketConf2Ack.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketConfirm.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketDHPart.cpp \
                    $(ROOT_SRC_PATH)/src/Base32.cpp \
                    $(ROOT_SRC_PATH)/src/ZIDRecord.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketGoClear.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketClearAck.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketHelloAck.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketHello.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketError.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketErrorAck.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketPingAck.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketPing.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketSASrelay.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpPacketRelayAck.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpStateClass.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpTextData.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpConfigure.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpCWrapper.cpp \
                    $(ROOT_SRC_PATH)/src/ZrtpQueue.cpp \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/TwoCFB.cpp \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/twofish_cfb.c \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/twofish.c \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/openssl/sha256.cpp \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/openssl/hmac384.cpp \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/openssl/hmac256.cpp \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/openssl/sha384.cpp \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/openssl/ZrtpDH.cpp \
                    $(ROOT_SRC_PATH)/src/libzrtpcpp/crypto/openssl/AesCFB.cpp \
                    $(ROOT_SRC_PATH)/srtp/crypto/skein.c \
                    $(ROOT_SRC_PATH)/srtp/crypto/skeinApi.c \
                    $(ROOT_SRC_PATH)/srtp/crypto/skein_block.c \
                    $(ROOT_SRC_PATH)/srtp/crypto/macSkein.cpp \
                    $(ROOT_SRC_PATH)/srtp/CryptoContext.cpp \
                    $(ROOT_SRC_PATH)/srtp/CryptoContextCtrl.cpp \
                    $(ROOT_SRC_PATH)/srtp/crypto/openssl/hmac.cpp \
                    $(ROOT_SRC_PATH)/srtp/crypto/openssl/SrtpSymCrypto.cpp \


LOCAL_STATIC_LIBRARY += libccrtp1


include $(BUILD_STATIC_LIBRARY)

