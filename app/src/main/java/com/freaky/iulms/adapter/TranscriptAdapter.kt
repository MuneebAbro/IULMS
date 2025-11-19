package com.freaky.iulms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.model.TranscriptItem

class TranscriptAdapter(private val items: List<TranscriptItem>) : RecyclerView.Adapter<TranscriptAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transcript_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val semesterNameTextView: TextView = itemView.findViewById(R.id.semester_name_textview)
        private val subjectsContainer: LinearLayout = itemView.findViewById(R.id.subjects_container)
        private val gpaTextView: TextView = itemView.findViewById(R.id.gpa_textview)


        fun bind(item: TranscriptItem) {
            semesterNameTextView.text = item.semester
            gpaTextView.text = "GPA: ${item.gpa}"

            // Dynamically add subject rows
            subjectsContainer.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)
            for (subject in item.subjects) {
                val subjectView = inflater.inflate(R.layout.item_subject_row, subjectsContainer, false)
                val codeTextView: TextView = subjectView.findViewById(R.id.subject_code_textview)
                val titleTextView: TextView = subjectView.findViewById(R.id.subject_title_textview)
                val gradeTextView: TextView = subjectView.findViewById(R.id.subject_grade_textview)
                val creditsTextView: TextView = subjectView.findViewById(R.id.subject_credits_textview)

                codeTextView.text = subject.code
                titleTextView.text = subject.title
                gradeTextView.text = subject.grade
                creditsTextView.text = subject.creditHours

                subjectsContainer.addView(subjectView)
            }
        }
    }
}