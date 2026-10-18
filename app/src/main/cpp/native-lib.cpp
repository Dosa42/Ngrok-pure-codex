#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include "tunnel_forwarder.h"

extern "C" {

JNIEXPORT jint JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeProbePort(
        JNIEnv* env,
        jobject /* this */,
        jstring jHost,
        jint jPort) {
    const char* hostChars = env->GetStringUTFChars(jHost, nullptr);
    std::string host(hostChars ? hostChars : "127.0.0.1");
    if (hostChars) env->ReleaseStringUTFChars(jHost, hostChars);

    return TunnelForwarder::getInstance().probePort(host, jPort);
}

JNIEXPORT jboolean JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeStart(
        JNIEnv* env,
        jobject /* this */,
        jstring jHost,
        jint jPort,
        jstring jToken) {
    const char* hostChars = env->GetStringUTFChars(jHost, nullptr);
    const char* tokenChars = env->GetStringUTFChars(jToken, nullptr);

    std::string host(hostChars ? hostChars : "127.0.0.1");
    std::string token(tokenChars ? tokenChars : "");

    if (hostChars) env->ReleaseStringUTFChars(jHost, hostChars);
    if (tokenChars) env->ReleaseStringUTFChars(jToken, tokenChars);

    bool res = TunnelForwarder::getInstance().start(host, jPort, token);
    return res ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeStop(
        JNIEnv* /* env */,
        jobject /* this */) {
    TunnelForwarder::getInstance().stop();
}

JNIEXPORT jint JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeGetStatus(
        JNIEnv* /* env */,
        jobject /* this */) {
    return static_cast<jint>(TunnelForwarder::getInstance().getState());
}

JNIEXPORT jstring JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeGetPublicUrl(
        JNIEnv* env,
        jobject /* this */) {
    std::string url = TunnelForwarder::getInstance().getPublicUrl();
    return env->NewStringUTF(url.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeGetLastError(
        JNIEnv* env,
        jobject /* this */) {
    std::string err = TunnelForwarder::getInstance().getLastError();
    return env->NewStringUTF(err.c_str());
}

JNIEXPORT jlongArray JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeGetStats(
        JNIEnv* env,
        jobject /* this */) {
    ForwarderStats stats = TunnelForwarder::getInstance().getStats();
    jlongArray arr = env->NewLongArray(4);
    if (!arr) return nullptr;

    jlong elements[4] = {
        static_cast<jlong>(stats.bytesIn),
        static_cast<jlong>(stats.bytesOut),
        static_cast<jlong>(stats.activeConnections),
        static_cast<jlong>(stats.totalRequests)
    };
    env->SetLongArrayRegion(arr, 0, 4, elements);
    return arr;
}

JNIEXPORT jbyteArray JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeForwardBytes(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray jReq,
        jstring jClientIp) {
    if (!jReq) return nullptr;

    jsize reqLen = env->GetArrayLength(jReq);
    std::vector<uint8_t> reqVec(reqLen);
    env->GetByteArrayRegion(jReq, 0, reqLen, reinterpret_cast<jbyte*>(reqVec.data()));

    const char* ipChars = jClientIp ? env->GetStringUTFChars(jClientIp, nullptr) : nullptr;
    std::string clientIp(ipChars ? ipChars : "127.0.0.1");
    if (ipChars) env->ReleaseStringUTFChars(jClientIp, ipChars);

    int statusCode = 0;
    std::string errorMsg;
    std::vector<uint8_t> respVec = TunnelForwarder::getInstance().forwardDirect(reqVec, clientIp, statusCode, errorMsg);

    jbyteArray jResp = env->NewByteArray(static_cast<jsize>(respVec.size()));
    if (jResp && !respVec.empty()) {
        env->SetByteArrayRegion(jResp, 0, static_cast<jsize>(respVec.size()), reinterpret_cast<const jbyte*>(respVec.data()));
    }
    return jResp;
}

JNIEXPORT jstring JNICALL
Java_com_example_ngrokforwarder_NativeTunnelBridge_nativeGetRecentEventsJson(
        JNIEnv* env,
        jobject /* this */,
        jint maxCount) {
    auto events = TunnelForwarder::getInstance().getRecentEvents(maxCount > 0 ? static_cast<size_t>(maxCount) : 50);

    // Build simple JSON array
    std::ostringstream ss;
    ss << "[";
    for (size_t i = 0; i < events.size(); ++i) {
        const auto& evt = events[i];
        if (i > 0) ss << ",";
        ss << "{";
        ss << "\"timestamp\":" << evt.timestamp << ",";
        ss << "\"method\":\"" << evt.method << "\",";
        ss << "\"path\":\"" << evt.path << "\",";
        ss << "\"statusCode\":" << evt.statusCode << ",";
        ss << "\"durationMs\":" << evt.durationMs << ",";
        ss << "\"bytesReceived\":" << evt.bytesReceived << ",";
        ss << "\"bytesSent\":" << evt.bytesSent << ",";
        ss << "\"clientIp\":\"" << evt.clientIp << "\",";
        ss << "\"statusMessage\":\"" << evt.statusMessage << "\"";
        ss << "}";
    }
    ss << "]";

    std::string jsonStr = ss.str();
    return env->NewStringUTF(jsonStr.c_str());
}

} // extern "C"
