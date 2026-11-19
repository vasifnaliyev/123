package com.example.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val LOG_TAG = "VoxAppCheck"
private const val EXTRA_DEBUG_TOKEN = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val PREFS_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val KEY_SECRET = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RES_RECAPTCHA_KEY = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            val token = intent?.getStringExtra(EXTRA_DEBUG_TOKEN)
            if (!token.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val store = context.getSharedPreferences(
                    "$PREFS_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                store.edit().putString(KEY_SECRET, token).commit()
                Log.d(LOG_TAG, "Stored App Check debug token synchronously.")
            } else {
                Log.w(LOG_TAG, "Launch intent did not include an App Check debug token.")
            }
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            val keyResId = context.resources.getIdentifier(
                RES_RECAPTCHA_KEY,
                "string",
                context.packageName
            )
            if (keyResId != 0) {
                val siteKey = context.getString(keyResId)
                if (siteKey.isNotBlank()) {
                    appCheck.installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                } else {
                    Log.e(LOG_TAG, "reCAPTCHA site key string is empty.")
                }
            } else {
                Log.e(LOG_TAG, "Missing recaptcha_site_key resource.")
            }
        }
    } catch (error: Exception) {
        Log.e(LOG_TAG, "Unable to configure Firebase App Check", error)
    }
}
