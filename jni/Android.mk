
LOCAL_PATH:= $(call my-dir)
LOCAL_CODECS_PATH = sflphone/daemon/src/audio/codecs
LOCAL_AUDIO_PATH = sflphone/daemon/src/audio
LOCAL_SRC_PATH = sflphone/daemon/src


include $(CLEAR_VARS)
# /!\ absolutely necessary when including submakefiles
# and defining targets in the "same Android.mk"
#include $(call all-subdir-makefiles)

# FIXME
VERSION="1.1.0"
MY_PREFIX=/sdcard
MY_DATADIR=/data/data
TARGET_NAME=arm-unknown-linux-androideabi

LOCAL_CODECS_PATH = sflphone/daemon/src/audio/codecs

MY_PJPROJECT=pjproject-android
MY_COMMONCPP=commoncpp2-android
MY_CCRTP=ccrtp-android
MY_LIBSAMPLE=libsamplerate
MY_SPEEX=libspeex
MY_OPENSSL=openssl
MY_LIBYAML=libyaml
MY_PCRE=libpcre
MY_LIBEXPAT=libexpat
MY_OPUS=libopus
MY_LIBSNDFILE=libsndfile
MY_LIBGSM=libgsm
MY_JNI_WRAP := $(LOCAL_SRC_PATH)/client/android/callmanager_wrap.cpp

include $(CLEAR_VARS)

$(MY_JNI_WRAP): $(LOCAL_SRC_PATH)/client/android/jni_interface.i $(LOCAL_SRC_PATH)/client/android/sflphoneservice.c.template
	@echo "in $(MY_JNI_WRAP) target"
	./make-swig.sh

LOCAL_SRC_FILES := \
		$(LOCAL_SRC_PATH)/conference.cpp \
		$(LOCAL_SRC_PATH)/voiplink.cpp \
		$(LOCAL_SRC_PATH)/preferences.cpp \
		$(LOCAL_SRC_PATH)/managerimpl.cpp \
		$(LOCAL_SRC_PATH)/manager.cpp \
		$(LOCAL_SRC_PATH)/eventthread.cpp \
		$(LOCAL_SRC_PATH)/call.cpp \
		$(LOCAL_SRC_PATH)/account.cpp \
		$(LOCAL_SRC_PATH)/numbercleaner.cpp \
		$(LOCAL_SRC_PATH)/fileutils.cpp \
		$(LOCAL_SRC_PATH)/audio/audioloop.cpp \
		$(LOCAL_SRC_PATH)/audio/ringbuffer.cpp \
		$(LOCAL_SRC_PATH)/audio/mainbuffer.cpp \
		$(LOCAL_SRC_PATH)/audio/audiorecord.cpp \
		$(LOCAL_SRC_PATH)/audio/audiobuffer.cpp \
		$(LOCAL_SRC_PATH)/audio/audiorecorder.cpp \
		$(LOCAL_SRC_PATH)/audio/recordable.cpp \
		$(LOCAL_SRC_PATH)/audio/audiolayer.cpp \
		$(LOCAL_SRC_PATH)/audio/samplerateconverter.cpp \
		$(LOCAL_SRC_PATH)/audio/delaydetection.cpp \
		$(LOCAL_SRC_PATH)/audio/dcblocker.cpp \
		$(LOCAL_SRC_PATH)/audio/opensl/opensllayer.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/audiofile.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/tone.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/tonelist.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/dtmf.cpp \
		$(LOCAL_SRC_PATH)/audio/sound/dtmfgenerator.cpp \
		$(LOCAL_SRC_PATH)/audio/codecs/audiocodecfactory.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_rtp_session.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_symmetric_rtp_session.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_rtp_record_handler.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_rtp_factory.cpp \
		$(LOCAL_SRC_PATH)/audio/audiortp/audio_srtp_session.cpp \
		$(LOCAL_SRC_PATH)/config/sfl_config.cpp \
		$(LOCAL_SRC_PATH)/config/yamlemitter.cpp \
		$(LOCAL_SRC_PATH)/config/yamlparser.cpp \
		$(LOCAL_SRC_PATH)/config/yamlnode.cpp \
		$(LOCAL_SRC_PATH)/client/android/client.cpp \
		$(LOCAL_SRC_PATH)/client/callmanager.cpp \
		$(LOCAL_SRC_PATH)/client/android/callmanager_jni.cpp \
		$(LOCAL_SRC_PATH)/client/configurationmanager.cpp  \
		$(LOCAL_SRC_PATH)/client/android/configurationmanager_jni.cpp  \
		$(LOCAL_SRC_PATH)/client/presencemanager.cpp  \
		$(LOCAL_SRC_PATH)/client/android/presencemanager_jni.cpp  \
		$(LOCAL_SRC_PATH)/client/android/callmanager_wrap.cpp \
		$(LOCAL_SRC_PATH)/history/historyitem.cpp \
		$(LOCAL_SRC_PATH)/history/history.cpp \
		$(LOCAL_SRC_PATH)/history/historynamecache.cpp \
		$(LOCAL_SRC_PATH)/hooks/urlhook.cpp \
		$(LOCAL_SRC_PATH)/im/instant_messaging.cpp \
		$(LOCAL_SRC_PATH)/sip/sdp.cpp \
		$(LOCAL_SRC_PATH)/sip/sipaccount.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp \
		$(LOCAL_SRC_PATH)/sip/sipcall.cpp \
		$(LOCAL_SRC_PATH)/sip/sipvoiplink.cpp \
		$(LOCAL_SRC_PATH)/sip/siptransport.cpp \
		$(LOCAL_SRC_PATH)/sip/sip_utils.cpp \
		$(LOCAL_SRC_PATH)/sip/sippresence.cpp \
		$(LOCAL_SRC_PATH)/sip/pattern.cpp \
		$(LOCAL_SRC_PATH)/sip/sdes_negotiator.cpp \
		$(LOCAL_SRC_PATH)/sip/pres_sub_client.cpp \
		$(LOCAL_SRC_PATH)/sip/pres_sub_server.cpp

# FIXME
LOCAL_C_INCLUDES += $(LOCAL_SRC_PATH)/.. \
			$(LOCAL_SRC_PATH) \
			$(LOCAL_SRC_PATH)/audio \
			$(LOCAL_SRC_PATH)/audio/opensl \
			$(LOCAL_SRC_PATH)/audio/sound \
			$(LOCAL_SRC_PATH)/audio/codecs \
			$(LOCAL_SRC_PATH)/audio/audiortp \
			$(LOCAL_SRC_PATH)/config \
			$(LOCAL_SRC_PATH)/client/android \
			$(LOCAL_SRC_PATH)/history \
			$(LOCAL_SRC_PATH)/hooks \
			$(LOCAL_SRC_PATH)/im \
			$(LOCAL_SRC_PATH)/sip \
			$(APP_PROJECT_PATH)/jni/$(MY_SPEEX)/include \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc \
			$(APP_PROJECT_PATH)/jni/$(MY_LIBYAML)/inc \
			$(APP_PROJECT_PATH)/jni/$(MY_PCRE) \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_LIBSAMPLE)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_OPENSSL)/include \
			$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjsip/include \
			$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjlib/include \
			$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjlib-util/include \
			$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjmedia/include \
			$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjnath/include \
			$(APP_PROJECT_PATH)/jni/$(MY_LIBEXPAT) \
			$(APP_PROJECT_PATH)/jni/$(MY_LIBSNDFILE)/src

LOCAL_MODULE := libsflphone

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
					-DCCPP_PREFIX \
					-DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
					-DPREFIX=\"$(MY_PREFIX)\" \
					-DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
					-DHAVE_CONFIG_H \
					-DHAVE_SPEEX_CODEC \
					-DHAVE_GSM_CODEC \
					-w \
					-std=c++11 -frtti -fexceptions -fpermissive \
					-DAPP_NAME=\"sflphone\" \
					-DSWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON \
					-DDEBUG_DIRECTOR_OWNED \
					-DPJ_AUTOCONF=1

#-L$(APP_PROJECT_PATH)/obj/local/armeabi \

LOCAL_LDLIBS  += -L$(APP_PROJECT_PATH)/obj/local/armeabi-v7a \
		 -L$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjsip/lib \
		 -L$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjlib/lib \
		 -L$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjlib-util/lib \
		 -L$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjmedia/lib \
		 -L$(APP_PROJECT_PATH)/jni/$(MY_PJPROJECT)/pjnath/lib \
		 -lpjsua-$(TARGET_NAME) \
		 -lpjsip-ua-$(TARGET_NAME) \
		 -lpjsip-simple-$(TARGET_NAME) \
		 -lpjsip-$(TARGET_NAME) \
		 -lpjmedia-codec-$(TARGET_NAME) \
		 -lpjmedia-$(TARGET_NAME) \
		 -lpjnath-$(TARGET_NAME) \
		 -lpjlib-util-$(TARGET_NAME) \
		 -lpj-$(TARGET_NAME) \
		 -lccgnu2 \
		 -lsamplerate \
		 -lspeexresampler \
		 -lsamplerate \
		 -lcrypto \
		 -lz \
		 -llog \
		 -lOpenSLES \
		 -lgnustl_shared

# LOCAL_STATIC_LIBRARIES (NDK documentation)
#   The list of static libraries modules (built with BUILD_STATIC_LIBRARY)
#   that should be linked to this module. This only makes sense in
#   shared library modules.
LOCAL_STATIC_LIBRARIES += 	libpjsua-$(TARGET_NAME) \
							libpjsip-ua-$(TARGET_NAME) \
							libpjsip-simple-$(TARGET_NAME) \
							libpjsip-$(TARGET_NAME) \
							libpjmedia-codec-$(TARGET_NAME) \
							libpjmedia-$(TARGET_NAME) \
							libpjnath-$(TARGET_NAME) \
							libpjlib-util-$(TARGET_NAME) \
							libpj-$(TARGET_NAME) \
							libssl \
							libsamplerate \
							libcrypto
						


LOCAL_SHARED_LIBRARIES += libccrtp1 \
				libexpat_shared \
				libpcre \
				libspeexresampler \
				libyaml \
	 			libsndfile

include $(BUILD_SHARED_LIBRARY)

############### libsndfile ##################

include $(CLEAR_VARS)

LOCAL_MODULE   := libsndfile

LOCAL_SRC_FILES := 	$(MY_LIBSNDFILE)/src/mat5.c \
					$(MY_LIBSNDFILE)/src/windows.c \
					$(MY_LIBSNDFILE)/src/G72x/g723_24.c \
					$(MY_LIBSNDFILE)/src/G72x/g72x.c \
					$(MY_LIBSNDFILE)/src/G72x/g723_40.c \
					$(MY_LIBSNDFILE)/src/G72x/g721.c \
					$(MY_LIBSNDFILE)/src/G72x/g723_16.c \
					$(MY_LIBSNDFILE)/src/float32.c \
					$(MY_LIBSNDFILE)/src/chanmap.c $(MY_LIBSNDFILE)/src/test_endswap.c $(MY_LIBSNDFILE)/src/rf64.c \
					$(MY_LIBSNDFILE)/src/sndfile.c \
					$(MY_LIBSNDFILE)/src/htk.c $(MY_LIBSNDFILE)/src/dither.c \
       				$(MY_LIBSNDFILE)/src/test_log_printf.c $(MY_LIBSNDFILE)/src/txw.c \
					$(MY_LIBSNDFILE)/src/ms_adpcm.c $(MY_LIBSNDFILE)/src/ima_adpcm.c \
					$(MY_LIBSNDFILE)/src/flac.c $(MY_LIBSNDFILE)/src/aiff.c \
					$(MY_LIBSNDFILE)/src/wav.c \
					$(MY_LIBSNDFILE)/src/macbinary3.c \
					$(MY_LIBSNDFILE)/src/mat4.c \
					$(MY_LIBSNDFILE)/src/pcm.c \
					$(MY_LIBSNDFILE)/src/caf.c \
					$(MY_LIBSNDFILE)/src/audio_detect.c \
					$(MY_LIBSNDFILE)/src/id3.c \
					$(MY_LIBSNDFILE)/src/alaw.c $(MY_LIBSNDFILE)/src/macos.c $(MY_LIBSNDFILE)/src/file_io.c $(MY_LIBSNDFILE)/src/broadcast.c \
					$(MY_LIBSNDFILE)/src/double64.c \
					$(MY_LIBSNDFILE)/src/raw.c $(MY_LIBSNDFILE)/src/test_broadcast_var.c \
					$(MY_LIBSNDFILE)/src/g72x.c $(MY_LIBSNDFILE)/src/command.c \
					$(MY_LIBSNDFILE)/src/chunk.c $(MY_LIBSNDFILE)/src/avr.c \
					$(MY_LIBSNDFILE)/src/sd2.c $(MY_LIBSNDFILE)/src/voc.c \
					$(MY_LIBSNDFILE)/src/test_audio_detect.c \
					$(MY_LIBSNDFILE)/src/mpc2k.c $(MY_LIBSNDFILE)/src/gsm610.c $(MY_LIBSNDFILE)/src/dwd.c \
					$(MY_LIBSNDFILE)/src/interleave.c $(MY_LIBSNDFILE)/src/common.c \
					$(MY_LIBSNDFILE)/src/test_strncpy_crlf.c $(MY_LIBSNDFILE)/src/sds.c \
					$(MY_LIBSNDFILE)/src/pvf.c $(MY_LIBSNDFILE)/src/paf.c \
					$(MY_LIBSNDFILE)/src/au.c \
					$(MY_LIBSNDFILE)/src/test_float.c \
					$(MY_LIBSNDFILE)/src/vox_adpcm.c $(MY_LIBSNDFILE)/src/ulaw.c \
					$(MY_LIBSNDFILE)/src/strings.c $(MY_LIBSNDFILE)/src/svx.c \
					$(MY_LIBSNDFILE)/src/test_conversions.c $(MY_LIBSNDFILE)/src/rx2.c \
					$(MY_LIBSNDFILE)/src/nist.c \
					$(MY_LIBSNDFILE)/src/GSM610/code.c $(MY_LIBSNDFILE)/src/GSM610/gsm_destroy.c \
					$(MY_LIBSNDFILE)/src/GSM610/gsm_decode.c $(MY_LIBSNDFILE)/src/GSM610/short_term.c $(MY_LIBSNDFILE)/src/GSM610/gsm_create.c \
					$(MY_LIBSNDFILE)/src/GSM610/decode.c $(MY_LIBSNDFILE)/src/GSM610/gsm_option.c \
					$(MY_LIBSNDFILE)/src/GSM610/long_term.c $(MY_LIBSNDFILE)/src/GSM610/table.c $(MY_LIBSNDFILE)/src/GSM610/rpe.c $(MY_LIBSNDFILE)/src/GSM610/preprocess.c \
					$(MY_LIBSNDFILE)/src/GSM610/gsm_encode.c $(MY_LIBSNDFILE)/src/GSM610/lpc.c \
					$(MY_LIBSNDFILE)/src/GSM610/add.c $(MY_LIBSNDFILE)/src/dwvw.c \
					$(MY_LIBSNDFILE)/src/wav_w64.c $(MY_LIBSNDFILE)/src/wve.c $(MY_LIBSNDFILE)/src/ogg.c $(MY_LIBSNDFILE)/src/w64.c \
					$(MY_LIBSNDFILE)/src/test_file_io.c \
					$(MY_LIBSNDFILE)/src/ircam.c $(MY_LIBSNDFILE)/src/xi.c $(MY_LIBSNDFILE)/src/ima_oki_adpcm.c

LOCAL_C_INCLUDES += $(APP_PROJECT_PATH)/jni/$(MY_LIBSNDFILE)/src \
					$(APP_PROJECT_PATH)/jni/sflphone/daemon/src

LOCAL_LDLIBS  += -L$(APP_PROJECT_PATH)/obj/local/armeabi-v7a \
			


LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)


############# ulaw ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/ulaw.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp

# FIXME
LOCAL_C_INCLUDES += $(LOCAL_CODECS_PATH)/.. \
			$(LOCAL_CODECS_PATH)/../.. \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc 

LOCAL_MODULE := libcodec_ulaw

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codec_ulaw\"

include $(BUILD_SHARED_LIBRARY)



############# alaw ###############

include $(CLEAR_VARS)



LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/alaw.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_CODECS_PATH)/.. \
			$(LOCAL_CODECS_PATH)/../.. \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc \

LOCAL_MODULE := libcodec_alaw

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codec_alaw\"

LOCAL_LDFLAGS += -Wl,--export-dynamic

include $(BUILD_SHARED_LIBRARY)


############# g722 ###############

include $(CLEAR_VARS)



LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/g722.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_CODECS_PATH)/.. \
			$(LOCAL_CODECS_PATH)/../.. \
			$(LOCAL_CODECS_PATH)/../../.. \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc 

LOCAL_MODULE := libcodec_g722

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)

############# libgsm ###############

include $(CLEAR_VARS)



LOCAL_SRC_FILES := 	$(LOCAL_CODECS_PATH)/gsmcodec.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp \
					$(MY_LIBGSM)/src/add.c \
					$(MY_LIBGSM)/src/debug.c \
					$(MY_LIBGSM)/src/gsm_create.c \
					$(MY_LIBGSM)/src/gsm_destroy.c \
					$(MY_LIBGSM)/src/gsm_explode.c \
					$(MY_LIBGSM)/src/gsm_option.c \
					$(MY_LIBGSM)/src/long_term.c \
					$(MY_LIBGSM)/src/preprocess.c \
					$(MY_LIBGSM)/src/short_term.c \
					$(MY_LIBGSM)/src/code.c \
					$(MY_LIBGSM)/src/gsm_decode.c \
					$(MY_LIBGSM)/src/gsm_encode.c \
					$(MY_LIBGSM)/src/decode.c \
					$(MY_LIBGSM)/src/gsm_print.c \
					$(MY_LIBGSM)/src/lpc.c \
					$(MY_LIBGSM)/src/rpe.c \
					$(MY_LIBGSM)/src/table.c \

LOCAL_C_INCLUDES += $(LOCAL_CODECS_PATH)/.. \
			$(LOCAL_CODECS_PATH)/../.. \
			$(LOCAL_CODECS_PATH)/../../.. \
			$(APP_PROJECT_PATH)/jni/$(MY_LIBGSM)/inc \

LOCAL_MODULE := libcodec_gsm

LOCAL_LDLIBS := -llog

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
					-DCCPP_PREFIX \
					-DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
					-DPREFIX=\"$(MY_PREFIX)\" \
					-DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
					-DHAVE_CONFIG_H \
					-std=c++11 -frtti -fpermissive -fexceptions \
					-DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)

############# libopus ###############

include $(CLEAR_VARS)

LOCAL_CELT_SOURCES := 	$(MY_OPUS)/celt/bands.c \
						$(MY_OPUS)/celt/celt_lpc.c \
						$(MY_OPUS)/celt/rate.c \
						$(MY_OPUS)/celt/entenc.c \
						$(MY_OPUS)/celt/modes.c \
						$(MY_OPUS)/celt/mdct.c \
						$(MY_OPUS)/celt/entcode.c \
						$(MY_OPUS)/celt/celt.c \
						$(MY_OPUS)/celt/laplace.c \
						$(MY_OPUS)/celt/cwrs.c \
						$(MY_OPUS)/celt/quant_bands.c \
						$(MY_OPUS)/celt/opus_custom_demo.c \
						$(MY_OPUS)/celt/pitch.c \
						$(MY_OPUS)/celt/entdec.c \
						$(MY_OPUS)/celt/kiss_fft.c \
						$(MY_OPUS)/celt/mathops.c \
						$(MY_OPUS)/celt/vq.c
    

LOCAL_SILK_SOURCES :=  	$(MY_OPUS)/silk/CNG.c \
						$(MY_OPUS)/silk/VQ_WMat_EC.c \
						$(MY_OPUS)/silk/tables_pulses_per_block.c \
						$(MY_OPUS)/silk/check_control_input.c \
						$(MY_OPUS)/silk/NLSF_encode.c \
						$(MY_OPUS)/silk/gain_quant.c \
						$(MY_OPUS)/silk/stereo_MS_to_LR.c \
						$(MY_OPUS)/silk/control_SNR.c \
						$(MY_OPUS)/silk/stereo_decode_pred.c \
						$(MY_OPUS)/silk/init_encoder.c \
						$(MY_OPUS)/silk/bwexpander_32.c \
						$(MY_OPUS)/silk/ana_filt_bank_1.c \
						$(MY_OPUS)/silk/control_codec.c \
						$(MY_OPUS)/silk/stereo_encode_pred.c \
						$(MY_OPUS)/silk/shell_coder.c \
						$(MY_OPUS)/silk/PLC.c \
						$(MY_OPUS)/silk/encode_pulses.c \
						$(MY_OPUS)/silk/resampler_rom.c \
						$(MY_OPUS)/silk/stereo_quant_pred.c \
						$(MY_OPUS)/silk/CNG.c \
						$(MY_OPUS)/silk/biquad_alt.c \
						$(MY_OPUS)/silk/resampler_down2_3.c \
						$(MY_OPUS)/silk/VAD.c \
						$(MY_OPUS)/silk/LPC_analysis_filter.c \
						$(MY_OPUS)/silk/NSQ_del_dec.c \
						$(MY_OPUS)/silk/NLSF_stabilize.c \
						$(MY_OPUS)/silk/tables_pitch_lag.c \
						$(MY_OPUS)/silk/decode_indices.c \
						$(MY_OPUS)/silk/NLSF_del_dec_quant.c \
						$(MY_OPUS)/silk/A2NLSF.c \
						$(MY_OPUS)/silk/resampler.c \
						$(MY_OPUS)/silk/decode_frame.c \
						$(MY_OPUS)/silk/tables_other.c \
						$(MY_OPUS)/silk/tables_NLSF_CB_NB_MB.c \
						$(MY_OPUS)/silk/decode_pitch.c \
						$(MY_OPUS)/silk/resampler_down2.c \
						$(MY_OPUS)/silk/encode_indices.c \
						$(MY_OPUS)/silk/decode_parameters.c \
						$(MY_OPUS)/silk/resampler_private_AR2.c \
						$(MY_OPUS)/silk/init_decoder.c \
						$(MY_OPUS)/silk/quant_LTP_gains.c \
						$(MY_OPUS)/silk/decode_core.c \
						$(MY_OPUS)/silk/enc_API.c \
						$(MY_OPUS)/silk/code_signs.c \
						$(MY_OPUS)/silk/lin2log.c \
						$(MY_OPUS)/silk/control_audio_bandwidth.c \
						$(MY_OPUS)/silk/NLSF2A.c \
						$(MY_OPUS)/silk/NSQ.c \
						$(MY_OPUS)/silk/tables_gain.c \
						$(MY_OPUS)/silk/dec_API.c \
						$(MY_OPUS)/silk/table_LSF_cos.c \
						$(MY_OPUS)/silk/resampler_private_down_FIR.c \
						$(MY_OPUS)/silk/NLSF_decode.c \
						$(MY_OPUS)/silk/sum_sqr_shift.c \
						$(MY_OPUS)/silk/interpolate.c \
						$(MY_OPUS)/silk/bwexpander.c \
						$(MY_OPUS)/silk/sigm_Q15.c \
						$(MY_OPUS)/silk/LPC_inv_pred_gain.c \
						$(MY_OPUS)/silk/NLSF_unpack.c \
						$(MY_OPUS)/silk/tables_LTP.c \
						$(MY_OPUS)/silk/decode_pulses.c \
						$(MY_OPUS)/silk/inner_prod_aligned.c \
						$(MY_OPUS)/silk/LP_variable_cutoff.c \
						$(MY_OPUS)/silk/debug.c \
						$(MY_OPUS)/silk/stereo_LR_to_MS.c \
						$(MY_OPUS)/silk/stereo_find_predictor.c \
						$(MY_OPUS)/silk/process_NLSFs.c \
						$(MY_OPUS)/silk/tables_NLSF_CB_WB.c \
						$(MY_OPUS)/silk/NLSF_VQ.c \
						$(MY_OPUS)/silk/log2lin.c \
						$(MY_OPUS)/silk/decoder_set_fs.c \
						$(MY_OPUS)/silk/sort.c \
						$(MY_OPUS)/silk/HP_variable_cutoff.c \
						$(MY_OPUS)/silk/NLSF_VQ_weights_laroia.c \
						$(MY_OPUS)/silk/resampler_private_up2_HQ.c \
						$(MY_OPUS)/silk/pitch_est_tables.c \
						$(MY_OPUS)/silk/resampler_private_IIR_FIR.c




LOCAL_SILK_SOURCES_FIXED := $(MY_OPUS)/silk/fixed/LTP_analysis_filter_FIX.c \
							$(MY_OPUS)/silk/fixed/find_pitch_lags_FIX.c \
							$(MY_OPUS)/silk/fixed/solve_LS_FIX.c \
							$(MY_OPUS)/silk/fixed/k2a_FIX.c \
							$(MY_OPUS)/silk/fixed/regularize_correlations_FIX.c \
							$(MY_OPUS)/silk/fixed/apply_sine_window_FIX.c \
							$(MY_OPUS)/silk/fixed/corrMatrix_FIX.c \
							$(MY_OPUS)/silk/fixed/process_gains_FIX.c \
							$(MY_OPUS)/silk/fixed/prefilter_FIX.c \
							$(MY_OPUS)/silk/fixed/autocorr_FIX.c \
							$(MY_OPUS)/silk/fixed/find_LTP_FIX.c \
							$(MY_OPUS)/silk/fixed/warped_autocorrelation_FIX.c \
							$(MY_OPUS)/silk/fixed/schur_FIX.c \
							$(MY_OPUS)/silk/fixed/burg_modified_FIX.c \
							$(MY_OPUS)/silk/fixed/noise_shape_analysis_FIX.c \
							$(MY_OPUS)/silk/fixed/residual_energy_FIX.c \
							$(MY_OPUS)/silk/fixed/find_LPC_FIX.c \
							$(MY_OPUS)/silk/fixed/encode_frame_FIX.c \
							$(MY_OPUS)/silk/fixed/k2a_Q16_FIX.c \
							$(MY_OPUS)/silk/fixed/LTP_analysis_filter_FIX.c \
							$(MY_OPUS)/silk/fixed/find_pred_coefs_FIX.c \
							$(MY_OPUS)/silk/fixed/schur64_FIX.c \
							$(MY_OPUS)/silk/fixed/pitch_analysis_core_FIX.c \
							$(MY_OPUS)/silk/fixed/vector_ops_FIX.c \
							$(MY_OPUS)/silk/fixed/residual_energy16_FIX.c \
							$(MY_OPUS)/silk/fixed/LTP_scale_ctrl_FIX.c \



LOCAL_SILK_SOURCES_FLOAT := $(MY_OPUS)/silk/float/apply_sine_window_FLP.c \
							$(MY_OPUS)/silk/float/inner_product_FLP.c \
							$(MY_OPUS)/silk/float/warped_autocorrelation_FLP.c \
							$(MY_OPUS)/silk/float/autocorrelation_FLP.c \
							$(MY_OPUS)/silk/float/find_pred_coefs_FLP.c \
							$(MY_OPUS)/silk/float/LPC_analysis_filter_FLP.c \
							$(MY_OPUS)/silk/float/find_LPC_FLP.c \
							$(MY_OPUS)/silk/float/scale_copy_vector_FLP.c \
							$(MY_OPUS)/silk/float/wrappers_FLP.c \
							$(MY_OPUS)/silk/float/process_gains_FLP.c \
							$(MY_OPUS)/silk/float/burg_modified_FLP.c \
							$(MY_OPUS)/silk/float/k2a_FLP.c \
							$(MY_OPUS)/silk/float/prefilter_FLP.c \
							$(MY_OPUS)/silk/float/corrMatrix_FLP.c \
							$(MY_OPUS)/silk/float/LPC_inv_pred_gain_FLP.c \
							$(MY_OPUS)/silk/float/LTP_scale_ctrl_FLP.c \
							$(MY_OPUS)/silk/float/pitch_analysis_core_FLP.c \
							$(MY_OPUS)/silk/float/solve_LS_FLP.c \
							$(MY_OPUS)/silk/float/energy_FLP.c \
							$(MY_OPUS)/silk/float/find_pitch_lags_FLP.c \
							$(MY_OPUS)/silk/float/LTP_analysis_filter_FLP.c \
							$(MY_OPUS)/silk/float/residual_energy_FLP.c \
							$(MY_OPUS)/silk/float/regularize_correlations_FLP.c \
							$(MY_OPUS)/silk/float/bwexpander_FLP.c \
							$(MY_OPUS)/silk/float/noise_shape_analysis_FLP.c \
							$(MY_OPUS)/silk/float/encode_frame_FLP.c \
							$(MY_OPUS)/silk/float/scale_vector_FLP.c \
							$(MY_OPUS)/silk/float/sort_FLP.c \
							$(MY_OPUS)/silk/float/find_LTP_FLP.c \
							$(MY_OPUS)/silk/float/levinsondurbin_FLP.c \
							$(MY_OPUS)/silk/float/schur_FLP.c \

LOCAL_OPUS_SOURCES := 	$(MY_OPUS)/src/opus.c \
						$(MY_OPUS)/src/repacketizer_demo.c \
						$(MY_OPUS)/src/opus_decoder.c \
						$(MY_OPUS)/src/repacketizer.c \
						$(MY_OPUS)/src/opus_demo.c \
						$(MY_OPUS)/src/opus_multistream.c \
						$(MY_OPUS)/src/opus_encoder.c \
						$(MY_OPUS)/src/opus_compare.c \

LOCAL_CFLAGS := -DNONTHREADSAFE_PSEUDOSTACK -DOPUS_BUILD

LOCAL_C_INCLUDES := $(MY_OPUS) \
					$(MY_OPUS)/include \
					$(MY_OPUS)/celt \
					$(MY_OPUS)/silk \
					$(MY_OPUS)/silk/fixed \
					$(MY_OPUS)/silk/float

LOCAL_SRC_FILES := 	$(LOCAL_OPUS_SOURCES) \
					$(LOCAL_CELT_SOURCES) \
					$(LOCAL_SILK_SOURCES) \
					$(LOCAL_SILK_SOURCES_FIXED) \
					$(LOCAL_SILK_SOURCES_FLOAT)

LOCAL_EXPORT_C_INCLUDES := $(MY_OPUS)/include

LOCAL_MODULE := libopus

LOCAL_LDLIBS := -llog 
				
include $(BUILD_STATIC_LIBRARY)

############# libcodec_opus ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/opus.cpp \
		$(LOCAL_CODECS_PATH)/audiocodec.cpp 

LOCAL_C_INCLUDES += $(LOCAL_PATH)/.. \
			$(LOCAL_PATH)/../.. \
			$(LOCAL_PATH)/../../.. \
			$(APP_PROJECT_PATH)/jni/sflphone/daemon/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/include \

LOCAL_MODULE := libcodec_opus

LOCAL_LDLIBS := -llog 
				
LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

LOCAL_STATIC_LIBRARIES := libopus

include $(BUILD_SHARED_LIBRARY)

############# speex #################

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

LOCAL_C_INCLUDES += \
	$(MY_SPEEX)/include

include $(BUILD_STATIC_LIBRARY)


############# speexresampler #################

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES := \
	$(MY_SPEEX)/libspeex/resample.c

LOCAL_MODULE:= libspeexresampler
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS += -DEXPORT= -DFIXED_POINT -DRESAMPLE_FORCE_FULL_SINC_TABLE
LOCAL_CFLAGS += -O3 -fstrict-aliasing -fprefetch-loop-arrays

ifeq ($(ARCH_ARM_HAVE_NEON),true)
LOCAL_CFLAGS += -D_USE_NEON
endif

LOCAL_C_INCLUDES += \
	$(MY_SPEEX)/include

include $(BUILD_SHARED_LIBRARY)


############# speex_nb ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  $(LOCAL_CODECS_PATH)/speexcodec_nb.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_SRC_PATH) \
			$(LOCAL_PATH)/.. \
			$(LOCAL_PATH)/../.. \
			$(MY_SPEEX)/include/speex \
			$(MY_SPEEX)/include \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc 

LOCAL_MODULE := libcodec_speex_nb

LOCAL_LDLIBS := -llog

LOCAL_STATIC_LIBRARIES := libspeex

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)



############# speex_ub ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/speexcodec_ub.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_SRC_PATH) \
			$(LOCAL_PATH)/.. \
			$(LOCAL_PATH)/../.. \
			$(MY_SPEEX)/include/speex \
			$(MY_SPEEX)/include \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc 

LOCAL_MODULE := libcodec_speex_ub

LOCAL_LDLIBS := -llog

LOCAL_STATIC_LIBRARIES := libspeex

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)

############# speex_wb ###############

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LOCAL_CODECS_PATH)/speexcodec_wb.cpp \
					$(LOCAL_CODECS_PATH)/audiocodec.cpp

LOCAL_C_INCLUDES += $(LOCAL_SRC_PATH) \
			$(LOCAL_PATH)/.. \
			$(MY_SPEEX)/include/speex \
			$(MY_SPEEX)/include \
			$(LOCAL_PATH)/../.. \
			$(APP_PROJECT_PATH)/jni/$(MY_CCRTP)/src \
			$(APP_PROJECT_PATH)/jni/$(MY_COMMONCPP)/inc 

LOCAL_MODULE := libcodec_speex_wb

LOCAL_LDLIBS := -llog

LOCAL_STATIC_LIBRARIES := libspeex

LOCAL_CPPFLAGS += $(NETWORKMANAGER) \
				  -DCCPP_PREFIX \
				  -DCODECS_DIR=\"/usr/lib/sflphone/audio/codec\" \
				  -DPREFIX=\"$(MY_PREFIX)\" \
				  -DPROGSHAREDIR=\"${MY_DATADIR}/sflphone\" \
				  -DHAVE_CONFIG_H \
				  -std=c++11 -frtti -fpermissive -fexceptions \
				  -DAPP_NAME=\"codecfactory\"

include $(BUILD_SHARED_LIBRARY)


################# common cpp ####################

include $(CLEAR_VARS)

LT_VERSION = "0:1"
LT_RELEASE = "1.8"
SHARED_FLAGS = "-no-undefined"

LOCAL_COMMONCPP_PATH = commoncpp2-android/src

LOCAL_CPPFLAGS   += -std=c++11 -Wno-psabi -frtti -pthread -fexceptions
LOCAL_MODULE     := libccgnu2
LOCAL_LDLIBS     := -L$(SYSROOT)/usr/lib

LOCAL_C_INCLUDES += $(LOCAL_COMMONCPP_PATH)/.. \
			$(LOCAL_COMMONCPP_PATH)/../inc
		

LOCAL_SRC_FILES  := $(LOCAL_COMMONCPP_PATH)/thread.cpp \
		$(LOCAL_COMMONCPP_PATH)/mutex.cpp \
		$(LOCAL_COMMONCPP_PATH)/semaphore.cpp \
		$(LOCAL_COMMONCPP_PATH)/threadkey.cpp \
		$(LOCAL_COMMONCPP_PATH)/friends.cpp \
		$(LOCAL_COMMONCPP_PATH)/event.cpp \
		$(LOCAL_COMMONCPP_PATH)/slog.cpp \
		$(LOCAL_COMMONCPP_PATH)/dir.cpp \
		$(LOCAL_COMMONCPP_PATH)/file.cpp \
		$(LOCAL_COMMONCPP_PATH)/inaddr.cpp \
		$(LOCAL_COMMONCPP_PATH)/peer.cpp \
		$(LOCAL_COMMONCPP_PATH)/timer.cpp \
		$(LOCAL_COMMONCPP_PATH)/socket.cpp \
		$(LOCAL_COMMONCPP_PATH)/strchar.cpp \
		$(LOCAL_COMMONCPP_PATH)/simplesocket.cpp \
		$(LOCAL_COMMONCPP_PATH)/mempager.cpp \
		$(LOCAL_COMMONCPP_PATH)/keydata.cpp \
		$(LOCAL_COMMONCPP_PATH)/dso.cpp \
		$(LOCAL_COMMONCPP_PATH)/exception.cpp \
		$(LOCAL_COMMONCPP_PATH)/missing.cpp \
		$(LOCAL_COMMONCPP_PATH)/process.cpp \
		$(LOCAL_COMMONCPP_PATH)/string.cpp \
		$(LOCAL_COMMONCPP_PATH)/in6addr.cpp \
		$(LOCAL_COMMONCPP_PATH)/buffer.cpp \
		$(LOCAL_COMMONCPP_PATH)/lockfile.cpp \
		$(LOCAL_COMMONCPP_PATH)/nat.cpp \
		$(LOCAL_COMMONCPP_PATH)/runlist.cpp \
		$(LOCAL_COMMONCPP_PATH)/assoc.cpp \
		$(LOCAL_COMMONCPP_PATH)/pointer.cpp \
		$(LOCAL_COMMONCPP_PATH)/linked.cpp \
		$(LOCAL_COMMONCPP_PATH)/map.cpp \
		$(LOCAL_COMMONCPP_PATH)/cidr.cpp

#LOCAL_LDFLAGS    := -version-info $(LT_VERSION) -release $(LT_RELEASE) $(SHARED_FLAGS)

include $(BUILD_SHARED_LIBRARY)


########## libsamplerate ###################


# We need to build this for both the device (as a shared library)
# and the host (as a static library for tools to use).

# Device shared library
include $(CLEAR_VARS)

common_SRC_FILES := $(MY_LIBSAMPLE)/src/samplerate.c \
                    $(MY_LIBSAMPLE)/src/src_sinc.c \
			$(MY_LIBSAMPLE)/src/src_zoh.c \
			$(MY_LIBSAMPLE)/src/src_linear.c




LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += -Werror -g
LOCAL_LDFLAGS := 
LOCAL_C_INCLUDES += $(MY_LIBSAMPLE)/

LOCAL_MODULE:= libsamplerate

include $(BUILD_SHARED_LIBRARY)



################# libexpat ####################

include $(CLEAR_VARS)

# We need to build this for both the device (as a shared library)
# and the host (as a static library for tools to use).

common_SRC_FILES :=  \
	libexpat/xmlparse.c \
	libexpat/xmlrole.c \
	libexpat/xmltok.c

common_CFLAGS := -Wall -Wmissing-prototypes -Wstrict-prototypes -fexceptions -DHAVE_EXPAT_CONFIG_H

common_COPY_HEADERS_TO := libexpat
common_COPY_HEADERS := libexpat/ \
	libexpat/lib/expat.h \
	libexpat/lib/expat_external.h

# For the device
# =====================================================

# Device static library
include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH),arm)
LOCAL_NDK_VERSION := 9
LOCAL_SDK_VERSION := 14
endif

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += libexpat

LOCAL_MODULE:= libexpat_static
LOCAL_MODULE_FILENAME := libexpat
LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)

# Device shared library
include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH),arm)
LOCAL_NDK_VERSION := 9
LOCAL_SDK_VERSION := 14
endif

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += libexpat

LOCAL_MODULE:= libexpat_shared
LOCAL_MODULE_FILENAME := libexpat
LOCAL_MODULE_TAGS := optional
LOCAL_COPY_HEADERS_TO := $(common_COPY_HEADERS_TO)
LOCAL_COPY_HEADERS := $(common_COPY_HEADERS)

include $(BUILD_SHARED_LIBRARY)




########### ccrtp1 ####################


include $(CLEAR_VARS)

LOCAL_CCRTP1_PATH = $(MY_CCRTP)/src

LT_VERSION = 
LT_RELEASE = 
SHARED_FLAGS = "-no-undefined"
SRTP_OPENSSL =
SRTP_GCRYPT =

#LOCAL_CPPFLAGS   += -Wno-psabi -frtti -pthread -fexceptions
LOCAL_CPPFLAGS   += -std=c++11 -fexceptions
LOCAL_C_INCLUDES +=  $(LOCAL_CCRTP1_PATH) \
					$(MY_COMMONCPP)/inc \
		    		$(MY_OPENSSL)/include

LOCAL_MODULE     := libccrtp1

LOCAL_SHARED_LIBRARIES += libccgnu2 \
						  libssl_shared

LOCAL_LDLIBS     := -L$(SYSROOT)/usr/lib \
                    -L$(APP_PROJECT_PATH)/obj/local/armeabi-v7a \
					-llog

LOCAL_SHARED_LIBRARIES := 	libccgnu2 \
							libssl \
							libcrypto

LOCAL_CPP_EXTENSION := .cxx .cpp

SRTP_SRC_O = 	$(LOCAL_CCRTP1_PATH)/ccrtp/crypto/openssl/hmac.cpp \
		$(LOCAL_CCRTP1_PATH)/ccrtp/crypto/openssl/AesSrtp.cxx \
		$(LOCAL_CCRTP1_PATH)/ccrtp/crypto/openssl/InitializeOpenSSL.cxx

ifneq ($(SRTP_GCRYPT),)
SRTP_SRC_G =    $(LOCAL_CCRTP1_PATH)/ccrtp/crypto/gcrypt/gcrypthmac.cxx \
		$(LOCAL_CCRTP1_PATH)/ccrtp/crypto/gcrypt/gcryptAesSrtp.cxx \
		$(LOCAL_CCRTP1_PATH)/ccrtp/crypto/gcrypt/InitializeGcrypt.cxx
endif

SKEIN_SRCS = $(LOCAL_CCRTP1_PATH)/ccrtp/crypto/macSkein.cpp \
        $(LOCAL_CCRTP1_PATH)/ccrtp/crypto/skein.c \
        $(LOCAL_CCRTP1_PATH)/ccrtp/crypto/skein_block.c \
        $(LOCAL_CCRTP1_PATH)/ccrtp/crypto/skeinApi.c

LOCAL_SRC_FILES  := $(LOCAL_CCRTP1_PATH)/rtppkt.cpp \
			$(LOCAL_CCRTP1_PATH)/rtcppkt.cpp \
			$(LOCAL_CCRTP1_PATH)/source.cpp \
			$(LOCAL_CCRTP1_PATH)/data.cpp \
			$(LOCAL_CCRTP1_PATH)/incqueue.cpp \
			$(LOCAL_CCRTP1_PATH)/outqueue.cpp \
			$(LOCAL_CCRTP1_PATH)/queue.cpp \
			$(LOCAL_CCRTP1_PATH)/control.cpp \
			$(LOCAL_CCRTP1_PATH)/members.cpp \
			$(LOCAL_CCRTP1_PATH)/socket.cpp \
			$(LOCAL_CCRTP1_PATH)/duplex.cpp $(LOCAL_CCRTP1_PATH)/pool.cpp \
			$(LOCAL_CCRTP1_PATH)/CryptoContext.cxx $(SRTP_SRC_G) $(SRTP_SRC_O) $(SKEIN_SRCS)


#LOCAL_LDFLAGS    := -version-info $(LT_VERSION) -release $(LT_RELEASE) $(SHARED_FLAGS)

include $(BUILD_SHARED_LIBRARY)

############### libpcre ##################

include $(CLEAR_VARS)

LOCAL_MODULE := libpcre
LOCAL_CFLAGS := -DHAVE_CONFIG_H

LOCAL_SRC_FILES :=  \
  libpcre/pcre_compile.c \
  libpcre/pcre_chartables.c \
  libpcre/pcre_config.c \
  libpcre/pcre_dfa_exec.c \
  libpcre/pcre_exec.c \
  libpcre/pcre_fullinfo.c \
  libpcre/pcre_get.c \
  libpcre/pcre_globals.c \
  libpcre/pcre_info.c \
  libpcre/pcre_maketables.c \
  libpcre/pcre_newline.c \
  libpcre/pcre_ord2utf8.c \
  libpcre/pcre_refcount.c \
  libpcre/pcre_study.c \
  libpcre/pcre_tables.c \
  libpcre/pcre_try_flipped.c \
  libpcre/pcre_ucd.c \
  libpcre/pcre_valid_utf8.c \
  libpcre/pcre_version.c \
  libpcre/pcre_xclass.c

include $(BUILD_SHARED_LIBRARY)

############### libyaml ##################

include $(CLEAR_VARS)

LOCAL_CFLAGS := -DYAML_VERSION_STRING=\"0.1.4\" \
				-DYAML_VERSION_MAJOR=0 \
				-DYAML_VERSION_MINOR=1 \
				-DYAML_VERSION_PATCH=4
LOCAL_MODULE     := libyaml
LOCAL_LDLIBS     := -L$(SYSROOT)/usr/lib
LOCAL_SRC_FILES  := libyaml/api.c libyaml/reader.c libyaml/scanner.c \
                    libyaml/parser.c libyaml/loader.c libyaml/writer.c libyaml/emitter.c libyaml/dumper.c
LOCAL_C_INCLUDES += libyaml/inc

include $(BUILD_SHARED_LIBRARY)

############### openssl-apps ###################
include $(CLEAR_VARS)
# Copyright 2006 The Android Open Source Project

LOCAL_APP_OPENSSL = openssl/apps

local_src_files:= $(LOCAL_APP_OPENSSL)/app_rand.c \
	$(LOCAL_APP_OPENSSL)/apps.c \
	$(LOCAL_APP_OPENSSL)/asn1pars.c \
	$(LOCAL_APP_OPENSSL)/ca.c \
	$(LOCAL_APP_OPENSSL)/ciphers.c \
	$(LOCAL_APP_OPENSSL)/crl.c \
	$(LOCAL_APP_OPENSSL)/crl2p7.c \
	$(LOCAL_APP_OPENSSL)/dgst.c \
	$(LOCAL_APP_OPENSSL)/dh.c \
	$(LOCAL_APP_OPENSSL)/dhparam.c \
	$(LOCAL_APP_OPENSSL)/dsa.c \
	$(LOCAL_APP_OPENSSL)/dsaparam.c \
	$(LOCAL_APP_OPENSSL)/ecparam.c \
	$(LOCAL_APP_OPENSSL)/ec.c \
	$(LOCAL_APP_OPENSSL)/enc.c \
	$(LOCAL_APP_OPENSSL)/engine.c \
	$(LOCAL_APP_OPENSSL)/errstr.c \
	$(LOCAL_APP_OPENSSL)/gendh.c \
	$(LOCAL_APP_OPENSSL)/gendsa.c \
	$(LOCAL_APP_OPENSSL)/genpkey.c \
	$(LOCAL_APP_OPENSSL)/genrsa.c \
	$(LOCAL_APP_OPENSSL)/nseq.c \
	$(LOCAL_APP_OPENSSL)/ocsp.c \
	$(LOCAL_APP_OPENSSL)/openssl.c \
	$(LOCAL_APP_OPENSSL)/passwd.c \
	$(LOCAL_APP_OPENSSL)/pkcs12.c \
	$(LOCAL_APP_OPENSSL)/pkcs7.c \
	$(LOCAL_APP_OPENSSL)/pkcs8.c \
	$(LOCAL_APP_OPENSSL)/pkey.c \
	$(LOCAL_APP_OPENSSL)/pkeyparam.c \
	$(LOCAL_APP_OPENSSL)/pkeyutl.c \
	$(LOCAL_APP_OPENSSL)/prime.c \
	$(LOCAL_APP_OPENSSL)/rand.c \
	$(LOCAL_APP_OPENSSL)/req.c \
	$(LOCAL_APP_OPENSSL)/rsa.c \
	$(LOCAL_APP_OPENSSL)/rsautl.c \
	$(LOCAL_APP_OPENSSL)/s_cb.c \
	$(LOCAL_APP_OPENSSL)/s_client.c \
	$(LOCAL_APP_OPENSSL)/s_server.c \
	$(LOCAL_APP_OPENSSL)/s_socket.c \
	$(LOCAL_APP_OPENSSL)/s_time.c \
	$(LOCAL_APP_OPENSSL)/sess_id.c \
	$(LOCAL_APP_OPENSSL)/smime.c \
	$(LOCAL_APP_OPENSSL)/speed.c \
	$(LOCAL_APP_OPENSSL)/spkac.c \
	$(LOCAL_APP_OPENSSL)/verify.c \
	$(LOCAL_APP_OPENSSL)/version.c \
	$(LOCAL_APP_OPENSSL)/x509.c

LOCAL_SHARED_LIBRARIES := 	libssl \
							libcrypto

local_c_includes := \
        $(LOCAL_APP_OPENSSL)/.. \
        $(LOCAL_APP_OPENSSL)/../include \
	external/openssl \
	external/openssl/include

local_cflags := -DMONOLITH

# These flags omit whole features from the commandline "openssl".
# However, portions of these features are actually turned on.
local_cflags += -DOPENSSL_NO_DTLS1

include $(CLEAR_VARS)
LOCAL_MODULE:= openssl
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(local_src_files)
LOCAL_SHARED_LIBRARIES := $(local_shared_libraries)
LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_CFLAGS := $(local_cflags)
include $(LOCAL_APP_OPENSSL)/../android-config.mk
include $(BUILD_EXECUTABLE)

#include $(CLEAR_VARS)
#LOCAL_MODULE:= openssl
#LOCAL_MODULE_TAGS := optional
#LOCAL_SRC_FILES := $(local_src_files)
#LOCAL_SHARED_LIBRARIES := $(local_shared_libraries)
#LOCAL_C_INCLUDES := $(local_c_includes)
#LOCAL_CFLAGS := $(local_cflags)
#include $(LOCAL_PATH)/../android-config.mk
#include $(BUILD_HOST_EXECUTABLE)



############ openssl-crypto ###################

include $(CLEAR_VARS)

LOCAL_CRYPTO_OPENSSL = openssl/crypto

arm_cflags := -DOPENSSL_BN_ASM_MONT -DAES_ASM -DSHA1_ASM -DSHA256_ASM -DSHA512_ASM
arm_src_files := \
    $(LOCAL_CRYPTO_OPENSSL)/aes/asm/aes-armv4.s \
    $(LOCAL_CRYPTO_OPENSSL)/bn/asm/armv4-mont.s \
    $(LOCAL_CRYPTO_OPENSSL)/sha/asm/sha1-armv4-large.s \
    $(LOCAL_CRYPTO_OPENSSL)/sha/asm/sha256-armv4.s \
    $(LOCAL_CRYPTO_OPENSSL)/sha/asm/sha512-armv4.s
non_arm_src_files := $(LOCAL_CRYPTO_OPENSSL)/aes/aes_core.c

local_src_files := \
	$(LOCAL_CRYPTO_OPENSSL)/cryptlib.c \
	$(LOCAL_CRYPTO_OPENSSL)/mem.c \
	$(LOCAL_CRYPTO_OPENSSL)/mem_clr.c \
	$(LOCAL_CRYPTO_OPENSSL)/mem_dbg.c \
	$(LOCAL_CRYPTO_OPENSSL)/cversion.c \
	$(LOCAL_CRYPTO_OPENSSL)/ex_data.c \
	$(LOCAL_CRYPTO_OPENSSL)/cpt_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/ebcdic.c \
	$(LOCAL_CRYPTO_OPENSSL)/uid.c \
	$(LOCAL_CRYPTO_OPENSSL)/o_time.c \
	$(LOCAL_CRYPTO_OPENSSL)/o_str.c \
	$(LOCAL_CRYPTO_OPENSSL)/o_dir.c \
	$(LOCAL_CRYPTO_OPENSSL)/aes/aes_cbc.c \
	$(LOCAL_CRYPTO_OPENSSL)/aes/aes_cfb.c \
	$(LOCAL_CRYPTO_OPENSSL)/aes/aes_ctr.c \
	$(LOCAL_CRYPTO_OPENSSL)/aes/aes_ecb.c \
	$(LOCAL_CRYPTO_OPENSSL)/aes/aes_misc.c \
	$(LOCAL_CRYPTO_OPENSSL)/aes/aes_ofb.c \
	$(LOCAL_CRYPTO_OPENSSL)/aes/aes_wrap.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_bitstr.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_bool.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_bytes.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_d2i_fp.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_digest.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_dup.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_enum.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_gentm.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_i2d_fp.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_int.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_mbstr.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_object.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_octet.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_print.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_set.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_sign.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_strex.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_strnid.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_time.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_type.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_utctm.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_utf8.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/a_verify.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/ameth_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/asn1_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/asn1_gen.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/asn1_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/asn1_par.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/asn_mime.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/asn_moid.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/asn_pack.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/bio_asn1.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/bio_ndef.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/d2i_pr.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/d2i_pu.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/evp_asn1.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/f_enum.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/f_int.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/f_string.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/i2d_pr.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/i2d_pu.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/n_pkey.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/nsseq.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/p5_pbe.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/p5_pbev2.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/p8_pkey.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/t_bitst.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/t_crl.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/t_pkey.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/t_req.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/t_spki.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/t_x509.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/t_x509a.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/tasn_dec.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/tasn_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/tasn_fre.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/tasn_new.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/tasn_prn.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/tasn_typ.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/tasn_utl.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_algor.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_attrib.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_bignum.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_crl.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_exten.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_info.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_long.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_name.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_nx509.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_pkey.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_pubkey.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_req.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_sig.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_spki.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_val.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_x509.c \
	$(LOCAL_CRYPTO_OPENSSL)/asn1/x_x509a.c \
	$(LOCAL_CRYPTO_OPENSSL)/bf/bf_cfb64.c \
	$(LOCAL_CRYPTO_OPENSSL)/bf/bf_ecb.c \
	$(LOCAL_CRYPTO_OPENSSL)/bf/bf_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/bf/bf_ofb64.c \
	$(LOCAL_CRYPTO_OPENSSL)/bf/bf_skey.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/b_dump.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/b_print.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/b_sock.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bf_buff.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bf_nbio.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bf_null.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bio_cb.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bio_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bio_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_acpt.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_bio.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_conn.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_dgram.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_fd.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_file.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_log.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_mem.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_null.c \
	$(LOCAL_CRYPTO_OPENSSL)/bio/bss_sock.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_add.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_asm.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_blind.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_const.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_ctx.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_div.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_exp.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_exp2.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_gcd.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_gf2m.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_kron.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_mod.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_mont.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_mpi.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_mul.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_nist.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_prime.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_print.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_rand.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_recp.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_shift.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_sqr.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_sqrt.c \
	$(LOCAL_CRYPTO_OPENSSL)/bn/bn_word.c \
	$(LOCAL_CRYPTO_OPENSSL)/buffer/buf_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/buffer/buffer.c \
	$(LOCAL_CRYPTO_OPENSSL)/comp/c_rle.c \
	$(LOCAL_CRYPTO_OPENSSL)/comp/c_zlib.c \
	$(LOCAL_CRYPTO_OPENSSL)/comp/comp_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/comp/comp_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/conf/conf_api.c \
	$(LOCAL_CRYPTO_OPENSSL)/conf/conf_def.c \
	$(LOCAL_CRYPTO_OPENSSL)/conf/conf_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/conf/conf_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/conf/conf_mall.c \
	$(LOCAL_CRYPTO_OPENSSL)/conf/conf_mod.c \
	$(LOCAL_CRYPTO_OPENSSL)/conf/conf_sap.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/cbc_cksm.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/cbc_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/cfb64ede.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/cfb64enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/cfb_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/des_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/des_old.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/des_old2.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/ecb3_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/ecb_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/ede_cbcm_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/enc_read.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/enc_writ.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/fcrypt.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/fcrypt_b.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/ofb64ede.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/ofb64enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/ofb_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/pcbc_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/qud_cksm.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/rand_key.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/read2pwd.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/rpc_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/set_key.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/str2key.c \
	$(LOCAL_CRYPTO_OPENSSL)/des/xcbc_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_ameth.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_asn1.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_check.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_depr.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_gen.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_key.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/dh/dh_pmeth.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_ameth.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_asn1.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_depr.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_gen.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_key.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_ossl.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_pmeth.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_prn.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_sign.c \
	$(LOCAL_CRYPTO_OPENSSL)/dsa/dsa_vrf.c \
	$(LOCAL_CRYPTO_OPENSSL)/dso/dso_dl.c \
	$(LOCAL_CRYPTO_OPENSSL)/dso/dso_dlfcn.c \
	$(LOCAL_CRYPTO_OPENSSL)/dso/dso_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/dso/dso_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/dso/dso_null.c \
	$(LOCAL_CRYPTO_OPENSSL)/dso/dso_openssl.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec2_mult.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec2_smpl.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_ameth.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_asn1.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_check.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_curve.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_cvt.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_key.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_mult.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_pmeth.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ec_print.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/eck_prn.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ecp_mont.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ecp_nist.c \
	$(LOCAL_CRYPTO_OPENSSL)/ec/ecp_smpl.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdh/ech_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdh/ech_key.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdh/ech_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdh/ech_ossl.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdsa/ecs_asn1.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdsa/ecs_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdsa/ecs_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdsa/ecs_ossl.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdsa/ecs_sign.c \
	$(LOCAL_CRYPTO_OPENSSL)/ecdsa/ecs_vrf.c \
	$(LOCAL_CRYPTO_OPENSSL)/err/err.c \
	$(LOCAL_CRYPTO_OPENSSL)/err/err_all.c \
	$(LOCAL_CRYPTO_OPENSSL)/err/err_prn.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/bio_b64.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/bio_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/bio_md.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/bio_ok.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/c_all.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/c_allc.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/c_alld.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/digest.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_aes.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_bf.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_des.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_des3.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_null.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_old.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_rc2.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_rc4.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_rc5.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/e_xcbc_d.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/encode.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/evp_acnf.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/evp_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/evp_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/evp_key.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/evp_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/evp_pbe.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/evp_pkey.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_dss.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_dss1.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_ecdsa.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_md4.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_md5.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_mdc2.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_null.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_ripemd.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_sha1.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_sigver.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/m_wp.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/names.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p5_crpt.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p5_crpt2.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p_dec.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p_open.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p_seal.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p_sign.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/p_verify.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/pmeth_fn.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/pmeth_gn.c \
	$(LOCAL_CRYPTO_OPENSSL)/evp/pmeth_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/hmac/hm_ameth.c \
	$(LOCAL_CRYPTO_OPENSSL)/hmac/hm_pmeth.c \
	$(LOCAL_CRYPTO_OPENSSL)/hmac/hmac.c \
	$(LOCAL_CRYPTO_OPENSSL)/krb5/krb5_asn.c \
	$(LOCAL_CRYPTO_OPENSSL)/lhash/lh_stats.c \
	$(LOCAL_CRYPTO_OPENSSL)/lhash/lhash.c \
	$(LOCAL_CRYPTO_OPENSSL)/md4/md4_dgst.c \
	$(LOCAL_CRYPTO_OPENSSL)/md4/md4_one.c \
	$(LOCAL_CRYPTO_OPENSSL)/md5/md5_dgst.c \
	$(LOCAL_CRYPTO_OPENSSL)/md5/md5_one.c \
	$(LOCAL_CRYPTO_OPENSSL)/modes/cbc128.c \
	$(LOCAL_CRYPTO_OPENSSL)/modes/cfb128.c \
	$(LOCAL_CRYPTO_OPENSSL)/modes/ctr128.c \
	$(LOCAL_CRYPTO_OPENSSL)/modes/ofb128.c \
	$(LOCAL_CRYPTO_OPENSSL)/objects/o_names.c \
	$(LOCAL_CRYPTO_OPENSSL)/objects/obj_dat.c \
	$(LOCAL_CRYPTO_OPENSSL)/objects/obj_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/objects/obj_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/objects/obj_xref.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_asn.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_cl.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_ext.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_ht.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_prn.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_srv.c \
	$(LOCAL_CRYPTO_OPENSSL)/ocsp/ocsp_vfy.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_all.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_info.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_oth.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_pk8.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_pkey.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_seal.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_sign.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_x509.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pem_xaux.c \
	$(LOCAL_CRYPTO_OPENSSL)/pem/pvkfmt.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_add.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_asn.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_attr.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_crpt.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_crt.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_decr.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_init.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_key.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_kiss.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_mutl.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_npas.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_p8d.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_p8e.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/p12_utl.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs12/pk12err.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs7/pk7_asn1.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs7/pk7_attr.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs7/pk7_doit.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs7/pk7_lib.c	\
	$(LOCAL_CRYPTO_OPENSSL)/pkcs7/pk7_mime.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs7/pk7_smime.c \
	$(LOCAL_CRYPTO_OPENSSL)/pkcs7/pkcs7err.c \
	$(LOCAL_CRYPTO_OPENSSL)/rand/md_rand.c \
	$(LOCAL_CRYPTO_OPENSSL)/rand/rand_egd.c \
	$(LOCAL_CRYPTO_OPENSSL)/rand/rand_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/rand/rand_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/rand/rand_unix.c \
	$(LOCAL_CRYPTO_OPENSSL)/rand/randfile.c \
	$(LOCAL_CRYPTO_OPENSSL)/rc2/rc2_cbc.c \
	$(LOCAL_CRYPTO_OPENSSL)/rc2/rc2_ecb.c \
	$(LOCAL_CRYPTO_OPENSSL)/rc2/rc2_skey.c \
	$(LOCAL_CRYPTO_OPENSSL)/rc2/rc2cfb64.c \
	$(LOCAL_CRYPTO_OPENSSL)/rc2/rc2ofb64.c \
	$(LOCAL_CRYPTO_OPENSSL)/rc4/rc4_enc.c \
	$(LOCAL_CRYPTO_OPENSSL)/rc4/rc4_skey.c \
	$(LOCAL_CRYPTO_OPENSSL)/ripemd/rmd_dgst.c \
	$(LOCAL_CRYPTO_OPENSSL)/ripemd/rmd_one.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_ameth.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_asn1.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_chk.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_eay.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_gen.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_none.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_null.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_oaep.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_pk1.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_pmeth.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_prn.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_pss.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_saos.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_sign.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_ssl.c \
	$(LOCAL_CRYPTO_OPENSSL)/rsa/rsa_x931.c \
	$(LOCAL_CRYPTO_OPENSSL)/sha/sha1_one.c \
	$(LOCAL_CRYPTO_OPENSSL)/sha/sha1dgst.c \
	$(LOCAL_CRYPTO_OPENSSL)/sha/sha256.c \
	$(LOCAL_CRYPTO_OPENSSL)/sha/sha512.c \
	$(LOCAL_CRYPTO_OPENSSL)/sha/sha_dgst.c \
	$(LOCAL_CRYPTO_OPENSSL)/stack/stack.c \
	$(LOCAL_CRYPTO_OPENSSL)/ts/ts_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/txt_db/txt_db.c \
	$(LOCAL_CRYPTO_OPENSSL)/ui/ui_compat.c \
	$(LOCAL_CRYPTO_OPENSSL)/ui/ui_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/ui/ui_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/ui/ui_openssl.c \
	$(LOCAL_CRYPTO_OPENSSL)/ui/ui_util.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/by_dir.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/by_file.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_att.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_cmp.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_d2.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_def.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_err.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_ext.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_lu.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_obj.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_r2x.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_req.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_set.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_trs.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_txt.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_v3.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_vfy.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509_vpm.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509cset.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509name.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509rset.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509spki.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x509type.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509/x_all.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/pcy_cache.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/pcy_data.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/pcy_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/pcy_map.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/pcy_node.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/pcy_tree.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_akey.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_akeya.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_alt.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_bcons.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_bitst.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_conf.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_cpols.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_crld.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_enum.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_extku.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_genn.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_ia5.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_info.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_int.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_lib.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_ncons.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_ocsp.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_pci.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_pcia.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_pcons.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_pku.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_pmaps.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_prn.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_purp.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_skey.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_sxnet.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3_utl.c \
	$(LOCAL_CRYPTO_OPENSSL)/x509v3/v3err.c

local_c_includes := $(LOCAL_CRYPTO_OPENSSL)/.. \
$(LOCAL_CRYPTO_OPENSSL) \
        $(LOCAL_CRYPTO_OPENSSL)/asn1 \
	$(LOCAL_CRYPTO_OPENSSL)/evp \
	$(LOCAL_CRYPTO_OPENSSL)/../include \
	$(LOCAL_CRYPTO_OPENSSL)/../include/openssl \
	external/openssl \
	external/openssl/crypto/asn1 \
	external/openssl/crypto/evp \
	external/openssl/include \
	external/openssl/include/openssl \
	external/zlib

local_c_flags := -DNO_WINDOWS_BRAINDEATH

#######################################
# target static library
include $(CLEAR_VARS)
include $(LOCAL_CRYPTO_OPENSSL)/../android-config.mk

ifneq ($(TARGET_ARCH),x86)
LOCAL_NDK_VERSION := 9
LOCAL_SDK_VERSION := 14
endif

LOCAL_SRC_FILES += $(local_src_files)
LOCAL_CFLAGS += $(local_c_flags)
LOCAL_C_INCLUDES += $(local_c_includes)
ifeq ($(TARGET_ARCH),arm)
	LOCAL_SRC_FILES += $(arm_src_files)
	LOCAL_CFLAGS += $(arm_cflags)
else
	LOCAL_SRC_FILES += $(non_arm_src_files)
endif
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libcrypto_static
include $(BUILD_STATIC_LIBRARY)

#######################################
# target shared library
include $(CLEAR_VARS)
include $(LOCAL_CRYPTO_OPENSSL)/../android-config.mk

ifneq ($(TARGET_ARCH),x86)
LOCAL_NDK_VERSION := 9
LOCAL_SDK_VERSION := 14
# Use the NDK prebuilt libz and libdl.
LOCAL_LDFLAGS += -lz -ldl
else
LOCAL_SHARED_LIBRARIES += libz libdl
endif

LOCAL_SRC_FILES += $(local_src_files)
LOCAL_CFLAGS += $(local_c_flags)
LOCAL_C_INCLUDES += $(local_c_includes)
ifeq ($(TARGET_ARCH),arm)
	LOCAL_SRC_FILES += $(arm_src_files)
	LOCAL_CFLAGS += $(arm_cflags)
else
	LOCAL_SRC_FILES += $(non_arm_src_files)
endif
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libcrypto
include $(BUILD_SHARED_LIBRARY)

#######################################
# host shared library
#include $(CLEAR_VARS)
#include $(LOCAL_PATH)/../android-config.mk
#LOCAL_SRC_FILES += $(local_src_files)
#LOCAL_CFLAGS += $(local_c_flags) -DPURIFY
#LOCAL_C_INCLUDES += $(local_c_includes)
#LOCAL_SRC_FILES += $(non_arm_src_files)
#LOCAL_STATIC_LIBRARIES += libz
#LOCAL_LDLIBS += -ldl
#LOCAL_MODULE_TAGS := optional
#LOCAL_MODULE:= libcrypto
#include $(BUILD_HOST_SHARED_LIBRARY)

########################################
# host static library, which is used by some SDK tools.
#
#include $(CLEAR_VARS)
#include $(LOCAL_PATH)/../android-config.mk
#LOCAL_SRC_FILES += $(local_src_files)
#LOCAL_CFLAGS += $(local_c_flags) -DPURIFY
#LOCAL_C_INCLUDES += $(local_c_includes)
#LOCAL_SRC_FILES += $(non_arm_src_files)
#LOCAL_STATIC_LIBRARIES += libz
#LOCAL_LDLIBS += -ldl
#LOCAL_MODULE_TAGS := optional
#LOCAL_MODULE:= libcrypto_static
#include $(BUILD_HOST_STATIC_LIBRARY)




############# libssl ##################

include $(CLEAR_VARS)
LOCAL_SSL_PATH = openssl/ssl

local_c_includes := \
	$(LOCAL_SSL_PATH)/..\
	$(LOCAL_SSL_PATH)/../include \
	$(LOCAL_SSL_PATH)/../crypto \
	external/openssl \
	external/openssl/include \
	external/openssl/crypto

local_src_files:= \
	$(LOCAL_SSL_PATH)/s2_meth.c \
	$(LOCAL_SSL_PATH)/s2_srvr.c \
	$(LOCAL_SSL_PATH)/s2_clnt.c \
	$(LOCAL_SSL_PATH)/s2_lib.c \
	$(LOCAL_SSL_PATH)/s2_enc.c \
	$(LOCAL_SSL_PATH)/s2_pkt.c \
	$(LOCAL_SSL_PATH)/s3_meth.c \
	$(LOCAL_SSL_PATH)/s3_srvr.c \
	$(LOCAL_SSL_PATH)/s3_clnt.c \
	$(LOCAL_SSL_PATH)/s3_lib.c \
	$(LOCAL_SSL_PATH)/s3_enc.c \
	$(LOCAL_SSL_PATH)/s3_pkt.c \
	$(LOCAL_SSL_PATH)/s3_both.c \
	$(LOCAL_SSL_PATH)/s23_meth.c \
	$(LOCAL_SSL_PATH)/s23_srvr.c \
	$(LOCAL_SSL_PATH)/s23_clnt.c \
	$(LOCAL_SSL_PATH)/s23_lib.c \
	$(LOCAL_SSL_PATH)/s23_pkt.c \
	$(LOCAL_SSL_PATH)/t1_meth.c \
	$(LOCAL_SSL_PATH)/t1_srvr.c \
	$(LOCAL_SSL_PATH)/t1_clnt.c \
	$(LOCAL_SSL_PATH)/t1_lib.c \
	$(LOCAL_SSL_PATH)/t1_enc.c \
	$(LOCAL_SSL_PATH)/t1_reneg.c \
	$(LOCAL_SSL_PATH)/ssl_lib.c \
	$(LOCAL_SSL_PATH)/ssl_err2.c \
	$(LOCAL_SSL_PATH)/ssl_cert.c \
	$(LOCAL_SSL_PATH)/ssl_sess.c \
	$(LOCAL_SSL_PATH)/ssl_ciph.c \
	$(LOCAL_SSL_PATH)/ssl_stat.c \
	$(LOCAL_SSL_PATH)/ssl_rsa.c \
	$(LOCAL_SSL_PATH)/ssl_asn1.c \
	$(LOCAL_SSL_PATH)/ssl_txt.c \
	$(LOCAL_SSL_PATH)/ssl_algs.c \
	$(LOCAL_SSL_PATH)/bio_ssl.c \
	$(LOCAL_SSL_PATH)/ssl_err.c \
	$(LOCAL_SSL_PATH)/kssl.c

#######################################
# target static library
include $(CLEAR_VARS)
include $(LOCAL_SSL_PATH)/../android-config.mk

ifneq ($(TARGET_ARCH),x86)
LOCAL_NDK_VERSION := 9
LOCAL_SDK_VERSION := 14
endif
LOCAL_SRC_FILES += $(local_src_files)
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libssl_static
include $(BUILD_STATIC_LIBRARY)

#######################################
# target shared library
include $(CLEAR_VARS)
include $(LOCAL_SSL_PATH)/../android-config.mk

ifneq ($(TARGET_ARCH),x86)
LOCAL_NDK_VERSION := 9
LOCAL_SDK_VERSION := 14
endif
LOCAL_SRC_FILES += $(local_src_files)
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_SHARED_LIBRARIES += libcrypto
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libssl_shared
LOCAL_MODULE_FILENAME := libssl
include $(BUILD_SHARED_LIBRARY)

#######################################
# host shared library
include $(CLEAR_VARS)
include $(LOCAL_SSL_PATH)/../android-config.mk
LOCAL_SRC_FILES += $(local_src_files)
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_SHARED_LIBRARIES += libcrypto
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libssl
include $(BUILD_HOST_SHARED_LIBRARY)

#######################################
# ssltest
include $(CLEAR_VARS)
include $(LOCAL_SSL_PATH)/../android-config.mk
LOCAL_SRC_FILES:= ssltest.c
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_SHARED_LIBRARIES := libssl libcrypto
LOCAL_MODULE:= ssltest
LOCAL_MODULE_TAGS := optional
include $(BUILD_EXECUTABLE)



