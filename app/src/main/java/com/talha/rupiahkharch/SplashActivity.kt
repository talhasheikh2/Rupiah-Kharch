package com.talha.rupiahkharch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1. Find the views
        val cvCircle = findViewById<CardView>(R.id.cvCircle)
        val ivLogo = findViewById<ImageView>(R.id.ivLogoInside)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)

        // 2. Load the animations you created in the anim folder
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade)
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 2000

        // 3. Start the Circle Animation immediately
        cvCircle.startAnimation(slideDown)

        // 4. Sequence Logic: Wait for Circle to finish, then start the Logo
        slideDown.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Now that the circle is in place, bring the logo up from the bottom
                ivLogo.visibility = View.VISIBLE
                ivLogo.startAnimation(slideUp)

                // Also fade in the text for a professional look
                tvAppName.visibility = View.VISIBLE
                tvAppName.startAnimation(fadeIn)
                tvTagline.visibility = View.VISIBLE
                tvTagline.startAnimation(fadeIn)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // 5. Move to MainActivity after the animations are done (4 seconds total)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 4000)
    }
}