package com.freaky.iulms.auth

import okhttp3.OkHttpClient

object AuthManager {
    private var authInstance: IULmsAuth? = null

    fun setAuth(auth: IULmsAuth) {
        this.authInstance = auth
    }

    fun getAuthenticatedClient(): OkHttpClient? {
        return authInstance?.client
    }
}