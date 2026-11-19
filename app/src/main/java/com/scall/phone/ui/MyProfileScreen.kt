package com.scall.phone.ui

import android.content.Context
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** A user-managed, device-local S Caller identity, NOT a public caller-ID profile. */
internal data class MyProfile(
    val name: String = "", val phone: String = "", val email: String = "",
    val birthday: String = "", val company: String = "", val profession: String = "",
    val address: String = "", val city: String = "", val state: String = "",
    val postalCode: String = "", val country: String = "", val about: String = "",
    val photoPath: String = ""
)

internal class MyProfileStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("s_caller_my_profile", Context.MODE_PRIVATE)
    private val fields = listOf("name", "phone", "email", "birthday", "company", "profession",
        "address", "city", "state", "postalCode", "country", "about", "photoPath")

    fun load(): MyProfile = try {
        val json = JSONObject(prefs.getString("profile", "{}") ?: "{}")
        fun v(key: String) = json.optString(key).take(2000)
        MyProfile(v("name"), v("phone"), v("email"), v("birthday"),
            v("company"), v("profession"), v("address"), v("city"),
            v("state"), v("postalCode"), v("country"), v("about"), v("photoPath"))
    } catch (_: Exception) { MyProfile() }

    /** Persist only after photo processing succeeds, so a failed photo import cannot corrupt a profile. */
    suspend fun save(profile: MyProfile, selectedPhoto: Uri?): MyProfile = withContext(Dispatchers.IO) {
        require(profile.name.trim().isNotEmpty()) { "Enter your full name" }
        require(profile.name.length <= 120) { "Name is too long" }
        require(profile.email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(profile.email.trim()).matches()) {
            "Enter a valid email address"
        }
        require(profile.phone.isBlank() ||
            (profile.phone.count(Char::isDigit) in 7..15 &&
                profile.phone.all { it.isDigit() || it in "+ -()" })) {
            "Enter a valid phone number"
        }
        require(profile.about.length <= 500) { "About must be under 500 characters" }
        require(fields.all { key ->
            when (key) {
                "name" -> profile.name.length <= 120
                "phone" -> profile.phone.length <= 40
                "email" -> profile.email.length <= 254
                "about" -> profile.about.length <= 500
                "photoPath" -> true
                else -> true
            }
        }) { "Profile details are too long" }
        val photoPath = if (selectedPhoto != null) {
            val destination = File(context.filesDir, "my_profile_photo")
            val temporary = File(context.filesDir, "my_profile_photo.tmp")
            try {
                context.contentResolver.openInputStream(selectedPhoto)?.use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= 8L * 1024 * 1024) { "Choose a photo smaller than 8 MB" }
                            output.write(buffer, 0, read)
                        }
                        require(total > 0) { "Selected photo was empty" }
                    }
                } ?: throw IllegalArgumentException("Cannot read selected photo")
                require(temporary.renameTo(destination)) { "Could not save profile photo" }
                destination.absolutePath
            } finally { temporary.delete() }
        } else profile.photoPath
        val cleaned = profile.copy(
            name = profile.name.trim(), phone = profile.phone.trim(),
            email = profile.email.trim(), photoPath = photoPath
        )
        val json = JSONObject().apply {
            put("name", cleaned.name); put("phone", cleaned.phone); put("email", cleaned.email)
            put("birthday", cleaned.birthday); put("company", cleaned.company)
            put("profession", cleaned.profession); put("address", cleaned.address)
            put("city", cleaned.city); put("state", cleaned.state)
            put("postalCode", cleaned.postalCode); put("country", cleaned.country)
            put("about", cleaned.about); put("photoPath", cleaned.photoPath)
        }
        check(prefs.edit().putString("profile", json.toString()).commit()) {
            "Could not save profile to this device"
        }
        cleaned
    }
}

@Composable
internal fun MyProfileScreen() {
    val context = LocalContext.current
    val store = remember { MyProfileStore(context.applicationContext) }
    var data by remember { mutableStateOf(store.load()) }
    var pickedPhoto by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(data.name.isNotBlank()) }
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        uri -> if (uri != null) { pickedPhoto = uri; saved = false }
    }

    fun field(title: String, value: String, keyboard: KeyboardType = KeyboardType.Text,
              update: (String) -> Unit): @Composable () -> Unit = {
        OutlinedTextField(value = value, onValueChange = {
            update(it); saved = false
        }, label = { Text(title) }, modifier = Modifier.fillMaxWidth(),
            singleLine = title != "About me", maxLines = if (title == "About me") 4 else 1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("My Profile", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Your private S Caller profile, saved on this device.",
                style = MaterialTheme.typography.bodySmall)
        }
        item {
            val photo = pickedPhoto ?: data.photoPath.takeIf { it.isNotBlank() }?.let { File(it) }
            if (photo != null) {
                AsyncImage(model = photo, contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(104.dp).clip(CircleShape))
            } else {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile photo",
                    modifier = Modifier.size(104.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = { photoPicker.launch("image/*") }, enabled = !busy) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(7.dp))
                Text("Choose profile photo")
            }
        }
        item { field("Full name *", data.name) { data = data.copy(name = it) }.invoke() }
        item { field("Mobile number", data.phone, KeyboardType.Phone) { data = data.copy(phone = it) }.invoke() }
        item { field("Email address", data.email, KeyboardType.Email) { data = data.copy(email = it) }.invoke() }
        item { field("Birthday (DD/MM/YYYY)", data.birthday) { data = data.copy(birthday = it) }.invoke() }
        item { field("Company / Organisation", data.company) { data = data.copy(company = it) }.invoke() }
        item { field("Profession / Job title", data.profession) { data = data.copy(profession = it) }.invoke() }
        item { field("Street address", data.address) { data = data.copy(address = it) }.invoke() }
        item { field("City", data.city) { data = data.copy(city = it) }.invoke() }
        item { field("State", data.state) { data = data.copy(state = it) }.invoke() }
        item { field("PIN / Postal code", data.postalCode) { data = data.copy(postalCode = it) }.invoke() }
        item { field("Country", data.country) { data = data.copy(country = it) }.invoke() }
        item { field("About me", data.about) { data = data.copy(about = it.take(500)) }.invoke() }
        item {
            Button(onClick = {
                if (busy) return@Button
                busy = true
                scope.launch {
                    try {
                        data = store.save(data, pickedPhoto)
                        pickedPhoto = null
                        saved = true
                        Toast.makeText(context, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        Toast.makeText(context, error.message ?: "Could not save profile",
                            Toast.LENGTH_LONG).show()
                    } finally { busy = false }
                }
            }, enabled = !busy && data.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (busy) "Saving..." else if (saved) "Update Profile" else "Save Profile")
            }
        }
        item {
            Text("Profile details stay on this device and are not shared with callers.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
