package com.freaky.iulms.auth

import android.util.Log
import com.freaky.iulms.parser.HtmlParserUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

class IULmsDataFetcher(private val client: OkHttpClient) {

    // Simplified to fetch only a single URL
    suspend fun fetchSingleUrl(url: String): String = withContext(Dispatchers.IO) {
        // Special handling for the transcript, which requires a JSON fetch
        if (url.contains("Transcript.php")) {
            return@withContext fetchTranscriptJson(url)
        }

        return@withContext try {
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d("IULmsDataFetcher", "Fetched ${url}: ${body.length} chars")
                body
            } else {
                Log.e("IULmsDataFetcher", "Failed to fetch $url. Code: ${response.code}")
                "Error: Failed to load content. Status code: ${response.code}"
            }
        } catch (e: IOException) {
            Log.e("IULmsDataFetcher", "Error fetching $url", e)
            "Error: Network request failed. ${e.message}"
        }
    }

    private fun fetchTranscriptJson(transcriptPageUrl: String): String {
        return try {
            val getRequest = Request.Builder().url(transcriptPageUrl).header("User-Agent", "Mozilla/5.0").build()
            val getResponse = client.newCall(getRequest).execute()
            if (!getResponse.isSuccessful) return "Error: Failed to fetch transcript page (Code: ${getResponse.code})"

            val html = getResponse.body?.string() ?: return "Error: Transcript page body is empty."
            val doc = Jsoup.parse(html)
            val degreeId = doc.select("#cmbDegree option").first()?.attr("value")

            if (degreeId.isNullOrEmpty()) {
                return "Error: Could not find degreeId on transcript page."
            }

            val dataServiceUrl = "https://iulms.edu.pk/sic/SICDataService.php"
            val formBody = FormBody.Builder()
                .add("action", "GetTranscript")
                .add("degreeId", degreeId)
                .build()

            val postRequest = Request.Builder().url(dataServiceUrl).post(formBody).build()
            val postResponse = client.newCall(postRequest).execute()

            if (postResponse.isSuccessful) {
                val jsonBody = postResponse.body?.string() ?: ""
                Log.d("IULmsDataFetcher", "Fetched Transcript JSON: ${jsonBody.length} chars")
                jsonBody
            } else {
                "Error: Failed to fetch transcript JSON data. Code: ${postResponse.code}"
            }

        } catch (e: IOException) {
            Log.e("IULmsDataFetcher", "Error fetching Transcript JSON", e)
            "Error: Network request failed for transcript. ${e.message}"
        }
    }
}