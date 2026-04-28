package com.talha.rupiahkharch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64 // Added for Base64 decoding
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
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isDark = sharedPrefs.getBoolean("dark_mode", false)

        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rlEditProfile = findViewById<RelativeLayout>(R.id.rlEditProfile)
        val themeSwitch = findViewById<MaterialSwitch>(R.id.theme_switch)

        tvName = findViewById(R.id.tvProfileName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        tvPhone = findViewById(R.id.tvProfilePhone)

        loadUserProfile()

        btnBack.setOnClickListener { finish() }

        rlEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

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

    // UPDATED: Now handles Base64 strings and standard URIs
    private fun loadUserProfile() {
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)

        val name = sharedPref.getString("user_name", "Your Name")
        val email = sharedPref.getString("user_email", "rupiakharch@gmail.com")
        val phone = sharedPref.getString("user_phone", "+92 324830391")
        val savedImageData = sharedPref.getString("user_image", null)

        tvName.text = name
        tvEmail.text = email
        tvPhone.text = phone

        val ivProfile = findViewById<ImageView>(R.id.ivProfile)

        if (savedImageData != null) {
            // Check if the string is a Base64 string (usually very long)
            if (savedImageData.length > 200) {
                try {
                    val imageBytes = Base64.decode(savedImageData, Base64.DEFAULT)
                    ivProfile.load(imageBytes) {
                        placeholder(R.drawable.face)
                        error(R.drawable.face)
                        transformations(CircleCropTransformation())
                    }
                } catch (e: Exception) {
                    // If decoding fails, try loading it as a URI anyway
                    ivProfile.load(savedImageData) {
                        placeholder(R.drawable.face)
                        error(R.drawable.face)
                        transformations(CircleCropTransformation())
                    }
                }
            } else {
                // It's a normal URI path
                ivProfile.load(savedImageData) {
                    placeholder(R.drawable.face)
                    error(R.drawable.face)
                    transformations(CircleCropTransformation())
                }
            }
        } else {
            // No image saved yet
            ivProfile.load(R.drawable.face) {
                transformations(CircleCropTransformation())
            }
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