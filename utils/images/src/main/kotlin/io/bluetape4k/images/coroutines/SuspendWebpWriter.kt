package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.webp.WebpWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * Coroutines 방식으로 WebP 형식의 이미지를 생성하는 [SuspendImageWriter] 입니다.
 *
 * 사진보다는 일러스트같은 이미지 형태에 대해서 압축률이 상당히 좋다.
 * JPG와 비교하여 처리하는 데는 시간이 더 걸리지만, 압축률은 2배 이상 좋다
 *
 * **NOTE: 동적인 처리 시에는 사용하지 않는 것을 추천합니다.**
 *
 * ```kotlin
 * val writer = SuspendWebpWriter()
 * val image = immutableImageOf(File("image.webp"))
 * writer.write(image, File("output.webp"))
 * // output.webp 파일 생성됨
 * ```
 */
class SuspendWebpWriter(
    private val z: Int = -1,
    private val q: Int = -1,
    private val m: Int = -1,
    private val lossless: Boolean = false,
    private val noAlpha: Boolean = false,
): WebpWriter(z, q, m, lossless, noAlpha), SuspendImageWriter {

    companion object: KLoggingChannel() {
        @JvmStatic
        val Default = SuspendWebpWriter()

        /**
         * Max lossless compression - 압축에 많은 시간이 걸린다. 배치 작업 시 사용하기 좋다.
         */
        @JvmStatic
        val MaxLosslessCompression = Default.withZ(9)
    }

    /**
     * 무손실(lossless) 압축을 활성화한 새 [SuspendWebpWriter]를 반환합니다.
     *
     * @return 무손실 압축이 활성화된 [SuspendWebpWriter]
     */
    override fun withLossless(): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, true, noAlpha)
    }

    /**
     * 알파 채널을 제외한 새 [SuspendWebpWriter]를 반환합니다.
     *
     * @return 알파 채널이 비활성화된 [SuspendWebpWriter]
     */
    override fun withoutAlpha(): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, lossless, true)
    }

    /**
     * 품질(Quality) 값을 설정한 새 [SuspendWebpWriter]를 반환합니다.
     *
     * @param q 품질 값 (0~100, -1이면 기본값)
     * @return 품질이 설정된 [SuspendWebpWriter]
     */
    override fun withQ(q: Int): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, lossless, noAlpha)
    }

    /**
     * 압축 방법(Method) 값을 설정한 새 [SuspendWebpWriter]를 반환합니다.
     *
     * @param m 압축 방법 값 (0~6, -1이면 기본값)
     * @return 압축 방법이 설정된 [SuspendWebpWriter]
     */
    override fun withM(m: Int): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, lossless, noAlpha)
    }

    /**
     * 무손실 압축 레벨(Z) 값을 설정한 새 [SuspendWebpWriter]를 반환합니다.
     *
     * @param z 무손실 압축 레벨 (0~9, -1이면 기본값)
     * @return 무손실 압축 레벨이 설정된 [SuspendWebpWriter]
     */
    override fun withZ(z: Int): SuspendWebpWriter {
        return SuspendWebpWriter(z, q, m, lossless, noAlpha)
    }
}
