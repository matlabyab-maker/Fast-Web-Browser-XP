package com.fastbrowser.xp

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Downloader 2: an independent browser-owned downloader inspired by the
 * supplied Quick Browser APK's download flow (filename prompt, task/progress
 * handling and downloaded-file management). No code is copied from that APK.
 */
object Download2 {
    private val executor = Executors.newCachedThreadPool()

    fun start(context: Context, url: String, fileName: String, userAgent: String?, referer: String?) {
        val dialog = ProgressDialog(context).apply {
            setTitle("Downloader 2")
            setMessage("در حال شروع دانلود…")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            progress = 0
            setCancelable(true)
            show()
        }
        val cancel = AtomicBoolean(false)
        dialog.setOnCancelListener { cancel.set(true) }

        executor.execute {
            var conn: HttpURLConnection? = null
            var output: OutputStream? = null
            var targetUri: Uri? = null
            var targetFile: File? = null
            try {
                var currentUrl = url
                var response: HttpURLConnection? = null
                var redirects = 0
                while (redirects < 6) {
                    response?.disconnect()
                    val c = URL(currentUrl).openConnection() as HttpURLConnection
                    c.instanceFollowRedirects = false
                    c.connectTimeout = 15000
                    c.readTimeout = 30000
                    c.setRequestProperty("User-Agent", userAgent ?: "Fast Browser XP")
                    if (!referer.isNullOrBlank()) c.setRequestProperty("Referer", referer)
                    val cookie = android.webkit.CookieManager.getInstance().getCookie(currentUrl)
                    if (!cookie.isNullOrBlank()) c.setRequestProperty("Cookie", cookie)
                    c.connect()
                    if (c.responseCode in 300..399) {
                        val location = c.getHeaderField("Location")
                        if (location.isNullOrBlank()) { response = c; break }
                        currentUrl = URL(URL(currentUrl), location).toString()
                        redirects++
                        continue
                    }
                    response = c
                    break
                }
                conn = response ?: throw Exception("No connection")
                if (conn!!.responseCode !in 200..299) throw Exception("HTTP ${conn!!.responseCode}")

                val target = createTarget(context, fileName)
                output = target.output
                targetUri = target.uri
                targetFile = target.file

                val total = conn!!.contentLengthLong
                BufferedInputStream(conn!!.inputStream, 32 * 1024).use { input ->
                    val buffer = ByteArray(32 * 1024)
                    var done = 0L
                    var lastUi = 0L
                    while (!cancel.get()) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output!!.write(buffer, 0, n)
                        done += n
                        val now = System.currentTimeMillis()
                        if (now - lastUi > 250) {
                            lastUi = now
                            val pct = if (total > 0) ((done * 100L) / total).toInt().coerceIn(0, 100) else 0
                            Handler(Looper.getMainLooper()).post {
                                if (dialog.isShowing) {
                                    dialog.progress = pct
                                    dialog.setMessage(if (total > 0) "${formatBytes(done)} / ${formatBytes(total)}" else formatBytes(done))
                                }
                            }
                        }
                    }
                }
                output!!.flush()
                output!!.close()
                output = null

                if (cancel.get()) {
                    deleteTarget(context, targetUri, targetFile)
                    Handler(Looper.getMainLooper()).post { if (dialog.isShowing) dialog.dismiss() }
                    return@execute
                }

                if (Build.VERSION.SDK_INT >= 29 && targetUri != null) {
                    val doneValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                    context.contentResolver.update(targetUri!!, doneValues, null, null)
                }
                Handler(Looper.getMainLooper()).post {
                    if (dialog.isShowing) dialog.dismiss()
                    android.widget.Toast.makeText(context, "Downloader 2 تمام شد: ${fileName}", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                try { output?.close() } catch (_: Exception) {}
                deleteTarget(context, targetUri, targetFile)
                Handler(Looper.getMainLooper()).post {
                    if (dialog.isShowing) dialog.dismiss()
                    android.widget.Toast.makeText(context, "Downloader 2 خطا: ${e.message ?: "دانلود ناموفق"}", android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    private data class Target(val output: OutputStream, val uri: Uri?, val file: File?)

    private fun createTarget(context: Context, fileName: String): Target {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("ساخت فایل دانلود ممکن نشد")
            return try {
                Target(context.contentResolver.openOutputStream(uri) ?: throw Exception("باز کردن فایل ممکن نشد"), uri, null)
            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw e
            }
        }
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        val file = uniqueFile(File(dir, fileName))
        return Target(FileOutputStream(file), null, file)
    }

    private fun deleteTarget(context: Context, uri: Uri?, file: File?) {
        try { if (uri != null) context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
        try { file?.delete() } catch (_: Exception) {}
    }

    private fun uniqueFile(base: File): File {
        if (!base.exists()) return base
        val dot = base.name.lastIndexOf('.')
        val stem = if (dot > 0) base.name.substring(0, dot) else base.name
        val ext = if (dot > 0) base.name.substring(dot) else ""
        var i = 1
        while (true) {
            val f = File(base.parentFile, "$stem ($i)$ext")
            if (!f.exists()) return f
            i++
        }
    }

    private fun formatBytes(v: Long): String {
        if (v < 1024) return "$v B"
        if (v < 1024 * 1024) return "%.1f KB".format(v / 1024.0)
        if (v < 1024L * 1024L * 1024L) return "%.1f MB".format(v / (1024.0 * 1024.0))
        return "%.1f GB".format(v / (1024.0 * 1024.0 * 1024.0))
    }
}
