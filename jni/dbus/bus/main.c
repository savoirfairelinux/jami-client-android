/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* main.c  main() for message bus
 *
 * Copyright (C) 2003 Red Hat, Inc.
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
#include "bus.h"
#include "driver.h"
#include <dbus/dbus-internals.h>
#include <dbus/dbus-watch.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef HAVE_SIGNAL_H
#include <signal.h>
#endif
#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif
#include "selinux.h"

static BusContext *context;

static int reload_pipe[2];
#define RELOAD_READ_END 0
#define RELOAD_WRITE_END 1

static void close_reload_pipe (void);

static void
signal_handler (int sig)
{

  switch (sig)
    {
#ifdef DBUS_BUS_ENABLE_DNOTIFY_ON_LINUX
    case SIGIO:
      /* explicit fall-through */
#endif /* DBUS_BUS_ENABLE_DNOTIFY_ON_LINUX  */
#ifdef SIGHUP
    case SIGHUP:
      {
        DBusString str;
        _dbus_string_init_const (&str, "foo");
        if ((reload_pipe[RELOAD_WRITE_END] > 0) &&
            !_dbus_write_socket (reload_pipe[RELOAD_WRITE_END], &str, 0, 1))
          {
            _dbus_warn ("Unable to write to reload pipe.\n");
            close_reload_pipe ();
          }
      }
      break;
#endif
    }
}

static void
usage (void)
{
  fprintf (stderr, DBUS_DAEMON_NAME " [--version] [--session] [--system] [--config-file=FILE] [--print-address[=DESCRIPTOR]] [--print-pid[=DESCRIPTOR]] [--fork] [--nofork] [--introspect] [--address=ADDRESS] [--systemd-activation]\n");
  exit (1);
}

static void
version (void)
{
  printf ("D-Bus Message Bus Daemon %s\n"
          "Copyright (C) 2002, 2003 Red Hat, Inc., CodeFactory AB, and others\n"
          "This is free software; see the source for copying conditions.\n"
          "There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n",
          DBUS_VERSION_STRING);
  exit (0);
}

static void
introspect (void)
{
  DBusString xml;
  const char *v_STRING;

  if (!_dbus_string_init (&xml))
    goto oom;

  if (!bus_driver_generate_introspect_string (&xml))
    {
      _dbus_string_free (&xml);
      goto oom;
    }

  v_STRING = _dbus_string_get_const_data (&xml);
  printf ("%s\n", v_STRING);

  exit (0);

 oom:
  _dbus_warn ("Can not introspect - Out of memory\n");
  exit (1);
}

static void
check_two_config_files (const DBusString *config_file,
                        const char       *extra_arg)
{
  if (_dbus_string_get_length (config_file) > 0)
    {
      fprintf (stderr, "--%s specified but configuration file %s already requested\n",
               extra_arg, _dbus_string_get_const_data (config_file));
      exit (1);
    }
}

static void
check_two_addresses (const DBusString *address,
                     const char       *extra_arg)
{
  if (_dbus_string_get_length (address) > 0)
    {
      fprintf (stderr, "--%s specified but address %s already requested\n",
               extra_arg, _dbus_string_get_const_data (address));
      exit (1);
    }
}

static void
check_two_addr_descriptors (const DBusString *addr_fd,
                            const char       *extra_arg)
{
  if (_dbus_string_get_length (addr_fd) > 0)
    {
      fprintf (stderr, "--%s specified but printing address to %s already requested\n",
               extra_arg, _dbus_string_get_const_data (addr_fd));
      exit (1);
    }
}

static void
check_two_pid_descriptors (const DBusString *pid_fd,
                           const char       *extra_arg)
{
  if (_dbus_string_get_length (pid_fd) > 0)
    {
      fprintf (stderr, "--%s specified but printing pid to %s already requested\n",
               extra_arg, _dbus_string_get_const_data (pid_fd));
      exit (1);
    }
}

static dbus_bool_t
handle_reload_watch (DBusWatch    *watch,
		     unsigned int  flags,
		     void         *data)
{
  DBusError error;
  DBusString str;

  while (!_dbus_string_init (&str))
    _dbus_wait_for_memory ();

  if ((reload_pipe[RELOAD_READ_END] > 0) &&
      _dbus_read_socket (reload_pipe[RELOAD_READ_END], &str, 1) != 1)
    {
      _dbus_warn ("Couldn't read from reload pipe.\n");
      close_reload_pipe ();
      return TRUE;
    }
  _dbus_string_free (&str);

  /* this can only fail if we don't understand the config file
   * or OOM.  Either way we should just stick with the currently
   * loaded config.
   */
  dbus_error_init (&error);
  if (! bus_context_reload_config (context, &error))
    {
      _DBUS_ASSERT_ERROR_IS_SET (&error);
      _dbus_assert (dbus_error_has_name (&error, DBUS_ERROR_FAILED) ||
		    dbus_error_has_name (&error, DBUS_ERROR_NO_MEMORY));
      _dbus_warn ("Unable to reload configuration: %s\n",
		  error.message);
      dbus_error_free (&error);
    }
  return TRUE;
}

static dbus_bool_t
reload_watch_callback (DBusWatch    *watch,
		       unsigned int  condition,
		       void         *data)
{
  return dbus_watch_handle (watch, condition);
}

static void
setup_reload_pipe (DBusLoop *loop)
{
  DBusError error;
  DBusWatch *watch;

  dbus_error_init (&error);

  if (!_dbus_full_duplex_pipe (&reload_pipe[0], &reload_pipe[1],
			       TRUE, &error))
    {
      _dbus_warn ("Unable to create reload pipe: %s\n",
		  error.message);
      dbus_error_free (&error);
      exit (1);
    }

  watch = _dbus_watch_new (reload_pipe[RELOAD_READ_END],
			   DBUS_WATCH_READABLE, TRUE,
			   handle_reload_watch, NULL, NULL);

  if (watch == NULL)
    {
      _dbus_warn ("Unable to create reload watch: %s\n",
		  error.message);
      dbus_error_free (&error);
      exit (1);
    }

  if (!_dbus_loop_add_watch (loop, watch, reload_watch_callback,
			     NULL, NULL))
    {
      _dbus_warn ("Unable to add reload watch to main loop: %s\n",
		  error.message);
      dbus_error_free (&error);
      exit (1);
    }

}

static void
close_reload_pipe (void)
{
    _dbus_close_socket (reload_pipe[RELOAD_READ_END], NULL);
    reload_pipe[RELOAD_READ_END] = -1;

    _dbus_close_socket (reload_pipe[RELOAD_WRITE_END], NULL);
    reload_pipe[RELOAD_WRITE_END] = -1;
}

int
main (int argc, char **argv)
{
  DBusError error;
  DBusString config_file;
  DBusString address;
  DBusString addr_fd;
  DBusString pid_fd;
  const char *prev_arg;
  DBusPipe print_addr_pipe;
  DBusPipe print_pid_pipe;
  int i;
  dbus_bool_t print_address;
  dbus_bool_t print_pid;
  dbus_bool_t is_session_bus;
  int force_fork;
  dbus_bool_t systemd_activation;

  if (!_dbus_string_init (&config_file))
    return 1;

  if (!_dbus_string_init (&address))
    return 1;

  if (!_dbus_string_init (&addr_fd))
    return 1;

  if (!_dbus_string_init (&pid_fd))
    return 1;

  print_address = FALSE;
  print_pid = FALSE;
  is_session_bus = FALSE;
  force_fork = FORK_FOLLOW_CONFIG_FILE;
  systemd_activation = FALSE;

  prev_arg = NULL;
  i = 1;
  while (i < argc)
    {
      const char *arg = argv[i];

      if (strcmp (arg, "--help") == 0 ||
          strcmp (arg, "-h") == 0 ||
          strcmp (arg, "-?") == 0)
        usage ();
      else if (strcmp (arg, "--version") == 0)
        version ();
      else if (strcmp (arg, "--introspect") == 0)
        introspect ();
      else if (strcmp (arg, "--nofork") == 0)
        force_fork = FORK_NEVER;
      else if (strcmp (arg, "--fork") == 0)
        force_fork = FORK_ALWAYS;
      else if (strcmp (arg, "--systemd-activation") == 0)
        systemd_activation = TRUE;
      else if (strcmp (arg, "--system") == 0)
        {
          check_two_config_files (&config_file, "system");

          if (!_dbus_append_system_config_file (&config_file))
            exit (1);
        }
      else if (strcmp (arg, "--session") == 0)
        {
          check_two_config_files (&config_file, "session");

          if (!_dbus_append_session_config_file (&config_file))
            exit (1);
        }
      else if (strstr (arg, "--config-file=") == arg)
        {
          const char *file;

          check_two_config_files (&config_file, "config-file");

          file = strchr (arg, '=');
          ++file;

          if (!_dbus_string_append (&config_file, file))
            exit (1);
        }
      else if (prev_arg &&
               strcmp (prev_arg, "--config-file") == 0)
        {
          check_two_config_files (&config_file, "config-file");

          if (!_dbus_string_append (&config_file, arg))
            exit (1);
        }
      else if (strcmp (arg, "--config-file") == 0)
        ; /* wait for next arg */
      else if (strstr (arg, "--address=") == arg)
        {
          const char *file;

          check_two_addresses (&address, "address");

          file = strchr (arg, '=');
          ++file;

          if (!_dbus_string_append (&address, file))
            exit (1);
        }
      else if (prev_arg &&
               strcmp (prev_arg, "--address") == 0)
        {
          check_two_addresses (&address, "address");

          if (!_dbus_string_append (&address, arg))
            exit (1);
        }
      else if (strcmp (arg, "--address") == 0)
        ; /* wait for next arg */
      else if (strstr (arg, "--print-address=") == arg)
        {
          const char *desc;

          check_two_addr_descriptors (&addr_fd, "print-address");

          desc = strchr (arg, '=');
          ++desc;

          if (!_dbus_string_append (&addr_fd, desc))
            exit (1);

          print_address = TRUE;
        }
      else if (prev_arg &&
               strcmp (prev_arg, "--print-address") == 0)
        {
          check_two_addr_descriptors (&addr_fd, "print-address");

          if (!_dbus_string_append (&addr_fd, arg))
            exit (1);

          print_address = TRUE;
        }
      else if (strcmp (arg, "--print-address") == 0)
        print_address = TRUE; /* and we'll get the next arg if appropriate */
      else if (strstr (arg, "--print-pid=") == arg)
        {
          const char *desc;

          check_two_pid_descriptors (&pid_fd, "print-pid");

          desc = strchr (arg, '=');
          ++desc;

          if (!_dbus_string_append (&pid_fd, desc))
            exit (1);

          print_pid = TRUE;
        }
      else if (prev_arg &&
               strcmp (prev_arg, "--print-pid") == 0)
        {
          check_two_pid_descriptors (&pid_fd, "print-pid");

          if (!_dbus_string_append (&pid_fd, arg))
            exit (1);

          print_pid = TRUE;
        }
      else if (strcmp (arg, "--print-pid") == 0)
        print_pid = TRUE; /* and we'll get the next arg if appropriate */
      else
        usage ();

      prev_arg = arg;

      ++i;
    }

  if (_dbus_string_get_length (&config_file) == 0)
    {
      fprintf (stderr, "No configuration file specified.\n");
      usage ();
    }

  _dbus_pipe_invalidate (&print_addr_pipe);
  if (print_address)
    {
      _dbus_pipe_init_stdout (&print_addr_pipe);
      if (_dbus_string_get_length (&addr_fd) > 0)
        {
          long val;
          int end;
          if (!_dbus_string_parse_int (&addr_fd, 0, &val, &end) ||
              end != _dbus_string_get_length (&addr_fd) ||
              val < 0 || val > _DBUS_INT_MAX)
            {
              fprintf (stderr, "Invalid file descriptor: \"%s\"\n",
                       _dbus_string_get_const_data (&addr_fd));
              exit (1);
            }

          _dbus_pipe_init (&print_addr_pipe, val);
        }
    }
  _dbus_string_free (&addr_fd);

  _dbus_pipe_invalidate (&print_pid_pipe);
  if (print_pid)
    {
      _dbus_pipe_init_stdout (&print_pid_pipe);
      if (_dbus_string_get_length (&pid_fd) > 0)
        {
          long val;
          int end;
          if (!_dbus_string_parse_int (&pid_fd, 0, &val, &end) ||
              end != _dbus_string_get_length (&pid_fd) ||
              val < 0 || val > _DBUS_INT_MAX)
            {
              fprintf (stderr, "Invalid file descriptor: \"%s\"\n",
                       _dbus_string_get_const_data (&pid_fd));
              exit (1);
            }

          _dbus_pipe_init (&print_pid_pipe, val);
        }
    }
  _dbus_string_free (&pid_fd);

  if (!bus_selinux_pre_init ())
    {
      _dbus_warn ("SELinux pre-initialization failed\n");
      exit (1);
    }

  dbus_error_init (&error);
  context = bus_context_new (&config_file, force_fork,
                             &print_addr_pipe, &print_pid_pipe,
                             _dbus_string_get_length(&address) > 0 ? &address : NULL,
                             systemd_activation,
                             &error);
  _dbus_string_free (&config_file);
  if (context == NULL)
    {
      _dbus_warn ("Failed to start message bus: %s\n",
                  error.message);
      dbus_error_free (&error);
      exit (1);
    }

  is_session_bus = bus_context_get_type(context) != NULL
      && strcmp(bus_context_get_type(context),"session") == 0;

  if (is_session_bus)
    _dbus_daemon_publish_session_bus_address (bus_context_get_address (context));

  /* bus_context_new() closes the print_addr_pipe and
   * print_pid_pipe
   */

  setup_reload_pipe (bus_context_get_loop (context));

#ifdef SIGHUP
  _dbus_set_signal_handler (SIGHUP, signal_handler);
#endif
#ifdef DBUS_BUS_ENABLE_DNOTIFY_ON_LINUX
  _dbus_set_signal_handler (SIGIO, signal_handler);
#endif /* DBUS_BUS_ENABLE_DNOTIFY_ON_LINUX */

  _dbus_verbose ("We are on D-Bus...\n");
  _dbus_loop_run (bus_context_get_loop (context));

  bus_context_shutdown (context);
  bus_context_unref (context);
  bus_selinux_shutdown ();

  if (is_session_bus)
    _dbus_daemon_unpublish_session_bus_address ();

  return 0;
}
