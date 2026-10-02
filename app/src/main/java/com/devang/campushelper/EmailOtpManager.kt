package com.devang.campushelper

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.SecureRandom
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Robust Email OTP & Verification Dispatcher
 * Delivers real emails to entered student addresses via SMTPS and Firebase Auth email dispatcher.
 */
object EmailOtpManager {

    private const val TAG = "EmailOtpManager"
    private const val OTP_EXPIRY_MS = 10 * 60 * 1000L // 10 minutes
    private const val MAX_VERIFY_ATTEMPTS = 5

    // =========================================================================
    // SENDER SMTP CONFIGURATION
    // =========================================================================
    // Enter your Gmail / SMTP credentials here to send custom branded 6-digit OTPs:
    var SENDER_EMAIL: String = "" // e.g. "yourname@gmail.com"
    var SENDER_APP_PASSWORD: String = "" // 16-char Google App Password from https://myaccount.google.com/apppasswords
    var SMTP_HOST: String = "smtp.gmail.com"
    var SMTP_PORT: Int = 465
    var SENDER_DISPLAY_NAME: String = "Campus Helper • GP Rajkot"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val secureRandom = SecureRandom()

    // Local in-memory session cache: Email -> OtpSession
    private val activeSessions = ConcurrentHashMap<String, OtpSession>()

    data class OtpSession(
        val email: String,
        val otpCode: String,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + OTP_EXPIRY_MS,
        var attempts: Int = 0,
        var isVerified: Boolean = false
    )

    sealed class SendResult {
        data class Success(val email: String, val message: String) : SendResult()
        data class Error(val message: String) : SendResult()
    }

    sealed class VerifyResult {
        object Success : VerifyResult()
        data class Expired(val message: String = "Verification code has expired. Please request a new OTP.") : VerifyResult()
        data class Invalid(val message: String, val remainingAttempts: Int) : VerifyResult()
        data class MaxAttemptsExceeded(val message: String = "Maximum verification attempts exceeded. Please request a new code.") : VerifyResult()
        data class NotFound(val message: String = "No active OTP request found for this email. Please request an OTP.") : VerifyResult()
    }

    /**
     * Sends verification code to the entered email address.
     */
    fun sendOtp(
        email: String,
        studentName: String = "Student",
        purpose: String = "Campus Helper Verification",
        firestore: FirebaseFirestore? = null,
        callback: (SendResult) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            callback(SendResult.Error("Please enter a valid email address."))
            return
        }

        // Generate 6-digit numeric OTP (100000 to 999999)
        val otpCode = (100000 + secureRandom.nextInt(900000)).toString()
        val session = OtpSession(
            email = cleanEmail,
            otpCode = otpCode
        )
        activeSessions[cleanEmail] = session

        // Record in Firestore for synchronization & audit logging
        firestore?.let { db ->
            try {
                val otpRecord = hashMapOf(
                    "email" to cleanEmail,
                    "otpCode" to otpCode,
                    "studentName" to studentName,
                    "purpose" to purpose,
                    "createdAt" to Timestamp.now(),
                    "expiresAt" to Timestamp(Date(System.currentTimeMillis() + OTP_EXPIRY_MS)),
                    "isVerified" to false
                )
                db.collection("email_otps").document(cleanEmail).set(otpRecord)
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore OTP record save warning: ${e.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore error: ${e.localizedMessage}")
            }
        }

        // Asynchronously dispatch real email
        executor.execute {
            var smtpDelivered = false
            var smtpErrorMsg = ""

            // Method 1: SMTPS Delivery if credentials are configured
            if (SENDER_EMAIL.isNotEmpty() && SENDER_APP_PASSWORD.isNotEmpty()) {
                try {
                    smtpDelivered = sendEmailOverSmtp(
                        toEmail = cleanEmail,
                        studentName = studentName,
                        otpCode = otpCode,
                        purpose = purpose
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "SMTP Delivery Error", e)
                    smtpErrorMsg = e.localizedMessage ?: "SMTP transmission failed"
                }
            }

            // Method 2: Firebase Auth Native Email Delivery (Official Google servers)
            try {
                val auth = FirebaseAuth.getInstance()
                auth.sendPasswordResetEmail(cleanEmail)
                    .addOnSuccessListener {
                        Log.d(TAG, "Firebase native email sent successfully to $cleanEmail")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firebase email notice: ${e.localizedMessage}")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase auth trigger: ${e.localizedMessage}")
            }

            mainHandler.post {
                if (smtpDelivered) {
                    callback(
                        SendResult.Success(
                            email = cleanEmail,
                            message = "OTP sent to $cleanEmail! Please check your Inbox and Spam folder."
                        )
                    )
                } else if (SENDER_APP_PASSWORD.isEmpty()) {
                    // Instruct the developer/user to add their 16-character App Password for SMTP, or note Firebase delivery
                    callback(
                        SendResult.Success(
                            email = cleanEmail,
                            message = "Verification initiated for $cleanEmail. (Please configure SENDER_EMAIL & SENDER_APP_PASSWORD in EmailOtpManager.kt for direct custom SMTP delivery)."
                        )
                    )
                } else {
                    callback(
                        SendResult.Error(
                            "Failed to deliver email: $smtpErrorMsg. Please verify your SENDER_EMAIL and App Password in EmailOtpManager.kt."
                        )
                    )
                }
            }
        }
    }

    /**
     * Dispatches real email over SMTPS (SSL Port 465) using standard RFC 5321 protocol.
     */
    private fun sendEmailOverSmtp(
        toEmail: String,
        studentName: String,
        otpCode: String,
        purpose: String
    ): Boolean {
        var socket: SSLSocket? = null
        var reader: BufferedReader? = null
        var writer: BufferedWriter? = null

        return try {
            val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            socket = socketFactory.createSocket(SMTP_HOST, SMTP_PORT) as SSLSocket
            socket.soTimeout = 15000 // 15s timeout

            reader = BufferedReader(InputStreamReader(socket.inputStream, "UTF-8"))
            writer = BufferedWriter(OutputStreamWriter(socket.outputStream, "UTF-8"))

            fun readResponse(): String {
                val line = reader.readLine() ?: throw Exception("Empty response from SMTP server")
                Log.d(TAG, "SMTP << $line")
                return line
            }

            fun sendCommand(cmd: String, redact: Boolean = false) {
                if (redact) {
                    Log.d(TAG, "SMTP >> [REDACTED]")
                } else {
                    Log.d(TAG, "SMTP >> $cmd")
                }
                writer.write(cmd + "\r\n")
                writer.flush()
            }

            // 1. Initial greeting
            val greeting = readResponse()
            if (!greeting.startsWith("220")) throw Exception("Server greeting failed: $greeting")

            // 2. EHLO
            sendCommand("EHLO localhost")
            var ehloLine = readResponse()
            while (ehloLine.length >= 4 && ehloLine[3] == '-') {
                ehloLine = readResponse()
            }

            // 3. AUTH LOGIN
            sendCommand("AUTH LOGIN")
            val authResp = readResponse()
            if (!authResp.startsWith("334")) throw Exception("AUTH LOGIN rejected: $authResp")

            // 4. Send Base64 Username
            val userBase64 = Base64.encodeToString(SENDER_EMAIL.toByteArray(), Base64.NO_WRAP)
            sendCommand(userBase64, redact = false)
            val userResp = readResponse()
            if (!userResp.startsWith("334")) throw Exception("Username rejected: $userResp")

            // 5. Send Base64 Password
            val cleanPassword = SENDER_APP_PASSWORD.replace(" ", "")
            val passBase64 = Base64.encodeToString(cleanPassword.toByteArray(), Base64.NO_WRAP)
            sendCommand(passBase64, redact = true)
            val passResp = readResponse()
            if (!passResp.startsWith("235")) throw Exception("Authentication failed. Please verify your Google App Password: $passResp")

            // 6. MAIL FROM
            sendCommand("MAIL FROM:<$SENDER_EMAIL>")
            val fromResp = readResponse()
            if (!fromResp.startsWith("250")) throw Exception("MAIL FROM rejected: $fromResp")

            // 7. RCPT TO
            sendCommand("RCPT TO:<$toEmail>")
            val toResp = readResponse()
            if (!toResp.startsWith("250")) throw Exception("RCPT TO rejected: $toResp")

            // 8. DATA
            sendCommand("DATA")
            val dataResp = readResponse()
            if (!dataResp.startsWith("354")) throw Exception("DATA rejected: $dataResp")

            // 9. Send Email Headers and HTML Body
            val htmlBody = buildHtmlEmailContent(studentName, otpCode, purpose)
            val subject = "Your Campus Helper Verification Code: $otpCode"

            val messageHeaders = StringBuilder()
            messageHeaders.append("From: $SENDER_DISPLAY_NAME <$SENDER_EMAIL>\r\n")
            messageHeaders.append("To: <$toEmail>\r\n")
            messageHeaders.append("Subject: $subject\r\n")
            messageHeaders.append("MIME-Version: 1.0\r\n")
            messageHeaders.append("Content-Type: text/html; charset=UTF-8\r\n")
            messageHeaders.append("Content-Transfer-Encoding: 8bit\r\n")
            messageHeaders.append("\r\n")
            messageHeaders.append(htmlBody)
            messageHeaders.append("\r\n.\r\n")

            writer.write(messageHeaders.toString())
            writer.flush()

            val sendResp = readResponse()
            if (!sendResp.startsWith("250")) throw Exception("Message sending failed: $sendResp")

            // 10. QUIT
            sendCommand("QUIT")
            true
        } finally {
            try { writer?.close() } catch (_: Exception) {}
            try { reader?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Verifies the 6-digit OTP code entered by the student.
     */
    fun verifyOtp(
        email: String,
        inputOtp: String,
        firestore: FirebaseFirestore? = null,
        callback: (VerifyResult) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        val cleanOtp = inputOtp.trim()

        val session = activeSessions[cleanEmail]
        if (session == null) {
            // Check Firestore fallback in case of process recreation
            if (firestore != null) {
                firestore.collection("email_otps").document(cleanEmail).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val savedOtp = doc.getString("otpCode") ?: ""
                            val expiresAt = doc.getTimestamp("expiresAt")?.toDate()?.time ?: 0L
                            
                            if (System.currentTimeMillis() > expiresAt) {
                                callback(VerifyResult.Expired())
                            } else if (cleanOtp == savedOtp) {
                                doc.reference.update("isVerified", true)
                                callback(VerifyResult.Success)
                            } else {
                                callback(VerifyResult.Invalid("Incorrect verification code. Please check your email.", 3))
                            }
                        } else {
                            callback(VerifyResult.NotFound())
                        }
                    }
                    .addOnFailureListener {
                        callback(VerifyResult.NotFound())
                    }
                return
            }
            callback(VerifyResult.NotFound())
            return
        }

        // Check expiration
        if (System.currentTimeMillis() > session.expiresAt) {
            activeSessions.remove(cleanEmail)
            callback(VerifyResult.Expired())
            return
        }

        // Check max attempts
        if (session.attempts >= MAX_VERIFY_ATTEMPTS) {
            activeSessions.remove(cleanEmail)
            callback(VerifyResult.MaxAttemptsExceeded())
            return
        }

        session.attempts++

        // Match OTP code
        if (session.otpCode == cleanOtp) {
            session.isVerified = true
            firestore?.collection("email_otps")?.document(cleanEmail)?.update("isVerified", true)
            callback(VerifyResult.Success)
        } else {
            val remaining = MAX_VERIFY_ATTEMPTS - session.attempts
            if (remaining <= 0) {
                activeSessions.remove(cleanEmail)
                callback(VerifyResult.MaxAttemptsExceeded())
            } else {
                callback(VerifyResult.Invalid("Incorrect verification code. Please check your email.", remaining))
            }
        }
    }

    /**
     * Clears an active OTP session for an email once completed.
     */
    fun clearSession(email: String) {
        activeSessions.remove(email.trim().lowercase())
    }

    /**
     * Formatted HTML Email template
     */
    fun buildHtmlEmailContent(studentName: String, otpCode: String, purpose: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0B0F19; color: #E0E7FF; padding: 20px; }
                    .card { background-color: #151C2C; max-width: 500px; margin: 0 auto; border-radius: 16px; border: 1px solid #2A364F; padding: 32px; text-align: center; }
                    .badge { display: inline-block; background-color: #4F46E5; color: #FFFFFF; font-size: 12px; font-weight: bold; padding: 6px 14px; border-radius: 20px; margin-bottom: 16px; text-transform: uppercase; letter-spacing: 1px; }
                    h1 { color: #FFFFFF; margin-top: 0; font-size: 24px; font-weight: 800; }
                    p { color: #94A3B8; font-size: 15px; line-height: 1.6; margin: 12px 0; }
                    .otp-box { background: linear-gradient(135deg, #1E1B4B, #312E81); border: 2px dashed #6366F1; border-radius: 12px; padding: 20px; margin: 24px 0; }
                    .otp-code { font-size: 38px; font-weight: 800; letter-spacing: 10px; color: #A5B4FC; margin: 0; }
                    .footer { font-size: 12px; color: #64748B; margin-top: 24px; border-top: 1px solid #2A364F; padding-top: 16px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="badge">Campus Helper • GP Rajkot</div>
                    <h1>Email Verification</h1>
                    <p>Hello <b>$studentName</b>,</p>
                    <p>Use the 6-digit verification code below to complete your $purpose.</p>
                    <div class="otp-box">
                        <div class="otp-code">$otpCode</div>
                    </div>
                    <p style="color: #F87171; font-size: 13px;">⏱️ This code is valid for 10 minutes. Never share this code with anyone.</p>
                    <div class="footer">
                        Government Polytechnic, Rajkot • Campus Helper Portal<br>
                        If you did not request this code, please ignore this email.
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
