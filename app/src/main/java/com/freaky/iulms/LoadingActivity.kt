package com.freaky.iulms

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freaky.iulms.auth.AuthManager
import com.freaky.iulms.auth.IULmsDataFetcher
import kotlinx.coroutines.launch

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val url = intent.getStringExtra("URL_TO_FETCH")
        val destinationClassName = intent.getStringExtra("DESTINATION_ACTIVITY")

        if (url == null || destinationClassName == null) {
            Toast.makeText(this, "Error: Missing required data for loading.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        fetchDataAndLaunch(url, destinationClassName)
    }

    private fun fetchDataAndLaunch(url: String, destinationClassName: String) {
        val client = AuthManager.getAuthenticatedClient()
        if (client == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show()
            // You might want to navigate to LoginActivity here
            finish()
            return
        }

        lifecycleScope.launch {
            val dataFetcher = IULmsDataFetcher(client)
            val html = dataFetcher.fetchSingleUrl(url)

            if (html.startsWith("Error:")) {
                Toast.makeText(this@LoadingActivity, html, Toast.LENGTH_LONG).show()
                finish()
            } else {
                try {
                    val destinationClass = Class.forName(destinationClassName)
                    val intent = Intent(this@LoadingActivity, destinationClass)
                    intent.putExtra("RAW_DATA", html)
                    startActivity(intent)
                    finish() // Finish the loading activity
                } catch (e: ClassNotFoundException) {
                    Toast.makeText(this@LoadingActivity, "Error: Could not find destination activity.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}
