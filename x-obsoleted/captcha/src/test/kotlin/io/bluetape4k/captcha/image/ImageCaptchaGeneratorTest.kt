package io.bluetape4k.captcha.image

import io.bluetape4k.captcha.AbstractCaptchaTest
import io.bluetape4k.captcha.CaptchaCodeGenerator
import io.bluetape4k.captcha.config.CaptchaConfig
import io.bluetape4k.captcha.config.CaptchaTheme
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
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

    private lateinit var tempFolder: TempFolder
    private val useTempFolder: Boolean = true

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @BeforeEach
    fun beforeEach() {
        clearMocks(codeGenerator)
        captchaGenerator = ImageCaptchaGenerator(config, codeGenerator)
    }

    @Test
    fun `주어진 code에 해당하는 이미지 Captcha 생성`() {
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
    fun `대문자를 랜덤 코드로 Image Captcha 생성`() {
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
    fun `대문자와 숫자를 랜덤 코드로 Image Captcha 생성`() {
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

    @Test
    fun `멀티 스레딩 환경에서 대문자와 숫자를 랜덤 코드로 Image Captcha 생성`() {
        val newConfig = config.copy(noiseCount = 6, theme = CaptchaTheme.LIGHT)
        val codeGen = CaptchaCodeGenerator(symbols = CaptchaCodeGenerator.UPPER_DIGITS)
        val captchaGen = ImageCaptchaGenerator(newConfig, codeGen)

        MultithreadingTester()
            .workers(Runtime.getRuntime().availableProcessors() * 4)
            .rounds(4)
            .add {
                val captcha = captchaGen.generate()
                captcha.content.shouldNotBeNull()

                val suffix = Base58.randomString(12)
                val path = if (useTempFolder) {
                    tempFolder.createFile().toPath()
                } else {
                    Path.of("./captcha-upper-digits-$suffix.webp")
                }

                captcha.writeToFile(path)
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `버추얼 스레딩 환경에서 대문자와 숫자를 랜덤 코드로 Image Captcha 생성`() {
        val newConfig = config.copy(noiseCount = 6, theme = CaptchaTheme.LIGHT)
        val codeGen = CaptchaCodeGenerator(symbols = CaptchaCodeGenerator.UPPER_DIGITS)
        val captchaGen = ImageCaptchaGenerator(newConfig, codeGen)

        StructuredTaskScopeTester()
            .rounds(Runtime.getRuntime().availableProcessors() * 4 * 4)
            .add {
                val captcha = captchaGen.generate()
                captcha.content.shouldNotBeNull()

                val suffix = Base58.randomString(12)
                val path = if (useTempFolder) {
                    tempFolder.createFile().toPath()
                } else {
                    Path.of("./captcha-upper-digits-$suffix.webp")
                }

                captcha.writeToFile(path)
            }
            .run()
    }

    @Test
    fun `코루틴 환경에서 대문자와 숫자를 랜덤 코드로 Image Captcha 생성`(tempFolder: TempFolder) = runSuspendIO {
        val newConfig = config.copy(noiseCount = 6, theme = CaptchaTheme.LIGHT)
        val codeGen = CaptchaCodeGenerator(symbols = CaptchaCodeGenerator.UPPER_DIGITS)
        val captchaGen = ImageCaptchaGenerator(newConfig, codeGen)

        SuspendedJobTester()
            .workers(Runtime.getRuntime().availableProcessors() * 4)
            .rounds(Runtime.getRuntime().availableProcessors() * 4 * 4)
            .add {
                val captcha = captchaGen.generate()
                captcha.content.shouldNotBeNull()

                val suffix = Base58.randomString(12)
                val path = if (useTempFolder) {
                    tempFolder.createFile().toPath()
                } else {
                    Path.of("./captcha-upper-digits-$suffix.webp")
                }

                captcha.writeToFile(path)
            }
            .run()
    }
}
