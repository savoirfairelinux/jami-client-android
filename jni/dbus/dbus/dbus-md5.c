/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-md5.c md5 implementation (based on L Peter Deutsch implementation)
 *
 * Copyright (C) 2003 Red Hat Inc.
 * Copyright (C) 1999, 2000 Aladdin Enterprises.  All rights reserved.
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 *
 * L. Peter Deutsch
 * ghost@aladdin.com
 */
/*
 * Independent implementation of MD5 (RFC 1321).
 *
 * This code implements the MD5 Algorithm defined in RFC 1321.
 * It is derived directly from the text of the RFC and not from the
 * reference implementation.
 *
 * The original and principal author of md5.c is L. Peter Deutsch
 * <ghost@aladdin.com>.
 */

#include <config.h>
#include "dbus-internals.h"
#include "dbus-md5.h"
#include <string.h>

/**
 * @defgroup DBusMD5 MD5 implementation
 * @ingroup  DBusInternals
 * @brief MD5 hash
 *
 * Types and functions related to computing MD5 sums.
 */

/**
 * @defgroup DBusMD5Internals MD5 implementation details
 * @ingroup  DBusInternals
 * @brief Internals of MD5 implementation.
 *
 * The implementation of MD5 (see http://www.ietf.org/rfc/rfc1321.txt).
 * This MD5 implementation was written by L. Peter Deutsch and
 * is not derived from the RSA reference implementation in the
 * RFC. The version included in D-Bus comes from the Ghostscript
 * 7.05 distribution.
 *
 * @{
 */
#ifndef DOXYGEN_SHOULD_SKIP_THIS
/*
 * For reference, here is the program that computed the T values.
 */
#ifdef COMPUTE_T_VALUES
#include <math.h>
int
main(int argc, char **argv)
{
  int i;
  for (i = 1; i <= 64; ++i)
    {
      unsigned long v = (unsigned long)(4294967296.0 * fabs(sin((double)i)));

      /*
       * The following nonsense is only to avoid compiler warnings about
       * "integer constant is unsigned in ANSI C, signed with -traditional".
       */
      if (v >> 31)
        {
          printf("#define T%d /* 0x%08lx */ (T_MASK ^ 0x%08lx)\n", i,
                 v, (unsigned long)(unsigned int)(~v));
        } else {
        printf("#define T%d    0x%08lx\n", i, v);
      }
    }
  return 0;
}
#endif /* COMPUTE_T_VALUES */
/*
 * End of T computation program.
 */

#define T_MASK ((dbus_uint32_t)~0)
#define T1 /* 0xd76aa478 */ (T_MASK ^ 0x28955b87)
#define T2 /* 0xe8c7b756 */ (T_MASK ^ 0x173848a9)
#define T3    0x242070db
#define T4 /* 0xc1bdceee */ (T_MASK ^ 0x3e423111)
#define T5 /* 0xf57c0faf */ (T_MASK ^ 0x0a83f050)
#define T6    0x4787c62a
#define T7 /* 0xa8304613 */ (T_MASK ^ 0x57cfb9ec)
#define T8 /* 0xfd469501 */ (T_MASK ^ 0x02b96afe)
#define T9    0x698098d8
#define T10 /* 0x8b44f7af */ (T_MASK ^ 0x74bb0850)
#define T11 /* 0xffff5bb1 */ (T_MASK ^ 0x0000a44e)
#define T12 /* 0x895cd7be */ (T_MASK ^ 0x76a32841)
#define T13    0x6b901122
#define T14 /* 0xfd987193 */ (T_MASK ^ 0x02678e6c)
#define T15 /* 0xa679438e */ (T_MASK ^ 0x5986bc71)
#define T16    0x49b40821
#define T17 /* 0xf61e2562 */ (T_MASK ^ 0x09e1da9d)
#define T18 /* 0xc040b340 */ (T_MASK ^ 0x3fbf4cbf)
#define T19    0x265e5a51
#define T20 /* 0xe9b6c7aa */ (T_MASK ^ 0x16493855)
#define T21 /* 0xd62f105d */ (T_MASK ^ 0x29d0efa2)
#define T22    0x02441453
#define T23 /* 0xd8a1e681 */ (T_MASK ^ 0x275e197e)
#define T24 /* 0xe7d3fbc8 */ (T_MASK ^ 0x182c0437)
#define T25    0x21e1cde6
#define T26 /* 0xc33707d6 */ (T_MASK ^ 0x3cc8f829)
#define T27 /* 0xf4d50d87 */ (T_MASK ^ 0x0b2af278)
#define T28    0x455a14ed
#define T29 /* 0xa9e3e905 */ (T_MASK ^ 0x561c16fa)
#define T30 /* 0xfcefa3f8 */ (T_MASK ^ 0x03105c07)
#define T31    0x676f02d9
#define T32 /* 0x8d2a4c8a */ (T_MASK ^ 0x72d5b375)
#define T33 /* 0xfffa3942 */ (T_MASK ^ 0x0005c6bd)
#define T34 /* 0x8771f681 */ (T_MASK ^ 0x788e097e)
#define T35    0x6d9d6122
#define T36 /* 0xfde5380c */ (T_MASK ^ 0x021ac7f3)
#define T37 /* 0xa4beea44 */ (T_MASK ^ 0x5b4115bb)
#define T38    0x4bdecfa9
#define T39 /* 0xf6bb4b60 */ (T_MASK ^ 0x0944b49f)
#define T40 /* 0xbebfbc70 */ (T_MASK ^ 0x4140438f)
#define T41    0x289b7ec6
#define T42 /* 0xeaa127fa */ (T_MASK ^ 0x155ed805)
#define T43 /* 0xd4ef3085 */ (T_MASK ^ 0x2b10cf7a)
#define T44    0x04881d05
#define T45 /* 0xd9d4d039 */ (T_MASK ^ 0x262b2fc6)
#define T46 /* 0xe6db99e5 */ (T_MASK ^ 0x1924661a)
#define T47    0x1fa27cf8
#define T48 /* 0xc4ac5665 */ (T_MASK ^ 0x3b53a99a)
#define T49 /* 0xf4292244 */ (T_MASK ^ 0x0bd6ddbb)
#define T50    0x432aff97
#define T51 /* 0xab9423a7 */ (T_MASK ^ 0x546bdc58)
#define T52 /* 0xfc93a039 */ (T_MASK ^ 0x036c5fc6)
#define T53    0x655b59c3
#define T54 /* 0x8f0ccc92 */ (T_MASK ^ 0x70f3336d)
#define T55 /* 0xffeff47d */ (T_MASK ^ 0x00100b82)
#define T56 /* 0x85845dd1 */ (T_MASK ^ 0x7a7ba22e)
#define T57    0x6fa87e4f
#define T58 /* 0xfe2ce6e0 */ (T_MASK ^ 0x01d3191f)
#define T59 /* 0xa3014314 */ (T_MASK ^ 0x5cfebceb)
#define T60    0x4e0811a1
#define T61 /* 0xf7537e82 */ (T_MASK ^ 0x08ac817d)
#define T62 /* 0xbd3af235 */ (T_MASK ^ 0x42c50dca)
#define T63    0x2ad7d2bb
#define T64 /* 0xeb86d391 */ (T_MASK ^ 0x14792c6e)
#endif /* !DOXYGEN_SHOULD_SKIP_THIS */

static void
md5_process(DBusMD5Context *context, const unsigned char *data /*[64]*/)
{
  dbus_uint32_t
    a = context->abcd[0], b = context->abcd[1],
    c = context->abcd[2], d = context->abcd[3];
  dbus_uint32_t t;

#ifdef WORDS_BIGENDIAN
  /*
   * On big-endian machines, we must arrange the bytes in the right
   * order.  (This also works on machines of unknown byte order.)
   */
  dbus_uint32_t X[16];
  const unsigned char *xp = data;
  int i;

  for (i = 0; i < 16; ++i, xp += 4)
    X[i] = xp[0] + (xp[1] << 8) + (xp[2] << 16) + (xp[3] << 24);

#else  /* !WORDS_BIGENDIAN */
  /*
   * On little-endian machines, we can process properly aligned data
   * without copying it.
   */
  dbus_uint32_t xbuf[16];
  const dbus_uint32_t *X;

  if (!((data - (const unsigned char *)0) & 3))
    {
      /* data are properly aligned */
      X = (const dbus_uint32_t *)data;
    }
  else
    {
      /* not aligned */
      memcpy(xbuf, data, 64);
      X = xbuf;
    }
#endif

#define ROTATE_LEFT(x, n) (((x) << (n)) | ((x) >> (32 - (n))))

  /* Round 1. */
  /* Let [abcd k s i] denote the operation
     a = b + ((a + F(b,c,d) + X[k] + T[i]) <<< s). */
#define F(x, y, z) (((x) & (y)) | (~(x) & (z)))
#define SET(a, b, c, d, k, s, Ti)               \
  t = a + F(b,c,d) + X[k] + Ti;                 \
  a = ROTATE_LEFT(t, s) + b
  /* Do the following 16 operations. */
  SET(a, b, c, d,  0,  7,  T1);
  SET(d, a, b, c,  1, 12,  T2);
  SET(c, d, a, b,  2, 17,  T3);
  SET(b, c, d, a,  3, 22,  T4);
  SET(a, b, c, d,  4,  7,  T5);
  SET(d, a, b, c,  5, 12,  T6);
  SET(c, d, a, b,  6, 17,  T7);
  SET(b, c, d, a,  7, 22,  T8);
  SET(a, b, c, d,  8,  7,  T9);
  SET(d, a, b, c,  9, 12, T10);
  SET(c, d, a, b, 10, 17, T11);
  SET(b, c, d, a, 11, 22, T12);
  SET(a, b, c, d, 12,  7, T13);
  SET(d, a, b, c, 13, 12, T14);
  SET(c, d, a, b, 14, 17, T15);
  SET(b, c, d, a, 15, 22, T16);
#undef SET

  /* Round 2. */
  /* Let [abcd k s i] denote the operation
     a = b + ((a + G(b,c,d) + X[k] + T[i]) <<< s). */
#define G(x, y, z) (((x) & (z)) | ((y) & ~(z)))
#define SET(a, b, c, d, k, s, Ti)               \
  t = a + G(b,c,d) + X[k] + Ti;                 \
  a = ROTATE_LEFT(t, s) + b
  /* Do the following 16 operations. */
  SET(a, b, c, d,  1,  5, T17);
  SET(d, a, b, c,  6,  9, T18);
  SET(c, d, a, b, 11, 14, T19);
  SET(b, c, d, a,  0, 20, T20);
  SET(a, b, c, d,  5,  5, T21);
  SET(d, a, b, c, 10,  9, T22);
  SET(c, d, a, b, 15, 14, T23);
  SET(b, c, d, a,  4, 20, T24);
  SET(a, b, c, d,  9,  5, T25);
  SET(d, a, b, c, 14,  9, T26);
  SET(c, d, a, b,  3, 14, T27);
  SET(b, c, d, a,  8, 20, T28);
  SET(a, b, c, d, 13,  5, T29);
  SET(d, a, b, c,  2,  9, T30);
  SET(c, d, a, b,  7, 14, T31);
  SET(b, c, d, a, 12, 20, T32);
#undef SET

  /* Round 3. */
  /* Let [abcd k s t] denote the operation
     a = b + ((a + H(b,c,d) + X[k] + T[i]) <<< s). */
#define H(x, y, z) ((x) ^ (y) ^ (z))
#define SET(a, b, c, d, k, s, Ti)               \
  t = a + H(b,c,d) + X[k] + Ti;                 \
  a = ROTATE_LEFT(t, s) + b
  /* Do the following 16 operations. */
  SET(a, b, c, d,  5,  4, T33);
  SET(d, a, b, c,  8, 11, T34);
  SET(c, d, a, b, 11, 16, T35);
  SET(b, c, d, a, 14, 23, T36);
  SET(a, b, c, d,  1,  4, T37);
  SET(d, a, b, c,  4, 11, T38);
  SET(c, d, a, b,  7, 16, T39);
  SET(b, c, d, a, 10, 23, T40);
  SET(a, b, c, d, 13,  4, T41);
  SET(d, a, b, c,  0, 11, T42);
  SET(c, d, a, b,  3, 16, T43);
  SET(b, c, d, a,  6, 23, T44);
  SET(a, b, c, d,  9,  4, T45);
  SET(d, a, b, c, 12, 11, T46);
  SET(c, d, a, b, 15, 16, T47);
  SET(b, c, d, a,  2, 23, T48);
#undef SET

  /* Round 4. */
  /* Let [abcd k s t] denote the operation
     a = b + ((a + I(b,c,d) + X[k] + T[i]) <<< s). */
#define I(x, y, z) ((y) ^ ((x) | ~(z)))
#define SET(a, b, c, d, k, s, Ti)               \
  t = a + I(b,c,d) + X[k] + Ti;                 \
  a = ROTATE_LEFT(t, s) + b
  /* Do the following 16 operations. */
  SET(a, b, c, d,  0,  6, T49);
  SET(d, a, b, c,  7, 10, T50);
  SET(c, d, a, b, 14, 15, T51);
  SET(b, c, d, a,  5, 21, T52);
  SET(a, b, c, d, 12,  6, T53);
  SET(d, a, b, c,  3, 10, T54);
  SET(c, d, a, b, 10, 15, T55);
  SET(b, c, d, a,  1, 21, T56);
  SET(a, b, c, d,  8,  6, T57);
  SET(d, a, b, c, 15, 10, T58);
  SET(c, d, a, b,  6, 15, T59);
  SET(b, c, d, a, 13, 21, T60);
  SET(a, b, c, d,  4,  6, T61);
  SET(d, a, b, c, 11, 10, T62);
  SET(c, d, a, b,  2, 15, T63);
  SET(b, c, d, a,  9, 21, T64);
#undef SET

  /* Then perform the following additions. (That is increment each
     of the four registers by the value it had before this block
     was started.) */
  context->abcd[0] += a;
  context->abcd[1] += b;
  context->abcd[2] += c;
  context->abcd[3] += d;
}

static void
md5_init (DBusMD5Context *context)
{
  context->count[0] = context->count[1] = 0;
  context->abcd[0] = 0x67452301;
  context->abcd[1] = /*0xefcdab89*/ T_MASK ^ 0x10325476;
  context->abcd[2] = /*0x98badcfe*/ T_MASK ^ 0x67452301;
  context->abcd[3] = 0x10325476;
}

static void
md5_append (DBusMD5Context *context, const unsigned char *data, int nbytes)
{
  const unsigned char *p = data;
  int left = nbytes;
  int offset = (context->count[0] >> 3) & 63;
  dbus_uint32_t nbits = (dbus_uint32_t)(nbytes << 3);

  if (nbytes <= 0)
    return;

  /* Update the message length. */
  context->count[1] += nbytes >> 29;
  context->count[0] += nbits;
  if (context->count[0] < nbits)
    context->count[1]++;

  /* Process an initial partial block. */
  if (offset)
    {
      int copy = (offset + nbytes > 64 ? 64 - offset : nbytes);

      memcpy(context->buf + offset, p, copy);
      if (offset + copy < 64)
        return;
      p += copy;
      left -= copy;
      md5_process(context, context->buf);
    }

  /* Process full blocks. */
  for (; left >= 64; p += 64, left -= 64)
    md5_process(context, p);

  /* Process a final partial block. */
  if (left)
    memcpy(context->buf, p, left);
}

static void
md5_finish (DBusMD5Context *context, unsigned char digest[16])
{
  static const unsigned char pad[64] = {
    0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
  };
  unsigned char data[8];
  int i;

  /* Save the length before padding. */
  for (i = 0; i < 8; ++i)
    data[i] = (unsigned char)(context->count[i >> 2] >> ((i & 3) << 3));
  /* Pad to 56 bytes mod 64. */
  md5_append(context, pad, ((55 - (context->count[0] >> 3)) & 63) + 1);
  /* Append the length. */
  md5_append(context, data, 8);
  for (i = 0; i < 16; ++i)
    digest[i] = (unsigned char)(context->abcd[i >> 2] >> ((i & 3) << 3));
}

/** @} */ /* End of internals */

/**
 * @addtogroup DBusMD5
 *
 * @{
 */

/**
 * Initializes the MD5 context.
 *
 * @param context an uninitialized context, typically on the stack.
 */
void
_dbus_md5_init (DBusMD5Context *context)
{
  md5_init (context);
}


/**
 * Feeds more data into an existing md5sum computation.
 *
 * @param context the MD5 context
 * @param data the additional data to hash
 */
void
_dbus_md5_update (DBusMD5Context   *context,
                  const DBusString *data)
{
  unsigned int inputLen;
  unsigned char *input;

  _dbus_string_get_const_data (data, (const char**) &input);
  inputLen = _dbus_string_get_length (data);

  md5_append (context, input, inputLen);
}

/**
 * MD5 finalization. Ends an MD5 message-digest operation, writing the
 * the message digest and zeroing the context.  The results are
 * returned as a raw 16-byte digest, not as the ascii-hex-digits
 * string form of the digest.
 *
 * @param context the MD5 context
 * @param results string to append the 16-byte MD5 digest to
 * @returns #FALSE if not enough memory to append the digest
 *
 */
dbus_bool_t
_dbus_md5_final (DBusMD5Context   *context,
                 DBusString       *results)
{
  unsigned char digest[16];

  md5_finish (context, digest);

  if (!_dbus_string_append_len (results, digest, 16))
    return FALSE;

  /* some kind of security paranoia, though it seems pointless
   * to me given the nonzeroed stuff flying around
   */
  _DBUS_ZERO(*context);

  return TRUE;
}

/**
 * Computes the ASCII hex-encoded md5sum of the given data and
 * appends it to the output string.
 *
 * @param data input data to be hashed
 * @param ascii_output string to append ASCII md5sum to
 * @returns #FALSE if not enough memory
 */
dbus_bool_t
_dbus_md5_compute (const DBusString *data,
                   DBusString       *ascii_output)
{
  DBusMD5Context context;
  DBusString digest;

  _dbus_md5_init (&context);

  _dbus_md5_update (&context, data);

  if (!_dbus_string_init (&digest))
    return FALSE;

  if (!_dbus_md5_final (&context, &digest))
    goto error;

  if (!_dbus_string_hex_encode (&digest, 0, ascii_output,
                                _dbus_string_get_length (ascii_output)))
    goto error;

  _dbus_string_free (&digest);
  
  return TRUE;

 error:
  _dbus_string_free (&digest);
  return FALSE;
}

/** @} */ /* end of exported functions */

#ifdef DBUS_BUILD_TESTS
#include "dbus-test.h"
#include <stdio.h>

static dbus_bool_t
check_md5_binary (const unsigned char *input,
                  int                  input_len,
                  const char          *expected)
{
  DBusString input_str;
  DBusString expected_str;
  DBusString results;

  _dbus_string_init_const_len (&input_str, input, input_len);
  _dbus_string_init_const (&expected_str, expected);

  if (!_dbus_string_init (&results))
    _dbus_assert_not_reached ("no memory for md5 results");

  if (!_dbus_md5_compute (&input_str, &results))
    _dbus_assert_not_reached ("no memory for md5 results");

  if (!_dbus_string_equal (&expected_str, &results))
    {
      const char *s;
      _dbus_string_get_const_data (&results, &s);
      _dbus_warn ("Expected hash %s got %s for md5 sum\n",
                  expected, s);
      _dbus_string_free (&results);
      return FALSE;
    }

  _dbus_string_free (&results);
  return TRUE;
}

static dbus_bool_t
check_md5_str (const char *input,
               const char *expected)
{
  return check_md5_binary (input, strlen (input), expected);
}

/**
 * @ingroup DBusMD5Internals
 * Unit test for MD5 computation.
 *
 * @returns #TRUE on success.
 */
dbus_bool_t
_dbus_md5_test (void)
{
  unsigned char all_bytes[256];
  int i;

  i = 0;
  while (i < 256)
    {
      all_bytes[i] = i;
      ++i;
    }

  if (!check_md5_binary (all_bytes, 256,
                         "e2c865db4162bed963bfaa9ef6ac18f0"))
    return FALSE;

#define CHECK(input,expected) if (!check_md5_str (input, expected)) return FALSE

  CHECK ("", "d41d8cd98f00b204e9800998ecf8427e");
  CHECK ("a", "0cc175b9c0f1b6a831c399e269772661");
  CHECK ("abc", "900150983cd24fb0d6963f7d28e17f72");
  CHECK ("message digest", "f96b697d7cb7938d525a2f31aaf161d0");
  CHECK ("abcdefghijklmnopqrstuvwxyz", "c3fcd3d76192e4007dfb496cca67e13b");
  CHECK ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
         "d174ab98d277d9f5a5611c2c9f419d9f");
  CHECK ("12345678901234567890123456789012345678901234567890123456789012345678901234567890",
         "57edf4a22be3c955ac49da2e2107b67a");

  return TRUE;
}

#endif /* DBUS_BUILD_TESTS */
