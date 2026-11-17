package com.scall.phone

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
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
import com.scall.phone.ui.SCallApp
import com.scall.phone.ui.nextExternalDialRequestId

class MainActivity : ComponentActivity() {
    private lateinit var repository: PhoneRepository
    private var dialNumber by mutableStateOf("")
    private var dialIntentVersion by mutableIntStateOf(0)
    private var refreshVersion by mutableIntStateOf(0)
    private var lastResumeAt = 0L

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshVersion++ }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshVersion++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        repository = PhoneRepository(applicationContext)
        com.scall.phone.telecom.MissedCallBadges.refresh(applicationContext)
        readDialIntent(intent)
        setContent {
            SCallApp(
                repository = repository,
                externalDialNumber = dialNumber,
                externalDialRequestId = dialIntentVersion,
                refreshVersion = refreshVersion,
                requestDialerRole = ::requestDialerRole,
                requestPermissions = ::requestPermissions,
                requestContactPermissions = ::requestContactPermissions
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
        val now = android.os.SystemClock.elapsedRealtime()
        // onCreate already loads records. Do not fire a duplicate provider scan
        // when Activity immediately enters onResume, or on quick app transitions.
        if (lastResumeAt > 0L && now - lastResumeAt >= 1000L) {
            refreshVersion++
            com.scall.phone.telecom.MissedCallBadges.refresh(applicationContext)
        }
        lastResumeAt = now
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

    private fun requestContactPermissions() {
        try {
            permissionsLauncher.launch(arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS
            ))
        } catch (error: RuntimeException) {
            android.util.Log.w("SCall", "Contact permission request unavailable", error)
            android.widget.Toast.makeText(this, "Enable Contacts access in Android settings",
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
