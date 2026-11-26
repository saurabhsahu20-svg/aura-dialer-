package com.scall.phone

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.scall.phone.data.PhoneRepository
import com.scall.phone.telecom.IncomingCallVisibility
import com.scall.phone.ui.SCallApp
import com.scall.phone.ui.nextExternalDialRequestId

class MainActivity : ComponentActivity() {
    private lateinit var repository: PhoneRepository
    private var dialNumber by mutableStateOf("")
    private var dialIntentVersion by mutableIntStateOf(0)
    private var refreshVersion by mutableIntStateOf(0)
    private var callLogRefreshVersion by mutableIntStateOf(0)
    private val historyRefreshHandler = Handler(Looper.getMainLooper())
    private val historyRefresh = Runnable { callLogRefreshVersion++ }
    private var observingCallLog = false
    private val callLogObserver = object : ContentObserver(historyRefreshHandler) {
        override fun onChange(selfChange: Boolean) {
            // CallLog providers may dispatch several events for one insertion.
            // One debounced query prevents duplicate alerts and UI jank.
            historyRefreshHandler.removeCallbacks(historyRefresh)
            historyRefreshHandler.postDelayed(historyRefresh, 400L)
        }
    }

    private fun observeCallLogIfPermitted() {
        if (observingCallLog ||
            checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI, true, callLogObserver
            )
            observingCallLog = true
        } catch (error: RuntimeException) {
            android.util.Log.w("SCall", "Call history observer unavailable", error)
        }
    }
    private var lastResumeAt = 0L

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshVersion++ }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshVersion++
        observeCallLogIfPermitted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        repository = PhoneRepository(applicationContext)
        // Make incoming-call alert channel configurable even before the first
        // ringing call. Never alter a channel whose importance user customized.
        try {
            IncomingCallVisibility.ensureChannel(this)
        } catch (error: RuntimeException) {
            android.util.Log.w("SCall", "Incoming call alert channel setup unavailable", error)
        }
        readDialIntent(intent)
        setContent {
            SCallApp(
                repository = repository,
                externalDialNumber = dialNumber,
                externalDialRequestId = dialIntentVersion,
                refreshVersion = refreshVersion,
                callLogRefreshVersion = callLogRefreshVersion,
                requestDialerRole = ::requestDialerRole,
                requestPermissions = ::requestPermissions
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readDialIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        observeCallLogIfPermitted()
        val now = android.os.SystemClock.elapsedRealtime()
        // onCreate already loads records. Do not fire a duplicate provider scan
        // when Activity immediately enters onResume, or on quick app transitions.
        if (lastResumeAt > 0L && now - lastResumeAt >= 1000L) {
            refreshVersion++
        }
        lastResumeAt = now
    }

    override fun onStop() {
        historyRefreshHandler.removeCallbacks(historyRefresh)
        if (observingCallLog) {
            runCatching { contentResolver.unregisterContentObserver(callLogObserver) }
            observingCallLog = false
        }
        super.onStop()
    }

    private fun readDialIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_DIAL) {
            // A repeated ACTION_DIAL with the same number must still open the keypad.
            // A state change to the text alone does not trigger Compose if it is equal.
            dialNumber = intent.data?.schemeSpecificPart.orEmpty()
            dialIntentVersion = nextExternalDialRequestId(dialIntentVersion)
        }
    }

    private fun requestDialerRole() {
        if (repository.isDefaultDialer()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val manager = getSystemService(RoleManager::class.java)
                if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    roleLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                }
            } else {
                @Suppress("DEPRECATION")
                roleLauncher.launch(Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName
                ))
            }
        } catch (error: RuntimeException) {
            android.util.Log.e("SCall", "Default Phone app settings unavailable", error)
            android.widget.Toast.makeText(this,
                "Choose S Caller under Settings > Apps > Default apps > Phone",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissions() {
        val required = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )
        // Call log editing is only supported while S call owns the default-dialer role.
        if (repository.isDefaultDialer()) required += Manifest.permission.WRITE_CALL_LOG
        if (Build.VERSION.SDK_INT >= 28 && repository.isDefaultDialer()) {
            required += Manifest.permission.ANSWER_PHONE_CALLS
        }
        if (Build.VERSION.SDK_INT >= 33) required += Manifest.permission.POST_NOTIFICATIONS
        try {
            permissionsLauncher.launch(required.toTypedArray())
        } catch (error: RuntimeException) {
            android.util.Log.e("SCall", "Unable to request phone permissions", error)
            android.widget.Toast.makeText(this, "Allow Phone access in Android app settings",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
