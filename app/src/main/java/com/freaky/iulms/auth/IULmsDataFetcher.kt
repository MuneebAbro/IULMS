package com.freaky.iulms.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
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
                    val content = if (name == "Transcript") {
                        fetchTranscriptJson(url)
                    } else {
                        fetchGenericData(name, url)
                    }
                    name to content
                }
            }.map { it.await() }
            results.toMap()
        }
    }

    private fun fetchTranscriptJson(transcriptPageUrl: String): String {
        try {
            // 1. GET the initial transcript page to extract the degreeId
            val getRequest = Request.Builder().url(transcriptPageUrl).header("User-Agent", "Mozilla/5.0").build()
            val getResponse = client.newCall(getRequest).execute()
            if (!getResponse.isSuccessful) return "Error: Failed to fetch transcript page (Code: ${getResponse.code})"

            val html = getResponse.body?.string() ?: return "Error: Transcript page body is empty."
            val doc = Jsoup.parse(html)
            val degreeId = doc.select("#cmbDegree option").first()?.attr("value")

            if (degreeId.isNullOrEmpty()) {
                return "Error: Could not find degreeId on transcript page."
            }

            // 2. POST to the data service to get the actual transcript data as JSON
            val dataServiceUrl = "https://iulms.edu.pk/sic/SICDataService.php"
            val formBody = FormBody.Builder()
                .add("action", "GetTranscript")
                .add("degreeId", degreeId)
                .build()

            val postRequest = Request.Builder().url(dataServiceUrl).post(formBody).build()
            val postResponse = client.newCall(postRequest).execute()

            return if (postResponse.isSuccessful) {
                val jsonBody = postResponse.body?.string() ?: ""
                Log.d("IULmsDataFetcher", "Fetched Transcript JSON: ${jsonBody.length} chars")
                jsonBody
            } else {
                "Error: Failed to fetch transcript JSON data. Code: ${postResponse.code}"
            }

        } catch (e: IOException) {
            Log.e("IULmsDataFetcher", "Error fetching Transcript JSON", e)
            return "Error: Network request failed for transcript. ${e.message}"
        }
    }

    private fun fetchGenericData(name: String, url: String): String {
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