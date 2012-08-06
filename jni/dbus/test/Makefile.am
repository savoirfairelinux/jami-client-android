## the "name-test" subdir in fact contains a bunch of tests now that need a temporary bus 
## to be running to do stuff with. The directory should be renamed.
## We want to build the current directory first to pick up the testutils lib
SUBDIRS= . name-test 
DIST_SUBDIRS=name-test

INCLUDES=-I$(top_srcdir) $(DBUS_TEST_CFLAGS) 

libdbus_testutils_la_SOURCES = test-utils.h test-utils.c

noinst_LTLIBRARIES = libdbus-testutils.la

if DBUS_BUILD_TESTS
## break-loader removed for now
## most of these binaries are used in tests but are not themselves tests
TEST_BINARIES=test-service test-names test-shell-service shell-test spawn-test test-segfault test-exit test-sleep-forever

## these are the things to run in make check (i.e. they are actual tests)
## (binaries in here must also be in TEST_BINARIES)
TESTS=shell-test
else
TEST_BINARIES=
TESTS=
endif

if DBUS_GCOV_ENABLED
GCOV_BINARIES=decode-gcov
else
GCOV_BINARIES=
endif

noinst_PROGRAMS= $(TEST_BINARIES) $(GCOV_BINARIES)

test_service_SOURCES=				\
	test-service.c

test_names_SOURCES=				\
	test-names.c

##break_loader_SOURCES=				\
##	break-loader.c

test_shell_service_SOURCES =			\
	test-shell-service.c

shell_test_SOURCES=                             \
        shell-test.c

spawn_test_SOURCES=				\
	spawn-test.c

test_exit_SOURCES =				\
	test-exit.c

test_segfault_SOURCES =				\
	test-segfault.c

test_sleep_forever_SOURCES =			\
	test-sleep-forever.c

decode_gcov_SOURCES=				\
	decode-gcov.c

# When any programs are not linked to libdbus-internal, fix this.
AM_CPPFLAGS=-DDBUS_STATIC_BUILD
TEST_LIBS=$(top_builddir)/dbus/libdbus-internal.la $(DBUS_TEST_LIBS)

test_service_LDADD=libdbus-testutils.la $(TEST_LIBS)
test_service_LDFLAGS=@R_DYNAMIC_LDFLAG@
test_names_LDADD=libdbus-testutils.la $(TEST_LIBS)
test_names_LDFLAGS=@R_DYNAMIC_LDFLAG@
## break_loader_LDADD= $(TEST_LIBS)
## break_loader_LDFLAGS=@R_DYNAMIC_LDFLAG@
test_shell_service_LDADD=libdbus-testutils.la $(TEST_LIBS)
test_shell_service_LDFLAGS=@R_DYNAMIC_LDFLAG@
shell_test_LDADD=libdbus-testutils.la $(TEST_LIBS)
shell_test_LDFLAGS=@R_DYNAMIC_LDFLAG@
spawn_test_LDADD=$(TEST_LIBS)
spawn_test_LDFLAGS=@R_DYNAMIC_LDFLAG@
decode_gcov_LDADD=$(TEST_LIBS)
decode_gcov_LDFLAGS=@R_DYNAMIC_LDFLAG@

EXTRA_DIST=

## keep these in creation order, i.e. uppermost dirs first 
TESTDIRS=					\
	data					\
	data/valid-messages			\
	data/invalid-messages			\
	data/incomplete-messages		\
	data/auth				\
	data/sha-1				\
	data/valid-config-files			\
	data/valid-config-files-system		\
	data/valid-config-files/basic.d		\
	data/valid-config-files/session.d	\
	data/valid-config-files/system.d	\
	data/valid-service-files		\
	data/valid-service-files-system		\
	data/invalid-service-files-system	\
	data/invalid-config-files		\
	data/invalid-config-files-system	\
	data/equiv-config-files			\
	data/equiv-config-files/basic		\
	data/equiv-config-files/basic/basic.d	\
	data/equiv-config-files/entities	\
	data/equiv-config-files/entities/basic.d


FIND_TESTS=find . -type f -a \( -name "*.message" -o -name "*.message-raw" -o -name "*.auth-script" -o -name "*.sha1" -o -name "*.txt" -o -name "*.conf" -o -name "*.service" \)

dist-hook:
	for D in $(TESTDIRS); do						\
		test -d $(distdir)/$$D || mkdir $(distdir)/$$D || exit 1 ;	\
	done ;									\
	FILES=`(cd $(srcdir) && $(FIND_TESTS) -o -name "*.in" -a -not -name Makefile.in | grep -Ev "(.svn|CVS)" )` ;					\
        for F in $$FILES; do							\
                echo '-- Disting file '$$F ;					\
		cp -f $(srcdir)/$$F $(distdir)/$$F || exit 1 ;				\
	done

## copy tests to builddir so that generated tests and static tests 
## are all in one place.
all-local:
	for D in $(TESTDIRS); do								\
		test -d $(top_builddir)/test/$$D || mkdir $(top_builddir)/test/$$D || exit 1 ;	\
	done ;											\
	if ! (test $(srcdir) = . || test $(srcdir) -ef .) ; then								\
		FILES=`(cd $(srcdir) && $(FIND_TESTS) | grep -Ev "(.svn|CVS)" )` ;					\
	        for F in $$FILES; do								\
			SRC=$(srcdir)/$$F ;							\
			DEST=$(top_builddir)/test/$$F ;						\
	                echo '-- Copying test file '$$F ;					\
			cp $$SRC $$DEST || exit 1 ;						\
			chmod u+w $$DEST || exit 1 ;						\
		done ;										\
	else											\
		echo '-- No need to copy test data as srcdir = builddir' ;			\
	fi ;											\
	echo '-- Copying' $(top_builddir)/bus/*.conf 'to test directory' ;			\
	cp $(top_builddir)/bus/*.conf $(top_builddir)/test/data/valid-config-files || exit 1 ;  \
	chmod u+w $(top_builddir)/test/data/valid-config-files/*.conf || exit 1

## this doesn't clean generated test data files when srcdir=builddir
clean-local:
	if test $(srcdir) != . ; then					\
		FILES=`(cd $(top_builddir)/test && $(FIND_TESTS) | grep -Ev "(.svn|CVS)" )` ;	\
	        for F in $$FILES; do					\
			DEST=$(top_builddir)/test/$$F ;			\
	                echo '-- Deleting test file '$$F ;		\
			rm $$DEST || exit 1 ;				\
		done ;							\
		REVERSEDIRS= ;						\
		for D in $(TESTDIRS); do				\
			REVERSEDIRS="$$D $$REVERSEDIRS" ;		\
		done ;							\
		for D in $$REVERSEDIRS; do				\
			rmdir $(top_builddir)/test/$$D || exit 1 ;	\
		done ;							\
	fi
