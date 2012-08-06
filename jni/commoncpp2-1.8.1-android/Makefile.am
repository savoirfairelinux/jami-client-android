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

AUTOMAKE_OPTIONS = no-dependencies dist-shar dist-zip
ACLOCAL_AMFLAGS = -I m4
EXTRA_DIST = autogen.sh TODO commoncpp2.spec commoncpp2.spec.in \
	commoncpp2.list.in commoncpp2.lsm.in autoconf/* \
	commoncpp2.list COPYING.addendum INSTALL.w32 SUPPORT

MAINTAINERCLEANFILES = configure aclocal.m4 libtool Makefile.in Makefile \
	config/* commoncpp2.list CommonC++.spec INSTALL

DIST_SUBDIRS = src w32 m4 doc demo tests inc
if WIN32
SUBDIRS = inc src doc
else
if MSWIN32
SUBDIRS = inc w32 doc
else
SUBDIRS = inc src doc
endif
endif

macosx:
	(cd src ; make macosx)

darwin:
	(cd src ; make macosx)


