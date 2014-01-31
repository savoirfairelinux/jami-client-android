# Copyright (C) 2009 David Sugar, Tycho Softworks
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
# This is a simplified build system for GNU uCommon.  In particular, it
# offers limited cmodel linkage support, at least for mingw32 support (and
# probably visual c 6 also).  If one really needs to do such things outside
# these use cases, it is suggested to continue using the existing autotools
# configure script driven build environment instead.  Mostly the cmake one is
# meant for generating project files for those working with IDE's to do
# otherwise generic builds of the library and supporting applications.

cmake_minimum_required(VERSION 2.6)
PROJECT(ucommon)
set (VERSION 6.0.7)

set(RC_VERSION ${VERSION})
STRING(REGEX REPLACE "[.]" "," RC_VERSION ${RC_VERSION})
set(RC_VERSION "${RC_VERSION},0")

set(SOVERSION ${VERSION})
STRING(REGEX REPLACE "[.].*$" "" SOVERSION ${SOVERSION})

# when we override default install prefix, assume full path is used...

set(INSTALL_INCLUDEDIR include)
set(INSTALL_BINDIR bin)

if(WIN32)
    set(INSTALL_MANDIR man)
    set(INSTALL_DOCDIR doc/${PROJECT_NAME})
    set(INSTALL_LOCALE locale)
    set(INSTALL_LIBDIR bin)
    set(INSTALL_SBINDIR bin)
    set(INSTALL_INFODIR info)
    set(SYSCONFDIR "etc" CACHE PATH "config directory")
    set(STATEDIR "var" CACHE PATH "state directory")
else()
    if (NOT LIB_SUFFIX)
        if (NOT EXISTS /etc/debian_version)
            if (${CMAKE_SYSTEM_PROCESSOR} STREQUAL "x86_64")
                set(LIB_SUFFIX 64)
            endif()
        endif()
    endif()
    set(INSTALL_SBINDIR sbin)
    set(INSTALL_MANDIR share/man)
    set(INSTALL_LOCALE share/locale)
    set(INSTALL_DOCDIR share/doc/${PROJECT_NAME})
    set(INSTALL_INFODIR share/info)
    set(INSTALL_LIBDIR lib${LIB_SUFFIX})
    set(SYSCONFDIR "/etc" CACHE PATH "config directory")
    set(STATEDIR "/var" CACHE PATH "state directory")
endif()

set(UCOMMON_LOCALE ${CMAKE_INSTALL_PREFIX}/${INSTALL_LOCALE})
set(UCOMMON_PREFIX ${CMAKE_INSTALL_PREFIX})

set(UCOMMON_CFGPATH ${SYSCONFDIR})
set(UCOMMON_VARPATH ${STATEDIR})

# project options

if(MSVC60)
    set(BUILD_STATIC ON CACHE BOOL "static linking only" FORCE)
    set(BUILD_STDLIB OFF CACHE BOOL "disable C++ stdlib" FORCE)
    set(POSIX_TIMERS OFF CACHE BOOL "does not use posix timers" FORCE)
    set(GCC_ATOMICS OFF CACHE BOOL "does not use gcc atomics" FORCE)
    MARK_AS_ADVANCED(FORCE BUILD_STDLIB BUILD_STATIC POSIX_TIMERS GCC_ATOMICS)
elseif(WIN32)
        option(BUILD_RUNTIME "Set to OFF to build static runtime" ON)
        if(BUILD_RUNTIME)
            set(BUILD_RUNTIME_TYPE SHARED)
        endif()
    option(BUILD_STATIC "Set to OFF to build shared libraries" ON)
    option(BUILD_STDLIB "Set to OFF to disable C++ stdlib" ON)
    set(POSIX_TIMERS OFF CACHE BOOL "does not use posix timers" FORCE)
    set(GCC_ATOMICS OFF CACHE BOOL "does not use gcc atomics" FORCE)
else()
    option(BUILD_STATIC "Set to ON to build static libraries" OFF)
    option(BUILD_STDLIB "Set to OFF to disable C++ stdlib" ON)
    option(POSIX_TIMERS "Set to ON to enable" OFF)
    option(GCC_ATOMICS "Set to ON to enable" OFF)
endif()

MARK_AS_ADVANCED(POSIX_TIMERS GCC_ATOMICS)

option(BUILD_TESTING "Set to ON to build test programs" OFF)

MESSAGE( STATUS "Configuring GNU ${PROJECT_NAME} ${VERSION}...")
include(CheckFunctionExists)
include(CheckLibraryExists)
include(CheckIncludeFiles)
include(CTest)

SET(CPACK_PACKAGE_DESCRIPTION_SUMMARY "${PROJECT_NAME} library")
SET(CPACK_PACKAGE_VENDOR              "David Sugar")
SET(CPACK_PACKAGE_DESCRIPTION_FILE    "${CMAKE_CURRENT_SOURCE_DIR}/README")
SET(CPACK_RESOURCE_FILE_LICENSE       "${CMAKE_CURRENT_SOURCE_DIR}/COPYING")
set(PACKAGE_FILE_NAME                 ${PROJECT_NAME})
set(PACKAGE_FILE_VERSION              ${VERSION})

IF (WIN32)
    SET(CPACK_PACKAGE_FILE_NAME "${PACKAGE_FILE_NAME}-${PACKAGE_FILE_VERSION}")
ELSE (WIN32)
    SET(CPACK_PACKAGE_FILE_NAME "${PACKAGE_FILE_NAME}-bin-${PACKAGE_FILE_VERSION}")
    if (NOT CPACK_GENERATOR)
        SET(CPACK_GENERATOR "TBZ2")
    endif()
ENDIF (WIN32)

IF(WIN32 AND NOT UNIX)
    SET(CPACK_PACKAGE_INSTALL_DIRECTORY "${CPACK_PACKAGE_FILE_NAME}")

    # There is a bug in NSI that does not handle full unix paths properly. Make
    # sure there is at least one set of four (4) backlasshes.
    # SET(CPACK_PACKAGE_ICON "${CMake_SOURCE_DIR}/Utilities/Release\\\\InstallIcon.bmp")
    # SET(CPACK_NSIS_INSTALLED_ICON_NAME "bin\\\\MyExecutable.exe")
    SET(CPACK_NSIS_DISPLAY_NAME "${CPACK_PACKAGE_INSTALL_DIRECTORY} ${PROJECT_NAME}$")
    SET(CPACK_NSIS_HELP_LINK "http:\\\\\\\\www.gnutelephony.org")
    SET(CPACK_NSIS_URL_INFO_ABOUT "http:\\\\\\\\www.gnutelephony.org")
    SET(CPACK_NSIS_CONTACT "dyfet@gnutelephony.org")
    SET(CPACK_NSIS_MODIFY_PATH ON)

ENDIF(WIN32 AND NOT UNIX)

set(CPACK_COMPONENTS_ALL libraries headers)
set(CPACK_COMPONENT_LIBRARIES_DISPLAY_NAME "Libraries")
set(CPACK_COMPONENT_HEADERS_DISPLAY_NAME   "C++ Headers")
set(CPACK_COMPONENT_LIBRARIES_DESCRIPTION  "Dynamic library")
set(CPACK_COMPONENT_HEADERS_DESCRIPTION
    "Header files needed to build applications using ucommon library")

# The following components are regex's to match anywhere (unless anchored)
# in absolute path + filename to find files or directories to be excluded
# from source tarball.
SET (CPACK_SOURCE_IGNORE_FILES
#svn, git files
"\\\\.svn/" "\\\\.cvsignore$" "/\\\\.git/"
# temporary files
"\\\\.swp$"
# backup files
"~$"
# eclipse, kdevelop and othe IDE files
"\\\\.cdtproject$" "\\\\.cproject$" "\\\\.project$" "\\\\.settings/" "\\\\.kdev4/" "\\\\.kdev4$"
# others
"\\\\.#" "/#"
# don't copy build and other dirs used for building
"/build/" "/autom4te\\\\.cache/" "/_build/"
# used before
"/CVS/" "/\\\\.libs/" "/\\\\.deps/" "\\\\.o$" "\\\\.lo$" "\\\\.la$" "\\\\.sh$" "Makefile\\\\.in$")

SET(CPACK_SOURCE_GENERATOR "TBZ2")
SET(CPACK_SOURCE_PACKAGE_FILE_NAME "${PACKAGE_FILE_NAME}-${PACKAGE_FILE_VERSION}")

# This must always be last!
INCLUDE(CPack)

# set to true for debug and trace during CMakeLists development
set(CMAKE_VERBOSE_MAKEFILE FALSE)

# add module path of project if it exists...
if (EXISTS "${CMAKE_SOURCE_DIR}/inc/")
    set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/inc/")
endif()

include (inc/ucommon.cmake)

find_package(Threads)
if (CMAKE_HAVE_PTHREAD_H)
    set(HAVE_PTHREAD_H TRUE)
endif()
set (UCOMMON_LIBS ${UCOMMON_LIBS} ${CMAKE_THREAD_LIBS_INIT} ${WITH_LDFLAGS})

if (MINGW OR MSYS)
    set (UCOMMON_LIBS ${UCOMMON_LIBS} mingwex mingw32)
else()
    if (WIN32 AND CMAKE_COMPILER_IS_GNUCXX)
        set (UCOMMON_LIBS ${UCOMMON_LIBS} mingwex mingw32)
    endif()
endif()

if (WIN32 OR MINGW OR MSYS OR CMAKE_SYSTEM MATCHES "Windows")
    set (UCOMMON_LIBS ${UCOMMON_LIBS} crypt32 advapi32 user32 ws2_32 wsock32 kernel32)
endif()

if(UNIX AND NOT BUILD_STDLIB)
    set(UCOMMON_LIBS ${UCOMMON_LIBS} c)
endif()

if(MSYS OR MINGW)
    set(UCOMMON_LIBS ${UCOMMON_LIBS} msvcrt)
endif()

if(CMAKE_COMPILER_IS_GNUCXX AND NOT BUILD_STDLIB)
    check_library_exists(gcc __modsi3 "" HAVE_GCC_LIB)
    if(HAVE_GCC_LIB)
            set(UCOMMON_LIBS ${UCOMMON_LIBS} gcc)
    endif()
endif()


if(UNIX OR MSYS OR MINGW OR CYGWIN)
    check_library_exists(dl dlopen "" HAVE_DL_LIB)
    if (HAVE_DL_LIB)
        set (UCOMMON_LIBS ${UCOMMON_LIBS} dl)
    else()
        check_library_exists(compat dlopen "" HAVE_COMPAT_LIB)
        if(HAVE_COMPAT_LIB)
            set (UCOMMON_LIBS ${UCOMMON_LIBS} compat)
        endif()
    endif()

    check_library_exists(dld shl_load "" HAVE DLD_LIB)
    if (HAVE_DLD_LIB)
        set (UCOMMON_LIBS ${UCOMMON_LIBS} dld)
    endif()

    check_library_exists(socket socket "" HAVE_SOCKET_LIB)
    if (HAVE_SOCKET_LIB)
        set (UCOMMON_LIBS ${UCOMMON_LIBS} socket)
    endif()

    check_library_exists(posix4 sem_wait "" HAVE_POSIX4_LIB)
    if (HAVE_POSIX4_LIB)
        set(UCOMMON_LIBS ${UCOMMON_LIBS} posix4)
    endif()

    check_library_exists(rt clock_gettime "" HAVE_RT_LIB)
    if (HAVE_RT_LIB)
        set(UCOMMON_LIBS ${UCOMMON_LIBS} rt)
    endif()
endif()

set (CMAKE_REQUIRED_LIBRARIES ${UCOMMON_LIBS})
check_function_exists(getaddrinfo HAVE_GETADDRINFO)
check_function_exists(socketpair HAVE_SOCKETPAIR)
check_function_exists(inet_ntop HAVE_INET_NTOP)
check_function_exists(gethostbyname2 HAVE_GETHOSTBYNAME2)
check_function_exists(strcoll HAVE_STRCOLL)
check_function_exists(stricmp HAVE_STRICMP)
check_function_exists(stristr HAVE_STRISTR)
check_function_exists(sysconf HAVE_SYSCONF)
check_function_exists(posix_memalign HAVE_POSIX_MEMALIGN)
check_function_exists(dlopen HAVE_DLOPEN)
check_function_exists(shl_open HAVE_SHL_OPEN)
check_function_exists(pthread_condattr_setclock HAVE_PTHREAD_CONDATTR_SETCLOCK)
check_function_exists(pthread_setconcurrency HAVE_PTHREAD_SETCONCURRENCY)
check_function_exists(pthread_yield HAVE_PTHREAD_YIELD)
check_function_exists(pthread_yield_np HAVE_PTHREAD_YIELD_NP)
check_function_exists(pthread_delay HAVE_PTHREAD_DELAY)
check_function_exists(pthread_delay_np HAVE_PTHREAD_DELAY_NP)
check_function_exists(pthread_setschedprio HAVE_PTHREAD_SETSCHEDPRIO)
check_function_exists(ftok HAVE_FTOK)
check_function_exists(shm_open HAVE_SHM_OPEN)
check_function_exists(localtime_r HAVE_LOCALTIME_R)
check_function_exists(gmtime_r HAVE_GMTIME_R)
check_function_exists(strerror_r HAVE_STRERROR_R)
check_function_exists(nanosleep HAVE_NANOSLEEP)
check_function_exists(clock_nanosleep HAVE_CLOCK_NANOSLEEP)
check_function_exists(clock_gettime HAVE_CLOCK_GETTIME)
check_function_exists(posix_fadvise HAVE_POSIX_FADVISE)
check_function_exists(ftruncate HAVE_FTRUNCATE)
check_function_exists(pwrite HAVE_PWRITE)
check_function_exists(setpgrp HAVE_SETPGRP)
check_function_exists(setlocale HAVE_SETLOCALE)
check_function_exists(gettext HAVE_GETTEXT)
check_function_exists(execvp HAVE_EXECVP)
check_function_exists(atexit HAVE_ATEXIT)
check_function_exists(lstat HAVE_LSTAT)
check_function_exists(realpath HAVE_REALPATH)
check_function_exists(symlink HAVE_SYMLINK)
check_function_exists(readlink HAVE_READLINK)
check_function_exists(waitpid HAVE_WAITPID)
check_function_exists(wait4 HAVE_WAIT4)

check_include_files(sys/stat.h HAVE_SYS_STAT_H)
check_include_files(strings.h HAVE_STRINGS_H)
check_include_files(stdlib.h HAVE_STDLIB_H)
check_include_files(string.h HAVE_STRING_H)
check_include_files(memory.h HAVE_MEMORY_H)
check_include_files(inttypes.h HAVE_INTTYPES_H)
check_include_files(dlfcn.h HAVE_DLFCN_H)
check_include_files(stdint.h HAVE_STDINT_H)
check_include_files(poll.h HAVE_POLL_H)
check_include_files(sys/mman.h HAVE_SYS_MMAN_H)
check_include_files(sys/shm.h HAVE_SYS_SHM_H)
check_include_files(sys/poll.h HAVE_SYS_POLL_H)
check_include_files(sys/timeb.h HAVE_SYS_TIMEB_H)
check_include_files(sys/types.h HAVE_SYS_TYPES_H)
check_include_files(sys/wait.h HAVE_SYS_WAIT_H)
check_include_files(endian.h HAVE_ENDIAN_H)
check_include_files(sys/filio.h HAVE_SYS_FILIO_H)
check_include_files(dirent.h HAVE_DIRENT_H)
check_include_files(unistd.h HAVE_UNISTD_H)
check_include_files(sys/resource.h HAVE_SYS_RESOURCE_H)
check_include_files(wchar.h HAVE_WCHAR_H)
check_include_files(mach/clock.h HAVE_MACH_CLOCK_H)
check_include_files(mach-o/dyld.h HAVE_MACH_O_DYLD_H)
check_include_files(linux/version.h HAVE_LINUX_VERSION_H)
check_include_files(regex.h HAVE_REGEX_H)
check_include_files(sys/inotify.h HAVE_SYS_INOTIFY_H)
check_include_files(sys/event.h HAVE_SYS_EVENT_H)
check_include_files(syslog.h HAVE_SYSLOG_H)
check_include_files(openssl/ssl.h HAVE_OPENSSL)
check_include_files(openssl/fips.h HAVE_OPENSSL_FIPS_H)
check_include_files(libintl.h HAVE_LIBINTL_H)
check_include_files(netinet/in.h HAVE_NETINET_IN_H)
check_include_files(net/if.h HAVE_NET_IF_H)
check_include_files(fcntl.h HAVE_FCNTL_H)
check_include_files(termios.h HAVE_TERMIOS_H)
check_include_files(termio.h HAVE_TERMIO_H)
check_include_files(sys/param.h HAVE_SYS_PARAM_H)
check_include_files(sys/file.h HAVE_SYS_FILE_H)
check_include_files(sys/lockf.h HAVE_SYS_LOCKF_H)
check_include_files(regex.h HAVE_REGEX_H)

if(HAVE_REGEX_H)
    check_library_exists(regex regfree "" HAVE_REGEX_LIB)
    if(HAVE_REGEX_LIB OR MINGW OR MSYS)
        set(UCOMMON_LIBS ${UCOMMON_LIBS} "regex")
    endif()
endif()

set(UCOMMON_LIBS ${UCOMMON_LIBS} ${UCOMMON_LINKING})

# for some reason, normal library searches always fail on broken windows
if (WIN32 AND NOT UNIX AND NOT MINGW AND NOT MSYS)
    set(HAVE_GETADDRINFO True)
    set(HAVE_INET_NTOP True)
endif()

# we can mklink a build framework to top directory of a project...
# this is done because we may have a project specific optimized build of
# common libraries such as openssl...
if(WIN32 AND EXISTS "${CMAKE_BINARY_DIR}/Frameworks/OpenSSL")
    set(OPENSSL_FOUND TRUE)
    if(EXISTS ${CMAKE_BINARY_DIR}/Frameworks/OpenSSL/inc32)
        set(OPENSSL_INCLUDE_DIR ${CMAKE_BINARY_DIR}/Frameworks/OpenSSL/inc32)
    else()
        set(OPENSSL_INCLUDE_DIR ${CMAKE_BINARY_DIR}/Frameworks/OpenSSL/include)
    endif()
    if(NOT BUILD_STATIC AND EXISTS ${CMAKE_BINARY_DIR}/Frameworks/OpenSSL/out32dll)
        set(OPENSSL_LIBRARIES ${CMAKE_BINARY_DIR}/Frameworks/OpenSSL/out32dll/ssleay32.lib ${CMAKE_BINARY_DIR}/Frameworks/OpenSSL/lib/libeay32.lib)
    else()
        set(OPENSSL_LIBRARIES ${CMAKE_BINARY_DIR}/Frameworks/OpenSSL/out32dll/ssleay32.lib ${CMAKE_BINARY_DIR}/Frameworks/OpenSSL/lib/libeay32.lib)
    endif()
endif()

# alternate we use generic cmake openssl search...
if(NOT HAVE_OPENSSL AND NOT OPENSSL_FOUND)
    FIND_PACKAGE(OpenSSL)
endif()

# common build options can be passed to cmake using WITH_CFLAGS, WITH_LIBS,
# and WITH_INCLUDES.
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/inc ${WITH_INCLUDES})
add_definitions(${UCOMMON_FLAGS} ${WITH_CFLAGS})
link_libraries(${WITH_LIBS})

# by default we build static libs for windows, shared libs for unix.
# we may also set this from a top level cmake or -DWITH_XX_LIBS

if(BUILD_STATIC)
    set(BUILD_LIBRARY_TYPE STATIC)
else()
    set(BUILD_LIBRARY_TYPE SHARED)
endif()

if(NOT BUILD_RUNTIME_TYPE)
    set(BUILD_RUNTIME_TYPE ${BUILD_LIBRARY_TYPE})
endif()

file(GLOB common_src corelib/*.cpp)
file(GLOB ucommon_inc inc/ucommon/*.h)
file(GLOB commoncpp_src commoncpp/*.cpp)
file(GLOB commoncpp_inc inc/commoncpp/*.h)
file(GLOB ucommon_man utils/*.1)
set(ucommon_man commoncpp-config.1 ucommon-config.1 ${ucommon_man})
list(REMOVE_ITEM ucommon_inc inc/ucommon/secure.h)

if(HAVE_OPENSSL)
    set(secure_inc inc/ucommon/secure.h)
    file(GLOB secure_src openssl/*.cpp openssl/*.h)
    list(APPEND secure_src nossl/common.cpp)
    if(MINGW)
        set(SECURE_LIBS ssl crypto gdi32 z)
    elseif(WIN32 AND NOT CYGWIN)
        set(SECURE_LIBS ssleay32 libeay32 gdi32)
    else()
        set(SECURE_LIBS ssl crypto)
    endif()
elseif(OPENSSL_FOUND)
    file(GLOB secure_src openssl/*.cpp openssl/*.h)
    list(APPEND secure_src nossl/common.cpp)
    include_directories(${OPENSSL_INCLUDE_DIR})
    set(SECURE_LIBS ${OPENSSL_LIBRARIES})
else()
    file(GLOB secure_src nossl/*.cpp nossl/*.h)
endif()

if(NOT WIN32)
    foreach(flag ${UCOMMON_FLAGS})
        set(PACKAGE_FLAGS "${PACKAGE_FLAGS} ${flag}")
    endforeach()
    foreach(lib ${UCOMMON_LIBS})
        STRING(REGEX REPLACE "^[-]l" "" slib ${lib})
        set(PACKAGE_LIBS "${PACKAGE_LIBS} -l${slib}")
    endforeach()
    foreach(lib ${SECURE_LIBS})
        STRING(REGEX REPLACE "^[-]l" "" slib ${lib})
        set(ADDITIONAL_LIBS "${ADDITIONAL_LIBS} -l${slib}")
    endforeach()
    configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${PROJECT_NAME}.pc.cmake ${CMAKE_CURRENT_BINARY_DIR}/${PROJECT_NAME}.pc)
    configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${PROJECT_NAME}-config.cmake ${CMAKE_CURRENT_BINARY_DIR}/${PROJECT_NAME}-config)
    configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${PROJECT_NAME}.spec.cmake ${CMAKE_CURRENT_BINARY_DIR}/${PROJECT_NAME}.spec @ONLY)

    if(BUILD_STDLIB)
        configure_file(${CMAKE_CURRENT_SOURCE_DIR}/commoncpp.pc.cmake ${CMAKE_CURRENT_BINARY_DIR}/commoncpp.pc)
        configure_file(${CMAKE_CURRENT_SOURCE_DIR}/commoncpp-config.cmake ${CMAKE_CURRENT_BINARY_DIR}/commoncpp-config)
    endif()
endif(NOT WIN32)

configure_file(ucommon-config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/ucommon-config.h)
include_directories(${CMAKE_CURRENT_BINARY_DIR})

add_library(ucommon ${BUILD_RUNTIME_TYPE} ${common_src} ${ucommon_inc})
set_target_properties(ucommon PROPERTIES VERSION ${VERSION} SOVERSION ${SOVERSION})
target_link_libraries(ucommon ${UCOMMON_LIBS} ${WITH_LIBS})

add_library(usecure ${BUILD_LIBRARY_TYPE} ${secure_src} ${secure_inc})
set_target_properties(usecure PROPERTIES VERSION ${VERSION} SOVERSION ${SOVERSION})
target_link_libraries(usecure ucommon ${WITH_LIBS} ${SECURE_LIBS} ${UCOMMON_LIBS})
add_dependencies(usecure ucommon)

if(BUILD_STDLIB)
    add_library(commoncpp ${BUILD_LIBRARY_TYPE} ${commoncpp_src} ${commoncpp_inc})
    set_target_properties(commoncpp PROPERTIES VERSION ${VERSION} SOVERSION ${SOVERSION})
    target_link_libraries(commoncpp ucommon ${UCOMMON_LIBS} ${WITH_LIBS} ${SECURE_LIBS})
    add_dependencies(commoncpp ucommon)
endif()

add_executable(ucommon-args utils/args.cpp)
add_dependencies(ucommon-args ucommon)
set_target_properties(ucommon-args PROPERTIES OUTPUT_NAME args)
target_link_libraries(ucommon-args ucommon ${UCOMMON_LIBS} ${WITH_LIBS})

add_executable(usecure-car utils/car.cpp)
add_dependencies(usecure-car usecure ucommon)
set_target_properties(usecure-car PROPERTIES OUTPUT_NAME car)
target_link_libraries(usecure-car usecure ucommon ${SECURE_LIBS} ${UCOMMON_LIBS} ${WITH_LIBS})

add_executable(ucommon-pdetach utils/pdetach.cpp)
add_dependencies(ucommon-pdetach ucommon)
set_target_properties(ucommon-pdetach PROPERTIES OUTPUT_NAME pdetach)
target_link_libraries(ucommon-pdetach ucommon ${UCOMMON_LIBS} ${WITH_LIBS})

add_executable(ucommon-sockaddr utils/sockaddr.cpp)
add_dependencies(ucommon-sockaddr ucommon)
set_target_properties(ucommon-sockaddr PROPERTIES OUTPUT_NAME sockaddr)
target_link_libraries(ucommon-sockaddr ucommon ${UCOMMON_LIBS} ${WITH_LIBS})

add_executable(usecure-scrub utils/scrub.cpp)
add_dependencies(usecure-scrub usecure ucommon)
set_target_properties(usecure-scrub PROPERTIES OUTPUT_NAME scrub-files)
target_link_libraries(usecure-scrub usecure ucommon ${SECURE_LIBS} ${UCOMMON_LIBS} ${WITH_LIBS})

add_executable(usecure-mdsum utils/mdsum.cpp)
add_dependencies(usecure-mdsum usecure ucommon)
set_target_properties(usecure-mdsum PROPERTIES OUTPUT_NAME mdsum)
target_link_libraries(usecure-mdsum usecure ucommon ${SECURE_LIBS} ${UCOMMON_LIBS} ${WITH_LIBS})

add_executable(usecure-zerofill utils/zerofill.cpp)
add_dependencies(usecure-zerofill usecure ucommon)
set_target_properties(usecure-zerofill PROPERTIES OUTPUT_NAME zerofill)
target_link_libraries(usecure-zerofill usecure ucommon ${SECURE_LIBS} ${UCOMMON_LIBS} ${WITH_LIBS})

find_package(Doxygen)

if (DOXYGEN_FOUND)
    configure_file(${CMAKE_CURRENT_SOURCE_DIR}/doxy.config.cmake
        ${CMAKE_CURRENT_BINARY_DIR}/doxy.config
        @ONLY )

    seT(DOXY_CONFIG "${CMAKE_CURRENT_BINARY_DIR}/doxy.config")

    #add_custom_target(doc ${DOXYGEN_EXECUTABLE} ${DOXY_CONFIG})
    add_custom_target(doc
                    ${DOXYGEN_EXECUTABLE} ${DOXY_CONFIG}
                    WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
                    COMMENT "Generating API documentation with Doxygen" VERBATIM
                   )

    #
    # TODO an: taken from internet, to be tested
    #
    # create a windows help .chm file using hhc.exe
    # HTMLHelp DLL must be in path!
    # fallback: use hhw.exe interactively
    IF    (WIN32)
        FIND_PACKAGE(HTMLHelp)
        IF   (HTML_HELP_COMPILER)
            SET (TMP "${CMAKE_CURRENT_BINARY_DIR}\\Doc\\html\\index.hhp")
            STRING(REGEX REPLACE "[/]" "\\\\" HHP_FILE ${TMP} )
            # MESSAGE(SEND_ERROR "DBG  HHP_FILE=${HHP_FILE}")
            ADD_CUSTOM_TARGET(winhelp ${HTML_HELP_COMPILER} ${HHP_FILE})
            ADD_DEPENDENCIES (winhelp doc)

            IF (NOT TARGET_DOC_SKIP_INSTALL)
                # install windows help?
                # determine useful name for output file
                # should be project and version unique to allow installing
                # multiple projects into one global directory
                IF   (EXISTS "${PROJECT_BINARY_DIR}/Doc/html/index.chm")
                    IF   (PROJECT_NAME)
                        SET(OUT "${PROJECT_NAME}")
                    ELSE (PROJECT_NAME)
                        SET(OUT "Documentation") # default
                    ENDIF(PROJECT_NAME)
                    IF   (${PROJECT_NAME}_VERSION_MAJOR)
                        SET(OUT "${OUT}-${${PROJECT_NAME}_VERSION_MAJOR}")
                        IF   (${PROJECT_NAME}_VERSION_MINOR)
                            SET(OUT  "${OUT}.${${PROJECT_NAME}_VERSION_MINOR}")
                            IF   (${PROJECT_NAME}_VERSION_PATCH)
                                SET(OUT "${OUT}.${${PROJECT_NAME}_VERSION_PATCH}")
                            ENDIF(${PROJECT_NAME}_VERSION_PATCH)
                        ENDIF(${PROJECT_NAME}_VERSION_MINOR)
                    ENDIF(${PROJECT_NAME}_VERSION_MAJOR)
                    # keep suffix
                    SET(OUT  "${OUT}.chm")

                    #MESSAGE("DBG ${PROJECT_BINARY_DIR}/Doc/html/index.chm \n${OUT}")
                    # create target used by install and package commands
                    INSTALL(FILES "${PROJECT_BINARY_DIR}/Doc/html/index.chm"
                        DESTINATION ${INSTALL_DOCDIR} RENAME "${OUT}")
                ENDIF(EXISTS "${PROJECT_BINARY_DIR}/Doc/html/index.chm")
            ENDIF(NOT TARGET_DOC_SKIP_INSTALL)

        ENDIF(HTML_HELP_COMPILER)
        # MESSAGE(SEND_ERROR "HTML_HELP_COMPILER=${HTML_HELP_COMPILER}")
    ENDIF (WIN32)
ENDIF(DOXYGEN_FOUND)


message (STATUS)
message (STATUS "-------------------------------------------------------------------------------")
message (STATUS "CMAKE_INSTALL_PREFIX " ${CMAKE_INSTALL_PREFIX} )
message (STATUS "LIB_SUFFIX           " ${LIB_SUFFIX})
message (STATUS "BUILD_STATIC         " ${BUILD_STATIC})
message (STATUS "BUILD STDLIB         " ${BUILD_STDLIB})
message (STATUS "BUILD_TESTING        " ${BUILD_TESTING})
message (STATUS "-------------------------------------------------------------------------------")
message (STATUS)
message (STATUS "Change a value with: cmake -D<Variable>=<Value>")
message (STATUS)

if (BUILD_TESTING)
    ENABLE_TESTING()
    add_subdirectory(test)
endif()

if(NOT WIN32)
    install(FILES ${CMAKE_CURRENT_BINARY_DIR}/${PROJECT_NAME}.pc DESTINATION ${INSTALL_LIBDIR}/pkgconfig)
    install(FILES ${CMAKE_CURRENT_BINARY_DIR}/ucommon-config DESTINATION ${INSTALL_BINDIR})
    if(BUILD_STDLIB)
        install(FILES ${CMAKE_CURRENT_BINARY_DIR}/commoncpp-config DESTINATION ${INSTALL_BINDIR})
        install(FILES ${CMAKE_CURRENT_BINARY_DIR}/commoncpp.pc DESTINATION ${INSTALL_LIBDIR}/pkgconfig)
    endif()
endif(NOT WIN32)

install(FILES ${ucommon_inc} ${secure_inc} DESTINATION ${INSTALL_INCLUDEDIR}/ucommon    COMPONENT headers)
install(FILES ${commoncpp_inc} DESTINATION ${INSTALL_INCLUDEDIR}/commoncpp  COMPONENT headers)

if(BUILD_STDLIB)
    install(TARGETS commoncpp DESTINATION ${INSTALL_LIBDIR} COMPONENT libraries)
endif()

install(TARGETS ucommon usecure DESTINATION ${INSTALL_LIBDIR} COMPONENT libraries)
install(TARGETS ucommon-args ucommon-pdetach usecure-car usecure-scrub usecure-mdsum ucommon-sockaddr usecure-zerofill DESTINATION ${INSTALL_BINDIR})
install(FILES ${ucommon_man} DESTINATION ${INSTALL_MANDIR}/man1)

if(CMAKE_GENERATOR MATCHES "Unix Makefiles")
    add_custom_target(uninstall
        WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}"
        COMMAND xargs <install_manifest.txt
    )
endif()
