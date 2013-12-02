# Copyright (C) 2002-2005 Open Source Telecom Corporation.
# Copyright (C) 2006-2008 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

AUTOMAKE_OPTIONS = no-dependencies dist-shar
ccxxincludedir=$(includedir)/ccrtp

ccxxinclude_HEADERS = base.h formats.h rtppkt.h rtcppkt.h sources.h channel.h \
	queuebase.h iqueue.h oqueue.h ioqueue.h cqueue.h ext.h rtp.h pool.h \
	CryptoContext.h

kdoc_headers = base.h formats.h rtppkt.h rtcppkt.h sources.h channel.h \
	queuebase.h iqueue.h oqueue.h ioqueue.h cqueue.h ext.h CryptoContext.h

kdoc:
	kdoc -f html -d $(KDOC_DIR) -L$(KDOC_DIR) -n ccrtp $(kdoc_headers)
