# Copyright (C) 2000-2002 Open Source Telecom Corporation.
#  
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without 
# modifications, as long as this notice is preserved.
# 
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

AUTOMAKE_OPTIONS = no-dependencies
EXTRA_DIST = srcmodel.txt srcmodel.dia Doxyfile Doxymini

info_TEXINFOS = ccrtp.texi
ccrtp_TEXINFOS = gpl.texi fdl.texi fdlnotice.texi

if DOXY
noinst_DATA = doxy

doxy:   Doxyfile
	(cd @srcdir@ ; $(DOXYGEN) Doxymini)
	touch doxy

endif


