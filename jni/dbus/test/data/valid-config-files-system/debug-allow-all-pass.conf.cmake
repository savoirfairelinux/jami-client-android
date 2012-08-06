<!-- Bus that listens on a debug pipe and doesn't create any restrictions -->

<!DOCTYPE busconfig PUBLIC "-//freedesktop//DTD D-BUS Bus Configuration 1.0//EN"
 "http://www.freedesktop.org/standards/dbus/1.0/busconfig.dtd">
<busconfig>
  <listen>@TEST_LISTEN@</listen>
  <type>system</type>
  <servicehelper>@TEST_LAUNCH_HELPER_BINARY@</servicehelper>
  <servicedir>@TEST_VALID_SERVICE_SYSTEM_DIR@</servicedir>
  <policy context="default">
    <allow send_interface="*"/>
    <allow receive_interface="*"/>
    <allow own="*"/>
    <allow user="*"/>
  </policy>
</busconfig>
