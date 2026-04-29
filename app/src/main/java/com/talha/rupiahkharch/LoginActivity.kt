package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.talha.rupiahkharch.sync.SyncManager
import com.talha.rupiahkharch.viewmodel.ExpenseViewModel
import com.talha.rupiahkharch.viewmodel.SavingsViewModel // ADD THIS IMPORT
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

    private lateinit var auth: FirebaseAuth

    // UPDATED: Now initializing BOTH ViewModels
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private val savingsViewModel: SavingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

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
            tilEmail.error = "Email is required"; return
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"; return
        }

        lifecycleScope.launch {
            try {
                btnLogin.isEnabled = false
                val authResult = auth.signInWithEmailAndPassword(email, password).await()

                if (authResult.user != null) {
                    Toast.makeText(this@LoginActivity, "Restoring data from cloud...", Toast.LENGTH_SHORT).show()

                    // FIXED: Now passing both expenseViewModel AND savingsViewModel
                    val syncManager = SyncManager(this@LoginActivity, expenseViewModel, savingsViewModel)

                    withContext(Dispatchers.IO) {
                        syncManager.downloadUserData()
                    }

                    Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                btnLogin.isEnabled = true
                Toast.makeText(this@LoginActivity, "Login Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}