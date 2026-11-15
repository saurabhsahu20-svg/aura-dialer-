package com.scall.phone.data

/** Requires an explicit Telecom account handle for a user-chosen SIM. */
internal enum class CallRoute { SPECIFIC_ACCOUNT, ASK_USER, ANDROID_DEFAULT, SETUP_REQUIRED }

internal fun chooseCallRoute(preference: String, accountKeys: List<String>, visibleSimCount: Int): CallRoute {
    if (preference == SimPreferences.SYSTEM) return CallRoute.ANDROID_DEFAULT
    if (preference != SimPreferences.ASK) {
        return when {
            preference in accountKeys -> CallRoute.SPECIFIC_ACCOUNT
            accountKeys.isNotEmpty() -> CallRoute.ASK_USER
            else -> CallRoute.SETUP_REQUIRED
        }
    }
    return when {
        accountKeys.size == 1 -> CallRoute.SPECIFIC_ACCOUNT
        accountKeys.size > 1 -> CallRoute.ASK_USER
        visibleSimCount > 1 -> CallRoute.SETUP_REQUIRED
        else -> CallRoute.ANDROID_DEFAULT
    }
}
