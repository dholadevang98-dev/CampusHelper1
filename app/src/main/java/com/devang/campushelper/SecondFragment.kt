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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SecondFragment : Fragment() {

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private lateinit var prefHelper: PreferenceHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            // Handled gracefully
        }
        prefHelper = PreferenceHelper(requireContext())

        val tilName = view.findViewById<TextInputLayout>(R.id.tilName)
        val tilSignUpEmail = view.findViewById<TextInputLayout>(R.id.tilSignUpEmail)
        val tilSignUpPassword = view.findViewById<TextInputLayout>(R.id.tilSignUpPassword)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etSignUpEmail)
        val etPassword = view.findViewById<EditText>(R.id.etSignUpPassword)

        val btnSignUp = view.findViewById<Button>(R.id.btnSignUp)
        val tvLogin = view.findViewById<TextView>(R.id.tvLogin)

        tvLogin.text = Html.fromHtml(
            "Already have an account? <font color='#818CF8'><b>Login</b></font>",
            Html.FROM_HTML_MODE_COMPACT
        )

        // Clear errors on typing
        val clearErrorWatcher = { til: TextInputLayout ->
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    til.error = null
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        }

        etName.addTextChangedListener(clearErrorWatcher(tilName))
        etEmail.addTextChangedListener(clearErrorWatcher(tilSignUpEmail))
        etPassword.addTextChangedListener(clearErrorWatcher(tilSignUpPassword))

        // ==================== SIGN UP SUBMISSION ====================
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            var isValid = true
            tilName.error = null
            tilSignUpEmail.error = null
            tilSignUpPassword.error = null

            // 1. Full Name
            if (name.isEmpty()) {
                tilName.error = "Please enter your full name"
                isValid = false
            } else if (name.length < 2) {
                tilName.error = "Name must be at least 2 characters"
                isValid = false
            }

            // 2. Email
            if (email.isEmpty()) {
                tilSignUpEmail.error = "Please enter your email address"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilSignUpEmail.error = "Please enter a valid email address"
                isValid = false
            }

            // 3. Password
            if (password.isEmpty()) {
                tilSignUpPassword.error = "Please enter a password"
                isValid = false
            } else if (password.length < 6) {
                tilSignUpPassword.error = "Password must be at least 6 characters"
                isValid = false
            }

            if (!isValid) {
                Toast.makeText(requireContext(), "Please resolve the errors above", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proceed with account creation
            proceedWithAccountCreation(name, email, password)
        }

        // Login navigation
        tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    private fun proceedWithAccountCreation(
        name: String,
        email: String,
        password: String
    ) {
        val btnSignUp = view?.findViewById<Button>(R.id.btnSignUp)
        btnSignUp?.isEnabled = false
        btnSignUp?.text = "Creating Account..."

        val firebaseAuth = auth
        val departmentName = "Government Polytechnic, Rajkot"

        if (firebaseAuth != null) {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (!isAdded) return@addOnCompleteListener
                    btnSignUp?.isEnabled = true
                    btnSignUp?.text = "Create Student Account"

                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        val uid = user?.uid ?: "user_${System.currentTimeMillis()}"
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        user?.updateProfile(profileUpdates)

                        // Save student profile document
                        val studentDoc = hashMapOf(
                            "studentName" to name,
                            "email" to email,
                            "department" to departmentName,
                            "role" to "STUDENT",
                            "isVerified" to true,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                        firestore?.collection("students")?.document(uid)?.set(studentDoc)

                        // Store local session
                        prefHelper.saveUserSession(
                            name = name,
                            email = email,
                            role = "STUDENT",
                            uid = uid,
                            department = departmentName,
                            isVerified = true
                        )

                        Toast.makeText(
                            requireContext(),
                            "Welcome to CampusHelper, $name! 🎓",
                            Toast.LENGTH_LONG
                        ).show()

                        findNavController().navigate(R.id.action_SecondFragment_to_homeFragment)
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthUserCollisionException) {
                            val tilSignUpEmail = view?.findViewById<TextInputLayout>(R.id.tilSignUpEmail)
                            tilSignUpEmail?.error = "An account already exists with this email address"
                        } else {
                            showSecurityAlert("Registration Failed", exception?.localizedMessage ?: "Unknown error")
                        }
                    }
                }
        } else {
            // Offline / direct save
            btnSignUp?.isEnabled = true
            btnSignUp?.text = "Create Student Account"
            prefHelper.saveUserSession(
                name = name,
                email = email,
                role = "STUDENT",
                uid = "user_${System.currentTimeMillis()}",
                department = departmentName,
                isVerified = true
            )
            Toast.makeText(requireContext(), "Account created successfully! 🎓", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_SecondFragment_to_homeFragment)
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