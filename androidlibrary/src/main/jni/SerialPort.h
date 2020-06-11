/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_winplus_serial_utils_SerialPort */

#ifndef _Included_android_serialport_SerialPort
#define _Included_android_serialport_SerialPort
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_winplus_serial_utils_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;II)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL Java_android_serialport_SerialPort_open
        (JNIEnv *env, jclass thiz, jstring path, jint baudrate,
         jint databits, jint stopbits, jchar parity);
/*
 * Class:     org_winplus_serial_utils_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_android_serialport_SerialPort_close
        (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif