package io.bluetape4k.idgenerators.ulid

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class KotlinUuidSupportTest : AbstractULIDTest() {
    @RepeatedTest(REPEAT_SIZE)
    fun `test ULID to UUID round-trip`() {
        val ulid = ULID.nextULID()
        val uuid = ulid.toUuid()
        val roundTripped = ULID.fromUuid(uuid)
        roundTripped shouldBeEqualTo ulid
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `test UUID to ULID round-trip`() {
        val uuid = Uuid.generateV7()
        val ulid = ULID.fromUuid(uuid)
        val roundTripped = ulid.toUuid()
        roundTripped shouldBeEqualTo uuid
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `test toUUID preserves bits`() {
        val ulid = ULID.nextULID()
        val uuid = ulid.toUuid()

        uuid.toByteArray() shouldBeEqualTo ulid.toByteArray()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `test fromUUID preserves bits`() {
        val uuid = Uuid.generateV4()
        val ulid = ULID.fromUuid(uuid)

        ulid.toByteArray() shouldBeEqualTo uuid.toByteArray()
    }
}
