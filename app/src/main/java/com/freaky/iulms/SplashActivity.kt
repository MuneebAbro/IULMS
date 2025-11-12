package com.freaky.iulms

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        lifecycleScope.launch {
            delay(1500) // Keep splash screen visible for 1.5 seconds
            // Directly navigate to LoginActivity, which will handle the logic.
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }
    }
}