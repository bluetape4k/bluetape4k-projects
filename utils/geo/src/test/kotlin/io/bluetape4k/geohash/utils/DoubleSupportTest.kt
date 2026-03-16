package io.bluetape4k.geohash.utils

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class DoubleSupportTest {

    companion object: KLogging() {
        private const val DELTA = 1e-12
    }

    @Test
    fun `get remainder with fix from positive value`() {
        58.1541.remainderWithFix(360).shouldBeNear(58.1541, DELTA)
        453.1541.remainderWithFix(360).shouldBeNear(93.1541, DELTA)
    }

    @Test
    fun `get remainder with fix from negative value`() {
        (-58.1541).remainderWithFix(360).shouldBeNear(301.8459, DELTA)
        (-453.1541).remainderWithFix(360).shouldBeNear(266.8459, DELTA)
    }
}
