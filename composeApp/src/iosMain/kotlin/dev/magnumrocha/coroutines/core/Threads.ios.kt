package dev.magnumrocha.coroutines.core

import platform.Foundation.NSThread
import kotlin.time.Duration

actual fun threadSleep(duration: Duration) {
    NSThread.sleepForTimeInterval(duration.inWholeMicroseconds / 1_000_000.0)
}
