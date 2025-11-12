package com.freaky.iulms.model

data class ExamResultItem(
    val subjectName: String,
    val midterm: String,
    val quizzesAndAssignments: String,
    val project: String,
    val final: String,
    val total: String,
    val grade: String,
    val points: String
)