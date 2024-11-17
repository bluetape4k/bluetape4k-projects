package io.bluetape4k.coroutines.flow.extensions.subject

import kotlinx.coroutines.delay

suspend fun <T> SubjectApi<T>.awaitCollector() {
    while (!hasCollectors) {
        delay(1)
    }
}

suspend fun <T> SubjectApi<T>.awaitCollectors(minCollectorCount: Int = 1) {
    val limit = minCollectorCount.coerceAtLeast(1)
    while (collectorCount < limit) {
        delay(1)
    }
}

fun <L, R> areEqualAsAny(left: L, right: R): Boolean =
    (left as Any == right as Any)
