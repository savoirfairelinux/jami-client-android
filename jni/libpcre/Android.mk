include $(CLEAR_VARS)

MY_PCRE = libpcre/sources

LOCAL_MODULE := libpcre
LOCAL_CFLAGS := -DHAVE_CONFIG_H

LOCAL_SRC_FILES :=  \
  $(MY_PCRE)/pcre_compile.c \
  $(MY_PCRE)/pcre_chartables.c \
  $(MY_PCRE)/pcre_config.c \
  $(MY_PCRE)/pcre_dfa_exec.c \
  $(MY_PCRE)/pcre_exec.c \
  $(MY_PCRE)/pcre_fullinfo.c \
  $(MY_PCRE)/pcre_get.c \
  $(MY_PCRE)/pcre_globals.c \
  $(MY_PCRE)/pcre_info.c \
  $(MY_PCRE)/pcre_maketables.c \
  $(MY_PCRE)/pcre_newline.c \
  $(MY_PCRE)/pcre_ord2utf8.c \
  $(MY_PCRE)/pcre_refcount.c \
  $(MY_PCRE)/pcre_study.c \
  $(MY_PCRE)/pcre_tables.c \
  $(MY_PCRE)/pcre_try_flipped.c \
  $(MY_PCRE)/pcre_ucd.c \
  $(MY_PCRE)/pcre_valid_utf8.c \
  $(MY_PCRE)/pcre_version.c \
  $(MY_PCRE)/pcre_xclass.c

include $(BUILD_STATIC_LIBRARY)
