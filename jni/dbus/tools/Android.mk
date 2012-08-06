LOCAL_PATH := $(call my-dir)

# common

include $(CLEAR_VARS)

LOCAL_SRC_FILES := dbus-print-message.c
LOCAL_C_INCLUDES += $(call include-path-for, dbus) \
                    $(LOCAL_PATH)/..
LOCAL_SHARED_LIBRARIES += libdbus
LOCAL_CFLAGS += \
	-DDBUS_COMPILATION \
	-DDBUS_MACHINE_UUID_FILE=\"/etc/machine-id\"
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE := libdbus-tools-common
include $(BUILD_SHARED_LIBRARY)

# dbus-monitor

include $(CLEAR_VARS)

LOCAL_SRC_FILES := dbus-monitor.c
LOCAL_C_INCLUDES += $(call include-path-for, dbus) \
                    $(LOCAL_PATH)/..
LOCAL_SHARED_LIBRARIES += libdbus
LOCAL_STATIC_LIBRARIES += libdbus-tools-common
LOCAL_CFLAGS += \
	-DDBUS_COMPILATION \
	-DDBUS_MACHINE_UUID_FILE=\"/etc/machine-id\"
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE := dbus-monitor
include $(BUILD_EXECUTABLE)

# dbus-send

include $(CLEAR_VARS)

LOCAL_SRC_FILES := dbus-send.c
LOCAL_C_INCLUDES += $(call include-path-for, dbus) \
                    $(LOCAL_PATH)/..
LOCAL_SHARED_LIBRARIES += libdbus
LOCAL_STATIC_LIBRARIES += libdbus-tools-common
LOCAL_CFLAGS += \
	-DDBUS_COMPILATION \
	-DDBUS_MACHINE_UUID_FILE=\"/etc/machine-id\"
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE := dbus-send
include $(BUILD_EXECUTABLE)
