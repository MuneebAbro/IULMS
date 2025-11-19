package com.freaky.iulms

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ContextThemeWrapper
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.adapter.DashboardAdapter
import com.freaky.iulms.model.DashboardItem
import com.freaky.iulms.update.UpdateChecker
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val INSTALL_PERMISSION_REQUEST_CODE = 101
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 102


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNeededPermissions()

        val dashboardRecyclerView = findViewById<RecyclerView>(R.id.dashboard_recycler_view)
        val logoutButton = findViewById<ImageButton>(R.id.logout_button)
        val themeButton = findViewById<ImageButton>(R.id.theme_button)
        val logoutcon = findViewById<LinearLayout>(R.id.logout_container)

        setupRecyclerView(dashboardRecyclerView)

        logoutButton.setOnClickListener {
            vibrate()
            showLogoutConfirmationDialog()
        }
        logoutcon.setOnClickListener {
            vibrate()
            showLogoutConfirmationDialog()
        }
        themeButton.setOnClickListener {
            vibrate()
            showThemeNotImplementedDialog()
        }

        lifecycleScope.launch {
            UpdateChecker.checkForUpdates(this@MainActivity)
        }

    }
    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            } else {
                // If notification permission is already granted, proceed to check for updates
                checkForAppUpdates()
            }
        } else {
            // On older versions, no notification permission is needed, so check for updates directly
            checkForAppUpdates()
        }
        // Note: The permission to install packages is handled differently, via an intent to system settings,
        // which the UpdateChecker now handles automatically if needed.
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // After any permission request, try checking for updates again.
        checkForAppUpdates()
    }

    private fun checkForAppUpdates() {
        lifecycleScope.launch {
            UpdateChecker.checkForUpdates(this@MainActivity)
        }
    }
    private fun setupRecyclerView(recyclerView: RecyclerView) {
        val dashboardItems = listOf(
            DashboardItem("Time Table", "https://iulms.edu.pk/sic/Schedule.php", R.drawable.ic_schedule, Activity4::class.java),
            DashboardItem("Attendance", "https://iulms.edu.pk/sic/StudentAttendance.php", R.drawable.ic_attendance, Activity3::class.java),
            DashboardItem("Transcript", "https://iulms.edu.pk/sic/Transcript.php", R.drawable.ic_transcript, Activity1::class.java),
            DashboardItem("Exam Result", "https://iulms.edu.pk/sic/examresult.php", R.drawable.ic_exam_result, Activity2::class.java),
            DashboardItem("Vouchers", "https://iulms.edu.pk/sic/Vouchers.php", R.drawable.ic_vouchers, Activity5::class.java),
            DashboardItem("Exam Schedule", "https://iulms.edu.pk/sic/examschedule.php", R.drawable.ic_exam_schedule, Activity6::class.java)
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DashboardAdapter(dashboardItems) { selectedItem ->
            vibrate()
            val intent = Intent(this, LoadingActivity::class.java).apply {
                putExtra("URL_TO_FETCH", selectedItem.urlToFetch)
                putExtra("DESTINATION_ACTIVITY", selectedItem.destinationActivity.name)
            }
            startActivity(intent)
        }
    }

    private fun showLogoutConfirmationDialog() {
        val dialog = AlertDialog.Builder(
            this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert
        )
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .create()

        dialog.show()

// Now you can access buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#1A73E8"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#E53935"))

    }

    private fun showThemeNotImplementedDialog() {
        AlertDialog.Builder(ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert))
            .setTitle("😢 Oops!")
            .setMessage("Themes not implemented yet.")
            .setPositiveButton("Okay", null)
            .create()
            .show()
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("IULMS_ACCOUNTS", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("LAST_LOGGED_IN_USER")
            apply()
        }
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, 40))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }
}