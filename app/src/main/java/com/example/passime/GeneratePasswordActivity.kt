package com.example.passime

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class GeneratePasswordActivity : Activity() {

    private fun closeToBackground() {
        finish()
        moveTaskToBack(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_password)

        val passwordNameInput = findViewById<EditText>(R.id.passwordNameInput)
        val passwordLengthInput = findViewById<EditText>(R.id.passwordLengthInput)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val generateButton = findViewById<Button>(R.id.generateButton)

        cancelButton.setOnClickListener {
            closeToBackground()
        }

        generateButton.setOnClickListener {
            val passwordName = passwordNameInput.text.toString().trim()
            val passwordLength = passwordLengthInput.text.toString().trim()

            if (passwordName.isEmpty()) {
                passwordNameInput.error = "Enter a password name"
                passwordNameInput.requestFocus()
                return@setOnClickListener
            }

            val length = passwordLength.toIntOrNull()

            if (length == null || length <= 0) {
                passwordLengthInput.error = "Enter a valid length"
                passwordLengthInput.requestFocus()
                return@setOnClickListener
            }

            val service = PassImeService.instance

            if (service == null) {
                Toast.makeText(
                    this,
                    "PassIme service is not running",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            service.startPasswordGeneration(
                passwordName,
                passwordLength
            )

            closeToBackground()
        }
    }
}
