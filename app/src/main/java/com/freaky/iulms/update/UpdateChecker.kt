package com.freaky.iulms.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.freaky.iulms.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
object UpdateChecker {

    // THE FIX: Use the correct GitHub raw URL
    private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/MuneebAbro/IULMS/main/version.json"

    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Fetches the latest version info and shows an update dialog if necessary.
     *
     * @param context The context to use for showing the dialog.
     */
    suspend fun checkForUpdates(context: Context) {
        val updateInfo = fetchUpdateInfo() ?: return // Exit if fetch fails

        val currentVersion = BuildConfig.VERSION_NAME
        val latestVersion = updateInfo.latestVersion

        if (isUpdateRequired(currentVersion, latestVersion)) {
            withContext(Dispatchers.Main) {
                showUpdateDialog(context, updateInfo)
            }
        }
    }

    /**
     * Fetches and parses the UpdateInfo from the remote JSON file.
     */
    private suspend fun fetchUpdateInfo(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(VERSION_JSON_URL).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                gson.fromJson(body, UpdateInfo::class.java)
            } else {
                Log.e("UpdateChecker", "Fetch failed with code: ${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e("UpdateChecker", "Network error fetching update info", e)
            null
        } catch (e: JsonSyntaxException) {
            Log.e("UpdateChecker", "Error parsing update JSON", e)
            null
        }
    }

    /**
     * Compares two version strings (e.g., "1.0.5" vs "1.1.0").
     *
     * @return true if the latestVersion is greater than the currentVersion.
     */
    private fun isUpdateRequired(currentVersion: String, latestVersion: String): Boolean {
        val currentParts = currentVersion.split('.').map { it.toIntOrNull() ?: 0 }
        val latestParts = latestVersion.split('.').map { it.toIntOrNull() ?: 0 }
        val maxParts = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until maxParts) {
            val current = currentParts.getOrElse(i) { 0 }
            val latest = latestParts.getOrElse(i) { 0 }
            if (latest > current) return true
            if (current > latest) return false
        }
        return false // Versions are identical
    }

    /**
     * Shows the update dialog to the user.
     */
    private fun showUpdateDialog(context: Context, updateInfo: UpdateInfo) {
        val builder = AlertDialog.Builder(context)
            .setTitle("Update Available")
            .setMessage("A newer version (${updateInfo.latestVersion}) is available.")
            .setPositiveButton("Update Now") { _, _ ->
                openUrl(context, updateInfo.apkUrl)
            }

        if (!updateInfo.forceUpdate) {
            builder.setNegativeButton("Later", null)
        }

        builder.setCancelable(!updateInfo.forceUpdate)
        builder.show()
    }

    /**
     * Opens the given URL in the user's browser.
     */
    private fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}