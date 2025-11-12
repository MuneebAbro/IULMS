package com.freaky.iulms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freaky.iulms.auth.AuthManager
import com.freaky.iulms.auth.IULmsAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login_button)
        progressBar = findViewById(R.id.progress_bar)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                loginButton.isEnabled = false

                lifecycleScope.launch {
                    val auth = IULmsAuth()
                    val (success, message) = auth.login("https://iulms.edu.pk/login/index.php", username, password)

                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true

                    Log.d("IULmsAuth", "Login result: $success -> $message")
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()

                    if (success) {
                        // **THE FIX**: Save credentials on successful login
                        saveCredentials(username, password)
                        AuthManager.setAuth(auth)
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finishAffinity() // Finish all activities in the stack
                    }
                }
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveCredentials(username: String, password: String) {
        val sharedPreferences = getSharedPreferences("IULMS_PREFS", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("USERNAME", username)
            putString("PASSWORD", password)
            apply()
        }
    }
}