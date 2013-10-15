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

AUTOMAKE_OPTIONS = no-dependencies
EXTRA_DIST = $(M4)
MAINTAINERCLEANFILES = Makefile.in

M4 =	ost_prog.m4 ost_cxx.m4 ost_pthread.m4 ost_posix.m4 ost_systime.m4 \
	ost_maint.m4 ost_signal.m4 ost_socket.m4 ost_string.m4 ost_types.m4 \
	ost_win32.m4 ost_dynamic.m4 ost_poll.m4 ost_misc.m4 ost_endian.m4 \
	ost_debug.m4 ost_getopt.m4 ost_reentrant.m4 win32msc.m4 ost_stlport.m4 

SOURCES = $(M4)

../aclocal.m4: $(M4)
	cd .. ;	$(ACLOCAL) -I m4

all-local:	../aclocal.m4
	
