This directory contains configuration files for the cmake build system 

Requirements 
------------
- cmake version >= 2.4.4 see http://www.cmake.org 
- installed libxml2 or libexpat 

Building 
--------

unix
1. install cmake and libxml or libexpat 
2. get dbus sources 
3. mkdir dbus-build 
4. cd dbus-build 
5. cmake <dbus-src-root>/cmake or cmake -DDBUS_USE_EXPAT=on <dbus-src-root>/cmake in case libexpat should de used
5. make 
6. make install

win32-mingw
1. install cmake and libxml or libexpat in <ProgramDir>\gnuwin32
2. get dbus sources 
3. mkdir dbus-build 
4. cd dbus-build 
5. cmake -G "MinGW Makefiles" <dbus-src-root>/cmake
6. make 
7. make install

win32-msvc
1. install cmake and libxml or libexpat in <ProgramDir>\gnuwin32
2. get dbus sources 
3. mkdir dbus-build 
4. cd dbus-build 
5. cmake -G <msvc available target, see cmake --help for a list" <dbus-src-root>/cmake
6. make 
7. make install


Some build options (use -D<key>=<value> on command line)
------------------
    key                        description                            default value
    ---                        -----------                            -------------
DBUS_USE_EXPAT              "Use expat (== ON) or libxml2 (==OFF)         OFF
DBUS_DISABLE_ASSERTS        "Disable assertion checking"                  OFF
DBUS_BUILD_TESTS            "enable unit test code"                       ON
DBUS_ENABLE_ANSI            "enable -ansi -pedantic gcc flags"            OFF
DBUS_ENABLE_GCOV            "compile with coverage profiling 
                             instrumentation (gcc only)"                  OFF
DBUS_ENABLE_VERBOSE_MODE    "support verbose debug mode"                  ON
DBUS_DISABLE_CHECKS         "Disable public API sanity checking"          OFF
DBUS_INSTALL_SYSTEM_LIBS    "install required system libraries 
                             (mingw: libxml2, libiconv, mingw10)"         OFF
CMAKE_BUILD_TYPE            "build type (== debug) or (== release)        release

