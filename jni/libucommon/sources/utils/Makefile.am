# Copyright (C) 2010 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

MAINTAINERCLEANFILES = Makefile.in Makefile
AM_CXXFLAGS = -I$(top_srcdir)/inc @UCOMMON_FLAGS@
AM_CPPFLAGS = -I$(top_srcdir)/inc
LDADD = ../corelib/libucommon.la @UCOMMON_LIBS@ @UCOMMON_CLINK@
EXTRA_DIST = *.1

man_MANS = args.1 scrub-files.1 mdsum.1 zerofill.1 car.1 sockaddr.1 pdetach.1
bin_PROGRAMS = args scrub-files mdsum zerofill car sockaddr pdetach

args_SOURCES = args.cpp

sockaddr_SOURCES = sockaddr.cpp

pdetach_SOURCES = pdetach.cpp

scrub_files_SOURCES = scrub.cpp
scrub_files_LDFLAGS = @SECURE_LOCAL@

mdsum_SOURCES = mdsum.cpp
mdsum_LDFLAGS = @SECURE_LOCAL@

zerofill_SOURCES = zerofill.cpp
zerofill_LDFLAGS = @SECURE_LOCAL@

car_SOURCES = car.cpp
car_LDFLAGS = @SECURE_LOCAL@

