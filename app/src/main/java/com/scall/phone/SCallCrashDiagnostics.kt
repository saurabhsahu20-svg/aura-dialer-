package com.scall.phone

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stores a short local stack trace for crashes that occur on actual phones.
 * Exception messages are deliberately omitted so phone numbers, names and
 * other user-provided details are not copied into support diagnostics.
 * Nothing is uploaded to any server.
 */
internal object SCallCrashDiagnostics {
    private const val SETTINGS = "s_call_crash_diagnostics"
    private const val LAST = "last_crash"
    private val installed = AtomicBoolean(false)

    fun install(application: Application) {
        if (!installed.compareAndSet(false, true)) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                application.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
                    .edit().putString(LAST, render(error, Build.VERSION.SDK_INT)).commit()
            } catch (_: Throwable) {
                // Never replace the system's real crash handling.
            } finally {
                if (previous != null) {
                    previous.uncaughtException(thread, error)
                } else {
                    Process.killProcess(Process.myPid())
                    kotlin.system.exitProcess(10)
                }
            }
        }
    }

    fun last(context: Context): String? =
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
            .getString(LAST, null)?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
            .edit().remove(LAST).apply()
    }

    internal fun render(error: Throwable, androidApi: Int): String = buildString {
        appendLine("S Caller local crash diagnostics")
        appendLine("Android API: $androidApi")
        var current: Throwable? = error
        var causes = 0
        while (current != null && causes++ < 4) {
            // Do NOT print throwable.message: it may include a phone number/contact.
            appendLine("Exception: ${current.javaClass.name}")
            current.stackTrace.take(45).forEach { frame ->
                appendLine("  at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
            }
            current = current.cause
        }
    }.take(12_000)
}

class SCallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SCallCrashDiagnostics.install(this)
    }
}
