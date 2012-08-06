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

MAINTAINERCLEANFILES = Makefile.in Makefile
EXTRA_DIST = ccgnu2-config.in ccgnu2-config ost_check2.m4 \
	getopt.h getopt.c getopt1.c *.pc *.pc.in
LT_VERSION = @LT_CCXX_VERSION@
AM_CXXFLAGS = -I../src -DCCXX_EXPORT_LIBRARY $(THREAD_FLAGS) \
        $(COMMON_FLAGS) -I$(CCXX_DIR) $(DEBUG_FLAGS)
AM_CFLAGS = $(THREAD_FLAGS)
INCLUDES = -I$(top_srcdir)/inc 
RELEASE = $(LT_VERSION) -release $(LT_RELEASE)

if EXTRAS
pkgconfig_DATA = libccgnu2.pc libccext2.pc
lib_LTLIBRARIES = libccgnu2.la libccext2.la
else
pkgconfig_DATA = libccgnu2.pc
lib_LTLIBRARIES = libccgnu2.la
endif

pkgconfigdir = $(libdir)/pkgconfig
aclocaldir = $(datadir)/aclocal
aclocal_DATA = ost_check2.m4
scriptdir = $(bindir)
script_DATA = ccgnu2-config

libccgnu2_la_LIBADD = @THREAD_LIBS@ @DYN_LOADER@ @SSL_LIBS@
libccgnu2_la_LDFLAGS = $(RELEASE) $(SHARED_FLAGS)
noinst_DATA = @STAGE2@

libccgnu2_la_SOURCES = thread.cpp mutex.cpp semaphore.cpp threadkey.cpp \
	friends.cpp event.cpp slog.cpp dir.cpp file.cpp inaddr.cpp \
	peer.cpp timer.cpp socket.cpp strchar.cpp simplesocket.cpp \
	mempager.cpp keydata.cpp dso.cpp exception.cpp missing.cpp \
	process.cpp string.cpp in6addr.cpp buffer.cpp lockfile.cpp \
	nat.cpp runlist.cpp assoc.cpp pointer.cpp linked.cpp map.cpp \
	cidr.cpp private.h nat.h

if EXTRAS
libccext2_la_LIBADD = @THREAD_LIBS@ @BASE_LIB@ @SSL_LIBS@ @ZSTREAM_LIBS@
libccext2_la_LDFLAGS = $(RELEASE) $(SHARED_FLAGS)

if GETOPT_LONG
optincludedir=$(includedir)/cc++2
optinclude_HEADERS = getopt.h
libccext2_la_SOURCES = numbers.cpp zstream.cpp socketport.cpp \
	url.cpp xml.cpp persist.cpp engine.cpp digest.cpp cmdoptns.cpp \
	date.cpp md5.cpp unix.cpp network.cpp serial.cpp urlstring.cpp \
	mime.cpp tokenizer.cpp ssl.cpp getopt.c getopt1.c applog.cpp

else

libccext2_la_SOURCES = numbers.cpp zstream.cpp socketport.cpp \
        url.cpp xml.cpp persist.cpp engine.cpp digest.cpp cmdoptns.cpp \
        date.cpp md5.cpp unix.cpp network.cpp serial.cpp urlstring.cpp \
	tokenizer.cpp mime.cpp ssl.cpp applog.cpp

endif
endif

# private.h and nat.h are internal headers

macosx: libccext2.la libccgnu2.la
	rm -rf .libs/*.dylib
	ld -r -o ccgnu2.lo $(libccgnu2_la_OBJECTS)
	../libtool --mode=link c++ -dynamic -dynamiclib -o libccgnu2.la \
		-rpath $(libdir) ccgnu2.lo $(LDFLAGS) -lpthread
	ld -r -o ccext2.lo $(libccext2_la_OBJECTS)
	../libtool --mode=link c++ -dynamic -dynamiclib -o libccext2.la \
		-rpath $(libdir) ccext2.lo $(LDFLAGS) -lpthread
	touch macosx

install-data-hook:
	chmod +x $(DESTDIR)${scriptdir}/ccgnu2-config

