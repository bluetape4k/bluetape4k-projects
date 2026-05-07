package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.idgenerators.ulid.utils.MaxRandomPart
import io.bluetape4k.idgenerators.ulid.utils.MaxTimestampPart
import io.bluetape4k.idgenerators.ulid.utils.MinRandomPart
import io.bluetape4k.idgenerators.ulid.utils.PastTimestampPart
import io.bluetape4k.idgenerators.ulid.utils.partsOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.assertions.shouldBeLessOrEqualTo

abstract class AbstractULIDTest {
    companion object: KLogging() {
        const val REPEAT_SIZE = 5
    }

    protected fun assertValidParts(ulidStr: String) {
        log.debug { "random ULID=$ulidStr" }
        ulidStr.length shouldBeEqualTo 26
        val (timePart, randomPart) = partsOf(ulidStr)
        timePart shouldBeGreaterThan PastTimestampPart
        timePart shouldBeLessOrEqualTo MaxTimestampPart
        randomPart shouldBeInRange (MinRandomPart..MaxRandomPart)
    }
}
