package com.example.amphibians.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

private const val TAG = "LoggingInterceptor"


class LoggingInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        // 1) Origin request leaving your app
        val request = chain.request()

        Log.d(TAG, "➡️ Sending request: ${request.method} ${request.url}")
        Log.d(TAG, "➡️ Request headers: ${request.headers}")

        // 2) Actually perform the request and get a response back
        val response = chain.proceed(request)

        Log.d(TAG, "⬅️ Response code: ${response.code} for ${request.url}")
        Log.d(TAG, "⬅️ Response headers: ${response.headers}")

        return response

    }
}