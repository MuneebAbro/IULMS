package com.freaky.iulms.model

data class AttendanceItem(
    val courseName: String,
    val totalClasses: Int,
    val attendedClasses: Int,
    val percentage: Double,
    val details: List<AttendanceDetailItem>
)