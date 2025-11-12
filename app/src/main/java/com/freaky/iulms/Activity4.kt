package com.freaky.iulms

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.adapter.ScheduleAdapter
import com.freaky.iulms.parser.HtmlParserUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Activity4 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_4)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val rawData = intent.getStringExtra("RAW_DATA")

        if (rawData.isNullOrEmpty() || rawData.startsWith("Error:")) {
            Toast.makeText(this, rawData ?: "No data received.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val scheduleItems = withContext(Dispatchers.IO) {
                HtmlParserUtils.parseSchedule(rawData)
            }

            if (scheduleItems.isNotEmpty()) {
                Log.d("Activity4", "Parsed ${scheduleItems.size} schedule items.")
                recyclerView.adapter = ScheduleAdapter(scheduleItems)
            } else {
                Toast.makeText(this@Activity4, "Unable to parse schedule data.", Toast.LENGTH_LONG).show()
            }
        }
    }
}