package com.example.passime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PasswordReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != "com.example.passime.PASSWORD") {
            return
        }

        val receivedToken =
            intent.getStringExtra("token")

        val password =
            intent.getStringExtra("password")

        val service =
            PassImeService.instance

        val expectedToken =
            service?.pendingAuthToken

        if (receivedToken == null || password == null) {
            return
        }

        if (expectedToken == null) {
            return
        }

        if (receivedToken != expectedToken) {
            return
        }

        // Token is one-time-use.
        service.pendingAuthToken = null

        // Store the password for explicit insertion through
        // the Password button.
        service.pendingPassword = password
    }
}
