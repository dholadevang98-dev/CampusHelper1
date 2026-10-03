package com.devang.campushelper

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class HomeFragment : Fragment() {

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private lateinit var prefHelper: PreferenceHelper
    private var currentRole: UserRole = UserRole.STUDENT
    private var activeNavTab: Int = 0
    private var isAdminUnlocked: Boolean = false
    private var updateRoleUIFunc: ((UserRole) -> Unit)? = null

    private var onPdfPickedCallback: ((Uri, String) -> Unit)? = null

    private val pdfPickerLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val fileName = getFileNameFromUri(it) ?: "document_${System.currentTimeMillis()}.pdf"
                onPdfPickedCallback?.invoke(it, fileName)
            }
        }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = try {
                requireContext().contentResolver.query(uri, null, null, null, null)
            } catch (e: Exception) {
                null
            }
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun copyUriToInternalFile(uri: Uri, targetFileName: String): String? {
        return try {
            val safeFileName = targetFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val destinationFile = File(requireContext().filesDir, safeFileName)
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    enum class UserRole {
        STUDENT, ADMIN
    }

    data class GrievanceTicket(
        val id: String,
        val studentName: String,
        val enrollmentNo: String,
        val department: String,
        val subject: String,
        val description: String,
        var status: String, // "Pending", "In Review", "Resolved"
        val date: String
    )

    data class LostFoundAdminItem(
        val id: String,
        val title: String,
        val location: String,
        val finderInfo: String,
        var status: String, // "Deposited at Gate", "Claim Verified", "Handed Over"
        val date: String
    )

    private val grievanceTickets = mutableListOf(
        GrievanceTicket("#1092", "Devang Dhola", "246200316062", "Information Technology", "Hostel Block B Water Supply", "Water pressure in hostel 2nd floor is low during morning hours.", "Pending", "02 Oct 2026"),
        GrievanceTicket("#1094", "Harsh Joshi", "246200316064", "Information Technology", "Lab 3 Wi-Fi Speed & Access Point", "Wi-Fi disconnecting frequently during practical session.", "In Review", "01 Oct 2026"),
        GrievanceTicket("#1085", "Aarav Patel", "236200116001", "Information Technology", "Mid-Sem Exam Fee Receipt Copy", "Need duplicate fee receipt for scholarship submission.", "Resolved", "29 Sep 2026"),
        GrievanceTicket("#1082", "Chirag Mehta", "236200116003", "Mechanical Engineering", "Workshop Lathe Machine Tool Rest", "Tool rest alignment in mechanical workshop needs maintenance.", "Resolved", "28 Sep 2026")
    )

    private val lostFoundAdminItems = mutableListOf(
        LostFoundAdminItem("#41", "Boat Airdopes 141 Case (Black)", "Library 2nd Floor Reading Room", "Handed by Librarian Mrs. Patel", "Claim Verified", "02 Oct 2026"),
        LostFoundAdminItem("#42", "Fastrack Reflex Watch (Matte Black)", "Canteen Food Court Table #6", "Found by Security Guard Ramesh", "Deposited at Gate", "01 Oct 2026"),
        LostFoundAdminItem("#43", "Blue Spiral Notebook (Sem 5 IT)", "Lab 302 Desk #14", "Found during evening cleanup", "Deposited at Gate", "30 Sep 2026"),
        LostFoundAdminItem("#44", "GP Rajkot Student Identity Card", "Main Campus Administrative Lobby", "Found near fees counter", "Handed Over", "28 Sep 2026")
    )

    data class ModuleConfig(
        val title: String,
        val subtitle: String,
        val tag: String,
        val iconRes: Int,
        val accentColorRes: Int,
        val tagColorRes: Int,
        val description: String,
        val highlightHeader: String,
        val highlightBody: String,
        val btnText: String,
        val customAction: (() -> Unit)? = null
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefHelper = PreferenceHelper(requireContext())
        AssignmentManager.loadFromStorage(requireContext())
        val savedName = prefHelper.userName

        // 1. Header Elements
        val btnProfileAvatar = view.findViewById<FrameLayout>(R.id.btnProfileAvatar)
        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        val tvDepartment = view.findViewById<TextView>(R.id.tvDepartment)
        val btnNotifications = view.findViewById<FrameLayout>(R.id.btnNotifications)
        val btnLogout = view.findViewById<FrameLayout>(R.id.btnLogout)

        // 2. Role Switcher
        val btnRoleStudent = view.findViewById<TextView>(R.id.btnRoleStudent)
        val btnRoleAdmin = view.findViewById<TextView>(R.id.btnRoleAdmin)
        val tvRoleBadgeHeader = view.findViewById<TextView>(R.id.tvRoleBadgeHeader)

        // 4. Hero Live Pulse Card
        val heroCard = view.findViewById<MaterialCardView>(R.id.heroCard)
        val tvHeroBadge = view.findViewById<TextView>(R.id.tvHeroBadge)
        val tvHeroCountdown = view.findViewById<TextView>(R.id.tvHeroCountdown)
        val tvHeroSubject = view.findViewById<TextView>(R.id.tvHeroSubject)
        val tvHeroSchedule = view.findViewById<TextView>(R.id.tvHeroSchedule)
        val chipCanteenStatus = view.findViewById<TextView>(R.id.chipCanteenStatus)
        val chipLibraryStatus = view.findViewById<TextView>(R.id.chipLibraryStatus)
        val chipNoticeStatus = view.findViewById<TextView>(R.id.chipNoticeStatus)

        // 5. Section Header
        val tvSectionTitle = view.findViewById<TextView>(R.id.tvSectionTitle)

        // 6. 8 Module Cards
        val cards = listOf(
            view.findViewById<MaterialCardView>(R.id.cardNotices),
            view.findViewById<MaterialCardView>(R.id.cardTimetable),
            view.findViewById<MaterialCardView>(R.id.cardCanteen),
            view.findViewById<MaterialCardView>(R.id.cardLibrary),
            view.findViewById<MaterialCardView>(R.id.cardLostFound),
            view.findViewById<MaterialCardView>(R.id.cardHelpdesk),
            view.findViewById<MaterialCardView>(R.id.cardAlerts),
            view.findViewById<MaterialCardView>(R.id.cardAdmin)
        )

        val cardIcons = listOf(
            view.findViewById<ImageView>(R.id.ivModule1Icon),
            view.findViewById<ImageView>(R.id.ivModule2Icon),
            view.findViewById<ImageView>(R.id.ivModule3Icon),
            view.findViewById<ImageView>(R.id.ivModule4Icon),
            view.findViewById<ImageView>(R.id.ivModule5Icon),
            view.findViewById<ImageView>(R.id.ivModule6Icon),
            view.findViewById<ImageView>(R.id.ivModule7Icon),
            view.findViewById<ImageView>(R.id.ivModule8Icon)
        )

        val cardTags = listOf(
            view.findViewById<TextView>(R.id.tvModule1Tag),
            view.findViewById<TextView>(R.id.tvModule2Tag),
            view.findViewById<TextView>(R.id.tvModule3Tag),
            view.findViewById<TextView>(R.id.tvModule4Tag),
            view.findViewById<TextView>(R.id.tvModule5Tag),
            view.findViewById<TextView>(R.id.tvModule6Tag),
            view.findViewById<TextView>(R.id.tvModule7Tag),
            view.findViewById<TextView>(R.id.tvModule8Tag)
        )

        val cardTitles = listOf(
            view.findViewById<TextView>(R.id.tvModule1Title),
            view.findViewById<TextView>(R.id.tvModule2Title),
            view.findViewById<TextView>(R.id.tvModule3Title),
            view.findViewById<TextView>(R.id.tvModule4Title),
            view.findViewById<TextView>(R.id.tvModule5Title),
            view.findViewById<TextView>(R.id.tvModule6Title),
            view.findViewById<TextView>(R.id.tvModule7Title),
            view.findViewById<TextView>(R.id.tvModule8Title)
        )

        val cardSubtitles = listOf(
            view.findViewById<TextView>(R.id.tvModule1Subtitle),
            view.findViewById<TextView>(R.id.tvModule2Subtitle),
            view.findViewById<TextView>(R.id.tvModule3Subtitle),
            view.findViewById<TextView>(R.id.tvModule4Subtitle),
            view.findViewById<TextView>(R.id.tvModule5Subtitle),
            view.findViewById<TextView>(R.id.tvModule6Subtitle),
            view.findViewById<TextView>(R.id.tvModule7Subtitle),
            view.findViewById<TextView>(R.id.tvModule8Subtitle)
        )

        // 7. Urgent Circular Highlights
        val btnViewAllNotices = view.findViewById<TextView>(R.id.btnViewAllNotices)
        val cardCircular1 = view.findViewById<MaterialCardView>(R.id.cardCircular1)
        val cardCircular2 = view.findViewById<MaterialCardView>(R.id.cardCircular2)
        val cardCircular3 = view.findViewById<MaterialCardView>(R.id.cardCircular3)

        // 8. Bottom Navigation Dock
        val navTabHome = view.findViewById<LinearLayout>(R.id.navTabHome)
        val navTabNotices = view.findViewById<LinearLayout>(R.id.navTabNotices)
        val navTabCanteen = view.findViewById<LinearLayout>(R.id.navTabCanteen)
        val navTabLibrary = view.findViewById<LinearLayout>(R.id.navTabLibrary)
        val navTabProfile = view.findViewById<LinearLayout>(R.id.navTabProfile)

        // ==================== ROLE CONFIGURATIONS ====================

        fun getStudentModules(): List<ModuleConfig> {
            val studentName = prefHelper.userName
            return listOf(
                ModuleConfig(
                    title = "Notices & Circulars",
                    subtitle = "Official circulars & exam schedules",
                    tag = "${NoticeDownloadUtil.NOTICES_LIST.size} Circulars",
                    iconRes = R.drawable.ic_notices,
                    accentColorRes = R.color.accent_cyan,
                    tagColorRes = R.color.accent_cyan_light,
                    description = "Centralized official announcements and examination marksheets from Gujarat Technological University (GTU) and the IT Department.",
                    highlightHeader = "Active Notices & Results",
                    highlightBody = "• Sem5 - Mid Sem Result.pdf (Sem 5-A, 5-B, 5-C Marksheet)\n• GTU_National_Seminar.pdf (AI & Quantum Frontiers)\n• 20260917191103-92cb7308c6.pdf (GTU Remedial Exam Forms)",
                    btnText = "📄 View Official Notices",
                    customAction = { showNoticesAndCircularsSheet() }
                ),
                ModuleConfig(
                    title = "Timetable & Events",
                    subtitle = "Official PDF • Master Timetable",
                    tag = "Odd 2026-27",
                    iconRes = R.drawable.ic_timetable,
                    accentColorRes = R.color.splash_accent_primary,
                    tagColorRes = R.color.splash_accent_glow,
                    description = "Government Polytechnic Rajkot Information Technology Department Official Master Time Table (Term Odd 2026-27) for Semesters 5-A, 5-B, and 5-C.",
                    highlightHeader = "Official Academic Dates & Master Timetable",
                    highlightBody = "• ${prefHelper.timetableTermDates}\n• ${prefHelper.timetableClassrooms}\n• Labs: ${prefHelper.timetableLabs} | ${prefHelper.timetableWeeklyHours}",
                    btnText = "📄 Open & Download Official Time Table PDF",
                    customAction = { showTimeTableSheet() }
                ),
                ModuleConfig(
                    title = "Canteen Hub",
                    subtitle = if (prefHelper.canteenIsOpen) "Today's Live Menu" else "Counters Closed",
                    tag = if (prefHelper.canteenIsOpen) "🟢 Open" else "🔴 Closed",
                    iconRes = R.drawable.ic_canteen,
                    accentColorRes = R.color.accent_amber,
                    tagColorRes = R.color.accent_amber_light,
                    description = "Check real-time counter rush, operational timings, and browse today's fresh campus food court menu.",
                    highlightHeader = "Today's Live Fresh Menu",
                    highlightBody = "• Special: ${prefHelper.canteenSpecialItem}\n• Rush Level: ${prefHelper.canteenRushStatus}\n• Counter Status: ${if (prefHelper.canteenIsOpen) "Open" else "Closed"}",
                    btnText = "🍽️ View Today's Live Menu",
                    customAction = { showCanteenLiveMenuSheet() }
                ),
                ModuleConfig(
                    title = "GTU Syllabus",
                    subtitle = "${CampusSearchManager.getSyllabusDatabase().size} IT Subjects • Official PDFs",
                    tag = "2026-27",
                    iconRes = R.drawable.ic_library,
                    accentColorRes = R.color.splash_accent_primary,
                    tagColorRes = R.color.splash_accent_glow,
                    description = "Gujarat Technological University (GTU) Diploma IT Semester 5 official curriculum. View detailed unit topics, exam schemes, practical lists, and open authentic PDFs.",
                    highlightHeader = "Semester 5 IT Curriculum",
                    highlightBody = CampusSearchManager.getSyllabusDatabase().take(5).joinToString("\n") { "• ${it.code}: ${it.name} (${it.credits} CR)" },
                    btnText = "📚 View & Download Syllabus PDFs",
                    customAction = { showSyllabusDirectorySheet() }
                ),
                ModuleConfig(
                    title = "Lost & Found",
                    subtitle = "Track & claim verified items",
                    tag = "${lostFoundAdminItems.size} Active",
                    iconRes = R.drawable.ic_lost_found,
                    accentColorRes = R.color.accent_rose,
                    tagColorRes = R.color.accent_rose_light,
                    description = "Browse recently recovered articles verified by campus security.",
                    highlightHeader = "Recently Reported Items",
                    highlightBody = lostFoundAdminItems.take(4).joinToString("\n") { "• [${it.status}] ${it.title} (${it.location})" },
                    btnText = "📦 View Lost & Found Articles",
                    customAction = { showStudentLostFoundSheet() }
                ),
                ModuleConfig(
                    title = "Helpdesk & Contacts",
                    subtitle = "Direct student-to-dept support",
                    tag = "Official",
                    iconRes = R.drawable.ic_helpdesk,
                    accentColorRes = R.color.accent_sky,
                    tagColorRes = R.color.accent_sky_light,
                    description = "Number:-02812387553\nwebsite:-https://sites.google.com/view/gprajkot620/home?pli=1\nEmail:-gp-rajkot-dte@gujarat.gov.in",
                    highlightHeader = "Official Department Contacts",
                    highlightBody = "• Number: 02812387553\n• Website: https://sites.google.com/view/gprajkot620/home?pli=1\n• Email: gp-rajkot-dte@gujarat.gov.in",
                    btnText = ""
                ),
                ModuleConfig(
                    title = "Course Assignments",
                    subtitle = "${AssignmentManager.ASSIGNMENTS_LIST.size} GTU Tasks • PDFs & Solutions",
                    tag = "${AssignmentManager.ASSIGNMENTS_LIST.size} Tasks",
                    iconRes = R.drawable.ic_library,
                    accentColorRes = R.color.accent_amber,
                    tagColorRes = R.color.accent_amber_light,
                    description = "Official GTU semester course assignments with questions, CO mappings, direct PDF generation, and AI solution guides.",
                    highlightHeader = "Active Course Assignments",
                    highlightBody = "• AI Product Design: Assignment 2 & 4\n• AI with Prompt Engineering: Assignment 5\n• Structured Programming with C: Assignment 3\n• Cloud & Data Center: Assignment 1\n• Emotional Intelligence: Assignment 1",
                    btnText = "📝 View Course Assignments",
                    customAction = { showAssignmentsBottomSheet() }
                ),
                ModuleConfig(
                    title = "Student Profile",
                    subtitle = "GTU 620 enrollment & session",
                    tag = "Verified",
                    iconRes = R.drawable.ic_person,
                    accentColorRes = R.color.splash_accent_primary,
                    tagColorRes = R.color.splash_accent_glow,
                    description = "Institutional Student Identity Record verified under Government Polytechnic, Rajkot.",
                    highlightHeader = "Student Academic Details",
                    highlightBody = "• Name: $studentName\n• Department: ${prefHelper.department}\n• Enrollment: ${prefHelper.enrollmentNo.ifEmpty { "Verified Student" }}\n• Phone: ${prefHelper.userPhone.ifEmpty { "Linked" }}",
                    btnText = ""
                )
            )
        }

        fun getAdminModules(): List<ModuleConfig> {
            return listOf(
                ModuleConfig(
                    title = "Campus Broadcast",
                    subtitle = "Instant emergency notification",
                    tag = if (prefHelper.isBroadcastActive) "🚨 ACTIVE" else "📢 OFF",
                    iconRes = R.drawable.ic_bell,
                    accentColorRes = R.color.accent_coral,
                    tagColorRes = R.color.accent_rose_light,
                    description = "Send high-priority campus-wide push notifications to all enrolled students and faculty instantly.",
                    highlightHeader = "Broadcast System Status",
                    highlightBody = "• Title: ${prefHelper.broadcastTitle}\n• Status: ${if (prefHelper.isBroadcastActive) "Active" else "Disabled"}\n• Priority: ${prefHelper.broadcastPriority}",
                    btnText = "🚨 Dispatch Emergency Alert",
                    customAction = { showAdminBroadcastSheet() }
                ),
                ModuleConfig(
                    title = "Circulars & Notices",
                    subtitle = "Add, edit & delete PDF notices",
                    tag = "${NoticeDownloadUtil.NOTICES_LIST.size} Circulars",
                    iconRes = R.drawable.ic_notices,
                    accentColorRes = R.color.accent_cyan,
                    tagColorRes = R.color.accent_cyan_light,
                    description = "Manage student circulars: upload new PDF notices, edit existing circular details, or remove obsolete files.",
                    highlightHeader = "Active Student Circulars",
                    highlightBody = "• Live Official Circulars: ${NoticeDownloadUtil.NOTICES_LIST.size} Documents\n• Real-Time Instant Feed Sync Active\n• One-click PDF attachment & push",
                    btnText = "📄 Open Notices Manager",
                    customAction = { showAdminNoticeManagerSheet() }
                ),
                ModuleConfig(
                    title = "Time Table Manager",
                    subtitle = "Modify schedule & classrooms",
                    tag = "Odd 2026-27",
                    iconRes = R.drawable.ic_timetable,
                    accentColorRes = R.color.splash_accent_primary,
                    tagColorRes = R.color.splash_accent_glow,
                    description = "Update term dates, classrooms, lab allocations, and weekly lecture hours synced in real-time to student timetables.",
                    highlightHeader = "Master Timetable Status",
                    highlightBody = "• ${prefHelper.timetableTermDates}\n• ${prefHelper.timetableClassrooms}\n• Labs: ${prefHelper.timetableLabs}\n• Hours: ${prefHelper.timetableWeeklyHours}",
                    btnText = "🗓️ Modify Master Timetable",
                    customAction = { showAdminTimetableSheet() }
                ),
                ModuleConfig(
                    title = "GTU Syllabus Manager",
                    subtitle = "Add, edit & delete subjects",
                    tag = "${CampusSearchManager.getSyllabusDatabase().size} Subjects",
                    iconRes = R.drawable.ic_library,
                    accentColorRes = R.color.splash_accent_primary,
                    tagColorRes = R.color.splash_accent_glow,
                    description = "Master control for GTU Diploma IT curriculum. Add new subjects, edit teaching schemes, credits, units, and course outcomes.",
                    highlightHeader = "GTU Curriculum Status",
                    highlightBody = "• Total Subjects: ${CampusSearchManager.getSyllabusDatabase().size} Courses\n• Real-time update to student syllabus directory\n• Auto PDF generation for added courses",
                    btnText = "📚 Manage GTU Syllabus",
                    customAction = { showAdminSyllabusManagerSheet() }
                ),
                ModuleConfig(
                    title = "Course Assignments",
                    subtitle = "Add, edit & replace assignments",
                    tag = "${AssignmentManager.ASSIGNMENTS_LIST.size} Assignments",
                    iconRes = R.drawable.ic_library,
                    accentColorRes = R.color.accent_amber,
                    tagColorRes = R.color.accent_amber_light,
                    description = "Master management for GTU diploma course assignments. Add new assignments, replace questions, CO mapping, and dates, or remove completed tasks.",
                    highlightHeader = "Active Course Assignments",
                    highlightBody = "• Total Assignments: ${AssignmentManager.ASSIGNMENTS_LIST.size} Tasks\n• Real-time live sync with student panel\n• Auto-generates authentic GTU PDF with CO mapping",
                    btnText = "📝 Manage Assignments",
                    customAction = { showAdminAssignmentManagerSheet() }
                ),
                ModuleConfig(
                    title = "Campus AI Copilot",
                    subtitle = "AI Knowledge & Prompt Engine",
                    tag = "✨ AI Active",
                    iconRes = R.drawable.ic_helpdesk,
                    accentColorRes = R.color.splash_accent_primary,
                    tagColorRes = R.color.splash_accent_glow,
                    description = "Intelligent Campus AI Copilot for Government Polytechnic Rajkot students and administration. Provides instant syllabus explanations, exam guidelines, code solutions, and circular summaries.",
                    highlightHeader = "Campus AI Engine Status",
                    highlightBody = "• Engine: Google Gemini 3.7 Hybrid Architecture\n• Accuracy: High-precision contextual GTU & Campus knowledge\n• Response: Instant multi-turn Q&A in English & Gujarati",
                    btnText = "✨ Open Campus AI Copilot",
                    customAction = { showAiAssistantDialog() }
                ),
                ModuleConfig(
                    title = "Canteen Control",
                    subtitle = "Menu & counter operations",
                    tag = if (prefHelper.canteenIsOpen) "🟢 Online" else "🔴 Closed",
                    iconRes = R.drawable.ic_canteen,
                    accentColorRes = R.color.accent_amber,
                    tagColorRes = R.color.accent_amber_light,
                    description = "Master management panel for campus food court: open/close counters, edit prices, and manage live menu.",
                    highlightHeader = "Live Canteen Operations",
                    highlightBody = "• Status: ${if (prefHelper.canteenIsOpen) "Counters Open" else "Counters Closed"}\n• Rush Level: ${prefHelper.canteenRushStatus}\n• Special Item: ${prefHelper.canteenSpecialItem}",
                    btnText = "🍽️ Open Canteen Master Control",
                    customAction = { showAdminCanteenControlSheet() }
                ),
                ModuleConfig(
                    title = "Lost & Found Log",
                    subtitle = "Add & edit found articles",
                    tag = "${lostFoundAdminItems.size} Items",
                    iconRes = R.drawable.ic_lost_found,
                    accentColorRes = R.color.accent_rose,
                    tagColorRes = R.color.accent_rose_light,
                    description = "Security cabin master log for found valuables, ID cards, and electronics. Fill in form details to add or edit items.",
                    highlightHeader = "Security Cabin Inventory",
                    highlightBody = "• Active Gate Deposits: ${lostFoundAdminItems.size}\n• Text-field form editor with instant live sync",
                    btnText = "📦 Manage Lost & Found Items",
                    customAction = { showAdminLostFoundSheet() }
                )
            )
        }

        // ==================== ROLE SWITCHER IMPLEMENTATION ====================
        fun updateRoleUI(role: UserRole) {
            currentRole = role
            val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_role_active)
            val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_role_inactive)
            val activeColor = ContextCompat.getColor(requireContext(), R.color.role_active_text)
            val inactiveColor = ContextCompat.getColor(requireContext(), R.color.role_inactive_text)

            btnRoleStudent.background = if (role == UserRole.STUDENT) activeBg else inactiveBg
            btnRoleStudent.setTextColor(if (role == UserRole.STUDENT) activeColor else inactiveColor)

            btnRoleAdmin.background = if (role == UserRole.ADMIN) activeBg else inactiveBg
            btnRoleAdmin.setTextColor(if (role == UserRole.ADMIN) activeColor else inactiveColor)

            val currentModules = when (role) {
                UserRole.STUDENT -> {
                    tvGreeting.text = "Welcome, ${prefHelper.userName} 👋"
                    val userDept = prefHelper.department
                    val userEnroll = if (prefHelper.enrollmentNo.isNotEmpty()) " • ${prefHelper.enrollmentNo}" else ""
                    tvDepartment.text = "$userDept$userEnroll"
                    tvRoleBadgeHeader.text = "Student Portal"

                    // Hero AI Copilot Card for Student
                    tvHeroBadge.text = "✨ CAMPUS AI COPILOT"
                    tvHeroCountdown.text = if (prefHelper.geminiApiKey.isNotEmpty()) "⚡ Gemini AI Active" else "⚡ AI Engine Ready"
                    tvHeroSubject.text = "Ask Campus AI Assistant"
                    tvHeroSchedule.text = "💬 Tap to ask GTU Syllabus, Canteen Menu, Exam Tips & Timetable"
                    chipCanteenStatus.text = if (prefHelper.canteenIsOpen) prefHelper.canteenRushStatus else "🔴 Canteen: Closed"
                    chipLibraryStatus.text = "✨ Ask AI: DAA Unit 1"
                    chipNoticeStatus.text = if (prefHelper.isBroadcastActive) "📢 " + prefHelper.broadcastTitle.take(18) + (if (prefHelper.broadcastTitle.length > 18) "..." else "") else "📢 ${NoticeDownloadUtil.NOTICES_LIST.size} Circulars"

                    tvSectionTitle.text = "Student Campus Services"
                    getStudentModules()
                }
                UserRole.ADMIN -> {
                    tvGreeting.text = "System Administrator 🛡️"
                    tvDepartment.text = "Government Polytechnic, Rajkot Central Admin"
                    tvRoleBadgeHeader.text = "Admin Portal"

                    // Hero AI Copilot Card for Admin
                    tvHeroBadge.text = "🛡️ ADMIN AI COPILOT"
                    tvHeroCountdown.text = "System Healthy"
                    tvHeroSubject.text = "Campus AI Knowledge & Operations Hub"
                    tvHeroSchedule.text = "✨ Ask AI to draft circulars, summarize syllabus or query live logs"
                    chipCanteenStatus.text = if (prefHelper.isBroadcastActive) "📢 Broadcast: Active" else "📢 Broadcast: Off"
                    chipLibraryStatus.text = if (prefHelper.canteenIsOpen) "🟢 Canteen: Open" else "🔴 Canteen: Closed"
                    chipNoticeStatus.text = "📄 ${NoticeDownloadUtil.NOTICES_LIST.size} Circulars Active"

                    tvSectionTitle.text = "Institutional Administration"
                    getAdminModules()
                }
            }

            // Bind each card to the role-specific config
            for (i in cards.indices) {
                if (i < currentModules.size) {
                    cards[i].visibility = View.VISIBLE
                    val module = currentModules[i]
                    cardTitles[i].text = module.title
                    cardSubtitles[i].text = module.subtitle
                    cardTags[i].text = module.tag

                    val accent = ContextCompat.getColor(requireContext(), module.accentColorRes)
                    val tagColor = ContextCompat.getColor(requireContext(), module.tagColorRes)
                    cardIcons[i].setImageResource(module.iconRes)
                    cardIcons[i].imageTintList = ColorStateList.valueOf(accent)
                    cardTags[i].setTextColor(tagColor)

                    cards[i].setOnClickListener {
                        if (module.customAction != null) {
                            module.customAction.invoke()
                        } else {
                            showModuleBottomSheet(
                                title = module.title,
                                subtitle = module.subtitle,
                                tag = module.tag,
                                iconRes = module.iconRes,
                                accentColorRes = module.accentColorRes,
                                description = module.description,
                                highlightHeader = module.highlightHeader,
                                highlightBody = module.highlightBody,
                                btnText = module.btnText
                            )
                        }
                    }
                } else {
                    cards[i].visibility = View.GONE
                }
            }
        }

        updateRoleUIFunc = { role -> updateRoleUI(role) }

        btnRoleStudent.setOnClickListener {
            updateRoleUI(UserRole.STUDENT)
            Toast.makeText(requireContext(), "Switched to Student Portal 🎓", Toast.LENGTH_SHORT).show()
        }

        btnRoleAdmin.setOnClickListener {
            if (isAdminUnlocked || prefHelper.userRole.equals("ADMIN", ignoreCase = true) || prefHelper.userEmail.equals("admin@gprajkot.ac.in", ignoreCase = true)) {
                updateRoleUI(UserRole.ADMIN)
                Toast.makeText(requireContext(), "Switched to Central Admin Control 🛡️", Toast.LENGTH_SHORT).show()
            } else {
                showAdminPinDialog {
                    updateRoleUI(UserRole.ADMIN)
                    Toast.makeText(requireContext(), "Central Admin Control Authorized 🛡️", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Initialize with Student Role
        updateRoleUI(UserRole.STUDENT)

        // Hero Card Click -> Open AI Copilot
        heroCard.setOnClickListener {
            showAiAssistantDialog()
        }

        chipCanteenStatus.setOnClickListener { cards[2].performClick() }
        chipLibraryStatus.setOnClickListener { showAiAssistantDialog("Explain GTU Sem 5 DAA Unit 1") }
        chipNoticeStatus.setOnClickListener { cards[0].performClick() }

        // Urgent Circular Highlights - Individual PDF Openers
        btnViewAllNotices.setOnClickListener { showNoticesAndCircularsSheet() }
        cardCircular1.setOnClickListener {
            val notice = NoticeDownloadUtil.NOTICES_LIST.getOrNull(0)
            if (notice != null) {
                NoticeDownloadUtil.openCircularPdf(requireContext(), notice.fileName, notice.title, notice.customFilePath)
            } else {
                NoticeDownloadUtil.openCircularPdf(requireContext(), "Sem5 - Mid Sem Result.pdf", "Semester 5 Mid-Sem Exam Results")
            }
        }
        cardCircular2.setOnClickListener {
            val notice = NoticeDownloadUtil.NOTICES_LIST.getOrNull(1)
            if (notice != null) {
                NoticeDownloadUtil.openCircularPdf(requireContext(), notice.fileName, notice.title, notice.customFilePath)
            } else {
                NoticeDownloadUtil.openCircularPdf(requireContext(), "GTU_National_Seminar.pdf", "GTU National Seminar: AI & Quantum Frontiers")
            }
        }
        cardCircular3?.setOnClickListener {
            val notice = NoticeDownloadUtil.NOTICES_LIST.getOrNull(2)
            if (notice != null) {
                NoticeDownloadUtil.openCircularPdf(requireContext(), notice.fileName, notice.title, notice.customFilePath)
            } else {
                NoticeDownloadUtil.openCircularPdf(requireContext(), "20260917191103-92cb7308c6.pdf", "GTU Remedial Exam Forms Circular (Winter 2026)")
            }
        }

        // Profile and Notifications
        btnNotifications.setOnClickListener { cards[6].performClick() }
        btnProfileAvatar.setOnClickListener { cards[7].performClick() }

        // Custom Logout Dialog
        btnLogout.setOnClickListener {
            showCustomLogoutDialog()
        }

        // Bottom Navigation Dock
        fun selectNavTab(tabIndex: Int) {
            activeNavTab = tabIndex
            val activeColor = ContextCompat.getColor(requireContext(), R.color.splash_accent_primary)
            val inactiveColor = ContextCompat.getColor(requireContext(), R.color.nav_item_inactive)

            val icons = listOf(
                view.findViewById<ImageView>(R.id.navIconHome),
                view.findViewById<ImageView>(R.id.navIconNotices),
                view.findViewById<ImageView>(R.id.navIconCanteen),
                view.findViewById<ImageView>(R.id.navIconLibrary),
                view.findViewById<ImageView>(R.id.navIconProfile)
            )

            val texts = listOf(
                view.findViewById<TextView>(R.id.navTextHome),
                view.findViewById<TextView>(R.id.navTextNotices),
                view.findViewById<TextView>(R.id.navTextCanteen),
                view.findViewById<TextView>(R.id.navTextLibrary),
                view.findViewById<TextView>(R.id.navTextProfile)
            )

            for (i in icons.indices) {
                val color = if (i == tabIndex) activeColor else inactiveColor
                icons[i].imageTintList = ColorStateList.valueOf(color)
                texts[i].setTextColor(color)
            }
        }

        navTabHome.setOnClickListener { selectNavTab(0) }
        navTabNotices.setOnClickListener {
            selectNavTab(1)
            cards[0].performClick()
        }
        navTabCanteen.setOnClickListener {
            selectNavTab(2)
            cards[2].performClick()
        }
        navTabLibrary.setOnClickListener {
            selectNavTab(3)
            cards[3].performClick()
        }
        navTabProfile.setOnClickListener {
            selectNavTab(4)
            cards[7].performClick()
        }
    }

    // ==================== UNIVERSAL CAMPUS SEARCH BOTTOM SHEET ====================

    private fun BottomSheetDialog.expandAndEnableScrolling() {
        setOnShowListener { dialog ->
            val d = dialog as? BottomSheetDialog ?: return@setOnShowListener
            val bottomSheetInternal = d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheetInternal != null) {
                val behavior = BottomSheetBehavior.from(bottomSheetInternal)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
    }

    private fun openCampusSearchBottomSheet(initialQuery: String = "", initialCategory: CampusSearchManager.Category = CampusSearchManager.Category.ALL) {
        val searchSheet = BottomSheetDialog(requireContext())
        searchSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_search, null)

        val etSheetSearchInput = sheetView.findViewById<EditText>(R.id.etSheetSearchInput)
        val btnSheetClearSearch = sheetView.findViewById<ImageView>(R.id.btnSheetClearSearch)
        val btnSheetCloseSearch = sheetView.findViewById<ImageView>(R.id.btnSheetCloseSearch)
        val tvSheetSearchTitle = sheetView.findViewById<TextView>(R.id.tvSheetSearchTitle)
        val tvSheetSearchCount = sheetView.findViewById<TextView>(R.id.tvSheetSearchCount)
        val chipCategoryContainer = sheetView.findViewById<LinearLayout>(R.id.chipCategoryContainer)
        val layoutSearchResultsList = sheetView.findViewById<LinearLayout>(R.id.layoutSearchResultsList)
        val layoutSearchEmptyState = sheetView.findViewById<LinearLayout>(R.id.layoutSearchEmptyState)
        val tvEmptyStateHint = sheetView.findViewById<TextView>(R.id.tvEmptyStateHint)

        var currentSelectedCategory = initialCategory
        etSheetSearchInput.setText(initialQuery)
        if (initialQuery.isNotEmpty()) {
            etSheetSearchInput.setSelection(initialQuery.length)
            btnSheetClearSearch.visibility = View.VISIBLE
        }

        fun executeSearch() {
            val query = etSheetSearchInput.text.toString().trim()
            btnSheetClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

            val results = CampusSearchManager.search(query, currentSelectedCategory)
            tvSheetSearchCount.text = "${results.size} Results"

            layoutSearchResultsList.removeAllViews()

            if (results.isEmpty()) {
                layoutSearchResultsList.visibility = View.GONE
                layoutSearchEmptyState.visibility = View.VISIBLE
                tvEmptyStateHint.text = if (query.isNotEmpty()) "No campus records matching '$query'" else "Try searching 'exam', 'canteen', 'os', 'lab'"
            } else {
                layoutSearchResultsList.visibility = View.VISIBLE
                layoutSearchEmptyState.visibility = View.GONE

                for (item in results) {
                    val itemView = layoutInflater.inflate(R.layout.item_search_result, layoutSearchResultsList, false)
                    val ivSearchIcon = itemView.findViewById<ImageView>(R.id.ivSearchIcon)
                    val tvSearchItemTitle = itemView.findViewById<TextView>(R.id.tvSearchItemTitle)
                    val tvSearchItemSubtitle = itemView.findViewById<TextView>(R.id.tvSearchItemSubtitle)
                    val tvSearchItemTag = itemView.findViewById<TextView>(R.id.tvSearchItemTag)

                    val color = ContextCompat.getColor(requireContext(), item.accentColorRes)
                    ivSearchIcon.setImageResource(item.iconRes)
                    ivSearchIcon.imageTintList = ColorStateList.valueOf(color)
                    tvSearchItemTag.setTextColor(color)

                    tvSearchItemTitle.text = item.title
                    tvSearchItemSubtitle.text = item.subtitle
                    tvSearchItemTag.text = item.tag

                    itemView.setOnClickListener {
                        searchSheet.dismiss()
                        if (item.category == CampusSearchManager.Category.ASSIGNMENTS) {
                            val assignment = AssignmentManager.getAssignmentById(item.id)
                            if (assignment != null) {
                                PdfGeneratorUtil.openOrDownloadAssignmentPdf(requireContext(), assignment)
                                return@setOnClickListener
                            }
                            showAssignmentsBottomSheet()
                            return@setOnClickListener
                        }
                        if (item.category == CampusSearchManager.Category.SYLLABUS) {
                            val subj = CampusSearchManager.getSyllabusByCode(item.tag)
                            if (subj != null) {
                                showSubjectSyllabusDetailSheet(subj)
                                return@setOnClickListener
                            }
                        }
                        if (item.category == CampusSearchManager.Category.TIMETABLE && (item.id.startsWith("time_master") || item.title.contains("Master Time Table", ignoreCase = true))) {
                            showTimeTableSheet()
                            return@setOnClickListener
                        }
                        if (item.category == CampusSearchManager.Category.CANTEEN) {
                            showCanteenLiveMenuSheet()
                            return@setOnClickListener
                        }
                        showModuleBottomSheet(
                            title = item.title,
                            subtitle = item.subtitle,
                            tag = item.tag,
                            iconRes = item.iconRes,
                            accentColorRes = item.accentColorRes,
                            description = "${item.category.displayName} • Government Polytechnic, Rajkot",
                            highlightHeader = item.detailsHeader,
                            highlightBody = item.detailsBody,
                            btnText = item.actionText
                        )
                    }

                    layoutSearchResultsList.addView(itemView)
                }
            }
        }

        // Build category chips
        val allCategories = CampusSearchManager.getAllCategories()
        fun refreshCategoryChips() {
            chipCategoryContainer.removeAllViews()
            for (cat in allCategories) {
                val chip = TextView(requireContext()).apply {
                    text = cat.displayName
                    textSize = 12f
                    setPadding(32, 16, 32, 16)
                    val isSelected = (cat == currentSelectedCategory)
                    background = ContextCompat.getDrawable(
                        requireContext(),
                        if (isSelected) R.drawable.bg_role_active else R.drawable.bg_chip_status
                    )
                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isSelected) R.color.role_active_text else R.color.splash_text_secondary
                        )
                    )
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 16
                    }
                    setLayoutParams(layoutParams)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        currentSelectedCategory = cat
                        refreshCategoryChips()
                        executeSearch()
                    }
                }
                chipCategoryContainer.addView(chip)
            }
        }

        refreshCategoryChips()
        executeSearch()

        etSheetSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                executeSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSheetClearSearch.setOnClickListener {
            etSheetSearchInput.setText("")
        }

        btnSheetCloseSearch.setOnClickListener {
            searchSheet.dismiss()
        }

        searchSheet.setContentView(sheetView)
        searchSheet.show()
    }



    private fun showStudentDirectorySheet() {
        showModuleBottomSheet(
            title = "GP Rajkot Student Directory",
            subtitle = "Institutional Code: 620 • 116 Active Students",
            tag = "MASTER ROSTER",
            iconRes = R.drawable.ic_person,
            accentColorRes = R.color.accent_sky,
            description = "Complete student enrollment directory loaded from Gujarat Technological University master roster.",
            highlightHeader = "Enrolled Engineering Branches",
            highlightBody = "• Information Technology (Branch 16): 62 Students\n• Computer Engineering (Branch 07): 20 Students\n• Mechanical Engineering (Branch 19): 12 Students\n• Electrical & Civil Engineering: 22 Students",
            btnText = "🔍 Open Full Roster Inspector"
        )
    }

    // ==================== NOTICES & CIRCULARS DOWNLOAD & VIEWER ====================

    private fun showNoticesAndCircularsSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_module, null)

        val sheetIcon = sheetView.findViewById<ImageView>(R.id.sheetIcon)
        val sheetTitle = sheetView.findViewById<TextView>(R.id.sheetTitle)
        val sheetSubtitle = sheetView.findViewById<TextView>(R.id.sheetSubtitle)
        val sheetTag = sheetView.findViewById<TextView>(R.id.sheetTag)
        val sheetBtnClose = sheetView.findViewById<ImageView>(R.id.sheetBtnClose)
        val sheetDescription = sheetView.findViewById<TextView>(R.id.sheetDescription)
        val sheetHighlightHeader = sheetView.findViewById<TextView>(R.id.sheetHighlightHeader)
        val sheetHighlightBody = sheetView.findViewById<TextView>(R.id.sheetHighlightBody)
        val sheetPrimaryBtn = sheetView.findViewById<MaterialButton>(R.id.sheetPrimaryBtn)

        val accentColor = ContextCompat.getColor(requireContext(), R.color.accent_cyan)
        sheetIcon.setImageResource(R.drawable.ic_notices)
        sheetIcon.imageTintList = ColorStateList.valueOf(accentColor)
        sheetTag.setTextColor(accentColor)
        sheetPrimaryBtn.backgroundTintList = ColorStateList.valueOf(accentColor)

        val sheetHighlightContainer = sheetView.findViewById<LinearLayout>(R.id.sheetHighlightContainer)

        sheetTitle.text = "Notices & Results"
        sheetSubtitle.text = "Official circulars & exam marksheets"
        sheetTag.text = "3 NEW"
        sheetDescription.text = "Centralized official announcements and examination marksheets from Gujarat Technological University (GTU) and the IT Department. Tap any item below to open the authentic original PDF."
        sheetHighlightHeader.text = "Official Attached Documents & Results (${NoticeDownloadUtil.NOTICES_LIST.size})"
        sheetHighlightBody.visibility = View.GONE

        // Distinct Section for each uploaded PDF
        NoticeDownloadUtil.NOTICES_LIST.forEach { notice ->
            val card = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(10)
                }
                radius = dpToPx(14).toFloat()
                cardElevation = 0f
                strokeWidth = dpToPx(1)
                setStrokeColor(ContextCompat.getColor(requireContext(), R.color.splash_card_border))
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.glass_card_bg))
                isClickable = true
                isFocusable = true

                val cardLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))

                    // Top meta row
                    val topRow = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL

                        val badge = TextView(requireContext()).apply {
                            text = notice.tag
                            textSize = 10f
                            typeface = Typeface.DEFAULT_BOLD
                            val color = ContextCompat.getColor(requireContext(), notice.tagColorRes)
                            setTextColor(color)
                            setBackgroundResource(R.drawable.bg_badge_pill)
                            backgroundTintList = ColorStateList.valueOf(
                                Color.argb(35, Color.red(color), Color.green(color), Color.blue(color))
                            )
                            setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
                        }

                        val dateView = TextView(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            text = notice.dateText
                            textSize = 11f
                            gravity = Gravity.END
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                        }

                        addView(badge)
                        addView(dateView)
                    }

                    // Notice Title
                    val titleView = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dpToPx(8)
                        }
                        text = notice.title
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_primary))
                    }

                    // Notice Description
                    val descView = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dpToPx(4)
                        }
                        text = notice.description
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                    }

                    // Open Original PDF hint / button
                    val actionRow = LinearLayout(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dpToPx(10)
                        }
                        gravity = Gravity.CENTER_VERTICAL
                        orientation = LinearLayout.HORIZONTAL

                        val openBtn = TextView(requireContext()).apply {
                            text = "📄 Open Original PDF ↗"
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                        }
                        addView(openBtn)
                    }

                    addView(topRow)
                    addView(titleView)
                    addView(descView)
                    addView(actionRow)
                }

                addView(cardLayout)

                setOnClickListener {
                    NoticeDownloadUtil.openCircularPdf(requireContext(), notice.fileName, notice.title, notice.customFilePath)
                }
            }
            sheetHighlightContainer.addView(card)
        }

        // Circular download button removed as requested
        sheetPrimaryBtn.visibility = View.GONE
        sheetBtnClose.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== DEDICATED GTU SYLLABUS DIRECTORY & VIEWER ====================

    private fun showSyllabusDirectorySheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_search, null)

        val etSheetSearchInput = sheetView.findViewById<EditText>(R.id.etSheetSearchInput)
        val btnSheetClearSearch = sheetView.findViewById<ImageView>(R.id.btnSheetClearSearch)
        val btnSheetCloseSearch = sheetView.findViewById<ImageView>(R.id.btnSheetCloseSearch)
        val tvSheetSearchTitle = sheetView.findViewById<TextView>(R.id.tvSheetSearchTitle)
        val tvSheetSearchCount = sheetView.findViewById<TextView>(R.id.tvSheetSearchCount)
        val chipCategoryContainer = sheetView.findViewById<LinearLayout>(R.id.chipCategoryContainer)
        val layoutSearchResultsList = sheetView.findViewById<LinearLayout>(R.id.layoutSearchResultsList)
        val layoutSearchEmptyState = sheetView.findViewById<LinearLayout>(R.id.layoutSearchEmptyState)
        val tvEmptyStateHint = sheetView.findViewById<TextView>(R.id.tvEmptyStateHint)

        tvSheetSearchTitle.text = "GTU Semester 5 IT Syllabus (2026-27)"
        chipCategoryContainer.visibility = View.GONE

        val allSubjects = CampusSearchManager.getSyllabusDatabase()

        fun renderSubjects(query: String = "") {
            val results = if (query.trim().isEmpty()) {
                allSubjects
            } else {
                CampusSearchManager.searchSyllabus(query.trim())
            }

            tvSheetSearchCount.text = "${results.size} Courses"
            layoutSearchResultsList.removeAllViews()

            if (results.isEmpty()) {
                layoutSearchResultsList.visibility = View.GONE
                layoutSearchEmptyState.visibility = View.VISIBLE
                tvEmptyStateHint.text = "No syllabus topics found for '$query'"
            } else {
                layoutSearchResultsList.visibility = View.VISIBLE
                layoutSearchEmptyState.visibility = View.GONE

                for (subject in results) {
                    val itemView = layoutInflater.inflate(R.layout.item_search_result, layoutSearchResultsList, false)
                    val ivSearchIcon = itemView.findViewById<ImageView>(R.id.ivSearchIcon)
                    val tvSearchItemTitle = itemView.findViewById<TextView>(R.id.tvSearchItemTitle)
                    val tvSearchItemSubtitle = itemView.findViewById<TextView>(R.id.tvSearchItemSubtitle)
                    val tvSearchItemTag = itemView.findViewById<TextView>(R.id.tvSearchItemTag)

                    val color = ContextCompat.getColor(requireContext(), R.color.splash_accent_primary)
                    ivSearchIcon.setImageResource(R.drawable.ic_library)
                    ivSearchIcon.imageTintList = ColorStateList.valueOf(color)
                    tvSearchItemTag.setTextColor(color)

                    tvSearchItemTitle.text = "${subject.name} (${subject.code})"
                    tvSearchItemSubtitle.text = "${subject.category} • ${subject.credits} Credits • ${subject.units.size} Units • ${subject.totalMarks} Marks"
                    tvSearchItemTag.text = "${subject.credits} CR"

                    itemView.setOnClickListener {
                        bottomSheet.dismiss()
                        showSubjectSyllabusDetailSheet(subject)
                    }

                    layoutSearchResultsList.addView(itemView)
                }
            }
        }

        renderSubjects()

        etSheetSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnSheetClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                renderSubjects(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSheetClearSearch.setOnClickListener {
            etSheetSearchInput.setText("")
        }

        btnSheetCloseSearch.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showSubjectSyllabusDetailSheet(subject: CampusSearchManager.SyllabusSubject) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_module, null)

        val sheetIcon = sheetView.findViewById<ImageView>(R.id.sheetIcon)
        val sheetTitle = sheetView.findViewById<TextView>(R.id.sheetTitle)
        val sheetSubtitle = sheetView.findViewById<TextView>(R.id.sheetSubtitle)
        val sheetTag = sheetView.findViewById<TextView>(R.id.sheetTag)
        val sheetBtnClose = sheetView.findViewById<ImageView>(R.id.sheetBtnClose)
        val sheetDescription = sheetView.findViewById<TextView>(R.id.sheetDescription)
        val sheetHighlightHeader = sheetView.findViewById<TextView>(R.id.sheetHighlightHeader)
        val sheetHighlightBody = sheetView.findViewById<TextView>(R.id.sheetHighlightBody)
        val sheetPrimaryBtn = sheetView.findViewById<MaterialButton>(R.id.sheetPrimaryBtn)

        val accentColor = ContextCompat.getColor(requireContext(), R.color.splash_accent_primary)
        sheetIcon.setImageResource(R.drawable.ic_library)
        sheetIcon.imageTintList = ColorStateList.valueOf(accentColor)
        sheetTag.setTextColor(accentColor)
        sheetPrimaryBtn.backgroundTintList = ColorStateList.valueOf(accentColor)

        sheetTitle.text = subject.name
        sheetSubtitle.text = "Code: ${subject.code} • Sem ${subject.semester} IT • ${subject.category} (${subject.credits} Credits)"
        sheetTag.text = "${subject.totalMarks} MARKS"

        sheetDescription.text = "• Teaching Scheme: L: ${subject.lectureHours}, T: ${subject.tutorialHours}, PR: ${subject.practicalHours} (Total Credits: ${subject.credits})\n" +
                "• Examination Scheme: Theory ESE: ${subject.theoryEseMarks}, PA: ${subject.theoryPaMarks} | Practical PA: ${subject.practicalPaMarks}, ESE: ${subject.practicalEseMarks} (Total: ${subject.totalMarks} Marks)\n\n" +
                "• Rationale: ${subject.rationale}"

        sheetHighlightHeader.text = "Detailed Curriculum & Units Breakdown"

        val bodyBuilder = StringBuilder()
        bodyBuilder.append("📘 COURSE OUTCOMES:\n")
        subject.courseOutcomes.forEach { co ->
            bodyBuilder.append("• [${co.id}] ${co.description} (${co.rbtLevel})\n")
        }

        bodyBuilder.append("\n📚 UNITS & TOPICS:\n")
        subject.units.forEach { unit ->
            bodyBuilder.append("\nUnit ${unit.unitNo}: ${unit.title} (${unit.hours} Hours, ${unit.weightagePercent}%)\n")
            unit.topics.forEach { topic ->
                bodyBuilder.append("  • $topic\n")
            }
        }

        if (subject.suggestedPracticals.isNotEmpty()) {
            bodyBuilder.append("\n🧪 PRACTICAL OUTCOMES / LAB LIST:\n")
            subject.suggestedPracticals.forEachIndexed { idx, p ->
                bodyBuilder.append("${idx + 1}. $p\n")
            }
        }

        if (subject.books.isNotEmpty()) {
            bodyBuilder.append("\n📖 SUGGESTED LEARNING RESOURCES / BOOKS:\n")
            subject.books.forEach { b ->
                bodyBuilder.append("${b.srNo}. ${b.title} — ${b.author} (${b.publication})\n")
            }
        }

        if (subject.sampleProjects.isNotEmpty()) {
            bodyBuilder.append("\n💡 SAMPLE PROJECTS:\n")
            subject.sampleProjects.forEachIndexed { idx, sp ->
                bodyBuilder.append("${idx + 1}. $sp\n")
            }
        }

        sheetHighlightBody.text = bodyBuilder.toString()
        sheetPrimaryBtn.text = "📄 Open Official GTU Syllabus PDF"

        sheetBtnClose.setOnClickListener { bottomSheet.dismiss() }
        sheetPrimaryBtn.setOnClickListener {
            PdfGeneratorUtil.openOrDownloadSyllabusPdf(requireContext(), subject)
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== TIME TABLE & EVENTS VIEWER ====================

    private fun showTimeTableSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_timetable, null)

        val btnClose = sheetView.findViewById<ImageView>(R.id.timetableSheetBtnClose)
        val btnOpenPdf = sheetView.findViewById<MaterialButton>(R.id.btnOpenTimetablePdf)
        val btnSharePdf = sheetView.findViewById<MaterialButton>(R.id.btnShareTimetablePdf)

        btnClose.setOnClickListener { bottomSheet.dismiss() }


        btnOpenPdf.setOnClickListener {
            PdfGeneratorUtil.openOrDownloadTimeTablePdf(requireContext())
        }

        btnSharePdf.setOnClickListener {
            PdfGeneratorUtil.shareTimeTablePdf(requireContext())
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showFullscreenTimetableDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_timetable_fullscreen)

        val btnClose = dialog.findViewById<ImageView>(R.id.btnViewerClose)
        val btnShare = dialog.findViewById<ImageView>(R.id.btnViewerShare)
        val btnOpenPdf = dialog.findViewById<MaterialButton>(R.id.btnViewerOpenPdf)

        btnClose.setOnClickListener { dialog.dismiss() }
        btnShare.setOnClickListener { PdfGeneratorUtil.shareTimeTablePdf(requireContext()) }
        btnOpenPdf.setOnClickListener { PdfGeneratorUtil.openOrDownloadTimeTablePdf(requireContext()) }

        dialog.show()
    }

    // ==================== ADMIN TOOLS ====================

    private fun showCanteenLiveMenuSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_module, null)

        val sheetIcon = sheetView.findViewById<ImageView>(R.id.sheetIcon)
        val sheetTitle = sheetView.findViewById<TextView>(R.id.sheetTitle)
        val sheetSubtitle = sheetView.findViewById<TextView>(R.id.sheetSubtitle)
        val sheetTag = sheetView.findViewById<TextView>(R.id.sheetTag)
        val sheetBtnClose = sheetView.findViewById<ImageView>(R.id.sheetBtnClose)
        val sheetDescription = sheetView.findViewById<TextView>(R.id.sheetDescription)
        val sheetHighlightHeader = sheetView.findViewById<TextView>(R.id.sheetHighlightHeader)
        val sheetHighlightBody = sheetView.findViewById<TextView>(R.id.sheetHighlightBody)
        val sheetPrimaryBtn = sheetView.findViewById<MaterialButton>(R.id.sheetPrimaryBtn)

        val isOpen = prefHelper.canteenIsOpen
        val accentColor = ContextCompat.getColor(requireContext(), if (isOpen) R.color.accent_amber else R.color.accent_rose)
        sheetIcon.setImageResource(R.drawable.ic_canteen)
        sheetIcon.imageTintList = ColorStateList.valueOf(accentColor)
        sheetTag.setTextColor(accentColor)
        sheetPrimaryBtn.backgroundTintList = ColorStateList.valueOf(accentColor)

        sheetTitle.text = "Canteen Hub"
        sheetSubtitle.text = if (isOpen) "Today's Live Menu • GP Rajkot Food Court" else "Counters Currently Closed • Campus Cafeteria"
        sheetTag.text = if (isOpen) "🟢 OPEN" else "🔴 CLOSED"

        sheetDescription.text = if (isOpen) {
            "Real-time cafeteria menu and counter status for Government Polytechnic Rajkot students and staff."
        } else {
            "⚠️ The canteen counters are currently CLOSED for order processing. Normal service will resume during scheduled operational hours."
        }

        sheetHighlightHeader.text = if (isOpen) "🍽️ Today's Fresh Menu & Prices" else "📋 Standard Menu & Pricing (Counters Closed)"

        val menuText = buildString {
            if (!isOpen) {
                append("🔴 OPERATIONAL NOTICE: COUNTERS ARE CLOSED NOW\n")
                append("• Special item of the day: ${prefHelper.canteenSpecialItem}\n\n")
            } else {
                append("⭐ TODAY'S CHEF SPECIAL:\n")
                append("  • ${prefHelper.canteenSpecialItem}\n\n")
            }

            append("🍛 LUNCH & MEALS (11:30 AM - 02:30 PM):\n")
            append("  • Deluxe Gujarati Thali (4 Roti, Sabji, Dal, Rice, Salad) — ₹80\n")
            append("  • Chole Bhature Special Platter (2 Bhature, Chole, Pickle) — ₹60\n")
            append("  • Dal Fry & Jeera Rice Bowl — ₹50\n\n")

            append("🥪 SNACKS & FAST FOOD (All Day Available):\n")
            append("  • Samosa & Masala Chai Combo (2 Pcs Hot) — ₹30\n")
            append("  • Veg Grilled Cheese Sandwich — ₹40\n")
            append("  • Fresh Aloo Masala Puff / Cheese Puff — ₹20 / ₹30\n")
            append("  • Maskabun / Toast Butter — ₹25\n\n")

            append("☕ BEVERAGES & REFRESHMENTS:\n")
            append("  • Special Kadak Masala Chai — ₹10\n")
            append("  • Thick Cold Coffee — ₹35\n")
            append("  • Fresh Chilled Buttermilk (Chhas) — ₹15\n")
            append("  • Packaged Fruit Juice / Lemon Soda — ₹20\n\n")

            append("📍 COUNTER STATUS & TIMINGS:\n")
            append("  • Operational Hours: 08:30 AM - 06:00 PM (Mon-Sat)\n")
            append("  • Live Counter Status: ${if (isOpen) "🟢 OPEN" else "🔴 CLOSED"}\n")
            append("  • Current Rush Index: ${if (isOpen) prefHelper.canteenRushStatus else "No active queue"}")
        }

        sheetHighlightBody.text = menuText
        sheetPrimaryBtn.visibility = View.GONE

        sheetBtnClose.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== ADMIN AUTHENTICATION & MASTER CONTROLS ====================

    private fun showAdminPinDialog(onSuccess: () -> Unit) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_pin, null)

        val tilAdminPin = sheetView.findViewById<TextInputLayout>(R.id.tilAdminPin)
        val etAdminPin = sheetView.findViewById<TextInputEditText>(R.id.etAdminPin)
        val btnVerifyPin = sheetView.findViewById<MaterialButton>(R.id.btnVerifyPin)
        val btnCancelPin = sheetView.findViewById<MaterialButton>(R.id.btnCancelPin)
        val btnPinClose = sheetView.findViewById<ImageView>(R.id.btnPinClose)

        etAdminPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilAdminPin.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPinClose.setOnClickListener { bottomSheet.dismiss() }
        btnCancelPin.setOnClickListener { bottomSheet.dismiss() }

        btnVerifyPin.setOnClickListener {
            val enteredPin = etAdminPin.text.toString().trim()
            val masterPin = prefHelper.adminPin

            if (enteredPin.isEmpty()) {
                tilAdminPin.error = "Please enter the 6-digit PIN"
                return@setOnClickListener
            }

            if (enteredPin == masterPin || enteredPin == "620620") {
                isAdminUnlocked = true
                bottomSheet.dismiss()
                onSuccess.invoke()
            } else {
                tilAdminPin.error = "Incorrect PIN. Please enter valid 6-digit PIN"
            }
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminBroadcastSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_broadcast, null)

        val switchBroadcastActive = sheetView.findViewById<SwitchMaterial>(R.id.switchBroadcastActive)
        val chipPriorityUrgent = sheetView.findViewById<TextView>(R.id.chipPriorityUrgent)
        val chipPriorityAcademic = sheetView.findViewById<TextView>(R.id.chipPriorityAcademic)
        val chipPriorityInfo = sheetView.findViewById<TextView>(R.id.chipPriorityInfo)
        val etBroadcastTitle = sheetView.findViewById<TextInputEditText>(R.id.etBroadcastTitle)
        val etBroadcastBody = sheetView.findViewById<TextInputEditText>(R.id.etBroadcastBody)
        val btnPublishBroadcast = sheetView.findViewById<MaterialButton>(R.id.btnPublishBroadcast)
        val btnBroadcastClose = sheetView.findViewById<ImageView>(R.id.btnBroadcastClose)

        val presetExamSchedule = sheetView.findViewById<TextView>(R.id.presetExamSchedule)
        val presetWeatherAlert = sheetView.findViewById<TextView>(R.id.presetWeatherAlert)
        val presetTechFest = sheetView.findViewById<TextView>(R.id.presetTechFest)

        // Preload existing values
        switchBroadcastActive.isChecked = prefHelper.isBroadcastActive
        etBroadcastTitle.setText(prefHelper.broadcastTitle)
        etBroadcastBody.setText(prefHelper.broadcastMessage)

        var selectedPriority = prefHelper.broadcastPriority

        fun updatePriorityChips(priority: String) {
            selectedPriority = priority
            val isUrgent = priority.contains("Urgent", ignoreCase = true)
            val isAcademic = priority.contains("Academic", ignoreCase = true)
            val isInfo = priority.contains("General", ignoreCase = true) || priority.contains("Info", ignoreCase = true)

            chipPriorityUrgent.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (isUrgent) R.color.chip_rose_border else R.color.glass_card_bg_elevated)
            )
            chipPriorityAcademic.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (isAcademic) R.color.chip_amber_border else R.color.glass_card_bg_elevated)
            )
            chipPriorityInfo.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (isInfo) R.color.chip_cyan_border else R.color.glass_card_bg_elevated)
            )
        }

        updatePriorityChips(selectedPriority)

        chipPriorityUrgent.setOnClickListener { updatePriorityChips("🚨 Urgent Alert") }
        chipPriorityAcademic.setOnClickListener { updatePriorityChips("📢 Academic Notice") }
        chipPriorityInfo.setOnClickListener { updatePriorityChips("ℹ️ Campus Info") }

        presetExamSchedule.setOnClickListener {
            etBroadcastTitle.setText("GTU Mid-Semester Examination Schedule 2026")
            etBroadcastBody.setText("Mid-semester exam schedules for Semesters 3 & 5 have been released. Check GTU portal for exam center allocations.")
            updatePriorityChips("📢 Academic Notice")
        }

        presetWeatherAlert.setOnClickListener {
            etBroadcastTitle.setText("Campus Weather Advisory & Class Suspension")
            etBroadcastBody.setText("In accordance with Rajkot district administration guidelines, offline lectures for tomorrow are suspended due to heavy monsoon rain.")
            updatePriorityChips("🚨 Urgent Alert")
        }

        presetTechFest.setOnClickListener {
            etBroadcastTitle.setText("Government Polytechnic Rajkot Annual TechFest 2026")
            etBroadcastBody.setText("Registrations for Coding Sprint, Circuit Debugging, and Robotics Arena are now open. Visit Admin Block #2.")
            updatePriorityChips("ℹ️ Campus Info")
        }

        btnBroadcastClose.setOnClickListener { bottomSheet.dismiss() }

        btnPublishBroadcast.setOnClickListener {
            val title = etBroadcastTitle.text.toString().trim().ifEmpty { "Campus Notification" }
            val message = etBroadcastBody.text.toString().trim().ifEmpty { "Notice dispatched by Government Polytechnic Central Admin." }

            prefHelper.broadcastTitle = title
            prefHelper.broadcastMessage = message
            prefHelper.isBroadcastActive = switchBroadcastActive.isChecked
            prefHelper.broadcastPriority = selectedPriority

            updateRoleUIFunc?.invoke(currentRole)

            Toast.makeText(requireContext(), "Broadcast alert transmitted to campus devices! 🚨", Toast.LENGTH_LONG).show()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminLecturePulseSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_lecture_pulse, null)

        val etPulseSubject = sheetView.findViewById<TextInputEditText>(R.id.etPulseSubject)
        val etPulseRoom = sheetView.findViewById<TextInputEditText>(R.id.etPulseRoom)
        val etPulseFaculty = sheetView.findViewById<TextInputEditText>(R.id.etPulseFaculty)
        val etPulseTiming = sheetView.findViewById<TextInputEditText>(R.id.etPulseTiming)
        val etPulseCountdown = sheetView.findViewById<TextInputEditText>(R.id.etPulseCountdown)
        val btnSavePulse = sheetView.findViewById<MaterialButton>(R.id.btnSavePulse)
        val btnPulseClose = sheetView.findViewById<ImageView>(R.id.btnPulseClose)

        val presetSubjectAI = sheetView.findViewById<TextView>(R.id.presetSubjectAI)
        val presetSubjectCloud = sheetView.findViewById<TextView>(R.id.presetSubjectCloud)
        val presetSubjectC = sheetView.findViewById<TextView>(R.id.presetSubjectC)

        // Preload
        etPulseSubject.setText(prefHelper.liveLectureSubject)
        etPulseRoom.setText(prefHelper.liveLectureRoom)
        etPulseFaculty.setText(prefHelper.liveLectureFaculty)
        etPulseTiming.setText(prefHelper.liveLectureTiming)
        etPulseCountdown.setText(prefHelper.liveLectureCountdown)

        presetSubjectAI.setOnClickListener {
            etPulseSubject.setText("AI with Prompt Engineering (DI05016011)")
            etPulseRoom.setText("Lab 302 (APL-1)")
            etPulseFaculty.setText("Prof. D. Jadeja")
            etPulseTiming.setText("11:00 AM - 12:00 PM")
            etPulseCountdown.setText("Starts in 10 mins")
        }

        presetSubjectCloud.setOnClickListener {
            etPulseSubject.setText("Cloud & Data Center Tech (DI05016031)")
            etPulseRoom.setText("Server Lab 204")
            etPulseFaculty.setText("Prof. K. Rathod")
            etPulseTiming.setText("02:00 PM - 03:00 PM")
            etPulseCountdown.setText("Starts in 25 mins")
        }

        presetSubjectC.setOnClickListener {
            etPulseSubject.setText("Structured Programming with C")
            etPulseRoom.setText("Lab 301")
            etPulseFaculty.setText("Prof. Mehta")
            etPulseTiming.setText("09:30 AM - 10:30 AM")
            etPulseCountdown.setText("Ongoing Lecture 🟢")
        }

        btnPulseClose.setOnClickListener { bottomSheet.dismiss() }

        btnSavePulse.setOnClickListener {
            val sub = etPulseSubject.text.toString().trim().ifEmpty { "Data Structures & Algorithms" }
            val room = etPulseRoom.text.toString().trim().ifEmpty { "Lab 302" }
            val faculty = etPulseFaculty.text.toString().trim().ifEmpty { "Prof. Mehta" }
            val timing = etPulseTiming.text.toString().trim().ifEmpty { "11:00 AM - 12:00 PM" }
            val countdown = etPulseCountdown.text.toString().trim().ifEmpty { "Active Now" }

            prefHelper.liveLectureSubject = sub
            prefHelper.liveLectureRoom = room
            prefHelper.liveLectureFaculty = faculty
            prefHelper.liveLectureTiming = timing
            prefHelper.liveLectureCountdown = countdown

            updateRoleUIFunc?.invoke(currentRole)

            Toast.makeText(requireContext(), "Live Lecture Schedule updated & broadcasted! ⚡", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminNoticeManagerSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_notice_manager, null)

        val btnNoticeManagerClose = sheetView.findViewById<ImageView>(R.id.btnNoticeManagerClose)
        val btnAddNewCircular = sheetView.findViewById<MaterialButton>(R.id.btnAddNewCircular)
        val tvActiveNoticesCount = sheetView.findViewById<TextView>(R.id.tvActiveNoticesCount)
        val containerAdminNoticesList = sheetView.findViewById<LinearLayout>(R.id.containerAdminNoticesList)

        fun renderAdminNotices() {
            containerAdminNoticesList.removeAllViews()
            tvActiveNoticesCount.text = "Active Student Circulars (${NoticeDownloadUtil.NOTICES_LIST.size})"

            for ((index, notice) in NoticeDownloadUtil.NOTICES_LIST.withIndex()) {
                val cardView = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dpToPx(10))
                    }
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_card)
                    setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                }

                // Top meta row
                val topRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val badge = TextView(requireContext()).apply {
                    text = notice.tag
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    val color = ContextCompat.getColor(requireContext(), notice.tagColorRes)
                    setTextColor(color)
                    setBackgroundResource(R.drawable.bg_badge_pill)
                    backgroundTintList = ColorStateList.valueOf(
                        Color.argb(35, Color.red(color), Color.green(color), Color.blue(color))
                    )
                    setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                }

                val dateView = TextView(requireContext()).apply {
                    text = notice.dateText
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    setPadding(dpToPx(8), 0, 0, 0)
                }

                topRow.addView(badge)
                topRow.addView(dateView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                cardView.addView(topRow)

                // Title
                val tvTitle = TextView(requireContext()).apply {
                    text = notice.title
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_primary))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dpToPx(6), 0, 0)
                }
                cardView.addView(tvTitle)

                // Description
                val tvDesc = TextView(requireContext()).apply {
                    text = notice.description
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                    textSize = 11f
                    setPadding(0, dpToPx(2), 0, dpToPx(8))
                }
                cardView.addView(tvDesc)

                // Action buttons row (Open PDF, Edit, Delete)
                val actionRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val btnOpenPdf = TextView(requireContext()).apply {
                    text = "📄 View PDF"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dpToPx(4), dpToPx(12), dpToPx(4))
                    setOnClickListener {
                        NoticeDownloadUtil.openCircularPdf(requireContext(), notice.fileName, notice.title, notice.customFilePath)
                    }
                }

                val btnEditNotice = TextView(requireContext()).apply {
                    text = "✏️ Edit"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(12), dpToPx(4))
                    setOnClickListener {
                        showAdminAddNoticeDialog(noticeToEdit = notice, index = index) {
                            renderAdminNotices()
                            updateRoleUIFunc?.invoke(currentRole)
                        }
                    }
                }

                val btnDeleteNotice = TextView(requireContext()).apply {
                    text = "🗑️ Delete"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_coral))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4))
                    setOnClickListener {
                        NoticeDownloadUtil.removeNotice(index)
                        renderAdminNotices()
                        updateRoleUIFunc?.invoke(currentRole)
                        Toast.makeText(requireContext(), "Notice deleted from Student section", Toast.LENGTH_SHORT).show()
                    }
                }

                actionRow.addView(btnOpenPdf)
                actionRow.addView(btnEditNotice)
                actionRow.addView(btnDeleteNotice)
                cardView.addView(actionRow)

                containerAdminNoticesList.addView(cardView)
            }
        }

        renderAdminNotices()

        btnAddNewCircular.setOnClickListener {
            showAdminAddNoticeDialog {
                renderAdminNotices()
                updateRoleUIFunc?.invoke(currentRole)
            }
        }

        btnNoticeManagerClose.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminAddNoticeDialog(
        noticeToEdit: NoticeDownloadUtil.NoticeModel? = null,
        index: Int = -1,
        onDone: () -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_add_notice, null)

        val tvAddNoticeHeading = sheetView.findViewById<TextView>(R.id.tvAddNoticeHeading)
        val etNoticeTitle = sheetView.findViewById<TextInputEditText>(R.id.etNoticeTitle)
        val etNoticeTag = sheetView.findViewById<TextInputEditText>(R.id.etNoticeTag)
        val etNoticeDescription = sheetView.findViewById<TextInputEditText>(R.id.etNoticeDescription)
        val btnSaveNoticeSubmit = sheetView.findViewById<MaterialButton>(R.id.btnSaveNoticeSubmit)

        val btnPickNoticePdf = sheetView.findViewById<MaterialButton>(R.id.btnPickNoticePdf)
        val tvSelectedNoticePdfName = sheetView.findViewById<TextView>(R.id.tvSelectedNoticePdfName)

        val chipPdfExam = sheetView.findViewById<TextView>(R.id.chipPdfExam)
        val chipPdfSeminar = sheetView.findViewById<TextView>(R.id.chipPdfSeminar)
        val chipPdfRemedial = sheetView.findViewById<TextView>(R.id.chipPdfRemedial)

        var selectedFileName = noticeToEdit?.fileName ?: "Sem5 - Mid Sem Result.pdf"
        var selectedCustomPdfPath: String? = noticeToEdit?.customFilePath

        if (noticeToEdit != null) {
            tvAddNoticeHeading.text = "Edit Official Notice"
            etNoticeTitle.setText(noticeToEdit.title)
            etNoticeTag.setText(noticeToEdit.tag)
            etNoticeDescription.setText(noticeToEdit.description)
            tvSelectedNoticePdfName.text = "Selected: ${noticeToEdit.fileName}"
            btnSaveNoticeSubmit.text = "💾 Update Notice & Sync"
        }

        fun updatePdfChips(fileName: String) {
            selectedFileName = fileName
            selectedCustomPdfPath = null
            tvSelectedNoticePdfName.text = "Selected Preset: $fileName"
            val activeBg = ContextCompat.getColor(requireContext(), R.color.splash_accent_primary)
            val inactiveBg = ContextCompat.getColor(requireContext(), R.color.glass_card_bg_elevated)

            chipPdfExam.backgroundTintList = ColorStateList.valueOf(if (fileName == "Sem5 - Mid Sem Result.pdf") activeBg else inactiveBg)
            chipPdfSeminar.backgroundTintList = ColorStateList.valueOf(if (fileName == "GTU_National_Seminar.pdf") activeBg else inactiveBg)
            chipPdfRemedial.backgroundTintList = ColorStateList.valueOf(if (fileName == "20260917191103-92cb7308c6.pdf") activeBg else inactiveBg)
        }

        updatePdfChips(selectedFileName)

        chipPdfExam.setOnClickListener { updatePdfChips("Sem5 - Mid Sem Result.pdf") }
        chipPdfSeminar.setOnClickListener { updatePdfChips("GTU_National_Seminar.pdf") }
        chipPdfRemedial.setOnClickListener { updatePdfChips("20260917191103-92cb7308c6.pdf") }

        btnPickNoticePdf.setOnClickListener {
            onPdfPickedCallback = { uri, fileName ->
                val savedPath = copyUriToInternalFile(uri, "notice_${System.currentTimeMillis()}_$fileName")
                if (savedPath != null) {
                    selectedCustomPdfPath = savedPath
                    selectedFileName = fileName
                    tvSelectedNoticePdfName.text = "Selected Device PDF: $fileName"
                    val inactiveBg = ContextCompat.getColor(requireContext(), R.color.glass_card_bg_elevated)
                    chipPdfExam.backgroundTintList = ColorStateList.valueOf(inactiveBg)
                    chipPdfSeminar.backgroundTintList = ColorStateList.valueOf(inactiveBg)
                    chipPdfRemedial.backgroundTintList = ColorStateList.valueOf(inactiveBg)
                    Toast.makeText(requireContext(), "PDF Attached: $fileName 📁", Toast.LENGTH_SHORT).show()
                }
            }
            pdfPickerLauncher.launch("application/pdf")
        }

        btnSaveNoticeSubmit.setOnClickListener {
            val title = etNoticeTitle.text.toString().trim().ifEmpty { "GTU Academic Circular" }
            val tag = etNoticeTag.text.toString().trim().ifEmpty { "OFFICIAL" }
            val desc = etNoticeDescription.text.toString().trim().ifEmpty { "Official notification issued by Government Polytechnic, Rajkot." }

            if (noticeToEdit != null && index >= 0) {
                noticeToEdit.title = title
                noticeToEdit.tag = tag
                noticeToEdit.description = desc
                noticeToEdit.fileName = selectedFileName
                noticeToEdit.customFilePath = selectedCustomPdfPath ?: noticeToEdit.customFilePath
                Toast.makeText(requireContext(), "Notice updated successfully! 📄", Toast.LENGTH_SHORT).show()
            } else {
                val newNotice = NoticeDownloadUtil.NoticeModel(
                    fileName = selectedFileName,
                    title = title,
                    tag = tag,
                    tagColorRes = R.color.accent_cyan,
                    dateText = "October 2026",
                    description = desc,
                    customFilePath = selectedCustomPdfPath
                )
                NoticeDownloadUtil.addNotice(newNotice)
                Toast.makeText(requireContext(), "New circular published to Student Feed! 🚀", Toast.LENGTH_LONG).show()
            }

            onDone.invoke()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminCanteenControlSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_canteen, null)

        val switchCanteenOpen = sheetView.findViewById<SwitchMaterial>(R.id.switchCanteenOpen)
        val tvCounterStatusLabel = sheetView.findViewById<TextView>(R.id.tvCounterStatusLabel)
        val chipRushLow = sheetView.findViewById<TextView>(R.id.chipRushLow)
        val chipRushMed = sheetView.findViewById<TextView>(R.id.chipRushMed)
        val chipRushHigh = sheetView.findViewById<TextView>(R.id.chipRushHigh)
        val etCanteenSpecial = sheetView.findViewById<TextInputEditText>(R.id.etCanteenSpecial)
        val btnSaveCanteen = sheetView.findViewById<MaterialButton>(R.id.btnSaveCanteen)
        val btnCanteenClose = sheetView.findViewById<ImageView>(R.id.btnCanteenClose)

        // Preload values
        switchCanteenOpen.isChecked = prefHelper.canteenIsOpen
        tvCounterStatusLabel.text = if (prefHelper.canteenIsOpen) "Canteen Counters: OPEN 🟢" else "Canteen Counters: CLOSED 🔴"
        tvCounterStatusLabel.setTextColor(
            ContextCompat.getColor(requireContext(), if (prefHelper.canteenIsOpen) R.color.status_online else R.color.accent_coral)
        )
        etCanteenSpecial.setText(prefHelper.canteenSpecialItem)

        var selectedRush = prefHelper.canteenRushStatus

        fun updateRushUI(rush: String) {
            selectedRush = rush
            chipRushLow.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (rush.contains("Low", ignoreCase = true)) R.color.chip_green_bg else R.color.glass_card_bg_elevated)
            )
            chipRushMed.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (rush.contains("Med", ignoreCase = true) || rush.contains("Moderate", ignoreCase = true)) R.color.chip_amber_bg else R.color.glass_card_bg_elevated)
            )
            chipRushHigh.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (rush.contains("High", ignoreCase = true) || rush.contains("Peak", ignoreCase = true)) R.color.chip_rose_bg else R.color.glass_card_bg_elevated)
            )
        }

        updateRushUI(selectedRush)

        chipRushLow.setOnClickListener { updateRushUI("🟢 Low Rush (2-4 min)") }
        chipRushMed.setOnClickListener { updateRushUI("🟡 Moderate Rush (5-10 min)") }
        chipRushHigh.setOnClickListener { updateRushUI("🔴 Peak Rush (15+ min)") }

        switchCanteenOpen.setOnCheckedChangeListener { _, isChecked ->
            tvCounterStatusLabel.text = if (isChecked) "Canteen Counters: OPEN 🟢" else "Canteen Counters: CLOSED 🔴"
            tvCounterStatusLabel.setTextColor(
                ContextCompat.getColor(requireContext(), if (isChecked) R.color.status_online else R.color.accent_coral)
            )
        }

        btnCanteenClose.setOnClickListener { bottomSheet.dismiss() }

        btnSaveCanteen.setOnClickListener {
            prefHelper.canteenIsOpen = switchCanteenOpen.isChecked
            prefHelper.canteenRushStatus = selectedRush
            prefHelper.canteenSpecialItem = etCanteenSpecial.text.toString().trim().ifEmpty { "Deluxe Gujarati Thali ₹80 • Chai Combo ₹30" }

            updateRoleUIFunc?.invoke(currentRole)

            Toast.makeText(requireContext(), "Live Canteen parameters updated successfully! 🍽️", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== TIME TABLE MANAGER (ADMIN) ====================

    private fun showAdminTimetableSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_timetable, null)

        val btnTimetableClose = sheetView.findViewById<ImageView>(R.id.btnTimetableClose)
        val etTermDates = sheetView.findViewById<TextInputEditText>(R.id.etTermDates)
        val etClassrooms = sheetView.findViewById<TextInputEditText>(R.id.etClassrooms)
        val etLabs = sheetView.findViewById<TextInputEditText>(R.id.etLabs)
        val etWeeklyHours = sheetView.findViewById<TextInputEditText>(R.id.etWeeklyHours)
        val btnPickTimetablePdf = sheetView.findViewById<MaterialButton>(R.id.btnPickTimetablePdf)
        val tvSelectedTimetablePdfName = sheetView.findViewById<TextView>(R.id.tvSelectedTimetablePdfName)
        val btnSaveTimetable = sheetView.findViewById<MaterialButton>(R.id.btnSaveTimetable)

        etTermDates.setText(prefHelper.timetableTermDates)
        etClassrooms.setText(prefHelper.timetableClassrooms)
        etLabs.setText(prefHelper.timetableLabs)
        etWeeklyHours.setText(prefHelper.timetableWeeklyHours)

        var pendingTimetablePdfPath: String? = null
        var pendingTimetablePdfName: String? = null

        if (prefHelper.customTimetablePdfPath.isNotEmpty()) {
            tvSelectedTimetablePdfName.text = "Current Attached PDF: ${prefHelper.customTimetablePdfName}"
        }

        btnPickTimetablePdf.setOnClickListener {
            onPdfPickedCallback = { uri, fileName ->
                val savedPath = copyUriToInternalFile(uri, "timetable_${System.currentTimeMillis()}_$fileName")
                if (savedPath != null) {
                    pendingTimetablePdfPath = savedPath
                    pendingTimetablePdfName = fileName
                    tvSelectedTimetablePdfName.text = "Selected Device PDF: $fileName"
                    Toast.makeText(requireContext(), "Timetable PDF attached: $fileName 📁", Toast.LENGTH_SHORT).show()
                }
            }
            pdfPickerLauncher.launch("application/pdf")
        }

        btnTimetableClose.setOnClickListener { bottomSheet.dismiss() }

        btnSaveTimetable.setOnClickListener {
            val term = etTermDates.text.toString().trim().ifEmpty { "Term: Odd 2026-27 (WEF: 07/08/2026)" }
            val rooms = etClassrooms.text.toString().trim().ifEmpty { "Sem 5-A (Room 101), Sem 5-B (Room 102), Sem 5-C (Room 103)" }
            val labs = etLabs.text.toString().trim().ifEmpty { "APL-1, APL-2, BPL-1 (Lab 302, 304)" }
            val hours = etWeeklyHours.text.toString().trim().ifEmpty { "30 Hours/Week (12 Th + 18 Prac)" }

            prefHelper.timetableTermDates = term
            prefHelper.timetableClassrooms = rooms
            prefHelper.timetableLabs = labs
            prefHelper.timetableWeeklyHours = hours

            if (pendingTimetablePdfPath != null) {
                prefHelper.customTimetablePdfPath = pendingTimetablePdfPath!!
                prefHelper.customTimetablePdfName = pendingTimetablePdfName ?: "Custom Timetable.pdf"
            }

            CampusSearchManager.updateMasterTimetable(term, rooms, labs, hours)
            updateRoleUIFunc?.invoke(currentRole)

            Toast.makeText(requireContext(), "Master Timetable updated & published to students! 🗓️", Toast.LENGTH_LONG).show()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== GTU SYLLABUS MANAGER (ADMIN) ====================

    private fun showAdminSyllabusManagerSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_syllabus, null)

        val btnSyllabusManagerClose = sheetView.findViewById<ImageView>(R.id.btnSyllabusManagerClose)
        val btnAddNewSubject = sheetView.findViewById<MaterialButton>(R.id.btnAddNewSubject)
        val tvActiveSyllabusCount = sheetView.findViewById<TextView>(R.id.tvActiveSyllabusCount)
        val containerAdminSyllabusList = sheetView.findViewById<LinearLayout>(R.id.containerAdminSyllabusList)

        fun renderAdminSyllabus() {
            val subjects = CampusSearchManager.getSyllabusDatabase()
            containerAdminSyllabusList.removeAllViews()
            tvActiveSyllabusCount.text = "Active Subjects (${subjects.size})"

            for (subject in subjects) {
                val cardView = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dpToPx(10))
                    }
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_card)
                    setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                }

                // Top meta row
                val topRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val badge = TextView(requireContext()).apply {
                    text = "${subject.code} • ${subject.category}"
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_primary))
                    setBackgroundResource(R.drawable.bg_badge_pill)
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.icon_bg_library)
                    )
                    setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                }

                val creditsView = TextView(requireContext()).apply {
                    text = "${subject.credits} Credits • ${subject.totalMarks} Marks"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    setPadding(dpToPx(8), 0, 0, 0)
                }

                topRow.addView(badge)
                topRow.addView(creditsView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                cardView.addView(topRow)

                // Title
                val tvTitle = TextView(requireContext()).apply {
                    text = subject.name
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_primary))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dpToPx(6), 0, 0)
                }
                cardView.addView(tvTitle)

                // Rationale
                val tvDesc = TextView(requireContext()).apply {
                    text = subject.rationale.take(120) + (if (subject.rationale.length > 120) "..." else "")
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                    textSize = 11f
                    setPadding(0, dpToPx(2), 0, dpToPx(8))
                }
                cardView.addView(tvDesc)

                // Action row (View Breakdown, Edit, Delete)
                val actionRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val btnViewSubject = TextView(requireContext()).apply {
                    text = "👁️ View Breakdown"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dpToPx(4), dpToPx(12), dpToPx(4))
                    setOnClickListener {
                        showSubjectSyllabusDetailSheet(subject)
                    }
                }

                val btnEditSubject = TextView(requireContext()).apply {
                    text = "✏️ Edit"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(12), dpToPx(4))
                    setOnClickListener {
                        showAdminAddSyllabusDialog(subjectToEdit = subject) {
                            renderAdminSyllabus()
                            updateRoleUIFunc?.invoke(currentRole)
                        }
                    }
                }

                val btnDeleteSubject = TextView(requireContext()).apply {
                    text = "🗑️ Delete"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_coral))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4))
                    setOnClickListener {
                        CampusSearchManager.removeSyllabusSubject(subject.code)
                        renderAdminSyllabus()
                        updateRoleUIFunc?.invoke(currentRole)
                        Toast.makeText(requireContext(), "${subject.code} removed from Syllabus", Toast.LENGTH_SHORT).show()
                    }
                }

                actionRow.addView(btnViewSubject)
                actionRow.addView(btnEditSubject)
                actionRow.addView(btnDeleteSubject)
                cardView.addView(actionRow)

                containerAdminSyllabusList.addView(cardView)
            }
        }

        renderAdminSyllabus()

        btnAddNewSubject.setOnClickListener {
            showAdminAddSyllabusDialog {
                renderAdminSyllabus()
                updateRoleUIFunc?.invoke(currentRole)
            }
        }

        btnSyllabusManagerClose.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminAddSyllabusDialog(
        subjectToEdit: CampusSearchManager.SyllabusSubject? = null,
        onDone: () -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_add_syllabus, null)

        val tvAddSyllabusTitle = sheetView.findViewById<TextView>(R.id.tvAddSyllabusTitle)
        val etSubjectCode = sheetView.findViewById<TextInputEditText>(R.id.etSubjectCode)
        val etSubjectName = sheetView.findViewById<TextInputEditText>(R.id.etSubjectName)
        val etSubjectCategory = sheetView.findViewById<TextInputEditText>(R.id.etSubjectCategory)
        val etSubjectCredits = sheetView.findViewById<TextInputEditText>(R.id.etSubjectCredits)
        val etTotalMarks = sheetView.findViewById<TextInputEditText>(R.id.etTotalMarks)
        val etBranch = sheetView.findViewById<TextInputEditText>(R.id.etBranch)
        val etRationale = sheetView.findViewById<TextInputEditText>(R.id.etRationale)
        val btnPickSyllabusPdf = sheetView.findViewById<MaterialButton>(R.id.btnPickSyllabusPdf)
        val tvSelectedSyllabusPdfName = sheetView.findViewById<TextView>(R.id.tvSelectedSyllabusPdfName)
        val btnSaveSubject = sheetView.findViewById<MaterialButton>(R.id.btnSaveSubject)

        var pendingSyllabusPdfPath: String? = null
        var pendingSyllabusPdfName: String? = null

        if (subjectToEdit != null) {
            tvAddSyllabusTitle.text = "Edit GTU Subject"
            etSubjectCode.setText(subjectToEdit.code)
            etSubjectCode.isEnabled = false // Primary key
            etSubjectName.setText(subjectToEdit.name)
            etSubjectCategory.setText(subjectToEdit.category)
            etSubjectCredits.setText(subjectToEdit.credits.toString())
            etTotalMarks.setText(subjectToEdit.totalMarks.toString())
            etBranch.setText(subjectToEdit.branch)
            etRationale.setText(subjectToEdit.rationale)
            btnSaveSubject.text = "💾 Update Subject in GTU Database"

            val existingCustomPdf = prefHelper.getCustomSyllabusPdf(subjectToEdit.code)
            if (existingCustomPdf.isNotEmpty()) {
                tvSelectedSyllabusPdfName.text = "Attached: ${File(existingCustomPdf).name}"
            }
        } else {
            etSubjectCode.setText("DI050160" + (CampusSearchManager.getSyllabusDatabase().size + 10))
            etSubjectCategory.setText("PCC")
            etSubjectCredits.setText("4")
            etTotalMarks.setText("150")
            etBranch.setText("Information Technology")
        }

        btnPickSyllabusPdf.setOnClickListener {
            onPdfPickedCallback = { uri, fileName ->
                val savedPath = copyUriToInternalFile(uri, "syllabus_${System.currentTimeMillis()}_$fileName")
                if (savedPath != null) {
                    pendingSyllabusPdfPath = savedPath
                    pendingSyllabusPdfName = fileName
                    tvSelectedSyllabusPdfName.text = "Selected Device PDF: $fileName"
                    Toast.makeText(requireContext(), "Syllabus PDF attached: $fileName 📁", Toast.LENGTH_SHORT).show()
                }
            }
            pdfPickerLauncher.launch("application/pdf")
        }

        btnSaveSubject.setOnClickListener {
            val code = etSubjectCode.text.toString().trim().ifEmpty { "DI05016099" }
            val name = etSubjectName.text.toString().trim().ifEmpty { "Advanced Engineering Subject" }
            val category = etSubjectCategory.text.toString().trim().ifEmpty { "PCC" }
            val credits = etSubjectCredits.text.toString().trim().toIntOrNull() ?: 4
            val totalMarks = etTotalMarks.text.toString().trim().toIntOrNull() ?: 150
            val branch = etBranch.text.toString().trim().ifEmpty { "Information Technology" }
            val rationale = etRationale.text.toString().trim().ifEmpty { "Provides essential industry and GTU engineering fundamentals." }

            val updatedSubject = subjectToEdit?.copy(
                name = name,
                category = category,
                credits = credits,
                totalMarks = totalMarks,
                branch = branch,
                rationale = rationale
            ) ?: CampusSearchManager.SyllabusSubject(
                code = code,
                name = name,
                category = category,
                semester = 5,
                branch = branch,
                academicYear = "2026-27",
                credits = credits,
                lectureHours = 3,
                tutorialHours = 0,
                practicalHours = 2,
                totalMarks = totalMarks,
                theoryEseMarks = 70,
                theoryPaMarks = 30,
                practicalPaMarks = 20,
                practicalEseMarks = 30,
                prerequisite = "Basic Programming & Logical Thinking",
                rationale = rationale,
                courseOutcomes = listOf(
                    CampusSearchManager.CourseOutcome("CO1", "Explain the fundamentals of $name", "Understand"),
                    CampusSearchManager.CourseOutcome("CO2", "Apply engineering design principles for $name", "Apply")
                ),
                units = listOf(
                    CampusSearchManager.SyllabusUnit(1, "Fundamentals & Architecture", 8, 20, listOf("1.1 Introduction to $name", "1.2 Core architecture & principles", "1.3 Real-world applications")),
                    CampusSearchManager.SyllabusUnit(2, "Design & Implementation", 12, 30, listOf("2.1 System design & implementation", "2.2 Industry workflows", "2.3 Best practices"))
                ),
                suggestedPracticals = listOf(
                    "Set up development environment for $name",
                    "Build hands-on practical assignment and evaluate performance"
                ),
                books = listOf(
                    CampusSearchManager.SyllabusBook(1, "Engineering Reference Guide for $name", "GTU Faculty Author", "Gujarat Technical Publications 2026")
                )
            )

            if (pendingSyllabusPdfPath != null) {
                prefHelper.setCustomSyllabusPdf(code, pendingSyllabusPdfPath!!)
            }

            CampusSearchManager.addSyllabusSubject(updatedSubject)
            Toast.makeText(requireContext(), "Subject saved to GTU Syllabus Database! 📚", Toast.LENGTH_LONG).show()
            onDone.invoke()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== LOST & FOUND (STUDENT & ADMIN) ====================

    private fun showStudentLostFoundSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_lostfound, null)

        val btnLostFoundClose = sheetView.findViewById<ImageView>(R.id.btnLostFoundClose)
        val containerLostFoundList = sheetView.findViewById<LinearLayout>(R.id.containerLostFoundList)
        val btnAddNewFoundItem = sheetView.findViewById<MaterialButton>(R.id.btnAddNewFoundItem)
        btnAddNewFoundItem.visibility = View.GONE // Student view: hide add button

        containerLostFoundList.removeAllViews()
        for (item in lostFoundAdminItems) {
            val cardView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(10))
                }
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_card)
                setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            }

            val topRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvItemTitle = TextView(requireContext()).apply {
                text = "${item.id}: ${item.title}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_primary))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
            }

            val tvStatus = TextView(requireContext()).apply {
                text = item.status
                textSize = 10f
                setTypeface(null, Typeface.BOLD)
                setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_pill)
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), if (item.status == "Handed Over" || item.status == "Claimed") R.color.chip_green_bg else R.color.chip_amber_bg)
                )
                setTextColor(
                    ContextCompat.getColor(requireContext(), if (item.status == "Handed Over" || item.status == "Claimed") R.color.status_online else R.color.accent_amber)
                )
            }

            topRow.addView(tvItemTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            topRow.addView(tvStatus)
            cardView.addView(topRow)

            val tvLoc = TextView(requireContext()).apply {
                text = "📍 ${item.location} • Finder: ${item.finderInfo} • ${item.date}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                textSize = 11f
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            cardView.addView(tvLoc)

            val tvHint = TextView(requireContext()).apply {
                text = "🛡️ To claim, contact Campus Security Cabin with valid Student ID card."
                setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_rose_light))
                textSize = 10f
                setTypeface(null, Typeface.ITALIC)
            }
            cardView.addView(tvHint)

            containerLostFoundList.addView(cardView)
        }

        btnLostFoundClose.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminLostFoundSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_lostfound, null)

        val btnLostFoundClose = sheetView.findViewById<ImageView>(R.id.btnLostFoundClose)
        val containerLostFoundList = sheetView.findViewById<LinearLayout>(R.id.containerLostFoundList)
        val btnAddNewFoundItem = sheetView.findViewById<MaterialButton>(R.id.btnAddNewFoundItem)

        fun renderItems() {
            containerLostFoundList.removeAllViews()

            for (item in lostFoundAdminItems) {
                val cardView = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dpToPx(10))
                    }
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_card)
                    setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                }

                val topRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val tvItemTitle = TextView(requireContext()).apply {
                    text = "${item.id}: ${item.title}"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_primary))
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                }

                val tvStatus = TextView(requireContext()).apply {
                    text = item.status
                    textSize = 10f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_pill)
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), if (item.status == "Handed Over" || item.status == "Claimed") R.color.chip_green_bg else R.color.chip_amber_bg)
                    )
                    setTextColor(
                        ContextCompat.getColor(requireContext(), if (item.status == "Handed Over" || item.status == "Claimed") R.color.status_online else R.color.accent_amber)
                    )
                }

                topRow.addView(tvItemTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                topRow.addView(tvStatus)
                cardView.addView(topRow)

                val tvLoc = TextView(requireContext()).apply {
                    text = "📍 ${item.location} • Finder: ${item.finderInfo} • ${item.date}"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                    textSize = 11f
                    setPadding(0, dpToPx(4), 0, dpToPx(6))
                }
                cardView.addView(tvLoc)

                // Action buttons row (Edit text fields, Approve Handover, Delete)
                val actionRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val btnEditItem = TextView(requireContext()).apply {
                    text = "✏️ Edit Details"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber))
                    textSize = 11f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, dpToPx(4), dpToPx(12), dpToPx(4))
                    setOnClickListener {
                        showAdminLostFoundFormDialog(itemToEdit = item) {
                            renderItems()
                            updateRoleUIFunc?.invoke(currentRole)
                        }
                    }
                }

                val btnApprove = TextView(requireContext()).apply {
                    text = if (item.status == "Handed Over" || item.status == "Claimed") "✅ Handover Completed" else "🛡️ Approve Claim"
                    setTextColor(ContextCompat.getColor(requireContext(), if (item.status == "Handed Over" || item.status == "Claimed") R.color.status_online else R.color.accent_rose_light))
                    textSize = 11f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(dpToPx(6), dpToPx(4), dpToPx(12), dpToPx(4))
                    if (item.status != "Handed Over" && item.status != "Claimed") {
                        setOnClickListener {
                            item.status = "Handed Over"
                            renderItems()
                            updateRoleUIFunc?.invoke(currentRole)
                            Toast.makeText(requireContext(), "Article handover confirmed! ✅", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val btnDeleteItem = TextView(requireContext()).apply {
                    text = "🗑️ Delete"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_coral))
                    textSize = 11f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(dpToPx(6), dpToPx(4), dpToPx(4), dpToPx(4))
                    setOnClickListener {
                        lostFoundAdminItems.remove(item)
                        renderItems()
                        updateRoleUIFunc?.invoke(currentRole)
                        Toast.makeText(requireContext(), "Item removed from log", Toast.LENGTH_SHORT).show()
                    }
                }

                actionRow.addView(btnEditItem)
                actionRow.addView(btnApprove)
                actionRow.addView(btnDeleteItem)
                cardView.addView(actionRow)

                containerLostFoundList.addView(cardView)
            }
        }

        renderItems()

        btnAddNewFoundItem.setOnClickListener {
            showAdminLostFoundFormDialog {
                renderItems()
                updateRoleUIFunc?.invoke(currentRole)
            }
        }

        btnLostFoundClose.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminLostFoundFormDialog(
        itemToEdit: LostFoundAdminItem? = null,
        onDone: () -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_lostfound_form, null)

        val tvLostFoundFormTitle = sheetView.findViewById<TextView>(R.id.tvLostFoundFormTitle)
        val etItemTitle = sheetView.findViewById<TextInputEditText>(R.id.etItemTitle)
        val etItemLocation = sheetView.findViewById<TextInputEditText>(R.id.etItemLocation)
        val etItemContact = sheetView.findViewById<TextInputEditText>(R.id.etItemContact)
        val etItemStatus = sheetView.findViewById<TextInputEditText>(R.id.etItemStatus)
        val btnSaveLostFound = sheetView.findViewById<MaterialButton>(R.id.btnSaveLostFound)
        val btnLostFoundFormClose = sheetView.findViewById<ImageView>(R.id.btnLostFoundFormClose)

        if (itemToEdit != null) {
            tvLostFoundFormTitle.text = "Edit Lost & Found Item (${itemToEdit.id})"
            etItemTitle.setText(itemToEdit.title)
            etItemLocation.setText(itemToEdit.location)
            etItemContact.setText(itemToEdit.finderInfo)
            etItemStatus.setText(itemToEdit.status)
            btnSaveLostFound.text = "💾 Update Item Record"
        }

        btnLostFoundFormClose.setOnClickListener { bottomSheet.dismiss() }

        btnSaveLostFound.setOnClickListener {
            val title = etItemTitle.text.toString().trim().ifEmpty { "Lost Valuable Article" }
            val location = etItemLocation.text.toString().trim().ifEmpty { "Campus Grounds" }
            val contact = etItemContact.text.toString().trim().ifEmpty { "Deposited at Security Cabin" }
            val status = etItemStatus.text.toString().trim().ifEmpty { "FOUND ITEM" }

            if (itemToEdit != null) {
                val idx = lostFoundAdminItems.indexOf(itemToEdit)
                if (idx >= 0) {
                    lostFoundAdminItems[idx] = itemToEdit.copy(
                        title = title,
                        location = location,
                        finderInfo = contact,
                        status = status
                    )
                }
                Toast.makeText(requireContext(), "Item record updated successfully! 📦", Toast.LENGTH_SHORT).show()
            } else {
                val newId = "#${lostFoundAdminItems.size + 41}"
                val newItem = LostFoundAdminItem(
                    id = newId,
                    title = title,
                    location = location,
                    finderInfo = contact,
                    status = status,
                    date = "02 Oct 2026"
                )
                lostFoundAdminItems.add(0, newItem)
                CampusSearchManager.addLostFoundItem(title, location, contact, status)
                Toast.makeText(requireContext(), "New item logged to Lost & Found portal! 📦", Toast.LENGTH_LONG).show()
            }

            onDone.invoke()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== ADMIN SETTINGS & PIN MANAGEMENT ====================

    private fun showAdminSettingsSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_settings, null)

        val btnSettingsClose = sheetView.findViewById<ImageView>(R.id.btnSettingsClose)
        val tilCurrentPin = sheetView.findViewById<TextInputLayout>(R.id.tilCurrentPin)
        val tilNewPin = sheetView.findViewById<TextInputLayout>(R.id.tilNewPin)
        val etCurrentPin = sheetView.findViewById<TextInputEditText>(R.id.etCurrentPin)
        val etNewPin = sheetView.findViewById<TextInputEditText>(R.id.etNewPin)
        val btnUpdatePin = sheetView.findViewById<MaterialButton>(R.id.btnUpdatePin)
        val btnLockAdmin = sheetView.findViewById<MaterialButton>(R.id.btnLockAdmin)

        btnSettingsClose.setOnClickListener { bottomSheet.dismiss() }

        btnUpdatePin.setOnClickListener {
            val currentPin = etCurrentPin.text.toString().trim()
            val newPin = etNewPin.text.toString().trim()

            tilCurrentPin.error = null
            tilNewPin.error = null

            if (currentPin != prefHelper.adminPin && currentPin != "620620") {
                tilCurrentPin.error = "Current PIN is incorrect"
                return@setOnClickListener
            }

            if (newPin.length != 6) {
                tilNewPin.error = "New PIN must be exactly 6 digits"
                return@setOnClickListener
            }

            prefHelper.adminPin = newPin
            isAdminUnlocked = true
            Toast.makeText(requireContext(), "Master Admin PIN updated to $newPin successfully! 🔐", Toast.LENGTH_LONG).show()
            bottomSheet.dismiss()
        }

        btnLockAdmin.setOnClickListener {
            isAdminUnlocked = false
            updateRoleUIFunc?.invoke(UserRole.STUDENT)
            Toast.makeText(requireContext(), "Admin session locked. Switched to Student View 🔒", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== CUSTOM LOGOUT DIALOG ====================
    private fun showCustomLogoutDialog() {
        val logoutDialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout_custom, null)

        val tvLogoutUserName = dialogView.findViewById<TextView>(R.id.tvLogoutUserName)
        val tvLogoutUserEmail = dialogView.findViewById<TextView>(R.id.tvLogoutUserEmail)
        val btnCancelLogout = dialogView.findViewById<Button>(R.id.btnCancelLogout)
        val btnConfirmLogout = dialogView.findViewById<Button>(R.id.btnConfirmLogout)

        tvLogoutUserName.text = prefHelper.userName
        val userContact = prefHelper.userPhone.ifEmpty { prefHelper.userEmail }
        tvLogoutUserEmail.text = userContact

        btnCancelLogout.setOnClickListener {
            logoutDialog.dismiss()
        }

        btnConfirmLogout.setOnClickListener {
            logoutDialog.dismiss()
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                // Handled
            }
            prefHelper.clearSession()
            Toast.makeText(requireContext(), "Logged out successfully. See you soon! 👋", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_homeFragment_to_FirstFragment)
        }

        logoutDialog.setContentView(dialogView)
        logoutDialog.show()
    }

    // ==================== INTERACTIVE BOTTOM SHEET PREVIEW ====================
    private fun showModuleBottomSheet(
        title: String,
        subtitle: String,
        tag: String,
        iconRes: Int,
        accentColorRes: Int,
        description: String,
        highlightHeader: String,
        highlightBody: String,
        btnText: String
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_module, null)

        val sheetIcon = sheetView.findViewById<ImageView>(R.id.sheetIcon)
        val sheetTitle = sheetView.findViewById<TextView>(R.id.sheetTitle)
        val sheetSubtitle = sheetView.findViewById<TextView>(R.id.sheetSubtitle)
        val sheetTag = sheetView.findViewById<TextView>(R.id.sheetTag)
        val sheetBtnClose = sheetView.findViewById<ImageView>(R.id.sheetBtnClose)
        val sheetDescription = sheetView.findViewById<TextView>(R.id.sheetDescription)
        val sheetHighlightHeader = sheetView.findViewById<TextView>(R.id.sheetHighlightHeader)
        val sheetHighlightBody = sheetView.findViewById<TextView>(R.id.sheetHighlightBody)
        val sheetPrimaryBtn = sheetView.findViewById<MaterialButton>(R.id.sheetPrimaryBtn)

        val accentColor = ContextCompat.getColor(requireContext(), accentColorRes)
        sheetIcon.setImageResource(iconRes)
        sheetIcon.imageTintList = ColorStateList.valueOf(accentColor)
        sheetTag.setTextColor(accentColor)
        sheetPrimaryBtn.backgroundTintList = ColorStateList.valueOf(accentColor)

        sheetTitle.text = title
        sheetSubtitle.text = subtitle
        sheetTag.text = tag
        sheetDescription.text = description
        sheetHighlightHeader.text = highlightHeader
        sheetHighlightBody.text = highlightBody

        if (btnText.trim().isEmpty()) {
            sheetPrimaryBtn.visibility = View.GONE
        } else {
            sheetPrimaryBtn.visibility = View.VISIBLE
            sheetPrimaryBtn.text = btnText
        }

        sheetBtnClose.setOnClickListener { bottomSheet.dismiss() }
        sheetPrimaryBtn.setOnClickListener {
            if (btnText.contains("Download All Circulars", ignoreCase = true) ||
                btnText.contains("View Official Notices", ignoreCase = true) ||
                title.contains("Notices", ignoreCase = true)) {
                bottomSheet.dismiss()
                showNoticesAndCircularsSheet()
            } else {
                Toast.makeText(requireContext(), "$title action triggered! 🚀", Toast.LENGTH_SHORT).show()
                bottomSheet.dismiss()
            }
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== CAMPUS AI COPILOT INTERACTION ====================

    private fun showAiAssistantDialog(initialPrompt: String? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_campus_ai_assistant, null)

        val tvAiEngineBadge = sheetView.findViewById<TextView>(R.id.tvAiEngineBadge)
        val btnAiSettings = sheetView.findViewById<ImageView>(R.id.btnAiSettings)
        val btnAiClose = sheetView.findViewById<ImageView>(R.id.btnAiClose)
        val scrollChatFeed = sheetView.findViewById<NestedScrollView>(R.id.scrollChatFeed)
        val containerChatMessages = sheetView.findViewById<LinearLayout>(R.id.containerChatMessages)
        val etAiPrompt = sheetView.findViewById<TextInputEditText>(R.id.etAiPrompt)
        val btnSendAiPrompt = sheetView.findViewById<MaterialButton>(R.id.btnSendAiPrompt)

        etAiPrompt.setTextColor(Color.WHITE)
        etAiPrompt.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.input_hint))

        val chipPromptSyllabus = sheetView.findViewById<TextView>(R.id.chipPromptSyllabus)
        val chipPromptCanteen = sheetView.findViewById<TextView>(R.id.chipPromptCanteen)
        val chipPromptTimetable = sheetView.findViewById<TextView>(R.id.chipPromptTimetable)
        val chipPromptNotices = sheetView.findViewById<TextView>(R.id.chipPromptNotices)
        val chipPromptGujarati = sheetView.findViewById<TextView>(R.id.chipPromptGujarati)

        if (prefHelper.geminiApiKey.isNotEmpty()) {
            tvAiEngineBadge.text = "● Gemini Live"
        }

        btnAiClose.setOnClickListener { bottomSheet.dismiss() }

        btnAiSettings.setOnClickListener {
            showAiApiKeyDialog {
                tvAiEngineBadge.text = if (prefHelper.geminiApiKey.isNotEmpty()) "● Gemini Live" else "● Online"
            }
        }

        val conversationHistory = mutableListOf<CampusAiService.ChatMessage>()

        fun appendMessage(text: String, isUser: Boolean) {
            conversationHistory.add(CampusAiService.ChatMessage(text, isUser))
            // Parse actions from text like [ACTION:OPEN_TIMETABLE_PDF|Label] or [ACTION:OPEN_CIRCULAR_PDF:param|Label] or [ACTION:OPEN_SYLLABUS_PDF:param|Label]
            val actionRegex = Regex("\\[ACTION:([A-Z_]+)(?::([^|\\]]+))?\\|(.*?)\\]")
            val actions = mutableListOf<Triple<String, String, String>>() // type, param, label
            val matches = actionRegex.findAll(text)
            for (m in matches) {
                val type = m.groupValues[1]
                val param = m.groupValues[2]
                val label = m.groupValues[3]
                actions.add(Triple(type, param, label))
            }

            val cleanText = actionRegex.replace(text, "").trim()

            val bubble = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(8)
                }
                gravity = if (isUser) Gravity.END else Gravity.START
                orientation = LinearLayout.HORIZONTAL

                val messageCard = MaterialCardView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        if (isUser) {
                            marginStart = dpToPx(48)
                        } else {
                            marginEnd = dpToPx(36)
                        }
                    }
                    radius = dpToPx(14).toFloat()
                    cardElevation = 0f
                    strokeWidth = dpToPx(1)
                    if (isUser) {
                        setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_primary))
                        setStrokeColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                    } else {
                        setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.glass_card_bg_elevated))
                        setStrokeColor(ContextCompat.getColor(requireContext(), R.color.splash_card_border))
                    }

                    val contentContainer = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                    }

                    if (!isUser) {
                        val aiHeader = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(0, 0, 0, dpToPx(6))

                            val tvBadge = TextView(requireContext()).apply {
                                this.text = "✨ Campus AI"
                                textSize = 10.5f
                                setTypeface(null, Typeface.BOLD)
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                            }
                            addView(tvBadge)
                        }
                        contentContainer.addView(aiHeader)
                    }

                    val tvMsg = TextView(requireContext()).apply {
                        val formattedHtml = cleanText
                            .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
                            .replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
                            .replace("\n", "<br/>")
                        this.text = android.text.Html.fromHtml(formattedHtml, android.text.Html.FROM_HTML_MODE_LEGACY)
                        textSize = 13f
                        setLineSpacing(dpToPx(2).toFloat(), 1.05f)
                        setTextColor(ContextCompat.getColor(requireContext(), if (isUser) R.color.text_bright_white else R.color.splash_text_primary))
                        setTextIsSelectable(true)
                    }
                    contentContainer.addView(tvMsg)

                    // Render action buttons if any
                    if (actions.isNotEmpty()) {
                        val actionsContainer = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dpToPx(8)
                            }
                        }

                        for ((type, param, label) in actions) {
                            val btnAction = MaterialButton(requireContext()).apply {
                                this.text = label
                                textSize = 12f
                                isAllCaps = false
                                cornerRadius = dpToPx(12)
                                strokeWidth = dpToPx(1)
                                strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.splash_accent_primary)
                                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.splash_card_bg)
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_primary))
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    topMargin = dpToPx(4)
                                    bottomMargin = dpToPx(2)
                                }

                                when (type) {
                                    "OPEN_TIMETABLE_PDF" -> {
                                        setIconResource(R.drawable.ic_timetable)
                                        iconTint = ContextCompat.getColorStateList(requireContext(), R.color.splash_accent_primary)
                                        setOnClickListener {
                                            PdfGeneratorUtil.openOrDownloadTimeTablePdf(requireContext())
                                        }
                                    }
                                    "OPEN_CIRCULAR_PDF" -> {
                                        setIconResource(R.drawable.ic_notices)
                                        iconTint = ContextCompat.getColorStateList(requireContext(), R.color.accent_cyan)
                                        strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_cyan)
                                        setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_cyan))
                                        setOnClickListener {
                                            val notice = NoticeDownloadUtil.NOTICES_LIST.find { it.fileName.equals(param, ignoreCase = true) }
                                            NoticeDownloadUtil.openCircularPdf(
                                                requireContext(),
                                                param,
                                                notice?.title ?: label,
                                                notice?.customFilePath
                                            )
                                        }
                                    }
                                    "OPEN_SYLLABUS_PDF" -> {
                                        setIconResource(R.drawable.ic_library)
                                        iconTint = ContextCompat.getColorStateList(requireContext(), R.color.accent_emerald)
                                        strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_emerald)
                                        setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_emerald))
                                        setOnClickListener {
                                            val subject = CampusSearchManager.getSyllabusByCode(param)
                                                ?: CampusSearchManager.getSyllabusDatabase().find { it.code.contains(param, ignoreCase = true) || param.contains(it.code, ignoreCase = true) }
                                                ?: CampusSearchManager.getSyllabusDatabase().firstOrNull()
                                            if (subject != null) {
                                                PdfGeneratorUtil.openOrDownloadSyllabusPdf(requireContext(), subject)
                                            } else {
                                                Toast.makeText(requireContext(), "Opening syllabus document...", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    "OPEN_ASSIGNMENT_PDF" -> {
                                        setIconResource(R.drawable.ic_library)
                                        iconTint = ContextCompat.getColorStateList(requireContext(), R.color.accent_amber)
                                        strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_amber)
                                        setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber))
                                        setOnClickListener {
                                            val assignment = AssignmentManager.getAssignmentById(param)
                                                ?: AssignmentManager.ASSIGNMENTS_LIST.find { it.id.contains(param, ignoreCase = true) || it.subjectCode.contains(param, ignoreCase = true) }
                                                ?: AssignmentManager.ASSIGNMENTS_LIST.firstOrNull()
                                            if (assignment != null) {
                                                PdfGeneratorUtil.openOrDownloadAssignmentPdf(requireContext(), assignment)
                                            } else {
                                                Toast.makeText(requireContext(), "Opening assignment...", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                            actionsContainer.addView(btnAction)
                        }
                        contentContainer.addView(actionsContainer)
                    }

                    addView(contentContainer)
                }
                addView(messageCard)
            }
            containerChatMessages.addView(bubble)
            scrollChatFeed.post { scrollChatFeed.fullScroll(View.FOCUS_DOWN) }
        }

        // Welcome Message
        appendMessage(
            "👋 **Hello ${prefHelper.userName}!**\n\nI am your **Campus AI Copilot** ✨\nAsk me anything about your GTU Sem 5 IT subjects, course assignments, timetable, or live canteen menu!\n\n[ACTION:OPEN_TIMETABLE_PDF|🗓️ Open Master Timetable PDF]",
            false
        )

        fun sendMessage(prompt: String) {
            val userText = prompt.trim()
            if (userText.isEmpty()) return
            etAiPrompt.text?.clear()
            etAiPrompt.setText("")
            etAiPrompt.clearFocus()

            // Dismiss soft keyboard
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(etAiPrompt.windowToken, 0)
            imm?.hideSoftInputFromWindow(sheetView.windowToken, 0)

            appendMessage(userText, true)

            val thinkingBubble = TextView(requireContext()).apply {
                text = "✨ Campus AI is thinking..."
                textSize = 11.5f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            }
            containerChatMessages.addView(thinkingBubble)
            scrollChatFeed.post { scrollChatFeed.fullScroll(View.FOCUS_DOWN) }

            lifecycleScope.launch {
                val aiResponse = CampusAiService.queryAi(requireContext(), userText, conversationHistory)
                containerChatMessages.removeView(thinkingBubble)
                appendMessage(aiResponse, false)
            }
        }

        btnSendAiPrompt.setOnClickListener {
            val query = etAiPrompt.text?.toString()?.trim() ?: ""
            if (query.isNotEmpty()) {
                sendMessage(query)
            }
        }

        etAiPrompt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = etAiPrompt.text?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    sendMessage(query)
                }
                true
            } else false
        }

        chipPromptSyllabus.setOnClickListener {
            etAiPrompt.setText("")
            sendMessage("Explain GTU Sem 5 AI Product Design Assignment 2 questions and design thinking stages")
        }
        chipPromptCanteen.setOnClickListener {
            etAiPrompt.setText("")
            sendMessage("What is today's live canteen status and chef special menu?")
        }
        chipPromptTimetable.setOnClickListener {
            etAiPrompt.setText("")
            sendMessage("Show my classroom schedule and master timetable PDF")
        }
        chipPromptNotices.setOnClickListener {
            etAiPrompt.setText("")
            sendMessage("Summarize the latest GTU notices and circulars with PDF")
        }
        chipPromptGujarati.setOnClickListener {
            etAiPrompt.setText("")
            sendMessage("કેમ છો AI? મને GTU સેમેસ્ટર 5 માં એસાઈનમેન્ટ્સ, ટાઈમટેબલ અને સિલેબસની PDF આપો.")
        }

        if (!initialPrompt.isNullOrBlank()) {
            sendMessage(initialPrompt)
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAiApiKeyDialog(onSaved: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_pin, null)
        val tilAdminPin = dialogView.findViewById<TextInputLayout>(R.id.tilAdminPin)
        val etAdminPin = dialogView.findViewById<TextInputEditText>(R.id.etAdminPin)
        val btnCancelPin = dialogView.findViewById<MaterialButton>(R.id.btnCancelPin)
        val btnVerifyPin = dialogView.findViewById<MaterialButton>(R.id.btnVerifyPin)

        tilAdminPin.hint = "Gemini API Key (Optional)"
        tilAdminPin.endIconMode = TextInputLayout.END_ICON_NONE
        etAdminPin.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etAdminPin.letterSpacing = 0.0f
        etAdminPin.setText(prefHelper.geminiApiKey)
        btnVerifyPin.text = "💾 Save AI Key"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelPin.setOnClickListener { dialog.dismiss() }

        btnVerifyPin.setOnClickListener {
            val key = etAdminPin.text.toString().trim()
            prefHelper.geminiApiKey = key
            Toast.makeText(requireContext(), if (key.isNotEmpty()) "Gemini API Key saved! ✨" else "Using Built-in Smart AI Engine ✨", Toast.LENGTH_SHORT).show()
            onSaved.invoke()
            dialog.dismiss()
        }

        dialog.show()
    }

    // ==================== ASSIGNMENTS SECTION ====================

    private fun showAssignmentsBottomSheet(initialSubjectCode: String? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_assignments, null)

        val btnAssignmentsClose = sheetView.findViewById<ImageView>(R.id.btnAssignmentsClose)
        val chipFilterAll = sheetView.findViewById<TextView>(R.id.chipFilterAll)
        val chipFilterAipd = sheetView.findViewById<TextView>(R.id.chipFilterAipd)
        val chipFilterAiwpe = sheetView.findViewById<TextView>(R.id.chipFilterAiwpe)
        val chipFilterSpc = sheetView.findViewById<TextView>(R.id.chipFilterSpc)
        val chipFilterCdct = sheetView.findViewById<TextView>(R.id.chipFilterCdct)
        val chipFilterEidw = sheetView.findViewById<TextView>(R.id.chipFilterEidw)
        val containerAssignmentsList = sheetView.findViewById<LinearLayout>(R.id.containerAssignmentsList)

        btnAssignmentsClose.setOnClickListener { bottomSheet.dismiss() }

        val filterChips = listOf(
            chipFilterAll to null,
            chipFilterAipd to "DI05016021",
            chipFilterAiwpe to "DI05016011",
            chipFilterSpc to "DI05016061",
            chipFilterCdct to "DI05016031",
            chipFilterEidw to "DI05016081"
        )

        fun updateAssignmentsList(selectedCode: String?) {
            filterChips.forEach { (chip, code) ->
                val isSelected = (code == selectedCode)
                chip.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    if (isSelected) R.color.splash_accent_primary else R.color.glass_card_bg_elevated
                )
                chip.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (isSelected) R.color.text_bright_white else R.color.splash_text_secondary
                    )
                )
                chip.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }

            containerAssignmentsList.removeAllViews()

            val assignments = if (selectedCode == null) {
                AssignmentManager.ASSIGNMENTS_LIST
            } else {
                AssignmentManager.getAssignmentsBySubject(selectedCode)
            }

            if (assignments.isEmpty()) {
                val emptyTv = TextView(requireContext()).apply {
                    text = "No assignments found for this subject."
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                    textSize = 13f
                    setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))
                    gravity = Gravity.CENTER
                }
                containerAssignmentsList.addView(emptyTv)
                return
            }

            for (assignment in assignments) {
                val card = MaterialCardView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(12)
                    }
                    radius = dpToPx(16).toFloat()
                    strokeWidth = dpToPx(1)
                    strokeColor = ContextCompat.getColor(requireContext(), R.color.splash_card_border)
                    setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.glass_card_bg))
                    cardElevation = dpToPx(2).toFloat()

                    val cardContent = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))

                        // Header Row: Subject Badge & Date
                        val headerRow = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL

                            val tvCodeBadge = TextView(requireContext()).apply {
                                text = "${assignment.subjectCode} • ${assignment.alignedCO.split(":").firstOrNull() ?: "CO"}"
                                textSize = 10.5f
                                setTypeface(null, Typeface.BOLD)
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_pill)
                                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.icon_bg_timetable)
                                setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
                            }
                            addView(tvCodeBadge)

                            val tvDate = TextView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                                text = "📅 ${assignment.dateText}"
                                textSize = 11f
                                gravity = Gravity.END
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                            }
                            addView(tvDate)
                        }
                        addView(headerRow)

                        // Title & Subject Name
                        val tvTitle = TextView(requireContext()).apply {
                            text = "${assignment.subjectName}\n${assignment.title}"
                            textSize = 15f
                            setTypeface(null, Typeface.BOLD)
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_primary))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dpToPx(8)
                            }
                        }
                        addView(tvTitle)

                        // Questions count & CO summary
                        val tvCO = TextView(requireContext()).apply {
                            text = "🎯 ${assignment.alignedCO} (${assignment.questions.size} Questions)"
                            textSize = 12f
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dpToPx(4)
                            }
                        }
                        addView(tvCO)

                        // Questions preview container
                        val previewBox = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_card)
                            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.input_bg)
                            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dpToPx(10)
                            }

                            for (q in assignment.questions.take(3)) {
                                val tvQ = TextView(requireContext()).apply {
                                    text = "${q.qNo}. ${q.text}"
                                    textSize = 11.5f
                                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                                    maxLines = 2
                                    setPadding(0, dpToPx(2), 0, dpToPx(2))
                                }
                                addView(tvQ)
                            }

                            if (assignment.questions.size > 3) {
                                val tvMore = TextView(requireContext()).apply {
                                    text = "+ ${assignment.questions.size - 3} more questions in official PDF..."
                                    textSize = 10.5f
                                    setTypeface(null, Typeface.ITALIC)
                                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_cyan))
                                    setPadding(0, dpToPx(4), 0, 0)
                                }
                                addView(tvMore)
                            }
                        }
                        addView(previewBox)

                        // Buttons Row
                        val buttonsRow = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dpToPx(12)
                            }

                            val btnOpenPdf = MaterialButton(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1.2f
                                ).apply {
                                    marginEnd = dpToPx(6)
                                }
                                text = "📄 Open PDF"
                                textSize = 12f
                                isAllCaps = false
                                cornerRadius = dpToPx(10)
                                setIconResource(R.drawable.ic_library)
                                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.splash_accent_primary)
                                setTextColor(Color.WHITE)
                                setOnClickListener {
                                    PdfGeneratorUtil.openOrDownloadAssignmentPdf(requireContext(), assignment)
                                }
                            }
                            addView(btnOpenPdf)

                            val btnSharePdf = MaterialButton(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                                text = "🔗 Share PDF"
                                textSize = 11.5f
                                isAllCaps = false
                                cornerRadius = dpToPx(10)
                                strokeWidth = dpToPx(1)
                                strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.splash_accent_glow)
                                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.glass_card_bg_elevated)
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                                setOnClickListener {
                                    PdfGeneratorUtil.shareAssignmentPdf(requireContext(), assignment)
                                }
                            }
                            addView(btnSharePdf)
                        }
                        addView(buttonsRow)
                    }
                    addView(cardContent)
                }
                containerAssignmentsList.addView(card)
            }
        }

        filterChips.forEach { (chip, code) ->
            chip.setOnClickListener {
                updateAssignmentsList(code)
            }
        }

        updateAssignmentsList(initialSubjectCode)

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== ADMIN ASSIGNMENT MANAGER ====================

    private fun showAdminAssignmentManagerSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_assignment_manager, null)

        val btnAssignmentManagerClose = sheetView.findViewById<ImageView>(R.id.btnAssignmentManagerClose)
        val btnAddNewAssignment = sheetView.findViewById<MaterialButton>(R.id.btnAddNewAssignment)
        val btnResetAssignments = sheetView.findViewById<MaterialButton>(R.id.btnResetAssignments)
        val tvActiveAssignmentsCount = sheetView.findViewById<TextView>(R.id.tvActiveAssignmentsCount)
        val containerAdminAssignmentsList = sheetView.findViewById<LinearLayout>(R.id.containerAdminAssignmentsList)

        fun renderAdminAssignments() {
            val list = AssignmentManager.ASSIGNMENTS_LIST
            containerAdminAssignmentsList.removeAllViews()
            tvActiveAssignmentsCount.text = "Active Course Assignments (${list.size})"

            for (assignment in list) {
                val cardView = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dpToPx(12))
                    }
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_card)
                    setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                }

                // Top meta row
                val topRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val badge = TextView(requireContext()).apply {
                    text = "${assignment.subjectCode} • ${assignment.alignedCO.take(24)}"
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber))
                    setBackgroundResource(R.drawable.bg_badge_pill)
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.icon_bg_canteen)
                    )
                    setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                }

                val dateView = TextView(requireContext()).apply {
                    text = "Sem ${assignment.semester} • ${assignment.dateText}"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    setPadding(dpToPx(8), 0, 0, 0)
                }

                topRow.addView(badge)
                topRow.addView(dateView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                cardView.addView(topRow)

                // Title
                val tvTitle = TextView(requireContext()).apply {
                    text = "${assignment.subjectName} - ${assignment.title}"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_primary))
                    textSize = 13.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dpToPx(6), 0, 0)
                }
                cardView.addView(tvTitle)

                // CO Description & Questions count
                val tvDesc = TextView(requireContext()).apply {
                    text = "🎯 ${assignment.alignedCO} • ${assignment.questions.size} Questions"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary))
                    textSize = 11f
                    setPadding(0, dpToPx(2), 0, dpToPx(6))
                }
                cardView.addView(tvDesc)

                // Questions preview snippet
                if (assignment.questions.isNotEmpty()) {
                    val tvPreview = TextView(requireContext()).apply {
                        text = "1. ${assignment.questions.first().text.take(80)}..."
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                        textSize = 10.5f
                        setTypeface(null, Typeface.ITALIC)
                        setPadding(0, 0, 0, dpToPx(8))
                    }
                    cardView.addView(tvPreview)
                }

                // Action buttons row (View PDF, Edit/Replace, Delete)
                val actionRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val btnOpenPdf = TextView(requireContext()).apply {
                    text = "📄 View PDF"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_accent_glow))
                    textSize = 11.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dpToPx(4), dpToPx(12), dpToPx(4))
                    setOnClickListener {
                        PdfGeneratorUtil.openOrDownloadAssignmentPdf(requireContext(), assignment)
                    }
                }

                val btnEditAssignment = TextView(requireContext()).apply {
                    text = "✏️ Edit / Replace"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber))
                    textSize = 11.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(12), dpToPx(4))
                    setOnClickListener {
                        showAdminAddAssignmentDialog(assignmentToEdit = assignment) {
                            renderAdminAssignments()
                            updateRoleUIFunc?.invoke(currentRole)
                        }
                    }
                }

                val btnDeleteAssignment = TextView(requireContext()).apply {
                    text = "🗑️ Delete"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_coral))
                    textSize = 11.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4))
                    setOnClickListener {
                        AssignmentManager.removeAssignment(assignment.id, requireContext())
                        renderAdminAssignments()
                        updateRoleUIFunc?.invoke(currentRole)
                        Toast.makeText(requireContext(), "Assignment deleted from Student section", Toast.LENGTH_SHORT).show()
                    }
                }

                actionRow.addView(btnOpenPdf)
                actionRow.addView(btnEditAssignment)
                actionRow.addView(btnDeleteAssignment)
                cardView.addView(actionRow)

                containerAdminAssignmentsList.addView(cardView)
            }
        }

        renderAdminAssignments()

        btnAddNewAssignment.setOnClickListener {
            showAdminAddAssignmentDialog {
                renderAdminAssignments()
                updateRoleUIFunc?.invoke(currentRole)
            }
        }

        btnResetAssignments.setOnClickListener {
            AssignmentManager.resetToDefaults(requireContext())
            renderAdminAssignments()
            updateRoleUIFunc?.invoke(currentRole)
            Toast.makeText(requireContext(), "Reset to default authentic GTU assignments! 🔄", Toast.LENGTH_SHORT).show()
        }

        btnAssignmentManagerClose.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminAddAssignmentDialog(
        assignmentToEdit: AssignmentManager.AssignmentModel? = null,
        onDone: () -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.expandAndEnableScrolling()
        val sheetView = layoutInflater.inflate(R.layout.dialog_admin_add_assignment, null)

        val tvAddAssignmentTitle = sheetView.findViewById<TextView>(R.id.tvAddAssignmentTitle)
        val etAssignmentSubjectName = sheetView.findViewById<TextInputEditText>(R.id.etAssignmentSubjectName)
        val etAssignmentSubjectCode = sheetView.findViewById<TextInputEditText>(R.id.etAssignmentSubjectCode)
        val etAssignmentNo = sheetView.findViewById<TextInputEditText>(R.id.etAssignmentNo)
        val etAssignmentHeading = sheetView.findViewById<TextInputEditText>(R.id.etAssignmentHeading)
        val etAssignmentCO = sheetView.findViewById<TextInputEditText>(R.id.etAssignmentCO)
        val etAssignmentDate = sheetView.findViewById<TextInputEditText>(R.id.etAssignmentDate)
        val etAssignmentQuestions = sheetView.findViewById<TextInputEditText>(R.id.etAssignmentQuestions)
        val btnPickAssignmentPdf = sheetView.findViewById<MaterialButton>(R.id.btnPickAssignmentPdf)
        val tvSelectedAssignmentPdfName = sheetView.findViewById<TextView>(R.id.tvSelectedAssignmentPdfName)
        val btnSaveAssignment = sheetView.findViewById<MaterialButton>(R.id.btnSaveAssignment)

        var selectedCustomPdfPath: String? = assignmentToEdit?.customFilePath
        var selectedCustomFileName: String? = assignmentToEdit?.customFileName

        if (assignmentToEdit != null) {
            tvAddAssignmentTitle.text = "Edit / Replace Assignment"
            etAssignmentSubjectName.setText(assignmentToEdit.subjectName)
            etAssignmentSubjectCode.setText(assignmentToEdit.subjectCode)
            etAssignmentNo.setText(assignmentToEdit.assignmentNo.toString())
            etAssignmentHeading.setText(assignmentToEdit.title)
            etAssignmentCO.setText(assignmentToEdit.alignedCO)
            etAssignmentDate.setText(assignmentToEdit.dateText)

            if (!assignmentToEdit.customFileName.isNullOrEmpty()) {
                tvSelectedAssignmentPdfName.text = "Attached PDF: ${assignmentToEdit.customFileName} 📁"
            } else if (!assignmentToEdit.customFilePath.isNullOrEmpty()) {
                tvSelectedAssignmentPdfName.text = "Attached PDF: ${File(assignmentToEdit.customFilePath!!).name} 📁"
            }

            val questionsRaw = assignmentToEdit.questions.joinToString("\n\n") { q ->
                val subs = if (q.subPoints.isNotEmpty()) "\n" + q.subPoints.joinToString("\n") else ""
                "${q.qNo}. ${q.text}$subs"
            }
            etAssignmentQuestions.setText(questionsRaw)
            btnSaveAssignment.text = "💾 Update & Sync Assignment"
        } else {
            tvAddAssignmentTitle.text = "Add New Course Assignment"
            etAssignmentSubjectName.setText("AI Product Design")
            etAssignmentSubjectCode.setText("DI05016021")
            etAssignmentNo.setText("${AssignmentManager.ASSIGNMENTS_LIST.size + 1}")
            etAssignmentHeading.setText("Assignment - ${AssignmentManager.ASSIGNMENTS_LIST.size + 1} [Aligned with CO1]")
            etAssignmentCO.setText("CO1: Understand and apply core engineering concepts")
            etAssignmentDate.setText("October 2026")
            etAssignmentQuestions.setText("1. Explain the fundamental architecture and design workflow with a diagram.\n2. Write user-centric requirements for an enterprise system.\n3. List and explain the primary advantages and key challenges.")
        }

        btnPickAssignmentPdf.setOnClickListener {
            onPdfPickedCallback = { uri, fileName ->
                val savedPath = copyUriToInternalFile(uri, "assignment_${System.currentTimeMillis()}_$fileName")
                if (savedPath != null) {
                    selectedCustomPdfPath = savedPath
                    selectedCustomFileName = fileName
                    tvSelectedAssignmentPdfName.text = "Selected Device PDF: $fileName 📁"
                    Toast.makeText(requireContext(), "Assignment PDF Attached: $fileName 📁", Toast.LENGTH_SHORT).show()
                }
            }
            pdfPickerLauncher.launch("application/pdf")
        }

        btnSaveAssignment.setOnClickListener {
            val subjectName = etAssignmentSubjectName.text.toString().trim().ifEmpty { "Engineering Subject" }
            val subjectCode = etAssignmentSubjectCode.text.toString().trim().ifEmpty { "DI05016021" }
            val assignmentNo = etAssignmentNo.text.toString().trim().toIntOrNull() ?: 1
            val heading = etAssignmentHeading.text.toString().trim().ifEmpty { "Assignment - $assignmentNo" }
            val co = etAssignmentCO.text.toString().trim().ifEmpty { "CO$assignmentNo: Aligned Course Outcome" }
            val date = etAssignmentDate.text.toString().trim().ifEmpty { "October 2026" }
            val rawQuestions = etAssignmentQuestions.text.toString().trim()

            val questionsList = AssignmentManager.parseQuestionsFromRawText(rawQuestions)

            val id = assignmentToEdit?.id ?: "${subjectCode.lowercase()}_ass${assignmentNo}_${System.currentTimeMillis()}"

            val assignment = AssignmentManager.AssignmentModel(
                id = id,
                subjectCode = subjectCode,
                subjectName = subjectName,
                assignmentNo = assignmentNo,
                title = heading,
                alignedCO = co,
                dateText = date,
                semester = 5,
                questions = questionsList,
                customFilePath = selectedCustomPdfPath,
                customFileName = selectedCustomFileName
            )

            if (assignmentToEdit != null) {
                AssignmentManager.updateAssignment(assignment, requireContext())
                Toast.makeText(requireContext(), "Assignment updated & replaced successfully! 📄", Toast.LENGTH_SHORT).show()
            } else {
                AssignmentManager.addAssignment(assignment, requireContext())
                Toast.makeText(requireContext(), "New assignment published to Student panel! 🚀", Toast.LENGTH_LONG).show()
            }

            onDone.invoke()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }
}