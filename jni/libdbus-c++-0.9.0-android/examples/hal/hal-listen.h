#ifndef __DEMO_HAL_LISTEN_H
#define __DEMO_HAL_LISTEN_H

#include <dbus-c++/dbus.h>
#include <vector>
#include <map>

class HalDeviceProxy;

class HalManagerProxy
  : public DBus::InterfaceProxy,
  public DBus::ObjectProxy
{
public:

  HalManagerProxy(DBus::Connection &connection);

  std::vector< std::string > GetAllDevices();

private:

  void DeviceAddedCb(const DBus::SignalMessage &sig);

  void DeviceRemovedCb(const DBus::SignalMessage &sig);

  std::map< std::string, DBus::RefPtr< HalDeviceProxy > > _devices;
};

class HalDeviceProxy
  : public DBus::InterfaceProxy,
  public DBus::ObjectProxy
{
public:

  HalDeviceProxy(DBus::Connection &connection, DBus::Path &udi);

private:

  void PropertyModifiedCb(const DBus::SignalMessage &sig);

  void ConditionCb(const DBus::SignalMessage &sig);
};

#endif//__DEMO_HAL_LISTEN_H
