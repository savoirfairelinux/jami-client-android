# Copyright (C) 1999-2005 Open Source Telecom Corporation.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

MAINTAINERCLEANFILES = Makefile.in Makefile
EXTRA_DIST = Doxyfile Doxyfile.in

info_TEXINFOS = commoncpp2.texi
commoncpp2_TEXINFOS = gpl.texi fdl.texi fdlnotice.texi

if DOXY
noinst_DATA = doxy

doxy:   Doxyfile
	$(DOXYGEN) Doxyfile
	touch doxy

endif

clean-local:
	-rm -rf html
	-rm -rf latex
	-rm -rf man3

