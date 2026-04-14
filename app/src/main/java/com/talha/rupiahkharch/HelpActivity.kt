package com.talha.rupiahkharch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class HelpActivity : AppCompatActivity() {

    private var attachedImageUri: Uri? = null
    private lateinit var btnAttach: MaterialButton

    // Launcher to pick a screenshot
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            attachedImageUri = result.data?.data
            attachedImageUri?.let {
                btnAttach.text = "File Attached ✅"
                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // 1. Correctly Bind Views
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etDescription = findViewById<TextInputEditText>(R.id.etDescription)
        val actvCategory = findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory)
        btnAttach = findViewById(R.id.btnAttach)
        val btnSend = findViewById<MaterialButton>(R.id.btnSend)

        // 2. Setup Dropdown
        val categories = arrayOf("Bug Report", "Feature Request", "Account Issue", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        actvCategory.setAdapter(adapter)

        // 3. Attachment logic
        btnAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            pickFileLauncher.launch(intent)
        }

        // 4. Send button logic with validation
        btnSend.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val userEmail = etEmail.text.toString().trim()
            val category = actvCategory.text.toString()
            val description = etDescription.text.toString().trim()

            if (name.isEmpty() || userEmail.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                sendSupportEmail(name, userEmail, category, description)

                // --- NEW CODE: CLEAR FIELDS ---
                etFullName.text?.clear()
                etEmail.text?.clear()
                etDescription.text?.clear()
                actvCategory.setText("", false) // Clears the dropdown
                attachedImageUri = null // Reset attachment
                btnAttach.text = "Attach Screenshot (Optional)"

                // Show the Toast
                Toast.makeText(this, "Sent to IT Support team!", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun sendSupportEmail(name: String, email: String, category: String, desc: String) {
        val recipient = "support@rupiahkharch.com"
        val subject = "Support: $category [$name]"
        val body = "User Details:\nName: $name\nEmail: $email\n\nIssue:\n$desc"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)

            attachedImageUri?.let {
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        try {
            // We use Intent.createChooser so the user can pick their email app
            startActivity(Intent.createChooser(intent, "Send email via..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No email client installed", Toast.LENGTH_SHORT).show()
        }
    }
}