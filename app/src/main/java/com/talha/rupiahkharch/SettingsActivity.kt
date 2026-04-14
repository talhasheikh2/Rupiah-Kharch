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

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // --- PERSISTENCE LOGIC START ---
        // Load preference BEFORE super.onCreate to ensure the theme is applied correctly on startup
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isDark = sharedPrefs.getBoolean("dark_mode", false)

        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        // --- PERSISTENCE LOGIC END ---

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 1. Initialize Navigation & Theme Views
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rlEditProfile = findViewById<RelativeLayout>(R.id.rlEditProfile)
        val themeSwitch = findViewById<MaterialSwitch>(R.id.theme_switch)

        // 2. Initialize Profile TextViews
        tvName = findViewById(R.id.tvProfileName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        tvPhone = findViewById(R.id.tvProfilePhone)

        // 3. Load initial data
        loadUserProfile()

        // 4. Navigation Logic
        btnBack.setOnClickListener {
            finish()
        }

        rlEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // 5. Update Switch UI state
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

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)

        val name = sharedPref.getString("user_name", "Your Name")
        val email = sharedPref.getString("user_email", "rupiakharch@gmail.com")
        val phone = sharedPref.getString("user_phone", "+92 324830391")
        val imageUriString = sharedPref.getString("user_image", null)

        tvName.text = name
        tvEmail.text = email
        tvPhone.text = phone

        val ivProfile = findViewById<ImageView>(R.id.ivProfile)

        ivProfile.load(imageUriString) {
            placeholder(R.drawable.face) // Changed from placeholder to motorbike as per your code
            error(R.drawable.face)
            crossfade(true)
            crossfade(300)
            transformations(CircleCropTransformation())
        }
    }

    private fun saveThemePreference(isDark: Boolean) {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("dark_mode", isDark)
            apply() // apply() is asynchronous and safe for UI thread
        }
    }
}