#! /bin/bash
# Build Ring daemon for architecture specified by ANDROID_ABI

# Tensorflow lite root library path
# There should be different builds according to the different supported platforms
# E.g: TENSORFLOW_LITE_ROOT_LIB_DIR/arm64-v8a/ for armv8 architecture
TENSORFLOW_LITE_ROOT_LIB_DIR="/home/ayounes/Libs/_tensorflow_dist_/lib"

#for OSX/BSD
realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

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
ANDROID_APP_DIR="$(pwd)/ring-android"

HAVE_ARM=0
HAVE_X86=0
HAVE_MIPS=0
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
elif [ ${ANDROID_ABI} = "mips" ] ; then
    TARGET_TUPLE="mipsel-linux-android"
    PJ_TARGET_TUPLE="mips-unknown-linux-androideabi"
    PATH_HOST=$TARGET_TUPLE
    HAVE_MIPS=1
    PLATFORM_SHORT_ARCH="mips"
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

export ANDROID_TOOLCHAIN="`pwd`/android-toolchain-$ANDROID_API_VERS-$PLATFORM_SHORT_ARCH"
if [ ! -d "$ANDROID_TOOLCHAIN" ]; then
    $ANDROID_NDK/build/tools/make_standalone_toolchain.py \
        --arch=$PLATFORM_SHORT_ARCH \
        --api $ANDROID_API_VERS \
        --stl libc++ \
        --install-dir=$ANDROID_TOOLCHAIN
fi

GCCVER=clang
CXXSTL="/"${GCCVER}

export GCCVER
export CXXSTL
export ANDROID_API
export TARGET_TUPLE
export PATH_HOST
export HAVE_ARM
export HAVE_X86
export HAVE_MIPS
export HAVE_64
export PLATFORM_SHORT_ARCH

# Add the NDK toolchain to the PATH, needed both for contribs and for building
# stub libraries
NDK_TOOLCHAIN_PATH=`echo ${ANDROID_TOOLCHAIN}/bin`
export NDK_TOOLCHAIN_PATH=${NDK_TOOLCHAIN_PATH}
export PATH=${NDK_TOOLCHAIN_PATH}:${PATH}

if [ -z "$DAEMON_DIR" ]; then
    DAEMON_DIR="$(pwd)/../daemon"
    echo "DAEMON_DIR not provided trying to find it in $DAEMON_DIR"
fi

if [ ! -d "$DAEMON_DIR" ]; then
    echo 'Daemon not found.'
    echo 'If you cloned the daemon in a custom location override' \
            'DAEMON_DIR to point to it'
    echo "You can also use our meta repo which contains both:
          https://gerrit-ring.savoirfairelinux.com/#/admin/projects/ring-project"
    exit 1
fi

# Setup LDFLAGS
if [ ${ANDROID_ABI} = "armeabi-v7a" ] ; then
    EXTRA_CFLAGS="${EXTRA_CFLAGS} -march=armv7-a -mthumb -mfpu=vfpv3-d16"
    EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -march=armv7-a -mthumb -mfpu=vfpv3-d16"
elif [ ${ANDROID_ABI} = "arm64-v8a" ] ; then
    EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -L${ANDROID_TOOLCHAIN}/sysroot/usr/lib -L${ANDROID_TOOLCHAIN}/${TARGET_TUPLE}/lib"
fi
EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -L${ANDROID_TOOLCHAIN}/${TARGET_TUPLE}/${LIBDIR}/${ANDROID_ABI} -L${ANDROID_TOOLCHAIN}/${TARGET_TUPLE}/${LIBDIR}"

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
export PATH=`pwd`/extras/tools/build/bin:$PATH
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
cd $DAEMON_DIR
echo "Building the contribs"
mkdir -p contrib/native-${TARGET_TUPLE}

CROSS_COMPILE=${NDK_TOOLCHAIN_PATH}/${TARGET_TUPLE}-
export CROSS_COMPILE="${CROSS_COMPILE}"

mkdir -p contrib/${TARGET_TUPLE}/lib/pkgconfig

cd $DAEMON_DIR/contrib/native-${TARGET_TUPLE}
../bootstrap --host=${TARGET_TUPLE} --disable-libav --enable-ffmpeg --disable-speexdsp

# Always strip symbols for libring.so remove it if you want to debug the daemon
STRIP_ARG="-s "

EXTRA_CFLAGS="${EXTRA_CFLAGS} -DNDEBUG -fPIC -fno-integrated-as"
EXTRA_CXXFLAGS="${EXTRA_CXXFLAGS} -DNDEBUG -fPIC"
EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -L${SYSROOT}/usr/${LIBDIR}"

if [ "${RELEASE}" -eq 1 ]; then
    echo "Daemon in release mode."
    OPTS=""
else
    echo "Daemon in debug mode."
    OPTS="--enable-debug"
fi

export SYSROOT=$ANDROID_TOOLCHAIN/sysroot
echo "EXTRA_CFLAGS= ${EXTRA_CFLAGS}" >> config.mak
echo "EXTRA_CXXFLAGS= ${EXTRA_CXXFLAGS}" >> config.mak
echo "EXTRA_LDFLAGS= ${EXTRA_LDFLAGS}" >> config.mak
export RING_EXTRA_CFLAGS="${EXTRA_CFLAGS}"
export RING_EXTRA_CXXFLAGS="${EXTRA_CXXFLAGS}"
export RING_EXTRA_LDFLAGS="${EXTRA_LDFLAGS}"

make list
make fetch
export PATH="$PATH:$PWD/../$TARGET_TUPLE/bin"
make $MAKEFLAGS

############
# Make Ring #
############
cd $DAEMON_DIR
RING_SRC_DIR="${DAEMON_DIR}"
RING_BUILD_DIR="`realpath build-android-${TARGET_TUPLE}`"
export RING_SRC_DIR="${RING_SRC_DIR}"
export RING_BUILD_DIR="${RING_BUILD_DIR}"

mkdir -p build-android-${TARGET_TUPLE}
cd ${ANDROID_APP_DIR}

if [ ! -f config.h ]; then
    echo "Bootstraping"
    cd ${DAEMON_DIR}
    ./autogen.sh
    cd "${DAEMON_DIR}/build-android-${TARGET_TUPLE}"
    echo "Configuring with ${OPTS}"
    CFLAGS="${EXTRA_CFLAGS}" \
    CXXFLAGS="${EXTRA_CXXFLAGS}" \
    LDFLAGS="${EXTRA_LDFLAGS}" \
    ${ANDROID_TOPLEVEL_DIR}/configure.sh ${OPTS}
fi

if [ ${ANDROID_API} = "android-21" ] ; then
    # android-21 has empty sys/shm.h headers that triggers shm detection but it
    # doesn't have any shm functions and/or symbols. */
    export ac_cv_header_sys_shm_h=no
fi
if [ ${ANDROID_ABI} = "x86" -a ${ANDROID_API} != "android-21" ] ; then
    # NDK x86 libm.so has nanf symbol but no nanf definition, we don't known if
    # intel devices has nanf. Assume they don't have it.
    export ac_cv_lib_m_nanf=no
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
                -lsecp256k1 \
                -lgnutls -lhogweed -lnettle -lgmp \
                -lrestbed -lssl -lcrypto \
                -lavformat -lavdevice -lavfilter -lavcodec -lswresample -lswscale -lavutil \
                -lyaml-cpp -ljsoncpp \
                -luuid -lz \
                -lvpx -lopus -lspeex -lx264 \
                -largon2 \
                -liconv \
                -lopencv_core -lopencv_imgproc -lopencv_highgui -lcpufeatures"


LIBRING_JNI_DIR=${ANDROID_APP_DIR}/app/src/main/libs/${ANDROID_ABI}

echo "Building Ring JNI library for Android to ${LIBRING_JNI_DIR}"
mkdir -p ${LIBRING_JNI_DIR}

${NDK_TOOLCHAIN_PATH}/clang++ \
                --shared \
                -Wall -Wextra \
                -Wno-unused-variable \
                -Wno-unused-function \
                -Wno-unused-parameter \
                ${JNIDIR}/ring_wrapper.cpp \
                ${RING_BUILD_DIR}/src/.libs/libring.a \
                -static-libstdc++ \
                -isystem ${RING_SRC_DIR}/contrib/${TARGET_TUPLE}/include \
                -I${RING_SRC_DIR}/src \
                -L${RING_SRC_DIR}/contrib/${TARGET_TUPLE}/lib \
                ${STATIC_LIBS_ALL} \
                ${STRIP_ARG} --std=c++14 -O3 -fPIC \
                -L${TENSORFLOW_LITE_ROOT_LIB_DIR}/${ANDROID_ABI} \
                -ltensorflowlite \
                -o ${LIBRING_JNI_DIR}/libring.so

# Tensorflow
cp $HOME/Libs/_tensorflow_dist_/lib/${ANDROID_ABI}/libtensorflowlite.so $LIBRING_JNI_DIR