#!/bin/sh

if [ -z "$ANDROID_NDK" ]; then
    echo "Please set the ANDROID_NDK environment variable with its path."
    exit 1
fi

if [ -z "$ANDROID_ABI" ]; then
    echo "Please set ANDROID_ABI to your architecture: armeabi-v7a, armeabi, x86 or mips."
    exit 1
fi

# ANDROID_API must be previously set by compile.sh or env.sh
if [ -z "$ANDROID_API" ];then
    echo "ANDROID_API not set, call ./compile.sh first"
    exit 1
fi

SFLPHONE_SOURCEDIR=`cd ..; pwd`

CFLAGS="-g -O2 -fstrict-aliasing -funsafe-math-optimizations"
if [ -n "$HAVE_ARM" ]; then
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
ANDROID_BIN=`echo $ANDROID_NDK/toolchains/${PATH_HOST}-${GCCVER}/prebuilt/\`uname|tr A-Z a-z\`-*/bin/`
CROSS_COMPILE=${ANDROID_BIN}/${TARGET_TUPLE}-
# FIXME: this a temporary kludge to fix linking on Debian/Ubuntu, these are
# libaries we don't use directly and so shouldn't be adding them here
SFLPHONE_BROKEN_LIBS=" -L$SFLPHONE_SOURCEDIR/contrib/$TARGET_TUPLE/lib -lgcrypt -lFLAC -lvorbisenc -lgpg-error -lvorbis -logg"

CPPFLAGS="$CPPFLAGS" \
CFLAGS="$CFLAGS ${SFLPHONE_EXTRA_CFLAGS}" \
CXXFLAGS="$CXXFLAGS ${SFLPHONE_EXTRA_CXXFLAGS}" \
LDFLAGS="$LDFLAGS ${SFLPHONE_EXTRA_LDFLAGS}" \
LIBS="$LIBS ${SFLPHONE_BROKEN_LIBS}" \
CC="${CROSS_COMPILE}gcc --sysroot=${SYSROOT}" \
CXX="${CROSS_COMPILE}g++ --sysroot=${SYSROOT}" \
NM="${CROSS_COMPILE}nm" \
STRIP="${CROSS_COMPILE}strip" \
RANLIB="${CROSS_COMPILE}ranlib" \
AR="${CROSS_COMPILE}ar" \
PKG_CONFIG_LIBDIR=$SFLPHONE_SOURCEDIR/contrib/$TARGET_TUPLE/lib/pkgconfig \
sh $SFLPHONE_SOURCEDIR/configure --host=$TARGET_TUPLE $EXTRA_PARAMS \
                   --disable-video --with-opensl --without-zrtp --without-dbus --without-alsa --without-pulse --without-tls \
                   $*
