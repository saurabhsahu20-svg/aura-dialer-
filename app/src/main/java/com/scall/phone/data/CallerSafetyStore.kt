package com.scall.phone.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.BlockedNumberContract
import android.telecom.TelecomManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local user labels, never a speculative online spam reputation score.
 * The Android system's blocked-number provider is the only authority for
 * whether inbound calls are actually blocked.
 */
internal class CallerSafetyStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("s_caller_safety_labels_v1", Context.MODE_PRIVATE)
    private fun key(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    /** 1 = user-marked spam; 2 = user-marked safe; 0 = unclassified. */
    fun label(number: String): Int {
        val k = key(number)
        return if (k.length < 7) 0 else prefs.getInt(k, 0).coerceIn(0, 2)
    }

    fun setLabel(number: String, kind: Int): Boolean {
        val k = key(number)
        if (k.length < 7 || kind !in 0..2) return false
        return prefs.edit().apply {
            if (kind == 0) remove(k) else putInt(k, kind)
        }.commit()
    }

    /**
     * Android can block numbers when this app is the default dialer and
     * BlockedNumberContract accepts the request. No overlay, no pretend block.
     */
    suspend fun block(number: String): Boolean = withContext(Dispatchers.IO) {
        val raw = number.trim()
        if (raw.count(Char::isDigit) < 7) return@withContext false
        if (!canManageBlockedNumbers()) return@withContext false
        runCatching {
            context.contentResolver.insert(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                ContentValues().apply {
                    put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, raw)
                }
            ) != null
        }.getOrDefault(false)
    }

    suspend fun unblock(number: String): Boolean = withContext(Dispatchers.IO) {
        if (!canManageBlockedNumbers()) return@withContext false
        runCatching {
            context.contentResolver.delete(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER}=?",
                arrayOf(number.trim())
            ) > 0
        }.getOrDefault(false)
    }

    suspend fun isBlocked(number: String): Boolean = withContext(Dispatchers.IO) {
        if (!canManageBlockedNumbers()) return@withContext false
        runCatching { BlockedNumberContract.isBlocked(context, number.trim()) }.getOrDefault(false)
    }

    fun canManageBlockedNumbers(): Boolean = if (Build.VERSION.SDK_INT < 24) false else {
        runCatching {
            val manager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            manager.defaultDialerPackage == context.packageName &&
                BlockedNumberContract.canCurrentUserBlockNumbers(context)
        }.getOrDefault(false)
    }
}
