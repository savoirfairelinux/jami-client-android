# Autojunk script for libdbus-c++

AC_PREREQ(2.59)
AC_INIT([libdbus-c++], 0.9.0, [andreas.volz@tux-style.com])

AM_INIT_AUTOMAKE(AC_PACKAGE_NAME, AC_PACKAGE_VERSION)
AM_CONFIG_HEADER([config.h])
AC_CONFIG_MACRO_DIR([m4])

AC_CANONICAL_HOST

AC_SUBST(PACKAGE_VERSION)


# Set configuration options

AC_ARG_ENABLE(ecore,
	AS_HELP_STRING([--enable-ecore],
		[enable ecore integration]),
	[enable_ecore=$enableval],
	[enable_ecore=yes]
)

AC_ARG_ENABLE(glib,
	AS_HELP_STRING([--enable-glib],
		[enable glib integration]),
	[enable_glib=$enableval],
	[enable_glib=yes]
)

AC_ARG_ENABLE(doxygen-docs,
	AS_HELP_STRING([--enable-doxygen-docs],
		[build DOXYGEN documentation (requires Doxygen)]),
	[enable_doxygen_docs=$enableval],
 	[enable_doxygen_docs=no]
)

AC_ARG_ENABLE(tests,
	AS_HELP_STRING([--enable-tests],
		[build tests]),
	[enable_tests=$enableval],
 	[enable_tests=yes]
)

AC_ARG_ENABLE(examples,
	AS_HELP_STRING([--enable-examples],
		[build examples]),
	[enable_examples=$enableval],
 	[enable_examples=yes]
)

AC_CHECK_FUNCS(clock_gettime, [], [
  AC_CHECK_LIB(rt, clock_gettime, [
    AC_DEFINE(HAVE_CLOCK_GETTIME, 1)
    RT_LIBS="-lrt"
  ])
])
AC_SUBST(RT_LIBS)

# Check for programs

AC_LANG_CPLUSPLUS

AC_PROG_CC
AC_PROG_CXX

CXX_FOR_BUILD=${CXX_FOR_BUILD-${CXX}}
AC_SUBST(CXX_FOR_BUILD)

AM_PROG_LIBTOOL

PKG_PROG_PKG_CONFIG


AC_MSG_CHECKING([whether $CXX supports symbol visibility])

vtest=`$CXX --help --verbose 2>&1 | grep fvisibility`

if test -n "$vtest"; then
	AC_MSG_RESULT(yes)

	AC_DEFINE(GCC_HASCLASSVISIBILITY, 1, [to enable hidden symbols])
	CXXFLAGS="$CXXFLAGS -fvisibility=hidden"
else
	AC_MSG_RESULT(no)
fi 


# Check for dependencies

DBUS_REQUIRED_VERSION=0.60
PKG_CHECK_MODULES(dbus, [dbus-1 >= $DBUS_REQUIRED_VERSION],,
	AC_MSG_ERROR([You need the DBus libraries (version 0.6 or better)]
	[http://www.freedesktop.org/wiki/Software_2fdbus])
)
AC_SUBST(dbus_CFLAGS)
AC_SUBST(dbus_LIBS)

DBUS_API_STABLE_VERSION=1.0.0
PKG_CHECK_EXISTS([dbus-1 < $DBUS_API_STABLE_VERSION],
	[AC_DEFINE(DBUS_API_SUBJECT_TO_CHANGE, , [unstable DBus])]
)

DBUS_THREADS_INIT_DEFAULT_VERSION=0.93
PKG_CHECK_EXISTS([dbus-1 >= $DBUS_THREADS_INIT_DEFAULT_VERSION],
	[AC_DEFINE(DBUS_HAS_THREADS_INIT_DEFAULT, , [dbus_threads_init_default (needs DBus >= 0.93)])]
)

DBUS_RECURSIVE_MUTEX_VERSION=0.95
PKG_CHECK_EXISTS([dbus-1 >= $DBUS_RECURSIVE_MUTEX_VERSION],
	[AC_DEFINE(DBUS_HAS_RECURSIVE_MUTEX, , [DBus supports recursive mutexes (needs DBus >= 0.95)])]
)


if test "$enable_glib" = "yes" ; then
PKG_CHECK_MODULES([glib], glib-2.0)
AC_SUBST(glib_CFLAGS)
AC_SUBST(glib_LIBS)
AM_CONDITIONAL(ENABLE_GLIB, test 1 = 1)
PKG_CHECK_MODULES([gtkmm], gtkmm-2.4,
	AM_CONDITIONAL(HAVE_GTKMM, test 1 = 1),
	AM_CONDITIONAL(HAVE_GTKMM, test 0 = 1)
)
AC_SUBST(gtkmm_CFLAGS)
AC_SUBST(gtkmm_LIBS)
else
AM_CONDITIONAL(ENABLE_GLIB, test 0 = 1)
AM_CONDITIONAL(HAVE_GTKMM, test 0 = 1)
fi

if test "$enable_ecore" = "yes" ; then
PKG_CHECK_MODULES([ecore], ecore)
AC_SUBST(ecore_CFLAGS)
AC_SUBST(ecore_LIBS)
AM_CONDITIONAL(ENABLE_ECORE, test 1 = 1)
else
AM_CONDITIONAL(ENABLE_ECORE, test 0 = 1)
fi

if test "$enable_tests" = "yes" ; then
AM_CONDITIONAL(ENABLE_TESTS, test 1 = 1)
else
AM_CONDITIONAL(ENABLE_TESTS, test 0 = 1)
fi

if test "$enable_examples" = "yes" ; then
AM_CONDITIONAL(ENABLE_EXAMPLES, test 1 = 1)
else
AM_CONDITIONAL(ENABLE_EXAMPLES, test 0 = 1)
fi

AC_CHECK_LIB([expat], XML_ParserCreate_MM,
   	[AC_CHECK_HEADERS(expat.h, have_expat=true, have_expat=false)],
	have_expat=false)

if ! $have_expat; then
	AC_MSG_ERROR([You need the eXpat xml parser]
		[http://expat.sourceforge.net/])
fi

xml_CFLAGS=
xml_LIBS=-lexpat

AC_SUBST(xml_CFLAGS)
AC_SUBST(xml_LIBS)

ACX_PTHREAD
if test x"$acx_pthread_ok" = xyes; then
	AC_DEFINE(HAVE_PTHREAD_H, 1, [Define to enable pthread support])
	AM_CONDITIONAL(HAVE_PTHREAD, test x"$acx_pthread_ok" = xyes)
fi

# Doxygen Documentation

AC_PATH_PROG(DOXYGEN, doxygen, no)

AC_MSG_CHECKING([whether to build Doxygen documentation])

if test "$DOXYGEN" = "no" ; then
	have_doxygen=no
else
	have_doxygen=yes
fi

if test "$enable_doxygen_docs" = "auto" ; then
	enable_doxygen_docs=no

	AC_MSG_RESULT(no)
fi

if test "$enable_doxygen_docs" = "yes" ; then
	if test "$have_doxygen" = "no"; then
		AC_MSG_ERROR([Building Doxygen docs explicitly required, but Doxygen not found])
	fi

	AC_MSG_RESULT(yes)
fi

AM_CONDITIONAL(DBUS_DOXYGEN_DOCS_ENABLED, test "$enable_doxygen_docs" = "yes")

# For the tools/, we need libdbus-c++ for the "build" architecture as well

AM_CONDITIONAL(CROSS_COMPILING, test "$cross_compiling" = "yes")

AC_ARG_WITH(build-libdbus-cxx,
	AS_HELP_STRING([--with-build-libdbus-cxx],
	[For cross compilation: path to libdbus-cxx which was compiled for the 'build' system.]),
	[ BUILD_LIBDBUS_CXX_DIR=${withval} ],
	[ BUILD_LIBDBUS_CXX_DIR="\$(top_builddir)" ]
)
AC_SUBST(BUILD_LIBDBUS_CXX_DIR)

# Save processed files

AC_OUTPUT(
	Makefile
        src/Makefile
        src/integration/Makefile
        src/integration/ecore/Makefile
        src/integration/glib/Makefile
        tools/Makefile
	test/Makefile
	test/generator/Makefile
	test/functional/Makefile
	test/functional/Test1/Makefile
	data/Makefile
	doc/Makefile
	doc/Doxyfile
	examples/Makefile
	examples/properties/Makefile
	examples/echo/Makefile
	examples/ecore/Makefile
	examples/hal/Makefile
	examples/glib/Makefile
	examples/echo_ecore/Makefile
	dbus-c++-1.pc
	dbus-c++-1-uninstalled.pc
	dbus-c++-ecore-1.pc
	dbus-c++-ecore-1-uninstalled.pc
	dbus-c++-glib-1.pc
	dbus-c++-glib-1-uninstalled.pc
	libdbus-c++.spec
)

echo
echo "Build configuration:"
echo "--------------------"
echo
echo "Mainloop integration support:"
echo "ecore ......................... $enable_ecore"
echo "glib........................... $enable_glib"
echo
echo "Optional build items:"
echo "build tests.................... $enable_tests"
echo "build examples................. $enable_examples"
echo "build doxygen documentation.... $enable_doxygen_docs"
echo "Cross Compiling activated...... $cross_compiling"
echo "PThread support found.......... $acx_pthread_ok"
echo

