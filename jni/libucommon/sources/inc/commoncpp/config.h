#ifndef COMMONCPP_CONFIG_H_
#define COMMONCPP_CONFIG_H_

#ifndef _UCOMMON_UCOMMON_H_
#include <ucommon/ucommon.h>
#endif

#ifdef  __EXPORT
#undef  __EXPORT
#endif

#define __EXPORT    __SHARED

// #include <streambuf>
#include <iostream>

#define COMMONCPP_HEADERS
#define CCXX_NAMESPACES
#define COMMONCPP_NAMESPACE ost
#define NAMESPACE_COMMONCPP namespace ost {
#define TIMEOUT_INF ucommon::Timer::inf

#ifdef  _UCOMMON_EXTENDED_
#define CCXX_EXCEPTIONS
#endif

#ifdef  AF_INET6
#define CCXX_IPV6
#endif

#ifdef  AF_INET
#define CCXX_IPV4
#endif

typedef pthread_t   cctid_t;
typedef int8_t      int8;
typedef uint8_t     uint8;
typedef int16_t     int16;
typedef uint16_t    uint16;
typedef int32_t     int32;
typedef uint32_t    uint32;
typedef int64_t     int64;
typedef uint64_t    uint64;

#if !defined(_MSWINDOWS_) && !defined(__QNX__)

/**
 * Convenience function for case insensitive null terminated string compare.
 * @param string1 to compare.
 * @param string2 to compare.
 * @return 0 if equal, > 0 if s2 > s1, < 0 if s2 < s1.
 */
extern "C" inline int stricmp(const char *string1, const char *string2)
    {return ucommon::String::case_compare(string1, string2);}

/**
 * Convenience function for case insensitive null terminated string compare.
 * @param string1 to compare.
 * @param string2 to compare.
 * @param max size of string to compare.
 * @return 0 if equal, > 0 if s2 > s1, < 0 if s2 < s1.
 */
extern "C" inline int strnicmp(const char *string1, const char *string2, size_t max)
    {return ucommon::String::case_compare(string1, string2, max);}

#endif



#endif
