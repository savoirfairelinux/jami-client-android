#
# Copyright (c) 2013 Slient Circle LLC.  All rights reserved.
#
# @author Werner Dittmann <Werner.Dittmann@t-online.de>
#
###########
# zrtpcpp #
###########

LOCAL_PATH := @CMAKE_SOURCE_DIR@

include $(CLEAR_VARS)
LOCAL_MODULE := zrtpcpp

# include paths
LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/srtp $(LOCAL_PATH)/zrtp $(LOCAL_PATH)/bnlib \
                    $(LOCAL_PATH)/clients/tivi $(LOCAL_PATH)/clients/tiviAndroid/jni/sqlite3


# LOCAL_CFLAGS := @random@

LOCAL_SRC_FILES += @zrtpcpp_src_spc@

LOCAL_STATIC_LIBRARIES := $(LOCAL_PATH)/clients/tiviAndroid/jni/sqlite3/sqlite3

include $(BUILD_SHARED_LIBRARY)
