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
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var ivProfilePicture: ImageView
    private var cameraImageUri: Uri? = null

    // Firebase instances (Notice: Removed FirebaseStorage)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val pickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: cameraImageUri
            uri?.let {
                selectedImageUri = it
                ivProfilePicture.load(it) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openImagePicker() else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
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

        // 1. Load Local Data
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        etFullName.setText(sharedPref.getString("user_name", ""))
        etEmail.setText(sharedPref.getString("user_email", ""))
        etPhone.setText(sharedPref.getString("user_phone", ""))

        // Handle loading the image (could be a local URI or a Base64 string)
        val savedImage = sharedPref.getString("user_image", null)
        if (savedImage != null) {
            if (savedImage.startsWith("data:image") || savedImage.length > 1000) {
                // It's Base64
                val imageBytes = Base64.decode(savedImage, Base64.DEFAULT)
                ivProfilePicture.load(imageBytes) { transformations(CircleCropTransformation()) }
            } else {
                // It's a URI
                ivProfilePicture.load(Uri.parse(savedImage)) { transformations(CircleCropTransformation()) }
            }
        }

        btnBack.setOnClickListener { finish() }
        btnChangeImage.setOnClickListener { checkCameraPermissionAndOpen() }

        btnSave.setOnClickListener {
            val name = etFullName.text.toString()
            val email = etEmail.text.toString()
            val phone = etPhone.text.toString()

            if (name.isEmpty()) {
                etFullName.error = "Name required"
                return@setOnClickListener
            }

            saveProfileToCloud(name, email, phone)
        }
    }

    // NEW: Helper to convert Image to String for Firestore
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveProfileToCloud(name: String, email: String, phone: String) {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userMap = mutableMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "lastUpdated" to System.currentTimeMillis()
                )

                // Convert image to String if a new one was picked
                var imageStringForLocal = ""
                selectedImageUri?.let { uri ->
                    val base64 = uriToBase64(uri)
                    if (base64 != null) {
                        userMap["profileImageBase64"] = base64
                        imageStringForLocal = base64
                    }
                }

                // Save to Firestore (Same place as your expenses!)
                firestore.collection("users").document(userId)
                    .set(userMap, SetOptions.merge())
                    .await()

                withContext(Dispatchers.Main) {
                    // Update local SharedPreferences
                    val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("user_name", name)
                    editor.putString("user_email", email)
                    editor.putString("user_phone", phone)
                    if (imageStringForLocal.isNotEmpty()) {
                        editor.putString("user_image", imageStringForLocal)
                    }
                    editor.apply()

                    Toast.makeText(this@EditProfileActivity, "Profile Synced to Cloud!", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Sync Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
            val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(storageDir, "IMG_PROFILE.jpg")
            cameraImageUri = FileProvider.getUriForFile(this, "com.talha.rupiahkharch.fileprovider", file)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            val chooser = Intent.createChooser(galleryIntent, "Select Image Source")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            pickerLauncher.launch(chooser)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}