<!-- Bus that listens on a debug pipe and requires SHA1 auth, used to test SHA1 -->

<!DOCTYPE busconfig PUBLIC "-//freedesktop//DTD D-BUS Bus Configuration 1.0//EN"
 "http://www.freedesktop.org/standards/dbus/1.0/busconfig.dtd">
<busconfig>
  <listen>@TEST_LISTEN@</listen>
  <servicedir>@TEST_VALID_SERVICE_DIR@</servicedir>
  <auth>DBUS_COOKIE_SHA1</auth>
  <policy context="default">
    <allow send_interface="*"/>
    <allow receive_interface="*"/>
    <allow own="*"/>
    <allow user="*"/>
  </policy>
</busconfig>
