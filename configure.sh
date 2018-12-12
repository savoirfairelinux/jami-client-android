#!/bin/sh

if [ -z "$ANDROID_NDK" ]; then
    echo "Please set the ANDROID_NDK environment variable with its path."
    exit 1
fi

if [ -z "$NDK_TOOLCHAIN_PATH" ]; then
    echo "Please set the NDK_TOOLCHAIN_PATH environment variable with its path."
    exit 1
fi

if [ -z "$ANDROID_ABI" ]; then
    echo "Please set ANDROID_ABI to your architecture: armeabi-v7a, armeabi, arm64-v8a, x86, x86_64 or mips."
    exit 1
fi

# ANDROID_API must be previously set by compile.sh or env.sh
if [ -z "$ANDROID_API" ];then
    echo "ANDROID_API not set, call ./compile.sh first"
    exit 1
fi

CPPFLAGS="-I${RING_SRC_DIR}/contrib/${TARGET_TUPLE}/include "
LDFLAGS="$LDFLAGS -L${RING_SRC_DIR}/contrib/${TARGET_TUPLE}/lib "

SYSROOT=$NDK_TOOLCHAIN_PATH/sysroot

CPPFLAGS="$CPPFLAGS" \
CFLAGS="$CFLAGS ${RING_EXTRA_CFLAGS}" \
CXXFLAGS="$CXXFLAGS ${RING_EXTRA_CXXFLAGS}" \
LDFLAGS="$LDFLAGS ${RING_EXTRA_LDFLAGS}" \
CC="${CROSS_COMPILE}clang" \
CXX="${CROSS_COMPILE}clang++" \
NM="${CROSS_COMPILE}nm" \
STRIP="${CROSS_COMPILE}strip" \
RANLIB="${CROSS_COMPILE}ranlib" \
AR="${CROSS_COMPILE}ar" \
AS="${CROSS_COMPILE}as" \
PKG_CONFIG_LIBDIR=$RING_SRC_DIR/contrib/$TARGET_TUPLE/lib/pkgconfig \
$RING_SRC_DIR/configure --host=$TARGET_TUPLE $EXTRA_PARAMS \
                   --disable-shared --with-opensl --without-dbus --without-alsa --without-pulse --without-speexdsp --enable-accel\
                   --prefix=$RING_SRC_DIR/install-android-$TARGET_TUPLE \
                   $*
