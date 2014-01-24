#include <jni.h>

#include "native_log.h"

JNIEXPORT jobject JNICALL Java_me_wtao_service_ScreenCaptureService_nativeTakeScreenCapture
  (JNIEnv *env, jobject thiz) {
	nativeDebug("start taking screen capture...");
	return 0;
}
