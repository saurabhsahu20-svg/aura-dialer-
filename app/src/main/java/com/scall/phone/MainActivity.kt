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

class MainActivity : ComponentActivity() {
    private lateinit var repository: PhoneRepository
    private var dialNumber by mutableStateOf("")
    private var refreshVersion by mutableIntStateOf(0)

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
        readDialIntent(intent)
        setContent {
            SCallApp(
                repository = repository,
                externalDialNumber = dialNumber,
                refreshVersion = refreshVersion,
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
        refreshVersion++
    }

    private fun readDialIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_DIAL) {
            dialNumber = intent.data?.schemeSpecificPart.orEmpty()
        }
    }

    private fun requestDialerRole() {
        if (repository.isDefaultDialer()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            if (manager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                roleLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        } else {
            @Suppress("DEPRECATION")
            roleLauncher.launch(Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName
            ))
        }
    }

    private fun requestPermissions() {
        val required = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= 33) required += Manifest.permission.POST_NOTIFICATIONS
        permissionsLauncher.launch(required.toTypedArray())
    }
}
