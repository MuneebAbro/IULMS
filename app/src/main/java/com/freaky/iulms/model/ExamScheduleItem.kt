package com.freaky.iulms.model

data class ExamScheduleItem(
    val date: String,
    val exams: List<ExamDetail>
) {
    data class ExamDetail(
        val subject: String,
        val courseCode: String,
        val time: String,
        val room: String
    )
}