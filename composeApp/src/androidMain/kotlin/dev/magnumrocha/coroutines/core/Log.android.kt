package dev.magnumrocha.coroutines.core

import android.util.Log

actual fun debugLog(tag: String, message: String) {
    Log.d(tag, message)
}
