package com.freaky.iulms.adapter

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.model.AttendanceItem

class AttendanceAdapter(private val items: List<AttendanceItem>) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val courseNameTextView: TextView = itemView.findViewById(R.id.course_name_textview)
        private val totalClassesTextView: TextView = itemView.findViewById(R.id.total_classes_textview)
        private val attendedClassesTextView: TextView = itemView.findViewById(R.id.attended_classes_textview)
        private val percentageTextView: TextView = itemView.findViewById(R.id.percentage_textview)

        fun bind(item: AttendanceItem) {
            courseNameTextView.text = item.courseName
            totalClassesTextView.text = "Total: ${item.totalClasses}"
            attendedClassesTextView.text = "Attended: ${item.attendedClasses}"
            percentageTextView.text = String.format("%.2f%%", item.percentage)

            // Set text color based on attendance percentage
            if (item.percentage < 75) {
                percentageTextView.setTextColor(Color.RED)
            } else {
                percentageTextView.setTextColor(Color.parseColor("#006400")) // Dark Green
            }

            // Set click listener to show details dialog
            itemView.setOnClickListener {
                if (item.details.isNotEmpty()) {
                    showDetailDialog(item)
                }
            }
        }

        private fun showDetailDialog(item: AttendanceItem) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_attendance_detail, null)

            val titleTextView: TextView = dialogView.findViewById(R.id.dialog_title_textview)
            val detailRecyclerView: RecyclerView = dialogView.findViewById(R.id.detail_recycler_view)

            titleTextView.text = item.courseName
            detailRecyclerView.layoutManager = LinearLayoutManager(context)
            detailRecyclerView.adapter = AttendanceDetailAdapter(item.details)

            val dialog = AlertDialog.Builder(
                ContextThemeWrapper(
                    context,
                    androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert
                )
            )
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create()

            dialog.show()

// Force white background in case dark theme tries to override it
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

        }
    }
}