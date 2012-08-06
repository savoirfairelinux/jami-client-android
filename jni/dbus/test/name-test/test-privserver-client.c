#include <config.h>
#include "../test-utils.h"

static void
die (const char *message, ...)
{
  va_list args;
  va_start (args, message);
  vfprintf (stderr, message, args);
  va_end (args);
  exit (1);
}

static DBusHandlerResult 
filter_private_message (DBusConnection     *connection,
                        DBusMessage        *message,
                        void               *user_data)
{
  if (dbus_message_is_signal (message,
                              DBUS_INTERFACE_LOCAL,
                              "Disconnected"))
    {
       DBusLoop *loop = user_data;      
       _dbus_loop_quit (loop);
       return DBUS_HANDLER_RESULT_HANDLED;
    }
  return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
}

static void
open_shutdown_private_connection (dbus_bool_t use_guid)
{
  DBusError error;
  DBusLoop *loop;
  DBusConnection *session;
  DBusMessage *msg;
  DBusMessage *reply;
  DBusConnection *privconn;
  char *addr;
  char *comma;

  dbus_error_init (&error);

  loop = _dbus_loop_new ();

  session = dbus_bus_get (DBUS_BUS_SESSION, &error);
  if (!session)
    die ("couldn't access session bus\n");
  dbus_connection_set_exit_on_disconnect (session, FALSE);
  msg = dbus_message_new_method_call ("org.freedesktop.DBus.TestSuite.PrivServer",
                                      "/",
                                      "org.freedesktop.DBus.TestSuite.PrivServer",
                                      "GetPrivateAddress");
  if (!(reply = dbus_connection_send_with_reply_and_block (session, msg, -1, &error)))
    die ("couldn't send message: %s\n", error.message);
  dbus_message_unref (msg);
  if (!dbus_message_get_args (reply, &error, DBUS_TYPE_STRING, &addr, DBUS_TYPE_INVALID))
    die ("couldn't parse message replym\n");
  printf ("got private temp address %s\n", addr);
  addr = strdup (addr);
  if (!use_guid)
    {
      char *comma = strrchr (addr, ',');
      if (comma)
        *comma = '\0';
    }
  privconn = dbus_connection_open (addr, &error);
  free (addr);
  if (!privconn)
    die ("couldn't connect to server direct connection: %s\n", error.message);
  dbus_message_unref (reply);

  dbus_connection_set_exit_on_disconnect (privconn, FALSE);
  dbus_connection_add_filter (privconn, filter_private_message, loop, NULL);
  test_connection_setup (loop, privconn);

  msg = dbus_message_new_method_call ("org.freedesktop.DBus.TestSuite.PrivServer",
                                      "/",
                                      "org.freedesktop.DBus.TestSuite.PrivServer",
                                      "Quit");
  if (!dbus_connection_send (session, msg, NULL))
    die ("couldn't send Quit message\n");
  dbus_message_unref (msg);

  _dbus_loop_run (loop);  

  test_connection_shutdown (loop, session);
  dbus_connection_unref (session);

  test_connection_shutdown (loop, privconn);
  dbus_connection_remove_filter (privconn, filter_private_message, loop);
  dbus_connection_unref (privconn);

  _dbus_loop_unref (loop);
}

int
main (int argc, char *argv[])
{
  open_shutdown_private_connection (TRUE);

  dbus_shutdown ();

  open_shutdown_private_connection (TRUE);

  dbus_shutdown ();

  open_shutdown_private_connection (FALSE);

  dbus_shutdown ();

  open_shutdown_private_connection (FALSE);

  dbus_shutdown ();

  return 0;
}
