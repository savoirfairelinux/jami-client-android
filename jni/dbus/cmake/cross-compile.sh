#!/bin/sh
#
#  cross compile script for cmake
# 
# initial written by Fridrich Strba
# refactored to debian/lenny by Ralf Habacker
#
#  reported to work at least on debian/lenny 
# 

if test -f /usr/bin/i686-pc-mingw32-gcc; then
    cross_cc=i686-pc-mingw32
elif test -f /usr/bin/i586-mingw32msvc-gcc; then
    cross_cc=i586-mingw32msvc
else
    echo "could not determine mingw cross compiler"
    exit 1
fi

if test -d ~/$cross_cc; then
    cross_root=~/$cross_cc
elif test -d /usr/$cross_cc/sys-root/mingw; then
    cross_root=/usr/$cross_cc/sys-root/mingw
elif test -d /usr/$cross_cc/lib; then
    cross_root=/usr/$cross_cc
else
    echo "could not determine mingw cross compiler sdk"
    exit 1
fi

# make cmake happy 
export TEMP=/tmp

HOST_CC=gcc; export HOST_CC;

if test -d $cross_root/lib/pkgconfig; then 
    PKG_CONFIG_PATH="$cross_root/lib/pkgconfig:$cross_root/share/pkgconfig"; export PKG_CONFIG_PATH;
fi 

if test -d "$MINGW32_CLASSPATH" ||  test -f "$cross_root/share/java/libgcj.jar";  then 
    CLASSPATH="$CLASSPATH:${MINGW32_CLASSPATH:-$cross_root/share/java/libgcj.jar:$cross_root/share/java/libgcj-tools.jar}"; export CLASSPATH;
fi

_PREFIX="/usr/bin/$cross_cc-";
for i in `ls -1 ${_PREFIX}* | grep -v 'gcc-'`; do
    x=`echo $i|sed "s,${_PREFIX},,"|sed "s,\.awk*,,"|tr "a-z+-" "A-ZX_"`;
    declare -x $x="$i" ; export $x;
done;
unset _PREFIX;

CC="${MINGW32_CC:-$cross_cc-gcc}"; export CC;
CFLAGS="${MINGW32_CFLAGS:--O2 -g -pipe -Wall -fexceptions -fno-omit-frame-pointer -fno-optimize-sibling-calls --param=ssp-buffer-size=4 -mms-bitfields}"; export CFLAGS;
LDFLAGS="${MINGW32_LDFLAGS:--Wl,--exclude-libs=libintl.a -Wl,--exclude-libs=libiconv.a}"; export LDFLAGS;

if [ -x "/usr/bin/$cross_cc-g++" ]; then
    CXX="${MINGW32_CXX:-$cross_cc-g++}"; export CXX;
    CXXFLAGS="${MINGW32_CXXFLAGS:--O2 -g -pipe -Wall -fexceptions -fno-omit-frame-pointer -fno-optimize-sibling-calls --param=ssp-buffer-size=4 -mms-bitfields}"; export CXXFLAGS;
else
    CXX=; export CXX;
    ac_cv_prog_CXX=no; export ac_cv_prog_CXX;
    CXXFLAGS=; export CXXFLAGS;
fi;
for i in `ls $cross_root/bin/*|grep -- "-config$"` ; do
    x=`basename $i|tr "a-z+-" "A-ZX_"|sed "s,\.,,"`;
    declare -x $x="$i" ; export $x;
done;
unset x i ;

if ! test -f "$cross_root/lib/libexpat.dll.a"; then
    (cd /tmp; wget http://www.winkde.org/pub/kde/ports/win32/repository/win32libs/expat-2.0.1-bin.zip)
    (cd /tmp; wget http://www.winkde.org/pub/kde/ports/win32/repository/win32libs/expat-2.0.1-lib.zip)
    (cd $cross_root; unzip -x /tmp/expat-2.0.1-bin.zip)
    (cd $cross_root; unzip -x /tmp/expat-2.0.1-lib.zip)
fi 

if test -f "$cross_root/lib/libexpat.dll.a"; then
    xml_library=-DDBUS_USE_EXPAT=On -DLIBEXPAT_INCLUDE_DIR:PATH=$cross_root/include -DLIBEXPAT_LIBRARIES:PATH=$cross_root/lib/libexpat.dll.a 
else
    echo "could not find a cross compile xml libraray"
    exit 1
fi

cmake \
    -DCMAKE_SYSTEM_NAME="Windows" \
    -DCMAKE_VERBOSE_MAKEFILE=ON \
    -DCMAKE_INSTALL_PREFIX:PATH=$cross_root \
    -DCMAKE_INSTALL_LIBDIR:PATH=$cross_root/lib \
    -DINCLUDE_INSTALL_DIR:PATH=$cross_root/include \
    -DLIB_INSTALL_DIR:PATH=$cross_root/lib \
    -DSYSCONF_INSTALL_DIR:PATH=$cross_root/etc \
    -DSHARE_INSTALL_PREFIX:PATH=$cross_root/share \
    -DBUILD_SHARED_LIBS:BOOL=ON \
    -DCMAKE_C_COMPILER="/usr/bin/$cross_cc-gcc" \
    -DCMAKE_CXX_COMPILER="/usr/bin/$cross_cc-g++" \
    -DCMAKE_FIND_ROOT_PATH="$cross_root" \
    -DCMAKE_FIND_ROOT_PATH_MODE_LIBRARY=ONLY \
    -DCMAKE_FIND_ROOT_PATH_MODE_INCLUDE=ONLY \
    -DCMAKE_CXX_COMPILER="/usr/bin/$cross_cc-g++" \
    -DCMAKE_FIND_ROOT_PATH="$cross_root" \
    -DCMAKE_FIND_ROOT_PATH_MODE_LIBRARY=ONLY \
    -DCMAKE_FIND_ROOT_PATH_MODE_INCLUDE=ONLY \
    $xml_library \
    -DCMAKE_FIND_ROOT_PATH_MODE_PROGRAM=NEVER \
    $*

