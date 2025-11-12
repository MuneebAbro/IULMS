package com.freaky.iulms.model

data class ScheduleItem(
    val day: String,
    val classes: List<ClassDetail>
) {
    data class ClassDetail(
        val subject: String,
        val teacher: String,
        val room: String,
        val time: String,
        val courseCode: String,
        val edpCode: String
    )
}