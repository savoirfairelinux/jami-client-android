NDK_TOOLCHAIN_VERSION := 4.8

APP_PLATFORM := android-14
APP_OPTIM := debug
APP_STL := gnustl_shared
APP_ABI := armeabi-v7a

APP_MODULES += libcodec_ulaw
APP_MODULES += libcodec_alaw
APP_MODULES += libcodec_g722
APP_MODULES += libcodec_opus
APP_MODULES += libcodec_gsm
APP_MODULES += libcodec_speex_nb
APP_MODULES += libcodec_speex_ub
APP_MODULES += libcodec_speex_wb
APP_MODULES += libsflphone
