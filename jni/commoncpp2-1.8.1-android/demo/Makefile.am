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

EXTRA_DIST = README test.xml Makefile.bcc
AM_CXXFLAGS = $(THREAD_FLAGS) @WARN_FLAGS@ -Wno-deprecated
INCLUDES = -I$(top_srcdir)/inc
LDADD = ../src/libccgnu2.la $(THREAD_LIBS) $(DYN_LOADER)
Z_LIBS = -lz

noinst_PROGRAMS = tcp tcpthread tcpservice dccpclient dccpserver serial dir \
	str buffer keydump urlfetch xmlfetch portsample netdevices slogTest \
	crc32 cmdlineopt timer testAlog
noinst_HEADERS = serialecho.h SampleSocketPort.h

urlfetch_SOURCES = urlfetch.cpp
urlfetch_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

xmlfetch_SOURCES = xmlfetch.cpp
xmlfetch_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

buffer_SOURCES = buffer.cpp
buffer_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

cmdlineopt_SOURCES = cmdlineopt.cpp
cmdlineopt_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

slogTest_SOURCES = slogTest.cpp

dir_SOURCES = dir.cpp

keydump_SOURCES = keydump.cpp

str_SOURCES = str.cpp
str_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

tcp_SOURCES = tcp.cpp

tcpthread_SOURCES = tcpthread.cpp

dccpclient_SOURCES = dccpclient.cpp

dccpserver_SOURCES = dccpserver.cpp

timer_SOURCES = timer.cpp

tcpservice_SOURCES = tcpservice.cpp
tcpservice_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

portsample_SOURCES = SampleSocketPort.cpp
portsample_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

netdevices_SOURCES = netdevices.cpp
netdevices_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

serial_SOURCES = serialmain.cpp serialecho.cpp
serial_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

crc32_SOURCES = crc32.cpp
crc32_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

testAlog_SOURCES = test_alog.cpp 
testAlog_LDADD = ../src/libccext2.la $(SSL_LIBS) $(XML_LIBS) $(Z_LIBS) $(LDADD)

