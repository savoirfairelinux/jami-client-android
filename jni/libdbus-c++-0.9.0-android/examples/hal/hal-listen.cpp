#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include "hal-listen.h"

#include <signal.h>
#include <iostream>

HalManagerProxy::HalManagerProxy(DBus::Connection &connection)
  : DBus::InterfaceProxy("org.freedesktop.Hal.Manager"),
    DBus::ObjectProxy(connection, "/org/freedesktop/Hal/Manager", "org.freedesktop.Hal")
{
  connect_signal(HalManagerProxy, DeviceAdded, DeviceAddedCb);
  connect_signal(HalManagerProxy, DeviceRemoved, DeviceRemovedCb);

  std::vector< std::string > devices = GetAllDevices();

  std::vector< std::string >::iterator it;
  for (it = devices.begin(); it != devices.end(); ++it)
  {
    DBus::Path udi = *it;

    std::cout << "found device " << udi << std::endl;

    _devices[udi] = new HalDeviceProxy(connection, udi);
  }
}

std::vector< std::string > HalManagerProxy::GetAllDevices()
{
  std::vector< std::string > udis;
  DBus::CallMessage call;

  call.member("GetAllDevices");

  DBus::Message reply = invoke_method(call);
  DBus::MessageIter it = reply.reader();

  it >> udis;
  return udis;
}

void HalManagerProxy::DeviceAddedCb(const DBus::SignalMessage &sig)
{
  DBus::MessageIter it = sig.reader();
  std::string devname;

  it >> devname;

  DBus::Path udi(devname);

  _devices[devname] = new HalDeviceProxy(conn(), udi);
  std::cout << "added device " << udi << std::endl;
}

void HalManagerProxy::DeviceRemovedCb(const DBus::SignalMessage &sig)
{
  DBus::MessageIter it = sig.reader();
  std::string devname;

  it >> devname;

  std::cout << "removed device " << devname << std::endl;

  _devices.erase(devname);
}

HalDeviceProxy::HalDeviceProxy(DBus::Connection &connection, DBus::Path &udi)
  : DBus::InterfaceProxy("org.freedesktop.Hal.Device"),
    DBus::ObjectProxy(connection, udi, "org.freedesktop.Hal")
{
  connect_signal(HalDeviceProxy, PropertyModified, PropertyModifiedCb);
  connect_signal(HalDeviceProxy, Condition, ConditionCb);
}

void HalDeviceProxy::PropertyModifiedCb(const DBus::SignalMessage &sig)
{
  typedef DBus::Struct< std::string, bool, bool > HalProperty;

  DBus::MessageIter it = sig.reader();
  int32_t number;

  it >> number;

  DBus::MessageIter arr = it.recurse();

  for (int i = 0; i < number; ++i, ++arr)
  {
    HalProperty hp;

    arr >> hp;

    std::cout << "modified property " << hp._1 << " in " << path() << std::endl;
  }
}

void HalDeviceProxy::ConditionCb(const DBus::SignalMessage &sig)
{
  DBus::MessageIter it = sig.reader();
  std::string condition;

  it >> condition;

  std::cout << "encountered condition " << condition << " in " << path() << std::endl;
}

DBus::BusDispatcher dispatcher;

void niam(int sig)
{
  dispatcher.leave();
}

int main()
{
  signal(SIGTERM, niam);
  signal(SIGINT, niam);

  DBus::default_dispatcher = &dispatcher;

  DBus::Connection conn = DBus::Connection::SystemBus();

  HalManagerProxy hal(conn);

  dispatcher.enter();

  return 0;
}
