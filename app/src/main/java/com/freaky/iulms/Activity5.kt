package com.freaky.iulms

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.adapter.VoucherAdapter
import com.freaky.iulms.parser.HtmlParserUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Activity5 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use a different layout that can show a message.
        setContentView(R.layout.activity_generic_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val messageTextView = findViewById<TextView>(R.id.message_textview)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val rawData = intent.getStringExtra("RAW_DATA")

        if (rawData.isNullOrEmpty() || rawData.startsWith("Error:")) {
            Toast.makeText(this, rawData ?: "No data received.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val voucherItems = withContext(Dispatchers.IO) {
                HtmlParserUtils.parseVouchers(rawData)
            }

            if (voucherItems.isNotEmpty()) {
                Log.d("Activity5", "Parsed ${voucherItems.size} voucher items.")
                recyclerView.adapter = VoucherAdapter(voucherItems)
                recyclerView.isVisible = true
                messageTextView.isVisible = false
            } else {
                // If the parser returns an empty list, it means no vouchers were found.
                messageTextView.text = "All Fees Paid! No outstanding vouchers found."
                recyclerView.isVisible = false
                messageTextView.isVisible = true
            }
        }
    }
}