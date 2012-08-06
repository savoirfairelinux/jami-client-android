SUBDIRS = src tools data doc examples  test 

if ENABLE_GLIB
PKG_CONFIG_GLIB = dbus-c++-glib-1.pc
endif

if ENABLE_ECORE
PKG_CONFIG_ECORE = dbus-c++-ecore-1.pc
endif

EXTRA_DIST = autogen.sh bootstrap libdbus-c++.spec.in

ACLOCAL_AMFLAGS = -I m4

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = dbus-c++-1.pc $(PKG_CONFIG_GLIB) $(PKG_CONFIG_ECORE)

MAINTAINERCLEANFILES = \
	configure \
	Makefile.in \
	aclocal.m4 \
	compile \
	config.guess \
	config.sub \
	depcomp \
	install-sh \
	ltmain.sh \
	mdate-sh \
	missing \
	mkinstalldirs \
	libdbus-c++.spec

