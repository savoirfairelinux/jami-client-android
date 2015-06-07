#!/bin/bash -
#
#  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#
#  Additional permission under GNU GPL version 3 section 7:
#
#  If you modify this program, or any covered work, by linking or
#  combining it with the OpenSSL project's OpenSSL library (or a
#  modified version of that library), containing parts covered by the
#  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
#  grants you additional permission to convey the resulting work.
#  Corresponding Source for a non-source form of such a combination
#  shall include the source code for the parts of OpenSSL used as well
#  as that of the covered work.
#

# input: jni/jni_interface.i
# output: ringservice_loader.c
#         ring_wrapper.cpp
#         ringservice.java
#         ringserviceJNI.java
#         ManagerImpl.java

SRCDIR=app/src/main/jni
PACKAGE=cx.ring.service
PACKAGEDIR=app/src/main/java/cx/ring/service
ROOT=`pwd`

echo "in $ROOT"

echo "Checking that swig is 2.0.6 or later"
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

if [[ $SWIGVER1 -ge 2 ]]; then
    echo "swig version is greater than 2.x"
    if [[ $SWIGVER1 -gt 2 ]]; then
        echo "swig version is greater than 3.x"
    else
        if [[ $SWIGVER2 -ge 1 ]]; then
            echo "swig version is greater than 2.1"
        else
            echo "swig version is less than 2.1"
            if [[ $SWIGVER3 -ge 6 ]]; then
                echo "swig version is greater than 2.0.6"
            else
                echo "swig version is less than 2.0.6"
                echo "exiting..."
                exit 4
            fi
        fi
    fi
else
    echo "swig version is less than 2.x"
    echo "exiting..."
    exit 3
fi

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
