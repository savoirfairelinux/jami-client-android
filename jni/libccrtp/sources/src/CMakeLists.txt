## Copyright (C) 1999-2005 Open Source Telecom Corporation.
## Copyright (C) 2006-2008 David Sugar, Tycho Softworks.
## Copyright (C) 2009-2010 Werner Dittmann
##
## This file is free software; as a special exception the author gives
## unlimited permission to copy and/or distribute it, with or without
## modifications, as long as this notice is preserved.
##
## This program is distributed in the hope that it will be useful, but
## WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
## implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#

########### next target ###############

set(openssl_srcs ccrtp/crypto/openssl/hmac.cpp
         ccrtp/crypto/openssl/SrtpSymCrypto.cpp)

set(gcrypt_srcs ccrtp/crypto/gcrypt/gcrypthmac.cpp
        ccrtp/crypto/gcrypt/gcryptSrtpSymCrypto.cpp
        ccrtp/crypto/gcrypt/InitializeGcrypt.cpp)

set(skein_srcs ccrtp/crypto/macSkein.cpp
        ccrtp/crypto/skein.c
        ccrtp/crypto/skein_block.c
        ccrtp/crypto/skeinApi.c)

set(twofish_srcs ccrtp/crypto/twofish.c)

if (SRTP_SUPPORT)
  if(GCRYPT_FOUND)
    set(crypto_srcs ${gcrypt_srcs})
  else()
    set(crypto_srcs ${openssl_srcs})
  endif()
endif()

set(ccrtp_srcs
    rtppkt.cpp
    rtcppkt.cpp
    source.cpp
    data.cpp
    incqueue.cpp
    outqueue.cpp
    queue.cpp
    control.cpp
    members.cpp
    socket.cpp
    duplex.cpp
    pool.cpp
    CryptoContext.cpp
    CryptoContextCtrl.cpp)

add_subdirectory(ccrtp)

if(BUILD_STATIC AND NOT BUILD_SHARED)
    set(BUILD_LIBRARY_TYPE STATIC)
else()
    set(BUILD_LIBRARY_TYPE SHARED)
endif()

add_library(ccrtp ${BUILD_LIBRARY_TYPE} ${ccrtp_srcs} ${crypto_srcs} ${skein_srcs} ${twofish_srcs})
set_target_properties(ccrtp PROPERTIES VERSION ${VERSION} SOVERSION ${SOVERSION})
target_link_libraries(ccrtp commoncpp ${LIBS})
add_dependencies(ccrtp commoncpp)

########### install files ###############

install(TARGETS ccrtp DESTINATION lib${LIB64})
