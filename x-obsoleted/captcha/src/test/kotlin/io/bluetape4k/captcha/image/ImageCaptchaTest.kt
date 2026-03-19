package io.bluetape4k.captcha.image

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.captcha.AbstractCaptchaTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class ImageCaptchaTest: AbstractCaptchaTest() {

    @Test
    fun `ImageCaptcha 인스턴스 생성`() {
        val code = "ABC123"
        val image = ImmutableImage.create(100, 50, BufferedImage.TYPE_INT_RGB)

        val captcha = ImageCaptcha(code, image)

        captcha.code shouldBeEqualTo code
        captcha.content shouldBeEqualTo image
    }

    @Test
    fun `ImageCaptcha 를 이미지 포맷의 ByteArray로 변환`() {
        val code = "ABC123"
        val image = ImmutableImage.create(100, 50, BufferedImage.TYPE_INT_RGB)

        val captcha = ImageCaptcha(code, image)


        // JPEG 포맷으로 변환
        captcha.toByteArray(JPG_IMAGE_WRITER).shouldNotBeEmpty()

        // WEBP 포맷으로 변환
        captcha.toByteArray(WEBP_IMAGE_WRITER).shouldNotBeEmpty()
    }
}
