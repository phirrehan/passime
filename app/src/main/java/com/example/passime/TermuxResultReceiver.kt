package com.example.passime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TermuxResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d("PassIme", "Termux result received")

        val resultBundle = intent.getBundleExtra("result")

        if (resultBundle == null) {
            Log.e("PassIme", "No Termux result bundle")
            return
        }

        val stdout = resultBundle.getString("stdout")
        val stderr = resultBundle.getString("stderr")
        val exitCode = resultBundle.getInt("exitCode", -999)
        val errorMessage = resultBundle.getString("errmsg")

        Log.d("PassIme", "exitCode=$exitCode")
        Log.d("PassIme", "stdout=$stdout")
        Log.d("PassIme", "stderr=$stderr")
        Log.d("PassIme", "errmsg=$errorMessage")

        if (exitCode != 0) {
            Log.e("PassIme", "Termux command failed: $errorMessage")
            return
        }

        if (stdout == null) {
            Log.e("PassIme", "Termux returned no stdout")
            return
        }

        PassImeService.instance
            ?.currentInputConnection
            ?.commitText(stdout.trim(), 1)
    }
}
