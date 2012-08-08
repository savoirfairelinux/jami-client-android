LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	xml.cpp xml2cpp.cpp \
	generate_adaptor.cpp generate_proxy.cpp \
	generator_utils.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../include \
                    $(LOCAL_PATH)/../../dbus

# do not cross-compile, host tool only is needed
# FIXME Android NDK cannot build host tool, android sources needed...
LOCAL_MODULE := dbusxx-xml2cpp
LOCAL_CFLAGS += -Wall
LOCAL_SHARED_LIBRARIES += libexpat

include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)

LOCAL_SRC_FILES := introspect.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../include
#                    $(LOCAL_PATH)/../../dbus

LOCAL_MODULE := dbusxx-introspect
LOCAL_CFLAGS += -Wall

LOCAL_SHARED_LIBRARIES += libdbus-c++-1

include $(BUILD_EXECUTABLE)

