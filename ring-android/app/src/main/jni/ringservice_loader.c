#include "logger.h"

JavaVM *gJavaVM;
const char *kringservicePath = "cx/ring/service/RingserviceJNI";

void deinitClassHelper(JNIEnv *env, jobject obj) {
	RING_INFO("deinitClassHelper");

	/* delete cached object instances */
    env->DeleteGlobalRef(obj);
	RING_INFO("deinitClassHelper: object %x deleted", obj);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	JNIEnv *env;
	jclass clazz;
	jint r;

    RING_INFO("JNI_OnLoad");

	//Assume it is c++
	r = vm->GetEnv ((void **) &env, JNI_VERSION_1_6);
    if (r != JNI_OK) {
		RING_ERR("JNI_OnLoad: failed to get the environment using GetEnv()");
        return -1;
    }
	RING_INFO("JNI_Onload: GetEnv %p", env);

	clazz = env->FindClass (kringservicePath);
	if (!clazz) {
        RING_ERR("JNI_Onload: whoops, %s class not found!", kringservicePath);
	}
	gJavaVM = vm;
	RING_INFO("JNI_Onload: JavaVM %p", gJavaVM);

	/* put instances of class object we need into cache */
    //initClassHelper(env, kManagerPath, &gManagerObject);

	JNINativeMethod methods[] = {

	{"new_StringMap__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1StringMap_1_1SWIG_10},
{"new_StringMap__SWIG_1", "(JLcx/ring/service/StringMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1StringMap_1_1SWIG_11},
{"StringMap_size", "(JLcx/ring/service/StringMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1size},
{"StringMap_empty", "(JLcx/ring/service/StringMap;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1empty},
{"StringMap_clear", "(JLcx/ring/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1clear},
{"StringMap_get", "(JLcx/ring/service/StringMap;Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1get},
{"StringMap_set", "(JLcx/ring/service/StringMap;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1set},
{"StringMap_del", "(JLcx/ring/service/StringMap;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1del},
{"StringMap_has_key", "(JLcx/ring/service/StringMap;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1has_1key},
{"StringMap_keys", "(JLcx/ring/service/StringMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_StringMap_1keys},
{"delete_StringMap", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1StringMap},
{"new_StringVect__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1StringVect_1_1SWIG_10},
{"new_StringVect__SWIG_1", "(J)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1StringVect_1_1SWIG_11},
{"StringVect_capacity", "(JLcx/ring/service/StringVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1capacity},
{"StringVect_reserve", "(JLcx/ring/service/StringVect;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1reserve},
{"StringVect_isEmpty", "(JLcx/ring/service/StringVect;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1isEmpty},
{"StringVect_clear", "(JLcx/ring/service/StringVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1clear},
{"StringVect_get", "(JLcx/ring/service/StringVect;I)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1get},
{"StringVect_set", "(JLcx/ring/service/StringVect;ILjava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1set},
{"StringVect_add", "(JLcx/ring/service/StringVect;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1add},
{"StringVect_size", "(JLcx/ring/service/StringVect;)I", (void*)& Java_cx_ring_service_RingserviceJNI_StringVect_1size},
{"delete_StringVect", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1StringVect},
{"new_VectMap__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1VectMap_1_1SWIG_10},
{"new_VectMap__SWIG_1", "(J)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1VectMap_1_1SWIG_11},
{"VectMap_size", "(JLcx/ring/service/VectMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1size},
{"VectMap_capacity", "(JLcx/ring/service/VectMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1capacity},
{"VectMap_reserve", "(JLcx/ring/service/VectMap;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1reserve},
{"VectMap_isEmpty", "(JLcx/ring/service/VectMap;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1isEmpty},
{"VectMap_clear", "(JLcx/ring/service/VectMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1clear},
{"VectMap_add", "(JLcx/ring/service/VectMap;JLcx/ring/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1add},
{"VectMap_get", "(JLcx/ring/service/VectMap;I)J", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1get},
{"VectMap_set", "(JLcx/ring/service/VectMap;IJLcx/ring/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_VectMap_1set},
{"delete_VectMap", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1VectMap},
{"new_IntegerMap__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1IntegerMap_1_1SWIG_10},
{"new_IntegerMap__SWIG_1", "(JLcx/ring/service/IntegerMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1IntegerMap_1_1SWIG_11},
{"IntegerMap_size", "(JLcx/ring/service/IntegerMap;)J", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1size},
{"IntegerMap_empty", "(JLcx/ring/service/IntegerMap;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1empty},
{"IntegerMap_clear", "(JLcx/ring/service/IntegerMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1clear},
{"IntegerMap_get", "(JLcx/ring/service/IntegerMap;Ljava/lang/String;)I", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1get},
{"IntegerMap_set", "(JLcx/ring/service/IntegerMap;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1set},
{"IntegerMap_del", "(JLcx/ring/service/IntegerMap;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1del},
{"IntegerMap_has_key", "(JLcx/ring/service/IntegerMap;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_IntegerMap_1has_1key},
{"delete_IntegerMap", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1IntegerMap},
{"new_IntVect__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1IntVect_1_1SWIG_10},
{"new_IntVect__SWIG_1", "(J)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1IntVect_1_1SWIG_11},
{"IntVect_size", "(JLcx/ring/service/IntVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1size},
{"IntVect_capacity", "(JLcx/ring/service/IntVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1capacity},
{"IntVect_reserve", "(JLcx/ring/service/IntVect;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1reserve},
{"IntVect_isEmpty", "(JLcx/ring/service/IntVect;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1isEmpty},
{"IntVect_clear", "(JLcx/ring/service/IntVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1clear},
{"IntVect_add", "(JLcx/ring/service/IntVect;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1add},
{"IntVect_get", "(JLcx/ring/service/IntVect;I)I", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1get},
{"IntVect_set", "(JLcx/ring/service/IntVect;II)V", (void*)& Java_cx_ring_service_RingserviceJNI_IntVect_1set},
{"delete_IntVect", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1IntVect},
{"new_UintVect__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1UintVect_1_1SWIG_10},
{"new_UintVect__SWIG_1", "(J)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1UintVect_1_1SWIG_11},
{"UintVect_size", "(JLcx/ring/service/UintVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_UintVect_1size},
{"UintVect_capacity", "(JLcx/ring/service/UintVect;)J", (void*)& Java_cx_ring_service_RingserviceJNI_UintVect_1capacity},
{"UintVect_reserve", "(JLcx/ring/service/UintVect;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_UintVect_1reserve},
{"UintVect_isEmpty", "(JLcx/ring/service/UintVect;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_UintVect_1isEmpty},
{"UintVect_clear", "(JLcx/ring/service/UintVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_UintVect_1clear},
{"UintVect_add", "(JLcx/ring/service/UintVect;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_UintVect_1add},
{"UintVect_get", "(JLcx/ring/service/UintVect;I)J", (void*)& Java_cx_ring_service_RingserviceJNI_UintVect_1get},
{"UintVect_set", "(JLcx/ring/service/UintVect;IJ)V", (void*)& Java_cx_ring_service_RingserviceJNI_UintVect_1set},
{"delete_UintVect", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1UintVect},
{"new_Blob__SWIG_0", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1Blob_1_1SWIG_10},
{"new_Blob__SWIG_1", "(J)J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1Blob_1_1SWIG_11},
{"Blob_size", "(JLcx/ring/service/Blob;)J", (void*)& Java_cx_ring_service_RingserviceJNI_Blob_1size},
{"Blob_capacity", "(JLcx/ring/service/Blob;)J", (void*)& Java_cx_ring_service_RingserviceJNI_Blob_1capacity},
{"Blob_reserve", "(JLcx/ring/service/Blob;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_Blob_1reserve},
{"Blob_isEmpty", "(JLcx/ring/service/Blob;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_Blob_1isEmpty},
{"Blob_clear", "(JLcx/ring/service/Blob;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Blob_1clear},
{"Blob_add", "(JLcx/ring/service/Blob;S)V", (void*)& Java_cx_ring_service_RingserviceJNI_Blob_1add},
{"Blob_get", "(JLcx/ring/service/Blob;I)S", (void*)& Java_cx_ring_service_RingserviceJNI_Blob_1get},
{"Blob_set", "(JLcx/ring/service/Blob;IS)V", (void*)& Java_cx_ring_service_RingserviceJNI_Blob_1set},
{"delete_Blob", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1Blob},
{"fini", "()V", (void*)& Java_cx_ring_service_RingserviceJNI_fini},
{"pollEvents", "()V", (void*)& Java_cx_ring_service_RingserviceJNI_pollEvents},
{"placeCall", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_placeCall},
{"refuse", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_refuse},
{"accept", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_accept},
{"hangUp", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_hangUp},
{"hold", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_hold},
{"unhold", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_unhold},
{"muteLocalMedia", "(Ljava/lang/String;Ljava/lang/String;Z)Z", (void*)& Java_cx_ring_service_RingserviceJNI_muteLocalMedia},
{"transfer", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_transfer},
{"attendedTransfer", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_attendedTransfer},
{"getCallDetails", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getCallDetails},
{"getCallList", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getCallList},
{"removeConference", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_removeConference},
{"joinParticipant", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_joinParticipant},
{"createConfFromParticipantList", "(JLcx/ring/service/StringVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_createConfFromParticipantList},
{"isConferenceParticipant", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_isConferenceParticipant},
{"addParticipant", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_addParticipant},
{"addMainParticipant", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_addMainParticipant},
{"detachParticipant", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_detachParticipant},
{"joinConference", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_joinConference},
{"hangUpConference", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_hangUpConference},
{"holdConference", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_holdConference},
{"unholdConference", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_unholdConference},
{"getConferenceList", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getConferenceList},
{"getParticipantList", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getParticipantList},
{"getDisplayNames", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getDisplayNames},
{"getConferenceId", "(Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_getConferenceId},
{"getConferenceDetails", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getConferenceDetails},
{"startRecordedFilePlayback", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_startRecordedFilePlayback},
{"stopRecordedFilePlayback", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_stopRecordedFilePlayback},
{"toggleRecording", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_toggleRecording},
{"setRecording", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setRecording},
{"recordPlaybackSeek", "(D)V", (void*)& Java_cx_ring_service_RingserviceJNI_recordPlaybackSeek},
{"getIsRecording", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_getIsRecording},
{"getCurrentAudioCodecName", "(Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_getCurrentAudioCodecName},
{"playDTMF", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_playDTMF},
{"startTone", "(II)V", (void*)& Java_cx_ring_service_RingserviceJNI_startTone},
{"switchInput", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_switchInput},
{"setSASVerified", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setSASVerified},
{"resetSASVerified", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_resetSASVerified},
{"setConfirmGoClear", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setConfirmGoClear},
{"requestGoClear", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_requestGoClear},
{"acceptEnrollment", "(Ljava/lang/String;Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_acceptEnrollment},
{"sendTextMessage__SWIG_0", "(Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sendTextMessage_1_1SWIG_10},
{"sendTextMessage__SWIG_1", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sendTextMessage_1_1SWIG_11},
{"delete_Callback", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1Callback},
{"Callback_callStateChanged", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callStateChanged},
{"Callback_callStateChangedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1callStateChangedSwigExplicitCallback},
{"Callback_transferFailed", "(JLcx/ring/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1transferFailed},
{"Callback_transferFailedSwigExplicitCallback", "(JLcx/ring/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1transferFailedSwigExplicitCallback},
{"Callback_transferSucceeded", "(JLcx/ring/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1transferSucceeded},
{"Callback_transferSucceededSwigExplicitCallback", "(JLcx/ring/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1transferSucceededSwigExplicitCallback},
{"Callback_recordPlaybackStopped", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1recordPlaybackStopped},
{"Callback_recordPlaybackStoppedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1recordPlaybackStoppedSwigExplicitCallback},
{"Callback_voiceMailNotify", "(JLcx/ring/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1voiceMailNotify},
{"Callback_voiceMailNotifySwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1voiceMailNotifySwigExplicitCallback},
{"Callback_incomingMessage", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1incomingMessage},
{"Callback_incomingMessageSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1incomingMessageSwigExplicitCallback},
{"Callback_incomingCall", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1incomingCall},
{"Callback_incomingCallSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1incomingCallSwigExplicitCallback},
{"Callback_recordPlaybackFilepath", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1recordPlaybackFilepath},
{"Callback_recordPlaybackFilepathSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1recordPlaybackFilepathSwigExplicitCallback},
{"Callback_conferenceCreated", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1conferenceCreated},
{"Callback_conferenceCreatedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1conferenceCreatedSwigExplicitCallback},
{"Callback_conferenceChanged", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1conferenceChanged},
{"Callback_conferenceChangedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1conferenceChangedSwigExplicitCallback},
{"Callback_conferenceRemoved", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1conferenceRemoved},
{"Callback_conferenceRemovedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1conferenceRemovedSwigExplicitCallback},
{"Callback_newCallCreated", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1newCallCreated},
{"Callback_newCallCreatedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1newCallCreatedSwigExplicitCallback},
{"Callback_updatePlaybackScale", "(JLcx/ring/service/Callback;Ljava/lang/String;II)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1updatePlaybackScale},
{"Callback_updatePlaybackScaleSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;II)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1updatePlaybackScaleSwigExplicitCallback},
{"Callback_conferenceRemove", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1conferenceRemove},
{"Callback_conferenceRemoveSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1conferenceRemoveSwigExplicitCallback},
{"Callback_newCall", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1newCall},
{"Callback_newCallSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1newCallSwigExplicitCallback},
{"Callback_sipCallStateChange", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1sipCallStateChange},
{"Callback_sipCallStateChangeSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1sipCallStateChangeSwigExplicitCallback},
{"Callback_recordingStateChanged", "(JLcx/ring/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1recordingStateChanged},
{"Callback_recordingStateChangedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1recordingStateChangedSwigExplicitCallback},
{"Callback_recordStateChange", "(JLcx/ring/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1recordStateChange},
{"Callback_recordStateChangeSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1recordStateChangeSwigExplicitCallback},
{"Callback_secureSdesOn", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1secureSdesOn},
{"Callback_secureSdesOnSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1secureSdesOnSwigExplicitCallback},
{"Callback_secureSdesOff", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1secureSdesOff},
{"Callback_secureSdesOffSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1secureSdesOffSwigExplicitCallback},
{"Callback_secureZrtpOn", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1secureZrtpOn},
{"Callback_secureZrtpOnSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1secureZrtpOnSwigExplicitCallback},
{"Callback_secureZrtpOff", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1secureZrtpOff},
{"Callback_secureZrtpOffSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1secureZrtpOffSwigExplicitCallback},
{"Callback_showSAS", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1showSAS},
{"Callback_showSASSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1showSASSwigExplicitCallback},
{"Callback_zrtpNotSuppOther", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1zrtpNotSuppOther},
{"Callback_zrtpNotSuppOtherSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1zrtpNotSuppOtherSwigExplicitCallback},
{"Callback_zrtpNegotiationFailed", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1zrtpNegotiationFailed},
{"Callback_zrtpNegotiationFailedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1zrtpNegotiationFailedSwigExplicitCallback},
{"Callback_onRtcpReportReceived", "(JLcx/ring/service/Callback;Ljava/lang/String;JLcx/ring/service/IntegerMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1onRtcpReportReceived},
{"Callback_onRtcpReportReceivedSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;JLcx/ring/service/IntegerMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1onRtcpReportReceivedSwigExplicitCallback},
{"Callback_peerHold", "(JLcx/ring/service/Callback;Ljava/lang/String;Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1peerHold},
{"Callback_peerHoldSwigExplicitCallback", "(JLcx/ring/service/Callback;Ljava/lang/String;Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1peerHoldSwigExplicitCallback},
{"new_Callback", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1Callback},
{"Callback_director_connect", "(Lcx/ring/service/Callback;JZZ)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1director_1connect},
{"Callback_change_ownership", "(Lcx/ring/service/Callback;JZ)V", (void*)& Java_cx_ring_service_RingserviceJNI_Callback_1change_1ownership},
{"getAccountDetails", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getAccountDetails},
{"getVolatileAccountDetails", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getVolatileAccountDetails},
{"setAccountDetails", "(Ljava/lang/String;JLcx/ring/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setAccountDetails},
{"getAccountTemplate", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getAccountTemplate},
{"addAccount", "(JLcx/ring/service/StringMap;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_addAccount},
{"removeAccount", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_removeAccount},
{"getAccountList", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getAccountList},
{"sendRegister", "(Ljava/lang/String;Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_sendRegister},
{"registerAllAccounts", "()V", (void*)& Java_cx_ring_service_RingserviceJNI_registerAllAccounts},
{"sendAccountTextMessage", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sendAccountTextMessage},
{"getTlsDefaultSettings", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getTlsDefaultSettings},
{"getCodecList", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getCodecList},
{"getSupportedTlsMethod", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getSupportedTlsMethod},
{"getSupportedCiphers", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getSupportedCiphers},
{"getCodecDetails", "(Ljava/lang/String;J)J", (void*)& Java_cx_ring_service_RingserviceJNI_getCodecDetails},
{"setCodecDetails", "(Ljava/lang/String;JJLcx/ring/service/StringMap;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_setCodecDetails},
{"getActiveCodecList", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getActiveCodecList},
{"setActiveCodecList", "(Ljava/lang/String;JLcx/ring/service/UintVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setActiveCodecList},
{"getAudioPluginList", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getAudioPluginList},
{"setAudioPlugin", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setAudioPlugin},
{"getAudioOutputDeviceList", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getAudioOutputDeviceList},
{"setAudioOutputDevice", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_setAudioOutputDevice},
{"setAudioInputDevice", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_setAudioInputDevice},
{"setAudioRingtoneDevice", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_setAudioRingtoneDevice},
{"getAudioInputDeviceList", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getAudioInputDeviceList},
{"getCurrentAudioDevicesIndex", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getCurrentAudioDevicesIndex},
{"getAudioInputDeviceIndex", "(Ljava/lang/String;)I", (void*)& Java_cx_ring_service_RingserviceJNI_getAudioInputDeviceIndex},
{"getAudioOutputDeviceIndex", "(Ljava/lang/String;)I", (void*)& Java_cx_ring_service_RingserviceJNI_getAudioOutputDeviceIndex},
{"getCurrentAudioOutputPlugin", "()Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_getCurrentAudioOutputPlugin},
{"getNoiseSuppressState", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_getNoiseSuppressState},
{"setNoiseSuppressState", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_setNoiseSuppressState},
{"isAgcEnabled", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_isAgcEnabled},
{"setAgcState", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_setAgcState},
{"muteDtmf", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_muteDtmf},
{"isDtmfMuted", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_isDtmfMuted},
{"isCaptureMuted", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_isCaptureMuted},
{"muteCapture", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_muteCapture},
{"isPlaybackMuted", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_isPlaybackMuted},
{"mutePlayback", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_mutePlayback},
{"getAudioManager", "()Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_getAudioManager},
{"setAudioManager", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_setAudioManager},
{"isIax2Enabled", "()I", (void*)& Java_cx_ring_service_RingserviceJNI_isIax2Enabled},
{"getRecordPath", "()Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_getRecordPath},
{"setRecordPath", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setRecordPath},
{"getIsAlwaysRecording", "()Z", (void*)& Java_cx_ring_service_RingserviceJNI_getIsAlwaysRecording},
{"setIsAlwaysRecording", "(Z)V", (void*)& Java_cx_ring_service_RingserviceJNI_setIsAlwaysRecording},
{"setHistoryLimit", "(I)V", (void*)& Java_cx_ring_service_RingserviceJNI_setHistoryLimit},
{"getHistoryLimit", "()I", (void*)& Java_cx_ring_service_RingserviceJNI_getHistoryLimit},
{"setAccountsOrder", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setAccountsOrder},
{"getHookSettings", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getHookSettings},
{"setHookSettings", "(JLcx/ring/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setHookSettings},
{"getIp2IpDetails", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getIp2IpDetails},
{"getCredentials", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getCredentials},
{"setCredentials", "(Ljava/lang/String;JLcx/ring/service/VectMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setCredentials},
{"getAddrFromInterfaceName", "(Ljava/lang/String;)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_getAddrFromInterfaceName},
{"getAllIpInterface", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getAllIpInterface},
{"getAllIpInterfaceByName", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getAllIpInterfaceByName},
{"getShortcuts", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getShortcuts},
{"setShortcuts", "(JLcx/ring/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_setShortcuts},
{"setVolume", "(Ljava/lang/String;D)V", (void*)& Java_cx_ring_service_RingserviceJNI_setVolume},
{"getVolume", "(Ljava/lang/String;)D", (void*)& Java_cx_ring_service_RingserviceJNI_getVolume},
{"validateCertificate", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_validateCertificate},
{"validateCertificateRaw", "(Ljava/lang/String;JLcx/ring/service/Blob;)J", (void*)& Java_cx_ring_service_RingserviceJNI_validateCertificateRaw},
{"getCertificateDetails", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getCertificateDetails},
{"getCertificateDetailsRaw", "(JLcx/ring/service/Blob;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getCertificateDetailsRaw},
{"getPinnedCertificates", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_getPinnedCertificates},
{"pinCertificate", "(JLcx/ring/service/Blob;Z)Ljava/lang/String;", (void*)& Java_cx_ring_service_RingserviceJNI_pinCertificate},
{"unpinCertificate", "(Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_unpinCertificate},
{"pinCertificatePath", "(Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_pinCertificatePath},
{"unpinCertificatePath", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_unpinCertificatePath},
{"pinRemoteCertificate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_pinRemoteCertificate},
{"setCertificateStatus", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_setCertificateStatus},
{"getCertificatesByStatus", "(Ljava/lang/String;Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getCertificatesByStatus},
{"getTrustRequests", "(Ljava/lang/String;)J", (void*)& Java_cx_ring_service_RingserviceJNI_getTrustRequests},
{"acceptTrustRequest", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_acceptTrustRequest},
{"discardTrustRequest", "(Ljava/lang/String;Ljava/lang/String;)Z", (void*)& Java_cx_ring_service_RingserviceJNI_discardTrustRequest},
{"sendTrustRequest", "(Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_sendTrustRequest},
{"delete_ConfigurationCallback", "(J)V", (void*)& Java_cx_ring_service_RingserviceJNI_delete_1ConfigurationCallback},
{"ConfigurationCallback_volumeChanged", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1volumeChanged},
{"ConfigurationCallback_volumeChangedSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1volumeChangedSwigExplicitConfigurationCallback},
{"ConfigurationCallback_accountsChanged", "(JLcx/ring/service/ConfigurationCallback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1accountsChanged},
{"ConfigurationCallback_accountsChangedSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1accountsChangedSwigExplicitConfigurationCallback},
{"ConfigurationCallback_historyChanged", "(JLcx/ring/service/ConfigurationCallback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1historyChanged},
{"ConfigurationCallback_historyChangedSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1historyChangedSwigExplicitConfigurationCallback},
{"ConfigurationCallback_stunStatusFailure", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1stunStatusFailure},
{"ConfigurationCallback_stunStatusFailureSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1stunStatusFailureSwigExplicitConfigurationCallback},
{"ConfigurationCallback_registrationStateChanged", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1registrationStateChanged},
{"ConfigurationCallback_registrationStateChangedSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1registrationStateChangedSwigExplicitConfigurationCallback},
{"ConfigurationCallback_volatileAccountDetailsChanged", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;JLcx/ring/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1volatileAccountDetailsChanged},
{"ConfigurationCallback_volatileAccountDetailsChangedSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;JLcx/ring/service/StringMap;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1volatileAccountDetailsChangedSwigExplicitConfigurationCallback},
{"ConfigurationCallback_incomingAccountMessage", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1incomingAccountMessage},
{"ConfigurationCallback_incomingAccountMessageSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1incomingAccountMessageSwigExplicitConfigurationCallback},
{"ConfigurationCallback_incomingTrustRequest", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1incomingTrustRequest},
{"ConfigurationCallback_incomingTrustRequestSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;J)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1incomingTrustRequestSwigExplicitConfigurationCallback},
{"ConfigurationCallback_certificatePinned", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1certificatePinned},
{"ConfigurationCallback_certificatePinnedSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1certificatePinnedSwigExplicitConfigurationCallback},
{"ConfigurationCallback_certificatePathPinned", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;JLcx/ring/service/StringVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1certificatePathPinned},
{"ConfigurationCallback_certificatePathPinnedSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;JLcx/ring/service/StringVect;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1certificatePathPinnedSwigExplicitConfigurationCallback},
{"ConfigurationCallback_certificateExpired", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1certificateExpired},
{"ConfigurationCallback_certificateExpiredSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1certificateExpiredSwigExplicitConfigurationCallback},
{"ConfigurationCallback_certificateStateChanged", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1certificateStateChanged},
{"ConfigurationCallback_certificateStateChangedSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1certificateStateChangedSwigExplicitConfigurationCallback},
{"ConfigurationCallback_errorAlert", "(JLcx/ring/service/ConfigurationCallback;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1errorAlert},
{"ConfigurationCallback_errorAlertSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;I)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1errorAlertSwigExplicitConfigurationCallback},
{"ConfigurationCallback_configGetHardwareAudioFormat", "(JLcx/ring/service/ConfigurationCallback;)J", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configGetHardwareAudioFormat},
{"ConfigurationCallback_configGetHardwareAudioFormatSwigExplicitConfigurationCallback", "(JLcx/ring/service/ConfigurationCallback;)J", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1configGetHardwareAudioFormatSwigExplicitConfigurationCallback},
{"new_ConfigurationCallback", "()J", (void*)& Java_cx_ring_service_RingserviceJNI_new_1ConfigurationCallback},
{"ConfigurationCallback_director_connect", "(Lcx/ring/service/ConfigurationCallback;JZZ)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1director_1connect},
{"ConfigurationCallback_change_ownership", "(Lcx/ring/service/ConfigurationCallback;JZ)V", (void*)& Java_cx_ring_service_RingserviceJNI_ConfigurationCallback_1change_1ownership},
{"init", "(JLcx/ring/service/ConfigurationCallback;JLcx/ring/service/Callback;)V", (void*)& Java_cx_ring_service_RingserviceJNI_init}

	};

	r = env->RegisterNatives (clazz, methods, (int) (sizeof(methods) / sizeof(methods[0])));
	return JNI_VERSION_1_6;
}

void JNI_OnUnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
	jclass clazz;

	RING_INFO("JNI_OnUnLoad");

	/* get env */
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
		RING_ERR("JNI_OnUnLoad: failed to get the environment using GetEnv()");
        return;
    }
	RING_INFO("JNI_OnUnLoad: GetEnv %p", env);

    /* Get jclass with env->FindClass */
	clazz = env->FindClass(kringservicePath);
	if (!clazz) {
        RING_ERR("JNI_OnUnLoad: whoops, %s class not found!", kringservicePath);
	}

	/* remove instances of class object we need into cache */
    //deinitClassHelper(env, gManagerObject);

	env->UnregisterNatives(clazz);
	RING_INFO("JNI_OnUnLoad: Native functions unregistered");
}
