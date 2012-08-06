configdir=$(sysconfdir)/dbus-1

INCLUDES=-I$(top_srcdir) $(DBUS_CLIENT_CFLAGS) $(DBUS_X_CFLAGS) -DDBUS_LOCALEDIR=\"@EXPANDED_DATADIR@/locale\" -DDBUS_COMPILATION -DDBUS_MACHINE_UUID_FILE=\""$(localstatedir)/lib/dbus/machine-id"\"

extra_bin_programs=
if DBUS_UNIX
extra_bin_programs += dbus-cleanup-sockets dbus-uuidgen
endif

bin_PROGRAMS=dbus-launch dbus-send dbus-monitor $(extra_bin_programs)

dbus_send_SOURCES=				\
	dbus-print-message.c			\
	dbus-print-message.h			\
	dbus-send.c

dbus_monitor_SOURCES=				\
	dbus-monitor.c				\
	dbus-print-message.c			\
	dbus-print-message.h

if DBUS_WIN
dbus_launch_SOURCES=				\
	dbus-launch-win.c			\
	dbus-launch.h
else
dbus_launch_SOURCES=				\
	dbus-launch.c				\
	dbus-launch-x11.c			\
	dbus-launch.h
endif

dbus_cleanup_sockets_SOURCES=			\
	dbus-cleanup-sockets.c

dbus_uuidgen_SOURCES=				\
	dbus-uuidgen.c

dbus_send_LDADD= $(top_builddir)/dbus/libdbus-1.la $(DBUS_CLIENT_LIBS)
dbus_send_LDFLAGS=@R_DYNAMIC_LDFLAG@

dbus_monitor_LDADD= $(top_builddir)/dbus/libdbus-1.la $(DBUS_CLIENT_LIBS)
dbus_monitor_LDFLAGS=@R_DYNAMIC_LDFLAG@

dbus_uuidgen_LDADD= $(top_builddir)/dbus/libdbus-1.la $(DBUS_CLIENT_LIBS)
dbus_uuidgen_LDFLAGS=@R_DYNAMIC_LDFLAG@

dbus_launch_LDADD= $(DBUS_X_LIBS) $(DBUS_CLIENT_LIBS)
dbus_launch_LDFLAGS=@R_DYNAMIC_LDFLAG@

man_MANS = dbus-send.1 dbus-monitor.1 dbus-launch.1 dbus-cleanup-sockets.1 dbus-uuidgen.1
EXTRA_DIST = $(man_MANS) run-with-tmp-session-bus.sh strtoll.c strtoull.c
CLEANFILES = 				\
	run-with-tmp-session-bus.conf

#create the /var/lib/data directory for dbus-uuidgen
localstatelibdir = $(localstatedir)/lib/dbus
localstatelib_DATA =
