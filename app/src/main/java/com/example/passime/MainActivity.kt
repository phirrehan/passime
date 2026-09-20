package com.example.passime

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(
            R.color.keyboard_background
        )

        setContentView(R.layout.activity_main)
    }
}
