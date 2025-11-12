package com.freaky.iulms.model

data class TranscriptItem(
    val semester: String,
    val subjects: List<Subject>,
    val gpa: String,
    val cgpa: String
) {
    data class Subject(
        val code: String,
        val title: String,
        val grade: String,
        val creditHours: String
    )
}