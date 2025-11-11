package com.freaky.iulms.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import java.io.IOException

/**
 * IULmsAuth handles authentication against the IU LMS (Moodle) website.
 * It uses OkHttp for network requests and Jsoup for HTML parsing.
 *
 * Example Usage (in an Activity or ViewModel with a CoroutineScope):
 * ```
 * lifecycleScope.launch {
 *     val auth = IULmsAuth()
 *     val (success, message) = auth.login(
 *         "https://iulms.edu.pk/login/index.php",
 *         "<YOUR_USERNAME>",
 *         "<YOUR_PASSWORD>"
 *     )
 *     Log.d("IULmsAuth", "Login result: $success -> $message")
 *     if (success) {
 *         Log.d("IULmsAuth", "Cookies: ${auth.dumpCookies()}")
 *         // You can now use auth.client to make authenticated requests
 *     }
 * }
 * ```
 */
class IULmsAuth(client: OkHttpClient) {

    val client: OkHttpClient
    private val cookieJar = SimpleCookieJar()

    init {
        // The client passed to the constructor is used as a base.
        // We build our internal client with a cookie jar and redirection enabled.
        this.client = client.newBuilder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .build()
    }

    constructor() : this(OkHttpClient())

    /**
     * Attempts to log in to the IU LMS.
     *
     * @param loginPageUrl The URL of the Moodle login page.
     * @param username The user's username.
     * @param password The user's password.
     * @return A Pair where the first element is true on success, false on failure,
     *         and the second element is a message describing the outcome.
     */
    suspend fun login(loginPageUrl: String, username: String, password: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // 1. GET the login page to get the session cookie and hidden form inputs
            val getResponse = client.newCall(Request.Builder().url(loginPageUrl).header("User-Agent", "Mozilla/5.0").build()).execute()
            if (!getResponse.isSuccessful) return@withContext false to "Failed to fetch login page: ${getResponse.code}"
            Log.d("IULmsAuth", "GET $loginPageUrl -> ${getResponse.code}, length=${getResponse.body?.contentLength()}")

            val responseBody = getResponse.body?.string() ?: return@withContext false to "Login page is empty"
            val doc = Jsoup.parse(responseBody, loginPageUrl)

            // 2. Find the login form and extract its inputs
            val form = doc.select("form").find { it.select("input[name=username], input[name=password]").size >= 2 } 
                ?: doc.select("form#login").first() 
                ?: doc.select("form").first()
                ?: return@withContext false to "Could not find a login form on the page."

            val formActionUrl = form.absUrl("action").ifEmpty { loginPageUrl }
            val formInputs = form.select("input").associate { it.attr("name") to it.attr("value") }.toMutableMap()
            Log.d("IULmsAuth", "Parsed form inputs: ${formInputs.keys}")

            // 3. Overwrite credentials and build the POST request body
            formInputs["username"] = username
            formInputs["password"] = password
            // Moodle might check for this to enable cookies
            formInputs["testcookies"] = "1"

            val formBody = FormBody.Builder().apply {
                formInputs.forEach { (key, value) -> add(key, value) }
            }.build()

            // 4. POST the login credentials
            val postRequest = Request.Builder()
                .url(formActionUrl)
                .header("User-Agent", "Mozilla/5.0")
                .post(formBody)
                .build()

            val postResponse = client.newCall(postRequest).execute()
            val finalUrl = postResponse.request.url.toString()
            Log.d("IULmsAuth", "POST to $formActionUrl -> finalUrl=$finalUrl")

            // 5. Verify login success
            if (finalUrl.contains("/my/") || finalUrl.contains("/dashboard") || finalUrl.contains("/course")) {
                return@withContext true to "Login successful. Redirected to dashboard."
            }

            val postResponseBody = postResponse.body?.string() ?: ""
            if (postResponseBody.contains("Invalid login", ignoreCase = true) || postResponseBody.contains("incorrect password")) {
                return@withContext false to "Invalid credentials."
            }
            
            if (doc.select("input[name=captcha]").isNotEmpty()) {
                 return@withContext false to "CAPTCHA detected on login page, cannot proceed."
            }

            // As a final check, get the dashboard page and see if the login form is still there
            val dashboardUrl = loginPageUrl.toHttpUrlOrNull()?.newBuilder("/my/")?.build()
                ?: return@withContext false to "Could not construct dashboard URL from login page URL."
            val dashboardResponse = client.newCall(Request.Builder().url(dashboardUrl).build()).execute()
            val dashboardBody = dashboardResponse.body?.string() ?: ""
            val stillOnLoginPage = dashboardBody.contains("login") && dashboardBody.contains("password")
            Log.d("IULmsAuth", "Dashboard fetch -> containsLoginForm=$stillOnLoginPage")

            if (!stillOnLoginPage) {
                true to "Login successful (verified by dashboard fetch)."
            } else {
                false to "Login failed. Please check credentials."
            }

        } catch (e: IOException) {
            Log.e("IULmsAuth", "Network error during login", e)
            false to "Network error: ${e.message}"
        } catch (e: Exception) {
            Log.e("IULmsAuth", "An unexpected error occurred", e)
            false to "An unexpected error occurred: ${e.message}"
        }
    }

    /**
     * Dumps all cookies currently in the CookieJar for debugging.
     * @return A list of all cookies.
     */
    fun dumpCookies(): List<Cookie> {
        return cookieJar.dumpAllCookies()
    }
}

/**
 * A simple in-memory CookieJar implementation.
 */
class SimpleCookieJar : CookieJar {
    private val storage = mutableListOf<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        storage.addAll(cookies.filter { it.expiresAt > System.currentTimeMillis() })
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        storage.removeAll { it.expiresAt < System.currentTimeMillis() }
        return storage.filter { it.matches(url) }
    }

    fun dumpAllCookies(): List<Cookie> = storage
}