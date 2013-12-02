cmake_minimum_required (VERSION 2.6)

# add_subdirectory(libzrtpcpp)
# add_subdirectory(libzrtpcpp/crypto)

include_directories (${CMAKE_CURRENT_SOURCE_DIR})

set(gcrypt_src
    libzrtpcpp/crypto/gcrypt/gcryptZrtpDH.cpp
    libzrtpcpp/crypto/gcrypt/gcrypthmac256.cpp
    libzrtpcpp/crypto/gcrypt/gcryptsha256.cpp
    libzrtpcpp/crypto/gcrypt/gcrypthmac384.cpp
    libzrtpcpp/crypto/gcrypt/gcryptsha384.cpp
    libzrtpcpp/crypto/gcrypt/gcryptAesCFB.cpp
    libzrtpcpp/crypto/gcrypt/InitializeGcrypt.cpp)

set(openssl_src
    libzrtpcpp/crypto/openssl/ZrtpDH.cpp
    libzrtpcpp/crypto/openssl/hmac256.cpp
    libzrtpcpp/crypto/openssl/sha256.cpp
    libzrtpcpp/crypto/openssl/hmac384.cpp
    libzrtpcpp/crypto/openssl/sha384.cpp
    libzrtpcpp/crypto/openssl/AesCFB.cpp
    libzrtpcpp/crypto/openssl/InitializeOpenSSL.cpp)

if (GCRYPT_FOUND)
    set(crypto_src ${gcrypt_src})
endif()

if (OPENSSL_FOUND AND HAVE_OPENSSL_EC_H)
    set(crypto_src ${openssl_src})
endif()

if(enable_ccrtp)
  set(ccrtp_src ZrtpQueue.cpp)
endif()

set(twofish_srcs libzrtpcpp/crypto/twofish.c
         libzrtpcpp/crypto/twofish_cfb.c
         libzrtpcpp/crypto/TwoCFB.cpp)

set(zrtp_src
    ZrtpCallbackWrapper.cpp
    ZIDFile.cpp
    ZIDRecord.cpp
    Zrtp.cpp
    ZrtpCrc32.cpp
    ZrtpPacketCommit.cpp
    ZrtpPacketConf2Ack.cpp
    ZrtpPacketConfirm.cpp
    ZrtpPacketDHPart.cpp
    ZrtpPacketGoClear.cpp
    ZrtpPacketClearAck.cpp
    ZrtpPacketHelloAck.cpp
    ZrtpPacketHello.cpp
    ZrtpPacketError.cpp
    ZrtpPacketErrorAck.cpp
    ZrtpPacketPingAck.cpp
    ZrtpPacketPing.cpp
    ZrtpPacketSASrelay.cpp
    ZrtpPacketRelayAck.cpp
    ZrtpStateClass.cpp
    ZrtpTextData.cpp
    ZrtpConfigure.cpp
    ZrtpCWrapper.cpp
    Base32.cpp)

set(zrtpcpp_src ${zrtp_src} ${ccrtp_src} ${crypto_src} ${twofish_srcs})

if(BUILD_STATIC AND NOT BUILD_SHARED)
    set(LIBRARY_BUILD_TYPE STATIC)
else()
    set(LIBRARY_BUILD_TYPE SHARED)
endif()

add_library(${zrtplib} ${LIBRARY_BUILD_TYPE} ${zrtpcpp_src})
set_target_properties(${zrtplib} PROPERTIES VERSION ${VERSION} SOVERSION ${SOVERSION})
target_link_libraries(${zrtplib} ${LIBS})

if(enable_ccrtp)
    add_dependencies(${zrtplib} ccrtp)
endif()

add_subdirectory(libzrtpcpp)

install(TARGETS ${zrtplib} DESTINATION ${LIBDIRNAME})

