package com.devang.campushelper

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class FirstFragment : Fragment() {

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private lateinit var prefHelper: PreferenceHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            StudentVerificationManager.initialize(requireContext().applicationContext)
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            firestore?.let { StudentVerificationManager.seedMasterRosterToFirestore(it) }
        } catch (e: Exception) {
            // Handled gracefully
        }
        prefHelper = PreferenceHelper(requireContext())

        // UI Elements
        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)
        val tvSignUp = view.findViewById<TextView>(R.id.tvSignUp)

        tvSignUp.text = Html.fromHtml(
            "Don't have an account? <font color='#818CF8'><b>Sign Up</b></font>",
            Html.FROM_HTML_MODE_COMPACT
        )

        // Clear errors on typing
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilEmail.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilPassword.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // ==================== EMAIL & PASSWORD LOGIN ACTION ====================
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            var isValid = true
            tilEmail.error = null
            tilPassword.error = null

            if (email.isEmpty()) {
                tilEmail.error = "Please enter your email address"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
                isValid = false
            }

            if (password.isEmpty()) {
                tilPassword.error = "Please enter your password"
                isValid = false
            } else if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            btnLogin.isEnabled = false
            btnLogin.text = "Signing In..."

            val firebaseAuth = auth
            if (firebaseAuth != null) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (!isAdded) return@addOnCompleteListener
                        btnLogin.isEnabled = true
                        btnLogin.text = "Sign In"

                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            val uid = user?.uid ?: ""
                            val displayName = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

                            firestore?.collection("students")?.document(uid)?.get()
                                ?.addOnSuccessListener { doc ->
                                    val enrollment = doc?.getString("enrollmentNo") ?: ""
                                    val dept = doc?.getString("department") ?: "Government Polytechnic, Rajkot"
                                    val phone = doc?.getString("phone") ?: ""

                                    prefHelper.saveUserSession(
                                        name = displayName,
                                        email = email,
                                        phone = phone,
                                        role = "STUDENT",
                                        uid = uid,
                                        enrollmentNo = enrollment,
                                        department = dept,
                                        isVerified = true
                                    )

                                    Toast.makeText(requireContext(), "Welcome back, $displayName! 🎓", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(R.id.action_FirstFragment_to_homeFragment)
                                }
                                ?.addOnFailureListener {
                                    prefHelper.saveUserSession(
                                        name = displayName,
                                        email = email,
                                        role = "STUDENT",
                                        uid = uid,
                                        department = "Government Polytechnic, Rajkot",
                                        isVerified = true
                                    )
                                    findNavController().navigate(R.id.action_FirstFragment_to_homeFragment)
                                }
                        } else {
                            when (task.exception) {
                                is FirebaseAuthInvalidUserException -> tilEmail.error = "No account found with this email"
                                is FirebaseAuthInvalidCredentialsException -> tilPassword.error = "Invalid password. Please try again."
                                else -> showSecurityAlert("Login Failed", task.exception?.localizedMessage ?: "Invalid credentials.")
                            }
                        }
                    }
            } else {
                btnLogin.isEnabled = true
                btnLogin.text = "Sign In"
                showSecurityAlert("Notice", "Authentication service not initialized.")
            }
        }

        // FORGOT PASSWORD
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                auth?.sendPasswordResetEmail(email)
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Password reset link sent to $email", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                tilEmail.error = "Enter your email to receive reset link"
            }
        }

        // SIGN UP NAVIGATION
        tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    private fun showSecurityAlert(title: String, message: String) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}