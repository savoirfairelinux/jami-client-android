# Copyright (C) 2002 Open Source Telecom Corporation.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

AUTOMAKE_OPTIONS = no-dependencies dist-shar
ccxxincludedir=$(includedir)/ccrtp/crypto

noinst_HEADERS = AesSrtp.h hmac.h macSkein.h skein.h skein_iv.h skein_port.h skeinApi.h \
		brg_endian.h brg_types.h

kdoc_headers = AesSrtp.h hmac.h

kdoc:
	kdoc -f html -d $(KDOC_DIR) -L$(KDOC_DIR) -n ccrtp $(kdoc_headers)
