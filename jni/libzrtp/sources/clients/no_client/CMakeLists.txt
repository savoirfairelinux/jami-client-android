cmake_minimum_required (VERSION 2.6)

# setup the Thread include and lib
find_package(Threads)
if(CMAKE_HAVE_PTHREAD_H)
  set(HAVE_PTHREAD_H TRUE)
endif()
set(LIBS ${LIBS} ${CMAKE_THREAD_LIBS_INIT})


#to make sure includes are first taken - it contains config.h
include_directories(BEFORE ${CMAKE_BINARY_DIR})
include_directories (${CMAKE_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_SOURCE_DIR}/zrtp
    ${CMAKE_SOURCE_DIR}/srtp ${CMAKE_SOURCE_DIR}/bnlib)

# **** setup the various crypto interface implementations ***
# Twofish is a special case: its always a standalone modlue and thus
# not specific to a library. Same is true for Skein hash.
# NOTE: the standalone modules live in the 'crypto'

set(cryptcommon_srcs
    ${CMAKE_SOURCE_DIR}/cryptcommon/macSkein.cpp
    ${CMAKE_SOURCE_DIR}/cryptcommon/skein.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/skein_block.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/skeinApi.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/twofish.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/twofish_cfb.c ${zrtp_skein_src})

if (OPENSSL_FOUND)
    set(crypto_src
        ${CMAKE_SOURCE_DIR}/zrtp/crypto/openssl/zrtpDH.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/crypto/openssl/hmac256.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/crypto/openssl/sha256.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/crypto/openssl/hmac384.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/crypto/openssl/sha384.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/crypto/openssl/aesCFB.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/crypto/openssl/InitializeOpenSSL.cpp
        ${CMAKE_SOURCE_DIR}/zrtp/crypto/twoCFB.cpp)

endif()

if (CRYPTO_STANDALONE)
    set(crypto_src
        ${CMAKE_SOURCE_DIR}/cryptcommon/ZrtpRandom.cpp
        ${CMAKE_SOURCE_DIR}/common/Thread.cpp
        ${CMAKE_SOURCE_DIR}/common/MutexClass.cpp
        ${CMAKE_SOURCE_DIR}/common/EventClass.cpp
        ${zrtp_crypto_src} ${bnlib_src})

    set(cryptcommon_srcs ${cryptcommon_srcs}
        ${CMAKE_SOURCE_DIR}/cryptcommon/aescrypt.c
        ${CMAKE_SOURCE_DIR}/cryptcommon/aeskey.c
        ${CMAKE_SOURCE_DIR}/cryptcommon/aestab.c
        ${CMAKE_SOURCE_DIR}/cryptcommon/aes_modes.c)
endif()

set(zrtpcpp_src ${zrtp_src} ${crypto_src} ${cryptcommon_srcs})

if(BUILD_STATIC AND NOT BUILD_SHARED)
    set(LIBRARY_BUILD_TYPE STATIC)
else()
    set(LIBRARY_BUILD_TYPE SHARED)
endif()

add_library(${zrtplibName} ${LIBRARY_BUILD_TYPE} ${zrtpcpp_src})
set_target_properties(${zrtplibName} PROPERTIES VERSION ${VERSION} SOVERSION ${SOVERSION})
target_link_libraries(${zrtplibName} ${LIBS})

# **** Setup packing environment ****
#
if(${PROJECT_NAME} STREQUAL ${CMAKE_PROJECT_NAME})
    include(${CMAKE_SOURCE_DIR}/cmake/Modules/GeneratePackage.cmake)

    GENERATE_PACKAGING(${PACKAGE} ${VERSION})
endif()

# **** Create the external files for RPM and pkgconfig ****
#
set(prefix ${CMAKE_INSTALL_PREFIX})
set(exec_prefix ${prefix}/bin)
set(libdir ${prefix}/${LIBDIRNAME})
set(includedir ${prefix}/include)
set(PACKAGE pkgconfig)

configure_file(${CMAKE_SOURCE_DIR}/libzrtpcpp.pc.cmake ${CMAKE_CURRENT_BINARY_DIR}/lib${zrtplibName}.pc @ONLY)
configure_file(${CMAKE_SOURCE_DIR}/libzrtpcpp.spec.cmake ${CMAKE_CURRENT_BINARY_DIR}/lib${zrtplibName}.spec @ONLY)

# **** install files ****
#
install(FILES
        ${CMAKE_SOURCE_DIR}/zrtp/libzrtpcpp/ZrtpCodes.h
        ${CMAKE_SOURCE_DIR}/zrtp/libzrtpcpp/ZrtpConfigure.h
        ${CMAKE_SOURCE_DIR}/zrtp/libzrtpcpp/ZrtpCallback.h
        ${CMAKE_SOURCE_DIR}/zrtp/libzrtpcpp/ZrtpCWrapper.h
        ${CMAKE_SOURCE_DIR}/zrtp/libzrtpcpp/ZrtpUserCallback.h DESTINATION include/libzrtpcpp)

install(FILES ${CMAKE_SOURCE_DIR}/common/osSpecifics.h DESTINATION include/libzrtpcpp/common)

install(FILES ${CMAKE_CURRENT_BINARY_DIR}/lib${zrtplibName}.pc DESTINATION ${LIBDIRNAME}/pkgconfig)

install(TARGETS ${zrtplibName} DESTINATION ${LIBDIRNAME})

if(${PROJECT_NAME} STREQUAL ${CMAKE_PROJECT_NAME})

    ########### Add uninstall target ###############
    configure_file("${CMAKE_SOURCE_DIR}/cmake/cmake_uninstall.cmake.in" "${CMAKE_CURRENT_BINARY_DIR}/cmake_uninstall.cmake" IMMEDIATE @ONLY)
    add_custom_target(uninstall "${CMAKE_COMMAND}" -P "${CMAKE_CURRENT_BINARY_DIR}/cmake_uninstall.cmake")

endif()


