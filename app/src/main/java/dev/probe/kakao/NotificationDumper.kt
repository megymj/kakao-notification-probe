package dev.probe.kakao

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** StatusBarNotification 하나를 분석용 JSON으로 변환한다. extras 값은 원문 그대로 기록한다. */
object NotificationDumper {

    fun dump(sbn: StatusBarNotification): JSONObject {
        val n = sbn.notification
        val extras = n.extras ?: Bundle.EMPTY
        return JSONObject().apply {
            put("capturedAt", System.currentTimeMillis())
            put("postTime", sbn.postTime)
            put("key", sbn.key)
            put("id", sbn.id)
            put("tag", sbn.tag ?: JSONObject.NULL)
            put("groupKey", sbn.groupKey ?: JSONObject.NULL)
            put("isGroupSummary", n.flags and Notification.FLAG_GROUP_SUMMARY != 0)
            put("channelId", n.channelId ?: JSONObject.NULL)
            put("shortcutId", n.shortcutId ?: JSONObject.NULL)
            put("category", n.category ?: JSONObject.NULL)
            put("extras", typedBundle(extras))
            put("messagingStyle", messagingStyle(n, extras))
        }
    }

    /** 최상위 extras: 키마다 {type, value}로 기록해 어떤 타입이 오는지 함께 확인한다. */
    private fun typedBundle(bundle: Bundle): JSONObject {
        val out = JSONObject()
        for (key in bundle.keySet().sorted()) {
            out.put(key, try {
                @Suppress("DEPRECATION")
                val value = bundle.get(key)
                JSONObject()
                    .put("type", value?.javaClass?.name ?: "null")
                    .put("value", toJson(value))
            } catch (e: Exception) {
                JSONObject().put("type", "<error>").put("value", "<error: $e>")
            })
        }
        return out
    }

    private fun plainBundle(bundle: Bundle): JSONObject {
        val out = JSONObject()
        for (key in bundle.keySet().sorted()) {
            out.put(key, try {
                @Suppress("DEPRECATION")
                toJson(bundle.get(key))
            } catch (e: Exception) {
                "<error: $e>"
            })
        }
        return out
    }

    private fun toJson(value: Any?): Any = when {
        value == null -> JSONObject.NULL
        value is CharSequence -> value.toString()
        value is Number || value is Boolean -> value
        value is Bundle -> plainBundle(value)
        value is Array<*> -> JSONArray().also { arr -> value.forEach { arr.put(toJson(it)) } }
        value is IntArray -> JSONArray(value.toList())
        value is LongArray -> JSONArray(value.toList())
        value is BooleanArray -> JSONArray(value.toList())
        value is FloatArray -> JSONArray(value.map { it.toDouble() })
        value is DoubleArray -> JSONArray(value.toList())
        value is List<*> -> JSONArray().also { arr -> value.forEach { arr.put(toJson(it)) } }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && value is android.app.Person ->
            JSONObject()
                .put("name", value.name?.toString() ?: JSONObject.NULL)
                .put("key", value.key ?: JSONObject.NULL)
                .put("uri", value.uri ?: JSONObject.NULL)
        value is Bitmap -> "<Bitmap ${value.width}x${value.height}>"
        value is Icon -> "<Icon>"
        else -> "<${value.javaClass.name}>"
    }

    private fun messagingStyle(n: Notification, extras: Bundle): Any {
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
            ?: return JSONObject.NULL

        // 중복 제거 키 후보에 쓸 방 이름: conversationTitle → subText → title 순으로 사용
        val room = style.conversationTitle?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: ""

        val messages = JSONArray()
        for (m in style.messages) {
            val sender = m.person?.name?.toString() ?: ""
            val text = m.text?.toString() ?: ""
            messages.put(
                JSONObject()
                    .put("sender", m.person?.name?.toString() ?: JSONObject.NULL)
                    .put("senderKey", m.person?.key ?: JSONObject.NULL)
                    .put("timestamp", m.timestamp)
                    .put("text", m.text?.toString() ?: JSONObject.NULL)
                    .put("dedupKey", sha256("$room|$sender|${m.timestamp}|$text"))
            )
        }

        return JSONObject()
            .put("conversationTitle", style.conversationTitle?.toString() ?: JSONObject.NULL)
            .put("isGroupConversation", style.isGroupConversation)
            .put("user", style.user.name?.toString() ?: JSONObject.NULL)
            .put("dedupRoom", room)
            .put("messages", messages)
            .put("historicMessagesCount", style.historicMessages.size)
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
