package com.freaky.iulms

import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.freaky.iulms.auth.AuthManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URLEncoder

class VoucherPrintActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voucher_print)

        webView = findViewById(R.id.print_webview)
        val fab: FloatingActionButton = findViewById(R.id.fab_save_pdf)

        val voucherNumber = intent.getStringExtra("VOUCHER_NUMBER")
        val studentId = intent.getStringExtra("STUDENT_ID")

        if (voucherNumber.isNullOrEmpty() || studentId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Missing voucher information.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupWebView(voucherNumber, studentId)

        fab.setOnClickListener {
            createWebPrintJob(webView)
        }
    }

    private fun setupWebView(voucherNumber: String, studentId: String) {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // Page is loaded, FAB will handle the printing.
            }
        }

        val authClient = AuthManager.getAuthenticatedClient()
        if (authClient == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val urlDomain = "https://iulms.edu.pk"
        val cookies = authClient.cookieJar.loadForRequest(urlDomain.toHttpUrl())
        cookies.forEach { cookie ->
            cookieManager.setCookie(urlDomain, cookie.toString())
        }

        val url = "https://iulms.edu.pk/sic/PrintVoucher.php"
        val postData = "VoucherNumber=${URLEncoder.encode(voucherNumber, "UTF-8")}&studentId=${URLEncoder.encode(studentId, "UTF-8")}"
        webView.postUrl(url, postData.toByteArray())
    }

    private fun createWebPrintJob(webView: WebView) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter("MyVoucher")
        val jobName = "${getString(R.string.app_name)} Voucher"

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }
}
