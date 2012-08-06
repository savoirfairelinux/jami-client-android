/* config.h.in.  Generated from configure.in by autoheader.  */
#ifdef ANDROID_MANAGED_SOCKET
#include <cutils/sockets.h>
#endif

#define ANDROID_SOCKET_DIR "/dev/socket"

/* Directory for installing the binaries */
#define DBUS_BINDIR "/system/bin"

/* Build test code */
#undef DBUS_BUILD_TESTS

/* Build X11-dependent code */
#undef DBUS_BUILD_X11

/* whether -export-dynamic was passed to libtool */
#undef DBUS_BUILT_R_DYNAMIC

/* Use dnotify on Linux */
#undef DBUS_BUS_ENABLE_DNOTIFY_ON_LINUX

/* Use kqueue */
#undef DBUS_BUS_ENABLE_KQUEUE

/* Directory to check for console ownerhip */
#define DBUS_CONSOLE_AUTH_DIR "/etc"

/* File to check for console ownerhip */
#undef DBUS_CONSOLE_OWNER_FILE

/* Directory for installing the DBUS daemon */
#define DBUS_DAEMONDIR "/system/bin"

/* Directory for installing DBUS data files */
#define DBUS_DATADIR "/data"

/* Disable assertion checking */
#undef DBUS_DISABLE_ASSERT

/* Disable public API sanity checking */
#undef DBUS_DISABLE_CHECKS

/* Support a verbose mode */
#undef DBUS_ENABLE_VERBOSE_MODE

/* Defined if gcov is enabled to force a rebuild due to config.h changing */
#undef DBUS_GCOV_ENABLED

/* Some atomic integer implementation present */
#undef DBUS_HAVE_ATOMIC_INT


        #if (defined(__i386__) || defined(__x86_64__))
        # define DBUS_HAVE_ATOMIC_INT 1
        #endif
      

/* Defined if we have gcc 3.3 and thus the new gcov format */
#undef DBUS_HAVE_GCC33_GCOV

/* Where per-session bus puts its sockets */
#define DBUS_SESSION_SOCKET_DIR "/data"

/* The default D-Bus address of the system bus */
#define DBUS_SYSTEM_BUS_DEFAULT_ADDRESS "unix:path="DBUS_SYSTEM_SOCKET

/* The name of the socket the system bus listens on by default */
#define DBUS_SYSTEM_SOCKET ANDROID_SOCKET_DIR"/dbus"

/* Where to put test sockets */
#define DBUS_TEST_SOCKET_DIR "/data"

/* User for running the system BUS daemon */
#undef DBUS_USER

/* Use atomic integer implementation for 486 */
#undef DBUS_USE_ATOMIC_INT_486


              #if (defined(__i386__) || defined(__x86_64__))
              # define DBUS_USE_ATOMIC_INT_486 1
              #endif
            

/* A 'va_copy' style function */
#define DBUS_VA_COPY va_copy

/* 'va_lists' cannot be copies as values */
#undef DBUS_VA_COPY_AS_ARRAY

/* The name of the gettext domain */
#undef GETTEXT_PACKAGE

/* Disable GLib assertion macros */
#undef G_DISABLE_ASSERT 

/* Disable GLib public API sanity checking */
#undef G_DISABLE_CHECKS

/* Have abstract socket namespace */
#undef HAVE_ABSTRACT_SOCKETS

/* Define to 1 if you have the `backtrace' function. */
#undef HAVE_BACKTRACE

/* Have cmsgcred structure */
#undef HAVE_CMSGCRED

/* Have console owner file */
#undef HAVE_CONSOLE_OWNER_FILE

/* Have the ddfd member of DIR */
#undef HAVE_DDFD

/* Have dirfd function */
#undef HAVE_DIRFD

/* Define to 1 if you have the <dlfcn.h> header file. */
#define HAVE_DLFCN_H 1

/* Define to 1 if you have the <errno.h> header file. */
#define HAVE_ERRNO_H 1

/* Define to 1 if you have the <execinfo.h> header file. */
#define HAVE_EXECINFO_H 1

/* Define to 1 if you have the <expat.h> header file. */
#define HAVE_EXPAT_H 1

/* Define to 1 if you have the `fpathconf' function. */
#undef HAVE_FPATHCONF

/* Define to 1 if you have the `getgrouplist' function. */
#define HAVE_GETGROUPLIST 1

/* Define to 1 if you have the `getpeereid' function. */
#undef HAVE_GETPEEREID

/* Define to 1 if you have the `getpeerucred' function. */
#undef HAVE_GETPEERUCRED

/* Have GNU-style varargs macros */
#undef HAVE_GNUC_VARARGS

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Have ISO C99 varargs macros */
#define HAVE_ISO_VARARGS 1

/* Define to 1 if you have the `nsl' library (-lnsl). */
#undef HAVE_LIBNSL

/* Define to 1 if you have the `socket' library (-lsocket). */
#undef HAVE_LIBSOCKET

/* Define to 1 if you have the <memory.h> header file. */
#define HAVE_MEMORY_H 1

/* Define to 1 if you have the `nanosleep' function. */
#define HAVE_NANOSLEEP 1

/* Have non-POSIX function getpwnam_r */
#undef HAVE_NONPOSIX_GETPWNAM_R

/* Define to 1 if you have the `poll' function. */
#define HAVE_POLL 1

/* Have POSIX function getpwnam_r */
#undef HAVE_POSIX_GETPWNAM_R

/* SELinux support */
#undef HAVE_SELINUX

/* Define to 1 if you have the `setenv' function. */
#define HAVE_SETENV 1

/* Define to 1 if you have the `socketpair' function. */
#define HAVE_SOCKETPAIR 1

/* Have socklen_t type */
#define HAVE_SOCKLEN_T 1

/* Define to 1 if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the <strings.h> header file. */
#define HAVE_STRINGS_H 1

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define to 1 if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define to 1 if you have the <sys/syslimits.h> header file. */
#define HAVE_SYS_SYSLIMITS_H 1

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define to 1 if you have the <sys/uio.h> header file. */
/* actually defined by AndroidConfig.h, commented to get rid of compiler warnings #define HAVE_SYS_UIO_H 1 */

/* Define to 1 if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* Define to 1 if you have the `unsetenv' function. */
#define HAVE_UNSETENV 1

/* Define to 1 if you have the `usleep' function. */
#define HAVE_USLEEP 1

/* Define to 1 if you have the `vasprintf' function. */
#define HAVE_VASPRINTF 1

/* Define to 1 if you have the `vsnprintf' function. */
#define HAVE_VSNPRINTF 1

/* Define to 1 if you have the `writev' function. */
#define HAVE_WRITEV 1

/* Name of package */
#define PACKAGE "dbus"

/* Define to the address where bug reports for this package should be sent. */
#undef PACKAGE_BUGREPORT

/* Define to the full name of this package. */
#define PACKAGE_NAME "dbus"

/* Define to the full name and version of this package. */
#define PACKAGE_STRING "dbus-0.95"

/* Define to the one symbol short name of this package. */
#undef PACKAGE_TARNAME

/* Define to the version of this package. */
#define PACKAGE_VERSION "0.95"

/* The size of a `char', as computed by sizeof. */
#define SIZEOF_CHAR sizeof(char)

/* The size of a `int', as computed by sizeof. */
#define SIZEOF_INT sizeof(int)

/* The size of a `long', as computed by sizeof. */
#define SIZEOF_LONG sizeof(long)

/* The size of a `long long', as computed by sizeof. */
#define SIZEOF_LONG_LONG sizeof(long long)

/* The size of a `short', as computed by sizeof. */
#define SIZEOF_SHORT sizeof(short)

/* The size of a `void *', as computed by sizeof. */
#define SIZEOF_VOID_P sizeof(void *)

/* The size of a `__int64', as computed by sizeof. */
#define SIZEOF___INT64 sizeof(long long)

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Full path to the daemon in the builddir */
#define TEST_BUS_BINARY "/system/bin/dbus-test"

/* Full path to test file test/test-exit in builddir */
#define TEST_EXIT_BINARY "/system/bin/dbus-test-exit"

/* Full path to test file test/test-segfault in builddir */
#define TEST_SEGFAULT_BINARY "/system/bin/dbus-test-segfault"

/* Full path to test file test/test-service in builddir */
#define TEST_SERVICE_BINARY "/system/bin/dbus-test-service"

/* Full path to test file test/data/valid-service-files in builddir */
#define TEST_SERVICE_DIR "/etc/dbus-test-data/valid-service-files"

/* Full path to test file test/test-shell-service in builddir */
#define TEST_SHELL_SERVICE_BINARY "/system/bin/dbus-shell-service"

/* Full path to test file test/test-sleep-forever in builddir */
#define TEST_SLEEP_FOREVER_BINARY "/system/bin/dbus-sleep-forever"

/* Version number of package */
#define VERSION "0.95"

/* Define to 1 if your processor stores words with the most significant byte
   first (like Motorola and SPARC, unlike Intel and VAX). */
#undef WORDS_BIGENDIAN


			/* Use the compiler-provided endianness defines to allow universal compiling. */
			#if defined(__BIG_ENDIAN__)
			#define WORDS_BIGENDIAN 1
			#endif
		

/* Define to 1 if the X Window System is missing or not being used. */
#define X_DISPLAY_MISSING 1

#define HAVE_UNIX_FD_PASSING 1
