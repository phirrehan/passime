package com.example.passime

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import java.security.SecureRandom

private enum class KeyboardMode {
    QWERTY,
    SYMBOLS,
    FUNCTIONS
}

private var executionId = 0

class PassImeService : InputMethodService() {

    companion object {
        var instance: PassImeService? = null
    }

    private var keyboardMode = KeyboardMode.QWERTY
    private var shiftEnabled = false

    var pendingAuthToken: String? = null
    var pendingPassword: String? = null

    private val handler = Handler(Looper.getMainLooper())

    private val keys = mapOf(
        R.id.keyQ to "q",
        R.id.keyW to "w",
        R.id.keyE to "e",
        R.id.keyR to "r",
        R.id.keyT to "t",
        R.id.keyY to "y",
        R.id.keyU to "u",
        R.id.keyI to "i",
        R.id.keyO to "o",
        R.id.keyP to "p",
        R.id.keyA to "a",
        R.id.keyS to "s",
        R.id.keyD to "d",
        R.id.keyF to "f",
        R.id.keyG to "g",
        R.id.keyH to "h",
        R.id.keyJ to "j",
        R.id.keyK to "k",
        R.id.keyL to "l",
        R.id.keyZ to "z",
        R.id.keyX to "x",
        R.id.keyC to "c",
        R.id.keyV to "v",
        R.id.keyB to "b",
        R.id.keyN to "n",
        R.id.keyM to "m"
    )

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        pendingPassword = null
        pendingAuthToken = null
        instance = null

        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.ime, null)

        keyboardMode = KeyboardMode.QWERTY
        shiftEnabled = false

        setupKeyboard(view)

        return view
    }

    fun openGeneratePasswordPrompt() {
        val intent = Intent(
            this,
            GeneratePasswordActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
    }

    private fun authenticateAndFetch() {
        val intent = Intent(
            this,
            BiometricActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(
                BiometricActivity.EXTRA_ACTION,
                BiometricActivity.ACTION_FETCH
            )
        }

        startActivity(intent)
    }

    private fun authenticateAndGenerate() {
        val intent = Intent(
            this,
            BiometricActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(
                BiometricActivity.EXTRA_ACTION,
                BiometricActivity.ACTION_GENERATE
            )
        }

        startActivity(intent)
    }

    fun performPasswordFetch() {
        requestPasswordFromTermux()
    }

    private fun setupKeyboard(view: View) {
        setupNumberKeys(
            view,
            listOf(
                R.id.key1,
                R.id.key2,
                R.id.key3,
                R.id.key4,
                R.id.key5,
                R.id.key6,
                R.id.key7,
                R.id.key8,
                R.id.key9,
                R.id.key0
            )
        )

        setupAlphabetKeys(view)
        setupSpecialKeys(view)
        setupSpacebar(view)
        setupBackspaceKeys(view)
        setupKeyboardModes(view)

        updateKeyboardState(view)
        switchKeyboardMode(view, KeyboardMode.QWERTY)
    }

    private fun setupAlphabetKeys(view: View) {
        for ((id, character) in keys) {
            val button = view.findViewById<Button>(id)

            button.setOnClickListener {
                val text = if (shiftEnabled) {
                    character.uppercase()
                } else {
                    character
                }

                currentInputConnection?.commitText(text, 1)

                if (shiftEnabled) {
                    shiftEnabled = false
                    updateKeyLabels(view)
                    updateShiftButton(view)
                }
            }
        }

        view.findViewById<Button>(R.id.shiftButton).setOnClickListener {
            shiftEnabled = !shiftEnabled

            updateKeyLabels(view)
            updateShiftButton(view)
        }
    }

    private fun setupSpecialKeys(view: View) {
        view.findViewById<Button>(R.id.dotButton).setOnClickListener {
            currentInputConnection?.commitText(".", 1)
        }

        view.findViewById<Button>(R.id.enterButton).setOnClickListener {
            currentInputConnection?.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_ENTER
                )
            )

            currentInputConnection?.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_ENTER
                )
            )
        }
    }

    private fun setupKeyboardModes(view: View) {
        setupSymbols(view)
        setupFunctionKeyboard(view)
    }

    private fun updateKeyboardState(view: View) {
        updateKeyLabels(view)
        updateShiftButton(view)
    }

    private fun switchKeyboardMode(
        view: View,
        mode: KeyboardMode
    ) {
        view.findViewById<View>(R.id.qwertyLayout).visibility =
            if (mode == KeyboardMode.QWERTY) {
                View.VISIBLE
            } else {
                View.GONE
            }

        view.findViewById<View>(R.id.symbolLayout).visibility =
            if (mode == KeyboardMode.SYMBOLS) {
                View.VISIBLE
            } else {
                View.GONE
            }

        view.findViewById<View>(R.id.functionLayout).visibility =
            if (mode == KeyboardMode.FUNCTIONS) {
                View.VISIBLE
            } else {
                View.GONE
            }

        keyboardMode = mode

        val symbolButton = view.findViewById<Button>(R.id.symbolButton)
        val functionButton = view.findViewById<Button>(R.id.functionButton)

        symbolButton.text =
            if (mode == KeyboardMode.SYMBOLS) "ABC" else "?123"

        functionButton.text =
            if (mode == KeyboardMode.FUNCTIONS) "ABC" else "PASS"
    }

    private fun updateKeyLabels(view: View) {
        for ((id, character) in keys) {
            val button = view.findViewById<Button>(id)

            button.text = if (shiftEnabled) {
                character.uppercase()
            } else {
                character
            }
        }
    }

    private fun updateShiftButton(view: View) {
        view.findViewById<Button>(R.id.shiftButton).isSelected = shiftEnabled
    }

    private fun setupSpacebar(view: View) {
        val spaceButton = view.findViewById<Button>(R.id.spaceButton)

        var longPressed = false

        val openPicker = Runnable {
            longPressed = true

            val imm = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

            imm.showInputMethodPicker()
        }

        spaceButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longPressed = false
                    handler.postDelayed(openPicker, 200)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(openPicker)

                    if (!longPressed) {
                        currentInputConnection?.commitText(" ", 1)
                    }

                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(openPicker)
                    true
                }

                else -> false
            }
        }
    }

    private fun deleteCharacter() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun setupBackspace(button: Button) {
        var deleting = false

        val deleteRunnable = object : Runnable {
            override fun run() {
                deleteCharacter()

                if (deleting) {
                    handler.postDelayed(this, 50)
                }
            }
        }

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    deleting = true
                    deleteCharacter()
                    handler.postDelayed(deleteRunnable, 400)
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    deleting = false
                    handler.removeCallbacks(deleteRunnable)
                    true
                }

                else -> false
            }
        }
    }

    private fun setupBackspaceKeys(view: View) {
        setupBackspace(
            view.findViewById(R.id.backspaceButton)
        )

        setupBackspace(
            view.findViewById(R.id.symbolBackspaceButton)
        )
    }

    private fun setupNumberKeys(
        view: View,
        ids: List<Int>
    ) {
        val numbers = listOf(
            "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "0"
        )

        for (i in numbers.indices) {
            val button = view.findViewById<Button>(ids[i])

            button.setOnClickListener {
                currentInputConnection?.commitText(numbers[i], 1)
            }
        }
    }

    private fun setupSymbols(view: View) {
        setupNumberKeys(
            view,
            listOf(
                R.id.symbol1,
                R.id.symbol2,
                R.id.symbol3,
                R.id.symbol4,
                R.id.symbol5,
                R.id.symbol6,
                R.id.symbol7,
                R.id.symbol8,
                R.id.symbol9,
                R.id.symbol0
            )
        )

        val symbols = mapOf(
            R.id.symbolExclamation to "!",
            R.id.symbolAt to "@",
            R.id.symbolHash to "#",
            R.id.symbolDollar to "$",
            R.id.symbolPercent to "%",
            R.id.symbolCaret to "^",
            R.id.symbolAmpersand to "&",
            R.id.symbolAsterisk to "*",
            R.id.symbolParenLeft to "(",
            R.id.symbolParenRight to ")",

            R.id.symbolMinus to "-",
            R.id.symbolUnderscore to "_",
            R.id.symbolEquals to "=",
            R.id.symbolPlus to "+",
            R.id.symbolBracketLeft to "[",
            R.id.symbolBracketRight to "]",
            R.id.symbolBraceLeft to "{",
            R.id.symbolBraceRight to "}",
            R.id.symbolBackslash to "\\",
            R.id.symbolPipe to "|",

            R.id.symbolSemicolon to ";",
            R.id.symbolColon to ":",
            R.id.symbolApostrophe to "'",
            R.id.symbolQuote to "\"",
            R.id.symbolComma to ",",
            R.id.symbolDot to ".",
            R.id.symbolSlash to "/",
            R.id.symbolLessThan to "<",
            R.id.symbolGreaterThan to ">"
        )

        for ((id, symbol) in symbols) {
            val button = view.findViewById<Button>(id)

            button.setOnClickListener {
                currentInputConnection?.commitText(symbol, 1)
            }
        }

        val symbolButton = view.findViewById<Button>(R.id.symbolButton)

        symbolButton.setOnClickListener {
            val nextMode =
                if (keyboardMode == KeyboardMode.SYMBOLS) {
                    KeyboardMode.QWERTY
                } else {
                    KeyboardMode.SYMBOLS
                }

            switchKeyboardMode(view, nextMode)
        }
    }

    private fun setupFunctionKeyboard(view: View) {
        val functionButton = view.findViewById<Button>(R.id.functionButton)

        functionButton.setOnClickListener {
            val nextMode =
                if (keyboardMode == KeyboardMode.FUNCTIONS) {
                    KeyboardMode.QWERTY
                } else {
                    KeyboardMode.FUNCTIONS
                }

            switchKeyboardMode(view, nextMode)
        }

        view.findViewById<Button>(R.id.fetchPasswordButton)
            .setOnClickListener {
                authenticateAndFetch()
            }

        view.findViewById<Button>(R.id.generatePasswordButton)
            .setOnClickListener {
                authenticateAndGenerate()
            }

        view.findViewById<Button>(R.id.commitPasswordButton)
            .setOnClickListener {
                val password = pendingPassword
                    ?: return@setOnClickListener

                currentInputConnection?.commitText(
                    password,
                    1
                )

                pendingPassword = null
            }
    }

    private fun requestPasswordFromTermux() {
        executionId++

        val token = generateAuthToken()
        pendingAuthToken = token

        val resultIntent = Intent(
            this,
            TermuxResultReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            executionId,
            resultIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
        )

        val intent = Intent().apply {
            setClassName(
                "com.termux",
                "com.termux.app.RunCommandService"
            )

            action = "com.termux.RUN_COMMAND"

            putExtra(
                "com.termux.RUN_COMMAND_PATH",
                "/data/data/com.termux/files/usr/bin/bash"
            )

            putExtra(
                "com.termux.RUN_COMMAND_ARGUMENTS",
                arrayOf(
                    "/data/data/com.termux/files/home/.local/bin/passget.sh",
                    token
                )
            )

            putExtra(
                "com.termux.RUN_COMMAND_WORKDIR",
                "/data/data/com.termux/files/home"
            )

            putExtra(
                "com.termux.RUN_COMMAND_BACKGROUND",
                false
            )

            putExtra(
                "com.termux.RUN_COMMAND_SESSION_ACTION",
                "0"
            )

            putExtra(
                "com.termux.RUN_COMMAND_PENDING_INTENT",
                pendingIntent
            )
        }

        try {
            startService(intent)

            handler.postDelayed({
                openTermuxActivity()
            }, 200)
        } catch (_: Exception) {
            pendingAuthToken = null
        }
    }

    fun startPasswordGeneration(
        passwordName: String,
        passwordLength: String
    ) {
        executionId++

        val token = generateAuthToken()
        pendingAuthToken = token

        runPassgen(
            token,
            passwordName,
            passwordLength
        )
    }

    private fun runPassgen(
        token: String,
        passwordName: String,
        passwordLength: String
    ) {
        val resultIntent = Intent(
            this,
            TermuxResultReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            executionId,
            resultIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
        )

        val intent = Intent().apply {
            setClassName(
                "com.termux",
                "com.termux.app.RunCommandService"
            )

            action = "com.termux.RUN_COMMAND"

            putExtra(
                "com.termux.RUN_COMMAND_PATH",
                "/data/data/com.termux/files/usr/bin/bash"
            )

            putExtra(
                "com.termux.RUN_COMMAND_ARGUMENTS",
                arrayOf(
                    "/data/data/com.termux/files/home/.local/bin/passgen.sh",
                    token,
                    passwordName,
                    passwordLength
                )
            )

            putExtra(
                "com.termux.RUN_COMMAND_WORKDIR",
                "/data/data/com.termux/files/home"
            )

            putExtra(
                "com.termux.RUN_COMMAND_BACKGROUND",
                false
            )

            putExtra(
                "com.termux.RUN_COMMAND_SESSION_ACTION",
                "0"
            )

            putExtra(
                "com.termux.RUN_COMMAND_PENDING_INTENT",
                pendingIntent
            )
        }

        try {
            startService(intent)

            handler.postDelayed({
                openTermuxActivity()
            }, 200)
        } catch (_: Exception) {
            pendingAuthToken = null
        }
    }

    private fun openTermuxActivity() {
        val intent = Intent().apply {
            setClassName(
                "com.termux",
                "com.termux.app.TermuxActivity"
            )

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            // TermuxActivity could not be launched.
        }
    }

    private fun generateAuthToken(): String {
        val bytes = ByteArray(32)

        SecureRandom().nextBytes(bytes)

        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or
                Base64.NO_PADDING or
                Base64.URL_SAFE
        )
    }

    fun restoreOriginalAppAndCommit(password: String) {
        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)
                ?: return

        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )

        startActivity(launchIntent)

        handler.postDelayed({
            currentInputConnection?.commitText(
                password,
                1
            )
        }, 500)
    }
}
