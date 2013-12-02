#
# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

include $(CLEAR_VARS)

MY_SPEEX := libspeex/sources

LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES := \
	$(MY_SPEEX)/libspeex/mdf.c \
	$(MY_SPEEX)/libspeex/preprocess.c \
	$(MY_SPEEX)/libspeex/filterbank.c \
	$(MY_SPEEX)/libspeex/fftwrap.c \
	$(MY_SPEEX)/libspeex/smallft.c \
	$(MY_SPEEX)/libspeex/bits.c \
	$(MY_SPEEX)/libspeex/buffer.c \
	$(MY_SPEEX)/libspeex/cb_search.c \
	$(MY_SPEEX)/libspeex/exc_10_16_table.c \
	$(MY_SPEEX)/libspeex/exc_10_32_table.c \
	$(MY_SPEEX)/libspeex/exc_20_32_table.c \
	$(MY_SPEEX)/libspeex/exc_5_256_table.c \
	$(MY_SPEEX)/libspeex/exc_5_64_table.c \
	$(MY_SPEEX)/libspeex/exc_8_128_table.c \
	$(MY_SPEEX)/libspeex/filters.c \
	$(MY_SPEEX)/libspeex/gain_table.c \
	$(MY_SPEEX)/libspeex/gain_table_lbr.c \
	$(MY_SPEEX)/libspeex/modes.c \
	$(MY_SPEEX)/libspeex/modes_wb.c \
	$(MY_SPEEX)/libspeex/speex.c \
	$(MY_SPEEX)/libspeex/hexc_10_32_table.c \
	$(MY_SPEEX)/libspeex/hexc_table.c \
	$(MY_SPEEX)/libspeex/high_lsp_tables.c \
	$(MY_SPEEX)/libspeex/jitter.c \
	$(MY_SPEEX)/libspeex/kiss_fft.c \
	$(MY_SPEEX)/libspeex/kiss_fftr.c \
	$(MY_SPEEX)/libspeex/lpc.c \
	$(MY_SPEEX)/libspeex/lsp.c \
	$(MY_SPEEX)/libspeex/lsp_tables_nb.c \
	$(MY_SPEEX)/libspeex/ltp.c \
	$(MY_SPEEX)/libspeex/nb_celp.c \
	$(MY_SPEEX)/libspeex/quant_lsp.c \
	$(MY_SPEEX)/libspeex/sb_celp.c \
	$(MY_SPEEX)/libspeex/scal.c \
	$(MY_SPEEX)/libspeex/speex_callbacks.c \
	$(MY_SPEEX)/libspeex/speex_header.c \
	$(MY_SPEEX)/libspeex/stereo.c \
	$(MY_SPEEX)/libspeex/vbr.c \
	$(MY_SPEEX)/libspeex/vq.c \
	$(MY_SPEEX)/libspeex/window.c \


LOCAL_MODULE:= libspeex

LOCAL_CFLAGS+= -DEXPORT= -DFLOATING_POINT -DUSE_SMALLFT -DVAR_ARRAYS
LOCAL_CFLAGS+= -O3 -fstrict-aliasing -fprefetch-loop-arrays 

LOCAL_C_INCLUDES += $(MY_SPEEX)/include

include $(BUILD_STATIC_LIBRARY)



############# speexresampler #################

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES :=	$(MY_SPEEX)/libspeex/resample.c

LOCAL_MODULE:= libspeexresampler
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS += -DEXPORT= -DFIXED_POINT -DRESAMPLE_FORCE_FULL_SINC_TABLE
LOCAL_CFLAGS += -O3 -fstrict-aliasing -fprefetch-loop-arrays

ifeq ($(ARCH_ARM_HAVE_NEON),true)
LOCAL_CFLAGS += -D_USE_NEON
endif

LOCAL_C_INCLUDES += $(MY_SPEEX)/include

include $(BUILD_STATIC_LIBRARY)



