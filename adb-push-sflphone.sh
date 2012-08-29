#!/bin/bash
#
#  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
#
#  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

ANDROID_REMT_CMD="abd remount"
ANDROID_PUSH_CMD="adb push"

ANDROID_SYSTEM="/system"
ANDROID_SYSTEM_BIN="$ANDROID_SYSTEM/bin"
ANDROID_SYSTEM_XBN="$ANDROID_SYSTEM/xbin"
ANDROID_SYSTEM_LIB="$ANDROID_SYSTEM/lib"
ANDROID_SYSTEM_ETC="$ANDROID_SYSTEM/etc"

ANDROID_DATA_DATA="/data/data"
ANDROID_DATA_CODECS="$ANDROID_DATA_DATA/codecs"
ANDROID_DATA_CONFIG="$ANDROID_DATA_DATA/org.sflphone.service"

LOCAL_BIN_PATH="$PWD/obj/local/armeabi"

DBUS_SESSION_D=/system/etc/session.d


adb remount

adb shell mkdir $ANDROID_DATA_CODECS
adb shell mkdir $ANDROID_DATA_CONFIG
adb shell mkdir $DBUS_SESSION_D

eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/busybox $ANDROID_SYSTEM_XBN"

eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/dbus-daemon $ANDROID_SYSTEM_BIN"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/dbus-send $ANDROID_SYSTEM_BIN"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/dbus-monitor $ANDROID_SYSTEM_BIN"

eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libccgnu2.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libcrypto.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libdbus.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libgnustl_shared.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libspeexresampler.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libcodec_ulaw.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libcodec_alaw.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libccrtp1.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libdbus-c++-1.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libexpat.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libsamplerate.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libssl.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libyaml.so $ANDROID_SYSTEM_LIB"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libsflphone.so $ANDROID_SYSTEM_LIB"

eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/session.conf $ANDROID_SYSTEM_ETC"

eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libcodec_ulaw.so $ANDROID_DATA_CODECS"
eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/libcodec_alaw.so $ANDROID_DATA_CODECS"

eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/sflphoned.yml $ANDROID_DATA_CONFIG"

eval "$ANDROID_PUSH_CMD $LOCAL_BIN_PATH/sflphoned $ANDROID_SYSTEM_BIN"
