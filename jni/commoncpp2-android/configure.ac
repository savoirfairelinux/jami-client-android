# Copyright (C) 1999-2005 Open Source Telecom Corporation.
# Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

AC_INIT(src/thread.cpp)
AC_CONFIG_MACRO_DIR([m4])
m4_pattern_allow(LT_VERSION)

VERSION="1.8.1"
LT_RELEASE="1.8"
LT_VERSION="0:1"

dnl this is a C++ library after all..
AC_LANG(C++)

# lsb build option

ccincludedir=""
ost_cv_thread_library="none"

COMMON_FLAGS="-D_GNU_SOURCE"

AC_ARG_WITH(lsb,           [  --with-lsb=name         Build lsb compliant package],[
    if test ! -d "$prefix" ; then
        prefix="/opt/$withval" ; fi
    if test ! -d "$sysconfdir" ; then
        sysconfdir="/etc$prefix" ; fi
    if test ! -d "$localstatedir" ; then
        localstatedir="/var$prefix" ; fi
    AC_SUBST(prefix)
    CC=/opt/lsbdev-cc/bin/lsbcc
    AC_SUBST(CC)
    CXX=/opt/lsbdev-cc/bin/lsbc++
    AC_SUBST(CXX)
    AC_SUBST(sysconfdir)
    AC_SUBST(localstatedir)
])

case "$prefix" in
/opt/*)
    if test "$datadir" == '${prefix}/share' ; then
        if test "$mandir" == '${datadir}/man' ; then
            mandir='${prefix}/man' ; fi
        if test "$infodir" == '${datadir}/info' ; then
            infodir='${prefix}/info' ; fi
        datadir='${prefix}'
    fi
    if test "$sysconfdir" == '${prefix}/etc' ; then
        sysconfdir=/etc${prefix} ; fi
    if test "$localstatedir" == '${prefix}/var' ; then
        localstatedir=/var${prefix} ; fi
    if test "$includedir" == '${prefix}/include' ; then
        ccincludedir="$includedir" ; fi
    AC_SUBST(mandir)
    AC_SUBST(infodir)
    AC_SUBST(sysconfdir)
    AC_SUBST(localstatedir)
    AC_SUBST(datadir)
    AC_SUBST(prefix)
    ;;
esac

if test -z "$ccincludedir" ; then
    case "$includedir" in
    */lib/*)
        ccincludedir='${includedir}'
        ;;
    *)
        ccincludedir='${includedir}/cc++2'
        ;;
    esac
fi

AC_SUBST(ccincludedir)

AC_CONFIG_AUX_DIR(autoconf)
AC_CANONICAL_SYSTEM
AC_PROG_CPP
AC_PROG_CC
AC_PROG_CXXCPP
AC_PROG_CXX
OST_PROG_CC_POSIX
NP_PROG_MSC
AC_LIBTOOL_WIN32_DLL
AM_PROG_LIBTOOL
AM_INIT_AUTOMAKE(commoncpp2, [$VERSION])
AM_CONFIG_HEADER(config.h)

AC_C_RESTRICT
AC_C_VOLATILE
AC_C_INLINE

AH_TOP(
#ifndef CCXX_CONFIG_H_
#define CCXX_CONFIG_H_
#define __DLL
#define __EXPORT_TEMPLATE(x)
#undef CCXX_EMPTY
#define CCXX_EMPTY

#define COMMON_64_CLEAN
#define COMMON_ASYNC_OVERRIDE
#define COMMON_OST_NAMESPACE
#define COMMON_THREAD_SLEEP
#define COMMON_NET_DEVICES
#define COMMON_THREAD_DEBUG
#define COMMON_DEADLOCK_DEBUG
#define COMMON_NAMED_MUTEX
#define COMMON_PROCESS_ATTACH
#define COMMON_XML_PARSING
#define COMMON_TIMER_SLEEP

#if __GNUC__ > 1 && !defined(__STRICT_ANSI__) && !defined(__PEDANTIC__)
#define DYNAMIC_LOCAL_ARRAYS
#endif

#if defined(__CYGWIN__)
#define _POSIX_REALTIME_SIGNALS
#define _POSIX_THREADS
#endif

#if defined(__APPLE__) && defined(__MACH__)
#ifndef MACOSX
#define MACOSX
#define _P1003_1B_VISIBLE
#endif
#ifndef _PTHREADS
#define _PTHREADS 1
#endif
#endif

#if defined(__FreeBSD__)
#ifndef __BSD_VISIBLE
#define __BSD_VISIBLE 1
#endif
#endif

#ifdef _AIX
#ifndef _ALL_SOURCE
#define _ALL_SOURCE
#endif
#endif

#ifdef  __hpux
#ifndef _XOPEN_SOURCE_EXTENDED
#define _XOPEN_SOURCE_EXTENDED
#endif
#ifndef _INCLUDE_LONGLONG
#define _INCLUDE_LONGLONG
#endif
#endif

#define CCXX_PACKING
#if defined(__GNUC__)
#define CCXX_PACKED
#elif !defined(__hpux) && !defined(_AIX)
#define CCXX_PACKED
#endif

#if defined(__sun) || defined(__SUN__)
#define __EXTENSIONS__ 1
#endif

#ifndef _REENTRANT
#define _REENTRANT 1
#endif

#ifndef _THREAD_SAFE
#define _THREAD_SAFE 1
#endif

#ifndef _GNU_SOURCE
#define _GNU_SOURCE 1
#endif

#if     !defined(_XOPEN_SOURCE) && !defined(__FreeBSD__) &&!defined(__OpenBSD__) && !defined(__MACH__) && !defined(__NetBSD__)
#define _XOPEN_SOURCE 600
#endif
)

if test "$prefix" != "/usr" ; then
    if test -d "$prefix" ; then
        bprefix="$prefix"
    else if test "$GCC" = "no" ; then
        bprefix=/usr/local ; fi
    fi
    if test -d $bprefix/include ; then
        COMMON_FLAGS="$COMMON_FLAGS -I$bprefix/include"
    fi
    if test -d $bprefix/lib ; then
        LIBS="$LIBS -L$bprefix/lib"
    fi
fi

OST_PROG_COMMON
OST_PROG_LIBVER
OST_PROG_LIBRARY(CCXX,[$LT_VERSION])
OST_AUTOMAKE_MODE
OST_MAINTAINER_MODE
OST_WINVER
OST_WIN32
OST_CC_FCNTL
OST_CC_SYSTIME
OST_CC_TYPES
OST_CC_ENDIAN
OST_CC_SIGNAL
OST_CC_STRING
OST_CC_GETOPT
OST_CC_DYNAMIC
OST_LIB_PTHREAD
OST_LIB_REENTRANT
OST_LIB_POLL
OST_SYS_SOCKET

ost_cv_ssl=no
AC_ARG_WITH(monotonic,   [  --without-monotonic     Disable monotonic clock],
    [],[AC_DEFINE(USE_MONOTONIC_TIMER, [1], [use monotonic])
])

AC_ARG_WITH(extras,      [  --without-extras        Disables extras lib],[
    OST_LIB_NOZLIB
    OST_LIB_NOXML
    ost_cv_extras=no
],[
    ost_cv_extras=yes
    AC_DEFINE(HAVE_EXTRAS, [1], [Enable extras])

    AC_ARG_WITH(gnutls, [  --with-gnutls         Enable gnutls support], [
    AC_CHECK_HEADER(gnutls/gnutls.h,[
        AC_CHECK_LIB(gnutls, gnutls_init, [
            SSL_LIBS="-lgnutls -lgcrypt"
            ost_cv_ssl=true
            AC_DEFINE(CCXX_SSL, [GNUTLS], [defines ssl])
            AC_DEFINE(CCXX_GNUTLS, [1], [define gnutls])
        ])
    ],[])
],[
    AC_ARG_WITH(openssl, [  --with-openssl       Enable openssl support],
        [OST_LIB_SSL],
        [OST_LIB_NOSSL])
    ])
    AC_ARG_WITH(compression, [  --without-compression   Disable libz compression],
    [OST_LIB_NOZLIB],
    [OST_LIB_ZLIB])
])

AC_ARG_WITH(memaudit,      [  --with-memaudit         Enable memory auditing],
    [AC_DEFINE(COMMON_MEMORY_AUDIT, [1], [enable auditing])])

AC_ARG_WITH(cppunit,      [  --with-cppunit          Build cppunit based test suite],[
    if test "$withval" != "yes"; then
        CPPUNIT_LIBS="-l$withval"
    else
        CPPUNIT_LIBS="-lcppunit"
    fi
],[
    CPPUNIT_LIBS=" "
])
AC_SUBST(CPPUNIT_LIBS)

AM_CONDITIONAL(WITH_CPPUNIT_TESTS, test "$CPPUNIT_LIBS" != " ")

AC_CHECK_HEADERS(sys/file.h sys/param.h sys/wait.h syslog.h syslog.hposix_evlog.h ss.h ioctl.h)
AC_CHECK_FUNCS(realpath lstat snprintf memmove strdup lockf waitpid wait4 gettimeofday)
AC_CHECK_FUNCS(posix_memalign setegid setpgrp getpagesize)

# C++ stuff must done after library and header
# (some C++ define require some header)
OST_CXX_PROGRAMMING

# allow build of library without exception handling, for use in
# dedicated targets, etc...

AC_ARG_WITH(exceptions, [  --without-exceptions    Disable exception handling],
    [OST_CXX_NOEXCEPTIONS],
    [OST_CXX_EXCEPTIONS]
)

OST_CXX_MUTABLE
OST_CXX_NAMESPACE
OST_CXX_IOSTREAM
OST_CXX_NEW_INIT
OST_SGI_STLPORT

# Are we using the GNU compiler?
if test "$GCC" = yes ; then
    WARN_FLAGS="-pedantic -Wall"
else
    WARN_FLAGS=""
fi
AC_SUBST(WARN_FLAGS)

OST_DEBUG

if test $ost_cv_gnuwin32 = yes ; then
    CCXX_DIR="\$(top_srcdir)/w32"
else
    if test $np_cv_prog_msc = yes ; then
        CCXX_DIR="\$(top_srcdir)/w32"
    else
        CCXX_DIR="\$(top_srcdir)/inc"
    fi
fi
KDOC_DIR="\$(top_srcdir)/doc"
AC_SUBST(LT_RELEASE)
AC_SUBST(KDOC_DIR)
AC_SUBST(CCXX_DIR)
AC_SUBST(incprefix)
AM_CONDITIONAL(WIN32, test $ost_cv_gnuwin32 = yes)
AM_CONDITIONAL(EXTRAS, test $ost_cv_extras = yes)
AC_SUBST(ost_cv_dynloader)

# some peculiar things needed for cygwin dll builds and the currently broken toolchain...

SHARED_FLAGS=""
MODULE_FLAGS="-module -shared -avoid-version"
STAGE2=""
BASE_LIB=""
case "$target_os" in
osf*)
    COMMON_FLAGS="$COMMON_FLAGS -D_POSIX_C_SOURCE=1 -D_OSF_SOURCE=1 -D__USE_STD_IOSTREAM"
    ;;
cygwin*|mingw*)
    BASE_LIB="../src/libccgnu2.la $XML_LIBS $ZSTREAM_LIBS $SSL_LIBS"
    AC_DEFINE(CYGWIN_IMPORTS, [1], [cygwin environment])
    SHARED_FLAGS="-no-undefined"
    MODULE_FLAGS="-module -shared -avoid-version -no-undefined"
    ;;
darwin6*)
    MODULE_FLAGS="-dynamic -bundle -undefined suppress -flat_namespace -read_only_relocs suppress"
    STAGE2="macosx"
    AC_DEFINE(_DARWIN6_, [1], [darwin6 environment])
    ;;
darwin*)
    MODULE_FLAGS="-dynamic -bundle -undefined suppress -flat_namespace -read_only_relocs suppress"
    ;;
linux*)
    BASE_LIB="../src/libccgnu2.la"
    ;;
esac

AC_SUBST(COMMON_FLAGS)
AC_SUBST(SHARED_FLAGS)
AC_SUBST(MODULE_FLAGS)
AC_SUBST(BASE_LIB)
AC_SUBST(STAGE2)

AC_SUBST(sysconfdir)
AC_SUBST_DIR(etc_confdir, sysconfdir)

if test "$sysconfdir" != '${prefix}/etc' ; then
    AC_DEFINE_UNQUOTED(ETC_PREFIX, "$etc_confdir/", [system config prefix])
elif test "$sysconfdir" != '/etc' ; then
    AC_DEFINE_UNQUOTED(ETC_CONFDIR, "$etc_confdir/", [primary config prefix])
fi

AH_BOTTOM([

#ifdef  HAVE_SS_H
#include <ss.h>
#define COMMON_SECURE
#endif

#define COMMON_NAMESPACE    ost
#define NAMESPACE_COMMON    namespace ost {
#define END_NAMESPACE       }

#ifdef  HAVE_VISIBILITY
#define __EXPORT __attribute__ ((visibility("default")))
#define __DLLRTL __attribute__ ((visibility("default")))
#define __LOCAL  __attribute__ ((visibility("hidden")))
#else
#define __EXPORT
#define __DLLRTL
#define __LOCAL
#endif

#ifndef ETC_PREFIX
#ifdef  WIN32
#define ETC_PREFIX "C:\\WINDOWS\\"
#endif

#ifndef ETC_PREFIX
#define ETC_PREFIX "/etc/"
#endif
#endif

#endif

])

AC_PATH_PROG(DOXYGEN, doxygen, no)
AC_SUBST(DOXYGEN)
AM_CONDITIONAL(DOXY, test "$DOXYGEN" != "no")

AM_CONDITIONAL(GETOPT_LONG, [test ! -z "$LIBGETOPTOBJS"])

LIB_VERSION=`echo $LT_RELEASE | sed -e 's/[\.]/_/'`
LIB_MAJOR=`echo $LT_VERSION | sed -e 's/[:].*$//'`

AC_SUBST(LIB_VERSION)
AC_SUBST(LIB_MAJOR)

AC_OUTPUT([src/ccgnu2-config src/libccext2.pc src/libccgnu2.pc
src/Makefile w32/Makefile w32/vs2008/Makefile w32/vs2008/ccext2.vcproj w32/vs2008/ccgnu2.vcproj w32/vs2008/common.sln m4/Makefile doc/Doxyfile
doc/Makefile demo/Makefile inc/Makefile inc/cc++/Makefile Makefile
commoncpp2.spec tests/Makefile commoncpp2.list w32/ccgnu2.dsp w32/ccext2.dsp w32/ccgnu2.vcproj w32/ccext2.vcproj])

# if test ! -f inc/cc++/thread.h ; then
#   cp ${srcdir}/inc/cc++/*.h inc/cc++ ; fi

cd inc
rm -f config.tmp
cp ../config.h config.tmp
sed -e s!"@thrprefix@"!"$thrprefix"! -e s!"@USR_PREFIX@"!"$prefix"! \
    -e s!PACKAGE!CCXX_PACKAGE! -e s!VERSION!CCXX_VERSION! <config.tmp >cc++/config.h
cd ..
#
# Visual Studio
#
DLLVERSION=`echo "$LT_RELEASE" | sed -e 's/\.//'g`
#
# Visual Studio 2008
#
rm -f w32/vs2008/ccext2.vcproj.tmp w32/vs2008/ccgnu2.vcproj.tmp
cp -p w32/vs2008/ccext2.vcproj w32/vs2008/ccext2.vcproj.tmp
cp -p w32/vs2008/ccgnu2.vcproj w32/vs2008/ccgnu2.vcproj.tmp
sed -e s/VCVERSION/$VERSION/g < w32/vs2008/ccext2.vcproj.tmp > w32/vs2008/ccext2.vcproj
sed -e s/VCVERSION/$VERSION/g < w32/vs2008/ccgnu2.vcproj.tmp > w32/vs2008/ccgnu2.vcproj

cp -p w32/vs2008/ccext2.vcproj w32/vs2008/ccext2.vcproj.tmp
cp -p w32/vs2008/ccgnu2.vcproj w32/vs2008/ccgnu2.vcproj.tmp
sed -e s/DLLVERSION/$DLLVERSION/g < w32/vs2008/ccext2.vcproj.tmp > w32/vs2008/ccext2.vcproj
sed -e s/DLLVERSION/$DLLVERSION/g < w32/vs2008/ccgnu2.vcproj.tmp > w32/vs2008/ccgnu2.vcproj
rm -f w32/vs2008/ccext2.vcproj.tmp w32/vs2008/ccgnu2.vcproj.tmp

#
# Visual Studio 2005
#
rm -f w32/ccext2.vcproj.tmp w32/ccgnu2.vcproj.tmp
cp -p w32/ccext2.vcproj w32/ccext2.vcproj.tmp
cp -p w32/ccgnu2.vcproj w32/ccgnu2.vcproj.tmp
sed -e s/VCVERSION/$VERSION/g < w32/ccext2.vcproj.tmp > w32/ccext2.vcproj
sed -e s/VCVERSION/$VERSION/g < w32/ccgnu2.vcproj.tmp > w32/ccgnu2.vcproj

cp -p w32/ccext2.vcproj w32/ccext2.vcproj.tmp
cp -p w32/ccgnu2.vcproj w32/ccgnu2.vcproj.tmp
sed -e s/DLLVERSION/$DLLVERSION/g < w32/ccext2.vcproj.tmp > w32/ccext2.vcproj
sed -e s/DLLVERSION/$DLLVERSION/g < w32/ccgnu2.vcproj.tmp > w32/ccgnu2.vcproj
rm -f w32/ccext2.vcproj.tmp w32/ccgnu2.vcproj.tmp

#
# Visual Studio 6
#
rm -f w32/ccext2.dsp.tmp w32/ccgnu2.dsp.tmp
cp -p w32/ccext2.dsp w32/ccext2.dsp.tmp
cp -p w32/ccgnu2.dsp w32/ccgnu2.dsp.tmp
sed -e s/VCVERSION/$VERSION/g < w32/ccext2.dsp.tmp > w32/ccext2.dsp
sed -e s/VCVERSION/$VERSION/g < w32/ccgnu2.dsp.tmp > w32/ccgnu2.dsp

cp -p w32/ccext2.dsp w32/ccext2.dsp.tmp
cp -p w32/ccgnu2.dsp w32/ccgnu2.dsp.tmp
sed -e s/DLLVERSION/$DLLVERSION/g < w32/ccext2.dsp.tmp > w32/ccext2.dsp
sed -e s/DLLVERSION/$DLLVERSION/g < w32/ccgnu2.dsp.tmp > w32/ccgnu2.dsp
rm -f w32/ccext2.dsp.tmp w32/ccgnu2.dsp.tmp

