package com.devang.campushelper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Intelligent Campus AI Engine for CampusHelper (GP Rajkot).
 * Provides hybrid AI responses:
 * 1. Live Google Gemini 1.5 API if configured with full context of Syllabus, Timetable, GTU Circulars, and Course Assignments.
 * 2. High-precision contextual GTU & Campus knowledge engine for instant, accurate offline/online responses in Gujarati & English with actionable PDF attachments.
 */
object CampusAiService {

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val timestamp: String = "Just now"
    )

    data class AiAction(
        val type: String,
        val param: String = "",
        val label: String
    )

    /**
     * Queries the AI assistant with context of the current campus state and conversation history.
     */
    suspend fun queryAi(
        context: Context,
        userPrompt: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val prefHelper = PreferenceHelper(context)
        val apiKey = prefHelper.geminiApiKey.trim()

        if (apiKey.isNotEmpty()) {
            val liveResponse = callGeminiApi(context, apiKey, userPrompt, prefHelper, conversationHistory)
            if (liveResponse != null && liveResponse.isNotBlank()) {
                return@withContext liveResponse
            }
        }

        // Context-aware intelligent engine
        return@withContext generateSmartCampusResponse(context, userPrompt, prefHelper)
    }

    private fun callGeminiApi(
        context: Context,
        apiKey: String,
        prompt: String,
        prefHelper: PreferenceHelper,
        history: List<ChatMessage> = emptyList()
    ): String? {
        return try {
            val noticesList = NoticeDownloadUtil.NOTICES_LIST
            val syllabusList = CampusSearchManager.getSyllabusDatabase()
            val assignmentsList = AssignmentManager.ASSIGNMENTS_LIST

            val noticesSummary = noticesList.joinToString("\n") { notice ->
                "- [${notice.tag}] ${notice.title} (${notice.dateText}): ${notice.description} -> Action: [ACTION:OPEN_CIRCULAR_PDF:${notice.fileName}|📄 Open ${notice.title} (PDF)]"
            }

            val syllabusSummary = syllabusList.joinToString("\n") { sub ->
                val unitsStr = sub.units.joinToString("; ") { "Unit ${it.unitNo}: ${it.title} (${it.weightagePercent}%)" }
                "- ${sub.name} (${sub.code}) | Category: ${sub.category} | ${sub.credits} Credits | Marks: ${sub.totalMarks} (Theory: ${sub.theoryEseMarks}+${sub.theoryPaMarks}, Prac: ${sub.practicalEseMarks}+${sub.practicalPaMarks}) | Units: $unitsStr -> Action: [ACTION:OPEN_SYLLABUS_PDF:${sub.code}|📄 Download ${sub.name} Syllabus PDF]"
            }

            val assignmentsSummary = assignmentsList.joinToString("\n") { ass ->
                val questionsStr = ass.questions.joinToString(" | ") { "${it.qNo}. ${it.text}" }
                "- ${ass.subjectName} (${ass.subjectCode}): ${ass.title} [${ass.alignedCO}] (Date: ${ass.dateText}) Questions: $questionsStr -> Action: [ACTION:OPEN_ASSIGNMENT_PDF:${ass.id}|📄 Open ${ass.subjectName} Assignment ${ass.assignmentNo} PDF]"
            }

            val systemContext = """
You are 'Campus AI', the premier high-level academic & campus copilot for Government Polytechnic Rajkot (GTU Diploma IT Department, Semester 5).

CAMPUS REAL-TIME DATABASE:
1. MASTER TIMETABLE & CLASSROOMS:
- Term Dates: ${prefHelper.timetableTermDates} (WEF: 07/08/2026)
- Allocated Rooms: ${prefHelper.timetableClassrooms} (Sem 5-A in Room 101, Sem 5-B in Room 102, Sem 5-C in Room 103)
- Allocated Labs: ${prefHelper.timetableLabs} (APL-1, APL-2, BPL-1)
- Working Hours: ${prefHelper.timetableWeeklyHours}
- Action for Timetable: [ACTION:OPEN_TIMETABLE_PDF|📄 Open Official Master Timetable PDF]

2. ACTIVE GTU CIRCULARS & NOTICES:
$noticesSummary

3. GTU SEMESTER 5 IT SYLLABUS COURSES:
$syllabusSummary

4. OFFICIAL COURSE ASSIGNMENTS:
$assignmentsSummary

5. LIVE CANTEEN STATUS:
- Status: ${if (prefHelper.canteenIsOpen) "OPEN" else "CLOSED"}
- Chef Special: ${prefHelper.canteenSpecialItem}
- Rush Level: ${prefHelper.canteenRushStatus}
- Timings: 08:30 AM - 06:00 PM (Monday - Saturday)
- Popular: Gujarati Thali (₹80), Chole Bhature (₹60), Samosa + Chai (₹30), Cold Coffee (₹35)

6. FACULTY & CONTACTS:
- HOD IT: Prof. H. Mehta (Ext 301, hod.it@gprajkot.ac.in, Office: 3rd Floor IT Block)
- Security: Ext 100
- Official Portal: https://sites.google.com/view/gprajkot620

ADVANCED CAPABILITIES & INSTRUCTIONS:
1. High-Precision Multilingual: Seamlessly converse in Gujarati, English, or Gujarati-English mix (Gujlish) based on user tone.
2. Exact Academic Solutions: Provide complete, structured, step-by-step code, design diagrams, or conceptual answers for all GTU IT subjects.
3. PDF Action Integration: Always attach actionable direct PDF buttons using `[ACTION:OPEN_ASSIGNMENT_PDF:id|Label]`, `[ACTION:OPEN_SYLLABUS_PDF:code|Label]`, `[ACTION:OPEN_CIRCULAR_PDF:file|Label]`, or `[ACTION:OPEN_TIMETABLE_PDF|Label]`.
4. Tone & Formatting: Clean Markdown with bold highlights, bullets, emojis, and professional code blocks.
""".trimIndent()

            // Try standard models (gemini-3.7-flash, gemini-2.0-flash with fallback hierarchy)
            val modelEndpoints = listOf(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=$apiKey",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=$apiKey"
            )

            for (endpointUrl in modelEndpoints) {
                try {
                    val url = URL(endpointUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 7000
                    conn.readTimeout = 7000

                    val jsonBody = JSONObject().apply {
                        // System Instruction for Gemini 1.5
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", systemContext))
                            })
                        })

                        // Multi-turn conversation contents
                        val contentsArray = JSONArray()
                        
                        // Recent history context (last 6 turns)
                        val recentHistory = history.takeLast(6)
                        for (msg in recentHistory) {
                            val role = if (msg.isUser) "user" else "model"
                            contentsArray.put(JSONObject().apply {
                                put("role", role)
                                put("parts", JSONArray().apply {
                                    put(JSONObject().put("text", msg.text))
                                })
                            })
                        }

                        // Current User Prompt
                        contentsArray.put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })

                        put("contents", contentsArray)

                        // Generation Config for high accuracy & rich formatting
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.7)
                            put("topK", 40)
                            put("topP", 0.95)
                            put("maxOutputTokens", 2048)
                        })
                    }

                    OutputStreamWriter(conn.outputStream).use { writer ->
                        writer.write(jsonBody.toString())
                        writer.flush()
                    }

                    if (conn.responseCode == 200) {
                        val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                        val rootObj = JSONObject(responseText)
                        val candidates = rootObj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val reply = parts.getJSONObject(0).optString("text", "").trim()
                                if (reply.isNotEmpty()) return reply
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateSmartCampusResponse(
        context: Context,
        prompt: String,
        prefHelper: PreferenceHelper
    ): String {
        val q = prompt.lowercase(Locale.ROOT).trim()

        // 1. Assignments Queries
        if (q.contains("assignment") || q.contains("એસાઈનમેન્ટ") || q.contains("એસ્સાઈનમેન્ટ") || q.contains("design thinking") || q.contains("empathy map") || q.contains("virtual influencer") || (q.contains("function") && q.contains("c"))) {
            val assignments = AssignmentManager.ASSIGNMENTS_LIST

            // Specific match for AIPD Assignment 2
            if (q.contains("aipd") && (q.contains("2") || q.contains("two") || q.contains("design thinking") || q.contains("empathy"))) {
                val a = AssignmentManager.getAssignmentById("aipd_ass2")!!
                return "📝 **GTU Assignment: AI Product Design (${a.subjectCode}) - ${a.title}**\n\n" +
                        "• **Course Outcome:** ${a.alignedCO}\n" +
                        "• **Submission Date:** ${a.dateText}\n\n" +
                        "**Key Questions & Solutions Guide:**\n" +
                        "1. **Design Thinking Stages:** Empathize, Define, Ideate, Prototype, Test.\n" +
                        "2. **Empathy Map:** Says, Thinks, Does, Feels (applied to AI Chatbot user personas).\n" +
                        "3. **UX Principles for AI:** Transparency, Predictability, User Control, Feedback, Error Tolerance.\n" +
                        "4. **AI Bias & XAI:** Mitigate training data bias; ensure explainability in Healthcare & Banking.\n\n" +
                        "તમે નીચે આપેલા બટન પર ક્લિક કરીને ઓફિશિયલ એસાઈનમેન્ટ PDF સીધી ઓપન અથવા ડાઉનલોડ કરી શકો છો:\n\n" +
                        "[ACTION:OPEN_ASSIGNMENT_PDF:aipd_ass2|📄 Open AIPD Assignment 2 PDF]"
            }

            // Specific match for AI Prompt Engineering Assignment 5
            if ((q.contains("prompt") || q.contains("aiwpe") || q.contains("api")) && (q.contains("5") || q.contains("five") || q.contains("email") || q.contains("bug") || q.contains("code"))) {
                val a = AssignmentManager.getAssignmentById("aiwpe_ass5")!!
                return "📝 **GTU Assignment: AI with Prompt Engineering (${a.subjectCode}) - ${a.title}**\n\n" +
                        "• **Course Outcome:** ${a.alignedCO}\n" +
                        "• **Key Topics:**\n" +
                        "  - Q1: AI productivity tools for email writing, reports, and presentations.\n" +
                        "  - Q2: AI for software dev (Code generation, explanation, documentation & debugging).\n" +
                        "  - Q3: Step-by-step bug isolation & fixing with LLMs.\n" +
                        "  - Q4: OpenAI API & Google Gemini API integrations.\n" +
                        "  - Q5: Applications: Chatbot, Blog Writer, Doc Summarizer, Question Generator.\n\n" +
                        "[ACTION:OPEN_ASSIGNMENT_PDF:aiwpe_ass5|📄 Open AI Prompt Engg Assignment 5 PDF]"
            }

            // Specific match for Structured Programming with C Assignment 3
            if (q.contains("c") && (q.contains("3") || q.contains("three") || q.contains("function") || q.contains("call") || q.contains("prototype"))) {
                val a = AssignmentManager.getAssignmentById("spc_ass3")!!
                return "📝 **GTU Assignment: Structured Programming with C (${a.subjectCode}) - ${a.title}**\n\n" +
                        "• **Course Outcome:** ${a.alignedCO}\n" +
                        "• **Core Topics:**\n" +
                        "  - Q1: Function definition, modularity & code reusability advantages.\n" +
                        "  - Q2: Elements of user-defined functions: Function Name, Return Type, Parameters, Body.\n" +
                        "  - Q3: Function declaration/prototype syntax & compiler type checking.\n" +
                        "  - Q4: Function call by value & execution stack flow.\n" +
                        "  - Q5: Four categories of functions (Arguments & Return values).\n\n" +
                        "[ACTION:OPEN_ASSIGNMENT_PDF:spc_ass3|📄 Open C Programming Assignment 3 PDF]"
            }

            // Specific match for AI Product Design Assignment 4
            if (q.contains("aipd") || q.contains("influencer") || q.contains("social media") || q.contains("fake news") || q.contains("bot")) {
                val a = AssignmentManager.getAssignmentById("aipd_ass4")!!
                return "📝 **GTU Assignment: AI Product Design (${a.subjectCode}) - ${a.title}**\n\n" +
                        "• **Course Outcome:** ${a.alignedCO}\n" +
                        "• **Key Questions:** Virtual Influencers, Social Media recommender algorithms, E-commerce chatbots, Deepfake mitigation, Bot Networks, and AI UI Prototyping.\n\n" +
                        "[ACTION:OPEN_ASSIGNMENT_PDF:aipd_ass4|📄 Open AIPD Assignment 4 PDF]"
            }

            // General list of all assignments
            val list = assignments.joinToString("\n\n") { ass ->
                "• **${ass.subjectName} (${ass.subjectCode}):** ${ass.title}\n  [ACTION:OPEN_ASSIGNMENT_PDF:${ass.id}|📄 Open ${ass.subjectName.take(24)}... Ass ${ass.assignmentNo} PDF]"
            }

            return "📝 **Government Polytechnic Rajkot - Semester 5 IT Official Assignments**\n\n" +
                    "અહીં સેમેસ્ટર 5 ના તમામ વિષયોના સત્તાવાર એસાઈનમેન્ટ ઉપલબ્ધ છે. જે એસાઈનમેન્ટની PDF જોઈતી હોય તેના બટન પર ક્લિક કરો:\n\n" +
                    list
        }

        // 2. Timetable & Schedule Queries
        if (q.contains("timetable") || q.contains("time table") || q.contains("schedule") || q.contains("class") || q.contains("room") || q.contains("લેક્ચર") || q.contains("ટાઇમટેબલ") || q.contains("ટાઈમટેબલ") || q.contains("lecture")) {
            return "🗓️ **GP Rajkot IT Department Master Timetable (Term Odd 2026-27)**\n\n" +
                    "• **Academic Term:** ${prefHelper.timetableTermDates} (WEF: 07/08/2026)\n" +
                    "• **Classrooms:** ${prefHelper.timetableClassrooms}\n" +
                    "  - Sem 5-A: Room 101 | Sem 5-B: Room 102 | Sem 5-C: Room 103\n" +
                    "• **Laboratories:** ${prefHelper.timetableLabs}\n" +
                    "• **Weekly Workload:** ${prefHelper.timetableWeeklyHours}\n\n" +
                    "તમે નીચે આપેલા બટન પર ક્લિક કરીને ઓફિશિયલ સાઇન્ડ માસ્ટર ટાઇમટેબલ PDF સીધી ઓપન અથવા ડાઉનલોડ કરી શકો છો:\n\n" +
                    "[ACTION:OPEN_TIMETABLE_PDF|📄 Open Master Timetable PDF]"
        }

        // 3. Mid-Sem Results & Marks
        if (q.contains("result") || q.contains("mark") || q.contains("mid sem") || q.contains("mid-sem") || q.contains("રીઝલ્ટ") || q.contains("માર્ક્સ") || q.contains("પરિણામ")) {
            return "📊 **GTU Semester 5 Mid-Sem Exam Results / Marksheet**\n\n" +
                    "• **Department:** Information Technology (Sem 5-A, 5-B, 5-C)\n" +
                    "• **Status:** Official verified marksheet uploaded for all subjects.\n" +
                    "• **Evaluation:** Theory PA (30 Marks) + Practical evaluation included.\n\n" +
                    "તમારું અને તમારા ક્લાસનું ઓફિશિયલ પરિણામ જોવા માટે નીચેનું PDF બટન દબાવો:\n\n" +
                    "[ACTION:OPEN_CIRCULAR_PDF:Sem5 - Mid Sem Result.pdf|📄 Open Sem 5 Mid-Sem Results PDF]"
        }

        // 4. Notices & GTU Circulars (General or Specific)
        if (q.contains("notice") || q.contains("circular") || q.contains("seminar") || q.contains("remedial") || q.contains("form") || q.contains("exam") || q.contains("નોટિસ") || q.contains("પરિપત્ર") || q.contains("પરીક્ષા")) {
            val notices = NoticeDownloadUtil.NOTICES_LIST
            val count = notices.size
            val noticesText = notices.mapIndexed { idx, n ->
                "${idx + 1}. **[${n.tag}] ${n.title}** (${n.dateText})\n   ${n.description}\n   [ACTION:OPEN_CIRCULAR_PDF:${n.fileName}|📄 Open ${n.title.take(30)}... PDF]"
            }.joinToString("\n\n")

            return "📄 **Active GTU Notices & Academic Circulars ($count Active)**\n\n" +
                    noticesText + "\n\n" +
                    "💡 *કોઈપણ પરિપત્રની ઓરિજિનલ PDF જોવા માટે ઉપરના બટન પર ક્લિક કરો.*"
        }

        // 5. GTU Syllabus & Subject Explanations
        if (q.contains("syllabus") || q.contains("daa") || q.contains("prompt") || q.contains("ai") || q.contains("product") || q.contains("cloud") || q.contains("data center") || q.contains("c programming") || q.contains("emotional") || q.contains("wellbeing") || q.contains("subject") || q.contains("credit") || q.contains("સિલેબસ")) {
            val subjects = CampusSearchManager.getSyllabusDatabase()
            val matchedSubject = subjects.find {
                q.contains(it.code.lowercase(Locale.ROOT)) ||
                        q.contains(it.name.lowercase(Locale.ROOT).split(" ").firstOrNull() ?: "") ||
                        (q.contains("cloud") && it.code.contains("16031")) ||
                        (q.contains("prompt") && it.code.contains("16011")) ||
                        (q.contains("product") && it.code.contains("16021")) ||
                        (q.contains("emotional") && it.code.contains("16081")) ||
                        (q.contains("programming") && it.code.contains("16061"))
            }

            if (matchedSubject != null) {
                val units = matchedSubject.units.joinToString("\n") { "  • **Unit ${it.unitNo}:** ${it.title} (${it.hours} hrs, ${it.weightagePercent}% weightage)" }
                return "📚 **GTU Course Guide: ${matchedSubject.name} (${matchedSubject.code})**\n\n" +
                        "• **Category:** ${matchedSubject.category} | **Credits:** ${matchedSubject.credits} CR\n" +
                        "• **Teaching Scheme:** ${matchedSubject.lectureHours}L + ${matchedSubject.practicalHours}P (${matchedSubject.totalMarks} Total Marks)\n" +
                        "• **Exam Scheme:** Theory ESE: ${matchedSubject.theoryEseMarks}, PA: ${matchedSubject.theoryPaMarks} | Practical PA: ${matchedSubject.practicalPaMarks}, ESE: ${matchedSubject.practicalEseMarks}\n\n" +
                        "**📖 Key Course Units & Weightage:**\n" +
                        units + "\n\n" +
                        "• **Prerequisites:** ${matchedSubject.prerequisite}\n\n" +
                        "ઓફિશિયલ GTU સિલેબસની સહીવાળી PDF ડાઉનલોડ/ઓપન કરવા માટે:\n\n" +
                        "[ACTION:OPEN_SYLLABUS_PDF:${matchedSubject.code}|📄 Download ${matchedSubject.name} PDF]"
            } else {
                val listStr = subjects.joinToString("\n\n") { sub ->
                    "• **${sub.name} (${sub.code})** - ${sub.credits} Credits (${sub.category})\n  [ACTION:OPEN_SYLLABUS_PDF:${sub.code}|📄 Download ${sub.name} PDF]"
                }
                return "📚 **GTU Diploma IT Semester 5 Complete Syllabus**\n\n" +
                        "નીચે સેમેસ્ટર 5 ના તમામ વિષયોની યાદી છે. જે વિષયની PDF જોઈતી હોય તેના બટન પર ક્લિક કરો:\n\n" +
                        listStr
            }
        }

        // 6. Canteen Queries
        if (q.contains("canteen") || q.contains("food") || q.contains("thali") || q.contains("lunch") || q.contains("menu") || q.contains("નાસ્તો") || q.contains("કેન્ટીન")) {
            val status = if (prefHelper.canteenIsOpen) "🟢 OPEN" else "🔴 CURRENTLY CLOSED"
            return "🍽️ **Campus Canteen Live Status ($status)**\n\n" +
                    "• **Counter Status:** $status\n" +
                    "• **Today's Chef Special:** ${prefHelper.canteenSpecialItem}\n" +
                    "• **Queue & Rush Level:** ${if (prefHelper.canteenIsOpen) prefHelper.canteenRushStatus else "No active queue"}\n" +
                    "• **Timings:** 08:30 AM - 06:00 PM (Monday - Saturday)\n" +
                    "• **Popular Items:** Gujarati Thali (₹80), Chole Bhature (₹60), Samosa + Kadak Chai (₹30), Cold Coffee (₹35)."
        }

        // 7. Lost and Found
        if (q.contains("lost") || q.contains("found") || q.contains("claim") || q.contains("ખોવાયેલ") || q.contains("મળેલ")) {
            return "📦 **Campus Lost & Found Assistance**\n\n" +
                    "• Currently tracked items at the Main Security Gate Cabin.\n" +
                    "• If you have lost an item, please visit the Lost & Found section to check verified logs or contact the Security Gate Office.\n" +
                    "• To claim an item, provide valid student enrollment verification."
        }

        // 8. Helpdesk & Faculty Contacts
        if (q.contains("help") || q.contains("contact") || q.contains("email") || q.contains("phone") || q.contains("સંપર્ક")) {
            return "🏛️ **Government Polytechnic, Rajkot - Official Contacts**\n\n" +
                    "• **HOD IT:** Prof. H. Mehta (Ext 301, hod.it@gprajkot.ac.in)\n" +
                    "• **Phone:** 0281-2387553\n" +
                    "• **Official Email:** gp-rajkot-dte@gujarat.gov.in\n" +
                    "• **Official Portal:** https://sites.google.com/view/gprajkot620\n" +
                    "• **Location:** Bhavnagar Road, Near Aji Dam, Rajkot, Gujarat - 360003"
        }

        // 9. General Coding / Engineering questions
        if (q.contains("code") || q.contains("algorithm") || q.contains("array") || q.contains("stack") || q.contains("queue") || q.contains("tree") || q.contains("sql") || q.contains("python") || q.contains("java")) {
            return "💻 **Campus AI Engineering Solution**\n\n" +
                    "**Concept Overview:**\n" +
                    "In Diploma IT Semester 5, mastering practical programming and system architectures is essential for GTU practical examinations and capstone projects.\n\n" +
                    "**Key Best Practices:**\n" +
                    "1. Always verify Time & Space Complexity (Big-O notation).\n" +
                    "2. Follow standard coding guidelines with proper variable naming and edge-case validation.\n" +
                    "3. Practice hands-on code for lab assignments in your allocated labs (${prefHelper.timetableLabs}).\n\n" +
                    "Ask me any specific concept (like *'Design Thinking in AI Product'* or *'C Function categories'*) for detailed explanations!"
        }

        // 10. Gujarati greetings & general help
        if (q.contains("kem cho") || q.contains("kemcho") || q.contains("કેમ છો") || q.contains("નમસ્તે") || q.contains("hello") || q.contains("hi") || q.contains("hey")) {
            return "👋 **નમસ્તે / Hello! I am your Campus AI Copilot.**\n\n" +
                    "હું GP Rajkot IT ડિપાર્ટમેન્ટ માટેનો તમારો સ્માર્ટ AI આસિસ્ટન્ટ છું. હું તમને નીચેની બાબતોમાં PDF સાથે સીધી મદદ કરી શકું છું:\n" +
                    "• 📝 **GTU Course Assignments & PDF Solutions**\n" +
                    "• 📚 **GTU Syllabus & Official PDF Download**\n" +
                    "• 📄 **Exam Notices & Signed Circular PDFs**\n" +
                    "• 🗓️ **Class Master Timetable & Room Locations**\n" +
                    "• 🍽️ **Live Canteen Status & Today's Menu**\n" +
                    "• 💻 **Programming & Practical Exam Guidance**\n\n" +
                    "તમારો પ્રશ્ન નીચે ટાઈપ કરો અથવા ઉપરના ક્વિક ચિપ્સ પર ક્લિક કરો!\n\n" +
                    "[ACTION:OPEN_ASSIGNMENT_PDF:aipd_ass2|📝 View AIPD Assignment 2 PDF]"
        }

        // Default intelligent fallback
        return "✨ **Campus AI Answer**\n\n" +
                "I am here to assist you with all academic, syllabus, and campus operations at Government Polytechnic, Rajkot.\n\n" +
                "• **Academic Term:** ${prefHelper.timetableTermDates}\n" +
                "• **Official Assignments:** 6 Active GTU IT Course Assignments\n" +
                "• **Allocated Classrooms:** ${prefHelper.timetableClassrooms}\n" +
                "• **Live Canteen Status:** ${if (prefHelper.canteenIsOpen) "🟢 Open (${prefHelper.canteenSpecialItem})" else "🔴 Closed"}\n" +
                "• **Active Circulars:** ${NoticeDownloadUtil.NOTICES_LIST.size} Official Documents\n\n" +
                "Feel free to ask about syllabus topics, assignments, timetable, mid-sem results, or circulars for instant answers and direct PDF downloads!\n\n" +
                "[ACTION:OPEN_ASSIGNMENT_PDF:aiwpe_ass5|📝 Open AI Prompt Assignment 5 PDF]"
    }
}
