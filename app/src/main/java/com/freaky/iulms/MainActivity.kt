package com.freaky.iulms

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.freaky.iulms.adapter.DashboardAdapter
import com.freaky.iulms.auth.AuthManager
import com.freaky.iulms.auth.IULmsDataFetcher
import com.freaky.iulms.model.DashboardItem
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var fetchedData: Map<String, String>? = null
    private lateinit var mainProgressBar: LottieAnimationView
    private lateinit var cardView1: MaterialCardView
    private lateinit var dashboardRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainProgressBar = findViewById(R.id.main_progress_bar)
        cardView1 = findViewById(R.id.cardView1)
        dashboardRecyclerView = findViewById(R.id.dashboard_recycler_view)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val logoutButton = findViewById<ImageButton>(R.id.logout_button)
        val themeButton = findViewById<ImageButton>(R.id.theme_button)
        val logoutcon = findViewById<LinearLayout>(R.id.logout_container)

        setupRecyclerView()


        logoutButton.setOnClickListener {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


            // Vibrate for error
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, 40))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
            showLogoutConfirmationDialog()
        }
        logoutcon.setOnClickListener {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


            // Vibrate for error
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, 40))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
            showLogoutConfirmationDialog()
        }
        themeButton.setOnClickListener {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


            // Vibrate for error
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, 40))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
            val dialog = AlertDialog.Builder(
                ContextThemeWrapper(
                    this,
                    androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert
                )
            )
                .setTitle("😢 Oops!")
                .setMessage("Themes not implemented yet.")
                .setPositiveButton("Okay", null)
                .create()

            dialog.show()
        }


        fetchData()
    }

    private fun setupRecyclerView() {
        val dashboardItems = listOf(
            DashboardItem("Time Table", "Schedule", R.drawable.ic_schedule, Activity4::class.java),
            DashboardItem("Attendance", "Attendance", R.drawable.ic_attendance, Activity3::class.java),
            DashboardItem("Transcript", "Transcript", R.drawable.ic_transcript, Activity1::class.java),
            DashboardItem("Exam Result", "ExamResult", R.drawable.ic_exam_result, Activity2::class.java),


            DashboardItem("Vouchers", "Vouchers", R.drawable.ic_vouchers, Activity5::class.java),
            DashboardItem("Exam Schedule", "ExamSchedule", R.drawable.ic_exam_schedule, Activity6::class.java)
        )

        dashboardRecyclerView.layoutManager = LinearLayoutManager(this)
        dashboardRecyclerView.adapter = DashboardAdapter(dashboardItems) { selectedItem ->
            // **THE FIX**: Use the new dataKey to get the correct data
            openActivityWithData(selectedItem.destinationActivity, fetchedData?.get(selectedItem.dataKey))
        }
    }

    private fun fetchData() {
        val client = AuthManager.getAuthenticatedClient()
        if (client == null) {
            Toast.makeText(this, "Error: Not authenticated. Please login again.", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        mainProgressBar.visibility = View.VISIBLE
        cardView1.visibility = View.VISIBLE
        dashboardRecyclerView.visibility = View.GONE

        lifecycleScope.launch {
            val dataFetcher = IULmsDataFetcher(client)
            fetchedData = dataFetcher.fetchAllData()
            mainProgressBar.visibility = View.GONE
            cardView1.visibility = View.GONE
            dashboardRecyclerView.visibility = View.VISIBLE

        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(
            ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
        )
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
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

    private fun openActivityWithData(activityClass: Class<*>, data: String?) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


        // Vibrate for error
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 20))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
        if (data != null) {
            val intent = Intent(this, activityClass)
            intent.putExtra("RAW_DATA", data)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Data not available. Please wait or try again.", Toast.LENGTH_SHORT).show()
        }
    }
}