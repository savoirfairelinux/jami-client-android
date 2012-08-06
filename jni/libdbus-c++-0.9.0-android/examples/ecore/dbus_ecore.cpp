#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include "dbus_ecore.h"

#include <iostream>
#include <vector>

using namespace std;

static const char *DBUS_SERVER_NAME = "org.freedesktop.DBus";
static const char *DBUS_SERVER_PATH = "/org/freedesktop/DBus";

typedef vector <string> Names;

DBusBrowser::DBusBrowser(::DBus::Connection &conn)
  : ::DBus::ObjectProxy(conn, DBUS_SERVER_PATH, DBUS_SERVER_NAME)
{
  typedef std::vector< std::string > Names;

  Names names = ListNames();

  for (Names::iterator it = names.begin(); it != names.end(); ++it)
  {
    cout << *it << endl;
  }
}

void DBusBrowser::NameOwnerChanged(
  const std::string &name, const std::string &old_owner, const std::string &new_owner)
{
  cout << name << ": " << old_owner << " -> " << new_owner << endl;
}

void DBusBrowser::NameLost(const std::string &name)
{
  cout << name << " lost" << endl;
}

void DBusBrowser::NameAcquired(const std::string &name)
{
  cout << name << " acquired" << endl;
}

DBus::Ecore::BusDispatcher dispatcher;

void niam(int sig)
{
  ecore_main_loop_quit();
}

int main(int argc, char *argv[])
{
  signal(SIGTERM, niam);
  signal(SIGINT, niam);

  ecore_init();

  DBus::default_dispatcher = &dispatcher;

  DBus::Connection conn = DBus::Connection::SessionBus();

  DBusBrowser browser(conn);

  ecore_main_loop_begin();
  ecore_shutdown();

  return 0;
}
