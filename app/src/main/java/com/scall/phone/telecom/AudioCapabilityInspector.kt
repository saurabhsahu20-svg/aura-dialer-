package com.scall.phone.telecom

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor

/**
 * Capability inspection never opens a microphone, reads call audio or creates
 * an AudioEffect. AudioRecord's audio session is required to enable these
 * effects; standard carrier-call audio sessions belong to Android/firmware.
 */
internal data class AudioCapabilities(
    val noiseSuppressor: Boolean,
    val echoCanceler: Boolean,
    val autoGainControl: Boolean
)

internal object AudioCapabilityInspector {
    fun inspect(): AudioCapabilities = AudioCapabilities(
        noiseSuppressor = runCatching { NoiseSuppressor.isAvailable() }.getOrDefault(false),
        echoCanceler = runCatching { AcousticEchoCanceler.isAvailable() }.getOrDefault(false),
        autoGainControl = runCatching { AutomaticGainControl.isAvailable() }.getOrDefault(false)
    )
}

/** The dialer never claims an AI/audio effect is active for a SIM call. */
internal fun cellularAudioProcessingLabel(): String =
    "Cellular voice processing: Android, phone hardware & carrier controlled"
