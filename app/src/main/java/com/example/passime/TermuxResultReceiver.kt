package com.example.passime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TermuxResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PassIme", "Termux result received")

        val resultBundle = intent.getBundleExtra("result") ?: run {
            Log.e("PassIme", "No Termux result bundle")
            return
        }

        val stderr = resultBundle.getString("stderr")
        val exitCode = resultBundle.getInt("exitCode", -999)
        val errorMessage = resultBundle.getString("errmsg")

        Log.d("PassIme", "exitCode=$exitCode")
        Log.d("PassIme", "stderr=$stderr")
        Log.d("PassIme", "errmsg=$errorMessage")

        if (exitCode != 0) {
            Log.e(
                "PassIme",
                "Termux command failed: $errorMessage"
            )

            PassImeService.instance?.pendingAuthToken = null
        }
    }
}
