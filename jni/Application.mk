#  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 #
 #  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>           
 #          Adrien Beraud <adrien.beraud@gmail.com> 
 #
 #  This program is free software; you can redistribute it and/or modify
 #  it under the terms of the GNU General Public License as published by
 #  the Free Software Foundation; either version 3 of the License, or
 #  (at your option) any later version.
 #  This program is distributed in the hope that it will be useful,
 #  but WITHOUT ANY WARRANTY; without even the implied warranty of
 #  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 #  GNU General Public License for more details.
 #
 #  You should have received a copy of the GNU General Public License
 #  along with this program; if not, write to the Free Software
 #  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA.
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


BASE_PJSIP_FLAGS := -DPJ_ANDROID=1
# about codecs
BASE_PJSIP_FLAGS += -DPJMEDIA_HAS_G729_CODEC=0 -DPJMEDIA_HAS_G726_CODEC=0 \
    -DPJMEDIA_HAS_ILBC_CODEC=0 -DPJMEDIA_HAS_G722_CODEC=0 \
    -DPJMEDIA_HAS_SPEEX_CODEC=0 -DPJMEDIA_HAS_GSM_CODEC=0 \
    -DPJMEDIA_HAS_SILK_CODEC=0 -DPJMEDIA_HAS_CODEC2_CODEC=0 \
    -DPJMEDIA_HAS_G7221_CODEC=0 -DPJMEDIA_HAS_WEBRTC_CODEC=0 \
    -DPJMEDIA_HAS_OPUS_CODEC=0

# TLS ZRTP
BASE_PJSIP_FLAGS += -DPJ_HAS_SSL_SOCK=1 -DPJMEDIA_HAS_ZRTP=1

NDK_TOOLCHAIN_VERSION := 4.8

APP_PLATFORM := android-14
APP_OPTIM := debug
NDK_DEBUG := 1
APP_STL := gnustl_shared
APP_ABI := armeabi-v7a x86


APP_MODULES += libcodec_ulaw
APP_MODULES += libcodec_alaw
APP_MODULES += libcodec_g722
APP_MODULES += libcodec_opus
APP_MODULES += libcodec_gsm
APP_MODULES += libcodec_speex_nb
APP_MODULES += libcodec_speex_ub
APP_MODULES += libcodec_speex_wb
APP_MODULES += libsflphone
