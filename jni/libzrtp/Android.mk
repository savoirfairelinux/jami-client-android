#
# Define and build the zrtpcpp static lib
#
include $(CLEAR_VARS)

MY_LIBZRTPCPP = libzrtp

LOCAL_MODULE := libzrtpcpp
LOCAL_CPP_FEATURES := exceptions
#
# set to false if testing/compiling new modules to catch undefined symbols (if build shared lib without TIVI_ENV)
# LOCAL_ALLOW_UNDEFINED_SYMBOLS := true

# include paths for zrtpcpp modules
LOCAL_C_INCLUDES += $(MY_LIBZRTPCPP) \
					$(MY_LIBZRTPCPP)/srtp \
					$(MY_LIBZRTPCPP)/src \
					/ucommon/inc/ \
					$(APP_PROJECT_PATH)/jni/ucommon/inc \
					$(MY_LIBZRTPCPP)/src/libzrtpcpp \
					$(MY_OPENSSL)/include \

LOCAL_SRC_FILES += \
					$(MY_LIBZRTPCPP)/src/ZrtpCallbackWrapper.cpp \
					$(MY_LIBZRTPCPP)/src/Zrtp.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpCrc32.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketCommit.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketConf2Ack.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketConfirm.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketDHPart.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketGoClear.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketClearAck.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketHelloAck.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketHello.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketError.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketErrorAck.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketPingAck.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketPing.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketSASrelay.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpPacketRelayAck.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpStateClass.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpTextData.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpConfigure.cpp \
					$(MY_LIBZRTPCPP)/src/ZrtpCWrapper.cpp \
					$(MY_LIBZRTPCPP)/src/Base32.cpp \
					$(MY_LIBZRTPCPP)/srtp/CryptoContext.cpp \
					$(MY_LIBZRTPCPP)/srtp/CryptoContextCtrl.cpp \
					$(MY_LIBZRTPCPP)/srtp/crypto/openssl/hmac.cpp \
					$(MY_LIBZRTPCPP)/srtp/crypto/openssl/SrtpSymCrypto.cpp \
					$(MY_LIBZRTPCPP)/srtp/crypto/skein_block.c \
					$(MY_LIBZRTPCPP)/srtp/crypto/macSkein.cpp \
					$(MY_LIBZRTPCPP)/srtp/crypto/skein.c \


include $(BUILD_STATIC_LIBRARY)

