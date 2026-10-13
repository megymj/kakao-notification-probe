package dev.probe.kakao

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var countText: TextView
    private lateinit var dumpText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        countText = findViewById(R.id.countText)
        dumpText = findViewById(R.id.dumpText)

        findViewById<Button>(R.id.grantButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.batteryButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<Button>(R.id.refreshButton).setOnClickListener { render() }
        findViewById<Button>(R.id.shareButton).setOnClickListener { shareDumps() }
        findViewById<Button>(R.id.clearButton).setOnClickListener { confirmClear() }
    }

    override fun onResume() {
        super.onResume()
        DumpStore.onAppended = { render() }
        render()
    }

    override fun onPause() {
        DumpStore.onAppended = null
        super.onPause()
    }

    private fun render() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
        statusText.setText(if (granted) R.string.status_granted else R.string.status_denied)

        val recent = DumpStore.readRecent(this, RECENT_LIMIT)
        countText.text = getString(R.string.count_format, DumpStore.count(this), recent.size)
        dumpText.text = if (recent.isEmpty()) {
            getString(R.string.empty)
        } else {
            recent.joinToString("\n\n") { it.toString(2) }
        }
    }

    private fun shareDumps() {
        val file = DumpStore.file(this)
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(this, R.string.share_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, getString(R.string.share)))
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setMessage(R.string.clear_confirm)
            .setPositiveButton(R.string.clear) { _, _ ->
                DumpStore.clear(this)
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        private const val RECENT_LIMIT = 20
    }
}
