#ifndef __DEMO_PROPS_SERVER_H
#define __DEMO_PROPS_SERVER_H

#include <dbus-c++/dbus.h>
#include "propsgs-glue-proxy.h"

class PropsClient
  : public org::freedesktop::DBus::PropsGSDemo_proxy,
  public DBus::IntrospectableProxy,
  public DBus::PropertiesProxy,
  public DBus::ObjectProxy
{
public:

  PropsClient(DBus::Connection &connection, const char *path, const char *name);

  void MessageChanged(const std::string &message);

  void DataChanged(const double &data);
};

#endif//__DEMO_PROPS_SERVER_H
