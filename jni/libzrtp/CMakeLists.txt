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
set (VERSION 2.3.4)
set (PACKAGE libzrtpcpp)
set (SOVERSION ${VERSION})
STRING(REGEX REPLACE "[.].*$" "" SOVERSION ${SOVERSION})

SET(CPACK_PACKAGE_VERSION_MAJOR ${SOVERSION})
SET(CPACK_PACKAGE_VERSION_MINOR ${VERSION})
SET(CPACK_PACKAGE_VERSION_PATCH ${VERSION})
STRING(REGEX REPLACE "[.][0-9]*$" "" CPACK_PACKAGE_VERSION_MINOR ${VERSION})
STRING(REGEX REPLACE ".*[.]" "" CPACK_PACKAGE_VERSION_MINOR ${CPACK_PACKAGE_VERSION_MINOR})
STRING(REGEX REPLACE ".*[.]" "" CPACK_PACKAGE_VERSION_PATCH ${VERSION})

if(CMAKE_GENERATOR MATCHES "Unix Makefiles")
	add_custom_target(cleandist
		WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}[-_]*.gz"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}_*.dsc"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}-*.rpm"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}[-_]*.deb"
    COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}[-_]*.changes"
		COMMAND rm -f "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}-*.zip"
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
set(CMAKE_VERBOSE_MAKEFILE FALSE)

MESSAGE( STATUS "Configuring GNU ${PROJECT_NAME} ${VERSION}...")

# include most of the fine stuff we need
include(cmake/Modules/FindGcryptConfig.cmake)
include(FindPkgConfig)
include(CheckLibraryExists)
include(CheckIncludeFiles)
include(cmake/Modules/AutoArgs.cmake)

if(${PROJECT_NAME} STREQUAL ${CMAKE_PROJECT_NAME})
    include(cmake/Modules/GeneratePackage.cmake)

    GENERATE_PACKAGING(${PACKAGE} ${VERSION})
endif()

# check the -Denable-ccrtp setting, defaults to true
enable_arg(ccrtp true "Enable GNU ccRTP support for GNU ZRTP")
args_help()

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

# setup the Thread include and lib
find_package(Threads)
if(CMAKE_HAVE_PTHREAD_H)
  set(HAVE_PTHREAD_H TRUE)
endif()
set(LIBS ${LIBS} ${CMAKE_THREAD_LIBS_INIT})

# define the name of the lib. zrtpcppcore does not include the ccRTP stuff.
set(zrtplib zrtpcppcore)
if(enable_ccrtp)
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
    set(zrtplib zrtpcpp)
endif()

# now get info about crypto libraries
pkg_check_modules(OPENSSL libcrypto>=0.9.8)
if (OPENSSL_FOUND)
  set(CMAKE_REQUIRED_INCLUDES ${CMAKE_REQUIRED_INCLUDES} ${OPENSSL_INCLUDE_DIRS}) #update include files search directory
  check_include_files(openssl/bn.h HAVE_OPENSSL_BN_H)
  check_include_files(openssl/aes.h HAVE_OPENSSL_AES_H)
  check_include_files(openssl/sha.h HAVE_OPENSSL_SHA_H)
  check_include_files(openssl/ec.h HAVE_OPENSSL_EC_H)
  check_library_exists(crypto EVP_CipherInit_ex "${OPENSSL_LIBDIR}" HAVE_SSL_CRYPT) #use search lib directory from pkg-config
  set(LIBS ${LIBS} -lcrypto)
  set(CRYPTOBACKEND "libcrypto >= 0.9.8")
  set(BUILD_REQ "libopenssl-devel >= 0.9.8")
  set(PACKAGE_REQ "libopenssl >= 0.9.8")
  include_directories(${OPENSSL_INCLUDE_DIRS}) #update includes directory from pkg-config
endif()

if(NOT HAVE_OPENSSL_EC_H)
  gcr_check(GCRYPT gcrypt)
  if(GCRYPT_FOUND)
    check_include_files(gcrypt.h HAVE_GCRYPT_H)
    set(LIBS ${LIBS} ${GCRYPT_LIBRARIES})
    set(BUILD_REQ "libgcrypt-devel")
    set(CRYPTOBACKEND="")
    set(PACKAGE_REQ "libgcrypt")
  endif()
endif()

if(NOT OPENSSL_FOUND AND NOT GCRYPT_FOUND)
  message(FATAL_ERROR "No crypto library found")
endif()

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
configure_file(libzrtpcpp.pc.cmake ${CMAKE_CURRENT_BINARY_DIR}/lib${zrtplib}.pc @ONLY)

configure_file(libzrtpcpp.spec.cmake ${CMAKE_CURRENT_BINARY_DIR}/libzrtpcpp.spec @ONLY)

#to make sure includes are first taken from those directory
include_directories(BEFORE ${CMAKE_CURRENT_BINARY_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/src)

add_definitions(-g -O2 -fno-strict-aliasing)
if(CMAKE_COMPILER_IS_GNUCXX)
  add_definitions(-Wno-long-long -Wno-char-subscripts)
  add_definitions(-Wall -pedantic)
  add_definitions(-DNEW_STDCPP)
endif()

add_subdirectory(src)

if (enable_ccrtp)
    add_subdirectory(demo)
endif()

if (NOT EXISTS ${CMAKE_CURRENT_BINARY_DIR}/package/)
    MESSAGE(STATUS "package dir not found")
    file(MAKE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/package/)
endif()

########### install files ###############
install(FILES ${CMAKE_CURRENT_BINARY_DIR}/lib${zrtplib}.pc DESTINATION ${LIBDIRNAME}/pkgconfig)

if(${PROJECT_NAME} STREQUAL ${CMAKE_PROJECT_NAME})

    ########### Add uninstall target ###############
    configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/cmake/cmake_uninstall.cmake.in"
    "${CMAKE_CURRENT_BINARY_DIR}/cmake_uninstall.cmake"
    IMMEDIATE @ONLY)
    add_custom_target(uninstall
    "${CMAKE_COMMAND}" -P "${CMAKE_CURRENT_BINARY_DIR}/cmake_uninstall.cmake")

endif()
##very usefull for macosx, specially when using gtkosx bundler
if(APPLE)
    if (NOT CMAKE_INSTALL_NAME_DIR)
        set(CMAKE_INSTALL_NAME_DIR "${CMAKE_INSTALL_PREFIX}/lib" CACHE STRING "CMAKE_INSTALL_NAME_DIR set for macosx" )
    endif (NOT CMAKE_INSTALL_NAME_DIR)
endif(APPLE)


