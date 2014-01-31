ROOT_SRC_PATH := $(LOCAL_PATH)/libzrtp/sources

OLD_PATH = $(LOCAL_PATH)
LOCAL_PATH = libzrtp/sources
#
# Define and build the zrtpcpp static lib
#
include $(CLEAR_VARS)
LOCAL_MODULE := libzrtpcpp
LOCAL_CPP_FEATURES := exceptions

MY_CCRTP = libccrtp/sources
MY_COMMONCPP = libucommon/sources

# include paths for zrtpcpp modules
LOCAL_C_INCLUDES += $(ROOT_SRC_PATH) \
                    $(ROOT_SRC_PATH)/srtp \
                    $(ROOT_SRC_PATH)/zrtp \
                    $(ROOT_SRC_PATH)/clients/ccrtp \
                    $(MY_CCRTP)/src \
                    ${MY_COMMONCPP}/inc \
                    $(ROOT_SRC_PATH)/bnlib \
                    $(ROOT_SRC_PATH)/bnlib/ec

EC_SRCS =   bnlib/ec/ec.c \
            bnlib/ec/ecdh.c

COMMON_SRCS =   common/osSpecifics.c \
                common/Thread.cpp \
                common/MutexClass.cpp \
                common/EventClass.cpp

BNLIB_SRCS =    bnlib/bninit64.c \
                bnlib/legal.c \
                bnlib/sieve.c \
                bnlib/bn16.c \
                bnlib/bn64.c \
                bnlib/bnprint.c \
                bnlib/germain.c \
                bnlib/lbn32.c \
                bnlib/bninit16.c \
                bnlib/lbnmem.c \
                bnlib/prime.c \
                bnlib/bn32.c \
                bnlib/bn.c \
                bnlib/bninit32.c \
                bnlib/jacobi.c              


LOCAL_SRC_FILES +=  zrtp/ZrtpCallbackWrapper.cpp \
                    zrtp/ZRtp.cpp \
                    zrtp/ZrtpCrc32.cpp \
                    zrtp/ZrtpPacketCommit.cpp \
                    zrtp/ZrtpPacketConf2Ack.cpp \
                    zrtp/ZrtpPacketConfirm.cpp \
                    zrtp/ZrtpPacketDHPart.cpp \
                    zrtp/ZrtpPacketGoClear.cpp \
                    zrtp/ZrtpPacketClearAck.cpp \
                    zrtp/ZrtpPacketHelloAck.cpp \
                    zrtp/ZrtpPacketHello.cpp \
                    zrtp/ZrtpPacketError.cpp \
                    zrtp/ZrtpPacketErrorAck.cpp \
                    zrtp/ZrtpPacketPingAck.cpp \
                    zrtp/ZrtpPacketPing.cpp \
                    zrtp/ZrtpPacketSASrelay.cpp \
                    zrtp/ZrtpPacketRelayAck.cpp \
                    zrtp/ZrtpStateClass.cpp \
                    zrtp/ZrtpTextData.cpp \
                    zrtp/ZrtpConfigure.cpp \
                    zrtp/ZrtpCWrapper.cpp \
                    clients/ccrtp/ZrtpQueue.cpp \
                    zrtp/Base32.cpp \
                    zrtp/zrtpB64Encode.c \
                    zrtp/zrtpB64Decode.c \
                    zrtp/ZrtpSdesStream.cpp \
                    zrtp/ZIDCacheDb.cpp \
                    zrtp/ZIDRecordDb.cpp \
                    zrtp/crypto/zrtpDH.cpp \
                    zrtp/crypto/hmac256.cpp \
                    zrtp/crypto/sha256.cpp \
                    zrtp/crypto/hmac384.cpp \
                    zrtp/crypto/sha384.cpp \
                    zrtp/crypto/aesCFB.cpp \
                    zrtp/crypto/twoCFB.cpp \
                    zrtp/crypto/sha2.c \
                    srtp/CryptoContext.cpp \
                    srtp/CryptoContextCtrl.cpp \
                    srtp/SrtpHandler.cpp \
                    srtp/crypto/hmac.cpp \
                    srtp/crypto/SrtpSymCrypto.cpp \
                    srtp/crypto/sha1.c \
                    cryptcommon/twofish.c \
                    cryptcommon/twofish_cfb.c \
                    cryptcommon/aescrypt.c \
                    cryptcommon/aeskey.c \
                    cryptcommon/aestab.c \
                    cryptcommon/aes_modes.c \
                    cryptcommon/macSkein.cpp \
                    cryptcommon/skein.c \
                    cryptcommon/skein_block.c \
                    cryptcommon/skeinApi.c \
                    cryptcommon/ZrtpRandom.cpp \
                    $(EC_SRCS) \
                    $(COMMON_SRCS) \
                    $(BNLIB_SRCS)


include $(BUILD_STATIC_LIBRARY)

LOCAL_PATH = $(ROOT_SRC_PATH)/../..
