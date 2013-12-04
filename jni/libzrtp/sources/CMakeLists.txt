# Copyright (C) 2009 Werner Dittman
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
cmake_minimum_required(VERSION 2.6)

PROJECT(libzrtpcpp)

SET(CPACK_PACKAGE_VERSION_MAJOR 4)
SET(CPACK_PACKAGE_VERSION_MINOR 0)
SET(CPACK_PACKAGE_VERSION_PATCH 0)

set (VERSION 4.0.0)
set (SOVERSION 4)

# Define supported command line parameters.
#
# Example to build the tivi client: cmake -DTIVI=true ..
# Without any options cmake generates libzrtpcpp for use with GNU ccRTP
#
option(CCRTP "Build library to use with GNU ccRTP." OFF)
option(CORE_LIB "Build core library only, no spcific client support." OFF)
option(CRYPTO_STANDALONE "Use embedded crypto and big number modules." ON)
option(TIVI "Build library for the tivi client, implies '-DCRYPTO_STNDALONE=true'." OFF)
option(SQLITE "Use SQLite DB as backend for ZRTP cache." OFF)

option(ANDROID "Generate Android makefiles (Android.mk)" ON)
option(JAVA "Generate Java support files (requires JDK and SWIG)" OFF)


# **** Check what and how to build ****
#
if (CCRTP AND TIVI)
    MESSAGE(FATAL_ERROR "Cannot build more than one client at once. Use different build directories.")
endif()

if (CCRTP)
    set (PACKAGE libzrtpcpp)
    set(zrtplibName zrtpcpp)
elseif (TIVI)
    set (PACKAGE libzrtptivi)
    set(zrtplibName zrtptivi)
    set(CRYPTO_STANDALONE true)
    set(SQLITE true)
elseif (CORE_LIB)
    set (PACKAGE libzrtpcore)
    set(zrtplibName zrtpcppcore)
else()
    MESSAGE(WARNING "No client defined, building for GNU ccRTP.")
    set (PACKAGE libzrtpcpp)
    set(CCRTP true)
    set(zrtplibName zrtpcpp)
endif()


if(MSVC60)
    set(BUILD_STATIC ON CACHE BOOL "static linking only" FORCE)
    MARK_AS_ADVANCED(BUILD_STATIC)
else()
    option(BUILD_STATIC "Set to OFF to build shared libraries" OFF)
endif()

# set to true for debug and trace during CMakeLists development
# set(CMAKE_VERBOSE_MAKEFILE TRUE)

execute_process(COMMAND git rev-parse --short HEAD OUTPUT_VARIABLE GIT_COMMIT)
STRING(REGEX REPLACE "(\r?\n)+$" "" GIT_COMMIT "${GIT_COMMIT}")

MESSAGE( STATUS "Configuring GNU ${PROJECT_NAME} ${VERSION} for ${PACKAGE}, commit: ${GIT_COMMIT} ...")

# include most of the fine stuff we need
include(FindPkgConfig)
include(CheckLibraryExists)
include(CheckIncludeFiles)
include(CheckFunctionExists)

if (NOT LIB_SUFFIX)
    set(LIBDIRNAME "lib")
    # this caused problems in debian where it has to always be lib....
    if (NOT EXISTS /etc/debian_version)
        if ( "${CMAKE_SYSTEM_PROCESSOR}" STREQUAL "x86_64" )
            set(LIBDIRNAME "lib64")
        endif()
    endif()
else()
    set(LIBDIRNAME "lib${LIB_SUFFIX}")
endif()

check_include_files(stdlib.h HAVE_STDLIB_H)
check_include_files(string.h HAVE_STRING_H)

if (NOT CRYPTO_STANDALONE)
    pkg_check_modules(OPENSSL libcrypto>=0.9.8)
    if (OPENSSL_FOUND)
      set(CMAKE_REQUIRED_INCLUDES ${CMAKE_REQUIRED_INCLUDES} ${OPENSSL_INCLUDE_DIRS}) #update include files search directory
      check_include_files(openssl/bn.h HAVE_OPENSSL_BN_H)
      check_include_files(openssl/aes.h HAVE_OPENSSL_AES_H)
      check_include_files(openssl/sha.h HAVE_OPENSSL_SHA_H)
      check_library_exists(crypto EVP_CipherInit_ex "${OPENSSL_LIBDIR}" HAVE_SSL_CRYPT) #use search lib directory from pkg-config
      set(LIBS ${LIBS} -lcrypto)
      set(CRYPTOBACKEND "libcrypto >= 0.9.8")
      set(BUILD_REQ "libopenssl-devel >= 0.9.8")
      set(PACKAGE_REQ "libopenssl >= 0.9.8")
      include_directories(${OPENSSL_INCLUDE_DIRS}) #update includes directory from pkg-config
  else()
      message(FATAL_ERROR "No crypto library found")
  endif()
else()
    # For crypto standalone mode we need to configure the bnlib. In a first step
    # without the tests and demos.
    check_include_files(stdint.h HAVE_STDINT_H)
    check_include_files(stdint.h HAVE_ASSERT_H)
    check_include_files(limits.h HAVE_LIMITS_H)

    check_function_exists(memmove HAVE_MEMMOVE)
    check_function_exists(memcpy HAVE_MEMCPY)

    # TODO: check if we compile the tests for bnlib
    #
    # check_function_exists(clock_gettime HAVE_CLOCK_GETTIME)
    # check_function_exists(getrusage HAVE_GETRUSAGE)
    # check_function_exists(clock HAVE_CLOCK)
    # check_function_exists(time HAVE_TIME)

    # Check if Solaris-style gethrvtime() is available
    # check_function_exists(gethrvtime HAVE_GETHRVTIME)
    #
    # until here

    # necessary and required modules checked, ready to generate config.h
    configure_file(${CMAKE_SOURCE_DIR}/bnlib/bnconfig.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/bnconfig.h)
endif()

if (SQLITE)
    pkg_check_modules(SQLITE3 sqlite3>=3.7)
    if (SQLITE3_FOUND)
        check_include_files(sqlite3.h HAVE_SQLITE_H)
        set(LIBS ${LIBS} -lsqlite3)
    else()
      message(FATAL_ERROR "SQLite3 library not found")
    endif()
endif()

# necessary and required modules checked, ready to generate config.h in top-level build directory
configure_file(config.h.cmake ${CMAKE_BINARY_DIR}/config.h)

add_definitions(-g -O2 -fno-strict-aliasing)
if(CMAKE_COMPILER_IS_GNUCXX)
#    add_definitions(-Wno-long-long -Wno-char-subscripts)
#    add_definitions(-Wall -ansi -pedantic)
#    add_definitions(-Wall -pedantic)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -pedantic -std=c99")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -pedantic -std=c++11")
    add_definitions(-DNEW_STDCPP)
endif()

include_directories(BEFORE ${CMAKE_BINARY_DIR})
include_directories (${CMAKE_SOURCE_DIR} ${CMAKE_SOURCE_DIR}/zrtp)

if(CRYPTO_STANDALONE)
    add_definitions(-DSUPPORT_NON_NIST)
    include_directories (${CMAKE_SOURCE_DIR}/bnlib)
endif()

if (NOT CCRTP)
    set (sdes_src ${CMAKE_SOURCE_DIR}/zrtp/ZrtpSdesStream.cpp)
endif()

# **** The following source files a common for all clients ****
#
set(zrtp_src_no_cache
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpCallbackWrapper.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZRtp.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpCrc32.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketCommit.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketConf2Ack.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketConfirm.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketDHPart.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketGoClear.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketClearAck.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketHelloAck.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketHello.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketError.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketErrorAck.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketPingAck.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketPing.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketSASrelay.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpPacketRelayAck.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpStateClass.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpTextData.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpConfigure.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/ZrtpCWrapper.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/Base32.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/zrtpB64Encode.c
    ${CMAKE_SOURCE_DIR}/zrtp/zrtpB64Decode.c
    ${CMAKE_SOURCE_DIR}/common/osSpecifics.c ${sdes_src})

set(bnlib_src
    ${CMAKE_SOURCE_DIR}/bnlib/bn00.c
    ${CMAKE_SOURCE_DIR}/bnlib/lbn00.c
    ${CMAKE_SOURCE_DIR}/bnlib/bn.c
    ${CMAKE_SOURCE_DIR}/bnlib/lbnmem.c
    ${CMAKE_SOURCE_DIR}/bnlib/sieve.c
    ${CMAKE_SOURCE_DIR}/bnlib/prime.c
    ${CMAKE_SOURCE_DIR}/bnlib/bnprint.c
    ${CMAKE_SOURCE_DIR}/bnlib/jacobi.c
    ${CMAKE_SOURCE_DIR}/bnlib/germain.c
    ${CMAKE_SOURCE_DIR}/bnlib/ec/ec.c
    ${CMAKE_SOURCE_DIR}/bnlib/ec/ecdh.c
    ${CMAKE_SOURCE_DIR}/bnlib/ec/curve25519-donna.c)

set(zrtp_skein_src
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/skeinMac256.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/skein256.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/skeinMac384.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/skein384.cpp)

set(zrtp_crypto_src
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/zrtpDH.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/hmac256.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/sha256.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/hmac384.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/sha384.cpp

    ${CMAKE_SOURCE_DIR}/zrtp/crypto/aesCFB.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/twoCFB.cpp
    ${CMAKE_SOURCE_DIR}/zrtp/crypto/sha2.c)

if (NOT SQLITE)
    set(zrtp_src ${zrtp_src_no_cache}
        ${CMAKE_SOURCE_DIR}/zrtp/ZIDCacheFile.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/ZIDRecordFile.cpp)
else()
    set(zrtp_src ${zrtp_src_no_cache}
        ${CMAKE_SOURCE_DIR}/zrtp/ZIDCacheDb.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/ZIDRecordDb.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/zrtpCacheSqliteBackend.c)

endif()

if (CCRTP)
    add_subdirectory(clients/ccrtp)
    add_subdirectory(demo)
endif()

if (TIVI)
    add_subdirectory(clients/tivi)
endif()

if (CORE_LIB)
    add_subdirectory(clients/no_client)
endif()

##very usefull for macosx, specially when using gtkosx bundler
if(APPLE)
    if (NOT CMAKE_INSTALL_NAME_DIR)
        set(CMAKE_INSTALL_NAME_DIR "${CMAKE_INSTALL_PREFIX}/lib" CACHE STRING "CMAKE_INSTALL_NAME_DIR set for macosx" )
    endif (NOT CMAKE_INSTALL_NAME_DIR)
endif(APPLE)
