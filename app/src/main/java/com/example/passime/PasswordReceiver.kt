package com.example.passime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PasswordReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.passime.PASSWORD") {
            return
        }

        val receivedToken = intent.getStringExtra("token")
        val password = intent.getStringExtra("password")
        val service = PassImeService.instance ?: return
        val expectedToken = service.pendingAuthToken ?: return

        if (receivedToken == null || password == null) {
            return
        }

        if (receivedToken != expectedToken) {
            return
        }

        service.pendingAuthToken = null
        service.pendingPassword = password
    }
}
