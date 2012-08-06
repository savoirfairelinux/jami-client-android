/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* dbus-break-loader.c  Program to find byte streams that break the message loader
 *
 * Copyright (C) 2003  Red Hat Inc.
 *
 * Licensed under the Academic Free License version 2.1
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

#include <config.h>
#include <dbus/dbus.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/wait.h>
#include <string.h>

#define DBUS_COMPILATION 
#include <dbus/dbus-string.h>
#include <dbus/dbus-internals.h>
#include <dbus/dbus-test.h>
#include <dbus/dbus-marshal-basic.h>
#undef DBUS_COMPILATION

static DBusString failure_dir;
static int total_attempts;
static int failures_this_iteration;

static int
random_int_in_range (int start,
                     int end)
{
  /* such elegant math */
  double gap;
  double v_double;
  int v;

  if (start == end)
    return start;

  _dbus_assert (end > start);
  
  gap = end - start - 1; /* -1 to not include "end" */
  v_double = ((double)start) + (((double)rand ())/RAND_MAX) * gap;
  if (v_double < 0.0)
    v = (v_double - 0.5);
  else
    v = (v_double + 0.5);
  
  if (v < start)
    {
      fprintf (stderr, "random_int_in_range() generated %d for range [%d,%d)\n",
               v, start, end);
      v = start;
    }
  else if (v >= end)
    {
      fprintf (stderr, "random_int_in_range() generated %d for range [%d,%d)\n",
               v, start, end);
      v = end - 1;
    }

  /* printf ("  %d of [%d,%d)\n", v, start, end); */
  
  return v;
}

static dbus_bool_t
try_mutated_data (const DBusString *data)
{
  int pid;

  total_attempts += 1;
  /* printf ("  attempt %d\n", total_attempts); */
  
  pid = fork ();

  if (pid < 0)
    {
      fprintf (stderr, "fork() failed: %s\n",
               strerror (errno));
      exit (1);
      return FALSE;
    }

  if (pid == 0)
    {
      /* Child, try loading the data */
      if (!dbus_internal_do_not_use_try_message_data (data, _DBUS_MESSAGE_UNKNOWN))
        exit (1);
      else
        exit (0);
    }
  else
    {
      /* Parent, wait for child */
      int status;
      DBusString filename;
      dbus_bool_t failed;

      if (waitpid (pid, &status, 0) < 0)
        {
          fprintf (stderr, "waitpid() failed: %s\n", strerror (errno));
          exit (1);
          return FALSE;
        }

      failed = FALSE;

      if (!_dbus_string_init (&filename) ||
          !_dbus_string_copy (&failure_dir, 0,
                              &filename, 0) ||
          !_dbus_string_append_byte (&filename, '/'))
        {
          fprintf (stderr, "out of memory\n");
          exit (1);
        }

      _dbus_string_append_int (&filename, total_attempts);

      if (WIFEXITED (status))
        {
          if (WEXITSTATUS (status) != 0)
            {
              _dbus_string_append (&filename, "-exited-");
              _dbus_string_append_int (&filename, WEXITSTATUS (status));
              failed = TRUE;
            }
        }
      else if (WIFSIGNALED (status))
        {
          _dbus_string_append (&filename, "signaled-");
          _dbus_string_append_int (&filename, WTERMSIG (status));
          failed = TRUE;
        }

      if (failed)
        {
          DBusError error;

          _dbus_string_append (&filename, ".message-raw");
          
          printf ("Child failed, writing %s\n", _dbus_string_get_const_data (&filename));

          dbus_error_init (&error);
          if (!_dbus_string_save_to_file (data, &filename, FALSE, &error))
            {
              fprintf (stderr, "Failed to save failed message data: %s\n",
                       error.message);
              dbus_error_free (&error);
              exit (1); /* so we can see the seed that was printed out */
            }

          failures_this_iteration += 1;

	  _dbus_string_free (&filename);
	  
          return FALSE;
        }
      else
	{
	  _dbus_string_free (&filename);	  
	  return TRUE;
	}
    }

  _dbus_assert_not_reached ("should not be reached");
  return TRUE;
}

static void
randomly_shorten_or_lengthen (const DBusString *orig_data,
                              DBusString       *mutated)
{
  int delta;

  if (orig_data != mutated)
    {
      _dbus_string_set_length (mutated, 0);
      
      if (!_dbus_string_copy (orig_data, 0, mutated, 0))
        _dbus_assert_not_reached ("out of mem");
    }

  if (_dbus_string_get_length (mutated) == 0)
    delta = random_int_in_range (0, 10);
  else
    delta = random_int_in_range (- _dbus_string_get_length (mutated),
                                 _dbus_string_get_length (mutated) * 3);
  
  if (delta < 0)
    _dbus_string_shorten (mutated, - delta);
  else if (delta > 0)
    {
      int i = 0;

      i = _dbus_string_get_length (mutated);
      if (!_dbus_string_lengthen (mutated, delta))
        _dbus_assert_not_reached ("couldn't lengthen string");

      while (i < _dbus_string_get_length (mutated))
        {
          _dbus_string_set_byte (mutated,
                                 i,
                                 random_int_in_range (0, 256));
          ++i;
        }
    }
}

static void
randomly_change_one_byte (const DBusString *orig_data,
                          DBusString       *mutated)
{
  int i;

  if (orig_data != mutated)
    {
      _dbus_string_set_length (mutated, 0);
      
      if (!_dbus_string_copy (orig_data, 0, mutated, 0))
        _dbus_assert_not_reached ("out of mem");
    }

  if (_dbus_string_get_length (mutated) == 0)
    return;
  
  i = random_int_in_range (0, _dbus_string_get_length (mutated));

  _dbus_string_set_byte (mutated, i,
                         random_int_in_range (0, 256));
}

static void
randomly_remove_one_byte (const DBusString *orig_data,
                          DBusString       *mutated)
{
  int i;

  if (orig_data != mutated)
    {
      _dbus_string_set_length (mutated, 0);
      
      if (!_dbus_string_copy (orig_data, 0, mutated, 0))
        _dbus_assert_not_reached ("out of mem");
    }

  if (_dbus_string_get_length (mutated) == 0)
    return;
  
  i = random_int_in_range (0, _dbus_string_get_length (mutated));

  _dbus_string_delete (mutated, i, 1);
}


static void
randomly_add_one_byte (const DBusString *orig_data,
                       DBusString       *mutated)
{
  int i;

  if (orig_data != mutated)
    {
      _dbus_string_set_length (mutated, 0);
      
      if (!_dbus_string_copy (orig_data, 0, mutated, 0))
        _dbus_assert_not_reached ("out of mem");
    }

  i = random_int_in_range (0, _dbus_string_get_length (mutated));

  _dbus_string_insert_bytes (mutated, i, 1,
			     random_int_in_range (0, 256));
}

static void
randomly_modify_length (const DBusString *orig_data,
                        DBusString       *mutated)
{
  int i;
  int byte_order;
  const char *d;
  dbus_uint32_t orig;
  int delta;
  
  if (orig_data != mutated)
    {
      _dbus_string_set_length (mutated, 0);
      
      if (!_dbus_string_copy (orig_data, 0, mutated, 0))
        _dbus_assert_not_reached ("out of mem");
    }

  if (_dbus_string_get_length (mutated) < 12)
    return;
  
  d = _dbus_string_get_const_data (mutated);

  if (!(*d == DBUS_LITTLE_ENDIAN ||
        *d == DBUS_BIG_ENDIAN))
    return;

  byte_order = *d;
  
  i = random_int_in_range (4, _dbus_string_get_length (mutated) - 8);
  i = _DBUS_ALIGN_VALUE (i, 4);

  orig = _dbus_demarshal_uint32 (mutated, byte_order, i, NULL);

  delta = random_int_in_range (-10, 10);  
  
  _dbus_marshal_set_uint32 (mutated, byte_order, i,
                            (unsigned) (orig + delta));
}

static void
randomly_set_extreme_ints (const DBusString *orig_data,
                           DBusString       *mutated)
{
  int i;
  int byte_order;
  const char *d;
  dbus_uint32_t orig;
  static int which = 0;
  unsigned int extreme_ints[] = {
    _DBUS_INT_MAX,
    _DBUS_UINT_MAX,
    _DBUS_INT_MAX - 1,
    _DBUS_UINT_MAX - 1,
    _DBUS_INT_MAX - 2,
    _DBUS_UINT_MAX - 2,
    _DBUS_INT_MAX - 17,
    _DBUS_UINT_MAX - 17,
    _DBUS_INT_MAX / 2,
    _DBUS_INT_MAX / 3,
    _DBUS_UINT_MAX / 2,
    _DBUS_UINT_MAX / 3,
    0, 1, 2, 3,
    (unsigned int) -1,
    (unsigned int) -2,
    (unsigned int) -3
  };
    
  if (orig_data != mutated)
    {
      _dbus_string_set_length (mutated, 0);
      
      if (!_dbus_string_copy (orig_data, 0, mutated, 0))
        _dbus_assert_not_reached ("out of mem");
    }

  if (_dbus_string_get_length (mutated) < 12)
    return;
  
  d = _dbus_string_get_const_data (mutated);

  if (!(*d == DBUS_LITTLE_ENDIAN ||
        *d == DBUS_BIG_ENDIAN))
    return;

  byte_order = *d;
  
  i = random_int_in_range (4, _dbus_string_get_length (mutated) - 8);
  i = _DBUS_ALIGN_VALUE (i, 4);

  orig = _dbus_demarshal_uint32 (mutated, byte_order, i, NULL);

  which = random_int_in_range (0, _DBUS_N_ELEMENTS (extreme_ints));

  _dbus_assert (which >= 0);
  _dbus_assert (which < _DBUS_N_ELEMENTS (extreme_ints));
  
  _dbus_marshal_set_uint32 (mutated, byte_order, i,
                            extreme_ints[which]);
}

static int
random_type (void)
{
  const char types[] = {
    DBUS_TYPE_INVALID,
    DBUS_TYPE_NIL,
    DBUS_TYPE_BYTE,
    DBUS_TYPE_BOOLEAN,
    DBUS_TYPE_INT32,
    DBUS_TYPE_UINT32,
    DBUS_TYPE_INT64,
    DBUS_TYPE_UINT64,
    DBUS_TYPE_DOUBLE,
    DBUS_TYPE_STRING,
    DBUS_TYPE_CUSTOM,
    DBUS_TYPE_ARRAY,
    DBUS_TYPE_DICT,
    DBUS_TYPE_OBJECT_PATH
  };

  _dbus_assert (_DBUS_N_ELEMENTS (types) == DBUS_NUMBER_OF_TYPES + 1);

  return types[ random_int_in_range (0, _DBUS_N_ELEMENTS (types)) ];
}

static void
randomly_change_one_type (const DBusString *orig_data,
                          DBusString       *mutated)
{
  int i;
  int len;
  
  if (orig_data != mutated)
    {
      _dbus_string_set_length (mutated, 0);
      
      if (!_dbus_string_copy (orig_data, 0, mutated, 0))
        _dbus_assert_not_reached ("out of mem");
    }

  if (_dbus_string_get_length (mutated) == 0)
    return;

  len = _dbus_string_get_length (mutated);
  i = random_int_in_range (0, len);

  /* Look for a type starting at a random location,
   * and replace with a different type
   */
  while (i < len)
    {
      int b;
      b = _dbus_string_get_byte (mutated, i);
      if (_dbus_type_is_valid (b))
        {
          _dbus_string_set_byte (mutated, i, random_type ());
          return;
        }
      ++i;
    }
}

static int times_we_did_each_thing[7] = { 0, };

static void
randomly_do_n_things (const DBusString *orig_data,
                      DBusString       *mutated,
                      int               n)
{
  int i;
  void (* functions[]) (const DBusString *orig_data,
                        DBusString       *mutated) =
    {
      randomly_shorten_or_lengthen,
      randomly_change_one_byte,
      randomly_add_one_byte,
      randomly_remove_one_byte,
      randomly_modify_length,
      randomly_set_extreme_ints,
      randomly_change_one_type
    };

  _dbus_string_set_length (mutated, 0);

  if (!_dbus_string_copy (orig_data, 0, mutated, 0))
    _dbus_assert_not_reached ("out of mem");

  i = 0;
  while (i < n)
    {
      int which;

      which = random_int_in_range (0, _DBUS_N_ELEMENTS (functions));

      (* functions[which]) (mutated, mutated);
      times_we_did_each_thing[which] += 1;
      
      ++i;
    }
}

static dbus_bool_t
find_breaks_based_on (const DBusString   *filename,
                      dbus_bool_t         is_raw,
                      DBusMessageValidity expected_validity,
                      void               *data)
{
  DBusString orig_data;
  DBusString mutated;
  const char *filename_c;
  dbus_bool_t retval;
  int i;

  filename_c = _dbus_string_get_const_data (filename);

  retval = FALSE;

  if (!_dbus_string_init (&orig_data))
    _dbus_assert_not_reached ("could not allocate string\n");

  if (!_dbus_string_init (&mutated))
    _dbus_assert_not_reached ("could not allocate string\n");

  if (!dbus_internal_do_not_use_load_message_file (filename, is_raw,
                                                   &orig_data))
    {
      fprintf (stderr, "could not load file %s\n", filename_c);
      goto failed;
    }

  printf ("        changing one random byte 100 times\n");
  i = 0;
  while (i < 100)
    {
      randomly_change_one_byte (&orig_data, &mutated);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        changing length 50 times\n");
  i = 0;
  while (i < 50)
    {
      randomly_modify_length (&orig_data, &mutated);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        removing one byte 50 times\n");
  i = 0;
  while (i < 50)
    {
      randomly_remove_one_byte (&orig_data, &mutated);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        adding one byte 50 times\n");
  i = 0;
  while (i < 50)
    {
      randomly_add_one_byte (&orig_data, &mutated);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        changing ints to boundary values 50 times\n");
  i = 0;
  while (i < 50)
    {
      randomly_set_extreme_ints (&orig_data, &mutated);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        changing typecodes 50 times\n");
  i = 0;
  while (i < 50)
    {
      randomly_change_one_type (&orig_data, &mutated);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        changing message length 15 times\n");
  i = 0;
  while (i < 15)
    {
      randomly_shorten_or_lengthen (&orig_data, &mutated);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        randomly making 2 of above modifications 42 times\n");
  i = 0;
  while (i < 42)
    {
      randomly_do_n_things (&orig_data, &mutated, 2);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        randomly making 3 of above modifications 42 times\n");
  i = 0;
  while (i < 42)
    {
      randomly_do_n_things (&orig_data, &mutated, 3);
      try_mutated_data (&mutated);

      ++i;
    }

  printf ("        randomly making 4 of above modifications 42 times\n");
  i = 0;
  while (i < 42)
    {
      randomly_do_n_things (&orig_data, &mutated, 4);
      try_mutated_data (&mutated);

      ++i;
    }
  
  retval = TRUE;
  
 failed:

  _dbus_string_free (&orig_data);
  _dbus_string_free (&mutated);

  /* FALSE means end the whole process */
  return retval;
}

static unsigned int
get_random_seed (void)
{
  DBusString bytes;
  unsigned int seed;
  int fd;
  const char *s;

  seed = 0;

  if (!_dbus_string_init (&bytes))
    exit (1);

  fd = open ("/dev/urandom", O_RDONLY);
  if (fd < 0)
    goto use_fallback;

  if (_dbus_read (fd, &bytes, 4) != 4)
    goto use_fallback;

  close (fd);

  s = _dbus_string_get_const_data (&bytes);

  seed = * (unsigned int*) s;
  goto out;

 use_fallback:
  {
    long tv_usec;

    fprintf (stderr, "could not open/read /dev/urandom, using current time for seed\n");

    _dbus_get_current_time (NULL, &tv_usec);

    seed = tv_usec;
  }

 out:
  _dbus_string_free (&bytes);

  return seed;
}

int
main (int    argc,
      char **argv)
{
  const char *test_data_dir;
  const char *failure_dir_c;
  int total_failures_found;
  
  if (argc > 1)
    test_data_dir = argv[1];
  else
    {
      fprintf (stderr, "Must specify a top_srcdir/test/data directory\n");
      return 1;
    }

  total_failures_found = 0;
  total_attempts = 0;

  if (!_dbus_string_init (&failure_dir))
    return 1;

  /* so you can leave it overnight safely */
#define MAX_FAILURES 1000

  while (total_failures_found < MAX_FAILURES)
    {
      unsigned int seed;

      failures_this_iteration = 0;

      seed = get_random_seed ();

      _dbus_string_set_length (&failure_dir, 0);

      if (!_dbus_string_append (&failure_dir, "failures-"))
        return 1;

      if (!_dbus_string_append_uint (&failure_dir, seed))
        return 1;

      failure_dir_c = _dbus_string_get_const_data (&failure_dir);

      if (mkdir (failure_dir_c, 0700) < 0)
        {
          if (errno != EEXIST)
            fprintf (stderr, "didn't mkdir %s: %s\n",
                     failure_dir_c, strerror (errno));
        }

      printf ("next seed = %u \ttotal failures %d of %d attempts\n",
              seed, total_failures_found, total_attempts);

      srand (seed);

      if (!dbus_internal_do_not_use_foreach_message_file (test_data_dir,
                                                          find_breaks_based_on,
                                                          NULL))
        {
          fprintf (stderr, "fatal error iterating over message files\n");
          rmdir (failure_dir_c);
          return 1;
        }

      printf ("  did %d random mutations: %d %d %d %d %d %d %d\n",
              _DBUS_N_ELEMENTS (times_we_did_each_thing),
              times_we_did_each_thing[0],
              times_we_did_each_thing[1],
              times_we_did_each_thing[2],
              times_we_did_each_thing[3],
              times_we_did_each_thing[4],
              times_we_did_each_thing[5],
              times_we_did_each_thing[6]);
      
      printf ("Found %d failures with seed %u stored in %s\n",
              failures_this_iteration, seed, failure_dir_c);

      total_failures_found += failures_this_iteration;

      rmdir (failure_dir_c); /* does nothing if non-empty */
    }

  return 0;
}
