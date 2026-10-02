package com.devang.campushelper

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private lateinit var prefHelper: PreferenceHelper
    private var currentRole: UserRole = UserRole.STUDENT
    private var activeNavTab: Int = 0

    enum class UserRole {
        STUDENT, ADMIN
    }

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

        // 3. Search Bar Elements
        val etCampusSearch = view.findViewById<EditText>(R.id.etCampusSearch)
        val btnSearchClear = view.findViewById<ImageView>(R.id.btnSearchClear)
        val btnSearchFilter = view.findViewById<ImageView>(R.id.btnSearchFilter)

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

        val studentModules = listOf(
            ModuleConfig(
                title = "Notices & Circulars",
                subtitle = "Official circulars & exam schedules",
                tag = "3 New",
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
                subtitle = "Schedules & master timetable",
                tag = "Odd 2026-27",
                iconRes = R.drawable.ic_timetable,
                accentColorRes = R.color.splash_accent_primary,
                tagColorRes = R.color.splash_accent_glow,
                description = "Government Polytechnic Rajkot Information Technology Department Master Time Table (Term Odd 2026-27) for Semesters 5-A, 5-B, and 5-C.",
                highlightHeader = "Master Timetable Highlights",
                highlightBody = "• Sem 5-A (Room 101): AIPD, CDCT, AIWPE, SPC Labs in APL-1/APL-2\n• Sem 5-B (Room 102): CDCT, AIWPE, SPC, Min Project\n• Sem 5-C (Room 103): AIWPE, CDCT, AIPD Labs, EIDW (T)\n• Teaching Slots: 11:00 AM - 06:00 PM (Monday to Saturday)",
                btnText = "📄 Open & Download Time Table PDF",
                customAction = { showTimeTableSheet() }
            ),
            ModuleConfig(
                title = "Canteen Hub",
                subtitle = "Live menu & token booking",
                tag = "🟢 Open",
                iconRes = R.drawable.ic_canteen,
                accentColorRes = R.color.accent_amber,
                tagColorRes = R.color.accent_amber_light,
                description = "Check real-time counter rush, browse today's fresh menu, and pre-book food tokens without standing in queues.",
                highlightHeader = "Today's Fresh Specials",
                highlightBody = "• Paneer Butter Masala Thali — ₹80\n• Samosa & Masala Chai Combo — ₹30\n• Cold Coffee & Sandwich — ₹50\n• Estimated Wait: 4-6 minutes",
                btnText = "🎟️ Pre-Order Food Token"
            ),
            ModuleConfig(
                title = "GTU Syllabus",
                subtitle = "5 IT Subjects • PDF Download",
                tag = "2026-27",
                iconRes = R.drawable.ic_library,
                accentColorRes = R.color.splash_accent_primary,
                tagColorRes = R.color.splash_accent_glow,
                description = "Gujarat Technological University (GTU) Diploma IT Semester 5 official curriculum. View detailed unit topics, exam schemes, practical lists, and download authentic PDFs.",
                highlightHeader = "Semester 5 IT Curriculum",
                highlightBody = "• DI05016011: AI with Prompt Engineering (4 CR)\n• DI05016021: AI Product Design (4 CR)\n• DI05016031: Cloud & Data Center Tech (4 CR)\n• DI05016061: Structured Programming with C (4 CR)\n• DI05016081: Emotional Intelligence & Wellbeing (3 CR)",
                btnText = "📚 View & Download Syllabus PDFs",
                customAction = { showSyllabusDirectorySheet() }
            ),
            ModuleConfig(
                title = "Lost & Found",
                subtitle = "Track & claim verified items",
                tag = "4 Active",
                iconRes = R.drawable.ic_lost_found,
                accentColorRes = R.color.accent_rose,
                tagColorRes = R.color.accent_rose_light,
                description = "Browse recently recovered articles verified by campus security.",
                highlightHeader = "Recently Reported Items",
                highlightBody = "• [FOUND] Boat Airdopes Case (Library 2nd Floor)\n• [FOUND] Fastrack Black Watch (Canteen Area)\n• [LOST] Blue Spiral Notebook (Lab 302)\n• [FOUND] GP Rajkot Student ID Card",
                btnText = ""
            ),
            ModuleConfig(
                title = "Helpdesk & Grievance",
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
                title = "Campus Alerts",
                subtitle = "Emergency alerts & push updates",
                tag = "Instant",
                iconRes = R.drawable.ic_bell,
                accentColorRes = R.color.accent_coral,
                tagColorRes = R.color.accent_rose_light,
                description = "High-priority push broadcast system for emergency campus notices, GTU circulars, weather alerts, and fests.",
                highlightHeader = "Recent Broadcasts",
                highlightBody = "• Urgent: Heavy rain alert issued by GTU for tomorrow\n• Reminder: Submit project synopsis before Friday 4 PM\n• Seminar on Cloud Computing at 2 PM Auditorium",
                btnText = ""
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
                highlightBody = "• Name: $savedName\n• Department: ${prefHelper.department}\n• Enrollment: ${prefHelper.enrollmentNo.ifEmpty { "Verified Student" }}\n• Phone: ${prefHelper.userPhone.ifEmpty { "Linked" }}",
                btnText = ""
            )
        )

        val adminModules = listOf(
            ModuleConfig(
                title = "Campus Broadcast",
                subtitle = "Instant emergency notification",
                tag = "HIGH PRIORITY",
                iconRes = R.drawable.ic_bell,
                accentColorRes = R.color.accent_coral,
                tagColorRes = R.color.accent_rose_light,
                description = "Send high-priority campus-wide push notifications to all enrolled students and faculty instantly.",
                highlightHeader = "Broadcast System Status",
                highlightBody = "• Active Reach: 116 Students + 24 Faculty Members\n• Delivery Speed: < 2 seconds via Firebase Cloud Messaging\n• Categories: Weather, Exam, TechFest, Holiday",
                btnText = "🚨 Dispatch Emergency Alert",
                customAction = { showAdminBroadcastSheet() }
            ),
            ModuleConfig(
                title = "Canteen Control",
                subtitle = "Toggle rush & update menu",
                tag = "🟢 Online",
                iconRes = R.drawable.ic_canteen,
                accentColorRes = R.color.accent_amber,
                tagColorRes = R.color.accent_amber_light,
                description = "Master management panel for campus food court: open/close counters, edit prices, and monitor token queues.",
                highlightHeader = "Live Canteen Operations",
                highlightBody = "• Main Counter Status: [OPEN]\n• Orders Completed Today: 142 Tokens\n• Current Wait Time: 4 minutes (Low Rush)\n• Pre-order Token System: [Active]",
                btnText = "🍽️ Open Canteen Master Control",
                customAction = { showAdminCanteenControlSheet() }
            ),
            ModuleConfig(
                title = "Verify Roster",
                subtitle = "GTU Institute 620 database",
                tag = "116 Students",
                iconRes = R.drawable.ic_check,
                accentColorRes = R.color.accent_cyan,
                tagColorRes = R.color.accent_cyan_light,
                description = "Master roster inspector for Government Polytechnic, Rajkot. Verify enrollment claims and prevent duplicates.",
                highlightHeader = "Database Synchronization",
                highlightBody = "• College Code: GTU 620 (GP Rajkot)\n• Total Master Records: 116 Students\n• Claimed Accounts: 100% Verified\n• Storage: Firebase Firestore + In-Memory Cache",
                btnText = "🛡️ Inspect Master Roster",
                customAction = { showAdminRosterInspectionSheet() }
            ),
            ModuleConfig(
                title = "Grievance Desk",
                subtitle = "Review & resolve complaints",
                tag = "2 Pending",
                iconRes = R.drawable.ic_helpdesk,
                accentColorRes = R.color.accent_sky,
                tagColorRes = R.color.accent_sky_light,
                description = "Central campus grievance desk. Review student tickets, assign department officers, and log resolutions.",
                highlightHeader = "Ticket Queue",
                highlightBody = "• #1092: Hostel Water Supply — [Under Review ⚙️]\n• #1094: Lab 3 Wi-Fi Speed — [Assigned to IT Admin]\n• #1085: Fee receipt query — [Resolved ✅]\n• 94% 24-hr resolution rate",
                btnText = "⚖️ Resolve Campus Grievances"
            ),
            ModuleConfig(
                title = "Lost & Found Log",
                subtitle = "Approve verified item claims",
                tag = "4 Items",
                iconRes = R.drawable.ic_lost_found,
                accentColorRes = R.color.accent_rose,
                tagColorRes = R.color.accent_rose_light,
                description = "Security cabin master log for found valuables, ID cards, and electronics. Approve handover to students.",
                highlightHeader = "Security Cabin Inventory",
                highlightBody = "• Item #41: Boat Airdopes Case (Verified at Main Gate)\n• Item #42: Fastrack Black Watch (Awaiting Owner)\n• Handover protocol requires OTP & Student ID verify",
                btnText = "📦 Review Security Deposit Log"
            ),
            ModuleConfig(
                title = "System Health",
                subtitle = "Firestore sync & audit logs",
                tag = "Optimal",
                iconRes = R.drawable.ic_admin,
                accentColorRes = R.color.accent_teal,
                tagColorRes = R.color.accent_teal_light,
                description = "Monitor real-time system performance, Firebase Authentication latency, and institutional security access logs.",
                highlightHeader = "Cloud Metrics",
                highlightBody = "• Firebase Auth Status: Operational (100% uptime)\n• Firestore Read/Write latency: 42ms\n• Security Rules: Institutional domain lock active\n• Session Token Refresh: Normal",
                btnText = "📊 View System Audit Logs"
            ),
            ModuleConfig(
                title = "Campus Security",
                subtitle = "Gate passes & visitor log",
                tag = "Monitored",
                iconRes = R.drawable.ic_lock,
                accentColorRes = R.color.accent_emerald,
                tagColorRes = R.color.accent_emerald_light,
                description = "Campus gate access monitoring, vehicle pass verification, and visitor entry logs.",
                highlightHeader = "Gate Operations",
                highlightBody = "• Main Gate: RFID & Student Card Scanner Active\n• Visitors logged today: 18\n• Campus CCTV surveillance: 32 cameras online",
                btnText = "🛡️ Open Security Protocols"
            ),
            ModuleConfig(
                title = "Institution Config",
                subtitle = "Academic year & term dates",
                tag = "2026-27",
                iconRes = R.drawable.ic_timetable,
                accentColorRes = R.color.splash_accent_primary,
                tagColorRes = R.color.splash_accent_glow,
                description = "Configure institutional terms, holiday schedules, branch quotas, and GTU semester dates.",
                highlightHeader = "Academic Setup",
                highlightBody = "• Current Term: Odd Semester 2026\n• Total Active Branches: 10 Engineering Disciplines\n• Next Term Enrollment Period: Dec 15-22, 2026",
                btnText = "⚙️ Edit Academic Settings"
            )
        )

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
                    tvGreeting.text = "Welcome, $savedName 👋"
                    val userDept = prefHelper.department
                    val userEnroll = if (prefHelper.enrollmentNo.isNotEmpty()) " • ${prefHelper.enrollmentNo}" else ""
                    tvDepartment.text = "$userDept$userEnroll"
                    tvRoleBadgeHeader.text = "Student Portal"

                    // Hero Live Pulse
                    tvHeroBadge.text = "📡 LIVE PULSE"
                    tvHeroCountdown.text = "Starts in 18 mins"
                    tvHeroSubject.text = "Data Structures & Algorithms"
                    tvHeroSchedule.text = "⏰ 11:00 AM - 12:00 PM • Lab 302 (Prof. Mehta)"
                    chipCanteenStatus.text = "🟢 Canteen: Open (Low Wait)"
                    chipLibraryStatus.text = "⚡ Lab 302: Active"
                    chipNoticeStatus.text = "📢 3 New Circulars"

                    tvSectionTitle.text = "Student Campus Services"
                    studentModules
                }
                UserRole.ADMIN -> {
                    tvGreeting.text = "System Administrator 🛡️"
                    tvDepartment.text = "Government Polytechnic, Rajkot Central Admin"
                    tvRoleBadgeHeader.text = "Admin Portal"

                    // Hero Live Pulse
                    tvHeroBadge.text = "🛡️ CENTRAL HUB"
                    tvHeroCountdown.text = "System Healthy"
                    tvHeroSubject.text = "Campus Operations & Security Control"
                    tvHeroSchedule.text = "🏛️ 116 Enrolled Students • 10 Active Engineering Branches"
                    chipCanteenStatus.text = "📢 Broadcast Active"
                    chipLibraryStatus.text = "🟢 Canteen Online"
                    chipNoticeStatus.text = "🛡️ Roster 100% Verified"

                    tvSectionTitle.text = "Institutional Administration"
                    adminModules
                }
            }

            // Bind each card to the role-specific config
            for (i in 0 until minOf(cards.size, currentModules.size)) {
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
            }
        }

        btnRoleStudent.setOnClickListener {
            updateRoleUI(UserRole.STUDENT)
            Toast.makeText(requireContext(), "Switched to Student Portal 🎓", Toast.LENGTH_SHORT).show()
        }
        btnRoleAdmin.setOnClickListener {
            updateRoleUI(UserRole.ADMIN)
            Toast.makeText(requireContext(), "Switched to Central Admin Control 🛡️", Toast.LENGTH_SHORT).show()
        }

        // Initialize with Student Role
        updateRoleUI(UserRole.STUDENT)

        // ==================== COMPLETE WORKING SEARCH BAR ====================
        val layoutInlineSearchResults = view.findViewById<LinearLayout>(R.id.layoutInlineSearchResults)
        val tvInlineSearchCount = view.findViewById<TextView>(R.id.tvInlineSearchCount)
        val btnViewFullSearchResults = view.findViewById<TextView>(R.id.btnViewFullSearchResults)
        val layoutInlineResultsList = view.findViewById<LinearLayout>(R.id.layoutInlineResultsList)

        fun updateInlineSearchResults(query: String) {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) {
                layoutInlineSearchResults.visibility = View.GONE
                layoutInlineResultsList.removeAllViews()
                return
            }

            val results = CampusSearchManager.search(cleanQuery)
            if (results.isEmpty()) {
                layoutInlineSearchResults.visibility = View.VISIBLE
                tvInlineSearchCount.text = "No matches for '$cleanQuery'"
                layoutInlineResultsList.removeAllViews()

                val emptyView = TextView(requireContext()).apply {
                    text = "Try searching: 'exam', 'canteen', 'library', 'lab', 'hostel'"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    textSize = 12f
                    setPadding(8, 8, 8, 8)
                }
                layoutInlineResultsList.addView(emptyView)
            } else {
                layoutInlineSearchResults.visibility = View.VISIBLE
                tvInlineSearchCount.text = "${results.size} match${if (results.size > 1) "es" else ""} found"
                layoutInlineResultsList.removeAllViews()

                // Show top 4 items directly inline
                val displayResults = results.take(4)
                for (item in displayResults) {
                    val itemView = layoutInflater.inflate(R.layout.item_search_result, layoutInlineResultsList, false)
                    val ivSearchIcon = itemView.findViewById<ImageView>(R.id.ivSearchIcon)
                    val tvSearchItemTitle = itemView.findViewById<TextView>(R.id.tvSearchItemTitle)
                    val tvSearchItemSubtitle = itemView.findViewById<TextView>(R.id.tvSearchItemSubtitle)
                    val tvSearchItemTag = itemView.findViewById<TextView>(R.id.tvSearchItemTag)

                    tvSearchItemTitle.text = item.title
                    tvSearchItemSubtitle.text = item.subtitle
                    tvSearchItemTag.text = item.tag

                    val accent = ContextCompat.getColor(requireContext(), item.accentColorRes)
                    ivSearchIcon.setImageResource(item.iconRes)
                    ivSearchIcon.imageTintList = ColorStateList.valueOf(accent)
                    tvSearchItemTag.setTextColor(accent)

                    itemView.setOnClickListener {
                        if (item.category == CampusSearchManager.Category.SYLLABUS) {
                            val subj = CampusSearchManager.getSyllabusByCode(item.tag)
                            if (subj != null) {
                                showSubjectSyllabusDetailSheet(subj)
                                return@setOnClickListener
                            }
                        }
                        showModuleBottomSheet(
                            title = item.title,
                            subtitle = item.subtitle,
                            tag = item.tag,
                            iconRes = item.iconRes,
                            accentColorRes = item.accentColorRes,
                            description = item.detailsHeader,
                            highlightHeader = "Detailed Information",
                            highlightBody = item.detailsBody,
                            btnText = item.actionText
                        )
                    }
                    layoutInlineResultsList.addView(itemView)
                }
            }
        }

        etCampusSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnSearchClear.visibility = if (query.trim().isNotEmpty()) View.VISIBLE else View.GONE
                updateInlineSearchResults(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSearchClear.setOnClickListener {
            etCampusSearch.setText("")
            btnSearchClear.visibility = View.GONE
            layoutInlineSearchResults.visibility = View.GONE
            layoutInlineResultsList.removeAllViews()
        }

        btnViewFullSearchResults.setOnClickListener {
            val query = etCampusSearch.text.toString().trim()
            openCampusSearchBottomSheet(query)
        }

        etCampusSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = etCampusSearch.text.toString().trim()
                openCampusSearchBottomSheet(query)
                true
            } else {
                false
            }
        }

        // Search filter button opens category-oriented search bottom sheet
        btnSearchFilter.setOnClickListener {
            val query = etCampusSearch.text.toString().trim()
            openCampusSearchBottomSheet(query)
        }

        // Tapping the search bar container opens search sheet
        view.findViewById<View>(R.id.searchBar).setOnClickListener {
            val query = etCampusSearch.text.toString().trim()
            openCampusSearchBottomSheet(query)
        }

        // Hero Card Click
        heroCard.setOnClickListener {
            when (currentRole) {
                UserRole.STUDENT -> cards[1].performClick()
                UserRole.ADMIN -> showAdminBroadcastSheet()
            }
        }

        chipCanteenStatus.setOnClickListener { cards[2].performClick() }
        chipLibraryStatus.setOnClickListener { cards[3].performClick() }
        chipNoticeStatus.setOnClickListener { cards[0].performClick() }

        // Urgent Circular Highlights - Individual PDF Openers
        btnViewAllNotices.setOnClickListener { showNoticesAndCircularsSheet() }
        cardCircular1.setOnClickListener {
            NoticeDownloadUtil.openCircularPdf(requireContext(), "Sem5 - Mid Sem Result.pdf", "Semester 5 Mid-Sem Exam Results")
        }
        cardCircular2.setOnClickListener {
            NoticeDownloadUtil.openCircularPdf(requireContext(), "GTU_National_Seminar.pdf", "GTU National Seminar: AI & Quantum Frontiers")
        }
        cardCircular3?.setOnClickListener {
            NoticeDownloadUtil.openCircularPdf(requireContext(), "20260917191103-92cb7308c6.pdf", "GTU Remedial Exam Forms Circular (Winter 2026)")
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
                        if (item.category == CampusSearchManager.Category.SYLLABUS) {
                            val subj = CampusSearchManager.getSyllabusByCode(item.tag)
                            if (subj != null) {
                                showSubjectSyllabusDetailSheet(subj)
                                return@setOnClickListener
                            }
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
                    NoticeDownloadUtil.openCircularPdf(requireContext(), notice.fileName, notice.title)
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
        sheetPrimaryBtn.text = "📄 Open & Download Syllabus PDF"

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
        sheetIcon.setImageResource(R.drawable.ic_timetable)
        sheetIcon.imageTintList = ColorStateList.valueOf(accentColor)
        sheetTag.setTextColor(accentColor)
        sheetPrimaryBtn.backgroundTintList = ColorStateList.valueOf(accentColor)

        sheetTitle.text = "GP Rajkot IT Master Time Table"
        sheetSubtitle.text = "Term Odd 2026-27 • Sem 5-A, 5-B, 5-C • WEF: 29/06/2026"
        sheetTag.text = "30 HRS/WK"

        sheetDescription.text = "Government Polytechnic Rajkot Information Technology Department Official Master Time Table for Semester 5. Includes classroom, lab allocations, and faculty assignments."

        sheetHighlightHeader.text = "Division & Faculty Allocation Schedule"

        val scheduleText = buildString {
            append("📅 WEEKLY SCHEDULE SUMMARY (11:00 AM - 06:00 PM):\n\n")
            append("🔹 MONDAY:\n")
            append("  • Sem 5-A (Rm 101): AIPD (DMT), CDCT (SRT), AIWPE Lab (APL-2)\n")
            append("  • Sem 5-B (Rm 102): CDCT (SVN), AIWPE (HKV), SPC (MTV/HKV), Min Project\n")
            append("  • Sem 5-C (Rm 103): AIWPE (HKV), CDCT (SVN), AIPD Lab (APL-2), EIDW (T)\n\n")

            append("🔹 TUESDAY:\n")
            append("  • Sem 5-A (Rm 101): SPC Lab (APL-1), SPC (MTV/SBP), Min Project\n")
            append("  • Sem 5-B (Rm 102): AIPD Lab (APL-2), CDCT Lab (APL-2), EIDW (BPL-1)\n")
            append("  • Sem 5-C (Rm 103): AIWPE (SJS), SPC (SBP), AIPD (GJB)\n\n")

            append("🔹 WEDNESDAY:\n")
            append("  • Sem 5-A (Rm 101): AIPD Lab (APL-1), AIWPE (SJS), Min Project (107)\n")
            append("  • Sem 5-B (Rm 102): CDCT (SRT), SPC Lab (APL-2), EIDW Lab (BPL-1)\n")
            append("  • Sem 5-C (Rm 103): AIWPE Lab (BPL-1), AIPD (DMT), EIDW Lab\n\n")

            append("🔹 THURSDAY:\n")
            append("  • Sem 5-A (Rm 101): SPC (HKV), AIWPE (HKV), CDCT (SVN), AIPD (GJB)\n")
            append("  • Sem 5-B (Rm 102): AIPD (GJB), SPC (SBP), AIWPE (SJS), Min Project\n")
            append("  • Sem 5-C (Rm 103): CDCT Lab (APL-2), SPC Lab (APL-1), Min Project\n\n")

            append("🔹 FRIDAY:\n")
            append("  • Sem 5-A (Rm 101): EIDW Lab (APL-1), CDCT Lab (APL-2)\n")
            append("  • Sem 5-B (Rm 102): AIPD (DMT), AIWPE Lab (BPL-1), EIDW (T)\n")
            append("  • Sem 5-C (Rm 103): CDCT (SRT), SPC (MTV/HKV), Min Project (BPL-1)\n\n")

            append("🔹 SATURDAY:\n")
            append("  • Sem 5-A: Min Project (APL-2) | Sem 5-B: Min Project (APL-1) | Sem 5-C: Min Project (BPL-1)\n\n")

            append("🏛️ LAB LOCATIONS:\n")
            append("  • APL-1 & APL-2: Advanced Programming Lab\n")
            append("  • BPL-1: Basic Programming Lab\n\n")

            append("👨‍🏫 FACULTY IN CHARGE:\n")
            append("  • DMT: Prof. D. M. Tank | SVN: Prof. S. V. Nimavat\n")
            append("  • HKV: Prof. H. K. Vora | SRT: Prof. S. R. Tank\n")
            append("  • MTV: Prof. M. T. Vaghasia | SBP: Prof. S. B. Parmar\n")
            append("  • GJB: Prof. G. J. Bhensdadia | SJS: Prof. S. J. Sangani | AOB: Prof. A. O. Bhatt")
        }

        sheetHighlightBody.text = scheduleText
        sheetPrimaryBtn.text = "📄 Open & Download Time Table PDF"

        sheetBtnClose.setOnClickListener { bottomSheet.dismiss() }
        sheetPrimaryBtn.setOnClickListener {
            PdfGeneratorUtil.openOrDownloadTimeTablePdf(requireContext())
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    // ==================== ADMIN TOOLS ====================

    private fun showAdminBroadcastSheet() {
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

        val accentColor = ContextCompat.getColor(requireContext(), R.color.accent_coral)
        sheetIcon.setImageResource(R.drawable.ic_bell)
        sheetIcon.imageTintList = ColorStateList.valueOf(accentColor)
        sheetTag.setTextColor(accentColor)
        sheetPrimaryBtn.backgroundTintList = ColorStateList.valueOf(accentColor)

        sheetTitle.text = "Emergency Alert Broadcast"
        sheetSubtitle.text = "High-Priority Campus Push Notification System"
        sheetTag.text = "ADMIN ONLY"
        sheetDescription.text = "Dispatches a high-priority push banner to all registered smartphones across students and faculty in < 2 seconds."
        sheetHighlightHeader.text = "Active Broadcast Parameters"
        sheetHighlightBody.text = "• Target Devices: 140+ registered smartphones\n• Sound & Vibration: High Priority Alert\n• Trigger: GTU Circulars, Weather Warnings, Campus Security"
        sheetPrimaryBtn.text = "🚨 Dispatch Emergency Push Banner"

        sheetBtnClose.setOnClickListener { bottomSheet.dismiss() }
        sheetPrimaryBtn.setOnClickListener {
            Toast.makeText(requireContext(), "Broadcast alert transmitted to all campus devices! 🚨", Toast.LENGTH_LONG).show()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminCanteenControlSheet() {
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

        val accentColor = ContextCompat.getColor(requireContext(), R.color.accent_amber)
        sheetIcon.setImageResource(R.drawable.ic_canteen)
        sheetIcon.imageTintList = ColorStateList.valueOf(accentColor)
        sheetTag.setTextColor(accentColor)
        sheetPrimaryBtn.backgroundTintList = ColorStateList.valueOf(accentColor)

        sheetTitle.text = "Canteen Master Control"
        sheetSubtitle.text = "Food Court Counters & Digital Token Engine"
        sheetTag.text = "🟢 COUNTERS OPEN"
        sheetDescription.text = "Supervise campus food court rush, manage daily items, toggle counter status, and audit daily token transactions."
        sheetHighlightHeader.text = "Counter Management Summary"
        sheetHighlightBody.text = "• Lunch Thali Counter: [Active - 4 min wait]\n• Snacks & Chai Counter: [Active - No wait]\n• Pre-order Token Revenue Today: ₹11,360\n• Next Scheduled Restock: 03:00 PM"
        sheetPrimaryBtn.text = "🔄 Toggle Counter Status / Update Menu"

        sheetBtnClose.setOnClickListener { bottomSheet.dismiss() }
        sheetPrimaryBtn.setOnClickListener {
            Toast.makeText(requireContext(), "Canteen parameters updated successfully! 🍽️", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun showAdminRosterInspectionSheet() {
        showModuleBottomSheet(
            title = "GTU Master Roster Inspector",
            subtitle = "Government Polytechnic, Rajkot • Code 620",
            tag = "VERIFIED",
            iconRes = R.drawable.ic_check,
            accentColorRes = R.color.accent_cyan,
            description = "Central verification authority preventing duplicate enrollments and verifying student department allotments.",
            highlightHeader = "Audit Status",
            highlightBody = "• Total GTU Enrolled Students: 116\n• Anti-Duplicate Verification Engine: [Active ✅]\n• Institutional Domain Restrictions: [gprajkot.ac.in, gtu.ac.in]\n• Unclaimed Entries: 0",
            btnText = "📥 Export Roster Audit (CSV)"
        )
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
}