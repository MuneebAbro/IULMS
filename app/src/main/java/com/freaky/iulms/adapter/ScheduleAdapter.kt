package com.freaky.iulms.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.model.ScheduleItem
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(private val items: List<ScheduleItem>) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayNameTextView: TextView = itemView.findViewById(R.id.day_name_textview)
        private val classesContainer: LinearLayout = itemView.findViewById(R.id.classes_container)
        private val cardView: CardView = itemView.findViewById(R.id.schedule_card)

        fun bind(item: ScheduleItem) {
            dayNameTextView.text = item.day

            // Highlight current day
            val currentDay = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
            if (item.day.equals(currentDay, ignoreCase = true)) {
                cardView.setCardBackgroundColor(Color.parseColor("#E0F7FA")) // A light cyan color
            }

            classesContainer.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)
            for (classDetail in item.classes) {
                val classView = inflater.inflate(R.layout.item_class_row, classesContainer, false)
                val subjectTextView: TextView = classView.findViewById(R.id.subject_textview)
                val teacherTextView: TextView = classView.findViewById(R.id.teacher_textview)
                val roomTextView: TextView = classView.findViewById(R.id.room_textview)
                val timeTextView: TextView = classView.findViewById(R.id.time_textview)
                val courseCodeTextView: TextView = classView.findViewById(R.id.course_code_textview)
                val edpCodeTextView: TextView = classView.findViewById(R.id.edp_code_textview)

                subjectTextView.text = classDetail.subject
                teacherTextView.text = "Teacher: ${classDetail.teacher}"
                roomTextView.text = "Room: ${classDetail.room}"
                timeTextView.text = "Time: ${classDetail.time}"
                courseCodeTextView.text = "Course: ${classDetail.courseCode}"
                edpCodeTextView.text = "EDP: ${classDetail.edpCode}"

                classesContainer.addView(classView)
            }
        }
    }
}