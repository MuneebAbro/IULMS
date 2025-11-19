package com.freaky.iulms.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.freaky.iulms.BuildConfig
import com.freaky.iulms.dialog.InstallDialog
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
    private const val PREFS_NAME = "UpdateChecker"
    private const val KEY_PENDING_INSTALL = "pending_install"

    private val client = OkHttpClient()
    private val gson = Gson()
    private var installDialog: InstallDialog? = null
    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null
    private var downloadReceiver: BroadcastReceiver? = null

    suspend fun checkForUpdates(context: Context) {
        // Check if there's a pending install first
        checkPendingInstall(context)

        val updateInfo = fetchUpdateInfo() ?: return

        if (isUpdateRequired(BuildConfig.VERSION_NAME, updateInfo.latestVersion)) {
            withContext(Dispatchers.Main) {
                showUpdateDialog(context, updateInfo)
            }
        } else {
            Log.d("UpdateChecker", "App is up to date.")
        }
    }

    private fun checkPendingInstall(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasPendingInstall = prefs.getBoolean(KEY_PENDING_INSTALL, false)

        Log.d("UpdateChecker", "Checking pending install: $hasPendingInstall")

        if (hasPendingInstall) {
            Log.d("UpdateChecker", "Found pending install, attempting to install...")

            // Clear the flag
            prefs.edit().putBoolean(KEY_PENDING_INSTALL, false).apply()

            // Try to install
            val apkFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                APK_FILE_NAME
            )

            Log.d("UpdateChecker", "APK file exists: ${apkFile.exists()}")

            if (apkFile.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val canInstall = context.packageManager.canRequestPackageInstalls()
                    Log.d("UpdateChecker", "Has install permission: $canInstall")

                    if (canInstall) {
                        installApk(context, apkFile)
                    } else {
                        Log.d("UpdateChecker", "Still no install permission")
                    }
                } else {
                    installApk(context, apkFile)
                }
            } else {
                Log.e("UpdateChecker", "APK file not found for pending install")
            }
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
        val dialog = AlertDialog.Builder(
            ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_Alert)
        )
            .setTitle("Update Available")
            .setMessage("A newer version (${updateInfo.latestVersion}) is available.\n\nWhat's New:\n${updateInfo.changelog ?: "Bug fixes and improvements"}")
            .setPositiveButton("Update Now") { _, _ ->
                startDownload(context, updateInfo.apkUrl)
            }
            .apply {
                if (!updateInfo.forceUpdate) {
                    setNegativeButton("Later", null)
                }
            }
            .setCancelable(!updateInfo.forceUpdate)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#1A73E8"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#E53935"))

    }

    private fun startDownload(context: Context, url: String) {
        // Show install dialog
        installDialog = InstallDialog(context)
        installDialog?.show("Downloading Update")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val destination = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            APK_FILE_NAME
        )
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

        // Start tracking download progress
        trackDownloadProgress(context, downloadManager, downloadId)

        // Register receiver for download completion
        registerDownloadReceiver(context, downloadId)
    }

    private fun trackDownloadProgress(context: Context, downloadManager: DownloadManager, downloadId: Long) {
        progressHandler = Handler(Looper.getMainLooper())
        progressRunnable = object : Runnable {
            override fun run() {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor? = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1 && statusIndex != -1) {
                        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                        val bytesTotal = cursor.getLong(bytesTotalIndex)
                        val status = cursor.getInt(statusIndex)

                        if (bytesTotal > 0) {
                            val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                            installDialog?.updateProgress(progress)
                            installDialog?.updateStatus("Downloading... $progress%")
                        }

                        // Stop tracking if download is complete or failed
                        if (status == DownloadManager.STATUS_SUCCESSFUL ||
                            status == DownloadManager.STATUS_FAILED) {
                            cursor.close()
                            progressHandler?.removeCallbacks(this)

                            if (status == DownloadManager.STATUS_FAILED) {
                                Handler(Looper.getMainLooper()).post {
                                    installDialog?.dismiss()
                                    Toast.makeText(context, "Download failed. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            return
                        }
                    }
                    cursor.close()
                }

                // Check again after 500ms
                progressHandler?.postDelayed(this, 500)
            }
        }
        progressHandler?.post(progressRunnable!!)
    }

    private fun registerDownloadReceiver(context: Context, downloadId: Long) {
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Log.d("UpdateChecker", "Download complete for ID: $id")

                    // Stop progress tracking
                    progressHandler?.removeCallbacks(progressRunnable!!)

                    // Update dialog status
                    installDialog?.updateProgress(100)
                    installDialog?.updateStatus("Download Complete")

                    // Small delay before showing install prompt
                    Handler(Looper.getMainLooper()).postDelayed({
                        installDialog?.dismiss()
                        promptInstall(c)
                    }, 1000)

                    try {
                        c.unregisterReceiver(this)
                        downloadReceiver = null
                    } catch (e: Exception) {
                        Log.e("UpdateChecker", "Error unregistering receiver", e)
                    }
                }
            }
        }
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(downloadReceiver, intentFilter)
        }
    }

    private fun promptInstall(context: Context) {
        val apkFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            APK_FILE_NAME
        )

        Log.d("UpdateChecker", "Attempting to install APK")
        Log.d("UpdateChecker", "APK exists: ${apkFile.exists()}")
        Log.d("UpdateChecker", "APK path: ${apkFile.absolutePath}")

        if (!apkFile.exists()) {
            Log.e("UpdateChecker", "Downloaded APK not found!")
            Toast.makeText(context, "Update file not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for install permission first (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            Log.d("UpdateChecker", "Can request package installs: $canInstall")
            Log.d("UpdateChecker", "Android version: ${Build.VERSION.SDK_INT}")

            if (!canInstall) {
                showInstallPermissionDialog(context, apkFile)
                return
            }
        }

        // Proceed with installation
        installApk(context, apkFile)
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            Log.d("UpdateChecker", "Starting APK installation")
            Log.d("UpdateChecker", "APK size: ${apkFile.length()} bytes")

            // Try alternative installation method for Android 14+ (API 34+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Log.d("UpdateChecker", "Attempting modern installation method (Android 14+)")
                installApkModern(context, apkFile)
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.provider",
                apkFile
            )

            Log.d("UpdateChecker", "APK URI: $apkUri")
            Log.d("UpdateChecker", "Authority: ${BuildConfig.APPLICATION_ID}.provider")

            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }

            // Fallback to ACTION_VIEW if ACTION_INSTALL_PACKAGE doesn't work
            val installIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent
            } else {
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            // Check if there's an app that can handle this intent
            val activities = context.packageManager.queryIntentActivities(installIntent, 0)
            Log.d("UpdateChecker", "Activities that can handle install: ${activities.size}")

            if (activities.isNotEmpty()) {
                context.startActivity(installIntent)
                Toast.makeText(context, "Opening installer...", Toast.LENGTH_SHORT).show()
                Log.d("UpdateChecker", "Install intent launched successfully")
            } else {
                Log.e("UpdateChecker", "No app found to handle package installation")
                Toast.makeText(context, "No installer app found. Please install manually from Downloads.", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e("UpdateChecker", "Failed to start install", e)
            Log.e("UpdateChecker", "Error details: ${e.message}")
            Log.e("UpdateChecker", "Stack trace: ${e.stackTraceToString()}")
            Toast.makeText(context, "Failed to install: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun installApkModern(context: Context, apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(intent)
            Toast.makeText(context, "Opening installer...", Toast.LENGTH_SHORT).show()
            Log.d("UpdateChecker", "Modern install method launched")

        } catch (e: Exception) {
            Log.e("UpdateChecker", "Modern install failed, trying legacy method", e)
            // Fallback to regular method
            val apkUri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        }
    }

    private fun showInstallPermissionDialog(context: Context, apkFile: File) {
        // Save pending install state
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PENDING_INSTALL, true).apply()

        AlertDialog.Builder(context, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
            .setTitle("Permission Required")
            .setMessage("To install the update, please allow installation from this source.\n\nAfter granting permission, the installation will start automatically.")
            .setPositiveButton("Grant Permission") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    intent.data = Uri.parse("package:${context.packageName}")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Clear pending install if user cancels
                prefs.edit().putBoolean(KEY_PENDING_INSTALL, false).apply()
            }
            .setCancelable(false)
            .show()
    }

    fun cleanup() {
        try {
            installDialog?.dismiss()
            installDialog = null
            progressHandler?.removeCallbacks(progressRunnable!!)
            progressHandler = null
            progressRunnable = null
            downloadReceiver = null
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error during cleanup", e)
        }
    }
}