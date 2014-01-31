#!/bin/sh
# Copyright (C) 2006-2009 David Sugar, Tycho Softworks.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

args=""
tag="no"
shell=$1
shift
cmd=$1
shift
for arg in $* ; do
	case "$arg" in
	--tag=*)
		tag="no"
		;;
	--tag)
		tag="yes"
		;;
	*)
		if test "$tag" = "no" ; then
			args="$args $arg"
		fi
		tag="no"
		;;
	esac
done

# Replace for old libtool??

version=`${cmd} --version 2>/dev/null | sed -e 's/([^)]*//g;s/^[^0-9]*//;s/[- ].*//g;q'` 
if test -z "$version" ; then
	version="1.5.x" ; fi
case "$version" in
1.3.*|1.4.*)
	tag=""
	;;
*)
	tag="--tag=CC"
	;;
esac
exec $shell $cmd $tag $args



