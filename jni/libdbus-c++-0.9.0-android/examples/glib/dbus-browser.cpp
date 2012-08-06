#include "dbus-browser.h"

#include <xml.h>
#include <iostream>

using namespace std;

static const char *DBUS_SERVER_NAME = "org.freedesktop.DBus";
static const char *DBUS_SERVER_PATH = "/org/freedesktop/DBus";

DBusBrowser::DBusBrowser(::DBus::Connection &conn)
  : ::DBus::ObjectProxy(conn, DBUS_SERVER_PATH, DBUS_SERVER_NAME)
{
  set_title("D-Bus Browser");
  set_border_width(5);
  set_default_size(400, 500);

  typedef std::vector< std::string > Names;

  Names names = ListNames();

  for (Names::iterator it = names.begin(); it != names.end(); ++it)
  {
    _cb_busnames.append_text(*it);
  }

  _cb_busnames.signal_changed().connect(sigc::mem_fun(*this, &DBusBrowser::on_select_busname));

  _tm_inspect = Gtk::TreeStore::create(_records);
  _tv_inspect.set_model(_tm_inspect);
  _tv_inspect.append_column("Node", _records.name);

  _sc_tree.set_policy(Gtk::POLICY_AUTOMATIC, Gtk::POLICY_AUTOMATIC);
  _sc_tree.add(_tv_inspect);

  _vbox.pack_start(_cb_busnames, Gtk::PACK_SHRINK);
  _vbox.pack_start(_sc_tree);

  add(_vbox);

  show_all_children();
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

void DBusBrowser::on_select_busname()
{
  Glib::ustring busname = _cb_busnames.get_active_text();
  if (busname.empty()) return;

  _tm_inspect->clear();
  _inspect_append(NULL, "", busname);
}

void DBusBrowser::_inspect_append(Gtk::TreeModel::Row *row, const std::string &buspath, const std::string &busname)
{
  DBusInspector inspector(conn(), buspath.empty() ? "/" : buspath.c_str(), busname.c_str());

  ::DBus::Xml::Document doc(inspector.Introspect());
  ::DBus::Xml::Node &root = *(doc.root);

  ::DBus::Xml::Nodes ifaces = root["interface"];

  for (::DBus::Xml::Nodes::iterator ii = ifaces.begin(); ii != ifaces.end(); ++ii)
  {
    ::DBus::Xml::Node &iface = **ii;

    Gtk::TreeModel::Row i_row = row
                                ? *(_tm_inspect->append(row->children()))
                                : *(_tm_inspect->append());
    i_row[_records.name] = "interface: " + iface.get("name");

    ::DBus::Xml::Nodes methods = iface["method"];

    for (::DBus::Xml::Nodes::iterator im = methods.begin(); im != methods.end(); ++im)
    {
      Gtk::TreeModel::Row m_row = *(_tm_inspect->append(i_row.children()));
      m_row[_records.name] = "method: " + (*im)->get("name");
    }

    ::DBus::Xml::Nodes signals = iface["signal"];

    for (::DBus::Xml::Nodes::iterator is = signals.begin(); is != signals.end(); ++is)
    {
      Gtk::TreeModel::Row s_row = *(_tm_inspect->append(i_row.children()));
      s_row[_records.name] = "signal: " + (*is)->get("name");
    }
  }

  ::DBus::Xml::Nodes nodes = root["node"];

  for (::DBus::Xml::Nodes::iterator in = nodes.begin(); in != nodes.end(); ++in)
  {
    std::string name = (*in)->get("name");

    Gtk::TreeModel::Row n_row = row
                                ? *(_tm_inspect->append(row->children()))
                                : *(_tm_inspect->append());
    n_row[_records.name] = name;

    _inspect_append(&n_row, buspath + "/" + name, busname);
  }
}

DBus::Glib::BusDispatcher dispatcher;

int main(int argc, char *argv[])
{
  Gtk::Main kit(argc, argv);

  DBus::default_dispatcher = &dispatcher;

  dispatcher.attach(NULL);

  // activate one of both for either system or session bus
  // TODO: choose in the GUI
  DBus::Connection conn = DBus::Connection::SessionBus();
  //DBus::Connection conn = DBus::Connection::SystemBus();

  DBusBrowser browser(conn);

  Gtk::Main::run(browser);

  return 0;
}
