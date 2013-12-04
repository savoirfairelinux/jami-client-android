#
# Copyright (c) 2013 Slient Circle LLC.  All rights reserved.
#
# @author Werner Dittmann <Werner.Dittmann@t-online.de>
#
# ZRTP version: @VERSION@

commit := $(shell git rev-parse --short HEAD)

LOCAL_PATH := @CMAKE_SOURCE_DIR@
ROOT_SRC_PATH := $(LOCAL_PATH)

#
# Define and build the zrtpcpp static lib
#
include $(CLEAR_VARS)
LOCAL_MODULE := zrtpcpp
LOCAL_CPP_FEATURES := @local_cpp_features@

dummy := $(shell echo "char zrtpBuildInfo[] = \"@VERSION@:$(commit):$(TARGET_ARCH_ABI)\";" > $(ROOT_SRC_PATH)/buildinfo_$(TARGET_ARCH_ABI).c)

#
# set to false if testing/compiling new modules to catch undefined symbols (if build shared lib without TIVI_ENV)
# LOCAL_ALLOW_UNDEFINED_SYMBOLS := true

# include paths for zrtpcpp modules
LOCAL_C_INCLUDES += $(ROOT_SRC_PATH) $(ROOT_SRC_PATH)/srtp $(ROOT_SRC_PATH)/zrtp $(ROOT_SRC_PATH)/bnlib \
                    $(ROOT_SRC_PATH)/clients/tivi $(ROOT_SRC_PATH)/clients/tivi/android/jni/sqlite3

LOCAL_CFLAGS := -DSUPPORT_NON_NIST
LOCAL_SRC_FILES := clients/tivi/android/jni/sqlite3/sqlite3.c
LOCAL_SRC_FILES += buildinfo_$(TARGET_ARCH_ABI).c
LOCAL_SRC_FILES += @zrtpcpp_src_spc@

include $(BUILD_STATIC_LIBRARY)
