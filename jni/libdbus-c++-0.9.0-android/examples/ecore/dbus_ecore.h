#ifndef __DEMO_DBUS_BROWSER_H
#define __DEMO_DBUS_BROWSER_H

#include <dbus-c++/dbus.h>
#include <dbus-c++/ecore-integration.h>
#include <Ecore.h>

#include "dbus_ecore-glue.h"

class DBusBrowser
  : public org::freedesktop::DBus_proxy,
  public DBus::IntrospectableProxy,
  public DBus::ObjectProxy
{
public:

  DBusBrowser(::DBus::Connection &conn);

private:

  void NameOwnerChanged(const std::string &, const std::string &, const std::string &);

  void NameLost(const std::string &);

  void NameAcquired(const std::string &);

private:

};

#endif//__DEMO_DBUS_BROWSER_H
