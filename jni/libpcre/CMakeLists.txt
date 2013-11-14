# CMakeLists.txt
#
#
# This file allows building PCRE with the CMake configuration and build
# tool. Download CMake in source or binary form from http://www.cmake.org/
#
# Original listfile by Christian Ehrlicher <Ch.Ehrlicher@gmx.de>
# Refined and expanded by Daniel Richard G. <skunk@iSKUNK.ORG>
# 2007-09-14 mod by Sheri so 7.4 supported configuration options can be entered
# 2007-09-19 Adjusted by PH to retain previous default settings
# 2007-12-26 (a) On UNIX, use names libpcre instead of just pcre
#            (b) Ensure pcretest and pcregrep link with the local library,
#                not a previously-installed one.
#            (c) Add PCRE_SUPPORT_LIBREADLINE, PCRE_SUPPORT_LIBZ, and
#                PCRE_SUPPORT_LIBBZ2.
# 2008-01-20 Brought up to date to include several new features by Christian
#            Ehrlicher.
# 2008-01-22 Sheri added options for backward compatibility of library names
#            when building with minGW:
#            if "ON", NON_STANDARD_LIB_PREFIX causes shared libraries to
#            be built without "lib" as prefix. (The libraries will be named
#            pcre.dll, pcreposix.dll and pcrecpp.dll).
#            if "ON", NON_STANDARD_LIB_SUFFIX causes shared libraries to
#            be built with suffix of "-0.dll". (The libraries will be named
#            libpcre-0.dll, libpcreposix-0.dll and libpcrecpp-0.dll - same names
#            built by default with Configure and Make.
# 2008-01-23 PH removed the automatic build of pcredemo.
# 2008-04-22 PH modified READLINE support so it finds NCURSES when needed.
# 2008-07-03 PH updated for revised UCP property support (change of files)
# 2009-03-23 PH applied Steven Van Ingelgem's patch to change the name
#            CMAKE_BINARY_DIR to PROJECT_BINARY_DIR so that it works when PCRE
#            is included within another project.
# 2009-03-23 PH applied a modified version of Steven Van Ingelgem's patches to
#            add options to stop the building of pcregrep and the tests, and
#            to disable the final configuration report.
# 2009-04-11 PH applied Christian Ehrlicher's patch to show compiler flags that
#            are set by specifying a release type.
# 2010-01-02 PH added test for stdint.h
# 2010-03-02 PH added test for inttypes.h
# 2011-08-01 PH added PCREGREP_BUFSIZE
# 2011-08-22 PH added PCRE_SUPPORT_JIT
# 2011-09-06 PH modified WIN32 ADD_TEST line as suggested by Sergey Cherepanov
# 2011-09-06 PH added PCRE_SUPPORT_PCREGREP_JIT
# 2011-10-04 Sheri added support for including coff data in windows shared libraries
#            compiled with MINGW if pcre.rc and/or pcreposix.rc are placed in
#            the source dir by the user prior to building
# 2011-10-04 Sheri changed various add_test's to use exes' location built instead
#            of DEBUG location only (likely only matters in MSVC)
# 2011-10-04 Sheri added scripts to provide needed variables to RunTest and
#            RunGrepTest (used for UNIX and Msys)
# 2011-10-04 Sheri added scripts to provide needed variables and to execute
#            RunTest.bat in Win32 (for effortless testing with "make test")
# 2011-10-04 Sheri Increased minimum required cmake version

PROJECT(PCRE C CXX)

# Increased minimum to 2.8.0 to support newer add_test features

CMAKE_MINIMUM_REQUIRED(VERSION 2.8.0)

SET(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake) # for FindReadline.cmake

# external packages
FIND_PACKAGE( BZip2 )
FIND_PACKAGE( ZLIB )
FIND_PACKAGE( Readline )

# Configuration checks

INCLUDE(CheckIncludeFile)
INCLUDE(CheckIncludeFileCXX)
INCLUDE(CheckFunctionExists)
INCLUDE(CheckTypeSize)

CHECK_INCLUDE_FILE(dirent.h     HAVE_DIRENT_H)
CHECK_INCLUDE_FILE(stdint.h     HAVE_STDINT_H)
CHECK_INCLUDE_FILE(inttypes.h   HAVE_INTTYPES_H)
CHECK_INCLUDE_FILE(sys/stat.h   HAVE_SYS_STAT_H)
CHECK_INCLUDE_FILE(sys/types.h  HAVE_SYS_TYPES_H)
CHECK_INCLUDE_FILE(unistd.h     HAVE_UNISTD_H)
CHECK_INCLUDE_FILE(windows.h    HAVE_WINDOWS_H)

CHECK_INCLUDE_FILE_CXX(type_traits.h            HAVE_TYPE_TRAITS_H)
CHECK_INCLUDE_FILE_CXX(bits/type_traits.h       HAVE_BITS_TYPE_TRAITS_H)

CHECK_FUNCTION_EXISTS(bcopy     HAVE_BCOPY)
CHECK_FUNCTION_EXISTS(memmove   HAVE_MEMMOVE)
CHECK_FUNCTION_EXISTS(strerror  HAVE_STRERROR)
CHECK_FUNCTION_EXISTS(strtoll   HAVE_STRTOLL)
CHECK_FUNCTION_EXISTS(strtoq    HAVE_STRTOQ)
CHECK_FUNCTION_EXISTS(_strtoi64 HAVE__STRTOI64)

CHECK_TYPE_SIZE("long long"             LONG_LONG)
CHECK_TYPE_SIZE("unsigned long long"    UNSIGNED_LONG_LONG)

# User-configurable options
#
# (Note: CMakeSetup displays these in alphabetical order, regardless of
# the order we use here)

SET(BUILD_SHARED_LIBS OFF CACHE BOOL
    "Build shared libraries instead of static ones.")

OPTION(PCRE_BUILD_PCRECPP "Build the PCRE C++ library (pcrecpp)." ON)

SET(PCRE_EBCDIC OFF CACHE BOOL
    "Use EBCDIC coding instead of ASCII. (This is rarely used outside of mainframe systems)")

SET(PCRE_LINK_SIZE "2" CACHE STRING
    "Internal link size (2, 3 or 4 allowed). See LINK_SIZE in config.h.in for details.")

SET(PCRE_MATCH_LIMIT "10000000" CACHE STRING
    "Default limit on internal looping. See MATCH_LIMIT in config.h.in for details.")

SET(PCRE_MATCH_LIMIT_RECURSION "MATCH_LIMIT" CACHE STRING
    "Default limit on internal recursion. See MATCH_LIMIT_RECURSION in config.h.in for details.")

SET(PCREGREP_BUFSIZE "20480" CACHE STRING
    "Buffer size parameter for pcregrep. See PCREGREP_BUFSIZE in config.h.in for details.")

SET(PCRE_NEWLINE "LF" CACHE STRING
    "What to recognize as a newline (one of CR, LF, CRLF, ANY, ANYCRLF).")

SET(PCRE_NO_RECURSE OFF CACHE BOOL
    "If ON, then don't use stack recursion when matching. See NO_RECURSE in config.h.in for details.")

SET(PCRE_POSIX_MALLOC_THRESHOLD "10" CACHE STRING
    "Threshold for malloc() usage. See POSIX_MALLOC_THRESHOLD in config.h.in for details.")

SET(PCRE_SUPPORT_JIT OFF CACHE BOOL
    "Enable support for Just-in-time compiling.")

SET(PCRE_SUPPORT_PCREGREP_JIT ON CACHE BOOL
    "Enable use of Just-in-time compiling in pcregrep.")

SET(PCRE_SUPPORT_UNICODE_PROPERTIES OFF CACHE BOOL
    "Enable support for Unicode properties. (If set, UTF-8 support will be enabled as well)")

SET(PCRE_SUPPORT_UTF8 OFF CACHE BOOL
    "Enable support for the Unicode UTF-8 encoding.")

SET(PCRE_SUPPORT_BSR_ANYCRLF OFF CACHE BOOL
    "ON=Backslash-R matches only LF CR and CRLF, OFF=Backslash-R matches all Unicode Linebreaks")

OPTION(PCRE_SHOW_REPORT    "Show the final configuration report" ON)
OPTION(PCRE_BUILD_PCREGREP "Build pcregrep" ON)
OPTION(PCRE_BUILD_TESTS    "Build the tests" ON)

IF (PCRE_BUILD_TESTS)
  IF (NOT PCRE_BUILD_PCREGREP)
    MESSAGE(STATUS "** Building tests requires pcregrep: PCRE_BUILD_PCREGREP forced ON")
    SET(PCRE_BUILD_PCREGREP ON)
  ENDIF(NOT PCRE_BUILD_PCREGREP)
ENDIF(PCRE_BUILD_TESTS)

IF (MINGW)
  OPTION(NON_STANDARD_LIB_PREFIX
         "ON=Shared libraries built in mingw will be named pcre.dll, etc., instead of libpcre.dll, etc."
         OFF)

  OPTION(NON_STANDARD_LIB_SUFFIX
         "ON=Shared libraries built in mingw will be named libpcre-0.dll, etc., instead of libpcre.dll, etc."
         OFF)
ENDIF(MINGW)

# bzip2 lib
IF(BZIP2_FOUND)
  OPTION (PCRE_SUPPORT_LIBBZ2 "Enable support for linking pcregrep with libbz2." ON)
ENDIF(BZIP2_FOUND)
IF(PCRE_SUPPORT_LIBBZ2)
  INCLUDE_DIRECTORIES(${BZIP2_INCLUDE_DIR})
ENDIF(PCRE_SUPPORT_LIBBZ2)

# zlib
IF(ZLIB_FOUND)
  OPTION (PCRE_SUPPORT_LIBZ "Enable support for linking pcregrep with libz." ON)
ENDIF(ZLIB_FOUND)
IF(PCRE_SUPPORT_LIBZ)
  INCLUDE_DIRECTORIES(${ZLIB_INCLUDE_DIR})
ENDIF(PCRE_SUPPORT_LIBZ)

# readline lib
IF(READLINE_FOUND)
  OPTION (PCRE_SUPPORT_LIBREADLINE  "Enable support for linking pcretest with libreadline." ON)
ENDIF(READLINE_FOUND)
IF(PCRE_SUPPORT_LIBREADLINE)
  INCLUDE_DIRECTORIES(${READLINE_INCLUDE_DIR})
ENDIF(PCRE_SUPPORT_LIBREADLINE)

# Prepare build configuration

SET(pcre_have_type_traits 0)
SET(pcre_have_bits_type_traits 0)

IF(HAVE_TYPE_TRAITS_H)
        SET(pcre_have_type_traits 1)
ENDIF(HAVE_TYPE_TRAITS_H)

IF(HAVE_BITS_TYPE_TRAITS_H)
        SET(pcre_have_bits_type_traits 1)
ENDIF(HAVE_BITS_TYPE_TRAITS_H)

SET(pcre_have_long_long 0)
SET(pcre_have_ulong_long 0)

IF(HAVE_LONG_LONG)
        SET(pcre_have_long_long 1)
ENDIF(HAVE_LONG_LONG)

IF(HAVE_UNSIGNED_LONG_LONG)
        SET(pcre_have_ulong_long 1)
ENDIF(HAVE_UNSIGNED_LONG_LONG)

IF(NOT BUILD_SHARED_LIBS)
        SET(PCRE_STATIC 1)
ENDIF(NOT BUILD_SHARED_LIBS)

IF(PCRE_SUPPORT_BSR_ANYCRLF)
        SET(BSR_ANYCRLF 1)
ENDIF(PCRE_SUPPORT_BSR_ANYCRLF)

IF(PCRE_SUPPORT_UTF8 OR PCRE_SUPPORT_UNICODE_PROPERTIES)
        SET(SUPPORT_UTF8 1)
ENDIF(PCRE_SUPPORT_UTF8 OR PCRE_SUPPORT_UNICODE_PROPERTIES)

IF(PCRE_SUPPORT_UNICODE_PROPERTIES)
        SET(SUPPORT_UCP 1)
ENDIF(PCRE_SUPPORT_UNICODE_PROPERTIES)

IF(PCRE_SUPPORT_JIT)
        SET(SUPPORT_JIT 1)
ENDIF(PCRE_SUPPORT_JIT)

IF(PCRE_SUPPORT_PCREGREP_JIT)
        SET(SUPPORT_PCREGREP_JIT 1)
ENDIF(PCRE_SUPPORT_PCREGREP_JIT)

# This next one used to contain
#       SET(PCRETEST_LIBS ${READLINE_LIBRARY})
# but I was advised to add the NCURSES test as well, along with
# some modifications to cmake/FindReadline.cmake which should
# make it possible to override the default if necessary. PH

IF(PCRE_SUPPORT_LIBREADLINE)
        SET(SUPPORT_LIBREADLINE 1)
        SET(PCRETEST_LIBS ${READLINE_LIBRARY} ${NCURSES_LIBRARY})
ENDIF(PCRE_SUPPORT_LIBREADLINE)

IF(PCRE_SUPPORT_LIBZ)
        SET(SUPPORT_LIBZ 1)
        SET(PCREGREP_LIBS ${PCREGREP_LIBS} ${ZLIB_LIBRARIES})
ENDIF(PCRE_SUPPORT_LIBZ)

IF(PCRE_SUPPORT_LIBBZ2)
        SET(SUPPORT_LIBBZ2 1)
        SET(PCREGREP_LIBS ${PCREGREP_LIBS} ${BZIP2_LIBRARIES})
ENDIF(PCRE_SUPPORT_LIBBZ2)

SET(NEWLINE "")

IF(PCRE_NEWLINE STREQUAL "LF")
        SET(NEWLINE "10")
ENDIF(PCRE_NEWLINE STREQUAL "LF")
IF(PCRE_NEWLINE STREQUAL "CR")
        SET(NEWLINE "13")
ENDIF(PCRE_NEWLINE STREQUAL "CR")
IF(PCRE_NEWLINE STREQUAL "CRLF")
        SET(NEWLINE "3338")
ENDIF(PCRE_NEWLINE STREQUAL "CRLF")
IF(PCRE_NEWLINE STREQUAL "ANY")
        SET(NEWLINE "-1")
ENDIF(PCRE_NEWLINE STREQUAL "ANY")
IF(PCRE_NEWLINE STREQUAL "ANYCRLF")
        SET(NEWLINE "-2")
ENDIF(PCRE_NEWLINE STREQUAL "ANYCRLF")

IF(NEWLINE STREQUAL "")
        MESSAGE(FATAL_ERROR "The PCRE_NEWLINE variable must be set to one of the following values: \"LF\", \"CR\", \"CRLF\", \"ANY\", \"ANYCRLF\".")
ENDIF(NEWLINE STREQUAL "")

IF(PCRE_EBCDIC)
        SET(EBCDIC 1)
ENDIF(PCRE_EBCDIC)

IF(PCRE_NO_RECURSE)
        SET(NO_RECURSE 1)
ENDIF(PCRE_NO_RECURSE)

# Output files
CONFIGURE_FILE(config-cmake.h.in
               ${PROJECT_BINARY_DIR}/config.h
               @ONLY)

CONFIGURE_FILE(pcre.h.generic
               ${PROJECT_BINARY_DIR}/pcre.h
               COPYONLY)

# What about pcre-config and libpcre.pc?

IF(PCRE_BUILD_PCRECPP)
        CONFIGURE_FILE(pcre_stringpiece.h.in
                       ${PROJECT_BINARY_DIR}/pcre_stringpiece.h
                       @ONLY)

        CONFIGURE_FILE(pcrecpparg.h.in
                       ${PROJECT_BINARY_DIR}/pcrecpparg.h
                       @ONLY)
ENDIF(PCRE_BUILD_PCRECPP)

# Character table generation

OPTION(PCRE_REBUILD_CHARTABLES "Rebuild char tables" OFF)
IF(PCRE_REBUILD_CHARTABLES)
  ADD_EXECUTABLE(dftables dftables.c)

  GET_TARGET_PROPERTY(DFTABLES_EXE dftables LOCATION)

  ADD_CUSTOM_COMMAND(
    COMMENT "Generating character tables (pcre_chartables.c) for current locale"
    DEPENDS dftables
    COMMAND ${DFTABLES_EXE}
    ARGS        ${PROJECT_BINARY_DIR}/pcre_chartables.c
    OUTPUT      ${PROJECT_BINARY_DIR}/pcre_chartables.c
  )
ELSE(PCRE_REBUILD_CHARTABLES)
  CONFIGURE_FILE(${PROJECT_SOURCE_DIR}/pcre_chartables.c.dist
                    ${PROJECT_BINARY_DIR}/pcre_chartables.c
                    COPYONLY)
ENDIF(PCRE_REBUILD_CHARTABLES)

# Source code

SET(PCRE_HEADERS ${PROJECT_BINARY_DIR}/pcre.h)

SET(PCRE_SOURCES
  ${PROJECT_BINARY_DIR}/pcre_chartables.c
  pcre_compile.c
  pcre_config.c
  pcre_dfa_exec.c
  pcre_exec.c
  pcre_fullinfo.c
  pcre_get.c
  pcre_globals.c
  pcre_info.c
  pcre_jit_compile.c
  pcre_newline.c
  pcre_maketables.c
  pcre_ord2utf8.c
  pcre_refcount.c
  pcre_study.c
  pcre_tables.c
  pcre_try_flipped.c
  pcre_ucd.c
  pcre_valid_utf8.c
  pcre_version.c
  pcre_xclass.c
)


SET(PCREPOSIX_HEADERS pcreposix.h)

SET(PCREPOSIX_SOURCES pcreposix.c)

IF(MINGW AND NOT PCRE_STATIC)
IF (EXISTS ${PROJECT_SOURCE_DIR}/pcre.rc)
ADD_CUSTOM_COMMAND(OUTPUT ${PROJECT_SOURCE_DIR}/pcre.o
PRE-LINK
COMMAND windres ARGS pcre.rc pcre.o
WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
COMMENT Using pcre coff info in mingw build)
SET(PCRE_SOURCES
  ${PCRE_SOURCES} ${PROJECT_SOURCE_DIR}/pcre.o
)
ENDIF(EXISTS ${PROJECT_SOURCE_DIR}/pcre.rc)
IF (EXISTS ${PROJECT_SOURCE_DIR}/pcreposix.rc)
ADD_CUSTOM_COMMAND(OUTPUT ${PROJECT_SOURCE_DIR}/pcreposix.o
PRE-LINK
COMMAND windres ARGS pcreposix.rc pcreposix.o
WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
COMMENT Using pcreposix coff info in mingw build)
SET(PCREPOSIX_SOURCES
  ${PCREPOSIX_SOURCES} ${PROJECT_SOURCE_DIR}/pcreposix.o
)
ENDIF(EXISTS ${PROJECT_SOURCE_DIR}/pcreposix.rc)
ENDIF(MINGW AND NOT PCRE_STATIC)

SET(PCRECPP_HEADERS
  pcrecpp.h
  pcre_scanner.h
  ${PROJECT_BINARY_DIR}/pcrecpparg.h
  ${PROJECT_BINARY_DIR}/pcre_stringpiece.h
)

SET(PCRECPP_SOURCES
        pcrecpp.cc
        pcre_scanner.cc
        pcre_stringpiece.cc
)

# Build setup

ADD_DEFINITIONS(-DHAVE_CONFIG_H)

IF(MSVC)
        ADD_DEFINITIONS(-D_CRT_SECURE_NO_DEPRECATE)
ENDIF(MSVC)

SET(CMAKE_INCLUDE_CURRENT_DIR 1)
# needed to make sure to not link debug libs
# against release libs and vice versa
IF(WIN32)
  SET(CMAKE_DEBUG_POSTFIX "d")
ENDIF(WIN32)

SET(targets)

# Libraries
# pcre
ADD_LIBRARY(pcre ${PCRE_HEADERS} ${PCRE_SOURCES} ${PROJECT_BINARY_DIR}/config.h)
SET(targets ${targets} pcre)
ADD_LIBRARY(pcreposix ${PCREPOSIX_HEADERS} ${PCREPOSIX_SOURCES})
SET(targets ${targets} pcreposix)
TARGET_LINK_LIBRARIES(pcreposix pcre)
IF(MINGW AND NOT PCRE_STATIC)
  IF(NON_STANDARD_LIB_PREFIX)
    SET_TARGET_PROPERTIES(pcre pcreposix
                        PROPERTIES PREFIX ""
    )
  ENDIF(NON_STANDARD_LIB_PREFIX)

  IF(NON_STANDARD_LIB_SUFFIX)
    SET_TARGET_PROPERTIES(pcre pcreposix
                        PROPERTIES SUFFIX "-0.dll"
    )
  ENDIF(NON_STANDARD_LIB_SUFFIX)
ENDIF(MINGW AND NOT PCRE_STATIC)


# pcrecpp
IF(PCRE_BUILD_PCRECPP)
  ADD_LIBRARY(pcrecpp ${PCRECPP_HEADERS} ${PCRECPP_SOURCES})
SET(targets ${targets} pcrecpp)
  TARGET_LINK_LIBRARIES(pcrecpp pcre)

  IF(MINGW AND NOT PCRE_STATIC)
    IF(NON_STANDARD_LIB_PREFIX)
      SET_TARGET_PROPERTIES(pcrecpp
                            PROPERTIES PREFIX ""
      )
    ENDIF(NON_STANDARD_LIB_PREFIX)

    IF(NON_STANDARD_LIB_SUFFIX)
      SET_TARGET_PROPERTIES(pcrecpp
                          PROPERTIES SUFFIX "-0.dll"
      )
    ENDIF(NON_STANDARD_LIB_SUFFIX)
  ENDIF(MINGW AND NOT PCRE_STATIC)
ENDIF(PCRE_BUILD_PCRECPP)


# Executables

# Removed by PH (2008-01-23) because pcredemo shouldn't really be built
# automatically, and it gave trouble in some environments anyway.
# ADD_EXECUTABLE(pcredemo pcredemo.c)
# TARGET_LINK_LIBRARIES(pcredemo pcreposix)
# IF(NOT BUILD_SHARED_LIBS)
#     # make sure to not use declspec(dllimport) in static mode on windows
#         SET_TARGET_PROPERTIES(pcredemo PROPERTIES COMPILE_FLAGS "-DPCRE_STATIC")
# ENDIF(NOT BUILD_SHARED_LIBS)

IF(PCRE_BUILD_PCREGREP OR PCRE_BUILD_TESTS)
  ADD_EXECUTABLE(pcregrep pcregrep.c)
  SET(targets ${targets} pcregrep)
  TARGET_LINK_LIBRARIES(pcregrep pcreposix ${PCREGREP_LIBS})
ENDIF(PCRE_BUILD_PCREGREP OR PCRE_BUILD_TESTS)

# Testing
IF(PCRE_BUILD_TESTS)
  ENABLE_TESTING()

  ADD_EXECUTABLE(pcretest pcretest.c)
  SET(targets ${targets} pcretest)
  TARGET_LINK_LIBRARIES(pcretest pcreposix ${PCRETEST_LIBS})

  IF(PCRE_SUPPORT_JIT)
    ADD_EXECUTABLE(pcre_jit_test pcre_jit_test.c)
    SET(targets ${targets} pcre_jit_test)
    TARGET_LINK_LIBRARIES(pcre_jit_test pcre)
  ENDIF(PCRE_SUPPORT_JIT)

  IF(PCRE_BUILD_PCRECPP)
    ADD_EXECUTABLE(pcrecpp_unittest pcrecpp_unittest.cc)
    SET(targets ${targets} pcrecpp_unittest)
    TARGET_LINK_LIBRARIES(pcrecpp_unittest pcrecpp)
    IF(MINGW AND NON_STANDARD_LIB_NAMES AND NOT PCRE_STATIC)
      SET_TARGET_PROPERTIES(pcrecpp
                        PROPERTIES PREFIX ""
      )
    ENDIF(MINGW AND NON_STANDARD_LIB_NAMES AND NOT PCRE_STATIC)

    ADD_EXECUTABLE(pcre_scanner_unittest pcre_scanner_unittest.cc)
    SET(targets ${targets} pcre_scanner_unittest)
    TARGET_LINK_LIBRARIES(pcre_scanner_unittest pcrecpp)

    ADD_EXECUTABLE(pcre_stringpiece_unittest pcre_stringpiece_unittest.cc)
    SET(targets ${targets} pcre_stringpiece_unittest)
    TARGET_LINK_LIBRARIES(pcre_stringpiece_unittest pcrecpp)
  ENDIF(PCRE_BUILD_PCRECPP)

  # exes in Debug location tested by the RunTest shell script
  # via "make test"
  GET_TARGET_PROPERTY(PCREGREP_EXE pcregrep DEBUG_LOCATION)
  GET_TARGET_PROPERTY(PCRETEST_EXE pcretest DEBUG_LOCATION)

# =================================================
  # Write out a CTest configuration file
  #
  FILE(WRITE ${PROJECT_BINARY_DIR}/CTestCustom.ctest
  "# This is a generated file.
MESSAGE(\"When testing is complete, review test output in the
${PROJECT_BINARY_DIR}/Testing/Temporary folder.\")
MESSAGE(\"\")
")

      FILE(WRITE ${PROJECT_BINARY_DIR}/pcre_test.sh
      "#! /bin/sh
# This is a generated file.
srcdir=${PROJECT_SOURCE_DIR}
pcregrep=${PCREGREP_EXE}
pcretest=${PCRETEST_EXE}
source ${PROJECT_SOURCE_DIR}/RunTest
if test \"$?\" != \"0\"; then exit 1; fi
# End
")

      FILE(WRITE ${PROJECT_BINARY_DIR}/pcre_grep_test.sh
      "#! /bin/sh
# This is a generated file.
srcdir=${PROJECT_SOURCE_DIR}
pcregrep=${PCREGREP_EXE}
pcretest=${PCRETEST_EXE}
source ${PROJECT_SOURCE_DIR}/RunGrepTest
if test \"$?\" != \"0\"; then exit 1; fi
# End
")

  IF(UNIX)
    ADD_TEST(pcre_test      sh ${PROJECT_BINARY_DIR}/pcre_test.sh)
    ADD_TEST(pcre_grep_test sh ${PROJECT_BINARY_DIR}/pcre_grep_test.sh)
  ENDIF(UNIX)

  IF(WIN32)
    # Provide environment for executing the bat file version of RunTest
    string(REPLACE "/" "\\" winsrc "${PROJECT_SOURCE_DIR}")

    FILE(WRITE ${PROJECT_BINARY_DIR}/pcre_test.txt
    "\@REM This is a generated file.
\@Echo off
setlocal
SET\ srcdir=\${srcdir}
SET\ pcretest=\${pcretest}
SET\ pcregrep=\${pcregrep}
call \"\${srcdir}\\RunTest.Bat\"
if errorlevel 1 exit /b 1
echo RunTest.bat tests successfully completed
")

  FILE(WRITE ${PROJECT_BINARY_DIR}/BatDriver.cmake
  "# This is a generated file.
# this script is run with arguments via the cmake command in add_test(NAME pcre_test_bat)
# BatDriver feeds the actual location of pcretest.exe and pcregrep.exe
FILE(TO_NATIVE_PATH \${pcretestx} pcretest)
FILE(TO_NATIVE_PATH \${pcregrepx} pcregrep)
FILE(TO_NATIVE_PATH \${srcdirx} srcdir)
configure_file(\"\${bindirx}/pcre_test.txt\" \"\${bindirx}/pcre_test.bat\")
# MESSAGE(\"cmake\ variable\ pcretest\ is\ \${pcretest}\")
# MESSAGE(\"cmake\ variable\ pcregrep\ is\ \${pcregrep}\")
# STRING(REPLACE \" \" \"\\ \" bindir \${bindirx})
MESSAGE(\"COMMAND pcre_test.bat \")
EXECUTE_PROCESS(COMMAND pcre_test.bat
WORKING_DIRECTORY .
OUTPUT_VARIABLE batoutput)
MESSAGE(\"OUTPUT: \${batoutput}\")
")

  ADD_TEST(NAME pcre_test_bat
  COMMAND ${CMAKE_COMMAND} -D bindirx=${PROJECT_BINARY_DIR} -D srcdirx=${PROJECT_SOURCE_DIR} -D pcretestx=$<TARGET_FILE:pcretest> -D pcregrepx=$<TARGET_FILE:pcregrep> -P "${PROJECT_BINARY_DIR}/BatDriver.cmake")
  SET_TESTS_PROPERTIES(pcre_test_bat PROPERTIES
  PASS_REGULAR_EXPRESSION "RunTest\\.bat tests successfully completed")

    IF("$ENV{OSTYPE}" STREQUAL "msys")
    # Both the sh and bat file versions of RunTest are run if make test is used
    # in msys

      ADD_TEST(pcre_test_sh    sh.exe ${PROJECT_BINARY_DIR}/pcre_test.sh)
      ADD_TEST(pcre_grep_test sh.exe ${PROJECT_BINARY_DIR}/pcre_grep_test.sh)

    ENDIF("$ENV{OSTYPE}" STREQUAL "msys")
  ENDIF(WIN32)

  # Changed to accommodate testing whichever location was just built

  IF(PCRE_SUPPORT_JIT)
  ADD_TEST(pcre_jit_test       pcre_jit_test)
  ENDIF(PCRE_SUPPORT_JIT)

  IF(PCRE_BUILD_PCRECPP)
    ADD_TEST(pcrecpp_test          pcrecpp_unittest)
    ADD_TEST(pcre_scanner_test     pcre_scanner_unittest)
    ADD_TEST(pcre_stringpiece_test pcre_stringpiece_unittest)

  ENDIF(PCRE_BUILD_PCRECPP)
ENDIF(PCRE_BUILD_TESTS)

# Installation
SET(CMAKE_INSTALL_ALWAYS 1)

INSTALL(TARGETS ${targets}
        RUNTIME DESTINATION bin
        LIBRARY DESTINATION lib
        ARCHIVE DESTINATION lib)

INSTALL(FILES ${PCRE_HEADERS} ${PCREPOSIX_HEADERS} DESTINATION include)

FILE(GLOB html ${PROJECT_SOURCE_DIR}/doc/html/*.html)
FILE(GLOB man1 ${PROJECT_SOURCE_DIR}/doc/*.1)
FILE(GLOB man3 ${PROJECT_SOURCE_DIR}/doc/*.3)

IF(PCRE_BUILD_PCRECPP)
        INSTALL(FILES ${PCRECPP_HEADERS} DESTINATION include)
ELSE(PCRE_BUILD_PCRECPP)
        # Remove pcrecpp.3
        FOREACH(man ${man3})
                GET_FILENAME_COMPONENT(man_tmp ${man} NAME)
                IF(NOT man_tmp STREQUAL "pcrecpp.3")
                        SET(man3_new ${man3} ${man})
                ENDIF(NOT man_tmp STREQUAL "pcrecpp.3")
        ENDFOREACH(man ${man3})
        SET(man3 ${man3_new})
ENDIF(PCRE_BUILD_PCRECPP)

INSTALL(FILES ${man1} DESTINATION man/man1)
INSTALL(FILES ${man3} DESTINATION man/man3)
INSTALL(FILES ${html} DESTINATION share/doc/pcre/html)

# help, only for nice output
IF(BUILD_SHARED_LIBS)
  SET(BUILD_STATIC_LIBS OFF)
ELSE(BUILD_SHARED_LIBS)
  SET(BUILD_STATIC_LIBS ON)
ENDIF(BUILD_SHARED_LIBS)

IF(PCRE_SHOW_REPORT)
  STRING(TOUPPER "${CMAKE_BUILD_TYPE}" buildtype)
  IF (CMAKE_C_FLAGS)
    SET(cfsp " ")
  ENDIF(CMAKE_C_FLAGS)
  IF (CMAKE_CXX_FLAGS)
    SET(cxxfsp " ")
  ENDIF(CMAKE_CXX_FLAGS)
  MESSAGE(STATUS "")
  MESSAGE(STATUS "")
  MESSAGE(STATUS "PCRE configuration summary:")
  MESSAGE(STATUS "")
  MESSAGE(STATUS "  Install prefix .................. : ${CMAKE_INSTALL_PREFIX}")
  MESSAGE(STATUS "  C compiler ...................... : ${CMAKE_C_COMPILER}")
  MESSAGE(STATUS "  C++ compiler .................... : ${CMAKE_CXX_COMPILER}")
  MESSAGE(STATUS "  C compiler flags ................ : ${CMAKE_C_FLAGS}${cfsp}${CMAKE_C_FLAGS_${buildtype}}")
  MESSAGE(STATUS "  C++ compiler flags .............. : ${CMAKE_CXX_FLAGS}${cxxfsp}${CMAKE_CXX_FLAGS_${buildtype}}")
  MESSAGE(STATUS "")
  MESSAGE(STATUS "  Build C++ library ............... : ${PCRE_BUILD_PCRECPP}")
  MESSAGE(STATUS "  Enable JIT compiling support .... : ${PCRE_SUPPORT_JIT}")
  MESSAGE(STATUS "  Enable UTF-8 support ............ : ${PCRE_SUPPORT_UNICODE_PROPERTIES}")
  MESSAGE(STATUS "  Unicode properties .............. : ${PCRE_SUPPORT_UNICODE_PROPERTIES}")
  MESSAGE(STATUS "  Newline char/sequence ........... : ${PCRE_NEWLINE}")
  MESSAGE(STATUS "  \\R matches only ANYCRLF ......... : ${PCRE_SUPPORT_BSR_ANYCRLF}")
  MESSAGE(STATUS "  EBCDIC coding ................... : ${PCRE_EBCDIC}")
  MESSAGE(STATUS "  Rebuild char tables ............. : ${PCRE_REBUILD_CHARTABLES}")
  MESSAGE(STATUS "  No stack recursion .............. : ${PCRE_NO_RECURSE}")
  MESSAGE(STATUS "  POSIX mem threshold ............. : ${PCRE_POSIX_MALLOC_THRESHOLD}")
  MESSAGE(STATUS "  Internal link size .............. : ${PCRE_LINK_SIZE}")
  MESSAGE(STATUS "  Match limit ..................... : ${PCRE_MATCH_LIMIT}")
  MESSAGE(STATUS "  Match limit recursion ........... : ${PCRE_MATCH_LIMIT_RECURSION}")
  MESSAGE(STATUS "  Build shared libs ............... : ${BUILD_SHARED_LIBS}")
  MESSAGE(STATUS "  Build static libs ............... : ${BUILD_STATIC_LIBS}")
  MESSAGE(STATUS "  Build pcregrep .................. : ${PCRE_BUILD_PCREGREP}")
  MESSAGE(STATUS "  Enable JIT in pcregrep .......... : ${PCRE_SUPPORT_PCREGREP_JIT}")
  MESSAGE(STATUS "  Buffer size for pcregrep ........ : ${PCREGREP_BUFSIZE}")
  MESSAGE(STATUS "  Build tests (implies pcretest  .. : ${PCRE_BUILD_TESTS}")
  MESSAGE(STATUS "               and pcregrep)")
  IF(ZLIB_FOUND)
    MESSAGE(STATUS "  Link pcregrep with libz ......... : ${PCRE_SUPPORT_LIBZ}")
  ELSE(ZLIB_FOUND)
    MESSAGE(STATUS "  Link pcregrep with libz ......... : None" )
  ENDIF(ZLIB_FOUND)
  IF(BZIP2_FOUND)
    MESSAGE(STATUS "  Link pcregrep with libbz2 ....... : ${PCRE_SUPPORT_LIBBZ2}")
  ELSE(BZIP2_FOUND)
    MESSAGE(STATUS "  Link pcregrep with libbz2 ....... : None" )
  ENDIF(BZIP2_FOUND)
  IF(NOT PCRE_SUPPORT_LIBREADLINE)
    MESSAGE(STATUS "  Link pcretest with libreadline .. : None" )
  ELSE(NOT PCRE_SUPPORT_LIBREADLINE)
    MESSAGE(STATUS "  Link pcretest with libreadline .. : ${PCRE_SUPPORT_LIBREADLINE}")
  ENDIF(NOT PCRE_SUPPORT_LIBREADLINE)
  IF(MINGW AND NOT PCRE_STATIC)
    MESSAGE(STATUS "  Non-standard dll names (prefix) . : ${NON_STANDARD_LIB_PREFIX}")
    MESSAGE(STATUS "  Non-standard dll names (suffix) . : ${NON_STANDARD_LIB_SUFFIX}")
  ENDIF(MINGW AND NOT PCRE_STATIC)
  MESSAGE(STATUS "")
ENDIF(PCRE_SHOW_REPORT)

# end CMakeLists.txt
