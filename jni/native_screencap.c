#include <jni.h>

#include "native_log.h"

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	nativeDebug("JNI_OnLoad");

	return JNI_VERSION_1_2; // use JNI_VERSION_1_2 or later
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{
	nativeDebug("JNI_OnUnload");
}

JNIEXPORT jobject JNICALL Java_me_wtao_service_ScreenCaptureService_nativeTakeScreenCapture
  (JNIEnv *env, jobject thiz) {
	nativeDebug("start taking screen capture...");
	return 0;
}
