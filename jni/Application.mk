NDK_TOOLCHAIN_VERSION := 4.8

APP_PLATFORM := android-14
APP_OPTIM := debug
APP_STL := gnustl_shared
APP_ABI := armeabi-v7a

APP_MODULE += libopus
APP_MODULES += libccgnu2
APP_MODULES += libsamplerate

#APP_MODULES += libexpat_static
#APP_MODULES += libexpat_shared
APP_MODULES += libccrtp1
APP_MODULES += libsndfile


APP_MODULES += libpcre

APP_MODULES += libcrypto
#APP_MODULES += openssl

APP_MODULES += libspeexresampler
APP_MODULES += libcodec_ulaw
APP_MODULES += libcodec_alaw
APP_MODULES += libcodec_g722
APP_MODULES += libcodec_opus
APP_MODULES += libcodec_gsm
APP_MODULES += libcodec_speex_nb
APP_MODULES += libcodec_speex_ub
APP_MODULES += libcodec_speex_wb

APP_MODULES += libsflphone
