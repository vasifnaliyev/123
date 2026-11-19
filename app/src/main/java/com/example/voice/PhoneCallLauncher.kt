package com.example.voice

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

sealed interface DialLaunchOutcome {
    data object DirectCallStarted : DialLaunchOutcome
    data object DialerOpened : DialLaunchOutcome
    data object InvalidNumber : DialLaunchOutcome
    data object NoDialerAvailable : DialLaunchOutcome
}

object PhoneCallLauncher {

    fun sanitizePhoneNumber(raw: String): String {
        return raw.trim().filter { ch ->
            ch.isDigit() || ch == '+' || ch == '*' || ch == '#' || ch == ',' || ch == ';'
        }
    }

    fun hasDirectCallPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun placeOrDialCall(
        context: Context,
        rawPhoneNumber: String,
        preferDirectCall: Boolean
    ): DialLaunchOutcome {
        val cleaned = sanitizePhoneNumber(rawPhoneNumber)
        if (cleaned.count { it.isDigit() } < 3) {
            return DialLaunchOutcome.InvalidNumber
        }

        val telUri = Uri.fromParts("tel", cleaned, null)
        val useDirectCall = preferDirectCall && hasDirectCallPermission(context)
        val intent = Intent(
            if (useDirectCall) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            telUri
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            if (useDirectCall) DialLaunchOutcome.DirectCallStarted else DialLaunchOutcome.DialerOpened
        } catch (_: ActivityNotFoundException) {
            DialLaunchOutcome.NoDialerAvailable
        } catch (_: SecurityException) {
            val fallback = Intent(Intent.ACTION_DIAL, telUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
                DialLaunchOutcome.DialerOpened
            } catch (_: ActivityNotFoundException) {
                DialLaunchOutcome.NoDialerAvailable
            }
        }
    }
}
