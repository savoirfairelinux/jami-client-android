# Copyright (C) 1999-2005 Open Source Telecom Corporation.
# Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

AUTOMAKE_OPTIONS = no-dependencies dist-shar
LT_VERSION=@LT_CCXX_VERSION@
#ignored for programs
#$(LT_VERSION) -release $(LT_RELEASE)
AM_CXXFLAGS = @WARN_FLAGS@ -I$(top_srcdir)/src
ccxxincludedir=$(includedir)/cc++

if SRTP_GCRYPT
srtp_src = ccsrtptest
ccsrtptest_SOURCES = ccsrtptest.cpp
ccsrtptest_LDFLAGS = ../src/libccrtp1.la
endif

if SRTP_OPENSSL
srtp_src = ccsrtptest
ccsrtptest_SOURCES = ccsrtptest.cpp
ccsrtptest_LDFLAGS = ../src/libccrtp1.la
endif

noinst_PROGRAMS = rtpsend rtplisten rtphello rtpduphello audiorx audiotx \
	ccrtptest $(srtp_src)

rtpsend_SOURCES = rtpsend.cpp
rtpsend_LDFLAGS = ../src/libccrtp1.la

rtplisten_SOURCES = rtplisten.cpp
rtplisten_LDFLAGS = ../src/libccrtp1.la

rtphello_SOURCES = rtphello.cpp
rtphello_LDFLAGS = ../src/libccrtp1.la

rtpduphello_SOURCES = rtpduphello.cpp
rtpduphello_LDFLAGS = ../src/libccrtp1.la

audiorx_SOURCES = audiorx.cpp audio.h
audiorx_LDFLAGS = ../src/libccrtp1.la

audiotx_SOURCES = audiotx.cpp
audiotx_LDFLAGS = ../src/libccrtp1.la

ccrtptest_SOURCES = ccrtptest.cpp
ccrtptest_LDFLAGS = ../src/libccrtp1.la
