package com.scall.phone.telecom

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.scall.phone.R
import com.scall.phone.data.RecordingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Only elevated, OEM-authorized builds with the Android CAPTURE_AUDIO_OUTPUT
 * signature permission can access native two-way cellular call audio.
 * Ordinary third-party dialers MUST NOT try to emulate this with microphone
 * recording, accessibility services, or Android permission bypasses.
 */
internal enum class ManualRecordAccess {
    NEED_ACTIVE_CALL, SYSTEM_AUDIO_UNAVAILABLE, NEED_MIC_PERMISSION, AVAILABLE
}

internal fun manualRecordingAccess(
    active: Boolean, hasSystemAudioGrant: Boolean, hasMicrophonePermission: Boolean
): ManualRecordAccess = when {
    !active -> ManualRecordAccess.NEED_ACTIVE_CALL
    !hasSystemAudioGrant -> ManualRecordAccess.SYSTEM_AUDIO_UNAVAILABLE
    !hasMicrophonePermission -> ManualRecordAccess.NEED_MIC_PERMISSION
    else -> ManualRecordAccess.AVAILABLE
}

internal data class RecordingStatus(
    val phase: Phase = Phase.IDLE,
    val startedElapsedMs: Long = 0L,
    val message: String = ""
) {
    enum class Phase { IDLE, STARTING, RECORDING, SAVING, SAVED, ERROR }
}

internal object ManualCallRecording {
    private val mutable = MutableStateFlow(RecordingStatus())
    val status = mutable.asStateFlow()

    fun access(context: Context, callState: Int?): ManualRecordAccess =
        manualRecordingAccess(
            active = callState == Call.STATE_ACTIVE,
            hasSystemAudioGrant = ContextCompat.checkSelfPermission(
                context, "android.permission.CAPTURE_AUDIO_OUTPUT"
            ) == PackageManager.PERMISSION_GRANTED,
            hasMicrophonePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )

    fun begin(context: Context) {
        // Called ONLY from the user's explicit confirmed tap in the foreground UI.
        if (access(context, CallBridge.current.value?.state) != ManualRecordAccess.AVAILABLE) {
            mutable.value = RecordingStatus(
                RecordingStatus.Phase.ERROR, message = "Native call recording is not available"
            )
            return
        }
        if (mutable.value.phase in listOf(
                RecordingStatus.Phase.STARTING,
                RecordingStatus.Phase.RECORDING,
                RecordingStatus.Phase.SAVING
            )) return
        mutable.value = RecordingStatus(RecordingStatus.Phase.STARTING)
        try {
            ContextCompat.startForegroundService(
                context, Intent(context, ManualCallRecordingService::class.java)
                    .setAction(ManualCallRecordingService.START)
            )
        } catch (error: RuntimeException) {
            android.util.Log.e("SCallRecording", "Unable to start recording service", error)
            mutable.value = RecordingStatus(
                RecordingStatus.Phase.ERROR, message = "Recording could not start"
            )
        }
    }

    fun stop(context: Context) {
        if (mutable.value.phase != RecordingStatus.Phase.RECORDING) return
        try {
            context.startService(
                Intent(context, ManualCallRecordingService::class.java)
                    .setAction(ManualCallRecordingService.STOP)
            )
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallRecording", "Unable to request stop", error)
        }
    }

    internal fun update(newStatus: RecordingStatus) {
        mutable.value = newStatus
    }
}

class ManualCallRecordingService : Service() {
    companion object {
        const val START = "com.scall.phone.recording.START_MANUALLY"
        const val STOP = "com.scall.phone.recording.STOP_MANUALLY"
        private const val CHANNEL = "s_call_manual_recording"
        private const val NOTIFICATION = 4103
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaRecorder: MediaRecorder? = null
    private var temporary: File? = null
    private var number = ""
    private var startedAt = 0L
    private var finalizing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            CallBridge.current.collect { live ->
                if (mediaRecorder != null && live?.state != Call.STATE_ACTIVE) finishRecording()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START -> startManualCapture()
            STOP -> finishRecording()
            else -> stopSelf()
        }
        // No restart: never automatically record after service/process recreation.
        return START_NOT_STICKY
    }

    private fun startManualCapture() {
        if (mediaRecorder != null || finalizing) return
        if (ManualCallRecording.access(this, CallBridge.current.value?.state) !=
            ManualRecordAccess.AVAILABLE
        ) {
            ManualCallRecording.update(RecordingStatus(
                RecordingStatus.Phase.ERROR, message = "Call audio capture unavailable"
            ))
            stopSelf()
            return
        }

        val active = CallBridge.current.value ?: run { stopSelf(); return }
        number = active.number
        startedAt = System.currentTimeMillis()
        val folder = File(filesDir, "recordings").apply { mkdirs() }
        val output = File(folder, "${UUID.randomUUID()}.partial")
        temporary = output
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Manual call recording",
                        NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Visible only while the user has manually started recording"
                    }
                )
            }
            val stopAction = PendingIntent.getService(
                this, 23, Intent(this, ManualCallRecordingService::class.java).setAction(STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_call)
                .setContentTitle("S Caller: Call recording active")
                .setContentText("Manually started • Tap Stop to save")
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(0, "Stop recording", stopAction)
                .build()
            startForeground(NOTIFICATION, notification)
            @Suppress("DEPRECATION")
            val recorder = MediaRecorder()
            mediaRecorder = recorder
            recorder.apply {
                // Android protects this source. It is not accessible to ordinary APKs.
                setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(output.absolutePath)
                prepare()
            }
            recorder.start()
            ManualCallRecording.update(RecordingStatus(
                RecordingStatus.Phase.RECORDING, SystemClock.elapsedRealtime()
            ))
        } catch (error: Exception) {
            android.util.Log.e("SCallRecording", "Native recording failed", error)
            cleanupIncomplete()
            ManualCallRecording.update(RecordingStatus(
                RecordingStatus.Phase.ERROR, message = "Recording is unsupported or could not start"
            ))
            leaveForeground()
            stopSelf()
        }
    }

    private fun leaveForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun cleanupIncomplete() {
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        temporary?.delete()
        temporary = null
    }

    private fun finishRecording() {
        if (finalizing) return
        val recorder = mediaRecorder ?: run { stopSelf(); return }
        finalizing = true
        ManualCallRecording.update(RecordingStatus(RecordingStatus.Phase.SAVING))
        val recordingFile = temporary
        mediaRecorder = null
        temporary = null
        val stopped = try {
            recorder.stop()
            true
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallRecording", "Audio too short or interrupted", error)
            false
        } finally {
            runCatching { recorder.release() }
        }
        leaveForeground()
        if (!stopped || recordingFile == null) {
            recordingFile?.delete()
            ManualCallRecording.update(RecordingStatus(
                RecordingStatus.Phase.ERROR, message = "Recording ended without valid audio"
            ))
            stopSelf()
            return
        }
        val savedNumber = number
        val savedStartedAt = startedAt
        scope.launch {
            try {
                val item = RecordingStore(applicationContext)
                    .saveManualCapture(recordingFile, savedNumber, savedStartedAt)
                ManualCallRecording.update(RecordingStatus(
                    RecordingStatus.Phase.SAVED, message = "Recording saved: ${item.title}"
                ))
            } catch (error: Exception) {
                android.util.Log.w("SCallRecording", "Could not verify recording", error)
                recordingFile.delete()
                ManualCallRecording.update(RecordingStatus(
                    RecordingStatus.Phase.ERROR, message = "Recording was not saved: invalid audio"
                ))
            } finally {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        // A removed service cannot leave an active microphone or stale file.
        cleanupIncomplete()
        scope.cancel()
        super.onDestroy()
    }
}
