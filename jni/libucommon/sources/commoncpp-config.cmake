#!/bin/sh
# Copyright (C) 2006-2007 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

prefix=${CMAKE_INSTALL_PREFIX}
exec_prefix=${CMAKE_INSTALL_PREFIX}/bin
includedir=${CMAKE_INSTALL_PREFIX}/include
libdir=${CMAKE_INSTALL_PREFIX}/lib${LIB_SUFFIX}

usage()
{
    cat <<EOF
Usage: commoncpp-config [OPTION]

Known values for OPTION are:

  --prefix=DIR      change ucommon prefix [default $prefix]
  --exec-prefix=DIR change ucommon exec prefix [default $exec_prefix]
  --libs        print library linking information
  --cflags      print pre-processor and compiler flags
  --includes    print framework include directory
  --plugins     print framework plugin directory
  --help        display this help and exit
  --version     output version information
EOF

    exit $1
}

if test $# -eq 0; then
    usage 1
fi

cflags=false
libs=false

while test $# -gt 0; do
    case "$1" in
    -*=*) optarg=`echo "$1" | sed 's/[-_a-zA-Z0-9]*=//'` ;;
    *) optarg= ;;
    esac

    case "$1" in
    --prefix=*)
        prefix=$optarg
        includedir=$prefix/include
        libdir=$prefix/lib
        ;;

    --prefix)
        echo $prefix
        ;;

    --exec-prefix=*)
        exec_prefix=$optarg
        libdir=$exec_prefix/lib
        ;;

    --exec-prefix)
        echo $exec_prefix
        ;;

    --version)
        echo ${PACKAGE_FILE_VERSION}
        exit 0
        ;;

    --help)
        usage 0
        ;;

    --cflags)
        echo ${PACKAGE_FLAGS}
        ;;

    --libtool-libs)
        if [ -r ${libdir}/libcommoncpp.la ]
        then
            echo ${libdir}/libcommoncpp.la
        fi
        ;;

    --libs)
        echo -lcommoncpp -lucommon ${PACKAGE_LIBS}
        ;;

    --includes)
        echo ${CMAKE_INSTALL_PREFIX}/include
        ;;

    *)
        usage
        exit 1
        ;;

    esac
    shift
done

exit 0
