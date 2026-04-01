package com.talha.rupiahkharch

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var ivProfilePicture: ImageView
    private var cameraImageUri: Uri? = null

    // Launcher for Camera/Gallery Result
    private val pickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Check if the result comes from Gallery (result.data) or Camera (cameraImageUri)
            val uri = result.data?.data ?: cameraImageUri
            uri?.let {
                selectedImageUri = it
                ivProfilePicture.load(it) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }

                // Grant Persistence for Gallery URIs so they don't expire
                if (result.data?.data != null) {
                    try {
                        contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    // Launcher for Runtime Permission Request
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile)

        val btnBack = findViewById<MaterialCardView>(R.id.btnBack)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnChangeImage = findViewById<MaterialCardView>(R.id.btnChangeImage)
        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)

        // Load existing data from SharedPreferences
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        etFullName.setText(sharedPref.getString("user_name", ""))
        etEmail.setText(sharedPref.getString("user_email", ""))
        etPhone.setText(sharedPref.getString("user_phone", ""))

        sharedPref.getString("user_image", null)?.let {
            ivProfilePicture.load(Uri.parse(it)) {
                transformations(CircleCropTransformation())
            }
        }

        btnBack.setOnClickListener { finish() }

        btnChangeImage.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        btnSave.setOnClickListener {
            val editor = sharedPref.edit()
            editor.putString("user_name", etFullName.text.toString())
            editor.putString("user_email", etEmail.text.toString())
            editor.putString("user_phone", etPhone.text.toString())

            // Save the selected URI (as a string) to be used by MainActivity
            selectedImageUri?.let { editor.putString("user_image", it.toString()) }

            editor.apply()
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openImagePicker() {
        try {
            // Intent for Gallery
            val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }

            // Intent for Camera
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            // Setup local file for camera to save into
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir != null && !storageDir.exists()) storageDir.mkdirs()
            val file = File(storageDir, "IMG_PROFILE.jpg")

            // Hardcode authority to match Manifest for 100% stability
            cameraImageUri = FileProvider.getUriForFile(
                this,
                "com.talha.rupiahkharch.fileprovider",
                file
            )

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            // Combine both into a Chooser
            val chooser = Intent.createChooser(galleryIntent, "Select Image Source")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            pickerLauncher.launch(chooser)

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}