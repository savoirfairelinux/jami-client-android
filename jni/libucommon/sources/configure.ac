# Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

AC_INIT([ucommon],[6.0.7])
AC_CONFIG_SRCDIR([inc/ucommon/ucommon.h])

LT_VERSION="6:3:0"
OPENSSL_REQUIRES="0.9.7"

AC_CONFIG_AUX_DIR(autoconf)
AC_CANONICAL_SYSTEM
AC_PROG_CPP
AC_PROG_CC
AC_PROG_CXXCPP
AC_PROG_CXX
AC_LIBTOOL_WIN32_DLL
AM_PROG_LIBTOOL
AM_INIT_AUTOMAKE([dist-shar dist-zip])
AM_CONFIG_HEADER(ucommon-config.h)

AC_C_RESTRICT
AC_C_VOLATILE
AC_C_INLINE

UCOMMON_VISIBILITY="0"
UCOMMON_FLAGS=""
UCOMMON_MODEL="CXX"
UCOMMON_CLINK=""
UCOMMON_LIBC="-lc"
UCOMMON_LIBS=""
OPENSSL_LINK=""

CHECKFLAGS="$CHECKFLAGS"

case "$with_crypto" in
ssl|openssl)
    ssl="openssl"
    ;;
gnutls|gnu)
    ssl="gnutls"
    ;;
none|nossl)
    ssl="nossl"
    ;;
*)
    ssl="detect"
    ;;
esac

have_gettext="no"
socket_lib=""
posix4_lib=""
rt_lib=""

test -z "$localedir" && localedir='${datadir}/locale'

AC_DEFUN([AC_SUBST_DIR], [
        ifelse($2,,,$1="[$]$2")
        result="***"
        prior="A"
        while test "$prior" != "$result" ; do
                prior=`(echo "[$]$1")`
                $1=`(
                test "x$prefix" = xNONE && prefix="$ac_default_prefix"
                test "x$exec_prefix" = xNONE && exec_prefix="${prefix}"
                eval echo \""[$]$1"\"
                )`
                result=`(echo "[$]$1")`
        done
        AC_SUBST($1)
])

# for very old gcc and for minix we disable full ucommon build by default
# we also disable for mingw32 or when no "shared" library support because
# libstdc++ is not dll and really bloats in linkage of plugins.  Any case,
# you can always override with --enable-stdcpp...

if test -z "$enable_stdcpp" ; then
    if test "x$GCC" = "xyes" ; then
        gccver=`$CC -dumpversion | cut -f1 -d.`
    fi
    if test -z "$gccver" ; then
        gccver="3"
    fi
    if test "$gccver" -lt "3" ; then
        enable_stdcpp="no"
    fi
    if test "x$enable_shared" = "xno" ; then
        enable_stdcpp="no"
    fi
    case "$target_os" in
    *minix*|mingw*)
        enable_stdcpp="no"
        ;;
    esac
fi

AC_ARG_ENABLE(posix-timers,
    AC_HELP_STRING([--enable-posix-timers],
        [enable posix timers]))

if test "x$enable_posix_timers" = "xyes" ; then
    UCOMMON_FLAGS="$UCOMMON_FLAGS -DPOSIX_TIMERS"
fi

AC_ARG_ENABLE(stdcpp,
    AC_HELP_STRING([--disable-stdcpp],
        [compile without stdc++ runtime overhead]))

    AC_MSG_CHECKING([full c++ support and linking model])

if test "x$enable_stdcpp" = "xno" ; then
    UCOMMON_MODEL="CC"
    AC_MSG_RESULT(no)
    COMPAT=""
else
    COMPAT="commoncpp"
    AC_MSG_RESULT(yes)
fi

AC_ARG_WITH(sslstack,
    AC_HELP_STRING([--with-sslstack=lib],[specify which ssl stack to build]),[
    if test x$with_sslstack != x ; then
        ssl="${with_sslstack}" ; fi
])

AC_ARG_WITH(libc,
    AC_HELP_STRING([--with-libc=path],[specify path to alternate libc]),[
    if test x$with_libc != x ; then
        clib=`echo ${with_libc} | sed s/[-]l//`
        UCOMMON_LIBC=-l${clib} ; fi
])

AC_ARG_ENABLE(debug,
    AC_HELP_STRING([--enable-debug],[compile for debugging]))
if test -z "$enable_debug" ; then
    enable_debug="no"
elif test $enable_debug = "yes" ; then
    CXXFLAGS="${CXXFLAGS} -g -DDEBUG"
fi

AH_BOTTOM([
#include <ucommon/platform.h>
])

threading="none"
clib=`echo ${UCOMMON_LIBC} | sed s/[-]l//`
tlib=""

AC_CHECK_HEADERS(stdint.h poll.h sys/mman.h sys/shm.h sys/poll.h sys/timeb.h endian.h sys/filio.h dirent.h sys/resource.h wchar.h netinet/in.h net/if.h)
AC_CHECK_HEADERS(mach/clock.h mach-o/dyld.h linux/version.h sys/inotify.h sys/event.h syslog.h sys/wait.h termios.h termio.h fcntl.h unistd.h)
AC_CHECK_HEADERS(sys/param.h sys/lockf.h sys/file.h dlfcn.h)

AC_CHECK_HEADER(regex.h, [
    AC_DEFINE(HAVE_REGEX_H, [1], [have regex header])
    AC_CHECK_LIB(regex, regfree, [
        UCOMMON_LIBS="$UCOMMON_LIBS -lregex"
    ])
])

AC_CHECK_LIB(msvcrt, fopen, [
    threading="msw"
    clib="msvcrt"
    UCOMMON_VISIBILITY="0"
    UCOMMON_LIBS="--enable-stdcall-fixup $UCOMMON_LIBS -lmingwex -lmingw32 -lcrypt32 -ladvapi32 -luser32 -lws2_32 -lwsock32 -lkernel32"
    UCOMMON_LIBC="-lmsvcrt -lgcc"
    OPENSSL_LINK="-lgdi32"
    AC_DEFINE(HAVE_INET_NTOP, [1], [have inet_ntop])
    AC_DEFINE(HAVE_GETADDRINFO, [1], [have getaddrinfo])
],[
    AC_CHECK_LIB(gcc, __modsi3, [
        UCOMMON_LIBC="$UCOMMON_LIBC -lgcc"
    ])
    AC_CHECK_LIB($clib, socketpair, [
        AC_DEFINE(HAVE_SOCKETPAIR, [1], [have socketpair])
    ],[
            AC_CHECK_LIB(socket, socketpair, [AC_DEFINE(HAVE_SOCKETPAIR, [1], [have socketpair])])
    ])
    AC_CHECK_LIB($clib, getaddrinfo, [
        AC_DEFINE(HAVE_GETADDRINFO, [1], [have getaddrinfo])
    ],[
            AC_CHECK_LIB(socket, getaddrinfo, [AC_DEFINE(HAVE_GETADDRINFO, [1], [have getaddrinfo])])
    ])
    AC_CHECK_LIB($clib, gethostbyname2, [
        AC_DEFINE(HAVE_GETHOSTBYNAME2, [1], [have gethostbyname2])
    ],[
            AC_CHECK_LIB(socket, gethostbyname2, [AC_DEFINE(HAVE_GETHOSTBYNAME2, [1], [have gethostbyname2])])
    ])

    AC_CHECK_LIB($clib, inet_ntop, [
        AC_DEFINE(HAVE_INET_NTOP, [1], [have inet ntop])
    ],[
        AC_CHECK_LIB(socket, inet_ntop, [
            AC_DEFINE(HAVE_INET_NTOP, [1], [have inet ntop])
        ],[
            AC_CHECK_LIB(nsl, inet_ntop, [
                AC_DEFINE(HAVE_INET_NTOP, [1], [have inet_ntop])
                socket_lib="$socket_lib -lnsl"
            ])
        ])
    ])

    AC_CHECK_LIB(socket, socket,[
        AC_CHECK_LIB(socket, socketpair,[
            AC_DEFINE(HAVE_SOCKETPAIR, [1], [have socketpair in libsocket])
        ])
        socket_lib="-lsocket"
    ])
])

AC_ARG_ENABLE(socks,
    AC_HELP_STRING([--enable-socks],
        [compile with socks proxy support]))

if test "x$enable_socks" = "xyes" ; then
    enable_socks="no"
    AC_CHECK_HEADERS(socks.h, [
        enable_socks="yes"
    ])
else
    enable_socks="no"
fi

AC_MSG_CHECKING([for socks proxy support])

if test "x$enable_socks" = "xyes" ; then
    socket_lib="-lsocksd $socket_lib"
    AC_DEFINE(HAVE_SOCKS, [1], "Socks proxy support")
    AC_MSG_RESULT(yes)
else
    AC_MSG_RESULT(no)
fi

AC_CHECK_LIB($clib, lstat, [
    AC_DEFINE(HAVE_LSTAT, [1], [have lstat])
])

AC_CHECK_LIB($clib, strcoll, [
    AC_DEFINE(HAVE_STRCOLL, [1], [string collation])
])

AC_CHECK_LIB($clib, stricmp, [
    AC_DEFINE(HAVE_STRICMP, [1], [string icmp])
])

AC_CHECK_LIB($clib, stristr, [
    AC_DEFINE(HAVE_STRISTR, [1], [string istr])
])

AC_CHECK_LIB($clib, sysconf, [
    AC_DEFINE(HAVE_SYSCONF, [1], [system config])
])

AC_CHECK_LIB($clib, posix_memalign, [
    AC_DEFINE(HAVE_POSIX_MEMALIGN, [1], [posix memory alignment])
])

AC_CHECK_LIB($clib, dlopen,,[
    AC_CHECK_LIB(dl, dlopen, [UCOMMON_LIBS="$UCOMMON_LIBS -ldl"],[
        AC_CHECK_LIB(compat, dlopen, [UCOMMON_LIBS="$UCOMMON_LIBS -lcompat"])
        AC_CHECK_LIB(dld, shl_load,[
            AC_DEFINE(HAVE_SHL_LOAD, [1], [alternate dyn loader])
            UCOMMON_LIBS="$UCOMMON_LIBS -ldld"
        ])
    ])
])

PTHREAD_FLAGS=""

if test "$enable_shared" = "no" ; then
    UCOMMON_FLAGS="$UCOMMON_FLAGS -DUCOMMON_STATIC" ; fi

AC_ARG_ENABLE(atomics, [
    AC_HELP_STRING([--enable-atomics],[enable gcc atomic support])], [
    AC_DEFINE(HAVE_GCC_ATOMICS, [1], ["cannot test in autoconf safely"])
])

AC_ARG_ENABLE(pth, [
    AC_HELP_STRING([--enable-pth],[always use GNU pth for threading])
])

if test "$threading" = "none" ; then
    if test "$enable_pth" != "yes" ; then
        AC_CHECK_HEADER(pthread.h, [
            PTHREAD_FLAGS="-mt -pthread -pthreads"
            AC_CHECK_LIB(pthread,pthread_create,[
                tlib="pthread"
                UCOMMON_LIBS="$UCOMMON_LIBS -lpthread"
            ])
            if test -z "$tlib" ; then
                AC_CHECK_LIB($clib, pthread_create, [tlib="$clib"])
            fi
            if test -z "$tlib" ; then
                AC_CHECK_LIB(c_r, pthread_create, [
                    tlib="c_r"
                    if test "$clib" = "c" ; then
                        AC_CHECK_LIB(c_r, malloc, [clib="c_r"])
                    fi
                ])
            fi
            if test ! -z "$tlib" ; then
                AC_CHECK_LIB($tlib,pthread_condattr_setclock,[
                    AC_DEFINE(HAVE_PTHREAD_CONDATTR_SETCLOCK, [1], ["pthread clocking"])
                ])
                AC_CHECK_LIB($tlib,pthread_setconcurrency,[
                    AC_DEFINE(HAVE_PTHREAD_SETCONCURRENCY, [1], ["pthread concurrency"])
                ])

                AC_CHECK_LIB($tlib,pthread_yield,[
                    AC_DEFINE(HAVE_PTHREAD_YIELD, [1], ["pthread yield"],[
                        AC_CHECK_LIB($tlib,pthread_yield_np,[
                            AC_DEFINE(HAVE_PTHREAD_YIELD_NP, [1], ["pthread yield np"])
                        ])
                    ])
                ])

                AC_CHECK_LIB($tlib,pthread_delay,[
                    AC_DEFINE(HAVE_PTHREAD_DELAY, [1], ["pthread delay"])
                ])
                AC_CHECK_LIB($tlib,pthread_delay_np,[
                    AC_DEFINE(HAVE_PTHREAD_DELAY_NP, [1], ["pthread delay np"])
                ])
                AC_CHECK_LIB($tlib,pthread_setschedprio,[
                    AC_DEFINE(HAVE_PTHREAD_SETSCHEDPRIO, [1], ["pthread scheduling"])
                ])
            fi
        ],[
            AC_CHECK_HEADER(windows.h,, [
                AC_CHECK_HEADER(pth.h,[
                    UCOMMON_LIBS="$UCOMMON_LIBS -lpth"
                    UCOMMON_FLAGS="$UCOMMON_FLAGS -D__PTH__"
                ],[
                    AC_MSG_ERROR("pthread or GNU pth support required")
                ])
            ])
        ])
    else
        AC_CHECK_HEADER(pth.h,[
            UCOMMON_LIBS="$UCOMMON_LIBS -lpth"
            UCOMMON_FLAGS="$UCOMMON_FLAGS -D__PTH__"
        ],[
            AC_MSG_ERROR("GNU pth required")
        ])
    fi
fi

for func in ftok shm_open nanosleep clock_nanosleep clock_gettime strerror_r localtime_r gmtime_r posix_fadvise ftruncate pwrite setpgrp setlocale gettext execvp atexit realpath symlink readlink waitpid wait4 ; do
    found="no"
    AC_CHECK_FUNC($func,[
        found=$func
    ],[
        AC_CHECK_LIB(posix4, $func, [
            found=$func
            posix4_lib="-lposix4"
        ],[
            AC_CHECK_LIB(rt, $func, [
                found=$func
                rt_lib="-lrt"
            ],[
                if test ! -z "$tlib" ; then
                    AC_CHECK_LIB($tlib, $func, [
                        found=$func
                    ])
                fi
            ])
        ])
    ])
    case $found in
    shm_open)
        AC_DEFINE(HAVE_SHM_OPEN, [1], [shared memory open])
        ;;
    setpgrp)
        AC_DEFINE(HAVE_SETPGRP, [1], [process group control])
        ;;
    realpath)
        AC_DEFINE(HAVE_REALPATH, [1], [realpath support])
        ;;
    ftok)
        AC_DEFINE(HAVE_FTOK, [1], [has ftok])
        ;;
    gmtime_r)
        AC_DEFINE(HAVE_GMTIME_R, [1], [has localtime_r])
        ;;
    localtime_r)
        AC_DEFINE(HAVE_LOCALTIME_R, [1], [has localtime_r])
        ;;
    strerror_r)
        AC_DEFINE(HAVE_STRERROR_R, [1], [has strerror_r])
        ;;
    nanosleep)
        AC_DEFINE(HAVE_NANOSLEEP, [1], [has nanosleep])
        ;;
    clock_nanosleep)
        AC_DEFINE(HAVE_CLOCK_NANOSLEEP, [1], [has clocked nanosleep])
        ;;
    clock_gettime)
        AC_DEFINE(HAVE_CLOCK_GETTIME, [1], [has posix clock functions])
        ;;
    posix_fadvise)
        AC_DEFINE(HAVE_POSIX_FADVISE, [1], [can specify access options])
        ;;
    ftruncate)
        AC_DEFINE(HAVE_FTRUNCATE, [1], [can truncate files])
        ;;
    pwrite)
        AC_DEFINE(HAVE_PWRITE, [1], [can do atomic write with offset])
        ;;
    setlocale)
        AC_DEFINE(HAVE_SETLOCALE, [1], [can set localization])
        ;;
    gettext)
        AC_DEFINE(HAVE_GETTEXT, [1], [has gettext in libc])
        have_gettext="yes"
        ;;
    execvp)
        AC_DEFINE(HAVE_EXECVP, [1], [has execvp in libc])
        ;;
    symlink)
        AC_DEFINE(HAVE_SYMLINK, [1], [has symlink in libc])
        ;;
    readlink)
        AC_DEFINE(HAVE_READLINK, [1], [has readlink in libc])
        ;;
    waitpid)
        AC_DEFINE(HAVE_WAITPID, [1], [has waitpid in libc])
        ;;
    wait4)
        AC_DEFINE(HAVE_WAIT4, [1], [has wait4 in libc])
        ;;
    esac
done

UCOMMON_LINKED="$socket_lib $posix4_lib $rt_lib $UCOMMON_LIBS"
UCOMMON_LIBS="$socket_lib $posix4_lib $rt_lib $UCOMMON_LIBS $UCOMMON_LIBC"

if test "$UCOMMON_MODEL" = "CC" ; then
    AC_CHECK_LIB(Systemstubs, printf, [
        UCOMMON_CLINK="$UCOMMON_CLINK -lSystemstubs"
        UCOMMON_LIBC="$UCOMMON_LIBC -lgcc"
    ])
fi

AC_LANG_SAVE
AC_LANG_CPLUSPLUS
COMPILER_FLAGS=""

if test "x$GCC" = "xyes" ; then
    UCOMMON_FLAGS="$UCOMMON_FLAGS -Wno-long-long" ; fi

for flag in $CXXFLAGS ; do
    case "$flag" in
    -f*exceptions|-f*rtti|-f*check-new|-f*enforce-eh-specs|-finline|-f*except)
        if test "$UCOMMON_MODEL" = "CXX" ; then
            UCOMMON_FLAGS="$UCOMMON_FLAGS $flag" ; fi
        ;;
    -fvisibility=*)
        VISIBLE_FLAGS=""
        COMPILER_FLAGS="$COMPILER_FLAGS $flag"
        ;;
    *)
        COMPILER_FLAGS="$COMPILER_FLAGS $flag"
        ;;
    esac
done
export CXXFLAGS="$COMPILER_FLAGS"

if test "$UCOMMON_MODEL" = "CC" ; then
    UCOMMON_FLAGS="$UCOMMON_FLAGS"

    if test x"$GCC" = "xyes" ; then
        ucflags="-fno-exceptions -fno-rtti -fno-enforce-eh-specs"
        lcflags="-nodefaultlibs -nostdinc++"
    else
        ucflags="-library=no%Cstd -features=no%except -features=namespace -features=no%rtti"
        lcflags=""
    fi

    for flag in $ucflags ; do
        AC_MSG_CHECKING([whether ${CXX} supports $flag])
        echo 'void f(){}' >conftest.cpp
        if test -z "`${CXX} $flag -c conftest.cpp 2>&1`"; then
            UCOMMON_FLAGS="$UCOMMON_FLAGS $flag"
            AC_MSG_RESULT(yes)
            if test "$flag" = "-library=no%Cstd" ; then
                UCOMMON_MODEL="CXX" ; fi
        else
            AC_MSG_RESULT(no)
        fi
        rm -f conftest*
    done

    for flag in $lcflags ; do
        AC_MSG_CHECKING([whether ${CXX} supports $flag])
        echo 'void f(){}' >conftest.cpp
        if test -z "`${CXX} $flag -c conftest.cpp 2>&1`"; then
            UCOMMON_CLINK="$UCOMMON_CLINK $flag"
            AC_MSG_RESULT(yes)
        else
            AC_MSG_RESULT(no)
        fi
        rm -f conftest*
    done
else
    ac_save_CXXFLAGS="$CXXFLAGS"
    CXXFLAGS=""
    AC_LANG_SAVE
    AC_LANG_CPLUSPLUS
    AC_CHECK_HEADERS(stdexcept)
    AC_MSG_CHECKING(for old style iostream)
    AC_TRY_COMPILE([
        #include <iostream>
        using namespace std;

        class mystr : public streambuf, public iostream
        {
            mystr();
        };

        mystr::mystr() : streambuf(), iostream((streambuf *)this)
        {
        }
    ],[return 0;],[
        AC_MSG_RESULT(no)
        UCOMMON_FLAGS="$UCOMMON_FLAGS -DNEW_STDCPP"
    ],[
        AC_MSG_RESULT(yes)
        UCOMMON_FLAGS="$UCOMMON_FLAGS -DOLD_STDCPP"
    ])
    AC_LANG_RESTORE
    CXXFLAGS="$ac_save_CXXFLAGS"
fi

if test ! -z "$PTHREAD_FLAGS" ; then
    for flag in $PTHREAD_FLAGS ; do
        AC_MSG_CHECKING([whether ${CXX} supports $flag])
        echo 'void f(){}' >conftest.cpp
        if test -z "`${CXX} $flag -c conftest.cpp 2>&1`"; then
            AC_MSG_RESULT(yes)
            UCOMMON_FLAGS="$UCOMMON_FLAGS $flag"
            break
        else
            AC_MSG_RESULT(no)
        fi
    done
fi

export CXXFLAGS

# libstdc++ on some targets broken badly if inlines hidden.  Might be
# deeper issue with ansi c++ library model...
flags="-fno-check-new -finline"
if test "$UCOMMON_MODEL" = "CC" ; then
    if test "$enable_shared" != "no" ; then
        flags="$flags -fvisibility-inlines-hidden"
    fi
fi

for flag in $flags ; do
    AC_MSG_CHECKING([whether ${CXX} supports $flag])
    echo 'void f(){}' >conftest.cpp
    if test -z "`${CXX} $flag -c conftest.cpp 2>&1`"; then
        UCOMMON_FLAGS="$UCOMMON_FLAGS $flag"
        AC_MSG_RESULT(yes)
    else
        AC_MSG_RESULT(no)
    fi
    rm -f conftest*
done

AC_MSG_CHECKING([whether ${CXX} supports visibility])
echo 'void f(){}' >conftest.cpp
if test -z "`${CXX} -fvisibility=hidden -c conftest.cpp 2>&1`"; then
    UCOMMON_FLAGS="$UCOMMON_FLAGS -fvisibility=hidden -DUCOMMON_VISIBILITY=1"
    AC_MSG_RESULT(yes)
    UCOMMON_VISIBILITY="1"
else
    UCOMMON_FLAGS="$UCOMMON_FLAGS -DUCOMMON_VISIBILITY=0"
    AC_MSG_RESULT(no)
fi
rm -f conftest*

AC_LANG_RESTORE

AC_SUBST_DIR(includes, includedir)
AC_SUBST_DIR(libs, libdir)

case "$libs" in
/usr/*)
    ;;
*)
    UCOMMON_LINKED="-L$libs $UCOMMON_LINKED"
    ;;
esac

case "$includes" in
/usr/*)
    ;;
*)
    UCOMMON_FLAGS="$UCOMMON_FLAGS -I$includes"
    ;;
esac

case "$target_os" in
*bsd*)
    if test "$UCOMMON_MODEL" = "CC" ; then
        if test "$GCC" = "yes" -a "$UCOMMON_LIBC" = "-lc" ; then
            UCOMMON_LIBC="-Wl,-lc"
        elif test "$GCC" = "yes" -a "$UCOMMON_LIBC" = "-lc_r" ; then
            UCOMMON_LIBC="-Wl,-lc_r"
        fi
    fi
    ;;
osf*)
    UCOMMON_FLAGS="$UCOMMON_FLAGS -D_POSIX_C_SOURCE=1 -D_OSF_SOURCE=1 -D__USE_STD_IOSTREAM"
    ;;
cygwin*|mingw*)
    UCOMMON_LINKED="$UCOMMON_LINKED -no-undefined"
    UCOMMON_LIBS="$UCOMMON_LIBS -no-undefined"
    ;;
esac

UCOMMON_CLINK="$UCOMMON_CLINK $UCOMMON_LIBC"

if test -z "$UCOMMON_SSLSTACK" ; then
    UCOMMON_SSLSTACK="$sslstack" ; fi

OPENSSL_LIBS=""
GNUTLS_LIBS=""

AC_ARG_WITH(pkg-config,
    AC_HELP_STRING([--with-pkg-config],[enable support for pkg-config]),[
    PKG_CHECK_MODULES(OPENSSL, [openssl >= $OPENSSL_REQUIRES], [], [
        OPENSSL_LIBS=""
        AC_CHECK_HEADERS(openssl/ssl.h, [
            OPENSSL_LIBS="-lssl -lcrypto -lz $OPENSSL_LINK"
        ])
        AC_CHECK_HEADERS(openssl/fips.h)
    ])
    PKG_CHECK_MODULES(GNUTLS, [gnutls >= 2.8.0], [], [GNUTLS_LIBS=""])
],[
    AC_CHECK_HEADERS(openssl/ssl.h, [
        OPENSSL_LIBS="-lssl -lcrypto -lz $OPENSSL_LINK"
    ])
    AC_CHECK_HEADERS(openssl/fips.h)
    AC_CHECK_LIB(gnutls, gnutls_init, [
        GNUTLS_LIBS="-lgnutls"
    ])
])


SECURE=""
case "$ssl" in
secure|any|detect)
    if test ! -z "$GNUTLS_LIBS" ; then
        SECURE_LIBS="$GNUTLS_LIBS"
        SECURE="gnutls"
    fi
    if test -z "$SECURE" ; then
        if test ! -z "$OPENSSL_LIBS" ; then
            SECURE_LIBS="$OPENSSL_LIBS"
            SECURE="openssl"
        fi
    fi
    ;;
gnutls|gnu)
    if test ! -z "$GNUTLS_LIBS" ; then
        SECURE_LIBS="$GNUTLS_LIBS"
        SECURE="gnutls"
    fi
    ;;
openssl|ssl)
    if test ! -z "$OPENSSL_LIBS" ; then
        SECURE_LIBS="$OPENSSL_LIBS"
        SECURE="openssl"
    fi
    ;;
esac

if test -z "$SECURE" ; then
    SECURE_LIBS=""
    SECURE="nossl"
fi

AC_MSG_CHECKING([secure library mode])
AC_MSG_RESULT([$SECURE])

SECURE_LOCAL="../$SECURE/libusecure.la $SECURE_LIBS"

LT_GENERIC="${LIBTOOL}"
if test "$UCOMMON_MODEL" = "CC" ; then
    LIBTOOL='$(SHELL) $(top_srcdir)/cmodel.sh'" ${LIBTOOL}" ; fi

includes='${includedir}/ucommon'

if test "$sysconfdir" = '${prefix}/etc' ; then
    sysconfdir="/etc" ; fi

if test "$localstatedir" = '${prefix}/var' ; then
    localstatedir="/var" ; fi

AC_CHECK_HEADER(libintl.h, [
    AC_DEFINE(HAVE_LIBINTL_H, [1], [international headers])
    if test "$have_gettext" = "no" ; then
        AC_CHECK_LIB(intl, gettext, [
            AC_DEFINE(HAVE_GETTEXT, [1], [has gettext in glibc])
            UCOMMON_LIBS="$UCOMMON_LIBS -lintl"
        ])
    fi
])

if test "$prefix" = "NONE" ; then
    prefixdir="$ac_default_prefix"
else
    prefixdir="$prefix" ; fi

AC_SUBST_DIR(UCOMMON_INCLUDES, includes)
AC_SUBST_DIR(UCOMMON_LOCALE, localedir)
AC_SUBST_DIR(UCOMMON_CFGPATH, sysconfdir)
AC_SUBST_DIR(UCOMMON_VARPATH, localstatedir)
AC_SUBST_DIR(UCOMMON_PREFIX, prefixdir)
AC_DEFINE_UNQUOTED(UCOMMON_PREFIX, "$UCOMMON_PREFIX", [config path])
AC_DEFINE_UNQUOTED(UCOMMON_CFGPATH, "$UCOMMON_CFGPATH", [config path])
AC_DEFINE_UNQUOTED(UCOMMON_VARPATH, "$UCOMMON_VARPATH", [config path])
AC_DEFINE_UNQUOTED(UCOMMON_LOCALE, "$UCOMMON_LOCALE", [locale path])

AC_PATH_PROG(DOXYGEN, doxygen, doxygen)
AC_SUBST(DOXYGEN)
AC_SUBST(CHECKFLAGS)
AC_SUBST(CXXFLAGS)
AC_SUBST(UCOMMON_VISIBILITY)
AC_SUBST(UCOMMON_INCLUDES)
AC_SUBST(UCOMMON_LOCALE)
AC_SUBST(UCOMMON_LINKED)
AC_SUBST(UCOMMON_MODEL)
AC_SUBST(UCOMMON_FLAGS)
AC_SUBST(UCOMMON_CLINK)
AC_SUBST(UCOMMON_LIBC)
AC_SUBST(UCOMMON_LIBS)
AC_SUBST(SECURE_LOCAL)
AC_SUBST(SECURE_LIBS)
AC_SUBST(SECURE)
AC_SUBST(COMPAT)
AC_SUBST(OPENSSL_LIBS)
AC_SUBST(GNUTLS_LIBS)
AC_SUBST(LT_VERSION)
AC_SUBST(LT_GENERIC)
AC_SUBST(localedir)

AC_OUTPUT(Makefile corelib/Makefile commoncpp/Makefile
openssl/Makefile gnutls/Makefile nossl/Makefile utils/Makefile
inc/Makefile inc/ucommon/Makefile inc/commoncpp/Makefile test/Makefile
directive commoncpp.pc ucommon.pc ucommon.spec ucommon-config commoncpp-config)

