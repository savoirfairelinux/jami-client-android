LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	connection.cpp    \
	debug.cpp    \
	dispatcher.cpp    \
	error.cpp    \
	eventloop.cpp    \
	eventloop-integration.cpp    \
	interface.cpp    \
	introspection.cpp    \
	message.cpp    \
	object.cpp    \
	pendingcall.cpp    \
	pipe.cpp    \
	property.cpp    \
	server.cpp    \
	types.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../include \
                    $(LOCAL_PATH)/../../dbus

LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/../include

LOCAL_MODULE := libdbus-c++-1
LOCAL_CFLAGS += -Wno-unused-parameter -fexceptions -frtti
LOCAL_SHARED_LIBRARIES += libdbus

include $(BUILD_SHARED_LIBRARY)

