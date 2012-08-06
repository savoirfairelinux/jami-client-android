EXTRA_DIST = propsgs-introspect.xml

AM_CPPFLAGS = -I$(top_srcdir)/include 

noinst_PROGRAMS = propsgs-server propsgs-client

propsgs_server_SOURCES = propsgs-glue-adaptor.h propsgs-server.h propsgs-server.cpp
propsgs_server_LDADD = $(top_builddir)/src/libdbus-c++-1.la

propsgs_client_SOURCES = propsgs-glue-proxy.h propsgs-client.h propsgs-client.cpp
propsgs_client_LDADD = $(top_builddir)/src/libdbus-c++-1.la -lpthread

propsgs-glue-adaptor.h: propsgs-introspect.xml
	$(top_builddir)/tools/dbusxx-xml2cpp $^ --adaptor=$@ --

propsgs-glue-proxy.h: propsgs-introspect.xml
	$(top_builddir)/tools/dbusxx-xml2cpp $^ --proxy=$@ --

BUILT_SOURCES = propsgs-glue-adaptor.h propsgs-glue-proxy.h
CLEANFILES = $(BUILT_SOURCES)

dist-hook:
	cd $(distdir); rm -f $(BUILT_SOURCES)

MAINTAINERCLEANFILES = \
	Makefile.in
