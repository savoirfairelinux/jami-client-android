
LOCAL_PATH:=@CMAKE_SOURCE_DIR@/clients/tiviAndroid/jni/sqlite3

#####################################################################
#            build sqlite3                                            #
#####################################################################
include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_MODULE     :=sqlite3
LOCAL_SRC_FILES  :=sqlite3.c

include $(BUILD_STATIC_LIBRARY)
# include $(BUILD_SHARED_LIBRARY)
