package com.freaky.iulms

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.adapter.ExamScheduleAdapter
import com.freaky.iulms.parser.HtmlParserUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Activity6 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // THE FIX: Use the correct layout file
        setContentView(R.layout.activity_6)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val messageTextView = findViewById<TextView>(R.id.message_textview)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val backButton = findViewById<LinearLayout>(R.id.back_container)
        val backButton2 = findViewById<ImageButton>(R.id.back_button)

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        backButton2.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        val rawData = intent.getStringExtra("RAW_DATA")

        if (rawData.isNullOrEmpty() || rawData.startsWith("Error:")) {
            Toast.makeText(this, rawData ?: "No data received.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val examScheduleItems = withContext(Dispatchers.IO) {
                HtmlParserUtils.parseExamSchedule(rawData)
            }

            if (examScheduleItems.isNotEmpty()) {
                Log.d("Activity6", "Parsed ${examScheduleItems.size} exam schedule items.")
                recyclerView.adapter = ExamScheduleAdapter(examScheduleItems)
                recyclerView.isVisible = true
                messageTextView.isVisible = false
            } else {
                messageTextView.text = "Exam schedule is not available at the moment."
                recyclerView.isVisible = false
                messageTextView.isVisible = true
            }
        }
    }
}