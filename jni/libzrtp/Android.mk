ROOT_SRC_PATH := $(LOCAL_PATH)/libzrtp/sources

OLD_PATH = $(LOCAL_PATH)
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

EC_SRCS =   $(ROOT_SRC_PATH)/bnlib/ec/ec.c \
            $(ROOT_SRC_PATH)/bnlib/ec/ecdh.c

COMMON_SRCS =   $(ROOT_SRC_PATH)/common/osSpecifics.c \
                $(ROOT_SRC_PATH)/common/Thread.cpp \
                $(ROOT_SRC_PATH)/common/MutexClass.cpp \
                $(ROOT_SRC_PATH)/common/EventClass.cpp

BNLIB_SRCS =    $(ROOT_SRC_PATH)/bnlib/bninit64.c \
                $(ROOT_SRC_PATH)/bnlib/legal.c \
                $(ROOT_SRC_PATH)/bnlib/sieve.c \
                $(ROOT_SRC_PATH)/bnlib/bn16.c \
                $(ROOT_SRC_PATH)/bnlib/bn64.c \
                $(ROOT_SRC_PATH)/bnlib/bnprint.c \
                $(ROOT_SRC_PATH)/bnlib/germain.c \
                $(ROOT_SRC_PATH)/bnlib/lbn32.c \
                $(ROOT_SRC_PATH)/bnlib/bninit16.c \
                $(ROOT_SRC_PATH)/bnlib/lbnmem.c \
                $(ROOT_SRC_PATH)/bnlib/prime.c \
                $(ROOT_SRC_PATH)/bnlib/bn32.c \
                $(ROOT_SRC_PATH)/bnlib/bn.c \
                $(ROOT_SRC_PATH)/bnlib/bninit32.c \
                $(ROOT_SRC_PATH)/bnlib/jacobi.c              


LOCAL_SRC_FILES +=  $(ROOT_SRC_PATH)/zrtp/ZrtpCallbackWrapper.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZRtp.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpCrc32.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketCommit.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketConf2Ack.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketConfirm.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketDHPart.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketGoClear.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketClearAck.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketHelloAck.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketHello.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketError.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketErrorAck.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketPingAck.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketPing.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketSASrelay.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpPacketRelayAck.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpStateClass.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpTextData.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpConfigure.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpCWrapper.cpp \
                    $(ROOT_SRC_PATH)/clients/ccrtp/ZrtpQueue.cpp \
                    $(ROOT_SRC_PATH)/zrtp/Base32.cpp \
                    $(ROOT_SRC_PATH)/zrtp/zrtpB64Encode.c \
                    $(ROOT_SRC_PATH)/zrtp/zrtpB64Decode.c \
                    $(ROOT_SRC_PATH)/zrtp/ZrtpSdesStream.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZIDRecordDb.cpp \
                    $(ROOT_SRC_PATH)/zrtp/crypto/zrtpDH.cpp \
                    $(ROOT_SRC_PATH)/zrtp/crypto/hmac256.cpp \
                    $(ROOT_SRC_PATH)/zrtp/crypto/sha256.cpp \
                    $(ROOT_SRC_PATH)/zrtp/crypto/hmac384.cpp \
                    $(ROOT_SRC_PATH)/zrtp/crypto/sha384.cpp \
                    $(ROOT_SRC_PATH)/zrtp/crypto/aesCFB.cpp \
                    $(ROOT_SRC_PATH)/zrtp/crypto/twoCFB.cpp \
                    $(ROOT_SRC_PATH)/zrtp/crypto/sha2.c \
                    $(ROOT_SRC_PATH)/zrtp/ZIDCacheFile.cpp \
                    $(ROOT_SRC_PATH)/zrtp/ZIDRecordFile.cpp \
                    $(ROOT_SRC_PATH)/srtp/CryptoContext.cpp \
                    $(ROOT_SRC_PATH)/srtp/CryptoContextCtrl.cpp \
                    $(ROOT_SRC_PATH)/srtp/SrtpHandler.cpp \
                    $(ROOT_SRC_PATH)/srtp/crypto/hmac.cpp \
                    $(ROOT_SRC_PATH)/srtp/crypto/SrtpSymCrypto.cpp \
                    $(ROOT_SRC_PATH)/srtp/crypto/sha1.c \
                    $(ROOT_SRC_PATH)/cryptcommon/twofish.c \
                    $(ROOT_SRC_PATH)/cryptcommon/twofish_cfb.c \
                    $(ROOT_SRC_PATH)/cryptcommon/aescrypt.c \
                    $(ROOT_SRC_PATH)/cryptcommon/aeskey.c \
                    $(ROOT_SRC_PATH)/cryptcommon/aestab.c \
                    $(ROOT_SRC_PATH)/cryptcommon/aes_modes.c \
                    $(ROOT_SRC_PATH)/cryptcommon/macSkein.cpp \
                    $(ROOT_SRC_PATH)/cryptcommon/skein.c \
                    $(ROOT_SRC_PATH)/cryptcommon/skein_block.c \
                    $(ROOT_SRC_PATH)/cryptcommon/skeinApi.c \
                    $(ROOT_SRC_PATH)/cryptcommon/ZrtpRandom.cpp \
                    $(EC_SRCS) \
                    $(COMMON_SRCS) \
                    $(BNLIB_SRCS)


include $(BUILD_STATIC_LIBRARY)

