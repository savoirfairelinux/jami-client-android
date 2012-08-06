include $(top_srcdir)/globals.mak

noinst_LTLIBRARIES = libsiplink.la

libsiplink_la_SOURCES = \
		sdp.cpp \
		sipaccount.cpp \
		sipcall.cpp \
		sipvoiplink.cpp \
		siptransport.cpp \
		sdp.h \
		sipaccount.h \
		sipcall.h \
		sipvoiplink.h \
		siptransport.h \
		sip_utils.cpp \
		sip_utils.h

if BUILD_SDES
libsiplink_la_SOURCES+= sdes_negotiator.cpp \
			sdes_negotiator.h \
			pattern.cpp \
			pattern.h


libsiplink_la_CXXFLAGS = \
		@PCRE_LIBS@
endif
