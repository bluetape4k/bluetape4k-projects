package io.bluetape4k.tink.keyset

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.AbstractTinkTest
import io.bluetape4k.tink.aead.TinkAead
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.daead.TinkDeterministicAead
import io.bluetape4k.tink.daeadKeysetHandle
import io.bluetape4k.tink.mac.TinkMac
import io.bluetape4k.tink.macKeysetHandle
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class TinkKeysetJsonSupportTest: AbstractTinkTest() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `AEAD KeysetHandle JSON 직렬화 후 복원하면 동일 키 사용`() {
        val original = aeadKeysetHandle()
        val json = original.toJsonKeyset()

        json.shouldNotBeEmpty()

        val restored = keysetHandleOf(json)
        val aead1 = TinkAead(original)
        val aead2 = TinkAead(restored)

        val plaintext = faker.lorem().paragraph()
        val ciphertext = aead1.encrypt(plaintext)

        // 복원된 키셋으로 복호화 가능
        aead2.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DAEAD KeysetHandle JSON 직렬화 후 복원하면 동일 키 사용`() {
        val original = daeadKeysetHandle()
        val json = original.toJsonKeyset()

        json.shouldNotBeEmpty()

        val restored = keysetHandleOf(json)
        val daead1 = TinkDeterministicAead(original)
        val daead2 = TinkDeterministicAead(restored)

        val plaintext = faker.lorem().paragraph()
        val ciphertext = daead1.encryptDeterministically(plaintext)

        daead2.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `MAC KeysetHandle JSON 직렬화 후 복원하면 동일 키 사용`() {
        val original = macKeysetHandle()
        val json = original.toJsonKeyset()

        json.shouldNotBeEmpty()

        val restored = keysetHandleOf(json)
        val mac1 = TinkMac(original)
        val mac2 = TinkMac(restored)

        val data = faker.lorem().paragraph()
        val tag = mac1.computeMac(data)

        // 복원된 키셋으로 MAC 검증 가능
        mac2.verifyMac(tag, data).shouldBeTrue()
    }

    @Test
    fun `JSON 형식은 유효한 JSON 문자열`() {
        val handle = aeadKeysetHandle()
        val json = handle.toJsonKeyset()

        // Tink JSON keyset은 중괄호로 시작
        json.trimStart().startsWith("{").shouldBeTrue()
    }

    @Test
    fun `빈 문자열로 keysetHandleOf 호출시 예외 발생`() {
        runCatching {
            keysetHandleOf("")
        }.isFailure.shouldBeTrue()
    }
}
