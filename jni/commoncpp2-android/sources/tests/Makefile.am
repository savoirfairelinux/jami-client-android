# Copyright (C) 1999-2005 Open Source Telecom Corporation.
# Copyright (C) 2006-2008 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

MAINTAINERCLEANFILES = Makefile.in Makefile

EXTRA_DIST = README dotests.sh test.sh output.txt vc6.mak Makefile.bcc
AM_CXXFLAGS = $(THREAD_FLAGS) @WARN_FLAGS@
INCLUDES = -I$(top_srcdir)/inc
LDADD = ../src/libccgnu2.la $(THREAD_LIBS) $(DYN_LOADER)
Z_LIBS = -lz

if WITH_CPPUNIT_TESTS
TEST_SUITE = ccxx_tests
else
TEST_SUITE =
endif

noinst_PROGRAMS = $(TEST_SUITE) \
	bug1 bug2 thread1 thread2 thread3 digest tcpstr1 url1 forever

noinst_HEADERS = SampleObject.h SampleSubObject.h Test_Date.h Test_Digest.h \
        Test_Engine.h Test_SHATumbler.h Test_TCPStream.h Test_URLString.h

if WITH_CPPUNIT_TESTS
ccxx_tests_SOURCES = ccxx_tests.cpp SampleObject.cpp SampleSubObject.cpp \
	Test_Date.cpp Test_Engine.cpp Test_TCPStream.cpp Test_URLString.cpp
# Test_Digest.cpp
ccxx_tests_LDADD = ../src/libccext2.la $(XML_LIBS) $(Z_LIBS) $(LDADD) \
	$(SSL_LIBS) $(CPPUNIT_LIBS)
endif

bug1_SOURCES = bug1.cpp

bug2_SOURCES = bug2.cpp

forever_SOURCES = forever.cpp

digest_SOURCES = digest.cpp
digest_LDADD = ../src/libccext2.la $(XML_LIBS) $(Z_LIBS) $(SSL_LIBS) $(LDADD)

thread1_SOURCES = thread1.cpp

thread2_SOURCES = thread2.cpp

thread3_SOURCES = thread3.cpp

tcpstr1_SOURCES = tcpstr1.cpp

url1_SOURCES = url1.cpp
url1_LDADD = ../src/libccext2.la $(XML_LIBS) $(Z_LIBS) $(SSL_LIBS) $(LDADD)
