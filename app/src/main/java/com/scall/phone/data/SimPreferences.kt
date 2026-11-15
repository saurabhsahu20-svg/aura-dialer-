package com.scall.phone.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.PhoneAccountHandle

/** Stable per-app preferences. Only the mobile OS can change system data/SMS defaults. */
internal class SimPreferences(context: Context) {
    private val storage = context.getSharedPreferences("s_call_sim_prefs", Context.MODE_PRIVATE)

    fun voiceChoice(): String = storage.getString("voice_choice", ASK) ?: ASK
    fun smsChoice(): String = storage.getString("sms_choice", ASK) ?: ASK
    fun setVoiceChoice(choice: String) = storage.edit().putString("voice_choice", choice).apply()
    fun setSmsChoice(choice: String) = storage.edit().putString("sms_choice", choice).apply()

    companion object {
        const val ASK = "ask"
        const val SYSTEM = "system"
        fun accountKey(handle: PhoneAccountHandle): String =
            "${handle.componentName.flattenToString()}::${handle.id}"
        fun smsKey(subscriptionId: Int): String = "sms:$subscriptionId"
    }
}

internal data class SmsSubscription(val subscriptionId: Int, val slot: Int, val label: String) {
    val key: String get() = SimPreferences.smsKey(subscriptionId)
}

/** Open the system SIM/network settings: apps without carrier privileges cannot change data SIM. */
internal fun Context.openMobileNetworkSettings() {
    try {
        startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { /* Some managed devices block Settings intents. */ }
    }
}
