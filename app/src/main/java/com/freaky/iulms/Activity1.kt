package com.freaky.iulms

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.adapter.TranscriptAdapter
import com.freaky.iulms.parser.HtmlParserUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Activity1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_1)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val backButton = findViewById<LinearLayout>(R.id.back_container)
        val backButton2 = findViewById<ImageButton>(R.id.back_button)
        // handle click
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()  // this safely handles back navigation
        }
        backButton2.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()  // this safely handles back navigation
        }
        val rawData = intent.getStringExtra("RAW_DATA")

        if (rawData.isNullOrEmpty() || rawData.startsWith("Error:")) {
            Toast.makeText(this, rawData ?: "No data received.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val transcriptItems = withContext(Dispatchers.IO) {
                HtmlParserUtils.parseTranscript(rawData)
            }

            if (transcriptItems.isNotEmpty()) {
                Log.d("Activity1", "Parsed ${transcriptItems.size} transcript items.")
                recyclerView.adapter = TranscriptAdapter(transcriptItems)
            } else {
                Toast.makeText(this@Activity1, "Unable to parse transcript data.", Toast.LENGTH_LONG).show()
            }
        }
    }
}