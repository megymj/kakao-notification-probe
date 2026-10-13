package dev.probe.kakao

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class KakaoNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        Log.i(TAG, "listener connected")
    }

    override fun onListenerDisconnected() {
        // 절전 등으로 연결이 끊기면 시스템에 재연결을 요청한다.
        Log.w(TAG, "listener disconnected, requesting rebind")
        requestRebind(ComponentName(this, KakaoNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 카카오톡 이외의 알림은 읽지도 저장하지도 않는다.
        if (sbn.packageName != KAKAO_PACKAGE) return

        try {
            val entry = NotificationDumper.dump(sbn)
            DumpStore.append(this, entry)
            // Logcat에는 원문 대신 키 목록만 남긴다.
            Log.d(TAG, "dumped kakao notification, extras keys=${sbn.notification.extras?.keySet()}")
        } catch (e: Exception) {
            Log.e(TAG, "failed to dump notification", e)
        }
    }

    companion object {
        private const val TAG = "KakaoProbe"
        const val KAKAO_PACKAGE = "com.kakao.talk"
    }
}
