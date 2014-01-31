/* Copyright (C) 2009 David Sugar, Tycho Softworks

   This file is free software; as a special exception the author gives
   unlimited permission to copy and/or distribute it, with or without
   modifications, as long as this notice is preserved.

   This program is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
   implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
*/

#define STDC_HEADERS 1

#cmakedefine PACKAGE ${PROJECT_NAME}
#cmakedefine VERSION "${VERSION}"

#cmakedefine HAVE_CLOCK_NANOSLEEP 1
#cmakedefine HAVE_DIRENT_H 1
#cmakedefine HAVE_DLFCN_H 1
#cmakedefine HAVE_ENDIAN_H 1
#cmakedefine HAVE_INTTYPES_H 1
#cmakedefine HAVE_LINUX_VERSION_H 1
#cmakedefine HAVE_STDINT_H 1
#cmakedefine HAVE_STDLIB_H 1
#cmakedefine HAVE_SYS_FILIO_H 1
#cmakedefine HAVE_SYS_MMAN_H 1
#cmakedefine HAVE_SYS_POLL_H 1
#cmakedefine HAVE_SYS_RESOURCE_H 1
#cmakedefine HAVE_SYS_SHM_H 1
#cmakedefine HAVE_SYS_STAT_H 1
#cmakedefine HAVE_SYS_TIMEB_H 1
#cmakedefine HAVE_SYS_TYPES_H 1
#cmakedefine HAVE_SYS_WAIT_H 1
#cmakedefine HAVE_UNISTD_H 1
#cmakedefine HAVE_WCHAR_H 1
#cmakedefine HAVE_REGEX_H 1
#cmakedefine HAVE_SYS_INOTIFY_H 1
#cmakedefine HAVE_SYS_EVENT_H 1
#cmakedefine HAVE_SYSLOG_H 1
#cmakedefine HAVE_LIBINTL_H 1
#cmakedefine HAVE_NETINET_IN_H 1
#cmakedefine HAVE_NET_IF_H 1
#cmakedefine HAVE_SYS_PARAM_H 1
#cmakedefine HAVE_SYS_FILE_H 1
#cmakedefine HAVE_SYS_LOCKF_H 1
#cmakedefine HAVE_REGEX_H 1

#cmakedefine HAVE_FTOK 1
#cmakedefine HAVE_GETADDRINFO 1
#cmakedefine HAVE_GETHOSTBYNAME2 1
#cmakedefine HAVE_INET_NTOP 1
#cmakedefine HAVE_GMTIME_R 1
#cmakedefine HAVE_LOCALTIME_R 1
#cmakedefine HAVE_STRERROR_R 1
#cmakedefine HAVE_MACH_CLOCK_H 1
#cmakedefine HAVE_MACH_O_DYLD_H 1
#cmakedefine HAVE_MEMORY_H 1
#cmakedefine HAVE_NANOSLEEP 1
#cmakedefine HAVE_POLL_H 1
#cmakedefine HAVE_CLOCK_GETTIME 1
#cmakedefine HAVE_POSIX_FADVISE 1
#cmakedefine HAVE_POSIX_MEMALIGN 1
#cmakedefine HAVE_PTHREAD_CONDATTR_SETCLOCK 1
#cmakedefine HAVE_PTHREAD_DELAY 1
#cmakedefine HAVE_PTHREAD_DELAY_NP 1
#cmakedefine HAVE_PTHREAD_SETCONCURRENCY 1
#cmakedefine HAVE_PTHREAD_SETSCHEDPRIO 1
#cmakedefine HAVE_PTHREAD_YIELD 1
#cmakedefine HAVE_PTHREAD_YIELD_NP 1
#cmakedefine HAVE_SHL_LOAD 1
#cmakedefine HAVE_SHM_OPEN 1
#cmakedefine HAVE_SOCKETPAIR 1
#define HAVE_STDEXCEPT 1        /* cannot seem to test in cmake... */
#cmakedefine HAVE_STRICMP 1
#cmakedefine HAVE_STRCOLL 1
#cmakedefine HAVE_STRINGS_H 1
#cmakedefine HAVE_STRISTR 1
#cmakedefine HAVE_SYSCONF 1
#cmakedefine HAVE_FTRUNCATE 1
#cmakedefine HAVE_PWRITE 1
#cmakedefine HAVE_SETPGRP 1
#cmakedefine HAVE_SETLOCALE 1
#cmakedefine HAVE_GETTEXT 1
#cmakedefine HAVE_EXECVP 1
#cmakedefine HAVE_ATEXIT 1
#cmakedefine HAVE_LSTAT 1
#cmakedefine HAVE_REALPATH 1
#cmakedefine HAVE_SYMLINK 1
#cmakedefine HAVE_READLINK 1
#cmakedefine HAVE_WAITPID 1
#cmakedefine HAVE_WAIT4 1
#cmakedefine HAVE_FCNTL_H 1
#cmakedefine HAVE_TERMIOS_H 1
#cmakedefine HAVE_TERMIO_H 1
#cmakedefine HAVE_OPENSSL_FIPS_H 1

#cmakedefine POSIX_TIMERS 1
#cmakedefine GCC_ATOMICS 1

#cmakedefine UCOMMON_LOCALE "${UCOMMON_LOCALE}"
#cmakedefine UCOMMON_CFGPATH "${UCOMMON_CFGPATH}"
#cmakedefine UCOMMON_VARPATH "${UCOMMON_VARPATH}"
#cmakedefine UCOMMON_PREFIX "${UCOMMON_PREFIX}"

#ifdef  GCC_ATOMICS
#define HAVE_GCC_ATOMICS
#endif

#include <ucommon/platform.h>

