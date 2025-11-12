package com.freaky.iulms.parser

import android.util.Log
import com.freaky.iulms.model.AttendanceDetailItem
import com.freaky.iulms.model.AttendanceItem
import com.freaky.iulms.model.ExamResultItem
import com.freaky.iulms.model.ExamScheduleItem
import com.freaky.iulms.model.ScheduleItem
import com.freaky.iulms.model.TranscriptItem
import com.freaky.iulms.model.VoucherItem
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object HtmlParserUtils {

    fun parseTranscript(json: String): List<TranscriptItem> {
        val items = mutableListOf<TranscriptItem>()
        try {
            val root = JSONObject(json)
            val coursesArray = root.getJSONArray("attemptedCourses")

            val coursesBySemester = (0 until coursesArray.length())
                .map { coursesArray.getJSONObject(it) }
                .groupBy { it.getInt("semNo") }

            var regularSemesterCount = 1
            for ((semNo, courses) in coursesBySemester.toSortedMap()) {
                var semesterGpa = 0.0
                var semesterCredits = 0.0
                var totalCreditsForSemester = 0.0

                val subjects = courses.mapNotNull { courseJson ->
                    val grade = courseJson.getString("crsGrade")
                    val credits = courseJson.getString("crsHours").toDoubleOrNull() ?: 0.0
                    totalCreditsForSemester += credits

                    val gpaPoints = courseJson.optDouble("gpa", 0.0)
                    if (grade != "I" && grade != "W" && grade != "PASS") {
                        semesterGpa += gpaPoints * credits
                        semesterCredits += credits
                    }

                    TranscriptItem.Subject(
                        code = courseJson.getString("crsCode"),
                        title = courseJson.getString("crsTitle"),
                        grade = grade,
                        creditHours = credits.toString()
                    )
                }

                val finalSemesterGpa = if (semesterCredits > 0) String.format("%.2f", semesterGpa / semesterCredits) else "0.00"
                val overallCgpa = root.optString("cgpa", "N/A")

                val semesterName = if (totalCreditsForSemester < 9) {
                    "Summer Semester"
                } else {
                    "Semester ${regularSemesterCount++}"
                }

                items.add(TranscriptItem(semesterName, subjects, finalSemesterGpa, overallCgpa))
            }

        } catch (e: Exception) {
            Log.e("HtmlParserUtils", "Failed to parse transcript JSON", e)
        }

        if (items.isEmpty()) {
            Log.d("TranscriptJSON", "Parsing finished with 0 items. JSON start: ${json.take(500)}")
        }

        return items
    }

    fun parseExamResult(html: String): Pair<List<ExamResultItem>, String> {
        val items = mutableListOf<ExamResultItem>()
        var gpa = ""
        try {
            val doc: Document = Jsoup.parse(html)
            val table = doc.select("table.tblAttendance").first()

            if (table == null) {
                Log.w("HtmlParserUtils", "Exam result table with class 'tblAttendance' not found.")
                return Pair(emptyList(), gpa)
            }

            val rows = table.select("tr")
            for (row in rows.drop(1)) { // Skip header
                val cols = row.select("td")
                if (cols.size >= 8) {
                    try {
                        items.add(ExamResultItem(
                            subjectName = cols[0].text().trim(),
                            midterm = cols[1].text().trim(),
                            quizzesAndAssignments = cols[2].text().trim(),
                            project = cols[3].text().trim(),
                            final = cols[4].text().trim(),
                            total = cols[5].text().trim(),
                            grade = cols[6].text().trim(),
                            points = cols[7].text().trim()
                        ))
                    } catch (e: Exception) {
                        Log.e("HtmlParserUtils", "Failed to parse exam result row: ${row.text()}", e)
                    }
                } else if (cols.size == 1 && cols.text().contains("GPA:")) {
                    gpa = cols.text().trim()
                }
            }
        } catch (e: Exception) {
            Log.e("HtmlParserUtils", "Failed to parse exam result HTML", e)
        }

        if (items.isEmpty()) {
            Log.d("ExamResultHTML", "Parsing exam results finished with 0 items. HTML start: ${html.take(500)}")
        }

        return Pair(items, gpa)
    }

    fun parseAttendance(html: String): List<AttendanceItem> {
        val items = mutableListOf<AttendanceItem>()
        try {
            val doc: Document = Jsoup.parse(html)

            val summaryTable = doc.select("table.attendance-table").first() ?: return emptyList()
            val summaryRows = summaryTable.select("tr.attendanceRow")

            val detailModals = doc.select("div.modal")

            for ((index, summaryRow) in summaryRows.withIndex()) {
                val summaryCols = summaryRow.select("td")
                if (summaryCols.size < 4) continue

                val courseName = summaryCols[0].text().trim()
                val total = summaryCols[1].text().toIntOrNull() ?: 0
                val present = summaryCols[2].text().toIntOrNull() ?: 0
                val percentage = if (total > 0) (present.toDouble() / total) * 100 else 0.0

                val detailList = mutableListOf<AttendanceDetailItem>()
                if (index < detailModals.size) {
                    val detailTable = detailModals[index].select("table.attendance-table").first()
                    val detailRows = detailTable?.select("tr.attendanceRow")

                    detailRows?.forEach {
                        val detailCols = it.select("td")
                        if (detailCols.size >= 3) {
                            detailList.add(
                                AttendanceDetailItem(
                                lectureNumber = detailCols[0].text().replace(".", ""),
                                session1Status = detailCols[1].text(),
                                session2Status = detailCols[2].text()
                            ))
                        }
                    }
                }

                items.add(AttendanceItem(courseName, total, present, percentage, detailList))
            }
        } catch (e: Exception) {
            Log.e("HtmlParserUtils", "Failed to parse attendance HTML", e)
        }

        if (items.isEmpty()) {
            Log.d("AttendanceHTML", "Parsing attendance finished with 0 items. HTML start: ${html.take(500)}")
        }

        return items
    }

   fun parseSchedule(html: String): List<ScheduleItem> {
        val allClassDetails = mutableListOf<TempScheduleItem>()
        try {
            val doc = Jsoup.parse(html)
            val rows = doc.select("center > table > tbody > tr > td > table > tbody > tr")

            for (row in rows) {
                val dayCell = row.select("td.dateStyle").first()
                val detailsCell = row.select("td.detailsStyle").first()

                if (dayCell == null || detailsCell == null) continue

                val day = dayCell.select("span.dayStyle").text().trim()
                val time = dayCell.select("td[style=text-align:center]").last()?.text()?.trim() ?: ""

                val courseTitle = detailsCell.select("tr:contains(Course Title)").text().substringAfter(":").trim()
                val faculty = detailsCell.select("tr:contains(Faculty)").text().substringAfter(":").trim()
                val location = detailsCell.select("tr:contains(Location)").text().substringAfter(":").trim()

                val codeHtml = detailsCell.select("tr:contains(EDP Code)").html()
                val codes = Jsoup.parse(codeHtml).text().split("Course Code :").map { it.replace("EDP Code :", "").trim() }
                val edpCode = codes.getOrNull(0) ?: ""
                val courseCode = codes.getOrNull(1) ?: ""

                if (day.isNotEmpty() && courseTitle.isNotEmpty()) {
                    allClassDetails.add(TempScheduleItem(day, ScheduleItem.ClassDetail(
                        subject = courseTitle,
                        teacher = faculty,
                        room = location,
                        time = time,
                        courseCode = courseCode,
                        edpCode = edpCode
                    )))
                }
            }
        } catch (e: Exception) {
            Log.e("HtmlParserUtils", "Error parsing schedule HTML", e)
        }

        if (allClassDetails.isEmpty()) {
            Log.d("ScheduleHTML", "Parsing finished with 0 items. HTML start: ${html.take(500)}")
        }

        return allClassDetails.groupBy { it.day }
            .map { (day, tempItems) ->
                ScheduleItem(day, tempItems.map { it.detail })
            }
            .sortedBy { 
                when (it.day.toUpperCase()) {
                    "MON" -> 1
                    "TUE" -> 2
                    "WED" -> 3
                    "THU" -> 4
                    "FRI" -> 5
                    "SAT" -> 6
                    "SUN" -> 7
                    else -> 8
                }
            }
    }

    private data class TempScheduleItem(val day: String, val detail: ScheduleItem.ClassDetail)

    fun parseVouchers(html: String): List<VoucherItem> {
        val items = mutableListOf<VoucherItem>()
        try {
            val doc: Document = Jsoup.parse(html)
            val voucherTable = doc.select("table#voucherTable").first()

            if (voucherTable == null) {
                if (doc.text().contains("No records found")) {
                } else {
                    Log.w("HtmlParserUtils", "Voucher table not found and 'No records' message absent.")
                }
                return emptyList()
            }

            val rows = voucherTable.select("tbody tr")
            for (row in rows) {
                val cols = row.select("td")
                if (cols.size >= 8) {
                    val voucherNumber = cols[1].text()
                    val semester = cols[2].text()
                    val dueDate = cols[3].text()
                    val installment = cols[4].text()
                    val description = cols[5].text()
                    val amount = cols[6].text()
                    val isLate = row.hasClass("latePayment")

                    val form = row.select("form").first()
                    val printableVoucher = form?.select("input[name=VoucherNumber]")?.attr("value") ?: ""
                    val printableStudentId = form?.select("input[name=studentId]")?.attr("value") ?: ""

                    items.add(VoucherItem(
                        voucherNumber = voucherNumber,
                        semester = semester,
                        dueDate = dueDate,
                        installmentNumber = installment,
                        description = description,
                        amount = amount,
                        isLate = isLate,
                        printableVoucherNumber = printableVoucher,
                        printableStudentId = printableStudentId
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("HtmlParserUtils", "Failed to parse vouchers HTML", e)
        }

        if (items.isEmpty() && !html.contains("No records found")) {
            Log.d("VoucherHTML", "Parsing vouchers finished with 0 items. HTML start: ${html.take(500)}")
        }

        return items
    }

    fun parseExamSchedule(html: String): List<ExamScheduleItem> {
        val items = mutableListOf<ExamScheduleItem>()
        try {
            val doc: Document = Jsoup.parse(html)

            if (doc.text().contains("This portion of the website is not available at the moment.", ignoreCase = true)) {
                Log.d("HtmlParserUtils", "Exam schedule is not available.")
                return emptyList()
            }

            val tables = doc.select("table")

            for (table in tables) {
                val dateStr = table.select("th").first()?.text()?.split("(")?.first()?.trim() ?: ""
                if (dateStr.isEmpty()) continue

                val exams = mutableListOf<ExamScheduleItem.ExamDetail>()
                val rows = table.select("tbody tr")

                for (row in rows) {
                    val cols = row.select("td")
                    if (cols.size >= 4) {
                        exams.add(ExamScheduleItem.ExamDetail(
                            subject = cols[0].text(),
                            courseCode = cols[1].text(),
                            time = cols[2].text(),
                            room = cols[3].text()
                        ))
                    }
                }
                if (exams.isNotEmpty()) {
                    items.add(ExamScheduleItem(dateStr, exams))
                }
            }
        } catch (e: Exception) {
            Log.e("HtmlParserUtils", "Failed to parse exam schedule HTML", e)
        }

        if (items.isEmpty() && !html.contains("not available at the moment")) {
            Log.d("ExamScheduleHTML", "Parsing exam schedule finished with 0 items. HTML start: ${html.take(500)}")
        }

        return items
    }
}