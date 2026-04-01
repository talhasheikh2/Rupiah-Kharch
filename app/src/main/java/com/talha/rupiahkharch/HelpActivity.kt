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

    // Launcher to pick a screenshot/video
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            attachedImageUri = result.data?.data
            attachedImageUri?.let {
                btnAttach.text = "File Attached ✅"
                Toast.makeText(this, "Image attached successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // 1. Initialize Views
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etDescription = findViewById<TextInputEditText>(R.id.etDescription)
        val actvCategory = findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory)
        btnAttach = findViewById(R.id.btnAttach)
        val btnSend = findViewById<MaterialButton>(R.id.btnSend)

        // 2. Setup Dropdown (Issue Category)
        val categories = arrayOf("Bug Report", "Feature Request", "Account Issue", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        actvCategory.setAdapter(adapter)

        // 3. Attachment Button Logic
        btnAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*" // Allow images
            pickFileLauncher.launch(intent)
        }

        // 4. Send Report Logic
        btnSend.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val userEmail = etEmail.text.toString().trim()
            val category = actvCategory.text.toString()
            val description = etDescription.text.toString().trim()

            if (name.isEmpty() || userEmail.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendEmail(name, userEmail, category, description)
        }
    }

    private fun sendEmail(name: String, userEmail: String, category: String, description: String) {
        val recipient = "support@rupiahkharch.com" // Replace with your actual email
        val subject = "Support Request: $category from $name"

        val emailBody = """
            Name: $name
            User Email: $userEmail
            Category: $category
            
            Problem Description:
            $description
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, emailBody)

            // Attach the image if the user picked one
            attachedImageUri?.let {
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        try {
            startActivity(Intent.createChooser(intent, "Send report via..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}