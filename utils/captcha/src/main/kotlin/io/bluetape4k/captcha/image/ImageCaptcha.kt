package io.bluetape4k.captcha.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImageWriter
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.webp.WebpWriter
import io.bluetape4k.captcha.Captcha
import java.nio.file.Path

/**
 * 이미지 기반 Captcha
 *
 * ## 동작/계약
 * - [code]는 정답 문자열, [content]는 렌더링된 불변 이미지입니다.
 * - 값 객체로서 새 인스턴스로 전달/직렬화됩니다.
 *
 * ```kotlin
 * val captcha: ImageCaptcha = generator.generate()
 * // captcha.code.isNotBlank() == true
 * ```
 *
 * @property code Captcha 코드
 * @property content Captcha 이미지
 */
data class ImageCaptcha(
    override val code: String,
    override val content: ImmutableImage,
): Captcha<ImmutableImage>


/**
 * Captcha 이미지를 변환하는 기본 [ImageWriter] 인스턴스 (기본값: [WEBP_IMAGE_WRITER], [WebpWriter])
 */
internal val DEFAULT_IMAGE_WRITER by lazy { WEBP_IMAGE_WRITER }

/**
 * Captcha 이미지를 JPEG로 변환하는 [ImageWriter] 인스턴스 (see [JpegWriter])
 */
internal val JPG_IMAGE_WRITER by lazy { JpegWriter() }

/**
 * Captcha 이미지를 WebP로 변환하는 [ImageWriter] 인스턴스 (see [WebpWriter])
 */
internal val WEBP_IMAGE_WRITER by lazy { WebpWriter() }

/**
 * [ImageCaptcha]의 이미지를 원하는 포맷의 ByteArray로 변환합니다.
 *
 * ## 동작/계약
 * - [writer]가 지정한 인코더 포맷으로 새 바이트 배열을 생성합니다.
 * - 원본 이미지 객체를 mutate하지 않습니다.
 *
 * ```kotlin
 * val bytes = captcha.toByteArray()
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param writer 이미지 정보를 특정 포맷으로 변환하는 [ImageWriter] 인스턴스 (기본값: [DEFAULT_IMAGE_WRITER])
 * @return 변환된 ByteArray
 */
fun Captcha<ImmutableImage>.toByteArray(writer: ImageWriter = DEFAULT_IMAGE_WRITER): ByteArray {
    return content.bytes(writer)

}

/**
 * [ImageCaptcha]의 이미지를 원하는 포맷의 파일로 지정한 경로 [path]에 저장합니다.
 *
 * ## 동작/계약
 * - [path] 위치에 인코딩된 이미지 파일을 씁니다.
 * - 파일 시스템 I/O 오류는 예외로 전파됩니다.
 *
 * ```kotlin
 * captcha.toFile(path)
 * // path.toFile().exists() == true
 * ```
 *
 * @param path 저장할 파일의 경로
 * @param writer 이미지 정보를 특정 포맷으로 변환하는 [ImageWriter] 인스턴스 (기본값: [DEFAULT_IMAGE_WRITER])
 * @return 저장된 파일의 경로
 */
fun Captcha<ImmutableImage>.writeToFile(path: Path, writer: ImageWriter = DEFAULT_IMAGE_WRITER): Path {
    return content.output(writer, path)
}
