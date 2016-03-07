#! /bin/bash

# Read the Android Wiki http://wiki.videolan.org/AndroidCompile
# Setup all that stuff correctly.
# Get the latest Android SDK Platform or modify numbers in configure.sh and sflphone-android/default.properties.

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
   echo "Please set ANDROID_ABI to your architecture: armeabi-v7a, armeabi, arm64-v8a, x86, x86_64 or mips."
   exit 1
fi

if [ -z "$NO_FPU" ];then
    NO_FPU=0
fi
if [ -z "$NO_ARMV6" ];then
    NO_ARMV6=0
fi

BUILD=0
FETCH=0
RELEASE=0
JNI=0

for i in ${@}; do
    case "$i" in
        --fetch)
        FETCH=1
        ;;
        --build)
        BUILD=1
        ;;
        release|--release)
        RELEASE=1
        ;;
        jni|--jni)
        JNI=1
        ;;
        *)
        ;;
    esac
done

if [ "$BUILD" = 0 -a "$FETCH" = 0 ];then
    BUILD=1
    FETCH=1
fi

if [ `set -- ${ANDROID_ABI}; echo $#` -gt 1 ]; then
    ANDROID_ABI_LIST="${ANDROID_ABI}"
    echo "More than one ABI specified: ${ANDROID_ABI_LIST}"
    for i in ${ANDROID_ABI_LIST}; do
        echo "$i starts building"
        ANDROID_NDK=$ANDROID_NDK ANDROID_SDK=$ANDROID_SDK \
            NO_FPU=$NO_FPU NO_ARMV6=$NO_ARMV6 ANDROID_ABI=$i \
            ./compile.sh $* --jni || { echo "$i build KO"; exit 1; }
        mkdir -p obj/
        cp -r ring-android/app/src/main/libs/$i obj
        echo "$i build OK"
    done
    for i in ${ANDROID_ABI_LIST}; do
        cp -r obj/$i ring-android/libs/
        rm -rf obj/$i
    done
    make -b -j1 RELEASE=$RELEASE apk || exit 1
    exit 0
fi

HAVE_ARM=0
HAVE_X86=0
HAVE_MIPS=0
HAVE_64=0


# Set up ABI variables
if [ ${ANDROID_ABI} = "x86" ] ; then
    TARGET_TUPLE="i686-linux-android"
    PATH_HOST="x86"
    HAVE_X86=1
    PLATFORM_SHORT_ARCH="x86"
elif [ ${ANDROID_ABI} = "x86_64" ] ; then
    TARGET_TUPLE="x86_64-linux-android"
    PATH_HOST="x86_64"
    HAVE_X86=1
    HAVE_64=1
    PLATFORM_SHORT_ARCH="x86_64"
elif [ ${ANDROID_ABI} = "mips" ] ; then
    TARGET_TUPLE="mipsel-linux-android"
    PATH_HOST=$TARGET_TUPLE
    HAVE_MIPS=1
    PLATFORM_SHORT_ARCH="mips"
elif [ ${ANDROID_ABI} = "arm64-v8a" ] ; then
    TARGET_TUPLE="aarch64-linux-android"
    PATH_HOST=$TARGET_TUPLE
    HAVE_ARM=1
    HAVE_64=1
    PLATFORM_SHORT_ARCH="arm64"
else
    TARGET_TUPLE="arm-linux-androideabi"
    PATH_HOST=$TARGET_TUPLE
    HAVE_ARM=1
    PLATFORM_SHORT_ARCH="arm"
fi

# try to detect NDK version
GCCVER=4.9
REL=$(grep -o '^r[0-9]*.*' $ANDROID_NDK/RELEASE.TXT 2>/dev/null|cut -b2-)
case "$REL" in
    10*)
        if [ "${HAVE_64}" = 1 ];then
            ANDROID_API=android-21
        else
            ANDROID_API=android-16
        fi
        CXXSTL="/"${GCCVER}
    ;;
    7|8|9|*)
        echo "You need the NDKv10 or later"
        exit 1
    ;;
esac

export GCCVER
export CXXSTL
export ANDROID_API

# XXX : important!
[ "$HAVE_ARM" = 1 ] && cat << EOF
For an ARMv6 device without FPU:
$ export NO_FPU=1
For an ARMv5 device:
$ export NO_ARMV6=1

If you plan to use a release build, run 'compile.sh release'
EOF

export TARGET_TUPLE
export PATH_HOST
export HAVE_ARM
export HAVE_X86
export HAVE_MIPS
export HAVE_64
export PLATFORM_SHORT_ARCH

# Add the NDK toolchain to the PATH, needed both for contribs and for building
# stub libraries
NDK_TOOLCHAIN_PATH=`echo ${ANDROID_NDK}/toolchains/${PATH_HOST}-${GCCVER}/prebuilt/\`uname|tr A-Z a-z\`-*/bin`
export NDK_TOOLCHAIN_PATH=${NDK_TOOLCHAIN_PATH}
export PATH=${NDK_TOOLCHAIN_PATH}:${PATH}

ANDROID_PATH="`pwd`"

# Fetch ring daemon source
if [ "$FETCH" = 1 ]
then
    # 1/ dring
    TESTED_HASH=c4ee1b4e7b637c2034c31071607f26cdf83e95ce
    if [ ! -d "ring-daemon" ]; then
        echo "ring daemon source not found, cloning"
        git clone https://gerrit-ring.savoirfairelinux.com/ring-daemon.git
        pushd ring-daemon
        echo android/ >> .git/info/exclude
        echo contrib/android/ >> .git/info/exclude
	    git checkout $TESTED_HASH
    else
        echo "ring daemon source found"
        pushd ring-daemon
	    git fetch
        #git checkout ${TESTED_HASH}
#        if ! git cat-file -e ${TESTED_HASH}; then
#            cat << EOF
#***
#*** Error: Your ring checkout does not contain the latest tested commit ***
#***
#
#Please update your source with something like:
#
#cd ring
#git reset --hard origin
#git pull origin master
#git checkout -B android ${TESTED_HASH}
#
#*** : This will delete any changes you made to the current branch ***
#
#EOF
#           exit 1
#        fi
    fi
else
    pushd ring
fi

if [ -z "$BUILD" ]
then
    echo "Not building anything, please run $0 --build"
    exit 0
fi

# Setup CFLAGS
if [ ${ANDROID_ABI} = "armeabi-v7a-hard" ] ; then
    EXTRA_CFLAGS="-march=armv7-a -mfpu=vfpv3-d16 -mcpu=cortex-a8 -D_NDK_MATH_NO_SOFTFP=1"
elif [ ${ANDROID_ABI} = "armeabi-v7a" ] ; then
    EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16 -mthumb"
    #EXTRA_CFLAGS="-march=armv7-a -mfpu=vfpv3-d16 -mthumb -mcpu=cortex-a8"
elif [ ${ANDROID_ABI} = "armeabi" ] ; then
    if [ -n "${NO_ARMV6}" ]; then
        EXTRA_CFLAGS="-march=armv5te -mtune=arm9tdmi -msoft-float "
    else
        if [ -n "${NO_FPU}" ]; then
            EXTRA_CFLAGS="-march=armv6j -mtune=arm1136j-s -msoft-float"
        else
            EXTRA_CFLAGS="-mfpu=vfp -mcpu=arm1136jf-s -mfloat-abi=softfp"
        fi
    fi
elif [ ${ANDROID_ABI} = "arm64-v8a" ] ; then
    EXTRA_CFLAGS=""
elif [ ${ANDROID_ABI} = "x86" ] ; then
    EXTRA_CFLAGS="-march=pentium -m32"
elif [ ${ANDROID_ABI} = "x86_64" ] ; then
    EXTRA_CFLAGS=""
elif [ ${ANDROID_ABI} = "mips" ] ; then
    EXTRA_CFLAGS="-march=mips32 -mtune=mips32r2 -mhard-float"
    # All MIPS Linux kernels since 2.4.4 will trap any unimplemented FPU
    # instruction and emulate it, so we select -mhard-float.
    # See http://www.linux-mips.org/wiki/Floating_point#The_Linux_kernel_and_floating_point
else
    echo "Unknown ABI ${ANDROID_ABI}. Die, die, die!"
    exit 2
fi

EXTRA_CFLAGS="${EXTRA_CFLAGS} -O2 -DHAVE_PTHREADS"
EXTRA_CFLAGS="${EXTRA_CFLAGS} -I${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++${CXXSTL}/include"
EXTRA_CFLAGS="${EXTRA_CFLAGS} -I${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++${CXXSTL}/libs/${ANDROID_ABI}/include"
EXTRA_CXXFLAGS="${EXTRA_CFLAGS}"
EXTRA_CFLAGS="-std=gnu11 ${EXTRA_CFLAGS}"

# Setup LDFLAGS
if [ ${ANDROID_ABI} = "armeabi-v7a-hard" ] ; then
    EXTRA_LDFLAGS="-march=armv7-a -mfpu=vfpv3-d16 -mcpu=cortex-a8 -lm_hard -D_NDK_MATH_NO_SOFTFP=1"
elif [ ${ANDROID_ABI} = "armeabi-v7a" ] ; then
    EXTRA_LDFLAGS="-march=armv7-a -mthumb"
fi
EXTRA_LDFLAGS="${EXTRA_LDFLAGS} -L${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++${CXXSTL}/libs/${ANDROID_ABI} -lgnustl_static"

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
mkdir -p contrib/native

gen_pc_file() {
    echo "Generating $1 pkg-config file"
    echo "Name: $1
Description: $1
Version: $2
Libs: -l$1
Cflags:" > contrib/${TARGET_TUPLE}/lib/pkgconfig/`echo $1|tr 'A-Z' 'a-z'`.pc
}


ANDROID_BIN=${NDK_TOOLCHAIN_PATH}
CROSS_COMPILE=${ANDROID_BIN}/${TARGET_TUPLE}-
export CROSS_COMPILE="${CROSS_COMPILE}"


mkdir -p contrib/${TARGET_TUPLE}/lib/pkgconfig

pushd contrib/native
../bootstrap --host=${TARGET_TUPLE}

# Some libraries have arm assembly which won't build in thumb mode
# We append -marm to the CFLAGS of these libs to disable thumb mode
[ ${ANDROID_ABI} = "armeabi-v7a" ] && echo "NOTHUMB := -marm" >> config.mak
[ ${ANDROID_ABI} = "armeabi-v7a-hard" ] && echo "NOTHUMB := -marm" >> config.mak

# Release or not?
if [ $# -ne 0 ] && [ "$1" = "release" ]; then
    OPTS=""
    EXTRA_CFLAGS="${EXTRA_CFLAGS} -DNDEBUG "
    RELEASE=1
else
    OPTS="--enable-debug"
    EXTRA_CFLAGS="${EXTRA_CFLAGS} -DNDEBUG "
    RELEASE=0
fi

echo "EXTRA_CFLAGS= -g -fpic ${EXTRA_CFLAGS}" >> config.mak
echo "EXTRA_CXXFLAGS= -g -fpic ${EXTRA_CXXFLAGS}" >> config.mak
echo "EXTRA_LDFLAGS= ${EXTRA_LDFLAGS}" >> config.mak
export RING_EXTRA_CFLAGS="${EXTRA_CFLAGS}"
export RING_EXTRA_CXXFLAGS="${EXTRA_CXXFLAGS}"
export RING_EXTRA_LDFLAGS="${EXTRA_LDFLAGS}"
#export LOCAL_EXTRA_LDFLAGS="${EXTRA_LDFLAGS}"

make list
make install
echo ${PWD}
# We already have zlib available
[ -e .zlib ] || (mkdir -p zlib; touch .zlib)
which autopoint >/dev/null || make $MAKEFLAGS .gettext
export PATH="$PATH:$PWD/../$TARGET_TUPLE/bin"
popd

############
# Make Ring #
############
RING_SRC_DIR="${PWD}"
RING_BUILD_DIR="`realpath build-android-${TARGET_TUPLE}`"
#RING_INSTALL_DIR="`realpath install-android-${TARGET_TUPLE}`"
export RING_SRC_DIR="${RING_SRC_DIR}"
export RING_BUILD_DIR="${RING_BUILD_DIR}"

#mkdir -p ${RING_INSTALL_DIR}
mkdir -p build-android-${TARGET_TUPLE} && pushd build-android-${TARGET_TUPLE}
DRING_PATH="`pwd`"

if [ "$JNI" = 1 ]; then
    CLEAN="jniclean"
    TARGET="ring-android/app/src/main/obj/local/${ANDROID_ABI}/libring.so"
else
    CLEAN="distclean"
    TARGET=
fi

if [ ! -f config.h ]; then
    echo "Bootstraping"
    cd ../
    ./autogen.sh
    cd ../
    cd ring-android
    ./make-swig.sh
    cd ../ring-daemon/build-android-${TARGET_TUPLE}
    echo "Configuring"
    ${ANDROID_PATH}/configure.sh ${OPTS}
fi

# ANDROID NDK FIXUP (BLAME GOOGLE)
config_undef ()
{
    previous_change=`stat -c "%y" config.h`
    sed -i 's,#define '$1' 1,/\* #undef '$1' \*/,' config.h
    # don't change modified date in order to don't trigger a full build
    touch -d "$previous_change" config.h
}

# if config dependencies change, ./config.status --recheck
# is run and overwrite previously hacked config.h. So call make Makefile here
# and hack config.h after.

make $MAKEFLAGS Makefile

if [ ${ANDROID_ABI} = "x86" -a ${ANDROID_API} != "android-21" ] ; then
    # NDK x86 libm.so has nanf symbol but no nanf definition, we don't known if
    # intel devices has nanf. Assume they don't have it.
    config_undef HAVE_NANF
fi
if [ ${ANDROID_API} = "android-21" ] ; then
    # android-21 has empty sys/shm.h headers that triggers shm detection but it
    # doesn't have any shm functions and/or symbols. */
    config_undef HAVE_SYS_SHM_H
fi
# END OF ANDROID NDK FIXUP

echo "Building dring ${MAKEFLAGS}"
V=99 make $MAKEFLAGS

####################################
# Ring android UI and specific code
####################################
cd ../..

echo "Building Ring for Android ${PWD}" 
make $CLEAN
ANDROID_ABI="${ANDROID_ABI}" make -j1 TARGET_TUPLE=$TARGET_TUPLE PLATFORM_SHORT_ARCH=$PLATFORM_SHORT_ARCH CXXSTL=$CXXSTL RELEASE=$RELEASE $TARGET

#
# Exporting a environment script with all the necessary variables
#
echo "Generating environment script."
cat <<EOF
This is a script that will export many of the variables used in this
script. It will allow you to compile parts of the build without having
to rebuild the entire build (e.g. recompile only the Java part).

To use it, include the script into your shell, like this:
    source env.sh

Now, you can use this command to build the Java portion:
    make -e

The file will be automatically regenerated by compile.sh, so if you change
your NDK/SDK locations or any build configurations, just re-run this
script (sh compile.sh) and it will automatically update the file.

EOF

echo "# This file was automatically generated by compile.sh" > env.sh
echo "# Re-run 'sh compile.sh' to update this file." >> env.sh

# The essentials
cat <<EssentialsA >> env.sh
export ANDROID_ABI=$ANDROID_ABI
export ANDROID_SDK=$ANDROID_SDK
export ANDROID_NDK=$ANDROID_NDK
export GCCVER=$GCCVER
export CXXSTL=$CXXSTL
export RING_BUILD_DIR=$RING_BUILD_DIR
export TARGET_TUPLE=$TARGET_TUPLE
export PATH_HOST=$PATH_HOST
export PLATFORM_SHORT_ARCH=$PLATFORM_SHORT_ARCH
EssentialsA

# PATH
echo "export PATH=$NDK_TOOLCHAIN_PATH:\${ANDROID_SDK}/platform-tools:\${PATH}" >> env.sh

# CPU flags
if [ -n "${HAVE_ARM}" ]; then
    echo "export HAVE_ARM=1" >> env.sh
elif [ -n "${HAVE_X86}" ]; then
    echo "export HAVE_X86=1" >> env.sh
elif [ -n "${HAVE_MIPS}" ]; then
    echo "export HAVE_MIPS=1" >> env.sh
fi

if [ -n "${NO_ARMV6}" ]; then
    echo "export NO_ARMV6=1" >> env.sh
fi
if [ -n "${NO_FPU}" ]; then
    echo "export NO_FPU=1" >> env.sh
fi
