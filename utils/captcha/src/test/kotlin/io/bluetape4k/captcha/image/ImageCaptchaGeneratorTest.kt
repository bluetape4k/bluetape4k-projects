package io.bluetape4k.captcha.image

import io.bluetape4k.captcha.AbstractCaptchaTest
import io.bluetape4k.captcha.CaptchaCodeGenerator
import io.bluetape4k.captcha.config.CaptchaConfig
import io.bluetape4k.captcha.config.CaptchaTheme
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.nio.file.Path

@TempFolderTest
class ImageCaptchaGeneratorTest: AbstractCaptchaTest() {

    companion object: KLogging()

    private val config = CaptchaConfig(
        width = 200,
        height = 80,
        length = 6,
        noiseCount = 5,
        theme = CaptchaTheme.DARK,
        darkBackgroundColor = Color.BLACK,
        lightBackgroundColor = Color.WHITE
    )

    private val codeGenerator = mockk<CaptchaCodeGenerator>()
    private lateinit var captchaGenerator: ImageCaptchaGenerator

    private val useTempFolder: Boolean = true

    @BeforeEach
    fun beforeEach() {
        clearMocks(codeGenerator)
        captchaGenerator = ImageCaptchaGenerator(config, codeGenerator)
    }

    @Test
    fun `주어진 code에 해당하는 이미지 Captcha 생성`(tempFolder: TempFolder) {
        val code = "ABC123"
        every { codeGenerator.next(config.length) } returns code

        val captcha = captchaGenerator.generate()

        captcha.content.shouldNotBeNull()
        captcha.code shouldBeEqualTo code

        captcha.toByteArray().shouldNotBeNull()

        val path = if (useTempFolder) {
            tempFolder.createFile().toPath()
        } else {
            Path.of("./captcha.webp")
        }
        captcha.writeToFile(path)
    }

    @Test
    fun `대문자를 랜덤 코드로 Image Captcha 생성`(tempFolder: TempFolder) {
        val newConfig = config.copy(noiseCount = 6)
        val codeGen = CaptchaCodeGenerator(symbols = CaptchaCodeGenerator.UPPER)
        val captchaGen = ImageCaptchaGenerator(newConfig, codeGen)

        repeat(10) {
            val captcha = captchaGen.generate()
            captcha.content.shouldNotBeNull()

            val path = if (useTempFolder) {
                tempFolder.createFile().toPath()
            } else {
                Path.of("./captcha-upper-$it.webp")
            }
            captcha.writeToFile(path)
        }
    }


    @Test
    fun `대문자와 숫자를 랜덤 코드로 Image Captcha 생성`(tempFolder: TempFolder) {
        val newConfig = config.copy(noiseCount = 6, theme = CaptchaTheme.LIGHT)
        val codeGen = CaptchaCodeGenerator(symbols = CaptchaCodeGenerator.UPPER_DIGITS)
        val captchaGen = ImageCaptchaGenerator(newConfig, codeGen)

        repeat(10) {
            val captcha = captchaGen.generate()
            captcha.content.shouldNotBeNull()

            val path = if (useTempFolder) {
                tempFolder.createFile().toPath()
            } else {
                Path.of("./captcha-upper-digits-$it.webp")
            }

            captcha.writeToFile(path)
        }
    }
}
