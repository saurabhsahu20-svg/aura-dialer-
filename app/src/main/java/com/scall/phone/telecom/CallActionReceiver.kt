package com.scall.phone.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Explicit, non-exported notification actions. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ANSWER" -> CallBridge.answer()
            "REJECT" -> CallBridge.reject()
            "END" -> CallBridge.disconnect()
        }
    }
}
