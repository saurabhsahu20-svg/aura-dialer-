package com.scall.phone.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Recording library for user-provided, consented audio files.
 *
 * This does NOT capture SIM/Telecom call audio or use inaccessible
 * VOICE_CALL / CAPTURE_AUDIO_OUTPUT sources. A private collection is
 * stored under filesDir, never external/public storage.
 */
internal data class SavedRecording(
    val id: String,
    val title: String,
    val phoneNumber: String,
    val importedAtMillis: Long,
    val durationMillis: Long,
    val fileName: String,
    val bytes: Long
)

internal fun recordingRetentionExpired(
    importedAtMillis: Long, nowMillis: Long, days: Int
): Boolean = days > 0 &&
    importedAtMillis in 1 until nowMillis &&
    nowMillis - importedAtMillis > days.toLong() * 24L * 60L * 60L * 1000L

internal fun safeRecordingTitle(value: String): String =
    value.trim().take(90).ifBlank { "Imported recording" }

internal fun canAutomaticallyRecordCellularCalls(): Boolean = false

internal class RecordingStore(private val context: Context) {
    companion object {
        private const val MAX_FILE_BYTES = 180L * 1024L * 1024L
        private val safeId = Regex("^[a-f0-9-]{36}$")
        private val supportedExtensions = setOf("m4a", "mp3", "wav", "ogg", "opus", "aac", "amr", "3gp", "flac")
        const val KEEP_FOREVER = 0
    }

    private val dir: File get() = File(context.filesDir, "recordings").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("s_call_recordings", Context.MODE_PRIVATE)

    fun retentionDays(): Int = prefs.getInt("retention_days", KEEP_FOREVER)
    fun setRetentionDays(value: Int) {
        require(value == 0 || value == 30 || value == 90)
        prefs.edit().putInt("retention_days", value).apply()
    }

    private fun metadata(id: String): File = File(dir, "$id.json")

    private fun audioFile(item: SavedRecording): File? {
        if (!safeId.matches(item.id)) return null
        if (!item.fileName.startsWith(item.id + ".")) return null
        if (item.fileName.substringAfterLast('.') !in supportedExtensions) return null
        return File(dir, item.fileName)
    }

    fun playableFile(item: SavedRecording): File? =
        audioFile(item)?.takeIf { it.isFile && it.length() > 0 }

    private fun encode(item: SavedRecording): ByteArray =
        JSONObject()
            .put("id", item.id).put("title", item.title)
            .put("number", item.phoneNumber)
            .put("imported", item.importedAtMillis)
            .put("duration", item.durationMillis)
            .put("file", item.fileName).put("bytes", item.bytes)
            .toString().toByteArray(Charsets.UTF_8)

    private fun read(file: File): SavedRecording? = runCatching {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        val item = SavedRecording(
            json.getString("id"), json.getString("title"),
            json.optString("number"), json.getLong("imported"),
            json.getLong("duration"), json.getString("file"),
            json.getLong("bytes")
        )
        if (file.name != "${item.id}.json" || playableFile(item) == null) null else item
    }.getOrNull()

    private fun write(item: SavedRecording) {
        require(safeId.matches(item.id))
        val atomic = AtomicFile(metadata(item.id))
        val output = atomic.startWrite()
        try {
            output.write(encode(item))
            atomic.finishWrite(output)
        } catch (error: Exception) {
            atomic.failWrite(output)
            throw error
        }
    }

    suspend fun all(): List<SavedRecording> = withContext(Dispatchers.IO) {
        dir.listFiles()?.filter { it.name.endsWith(".json") }
            ?.mapNotNull(::read)?.sortedByDescending { it.importedAtMillis }.orEmpty()
    }

    suspend fun importAudio(uri: Uri): SavedRecording = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty().lowercase()
        if (!mime.startsWith("audio/") && mime != "application/ogg") {
            throw IllegalArgumentException("Select an audio recording (MP3, M4A, WAV, etc.)")
        }
        var displayName = "Recording"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) displayName = cursor.getString(0) ?: displayName
        }
        val extension = displayName.substringAfterLast('.', "").lowercase().let {
            when {
                it in supportedExtensions -> it
                mime.contains("mpeg") -> "mp3"
                mime.contains("mp4") || mime.contains("m4a") -> "m4a"
                mime.contains("wav") -> "wav"
                mime.contains("flac") -> "flac"
                mime.contains("ogg") -> "ogg"
                mime.contains("3gpp") -> "3gp"
                else -> throw IllegalArgumentException("Unsupported audio format")
            }
        }
        val id = UUID.randomUUID().toString()
        val temporary = File(dir, "$id.partial")
        val destination = File(dir, "$id.$extension")
        try {
            var copied = 0L
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temporary).use { output ->
                    val block = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(block)
                        if (count < 0) break
                        copied += count
                        if (copied > MAX_FILE_BYTES) throw IllegalArgumentException(
                            "File exceeds 180 MB. Choose a smaller recording."
                        )
                        output.write(block, 0, count)
                    }
                    output.fd.sync()
                }
            } ?: throw IllegalArgumentException("Cannot open selected audio")
            if (copied < 1024L) throw IllegalArgumentException("Audio file is empty or too short")
            val duration = MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(temporary.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            }
            if (duration <= 0L) throw IllegalArgumentException(
                "The selected file has no readable audio duration"
            )
            if (!temporary.renameTo(destination))
                throw IllegalStateException("Could not save audio to private storage")
            val item = SavedRecording(
                id, safeRecordingTitle(displayName.substringBeforeLast('.', displayName)),
                "", System.currentTimeMillis(), duration, destination.name, copied
            )
            write(item)
            item
        } catch (error: Exception) {
            temporary.delete()
            destination.delete()
            throw error
        }
    }

    suspend fun rename(id: String, title: String, phoneNumber: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = metadata(id)
            val original = if (safeId.matches(id) && file.exists()) read(file) else null
            if (original == null) false else {
                write(original.copy(
                    title = safeRecordingTitle(title),
                    phoneNumber = phoneNumber.filter { it.isDigit() || it == '+' }.take(24)
                ))
                true
            }
        }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!safeId.matches(id)) return@withContext false
        val file = metadata(id)
        val recording = if (file.exists()) read(file) else null
        if (recording == null) return@withContext false
        val audio = audioFile(recording) ?: return@withContext false
        if (!audio.delete()) return@withContext false
        file.delete()
        true
    }

    /** Cleanup is deliberately explicit; no hidden/background deletion. */
    suspend fun cleanupExpired(days: Int): Int = withContext(Dispatchers.IO) {
        if (days == KEEP_FOREVER) return@withContext 0
        require(days == 30 || days == 90)
        val now = System.currentTimeMillis()
        val expired = all().filter { recordingRetentionExpired(it.importedAtMillis, now, days) }
        expired.count { delete(it.id) }
    }
}
