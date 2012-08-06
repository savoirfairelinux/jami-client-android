#! /bin/bash

SCRIPTNAME=$0
WRAPPED_SCRIPT=$1
shift

die() 
{
    if ! test -z "$DBUS_SESSION_BUS_PID" ; then
        echo "killing message bus "$DBUS_SESSION_BUS_PID >&2
        kill -9 $DBUS_SESSION_BUS_PID
    fi
    echo $SCRIPTNAME: $* >&2
    exit 1
}

if test -z "$DBUS_TOP_BUILDDIR" ; then
    die "Must set DBUS_TOP_BUILDDIR"
fi

## convenient to be able to ctrl+C without leaking the message bus process
trap 'die "Received SIGINT"' INT

CONFIG_FILE=./run-with-tmp-session-bus.conf
SERVICE_DIR="$DBUS_TOP_BUILDDIR/test/data/valid-service-files"
ESCAPED_SERVICE_DIR=`echo $SERVICE_DIR | sed -e 's/\//\\\\\\//g'`
echo "escaped service dir is: $ESCAPED_SERVICE_DIR" >&2

if test -z "$SOURCE_CONFIG_FILE"; then
    SOURCE_CONFIG_FILE="$DBUS_TOP_BUILDDIR/bus/session.conf";
fi
## create a configuration file based on the standard session.conf
cat $SOURCE_CONFIG_FILE |  \
    sed -e 's/<standard_session_servicedirs.*$/<servicedir>'$ESCAPED_SERVICE_DIR'<\/servicedir>/g' |  \
    sed -e 's/<include.*$//g'                \
  > $CONFIG_FILE

echo "Created configuration file $CONFIG_FILE" >&2

if ! test -e "$DBUS_TOP_BUILDDIR"/bus/dbus-daemon ; then
    die "$DBUS_TOP_BUILDDIR/bus/dbus-daemon does not exist"
fi

PATH="$DBUS_TOP_BUILDDIR"/bus:$PATH
export PATH

## the libtool script found by the path search should already do this, but
LD_LIBRARY_PATH=$DBUS_TOP_BUILDDIR/dbus/.libs:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH
unset DBUS_SESSION_BUS_ADDRESS
unset DBUS_SESSION_BUS_PID

echo "Running $DBUS_TOP_BUILDDIR/tools/dbus-launch --sh-syntax --config-file=$CONFIG_FILE" >&2

DBUS_USE_TEST_BINARY=1 
export DBUS_USE_TEST_BINARY
eval `$DBUS_TOP_BUILDDIR/tools/dbus-launch --sh-syntax --config-file=$CONFIG_FILE`

if test -z "$DBUS_SESSION_BUS_PID" ; then
    die "Failed to launch message bus for test script to run"
fi

echo "Started bus pid $DBUS_SESSION_BUS_PID at $DBUS_SESSION_BUS_ADDRESS" >&2

# Execute wrapped script
echo "Running $WRAPPED_SCRIPT $@" >&2
$WRAPPED_SCRIPT "$@" || die "script \"$WRAPPED_SCRIPT\" failed"

kill -TERM $DBUS_SESSION_BUS_PID || die "Message bus vanished! should not have happened" && echo "Killed daemon $DBUS_SESSION_BUS_PID" >&2

sleep 2

## be sure it really died 
kill -9 $DBUS_SESSION_BUS_PID > /dev/null 2>&1 || true

exit 0
