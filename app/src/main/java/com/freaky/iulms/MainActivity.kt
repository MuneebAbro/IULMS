package com.freaky.iulms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freaky.iulms.auth.AuthManager
import com.freaky.iulms.auth.IULmsDataFetcher
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var fetchedData: Map<String, String>? = null
    private lateinit var mainProgressBar: ProgressBar
    private lateinit var mainGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainProgressBar = findViewById(R.id.main_progress_bar)
        mainGrid = findViewById(R.id.main_grid)

        val cardTranscript = findViewById<MaterialCardView>(R.id.card_transcript)
        val cardExamResult = findViewById<MaterialCardView>(R.id.card_exam_result)
        val cardAttendance = findViewById<MaterialCardView>(R.id.card_attendance)
        val cardSchedule = findViewById<MaterialCardView>(R.id.card_schedule)
        val cardVouchers = findViewById<MaterialCardView>(R.id.card_vouchers)
        val cardExamSchedule = findViewById<MaterialCardView>(R.id.card_exam_schedule)
        val themeButton = findViewById<ImageButton>(R.id.theme_button)
        val logoutButton = findViewById<ImageButton>(R.id.logout_button)

        cardTranscript.setOnClickListener { openActivityWithData(Activity1::class.java, fetchedData?.get("Transcript")) }
        cardExamResult.setOnClickListener { openActivityWithData(Activity2::class.java, fetchedData?.get("ExamResult")) }
        cardAttendance.setOnClickListener { openActivityWithData(Activity3::class.java, fetchedData?.get("Attendance")) }
        cardSchedule.setOnClickListener { openActivityWithData(Activity4::class.java, fetchedData?.get("Schedule")) }
        cardVouchers.setOnClickListener { openActivityWithData(Activity5::class.java, fetchedData?.get("Vouchers")) }
        cardExamSchedule.setOnClickListener { openActivityWithData(Activity6::class.java, fetchedData?.get("ExamSchedule")) }

        themeButton.setOnClickListener {
            Toast.makeText(this, "Theme switching will be implemented soon!", Toast.LENGTH_SHORT).show()
        }

        logoutButton.setOnClickListener {
            logout()
        }

        fetchData()
    }

    private fun fetchData() {
        val client = AuthManager.getAuthenticatedClient()
        if (client == null) {
            Toast.makeText(this, "Error: Not authenticated. Please login again.", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        mainProgressBar.visibility = View.VISIBLE
        mainGrid.visibility = View.GONE

        lifecycleScope.launch {
            val dataFetcher = IULmsDataFetcher(client)
            fetchedData = dataFetcher.fetchAllData()
            mainProgressBar.visibility = View.GONE
            mainGrid.visibility = View.VISIBLE
            Toast.makeText(this@MainActivity, "Data loaded successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        // Clear saved credentials
        val sharedPreferences = getSharedPreferences("IULMS_PREFS", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
        // Go to Login Activity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun openActivityWithData(activityClass: Class<*>, data: String?) {
        if (data != null) {
            val intent = Intent(this, activityClass)
            intent.putExtra("RAW_DATA", data)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Data not available. Please wait or try again.", Toast.LENGTH_SHORT).show()
        }
    }
}