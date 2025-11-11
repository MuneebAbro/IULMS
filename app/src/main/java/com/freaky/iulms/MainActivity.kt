package com.freaky.iulms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freaky.iulms.auth.AuthManager
import com.freaky.iulms.auth.IULmsDataFetcher
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var fetchedData: Map<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val box1 = findViewById<Button>(R.id.box1)
        val box2 = findViewById<Button>(R.id.box2)
        val box3 = findViewById<Button>(R.id.box3)
        val box4 = findViewById<Button>(R.id.box4)
        val box5 = findViewById<Button>(R.id.box5)
        val box6 = findViewById<Button>(R.id.box6)

        box1.setOnClickListener { openActivityWithData(Activity1::class.java, fetchedData?.get("Transcript")) }
        box2.setOnClickListener { openActivityWithData(Activity2::class.java, fetchedData?.get("ExamResult")) }
        box3.setOnClickListener { openActivityWithData(Activity3::class.java, fetchedData?.get("Attendance")) }
        box4.setOnClickListener { openActivityWithData(Activity4::class.java, fetchedData?.get("Schedule")) }
        box5.setOnClickListener { openActivityWithData(Activity5::class.java, fetchedData?.get("Vouchers")) }
        box6.setOnClickListener { openActivityWithData(Activity6::class.java, fetchedData?.get("ExamSchedule")) }

        fetchData()
    }

    private fun fetchData() {
        val client = AuthManager.getAuthenticatedClient()
        if (client == null) {
            Toast.makeText(this, "Error: Not authenticated. Please login again.", Toast.LENGTH_LONG).show()
            // Redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val dataFetcher = IULmsDataFetcher(client)
            fetchedData = dataFetcher.fetchAllData()
            Toast.makeText(this@MainActivity, "All data fetched!", Toast.LENGTH_SHORT).show()
        }
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