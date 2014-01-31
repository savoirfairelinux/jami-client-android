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
LIBTOOL = @LT_GENERIC@

lib_LTLIBRARIES = libcommoncpp.la

libcommoncpp_la_LDFLAGS = ../corelib/libucommon.la @UCOMMON_LIBS@ $(RELEASE)
libcommoncpp_la_SOURCES = strchar.cpp slog.cpp exception.cpp thread.cpp \
	address.cpp socket.cpp tcp.cpp udp.cpp dccp.cpp applog.cpp mime.cpp \
	process.cpp serial.cpp tokenizer.cpp file.cpp dso.cpp linked.cpp \
	pointer.cpp map.cpp

