#ifndef __DEMO_DBUS_BROWSER_H
#define __DEMO_DBUS_BROWSER_H

#include <dbus-c++/dbus.h>
#include <dbus-c++/glib-integration.h>
#include <gtkmm.h>

#include "dbus-glue.h"

class DBusInspector
  : public DBus::IntrospectableProxy,
  public DBus::ObjectProxy
{
public:

  DBusInspector(DBus::Connection &conn, const char *path, const char *service)
    : DBus::ObjectProxy(conn, path, service)
  {}
};

class DBusBrowser
  : public org::freedesktop::DBus_proxy,
  public DBus::IntrospectableProxy,
  public DBus::ObjectProxy,
  public Gtk::Window
{
public:

  DBusBrowser(::DBus::Connection &);

private:

  void NameOwnerChanged(const std::string &, const std::string &, const std::string &);

  void NameLost(const std::string &);

  void NameAcquired(const std::string &);

  void on_select_busname();

  void _inspect_append(Gtk::TreeModel::Row *, const std::string &, const std::string &);

private:

  class InspectRecord : public Gtk::TreeModel::ColumnRecord
  {
  public:

    InspectRecord()
    {
      add(name);
    }

    Gtk::TreeModelColumn<Glib::ustring> name;
  };

  Gtk::VBox			_vbox;
  Gtk::ScrolledWindow		_sc_tree;
  Gtk::ComboBoxText		_cb_busnames;
  Gtk::TreeView			_tv_inspect;
  Glib::RefPtr<Gtk::TreeStore>	_tm_inspect;
  InspectRecord			_records;
};

#endif//__DEMO_DBUS_BROWSER_H
