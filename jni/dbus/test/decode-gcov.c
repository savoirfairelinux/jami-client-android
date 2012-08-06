 /* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* decode-gcov.c gcov decoder program
 *
 * Copyright (C) 2003  Red Hat Inc.
 *
 * Partially derived from gcov,
 * Copyright (C) 1990, 1991, 1992, 1993, 1994, 1996, 1997, 1998,
 * 1999, 2000, 2001, 2002 Free Software Foundation, Inc.
 *
 * This file is NOT licensed under the Academic Free License
 * as it is largely derived from gcov.c and gcov-io.h in the
 * gcc source code.
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
#define DBUS_COMPILATION /* cheat */
#include <dbus/dbus-list.h>
#include <dbus/dbus-string.h>
#include <dbus/dbus-sysdeps.h>
#include <dbus/dbus-marshal.h>
#include <dbus/dbus-hash.h>
#undef DBUS_COMPILATION
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef DBUS_HAVE_INT64
#error "gcov support can't be built without 64-bit integer support"
#endif

static void
die (const char *message)
{
  fprintf (stderr, "%s", message);
  exit (1);
}

/* This bizarro function is from gcov-io.h in gcc source tree */
static int
fetch_long (long        *dest,
            const char  *source,
            size_t       bytes)
{
  long value = 0;
  int i;
                                                                                
  for (i = bytes - 1; (size_t) i > (sizeof (*dest) - 1); i--)
    if (source[i] & ((size_t) i == (bytes - 1) ? 127 : 255 ))
      return 1;
                                                                                
  for (; i >= 0; i--)
    value = value * 256 + (source[i] & ((size_t)i == (bytes - 1) ? 127 : 255));
                                                                                
  if ((source[bytes - 1] & 128) && (value > 0))
    value = - value;
                                                                                
  *dest = value;
  return 0;
}

static int
fetch_long64 (dbus_int64_t *dest,
              const char   *source,
              size_t        bytes)
{
  dbus_int64_t value = 0;
  int i;
                                                                                
  for (i = bytes - 1; (size_t) i > (sizeof (*dest) - 1); i--)
    if (source[i] & ((size_t) i == (bytes - 1) ? 127 : 255 ))
      return 1;
                                                                                
  for (; i >= 0; i--)
    value = value * 256 + (source[i] & ((size_t)i == (bytes - 1) ? 127 : 255));
                                                                                
  if ((source[bytes - 1] & 128) && (value > 0))
    value = - value;
                                                                                
  *dest = value;
  return 0;
}

#define BB_FILENAME 	(-1)
#define BB_FUNCTION 	(-2)
#define BB_ENDOFLIST	0

static dbus_bool_t
string_get_int (const DBusString *str,
                int               start,
                long             *val)
{
  const char *p;
  
  if ((_dbus_string_get_length (str) - start) < 4)
    return FALSE;

  p = _dbus_string_get_const_data (str);

  p += start;

  fetch_long (val, p, 4);
  
  return TRUE;
}

static dbus_bool_t
string_get_int64 (const DBusString *str,
                  int               start,
                  dbus_int64_t     *val)
{
  const char *p;
  
  if ((_dbus_string_get_length (str) - start) < 8)
    return FALSE;

  p = _dbus_string_get_const_data (str);

  p += start;

  fetch_long64 (val, p, 8);
  
  return TRUE;
}

static dbus_bool_t
string_get_string (const DBusString *str,
                   int               start,
                   long              terminator,
                   DBusString       *val,
                   int              *end)
{
  int i;
  long n;
  
  i = start;
  while (string_get_int (str, i, &n))
    {
      unsigned char b;
      
      i += 4;
      
      if (n == terminator)
        break;

      b = n & 0xff;
      if (b)
        {
          _dbus_string_append_byte (val, b);
          b = (n >> 8) & 0xff;
          if (b)
            {
              _dbus_string_append_byte (val, b);
              b = (n >> 16) & 0xff;
              if (b)
                {
                  _dbus_string_append_byte (val, b);
                  b = (n >> 24) & 0xff;
                  if (b)
                    _dbus_string_append_byte (val, b);
                }
            }
        }
    }

  *end = i;
  
  return TRUE;
}

#ifdef DBUS_HAVE_GCC33_GCOV
/* In gcc33 .bbg files, there's a function name of the form:
 *   -1, length, name (padded to 4), -1, checksum
 */
static dbus_bool_t
string_get_function (const DBusString *str,
                     int               start,
                     DBusString       *funcname,
                     int              *checksum,
                     int              *next)
{
  int end;
  long val;
  int i;

  i = start;
  
  if (!string_get_int (str, i, &val))
    die ("no room for -1 before function name\n");
        
  i += 4;

  if (val != -1)
    die ("value before function name is not -1\n");
  
  if (!string_get_int (str, i, &val))
    die ("no length found for function name\n");
        
  i += 4;

  end = i + val;
  if (end > _dbus_string_get_length (str))
    die ("Function name length points past end of file\n");

  if (!_dbus_string_append (funcname,
                            _dbus_string_get_const_data (str) + i))
    die ("no memory\n");
        
  /* skip alignment padding the length doesn't include the nul so add 1
   */
  i = _DBUS_ALIGN_VALUE (end + 1, 4);
        
  if (!string_get_int (str, i, &val) ||
      val != -1)
    die ("-1 at end of function name not found\n");
        
  i += 4;

  if (!string_get_int (str, i, &val))
    die ("no checksum found at end of function name\n");
        
  i += 4;

  *checksum = val;

  *next = i;

  return TRUE;
}
#endif /* DBUS_HAVE_GCC33_GCOV */

static void
dump_bb_file (const DBusString *contents)
{
  int i;
  long val;
  int n_functions;

  n_functions = 0;
  i = 0;
  while (string_get_int (contents, i, &val))
    {
      i += 4;
      
      switch (val)
        {
        case BB_FILENAME:
          {
            DBusString f;

            if (!_dbus_string_init (&f))
              die ("no memory\n");

            if (string_get_string (contents, i,
                                   BB_FILENAME,
                                   &f, &i))
              {
                printf ("File %s\n", _dbus_string_get_const_data (&f));
              }
            _dbus_string_free (&f);
          }
          break;
        case BB_FUNCTION:
          {
            DBusString f;
            if (!_dbus_string_init (&f))
              die ("no memory\n");

            if (string_get_string (contents, i,
                                   BB_FUNCTION,
                                   &f, &i))
              {
                printf ("Function %s\n", _dbus_string_get_const_data (&f));
              }
            _dbus_string_free (&f);

            n_functions += 1;
          }
          break;
        case BB_ENDOFLIST:
          printf ("End of block\n");
          break;
        default:
          printf ("Line %ld\n", val);
          break;
        }
    }

  printf ("%d functions in file\n", n_functions);
}

#define FLAG_ON_TREE 0x1
#define FLAG_FAKE 0x2
#define FLAG_FALL_THROUGH 0x4

static void
dump_bbg_file (const DBusString *contents)
{
  int i;
  long val;
  int n_functions;
  int n_arcs;
  int n_blocks;
  int n_arcs_off_tree;
  
  n_arcs_off_tree = 0;
  n_blocks = 0;
  n_arcs = 0;
  n_functions = 0;
  i = 0;
  while (i < _dbus_string_get_length (contents))
    {
      long n_blocks_in_func;
      long n_arcs_in_func; 
      int j;

#ifdef DBUS_HAVE_GCC33_GCOV
      /* In gcc33 .bbg files, there's a function name of the form:
       *   -1, length, name (padded to 4), -1, checksum
       * after that header on each function description, it's
       * the same as in gcc32
       */

      {
        DBusString funcname;
        int checksum;
        
        if (!_dbus_string_init (&funcname))
          die ("no memory\n");

        if (!string_get_function (contents, i,
                                  &funcname, &checksum, &i))
          die ("could not read function name\n");
        
        printf ("Function name is \"%s\" checksum %d\n",
                _dbus_string_get_const_data (&funcname),
                checksum);
        
        _dbus_string_free (&funcname);
      }
#endif /* DBUS_HAVE_GCC33_GCOV */
      
      if (!string_get_int (contents, i, &val))
        die ("no count of blocks in func found\n");
      
      i += 4;
      
      n_blocks_in_func = val;

      if (!string_get_int (contents, i, &n_arcs_in_func))
        break;

      i += 4;

      printf ("Function has %ld blocks and %ld arcs\n",
              n_blocks_in_func, n_arcs_in_func);

      n_functions += 1;
      n_blocks += n_blocks_in_func;
      n_arcs += n_arcs_in_func;
      
      j = 0;
      while (j < n_blocks_in_func)
        {
          long n_arcs_in_block;
          int k;
          
          if (!string_get_int (contents, i, &n_arcs_in_block))
            break;

          i += 4;

          printf ("  Block has %ld arcs\n", n_arcs_in_block);
          
          k = 0;
          while (k < n_arcs_in_block)
            {
              long destination_block;
              long flags;
              
              if (!string_get_int (contents, i, &destination_block))
                break;

              i += 4;
              
              if (!string_get_int (contents, i, &flags))
                break;

              i += 4;

              printf ("    Arc has destination block %ld flags 0x%lx\n",
                      destination_block, flags);

              if ((flags & FLAG_ON_TREE) == 0)
                n_arcs_off_tree += 1;
              
              ++k;
            }

          if (k < n_arcs_in_block)
            break;
          
          ++j;
        }

      if (j < n_blocks_in_func)
        break;

      if (!string_get_int (contents, i, &val))
        break;

      i += 4;

      if (val != -1)
        die ("-1 separator not found\n");
    }

  printf ("%d functions %d blocks %d arcs %d off-tree arcs in file\n",
          n_functions, n_blocks, n_arcs, n_arcs_off_tree);
}

#ifndef DBUS_HAVE_GCC33_GCOV

/* gcc 3.2 version:
 * The da file contains first a count of arcs in the file,
 * then a count of executions for all "off tree" arcs
 * in the file.
 */
static void
dump_da_file (const DBusString *contents)
{
  int i;
  dbus_int64_t val;
  int n_arcs;
  int claimed_n_arcs;

  i = 0;
  if (!string_get_int64 (contents, i, &val))
    return;

  i += 8;
  
  printf ("%ld arcs in file\n", (long) val);
  claimed_n_arcs = val;
  
  n_arcs = 0;
  while (string_get_int64 (contents, i, &val))
    {
      i += 8;

      printf ("%ld executions of arc %d\n",
              (long) val, n_arcs);

      ++n_arcs;
    }

  if (n_arcs != claimed_n_arcs)
    {
      printf ("File claimed to have %d arcs but only had %d\n",
              claimed_n_arcs, n_arcs);
    }
}

#else /* DBUS_HAVE_GCC33_GCOV */

/* gcc 3.3 version:
 * The da file is more complex than 3.2.
 *
 * We have a magic value of "-123" only it isn't really
 * -123, it's -123 as encoded by the crackass gcov-io.h
 * routines. Anyway, 4 bytes.
 *
 * We then have:
 *
 *   - 4 byte count of how many functions in the following list
 *   - 4 byte length of random extra data
 *   - the random extra data, just skip it, info pages have some
 *     details on what might be in there or see __bb_exit_func in gcc
 *   - then for each function (number of functions given above):
 *     . -1, length, funcname, alignment padding, -1
 *     . checksum
 *     . 4 byte number of arcs in function
 *     . 8 bytes each, a count of execution for each arc
 *
 * Now, the whole thing *starting with the magic* can repeat.
 * This is caused by multiple runs of the profiled app appending
 * to the file.
 */
static void
dump_da_file (const DBusString *contents)
{
  int i;
  dbus_int64_t v64;
  long val;
  int n_sections;
  int total_functions;

  total_functions = 0;
  n_sections = 0;

  i = 0;
  while (i < _dbus_string_get_length (contents))
    {
      int claimed_n_functions;
      int n_functions;
      int total_arcs;

      printf (".da file section %d\n", n_sections);
      
      if (!string_get_int (contents, i, &val))
        die ("no magic found in .da file\n");

      i += 4;

      if (val != -123)
        die ("wrong file magic in .da file\n");

      if (!string_get_int (contents, i, &val))
        die ("no function count in .da file\n");
      i += 4;
      claimed_n_functions = val;

      printf ("%d functions expected in section %d of .da file\n",
              claimed_n_functions, n_sections);
      
      if (!string_get_int (contents, i, &val))
        die ("no extra data length in .da file\n");

      i += 4;

      i += val;

      total_arcs = 0;
      n_functions = 0;
      while (n_functions < claimed_n_functions)
        {
          DBusString funcname;
          int checksum;
          int claimed_n_arcs;
          int n_arcs;
          
          if (!_dbus_string_init (&funcname))
            die ("no memory\n");
          
          if (!string_get_function (contents, i,
                                    &funcname, &checksum, &i))
            die ("could not read function name\n");
          
          if (!string_get_int (contents, i, &val))
            die ("no arc count for function\n");
          
          i += 4;
          claimed_n_arcs = val;
          
          printf ("  %d arcs in function %d %s checksum %d\n",
                  claimed_n_arcs, n_functions,
                  _dbus_string_get_const_data (&funcname),
                  checksum);
          
          n_arcs = 0;
          while (n_arcs < claimed_n_arcs)
            {
              if (!string_get_int64 (contents, i, &v64))
                die ("did not get execution count for arc\n");
              
              i += 8;
              
              printf ("    %ld executions of arc %d (total arcs %d)\n",
                      (long) v64, n_arcs, total_arcs + n_arcs);
              
              ++n_arcs;
            }

          _dbus_string_free (&funcname);

          total_arcs += n_arcs;
          ++n_functions;
        }

      printf ("total of %d functions and %d arcs in section %d\n",
              n_functions, total_arcs, n_sections);
      
      total_functions += n_functions;
      ++n_sections;
    }

  printf ("%d total function sections in %d total .da file sections\n",
          total_functions, n_sections);
}

#endif /* DBUS_HAVE_GCC33_GCOV */

typedef struct Arc Arc;
typedef struct Block Block;
typedef struct Function Function;
typedef struct File File;
typedef struct Line Line;

struct Arc
{
  int source;
  int target;
  dbus_int64_t arc_count;
  unsigned int count_valid : 1;
  unsigned int on_tree : 1;
  unsigned int fake : 1;
  unsigned int fall_through : 1;
  Arc *pred_next;
  Arc *succ_next;
};

struct Block
{
  Arc *succ;
  Arc *pred;
  dbus_int64_t succ_count;
  dbus_int64_t pred_count;
  dbus_int64_t exec_count;
  DBusList *lines;
  unsigned int count_valid : 1;
  unsigned int on_tree : 1;
  unsigned int inside_dbus_build_tests : 1;
};

struct Function
{
  char *name;
  int checksum;
  Block *block_graph;
  int n_blocks;
  /* number of blocks in DBUS_BUILD_TESTS */
  int n_test_blocks;
  int n_test_blocks_executed;
  /* number of blocks outside DBUS_BUILD_TESTS */
  int n_nontest_blocks;
  int n_nontest_blocks_executed;
  /* Summary result flags */
  unsigned int unused : 1;
  unsigned int inside_dbus_build_tests : 1;
  unsigned int partial : 1; /* only some of the blocks were executed */
};

struct Line
{
  int    number;
  char  *text;
  DBusList *blocks;
  unsigned int inside_dbus_build_tests : 1;
  unsigned int partial : 1; /* only some of the blocks were executed */
};

struct File
{
  char *name;
  Line *lines;
  int   n_lines;
  DBusList *functions;
};

static void
function_add_arc (Function *function,
                  long      source,
                  long      target,
                  long      flags)
{
  Arc *arc;

  arc = dbus_new0 (Arc, 1);
  if (arc == NULL)
    die ("no memory\n");
  
  arc->target = target;
  arc->source = source;

  arc->succ_next = function->block_graph[source].succ;
  function->block_graph[source].succ = arc;
  function->block_graph[source].succ_count += 1;

  arc->pred_next = function->block_graph[target].pred;
  function->block_graph[target].pred = arc;
  function->block_graph[target].pred_count += 1;

  if ((flags & FLAG_ON_TREE) != 0)
    arc->on_tree = TRUE;

  if ((flags & FLAG_FAKE) != 0)
    arc->fake = TRUE;

  if ((flags & FLAG_FALL_THROUGH) != 0)
    arc->fall_through = TRUE;
}


static Arc*
reverse_arcs (Arc *arc)
{
  struct Arc *prev = 0;
  struct Arc *next;

  for ( ; arc; arc = next)
    {
      next = arc->succ_next;
      arc->succ_next = prev;
      prev = arc;
    }

  return prev;
}

static void
function_reverse_succ_arcs (Function *func)
{
  /* Must reverse the order of all succ arcs, to ensure that they match
   * the order of the data in the .da file.
   */
  int i;
  
  for (i = 0; i < func->n_blocks; i++)
    if (func->block_graph[i].succ)
      func->block_graph[i].succ = reverse_arcs (func->block_graph[i].succ);
}

static void
get_functions_from_bbg (const DBusString  *contents,
                        DBusList         **functions)
{
  int i;
  long val;
  int n_functions;
  int n_arcs;
  int n_blocks;
  int n_arcs_off_tree;

#if 0
  printf ("Loading arcs and blocks from .bbg file\n");
#endif
  
  n_arcs_off_tree = 0;
  n_blocks = 0;
  n_arcs = 0;
  n_functions = 0;
  i = 0;
  while (i < _dbus_string_get_length (contents))
    {
      Function *func;
      long n_blocks_in_func;
      long n_arcs_in_func; 
      int j;

#ifdef DBUS_HAVE_GCC33_GCOV
      DBusString funcname;
      int checksum;

      /* In gcc33 .bbg files, there's a function name of the form:
       *   -1, length, name (padded to 4), -1, checksum
       * after that header on each function description, it's
       * the same as in gcc32
       */
      if (!_dbus_string_init (&funcname))
        die ("no memory\n");
      
      if (!string_get_function (contents, i,
                                &funcname, &checksum, &i))
        die ("could not read function name\n");
#endif /* DBUS_HAVE_GCC33_GCOV */

      if (!string_get_int (contents, i, &val))
        break;
      
      n_blocks_in_func = val;
      
      i += 4;

      if (!string_get_int (contents, i, &n_arcs_in_func))
        break;

      i += 4;

      n_functions += 1;
      n_blocks += n_blocks_in_func;
      n_arcs += n_arcs_in_func;

      func = dbus_new0 (Function, 1);
      if (func == NULL)
        die ("no memory\n");

#ifdef DBUS_HAVE_GCC33_GCOV
      func->name = _dbus_strdup (_dbus_string_get_const_data (&funcname));
      func->checksum = checksum;
      _dbus_string_free (&funcname);
#endif
      
      func->block_graph = dbus_new0 (Block, n_blocks_in_func);
      func->n_blocks = n_blocks_in_func;
      
      j = 0;
      while (j < n_blocks_in_func)
        {
          long n_arcs_in_block;
          int k;
          
          if (!string_get_int (contents, i, &n_arcs_in_block))
            break;

          i += 4;
          
          k = 0;
          while (k < n_arcs_in_block)
            {
              long destination_block;
              long flags;
              
              if (!string_get_int (contents, i, &destination_block))
                break;

              i += 4;
              
              if (!string_get_int (contents, i, &flags))
                break;

              i += 4;

              if ((flags & FLAG_ON_TREE) == 0)
                n_arcs_off_tree += 1;

              function_add_arc (func, j, destination_block,
                                flags);
              
              ++k;
            }

          if (k < n_arcs_in_block)
            break;
          
          ++j;
        }

      if (j < n_blocks_in_func)
        break;

      function_reverse_succ_arcs (func);
      
      if (!_dbus_list_append (functions, func))
        die ("no memory\n");
      
      if (!string_get_int (contents, i, &val))
        break;

      i += 4;

      if (val != -1)
        die ("-1 separator not found in .bbg file\n");
    }

#if 0
  printf ("%d functions %d blocks %d arcs %d off-tree arcs in file\n",
          n_functions, n_blocks, n_arcs, n_arcs_off_tree);
#endif
  
  _dbus_assert (n_functions == _dbus_list_get_length (functions));
}

#ifdef DBUS_HAVE_GCC33_GCOV
static void
add_counts_from_da (const DBusString  *contents,
                    DBusList         **functions)
{
  int i;
  dbus_int64_t v64;
  long val;
  int n_sections;
  DBusList *link;
  Function *current_func;  
  int current_block;
  Arc *current_arc;

  n_sections = 0;

  i = 0;
  while (i < _dbus_string_get_length (contents))
    {
      int claimed_n_functions;
      int n_functions;
      
      if (!string_get_int (contents, i, &val))
        die ("no magic found in .da file\n");

      i += 4;

      if (val != -123)
        die ("wrong file magic in .da file\n");

      if (!string_get_int (contents, i, &val))
        die ("no function count in .da file\n");
      i += 4;
      claimed_n_functions = val;
      
      if (!string_get_int (contents, i, &val))
        die ("no extra data length in .da file\n");

      i += 4;

      i += val;

      link = _dbus_list_get_first_link (functions);
      if (link == NULL)
        goto no_more_functions;
      
      n_functions = 0;
      while (n_functions < claimed_n_functions && link != NULL)
        {
          DBusString funcname;
          int checksum;
          int claimed_n_arcs;
          int n_arcs;

          current_func = link->data;
          current_block = 0;
          current_arc = current_func->block_graph[current_block].succ;
          
          if (!_dbus_string_init (&funcname))
            die ("no memory\n");
          
          if (!string_get_function (contents, i,
                                    &funcname, &checksum, &i))
            die ("could not read function name\n");

          if (!_dbus_string_equal_c_str (&funcname, current_func->name))
            {
              fprintf (stderr, "Expecting .da info for %s but got %s\n",
                       current_func->name,
                       _dbus_string_get_const_data (&funcname));
              exit (1);
            }
          
          if (checksum != current_func->checksum)
            die (".da file checksum doesn't match checksum from .bbg file\n");
          
          if (!string_get_int (contents, i, &val))
            die ("no arc count for function\n");
          
          i += 4;
          claimed_n_arcs = val;

          /* For each arc in the profile, find the corresponding
           * arc in the function and increment its count
           */
          n_arcs = 0;
          while (n_arcs < claimed_n_arcs)
            {
              if (!string_get_int64 (contents, i, &v64))
                die ("did not get execution count for arc\n");
              
              i += 8;

              /* Find the next arc in the function that isn't on tree */
              while (current_arc == NULL ||
                     current_arc->on_tree)
                {
                  if (current_arc == NULL)
                    {
                      ++current_block;
              
                      if (current_block >= current_func->n_blocks)
                        die ("too many blocks in function\n");
              
                      current_arc = current_func->block_graph[current_block].succ;
                    }
                  else
                    {
                      current_arc = current_arc->succ_next;
                    }
                }
              
              _dbus_assert (current_arc != NULL);
              _dbus_assert (!current_arc->on_tree);
              
              current_arc->arc_count = v64;
              current_arc->count_valid = TRUE;
              current_func->block_graph[current_block].succ_count -= 1;
              current_func->block_graph[current_arc->target].pred_count -= 1;
              
              ++n_arcs;
              
              current_arc = current_arc->succ_next;
            }

          _dbus_string_free (&funcname);

          link = _dbus_list_get_next_link (functions, link);
          ++n_functions;

          if (link == NULL && n_functions < claimed_n_functions)
            {
              fprintf (stderr, "Ran out of functions loading .da file\n");
              goto no_more_functions;
            }
        }

    no_more_functions:
      
      ++n_sections;
    }
}
#else /* DBUS_HAVE_GCC33_GCOV */
static void
add_counts_from_da (const DBusString  *contents,
                    DBusList         **functions)
{
  int i;
  dbus_int64_t val;
  int n_arcs;
  int claimed_n_arcs;
  DBusList *link;
  Function *current_func;  
  int current_block;
  Arc *current_arc;

#if 0
  printf ("Loading execution count for each arc from .da file\n");
#endif
  
  i = 0;
  if (!string_get_int64 (contents, i, &val))
    return;

  i += 8;
  
  claimed_n_arcs = val;

  link = _dbus_list_get_first_link (functions);
  if (link == NULL)
    goto done;

  current_func = link->data;
  current_block = 0;
  current_arc = current_func->block_graph[current_block].succ;
  
  n_arcs = 0;
  while (string_get_int64 (contents, i, &val))
    {
      i += 8;

      while (current_arc == NULL ||
             current_arc->on_tree)
        {
          if (current_arc == NULL)
            {
              ++current_block;
              
              if (current_block == current_func->n_blocks)
                {
                  link = _dbus_list_get_next_link (functions, link);
                  if (link == NULL)
                    {
                      fprintf (stderr, "Ran out of functions loading .da file\n");
                      goto done;
                    }
                  current_func = link->data;
                  current_block = 0;
                }
              
              current_arc = current_func->block_graph[current_block].succ;
            }
          else
            {
              current_arc = current_arc->succ_next;
            }
        }

      _dbus_assert (current_arc != NULL);
      _dbus_assert (!current_arc->on_tree);

      current_arc->arc_count = val;
      current_arc->count_valid = TRUE;
      current_func->block_graph[current_block].succ_count -= 1;
      current_func->block_graph[current_arc->target].pred_count -= 1;
      
      ++n_arcs;

      current_arc = current_arc->succ_next;
    }

 done:
  
  if (n_arcs != claimed_n_arcs)
    {
      fprintf (stderr, "File claimed to have %d arcs but only had %d\n",
               claimed_n_arcs, n_arcs);
      exit (1);
    }

#if 0
  printf ("%d arcs in file\n", n_arcs);
#endif
}
#endif

static void
function_solve_graph (Function *func)
{
  int passes, changes;
  dbus_int64_t total;
  int i;
  Arc *arc;
  Block *block_graph;
  int n_blocks;

#if 0
  printf ("Solving function graph\n");
#endif
  
  n_blocks = func->n_blocks;
  block_graph = func->block_graph;

  /* For every block in the file,
     - if every exit/entrance arc has a known count, then set the block count
     - if the block count is known, and every exit/entrance arc but one has
     a known execution count, then set the count of the remaining arc

     As arc counts are set, decrement the succ/pred count, but don't delete
     the arc, that way we can easily tell when all arcs are known, or only
     one arc is unknown.  */

  /* The order that the basic blocks are iterated through is important.
     Since the code that finds spanning trees starts with block 0, low numbered
     arcs are put on the spanning tree in preference to high numbered arcs.
     Hence, most instrumented arcs are at the end.  Graph solving works much
     faster if we propagate numbers from the end to the start.

     This takes an average of slightly more than 3 passes.  */

  changes = 1;
  passes = 0;
  while (changes)
    {
      passes++;
      changes = 0;

      for (i = n_blocks - 1; i >= 0; i--)
	{
	  if (! block_graph[i].count_valid)
	    {
	      if (block_graph[i].succ_count == 0)
		{
		  total = 0;
		  for (arc = block_graph[i].succ; arc;
		       arc = arc->succ_next)
		    total += arc->arc_count;
		  block_graph[i].exec_count = total;
		  block_graph[i].count_valid = 1;
		  changes = 1;
		}
	      else if (block_graph[i].pred_count == 0)
		{
		  total = 0;
		  for (arc = block_graph[i].pred; arc;
		       arc = arc->pred_next)
		    total += arc->arc_count;
		  block_graph[i].exec_count = total;
		  block_graph[i].count_valid = 1;
		  changes = 1;
		}
	    }
	  if (block_graph[i].count_valid)
	    {
	      if (block_graph[i].succ_count == 1)
		{
		  total = 0;
		  /* One of the counts will be invalid, but it is zero,
		     so adding it in also doesn't hurt.  */
		  for (arc = block_graph[i].succ; arc;
		       arc = arc->succ_next)
		    total += arc->arc_count;
		  /* Calculate count for remaining arc by conservation.  */
		  total = block_graph[i].exec_count - total;
		  /* Search for the invalid arc, and set its count.  */
		  for (arc = block_graph[i].succ; arc;
		       arc = arc->succ_next)
		    if (! arc->count_valid)
		      break;
		  if (! arc)
		    die ("arc == NULL\n");
		  arc->count_valid = 1;
		  arc->arc_count = total;
		  block_graph[i].succ_count -= 1;

		  block_graph[arc->target].pred_count -= 1;
		  changes = 1;
		}
	      if (block_graph[i].pred_count == 1)
		{
		  total = 0;
		  /* One of the counts will be invalid, but it is zero,
		     so adding it in also doesn't hurt.  */
		  for (arc = block_graph[i].pred; arc;
		       arc = arc->pred_next)
		    total += arc->arc_count;
		  /* Calculate count for remaining arc by conservation.  */
		  total = block_graph[i].exec_count - total;
		  /* Search for the invalid arc, and set its count.  */
		  for (arc = block_graph[i].pred; arc;
		       arc = arc->pred_next)
		    if (! arc->count_valid)
		      break;
		  if (! arc)
                    die ("arc == NULL\n");
		  arc->count_valid = 1;
		  arc->arc_count = total;
		  block_graph[i].pred_count -= 1;

		  block_graph[arc->source].succ_count -= 1;
		  changes = 1;
		}
	    }
	}
    }

  /* If the graph has been correctly solved, every block will have a
   * succ and pred count of zero.
   */
  {
    dbus_bool_t header = FALSE;
    for (i = 0; i < n_blocks; i++)
      {
        if (block_graph[i].succ_count || block_graph[i].pred_count)
          {
            if (!header)
              {
                fprintf (stderr, "WARNING: Block graph solved incorrectly for function %s\n",
                         func->name);
                fprintf (stderr, " this error reflects a bug in decode-gcov.c or perhaps bogus data\n");
                header = TRUE;
              }
            fprintf (stderr, " block %d has succ_count = %d pred_count = %d\n",
                     i, (int) block_graph[i].succ_count, (int) block_graph[i].pred_count);
          }
      }
  }
}

static void
solve_graphs (DBusList **functions)
{
  DBusList *link;

  link = _dbus_list_get_first_link (functions);
  while (link != NULL)
    {
      Function *func = link->data;

      function_solve_graph (func);
      
      link = _dbus_list_get_next_link (functions, link);
    }
}

static void
load_functions_for_c_file (const DBusString *filename,
                           DBusList        **functions)
{
  DBusString bbg_filename;
  DBusString da_filename;
  DBusString gcno_filename;
  DBusString gcda_filename;
  DBusString contents;
  DBusString *name;
  DBusError error;

  /* With latest gcc it's .gcno instead of .bbg and
   * gcda instead of .da
   */
  
  dbus_error_init (&error);
  
  if (!_dbus_string_init (&bbg_filename) ||
      !_dbus_string_init (&da_filename) ||
      !_dbus_string_init (&gcno_filename) ||
      !_dbus_string_init (&gcda_filename) ||
      !_dbus_string_copy (filename, 0, &bbg_filename, 0) ||
      !_dbus_string_copy (filename, 0, &da_filename, 0) ||
      !_dbus_string_copy (filename, 0, &gcno_filename, 0) ||
      !_dbus_string_copy (filename, 0, &gcda_filename, 0) ||
      !_dbus_string_init (&contents))
    die ("no memory\n");

  _dbus_string_shorten (&bbg_filename, 2);
  _dbus_string_shorten (&da_filename, 2);

  if (!_dbus_string_append (&bbg_filename, ".bbg") ||
      !_dbus_string_append (&da_filename, ".da") ||
      !_dbus_string_append (&bbg_filename, ".gcno") ||
      !_dbus_string_append (&bbg_filename, ".gcda"))
    die ("no memory\n");

  if (_dbus_file_exists (_dbus_string_get_const_data (&gcno_filename)))
    name = &gcno_filename;
  else
    name = &bbg_filename;
  
  if (!_dbus_file_get_contents (&contents, name,
                                &error))
    {
      fprintf (stderr, "Could not open file: %s\n",
               error.message);
      exit (1);
    }

  get_functions_from_bbg (&contents, functions);

  _dbus_string_set_length (&contents, 0);

  if (_dbus_file_exists (_dbus_string_get_const_data (&gcda_filename)))
    name = &gcda_filename;
  else
    name = &da_filename;
  
  if (!_dbus_file_get_contents (&contents, name,
                                &error))
    {
      /* Try .libs/file.da */
      int slash;

      if (_dbus_string_find_byte_backward (name,
                                           _dbus_string_get_length (name),
                                           '/',
                                           &slash))
        {
          DBusString libs;
          _dbus_string_init_const (&libs, "/.libs");

          if (!_dbus_string_copy (&libs, 0, name, slash))
            die ("no memory");

          dbus_error_free (&error);
          if (!_dbus_file_get_contents (&contents, name,
                                        &error))
            {
              fprintf (stderr, "Could not open file: %s\n",
                       error.message);
              exit (1);
            }
        }
      else
        {
          fprintf (stderr, "Could not open file: %s\n",
                   error.message);
          exit (1);
        }
    }
  
  add_counts_from_da (&contents, functions);
  
  solve_graphs (functions);

  _dbus_string_free (&contents);
  _dbus_string_free (&da_filename);
  _dbus_string_free (&bbg_filename);
}

static void
get_lines_from_bb_file (const DBusString *contents,
                        File             *fl)
{
  int i;
  long val;
  int n_functions;
  dbus_bool_t in_our_file;
  DBusList *link;
  Function *func;
  int block;

#if 0
  printf ("Getting line numbers for blocks from .bb file\n");
#endif
  
  /* There's this "filename" field in the .bb file which
   * mysteriously comes *after* the first function in the
   * file in the .bb file; and every .bb file seems to
   * have only one filename. I don't understand
   * what's going on here, so just set in_our_file = TRUE
   * at the start categorically.
   */
  
  block = 0;
  func = NULL;
  in_our_file = TRUE;
  link = _dbus_list_get_first_link (&fl->functions);
  n_functions = 0;
  i = 0;
  while (string_get_int (contents, i, &val))
    {
      i += 4;
      
      switch (val)
        {
        case BB_FILENAME:
          {
            DBusString f;

            if (!_dbus_string_init (&f))
              die ("no memory\n");

            if (string_get_string (contents, i,
                                   BB_FILENAME,
                                   &f, &i))
              {
                /* fl->name is a full path and the filename in .bb is
                 * not.
                 */
                DBusString tmp_str;

                _dbus_string_init_const (&tmp_str, fl->name);
                
                if (_dbus_string_ends_with_c_str (&tmp_str,
                                                  _dbus_string_get_const_data (&f)))
                  in_our_file = TRUE;
                else
                  in_our_file = FALSE;
                
#if 0
                fprintf (stderr,
                         "File %s in .bb, looking for %s, in_our_file = %d\n",
                         _dbus_string_get_const_data (&f),
                         fl->name,
                         in_our_file);
#endif
              }
            _dbus_string_free (&f);
          }
          break;
        case BB_FUNCTION:
          {
            DBusString f;
            if (!_dbus_string_init (&f))
              die ("no memory\n");

            if (string_get_string (contents, i,
                                   BB_FUNCTION,
                                   &f, &i))
              {
#if 0
                fprintf (stderr, "Function %s\n", _dbus_string_get_const_data (&f));
#endif

                block = 0;
                
                if (in_our_file)
                  {
                    if (link == NULL)
                      {
                        fprintf (stderr, "No function object for function %s\n",
                                 _dbus_string_get_const_data (&f));
                      }
                    else
                      {
                        func = link->data;
                        link = _dbus_list_get_next_link (&fl->functions, link);

                        if (func->name == NULL)
                          {
                            if (!_dbus_string_copy_data (&f, &func->name))
                              die ("no memory\n");
                          }
                        else
                          {
                            if (!_dbus_string_equal_c_str (&f, func->name))
                              {
                                fprintf (stderr, "got function name \"%s\" (%d) from .bbg file, but \"%s\" (%d) from .bb file\n",
                                         func->name, strlen (func->name),
                                         _dbus_string_get_const_data (&f),
                                         _dbus_string_get_length (&f));

                              }
                          }
                      }
                  }
              }
            _dbus_string_free (&f);

            n_functions += 1;
          }
          break;
        case BB_ENDOFLIST:
          block += 1;
          break;
        default:
#if 0
          fprintf (stderr, "Line %ld\n", val);
#endif

          if (val >= fl->n_lines)
            {
              fprintf (stderr, "Line %ld but file only has %d lines\n",
                       val, fl->n_lines);
            }
          else if (func != NULL)
            {
              val -= 1; /* To convert the 1-based line number to 0-based */
              _dbus_assert (val >= 0);
              
              if (block < func->n_blocks)
                {
                  if (!_dbus_list_append (&func->block_graph[block].lines,
                                          &fl->lines[val]))
                    die ("no memory\n");
                  
                  
                  if (!_dbus_list_append (&fl->lines[val].blocks,
                                          &func->block_graph[block]))
                    die ("no memory\n");
                }
              else
                {
                  fprintf (stderr, "Line number for block %d but function only has %d blocks\n",
                           block, func->n_blocks);
                }
            }
          else
            {
              fprintf (stderr, "Line %ld given outside of any function\n",
                       val);
            }
          
          break;
        }
    }

#if 0
  printf ("%d functions in file\n", n_functions);
#endif
}


static void
load_block_line_associations (const DBusString *filename,
                              File             *f)
{
  DBusString bb_filename;
  DBusString contents;
  DBusError error;

  dbus_error_init (&error);
  
  if (!_dbus_string_init (&bb_filename) ||
      !_dbus_string_copy (filename, 0, &bb_filename, 0) ||
      !_dbus_string_init (&contents))
    die ("no memory\n");

  _dbus_string_shorten (&bb_filename, 2);

  if (!_dbus_string_append (&bb_filename, ".bb"))
    die ("no memory\n");
      
  if (!_dbus_file_get_contents (&contents, &bb_filename,
                                &error))
    {
      fprintf (stderr, "Could not open file: %s\n",
               error.message);
      exit (1);
    }
  
  get_lines_from_bb_file (&contents, f);

  _dbus_string_free (&contents);
  _dbus_string_free (&bb_filename);
}

static int
count_lines_in_string (const DBusString *str)
{
  int n_lines;
  const char *p;
  const char *prev;
  const char *end;
  const char *last_line_end;

#if 0
  printf ("Counting lines in source file\n");
#endif
  
  n_lines = 0;  
  prev = NULL;
  p = _dbus_string_get_const_data (str);
  end = p + _dbus_string_get_length (str);
  last_line_end = p;
  while (p != end)
    {
      /* too lazy to handle \r\n as one linebreak */
      if (*p == '\n' || *p == '\r')
        {
          ++n_lines;
          last_line_end = p + 1;
        }

      prev = p;
      ++p;
    }

  if (last_line_end != p)
    ++n_lines;
  
  return n_lines;
}

static void
fill_line_content (const DBusString *str,
                   Line             *lines)
{
  int n_lines;
  const char *p;
  const char *prev;
  const char *end;
  const char *last_line_end;

#if 0
  printf ("Saving contents of each line in source file\n");
#endif
  
  n_lines = 0;
  prev = NULL;
  p = _dbus_string_get_const_data (str);
  end = p + _dbus_string_get_length (str);
  last_line_end = p;
  while (p != end)
    {
      if (*p == '\n' || *p == '\r')
        {
          lines[n_lines].text = dbus_malloc0 (p - last_line_end + 1);
          if (lines[n_lines].text == NULL)
            die ("no memory\n");

          memcpy (lines[n_lines].text, last_line_end, p - last_line_end);
          lines[n_lines].number = n_lines + 1;
          
          ++n_lines;

          last_line_end = p + 1;
        }

      prev = p;
      ++p;
    }

  if (p != last_line_end)
    {
      memcpy (lines[n_lines].text, last_line_end, p - last_line_end);
      ++n_lines;
    }
}

static void
mark_inside_dbus_build_tests (File  *f)
{
  int i;
  DBusList *link;
  int inside_depth;

  inside_depth = 0;
  i = 0;
  while (i < f->n_lines)
    {
      Line *l = &f->lines[i];
      dbus_bool_t is_verbose;

      is_verbose = strstr (l->text, "_dbus_verbose") != NULL;

      if (inside_depth == 0)
        {
          const char *a, *b;
          
          a = strstr (l->text, "#if");
          b = strstr (l->text, "DBUS_BUILD_TESTS");
          if (a && b && (a < b))
            inside_depth += 1;
        }
      else
        {
          if (strstr (l->text, "#if") != NULL)
            inside_depth += 1;
          else if (strstr (l->text, "#endif") != NULL)
            inside_depth -= 1;
        }

      if (inside_depth > 0 || is_verbose)
        {
          /* Mark the line and its blocks */
          DBusList *blink;

          l->inside_dbus_build_tests = TRUE;
          
          blink = _dbus_list_get_first_link (&l->blocks);
          while (blink != NULL)
            {
              Block *b = blink->data;

              b->inside_dbus_build_tests = TRUE;
              
              blink = _dbus_list_get_next_link (&l->blocks, blink);
            }
        }
      
      ++i;
    }

  /* Now mark functions where for all blocks that are associated
   * with a source line, the block is inside_dbus_build_tests.
   */
  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      /* The issue is that some blocks aren't associated with a source line.
       * Assume they are inside/outside tests according to the source
       * line of the preceding block. For the first block, make it
       * match the first following block with a line associated.
       */
      if (func->block_graph[0].lines == NULL)
        {
          /* find first following line */
          i = 1;
          while (i < func->n_blocks)
            {
              if (func->block_graph[i].lines != NULL)
                {
                  func->block_graph[0].inside_dbus_build_tests =
                    func->block_graph[i].inside_dbus_build_tests;
                  break;
                }
              
              ++i;
            }
        }

      /* Now mark all blocks but the first */
      i = 1;
      while (i < func->n_blocks)
        {
          if (func->block_graph[i].lines == NULL)
            {
              func->block_graph[i].inside_dbus_build_tests =
                func->block_graph[i-1].inside_dbus_build_tests;
            }
          
          ++i;
        }
      
      i = 0;
      while (i < func->n_blocks)
        {
          /* Break as soon as any block is not a test block */
          if (func->block_graph[i].lines != NULL &&
              !func->block_graph[i].inside_dbus_build_tests)
            break;
          
          ++i;
        }

      if (i == func->n_blocks)
        func->inside_dbus_build_tests = TRUE;
      
      link = _dbus_list_get_next_link (&f->functions, link);
    } 
}

static void
mark_coverage (File  *f)
{
  int i;
  DBusList *link;
  
  i = 0;
  while (i < f->n_lines)
    {
      Line *l = &f->lines[i];
      DBusList *blink;
      int n_blocks;
      int n_blocks_executed;

      n_blocks = 0;
      n_blocks_executed = 0;
      blink = _dbus_list_get_first_link (&l->blocks);
      while (blink != NULL)
        {
          Block *b = blink->data;
          
          if (b->exec_count > 0)
            n_blocks_executed += 1;

          n_blocks += 1;
          
          blink = _dbus_list_get_next_link (&l->blocks, blink);
        }

      if (n_blocks_executed > 0 &&
          n_blocks_executed < n_blocks)
        l->partial = TRUE;

      ++i;
    }

  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;
      int i;
      int n_test_blocks;
      int n_test_blocks_executed;
      int n_nontest_blocks;
      int n_nontest_blocks_executed;
      
      n_test_blocks = 0;
      n_test_blocks_executed = 0;
      n_nontest_blocks = 0;
      n_nontest_blocks_executed = 0;      

      i = 0;
      while (i < func->n_blocks)
        {
          if (!func->block_graph[i].inside_dbus_build_tests)
            {
              n_nontest_blocks += 1;

              if (func->block_graph[i].exec_count > 0)
                n_nontest_blocks_executed += 1;
            }
          else
            {
              n_test_blocks += 1;

              if (func->block_graph[i].exec_count > 0)
                n_test_blocks_executed += 1;
            }

          ++i;
        }
      
      if (n_nontest_blocks_executed > 0 &&
          n_nontest_blocks_executed < n_nontest_blocks)
        func->partial = TRUE;

      if (n_nontest_blocks_executed == 0 &&
          n_nontest_blocks > 0)
        func->unused = TRUE;
      
      func->n_test_blocks = n_test_blocks;
      func->n_test_blocks_executed = n_test_blocks_executed;
      func->n_nontest_blocks = n_nontest_blocks;
      func->n_nontest_blocks_executed = n_nontest_blocks_executed;
      
      link = _dbus_list_get_next_link (&f->functions, link);
    }
}

static File*
load_c_file (const DBusString *filename)
{
  DBusString contents;
  DBusError error;
  File *f;
  
  f = dbus_new0 (File, 1);
  if (f == NULL)
    die ("no memory\n");

  if (!_dbus_string_copy_data (filename, &f->name))
    die ("no memory\n");
  
  if (!_dbus_string_init (&contents))
    die ("no memory\n");
      
  dbus_error_init (&error);

  if (!_dbus_file_get_contents (&contents, filename,
                                &error))
    {
      fprintf (stderr, "Could not open file: %s\n",
               error.message);
      dbus_error_free (&error);
      exit (1);
    }
      
  load_functions_for_c_file (filename, &f->functions);

  f->n_lines = count_lines_in_string (&contents);
  f->lines = dbus_new0 (Line, f->n_lines);
  if (f->lines == NULL)
    die ("no memory\n");

  fill_line_content (&contents, f->lines);
  
  _dbus_string_free (&contents);

  load_block_line_associations (filename, f);

  mark_inside_dbus_build_tests (f);
  mark_coverage (f);
  
  return f;
}

typedef struct Stats Stats;

struct Stats
{
  int n_blocks;
  int n_blocks_executed;
  int n_blocks_inside_dbus_build_tests;
  
  int n_lines; /* lines that have blocks on them */
  int n_lines_executed;
  int n_lines_partial;
  int n_lines_inside_dbus_build_tests;
  
  int n_functions;
  int n_functions_executed;
  int n_functions_partial;
  int n_functions_inside_dbus_build_tests;
};

static dbus_bool_t
line_was_executed (Line *l)
{
  DBusList *link;

  link = _dbus_list_get_first_link (&l->blocks);
  while (link != NULL)
    {
      Block *b = link->data;

      if (b->exec_count > 0)
        return TRUE;
      
      link = _dbus_list_get_next_link (&l->blocks, link);
    }

  return FALSE;
}


static int
line_exec_count (Line *l)
{
  DBusList *link;
  dbus_int64_t total;

  total = 0;
  link = _dbus_list_get_first_link (&l->blocks);
  while (link != NULL)
    {
      Block *b = link->data;

      total += b->exec_count;
      
      link = _dbus_list_get_next_link (&l->blocks, link);
    }

  return total;
}

static void
merge_stats_for_file (Stats *stats,
                      File  *f)
{
  int i;
  DBusList *link;
  
  for (i = 0; i < f->n_lines; ++i)
    {
      Line *l = &f->lines[i];
      
      if (l->inside_dbus_build_tests)
        {
          stats->n_lines_inside_dbus_build_tests += 1;
          continue;
        }
      
      if (line_was_executed (l))
        stats->n_lines_executed += 1;

      if (l->blocks != NULL)
        stats->n_lines += 1;

      if (l->partial)
        stats->n_lines_partial += 1;
    }

  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      if (func->inside_dbus_build_tests)
        stats->n_functions_inside_dbus_build_tests += 1;
      else
        {
          stats->n_functions += 1;

          if (!func->unused)
            stats->n_functions_executed += 1;
          
          if (func->partial)
            stats->n_functions_partial += 1;
        }

      stats->n_blocks_inside_dbus_build_tests +=
        func->n_test_blocks;
      
      stats->n_blocks_executed +=
        func->n_nontest_blocks_executed;
      
      stats->n_blocks +=
        func->n_nontest_blocks;

      link = _dbus_list_get_next_link (&f->functions, link);
    }
}

/* The output of this matches gcov exactly ("diff" shows no difference) */
static void
print_annotated_source_gcov_format (File *f)
{
  int i;
  
  i = 0;
  while (i < f->n_lines)
    {
      Line *l = &f->lines[i];

      if (l->blocks != NULL)
        {
          int exec_count;
          
          exec_count = line_exec_count (l);
          
          if (exec_count > 0)
            printf ("%12d    %s\n",
                    exec_count, l->text);
          else
            printf ("      ######    %s\n", l->text);
        }
      else
        {
          printf ("\t\t%s\n", l->text);
        }
          
      ++i;
    }
}

static void
print_annotated_source (File *f)
{
  int i;
  
  i = 0;
  while (i < f->n_lines)
    {
      Line *l = &f->lines[i];

      if (l->inside_dbus_build_tests)
        printf ("*");
      else
        printf (" ");
      
      if (l->blocks != NULL)
        {
          int exec_count;
          
          exec_count = line_exec_count (l);
          
          if (exec_count > 0)
            printf ("%12d    %s\n",
                    exec_count, l->text);
          else
            printf ("      ######    %s\n", l->text);
        }
      else
        {
          printf ("\t\t%s\n", l->text);
        }
          
      ++i;
    }
}

static void
print_block_superdetails (File *f)
{
  DBusList *link;
  int i;
  
  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      printf ("=== %s():\n", func->name);

      i = 0;
      while (i < func->n_blocks)
        {
          Block *b = &func->block_graph[i];
          DBusList *l;
          
          printf ("  %5d executed %d times%s\n", i,
                  (int) b->exec_count,
                  b->inside_dbus_build_tests ?
                  " [inside DBUS_BUILD_TESTS]" : "");
                  
          l = _dbus_list_get_first_link (&b->lines);
          while (l != NULL)
            {
              Line *line = l->data;

              printf ("4%d\t%s\n", line->number, line->text);

              l = _dbus_list_get_next_link (&b->lines, l);
            }
          
          ++i;
        }
      
      link = _dbus_list_get_next_link (&f->functions, link);
    }
}

static void
print_one_file (const DBusString *filename)
{
  if (_dbus_string_ends_with_c_str (filename, ".bb"))
    {
      DBusString contents;
      DBusError error;
      
      if (!_dbus_string_init (&contents))
        die ("no memory\n");
      
      dbus_error_init (&error);

      if (!_dbus_file_get_contents (&contents, filename,
                                    &error))
        {
          fprintf (stderr, "Could not open file: %s\n",
                   error.message);
          dbus_error_free (&error);
          exit (1);
        }
      
      dump_bb_file (&contents);

      _dbus_string_free (&contents);
    }
  else if (_dbus_string_ends_with_c_str (filename, ".bbg"))
    {
      DBusString contents;
      DBusError error;
      
      if (!_dbus_string_init (&contents))
        die ("no memory\n");
      
      dbus_error_init (&error);

      if (!_dbus_file_get_contents (&contents, filename,
                                    &error))
        {
          fprintf (stderr, "Could not open file: %s\n",
                   error.message);
          dbus_error_free (&error);
          exit (1);
        }
      
      dump_bbg_file (&contents);

      _dbus_string_free (&contents);
    }
  else if (_dbus_string_ends_with_c_str (filename, ".da"))
    {
      DBusString contents;
      DBusError error;
      
      if (!_dbus_string_init (&contents))
        die ("no memory\n");
      
      dbus_error_init (&error);

      if (!_dbus_file_get_contents (&contents, filename,
                                    &error))
        {
          fprintf (stderr, "Could not open file: %s\n",
                   error.message);
          dbus_error_free (&error);
          exit (1);
        }
      
      dump_da_file (&contents);

      _dbus_string_free (&contents);
    }
  else if (_dbus_string_ends_with_c_str (filename, ".c"))
    {
      File *f;
      
      f = load_c_file (filename);

      print_annotated_source (f);
    }
  else
    {
      fprintf (stderr, "Unknown file type %s\n",
               _dbus_string_get_const_data (filename));
      exit (1);
    }
}

static void
print_untested_functions (File *f)
{
  DBusList *link;
  dbus_bool_t found;

  found = FALSE;
  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      if (func->unused &&
          !func->inside_dbus_build_tests)
        found = TRUE;
      
      link = _dbus_list_get_next_link (&f->functions, link);
    }

  if (!found)
    return;
  
  printf ("Untested functions in %s\n", f->name);
  printf ("=======\n");
  
  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      if (func->unused &&
          !func->inside_dbus_build_tests)
        printf ("  %s\n", func->name);
      
      link = _dbus_list_get_next_link (&f->functions, link);
    }

  printf ("\n");
}

static void
print_poorly_tested_functions (File  *f,
                               Stats *stats)
{
  DBusList *link;
  dbus_bool_t found;

#define TEST_FRACTION(function) ((function)->n_nontest_blocks_executed / (double) (function)->n_nontest_blocks)

#define AVERAGE_COVERAGE ((stats)->n_blocks_executed / (double) (stats)->n_blocks)
  
#define POORLY_TESTED(function) (!(function)->unused &&                 \
                                 (function)->n_nontest_blocks > 0 &&    \
                                 TEST_FRACTION (function) < AVERAGE_COVERAGE)
  
  found = FALSE;
  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      if (POORLY_TESTED (func))
        found = TRUE;
      
      link = _dbus_list_get_next_link (&f->functions, link);
    }

  if (!found)
    return;

  printf ("Below average functions in %s\n", f->name);
  printf ("=======\n");
  
  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      if (POORLY_TESTED (func))
        printf ("  %s (%d%%)\n", func->name,
                (int) (TEST_FRACTION (func) * 100));
      
      link = _dbus_list_get_next_link (&f->functions, link);
    }

  printf ("\n");
}

static int
func_cmp (const void *a,
          const void *b)
{
  Function *af = *(Function**) a;
  Function *bf = *(Function**) b;
  int a_untested = af->n_nontest_blocks - af->n_nontest_blocks_executed;
  int b_untested = bf->n_nontest_blocks - bf->n_nontest_blocks_executed;
  
  /* Sort by number of untested blocks */
  return b_untested - a_untested;
}

static void
print_n_untested_blocks_by_function (File  *f,
                                     Stats *stats)
{
  DBusList *link;
  Function **funcs;
  int n_found;
  int i;
  
  n_found = 0;
  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      if (func->n_nontest_blocks_executed <
          func->n_nontest_blocks)
        n_found += 1;
      
      link = _dbus_list_get_next_link (&f->functions, link);
    }

  if (n_found == 0)
    return;

  /* make an array so we can use qsort */
  
  funcs = dbus_new (Function*, n_found);
  if (funcs == NULL)
    return;
  
  i = 0;
  link = _dbus_list_get_first_link (&f->functions);
  while (link != NULL)
    {
      Function *func = link->data;

      if (func->n_nontest_blocks_executed <
          func->n_nontest_blocks)
        {
          funcs[i] = func;
          ++i;
        }

      link = _dbus_list_get_next_link (&f->functions, link);
    }

  _dbus_assert (i == n_found);
  
  qsort (funcs, n_found, sizeof (Function*),
         func_cmp);
  
  printf ("Incomplete functions in %s\n", f->name);
  printf ("=======\n");

  i = 0;
  while (i < n_found)
    {
      Function *func = funcs[i];

      printf ("  %s (%d/%d untested blocks)\n",
              func->name,
              func->n_nontest_blocks - func->n_nontest_blocks_executed,
              func->n_nontest_blocks);
      
      ++i;
    }

  dbus_free (funcs);

  printf ("\n");
}

static void
print_stats (Stats      *stats,
             const char *of_what)
{
  int completely;
  
  printf ("Summary (%s)\n", of_what);
  printf ("=======\n");
  printf ("  %g%% blocks executed (%d of %d)\n",
          (stats->n_blocks_executed / (double) stats->n_blocks) * 100.0,
          stats->n_blocks_executed,
          stats->n_blocks);

  printf ("     (ignored %d blocks of test-only/debug-only code)\n",
          stats->n_blocks_inside_dbus_build_tests);
      
  printf ("  %g%% functions executed (%d of %d)\n",
          (stats->n_functions_executed / (double) stats->n_functions) * 100.0,
          stats->n_functions_executed,
          stats->n_functions);

  completely = stats->n_functions_executed - stats->n_functions_partial;
  printf ("  %g%% functions completely executed (%d of %d)\n",
          (completely / (double) stats->n_functions) * 100.0,
          completely,
          stats->n_functions);

  printf ("     (ignored %d functions of test-only/debug-only code)\n",
          stats->n_functions_inside_dbus_build_tests);
      
  printf ("  %g%% lines executed (%d of %d)\n",
          (stats->n_lines_executed / (double) stats->n_lines) * 100.0,
          stats->n_lines_executed,
          stats->n_lines);

  completely = stats->n_lines_executed - stats->n_lines_partial;
  printf ("  %g%% lines completely executed (%d of %d)\n",
          (completely / (double) stats->n_lines) * 100.0,
          completely,
          stats->n_lines);

  printf ("     (ignored %d lines of test-only/debug-only code)\n",
          stats->n_lines_inside_dbus_build_tests);

  printf ("\n");
}

typedef enum
{
  MODE_PRINT,
  MODE_REPORT,
  MODE_BLOCKS,
  MODE_GCOV
} Mode;

int
main (int argc, char **argv)
{
  DBusString filename;
  int i;
  Mode m;
  
  if (argc < 2)
    {
      fprintf (stderr, "Must specify files on command line\n");
      return 1;
    }

  m = MODE_PRINT;
  i = 1;

  if (strcmp (argv[i], "--report") == 0)
    {
      m = MODE_REPORT;
      ++i;
    }
  else if (strcmp (argv[i], "--blocks") == 0)
    {
      m = MODE_BLOCKS;
      ++i;
    }
  else if (strcmp (argv[i], "--gcov") == 0)
    {
      m = MODE_GCOV;
      ++i;
    }

  
  if (i == argc)
    {
      fprintf (stderr, "Must specify files on command line\n");
      return 1;
    }

  if (m == MODE_PRINT)
    {
      while (i < argc)
        {
          _dbus_string_init_const (&filename, argv[i]);
          
          print_one_file (&filename);
          
          ++i;
        }
    }
  else if (m == MODE_BLOCKS || m == MODE_GCOV)
    {
      while (i < argc)
        {
          File *f;
          
          _dbus_string_init_const (&filename, argv[i]);
      
          f = load_c_file (&filename);

          if (m == MODE_BLOCKS)
            print_block_superdetails (f);
          else if (m == MODE_GCOV)
            print_annotated_source_gcov_format (f);
          
          ++i;
        }
    }
  else if (m == MODE_REPORT)
    {
      Stats stats = { 0, };
      DBusList *files;
      DBusList *link;
      DBusHashTable *stats_by_dir;
      DBusHashIter iter;
      
      files = NULL;
      while (i < argc)
        {
          _dbus_string_init_const (&filename, argv[i]);

          if (_dbus_string_ends_with_c_str (&filename, ".c"))
            {
              File *f;
              
              f = load_c_file (&filename);
              
              if (!_dbus_list_append (&files, f))
                die ("no memory\n");
            }
          else
            {
              fprintf (stderr, "Unknown file type %s\n",
                       _dbus_string_get_const_data (&filename));
              exit (1);
            }
          
          ++i;
        }

      link = _dbus_list_get_first_link (&files);
      while (link != NULL)
        {
          File *f = link->data;

          merge_stats_for_file (&stats, f);
          
          link = _dbus_list_get_next_link (&files, link);
        }

      print_stats (&stats, "all files");

      stats_by_dir = _dbus_hash_table_new (DBUS_HASH_STRING,
                                           dbus_free, dbus_free);
      
      link = _dbus_list_get_first_link (&files);
      while (link != NULL)
        {
          File *f = link->data;
          DBusString dirname;
          char *dirname_c;
          Stats *dir_stats;
          
          _dbus_string_init_const (&filename, f->name);
            
          if (!_dbus_string_init (&dirname))
            die ("no memory\n");

          if (!_dbus_string_get_dirname (&filename, &dirname) ||
              !_dbus_string_copy_data (&dirname, &dirname_c))
            die ("no memory\n");

          dir_stats = _dbus_hash_table_lookup_string (stats_by_dir,
                                                      dirname_c);

          if (dir_stats == NULL)
            {
              dir_stats = dbus_new0 (Stats, 1);
              if (!_dbus_hash_table_insert_string (stats_by_dir, dirname_c,
                                                   dir_stats))
                die ("no memory\n");
            }
          else
            dbus_free (dirname_c);
          
          merge_stats_for_file (dir_stats, f);
          
          link = _dbus_list_get_next_link (&files, link);
        }

      _dbus_hash_iter_init (stats_by_dir, &iter);
      while (_dbus_hash_iter_next (&iter))
        {
          const char *dirname = _dbus_hash_iter_get_string_key (&iter);
          Stats *dir_stats = _dbus_hash_iter_get_value (&iter);

          print_stats (dir_stats, dirname);
        }

      _dbus_hash_table_unref (stats_by_dir);

      link = _dbus_list_get_first_link (&files);
      while (link != NULL)
        {
          File *f = link->data;

          print_untested_functions (f);
          
          link = _dbus_list_get_next_link (&files, link);
        }

      link = _dbus_list_get_first_link (&files);
      while (link != NULL)
        {
          File *f = link->data;

          print_poorly_tested_functions (f, &stats);
          
          link = _dbus_list_get_next_link (&files, link);
        }

      link = _dbus_list_get_first_link (&files);
      while (link != NULL)
        {
          File *f = link->data;
          
          print_n_untested_blocks_by_function (f, &stats);
          
          link = _dbus_list_get_next_link (&files, link);
        }
    }
  
  return 0;
}
