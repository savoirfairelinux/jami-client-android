#!/bin/bash - 
#===============================================================================
#
#          FILE:  make-swig.sh
# 
#         USAGE:  ./make-swig.sh 
# 
#   DESCRIPTION:  
#
# 
#        AUTHOR: Emeric Vigier (), emeric.vigier@savoirfairelinux.com
#       COMPANY: Savoir-faire Linux
#       CREATED: 2012-09-11 18:44:34 EDT
#===============================================================================

ROOT=`pwd`
echo "in $ROOT"

echo "Generating callmanager_wrap.cpp..."
swig -v -debug-tmused -c++ -java \
-package com.savoirfairelinux.sflphone.client \
-outdir src/com/savoirfairelinux/sflphone/client \
-o jni/sflphone/daemon/src/dbus/callmanager_wrap.cpp jni/sflphone/daemon/src/dbus/callmanager.i

pushd jni/sflphone/daemon/src
echo "in $PWD"

echo "Generating sflphoneservice_loader.c..."
python JavaJNI2CJNI_Load.py \
-i $ROOT/src/com/savoirfairelinux/sflphone/client/sflphoneserviceJNI.java \
-o nativesrc/sflphoneservice_loader.c \
-t sflphoneservice.c.template \
-m sflphoneservice \
-p com.savoirfairelinux.sflphone.client

echo "Appending callmanager_wrap.cpp..."
cat nativesrc/sflphoneservice_loader.c >> dbus/callmanager_wrap.cpp

echo -n "in " && popd
echo "Done"
