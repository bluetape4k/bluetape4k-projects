package io.bluetape4k.images.vips.testfixtures

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
 * vips 연산 결과를 골든 이미지와 비교하는 테스트 유틸리티.
 *
 * ## 동작 모드
 *
 * - **비교 모드** (기본): 골든 이미지가 존재하면 픽셀 단위로 비교합니다.
 *   골든 이미지가 없으면 [Assumptions.assumeTrue]를 통해 [TestAbortedException]을 throw하여 테스트를 skipped 처리합니다.
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
 * - 읽기: `testFixtures/resources/golden/vips/{key}.png` (클래스패스)
 * - 쓰기: `{user.dir}/../images-vips-api/src/testFixtures/resources/golden/vips/{key}.png`
 *
 * ## diff 이미지 경로
 *
 * 비교 실패 시 `build/reports/golden-diffs/vips/{key}-diff.png`에 실제 이미지를 저장합니다.
 *
 * ## 사용 예시
 *
 * ```kotlin
 * @Test
 * fun `resize 결과가 골든 이미지와 일치한다`() {
 *     val resultBytes: ByteArray = vipsImage.resize(320, 240)
 *     VipsGoldenAssert.assertSimilarToGolden(resultBytes, "resize-320x240")
 * }
 * ```
 */
object VipsGoldenAssert : KLogging() {

    private val UPDATE_MODE =
        System.getProperty("bluetape4k.images.golden.update", "false").toBoolean()

    private const val GOLDEN_BASE = "/golden/vips"

    /**
     * vips 연산 결과 [actualBytes]를 골든 이미지 [key]와 픽셀 단위로 비교합니다.
     *
     * 갱신 모드(`-Dbluetape4k.images.golden.update=true`)에서는 실제 결과를 골든 이미지로 저장한 뒤
     * [TestAbortedException]을 throw합니다.
     *
     * 골든 이미지가 없으면 [org.junit.internal.AssumptionViolatedException]을 throw하여 테스트를 skipped 처리합니다.
     *
     * 픽셀 비교에서 어느 픽셀이든 R/G/B 채널 절대 차이가 [tolerance]를 초과하면 diff 이미지를 저장하고
     * [Assertions.fail]을 호출합니다.
     *
     * @param actualBytes vips 연산 결과 ByteArray
     * @param key 골든 이미지 식별 키 (파일명 제외, 예: "resize-320x240")
     * @param tolerance 허용할 최대 픽셀 채널 절대 차이 (기본값 3)
     * @throws IllegalStateException CI 환경에서 갱신 모드 실행 시
     * @throws TestAbortedException 갱신 모드에서 골든 이미지 저장 완료 시 또는 골든 이미지가 없을 때 (JUnit5 skipped 처리)
     */
    fun assertSimilarToGolden(actualBytes: ByteArray, key: String, tolerance: Int = 3) {
        if (UPDATE_MODE) {
            val isCI = System.getenv("CI") != null
            if (isCI) {
                throw IllegalStateException("갱신 모드는 CI 환경에서 실행할 수 없습니다. key=$key")
            }
            saveGolden(actualBytes, key)
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

        val actual = ImmutableImageLoader.create().fromBytes(actualBytes)
        compareImages(actual, golden, key, tolerance)
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * 클래스패스에서 골든 이미지를 로드합니다.
     *
     * @param key 골든 이미지 식별 키
     * @return 골든 [ImmutableImage], 리소스가 없으면 `null`
     */
    private fun loadGolden(key: String): ImmutableImage? {
        val stream = VipsGoldenAssert::class.java.getResourceAsStream("$GOLDEN_BASE/$key.png")
            ?: return null
        return ImmutableImageLoader.create().fromStream(stream)
    }

    /**
     * 골든 이미지를 소스 트리에 저장합니다 (갱신 모드 전용).
     *
     * 저장 경로: `{user.dir}/../images-vips-api/src/testFixtures/resources/golden/vips/{key}.png`
     *
     * @param bytes 저장할 원본 ByteArray
     * @param key 골든 이미지 식별 키
     */
    private fun saveGolden(bytes: ByteArray, key: String) {
        val targetPath = resolveGoldenWritePath(key)
        Files.createDirectories(targetPath.parent)
        val image = ImmutableImageLoader.create().fromBytes(bytes)
        image.forWriter(PngWriter.MaxCompression).write(targetPath)
        log.debug { "골든 이미지 저장 완료: $targetPath" }
    }

    /**
     * 갱신 모드에서 골든 이미지를 저장할 절대 경로를 반환합니다.
     *
     * @param key 골든 이미지 식별 키
     * @return 정규화된 저장 경로
     */
    private fun resolveGoldenWritePath(key: String): Path {
        val userDir = System.getProperty("user.dir")
        log.debug { "user.dir=$userDir (갱신 모드 골든 경로 계산)" }
        return Paths.get(
            userDir, "..", "images-vips-api",
            "src", "testFixtures", "resources", "golden", "vips", "$key.png"
        ).normalize()
    }

    /**
     * 두 이미지를 픽셀 단위로 비교합니다.
     *
     * 크기가 다르거나 R/G/B 채널 절대 차이가 [tolerance]를 초과하는 픽셀이 있으면
     * diff 이미지를 저장하고 [Assertions.fail]을 호출합니다.
     *
     * @param actual 실제 이미지
     * @param expected 기대(골든) 이미지
     * @param key 골든 이미지 식별 키 (diff 파일명에 사용)
     * @param tolerance 허용할 최대 채널 절대 차이
     */
    private fun compareImages(
        actual: ImmutableImage,
        expected: ImmutableImage,
        key: String,
        tolerance: Int,
    ) {
        if (actual.width != expected.width || actual.height != expected.height) {
            saveDiff(key, actual, expected)
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
                saveDiff(key, actual, expected)
                Assertions.fail<Unit>(
                    "픽셀 ($x, $y) 에서 허용 오차($tolerance) 초과: " +
                        "actual=(${a.red()},${a.green()},${a.blue()}) " +
                        "expected=(${e.red()},${e.green()},${e.blue()}) " +
                        "delta=(dr=$dr, dg=$dg, db=$db) key=$key"
                )
            }
        }
    }

    /**
     * 비교 실패 시 실제 이미지를 diff 디렉토리에 저장합니다.
     *
     * 저장 경로: `build/reports/golden-diffs/vips/{key}-diff.png`
     *
     * @param key 골든 이미지 식별 키
     * @param actual 실제 이미지
     * @param expected 기대(골든) 이미지 (현재는 미사용, 향후 side-by-side 확장 예정)
     */
    private fun saveDiff(key: String, actual: ImmutableImage, expected: ImmutableImage) {
        val userDir = System.getProperty("user.dir")
        val diffDir = Paths.get(userDir, "build", "reports", "golden-diffs", "vips")
        Files.createDirectories(diffDir)
        val diffPath = diffDir.resolve("$key-diff.png")
        actual.forWriter(PngWriter.MaxCompression).write(diffPath)
        log.warn { "골든 diff 저장: $diffPath" }
    }
}
