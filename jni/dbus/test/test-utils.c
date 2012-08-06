#include <config.h>
#include "test-utils.h"

typedef struct
{
  DBusLoop *loop;
  DBusConnection *connection;

} CData;

static dbus_bool_t
connection_watch_callback (DBusWatch     *watch,
                           unsigned int   condition,
                           void          *data)
{
  return dbus_watch_handle (watch, condition);
}

static dbus_bool_t
add_watch (DBusWatch *watch,
	   void      *data)
{
  CData *cd = data;

  return _dbus_loop_add_watch (cd->loop,
                               watch,
                               connection_watch_callback,
                               cd, NULL);
}

static void
remove_watch (DBusWatch *watch,
	      void      *data)
{
  CData *cd = data;
  
  _dbus_loop_remove_watch (cd->loop,
                           watch, connection_watch_callback, cd);  
}

static void
connection_timeout_callback (DBusTimeout   *timeout,
                             void          *data)
{
  /* Can return FALSE on OOM but we just let it fire again later */
  dbus_timeout_handle (timeout);
}

static dbus_bool_t
add_timeout (DBusTimeout *timeout,
	     void        *data)
{
  CData *cd = data;

  return _dbus_loop_add_timeout (cd->loop,
                                 timeout, connection_timeout_callback, cd, NULL);
}

static void
remove_timeout (DBusTimeout *timeout,
		void        *data)
{
  CData *cd = data;

  _dbus_loop_remove_timeout (cd->loop,
                             timeout, connection_timeout_callback, cd);
}

static void
dispatch_status_function (DBusConnection    *connection,
                          DBusDispatchStatus new_status,
                          void              *data)
{
  DBusLoop *loop = data;
  
  if (new_status != DBUS_DISPATCH_COMPLETE)
    {
      while (!_dbus_loop_queue_dispatch (loop, connection))
        _dbus_wait_for_memory ();
    }
}

static void
cdata_free (void *data)
{
  CData *cd = data;

  dbus_connection_unref (cd->connection);
  _dbus_loop_unref (cd->loop);
  
  dbus_free (cd);
}

static CData*
cdata_new (DBusLoop       *loop,
           DBusConnection *connection)
{
  CData *cd;

  cd = dbus_new0 (CData, 1);
  if (cd == NULL)
    return NULL;

  cd->loop = loop;
  cd->connection = connection;

  dbus_connection_ref (cd->connection);
  _dbus_loop_ref (cd->loop);

  return cd;
}

dbus_bool_t
test_connection_setup (DBusLoop       *loop,
                       DBusConnection *connection)
{
  CData *cd;

  cd = NULL;
  
  dbus_connection_set_dispatch_status_function (connection, dispatch_status_function,
                                                loop, NULL);
  
  cd = cdata_new (loop, connection);
  if (cd == NULL)
    goto nomem;

  /* Because dbus-mainloop.c checks dbus_timeout_get_enabled(),
   * dbus_watch_get_enabled() directly, we don't have to provide
   * "toggled" callbacks.
   */
  
  if (!dbus_connection_set_watch_functions (connection,
                                            add_watch,
                                            remove_watch,
                                            NULL,
                                            cd, cdata_free))
    goto nomem;


  cd = cdata_new (loop, connection);
  if (cd == NULL)
    goto nomem;
  
  if (!dbus_connection_set_timeout_functions (connection,
                                              add_timeout,
                                              remove_timeout,
                                              NULL,
                                              cd, cdata_free))
    goto nomem;

  if (dbus_connection_get_dispatch_status (connection) != DBUS_DISPATCH_COMPLETE)
    {
      if (!_dbus_loop_queue_dispatch (loop, connection))
        goto nomem;
    }
  
  return TRUE;
  
 nomem:
  if (cd)
    cdata_free (cd);
  
  dbus_connection_set_dispatch_status_function (connection, NULL, NULL, NULL);
  dbus_connection_set_watch_functions (connection, NULL, NULL, NULL, NULL, NULL);
  dbus_connection_set_timeout_functions (connection, NULL, NULL, NULL, NULL, NULL);
  
  return FALSE;
}

void
test_connection_shutdown (DBusLoop       *loop,
                          DBusConnection *connection)
{
  if (!dbus_connection_set_watch_functions (connection,
                                            NULL,
                                            NULL,
                                            NULL,
                                            NULL, NULL))
    _dbus_assert_not_reached ("setting watch functions to NULL failed");
  
  if (!dbus_connection_set_timeout_functions (connection,
                                              NULL,
                                              NULL,
                                              NULL,
                                              NULL, NULL))
    _dbus_assert_not_reached ("setting timeout functions to NULL failed");

  dbus_connection_set_dispatch_status_function (connection, NULL, NULL, NULL);
}

typedef struct
{
  DBusLoop *loop;
  DBusServer *server;
} ServerData;

static void
serverdata_free (void *data)
{
  ServerData *sd = data;

  dbus_server_unref (sd->server);
  _dbus_loop_unref (sd->loop);
  
  dbus_free (sd);
}

static ServerData*
serverdata_new (DBusLoop       *loop,
                DBusServer     *server)
{
  ServerData *sd;

  sd = dbus_new0 (ServerData, 1);
  if (sd == NULL)
    return NULL;

  sd->loop = loop;
  sd->server = server;

  dbus_server_ref (sd->server);
  _dbus_loop_ref (sd->loop);

  return sd;
}

static dbus_bool_t
server_watch_callback (DBusWatch     *watch,
                       unsigned int   condition,
                       void          *data)
{
  /* FIXME this can be done in dbus-mainloop.c
   * if the code in activation.c for the babysitter
   * watch handler is fixed.
   */

  return dbus_watch_handle (watch, condition);
}

static dbus_bool_t
add_server_watch (DBusWatch  *watch,
                  void       *data)
{
  ServerData *context = data;

  return _dbus_loop_add_watch (context->loop,
                               watch, server_watch_callback, context,
                               NULL);
}

static void
remove_server_watch (DBusWatch  *watch,
                     void       *data)
{
  ServerData *context = data;
  
  _dbus_loop_remove_watch (context->loop,
                           watch, server_watch_callback, context);
}

static void
server_timeout_callback (DBusTimeout   *timeout,
                         void          *data)
{
  /* can return FALSE on OOM but we just let it fire again later */
  dbus_timeout_handle (timeout);
}

static dbus_bool_t
add_server_timeout (DBusTimeout *timeout,
                    void        *data)
{
  ServerData *context = data;

  return _dbus_loop_add_timeout (context->loop,
                                 timeout, server_timeout_callback, context, NULL);
}

static void
remove_server_timeout (DBusTimeout *timeout,
                       void        *data)
{
  ServerData *context = data;
  
  _dbus_loop_remove_timeout (context->loop,
                             timeout, server_timeout_callback, context);
}

dbus_bool_t
test_server_setup (DBusLoop      *loop,
                   DBusServer    *server)
{
  ServerData *sd;

  sd = serverdata_new (loop, server);
  if (sd == NULL)
    goto nomem;

  if (!dbus_server_set_watch_functions (server,
                                        add_server_watch,
                                        remove_server_watch,
                                        NULL,
                                        sd,
                                        serverdata_free))
    {
      return FALSE;
    }

  if (!dbus_server_set_timeout_functions (server,
                                          add_server_timeout,
                                          remove_server_timeout,
                                          NULL,
                                          sd, serverdata_free))
    {
      return FALSE;
    }   
  return TRUE;

 nomem:
  if (sd)
    serverdata_free (sd);
  
  test_server_shutdown (loop, server);
  
  return FALSE;
}

void
test_server_shutdown (DBusLoop         *loop,
                      DBusServer       *server)
{
  if (!dbus_server_set_watch_functions (server,
                                        NULL, NULL, NULL,
                                        NULL,
                                        NULL))
    _dbus_assert_not_reached ("setting watch functions to NULL failed");
  
  if (!dbus_server_set_timeout_functions (server,
                                          NULL, NULL, NULL,
                                          NULL,
                                          NULL))
    _dbus_assert_not_reached ("setting timeout functions to NULL failed");  
}
