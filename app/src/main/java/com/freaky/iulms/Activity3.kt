package com.freaky.iulms

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.adapter.AttendanceAdapter
import com.freaky.iulms.parser.HtmlParserUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Activity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_3)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val rawData = intent.getStringExtra("RAW_DATA")

        if (rawData.isNullOrEmpty() || rawData.startsWith("Error:")) {
            Toast.makeText(this, rawData ?: "No data received.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val attendanceItems = withContext(Dispatchers.IO) {
                HtmlParserUtils.parseAttendance(rawData)
            }

            if (attendanceItems.isNotEmpty()) {
                Log.d("Activity3", "Parsed ${attendanceItems.size} attendance items.")
                recyclerView.adapter = AttendanceAdapter(attendanceItems)
            } else {
                Toast.makeText(this@Activity3, "Unable to parse attendance data.", Toast.LENGTH_LONG).show()
            }
        }
    }
}