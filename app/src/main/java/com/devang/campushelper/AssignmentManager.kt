package com.devang.campushelper

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AssignmentManager {

    private const val TAG = "AssignmentManager"
    private const val STORAGE_FILE = "assignments_custom_data.json"

    data class AssignmentQuestion(
        val qNo: String,
        val text: String,
        val subPoints: List<String> = emptyList()
    )

    data class AssignmentModel(
        val id: String,
        val subjectCode: String,
        val subjectName: String,
        val assignmentNo: Int,
        val title: String,
        val alignedCO: String,
        val dateText: String,
        val semester: Int = 5,
        val questions: List<AssignmentQuestion> = emptyList(),
        var customFilePath: String? = null,
        var customFileName: String? = null
    )

    private val DEFAULT_ASSIGNMENTS = listOf(
        // 1. AI Product Design - Assignment 2
        AssignmentModel(
            id = "aipd_ass2",
            subjectCode = "DI05016021",
            subjectName = "AI Product Design",
            assignmentNo = 2,
            title = "Assignment - 2 [Aligned with CO2]",
            alignedCO = "CO2: Use design thinking principles to develop user-centric AI solutions",
            dateText = "14/08/2026",
            semester = 5,
            questions = listOf(
                AssignmentQuestion("1", "What is Design Thinking? Explain the five stages of Design Thinking with a suitable example."),
                AssignmentQuestion("2", "Explain the concept of Empathy Map. Design Empathy Map for AI Chatbot User."),
                AssignmentQuestion("3", "Explain the concept of User Persona development with suitable example."),
                AssignmentQuestion(
                    "4",
                    "Write user-centered problem statements for AI applications in each of the following areas:",
                    listOf("a. Education", "b. Healthcare", "c. Agriculture", "d. Banking", "e. Transportation")
                ),
                AssignmentQuestion("5", "Prepare a Customer Journey Map for a student using an AI-based online learning platform."),
                AssignmentQuestion("6", "Explain any five important UX principles for AI systems. Give one practical example for each principle."),
                AssignmentQuestion("7", "What is Human-AI Interaction? Draw a simple diagram showing the interaction between a user and an AI system."),
                AssignmentQuestion("8", "What is AI bias? Explain any four possible causes of bias in AI systems. Also give one real-world example showing how biased data can result in an unfair AI decision."),
                AssignmentQuestion("9", "What is Explainable AI (XAI)? Explain why explainability is important in AI systems used for Healthcare, Banking, and Education.")
            )
        ),

        // 2. Artificial Intelligence with Prompt Engineering - Assignment 5
        AssignmentModel(
            id = "aiwpe_ass5",
            subjectCode = "DI05016011",
            subjectName = "Artificial Intelligence with Prompt Engineering",
            assignmentNo = 5,
            title = "Assignment - 5 [Aligned with CO5]",
            alignedCO = "CO5: Develop simple AI-based applications using APIs and productivity tools",
            dateText = "September 2026",
            semester = 5,
            questions = listOf(
                AssignmentQuestion(
                    "Q1",
                    "Explain how AI tools can be used for:",
                    listOf("• Writing emails", "• Generating reports", "• Creating presentations")
                ),
                AssignmentQuestion(
                    "Q2",
                    "Explain the following applications of AI in software development:",
                    listOf("• Code generation", "• Code explanation", "• Code documentation", "• Code debugging")
                ),
                AssignmentQuestion("Q3", "Explain the steps involved in finding and fixing bugs using AI. Give a suitable example."),
                AssignmentQuestion(
                    "Q4",
                    "Explain the concept of AI APIs. Write short notes on:",
                    listOf("• OpenAI API", "• Gemini API")
                ),
                AssignmentQuestion(
                    "Q5",
                    "Explain any four applications of AI:",
                    listOf("• AI Chatbot", "• AI Blog Writer", "• AI Document Summarizer", "• AI Question Generator")
                )
            )
        ),

        // 3. AI Product Design - Assignment 4
        AssignmentModel(
            id = "aipd_ass4",
            subjectCode = "DI05016021",
            subjectName = "AI Product Design",
            assignmentNo = 4,
            title = "Assignment - 4 [Aligned with CO4]",
            alignedCO = "CO4: AI in Social Media, Recommendation Systems & Digital Experience",
            dateText = "September 2026",
            semester = 5,
            questions = listOf(
                AssignmentQuestion("1", "Define a Virtual Influencer. Name one real-world example of an AI-generated avatar used in digital marketing."),
                AssignmentQuestion("2", "Discuss the applications of AI in social media, content creation, and recommendation systems along with associated risks."),
                AssignmentQuestion("3", "Describe the working mechanism of a social media recommendation feed (e.g., Instagram Reels or TikTok)."),
                AssignmentQuestion("4", "Highlight three advantages of deploying AI chatbots for e-commerce platforms."),
                AssignmentQuestion("5", "Discuss the technical and societal challenges of mitigating AI-generated fake news on viral messaging platforms."),
                AssignmentQuestion("6", "Describe how coordinated Bot Networks operate to manipulate trending topics or public sentiment."),
                AssignmentQuestion("7", "Detail how a combination of an AI logo generator and an AI UI mockup tool can be used to quickly prototype a landing page for a fictional mobile application.")
            )
        ),

        // 4. Structured Programming with C - Assignment 3
        AssignmentModel(
            id = "spc_ass3",
            subjectCode = "DI05016061",
            subjectName = "Structured Programming with C",
            assignmentNo = 3,
            title = "Assignment - 3 [Aligned with CO3]",
            alignedCO = "CO3: Implement user-defined functions",
            dateText = "August 2026",
            semester = 5,
            questions = listOf(
                AssignmentQuestion("Q1", "Define a function in C. Explain any two needs/advantages of using functions."),
                AssignmentQuestion("Q2", "List and explain the elements of a user-defined function in C."),
                AssignmentQuestion("Q3", "What is a function declaration (prototype)? Write the general syntax with one suitable example."),
                AssignmentQuestion("Q4", "What is a function call? Explain with a suitable example."),
                AssignmentQuestion(
                    "Q5",
                    "Explain the following categories of functions:",
                    listOf("1. No arguments and no return value", "2. Arguments but no return value", "3. Arguments with return value", "4. No arguments but returns a value")
                )
            )
        ),

        // 5. Cloud and Data Center Technology - Assignment 1
        AssignmentModel(
            id = "cdct_ass1",
            subjectCode = "DI05016031",
            subjectName = "Cloud and Data Center Technology",
            assignmentNo = 1,
            title = "Assignment - 1 [Aligned with CO1 & CO2]",
            alignedCO = "CO1: Virtualization & Cloud Architecture",
            dateText = "August 2026",
            semester = 5,
            questions = listOf(
                AssignmentQuestion("1", "Define Cloud Computing. Differentiate between IaaS, PaaS, and SaaS with real-world industry examples."),
                AssignmentQuestion("2", "Explain Type 1 (Bare-metal) and Type 2 (Hosted) Hypervisors with architectural diagrams."),
                AssignmentQuestion("3", "What is Software Defined Networking (SDN) in modern Data Centers? Explain its control & data planes."),
                AssignmentQuestion("4", "Describe Infrastructure as Code (IaC) and explain its benefits in cloud deployment automation."),
                AssignmentQuestion("5", "Write short notes on Cloud IAM (Identity & Access Management) and the Cloud Shared Responsibility Model.")
            )
        ),

        // 6. Emotional Intelligence & Digital Wellbeing - Assignment 1
        AssignmentModel(
            id = "eidw_ass1",
            subjectCode = "DI05016081",
            subjectName = "Emotional Intelligence & Digital Wellbeing",
            assignmentNo = 1,
            title = "Assignment - 1 [Aligned with CO1]",
            alignedCO = "CO1: Foundations of EQ & Digital Wellbeing",
            dateText = "August 2026",
            semester = 5,
            questions = listOf(
                AssignmentQuestion("1", "Differentiate between IQ and EQ. Explain why Emotional Intelligence is crucial for IT engineers."),
                AssignmentQuestion("2", "Explain the five key domains of Daniel Goleman's Emotional Intelligence framework."),
                AssignmentQuestion("3", "What is Digital Burnout? Discuss 5 practical techniques for screen-time management and workplace mental health."),
                AssignmentQuestion("4", "Describe the concept of Green Computing and sustainable digital practices in engineering organizations.")
            )
        )
    )

    val ASSIGNMENTS_LIST = mutableListOf<AssignmentModel>().apply {
        addAll(DEFAULT_ASSIGNMENTS)
    }

    fun getAssignmentById(id: String): AssignmentModel? {
        return ASSIGNMENTS_LIST.find { it.id.equals(id, ignoreCase = true) }
    }

    fun getAssignmentsBySubject(subjectCode: String): List<AssignmentModel> {
        return ASSIGNMENTS_LIST.filter { it.subjectCode.equals(subjectCode, ignoreCase = true) }
    }

    fun addAssignment(assignment: AssignmentModel, context: Context? = null) {
        val existingIndex = ASSIGNMENTS_LIST.indexOfFirst { it.id.equals(assignment.id, ignoreCase = true) }
        if (existingIndex != -1) {
            ASSIGNMENTS_LIST[existingIndex] = assignment
        } else {
            ASSIGNMENTS_LIST.add(0, assignment)
        }
        if (context != null) {
            saveToStorage(context)
        }
    }

    fun updateAssignment(assignment: AssignmentModel, context: Context? = null) {
        val index = ASSIGNMENTS_LIST.indexOfFirst { it.id.equals(assignment.id, ignoreCase = true) }
        if (index != -1) {
            ASSIGNMENTS_LIST[index] = assignment
        } else {
            ASSIGNMENTS_LIST.add(0, assignment)
        }
        if (context != null) {
            saveToStorage(context)
        }
    }

    fun removeAssignment(id: String, context: Context? = null): Boolean {
        val removed = ASSIGNMENTS_LIST.removeAll { it.id.equals(id, ignoreCase = true) }
        if (removed && context != null) {
            saveToStorage(context)
        }
        return removed
    }

    fun removeAssignment(index: Int, context: Context? = null): Boolean {
        if (index in 0 until ASSIGNMENTS_LIST.size) {
            ASSIGNMENTS_LIST.removeAt(index)
            if (context != null) {
                saveToStorage(context)
            }
            return true
        }
        return false
    }

    fun resetToDefaults(context: Context? = null) {
        ASSIGNMENTS_LIST.clear()
        ASSIGNMENTS_LIST.addAll(DEFAULT_ASSIGNMENTS)
        if (context != null) {
            saveToStorage(context)
        }
    }

    fun parseQuestionsFromRawText(raw: String): List<AssignmentQuestion> {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<AssignmentQuestion>()
        var currentQNo: String? = null
        var currentQText: StringBuilder? = null
        val currentSubPoints = mutableListOf<String>()

        fun flush() {
            if (currentQNo != null && currentQText != null) {
                result.add(
                    AssignmentQuestion(
                        qNo = currentQNo!!,
                        text = currentQText.toString().trim(),
                        subPoints = currentSubPoints.toList()
                    )
                )
            }
            currentQNo = null
            currentQText = null
            currentSubPoints.clear()
        }

        val qPattern = Regex("""^(?:Q\.?|Question\s*)?(\d+)[.):\-]\s*(.*)""", RegexOption.IGNORE_CASE)
        val subPattern = Regex("""^([a-zA-Z][.):\-]|•|\*|-|\d+\.)\s*(.*)""")

        for (line in lines) {
            val qMatch = qPattern.find(line)
            if (qMatch != null && (line.startsWith("Q", ignoreCase = true) || line.startsWith("Question", ignoreCase = true) || (line[0].isDigit() && !line.startsWith("1.") || currentQNo == null || line.contains("?")))) {
                flush()
                currentQNo = "Q" + qMatch.groupValues[1]
                currentQText = StringBuilder(qMatch.groupValues[2].ifEmpty { line })
            } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || (line.length > 2 && line[1] == '.' && line[0].isLetter())) {
                if (currentQNo == null) {
                    currentQNo = "Q${result.size + 1}"
                    currentQText = StringBuilder(line)
                } else {
                    currentSubPoints.add(line)
                }
            } else {
                if (currentQNo == null) {
                    currentQNo = "Q${result.size + 1}"
                    currentQText = StringBuilder(line)
                } else {
                    if (line.endsWith("?") || currentQText?.toString()?.isEmpty() == true) {
                        flush()
                        currentQNo = "Q${result.size + 1}"
                        currentQText = StringBuilder(line)
                    } else {
                        currentQText?.append(" ")?.append(line)
                    }
                }
            }
        }
        flush()

        if (result.isEmpty() && lines.isNotEmpty()) {
            for ((idx, line) in lines.withIndex()) {
                result.add(AssignmentQuestion("Q${idx + 1}", line))
            }
        }

        return result
    }

    fun saveToStorage(context: Context) {
        try {
            val jsonArray = JSONArray()
            for (ass in ASSIGNMENTS_LIST) {
                val obj = JSONObject().apply {
                    put("id", ass.id)
                    put("subjectCode", ass.subjectCode)
                    put("subjectName", ass.subjectName)
                    put("assignmentNo", ass.assignmentNo)
                    put("title", ass.title)
                    put("alignedCO", ass.alignedCO)
                    put("dateText", ass.dateText)
                    put("semester", ass.semester)
                    if (ass.customFilePath != null) put("customFilePath", ass.customFilePath)
                    if (ass.customFileName != null) put("customFileName", ass.customFileName)

                    val qArray = JSONArray()
                    for (q in ass.questions) {
                        val qObj = JSONObject().apply {
                            put("qNo", q.qNo)
                            put("text", q.text)
                            val subArr = JSONArray()
                            q.subPoints.forEach { subArr.put(it) }
                            put("subPoints", subArr)
                        }
                        qArray.put(qObj)
                    }
                    put("questions", qArray)
                }
                jsonArray.put(obj)
            }

            val file = File(context.filesDir, STORAGE_FILE)
            file.writeText(jsonArray.toString(2))
            Log.d(TAG, "Assignments saved to storage: ${ASSIGNMENTS_LIST.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving assignments to storage", e)
        }
    }

    fun loadFromStorage(context: Context) {
        try {
            val file = File(context.filesDir, STORAGE_FILE)
            if (!file.exists()) return

            val content = file.readText()
            if (content.isBlank()) return

            val jsonArray = JSONArray(content)
            val loaded = mutableListOf<AssignmentModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val subjectCode = obj.getString("subjectCode")
                val subjectName = obj.getString("subjectName")
                val assignmentNo = obj.optInt("assignmentNo", 1)
                val title = obj.getString("title")
                val alignedCO = obj.optString("alignedCO", "")
                val dateText = obj.optString("dateText", "")
                val semester = obj.optInt("semester", 5)
                val customFilePath = if (obj.has("customFilePath") && !obj.isNull("customFilePath")) obj.getString("customFilePath") else null
                val customFileName = if (obj.has("customFileName") && !obj.isNull("customFileName")) obj.getString("customFileName") else null

                val qArray = obj.optJSONArray("questions") ?: JSONArray()
                val qList = mutableListOf<AssignmentQuestion>()
                for (j in 0 until qArray.length()) {
                    val qObj = qArray.getJSONObject(j)
                    val qNo = qObj.optString("qNo", "Q${j + 1}")
                    val text = qObj.optString("text", "")
                    val subArr = qObj.optJSONArray("subPoints") ?: JSONArray()
                    val subPoints = mutableListOf<String>()
                    for (k in 0 until subArr.length()) {
                        subPoints.add(subArr.getString(k))
                    }
                    qList.add(AssignmentQuestion(qNo, text, subPoints))
                }

                loaded.add(
                    AssignmentModel(
                        id = id,
                        subjectCode = subjectCode,
                        subjectName = subjectName,
                        assignmentNo = assignmentNo,
                        title = title,
                        alignedCO = alignedCO,
                        dateText = dateText,
                        semester = semester,
                        questions = qList,
                        customFilePath = customFilePath,
                        customFileName = customFileName
                    )
                )
            }

            if (loaded.isNotEmpty()) {
                ASSIGNMENTS_LIST.clear()
                ASSIGNMENTS_LIST.addAll(loaded)
                Log.d(TAG, "Assignments loaded from storage: ${ASSIGNMENTS_LIST.size} items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading assignments from storage", e)
        }
    }
}
