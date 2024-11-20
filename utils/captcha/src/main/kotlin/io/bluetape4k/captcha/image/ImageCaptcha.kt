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
 * @param writer 이미지 정보를 특정 포맷으로 변환하는 [ImageWriter] 인스턴스 (기본값: [DEFAULT_IMAGE_WRITER])
 * @return 변환된 ByteArray
 */
fun Captcha<ImmutableImage>.toByteArray(writer: ImageWriter = DEFAULT_IMAGE_WRITER): ByteArray {
    return content.bytes(writer)

}

/**
 * [ImageCaptcha]의 이미지를 원하는 포맷의 파일로 지정한 경로 [path]에 저장합니다.
 *
 * @param path 저장할 파일의 경로
 * @param writer 이미지 정보를 특정 포맷으로 변환하는 [ImageWriter] 인스턴스 (기본값: [DEFAULT_IMAGE_WRITER])
 * @return 저장된 파일의 경로
 */
fun Captcha<ImmutableImage>.writeToFile(path: Path, writer: ImageWriter = DEFAULT_IMAGE_WRITER): Path {
    return content.output(writer, path)
}
