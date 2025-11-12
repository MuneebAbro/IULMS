package com.freaky.iulms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freaky.iulms.auth.AuthManager
import com.freaky.iulms.auth.IULmsAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(1500) // Keep splash screen visible for 1.5 seconds
            checkForSavedCredentials()
        }
    }

    private fun checkForSavedCredentials() {
        val sharedPreferences = getSharedPreferences("IULMS_PREFS", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("USERNAME", null)
        val password = sharedPreferences.getString("PASSWORD", null)

        if (username != null && password != null) {
            lifecycleScope.launch {
                val auth = IULmsAuth()
                val (success, _) = auth.login("https://iulms.edu.pk/login/index.php", username, password)
                if (success) {
                    AuthManager.setAuth(auth)
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }
            }
        } else {
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }
    }
}