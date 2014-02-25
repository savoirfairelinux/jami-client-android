#!/bin/bash
#
#  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
#
#  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

# Meant to be run on the target platform using adb shell

cd jni/
ndk-build -j4
cd ..

$ANT_HOME clean
$ANT_HOME debug

adb install -r bin/SFLphone-debug.apk

adb shell am start -n org.sflphone/org.sflphone.client.HomeActivity
