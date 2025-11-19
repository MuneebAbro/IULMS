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
        // --- WebView Safe Settings ---
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true          // fit to screen
            useWideViewPort = true              // make layout behave like desktop
            builtInZoomControls = true          // user can zoom in/out
            displayZoomControls = false
            textZoom = 100                       // avoid automatic text scaling
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                // Inject CSS after the page loads
                injectFixCSS(webView)
            }
        }

        // --- Cookie Sync ---
        val authClient = AuthManager.getAuthenticatedClient()
        if (authClient == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        val domain = "https://iulms.edu.pk"
        val cookies = authClient.cookieJar.loadForRequest(domain.toHttpUrl())
        cookies.forEach { cookie ->
            cookieManager.setCookie(domain, cookie.toString())
        }

        // --- POST the voucher ---
        val url = "https://iulms.edu.pk/sic/PrintVoucher.php"
        val postData =
            "VoucherNumber=${URLEncoder.encode(voucherNumber, "UTF-8")}&studentId=${URLEncoder.encode(studentId, "UTF-8")}"

        webView.postUrl(url, postData.toByteArray())
    }

    private fun injectFixCSS(webView: WebView) {
        val css = """
        (function() {
            var style = document.createElement('style');
            style.innerHTML = `
                body {
                    margin: 0;
                    padding: 0;
                    transform-origin: left top;
                    -webkit-text-size-adjust: none !important;
                }
                table {
                    width: 100% !important;
                    border-collapse: collapse;
                }
                * {
                    max-width: 100% !important;
                    word-wrap: break-word;
                }
            `;
            document.head.appendChild(style);
        })();
    """

        webView.evaluateJavascript(css, null)
    }


    private fun createWebPrintJob(webView: WebView) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter("MyVoucher")
        val jobName = "${getString(R.string.app_name)} Voucher"

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }
}
