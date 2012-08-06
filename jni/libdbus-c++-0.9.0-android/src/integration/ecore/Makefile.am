if ENABLE_ECORE

lib_LTLIBRARIES = \
	libdbus-c++-ecore-1.la

libdbus_c___ecore_1_la_SOURCES = \
	ecore-integration.cpp

libdbus_c___ecore_1_la_CXXFLAGS = \
	-I$(top_srcdir)/include \
	-Wno-unused-parameter

libdbus_c___ecore_1_la_LIBADD = \
	$(dbus_LIBS) \
	$(ecore_LIBS)

AM_CPPFLAGS = \
	$(dbus_CFLAGS) \
	$(ecore_CFLAGS)

HEADER_DIR = $(top_srcdir)/include/dbus-c++
libdbus_c___ecore_1_HEADERS = \
	$(HEADER_DIR)/ecore-integration.h

libdbus_c___ecore_1dir = \
	$(includedir)/dbus-c++-1/dbus-c++/

EXTRA_DIST = \
	$(libdbus_c___ecore_1_HEADERS)

endif

## File created by the gnome-build tools

