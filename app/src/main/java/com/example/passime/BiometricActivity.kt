package com.example.passime

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricActivity : FragmentActivity() {

    companion object {
        const val ACTION_FETCH = "fetch"
        const val ACTION_GENERATE = "generate"

        const val EXTRA_ACTION = "action"
    }

    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.getStringExtra(EXTRA_ACTION)

        if (
            action != ACTION_FETCH &&
            action != ACTION_GENERATE
        ) {
            closeToBackground()
            return
        }

        val biometricManager = BiometricManager.from(this)

        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG

        if (
            biometricManager.canAuthenticate(authenticators) !=
                BiometricManager.BIOMETRIC_SUCCESS
        ) {
            closeToBackground()
            return
        }

        executor = mainExecutor

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("PassIme")
            .setSubtitle("Authentication required")
            .setDescription(
                if (action == ACTION_FETCH) {
                    "Authenticate to fetch your password"
                } else {
                    "Authenticate to generate a password"
                }
            )
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    val service = PassImeService.instance

                    if (service != null) {
                        if (action == ACTION_FETCH) {
                            service.performPasswordFetch()
                        } else {
                            service.openGeneratePasswordPrompt()
                        }
                    }

                    finish()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    closeToBackground()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    private fun closeToBackground() {
        finish()
        moveTaskToBack(true)
    }
}
