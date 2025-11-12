package com.freaky.iulms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.model.ExamScheduleItem

class ExamScheduleAdapter(private val items: List<ExamScheduleItem>) : RecyclerView.Adapter<ExamScheduleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reuse the schedule card layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.day_name_textview) // Re-using day_name_textview as date
        private val examsContainer: LinearLayout = itemView.findViewById(R.id.classes_container) // Re-using classes_container for exams

        fun bind(item: ExamScheduleItem) {
            dateTextView.text = item.date

            examsContainer.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)
            for (exam in item.exams) {
                // Reuse the class row layout for exam details
                val examView = inflater.inflate(R.layout.item_exam_row, examsContainer, false)
                val subjectTextView: TextView = examView.findViewById(R.id.subject_textview)
                val courseCodeTextView: TextView = examView.findViewById(R.id.course_code_textview)
                val timeTextView: TextView = examView.findViewById(R.id.time_textview)
                val roomTextView: TextView = examView.findViewById(R.id.room_textview)

                subjectTextView.text = exam.subject
                courseCodeTextView.text = "Course: ${exam.courseCode}"
                timeTextView.text = "Time: ${exam.time}"
                roomTextView.text = "Room: ${exam.room}"

                examsContainer.addView(examView)
            }
        }
    }
}