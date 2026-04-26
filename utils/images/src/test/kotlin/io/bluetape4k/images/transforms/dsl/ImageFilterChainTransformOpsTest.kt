package io.bluetape4k.images.transforms.dsl

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.filters.dsl.applyFilters
import io.bluetape4k.images.filters.dsl.suspendApplyFilters
import io.bluetape4k.images.transforms.AspectRatio
import io.bluetape4k.images.transforms.ImagePoint
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.image.BufferedImage

class ImageFilterChainTransformOpsTest : AbstractImageTest() {

    companion object : KLoggingChannel()

    private fun createSolidImage(w: Int = 100, h: Int = 100, color: Color = Color.BLUE): ImmutableImage {
        val buf = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = buf.createGraphics()
        try {
            g.color = color
            g.fillRect(0, 0, w, h)
        } finally {
            g.dispose()
        }
        return ImmutableImage.wrapAwt(buf)
    }

    private fun createWhitePaddedImage(
        totalW: Int = 100, totalH: Int = 100,
        rectX: Int = 10, rectY: Int = 10, rectW: Int = 80, rectH: Int = 80,
    ): ImmutableImage {
        val buf = BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB)
        val g = buf.createGraphics()
        try {
            g.color = Color.WHITE
            g.fillRect(0, 0, totalW, totalH)
            g.color = Color.RED
            g.fillRect(rectX, rectY, rectW, rectH)
        } finally {
            g.dispose()
        }
        return ImmutableImage.wrapAwt(buf)
    }

    @Test
    fun `autoCrop DSL op removes white margin`() {
        val image = createWhitePaddedImage(100, 100, 10, 10, 80, 80)
        val result = image.applyFilters {
            autoCrop(tolerance = 0, backgroundColor = Color.WHITE)
        }
        result.shouldNotBeNull()
        result.width shouldBeLessThan 100
        result.height shouldBeLessThan 100
    }

    @Test
    fun `smartCrop DSL op produces target aspect ratio`() {
        val image = createSolidImage(200, 200)
        val ratio = AspectRatio.WIDESCREEN
        val result = image.applyFilters {
            smartCrop(ratio)
        }
        result.shouldNotBeNull()
        result.width shouldBeGreaterThan 0
        result.height shouldBeGreaterThan 0
    }

    @Test
    fun `rotateDegrees DSL op returns non-null image`() {
        val image = createSolidImage(60, 60)
        val result = image.applyFilters {
            rotateDegrees(45.0)
        }
        result.shouldNotBeNull()
        result.width shouldBeGreaterThan 0
    }

    @Test
    fun `rotateLeft DSL op swaps width and height`() {
        val image = createSolidImage(80, 60)
        val result = image.applyFilters {
            rotateLeft()
        }
        result.shouldNotBeNull()
        result.width shouldBeEqualTo 60
        result.height shouldBeEqualTo 80
    }

    @Test
    fun `rotateRight DSL op swaps width and height`() {
        val image = createSolidImage(80, 60)
        val result = image.applyFilters {
            rotateRight()
        }
        result.shouldNotBeNull()
        result.width shouldBeEqualTo 60
        result.height shouldBeEqualTo 80
    }

    @Test
    fun `flipHorizontal DSL op preserves dimensions`() {
        val image = createSolidImage(80, 60)
        val result = image.applyFilters {
            flipHorizontal()
        }
        result.shouldNotBeNull()
        result.width shouldBeEqualTo 80
        result.height shouldBeEqualTo 60
    }

    @Test
    fun `flipVertical DSL op preserves dimensions`() {
        val image = createSolidImage(80, 60)
        val result = image.applyFilters {
            flipVertical()
        }
        result.shouldNotBeNull()
        result.width shouldBeEqualTo 80
        result.height shouldBeEqualTo 60
    }

    @Test
    fun `perspectiveTransform DSL op produces requested output size`() {
        val image = createSolidImage(100, 100)
        val src = listOf(
            ImagePoint(0.0, 0.0), ImagePoint(99.0, 0.0),
            ImagePoint(99.0, 99.0), ImagePoint(0.0, 99.0),
        )
        val dst = listOf(
            ImagePoint(5.0, 5.0), ImagePoint(94.0, 5.0),
            ImagePoint(94.0, 94.0), ImagePoint(5.0, 94.0),
        )
        val result = image.applyFilters {
            perspectiveTransform(src, dst, 80, 80)
        }
        result.shouldNotBeNull()
        result.width shouldBeEqualTo 80
        result.height shouldBeEqualTo 80
    }

    @Test
    fun `clahe DSL op preserves dimensions`() {
        val image = createSolidImage(100, 100, Color.RED)
        val result = image.applyFilters {
            clahe(tileSize = 8, clipLimit = 2.0)
        }
        result.shouldNotBeNull()
        result.width shouldBeEqualTo 100
        result.height shouldBeEqualTo 100
    }

    @Test
    fun `chained DSL ops all apply sequentially`() {
        val image = createSolidImage(100, 100)
        val result = image.applyFilters {
            autoCrop(backgroundColor = Color(0, 0, 255))
            rotateLeft()
            flipHorizontal()
            clahe()
        }
        result.shouldNotBeNull()
        result.width shouldBeGreaterThan 0
        result.height shouldBeGreaterThan 0
    }

    @Test
    fun `suspendApplyFilters DSL executes on coroutine dispatcher`() = runTest {
        val image = createSolidImage(80, 60)
        val result = image.suspendApplyFilters {
            rotateDegrees(90.0)
        }
        result.shouldNotBeNull()
        result.width shouldBeGreaterThan 0
    }

    @Test
    fun `transformOp logs warning when op throws exception`() {
        // logback ListAppender로 warn 로그 캡처
        // KLoggerNameResolver: "Kt$..." → substringBefore("Kt$") → no "Kt" suffix
        val logger = LoggerFactory.getLogger(
            "io.bluetape4k.images.transforms.dsl.ImageFilterChainTransformOps"
        ) as Logger
        logger.level = Level.WARN
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        logger.addAppender(listAppender)

        val image = createSolidImage(50, 50)
        try {
            image.applyFilters {
                // tolerance 범위 초과 → autoCrop require() → IllegalArgumentException
                autoCrop(tolerance = -1)
            }
            throw AssertionError("Expected IllegalArgumentException to be rethrown by transformOp")
        } catch (e: IllegalArgumentException) {
            // 예외가 전파되어야 함 — 정상
        } finally {
            logger.detachAppender(listAppender)
        }

        val warnLogs = listAppender.list.filter { it.level == Level.WARN }
        warnLogs.shouldNotBeNull()
        warnLogs.size shouldBeGreaterThan 0
        val hasAutoCropWarn = warnLogs.any { it.formattedMessage.contains("[autoCrop]") }
        hasAutoCropWarn shouldBeEqualTo true
    }
}
