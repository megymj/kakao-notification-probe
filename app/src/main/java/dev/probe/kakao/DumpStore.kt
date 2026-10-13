package dev.probe.kakao

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File

/** 수집한 알림 덤프를 앱 전용 저장소의 JSONL 파일 하나에 보관한다. */
object DumpStore {

    private const val FILE_NAME = "dumps.jsonl"
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 화면이 열려 있을 때 새 덤프가 추가되면 호출된다 (메인 스레드). */
    @Volatile
    var onAppended: (() -> Unit)? = null

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun append(context: Context, entry: JSONObject) {
        file(context).appendText(entry.toString() + "\n")
        mainHandler.post { onAppended?.invoke() }
    }

    @Synchronized
    fun readRecent(context: Context, limit: Int): List<JSONObject> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return f.readLines()
            .filter { it.isNotBlank() }
            .takeLast(limit)
            .asReversed()
            .map { JSONObject(it) }
    }

    @Synchronized
    fun count(context: Context): Int {
        val f = file(context)
        if (!f.exists()) return 0
        return f.useLines { lines -> lines.count { it.isNotBlank() } }
    }

    @Synchronized
    fun clear(context: Context) {
        file(context).delete()
    }
}
