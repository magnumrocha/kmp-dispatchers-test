package dev.magnumrocha.coroutines.core

import platform.Foundation.NSLog

actual fun debugLog(tag: String, message: String) {
    NSLog("%s: %s", tag, message)
}
