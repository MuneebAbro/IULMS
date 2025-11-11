package com.freaky.iulms

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Activity4 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_4)

        val textView = findViewById<TextView>(R.id.raw_data_textview)
        textView.movementMethod = ScrollingMovementMethod()

        val rawData = intent.getStringExtra("RAW_DATA")
        textView.text = rawData ?: "No data received."
    }
}