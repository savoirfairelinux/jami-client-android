#!/bin/sh

if [ -z "$ANDROID_NDK" ]; then
    echo "Please set the ANDROID_NDK environment variable with its path."
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

CFLAGS="-fpic -g -O2 -fstrict-aliasing -funsafe-math-optimizations"
if [ -n "$HAVE_ARM" -a ! -n "$HAVE_64" ]; then
    CFLAGS="${CFLAGS} -mlong-calls"
fi

LDFLAGS="-Wl,-Bdynamic,-dynamic-linker=/system/bin/linker -Wl,--no-undefined"

if [ -n "$HAVE_ARM" ]; then
    if [ ${ANDROID_ABI} = "armeabi-v7a" ]; then
        LDFLAGS="$LDFLAGS -Wl,--fix-cortex-a8"
    fi
fi

CPPFLAGS="-I${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++${CXXSTL}/include -I${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++${CXXSTL}/libs/${ANDROID_ABI}/include"
LDFLAGS="$LDFLAGS -L${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++${CXXSTL}/libs/${ANDROID_ABI}"

SYSROOT=$ANDROID_NDK/platforms/$ANDROID_API/arch-$PLATFORM_SHORT_ARCH

CPPFLAGS="$CPPFLAGS" \
CFLAGS="$CFLAGS ${RING_EXTRA_CFLAGS}" \
CXXFLAGS="$CXXFLAGS ${RING_EXTRA_CXXFLAGS}" \
LDFLAGS="$LDFLAGS ${RING_EXTRA_LDFLAGS}" \
CC="${CROSS_COMPILE}gcc --sysroot=${SYSROOT}" \
CXX="${CROSS_COMPILE}g++ --sysroot=${SYSROOT}" \
NM="${CROSS_COMPILE}nm" \
STRIP="${CROSS_COMPILE}strip" \
RANLIB="${CROSS_COMPILE}ranlib" \
AR="${CROSS_COMPILE}ar" \
AS="${CROSS_COMPILE}as" \
PKG_CONFIG_LIBDIR=$RING_SRC_DIR/contrib/$TARGET_TUPLE/lib/pkgconfig \
$RING_SRC_DIR/configure --host=$TARGET_TUPLE $EXTRA_PARAMS \
                   --disable-shared --with-opensl --without-dbus --without-alsa --without-pulse \
                   --prefix=$RING_SRC_DIR/install-android-$TARGET_TUPLE \
                   $*
