package com.talha.rupiahkharch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import coil.load
import coil.transform.CircleCropTransformation

class SettingsActivity : AppCompatActivity() {

    // Using nullable types or checking initialization to avoid crashes
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 1. Initialize Navigation & Theme Views
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rlEditProfile = findViewById<RelativeLayout>(R.id.rlEditProfile)
        val themeSwitch = findViewById<MaterialSwitch>(R.id.theme_switch)

        // 2. Initialize Profile TextViews
        // Ensure these IDs match exactly what you put in activity_settings.xml
        tvName = findViewById(R.id.tvProfileName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        tvPhone = findViewById(R.id.tvProfilePhone)

        // 3. Load initial data immediately
        loadUserProfile()

        // 4. Navigation Logic
        btnBack.setOnClickListener {
            finish()
        }

        rlEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // 5. Dark Mode Logic with Persistance
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isDark = sharedPrefs.getBoolean("dark_mode", false)
        themeSwitch.isChecked = isDark

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            saveThemePreference(isChecked)
        }
    }

    /**
     * onResume is the secret to "Live" updates.
     * When you hit 'Save' in EditProfileActivity and it calls finish(),
     * this Activity comes to the foreground and runs loadUserProfile() again.
     */
    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)

        // 1. Retrieve Values
        val name = sharedPref.getString("user_name", "Sarah Mitchell")
        val email = sharedPref.getString("user_email", "sarah.mitchell@company.com")
        val phone = sharedPref.getString("user_phone", "+1 (555) 248-0391")
        val imageUriString = sharedPref.getString("user_image", null)

        // 2. Update Text UI
        tvName.text = name
        tvEmail.text = email
        tvPhone.text = phone

        // 3. Update Image UI using Coil
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)

        ivProfile.load(imageUriString) {
            // This shows while the image is loading
            placeholder(R.drawable.motorbike)

            // This shows if the URI is broken or image fails to load
            error(R.drawable.motorbike)

            // Adds a nice 300ms fade-in effect
            crossfade(true)
            crossfade(300)

            // Optional: Ensures the image is perfectly circular if your
            // XML ShapeableImageView isn't doing it already
            transformations(CircleCropTransformation())
        }
    }
    private fun saveThemePreference(isDark: Boolean) {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("dark_mode", isDark)
            apply()
        }
    }
}