package dev.magnumrocha.coroutines.core

import java.lang.Thread
import kotlin.time.Duration

actual fun threadSleep(duration: Duration) {
    Thread.sleep(duration.inWholeMilliseconds)
}
