package io.bluetape4k.tink.keyset

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class VersionedTinkDaeadTest {

    companion object : KLogging()

    private fun newVersionedDaead() = VersionedTinkDaead(InMemoryVersionedDaeadKeysetStore())

    @Test
    fun `바이트 배열 encryptDeterministically decryptDeterministically 라운드트립`() {
        val vd = newVersionedDaead()
        val plaintext = "Hello, VersionedTinkDaead!".toByteArray()
        val ciphertext = vd.encryptDeterministically(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        vd.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `문자열 encryptDeterministically decryptDeterministically 라운드트립`() {
        val vd = newVersionedDaead()
        val plaintext = "결정적 버전 암호화 테스트"
        val encrypted = vd.encryptDeterministically(plaintext)

        encrypted shouldNotBeEqualTo plaintext
        vd.decryptDeterministically(encrypted) shouldBeEqualTo plaintext
    }

    @Test
    fun `같은 버전에서 동일 평문은 동일 암호문 생성 (결정적 특성)`() {
        val vd = newVersionedDaead()
        val plaintext = "동일 결과 검색 필드".toByteArray()

        val ct1 = vd.encryptDeterministically(plaintext)
        val ct2 = vd.encryptDeterministically(plaintext)

        ct1 shouldBeEqualTo ct2
    }

    @Test
    fun `associatedData를 포함한 라운드트립`() {
        val vd = newVersionedDaead()
        val plaintext = "AD 포함 결정적 암호화".toByteArray()
        val ad = "table=users,column=email".toByteArray()

        val ciphertext = vd.encryptDeterministically(plaintext, ad)
        vd.decryptDeterministically(ciphertext, ad) shouldBeEqualTo plaintext
    }

    @Test
    fun `currentVersion 은 1부터 시작`() {
        val vd = newVersionedDaead()
        vd.currentVersion() shouldBeEqualTo 1L
    }

    @Test
    fun `rotate 후 currentVersion 증가`() {
        val vd = newVersionedDaead()
        vd.currentVersion() // 초기화
        val newHandle = vd.rotate()

        (newHandle.version > 1L).shouldBeTrue()
        (vd.currentVersion() >= 2L).shouldBeTrue()
    }

    @Test
    fun `rotate 후 이전 버전 암호문 복호화 가능`() {
        val store = InMemoryVersionedDaeadKeysetStore()
        val vd = VersionedTinkDaead(store)

        val plaintext = "이전 버전 결정적 암호화".toByteArray()
        val oldCiphertext = vd.encryptDeterministically(plaintext)

        vd.rotate()

        // 이전 버전 암호문도 복호화 가능 (store에 이전 keyset 유지)
        vd.decryptDeterministically(oldCiphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `알 수 없는 버전으로 복호화시 예외 발생`() {
        val vd = newVersionedDaead()
        val fakePayload = packVersionedCiphertext(99L, "garbage".toByteArray())

        assertThrows<IllegalArgumentException> {
            vd.decryptDeterministically(fakePayload)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "이메일@예시.com", "주민등록번호-123456"])
    fun `다양한 문자열 결정적 encrypt decrypt 라운드트립`(plaintext: String) {
        val vd = newVersionedDaead()
        val encrypted = vd.encryptDeterministically(plaintext)
        vd.decryptDeterministically(encrypted) shouldBeEqualTo plaintext
    }

    @Test
    fun `rotateIfDue - 기간 미경과시 회전 없음`() {
        val vd = newVersionedDaead()
        val before = vd.currentVersion()
        vd.rotateIfDue(Duration.ofDays(30))
        vd.currentVersion() shouldBeEqualTo before
    }
}
