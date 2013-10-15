#!/bin/sh
# Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

WANT_AUTOCONF_2_5=1
export WANT_AUTOCONF_2_5

rm -rf autoconf auto*.cache libtool
if test ! -d autoconf ; then mkdir autoconf ; fi

libtoolize="libtoolize"
for lt in glibtoolize libtoolize15 libtoolize14 libtoolize13 ; do
    if test -x /usr/bin/$lt ; then
        libtoolize=$lt ; break
    fi
    if test -x /usr/local/bin/$lt ; then
        libtoolize=$lt ; break
    fi
done
$libtoolize --copy --force

AUTOMAKE_FLAGS=""
case $libtoolize in
*glibtoolize)
    AUTOMAKE_FLAGS="-i"
    ;;
esac

ACLOCALDIRS=""
if test -d m4 ; then
    ACLOCALDIRS="-I m4" ; fi

if test ! -z "$ACLOCAL" ; then
    ACLOCALDIRS="$ACLOCALDIRS -I"${ACLOCAL}
elif test ! -z "$ACLOCAL_FLAGS" ; then
    ACLOCALDIRS="$ACLOCALDIRS $ACLOCAL_FLAGS" ; fi

reconf=""
if test -f ~/.configure ; then
    reconf=`grep ^reconfig: ~/.configure | sed -e s/^reconfig://`
elif test -f /etc/configure.conf ; then
    reconf=`grep ^reconfig: /etc/configure.conf | sed -e s/^reconfig://` ; fi
if test ! -z "$reconf" ; then ACLOCALDIRS="$ACLOCALDIRS $reconf" ; fi

if [ -d ~/share/aclocal ] ; then
    ACLOCALDIRS="$ACLOCALDIRS -I ~/share/aclocal"
elif [ -d /usr/local/share/aclocal ] ; then
    ACLOCALDIRS="$ACLOCALDIRS -I /usr/local/share/aclocal"
fi

if test ! -z "$1" -o ! -z "${AUTOCONF_SUFFIX}" ; then
    ver="$1"
else
    for v in 2.53 2.57 ; do
        if test -f /usr/bin/autoconf-$v ; then
            ver=$v
        fi
    done
fi

if test "$ver" = "2.53" -a -z "$AUTOMAKE_SUFFIX" ; then
    if test -f /usr/bin/automake-1.5 ; then
        AUTOMAKE_SUFFIX="-1.5"
    fi
fi

aclocal${AUTOMAKE_SUFFIX} $ACLOCALDIRS

if test -f /usr/bin/autoheader-$ver ; then
    /usr/bin/autoheader-$ver
else
    autoheader${AUTOCONF_SUFFIX}
fi

automake${AUTOMAKE_SUFFIX} --add-missing --copy ${AUTOMAKE_FLAGS}

if test -f /usr/bin/autoconf-$ver ; then
    /usr/bin/autoconf-$ver
else
    autoconf${AUTOCONF_SUFFIX}
fi
rm -f config.cache

# fix for some broken...

if test -f /usr/bin/automake-1.4 ; then
    if test -f ltmain.sh ; then
        cp ltmain.sh autoconf/ltmain.sh
    fi
fi

