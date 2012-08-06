/* config.h.  Generated from config.h.in by configure.  */
/* config.h.in.  Generated from configure.ac by autoheader.  */

#ifndef CCXX_CONFIG_H_
#define CCXX_CONFIG_H_
#define __DLL
#define __EXPORT_TEMPLATE(x)
/* #undef CCXX_EMPTY */
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
#define _ALL_SOURCE 1
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



/* hack for BROKEN autoheader, since it will not predicitably order
   macros by any obvious means. */

#define HAVE_UNISTD_H 1
#define HAVE_FEATURES_H 1
#define HAVE_SYS_TYPES_H 1

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifndef WIN32
#ifdef  HAVE_FEATURES_H
#include <features.h>
#endif
#endif

#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif
    


#define HAVE_SYS_TIME_H 1
#define TIME_WITH_SYS_TIME 1
#if TIME_WITH_SYS_TIME
#include <sys/time.h>
#else
#if HAVE_SYS_TIME_H
#include <sys/time.h>
#endif
#endif
    



#define HAVE_SYS_TYPES_STD 1
#define HAVE_SYS_TYPES_64 1
#define HAVE_LONG_LONG 1
/* #undef HAVE_SYS_TYPES */

#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif

#ifdef  HAVE_BITS_WORSIZE_H
#include <bits/wordtypes.h>
#endif

#ifdef HAVE_SYS_TYPES_STD
typedef int8_t int8;
typedef u_int8_t uint8;
typedef int16_t int16;
typedef u_int16_t uint16;
typedef int32_t int32;
typedef u_int32_t uint32;
#ifdef HAVE_SYS_TYPES_64
#define HAVE_64_BITS
typedef int64_t int64;
typedef u_int64_t uint64;
#endif
#else
typedef char int8;
typedef unsigned char uint8;
typedef short int16;
typedef unsigned short uint16;
typedef int int32;
typedef unsigned int uint32;
#endif

#ifndef HAVE_SYS_TYPES_64
#if defined(__WORDSIZE) || defined(__arch64__)
#if __WORDSIZE >= 64 || defined(__arch64__)
typedef long int int64;
typedef unsigned long int uint64;
#define HAVE_SYS_TYPES_64 1
#define HAVE_64_BITS
#endif
#endif
#endif

#ifndef HAVE_SYS_TYPES_64
#ifdef  __GNUC__
#if defined(HAVE_LONG_LONG) || defined(_LONGLONG)
__extension__
typedef long long int int64;
__extension__
typedef unsigned long long int uint64;
#define HAVE_SYS_TYPES_64 1
#define HAVE_64_BITS
#endif
#endif
#endif

#ifndef HAVE_SYS_TYPES_64
#if defined(HAVE_LONG_LONG) || defined(_LONGLONG)
#define HAVE_64_BITS
typedef long long int64;
typedef unsigned long long uint64;
#endif
#endif
    

/* has c++ exception handling */
#define CCXX_EXCEPTIONS 1

/* define gnutls */
/* #undef CCXX_GNUTLS */

/* have new with init */
/* #undef CCXX_HAVE_NEW_INIT */
#define CCXX_HAVE_NEW_INIT 1

/* has c++ namespaces */
/* #undef CCXX_NAMESPACES */
#define CCXX_NAMESPACES 1

/* NAT support */
#define CCXX_NAT 1

/* define openssl */
/* #undef CCXX_OPENSSL */

/* defines ssl */
/* #undef CCXX_SSL */

/* aix fixes needed */
/* #undef COMMON_AIX_FIXES */

/* enable auditing */
/* #undef COMMON_MEMORY_AUDIT */

/* cygwin environment */
/* #undef CYGWIN_IMPORTS */

/* primary config prefix */
#define ETC_CONFDIR "/usr/local/etc/"

/* system config prefix */
/* #undef ETC_PREFIX */

/* Define to 1 if you have the <alloca.h> header file. */
#define HAVE_ALLOCA_H 1

/* Define to 1 if you have the <arpa/inet.h> header file. */
#define HAVE_ARPA_INET_H 1

/* atomic aix operations */
/* #undef HAVE_ATOMIC_AIX */

/* Define to 1 if you have the <bits/atomicity.h> header file. */
/* #undef HAVE_BITS_ATOMICITY_H */

/* Define to 1 if you have the <bits/wordsize.h> header file. */
/* #undef HAVE_BITS_WORDSIZE_H */

/* have bool type */
#define HAVE_BOOL_TYPE 1

/* Define to 1 if you have the <bsd/signal.h> header file. */
/* #undef HAVE_BSD_SIGNAL_H */

/* Define to 1 if you have the `clock_gettime' function. */
#define HAVE_CLOCK_GETTIME 1

/* Define to 1 if you have the <dlfcn.h> header file. */
#define HAVE_DLFCN_H 1

/* have endian header */
#define HAVE_ENDIAN_H 1

/* Define to 1 if you have the <errno.h> header file. */
#define HAVE_ERRNO_H 1

/* Define to 1 if you have the <exception> header file. */
/* #undef HAVE_EXCEPTION */
#define HAVE_EXCEPTION 1

/* Enable extras */
#define HAVE_EXTRAS 1

/* Define to 1 if you have the <fcntl.h> header file. */
#define HAVE_FCNTL_H 1

/* Define to 1 if you have the <features.h> header file. */
#define HAVE_FEATURES_H 1

/* has gcc atomic functions */
/* #undef HAVE_GCC_BITS_ATOMIC */

/* has __gnu_cxx atomic functions */
/* #undef HAVE_GCC_CXX_BITS_ATOMIC */

/* getaddrinfo interface support */
#define HAVE_GETADDRINFO 1

/* reentrant getgrnam */
/* #undef HAVE_GETGRNAM_R */

/* ipv6 host lookup */
#define HAVE_GETHOSTBYNAME2 1

/* have getopt header */
#define HAVE_GETOPT 1

/* Define to 1 if you have the <getopt.h> header file. */
#define HAVE_GETOPT_H 1

/* Define to 1 if you have the `getopt_long' function. */
#define HAVE_GETOPT_LONG 1

/* Define to 1 if you have the `getpagesize' function. */
/* #undef HAVE_GETPAGESIZE */

/* reentrant getnam */
/* #undef HAVE_GETPWNAM_R */

/* reentrant getuid */
/* #undef HAVE_GETPWUID_R */

/* Define to 1 if you have the `gettimeofday' function. */
#define HAVE_GETTIMEOFDAY 1

/* have hires */
/* #undef HAVE_HIRES_TIMER */

/* has inet_aton */
#define HAVE_INET_ATON 1

/* ipv6 support */
#define HAVE_INET_PTON 1

/* inet sockets */
#define HAVE_INET_SOCKETS 1

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Define to 1 if you have the <ioctl.h> header file. */
/* #undef HAVE_IOCTL_H */

/* Define to 1 if you have the <ip_compat.h> header file. */
/* #undef HAVE_IP_COMPAT_H */

/* Define to 1 if you have the <ip_fil_compat.h> header file. */
/* #undef HAVE_IP_FIL_COMPAT_H */

/* Define to 1 if you have the <ip_fil.h> header file. */
/* #undef HAVE_IP_FIL_H */

/* Define to 1 if you have the <ip_nat.h> header file. */
/* #undef HAVE_IP_NAT_H */

/* Define to 1 if you have the `malloc' library (-lmalloc). */
/* #undef HAVE_LIBMALLOC */

/* Define to 1 if you have the <limits.h> header file. */
#define HAVE_LIMITS_H 1

/* Define to 1 if you have the <linux/in6.h> header file. */
#define HAVE_LINUX_IN6_H 1

/* Define to 1 if you have the <linux/netfilter_ipv4.h> header file. */
#define HAVE_LINUX_NETFILTER_IPV4_H 1

/* Define to 1 if you have the <linux/netfilter_ipv6.h> header file. */
#define HAVE_LINUX_NETFILTER_IPV6_H 1

/* reentrant localtime */
#define HAVE_LOCALTIME_R 1

/* Define to 1 if you have the `lockf' function. */
/* #undef HAVE_LOCKF */

/* have long longs */
#define HAVE_LONG_LONG 1

/* Define to 1 if you have the `lstat' function. */
#define HAVE_LSTAT 1

/* mach dybloader */
/* #undef HAVE_MACH_DYLD */

/* Define to 1 if you have the <mach-o/dyld.h> header file. */
/* #undef HAVE_MACH_O_DYLD_H */

/* Define to 1 if you have the `memmove' function. */
#define HAVE_MEMMOVE 1

/* Define to 1 if you have the <memory.h> header file. */
#define HAVE_MEMORY_H 1

/* Define to 1 if you have the `mlock' function. */
#define HAVE_MLOCK 1

/* Define to 1 if you have the `mlockall' function. */
/* #undef HAVE_MLOCKALL */

/* support for plugin modules */
#define HAVE_MODULES 1

/* IPF NAT support */
/* #undef HAVE_NAT_IPF */

/* NetFilter NAT support */
#define HAVE_NAT_NETFILTER 1

/* PF NAT support */
/* #undef HAVE_NAT_PF */

/* Define to 1 if you have the <netinet6/in6.h> header file. */
/* #undef HAVE_NETINET6_IN6_H */

/* Define to 1 if you have the <netinet/inet.h> header file. */
/* #undef HAVE_NETINET_INET_H */

/* Define to 1 if you have the <netinet/in.h> header file. */
#define HAVE_NETINET_IN_H 1

/* Define to 1 if you have the <netinet/in_systm.h> header file. */
#define HAVE_NETINET_IN_SYSTM_H 1

/* Define to 1 if you have the <netinet/ip_compat.h> header file. */
/* #undef HAVE_NETINET_IP_COMPAT_H */

/* Define to 1 if you have the <netinet/ip_fil_compat.h> header file. */
/* #undef HAVE_NETINET_IP_FIL_COMPAT_H */

/* Define to 1 if you have the <netinet/ip_fil.h> header file. */
/* #undef HAVE_NETINET_IP_FIL_H */

/* Define to 1 if you have the <netinet/ip.h> header file. */
#define HAVE_NETINET_IP_H 1

/* Define to 1 if you have the <netinet/ip_nat.h> header file. */
/* #undef HAVE_NETINET_IP_NAT_H */

/* Define to 1 if you have the <net/if.h> header file. */
#define HAVE_NET_IF_H 1

/* Define to 1 if you have the <net/pfvar.h> header file. */
/* #undef HAVE_NET_PFVAR_H */

/* old style iostreams */
#define HAVE_OLD_IOSTREAM 1

/* Define to 1 if you have the `poll' function. */
#define HAVE_POLL 1

/* Define to 1 if you have the <poll.h> header file. */
#define HAVE_POLL_H 1

/* Define to 1 if you have the `posix_memalign' function. */
/* #undef HAVE_POSIX_MEMALIGN */

/* has pwrite */
#define HAVE_PREAD_PWRITE 1

/* has stack size */
#define HAVE_PTHREAD_ATTR_SETSTACKSIZE 1

/* has cancel */
/* #undef HAVE_PTHREAD_CANCEL */

/* has non portable delay */
/* #undef HAVE_PTHREAD_DELAY_NP */

/* posix thread header */
#define HAVE_PTHREAD_H 1

/* has mach link */
/* #undef HAVE_PTHREAD_MACH_THREAD_NP */

/* has non portable setkind */
/* #undef HAVE_PTHREAD_MUTEXATTR_SETKIND_NP */

/* has setttype */
#define HAVE_PTHREAD_MUTEXATTR_SETTYPE 1

/* has non portable settype */
/* #undef HAVE_PTHREAD_MUTEXATTR_SETTYPE_NP */

/* has nanosleep */
#define HAVE_PTHREAD_NANOSLEEP 1

/* Define to 1 if you have the <pthread_np.h> header file. */
/* #undef HAVE_PTHREAD_NP_H */

/* has rwlock support */
#define HAVE_PTHREAD_RWLOCK 1

/* has sched yield */
#define HAVE_PTHREAD_SCHED_YIELD 1

/* has setcancel */
/* #undef HAVE_PTHREAD_SETCANCEL */

/* has setcanceltype */
/* #undef HAVE_PTHREAD_SETCANCELTYPE */

/* has suspend */
/* #undef HAVE_PTHREAD_SUSPEND */

/* has yield */
/* #undef HAVE_PTHREAD_YIELD */

/* has np yield */
/* #undef HAVE_PTHREAD_YIELD_NP */

/* reentrant readdir */
#define HAVE_READDIR_R 1

/* Define to 1 if you have the `realpath' function. */
#define HAVE_REALPATH 1

/* Define to 1 if you have the `sched_getscheduler' function. */
#define HAVE_SCHED_GETSCHEDULER 1

/* Define to 1 if you have the <sched.h> header file. */
#define HAVE_SCHED_H 1

/* Define to 1 if you have the <select.h> header file. */
/* #undef HAVE_SELECT_H */

/* Define to 1 if you have the <semaphore.h> header file. */
#define HAVE_SEMAPHORE_H 1

/* Define to 1 if you have the `setegid' function. */
#define HAVE_SETEGID 1

/* Define to 1 if you have the `setenv' function. */
#define HAVE_SETENV 1

/* Define to 1 if you have the `setitimer' function. */
#define HAVE_SETITIMER 1

/* Define to 1 if you have the `setpgrp' function. */
#define HAVE_SETPGRP 1

/* have shload plugins */
/* #undef HAVE_SHL_LOAD */

/* Define to 1 if you have the `sigaction' function. */
#define HAVE_SIGACTION 1

/* Define to 1 if you have the `sigwait' function. */
#define HAVE_SIGWAIT 1

/* 2 argument form */
#define HAVE_SIGWAIT2 1

/* Define to 1 if you have the `snprintf' function. */
#define HAVE_SNPRINTF 1

/* has socklen_t type */
#define HAVE_SOCKLEN_T 1

/* Define to 1 if you have the <sstream> header file. */
/* #undef HAVE_SSTREAM */
#define HAVE_SSTREAM 1

/* Define to 1 if you have the <ss.h> header file. */
/* #undef HAVE_SS_H */

/* Define to 1 if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the `strcasecmp' function. */
#define HAVE_STRCASECMP 1

/* Define to 1 if you have the `strdup' function. */
#define HAVE_STRDUP 1

/* reentrant strerror */
#define HAVE_STRERROR_R 1

/* Define to 1 if you have the <strings.h> header file. */
#define HAVE_STRINGS_H 1

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* reentrant strtok */
#define HAVE_STRTOK_R 1

/* Define to 1 if you have the <syslog.h> header file. */
#define HAVE_SYSLOG_H 1

/* Define to 1 if you have the <syslog.hposix_evlog.h> header file. */
/* #undef HAVE_SYSLOG_HPOSIX_EVLOG_H */

/* Define to 1 if you have the <sys/atomic.h> header file. */
/* #undef HAVE_SYS_ATOMIC_H */

/* Define to 1 if you have the <sys/atomic_op.h> header file. */
/* #undef HAVE_SYS_ATOMIC_OP_H */

/* Define to 1 if you have the <sys/fcntl.h> header file. */
/* #undef HAVE_SYS_FCNTL_H */

/* Define to 1 if you have the <sys/file.h> header file. */
#define HAVE_SYS_FILE_H 1

/* Define to 1 if you have the <sys/ioctl.h> header file. */
#define HAVE_SYS_IOCTL_H 1

/* solaris endian */
/* #undef HAVE_SYS_ISA_DEFS_H */

/* Define to 1 if you have the <sys/libcsys.h> header file. */
/* #undef HAVE_SYS_LIBCSYS_H */

/* Define to 1 if you have the <sys/param.h> header file. */
#define HAVE_SYS_PARAM_H 1

/* Define to 1 if you have the <sys/poll.h> header file. */
#define HAVE_SYS_POLL_H 1

/* Define to 1 if you have the <sys/sched.h> header file. */
/* #undef HAVE_SYS_SCHED_H */

/* Define to 1 if you have the <sys/select.h> header file. */
#define HAVE_SYS_SELECT_H 1

/* Define to 1 if you have the <sys/socket.h> header file. */
#define HAVE_SYS_SOCKET_H 1

/* Define to 1 if you have the <sys/sockio.h> header file. */
/* #undef HAVE_SYS_SOCKIO_H */

/* Define to 1 if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define to 1 if you have the <sys/stream.h> header file. */
/* #undef HAVE_SYS_STREAM_H */

/* Define to 1 if you have the <sys/time.h> header file. */
#define HAVE_SYS_TIME_H 1

/* have 64 bit longs */
#define HAVE_SYS_TYPES_64 1

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* have systypes */
#define HAVE_SYS_TYPES_STD 1

/* Define to 1 if you have the <sys/un.h> header file. */
/* #undef HAVE_SYS_UN_H */

/* Define to 1 if you have the <sys/wait.h> header file. */
#define HAVE_SYS_WAIT_H 1

/* Define to 1 if you have the <thread.h> header file. */
/* #undef HAVE_THREAD_H */

/* Define to 1 if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* has unix domain sockets */
/* #undef HAVE_UNIX_SOCKETS */

/* Define to 1 if you have the `wait4' function. */
/* #undef HAVE_WAIT4 */

/* Define to 1 if you have the `waitpid' function. */
#define HAVE_WAITPID 1

/* Define to 1 if you have the <winsock2.h> header file. */
/* #undef HAVE_WINSOCK2_H */

/* Define to 1 if you have the <winsock.h> header file. */
/* #undef HAVE_WINSOCK_H */

/* has usable atomic functions */
/* #undef HAVE_WORKING_SYS_ATOMIC_H */

/* have zlib header */
#define HAVE_ZLIB_H 1

/* Define to the sub-directory in which libtool stores uninstalled libraries.
   */
#define LT_OBJDIR ".libs/"

/* Name of package */
#define CCXX_PACKAGE "commoncpp2"

/* Define to the address where bug reports for this package should be sent. */
#define CCXX_PACKAGE_BUGREPORT ""

/* Define to the full name of this package. */
#define CCXX_PACKAGE_NAME ""

/* Define to the full name and version of this package. */
#define CCXX_PACKAGE_STRING ""

/* Define to the one symbol short name of this package. */
#define CCXX_PACKAGE_TARNAME ""

/* Define to the home page for this package. */
#define CCXX_PACKAGE_URL ""

/* Define to the version of this package. */
#define CCXX_PACKAGE_CCXX_VERSION ""

/* mutex type */
#define PTHREAD_MUTEXTYPE_RECURSIVE PTHREAD_MUTEX_RECURSIVE_NP

/* Define as the return type of signal handlers (`int' or `void'). */
#define RETSIGTYPE void

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Define to 1 if you can safely include both <sys/time.h> and <time.h>. */
#define TIME_WITH_SYS_TIME 1

/* use monotonic */
#define USE_MONOTONIC_TIMER 1

/* Enable extensions on AIX 3, Interix.  */
#ifndef _ALL_SOURCE
# define _ALL_SOURCE 1
#endif
/* Enable GNU extensions on systems that have them.  */
#ifndef _GNU_SOURCE
# define _GNU_SOURCE 1
#endif
/* Enable threading extensions on Solaris.  */
#ifndef _POSIX_PTHREAD_SEMANTICS
# define _POSIX_PTHREAD_SEMANTICS 1
#endif
/* Enable extensions on HP NonStop.  */
#ifndef _TANDEM_SOURCE
# define _TANDEM_SOURCE 1
#endif
/* Enable general extensions on Solaris.  */
#ifndef __EXTENSIONS__
# define __EXTENSIONS__ 1
#endif


/* Version number of package */
#define CCXX_VERSION "1.8.1"

/* bsd system using linuxthreads */
/* #undef WITH_LINUXTHREADS */

/* darwin6 environment */
/* #undef _DARWIN6_ */

/* Define to 1 if on MINIX. */
/* #undef _MINIX */

/* Define to 2 if the system does not provide POSIX.1 features except with
   this defined. */
/* #undef _POSIX_1_SOURCE */

/* Define to 1 if you need to in order for `stat' and other things to work. */
/* #undef _POSIX_SOURCE */

/* endian byte order */
/* #undef __BYTE_ORDER */

/* Define to `__inline__' or `__inline' if that's what the C compiler
   calls it, or to nothing if 'inline' is not supported under any name.  */
#ifndef __cplusplus
/* #undef inline */
#endif

/* Define to the equivalent of the C99 'restrict' keyword, or to
   nothing if this is not supported.  Do not define if restrict is
   supported directly.  */
#define restrict __restrict
/* Work around a bug in Sun C++: it does not support _Restrict or
   __restrict__, even though the corresponding Sun C compiler ends up with
   "#define restrict _Restrict" or "#define restrict __restrict__" in the
   previous line.  Perhaps some future version of Sun C++ will work with
   restrict; if so, hopefully it defines __RESTRICT like Sun C does.  */
#if defined __SUNPRO_CC && !defined __RESTRICT
# define _Restrict
# define __restrict__
#endif

/* Define to empty if the keyword `volatile' does not work. Warning: valid
   code using `volatile' can become incorrect without. Disable with care. */
/* #undef volatile */



#ifndef HAVE_STRERROR_R
#define strerror_r(e, b, l) b = ::strerror(e)
#endif

#ifndef HAVE_GETPWUID_R
#define getpwuid_r(uid, rec, buf, size, ptr) ptr = ::getpwuid(uid)
#define getpwnam_r(name, rec, buf, size, ptr) ptr = ::getpwnam(name)
#endif

    


#ifdef HAVE_POLL_H
#include <poll.h>
#else
#ifdef HAVE_SYS_POLL_H
#include <sys/poll.h>
#endif
#endif

#if defined(HAVE_POLL) && defined(POLLRDNORM)
#define USE_POLL
#endif

    



#ifdef HAVE_SYS_LIBCSYS_H
#include <sys/libcsys.h>
#endif

#ifdef HAVE_WINSOCK2_H
#include <winsock2.h>
#else
#ifdef HAVE_WINSOCK_H
#include <winsock.h>
#else
#ifdef HAVE_SYS_SOCKET_H
#include <sys/socket.h>
#ifdef HAVE_SELECT_H
#include <select.h>
#else
#ifdef HAVE_SYS_SELECT_H
#include <sys/select.h>
#endif
#endif

#ifdef HAVE_NETINET_IN_H
#if defined(__hpux) && defined(_XOPEN_SOURCE_EXTENDED)
/* #undef _XOPEN_SOURCE_EXTENDED */
#endif
#include <netinet/in.h>
#ifdef  __hpux
#define _XOPEN_SOURCE_EXTENDED
#endif
#endif
#ifdef HAVE_ARPA_INET_H
#include <arpa/inet.h>
#include <netdb.h>
#endif

#ifdef  HAVE_NETINET6_IN6_H
#include <netinet6/in6.h>
#endif

#ifdef  HAVE_LINIX_IN6_H
#include <linux/in6.h>
#endif

#ifdef HAVE_NETINET_IN_SYSTM_H
#include <netinet/in_systm.h>
#endif
#ifdef HAVE_NETINET_IP_H
#include <netinet/ip.h>
#endif
#ifdef HAVE_SYS_UN_H
#include <sys/un.h>
#endif
#endif
#endif
#endif

#ifndef HAVE_INET_ATON
#define inet_aton(cp, addr) (((*(unsigned long int *)(addr)) = inet_addr(cp)) != -1)
#endif

#ifndef SUN_LEN
#ifdef SCM_RIGHTS
#define HAVE_UN_LEN
#endif
#ifdef __linux__
#define HAVE_UN_LEN
#endif
#ifdef HAVE_UN_LEN
#define SUN_LEN(ptr) sizeof(sockaddr_un.sun_len) + sizeof(sockaddr_un.sun_family) + sizeof(sockaddr_un.sun_path) + 1
#else
#define SUN_LEN(ptr) ((size_t)((struct sockaddr_un *)0)->sun_path) + strlen((ptr)->sun_path))
#endif
#endif

#ifndef _OSF_SOURCE
#ifndef HAVE_SOCKLEN_T
#if defined(i386) && defined(__svr4__)
#define HAVE_SOCKLEN_U
#else
#if defined(__CYGWIN32__)
#define socklen_t int
#else
typedef int socklen_t;
#endif
#endif

#ifdef HAVE_SOCKLEN_U
#if !defined(__CYGWIN32__) && !defined(__MINGW32__)
typedef unsigned socklen_t;
#else
typedef int socklen_t;
#endif
#endif
#endif
#endif

#ifdef  __hpux
#ifdef  mutable
/* #undef mutable */
#endif
#endif

#if defined(AF_INET6) && defined(HAVE_INET_PTON)
#define CCXX_IPV6
#endif

#define CCXX_MULTIFAMILY_IP

    


#ifndef HAVE_BOOL_TYPE
typedef enum { true=1, false=0 } bool;
#endif


    


#ifndef CCXX_EXCEPTIONS
/* disable HAVE_EXCEPTION */
#ifdef  HAVE_EXCEPTION
/* #undef HAVE_EXCEPTION */
#endif
/* throw - replacement to throw an exception */
#define THROW(x) abort()
/* throw - replacement to declare an exception */
#define THROWS(x)
/* throw - for empty list */
#define NEW_THROWS
#define THROWS_EMPTY
/*
 * work around dangeling if/else combinations:
 */
#else
#define THROW(x) throw x
#define THROWS(x) throw(x)
#define NEW_THROWS throw()
#define THROWS_EMPTY throw()
#endif

    


#ifdef CCXX_NAMESPACES
#define USING(x) using namespace x;
#else
#define USING(x)
#endif

#ifdef  __KCC
#define KAI_NONSTD_IOSTREAM 1
#endif
    



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





#ifndef HAVE_FCNTL_H
#ifdef HAVE_SYS_FCNTL_H
#include <sys/fcntl.h>
#endif
#else
#include <fcntl.h>
#ifndef O_NDELAY
#ifdef HAVE_SYS_FCNTL_H
#include <sys/fcntl.h>
#endif
#endif
#endif
    


#if defined(HAVE_ENDIAN_H)
 #include <endian.h>
#elif defined(HAVE_SYS_ISA_DEFS_H)
 #include <sys/isa_defs.h>
 #ifdef _LITTLE_ENDIAN
  #define   __BYTE_ORDER 1234
 #else
  #define   __BYTE_ORDER 4321
 #endif
 #if _ALIGNMENT_REQUIRED > 0
  #define   __BYTE_ALIGNMENT _MAX_ALIGNMENT
 #else
  #define   __BYTE_ALIGNMENT 1
 #endif
#endif

#ifndef __LITTLE_ENDIAN
#define __LITTLE_ENDIAN 1234
#define __BIG_ENDIAN 4321
#endif

#ifndef __BYTE_ORDER
#define __BYTE_ORDER 1234
#endif

#ifndef __BYTE_ALIGNMENT
#if defined(SPARC) || defined(sparc)
#if defined(__arch64__) || defined(__sparcv9)
#define __BYTE_ALIGNMENT 8
#else
#define __BYTE_ALIGNMENT 4
#endif
#endif
#endif

#ifndef __BYTE_ALIGNMENT
#define __BYTE_ALIGNMENT 1
#endif

    


#ifdef HAVE_SIGACTION
#ifdef HAVE_BSD_SIGNAL_H
/* #undef HAVE_BSD_SIGNAL_H */
#endif
#endif

/* Cause problem with Solaris... and perhaps Digital Unix?
  However, the autoconf test in ost_signal defines _POSIX_PTHREAD_SEMANTICS
 when trying to compile sigwait2. */

#ifdef  HAVE_SIGWAIT2
#ifndef _POSIX_PTHREAD_SEMANTICS
#define _POSIX_PTHREAD_SEMANTICS 1
#endif
#endif

#ifdef HAVE_BSD_SIGNAL_H
#include <bsd/signal.h>
#else
#include <signal.h>
#endif
#ifndef SA_ONESHOT
#define SA_ONESHOT SA_RESETHAND
#endif

    


#include <cstring>
#ifdef HAVE_STRINGS_H
#ifndef _AIX
#include <strings.h>
#endif
#endif

#ifdef HAVE_ALLOCA_H
#include <alloca.h>
#endif

#ifndef HAVE_SNPRINTF
#if defined(WIN32) && defined(_MSC_VER) && _MSC_VER < 1400
#define snprintf        _snprintf
#define vsnprintf       _vsnprintf
#endif
#endif

#ifdef HAVE_STRCASECMP
#ifndef stricmp
#define stricmp(x,y) strcasecmp(x,y)
#endif
#ifndef strnicmp
#define strnicmp(x,y,n) strncasecmp(x,y,n)
#endif
#ifndef stristr
#define stristr(x, y) strcasestr(x,y)
#endif
#endif

    


#ifdef HAVE_THREAD_H
#include "/usr/include/thread.h"
#if defined(i386) && defined(__svr4__) && !defined(__sun)
#define _THR_UNIXWARE
#endif
#if defined(__SVR4) && defined(__sun)
#define _THR_SUNOS5
#else
#if defined(__SVR4__) && defined(__SUN__)
#define _THR_SUNOS5
#endif
#endif
#endif

#ifdef HAVE_WORKING_SYS_ATOMIC_H
#include <sys/atomic.h>
#define HAVE_ATOMIC
#elif defined(HAVE_ATOMIC_AIX)
#include <sys/atomic_op.h>
#ifndef HAVE_ATOMIC
#define HAVE_ATOMIC
#endif
#endif

#if defined(__cplusplus)
#if defined(HAVE_GCC_BITS_ATOMIC) || defined(HAVE_GCC_CXX_BITS_ATOMIC)
#include <bits/atomicity.h>
#define HAVE_ATOMIC
#endif
#endif

#if defined(HAVE_PTHREAD_H) && ( defined(_THREAD_SAFE) || defined(_REENTRANT) )

#ifdef  __QNX__
#define __EXT_QNX
#endif

#include <pthread.h>

#ifdef HAVE_PTHREAD_NP_H
#include <pthread_np.h>
#endif

#ifdef HAVE_SEMAPHORE_H
#include <semaphore.h>
#endif
#ifdef _POSIX_PRIORITY_SCHEDULING
#ifdef HAVE_SCHED_H
#include <sched.h>
#else
#ifdef HAVE_SYS_SCHED_H
#include <sys/sched.h>
#endif
#endif
#endif

#define __PTHREAD_H__
#ifndef PTHREAD_MUTEXTYPE_RECURSIVE
#ifdef  MUTEX_TYPE_COUNTING_FAST
#define PTHREAD_MUTEXTYPE_RECURSIVE PTHREAD_MUTEX_RECURSIVE_NP
#endif
#endif
#ifndef PTHREAD_MUTEXTYPE_RECURSIVE
#ifdef  PTHREAD_MUTEX_RECURSIVE
#define PTHREAD_MUTEXTYPE_RECURSIVE PTHREAD_MUTEX_RECURSIVE_NP
#endif
#endif
#ifndef HAVE_PTHREAD_MUTEXATTR_SETTYPE
#if     HAVE_PTHREAD_MUTEXATTR_SETKIND_NP
#ifndef PTHREAD_MUTEXTYPE_RECURSIVE
#define PTHREAD_MUTEXTYPE_RECURSIVE PTHREAD_MUTEX_RECURSIVE_NP
#endif
#define pthread_mutexattr_gettype(x, y) pthread_mutexattr_getkind_np(x, y)
#define pthread_mutexattr_settype(x, y) pthread_mutexattr_setkind_np(x, y)
#endif
#if     HAVE_PTHREAD_MUTEXATTR_SETTYPE_NP
#ifndef PTHREAD_MUTEXTYPE_RECURSIVE
#define PTHREAD_MUTEXTYPE_RECURSIVE PTHREAD_MUTEX_RECURSIVE_NP
#endif
#define pthread_mutexattr_settype(x, y) pthread_mutexattr_settype_np(x, y)
#define pthread_mutexattr_gettype(x, y) pthread_mutexattr_gettype_np(x, y)
#endif
#endif

#ifdef  HAVE_PTHREAD_MACH_THREAD_NP
#define _THR_MACH
#endif

#ifndef HAVE_PTHREAD_YIELD
#ifdef  HAVE_PTHREAD_YIELD_NP
#define pthread_yield() pthread_yield_np()
#define HAVE_PTHREAD_YIELD
#endif
#endif

#ifndef HAVE_PTHREAD_YIELD
#ifdef HAVE_PTHREAD_SCHED_YIELD
#define pthread_yield() sched_yield()
#define HAVE_PTHREAD_YIELD
#endif
#endif

#ifndef HAVE_PTHREAD_DELAY
#ifdef HAVE_PTHREAD_DELAY_NP
#define HAVE_PTHREAD_DELAY
#define pthread_delay(x) pthread_delay_np(x)
#endif
#if defined(HAVE_PTHREAD_NANOSLEEP)
#ifndef HAVE_PTHREAD_DELAY
#define HAVE_PTHREAD_DELAY
#ifdef __FreeBSD__
#ifdef __cplusplus
extern "C" int nanosleep(const struct timespec *rqtp, struct timespec *rmtp);
#endif
#endif
#define pthread_delay(x) nanosleep(x, NULL)
#endif
#endif
#endif

#ifdef HAVE_PTHREAD_ATTR_SETSTACK
#ifndef PTHREAD_STACK_MIN
#define PTHREAD_STACK_MIN 32768
#endif
#endif

#ifndef HAVE_PTHREAD_CANCEL
#ifdef SIGCANCEL
#define CCXX_SIG_THREAD_CANCEL SIGCANCEL
#else
#define CCXX_SIG_THREAD_CANCEL SIGQUIT
#endif
#define pthread_cancel(x) pthread_kill(x, CCXX_SIG_THREAD_CANCEL)
#define pthread_setcanceltype(x, y)
#define pthread_setcancelstate(x, y)
#endif

#ifndef HAVE_PTHREAD_SETCANCELTYPE
#ifdef HAVE_PTHREAD_SETCANCEL
enum
{ PTHREAD_CANCEL_ASYNCHRONOUS = CANCEL_ON,
  PTHREAD_CANCEL_DEFERRED = CANCEL_OFF};
enum
{ PTHREAD_CANCEL_ENABLE = CANCEL_ON,
  PTHREAD_CANCEL_DISABLE = CANCEL_OFF};
#define pthread_setcancelstate(x, y) \
        (y == NULL) ? pthread_setcancel(x) : *y = pthread_setcancel
#define pthread_setcanceltype(x, y) \
        (y == NULL) ? pthread_setasynccancel(x) | *y = pthread_setasynccancel(x)
#else
#define pthread_setcanceltype(x, y)
#define pthread_setcancelstate(x, y)
#endif
#endif

#ifdef  _AIX
#ifdef  HAVE_PTHREAD_SUSPEND
/* #undef HAVE_PTHREAD_SUSPEND */
#endif
#endif

#endif


    
