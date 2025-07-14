package io.bluetape4k.collections

import io.bluetape4k.logging.KLogging

abstract class AbstractCollectionTest {

    companion object: KLogging() {
        const val SIZE = 1000
        const val REPEAT = 10
        const val WARMUP_ITERATIONS = 5
        const val MEASURE_ITERATIONS = 5
    }
}
