include $(CLEAR_VARS)

LOCAL_COMMONCPP_PATH = libucommon/sources
COMMON_SRC_PATH = ${LOCAL_COMMONCPP_PATH}/commoncpp
CORE_SRC_PATH = ${LOCAL_COMMONCPP_PATH}/corelib

LOCAL_MODULE     := libccgnu2

LOCAL_CPPFLAGS   += -std=c++11 -Wno-psabi -frtti -pthread -fexceptions

LOCAL_C_INCLUDES += $(LOCAL_COMMONCPP_PATH)/ \
                    $(LOCAL_COMMONCPP_PATH)/inc \
        

LOCAL_SRC_FILES  := $(COMMON_SRC_PATH)/address.cpp \
                    $(COMMON_SRC_PATH)/dso.cpp \
                    $(COMMON_SRC_PATH)/linked.cpp \
                    $(COMMON_SRC_PATH)/map.cpp \
                    $(COMMON_SRC_PATH)/process.cpp \
                    $(COMMON_SRC_PATH)/socket.cpp \
                    $(COMMON_SRC_PATH)/thread.cpp \
                    $(COMMON_SRC_PATH)/applog.cpp \
                    $(COMMON_SRC_PATH)/exception.cpp \
                    $(COMMON_SRC_PATH)/mime.cpp \
                    $(COMMON_SRC_PATH)/strchar.cpp \
                    $(COMMON_SRC_PATH)/tokenizer.cpp \
                    $(COMMON_SRC_PATH)/dccp.cpp \
                    $(COMMON_SRC_PATH)/file.cpp \
                    $(COMMON_SRC_PATH)/pointer.cpp \
                    $(COMMON_SRC_PATH)/slog.cpp \
                    $(COMMON_SRC_PATH)/tcp.cpp \
                    $(COMMON_SRC_PATH)/udp.cpp \
                    $(CORE_SRC_PATH)/string.cpp \
                    $(CORE_SRC_PATH)/access.cpp \
                    $(CORE_SRC_PATH)/cpr.cpp \
                    $(CORE_SRC_PATH)/linked.cpp \
                    $(CORE_SRC_PATH)/memory.cpp \
                    $(CORE_SRC_PATH)/regex.cpp \
                    $(CORE_SRC_PATH)/tcpbuffer.cpp \
                    $(CORE_SRC_PATH)/xml.cpp \
                    $(CORE_SRC_PATH)/atomic.cpp \
                    $(CORE_SRC_PATH)/datetime.cpp \
                    $(CORE_SRC_PATH)/thread.cpp \
                    $(CORE_SRC_PATH)/bitmap.cpp \
                    $(CORE_SRC_PATH)/file.cpp \
                    $(CORE_SRC_PATH)/object.cpp \
                    $(CORE_SRC_PATH)/socket.cpp \
                    $(CORE_SRC_PATH)/timer.cpp \
                    $(CORE_SRC_PATH)/containers.cpp \
                    $(CORE_SRC_PATH)/fsys.cpp \
                    $(CORE_SRC_PATH)/persist.cpp \
                    $(CORE_SRC_PATH)/stream.cpp \
                    $(CORE_SRC_PATH)/unicode.cpp \
                    $(CORE_SRC_PATH)/counter.cpp \
                    $(CORE_SRC_PATH)/keydata.cpp \
                    $(CORE_SRC_PATH)/protocols.cpp \
                    $(CORE_SRC_PATH)/vector.cpp \

                    #$(CORE_SRC_PATH)/mapped.cpp \

include $(BUILD_STATIC_LIBRARY)
