#! /bin/bash
# Build Jami daemon for architecture specified by ANDROID_ABI
set -e

if [ -z "$ANDROID_NDK" -o -z "$ANDROID_SDK" ]; then
   echo "You must define ANDROID_NDK, ANDROID_SDK and ANDROID_ABI before starting."
   echo "They must point to your NDK and SDK directories."
   exit 1
fi
if [ -z "$ANDROID_ABI" ]; then
   echo "Please set ANDROID_ABI to your architecture: armeabi-v7a, x86."
   exit 1
fi

ANDROID_TOPLEVEL_DIR="`pwd`"
ANDROID_APP_DIR="${ANDROID_TOPLEVEL_DIR}/ring-android"

HAVE_ARM=0
HAVE_X86=0
HAVE_64=0

# Set up ABI variables
if [ ${ANDROID_ABI} = "x86" ] ; then
    TARGET_TUPLE="i686-linux-android"
    PJ_TARGET_TUPLE="i686-pc-linux-android"
    PATH_HOST="x86"
    HAVE_X86=1
    PLATFORM_SHORT_ARCH="x86"
elif [ ${ANDROID_ABI} = "x86_64" ] ; then
    TARGET_TUPLE="x86_64-linux-android"
    PJ_TARGET_TUPLE="x86_64-pc-linux-android"
    PATH_HOST="x86_64"
    HAVE_X86=1
    HAVE_64=1
    PLATFORM_SHORT_ARCH="x86_64"
elif [ ${ANDROID_ABI} = "arm64-v8a" ] ; then
    TARGET_TUPLE="aarch64-linux-android"
    PJ_TARGET_TUPLE="aarch64-unknown-linux-android"
    PATH_HOST=$TARGET_TUPLE
    HAVE_ARM=1
    HAVE_64=1
    PLATFORM_SHORT_ARCH="arm64"
else
    TARGET_TUPLE="arm-linux-androideabi"
    PJ_TARGET_TUPLE="arm-unknown-linux-androideabi"
    PATH_HOST=$TARGET_TUPLE
    HAVE_ARM=1
    PLATFORM_SHORT_ARCH="arm"
fi

if [ "${HAVE_64}" = 1 ];then
    LIBDIR=lib64
else
    LIBDIR=lib
fi
ANDROID_API_VERS=21
ANDROID_API=android-$ANDROID_API_VERS

export TOOLCHAIN=$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64
export TARGET=${TARGET_TUPLE}
export API=$ANDROID_API_VERS
export AR=$TOOLCHAIN/bin/$TARGET-ar
export AS=$TOOLCHAIN/bin/$TARGET-as
export CC=$TOOLCHAIN/bin/$TARGET$API-clang
export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
export LD=$TOOLCHAIN/bin/$TARGET-ld
export RANLIB=$TOOLCHAIN/bin/$TARGET-ranlib
export STRIP=$TOOLCHAIN/bin/$TARGET-strip

export ANDROID_API
export TARGET_TUPLE
export HAVE_ARM
export HAVE_X86
export HAVE_64

# Add the NDK toolchain to the PATH, needed both for contribs and for building
# stub libraries
NDK_TOOLCHAIN_PATH="${ANDROID_TOOLCHAIN}/bin"

if [ -z "$DAEMON_DIR" ]; then
    DAEMON_DIR="$(pwd)/../daemon"
    echo "DAEMON_DIR not provided trying to find it in $DAEMON_DIR"
fi

if [ ! -d "$DAEMON_DIR" ]; then
    echo 'Daemon not found.'
    echo 'If you cloned the daemon in a custom location override' \
            'DAEMON_DIR to point to it'
    echo "You can also use our meta repo which contains both:
          https://review.jami.net/#/admin/projects/ring-project"
    exit 1
fi
export DAEMON_DIR

# Setup LDFLAGS
if [ ${ANDROID_ABI} = "armeabi-v7a" ] ; then
    EXTRA_CFLAGS="${EXTRA_CFLAGS} -march=armv7-a -mthumb -mfpu=vfpv3-d16"
    EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -march=armv7-a -mthumb -mfpu=vfpv3-d16"
elif [ ${ANDROID_ABI} = "arm64-v8a" ] ; then
    EXTRA_LDFLAGS="${EXTRA_LDFLAGS}"
fi
EXTRA_CFLAGS="${EXTRA_CFLAGS} -g -fPIC"
EXTRA_CXXFLAGS="${EXTRA_CXXFLAGS} -g -fPIC"
EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -L${SYSROOT}/usr/${LIBDIR} -g"
:> config.mak
echo "EXTRA_CFLAGS=${EXTRA_CFLAGS}" >> config.mak
echo "EXTRA_CXXFLAGS=${EXTRA_CXXFLAGS}" >> config.mak
echo "EXTRA_LDFLAGS=${EXTRA_LDFLAGS}" >> config.mak

if [ "${RELEASE}" -eq 1 ]; then
    echo "Daemon in release mode."
    OPTS=""
else
    echo "Daemon in debug mode."
    OPTS="--enable-debug"
fi

# Make in //
UNAMES=$(uname -s)
MAKEFLAGS=
if which nproc >/dev/null
then
MAKEFLAGS=-j`nproc`
elif [ "$UNAMES" == "Darwin" ] && which sysctl >/dev/null
then
MAKEFLAGS=-j`sysctl -n machdep.cpu.thread_count`
fi

# Build buildsystem tools
cd $DAEMON_DIR/extras/tools
export PATH=`pwd`/build/bin:$PATH
echo "Building tools"
./bootstrap
make $MAKEFLAGS
make .pkg-config
make .gas

JNIDIR=$DAEMON_DIR/bin/jni
PACKAGEDIR=$ANDROID_APP_DIR/libringclient/src/main/java/cx/ring/daemon

#Build JNI interface
cd $JNIDIR
PACKAGEDIR=$PACKAGEDIR $JNIDIR/make-swig.sh

############
# Contribs #
############
echo "Building the contribs"
CONTRIB_DIR=${DAEMON_DIR}/contrib/native-${TARGET_TUPLE}
CONTRIB_SYSROOT=${DAEMON_DIR}/contrib/${TARGET_TUPLE}
mkdir -p ${CONTRIB_DIR}
mkdir -p ${CONTRIB_SYSROOT}/lib/pkgconfig

cd ${CONTRIB_DIR}
../bootstrap --host=${TARGET_TUPLE} --enable-ffmpeg

make list
make fetch
export PATH="$PATH:$CONTRIB_SYSROOT/bin"
make $MAKEFLAGS

############
# Make Jami daemon #
############
DAEMON_BUILD_DIR="${DAEMON_DIR}/build-android-${TARGET_TUPLE}"
mkdir -p ${DAEMON_BUILD_DIR}

cd ${DAEMON_DIR}
if [ ! -f configure ]; then
    ./autogen.sh
fi

cd "${DAEMON_BUILD_DIR}"
if [ ! -f config.h ]; then
    echo "Configuring with ${OPTS}"
    CFLAGS="${EXTRA_CFLAGS}" \
    CXXFLAGS="${EXTRA_CXXFLAGS}" \
    LDFLAGS="${EXTRA_LDFLAGS}" \
    CPPFLAGS="${CPPFLAGS} -I${DAEMON_DIR}/contrib/${TARGET_TUPLE}/include " \
    LDFLAGS="${LDFLAGS} -L${DAEMON_DIR}/contrib/${TARGET_TUPLE}/lib " \
    PKG_CONFIG_LIBDIR=$DAEMON_DIR/contrib/$TARGET_TUPLE/lib/pkgconfig \
    ${DAEMON_DIR}/configure --host=$TARGET_TUPLE $EXTRA_PARAMS \
                   --disable-shared --with-opensl --without-dbus --without-alsa --without-pulse --enable-accel\
                   --prefix=$DAEMON_DIR/install-android-$TARGET_TUPLE \
                   ${OPTS}
fi

if [ ${ANDROID_API} = "android-21" ] ; then
    # android-21 has empty sys/shm.h headers that triggers shm detection but it
    # doesn't have any shm functions and/or symbols. */
    export ac_cv_header_sys_shm_h=no
fi

echo "Building dring ${MAKEFLAGS}"
V=99 make $MAKEFLAGS

######################
# Building JNI library
######################
cd ${ANDROID_TOPLEVEL_DIR}

STATIC_LIBS_ALL="-llog -lOpenSLES -landroid \
                -lopendht \
                -lpjsip-${PJ_TARGET_TUPLE} \
                -lpjsip-simple-${PJ_TARGET_TUPLE} \
                -lpjsip-ua-${PJ_TARGET_TUPLE} -lpjsua-${PJ_TARGET_TUPLE} \
                -lpjnath-${PJ_TARGET_TUPLE} \
                -lpjmedia-${PJ_TARGET_TUPLE} \
                -lpjlib-util-${PJ_TARGET_TUPLE} \
                -lpj-${PJ_TARGET_TUPLE} \
                -lupnp -lixml \
                -lgit2 \
                -larchive \
                -lsecp256k1 \
                -lgnutls -lhogweed -lnettle -lgmp \
                -lssl -lcrypto \
                -lavformat -lavdevice -lavfilter -lavcodec -lswresample -lswscale -lavutil \
                -lyaml-cpp -ljsoncpp -lhttp_parser -lfmt\
                -luuid -lz -ldl \
                -lvpx -lopus -lspeex -lspeexdsp -lx264 \
                -largon2 \
                -liconv"

LIBRING_JNI_DIR=${ANDROID_APP_DIR}/app/src/main/libs/${ANDROID_ABI}

echo "Building Jami JNI library for Android to ${LIBRING_JNI_DIR}"
mkdir -p ${LIBRING_JNI_DIR}

# Use a shared stl
cp $ANDROID_NDK/sources/cxx-stl/llvm-libc++/libs/${ANDROID_ABI}/libc++_shared.so $LIBRING_JNI_DIR

# Use a shared libc++_shared.so (shared by jami and all other plugins)
${CXX} --shared \
       -Wall -Wextra \
       -Wno-unused-variable \
       -Wno-unused-function \
       -Wno-unused-parameter \
       ${JNIDIR}/ring_wrapper.cpp \
       ${DAEMON_BUILD_DIR}/src/.libs/libring.a \
       -isystem ${DAEMON_DIR}/contrib/${TARGET_TUPLE}/include \
       -I${DAEMON_DIR}/src \
       -L${DAEMON_DIR}/contrib/${TARGET_TUPLE}/lib \
       ${STATIC_LIBS_ALL} \
       ${EXTRA_CXXFLAGS} -O3 --std=c++17 \
       -o ${LIBRING_JNI_DIR}/libring.so
