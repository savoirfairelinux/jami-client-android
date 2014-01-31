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
set (PACKAGE libzrtpcpp)
set (VERSION 3.0.0)
set (SOVERSION ${VERSION})
STRING(REGEX REPLACE "[.].*$" "" SOVERSION ${SOVERSION})

SET(CPACK_PACKAGE_VERSION_MAJOR ${SOVERSION})
SET(CPACK_PACKAGE_VERSION_MINOR ${VERSION})
SET(CPACK_PACKAGE_VERSION_PATCH ${VERSION})
STRING(REGEX REPLACE "[.][0-9]*$" "" CPACK_PACKAGE_VERSION_MINOR ${VERSION})
STRING(REGEX REPLACE ".*[.]" "" CPACK_PACKAGE_VERSION_MINOR ${CPACK_PACKAGE_VERSION_MINOR})
STRING(REGEX REPLACE ".*[.]" "" CPACK_PACKAGE_VERSION_PATCH ${VERSION})

include(cmake/Modules/AutoArgs.cmake)
include(cmake/Modules/FindGcryptConfig.cmake)

# Define supported -Denable-* command line parameters.
#
# Example to build the tivi client: cmake -Denable-tivi=true ..
# Without any options cmake generates libzrtpcpp for use with GNU ccRTP
#
enable_arg(ccrtp false "Build library to use with GNU ccRTP.")
enable_arg(crypto_standalone false "Use embedded crypto and big number modules.")
enable_arg(tivi false "Build library for the tivi client, implies '-Denable-crypto_standalone=true'.")
enable_arg(sqlite false "Use SQLite DB as backend for ZRTP cache.")
args_help()

# **** Check what and how to build ****
#
if (enable_ccrtp AND enable_tivi)
    MESSAGE(FATAL_ERROR "Cannot build more than one client at once. Use different build directories.")
endif()

if (enable_ccrtp)
    if (USES_CCRTP_INCLUDE_DIRS)
        message(STATUS "  Using local commoncpp dependency")
    else()
        find_package(PkgConfig)
        pkg_check_modules(USES_CCRTP libccrtp>=2.0.0)
    endif()
    include_directories(${USES_CCRTP_INCLUDE_DIRS})
    link_directories(${USES_CRTP_LIBRARY_DIRS})
    add_definitions(${USES_CCRTP_CFLAGS})
    set (LIBS ${LIBS} ${USES_CCRTP_LDFLAGS} ${USES_CCRTP_LIBRARIES})
    set (PACKAGE libzrtpcpp)
    set(zrtplibName zrtpcpp)
elseif (enable_tivi)
    set (PACKAGE libzrtptivi)
    set(zrtplibName zrtptivi)
    set (enable_crypto_standalone true)
else()
    MESSAGE(WARNING "No client defined, building for GNU ccRTP.")
    set (PACKAGE libzrtpcpp)
    set(enable_ccrtp true)
    set(zrtplibName zrtpcpp)
endif()

if (enable_crypto_standalone)
    set(CRYPTO_STANDALONE TRUE)
endif()

if(CMAKE_GENERATOR MATCHES "Unix Makefiles")
	add_custom_target(cleandist
		WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}[-_]*.gz"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}_*.dsc"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}-*.rpm"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/*${PACKAGE}*[-_]*.deb"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}-*.zip"
    COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/*${PACKAGE}*.changes"
	)

	add_custom_target(dist
		DEPENDS cleandist
		WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
		COMMAND git archive --format tar --output="${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}-${VERSION}.tar" --prefix="${PACKAGE}-${VERSION}/" HEAD
		COMMAND gzip "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}-${VERSION}.tar"
	)
endif()

if(MSVC60)
    set(BUILD_STATIC ON CACHE BOOL "static linking only" FORCE)
    MARK_AS_ADVANCED(BUILD_STATIC)
else()
    option(BUILD_STATIC "Set to OFF to build shared libraries" OFF)
endif()

# set to true for debug and trace during CMakeLists development
# set(CMAKE_VERBOSE_MAKEFILE TRUE)

MESSAGE( STATUS "Configuring GNU ${PROJECT_NAME} ${VERSION} for ${PACKAGE}...")

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

if (NOT enable_crypto_standalone)
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

if (enable_sqlite)
    pkg_check_modules(SQLITE3 sqlite3>=3.7)
    if (SQLITE3_FOUND)
        check_include_files(sqlite3.h HAVE_SQLITE_H)
        set(LIBS ${LIBS} -lsqlite3)
    else()
      message(FATAL_ERROR "SQLite3 library not found")
    endif()
endif()

# now get info about crypto libraries
gcr_check(GCRYPT gcrypt)
#if(GCRYPT_FOUND)
#  check_include_files(gcrypt.h HAVE_GCRYPT_H)
#  set(LIBS ${LIBS} ${GCRYPT_LIBRARIES})
#  set(BUILD_REQ "libgcrypt-devel")
#  set(CRYPTOBACKEND="")
#  set(PACKAGE_REQ "libgcrypt")
#else()
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
#endif()

check_include_files(stdlib.h HAVE_STDLIB_H)
check_include_files(string.h HAVE_STRING_H)

# necessary and required modules checked, ready to generate config.h
configure_file(libzrtpcpp-config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/libzrtpcpp-config.h)
include_directories(${CMAKE_CURRENT_BINARY_DIR})

# the following set(...) commands are only to have backward
# compatibility with autoconf stuff to generate the pc file
set(prefix ${CMAKE_INSTALL_PREFIX})
set(exec_prefix ${prefix}/bin)
set(libdir ${prefix}/${LIBDIRNAME})
set(includedir ${prefix}/include)
set(PACKAGE pkgconfig)
configure_file(libzrtpcpp.pc.cmake ${CMAKE_CURRENT_BINARY_DIR}/libzrtpcpp.pc @ONLY)

configure_file(libzrtpcpp.spec.cmake ${CMAKE_CURRENT_BINARY_DIR}/libzrtpcpp.spec @ONLY)

#to make sure includes are first taken from those directory
include_directories(BEFORE ${CMAKE_CURRENT_BINARY_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/src)

add_definitions(-g -O2 -fno-strict-aliasing)
if(CMAKE_COMPILER_IS_GNUCXX)
  add_definitions(-Wno-long-long -Wno-char-subscripts)
  add_definitions(-Wall -pedantic)
  add_definitions(-DNEW_STDCPP)
endif()

include_directories(BEFORE ${CMAKE_BINARY_DIR})
include_directories (${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/zrtp)
if(enable_standalone)
    include_directories (${CMAKE_CURRENT_SOURCE_DIR}/bnlib)
endif()

# **** The following source files a common for all clients ****
#
if (NOT enable_ccrtp)
    set (sdes_src ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpSdesStream.cpp)
endif()

set(zrtp_src_no_cache
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpCallbackWrapper.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZRtp.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpCrc32.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketCommit.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketConf2Ack.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketConfirm.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketDHPart.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketGoClear.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketClearAck.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketHelloAck.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketHello.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketError.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketErrorAck.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketPingAck.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketPing.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketSASrelay.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpPacketRelayAck.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpStateClass.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpTextData.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpConfigure.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZrtpCWrapper.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/Base32.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/common/osSpecifics.c ${sdes_src})

set(bnlib_src
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/bn00.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/lbn00.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/bn.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/lbnmem.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/sieve.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/prime.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/bnprint.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/jacobi.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/germain.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/ec/ec.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/ec/ecdh.c
    ${CMAKE_CURRENT_SOURCE_DIR}/bnlib/bnprint.c)

set(zrtp_crypto_src
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/crypto/zrtpDH.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/crypto/hmac256.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/crypto/sha256.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/crypto/hmac384.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/crypto/sha384.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/crypto/aesCFB.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/crypto/twoCFB.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/crypto/sha2.c)

if (NOT enable_sqlite)
    set(zrtp_src ${zrtp_src_no_cache} 
        ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZIDCacheFile.cpp 
        ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZIDRecordFile.cpp)
else()
    set(zrtp_src ${zrtp_src_no_cache}
        ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZIDCacheDb.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/ZIDRecordDb.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/zrtpB64Encode.c
        ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/zrtpB64Decode.c
        ${CMAKE_CURRENT_SOURCE_DIR}/zrtp/zrtpCacheSqliteBackend.c)
endif()

if (enable_ccrtp)
    add_subdirectory(clients/ccrtp)
    add_subdirectory(demo)
endif()

if (enable_tivi)
    add_subdirectory(clients/tivi)
endif()

##very usefull for macosx, specially when using gtkosx bundler
if(APPLE)
    if (NOT CMAKE_INSTALL_NAME_DIR)
        set(CMAKE_INSTALL_NAME_DIR "${CMAKE_INSTALL_PREFIX}/lib" CACHE STRING "CMAKE_INSTALL_NAME_DIR set for macosx" )
    endif (NOT CMAKE_INSTALL_NAME_DIR)
endif(APPLE)


