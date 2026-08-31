package com.example.passime

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.inputmethodservice.InputMethodService
import android.util.Base64
import java.security.SecureRandom

private const val TERMUX_PENDING_INTENT = "pendingIntent"

private var executionId = 0

class PassImeService : InputMethodService() {


    companion object {
        var instance: PassImeService? = null
    }

    /*
     * Shift state.
     */
    private var shiftEnabled = false

    /*
     * One-time authentication token used for communication
     * between PassIme and Termux.
     */
    var pendingAuthToken: String? = null
    var pendingPassword: String? = null
    /*
     * Handler used for long-press actions.
     */
    private val handler =
        Handler(Looper.getMainLooper())

    /*
     * All letter keys.
     *
     * This is a class-level property so every function
     * in this service can access it.
     */
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

        val view =
            layoutInflater.inflate(
                R.layout.ime,
                null
            )

        setupKeyboard(view)

        return view
    }


    override fun onStartInput(
        attribute: android.view.inputmethod.EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInput(
            attribute,
            restarting
        )

    }


    /*
     * =========================================================
     * KEYBOARD SETUP
     * =========================================================
     */

    private fun setupKeyboard(view: View) {

        /*
         * Letter keys
         */
        for ((id, character) in keys) {

            val button =
                view.findViewById<Button>(id)

            button.text = character

            button.setOnClickListener {

                val text =
                    if (shiftEnabled) {
                        character.uppercase()
                    } else {
                        character
                    }

                currentInputConnection?.commitText(
                    text,
                    1
                )

                /*
                 * Shift behaves like a normal one-shot
                 * keyboard shift.
                 */
                if (shiftEnabled) {

                    shiftEnabled = false

                    updateKeyLabels(view)
                    updateShiftButton(view)
                }
            }
        }


        /*
         * Shift
         */
        view.findViewById<Button>(
            R.id.shiftButton
        ).setOnClickListener {

            shiftEnabled =
                !shiftEnabled

            updateKeyLabels(view)
            updateShiftButton(view)
        }

        view.findViewById<Button>(
            R.id.passButton
        ).setOnClickListener {
          val packageName =     currentInputEditorInfo?.packageName
            requestFromTermux()
        }
        val passwordButton =
    view.findViewById<Button>(R.id.passwordButton)

passwordButton.setOnClickListener {

    val password = pendingPassword

    if (password != null) {

        currentInputConnection?.commitText(
            password,
            1
        )

        // Clear immediately after insertion.
        pendingPassword = null
    }
}

        /*
         * Spacebar
         *
         * Tap  -> insert space
         * Hold -> open IME picker
         */
        setupSpacebar(view)

        view.findViewById<Button>(
            R.id.dotButton
        ).setOnClickListener {
            currentInputConnection?.commitText(".", 1)
        }

        /*
         * Enter
         */
        view.findViewById<Button>(
            R.id.enterButton
        ).setOnClickListener {

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


        /*
         * Backspace
         *
         * Tap  -> delete one character
         * Hold -> repeatedly delete characters
         */
        setupBackspace(
            view.findViewById(R.id.backspaceButton)
        )

        setupBackspace(
            view.findViewById(R.id.symbolBackspaceButton)
        )

        setupSymbols(view)

        /*
         * Make sure the initial state is correct.
         */
        updateKeyLabels(view)
        updateShiftButton(view)
    }


    /*
     * =========================================================
     * LETTER LABELS
     * =========================================================
     */

    private fun updateKeyLabels(view: View) {

        for ((id, character) in keys) {

            val button =
                view.findViewById<Button>(id)

            button.text =
                if (shiftEnabled) {
                    character.uppercase()
                } else {
                    character
                }
        }
    }


    /*
     * =========================================================
     * SHIFT VISUAL STATE
     * =========================================================
     */

private fun updateShiftButton(view: View) {

    val shiftButton =
        view.findViewById<Button>(
            R.id.shiftButton
        )
}


    /*
     * =========================================================
     * SPACEBAR
     * =========================================================
     */

    private fun setupSpacebar(view: View) {

        val spaceButton =
            view.findViewById<Button>(
                R.id.spaceButton
            )

        var longPressed = false

        val openPicker =
            Runnable {

                longPressed = true

                val imm =
                    getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager

                imm.showInputMethodPicker()
            }


        spaceButton.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    longPressed = false

                    handler.postDelayed(
                        openPicker,
                        200
                    )

                    true
                }


                MotionEvent.ACTION_UP -> {

                    handler.removeCallbacks(
                        openPicker
                    )

                    /*
                     * Only insert a space if this wasn't
                     * a long press.
                     */
                    if (!longPressed) {

                        currentInputConnection
                            ?.commitText(
                                " ",
                                1
                            )
                    }

                    true
                }


                MotionEvent.ACTION_CANCEL -> {

                    handler.removeCallbacks(
                        openPicker
                    )

                    true
                }


                else -> false
            }
        }
    }


    /*
     * =========================================================
     * BACKSPACE
     * =========================================================
     */

private fun deleteCharacter() {
    currentInputConnection?.deleteSurroundingText(1, 0)
}

private fun setupBackspace(button: Button) {

    var deleting = false
    val handler = android.os.Handler(mainLooper)

    val deleteRunnable = object : Runnable {
        override fun run() {
            if (!deleting) return

            deleteCharacter()
            handler.postDelayed(this, 75)
        }
    }

    button.setOnClickListener {
        deleteCharacter()
    }

    button.setOnLongClickListener {

        deleting = true

        // Delete immediately once, then begin repeating.
        deleteCharacter()

        handler.postDelayed(deleteRunnable, 400)

        true
    }

    button.setOnTouchListener { _, event ->

        when (event.action) {

            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {

                deleting = false
                handler.removeCallbacks(deleteRunnable)
            }
        }

        false
    }
}

private fun setupSymbols(view: View) {

    val qwertyLayout =
        view.findViewById<View>(R.id.qwertyLayout)

    val symbolLayout =
        view.findViewById<View>(R.id.symbolLayout)

    val symbolButton =
        view.findViewById<Button>(R.id.symbolButton)

    val symbols: Map<Int, String> = mapOf(
        R.id.symbol1 to "1",
        R.id.symbol2 to "2",
        R.id.symbol3 to "3",
        R.id.symbol4 to "4",
        R.id.symbol5 to "5",
        R.id.symbol6 to "6",
        R.id.symbol7 to "7",
        R.id.symbol8 to "8",
        R.id.symbol9 to "9",
        R.id.symbol0 to "0",

        R.id.symbolAt to "@",
        R.id.symbolHash to "#",
        R.id.symbolDollar to "$",
        R.id.symbolPercent to "%",
        R.id.symbolAmpersand to "&",
        R.id.symbolAsterisk to "*",
        R.id.symbolMinus to "-",
        R.id.symbolPlus to "+",
        R.id.symbolParenLeft to "(",
        R.id.symbolParenRight to ")",

        R.id.symbolExclamation to "!",
        R.id.symbolQuestion to "?",
        R.id.symbolQuote to "\"",
        R.id.symbolApostrophe to "'",
        R.id.symbolColon to ":",
        R.id.symbolSemicolon to ";",
        R.id.symbolComma to ",",
        R.id.symbolSlash to "/",
        R.id.symbolBackslash to "\\"
    )

    for ((id, symbol) in symbols) {

        val button =
            view.findViewById<Button>(id)

        button.setOnClickListener {
            currentInputConnection?.commitText(symbol, 1)
        }
    }

    symbolButton.setOnClickListener {

        if (qwertyLayout.visibility == View.VISIBLE) {

            qwertyLayout.visibility = View.GONE
            symbolLayout.visibility = View.VISIBLE

            symbolButton.text = "ABC"

        } else {

            qwertyLayout.visibility = View.VISIBLE
            symbolLayout.visibility = View.GONE

            symbolButton.text = "?123"
        }
    }
}

    /*
     * =========================================================
     * TERMUX COMMUNICATION
     * =========================================================
     */

    private fun requestFromTermux() {

   executionId++

    val token = generateAuthToken()
    pendingAuthToken = token

        val resultIntent =
            Intent(
                this,
                TermuxResultReceiver::class.java
            )

        val pendingIntent =
            PendingIntent.getBroadcast(
                this,
                executionId,
                resultIntent,
                PendingIntent.FLAG_ONE_SHOT or
                        PendingIntent.FLAG_MUTABLE
            )


        /*
         * Ask Termux to run our password script.
         */
        val intent =
            Intent().apply {

                setClassName(
                    "com.termux",
                    "com.termux.app.RunCommandService"
                )

                action =
                    "com.termux.RUN_COMMAND"


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


                /*
                 * We deliberately launch an actual
                 * interactive Termux terminal because
                 * fzf and GPG require user interaction.
                 */
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

Handler(Looper.getMainLooper()).postDelayed({

    val termuxIntent = Intent().apply {
        setClassName(
            "com.termux",
            "com.termux.app.TermuxActivity"
        )

        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )
    }

    try {
        startActivity(termuxIntent)
    } catch (e: Exception) {
    }

}, 200)

} catch (e: Exception) {
    pendingAuthToken = null

}
    }


    /*
     * =========================================================
     * AUTHENTICATION TOKEN
     * =========================================================
     */

    private fun generateAuthToken(): String {

        val bytes =
            ByteArray(32)

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

    Handler(Looper.getMainLooper()).postDelayed({

        currentInputConnection?.commitText(password, 1)

    }, 500)
}
}
