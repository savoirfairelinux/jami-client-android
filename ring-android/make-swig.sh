#!/bin/bash -
#
#  Copyright (C) 2004-2017 Savoir-Faire Linux Inc.
#
#  Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program. If not, see <https://www.gnu.org/licenses/>.

# input: jni/jni_interface.i
# output: ringservice_loader.c
#         ring_wrapper.cpp
#         ringservice.java
#         ringserviceJNI.java
#         ManagerImpl.java

SRCDIR=libringclient/src/main/jni
PACKAGE=cx.ring.daemon
PACKAGEDIR=libringclient/src/main/java/cx/ring/daemon
ROOT=`pwd`

echo "in $ROOT"

echo "Checking that swig is 3.0.10 or later"
SWIGVER=`swig -version | \
    grep -i "SWIG version" | \
    awk '{print $3}'`
SWIGVER1=`echo $SWIGVER | \
    awk '{split($0, array, ".")} END{print array[1]}'`
SWIGVER2=`echo $SWIGVER | \
    awk '{split($0, array, ".")} END{print array[2]}'`
SWIGVER3=`echo $SWIGVER | \
    awk '{split($0, array, ".")} END{print array[3]}'`

echo swig-$SWIGVER1.$SWIGVER2.$SWIGVER3

if [[ $SWIGVER1 -ge 3 ]]; then
    if [[ $SWIGVER1 -gt 3 ]]; then
        echo "swig version is greater than 3.x"
    else
        if [[ $SWIGVER2 -ge 1 ]]; then
            echo "swig version is greater than 3.1"
        else
            if [[ $SWIGVER3 -ge 10 ]]; then
                echo "swig version is greater than 3.0.10"
            else
                echo "swig version is less than 3.0.10"
                echo "exiting..."
                exit 4
            fi
        fi
    fi
else
    echo "swig version is less than 3.x"
    echo "exiting..."
    exit 3
fi

echo "Creating package folder $PACKAGEDIR ..."

mkdir -p $PACKAGEDIR

echo "Generating ring_wrapper.cpp..."

swig -v -c++ -java \
-package $PACKAGE \
-outdir $PACKAGEDIR \
-o $SRCDIR/ring_wrapper.cpp $SRCDIR/jni_interface.i

echo "Generating ringservice_loader.c..."
python $SRCDIR/JavaJNI2CJNI_Load.py \
-i $ROOT/$PACKAGEDIR/RingserviceJNI.java \
-o $SRCDIR/ringservice_loader.c \
-t $SRCDIR/ringservice.c.template \
-m Ringservice \
-p $PACKAGE

echo "Appending ring_wrapper.cpp..."
cat $SRCDIR/ringservice_loader.c >> $SRCDIR/ring_wrapper.cpp

echo -n "in "
echo "Done"
exit 0
