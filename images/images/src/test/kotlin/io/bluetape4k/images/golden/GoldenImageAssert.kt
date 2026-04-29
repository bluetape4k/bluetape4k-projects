package io.bluetape4k.images.golden

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import com.sksamuel.scrimage.nio.PngWriter
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.abs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.opentest4j.TestAbortedException

/**
 * scrimage [ImmutableImage] 연산 결과를 골든 이미지와 비교하는 테스트 유틸리티.
 *
 * ## 동작 모드
 *
 * - **비교 모드** (기본): 골든 이미지가 존재하면 픽셀 단위로 비교합니다.
 *   골든 이미지가 없으면 [Assumptions.assumeTrue]로 테스트를 skipped 처리합니다.
 * - **갱신 모드**: `-Dbluetape4k.images.golden.update=true` 시스템 프로퍼티로 활성화합니다.
 *   실제 결과를 골든 이미지로 저장한 뒤 [TestAbortedException]을 throw하여 테스트를 skipped 처리합니다.
 *
 * ## CI 가드
 *
 * CI 환경(`CI` 환경변수 존재)에서는 갱신 모드 실행을 금지합니다.
 * 갱신 모드 + CI 감지 시 [IllegalStateException]을 throw합니다.
 *
 * ## 골든 이미지 경로
 *
 * - 읽기: `src/test/resources/golden/images/{key}.png` (클래스패스 `/golden/images/{key}.png`)
 * - 쓰기: `{user.dir}/src/test/resources/golden/images/{key}.png` (갱신 모드 시)
 *
 * ## diff 이미지 경로
 *
 * 비교 실패 시 `build/reports/golden-diffs/images/{key}-diff.png`에 실제 이미지를 저장합니다.
 *
 * ## 사용 예시
 *
 * ```kotlin
 * @Test
 * fun `resize 결과가 골든 이미지와 일치한다`() {
 *     val result: ImmutableImage = original.scaleTo(320, 240)
 *     GoldenImageAssert.assertSimilarToGolden(result, "resize-320x240")
 * }
 * ```
 */
object GoldenImageAssert : KLogging() {

    private val UPDATE_MODE =
        System.getProperty("bluetape4k.images.golden.update", "false").toBoolean()

    private const val GOLDEN_BASE = "/golden/images"

    /**
     * [actual] 이미지를 골든 이미지 [key]와 픽셀 단위로 비교합니다.
     *
     * 갱신 모드(`-Dbluetape4k.images.golden.update=true`)에서는 실제 이미지를 골든으로 저장한 뒤
     * [TestAbortedException]을 throw합니다.
     *
     * 골든 이미지가 없으면 [Assumptions.assumeTrue]로 테스트를 skipped 처리합니다.
     *
     * @param actual 실제 이미지
     * @param key 골든 이미지 식별 키 (파일명 제외, 예: "resize-320x240")
     * @param tolerance 허용할 최대 픽셀 채널 절대 차이 (기본값 3)
     * @throws IllegalStateException CI 환경에서 갱신 모드 실행 시
     * @throws TestAbortedException 갱신 모드에서 골든 저장 완료 시 또는 골든 이미지가 없을 때 (JUnit5 skipped)
     */
    fun assertSimilarToGolden(actual: ImmutableImage, key: String, tolerance: Int = 3) {
        if (UPDATE_MODE) {
            check(System.getenv("CI") == null) {
                "갱신 모드는 CI 환경에서 실행할 수 없습니다. key=$key"
            }
            saveGolden(actual, key)
            throw TestAbortedException("골든 이미지 갱신됨: $key — 다시 실행하면 비교 모드로 동작합니다")
        }

        val golden = loadGolden(key)
            ?: run {
                Assumptions.assumeTrue(
                    false,
                    "골든 이미지 없음: $key. 갱신 모드(-Dbluetape4k.images.golden.update=true)로 실행하여 먼저 생성하세요."
                )
                return
            }

        compareImages(actual, golden, key, tolerance)
    }

    /**
     * [actualBytes] (ByteArray)를 골든 이미지 [key]와 픽셀 단위로 비교합니다.
     *
     * scrimage [ImmutableImageLoader]로 bytes를 디코딩한 뒤 [assertSimilarToGolden]을 호출합니다.
     *
     * @param actualBytes 실제 결과 바이트 배열
     * @param key 골든 이미지 식별 키
     * @param tolerance 허용할 최대 픽셀 채널 절대 차이 (기본값 3)
     */
    fun assertSimilarToGolden(actualBytes: ByteArray, key: String, tolerance: Int = 3) {
        val actual = ImmutableImageLoader.create().fromBytes(actualBytes)
        assertSimilarToGolden(actual, key, tolerance)
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────────────────────────────────────

    private fun loadGolden(key: String): ImmutableImage? {
        val stream = GoldenImageAssert::class.java.getResourceAsStream("$GOLDEN_BASE/$key.png")
            ?: return null
        return ImmutableImageLoader.create().fromStream(stream)
    }

    private fun saveGolden(image: ImmutableImage, key: String) {
        val targetPath = resolveGoldenWritePath(key)
        Files.createDirectories(targetPath.parent)
        image.forWriter(PngWriter.MaxCompression).write(targetPath)
        log.debug { "골든 이미지 저장 완료: $targetPath" }
    }

    private fun resolveGoldenWritePath(key: String): Path {
        val userDir = System.getProperty("user.dir")
        return Paths.get(userDir, "src", "test", "resources", "golden", "images", "$key.png").normalize()
    }

    private fun compareImages(actual: ImmutableImage, expected: ImmutableImage, key: String, tolerance: Int) {
        if (actual.width != expected.width || actual.height != expected.height) {
            saveDiff(key, actual)
            Assertions.fail<Unit>(
                "골든 이미지와 크기 불일치: actual=(${actual.width}x${actual.height}) " +
                    "expected=(${expected.width}x${expected.height}) key=$key"
            )
        }

        val actualPixels = actual.pixels()
        val expectedPixels = expected.pixels()

        for (i in actualPixels.indices) {
            val a = actualPixels[i]
            val e = expectedPixels[i]
            val dr = abs(a.red() - e.red())
            val dg = abs(a.green() - e.green())
            val db = abs(a.blue() - e.blue())
            if (dr > tolerance || dg > tolerance || db > tolerance) {
                val x = i % actual.width
                val y = i / actual.width
                saveDiff(key, actual)
                Assertions.fail<Unit>(
                    "픽셀 ($x, $y) 에서 허용 오차($tolerance) 초과: " +
                        "actual=(${a.red()},${a.green()},${a.blue()}) " +
                        "expected=(${e.red()},${e.green()},${e.blue()}) " +
                        "delta=(dr=$dr, dg=$dg, db=$db) key=$key"
                )
            }
        }
    }

    private fun saveDiff(key: String, actual: ImmutableImage) {
        val userDir = System.getProperty("user.dir")
        val diffDir = Paths.get(userDir, "build", "reports", "golden-diffs", "images")
        Files.createDirectories(diffDir)
        val diffPath = diffDir.resolve("$key-diff.png")
        actual.forWriter(PngWriter.MaxCompression).write(diffPath)
        log.warn { "골든 diff 저장: $diffPath" }
    }
}
