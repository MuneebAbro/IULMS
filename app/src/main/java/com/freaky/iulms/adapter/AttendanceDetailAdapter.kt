package com.freaky.iulms.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.model.AttendanceDetailItem

class AttendanceDetailAdapter(private val items: List<AttendanceDetailItem>) : RecyclerView.Adapter<AttendanceDetailAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_detail_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lectureNumberTextView: TextView = itemView.findViewById(R.id.lecture_number_textview)
        private val session1StatusTextView: TextView = itemView.findViewById(R.id.session1_status_textview)
        private val session2StatusTextView: TextView = itemView.findViewById(R.id.session2_status_textview)

        fun bind(item: AttendanceDetailItem) {
            lectureNumberTextView.text = "Lecture ${item.lectureNumber}"
            session1StatusTextView.text = item.session1Status
            session2StatusTextView.text = item.session2Status

            // Color-code the status fields
            listOf(session1StatusTextView, session2StatusTextView).forEach { textView ->
                when (textView.text.toString().toUpperCase()) {
                    "P" -> textView.setTextColor(Color.parseColor("#006400")) // Dark Green
                    "A" -> textView.setTextColor(Color.RED)
                    else -> textView.setTextColor(Color.GRAY)
                }
            }
        }
    }
}