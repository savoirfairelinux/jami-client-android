#ifndef __DEMO_PROPS_SERVER_H
#define __DEMO_PROPS_SERVER_H

#include <dbus-c++/dbus.h>
#include "propsgs-glue-adaptor.h"

class PropsServer
  : public org::freedesktop::DBus::PropsGSDemo_adaptor,
  public DBus::IntrospectableAdaptor,
  public DBus::PropertiesAdaptor,
  public DBus::ObjectAdaptor
{
public:

  PropsServer(DBus::Connection &connection);

  void on_set_property
  (DBus::InterfaceAdaptor &interface, const std::string &property, const DBus::Variant &value);
};

#endif//__DEMO_PROPS_SERVER_H
