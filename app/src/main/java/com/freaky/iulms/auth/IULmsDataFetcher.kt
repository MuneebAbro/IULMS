package com.freaky.iulms.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class IULmsDataFetcher(private val client: OkHttpClient) {

    private val urls = mapOf(
        "Transcript" to "https://iulms.edu.pk/sic/Transcript.php",
        "ExamResult" to "https://iulms.edu.pk/sic/examresult.php",
        "Attendance" to "https://iulms.edu.pk/sic/StudentAttendance.php",
        "Schedule" to "https://iulms.edu.pk/sic/Schedule.php",
        "Vouchers" to "https://iulms.edu.pk/sic/Vouchers.php",
        "ExamSchedule" to "https://iulms.edu.pk/sic/examschedule.php"
    )

    suspend fun fetchAllData(): Map<String, String> = withContext(Dispatchers.IO) {
        coroutineScope {
            val results = urls.map { (name, url) ->
                async {
                    name to fetchDataForUrl(name, url)
                }
            }.map { it.await() }
            results.toMap()
        }
    }

    private suspend fun fetchDataForUrl(name: String, url: String): String {
        return try {
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d("IULmsDataFetcher", "Fetched $name: ${body.length} chars")
                body
            } else {
                Log.e("IULmsDataFetcher", "Failed to fetch $name. Code: ${response.code}")
                "Error: Failed to load content. Status code: ${response.code}"
            }
        } catch (e: IOException) {
            Log.e("IULmsDataFetcher", "Error fetching $name", e)
            "Error: Network request failed. ${e.message}"
        }
    }
}