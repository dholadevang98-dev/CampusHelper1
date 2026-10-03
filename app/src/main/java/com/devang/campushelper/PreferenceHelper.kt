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

    var canteenSpecialItem: String
        get() = prefs.getString("key_canteen_special_item", "Deluxe Gujarati Thali ₹80 • Chai Combo ₹30") ?: "Deluxe Gujarati Thali ₹80 • Chai Combo ₹30"
        set(value) = prefs.edit().putString("key_canteen_special_item", value).apply()

    var broadcastTitle: String
        get() = prefs.getString(KEY_BROADCAST_TITLE, "Mid-Semester Examination Schedule Released") ?: "Mid-Semester Examination Schedule Released"
        set(value) = prefs.edit().putString(KEY_BROADCAST_TITLE, value).apply()

    var broadcastMessage: String
        get() = prefs.getString(KEY_BROADCAST_MSG, "GTU mid-semester exams commence from 15th October. Hall tickets available on portal.") ?: "GTU mid-semester exams commence from 15th October. Hall tickets available on portal."
        set(value) = prefs.edit().putString(KEY_BROADCAST_MSG, value).apply()

    var broadcastPriority: String
        get() = prefs.getString("key_broadcast_priority", "🚨 HIGH PRIORITY") ?: "🚨 HIGH PRIORITY"
        set(value) = prefs.edit().putString("key_broadcast_priority", value).apply()

    var isBroadcastActive: Boolean
        get() = prefs.getBoolean(KEY_BROADCAST_ACTIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_BROADCAST_ACTIVE, value).apply()

    var adminPin: String
        get() = prefs.getString(KEY_ADMIN_PIN, "620620") ?: "620620"
        set(value) = prefs.edit().putString(KEY_ADMIN_PIN, value).apply()

    var liveLectureSubject: String
        get() = prefs.getString("key_live_lecture_subject", "Data Structures & Algorithms") ?: "Data Structures & Algorithms"
        set(value) = prefs.edit().putString("key_live_lecture_subject", value).apply()

    var liveLectureRoom: String
        get() = prefs.getString("key_live_lecture_room", "Lab 302") ?: "Lab 302"
        set(value) = prefs.edit().putString("key_live_lecture_room", value).apply()

    var liveLectureFaculty: String
        get() = prefs.getString("key_live_lecture_faculty", "Prof. Mehta") ?: "Prof. Mehta"
        set(value) = prefs.edit().putString("key_live_lecture_faculty", value).apply()

    var liveLectureTiming: String
        get() = prefs.getString("key_live_lecture_timing", "11:00 AM - 12:00 PM") ?: "11:00 AM - 12:00 PM"
        set(value) = prefs.edit().putString("key_live_lecture_timing", value).apply()

    var liveLectureCountdown: String
        get() = prefs.getString("key_live_lecture_countdown", "Starts in 18 mins") ?: "Starts in 18 mins"
        set(value) = prefs.edit().putString("key_live_lecture_countdown", value).apply()

    var timetableTermDates: String
        get() = prefs.getString("key_timetable_term_dates", "Term: Odd 2026-27 (WEF: 07/08/2026)") ?: "Term: Odd 2026-27 (WEF: 07/08/2026)"
        set(value) = prefs.edit().putString("key_timetable_term_dates", value).apply()

    var timetableClassrooms: String
        get() = prefs.getString("key_timetable_classrooms", "Sem 5-A (Room 101), Sem 5-B (Room 102), Sem 5-C (Room 103)") ?: "Sem 5-A (Room 101), Sem 5-B (Room 102), Sem 5-C (Room 103)"
        set(value) = prefs.edit().putString("key_timetable_classrooms", value).apply()

    var timetableLabs: String
        get() = prefs.getString("key_timetable_labs", "APL-1, APL-2, BPL-1 (Lab 302, Lab 304)") ?: "APL-1, APL-2, BPL-1 (Lab 302, Lab 304)"
        set(value) = prefs.edit().putString("key_timetable_labs", value).apply()

    var timetableWeeklyHours: String
        get() = prefs.getString("key_timetable_weekly_hours", "30 Hours/Week (12 Th + 18 Prac)") ?: "30 Hours/Week (12 Th + 18 Prac)"
        set(value) = prefs.edit().putString("key_timetable_weekly_hours", value).apply()

    var customTimetablePdfPath: String
        get() = prefs.getString("key_custom_timetable_pdf_path", "") ?: ""
        set(value) = prefs.edit().putString("key_custom_timetable_pdf_path", value).apply()

    var customTimetablePdfName: String
        get() = prefs.getString("key_custom_timetable_pdf_name", "") ?: ""
        set(value) = prefs.edit().putString("key_custom_timetable_pdf_name", value).apply()

    var geminiApiKey: String
        get() = prefs.getString("key_gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("key_gemini_api_key", value).apply()

    fun getCustomSyllabusPdf(subjectCode: String): String {
        return prefs.getString("key_custom_syllabus_pdf_${subjectCode.trim().uppercase()}", "") ?: ""
    }

    fun setCustomSyllabusPdf(subjectCode: String, path: String) {
        prefs.edit().putString("key_custom_syllabus_pdf_${subjectCode.trim().uppercase()}", path).apply()
    }

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
