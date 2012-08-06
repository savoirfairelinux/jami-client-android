
configdir=$(sysconfdir)/dbus-1

INCLUDES=-I$(top_srcdir) $(DBUS_BUS_CFLAGS) @PIE_CFLAGS@		\
	-DDBUS_SYSTEM_CONFIG_FILE=\""$(configdir)/system.conf"\"	\
	-DDBUS_COMPILATION

EFENCE=

CONFIG_IN_FILES=				\
	session.conf.in				\
	system.conf.in

config_DATA=					\
	session.conf				\
	system.conf

if DBUS_USE_LIBXML
XML_SOURCES=config-loader-libxml.c
endif
if DBUS_USE_EXPAT
XML_SOURCES=config-loader-expat.c
endif

if DBUS_BUS_ENABLE_KQUEUE
DIR_WATCH_SOURCE=dir-watch-kqueue.c
else
if DBUS_BUS_ENABLE_INOTIFY
DIR_WATCH_SOURCE=dir-watch-inotify.c
else
if DBUS_BUS_ENABLE_DNOTIFY_ON_LINUX
DIR_WATCH_SOURCE=dir-watch-dnotify.c
else
DIR_WATCH_SOURCE=dir-watch-default.c
endif
endif
endif

BUS_SOURCES=					\
	activation.c				\
	activation.h				\
	activation-exit-codes.h			\
	bus.c					\
	bus.h					\
	config-parser.c				\
	config-parser.h				\
	config-parser-common.c			\
	config-parser-common.h			\
	connection.c				\
	connection.h				\
	desktop-file.c				\
	desktop-file.h				\
	$(DIR_WATCH_SOURCE)			\
	dir-watch.h				\
	dispatch.c				\
	dispatch.h				\
	driver.c				\
	driver.h				\
	expirelist.c				\
	expirelist.h				\
	policy.c				\
	policy.h				\
	selinux.h				\
	selinux.c				\
	services.c				\
	services.h				\
	signals.c				\
	signals.h				\
	test.c					\
	test.h					\
	utils.c					\
	utils.h					\
	$(XML_SOURCES)

dbus_daemon_SOURCES=				\
	$(BUS_SOURCES)				\
	main.c

dbus_daemon_CPPFLAGS = -DDBUS_STATIC_BUILD
dbus_daemon_LDADD=					\
	$(top_builddir)/dbus/libdbus-internal.la	\
	$(EFENCE)					\
	$(DBUS_BUS_LIBS)

dbus_daemon_LDFLAGS=@R_DYNAMIC_LDFLAG@ @SECTION_LDFLAGS@ @PIE_LDFLAGS@

LAUNCH_HELPER_SOURCES=				\
	$(XML_SOURCES)				\
	config-parser-common.c			\
	config-parser-common.h			\
	config-parser-trivial.c			\
	config-parser-trivial.h			\
	desktop-file.c				\
	desktop-file.h				\
	utils.c					\
	utils.h					\
	activation-exit-codes.h			\
	activation-helper.h	  		\
	activation-helper.c

## This is the installed launch helper with the setuid checks
dbus_daemon_launch_helper_SOURCES=		\
	activation-helper-bin.c			\
	$(LAUNCH_HELPER_SOURCES)

dbus_daemon_launch_helper_CPPFLAGS = -DDBUS_STATIC_BUILD
dbus_daemon_launch_helper_LDADD=		\
	$(top_builddir)/dbus/libdbus-internal.la \
	$(DBUS_LAUNCHER_LIBS)

dbus_daemon_launch_helper_LDFLAGS=@R_DYNAMIC_LDFLAG@ @SECTION_LDFLAGS@

## we build another binary so we can do the launch testing without root privs.
## DO NOT INSTALL THIS FILE
dbus_daemon_launch_helper_test_SOURCES=		\
	activation-helper-bin.c			\
	$(LAUNCH_HELPER_SOURCES)

dbus_daemon_launch_helper_test_LDADD=		\
	$(top_builddir)/dbus/libdbus-internal.la \
	$(DBUS_LAUNCHER_LIBS)

dbus_daemon_launch_helper_test_LDFLAGS=@R_DYNAMIC_LDFLAG@ @SECTION_LDFLAGS@
dbus_daemon_launch_helper_test_CPPFLAGS= -DDBUS_STATIC_BUILD	\
	-DACTIVATION_LAUNCHER_TEST

## we build yet another binary so we can do the OOM tests
## DO NOT INSTALL THIS FILE
bus_test_launch_helper_SOURCES=		\
	test-launch-helper.c   		\
	$(LAUNCH_HELPER_SOURCES)

bus_test_launch_helper_LDADD=		\
	$(top_builddir)/dbus/libdbus-internal.la \
	$(DBUS_LAUNCHER_LIBS)

bus_test_launch_helper_LDFLAGS=@R_DYNAMIC_LDFLAG@ @SECTION_LDFLAGS@
bus_test_launch_helper_CPPFLAGS= -DDBUS_STATIC_BUILD	\
	-DACTIVATION_LAUNCHER_TEST	\
	-DACTIVATION_LAUNCHER_DO_OOM

extra_tests=
extra_noinst_programs=
extra_inst_programs=
if DBUS_UNIX
extra_tests+=bus-test-launch-helper
extra_noinst_programs+=dbus-daemon-launch-helper dbus-daemon-launch-helper-test dbus-daemon
endif
if DBUS_WIN
extra_inst_programs+=dbus-daemon
endif

## note that TESTS has special meaning (stuff to use in make check)
## so if adding tests not to be run in make check, don't add them to
## TESTS
if DBUS_BUILD_TESTS
TESTS_ENVIRONMENT=DBUS_TEST_DATA=$(top_builddir)/test/data DBUS_TEST_HOMEDIR=$(top_builddir)/dbus DBUS_FATAL_WARNINGS=1 DBUS_BLOCK_ON_ABORT=1
TESTS=bus-test bus-test-system $(extra_tests)
else
TESTS=
endif

## we use noinst_PROGRAMS not check_PROGRAMS so that we build
## even when not doing "make check"
noinst_PROGRAMS=$(TESTS) $(extra_noinst_programs)
bin_PROGRAMS=$(extra_inst_programs)

bus_test_system_SOURCES=			\
	$(XML_SOURCES)				\
	config-parser-common.c			\
	config-parser-common.h			\
	config-parser-trivial.c			\
	config-parser-trivial.h			\
	utils.c					\
	utils.h					\
	test-system.c

bus_test_system_CPPFLAGS = -DDBUS_STATIC_BUILD
bus_test_system_LDADD=$(top_builddir)/dbus/libdbus-internal.la $(DBUS_BUS_LIBS)
bus_test_system_LDFLAGS=@R_DYNAMIC_LDFLAG@

bus_test_SOURCES=				\
	$(BUS_SOURCES)				\
	test-main.c

bus_test_CPPFLAGS = -DDBUS_STATIC_BUILD
bus_test_LDADD=$(top_builddir)/dbus/libdbus-internal.la $(DBUS_BUS_LIBS)
bus_test_LDFLAGS=@R_DYNAMIC_LDFLAG@

## mop up the gcov files
clean-local:
	/bin/rm *.bb *.bbg *.da *.gcov || true

uninstall-hook:
	rm -f $(DESTDIR)$(DBUS_DAEMONDIR)/dbus-daemon$(EXEEXT)
	rm -f $(DESTDIR)$(libexecdir)/dbus-daemon-launch-helper$(EXEEXT)

install-data-hook:
	$(mkinstalldirs) $(DESTDIR)$(localstatedir)/run/dbus
	$(mkinstalldirs) $(DESTDIR)$(configdir)/system.d
	$(mkinstalldirs) $(DESTDIR)$(configdir)/session.d
	$(mkinstalldirs) $(DESTDIR)$(datadir)/dbus-1/services
	$(mkinstalldirs) $(DESTDIR)$(datadir)/dbus-1/system-services
if DBUS_UNIX
	if test '!' -d $(DESTDIR)$(DBUS_DAEMONDIR); then \
		$(mkinstalldirs) $(DESTDIR)$(DBUS_DAEMONDIR); \
		chmod 755 $(DESTDIR)$(DBUS_DAEMONDIR); \
	fi
	$(LIBTOOL) --mode=install $(INSTALL_PROGRAM) dbus-daemon$(EXEEXT) $(DESTDIR)$(DBUS_DAEMONDIR)
	$(mkinstalldirs) $(DESTDIR)$(libexecdir)/dbus-1
	if test -f dbus-daemon-launch-helper$(EXEEXT) ; then \
	$(LIBTOOL) --mode=install $(INSTALL_PROGRAM) dbus-daemon-launch-helper$(EXEEXT) $(DESTDIR)$(libexecdir); \
	if test `id -u` -eq 0; then \
		chown root:$(DBUS_USER) $(DESTDIR)$(libexecdir)/dbus-daemon-launch-helper$(EXEEXT); \
		chmod 4750 $(DESTDIR)$(libexecdir)/dbus-daemon-launch-helper$(EXEEXT); \
	else \
		echo "Not installing $(DESTDIR)$(libexecdir)/dbus-daemon-launch-helper binary setuid!"; \
		echo "You'll need to manually set permissions to root:$(DBUS_USER) and permissions 4750"; \
	fi \
	fi
endif
if HAVE_SYSTEMD
# Install dbus.socket as default implementation of a D-Bus stack
	$(mkinstalldirs) $(DESTDIR)$(systemdsystemunitdir)/dbus.target.wants
	$(LN_S) ../dbus.socket $(DESTDIR)$(systemdsystemunitdir)/dbus.target.wants/dbus.socket
# Unconditionally enable D-Bus on systemd installations
	$(mkinstalldirs) $(DESTDIR)$(systemdsystemunitdir)/sockets.target.wants
	$(LN_S) ../dbus.socket $(DESTDIR)$(systemdsystemunitdir)/sockets.target.wants/dbus.socket
	$(mkinstalldirs) $(DESTDIR)$(systemdsystemunitdir)/multi-user.target.wants
	$(LN_S) ../dbus.service $(DESTDIR)$(systemdsystemunitdir)/multi-user.target.wants/dbus.service
endif

#### Init scripts fun
SCRIPT_IN_FILES=messagebus.in \
		messagebus-config.in \
		rc.messagebus.in

## Red Hat start
if DBUS_INIT_SCRIPTS_RED_HAT

initddir=$(sysconfdir)/rc.d/init.d

initd_SCRIPTS= 	\
	messagebus

endif
 ## Red Hat end

## Slackware start
if DBUS_INIT_SCRIPTS_SLACKWARE

initddir=$(sysconfdir)/rc.d/

initd_SCRIPTS= 	\
	rc.messagebus

endif
## Slackware end

## Cygwin start
if DBUS_INIT_SCRIPTS_CYGWIN

bin_SCRIPTS= 	\
	messagebus-config

endif
## Cygwin end

if HAVE_SYSTEMD
SCRIPT_IN_FILES += \
	dbus.service.in \
	dbus.socket.in

systemdsystemunit_DATA = \
	dbus.service \
	dbus.socket
endif

MAN_IN_FILES=dbus-daemon.1.in
man_MANS = dbus-daemon.1

#### Extra dist

EXTRA_DIST=$(CONFIG_IN_FILES) $(SCRIPT_IN_FILES) $(man_MANS) $(MAN_IN_FILES)
