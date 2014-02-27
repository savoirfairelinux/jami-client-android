## Copyright (C) 1999-2005 Open Source Telecom Corporation.
## Copyright (C) 2006-2008 David Sugar, Tycho Softworks.
## Copyright (C) 2009-2011 Werner Dittmann
##
## This file is free software; as a special exception the author gives
## unlimited permission to copy and/or distribute it, with or without
## modifications, as long as this notice is preserved.
##
## This program is distributed in the hope that it will be useful, but
## WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
## implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#

cmake_minimum_required(VERSION 2.6)

PROJECT(libccrtp)
set (VERSION 2.0.7)
set (SOVERSION ${VERSION})
STRING(REGEX REPLACE "[.].*$" "" SOVERSION ${SOVERSION})
set (REQUIRES 5.0.0)
set (PACKAGE ccrtp)

# for debug and trace during CMakeList development
set(CMAKE_VERBOSE_MAKEFILE FALSE)

SET(CPACK_PACKAGE_VERSION_MAJOR 2)
SET(CPACK_PACKAGE_VERSION_MINOR 0)
SET(CPACK_PACKAGE_VERSION_PATCH 7)

# set some global variables, in particular version variables

# Set library postfix in case of 64 bit architecture
# this caused problems in debian where it has to always be lib....
set(LIB64 "")
if (NOT EXISTS /etc/debian_version)
    if ( "${CMAKE_SYSTEM_PROCESSOR}" STREQUAL "x86_64" )
        set(LIB64 "64")
    endif()
endif()

include(cmake/Modules/AutoArgs.cmake)
enable_arg(srtp true)

if(MSVC60)
    set(BUILD_STATIC ON CACHE BOOL "static linking only" FORCE)
    MARK_AS_ADVANCED(BUILD_STATIC)
else()
    option(BUILD_STATIC "Set to OFF to build shared libraries" OFF)
endif()

MESSAGE( STATUS "Configuring GNU ${PROJECT_NAME} ${VERSION}...")

# include all the fine stuff we need
include(cmake/Modules/FindGcryptConfig.cmake)
include(FindPkgConfig)
include(CheckLibraryExists)
include(CheckIncludeFiles)

if(${PROJECT_NAME} STREQUAL ${CMAKE_PROJECT_NAME})
    include(cmake/Modules/GeneratePackage.cmake)

    GENERATE_PACKAGING(${PACKAGE} ${VERSION})
endif()

# setup the Thread include and lib
find_package(Threads)
if (CMAKE_HAVE_PTHREAD_H)
    set(HAVE_PTHREAD_H TRUE)
endif()
set (LIBS ${LIBS} ${CMAKE_THREAD_LIBS_INIT})

if (USES_UCOMMON_INCLUDE_DIRS)
    message(STATUS "  Using local commoncpp dependency")
else()
    find_package(PkgConfig)
    pkg_check_modules(USES_UCOMMON commoncpp>=${REQUIRES})
endif()

include_directories(${USES_UCOMMON_INCLUDE_DIRS} ${CMAKE_CURRENT_SOURCE_DIR}/src/ccrtp)
link_directories(${USES_UCOMMON_LIBRARY_DIRS})
add_definitions(${USES_UCOMMON_CFLAGS})
set (LIBS ${LIBS} ${USES_UCOMMON_LDFLAGS} commoncpp ${USES_UCOMMON_LIBRARIES})

if(WIN32 AND NOT CYGWIN AND NOT MSYS AND NOT MINGW)
  check_include_files(openssl/ssl.h HAVE_OPENSSL_WINDOWS)
else()
  pkg_check_modules(OPENSSL libssl>=0.9.8)
endif()

if (enable_srtp)
  # now get info about crypto libraries
  gcr_check(GCRYPT gcrypt)
  if (GCRYPT_FOUND AND NOT OPENSSL_FOUND)
    set(SRTP_SUPPORT TRUE)
    check_include_files(gcrypt.h HAVE_GCRYPT_H)
    set(LIBS ${LIBS} ${GCRYPT_LIBRARIES})
  else ()
    set(GCRYPT_FOUND FALSE)
    if (OPENSSL_FOUND OR HAVE_OPENSSL_WINDOWS)
      set(SRTP_SUPPORT TRUE)
      check_include_files(openssl/bn.h HAVE_OPENSSL_BN_H)
      check_include_files(openssl/aes.h HAVE_OPENSSL_AES_H)
      check_include_files(openssl/sha.h HAVE_OPENSSL_SHA_H)
      check_library_exists(crypto EVP_CipherInit_ex "" HAVE_SSL_CRYPT)
      if(HAVE_OPENSSL_WINDOWS)
        set(LIBS ${LIBS} libeay32)
      else()
        set(LIBS ${LIBS} -lcrypto)
      endif()
    else()
      message(WARNING "No crypto library found")
    endif()
  endif()
endif()

# the following set(...) commands are only to have backward
# compatibility with autoconf stuff to generate the pc file
set(prefix ${CMAKE_INSTALL_PREFIX})
set(exec_prefix ${prefix}/bin)
set(libdir ${prefix}/lib${LIB64})
set(includedir ${prefix}/include)
set(PACKAGE pkgconfig)

configure_file(libccrtp.pc.in ${CMAKE_CURRENT_BINARY_DIR}/libccrtp.pc @ONLY)
configure_file(ccrtp.spec.cmake ${CMAKE_CURRENT_BINARY_DIR}/ccrtp.spec @ONLY)

if(CMAKE_COMPILER_IS_GNUCXX)
  add_definitions(-g -O2 -fno-strict-aliasing)
  add_definitions(-Wno-long-long -Wno-char-subscripts)
  add_definitions(-Wall -ansi -pedantic)
endif()

# now prepare and go for the sub-directories
include_directories(${CMAKE_CURRENT_BINARY_DIR}/src ${CMAKE_CURRENT_SOURCE_DIR}/src ${CMAKE_CURRENT_SOURCE_DIR}/src/ccrtp)

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/ccrtp-config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/ccrtp-config.h)
include_directories("${CMAKE_CURRENT_BINARY_DIR}")

add_subdirectory(src)

add_subdirectory(demo)

add_subdirectory(doc)

########### install files ###############
install(FILES ${CMAKE_CURRENT_BINARY_DIR}/libccrtp.pc DESTINATION lib${LIB64}/pkgconfig)

if(${PROJECT_NAME} STREQUAL ${CMAKE_PROJECT_NAME})

    ########### Add uninstall target ###############
    configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/cmake/cmake_uninstall.cmake.in"
    "${CMAKE_CURRENT_BINARY_DIR}/cmake_uninstall.cmake"
    IMMEDIATE @ONLY)
    add_custom_target(uninstall
    "${CMAKE_COMMAND}" -P "${CMAKE_CURRENT_BINARY_DIR}/cmake_uninstall.cmake")
endif()

####### define target to build RPM
#include(UseRPMTools)
#if(RPMTools_FOUND)
#  RPMTools_ADD_RPM_TARGETS(${PROJECT_NAME} ${CMAKE_SOURCE_DIR}/ccrtp.spec.cmake)
#endif()
