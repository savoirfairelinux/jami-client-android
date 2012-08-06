EXTRA_DIST = README echo-introspect.xml

AM_CPPFLAGS = -I$(top_srcdir)/include \
	$(ecore_CFLAGS)

if ENABLE_ECORE
noinst_PROGRAMS = echo-server
endif

echo_server_SOURCES = echo-server-glue.h echo-server.h echo-server.cpp
echo_server_LDADD = $(top_builddir)/src/integration/ecore/libdbus-c++-ecore-1.la \
	$(top_builddir)/src/libdbus-c++-1.la \
	$(ecore_LIBS)

echo-server-glue.h: echo-introspect.xml
	$(top_builddir)/tools/dbusxx-xml2cpp $^ --adaptor=$@

if ENABLE_ECORE
noinst_PROGRAMS += echo-client-mt
endif

echo_client_mt_SOURCES = echo-client-glue.h echo-client.h echo-client.cpp
echo_client_mt_LDADD = $(top_builddir)/src/integration/ecore/libdbus-c++-ecore-1.la \
	$(top_builddir)/src/libdbus-c++-1.la $(PTHREAD_LIBS) \
	$(ecore_LIBS)
echo_client_mt_CXXFLAGS = $(PTHREAD_CFLAGS)

echo-client-glue.h: echo-introspect.xml
	$(top_builddir)/tools/dbusxx-xml2cpp $^ --proxy=$@

BUILT_SOURCES = echo-server-glue.h echo-client-glue.h
CLEANFILES = $(BUILT_SOURCES)

dist-hook:
	cd $(distdir); rm -f $(BUILT_SOURCES)

MAINTAINERCLEANFILES = \
	Makefile.in
