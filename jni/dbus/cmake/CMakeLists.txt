project(dbus)

set (DBUS_MAJOR_VERSION "1")
set (DBUS_MINOR_VERSION "3")
set (DBUS_MICRO_VERSION "1")
# used by file version info
set (DBUS_PATCH_VERSION "1")
set (DBUS_VERSION ${DBUS_MAJOR_VERSION}.${DBUS_MINOR_VERSION}.${DBUS_MICRO_VERSION})
endif (DBUS_PATCH_VERSION)

set (DBUS_VERSION_STRING "${DBUS_VERSION}")

if (NOT DBUS_BUILD_TIMESTAMP)
    message(STATUS "FIXME set DBUS_BUILD_TIMESTAMP to current date or fix current time stamp generation for having actual build date in version file info")
    set (DBUS_BUILD_TIMESTAMP 20091231)
endif (NOT DBUS_BUILD_TIMESTAMP)

# we need to be up to date
CMAKE_MINIMUM_REQUIRED(VERSION 2.4.4 FATAL_ERROR)
if(COMMAND cmake_policy)
    cmake_policy(SET CMP0003 NEW)
endif(COMMAND cmake_policy)

# where to look first for cmake modules, before ${CMAKE_ROOT}/Modules/ is checked
set(CMAKE_MODULE_PATH "${CMAKE_SOURCE_DIR}/modules")

#enable building of shared library
SET(BUILD_SHARED_LIBS ON)

if (CYGWIN)
   set (WIN32)
endif (CYGWIN)

# search for required packages
if (WIN32)
    # include local header first to avoid using old installed header
    set (CMAKE_INCLUDE_PATH ${CMAKE_INCLUDE_PATH} ${CMAKE_SOURCE_DIR}/..)
	find_package(LibIconv)
    include(Win32Macros)
    addExplorerWrapper(${CMAKE_PROJECT_NAME})
endif (WIN32)
find_package(LibXml2)
find_package(LibExpat)
find_package(X11)

if(NOT WIN32)
	OPTION(DBUS_ENABLE_ABSTRACT_SOCKETS "enable support for abstract sockets" ON)
endif(NOT WIN32)

#AC_ARG_ENABLE(asserts, AS_HELP_STRING([--enable-asserts],[include assertion checks]),enable_asserts=$enableval,enable_asserts=$USE_MAINTAINER_MODE)
OPTION(DBUS_DISABLE_ASSERTS "Disable assertion checking" OFF)

# do config checks
INCLUDE(ConfigureChecks.cmake)

# @TODO: how to remove last dir from ${CMAKE_SOURCE_DIR} ?
SET(DBUS_SOURCE_DIR ${CMAKE_SOURCE_DIR}/..)

# make some more macros available
include (MacroLibrary)

if(VCS)
	set(DBUS_VERBOSE_C_S 1 CACHE TYPE STRING FORCE)
	set(DBUS_VERBOSE_C_S 1)
endif(VCS)

if(WIN32)
	set(CMAKE_DEBUG_POSTFIX "d")
	if(MSVC)
		# controll folders in msvc projects
		include(ProjectSourceGroup)
		if(NOT GROUP_CODE)
			#set(GROUP_CODE split) #cmake default
			set(GROUP_CODE flat)
		endif(NOT GROUP_CODE)
		ADD_DEFINITIONS(-D_CRT_SECURE_NO_DEPRECATE -D_CRT_NONSTDC_NO_DEPRECATE)


		# Use the highest warning level
		if (WALL)
			set(WALL 1 CACHE TYPE STRING FORCE)
			set(CMAKE_CXX_WARNING_LEVEL 4 CACHE TYPE STRING FORCE)

			if(CMAKE_CXX_FLAGS MATCHES "/W[0-4]")
				STRING(REGEX REPLACE "/W[0-4]" "/W4" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
			else(CMAKE_CXX_FLAGS MATCHES "/W[0-4]")
				SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /W4")
			endif(CMAKE_CXX_FLAGS MATCHES "/W[0-4]")

			if(CMAKE_C_FLAGS MATCHES "/W[0-4]")
				STRING(REGEX REPLACE "/W[0-4]" "/W4" CMAKE_C_FLAGS "${CMAKE_C_FLAGS}")
			else(CMAKE_C_FLAGS MATCHES "/W[0-4]")
				SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /W4")
			endif(CMAKE_C_FLAGS MATCHES "/W[0-4]")
		else (WALL)
			set(CMAKE_CXX_WARNING_LEVEL 3 CACHE TYPE STRING FORCE)
		endif (WALL)

		SET(MSVC_W_ERROR   " /we4028 /we4013 /we4133 /we4047 /we4031 /we4002 /we4003 /we4114")
		SET(MSVC_W_DISABLE " /wd4127 /wd4090 /wd4101 /wd4244")

		SET(CMAKE_C_FLAGS_DEBUG   "${CMAKE_C_FLAGS_DEBUG}   /FIconfig.h ${MSVC_W_ERROR} ${MSVC_W_DISABLE}")
		SET(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} /FIconfig.h ${MSVC_W_ERROR} ${MSVC_W_DISABLE}")
	endif(MSVC)
endif(WIN32)

if (UNIX AND NOT DBUS_DISABLE_ASSERTS)
	# required for backtrace
	SET(CMAKE_C_FLAGS_DEBUG   "${CMAKE_C_FLAGS_DEBUG}   -Wl,--export-dynamic")
	SET(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -Wl,--export-dynamic")
	add_definitions(-DDBUS_BUILT_R_DYNAMIC)
endif (UNIX AND NOT DBUS_DISABLE_ASSERTS)

SET(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG}  -D_DEBUG")

#########################################################################
# Windows CE (>= 5.0.0)
#
# WinCE support now relies on the presence of platform files, found in cmake/modules/platform
# Cmake 2.8.0 doesn't include WinCE platform files by default, but working ones can be found
# on CMake's bugtracker :
# http://public.kitware.com/Bug/view.php?id=7919
#
# for cmake 2.8.0 get the following patch only :
# http://public.kitware.com/Bug/file_download.php?file_id=2944&type=bug
#
# after applying the patch, you can enable the WinCE build by specifying :
# cmake [...] -DCMAKE_SYSTEM_NAME=WinCE -DCMAKE_SYSTEM_VERSION=X.XX
# (where X.XX is your actual WinCE version, e.g. 5.02 for Windows Mobile 6)
#
# Note that you should have a proper cross-compilation environment set up prior to running
# cmake, ie. the PATH, INCLUDE and LIB env vars pointing to your CE SDK/toolchain.
#
if(WINCE)

MESSAGE("Building for WinCE (${CMAKE_SYSTEM_VERSION})")

endif(WINCE)
#########################################################################


ENABLE_TESTING()

#########################################################################
# Disallow in-source build
#macro_ensure_out_of_source_build("dbus requires an out of source build. Please create a separate build directory and run 'cmake path_to_dbus [options]' there.")

# ... and warn in case of an earlier in-source build
#set(generatedFileInSourceDir EXISTS ${dbus_SOURCE_DIR}/config.h)
#if(${generatedFileInSourceDir})
#   message(STATUS "config.h exists in your source directory.")
#endif(${generatedFileInSourceDir})
#########################################################################

if (WIN32 OR CYGWIN)
	set (LIBRARY_OUTPUT_PATH  ${CMAKE_BINARY_DIR}/bin)
else (WIN32 OR CYGWIN)
	set (LIBRARY_OUTPUT_PATH  ${CMAKE_BINARY_DIR}/lib)
endif (WIN32 OR CYGWIN)

set (EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR}/bin)

# for including config.h and for includes like <dir/foo.h>
include_directories( ${CMAKE_SOURCE_DIR}/.. ${CMAKE_BINARY_DIR} ${CMAKE_INCLUDE_PATH} )

# linker search directories
link_directories(${DBUS_LIB_DIR} ${LIBRARY_OUTPUT_PATH} )
include_directories( ${CMAKE_LIBRARY_PATH}  )

set(DBUS_INCLUDES)

ENABLE_TESTING()


########### basic vars ###############

if (DBUSDIR)
	set(DBUS_INSTALL_DIR "${DBUSDIR}")
endif (DBUSDIR)
if ($ENV{DBUSDIR})
	set(DBUS_INSTALL_DIR "$ENV{DBUSDIR}")
endif ($ENV{DBUSDIR})

if (DBUS_INSTALL_DIR)
	set(CMAKE_INSTALL_PREFIX "${DBUS_INSTALL_DIR}" CACHE TYPE PATH FORCE)
else (DBUS_INSTALL_DIR)
	set(DBUS_INSTALL_DIR "${CMAKE_INSTALL_PREFIX}")
endif (DBUS_INSTALL_DIR)

set(prefix                   ${DBUS_INSTALL_DIR})
set(exec_prefix              ${prefix})
set(EXPANDED_LIBDIR          ${DBUS_INSTALL_DIR}/lib)
set(EXPANDED_INCLUDEDIR      ${DBUS_INSTALL_DIR}/include)
set(EXPANDED_BINDIR          ${DBUS_INSTALL_DIR}/bin)
set(EXPANDED_SYSCONFDIR      ${DBUS_INSTALL_DIR}/etc)
set(EXPANDED_DATADIR         ${DBUS_INSTALL_DIR}/data)
set(DBUS_MACHINE_UUID_FILE   ${DBUS_INSTALL_DIR}/lib/dbus/machine-id)
set(DBUS_BINDIR              ${EXPANDED_BINDIR})
set(DBUS_DAEMONDIR 			 ${EXPANDED_BINDIR})

########### command line options ###############
# TODO: take check from configure.in

#AC_ARG_ENABLE(tests, AS_HELP_STRING([--enable-tests],[enable unit test code]),enable_tests=$enableval,enable_tests=$USE_MAINTAINER_MODE)
OPTION(DBUS_BUILD_TESTS "enable unit test code" ON)
 
if(DBUS_BUILD_TESTS)
    add_definitions(-DDBUS_BUILD_TESTS)
endif(DBUS_BUILD_TESTS)

OPTION(DBUS_USE_OUTPUT_DEBUG_STRING "enable win32 debug port for message output" OFF)
if(DBUS_USE_OUTPUT_DEBUG_STRING)
    add_definitions(-DDBUS_USE_OUTPUT_DEBUG_STRING)
endif(DBUS_USE_OUTPUT_DEBUG_STRING)

if(WIN32)
	# win32 dbus service support - this support is not complete
	OPTION(DBUS_SERVICE "enable dbus service installer" OFF)
endif(WIN32)

#AC_ARG_ENABLE(ansi, AS_HELP_STRING([--enable-ansi],[enable -ansi -pedantic gcc flags]),enable_ansi=$enableval,enable_ansi=no)
OPTION(DBUS_ENABLE_ANSI "enable -ansi -pedantic gcc flags" OFF)
if(DBUS_ENABLE_ANSI)
   if(NOT MSVC)
        add_definitions(-ansi -D_POSIX_C_SOURCE=199309L -D_BSD_SOURCE -pedantic)
   else(NOT MSVC)
        add_definitions(-Za -D_POSIX_C_SOURCE=199309L -D_BSD_SOURCE -W4)
   endif(NOT MSVC)
endif(DBUS_ENABLE_ANSI)

#AC_ARG_ENABLE(verbose-mode, AS_HELP_STRING([--enable-verbose-mode],[support verbose debug mode]),enable_verbose_mode=$enableval,enable_verbose_mode=$USE_MAINTAINER_MODE)
OPTION(DBUS_ENABLE_VERBOSE_MODE "support verbose debug mode" ON)

#AC_ARG_ENABLE(checks, AS_HELP_STRING([--enable-checks],[include sanity checks on public API]),enable_checks=$enableval,enable_checks=yes)
OPTION(DBUS_DISABLE_CHECKS "Disable public API sanity checking" OFF)

if(NOT MSVC)
    #AC_ARG_ENABLE(gcov, AS_HELP_STRING([--enable-gcov],[compile with coverage profiling instrumentation (gcc only)]),enable_gcov=$enableval,enable_gcov=no)
    OPTION(DBUS_GCOV_ENABLED "compile with coverage profiling instrumentation (gcc only)" OFF)
    if(DBUS_GCOV_ENABLED)
            add_definitions(-fprofile-arcs -ftest-coverage)
            # FIXME!!!!
            ## remove optimization
    #        CFLAGS=`echo "$CFLAGS" | sed -e 's/-O[0-9]*//g'`
    endif(DBUS_GCOV_ENABLED)
endif(NOT MSVC)

#AC_ARG_ENABLE(abstract-sockets, AS_HELP_STRING([--enable-abstract-sockets],[use abstract socket namespace (linux only)]),enable_abstract_sockets=$enableval,enable_abstract_sockets=auto)
# -> moved before include(ConfigureChecks.cmake)

#AC_ARG_ENABLE(selinux, AS_HELP_STRING([--enable-selinux],[build with SELinux support]),enable_selinux=$enableval,enable_selinux=auto)
#selinux missing

#AC_ARG_ENABLE(dnotify, AS_HELP_STRING([--enable-dnotify],[build with dnotify support (linux only)]),enable_dnotify=$enableval,enable_dnotify=auto)
if("${CMAKE_SYSTEM_NAME}" STREQUAL "Linux")
    OPTION(DBUS_BUS_ENABLE_DNOTIFY_ON_LINUX "build with dnotify support (linux only)" ON) # add a check !
endif("${CMAKE_SYSTEM_NAME}" STREQUAL "Linux")

#AC_ARG_ENABLE(kqueue, AS_HELP_STRING([--enable-kqueue],[build with kqueue support (FreeBSD only)]),enable_kqueue=$enableval,enable_kqueue=auto)
#missing

#AC_ARG_ENABLE(console-owner-file, AS_HELP_STRING([--enable-console-owner-file],[enable console owner file]),enable_console_owner_file=$enableval,enable_console_owner_file=auto)
STRING(TOUPPER ${CMAKE_SYSTEM_NAME} sysname)
if("${sysname}" MATCHES ".*SOLARIS.*")
    OPTION(HAVE_CONSOLE_OWNER_FILE "enable console owner file (solaris only)" ON)
    if(HAVE_CONSOLE_OWNER_FILE)
        SET(DBUS_CONSOLE_OWNER_FILE "/dev/console" CACHE STRING "Directory to check for console ownerhip")
    endif(HAVE_CONSOLE_OWNER_FILE)
endif("${sysname}" MATCHES ".*SOLARIS.*")

#AC_ARG_WITH(xml, AS_HELP_STRING([--with-xml=[libxml/expat]],[XML library to use]))
if(NOT LIBXML2_FOUND AND NOT LIBEXPAT_FOUND)
    message(FATAL "Neither expat nor libxml2 found!")
endif(NOT LIBXML2_FOUND AND NOT LIBEXPAT_FOUND)

if(LIBEXPAT_FOUND)
    OPTION(DBUS_USE_EXPAT "Use expat (== ON) or libxml2 (==OFF)" ON)
else(LIBEXPAT_FOUND)
    OPTION(DBUS_USE_EXPAT "Use expat (== ON) or libxml2 (==OFF)" OFF)
endif(LIBEXPAT_FOUND)

if(DBUS_USE_EXPAT)
    SET(XML_LIB "Expat")
    SET(XML_LIBRARY     ${LIBEXPAT_LIBRARIES})
    SET(XML_INCLUDE_DIR ${LIBEXPAT_INCLUDE_DIR})
else(DBUS_USE_EXPAT)
    SET(XML_LIB "LibXML2")
    SET(XML_LIBRARY     ${LIBXML2_LIBRARIES})
    SET(XML_INCLUDE_DIR ${LIBXML2_INCLUDE_DIR})
endif(DBUS_USE_EXPAT)


#AC_ARG_WITH(init-scripts, AS_HELP_STRING([--with-init-scripts=[redhat]],[Style of init scripts to install]))
#AC_ARG_WITH(session-socket-dir, AS_HELP_STRING([--with-session-socket-dir=[dirname]],[Where to put sockets for the per-login-session message bus]))
#AC_ARG_WITH(test-socket-dir, AS_HELP_STRING([--with-test-socket-dir=[dirname]],[Where to put sockets for make check]))
#AC_ARG_WITH(system-pid-file, AS_HELP_STRING([--with-system-pid-file=[pidfile]],[PID file for systemwide daemon]))
#AC_ARG_WITH(system-socket, AS_HELP_STRING([--with-system-socket=[filename]],[UNIX domain socket for systemwide daemon]))
#AC_ARG_WITH(console-auth-dir, AS_HELP_STRING([--with-console-auth-dir=[dirname]],[directory to check for console ownerhip]))
#AC_ARG_WITH(console-owner-file, AS_HELP_STRING([--with-console-owner-file=[filename]],[file whose owner determines current console owner]))
#AC_ARG_WITH(dbus_user, AS_HELP_STRING([--with-dbus-user=<user>],[User for running the DBUS daemon (messagebus)]))
#AC_ARG_WITH(dbus_daemondir, AS_HELP_STRING([--with-dbus-daemondir=[dirname]],[Directory for installing the DBUS daemon]))
# all missing or hardcoded for now

# 'hidden' ones
set(atomic_int OFF)
set(atomic_int486 OFF)
if(CMAKE_COMPILER_IS_GNUCC AND NOT DBUS_ENABLE_ANSI)
    FIND_PROGRAM(UNAME_EXECUTABLE
                    NAMES uname
                    PATHS /bin /usr/bin /usr/local/bin c:/Programme/MSys/bin d:/Programme/MSys/bin)

    if(UNAME_EXECUTABLE)
        EXECUTE_PROCESS(COMMAND ${UNAME_EXECUTABLE} "-m"
                        OUTPUT_VARIABLE UNAME_OUTPUT)

        if("UNAME_OUTPUT" MATCHES "^.*i[0123]86.*$")
            set(atomic_int ON)
        else("UNAME_OUTPUT" MATCHES "^.*i[0123]86.*$")
            if("UNAME_OUTPUT" MATCHES "^.*i?86.*$")
                set(atomic_int ON)
                set(atomic_int_486 ON)
            endif("UNAME_OUTPUT" MATCHES "^.*i?86.*$")
        endif("UNAME_OUTPUT" MATCHES "^.*i[0123]86.*$")
    endif(UNAME_EXECUTABLE)
endif(CMAKE_COMPILER_IS_GNUCC AND NOT DBUS_ENABLE_ANSI)

OPTION(DBUS_HAVE_ATOMIC_INT    "Some atomic integer implementation present" ${atomic_int})
OPTION(DBUS_USE_ATOMIC_INT_486 "Use atomic integer implementation for 486" ${atomic_int_486})

if(X11_FOUND)
  OPTION(DBUS_BUILD_X11 "Build X11-dependent code " ON)
endif(X11_FOUND)

# test binary names
if (WIN32)
	set (EXT ".exe")
endif(WIN32)

if (MSVC_IDE)
    if(CMAKE_BUILD_TYPE MATCHES Debug)
		set(IDE_BIN Debug/ )
		message(STATUS)
		message(STATUS "Visual Studio: test programs will only work with 'Debug' configuration!")
		message(STATUS "To run tests with 'Release' configuration use -DCMAKE_BUILD_TYPE=Release")
		message(STATUS "Add '..\\..\\test\\data' to the command line option of the test programs")
		message(STATUS)
    else(CMAKE_BUILD_TYPE MATCHES Debug)
		set(IDE_BIN Release/)
		message(STATUS)
		message(STATUS "Visual Studio: test programs will only work with 'Release' configuration!")
		message(STATUS "To run tests with 'Debug' configuration use -DCMAKE_BUILD_TYPE=Debug")
		message(STATUS "Add '..\\..\\test\\data' to the command line option of the test programs")
		message(STATUS)
    endif(CMAKE_BUILD_TYPE MATCHES Debug)
	set (TEST_PATH_FORCE FORCE)
	FILE(REMOVE ${CMAKE_BINARY_DIR}/data/dbus-1/services)
endif (MSVC_IDE)

set(TEST_SERVICE_DIR          ${CMAKE_BINARY_DIR}/test/data/valid-service-files     CACHE STRING "Full path to test file test/data/valid-service-files in builddir" )
set(TEST_SERVICE_BINARY       ${CMAKE_BINARY_DIR}/bin/${IDE_BIN}test-service${EXT}       CACHE STRING "Full path to test file test/test-service in builddir" ${TEST_PATH_FORCE})
set(TEST_SHELL_SERVICE_BINARY ${CMAKE_BINARY_DIR}/bin/${IDE_BIN}test-shell-service${EXT} CACHE STRING "Full path to test file test/test-shell-service in builddir" ${TEST_PATH_FORCE})
set(TEST_EXIT_BINARY          ${CMAKE_BINARY_DIR}/bin/${IDE_BIN}test-exit${EXT}          CACHE STRING "Full path to test file test/test-exit in builddir" ${TEST_PATH_FORCE})
set(TEST_SEGFAULT_BINARY      ${CMAKE_BINARY_DIR}/bin/${IDE_BIN}test-segfault${EXT}      CACHE STRING "Full path to test file test/test-segfault in builddir" ${TEST_PATH_FORCE})
set(TEST_SLEEP_FOREVER_BINARY ${CMAKE_BINARY_DIR}/bin/${IDE_BIN}test-sleep-forever${EXT} CACHE STRING "Full path to test file test/test-sleep-forever in builddir" ${TEST_PATH_FORCE})

#### Find socket directories
 if (NOT $ENV{TMPDIR} STREQUAL "")
     set (DBUS_SESSION_SOCKET_DIR $ENV{TMPDIR})
 else (NOT $ENV{TMPDIR} STREQUAL "")
     if (NOT $ENV{TEMP} STREQUAL "")
         set (DBUS_SESSION_SOCKET_DIR $ENV{TEMP})
     else (NOT $ENV{TEMP} STREQUAL "")
         if (NOT $ENV{TMP} STREQUAL "")
             set (DBUS_SESSION_SOCKET_DIR $ENV{TMP})
         else (NOT $ENV{TMP} STREQUAL "")
         if (WIN32)
             #Should never happen, both TMP and TEMP seem always set on Windows
             message(FATAL "Could not determine a usable temporary directory")
         else(WIN32)
            set (DBUS_SESSION_SOCKET_DIR /tmp)
         endif(WIN32)
         endif (NOT $ENV{TMP} STREQUAL "")
     endif (NOT $ENV{TEMP} STREQUAL "")
 endif (NOT $ENV{TMPDIR} STREQUAL "")

#AC_ARG_WITH(test-socket-dir, AS_HELP_STRING([--with-test-socket-dir=[dirname]],[Where to put sockets for make check]))

#AC_ARG_WITH(system-pid-file, AS_HELP_STRING([--with-system-pid-file=[pidfile]],[PID file for systemwide daemon]))

#if ! test -z "$with_system_pid_file"; then
#   DBUS_SYSTEM_PID_FILE=$with_system_pid_file
#elif test x$operating_system = xredhat ; then
#   DBUS_SYSTEM_PID_FILE=${EXPANDED_LOCALSTATEDIR}/run/messagebus.pid
#else
#   DBUS_SYSTEM_PID_FILE=${EXPANDED_LOCALSTATEDIR}/run/dbus/pid
#fi
# TODO: fix redhet
if (WIN32)
  # bus-test expects a non empty string
	set (DBUS_SYSTEM_PID_FILE "/dbus-pid")
else (WIN32)
	set (DBUS_SYSTEM_PID_FILE ${EXPANDED_LOCALSTATEDIR}/run/dbus/pid)
endif (WIN32)

#AC_ARG_WITH(system-socket, AS_HELP_STRING([--with-system-socket=[filename]],[UNIX domain socket for systemwide daemon]))

#AC_ARG_WITH(console-auth-dir, AS_HELP_STRING([--with-console-auth-dir=[dirname]],[directory to check for console ownerhip]))

if (WIN32)
	set (DBUS_CONSOLE_AUTH_DIR "")
else (WIN32)
	set (DBUS_CONSOLE_AUTH_DIR "/var/run/console/")
endif (WIN32)

#AC_ARG_WITH(dbus_user, AS_HELP_STRING([--with-dbus-user=<user>],[User for running the DBUS daemon (messagebus)]))

set (DBUS_USER )


if (WIN32)
  set (DBUS_SYSTEM_BUS_DEFAULT_ADDRESS "nonce-tcp:" CACHE STRING "system bus default address" )
  set (DBUS_SESSION_BUS_DEFAULT_ADDRESS "nonce-tcp:" CACHE STRING "session bus default address" )

  set (DBUS_SYSTEM_CONFIG_FILE "etc/dbus-1/system.conf")
  set (DBUS_SESSION_CONFIG_FILE "etc/dbus-1/session.conf")
  # bus-test expects a non empty string
  set (DBUS_USER "Administrator")
  set (DBUS_DATADIR "data")
else (WIN32)
  set (DBUS_SYSTEM_BUS_DEFAULT_ADDRESS "unix:tmpdir=" CACHE STRING "system bus default address" )
  set (DBUS_SESSION_BUS_DEFAULT_ADDRESS "unix:path=${DBUS_SESSION_SOCKET_DIR}" CACHE STRING "session bus default address" )
  set (sysconfdir "")
  set (configdir ${sysconfdir}/dbus-1 )
  set (DBUS_SYSTEM_CONFIG_FILE  ${configdir}/system.conf)
  set (DBUS_SESSION_CONFIG_FILE ${configdir}/session.conf)
  set (DBUS_USER "root")
  set (DBUS_DATADIR ${EXPANDED_DATADIR})
endif (WIN32)

set(DBUS_DAEMON_NAME dbus-daemon CACHE STRING "The name of the dbus daemon executable")

########### create config.h ###############

#include(ConfigureChecks.cmake)

# better use flags for gcc
if (MINGW)
	set (HAVE_GNUC_VARARGS 1)
endif(MINGW)

# compiler definitions
add_definitions(-DHAVE_CONFIG_H=1)
add_definitions(${DBUS_BUS_CFLAGS} -DDBUS_API_SUBJECT_TO_CHANGE)


if (DBUS_BUILD_TESTS)
    # set variables used for the .in files (substituted by configure_file) in test/data:
    set(TEST_VALID_SERVICE_DIR ${CMAKE_BINARY_DIR}/test/data/valid-service-files)
    set(TEST_VALID_SERVICE_SYSTEM_DIR ${CMAKE_BINARY_DIR}/test/data/valid-service-files-system)
    set(TEST_INVALID_SERVICE_SYSTEM_DIR ${CMAKE_BINARY_DIR}/test/data/invalid-service-files-system)
    set(TEST_SOCKET_DIR ${DBUS_SESSION_SOCKET_DIR} )
    set(TEST_LAUNCH_HELPER_BINARY ${EXECUTABLE_OUTPUT_PATH}/dbus-daemon-launch-helper-test)
    set(TEST_PRIVSERVER_BINARY ${EXECUTABLE_OUTPUT_PATH}/test-privserver)
    if (UNIX)
        set (TEST_LISTEN "debug-pipe:name=test-server")
        set (TEST_CONNECTION "${TEST_LISTEN}")
    endif (UNIX)
    if (WIN32)
        set (TEST_LISTEN "tcp:host=localhost,port=12436")
        set (TEST_CONNECTION "${TEST_LISTEN}")
    endif (WIN32)
endif  (DBUS_BUILD_TESTS)

set(DBUS_LIBRARIES dbus-1)
set(DBUS_INTERNAL_LIBRARIES dbus-internal)

# settings for building and using static internal lib
# important note: DBUS_INTERNAL_xxxxx_DEFINITIONS must *not* be set when building dbus-1 library
set (DBUS_INTERNAL_ADD_LIBRARY_OPTIONS STATIC)
set (DBUS_INTERNAL_LIBRARY_DEFINITIONS "-DDBUS_STATIC_BUILD")
set (DBUS_INTERNAL_CLIENT_DEFINITIONS "-DDBUS_STATIC_BUILD")

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/config.h )
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/dbus-env.bat.cmake ${CMAKE_BINARY_DIR}/bin/dbus-env.bat )
install_files(/bin FILES ${CMAKE_BINARY_DIR}/bin/dbus-env.bat)
add_definitions(-DHAVE_CONFIG_H=1)

########### subdirs ###############

add_subdirectory( dbus )
add_subdirectory( bus )
if (DBUS_BUILD_TESTS)
	add_subdirectory( test )
endif (DBUS_BUILD_TESTS)
add_subdirectory( tools )
add_subdirectory( doc )


OPTION(DBUS_INSTALL_SYSTEM_LIBS "install required system libraries" OFF)
MESSAGE(" ")
MESSAGE("set -DDBUS_INSTALL_SYSTEM_LIBS=1 to install runtime libraries too")
MESSAGE("set DBUSDIR (environment or cmake option) to overwrite the default install directory ")
MESSAGE(" ")
MESSAGE(" ")
GET_FILENAME_COMPONENT(C_COMPILER ${CMAKE_C_COMPILER} NAME)
GET_FILENAME_COMPONENT(CXX_COMPILER ${CMAKE_CXX_COMPILER} NAME)

message("                  D-BUS ${DBUS_VERSION}                               ")
message("                  ===========                                         ")
message("                                                                      ")
message("        install prefix:           ${prefix}                           ")
message("        install exec_prefix:      ${exec_prefix}                      ")
message("        install libdir:           ${EXPANDED_LIBDIR}                  ")
message("        install bindir:           ${EXPANDED_BINDIR}                  ")
message("        install sysconfdir:       ${EXPANDED_SYSCONFDIR}              ")
#message("        install localstatedir:    ${EXPANDED_LOCALSTATEDIR}           ")
message("        install datadir:          ${EXPANDED_DATADIR}                 ")
message("        source code location:     ${DBUS_SOURCE_DIR}                  ")
message("        build dir:                ${CMAKE_BINARY_DIR}                 ")
message("        c compiler:               ${C_COMPILER}                       ")
message("        cflags:                   ${CMAKE_C_FLAGS}                    ")
message("        cflags debug:             ${CMAKE_C_FLAGS_DEBUG}              ")
message("        cflags release:           ${CMAKE_C_FLAGS_RELEASE}            ")
message("        cxx compiler:             ${CXX_COMPILER}                     ")
message("        cxxflags:                 ${CMAKE_CXX_FLAGS}                  ")
message("        cxxflags debug:           ${CMAKE_CXX_FLAGS_DEBUG}            ")
message("        cxxflags release:         ${CMAKE_CXX_FLAGS_RELEASE}          ")
message("        64-bit int:               ${DBUS_INT64_TYPE}                  ")
message("        32-bit int:               ${DBUS_INT32_TYPE}                  ")
message("        16-bit int:               ${DBUS_INT16_TYPE}                  ")
message("        Doxygen:                  ${DOXYGEN}                          ")
message("        Docbook Generator:        ${DOCBOOK_GENERATOR_NAME}           ")


#message("        Maintainer mode:          ${USE_MAINTAINER_MODE}              ")
message("        gcc coverage profiling:   ${DBUS_GCOV_ENABLED}                ")
message("        Building unit tests:      ${DBUS_BUILD_TESTS}                 ")
message("        Building verbose mode:    ${DBUS_ENABLE_VERBOSE_MODE}         ")
message("        Building w/o assertions:  ${DBUS_DISABLE_ASSERTS}             ")
message("        Building w/o checks:      ${DBUS_DISABLE_CHECKS}              ")
message("        installing system libs:   ${DBUS_INSTALL_SYSTEM_LIBS}         ")
#message("        Building SELinux support: ${have_selinux}                     ")
#message("        Building dnotify support: ${have_dnotify}                     ")
message("        Building Doxygen docs:    ${DBUS_ENABLE_DOXYGEN_DOCS}         ")
message("        Building XML docs:        ${DBUS_ENABLE_XML_DOCS}             ")
#message("        Gettext libs (empty OK):  ${INTLLIBS}                         ")
message("        Using XML parser:         ${XML_LIB}                          ")
message("        Daemon executable name:   ${DBUS_DAEMON_NAME}")
if (WIN32)
message("        System bus address:       ${DBUS_SYSTEM_BUS_DEFAULT_ADDRESS}  ")
message("        Session bus address:      ${DBUS_SESSION_BUS_DEFAULT_ADDRESS} ")
else (WIN32)
#message("        Init scripts style:       ${with_init_scripts}                ")
#message("        Abstract socket names:    ${have_abstract_sockets}            ")
message("        System bus socket:        ${DBUS_SYSTEM_SOCKET}               ")
message("        System bus address:       ${DBUS_SYSTEM_BUS_DEFAULT_ADDRESS}  ")
message("        System bus PID file:      ${DBUS_SYSTEM_PID_FILE}             ")
message("        Session bus socket dir:   ${DBUS_SESSION_SOCKET_DIR}          ")
message("        Console auth dir:         ${DBUS_CONSOLE_AUTH_DIR}            ")
message("        System bus user:          ${DBUS_USER}                        ")
message("        'make check' socket dir:  ${TEST_SOCKET_DIR}                  ")
endif (WIN32)
message("        Test listen address:      ${TEST_LISTEN}                      ")
if (MSVC)
message("        build timestamp:          ${DBUS_BUILD_TIMESTAMP}             ")
endif (MSVC)

MESSAGE(" ")
if (DBUS_BUILD_TESTS)
    message("NOTE: building with unit tests increases the size of the installed library and renders it insecure.")
endif(DBUS_BUILD_TESTS)

if (DBUS_BUILD_TESTS AND DBUS_DISABLE_ASSERTS)
    message("NOTE: building with unit tests but without assertions means tests may not properly report failures (this configuration is only useful when doing something like profiling the tests)")
endif(DBUS_BUILD_TESTS AND DBUS_DISABLE_ASSERTS)

if (DBUS_GCOV_ENABLED)
    message("NOTE: building with coverage profiling is definitely for developers only.")
endif(DBUS_GCOV_ENABLED)

if (DBUS_ENABLE_VERBOSE_MODE)
    message("NOTE: building with verbose mode increases library size, may slightly increase security risk, and decreases performance.")
endif(DBUS_ENABLE_VERBOSE_MODE)

if(NOT DBUS_DISABLE_ASSERTS)
    message("NOTE: building with assertions increases library size and decreases performance.")
endif(NOT DBUS_DISABLE_ASSERTS)

if (DBUS_DISABLE_CHECKS)
    message("NOTE: building without checks for arguments passed to public API makes it harder to debug apps using D-BUS, but will slightly decrease D-BUS library size and _very_ slightly improve performance.")
endif(DBUS_DISABLE_CHECKS)
MESSAGE(" ")

INCLUDE(modules/CPackInstallConfig.cmake)
