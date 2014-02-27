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

# additional include path necessary to compile the srtp crypto sources
AM_CXXFLAGS= @WARN_FLAGS@ -I$(top_srcdir)/src/ccrtp
AM_CPPFLAGS= @WARN_FLAGS@ -I$(top_srcdir)/src/ccrtp

# RELEASE = $(LT_VERSION) -release $(LT_RELEASE)
RELEASE = -version-info $(LT_VERSION)

lib_LTLIBRARIES = libccrtp.la

if SRTP_OPENSSL
srtp_src_o =    ccrtp/crypto/openssl/hmac.cpp \
        ccrtp/crypto/openssl/SrtpSymCrypto.cpp
endif

if SRTP_GCRYPT
srtp_src_g =    ccrtp/crypto/gcrypt/gcrypthmac.cpp \
        ccrtp/crypto/gcrypt/gcryptSrtpSymCrypto.cpp \
        ccrtp/crypto/gcrypt/InitializeGcrypt.cpp ccrtp/crypto/twofish.c
endif

skein_srcs= ccrtp/crypto/macSkein.cpp \
        ccrtp/crypto/skein.c \
        ccrtp/crypto/skein_block.c \
        ccrtp/crypto/skeinApi.c


SUBDIRS = ccrtp ccrtp/crypto

libccrtp_la_SOURCES = rtppkt.cpp rtcppkt.cpp source.cpp data.cpp incqueue.cpp \
    outqueue.cpp queue.cpp control.cpp members.cpp socket.cpp duplex.cpp pool.cpp \
    CryptoContext.cpp CryptoContextCtrl.cpp $(srtp_src_g) $(srtp_src_o) $(skein_srcs)

libccrtp_la_LDFLAGS = $(RELEASE)
libccrtp_la_LIBADD = @GNULIBS@

noinst_HEADERS = private.h
