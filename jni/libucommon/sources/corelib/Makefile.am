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
RELEASE = -version-info $(LT_VERSION) 
AM_CXXFLAGS = -I$(top_srcdir)/inc $(UCOMMON_FLAGS)

lib_LTLIBRARIES = libucommon.la 

libucommon_la_LDFLAGS = @UCOMMON_LIBS@ @UCOMMON_CLINK@ $(RELEASE) 
libucommon_la_SOURCES = object.cpp linked.cpp string.cpp mapped.cpp \
	counter.cpp bitmap.cpp timer.cpp memory.cpp socket.cpp access.cpp \
	thread.cpp fsys.cpp cpr.cpp vector.cpp xml.cpp stream.cpp persist.cpp \
	keydata.cpp numbers.cpp datetime.cpp unicode.cpp atomic.cpp file.cpp \
	regex.cpp protocols.cpp containers.cpp tcpbuffer.cpp shell.cpp

