package com.freaky.iulms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.freaky.iulms.adapter.SavedAccountsAdapter
import com.freaky.iulms.auth.AuthManager
import com.freaky.iulms.auth.IULmsAuth
import com.freaky.iulms.model.SavedAccount
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var manualLoginForm: LinearLayout
    private lateinit var savedAccountsRecyclerView: RecyclerView
    private lateinit var savedAccountsTitle: TextView

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loadingAnimationView = findViewById(R.id.loading_animation_view)
        manualLoginForm = findViewById(R.id.manual_login_form)
        savedAccountsRecyclerView = findViewById(R.id.saved_accounts_recycler_view)
        savedAccountsTitle = findViewById(R.id.saved_accounts_title)

        val usernameEditText = findViewById<TextInputEditText>(R.id.username)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login_button)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                handleLogin(username, password, true) // isManualLogin = true
            }
        }

        setupInitialView()
    }

    private fun setupInitialView() {
        val savedAccounts = getSavedAccounts()
        if (savedAccounts.isNotEmpty()) {
            savedAccountsTitle.visibility = View.VISIBLE
            savedAccountsRecyclerView.visibility = View.VISIBLE
            savedAccountsRecyclerView.layoutManager = LinearLayoutManager(this)
            savedAccountsRecyclerView.adapter = SavedAccountsAdapter(savedAccounts) { selectedAccount ->
                val password = getPasswordForAccount(selectedAccount.username)
                if (password != null) {
                    handleLogin(selectedAccount.username, password, false)
                }
            }
        } else {
            savedAccountsTitle.visibility = View.GONE
            savedAccountsRecyclerView.visibility = View.GONE
        }
        manualLoginForm.visibility = View.VISIBLE
    }

    private fun handleLogin(username: String, password: String, isManualLogin: Boolean) {
        loadingAnimationView.visibility = View.VISIBLE
        manualLoginForm.visibility = View.INVISIBLE // Use INVISIBLE to keep layout spacing
        savedAccountsRecyclerView.visibility = View.INVISIBLE
        savedAccountsTitle.visibility = View.INVISIBLE

        lifecycleScope.launch {
            val auth = IULmsAuth()
            val (success, message) = auth.login("https://iulms.edu.pk/login/index.php", username, password)

            loadingAnimationView.visibility = View.GONE

            if (success) {
                if (isManualLogin) {
                    showSaveCredentialDialog(username, password, auth)
                } else {
                    proceedToMain(auth)
                }
            } else {
                Toast.makeText(this@LoginActivity, "Login Failed: $message", Toast.LENGTH_LONG).show()
                // Restore view visibility on failure
                manualLoginForm.visibility = View.VISIBLE
                setupInitialView()
            }
        }
    }

    private fun showSaveCredentialDialog(username: String, password: String, auth: IULmsAuth) {
        AlertDialog.Builder(this)
            .setTitle("Save Login?")
            .setMessage("Would you like to save your credentials for faster login next time?")
            .setPositiveButton("Save") { _, _ ->
                saveAccount(username, password)
                proceedToMain(auth)
            }
            .setNegativeButton("Don't Save") { _, _ ->
                proceedToMain(auth)
            }
            .show()
    }

    private fun proceedToMain(auth: IULmsAuth) {
        AuthManager.setAuth(auth)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    // --- SharedPreferences Logic ---

    private fun saveAccount(username: String, password: String) {
        val sharedPreferences = getSharedPreferences("IULMS_ACCOUNTS", Context.MODE_PRIVATE)
        val accounts = getSavedAccounts().toMutableList()
        accounts.removeAll { it.username == username }
        accounts.add(SavedAccount(username, System.currentTimeMillis()))

        with(sharedPreferences.edit()) {
            putString("ACCOUNT_LIST", gson.toJson(accounts))
            putString("PASSWORD_$username", password) // Save password under a unique key
            apply()
        }
    }

    private fun getSavedAccounts(): List<SavedAccount> {
        val sharedPreferences = getSharedPreferences("IULMS_ACCOUNTS", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("ACCOUNT_LIST", null)
        return if (json != null) {
            val type = object : TypeToken<List<SavedAccount>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun getPasswordForAccount(username: String): String? {
        val sharedPreferences = getSharedPreferences("IULMS_ACCOUNTS", Context.MODE_PRIVATE)
        return sharedPreferences.getString("PASSWORD_$username", null)
    }
}