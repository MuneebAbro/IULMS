package com.freaky.iulms.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import java.io.IOException

class IULmsAuth {

    val client: OkHttpClient
    private val cookieJar = SimpleCookieJar()

    init {
        this.client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .addInterceptor(::addBrowserHeaders)
            .build()
    }

    private fun addBrowserHeaders(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            .header("Accept-Language", "en-US,en;q=0.9")
            // Remove Accept-Encoding - let OkHttp handle it automatically
            // .header("Accept-Encoding", "gzip, deflate, br")
            .header("Connection", "keep-alive")
            .header("Cache-Control", "max-age=0")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")

        if (originalRequest.header("Referer") == null && chain.connection() != null) {
            val referer = originalRequest.url.newBuilder().encodedPath("/").build().toString()
            builder.header("Referer", referer)
        }
        if (originalRequest.header("Origin") == null && chain.connection() != null) {
            val origin = originalRequest.url.newBuilder().encodedPath("/").build().toString()
            builder.header("Origin", origin)
        }

        return chain.proceed(builder.build())
    }

    suspend fun login(loginPageUrl: String, username: String, password: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // STEP 1: Perform an initial GET to establish a session and handle any cookie-test redirects.
            val initialRequest = Request.Builder().url(loginPageUrl).build()
            val initialResponse = client.newCall(initialRequest).execute()
            if (!initialResponse.isSuccessful) return@withContext false to "Failed to connect to login page: ${initialResponse.code}"

            // This first call might not have the form, but it sets the necessary session cookies.
            initialResponse.close()

            // Realistic delay
            delay(800)

            // STEP 2: Now, make a second GET request to the explicit login URL to ensure we get the form.
            val getLoginRequest = Request.Builder().url(loginPageUrl).header("Referer", loginPageUrl).build()
            val loginPageResponse = client.newCall(getLoginRequest).execute()
            if (!loginPageResponse.isSuccessful) return@withContext false to "Failed to fetch login page content: ${loginPageResponse.code}"

            // OkHttp automatically handles decompression when you use .string()
            val responseBody = loginPageResponse.body?.string() ?: return@withContext false to "Login page is empty"

            // Debug: Log first 500 chars to verify it's readable HTML
            Log.d("IULmsAuth", "Response preview: ${responseBody.take(500)}")

            val doc = Jsoup.parse(responseBody, loginPageUrl)

            // STEP 3: Extract the login form action URL and any CSRF tokens
            val loginForm = doc.selectFirst("form#login, form[name=login], form[action*=login]")
            if (loginForm == null) {
                Log.e("IULmsAuth", "Could not find login form on the page.")
                Log.d("IULmsAuth_FAIL_HTML", responseBody.take(2000))
                return@withContext false to "Could not find login form on the page."
            }

            // Extract the form action URL (might be different from loginPageUrl)
            val formAction = loginForm.attr("action")
            val postUrl = if (formAction.isNotEmpty()) {
                if (formAction.startsWith("http")) formAction else loginPageUrl.toHttpUrl().newBuilder().encodedPath(formAction).build().toString()
            } else {
                loginPageUrl
            }
            Log.d("IULmsAuth", "Form action URL: $postUrl")

            // Try to find logintoken (newer Moodle versions)
            val loginToken = doc.selectFirst("input[name=logintoken]")?.attr("value")
            if (loginToken != null) {
                Log.d("IULmsAuth", "Found Moodle logintoken: $loginToken")
            } else {
                Log.d("IULmsAuth", "No logintoken found - this appears to be an older Moodle version")
            }

            // STEP 4: Build the POST request body with all hidden fields
            val formBodyBuilder = FormBody.Builder()
                .add("username", username)
                .add("password", password)

            // Add logintoken if it exists (newer Moodle)
            if (loginToken != null) {
                formBodyBuilder.add("logintoken", loginToken)
            }

            // Add any other hidden input fields from the form
            loginForm.select("input[type=hidden]").forEach { hiddenInput ->
                val name = hiddenInput.attr("name")
                val value = hiddenInput.attr("value")
                if (name.isNotEmpty() && name != "logintoken") {
                    formBodyBuilder.add(name, value)
                    Log.d("IULmsAuth", "Adding hidden field: $name = $value")
                }
            }

            val formBody = formBodyBuilder.build()

            delay(1200)

            // STEP 5: POST the login credentials to the form action URL.
            val postRequest = Request.Builder()
                .url(postUrl)
                .header("Referer", loginPageUrl)
                .post(formBody)
                .build()

            val postResponse = client.newCall(postRequest).execute()
            val finalUrl = postResponse.request.url.toString()
            val postResponseBody = postResponse.body?.string() ?: ""
            Log.d("IULmsAuth", "POST to $postUrl -> finalUrl=$finalUrl")

            // STEP 6: Verify login success by checking multiple indicators
            val success = finalUrl.contains("/my/") ||
                    finalUrl.contains("my.php") ||
                    postResponseBody.contains("loggedinas", ignoreCase = true) ||
                    postResponseBody.contains("You are logged in as", ignoreCase = true)

            if (success) {
                true to "Login successful."
            } else {
                if (postResponseBody.contains("Invalid login", ignoreCase = true) ||
                    postResponseBody.contains("incorrect username", ignoreCase = true) ||
                    postResponseBody.contains("incorrect password", ignoreCase = true)) {
                    false to "Invalid username or password."
                } else {
                    Log.e("IULmsAuth", "Login failed for an unknown reason.")
                    Log.d("IULmsAuth_FAIL_HTML", postResponseBody.take(2000))
                    false to "Login failed. The site may have updated its login process."
                }
            }
        } catch (e: IOException) {
            Log.e("IULmsAuth", "Network error during login", e)
            false to "Network error: ${e.message}"
        } catch (e: Exception) {
            Log.e("IULmsAuth", "An unexpected error occurred", e)
            false to "An unexpected error occurred: ${e.message}"
        }
    }

    fun dumpCookies(): List<Cookie> {
        return cookieJar.dumpAllCookies()
    }
}

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