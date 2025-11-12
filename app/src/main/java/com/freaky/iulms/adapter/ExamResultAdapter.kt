package com.freaky.iulms.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.model.ExamResultItem

class ExamResultAdapter(private val items: List<ExamResultItem>) : RecyclerView.Adapter<ExamResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exam_result_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subjectNameTextView: TextView = itemView.findViewById(R.id.subject_name_textview)
        private val midtermTextView: TextView = itemView.findViewById(R.id.midterm_textview)
        private val quizzesTextView: TextView = itemView.findViewById(R.id.quizzes_textview)
        private val projectTextView: TextView = itemView.findViewById(R.id.project_textview)
        private val finalTextView: TextView = itemView.findViewById(R.id.final_textview)
        private val totalTextView: TextView = itemView.findViewById(R.id.total_textview)
        private val gradeTextView: TextView = itemView.findViewById(R.id.grade_textview)
        private val pointsTextView: TextView = itemView.findViewById(R.id.points_textview)

        fun bind(item: ExamResultItem) {
            subjectNameTextView.text = item.subjectName
            midtermTextView.text = "Mid:\n${item.midterm}"
            quizzesTextView.text = "Quiz/Ass:\n${item.quizzesAndAssignments}"
            projectTextView.text = "Project:\n${item.project}"
            finalTextView.text = "Final:\n${item.final}"
            totalTextView.text = item.total
            gradeTextView.text = item.grade
            pointsTextView.text = item.points

            // Set text color based on grade
            when (item.grade.firstOrNull()?.toUpperCase()) {
                'A' -> gradeTextView.setTextColor(Color.parseColor("#006400")) // Dark Green
                'B' -> gradeTextView.setTextColor(Color.parseColor("#108910")) // Lighter Green
                'C' -> gradeTextView.setTextColor(Color.parseColor("#FFA500")) // Orange
                'D' -> gradeTextView.setTextColor(Color.parseColor("#FF8C00")) // Dark Orange
                'F' -> gradeTextView.setTextColor(Color.RED)
                else -> gradeTextView.setTextColor(Color.BLACK)
            }
        }
    }
}