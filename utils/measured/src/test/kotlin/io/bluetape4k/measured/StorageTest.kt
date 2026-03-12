package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class StorageTest {

    companion object: KLogging()

    @Test
    fun `저장 용량 단위 변환이 동작한다`() {
        (1.gbytes() `in` Storage.megaBytes).shouldBeEqualTo(1024.0)
        (2048.bytes() `in` Storage.kiloBytes).shouldBeEqualTo(2.0)
    }

    @Test
    fun `저장 용량 toHuman 이 자동 단위를 선택한다`() {
        1536.bytes().toHuman() shouldBeEqualTo "1.5 KB"
    }

    // ----- 단위 변환 -----

    @Test
    fun `저장 용량 in 연산자로 단위 수치를 추출한다`() {
        (1.kbytes() `in` Storage.bytes).shouldBeNear(1024.0, 1e-10)
        (1.mbytes() `in` Storage.kiloBytes).shouldBeNear(1024.0, 1e-10)
        (1.gbytes() `in` Storage.megaBytes).shouldBeNear(1024.0, 1e-10)
        (1.tbytes() `in` Storage.gigaBytes).shouldBeNear(1024.0, 1e-10)
        (1.pbytes() `in` Storage.teraBytes).shouldBeNear(1024.0, 1e-10)
    }

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        // 2048 bytes -> KB
        val bytes = 2048.bytes()
        val asKb = bytes `as` Storage.kiloBytes
        (asKb `in` Storage.kiloBytes).shouldBeNear(2.0, 1e-10)

        // 1 GB -> MB
        val gb = 1.gbytes()
        val asMb = gb `as` Storage.megaBytes
        (asMb `in` Storage.megaBytes).shouldBeNear(1024.0, 1e-10)

        // 1024 MB -> GB
        val mb = 1024.mbytes()
        val asGb = mb `as` Storage.gigaBytes
        (asGb `in` Storage.gigaBytes).shouldBeNear(1.0, 1e-10)
    }

    // ----- 사칙 연산 -----

    @Test
    fun `저장 용량 사칙연산이 동작한다`() {
        val a = 512.0.mbytes()
        val b = 1024.0.mbytes()

        // 덧셈
        (a + a) shouldBeEqualTo b
        // 뺄셈
        (b - a) shouldBeEqualTo a
        // 스칼라 곱셈
        (a * 2) shouldBeEqualTo b
        // 스칼라 나눗셈
        (b / 2) shouldBeEqualTo a
    }

    @Test
    fun `저장 용량 사칙연산 - 다른 단위 혼합`() {
        val kb = 512.kbytes()
        val mb = 1.mbytes()

        val sum = kb + mb
        (sum `in` Storage.kiloBytes).shouldBeNear(1536.0, 1e-8)

        val diff = mb - kb
        (diff `in` Storage.kiloBytes).shouldBeNear(512.0, 1e-8)
    }

    // ----- 비교 -----

    @Test
    fun `저장 용량 비교 연산이 동작한다`() {
        2.gbytes() shouldBeGreaterThan 1.gbytes()
        1.mbytes() shouldBeGreaterThan 1023.kbytes()
        1.kbytes() shouldBeLessThan 1025.bytes()

        // 다른 단위 간 비교
        1.gbytes() shouldBeGreaterThan 1023.mbytes()
        1.tbytes() shouldBeGreaterThan 1023.gbytes()
        512.mbytes() shouldBeLessThan 1.gbytes()
    }

    // ----- 음수 단위 -----

    @Test
    fun `저장 용량 음수 단위가 동작한다`() {
        val positive = 1.gbytes()
        val negative = -positive

        (negative `in` Storage.gigaBytes).shouldBeNear(-1.0, 1e-10)

        val zero = positive + negative
        (zero `in` Storage.gigaBytes).shouldBeNear(0.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `저장 용량 toString 표현이 단위를 포함한다`() {
        1024.0.bytes().toString() shouldBeEqualTo "1024.0 B"
        1.5.kbytes().toString() shouldBeEqualTo "1.5 KB"
        2.0.mbytes().toString() shouldBeEqualTo "2.0 MB"
        1.0.gbytes().toString() shouldBeEqualTo "1.0 GB"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        1024.bytes().toHuman(Storage.kiloBytes) shouldBeEqualTo "1.0 KB"
        1024.kbytes().toHuman(Storage.megaBytes) shouldBeEqualTo "1.0 MB"
        1024.mbytes().toHuman(Storage.gigaBytes) shouldBeEqualTo "1.0 GB"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        512.bytes().toHuman() shouldBeEqualTo "512.0 B"
        1536.bytes().toHuman() shouldBeEqualTo "1.5 KB"
        2048.kbytes().toHuman() shouldBeEqualTo "2.0 MB"
        3072.mbytes().toHuman() shouldBeEqualTo "3.0 GB"
    }
}
