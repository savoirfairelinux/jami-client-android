# Copyright (C) 2000-2005 Open Source Telecom Corporation.
# Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

ACLOCAL_AMFLAGS = -I m4
EXTRA_DIST = autogen.sh ccrtp.spec ccrtp.spec.in COPYING.addendum \
    ccrtp.list ccrtp.list.in libccrtp.pc libccrtp.pc.in autoconf/* \
    ccrtp-config.h directive.in

SUBDIRS = src demo doc
DIST_SUBDIRS = m4 src phone doc demo w32

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = libccrtp.pc

dist-hook:  cleandist

cleandist:
	@rm -f ${PACKAGE}-[0-9]*.tar.gz
	@rm -f ${PACKAGE}-[0-9]*.tar.bz2
	@rm -f ${PACKAGE}-[0-9]*.shar.gz
	@rm -f ${PACKAGE}-[0-9]*.zip
	@rm -f *${PACKAGE}*.rpm
	@rm -f *${PACKAGE}*.deb
	@rm -f ${PACKAGE}_*.gz
	@rm -f ${PACKAGE}_*.dsc
	@rm -f *.changes

