package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.talha.rupiahkharch.model.ExpenseDatabase
import com.talha.rupiahkharch.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var btnSignUp: Button
    private lateinit var tvLoginUpLink: TextView

    // Initialize Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)

        btnSignUp = findViewById(R.id.btnSignUp)
        tvLoginUpLink = findViewById(R.id.tvLoginUpLink)

        btnSignUp.setOnClickListener {
            performSignUp()
        }

        tvLoginUpLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun performSignUp() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        tilName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        if (name.isEmpty()) {
            tilName.error = "Please enter your name"
            return
        }
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            return
        }
        if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            return
        }
        if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
            return
        }

        lifecycleScope.launch {
            try {
                // 1. Create User in Firebase Cloud
                // .await() allows us to wait for Firebase without using messy listeners
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                if (authResult.user != null) {
                    // 2. Cloud Success! Now Save to Local Room
                    val db = ExpenseDatabase.getDatabase(this@SignUpActivity)
                    val newUser = User(name = name, email = email, password = password)

                    withContext(Dispatchers.IO) {
                        db.userDao().insertUser(newUser)
                    }

                    Toast.makeText(this@SignUpActivity, "Account Created in Cloud!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                // This catches "Email already in use", "No internet", etc.
                Toast.makeText(this@SignUpActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}