package com.freaky.iulms

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.adapter.ExamResultAdapter
import com.freaky.iulms.parser.HtmlParserUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Activity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val rawData = intent.getStringExtra("RAW_DATA")

        if (rawData.isNullOrEmpty() || rawData.startsWith("Error:")) {
            Toast.makeText(this, rawData ?: "No data received.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val examResultItems = withContext(Dispatchers.IO) {
                HtmlParserUtils.parseExamResult(rawData)
            }

            if (examResultItems.isNotEmpty()) {
                Log.d("Activity2", "Parsed ${examResultItems.size} exam result items.")
                recyclerView.adapter = ExamResultAdapter(examResultItems)
            } else {
                Toast.makeText(this@Activity2, "Unable to parse exam result data.", Toast.LENGTH_LONG).show()
            }
        }
    }
}