#! /bin/sh

# Read the Android Wiki http://wiki.videolan.org/AndroidCompile
# Setup all that stuff correctly.
# Get the latest Android SDK Platform or modify numbers in configure.sh and sflphone-android/default.properties.

set -e

BUILD=
FETCH=
case "$1" in
    --fetch)
    FETCH=1
    shift
    ;;
    --build)
    BUILD=1
    shift
    ;;
    *)
    FETCH=1
    BUILD=1
    ;;
esac

if [ -z "$ANDROID_NDK" -o -z "$ANDROID_SDK" ]; then
   echo "You must define ANDROID_NDK, ANDROID_SDK and ANDROID_ABI before starting."
   echo "They must point to your NDK and SDK directories.\n"
   exit 1
fi

if [ -z "$ANDROID_ABI" ]; then
   echo "Please set ANDROID_ABI to your architecture: armeabi-v7a, armeabi, x86 or mips."
   exit 1
fi

# try to detect NDK version
REL=$(grep -o '^r[0-9]*.*' $ANDROID_NDK/RELEASE.TXT 2>/dev/null|cut -b2-)
case "$REL" in
    9|10|*)
        GCCVER=4.8
        CXXSTL="/"${GCCVER}
    ;;
    7|8|*)
        echo "You need the NDKv9 or later"
        exit 1
    ;;
esac

export GCCVER
export CXXSTL

# Set up ABI variables
if [ ${ANDROID_ABI} = "x86" ] ; then
    TARGET_TUPLE="i686-linux-android"
    PATH_HOST="x86"
    HAVE_X86=1
    PLATFORM_SHORT_ARCH="x86"
elif [ ${ANDROID_ABI} = "mips" ] ; then
    TARGET_TUPLE="mipsel-linux-android"
    PATH_HOST=$TARGET_TUPLE
    HAVE_MIPS=1
    PLATFORM_SHORT_ARCH="mips"
else
    TARGET_TUPLE="arm-linux-androideabi"
    PATH_HOST=$TARGET_TUPLE
    HAVE_ARM=1
    PLATFORM_SHORT_ARCH="arm"
fi

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
export PLATFORM_SHORT_ARCH

# Add the NDK toolchain to the PATH, needed both for contribs and for building
# stub libraries
NDK_TOOLCHAIN_PATH=`echo ${ANDROID_NDK}/toolchains/${PATH_HOST}-${GCCVER}/prebuilt/\`uname|tr A-Z a-z\`-*/bin`
export PATH=${NDK_TOOLCHAIN_PATH}:${PATH}

ANDROID_PATH="`pwd`"

# Fetch Ring source
if [ ! -z "$FETCH" ]
then
    # 1/ libsflphone
    TESTED_HASH=f9b3354a49a29f10c466a4d856216c5d82525666
    if [ ! -d "sflphone" ]; then
        echo "Ring source not found, cloning"
        git clone git@gitlab.savoirfairelinux.com:sfl-ports/sflphone.git sflphone
	cd sflphone
        echo android/ >> .git/info/exclude
        echo contrib/android/ >> .git/info/exclude
        #git checkout -B android ${TESTED_HASH}
	    git checkout contrib
    else
        echo "Ring source found"
        cd sflphone
	    git checkout contrib
#        if ! git cat-file -e ${TESTED_HASH}; then
#            cat << EOF
#***
#*** Error: Your sflphone checkout does not contain the latest tested commit ***
#***
#
#Please update your source with something like:
#
#cd sflphone
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
    cd sflphone
fi

if [ -z "$BUILD" ]
then
    echo "Not building anything, please run $0 --build"
    exit 0
fi

# Setup CFLAGS
if [ ${ANDROID_ABI} = "armeabi-v7a" ] ; then
    EXTRA_CFLAGS="-mfpu=vfpv3-d16 -mcpu=cortex-a8"
    EXTRA_CFLAGS="${EXTRA_CFLAGS} -mthumb -mfloat-abi=softfp"
elif [ ${ANDROID_ABI} = "armeabi" ] ; then
    if [ -n "${NO_ARMV6}" ]; then
        EXTRA_CFLAGS="-march=armv5te -mtune=arm9tdmi -msoft-float"
    else
        if [ -n "${NO_FPU}" ]; then
            EXTRA_CFLAGS="-march=armv6j -mtune=arm1136j-s -msoft-float"
        else
            EXTRA_CFLAGS="-mfpu=vfp -mcpu=arm1136jf-s -mfloat-abi=softfp"
        fi
    fi
elif [ ${ANDROID_ABI} = "x86" ] ; then
    EXTRA_CFLAGS="-march=pentium"
elif [ ${ANDROID_ABI} = "mips" ] ; then
    EXTRA_CFLAGS="-march=mips32 -mtune=mips32r2 -mhard-float"
    # All MIPS Linux kernels since 2.4.4 will trap any unimplemented FPU
    # instruction and emulate it, so we select -mhard-float.
    # See http://www.linux-mips.org/wiki/Floating_point#The_Linux_kernel_and_floating_point
else
    echo "Unknown ABI. Die, die, die!"
    exit 2
fi

EXTRA_CFLAGS="${EXTRA_CFLAGS} -O2"

EXTRA_CFLAGS="${EXTRA_CFLAGS} -I${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++${CXXSTL}/include"
EXTRA_CFLAGS="${EXTRA_CFLAGS} -I${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++${CXXSTL}/libs/${ANDROID_ABI}/include"

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
#export PATH=`pwd`/extras/tools/build/bin:$PATH
#echo "Building tools"
#cd contrib
#mkdir native && cd native
#../bootstrap
#make $MAKEFLAGS
#cd ../..

############
# Contribs #
############
echo "Building the contribs"
mkdir -p contrib/contrib-android-${TARGET_TUPLE}

gen_pc_file() {
    echo "Generating $1 pkg-config file"
    echo "Name: $1
Description: $1
Version: $2
Libs: -l$1
Cflags:" > contrib/${TARGET_TUPLE}/lib/pkgconfig/`echo $1|tr 'A-Z' 'a-z'`.pc
}

mkdir -p contrib/${TARGET_TUPLE}/lib/pkgconfig

cd contrib/contrib-android-${TARGET_TUPLE}
../bootstrap --host=${TARGET_TUPLE}

# Some libraries have arm assembly which won't build in thumb mode
# We append -marm to the CFLAGS of these libs to disable thumb mode
[ ${ANDROID_ABI} = "armeabi-v7a" ] && echo "NOTHUMB := -marm" >> config.mak

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

echo "EXTRA_CFLAGS= -g ${EXTRA_CFLAGS}" >> config.mak
export SFLPHONE_EXTRA_CFLAGS="${EXTRA_CFLAGS}"

make install
echo ${PWD}
# We already have zlib available
[ -e .zlib ] || (mkdir -p zlib; touch .zlib)
which autopoint >/dev/null || make $MAKEFLAGS .gettext
export PATH="$PATH:$PWD/../$TARGET_TUPLE/bin"

export SFLPHONE_BUILD_DIR=sflphone/build-android-${TARGET_TUPLE}
############
# Make SFLPHONE #
############
cd ../.. && mkdir -p build-android-${TARGET_TUPLE} && cd build-android-${TARGET_TUPLE}

if [ $# -eq 1 ] && [ "$1" = "jni" ]; then
    CLEAN="jniclean"
    TARGET="sflphone-android/obj/local/armeabi-v7a/libsflphone.so"
else
    CLEAN="distclean"
    if [ ! -f config.h ]; then
        echo "Bootstraping"
        pushd ../../
        ./configure.sh
        cd sflphone/daemon
        echo "Building"
        make $MAKEFLAGS
        popd
    fi
    TARGET=
fi

####################################
# Ring android UI and specific code
####################################
echo "Building Ring for Android"
cd ../../

make $CLEAN
make -j1 TARGET_TUPLE=$TARGET_TUPLE PLATFORM_SHORT_ARCH=$PLATFORM_SHORT_ARCH CXXSTL=$CXXSTL RELEASE=$RELEASE $TARGET

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
export ANDROID_SYS_HEADERS_GINGERBREAD=$ANDROID_SYS_HEADERS_GINGERBREAD
export ANDROID_SYS_HEADERS_HC=$ANDROID_SYS_HEADERS_HC
export ANDROID_SYS_HEADERS_ICS=$ANDROID_SYS_HEADERS_ICS
export SFLPHONE_BUILD_DIR=$PWD/sflphone/android
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
