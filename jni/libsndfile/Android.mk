include $(CLEAR_VARS)

MY_LIBSNDFILE = libsndfile/sources

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

LOCAL_C_INCLUDES += $(MY_LIBSNDFILE)/src \
					$(APP_PROJECT_PATH)/jni/sflphone/daemon/src

include $(BUILD_STATIC_LIBRARY)
