package io.bluetape4k.idgenerators.ulid

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import java.util.*

class JavaUUIDSupportTest : AbstractULIDTest() {
    @RepeatedTest(REPEAT_SIZE)
    fun `test ULID to UUID round-trip`() {
        val ulid = ULID.nextULID()
        val uuid = ulid.toUUID()
        val roundTripped = ULID.fromUUID(uuid)
        roundTripped shouldBeEqualTo ulid
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `test UUID to ULID round-trip`() {
        val uuid = UUID.randomUUID()
        val ulid = ULID.fromUUID(uuid)
        val roundTripped = ulid.toUUID()
        roundTripped shouldBeEqualTo uuid
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `test toUUID preserves bits`() {
        val ulid = ULID.nextULID()
        val uuid = ulid.toUUID()

        uuid.mostSignificantBits shouldBeEqualTo ulid.mostSignificantBits
        uuid.leastSignificantBits shouldBeEqualTo ulid.leastSignificantBits
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `test fromUUID preserves bits`() {
        val uuid = UUID.randomUUID()
        val ulid = ULID.fromUUID(uuid)

        ulid.mostSignificantBits shouldBeEqualTo uuid.mostSignificantBits
        ulid.leastSignificantBits shouldBeEqualTo uuid.leastSignificantBits
    }
}
