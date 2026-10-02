package com.devang.campushelper

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "CampusHelper_Prefs"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_PHONE = "key_user_phone"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_ENROLLMENT_NO = "key_enrollment_no"
        private const val KEY_DEPARTMENT = "key_department"
        private const val KEY_IS_VERIFIED = "key_is_verified"
        private const val KEY_COLLEGE_NAME = "key_college_name"
        private const val KEY_CANTEEN_IS_OPEN = "key_canteen_is_open"
        private const val KEY_CANTEEN_RUSH = "key_canteen_rush"
        private const val KEY_BROADCAST_TITLE = "key_broadcast_title"
        private const val KEY_BROADCAST_MSG = "key_broadcast_msg"
        private const val KEY_BROADCAST_ACTIVE = "key_broadcast_active"
        private const val KEY_ADMIN_PIN = "key_admin_pin"
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Student") ?: "Student"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userPhone: String
        get() = prefs.getString(KEY_USER_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "STUDENT") ?: "STUDENT"
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var userId: String
        get() = prefs.getString(KEY_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var enrollmentNo: String
        get() = prefs.getString(KEY_ENROLLMENT_NO, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ENROLLMENT_NO, value).apply()

    var department: String
        get() = prefs.getString(KEY_DEPARTMENT, "Government Polytechnic, Rajkot") ?: "Government Polytechnic, Rajkot"
        set(value) = prefs.edit().putString(KEY_DEPARTMENT, value).apply()

    var isVerified: Boolean
        get() = prefs.getBoolean(KEY_IS_VERIFIED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_VERIFIED, value).apply()

    var collegeName: String
        get() = prefs.getString(KEY_COLLEGE_NAME, "Government Polytechnic, Rajkot") ?: "Government Polytechnic, Rajkot"
        set(value) = prefs.edit().putString(KEY_COLLEGE_NAME, value).apply()

    var pendingEmailForSignIn: String
        get() = prefs.getString("key_pending_email_signin", "") ?: ""
        set(value) = prefs.edit().putString("key_pending_email_signin", value).apply()

    // ==================== LIVE ADMIN CONTROLS ====================

    var canteenIsOpen: Boolean
        get() = prefs.getBoolean(KEY_CANTEEN_IS_OPEN, true)
        set(value) = prefs.edit().putBoolean(KEY_CANTEEN_IS_OPEN, value).apply()

    var canteenRushStatus: String
        get() = prefs.getString(KEY_CANTEEN_RUSH, "🟢 Low Rush (4 min)") ?: "🟢 Low Rush (4 min)"
        set(value) = prefs.edit().putString(KEY_CANTEEN_RUSH, value).apply()

    var broadcastTitle: String
        get() = prefs.getString(KEY_BROADCAST_TITLE, "Mid-Semester Examination Schedule Released") ?: "Mid-Semester Examination Schedule Released"
        set(value) = prefs.edit().putString(KEY_BROADCAST_TITLE, value).apply()

    var broadcastMessage: String
        get() = prefs.getString(KEY_BROADCAST_MSG, "GTU mid-semester exams commence from 15th October. Hall tickets available on portal.") ?: "GTU mid-semester exams commence from 15th October. Hall tickets available on portal."
        set(value) = prefs.edit().putString(KEY_BROADCAST_MSG, value).apply()

    var isBroadcastActive: Boolean
        get() = prefs.getBoolean(KEY_BROADCAST_ACTIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_BROADCAST_ACTIVE, value).apply()

    var adminPin: String
        get() = prefs.getString(KEY_ADMIN_PIN, "620620") ?: "620620"
        set(value) = prefs.edit().putString(KEY_ADMIN_PIN, value).apply()

    /**
     * Save complete user login session with GP Rajkot verification data
     */
    fun saveUserSession(
        name: String,
        email: String = "",
        phone: String = "",
        role: String = "STUDENT",
        uid: String = "",
        enrollmentNo: String = "",
        department: String = "Government Polytechnic, Rajkot",
        isVerified: Boolean = true
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PHONE, phone)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_ID, uid)
            .putString(KEY_ENROLLMENT_NO, enrollmentNo)
            .putString(KEY_DEPARTMENT, department)
            .putBoolean(KEY_IS_VERIFIED, isVerified)
            .putString(KEY_COLLEGE_NAME, "Government Polytechnic, Rajkot")
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
