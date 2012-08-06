if ENABLE_GLIB

lib_LTLIBRARIES = \
	libdbus-c++-glib-1.la

libdbus_c___glib_1_la_SOURCES = \
	glib-integration.cpp

libdbus_c___glib_1_la_CXXFLAGS = \
	-I$(top_srcdir)/include \
	-Wno-unused-parameter

libdbus_c___glib_1_la_LIBADD = \
	$(dbus_LIBS) \
	$(glib_LIBS)

AM_CPPFLAGS = \
	$(dbus_CFLAGS) \
	$(glib_CFLAGS)

HEADER_DIR = $(top_srcdir)/include/dbus-c++
libdbus_c___glib_1_HEADERS = \
	$(HEADER_DIR)/glib-integration.h

libdbus_c___glib_1dir = \
	$(includedir)/dbus-c++-1/dbus-c++/

EXTRA_DIST = \
	$(libdbus_c___glib_1_HEADERS)

endif

## File created by the gnome-build tools

