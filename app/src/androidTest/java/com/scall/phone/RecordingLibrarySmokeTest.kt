package com.scall.phone

import android.content.Context
import android.media.MediaPlayer
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scall.phone.data.RecordingStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * End-to-end local library test using a generated silent WAV fixture.
 * No SIM calls, call interception or microphone recording takes place.
 */
@RunWith(AndroidJUnit4::class)
class RecordingLibrarySmokeTest {
    private fun silentWav(): ByteArray {
        val samples = 8000
        val pcmBytes = samples * 2
        val buffer = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + pcmBytes)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // PCM format
        buffer.putShort(1) // PCM audio
        buffer.putShort(1) // mono
        buffer.putInt(8000)
        buffer.putInt(8000 * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmBytes)
        repeat(samples) { buffer.putShort(0) }
        return buffer.array()
    }

    @Test fun consentedAudioImportMetadataPlaybackAndDeletion() = runBlocking {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = File(context.filesDir, "recordings/qa-import-source.wav")
        original.parentFile?.mkdirs()
        original.writeBytes(silentWav())
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.recordings", original
        )
        val store = RecordingStore(context)
        var importedId: String? = null
        try {
            val item = store.importAudio(uri)
            importedId = item.id
            assertTrue(item.durationMillis > 0L)
            assertTrue(item.bytes > 1024L)
            assertTrue(store.playableFile(item)?.isFile == true)
            assertTrue(store.rename(item.id, "QA recording", "+911234567890"))
            val saved = store.all().first { it.id == item.id }
            assertEquals("QA recording", saved.title)
            assertEquals("+911234567890", saved.phoneNumber)
            val player = MediaPlayer()
            try {
                player.setDataSource(store.playableFile(saved)!!.absolutePath)
                player.prepare()
                assertTrue(player.duration > 0)
            } finally {
                player.release()
            }
            assertTrue(store.delete(item.id))
            assertTrue(store.all().none { it.id == item.id })
        } finally {
            importedId?.let { store.delete(it) }
            original.delete()
        }
    }
}
