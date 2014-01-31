
include_directories(${CMAKE_CURRENT_BINARY_DIR}/src ${CMAKE_SOURCE_DIR}/src ${CMAKE_CURRENT_SOURCE_DIR})

########### next target ###############

set(phone_SRCS
    phone.cpp
    config.cpp
    server.cpp
    rtp.cpp)

add_executable(phone ${phone_SRCS})

target_link_libraries(phone ccrtp1)

#install(TARGETS phone DESTINATION bin )


########### install files ###############

# install(FILES  phone.h DESTINATION include/ccrtp)

src_distribution_init()
add_src_dist_files(${phone_SRCS})

#original Makefile.am contents follow:

## Copyright (C) 1999-2005 Open Source Telecom Corporation.
## Copyright (C) 2006-2008 David Sugar, Tycho Softworks.
##  
## This file is free software; as a special exception the author gives
## unlimited permission to copy and/or distribute it, with or without 
## modifications, as long as this notice is preserved.
## 
## This program is distributed in the hope that it will be useful, but
## WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
## implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
#AUTOMAKE_OPTIONS = no-dependencies dist-shar
#LT_VERSION=@LT_CCXX_VERSION@
##$(LT_VERSION) -release $(LT_RELEASE)
#AM_CXXFLAGS = @WARN_FLAGS@ -I$(top_srcdir)/src
#ccxxincludedir=$(includedir)/ccrtp
#EXTRA_DIST = phone.conf
#
#noinst_HEADERS = server.h
#ccxxinclude_HEADERS = phone.h
#bin_PROGRAMS = phone
#
#phone_SOURCES = phone.cpp config.cpp server.cpp rtp.cpp
#phone_LDADD = ../src/libccrtp1.la @EXTLIBS@ @GNULIBS@
