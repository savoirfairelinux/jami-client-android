#include "logger.h"

JavaVM *gJavaVM;
const char *ksflphoneservicePath = "cx/ring/service/RingserviceJNI";

void deinitClassHelper(JNIEnv *env, jobject obj) {
	SFL_INFO("deinitClassHelper");

	/* delete cached object instances */
    env->DeleteGlobalRef(obj);
	SFL_INFO("deinitClassHelper: object %x deleted", obj);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	JNIEnv *env;
	jclass clazz;
	jint r;

    SFL_INFO("JNI_OnLoad");

	//Assume it is c++
	r = vm->GetEnv ((void **) &env, JNI_VERSION_1_6);
    if (r != JNI_OK) {
		RING_ERR("JNI_OnLoad: failed to get the environment using GetEnv()");
        return -1;
    }
	SFL_INFO("JNI_Onload: GetEnv %p", env);

	clazz = env->FindClass (ksflphoneservicePath);
	if (!clazz) {
        RING_ERR("JNI_Onload: whoops, %s class not found!", ksflphoneservicePath);
	}
	gJavaVM = vm;
	SFL_INFO("JNI_Onload: JavaVM %p", gJavaVM);

	/* put instances of class object we need into cache */
    //initClassHelper(env, kManagerPath, &gManagerObject);

	JNINativeMethod methods[] = {

	{"new_StringMap__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1StringMap_1_1SWIG_10},
{"new_StringMap__SWIG_1", "(JLorg/sflphone/service/StringMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1StringMap_1_1SWIG_11},
{"StringMap_size", "(JLorg/sflphone/service/StringMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1size},
{"StringMap_empty", "(JLorg/sflphone/service/StringMap;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1empty},
{"StringMap_clear", "(JLorg/sflphone/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1clear},
{"StringMap_get", "(JLorg/sflphone/service/StringMap;Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1get},
{"StringMap_set", "(JLorg/sflphone/service/StringMap;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1set},
{"StringMap_del", "(JLorg/sflphone/service/StringMap;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1del},
{"StringMap_has_key", "(JLorg/sflphone/service/StringMap;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1has_1key},
{"delete_StringMap", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1StringMap},
{"new_StringVect__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1StringVect_1_1SWIG_10},
{"new_StringVect__SWIG_1", "(J)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1StringVect_1_1SWIG_11},
{"StringVect_size", "(JLorg/sflphone/service/StringVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1size},
{"StringVect_capacity", "(JLorg/sflphone/service/StringVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1capacity},
{"StringVect_reserve", "(JLorg/sflphone/service/StringVect;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1reserve},
{"StringVect_isEmpty", "(JLorg/sflphone/service/StringVect;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1isEmpty},
{"StringVect_clear", "(JLorg/sflphone/service/StringVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1clear},
{"StringVect_add", "(JLorg/sflphone/service/StringVect;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1add},
{"StringVect_get", "(JLorg/sflphone/service/StringVect;I)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1get},
{"StringVect_set", "(JLorg/sflphone/service/StringVect;ILjava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1set},
{"delete_StringVect", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1StringVect},
{"new_VectMap__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1VectMap_1_1SWIG_10},
{"new_VectMap__SWIG_1", "(J)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1VectMap_1_1SWIG_11},
{"VectMap_size", "(JLorg/sflphone/service/VectMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1size},
{"VectMap_capacity", "(JLorg/sflphone/service/VectMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1capacity},
{"VectMap_reserve", "(JLorg/sflphone/service/VectMap;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1reserve},
{"VectMap_isEmpty", "(JLorg/sflphone/service/VectMap;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1isEmpty},
{"VectMap_clear", "(JLorg/sflphone/service/VectMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1clear},
{"VectMap_add", "(JLorg/sflphone/service/VectMap;JLorg/sflphone/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1add},
{"VectMap_get", "(JLorg/sflphone/service/VectMap;I)J", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1get},
{"VectMap_set", "(JLorg/sflphone/service/VectMap;IJLorg/sflphone/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1set},
{"delete_VectMap", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1VectMap},
{"new_IntegerMap__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1IntegerMap_1_1SWIG_10},
{"new_IntegerMap__SWIG_1", "(JLorg/sflphone/service/IntegerMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1IntegerMap_1_1SWIG_11},
{"IntegerMap_size", "(JLorg/sflphone/service/IntegerMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1size},
{"IntegerMap_empty", "(JLorg/sflphone/service/IntegerMap;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1empty},
{"IntegerMap_clear", "(JLorg/sflphone/service/IntegerMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1clear},
{"IntegerMap_get", "(JLorg/sflphone/service/IntegerMap;Ljava/lang/String;)I", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1get},
{"IntegerMap_set", "(JLorg/sflphone/service/IntegerMap;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1set},
{"IntegerMap_del", "(JLorg/sflphone/service/IntegerMap;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1del},
{"IntegerMap_has_key", "(JLorg/sflphone/service/IntegerMap;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1has_1key},
{"delete_IntegerMap", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1IntegerMap},
{"new_IntVect__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1IntVect_1_1SWIG_10},
{"new_IntVect__SWIG_1", "(J)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1IntVect_1_1SWIG_11},
{"IntVect_size", "(JLorg/sflphone/service/IntVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1size},
{"IntVect_capacity", "(JLorg/sflphone/service/IntVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1capacity},
{"IntVect_reserve", "(JLorg/sflphone/service/IntVect;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1reserve},
{"IntVect_isEmpty", "(JLorg/sflphone/service/IntVect;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1isEmpty},
{"IntVect_clear", "(JLorg/sflphone/service/IntVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1clear},
{"IntVect_add", "(JLorg/sflphone/service/IntVect;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1add},
{"IntVect_get", "(JLorg/sflphone/service/IntVect;I)I", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1get},
{"IntVect_set", "(JLorg/sflphone/service/IntVect;II)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1set},
{"delete_IntVect", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1IntVect},
{"sflph_fini", "()V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1fini},
{"sflph_poll_events", "()V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1poll_1events},
{"sflph_call_place", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1place},
{"sflph_call_refuse", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1refuse},
{"sflph_call_accept", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1accept},
{"sflph_call_hang_up", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1hang_1up},
{"sflph_call_hold", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1hold},
{"sflph_call_unhold", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1unhold},
{"sflph_call_transfer", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1transfer},
{"sflph_call_attended_transfer", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1attended_1transfer},
{"sflph_call_get_call_details", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1get_1call_1details},
{"sflph_call_get_call_list", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1get_1call_1list},
{"sflph_call_remove_conference", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1remove_1conference},
{"sflph_call_join_participant", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1join_1participant},
{"sflph_call_create_conf_from_participant_list", "(JLorg/sflphone/service/StringVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1create_1conf_1from_1participant_1list},
{"sflph_call_is_conference_participant", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1is_1conference_1participant},
{"sflph_call_add_participant", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1add_1participant},
{"sflph_call_add_main_participant", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1add_1main_1participant},
{"sflph_call_detach_participant", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1detach_1participant},
{"sflph_call_join_conference", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1join_1conference},
{"sflph_call_hang_up_conference", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1hang_1up_1conference},
{"sflph_call_hold_conference", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1hold_1conference},
{"sflph_call_unhold_conference", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1unhold_1conference},
{"sflph_call_get_conference_list", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1get_1conference_1list},
{"sflph_call_get_participant_list", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1get_1participant_1list},
{"sflph_call_get_display_names", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1get_1display_1names},
{"sflph_call_get_conference_id", "(Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1get_1conference_1id},
{"sflph_call_get_conference_details", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1get_1conference_1details},
{"sflph_call_play_recorded_file", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1play_1recorded_1file},
{"sflph_call_stop_recorded_file", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1stop_1recorded_1file},
{"sflph_call_toggle_recording", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1toggle_1recording},
{"sflph_call_set_recording", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1set_1recording},
{"sflph_call_record_playback_seek", "(D)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1record_1playback_1seek},
{"sflph_call_is_recording", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1is_1recording},
{"sflph_call_get_current_audio_codec_name", "(Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1get_1current_1audio_1codec_1name},
{"sflph_call_play_dtmf", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1play_1dtmf},
{"sflph_call_start_tone", "(II)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1start_1tone},
{"sflph_call_set_sas_verified", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1set_1sas_1verified},
{"sflph_call_reset_sas_verified", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1reset_1sas_1verified},
{"sflph_call_set_confirm_go_clear", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1set_1confirm_1go_1clear},
{"sflph_call_request_go_clear", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1request_1go_1clear},
{"sflph_call_accept_enrollment", "(Ljava/lang/String;Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1accept_1enrollment},
{"sflph_call_send_text_message", "(Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1call_1send_1text_1message},
{"delete_Callback", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1Callback},
{"Callback_callOnStateChange", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnStateChange},
{"Callback_callOnStateChangeSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnStateChangeSwigExplicitCallback},
{"Callback_callOnTransferFail", "(JLorg/sflphone/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnTransferFail},
{"Callback_callOnTransferFailSwigExplicitCallback", "(JLorg/sflphone/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnTransferFailSwigExplicitCallback},
{"Callback_callOnTransferSuccess", "(JLorg/sflphone/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnTransferSuccess},
{"Callback_callOnTransferSuccessSwigExplicitCallback", "(JLorg/sflphone/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnTransferSuccessSwigExplicitCallback},
{"Callback_callOnRecordPlaybackStopped", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnRecordPlaybackStopped},
{"Callback_callOnRecordPlaybackStoppedSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnRecordPlaybackStoppedSwigExplicitCallback},
{"Callback_callOnVoiceMailNotify", "(JLorg/sflphone/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnVoiceMailNotify},
{"Callback_callOnVoiceMailNotifySwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnVoiceMailNotifySwigExplicitCallback},
{"Callback_callOnIncomingMessage", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnIncomingMessage},
{"Callback_callOnIncomingMessageSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnIncomingMessageSwigExplicitCallback},
{"Callback_callOnIncomingCall", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnIncomingCall},
{"Callback_callOnIncomingCallSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnIncomingCallSwigExplicitCallback},
{"Callback_callOnRecordPlaybackFilepath", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnRecordPlaybackFilepath},
{"Callback_callOnRecordPlaybackFilepathSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnRecordPlaybackFilepathSwigExplicitCallback},
{"Callback_callOnConferenceCreated", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnConferenceCreated},
{"Callback_callOnConferenceCreatedSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnConferenceCreatedSwigExplicitCallback},
{"Callback_callOnConferenceChanged", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnConferenceChanged},
{"Callback_callOnConferenceChangedSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnConferenceChangedSwigExplicitCallback},
{"Callback_callOnUpdatePlaybackScale", "(JLorg/sflphone/service/Callback;Ljava/lang/String;II)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnUpdatePlaybackScale},
{"Callback_callOnUpdatePlaybackScaleSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;II)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnUpdatePlaybackScaleSwigExplicitCallback},
{"Callback_callOnConferenceRemove", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnConferenceRemove},
{"Callback_callOnConferenceRemoveSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnConferenceRemoveSwigExplicitCallback},
{"Callback_callOnNewCall", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnNewCall},
{"Callback_callOnNewCallSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnNewCallSwigExplicitCallback},
{"Callback_callOnSipCallStateChange", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSipCallStateChange},
{"Callback_callOnSipCallStateChangeSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSipCallStateChangeSwigExplicitCallback},
{"Callback_callOnRecordStateChange", "(JLorg/sflphone/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnRecordStateChange},
{"Callback_callOnRecordStateChangeSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnRecordStateChangeSwigExplicitCallback},
{"Callback_callOnSecureSdesOn", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSecureSdesOn},
{"Callback_callOnSecureSdesOnSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSecureSdesOnSwigExplicitCallback},
{"Callback_callOnSecureSdesOff", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSecureSdesOff},
{"Callback_callOnSecureSdesOffSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSecureSdesOffSwigExplicitCallback},
{"Callback_callOnSecureZrtpOn", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSecureZrtpOn},
{"Callback_callOnSecureZrtpOnSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSecureZrtpOnSwigExplicitCallback},
{"Callback_callOnSecureZrtpOff", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSecureZrtpOff},
{"Callback_callOnSecureZrtpOffSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnSecureZrtpOffSwigExplicitCallback},
{"Callback_callOnShowSas", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnShowSas},
{"Callback_callOnShowSasSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnShowSasSwigExplicitCallback},
{"Callback_callOnZrtpNotSuppOther", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnZrtpNotSuppOther},
{"Callback_callOnZrtpNotSuppOtherSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnZrtpNotSuppOtherSwigExplicitCallback},
{"Callback_callOnZrtpNegotiationFail", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnZrtpNegotiationFail},
{"Callback_callOnZrtpNegotiationFailSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnZrtpNegotiationFailSwigExplicitCallback},
{"Callback_callOnRtcpReceiveReport", "(JLorg/sflphone/service/Callback;Ljava/lang/String;JLorg/sflphone/service/IntegerMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnRtcpReceiveReport},
{"Callback_callOnRtcpReceiveReportSwigExplicitCallback", "(JLorg/sflphone/service/Callback;Ljava/lang/String;JLorg/sflphone/service/IntegerMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callOnRtcpReceiveReportSwigExplicitCallback},
{"new_Callback", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1Callback},
{"Callback_director_connect", "(Lorg/sflphone/service/Callback;JZZ)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1director_1connect},
{"Callback_change_ownership", "(Lorg/sflphone/service/Callback;JZ)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1change_1ownership},
{"sflph_config_get_account_details", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1account_1details},
{"sflph_config_set_account_details", "(Ljava/lang/String;JLorg/sflphone/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1account_1details},
{"sflph_config_get_account_template", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1account_1template},
{"sflph_config_add_account", "(JLorg/sflphone/service/StringMap;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1add_1account},
{"sflph_config_remove_account", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1remove_1account},
{"sflph_config_get_account_list", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1account_1list},
{"sflph_config_send_register", "(Ljava/lang/String;Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1send_1register},
{"sflph_config_register_all_accounts", "()V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1register_1all_1accounts},
{"sflph_config_get_tls_default_settings", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1tls_1default_1settings},
{"sflph_config_get_audio_codec_list", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1audio_1codec_1list},
{"sflph_config_get_supported_tls_method", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1supported_1tls_1method},
{"sflph_config_get_audio_codec_details", "(I)J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1audio_1codec_1details},
{"sflph_config_get_active_audio_codec_list", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1active_1audio_1codec_1list},
{"sflph_config_set_active_audio_codec_list", "(JLorg/sflphone/service/StringVect;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1active_1audio_1codec_1list},
{"sflph_config_get_audio_plugin_list", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1audio_1plugin_1list},
{"sflph_config_set_audio_plugin", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1audio_1plugin},
{"sflph_config_get_audio_output_device_list", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1audio_1output_1device_1list},
{"sflph_config_set_audio_output_device", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1audio_1output_1device},
{"sflph_config_set_audio_input_device", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1audio_1input_1device},
{"sflph_config_set_audio_ringtone_device", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1audio_1ringtone_1device},
{"sflph_config_get_audio_input_device_list", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1audio_1input_1device_1list},
{"sflph_config_get_current_audio_devices_index", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1current_1audio_1devices_1index},
{"sflph_config_get_audio_input_device_index", "(Ljava/lang/String;)I", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1audio_1input_1device_1index},
{"sflph_config_get_audio_output_device_index", "(Ljava/lang/String;)I", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1audio_1output_1device_1index},
{"sflph_config_get_current_audio_output_plugin", "()Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1current_1audio_1output_1plugin},
{"sflph_config_get_noise_suppress_state", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1noise_1suppress_1state},
{"sflph_config_set_noise_suppress_state", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1noise_1suppress_1state},
{"sflph_config_is_agc_enabled", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1is_1agc_1enabled},
{"sflph_config_enable_agc", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1enable_1agc},
{"sflph_config_mute_dtmf", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1mute_1dtmf},
{"sflph_config_is_dtmf_muted", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1is_1dtmf_1muted},
{"sflph_config_is_capture_muted", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1is_1capture_1muted},
{"sflph_config_mute_capture", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1mute_1capture},
{"sflph_config_is_playback_muted", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1is_1playback_1muted},
{"sflph_config_mute_playback", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1mute_1playback},
{"sflph_config_get_ringtone_list", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1ringtone_1list},
{"sflph_config_get_audio_manager", "()Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1audio_1manager},
{"sflph_config_set_audio_manager", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1audio_1manager},
{"sflph_config_get_supported_audio_managers", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1supported_1audio_1managers},
{"sflph_config_is_iax2_enabled", "()I", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1is_1iax2_1enabled},
{"sflph_config_get_record_path", "()Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1record_1path},
{"sflph_config_set_record_path", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1record_1path},
{"sflph_config_is_always_recording", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1is_1always_1recording},
{"sflph_config_set_always_recording", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1always_1recording},
{"sflph_config_set_history_limit", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1history_1limit},
{"sflph_config_get_history_limit", "()I", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1history_1limit},
{"sflph_config_clear_history", "()V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1clear_1history},
{"sflph_config_set_accounts_order", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1accounts_1order},
{"sflph_config_get_hook_settings", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1hook_1settings},
{"sflph_config_set_hook_settings", "(JLorg/sflphone/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1hook_1settings},
{"sflph_config_get_history", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1history},
{"sflph_config_get_tls_settings", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1tls_1settings},
{"sflph_config_set_tls_settings", "(JLorg/sflphone/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1tls_1settings},
{"sflph_config_get_ip2ip_details", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1ip2ip_1details},
{"sflph_config_get_credentials", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1credentials},
{"sflph_config_set_credentials", "(Ljava/lang/String;JLorg/sflphone/service/VectMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1credentials},
{"sflph_config_get_addr_from_interface_name", "(Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1addr_1from_1interface_1name},
{"sflph_config_get_all_ip_interface", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1all_1ip_1interface},
{"sflph_config_get_all_ip_interface_by_name", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1all_1ip_1interface_1by_1name},
{"sflph_config_get_shortcuts", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1shortcuts},
{"sflph_config_set_shortcuts", "(JLorg/sflphone/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1shortcuts},
{"sflph_config_set_volume", "(Ljava/lang/String;D)V", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1set_1volume},
{"sflph_config_get_volume", "(Ljava/lang/String;)D", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1get_1volume},
{"sflph_config_check_for_private_key", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1check_1for_1private_1key},
{"sflph_config_check_certificate_validity", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1check_1certificate_1validity},
{"sflph_config_check_hostname_certificate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_sflph_1config_1check_1hostname_1certificate},
{"delete_ConfigurationCallback", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1ConfigurationCallback},
{"ConfigurationCallback_configOnVolumeChange", "(JLorg/sflphone/service/ConfigurationCallback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnVolumeChange},
{"ConfigurationCallback_configOnVolumeChangeSwigExplicitConfigurationCallback", "(JLorg/sflphone/service/ConfigurationCallback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnVolumeChangeSwigExplicitConfigurationCallback},
{"ConfigurationCallback_configOnAccountsChange", "(JLorg/sflphone/service/ConfigurationCallback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnAccountsChange},
{"ConfigurationCallback_configOnAccountsChangeSwigExplicitConfigurationCallback", "(JLorg/sflphone/service/ConfigurationCallback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnAccountsChangeSwigExplicitConfigurationCallback},
{"ConfigurationCallback_configOnHistoryChange", "(JLorg/sflphone/service/ConfigurationCallback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnHistoryChange},
{"ConfigurationCallback_configOnHistoryChangeSwigExplicitConfigurationCallback", "(JLorg/sflphone/service/ConfigurationCallback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnHistoryChangeSwigExplicitConfigurationCallback},
{"ConfigurationCallback_configOnStunStatusFail", "(JLorg/sflphone/service/ConfigurationCallback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnStunStatusFail},
{"ConfigurationCallback_configOnStunStatusFailSwigExplicitConfigurationCallback", "(JLorg/sflphone/service/ConfigurationCallback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnStunStatusFailSwigExplicitConfigurationCallback},
{"ConfigurationCallback_configOnRegistrationStateChange", "(JLorg/sflphone/service/ConfigurationCallback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnRegistrationStateChange},
{"ConfigurationCallback_configOnRegistrationStateChangeSwigExplicitConfigurationCallback", "(JLorg/sflphone/service/ConfigurationCallback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnRegistrationStateChangeSwigExplicitConfigurationCallback},
{"ConfigurationCallback_configOnSipRegistrationStateChange", "(JLorg/sflphone/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnSipRegistrationStateChange},
{"ConfigurationCallback_configOnSipRegistrationStateChangeSwigExplicitConfigurationCallback", "(JLorg/sflphone/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnSipRegistrationStateChangeSwigExplicitConfigurationCallback},
{"ConfigurationCallback_configOnError", "(JLorg/sflphone/service/ConfigurationCallback;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnError},
{"ConfigurationCallback_configOnErrorSwigExplicitConfigurationCallback", "(JLorg/sflphone/service/ConfigurationCallback;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configOnErrorSwigExplicitConfigurationCallback},
{"ConfigurationCallback_configGetHardwareAudioFormat", "(JLorg/sflphone/service/ConfigurationCallback;)J", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configGetHardwareAudioFormat},
{"ConfigurationCallback_configGetHardwareAudioFormatSwigExplicitConfigurationCallback", "(JLorg/sflphone/service/ConfigurationCallback;)J", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configGetHardwareAudioFormatSwigExplicitConfigurationCallback},
{"new_ConfigurationCallback", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1ConfigurationCallback},
{"ConfigurationCallback_director_connect", "(Lorg/sflphone/service/ConfigurationCallback;JZZ)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1director_1connect},
{"ConfigurationCallback_change_ownership", "(Lorg/sflphone/service/ConfigurationCallback;JZ)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1change_1ownership},
{"init", "(JLorg/sflphone/service/ConfigurationCallback;JLorg/sflphone/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_init}

	};

	r = env->RegisterNatives (clazz, methods, (int) (sizeof(methods) / sizeof(methods[0])));
	return JNI_VERSION_1_6;
}

void JNI_OnUnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
	jclass clazz;

	SFL_INFO("JNI_OnUnLoad");

	/* get env */
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
		RING_ERR("JNI_OnUnLoad: failed to get the environment using GetEnv()");
        return;
    }
	SFL_INFO("JNI_OnUnLoad: GetEnv %p", env);

    /* Get jclass with env->FindClass */
	clazz = env->FindClass(ksflphoneservicePath);
	if (!clazz) {
        RING_ERR("JNI_OnUnLoad: whoops, %s class not found!", ksflphoneservicePath);
	}

	/* remove instances of class object we need into cache */
    //deinitClassHelper(env, gManagerObject);

	env->UnregisterNatives(clazz);
	SFL_INFO("JNI_OnUnLoad: Native functions unregistered");
}
