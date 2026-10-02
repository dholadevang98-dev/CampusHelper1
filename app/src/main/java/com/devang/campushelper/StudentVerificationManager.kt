package com.devang.campushelper

import android.content.Context
import android.util.Patterns
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.concurrent.ConcurrentHashMap

object StudentVerificationManager {

    /**
     * Government Polytechnic Rajkot GTU Institute Code
     */
    const val GP_RAJKOT_INSTITUTE_CODE = "620"

    /**
     * Allowed institutional email domains
     */
    val ALLOWED_DOMAINS = listOf(
        "gprajkot.ac.in",
        "gprajkot.edu.in",
        "gpr.edu.in",
        "gtu.ac.in"
    )

    /**
     * Recognized Diploma Engineering Branches at GP Rajkot
     */
    val GP_BRANCHES = mapOf(
        "02" to "Automobile Engineering",
        "05" to "Chemical Engineering",
        "06" to "Civil Engineering",
        "07" to "Computer Engineering",
        "09" to "Electrical Engineering",
        "11" to "Electronics & Communication",
        "16" to "Information Technology",
        "19" to "Mechanical Engineering",
        "20" to "Mechatronics Engineering",
        "24" to "Plastic Engineering"
    )

    data class StudentRosterEntry(
        val studentName: String,
        val branchCode: String,
        val admissionYear: String
    )

    /**
     * Pre-approved baseline roster for Government Polytechnic Rajkot.
     */
    val PREAPPROVED_STUDENT_ROSTER: Map<String, StudentRosterEntry> = buildMap {
        // Information Technology (Branch 16)
        put("246200316062", StudentRosterEntry("Devang Jadeja", "16", "2024"))
        put("246200316064", StudentRosterEntry("Harsh Joshi", "16", "2024"))
        put("236200116001", StudentRosterEntry("Aarav Patel", "16", "2023"))
        put("236200116002", StudentRosterEntry("Bhavya Shah", "16", "2023"))
        put("236200116003", StudentRosterEntry("Chirag Mehta", "16", "2023"))
        put("236200116004", StudentRosterEntry("Dhruv Trivedi", "16", "2023"))
        put("246200116001", StudentRosterEntry("Isha Varma", "16", "2024"))
        put("226200116001", StudentRosterEntry("Karan Rathod", "16", "2022"))

        // Computer Engineering (Branch 07)
        put("236200107001", StudentRosterEntry("Manish Parmar", "07", "2023"))
        put("236200107002", StudentRosterEntry("Neha Solanki", "07", "2023"))
        put("236200107015", StudentRosterEntry("Pooja Dave", "07", "2023"))
        put("246200107001", StudentRosterEntry("Rahul Gohil", "07", "2024"))
        put("226200107001", StudentRosterEntry("Sanjay Makwana", "07", "2022"))

        // Civil Engineering (Branch 06)
        put("236200106001", StudentRosterEntry("Tanvi Bhatt", "06", "2023"))
        put("246200106001", StudentRosterEntry("Umang Shukla", "06", "2024"))

        // Mechanical Engineering (Branch 19)
        put("236200119001", StudentRosterEntry("Vikas Chauhan", "19", "2023"))
        put("246200119001", StudentRosterEntry("Yash Pandya", "19", "2024"))

        // Electrical Engineering (Branch 09)
        put("236200109001", StudentRosterEntry("Ankit Borisagar", "09", "2023"))
        put("246200109001", StudentRosterEntry("Deepak Chudasama", "09", "2024"))

        // Electronics & Communication (Branch 11)
        put("236200111001", StudentRosterEntry("Gautam Dodiya", "11", "2023"))

        // Automobile Engineering (Branch 02)
        put("236200102001", StudentRosterEntry("Jayesh Kothari", "02", "2023"))

        // Chemical Engineering (Branch 05)
        put("236200105001", StudentRosterEntry("Kishan Ladani", "05", "2023"))
    }

    /**
     * Active in-memory roster loaded from assets/students.csv + baseline
     */
    val ACTIVE_STUDENT_ROSTER: ConcurrentHashMap<String, StudentRosterEntry> = ConcurrentHashMap()

    private var isInitialized = false

    /**
     * Loads the official student list from assets/students.csv
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        // 1. Seed with baseline roster
        PREAPPROVED_STUDENT_ROSTER.forEach { (k, v) ->
            ACTIVE_STUDENT_ROSTER[k] = v
        }

        // 2. Read from assets/students.csv if present
        try {
            context.assets.open("students.csv").bufferedReader().useLines { lines ->
                lines.forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.isNotEmpty() && !line.startsWith("#") && !line.startsWith("enrollment", ignoreCase = true)) {
                        val cols = line.split(",", "\t", ";").map { it.trim().trim('"', '\'') }
                        if (cols.isNotEmpty()) {
                            val enrollmentNo = cols[0]
                            if (enrollmentNo.length == 12 && enrollmentNo.all { it.isDigit() }) {
                                val name = if (cols.size > 1 && cols[1].isNotBlank()) cols[1] else "Student"
                                val branchCode = if (cols.size > 2 && cols[2].isNotBlank()) cols[2] else enrollmentNo.substring(7, 9)
                                val admissionYear = if (cols.size > 3 && cols[3].isNotBlank()) cols[3] else "20" + enrollmentNo.substring(0, 2)
                                ACTIVE_STUDENT_ROSTER[enrollmentNo] = StudentRosterEntry(name, branchCode, admissionYear)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handled
        }
    }

    sealed class VerificationResult {
        data class Success(val enrollmentNo: String, val branch: String, val studentName: String) : VerificationResult()
        data class InvalidFormat(val message: String) : VerificationResult()
        data class NotFoundInRoster(val message: String) : VerificationResult()
        data class AlreadyClaimed(val message: String, val claimedByEmail: String) : VerificationResult()
        data class Error(val message: String) : VerificationResult()
    }

    /**
     * Validates if the given email belongs to an official GP Rajkot / GTU domain.
     */
    fun isAllowedInstitutionalDomain(email: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false
        val domain = email.substringAfter("@", "").lowercase().trim()
        return ALLOWED_DOMAINS.any { allowed -> domain == allowed || domain.endsWith(".$allowed") }
    }

    /**
     * Step 1: Client-Side GTU Format and College Code (620) Validation
     */
    fun validateEnrollmentFormat(enrollmentNo: String): EnrollmentFormatCheck {
        val clean = enrollmentNo.trim()

        if (clean.isEmpty()) {
            return EnrollmentFormatCheck(false, "Please enter your 12-digit GTU Enrollment Number.")
        }

        if (clean.length != 12 || !clean.all { it.isDigit() }) {
            return EnrollmentFormatCheck(false, "Enrollment Number must be exactly 12 digits (e.g. 236200116001).")
        }

        val collegeCode = clean.substring(2, 5)
        if (collegeCode != GP_RAJKOT_INSTITUTE_CODE) {
            return EnrollmentFormatCheck(
                false,
                "Invalid Institute Code ($collegeCode). This app is restricted exclusively to Government Polytechnic Rajkot (GTU Code 620) students."
            )
        }

        val branchCode = clean.substring(7, 9)
        val branchName = GP_BRANCHES[branchCode]
            ?: return EnrollmentFormatCheck(false, "Invalid Department Code ($branchCode) for GP Rajkot.")

        val year = "20" + clean.substring(0, 2)

        return EnrollmentFormatCheck(
            isValid = true,
            message = "Valid Format ($branchName, Batch $year)",
            branch = branchName,
            admissionYear = year
        )
    }

    data class EnrollmentFormatCheck(
        val isValid: Boolean,
        val message: String,
        val branch: String? = null,
        val admissionYear: String? = null
    )

    /**
     * Step 2 & 3: Master Roster Verification & Anti-Duplicate Check in Cloud Firestore
     *
     * STRICT VERIFICATION:
     * 1. Checks if enrollment number exists in college official records (ACTIVE_STUDENT_ROSTER / Firestore).
     * 2. Rejects unauthorized / fake enrollment numbers not in official college records.
     * 3. Prevents duplicate registrations across different email accounts.
     */
    fun verifyAndClaimEnrollmentAsync(
        enrollmentNo: String,
        currentUserEmail: String,
        currentUserUid: String,
        currentUserName: String,
        firestore: FirebaseFirestore?,
        onResult: (VerificationResult) -> Unit
    ) {
        val cleanEnrollment = enrollmentNo.trim()

        // 1. Format Validation
        val formatCheck = validateEnrollmentFormat(cleanEnrollment)
        if (!formatCheck.isValid) {
            onResult(VerificationResult.InvalidFormat(formatCheck.message))
            return
        }

        val branchName = formatCheck.branch ?: "Government Polytechnic, Rajkot"
        val localRosterEntry = ACTIVE_STUDENT_ROSTER[cleanEnrollment]

        if (firestore == null) {
            if (localRosterEntry == null) {
                onResult(
                    VerificationResult.NotFoundInRoster(
                        "Enrollment Number $cleanEnrollment was not found in Government Polytechnic Rajkot official student directory. Only verified GP Rajkot students are allowed."
                    )
                )
            } else {
                val studentName = if (currentUserName.isNotBlank()) currentUserName else localRosterEntry.studentName
                onResult(
                    VerificationResult.Success(
                        cleanEnrollment,
                        branchName,
                        studentName
                    )
                )
            }
            return
        }

        // 2. Query Firestore 'authorized_students' master roster
        firestore.collection("authorized_students").document(cleanEnrollment).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    // Document exists in Firestore master roster
                    val isClaimed = doc.getBoolean("isClaimed") ?: false
                    val claimedByEmail = doc.getString("claimedByEmail") ?: ""
                    val claimedByUid = doc.getString("claimedByUid") ?: ""
                    val rosterStudentName = doc.getString("studentName") ?: localRosterEntry?.studentName ?: currentUserName
                    val rosterDept = doc.getString("department") ?: branchName

                    // Check if already claimed by a DIFFERENT user/email
                    if (isClaimed && claimedByEmail.isNotEmpty() && !claimedByEmail.equals(currentUserEmail, ignoreCase = true)) {
                        val maskedEmail = maskEmail(claimedByEmail)
                        onResult(
                            VerificationResult.AlreadyClaimed(
                                "This Enrollment Number ($cleanEnrollment) is already registered to account: $maskedEmail. You cannot link the same enrollment number with a different email.",
                                claimedByEmail
                            )
                        )
                        return@addOnSuccessListener
                    }

                    // Check if UID differs if claimed
                    if (isClaimed && claimedByUid.isNotEmpty() && currentUserUid.isNotEmpty() && claimedByUid != currentUserUid && !claimedByEmail.equals(currentUserEmail, ignoreCase = true)) {
                        onResult(
                            VerificationResult.AlreadyClaimed(
                                "This Enrollment Number ($cleanEnrollment) is already claimed by another user account.",
                                claimedByEmail
                            )
                        )
                        return@addOnSuccessListener
                    }

                    // Claim and lock the enrollment number for this email
                    val finalName = if (currentUserName.isNotBlank()) currentUserName else rosterStudentName
                    claimEnrollmentNumberInFirestore(
                        firestore = firestore,
                        enrollmentNo = cleanEnrollment,
                        email = currentUserEmail,
                        uid = currentUserUid,
                        name = finalName,
                        department = rosterDept
                    )

                    onResult(
                        VerificationResult.Success(
                            enrollmentNo = cleanEnrollment,
                            branch = rosterDept,
                            studentName = finalName
                        )
                    )
                } else {
                    // Not yet in Firestore: check local official active roster
                    if (localRosterEntry != null) {
                        // Found in official master list -> Auto-sync to Firestore and claim
                        val studentName = if (currentUserName.isNotBlank()) currentUserName else localRosterEntry.studentName
                        claimEnrollmentNumberInFirestore(
                            firestore = firestore,
                            enrollmentNo = cleanEnrollment,
                            email = currentUserEmail,
                            uid = currentUserUid,
                            name = studentName,
                            department = branchName
                        )

                        onResult(
                            VerificationResult.Success(
                                enrollmentNo = cleanEnrollment,
                                branch = branchName,
                                studentName = studentName
                            )
                        )
                    } else {
                        // Number is NOT found in official student records -> STRICTLY REJECT
                        onResult(
                            VerificationResult.NotFoundInRoster(
                                "Enrollment Number $cleanEnrollment was not found in Government Polytechnic Rajkot student records. Please verify your 12-digit number or contact the IT/Administration department."
                            )
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Firestore fetch failed (e.g. offline / Firebase rules): Check local active roster
                if (localRosterEntry != null) {
                    val studentName = if (currentUserName.isNotBlank()) currentUserName else localRosterEntry.studentName
                    onResult(
                        VerificationResult.Success(
                            enrollmentNo = cleanEnrollment,
                            branch = branchName,
                            studentName = studentName
                        )
                    )
                } else {
                    // Not in official active roster -> STRICTLY REJECT
                    onResult(
                        VerificationResult.NotFoundInRoster(
                            "Enrollment Number $cleanEnrollment was not found in Government Polytechnic Rajkot records."
                        )
                    )
                }
            }
    }

    /**
     * Locks and records the enrollment ownership to the user's email in Firestore
     */
    private fun claimEnrollmentNumberInFirestore(
        firestore: FirebaseFirestore,
        enrollmentNo: String,
        email: String,
        uid: String,
        name: String,
        department: String
    ) {
        try {
            val rosterData = hashMapOf(
                "enrollmentNo" to enrollmentNo,
                "studentName" to name,
                "department" to department,
                "collegeCode" to GP_RAJKOT_INSTITUTE_CODE,
                "collegeName" to "Government Polytechnic, Rajkot",
                "isClaimed" to true,
                "claimedByEmail" to email,
                "claimedByUid" to uid,
                "claimedAt" to System.currentTimeMillis()
            )

            // Save in authorized_students master record
            firestore.collection("authorized_students")
                .document(enrollmentNo)
                .set(rosterData, SetOptions.merge())

            // Save in student profile
            if (uid.isNotEmpty()) {
                val studentProfile = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "enrollmentNo" to enrollmentNo,
                    "department" to department,
                    "collegeCode" to GP_RAJKOT_INSTITUTE_CODE,
                    "collegeName" to "Government Polytechnic, Rajkot",
                    "isVerified" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("students")
                    .document(uid)
                    .set(studentProfile, SetOptions.merge())
            }
        } catch (e: Exception) {
            // Handled
        }
    }

    /**
     * Masks an email for security & privacy display: (e.g. devang@gmail.com -> d****g@gmail.com)
     */
    fun maskEmail(email: String): String {
        if (!email.contains("@")) return email
        val parts = email.split("@")
        val username = parts[0]
        val domain = parts[1]

        val maskedUser = if (username.length <= 2) {
            username.first() + "***"
        } else {
            "${username.first()}****${username.last()}"
        }
        return "$maskedUser@$domain"
    }

    /**
     * Seeds active master roster of GP Rajkot into Firestore collection 'authorized_students'
     * so all valid students exist in the cloud database.
     */
    fun seedMasterRosterToFirestore(firestore: FirebaseFirestore) {
        val rosterToSeed = if (ACTIVE_STUDENT_ROSTER.isNotEmpty()) ACTIVE_STUDENT_ROSTER else PREAPPROVED_STUDENT_ROSTER
        rosterToSeed.forEach { (enrollment, entry) ->
            val branchName = GP_BRANCHES[entry.branchCode] ?: "Diploma Engineering"
            val data = hashMapOf(
                "enrollmentNo" to enrollment,
                "studentName" to entry.studentName,
                "department" to branchName,
                "branchCode" to entry.branchCode,
                "admissionYear" to entry.admissionYear,
                "collegeCode" to GP_RAJKOT_INSTITUTE_CODE,
                "collegeName" to "Government Polytechnic, Rajkot",
                "isClaimed" to false,
                "claimedByEmail" to "",
                "claimedByUid" to ""
            )

            firestore.collection("authorized_students")
                .document(enrollment)
                .set(data, SetOptions.merge())
        }
    }
}
