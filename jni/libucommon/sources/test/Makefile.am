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
AM_CXXFLAGS = -I$(top_srcdir)/inc $(UCOMMON_FLAGS) $(CHECKFLAGS)
LDADD = ../corelib/libucommon.la @UCOMMON_LIBS@ @UCOMMON_CLINK@
EXTRA_DIST = *.cpp keydata.conf CMakeLists.txt

TESTS = ucommonLinked ucommonSocket ucommonStrings ucommonThreads \
	ucommonMemory ucommonKeydata ucommonStream ucommonUnicode \
	ucommonQueue ucommonDatetime ucommonShell ucommonDigest ucommonCipher

noinst_PROGRAMS = demoSSL
demoSSL_SOURCES = ssl.cpp
demoSSL_LDFLAGS = @SECURE_LOCAL@

check_PROGRAMS = $(TESTS)

testing:	$(TESTS)

ucommonThreads_SOURCES = thread.cpp
ucommonStrings_SOURCES = string.cpp
ucommonLinked_SOURCES = linked.cpp
ucommonSocket_SOURCES = socket.cpp
ucommonMemory_SOURCES = memory.cpp
ucommonStream_SOURCES = stream.cpp
ucommonKeydata_SOURCES = keydata.cpp
ucommonUnicode_SOURCES = unicode.cpp
ucommonDatetime_SOURCES = datetime.cpp
ucommonQueue_SOURCES = queue.cpp
ucommonShell_SOURCES = shell.cpp
ucommonDigest_SOURCES = digest.cpp
ucommonDigest_LDFLAGS = @SECURE_LOCAL@
ucommonCipher_SOURCES = cipher.cpp
ucommonCipher_LDFLAGS = @SECURE_LOCAL@

# test using full stdc++ linkage...
stdcpp:	stdcpp.cpp
	$(CXX) @UCOMMON_FLAGS@ -o stdcpp -I../inc -L../src/.libs -lucommon stdcpp.cpp @UCOMMON_LIBS@
