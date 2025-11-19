package com.freaky.iulms.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.freaky.iulms.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

object UpdateChecker {

    private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/MuneebAbro/IULMS/main/version.json"
    private const val APK_FILE_NAME = "IULMS.apk"

    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun checkForUpdates(context: Context) {
        val updateInfo = fetchUpdateInfo() ?: return

        if (isUpdateRequired(BuildConfig.VERSION_NAME, updateInfo.latestVersion)) {
            withContext(Dispatchers.Main) {
                showUpdateDialog(context, updateInfo)
            }
        } else {
            Log.d("UpdateChecker", "App is up to date.")
        }
    }

    private suspend fun fetchUpdateInfo(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(VERSION_JSON_URL).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), UpdateInfo::class.java)
            } else {
                Log.e("UpdateChecker", "Fetch failed: ${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e("UpdateChecker", "Network error", e)
            null
        } catch (e: JsonSyntaxException) {
            Log.e("UpdateChecker", "JSON parsing error", e)
            null
        }
    }

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
        return false
    }

    private fun showUpdateDialog(context: Context, updateInfo: UpdateInfo) {
        AlertDialog.Builder(context)
            .setTitle("Update Available")
            .setMessage("A newer version (${updateInfo.latestVersion}) is available.")
            .setPositiveButton("Update Now") { _, _ ->
                startDownload(context, updateInfo.apkUrl)
            }
            .apply {
                if (!updateInfo.forceUpdate) {
                    setNegativeButton("Later", null)
                }
            }
            .setCancelable(!updateInfo.forceUpdate)
            .show()
    }

    private fun startDownload(context: Context, url: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val destination = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)
        if (destination.exists()) {
            destination.delete()
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("IULMS Update")
            .setDescription("Downloading new version...")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = downloadManager.enqueue(request)
        Toast.makeText(context, "Update download started...", Toast.LENGTH_SHORT).show()

        registerDownloadReceiver(context, downloadId)
    }

    private fun registerDownloadReceiver(context: Context, downloadId: Long) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Log.d("UpdateChecker", "Download complete for ID: $id")
                    promptInstall(c)
                    c.unregisterReceiver(this)
                }
            }
        }
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
    }

    private fun promptInstall(context: Context) {
        val apkFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)
        if (!apkFile.exists()) {
            Log.e("UpdateChecker", "Downloaded APK not found!")
            Toast.makeText(context, "Update file not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val apkUri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                showInstallPermissionDialog(context)
                return
            }
        }
        context.startActivity(intent)
    }
    
    private fun showInstallPermissionDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Permission Required")
            .setMessage("To update the app, you need to allow installing from unknown sources.")
            .setPositiveButton("Go to Settings") { _, _ ->
                 val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                 intent.data = Uri.parse("package:${context.packageName}")
                 context.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}