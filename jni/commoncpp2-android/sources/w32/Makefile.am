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

SUBDIRS=vs2008

EXTRA_DIST =  cc++/config.h \
	common.dsw ccgnu2.dsp ccext2.dsp common.sln *.vcproj demo/*.vcproj \
	tests/*.dsp demo/*.dsp Makefile.gcc Makefile.bcc common.reg *.bat

INCLUDES = -I$(CCXX_DIR)

# freddy77, now use posix dirs, gnuwin32 not tested
#lib_LTLIBRARIES = libccxx.la libccio.la

#libccxx_la_SOURCES = inaddr.cpp socket.cpp peer.cpp mutex.cpp event.cpp \
#	thread.cpp buffer.cpp threadkey.cpp url.cpp config.cpp

#libccio_la_SOURCES = pipe.cpp file.cpp

#libccxx_la_LDFLAGS = -lwsock32

#pkginclude_HEADERS = thread.h socket.h common.h file.h url.h
#noinst_HEADERS = export.h

