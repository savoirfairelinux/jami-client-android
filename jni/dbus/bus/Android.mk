LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# FIXME
LOCAL_C_INCLUDES:= \
	$(call include-path-for, dbus) \
	$(call include-path-for, dbus)/dbus \
	$(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/../../libexpat \

LOCAL_CFLAGS:=-O3
LOCAL_CFLAGS+=-DDBUS_COMPILATION
#LOCAL_CFLAGS+=-DDBUS_MACHINE_UUID_FILE=\"/system/etc/machine-id\"
LOCAL_CFLAGS+=-DDBUS_DAEMON_NAME=\"dbus-daemon\"
LOCAL_CFLAGS+=-DDBUS_SYSTEM_CONFIG_FILE=\"/system/etc/dbus.conf\"
LOCAL_CFLAGS+=-DDBUS_SESSION_CONFIG_FILE=\"/system/etc/session.conf\"

# We get warning in the _DBUS_ASSERT_ERROR_IS_SET macro.  Suppress
# this warning so that we can compile with Werror.  The warning
# is also ignored in dbus-1.4.6.
LOCAL_CFLAGS+=-Wno-address

LOCAL_LDFLAGS += -lexpat

LOCAL_SRC_FILES:= \
	activation.c \
	bus.c \
	config-loader-expat.c \
	config-parser.c \
    config-parser-common.c \
	connection.c \
	desktop-file.c \
	dir-watch-default.c \
	dispatch.c \
	driver.c \
	expirelist.c \
	main.c \
	policy.c \
	selinux.c \
	services.c \
	signals.c \
	utils.c

LOCAL_SHARED_LIBRARIES := \
	libexpat \
	libdbus

LOCAL_MODULE:=dbus-daemon

include $(BUILD_EXECUTABLE)

