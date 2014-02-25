include $(CLEAR_VARS)

MY_OPUS = libopus/sources

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
						$(MY_OPUS)/celt/pitch.c \
						$(MY_OPUS)/celt/celt_decoder.c \
						$(MY_OPUS)/celt/celt_encoder.c \
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
						$(MY_OPUS)/src/mlp.c \
						$(MY_OPUS)/src/mlp_data.c \
						$(MY_OPUS)/src/analysis.c \
						$(MY_OPUS)/src/opus_decoder.c \
						$(MY_OPUS)/src/repacketizer.c \
						$(MY_OPUS)/src/opus_multistream.c \
						$(MY_OPUS)/src/opus_encoder.c \
						$(MY_OPUS)/src/opus_compare.c \

LOCAL_CFLAGS := -DUSE_ALLOCA -DOPUS_BUILD -D__OPTIMIZE__

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_CFLAGS += -DOPUS_ARM_INLINE_NEON
endif

LOCAL_C_INCLUDES := $(MY_OPUS) \
					$(MY_OPUS)/include \
					$(MY_OPUS)/celt \
					$(MY_OPUS)/silk \
					$(MY_OPUS)/silk/fixed \
					$(MY_OPUS)/silk/float

LOCAL_SRC_FILES := 	$(LOCAL_OPUS_SOURCES) \
					$(LOCAL_CELT_SOURCES) \
					$(LOCAL_SILK_SOURCES) \
					$(LOCAL_SILK_SOURCES_FLOAT)

LOCAL_EXPORT_C_INCLUDES := $(MY_OPUS)/include

LOCAL_MODULE := libopus

include $(BUILD_STATIC_LIBRARY)
