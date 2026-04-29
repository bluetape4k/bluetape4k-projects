package io.bluetape4k.images.vips.java25.golden

import io.bluetape4k.images.vips.VipsImageFormat
import io.bluetape4k.images.vips.java25.AbstractFfmVipsTest
import io.bluetape4k.images.vips.java25.ffmVipsImageOf
import io.bluetape4k.images.vips.testfixtures.VipsGoldenAssert
import io.bluetape4k.images.vips.testfixtures.VipsTestFixtures
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE

/**
 * vips-ffm resize/thumbnail 연산의 골든 이미지 비교 테스트.
 *
 * 골든 이미지가 없으면 테스트를 skipped 처리합니다.
 * 갱신 모드 실행은 `-Dbluetape4k.images.golden.update=true`로 활성화하며,
 * java25가 골든 이미지의 마스터 소스이므로 갱신 메서드는 항상 @EnabledForJreRange(min = JRE.JAVA_25)로 보호합니다.
 */
class VipsGoldenResizeTest : AbstractFfmVipsTest() {

    companion object : KLogging()

    // ─── 비교 테스트 ───────────────────────────────────────────────────────────

    @Test
    fun `resize 320x240 결과가 골든 이미지와 일치한다`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.resize(320, 240).use { resized ->
                val resultBytes = resized.toBytes(VipsImageFormat.PNG)
                VipsGoldenAssert.assertSimilarToGolden(resultBytes, "vips-resize-320x240")
            }
        }
    }

    @Test
    fun `thumbnail 128 결과가 골든 이미지와 일치한다`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.thumbnail(128).use { thumb ->
                val resultBytes = thumb.toBytes(VipsImageFormat.PNG)
                VipsGoldenAssert.assertSimilarToGolden(resultBytes, "vips-thumbnail-128")
            }
        }
    }

    @Test
    fun `resize 400x300 결과가 골든 이미지와 일치한다`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.resize(400, 300).use { resized ->
                val resultBytes = resized.toBytes(VipsImageFormat.PNG)
                VipsGoldenAssert.assertSimilarToGolden(resultBytes, "vips-resize-fit-400x300")
            }
        }
    }

    // ─── 갱신 모드 (Java 25 전용) ──────────────────────────────────────────────

    /**
     * 골든 이미지 갱신용 테스트.
     * java25가 골든 이미지의 마스터 소스이므로 Java 25에서만 실행 가능하며,
     * -Dbluetape4k.images.golden.update=true 가 필요합니다.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_25)
    fun `골든 이미지 갱신 - resize 320x240`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.resize(320, 240).use { resized ->
                val resultBytes = resized.toBytes(VipsImageFormat.PNG)
                VipsGoldenAssert.assertSimilarToGolden(resultBytes, "vips-resize-320x240")
            }
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_25)
    fun `골든 이미지 갱신 - thumbnail 128`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.thumbnail(128).use { thumb ->
                val resultBytes = thumb.toBytes(VipsImageFormat.PNG)
                VipsGoldenAssert.assertSimilarToGolden(resultBytes, "vips-thumbnail-128")
            }
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_25)
    fun `골든 이미지 갱신 - resize 400x300`() {
        val bytes = VipsTestFixtures.loadFixture(VipsTestFixtures.SAMPLE_JPEG)
        ffmVipsImageOf(bytes).use { img ->
            img.resize(400, 300).use { resized ->
                val resultBytes = resized.toBytes(VipsImageFormat.PNG)
                VipsGoldenAssert.assertSimilarToGolden(resultBytes, "vips-resize-fit-400x300")
            }
        }
    }
}
