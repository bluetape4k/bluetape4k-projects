package io.bluetape4k.images.vips.java25.property

import io.bluetape4k.images.vips.VipsImageFormat
import io.bluetape4k.images.vips.java25.AbstractFfmVipsTest
import io.bluetape4k.images.vips.java25.ffmVipsImageOf
import io.bluetape4k.images.vips.testfixtures.VipsTestFixtures
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * vips-ffm 이미지 연산의 불변식(property-based) 테스트.
 *
 * JPEG, PNG, WebP 모든 입력 포맷에 대해 아래 불변식을 검증합니다:
 * 1. resize(320, 240) 후 width ≤ 320 && height ≤ 240
 * 2. thumbnail(128) 후 max(width, height) ≤ 128
 * 3. toBytes(PNG) 결과 바이트 길이 > 0
 * 4. toBytes(JPEG) 결과 바이트 길이 > 0
 * 5. resize 연산 후 원본 치수 불변 — 별도 인스턴스에서 원본 치수 재확인
 */
class VipsPropertyTest : AbstractFfmVipsTest() {

    companion object : KLogging() {

        @JvmStatic
        fun allFixtures(): Stream<String> = Stream.of(
            VipsTestFixtures.SAMPLE_JPEG,
            VipsTestFixtures.SAMPLE_PNG,
            VipsTestFixtures.SAMPLE_WEBP,
        )
    }

    // ─── 불변식 1: resize(320, 240) → width ≤ 320, height ≤ 240 ──────────────

    @ParameterizedTest(name = "resize(320,240) 불변식 — {0}")
    @MethodSource("allFixtures")
    fun `resize 후 치수가 지정 경계를 초과하지 않는다`(fixturePath: String) {
        val bytes = VipsTestFixtures.loadFixture(fixturePath)
        ffmVipsImageOf(bytes).use { img ->
            img.resize(320, 240).use { resized ->
                resized.width shouldBeLessOrEqualTo 320
                resized.height shouldBeLessOrEqualTo 240
            }
        }
    }

    // ─── 불변식 2: thumbnail(128) → max(width, height) ≤ 128 ─────────────────

    @ParameterizedTest(name = "thumbnail(128) 불변식 — {0}")
    @MethodSource("allFixtures")
    fun `thumbnail 후 긴 변이 maxDimension을 초과하지 않는다`(fixturePath: String) {
        val bytes = VipsTestFixtures.loadFixture(fixturePath)
        ffmVipsImageOf(bytes).use { img ->
            img.thumbnail(128).use { thumb ->
                maxOf(thumb.width, thumb.height) shouldBeLessOrEqualTo 128
            }
        }
    }

    // ─── 불변식 3: toBytes(PNG) 결과 비어 있지 않음 ───────────────────────────

    @ParameterizedTest(name = "toBytes(PNG) 길이 > 0 — {0}")
    @MethodSource("allFixtures")
    fun `PNG 인코딩 결과 바이트 길이가 0보다 크다`(fixturePath: String) {
        val bytes = VipsTestFixtures.loadFixture(fixturePath)
        ffmVipsImageOf(bytes).use { img ->
            val resultBytes = img.toBytes(VipsImageFormat.PNG)
            resultBytes.size shouldBeGreaterThan 0
        }
    }

    // ─── 불변식 4: toBytes(JPEG) 결과 비어 있지 않음 ──────────────────────────

    @ParameterizedTest(name = "toBytes(JPEG) 길이 > 0 — {0}")
    @MethodSource("allFixtures")
    fun `JPEG 인코딩 결과 바이트 길이가 0보다 크다`(fixturePath: String) {
        val bytes = VipsTestFixtures.loadFixture(fixturePath)
        ffmVipsImageOf(bytes).use { img ->
            val resultBytes = img.toBytes(VipsImageFormat.JPEG)
            resultBytes.size shouldBeGreaterThan 0
        }
    }

    // ─── 불변식 5: resize 연산이 원본 치수를 변경하지 않음 ────────────────────

    @ParameterizedTest(name = "원본 치수 불변 — {0}")
    @MethodSource("allFixtures")
    fun `resize 연산 후 원본 이미지의 치수는 변경되지 않는다`(fixturePath: String) {
        val bytes = VipsTestFixtures.loadFixture(fixturePath)
        ffmVipsImageOf(bytes).use { original ->
            val originalWidth = original.width
            val originalHeight = original.height

            // resize 는 새 인스턴스를 반환하므로 원본은 영향 받지 않아야 함
            original.resize(100, 100).use { /* 새 인스턴스 생성 후 즉시 닫음 */ }

            original.width shouldBeEqualTo originalWidth
            original.height shouldBeEqualTo originalHeight
        }
    }
}
