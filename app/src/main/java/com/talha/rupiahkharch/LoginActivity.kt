package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.sync.SyncManager
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: Button
    private lateinit var tvSignUpLink: TextView
    private lateinit var tvForgotPassword: TextView

    // Initialize Firebase Auth
    private lateinit var auth: FirebaseAuth

    // Get ViewModel to pass to SyncManager
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUpLink = findViewById(R.id.tvSignUpLink)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        btnLogin.setOnClickListener { performLogin() }

        tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                tilEmail.error = "Enter email to reset password"
            } else {
                auth.sendPasswordResetEmail(email)
                Toast.makeText(this, "Reset email sent to $email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        tilEmail.error = null
        tilPassword.error = null

        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            return
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            return
        }

        lifecycleScope.launch {
            try {
                // 1. Authenticate with FIREBASE CLOUD
                val authResult = auth.signInWithEmailAndPassword(email, password).await()

                if (authResult.user != null) {

                    // --- CLOUD SYNC START ---
                    // Show a message so user knows data is being restored
                    Toast.makeText(this@LoginActivity, "Restoring your data...", Toast.LENGTH_SHORT).show()

                    val syncManager = SyncManager(this@LoginActivity, viewModel)

                    withContext(Dispatchers.IO) {
                        syncManager.downloadUserData()
                    }
                    // --- CLOUD SYNC END ---

                    // Check local name for the welcome toast
                    val db = ExpenseDatabase.getDatabase(this@LoginActivity)
                    val localUser = withContext(Dispatchers.IO) {
                        db.userDao().isEmailRegistered(email)
                    }

                    val userName = localUser?.name ?: "User"
                    Toast.makeText(this@LoginActivity, "Welcome back, $userName!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}