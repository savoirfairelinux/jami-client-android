# Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

AUTOMAKE_OPTIONS = no-dependencies dist-zip
EXTRA_DIST = autogen.sh README* *.pc.in *.spec.in *.spec *-config.in \
    *-config.1 Doxyfile cmodel.sh BUILDS SUPPORT COPYING* COPYRIGHT \
    CMakeLists.txt ucommon-config.cmake ucommon.pc.cmake \
    commoncpp-config.cmake commoncpp.pc.cmake ucommon.spec.cmake \
    ucommon-config.h.cmake doxy.config.cmake directive.in

DIST_SUBDIRS = corelib commoncpp openssl gnutls nossl utils inc test
SUBDIRS = corelib @SECURE@ @COMPAT@ utils inc test

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = ucommon.pc commoncpp.pc

scriptdir = $(bindir)
script_SCRIPTS = ucommon-config commoncpp-config

man_MANS = ucommon-config.1 commoncpp-config.1

.phony:	doxy

doxy:
	-rm -rf doc
	$(DOXYGEN) Doxyfile

clean-local:
	-rm -rf doc

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

