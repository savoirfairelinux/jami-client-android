SUBDIRS=dbus bus doc tools test
DIST_SUBDIRS=dbus bus doc tools test

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = dbus-1.pc

DISTCLEANFILES = 		\
	dbus-1.pc

EXTRA_DIST =			\
	HACKING			\
	dbus-1.pc.in		\
	cleanup-man-pages.sh    \
        ChangeLog.pre-1-0       \
        NEWS.pre-1-0            \
	ChangeLog.pre-1-2       \
        NEWS.pre-1-2		\
	README.windbus

all-local: Doxyfile

if DBUS_GCOV_ENABLED
clean-gcov:
	find -name "*.da" -o -name "*.gcov" | xargs rm || true

clean-bbg:
	find -name "*.bbg" -o -name "*.bb" | xargs rm || true

GCOV_DIRS=dbus bus

## .PHONY so it always rebuilds it
.PHONY: coverage-report.txt
coverage-report.txt:
	BBG_FILES=`find $(GCOV_DIRS) -name "*.bbg" -o -name "*.gcno"` ;			\
	C_FILES= ;									\
	for F in $$BBG_FILES ; do							\
		F_nolibs=`echo $$F | sed -e 's/.libs\///g'` ;				\
		C=`echo $$F_nolibs | sed -e 's/.bbg/.c/g' | sed -e 's/.gcno/.c/g'`  ;	\
		B=`basename $$F .bbg` ;							\
		D=`dirname $$F` ;							\
		DA=`echo $$F | sed -e 's/.bbg/.da/g'` ;					\
		DA_libs=`echo $$D/.libs/$$B/.da` ;					\
		if test -e $$DA || test -e $$DA_libs; then				\
			C_FILES="$$C_FILES $$C" ;					\
		fi ;									\
	done ;										\
	echo $$C_FILES ;								\
	$(top_builddir)/test/decode-gcov --report $$C_FILES > coverage-report.txt

check-coverage: clean-gcov all check coverage-report.txt
	cat coverage-report.txt

else
coverage-report.txt:
	echo "Need to reconfigure with --enable-gcov"

check-coverage:
	echo "Need to reconfigure with --enable-gcov"

endif

update-authors:
	git shortlog -s -e | cut -c 8- | sort > AUTHORS

DISTCHECK_CONFIGURE_FLAGS = \
	--with-systemdsystemunitdir=$$dc_install_base/$(systemdsystemunitdir)
