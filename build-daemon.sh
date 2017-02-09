#! /bin/bash
# Build Ring daemon for architecture specified by ANDROID_ABI

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
    ANDROID_API_VERS=21
    LIBDIR=lib64
else
    ANDROID_API_VERS=16
    LIBDIR=lib
fi
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

EXTRA_CFLAGS="${EXTRA_CFLAGS} -O2 -DHAVE_PTHREADS -I${ANDROID_TOOLCHAIN}/include/c++/4.9.x"

# Setup LDFLAGS
if [ ${ANDROID_ABI} = "armeabi-v7a-hard" ] ; then
    EXTRA_CFLAGS="${EXTRA_CFLAGS} -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16"
    EXTRA_LDFLAGS="-march=armv7-a -mfpu=vfpv3-d16 -mcpu=cortex-a8 -lm_hard -D_NDK_MATH_NO_SOFTFP=1"
elif [ ${ANDROID_ABI} = "armeabi-v7a" ] ; then
    EXTRA_CFLAGS="${EXTRA_CFLAGS} -march=armv7-a -mthumb -mfloat-abi=softfp -mfpu=vfpv3-d16"
    EXTRA_LDFLAGS="-march=armv7-a -mthumb -mfloat-abi=softfp -mfpu=vfpv3-d16"
elif [ ${ANDROID_ABI} = "arm64-v8a" ] ; then
    EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -L${ANDROID_TOOLCHAIN}/sysroot/usr/lib -L${ANDROID_TOOLCHAIN}/${TARGET_TUPLE}/lib"
fi
EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -L${ANDROID_TOOLCHAIN}/${TARGET_TUPLE}/${LIBDIR}/${ANDROID_ABI} -L${ANDROID_TOOLCHAIN}/${TARGET_TUPLE}/${LIBDIR} -lm -latomic -landroid_support"

EXTRA_CXXFLAGS="${EXTRA_CFLAGS}"
EXTRA_CFLAGS="-std=c11 ${EXTRA_CFLAGS}"

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
cd "$DAEMON_DIR"
export PATH=`pwd`/extras/tools/build/bin:$PATH
echo "Building tools"
pushd extras/tools
./bootstrap
make $MAKEFLAGS
#FIXME
echo "HACK for old Jenkins builder...forcing libtool to be built"
make .libtool
popd

############
# Contribs #
############
echo "Building the contribs"
mkdir -p contrib/native-${TARGET_TUPLE}

ANDROID_BIN=${NDK_TOOLCHAIN_PATH}
CROSS_COMPILE=${ANDROID_BIN}/${TARGET_TUPLE}-
export CROSS_COMPILE="${CROSS_COMPILE}"

mkdir -p contrib/${TARGET_TUPLE}/lib/pkgconfig

pushd contrib/native-${TARGET_TUPLE}
../bootstrap --host=${TARGET_TUPLE} --disable-libav --enable-ffmpeg --disable-speexdsp

# Some libraries have arm assembly which won't build in thumb mode
# We append -marm to the CFLAGS of these libs to disable thumb mode
[ ${ANDROID_ABI} = "armeabi-v7a" ] && echo "NOTHUMB := -marm" >> config.mak
[ ${ANDROID_ABI} = "armeabi-v7a-hard" ] && echo "NOTHUMB := -marm" >> config.mak

# Always strip symbols for libring.so remove it if you want to debug the daemon
STRIP_ARG="-s "

EXTRA_CFLAGS="${EXTRA_CFLAGS} -DNDEBUG "
if [ "${RELEASE}" -eq 1 ]; then
    echo "Contribs in release mode."
    OPTS=""
else
    echo "Contribs in debug mode."
    OPTS="--enable-debug"
fi

export SYSROOT=$ANDROID_TOOLCHAIN/sysroot
echo "EXTRA_CFLAGS= -g -fpic ${EXTRA_CFLAGS}" >> config.mak
echo "EXTRA_CXXFLAGS= -g -fpic ${EXTRA_CXXFLAGS}" >> config.mak
echo "EXTRA_LDFLAGS= ${EXTRA_LDFLAGS} -L${SYSROOT}/usr/${LIBDIR}" >> config.mak
export RING_EXTRA_CFLAGS="${EXTRA_CFLAGS}"
export RING_EXTRA_CXXFLAGS="${EXTRA_CXXFLAGS}"
export RING_EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -L${SYSROOT}/usr/${LIBDIR}"

make list
make fetch
export PATH="$PATH:$PWD/../$TARGET_TUPLE/bin"
make $MAKEFLAGS
popd

############
# Make Ring #
############
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

####################################
# Ring android UI and specific code
####################################
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
                -lupnp -lixml -lthreadutil \
                -lsamplerate \
                -lgnutls -lnettle -lhogweed -lgmp -liconv \
                -lcryptopp -lboost_random -lboost_system \
                -lrestbed \
                -lavformat -lavdevice -lavcodec -lavfilter -lavutil \
                -lpcre -lsndfile -lyaml-cpp -ljsoncpp \
                -luuid -lz -lswscale \
                -lvpx -lopus -lspeex -lvorbis -lvorbisenc -logg -lFLAC"
LIBRING_JNI_DIR=${ANDROID_APP_DIR}/app/src/main/libs/${ANDROID_ABI}

echo "Building Ring for Android to ${LIBRING_JNI_DIR}"

ARCH="${ANDROID_ABI}" DAEMON_DIR="${DAEMON_DIR}" make jniclean

mkdir -p ${LIBRING_JNI_DIR}
${NDK_TOOLCHAIN_PATH}/clang++ --shared -Wall -Wextra  ${ANDROID_APP_DIR}/libringclient/src/main/jni/ring_wrapper.cpp \
                                        ${RING_BUILD_DIR}/src/.libs/libring.a \
                                        -static-libstdc++ \
                                        -I${RING_SRC_DIR}/src -L${RING_SRC_DIR}/contrib/${TARGET_TUPLE}/lib \
                                        ${STRIP_ARG} --std=c++11 \
                                        ${STATIC_LIBS_ALL} \
                                        -o ${LIBRING_JNI_DIR}/libring.so
