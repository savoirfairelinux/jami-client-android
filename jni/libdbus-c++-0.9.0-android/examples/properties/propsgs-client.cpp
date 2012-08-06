#include "propsgs-client.h"
#include <iostream>
#include <signal.h>
#include <pthread.h>

using namespace org::freedesktop::DBus;

static const char *PROPS_SERVER_NAME = "org.freedesktop.DBus.Examples.Properties";
static const char *PROPS_SERVER_PATH = "/org/freedesktop/DBus/Examples/Properties";

PropsClient::PropsClient(DBus::Connection &connection, const char *path, const char *name)
  : DBus::ObjectProxy(connection, path, name)
{
}

void PropsClient::MessageChanged(const std::string &message)
{
  std::cout << "MessageChanged signal, new value: " << message << "\n";
};

void PropsClient::DataChanged(const double &data)
{
  std::cout << "DataChanged signal, new value:" << data << "\n";
};

void *test_property_proxy(void *input)
{
  PropsClient *client = static_cast<PropsClient *>(input);

  std::cout << "read property 'Version', value:" << client->Version() << "\n";

  std::cout << "read property 'Message', value:" << client->Message() << "\n";

  client->Message("message set by property access");
  std::cout << "wrote property 'Message'\n";

  std::cout << "read property 'Message', value:" << client->Message() << "\n";

  client->Data(1.1);
  std::cout << "wrote property 'Data'\n";

  return NULL;
}

DBus::BusDispatcher dispatcher;

void niam(int sig)
{
  dispatcher.leave();
  pthread_exit(NULL);
}

int main()
{
  signal(SIGTERM, niam);
  signal(SIGINT, niam);

  DBus::default_dispatcher = &dispatcher;

  DBus::_init_threading();

  DBus::Connection conn = DBus::Connection::SessionBus();

  PropsClient client(conn, PROPS_SERVER_PATH, PROPS_SERVER_NAME);

  pthread_t thread;
  pthread_create(&thread, NULL, test_property_proxy, &client);

  dispatcher.enter();

  return 0;
}
